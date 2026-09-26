package com.malimall.backend.dto;

import com.malimall.backend.entity.enums.StatutCommande;
import com.malimall.backend.entity.enums.StatutLivraison;
import com.malimall.backend.entity.enums.MotifSignalement;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

import java.time.Instant;
import java.util.List;

public class CommandeDtos {

    public record ScannerCodeRequest(@NotBlank String code) {}

    public record NoterLivreurRequest(@Min(1) @Max(5) int note) {}

    public record SignalerRequest(@NotNull MotifSignalement motif, @Size(max = 1000) String description) {}

    public record LigneCommandeResponse(
            Long produitId,
            String produitNom,
            int quantite,
            int prixUnitaireMmc,
            int sousTotal
    ) {}

    /**
     * codeRetrait / codeLivraison sont volontairement nullable ici : le
     * mapper (CommandeMapper) ne les remplit QUE pour le détenteur légitime
     * de chaque code (le vendeur pour codeRetrait, le livreur assigné pour
     * codeLivraison une fois le colis récupéré) — voir CommandeMapper.
     */
    public record CommandeResponse(
            Long id,
            Long boutiqueId,
            String boutiqueNom,
            Long acheteurId,
            List<LigneCommandeResponse> lignes,
            int montantTotalMmc,
            int fraisLivraisonMmc,
            StatutCommande statut,
            StatutLivraison statutLivraison,
            String codeRetrait,
            String codeLivraison,
            Long chauffeurId,
            Instant dateCommande,
            Instant dateLivraison,
            /** "Prénom N." — affiché au vendeur et au livreur. */
            String acheteurNom,
            /** Renseignés dès qu'un livreur est assigné. */
            String chauffeurNom,
            String chauffeurTelephone,
            String vehiculeType,
            String vehiculeImmatriculation,
            /** Poids total du colis, quartier de retrait et adresse de livraison. */
            BigDecimal poidsTotalKg,
            String boutiqueQuartier,
            String adresseLivraison,
            Instant dateAssignation,
            Instant dateRetrait,
            Integer noteLivreur,
            boolean signalee,
            /** Point de livraison exact choisi par l'acheteur (facultatif). */
            Double livraisonLatitude,
            Double livraisonLongitude,
            /** Position en direct du livreur pendant la livraison, pour l'acheteur et le vendeur. */
            com.malimall.backend.dto.PositionDto chauffeurPosition
    ) {}
}
