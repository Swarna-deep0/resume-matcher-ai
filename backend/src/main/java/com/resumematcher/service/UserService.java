package com.resumematcher.service;

import com.resumematcher.dto.LoginRequest;
import com.resumematcher.dto.RegisterRequest;
import com.resumematcher.entity.User;
import com.resumematcher.exception.EmailAlreadyExistsException;
import com.resumematcher.exception.InvalidCredentialsException;
import com.resumematcher.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String dummyHash;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        // Used when an email is unknown, so login takes the same time either way
        this.dummyHash = passwordEncoder.encode("dummy-password-for-timing");
    }

    public User register(RegisterRequest request) {
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException();
        }

        User user = new User(email, passwordEncoder.encode(request.password()));
        try {
            return userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            // Two requests registered the same email at the same moment
            throw new EmailAlreadyExistsException();
        }
    }

    public User authenticate(LoginRequest request) {
        String email = normalizeEmail(request.email());
        Optional<User> found = userRepository.findByEmail(email);

        // Always run one BCrypt comparison, even if the email does not exist
        String hash = found.map(User::getPasswordHash).orElse(dummyHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hash);

        if (found.isEmpty() || !passwordMatches) {
            throw new InvalidCredentialsException();
        }
        return found.get();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}