package com.cloudtrap.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CloudTrapApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(CloudTrapApiApplication.class, args);
	}

}
