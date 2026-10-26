# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml ./
COPY src ./src
RUN --mount=type=cache,id=portfolio-platform-maven,target=/root/.m2 \
    mvn -B package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /workspace/target/*.jar app.jar

EXPOSE 8081
USER 10001:10001

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
