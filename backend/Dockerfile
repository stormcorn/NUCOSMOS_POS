FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /app/backend

COPY backend/pom.xml ./
COPY backend/src ./src
COPY backend/.mvn ./.mvn
COPY backend/mvnw ./

RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:17-jre

RUN apt-get update \
    && apt-get install -y --no-install-recommends docker.io curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=build /app/backend/target/backend-0.1.0-SNAPSHOT.jar /app/app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
