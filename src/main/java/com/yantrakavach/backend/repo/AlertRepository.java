package com.yantrakavach.backend.repo;

import com.yantrakavach.backend.domain.Alert;
import com.yantrakavach.backend.domain.enums.AlertStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, String> {
    List<Alert> findAllByOrderByCreatedAtDesc();
    List<Alert> findByStatusNot(AlertStatus status);
    List<Alert> findByMachineId(String machineId);
}
