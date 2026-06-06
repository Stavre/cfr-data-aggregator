package com.stavre.cfr_data_aggregator.cli;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.stavre.cfr_data_aggregator.export.StationExportService;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExportArrivalsDeparturesCommandTest {

  @Mock
  private StationExportService stationExportService;

  @Mock
  private LogFileConfigurer logFileConfigurer;

  @Test
  void defaultOutputFileNameMatchesTimestampPattern() throws Exception {
    ExportArrivalsDeparturesCommand cmd =
        new ExportArrivalsDeparturesCommand(stationExportService, logFileConfigurer);
    setField(cmd, "stations", List.of("all"));
    cmd.run();

    ArgumentCaptor<File> captor = ArgumentCaptor.forClass(File.class);
    verify(stationExportService).exportArrivalsDepartures(any(), captor.capture(), any());
    assertTrue(captor.getValue().getName().matches("arrivals-departures-\\d{8}_\\d{6}\\.csv"),
        "CSV filename must match arrivals-departures-yyyyMMdd_HHmmss.csv");
  }

  @Test
  void defaultLogFileNameMatchesTimestampPattern() throws Exception {
    ExportArrivalsDeparturesCommand cmd =
        new ExportArrivalsDeparturesCommand(stationExportService, logFileConfigurer);
    setField(cmd, "stations", List.of("all"));
    cmd.run();

    ArgumentCaptor<File> captor = ArgumentCaptor.forClass(File.class);
    verify(logFileConfigurer).attach(captor.capture());
    assertTrue(captor.getValue().getName().matches("arrivals-departures-\\d{8}_\\d{6}\\.log"),
        "Log filename must match arrivals-departures-yyyyMMdd_HHmmss.log");
  }

  @Test
  void customOutputFileIsPassedThrough() throws Exception {
    ExportArrivalsDeparturesCommand cmd =
        new ExportArrivalsDeparturesCommand(stationExportService, logFileConfigurer);
    setField(cmd, "stations", List.of("all"));
    File custom = new File("custom.csv");
    setField(cmd, "outputFile", custom);
    cmd.run();

    ArgumentCaptor<File> captor = ArgumentCaptor.forClass(File.class);
    verify(stationExportService).exportArrivalsDepartures(any(), captor.capture(), any());
    assertEquals("custom.csv", captor.getValue().getName(),
        "Custom output file must be passed to service");
  }

  @Test
  void logFileConfigurerDetachedEvenOnServiceException() throws Exception {
    doThrow(new IOException("boom")).when(stationExportService)
        .exportArrivalsDepartures(any(), any(), any());
    ExportArrivalsDeparturesCommand cmd =
        new ExportArrivalsDeparturesCommand(stationExportService, logFileConfigurer);
    setField(cmd, "stations", List.of("all"));

    assertDoesNotThrow(cmd::run, "IOException must not propagate out of run()");
    verify(logFileConfigurer).detach();
  }

  @Test
  void missingStationsPrintsErrorAndDoesNotCallService() throws IOException {
    ExportArrivalsDeparturesCommand cmd =
        new ExportArrivalsDeparturesCommand(stationExportService, logFileConfigurer);
    cmd.run();

    verify(stationExportService, never()).exportArrivalsDepartures(any(), any(), any());
  }

  @Test
  @SuppressWarnings("unchecked")
  void allKeywordPassesNullToService() throws Exception {
    ExportArrivalsDeparturesCommand cmd =
        new ExportArrivalsDeparturesCommand(stationExportService, logFileConfigurer);
    setField(cmd, "stations", List.of("all"));
    cmd.run();

    ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
    verify(stationExportService).exportArrivalsDepartures(any(), any(), captor.capture());
    assertNull(captor.getValue(), "'all' must be translated to null so service fetches all stations");
  }

  @Test
  @SuppressWarnings("unchecked")
  void allKeywordIsCaseInsensitive() throws Exception {
    ExportArrivalsDeparturesCommand cmd =
        new ExportArrivalsDeparturesCommand(stationExportService, logFileConfigurer);
    setField(cmd, "stations", List.of("ALL"));
    cmd.run();

    ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
    verify(stationExportService).exportArrivalsDepartures(any(), any(), captor.capture());
    assertNull(captor.getValue(), "'ALL' must be treated the same as 'all'");
  }

  @Test
  @SuppressWarnings("unchecked")
  void stationsListPassedToServiceWhenOptionProvided() throws Exception {
    ExportArrivalsDeparturesCommand cmd =
        new ExportArrivalsDeparturesCommand(stationExportService, logFileConfigurer);
    setField(cmd, "stations", List.of("Cluj-Napoca", "Brasov"));
    cmd.run();

    ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
    verify(stationExportService).exportArrivalsDepartures(any(), any(), captor.capture());
    assertEquals(List.of("Cluj-Napoca", "Brasov"), captor.getValue(),
        "provided stations list must be forwarded to service");
  }

  private void setField(Object target, String fieldName, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }
}
