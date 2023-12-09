FROM eclipse-temurin:17-jdk-jammy

# Select the working directory
WORKDIR /app

# Copy all the source files
COPY . ./

# Make sure gradlew is executable
RUN ["chmod", "+x", "gradlew"]

# Build
RUN ["./gradlew", "build"]

# Run
CMD ["./gradlew", "run"]
