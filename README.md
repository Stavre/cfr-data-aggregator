# CFR Data Aggregator

CLI tool that fetches train arrivals and departures for every Romanian railway station
from [cfr-api-adapter](https://github.com/Stavre/cfr-api-adapter) and exports them to CSV files.

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

### `export-arrivals`

Fetches arrivals for every station and writes a CSV file.

```bash
./gradlew bootRun --args="export-arrivals [options]"
```

| Option | Description | Default |
|---|---|---|
| `-d, --date` | Date in `dd.MM.yyyy` format | today |
| `-o, --output` | Output CSV file path | `arrivals-{yyyyMMdd_HHmmss}.csv` |
| `--log-file` | Log file path (WARN and above only) | `arrivals-{yyyyMMdd_HHmmss}.log` |

CSV columns:

```
currentTimestamp, station, trainId, trainOperator, fromStation, arrival, arrivalDelayMinutes, platform
```

### `export-departures`

Fetches departures for every station and writes a CSV file.

```bash
./gradlew bootRun --args="export-departures [options]"
```

| Option | Description | Default |
|---|---|---|
| `-d, --date` | Date in `dd.MM.yyyy` format | today |
| `-o, --output` | Output CSV file path | `departures-{yyyyMMdd_HHmmss}.csv` |
| `--log-file` | Log file path (WARN and above only) | `departures-{yyyyMMdd_HHmmss}.log` |

CSV columns:

```
currentTimestamp, station, trainId, trainOperator, toStation, departure, departureDelayMinutes, platform
```

## Running tests

```bash
./gradlew check
```
