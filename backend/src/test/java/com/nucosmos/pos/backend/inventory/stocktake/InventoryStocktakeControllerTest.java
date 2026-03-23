package com.nucosmos.pos.backend.inventory.stocktake;

import com.nucosmos.pos.backend.auth.TestLoginSupport;
import com.nucosmos.pos.backend.inventory.persistence.InventoryStockEntity;
import com.nucosmos.pos.backend.inventory.repository.InventoryStockRepository;
import com.nucosmos.pos.backend.store.persistence.StoreEntity;
import com.nucosmos.pos.backend.store.repository.StoreRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InventoryStocktakeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private InventoryStockRepository inventoryStockRepository;

    @Test
    void shouldCreateStocktakeAndAdjustInventory() throws Exception {
        String token = TestLoginSupport.loginAndExtractToken(mockMvc, """
                {
                  "storeCode": "TW001",
                  "roleCode": "MANAGER",
                  "pin": "9999",
                  "deviceCode": "POS-TABLET-001"
                }
                """);

        StoreEntity store = storeRepository.findByCodeAndStatus("TW001", "ACTIVE").orElseThrow();
        UUID productId = UUID.fromString("44444444-4444-4444-4444-444444444441");
        InventoryStockEntity stockBefore = inventoryStockRepository.findByStore_IdAndProduct_Id(store.getId(), productId).orElseThrow();
        int expectedBefore = stockBefore.getSellableQuantity();

        mockMvc.perform(post("/api/v1/admin/inventory/stocktakes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "note": "Cycle count",
                                  "items": [
                                    {
                                      "productId": "44444444-4444-4444-4444-444444444441",
                                      "countedSellableQuantity": %d,
                                      "reasonCode": "COUNT_VARIANCE",
                                      "note": "Found one extra cup"
                                    }
                                  ]
                                }
                                """.formatted(expectedBefore + 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("POSTED"))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].expectedSellableQuantity").value(expectedBefore))
                .andExpect(jsonPath("$.data.items[0].countedSellableQuantity").value(expectedBefore + 1))
                .andExpect(jsonPath("$.data.items[0].varianceQuantity").value(1));

        InventoryStockEntity stockAfter = inventoryStockRepository.findByStore_IdAndProduct_Id(store.getId(), productId).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(stockAfter.getSellableQuantity()).isEqualTo(expectedBefore + 1);
    }
}
