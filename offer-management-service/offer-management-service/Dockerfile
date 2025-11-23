# Étape 1 : Construction (Build avec Maven)
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# On compile le projet en sautant les tests pour aller plus vite
RUN mvn clean package -DskipTests

# Étape 2 : Exécution (Image légère)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# On récupère le .jar généré à l'étape précédente
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]