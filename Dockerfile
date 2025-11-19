FROM gradle:8.5-jdk17 AS build

WORKDIR /app

# Copy gradle files
COPY build.gradle.kts settings.gradle.kts gradle.properties ./

# Download dependencies (this layer will be cached)
RUN gradle dependencies --no-daemon || true

# Copy source code
COPY . .

# Build the application
RUN gradle build -x test --no-daemon

# Runtime stage
FROM openjdk:17-slim

WORKDIR /app

# Install useful tools
RUN apt-get update && apt-get install -y \
    curl \
    vim \
    && rm -rf /var/lib/apt/lists/*

# Copy the built application
COPY --from=build /app/build/libs/*.jar app.jar
COPY --from=build /app/build /app/build
COPY --from=build /root/.gradle /root/.gradle

# Copy source for development
COPY . .

ENTRYPOINT ["tail", "-f", "/dev/null"]
