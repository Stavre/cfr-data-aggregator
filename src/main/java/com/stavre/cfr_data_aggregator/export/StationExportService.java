package com.stavre.cfr_data_aggregator.export;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.stavre.cfr_data_aggregator.client.CfrApiClient;
import com.stavre.cfr_data_aggregator.client.dto.StationResponse;
import com.stavre.cfr_data_aggregator.client.dto.StationTrainResponse;
import com.stavre.cfr_data_aggregator.client.dto.TrainMetadataResponse;
import feign.FeignException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import me.tongfei.progressbar.ProgressBar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Fetches station train data from the CFR API and writes CSV files. */
@Service
public class StationExportService {

  private static final Logger log = LoggerFactory.getLogger(StationExportService.class);
  private static final DateTimeFormatter DT_FORMAT =
      DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

  private final CfrApiClient cfrApiClient;
  private final CsvMapper csvMapper = new CsvMapper();

  /** Creates the service with the CFR API client. */
  public StationExportService(CfrApiClient cfrApiClient) {
    this.cfrApiClient = cfrApiClient;
  }

  /** Fetches arrivals for every station and streams them to outputFile as CSV. */
  public void exportArrivals(String date, File outputFile) throws IOException {
    List<StationResponse> stations = cfrApiClient.getAllStations();
    try (SequenceWriter sw = openCsvWriter(ArrivalCsvRecord.class, outputFile);
        ProgressBar pb = new ProgressBar("Arrivals", stations.size())) {
      for (StationResponse station : stations) {
        pb.setExtraMessage(station.getName());
        try {
          LocalDateTime stamp = LocalDateTime.now();
          for (StationTrainResponse train :
              cfrApiClient.getStationArrivals(station.getName(), date)) {
            train.setCurrentTimestamp(stamp);
            sw.write(toArrivalRecord(station.getName(), train));
          }
        } catch (FeignException ex) {
          log.warn("Failed to fetch arrivals for station '{}': {}", station.getName(),
              ex.getMessage());
        }
        pb.step();
      }
    }
  }

  /** Fetches departures for every station and streams them to outputFile as CSV. */
  public void exportDepartures(String date, File outputFile) throws IOException {
    List<StationResponse> stations = cfrApiClient.getAllStations();
    try (SequenceWriter sw = openCsvWriter(DepartureCsvRecord.class, outputFile);
        ProgressBar pb = new ProgressBar("Departures", stations.size())) {
      for (StationResponse station : stations) {
        pb.setExtraMessage(station.getName());
        try {
          LocalDateTime stamp = LocalDateTime.now();
          for (StationTrainResponse train :
              cfrApiClient.getStationDepartures(station.getName(), date)) {
            train.setCurrentTimestamp(stamp);
            sw.write(toDepartureRecord(station.getName(), train));
          }
        } catch (FeignException ex) {
          log.warn("Failed to fetch departures for station '{}': {}", station.getName(),
              ex.getMessage());
        }
        pb.step();
      }
    }
  }

  private <T> SequenceWriter openCsvWriter(Class<T> recordClass, File outputFile)
      throws IOException {
    File parent = outputFile.getParentFile();
    if (parent != null) {
      Files.createDirectories(parent.toPath());
    }
    CsvSchema schema = csvMapper.schemaFor(recordClass).withHeader();
    return csvMapper.writer(schema).writeValues(outputFile);
  }

  private ArrivalCsvRecord toArrivalRecord(String stationName, StationTrainResponse dto) {
    TrainMetadataResponse meta = dto.getTrain();
    return ArrivalCsvRecord.builder()
        .currentTimestamp(formatDateTime(dto.getCurrentTimestamp()))
        .station(stationName)
        .trainId(meta != null ? meta.getId() : null)
        .trainOperator(meta != null ? meta.getOperator() : null)
        .fromStation(dto.getFromStation())
        .arrival(formatDateTime(dto.getArrival()))
        .arrivalDelayMinutes(dto.getArrivalDelay() != null
            ? dto.getArrivalDelay().toMinutes() : null)
        .platform(dto.getPlatform())
        .build();
  }

  private DepartureCsvRecord toDepartureRecord(String stationName, StationTrainResponse dto) {
    TrainMetadataResponse meta = dto.getTrain();
    return DepartureCsvRecord.builder()
        .currentTimestamp(formatDateTime(dto.getCurrentTimestamp()))
        .station(stationName)
        .trainId(meta != null ? meta.getId() : null)
        .trainOperator(meta != null ? meta.getOperator() : null)
        .toStation(dto.getToStation())
        .departure(formatDateTime(dto.getDeparture()))
        .departureDelayMinutes(dto.getDepartureDelay() != null
            ? dto.getDepartureDelay().toMinutes() : null)
        .platform(dto.getPlatform())
        .build();
  }

  private String formatDateTime(LocalDateTime dateTime) {
    return dateTime != null ? dateTime.format(DT_FORMAT) : null;
  }
}
