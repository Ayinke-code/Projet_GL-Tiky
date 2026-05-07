# Build de l'application
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

# Configurer Maven pour utiliser le proxy
ENV MAVEN_OPTS="-Dhttp.proxyHost=192.168.4.8 -Dhttp.proxyPort=3128 -Dhttps.proxyHost=192.168.4.8 -Dhttps.proxyPort=3128"

COPY pom.xml .
RUN mvn dependency:resolve
COPY . .
RUN mvn clean package -DskipTests

# Image finale
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]