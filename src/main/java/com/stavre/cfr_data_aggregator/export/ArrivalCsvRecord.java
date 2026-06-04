package com.stavre.cfr_data_aggregator.export;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Getter;

/** Flat CSV row for a single train arrival at a station. */
@SuppressWarnings("PMD.DataClass")
@Getter
@Builder
@JsonPropertyOrder({
  "currentTimestamp", "station", "trainId", "trainOperator",
  "fromStation", "arrival", "arrivalDelayMinutes", "platform"
})
public class ArrivalCsvRecord {

  private final String currentTimestamp;
  private final String station;
  private final String trainId;
  private final String trainOperator;
  private final String fromStation;
  private final String arrival;
  private final Long arrivalDelayMinutes;
  private final String platform;
}
