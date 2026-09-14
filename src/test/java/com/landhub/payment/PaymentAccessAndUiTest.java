package com.landhub.payment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentAccessAndUiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerCannotAccessPaymentAdministration() throws Exception {
        mockMvc.perform(get("/admin/payments"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "VERIFICATION_OFFICER")
    void verificationOfficerCannotAccessPaymentAdministration() throws Exception {
        mockMvc.perform(get("/admin/payments"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MARKETING_STAFF")
    void marketingStaffCannotAccessPaymentAdministration() throws Exception {
        mockMvc.perform(get("/admin/payments"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanAccessPaymentAdministration() throws Exception {
        mockMvc.perform(get("/admin/payments"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void staffCanAccessPaymentAdministration() throws Exception {
        mockMvc.perform(get("/admin/payments"))
                .andExpect(status().isOk());
    }

    @Test
    void bookingDetailsExposeRequiredPaymentStates() throws IOException {
        String template = Files.readString(Path.of("src/main/resources/templates/customer/bookings/details.html"));

        assertTrue(template.contains("Proceed to Payment"));
        assertTrue(template.contains("Fully Paid"));
        assertTrue(template.contains("paymentSummary.totalPaid"));
        assertTrue(template.contains("paymentSummary.remainingBalance"));
    }
}
