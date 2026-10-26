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
        var security = doc.get("paths").get("/users").get("get").get("security");
        assertTrue(java.util.stream.StreamSupport.stream(security.spliterator(), false)
                .anyMatch(requirement -> requirement.has("basicAuth")));
        for (String code : new String[]{"401", "403"}) {
            assertTrue(doc.get("paths").get("/users").get("post").get("responses").has(code));
        }
        var schemas = doc.get("components").get("schemas");
        var responseFields = schemas.get("UserResponseDTO").get("properties");
        for (String field : new String[]{"createdAt", "updatedAt"}) {
            assertEquals("date-time", responseFields.get(field).get("format").asText());
            assertTrue(responseFields.get(field).get("readOnly").asBoolean());
            assertFalse(schemas.get("UserRequestDTO").get("properties").has(field));
        }
        assertEquals(255, schemas.get("UserRequestDTO").get("properties").get("name").get("maxLength").asInt());
        var email = schemas.get("UserRequestDTO").get("properties").get("email");
        assertEquals("email", email.get("format").asText());
        assertEquals(254, email.get("maxLength").asInt());
        assertTrue(schemas.get("UserRequestDTO").get("required").toString().contains("email"));
        assertTrue(responseFields.has("email"));
        for (String method : new String[]{"post", "put"}) {
            String path = method.equals("post") ? "/users" : "/users/{id}";
            String conflict = doc.get("paths").get(path).get(method).get("responses").get("409")
                    .get("content").get("application/json").get("schema").get("$ref").asText();
            assertTrue(conflict.endsWith("/ErrorResponseDTO"));
        }
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
