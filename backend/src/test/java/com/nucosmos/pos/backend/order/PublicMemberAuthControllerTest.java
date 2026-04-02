package com.nucosmos.pos.backend.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nucosmos.pos.backend.auth.PhoneVerificationResult;
import com.nucosmos.pos.backend.auth.PhoneVerificationService;
import com.nucosmos.pos.backend.auth.TestLoginSupport;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PublicMemberAuthControllerTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PhoneVerificationService phoneVerificationService;

    @Test
    void shouldLoginMemberViaSmsAndExposeSession() throws Exception {
        when(phoneVerificationService.verifyPhoneNumber(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString()
        )).thenReturn(new PhoneVerificationResult("+886936993623", "firebase-member-001"));

        MvcResult loginResult = mockMvc.perform(post("/api/v1/public/member/login/sms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "測試會員",
                                  "firebaseIdToken": "firebase-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("nucosmos_member_session=")))
                .andExpect(jsonPath("$.data.authenticated").value(true))
                .andExpect(jsonPath("$.data.member.displayName").value("測試會員"))
                .andExpect(jsonPath("$.data.member.phoneNumber").value("+886936993623"))
                .andReturn();

        String cookie = loginResult.getResponse().getHeader(HttpHeaders.SET_COOKIE);

        mockMvc.perform(get("/api/v1/public/member/session")
                        .header(HttpHeaders.COOKIE, cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.authenticated").value(true))
                .andExpect(jsonPath("$.data.member.displayName").value("測試會員"))
                .andExpect(jsonPath("$.data.member.phoneNumber").value("+886936993623"));
    }

    @Test
    void shouldClaimReceiptUsingLoggedInMemberSession() throws Exception {
        when(phoneVerificationService.verifyPhoneNumber(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString()
        )).thenReturn(new PhoneVerificationResult("+886936993623", "firebase-member-002"));

        MvcResult loginResult = mockMvc.perform(post("/api/v1/public/member/login/sms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "會員小玉",
                                  "firebaseIdToken": "firebase-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String cookie = loginResult.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        String token = cashierToken();
        RedeemTicket ticket = createPaidOrder(token);

        mockMvc.perform(post("/api/v1/public/redeem/{token}/claim", ticket.token())
                        .header(HttpHeaders.COOKIE, cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.claimed").value(true))
                .andExpect(jsonPath("$.data.member.displayName").value("會員小玉"))
                .andExpect(jsonPath("$.data.member.phoneNumber").value("+886936993623"));
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

        return new RedeemTicket(redeemToken);
    }

    private record RedeemTicket(String token) {
    }
}
