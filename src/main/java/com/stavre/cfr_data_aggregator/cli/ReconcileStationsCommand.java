package com.stavre.cfr_data_aggregator.cli;

import com.stavre.cfr_data_aggregator.export.StationListService;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/** Appends stations from a new file that are not already present in a base file. */
@Component
@Command(
    name = "reconcile-stations",
    mixinStandardHelpOptions = true,
    description = "Append stations from a new file that are not already present in a base file"
)
public class ReconcileStationsCommand implements Runnable {

  private final StationListService stationListService;

  @SuppressWarnings("PMD.ImmutableField")
  @Option(
      names = {"-b", "--base-file"},
      required = true,
      description = "Base CSV file to compare against and append into"
  )
  private File baseFile;

  @SuppressWarnings("PMD.ImmutableField")
  @Option(
      names = {"-n", "--new-file"},
      required = true,
      description = "New CSV file with potentially new station names"
  )
  private File newFile;

  /** Creates the command with its required dependencies. */
  public ReconcileStationsCommand(StationListService stationListService) {
    this.stationListService = stationListService;
  }

  @Override
  public void run() {
    if (!baseFile.exists()) {
      System.out.println("Base file not found. Creating it from new file: " + baseFile.getPath());
    }
    if (!newFile.exists()) {
      System.err.println("Error: new file does not exist: " + newFile.getPath());
      return;
    }
    try {
      List<String> added = stationListService.reconcileStations(baseFile, newFile);
      if (added.isEmpty()) {
        System.out.println("No new stations found.");
      } else {
        for (String name : added) {
          System.out.println("Added: " + name);
        }
      }
    } catch (IOException ex) {
      System.err.println("Reconciliation failed: " + ex.getMessage());
    }
  }
}
