package com.financeflow.service;

import com.financeflow.dto.request.rule.RulePreviewRequest;
import com.financeflow.dto.request.rule.RuleRequest;
import com.financeflow.dto.response.rule.RuleResponse;
import com.financeflow.entity.Category;
import com.financeflow.entity.Rule;
import com.financeflow.entity.User;
import com.financeflow.enums.CategoryType;
import com.financeflow.mapper.RuleMapper;
import com.financeflow.mapper.RuleMapperImpl;
import com.financeflow.repository.CategoryRepository;
import com.financeflow.repository.RuleRepository;
import com.financeflow.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleServiceTest {

    private static final String USER_EMAIL = "user@financeflow.com";
    private static final Long USER_ID = 1L;

    @Mock
    private RuleRepository ruleRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private UserRepository userRepository;

    @Spy
    private RuleMapper ruleMapper = new RuleMapperImpl();

    @InjectMocks
    private RuleService ruleService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(USER_ID).email(USER_EMAIL).build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USER_EMAIL, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Rule rule(long id, String keyword, int priority, Category category, boolean active) {
        return Rule.builder()
                .id(id)
                .user(user)
                .keyword(keyword)
                .priority(priority)
                .isActive(active)
                .category(category)
                .build();
    }

    private Category category(long id, String name, CategoryType type) {
        return Category.builder()
                .id(id)
                .user(user)
                .name(name)
                .type(type)
                .build();
    }

    @Test
    void matchCategory_firstMatchingRuleByPriorityWins() {
        Category transport = category(1L, "Transport", CategoryType.EXPENSE);
        Category food = category(2L, "Food & Drink", CategoryType.EXPENSE);
        List<Rule> rules = List.of(
                rule(1L, "grab", 1, transport, true),
                rule(2L, "coffee", 2, food, true));

        Optional<Category> matched = ruleService.matchCategory(rules, "Grab Car to Coffee House");

        assertThat(matched).isPresent();
        assertThat(matched.get().getName()).isEqualTo("Transport");
    }

    @Test
    void matchCategory_noMatchingRule_returnsEmpty() {
        List<Rule> rules = List.of(rule(1L, "grab", 1, category(1L, "Transport", CategoryType.EXPENSE), true));

        assertThat(ruleService.matchCategory(rules, "Netflix subscription")).isEmpty();
    }

    @Test
    void matchCategory_blankDescription_returnsEmpty() {
        List<Rule> rules = List.of(rule(1L, "grab", 1, category(1L, "Transport", CategoryType.EXPENSE), true));

        assertThat(ruleService.matchCategory(rules, "  ")).isEmpty();
    }

    @Test
    void matchCategory_nullRules_returnsEmpty() {
        assertThat(ruleService.matchCategory(null, "Grab Car")).isEmpty();
    }

    @Test
    void matchCategory_caseInsensitive() {
        Category transport = category(1L, "Transport", CategoryType.EXPENSE);
        List<Rule> rules = List.of(rule(1L, "GRAB", 1, transport, true));

        Optional<Category> matched = ruleService.matchCategory(rules, "grab car");

        assertThat(matched).isPresent();
        assertThat(matched.get().getName()).isEqualTo("Transport");
    }

    @Test
    void findMatchingCategory_usesActiveRulesOrderedByPriority() {
        Category transport = category(1L, "Transport", CategoryType.EXPENSE);
        List<Rule> rules = List.of(rule(1L, "grab", 1, transport, true));

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(ruleRepository.findByUserIdAndIsActiveTrueOrderByPriorityAsc(USER_ID)).thenReturn(rules);

        Optional<Category> matched = ruleService.findMatchingCategory("Grab Car");

        assertThat(matched).isPresent();
        assertThat(matched.get().getName()).isEqualTo("Transport");
    }

    @Test
    void create_success_trimmedKeywordAndOwnedCategory() {
        Category food = category(2L, "Food & Drink", CategoryType.EXPENSE);
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(categoryRepository.findByIdAndUserId(2L, USER_ID)).thenReturn(Optional.of(food));
        when(ruleRepository.existsByUserIdAndKeyword(USER_ID, "coffee")).thenReturn(false);
        when(ruleRepository.save(any(Rule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RuleRequest request = RuleRequest.builder()
                .keyword("  coffee  ")
                .categoryId(2L)
                .priority(3)
                .isActive(true)
                .build();

        RuleResponse response = ruleService.create(request);

        assertThat(response.getKeyword()).isEqualTo("coffee");
        assertThat(response.getPriority()).isEqualTo(3);
        verify(ruleRepository).save(any(Rule.class));
    }

    @Test
    void create_duplicateKeyword_throwsConflict() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(ruleRepository.existsByUserIdAndKeyword(USER_ID, "coffee")).thenReturn(true);

        RuleRequest request = RuleRequest.builder()
                .keyword("coffee")
                .categoryId(2L)
                .priority(3)
                .isActive(true)
                .build();

        assertThatThrownBy(() -> ruleService.create(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already exists");

        verify(ruleRepository, never()).save(any(Rule.class));
    }

    @Test
    void create_categoryNotFound_throwsNotFound() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(ruleRepository.existsByUserIdAndKeyword(USER_ID, "coffee")).thenReturn(false);
        when(categoryRepository.findByIdAndUserId(2L, USER_ID)).thenReturn(Optional.empty());

        RuleRequest request = RuleRequest.builder()
                .keyword("coffee")
                .categoryId(2L)
                .priority(3)
                .isActive(true)
                .build();

        assertThatThrownBy(() -> ruleService.create(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Category not found");
    }

    @Test
    void update_success_updatesRuleFields() {
        Category transport = category(1L, "Transport", CategoryType.EXPENSE);
        Rule existing = rule(1L, "grab", 1, transport, true);
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(ruleRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(existing));
        when(ruleRepository.existsByUserIdAndKeywordAndIdNot(USER_ID, "grabcar", 1L)).thenReturn(false);
        when(categoryRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(transport));
        when(ruleRepository.save(any(Rule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RuleRequest request = RuleRequest.builder()
                .keyword("grabcar")
                .categoryId(1L)
                .priority(5)
                .isActive(false)
                .build();

        RuleResponse response = ruleService.update(1L, request);

        assertThat(response.getKeyword()).isEqualTo("grabcar");
        assertThat(response.getPriority()).isEqualTo(5);
        assertThat(response.getIsActive()).isFalse();
    }

    @Test
    void update_duplicateKeywordOnOtherRule_throwsConflict() {
        Category transport = category(1L, "Transport", CategoryType.EXPENSE);
        Rule existing = rule(1L, "grab", 1, transport, true);
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(ruleRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(existing));
        when(ruleRepository.existsByUserIdAndKeywordAndIdNot(USER_ID, "coffee", 1L)).thenReturn(true);

        RuleRequest request = RuleRequest.builder()
                .keyword("coffee")
                .categoryId(1L)
                .priority(5)
                .isActive(true)
                .build();

        assertThatThrownBy(() -> ruleService.update(1L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void preview_matchingDescription_returnsMatchedRule() {
        Category transport = category(1L, "Transport", CategoryType.EXPENSE);
        List<Rule> rules = List.of(rule(1L, "grab", 1, transport, true));

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(ruleRepository.findByUserIdAndIsActiveTrueOrderByPriorityAsc(USER_ID)).thenReturn(rules);

        var response = ruleService.preview(
                RulePreviewRequest.builder().description("Grab Car ride").build());

        assertThat(response.isMatched()).isTrue();
        assertThat(response.getKeyword()).isEqualTo("grab");
        assertThat(response.getCategoryName()).isEqualTo("Transport");
        assertThat(response.getCategoryId()).isEqualTo(1L);
    }

    @Test
    void preview_noMatchingDescription_returnsUnmatched() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(ruleRepository.findByUserIdAndIsActiveTrueOrderByPriorityAsc(USER_ID)).thenReturn(List.of());

        var response = ruleService.preview(
                RulePreviewRequest.builder().description("Netflix subscription").build());

        assertThat(response.isMatched()).isFalse();
        assertThat(response.getCategoryId()).isNull();
    }

    @Test
    void delete_success_deletesOwnedRule() {
        Rule existing = rule(1L, "grab", 1, category(1L, "Transport", CategoryType.EXPENSE), true);
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(ruleRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(existing));

        ruleService.delete(1L);

        verify(ruleRepository).delete(existing);
    }

    @Test
    void delete_ruleNotFound_throwsNotFound() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(ruleRepository.findByIdAndUserId(99L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ruleService.delete(99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Rule not found");
    }
}
