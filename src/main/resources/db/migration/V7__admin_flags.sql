-- Drapeaux de suspension utilisés par la console d'administration (web,
-- Angular) : une boutique/un chauffeur suspendu reste visible (historique
-- intact) mais n'apparaît plus dans les listes actives côté mobile.
ALTER TABLE boutique ADD COLUMN suspendue BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE chauffeur ADD COLUMN suspendu BOOLEAN NOT NULL DEFAULT FALSE;
