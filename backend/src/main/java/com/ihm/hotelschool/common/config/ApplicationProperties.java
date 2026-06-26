package com.ihm.hotelschool.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record ApplicationProperties(String timezone, String currencyCode) {
}
