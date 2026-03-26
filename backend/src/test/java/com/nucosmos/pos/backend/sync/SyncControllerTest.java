package com.nucosmos.pos.backend.sync;

import com.nucosmos.pos.backend.auth.TestLoginSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SyncControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnBootstrapCatalogForAuthenticatedDevice() throws Exception {
        String cashierToken = TestLoginSupport.loginAndExtractToken(mockMvc, """
                {
                  "storeCode": "TW001",
                  "roleCode": "CASHIER",
                  "pin": "123456",
                  "deviceCode": "POS-TABLET-001"
                }
                """);

        mockMvc.perform(get("/api/v1/sync/bootstrap")
                        .header("Authorization", "Bearer " + cashierToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.storeCode").value("TW001"))
                .andExpect(jsonPath("$.data.deviceCode").value("POS-TABLET-001"))
                .andExpect(jsonPath("$.data.deviceAuthorized").value(true))
                .andExpect(jsonPath("$.data.categories.length()").value(2))
                .andExpect(jsonPath("$.data.products.length()").value(5));
    }

    @Test
    void shouldReturnEmptyDeltaWhenSinceIsAfterSeedData() throws Exception {
        String cashierToken = TestLoginSupport.loginAndExtractToken(mockMvc, """
                {
                  "storeCode": "TW001",
                  "roleCode": "CASHIER",
                  "pin": "123456",
                  "deviceCode": "POS-TABLET-001"
                }
                """);

        mockMvc.perform(get("/api/v1/sync/catalog")
                        .param("since", "2026-03-21T00:00:00+08:00")
                        .header("Authorization", "Bearer " + cashierToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categories.length()").value(0))
                .andExpect(jsonPath("$.data.products.length()").value(0));
    }
}
