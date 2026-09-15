package com.landhub.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomAuthenticationSuccessHandler successHandler;

    public SecurityConfig(CustomAuthenticationSuccessHandler successHandler) {
        this.successHandler = successHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/lands", "/lands/**", "/login", "/register", "/error",
                                "/about", "/contact", "/promotions", "/css/**", "/js/**",
                                "/images/**", "/uploads/lands/**").permitAll()
                        .requestMatchers("/customer/**").hasRole(Role.CUSTOMER.name())
                        .requestMatchers("/verification/**").hasAnyRole(Role.ADMIN.name(), Role.VERIFICATION_OFFICER.name())
                        .requestMatchers("/marketing/**").hasAnyRole(Role.ADMIN.name(), Role.MARKETING_STAFF.name())
                        .requestMatchers("/admin/payments/**").hasAnyRole(Role.ADMIN.name(), Role.STAFF.name())
                        .requestMatchers("/admin/customers/**", "/admin/reviews/**", "/admin/feedback/**")
                        .hasAnyRole(Role.ADMIN.name(), Role.STAFF.name())
                        .requestMatchers("/admin/**").hasAnyRole(Role.ADMIN.name(), Role.STAFF.name(),
                                Role.VERIFICATION_OFFICER.name(), Role.MARKETING_STAFF.name())
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler(successHandler)
                        .failureUrl("/login?error")
                        .permitAll()
                )
                .rememberMe(remember -> remember
                        .key("landhub-development-remember-me-key")
                        .rememberMeParameter("remember-me")
                        .tokenValiditySeconds(7 * 24 * 60 * 60)
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .exceptionHandling(exception -> exception.accessDeniedPage("/403"));

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetailsService,
                                                            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
