package com.yantrakavach.backend.repo;

import com.yantrakavach.backend.domain.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, String> {
    Optional<Device> findByApiKeyHash(String apiKeyHash);
    List<Device> findByMachineId(String machineId);
}
