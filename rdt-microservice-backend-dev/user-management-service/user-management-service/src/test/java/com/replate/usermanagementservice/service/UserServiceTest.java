package com.replate.usermanagementservice.service;

import com.replate.usermanagementservice.dto.RegisterRequest;
import com.replate.usermanagementservice.exception.EmailAlreadyExistsException;
import com.replate.usermanagementservice.exception.InvalidCredentialsException;
import com.replate.usermanagementservice.exception.ResourceNotFoundException;
import com.replate.usermanagementservice.kafka.UserEventProducer;
import com.replate.usermanagementservice.model.Merchant;
import com.replate.usermanagementservice.model.User;
import com.replate.usermanagementservice.model.UserRole;
import com.replate.usermanagementservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserEventProducer userEventProducer;

    @InjectMocks
    private UserService userService;

    @Test
    void registerNewUser_Success_Merchant() {
        // GIVEN
        RegisterRequest request = new RegisterRequest();
        request.setEmail("merchant@test.com");
        request.setPassword("password");
        request.setRole("MERCHANT");
        request.setUsername("Shop owner");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPass");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // WHEN
        User result = userService.registerNewUser(request);

        // THEN
        assertTrue(result instanceof Merchant);
        assertEquals("merchant@test.com", result.getEmail());
        assertEquals("encodedPass", result.getPasswordHash());
        assertFalse(result.isValidated(), "Un marchand ne doit pas être validé par défaut");
        verify(userEventProducer).sendUserRegisteredEvent(result);
    }

    @Test
    void registerNewUser_Success_Admin() {
        // GIVEN
        RegisterRequest request = new RegisterRequest();
        request.setEmail("admin@test.com");
        request.setPassword("admin");
        request.setRole("ADMIN");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // WHEN
        User result = userService.registerNewUser(request);

        // THEN
        assertEquals(UserRole.ADMIN, result.getRole());
        assertTrue(result.isValidated(), "Un admin doit être auto-validé");
    }

    @Test
    void registerNewUser_EmailExists() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("exist@test.com");
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> userService.registerNewUser(request));
    }

    @Test
    void authenticate_Success() {
        User user = new User();
        user.setEmail("test@test.com");
        user.setPasswordHash("encoded");

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("raw", "encoded")).thenReturn(true);

        User result = userService.authenticate("test@test.com", "raw");
        assertNotNull(result);
    }

    @Test
    void authenticate_BadPassword() {
        User user = new User();
        user.setPasswordHash("encoded");

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> userService.authenticate("test@test.com", "wrong"));
    }
}