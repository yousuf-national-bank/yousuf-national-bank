# --- Build stage: compile the Java sources ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY src ./src
RUN javac -d out $(find src -name "*.java")

# --- Run stage: small runtime image ---
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/out ./out
COPY webroot ./webroot

# Persist the data file outside the container image



EXPOSE 8080
CMD ["java", "-cp", "out", "bank.web.WebServer"]
