package com.stavre.cfr_data_aggregator.export;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stavre.cfr_data_aggregator.client.CfrApiClient;
import com.stavre.cfr_data_aggregator.client.dto.StationResponse;
import com.stavre.cfr_data_aggregator.client.dto.StationTrainResponse;
import com.stavre.cfr_data_aggregator.client.dto.TrainMetadataResponse;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StationExportServiceTest {

  @Mock
  private CfrApiClient cfrApiClient;

  @InjectMocks
  private StationExportService service;

  @TempDir
  File tempDir;

  @Test
  void exportArrivalsDeparturesWritesHeaderAndRow() throws IOException {
    when(cfrApiClient.getAllStations()).thenReturn(List.of(stationResponse("Brasov")));
    when(cfrApiClient.getStation(eq("Brasov"), any())).thenReturn(List.of(trainResponse()));

    File out = new File(tempDir, "out.csv");
    service.exportArrivalsDepartures("04.06.2026", out, null);

    String csv = Files.readString(out.toPath(), StandardCharsets.UTF_8);
    assertTrue(csv.contains("currentTimestamp,cfr_date,station,trainId,trainOperator,"
        + "fromStation,arrival,arrivalDelayMinutes,"
        + "toStation,departure,departureDelayMinutes,platform"),
        "header must be present");
    assertTrue(csv.contains("Brasov"), "station name must appear in data");
  }

  @Test
  void exportArrivalsDeparturesSkipsStationOnFeignException() throws IOException {
    when(cfrApiClient.getAllStations())
        .thenReturn(List.of(stationResponse("Brasov"), stationResponse("Cluj-Napoca")));
    when(cfrApiClient.getStation(eq("Brasov"), any())).thenThrow(feignException());
    when(cfrApiClient.getStation(eq("Cluj-Napoca"), any())).thenReturn(List.of(trainResponse()));

    File out = new File(tempDir, "out.csv");
    service.exportArrivalsDepartures("04.06.2026", out, null);

    String csv = Files.readString(out.toPath(), StandardCharsets.UTF_8);
    assertTrue(csv.contains("Cluj-Napoca"), "successful station must be in CSV");
    assertFalse(csv.contains("Brasov"), "failed station must not be in CSV");
  }

  @Test
  void exportArrivalsDeparturesHandlesNullFields() throws IOException {
    StationTrainResponse dto = new StationTrainResponse();
    when(cfrApiClient.getAllStations()).thenReturn(List.of(stationResponse("Brasov")));
    when(cfrApiClient.getStation(eq("Brasov"), any())).thenReturn(List.of(dto));

    File out = new File(tempDir, "out.csv");
    service.exportArrivalsDepartures("04.06.2026", out, null);

    assertTrue(out.exists(), "output file must be created even with null fields");
    assertFalse(Files.readString(out.toPath(), StandardCharsets.UTF_8).isBlank(),
        "CSV must not be empty");
  }

  @Test
  void exportArrivalsDeparturesPopulatesCurrentTimestamp() throws IOException {
    when(cfrApiClient.getAllStations()).thenReturn(List.of(stationResponse("Brasov")));
    when(cfrApiClient.getStation(eq("Brasov"), any())).thenReturn(List.of(trainResponse()));

    File out = new File(tempDir, "out.csv");
    service.exportArrivalsDepartures("04.06.2026", out, null);

    String csv = Files.readString(out.toPath(), StandardCharsets.UTF_8);
    String dataLine = csv.lines().skip(1).findFirst().orElse("");
    assertFalse(dataLine.startsWith(","), "currentTimestamp column must not be empty");
  }

  @Test
  void exportArrivalsDeparturesCreatesParentDirectory() throws IOException {
    when(cfrApiClient.getAllStations()).thenReturn(List.of(stationResponse("Brasov")));
    when(cfrApiClient.getStation(eq("Brasov"), any())).thenReturn(List.of(trainResponse()));

    File out = new File(tempDir, "nested/dir/out.csv");
    service.exportArrivalsDepartures("04.06.2026", out, null);

    assertTrue(out.exists(), "output file must be created even when parent directory is missing");
  }

  @Test
  void exportArrivalsDeparturesPopulatesCfrDate() throws IOException {
    when(cfrApiClient.getAllStations()).thenReturn(List.of(stationResponse("Brasov")));
    when(cfrApiClient.getStation(eq("Brasov"), any())).thenReturn(List.of(trainResponse()));

    File out = new File(tempDir, "out.csv");
    service.exportArrivalsDepartures("04.06.2026", out, null);

    String csv = Files.readString(out.toPath(), StandardCharsets.UTF_8);
    String dataLine = csv.lines().skip(1).findFirst().orElse("");
    assertTrue(dataLine.contains("04.06.2026"), "cfr_date column must contain the queried date");
  }

  @Test
  void exportArrivalsDeparturesUsesProvidedStationsList() throws IOException {
    when(cfrApiClient.getStation(eq("Cluj-Napoca"), any())).thenReturn(List.of(trainResponse()));

    File out = new File(tempDir, "out.csv");
    service.exportArrivalsDepartures("04.06.2026", out, List.of("Cluj-Napoca"));

    verify(cfrApiClient, never()).getAllStations();
    String csv = Files.readString(out.toPath(), StandardCharsets.UTF_8);
    assertTrue(csv.contains("Cluj-Napoca"), "provided station must appear in CSV");
  }

  private StationResponse stationResponse(String name) {
    StationResponse sr = new StationResponse();
    sr.setName(name);
    return sr;
  }

  private StationTrainResponse trainResponse() {
    TrainMetadataResponse meta = new TrainMetadataResponse();
    meta.setId("IR1234");
    meta.setOperator("CFR Calatori");

    StationTrainResponse dto = new StationTrainResponse();
    dto.setTrain(meta);
    dto.setFromStation("Sinaia");
    dto.setToStation("Bucuresti Nord");
    dto.setArrival(LocalDateTime.of(2026, 6, 4, 10, 30));
    dto.setArrivalDelay(Duration.ofMinutes(5));
    dto.setDeparture(LocalDateTime.of(2026, 6, 4, 10, 35));
    dto.setDepartureDelay(Duration.ZERO);
    dto.setPlatform("3");
    return dto;
  }

  private FeignException feignException() {
    Request request = Request.create(
        Request.HttpMethod.GET, "http://localhost/test",
        Map.of(), new byte[0], StandardCharsets.UTF_8, new RequestTemplate());
    return FeignException.errorStatus("getStation",
        feign.Response.builder()
            .status(500)
            .reason("Internal Server Error")
            .request(request)
            .headers(Collections.emptyMap())
            .build());
  }
}
