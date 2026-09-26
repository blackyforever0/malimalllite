-- Publicité — un vendeur paie pour diffuser une bannière sur l'accueil
-- Market, activée après validation manuelle par un admin (voir
-- PubliciteService, calqué sur le module "ads" du vrai MaliMall).
CREATE TABLE publicite (
    id_publicite    BIGSERIAL PRIMARY KEY,
    boutique_id     BIGINT NOT NULL REFERENCES boutique(id_boutique),
    titre           VARCHAR(100) NOT NULL,
    image_url       VARCHAR(500),
    duree_jours     INT NOT NULL,
    prix_paye_mmc   INT NOT NULL,
    statut          VARCHAR(20) NOT NULL,
    date_creation   TIMESTAMP NOT NULL DEFAULT now(),
    date_activation TIMESTAMP,
    date_expiration TIMESTAMP
);

CREATE INDEX idx_publicite_boutique ON publicite(boutique_id);
CREATE INDEX idx_publicite_statut ON publicite(statut);
