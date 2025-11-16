package com.replate.usermanagementservice.service;

import com.replate.usermanagementservice.dto.RegisterRequest;
import com.replate.usermanagementservice.exception.EmailAlreadyExistsException;
import com.replate.usermanagementservice.exception.InvalidCredentialsException;
import com.replate.usermanagementservice.exception.MissingRequiredFieldsException;
import com.replate.usermanagementservice.exception.ResourceNotFoundException;
import com.replate.usermanagementservice.model.*;
import com.replate.usermanagementservice.repository.UserRepository;
import com.replate.usermanagementservice.kafka.UserEventProducer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserEventProducer userEventProducer;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, UserEventProducer userEventProducer) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userEventProducer = userEventProducer;
    }

    public User registerNewUser(RegisterRequest request)  {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Cet email est déjà utilisé.");
        }

        UserRole role = UserRole.valueOf(request.getRole().toUpperCase());
        User newUser;

        // Création de l'entité spécifique selon le rôle
        switch (role) {
            case MERCHANT:
                Merchant m = new Merchant();
                if(request.getDocumentUrl() == null || request.getDocumentUrl().isEmpty()) {
                    throw new MissingRequiredFieldsException("Le document justificatif est requis pour le rôle Merchant.");
                }
                m.setValidated(true);
                m.setDocumentUrl(request.getDocumentUrl());
                newUser = m;
                break;
            case ASSOCIATION:
                AssociationBeneficiary a = new AssociationBeneficiary();
                if(request.getDocumentUrl() == null || request.getDocumentUrl().isEmpty()) {
                    throw new MissingRequiredFieldsException("Le document justificatif est requis pour le rôle Association.");
                }
                a.setDocumentUrl(request.getDocumentUrl());
                newUser = a;
                break;
            case INDIVIDUAL:
                newUser = new IndividualBeneficiary();
                break;
            case ADMIN:
                newUser = new User();
                break;
            default:
                throw new IllegalArgumentException("Rôle invalide.");
        }

        newUser.setEmail(request.getEmail());
        newUser.setRole(role);
        newUser.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        if(request.getUsername() == null || request.getUsername().isEmpty()) {
            throw new MissingRequiredFieldsException("Le nom d'utilisateur est requis.");
        }
        newUser.setUsername(request.getUsername());
        newUser.setPhoneNumber(request.getPhoneNumber());
        newUser.setLocation(request.getLocation());

        if(role == UserRole.ADMIN) newUser.setValidated(true);

        User savedUser = userRepository.save(newUser);
        userEventProducer.sendUserRegisteredEvent(savedUser);

        return savedUser;
    }

    public User authenticate(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Aucun compte trouvé pour l'email: " + email));

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException("Mot de passe incorrect.");
        }

        return user;
    }

    public User validateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec l'ID: " + id));

        user.setValidated(true);
        return userRepository.save(user);
    }
    public List<User> getPendingValidationAccounts() {
        List<UserRole> rolesToValidate = List.of(UserRole.MERCHANT, UserRole.ASSOCIATION);
        return userRepository.findAllByIsValidatedFalseAndRoleIn(rolesToValidate);
    }

    public List<User> getAllUsers() {
        return userRepository.findAllByIsValidatedTrueAndRoleIn(List.of(UserRole.INDIVIDUAL, UserRole.MERCHANT, UserRole.ASSOCIATION));
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("Utilisateur non trouvé avec l'ID: " + id);
        }
        userRepository.deleteById(id);
    }

}