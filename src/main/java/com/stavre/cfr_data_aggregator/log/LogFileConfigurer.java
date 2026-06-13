package com.stavre.cfr_data_aggregator.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import java.io.File;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Attaches and detaches a WARN-level file appender for the duration of a command run. */
@Component
public class LogFileConfigurer {

  private FileAppender<ILoggingEvent> fileAppender;

  /** Adds a file appender that writes WARN and above to logFile. */
  public void attach(File logFile) {
    File parent = logFile.getParentFile();
    if (parent != null) {
      parent.mkdirs();
    }
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

    PatternLayoutEncoder encoder = new PatternLayoutEncoder();
    encoder.setContext(context);
    encoder.setPattern("%d{dd.MM.yyyy HH:mm:ss} %-5level %logger{36} - %msg%n");
    encoder.start();

    ThresholdFilter filter = new ThresholdFilter();
    filter.setLevel(Level.WARN.toString());
    filter.start();

    fileAppender = new FileAppender<>();
    fileAppender.setContext(context);
    fileAppender.setFile(logFile.getAbsolutePath());
    fileAppender.setEncoder(encoder);
    fileAppender.addFilter(filter);
    fileAppender.start();

    Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
    rootLogger.addAppender(fileAppender);
  }

  /** Removes the appender added by attach(). */
  public void detach() {
    if (fileAppender == null) {
      return;
    }
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
    rootLogger.detachAppender(fileAppender);
    fileAppender.stop();
    fileAppender = null;
  }
}
