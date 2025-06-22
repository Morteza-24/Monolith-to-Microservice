
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


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.spring.netflix.oss.microservices.model.Statement;

@RestController
@RequestMapping(value="/api")
public class StatementServiceController {
	
	private List<Statement> fakeRepo;
	
	@PostConstruct
	public void init(){
		this.fakeRepo = new ArrayList<>();
		fakeRepo.add(new Statement(1l, 2l,"01/11/15 08:41", "US$411.05"));
		fakeRepo.add(new Statement(2l, 1l,"13/04/13 20:16", "US$1,914.00"));
		fakeRepo.add(new Statement(3l, 3l,"31/12/15 18:00", "€12.10"));
		fakeRepo.add(new Statement(4l, 4l,"21/11/10 19:55", "US$1.50"));
		fakeRepo.add(new Statement(5l, 4l,"10/06/14 09:37", "US$116.00"));
		fakeRepo.add(new Statement(6l, 5l,"14/01/12 14:49", "R$11.02"));
		fakeRepo.add(new Statement(7l, 7l,"15/12/20 12:00", "US$14.60"));
		fakeRepo.add(new Statement(9l, 6l,"01/11/09 13:02", "€1,888.93"));
		fakeRepo.add(new Statement(10l, 6l,"01/11/20 08:41", "€293.30"));
		fakeRepo.add(new Statement(11l, 6l,"01/11/20 08:41", "€11.68"));
	}


	@RequestMapping(value="/statements", method = RequestMethod.GET)
	public List<Statement> getStatements() {
		return fakeRepo;
	}
	
	@RequestMapping(value="/statement/{statementId}", method = RequestMethod.GET)
	public Statement getStatament(@PathVariable Long statementId) {
		return Optional.ofNullable(
				fakeRepo
				.stream()
				.filter((statement) -> statement.getId().equals(statementId))
                .reduce(null, (u, v) -> {
                    if (u != null && v != null)
                        throw new IllegalStateException("More than one StatementId found");
                    else return u == null ? v : u;
                })).get();
		
	}
	
	@RequestMapping(value="/statement", method = RequestMethod.GET)
	public List<Statement> getStatements(@RequestParam Long cardId){
		if(cardId!=null){
			return fakeRepo
					.stream()
					.filter((statement) -> statement.getCardId().equals(cardId))
					.collect(Collectors.toList());
		}
		return null;
	}

}


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)
public class Statement {
	private Long id;
	private Long cardId;
	private String operationDate;
	private String value;
	
	public Statement() {
		super();
	}
	
	public Statement(Long id, Long cardId, String operationDate, String value) {
		super();
		this.id = id;
		this.cardId = cardId;
		this.operationDate = operationDate;
		this.value = value;
	}

	public Long getCardId() {
		return cardId;
	}

	public void setCardId(Long cardId) {
		this.cardId = cardId;
	}

	public String getOperationDate() {
		return operationDate;
	}

	public void setOperationDate(String operationDate) {
		this.operationDate = operationDate;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
	
	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return "Statement [id=" + id + ", cardId=" + cardId + ", operationDate=" + operationDate + ", value=" + value
				+ "]";
	}

}


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.feign.EnableFeignClients;

@SpringBootApplication
@EnableEurekaClient
@EnableFeignClients
@EnableCircuitBreaker
public class CardStatementCompositeApplication {

	public static void main(String[] args) {
		SpringApplication.run(CardStatementCompositeApplication.class, args);
	}
}


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


import org.springframework.cloud.netflix.feign.FeignClient;

@FeignClient(name = "card-service")
public interface CardClient extends CardService{

}


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


import org.springframework.cloud.netflix.feign.FeignClient;

@FeignClient(name = "statement-service")
public interface StatementClient extends StatementService{

}


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)
public class CardVO {

	private Long id;
	private String cardHolderName;
	private String pan;
	private String validDate;
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getCardHolderName() {
		return cardHolderName;
	}
	public void setCardHolderName(String cardHolderName) {
		this.cardHolderName = cardHolderName;
	}
	public String getPan() {
		return pan;
	}
	public void setPan(String pan) {
		this.pan = pan;
	}
	public String getValidDate() {
		return validDate;
	}
	public void setValidDate(String validDate) {
		this.validDate = validDate;
	}
	@Override
	public String toString() {
		return "CardVO [id=" + id + ", cardHolderName=" + cardHolderName + ", pan=" + pan + ", validDate=" + validDate
				+ "]";
	}
}


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)
public class StatementVO {
	private Long id;
	private Long cardId;
	private String operationDate;
	private String value;
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Long getCardId() {
		return cardId;
	}
	public void setCardId(Long cardId) {
		this.cardId = cardId;
	}
	public String getOperationDate() {
		return operationDate;
	}
	public void setOperationDate(String operationDate) {
		this.operationDate = operationDate;
	}
	
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	@Override
	public String toString() {
		return "StatementVO [id=" + id + ", cardId=" + cardId + ", operationDate=" + operationDate + ", value=" + value
				+ "]";
	}
	
}


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class CardServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CardServiceApplication.class, args);
	}
}


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


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)
public class Card {
	private Long id;
	private String cardHolderName;
	private String pan;
	private String validDate;
	
	public Card() {
		super();
	}
	
	public Card(Long id, String cardHolderName, String pan, String validDate) {
		super();
		this.id = id;
		this.cardHolderName = cardHolderName;
		this.pan = pan;
		this.validDate = validDate;
	}



	public Card(String cardHolderName, String pan, String validDate) {
		super();
		this.cardHolderName = cardHolderName;
		this.pan = pan;
		this.validDate = validDate;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getCardHolderName() {
		return cardHolderName;
	}

	public void setCardHolderName(String cardHolderName) {
		this.cardHolderName = cardHolderName;
	}

	public String getPan() {
		return pan;
	}

	public void setPan(String pan) {
		this.pan = pan;
	}

	public String getValidDate() {
		return validDate;
	}

	public void setValidDate(String validDate) {
		this.validDate = validDate;
	}

	@Override
	public String toString() {
		return "Card [id=" + id + ", cardHolderName=" + cardHolderName + ", pan=" + pan + ", validDate=" + validDate
				+ "]";
	}

}


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(DiscoveryServiceApplication.class, args);
	}
}

