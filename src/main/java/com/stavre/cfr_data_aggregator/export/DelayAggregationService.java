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

/** Reads an arrivalsDepartures CSV and writes per-station aggregated delay files. */
@Service
public class DelayAggregationService {

  private final CsvMapper csvMapper = new CsvMapper();

  /**
   * Reads inputFile, sums arrival and departure delays per station, and appends one row per
   * station to {basePath}/arrivals/{station}/delay.csv and
   * {basePath}/departures/{station}/delay.csv. If the output file already exists the row is
   * appended without writing a second header.
   */
  public void aggregateDelays(File inputFile, Path basePath, List<String> stations)
      throws IOException {
    Map<String, Long> arrivalDelays = new LinkedHashMap<>();
    Map<String, Long> departureDelays = new LinkedHashMap<>();
    Map<String, String> timestamps = new LinkedHashMap<>();
    Map<String, String> cfrDates = new LinkedHashMap<>();

    accumulateRows(inputFile, stations, arrivalDelays, departureDelays, timestamps, cfrDates);

    for (Map.Entry<String, Long> entry : arrivalDelays.entrySet()) {
      String station = entry.getKey();
      File out = basePath.resolve("arrivals").resolve(station).resolve("delay.csv").toFile();
      ArrivalDelayRecord record = ArrivalDelayRecord.builder()
          .currentTimestamp(timestamps.get(station))
          .cfrDate(cfrDates.get(station))
          .station(station)
          .totalDelayMinutes(entry.getValue())
          .build();
      try (SequenceWriter sw = openWriter(ArrivalDelayRecord.class, out)) {
        sw.write(record);
      }
    }

    for (Map.Entry<String, Long> entry : departureDelays.entrySet()) {
      String station = entry.getKey();
      File out = basePath.resolve("departures").resolve(station).resolve("delay.csv").toFile();
      DepartureDelayRecord record = DepartureDelayRecord.builder()
          .currentTimestamp(timestamps.get(station))
          .cfrDate(cfrDates.get(station))
          .station(station)
          .totalDelayMinutes(entry.getValue())
          .build();
      try (SequenceWriter sw = openWriter(DepartureDelayRecord.class, out)) {
        sw.write(record);
      }
    }
  }

  private void accumulateRows(
      File inputFile,
      List<String> stations,
      Map<String, Long> arrivalDelays,
      Map<String, Long> departureDelays,
      Map<String, String> timestamps,
      Map<String, String> cfrDates) throws IOException {
    try (MappingIterator<Map<String, String>> rows = openInputReader(inputFile)) {
      while (rows.hasNext()) {
        Map<String, String> row = rows.next();
        String station = row.get("station");
        if (stations != null && !stations.isEmpty() && !stations.contains(station)) {
          continue;
        }
        timestamps.putIfAbsent(station, row.get("currentTimestamp"));
        cfrDates.putIfAbsent(station, row.get("cfr_date"));

        String arrival = row.get("arrival");
        if (arrival != null && !arrival.isBlank()) {
          arrivalDelays.merge(station, parseLong(row.get("arrivalDelayMinutes")), Long::sum);
        }
        String departure = row.get("departure");
        if (departure != null && !departure.isBlank()) {
          departureDelays.merge(station, parseLong(row.get("departureDelayMinutes")), Long::sum);
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

  private long parseLong(String value) {
    if (value == null || value.isBlank()) {
      return 0L;
    }
    return Long.parseLong(value.trim());
  }
}
