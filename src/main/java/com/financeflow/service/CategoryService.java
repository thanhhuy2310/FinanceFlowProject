package com.financeflow.service;

import com.financeflow.dto.request.category.CategoryRequest;
import com.financeflow.dto.response.category.CategoryResponse;
import com.financeflow.entity.Category;
import com.financeflow.entity.User;
import com.financeflow.mapper.CategoryMapper;
import com.financeflow.repository.CategoryRepository;
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
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final CategoryMapper categoryMapper;

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        User currentUser = getCurrentUser();
        String categoryName = request.getName().trim();

        if (categoryRepository.existsByUserIdAndName(currentUser.getId(), categoryName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category name already exists");
        }

        Category category = categoryMapper.toEntity(request);
        LocalDateTime now = LocalDateTime.now();
        category.setName(categoryName);
        category.setUser(currentUser);
        category.setIsDefault(false);
        category.setCreatedAt(now);
        category.setUpdatedAt(now);

        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        User currentUser = getCurrentUser();
        return categoryRepository.findByUserId(currentUser.getId()).stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse findById(Long id) {
        return categoryMapper.toResponse(getOwnedCategory(id));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = getOwnedCategory(id);
        String categoryName = request.getName().trim();
        Long userId = category.getUser().getId();

        if (categoryRepository.existsByUserIdAndNameAndIdNot(userId, categoryName, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category name already exists");
        }

        category.setName(categoryName);
        category.setType(request.getType());
        category.setIcon(request.getIcon());
        category.setColor(request.getColor());
        category.setUpdatedAt(LocalDateTime.now());

        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Long id) {
        categoryRepository.delete(getOwnedCategory(id));
    }

    private Category getOwnedCategory(Long id) {
        User currentUser = getCurrentUser();
        return categoryRepository.findByIdAndUserId(id, currentUser.getId())
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
