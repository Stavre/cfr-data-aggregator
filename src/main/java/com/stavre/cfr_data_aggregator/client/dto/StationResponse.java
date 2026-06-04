package com.stavre.cfr_data_aggregator.client.dto;

import lombok.Data;

/** Station metadata returned by the CFR API. */
@SuppressWarnings("PMD.DataClass")
@Data
public class StationResponse {

  private String name;
}
