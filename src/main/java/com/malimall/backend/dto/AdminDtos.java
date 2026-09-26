package com.malimall.backend.dto;

import com.malimall.backend.dto.PubliciteDtos.PubliciteResponse;
import com.malimall.backend.entity.enums.StatutCommande;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

/**
 * DTOs de la console d'administration (web, Angular) — /api/admin/**.
 * Toujours en lecture agrégée à partir des mêmes tables que le reste de
 * l'application ; aucune table dédiée à l'admin.
 */
public class AdminDtos {

    /** Un point de série temporelle pour les mini-graphiques en barres (7 derniers jours). */
    public record PointJournalier(String jour, int total) {}

    public record CommandeApercuResponse(Long id, String produitOuBoutique, String clientNom, int montantMmc, Instant date) {}

    public record CourseApercuResponse(Long id, String chauffeurNom, String trajet, int prixMmc, Instant date) {}

    public record VueEnsembleResponse(
            long totalUtilisateurs,
            long commandesMoisCourant,
            long coursesMoisCourant,
            long volumeMmcMoisCourant,
            double variationCommandesPourcent,
            double variationCoursesPourcent,
            List<PointJournalier> commandesParJour,
            List<PointJournalier> coursesParJour,
            List<CommandeApercuResponse> dernieresCommandes,
            List<CourseApercuResponse> dernieresCourses
    ) {}

    public record AdminBoutiqueResponse(
            Long id,
            String nom,
            String proprietaireNom,
            String proprietaireTelephone,
            int nombreProduits,
            int ventesTotalesMmc,
            boolean certifiee,
            boolean suspendue,
            Instant dateCreation
    ) {}

    public record AdminProduitResponse(Long id, String nom, int prixMmc, int stock, boolean actif) {}

    public record AdminUtilisateurResponse(
            Long id,
            String nomComplet,
            String telephone,
            String email,
            List<String> roles,
            boolean verifie,
            boolean suspendu,
            int soldeMmc,
            int soldeGeleMmc,
            Instant dateCreation
    ) {}

    public record AdminBoutiqueDetailResponse(
            Long id,
            String nom,
            String proprietaireNom,
            String proprietaireTelephone,
            boolean certifiee,
            boolean suspendue,
            Instant dateCreation,
            int nombreProduits,
            int nombreCommandes,
            int ventesTotalesMmc,
            List<AdminProduitResponse> produits,
            List<AdminCommandeResponse> commandesRecentes,
            List<PubliciteResponse> publicites
    ) {}

    public record AdminCommandeResponse(
            Long id,
            String clientNom,
            String boutiqueNom,
            int montantMmc,
            StatutCommande statut,
            boolean signalee,
            Instant date
    ) {}

    /** Une étape du pipeline "deux codes" pour l'écran de traitement d'un litige. */
    public record EtapeLivraison(String libelle, Instant date, boolean echec) {}

    public record SignalementResponse(Long id, String auteurNom, String motif, String description, String statut, Instant date) {}

    public record AdminCommandeDetailResponse(
            Long id,
            String produitNom,
            String clientNom,
            String boutiqueNom,
            String livreurNom,
            int montantMmc,
            StatutCommande statut,
            boolean signalee,
            List<EtapeLivraison> pipeline,
            List<SignalementResponse> signalements
    ) {}

    public enum ActionLitige { CONFIRMER, REGENERER_CODE, REMBOURSER }

    public record ResoudreLitigeRequest(@NotNull ActionLitige action) {}

    public record AdminChauffeurResponse(
            Long id,
            String nom,
            String telephone,
            String vehiculeType,
            String vehiculeImmatriculation,
            java.math.BigDecimal noteMoyenne,
            int nombreCourses,
            int nombreLivraisons,
            boolean disponible,
            boolean suspendu
    ) {}

    public record AdminChauffeurDetailResponse(
            Long id,
            String nom,
            String telephone,
            String vehiculeType,
            String vehiculeImmatriculation,
            java.math.BigDecimal noteMoyenne,
            boolean disponible,
            boolean suspendu,
            Instant inscritDepuis,
            int nombreCourses,
            int nombreLivraisons,
            int revenusGeneresMmc,
            List<CourseApercuResponse> coursesRecentes,
            List<CommandeApercuResponse> livraisonsRecentes
    ) {}

    public record MotoTaxisResponse(
            long nbScooter,
            long nbTelimani,
            long nbTricycle,
            long chauffeursActifs,
            List<AdminChauffeurResponse> chauffeurs,
            List<CourseApercuResponse> coursesRecentes,
            List<CommandeApercuResponse> livraisonsRecentes
    ) {}

    public record TopContributeur(String nom, String sousTitre, int montantMmc) {}

    public record TransactionCommission(
            String type,
            String origine,
            int montantTotalMmc,
            Integer commissionMmc,
            boolean bloqueeParLitige,
            Instant date
    ) {}

    public record RevenusResponse(
            int totalMoisMmc,
            int commissionsMarketMmc,
            int commissionsMotoTaxisMmc,
            int soldePlateformeMmc,
            List<PointJournalier> revenusParJour,
            int partVentesPourcent,
            int partLivraisonsPourcent,
            int partCoursesPourcent,
            List<TopContributeur> topBoutiques,
            List<TopContributeur> topChauffeurs,
            List<TransactionCommission> transactionsRecentes
    ) {}
}
