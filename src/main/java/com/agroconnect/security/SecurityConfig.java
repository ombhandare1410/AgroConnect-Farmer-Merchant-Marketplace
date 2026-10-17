package com.agroconnect.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();      // one-way hash; plain passwords are never stored
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF is off to keep the project simple. The session cookie is HttpOnly + SameSite=Lax
            // (see application.properties). Re-enable CSRF tokens before a real production launch.
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/index.html", "/register.html", "/css/**", "/js/**",
                        "/images/**", "/favicon.ico", "/error",
                        "/api/health", "/api/auth/login", "/api/auth/register").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/categories").permitAll()
                .requestMatchers("/admin.html", "/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/appointments/**").hasAnyRole("FARMER", "MERCHANT")
                .requestMatchers("/api/deals/**", "/api/reviews/**").hasAnyRole("FARMER", "MERCHANT")
                .requestMatchers("/api/profiles/**").hasAnyRole("FARMER", "MERCHANT")
                .requestMatchers(HttpMethod.PUT, "/api/profile").hasAnyRole("FARMER", "MERCHANT")
                .requestMatchers("/api/listings/**", "/api/discover/merchants").hasRole("FARMER")
                .requestMatchers("/api/requirements/**", "/api/discover/farmers").hasRole("MERCHANT")
                .requestMatchers("/farmer.html").hasRole("FARMER")
                .requestMatchers("/merchant.html").hasRole("MERCHANT")
                .anyRequest().authenticated())
            .exceptionHandling(ex -> ex
                // Not logged in: API calls get JSON 401, pages go to the login page
                .authenticationEntryPoint((req, res, e) -> {
                    if (req.getRequestURI().startsWith("/api/")) {
                        res.setStatus(401);
                        res.setContentType("application/json");
                        res.getWriter().write("{\"code\":\"UNAUTHENTICATED\",\"message\":\"Please log in\"}");
                    } else {
                        res.sendRedirect("/index.html");
                    }
                })
                // Logged in but wrong role
                .accessDeniedHandler((req, res, e) -> {
                    if (req.getRequestURI().startsWith("/api/")) {
                        res.setStatus(403);
                        res.setContentType("application/json");
                        res.getWriter().write("{\"code\":\"FORBIDDEN\",\"message\":\"You are not allowed to do this\"}");
                    } else {
                        res.sendRedirect("/index.html");
                    }
                }));
        return http.build();
    }
}
