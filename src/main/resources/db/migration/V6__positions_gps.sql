-- Positions GPS réelles (carte Google Maps de l'application).

-- Dernière position envoyée par l'application du chauffeur quand il est en
-- ligne ; n'est montrée aux clients que si elle date de moins de 10 minutes.
ALTER TABLE chauffeur ADD COLUMN latitude DOUBLE PRECISION;
ALTER TABLE chauffeur ADD COLUMN longitude DOUBLE PRECISION;
ALTER TABLE chauffeur ADD COLUMN position_maj TIMESTAMP;

-- Point de livraison exact choisi par l'acheteur (facultatif, en plus du quartier).
ALTER TABLE commande ADD COLUMN livraison_latitude DOUBLE PRECISION;
ALTER TABLE commande ADD COLUMN livraison_longitude DOUBLE PRECISION;
