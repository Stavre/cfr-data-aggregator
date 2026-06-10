package com.stavre.cfr_data_aggregator.export;

import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.stavre.cfr_data_aggregator.client.CfrApiClient;
import com.stavre.cfr_data_aggregator.client.dto.TrainItineraryResponse;
import com.stavre.cfr_data_aggregator.client.dto.TrainMetadataResponse;
import com.stavre.cfr_data_aggregator.client.dto.TrainStopResponse;
import com.stavre.cfr_data_aggregator.records.TrainItineraryCsvRecord;
import feign.FeignException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import me.tongfei.progressbar.ProgressBar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Fetches train itinerary data from the CFR API and writes CSV files. */
@Service
public class TrainExportService {

  private static final Logger log = LoggerFactory.getLogger(TrainExportService.class);
  private static final DateTimeFormatter DT_FORMAT =
      DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

  private final CfrApiClient cfrApiClient;
  private final CsvMapper csvMapper = new CsvMapper();

  /** Creates the service with the CFR API client. */
  public TrainExportService(CfrApiClient cfrApiClient) {
    this.cfrApiClient = cfrApiClient;
  }

  /**
   * Fetches itineraries and streams them to outputFile as CSV.
   * If trains is null, all train numbers are discovered by fanning out to every station.
   */
  public void exportItineraries(String date, File outputFile, List<String> trains)
      throws IOException {
    Collection<String> trainNumbers;
    if (trains == null) {
      trainNumbers = cfrApiClient.getAllTrains().stream().toList();
    } else {
      trainNumbers = trains;
    }

    try (SequenceWriter sw = openCsvWriter(TrainItineraryCsvRecord.class, outputFile);
         ProgressBar pb = new ProgressBar("Fetching itineraries", trainNumbers.size())) {
      for (String trainNumber : trainNumbers) {
        pb.setExtraMessage(trainNumber);
        try {
          LocalDateTime stamp = LocalDateTime.now();
          TrainItineraryResponse itinerary = cfrApiClient.getTrain(trainNumber, date);
          for (Map.Entry<String, List<TrainStopResponse>> entry : itinerary.getStops().entrySet()) {
            String[] branch = parseBranchKey(entry.getKey());
            for (TrainStopResponse stop : entry.getValue()) {
              sw.write(toItineraryCsvRecord(date, stamp, itinerary.getMetadata(), branch, stop));
            }
          }
        } catch (FeignException ex) {
          log.warn("Failed to fetch itinerary for train '{}': {}", trainNumber, ex.getMessage());
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

  private TrainItineraryCsvRecord toItineraryCsvRecord(
      String date, LocalDateTime stamp, TrainMetadataResponse meta,
      String[] branch, TrainStopResponse stop) {
    return TrainItineraryCsvRecord.builder()
        .currentTimestamp(formatDateTime(stamp))
        .cfrDate(date)
        .trainId(meta != null ? meta.getId() : null)
        .trainOperator(meta != null ? meta.getOperator() : null)
        .branchName(branch[0])
        .branchOriginStation(branch[1])
        .branchDestinationStation(branch[2])
        .station(stop.getStation())
        .journeyKm(stop.getJourneyKm())
        .arrival(formatDateTime(stop.getArrival()))
        .arrivalDelayMinutes(stop.getArrivalDelay() != null
            ? stop.getArrivalDelay().toMinutes() : null)
        .departure(formatDateTime(stop.getDeparture()))
        .departureDelayMinutes(stop.getDepartureDelay() != null
            ? stop.getDepartureDelay().toMinutes() : null)
        .stopDurationMinutes(stop.getStopDuration() != null
            ? stop.getStopDuration().toMinutes() : null)
        .platform(stop.getPlatform())
        .build();
  }

  private static String[] parseBranchKey(String key) {
    int originIdx = key.indexOf(", originStation=");
    int destIdx = key.indexOf(", destinationStation=");
    String name = key.substring("TrainBranchDto[name=".length(), originIdx);
    String origin = key.substring(originIdx + ", originStation=".length(), destIdx);
    String destination = key.substring(destIdx + ", destinationStation=".length(),
        key.length() - 1);
    return new String[]{name, origin, destination};
  }

  private String formatDateTime(LocalDateTime dateTime) {
    return dateTime != null ? dateTime.format(DT_FORMAT) : null;
  }
}
