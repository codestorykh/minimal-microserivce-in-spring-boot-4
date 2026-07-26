package com.codestorykh.account.repository;

import com.codestorykh.account.domain.Account;
import com.codestorykh.account.type.AccountStatus;
import com.codestorykh.account.type.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findAccountByAccountNumber(String accountNumber);

    List<Account> findAccountByAccountTypeAndStatus(AccountType accountType, AccountStatus accountStatus);
}
