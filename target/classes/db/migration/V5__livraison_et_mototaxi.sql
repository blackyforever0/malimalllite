-- Livraison Market enrichie (poids, quartiers, suivi horodaté, notation,
-- signalements) et module MotoTaxi (courses passagers).

-- Poids d'un produit : sert à vérifier que le véhicule du livreur peut
-- transporter la commande (Scooter <= 10 kg, Telimani <= 20 kg, TriCycle sans limite).
ALTER TABLE produit ADD COLUMN poids_kg NUMERIC(6,2) NOT NULL DEFAULT 1.00;

-- Quartier de la boutique (point de retrait) et adresse de livraison de la commande.
ALTER TABLE boutique ADD COLUMN quartier VARCHAR(100);
ALTER TABLE commande ADD COLUMN adresse_livraison VARCHAR(150);

-- Suivi horodaté de la livraison, note donnée au livreur, signalement en cours.
ALTER TABLE commande ADD COLUMN date_assignation TIMESTAMP;
ALTER TABLE commande ADD COLUMN date_retrait TIMESTAMP;
ALTER TABLE commande ADD COLUMN note_livreur INT;
ALTER TABLE commande ADD COLUMN signalee BOOLEAN NOT NULL DEFAULT FALSE;

-- Nombre de notes reçues (pour recalculer note_moyenne) et numéro de permis.
ALTER TABLE chauffeur ADD COLUMN nombre_notes INT NOT NULL DEFAULT 0;
ALTER TABLE chauffeur ADD COLUMN numero_permis VARCHAR(40);

-- Courses MotoTaxi : le client demande un trajet entre deux quartiers, un
-- chauffeur (véhicule transportant des passagers) l'accepte puis la termine.
CREATE TABLE course (
    id_course         BIGSERIAL PRIMARY KEY,
    client_id         BIGINT       NOT NULL REFERENCES utilisateur (id_utilisateur),
    chauffeur_id      BIGINT       REFERENCES chauffeur (id_chauffeur),
    depart            VARCHAR(100) NOT NULL,
    destination       VARCHAR(100) NOT NULL,
    distance_km       NUMERIC(5,1) NOT NULL,
    prix_mmc          INT          NOT NULL,
    statut            VARCHAR(20)  NOT NULL DEFAULT 'DEMANDEE',
    note              INT,
    commentaire       VARCHAR(500),
    date_demande      TIMESTAMP    NOT NULL DEFAULT now(),
    date_acceptation  TIMESTAMP,
    date_fin          TIMESTAMP
);
CREATE INDEX idx_course_client ON course (client_id);
CREATE INDEX idx_course_chauffeur ON course (chauffeur_id);
CREATE INDEX idx_course_statut ON course (statut);

-- Signalements (livraison ou course) : traités par l'équipe MaliMall.
CREATE TABLE signalement (
    id_signalement  BIGSERIAL PRIMARY KEY,
    auteur_id       BIGINT       NOT NULL REFERENCES utilisateur (id_utilisateur),
    commande_id     BIGINT       REFERENCES commande (id_commande),
    course_id       BIGINT       REFERENCES course (id_course),
    motif           VARCHAR(40)  NOT NULL,
    description     VARCHAR(1000),
    statut          VARCHAR(20)  NOT NULL DEFAULT 'OUVERT',
    date_creation   TIMESTAMP    NOT NULL DEFAULT now()
);
CREATE INDEX idx_signalement_commande ON signalement (commande_id);

-- Données de démo : quartiers des boutiques et poids des produits existants.
UPDATE boutique SET quartier = 'Sabalibougou' WHERE quartier IS NULL;
UPDATE produit SET poids_kg = 25.00 WHERE nom = 'Sac de riz 25kg';
UPDATE produit SET poids_kg = 5.00 WHERE nom = 'Bidon d''huile 5L';
