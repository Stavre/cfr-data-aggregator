# Project Scope

## Purpose

CFR Data Aggregator is a CLI tool that fetches combined train arrivals and departures for Romanian railway stations from the cfr-api-adapter service and exports them to a CSV file. It also supports exporting train itineraries, exporting station and train lists, reconciling station lists, and aggregating delay totals per station or per train. It solves the problem of cfr-api-adapter exposing per-station and per-train data with no bulk export facility, bridging that gap for data analysis and reporting use cases.

## Problem Solved

The cfr-api-adapter service provides structured JSON endpoints for Romanian railway data, but it only serves one station or one train at a time and has no export capability. Any workflow that needs data across all stations or all trains — historical snapshots, trend analysis, delay reports — would require scripting hundreds of API calls and handling the serialization manually. CFR Data Aggregator automates this: it fans out requests, merges the results into flat records, and writes timestamped CSV files ready for analysis.

## Domain

The aggregator operates in the Romanian railway domain, consuming data already structured by cfr-api-adapter:

- **Stations**: identified by name; the full list is fetched from `/station/all` when no filter is provided.
- **Trains at a station**: each station returns a list of trains with their arrival and departure information for a given date.
- **Arrivals and departures**: each record contains origin station, destination station, arrival and departure times, delays in minutes, platform, and train metadata.
- **Train itineraries**: each train returns a full stop-by-stop itinerary with arrival/departure times, delays, journey km, stop duration, and branch metadata.
- **Delays**: represented as `Duration` values from cfr-api-adapter; converted to minutes in the CSV output.
- **Dates**: data is date-specific; the export targets a single date per run.

## Key Capabilities

| Capability | Description |
|---|---|
| Full station export | Fetches arrivals/departures for every known station in a single run (use `--stations all`) |
| Station filtering | Accepts a comma-delimited station list to export a subset |
| Train itinerary export | Fetches the full stop-by-stop itinerary for every known train or a subset |
| Date targeting | Exports data for a specific date; defaults to today |
| Per-item resilience | A failed API call for one station or train is logged as a warning and skipped |
| Default timestamped output | Auto-generates output CSV and log filenames with a `yyyyMMdd_HHmmss` timestamp when not specified |
| File-based logging | Captures WARN-level and above messages to a separate log file per run; no console output |
| Progress reporting | Displays a progress bar during export so the user can track long-running fetches |
| Station list export | Fetches the full list of known stations from cfr-api-adapter and writes them to a CSV |
| Train list export | Fetches the full list of known train numbers from cfr-api-adapter and writes them to a CSV |
| Station list reconciliation | Compares two station CSV files and appends to the base file any stations present in the new file but not already in the base |
| Delay aggregation (stations) | Reads an arrivals-departures CSV and writes per-station cumulative arrival and departure delay totals |
| Delay aggregation (trains) | Reads a train-itineraries CSV and writes per-train per-station cumulative arrival and departure delay totals |

## CLI Interface

### `export-arrivals-departures`

```
cfr export-arrivals-departures --stations <list|all> [OPTIONS]
```

| Option | Short | Type | Default | Description |
|---|---|---|---|---|
| `--stations` | `-s` | List\<String\> | — (required) | Use `all` to process every station, or a comma-delimited list |
| `--date` | `-d` | String | Today (`dd.MM.yyyy`) | Date to export data for |
| `--output` | `-o` | File | `arrivals-departures-{timestamp}.csv` | Output CSV file path |
| `--log-file` | — | File | `arrivals-departures-{timestamp}.log` | Log file path (WARN+ only) |

`--stations` is required. Pass `all` to process every station fetched from `/station/all`.

### `export-train-itineraries`

```
cfr export-train-itineraries --trains <list|all> [OPTIONS]
```

| Option | Short | Type | Default | Description |
|---|---|---|---|---|
| `--trains` | `-t` | List\<String\> | — (required) | Use `all` to process every train, or a comma-delimited list of train numbers |
| `--date` | `-d` | String | Today (`dd.MM.yyyy`) | Date to export data for |
| `--output` | `-o` | File | `train-itineraries-{timestamp}.csv` | Output CSV file path |
| `--log-file` | — | File | `train-itineraries-{timestamp}.log` | Log file path (WARN+ only) |

`--trains` is required. Pass `all` to process every train fetched from `/train/all`.

### `aggregate-delays`

```
cfr aggregate-delays --input-file <file> --base-path <dir> --stations <list|all>
```

| Option | Short | Type | Required | Description |
|---|---|---|---|---|
| `--input-file` | `-i` | File | yes | Source arrivals-departures CSV file |
| `--base-path` | `-p` | Path | yes | Base directory for output delay files |
| `--stations` | `-s` | List\<String\> | yes | Use `all` or a comma-delimited list of station names |

Reads the source CSV and writes one appended row per station to:
- `{basePath}/arrivals/{firstLetter}/{station}/delay.csv`
- `{basePath}/departures/{firstLetter}/{station}/delay.csv`

Does not require cfr-api-adapter to be running.

### `aggregate-trains`

```
cfr aggregate-trains --input-file <file> --base-path <dir> --trains <list|all>
```

| Option | Short | Type | Required | Description |
|---|---|---|---|---|
| `--input-file` | `-i` | File | yes | Source train-itineraries CSV file |
| `--base-path` | `-p` | Path | yes | Base directory for output delay files |
| `--trains` | `-t` | List\<String\> | yes | Use `all` or a comma-delimited list of train numbers |

Reads the source CSV and writes one appended row per train per station to:
- `{basePath}/arrivals/{firstLetter}/{trainId}/delay.csv`
- `{basePath}/departures/{firstLetter}/{trainId}/delay.csv`

Does not require cfr-api-adapter to be running.

### `export-stations`

```
cfr export-stations [OPTIONS]
```

| Option | Short | Type | Default | Description |
|---|---|---|---|---|
| `--output` | `-o` | File | `stations-{timestamp}.csv` | Output CSV file path |

Requires cfr-api-adapter to be running. All exported stations have `isImportant=false` by default.

### `export-trains`

```
cfr export-trains [OPTIONS]
```

| Option | Short | Type | Default | Description |
|---|---|---|---|---|
| `--output` | `-o` | File | `trains-{timestamp}.csv` | Output CSV file path |

Requires cfr-api-adapter to be running.

### `reconcile-stations`

```
cfr reconcile-stations --base-file <file> --new-file <file>
```

| Option | Short | Type | Required | Description |
|---|---|---|---|---|
| `--base-file` | `-b` | File | yes | Base stations CSV; new entries are appended here |
| `--new-file` | `-n` | File | yes | Source stations CSV to compare against the base |

Prints `Added: <name>` for each appended station, or `No new stations found.` when nothing changed. Does not require cfr-api-adapter to be running.

## CSV Output Formats

### Arrivals/Departures CSV

Each row represents one train's arrival/departure record at one station on the target date.

| Column | Type | Description |
|---|---|---|
| `currentTimestamp` | String | Timestamp when the station was queried (`dd.MM.yyyy HH:mm`) |
| `cfr_date` | String | The date for which data was requested |
| `station` | String | Station name |
| `trainId` | String | Train identifier |
| `trainOperator` | String | Rail operator name |
| `fromStation` | String | Train's origin station |
| `arrival` | String | Scheduled arrival time (`dd.MM.yyyy HH:mm`), null if terminus |
| `arrivalDelayMinutes` | Long | Arrival delay in minutes, null if not available |
| `toStation` | String | Train's destination station |
| `departure` | String | Scheduled departure time (`dd.MM.yyyy HH:mm`), null if terminus |
| `departureDelayMinutes` | Long | Departure delay in minutes, null if not available |
| `platform` | String | Platform number, null if not available |

### Train Itineraries CSV

Each row represents one stop on one branch of a train's itinerary.

| Column | Type | Description |
|---|---|---|
| `currentTimestamp` | String | Timestamp when the train was queried (`dd.MM.yyyy HH:mm`) |
| `cfr_date` | String | The date for which data was requested |
| `trainId` | String | Train identifier |
| `trainOperator` | String | Rail operator name |
| `branchName` | String | Name of the train branch |
| `branchOriginStation` | String | Origin station of the branch |
| `branchDestinationStation` | String | Destination station of the branch |
| `station` | String | Station name for this stop |
| `journeyKm` | Integer | Kilometres from the branch origin to this stop |
| `arrival` | String | Scheduled arrival time (`dd.MM.yyyy HH:mm`), null if first stop |
| `arrivalDelayMinutes` | Long | Arrival delay in minutes, null if not available |
| `departure` | String | Scheduled departure time (`dd.MM.yyyy HH:mm`), null if last stop |
| `departureDelayMinutes` | Long | Departure delay in minutes, null if not available |
| `stopDurationMinutes` | Long | Scheduled stop duration in minutes, null if not available |
| `platform` | String | Platform number, null if not available |

### Station Delay CSV (`arrivals/` and `departures/`)

Each row represents one station's cumulative delay total for one export run.

| Column | Type | Description |
|---|---|---|
| `currentTimestamp` | String | Timestamp of the source row for this station |
| `cfr_date` | String | The date for which data was aggregated |
| `station` | String | Station name |
| `totalDelayMinutes` | Long | Sum of delay minutes across all trains at this station |

### Train Delay CSV (`arrivals/` and `departures/`)

Each row represents one train's cumulative delay total at one station for one export run.

| Column | Type | Description |
|---|---|---|
| `currentTimestamp` | String | Timestamp of the source row for this train |
| `cfr_date` | String | The date for which data was aggregated |
| `trainId` | String | Train identifier |
| `station` | String | Station name |
| `totalDelayMinutes` | Long | Sum of delay minutes for this train at this station |

### Stations CSV

Each row represents one station.

| Column | Type | Description |
|---|---|---|
| `name` | String | Station name |
| `isImportant` | Boolean | User-managed importance flag; defaults to `false` on export |

### Trains CSV

Each row represents one train.

| Column | Type | Description |
|---|---|---|
| `number` | String | Train number |

## External Dependency

CFR Data Aggregator calls the cfr-api-adapter REST API. The adapter must be running and reachable before `export-arrivals-departures`, `export-train-itineraries`, `export-stations`, or `export-trains` is executed. The aggregation and reconciliation commands require no API access.

| Config key | Default | Description |
|---|---|---|
| `cfr-api.base-url` | `http://localhost:8080` | Base URL of the cfr-api-adapter instance |

Endpoints used:
- `GET /station/all` — fetches the full list of station names
- `GET /station/{stationName}?date={date}` — fetches arrivals and departures for one station on one date
- `GET /train/all` — fetches the full list of train numbers
- `GET /train/{trainNumber}?date={date}` — fetches the full itinerary for one train on one date

## Scope Boundaries

- **In scope**: reading data from cfr-api-adapter and writing it to CSV; managing station list CSV files; aggregating delay totals from exported CSVs.
- **Out of scope**: scraping CFR's website directly, booking tickets, modifying data, authentication.
- The tool is read-only with respect to cfr-api-adapter and stateless; it makes live requests on every run.
- `reconcile-stations` modifies the base file in place (append-only). Delay aggregation commands also append to existing delay files.
