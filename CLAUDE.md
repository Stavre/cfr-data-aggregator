# CFR Data Aggregator

## Project Scope

CFR Data Aggregator is a CLI tool that fans out requests to the cfr-api-adapter service for every Romanian railway station and exports the combined arrivals and departures to a CSV file. It solves the problem of cfr-api-adapter having no bulk export capability, making it practical to capture a full snapshot of railway data for analysis.

See [agent_docs/scope.md](agent_docs/scope.md) for full details.

## Technologies

Built with Java 25 and Spring Boot 4.0.6 (`web-application-type: none` — no web server). Uses PicoCLI 4.7.6 for the CLI interface, Spring Cloud OpenFeign for HTTP calls to cfr-api-adapter, Jackson CSV for output serialization, ProgressBar for terminal feedback, and Lombok for boilerplate reduction. Build system is Gradle with a version catalog. Code quality enforced with Checkstyle, PMD, and JaCoCo.

See [agent_docs/technologies.md](agent_docs/technologies.md) for full details.

## Architecture

The application uses a layered architecture: the Spring Boot entry point wires Picocli + Spring DI → root `CfrCommand` routes to `ExportArrivalsDeparturesCommand` → which calls `StationExportService` → which fans out to `CfrApiClient` (OpenFeign) and streams results into a CSV file via Jackson `CsvMapper`. `LogFileConfigurer` dynamically attaches a WARN-level Logback file appender for the duration of the export.

See [agent_docs/architecture.md](agent_docs/architecture.md) for full details.
