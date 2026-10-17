package com.agroconnect.service;

import com.agroconnect.dto.RegisterRequest;
import com.agroconnect.entity.User;
import com.agroconnect.enums.Language;
import com.agroconnect.enums.Role;
import com.agroconnect.exception.ApiException;
import com.agroconnect.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(RegisterRequest r) {
        // Nobody can register themselves as ADMIN
        if (r.role() != Role.FARMER && r.role() != Role.MERCHANT) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION", "Role must be FARMER or MERCHANT");
        }
        String email = r.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_EXISTS", "This email is already registered");
        }
        if (r.role() == Role.MERCHANT && (isBlank(r.businessName()) || isBlank(r.businessType()))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION", "Business name and type are required");
        }

        User u = new User();
        u.setRole(r.role());
        u.setName(r.name().trim());
        u.setPhone(r.phone());
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(r.password()));
        u.setPreferredLanguage(r.preferredLanguage() == null ? Language.EN : r.preferredLanguage());
        u.setState(r.state().trim());
        u.setDistrict(r.district().trim());
        u.setTaluka(emptyToNull(r.taluka()));
        u.setVillageOrCity(r.villageOrCity().trim());
        u.setPinCode(r.pinCode());
        if (r.role() == Role.FARMER) {
            u.setFarmName(emptyToNull(r.farmName()));
        } else {
            u.setBusinessName(r.businessName().trim());
            u.setBusinessType(r.businessType().trim());
            u.setBusinessDescription(emptyToNull(r.businessDescription()));
        }
        return userRepository.save(u);
    }

    @Transactional
    public User updateLanguage(User user, Language language) {
        user.setPreferredLanguage(language);
        return userRepository.save(user);
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
    private static String emptyToNull(String s) { return isBlank(s) ? null : s.trim(); }
}
