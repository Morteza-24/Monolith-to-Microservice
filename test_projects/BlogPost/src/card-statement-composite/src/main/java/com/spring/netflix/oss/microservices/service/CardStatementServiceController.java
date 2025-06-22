package com.spring.netflix.oss.microservices.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.spring.netflix.oss.microservices.model.CardVO;
import com.spring.netflix.oss.microservices.model.StatementVO;

@RestController
@RequestMapping("/api")
public class CardStatementServiceController {

	@Autowired
	CardClient cardClient;
	
	@Autowired
	StatementClient statementClient;
	
	@HystrixCommand(fallbackMethod = "defaultCardStatementError")
	@RequestMapping(value="/statement-by-card", method=RequestMethod.GET)
	public ResponseEntity<Map<CardVO, List<StatementVO>>> 
	getStatementByCardId(@RequestParam Long cardId){
		Map<CardVO, List<StatementVO>> response = new HashMap<>();
		
		response.put(cardClient.getCard(cardId), statementClient.getStatements(cardId));
		
		return new ResponseEntity<Map<CardVO,List<StatementVO>>>(response, HttpStatus.OK);
	}
	
	public ResponseEntity<Map<CardVO, List<StatementVO>>> 
	defaultCardStatementError(Long cardId){
		Map<CardVO, List<StatementVO>> response = new HashMap<>();
		return new ResponseEntity<Map<CardVO,List<StatementVO>>>(response, HttpStatus.OK);
		
	}
}
