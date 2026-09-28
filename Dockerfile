# Build multi-étapes : l'étape "build" compile le jar avec Maven + JDK 21,
# l'image finale ne contient qu'un JRE 21 (plus léger, rien du code source ni
# de Maven ne reste dans l'image livrée) — c'est cette image finale que
# Render (ou tout hébergeur Docker) exécute réellement.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copié séparément du reste du code : tant que pom.xml ne change pas, Docker
# réutilise le cache des dépendances déjà téléchargées au lieu de tout
# retélécharger à chaque build (plus rapide sur Render).
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/backend-0.1.0.jar app.jar

# Render fournit sa propre variable PORT au démarrage (voir application.yml,
# server.port: ${PORT:8080}) ; EXPOSE est purement documentaire ici.
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
