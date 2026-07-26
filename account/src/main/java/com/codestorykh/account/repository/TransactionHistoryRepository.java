package com.codestorykh.account.repository;

import com.codestorykh.account.domain.TransactionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;

public interface TransactionHistoryRepository extends JpaRepository<TransactionHistory, Long> {

    @Query("""
        SELECT COUNT(t) FROM TransactionHistory t
        WHERE t.account.id = :accountId
          AND t.amount BETWEEN :minAmount AND :maxAmount 
          AND t.createdAt >= :windowStart
    """)
    long countTransactionsInWindow(@Param("accountId") Long accountId, 
                                   @Param("minAmount") BigDecimal minAmount, 
                                   @Param("maxAmount") BigDecimal maxAmount, 
                                   @Param("windowStart") Instant windowStart);

    @Query("SELECT COUNT(t) FROM TransactionHistory t WHERE t.account.id = :accountId AND t.createdAt >= :windowStart")
    long countTotalTransactionsSince(@Param("accountId") Long accountId, @Param("windowStart") Instant windowStart);
}