FROM maven:3-sapmachine AS build
WORKDIR /workspace

# copy pom first to leverage Docker cache for dependencies
COPY pom.xml .

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN mvn dependency:go-offline -B

# copy source and build
COPY src ./src
RUN mvn -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
ENV JAVA_OPTS=""

# copy the built jar (matches any jar produced by the build)
COPY --from=build /workspace/target/*jar-with-dependencies.jar app.jar

# Create a non-root user for security
RUN groupadd -r osmtools && useradd -r -g osmtools osmtools
RUN chown -R osmtools:osmtools /app
USER osmtools

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]