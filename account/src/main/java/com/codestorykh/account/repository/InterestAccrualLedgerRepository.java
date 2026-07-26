package com.codestorykh.account.repository;

import com.codestorykh.account.domain.Account;
import com.codestorykh.account.domain.InterestAccrualLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface InterestAccrualLedgerRepository extends JpaRepository<InterestAccrualLedger, Long> {

    boolean existsByAccountAndAccrualDate(Account account, LocalDate accrualDate);

    List<InterestAccrualLedger> findByAccountAndIsPostedFalse(Account account);

    @Query("SELECT COALESCE(SUM(i.dailyAccrual), 0) FROM InterestAccrualLedger i WHERE i.account = :account AND i.isPosted = false")
    BigDecimal sumUnpostedInterestByAccount(@Param("account") Account account);
}