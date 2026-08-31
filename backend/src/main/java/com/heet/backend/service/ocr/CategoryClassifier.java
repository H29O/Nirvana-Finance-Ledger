package com.heet.backend.service.ocr;

import com.heet.backend.entity.Category;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CategoryClassifier {

    public record CategorySuggestion(Long categoryId, String categoryName, String confidence) {}

    private static final Map<String, Set<String>> CLUSTER_KEYWORDS = new LinkedHashMap<>();
    private static final Map<String, Set<String>> CLUSTER_TARGET_NAMES = new LinkedHashMap<>();

    static {
        // 1. Healthcare & Medical
        CLUSTER_KEYWORDS.put("HEALTHCARE", new HashSet<>(Arrays.asList(
                "pharmacy", "medical", "medicals", "medicine", "medicines", "tablet", "tablets",
                "capsule", "capsules", "syrup", "syrups", "injection", "injections", "ointment",
                "prescription", "hospital", "clinic", "doctor", "health", "healthcare", "pharma",
                "pharmaceutical", "pharmaceuticals", "drug", "drugs", "drugstore", "medico", "apollo", "medplus",
                "1mg", "netmeds", "jan sewa", "pathology", "diagnostic", "diagnostics", "lab",
                "dentist", "dental", "opticals", "eyecare", "rx", "dosage", "chemist", "chemists",
                "druggist", "druggists", "wellness", "ayurvedic", "homeopathy", "surgical", "bandage",
                "healthcare & medical", "healthcare / medical", "health & medical", "care"
        )));
        CLUSTER_TARGET_NAMES.put("HEALTHCARE", new HashSet<>(Arrays.asList(
                "healthcare & medical", "healthcare / medical", "healthcare", "medical", "health & medical",
                "health", "medicine", "medicines", "pharmacy", "wellness", "doctor", "clinic", "hospital", "pharma"
        )));

        // 2. Groceries
        CLUSTER_KEYWORDS.put("GROCERIES", new HashSet<>(Arrays.asList(
                "supermarket", "grocery", "groceries", "mart", "dmart", "d-mart",
                "kirana", "provision", "provisions", "vegetable", "vegetables", "fruits",
                "hypermarket", "spencer", "reliance fresh", "reliance smart", "nature's basket",
                "more", "blinkit", "zepto", "instamart", "bigbasket", "milk", "dairy", "bread",
                "flour", "atta", "rice", "dal", "oil", "spices", "food bazaar", "staples"
        )));
        CLUSTER_TARGET_NAMES.put("GROCERIES", new HashSet<>(Arrays.asList(
                "groceries", "supermarket", "grocery", "daily needs", "food & grocery",
                "provisions", "household"
        )));

        // 3. Food & Dining
        CLUSTER_KEYWORDS.put("FOOD_AND_DINING", new HashSet<>(Arrays.asList(
                "restaurant", "cafe", "café", "dining", "bistro", "kitchen", "food", "bakery",
                "pizzeria", "burger", "pizza", "coffee", "tea", "dhaba", "bar", "swiggy",
                "zomato", "mcdonalds", "mcdonald's", "kfc", "dominos", "domino's",
                "starbucks", "subway", "biryani", "sweet", "sweets", "mithai",
                "ice cream", "tandoor", "thali", "chaat", "dessert", "roast", "takeaway"
        )));
        CLUSTER_TARGET_NAMES.put("FOOD_AND_DINING", new HashSet<>(Arrays.asList(
                "food & dining", "food", "dining", "restaurants", "restaurant",
                "cafe", "eating out", "food & drinks", "cafe & drinks", "takeaway"
        )));

        // 4. Transportation
        CLUSTER_KEYWORDS.put("TRANSPORTATION", new HashSet<>(Arrays.asList(
                "fuel", "petrol", "diesel", "cng", "gas station", "oil", "cab", "taxi",
                "uber", "ola", "rapido", "metro", "toll", "parking", "auto", "railway",
                "irctc", "bus", "fastag", "hpcl", "bpcl", "iocl"
        )));
        CLUSTER_TARGET_NAMES.put("TRANSPORTATION", new HashSet<>(Arrays.asList(
                "transportation", "transport", "fuel", "transport & fuel",
                "vehicle", "commute"
        )));

        // 5. Housing & Rent
        CLUSTER_KEYWORDS.put("HOUSING", new HashSet<>(Arrays.asList(
                "rent", "housing", "landlord", "flat", "apartment", "maintenance",
                "nobroker", "magicbricks", "society", "home repair"
        )));
        CLUSTER_TARGET_NAMES.put("HOUSING", new HashSet<>(Arrays.asList(
                "housing & rent", "housing", "rent", "house rent", "property"
        )));

        // 6. Bills & Utilities
        CLUSTER_KEYWORDS.put("UTILITIES", new HashSet<>(Arrays.asList(
                "electricity", "broadband", "wifi", "telecom", "airtel", "jio", "vi",
                "vodafone", "bescom", "tneb", "mseb", "water bill", "gas bill", "recharge",
                "utility", "cylinder", "indane", "hp gas", "bharat gas", "dth", "tata sky"
        )));
        CLUSTER_TARGET_NAMES.put("UTILITIES", new HashSet<>(Arrays.asList(
                "bills & utilities", "utilities", "bills", "electricity & bills", "electricity",
                "mobile & internet", "utilities & bills", "recharge", "internet"
        )));

        // 7. Shopping
        CLUSTER_KEYWORDS.put("SHOPPING", new HashSet<>(Arrays.asList(
                "retail", "amazon", "flipkart", "croma", "vijay sales",
                "reliance digital", "electronics", "gadgets", "online shopping"
        )));
        CLUSTER_TARGET_NAMES.put("SHOPPING", new HashSet<>(Arrays.asList(
                "shopping", "online shopping", "retail", "electronics", "shopping & retail"
        )));

        // 8. Clothing
        CLUSTER_KEYWORDS.put("CLOTHING", new HashSet<>(Arrays.asList(
                "apparel", "clothing", "fashion", "zara", "h&m", "westside",
                "pantaloons", "myntra", "ajio", "trends", "lifestyle", "shopper",
                "shoppers stop", "shirt", "shoes", "footwear", "dress"
        )));
        CLUSTER_TARGET_NAMES.put("CLOTHING", new HashSet<>(Arrays.asList(
                "clothing", "apparel", "fashion", "clothing & lifestyle"
        )));

        // 9. Personal Care
        CLUSTER_KEYWORDS.put("PERSONAL_CARE", new HashSet<>(Arrays.asList(
                "salon", "spa", "cosmetics", "nykaa", "purplle", "shaving",
                "haircut", "parlour", "skincare", "grooming", "self-care", "beauty"
        )));
        CLUSTER_TARGET_NAMES.put("PERSONAL_CARE", new HashSet<>(Arrays.asList(
                "personal care", "salon", "grooming", "beauty", "wellness"
        )));

        // 10. Entertainment
        CLUSTER_KEYWORDS.put("ENTERTAINMENT", new HashSet<>(Arrays.asList(
                "cinema", "theatre", "theater", "movie", "movies", "pvr", "inox",
                "cinepolis", "bookmyshow", "gaming", "playstation", "concert", "amusement", "multiplex"
        )));
        CLUSTER_TARGET_NAMES.put("ENTERTAINMENT", new HashSet<>(Arrays.asList(
                "entertainment", "movies", "recreation", "fun"
        )));

        // 11. Education
        CLUSTER_KEYWORDS.put("EDUCATION", new HashSet<>(Arrays.asList(
                "course", "udemy", "coursera", "tuition", "school", "college",
                "university", "books", "stationery", "textbook", "education"
        )));
        CLUSTER_TARGET_NAMES.put("EDUCATION", new HashSet<>(Arrays.asList(
                "education", "learning", "tuition", "books"
        )));

        // 12. Travel
        CLUSTER_KEYWORDS.put("TRAVEL", new HashSet<>(Arrays.asList(
                "hotel", "resort", "booking.com", "airbnb", "oyo", "trip", "flight",
                "airline", "indigo", "air india", "spicejet", "makemytrip", "visa", "holiday", "travel"
        )));
        CLUSTER_TARGET_NAMES.put("TRAVEL", new HashSet<>(Arrays.asList(
                "travel", "vacation", "flights", "hotels", "travel & vacations"
        )));

        // 13. Subscriptions
        CLUSTER_KEYWORDS.put("SUBSCRIPTIONS", new HashSet<>(Arrays.asList(
                "netflix", "spotify", "prime", "prime video", "hotstar", "youtube premium",
                "apple music", "github", "chatgpt", "openai", "google one", "icloud", "saas", "subscription"
        )));
        CLUSTER_TARGET_NAMES.put("SUBSCRIPTIONS", new HashSet<>(Arrays.asList(
                "subscriptions", "subscription", "recurring", "memberships"
        )));
    }

    public CategorySuggestion suggestCategory(String rawText, String merchant, List<Category> userCategories) {
        if ((rawText == null && merchant == null) || userCategories == null || userCategories.isEmpty()) {
            return null;
        }

        String merchantText = merchant != null ? merchant.toLowerCase(Locale.ROOT) : "";
        String bodyText = rawText != null ? rawText.toLowerCase(Locale.ROOT) : "";
        String fullText = merchantText + " " + bodyText;

        // 1. Check if merchant text matches any cluster keywords with high weight
        String bestCluster = null;
        int maxClusterScore = 0;

        for (Map.Entry<String, Set<String>> entry : CLUSTER_KEYWORDS.entrySet()) {
            String cluster = entry.getKey();
            Set<String> keywords = entry.getValue();
            int score = 0;

            for (String kw : keywords) {
                if (!merchantText.isEmpty() && merchantText.contains(kw)) {
                    score += 10;
                } else if (!bodyText.isEmpty() && bodyText.contains(kw)) {
                    score += 2;
                }
            }

            if (score > maxClusterScore) {
                maxClusterScore = score;
                bestCluster = cluster;
            }
        }

        // 2. If a cluster is matched, search user's categories
        if (bestCluster != null && maxClusterScore > 0) {
            Set<String> targetNames = CLUSTER_TARGET_NAMES.get(bestCluster);
            Set<String> clusterKeywords = CLUSTER_KEYWORDS.get(bestCluster);

            // Pass 1: exact or partial target category name match
            for (Category userCat : userCategories) {
                String catNameLower = userCat.getName().toLowerCase(Locale.ROOT).trim();
                for (String target : targetNames) {
                    if (catNameLower.equals(target) || catNameLower.contains(target) || target.contains(catNameLower)) {
                        return new CategorySuggestion(userCat.getId(), userCat.getName(), "HIGH");
                    }
                }
            }

            // Pass 2: match on any cluster keyword (e.g. user category is "Medicines" or "Pharma")
            for (Category userCat : userCategories) {
                String catNameLower = userCat.getName().toLowerCase(Locale.ROOT).trim();
                for (String kw : clusterKeywords) {
                    if (kw.length() >= 4 && (catNameLower.equals(kw) || catNameLower.contains(kw) || kw.contains(catNameLower))) {
                        return new CategorySuggestion(userCat.getId(), userCat.getName(), "HIGH");
                    }
                }
            }
        }

        // 3. Fallback direct match: user category name appears directly in receipt text
        for (Category userCat : userCategories) {
            String catNameLower = userCat.getName().toLowerCase(Locale.ROOT).trim();
            if (catNameLower.length() >= 4 && fullText.contains(catNameLower)) {
                return new CategorySuggestion(userCat.getId(), userCat.getName(), "HIGH");
            }
        }

        return null;
    }
}
