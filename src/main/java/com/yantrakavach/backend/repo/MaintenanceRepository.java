package com.yantrakavach.backend.repo;

import com.yantrakavach.backend.domain.MaintenanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaintenanceRepository extends JpaRepository<MaintenanceRecord, String> {
    List<MaintenanceRecord> findAllByOrderByDateDesc();
    List<MaintenanceRecord> findByMachineId(String machineId);
}
