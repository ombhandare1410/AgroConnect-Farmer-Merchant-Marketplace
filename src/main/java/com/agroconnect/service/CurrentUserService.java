package com.agroconnect.service;

import com.agroconnect.entity.User;
import com.agroconnect.exception.ApiException;
import com.agroconnect.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/** Finds the logged-in user's database record from the Spring Security login. */
@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User get(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Please log in"));
        // An administrator may deactivate an account while its owner is still logged in: stop them immediately.
        if (!user.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED", "This account is deactivated");
        }
        return user;
    }
}
