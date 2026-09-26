-- MaliMall — schéma initial (Market : catalogue, panier, commande, livraison à deux codes, portefeuille)
-- Reflète fidèlement classe_v4.drawio et les entités JPA de com.malimall.backend.entity.

CREATE TABLE utilisateur (
    id_utilisateur      BIGSERIAL PRIMARY KEY,
    nom                 VARCHAR(100) NOT NULL,
    prenom              VARCHAR(100) NOT NULL,
    telephone           VARCHAR(20)  NOT NULL UNIQUE,
    email               VARCHAR(150) UNIQUE,
    mot_de_passe        VARCHAR(255) NOT NULL,
    statut_verification VARCHAR(20)  NOT NULL DEFAULT 'NON_VERIFIE',
    est_admin           BOOLEAN      NOT NULL DEFAULT FALSE,
    date_creation       TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE client (
    id_client                  BIGSERIAL PRIMARY KEY,
    utilisateur_id             BIGINT NOT NULL UNIQUE REFERENCES utilisateur (id_utilisateur),
    adresse_livraison_defaut   VARCHAR(255)
);

CREATE TABLE vendeur (
    id_vendeur      BIGSERIAL PRIMARY KEY,
    utilisateur_id  BIGINT NOT NULL UNIQUE REFERENCES utilisateur (id_utilisateur),
    ventes_totales  INT    NOT NULL DEFAULT 0
);

CREATE TABLE chauffeur (
    id_chauffeur    BIGSERIAL PRIMARY KEY,
    utilisateur_id  BIGINT       NOT NULL UNIQUE REFERENCES utilisateur (id_utilisateur),
    disponible      BOOLEAN      NOT NULL DEFAULT TRUE,
    note_moyenne    NUMERIC(2,1) NOT NULL DEFAULT 5.0
);

CREATE TABLE vehicule (
    id_vehicule     BIGSERIAL PRIMARY KEY,
    chauffeur_id    BIGINT      NOT NULL UNIQUE REFERENCES chauffeur (id_chauffeur),
    type            VARCHAR(20) NOT NULL,
    immatriculation VARCHAR(20) NOT NULL
);

CREATE TABLE portefeuille (
    id_portefeuille  BIGSERIAL PRIMARY KEY,
    utilisateur_id   BIGINT    NOT NULL UNIQUE REFERENCES utilisateur (id_utilisateur),
    solde_mmc        INT       NOT NULL DEFAULT 0,
    solde_gele_mmc   INT       NOT NULL DEFAULT 0,
    version          BIGINT    NOT NULL DEFAULT 0,
    date_maj         TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE ecriture_comptable (
    id_ecriture     BIGSERIAL PRIMARY KEY,
    utilisateur_id  BIGINT      NOT NULL REFERENCES utilisateur (id_utilisateur),
    type_operation  VARCHAR(20) NOT NULL,
    montant_mmc     INT         NOT NULL,
    reference_type  VARCHAR(20),
    reference_id    BIGINT,
    date_creation   TIMESTAMP   NOT NULL DEFAULT now()
);
CREATE INDEX idx_ecriture_utilisateur ON ecriture_comptable (utilisateur_id, date_creation DESC);
CREATE INDEX idx_ecriture_reference ON ecriture_comptable (reference_type, reference_id);

CREATE TABLE boutique (
    id_boutique     BIGSERIAL PRIMARY KEY,
    proprietaire_id BIGINT       NOT NULL REFERENCES vendeur (id_vendeur),
    nom             VARCHAR(150) NOT NULL,
    description     TEXT,
    categorie       VARCHAR(50),
    certifiee       BOOLEAN      NOT NULL DEFAULT FALSE,
    date_creation   TIMESTAMP    NOT NULL DEFAULT now()
);
CREATE INDEX idx_boutique_proprietaire ON boutique (proprietaire_id);

CREATE TABLE produit (
    id_produit      BIGSERIAL PRIMARY KEY,
    boutique_id     BIGINT       NOT NULL REFERENCES boutique (id_boutique),
    nom             VARCHAR(150) NOT NULL,
    description     TEXT,
    prix_mmc        INT          NOT NULL,
    stock           INT          NOT NULL,
    categorie       VARCHAR(50),
    actif           BOOLEAN      NOT NULL DEFAULT TRUE,
    date_creation   TIMESTAMP    NOT NULL DEFAULT now()
);
CREATE INDEX idx_produit_boutique ON produit (boutique_id);
CREATE INDEX idx_produit_actif ON produit (actif);

CREATE TABLE panier (
    id_panier       BIGSERIAL PRIMARY KEY,
    client_id       BIGINT    NOT NULL UNIQUE REFERENCES client (id_client),
    date_creation   TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE commande (
    id_commande         BIGSERIAL PRIMARY KEY,
    acheteur_id         BIGINT      NOT NULL REFERENCES utilisateur (id_utilisateur),
    boutique_id         BIGINT      NOT NULL REFERENCES boutique (id_boutique),
    montant_total_mmc   INT         NOT NULL,
    frais_livraison_mmc INT         NOT NULL,
    statut              VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE',
    statut_livraison    VARCHAR(20)          DEFAULT 'EN_ATTENTE_LIVREUR',
    code_retrait        VARCHAR(6),
    code_livraison      VARCHAR(6),
    chauffeur_id        BIGINT      REFERENCES chauffeur (id_chauffeur),
    date_commande       TIMESTAMP   NOT NULL DEFAULT now(),
    date_livraison      TIMESTAMP
);
CREATE INDEX idx_commande_acheteur ON commande (acheteur_id);
CREATE INDEX idx_commande_boutique ON commande (boutique_id);
CREATE INDEX idx_commande_chauffeur ON commande (chauffeur_id);

-- ligne_commande rattachée à EXACTEMENT UN parent à la fois (panier XOR commande) :
-- les deux FK sont nullable, jamais toutes deux NULL ni toutes deux renseignées
-- (contrainte appliquée côté service, cf. PanierService.validerPanier()).
CREATE TABLE ligne_commande (
    id_ligne            BIGSERIAL PRIMARY KEY,
    panier_id           BIGINT REFERENCES panier (id_panier),
    commande_id         BIGINT REFERENCES commande (id_commande),
    produit_id          BIGINT NOT NULL REFERENCES produit (id_produit),
    quantite            INT    NOT NULL,
    prix_unitaire_mmc   INT    NOT NULL
);
CREATE INDEX idx_ligne_panier ON ligne_commande (panier_id);
CREATE INDEX idx_ligne_commande ON ligne_commande (commande_id);
