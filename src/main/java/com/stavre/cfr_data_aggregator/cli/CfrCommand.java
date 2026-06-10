package com.stavre.cfr_data_aggregator.cli;

import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;

/** Root CLI command. Use a subcommand to perform an action. */
@Component
@Command(
    name = "cfr",
    mixinStandardHelpOptions = true,
    version = "0.0.1",
    description = "CFR Data Aggregator CLI",
    subcommands = {
        ExportArrivalsDeparturesCommand.class,
        ExportTrainItinerariesCommand.class,
        AggregateDelaysCommand.class,
        AggregateTrainsCommand.class,
        ExportStationsCommand.class,
        ExportTrainsCommand.class,
        ReconcileStationsCommand.class
    }
)
public class CfrCommand implements Runnable {

  @Override
  public void run() {
    System.out.println("CFR Data Aggregator — use --help to see available commands.");
  }
}
