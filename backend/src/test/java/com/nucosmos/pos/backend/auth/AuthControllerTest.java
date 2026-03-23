package com.nucosmos.pos.backend.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
                .andExpect(jsonPath("$.data.staff.activeRole").value("CASHIER"))
                .andExpect(jsonPath("$.data.staff.permissionKeys").isArray())
                .andExpect(jsonPath("$.data.staff.permissionKeys[0]").value("DASHBOARD_VIEW"));
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
                .andExpect(jsonPath("$.data.activeRole").value("MANAGER"))
                .andExpect(jsonPath("$.data.permissionKeys").isArray())
                .andExpect(jsonPath("$.data.permissionKeys").isNotEmpty());
    }

    @Test
    void shouldReturnEmptyNavigationPreferenceWhenNotSaved() throws Exception {
        String token = TestLoginSupport.loginAndExtractToken(mockMvc, """
                {
                  "storeCode": "TW001",
                  "roleCode": "CASHIER",
                  "pin": "1234",
                  "deviceCode": "POS-TABLET-001"
                }
                """);

        mockMvc.perform(get("/api/v1/auth/preferences/navigation")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rootOrder").isArray())
                .andExpect(jsonPath("$.data.rootOrder").isEmpty())
                .andExpect(jsonPath("$.data.childOrders").isMap());
    }

    @Test
    void shouldSaveNavigationPreference() throws Exception {
        String token = TestLoginSupport.loginAndExtractToken(mockMvc, """
                {
                  "storeCode": "TW001",
                  "roleCode": "MANAGER",
                  "pin": "9999",
                  "deviceCode": "POS-TABLET-001"
                }
                """);

        mockMvc.perform(put("/api/v1/auth/preferences/navigation")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rootOrder": ["/reports", "/", "/products"],
                                  "childOrders": {
                                    "/inventory": ["/inventory/materials", "/inventory/defective"]
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rootOrder[0]").value("/reports"))
                .andExpect(jsonPath("$.data.childOrders['/inventory'][0]").value("/inventory/materials"));

        mockMvc.perform(get("/api/v1/auth/preferences/navigation")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rootOrder[0]").value("/reports"))
                .andExpect(jsonPath("$.data.childOrders['/inventory'][1]").value("/inventory/defective"));
    }
}
