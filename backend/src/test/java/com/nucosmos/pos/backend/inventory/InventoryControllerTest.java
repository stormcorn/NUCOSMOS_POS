package com.nucosmos.pos.backend.inventory;

import com.nucosmos.pos.backend.auth.TestLoginSupport;
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
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAllowManagerToManageInventory() throws Exception {
        String managerToken = TestLoginSupport.loginAndExtractToken(mockMvc, """
                {
                  "storeCode": "TW001",
                  "roleCode": "MANAGER",
                  "pin": "9999",
                  "deviceCode": "POS-TABLET-001"
                }
                """);

        mockMvc.perform(get("/api/v1/admin/inventory/stocks")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(5))
                .andExpect(jsonPath("$.data[0].sku").value("drink-003"));

        mockMvc.perform(put("/api/v1/admin/inventory/stocks/{productId}/reorder-level", "44444444-4444-4444-4444-444444444443")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reorderLevel": 18
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reorderLevel").value(18))
                .andExpect(jsonPath("$.data.lowStock").value(true));

        mockMvc.perform(post("/api/v1/admin/inventory/movements")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "44444444-4444-4444-4444-444444444443",
                                  "movementType": "PURCHASE_IN",
                                  "quantity": 4,
                                  "unitCost": 2.50,
                                  "note": "Weekly restock"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.movementType").value("PURCHASE_IN"))
                .andExpect(jsonPath("$.data.quantityAfter").value(20));

        mockMvc.perform(get("/api/v1/admin/inventory/movements")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].movementType").value("PURCHASE_IN"))
                .andExpect(jsonPath("$.data[0].sku").value("drink-003"));
    }

    @Test
    void shouldRejectMovementWhenStockWouldGoNegative() throws Exception {
        String managerToken = TestLoginSupport.loginAndExtractToken(mockMvc, """
                {
                  "storeCode": "TW001",
                  "roleCode": "MANAGER",
                  "pin": "9999",
                  "deviceCode": "POS-TABLET-001"
                }
                """);

        mockMvc.perform(post("/api/v1/admin/inventory/movements")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "44444444-4444-4444-4444-444444444445",
                                  "movementType": "SALE_OUT",
                                  "quantity": 999,
                                  "note": "Should fail"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Insufficient stock for this movement"));
    }
}
