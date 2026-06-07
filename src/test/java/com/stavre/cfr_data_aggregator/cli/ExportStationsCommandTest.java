package com.stavre.cfr_data_aggregator.cli;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.stavre.cfr_data_aggregator.export.StationListService;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExportStationsCommandTest {

  @Mock
  private StationListService stationListService;

  @Test
  void defaultOutputFileNameMatchesTimestampPattern() throws Exception {
    ExportStationsCommand cmd = new ExportStationsCommand(stationListService);
    cmd.run();

    ArgumentCaptor<File> captor = ArgumentCaptor.forClass(File.class);
    verify(stationListService).exportStations(captor.capture());
    assertTrue(captor.getValue().getName().matches("stations-\\d{8}_\\d{6}\\.csv"),
        "CSV filename must match stations-yyyyMMdd_HHmmss.csv");
  }

  @Test
  void customOutputFileIsPassedThrough() throws Exception {
    ExportStationsCommand cmd = new ExportStationsCommand(stationListService);
    File custom = new File("custom.csv");
    setField(cmd, "outputFile", custom);
    cmd.run();

    ArgumentCaptor<File> captor = ArgumentCaptor.forClass(File.class);
    verify(stationListService).exportStations(captor.capture());
    assertEquals("custom.csv", captor.getValue().getName());
  }

  @Test
  void ioExceptionDoesNotPropagate() throws Exception {
    doThrow(new IOException("boom")).when(stationListService).exportStations(any());
    ExportStationsCommand cmd = new ExportStationsCommand(stationListService);

    assertDoesNotThrow(cmd::run);
  }

  @Test
  void serviceIsCalledOnce() throws Exception {
    ExportStationsCommand cmd = new ExportStationsCommand(stationListService);
    cmd.run();

    verify(stationListService).exportStations(any(File.class));
  }

  private void setField(Object target, String fieldName, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }
}
