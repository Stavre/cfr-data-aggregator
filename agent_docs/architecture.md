# Architecture

## Layered Architecture Overview

```
CLI invocation
      │
      ▼
┌──────────────────────────────────┐
│  CfrDataAggregatorApplication    │  Spring Boot entry point; wires Picocli + Spring DI
└──────────────────┬───────────────┘
                   │
                   ▼
           ┌──────────────┐
           │  CfrCommand  │  Root Picocli command; delegates to subcommands
           └──────┬───────┘
                  │
        ┌─────────┼──────────────────────┐
        ▼         ▼                      ▼
┌──────────────┐ ┌──────────────────┐ ┌────────────────────────────┐
│  ExportArr-  │ │  ExportStations  │ │  ReconcileStations         │
│  ivalsDep-   │ │  Command         │ │  Command                   │
│  Command     │ └────────┬─────────┘ └──────────────┬─────────────┘
└──────┬───────┘          │                           │
       │                  │                           │ (file I/O only,
       │          ┌───────┴──────────────────────────┐│  no API call)
       ▼          ▼                                  ││
┌───────────────┐ ┌──────────────────┐               ││
│ LogFile-      │ │ StationExport    │ ┌─────────────┐│
│ Configurer    │ │ Service          │ │ StationList ◄┘
└───────────────┘ └────────┬─────────┘ │ Service     │
                           │           └──────┬──────┘
                           │                  │
                           └─────┬────────────┘
                                 ▼
                        ┌──────────────────┐
                        │  CfrApiClient    │  OpenFeign HTTP client → cfr-api-adapter
                        └──────────────────┘
                                 │
                                 ▼
                           CSV file output
```

## Layer Descriptions

### Entry Point (`CfrDataAggregatorApplication`)

Implements `CommandLineRunner` and `ExitCodeGenerator`. On startup it:
1. Creates a Picocli `CommandLine` using Spring's `IFactory` so that all command beans receive dependency injection.
2. Passes the raw CLI args to Picocli for parsing and dispatch.
3. Propagates Picocli's exit code to the JVM so the process exits with the correct status.

No HTTP or business logic lives here — this class is purely bootstrap wiring.

### CLI Layer (`cli/`)

| Class | Responsibilities |
|---|---|
| `CfrCommand` | Root `@Command`; registers all subcommands; prints help when invoked with no arguments |
| `ExportArrivalsDeparturesCommand` | Declares `--date`, `--output`, `--log-file`, `--stations` options; resolves defaults (today's date, timestamped filenames); calls `StationExportService`; ensures `LogFileConfigurer` is always detached in a `finally` block |
| `ExportStationsCommand` | Declares `--output` option; resolves default timestamped filename (`stations-{timestamp}.csv`); calls `StationListService.exportStations` |
| `ReconcileStationsCommand` | Declares required `--base-file` and `--new-file` options; validates both files exist before delegating; prints each added station name or `No new stations found.` |
| `LogFileConfigurer` | Dynamically attaches a Logback `FileAppender` with a `ThresholdFilter` (WARN+) to the root logger before the export and detaches it after; creates parent directories for the log file if missing |

Commands contain no HTTP or CSV logic — that is pushed down to the export layer.

### Client Layer (`client/`)

| Class | Responsibilities |
|---|---|
| `CfrApiClient` | OpenFeign `@FeignClient` interface; declares `GET /station/all` and `GET /station/{stationName}` calls; base URL injected from `${cfr-api.base-url}` |

The client layer is transport-only. It does not transform or validate data.

### Export Layer (`export/`)

| Class | Responsibilities |
|---|---|
| `StationExportService` | Resolves the station list (fetches all or uses the provided filter); opens a Jackson `CsvMapper` writer; iterates stations with a progress bar; for each station calls `CfrApiClient`, stamps a `currentTimestamp` on each response, transforms to `ArrivalsDeparturesCsvRecord`, and writes the row; catches `FeignException` per station so one failure does not abort the run |
| `ArrivalsDeparturesCsvRecord` | Immutable flat record (Lombok `@Builder`); declares all CSV columns and their order via `@JsonPropertyOrder` |
| `StationListService` | `exportStations`: fetches all stations via `CfrApiClient` and writes them to a CSV with `isImportant=false`; `reconcileStations`: reads both CSV files, appends names from newFile absent in baseFile (preserving `isImportant`), returns the list of added names |
| `StationCsvRecord` | Immutable two-column record (`name`, `isImportant`); `Boolean` type on `isImportant` ensures Jackson generates a `getIsImportant()` getter so the column name serializes correctly |

### Configuration (`config/`)

| Class | Responsibilities |
|---|---|
| `FeignConfig` | `@Configuration` class annotated with `@EnableFeignClients`; enables scanning of the `com.stavre.cfr_data_aggregator` package for Feign client interfaces |

## DTO Hierarchy

```
cfr-api-adapter JSON response
        │
        ▼
┌──────────────────────────────┐
│  client/dto/                 │  Deserialized from JSON by Feign
│  ├── StationResponse         │  { name }
│  ├── StationTrainResponse    │  { fromStation, arrival, arrivalDelay,
│  │                           │    toStation, departure, departureDelay,
│  │                           │    train, platform, direction, errors,
│  │                           │    currentTimestamp (added by service) }
│  └── TrainMetadataResponse   │  { id, number, category, operator }
└──────────────┬───────────────┘
               │ transformed in StationExportService
               ▼
┌──────────────────────────────────┐
│  export/                         │  Serialized to CSV by Jackson CsvMapper
│  ├── ArrivalsDeparturesCsvRecord │  flat record: all fields as String/Long
│  └── StationCsvRecord            │  two-column record: name (String), isImportant (Boolean)
└──────────────────────────────────┘
```

`StationTrainResponse.currentTimestamp` is annotated `@JsonIgnore` — it is set by `StationExportService` at fetch time, not deserialized from the API response.

## Per-Station Resilience

The export loop in `StationExportService` wraps each station's API call in a try/catch for `FeignException`. A failed station logs a warning (written to the log file via `LogFileConfigurer`) and the loop advances to the next station. The CSV file is not rolled back — rows already written remain. This means partial output is possible and intentional; the log file records which stations failed.

`StationListService` makes a single `GET /station/all` call for `exportStations`; if it fails the entire command fails with an error message. `reconcileStations` makes no API calls at all.

## Package Structure

```
com.stavre.cfr_data_aggregator/
├── CfrDataAggregatorApplication.java
├── cli/
│   ├── CfrCommand.java
│   ├── ExportArrivalsDeparturesCommand.java
│   ├── ExportStationsCommand.java
│   ├── ReconcileStationsCommand.java
│   └── LogFileConfigurer.java
├── client/
│   ├── CfrApiClient.java
│   └── dto/
│       ├── StationResponse.java
│       ├── StationTrainResponse.java
│       └── TrainMetadataResponse.java
├── config/
│   └── FeignConfig.java
└── export/
    ├── ArrivalsDeparturesCsvRecord.java
    ├── StationCsvRecord.java
    ├── StationExportService.java
    └── StationListService.java
```
