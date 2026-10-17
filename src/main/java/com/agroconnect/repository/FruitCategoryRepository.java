package com.agroconnect.repository;

import com.agroconnect.entity.FruitCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FruitCategoryRepository extends JpaRepository<FruitCategory, Long> {
    List<FruitCategory> findByActiveTrueOrderByNameAsc();
    Optional<FruitCategory> findByNameIgnoreCase(String name);
}
