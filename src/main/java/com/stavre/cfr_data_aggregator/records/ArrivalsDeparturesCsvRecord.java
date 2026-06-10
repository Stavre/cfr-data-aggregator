package com.stavre.cfr_data_aggregator.records;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Getter;

/** Flat CSV row for a single train's combined arrival and departure at a station. */
@SuppressWarnings("PMD.DataClass")
@Getter
@Builder
@JsonPropertyOrder({
  "currentTimestamp", "cfr_date", "station", "trainId", "trainOperator",
  "fromStation", "arrival", "arrivalDelayMinutes",
  "toStation", "departure", "departureDelayMinutes",
  "platform"
})
public class ArrivalsDeparturesCsvRecord {

  private final String currentTimestamp;
  @JsonProperty("cfr_date")
  private final String cfrDate;
  private final String station;
  private final String trainId;
  private final String trainOperator;
  private final String fromStation;
  private final String arrival;
  private final Long arrivalDelayMinutes;
  private final String toStation;
  private final String departure;
  private final Long departureDelayMinutes;
  private final String platform;
}
