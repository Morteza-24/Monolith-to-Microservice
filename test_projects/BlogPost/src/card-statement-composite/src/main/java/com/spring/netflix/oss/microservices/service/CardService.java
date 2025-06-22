package com.spring.netflix.oss.microservices.service;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.spring.netflix.oss.microservices.model.CardVO;

public interface CardService {
	final static String PREFIX = "api/";
	
	@RequestMapping(value = PREFIX + "cards", method = GET)
	List<CardVO> getCards();
	
	@RequestMapping(value = PREFIX + "card/{cardId}", method = GET)
	CardVO getCard(@PathVariable("cardId") Long cardId);
	
	@RequestMapping(value= PREFIX + "new-card", method = POST, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE) //it could be PUT
	void createCard(@RequestBody CardVO card);
	
}
