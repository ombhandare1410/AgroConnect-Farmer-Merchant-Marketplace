package com.agroconnect.dto;

import com.agroconnect.entity.FruitCategory;

public record CategoryResponse(Long id, String name, String nameMr, String nameHi,
                               String imageUrl, boolean active) {
    public static CategoryResponse from(FruitCategory c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getNameMr(), c.getNameHi(),
                c.getImageUrl(), c.isActive());
    }
}
