-- Position GPS de la boutique (maquette "Ma boutique" — le vendeur enregistre
-- sa position pour que les livreurs puissent venir récupérer les colis chez
-- lui). Même format que chauffeur.latitude/longitude/position_maj (V6).
ALTER TABLE boutique ADD COLUMN latitude DOUBLE PRECISION;
ALTER TABLE boutique ADD COLUMN longitude DOUBLE PRECISION;
ALTER TABLE boutique ADD COLUMN position_maj TIMESTAMP;
