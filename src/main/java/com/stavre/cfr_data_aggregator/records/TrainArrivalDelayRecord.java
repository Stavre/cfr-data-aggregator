package com.stavre.cfr_data_aggregator.records;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Getter;

/** Aggregated arrival delay total for one train per run. */
@SuppressWarnings("PMD.DataClass")
@Getter
@Builder
@JsonPropertyOrder({"currentTimestamp", "cfr_date", "trainId", "station", "totalDelayMinutes"})
public class TrainArrivalDelayRecord {

  private final String currentTimestamp;
  @JsonProperty("cfr_date")
  private final String cfrDate;
  private final String trainId;
  private final String station;
  private final Long totalDelayMinutes;
}
