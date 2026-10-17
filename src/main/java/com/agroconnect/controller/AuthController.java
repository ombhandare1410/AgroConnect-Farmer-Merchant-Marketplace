package com.agroconnect.controller;

import com.agroconnect.dto.*;
import com.agroconnect.exception.ApiException;
import com.agroconnect.service.AuthService;
import com.agroconnect.service.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final CurrentUserService currentUser;
    private final SecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();

    public AuthController(AuthService authService, AuthenticationManager authenticationManager,
                          CurrentUserService currentUser) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.currentUser = currentUser;
    }

    @PostMapping("/auth/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest r,
                                                 HttpServletRequest req, HttpServletResponse res) {
        authService.register(r);
        UserResponse user = doLogin(r.email(), r.password(), req, res);   // log the new user in straight away
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @PostMapping("/auth/login")
    public UserResponse login(@Valid @RequestBody LoginRequest r,
                              HttpServletRequest req, HttpServletResponse res) {
        return doLogin(r.email(), r.password(), req, res);
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/auth/me")
    public UserResponse me(Authentication authentication) {
        return UserResponse.from(currentUser.get(authentication));
    }

    @PutMapping("/profile/language")
    public UserResponse setLanguage(@Valid @RequestBody LanguageRequest r, Authentication authentication) {
        return UserResponse.from(authService.updateLanguage(currentUser.get(authentication), r.language()));
    }

    private UserResponse doLogin(String email, String password, HttpServletRequest req, HttpServletResponse res) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(email.trim().toLowerCase(), password));
            if (req.getSession(false) != null) req.changeSessionId();   // protects against session fixation
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);
            contextRepository.saveContext(context, req, res);            // remembers the login in the session
            return UserResponse.from(currentUser.get(auth));
        } catch (DisabledException e) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED", "This account is deactivated");
        } catch (AuthenticationException e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS", "Wrong email or password");
        }
    }
}
