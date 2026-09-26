package com.malimall.backend.service;

import com.malimall.backend.exception.StorageNonConfigureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Set;
import java.util.UUID;

/**
 * Téléverse les photos (produits, boutiques) vers un bucket Supabase Storage,
 * c'est-à-dire du stockage d'objets adressable par
 * URL, pas une base de données. Toujours appelé côté serveur — la clé
 * "service_role" ne doit jamais atteindre le mobile, seule l'URL publique du
 * fichier une fois téléversé lui est renvoyée (dans ProduitResponse.imageUrl /
 * BoutiqueResponse.imageUrl).
 *
 * Configuration (voir application-local.yml pour les valeurs d'exemple) :
 *   malimall.supabase.url          ex: https://<ref>.supabase.co
 *   malimall.supabase.service-key  clé "service_role" (jamais la clé "anon") du projet
 *   malimall.supabase.bucket       nom du bucket, créé au préalable dans le dashboard Supabase
 *
 * Si l'une des deux premières valeurs est absente, le service reste inactif :
 * {@link #televerser} lève {@link StorageNonConfigureException} (mappée en
 * HTTP 503) plutôt que de planter le démarrage de l'application — le reste de
 * l'API continue de fonctionner sans photos.
 */
@Service
public class SupabaseStorageService {

    private static final Set<String> TYPES_AUTORISES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long TAILLE_MAX_OCTETS = 5L * 1024 * 1024; // 5 Mo

    private final boolean configure;
    private final String bucket;
    private final String urlPublique;
    private final RestClient restClient;

    public SupabaseStorageService(
            @Value("${malimall.supabase.url:}") String supabaseUrl,
            @Value("${malimall.supabase.service-key:}") String serviceKey,
            @Value("${malimall.supabase.bucket:malimall-media}") String bucket) {
        this.bucket = bucket;
        this.configure = !supabaseUrl.isBlank() && !serviceKey.isBlank();
        String base = supabaseUrl.replaceAll("/+$", "");
        this.urlPublique = base + "/storage/v1/object/public/" + bucket + "/";
        this.restClient = configure
                ? RestClient.builder()
                    .baseUrl(base + "/storage/v1/object/" + bucket)
                    .defaultHeader("Authorization", "Bearer " + serviceKey)
                    .defaultHeader("apikey", serviceKey)
                    .build()
                : null;
    }

    /**
     * @param dossier   "produits" ou "boutiques" — préfixe logique dans le bucket
     * @param entiteId  id du produit/de la boutique concerné, pour un chemin lisible
     * @return l'URL publique du fichier téléversé, à stocker tel quel dans imageUrl
     */
    public String televerser(String dossier, Long entiteId, MultipartFile fichier) {
        if (!configure) {
            throw new StorageNonConfigureException(
                    "Le stockage des photos (Supabase Storage) n'est pas configuré sur ce serveur : "
                            + "définissez malimall.supabase.url et malimall.supabase.service-key.");
        }
        if (fichier == null || fichier.isEmpty()) {
            throw new IllegalArgumentException("Aucun fichier reçu");
        }
        String contentType = fichier.getContentType();
        if (contentType == null || !TYPES_AUTORISES.contains(contentType)) {
            throw new IllegalArgumentException("Format d'image non supporté (jpeg, png ou webp uniquement)");
        }
        if (fichier.getSize() > TAILLE_MAX_OCTETS) {
            throw new IllegalArgumentException("Image trop volumineuse (5 Mo maximum)");
        }

        String extension = switch (contentType) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "jpg";
        };
        String chemin = dossier + "/" + entiteId + "-" + UUID.randomUUID() + "." + extension;

        try {
            restClient.post()
                    .uri("/" + chemin)
                    .header("x-upsert", "true")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(fichier.getBytes())
                    .retrieve()
                    .toBodilessEntity();
        } catch (IOException e) {
            throw new UncheckedIOException("Lecture du fichier téléversé impossible", e);
        }

        return urlPublique + chemin;
    }

    public boolean estConfigure() {
        return configure;
    }

    public String getBucket() {
        return bucket;
    }
}
