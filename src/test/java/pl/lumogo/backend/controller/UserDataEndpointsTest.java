package pl.lumogo.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Kontrakt nowych ekranów aplikacji (LUM-54): rejestracja z płcią, plany, preferencje, hasło, usuwanie konta. */
@SpringBootTest
@AutoConfigureMockMvc
class UserDataEndpointsTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    /** Dokładnie to, co wysyła RegistrationScreen: bez passwordConfirm, z gender. */
    private String register(String email, String gender) throws Exception {
        String body = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"firstName\":\"Zuza\",\"gender\":\"%s\",\"password\":\"Haslo!xyz\"}".formatted(email, gender)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static final String PLAN = """
        {"userId":999,"activityEmoji":"🚶","activityLabel":"Walking","dateLabel":"Tomorrow",
         "startTime":"14:00","endTime":"14:30","plannedDate":"2026-11-10"}
        """;

    @Test
    void registrationFromAppReturnsIdAndGender() throws Exception {
        String token = register("zuza@example.com", "Woman");
        mvc.perform(get("/api/auth/profile").header("Authorization", bearer(token)))
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.gender").value("Woman"));

        String noGender = register("nogender@example.com", "-");
        mvc.perform(get("/api/auth/profile").header("Authorization", bearer(noGender)))
            .andExpect(jsonPath("$.gender").doesNotExist());
    }

    @Test
    void editProfileUpdatesNameAndGender() throws Exception {
        String token = register("edit@example.com", "-");
        mvc.perform(put("/api/auth/profile").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"Zuzanna\",\"gender\":\"Woman\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.firstName").value("Zuzanna"))
            .andExpect(jsonPath("$.gender").value("Woman"));
    }

    @Test
    void plansAreIsolatedBetweenUsers() throws Exception {
        String alice = register("alice@example.com", "-");
        String bob = register("bob@example.com", "-");

        String created = mvc.perform(post("/api/plans").header("Authorization", bearer(alice))
                .contentType(MediaType.APPLICATION_JSON).content(PLAN))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.activityLabel").value("Walking"))
            .andReturn().getResponse().getContentAsString();
        long planId = json.readTree(created).get("id").asLong();

        mvc.perform(get("/api/plans").header("Authorization", bearer(alice))).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/plans").header("Authorization", bearer(bob))).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(delete("/api/plans/" + planId).header("Authorization", bearer(bob))).andExpect(status().isNotFound());
        mvc.perform(delete("/api/plans/" + planId).header("Authorization", bearer(alice))).andExpect(status().isNoContent());
        mvc.perform(get("/api/plans").header("Authorization", bearer(alice))).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/plans")).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsPlanWithBadTime() throws Exception {
        String token = register("badtime@example.com", "-");
        mvc.perform(post("/api/plans").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                .content(PLAN.replace("14:30", "25:00")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void preferencesHaveDefaultsAndCanBeSaved() throws Exception {
        String token = register("prefs@example.com", "-");
        mvc.perform(get("/api/preferences").header("Authorization", bearer(token)))
            .andExpect(jsonPath("$.defaultDuration").value(30))
            .andExpect(jsonPath("$.favoriteActivities").value(""));
        mvc.perform(put("/api/preferences").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"defaultDuration\":45,\"favoriteActivities\":\"walking,running\"}"))
            .andExpect(jsonPath("$.defaultDuration").value(45));
        mvc.perform(get("/api/preferences").header("Authorization", bearer(token)))
            .andExpect(jsonPath("$.favoriteActivities").value("walking,running"));
        mvc.perform(put("/api/preferences").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"defaultDuration\":5,\"favoriteActivities\":\"\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void changePasswordRequiresCurrentPasswordAndStrongNewOne() throws Exception {
        String token = register("pwd@example.com", "-");
        String path = "/api/auth/change-password";
        mvc.perform(post(path).header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"Zle!haslo\",\"newPassword\":\"Nowe!haslo\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post(path).header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"Haslo!xyz\",\"newPassword\":\"slabe123\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post(path).header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"Haslo!xyz\",\"newPassword\":\"Nowe!haslo\"}"))
            .andExpect(status().isNoContent());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"pwd@example.com\",\"password\":\"Nowe!haslo\"}"))
            .andExpect(status().isOk());
    }

    @Test
    void deleteAccountRemovesUserAndPlans() throws Exception {
        String token = register("gone@example.com", "-");
        mvc.perform(post("/api/plans").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON).content(PLAN))
            .andExpect(status().isCreated());
        mvc.perform(delete("/api/auth/account").header("Authorization", bearer(token))).andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/profile").header("Authorization", bearer(token))).andExpect(status().isNotFound());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"gone@example.com\",\"password\":\"Haslo!xyz\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void contentIsPublic() throws Exception {
        mvc.perform(get("/api/content/faq")).andExpect(status().isOk()).andExpect(jsonPath("$[0].question").isNotEmpty());
        mvc.perform(get("/api/content/privacy")).andExpect(status().isOk()).andExpect(jsonPath("$.sections[0].title").isNotEmpty());
    }
}
