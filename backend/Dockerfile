FROM gradle:jdk21 AS build
WORKDIR /app
COPY . .
# spotlessApply hooks into compileJava; skip it for image builds
RUN gradle bootJar -x spotlessApply -x test --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
