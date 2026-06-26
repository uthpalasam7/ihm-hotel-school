package com.ihm.hotelschool;

import com.ihm.hotelschool.common.config.ApplicationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ApplicationProperties.class)
public class HotelSchoolApplication {

	public static void main(String[] args) {
		SpringApplication.run(HotelSchoolApplication.class, args);
	}

}
