package com.stavre.cfr_data_aggregator.cli;

import com.stavre.cfr_data_aggregator.export.StationListService;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/** Exports all known CFR stations to a CSV file. */
@Component
@Command(
    name = "export-stations",
    mixinStandardHelpOptions = true,
    description = "Export all known CFR stations to a CSV file"
)
public class ExportStationsCommand implements Runnable {

  private static final DateTimeFormatter FILE_TS_FMT =
      DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

  private final StationListService stationListService;

  @SuppressWarnings("PMD.ImmutableField")
  @Option(names = {"-o", "--output"}, description = "Output CSV file path")
  private File outputFile;

  /** Creates the command with its required dependencies. */
  public ExportStationsCommand(StationListService stationListService) {
    this.stationListService = stationListService;
  }

  @Override
  public void run() {
    String timestamp = LocalDateTime.now().format(FILE_TS_FMT);
    File resolved = outputFile != null ? outputFile : new File("stations-" + timestamp + ".csv");
    try {
      stationListService.exportStations(resolved);
    } catch (IOException ex) {
      System.err.println("Export failed: " + ex.getMessage());
    }
  }
}
