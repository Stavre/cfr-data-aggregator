package com.stavre.cfr_data_aggregator;

import com.stavre.cfr_data_aggregator.cli.CfrCommand;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import picocli.CommandLine;
import picocli.CommandLine.IFactory;

/** Entry point for the CFR Data Aggregator CLI application. */
@SpringBootApplication
public class CfrDataAggregatorApplication implements CommandLineRunner, ExitCodeGenerator {

  private final IFactory factory;
  private final CfrCommand cfrCommand;
  private int exitCode;

  /** Wires the picocli root command and Spring-managed IFactory. */
  public CfrDataAggregatorApplication(IFactory factory, CfrCommand cfrCommand) {
    this.factory = factory;
    this.cfrCommand = cfrCommand;
  }

  /**
   * Launches the application and propagates the picocli exit code to the JVM.
   */
  public static void main(String[] args) {
    System.exit(SpringApplication.exit(
        SpringApplication.run(CfrDataAggregatorApplication.class, args)));
  }

  @Override
  public void run(String... args) {
    exitCode = new CommandLine(cfrCommand, factory).execute(args);
  }

  @Override
  public int getExitCode() {
    return exitCode;
  }
}