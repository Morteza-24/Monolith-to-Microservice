package com.spring.netflix.oss.microservices.service;

import org.springframework.cloud.netflix.feign.FeignClient;

@FeignClient(name = "statement-service")
public interface StatementClient extends StatementService{

}
