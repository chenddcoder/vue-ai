# Use Maven image for build stage
FROM maven:3.8.6-openjdk-8-slim AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Build the application
RUN mvn clean package -DskipTests

# Use OpenJDK 8 JRE for runtime
FROM openjdk:8-jre-slim
WORKDIR /app
COPY --from=build /app/target/vue-ai-server-0.0.1-SNAPSHOT.jar app.jar
COPY src/main/resources/application.yml application.yml

# Create directory for SQLite database
RUN mkdir -p data

# Expose port 8080
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
