package com.heet.backend.service.ocr;

import com.heet.backend.entity.Category;
import com.heet.backend.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CategoryClassifierTest {

    private CategoryClassifier classifier;
    private User testUser1;
    private User testUser2;
    private List<Category> user1Categories;
    private List<Category> user2Categories;

    @BeforeEach
    void setUp() {
        classifier = new CategoryClassifier();

        testUser1 = new User();
        testUser1.setId(1L);
        testUser1.setName("User 1");

        testUser2 = new User();
        testUser2.setId(2L);
        testUser2.setName("User 2");

        user1Categories = new ArrayList<>();
        user1Categories.add(createCategory(10L, "Healthcare & Medical", testUser1));
        user1Categories.add(createCategory(20L, "Groceries", testUser1));
        user1Categories.add(createCategory(30L, "Food & Dining", testUser1));
        user1Categories.add(createCategory(40L, "Transportation", testUser1));
        user1Categories.add(createCategory(50L, "Bills & Utilities", testUser1));
        user1Categories.add(createCategory(60L, "Shopping", testUser1));
        user1Categories.add(createCategory(70L, "Housing & Rent", testUser1));
        user1Categories.add(createCategory(80L, "Subscriptions", testUser1));
        user1Categories.add(createCategory(90L, "Personal Care", testUser1));

        user2Categories = new ArrayList<>();
        user2Categories.add(createCategory(100L, "Only User2 Specific Category", testUser2));
    }

    private Category createCategory(Long id, String name, User user) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setUser(user);
        return category;
    }

    @Test
    void suggestCategory_PharmacyReceipt_SuggestsHealthcareAndMedical() {
        String rawText = "Apollo Pharmacy\nRx: Paracetamol Tablets 650mg\nTotal: 450.00";
        String merchant = "Apollo Pharmacy";

        CategoryClassifier.CategorySuggestion suggestion = classifier.suggestCategory(rawText, merchant, user1Categories);

        assertNotNull(suggestion);
        assertEquals(10L, suggestion.categoryId());
        assertEquals("Healthcare & Medical", suggestion.categoryName());
    }

    @Test
    void suggestCategory_PharmaceuticalsMerchant_SuggestsHealthcareAndMedical() {
        String rawText = "JAN SEWA PHARMACEUTICALS & CHEMISTS\nBatch No: 9912\nNet Payable: 450.00";
        String merchant = "JAN SEWA PHARMACEUTICALS";

        CategoryClassifier.CategorySuggestion suggestion = classifier.suggestCategory(rawText, merchant, user1Categories);

        assertNotNull(suggestion);
        assertEquals(10L, suggestion.categoryId());
        assertEquals("Healthcare & Medical", suggestion.categoryName());
    }

    @Test
    void suggestCategory_MigliorPharmaceuticalsMerchant_SuggestsHealthcareAndMedical() {
        String rawText = "MIGLIOR PHARMACEUTICALS INDIA PVT.LTD\nPlot 44\nQTY RATE AMOUNT\n1 450.00 450.00";
        String merchant = "MIGLIOR PHARMACEUTICALS INDIA PVT.LTD";

        CategoryClassifier.CategorySuggestion suggestion = classifier.suggestCategory(rawText, merchant, user1Categories);

        assertNotNull(suggestion);
        assertEquals(10L, suggestion.categoryId());
        assertEquals("Healthcare & Medical", suggestion.categoryName());
    }

    @Test
    void suggestCategory_GroceryReceipt_SuggestsGroceries() {
        String rawText = "DMart Supermarket\nAtta 5kg, Rice 10kg, Sunflower Oil\nBill Total: 1250.00";
        String merchant = "DMart";

        CategoryClassifier.CategorySuggestion suggestion = classifier.suggestCategory(rawText, merchant, user1Categories);

        assertNotNull(suggestion);
        assertEquals(20L, suggestion.categoryId());
        assertEquals("Groceries", suggestion.categoryName());
    }

    @Test
    void suggestCategory_RestaurantReceipt_SuggestsFoodAndDining() {
        String rawText = "Barbeque Nation Restaurant\nBuffet Dinner\nGrand Total: 2100.00";
        String merchant = "Barbeque Nation Restaurant";

        CategoryClassifier.CategorySuggestion suggestion = classifier.suggestCategory(rawText, merchant, user1Categories);

        assertNotNull(suggestion);
        assertEquals(30L, suggestion.categoryId());
        assertEquals("Food & Dining", suggestion.categoryName());
    }

    @Test
    void suggestCategory_FuelReceipt_SuggestsTransportation() {
        String rawText = "Indian Oil Petrol Pump\nPetrol 10.5 Litres\nAmount: 1100.00";
        String merchant = "Indian Oil Gas Station";

        CategoryClassifier.CategorySuggestion suggestion = classifier.suggestCategory(rawText, merchant, user1Categories);

        assertNotNull(suggestion);
        assertEquals(40L, suggestion.categoryId());
        assertEquals("Transportation", suggestion.categoryName());
    }

    @Test
    void suggestCategory_NetflixSubscription_SuggestsSubscriptions() {
        String rawText = "Netflix Monthly Subscription Invoice\nPremium Plan 649.00";
        String merchant = "Netflix";

        CategoryClassifier.CategorySuggestion suggestion = classifier.suggestCategory(rawText, merchant, user1Categories);

        assertNotNull(suggestion);
        assertEquals(80L, suggestion.categoryId());
        assertEquals("Subscriptions", suggestion.categoryName());
    }

    @Test
    void suggestCategory_SalonReceipt_SuggestsPersonalCare() {
        String rawText = "Enrich Salon & Spa\nHaircut & Beard Grooming\nTotal: 750.00";
        String merchant = "Enrich Salon";

        CategoryClassifier.CategorySuggestion suggestion = classifier.suggestCategory(rawText, merchant, user1Categories);

        assertNotNull(suggestion);
        assertEquals(90L, suggestion.categoryId());
        assertEquals("Personal Care", suggestion.categoryName());
    }

    @Test
    void suggestCategory_NoMatchingUserCategory_ReturnsNull() {
        String rawText = "Apollo Pharmacy\nMedicine Total: 450";
        String merchant = "Apollo Pharmacy";

        // User only has entertainment category
        List<Category> limitedCategories = List.of(createCategory(99L, "Entertainment", testUser1));

        CategoryClassifier.CategorySuggestion suggestion = classifier.suggestCategory(rawText, merchant, limitedCategories);

        assertNull(suggestion);
    }

    @Test
    void suggestCategory_UserIsolation_DoesNotAccessOtherUserCategories() {
        String rawText = "Apollo Pharmacy\nMedicine Total: 450";
        String merchant = "Apollo Pharmacy";

        // When passing user2 categories which lacks Healthcare, should return null and not user1 categories
        CategoryClassifier.CategorySuggestion suggestion = classifier.suggestCategory(rawText, merchant, user2Categories);

        assertNull(suggestion);
    }

    @Test
    void suggestCategory_EmptyOrNullInputs_ReturnsNull() {
        assertNull(classifier.suggestCategory(null, null, user1Categories));
        assertNull(classifier.suggestCategory("Some receipt text", "Store", null));
        assertNull(classifier.suggestCategory("Some receipt text", "Store", List.of()));
    }
}
