package com.financeflow.service;

import com.financeflow.dto.request.rule.RuleRequest;
import com.financeflow.dto.response.rule.RuleResponse;
import com.financeflow.entity.Category;
import com.financeflow.entity.Rule;
import com.financeflow.entity.User;
import com.financeflow.mapper.RuleMapper;
import com.financeflow.repository.CategoryRepository;
import com.financeflow.repository.RuleRepository;
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
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RuleService {

    private final RuleRepository ruleRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final RuleMapper ruleMapper;

    @Transactional
    public RuleResponse create(RuleRequest request) {
        User currentUser = getCurrentUser();
        String keyword = request.getKeyword().trim();

        validateUniqueKeyword(currentUser.getId(), keyword, null);

        Rule rule = ruleMapper.toEntity(request);
        LocalDateTime now = LocalDateTime.now();
        rule.setUser(currentUser);
        rule.setCategory(getOwnedCategory(request.getCategoryId(), currentUser.getId()));
        rule.setKeyword(keyword);
        rule.setCreatedAt(now);
        rule.setUpdatedAt(now);

        return ruleMapper.toResponse(ruleRepository.save(rule));
    }

    @Transactional(readOnly = true)
    public List<RuleResponse> findAll() {
        User currentUser = getCurrentUser();
        return ruleRepository.findByUserId(currentUser.getId()).stream()
                .map(ruleMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<Category> findMatchingCategory(String description) {
        if (description == null || description.isBlank()) {
            return Optional.empty();
        }

        User currentUser = getCurrentUser();
        String normalizedDescription = description.toLowerCase(Locale.ROOT);

        return ruleRepository.findByUserIdAndIsActiveTrueOrderByPriorityAsc(currentUser.getId()).stream()
                .filter(rule -> normalizedDescription.contains(rule.getKeyword().toLowerCase(Locale.ROOT)))
                .map(Rule::getCategory)
                .findFirst();
    }

    @Transactional
    public RuleResponse update(Long id, RuleRequest request) {
        Rule rule = getOwnedRule(id);
        String keyword = request.getKeyword().trim();

        validateUniqueKeyword(rule.getUser().getId(), keyword, id);

        rule.setCategory(getOwnedCategory(request.getCategoryId(), rule.getUser().getId()));
        rule.setKeyword(keyword);
        rule.setPriority(request.getPriority());
        rule.setIsActive(request.getIsActive());
        rule.setUpdatedAt(LocalDateTime.now());

        return ruleMapper.toResponse(ruleRepository.save(rule));
    }

    @Transactional
    public void delete(Long id) {
        ruleRepository.delete(getOwnedRule(id));
    }

    private void validateUniqueKeyword(Long userId, String keyword, Long ruleId) {
        boolean keywordExists = ruleId == null
                ? ruleRepository.existsByUserIdAndKeyword(userId, keyword)
                : ruleRepository.existsByUserIdAndKeywordAndIdNot(userId, keyword, ruleId);

        if (keywordExists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Rule keyword already exists");
        }
    }

    private Rule getOwnedRule(Long id) {
        User currentUser = getCurrentUser();
        return ruleRepository.findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rule not found"));
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
