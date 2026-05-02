package com.anveshana.search.auth.service;

import com.anveshana.search.auth.entity.User;
import com.anveshana.search.auth.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User register(String fullName, String email, String rawPassword) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already in use");
        }

        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.getRoles().add("ROLE_USER");
        user.setEnabled(false);         // disabled until email is verified
        user.setEmailVerified(false);

        return userRepository.save(user);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    @org.springframework.transaction.annotation.Transactional
    public void enableUser(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setEnabled(true);
            user.setEmailVerified(true);
            userRepository.save(user);
        });
    }
}
