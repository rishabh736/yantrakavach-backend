package com.yantrakavach.backend.repo;

import com.yantrakavach.backend.domain.ThresholdRow;
import com.yantrakavach.backend.domain.enums.ParamKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ThresholdRepository extends JpaRepository<ThresholdRow, ParamKey> {
}
