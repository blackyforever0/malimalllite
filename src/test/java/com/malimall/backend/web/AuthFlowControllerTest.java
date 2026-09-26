package com.malimall.backend.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.malimall.backend.dto.AuthDtos.ConnexionRequest;
import com.malimall.backend.dto.AuthDtos.InscriptionRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Vérifie le chemin HTTP complet (filtre JWT + désérialisation/sérialisation
 * Jackson + spring.jpa.open-in-view=false), pas seulement les services
 * directement — c'est à cette frontière qu'une régression de sécurité ou de
 * sérialisation se manifesterait vraiment.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthFlowControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void inscriptionPuisMoi_avecLeJetonEmisRenvoieLeProfilEtSesRoles() throws Exception {
        InscriptionRequest inscription = new InscriptionRequest(
                "Keita", "Fanta", "+22376000010", "fanta.keita@example.com", "motdepasse1");

        MvcResult inscriptionResult = mockMvc.perform(post("/api/auth/inscription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inscription)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.roles[0]").value("ROLE_CLIENT"))
                .andReturn();

        JsonNode corps = objectMapper.readTree(inscriptionResult.getResponse().getContentAsString());
        String token = corps.get("token").asText();

        mockMvc.perform(get("/api/moi").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telephone").value("+22376000010"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_CLIENT"))
                // Jackson sérialise les champs null par défaut (pas de @JsonInclude(NON_NULL)
                // configuré) : le chemin JSON existe donc bien, avec une valeur null.
                .andExpect(jsonPath("$.vendeurId").value(nullValue()))
                .andExpect(jsonPath("$.chauffeurId").value(nullValue()));
    }

    @Test
    void inscriptionAvecUnTelephoneDejaUtilise_renvoie400() throws Exception {
        InscriptionRequest inscription = new InscriptionRequest(
                "Keita", "Fanta", "+22376000011", "fanta2@example.com", "motdepasse1");

        mockMvc.perform(post("/api/auth/inscription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inscription)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/inscription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inscription)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void connexionAvecUnMauvaisMotDePasse_renvoie401() throws Exception {
        InscriptionRequest inscription = new InscriptionRequest(
                "Keita", "Fanta", "+22376000012", "fanta3@example.com", "motdepasse1");
        mockMvc.perform(post("/api/auth/inscription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inscription)))
                .andExpect(status().isOk());

        ConnexionRequest mauvaiseConnexion = new ConnexionRequest("+22376000012", "mauvais-mot-de-passe");
        mockMvc.perform(post("/api/auth/connexion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mauvaiseConnexion)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Sans jeton, Spring Security renvoie 403 (pas 401) faute d'AuthenticationEntryPoint dédié — comportement déjà observé et documenté, pas un bug")
    void accesSansJeton_renvoie403() throws Exception {
        mockMvc.perform(get("/api/panier"))
                .andExpect(status().isForbidden());
    }
}
