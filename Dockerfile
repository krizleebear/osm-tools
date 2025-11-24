FROM maven:3-sapmachine AS build
WORKDIR /workspace

# copy pom first to leverage Docker cache for dependencies
COPY pom.xml .

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -B

# copy source and build
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app
ENV JAVA_OPTS=""

# copy the built jar (matches any jar produced by the build)
COPY --from=build /workspace/target/*jar-with-dependencies.jar app.jar

# Create a non-root user for security
RUN groupadd -r osmtools && useradd -r -g osmtools osmtools
RUN chown -R osmtools:osmtools /app
USER osmtools

COPY --chmod=755 <<EOT /entrypoint.sh
#!/usr/bin/env bash
set -exu
java \$JAVA_OPTS -jar /app/app.jar \$@
EOT

COPY --chmod=755 <<EOT /simplify.sh
#!/usr/bin/env bash
set -exu
java \$JAVA_OPTS -cp /app/app.jar net.leberfinger.geo.GeoJSONSimplify \$@
EOT


CMD ["/entrypoint.sh"]