package com.stavre.cfr_data_aggregator.cli;

import com.stavre.cfr_data_aggregator.export.StationExportService;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/** Exports station arrivals to a CSV file. */
@Component
@Command(
    name = "export-arrivals",
    mixinStandardHelpOptions = true,
    description = "Export station arrivals to a CSV file"
)
public class ExportArrivalsCommand implements Runnable {

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
  @Option(names = {"--log-file"}, description = "Log file path (default: arrivals-{timestamp}.log)")
  private File logFile;

  /** Creates the command with its required dependencies. */
  public ExportArrivalsCommand(
      StationExportService stationExportService,
      LogFileConfigurer logFileConfigurer) {
    this.stationExportService = stationExportService;
    this.logFileConfigurer = logFileConfigurer;
  }

  @Override
  public void run() {
    String resolvedDate = date != null ? date : LocalDate.now().format(DATE_FMT);
    String timestamp = LocalDateTime.now().format(FILE_TS_FMT);
    File resolvedOutput = outputFile != null
        ? outputFile : new File("arrivals-" + timestamp + ".csv");
    File resolvedLog = logFile != null
        ? logFile : new File("arrivals-" + timestamp + ".log");

    logFileConfigurer.attach(resolvedLog);
    try {
      stationExportService.exportArrivals(resolvedDate, resolvedOutput);
    } catch (IOException ex) {
      System.err.println("Export failed: " + ex.getMessage());
    } finally {
      logFileConfigurer.detach();
    }
  }
}
