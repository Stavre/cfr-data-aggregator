package com.stavre.cfr_data_aggregator.export;

import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.stavre.cfr_data_aggregator.client.CfrApiClient;
import com.stavre.cfr_data_aggregator.client.dto.StationResponse;
import com.stavre.cfr_data_aggregator.client.dto.StationTrainResponse;
import com.stavre.cfr_data_aggregator.client.dto.TrainMetadataResponse;
import com.stavre.cfr_data_aggregator.records.ArrivalsDeparturesCsvRecord;
import feign.FeignException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

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

  /**
   * Fetches merged arrivals and departures and streams them to outputFile as CSV.
   * If stations is null or empty, all stations are fetched from the API.
   */
  public void exportArrivalsDepartures(String date, File outputFile, List<String> stations)
      throws IOException {
    List<String> stationNames = resolveStations(stations);

    try (SequenceWriter sw = openCsvWriter(ArrivalsDeparturesCsvRecord.class, outputFile);
         ProgressBar pb = new ProgressBar("Arrivals+Departures", stationNames.size())) {

      for (String stationName : stationNames) {
        pb.setExtraMessage(stationName);
        try {
          LocalDateTime stamp = LocalDateTime.now();
          for (StationTrainResponse train : cfrApiClient.getStation(stationName, date)) {
            train.setCurrentTimestamp(stamp);
            sw.write(toArrivalsDeparturesRecord(stationName, date, train));
          }
        } catch (FeignException ex) {
          log.warn("Failed to fetch arrivals+departures for station '{}': {}", stationName,
              ex.getMessage());
        }
        pb.step();
      }
    }
  }

  private List<String> resolveStations(List<String> stations) {
    if (stations == null || stations.isEmpty()) {
      return cfrApiClient.getAllStations().stream()
              .map(StationResponse::getName)
              .collect(Collectors.toList());
    }
    return stations;
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

  private ArrivalsDeparturesCsvRecord toArrivalsDeparturesRecord(
      String stationName, String date, StationTrainResponse dto) {
    TrainMetadataResponse meta = dto.getTrain();
    return ArrivalsDeparturesCsvRecord.builder()
        .currentTimestamp(formatDateTime(dto.getCurrentTimestamp()))
        .cfrDate(date)
        .station(stationName)
        .trainId(meta != null ? meta.getId() : null)
        .trainOperator(meta != null ? meta.getOperator() : null)
        .fromStation(dto.getFromStation())
        .arrival(formatDateTime(dto.getArrival()))
        .arrivalDelayMinutes(dto.getArrivalDelay() != null
            ? dto.getArrivalDelay().toMinutes() : null)
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
