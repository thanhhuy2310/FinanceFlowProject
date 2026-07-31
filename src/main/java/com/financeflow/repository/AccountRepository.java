package com.financeflow.repository;
import com.financeflow.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account,Long>{
    List<Account> findByUserId(Long userId);

    Optional<Account> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndAccountName(Long userId, String accountName);

    boolean existsByUserIdAndAccountNameAndIdNot(Long userId, String accountName, Long id);

    boolean existsByAccountNumber(String accountNumber);

    boolean existsByAccountNumberAndIdNot(String accountNumber, Long id);
}
