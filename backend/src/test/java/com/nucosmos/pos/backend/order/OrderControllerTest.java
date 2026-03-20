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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCreateOrderForAuthenticatedCashier() throws Exception {
        String token = TestLoginSupport.loginAndExtractToken(mockMvc, """
                {
                  "storeCode": "TW001",
                  "roleCode": "CASHIER",
                  "pin": "1234",
                  "deviceCode": "POS-TABLET-001"
                }
                """);

        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "note": "Lunch rush order",
                                  "items": [
                                    {
                                      "productId": "44444444-4444-4444-4444-444444444441",
                                      "quantity": 2,
                                      "note": "Less ice"
                                    },
                                    {
                                      "productId": "44444444-4444-4444-4444-444444444443",
                                      "quantity": 1
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CREATED"))
                .andExpect(jsonPath("$.data.paymentStatus").value("UNPAID"))
                .andExpect(jsonPath("$.data.storeCode").value("TW001"))
                .andExpect(jsonPath("$.data.deviceCode").value("POS-TABLET-001"))
                .andExpect(jsonPath("$.data.createdByEmployeeCode").value("EMP-CASHIER-001"))
                .andExpect(jsonPath("$.data.itemCount").value(3))
                .andExpect(jsonPath("$.data.subtotalAmount").value(22.25))
                .andExpect(jsonPath("$.data.totalAmount").value(22.25))
                .andExpect(jsonPath("$.data.paidAmount").value(0.00))
                .andExpect(jsonPath("$.data.refundedAmount").value(0.00))
                .andExpect(jsonPath("$.data.payments.length()").value(0))
                .andExpect(jsonPath("$.data.refunds.length()").value(0))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].lineNumber").value(1))
                .andExpect(jsonPath("$.data.items[0].productSku").value("drink-001"))
                .andExpect(jsonPath("$.data.items[0].lineTotalAmount").value(17.00))
                .andExpect(jsonPath("$.data.items[1].productSku").value("drink-003"))
                .andExpect(jsonPath("$.data.items[1].lineTotalAmount").value(5.25));

    }

    @Test
    void shouldRejectOrderWhenProductIsUnknown() throws Exception {
        String token = TestLoginSupport.loginAndExtractToken(mockMvc, """
                {
                  "storeCode": "TW001",
                  "roleCode": "CASHIER",
                  "pin": "1234",
                  "deviceCode": "POS-TABLET-001"
                }
                """);

        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    {
                                      "productId": "99999999-9999-9999-9999-999999999999",
                                      "quantity": 1
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("One or more products are invalid or unavailable"));
    }

    @Test
    void shouldReturnOrderDetailsForAuthenticatedUserInSameStore() throws Exception {
        String token = TestLoginSupport.loginAndExtractToken(mockMvc, """
                {
                  "storeCode": "TW001",
                  "roleCode": "MANAGER",
                  "pin": "9999",
                  "deviceCode": "POS-TABLET-001"
                }
                """);

        MvcResult createResult = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    {
                                      "productId": "44444444-4444-4444-4444-444444444445",
                                      "quantity": 2
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        UUID orderId = TestLoginSupport.extractDataFieldAsUuid(createResult, "id");

        mockMvc.perform(get("/api/v1/orders/{orderId}", orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(orderId.toString()))
                .andExpect(jsonPath("$.data.createdByEmployeeCode").value("EMP-MANAGER-001"))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].productName").value("AI Lecture Pass"))
                .andExpect(jsonPath("$.data.totalAmount").value(50.00));
    }

    @Test
    void shouldListOrdersForCurrentStoreWithPaymentStatusFilter() throws Exception {
        String token = TestLoginSupport.loginAndExtractToken(mockMvc, """
                {
                  "storeCode": "TW001",
                  "roleCode": "CASHIER",
                  "pin": "1234",
                  "deviceCode": "POS-TABLET-001"
                }
                """);

        MvcResult unpaidOrder = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    {
                                      "productId": "44444444-4444-4444-4444-444444444443",
                                      "quantity": 1
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult paidOrder = mockMvc.perform(post("/api/v1/orders")
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

        UUID paidOrderId = TestLoginSupport.extractDataFieldAsUuid(paidOrder, "id");
        UUID unpaidOrderId = TestLoginSupport.extractDataFieldAsUuid(unpaidOrder, "id");

        mockMvc.perform(post("/api/v1/orders/{orderId}/payments", paidOrderId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentMethod": "CASH",
                                  "amount": 8.50
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/orders")
                        .header("Authorization", "Bearer " + token)
                        .queryParam("paymentStatus", "PAID")
                        .queryParam("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(paidOrderId.toString()))
                .andExpect(jsonPath("$.data.items[0].paymentStatus").value("PAID"))
                .andExpect(jsonPath("$.data.items[0].totalAmount").value(8.50));

        mockMvc.perform(get("/api/v1/orders")
                        .header("Authorization", "Bearer " + token)
                        .queryParam("status", "CREATED")
                        .queryParam("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(unpaidOrderId.toString()))
                .andExpect(jsonPath("$.data.items[0].status").value("CREATED"));
    }

    @Test
    void shouldPaginateSortAndFilterOrdersByDateRange() throws Exception {
        String token = TestLoginSupport.loginAndExtractToken(mockMvc, """
                {
                  "storeCode": "TW001",
                  "roleCode": "CASHIER",
                  "pin": "1234",
                  "deviceCode": "POS-TABLET-001"
                }
                """);

        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    {
                                      "productId": "44444444-4444-4444-4444-444444444443",
                                      "quantity": 1
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/orders")
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
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    {
                                      "productId": "44444444-4444-4444-4444-444444444445",
                                      "quantity": 1
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/orders")
                        .header("Authorization", "Bearer " + token)
                        .queryParam("page", "0")
                        .queryParam("size", "2")
                        .queryParam("sortBy", "totalAmount")
                        .queryParam("sortDirection", "asc")
                        .queryParam("from", "2020-01-01T00:00:00Z")
                        .queryParam("to", "2100-01-01T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.hasNext").value(true))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].totalAmount").value(5.25))
                .andExpect(jsonPath("$.data.items[1].totalAmount").value(8.50));

        mockMvc.perform(get("/api/v1/orders")
                        .header("Authorization", "Bearer " + token)
                        .queryParam("from", "2100-01-01T00:00:00Z")
                        .queryParam("to", "2100-01-02T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0))
                .andExpect(jsonPath("$.data.items.length()").value(0));
    }

    @Test
    void shouldRejectOrderListWhenDateRangeIsInvalid() throws Exception {
        String token = TestLoginSupport.loginAndExtractToken(mockMvc, """
                {
                  "storeCode": "TW001",
                  "roleCode": "CASHIER",
                  "pin": "1234",
                  "deviceCode": "POS-TABLET-001"
                }
                """);

        mockMvc.perform(get("/api/v1/orders")
                        .header("Authorization", "Bearer " + token)
                        .queryParam("from", "2026-03-20T12:00:00Z")
                        .queryParam("to", "2026-03-20T11:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("'from' must be earlier than or equal to 'to'"));
    }

    @Test
    void shouldCaptureCashPaymentAndMarkOrderPaid() throws Exception {
        String token = TestLoginSupport.loginAndExtractToken(mockMvc, """
                {
                  "storeCode": "TW001",
                  "roleCode": "CASHIER",
                  "pin": "1234",
                  "deviceCode": "POS-TABLET-001"
                }
                """);

        MvcResult createResult = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    {
                                      "productId": "44444444-4444-4444-4444-444444444442",
                                      "quantity": 2
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        UUID orderId = TestLoginSupport.extractDataFieldAsUuid(createResult, "id");

        mockMvc.perform(post("/api/v1/orders/{orderId}/payments", orderId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentMethod": "CASH",
                                  "amount": 13.50,
                                  "amountReceived": 20.00,
                                  "note": "Customer paid cash"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"))
                .andExpect(jsonPath("$.data.paymentStatus").value("PAID"))
                .andExpect(jsonPath("$.data.totalAmount").value(13.50))
                .andExpect(jsonPath("$.data.paidAmount").value(13.50))
                .andExpect(jsonPath("$.data.changeAmount").value(6.50))
                .andExpect(jsonPath("$.data.closedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.payments.length()").value(1))
                .andExpect(jsonPath("$.data.payments[0].paymentMethod").value("CASH"))
                .andExpect(jsonPath("$.data.payments[0].status").value("CAPTURED"))
                .andExpect(jsonPath("$.data.payments[0].amount").value(13.50))
                .andExpect(jsonPath("$.data.payments[0].amountReceived").value(20.00))
                .andExpect(jsonPath("$.data.payments[0].changeAmount").value(6.50))
                .andExpect(jsonPath("$.data.payments[0].cardTerminalProvider").isEmpty())
                .andExpect(jsonPath("$.data.payments[0].createdByEmployeeCode").value("EMP-CASHIER-001"))
                .andExpect(jsonPath("$.data.refundedAmount").value(0.00));
    }

    @Test
    void shouldRejectPaymentWhenReceivedAmountIsLessThanAppliedAmount() throws Exception {
        String token = TestLoginSupport.loginAndExtractToken(mockMvc, """
                {
                  "storeCode": "TW001",
                  "roleCode": "CASHIER",
                  "pin": "1234",
                  "deviceCode": "POS-TABLET-001"
                }
                """);

        MvcResult createResult = mockMvc.perform(post("/api/v1/orders")
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

        UUID orderId = TestLoginSupport.extractDataFieldAsUuid(createResult, "id");

        mockMvc.perform(post("/api/v1/orders/{orderId}/payments", orderId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentMethod": "CASH",
                                  "amount": 8.50,
                                  "amountReceived": 5.00
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Received amount cannot be less than applied amount"));
    }

    @Test
    void shouldRefundPaidOrderPartially() throws Exception {
        String token = TestLoginSupport.loginAndExtractToken(mockMvc, """
                {
                  "storeCode": "TW001",
                  "roleCode": "MANAGER",
                  "pin": "9999",
                  "deviceCode": "POS-TABLET-001"
                }
                """);

        MvcResult createResult = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    {
                                      "productId": "44444444-4444-4444-4444-444444444445",
                                      "quantity": 1
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        UUID orderId = TestLoginSupport.extractDataFieldAsUuid(createResult, "id");

        MvcResult authorizedResult = mockMvc.perform(post("/api/v1/orders/{orderId}/payments/authorize", orderId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 25.00
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CREATED"))
                .andExpect(jsonPath("$.data.paymentStatus").value("UNPAID"))
                .andExpect(jsonPath("$.data.payments[0].paymentMethod").value("CARD"))
                .andExpect(jsonPath("$.data.payments[0].status").value("AUTHORIZED"))
                .andExpect(jsonPath("$.data.payments[0].cardTerminalProvider").value("TCB_MOCK"))
                .andExpect(jsonPath("$.data.payments[0].cardTransactionStatus").value("AUTHORIZED"))
                .andExpect(jsonPath("$.data.payments[0].cardTerminalTransactionId").isNotEmpty())
                .andExpect(jsonPath("$.data.payments[0].cardApprovalCode").isNotEmpty())
                .andExpect(jsonPath("$.data.payments[0].cardMaskedPan").value("4111********1111"))
                .andExpect(jsonPath("$.data.payments[0].cardEntryMode").value("CONTACT"))
                .andExpect(jsonPath("$.data.payments[0].authorizedAt").isNotEmpty())
                .andReturn();

        UUID paymentId = TestLoginSupport.extractDataArrayFieldAsUuid(authorizedResult, "payments", 0, "id");

        mockMvc.perform(post("/api/v1/orders/{orderId}/payments/{paymentId}/capture", orderId, paymentId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "note": "Capture on checkout"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"))
                .andExpect(jsonPath("$.data.paymentStatus").value("PAID"))
                .andExpect(jsonPath("$.data.payments[0].status").value("CAPTURED"))
                .andExpect(jsonPath("$.data.payments[0].cardTransactionStatus").value("CAPTURED"))
                .andExpect(jsonPath("$.data.payments[0].capturedAt").isNotEmpty());

        mockMvc.perform(post("/api/v1/orders/{orderId}/refunds", orderId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentId": "%s",
                                  "refundMethod": "CARD_REFUND",
                                  "amount": 10.00,
                                  "reason": "Customer changed mind"
                                }
                                """.formatted(paymentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PARTIALLY_REFUNDED"))
                .andExpect(jsonPath("$.data.paymentStatus").value("PARTIALLY_REFUNDED"))
                .andExpect(jsonPath("$.data.paidAmount").value(25.00))
                .andExpect(jsonPath("$.data.refundedAmount").value(10.00))
                .andExpect(jsonPath("$.data.refunds.length()").value(1))
                .andExpect(jsonPath("$.data.refunds[0].paymentId").value(paymentId.toString()))
                .andExpect(jsonPath("$.data.refunds[0].refundMethod").value("CARD_REFUND"))
                .andExpect(jsonPath("$.data.refunds[0].amount").value(10.00))
                .andExpect(jsonPath("$.data.refunds[0].reason").value("Customer changed mind"))
                .andExpect(jsonPath("$.data.refunds[0].status").value("PROCESSED"));
    }

    @Test
    void shouldRejectRefundWhenAmountExceedsSelectedPaymentBalance() throws Exception {
        String token = TestLoginSupport.loginAndExtractToken(mockMvc, """
                {
                  "storeCode": "TW001",
                  "roleCode": "MANAGER",
                  "pin": "9999",
                  "deviceCode": "POS-TABLET-001"
                }
                """);

        MvcResult createResult = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    {
                                      "productId": "44444444-4444-4444-4444-444444444443",
                                      "quantity": 1
                                    },
                                    {
                                      "productId": "44444444-4444-4444-4444-444444444441",
                                      "quantity": 1
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        UUID orderId = TestLoginSupport.extractDataFieldAsUuid(createResult, "id");

        MvcResult firstAuthorization = mockMvc.perform(post("/api/v1/orders/{orderId}/payments/authorize", orderId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 5.25
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        UUID firstPaymentId = TestLoginSupport.extractDataArrayFieldAsUuid(firstAuthorization, "payments", 0, "id");

        mockMvc.perform(post("/api/v1/orders/{orderId}/payments/{paymentId}/capture", orderId, firstPaymentId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "note": "Capture first card payment"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/orders/{orderId}/payments", orderId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentMethod": "CASH",
                                  "amount": 8.50
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/orders/{orderId}/refunds", orderId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentId": "%s",
                                  "refundMethod": "CARD_REFUND",
                                  "amount": 6.00,
                                  "reason": "Refund exceeds first payment"
                                }
                                """.formatted(firstPaymentId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Refund amount exceeds refundable balance for the selected payment"));
    }

    @Test
    void shouldMarkCardPaymentRefundedWhenPaymentIsFullyRefunded() throws Exception {
        String token = TestLoginSupport.loginAndExtractToken(mockMvc, """
                {
                  "storeCode": "TW001",
                  "roleCode": "MANAGER",
                  "pin": "9999",
                  "deviceCode": "POS-TABLET-001"
                }
                """);

        MvcResult createResult = mockMvc.perform(post("/api/v1/orders")
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

        UUID orderId = TestLoginSupport.extractDataFieldAsUuid(createResult, "id");

        MvcResult authorizationResult = mockMvc.perform(post("/api/v1/orders/{orderId}/payments/authorize", orderId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 8.50
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        UUID paymentId = TestLoginSupport.extractDataArrayFieldAsUuid(authorizationResult, "payments", 0, "id");

        mockMvc.perform(post("/api/v1/orders/{orderId}/payments/{paymentId}/capture", orderId, paymentId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "note": "Capture full card payment"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/orders/{orderId}/refunds", orderId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentId": "%s",
                                  "refundMethod": "CARD_REFUND",
                                  "amount": 8.50,
                                  "reason": "Full refund"
                                }
                                """.formatted(paymentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REFUNDED"))
                .andExpect(jsonPath("$.data.paymentStatus").value("REFUNDED"))
                .andExpect(jsonPath("$.data.refundedAmount").value(8.50))
                .andExpect(jsonPath("$.data.payments[0].cardTransactionStatus").value("REFUNDED"))
                .andExpect(jsonPath("$.data.payments[0].refundedAt").isNotEmpty());
    }

    @Test
    void shouldRejectCashPaymentRefundWhenRefundMethodIsNotCash() throws Exception {
        String token = TestLoginSupport.loginAndExtractToken(mockMvc, """
                {
                  "storeCode": "TW001",
                  "roleCode": "CASHIER",
                  "pin": "1234",
                  "deviceCode": "POS-TABLET-001"
                }
                """);

        MvcResult createResult = mockMvc.perform(post("/api/v1/orders")
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

        UUID orderId = TestLoginSupport.extractDataFieldAsUuid(createResult, "id");

        MvcResult paymentResult = mockMvc.perform(post("/api/v1/orders/{orderId}/payments", orderId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentMethod": "CASH",
                                  "amount": 8.50
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        UUID paymentId = TestLoginSupport.extractDataArrayFieldAsUuid(paymentResult, "payments", 0, "id");

        mockMvc.perform(post("/api/v1/orders/{orderId}/refunds", orderId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentId": "%s",
                                  "refundMethod": "CARD_REFUND",
                                  "amount": 3.00
                                }
                                """.formatted(paymentId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Cash payments must use CASH refund method"));
    }

    @Test
    void shouldVoidUnpaidOrder() throws Exception {
        String token = TestLoginSupport.loginAndExtractToken(mockMvc, """
                {
                  "storeCode": "TW001",
                  "roleCode": "CASHIER",
                  "pin": "1234",
                  "deviceCode": "POS-TABLET-001"
                }
                """);

        MvcResult createResult = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    {
                                      "productId": "44444444-4444-4444-4444-444444444443",
                                      "quantity": 1
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        UUID orderId = TestLoginSupport.extractDataFieldAsUuid(createResult, "id");

        MvcResult authorizationResult = mockMvc.perform(post("/api/v1/orders/{orderId}/payments/authorize", orderId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 5.25
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        UUID authorizedPaymentId = TestLoginSupport.extractDataArrayFieldAsUuid(authorizationResult, "payments", 0, "id");

        mockMvc.perform(post("/api/v1/orders/{orderId}/cancel", orderId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "Cashier input mistake"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("VOIDED"))
                .andExpect(jsonPath("$.data.paymentStatus").value("VOIDED"))
                .andExpect(jsonPath("$.data.payments[0].id").value(authorizedPaymentId.toString()))
                .andExpect(jsonPath("$.data.payments[0].status").value("VOIDED"))
                .andExpect(jsonPath("$.data.payments[0].cardTransactionStatus").value("VOIDED"))
                .andExpect(jsonPath("$.data.payments[0].voidedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.voidedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.voidNote").value("Cashier input mistake"));
    }
}
