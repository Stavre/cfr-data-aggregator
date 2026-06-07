package com.stavre.cfr_data_aggregator.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.stavre.cfr_data_aggregator.client.CfrApiClient;
import com.stavre.cfr_data_aggregator.client.dto.StationResponse;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StationListServiceTest {

  @Mock
  private CfrApiClient cfrApiClient;

  @InjectMocks
  private StationListService stationListService;

  @TempDir
  File tempDir;

  @Test
  void exportStationsWritesHeaderAndRows() throws IOException {
    StationResponse a = new StationResponse();
    a.setName("Cluj-Napoca");
    StationResponse b = new StationResponse();
    b.setName("Brasov");
    when(cfrApiClient.getAllStations()).thenReturn(List.of(a, b));

    File output = new File(tempDir, "stations.csv");
    stationListService.exportStations(output);

    List<String> lines = Files.readAllLines(output.toPath(), StandardCharsets.UTF_8);
    assertEquals("name,isImportant", lines.get(0));
    assertEquals("Cluj-Napoca,false", lines.get(1));
    assertEquals("Brasov,false", lines.get(2));
  }

  @Test
  void exportStationsCreatesParentDirectories() throws IOException {
    when(cfrApiClient.getAllStations()).thenReturn(List.of());

    File output = new File(tempDir, "nested/dir/stations.csv");
    stationListService.exportStations(output);

    assertTrue(output.exists());
  }

  @Test
  void exportStationsEmptyListWritesHeaderOnly() throws IOException {
    when(cfrApiClient.getAllStations()).thenReturn(List.of());

    File output = new File(tempDir, "stations.csv");
    stationListService.exportStations(output);

    List<String> lines = Files.readAllLines(output.toPath(), StandardCharsets.UTF_8);
    assertEquals(1, lines.size());
    assertEquals("name,isImportant", lines.get(0));
  }

  @Test
  void reconcileAppendsNewNamesPreservingIsImportant() throws IOException {
    File baseFile = writeCsv("base.csv", "name,isImportant", "Cluj-Napoca,false");
    File newFile = writeCsv("new.csv", "name,isImportant", "Cluj-Napoca,false", "Brasov,true");

    List<String> added = stationListService.reconcileStations(baseFile, newFile);
    assertEquals(List.of("Brasov"), added);

    List<String> lines = Files.readAllLines(baseFile.toPath(), StandardCharsets.UTF_8);
    assertEquals("name,isImportant", lines.get(0));
    assertEquals("Cluj-Napoca,false", lines.get(1));
    assertEquals("Brasov,true", lines.get(2));
    assertEquals(3, lines.size());
  }

  @Test
  void reconcileDoesNotAppendDuplicates() throws IOException {
    File baseFile = writeCsv("base.csv", "name,isImportant", "Cluj-Napoca,false");
    File newFile = writeCsv("new.csv", "name,isImportant", "Cluj-Napoca,false");

    List<String> added = stationListService.reconcileStations(baseFile, newFile);

    List<String> lines = Files.readAllLines(baseFile.toPath(), StandardCharsets.UTF_8);
    assertEquals(2, lines.size());
    assertTrue(added.isEmpty());
  }

  @Test
  void reconcileDoesNotWriteSecondHeader() throws IOException {
    File baseFile = writeCsv("base.csv", "name,isImportant", "Cluj-Napoca,false");
    File newFile = writeCsv("new.csv", "name,isImportant", "Brasov,false");

    List<String> added = stationListService.reconcileStations(baseFile, newFile);

    long headerCount = Files.lines(baseFile.toPath())
        .filter(l -> l.startsWith("name"))
        .count();
    assertEquals(1, headerCount);
    assertEquals(List.of("Brasov"), added);
  }

  @Test
  void reconcileEmptyNewFileChangesNothing() throws IOException {
    File baseFile = writeCsv("base.csv", "name,isImportant", "Cluj-Napoca,false");
    File newFile = writeCsv("new.csv", "name,isImportant");

    long beforeSize = baseFile.length();
    List<String> added = stationListService.reconcileStations(baseFile, newFile);

    assertEquals(beforeSize, baseFile.length());
    assertTrue(added.isEmpty());
  }

  @Test
  void reconcileIsCaseSensitive() throws IOException {
    File baseFile = writeCsv("base.csv", "name,isImportant", "Brasov,false");
    File newFile = writeCsv("new.csv", "name,isImportant", "brasov,false");

    List<String> added = stationListService.reconcileStations(baseFile, newFile);

    List<String> lines = Files.readAllLines(baseFile.toPath(), StandardCharsets.UTF_8);
    assertEquals(3, lines.size(), "'brasov' must be treated as a distinct name from 'Brasov'");
    assertEquals(List.of("brasov"), added);
  }

  @Test
  void reconcileCreatesBaseFileFromNewFileWhenBaseDoesNotExist() throws IOException {
    File baseFile = new File(tempDir, "base.csv");
    File newFile = writeCsv("new.csv", "name,isImportant", "Cluj-Napoca,false", "Brasov,true");

    List<String> added = stationListService.reconcileStations(baseFile, newFile);

    assertEquals(List.of("Cluj-Napoca", "Brasov"), added);
    assertTrue(baseFile.exists());
    List<String> lines = Files.readAllLines(baseFile.toPath(), StandardCharsets.UTF_8);
    assertEquals("name,isImportant", lines.get(0));
    assertEquals(3, lines.size());
  }

  private File writeCsv(String filename, String... lines) throws IOException {
    File file = new File(tempDir, filename);
    Files.writeString(file.toPath(), String.join("\n", lines) + "\n", StandardCharsets.UTF_8);
    return file;
  }
}
