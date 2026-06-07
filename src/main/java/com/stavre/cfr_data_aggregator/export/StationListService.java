package com.stavre.cfr_data_aggregator.export;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.stavre.cfr_data_aggregator.client.CfrApiClient;
import com.stavre.cfr_data_aggregator.client.dto.StationResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** Fetches and manages station list CSV files. */
@Service
public class StationListService {

  private final CfrApiClient cfrApiClient;
  private final CsvMapper csvMapper = new CsvMapper();

  /** Creates the service with the CFR API client. */
  public StationListService(CfrApiClient cfrApiClient) {
    this.cfrApiClient = cfrApiClient;
  }

  /** Fetches all stations from the API and writes them to outputFile as CSV. */
  public void exportStations(File outputFile) throws IOException {
    List<StationResponse> stations = cfrApiClient.getAllStations();
    try (SequenceWriter sw = openCsvWriter(outputFile)) {
      for (StationResponse station : stations) {
        sw.write(StationCsvRecord.builder()
            .name(station.getName())
            .isImportant(Boolean.FALSE)
            .build());
      }
    }
  }

  /**
   * Reads newFile, finds station names not already present in baseFile, appends those rows to
   * baseFile without writing a second header, and returns the list of added station names.
   * isImportant is preserved from the source row.
   */
  public List<String> reconcileStations(File baseFile, File newFile) throws IOException {
    if (!baseFile.exists()) {
      List<Map<String, String>> newRecords = readStationRecords(newFile);
      try (SequenceWriter sw = openCsvWriter(baseFile)) {
        for (Map<String, String> row : newRecords) {
          sw.write(StationCsvRecord.builder()
              .name(row.get("name"))
              .isImportant(Boolean.parseBoolean(row.get("isImportant")))
              .build());
        }
      }
      return newRecords.stream().map(r -> r.get("name")).collect(Collectors.toList());
    }

    Set<String> existingNames = readStationNames(baseFile);
    List<Map<String, String>> newRecords = readStationRecords(newFile);

    List<Map<String, String>> toAppend = new ArrayList<>();
    for (Map<String, String> row : newRecords) {
      if (!existingNames.contains(row.get("name"))) {
        toAppend.add(row);
      }
    }

    if (toAppend.isEmpty()) {
      return Collections.emptyList();
    }

    List<String> addedNames = new ArrayList<>();
    CsvSchema schema = csvMapper.schemaFor(StationCsvRecord.class);
    try (SequenceWriter sw = csvMapper.writer(schema)
        .writeValues(new FileOutputStream(baseFile, true))) {
      for (Map<String, String> row : toAppend) {
        sw.write(StationCsvRecord.builder()
            .name(row.get("name"))
            .isImportant(Boolean.parseBoolean(row.get("isImportant")))
            .build());
        addedNames.add(row.get("name"));
      }
    }
    return addedNames;
  }

  private <T> SequenceWriter openCsvWriter(File outputFile) throws IOException {
    File parent = outputFile.getParentFile();
    if (parent != null) {
      Files.createDirectories(parent.toPath());
    }
    CsvSchema schema = csvMapper.schemaFor(StationCsvRecord.class).withHeader();
    return csvMapper.writer(schema).writeValues(outputFile);
  }

  private Set<String> readStationNames(File csvFile) throws IOException {
    Set<String> names = new LinkedHashSet<>();
    try (MappingIterator<Map<String, String>> it = openCsvRecordReader(csvFile)) {
      while (it.hasNext()) {
        String name = it.next().get("name");
        if (name != null) {
          names.add(name);
        }
      }
    }
    return names;
  }

  private List<Map<String, String>> readStationRecords(File csvFile) throws IOException {
    List<Map<String, String>> records = new ArrayList<>();
    try (MappingIterator<Map<String, String>> it = openCsvRecordReader(csvFile)) {
      while (it.hasNext()) {
        records.add(it.next());
      }
    }
    return records;
  }

  private MappingIterator<Map<String, String>> openCsvRecordReader(File csvFile)
      throws IOException {
    CsvSchema schema = CsvSchema.emptySchema().withHeader();
    return csvMapper
        .readerFor(new TypeReference<Map<String, String>>() {})
        .with(schema)
        .readValues(csvFile);
  }
}
