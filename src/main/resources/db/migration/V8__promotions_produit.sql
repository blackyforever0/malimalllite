-- Promotions vendeur sur un produit (voir mes_produits_screen côté mobile,
-- inspiré de la vraie app MaliMall Flutter : "Mettre en promotion").
ALTER TABLE produit ADD COLUMN en_promotion BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE produit ADD COLUMN promo_prix_mmc INTEGER;
ALTER TABLE produit ADD COLUMN promo_fin_at TIMESTAMP;
