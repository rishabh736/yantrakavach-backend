/*
 * Yantra Kavach — ESP32 sensor gateway
 * ------------------------------------
 * Reads whichever of the five machine parameters you have sensors wired up for, and POSTs them as JSON
 * to the Yantra Kavach backend every SEND_INTERVAL_MS. You do NOT need all five sensors to get started —
 * flip the ENABLE_* switches below to match what's actually wired, and the backend will only update the
 * parameters you send (see IngestRequest.java / DeviceService.java on the backend).
 *
 * REQUIRED LIBRARY (Arduino IDE: Tools → Manage Libraries…):
 *   - "ArduinoJson" by Benoit Blanchon (v6 or v7)
 *
 * BOARD: any ESP32 dev board (this sketch uses WiFi.h + HTTPClient.h, both built into the ESP32 core).
 *
 * BEFORE YOU FLASH THIS:
 *   1. In the Yantra Kavach backend, provision a device and copy the API key it prints (shown once):
 *        curl -u admin@yantrakavach.in -X POST http://<server>:8080/api/devices \
 *          -H "Authorization: Bearer <JWT from /api/auth/login>" \
 *          -H "Content-Type: application/json" \
 *          -d '{"deviceId":"ESP32-01","machineId":"01","label":"CNC Machine 01 gateway"}'
 *   2. Fill in WIFI_SSID / WIFI_PASSWORD, SERVER_HOST / SERVER_PORT and DEVICE_API_KEY below.
 *   3. Wire up whichever sensors you have and set the matching ENABLE_* flag to 1; leave the rest at 0 —
 *      an unwired analog pin will just read electrical noise, so don't send readings you didn't actually
 *      measure.
 *   4. Calibrate: the conversion formulas below (ADC counts → physical units) are the standard reference
 *      formulas for the sensor modules named in each comment, not a substitute for calibrating against a
 *      known-good reference instrument on your actual hardware. Treat the constants marked CALIBRATE ME
 *      as starting points.
 *
 * WIRE FORMAT: this sketch posts the richer, unit-suffixed JSON shape (temperature_c, vibration_rms, ...)
 * — the backend's IngestRequest.java accepts this shape and the shorter temperature/vibration/... shape
 * equally, via @JsonAlias, so either is fine. machine_id below is informational only: which machine gets
 * updated is always decided by DEVICE_API_KEY (i.e. by what you tied this device to when you provisioned
 * it), never by this field — see the backend README's "Two accepted JSON shapes" section for why.
 */

#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>
#include <time.h>

// ====================== EDIT THESE ======================
const char* WIFI_SSID       = "YOUR_WIFI_SSID";
const char* WIFI_PASSWORD   = "YOUR_WIFI_PASSWORD";

const char* SERVER_HOST     = "192.168.1.50";   // the machine running the Spring Boot backend
const uint16_t SERVER_PORT  = 8080;
const char* DEVICE_API_KEY  = "yk_PASTE_YOUR_PROVISIONED_KEY_HERE";

const unsigned long SEND_INTERVAL_MS = 4000;    // how often to post a reading (ms)

// Purely descriptive — see the WIRE FORMAT note above. Doesn't need to match anything on the backend.
const char* MACHINE_LABEL   = "YK-MOTOR-001";

// Which sensors are actually wired up. Set to 1 only for what you have.
#define ENABLE_VIBRATION   1   // accelerometer, e.g. ADXL335 (analog) or MPU6050 (I2C — see note below)
#define ENABLE_TEMPERATURE 1   // e.g. DS18B20 (OneWire) or an analog thermistor/LM35
#define ENABLE_SOUND       1   // sound sensor module, e.g. KY-038 (analog envelope output)
#define ENABLE_CURRENT     1   // current sensor, e.g. ACS712 (analog, Hall-effect)
#define ENABLE_VOLTAGE     1   // AC voltage sensor, e.g. ZMPT101B (analog) — never wire mains directly to the ESP32!
#define ENABLE_POWER       1   // derived from current × voltage below — not a separate sensor
#define ENABLE_RPM         0   // needs a tachometer / Hall-effect pulse sensor on PIN_RPM (digital, interrupt-capable)
#define ENABLE_TIMESTAMP   1   // needs NTP over the internet at boot; if your network has none, set to 0

// Analog pins (ESP32 ADC1 pins recommended: 32-39, since ADC2 conflicts with WiFi)
const int PIN_VIBRATION   = 34;
const int PIN_TEMPERATURE = 35;
const int PIN_SOUND       = 32;
const int PIN_CURRENT     = 33;
const int PIN_VOLTAGE     = 36;
const int PIN_RPM         = 27;      // digital input, pulse-per-revolution sensor
const int PULSES_PER_REV  = 1;       // CALIBRATE ME: how many pulses your sensor gives per shaft revolution
// ==========================================================

unsigned long lastSend = 0;

#if ENABLE_RPM
volatile unsigned long rpmPulseCount = 0;
void IRAM_ATTR onRpmPulse() { rpmPulseCount++; }
#endif

void connectWiFi() {
  if (WiFi.status() == WL_CONNECTED) return;
  Serial.print("Connecting to WiFi");
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  unsigned long start = millis();
  while (WiFi.status() != WL_CONNECTED && millis() - start < 15000) {
    delay(300);
    Serial.print(".");
  }
  Serial.println();
  if (WiFi.status() == WL_CONNECTED) {
    Serial.print("WiFi connected, IP: ");
    Serial.println(WiFi.localIP());
  } else {
    Serial.println("WiFi connect failed — will retry next cycle.");
  }
}

// ---- Sensor reads. Each returns a physical value in the unit the backend expects. ----
// ESP32 ADC: 12-bit, 0-4095, ~0-3.3V (actual usable range is nonlinear near the rails — for serious
// accuracy use esp_adc_cal / analogSetAttenuation, omitted here to keep the example short).

float readVibrationMmPerSec() {
  // ADXL335-style analog accelerometer: convert ADC counts to g, then to a crude mm/s "activity" figure.
  // CALIBRATE ME: ZERO_G_COUNTS is the ADC reading at rest; SENSITIVITY depends on your module's datasheet.
  const float ZERO_G_COUNTS = 2048.0;   // mid-scale at 0 g
  const float COUNTS_PER_G  = 1024.0;   // CALIBRATE ME
  int raw = analogRead(PIN_VIBRATION);
  float g = (raw - ZERO_G_COUNTS) / COUNTS_PER_G;
  float mmPerSec = fabs(g) * 35.0;      // CALIBRATE ME — scale factor to your machine's normal vibration range
  return mmPerSec;
}

float readTemperatureC() {
  // Analog thermistor/LM35-style sensor. If you use a DS18B20 instead, replace this with the OneWire /
  // DallasTemperature library's getTempCByIndex(0) call.
  int raw = analogRead(PIN_TEMPERATURE);
  float voltage = raw * (3.3 / 4095.0);
  float celsius = voltage * 100.0;      // LM35: 10 mV/°C — CALIBRATE ME for your actual sensor
  return celsius;
}

float readSoundDb() {
  // KY-038-style envelope output: higher ADC reading = louder. There is no universal counts→dB formula;
  // CALIBRATE ME against a phone decibel-meter app at a few known volumes and fit your own curve.
  int raw = analogRead(PIN_SOUND);
  float db = 40.0 + (raw / 4095.0) * 60.0; // placeholder linear map, 40-100 dB — CALIBRATE ME
  return db;
}

float readCurrentA() {
  // ACS712-style Hall-effect sensor. 185/100/66 mV per A depending on the module variant (5A/20A/30A).
  const float ZERO_CURRENT_V = 1.65;    // CALIBRATE ME: voltage at 0 A (usually mid-supply)
  const float MV_PER_AMP     = 100.0;   // CALIBRATE ME: match your ACS712 variant
  int raw = analogRead(PIN_CURRENT);
  float voltage = raw * (3.3 / 4095.0);
  float amps = fabs(voltage - ZERO_CURRENT_V) * 1000.0 / MV_PER_AMP;
  return amps;
}

float readVoltageV() {
  // ZMPT101B-style AC voltage sensor (outputs a scaled AC waveform — sample and scale to RMS mains volts).
  // This simple version just scales a single ADC sample; for real RMS, sample for one full mains cycle
  // (~20 ms at 50 Hz) and compute sqrt(mean(sample^2)).
  const float SCALE = 0.35; // CALIBRATE ME against a multimeter reading of your actual mains voltage
  int raw = analogRead(PIN_VOLTAGE);
  float volts = raw * SCALE;
  return volts;
}

#if ENABLE_RPM
int readRpm() {
  // Counts pulses since the last call and converts to revolutions per minute over that window.
  noInterrupts();
  unsigned long pulses = rpmPulseCount;
  rpmPulseCount = 0;
  interrupts();
  float minutesElapsed = SEND_INTERVAL_MS / 60000.0;
  return (int) round((pulses / (float) PULSES_PER_REV) / minutesElapsed);
}
#endif

#if ENABLE_TIMESTAMP
// Returns an ISO-8601 UTC timestamp like "2026-10-18T10:00:00Z", or "" if NTP hasn't synced yet
// (e.g. no internet access — time.h falls back to a clearly-wrong 1970 date in that case, which this
// checks for and suppresses rather than sending a misleading timestamp).
String getIsoTimestamp() {
  time_t now;
  time(&now);
  if (now < 1700000000) return ""; // before ~2023 → clock hasn't synced
  struct tm timeinfo;
  gmtime_r(&now, &timeinfo);
  char buf[25];
  strftime(buf, sizeof(buf), "%Y-%m-%dT%H:%M:%SZ", &timeinfo);
  return String(buf);
}
#endif

void postReadings() {
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("Skipping send — WiFi not connected.");
    return;
  }

  JsonDocument doc; // ArduinoJson v7; use StaticJsonDocument<384> doc; on ArduinoJson v6

  doc["machine_id"] = MACHINE_LABEL; // informational only — see the WIRE FORMAT note at the top of this file
#if ENABLE_TIMESTAMP
  String ts = getIsoTimestamp();
  if (ts.length() > 0) doc["timestamp"] = ts; // omitted entirely if NTP hasn't synced yet
#endif

  float current = 0, voltage = 0;
  bool haveCurrent = false, haveVoltage = false;

#if ENABLE_TEMPERATURE
  doc["temperature_c"] = readTemperatureC();
#endif
#if ENABLE_VIBRATION
  doc["vibration_rms"] = readVibrationMmPerSec();
#endif
#if ENABLE_SOUND
  doc["sound_level"] = readSoundDb();
#endif
#if ENABLE_CURRENT
  current = readCurrentA();
  haveCurrent = true;
  doc["current_a"] = current;
#endif
#if ENABLE_VOLTAGE
  voltage = readVoltageV();
  haveVoltage = true;
  doc["voltage_v"] = voltage;
#endif
#if ENABLE_POWER
  if (haveCurrent && haveVoltage) {
    doc["power_w"] = current * voltage; // single-phase approximation — adjust for 3-phase/power factor if needed
  }
#endif
#if ENABLE_RPM
  doc["rpm"] = readRpm();
#endif
  doc["sensor_status"] = "ok"; // set to something else here if you want to report a local fault condition

  doc["rssi"] = WiFi.RSSI();
  // Only meaningful if this gateway runs off a battery with a voltage-divider on another analog pin.
  // Left at a fixed 100 here; wire one up and replace this if you need real battery reporting.
  doc["battery"] = 100;

  String body;
  serializeJson(doc, body);

  HTTPClient http;
  String url = String("http://") + SERVER_HOST + ":" + String(SERVER_PORT) + "/api/ingest/readings";
  http.begin(url);
  http.addHeader("Content-Type", "application/json");
  http.addHeader("X-Device-Key", DEVICE_API_KEY);
  http.setTimeout(5000);

  int status = http.POST(body);
  if (status > 0) {
    Serial.printf("POST %s -> %d: %s\n", url.c_str(), status, http.getString().c_str());
  } else {
    Serial.printf("POST failed: %s\n", http.errorToString(status).c_str());
  }
  http.end();
}

void setup() {
  Serial.begin(115200);
  delay(500);
  analogReadResolution(12);
  connectWiFi();

#if ENABLE_TIMESTAMP
  if (WiFi.status() == WL_CONNECTED) {
    configTime(0, 0, "pool.ntp.org", "time.nist.gov"); // UTC; readings are timestamped in UTC (trailing "Z")
  }
#endif
#if ENABLE_RPM
  pinMode(PIN_RPM, INPUT_PULLUP);
  attachInterrupt(digitalPinToInterrupt(PIN_RPM), onRpmPulse, FALLING);
#endif
}

void loop() {
  connectWiFi(); // cheap no-op once connected; retries automatically if the link drops

  if (millis() - lastSend >= SEND_INTERVAL_MS) {
    lastSend = millis();
    postReadings();
  }
}
