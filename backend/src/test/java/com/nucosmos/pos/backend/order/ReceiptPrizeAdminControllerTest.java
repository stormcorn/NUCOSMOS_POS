package com.nucosmos.pos.backend.order;

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
class ReceiptPrizeAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCreateUpdateAndDeactivatePrize() throws Exception {
        String adminToken = adminToken();

        MvcResult createResult = mockMvc.perform(post("/api/v1/admin/redeem-prizes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "免費升級大杯",
                                  "description": "請向店員出示畫面",
                                  "probabilityPercent": 10.5,
                                  "remainingQuantity": 30,
                                  "active": true,
                                  "displayOrder": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("免費升級大杯"))
                .andExpect(jsonPath("$.data.probabilityPercent").value(10.5))
                .andReturn();

        UUID prizeId = TestLoginSupport.extractDataFieldAsUuid(createResult, "id");

        mockMvc.perform(put("/api/v1/admin/redeem-prizes/{prizeId}", prizeId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "免費升級大杯",
                                  "description": "更新後說明",
                                  "probabilityPercent": 8.0,
                                  "remainingQuantity": 18,
                                  "active": true,
                                  "displayOrder": 0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.description").value("更新後說明"))
                .andExpect(jsonPath("$.data.remainingQuantity").value(18));

        mockMvc.perform(post("/api/v1/admin/redeem-prizes/{prizeId}/deactivate", prizeId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));

        mockMvc.perform(get("/api/v1/admin/redeem-prizes")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("免費升級大杯"));
    }

    @Test
    void shouldRejectProbabilityOver100Percent() throws Exception {
        String adminToken = adminToken();

        mockMvc.perform(post("/api/v1/admin/redeem-prizes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "頭獎",
                                  "description": "",
                                  "probabilityPercent": 90,
                                  "remainingQuantity": 2,
                                  "active": true,
                                  "displayOrder": 0
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/redeem-prizes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "二獎",
                                  "description": "",
                                  "probabilityPercent": 20,
                                  "remainingQuantity": 3,
                                  "active": true,
                                  "displayOrder": 1
                                }
                                """))
                .andExpect(status().isBadRequest());
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
