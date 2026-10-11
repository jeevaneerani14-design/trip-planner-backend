package com.tripplanner;

import com.tripplanner.dto.request.RegisterRequest;
import com.tripplanner.entity.User;
import com.tripplanner.exception.BadRequestException;
import com.tripplanner.repository.UserRepository;
import com.tripplanner.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceTest {
    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void register_shouldCreateUserAndHashPassword() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Nimisha");
        request.setEmail("nimisha@example.com");
        request.setPassword("secret123");
        request.setConfirmPassword("secret123");

        var dto = authService.register(request);

        assertEquals("Nimisha", dto.getName());
        User stored = userRepository.findByEmail("nimisha@example.com").orElseThrow();
        assertTrue(passwordEncoder.matches("secret123", stored.getPasswordHash()));
        assertNotEquals("secret123", stored.getPasswordHash());
    }

    @Test
    void register_shouldRejectDuplicateEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Alice");
        request.setEmail("alice@example.com");
        request.setPassword("secret123");
        request.setConfirmPassword("secret123");
        authService.register(request);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> authService.register(request));
        assertTrue(exception.getMessage().contains("Email already registered"));
    }
}
