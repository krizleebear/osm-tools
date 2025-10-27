FROM maven:3-sapmachine AS build
WORKDIR /workspace

# copy pom first to leverage Docker cache for dependencies
COPY pom.xml .

# copy source and build
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
ENV JAVA_OPTS=""

# copy the built jar (matches any jar produced by the build)
COPY --from=build /workspace/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]