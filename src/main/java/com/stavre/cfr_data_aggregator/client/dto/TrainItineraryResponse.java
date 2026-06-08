package com.stavre.cfr_data_aggregator.client.dto;

import java.util.List;
import java.util.Map;
import lombok.Data;

/** Full itinerary for a train, as returned by the CFR API. */
@SuppressWarnings("PMD.DataClass")
@Data
public class TrainItineraryResponse {

  private TrainMetadataResponse metadata;
  private Map<String, List<TrainStopResponse>> stops;
}
