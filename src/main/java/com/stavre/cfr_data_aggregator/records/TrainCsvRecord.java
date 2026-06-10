package com.stavre.cfr_data_aggregator.records;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Getter;

/** One row in a trains CSV file. */
@SuppressWarnings("PMD.DataClass")
@Getter
@Builder
@JsonPropertyOrder({"number"})
public class TrainCsvRecord {

  private final String number;
}
