package com.yantrakavach.backend.repo;

import com.yantrakavach.backend.domain.Machine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MachineRepository extends JpaRepository<Machine, String> {
}
