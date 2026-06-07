package com.stavre.cfr_data_aggregator.export;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Getter;

/** One row in a stations CSV file. */
@SuppressWarnings("PMD.DataClass")
@Getter
@Builder
@JsonPropertyOrder({"name", "isImportant"})
public class StationCsvRecord {

  private final String name;
  private final Boolean isImportant;
}
