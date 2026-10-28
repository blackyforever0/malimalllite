-- Normalise l'ancienne catégorie "Habillement" et rattache chaque produit
-- existant à une sous-catégorie de sa catégorie principale de boutique.
UPDATE boutique
SET categorie = 'Mode'
WHERE categorie = 'Habillement';

UPDATE produit p
SET categorie = CASE b.categorie
    WHEN 'Mode' THEN 'Vêtements'
    WHEN 'Artisanat' THEN 'Artisanat divers'
    WHEN 'Bijoux' THEN 'Bijoux divers'
    WHEN 'Maison' THEN 'Autres articles maison'
    WHEN 'Beauté' THEN 'Soins du corps'
    WHEN 'Électronique' THEN 'Accessoires électroniques'
    WHEN 'Alimentation' THEN 'Épicerie'
    ELSE p.categorie
END
FROM boutique b
WHERE b.id_boutique = p.boutique_id
  AND (
      p.categorie IS NULL
      OR
      (b.categorie = 'Mode' AND p.categorie NOT IN ('Vêtements', 'Chaussures', 'Sacs et accessoires', 'Autres articles de mode'))
      OR (b.categorie = 'Artisanat' AND p.categorie NOT IN ('Poterie', 'Tissage', 'Vannerie', 'Artisanat divers'))
      OR (b.categorie = 'Bijoux' AND p.categorie NOT IN ('Colliers', 'Bracelets', 'Bagues', 'Boucles d’oreilles', 'Bijoux divers'))
      OR (b.categorie = 'Maison' AND p.categorie NOT IN ('Décoration', 'Mobilier', 'Cuisine', 'Linge de maison', 'Autres articles maison'))
      OR (b.categorie = 'Beauté' AND p.categorie NOT IN ('Soins du visage', 'Soins du corps', 'Maquillage', 'Parfums'))
      OR (b.categorie = 'Électronique' AND p.categorie NOT IN ('Téléphones et tablettes', 'Audio', 'Informatique', 'Accessoires électroniques', 'Électroménager'))
      OR (b.categorie = 'Alimentation' AND p.categorie NOT IN ('Céréales et légumineuses', 'Huiles et condiments', 'Boissons', 'Produits frais', 'Épicerie'))
  );
