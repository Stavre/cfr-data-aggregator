# Technologies

## Language & Runtime

- **Java 25** — minimum language version enforced via Gradle toolchain. Lombok is used for boilerplate reduction on DTOs and service classes.

## Core Framework

- **Spring Boot 4.0.6** — application framework providing auto-configuration and dependency injection. Configured as a non-web application (`web-application-type: none`) — no embedded Tomcat or HTTP server is started.

## CLI Framework

- **PicoCLI 4.7.6** (`picocli-spring-boot-starter`) — command-line interface framework with Spring Boot integration. PicoCLI's `IFactory` is used so that `@Command` classes are Spring beans and receive full dependency injection. Provides subcommand routing, option parsing, and built-in `--help` / `--version` support.

## HTTP Client

- **Spring Cloud OpenFeign** (via Spring Cloud BOM 2025.1.1) — declarative HTTP client. The `CfrApiClient` interface uses `@FeignClient` to define calls to cfr-api-adapter. Base URL is configured via `cfr-api.base-url` in `application.yaml` (default: `http://localhost:8080`). Feign client scanning is enabled by `@EnableFeignClients` in `FeignConfig`.

## Serialization

- **Jackson CSV dataformat** — used in `StationExportService` via `CsvMapper` to serialize `ArrivalsDeparturesCsvRecord` objects to CSV rows. Column order is declared with `@JsonPropertyOrder` on the record class.
- **Jackson datatype-jsr310** — registers serializers/deserializers for `java.time` types (`LocalDateTime`, `Duration`) so that cfr-api-adapter's JSON responses are correctly deserialized by Feign.

## Progress Reporting

- **ProgressBar 0.10.1** — displays a terminal progress bar during the export loop. One tick per station processed.

## Boilerplate Reduction

- **Lombok** — annotation processor used on DTOs and the service class to generate builders (`@Builder`), getters (`@Getter`), and data accessors (`@Data`).

## Logging

- **Logback** (via Spring Boot's logging starter) — logging implementation. Console logging is disabled by default (`logging.threshold.console: OFF`). `LogFileConfigurer` dynamically attaches a `FileAppender` with a `ThresholdFilter(WARN)` to the root logger at command start and detaches it at command end, producing a per-run log file.

## Build System

- **Gradle** with the **Spring Boot** and **Spring Dependency Management** plugins — resolves Spring Boot and Spring Cloud BOM versions. Build script is `build.gradle.kts`; project name is defined in `settings.gradle.kts`. Dependency versions are centralised in `gradle/libs.versions.toml` (Gradle version catalog).
- Build output: a self-contained bootable JAR placed in the project root.

## Testing

- **JUnit 5** — test runner for all unit and integration tests under `src/test/java`.
- **Mockito** — mock framework used to stub `CfrApiClient` and `LogFileConfigurer` in command and service tests.
- **AssertJ** — fluent assertion library used alongside JUnit 5.
- Tests use `@TempDir` for isolated file I/O and `ArgumentCaptor` to verify service call arguments.

## Code Quality

- **Checkstyle** — style enforcement. Configuration: `config/checkstyle/checkstyle.xml`. Applied to `main` and `test` source sets.
- **PMD** — static analysis. Rule set: `config/pmd/ruleset.xml`.
- **JaCoCo** — code coverage agent; generates XML and HTML reports on the `check` task.

## Configuration File

`src/main/resources/application.yaml`:

```yaml
spring:
  application:
    name: CFR-data-aggregator
  main:
    web-application-type: none

logging:
  threshold:
    console: "OFF"

cfr-api:
  base-url: http://localhost:8080
```

- `web-application-type: none` — disables the embedded Tomcat servlet container; the process starts and exits as a CLI tool.
- `console: "OFF"` — suppresses all console log output. Logging is only written to the file configured by `--log-file`.
- `cfr-api.base-url` — injected into `CfrApiClient` via `${cfr-api.base-url}`; can be overridden at runtime with `--spring.application.json` or environment variables.
