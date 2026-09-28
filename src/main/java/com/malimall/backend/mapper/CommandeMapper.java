package com.malimall.backend.mapper;

import com.malimall.backend.dto.PositionDto;
import com.malimall.backend.dto.CommandeDtos.CommandeResponse;
import com.malimall.backend.dto.CommandeDtos.LigneCommandeResponse;
import com.malimall.backend.entity.Chauffeur;
import com.malimall.backend.entity.Commande;
import com.malimall.backend.entity.LigneCommande;
import com.malimall.backend.entity.Utilisateur;
import com.malimall.backend.entity.Vehicule;
import com.malimall.backend.entity.enums.StatutCommande;
import com.malimall.backend.entity.enums.StatutLivraison;

import java.util.List;

/**
 * Applique ici, au dernier moment avant sérialisation, le modèle de sécurité
 * à deux codes des maquettes :
 * - codeRetrait : détenu par le vendeur propriétaire de la boutique (et
 *   l'admin). Le vendeur le présente au livreur, qui le scanne (maquettes
 *   28 et 32) : il n'est jamais montré ni au livreur ni à l'acheteur.
 * - codeLivraison : détenu par le livreur assigné, et seulement une fois le
 *   colis récupéré chez le vendeur. Le livreur le présente à l'acheteur, qui
 *   le scanne pour confirmer la réception (maquettes 34 et 26) : l'acheteur
 *   ne le voit donc jamais dans la réponse API.
 * Un même mapper centralise la règle une seule fois plutôt que de la
 * disperser dans chaque contrôleur qui renvoie une Commande.
 */
public final class CommandeMapper {

    private CommandeMapper() {}

    public static CommandeResponse toResponse(Commande commande, Long viewerUtilisateurId, boolean viewerEstAdmin) {
        boolean estAcheteur = commande.getAcheteur().getId().equals(viewerUtilisateurId);
        boolean estVendeurProprietaire = commande.getBoutique().getProprietaire()
                .getUtilisateur().getId().equals(viewerUtilisateurId);

        // En retrait sur place, c'est le CLIENT qui présente le code retrait au vendeur (pas de
        // livreur) : il doit donc le voir lui aussi, contrairement au circuit avec livreur normal.
        String codeRetrait = (viewerEstAdmin || estVendeurProprietaire
                || (commande.isRetraitParClient() && estAcheteur)) ? commande.getCodeRetrait() : null;

        boolean colisRecupere = commande.getStatutLivraison() == StatutLivraison.RECUPEREE
                || commande.getStatutLivraison() == StatutLivraison.EN_ROUTE
                || commande.getStatutLivraison() == StatutLivraison.LIVREE;
        Chauffeur chauffeur = commande.getChauffeur();
        boolean estChauffeurAssigne = chauffeur != null
                && chauffeur.getUtilisateur().getId().equals(viewerUtilisateurId);
        String codeLivraison = (viewerEstAdmin || (estChauffeurAssigne && colisRecupere))
                ? commande.getCodeLivraison() : null;

        Long chauffeurId = chauffeur != null ? chauffeur.getId() : null;
        String chauffeurNom = chauffeur != null ? nomCourt(chauffeur.getUtilisateur()) : null;
        // Téléphone du livreur : utile à l'acheteur et au vendeur pour le joindre.
        String chauffeurTelephone = chauffeur != null && (estAcheteur || estVendeurProprietaire || viewerEstAdmin)
                ? chauffeur.getUtilisateur().getTelephone() : null;
        Vehicule vehicule = chauffeur != null ? chauffeur.getVehicule() : null;
        // Position en direct : seulement pendant la livraison, et seulement pour les parties concernées.
        boolean livraisonEnCours = commande.getStatut() != StatutCommande.LIVREE
                && commande.getStatut() != StatutCommande.ANNULEE;
        PositionDto positionLivreur = livraisonEnCours && (estAcheteur || estVendeurProprietaire || viewerEstAdmin)
                ? PositionDto.recente(chauffeur) : null;

        List<LigneCommandeResponse> lignes = commande.getLignes().stream()
                .map(CommandeMapper::toLigneResponse)
                .toList();

        return new CommandeResponse(
                commande.getId(),
                commande.getBoutique().getId(),
                commande.getBoutique().getNom(),
                commande.getAcheteur().getId(),
                lignes,
                commande.getMontantTotalMmc(),
                commande.getFraisLivraisonMmc(),
                commande.getStatut(),
                commande.getStatutLivraison(),
                codeRetrait,
                codeLivraison,
                chauffeurId,
                commande.getDateCommande(),
                commande.getDateLivraison(),
                nomCourt(commande.getAcheteur()),
                chauffeurNom,
                chauffeurTelephone,
                vehicule != null ? vehicule.getType().name() : null,
                vehicule != null ? vehicule.getImmatriculation() : null,
                commande.poidsTotalKg(),
                commande.getBoutique().getQuartier(),
                commande.getAdresseLivraison(),
                commande.getDateAssignation(),
                commande.getDateRetrait(),
                commande.getNoteLivreur(),
                commande.isSignalee(),
                commande.isRetraitParClient(),
                commande.getLivraisonLatitude(),
                commande.getLivraisonLongitude(),
                positionLivreur,
                PositionDto.deLaBoutique(commande.getBoutique()));
    }

    /** "Ibrahim Diarra" -> "Ibrahim D." */
    private static String nomCourt(Utilisateur u) {
        String nom = u.getNom() == null || u.getNom().isBlank() ? "" : " " + u.getNom().charAt(0) + ".";
        return u.getPrenom() + nom;
    }

    private static LigneCommandeResponse toLigneResponse(LigneCommande ligne) {
        return new LigneCommandeResponse(
                ligne.getProduit().getId(),
                ligne.getProduit().getNom(),
                ligne.getQuantite(),
                ligne.getPrixUnitaireMmc(),
                ligne.sousTotal());
    }
}
