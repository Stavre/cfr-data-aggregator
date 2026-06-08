package com.stavre.cfr_data_aggregator.export;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/** Reads a train-itineraries CSV and writes per-train, per-station aggregated delay files. */
@Service
public class TrainAggregationService {

  private final CsvMapper csvMapper = new CsvMapper();

  /**
   * Reads inputFile, sums arrival and departure delays per train per station, and appends one row
   * per station to {basePath}/arrivals/{letter}/{trainId}/delay.csv and
   * {basePath}/departures/{letter}/{trainId}/delay.csv.
   * If the output file already exists rows are appended without writing a second header.
   */
  public void aggregateDelays(File inputFile, Path basePath, List<String> trains)
      throws IOException {
    Map<String, Map<String, Long>> arrivalDelays = new LinkedHashMap<>();
    Map<String, Map<String, Long>> departureDelays = new LinkedHashMap<>();
    Map<String, String> timestamps = new LinkedHashMap<>();
    Map<String, String> cfrDates = new LinkedHashMap<>();

    accumulateRows(inputFile, trains, arrivalDelays, departureDelays, timestamps, cfrDates);

    for (Map.Entry<String, Map<String, Long>> trainEntry : arrivalDelays.entrySet()) {
      String trainId = trainEntry.getKey();
      File out = basePath.resolve("arrivals").resolve(firstLetter(trainId))
          .resolve(sanitizeTrainId(trainId)).resolve("delay.csv").toFile();
      try (SequenceWriter sw = openWriter(TrainArrivalDelayRecord.class, out)) {
        for (Map.Entry<String, Long> stationEntry : trainEntry.getValue().entrySet()) {
          sw.write(TrainArrivalDelayRecord.builder()
              .currentTimestamp(timestamps.get(trainId))
              .cfrDate(cfrDates.get(trainId))
              .trainId(trainId)
              .station(stationEntry.getKey())
              .totalDelayMinutes(stationEntry.getValue())
              .build());
        }
      }
    }

    for (Map.Entry<String, Map<String, Long>> trainEntry : departureDelays.entrySet()) {
      String trainId = trainEntry.getKey();
      File out = basePath.resolve("departures").resolve(firstLetter(trainId))
          .resolve(sanitizeTrainId(trainId)).resolve("delay.csv").toFile();
      try (SequenceWriter sw = openWriter(TrainDepartureDelayRecord.class, out)) {
        for (Map.Entry<String, Long> stationEntry : trainEntry.getValue().entrySet()) {
          sw.write(TrainDepartureDelayRecord.builder()
              .currentTimestamp(timestamps.get(trainId))
              .cfrDate(cfrDates.get(trainId))
              .trainId(trainId)
              .station(stationEntry.getKey())
              .totalDelayMinutes(stationEntry.getValue())
              .build());
        }
      }
    }
  }

  private void accumulateRows(
      File inputFile,
      List<String> trains,
      Map<String, Map<String, Long>> arrivalDelays,
      Map<String, Map<String, Long>> departureDelays,
      Map<String, String> timestamps,
      Map<String, String> cfrDates) throws IOException {
    try (MappingIterator<Map<String, String>> rows = openInputReader(inputFile)) {
      while (rows.hasNext()) {
        Map<String, String> row = rows.next();
        String trainId = row.get("trainId");
        if (trains != null && !trains.isEmpty() && !trains.contains(trainId)) {
          continue;
        }
        String station = row.get("station");
        timestamps.putIfAbsent(trainId, row.get("currentTimestamp"));
        cfrDates.putIfAbsent(trainId, row.get("cfr_date"));

        String arrival = row.get("arrival");
        if (arrival != null && !arrival.isBlank()) {
          arrivalDelays.computeIfAbsent(trainId, k -> new LinkedHashMap<>())
              .merge(station, parseLong(row.get("arrivalDelayMinutes")), Long::sum);
        }
        String departure = row.get("departure");
        if (departure != null && !departure.isBlank()) {
          departureDelays.computeIfAbsent(trainId, k -> new LinkedHashMap<>())
              .merge(station, parseLong(row.get("departureDelayMinutes")), Long::sum);
        }
      }
    }
  }

  private MappingIterator<Map<String, String>> openInputReader(File inputFile) throws IOException {
    CsvSchema inputSchema = CsvSchema.emptySchema().withHeader();
    return csvMapper.readerFor(new TypeReference<Map<String, String>>() {})
        .with(inputSchema)
        .readValues(inputFile);
  }

  private <T> SequenceWriter openWriter(Class<T> recordClass, File outputFile) throws IOException {
    boolean appendMode = outputFile.exists();
    File parent = outputFile.getParentFile();
    if (parent != null) {
      Files.createDirectories(parent.toPath());
    }
    CsvSchema schema = csvMapper.schemaFor(recordClass);
    if (!appendMode) {
      schema = schema.withHeader();
    }
    return csvMapper.writer(schema).writeValues(new FileOutputStream(outputFile, true));
  }

  private String firstLetter(String trainId) {
    return trainId.substring(0, 1).toUpperCase();
  }

  private String sanitizeTrainId(String trainId) {
    return trainId.replaceAll("\\s", "_").replace(".", ",");
  }

  private long parseLong(String value) {
    if (value == null || value.isBlank()) {
      return 0L;
    }
    return Long.parseLong(value.trim());
  }
}
