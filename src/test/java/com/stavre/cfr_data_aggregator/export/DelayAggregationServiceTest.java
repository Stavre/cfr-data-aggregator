package com.stavre.cfr_data_aggregator.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DelayAggregationServiceTest {

  private final DelayAggregationService service = new DelayAggregationService();

  @TempDir
  Path tempDir;

  @Test
  void aggregateDelaysSumsArrivalAndDepartureDelaysSeparately() throws IOException {
    File input = writeInputCsv(
        "currentTimestamp,cfr_date,station,trainId,trainOperator,"
            + "fromStation,arrival,arrivalDelayMinutes,"
            + "toStation,departure,departureDelayMinutes,platform",
        "\"06.06.2026 10:00\",06.06.2026,Brasov,IR1,CFR,Sinaia,"
            + "\"06.06.2026 10:30\",5,Bucuresti Nord,\"06.06.2026 10:35\",3,3",
        "\"06.06.2026 10:01\",06.06.2026,Brasov,IR2,CFR,Ploiesti,"
            + "\"06.06.2026 11:00\",10,Sinaia,\"06.06.2026 11:05\",7,2"
    );

    service.aggregateDelays(input, tempDir, null);

    String arrivals = readOutput("arrivals", "Brasov");
    assertTrue(arrivals.contains("15"), "arrival delays must be summed (5+10=15)");

    String departures = readOutput("departures", "Brasov");
    assertTrue(departures.contains("10"), "departure delays must be summed (3+7=10)");
  }

  @Test
  void aggregateDelaysWritesHeaderOnNewFile() throws IOException {
    File input = writeInputCsv(
        "currentTimestamp,cfr_date,station,trainId,trainOperator,"
            + "fromStation,arrival,arrivalDelayMinutes,"
            + "toStation,departure,departureDelayMinutes,platform",
        "\"06.06.2026 10:00\",06.06.2026,Brasov,IR1,CFR,Sinaia,"
            + "\"06.06.2026 10:30\",5,Bucuresti Nord,\"06.06.2026 10:35\",3,3"
    );

    service.aggregateDelays(input, tempDir, null);

    String arrivals = readOutput("arrivals", "Brasov");
    assertTrue(arrivals.startsWith("currentTimestamp,cfr_date,station,totalDelayMinutes"),
        "header must be present on new file");
  }

  @Test
  void aggregateDelaysAppendsWithoutHeaderOnExistingFile() throws IOException {
    File input = writeInputCsv(
        "currentTimestamp,cfr_date,station,trainId,trainOperator,"
            + "fromStation,arrival,arrivalDelayMinutes,"
            + "toStation,departure,departureDelayMinutes,platform",
        "\"06.06.2026 10:00\",06.06.2026,Brasov,IR1,CFR,Sinaia,"
            + "\"06.06.2026 10:30\",5,Bucuresti Nord,\"06.06.2026 10:35\",3,3"
    );

    service.aggregateDelays(input, tempDir, null);
    service.aggregateDelays(input, tempDir, null);

    String arrivals = readOutput("arrivals", "Brasov");
    long headerCount = arrivals.lines()
        .filter(l -> l.startsWith("currentTimestamp"))
        .count();
    assertEquals(1, headerCount, "header must appear exactly once after two runs");

    long dataRows = arrivals.lines().filter(l -> l.contains("Brasov")).count();
    assertEquals(2, dataRows, "two data rows must be present after two runs");
  }

  @Test
  void aggregateDelaysTreatsNullDelayAsZero() throws IOException {
    File input = writeInputCsv(
        "currentTimestamp,cfr_date,station,trainId,trainOperator,"
            + "fromStation,arrival,arrivalDelayMinutes,"
            + "toStation,departure,departureDelayMinutes,platform",
        "\"06.06.2026 10:00\",06.06.2026,Brasov,IR1,CFR,Sinaia,"
            + "\"06.06.2026 10:30\",,Bucuresti Nord,\"06.06.2026 10:35\",,3"
    );

    service.aggregateDelays(input, tempDir, null);

    String arrivals = readOutput("arrivals", "Brasov");
    assertTrue(arrivals.contains(",0"), "null delay must be treated as 0");
  }

  @Test
  void aggregateDelaysSkipsStationsNotInFilter() throws IOException {
    File input = writeInputCsv(
        "currentTimestamp,cfr_date,station,trainId,trainOperator,"
            + "fromStation,arrival,arrivalDelayMinutes,"
            + "toStation,departure,departureDelayMinutes,platform",
        "\"06.06.2026 10:00\",06.06.2026,Brasov,IR1,CFR,Sinaia,"
            + "\"06.06.2026 10:30\",5,Bucuresti Nord,\"06.06.2026 10:35\",3,3",
        "\"06.06.2026 10:00\",06.06.2026,Cluj-Napoca,IR2,CFR,Oradea,"
            + "\"06.06.2026 12:00\",8,Bucuresti Nord,\"06.06.2026 12:05\",2,1"
    );

    service.aggregateDelays(input, tempDir, java.util.List.of("Brasov"));

    assertTrue(arrivalFileExists("Brasov"), "filtered-in station must produce output");
    assertFalse(arrivalFileExists("Cluj-Napoca"), "filtered-out station must not produce output");
  }

  @Test
  void aggregateDelaysWritesCfrDateFromInput() throws IOException {
    File input = writeInputCsv(
        "currentTimestamp,cfr_date,station,trainId,trainOperator,"
            + "fromStation,arrival,arrivalDelayMinutes,"
            + "toStation,departure,departureDelayMinutes,platform",
        "\"06.06.2026 10:00\",06.06.2026,Brasov,IR1,CFR,Sinaia,"
            + "\"06.06.2026 10:30\",5,Bucuresti Nord,\"06.06.2026 10:35\",3,3"
    );

    service.aggregateDelays(input, tempDir, null);

    String arrivals = readOutput("arrivals", "Brasov");
    assertTrue(arrivals.contains("06.06.2026"), "cfr_date must be present in output");
  }

  @Test
  void aggregateDelaysCreatesParentDirectories() throws IOException {
    File input = writeInputCsv(
        "currentTimestamp,cfr_date,station,trainId,trainOperator,"
            + "fromStation,arrival,arrivalDelayMinutes,"
            + "toStation,departure,departureDelayMinutes,platform",
        "\"06.06.2026 10:00\",06.06.2026,Brasov,IR1,CFR,Sinaia,"
            + "\"06.06.2026 10:30\",5,Bucuresti Nord,\"06.06.2026 10:35\",3,3"
    );

    Path nestedBase = tempDir.resolve("a/b/c");
    service.aggregateDelays(input, nestedBase, null);

    assertTrue(nestedBase.resolve("arrivals/Brasov/delay.csv").toFile().exists(),
        "output file must be created even when parent directories are missing");
  }

  private File writeInputCsv(String... lines) throws IOException {
    File csv = tempDir.resolve("input.csv").toFile();
    Files.writeString(csv.toPath(), String.join("\n", lines) + "\n", StandardCharsets.UTF_8);
    return csv;
  }

  private String readOutput(String type, String station) throws IOException {
    Path path = tempDir.resolve(type).resolve(station).resolve("delay.csv");
    return Files.readString(path, StandardCharsets.UTF_8);
  }

  private boolean arrivalFileExists(String station) {
    return tempDir.resolve("arrivals").resolve(station).resolve("delay.csv").toFile().exists();
  }
}
