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

## Running tests

```bash
./gradlew check
```
