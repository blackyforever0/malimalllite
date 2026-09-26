package com.malimall.backend.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.malimall.backend.dto.AuthDtos.InscriptionRequest;
import com.malimall.backend.dto.CatalogueDtos.BoutiqueRequest;
import com.malimall.backend.dto.CatalogueDtos.ProduitRequest;
import com.malimall.backend.service.SupabaseStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Vérifie le chemin HTTP complet du téléversement de photo (produit +
 * boutique) : n'appelle jamais le vrai Supabase Storage (SupabaseStorageService
 * est mocké — un appel réseau sortant n'a rien à faire dans une suite de tests
 * unitaires), mais vérifie tout le reste : sérialisation multipart, contrôle
 * "propriétaire uniquement" (403 si le vendeur appelant ne possède pas la
 * boutique/le produit), et que l'URL renvoyée par le service est bien
 * persistée puis restituée dans imageUrl.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PhotoUploadControllerTest {

    private static final String URL_FALSE = "https://fake.supabase.co/storage/v1/object/public/malimall-media/photo.jpg";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private SupabaseStorageService supabaseStorageService;

    private String tokenVendeurA;
    private Long boutiqueId;
    private Long produitId;

    @BeforeEach
    void creerUnVendeurAvecBoutiqueEtProduit() throws Exception {
        when(supabaseStorageService.televerser(anyString(), anyLong(), any())).thenReturn(URL_FALSE);

        tokenVendeurA = inscrireEtDevenirVendeur("+22376000030", "photo.a@example.com");

        MvcResult boutiqueResult = mockMvc.perform(post("/api/boutiques")
                        .header("Authorization", "Bearer " + tokenVendeurA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BoutiqueRequest("Atelier Test", "desc", "Mode", null))))
                .andExpect(status().isOk())
                .andReturn();
        boutiqueId = objectMapper.readTree(boutiqueResult.getResponse().getContentAsString()).get("id").asLong();

        MvcResult produitResult = mockMvc.perform(post("/api/boutiques/{id}/produits", boutiqueId)
                        .header("Authorization", "Bearer " + tokenVendeurA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ProduitRequest("Pagne", "desc", 1500, 10, "Mode", null))))
                .andExpect(status().isOk())
                .andReturn();
        produitId = objectMapper.readTree(produitResult.getResponse().getContentAsString()).get("id").asLong();
    }

    /** Inscription -> devenir-vendeur -> reconnexion (le rôle VENDEUR n'apparaît qu'au prochain jeton). */
    private String inscrireEtDevenirVendeur(String telephone, String email) throws Exception {
        InscriptionRequest inscription = new InscriptionRequest("Test", "Vendeur", telephone, email, "motdepasse1");
        MvcResult inscriptionResult = mockMvc.perform(post("/api/auth/inscription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inscription)))
                .andExpect(status().isOk())
                .andReturn();
        String tokenClient = objectMapper.readTree(inscriptionResult.getResponse().getContentAsString()).get("token").asText();

        mockMvc.perform(post("/api/moi/devenir-vendeur").header("Authorization", "Bearer " + tokenClient))
                .andExpect(status().isOk());

        MvcResult connexionResult = mockMvc.perform(post("/api/auth/connexion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telephone\":\"" + telephone + "\",\"motDePasse\":\"motdepasse1\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(connexionResult.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    void televerserPhotoProduit_parLeProprietaire_metAJourImageUrl() throws Exception {
        MockMultipartFile fichier = new MockMultipartFile("fichier", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/produits/{id}/photo", produitId)
                        .file(fichier)
                        .header("Authorization", "Bearer " + tokenVendeurA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").value(URL_FALSE));
    }

    @Test
    void televerserPhotoBoutique_parLeProprietaire_metAJourImageUrl() throws Exception {
        MockMultipartFile fichier = new MockMultipartFile("fichier", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/boutiques/{id}/photo", boutiqueId)
                        .file(fichier)
                        .header("Authorization", "Bearer " + tokenVendeurA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").value(URL_FALSE));
    }

    @Test
    void televerserPhotoProduit_parUnAutreVendeur_renvoie403() throws Exception {
        String tokenVendeurB = inscrireEtDevenirVendeur("+22376000031", "photo.b@example.com");
        MockMultipartFile fichier = new MockMultipartFile("fichier", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/produits/{id}/photo", produitId)
                        .file(fichier)
                        .header("Authorization", "Bearer " + tokenVendeurB))
                .andExpect(status().isForbidden());
    }

    @Test
    void televerserPhotoBoutique_parUnAutreVendeur_renvoie403() throws Exception {
        String tokenVendeurB = inscrireEtDevenirVendeur("+22376000032", "photo.c@example.com");
        MockMultipartFile fichier = new MockMultipartFile("fichier", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/boutiques/{id}/photo", boutiqueId)
                        .file(fichier)
                        .header("Authorization", "Bearer " + tokenVendeurB))
                .andExpect(status().isForbidden());
    }

    @Test
    void televerserPhoto_sansJeton_renvoie403() throws Exception {
        MockMultipartFile fichier = new MockMultipartFile("fichier", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/produits/{id}/photo", produitId).file(fichier))
                .andExpect(status().isForbidden());
    }

    @Test
    void televerserPhoto_typeDeFichierRefuseParLeService_renvoie400() throws Exception {
        // Simule le service qui rejette un format non supporté (comportement réel de
        // SupabaseStorageService.televerser, testé indépendamment plus bas dans
        // SupabaseStorageServiceTest) — ici on vérifie juste que GlobalExceptionHandler
        // traduit bien IllegalArgumentException en 400 sur ce chemin HTTP précis.
        when(supabaseStorageService.televerser(anyString(), anyLong(), any()))
                .thenThrow(new IllegalArgumentException("Format d'image non supporté (jpeg, png ou webp uniquement)"));
        MockMultipartFile fichier = new MockMultipartFile("fichier", "photo.gif", "image/gif", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/produits/{id}/photo", produitId)
                        .file(fichier)
                        .header("Authorization", "Bearer " + tokenVendeurA))
                .andExpect(status().isBadRequest());
    }

    @Test
    void televerserPhotoProfil_metAJourImageUrlEtEstVisibleViaMoi() throws Exception {
        MockMultipartFile fichier = new MockMultipartFile("fichier", "moi.jpg", "image/jpeg", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/moi/photo").file(fichier).header("Authorization", "Bearer " + tokenVendeurA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").value(URL_FALSE));

        mockMvc.perform(get("/api/moi").header("Authorization", "Bearer " + tokenVendeurA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").value(URL_FALSE))
                .andExpect(jsonPath("$.verifie").value(false))
                .andExpect(jsonPath("$.dateCreation").isNotEmpty());
    }

    @Test
    void televerserPhotoProfil_sansJeton_renvoie403() throws Exception {
        MockMultipartFile fichier = new MockMultipartFile("fichier", "moi.jpg", "image/jpeg", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/moi/photo").file(fichier)).andExpect(status().isForbidden());
    }

    @Test
    void obtenirProduit_renvoieImageUrlNullTantQuAucunePhotoNaEteTeleversee() throws Exception {
        JsonNode produit = objectMapper.readTree(mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/produits/{id}", produitId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        org.assertj.core.api.Assertions.assertThat(produit.get("imageUrl").isNull()).isTrue();
    }
}
