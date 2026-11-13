# Backend image. Build from the repository root, because the migrations in
# database/migrations are packaged into the jar:
#
#   docker build -f backend/Dockerfile -t proho-backend .
#
# Configured only by environment variables (backend/README.md), so the same
# image runs on any host. See docs/operations/deployment.md.

FROM eclipse-temurin:17-jdk AS build
WORKDIR /src
# The wrapper and build scripts first, so the Gradle download and dependency
# resolution stay cached while only sources change.
COPY backend/gradlew backend/settings.gradle.kts backend/build.gradle.kts backend/gradle.properties backend/
COPY backend/gradle backend/gradle
RUN cd backend && ./gradlew --no-daemon dependencies > /dev/null
COPY backend/src backend/src
COPY database/migrations database/migrations
# Tests run in CI; the image build only packages.
RUN cd backend && ./gradlew --no-daemon bootJar \
    && find build/libs -name '*.jar' ! -name '*-plain.jar' -exec cp {} /src/app.jar \; \
    && java -Djarmode=tools -jar /src/app.jar extract --destination /src/extracted

FROM eclipse-temurin:17-jre
RUN groupadd --system proho && useradd --system --gid proho --no-create-home proho
WORKDIR /app
# Dependencies change less often than the application, so they get their
# own layer.
COPY --from=build /src/extracted/lib lib
COPY --from=build /src/extracted/*.jar app.jar
USER proho
EXPOSE 8080
# Leaves room for the JVM's own memory outside the heap.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
