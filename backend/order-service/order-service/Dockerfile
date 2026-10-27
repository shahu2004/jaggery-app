FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
COPY src src
RUN ./mvnw --batch-mode -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 appuser && mkdir -p /app/uploads && chown -R appuser:appuser /app
COPY --from=build /build/target/*.jar /app/app.jar
USER appuser
EXPOSE 8083
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
