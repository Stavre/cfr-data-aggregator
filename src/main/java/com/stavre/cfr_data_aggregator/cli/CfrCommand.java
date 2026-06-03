package com.stavre.cfr_data_aggregator.cli;

import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Component
@Command(
    name = "cfr",
    mixinStandardHelpOptions = true,
    version = "0.0.1",
    description = "CFR Data Aggregator CLI"
)
public class CfrCommand implements Runnable {

    @Option(names = {"-v", "--verbose"}, description = "Enable verbose output")
    private boolean verbose;

    @Override
    public void run() {
        System.out.println("CFR Data Aggregator — use --help to see available commands.");
    }
}