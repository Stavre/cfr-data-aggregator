package com.stavre.cfr_data_aggregator.client.dto;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/** One station stop in a train's itinerary, as returned by the CFR API. */
@SuppressWarnings("PMD.DataClass")
@Data
public class TrainStopResponse {

  private LocalDateTime arrival;
  private Duration arrivalDelay;
  private LocalDateTime departure;
  private Duration departureDelay;
  private String station;
  private Integer journeyKm;
  private Duration stopDuration;
  private String platform;
  private List<String> trainStopMessages;
  private List<String> errors;
}
