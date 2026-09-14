package com.landhub.marketing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MarketingAccessAndUiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanManageMarketing() throws Exception {
        mockMvc.perform(get("/marketing")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MARKETING_STAFF")
    void marketingStaffCanManageMarketing() throws Exception {
        mockMvc.perform(get("/marketing")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void staffCannotManageMarketing() throws Exception {
        mockMvc.perform(get("/marketing")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "VERIFICATION_OFFICER")
    void verificationOfficerCannotManageMarketing() throws Exception {
        mockMvc.perform(get("/marketing")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerCannotManageMarketing() throws Exception {
        mockMvc.perform(get("/marketing")).andExpect(status().isForbidden());
    }

    @Test
    void publicPromotionPagesAndIntegrationsContainExpectedContracts() throws IOException {
        String promotionList = Files.readString(Path.of("src/main/resources/templates/promotions/list.html"));
        String landList = Files.readString(Path.of("src/main/resources/templates/lands.html"));
        String landDetails = Files.readString(Path.of("src/main/resources/templates/land-details.html"));

        assertTrue(promotionList.contains("promotionalPrice"));
        assertTrue(landList.contains("promotionsByLand"));
        assertTrue(landDetails.contains("promotion.endDate"));
    }

    @Test
    void publicPromotionAndLandPagesRender() throws Exception {
        mockMvc.perform(get("/promotions")).andExpect(status().isOk());
        mockMvc.perform(get("/lands")).andExpect(status().isOk());
        mockMvc.perform(get("/")).andExpect(status().isOk());
    }
}
