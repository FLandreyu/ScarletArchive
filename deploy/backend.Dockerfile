FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY backend/pom.xml ./
COPY backend/src/ ./src/
RUN mvn -B -DskipTests package
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/target/scarlet-archive-1.0.0.jar app.jar
RUN mkdir -p /app/data/uploads && chown -R 10001:10001 /app
USER 10001:10001
EXPOSE 8098
ENTRYPOINT ["java","-jar","app.jar","--debug=false"]
