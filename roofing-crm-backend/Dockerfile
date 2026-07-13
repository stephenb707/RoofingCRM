# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
ENV MAVEN_CONFIG=""

# Copy Maven descriptor + wrapper config first for caching
COPY pom.xml ./
COPY .mvn/ .mvn/
COPY mvnw mvnw.cmd ./

# Fix Windows CRLF line endings and ensure executable
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

# Cache dependencies
RUN ./mvnw -q -DskipTests dependency:go-offline

# Build app
COPY src/ src/
RUN ./mvnw -q -DskipTests package

# Runtime stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar /app/app.jar

# Default heap sizing: without an explicit setting the JVM only claims ~25% of
# container RAM (MaxRAMPercentage default), which is too little on small
# instances (e.g. 512MB Render service -> ~128MB heap) and caused startup
# OutOfMemoryError while Hibernate parsed repository @Query methods.
# 75% of a 512MB container is ~384MB heap, which starts comfortably.
# Deployments can still override JAVA_OPTS via environment.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
