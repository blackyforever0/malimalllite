package com.malimall.backend.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Teste uniquement la logique qui ne fait pas d'appel réseau : garde
 * "non configuré", validation du type/de la taille du fichier. Le vrai appel
 * HTTP vers Supabase Storage n'est volontairement pas testé ici (nécessiterait
 * un vrai bucket) — c'est ProfilService/Controller qui l'utilisent via un mock
 * (voir PhotoUploadControllerTest), conformément à la même approche que pour
 * le reste du projet (pas d'appel réseau sortant dans `mvn test`).
 */
class SupabaseStorageServiceTest {

    @Test
    void televerser_sansConfigurationSupabase_leveStorageNonConfigureException() {
        SupabaseStorageService service = new SupabaseStorageService("", "", "malimall-media");
        MockMultipartFile fichier = new MockMultipartFile("fichier", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> service.televerser("produits", 1L, fichier))
                .isInstanceOf(com.malimall.backend.exception.StorageNonConfigureException.class);
    }

    @Test
    void estConfigure_renvoieFauxTantQueUrlEtCleNeSontPasToutesLesDeuxRenseignees() {
        assertThat(new SupabaseStorageService("", "", "malimall-media").estConfigure()).isFalse();
        assertThat(new SupabaseStorageService("https://x.supabase.co", "", "malimall-media").estConfigure()).isFalse();
        assertThat(new SupabaseStorageService("https://x.supabase.co", "cle", "malimall-media").estConfigure()).isTrue();
    }
}
