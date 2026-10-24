package com.yantrakavach.backend.repo;

import com.yantrakavach.backend.domain.Sensor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SensorRepository extends JpaRepository<Sensor, String> {
    List<Sensor> findByMachineId(String machineId);
}
