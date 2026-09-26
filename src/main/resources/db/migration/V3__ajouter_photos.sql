-- Ajoute une URL de photo (hébergée sur Supabase Storage) sur les produits,
-- les boutiques et les utilisateurs (photo de profil). Nullable partout : une
-- ligne sans photo reste valide, le mobile affiche alors un visuel de
-- remplacement (voir ReseauImage côté Flutter) plutôt qu'une image cassée.
ALTER TABLE produit ADD COLUMN image_url VARCHAR(500);
ALTER TABLE boutique ADD COLUMN image_url VARCHAR(500);
ALTER TABLE utilisateur ADD COLUMN image_url VARCHAR(500);
