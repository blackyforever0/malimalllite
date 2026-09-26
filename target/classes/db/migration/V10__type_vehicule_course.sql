-- Une course MotoTaxi demande désormais une catégorie de véhicule (Scooter,
-- Telimani ou TriCycle), qui détermine le tarif par palier (voir
-- QuartiersBamako côté service) — plus seulement Telimani. Les courses déjà
-- en base (toutes passager, donc Telimani) sont rattachées à TELIMANI.
ALTER TABLE course ADD COLUMN type_vehicule VARCHAR(20) NOT NULL DEFAULT 'TELIMANI';
ALTER TABLE course ALTER COLUMN type_vehicule DROP DEFAULT;
