package com.codestorykh.account.repository;

import com.codestorykh.account.domain.AmlAlert;
import com.codestorykh.account.type.AmlAlertStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AmlAlertRepository extends JpaRepository<AmlAlert, Long> {

    List<AmlAlert> findByAccountId(Long accountId);

    List<AmlAlert> findByStatus(AmlAlertStatus status);

    List<AmlAlert> findByScreeningId(String screeningId);

    long countByAccountIdAndStatus(Long accountId, AmlAlertStatus status);
}
