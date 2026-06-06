package com.stavre.cfr_data_aggregator.cli;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.stavre.cfr_data_aggregator.export.DelayAggregationService;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AggregateDelaysCommandTest {

  @Mock
  private DelayAggregationService delayAggregationService;

  @Test
  void inputFileIsForwardedToService() throws Exception {
    AggregateDelaysCommand cmd = new AggregateDelaysCommand(delayAggregationService);
    File input = new File("arrivals-departures.csv");
    setField(cmd, "inputFile", input);
    setField(cmd, "basePath", Path.of("out"));
    setField(cmd, "stations", List.of("all"));
    cmd.run();

    ArgumentCaptor<File> captor = ArgumentCaptor.forClass(File.class);
    verify(delayAggregationService).aggregateDelays(captor.capture(), any(), any());
    assertEquals("arrivals-departures.csv", captor.getValue().getName(),
        "input file must be forwarded to service");
  }

  @Test
  void basePathIsForwardedToService() throws Exception {
    AggregateDelaysCommand cmd = new AggregateDelaysCommand(delayAggregationService);
    Path base = Path.of("output/delays");
    setField(cmd, "inputFile", new File("input.csv"));
    setField(cmd, "basePath", base);
    setField(cmd, "stations", List.of("all"));
    cmd.run();

    ArgumentCaptor<Path> captor = ArgumentCaptor.forClass(Path.class);
    verify(delayAggregationService).aggregateDelays(any(), captor.capture(), any());
    assertEquals(base, captor.getValue(), "base path must be forwarded to service");
  }

  @Test
  void missingStationsPrintsErrorAndDoesNotCallService() throws Exception {
    AggregateDelaysCommand cmd = new AggregateDelaysCommand(delayAggregationService);
    setField(cmd, "inputFile", new File("input.csv"));
    setField(cmd, "basePath", Path.of("out"));
    cmd.run();

    verify(delayAggregationService, never()).aggregateDelays(any(), any(), any());
  }

  @Test
  @SuppressWarnings("unchecked")
  void allKeywordPassesNullToService() throws Exception {
    AggregateDelaysCommand cmd = new AggregateDelaysCommand(delayAggregationService);
    setField(cmd, "inputFile", new File("input.csv"));
    setField(cmd, "basePath", Path.of("out"));
    setField(cmd, "stations", List.of("all"));
    cmd.run();

    ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
    verify(delayAggregationService).aggregateDelays(any(), any(), captor.capture());
    assertNull(captor.getValue(), "'all' must be translated to null so service processes all rows");
  }

  @Test
  @SuppressWarnings("unchecked")
  void allKeywordIsCaseInsensitive() throws Exception {
    AggregateDelaysCommand cmd = new AggregateDelaysCommand(delayAggregationService);
    setField(cmd, "inputFile", new File("input.csv"));
    setField(cmd, "basePath", Path.of("out"));
    setField(cmd, "stations", List.of("ALL"));
    cmd.run();

    ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
    verify(delayAggregationService).aggregateDelays(any(), any(), captor.capture());
    assertNull(captor.getValue(), "'ALL' must be treated the same as 'all'");
  }

  @Test
  @SuppressWarnings("unchecked")
  void stationsListPassedToServiceWhenOptionProvided() throws Exception {
    AggregateDelaysCommand cmd = new AggregateDelaysCommand(delayAggregationService);
    setField(cmd, "inputFile", new File("input.csv"));
    setField(cmd, "basePath", Path.of("out"));
    setField(cmd, "stations", List.of("Brasov", "Cluj-Napoca"));
    cmd.run();

    ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
    verify(delayAggregationService).aggregateDelays(any(), any(), captor.capture());
    assertEquals(List.of("Brasov", "Cluj-Napoca"), captor.getValue(),
        "provided stations list must be forwarded to service");
  }

  @Test
  void ioExceptionDoesNotPropagateOutOfRun() throws Exception {
    doThrow(new IOException("boom")).when(delayAggregationService)
        .aggregateDelays(any(), any(), any());
    AggregateDelaysCommand cmd = new AggregateDelaysCommand(delayAggregationService);
    setField(cmd, "inputFile", new File("input.csv"));
    setField(cmd, "basePath", Path.of("out"));
    setField(cmd, "stations", List.of("all"));

    assertDoesNotThrow(cmd::run, "IOException must not propagate out of run()");
  }

  private void setField(Object target, String fieldName, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }
}
