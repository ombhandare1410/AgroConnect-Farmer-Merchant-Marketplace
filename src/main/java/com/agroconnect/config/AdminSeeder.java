package com.agroconnect.config;

import com.agroconnect.entity.User;
import com.agroconnect.enums.Role;
import com.agroconnect.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates the first admin from ADMIN_EMAIL / ADMIN_PASSWORD environment variables (never hard-coded). */
@Component
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    @Value("${app.admin.email:}") private String adminEmail;
    @Value("${app.admin.password:}") private String adminPassword;

    public AdminSeeder(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (adminEmail.isBlank() || adminPassword.length() < 8) {
            System.out.println("[AgroConnect] No admin created. Set ADMIN_EMAIL and ADMIN_PASSWORD (8+ chars) to create one.");
            return;
        }
        String email = adminEmail.trim().toLowerCase();
        if (users.existsByEmail(email)) return;
        User a = new User();
        a.setRole(Role.ADMIN);
        a.setName("Administrator");
        a.setEmail(email);
        a.setPhone("0000000000");
        a.setPasswordHash(encoder.encode(adminPassword));
        a.setState("-"); a.setDistrict("-"); a.setVillageOrCity("-"); a.setPinCode("000000");
        users.save(a);
        System.out.println("[AgroConnect] Admin account created: " + email);
    }
}
