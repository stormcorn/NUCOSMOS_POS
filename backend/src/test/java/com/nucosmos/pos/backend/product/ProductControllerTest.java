package com.nucosmos.pos.backend.product;

import com.nucosmos.pos.backend.auth.TestLoginSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnSeededProductsFromDatabase() throws Exception {
        String token = TestLoginSupport.loginAndExtractToken(mockMvc, """
                {
                  "storeCode": "TW001",
                  "roleCode": "CASHIER",
                  "pin": "1234",
                  "deviceCode": "POS-TABLET-001"
                }
                """);

        mockMvc.perform(get("/api/v1/products")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(5))
                .andExpect(jsonPath("$.data[0].categoryName").value("Tea & Drinks"))
                .andExpect(jsonPath("$.data[0].name").value("Cold Brew Coffee"))
                .andExpect(jsonPath("$.data[0].imageUrl").isNotEmpty());
    }

    @Test
    void shouldAllowManagerToManageProducts() throws Exception {
        String managerToken = TestLoginSupport.loginAndExtractToken(mockMvc, """
                {
                  "storeCode": "TW001",
                  "roleCode": "MANAGER",
                  "pin": "9999",
                  "deviceCode": "POS-TABLET-001"
                }
                """);

        mockMvc.perform(get("/api/v1/admin/product-categories")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("tea-drinks"));

        MvcResult createResult = mockMvc.perform(post("/api/v1/admin/products")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": "33333333-3333-3333-3333-333333333331",
                                  "sku": "drink-999",
                                  "name": "Seasonal Jasmine Tea",
                                  "description": "Limited spring batch",
                                  "imageUrl": "https://example.com/seasonal-jasmine-tea.jpg",
                                  "price": 9.50
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sku").value("drink-999"))
                .andExpect(jsonPath("$.data.imageUrl").value("https://example.com/seasonal-jasmine-tea.jpg"))
                .andExpect(jsonPath("$.data.active").value(true))
                .andReturn();

        String productId = TestLoginSupport.extractDataFieldAsUuid(createResult, "id").toString();

        mockMvc.perform(put("/api/v1/admin/products/{productId}", productId)
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": "33333333-3333-3333-3333-333333333332",
                                  "sku": "event-999",
                                  "name": "Workshop Pass",
                                  "description": "Updated admin product",
                                  "imageUrl": "https://example.com/workshop-pass.jpg",
                                  "price": 29.00
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categoryCode").value("events"))
                .andExpect(jsonPath("$.data.sku").value("event-999"))
                .andExpect(jsonPath("$.data.imageUrl").value("https://example.com/workshop-pass.jpg"))
                .andExpect(jsonPath("$.data.price").value(29.0));

        mockMvc.perform(post("/api/v1/admin/products/{productId}/deactivate", productId)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));

        mockMvc.perform(get("/api/v1/admin/products")
                        .param("active", "false")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(productId))
                .andExpect(jsonPath("$.data[0].active").value(false));
    }

    @Test
    void shouldAllowManagerToManageProductCategories() throws Exception {
        String managerToken = TestLoginSupport.loginAndExtractToken(mockMvc, """
                {
                  "storeCode": "TW001",
                  "roleCode": "MANAGER",
                  "pin": "9999",
                  "deviceCode": "POS-TABLET-001"
                }
                """);

        MvcResult createResult = mockMvc.perform(post("/api/v1/admin/product-categories")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "seasonal-specials",
                                  "name": "Seasonal Specials",
                                  "displayOrder": 30
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("seasonal-specials"))
                .andExpect(jsonPath("$.data.name").value("Seasonal Specials"))
                .andExpect(jsonPath("$.data.active").value(true))
                .andReturn();

        String categoryId = TestLoginSupport.extractDataFieldAsUuid(createResult, "id").toString();

        mockMvc.perform(put("/api/v1/admin/product-categories/{categoryId}", categoryId)
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "seasonal-featured",
                                  "name": "Seasonal Featured",
                                  "displayOrder": 35
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("seasonal-featured"))
                .andExpect(jsonPath("$.data.name").value("Seasonal Featured"))
                .andExpect(jsonPath("$.data.displayOrder").value(35));

        mockMvc.perform(post("/api/v1/admin/product-categories/{categoryId}/deactivate", categoryId)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));
    }

    @Test
    void shouldRejectDeactivatingCategoryWithActiveProducts() throws Exception {
        String managerToken = TestLoginSupport.loginAndExtractToken(mockMvc, """
                {
                  "storeCode": "TW001",
                  "roleCode": "MANAGER",
                  "pin": "9999",
                  "deviceCode": "POS-TABLET-001"
                }
                """);

        mockMvc.perform(post("/api/v1/admin/product-categories/{categoryId}/deactivate", "33333333-3333-3333-3333-333333333331")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Product category still has active products"));
    }

    @Test
    void shouldRejectCashierFromManagingProducts() throws Exception {
        String cashierToken = TestLoginSupport.loginAndExtractToken(mockMvc, """
                {
                  "storeCode": "TW001",
                  "roleCode": "CASHIER",
                  "pin": "1234",
                  "deviceCode": "POS-TABLET-001"
                }
                """);

        mockMvc.perform(post("/api/v1/admin/products")
                        .header("Authorization", "Bearer " + cashierToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": "33333333-3333-3333-3333-333333333331",
                                  "sku": "drink-777",
                                  "name": "Unauthorized Product",
                                  "description": "Should fail",
                                  "imageUrl": "https://example.com/unauthorized.jpg",
                                  "price": 8.00
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectDuplicateSkuWhenCreatingProduct() throws Exception {
        String managerToken = TestLoginSupport.loginAndExtractToken(mockMvc, """
                {
                  "storeCode": "TW001",
                  "roleCode": "MANAGER",
                  "pin": "9999",
                  "deviceCode": "POS-TABLET-001"
                }
                """);

        mockMvc.perform(post("/api/v1/admin/products")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": "33333333-3333-3333-3333-333333333331",
                                  "sku": "drink-001",
                                  "name": "Duplicate SKU Product",
                                  "description": "Should fail",
                                  "imageUrl": "https://example.com/duplicate.jpg",
                                  "price": 8.00
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Product SKU already exists"));
    }
}
