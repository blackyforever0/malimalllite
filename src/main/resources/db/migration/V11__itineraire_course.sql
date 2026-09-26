-- Tracé routier réel (Directions API) d'une course, encodé en polyline
-- Google — null quand la clé Directions n'est pas configurée ou que l'appel
-- a échoué (repli sur la ligne droite entre les deux quartiers côté mobile).
ALTER TABLE course ADD COLUMN itineraire_polyline TEXT;
