package pl.lumogo.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    private static String registerBody(String email, String password) {
        return """
            {"firstName":"Anna","email":"%s","password":"%s","passwordConfirm":"%s","gender":"Woman"}
            """.formatted(email, password, password);
    }

    private String registerAndGetToken(String email) throws Exception {
        String body = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(registerBody(email, "Haslo!xyz")))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }

    @Test
    void registersWithoutLastNameAndReturnsToken() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(registerBody("Anna.Bez@Example.com", "Haslo!xyz")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("anna.bez@example.com"))
            .andExpect(jsonPath("$.token").isNotEmpty());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ANNA.bez@example.com\",\"password\":\"Haslo!xyz\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.firstName").value("Anna"))
            .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void duplicateEmailWithDifferentCaseReturns409WithCode() throws Exception {
        registerAndGetToken("dup@example.com");
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(registerBody("  DUP@example.com ", "Haslo!xyz")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
    }

    @Test
    void rejectsPasswordWithoutSpecialCharacter() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(registerBody("weak@example.com", "Haslo1234")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void wrongPasswordReturns401WithCode() throws Exception {
        registerAndGetToken("wrong@example.com");
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"wrong@example.com\",\"password\":\"Inne!haslo\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"nobody@example.com\",\"password\":\"Haslo!xyz\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void userEndpointsRequireToken() throws Exception {
        mvc.perform(get("/api/auth/profile")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/interests/user")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/widget/status")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/profile").header("Authorization", "Bearer nie.prawdziwy.token"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenGivesAccessOnlyToOwnProfile() throws Exception {
        String token = registerAndGetToken("owner@example.com");
        registerAndGetToken("victim@example.com");

        mvc.perform(get("/api/auth/profile").header("Authorization", "Bearer " + token)
                .param("email", "victim@example.com"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("owner@example.com"));

        mvc.perform(put("/api/auth/profile").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"victim@example.com\",\"firstName\":\"Ola\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("owner@example.com"))
            .andExpect(jsonPath("$.firstName").value("Ola"));

        mvc.perform(post("/api/interests").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"interests\":[\"Bieganie\",\"Spacer\"]}"))
            .andExpect(status().isOk());
        mvc.perform(get("/api/interests/user").header("Authorization", "Bearer " + token))
            .andExpect(jsonPath("$.interests[0]").value("Bieganie"));
        mvc.perform(get("/api/widget/status").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("owner@example.com"));
    }

    @Test
    void publicEndpointsStayOpen() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk());
        mvc.perform(get("/api/interests/options")).andExpect(status().isOk());
        mvc.perform(options("/api/auth/login")
                .header("Origin", "http://localhost:8081")
                .header("Access-Control-Request-Method", "POST"))
            .andExpect(status().isOk());
    }
}
