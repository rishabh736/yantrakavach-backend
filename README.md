# Yantra Kavach — Backend (Java / Spring Boot)

A Spring Boot 3 (Java 21) backend for the Yantra Kavach predictive-maintenance platform. It implements the
exact REST contract the React frontend's `src/services/api.ts` already calls, backed by a real database
(H2 by default — swap to Postgres/MySQL in minutes), JWT-based login, role checks, and a scheduled job
that nudges machine readings and raises alerts like a real sensor feed would.

> **How this was built — please read before deploying.** This project was written in a sandboxed
> environment with no internet access and no Maven/Spring jars available locally, so **it could not be
> compiled or run here**. Every file was written and re-checked by hand (package/import consistency,
> Spring Security and JPA API usage, Jackson serialization quirks — see the note on enum map keys below),
> but you should still run `mvn clean package` yourself and fix anything that surfaces before relying on
> this in production. Treat it as a thorough first draft, not a guaranteed-green build.

## Prerequisites

- JDK 21
- Maven 3.9+ (or use `./mvnw` if you generate one with `mvn -N wrapper:wrapper`)

## Run it

```bash
mvn spring-boot:run
```

or build a jar:

```bash
mvn clean package
java -jar target/yantra-kavach-backend.jar
```

The API starts on **http://localhost:8080**. On first run it seeds an H2 database file under `./data/`
with 24 machines, their sensors, a handful of alerts and maintenance records, default thresholds, and
three login accounts — all ported from the frontend's own sample-data generator
(`src/lib/mock.ts`) so a freshly connected frontend sees a familiar-looking plant. Seeding only happens
when a table is empty, so restarting the app does not reset data you've since changed.

Browse the database directly at `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:file:./data/yantrakavach`,
user `sa`, blank password) — useful for seeing exactly what the scheduled job is doing.

## Login accounts (password `demo123`)

| Role | Email |
| --- | --- |
| Admin | admin@yantrakavach.in |
| Maintenance Manager | manager@yantrakavach.in |
| Operator | operator@yantrakavach.in |

`POST /api/auth/login` returns a JWT. The frontend's own login screen doesn't call this yet — it's a
separate, client-side-only concept today — but the token this endpoint returns is ready to paste into
**Settings → API configuration → API key** once you want the frontend to authenticate its write requests
(see "Security model" below).

## Connect an ESP32

1. Start the backend, log in to get a token, and provision a device for the machine it will report for
   (Admin or Maintenance Manager role; machine ids are "01".."24" as seeded):

   ```bash
   TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"email":"admin@yantrakavach.in","password":"demo123"}' | python3 -c "import sys,json;print(json.load(sys.stdin)['token'])")

   curl -s -X POST http://localhost:8080/api/devices \
     -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
     -d '{"deviceId":"ESP32-01","machineId":"01","label":"CNC Machine 01 gateway"}'
   ```

   The response's `apiKey` is shown **once** — copy it now. (Only its SHA-256 hash is stored; it can't be
   retrieved again, only replaced by provisioning a new device.)

2. Open `firmware/esp32-yantra-kavach.ino` in the Arduino IDE, install the **ArduinoJson** library, fill in
   your Wi-Fi details, the backend's address, and the `apiKey` from step 1, wire up whichever of the five
   sensors you actually have (flip the matching `ENABLE_*` switch for each), and flash it to an ESP32.

3. The sketch POSTs to `/api/ingest/readings` every few seconds with only the parameters it's wired to
   measure. Watch it arrive:

   ```bash
   curl -s http://localhost:8080/api/machines/01 | python3 -m json.tool
   ```

No ESP32 yet? The same endpoint is just an HTTP POST, so you can simulate one from a terminal:

```bash
curl -s -X POST http://localhost:8080/api/ingest/readings \
  -H "X-Device-Key: <apiKey from step 1>" -H "Content-Type: application/json" \
  -d '{"temperature": 62.5, "vibration": 2.3, "current": 8.6}'
```

### Two accepted JSON shapes

`IngestRequest.java` accepts either the short field names shown above, **or** the richer,
`unit`-suffixed / snake_case shape some sensor firmware (and the updated example sketch) uses:

```json
{
  "machine_id": "YK-MOTOR-001",
  "timestamp": "2026-10-18T10:00:00Z",
  "temperature_c": 48.2,
  "vibration_rms": 1.8,
  "current_a": 3.4,
  "voltage_v": 230.1,
  "power_w": 720.0,
  "sound_level": null,
  "rpm": 1450,
  "sensor_status": "ok"
}
```

A few things worth knowing about this shape:

- `temperature_c`, `vibration_rms`, `current_a`, `voltage_v` and `sound_level` map onto the same five
  tracked parameters as `temperature`/`vibration`/`current`/`voltage`/`sound` — send either name, not both.
  `null` (like `sound_level` above) means "no sensor for this", exactly like omitting the field.
- **`machine_id` in the body is informational only.** Which machine gets updated is always decided by the
  `X-Device-Key` header — i.e. by which device you provisioned, and which machine you tied it to in step 1
  — never by this field. This is deliberate: if the body could redirect readings, one leaked device key
  could be used to overwrite *any* machine's data, not just the one it was provisioned for. Put whatever
  your firmware calls itself here for your own logging if that's useful; the backend logs it but never
  routes on it.
- `power_w`, `rpm` and `sensor_status` aren't part of the five-parameter health-scoring model (there's no
  threshold for RPM, for instance), but are stored on the machine as extra telemetry and returned from
  `GET /api/machines` — nothing you send is silently discarded.
- `timestamp` is stored as-is for reference. Staleness (deciding when a device has "gone quiet" and should
  fall back to the simulator) uses the backend's own clock at the moment it received the request, not this
  field — a cheap ESP32 RTC without NTP sync can't be trusted to stay accurate.

**What happens once a device is reporting:** that machine's row in `GET /api/machines` now holds the real
values you posted, its Sensor rows for the parameters you sent flip to "online", and crossing a threshold
raises a real alert — through the exact same scoring engine `/api/predictions` uses. The built-in simulator
(`TelemetryService`) automatically **stops** touching that machine's readings while its device is reporting
recently (within `app.simulation.device-timeout-seconds`, default 60 s), and resumes — with that machine's
sensors flipped back to "offline" — if the device goes quiet. One device is expected to cover all the
sensors wired to one machine; if you only send `temperature`, the other four readings simply hold their
last known value rather than being invented.

## Connect the React frontend

In the frontend project, either:

- create `.env` with `VITE_API_URL=http://localhost:8080`, or
- open the running app, go to **Settings → API configuration** (Admin role) and set the API base URL to
  `http://localhost:8080` (no trailing `/api` — the frontend already adds that).

`src/services/api.ts` tries this backend first and falls back to its own mock data if the backend is
unreachable, so nothing breaks while you're getting this running.

If your frontend runs somewhere other than `http://localhost:5173`, add its origin to
`app.cors.allowed-origins` in `src/main/resources/application.yml` (comma-separated).

## Endpoints

| Method | Path | Auth | Notes |
| --- | --- | --- | --- |
| GET | `/api/machines` | public | all machines, live readings |
| GET | `/api/machines/{id}` | public | one machine |
| POST | `/api/machines/{id}/fault` | Admin/Manager | demo helper: drifts a machine toward a fault |
| POST | `/api/machines/{id}/fault/reset` | Admin/Manager | demo helper: undoes the above |
| GET | `/api/sensors` | public | all sensors |
| GET | `/api/alerts` | public | newest first |
| POST | `/api/alerts/{id}/acknowledge` | any logged-in user | matches the frontend's `acknowledge()` |
| POST | `/api/alerts/{id}/resolve` | Admin/Manager | not yet called by the frontend (it resolves locally) — available for when you wire it up |
| POST | `/api/alerts/{id}/assign` | Admin/Manager | same as above |
| GET | `/api/maintenance` | public | newest first |
| POST | `/api/maintenance` | Admin/Manager | matches the frontend's `createMaintenance()` |
| GET | `/api/predictions` | public | explainable WHAT/WHERE/WHY/IMPACT/ACTION per machine |
| GET | `/api/analytics?range=7D\|30D\|90D` | public | plant-wide series + OEE/MTBF/MTTR/energy KPIs |
| GET | `/api/settings/thresholds` | public | current warn/crit limits, keyed by lowercase param name |
| PUT | `/api/settings/thresholds` | Admin/Manager | update limits; applies immediately |
| POST | `/api/auth/login` | public | `{ email, password }` → `{ token, user }` |
| GET | `/api/auth/me` | requires a valid token | who the current token belongs to |
| GET | `/api/devices` | Admin/Manager | list provisioned ESP32s and whether each is currently online |
| POST | `/api/devices` | Admin/Manager | provision a device; returns its API key **once** |
| DELETE | `/api/devices/{id}` | Admin/Manager | revoke a device's key |
| POST | `/api/ingest/readings` | device API key (`X-Device-Key` header, not a user login) | where an ESP32 posts its readings |

## Security model

- Most **reads** (`GET`) are open, so the dashboard works without logging in — matching how the frontend
  currently uses this API (it fetches plant data up front, before any login screen is involved).
- **Writes** (creating maintenance, acknowledging/resolving/assigning alerts, changing thresholds) require
  a valid JWT with the `Admin` or `Maintenance Manager` role, via Spring Security + a small self-contained
  HMAC-SHA256 JWT implementation in `security/JwtUtil.java` (no third-party JWT library, so there's one
  fewer dependency version to keep in sync).
- The current frontend doesn't send a token on these write calls (its own login is still a separate,
  client-side-only concept). That's fine by design: `src/services/api.ts` catches the resulting 401 and
  falls back to updating its local mock state, so nothing breaks — you just won't see the write actually
  persist server-side until the frontend is updated to log in against `/api/auth/login` and send the
  returned token as `Authorization: Bearer <token>`.
- **Change `app.jwt.secret` in `application.yml`** before deploying this anywhere other than your own
  machine.

## The telemetry engine

`service/ThresholdService.java` is a line-for-line port of the frontend's `src/lib/engine.ts`: the same
warn/crit comparison (voltage judged as % deviation from 415 V, everything else judged on its raw value),
the same weighted health score (vibration 30%, temperature 25%, current 15%, voltage 10%, sound 10%,
history 10%), and the same "worst parameter" logic. `service/PredictionService.java` ports `predict()` the
same way, so `/api/predictions` reads exactly like the frontend's own "Why is this machine at risk?" panel.

`service/TelemetryService.java` runs every `app.simulation.tick-ms` (1.5 s by default) and nudges every
machine's readings toward its target, the same way the frontend's `PlantContext.tsx` does locally. If a
machine's status gets worse, a new row is written to the `alerts` table — a real, server-side version of
"a sensor just tripped a threshold."

**One thing worth knowing:** today's frontend fetches `/api/machines` once when it starts, then runs its
*own* copy of this same simulation loop in the browser from then on — it does not re-poll the backend. So
once connected, you'll see this backend's seeded starting data, correctly computed, but you won't see the
backend's own ticking update the open tab live. To see the backend's simulation in action, watch the H2
console, call `GET /api/machines` again a few seconds apart (e.g. with curl), or add a `setInterval` re-fetch
(or a WebSocket) to the frontend — a natural next step once you're ready to make the backend the single
source of truth for live values.

## A known Jackson gotcha, fixed here

Jackson does **not** reliably apply an enum's `@JsonValue` to **Map keys** (only to values) across
versions. `/api/settings/thresholds` therefore returns `Map<String, ThresholdPairDto>` built explicitly
with `ParamKey.toJson()` as the key, rather than returning `Map<ParamKey, ...>` and hoping Jackson picks
the right serializer. Every other enum (`Severity`, `Status`, `SensorState`, `MaintenanceStatus`, `Role`)
is only ever used as a plain field value, where `@JsonValue` is applied reliably, so they didn't need the
same treatment.

## Project layout

```
src/main/java/com/yantrakavach/backend/
  domain/            JPA entities (Machine, Sensor, Alert, MaintenanceRecord, ThresholdRow, AppUser, Device)
  domain/enums/      ParamKey, Status, Severity, AlertStatus, SensorState, MaintenanceStatus, Role
                     (each serializes to the exact string the frontend's TypeScript types expect)
  repo/              Spring Data JPA repositories
  service/           ThresholdService (health engine), PredictionService, TelemetryService (the
                     scheduled tick + device staleness sweep), DeviceService (provisioning + ingest),
                     DeviceKeys (API key generation/hashing), AnalyticsService, AuthService, DataSeeder,
                     DiagnosisCatalog, MulberryRng (seeded RNG, ported from the frontend's mock data generator)
  security/          JwtUtil, JwtAuthenticationFilter
  config/            SecurityConfig (CORS + route rules)
  controller/        One controller per resource, matching src/services/api.ts's endpoint list, plus
                     DeviceController (provisioning) and IngestController (where ESP32s post readings)
  dto/               Request/response shapes that aren't plain entities
  exception/         ApiException + a @RestControllerAdvice that turns it into clean JSON errors
firmware/esp32-yantra-kavach.ino   Arduino sketch for an ESP32 sensor gateway (see "Connect an ESP32")
```

## Swapping H2 for a real database

Change the four `spring.datasource.*` lines in `application.yml` and add the right driver to `pom.xml`
(e.g. `org.postgresql:postgresql`). `spring.jpa.hibernate.ddl-auto: update` will create the tables; for
anything beyond a first deployment, switch to a proper migration tool (Flyway or Liquibase) instead.
