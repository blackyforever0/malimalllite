-- Permet à l'admin de suspendre un compte utilisateur (client) directement,
-- en plus des suspensions déjà possibles sur une Boutique ou un Chauffeur
-- (voir V7__admin_flags.sql). Un compte suspendu ne peut plus se connecter
-- (voir UserPrincipal.isEnabled()).
ALTER TABLE utilisateur ADD COLUMN suspendu BOOLEAN NOT NULL DEFAULT FALSE;
