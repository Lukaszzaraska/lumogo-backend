package pl.lumogo.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mvc;

    private static String register(String email, String password) {
        return """
            {"firstName":"Anna","email":"%s","password":"%s","passwordConfirm":"%s","gender":"Woman"}
            """.formatted(email, password, password);
    }

    @Test
    void registersWithoutLastNameAndLogsIn() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(register("Anna.Bez@Example.com", "Haslo!xyz")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.email").value("anna.bez@example.com"));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"anna.bez@example.com\",\"password\":\"Haslo!xyz\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.firstName").value("Anna"));
    }

    @Test
    void duplicateEmailWithDifferentCaseReturns409() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(register("dup@example.com", "Haslo!xyz")))
            .andExpect(status().isOk());

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(register("  DUP@example.com ", "Haslo!xyz")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    void rejectsPasswordWithoutSpecialCharacter() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(register("weak@example.com", "Haslo1234")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Hasło musi zawierać co najmniej jeden znak specjalny"));
    }

    @Test
    void wrongPasswordReturns400() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"nobody@example.com\",\"password\":\"Haslo!xyz\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void widgetEndpointIsReachable() throws Exception {
        mvc.perform(get("/api/widget/status").param("email", "a@b.pl"))
            .andExpect(status().isOk());
    }

    @Test
    void corsPreflightFromBrowserIsAccepted() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .options("/api/auth/login")
                .header("Origin", "http://localhost:8081")
                .header("Access-Control-Request-Method", "POST"))
            .andExpect(status().isOk());
    }

    @Test
    void profileByEmailParamWorks() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(register("profile@example.com", "Haslo!xyz")))
            .andExpect(status().isOk());
        mvc.perform(get("/api/auth/profile").param("email", "Profile@Example.com"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.firstName").value("Anna"));
        mvc.perform(get("/api/interests/user").param("email", "profile@example.com"))
            .andExpect(status().isOk());
    }
}
