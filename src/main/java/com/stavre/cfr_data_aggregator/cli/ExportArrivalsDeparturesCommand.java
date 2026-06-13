package com.stavre.cfr_data_aggregator.cli;

import com.stavre.cfr_data_aggregator.export.StationExportService;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.stavre.cfr_data_aggregator.log.LogFileConfigurer;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/** Exports station arrivals and departures to a CSV file. */
@Component
@Command(
    name = "export-arrivals-departures",
    mixinStandardHelpOptions = true,
    description = "Export station arrivals and departures to a CSV file"
)
public class ExportArrivalsDeparturesCommand implements Runnable {

  private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
  private static final DateTimeFormatter FILE_TS_FMT =
      DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

  private final StationExportService stationExportService;
  private final LogFileConfigurer logFileConfigurer;

  @SuppressWarnings("PMD.ImmutableField")
  @Option(names = {"-d", "--date"}, description = "Date in dd.MM.yyyy format (default: today)")
  private String date;

  @SuppressWarnings("PMD.ImmutableField")
  @Option(names = {"-o", "--output"}, description = "Output CSV file path")
  private File outputFile;

  @SuppressWarnings("PMD.ImmutableField")
  @Option(names = {"--log-file"},
      description = "Log file path (default: arrivals-departures-{timestamp}.log)")
  private File logFile;

  @SuppressWarnings("PMD.ImmutableField")
  @Option(
      names = {"-s", "--stations"},
      description = "Use 'all' to process every station, or a comma-delimited list. Required.",
      split = ","
  )
  private List<String> stations;

  /** Creates the command with its required dependencies. */
  public ExportArrivalsDeparturesCommand(
      StationExportService stationExportService,
      LogFileConfigurer logFileConfigurer) {
    this.stationExportService = stationExportService;
    this.logFileConfigurer = logFileConfigurer;
  }

  @Override
  public void run() {
    if (stations == null || stations.isEmpty()) {
      System.err.println(
          "Error: --stations is required. Use 'all' to process all stations "
          + "or supply a comma-delimited list.");
      return;
    }
    List<String> resolvedStations = stations.size() == 1
        && "all".equalsIgnoreCase(stations.get(0)) ? null : stations;

    String resolvedDate = date != null ? date : LocalDate.now().format(DATE_FMT);
    String timestamp = LocalDateTime.now().format(FILE_TS_FMT);
    File resolvedOutput = outputFile != null
        ? outputFile : new File("arrivals-departures-" + timestamp + ".csv");
    File resolvedLog = logFile != null
        ? logFile : new File("arrivals-departures-" + timestamp + ".log");

    logFileConfigurer.attach(resolvedLog);
    try {
      stationExportService.exportArrivalsDepartures(resolvedDate, resolvedOutput, resolvedStations);
    } catch (IOException ex) {
      System.err.println("Export failed: " + ex.getMessage());
    } finally {
      logFileConfigurer.detach();
    }
  }
}
