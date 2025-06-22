package com.spring.netflix.oss.microservices.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.spring.netflix.oss.microservices.model.Card;

@RestController
@RequestMapping(value="/api")
public class CardServiceController {
	
	private List<Card> fakeRepo;
	
	@PostConstruct
	public void init(){
		this.fakeRepo = new ArrayList<>();
		fakeRepo.add(new Card(1l, "John Warner", String.valueOf(Math.random()).substring(0, 16),"11/20"));
		fakeRepo.add(new Card(2l, "Paul Crarte", String.valueOf(Math.random()).substring(0, 16),"09/25"));
		fakeRepo.add(new Card(3l, "Ana Hassent", String.valueOf(Math.random()).substring(0, 16),"01/19"));
		fakeRepo.add(new Card(4l, "Elena Tarin", String.valueOf(Math.random()).substring(0, 16),"06/22"));
		fakeRepo.add(new Card(5l, "Wending Qua", String.valueOf(Math.random()).substring(0, 16),"03/25"));
		fakeRepo.add(new Card(6l, "Julio Sanch", String.valueOf(Math.random()).substring(0, 16),"09/18"));
		fakeRepo.add(new Card(7l, "Adolf Bianc", String.valueOf(Math.random()).substring(0, 16),"07/22"));
		
	}
	
	@RequestMapping(value="/cards", method = RequestMethod.GET)
	public List<Card> getCards() {
		return fakeRepo;
	}
	
	@RequestMapping(value="/card/{cardId}", method = RequestMethod.GET)
	public Card getCard(@PathVariable Long cardId) {
		return Optional.ofNullable(
				fakeRepo
				.stream()
				.filter((card) -> card.getId().equals(cardId))
                .reduce(null, (u, v) -> {
                    if (u != null && v != null)
                        throw new IllegalStateException("More than one CardId found");
                    else return u == null ? v : u;
                })).get();
		
	}

	@RequestMapping(value = "/new-card", method = RequestMethod.POST, headers = "Accept=application/json")
	public void createCard(@RequestBody Card newCard) {
		if(newCard.getId()!=null){
			fakeRepo.add(newCard);
		}
		System.out.println("New card passing: " + newCard);
	}
}
