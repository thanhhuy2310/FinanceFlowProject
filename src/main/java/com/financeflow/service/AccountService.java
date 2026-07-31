package com.financeflow.service;

import com.financeflow.dto.request.account.AccountRequest;
import com.financeflow.dto.response.account.AccountResponse;
import com.financeflow.entity.Account;
import com.financeflow.entity.Provider;
import com.financeflow.entity.User;
import com.financeflow.mapper.AccountMapper;
import com.financeflow.repository.AccountRepository;
import com.financeflow.repository.ProviderRepository;
import com.financeflow.repository.TransactionRepository;
import com.financeflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final ProviderRepository providerRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final AccountMapper accountMapper;

    @Transactional
    public AccountResponse create(AccountRequest request) {
        User currentUser = getCurrentUser();
        String accountName = request.getAccountName().trim();
        String accountNumber = request.getAccountNumber().trim();

        validateUniqueAccount(currentUser.getId(), accountName, accountNumber, null);

        Account account = accountMapper.toEntity(request);
        LocalDateTime now = LocalDateTime.now();
        account.setUser(currentUser);
        account.setProvider(getProvider(request.getProviderId()));
        account.setAccountName(accountName);
        account.setAccountNumber(accountNumber);
        account.setIsActive(true);
        account.setCreatedAt(now);
        account.setUpdatedAt(now);

        return accountMapper.toResponse(accountRepository.save(account));
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> findAll() {
        User currentUser = getCurrentUser();
        return accountRepository.findByUserId(currentUser.getId()).stream()
                .map(accountMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse findById(Long id) {
        return accountMapper.toResponse(getOwnedAccount(id));
    }

    @Transactional
    public AccountResponse update(Long id, AccountRequest request) {
        Account account = getOwnedAccount(id);
        String accountName = request.getAccountName().trim();
        String accountNumber = request.getAccountNumber().trim();

        validateUniqueAccount(account.getUser().getId(), accountName, accountNumber, id);

        account.setProvider(getProvider(request.getProviderId()));
        account.setAccountName(accountName);
        account.setAccountNumber(accountNumber);
        account.setAccountType(request.getAccountType());
        account.setBalance(request.getBalance());
        account.setUpdatedAt(LocalDateTime.now());

        return accountMapper.toResponse(accountRepository.save(account));
    }

    @Transactional
    public void delete(Long id) {
        Account account = getOwnedAccount(id);

        if (!transactionRepository.findByAccountId(account.getId()).isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot delete an account that has transactions"
            );
        }

        accountRepository.delete(account);
    }

    private void validateUniqueAccount(
            Long userId,
            String accountName,
            String accountNumber,
            Long accountId
    ) {
        boolean accountNameExists = accountId == null
                ? accountRepository.existsByUserIdAndAccountName(userId, accountName)
                : accountRepository.existsByUserIdAndAccountNameAndIdNot(userId, accountName, accountId);

        if (accountNameExists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account name already exists");
        }

        boolean accountNumberExists = accountId == null
                ? accountRepository.existsByAccountNumber(accountNumber)
                : accountRepository.existsByAccountNumberAndIdNot(accountNumber, accountId);

        if (accountNumberExists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account number already exists");
        }
    }

    private Account getOwnedAccount(Long id) {
        User currentUser = getCurrentUser();
        return accountRepository.findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
    }

    private Provider getProvider(Long providerId) {
        return providerRepository.findById(providerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Provider not found"));
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
