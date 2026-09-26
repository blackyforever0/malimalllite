-- Données de démonstration pour le profil "local" : de quoi dérouler le
-- parcours complet (inscription déjà faite ici) en moins d'une minute via
-- Swagger ou curl, sans dépendre de POST /api/auth/inscription au préalable.
--
-- Mot de passe en clair pour CHAQUE compte de démo : password123
-- (haché BCrypt ci-dessous, généré hors-ligne — cf. commentaire de session,
-- compatible org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder)

-- 1) Portefeuille plateforme — un Utilisateur "système" comme un autre,
--    le téléphone doit correspondre à malimall.plateforme.telephone
--    (application.yml, défaut "+000000000").
INSERT INTO utilisateur (nom, prenom, telephone, email, mot_de_passe, statut_verification, est_admin)
VALUES ('MaliMall', 'Plateforme', '+000000000', 'plateforme@malimall.local',
        '$2a$10$HYFaaHreWD9t8icYdPU1sOhOvbKeDBv6WQKUkIqCMQSWVPqZC3q8u', 'VERIFIE', TRUE);
INSERT INTO client (utilisateur_id, adresse_livraison_defaut)
SELECT id_utilisateur, NULL FROM utilisateur WHERE telephone = '+000000000';
INSERT INTO portefeuille (utilisateur_id, solde_mmc, solde_gele_mmc)
SELECT id_utilisateur, 0, 0 FROM utilisateur WHERE telephone = '+000000000';

-- 2) Admin de démo (pour tester les endpoints /api/admin/**)
INSERT INTO utilisateur (nom, prenom, telephone, email, mot_de_passe, statut_verification, est_admin)
VALUES ('Kaloga', 'Admin', '+22370000001', 'admin@malimall.local',
        '$2a$10$HYFaaHreWD9t8icYdPU1sOhOvbKeDBv6WQKUkIqCMQSWVPqZC3q8u', 'VERIFIE', TRUE);
INSERT INTO client (utilisateur_id) SELECT id_utilisateur FROM utilisateur WHERE telephone = '+22370000001';
INSERT INTO portefeuille (utilisateur_id) SELECT id_utilisateur FROM utilisateur WHERE telephone = '+22370000001';

-- 3) Acheteur de démo — préapprovisionné pour pouvoir valider un panier sans recharge manuelle
INSERT INTO utilisateur (nom, prenom, telephone, email, mot_de_passe, statut_verification, est_admin)
VALUES ('Diarra', 'Aïcha', '+22370000002', 'acheteur@malimall.local',
        '$2a$10$HYFaaHreWD9t8icYdPU1sOhOvbKeDBv6WQKUkIqCMQSWVPqZC3q8u', 'VERIFIE', FALSE);
INSERT INTO client (utilisateur_id) SELECT id_utilisateur FROM utilisateur WHERE telephone = '+22370000002';
INSERT INTO portefeuille (utilisateur_id, solde_mmc)
SELECT id_utilisateur, 50000 FROM utilisateur WHERE telephone = '+22370000002';

-- 4) Vendeur de démo + boutique + 2 produits
INSERT INTO utilisateur (nom, prenom, telephone, email, mot_de_passe, statut_verification, est_admin)
VALUES ('Traoré', 'Moussa', '+22370000003', 'vendeur@malimall.local',
        '$2a$10$HYFaaHreWD9t8icYdPU1sOhOvbKeDBv6WQKUkIqCMQSWVPqZC3q8u', 'VERIFIE', FALSE);
INSERT INTO client (utilisateur_id) SELECT id_utilisateur FROM utilisateur WHERE telephone = '+22370000003';
INSERT INTO portefeuille (utilisateur_id) SELECT id_utilisateur FROM utilisateur WHERE telephone = '+22370000003';
INSERT INTO vendeur (utilisateur_id, ventes_totales)
SELECT id_utilisateur, 0 FROM utilisateur WHERE telephone = '+22370000003';

INSERT INTO boutique (proprietaire_id, nom, description, categorie, certifiee)
SELECT id_vendeur, 'Boutique Traoré', 'Épicerie et produits locaux', 'Alimentation', TRUE
FROM vendeur v JOIN utilisateur u ON u.id_utilisateur = v.utilisateur_id WHERE u.telephone = '+22370000003';

INSERT INTO produit (boutique_id, nom, description, prix_mmc, stock, categorie, actif)
SELECT b.id_boutique, 'Sac de riz 25kg', 'Riz local de qualité', 1800, 40, 'Alimentation', TRUE
FROM boutique b JOIN vendeur v ON v.id_vendeur = b.proprietaire_id
JOIN utilisateur u ON u.id_utilisateur = v.utilisateur_id WHERE u.telephone = '+22370000003';

INSERT INTO produit (boutique_id, nom, description, prix_mmc, stock, categorie, actif)
SELECT b.id_boutique, 'Bidon d''huile 5L', 'Huile végétale', 900, 60, 'Alimentation', TRUE
FROM boutique b JOIN vendeur v ON v.id_vendeur = b.proprietaire_id
JOIN utilisateur u ON u.id_utilisateur = v.utilisateur_id WHERE u.telephone = '+22370000003';

-- 5) Chauffeur de démo + véhicule, disponible
INSERT INTO utilisateur (nom, prenom, telephone, email, mot_de_passe, statut_verification, est_admin)
VALUES ('Coulibaly', 'Ibrahim', '+22370000004', 'chauffeur@malimall.local',
        '$2a$10$HYFaaHreWD9t8icYdPU1sOhOvbKeDBv6WQKUkIqCMQSWVPqZC3q8u', 'VERIFIE', FALSE);
INSERT INTO client (utilisateur_id) SELECT id_utilisateur FROM utilisateur WHERE telephone = '+22370000004';
INSERT INTO portefeuille (utilisateur_id) SELECT id_utilisateur FROM utilisateur WHERE telephone = '+22370000004';
INSERT INTO chauffeur (utilisateur_id, disponible, note_moyenne)
SELECT id_utilisateur, TRUE, 5.0 FROM utilisateur WHERE telephone = '+22370000004';

INSERT INTO vehicule (chauffeur_id, type, immatriculation)
SELECT c.id_chauffeur, 'TELIMANI', 'BKO-1234-A'
FROM chauffeur c JOIN utilisateur u ON u.id_utilisateur = c.utilisateur_id WHERE u.telephone = '+22370000004';
