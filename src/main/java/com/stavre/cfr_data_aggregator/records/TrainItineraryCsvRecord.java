package com.stavre.cfr_data_aggregator.records;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Getter;

/** Flat CSV row for one station stop on one branch of a train's itinerary. */
@SuppressWarnings("PMD.DataClass")
@Getter
@Builder
@JsonPropertyOrder({
  "currentTimestamp", "cfr_date", "trainId", "trainOperator",
  "branchName", "branchOriginStation", "branchDestinationStation",
  "station", "journeyKm",
  "arrival", "arrivalDelayMinutes",
  "departure", "departureDelayMinutes",
  "stopDurationMinutes", "platform"
})
public class TrainItineraryCsvRecord {

  private final String currentTimestamp;
  @JsonProperty("cfr_date")
  private final String cfrDate;
  private final String trainId;
  private final String trainOperator;
  private final String branchName;
  private final String branchOriginStation;
  private final String branchDestinationStation;
  private final String station;
  private final Integer journeyKm;
  private final String arrival;
  private final Long arrivalDelayMinutes;
  private final String departure;
  private final Long departureDelayMinutes;
  private final Long stopDurationMinutes;
  private final String platform;
}