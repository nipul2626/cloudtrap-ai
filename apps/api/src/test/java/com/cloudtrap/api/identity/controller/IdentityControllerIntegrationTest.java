
package com.cloudtrap.api.identity.controller;

import com.cloudtrap.api.identity.entity.User;
import com.cloudtrap.api.identity.repository.UserRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;



import java.util.UUID;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("postgres-test")
@TestPropertySource(
        locations = "classpath:application-postgres-test.properties"
)
class IdentityControllerIntegrationTest {

    private static final String REGISTER_ENDPOINT =
            "/api/v1/auth/register";

    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-"
                    + "[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private String emailToCleanUp;

    @AfterEach
    void cleanUpCreatedUser() {
        if (emailToCleanUp != null) {
            userRepository.findByEmail(emailToCleanUp)
                    .ifPresent(userRepository::delete);
        }
    }

    @Test
    void registerShouldReturnCreatedWithSafeResponse() throws Exception {
        emailToCleanUp = uniqueEmail();

        String requestBody = objectMapper.writeValueAsString(
                new RegistrationPayload(
                        emailToCleanUp,
                        "StrongPass123",
                        "Integration Test User"
                )
        );

        MvcResult result = mockMvc.perform(
                        post(REGISTER_ENDPOINT)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.email").value(emailToCleanUp))
                .andExpect(jsonPath("$.displayName")
                        .value("Integration Test User"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andReturn();

        String requestId = result.getResponse()
                .getHeader(REQUEST_ID_HEADER);

        assertNotNull(requestId);
        assertTrue(UUID_PATTERN.matcher(requestId).matches());

        JsonNode responseBody = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );

        assertFalse(responseBody.has("password"));
        assertFalse(responseBody.has("passwordHash"));
        assertFalse(responseBody.has("password_hash"));

        User savedUser = userRepository.findByEmail(emailToCleanUp)
                .orElseThrow();

        assertEquals(emailToCleanUp, savedUser.getEmail());
        assertNotNull(savedUser.getPasswordHash());
        assertFalse(
                "StrongPass123".equals(savedUser.getPasswordHash()),
                "The stored password must not be plaintext"
        );
    }

    @Test
    void registerShouldReturnBadRequestForInvalidEmail() throws Exception {
        String requestBody = """
                {
                  "email": "not-an-email",
                  "password": "StrongPass123",
                  "displayName": "Integration Test User"
                }
                """;

        MvcResult result = mockMvc.perform(
                        post(REGISTER_ENDPOINT)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode")
                        .value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andReturn();

        assertRequestIdIsConsistent(result);
    }

    @Test
    void registerShouldReturnBadRequestForMalformedJson() throws Exception {
        MvcResult result = mockMvc.perform(
                        post(REGISTER_ENDPOINT)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"email":
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode")
                        .value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.message")
                        .value("Request body is missing or malformed"))
                .andReturn();

        assertRequestIdIsConsistent(result);
    }

    @Test
    void registerShouldReturnConflictForDuplicateEmail() throws Exception {
        emailToCleanUp = uniqueEmail();

        String requestBody = objectMapper.writeValueAsString(
                new RegistrationPayload(
                        emailToCleanUp,
                        "StrongPass123",
                        "First Registration"
                )
        );

        mockMvc.perform(
                        post(REGISTER_ENDPOINT)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isCreated());

        MvcResult duplicateResult = mockMvc.perform(
                        post(REGISTER_ENDPOINT)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        new RegistrationPayload(
                                                emailToCleanUp.toUpperCase(
                                                        java.util.Locale.ROOT
                                                ),
                                                "StrongPass123",
                                                "Duplicate Registration"
                                        )
                                ))
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode")
                        .value("EMAIL_ALREADY_REGISTERED"))
                .andExpect(jsonPath("$.message")
                        .value("An account with this email address already exists"))
                .andReturn();

        assertRequestIdIsConsistent(duplicateResult);
    }

    private void assertRequestIdIsConsistent(MvcResult result)
            throws Exception {
        String headerRequestId = result.getResponse()
                .getHeader(REQUEST_ID_HEADER);

        assertNotNull(headerRequestId);
        assertTrue(UUID_PATTERN.matcher(headerRequestId).matches());

        JsonNode responseBody = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );

        assertEquals(
                headerRequestId,
                responseBody.path("requestId").asText()
        );
    }

    private String uniqueEmail() {
        return "integration-" + UUID.randomUUID() + "@example.com";
    }

    private record RegistrationPayload(
            String email,
            String password,
            String displayName
    ) {
    }
}
