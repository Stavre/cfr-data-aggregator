package com.stavre.cfr_data_aggregator.export;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Getter;

/** Flat CSV row for a single train departure at a station. */
@SuppressWarnings("PMD.DataClass")
@Getter
@Builder
@JsonPropertyOrder({
  "currentTimestamp", "station", "trainId", "trainOperator",
  "toStation", "departure", "departureDelayMinutes", "platform"
})
public class DepartureCsvRecord {

  private final String currentTimestamp;
  private final String station;
  private final String trainId;
  private final String trainOperator;
  private final String toStation;
  private final String departure;
  private final Long departureDelayMinutes;
  private final String platform;
}
