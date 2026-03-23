package com.nucosmos.pos.backend.supply;

import com.nucosmos.pos.backend.auth.TestLoginSupport;
import com.nucosmos.pos.backend.supply.persistence.MaterialItemEntity;
import com.nucosmos.pos.backend.supply.persistence.PackagingItemEntity;
import com.nucosmos.pos.backend.supply.repository.MaterialItemRepository;
import com.nucosmos.pos.backend.supply.repository.PackagingItemRepository;
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
class SupplyProcurementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MaterialItemRepository materialItemRepository;

    @Autowired
    private PackagingItemRepository packagingItemRepository;

    @Test
    void shouldManageSuppliersAndReceivePurchaseOrder() throws Exception {
        String token = TestLoginSupport.loginAndExtractToken(mockMvc, """
                {
                  "storeCode": "TW001",
                  "roleCode": "MANAGER",
                  "pin": "9999",
                  "deviceCode": "POS-TABLET-001"
                }
                """);

        MvcResult supplierResult = mockMvc.perform(post("/api/v1/admin/suppliers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "SUP-TW-002",
                                  "name": "North Harbor Supply",
                                  "contactName": "Jason Wu",
                                  "phone": "02-2788-1234",
                                  "email": "jason@example.com",
                                  "note": "Primary procurement vendor"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("SUP-TW-002"))
                .andReturn();

        UUID supplierId = TestLoginSupport.extractDataFieldAsUuid(supplierResult, "id");

        mockMvc.perform(put("/api/v1/admin/suppliers/{supplierId}", supplierId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "SUP-TW-002",
                                  "name": "North Harbor Supply Ltd.",
                                  "contactName": "Jason Wu",
                                  "phone": "02-2788-5678",
                                  "email": "buyer@example.com",
                                  "note": "Updated vendor profile"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("North Harbor Supply Ltd."));

        MvcResult materialCreate = mockMvc.perform(post("/api/v1/admin/materials")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku": "MAT-SUGAR-001",
                                  "name": "Cane Sugar",
                                  "unit": "g",
                                  "purchaseUnit": "bag",
                                  "purchaseToStockRatio": 1000,
                                  "description": "Sugar for drinks",
                                  "reorderLevel": 50,
                                  "latestUnitCost": 0.03
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult packagingCreate = mockMvc.perform(post("/api/v1/admin/packaging-items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku": "PK-STRAW-001",
                                  "name": "Paper Straw",
                                  "unit": "pcs",
                                  "purchaseUnit": "box",
                                  "purchaseToStockRatio": 100,
                                  "specification": "12mm",
                                  "description": "Eco straw",
                                  "reorderLevel": 30,
                                  "latestUnitCost": 0.25
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        UUID materialId = TestLoginSupport.extractDataFieldAsUuid(materialCreate, "id");
        UUID packagingId = TestLoginSupport.extractDataFieldAsUuid(packagingCreate, "id");

        mockMvc.perform(get("/api/v1/admin/replenishment-suggestions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.sku=='MAT-SUGAR-001')]").exists())
                .andExpect(jsonPath("$.data[?(@.sku=='PK-STRAW-001')]").exists());

        MvcResult purchaseOrderResult = mockMvc.perform(post("/api/v1/admin/purchase-orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "supplierId": "%s",
                                  "note": "Weekly replenishment",
                                  "lines": [
                                    {
                                      "itemType": "MATERIAL",
                                      "itemId": "%s",
                                      "orderedQuantity": 120,
                                      "unitCost": 0.03,
                                      "note": "Restock sugar"
                                    },
                                    {
                                      "itemType": "PACKAGING",
                                      "itemId": "%s",
                                      "orderedQuantity": 80,
                                      "unitCost": 0.25,
                                      "note": "Restock straws"
                                    }
                                  ]
                                }
                                """.formatted(supplierId, materialId, packagingId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.lines.length()").value(2))
                .andExpect(jsonPath("$.data.lines[0].unit").value("bag"))
                .andExpect(jsonPath("$.data.lines[0].stockUnit").value("g"))
                .andExpect(jsonPath("$.data.lines[0].purchaseToStockRatio").value(1000))
                .andReturn();

        UUID purchaseOrderId = TestLoginSupport.extractDataFieldAsUuid(purchaseOrderResult, "id");

        mockMvc.perform(post("/api/v1/admin/purchase-orders/{purchaseOrderId}/receive", purchaseOrderId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "note": "Goods received in full"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RECEIVED"))
                .andExpect(jsonPath("$.data.receivedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.lines[0].receivedQuantity").value(120))
                .andExpect(jsonPath("$.data.lines[1].receivedQuantity").value(80))
                .andExpect(jsonPath("$.data.lines[0].receivedStockQuantity").value(120000))
                .andExpect(jsonPath("$.data.lines[1].receivedStockQuantity").value(8000));

        MaterialItemEntity material = materialItemRepository.findById(materialId).orElseThrow();
        PackagingItemEntity packaging = packagingItemRepository.findById(packagingId).orElseThrow();

        org.assertj.core.api.Assertions.assertThat(material.getQuantityOnHand()).isEqualTo(120000);
        org.assertj.core.api.Assertions.assertThat(packaging.getQuantityOnHand()).isEqualTo(8000);
    }
}
