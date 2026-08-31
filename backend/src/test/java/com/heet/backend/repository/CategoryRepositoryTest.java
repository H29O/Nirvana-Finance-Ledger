package com.heet.backend.repository;

import com.heet.backend.entity.Category;
import com.heet.backend.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        user1 = new User();
        user1.setName("Cat Test User 1");
        user1.setEmail("cat_user1_" + System.nanoTime() + "@example.com");
        user1.setPasswordHash("hash123");
        user1 = userRepository.save(user1);

        user2 = new User();
        user2.setName("Cat Test User 2");
        user2.setEmail("cat_user2_" + System.nanoTime() + "@example.com");
        user2.setPasswordHash("hash123");
        user2 = userRepository.save(user2);
    }

    @Test
    void existsByNameIgnoreCaseAndUserId_EnforcesCaseInsensitivityAndUserIsolation() {
        String baseName = "Groc_" + System.nanoTime();

        Category cat1 = new Category();
        cat1.setName(baseName);
        cat1.setColor("#123456");
        cat1.setIcon("cart");
        cat1.setUser(user1);
        categoryRepository.save(cat1);

        // Case-insensitive match for User 1
        assertTrue(categoryRepository.existsByNameIgnoreCaseAndUserId(baseName.toLowerCase(), user1.getId()));
        assertTrue(categoryRepository.existsByNameIgnoreCaseAndUserId(baseName.toUpperCase(), user1.getId()));
        assertTrue(categoryRepository.existsByNameIgnoreCaseAndUserId(baseName, user1.getId()));

        // Different user should return false (user isolation)
        assertFalse(categoryRepository.existsByNameIgnoreCaseAndUserId(baseName, user2.getId()));
    }

    @Test
    void existsByNameIgnoreCaseAndUserIdAndIdNot_DetectsCollisionsOnUpdate() {
        String name1 = "Entertain_" + System.nanoTime();
        String name2 = "Travel_" + System.nanoTime();

        Category cat1 = new Category();
        cat1.setName(name1);
        cat1.setUser(user1);
        cat1 = categoryRepository.save(cat1);

        Category cat2 = new Category();
        cat2.setName(name2);
        cat2.setUser(user1);
        cat2 = categoryRepository.save(cat2);

        // Updating cat2 with name of cat1 -> true (collision detected)
        assertTrue(categoryRepository.existsByNameIgnoreCaseAndUserIdAndIdNot(name1.toLowerCase(), user1.getId(), cat2.getId()));

        // Updating cat1 with its own name -> false (no collision)
        assertFalse(categoryRepository.existsByNameIgnoreCaseAndUserIdAndIdNot(name1.toLowerCase(), user1.getId(), cat1.getId()));
    }

    @Test
    void findByUserIdAndFindByIdAndUserId_EnforcesIsolation() {
        String name1 = "Rent_" + System.nanoTime();
        String name2 = "SalaryCat_" + System.nanoTime();

        Category cat1 = new Category();
        cat1.setName(name1);
        cat1.setUser(user1);
        cat1 = categoryRepository.save(cat1);

        Category cat2 = new Category();
        cat2.setName(name2);
        cat2.setUser(user2);
        categoryRepository.save(cat2);

        List<Category> user1Categories = categoryRepository.findByUserId(user1.getId());
        assertEquals(1, user1Categories.size());
        assertEquals(name1, user1Categories.get(0).getName());

        Optional<Category> foundByUser2 = categoryRepository.findByIdAndUserId(cat1.getId(), user2.getId());
        assertFalse(foundByUser2.isPresent());
    }

    @Test
    void sameCategoryNameAllowedForDifferentUsers() {
        String sharedCategoryName = "Shopping";

        Category catUser1 = new Category();
        catUser1.setName(sharedCategoryName);
        catUser1.setUser(user1);
        Category savedUser1 = categoryRepository.saveAndFlush(catUser1);

        Category catUser2 = new Category();
        catUser2.setName(sharedCategoryName);
        catUser2.setUser(user2);
        Category savedUser2 = categoryRepository.saveAndFlush(catUser2);

        assertNotNull(savedUser1.getId());
        assertNotNull(savedUser2.getId());
        assertNotEquals(savedUser1.getId(), savedUser2.getId());
        assertEquals(sharedCategoryName, savedUser1.getName());
        assertEquals(sharedCategoryName, savedUser2.getName());
    }

    @Test
    void duplicateCategoryNameRejectedForSameUser() {
        String categoryName = "Shopping";

        Category cat1 = new Category();
        cat1.setName(categoryName);
        cat1.setUser(user1);
        categoryRepository.saveAndFlush(cat1);

        Category cat2 = new Category();
        cat2.setName(categoryName);
        cat2.setUser(user1);

        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> {
            categoryRepository.saveAndFlush(cat2);
        });
    }
}
