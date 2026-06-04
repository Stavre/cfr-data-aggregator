package com.stavre.cfr_data_aggregator.cli;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.stavre.cfr_data_aggregator.export.StationExportService;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExportDeparturesCommandTest {

  @Mock
  private StationExportService stationExportService;

  @Mock
  private LogFileConfigurer logFileConfigurer;

  @Test
  void defaultOutputFileNameMatchesTimestampPattern() throws IOException {
    ExportDeparturesCommand cmd =
        new ExportDeparturesCommand(stationExportService, logFileConfigurer);
    cmd.run();

    ArgumentCaptor<File> captor = ArgumentCaptor.forClass(File.class);
    verify(stationExportService).exportDepartures(any(), captor.capture());
    assertTrue(captor.getValue().getName().matches("departures-\\d{8}_\\d{6}\\.csv"),
        "CSV filename must match departures-yyyyMMdd_HHmmss.csv");
  }

  @Test
  void defaultLogFileNameMatchesTimestampPattern() {
    ExportDeparturesCommand cmd =
        new ExportDeparturesCommand(stationExportService, logFileConfigurer);
    cmd.run();

    ArgumentCaptor<File> captor = ArgumentCaptor.forClass(File.class);
    verify(logFileConfigurer).attach(captor.capture());
    assertTrue(captor.getValue().getName().matches("departures-\\d{8}_\\d{6}\\.log"),
        "Log filename must match departures-yyyyMMdd_HHmmss.log");
  }

  @Test
  void customOutputFileIsPassedThrough() throws Exception {
    ExportDeparturesCommand cmd =
        new ExportDeparturesCommand(stationExportService, logFileConfigurer);
    File custom = new File("custom.csv");
    setField(cmd, "outputFile", custom);
    cmd.run();

    ArgumentCaptor<File> captor = ArgumentCaptor.forClass(File.class);
    verify(stationExportService).exportDepartures(any(), captor.capture());
    assertTrue(captor.getValue().getName().equals("custom.csv"),
        "Custom output file must be passed to service");
  }

  @Test
  void logFileConfigurerDetachedEvenOnServiceException() throws IOException {
    doThrow(new IOException("boom")).when(stationExportService).exportDepartures(any(), any());
    ExportDeparturesCommand cmd =
        new ExportDeparturesCommand(stationExportService, logFileConfigurer);

    assertDoesNotThrow(cmd::run, "IOException must not propagate out of run()");
    verify(logFileConfigurer).detach();
  }

  private void setField(Object target, String fieldName, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }
}
