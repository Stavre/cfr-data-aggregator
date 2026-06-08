package com.stavre.cfr_data_aggregator.cli;

import com.stavre.cfr_data_aggregator.export.TrainAggregationService;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/** Aggregates delay totals from a train-itineraries CSV into per-train delay files. */
@Component
@Command(
    name = "aggregate-trains",
    mixinStandardHelpOptions = true,
    description = "Aggregate arrival and departure delays per train from a train-itineraries CSV"
)
public class AggregateTrainsCommand implements Runnable {

  private final TrainAggregationService trainAggregationService;

  @SuppressWarnings("PMD.ImmutableField")
  @Option(names = {"-i", "--input-file"}, required = true,
      description = "Path to the source train-itineraries CSV file")
  private File inputFile;

  @SuppressWarnings("PMD.ImmutableField")
  @Option(names = {"-p", "--base-path"}, required = true,
      description = "Base directory for output delay files")
  private Path basePath;

  @SuppressWarnings("PMD.ImmutableField")
  @Option(
      names = {"-t", "--trains"},
      description = "Use 'all' to process every train, or a comma-delimited list. Required.",
      split = ","
  )
  private List<String> trains;

  /** Creates the command with its required dependencies. */
  public AggregateTrainsCommand(TrainAggregationService trainAggregationService) {
    this.trainAggregationService = trainAggregationService;
  }

  @Override
  public void run() {
    if (trains == null || trains.isEmpty()) {
      System.err.println(
          "Error: --trains is required. Use 'all' to process all trains "
          + "or supply a comma-delimited list.");
      return;
    }
    List<String> resolvedTrains = trains.size() == 1
        && "all".equalsIgnoreCase(trains.get(0)) ? null : trains;

    try {
      trainAggregationService.aggregateDelays(inputFile, basePath, resolvedTrains);
    } catch (IOException ex) {
      System.err.println("Aggregation failed: " + ex.getMessage());
    }
  }
}
