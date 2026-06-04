package com.stavre.cfr_data_aggregator.client.dto;

import lombok.Data;

/** Train identification fields returned by the CFR API. */
@SuppressWarnings("PMD.DataClass")
@Data
public class TrainMetadataResponse {

  private String id;
  private String number;
  private String category;
  private String operator;
}
