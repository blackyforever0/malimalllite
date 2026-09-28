package com.malimall.backend.entity;

import com.malimall.backend.entity.enums.StatutCommande;
import com.malimall.backend.entity.enums.StatutLivraison;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Une Commande = un panier validé pour UNE boutique (un retrait/une livraison
 * se fait chez un seul vendeur à la fois). Si le panier contenait des
 * produits de plusieurs boutiques, PanierService.validerPanier() génère
 * une Commande par boutique.
 *
 * montantTotalMmc = total produits uniquement ; fraisLivraisonMmc est
 * distinct, pour appliquer la commission à deux volets de la plateforme
 * (8 % sur chaque montant, séparément).
 */
@Entity
@Table(name = "commande")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Commande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_commande")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acheteur_id", nullable = false)
    private Utilisateur acheteur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "boutique_id", nullable = false)
    private Boutique boutique;

    @OneToMany(mappedBy = "commande", cascade = CascadeType.ALL)
    @Builder.Default
    private List<LigneCommande> lignes = new ArrayList<>();

    @Column(name = "montant_total_mmc", nullable = false)
    private int montantTotalMmc;

    @Column(name = "frais_livraison_mmc", nullable = false)
    private int fraisLivraisonMmc;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatutCommande statut = StatutCommande.EN_ATTENTE;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_livraison", length = 20)
    @Builder.Default
    private StatutLivraison statutLivraison = StatutLivraison.EN_ATTENTE_LIVREUR;

    @Column(name = "code_retrait", length = 6)
    private String codeRetrait;

    @Column(name = "code_livraison", length = 6)
    private String codeLivraison;

    /** Livreur assigné — association ajoutée par rapport au diagramme d'origine (voir classe_v4.drawio). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chauffeur_id")
    private Chauffeur chauffeur;

    @Column(name = "date_commande", nullable = false, updatable = false)
    @Builder.Default
    private Instant dateCommande = Instant.now();

    @Column(name = "date_livraison")
    private Instant dateLivraison;

    /** Adresse (quartier) de livraison saisie à la validation du panier. */
    @Column(name = "adresse_livraison", length = 150)
    private String adresseLivraison;

    /** Point de livraison exact (GPS), facultatif. */
    @Column(name = "livraison_latitude")
    private Double livraisonLatitude;

    @Column(name = "livraison_longitude")
    private Double livraisonLongitude;

    @Column(name = "date_assignation")
    private Instant dateAssignation;

    @Column(name = "date_retrait")
    private Instant dateRetrait;

    /** Note (1 à 5) donnée au livreur par l'acheteur après la livraison. */
    @Column(name = "note_livreur")
    private Integer noteLivreur;

    /** Un signalement est en cours : aucun fonds n'est débloqué tant qu'il n'est pas traité. */
    @Column(nullable = false)
    @Builder.Default
    private boolean signalee = false;

    /**
     * Le client vient récupérer lui-même sa commande chez le vendeur — aucun
     * livreur n'est jamais assigné (statutLivraison reste null), pas de frais
     * de livraison, et codeRetrait est montré au CLIENT (qui le présente au
     * vendeur), plutôt qu'au vendeur seul comme dans le circuit livreur — voir
     * CommandeMapper.
     */
    @Column(name = "retrait_par_client", nullable = false)
    @Builder.Default
    private boolean retraitParClient = false;

    @Transient
    public int montantTotalAvecLivraison() {
        return montantTotalMmc + fraisLivraisonMmc;
    }

    /** Poids total du colis (poids unitaire × quantité, toutes lignes). */
    @Transient
    public java.math.BigDecimal poidsTotalKg() {
        return lignes.stream()
                .map(l -> l.getProduit().getPoidsKg().multiply(java.math.BigDecimal.valueOf(l.getQuantite())))
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
    }
}
