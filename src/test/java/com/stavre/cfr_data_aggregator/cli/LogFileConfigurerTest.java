package com.stavre.cfr_data_aggregator.cli;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.FileAppender;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

class LogFileConfigurerTest {

  private final LogFileConfigurer configurer = new LogFileConfigurer();

  @TempDir
  File tempDir;

  @AfterEach
  void cleanup() {
    configurer.detach();
  }

  @Test
  void attachAddsFileAppenderToRootLogger() {
    configurer.attach(new File(tempDir, "test.log"));

    assertTrue(hasFileAppenderOnRoot(), "root logger must have a FileAppender after attach()");
  }

  @Test
  void detachRemovesAppenderFromRootLogger() {
    File logFile = new File(tempDir, "test.log");
    configurer.attach(logFile);
    configurer.detach();

    assertFalse(hasFileAppenderOnRoot(), "FileAppender must be removed after detach()");
  }

  private boolean hasFileAppenderOnRoot() {
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
    var it = rootLogger.iteratorForAppenders();
    while (it.hasNext()) {
      if (it.next() instanceof FileAppender) {
        return true;
      }
    }
    return false;
  }

  @Test
  void attachCreatesParentDirectory() {
    File logFile = new File(tempDir, "nested/dir/test.log");

    configurer.attach(logFile);

    assertTrue(logFile.getParentFile().exists(), "parent directory must be created by attach()");
    assertTrue(hasFileAppenderOnRoot(), "file appender must still be attached");
  }

  @Test
  void onlyWarnAndAboveAreWrittenToFile() throws IOException {
    File logFile = new File(tempDir, "test.log");
    configurer.attach(logFile);

    org.slf4j.Logger testLogger = LoggerFactory.getLogger(LogFileConfigurerTest.class);
    testLogger.info("this is info — must not appear");
    testLogger.warn("this is a warning — must appear");

    configurer.detach();

    String contents = Files.readString(logFile.toPath(), StandardCharsets.UTF_8);
    assertFalse(contents.contains("this is info"), "INFO messages must be filtered out");
    assertTrue(contents.contains("this is a warning"), "WARN messages must be written");
  }
}
