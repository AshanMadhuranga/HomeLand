package com.landhub.auth;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class DevelopmentAdminInitializer implements CommandLineRunner {

    private final Environment environment;
    private final UserService userService;

    public DevelopmentAdminInitializer(Environment environment, UserService userService) {
        this.environment = environment;
        this.userService = userService;
    }

    @Override
    public void run(String... args) {
        createUserFromEnvironment(
                "LANDHUB_ADMIN_EMAIL",
                "LANDHUB_ADMIN_PASSWORD",
                "Development",
                "Admin",
                Role.ADMIN
        );

        createUserFromEnvironment(
                "LANDHUB_VERIFICATION_EMAIL",
                "LANDHUB_VERIFICATION_PASSWORD",
                "Verification",
                "Officer",
                Role.VERIFICATION_OFFICER
        );
    }

    private void createUserFromEnvironment(String emailKey,
                                           String passwordKey,
                                           String firstName,
                                           String lastName,
                                           Role role) {
        String email = environment.getProperty(emailKey);
        String password = environment.getProperty(passwordKey);

        if (!hasText(email) || !hasText(password) || password.length() < 8 || userService.findByEmail(email).isPresent()) {
            return;
        }

        userService.createPrivilegedUser(firstName, lastName, email, password, role);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
