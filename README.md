# CFR Data Aggregator

CLI tool that fetches combined train arrivals and departures for every Romanian railway station
from [cfr-api-adapter](https://github.com/Stavre/cfr-api-adapter) and exports them to a CSV file.

## Prerequisites

- Java 25
- A running instance of cfr-api-adapter (default: `http://localhost:8080`)

## Build

```bash
./gradlew build
```

## Configuration

The base URL of cfr-api-adapter is configured in `src/main/resources/application.yaml`:

```yaml
cfr-api:
  base-url: http://localhost:8080
```

## Commands

### `export-arrivals-departures`

Fetches merged arrivals and departures via `/station/{station}` and writes a CSV file. Processes all stations by default; use `--stations` to restrict to a subset.

```bash
./gradlew bootRun --args="export-arrivals-departures [options]"
```

| Option | Description | Default |
|---|---|---|
| `-d, --date` | Date in `dd.MM.yyyy` format | today |
| `-o, --output` | Output CSV file path | `arrivals-departures-{yyyyMMdd_HHmmss}.csv` |
| `--log-file` | Log file path (WARN and above only) | `arrivals-departures-{yyyyMMdd_HHmmss}.log` |
| `-s, --stations` | Comma-delimited list of station names to process | all stations |

CSV columns:

```
currentTimestamp, station, trainId, trainOperator, fromStation, arrival, arrivalDelayMinutes, toStation, departure, departureDelayMinutes, platform
```

### `export-stations`

Fetches all station names from cfr-api-adapter via `/station/all` and writes them to a CSV file. Each station is saved with `isImportant=false` by default; the flag can be edited manually and is preserved by `reconcile-stations`.

```bash
./gradlew bootRun --args="export-stations [options]"
```

| Option | Description | Default |
|---|---|---|
| `-o, --output` | Output CSV file path | `stations-{yyyyMMdd_HHmmss}.csv` |

CSV columns: `name`, `isImportant`

### `reconcile-stations`

Compares two station CSV files and appends to the base file any station names present in the new file but not already in the base. The `isImportant` value from the new file is preserved for each appended row. Prints `Added: <name>` for each new station, or `No new stations found.` if nothing changed. Does not require cfr-api-adapter to be running.

```bash
./gradlew bootRun --args="reconcile-stations --base-file <path> --new-file <path>"
```

| Option | Description |
|---|---|
| `-b, --base-file` | Base CSV file to compare against and append into (required) |
| `-n, --new-file` | New CSV file with potentially new station names (required) |

## Running tests

```bash
./gradlew check
```
