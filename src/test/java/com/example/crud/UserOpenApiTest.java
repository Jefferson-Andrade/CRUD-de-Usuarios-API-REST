package com.example.crud;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserOpenApiTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Test
    void shouldDocumentValidationAndTypedErrorsInOpenApi() throws Exception {
        String json = mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        var doc = objectMapper.readTree(json);
        assertEquals("basic", doc.get("components").get("securitySchemes").get("basicAuth").get("scheme").asText());
        assertTrue(doc.get("paths").get("/users").get("get").get("security").get(0).has("basicAuth"));
        for (String code : new String[]{"401", "403"}) {
            assertTrue(doc.get("paths").get("/users").get("post").get("responses").has(code));
        }
        var schemas = doc.get("components").get("schemas");
        assertEquals(255, schemas.get("UserRequestDTO").get("properties").get("name").get("maxLength").asInt());
        var fields = schemas.get("ErrorResponseDTO").get("properties");
        for (String field : new String[]{"status", "error", "message", "timestamp"}) assertTrue(fields.has(field));
        assertEquals("date-time", fields.get("timestamp").get("format").asText());
        for (String method : new String[]{"post", "put"}) {
            String path = method.equals("post") ? "/users" : "/users/{id}";
            String ref = doc.get("paths").get(path).get(method).get("responses").get("400")
                    .get("content").get("application/json").get("schema").get("$ref").asText();
            assertTrue(ref.endsWith("/ErrorResponseDTO"));
        }
        for (String method : new String[]{"get", "put", "delete"}) {
            String ref = doc.get("paths").get("/users/{id}").get(method).get("responses").get("404")
                    .get("content").get("application/json").get("schema").get("$ref").asText();
            assertTrue(ref.endsWith("/ErrorResponseDTO"));
        }
    }


}
