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
     ┌────────────┼──────────────────────────────────────────────┐
     │            │            │            │          │         │      │
     ▼            ▼            ▼            ▼          ▼         ▼      ▼
ExportArr-  ExportTrain- AggregateD- AggregateTr- ExportSt- ExportTr- Reconcile-
ivalsDep-   Itineraries  elays       ains         ations    ains      Stations
Command     Command      Command     Command      Command   Command   Command
     │            │            │            │          │         │
     │            │            └────────────┘          └─────────┘
     │            │                  │ (reads CSV,             │ (API call /
     ▼            ▼                  │  no API)                │  no log)
LogFile-    LogFile-       DelayAggr- TrainAggr-   StationList-  TrainList-
Configurer  Configurer     egationSvc egationSvc   Service       Service
     │            │                               │             │
     ▼            ▼                               └──────┬───────┘
StationExport-  TrainExport-                             │
Service         Service                                  ▼
     │            │                              CfrApiClient
     └────────────┘
           │
           ▼
    CfrApiClient  →  cfr-api-adapter
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
| `ExportArrivalsDeparturesCommand` | Declares required `--stations`, `--date`, `--output`, `--log-file` options; resolves defaults; calls `StationExportService`; manages `LogFileConfigurer` in a `finally` block |
| `ExportTrainItinerariesCommand` | Declares required `--trains`, `--date`, `--output`, `--log-file` options; resolves defaults; calls `TrainExportService`; manages `LogFileConfigurer` in a `finally` block |
| `AggregateDelaysCommand` | Declares required `--input-file`, `--base-path`, `--stations` options; calls `DelayAggregationService`; reads CSV with no API access |
| `AggregateTrainsCommand` | Declares required `--input-file`, `--base-path`, `--trains` options; calls `TrainAggregationService`; reads CSV with no API access |
| `ExportStationsCommand` | Declares `--output` option; resolves default timestamped filename; calls `StationListService.exportStations` |
| `ExportTrainsCommand` | Declares `--output` option; resolves default timestamped filename; calls `TrainListService.exportTrains` |
| `ReconcileStationsCommand` | Declares required `--base-file` and `--new-file` options; validates both files exist before delegating; prints each added station name |
| `LogFileConfigurer` | Dynamically attaches a Logback `FileAppender` with a `ThresholdFilter` (WARN+) to the root logger before the export and detaches it after; creates parent directories for the log file if missing |

`--stations` in `ExportArrivalsDeparturesCommand` and `--trains` in `ExportTrainItinerariesCommand` are not optional: the command errors if they are absent. Pass `all` to process the full set fetched from the API.

Commands contain no HTTP or CSV logic — that is pushed down to the export layer.

### Client Layer (`client/`)

| Class | Responsibilities |
|---|---|
| `CfrApiClient` | OpenFeign `@FeignClient` interface; declares `GET /station/all`, `GET /station/{stationName}`, `GET /train/all`, and `GET /train/{trainNumber}` calls; base URL injected from `${cfr-api.base-url}` |

The client layer is transport-only. It does not transform or validate data.

### Export Layer (`export/`)

| Class | Responsibilities |
|---|---|
| `StationExportService` | Resolves the station list (fetches all or uses the provided filter); iterates stations with a progress bar; for each station calls `CfrApiClient`, stamps a `currentTimestamp`, transforms to `ArrivalsDeparturesCsvRecord`, and streams the row; catches `FeignException` per station |
| `TrainExportService` | Resolves the train list (fetches all or uses the provided filter); iterates trains with a progress bar; for each train calls `CfrApiClient`, stamps a `currentTimestamp`, maps each branch/stop to a `TrainItineraryCsvRecord`, and streams rows; catches `FeignException` per train; parses branch key string from API response |
| `DelayAggregationService` | Reads an arrivals-departures CSV, sums arrival and departure delays per station, writes one row per station to `{basePath}/arrivals/{letter}/{station}/delay.csv` and `{basePath}/departures/…`; appends to existing files without a second header |
| `TrainAggregationService` | Reads a train-itineraries CSV, sums arrival and departure delays per train per station, writes one row per train-station pair to `{basePath}/arrivals/{letter}/{trainId}/delay.csv` and `{basePath}/departures/…`; appends to existing files |
| `StationListService` | `exportStations`: fetches all stations via `CfrApiClient` and writes them to a CSV with `isImportant=false`; `reconcileStations`: reads both CSV files, appends names absent in baseFile, returns added names |
| `TrainListService` | `exportTrains`: fetches all train numbers via `CfrApiClient` and writes them to a CSV |
| `ArrivalsDeparturesCsvRecord` | Flat record for one train's combined arrival/departure at one station |
| `TrainItineraryCsvRecord` | Flat record for one stop on one branch of a train's itinerary |
| `ArrivalDelayRecord` | Aggregated arrival delay total for one station per run |
| `DepartureDelayRecord` | Aggregated departure delay total for one station per run |
| `TrainArrivalDelayRecord` | Aggregated arrival delay total for one train at one station per run |
| `TrainDepartureDelayRecord` | Aggregated departure delay total for one train at one station per run |
| `StationCsvRecord` | Two-column record (`name`, `isImportant`) |
| `TrainCsvRecord` | Single-column record (`number`) |

### Configuration (`config/`)

| Class | Responsibilities |
|---|---|
| `FeignConfig` | `@Configuration` class annotated with `@EnableFeignClients`; enables scanning of the `com.stavre.cfr_data_aggregator` package for Feign client interfaces |

## DTO Hierarchy

```
cfr-api-adapter JSON response
        │
        ▼
┌──────────────────────────────────┐
│  client/dto/                     │  Deserialized from JSON by Feign
│  ├── StationResponse             │  { name }
│  ├── StationTrainResponse        │  { fromStation, arrival, arrivalDelay,
│  │                               │    toStation, departure, departureDelay,
│  │                               │    train, platform, direction, errors,
│  │                               │    currentTimestamp (added by service) }
│  ├── TrainMetadataResponse       │  { id, number, category, operator }
│  ├── TrainItineraryResponse      │  { metadata: TrainMetadataResponse,
│  │                               │    stops: Map<String, List<TrainStopResponse>> }
│  └── TrainStopResponse           │  { arrival, arrivalDelay, departure, departureDelay,
│                                  │    station, journeyKm, stopDuration, platform,
│                                  │    trainStopMessages, errors }
└──────────────┬───────────────────┘
               │ transformed in service layer
               ▼
┌────────────────────────────────────────┐
│  export/                               │  Serialized to CSV by Jackson CsvMapper
│  ├── ArrivalsDeparturesCsvRecord       │  flat: all fields as String/Long
│  ├── TrainItineraryCsvRecord           │  flat: branch + stop fields as String/Long/Integer
│  ├── ArrivalDelayRecord               │  station delay aggregate: currentTimestamp, cfr_date,
│  │                                    │    station, totalDelayMinutes
│  ├── DepartureDelayRecord             │  station delay aggregate: same columns as above
│  ├── TrainArrivalDelayRecord          │  train delay aggregate: currentTimestamp, cfr_date,
│  │                                    │    trainId, station, totalDelayMinutes
│  ├── TrainDepartureDelayRecord        │  train delay aggregate: same columns as above
│  ├── StationCsvRecord                 │  two-column record: name, isImportant
│  └── TrainCsvRecord                   │  single-column record: number
└────────────────────────────────────────┘
```

`StationTrainResponse.currentTimestamp` is annotated `@JsonIgnore` — it is set by `StationExportService` at fetch time, not deserialized from the API response.

The `stops` map key in `TrainItineraryResponse` is a serialized `TrainBranchDto` string from cfr-api-adapter. `TrainExportService.parseBranchKey` parses out `name`, `originStation`, and `destinationStation` from it.

## Per-Item Resilience

`StationExportService` and `TrainExportService` both wrap each API call in a try/catch for `FeignException`. A failed item logs a warning (written to the log file via `LogFileConfigurer`) and the loop advances to the next item. The CSV file is not rolled back — rows already written remain. The log file records which items failed.

`StationListService.exportStations` and `TrainListService.exportTrains` each make a single bulk API call; if that call fails the entire command fails with an error message.

## Package Structure

```
com.stavre.cfr_data_aggregator/
├── CfrDataAggregatorApplication.java
├── cli/
│   ├── CfrCommand.java
│   ├── ExportArrivalsDeparturesCommand.java
│   ├── ExportTrainItinerariesCommand.java
│   ├── AggregateDelaysCommand.java
│   ├── AggregateTrainsCommand.java
│   ├── ExportStationsCommand.java
│   ├── ExportTrainsCommand.java
│   ├── ReconcileStationsCommand.java
│   └── LogFileConfigurer.java
├── client/
│   ├── CfrApiClient.java
│   └── dto/
│       ├── StationResponse.java
│       ├── StationTrainResponse.java
│       ├── TrainMetadataResponse.java
│       ├── TrainItineraryResponse.java
│       └── TrainStopResponse.java
├── config/
│   └── FeignConfig.java
└── export/
    ├── ArrivalsDeparturesCsvRecord.java
    ├── TrainItineraryCsvRecord.java
    ├── ArrivalDelayRecord.java
    ├── DepartureDelayRecord.java
    ├── TrainArrivalDelayRecord.java
    ├── TrainDepartureDelayRecord.java
    ├── StationCsvRecord.java
    ├── TrainCsvRecord.java
    ├── StationExportService.java
    ├── TrainExportService.java
    ├── DelayAggregationService.java
    ├── TrainAggregationService.java
    ├── StationListService.java
    └── TrainListService.java
```
