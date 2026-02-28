# Builder stage: Maven + JDK 25
FROM maven:3.9-eclipse-temurin-25 AS builder

WORKDIR /app

# Copy pom.xml first to leverage Docker cache
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the jar (skip tests for faster builds)
RUN mvn clean package -DskipTests

# Runtime stage: JRE only for smaller image
FROM eclipse-temurin:25-jre AS runner

WORKDIR /app

# Copy the jar produced by the Spring Boot plugin
COPY --from=builder /app/target/billing-service-0.0.1-SNAPSHOT.jar ./app.jar

# Expose application port
EXPOSE 4001
EXPOSE 9001

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
