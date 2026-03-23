package com.nucosmos.pos.backend.report;

import com.nucosmos.pos.backend.auth.TestLoginSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReportAndShiftControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldOpenAndCloseShiftWithCalculatedTotals() throws Exception {
        String cashierToken = TestLoginSupport.loginAndExtractToken(mockMvc, """
                {
                  "storeCode": "TW001",
                  "roleCode": "CASHIER",
                  "pin": "1234",
                  "deviceCode": "POS-TABLET-001"
                }
                """);

        MvcResult openResult = mockMvc.perform(post("/api/v1/shifts/open")
                        .header("Authorization", "Bearer " + cashierToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "openingCashAmount": 100.00,
                                  "note": "Start morning shift"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.openingCashAmount").value(100.0))
                .andReturn();

        UUID shiftId = TestLoginSupport.extractDataFieldAsUuid(openResult, "id");

        MvcResult cashOrderResult = createOrder(cashierToken, "44444444-4444-4444-4444-444444444441", 1);
        UUID cashOrderId = TestLoginSupport.extractDataFieldAsUuid(cashOrderResult, "id");
        payCash(cashierToken, cashOrderId, "8.50", "10.00");

        MvcResult cardOrderResult = createOrder(cashierToken, "44444444-4444-4444-4444-444444444443", 1);
        UUID cardOrderId = TestLoginSupport.extractDataFieldAsUuid(cardOrderResult, "id");
        UUID paymentId = authorizeAndCaptureCard(cashierToken, cardOrderId, "5.25");

        refundCard(cashierToken, cardOrderId, paymentId, "1.25");

        mockMvc.perform(get("/api/v1/shifts/current")
                        .header("Authorization", "Bearer " + cashierToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(shiftId.toString()))
                .andExpect(jsonPath("$.data.status").value("OPEN"));

        mockMvc.perform(post("/api/v1/shifts/{shiftId}/close", shiftId)
                        .header("Authorization", "Bearer " + cashierToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "closingCashAmount": 108.50,
                                  "note": "End morning shift"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"))
                .andExpect(jsonPath("$.data.orderCount").value(2))
                .andExpect(jsonPath("$.data.voidedOrderCount").value(0))
                .andExpect(jsonPath("$.data.cashSalesAmount").value(8.5))
                .andExpect(jsonPath("$.data.cardSalesAmount").value(5.25))
                .andExpect(jsonPath("$.data.refundedAmount").value(1.25))
                .andExpect(jsonPath("$.data.netSalesAmount").value(12.5))
                .andExpect(jsonPath("$.data.expectedCashAmount").value(108.5))
                .andExpect(jsonPath("$.data.closingCashAmount").value(108.5));
    }

    @Test
    void shouldReturnSalesSummaryForManager() throws Exception {
        String cashierToken = TestLoginSupport.loginAndExtractToken(mockMvc, """
                {
                  "storeCode": "TW001",
                  "roleCode": "CASHIER",
                  "pin": "1234",
                  "deviceCode": "POS-TABLET-001"
                }
                """);
        String managerToken = TestLoginSupport.loginAndExtractToken(mockMvc, """
                {
                  "storeCode": "TW001",
                  "roleCode": "MANAGER",
                  "pin": "9999",
                  "deviceCode": "POS-TABLET-001"
                }
                """);

        UUID orderId = TestLoginSupport.extractDataFieldAsUuid(createOrder(cashierToken, "44444444-4444-4444-4444-444444444442", 2), "id");
        payCash(cashierToken, orderId, "13.50", "20.00");

        OffsetDateTime now = OffsetDateTime.now();
        String from = now.minusDays(1).toString();
        String to = now.plusDays(1).toString();

        mockMvc.perform(get("/api/v1/reports/sales-summary")
                        .header("Authorization", "Bearer " + managerToken)
                        .param("from", from)
                        .param("to", to))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.storeCode").value("TW001"))
                .andExpect(jsonPath("$.data.orderCount").value(1))
                .andExpect(jsonPath("$.data.voidedOrderCount").value(0))
                .andExpect(jsonPath("$.data.grossSalesAmount").value(13.5))
                .andExpect(jsonPath("$.data.refundedAmount").value(0.0))
                .andExpect(jsonPath("$.data.netSalesAmount").value(13.5))
                .andExpect(jsonPath("$.data.cashSalesAmount").value(13.5))
                .andExpect(jsonPath("$.data.cardSalesAmount").value(0.0))
                .andExpect(jsonPath("$.data.averageOrderAmount").value(13.5));
    }

    @Test
    void shouldRejectCashierFromViewingSalesSummary() throws Exception {
        String cashierToken = TestLoginSupport.loginAndExtractToken(mockMvc, """
                {
                  "storeCode": "TW001",
                  "roleCode": "CASHIER",
                  "pin": "1234",
                  "deviceCode": "POS-TABLET-001"
                }
                """);

        OffsetDateTime now = OffsetDateTime.now();
        String from = now.minusDays(1).toString();
        String to = now.plusDays(1).toString();

        mockMvc.perform(get("/api/v1/reports/sales-summary")
                        .header("Authorization", "Bearer " + cashierToken)
                        .param("from", from)
                        .param("to", to))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnInventoryAnalyticsForManager() throws Exception {
        String managerToken = TestLoginSupport.loginAndExtractToken(mockMvc, """
                {
                  "storeCode": "TW001",
                  "roleCode": "MANAGER",
                  "pin": "9999",
                  "deviceCode": "POS-TABLET-001"
                }
                """);

        OffsetDateTime now = OffsetDateTime.now();
        String from = now.minusDays(1).toString();
        String to = now.plusDays(1).toString();

        mockMvc.perform(get("/api/v1/reports/inventory-analytics")
                        .header("Authorization", "Bearer " + managerToken)
                        .param("from", from)
                        .param("to", to))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.storeCode").value("TW001"))
                .andExpect(jsonPath("$.data.summary.productSkuCount").isNumber())
                .andExpect(jsonPath("$.data.summary.materialSkuCount").isNumber())
                .andExpect(jsonPath("$.data.summary.packagingSkuCount").isNumber())
                .andExpect(jsonPath("$.data.lowStockProducts").isArray())
                .andExpect(jsonPath("$.data.productMovementTotals").isArray())
                .andExpect(jsonPath("$.data.materialConsumption").isArray())
                .andExpect(jsonPath("$.data.defectiveAndWaste").isArray());
    }

    @Test
    void shouldReturnSalesTrendForManager() throws Exception {
        String cashierToken = TestLoginSupport.loginAndExtractToken(mockMvc, """
                {
                  "storeCode": "TW001",
                  "roleCode": "CASHIER",
                  "pin": "1234",
                  "deviceCode": "POS-TABLET-001"
                }
                """);
        String managerToken = TestLoginSupport.loginAndExtractToken(mockMvc, """
                {
                  "storeCode": "TW001",
                  "roleCode": "MANAGER",
                  "pin": "9999",
                  "deviceCode": "POS-TABLET-001"
                }
                """);

        UUID orderId = TestLoginSupport.extractDataFieldAsUuid(createOrder(cashierToken, "44444444-4444-4444-4444-444444444441", 1), "id");
        payCash(cashierToken, orderId, "8.50", "10.00");

        OffsetDateTime now = OffsetDateTime.now();
        String from = now.minusDays(2).toString();
        String to = now.plusDays(1).toString();

        mockMvc.perform(get("/api/v1/reports/sales-trend")
                        .header("Authorization", "Bearer " + managerToken)
                        .param("from", from)
                        .param("to", to))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.storeCode").value("TW001"))
                .andExpect(jsonPath("$.data.granularity").value("DAY"))
                .andExpect(jsonPath("$.data.points").isArray())
                .andExpect(jsonPath("$.data.points.length()").value(4))
                .andExpect(jsonPath("$.data.points[0].bucketLabel").exists())
                .andExpect(jsonPath("$.data.points[0].netSalesAmount").isNumber());
    }

    private MvcResult createOrder(String token, String productId, int quantity) throws Exception {
        return mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    {
                                      "productId": "%s",
                                      "quantity": %d
                                    }
                                  ]
                                }
                                """.formatted(productId, quantity)))
                .andExpect(status().isOk())
                .andReturn();
    }

    private void payCash(String token, UUID orderId, String amount, String amountReceived) throws Exception {
        mockMvc.perform(post("/api/v1/orders/{orderId}/payments", orderId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentMethod": "CASH",
                                  "amount": %s,
                                  "amountReceived": %s
                                }
                                """.formatted(amount, amountReceived)))
                .andExpect(status().isOk());
    }

    private UUID authorizeAndCaptureCard(String token, UUID orderId, String amount) throws Exception {
        MvcResult authorizeResult = mockMvc.perform(post("/api/v1/orders/{orderId}/payments/authorize", orderId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": %s
                                }
                                """.formatted(amount)))
                .andExpect(status().isOk())
                .andReturn();

        UUID paymentId = TestLoginSupport.extractDataArrayFieldAsUuid(authorizeResult, "payments", 0, "id");

        mockMvc.perform(post("/api/v1/orders/{orderId}/payments/{paymentId}/capture", orderId, paymentId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "note": "Capture card"
                                }
                                """))
                .andExpect(status().isOk());

        return paymentId;
    }

    private void refundCard(String token, UUID orderId, UUID paymentId, String amount) throws Exception {
        mockMvc.perform(post("/api/v1/orders/{orderId}/refunds", orderId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentId": "%s",
                                  "refundMethod": "CARD_REFUND",
                                  "amount": %s,
                                  "reason": "Partial refund"
                                }
                                """.formatted(paymentId, amount)))
                .andExpect(status().isOk());
    }
}
