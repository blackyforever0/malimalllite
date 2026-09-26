# MaliMall — Backend (Spring Boot)

Backend du parcours **Market** de MaliMall : catalogue, panier multi-produits,
commande, livraison à deux codes (retrait + livraison), portefeuille MMC et
commission plateforme. Java 21, Spring Boot 3.3.4, PostgreSQL, Flyway, JWT.

Ce code a été écrit et relu attentivement (imports, cohérence des noms de
champs, requêtes Spring Data, transactions JPA), mais **n'a pas encore été
compilé ni exécuté dans cet environnement** : le bac à sable cloud dans
lequel il a été rédigé n'a pas accès à Maven Central ni à Spring Initializr
(politique réseau du bac à sable). Il doit donc être compilé et testé une
première fois sur votre machine.

## Démarrer en local

Prérequis : JDK 21, Maven (ou le wrapper une fois généré), PostgreSQL 14+.

```bash
# 1. Créer la base et un rôle applicatif dédié
psql -U postgres -c "CREATE DATABASE malimall;"
psql -U postgres -c "CREATE USER malimall_app WITH PASSWORD 'malimall_app';"
psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE malimall TO malimall_app;"

# 2. Compiler et lancer (profil "local" actif par défaut, cf. application.yml)
mvn spring-boot:run
```

Flyway applique automatiquement `V1__init.sql` (schéma) puis
`V2__seed_demo_data.sql` (comptes de démo) au démarrage.

Documentation vivante : http://localhost:8080/swagger-ui.html — cliquer sur
"Authorize" avec le token JWT renvoyé par `POST /api/auth/connexion`.

## Configurer Supabase depuis zéro (base de données + photos)

Cette section part du principe que **rien n'a encore été créé côté
Supabase** — compte, projet, base, bucket. Deux choses distinctes à
brancher : la base de données Postgres (remplace votre Postgres local) et
le stockage de fichiers pour les photos (section suivante). Vous pouvez
faire l'une sans l'autre : sans la base Supabase, le backend continue de
tourner très bien sur le Postgres local (profil `local`, déjà utilisé
jusqu'ici) ; sans le bucket Storage, tout fonctionne sauf l'upload de
photos (503 explicite, jamais un crash).

### 1. Créer le compte et le projet

1. Aller sur [supabase.com](https://supabase.com) → **Start your project**
   → se connecter (GitHub, Google, ou email).
2. **New project** :
   - **Name** : `malimall` (ou ce que vous voulez, ça n'a pas d'impact sur le code).
   - **Database Password** : Supabase la génère ou vous la choisissez —
     **notez-la immédiatement dans un endroit sûr** (gestionnaire de mots
     de passe, note locale), elle ne sera plus jamais réaffichée en clair
     ensuite. C'est la valeur de `SUPABASE_DB_PASSWORD` plus bas.
   - **Region** : la plus proche de vous (ex. `Europe (Paris)` ou
     `Europe (Frankfurt)` si vous êtes au Mali/en Afrique de l'Ouest,
     latence moindre qu'un datacenter américain).
3. Cliquer **Create new project** — la création prend 1 à 2 minutes
   (Supabase provisionne une vraie base Postgres dédiée).

### 2. Récupérer la chaîne de connexion Postgres

Une fois le projet prêt : menu de gauche → **Project Settings** (icône
engrenage, en bas) → **Database**.

Dans la section **Connection string**, choisir l'onglet **URI** — Supabase
affiche quelque chose comme :

```
postgresql://postgres.xxxxxxxxxxxx:[YOUR-PASSWORD]@aws-0-eu-west-3.pooler.supabase.com:5432/postgres
```

Trois informations à en extraire pour les variables d'environnement :

| Variable d'environnement | D'où elle vient |
|---|---|
| `SUPABASE_JDBC_URL` | Même chaîne, préfixée `jdbc:` et **sans** les identifiants : `jdbc:postgresql://aws-0-eu-west-3.pooler.supabase.com:5432/postgres` |
| `SUPABASE_DB_USER` | La partie avant `:` dans `postgres.xxxxxxxxxxxx:...@` (ex. `postgres.xxxxxxxxxxxx`) |
| `SUPABASE_DB_PASSWORD` | Le mot de passe noté à l'étape 1 (remplace `[YOUR-PASSWORD]`) |

Si Supabase affiche plusieurs modes de connexion ("Session pooler" /
"Transaction pooler" / "Direct connection") : prenez **Session pooler**
(port `5432`, celui de l'exemple ci-dessus) — c'est celui qui se comporte
comme un Postgres classique et convient à Hibernate/Flyway, contrairement
au "Transaction pooler" (port `6543`) qui casse certaines fonctionnalités
JDBC.

### 3. Lancer le backend contre Supabase

```bash
export SUPABASE_JDBC_URL="jdbc:postgresql://aws-0-eu-west-3.pooler.supabase.com:5432/postgres"
export SUPABASE_DB_USER="postgres.xxxxxxxxxxxx"
export SUPABASE_DB_PASSWORD="le-mot-de-passe-noté-à-l'étape-1"

mvn spring-boot:run -Dspring-boot.run.profiles=supabase
```

Au démarrage, Flyway applique automatiquement `V1__init.sql` → `V4__ajouter_publicite.sql`
directement sur la base Supabase (les mêmes migrations que sur Postgres
local, rien à faire de plus). Pour vérifier : dashboard Supabase → **Table
Editor** — les tables `utilisateur`, `boutique`, `produit`, `commande`,
`publicite`, etc. doivent apparaître, ainsi que les 5 comptes de démo (voir
plus bas) si `V2__seed_demo_data.sql` s'est bien exécutée.

**Ne jamais** committer ces trois valeurs dans le dépôt (elles restent des
variables d'environnement locales, ou un fichier `.env` non versionné) —
`application-supabase.yml` est justement écrit pour ne rien lire d'autre
que des variables d'environnement, précisément pour éviter ça.

## Photos (Supabase Storage)

Les photos (produits, boutiques, profil) sont hébergées sur **Supabase
Storage**, le service de stockage de fichiers de Supabase. L'app mobile n'appelle jamais Supabase directement :
elle envoie la photo au backend (`POST /api/produits/{id}/photo`, etc.), et
c'est le backend (`SupabaseStorageService`) qui la retransmet à Supabase
avec la clé secrète `service_role` — cette clé n'est donc jamais présente
dans l'app mobile, seulement côté serveur.

### 1. Créer le bucket (une seule fois, dans le dashboard Supabase)

1. Ouvrir le projet Supabase → menu **Storage** → **New bucket**.
2. Nom du bucket : **`malimall-media`** (doit correspondre exactement à la
   valeur de `malimall.supabase.bucket` dans `application.yml`, par défaut
   `malimall-media` — modifiable via la variable d'environnement
   `SUPABASE_STORAGE_BUCKET` si vous préférez un autre nom).
3. **Activer "Public bucket"** en le créant. C'est le seul réglage
   nécessaire :
   - Un bucket public rend les URLs `storage/v1/object/public/...`
     directement lisibles par n'importe qui (sans jeton) — c'est ce dont
     l'app mobile a besoin pour simplement afficher une image avec
     `Image.network(...)`.
   - Aucune policy RLS supplémentaire n'est nécessaire : tous les envois
     (écritures) passent exclusivement par le backend avec la clé
     `service_role`, qui contourne RLS de toute façon. Le bucket n'a donc
     besoin d'aucune policy d'écriture ouverte au public.

### 2. Récupérer les identifiants et les fournir au backend

Dans le dashboard Supabase → **Project Settings** → **API** :

- **Project URL** (ex. `https://xxxxxxxxxxxx.supabase.co`) → variable
  d'environnement `SUPABASE_URL`.
- **Project API keys → `service_role`** (⚠️ pas la clé `anon` — celle-ci a
  tous les droits et ne doit **jamais** être mise dans l'app mobile ni
  commitée dans le dépôt) → variable d'environnement
  `SUPABASE_SERVICE_KEY`.

Avant de lancer le backend :

```bash
export SUPABASE_URL="https://xxxxxxxxxxxx.supabase.co"
export SUPABASE_SERVICE_KEY="eyJhbGciOi..."   # clé service_role, jamais anon
mvn spring-boot:run
```

Tant que ces deux variables ne sont pas définies, les endpoints
`POST .../photo` répondent `503 Service Unavailable`
(`StorageNonConfigureException`) — le reste de l'app (catalogue, panier,
commandes, portefeuille) fonctionne normalement sans elles, seul l'upload
de photo est indisponible.

### 3. Vérifier

Une fois les variables définies et le backend relancé :

```bash
curl -X POST http://localhost:8080/api/produits/1/photo \
  -H "Authorization: Bearer <JWT_VENDEUR>" \
  -F "fichier=@photo.jpg"
```

La réponse doit contenir un `imageUrl` du type
`https://xxxxxxxxxxxx.supabase.co/storage/v1/object/public/malimall-media/produits/1/....jpg`,
directement ouvrable dans un navigateur (bucket public).

## Comptes de démonstration (V2__seed_demo_data.sql)

Mot de passe identique pour tous : **`password123`**

| Téléphone      | Rôle                          |
|----------------|--------------------------------|
| `+000000000`   | Portefeuille plateforme (système, admin technique) |
| `+22370000001` | Admin (accès `/api/admin/**`) |
| `+22370000002` | Acheteur (50 000 MMC crédités) |
| `+22370000003` | Vendeur — "Boutique Traoré" (2 produits) |
| `+22370000004` | Chauffeur — véhicule Telimani, disponible |

## Parcours de bout en bout à tester

1. `POST /api/auth/connexion` avec `+22370000002` → JWT acheteur.
2. `GET /api/produits` → repérer l'id d'un produit de "Boutique Traoré".
3. `POST /api/panier/lignes` `{ "produitId": ..., "quantite": 2 }`.
4. `POST /api/panier/valider` → crée une Commande, fige `codeRetrait` /
   `codeLivraison`, gèle les fonds côté acheteur.
5. Se connecter avec `+22370000004` (chauffeur) → JWT chauffeur.
6. `POST /api/commandes/{id}/assigner-chauffeur`.
7. `POST /api/commandes/{id}/scanner-retrait` avec le `codeRetrait` — visible
   uniquement en se connectant avec `+22370000003` (le vendeur) et en
   rappelant `GET /api/commandes/{id}`.
8. Se reconnecter en acheteur, rappeler `GET /api/commandes/{id}` : le
   `codeLivraison` est maintenant visible (masqué avant le retrait).
9. `POST /api/commandes/{id}/confirmer-livraison` avec ce code → règle les
   fonds : vendeur et chauffeur crédités (92 % chacun de leur part), la
   plateforme (`+000000000`) créditée de la commission (8 % + 8 %).
10. Vérifier via `GET /api/portefeuille` (sur chaque compte) et
    `GET /api/portefeuille/historique`.

## Tests automatisés

```bash
mvn test
```

- `CommissionCalculatorTest` — tests unitaires purs (aucune dépendance),
  fixent la règle de commission (8 % séparément sur le produit et sur la
  livraison) avec les mêmes montants que le parcours vérifié via Swagger.
- `PortefeuilleServiceTest` — tests unitaires avec Mockito (geler/débloquer/
  créditer/débiter), sans base de données.
- `ParcoursMarketIntegrationTest` — rejoue tout le parcours Market déjà
  vérifié manuellement (panier → validation → assignation d'un chauffeur →
  scan retrait → scan livraison → règlement des fonds), assertions sur les
  soldes finaux et les 3 écritures comptables.
- `AuthFlowControllerTest` — vérifie le chemin HTTP complet via MockMvc
  (inscription, connexion, JWT, et le 403 sans jeton documenté plus haut).

Ces deux derniers tournent sur une base **H2 en mémoire** (profil `test`,
voir `src/test/resources/application-test.yml`) avec le schéma généré
directement depuis les entités JPA — **ni PostgreSQL ni Docker ne sont
requis** pour lancer `mvn test`. Les dépendances Testcontainers restent dans
le `pom.xml` si vous préférez un jour tester contre un vrai PostgreSQL.

Comme pour le reste du code, ces tests ont été écrits et relus avec soin
mais n'ont pas pu être compilés dans le bac à sable cloud (pas d'accès à
Maven Central) — voir la note ci-dessous.

## Ce qui n'est pas encore dans ce lot

- Module MotoTaxis (Course, Avis), Publicité — lots suivants.
- Annulation/litige après retrait — formule déjà codée et testée
  unitairement dans `CommissionCalculator.splitAnnulationApresRetrait`, mais
  aucun endpoint n'y est branché.
- Console Angular.
- Déploiement de la base de données sur Supabase Postgres réel : la marche
  à suivre est maintenant documentée en détail ("Configurer Supabase depuis
  zéro" ci-dessus), mais comme pour le reste du backend, je n'ai pas pu
  l'exécuter moi-même dans cet environnement (pas d'accès réseau sortant
  vers Supabase depuis ce bac à sable) — à tester une première fois chez
  vous en suivant les étapes.

## Si la compilation échoue

Dites-moi l'erreur exacte (`mvn compile` ou `mvn spring-boot:run`) et je
corrige : n'ayant pas pu compiler ce code moi-même dans cet environnement, il
est probable qu'il reste une ou deux erreurs de syntaxe ou d'import que je
n'ai pas vues à la relecture manuelle.
