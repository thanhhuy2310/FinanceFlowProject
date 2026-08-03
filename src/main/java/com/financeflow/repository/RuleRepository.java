package com.financeflow.repository;
import com.financeflow.entity.Rule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RuleRepository extends JpaRepository<Rule,Long> {
    List<Rule> findByUserId(Long userId);

    Optional<Rule> findByIdAndUserId(Long id, Long userId);

    List<Rule> findByUserIdAndIsActiveTrue(Long userId);

    List<Rule> findByUserIdAndIsActiveTrueOrderByPriorityAsc(Long userId);

    Optional<Rule> findByUserIdAndKeyword(
            Long userId,
            String keyword
    );

    boolean existsByUserIdAndKeyword(Long userId, String keyword);

    boolean existsByUserIdAndKeywordAndIdNot(Long userId, String keyword, Long id);
}
