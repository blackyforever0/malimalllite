-- Données de démo étendues : la V2 posait le strict minimum pour dérouler un
-- parcours technique ; celle-ci peuple les écrans (console admin + mobile)
-- pour qu'ils montrent un catalogue, des livraisons, des courses MotoTaxi et
-- des revenus réalistes, plutôt qu'une application vide.
--
-- Mot de passe en clair pour CHAQUE compte de démo ci-dessous : password123
-- (même hachage BCrypt que V2, réutilisé pour rester cohérent).

-- =====================================================================
-- 1) VENDEURS SUPPLÉMENTAIRES + BOUTIQUES + PRODUITS
--    Catégories volontairement partagées entre boutiques pour que la
--    section "Produits similaires" (fiche produit) ait de vraies
--    correspondances, et pour peupler les filtres par catégorie du Market.
-- =====================================================================

-- Vendeuse 2 — Boutique Kaloga Mode (Habillement), certifiée
INSERT INTO utilisateur (nom, prenom, telephone, email, mot_de_passe, statut_verification, est_admin)
VALUES ('Kaloga', 'Fatoumata', '+22370000010', 'fatoumata.boutique@malimall.local',
        '$2a$10$HYFaaHreWD9t8icYdPU1sOhOvbKeDBv6WQKUkIqCMQSWVPqZC3q8u', 'VERIFIE', FALSE);
INSERT INTO client (utilisateur_id) SELECT id_utilisateur FROM utilisateur WHERE telephone = '+22370000010';
INSERT INTO portefeuille (utilisateur_id, solde_mmc) SELECT id_utilisateur, 15000 FROM utilisateur WHERE telephone = '+22370000010';
INSERT INTO vendeur (utilisateur_id, ventes_totales) SELECT id_utilisateur, 12 FROM utilisateur WHERE telephone = '+22370000010';
INSERT INTO boutique (proprietaire_id, nom, description, categorie, certifiee, quartier)
SELECT id_vendeur, 'Kaloga Mode', 'Vêtements et tissus wax pour toute la famille', 'Habillement', TRUE, 'Hippodrome'
FROM vendeur v JOIN utilisateur u ON u.id_utilisateur = v.utilisateur_id WHERE u.telephone = '+22370000010';

-- Vendeur 3 — Électro Bamako (Électronique), non certifié
INSERT INTO utilisateur (nom, prenom, telephone, email, mot_de_passe, statut_verification, est_admin)
VALUES ('Keita', 'Oumar', '+22370000011', 'oumar.electro@malimall.local',
        '$2a$10$HYFaaHreWD9t8icYdPU1sOhOvbKeDBv6WQKUkIqCMQSWVPqZC3q8u', 'VERIFIE', FALSE);
INSERT INTO client (utilisateur_id) SELECT id_utilisateur FROM utilisateur WHERE telephone = '+22370000011';
INSERT INTO portefeuille (utilisateur_id, solde_mmc) SELECT id_utilisateur, 8000 FROM utilisateur WHERE telephone = '+22370000011';
INSERT INTO vendeur (utilisateur_id, ventes_totales) SELECT id_utilisateur, 5 FROM utilisateur WHERE telephone = '+22370000011';
INSERT INTO boutique (proprietaire_id, nom, description, categorie, certifiee, quartier)
SELECT id_vendeur, 'Électro Bamako', 'Téléphones, accessoires et petit électroménager', 'Électronique', FALSE, 'Badalabougou'
FROM vendeur v JOIN utilisateur u ON u.id_utilisateur = v.utilisateur_id WHERE u.telephone = '+22370000011';

-- Vendeur 4 — Fraîcheur du Fleuve (Alimentation, même catégorie que Boutique Traoré),
-- suspendue par un admin : sert de cas de test pour la console (boutique suspendue).
INSERT INTO utilisateur (nom, prenom, telephone, email, mot_de_passe, statut_verification, est_admin)
VALUES ('Diallo', 'Sekou', '+22370000012', 'sekou.fraicheur@malimall.local',
        '$2a$10$HYFaaHreWD9t8icYdPU1sOhOvbKeDBv6WQKUkIqCMQSWVPqZC3q8u', 'VERIFIE', FALSE);
INSERT INTO client (utilisateur_id) SELECT id_utilisateur FROM utilisateur WHERE telephone = '+22370000012';
INSERT INTO portefeuille (utilisateur_id, solde_mmc) SELECT id_utilisateur, 3000 FROM utilisateur WHERE telephone = '+22370000012';
INSERT INTO vendeur (utilisateur_id, ventes_totales) SELECT id_utilisateur, 2 FROM utilisateur WHERE telephone = '+22370000012';
INSERT INTO boutique (proprietaire_id, nom, description, categorie, certifiee, quartier, suspendue)
SELECT id_vendeur, 'Fraîcheur du Fleuve', 'Poissons et produits frais du fleuve Niger', 'Alimentation', FALSE, 'Niarela', TRUE
FROM vendeur v JOIN utilisateur u ON u.id_utilisateur = v.utilisateur_id WHERE u.telephone = '+22370000012';

-- Produits — Boutique Traoré (Alimentation) : compléments avec une promo active
INSERT INTO produit (boutique_id, nom, description, prix_mmc, stock, categorie, actif, poids_kg)
SELECT b.id_boutique, 'Sac de mil 25kg', 'Mil local pour couscous et bouillie', 1500, 25, 'Alimentation', TRUE, 25.00
FROM boutique b WHERE b.nom = 'Boutique Traoré';
INSERT INTO produit (boutique_id, nom, description, prix_mmc, stock, categorie, actif, poids_kg, en_promotion, promo_prix_mmc, promo_fin_at)
SELECT b.id_boutique, 'Sucre en poudre 1kg', 'Sucre blanc raffiné', 700, 80, 'Alimentation', TRUE, 1.00, TRUE, 550, now() + interval '10 days'
FROM boutique b WHERE b.nom = 'Boutique Traoré';
INSERT INTO produit (boutique_id, nom, description, prix_mmc, stock, categorie, actif, poids_kg)
SELECT b.id_boutique, 'Lait en poudre 400g', 'Lait entier en boîte', 1200, 0, 'Alimentation', TRUE, 0.50
FROM boutique b WHERE b.nom = 'Boutique Traoré';

-- Produits — Kaloga Mode (Habillement)
INSERT INTO produit (boutique_id, nom, description, prix_mmc, stock, categorie, actif, poids_kg)
SELECT b.id_boutique, 'Boubou wax homme', 'Boubou brodé en tissu wax, plusieurs tailles', 12000, 8, 'Habillement', TRUE, 0.80
FROM boutique b WHERE b.nom = 'Kaloga Mode';
INSERT INTO produit (boutique_id, nom, description, prix_mmc, stock, categorie, actif, poids_kg, en_promotion, promo_prix_mmc, promo_fin_at)
SELECT b.id_boutique, 'Robe pagne femme', 'Robe cousue sur mesure, tissu pagne', 9500, 6, 'Habillement', TRUE, 0.60, TRUE, 7500, now() + interval '5 days'
FROM boutique b WHERE b.nom = 'Kaloga Mode';
INSERT INTO produit (boutique_id, nom, description, prix_mmc, stock, categorie, actif, poids_kg)
SELECT b.id_boutique, 'Sandales en cuir', 'Sandales artisanales, cuir véritable', 4500, 15, 'Habillement', TRUE, 0.40
FROM boutique b WHERE b.nom = 'Kaloga Mode';
INSERT INTO produit (boutique_id, nom, description, prix_mmc, stock, categorie, actif, poids_kg)
SELECT b.id_boutique, 'Foulard en soie', 'Foulard imprimé motifs traditionnels', 3200, 20, 'Habillement', TRUE, 0.10
FROM boutique b WHERE b.nom = 'Kaloga Mode';

-- Produits — Électro Bamako (Électronique)
INSERT INTO produit (boutique_id, nom, description, prix_mmc, stock, categorie, actif, poids_kg)
SELECT b.id_boutique, 'Chargeur téléphone rapide', 'Chargeur USB-C 25W', 3500, 30, 'Électronique', TRUE, 0.15
FROM boutique b WHERE b.nom = 'Électro Bamako';
INSERT INTO produit (boutique_id, nom, description, prix_mmc, stock, categorie, actif, poids_kg, en_promotion, promo_prix_mmc, promo_fin_at)
SELECT b.id_boutique, 'Enceinte Bluetooth portable', 'Autonomie 10h, résistante aux éclaboussures', 18000, 10, 'Électronique', TRUE, 1.20, TRUE, 14500, now() + interval '3 days'
FROM boutique b WHERE b.nom = 'Électro Bamako';
INSERT INTO produit (boutique_id, nom, description, prix_mmc, stock, categorie, actif, poids_kg)
SELECT b.id_boutique, 'Ventilateur de bureau', 'Ventilateur 3 vitesses, base stable', 9000, 0, 'Électronique', TRUE, 2.50
FROM boutique b WHERE b.nom = 'Électro Bamako';
INSERT INTO produit (boutique_id, nom, description, prix_mmc, stock, categorie, actif, poids_kg)
SELECT b.id_boutique, 'Écouteurs filaires', 'Micro intégré, jack 3.5mm', 2000, 45, 'Électronique', TRUE, 0.05
FROM boutique b WHERE b.nom = 'Électro Bamako';

-- Produits — Fraîcheur du Fleuve (Alimentation) : boutique suspendue, produits désactivés en cascade
INSERT INTO produit (boutique_id, nom, description, prix_mmc, stock, categorie, actif, poids_kg)
SELECT b.id_boutique, 'Capitaine fumé (1kg)', 'Poisson fumé traditionnel', 4000, 12, 'Alimentation', FALSE, 1.00
FROM boutique b WHERE b.nom = 'Fraîcheur du Fleuve';

-- =====================================================================
-- 2) CLIENTS SUPPLÉMENTAIRES
-- =====================================================================
INSERT INTO utilisateur (nom, prenom, telephone, email, mot_de_passe, statut_verification, est_admin)
VALUES ('Cissé', 'Mariam', '+22370000020', 'mariam.cisse@malimall.local',
        '$2a$10$HYFaaHreWD9t8icYdPU1sOhOvbKeDBv6WQKUkIqCMQSWVPqZC3q8u', 'VERIFIE', FALSE);
INSERT INTO client (utilisateur_id, adresse_livraison_defaut) SELECT id_utilisateur, 'Rue 25, Porte 118, Badalabougou' FROM utilisateur WHERE telephone = '+22370000020';
INSERT INTO portefeuille (utilisateur_id, solde_mmc) SELECT id_utilisateur, 22000 FROM utilisateur WHERE telephone = '+22370000020';

INSERT INTO utilisateur (nom, prenom, telephone, email, mot_de_passe, statut_verification, est_admin)
VALUES ('Togola', 'Boubacar', '+22370000021', 'boubacar.togola@malimall.local',
        '$2a$10$HYFaaHreWD9t8icYdPU1sOhOvbKeDBv6WQKUkIqCMQSWVPqZC3q8u', 'VERIFIE', FALSE);
INSERT INTO client (utilisateur_id, adresse_livraison_defaut) SELECT id_utilisateur, 'Avenue Cheick Zayed, Hippodrome' FROM utilisateur WHERE telephone = '+22370000021';
INSERT INTO portefeuille (utilisateur_id, solde_mmc) SELECT id_utilisateur, 5000 FROM utilisateur WHERE telephone = '+22370000021';

INSERT INTO utilisateur (nom, prenom, telephone, email, mot_de_passe, statut_verification, est_admin)
VALUES ('Sangaré', 'Kadiatou', '+22370000022', 'kadiatou.sangare@malimall.local',
        '$2a$10$HYFaaHreWD9t8icYdPU1sOhOvbKeDBv6WQKUkIqCMQSWVPqZC3q8u', 'NON_VERIFIE', FALSE);
INSERT INTO client (utilisateur_id) SELECT id_utilisateur FROM utilisateur WHERE telephone = '+22370000022';
INSERT INTO portefeuille (utilisateur_id, solde_mmc) SELECT id_utilisateur, 1200 FROM utilisateur WHERE telephone = '+22370000022';

-- =====================================================================
-- 3) CHAUFFEURS SUPPLÉMENTAIRES + VÉHICULES + POSITIONS GPS
--    Positions autour de Bamako, fraîches (now()) pour apparaître sur la
--    carte MotoTaxi et le panneau "Livreurs à proximité" juste après le
--    redémarrage du backend (la fraîcheur GPS expire au bout de 10 min,
--    voir PositionDto.FRAICHEUR_MAX — redémarrer juste avant la démo).
-- =====================================================================
INSERT INTO utilisateur (nom, prenom, telephone, email, mot_de_passe, statut_verification, est_admin)
VALUES ('Konaté', 'Salif', '+22370000030', 'salif.konate@malimall.local',
        '$2a$10$HYFaaHreWD9t8icYdPU1sOhOvbKeDBv6WQKUkIqCMQSWVPqZC3q8u', 'VERIFIE', FALSE);
INSERT INTO client (utilisateur_id) SELECT id_utilisateur FROM utilisateur WHERE telephone = '+22370000030';
INSERT INTO portefeuille (utilisateur_id) SELECT id_utilisateur FROM utilisateur WHERE telephone = '+22370000030';
INSERT INTO chauffeur (utilisateur_id, disponible, note_moyenne, nombre_notes, numero_permis, latitude, longitude, position_maj)
SELECT id_utilisateur, TRUE, 4.8, 23, 'PM-2024-00891', 12.6438, -8.0026, now() FROM utilisateur WHERE telephone = '+22370000030';
INSERT INTO vehicule (chauffeur_id, type, immatriculation)
SELECT c.id_chauffeur, 'SCOOTER', 'BKO-5521-B' FROM chauffeur c JOIN utilisateur u ON u.id_utilisateur = c.utilisateur_id WHERE u.telephone = '+22370000030';

INSERT INTO utilisateur (nom, prenom, telephone, email, mot_de_passe, statut_verification, est_admin)
VALUES ('Doumbia', 'Adama', '+22370000031', 'adama.doumbia@malimall.local',
        '$2a$10$HYFaaHreWD9t8icYdPU1sOhOvbKeDBv6WQKUkIqCMQSWVPqZC3q8u', 'VERIFIE', FALSE);
INSERT INTO client (utilisateur_id) SELECT id_utilisateur FROM utilisateur WHERE telephone = '+22370000031';
INSERT INTO portefeuille (utilisateur_id) SELECT id_utilisateur FROM utilisateur WHERE telephone = '+22370000031';
INSERT INTO chauffeur (utilisateur_id, disponible, note_moyenne, nombre_notes, numero_permis, latitude, longitude, position_maj)
SELECT id_utilisateur, TRUE, 4.6, 15, 'PM-2024-00456', 12.6255, -7.9910, now() FROM utilisateur WHERE telephone = '+22370000031';
INSERT INTO vehicule (chauffeur_id, type, immatriculation)
SELECT c.id_chauffeur, 'TRICYCLE', 'BKO-7788-C' FROM chauffeur c JOIN utilisateur u ON u.id_utilisateur = c.utilisateur_id WHERE u.telephone = '+22370000031';

INSERT INTO utilisateur (nom, prenom, telephone, email, mot_de_passe, statut_verification, est_admin)
VALUES ('Fofana', 'Yacouba', '+22370000032', 'yacouba.fofana@malimall.local',
        '$2a$10$HYFaaHreWD9t8icYdPU1sOhOvbKeDBv6WQKUkIqCMQSWVPqZC3q8u', 'VERIFIE', FALSE);
INSERT INTO client (utilisateur_id) SELECT id_utilisateur FROM utilisateur WHERE telephone = '+22370000032';
INSERT INTO portefeuille (utilisateur_id) SELECT id_utilisateur FROM utilisateur WHERE telephone = '+22370000032';
-- Hors ligne (pas de disponibilité, pas de position) : sert de cas "chauffeur non affiché".
INSERT INTO chauffeur (utilisateur_id, disponible, note_moyenne, nombre_notes, numero_permis)
SELECT id_utilisateur, FALSE, 4.2, 9, 'PM-2023-00218' FROM utilisateur WHERE telephone = '+22370000032';
INSERT INTO vehicule (chauffeur_id, type, immatriculation)
SELECT c.id_chauffeur, 'TELIMANI', 'BKO-3344-D' FROM chauffeur c JOIN utilisateur u ON u.id_utilisateur = c.utilisateur_id WHERE u.telephone = '+22370000032';

-- Position fraîche pour le chauffeur déjà créé en V2 (Ibrahim Coulibaly), pour qu'il
-- apparaisse aussi sur la carte juste après le redémarrage.
UPDATE chauffeur SET latitude = 12.6392, longitude = -8.0029, position_maj = now()
WHERE utilisateur_id = (SELECT id_utilisateur FROM utilisateur WHERE telephone = '+22370000004');

-- Chauffeur suspendu (cas de test admin)
INSERT INTO utilisateur (nom, prenom, telephone, email, mot_de_passe, statut_verification, est_admin)
VALUES ('Camara', 'Lassana', '+22370000033', 'lassana.camara@malimall.local',
        '$2a$10$HYFaaHreWD9t8icYdPU1sOhOvbKeDBv6WQKUkIqCMQSWVPqZC3q8u', 'VERIFIE', FALSE);
INSERT INTO client (utilisateur_id) SELECT id_utilisateur FROM utilisateur WHERE telephone = '+22370000033';
INSERT INTO portefeuille (utilisateur_id) SELECT id_utilisateur FROM utilisateur WHERE telephone = '+22370000033';
INSERT INTO chauffeur (utilisateur_id, disponible, note_moyenne, nombre_notes, numero_permis, suspendu)
SELECT id_utilisateur, FALSE, 3.1, 6, 'PM-2022-00099', TRUE FROM utilisateur WHERE telephone = '+22370000033';
INSERT INTO vehicule (chauffeur_id, type, immatriculation)
SELECT c.id_chauffeur, 'SCOOTER', 'BKO-9012-E' FROM chauffeur c JOIN utilisateur u ON u.id_utilisateur = c.utilisateur_id WHERE u.telephone = '+22370000033';

-- =====================================================================
-- 4) COMMANDES (Market) — plusieurs statuts, pour peupler l'historique
--    acheteur, l'espace vendeur et la console admin (Commandes/Litiges).
-- =====================================================================

-- 4a) Commande livrée (acheteur Aïcha Diarra, boutique Traoré)
INSERT INTO commande (acheteur_id, boutique_id, montant_total_mmc, frais_livraison_mmc, statut, statut_livraison,
                       code_retrait, code_livraison, chauffeur_id, adresse_livraison, date_commande, date_assignation, date_retrait, date_livraison, note_livreur)
SELECT u.id_utilisateur, b.id_boutique, 3600, 500, 'LIVREE', 'LIVREE', '482913', '117260',
       c.id_chauffeur, 'Rue 12, Porte 45, Sabalibougou',
       now() - interval '4 days', now() - interval '4 days' + interval '20 minutes',
       now() - interval '4 days' + interval '40 minutes', now() - interval '4 days' + interval '80 minutes', 5
FROM utilisateur u, boutique b, chauffeur c
JOIN utilisateur uc ON uc.id_utilisateur = c.utilisateur_id
WHERE u.telephone = '+22370000002' AND b.nom = 'Boutique Traoré' AND uc.telephone = '+22370000004';

INSERT INTO ligne_commande (commande_id, produit_id, quantite, prix_unitaire_mmc)
SELECT co.id_commande, p.id_produit, 2, 1800
FROM commande co, produit p WHERE co.code_retrait = '482913' AND p.nom = 'Sac de riz 25kg';

-- 4b) Commande en livraison en cours (Mariam Cissé, boutique Kaloga Mode)
INSERT INTO commande (acheteur_id, boutique_id, montant_total_mmc, frais_livraison_mmc, statut, statut_livraison,
                       code_retrait, code_livraison, chauffeur_id, adresse_livraison, date_commande, date_assignation, date_retrait)
SELECT u.id_utilisateur, b.id_boutique, 9500, 600, 'EN_LIVRAISON', 'EN_ROUTE', '635208', '904471',
       c.id_chauffeur, 'Rue 25, Porte 118, Badalabougou',
       now() - interval '35 minutes', now() - interval '25 minutes', now() - interval '10 minutes'
FROM utilisateur u, boutique b, chauffeur c
JOIN utilisateur uc ON uc.id_utilisateur = c.utilisateur_id
WHERE u.telephone = '+22370000020' AND b.nom = 'Kaloga Mode' AND uc.telephone = '+22370000030';

INSERT INTO ligne_commande (commande_id, produit_id, quantite, prix_unitaire_mmc)
SELECT co.id_commande, p.id_produit, 1, 7500
FROM commande co, produit p WHERE co.code_retrait = '635208' AND p.nom = 'Robe pagne femme';

-- 4c) Commande en attente de livreur (Boubacar Togola, Électro Bamako)
INSERT INTO commande (acheteur_id, boutique_id, montant_total_mmc, frais_livraison_mmc, statut, statut_livraison,
                       code_retrait, code_livraison, adresse_livraison, date_commande)
SELECT u.id_utilisateur, b.id_boutique, 14500, 700, 'CONFIRMEE', 'EN_ATTENTE_LIVREUR', '221047', '558639',
       'Avenue Cheick Zayed, Hippodrome', now() - interval '8 minutes'
FROM utilisateur u, boutique b WHERE u.telephone = '+22370000021' AND b.nom = 'Électro Bamako';

INSERT INTO ligne_commande (commande_id, produit_id, quantite, prix_unitaire_mmc)
SELECT co.id_commande, p.id_produit, 1, 14500
FROM commande co, produit p WHERE co.code_retrait = '221047' AND p.nom = 'Enceinte Bluetooth portable';

-- 4d) Commande annulée (Kadiatou Sangaré, Boutique Traoré)
INSERT INTO commande (acheteur_id, boutique_id, montant_total_mmc, frais_livraison_mmc, statut,
                       code_retrait, code_livraison, adresse_livraison, date_commande)
SELECT u.id_utilisateur, b.id_boutique, 700, 500, 'ANNULEE', '349612', '806355',
       'Quartier Niarela', now() - interval '2 days'
FROM utilisateur u, boutique b WHERE u.telephone = '+22370000022' AND b.nom = 'Boutique Traoré';

INSERT INTO ligne_commande (commande_id, produit_id, quantite, prix_unitaire_mmc)
SELECT co.id_commande, p.id_produit, 1, 700
FROM commande co, produit p WHERE co.code_retrait = '349612' AND p.nom = 'Sucre en poudre 1kg';

-- 4e) Commande en livraison, signalée (colis endommagé) — pour l'écran admin
--     "Litiges" : livreur déjà en route, fonds encore gelés, en attente de la
--     décision de l'admin (Confirmer / Régénérer le code / Rembourser).
INSERT INTO commande (acheteur_id, boutique_id, montant_total_mmc, frais_livraison_mmc, statut, statut_livraison,
                       code_retrait, code_livraison, chauffeur_id, adresse_livraison, date_commande, date_assignation, date_retrait, signalee)
SELECT u.id_utilisateur, b.id_boutique, 12000, 500, 'EN_LIVRAISON', 'EN_ROUTE', '773510', '290184',
       c.id_chauffeur, 'Rue 25, Porte 118, Badalabougou',
       now() - interval '1 day', now() - interval '1 day' + interval '15 minutes',
       now() - interval '1 day' + interval '30 minutes', TRUE
FROM utilisateur u, boutique b, chauffeur c
JOIN utilisateur uc ON uc.id_utilisateur = c.utilisateur_id
WHERE u.telephone = '+22370000020' AND b.nom = 'Kaloga Mode' AND uc.telephone = '+22370000031';

INSERT INTO ligne_commande (commande_id, produit_id, quantite, prix_unitaire_mmc)
SELECT co.id_commande, p.id_produit, 1, 12000
FROM commande co, produit p WHERE co.code_retrait = '773510' AND p.nom = 'Boubou wax homme';

INSERT INTO signalement (auteur_id, commande_id, motif, description, statut, date_creation)
SELECT u.id_utilisateur, co.id_commande, 'COLIS_ENDOMMAGE', 'Le boubou est arrivé taché, emballage abîmé.', 'OUVERT', now() - interval '20 hours'
FROM utilisateur u, commande co WHERE u.telephone = '+22370000020' AND co.code_retrait = '773510';

-- =====================================================================
-- 5) COURSES MOTOTAXI — plusieurs statuts pour peupler l'historique
--    client, l'espace livreur et la console admin (MotoTaxis/Revenus).
-- =====================================================================

-- 5a) Course terminée (Mariam Cissé avec Salif Konaté)
INSERT INTO course (client_id, chauffeur_id, depart, destination, distance_km, prix_mmc, statut, note, commentaire, date_demande, date_acceptation, date_fin)
SELECT u.id_utilisateur, c.id_chauffeur, 'Badalabougou', 'Centre-ville', 4.2, 1500, 'TERMINEE', 5, 'Chauffeur ponctuel et courtois.',
       now() - interval '3 hours', now() - interval '3 hours' + interval '3 minutes', now() - interval '3 hours' + interval '18 minutes'
FROM utilisateur u, chauffeur c JOIN utilisateur uc ON uc.id_utilisateur = c.utilisateur_id
WHERE u.telephone = '+22370000020' AND uc.telephone = '+22370000030';

-- 5b) Course en cours (Boubacar Togola avec Adama Doumbia)
INSERT INTO course (client_id, chauffeur_id, depart, destination, distance_km, prix_mmc, statut, date_demande, date_acceptation)
SELECT u.id_utilisateur, c.id_chauffeur, 'Hippodrome', 'ACI 2000', 6.0, 2000, 'ACCEPTEE',
       now() - interval '6 minutes', now() - interval '3 minutes'
FROM utilisateur u, chauffeur c JOIN utilisateur uc ON uc.id_utilisateur = c.utilisateur_id
WHERE u.telephone = '+22370000021' AND uc.telephone = '+22370000031';

-- 5c) Course demandée, en attente d'un chauffeur (Kadiatou Sangaré)
INSERT INTO course (client_id, depart, destination, distance_km, prix_mmc, statut, date_demande)
SELECT u.id_utilisateur, 'Niarela', 'Missira', 2.8, 1000, 'DEMANDEE', now() - interval '90 seconds'
FROM utilisateur u WHERE u.telephone = '+22370000022';

-- 5d) Course annulée (Aïcha Diarra)
INSERT INTO course (client_id, depart, destination, distance_km, prix_mmc, statut, date_demande)
SELECT u.id_utilisateur, 'Sabalibougou', 'Kalaban Coura', 5.5, 1800, 'ANNULEE', now() - interval '1 day'
FROM utilisateur u WHERE u.telephone = '+22370000002';

-- =====================================================================
-- 6) PUBLICITÉS — une active, une en attente de validation admin
-- =====================================================================
INSERT INTO publicite (boutique_id, titre, duree_jours, prix_paye_mmc, statut, date_creation, date_activation, date_expiration)
SELECT b.id_boutique, 'Kaloga Mode : nouvelle collection wax', 7, 3500, 'ACTIVE',
       now() - interval '2 days', now() - interval '2 days', now() + interval '5 days'
FROM boutique b WHERE b.nom = 'Kaloga Mode';

INSERT INTO publicite (boutique_id, titre, duree_jours, prix_paye_mmc, statut, date_creation)
SELECT b.id_boutique, 'Électro Bamako : promo rentrée', 5, 2500, 'EN_ATTENTE', now() - interval '3 hours'
FROM boutique b WHERE b.nom = 'Électro Bamako';

-- =====================================================================
-- 7) ÉCRITURES COMPTABLES — pour que l'écran admin "Revenus" (commissions
--    plateforme) affiche des montants réels, cohérents avec les commandes
--    et courses ci-dessus.
-- =====================================================================

-- Vente + commission sur la commande livrée 4a (vendeur Moussa Traoré)
INSERT INTO ecriture_comptable (utilisateur_id, type_operation, montant_mmc, reference_type, reference_id, date_creation)
SELECT u.id_utilisateur, 'VENTE', 3600, 'COMMANDE', co.id_commande, co.date_livraison
FROM utilisateur u, commande co WHERE u.telephone = '+22370000003' AND co.code_retrait = '482913';

INSERT INTO ecriture_comptable (utilisateur_id, type_operation, montant_mmc, reference_type, reference_id, date_creation)
SELECT u.id_utilisateur, 'COMMISSION', 360, 'COMMANDE', co.id_commande, co.date_livraison
FROM utilisateur u, commande co WHERE u.telephone = '+000000000' AND co.code_retrait = '482913';

INSERT INTO ecriture_comptable (utilisateur_id, type_operation, montant_mmc, reference_type, reference_id, date_creation)
SELECT c.utilisateur_id, 'FRAIS_LIVRAISON', 500, 'COMMANDE', co.id_commande, co.date_livraison
FROM commande co JOIN chauffeur c ON c.id_chauffeur = co.chauffeur_id WHERE co.code_retrait = '482913';

-- Note : la commande 773510 (4e) est encore en litige, ses fonds restent
-- gelés — aucune écriture VENTE/COMMISSION tant que l'admin ne l'a pas
-- résolue (cf. AdminReportingService.resoudreLitige), donc rien à insérer ici.

-- Course terminée 5a : paiement client + revenu chauffeur + commission plateforme
INSERT INTO ecriture_comptable (utilisateur_id, type_operation, montant_mmc, reference_type, reference_id, date_creation)
SELECT cl.id_utilisateur, 'COURSE_PAIEMENT', 1500, 'COURSE', crs.id_course, crs.date_fin
FROM course crs JOIN utilisateur cl ON cl.id_utilisateur = crs.client_id
WHERE crs.statut = 'TERMINEE' AND crs.prix_mmc = 1500;

INSERT INTO ecriture_comptable (utilisateur_id, type_operation, montant_mmc, reference_type, reference_id, date_creation)
SELECT ch.utilisateur_id, 'COURSE_REVENU', 1350, 'COURSE', crs.id_course, crs.date_fin
FROM course crs JOIN chauffeur ch ON ch.id_chauffeur = crs.chauffeur_id
WHERE crs.statut = 'TERMINEE' AND crs.prix_mmc = 1500;

INSERT INTO ecriture_comptable (utilisateur_id, type_operation, montant_mmc, reference_type, reference_id, date_creation)
SELECT u.id_utilisateur, 'COMMISSION', 150, 'COURSE', crs.id_course, crs.date_fin
FROM utilisateur u, course crs WHERE u.telephone = '+000000000' AND crs.statut = 'TERMINEE' AND crs.prix_mmc = 1500;

-- Publicité active (Kaloga Mode) : paiement encaissé par la plateforme
INSERT INTO ecriture_comptable (utilisateur_id, type_operation, montant_mmc, reference_type, reference_id, date_creation)
SELECT u.id_utilisateur, 'PUBLICITE', 3500, 'PUBLICITE', pub.id_publicite, pub.date_creation
FROM utilisateur u, publicite pub WHERE u.telephone = '+22370000010' AND pub.titre LIKE 'Kaloga Mode%';

-- Recharges de portefeuille (pour l'historique "Portefeuille" côté mobile)
INSERT INTO ecriture_comptable (utilisateur_id, type_operation, montant_mmc, reference_type, reference_id, date_creation)
SELECT id_utilisateur, 'RECHARGE', 20000, NULL, NULL, now() - interval '6 days' FROM utilisateur WHERE telephone = '+22370000020';
INSERT INTO ecriture_comptable (utilisateur_id, type_operation, montant_mmc, reference_type, reference_id, date_creation)
SELECT id_utilisateur, 'RECHARGE', 50000, NULL, NULL, now() - interval '10 days' FROM utilisateur WHERE telephone = '+22370000002';
