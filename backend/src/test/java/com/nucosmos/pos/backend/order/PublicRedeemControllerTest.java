package com.nucosmos.pos.backend.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PublicRedeemControllerTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldLookupAndClaimReceiptByToken() throws Exception {
        String token = TestLoginSupport.loginAndExtractToken(mockMvc, """
                {
                  "storeCode": "TW001",
                  "roleCode": "CASHIER",
                  "pin": "123456",
                  "deviceCode": "POS-TABLET-001"
                }
                """);

        MvcResult createOrder = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    {
                                      "productId": "44444444-4444-4444-4444-444444444441",
                                      "quantity": 1
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode createdJson = OBJECT_MAPPER.readTree(createOrder.getResponse().getContentAsString());
        String orderId = createdJson.path("data").path("id").asText();
        String redeemUrl = createdJson.path("data").path("redeemUrl").asText();
        String redeemCode = createdJson.path("data").path("redeemCode").asText();
        String redeemToken = redeemUrl.substring(redeemUrl.lastIndexOf('/') + 1);

        mockMvc.perform(post("/api/v1/orders/{orderId}/payments", UUID.fromString(orderId))
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentMethod": "CASH",
                                  "amount": 8.50,
                                  "amountReceived": 8.50
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentStatus").value("PAID"));

        mockMvc.perform(get("/api/v1/public/redeem/{token}", redeemToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.claimCode").value(redeemCode))
                .andExpect(jsonPath("$.data.claimed").value(false))
                .andExpect(jsonPath("$.data.claimable").value(true))
                .andExpect(jsonPath("$.data.redeemUrl").value(containsString("/redeem/")));

        mockMvc.perform(get("/api/v1/public/redeem/search").param("code", redeemCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value(redeemToken))
                .andExpect(jsonPath("$.data.claimCode").value(redeemCode));

        mockMvc.perform(post("/api/v1/public/redeem/{token}/claim", redeemToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.claimed").value(true))
                .andExpect(jsonPath("$.data.claimable").value(false))
                .andExpect(jsonPath("$.data.claimedAt").isNotEmpty());
    }
}
