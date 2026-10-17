package com.agroconnect.repository;

import com.agroconnect.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import com.agroconnect.enums.Role;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByRoleInOrderByCreatedAtDesc(Collection<Role> roles);
    long countByRole(Role role);
}
