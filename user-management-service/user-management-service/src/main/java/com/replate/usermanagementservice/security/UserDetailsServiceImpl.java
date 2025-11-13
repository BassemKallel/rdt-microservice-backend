package com.replate.usermanagementservice.security;

import com.replate.usermanagementservice.repository.UserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        com.replate.usermanagementservice.model.User appUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé: " + email));

        return new User(
                appUser.getEmail(),
                appUser.getPasswordHash(),
                Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + appUser.getRole().name()))
        );
    }
}