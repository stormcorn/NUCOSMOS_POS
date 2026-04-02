package com.nucosmos.pos.backend.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nucosmos.pos.backend.auth.TestLoginSupport;
import com.nucosmos.pos.backend.order.persistence.ReceiptPrizeEntity;
import com.nucosmos.pos.backend.order.repository.ReceiptPrizeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PublicRedeemControllerTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReceiptPrizeRepository receiptPrizeRepository;

    @Test
    void shouldServePublicRedeemEntryPage() throws Exception {
        mockMvc.perform(get("/redeem/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<!doctype html>")))
                .andExpect(content().string(containsString("requestJson")));
    }

    @Test
    void shouldLookupAndClaimReceiptByTokenAsLoseFlow() throws Exception {
        String token = cashierToken();
        RedeemTicket ticket = createPaidOrder(token);

        mockMvc.perform(get("/api/v1/public/redeem/{token}", ticket.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.claimCode").value(ticket.claimCode()))
                .andExpect(jsonPath("$.data.claimed").value(false))
                .andExpect(jsonPath("$.data.claimable").value(true))
                .andExpect(jsonPath("$.data.redeemUrl").value(containsString("/redeem/")))
                .andExpect(jsonPath("$.data.member").doesNotExist())
                .andExpect(jsonPath("$.data.availablePrizes").isArray())
                .andExpect(jsonPath("$.data.shareCouponHint").isNotEmpty());

        mockMvc.perform(get("/api/v1/public/redeem/search").param("code", ticket.claimCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value(ticket.token()))
                .andExpect(jsonPath("$.data.claimCode").value(ticket.claimCode()));

        mockMvc.perform(post("/api/v1/public/redeem/{token}/claim", ticket.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "王小美",
                                  "phoneNumber": "0936993623"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.claimed").value(true))
                .andExpect(jsonPath("$.data.claimable").value(false))
                .andExpect(jsonPath("$.data.claimedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.member.displayName").value("王小美"))
                .andExpect(jsonPath("$.data.member.phoneNumber").value("+886936993623"))
                .andExpect(jsonPath("$.data.member.pointBalance").value(1))
                .andExpect(jsonPath("$.data.member.totalClaims").value(1))
                .andExpect(jsonPath("$.data.rewards.awardedPoints").value(1))
                .andExpect(jsonPath("$.data.rewards.pointsBalance").value(1))
                .andExpect(jsonPath("$.data.rewards.nextCouponThreshold").value(5))
                .andExpect(jsonPath("$.data.rewards.nextCouponAmount").value(50.00))
                .andExpect(jsonPath("$.data.rewards.issuedCoupon").doesNotExist())
                .andExpect(jsonPath("$.data.draw.outcome").value("LOSE"))
                .andExpect(jsonPath("$.data.draw.won").value(false))
                .andExpect(jsonPath("$.data.draw.title").value("銘謝惠顧，再接再厲"))
                .andExpect(jsonPath("$.data.draw.prize").doesNotExist());
    }

    @Test
    void shouldAwardPrizeWhenProbabilityGuaranteesWin() throws Exception {
        receiptPrizeRepository.save(new ReceiptPrizeEntity(
                "免費升級大杯",
                "請向店員出示中獎畫面兌領",
                new BigDecimal("100.00"),
                3,
                true,
                0
        ));

        String token = cashierToken();
        RedeemTicket ticket = createPaidOrder(token);

        mockMvc.perform(post("/api/v1/public/redeem/{token}/claim", ticket.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "王小美",
                                  "phoneNumber": "0936993623"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.claimed").value(true))
                .andExpect(jsonPath("$.data.member.pointBalance").value(0))
                .andExpect(jsonPath("$.data.rewards.awardedPoints").value(0))
                .andExpect(jsonPath("$.data.draw.outcome").value("WIN"))
                .andExpect(jsonPath("$.data.draw.won").value(true))
                .andExpect(jsonPath("$.data.draw.prize.name").value("免費升級大杯"));
    }

    private String cashierToken() throws Exception {
        return TestLoginSupport.loginAndExtractToken(mockMvc, """
                {
                  "storeCode": "TW001",
                  "roleCode": "CASHIER",
                  "pin": "123456",
                  "deviceCode": "POS-TABLET-001"
                }
                """);
    }

    private RedeemTicket createPaidOrder(String token) throws Exception {
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

        return new RedeemTicket(redeemToken, redeemCode);
    }

    private record RedeemTicket(String token, String claimCode) {
    }
}
