package com.stavre.cfr_data_aggregator.cli;

import com.stavre.cfr_data_aggregator.export.DelayAggregationService;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/** Aggregates delay totals from an arrivalsDepartures CSV into per-station delay files. */
@Component
@Command(
    name = "aggregate-delays",
    mixinStandardHelpOptions = true,
    description = "Aggregate arrival and departure delays per station from an export CSV"
)
public class AggregateDelaysCommand implements Runnable {

  private final DelayAggregationService delayAggregationService;

  @SuppressWarnings("PMD.ImmutableField")
  @Option(names = {"-i", "--input-file"}, required = true,
      description = "Path to the source arrivalsDepartures CSV file")
  private File inputFile;

  @SuppressWarnings("PMD.ImmutableField")
  @Option(names = {"-p", "--base-path"}, required = true,
      description = "Base directory for output delay files")
  private Path basePath;

  @SuppressWarnings("PMD.ImmutableField")
  @Option(
      names = {"-s", "--stations"},
      description = "Use 'all' to process every station, or a comma-delimited list. Required.",
      split = ","
  )
  private List<String> stations;

  /** Creates the command with its required dependencies. */
  public AggregateDelaysCommand(DelayAggregationService delayAggregationService) {
    this.delayAggregationService = delayAggregationService;
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

    try {
      delayAggregationService.aggregateDelays(inputFile, basePath, resolvedStations);
    } catch (IOException ex) {
      System.err.println("Aggregation failed: " + ex.getMessage());
    }
  }
}
