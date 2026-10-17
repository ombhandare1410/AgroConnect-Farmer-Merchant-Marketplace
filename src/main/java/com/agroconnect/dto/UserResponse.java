package com.agroconnect.dto;

import com.agroconnect.entity.User;
import com.agroconnect.enums.Language;
import com.agroconnect.enums.Role;

/** What we send to the browser about a user. Note: no password hash. */
public record UserResponse(Long id, Role role, String name, String email, String phone,
                           Language preferredLanguage, String state, String district, String taluka,
                           String villageOrCity, String pinCode, String farmName, String businessName,
                           String businessType, String businessDescription, String avatarUrl) {

    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getRole(), u.getName(), u.getEmail(), u.getPhone(),
                u.getPreferredLanguage(), u.getState(), u.getDistrict(), u.getTaluka(),
                u.getVillageOrCity(), u.getPinCode(), u.getFarmName(), u.getBusinessName(),
                u.getBusinessType(), u.getBusinessDescription(), u.getAvatarUrl());
    }
}
