package com.stavre.cfr_data_aggregator.client;

import com.stavre.cfr_data_aggregator.client.dto.StationResponse;
import com.stavre.cfr_data_aggregator.client.dto.StationTrainResponse;
import com.stavre.cfr_data_aggregator.client.dto.TrainItineraryResponse;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Feign client for the cfr-api-adapter service. */
@FeignClient(name = "cfr-api", url = "${cfr-api.base-url:http://localhost:8080}")
public interface CfrApiClient {

  /** Returns the list of all known stations. */
  @GetMapping("/station/all")
  List<StationResponse> getAllStations();

  /** Returns merged arrivals and departures for a station on the given date (dd.MM.yyyy). */
  @GetMapping("/station/{stationName}")
  List<StationTrainResponse> getStation(
      @PathVariable("stationName") String stationName,
      @RequestParam(value = "date", required = false) String date);

  /** Returns the full itinerary for a train on the given date (dd.MM.yyyy). */
  @GetMapping("/train/{trainNumber}")
  TrainItineraryResponse getTrain(
      @PathVariable("trainNumber") String trainNumber,
      @RequestParam(value = "date", required = false) String date);


  /** Returns the list of all known stations. */
  @GetMapping("/train/all")
  List<String> getAllTrains();
}
