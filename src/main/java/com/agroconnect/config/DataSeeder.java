package com.agroconnect.config;

import com.agroconnect.entity.FruitCategory;
import com.agroconnect.repository.FruitCategoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final FruitCategoryRepository categoryRepository;

    public DataSeeder(FruitCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(String... args) {
        if (categoryRepository.count() > 0) {
            return; // already seeded
        }
        add("Grapes", "द्राक्षे", "अंगूर");
        add("Mango", "आंबा", "आम");
        add("Pomegranate", "डाळिंब", "अनार");
        add("Banana", "केळी", "केला");
        add("Guava", "पेरू", "अमरूद");
        add("Apple", "सफरचंद", "सेब");
        add("Orange", "संत्री", "संतरा");
        add("Custard apple (Sitaphal)", "सीताफळ", "सीताफल");
        add("Chikoo", "चिकू", "चीकू");
        add("Jamun", "जांभूळ", "जामुन");
        add("Papaya", "पपई", "पपीता");
        add("Watermelon", "कलिंगड", "तरबूज");
        add("Muskmelon", "खरबूज", "खरबूजा");
        add("Pineapple", "अननस", "अनानास");
        add("Lemon", "लिंबू", "नींबू");
        add("Sweet lime", "मोसंबी", "मौसंबी");
        add("Jackfruit", "फणस", "कटहल");
        add("Strawberry", "स्ट्रॉबेरी", "स्ट्रॉबेरी");
        add("Other seasonal fruits", "इतर हंगामी फळे", "अन्य मौसमी फल");
    }

    private void add(String name, String nameMr, String nameHi) {
        FruitCategory c = new FruitCategory();
        c.setName(name);
        c.setNameMr(nameMr);
        c.setNameHi(nameHi);
        categoryRepository.save(c);
    }
}
