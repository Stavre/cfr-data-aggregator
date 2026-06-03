package com.stavre.cfr_data_aggregator.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "com.stavre.cfr_data_aggregator")
public class FeignConfig {
}