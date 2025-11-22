package com.replate.usermanagementservice.service;

import com.replate.usermanagementservice.dto.RegisterRequest;
import com.replate.usermanagementservice.dto.UpdateProfileRequest;
import com.replate.usermanagementservice.exception.EmailAlreadyExistsException;
import com.replate.usermanagementservice.exception.InvalidCredentialsException;
import com.replate.usermanagementservice.exception.MissingRequiredFieldsException;
import com.replate.usermanagementservice.exception.ResourceNotFoundException;
import com.replate.usermanagementservice.model.*;
import com.replate.usermanagementservice.repository.UserRepository;
import com.replate.usermanagementservice.kafka.UserEventProducer;
import jakarta.transaction.Transactional;
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

        switch (role) {
            case MERCHANT:
                Merchant m = new Merchant();
                if (request.getDocumentUrl() == null || request.getDocumentUrl().trim().isEmpty()) {
                    throw new MissingRequiredFieldsException("Le document justificatif (documentUrl) est obligatoire pour un commerçant.");
                }
                m.setDocumentUrl(request.getDocumentUrl());
                newUser = m;
                break;
            case ASSOCIATION:
                AssociationBeneficiary a = new AssociationBeneficiary();
                if (request.getDocumentUrl() == null || request.getDocumentUrl().trim().isEmpty()) {
                    throw new MissingRequiredFieldsException("Le document justificatif (documentUrl) est obligatoire pour un commerçant.");
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

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Aucun compte trouvé pour l'email: " + email));
    }

    public User validateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec l'ID: " + id));

        user.setStatus(AccountStatus.ACTIVE);
        return userRepository.save(user);
    }
    public List<User> getPendingValidationAccounts() {
        List<UserRole> rolesToValidate = List.of(UserRole.MERCHANT, UserRole.ASSOCIATION);
        return userRepository.findAllByStatusAndRoleIn(AccountStatus.PENDING, rolesToValidate);
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Utilisateur non trouvé avec l'ID: " + id)
                );
    }

    public List<User> getAllUsers(){
        return userRepository.findAll();
    }


    @Transactional
    public User updateUser(Long id, UpdateProfileRequest updatedUser){
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec l'ID: " + id));

        if (updatedUser.getPhoneNumber() != null && !updatedUser.getPhoneNumber().isEmpty()) {
            existingUser.setPhoneNumber(updatedUser.getPhoneNumber());
        }
        if (updatedUser.getLocation() != null && !updatedUser.getLocation().isEmpty()) {
            existingUser.setLocation(updatedUser.getLocation());
        }
        if (updatedUser.getUsername() != null && !updatedUser.getUsername().isEmpty()) {
            existingUser.setUsername(updatedUser.getUsername());
        }
        return userRepository.save(existingUser);
    }

    @Transactional
    public void changePassword(Long id , String oldPassword, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec l'ID: " + id));

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException("Ancien mot de passe incorrect.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("Utilisateur non trouvé avec l'ID :" + id);
        }
        userRepository.deleteById(id);
    }

    public void rejectUser(Long id){
        User user = userRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Utilisateur non trouvé avec l'ID :" + id));
        user.setStatus(AccountStatus.REJECTED);
        userRepository.save(user);
    }

    public List<User> getRejectedUsers(){
        List<UserRole> rolesToValidate = List.of(UserRole.MERCHANT, UserRole.ASSOCIATION);
        return userRepository.findAllByStatusAndRoleIn(AccountStatus.REJECTED, rolesToValidate);
    }

    public List<User> getSuspendedUsers(){
        List<UserRole> rolesToValidate = List.of(UserRole.MERCHANT, UserRole.ASSOCIATION);
        return userRepository.findAllByStatusAndRoleIn(AccountStatus.SUSPENDED, rolesToValidate);
    }

}