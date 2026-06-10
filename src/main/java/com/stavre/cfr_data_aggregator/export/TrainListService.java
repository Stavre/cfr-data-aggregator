package com.stavre.cfr_data_aggregator.export;

import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.stavre.cfr_data_aggregator.client.CfrApiClient;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import com.stavre.cfr_data_aggregator.records.TrainCsvRecord;
import org.springframework.stereotype.Service;

/** Fetches and exports the train list CSV file. */
@Service
public class TrainListService {

  private final CfrApiClient cfrApiClient;
  private final CsvMapper csvMapper = new CsvMapper();

  /** Creates the service with the CFR API client. */
  public TrainListService(CfrApiClient cfrApiClient) {
    this.cfrApiClient = cfrApiClient;
  }

  /** Fetches all train numbers from the API and writes them to outputFile as CSV. */
  public void exportTrains(File outputFile) throws IOException {
    List<String> numbers = cfrApiClient.getAllTrains();
    try (SequenceWriter sw = openCsvWriter(outputFile)) {
      for (String number : numbers) {
        sw.write(TrainCsvRecord.builder().number(number).build());
      }
    }
  }

  private SequenceWriter openCsvWriter(File outputFile) throws IOException {
    File parent = outputFile.getParentFile();
    if (parent != null) {
      Files.createDirectories(parent.toPath());
    }
    CsvSchema schema = csvMapper.schemaFor(TrainCsvRecord.class).withHeader();
    return csvMapper.writer(schema).writeValues(outputFile);
  }
}
