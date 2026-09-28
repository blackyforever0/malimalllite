-- Retrait par le client lui-même chez le vendeur (pas de livreur) : le client
-- choisit à la validation du panier de venir récupérer sa commande sur place
-- plutôt que de se faire livrer.
ALTER TABLE commande ADD COLUMN retrait_par_client BOOLEAN NOT NULL DEFAULT FALSE;
