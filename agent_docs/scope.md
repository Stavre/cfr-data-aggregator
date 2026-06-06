# Project Scope

## Purpose

CFR Data Aggregator is a CLI tool that fetches combined train arrivals and departures for Romanian railway stations from the cfr-api-adapter service and exports them to a CSV file. It solves the problem of cfr-api-adapter exposing per-station data with no bulk export facility, bridging that gap for data analysis and reporting use cases.

## Problem Solved

The cfr-api-adapter service provides structured JSON endpoints for Romanian railway data, but it only serves one station at a time and has no export capability. Any workflow that needs data across all stations — historical snapshots, trend analysis, delay reports — would require scripting hundreds of API calls and handling the serialization manually. CFR Data Aggregator automates this: it fans out requests to all stations (or a filtered subset), merges the results into a flat record per train per station, and writes a timestamped CSV file ready for analysis.

## Domain

The aggregator operates in the Romanian railway domain, consuming data already structured by cfr-api-adapter:

- **Stations**: identified by name; the full list is fetched from `/station/all` when no filter is provided.
- **Trains at a station**: each station returns a list of trains with their arrival and departure information for a given date.
- **Arrivals and departures**: each record contains origin station, destination station, arrival and departure times, delays in minutes, platform, and train metadata.
- **Delays**: represented as `Duration` values from cfr-api-adapter; converted to minutes in the CSV output.
- **Dates**: data is date-specific; the export targets a single date per run.

## Key Capabilities

| Capability | Description |
|---|---|
| Full station export | Fetches arrivals/departures for every known station in a single run |
| Station filtering | Accepts a comma-delimited station list to export a subset instead of all stations |
| Date targeting | Exports data for a specific date; defaults to today |
| Per-station resilience | A failed API call for one station is logged as a warning and skipped — the rest of the export continues |
| Default timestamped output | Auto-generates output CSV and log filenames with a `yyyyMMdd_HHmmss` timestamp when not specified |
| File-based logging | Captures WARN-level and above messages to a separate log file per run; no console output |
| Progress reporting | Displays a progress bar during export so the user can track long-running fetches |

## CLI Interface

Entry point: the `export-arrivals-departures` subcommand of the `cfr` root command.

```
cfr export-arrivals-departures [OPTIONS]
```

| Option | Short | Type | Default | Description |
|---|---|---|---|---|
| `--date` | `-d` | String | Today (`dd.MM.yyyy`) | Date to export data for |
| `--output` | `-o` | File | `arrivals-departures-{timestamp}.csv` | Output CSV file path |
| `--log-file` | — | File | `arrivals-departures-{timestamp}.log` | Log file path (WARN+ only) |
| `--stations` | `-s` | List\<String\> | null (all stations) | Comma-delimited station names to include |

## CSV Output Format

Each row represents one train's arrival/departure record at one station on the target date.

**Columns (in order):**

| Column | Type | Description |
|---|---|---|
| `currentTimestamp` | String | Timestamp when the station was queried (`dd.MM.yyyy HH:mm`) |
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

## External Dependency

CFR Data Aggregator calls the cfr-api-adapter REST API. The adapter must be running and reachable before the export command is executed.

| Config key | Default | Description |
|---|---|---|
| `cfr-api.base-url` | `http://localhost:8080` | Base URL of the cfr-api-adapter instance |

Two endpoints are used:
- `GET /station/all` — fetches the full list of station names (called only when `--stations` is not provided)
- `GET /station/{stationName}?date={date}` — fetches arrivals and departures for one station on one date

## Scope Boundaries

- **In scope**: reading data from cfr-api-adapter and writing it to CSV.
- **Out of scope**: scraping CFR's website directly, booking tickets, modifying data, authentication.
- The tool is read-only and stateless; it makes live requests to cfr-api-adapter on every run.
