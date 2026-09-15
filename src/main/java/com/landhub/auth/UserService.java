package com.landhub.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;

import java.util.Optional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email);
    }

    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public java.util.List<User> findCustomers() {
        return userRepository.findByRoleOrderByCreatedAtDesc(Role.CUSTOMER);
    }

    @Transactional(readOnly = true)
    public long countCustomers() {
        return userRepository.countByRole(Role.CUSTOMER);
    }

    public boolean validateRegistration(RegisterForm form, BindingResult bindingResult) {
        if (!form.passwordsMatch()) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "Passwords do not match");
        }

        if (form.getEmail() != null && userRepository.existsByEmailIgnoreCase(form.getEmail())) {
            bindingResult.rejectValue("email", "email.exists", "An account with this email already exists");
        }

        return !bindingResult.hasErrors();
    }

    public User registerCustomer(RegisterForm form) {
        User user = new User();
        user.setFirstName(form.getFirstName().trim());
        user.setLastName(form.getLastName().trim());
        user.setEmail(form.getEmail().trim().toLowerCase());
        user.setPhone(form.getPhone().trim());
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        user.setRole(Role.CUSTOMER);
        user.setEnabled(true);
        return userRepository.save(user);
    }

    public User createPrivilegedUser(String firstName, String lastName, String email, String rawPassword, Role role) {
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email.trim().toLowerCase());
        user.setPhone("+0000000000");
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setEnabled(true);
        return userRepository.save(user);
    }

    public User updateCustomerProfile(String email, String firstName, String lastName, String phone) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Customer account was not found."));
        if (user.getRole() != Role.CUSTOMER) {
            throw new IllegalArgumentException("Only customer profiles can be updated here.");
        }
        user.setFirstName(requiredText(firstName, "First name is required."));
        user.setLastName(requiredText(lastName, "Last name is required."));
        user.setPhone(requiredText(phone, "Phone is required."));
        return userRepository.save(user);
    }

    public User setCustomerEnabled(Long id, boolean enabled) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer account was not found."));
        if (user.getRole() != Role.CUSTOMER) {
            throw new IllegalArgumentException("Only customer accounts can be changed here.");
        }
        user.setEnabled(enabled);
        return userRepository.save(user);
    }

    private String requiredText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
