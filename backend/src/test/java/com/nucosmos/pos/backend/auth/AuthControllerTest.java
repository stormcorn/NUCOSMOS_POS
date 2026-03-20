package com.nucosmos.pos.backend.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldLoginWithValidPin() throws Exception {
        mockMvc.perform(post("/api/v1/auth/pin-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeCode": "TW001",
                                  "roleCode": "CASHIER",
                                  "pin": "1234",
                                  "deviceCode": "POS-TABLET-001"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.staff.employeeCode").value("EMP-CASHIER-001"))
                .andExpect(jsonPath("$.data.staff.activeRole").value("CASHIER"));
    }

    @Test
    void shouldRejectInvalidPin() throws Exception {
        mockMvc.perform(post("/api/v1/auth/pin-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeCode": "TW001",
                                  "roleCode": "CASHIER",
                                  "pin": "0000",
                                  "deviceCode": "POS-TABLET-001"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void shouldReturnCurrentSessionWhenTokenPresent() throws Exception {
        String token = TestLoginSupport.loginAndExtractToken(mockMvc, """
                {
                  "storeCode": "TW001",
                  "roleCode": "MANAGER",
                  "pin": "9999",
                  "deviceCode": "POS-TABLET-001"
                }
                """);

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.employeeCode").value("EMP-MANAGER-001"))
                .andExpect(jsonPath("$.data.activeRole").value("MANAGER"));
    }
}
