package com.codestorykh.money.repository;

import com.codestorykh.money.domain.FeeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeeConfigRepository extends JpaRepository<FeeConfig, Long> {

    List<FeeConfig> findByServiceTypeIgnoreCase(String serviceType);
}
