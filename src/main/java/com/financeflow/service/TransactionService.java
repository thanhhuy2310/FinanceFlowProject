package com.financeflow.service;

import com.financeflow.dto.request.transaction.TransactionRequest;
import com.financeflow.dto.response.transaction.TransactionResponse;
import com.financeflow.entity.Account;
import com.financeflow.entity.Category;
import com.financeflow.entity.Transaction;
import com.financeflow.entity.User;
import com.financeflow.enums.TransactionType;
import com.financeflow.mapper.TransactionMapper;
import com.financeflow.repository.AccountRepository;
import com.financeflow.repository.CategoryRepository;
import com.financeflow.repository.TransactionRepository;
import com.financeflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TransactionMapper transactionMapper;

    @Transactional
    public TransactionResponse create(TransactionRequest request) {
        User currentUser = getCurrentUser();
        Account account = getOwnedAccount(request.getAccountId(), currentUser.getId());
        Category category = getOwnedCategory(request.getCategoryId(), currentUser.getId());

        updateBalanceForCreation(account, request.getAmount(), request.getTransactionType());

        Transaction transaction = transactionMapper.toEntity(request);
        LocalDateTime now = LocalDateTime.now();
        transaction.setUser(currentUser);
        transaction.setAccount(account);
        transaction.setCategory(category);
        transaction.setCreatedAt(now);
        transaction.setUpdatedAt(now);

        return transactionMapper.toResponse(transactionRepository.save(transaction));
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> findAll() {
        User currentUser = getCurrentUser();
        return transactionRepository.findByUserIdOrderByTransactionDateDesc(currentUser.getId()).stream()
                .map(transactionMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TransactionResponse findById(Long id) {
        return transactionMapper.toResponse(getOwnedTransaction(id));
    }

    @Transactional
    public void delete(Long id) {
        Transaction transaction = getOwnedTransaction(id);
        Account account = transaction.getAccount();

        rollbackBalanceForDeletion(account, transaction.getAmount(), transaction.getTransactionType());
        transactionRepository.delete(transaction);
    }

    private void updateBalanceForCreation(Account account, BigDecimal amount, TransactionType transactionType) {
        if (transactionType == TransactionType.INCOME) {
            account.setBalance(account.getBalance().add(amount));
        } else if (transactionType == TransactionType.EXPENSE) {
            BigDecimal updatedBalance = account.getBalance().subtract(amount);
            if (updatedBalance.signum() < 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient account balance");
            }
            account.setBalance(updatedBalance);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transaction type is not supported");
        }

        account.setUpdatedAt(LocalDateTime.now());
    }

    private void rollbackBalanceForDeletion(Account account, BigDecimal amount, TransactionType transactionType) {
        if (transactionType == TransactionType.INCOME) {
            BigDecimal updatedBalance = account.getBalance().subtract(amount);
            if (updatedBalance.signum() < 0) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Cannot delete this income transaction because the account balance would become negative"
                );
            }
            account.setBalance(updatedBalance);
        } else if (transactionType == TransactionType.EXPENSE) {
            account.setBalance(account.getBalance().add(amount));
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transaction type is not supported");
        }

        account.setUpdatedAt(LocalDateTime.now());
    }

    private Transaction getOwnedTransaction(Long id) {
        User currentUser = getCurrentUser();
        return transactionRepository.findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
    }

    private Account getOwnedAccount(Long id, Long userId) {
        return accountRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
    }

    private Category getOwnedCategory(Long id, Long userId) {
        return categoryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));
    }
}
