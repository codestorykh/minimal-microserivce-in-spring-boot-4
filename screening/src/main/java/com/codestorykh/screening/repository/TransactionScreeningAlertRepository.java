package com.codestorykh.screening.repository;

import com.codestorykh.screening.domain.transaction.TransactionScreeningAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionScreeningAlertRepository extends JpaRepository<TransactionScreeningAlert, Long> {

    List<TransactionScreeningAlert> findByScreeningId(String screeningId);

    List<TransactionScreeningAlert> findByAccountNumber(String accountNumber);
}
