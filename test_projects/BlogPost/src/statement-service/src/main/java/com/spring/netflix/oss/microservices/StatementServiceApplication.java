package com.spring.netflix.oss.microservices;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class StatementServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(StatementServiceApplication.class, args);
	}
}
