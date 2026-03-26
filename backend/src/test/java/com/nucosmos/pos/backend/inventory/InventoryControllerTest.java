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
                  "pin": "999999",
                  "deviceCode": "POS-TABLET-001"
                }
                """);

        mockMvc.perform(get("/api/v1/admin/inventory/stocks")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(5))
                .andExpect(jsonPath("$.data[0].sku").value("drink-003"))
                .andExpect(jsonPath("$.data[0].sellableQuantity").value(16))
                .andExpect(jsonPath("$.data[0].defectiveQuantity").value(0))
                .andExpect(jsonPath("$.data[0].quantityOnHand").value(16));

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
                .andExpect(jsonPath("$.data.stockBucket").value("SELLABLE"))
                .andExpect(jsonPath("$.data.quantityDelta").value(4))
                .andExpect(jsonPath("$.data.quantityAfter").value(20))
                .andExpect(jsonPath("$.data.sellableQuantityAfter").value(20))
                .andExpect(jsonPath("$.data.defectiveQuantityAfter").value(0));

        mockMvc.perform(post("/api/v1/admin/inventory/movements")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "44444444-4444-4444-4444-444444444443",
                                  "movementType": "REFUND_DEFECT",
                                  "quantity": 2,
                                  "note": "Customer returned damaged items"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.movementType").value("REFUND_DEFECT"))
                .andExpect(jsonPath("$.data.stockBucket").value("DEFECTIVE"))
                .andExpect(jsonPath("$.data.quantityDelta").value(2))
                .andExpect(jsonPath("$.data.sellableQuantityDelta").value(0))
                .andExpect(jsonPath("$.data.defectiveQuantityDelta").value(2))
                .andExpect(jsonPath("$.data.quantityAfter").value(22))
                .andExpect(jsonPath("$.data.sellableQuantityAfter").value(20))
                .andExpect(jsonPath("$.data.defectiveQuantityAfter").value(2));

        mockMvc.perform(post("/api/v1/admin/inventory/movements")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "44444444-4444-4444-4444-444444444443",
                                  "movementType": "SCRAP_OUT",
                                  "quantity": 1,
                                  "note": "Discarded damaged stock"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.movementType").value("SCRAP_OUT"))
                .andExpect(jsonPath("$.data.stockBucket").value("DEFECTIVE"))
                .andExpect(jsonPath("$.data.quantityDelta").value(-1))
                .andExpect(jsonPath("$.data.sellableQuantityAfter").value(20))
                .andExpect(jsonPath("$.data.defectiveQuantityAfter").value(1))
                .andExpect(jsonPath("$.data.quantityAfter").value(21));

        mockMvc.perform(get("/api/v1/admin/inventory/movements")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].movementType").value("SCRAP_OUT"))
                .andExpect(jsonPath("$.data[0].sku").value("drink-003"));

        mockMvc.perform(get("/api/v1/admin/inventory/stocks")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].sellableQuantity").value(20))
                .andExpect(jsonPath("$.data[0].defectiveQuantity").value(1))
                .andExpect(jsonPath("$.data[0].quantityOnHand").value(21))
                .andExpect(jsonPath("$.data[0].lowStock").value(false));
    }

    @Test
    void shouldRejectMovementWhenStockWouldGoNegative() throws Exception {
        String managerToken = TestLoginSupport.loginAndExtractToken(mockMvc, """
                {
                  "storeCode": "TW001",
                  "roleCode": "MANAGER",
                  "pin": "999999",
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

    @Test
    void shouldRejectScrapOutWhenDefectiveStockWouldGoNegative() throws Exception {
        String managerToken = TestLoginSupport.loginAndExtractToken(mockMvc, """
                {
                  "storeCode": "TW001",
                  "roleCode": "MANAGER",
                  "pin": "999999",
                  "deviceCode": "POS-TABLET-001"
                }
                """);

        mockMvc.perform(post("/api/v1/admin/inventory/movements")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "44444444-4444-4444-4444-444444444441",
                                  "movementType": "SCRAP_OUT",
                                  "quantity": 1,
                                  "note": "Should fail because no defective stock"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Insufficient stock for this movement"));
    }
}
