package com.stavre.cfr_data_aggregator.cli;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stavre.cfr_data_aggregator.export.StationListService;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReconcileStationsCommandTest {

  @Mock
  private StationListService stationListService;

  @TempDir
  File tempDir;

  @Test
  void serviceCalledWhenBothFilesExist() throws Exception {
    File base = createTempFile("base.csv");
    File newf = createTempFile("new.csv");
    when(stationListService.reconcileStations(any(), any())).thenReturn(List.of());
    ReconcileStationsCommand cmd = buildCmd(base, newf);

    cmd.run();

    verify(stationListService).reconcileStations(base, newf);
  }

  @Test
  void missingBaseFileForwardsToService() throws Exception {
    File base = new File(tempDir, "nonexistent-base.csv");
    File newf = createTempFile("new.csv");
    when(stationListService.reconcileStations(any(), any())).thenReturn(List.of());
    ReconcileStationsCommand cmd = buildCmd(base, newf);

    cmd.run();

    verify(stationListService).reconcileStations(base, newf);
  }

  @Test
  void missingNewFilePrintsErrorAndDoesNotCallService() throws Exception {
    File base = createTempFile("base.csv");
    File newf = new File(tempDir, "nonexistent-new.csv");
    ReconcileStationsCommand cmd = buildCmd(base, newf);

    cmd.run();

    verify(stationListService, never()).reconcileStations(any(), any());
  }

  @Test
  void baseFileArgumentIsForwardedToService() throws Exception {
    File base = createTempFile("base.csv");
    File newf = createTempFile("new.csv");
    when(stationListService.reconcileStations(any(), any())).thenReturn(List.of());
    ReconcileStationsCommand cmd = buildCmd(base, newf);
    cmd.run();

    ArgumentCaptor<File> captor = ArgumentCaptor.forClass(File.class);
    verify(stationListService).reconcileStations(captor.capture(), any());
    assertEquals(base, captor.getValue());
  }

  @Test
  void newFileArgumentIsForwardedToService() throws Exception {
    File base = createTempFile("base.csv");
    File newf = createTempFile("new.csv");
    when(stationListService.reconcileStations(any(), any())).thenReturn(List.of());
    ReconcileStationsCommand cmd = buildCmd(base, newf);
    cmd.run();

    ArgumentCaptor<File> captor = ArgumentCaptor.forClass(File.class);
    verify(stationListService).reconcileStations(any(), captor.capture());
    assertEquals(newf, captor.getValue());
  }

  @Test
  void ioExceptionDoesNotPropagate() throws Exception {
    File base = createTempFile("base.csv");
    File newf = createTempFile("new.csv");
    doThrow(new IOException("boom")).when(stationListService).reconcileStations(any(), any());
    ReconcileStationsCommand cmd = buildCmd(base, newf);

    assertDoesNotThrow(cmd::run);
  }

  private ReconcileStationsCommand buildCmd(File base, File newFile) throws Exception {
    ReconcileStationsCommand cmd = new ReconcileStationsCommand(stationListService);
    setField(cmd, "baseFile", base);
    setField(cmd, "newFile", newFile);
    return cmd;
  }

  private File createTempFile(String name) throws IOException {
    File f = new File(tempDir, name);
    assertTrue(f.createNewFile());
    return f;
  }

  private void setField(Object target, String fieldName, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  private static void assertTrue(boolean condition) {
    if (!condition) {
      throw new AssertionError("Expected true");
    }
  }
}
