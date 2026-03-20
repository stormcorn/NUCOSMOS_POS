package com.nucosmos.pos.backend.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public final class TestLoginSupport {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private TestLoginSupport() {
    }

    public static String loginAndExtractToken(MockMvc mockMvc, String requestJson) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/pin-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode jsonNode = OBJECT_MAPPER.readTree(result.getResponse().getContentAsString());
        return jsonNode.path("data").path("accessToken").asText();
    }

    public static UUID extractDataFieldAsUuid(MvcResult result, String fieldName) throws Exception {
        JsonNode jsonNode = OBJECT_MAPPER.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(jsonNode.path("data").path(fieldName).asText());
    }

    public static UUID extractDataArrayFieldAsUuid(MvcResult result, String arrayFieldName, int index, String fieldName) throws Exception {
        JsonNode jsonNode = OBJECT_MAPPER.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(jsonNode.path("data").path(arrayFieldName).path(index).path(fieldName).asText());
    }
}
