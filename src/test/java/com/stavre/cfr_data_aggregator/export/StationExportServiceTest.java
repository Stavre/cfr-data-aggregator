package com.stavre.cfr_data_aggregator.export;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
  void exportArrivalsWritesHeaderAndRow() throws IOException {
    when(cfrApiClient.getAllStations()).thenReturn(List.of(stationResponse("Brasov")));
    when(cfrApiClient.getStationArrivals(eq("Brasov"), any()))
        .thenReturn(List.of(trainResponse()));

    File out = new File(tempDir, "arrivals.csv");
    service.exportArrivals("04.06.2026", out);

    String csv = Files.readString(out.toPath(), StandardCharsets.UTF_8);
    assertTrue(csv.contains("currentTimestamp,station,trainId,trainOperator,"
        + "fromStation,arrival,arrivalDelayMinutes,platform"), "header must be present");
    assertTrue(csv.contains("Brasov"), "station name must appear in data");
  }

  @Test
  void exportArrivalsSkipsStationOnFeignException() throws IOException {
    when(cfrApiClient.getAllStations())
        .thenReturn(List.of(stationResponse("Brasov"), stationResponse("Cluj-Napoca")));
    when(cfrApiClient.getStationArrivals(eq("Brasov"), any()))
        .thenThrow(feignException());
    when(cfrApiClient.getStationArrivals(eq("Cluj-Napoca"), any()))
        .thenReturn(List.of(trainResponse()));

    File out = new File(tempDir, "arrivals.csv");
    service.exportArrivals("04.06.2026", out);

    String csv = Files.readString(out.toPath(), StandardCharsets.UTF_8);
    assertTrue(csv.contains("Cluj-Napoca"), "successful station must be in CSV");
    assertFalse(csv.contains("Brasov"), "failed station must not be in CSV");
  }

  @Test
  void exportArrivalsHandlesNullFields() throws IOException {
    StationTrainResponse dto = new StationTrainResponse();
    when(cfrApiClient.getAllStations()).thenReturn(List.of(stationResponse("Brasov")));
    when(cfrApiClient.getStationArrivals(eq("Brasov"), any())).thenReturn(List.of(dto));

    File out = new File(tempDir, "arrivals.csv");
    service.exportArrivals("04.06.2026", out);

    String csv = Files.readString(out.toPath(), StandardCharsets.UTF_8);
    assertTrue(out.exists(), "output file must be created even with null fields");
    assertFalse(csv.isBlank(), "CSV must not be empty");
  }

  @Test
  void exportArrivalsPopulatesCurrentTimestamp() throws IOException {
    when(cfrApiClient.getAllStations()).thenReturn(List.of(stationResponse("Brasov")));
    when(cfrApiClient.getStationArrivals(eq("Brasov"), any()))
        .thenReturn(List.of(trainResponse()));

    File out = new File(tempDir, "arrivals.csv");
    service.exportArrivals("04.06.2026", out);

    String csv = Files.readString(out.toPath(), StandardCharsets.UTF_8);
    String dataLine = csv.lines().skip(1).findFirst().orElse("");
    assertFalse(dataLine.startsWith(","), "currentTimestamp column must not be empty");
  }

  @Test
  void exportDeparturesWritesHeaderAndRow() throws IOException {
    when(cfrApiClient.getAllStations()).thenReturn(List.of(stationResponse("Brasov")));
    when(cfrApiClient.getStationDepartures(eq("Brasov"), any()))
        .thenReturn(List.of(trainResponse()));

    File out = new File(tempDir, "departures.csv");
    service.exportDepartures("04.06.2026", out);

    String csv = Files.readString(out.toPath(), StandardCharsets.UTF_8);
    assertTrue(csv.contains("currentTimestamp,station,trainId,trainOperator,"
        + "toStation,departure,departureDelayMinutes,platform"), "header must be present");
    assertTrue(csv.contains("Brasov"), "station name must appear in data");
  }

  @Test
  void exportDeparturesSkipsStationOnFeignException() throws IOException {
    when(cfrApiClient.getAllStations())
        .thenReturn(List.of(stationResponse("Brasov"), stationResponse("Cluj-Napoca")));
    when(cfrApiClient.getStationDepartures(eq("Brasov"), any()))
        .thenThrow(feignException());
    when(cfrApiClient.getStationDepartures(eq("Cluj-Napoca"), any()))
        .thenReturn(List.of(trainResponse()));

    File out = new File(tempDir, "departures.csv");
    service.exportDepartures("04.06.2026", out);

    String csv = Files.readString(out.toPath(), StandardCharsets.UTF_8);
    assertTrue(csv.contains("Cluj-Napoca"), "successful station must be in CSV");
    assertFalse(csv.contains("Brasov"), "failed station must not be in CSV");
  }

  @Test
  void exportDeparturesHandlesNullFields() throws IOException {
    StationTrainResponse dto = new StationTrainResponse();
    when(cfrApiClient.getAllStations()).thenReturn(List.of(stationResponse("Brasov")));
    when(cfrApiClient.getStationDepartures(eq("Brasov"), any())).thenReturn(List.of(dto));

    File out = new File(tempDir, "departures.csv");
    service.exportDepartures("04.06.2026", out);

    assertTrue(out.exists(), "output file must be created even with null fields");
  }

  @Test
  void exportDeparturesPopulatesCurrentTimestamp() throws IOException {
    when(cfrApiClient.getAllStations()).thenReturn(List.of(stationResponse("Brasov")));
    when(cfrApiClient.getStationDepartures(eq("Brasov"), any()))
        .thenReturn(List.of(trainResponse()));

    File out = new File(tempDir, "departures.csv");
    service.exportDepartures("04.06.2026", out);

    String csv = Files.readString(out.toPath(), StandardCharsets.UTF_8);
    String dataLine = csv.lines().skip(1).findFirst().orElse("");
    assertFalse(dataLine.startsWith(","), "currentTimestamp column must not be empty");
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
    return FeignException.errorStatus("getStationArrivals",
        feign.Response.builder()
            .status(500)
            .reason("Internal Server Error")
            .request(request)
            .headers(Collections.emptyMap())
            .build());
  }
}
