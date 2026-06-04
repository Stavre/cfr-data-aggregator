package com.stavre.cfr_data_aggregator.client;

import com.stavre.cfr_data_aggregator.client.dto.StationResponse;
import com.stavre.cfr_data_aggregator.client.dto.StationTrainResponse;
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

  /** Returns all arrivals for a station on the given date (dd.MM.yyyy). */
  @GetMapping("/station/arrivals/{stationName}")
  List<StationTrainResponse> getStationArrivals(
      @PathVariable("stationName") String stationName,
      @RequestParam(value = "date", required = false) String date);

  /** Returns all departures for a station on the given date (dd.MM.yyyy). */
  @GetMapping("/station/departures/{stationName}")
  List<StationTrainResponse> getStationDepartures(
      @PathVariable("stationName") String stationName,
      @RequestParam(value = "date", required = false) String date);
}
