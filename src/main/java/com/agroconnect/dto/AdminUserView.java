package com.agroconnect.dto;

import com.agroconnect.entity.User;
import com.agroconnect.enums.Role;
import java.time.LocalDateTime;

/** Administrator's view of an account (the admin may see contact details; other users may not). */
public record AdminUserView(Long id, Role role, String name, String businessName, String email, String phone,
                            String location, boolean active, LocalDateTime createdAt) {
    public static AdminUserView from(User u) {
        return new AdminUserView(u.getId(), u.getRole(), u.getName(), u.getBusinessName(), u.getEmail(), u.getPhone(),
                u.getVillageOrCity() + ", " + u.getDistrict(), u.isActive(), u.getCreatedAt());
    }
}
