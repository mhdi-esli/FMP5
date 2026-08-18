# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Copy pom.xml and download dependencies first for better caching
COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .
RUN mvn -B dependency:go-offline

# Copy source code and build
COPY src/ src/
RUN mvn -B package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user for security
RUN addgroup -S app && adduser -S -G app app

# Copy the built JAR
COPY --from=build /build/target/*.jar app.jar

# Change ownership to non-root user
RUN chown -R app:app /app

USER app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
