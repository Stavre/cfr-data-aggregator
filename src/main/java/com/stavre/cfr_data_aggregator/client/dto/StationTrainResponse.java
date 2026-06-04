package com.stavre.cfr_data_aggregator.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/** Arrival or departure entry for a train at a station, as returned by the CFR API. */
@SuppressWarnings("PMD.DataClass")
@Data
public class StationTrainResponse {

  private String fromStation;
  private LocalDateTime arrival;
  private Duration arrivalDelay;
  private String toStation;
  private LocalDateTime departure;
  private Duration departureDelay;
  private TrainMetadataResponse train;
  private String platform;
  private List<String> direction;
  private List<String> errors;

  @JsonIgnore
  private LocalDateTime currentTimestamp;
}
