package com.nucosmos.pos.backend.auth.admin;

import com.nucosmos.pos.backend.auth.TestLoginSupport;
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

    @Test
    void shouldListUsersRolesAndPermissions() throws Exception {
        String managerToken = managerToken();

        mockMvc.perform(get("/api/v1/admin/access/users")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].employeeCode").exists())
                .andExpect(jsonPath("$.data[0].roleCodes").isArray());

        mockMvc.perform(get("/api/v1/admin/access/roles")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].code").exists())
                .andExpect(jsonPath("$.data[0].permissionKeys").isArray());

        mockMvc.perform(get("/api/v1/admin/access/permissions")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].key").exists())
                .andExpect(jsonPath("$.data[0].groupName").exists());
    }

    @Test
    void shouldCreateAndUpdateUser() throws Exception {
        String managerToken = managerToken();

        MvcResult createResult = mockMvc.perform(post("/api/v1/admin/access/users")
                        .header("Authorization", "Bearer " + managerToken)
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
                        .header("Authorization", "Bearer " + managerToken)
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
    void shouldCreateAndUpdateRolePermissions() throws Exception {
        String managerToken = managerToken();

        MvcResult createResult = mockMvc.perform(post("/api/v1/admin/access/roles")
                        .header("Authorization", "Bearer " + managerToken)
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
                        .header("Authorization", "Bearer " + managerToken)
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
    void shouldAuthorizeUsingSelectedActiveRoleForMultiRoleUser() throws Exception {
        String cashierToken = TestLoginSupport.loginAndExtractToken(mockMvc, """
                {
                  "storeCode": "TW001",
                  "roleCode": "CASHIER",
                  "pin": "567890",
                  "deviceCode": "POS-TABLET-001"
                }
                """);

        String managerToken = TestLoginSupport.loginAndExtractToken(mockMvc, """
                {
                  "storeCode": "TW001",
                  "roleCode": "MANAGER",
                  "pin": "567890",
                  "deviceCode": "POS-TABLET-001"
                }
                """);

        mockMvc.perform(get("/api/v1/admin/access/users")
                        .header("Authorization", "Bearer " + cashierToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/access/users")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk());
    }

    private String managerToken() throws Exception {
        return TestLoginSupport.loginAndExtractToken(mockMvc, """
                {
                  "storeCode": "TW001",
                  "roleCode": "MANAGER",
                  "pin": "999999",
                  "deviceCode": "POS-TABLET-001"
                }
                """);
    }
}
