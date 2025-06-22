package com.spring.netflix.oss.microservices.service;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

import java.util.List;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.spring.netflix.oss.microservices.model.StatementVO;

public interface StatementService {
	
final static String PREFIX = "api/";
	
	@RequestMapping(value = PREFIX + "statements", method = GET)
	List<StatementVO> getStatements();
	
	@RequestMapping(value = PREFIX + "statement/{statementId}", method = GET)
	StatementVO getStatament(@PathVariable("statementId") Long statementId);
	
	@RequestMapping(value= PREFIX + "statement", method = GET)
	List<StatementVO> getStatements(@RequestParam("cardId") Long cardId);

}
