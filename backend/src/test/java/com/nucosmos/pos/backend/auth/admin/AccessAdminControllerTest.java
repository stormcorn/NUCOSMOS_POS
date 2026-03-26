package com.nucosmos.pos.backend.auth.admin;

import com.nucosmos.pos.backend.auth.TestLoginSupport;
import com.nucosmos.pos.backend.auth.repository.PhoneRegistrationRequestRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AccessAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PhoneRegistrationRequestRepository phoneRegistrationRequestRepository;

    @Test
    void shouldListUsersRolesAndPermissions() throws Exception {
        String adminToken = adminToken();

        mockMvc.perform(get("/api/v1/admin/access/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].employeeCode").exists())
                .andExpect(jsonPath("$.data[0].roleCodes").isArray());

        mockMvc.perform(get("/api/v1/admin/access/roles")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].code").exists())
                .andExpect(jsonPath("$.data[0].permissionKeys").isArray());

        mockMvc.perform(get("/api/v1/admin/access/permissions")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].key").exists())
                .andExpect(jsonPath("$.data[0].groupName").exists());
    }

    @Test
    void shouldCreateAndUpdateUser() throws Exception {
        String adminToken = adminToken();

        MvcResult createResult = mockMvc.perform(post("/api/v1/admin/access/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "employeeCode": "EMP-NEW-001",
                                  "displayName": "Diana Operator",
                                  "pin": "246810",
                                  "status": "ACTIVE",
                                  "roleCodes": ["CASHIER"],
                                  "storeCodes": ["TW001"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.employeeCode").value("EMP-NEW-001"))
                .andExpect(jsonPath("$.data.displayName").value("Diana Operator"))
                .andExpect(jsonPath("$.data.roleCodes[0]").value("CASHIER"))
                .andReturn();

        UUID userId = TestLoginSupport.extractDataFieldAsUuid(createResult, "id");

        mockMvc.perform(put("/api/v1/admin/access/users/{userId}", userId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "employeeCode": "EMP-NEW-001",
                                  "displayName": "Diana Supervisor",
                                  "pin": "",
                                  "status": "INACTIVE",
                                  "roleCodes": ["MANAGER"],
                                  "storeCodes": ["TW001"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("Diana Supervisor"))
                .andExpect(jsonPath("$.data.status").value("INACTIVE"))
                .andExpect(jsonPath("$.data.roleCodes[0]").value("MANAGER"));
    }

    @Test
    void shouldClearPendingPhoneRegistrations() throws Exception {
        String adminToken = adminToken();

        mockMvc.perform(post("/api/v1/auth/register/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeCode": "TW001",
                                  "phoneNumber": "0912345682",
                                  "pin": "654321"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/access/phone-registrations/clear-pending")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phoneNumber": "0912345682"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phoneNumber").value("+886912345682"))
                .andExpect(jsonPath("$.data.clearedCount").value(1))
                .andExpect(jsonPath("$.data.clearedStatus").value("EXPIRED"));

        org.assertj.core.api.Assertions.assertThat(
                        phoneRegistrationRequestRepository.findAllByPhoneNumberAndStatusIn(
                                "+886912345682",
                                java.util.List.of("PENDING_VERIFICATION")
                        ))
                .isEmpty();
    }

    @Test
    void shouldCreateAndUpdateRolePermissions() throws Exception {
        String adminToken = adminToken();

        MvcResult createResult = mockMvc.perform(post("/api/v1/admin/access/roles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "SUPPORT",
                                  "name": "Support",
                                  "description": "Support role",
                                  "active": true,
                                  "permissionKeys": ["DASHBOARD_VIEW", "ORDERS_VIEW", "USERS_VIEW"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("SUPPORT"))
                .andExpect(jsonPath("$.data.permissionKeys.length()").value(3))
                .andReturn();

        UUID roleId = TestLoginSupport.extractDataFieldAsUuid(createResult, "id");

        mockMvc.perform(put("/api/v1/admin/access/roles/{roleId}", roleId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "SUPPORT",
                                  "name": "Support Updated",
                                  "description": "Updated role",
                                  "active": false,
                                  "permissionKeys": ["DASHBOARD_VIEW"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Support Updated"))
                .andExpect(jsonPath("$.data.active").value(false))
                .andExpect(jsonPath("$.data.permissionKeys.length()").value(1))
                .andExpect(jsonPath("$.data.permissionKeys[0]").value("DASHBOARD_VIEW"));
    }

    @Test
    void shouldRestrictAccessManagementToAdminRole() throws Exception {
        String cashierToken = TestLoginSupport.loginAndExtractToken(mockMvc, """
                {
                  "storeCode": "TW001",
                  "roleCode": "CASHIER",
                  "pin": "123456",
                  "deviceCode": "POS-TABLET-001"
                }
                """);

        String managerToken = TestLoginSupport.loginAndExtractToken(mockMvc, """
                {
                  "storeCode": "TW001",
                  "roleCode": "MANAGER",
                  "pin": "999999",
                  "deviceCode": "POS-TABLET-001"
                }
                """);

        mockMvc.perform(get("/api/v1/admin/access/users")
                        .header("Authorization", "Bearer " + cashierToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/access/users")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/access/users")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk());
    }

    private String adminToken() throws Exception {
        return TestLoginSupport.loginAndExtractToken(mockMvc, """
                {
                  "storeCode": "TW001",
                  "roleCode": "ADMIN",
                  "pin": "654265",
                  "deviceCode": "POS-TABLET-001"
                }
                """);
    }
}
