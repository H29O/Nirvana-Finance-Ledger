package com.heet.backend.repository;

import com.heet.backend.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByUserId(Long userId);

    Optional<Category> findByIdAndUserId(Long id, Long userId);

    boolean existsByNameIgnoreCaseAndUserId(String name, Long userId);

    boolean existsByNameIgnoreCaseAndUserIdAndIdNot(String name, Long userId, Long id);

    boolean existsByIdAndUserId(Long id, Long userId);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
