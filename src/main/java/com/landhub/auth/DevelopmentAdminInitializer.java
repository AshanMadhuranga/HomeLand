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
        String email = environment.getProperty("LANDHUB_ADMIN_EMAIL");
        String password = environment.getProperty("LANDHUB_ADMIN_PASSWORD");

        if (!hasText(email) || !hasText(password) || password.length() < 8 || userService.findByEmail(email).isPresent()) {
            return;
        }

        userService.createPrivilegedUser("Development", "Admin", email, password, Role.ADMIN);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
