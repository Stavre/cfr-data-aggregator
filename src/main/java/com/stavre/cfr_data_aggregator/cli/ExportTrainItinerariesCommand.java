package com.stavre.cfr_data_aggregator.cli;

import com.stavre.cfr_data_aggregator.export.TrainExportService;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/** Exports train itineraries to a CSV file. */
@Component
@Command(
    name = "export-train-itineraries",
    mixinStandardHelpOptions = true,
    description = "Export train itineraries to a CSV file"
)
public class ExportTrainItinerariesCommand implements Runnable {

  private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
  private static final DateTimeFormatter FILE_TS_FMT =
      DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

  private final TrainExportService trainExportService;
  private final LogFileConfigurer logFileConfigurer;

  @SuppressWarnings("PMD.ImmutableField")
  @Option(
      names = {"-t", "--trains"},
      description = "Use 'all' to process every train, or a comma-delimited list of train numbers."
          + " Required.",
      split = ","
  )
  private List<String> trains;

  @SuppressWarnings("PMD.ImmutableField")
  @Option(names = {"-d", "--date"}, description = "Date in dd.MM.yyyy format (default: today)")
  private String date;

  @SuppressWarnings("PMD.ImmutableField")
  @Option(names = {"-o", "--output"}, description = "Output CSV file path")
  private File outputFile;

  @SuppressWarnings("PMD.ImmutableField")
  @Option(names = {"--log-file"},
      description = "Log file path (default: train-itineraries-{timestamp}.log)")
  private File logFile;

  /** Creates the command with its required dependencies. */
  public ExportTrainItinerariesCommand(
      TrainExportService trainExportService,
      LogFileConfigurer logFileConfigurer) {
    this.trainExportService = trainExportService;
    this.logFileConfigurer = logFileConfigurer;
  }

  @Override
  public void run() {
    if (trains == null || trains.isEmpty()) {
      System.err.println(
          "Error: --trains is required. Use 'all' to process all trains "
          + "or supply a comma-delimited list of train numbers.");
      return;
    }
    List<String> resolvedTrains = trains.size() == 1
        && "all".equalsIgnoreCase(trains.get(0)) ? null : trains;

    String resolvedDate = date != null ? date : LocalDate.now().format(DATE_FMT);
    String timestamp = LocalDateTime.now().format(FILE_TS_FMT);
    File resolvedOutput = outputFile != null
        ? outputFile : new File("train-itineraries-" + timestamp + ".csv");
    File resolvedLog = logFile != null
        ? logFile : new File("train-itineraries-" + timestamp + ".log");

    logFileConfigurer.attach(resolvedLog);
    try {
      trainExportService.exportItineraries(resolvedDate, resolvedOutput, resolvedTrains);
    } catch (IOException ex) {
      System.err.println("Export failed: " + ex.getMessage());
    } finally {
      logFileConfigurer.detach();
    }
  }
}
