package com.nucosmos.pos.backend.auth;

import com.nucosmos.pos.backend.auth.repository.PhoneRegistrationRequestRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PhoneRegistrationRequestRepository phoneRegistrationRequestRepository;

    @Test
    void shouldLoginWithValidPin() throws Exception {
        mockMvc.perform(post("/api/v1/auth/pin-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeCode": "TW001",
                                  "roleCode": "CASHIER",
                                  "pin": "123456",
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
    void shouldInferRoleFromPinWhenRoleNotProvided() throws Exception {
        mockMvc.perform(post("/api/v1/auth/pin-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeCode": "TW001",
                                  "pin": "567890",
                                  "deviceCode": "AUTO-TABLET-001",
                                  "deviceName": "Galaxy Tab Demo",
                                  "devicePlatform": "ANDROID"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.deviceCode").value("AUTO-TABLET-001"))
                .andExpect(jsonPath("$.data.staff.employeeCode").value("EMP-SUPERVISOR-001"))
                .andExpect(jsonPath("$.data.staff.activeRole").value("MANAGER"));
    }

    @Test
    void shouldListAvailableStoresWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/stores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].code").value("TW001"))
                .andExpect(jsonPath("$.data[0].name").value("NUCOSMOS Demo Store"));
    }

    @Test
    void shouldStartPhoneRegistrationWithSixDigitPin() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeCode": "TW001",
                                  "phoneNumber": "+886912345679",
                                  "pin": "654321"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.registrationId").isString())
                .andExpect(jsonPath("$.data.storeCode").value("TW001"))
                .andExpect(jsonPath("$.data.phoneNumber").value("+886912345679"))
                .andExpect(jsonPath("$.data.provider").value("FIREBASE_SMS"))
                .andExpect(jsonPath("$.data.status").value("PENDING_VERIFICATION"));
    }

    @Test
    void shouldRejectShortRegistrationPin() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeCode": "TW001",
                                  "phoneNumber": "+886912345680",
                                  "pin": "1234"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void shouldAllowRestartingRegistrationWhenPreviousRequestExpired() throws Exception {
        String payload = """
                {
                  "storeCode": "TW001",
                  "phoneNumber": "+886912345681",
                  "pin": "654321"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        var existingRequest = phoneRegistrationRequestRepository.findAllByPhoneNumberAndStatusIn(
                "+886912345681",
                List.of("PENDING_VERIFICATION")
        ).get(0);
        existingRequest.setExpiresAt(OffsetDateTime.now().minusMinutes(1));

        mockMvc.perform(post("/api/v1/auth/register/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phoneNumber").value("+886912345681"));
    }

    @Test
    void shouldRejectInvalidPin() throws Exception {
        mockMvc.perform(post("/api/v1/auth/pin-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeCode": "TW001",
                                  "roleCode": "CASHIER",
                                  "pin": "000000",
                                  "deviceCode": "POS-TABLET-001"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void shouldThrottleRepeatedInvalidPinAttempts() throws Exception {
        String payload = """
                {
                  "storeCode": "TW001",
                  "pin": "000000",
                  "deviceCode": "POS-TABLET-SECURITY"
                }
                """;

        for (int attempt = 0; attempt < 5; attempt++) {
            mockMvc.perform(post("/api/v1/auth/pin-login")
                            .header("X-Forwarded-For", "203.0.113.10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false));
        }

        mockMvc.perform(post("/api/v1/auth/pin-login")
                        .header("X-Forwarded-For", "203.0.113.10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeCode": "TW001",
                                  "pin": "999999",
                                  "deviceCode": "POS-TABLET-SECURITY"
                                }
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Too many PIN login attempts. Please wait 15 minutes and try again."));
    }

    @Test
    void shouldReturnCurrentSessionWhenTokenPresent() throws Exception {
        String token = TestLoginSupport.loginAndExtractToken(mockMvc, """
                {
                  "storeCode": "TW001",
                  "roleCode": "MANAGER",
                  "pin": "999999",
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
                  "pin": "123456",
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
                  "pin": "999999",
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
