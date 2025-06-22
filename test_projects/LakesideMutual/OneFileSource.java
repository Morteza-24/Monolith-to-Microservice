
import org.microserviceapipatterns.domaindrivendesign.BoundedContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CustomerCoreApplication is the execution entry point of the Customer Core which
 * is one of the functional/system/application Bounded Contexts of Lakeside Mutual.
 */
@SpringBootApplication
public class CustomerCoreApplication implements BoundedContext {
	private static Logger logger = LoggerFactory.getLogger(CustomerCoreApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(CustomerCoreApplication.class, args);
		logger.info("--- Customer Core started ---");
	}
}


import org.springframework.data.jpa.repository.JpaRepository;

import com.lakesidemutual.customercore.domain.customer.CustomerAggregateRoot;
import com.lakesidemutual.customercore.domain.customer.CustomerId;
import org.microserviceapipatterns.domaindrivendesign.Repository;

/**
 * The CustomerRepository can be used to read and write CustomerAggregateRoot objects from and to the backing database. Spring automatically
 * searches for interfaces that extend the JpaRepository interface and creates a corresponding Spring bean for each of them. For more information
 * on repositories visit the <a href="https://docs.spring.io/spring-data/jpa/docs/current/reference/html/">Spring Data JPA - Reference Documentation</a>.
 * */
public interface CustomerRepository extends JpaRepository<CustomerAggregateRoot, CustomerId>, Repository {

	default CustomerId nextId() {
		return CustomerId.random();
	}
}


import java.util.List;

/**
 * An instance of the Page class can be used to represent a subset of a specific type of resource.
 * */
public class Page<T> {
	private final List<T> elements;
	private final int offset;
	private final int limit;
	private final int size;

	public Page(List<T> elements, int offset, int limit, int size) {
		this.elements = elements;
		this.offset = offset;
		this.limit = limit;
		this.size = size;
	}

	public List<T> getElements() {
		return elements;
	}

	public int getOffset() {
		return offset;
	}

	public int getLimit() {
		return limit;
	}

	public int getSize() {
		return size;
	}
}


import com.lakesidemutual.customercore.domain.customer.*;
import com.lakesidemutual.customercore.infrastructure.CustomerRepository;
import jakarta.persistence.EntityManager;
import org.microserviceapipatterns.domaindrivendesign.ApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * The CustomerService class is an application service that is
 * responsible for creating, updating and retrieving customer entities.
 */
@Component
public class CustomerService implements ApplicationService {
	@Autowired
	private CustomerRepository customerRepository;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private CustomerFactory customerFactory;

	public Optional<CustomerAggregateRoot> updateAddress(CustomerId customerId, Address updatedAddress) {
		Optional<CustomerAggregateRoot> optCustomer = customerRepository.findById(customerId);
		if (optCustomer.isEmpty()) {
			return optCustomer;
		}

		CustomerAggregateRoot customer = optCustomer.get();
		customer.moveToAddress(updatedAddress);
		customerRepository.save(customer);
		return optCustomer;
	}

	public Optional<CustomerAggregateRoot> updateCustomerProfile(CustomerId customerId, CustomerProfileEntity updatedCustomerProfile) {
		Optional<CustomerAggregateRoot> optCustomer = customerRepository.findById(customerId);
		if (optCustomer.isEmpty()) {
			return optCustomer;
		}

		CustomerAggregateRoot customer = optCustomer.get();
		customer.updateCustomerProfile(updatedCustomerProfile);
		customerRepository.save(customer);
		return optCustomer;
	}

	public CustomerAggregateRoot createCustomer(CustomerProfileEntity customerProfile) {
		CustomerAggregateRoot customer = customerFactory.create(customerProfile);
		customerRepository.save(customer);
		return customer;
	}

	public List<CustomerAggregateRoot> getCustomers(String ids) {
		List<CustomerId> customerIds = Arrays.stream(ids.split(",")).map(id -> new CustomerId(id.trim())).toList();

		List<CustomerAggregateRoot> customers = new ArrayList<>();
		for (CustomerId customerId : customerIds) {
			Optional<CustomerAggregateRoot> customer = customerRepository.findById(customerId);
			customer.ifPresent(customers::add);
		}
		return customers;
	}

	public Page<CustomerAggregateRoot> getCustomers(String filter, int limit, int offset) {

		// See https://vladmihalcea.com/fix-hibernate-hhh000104-entity-fetch-pagination-warning-message/
		// for details on the following implementation:

		String filterParameter = "%" + filter + "%";

		long totalSize = entityManager.createQuery(
						"select count(1) from CustomerAggregateRoot c " +
								"left join c.customerProfile " +
								"where c.customerProfile.firstname like :filter or c.customerProfile.lastname like :filter", Long.class)
				.setParameter("filter", filterParameter)
				.getSingleResult();

		List<CustomerId> customerIds = entityManager.createQuery(
						"select c.id from CustomerAggregateRoot c " +
								"left join c.customerProfile " +
								"where c.customerProfile.firstname like :filter or c.customerProfile.lastname like :filter " +
								"order by c.customerProfile.firstname, c.customerProfile.lastname", CustomerId.class)
				.setParameter("filter", filterParameter)
				.setFirstResult(offset)
				.setMaxResults(limit)
				.getResultList();

		List<CustomerAggregateRoot> customerAggregateRoots = entityManager.createQuery(
						"select c from CustomerAggregateRoot c " +
								"left join fetch c.customerProfile " +
								"left join fetch c.customerProfile.moveHistory " +
								"where c.id in (:customerIds) " +
								"order by c.customerProfile.firstname, c.customerProfile.lastname", CustomerAggregateRoot.class)
				.setParameter("customerIds", customerIds)
				.getResultList();

		return new Page<>(customerAggregateRoots, offset, limit, (int) totalSize);
	}
}


import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.lakesidemutual.customercore.domain.customer.Address;
import com.lakesidemutual.customercore.domain.customer.CustomerAggregateRoot;
import com.lakesidemutual.customercore.domain.customer.CustomerId;
import com.lakesidemutual.customercore.domain.customer.CustomerProfileEntity;
import com.lakesidemutual.customercore.infrastructure.CustomerRepository;

/**
 * The run() method of the DataLoader class is automatically executed when the application launches.
 * It populates the database with sample customers that can be used to test the application.
 * */
@Component
@Profile("!test")
public class DataLoader implements ApplicationRunner {
	@Autowired
	private CustomerRepository customerRepository;

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	private List<CustomerAggregateRoot> createCustomersFromDummyData(List<Map<String, String>> customerData) {
		SimpleDateFormat birthdayFormat = new SimpleDateFormat("MM/dd/yyyy");
		List<CustomerAggregateRoot> customers = customerData.stream().map(data -> {
			final CustomerId customerId = new CustomerId(data.get("id"));
			final String firstName = data.get("first_name");
			final String lastName = data.get("last_name");
			Date birthday = null;
			try {
				birthday = birthdayFormat.parse(data.get("birthday"));
			} catch (ParseException e) {
			}
			final Address currentAddress = new Address(data.get("street_address"), data.get("postal_code"), data.get("city"));
			final String email = data.get("email");
			final String phoneNumber = data.get("phone_number");
			final CustomerProfileEntity customerProfile = new CustomerProfileEntity(firstName, lastName, birthday, currentAddress, email, phoneNumber);
			return new CustomerAggregateRoot(customerId, customerProfile);
		}).collect(Collectors.toList());
		return customers;
	}


	/**
	 * Note: The files mock_customers_small.csv and mock_customers_large.csv are the same as the files
	 * mock_users_small.csv and mock_users_large.csv in the Customer Self-Service backend. The Customer
	 * Core DataLoader creates a customer for each row in the corresponding CSV file and the
	 * Customer Self-Service DataLoader creates a user login for each of these customers. Therefore,
	 * these files should be kept in sync.
	 *
	 * This dummy data was generated using https://mockaroo.com/
	 */
	private List<Map<String, String>> loadCustomers() {
		try(InputStream file = new ClassPathResource("mock_customers_small.csv").getInputStream()) {
			CsvMapper mapper = new CsvMapper();
			CsvSchema schema = CsvSchema.emptySchema().withHeader();
			MappingIterator<Map<String, String>> readValues = mapper.readerFor(Map.class).with(schema).readValues(file);
			return readValues.readAll();
		} catch (Exception e) {
			return Collections.emptyList();
		}
	}

	@Override
	public void run(ApplicationArguments args) throws ParseException {
		if(customerRepository.count() > 0) {
			logger.info("Skipping import of application dummy data, because the database already contains existing entities.");
			return;
		}

		List<Map<String, String>> customerData = loadCustomers();

		logger.info("Loaded " + customerData.size() + " customers.");

		List<CustomerAggregateRoot> customers = createCustomersFromDummyData(customerData);

		logger.info("Created " + customerData.size() + " customers.");

		customerRepository.saveAll(customers);

		logger.info("Inserted " + customerData.size() + " customers into database.");

		logger.info("DataLoader has successfully imported all application dummy data, the application is now ready.");
	}
}

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * This controller demonstrates the use of a 301 Moved Permanently response to redirect requests to a new URI.
 *
 * See the <a href="https://interface-refactoring.github.io/refactorings/renameendpoint">Rename Endpoint</a> refactoring for more information.
 */
@RestController
@RequestMapping(path = "getCustomers")
public class OldCustomerInformationHolder {

    /**
     * Handles GET requests for customer information.
     *
     * @param ids    The customer IDs.
     * @param fields The fields to be included in the response (optional).
     * @return A ResponseEntity with a status of 301 Moved Permanently and the location header set to the new URI.
     */
    @GetMapping(value = "/{ids}")
    @ResponseBody
    public ResponseEntity getCustomer(
            @PathVariable String ids,
            @RequestParam(value = "fields", required = false, defaultValue = "") String fields) {
        URI movedTo = linkTo(methodOn(CustomerInformationHolder.class).getCustomer(ids, fields)).toUri();
        return ResponseEntity
                .status(HttpStatus.MOVED_PERMANENTLY)
                .location(movedTo)
                .build();
    }
}

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lakesidemutual.customercore.domain.city.CityLookupService;
import com.lakesidemutual.customercore.interfaces.dtos.city.CitiesResponseDto;

/**
 * This REST controller allows clients to retrieve a list of cities that match a given postal code. It is an application of
 * the <a href="https://www.microservice-api-patterns.org/patterns/responsibility/informationHolderEndpointTypes/ReferenceDataHolder">Reference Data Holder</a> pattern.
 * A Reference Data Holder is a dedicated endpoint that serves as single point of reference for static data (i.e., data that almost never changes).
 *
 * @see <a href=
 *      "https://www.microservice-api-patterns.org/patterns/responsibility/informationHolderEndpointTypes/ReferenceDataHolder">https://www.microservice-api-patterns.org/patterns/responsibility/informationHolderEndpointTypes/ReferenceDataHolder</a>
 */
@RestController
@RequestMapping("/cities")
public class CityReferenceDataHolder {
	@Autowired
	private CityLookupService cityLookupService;

	@Operation(summary = "Get the cities for a particular postal code.")
	@GetMapping(value = "/{postalCode}")
	public ResponseEntity<CitiesResponseDto> getCitiesForPostalCode(
			@Parameter(description = "the postal code", required = true) @PathVariable String postalCode) {

		List<String> cities = cityLookupService.getCitiesForPostalCode(postalCode);
		CitiesResponseDto response = new CitiesResponseDto(cities);
		return ResponseEntity.ok(response);
	}
}


import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.web.servlet.error.AbstractErrorController;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

/**
 * This class implements a custom error controller that returns an <a href=
 * "https://www.microservice-api-patterns.org/patterns/quality/qualityManagementAndGovernance/ErrorReport">Error
 * Report</a>.
 */
@Controller
public class ErrorController extends AbstractErrorController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	public ErrorController(ErrorAttributes errorAttributes) {
		super(errorAttributes);
	}

	@RequestMapping(value = "/error", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Map<String, Object> handleError(HttpServletRequest request) {
		Map<String, Object> errorAttributes = super.getErrorAttributes(request, ErrorAttributeOptions.defaults());

		Object path = errorAttributes.get("path");
		Object status = errorAttributes.get("status");
		Object error = errorAttributes.get("error");
		Object message = errorAttributes.get("message");

		logger.info("An error occurred while accessing {}: {} {}, {}", path, status, error, message);

		return errorAttributes;
	}
}

import com.lakesidemutual.customercore.application.CustomerService;
import com.lakesidemutual.customercore.application.Page;
import com.lakesidemutual.customercore.domain.customer.Address;
import com.lakesidemutual.customercore.domain.customer.CustomerAggregateRoot;
import com.lakesidemutual.customercore.domain.customer.CustomerId;
import com.lakesidemutual.customercore.domain.customer.CustomerProfileEntity;
import com.lakesidemutual.customercore.interfaces.dtos.customer.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriUtils;

import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * This REST controller gives clients access to the customer data. It is an example of the
 * <i>Information Holder Resource</i> pattern. This particular one is a special type of information holder called <i>Master Data Holder</i>.
 *
 * @see <a href="https://www.microservice-api-patterns.org/patterns/responsibility/endpointRoles/InformationHolderResource">Information Holder Resource</a>
 * @see <a href="https://www.microservice-api-patterns.org/patterns/responsibility/informationHolderEndpointTypes/MasterDataHolder">Master Data Holder</a>
 *
 * Note: JAX-WS and JAX-RS (or Spring Web Services and REST annotations) support could be added to the same controller/port class (in port-and-adapter or hexagon style terms, by A. Cockburn). 
 * But such hybrid approach gets messy soon (due to "annotation jungle"). Hence, there is a separate CustomerInformationHolderSOAPAdapter.
 *
 */

@RestController
@RequestMapping("/customers")
public class CustomerInformationHolder {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	/**
	 * Attributes marked with @Autowired are automatically created and injected by
	 * Spring.
	 */
	@Autowired
	private CustomerService customerService;
	
	private Set<String> getIncludedFields(String fields) {
		if (fields.trim().isEmpty()) {
			return Collections.emptySet();
		} else {
			return new HashSet<>(Arrays.asList(fields.split(",")));
		}
	}

	private CustomerResponseDto createCustomerResponseDto(CustomerAggregateRoot customer, String fields) {
		final Set<String> includedFields = getIncludedFields(fields);
		CustomerResponseDto customerResponseDto = new CustomerResponseDto(includedFields, customer);
		Link selfLink = linkTo(
				methodOn(CustomerInformationHolder.class).getCustomer(customer.getId().toString(), fields))
				.withSelfRel();

		Link updateAddressLink = linkTo(methodOn(CustomerInformationHolder.class).changeAddress(customer.getId(), null))
				.withRel("address.change");

		customerResponseDto.add(selfLink);
		customerResponseDto.add(updateAddressLink);
		return customerResponseDto;
	}

	private PaginatedCustomerResponseDto createPaginatedCustomerResponseDto(String filter, Integer limit,
			Integer offset, int size, String fields, List<CustomerResponseDto> customerDtos) {

		PaginatedCustomerResponseDto paginatedCustomerResponseDto = new PaginatedCustomerResponseDto(filter, limit,
				offset, size, customerDtos);

		paginatedCustomerResponseDto
		.add(linkTo(methodOn(CustomerInformationHolder.class).getCustomers(filter, limit, offset, fields))
				.withSelfRel());

		if (offset > 0) {
			paginatedCustomerResponseDto.add(linkTo(methodOn(CustomerInformationHolder.class).getCustomers(filter,
					limit, Math.max(0, offset - limit), fields)).withRel("prev"));
		}

		if (offset < size - limit) {
			paginatedCustomerResponseDto.add(linkTo(
					methodOn(CustomerInformationHolder.class).getCustomers(filter, limit, offset + limit, fields))
					.withRel("next"));
		}

		return paginatedCustomerResponseDto;
	}
	

	/**
	 * Returns a 'page' of customers. <br>
	 * <br>
	 * The query parameters {@code limit} and {@code offset} can be used to specify
	 * the maximum size of the page and the offset of the page's first customer.
	 * Example Usage:
	 *
	 * <pre>
	 * <code>
	 * GET http://localhost:8110/customers?limit=2&offset=2
	 *
	 * {
	 *   "limit" : 2,
	 *   "offset" : 2,
	 *   "size" : 50,
	 *   "customers" : [ {
	 *     "customerId" : "ctkdzorjl0",
	 *     "firstname" : "Rahel",
	 *     "lastname" : "Piletic",
	 *     ...
	 *  }, {
	 *     "customerId" : "5xvivyzxvc",
	 *     "firstname" : "Cullin",
	 *     "lastname" : "Manske",
	 *     ...
	 *  } ],
	 *   "_links" : {
	 *     "self" : {
	 *       "href" : "http://localhost:8110/customers?limit=2&offset=2&fields="
	 *     },
	 *     "prev" : {
	 *       "href" : "http://localhost:8110/customers?limit=2&offset=0&fields="
	 *     },
	 *     "next" : {
	 *       "href" : "http://localhost:8110/customers?limit=2&offset=4&fields="
	 *     }
	 *   }
	 * }
	 * </code>
	 * </pre>
	 *
	 * The response contains the customers, limit and offset of the current page as
	 * well as the total number of customers (size). Additionally, it contains
	 * HATEOAS-style links that link to the endpoint address of the current,
	 * previous and next page.
	 *
	 * @see <a href=
	 *      "https://www.microservice-api-patterns.org/patterns/structure/compositeRepresentations/Pagination">https://www.microservice-api-patterns.org/patterns/structure/compositeRepresentations/Pagination</a>
	 */
	@Operation(summary = "Get all customers in pages of 10 entries per page.")
	@GetMapping // MAP operation responsibility: Retrieval Operation
	public ResponseEntity<PaginatedCustomerResponseDto> getCustomers(
			@Parameter(description = "search terms to filter the customers by name", required = false) @RequestParam(value = "filter", required = false, defaultValue = "") String filter,
			@Parameter(description = "the maximum number of customers per page", required = false) @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit,
			@Parameter(description = "the offset of the page's first customer", required = false) @RequestParam(value = "offset", required = false, defaultValue = "0") Integer offset,
			@Parameter(description = "a comma-separated list of the fields that should be included in the response", required = false) @RequestParam(value = "fields", required = false, defaultValue = "") String fields) {

		final String decodedFilter = UriUtils.decode(filter, "UTF-8");
		final Page<CustomerAggregateRoot> customerPage = customerService.getCustomers(decodedFilter, limit, offset);
		List<CustomerResponseDto> customerDtos = customerPage.getElements().stream().map(c -> createCustomerResponseDto(c, fields)).collect(Collectors.toList());

		PaginatedCustomerResponseDto paginatedCustomerResponseDto = createPaginatedCustomerResponseDto(
				filter,
				customerPage.getLimit(),
				customerPage.getOffset(),
				customerPage.getSize(),
				fields,
				customerDtos);
		return ResponseEntity.ok(paginatedCustomerResponseDto);
	}

	/**
	 * Returns the customers for the given customer ids. <br>
	 * <br>
	 * The client can provide a comma-separated list of customer ids to fetch a
	 * particular set of customers. This is a variant of the <a href=
	 * "https://www.microservice-api-patterns.org/patterns/quality/dataTransferParsimony/RequestBundle">Request
	 * Bundle</a> pattern:
	 *
	 * <pre>
	 *   <code>
	 * GET http://localhost:8110/customers/ce4btlyluu,rgpp0wkpec
	 *
	 * {
	 *   "customers" : [
	 *     {
	 *       "customerId" : "ce4btlyluu",
	 *       "firstname" : "Robbie",
	 *       "lastname" : "Davenhall",
	 *       "birthday" : "1961-08-11T23:00:00.000+0000",
	 *       ...
	 *       "_links" : { ... }
	 *     },
	 *     {
	 *       "customerId" : "rgpp0wkpec",
	 *       "firstname" : "Max",
	 *       "lastname" : "Mustermann",
	 *       "birthday" : "1989-12-31T23:00:00.000+0000",
	 *       ...
	 *       "_links" : { ... }
	 *     }
	 *   ],
	 *   "_links" : {
	 *     "self" : {
	 *       "href" : "http://localhost:8110/customers/ce4btlyluu,rgpp0wkpec?fields="
	 *     }
	 *   }
	 * }
	 *   </code>
	 * </pre>
	 *
	 * By default getCustomer() returns a response that includes all fields. The
	 * client can also reduce the size of the response by providing a so-called
	 * <a href=
	 * "https://www.microservice-api-patterns.org/patterns/quality/dataTransferParsimony/WishList">Wish
	 * List</a> with the query parameter {@code fields}:
	 *
	 * <pre>
	 *   <code>
	 * GET http://localhost:8110/customers/ce4btlyluu,rgpp0wkpec?fields=firstname,lastname
	 *
	 * {
	 *   "customers" : [
	 *     {
	 *       "firstname" : "Robbie",
	 *       "lastname" : "Davenhall",
	 *       "_links" : { ... }
	 *     },
	 *     {
	 *       "firstname" : "Max",
	 *       "lastname" : "Mustermann",
	 *       "_links" : { ... }
	 *     }
	 *   ],
	 *   "_links" : {
	 *     "self" : {
	 *       "href" : "http://localhost:8110/customers/ce4btlyluu,rgpp0wkpec?fields="
	 *     }
	 *   }
	 * }
	 *   </code>
	 * </pre>
	 *
	 * @see <a href=
	 *      "https://www.microservice-api-patterns.org/patterns/quality/dataTransferParsimony/RequestBundle">https://www.microservice-api-patterns.org/patterns/quality/dataTransferParsimony/RequestBundle</a>
	 * @see <a href=
	 *      "https://www.microservice-api-patterns.org/patterns/quality/dataTransferParsimony/WishList">https://www.microservice-api-patterns.org/patterns/quality/dataTransferParsimony/WishList</a>
	 */
	@Operation(summary = "Get a specific set of customers.")
	@GetMapping(value = "/{ids}") // MAP operation responsibility: Retrieval Operation
	public ResponseEntity<CustomersResponseDto> getCustomer(
			@Parameter(description = "a comma-separated list of customer ids", required = true) @PathVariable String ids,
			@Parameter(description = "a comma-separated list of the fields that should be included in the response", required = false) @RequestParam(value = "fields", required = false, defaultValue = "") String fields) {

		List<CustomerAggregateRoot> customers = customerService.getCustomers(ids);
		List<CustomerResponseDto> customerResponseDtos = customers.stream()
				.map(customer -> createCustomerResponseDto(customer, fields)).collect(Collectors.toList());
		CustomersResponseDto customersResponseDto = new CustomersResponseDto(customerResponseDtos);
		Link selfLink = linkTo(methodOn(CustomerInformationHolder.class).getCustomer(ids, fields)).withSelfRel();
		customersResponseDto.add(selfLink);
		return ResponseEntity.ok(customersResponseDto);
	}

	@Operation(summary = "Update the profile of the customer with the given customer id")
	@PutMapping(value = "/{customerId}") // MAP operation responsibility: State Transition Operation (Full Replace)
	public ResponseEntity<CustomerResponseDto> updateCustomer(
			@Parameter(description = "the customer's unique id", required = true) @PathVariable CustomerId customerId,
			@Parameter(description = "the customer's updated profile", required = true) @Valid @RequestBody CustomerProfileUpdateRequestDto requestDto) {
		final CustomerProfileEntity updatedCustomerProfile = requestDto.toDomainObject();

		Optional<CustomerAggregateRoot> optCustomer = customerService.updateCustomerProfile(customerId, updatedCustomerProfile);
		if(!optCustomer.isPresent()) {
			final String errorMessage = "Failed to find a customer with id '" + customerId.toString() + "'.";
			logger.info(errorMessage);
			throw new CustomerNotFoundException(errorMessage);
		}

		CustomerAggregateRoot customer = optCustomer.get();
		CustomerResponseDto response = new CustomerResponseDto(Collections.emptySet(), customer);
		return ResponseEntity.ok(response);
	}
	
	@Operation(summary = "Change a customer's address.")
	@PutMapping(value = "/{customerId}/address") // MAP operation responsibility: State Transition Operation (Partial Update)
	public ResponseEntity<AddressDto> changeAddress(
			@Parameter(description = "the customer's unique id", required = true) @PathVariable CustomerId customerId,
			@Parameter(description = "the customer's new address", required = true) @Valid @RequestBody AddressDto requestDto) {

		Address updatedAddress = requestDto.toDomainObject();
		Optional<CustomerAggregateRoot> optCustomer = customerService.updateAddress(customerId, updatedAddress);
		if (!optCustomer.isPresent()) {
			final String errorMessage = "Failed to find a customer with id '" + customerId.toString() + "'.";
			logger.info(errorMessage);
			throw new CustomerNotFoundException(errorMessage);
		}

		AddressDto responseDto = AddressDto.fromDomainObject(updatedAddress);
		return ResponseEntity.ok(responseDto);
	}

	@Operation(summary = "Create a new customer.")
	@PostMapping // MAP operation responsibility: State Creation Operation
	public ResponseEntity<CustomerResponseDto> createCustomer(
			@Parameter(description = "the customer's profile information", required = true) @Valid @RequestBody CustomerProfileUpdateRequestDto requestDto) {

		CustomerProfileEntity customerProfile = requestDto.toDomainObject();
		CustomerAggregateRoot customer = customerService.createCustomer(customerProfile);
		return ResponseEntity.ok(createCustomerResponseDto(customer, ""));
	}
}


import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The SwaggerConfiguration class configures the HTTP resource API documentation.
 */
@Configuration
public class SwaggerConfiguration {

	@Bean
	public OpenAPI customerSelfServiceApi() {
		return new OpenAPI()
				.info(new Info().title("Customer Core API")
						.description("This API allows clients to create new customers and retrieve details about existing customers.")
						.version("v1.0.0")
						.license(new License().name("Apache 2.0")));
	}
}


import jakarta.servlet.Filter;
import org.h2.server.web.JakartaWebServlet;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * The WebConfiguration class is used to customize the default Spring MVC configuration.
 * */
@Configuration
public class WebConfiguration implements WebMvcConfigurer {
	/**
	 * This web servlet makes the web console of the H2 database engine available at the "/console" endpoint.
	 * */
	@Bean
	public ServletRegistrationBean<JakartaWebServlet> h2servletRegistration() {
		ServletRegistrationBean<JakartaWebServlet> registrationBean = new ServletRegistrationBean<>(new JakartaWebServlet());
		registrationBean.addUrlMappings("/console/*");
		return registrationBean;
	}

	/**
	 * This is a filter that generates an ETag value based on the content of the response. This ETag is compared to the If-None-Match header
	 * of the request. If these headers are equal, the response content is not sent, but rather a 304 "Not Modified" status instead.
	 * */
	@Bean
	public Filter shallowETagHeaderFilter() {
		return new ShallowEtagHeaderFilter();
	}
}

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

/**
 * This class extracts the API Key from the request's HTTP header.
 * */
public class APIKeyAuthFilter extends AbstractPreAuthenticatedProcessingFilter {
	private String principalRequestHeader;

	public APIKeyAuthFilter(String principalRequestHeader) {
		this.principalRequestHeader = principalRequestHeader;
	}

	@Override
	protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
		return request.getHeader(principalRequestHeader);
	}

	@Override
	protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
		return "N/A";
	}
}


import java.util.List;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * The APIKeyAuthenticationManager ensures that only known clients can access the Customer Core API. It is an example of the <a href=
 * "https://www.microservice-api-patterns.org/patterns/quality/qualityManagementAndGovernance/APIKey">API Key</a> pattern
 * where each client identifies itself with an API Key. Example:
 * <br/>
 *
 * <br/>
 * <b>Missing API Key</b>
 * <pre>
 *   <code>
 * $ curl localhost:8110/customers
 * {
 *  "timestamp" : "2018-11-12T15:39:14.959+0000",
 *  "status" : 403,
 *  "error" : "Forbidden",
 *  "message" : "Access Denied",
 *  "path" : "/customers"
 * }
 *   </code>
 * </pre>
 *
 * <b>Valid API Key</b>
 * <pre>
 *   <code>
 * $ curl -H "Authorization: Bearer b318ad736c6c844b" localhost:8110/customers
 * {
 *  "filter" : "",
 *  "limit" : 10,
 *  "offset" : 0,
 *  "size" : 50,
 *  "customers" : [
 *  	...
 *  ]
 * }
 *   </code>
 * </pre>
 *
 * @see <a href=
 *      "https://www.microservice-api-patterns.org/patterns/quality/qualityManagementAndGovernance/APIKey">https://www.microservice-api-patterns.org/patterns/quality/qualityManagementAndGovernance/APIKey</a>
 */
public class APIKeyAuthenticationManager implements AuthenticationManager {
	private final List<String> validAPIKeys;

	public APIKeyAuthenticationManager(List<String> validAPIKeys) {
		this.validAPIKeys = validAPIKeys;
	}

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		final String bearerPrefix = "Bearer ";
		final String principal = (String)authentication.getPrincipal();
		if(!principal.startsWith(bearerPrefix)) {
			throw new BadCredentialsException("Invalid API Key");
		}

		final String apiKey = principal.substring(bearerPrefix.length());
		if(!validAPIKeys.contains(apiKey)) {
			throw new BadCredentialsException("Invalid API Key");
		}

		authentication.setAuthenticated(true);
		return authentication;
	}
}


import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Arrays;
import java.util.List;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

/**
 * The WebSecurityConfiguration class configures the security policies used for the exposed HTTP resource API.
 * In this case, it ensures that only clients with a valid API key can access the API.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfiguration {
    private static final String[] AUTH_WHITELIST = {
            // -- swagger ui
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/webjars/**",
            // spring-boot-starter-actuator health checks and other info
            "/actuator/**",
            "/actuator",
            // H2 Console
            "/console/**",
            // Spring Web Services
            "/ws/**",
            "/ws",
            // Thymeleaf demo FE
            "/customercorefe",
    };

    @Value("${apikey.header}")
    private String apiKeyHeader;

    @Value("${apikey.validkeys}")
    private String apiKeyValidKeys;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        final List<String> validAPIKeys = Arrays.asList(apiKeyValidKeys.split(";"));
        final APIKeyAuthFilter filter = new APIKeyAuthFilter(apiKeyHeader);
        filter.setAuthenticationManager(new APIKeyAuthenticationManager(validAPIKeys));

        http
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)
                        .cacheControl(HeadersConfigurer.CacheControlConfig::disable)
                )
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((request, response, exception) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"))
                        .accessDeniedHandler((request, response, exception) -> response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden"))
                )
                .sessionManagement(sessionManagement -> sessionManagement
                        .sessionCreationPolicy(STATELESS)
                )
                .addFilter(filter)
                .authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests
                        .requestMatchers(AUTH_WHITELIST).permitAll()
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}



import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.config.annotation.WsConfigurerAdapter;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

@EnableWs
@Configuration
public class WebServicesConfiguration extends WsConfigurerAdapter {
	@Bean
	public ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(ApplicationContext applicationContext) {
		MessageDispatcherServlet servlet = new MessageDispatcherServlet();
		servlet.setApplicationContext(applicationContext);
		servlet.setTransformWsdlLocations(true);
		return new ServletRegistrationBean<MessageDispatcherServlet>(servlet, "/ws/*");
	}

	@Bean(name = "customers")
	public DefaultWsdl11Definition defaultWsdl11Definition(XsdSchema countriesSchema) {
		DefaultWsdl11Definition wsdl11Definition = new DefaultWsdl11Definition();
		wsdl11Definition.setPortTypeName("CustomersPort");
		wsdl11Definition.setLocationUri("/ws");
		wsdl11Definition.setTargetNamespace("http://lm.com/ccore");
		wsdl11Definition.setSchema(countriesSchema);
		return wsdl11Definition;
	}

	@Bean
	public XsdSchema countriesSchema() {
		return new SimpleXsdSchema(new ClassPathResource("customers.xsd"));
	}
}



import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

/**
 * This filter enabled logging on all requests. See
 * <a href="https://www.baeldung.com/spring-http-logging">this blog post</a> for
 * details.
 */
@Configuration
public class RequestLoggingFilterConfig {

	@Bean
	public CommonsRequestLoggingFilter logFilter() {
		CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
		filter.setIncludeQueryString(true);
		filter.setIncludePayload(false);
		filter.setIncludeHeaders(true);
		return filter;
	}
}

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This @interface declaration defines a custom @PhoneNumber annotation which
 * can be used to validate phone numbers in a request DTO. Note that the
 * PhoneNumberValidator class performs the actual validation.
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PhoneNumberValidator.class)
public @interface PhoneNumber {
	String message() default "must be a valid Swiss phone number (e.g, +4155 222 41 11)";
	Class<?>[] groups() default {};
	Class<? extends Payload>[] payload() default {};
}


import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class validates phone numbers using Google's libphonenumber (see https://github.com/googlei18n/libphonenumber).
 * */
public class PhoneNumberValidator implements ConstraintValidator<PhoneNumber, String> {
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	@Override
	public boolean isValid(String phoneNumberStr, ConstraintValidatorContext context) {
		PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
		try {
			Phonenumber.PhoneNumber phoneNumber = phoneUtil.parse(phoneNumberStr, "CH");
			return phoneUtil.isValidNumber(phoneNumber);
		} catch (NumberParseException e) {
			logger.info("'" + phoneNumberStr + "' is not a valid phone number.", e);
			return false;
		}
	}
}


import java.util.List;

/**
 * The CitiesResponseDto represents a list of city names, transferred as simple
 * (atomic) strings. This is an example of the <a href=
 * "https://www.microservice-api-patterns.org/patterns/structure/representationElements/AtomicParameter">Atomic
 * Parameter</a> pattern.
 */
public class CitiesResponseDto {
	private final List<String> cities;

	public CitiesResponseDto(List<String> cities) {
		this.cities = cities;
	}

	public List<String> getCities() {
		return cities;
	}
}


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * You might be wondering why this Exception class is in the dtos package. Exceptions of this type
 * are thrown whenever the client sends a request with an invalid customer id. Spring will then
 * convert this exception into an HTTP 404 response.
 * */
@ResponseStatus(code = HttpStatus.NOT_FOUND)
public class CustomerNotFoundException extends RuntimeException {
	private static final long serialVersionUID = -402909143828880299L;

	public CustomerNotFoundException(String errorMessage) {
		super(errorMessage);
	}
}

import java.util.List;

import org.springframework.hateoas.RepresentationModel;

/**
 * The CustomersResponseDto holds a collection of @CustomerResponseDto
 * Parameter Trees. This class is an example of the <a href=
 * "https://www.microservice-api-patterns.org/patterns/structure/representationElements/ParameterForest">Parameter
 * Forest</a> pattern.
 */
public class CustomersResponseDto extends RepresentationModel {
	private final List<CustomerResponseDto> customers;

	public CustomersResponseDto(List<CustomerResponseDto> customers) {
		this.customers = customers;
	}

	public List<CustomerResponseDto> getCustomers() {
		return customers;
	}
}


import com.fasterxml.jackson.annotation.JsonFormat;
import com.lakesidemutual.customercore.domain.customer.Address;
import com.lakesidemutual.customercore.domain.customer.CustomerProfileEntity;
import com.lakesidemutual.customercore.interfaces.validation.PhoneNumber;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;

import java.util.Date;
import java.util.Objects;

/**
 * CustomerProfileUpdateRequestDto is a data transfer object (DTO) that represents the personal data (customer profile) of a customer.
 * It is sent to the CustomerInformationHolder when a new customer is created or the profile of an existing customer is updated.
 */
public class CustomerProfileUpdateRequestDto {
    @NotEmpty
    private String firstname;

    @NotEmpty
    private String lastname;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date birthday;

    @NotEmpty
    private String streetAddress;

    @NotEmpty
    private String postalCode;

    @NotEmpty
    private String city;

    @Email
    @NotEmpty
    private String email;

    @PhoneNumber
    private String phoneNumber;

    public CustomerProfileUpdateRequestDto() {
    }

    public CustomerProfileUpdateRequestDto(String firstname, String lastname, Date birthday, String streetAddress, String postalCode, String city, String email, String phoneNumber) {
        this.firstname = firstname;
        this.lastname = lastname;
        this.birthday = birthday;
        this.streetAddress = streetAddress;
        this.postalCode = postalCode;
        this.city = city;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }

    public CustomerProfileEntity toDomainObject() {
        Address address = new Address(getStreetAddress(), getPostalCode(), getCity());
        return new CustomerProfileEntity(getFirstname(), getLastname(), getBirthday(), address, getEmail(), getPhoneNumber());
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public Date getBirthday() {
        return birthday;
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }

    public String getStreetAddress() {
        return streetAddress;
    }

    public void setStreetAddress(String streetAddress) {
        this.streetAddress = streetAddress;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomerProfileUpdateRequestDto that = (CustomerProfileUpdateRequestDto) o;
        return Objects.equals(firstname, that.firstname) && Objects.equals(lastname, that.lastname) && Objects.equals(birthday, that.birthday) && Objects.equals(streetAddress, that.streetAddress) && Objects.equals(postalCode, that.postalCode) && Objects.equals(city, that.city) && Objects.equals(email, that.email) && Objects.equals(phoneNumber, that.phoneNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstname, lastname, birthday, streetAddress, postalCode, city, email, phoneNumber);
    }
}


import java.util.List;

import org.springframework.hateoas.RepresentationModel;

/**
 * The PaginatedCustomerResponseDto holds a collection of CustomerResponseDto
 * with additional control 
 * <a href="https://microservice-api-patterns.org/patterns/structure/elementStereotypes/MetadataElement">Metadata Elements</a> 
 * such as the limit, offset and size that are used in the 
 * <a href="https://www.microservice-api-patterns.org/patterns/structure/compositeRepresentations/Pagination">Pagination</a>
 * pattern, specifically the <em>Offset-Based</em> Pagination variant.
 */
public class PaginatedCustomerResponseDto extends RepresentationModel {
	private final String filter;
	private final int limit;
	private final int offset;
	private final int size;

	private final List<CustomerResponseDto> customers;

	public PaginatedCustomerResponseDto(String filter, int limit, int offset, int size,
			List<CustomerResponseDto> customers) {
		this.filter = filter;
		this.limit = limit;
		this.offset = offset;
		this.size = size;
		this.customers = customers;
	}

	public List<CustomerResponseDto> getCustomers() {
		return customers;
	}

	public String getFilter() {
		return filter;
	}

	public int getLimit() {
		return limit;
	}

	public int getOffset() {
		return offset;
	}

	public int getSize() {
		return size;
	}
}

import java.util.Collection;
import java.util.Date;
import java.util.Set;

import org.springframework.hateoas.RepresentationModel;

import com.lakesidemutual.customercore.domain.customer.Address;
import com.lakesidemutual.customercore.domain.customer.CustomerAggregateRoot;
import com.lakesidemutual.customercore.domain.customer.CustomerProfileEntity;

/**
 * The CustomerResponseDto represents a customer, including their complete move history. This is an example of the <a href=
 * "https://www.microservice-api-patterns.org/patterns/structure/representationElements/ParameterTree">Parameter
 * Tree</a> pattern.
 */
public class CustomerResponseDto extends RepresentationModel {
	private final String customerId;

	private final String firstname;

	private final String lastname;

	private final Date birthday;

	private final String streetAddress;

	private final String postalCode;

	private final String city;

	private final String email;

	private final String phoneNumber;

	private final Collection<Address> moveHistory;

	public CustomerResponseDto(Set<String> includedFields, CustomerAggregateRoot customer) {
		this.customerId = select(includedFields, "customerId", customer.getId().getId());

		final CustomerProfileEntity profile = customer.getCustomerProfile();
		this.firstname = select(includedFields, "firstname", profile.getFirstname());
		this.lastname = select(includedFields, "lastname", profile.getLastname());
		this.birthday = select(includedFields, "birthday", profile.getBirthday());
		this.streetAddress = select(includedFields, "streetAddress", profile.getCurrentAddress().getStreetAddress());
		this.postalCode = select(includedFields, "postalCode", profile.getCurrentAddress().getPostalCode());
		this.city = select(includedFields, "city", profile.getCurrentAddress().getCity());
		this.email = select(includedFields, "email", profile.getEmail());
		this.phoneNumber = select(includedFields, "phoneNumber", profile.getPhoneNumber());
		this.moveHistory = select(includedFields, "moveHistory", profile.getMoveHistory());
	}

	private static <T> T select(Set<String> includedFields, String fieldName, T value) {
		if(includedFields.isEmpty() || includedFields.contains(fieldName)) {
			return value;
		} else {
			return null;
		}
	}

	public String getCustomerId() {
		return customerId;
	}

	public String getFirstname() {
		return firstname;
	}

	public String getLastname() {
		return lastname;
	}

	public Date getBirthday() {
		return birthday;
	}

	public String getStreetAddress() {
		return streetAddress;
	}

	public String getPostalCode() {
		return postalCode;
	}

	public String getCity() {
		return city;
	}

	public String getEmail() {
		return email;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public Collection<Address> getMoveHistory() {
		return moveHistory;
	}
}

import com.lakesidemutual.customercore.domain.customer.Address;
import jakarta.validation.constraints.NotEmpty;

/**
 * The AddressDto represents the message payload to change a customer's address. This is an example of the <a href=
 * "https://www.microservice-api-patterns.org/patterns/structure/representationElements/AtomicParameterList">Atomic Parameter List</a> pattern.
 */
public class AddressDto {
	@NotEmpty
	private String streetAddress;

	@NotEmpty
	private String postalCode;

	@NotEmpty
	private String city;

	public AddressDto() {
	}

	public AddressDto(String streetAddress, String postalCode, String city) {
		this.streetAddress = streetAddress;
		this.postalCode = postalCode;
		this.city = city;
	}

	public static AddressDto fromDomainObject(Address address) {
		return new AddressDto(address.getStreetAddress(), address.getPostalCode(), address.getCity());
	}

	public Address toDomainObject() {
		return new Address(streetAddress, postalCode, city);
	}

	public String getStreetAddress() {
		return streetAddress;
	}

	public String getPostalCode() {
		return postalCode;
	}

	public String getCity() {
		return city;
	}

	public void setStreetAddress(String streetAddress) {
		this.streetAddress = streetAddress;
	}

	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}

	public void setCity(String city) {
		this.city = city;
	}
}


import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.lakesidemutual.customercore.domain.customer.CustomerId;

/**
 * This converter class allows us to use CustomerId as the type of
 * a @PathVariable parameter in a Spring @RestController class.
 */
@Component
public class StringToCustomerIdConverter implements Converter<String, CustomerId> {
	@Override
	public CustomerId convert(String source) {
		return new CustomerId(source);
	}
}


import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.microserviceapipatterns.domaindrivendesign.DomainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

/**
 * This is a DDD Domain Service and is automatically injected thanks
 * to the @Component annotation. Note that we could also have used the @Service
 * annotation, which is an alias for @Component.
 */
@Component
public class CityLookupService implements DomainService {
	private final static String CSV_FILE = "cities_by_postalcode_switzerland.csv";
	private final static char CSV_SEPARATOR = ';';
	private final static String POSTAL_CODE_KEY = "postalCode";
	private final static String CITY_KEY = "city";
	private static Multimap<String, String> lookupMap = null;
	private final static Logger logger = LoggerFactory.getLogger(CityLookupService.class);

	private static Multimap<String, String> loadLookupMap() {
		Multimap<String, String> map = TreeMultimap.create();
		try(InputStream file = new ClassPathResource(CSV_FILE).getInputStream()) {
			CsvMapper mapper = new CsvMapper();
			CsvSchema schema = CsvSchema.emptySchema().withHeader().withColumnSeparator(CSV_SEPARATOR);
			MappingIterator<Map<String, String>> readValues = mapper.readerFor(Map.class).with(schema).readValues(file);
			List<Map<String, String>> values = readValues.readAll();

			for (Map<String, String> value : values) {
				String postalCode = value.get(POSTAL_CODE_KEY).trim();
				String city = value.get(CITY_KEY).trim();
				if (city == null || postalCode == null) {
					continue;
				}
				map.put(postalCode, city);
			}
		} catch (IOException e) {
			logger.error("Failed to create city lookup-map.", e);
		}
		return map;
	}

	private static Multimap<String, String> getLookupMap() {
		if (lookupMap == null) {
			lookupMap = loadLookupMap();
			logger.info("Loaded " + lookupMap.size() + " postal-code / city pairs.");
		}
		return lookupMap;
	}

	/**
	 * Returns an alphabetically ordered list of cities that match the given postal
	 * code.
	 */
	public List<String> getCitiesForPostalCode(String postalCode) {
		Multimap<String, String> lookupMap = getLookupMap();
		return new ArrayList<>(lookupMap.get(postalCode));
	}
}


import jakarta.persistence.Embeddable;
import org.apache.commons.lang3.RandomStringUtils;
import org.microserviceapipatterns.domaindrivendesign.EntityIdentifier;
import org.microserviceapipatterns.domaindrivendesign.ValueObject;

import java.io.Serializable;
import java.util.Objects;

/**
 * A CustomerId is a value object that is used to represent the unique id of a customer.
 */
@Embeddable
public class CustomerId implements Serializable, ValueObject, EntityIdentifier<String> {
	private static final long serialVersionUID = 1L;

	private String id;

	public CustomerId() {
		this.setId(null);
	}

	/**
	 * This constructor is needed by ControllerLinkBuilder, see the following
	 * spring-hateoas issue for details:
	 * https://github.com/spring-projects/spring-hateoas/issues/352
	 */
	public CustomerId(String id) {
		this.setId(id);
	}

	public static CustomerId random() {
		return new CustomerId(RandomStringUtils.randomAlphanumeric(10).toLowerCase());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj == null) {
			return false;
		}

		if (getClass() != obj.getClass()) {
			return false;
		}

		CustomerId other = (CustomerId) obj;
		return Objects.equals(getId(), other.getId());
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(getId());
	}

	@Override
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return getId();
	}
}


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.lakesidemutual.customercore.infrastructure.CustomerRepository;
import org.microserviceapipatterns.domaindrivendesign.Factory;

/**
 * CustomerFactory is a factory that is responsible for the creation of CustomerAggregateRoot objects.
 * It makes sure that each new customer has a unique CustomerId and that phone numbers are formatted
 * correctly.
 * */
@Component
public class CustomerFactory implements Factory {
	@Autowired
	private CustomerRepository customerRepository;

	public CustomerAggregateRoot create(CustomerProfileEntity customerProfile) {
		CustomerId id = customerRepository.nextId();
		customerProfile.setPhoneNumber(formatPhoneNumber(customerProfile.getPhoneNumber()));
		return new CustomerAggregateRoot(id, customerProfile);
	}

	public static String formatPhoneNumber(String phoneNumberStr) {
		PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
		try {
			PhoneNumber phoneNumber = phoneUtil.parse(phoneNumberStr, "CH");
			return phoneUtil.format(phoneNumber, PhoneNumberFormat.NATIONAL);
		} catch (NumberParseException e) {
			throw new AssertionError();
		}
	}
}


import io.github.adr.embedded.MADR;
import jakarta.persistence.CascadeType;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.microserviceapipatterns.domaindrivendesign.RootEntity;

/**
 * CustomerAggregateRoot is the root entity of the Customer aggregate. Note that there is
 * no class for the Customer aggregate, so the package can be seen as aggregate.
 */
@Entity
@Table(name = "customers")
public class CustomerAggregateRoot implements RootEntity {

	@EmbeddedId
	private CustomerId id;

	@OneToOne(cascade = CascadeType.ALL)
	private CustomerProfileEntity customerProfile;

	public CustomerAggregateRoot() {
	}

	public CustomerAggregateRoot(CustomerId id, CustomerProfileEntity customerProfile) {
		this.id = id;
		this.customerProfile = customerProfile;
	}

	public CustomerProfileEntity getCustomerProfile() {
		return customerProfile;
	}

	public CustomerId getId() {
		return id;
	}

	@MADR(
			value = 1,
			title = "Data transfer between interface layer and domain layer",
			contextAndProblem = "Need to pass information from the interfaces layer to the domain layer without introducing a layering violation",
			alternatives = {
					"Pass existing domain objects",
					"Pass the DTOs directly",
					"Pass the components of the DTO",
					"Add a new value type in the domain layer and use it as parameter object"
			},
			chosenAlternative = "Pass existing domain objects",
			justification = "This solution doesn't introduce a layering violation and it is simple because it doesn't require any additional classes."
			)
	public void moveToAddress(Address address) {
		customerProfile.moveToAddress(address);
	}

	public void updateCustomerProfile(CustomerProfileEntity updatedCustomerProfile) {
		customerProfile.setFirstname(updatedCustomerProfile.getFirstname());
		customerProfile.setLastname(updatedCustomerProfile.getLastname());
		customerProfile.setBirthday(updatedCustomerProfile.getBirthday());
		customerProfile.moveToAddress(updatedCustomerProfile.getCurrentAddress());
		customerProfile.setEmail(updatedCustomerProfile.getEmail());
		customerProfile.setPhoneNumber(CustomerFactory.formatPhoneNumber(updatedCustomerProfile.getPhoneNumber()));
	}
}


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.microserviceapipatterns.domaindrivendesign.ValueObject;

import java.util.Objects;

/**
 * An Address is a value object that is used to represent the postal address of a customer.
 *
 * You might be wondering why the Address class implements the ValueObject interface even though it has a JPA @Entity annotation.
 * This discrepancy exists for technical reasons. JPA requires Address to be declared as an entity, because it is part of a one-to-many
 * relationship. However, in the DDD sense, Address behaves like a Value Object (i.e., it has no id and is immutable).
 * */
@Entity
@Table(name = "addresses")
public class Address implements ValueObject {
	@GeneratedValue
	@Id
	private Long id;

	private final String streetAddress;

	private final String postalCode;

	private final String city;

	public Address() {
		this.streetAddress = null;
		this.postalCode = null;
		this.city = null;
	}

	public Address(String streetAddress, String postalCode, String city) {
		this.streetAddress = streetAddress;
		this.postalCode = postalCode;
		this.city = city;
	}

	public String getStreetAddress() {
		return streetAddress;
	}

	public String getPostalCode() {
		return postalCode;
	}

	public String getCity() {
		return city;
	}

	@Override
	public String toString() {
		return String.format("%s, %s %ss", streetAddress, postalCode, city);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}

		Address other = (Address) obj;
		return Objects.equals(streetAddress, other.streetAddress) &&
				Objects.equals(postalCode, other.postalCode) &&
				Objects.equals(city, other.city);
	}

	@Override
	public int hashCode() {
		return Objects.hash(streetAddress, postalCode, city);
	}
}

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Objects;

/**
 * CustomerProfileEntity is an entity that contains the personal data (customer profile) of a CustomerAggregateRoot.
 */
@Entity
public class CustomerProfileEntity implements Serializable, org.microserviceapipatterns.domaindrivendesign.Entity {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstname;

    private String lastname;

    private Date birthday;

    /**
     * The usage of the javax.persistance annotations breaks the strict layering. We do this deliberately here, because the relatively small
     * size of this application does not warrant the additional complexity of having a separate infrastructure data model (yet).
     */
    @OneToOne(cascade = CascadeType.ALL)
    private Address currentAddress;

    private String email;

    private String phoneNumber;

    @OneToMany(cascade = CascadeType.ALL)
    private Collection<Address> moveHistory;

    public CustomerProfileEntity() {
    }

    public CustomerProfileEntity(String firstname, String lastname, Date birthday, Address currentAddress, String email, String phoneNumber) {
        this.firstname = firstname;
        this.lastname = lastname;
        this.birthday = birthday;
        this.currentAddress = currentAddress;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.moveHistory = new ArrayList<>();
    }

    public Date getBirthday() {
        return birthday;
    }

    public Address getCurrentAddress() {
        return currentAddress;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstname() {
        return firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public Collection<Address> getMoveHistory() {
        return moveHistory;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void moveToAddress(Address address) {
        if (!currentAddress.equals(address)) {
            moveHistory.add(currentAddress);
            setCurrentAddress(address);
        }
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }

    public void setCurrentAddress(Address currentAddress) {
        this.currentAddress = currentAddress;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public void setMoveHistory(Collection<Address> moveHistory) {
        this.moveHistory = moveHistory;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomerProfileEntity that = (CustomerProfileEntity) o;
        return Objects.equals(firstname, that.firstname) &&
                Objects.equals(lastname, that.lastname) &&
                Objects.equals(birthday, that.birthday) &&
                Objects.equals(currentAddress, that.currentAddress) &&
                Objects.equals(email, that.email) &&
                Objects.equals(phoneNumber, that.phoneNumber) &&
                Objects.equals(moveHistory, that.moveHistory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstname, lastname, birthday, currentAddress, email, phoneNumber);
    }
}

import org.microserviceapipatterns.domaindrivendesign.BoundedContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CustomerSelfServiceApplication is the execution entry point of the Customer Self-Service backend which
 * is one of the functional/system/application Bounded Contexts of Lakeside Mutual.
 */
@SpringBootApplication
public class CustomerSelfServiceApplication implements BoundedContext {
	private static Logger logger = LoggerFactory.getLogger(CustomerSelfServiceApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(CustomerSelfServiceApplication.class, args);
		logger.info("--- Customer Self-Service backend started ---");
	}
}


import com.lakesidemutual.customerselfservice.domain.customer.CustomerId;
import com.lakesidemutual.customerselfservice.interfaces.dtos.city.CitiesResponseDto;
import com.lakesidemutual.customerselfservice.interfaces.dtos.customer.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.microserviceapipatterns.domaindrivendesign.InfrastructureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * CustomerCoreRemoteProxy is a remote proxy that interacts with the Customer Core in order to give
 * the Customer Self-Service Backend's own clients access to the shared customer data.
 */
@Component
public class CustomerCoreRemoteProxy implements InfrastructureService, CustomerCoreServiceMBean {
    @Value("${customercore.baseURL}")
    private String customerCoreBaseURL;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private RestTemplate restTemplate;

    int successfulAttemptsCounter = 0;
    int unsuccessfulAttemptsCounter = 0;
    int fallBackMethodExecutionsCounter = 0;

    // TODO test/demo that protected methods actually stops executing for defined time period,
    // otherwise the circuit breaker does not do much more than a regular exception handler
    @CircuitBreaker(name = "CustomerCoreRemoteProxy", fallbackMethod = "getDummyCustomer")
    public CustomerDto getCustomer(CustomerId customerId) {
        try {
            final String url = customerCoreBaseURL + "/customers/" + customerId.getId();
            logger.info("About to GET customer with id " + customerId);
            List<CustomerDto> customers = restTemplate.getForObject(url, CustomersDto.class).getCustomers();
            successfulAttemptsCounter++;
            return customers.isEmpty() ? null : customers.get(0);
        } catch (RestClientException e) {
            final String errorMessage = "Failed to connect to Customer Core.";
            logger.info(errorMessage, e);
            unsuccessfulAttemptsCounter++;
            throw new CustomerCoreNotAvailableException(errorMessage);
        }
    }

    public CustomerDto getDummyCustomer(CustomerId customerId) {
        logger.warn("Circuit Breaker active, failed to connect to Customer Core!");

        // TODO return dummy/default customer (from local DB)?
        fallBackMethodExecutionsCounter++;

        CustomerDto customerCopy = new CustomerDto();
        customerCopy.setCustomerId(customerId.toString());
        CustomerProfileDto emptyProfile = new CustomerProfileDto();
        emptyProfile.setFirstname("WARNING: No first name info available");
        emptyProfile.setLastname("WARNING: No last name info available");
        emptyProfile.setEmail("it-support@lm.com");
        emptyProfile.setPhoneNumber("WARNING: No phone number info available");
        AddressDto currentAddress = new AddressDto();
        currentAddress.setStreetAddress("n/a");
        currentAddress.setPostalCode("n/a");
        currentAddress.setCity("n/a");
        emptyProfile.setCurrentAddress(currentAddress);
        customerCopy.setCustomerProfile(emptyProfile);
        return customerCopy;
    }

    // TODO protect other methods too?

    public ResponseEntity<AddressDto> changeAddress(CustomerId customerId, AddressDto requestDto) {
        try {
            final String url = customerCoreBaseURL + "/customers/" + customerId.getId() + "/address";
            final HttpEntity<AddressDto> requestEntity = new HttpEntity<>(requestDto);
            return restTemplate.exchange(url, HttpMethod.PUT, requestEntity, AddressDto.class);
        } catch (RestClientException e) {
            final String errorMessage = "Failed to connect to Customer Core.";
            logger.info(errorMessage, e);
            throw new CustomerCoreNotAvailableException(errorMessage);
        }
    }

    public CustomerDto createCustomer(CustomerProfileUpdateRequestDto requestDto) {
        try {
            final String url = customerCoreBaseURL + "/customers";
            return restTemplate.postForObject(url, requestDto, CustomerDto.class);
        } catch (RestClientException e) {
            final String errorMessage = "Failed to connect to Customer Core.";
            logger.info(errorMessage, e);
            throw new CustomerCoreNotAvailableException(errorMessage);
        }
    }

    public ResponseEntity<CitiesResponseDto> getCitiesForPostalCode(String postalCode) {
        try {
            final String url = customerCoreBaseURL + "/cities/" + postalCode;
            return restTemplate.getForEntity(url, CitiesResponseDto.class);
        } catch (RestClientException e) {
            final String errorMessage = "Failed to connect to Customer Core.";
            logger.info(errorMessage, e);
            throw new CustomerCoreNotAvailableException(errorMessage);
        }
    }

    @Override
    public int getSuccessfullAttemptsCounter() {
        return successfulAttemptsCounter;
    }

    @Override
    public int getUnuccessfullAttemptsCounter() {
        return unsuccessfulAttemptsCounter;
    }

    @Override
    public int getFallbackMethodExecutionCounter() {
        return fallBackMethodExecutionsCounter;
    }


    @Override
    public void resetCounters() {
        successfulAttemptsCounter = 0;
        unsuccessfulAttemptsCounter = 0;
        fallBackMethodExecutionsCounter = 0;
    }

}


public interface CustomerCoreServiceMBean {

    public int getSuccessfullAttemptsCounter();
    public int getUnuccessfullAttemptsCounter();
    public int getFallbackMethodExecutionCounter();
    
    public void resetCounters();
}

import org.springframework.data.jpa.repository.JpaRepository;

import com.lakesidemutual.customerselfservice.domain.identityaccess.UserLoginEntity;
import org.microserviceapipatterns.domaindrivendesign.Repository;

/**
 * The UserLoginRepository can be used to read and write UserLogin objects from and to the backing database. Spring automatically
 * searches for interfaces that extend the JpaRepository interface and creates a corresponding Spring bean for each of them. For more information
 * on repositories visit the <a href="https://docs.spring.io/spring-data/jpa/docs/current/reference/html/">Spring Data JPA - Reference Documentation</a>.
 * */
public interface UserLoginRepository extends JpaRepository<UserLoginEntity, Long>, Repository {

	public UserLoginEntity findByEmail(String email);

}


import java.util.Date;

import org.microserviceapipatterns.domaindrivendesign.InfrastructureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import com.lakesidemutual.customerselfservice.domain.insurancequoterequest.CustomerDecisionEvent;
import com.lakesidemutual.customerselfservice.domain.insurancequoterequest.InsuranceQuoteRequestEvent;
import com.lakesidemutual.customerselfservice.interfaces.dtos.insurancequoterequest.InsuranceQuoteRequestDto;

/**
 * PolicyManagementMessageProducer is an infrastructure service class that is used to notify the Policy Management Backend
 * when a new insurance quote request has been created (InsuranceQuoteRequestEvent) or when a customer has accepted or rejected
 * an insurance quote (CustomerDecisionEvent). These events are transmitted via an ActiveMQ message queue.
 * */
@Component
public class PolicyManagementMessageProducer implements InfrastructureService {
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	@Value("${insuranceQuoteRequestEvent.queueName}")
	private String insuranceQuoteRequestEventQueue;

	@Value("${customerDecisionEvent.queueName}")
	private String customerDecisionEventQueue;

	@Autowired
	private JmsTemplate jmsTemplate;

	public void sendInsuranceQuoteRequest(Date date, InsuranceQuoteRequestDto insuranceQuoteRequestDto) {
		InsuranceQuoteRequestEvent insuranceQuoteRequestEvent = new InsuranceQuoteRequestEvent(date, insuranceQuoteRequestDto);
		emitInsuranceQuoteRequestEvent(insuranceQuoteRequestEvent);
	}

	public void sendCustomerDecision(Date date, Long insuranceQuoteRequestId, boolean quoteAccepted) {
		CustomerDecisionEvent customerDecisionEvent = new CustomerDecisionEvent(date, insuranceQuoteRequestId, quoteAccepted);
		emitCustomerDecisionEvent(customerDecisionEvent);
	}

	private void emitInsuranceQuoteRequestEvent(InsuranceQuoteRequestEvent insuranceQuoteRequestEvent) {
		try {
			jmsTemplate.convertAndSend(insuranceQuoteRequestEventQueue, insuranceQuoteRequestEvent);
			logger.info("Successfully sent a insurance quote request to the Policy Management backend.");
		} catch(JmsException exception) {
			logger.error("Failed to send a insurance quote request to the Policy Management backend.", exception);
		}
	}

	private void emitCustomerDecisionEvent(CustomerDecisionEvent customerDecisionEvent) {
		try {
			jmsTemplate.convertAndSend(customerDecisionEventQueue, customerDecisionEvent);
			logger.info("Successfully sent a customer decision event to the Policy Management backend.");
		} catch(JmsException exception) {
			logger.error("Failed to send a customer decision event to the Policy Management backend.", exception);
		}
	}
}

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lakesidemutual.customerselfservice.domain.customer.CustomerId;
import com.lakesidemutual.customerselfservice.domain.insurancequoterequest.InsuranceQuoteRequestAggregateRoot;
import org.microserviceapipatterns.domaindrivendesign.Repository;

/**
 * The InsuranceQuoteRequestRepository can be used to read and write InsuranceQuoteRequestAggregateRoot objects from and to the backing database. Spring automatically
 * searches for interfaces that extend the JpaRepository interface and creates a corresponding Spring bean for each of them. For more information
 * on repositories visit the <a href="https://docs.spring.io/spring-data/jpa/docs/current/reference/html/">Spring Data JPA - Reference Documentation</a>.
 * */
public interface InsuranceQuoteRequestRepository extends JpaRepository<InsuranceQuoteRequestAggregateRoot, Long>, Repository {
	List<InsuranceQuoteRequestAggregateRoot> findByCustomerInfo_CustomerIdOrderByDateDesc(CustomerId customerId);
	List<InsuranceQuoteRequestAggregateRoot> findAllByOrderByDateDesc();
}

import java.io.InputStream;
import java.text.ParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.lakesidemutual.customerselfservice.domain.customer.CustomerId;
import com.lakesidemutual.customerselfservice.domain.identityaccess.UserLoginEntity;
import com.lakesidemutual.customerselfservice.infrastructure.UserLoginRepository;

/**
 * The run() method of the DataLoader class is automatically executed when the application launches.
 * It populates the database with sample user logins that can be used to test the application.
 * */
@Component
@Profile("!test")
public class DataLoader implements ApplicationRunner {
	@Autowired
	private UserLoginRepository userRepository;

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	private List<UserLoginEntity> createUserLoginsFromDummyData(List<Map<String, String>> userData) {
		PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
		return userData.stream().map(data -> {
			final String email = data.get("email");
			final String hashedPassword = passwordEncoder.encode(data.get("password"));
			final CustomerId customerId = new CustomerId(data.get("id"));
			return new UserLoginEntity(email, hashedPassword, "USER", customerId);
		}).collect(Collectors.toList());
	}

	/**
	 * Note: The files mock_users_small.csv and mock_users_large.csv are the same as the files
	 * mock_customers_small.csv and mock_customers_large.csv in the Customer Core. The Customer
	 * Core DataLoader creates a customer for each row in the corresponding CSV file and the
	 * Customer Self-Service DataLoader creates a user login for each of these customers. Therefore,
	 * these files should be kept in sync.
	 *
	 * This dummy data was generated using https://mockaroo.com/
	 */
	private List<Map<String, String>> loadUsers() {
		try(InputStream file = new ClassPathResource("mock_users_small.csv").getInputStream()) {
			CsvMapper mapper = new CsvMapper();
			CsvSchema schema = CsvSchema.emptySchema().withHeader();
			MappingIterator<Map<String, String>> readValues = mapper.readerFor(Map.class).with(schema).readValues(file);
			return readValues.readAll();
		} catch (Exception e) {
			return Collections.emptyList();
		}
	}

	@Override
	public void run(ApplicationArguments args) throws ParseException {
		if(userRepository.count() > 0) {
			logger.info("Skipping import of application dummy data, because the database already contains existing entities.");
			return;
		}

		List<Map<String, String>> userData = loadUsers();

		logger.info("Loaded " + userData.size() + " users.");

		List<UserLoginEntity> userLogins = createUserLoginsFromDummyData(userData);

		logger.info("Created " + userLogins.size() + " user logins.");

		userRepository.saveAll(userLogins);

		logger.info("Inserted " + userLogins.size() + " user logins into database.");

		logger.info("DataLoader has successfully imported all application dummy data, the application is now ready.");
	}
}

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lakesidemutual.customerselfservice.domain.identityaccess.UserLoginEntity;
import com.lakesidemutual.customerselfservice.infrastructure.UserLoginRepository;
import com.lakesidemutual.customerselfservice.interfaces.configuration.JwtUtils;
import com.lakesidemutual.customerselfservice.interfaces.dtos.identityaccess.AuthenticationRequestDto;
import com.lakesidemutual.customerselfservice.interfaces.dtos.identityaccess.AuthenticationResponseDto;
import com.lakesidemutual.customerselfservice.interfaces.dtos.identityaccess.SignupRequestDto;
import com.lakesidemutual.customerselfservice.interfaces.dtos.identityaccess.UserAlreadyExistsException;
import com.lakesidemutual.customerselfservice.interfaces.dtos.identityaccess.UserResponseDto;


/**
 * This class is a REST Controller that is used to authenticate existing users and to sign up new users.
 * */
@RestController
@RequestMapping("/auth")
public class AuthenticationController {
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private AuthenticationManager authenticationManager;

	@Value("${token.header}")
	private String tokenHeader;

	@Autowired
	private JwtUtils tokenUtils;

	@Autowired
	private UserDetailsService userDetailsService;

	@Autowired
	private UserLoginRepository userRepository;

	@Operation(summary = "Authenticate a user based on a given email address and password.")
	@PostMapping
	public ResponseEntity<AuthenticationResponseDto> authenticationRequest(
			@Parameter(description = "the email and password used to authenticate the user", required = true) @RequestBody AuthenticationRequestDto authenticationRequest)
					throws AuthenticationException {

		// Perform the authentication
		try {
			Authentication authentication = this.authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
					authenticationRequest.getEmail(), authenticationRequest.getPassword()));
			SecurityContextHolder.getContext().setAuthentication(authentication);
		} catch(AuthenticationException e) {
			logger.info("Authentication of user '" + authenticationRequest.getEmail() + "' failed.");
			throw e;
		}

		// Reload password post-authentication so we can generate token
		UserDetails userDetails = this.userDetailsService.loadUserByUsername(authenticationRequest.getEmail());
		String token = this.tokenUtils.generateToken(userDetails);

		// Return the token
		return ResponseEntity.ok(new AuthenticationResponseDto(authenticationRequest.getEmail(), token));
	}

	@Operation(summary = "Create a new user.")
	@PostMapping(value = "/signup")
	public ResponseEntity<UserResponseDto> signupUser(
			@Parameter(description = "the email and password used to create a new user", required = true) @Valid @RequestBody SignupRequestDto registration) {

		if (userRepository.findByEmail(registration.getEmail()) != null) {
			final String errorMessage = "User with email '" + registration.getEmail() + "' does already exist.";
			logger.info(errorMessage);
			throw new UserAlreadyExistsException(errorMessage);
		}

		PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
		String hashedPassword = passwordEncoder.encode(registration.getPassword());
		UserLoginEntity newUser = new UserLoginEntity(registration.getEmail(), hashedPassword, "ADMIN", null);
		userRepository.save(newUser);
		return ResponseEntity.ok(new UserResponseDto(registration.getEmail(), null));
	}
}


import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import com.lakesidemutual.customerselfservice.domain.insurancequoterequest.InsuranceQuoteExpiredEvent;
import com.lakesidemutual.customerselfservice.domain.insurancequoterequest.InsuranceQuoteRequestAggregateRoot;
import com.lakesidemutual.customerselfservice.infrastructure.InsuranceQuoteRequestRepository;

/**
 * InsuranceQuoteExpiredMessageConsumer is a Spring component that consumes InsuranceQuoteExpiredEvents
 * as they arrive through the ActiveMQ message queue. It processes these events by marking the corresponding
 * insurance quote requests as expired.
 * */
@Component
public class InsuranceQuoteExpiredMessageConsumer {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private InsuranceQuoteRequestRepository insuranceQuoteRequestRepository;

	@JmsListener(destination = "${insuranceQuoteExpiredEvent.queueName}")
	public void receiveInsuranceQuoteExpiredEvent(final Message<InsuranceQuoteExpiredEvent> message) {
		logger.info("A new InsuranceQuoteResponseEvent has been received.");
		final InsuranceQuoteExpiredEvent insuranceQuoteExpiredEvent = message.getPayload();
		final Long id = insuranceQuoteExpiredEvent.getInsuranceQuoteRequestId();
		final Optional<InsuranceQuoteRequestAggregateRoot> insuranceQuoteRequestOpt = insuranceQuoteRequestRepository.findById(id);

		if(!insuranceQuoteRequestOpt.isPresent()) {
			logger.error("Unable to process an insurance quote expired event with an invalid insurance quote request id.");
			return;
		}

		final InsuranceQuoteRequestAggregateRoot insuranceQuoteRequest = insuranceQuoteRequestOpt.get();
		insuranceQuoteRequest.markQuoteAsExpired(insuranceQuoteExpiredEvent.getDate());
		logger.info("The insurance quote for insurance quote request " + insuranceQuoteRequest.getId() + " has expired.");
		insuranceQuoteRequestRepository.save(insuranceQuoteRequest);
	}
}

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lakesidemutual.customerselfservice.domain.identityaccess.UserLoginEntity;
import com.lakesidemutual.customerselfservice.infrastructure.UserLoginRepository;
import com.lakesidemutual.customerselfservice.interfaces.dtos.identityaccess.UserResponseDto;

/**
 * This REST controller gives clients access to the current user. It is an example of the
 * <i>Information Holder Resource</i> pattern. This particular one is a special type of information holder called <i>Master Data Holder</i>.
 *
 * @see <a href="https://www.microservice-api-patterns.org/patterns/responsibility/endpointRoles/InformationHolderResource">Information Holder Resource</a>
 * @see <a href="https://www.microservice-api-patterns.org/patterns/responsibility/informationHolderEndpointTypes/MasterDataHolder">Master Data Holder</a>
 */
@RestController
@RequestMapping("/user")
public class UserInformationHolder {

	@Autowired
	private UserLoginRepository userLoginRepository;

	@Operation(summary = "Get the user details for the currently logged-in user.")
	@PreAuthorize("isAuthenticated()")
	@GetMapping
	public ResponseEntity<UserResponseDto> getCurrentUser(Authentication authentication) {
		String username = authentication.getName();
		UserLoginEntity user = userLoginRepository.findByEmail(username);
		String email = user.getEmail();
		String customerId = user.getCustomerId() != null ? user.getCustomerId().getId() : null;
		UserResponseDto response = new UserResponseDto(email, customerId);
		return ResponseEntity.ok(response);
	}
}


import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import com.lakesidemutual.customerselfservice.domain.insurancequoterequest.InsuranceQuoteRequestAggregateRoot;
import com.lakesidemutual.customerselfservice.domain.insurancequoterequest.PolicyCreatedEvent;
import com.lakesidemutual.customerselfservice.infrastructure.InsuranceQuoteRequestRepository;

/**
 * PolicyCreatedMessageConsumer is a Spring component that consumes PolicyCreatedEvents
 * as they arrive through the ActiveMQ message queue. It processes these events by updating
 * the status of the corresponding insurance quote requests.
 * */
@Component
public class PolicyCreatedMessageConsumer {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private InsuranceQuoteRequestRepository insuranceQuoteRequestRepository;

	@JmsListener(destination = "${policyCreatedEvent.queueName}")
	public void receivePolicyCreatedEvent(final Message<PolicyCreatedEvent> message) {
		logger.info("A new PolicyCreatedEvent has been received.");
		final PolicyCreatedEvent policyCreatedEvent = message.getPayload();
		final Long id = policyCreatedEvent.getInsuranceQuoteRequestId();
		final Optional<InsuranceQuoteRequestAggregateRoot> insuranceQuoteRequestOpt = insuranceQuoteRequestRepository.findById(id);

		if(!insuranceQuoteRequestOpt.isPresent()) {
			logger.error("Unable to process a policy created event with an invalid insurance quote request id.");
			return;
		}

		final InsuranceQuoteRequestAggregateRoot insuranceQuoteRequest = insuranceQuoteRequestOpt.get();
		insuranceQuoteRequest.finalizeQuote(policyCreatedEvent.getPolicyId(), policyCreatedEvent.getDate());
		logger.info("The insurance quote for insurance quote request " + insuranceQuoteRequest.getId() + " has expired.");
		insuranceQuoteRequestRepository.save(insuranceQuoteRequest);
	}
}

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lakesidemutual.customerselfservice.infrastructure.CustomerCoreRemoteProxy;
import com.lakesidemutual.customerselfservice.interfaces.dtos.city.CitiesResponseDto;

/**
 * This REST controller allows clients to retrieve a list of cities that match a given postal code. It is an application of
 * the <a href="https://www.microservice-api-patterns.org/patterns/responsibility/informationHolderEndpointTypes/ReferenceDataHolder">Reference Data Holder</a> pattern.
 * A Reference Data Holder is a dedicated endpoint that serves as single point of reference for static data (i.e., data that almost never changes).
 *
 * @see <a href=
 *      "https://www.microservice-api-patterns.org/patterns/responsibility/informationHolderEndpointTypes/ReferenceDataHolder">https://www.microservice-api-patterns.org/patterns/responsibility/informationHolderEndpointTypes/ReferenceDataHolder</a>
 */
@RestController
@RequestMapping("/cities")
public class CityReferenceDataHolder {
	@Autowired
	private CustomerCoreRemoteProxy customerCoreRemoteProxy;

	@Operation(summary = "Get the cities for a particular postal code.")
	@GetMapping(value = "/{postalCode}")
	public ResponseEntity<CitiesResponseDto> getCitiesForPostalCode(
			@Parameter(description = "the postal code", required = true) @PathVariable String postalCode) {

		return customerCoreRemoteProxy.getCitiesForPostalCode(postalCode);
	}
}


import java.util.Date;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import com.lakesidemutual.customerselfservice.domain.insurancequoterequest.InsuranceQuoteEntity;
import com.lakesidemutual.customerselfservice.domain.insurancequoterequest.InsuranceQuoteRequestAggregateRoot;
import com.lakesidemutual.customerselfservice.domain.insurancequoterequest.InsuranceQuoteResponseEvent;
import com.lakesidemutual.customerselfservice.domain.insurancequoterequest.MoneyAmount;
import com.lakesidemutual.customerselfservice.infrastructure.InsuranceQuoteRequestRepository;

/**
 * InsuranceQuoteResponseMessageConsumer is a Spring component that consumes InsuranceQuoteResponseEvents
 * as they arrive through the ActiveMQ message queue. It processes these events by updating the status
 * of the corresponding insurance quote requests.
 * */
@Component
public class InsuranceQuoteResponseMessageConsumer {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private InsuranceQuoteRequestRepository insuranceQuoteRequestRepository;

	@JmsListener(destination = "${insuranceQuoteResponseEvent.queueName}")
	public void receiveInsuranceQuoteResponse(final Message<InsuranceQuoteResponseEvent> message) {
		logger.info("A new InsuranceQuoteResponseEvent has been received.");
		final InsuranceQuoteResponseEvent insuranceQuoteResponseEvent = message.getPayload();
		final Long id = insuranceQuoteResponseEvent.getInsuranceQuoteRequestId();
		final Optional<InsuranceQuoteRequestAggregateRoot> insuranceQuoteRequestOpt = insuranceQuoteRequestRepository.findById(id);

		if(!insuranceQuoteRequestOpt.isPresent()) {
			logger.error("Unable to process an insurance quote response event with an invalid insurance quote request id.");
			return;
		}

		final Date date = insuranceQuoteResponseEvent.getDate();
		final InsuranceQuoteRequestAggregateRoot insuranceQuoteRequest = insuranceQuoteRequestOpt.get();
		if(insuranceQuoteResponseEvent.isRequestAccepted()) {
			logger.info("The insurance quote request " + insuranceQuoteRequest.getId() + " has been accepted.");
			Date expirationDate = insuranceQuoteResponseEvent.getExpirationDate();
			MoneyAmount insurancePremium = insuranceQuoteResponseEvent.getInsurancePremium().toDomainObject();
			MoneyAmount policyLimit = insuranceQuoteResponseEvent.getPolicyLimit().toDomainObject();
			InsuranceQuoteEntity insuranceQuote = new InsuranceQuoteEntity(expirationDate, insurancePremium, policyLimit);
			insuranceQuoteRequest.acceptRequest(insuranceQuote, date);
		} else {
			logger.info("The insurance quote request " + insuranceQuoteRequest.getId() + " has been rejected.");
			insuranceQuoteRequest.rejectRequest(date);
		}

		insuranceQuoteRequestRepository.save(insuranceQuoteRequest);
	}
}


import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.web.servlet.error.AbstractErrorController;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * This class implements a custom error controller that returns an <a href=
 * "https://www.microservice-api-patterns.org/patterns/quality/qualityManagementAndGovernance/ErrorReport">Error
 * Report</a>.
 */
@Controller
public class ErrorController extends AbstractErrorController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	public ErrorController(ErrorAttributes errorAttributes) {
		super(errorAttributes);
	}

	@RequestMapping(value = "/error", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Map<String, Object> handleError(HttpServletRequest request) {
		Map<String, Object> errorAttributes = super.getErrorAttributes(request, ErrorAttributeOptions.defaults());

		Object path = errorAttributes.get("path");
		Object status = errorAttributes.get("status");
		Object error = errorAttributes.get("error");
		Object message = errorAttributes.get("message");

		logger.info("An error occurred while accessing {}: {} {}, {}", path, status, error, message);

		return errorAttributes;
	}
}

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lakesidemutual.customerselfservice.domain.customer.CustomerId;
import com.lakesidemutual.customerselfservice.domain.identityaccess.UserLoginEntity;
import com.lakesidemutual.customerselfservice.domain.insurancequoterequest.InsuranceQuoteRequestAggregateRoot;
import com.lakesidemutual.customerselfservice.infrastructure.CustomerCoreRemoteProxy;
import com.lakesidemutual.customerselfservice.infrastructure.InsuranceQuoteRequestRepository;
import com.lakesidemutual.customerselfservice.infrastructure.UserLoginRepository;
import com.lakesidemutual.customerselfservice.interfaces.dtos.customer.AddressDto;
import com.lakesidemutual.customerselfservice.interfaces.dtos.customer.CustomerDto;
import com.lakesidemutual.customerselfservice.interfaces.dtos.customer.CustomerNotFoundException;
import com.lakesidemutual.customerselfservice.interfaces.dtos.customer.CustomerProfileUpdateRequestDto;
import com.lakesidemutual.customerselfservice.interfaces.dtos.customer.CustomerRegistrationRequestDto;
import com.lakesidemutual.customerselfservice.interfaces.dtos.insurancequoterequest.InsuranceQuoteRequestDto;

/**
 * This REST controller gives clients access to the customer data. It is an example of the
 * <i>Information Holder Resource</i> pattern. This particular one is a special type of information holder called <i>Master Data Holder</i>.
 *
 * @see <a href="https://www.microservice-api-patterns.org/patterns/responsibility/endpointRoles/InformationHolderResource">Information Holder Resource</a>
 * @see <a href="https://www.microservice-api-patterns.org/patterns/responsibility/informationHolderEndpointTypes/MasterDataHolder">Master Data Holder</a>
 */
@RestController
@RequestMapping("/customers")
public class CustomerInformationHolder {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private UserLoginRepository userLoginRepository;

	@Autowired
	private InsuranceQuoteRequestRepository insuranceQuoteRequestRepository;

	@Autowired
	private CustomerCoreRemoteProxy customerCoreRemoteProxy;

	@Operation(summary = "Change a customer's address.")
	@PreAuthorize("isAuthenticated()")
	@PutMapping(value = "/{customerId}/address")
	public ResponseEntity<AddressDto> changeAddress(
			@Parameter(description = "the customer's unique id", required = true) @PathVariable CustomerId customerId,
			@Parameter(description = "the customer's new address", required = true) @Valid @RequestBody AddressDto requestDto) {
		return customerCoreRemoteProxy.changeAddress(customerId, requestDto);
	}

	@Operation(summary = "Get customer with a given customer id.")
	@PreAuthorize("isAuthenticated()")
	@GetMapping(value = "/{customerId}")
	public ResponseEntity<CustomerDto> getCustomer(
			Authentication authentication,
			@Parameter(description = "the customer's unique id", required = true) @PathVariable CustomerId customerId) {

		CustomerDto customer = customerCoreRemoteProxy.getCustomer(customerId);
		if(customer == null) {
			final String errorMessage = "Failed to find a customer with id '" + customerId.getId() + "'.";
			logger.info(errorMessage);
			throw new CustomerNotFoundException(errorMessage);
		}

		addHATEOASLinks(customer);
		return ResponseEntity.ok(customer);
	}

	@Operation(summary = "Complete the registration of a new customer.")
	@PreAuthorize("isAuthenticated()")
	@PostMapping
	public ResponseEntity<CustomerDto> registerCustomer(
			Authentication authentication,
			@Parameter(description = "the customer's profile information", required = true) @Valid @RequestBody CustomerRegistrationRequestDto requestDto) {
		String loggedInUserEmail = authentication.getName();
		CustomerProfileUpdateRequestDto dto = new CustomerProfileUpdateRequestDto(
				requestDto.getFirstname(), requestDto.getLastname(), requestDto.getBirthday(), requestDto.getStreetAddress(),
				requestDto.getPostalCode(), requestDto.getCity(), loggedInUserEmail, requestDto.getPhoneNumber());
		CustomerDto customer = customerCoreRemoteProxy.createCustomer(dto);
		UserLoginEntity loggedInUser = userLoginRepository.findByEmail(loggedInUserEmail);
		loggedInUser.setCustomerId(new CustomerId(customer.getCustomerId()));
		userLoginRepository.save(loggedInUser);

		addHATEOASLinks(customer);
		return ResponseEntity.ok(customer);
	}

	@Operation(summary = "Get a customer's insurance quote requests.")
	@PreAuthorize("isAuthenticated()")
	@GetMapping(value = "/{customerId}/insurance-quote-requests")
	public ResponseEntity<List<InsuranceQuoteRequestDto>> getInsuranceQuoteRequests(
			@Parameter(description = "the customer's unique id", required = true) @PathVariable CustomerId customerId) {
		List<InsuranceQuoteRequestAggregateRoot> insuranceQuoteRequests = insuranceQuoteRequestRepository.findByCustomerInfo_CustomerIdOrderByDateDesc(customerId);
		List<InsuranceQuoteRequestDto> insuranceQuoteRequestDtos = insuranceQuoteRequests.stream().map(InsuranceQuoteRequestDto::fromDomainObject).collect(Collectors.toList());
		return ResponseEntity.ok(insuranceQuoteRequestDtos);
	}

	private void addHATEOASLinks(CustomerDto customerDto) {
		CustomerId customerId = new CustomerId(customerDto.getCustomerId());
		Link selfLink = linkTo(methodOn(CustomerInformationHolder.class).getCustomer(null, customerId))
				.withSelfRel();
		Link updateAddressLink = linkTo(methodOn(CustomerInformationHolder.class).changeAddress(customerId, null))
				.withRel("address.change");
		// When getting the DTO from the proxy, it already contains links
		// pointing to the customer-core, so we first remove them ...
		customerDto.removeLinks();
		// and add our own:
		customerDto.add(selfLink);
		customerDto.add(updateAddressLink);
	}
}


import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lakesidemutual.customerselfservice.domain.customer.CustomerId;
import com.lakesidemutual.customerselfservice.domain.identityaccess.UserLoginEntity;
import com.lakesidemutual.customerselfservice.domain.insurancequoterequest.CustomerInfoEntity;
import com.lakesidemutual.customerselfservice.domain.insurancequoterequest.InsuranceOptionsEntity;
import com.lakesidemutual.customerselfservice.domain.insurancequoterequest.InsuranceQuoteRequestAggregateRoot;
import com.lakesidemutual.customerselfservice.domain.insurancequoterequest.RequestStatus;
import com.lakesidemutual.customerselfservice.infrastructure.InsuranceQuoteRequestRepository;
import com.lakesidemutual.customerselfservice.infrastructure.PolicyManagementMessageProducer;
import com.lakesidemutual.customerselfservice.infrastructure.UserLoginRepository;
import com.lakesidemutual.customerselfservice.interfaces.dtos.insurancequoterequest.CustomerInfoDto;
import com.lakesidemutual.customerselfservice.interfaces.dtos.insurancequoterequest.InsuranceQuoteRequestDto;
import com.lakesidemutual.customerselfservice.interfaces.dtos.insurancequoterequest.InsuranceQuoteRequestNotFoundException;
import com.lakesidemutual.customerselfservice.interfaces.dtos.insurancequoterequest.InsuranceQuoteResponseDto;


/**
 * This REST controller gives clients access to the insurance quote requests. It is an example of the
 * <i>Information Holder Resource</i> pattern. This particular one is a special type of information holder called <i>Operational Data Holder</i>.
 *
 * @see <a href="https://www.microservice-api-patterns.org/patterns/responsibility/informationHolderEndpointTypes/OperationalDataHolder">Operational Data Holder</a>
 * 
 * Requests are created and updated here too, so it also is a Processing Resource:
 * 
 *  * @see <a href="https://www.microservice-api-patterns.org/patterns/responsibility/endpointRoles/ProcessingResource">Processing Resource</a>
 *  
 *Matching RDD role stereotypes are <i>Coordinator</i> and <i>Information Holder</i>:
 *
 *  * @see <a href="http://www.wirfs-brock.com/PDFs/A_Brief-Tour-of-RDD.pdf">A Brief Tour of RDD</a>
 */
@RestController
@RequestMapping("/insurance-quote-requests")
public class InsuranceQuoteRequestCoordinator {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private InsuranceQuoteRequestRepository insuranceQuoteRequestRepository;

	@Autowired
	private UserLoginRepository userLoginRepository;

	@Autowired
	private PolicyManagementMessageProducer policyManagementMessageProducer;

	/**
	 * This endpoint is only used for debugging purposes.
	 * */
	@Operation(summary = "Get all Insurance Quote Requests.")
	@GetMapping /* MAP: Retrieval Operation */ 
	public ResponseEntity<List<InsuranceQuoteRequestDto>> getInsuranceQuoteRequests() {
		List<InsuranceQuoteRequestAggregateRoot> quoteRequests = insuranceQuoteRequestRepository.findAllByOrderByDateDesc();
		List<InsuranceQuoteRequestDto> quoteRequestDtos = quoteRequests.stream().map(InsuranceQuoteRequestDto::fromDomainObject).collect(Collectors.toList());
		return ResponseEntity.ok(quoteRequestDtos);
	}

	@Operation(summary = "Get a specific Insurance Quote Request.")
	@PreAuthorize("isAuthenticated()")
	@GetMapping(value = "/{insuranceQuoteRequestId}") /* MAP: Retrieval Operation */ 
	public ResponseEntity<InsuranceQuoteRequestDto> getInsuranceQuoteRequest(
			Authentication authentication,
			@Parameter(description = "the insurance quote request's unique id", required = true) @PathVariable Long insuranceQuoteRequestId) {
		Optional<InsuranceQuoteRequestAggregateRoot> optInsuranceQuoteRequest = insuranceQuoteRequestRepository.findById(insuranceQuoteRequestId);
		if(!optInsuranceQuoteRequest.isPresent()) {
			final String errorMessage = "Failed to find an insurance quote request with id '" + insuranceQuoteRequestId + "'.";
			logger.info(errorMessage);
			throw new InsuranceQuoteRequestNotFoundException(errorMessage);
		}

		InsuranceQuoteRequestAggregateRoot insuranceQuoteRequest = optInsuranceQuoteRequest.get();
		CustomerId loggedInCustomerId = userLoginRepository.findByEmail(authentication.getName()).getCustomerId();
		if (!insuranceQuoteRequest.getCustomerInfo().getCustomerId().equals(loggedInCustomerId)) {
			logger.info("Can't access an Insurance Quote Request of a different customer.");
			return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
		}

		return ResponseEntity.ok(InsuranceQuoteRequestDto.fromDomainObject(insuranceQuoteRequest));
	}

	@Operation(summary = "Create a new Insurance Quote Request.")
	@PreAuthorize("isAuthenticated()")
	@PostMapping /* MAP: State Creation Operation */ 
	public ResponseEntity<InsuranceQuoteRequestDto> createInsuranceQuoteRequest(
			Authentication authentication,
			@Parameter(description = "the insurance quote request", required = true) @Valid @RequestBody InsuranceQuoteRequestDto requestDto) {
		String loggedInUserEmail = authentication.getName();
		UserLoginEntity loggedInUser = userLoginRepository.findByEmail(loggedInUserEmail);
		CustomerId loggedInCustomerId = loggedInUser.getCustomerId();
		if (loggedInCustomerId == null) {
			logger.info("Customer needs to complete registration first.");
			return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
		}

		CustomerInfoDto customerInfoDto = requestDto.getCustomerInfo();
		CustomerId customerId = new CustomerId(customerInfoDto.getCustomerId());

		if (!customerId.equals(loggedInCustomerId)) {
			logger.info("Can't create an Insurance Quote Request for a different customer.");
			return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
		}

		CustomerInfoEntity customerInfoEntity = customerInfoDto.toDomainObject();
		InsuranceOptionsEntity insuranceOptionsEntity = requestDto.getInsuranceOptions().toDomainObject();

		final Date date = new Date();
		InsuranceQuoteRequestAggregateRoot insuranceQuoteRequest = new InsuranceQuoteRequestAggregateRoot(date, RequestStatus.REQUEST_SUBMITTED, customerInfoEntity, insuranceOptionsEntity, null, null);
		insuranceQuoteRequestRepository.save(insuranceQuoteRequest);
		InsuranceQuoteRequestDto responseDto = InsuranceQuoteRequestDto.fromDomainObject(insuranceQuoteRequest);

		policyManagementMessageProducer.sendInsuranceQuoteRequest(date, responseDto);

		return ResponseEntity.ok(responseDto);
	}

	@Operation(summary = "Updates the status of an existing Insurance Quote Request")
	@PreAuthorize("isAuthenticated()")
	@PatchMapping(value = "/{id}") /* MAP: State Transition Operation */ 
	public ResponseEntity<InsuranceQuoteRequestDto> respondToInsuranceQuote(
			Authentication authentication,
			@Parameter(description = "the insurance quote request's unique id", required = true) @PathVariable Long id,
			@Parameter(description = "the response that contains the customer's decision whether to accept or reject an insurance quote", required = true)
			@Valid @RequestBody InsuranceQuoteResponseDto insuranceQuoteResponseDto) {
		String loggedInUserEmail = authentication.getName();
		UserLoginEntity loggedInUser = userLoginRepository.findByEmail(loggedInUserEmail);
		CustomerId loggedInCustomerId = loggedInUser.getCustomerId();
		if (loggedInCustomerId == null) {
			logger.info("Customer needs to complete registration first.");
			return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
		}

		Optional<InsuranceQuoteRequestAggregateRoot> optInsuranceQuoteRequest = insuranceQuoteRequestRepository.findById(id);
		if (!optInsuranceQuoteRequest.isPresent()) {
			final String errorMessage = "Failed to respond to insurance quote, because there is no insurance quote request with id '" + id + "'.";
			logger.info(errorMessage);
			throw new InsuranceQuoteRequestNotFoundException(errorMessage);
		}

		final InsuranceQuoteRequestAggregateRoot insuranceQuoteRequest = optInsuranceQuoteRequest.get();
		CustomerId customerId = insuranceQuoteRequest.getCustomerInfo().getCustomerId();
		if (!customerId.equals(loggedInCustomerId)) {
			logger.info("Can't update an Insurance Quote Request of a different customer.");
			return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
		}

		final Date date = new Date();
		if(insuranceQuoteResponseDto.getStatus().equals(RequestStatus.QUOTE_ACCEPTED.toString())) {
			logger.info("Insurance Quote has been accepted.");
			insuranceQuoteRequest.acceptQuote(date);
			policyManagementMessageProducer.sendCustomerDecision(date, insuranceQuoteRequest.getId(), true);
		} else if(insuranceQuoteResponseDto.getStatus().equals(RequestStatus.QUOTE_REJECTED.toString())) {
			logger.info("Insurance Quote has been rejected.");
			insuranceQuoteRequest.rejectQuote(date);
			policyManagementMessageProducer.sendCustomerDecision(date, insuranceQuoteRequest.getId(), false);
		}
		insuranceQuoteRequestRepository.save(insuranceQuoteRequest);

		InsuranceQuoteRequestDto insuranceQuoteRequestDto = InsuranceQuoteRequestDto.fromDomainObject(insuranceQuoteRequest);
		return ResponseEntity.ok(insuranceQuoteRequestDto);
	}
}


import java.io.IOException;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

@Component
public class HeaderRequestInterceptor implements ClientHttpRequestInterceptor {
	@Value("${apikey.header}")
	private String apiKeyHeader;

	@Value("${apikey.value}")
	private String apiKeyValue;

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
			throws IOException {
		final HttpHeaders httpHeaders = request.getHeaders();
		httpHeaders.set(apiKeyHeader, "Bearer " + apiKeyValue);
		httpHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
		return execution.execute(request, body);
	}
}


import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;

/**
 * Provides a RestTemplate that can be injected into other components. The
 * RestTemplate uses the configured API Key when making a request.
 */
@Configuration
@Profile("default")
public class DefaultAuthenticatedRestTemplateClient {
	@Autowired
	private HeaderRequestInterceptor headerRequestInterceptor;

	@Bean
	public RestTemplate restTemplate() {
		System.out.println("loading default rest template");
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setInterceptors(Arrays.asList(headerRequestInterceptor));
		return restTemplate;
	}
}

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.function.Predicate;

/**
 * The SwaggerConfiguration class configures the HTTP resource API documentation
 * that is generated by Springfox.
 */
@Configuration
public class SwaggerConfiguration {

	@Bean
	public OpenAPI customerSelfServiceApi() {
		return new OpenAPI()
				.info(new Info().title("Customer Self-Service API")
						.description("This API allows customers of Lakeside Mutual to sign up, log in and manage their policies and user profile themselves.")
						.version("v1.0.0")
						.license(new License().name("Apache 2.0")));
	}
}


import jakarta.servlet.Filter;

import jakarta.servlet.Servlet;
import org.apache.catalina.servlets.DefaultServlet;
import org.h2.server.web.JakartaWebServlet;
import org.h2.server.web.WebServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * The WebConfiguration class is used to customize the default Spring MVC configuration.
 * */
@Configuration
public class WebConfiguration implements WebMvcConfigurer {

	@Autowired
	HandlerInterceptor rateLimitInterceptor;

	/**
	 * This method adds a custom interceptor to the InterceptorRegistry in order to enable rate-limiting.
	 * */
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(rateLimitInterceptor);
	}

	/**
	 * This web servlet makes the web console of the H2 database engine available at the "/console" endpoint.
	 * */
	@Bean
	public ServletRegistrationBean<JakartaWebServlet> h2servletRegistration() {
		ServletRegistrationBean<JakartaWebServlet> registrationBean = new ServletRegistrationBean<>(new JakartaWebServlet());
		registrationBean.addUrlMappings("/console/*");
		return registrationBean;
	}

	/**
	 * This is a filter that generates an ETag value based on the content of the response. This ETag is compared to the If-None-Match header
	 * of the request. If these headers are equal, the response content is not sent, but rather a 304 "Not Modified" status instead.
	 * */
	@Bean
	public Filter shallowETagHeaderFilter() {
		return new ShallowEtagHeaderFilter();
	}
}

import java.util.HashMap;
import java.util.Map;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

import com.lakesidemutual.customerselfservice.domain.insurancequoterequest.InsuranceQuoteExpiredEvent;
import com.lakesidemutual.customerselfservice.domain.insurancequoterequest.InsuranceQuoteResponseEvent;
import com.lakesidemutual.customerselfservice.domain.insurancequoterequest.PolicyCreatedEvent;

@Configuration
public class MessagingConfiguration {
	@Value("${policymanagement.tcpBrokerBindAddress}")
	private String brokerURL;

	@Value("${spring.activemq.user}")
	private String username;

	@Value("${spring.activemq.password}")
	private String password;

	@Bean
	public JmsTemplate jmsTemplate(){
		JmsTemplate template = new JmsTemplate();
		template.setMessageConverter(jacksonJmsMessageConverter());
		template.setConnectionFactory(connectionFactory());
		return template;
	}

	@Bean
	public ActiveMQConnectionFactory connectionFactory(){
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory();
		connectionFactory.setBrokerURL(brokerURL);
		connectionFactory.setUserName(username);
		connectionFactory.setPassword(password);
		connectionFactory.setTrustAllPackages(true);
		return connectionFactory;
	}

	@Bean
	public MessageConverter jacksonJmsMessageConverter() {
		MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
		converter.setTargetType(MessageType.TEXT);
		converter.setTypeIdPropertyName("_type");

		final Map<String, Class<?>> typeIdMappings = new HashMap<>();
		typeIdMappings.put("com.lakesidemutual.policymanagement.domain.insurancequoterequest.InsuranceQuoteResponseEvent", InsuranceQuoteResponseEvent.class);
		typeIdMappings.put("com.lakesidemutual.policymanagement.domain.insurancequoterequest.InsuranceQuoteExpiredEvent", InsuranceQuoteExpiredEvent.class);
		typeIdMappings.put("com.lakesidemutual.policymanagement.domain.insurancequoterequest.PolicyCreatedEvent", PolicyCreatedEvent.class);
		converter.setTypeIdMappings(typeIdMappings);
		return converter;
	}

	@Bean
	public DefaultJmsListenerContainerFactory jmsListenerContainerFactory() {
		DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
		factory.setConnectionFactory(connectionFactory());
		factory.setConcurrency("1-1");
		factory.setMessageConverter(jacksonJmsMessageConverter());
		return factory;
	}
}

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

/**
 * The AuthenticationTokenFilter class ensures that only requests with a valid JWT (JSON Web Token) get through.
 * */
public class AuthenticationTokenFilter extends UsernamePasswordAuthenticationFilter {

	@Value("${token.header}")
	private String tokenHeader;

	@Autowired
	private JwtUtils tokenUtils;

	@Autowired
	private UserDetailsService userDetailsService;

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		HttpServletRequest httpRequest = (HttpServletRequest) request;
		String authToken = httpRequest.getHeader(tokenHeader);
		String username = this.tokenUtils.getUsernameFromToken(authToken);

		if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
			UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
			if (userDetails != null && this.tokenUtils.validateToken(authToken, userDetails)) {
				UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
						userDetails, null, userDetails.getAuthorities());
				authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(httpRequest));
				SecurityContextHolder.getContext().setAuthentication(authentication);
			}
		}

		chain.doFilter(request, response);
	}
}


import com.lakesidemutual.customerselfservice.domain.identityaccess.UserSecurityDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JwtUtils is a utility class that lets clients generate, validate and parse JSON Web Tokens (JWT).
 */
@Component
public class JwtUtils {

    private final SecretKey secret = Jwts.SIG.HS512.key().build();

    @Value("${token.expiration}")
    private int expirationInSeconds = 3600 * 24 * 7;

    public String getUsernameFromToken(String token) {
        String username;
        try {
            final Claims claims = this.getClaimsFromToken(token);
            username = claims.getSubject();
        } catch (Exception e) {
            username = null;
        }
        return username;
    }

    public Date getCreatedDateFromToken(String token) {
        Date created;
        try {
            final Claims claims = this.getClaimsFromToken(token);
            created = new Date((Long) claims.get("created"));
        } catch (Exception e) {
            created = null;
        }
        return created;
    }

    public Date getExpirationDateFromToken(String token) {
        Date expiration;
        try {
            final Claims claims = this.getClaimsFromToken(token);
            expiration = claims.getExpiration();
        } catch (Exception e) {
            expiration = null;
        }
        return expiration;
    }

    public String getAudienceFromToken(String token) {
        String audience;
        try {
            final Claims claims = this.getClaimsFromToken(token);
            audience = (String) claims.get("audience");
        } catch (Exception e) {
            audience = null;
        }
        return audience;
    }

    private Claims getClaimsFromToken(String token) {
        Claims claims;
        try {
            claims = Jwts.parser().verifyWith(this.secret).build().parseSignedClaims(token).getPayload();
        } catch (Exception e) {
            claims = null;
        }
        return claims;
    }

    private Date generateCurrentDate() {
        return new Date(System.currentTimeMillis());
    }

    private Date generateExpirationDate() {
        return new Date(System.currentTimeMillis() + this.expirationInSeconds * 1000);
    }

    private Boolean isTokenExpired(String token) {
        final Date expiration = this.getExpirationDateFromToken(token);
        return expiration.before(this.generateCurrentDate());
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<String, Object>();
        claims.put("sub", userDetails.getUsername());
        claims.put("created", this.generateCurrentDate());
        return this.generateToken(claims);
    }

    private String generateToken(Map<String, Object> claims) {
        return Jwts.builder()
                .claims(claims)
                .expiration(this.generateExpirationDate())
                .signWith(this.secret)
                .compact();
    }

    public String refreshToken(String token) {
        String refreshedToken;
        try {
            final Claims claims = this.getClaimsFromToken(token);
            claims.put("created", this.generateCurrentDate());
            refreshedToken = this.generateToken(claims);
        } catch (Exception e) {
            refreshedToken = null;
        }
        return refreshedToken;
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        UserSecurityDetails user = (UserSecurityDetails) userDetails;
        final String username = this.getUsernameFromToken(token);
        return username.equals(user.getUsername()) && !(this.isTokenExpired(token));
    }

}


import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * This class defines that the response to unauthorized requests should be HTTP 401 Unauthorized.
 */
@Component
public class UnauthorizedHandler implements AuthenticationEntryPoint {

	@Override
	public void commence(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
			AuthenticationException e) throws IOException, ServletException {
		httpServletResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Access Denied");
	}
}


import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

/**
 * The WebSecurityConfiguration class configures the security policies used for the exposed HTTP resource API.
 * In this case, it ensures that only authenticated users can access the API.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfiguration {
    private static final String[] AUTH_WHITELIST = {
            // -- Swagger UI v3 (OpenAPI)
            "/v3/api-docs/**",
            "/swagger-ui/**",
            // spring-boot-starter-actuator health checks and other info
            "/actuator/**",
            "/actuator",
            // Other whitelisted endpoints,
            "/customers",
            "/customers/**",
            "/cities",
            "/cities/**",
            "/insurance-quote-requests",
            "/insurance-quote-requests/**",
            "/auth/**",
            "/console/**"
    };

    @Autowired
    private UnauthorizedHandler unauthorizedHandler;

    @Autowired
    private UserDetailsService userDetailsService;

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder
                .userDetailsService(userDetailsService)
                .passwordEncoder(new BCryptPasswordEncoder());
        return authenticationManagerBuilder.build();
    }

    @Bean
    public AuthenticationTokenFilter authenticationTokenFilterBean(HttpSecurity http) throws Exception {
        AuthenticationTokenFilter authenticationTokenFilter = new AuthenticationTokenFilter();
        authenticationTokenFilter.setAuthenticationManager(authenticationManager(http));
        return authenticationTokenFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)
                        .cacheControl(HeadersConfigurer.CacheControlConfig::disable)
                )
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((request, response, exception) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"))
                        .accessDeniedHandler((request, response, exception) -> response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden"))
                )
                .sessionManagement(sessionManagement -> sessionManagement
                        .sessionCreationPolicy(STATELESS)
                )
                .authorizeHttpRequests(authorizationManagerRequestMatcherRegistry ->
                        authorizationManagerRequestMatcherRegistry
                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                .requestMatchers(AUTH_WHITELIST).permitAll()
                                .anyRequest().authenticated()
                )
                .addFilterBefore(authenticationTokenFilterBean(http), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("authorization", "content-type", "x-auth-token"));
        configuration.setExposedHeaders(Arrays.asList("x-auth-token"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}


import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import es.moki.ratelimitj.core.limiter.request.RequestLimitRule;
import es.moki.ratelimitj.core.limiter.request.RequestRateLimiter;
import es.moki.ratelimitj.inmemory.request.InMemorySlidingWindowRequestRateLimiter;

/**
 * RateLimitInterceptor ensures that client can't overuse the service. What
 * follows is a simple implementation that limits clients to 60 requests per
 * minute. Clients are identified by their IP address. Once the limit is
 * reached, a {@code 429 Too Many Requests} response is returned.
 *
 * To test the limit, the Apache Bench tool ab can be used:
 *
 * <pre>
 * {@code
 *
 * ab -n 100  http://localhost:8080/customers
 *
 * }
 * </pre>
 *
 * In the output you should see that all 100 requests were answered but 40 had a
 * Non-2xx response:
 *
 * <pre>
 * {@code
 *
 * Complete requests:      100
 * Failed requests:        40
 * Non-2xx responses:      40
 *
 * }
 * </pre>
 *
 * Note that the Rate Limiting library uses an in-memory store for the rate
 * limits, so you can simply restart the server to reset the limit.
 *
 * This interceptor is configured in {@link WebConfiguration} to intercept all
 * requests to this application.
 *
 * @see <a href=
 *      "https://www.microservice-api-patterns.org/patterns/quality/qualityManagementAndGovernance/RateLimit">https://www.microservice-api-patterns.org/patterns/quality/qualityManagementAndGovernance/RateLimit</a>
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	private RequestRateLimiter requestRateLimiter;

	private int requestsPerMinute;

	@Autowired
	public RateLimitInterceptor(@Value("${rate.limit.perMinute}") int requestsPerMinute) {
		this.requestsPerMinute = requestsPerMinute;
		Set<RequestLimitRule> rules = Collections
				.singleton(RequestLimitRule.of(Duration.of( 1, ChronoUnit.MINUTES), requestsPerMinute));
		requestRateLimiter = new InMemorySlidingWindowRequestRateLimiter(rules);
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object object) throws Exception {

		String clientRemoteAddr = request.getRemoteAddr();

		boolean overLimit = requestRateLimiter.overLimitWhenIncremented(clientRemoteAddr);

		if (overLimit) {
			logger.info("Client " + clientRemoteAddr + " has been rate limited.");
			response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
		}
		response.addHeader("X-RateLimit-Limit", String.valueOf(requestsPerMinute));
		return !overLimit;
	}

}

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * This @interface declaration defines a custom @PhoneNumber annotation which
 * can be used to validate phone numbers in a request DTO. Note that the
 * PhoneNumberValidator class performs the actual validation.
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PhoneNumberValidator.class)
public @interface PhoneNumber {
	String message() default "must be a valid Swiss phone number (e.g, +4155 222 41 11)";
	Class<?>[] groups() default {};
	Class<? extends Payload>[] payload() default {};
}


import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

/**
 * This class validates phone numbers using Google's libphonenumber (see https://github.com/googlei18n/libphonenumber).
 * */
public class PhoneNumberValidator implements ConstraintValidator<PhoneNumber, String> {
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	@Override
	public boolean isValid(String phoneNumberStr, ConstraintValidatorContext context) {
		PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
		try {
			Phonenumber.PhoneNumber phoneNumber = phoneUtil.parse(phoneNumberStr, "CH");
			return phoneUtil.isValidNumber(phoneNumber);
		} catch (NumberParseException e) {
			logger.info("'" + phoneNumberStr + "' is not a valid phone number.", e);
			return false;
		}
	}
}


import java.util.List;

/**
 * The CitiesResponseDto represents a list of city names, transferred as simple
 * (atomic) strings. This is an example of the <a href=
 * "https://www.microservice-api-patterns.org/patterns/structure/representationElements/AtomicParameter">Atomic
 * Parameter</a> pattern.
 */
public class CitiesResponseDto {
	private List<String> cities;

	public CitiesResponseDto() {
	}

	public CitiesResponseDto(List<String> cities) {
		this.cities = cities;
	}

	public List<String> getCities() {
		return cities;
	}

	public void setCities(List<String> cities) {
		this.cities = cities;
	}
}

import java.util.Date;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lakesidemutual.customerselfservice.domain.insurancequoterequest.InsuranceOptionsEntity;
import com.lakesidemutual.customerselfservice.domain.insurancequoterequest.InsuranceType;

/**
 * InsuranceOptionsDto is a data transfer object (DTO) that contains the insurance options
 * (e.g., start date, insurance type, etc.) that a customer selected for an Insurance Quote Request.
 */
public class InsuranceOptionsDto {
	@Valid
	@NotNull
	@JsonFormat(pattern = "yyyy-MM-dd")
	private Date startDate;

	@NotEmpty
	private String insuranceType;

	@Valid
	@NotNull
	private MoneyAmountDto deductible;

	public InsuranceOptionsDto() {
	}

	private InsuranceOptionsDto(Date startDate, String insuranceType, MoneyAmountDto deductible) {
		this.startDate = startDate;
		this.insuranceType = insuranceType;
		this.deductible = deductible;
	}

	public static InsuranceOptionsDto fromDomainObject(InsuranceOptionsEntity insuranceOptions) {
		Date startDate = insuranceOptions.getStartDate();
		InsuranceType insuranceType = insuranceOptions.getInsuranceType();
		String insuranceTypeDto = insuranceType.getName();
		MoneyAmountDto deductibleDto = MoneyAmountDto.fromDomainObject(insuranceOptions.getDeductible());
		return new InsuranceOptionsDto(startDate, insuranceTypeDto, deductibleDto);
	}

	public InsuranceOptionsEntity toDomainObject() {
		return new InsuranceOptionsEntity(startDate, new InsuranceType(insuranceType), deductible.toDomainObject());
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public String getInsuranceType() {
		return insuranceType;
	}

	public void setInsuranceType(String insuranceType) {
		this.insuranceType = insuranceType;
	}

	public MoneyAmountDto getDeductible() {
		return deductible;
	}

	public void setDeductible(MoneyAmountDto deductible) {
		this.deductible = deductible;
	}
}


import java.math.BigDecimal;
import java.util.Currency;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.lakesidemutual.customerselfservice.domain.insurancequoterequest.MoneyAmount;

/**
 * MoneyAmountDto is a data transfer object (DTO) that represents an amount of money in a specific currency.
 */
public class MoneyAmountDto {
	@NotNull
	@DecimalMax(value = "1000000000000", inclusive = false)
	@DecimalMin("0")
	private BigDecimal amount;

	@NotEmpty
	private String currency;

	public MoneyAmountDto() {
	}

	private MoneyAmountDto(BigDecimal amount, String currency) {
		this.amount = amount;
		this.currency = currency;
	}

	public static MoneyAmountDto fromDomainObject(MoneyAmount moneyAmount) {
		return new MoneyAmountDto(moneyAmount.getAmount(), moneyAmount.getCurrency().toString());
	}

	public MoneyAmount toDomainObject() {
		return new MoneyAmount(amount, Currency.getInstance(currency));
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public String getCurrency() {
		return currency;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}
}


import java.util.Date;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

/**
 * InsuranceQuoteResponseDto is a data transfer object (DTO) that contains Lakeside Mutual's
 * response to a specific insurance quote request.
 * */
public class InsuranceQuoteResponseDto {
	@NotEmpty
	private String status;

	@Valid
	private Date expirationDate;

	@Valid
	private MoneyAmountDto insurancePremium;

	@Valid
	private MoneyAmountDto policyLimit;

	public InsuranceQuoteResponseDto() {
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Date getExpirationDate() {
		return expirationDate;
	}

	public void setExpirationDate(Date expirationDate) {
		this.expirationDate = expirationDate;
	}

	public MoneyAmountDto getInsurancePremium() {
		return insurancePremium;
	}

	public void setInsurancePremium(MoneyAmountDto insurancePremium) {
		this.insurancePremium = insurancePremium;
	}

	public MoneyAmountDto getPolicyLimit() {
		return policyLimit;
	}

	public void setPolicyLimit(MoneyAmountDto policyLimit) {
		this.policyLimit = policyLimit;
	}
}

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.lakesidemutual.customerselfservice.domain.insurancequoterequest.InsuranceQuoteRequestAggregateRoot;

/**
 * InsuranceQuoteRequestDto is a data transfer object (DTO) that represents a request
 * by a customer for a new insurance quote.
 */
public class InsuranceQuoteRequestDto {
	private Long id;

	@Valid
	private Date date;

	@Valid
	private List<RequestStatusChangeDto> statusHistory;

	@Valid
	@NotNull
	private CustomerInfoDto customerInfo;

	@Valid
	@NotNull
	private InsuranceOptionsDto insuranceOptions;

	@Valid
	private InsuranceQuoteDto insuranceQuote;

	private String policyId;

	public InsuranceQuoteRequestDto() {
	}

	public InsuranceQuoteRequestDto(Long id, Date date, List<RequestStatusChangeDto> statusHistory, CustomerInfoDto customerInfo, InsuranceOptionsDto insuranceOptions, InsuranceQuoteDto insuranceQuote, String policyId) {
		this.id = id;
		this.date = date;
		this.statusHistory = statusHistory;
		this.customerInfo = customerInfo;
		this.insuranceOptions = insuranceOptions;
		this.insuranceQuote = insuranceQuote;
		this.policyId = policyId;
	}

	public static InsuranceQuoteRequestDto fromDomainObject(InsuranceQuoteRequestAggregateRoot insuranceQuoteRequest) {
		Long id = insuranceQuoteRequest.getId();
		Date date = insuranceQuoteRequest.getDate();
		List<RequestStatusChangeDto> statusHistory = insuranceQuoteRequest.getStatusHistory().stream()
				.map(RequestStatusChangeDto::fromDomainObject)
				.collect(Collectors.toList());
		CustomerInfoDto customerInfoDto = CustomerInfoDto.fromDomainObject(insuranceQuoteRequest.getCustomerInfo());
		InsuranceOptionsDto insuranceOptionsDto = InsuranceOptionsDto.fromDomainObject(insuranceQuoteRequest.getInsuranceOptions());
		InsuranceQuoteDto insuranceQuoteDto = insuranceQuoteRequest.getInsuranceQuote() != null ? InsuranceQuoteDto.fromDomainObject(insuranceQuoteRequest.getInsuranceQuote()) : null;
		String policyId = insuranceQuoteRequest.getPolicyId();
		return new InsuranceQuoteRequestDto(id, date, statusHistory, customerInfoDto, insuranceOptionsDto, insuranceQuoteDto, policyId);
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public List<RequestStatusChangeDto> getStatusHistory() {
		return statusHistory;
	}

	public void setStatusHistory(List<RequestStatusChangeDto> statusHistory) {
		this.statusHistory = statusHistory;
	}

	public CustomerInfoDto getCustomerInfo() {
		return customerInfo;
	}

	public void setCustomerInfo(CustomerInfoDto customerInfo) {
		this.customerInfo = customerInfo;
	}

	public InsuranceOptionsDto getInsuranceOptions() {
		return insuranceOptions;
	}

	public void setInsuranceOptions(InsuranceOptionsDto insuranceOptions) {
		this.insuranceOptions = insuranceOptions;
	}

	public InsuranceQuoteDto getInsuranceQuote() {
		return insuranceQuote;
	}

	public void setInsuranceQuote(InsuranceQuoteDto insuranceQuote) {
		this.insuranceQuote = insuranceQuote;
	}

	public String getPolicyId() {
		return policyId;
	}

	public void setPolicyId(String policyId) {
		this.policyId = policyId;
	}
}


import java.util.Date;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import com.lakesidemutual.customerselfservice.domain.insurancequoterequest.RequestStatusChange;

/**
 * RequestStatusChangeDto is a data transfer object (DTO) that represents a status change of an insurance quote request.
 */
public class RequestStatusChangeDto {
	@Valid
	private Date date;

	@NotEmpty
	private String status;

	public RequestStatusChangeDto() {
	}

	public RequestStatusChangeDto(Date date, String status) {
		this.date = date;
		this.status = status;
	}

	public static RequestStatusChangeDto fromDomainObject(RequestStatusChange requestStatusChange) {
		return new RequestStatusChangeDto(requestStatusChange.getDate(), requestStatusChange.getStatus().name());
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.lakesidemutual.customerselfservice.domain.customer.CustomerId;
import com.lakesidemutual.customerselfservice.domain.insurancequoterequest.CustomerInfoEntity;
import com.lakesidemutual.customerselfservice.interfaces.dtos.customer.AddressDto;

/**
 * CustomerInfoDto is a data transfer object (DTO) that represents the
 * customer infos that are part of an Insurance Quote Request.
 * */
public class CustomerInfoDto {
	@NotEmpty
	private String customerId;

	@NotEmpty
	private String firstname;

	@NotEmpty
	private String lastname;

	@Valid
	@NotNull
	private AddressDto contactAddress;

	@Valid
	@NotNull
	private AddressDto billingAddress;

	public CustomerInfoDto() {
	}

	private CustomerInfoDto(String customerId, String firstname, String lastname, AddressDto contactAddress, AddressDto billingAddress) {
		this.customerId = customerId;
		this.firstname = firstname;
		this.lastname = lastname;
		this.contactAddress = contactAddress;
		this.billingAddress = billingAddress;
	}

	public static CustomerInfoDto fromDomainObject(CustomerInfoEntity customerInfo) {
		String customerId = customerInfo.getCustomerId().getId();
		String firstname = customerInfo.getFirstname();
		String lastname = customerInfo.getLastname();
		AddressDto contactAddressDto = AddressDto.fromDomainObject(customerInfo.getContactAddress());
		AddressDto billingAddressDto = AddressDto.fromDomainObject(customerInfo.getBillingAddress());
		return new CustomerInfoDto(customerId, firstname, lastname, contactAddressDto, billingAddressDto);
	}

	public CustomerInfoEntity toDomainObject() {
		return new CustomerInfoEntity(new CustomerId(customerId), firstname, lastname, contactAddress.toDomainObject(), billingAddress.toDomainObject());
	}

	public String getCustomerId() {
		return customerId;
	}

	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}

	public String getFirstname() {
		return firstname;
	}

	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

	public String getLastname() {
		return lastname;
	}

	public void setLastname(String lastname) {
		this.lastname = lastname;
	}

	public AddressDto getContactAddress() {
		return contactAddress;
	}

	public void setContactAddress(AddressDto contactAddress) {
		this.contactAddress = contactAddress;
	}

	public AddressDto getBillingAddress() {
		return billingAddress;
	}

	public void setBillingAddress(AddressDto billingAddress) {
		this.billingAddress = billingAddress;
	}
}

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * You might be wondering why this Exception class is in the dtos package. Exceptions of this type
 * are thrown whenever the client tries to fetch an insurance quote request that doesn't exist. Spring will then
 * convert this exception into an HTTP 404 response.
 * */
@ResponseStatus(code = HttpStatus.NOT_FOUND)
public class InsuranceQuoteRequestNotFoundException extends RuntimeException {
	private static final long serialVersionUID = 7849369545115229681L;

	public InsuranceQuoteRequestNotFoundException(String errorMessage) {
		super(errorMessage);
	}
}


import java.util.Date;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.lakesidemutual.customerselfservice.domain.insurancequoterequest.InsuranceQuoteEntity;

/**
 * InsuranceQuoteDto is a data transfer object (DTO) that represents an Insurance Quote
 * which has been submitted as a response to a specific Insurance Quote Request.
 */
public class InsuranceQuoteDto {
	@Valid
	@NotNull
	private Date expirationDate;

	@Valid
	@NotNull
	private MoneyAmountDto insurancePremium;

	@Valid
	@NotNull
	private MoneyAmountDto policyLimit;

	public InsuranceQuoteDto() {
	}

	private InsuranceQuoteDto(Date expirationDate, MoneyAmountDto insurancePremium, MoneyAmountDto policyLimit) {
		this.expirationDate = expirationDate;
		this.insurancePremium = insurancePremium;
		this.policyLimit = policyLimit;
	}

	public static InsuranceQuoteDto fromDomainObject(InsuranceQuoteEntity insuranceQuote) {
		Date expirationDate = insuranceQuote.getExpirationDate();
		MoneyAmountDto insurancePremiumDto = MoneyAmountDto.fromDomainObject(insuranceQuote.getInsurancePremium());
		MoneyAmountDto policyLimitDto = MoneyAmountDto.fromDomainObject(insuranceQuote.getPolicyLimit());
		return new InsuranceQuoteDto(expirationDate, insurancePremiumDto, policyLimitDto);
	}

	public Date getExpirationDate() {
		return expirationDate;
	}

	public void setExpirationDate(Date expirationDate) {
		this.expirationDate = expirationDate;
	}

	public MoneyAmountDto getInsurancePremium() {
		return insurancePremium;
	}

	public void setInsurancePremium(MoneyAmountDto insurancePremium) {
		this.insurancePremium = insurancePremium;
	}

	public MoneyAmountDto getPolicyLimit() {
		return policyLimit;
	}

	public void setPolicyLimit(MoneyAmountDto policyLimit) {
		this.policyLimit = policyLimit;
	}
}

import org.springframework.hateoas.RepresentationModel;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * The CustomerDto class is a data transfer object (DTO) that represents a single customer.
 * It inherits from the ResourceSupport class which allows us to create a REST representation (e.g., JSON, XML)
 * that follows the HATEOAS principle. For example, links can be added to the representation (e.g., self, address.change)
 * which means that future actions the client may take can be discovered from the resource representation.
 *
 * @see <a href="https://docs.spring.io/spring-hateoas/docs/current/reference/html/">Spring HATEOAS - Reference Documentation</a>
 */
public class CustomerDto extends RepresentationModel {
	private String customerId;
	@JsonUnwrapped
	private CustomerProfileDto customerProfile;

	public CustomerDto() {
	}

	public String getCustomerId() {
		return customerId;
	}

	public CustomerProfileDto getCustomerProfile() {
		return this.customerProfile;
	}

	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}

	public void setCustomerProfile(CustomerProfileDto customerProfile) {
		this.customerProfile = customerProfile;
	}
}


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * You might be wondering why this Exception class is in the dtos package. Exceptions of this type
 * are thrown whenever the client sends a request with an invalid customer id. Spring will then
 * convert this exception into an HTTP 404 response.
 * */
@ResponseStatus(code = HttpStatus.NOT_FOUND)
public class CustomerNotFoundException extends RuntimeException {
	private static final long serialVersionUID = -402909143828880299L;

	public CustomerNotFoundException(String errorMessage) {
		super(errorMessage);
	}
}

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * CustomerProfileDto is a data transfer object (DTO) that represents the personal data (customer profile) of a customer.
 */
public class CustomerProfileDto {
	private String firstname;
	private String lastname;
	private Date birthday;
	@JsonUnwrapped
	private AddressDto currentAddress;
	private String email;
	private String phoneNumber;
	private List<AddressDto> moveHistory;

	public CustomerProfileDto() {
	}

	public String getFirstname() {
		return firstname;
	}

	public String getLastname() {
		return lastname;
	}

	public Date getBirthday() {
		return birthday;
	}

	public AddressDto getCurrentAddress() {
		return currentAddress;
	}

	public String getEmail() {
		return email;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public List<AddressDto> getMoveHistory() {
		return moveHistory;
	}

	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

	public void setLastname(String lastname) {
		this.lastname = lastname;
	}

	public void setBirthday(Date birthday) {
		this.birthday = birthday;
	}

	public void setCurrentAddress(AddressDto currentAddress) {
		this.currentAddress = currentAddress;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public void setMoveHistory(List<AddressDto> moveHistory) {
		this.moveHistory = moveHistory;
	}
}


import java.util.List;

import org.springframework.hateoas.RepresentationModel;

/**
 * The CustomersDto class is a data transfer object (DTO) that contains a list of customers.
 * It inherits from the ResourceSupport class which allows us to create a REST representation (e.g., JSON, XML)
 * that follows the HATEOAS principle. For example, links can be added to the representation (e.g., self, next, prev)
 * which means that future actions the client may take can be discovered from the resource representation.
 *
 * @see <a href="https://docs.spring.io/spring-hateoas/docs/current/reference/html/">Spring HATEOAS - Reference Documentation</a>
 */
public class CustomersDto extends RepresentationModel {
	private List<CustomerDto> customers;

	public CustomersDto() {}

	public CustomersDto(List<CustomerDto> customers) {
		this.customers = customers;
	}

	public List<CustomerDto> getCustomers() {
		return customers;
	}

	public void setCustomers(List<CustomerDto> customers) {
		this.customers = customers;
	}
}

import java.util.Date;
import java.util.Objects;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lakesidemutual.customerselfservice.interfaces.validation.PhoneNumber;

/**
 * CustomerProfileUpdateRequestDto is a data transfer object (DTO) that represents the personal data (customer profile) of a customer.
 * It is sent to the Customer Core when a new customer is created.
 */
public class CustomerProfileUpdateRequestDto {
	@NotEmpty
	private String firstname;

	@NotEmpty
	private String lastname;

	@JsonFormat(pattern = "yyyy-MM-dd")
	private Date birthday;

	@NotEmpty
	private String streetAddress;

	@NotEmpty
	private String postalCode;

	@NotEmpty
	private String city;

	@Email
	@NotEmpty
	private String email;

	@PhoneNumber
	private String phoneNumber;

	public CustomerProfileUpdateRequestDto() {
	}

	public CustomerProfileUpdateRequestDto(String firstname, String lastname, Date birthday, String streetAddress, String postalCode, String city, String email, String phoneNumber) {
		this.firstname = firstname;
		this.lastname = lastname;
		this.birthday = birthday;
		this.streetAddress = streetAddress;
		this.postalCode = postalCode;
		this.city = city;
		this.email = email;
		this.phoneNumber = phoneNumber;
	}

	public String getFirstname() {
		return firstname;
	}

	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

	public String getLastname() {
		return lastname;
	}

	public void setLastname(String lastname) {
		this.lastname = lastname;
	}

	public Date getBirthday() {
		return birthday;
	}

	public void setBirthday(Date birthday) {
		this.birthday = birthday;
	}

	public String getStreetAddress() {
		return streetAddress;
	}

	public void setStreetAddress(String streetAddress) {
		this.streetAddress = streetAddress;
	}

	public String getPostalCode() {
		return postalCode;
	}

	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CustomerProfileUpdateRequestDto that = (CustomerProfileUpdateRequestDto) o;
		return Objects.equals(firstname, that.firstname) && Objects.equals(lastname, that.lastname) && Objects.equals(birthday, that.birthday) && Objects.equals(streetAddress, that.streetAddress) && Objects.equals(postalCode, that.postalCode) && Objects.equals(city, that.city) && Objects.equals(email, that.email) && Objects.equals(phoneNumber, that.phoneNumber);
	}

	@Override
	public int hashCode() {
		return Objects.hash(firstname, lastname, birthday, streetAddress, postalCode, city, email, phoneNumber);
	}
}


/**
 * CustomerIdDto is a data transfer object (DTO) that represents the unique ID of a customer.
 * */
public class CustomerIdDto {
	private String id;

	public CustomerIdDto() {
	}

	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}
}


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * You might be wondering why this Exception class is in the dtos package. Exceptions of this type
 * are thrown whenever the Customer Self-Service backend can't connect to the Customer Core. Spring will then
 * convert this exception into an HTTP 502 response.
 * */
@ResponseStatus(code = HttpStatus.BAD_GATEWAY)
public class CustomerCoreNotAvailableException extends RuntimeException {
	private static final long serialVersionUID = -156378720396633916L;

	public CustomerCoreNotAvailableException(String errorMessage) {
		super(errorMessage);
	}
}

import java.util.Date;
import java.util.Objects;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lakesidemutual.customerselfservice.interfaces.validation.PhoneNumber;

/**
 * CustomerRegistrationRequestDto is a data transfer object (DTO) that represents the personal data (customer profile) of a customer.
 * It is sent to the CustomerInformationHolder when a user completes the registration process in the Customer Self-Service frontend.
 */
public class CustomerRegistrationRequestDto {

	@NotEmpty
	private String firstname;

	@NotEmpty
	private String lastname;

	@NotNull
	@Past
	@JsonFormat(pattern = "yyyy-MM-dd")
	private Date birthday;

	@NotEmpty
	private String city;

	@NotEmpty
	private String streetAddress;

	@NotEmpty
	private String postalCode;

	@PhoneNumber
	private String phoneNumber;

	public CustomerRegistrationRequestDto() {
	}

	public Date getBirthday() {
		return birthday;
	}

	public String getCity() {
		return city;
	}

	public String getFirstname() {
		return firstname;
	}

	public String getLastname() {
		return lastname;
	}

	public String getStreetAddress() {
		return streetAddress;
	}

	public String getPostalCode() {
		return postalCode;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setBirthday(Date birthday) {
		this.birthday = birthday;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

	public void setLastname(String lastname) {
		this.lastname = lastname;
	}

	public void setStreetAddress(String streetAddress) {
		this.streetAddress = streetAddress;
	}

	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CustomerRegistrationRequestDto that = (CustomerRegistrationRequestDto) o;
		return Objects.equals(firstname, that.firstname) && Objects.equals(lastname, that.lastname) && Objects.equals(birthday, that.birthday) && Objects.equals(city, that.city) && Objects.equals(streetAddress, that.streetAddress) && Objects.equals(postalCode, that.postalCode) && Objects.equals(phoneNumber, that.phoneNumber);
	}

	@Override
	public int hashCode() {
		return Objects.hash(firstname, lastname, birthday, city, streetAddress, postalCode, phoneNumber);
	}
}


import jakarta.validation.constraints.NotEmpty;

import com.lakesidemutual.customerselfservice.domain.customer.Address;

/**
 * AddressDto is a data transfer object (DTO) that represents the postal address of a customer.
 * */
public class AddressDto {
	@NotEmpty
	private String streetAddress;

	@NotEmpty
	private String postalCode;

	@NotEmpty
	private String city;

	public AddressDto() {
	}

	public AddressDto(String streetAddress, String postalCode, String city) {
		this.streetAddress = streetAddress;
		this.postalCode = postalCode;
		this.city = city;
	}

	public static AddressDto fromDomainObject(Address address) {
		return new AddressDto(address.getStreetAddress(), address.getPostalCode(), address.getCity());
	}

	public Address toDomainObject() {
		return new Address(streetAddress, postalCode, city);
	}

	public String getStreetAddress() {
		return streetAddress;
	}

	public String getPostalCode() {
		return postalCode;
	}

	public String getCity() {
		return city;
	}

	public void setStreetAddress(String streetAddress) {
		this.streetAddress = streetAddress;
	}

	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}

	public void setCity(String city) {
		this.city = city;
	}
}

/**
 * UserResponseDto is a data transfer object (DTO) that represents a user. The customerId property references the customer object in the
 * Customer Core that belongs to this user. A customerId that is set to null indicates that the user has not yet completed the registration.
 * */
public class UserResponseDto {
	private final String email;
	private final String customerId;

	public UserResponseDto(String email, String customerId) {
		this.email = email;
		this.customerId = customerId;
	}

	public String getEmail() {
		return email;
	}

	public String getCustomerId() {
		return customerId;
	}
}


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * You might be wondering why this Exception class is in the dtos package. Exceptions of this type
 * are thrown whenever the client tries to create an account for a user that already exists.
 * Spring will then convert this exception into an HTTP 409 response.
 * */
@ResponseStatus(code = HttpStatus.CONFLICT)
public class UserAlreadyExistsException extends RuntimeException {
	private static final long serialVersionUID = 5852316265258762036L;

	public UserAlreadyExistsException(String errorMessage) {
		super(errorMessage);
	}
}

/**
 * AuthenticationResponseDto is a data transfer object (DTO) which contains the JWT token that is sent to a user
 * after he or she successfully logs into the Customer Self-Service frontend.
 */
public class AuthenticationResponseDto {
	private String email;
	private String token;

	public AuthenticationResponseDto(String email, String token) {
		this.email = email;
		this.token = token;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

}


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;

/**
 * SignupRequestDto is a data transfer object (DTO) that represents the login credentials of a new user. It is
 * sent to the AuthenticationController when a new user tries to sign up in the Customer Self-Service frontend.
 */
public class SignupRequestDto {
	@Email
	@NotEmpty
	private String email;

	@NotEmpty
	private String password;

	public SignupRequestDto() {
	}

	public SignupRequestDto(String email, String password) {
		this.email = email;
		this.password = password;
	}

	public String getEmail() {
		return email;
	}

	public String getPassword() {
		return this.password;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}


/**
 * AuthenticationRequestDto is a data transfer object (DTO) that represents the login credentials of a user.
 * It is sent to the AuthenticationController when a user tries to log into the Customer Self-Service frontend.
 */
public class AuthenticationRequestDto {
	private String email;
	private String password;

	public AuthenticationRequestDto() {
	}

	public String getEmail() {
		return this.email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

}


import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.lakesidemutual.customerselfservice.domain.customer.CustomerId;

/**
 * This converter class allows us to use CustomerId as the type of
 * a @PathVariable parameter in a Spring @RestController class.
 */
@Component
public class StringToCustomerIdConverter implements Converter<String, CustomerId> {
	@Override
	public CustomerId convert(String source) {
		return new CustomerId(source);
	}
}


import java.util.Date;

import org.microserviceapipatterns.domaindrivendesign.DomainEvent;

/**
 * InsuranceQuoteExpiredEvent is a domain event class that is used to notify the Customer Self-Service Backend
 * when the Insurance Quote for a specific Insurance Quote Request has expired.
 * */
public class InsuranceQuoteExpiredEvent implements DomainEvent {
	private Date date;
	private Long insuranceQuoteRequestId;

	public InsuranceQuoteExpiredEvent() {
	}

	public InsuranceQuoteExpiredEvent(Date date, Long insuranceQuoteRequestId) {
		this.date = date;
		this.insuranceQuoteRequestId = insuranceQuoteRequestId;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public Long getInsuranceQuoteRequestId() {
		return insuranceQuoteRequestId;
	}

	public void setInsuranceQuoteRequestId(Long insuranceQuoteRequestId) {
		this.insuranceQuoteRequestId = insuranceQuoteRequestId;
	}
}

import java.util.Date;

import org.microserviceapipatterns.domaindrivendesign.DomainEvent;

/**
 * CustomerDecisionEvent is a domain event class that is used to notify the Policy Management Backend
 * about a decision by a customer to accept or reject a specific Insurance Quote.
 * */
public class CustomerDecisionEvent implements DomainEvent {
	private Date date;
	private Long insuranceQuoteRequestId;
	private boolean quoteAccepted;

	public CustomerDecisionEvent() {
	}

	public CustomerDecisionEvent(Date date, Long insuranceQuoteRequestId, boolean quoteAccepted) {
		this.date = date;
		this.insuranceQuoteRequestId = insuranceQuoteRequestId;
		this.quoteAccepted = quoteAccepted;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public Long getInsuranceQuoteRequestId() {
		return insuranceQuoteRequestId;
	}

	public void setInsuranceQuoteRequestId(Long insuranceQuoteRequestId) {
		this.insuranceQuoteRequestId = insuranceQuoteRequestId;
	}

	public boolean isQuoteAccepted() {
		return quoteAccepted;
	}

	public void setQuoteAccepted(boolean quoteAccepted) {
		this.quoteAccepted = quoteAccepted;
	}
}


import java.util.Date;
import java.util.Objects;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.microserviceapipatterns.domaindrivendesign.ValueObject;

/**
 * An instance of RequestStatusChange is a value object that represents a status change
 * of an insurance quote request. It contains the date of the status change as well as the new status.
 */
@Entity
@Table(name = "requeststatuschanges")
@Embeddable
public class RequestStatusChange implements ValueObject {
	@GeneratedValue
	@Id
	private Long id;

	private Date date;

	@Enumerated(EnumType.STRING)
	private RequestStatus status;

	public RequestStatusChange() {}

	public RequestStatusChange(Date date, RequestStatus status) {
		Objects.requireNonNull(date);
		Objects.requireNonNull(status);
		this.date = date;
		this.status = status;
	}

	public Date getDate() {
		return date;
	}

	public RequestStatus getStatus() {
		return status;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}

		RequestStatusChange other = (RequestStatusChange) obj;
		return Objects.equals(date, other.date) && Objects.equals(status, other.status);
	}

	@Override
	public int hashCode() {
		return Objects.hash(date, status);
	}
}

import java.util.Date;

import org.microserviceapipatterns.domaindrivendesign.DomainEvent;

import com.lakesidemutual.customerselfservice.interfaces.dtos.insurancequoterequest.MoneyAmountDto;

/**
 * InsuranceQuoteResponseEvent is a domain event class that is used to notify the Customer Self-Service Backend
 * when Lakeside Mutual has submitted a response for a specific Insurance Quote Request.
 * */
public class InsuranceQuoteResponseEvent implements DomainEvent {
	private Date date;
	private Long insuranceQuoteRequestId;
	private boolean requestAccepted;
	private Date expirationDate;
	private MoneyAmountDto insurancePremium;
	private MoneyAmountDto policyLimit;

	public InsuranceQuoteResponseEvent() {
	}

	public InsuranceQuoteResponseEvent(Date date, Long insuranceQuoteRequestId, boolean requestAccepted, Date expirationDate, MoneyAmountDto insurancePremium, MoneyAmountDto policyLimit) {
		this.date = date;
		this.insuranceQuoteRequestId = insuranceQuoteRequestId;
		this.requestAccepted = requestAccepted;
		this.expirationDate = expirationDate;
		this.insurancePremium = insurancePremium;
		this.policyLimit = policyLimit;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public Long getInsuranceQuoteRequestId() {
		return insuranceQuoteRequestId;
	}

	public void setInsuranceQuoteRequestId(Long insuranceQuoteRequestId) {
		this.insuranceQuoteRequestId = insuranceQuoteRequestId;
	}

	public boolean isRequestAccepted() {
		return requestAccepted;
	}

	public void setRequestAccepted(boolean requestAccepted) {
		this.requestAccepted = requestAccepted;
	}

	public Date getExpirationDate() {
		return expirationDate;
	}

	public void setExpirationDate(Date expirationDate) {
		this.expirationDate = expirationDate;
	}

	public MoneyAmountDto getInsurancePremium() {
		return insurancePremium;
	}

	public void setInsurancePremium(MoneyAmountDto insurancePremium) {
		this.insurancePremium = insurancePremium;
	}

	public MoneyAmountDto getPolicyLimit() {
		return policyLimit;
	}

	public void setPolicyLimit(MoneyAmountDto policyLimit) {
		this.policyLimit = policyLimit;
	}
}

import java.util.Date;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * InsuranceOptionsEntity is an entity that contains the insurance options (e.g., start date, insurance type, etc.)
 * that a customer selected for an Insurance Quote Request.
 */
@Entity
@Table(name = "insuranceoptions")
public class InsuranceOptionsEntity implements org.microserviceapipatterns.domaindrivendesign.Entity {
	@GeneratedValue
	@Id
	private Long id;

	private Date startDate;

	@Embedded
	private InsuranceType insuranceType;

	@Embedded
	@AttributeOverrides({
		@AttributeOverride(name="amount", column=@Column(name="deductibleAmount")),
		@AttributeOverride(name="currency", column=@Column(name="deductibleCurrency"))})
	private MoneyAmount deductible;

	public InsuranceOptionsEntity() {
	}

	public InsuranceOptionsEntity(Date startDate, InsuranceType insuranceType, MoneyAmount deductible) {
		this.startDate = startDate;
		this.insuranceType = insuranceType;
		this.deductible = deductible;
	}

	public Date getStartDate() {
		return startDate;
	}

	public InsuranceType getInsuranceType() {
		return insuranceType;
	}

	public MoneyAmount getDeductible() {
		return deductible;
	}
}


import java.util.Date;

import org.microserviceapipatterns.domaindrivendesign.DomainEvent;

/**
 * PolicyCreatedEvent is a domain event class that is used to notify the Customer Self-Service Backend
 * when a new Policy has been created after an Insurance Quote has been accepted.
 * */
public class PolicyCreatedEvent implements DomainEvent {
	private Date date;
	private Long insuranceQuoteRequestId;
	private String policyId;

	public PolicyCreatedEvent() {
	}

	public PolicyCreatedEvent(Date date, Long insuranceQuoteRequestId, String policyId) {
		this.date = date;
		this.insuranceQuoteRequestId = insuranceQuoteRequestId;
		this.policyId = policyId;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public Long getInsuranceQuoteRequestId() {
		return insuranceQuoteRequestId;
	}

	public void setInsuranceQuoteRequestId(Long insuranceQuoteRequestId) {
		this.insuranceQuoteRequestId = insuranceQuoteRequestId;
	}

	public String getPolicyId() {
		return policyId;
	}

	public void setPolicyId(String policyId) {
		this.policyId = policyId;
	}
}

import java.util.Date;

import com.lakesidemutual.customerselfservice.interfaces.dtos.insurancequoterequest.InsuranceQuoteRequestDto;
import org.microserviceapipatterns.domaindrivendesign.DomainEvent;

/**
 * InsuranceQuoteRequestEvent is a domain event class that is used to notify the Policy Management Backend
 * when a new Insurance Quote Request has been submitted by a customer.
 * */
public class InsuranceQuoteRequestEvent implements DomainEvent {
	private Date date;
	private InsuranceQuoteRequestDto insuranceQuoteRequestDto;

	public InsuranceQuoteRequestEvent() {
	}

	public InsuranceQuoteRequestEvent(Date date, InsuranceQuoteRequestDto insuranceQuoteRequestDto) {
		this.date = date;
		this.insuranceQuoteRequestDto = insuranceQuoteRequestDto;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public InsuranceQuoteRequestDto getInsuranceQuoteRequestDto() {
		return insuranceQuoteRequestDto;
	}

	public void setInsuranceQuoteRequestDto(InsuranceQuoteRequestDto insuranceQuoteRequestDto) {
		this.insuranceQuoteRequestDto = insuranceQuoteRequestDto;
	}
}


import java.util.Date;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * InsuranceQuoteEntity is an entity that represents an Insurance Quote
 * which has been submitted as a response to a specific Insurance Quote Request.
 */
@Entity
@Table(name = "insurancequotes")
public class InsuranceQuoteEntity implements org.microserviceapipatterns.domaindrivendesign.Entity {
	@GeneratedValue
	@Id
	private Long id;

	private Date expirationDate;

	@Embedded
	@AttributeOverrides({
		@AttributeOverride(name="amount", column=@Column(name="insurancePremiumAmount")),
		@AttributeOverride(name="currency", column=@Column(name="insurancePremiumCurrency"))})
	private MoneyAmount insurancePremium;

	@Embedded
	@AttributeOverrides({
		@AttributeOverride(name="amount", column=@Column(name="policyLimitAmount")),
		@AttributeOverride(name="currency", column=@Column(name="policyLimitCurrency"))})
	private MoneyAmount policyLimit;

	public InsuranceQuoteEntity() {
	}

	public InsuranceQuoteEntity(Date expirationDate, MoneyAmount insurancePremium, MoneyAmount policyLimit) {
		this.expirationDate = expirationDate;
		this.insurancePremium = insurancePremium;
		this.policyLimit = policyLimit;
	}

	public Date getExpirationDate() {
		return expirationDate;
	}

	public MoneyAmount getInsurancePremium() {
		return insurancePremium;
	}

	public MoneyAmount getPolicyLimit() {
		return policyLimit;
	}
}


import org.microserviceapipatterns.domaindrivendesign.ValueObject;

/**
 * A RequestStatus is a value object that is used to represent
 * the current status of an insurance quote request.
 *
 * The following diagram shows the possible state transitions:
 *
 * <pre>
 *
 *                                               │
 *                                               ▼
 *                                        ┌────────────┐
 *                                        │  REQUEST_  │
 *                                        │ SUBMITTED  │
 *                                        └────────────┘
 *                                               │
 *                             ┌─────────────────┴────────────────┐
 *                             │                                  │
 *                             ▼                                  ▼
 *                      ┌────────────┐                     ╔════════════╗
 *                      │   QUOTE_   │                     ║  REQUEST_  ║
 *          ┌───────────│  RECEIVED  │─────────────┐       ║  REJECTED  ║
 *          │           └────────────┘             │       ╚════════════╝
 *          │                  │                   │
 *          │                  │                   │
 *          ▼                  ▼                   ▼
 *   ╔════════════╗     ┌────────────┐      ╔════════════╗
 *   ║   QUOTE_   ║     │   QUOTE_   │      ║   QUOTE_   ║
 *   ║  REJECTED  ║     │  ACCEPTED  │─────▶║  EXPIRED   ║
 *   ╚════════════╝     └────────────┘      ╚════════════╝
 *                             │
 *                             │
 *                             ▼
 *                      ╔════════════╗
 *                      ║  POLICY_   ║
 *                      ║  CREATED   ║
 *                      ╚════════════╝
 *
 * </pre>
 */
public enum RequestStatus implements ValueObject {
	/** The customer has submitted a request. No answer has been received yet. */
	REQUEST_SUBMITTED,

	/** Lakeside Mutual has rejected the request. No quote has been made. */
	REQUEST_REJECTED,

	/** Lakeside Mutual has accepted the request and made a corresponding quote. */
	QUOTE_RECEIVED,

	/** The customer has accepted Lakeside Mutual's quote. */
	QUOTE_ACCEPTED,

	/** The customer has rejected Lakeside Mutual's quote. */
	QUOTE_REJECTED,

	/** The quote has expired and is no longer valid. */
	QUOTE_EXPIRED,

	/** A new insurance policy has been created. */
	POLICY_CREATED;

	public boolean canTransitionTo(RequestStatus newStatus) {
		switch(this) {
		case REQUEST_SUBMITTED:
			return newStatus == REQUEST_REJECTED || newStatus == QUOTE_RECEIVED;
		case QUOTE_RECEIVED:
			return newStatus == QUOTE_ACCEPTED || newStatus == QUOTE_REJECTED || newStatus == QUOTE_EXPIRED;
		case QUOTE_ACCEPTED:
			return newStatus == POLICY_CREATED || newStatus == QUOTE_EXPIRED;
		case REQUEST_REJECTED:
		case POLICY_CREATED:
		case QUOTE_REJECTED:
		case QUOTE_EXPIRED:
			return false;
		default:
			return false;
		}
	}
}

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.microserviceapipatterns.domaindrivendesign.RootEntity;

/**
 * InsuranceQuoteRequestAggregateRoot is the root entity of the Insurance Quote Request aggregate. Note that there is
 * no class for the Insurance Quote Request aggregate, so the package can be seen as aggregate.
 */
@Entity
@Table(name = "insurancequoterequests")
public class InsuranceQuoteRequestAggregateRoot implements RootEntity {
	@Id
	@GeneratedValue
	private Long id;

	private Date date;

	@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	private List<RequestStatusChange> statusHistory;

	@OneToOne(cascade = CascadeType.ALL)
	private CustomerInfoEntity customerInfo;

	@OneToOne(cascade = CascadeType.ALL)
	private InsuranceOptionsEntity insuranceOptions;

	@OneToOne(cascade = CascadeType.ALL)
	private InsuranceQuoteEntity insuranceQuote;

	private String policyId;

	public InsuranceQuoteRequestAggregateRoot() {}

	public InsuranceQuoteRequestAggregateRoot(Date date, RequestStatus initialStatus, CustomerInfoEntity customerInfo, InsuranceOptionsEntity insuranceOptions, InsuranceQuoteEntity insuranceQuote, String policyId) {
		this.date = date;
		List<RequestStatusChange> statusHistory = new ArrayList<>();
		statusHistory.add(new RequestStatusChange(date, initialStatus));
		this.statusHistory = statusHistory;
		this.customerInfo = customerInfo;
		this.insuranceOptions = insuranceOptions;
		this.insuranceQuote = insuranceQuote;
		this.policyId = policyId;
	}

	public Long getId() {
		return id;
	}

	public Date getDate() {
		return date;
	}

	public RequestStatus getStatus() {
		return statusHistory.get(statusHistory.size()-1).getStatus();
	}

	public List<RequestStatusChange> getStatusHistory() {
		return statusHistory;
	}

	private void changeStatusTo(RequestStatus newStatus, Date date) {
		if (!getStatus().canTransitionTo(newStatus)) {
			throw new RuntimeException(String.format("Cannot change insurance quote request status from %s to %s", getStatus(), newStatus));
		}
		statusHistory.add(new RequestStatusChange(date, newStatus));
	}

	public void acceptRequest(InsuranceQuoteEntity insuranceQuote, Date date) {
		this.insuranceQuote = insuranceQuote;
		changeStatusTo(RequestStatus.QUOTE_RECEIVED, date);
	}

	public void rejectRequest(Date date) {
		changeStatusTo(RequestStatus.REQUEST_REJECTED, date);
	}

	public void acceptQuote(Date date) {
		changeStatusTo(RequestStatus.QUOTE_ACCEPTED, date);
	}

	public void rejectQuote(Date date) {
		changeStatusTo(RequestStatus.QUOTE_REJECTED, date);
	}

	public void markQuoteAsExpired(Date date) {
		changeStatusTo(RequestStatus.QUOTE_EXPIRED, date);
	}

	public void finalizeQuote(String policyId, Date date) {
		changeStatusTo(RequestStatus.POLICY_CREATED, date);
	}

	public CustomerInfoEntity getCustomerInfo() {
		return customerInfo;
	}

	public InsuranceOptionsEntity getInsuranceOptions() {
		return insuranceOptions;
	}

	public InsuranceQuoteEntity getInsuranceQuote() {
		return insuranceQuote;
	}

	public String getPolicyId() {
		return policyId;
	}
}


import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import com.lakesidemutual.customerselfservice.domain.customer.Address;
import com.lakesidemutual.customerselfservice.domain.customer.CustomerId;

/**
 * CustomerInfoEntity is an entity that is part of an InsuranceQuoteRequestAggregateRoot
 * and contains infos about the initiator of the request.
 */
@Entity
@Table(name = "customerinfos")
public class CustomerInfoEntity implements org.microserviceapipatterns.domaindrivendesign.Entity {
	@GeneratedValue
	@Id
	private Long id;

	@Embedded
	@AttributeOverrides({
		@AttributeOverride(name="id", column=@Column(name="customerId"))})
	private final CustomerId customerId;

	private final String firstname;

	private final String lastname;

	@OneToOne(cascade = CascadeType.ALL)
	private final Address contactAddress;

	@OneToOne(cascade = CascadeType.ALL)
	private final Address billingAddress;

	public CustomerInfoEntity() {
		this.customerId = null;
		this.firstname = null;
		this.lastname = null;
		this.contactAddress = null;
		this.billingAddress = null;
	}

	public CustomerInfoEntity(CustomerId customerId, String firstname, String lastname, Address contactAddress, Address billingAddress) {
		this.customerId = customerId;
		this.firstname = firstname;
		this.lastname = lastname;
		this.contactAddress = contactAddress;
		this.billingAddress = billingAddress;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public CustomerId getCustomerId() {
		return customerId;
	}

	public String getFirstname() {
		return firstname;
	}

	public String getLastname() {
		return lastname;
	}

	public Address getContactAddress() {
		return contactAddress;
	}

	public Address getBillingAddress() {
		return billingAddress;
	}
}


import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

import jakarta.persistence.Embeddable;

import org.microserviceapipatterns.domaindrivendesign.ValueObject;

/**
 * An instance of MoneyAmount is a value object that represents an amount of money in a specific currency.
 * For example, this is used to represent the insurance premium of a policy.
 */
@Embeddable
public class MoneyAmount implements ValueObject {
	private BigDecimal amount;
	private Currency currency;

	public MoneyAmount() {}

	public MoneyAmount(BigDecimal amount, Currency currency) {
		Objects.requireNonNull(amount);
		Objects.requireNonNull(currency);
		this.amount = amount;
		this.currency = currency;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public Currency getCurrency() {
		return currency;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}

		MoneyAmount other = (MoneyAmount) obj;
		if(amount.compareTo(other.amount) != 0) {
			return false;
		}

		if(!currency.equals(other.currency)) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return Objects.hash(amount, currency);
	}
}

import java.util.Objects;

import org.microserviceapipatterns.domaindrivendesign.ValueObject;

/**
 * An instance of InsuranceType is a value object that is used to represent the type of insurance (e.g., health insurance, life insurance, etc).
 */
public class InsuranceType implements ValueObject {
	private String name;

	public InsuranceType() {
		this.name = "";
	}

	public InsuranceType(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}

		InsuranceType other = (InsuranceType) obj;
		return Objects.equals(name, other.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}
}

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Embeddable;

import org.apache.commons.lang3.RandomStringUtils;

import org.microserviceapipatterns.domaindrivendesign.EntityIdentifier;
import org.microserviceapipatterns.domaindrivendesign.ValueObject;

/**
 * A CustomerId is a value object that is used to represent the unique id of a customer.
 */
@Embeddable
public class CustomerId implements Serializable, ValueObject, EntityIdentifier<String> {
	private static final long serialVersionUID = 1L;

	private String id;

	public CustomerId() {
		this.setId(null);
	}

	/**
	 * This constructor is needed by ControllerLinkBuilder, see the following
	 * spring-hateoas issue for details:
	 * https://github.com/spring-projects/spring-hateoas/issues/352
	 */
	public CustomerId(String id) {
		this.setId(id);
	}

	public static CustomerId random() {
		return new CustomerId(RandomStringUtils.randomAlphanumeric(10).toLowerCase());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj == null) {
			return false;
		}

		if (getClass() != obj.getClass()) {
			return false;
		}

		CustomerId other = (CustomerId) obj;
		return Objects.equals(getId(), other.getId());
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(getId());
	}

	@Override
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return getId();
	}
}


import java.util.Objects;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.microserviceapipatterns.domaindrivendesign.ValueObject;

/**
 * An Address is a value object that is used to represent the postal address of a customer.
 *
 * You might be wondering why the Address class implements the ValueObject interface even though it has a JPA @Entity annotation.
 * This discrepancy exists for technical reasons. JPA requires Address to be declared as an entity, because it is part of a one-to-many
 * relationship. However, in the DDD sense, Address behaves like a value object.
 *
 * You also might be wondering why this code is the same as that in the Customer Core. This does violate DRY but for
 * good reasons: strategic DD suggests that model boundaries decouple (sub-)systems so that they can be deployed and
 * evolved independently.
 * */
@Entity
@Table(name = "addresses")
public class Address implements ValueObject {
	@GeneratedValue
	@Id
	private Long id;

	private final String streetAddress;

	private final String postalCode;

	private final String city;

	public Address() {
		this.streetAddress = null;
		this.postalCode = null;
		this.city = null;
	}

	public Address(String streetAddress, String postalCode, String city) {
		this.streetAddress = streetAddress;
		this.postalCode = postalCode;
		this.city = city;
	}

	public String getStreetAddress() {
		return streetAddress;
	}

	public String getPostalCode() {
		return postalCode;
	}

	public String getCity() {
		return city;
	}

	@Override
	public String toString() {
		return String.format("%s, %s %ss", streetAddress, postalCode, city);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}

		Address other = (Address) obj;
		return Objects.equals(streetAddress, other.streetAddress) &&
				Objects.equals(postalCode, other.postalCode) &&
				Objects.equals(city, other.city);
	}

	@Override
	public int hashCode() {
		return Objects.hash(streetAddress, postalCode, city);
	}
}

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import com.lakesidemutual.customerselfservice.infrastructure.UserLoginRepository;
import org.microserviceapipatterns.domaindrivendesign.DomainService;

/**
 * UserDetailsServiceImpl is a domain service that can load user logins from the UserLoginRepository.
 * */
@Component
public class UserDetailsServiceImpl implements UserDetailsService, DomainService {
	@Autowired
	private UserLoginRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String email) {
		UserLoginEntity user = this.userRepository.findByEmail(email);

		if (user == null) {
			return null;
		} else {
			return new UserSecurityDetails(
					user.getId(),
					user.getEmail(),
					user.getPassword(),
					AuthorityUtils.commaSeparatedStringToAuthorityList(user.getAuthorities())
					);
		}
	}
}


import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * UserSecurityDetails is an adapter class which makes the login credentials of a specific UserLogin
 * available through Spring's UserDetails interface.
 */
public class UserSecurityDetails implements UserDetails {

	private static final long serialVersionUID = 1L;

	private Boolean accountNonExpired = true;
	private Boolean accountNonLocked = true;
	private Collection<? extends GrantedAuthority> authorities;
	private Boolean credentialsNonExpired = true;
	private String email;
	private Boolean enabled = true;
	private Long id;
	private String password;

	public UserSecurityDetails(Long id, String email, String password,
			Collection<? extends GrantedAuthority> authorities) {
		this.setId(id);
		this.setUsername(email);
		this.setPassword(password);
		this.setEmail(email);
		this.setAuthorities(authorities);
	}

	@JsonIgnore
	public Boolean getAccountNonExpired() {
		return this.accountNonExpired;
	}

	@JsonIgnore
	public Boolean getAccountNonLocked() {
		return this.accountNonLocked;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return this.authorities;
	}

	@JsonIgnore
	public Boolean getCredentialsNonExpired() {
		return this.credentialsNonExpired;
	}

	public String getEmail() {
		return this.email;
	}

	@JsonIgnore
	public Boolean getEnabled() {
		return this.enabled;
	}

	public Long getId() {
		return this.id;
	}

	@Override
	@JsonIgnore
	public String getPassword() {
		return this.password;
	}

	@Override
	public String getUsername() {
		return this.email;
	}

	@Override
	public boolean isAccountNonExpired() {
		return this.getAccountNonExpired();
	}

	@Override
	public boolean isAccountNonLocked() {
		return this.getAccountNonLocked();
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return this.getCredentialsNonExpired();
	}

	@Override
	public boolean isEnabled() {
		return this.getEnabled();
	}

	public void setAccountNonExpired(Boolean accountNonExpired) {
		this.accountNonExpired = accountNonExpired;
	}

	public void setAccountNonLocked(Boolean accountNonLocked) {
		this.accountNonLocked = accountNonLocked;
	}

	public void setAuthorities(Collection<? extends GrantedAuthority> authorities) {
		this.authorities = authorities;
	}

	public void setCredentialsNonExpired(Boolean credentialsNonExpired) {
		this.credentialsNonExpired = credentialsNonExpired;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setUsername(String username) {
		this.password = username;
	}

}


import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.lakesidemutual.customerselfservice.domain.customer.CustomerId;

/**
 * UserLogin is an entity that contains the login credentials of a specific user.
 */
@Entity
@Table(name = "user_logins")
public class UserLoginEntity implements org.microserviceapipatterns.domaindrivendesign.Entity {

	@Id
	@GeneratedValue
	private Long id;
	private String authorities;
	private String email;
	private String password;

	@Embedded
	@AttributeOverrides({
		@AttributeOverride(name="id", column=@Column(name="customerId"))})
	private CustomerId customerId;

	public UserLoginEntity() {
	}

	public UserLoginEntity(String email, String password, String authorities, CustomerId customerId) {
		this.email = email;
		this.password = password;
		this.authorities = authorities;
		this.customerId = customerId;
	}

	public String getAuthorities() {
		return authorities;
	}

	public CustomerId getCustomerId() {
		return customerId;
	}

	public String getEmail() {
		return email;
	}

	public Long getId() {
		return id;
	}

	public String getPassword() {
		return password;
	}

	public void setAuthorities(String authorities) {
		this.authorities = authorities;
	}

	public void setCustomerId(CustomerId customerId) {
		this.customerId = customerId;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setPassword(String password) {
		this.password = password;
	}

}


import org.microserviceapipatterns.domaindrivendesign.BoundedContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * CustomerManagementApplication is the execution entry point of the Customer Management backend which
 * is one of the functional/system/application Bounded Contexts of Lakeside Mutual.
 */
@SpringBootApplication
@EnableFeignClients
public class CustomerManagementApplication implements BoundedContext {
	private static Logger logger = LoggerFactory.getLogger(CustomerManagementApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(CustomerManagementApplication.class, args);
		logger.info("--- Customer Management backend started ---");
	}
}


import org.springframework.data.jpa.repository.JpaRepository;

import com.lakesidemutual.customermanagement.domain.interactionlog.InteractionLogAggregateRoot;
import org.microserviceapipatterns.domaindrivendesign.Repository;

/**
 * The InteractionLogRepository can be used to read and write InteractionLogAggregateRoot objects from and to the backing database. Spring automatically
 * searches for interfaces that extend the JpaRepository interface and creates a corresponding Spring bean for each of them. For more information
 * on repositories visit the <a href="https://docs.spring.io/spring-data/jpa/docs/current/reference/html/">Spring Data JPA - Reference Documentation</a>.
 * */
public interface InteractionLogRepository extends JpaRepository<InteractionLogAggregateRoot, String>, Repository {

}


import org.microserviceapipatterns.domaindrivendesign.InfrastructureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.lakesidemutual.customermanagement.domain.customer.CustomerId;
import com.lakesidemutual.customermanagement.interfaces.dtos.CustomerCoreNotAvailableException;
import com.lakesidemutual.customermanagement.interfaces.dtos.CustomerDto;
import com.lakesidemutual.customermanagement.interfaces.dtos.CustomerProfileDto;
import com.lakesidemutual.customermanagement.interfaces.dtos.CustomersDto;
import com.lakesidemutual.customermanagement.interfaces.dtos.PaginatedCustomerResponseDto;

import feign.FeignException;

/**
 * CustomerCoreRemoteProxy is a remote proxy that interacts with the Customer
 * Core in order to give the Customer Management Backend's own clients access to
 * the shared customer data.
 */
@Component
public class CustomerCoreRemoteProxy implements InfrastructureService {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private CustomerCoreClient customerCoreClient;

	public CustomerDto getCustomer(CustomerId customerId) {
		try {
			CustomersDto customersDto = customerCoreClient.getCustomer(customerId.getId());
			return customersDto.getCustomers().isEmpty() ? null : customersDto.getCustomers().get(0);
		} catch(FeignException e) {
			final String errorMessage = "Failed to connect to Customer Core.";
			logger.info(errorMessage, e);
			throw new CustomerCoreNotAvailableException(errorMessage);
		}
	}

	public PaginatedCustomerResponseDto getCustomers(String filter, int limit, int offset) {
		try {
			return customerCoreClient.getCustomers(filter, limit, offset);
		} catch(FeignException e) {
			final String errorMessage = "Failed to connect to Customer Core.";
			logger.info(errorMessage, e);
			throw new CustomerCoreNotAvailableException(errorMessage);
		}
	}

	public ResponseEntity<CustomerDto> updateCustomer(CustomerId customerId, CustomerProfileDto customerProfile) {
		try {
			return customerCoreClient.updateCustomer(customerId, customerProfile);
		} catch(FeignException e) {
			final String errorMessage = "Failed to connect to Customer Core.";
			logger.info(errorMessage, e);
			throw new CustomerCoreNotAvailableException(errorMessage);
		}
	}
}


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.lakesidemutual.customermanagement.domain.customer.CustomerId;
import com.lakesidemutual.customermanagement.interfaces.dtos.CustomerDto;
import com.lakesidemutual.customermanagement.interfaces.dtos.CustomerProfileDto;
import com.lakesidemutual.customermanagement.interfaces.dtos.CustomersDto;
import com.lakesidemutual.customermanagement.interfaces.dtos.PaginatedCustomerResponseDto;

/**
 * CustomerCoreClient is a FeignClient interface that is used to declare a web service client for the customer-core service.
 * */
@FeignClient(name="customercore", url="${customercore.baseURL}", configuration=CustomerCoreClientConfiguration.class)
public interface CustomerCoreClient {
	@GetMapping(value = "/customers")
	PaginatedCustomerResponseDto getCustomers(
			@RequestParam(value = "filter") String filter,
			@RequestParam(value = "limit") Integer limit,
			@RequestParam(value = "offset") Integer offset);

	@GetMapping(value = "/customers/{ids}")
	CustomersDto getCustomer(
			@PathVariable String ids);

	@PutMapping(value = "/customers/{customerId}")
	ResponseEntity<CustomerDto> updateCustomer(
			@PathVariable CustomerId customerId,
			@RequestBody CustomerProfileDto requestDto);
}

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import feign.RequestInterceptor;
import feign.RequestTemplate;

/**
 * The APIKeyRequestInterceptor class sets the API Key for outgoing requests to the customer-core service.
 * */
@Component
public class APIKeyRequestInterceptor implements RequestInterceptor {
	@Value("${apikey.header}")
	private String apiKeyHeader;

	@Value("${apikey.value}")
	private String apiKeyValue;

	@Override
	public void apply(RequestTemplate template) {
		template.header(apiKeyHeader, "Bearer " + apiKeyValue);
		template.header("Accept", "application/json");
	}
}


import java.util.Arrays;
import java.util.Collection;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import feign.RequestInterceptor;

/**
 * The CustomerCoreClientConfiguration class configures the request interceptors for the CustomerCoreClient.
 * */
@Configuration
public class CustomerCoreClientConfiguration {
	@Bean
	public Collection<RequestInterceptor> interceptors() {
		return Arrays.asList(new APIKeyRequestInterceptor());
	}
}


import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.lakesidemutual.customermanagement.domain.interactionlog.InteractionEntity;
import com.lakesidemutual.customermanagement.domain.interactionlog.InteractionLogAggregateRoot;
import com.lakesidemutual.customermanagement.domain.interactionlog.InteractionLogService;
import com.lakesidemutual.customermanagement.infrastructure.InteractionLogRepository;
import com.lakesidemutual.customermanagement.interfaces.dtos.MessageDto;
import com.lakesidemutual.customermanagement.interfaces.dtos.NotificationDto;

/**
 * This class is a Controller that processes WebSocket chat messages and broadcasts them to all subscribers.
 * */
@Controller
public class CustomerMessageController {
	private final static Logger logger = LoggerFactory.getLogger(CustomerMessageController.class);

	@Autowired
	private InteractionLogRepository interactionLogRepository;

	@Autowired
	private InteractionLogService interactionLogService;

	@Autowired
	private SimpMessagingTemplate simpMessagingTemplate;

	@MessageMapping("/chat/messages")
	@SendTo("/topic/messages")
	public MessageDto processMessage(MessageDto message) throws Exception {
		logger.info("Processing message from " + message.getUsername());

		final String customerId = message.getCustomerId();
		final String id = UUID.randomUUID().toString();
		final Date date = new Date();
		final InteractionEntity interaction = new InteractionEntity(id, date, message.getContent(), message.isSentByOperator());

		final Optional<InteractionLogAggregateRoot> optInteractionLog = interactionLogRepository.findById(customerId);
		InteractionLogAggregateRoot interactionLog;
		if (optInteractionLog.isPresent()) {
			interactionLog = optInteractionLog.get();
			interactionLog.getInteractions().add(interaction);
		} else {
			Collection<InteractionEntity> interactions = new ArrayList<>();
			interactions.add(interaction);
			interactionLog = new InteractionLogAggregateRoot(customerId, message.getUsername(), null, interactions);
		}

		interactionLogRepository.save(interactionLog);
		broadcastNotifications();
		return new MessageDto(id, date, message.getCustomerId(), message.getUsername(), message.getContent(), message.isSentByOperator());
	}

	private void broadcastNotifications() {
		logger.info("Broadcasting updated notifications");
		final List<NotificationDto> notifications = interactionLogService.getNotifications().stream()
				.map(notification -> new NotificationDto(notification.getCustomerId(), notification.getUsername(), notification.getCount()))
				.collect(Collectors.toList());
		simpMessagingTemplate.convertAndSend("/topic/notifications", notifications);
	}
}


import java.util.List;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lakesidemutual.customermanagement.domain.interactionlog.InteractionLogService;
import com.lakesidemutual.customermanagement.interfaces.dtos.NotificationDto;

/**
 * This REST controller gives clients access the current list of unacknowledged chat notifications. It is an example of the
 * <i>Information Holder Resource</i> pattern. This particular one is a special type of information holder called <i>Master Data Holder</i>.
 *
 * @see <a href="https://www.microservice-api-patterns.org/patterns/responsibility/endpointRoles/InformationHolderResource">Information Holder Resource</a>
 * @see <a href="https://www.microservice-api-patterns.org/patterns/responsibility/informationHolderEndpointTypes/MasterDataHolder">Master Data Holder</a>
 */
@RestController
@RequestMapping("/notifications")
public class NotificationInformationHolder {
	@Autowired
	private InteractionLogService interactionLogService;

	@Operation(summary = "Get a list of all unacknowledged notifications.")
	@GetMapping
	public ResponseEntity<List<NotificationDto>> getNotifications() {
		final List<NotificationDto> notifications = interactionLogService.getNotifications().stream()
				.map(notification -> new NotificationDto(notification.getCustomerId(), notification.getUsername(), notification.getCount()))
				.collect(Collectors.toList());
		return ResponseEntity.ok(notifications);
	}
}


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lakesidemutual.customermanagement.domain.customer.CustomerId;
import com.lakesidemutual.customermanagement.domain.interactionlog.InteractionLogAggregateRoot;
import com.lakesidemutual.customermanagement.domain.interactionlog.InteractionLogService;
import com.lakesidemutual.customermanagement.infrastructure.InteractionLogRepository;
import com.lakesidemutual.customermanagement.interfaces.dtos.InteractionAcknowledgementDto;
import com.lakesidemutual.customermanagement.interfaces.dtos.InteractionLogNotFoundException;
import com.lakesidemutual.customermanagement.interfaces.dtos.NotificationDto;

/**
 * This REST controller gives clients access to a customer's interaction log. It is an example of the
 * <i>Information Holder Resource</i> pattern. This particular one is a special type of information holder called <i>Master Data Holder</i>.
 *
 * @see <a href="https://www.microservice-api-patterns.org/patterns/responsibility/endpointRoles/InformationHolderResource">Information Holder Resource</a>
 * @see <a href="https://www.microservice-api-patterns.org/patterns/responsibility/informationHolderEndpointTypes/MasterDataHolder">Master Data Holder</a>
 */
@RestController
@RequestMapping("/interaction-logs")
public class InteractionLogInformationHolder {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private InteractionLogService interactionLogService;

	@Autowired
	private InteractionLogRepository interactionLogRepository;

	@Autowired
	private SimpMessagingTemplate simpMessagingTemplate;

	@Operation(summary = "Get the interaction log for a customer with a given customer id.")
	@GetMapping(value = "/{customerId}")
	public ResponseEntity<InteractionLogAggregateRoot> getInteractionLog(
			@Parameter(description = "the customer's unique id", required = true) @PathVariable CustomerId customerId) {
		final String customerIdStr = customerId.getId();
		Optional<InteractionLogAggregateRoot> optInteractionLog = interactionLogRepository.findById(customerIdStr);
		if (!optInteractionLog.isPresent()) {
			logger.info("Failed to find an interaction log for the customer with id '" + customerId.toString() + "'. Returning an empty interaction log instead.");
			final InteractionLogAggregateRoot emptyInteractionLog = new InteractionLogAggregateRoot(customerIdStr, "", null, new ArrayList<>());
			return ResponseEntity.ok(emptyInteractionLog);
		}

		final InteractionLogAggregateRoot interactionLog = optInteractionLog.get();
		return ResponseEntity.ok(interactionLog);
	}

	@Operation(summary = "Acknowledge all of a given customer's interactions up to the given interaction id.")
	@PatchMapping(value = "/{customerId}")
	public ResponseEntity<InteractionLogAggregateRoot> acknowledgeInteractions(
			@Parameter(description = "the customer's unique id", required = true) @PathVariable CustomerId customerId,
			@Parameter(description = "the id of the newest acknowledged interaction", required = true) @Valid @RequestBody InteractionAcknowledgementDto interactionAcknowledgementDto) {
		final String customerIdStr = customerId.getId();
		Optional<InteractionLogAggregateRoot> optInteractionLog = interactionLogRepository.findById(customerIdStr);
		if (!optInteractionLog.isPresent()) {
			final String errorMessage = "Failed to acknowledge interactions, because there is no interaction log for customer with id '" + customerId.getId() + "'.";
			logger.info(errorMessage);
			throw new InteractionLogNotFoundException(errorMessage);
		}

		final InteractionLogAggregateRoot interactionLog = optInteractionLog.get();
		interactionLog.setLastAcknowledgedInteractionId(interactionAcknowledgementDto.getLastAcknowledgedInteractionId());
		interactionLogRepository.save(interactionLog);
		broadcastNotifications();
		return ResponseEntity.ok(interactionLog);
	}

	private void broadcastNotifications() {
		logger.info("Broadcasting updated notifications");
		final List<NotificationDto> notifications = interactionLogService.getNotifications().stream()
				.map(notification -> new NotificationDto(notification.getCustomerId(), notification.getUsername(), notification.getCount()))
				.collect(Collectors.toList());
		simpMessagingTemplate.convertAndSend("/topic/notifications", notifications);
	}
}


import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.web.servlet.error.AbstractErrorController;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * This class implements a custom error controller that returns an <a href=
 * "https://www.microservice-api-patterns.org/patterns/quality/qualityManagementAndGovernance/ErrorReport">Error
 * Report</a>.
 */
@Controller
public class ErrorController extends AbstractErrorController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	public ErrorController(ErrorAttributes errorAttributes) {
		super(errorAttributes);
	}

	@RequestMapping(value = "/error", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Map<String, Object> handleError(HttpServletRequest request) {
		Map<String, Object> errorAttributes = super.getErrorAttributes(request, ErrorAttributeOptions.defaults());

		Object path = errorAttributes.get("path");
		Object status = errorAttributes.get("status");
		Object error = errorAttributes.get("error");
		Object message = errorAttributes.get("message");

		logger.info("An error occurred while accessing {}: {} {}, {}", path, status, error, message);

		return errorAttributes;
	}
}

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lakesidemutual.customermanagement.domain.customer.CustomerId;
import com.lakesidemutual.customermanagement.infrastructure.CustomerCoreRemoteProxy;
import com.lakesidemutual.customermanagement.interfaces.dtos.CustomerDto;
import com.lakesidemutual.customermanagement.interfaces.dtos.CustomerNotFoundException;
import com.lakesidemutual.customermanagement.interfaces.dtos.CustomerProfileDto;
import com.lakesidemutual.customermanagement.interfaces.dtos.PaginatedCustomerResponseDto;

/**
 * This REST controller gives clients access to the customer data. It is an example of the
 * <i>Information Holder Resource</i> pattern. This particular one is a special type of information holder called <i>Master Data Holder</i>.
 *
 * @see <a href="https://www.microservice-api-patterns.org/patterns/responsibility/endpointRoles/InformationHolderResource">Information Holder Resource</a>
 * @see <a href="https://www.microservice-api-patterns.org/patterns/responsibility/informationHolderEndpointTypes/MasterDataHolder">Master Data Holder</a>
 */
@RestController
@RequestMapping("/customers")
public class CustomerInformationHolder {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private CustomerCoreRemoteProxy customerCoreRemoteProxy;

	@Operation(summary = "Get all customers.")
	@GetMapping
	public ResponseEntity<PaginatedCustomerResponseDto> getCustomers(
			@Parameter(description = "search terms to filter the customers by name", required = false) @RequestParam(value = "filter", required = false, defaultValue = "") String filter,
			@Parameter(description = "the maximum number of customers per page", required = false) @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit,
			@Parameter(description = "the offset of the page's first customer", required = false) @RequestParam(value = "offset", required = false, defaultValue = "0") Integer offset) {
		return ResponseEntity.ok(customerCoreRemoteProxy.getCustomers(filter, limit, offset));
	}

	@Operation(summary = "Get customer with a given customer id.")
	@GetMapping(value = "/{customerId}")
	public ResponseEntity<CustomerDto> getCustomer(
			@Parameter(description = "the customer's unique id", required = true) @PathVariable CustomerId customerId) {

		CustomerDto customer = customerCoreRemoteProxy.getCustomer(customerId);
		if(customer == null) {
			final String errorMessage = "Failed to find a customer with id '" + customerId.getId() + "'.";
			logger.info(errorMessage);
			throw new CustomerNotFoundException(errorMessage);
		}
		return ResponseEntity.ok(customer);
	}

	@Operation(summary = "Update the profile of the customer with the given customer id")
	@PutMapping(value = "/{customerId}")
	public ResponseEntity<CustomerDto> updateCustomer(
			@Parameter(description = "the customer's unique id", required = true) @PathVariable CustomerId customerId,
			@Parameter(description = "the customer's updated profile", required = true) @Valid @RequestBody CustomerProfileDto customerProfile) {
		return customerCoreRemoteProxy.updateCustomer(customerId, customerProfile);
	}
}


import java.io.IOException;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

@Component
public class HeaderRequestInterceptor implements ClientHttpRequestInterceptor {
	@Value("${apikey.header}")
	private String apiKeyHeader;

	@Value("${apikey.value}")
	private String apiKeyValue;

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
			throws IOException {
		final HttpHeaders httpHeaders = request.getHeaders();
		httpHeaders.set(apiKeyHeader, "Bearer " + apiKeyValue);
		httpHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
		return execution.execute(request, body);
	}
}

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;

/**
 * Provides a RestTemplate that can be injected into other components. The
 * RestTemplate uses the configured API Key when making a request.
 */
@Configuration
@Profile("default")
public class DefaultAuthenticatedRestTemplateClient {
	@Autowired
	private HeaderRequestInterceptor headerRequestInterceptor;

	@Bean
	public RestTemplate restTemplate() {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setInterceptors(Arrays.asList(headerRequestInterceptor));
		return restTemplate;
	}
}

import java.util.Collections;
import java.util.function.Predicate;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The SwaggerConfiguration class configures the HTTP resource API documentation.
 */
@Configuration
public class SwaggerConfiguration {

	@Bean
	public OpenAPI customerSelfServiceApi() {
		return new OpenAPI()
				.info(new Info().title("Customer Management API")
						.description("This API allows call center operators to interact with customers and to edit their user profiles.")
						.version("v1.0.0")
						.license(new License().name("Apache 2.0")));
	}
}


import jakarta.servlet.Filter;

import jakarta.servlet.Servlet;
import org.apache.catalina.servlets.DefaultServlet;
import org.h2.server.web.JakartaWebServlet;
import org.h2.server.web.WebServlet;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * The WebConfiguration class is used to customize the default Spring MVC configuration.
 * */
@Configuration
public class WebConfiguration implements WebMvcConfigurer {
	/**
	 * This web servlet makes the web console of the H2 database engine available at the "/console" endpoint.
	 * */
	@Bean
	public ServletRegistrationBean<JakartaWebServlet> h2servletRegistration() {
		ServletRegistrationBean<JakartaWebServlet> registrationBean = new ServletRegistrationBean<>(new JakartaWebServlet());
		registrationBean.addUrlMappings("/console/*");
		return registrationBean;
	}

	/**
	 * This is a filter that generates an ETag value based on the content of the response. This ETag is compared to the If-None-Match header
	 * of the request. If these headers are equal, the response content is not sent, but rather a 304 "Not Modified" status instead.
	 * */
	@Bean
	public Filter shallowETagHeaderFilter() {
		return new ShallowEtagHeaderFilter();
	}
}

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * The WebSocketConfiguration class configures the WebSocket message broker. The WebSocket protocol
 * is used to enable the chat communication between a customer and a customer service operator.
 * */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfiguration implements WebSocketMessageBrokerConfigurer {
	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		registry.enableSimpleBroker("/topic");
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
		registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
	}
}


import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

/**
 * The WebSecurityConfiguration class configures the security policies used for the exposed HTTP resource API.
 * In this case, the API is accessible without authentication.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfiguration {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)
                        .cacheControl(HeadersConfigurer.CacheControlConfig::disable)
                )
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((request, response, exception) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"))
                        .accessDeniedHandler((request, response, exception) -> response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden"))
                )
                .sessionManagement(sessionManagement -> sessionManagement
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        return http.build();
    }

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOriginPattern("*");
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("authorization", "content-type", "x-auth-token"));
        configuration.setExposedHeaders(Collections.singletonList("x-auth-token"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/notifications*", configuration);
        source.registerCorsConfiguration("/notifications/*", configuration);
        source.registerCorsConfiguration("/interaction-logs/*", configuration);
        source.registerCorsConfiguration("/customers*", configuration);
        source.registerCorsConfiguration("/customers/*", configuration);
        return source;
    }
}



import org.springframework.hateoas.RepresentationModel;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * The CustomerDto class is a data transfer object (DTO) that represents a single customer.
 * It inherits from the ResourceSupport class which allows us to create a REST representation (e.g., JSON, XML)
 * that follows the HATEOAS principle. For example, links can be added to the representation (e.g., self, address.change)
 * which means that future actions the client may take can be discovered from the resource representation.
 *
 * @see <a href="https://docs.spring.io/spring-hateoas/docs/current/reference/html/">Spring HATEOAS - Reference Documentation</a>
 */
public class CustomerDto extends RepresentationModel {
	private String customerId;

	/**
	 * @JsonUnwrapped indicates that the annotated object should be inlined in the containing object. In other words: When
	 * serialized as a JSON object, the properties of the annotated object will be directly included in the containing object.
	 * */
	@JsonUnwrapped
	private CustomerProfileDto customerProfile;

	public CustomerDto() {
	}

	public String getCustomerId() {
		return customerId;
	}

	public CustomerProfileDto getCustomerProfile() {
		return this.customerProfile;
	}

	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}

	public void setCustomerProfile(CustomerProfileDto customerProfile) {
		this.customerProfile = customerProfile;
	}
}


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * You might be wondering why this Exception class is in the dtos package. Exceptions of this type
 * are thrown whenever the client sends a request with an invalid customer id. Spring will then
 * convert this exception into an HTTP 404 response.
 * */
@ResponseStatus(code = HttpStatus.NOT_FOUND)
public class CustomerNotFoundException extends RuntimeException {
	private static final long serialVersionUID = 7630575576348582839L;

	public CustomerNotFoundException(String errorMessage) {
		super(errorMessage);
	}
}

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * CustomerProfileDto is a data transfer object (DTO) that represents the personal data (customer profile) of a customer.
 */
public class CustomerProfileDto {
	private String firstname;
	private String lastname;
	private Date birthday;
	@JsonUnwrapped
	private AddressDto currentAddress;
	private String email;
	private String phoneNumber;
	private List<AddressDto> moveHistory;

	public CustomerProfileDto() {
	}

	public String getFirstname() {
		return firstname;
	}

	public String getLastname() {
		return lastname;
	}

	public Date getBirthday() {
		return birthday;
	}

	public AddressDto getCurrentAddress() {
		return currentAddress;
	}

	public String getEmail() {
		return email;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public List<AddressDto> getMoveHistory() {
		return moveHistory;
	}

	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

	public void setLastname(String lastname) {
		this.lastname = lastname;
	}

	public void setBirthday(Date birthday) {
		this.birthday = birthday;
	}

	public void setCurrentAddress(AddressDto currentAddress) {
		this.currentAddress = currentAddress;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public void setMoveHistory(List<AddressDto> moveHistory) {
		this.moveHistory = moveHistory;
	}
}

import jakarta.validation.constraints.NotEmpty;

/**
 * InteractionAcknowledgementDto is a data transfer object (DTO) that is sent from the Customer Management Frontend
 * in order to acknowledge notifications up to a specific interaction. This means that the corresponding notification
 * disappears from the UI, because it was acknowledged/read.
 */
public class InteractionAcknowledgementDto {
	@NotEmpty
	private String lastAcknowledgedInteractionId;

	public InteractionAcknowledgementDto() {
	}

	public InteractionAcknowledgementDto(String lastAcknowledgedInteractionId) {
		this.lastAcknowledgedInteractionId = lastAcknowledgedInteractionId;
	}

	public String getLastAcknowledgedInteractionId() {
		return lastAcknowledgedInteractionId;
	}

	public void setLastAcknowledgedInteractionId(String lastAcknowledgedInteractionId) {
		this.lastAcknowledgedInteractionId = lastAcknowledgedInteractionId;
	}
}


import java.util.List;

import org.springframework.hateoas.RepresentationModel;

/**
 * The CustomersDto class is a data transfer object (DTO) that contains a list of customers.
 * It inherits from the ResourceSupport class which allows us to create a REST representation (e.g., JSON, XML)
 * that follows the HATEOAS principle. For example, links can be added to the representation (e.g., self, next, prev)
 * which means that future actions the client may take can be discovered from the resource representation.
 *
 * @see <a href="https://docs.spring.io/spring-hateoas/docs/current/reference/html/">Spring HATEOAS - Reference Documentation</a>
 */
public class CustomersDto extends RepresentationModel {
	private List<CustomerDto> customers;

	public CustomersDto() {}

	public CustomersDto(List<CustomerDto> customers) {
		this.customers = customers;
	}

	public List<CustomerDto> getCustomers() {
		return customers;
	}

	public void setCustomers(List<CustomerDto> customers) {
		this.customers = customers;
	}
}


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * You might be wondering why this Exception class is in the dtos package. Exceptions of this type
 * are thrown whenever the client tries to fetch an interaction log that doesn't exist. Spring will then
 * convert this exception into an HTTP 404 response.
 * */
@ResponseStatus(code = HttpStatus.NOT_FOUND)
public class InteractionLogNotFoundException extends RuntimeException {
	private static final long serialVersionUID = 6747931627482045640L;

	public InteractionLogNotFoundException(String errorMessage) {
		super(errorMessage);
	}
}


/**
 * NotificationDto is a data transfer object (DTO) that represents a chat notification in the Customer Management Frontend.
 * It describes how many as yet unacknowledged messages have been sent by a specific customer.
 */
public class NotificationDto {
	private String customerId;
	private String username;
	private int count;

	public NotificationDto() {}

	public NotificationDto(String customerId, String username, int count) {
		this.customerId = customerId;
		this.username = username;
		this.count = count;
	}

	public String getCustomerId() {
		return customerId;
	}

	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}
}

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * You might be wondering why this Exception class is in the dtos package. Exceptions of this type
 * are thrown whenever the Customer Management backend can't connect to the Customer Core. Spring will then
 * convert this exception into an HTTP 502 response.
 * */
@ResponseStatus(code = HttpStatus.BAD_GATEWAY)
public class CustomerCoreNotAvailableException extends RuntimeException {
	private static final long serialVersionUID = 2146599135907479601L;

	public CustomerCoreNotAvailableException(String errorMessage) {
		super(errorMessage);
	}
}

import java.util.List;

import org.springframework.hateoas.RepresentationModel;

/**
 * The PaginatedCustomerResponseDto holds a collection of CustomerDto
 * with additional metadata parameters such as the limit, offset and size that
 * are used in the <a href=
 * "https://www.microservice-api-patterns.org/patterns/structure/compositeRepresentations/Pagination">Pagination</a>
 * pattern, specifically the <em>Offset-Based</em> Pagination variant.
 */
public class PaginatedCustomerResponseDto extends RepresentationModel {
	private String filter;
	private int limit;
	private int offset;
	private int size;
	private List<CustomerDto> customers;

	public PaginatedCustomerResponseDto() {}

	public PaginatedCustomerResponseDto(String filter, int limit, int offset, int size, List<CustomerDto> customers) {
		this.filter = filter;
		this.limit = limit;
		this.offset = offset;
		this.size = size;
		this.customers = customers;
	}

	public List<CustomerDto> getCustomers() {
		return customers;
	}

	public void setCustomers(List<CustomerDto> customers) {
		this.customers = customers;
	}

	public String getFilter() {
		return filter;
	}

	public void setFilter(String filter) {
		this.filter = filter;
	}

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	public int getOffset() {
		return offset;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}
}

/**
 * AddressDto is a data transfer object (DTO) that represents the postal address of a customer.
 * */
public class AddressDto {
	private String streetAddress;
	private String postalCode;
	private String city;

	public AddressDto() {
	}

	public String getStreetAddress() {
		return streetAddress;
	}

	public String getPostalCode() {
		return postalCode;
	}

	public String getCity() {
		return city;
	}

	public void setStreetAddress(String streetAddress) {
		this.streetAddress = streetAddress;
	}

	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}

	public void setCity(String city) {
		this.city = city;
	}
}



import java.util.Date;

import jakarta.validation.constraints.NotEmpty;

/**
 * MessageDto is a data transfer object (DTO) that contains the content and metadata of a chat message.
 */
public class MessageDto {
	private String id;

	private Date date;

	@NotEmpty
	private String customerId;

	@NotEmpty
	private String username;

	@NotEmpty
	private String content;

	private boolean sentByOperator;

	public MessageDto() {
	}

	public MessageDto(String id, Date date, String customerId, String username, String content, boolean sentByOperator) {
		this.id = id;
		this.date = date;
		this.customerId = customerId;
		this.username = username;
		this.content = content;
		this.sentByOperator = sentByOperator;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getCustomerId() {
		return customerId;
	}

	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public boolean isSentByOperator() {
		return sentByOperator;
	}

	public void setSentByOperator(boolean sentByOperator) {
		this.sentByOperator = sentByOperator;
	}
}


import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.microserviceapipatterns.domaindrivendesign.RootEntity;

/**
 * InteractionLogAggregateRoot is the root entity of the InteractionLog aggregate. Note that there is
 * no class for the InteractionLog aggregate, so the package can be seen as aggregate.
 */
@Entity
@Table(name = "interactionlogs")
public class InteractionLogAggregateRoot implements RootEntity {
	@Id
	private String customerId;

	private String username;

	private String lastAcknowledgedInteractionId;

	@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	private final Collection<InteractionEntity> interactions;

	public InteractionLogAggregateRoot() {
		this.customerId = null;
		this.username = null;
		this.lastAcknowledgedInteractionId = null;
		this.interactions = null;
	}

	public InteractionLogAggregateRoot(String customerId, String username, String lastAcknowledgedInteractionId, Collection<InteractionEntity> interactions) {
		this.customerId = customerId;
		this.username = username;
		this.lastAcknowledgedInteractionId = lastAcknowledgedInteractionId;
		this.interactions = interactions;
	}

	public String getCustomerId() {
		return customerId;
	}

	public String getUsername() {
		return username;
	}

	public String getLastAcknowledgedInteractionId() {
		return lastAcknowledgedInteractionId;
	}

	public void setLastAcknowledgedInteractionId(String lastAcknowledgedInteractionId) {
		this.lastAcknowledgedInteractionId = lastAcknowledgedInteractionId;
	}

	public Collection<InteractionEntity> getInteractions() {
		return interactions;
	}

	public int getNumberOfUnacknowledgedInteractions() {
		final List<InteractionEntity> interactions = getInteractions().stream().filter(interaction -> !interaction.isSentByOperator()).collect(Collectors.toList());
		if(lastAcknowledgedInteractionId == null) {
			return interactions.size();
		} else {
			int count = 0;
			for(int i = interactions.size()-1; i >= 0; i--) {
				if(lastAcknowledgedInteractionId.equals(interactions.get(i).getId())) {
					break;
				} else {
					count += 1;
				}
			}
			return count;
		}
	}
}


import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * InteractionEntity is an entity that contains the content and metadata of a chat message.
 */
@Entity
@Table(name = "interactions")
public class InteractionEntity implements org.microserviceapipatterns.domaindrivendesign.Entity {
	@Id
	private String id;
	private Date date;
	private String content;
	private boolean sentByOperator;

	public InteractionEntity() {}

	public InteractionEntity(String id, Date date, String content, boolean sentByOperator) {
		this.id = id;
		this.date = date;
		this.content = content;
		this.sentByOperator = sentByOperator;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public boolean isSentByOperator() {
		return sentByOperator;
	}

	public void setSentByOperator(boolean sentByOperator) {
		this.sentByOperator = sentByOperator;
	}
}


import org.microserviceapipatterns.domaindrivendesign.ValueObject;

/**
 * A Notification is a value object that is used to represent
 * the number of unread messages sent by a specific customer.
 * */
public class Notification implements ValueObject {
	private final String customerId;
	private final String username;
	private final int count;

	public Notification(String customerId, String username, int count) {
		this.customerId = customerId;
		this.username = username;
		this.count = count;
	}

	public String getCustomerId() {
		return customerId;
	}

	public String getUsername() {
		return username;
	}

	public int getCount() {
		return count;
	}
}

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.lakesidemutual.customermanagement.infrastructure.InteractionLogRepository;
import org.microserviceapipatterns.domaindrivendesign.DomainService;

/**
 * InteractionLogService is a domain service which generates notification objects for any unacknowledged interactions.
 */
@Component
public class InteractionLogService implements DomainService {
	@Autowired
	private InteractionLogRepository interactionLogRepository;

	public List<Notification> getNotifications() {
		final List<Notification> notifications = new ArrayList<>();
		final List<InteractionLogAggregateRoot> interactionLogs = interactionLogRepository.findAll();
		for(InteractionLogAggregateRoot interactionLog : interactionLogs) {
			int count = interactionLog.getNumberOfUnacknowledgedInteractions();
			if(count > 0) {
				Notification notification = new Notification(interactionLog.getCustomerId(), interactionLog.getUsername(), count);
				notifications.add(notification);
			}
		}
		return notifications;
	}
}

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Embeddable;

import org.microserviceapipatterns.domaindrivendesign.EntityIdentifier;
import org.microserviceapipatterns.domaindrivendesign.ValueObject;

/**
 * A CustomerId is a value object that is used to represent the unique id of a customer.
 */
@Embeddable
public class CustomerId implements Serializable, ValueObject, EntityIdentifier<String> {
	private static final long serialVersionUID = 1L;

	private String id;

	public CustomerId() {
		this.setId(null);
	}

	public CustomerId(String id) {
		this.id = id;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj == null) {
			return false;
		}

		if (getClass() != obj.getClass()) {
			return false;
		}

		CustomerId other = (CustomerId) obj;
		return Objects.equals(getId(), other.getId());
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(getId());
	}

	@Override
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return getId();
	}
}


import org.microserviceapipatterns.domaindrivendesign.BoundedContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jms.annotation.EnableJms;

/**
 * PolicyManagementApplication is the execution entry point of the Policy Management backend which
 * is one of the functional/system/application Bounded Contexts of Lakeside Mutual.
 */
@SpringBootApplication
@EnableJms
public class PolicyManagementApplication implements BoundedContext {
	private static Logger logger = LoggerFactory.getLogger(PolicyManagementApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(PolicyManagementApplication.class, args);
		logger.info("--- Policy Management backend started ---");
	}
}


import java.util.List;

import org.microserviceapipatterns.domaindrivendesign.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import com.lakesidemutual.policymanagement.domain.customer.CustomerId;
import com.lakesidemutual.policymanagement.domain.policy.PolicyAggregateRoot;
import com.lakesidemutual.policymanagement.domain.policy.PolicyId;

/**
 * The PolicyRepository can be used to read and write PolicyAggregateRoot objects from and to the backing database. Spring automatically
 * searches for interfaces that extend the JpaRepository interface and creates a corresponding Spring bean for each of them. For more information
 * on repositories visit the <a href="https://docs.spring.io/spring-data/jpa/docs/current/reference/html/">Spring Data JPA - Reference Documentation</a>.
 * */
public interface PolicyRepository extends JpaRepository<PolicyAggregateRoot, PolicyId>, Repository {
	default PolicyId nextId() {
		return PolicyId.random();
	}

	public List<PolicyAggregateRoot> findAllByCustomerIdOrderByCreationDateDesc(CustomerId customerId);
}


import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.microserviceapipatterns.domaindrivendesign.InfrastructureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.lakesidemutual.policymanagement.domain.customer.CustomerId;
import com.lakesidemutual.policymanagement.interfaces.dtos.CustomerCoreNotAvailableException;
import com.lakesidemutual.policymanagement.interfaces.dtos.customer.CustomerDto;
import com.lakesidemutual.policymanagement.interfaces.dtos.customer.CustomersDto;
import com.lakesidemutual.policymanagement.interfaces.dtos.customer.PaginatedCustomerResponseDto;

/**
 * CustomerCoreRemoteProxy is a remote proxy that interacts with the Customer Core in order to give
 * the Policy Management Backend's own clients access to the shared customer data.
 * */
@Component
public class CustomerCoreRemoteProxy implements InfrastructureService {
	@Value("${customercore.baseURL}")
	private String customerCoreBaseURL;

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private RestTemplate restTemplate;


	public CustomerDto getCustomer(CustomerId customerId) {
		List<CustomerDto> customers = getCustomersById(customerId);
		return customers.isEmpty() ? null : customers.get(0);
	}

	public PaginatedCustomerResponseDto getCustomers(String filter, int limit, int offset) {
		try {
			UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(customerCoreBaseURL + "/customers")
					.queryParam("filter", filter)
					.queryParam("limit", limit)
					.queryParam("offset", offset);
			return restTemplate.getForObject(builder.toUriString(), PaginatedCustomerResponseDto.class);
		} catch(RestClientException e) {
			final String errorMessage = "Failed to connect to Customer Core.";
			logger.warn(errorMessage, e);
			throw new CustomerCoreNotAvailableException(errorMessage);
		}
	}

	public List<CustomerDto> getCustomersById(CustomerId... customerIds) {
		try {
			List<String> customerIdStrings = Arrays.asList(customerIds).stream().map(id -> id.getId()).collect(Collectors.toList());
			String ids = String.join(",", customerIdStrings);
			return restTemplate.getForObject(customerCoreBaseURL + "/customers/" + ids, CustomersDto.class).getCustomers();
		} catch(RestClientException e) {
			final String errorMessage = "Failed to connect to Customer Core.";
			logger.warn(errorMessage, e);
			throw new CustomerCoreNotAvailableException(errorMessage);
		}
	}
}


import org.microserviceapipatterns.domaindrivendesign.InfrastructureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import com.lakesidemutual.policymanagement.domain.insurancequoterequest.InsuranceQuoteExpiredEvent;
import com.lakesidemutual.policymanagement.domain.insurancequoterequest.InsuranceQuoteResponseEvent;
import com.lakesidemutual.policymanagement.domain.insurancequoterequest.PolicyCreatedEvent;

/**
 * CustomerSelfServiceMessageProducer is an infrastructure service class that is used to notify the Customer Self-Service Backend
 * when Lakeside Mutual has responded to a customer's insurance quote request (InsuranceQuoteResponseEvent) or when an insurance quote
 * has expired (InsuranceQuoteExpiredEvent). These events are transmitted via an ActiveMQ message queue.
 * */
@Component
public class CustomerSelfServiceMessageProducer implements InfrastructureService {
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	@Value("${insuranceQuoteResponseEvent.queueName}")
	private String quoteResponseQueue;

	@Value("${insuranceQuoteExpiredEvent.queueName}")
	private String quoteExpiredQueue;

	@Value("${policyCreatedEvent.queueName}")
	private String policyCreatedQueue;

	@Autowired
	private JmsTemplate jmsTemplate;

	public void sendInsuranceQuoteResponseEvent(InsuranceQuoteResponseEvent event) {
		try {
			jmsTemplate.convertAndSend(quoteResponseQueue, event);
			logger.info("Successfully sent an insurance quote response to the Customer Self-Service backend.");
		} catch(JmsException exception) {
			logger.error("Failed to send an insurance quote response to the Customer Self-Service backend.", exception);
		}
	}

	public void sendInsuranceQuoteExpiredEvent(InsuranceQuoteExpiredEvent event) {
		try {
			jmsTemplate.convertAndSend(quoteExpiredQueue, event);
			logger.info("Successfully sent an insurance quote expired event to the Customer Self-Service backend.");
		} catch(JmsException exception) {
			logger.error("Failed to send an insurance quote expired event to the Customer Self-Service backend.", exception);
		}
	}

	public void sendPolicyCreatedEvent(PolicyCreatedEvent event) {
		try {
			jmsTemplate.convertAndSend(policyCreatedQueue, event);
			logger.info("Successfully sent an policy created event to the Customer Self-Service backend.");
		} catch(JmsException exception) {
			logger.error("Failed to send an policy created event to the Customer Self-Service backend.", exception);
		}
	}
}

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import org.microserviceapipatterns.domaindrivendesign.Repository;
import com.lakesidemutual.policymanagement.domain.customer.CustomerId;
import com.lakesidemutual.policymanagement.domain.insurancequoterequest.InsuranceQuoteRequestAggregateRoot;

/**
 * The InsuranceQuoteRequestRepository can be used to read and write InsuranceQuoteRequestAggregateRoot objects from and to the backing database. Spring automatically
 * searches for interfaces that extend the JpaRepository interface and creates a corresponding Spring bean for each of them. For more information
 * on repositories visit the <a href="https://docs.spring.io/spring-data/jpa/docs/current/reference/html/">Spring Data JPA - Reference Documentation</a>.
 * */
public interface InsuranceQuoteRequestRepository extends JpaRepository<InsuranceQuoteRequestAggregateRoot, Long>, Repository {
	List<InsuranceQuoteRequestAggregateRoot> findByCustomerInfo_CustomerId(CustomerId customerId);
	List<InsuranceQuoteRequestAggregateRoot> findAllByOrderByDateDesc();
}

import org.microserviceapipatterns.domaindrivendesign.InfrastructureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

/**
 * RiskManagementMessageProducer is an infrastructure service class that is used to notify the Risk Management Server
 * about policy events (e.g., a new policy is created). These events are transmitted via an ActiveMQ message queue.
 * */
@Component
public class RiskManagementMessageProducer implements InfrastructureService {
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	@Value("${riskmanagement.queueName}")
	private String queueName;

	/**
	 * JmsTemplate is a helper class that makes it easy to synchronously access a message queue. Note that this application
	 * uses an ActiveMQ message queue and the corresponding broker service is configured in the MessagingConfiguration
	 * class.
	 *
	 * @see <a href="https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/jms/core/JmsTemplate.html">JmsTemplate</a>
	 * @see <a href="https://docs.spring.io/spring/docs/3.1.x/spring-framework-reference/html/jms.html">JMS (Java Messaging Service)</a>
	 * */
	@Autowired
	private JmsTemplate jmsTemplate;

	/**
	 * This method first converts the event into a JSON payload using the MappingJackson2MessageConverter that was set up in the
	 * MessagingConfiguration class. It then sends this payload to the ActiveMQ queue with the given queue name.
	 */
	public void emitEvent(Object event) {
		try {
			jmsTemplate.convertAndSend(queueName, event);
			logger.info("Successfully sent a policy event to the risk management message queue.");
		} catch(JmsException exception) {
			logger.error("Failed to send a policy event to the risk management message queue.", exception);
		}
	}
}



import org.quartz.JobDetail;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

/**
 * The ExpirationCheckerJobConfiguration class is used to set up
 * the scheduler to periodically execute the ExpirationCheckerJob.
 * */
@Configuration
public class ExpirationCheckerJobConfiguration {
	private final static int START_DELAY = 60000;
	private final static int REPEAT_INTERVAL = 60000;

	@Autowired
	private ApplicationContext applicationContext;

	@Bean
	public JobDetailFactoryBean jobDetail() {
		JobDetailFactoryBean jobDetailFactory = new JobDetailFactoryBean();
		jobDetailFactory.setJobClass(ExpirationCheckerJob.class);
		jobDetailFactory.setDescription("Invoke Expiration Checker Job...");
		jobDetailFactory.setDurability(true);
		return jobDetailFactory;
	}

	@Bean
	public SimpleTriggerFactoryBean trigger(JobDetail job) {
		SimpleTriggerFactoryBean trigger = new SimpleTriggerFactoryBean();
		trigger.setJobDetail(job);
		trigger.setStartDelay(START_DELAY);
		trigger.setRepeatInterval(REPEAT_INTERVAL);
		trigger.setRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY);
		return trigger;
	}

	@Bean
	public SchedulerFactoryBean scheduler(Trigger trigger, JobDetail job) {
		SchedulerFactoryBean schedulerFactory = new SchedulerFactoryBean();
		schedulerFactory.setConfigLocation(new ClassPathResource("quartz.properties"));
		schedulerFactory.setJobFactory(springBeanJobFactory());
		schedulerFactory.setJobDetails(job);
		schedulerFactory.setTriggers(trigger);
		return schedulerFactory;
	}

	@Bean
	public SpringBeanJobFactory springBeanJobFactory() {
		SpringBeanJobFactory jobFactory = new SpringBeanJobFactory();
		jobFactory.setApplicationContext(applicationContext);
		return jobFactory;
	}
}

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.lakesidemutual.policymanagement.domain.insurancequoterequest.InsuranceQuoteExpiredEvent;
import com.lakesidemutual.policymanagement.domain.insurancequoterequest.InsuranceQuoteRequestAggregateRoot;
import com.lakesidemutual.policymanagement.infrastructure.CustomerSelfServiceMessageProducer;
import com.lakesidemutual.policymanagement.infrastructure.InsuranceQuoteRequestRepository;

/**
 * ExpirationCheckerJob is a Quartz job that periodically checks for expired insurance quotes. For each
 * expired insurance quote, it also sends an InsuranceQuoteExpiredEvent to the Customer Self-Service backend.
 * */
public class ExpirationCheckerJob implements Job {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private CustomerSelfServiceMessageProducer customerSelfServiceMessageProducer;

	@Autowired
	private InsuranceQuoteRequestRepository insuranceQuoteRequestRepository;

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		logger.debug("Checking for expired insurance quotes...");

		final Date date = new Date();
		List<InsuranceQuoteRequestAggregateRoot> quoteRequests = insuranceQuoteRequestRepository.findAll();
		List<InsuranceQuoteRequestAggregateRoot> expiredQuoteRequests = quoteRequests.stream()
				.filter(quoteRequest -> quoteRequest.checkQuoteExpirationDate(date))
				.collect(Collectors.toList());
		insuranceQuoteRequestRepository.saveAll(expiredQuoteRequests);
		expiredQuoteRequests.forEach(expiredQuoteRequest -> {
			InsuranceQuoteExpiredEvent event = new InsuranceQuoteExpiredEvent(date, expiredQuoteRequest.getId());
			customerSelfServiceMessageProducer.sendInsuranceQuoteExpiredEvent(event);
		});

		if(expiredQuoteRequests.size() > 0) {
			logger.info("Found {} expired insurance quotes", expiredQuoteRequests.size());
		} else {
			logger.debug("Found no expired insurance quotes");
		}
	}
}


import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.lakesidemutual.policymanagement.domain.customer.CustomerId;
import com.lakesidemutual.policymanagement.domain.policy.InsuringAgreementEntity;
import com.lakesidemutual.policymanagement.domain.policy.MoneyAmount;
import com.lakesidemutual.policymanagement.domain.policy.PolicyAggregateRoot;
import com.lakesidemutual.policymanagement.domain.policy.PolicyId;
import com.lakesidemutual.policymanagement.domain.policy.PolicyPeriod;
import com.lakesidemutual.policymanagement.domain.policy.PolicyType;
import com.lakesidemutual.policymanagement.infrastructure.PolicyRepository;

/**
 * The run() method of the DataLoader class is automatically executed when the application launches.
 * It populates the database with sample policies that can be used to test the application.
 * */
@Component
public class DataLoader implements ApplicationRunner {
	@Autowired
	private PolicyRepository policyRepository;

	private Logger logger = Logger.getLogger(this.getClass().getName());

	@Override
	public void run(ApplicationArguments args) throws ParseException {
		if(policyRepository.count() > 0) {
			logger.info("Skipping import of application dummy data, because the database already contains existing entities.");
			return;
		}

		PolicyId policyId = new PolicyId("fvo5pkqerr");
		CustomerId customerId = new CustomerId("rgpp0wkpec");
		Date startDate = new GregorianCalendar(2018, Calendar.FEBRUARY, 5).getTime();
		Date endDate = new GregorianCalendar(2018, Calendar.FEBRUARY, 10).getTime();
		PolicyPeriod policyPeriod = new PolicyPeriod(startDate, endDate);
		PolicyType policyType = new PolicyType("Health Insurance");
		MoneyAmount deductible = new MoneyAmount(BigDecimal.valueOf(1500), Currency.getInstance("CHF"));
		MoneyAmount policyLimit = new MoneyAmount(BigDecimal.valueOf(1000000), Currency.getInstance("CHF"));
		MoneyAmount insurancePremium = new MoneyAmount(BigDecimal.valueOf(250), Currency.getInstance("CHF"));
		InsuringAgreementEntity insuringAgreement = new InsuringAgreementEntity(Collections.emptyList());
		PolicyAggregateRoot policy = new PolicyAggregateRoot(policyId, customerId, new Date(), policyPeriod, policyType,
				deductible, policyLimit, insurancePremium, insuringAgreement);
		policyRepository.save(policy);
		logger.info("DataLoader has successfully imported all application dummy data, the application is now ready.");
	}
}

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lakesidemutual.policymanagement.domain.customer.CustomerId;
import com.lakesidemutual.policymanagement.domain.policy.DeletePolicyEvent;
import com.lakesidemutual.policymanagement.domain.policy.InsuringAgreementEntity;
import com.lakesidemutual.policymanagement.domain.policy.MoneyAmount;
import com.lakesidemutual.policymanagement.domain.policy.PolicyAggregateRoot;
import com.lakesidemutual.policymanagement.domain.policy.PolicyId;
import com.lakesidemutual.policymanagement.domain.policy.PolicyPeriod;
import com.lakesidemutual.policymanagement.domain.policy.PolicyType;
import com.lakesidemutual.policymanagement.domain.policy.UpdatePolicyEvent;
import com.lakesidemutual.policymanagement.infrastructure.CustomerCoreRemoteProxy;
import com.lakesidemutual.policymanagement.infrastructure.PolicyRepository;
import com.lakesidemutual.policymanagement.infrastructure.RiskManagementMessageProducer;
import com.lakesidemutual.policymanagement.interfaces.dtos.UnknownCustomerException;
import com.lakesidemutual.policymanagement.interfaces.dtos.customer.CustomerDto;
import com.lakesidemutual.policymanagement.interfaces.dtos.policy.CreatePolicyRequestDto;
import com.lakesidemutual.policymanagement.interfaces.dtos.policy.PaginatedPolicyResponseDto;
import com.lakesidemutual.policymanagement.interfaces.dtos.policy.PolicyDto;
import com.lakesidemutual.policymanagement.interfaces.dtos.policy.PolicyNotFoundException;

/**
 * This REST controller gives clients access to the insurance policies. It is an example of the
 * <i>Information Holder Resource</i> pattern. This particular one is a special type of information holder called <i>Master Data Holder</i>.
 *
 * @see <a href="https://www.microservice-api-patterns.org/patterns/responsibility/endpointRoles/InformationHolderResource">Information Holder Resource</a>
 * @see <a href="https://www.microservice-api-patterns.org/patterns/responsibility/informationHolderEndpointTypes/MasterDataHolder">Master Data Holder</a>
 */
@RestController
@RequestMapping("/policies")
public class PolicyInformationHolder {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private PolicyRepository policyRepository;

	@Autowired
	private RiskManagementMessageProducer riskManagementMessageProducer;

	@Autowired
	private CustomerCoreRemoteProxy customerCoreRemoteProxy;

	@Operation(summary = "Create a new policy.")
	@PostMapping
	public ResponseEntity<PolicyDto> createPolicy(
			@Parameter(description = "the policy that is to be added", required = true)
			@Valid
			@RequestBody
			CreatePolicyRequestDto createPolicyDto,
			HttpServletRequest request) {
		String customerIdString = createPolicyDto.getCustomerId();
		logger.info("Creating a new policy for customer with id '{}'", customerIdString);
		CustomerId customerId = new CustomerId(customerIdString);
		List<CustomerDto> customers = customerCoreRemoteProxy.getCustomersById(customerId);
		if(customers.isEmpty()) {
			final String errorMessage = "Failed to find a customer with id '{}'";
			logger.warn(errorMessage, customerId.getId());
			throw new UnknownCustomerException(errorMessage);
		}

		PolicyId id = PolicyId.random();
		PolicyType policyType = new PolicyType(createPolicyDto.getPolicyType());
		PolicyPeriod policyPeriod = createPolicyDto.getPolicyPeriod().toDomainObject();
		MoneyAmount deductible = createPolicyDto.getDeductible().toDomainObject();
		MoneyAmount policyLimit = createPolicyDto.getPolicyLimit().toDomainObject();
		MoneyAmount insurancePremium = createPolicyDto.getInsurancePremium().toDomainObject();
		InsuringAgreementEntity insuringAgreement = createPolicyDto.getInsuringAgreement().toDomainObject();
		PolicyAggregateRoot policy = new PolicyAggregateRoot(id, customerId, new Date(), policyPeriod, policyType, deductible, policyLimit, insurancePremium, insuringAgreement);
		policyRepository.save(policy);

		CustomerDto customer = customers.get(0);
		PolicyDto policyDto = createPolicyDtos(Arrays.asList(policy), "").get(0);
		final UpdatePolicyEvent event = new UpdatePolicyEvent(request.getRemoteAddr(), new Date(), customer, policyDto);
		riskManagementMessageProducer.emitEvent(event);
		return ResponseEntity.ok(policyDto);
	}

	@Operation(summary = "Update an existing policy.")
	@PutMapping(value = "/{policyId}")
	public ResponseEntity<PolicyDto> updatePolicy(
			@Parameter(description = "the policy's unique id", required = true) @PathVariable PolicyId policyId,
			@Parameter(description = "the updated policy", required = true) @Valid @RequestBody CreatePolicyRequestDto createPolicyDto,
			HttpServletRequest request) {
		logger.info("Updating policy with id '{}'", policyId.getId());

		Optional<PolicyAggregateRoot> optPolicy = policyRepository.findById(policyId);
		if(!optPolicy.isPresent()) {
			final String errorMessage = "Failed to find a policy with id '{}'";
			logger.warn(errorMessage, policyId.getId());
			throw new PolicyNotFoundException(errorMessage);
		}

		CustomerId customerId = new CustomerId(createPolicyDto.getCustomerId());
		List<CustomerDto> customers = customerCoreRemoteProxy.getCustomersById(customerId);
		if(customers.isEmpty()) {
			final String errorMessage = "Failed to find a customer with id '{}'";
			logger.warn(errorMessage, customerId.getId());
			throw new UnknownCustomerException(errorMessage);
		}

		PolicyType policyType = new PolicyType(createPolicyDto.getPolicyType());
		PolicyPeriod policyPeriod = createPolicyDto.getPolicyPeriod().toDomainObject();
		MoneyAmount deductible = createPolicyDto.getDeductible().toDomainObject();
		MoneyAmount policyLimit = createPolicyDto.getPolicyLimit().toDomainObject();
		MoneyAmount insurancePremium = createPolicyDto.getInsurancePremium().toDomainObject();
		InsuringAgreementEntity insuringAgreement = createPolicyDto.getInsuringAgreement().toDomainObject();

		PolicyAggregateRoot policy = optPolicy.get();
		policy.setPolicyPeriod(policyPeriod);
		policy.setPolicyType(policyType);
		policy.setDeductible(deductible);
		policy.setPolicyLimit(policyLimit);
		policy.setInsurancePremium(insurancePremium);
		policy.setInsuringAgreement(insuringAgreement);
		policyRepository.save(policy);

		CustomerDto customer = customers.get(0);
		PolicyDto policyDto = createPolicyDtos(Arrays.asList(policy), "").get(0);
		final UpdatePolicyEvent event = new UpdatePolicyEvent(request.getRemoteAddr(), new Date(), customer, policyDto);
		riskManagementMessageProducer.emitEvent(event);

		PolicyDto response = createPolicyDtos(Arrays.asList(policy), "").get(0);
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "Delete an existing policy.")
	@DeleteMapping(value = "/{policyId}")
	public ResponseEntity<Void> deletePolicy(
			@Parameter(description = "the policy's unique id", required = true) @PathVariable PolicyId policyId,
			HttpServletRequest request) {
		logger.info("Deleting policy with id '{}'", policyId.getId());
		policyRepository.deleteById(policyId);

		final DeletePolicyEvent event = new DeletePolicyEvent(request.getRemoteAddr(), new Date(), policyId.getId());
		riskManagementMessageProducer.emitEvent(event);

		return ResponseEntity.noContent().build();
	}

	private List<PolicyDto> createPolicyDtos(List<PolicyAggregateRoot> policies, String expand) {
		List<CustomerDto> customers = null;
		if(expand.equals("customer")) {
			List<CustomerId> customerIds = policies.stream().map(p -> p.getCustomerId()).collect(Collectors.toList());
			customers = customerCoreRemoteProxy.getCustomersById(customerIds.toArray(new CustomerId[customerIds.size()]));
		}

		List<PolicyDto> policyDtos = new ArrayList<>();
		for(int i = 0; i < policies.size(); i++) {
			PolicyAggregateRoot policy = policies.get(i);
			PolicyDto policyDto = PolicyDto.fromDomainObject(policy);
			if(customers != null) {
				CustomerDto customer = customers.get(i);
				policyDto.setCustomer(customer);
			}
			policyDtos.add(policyDto);
		}
		return policyDtos;
	}

	private PaginatedPolicyResponseDto createPaginatedPolicyResponseDto(Integer limit, Integer offset, String expand, int size,
			List<PolicyDto> policyDtos) {

		PaginatedPolicyResponseDto paginatedPolicyResponseDto = new PaginatedPolicyResponseDto(limit, offset,
				size, policyDtos);

		paginatedPolicyResponseDto.add(linkTo(methodOn(PolicyInformationHolder.class).getPolicies(limit, offset, expand)).withSelfRel());

		if (offset > 0) {
			paginatedPolicyResponseDto.add(linkTo(
					methodOn(PolicyInformationHolder.class).getPolicies(limit, Math.max(0, offset - limit), expand))
					.withRel("prev"));
		}

		if (offset < size - limit) {
			paginatedPolicyResponseDto.add(linkTo(methodOn(PolicyInformationHolder.class).getPolicies(limit, offset + limit, expand))
					.withRel("next"));
		}

		return paginatedPolicyResponseDto;
	}

	@Operation(summary = "Get all policies, newest first.")
	@GetMapping
	public ResponseEntity<PaginatedPolicyResponseDto> getPolicies(
			@Parameter(description = "the maximum number of policies per page", required = false) @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit,
			@Parameter(description = "the offset of the page's first policy", required = false) @RequestParam(value = "offset", required = false, defaultValue = "0") Integer offset,
			@Parameter(description = "a comma-separated list of the fields that should be expanded in the response", required = false) @RequestParam(value = "expand", required = false, defaultValue = "") String expand) {
		logger.debug("Fetching a page of policies (offset={},limit={},fields='{}')", offset, limit, expand);
		List<PolicyAggregateRoot> allPolicies = policyRepository.findAll(Sort.by(Sort.Direction.DESC, PolicyAggregateRoot.FIELD_CREATION_DATE));
		List<PolicyAggregateRoot> policies = allPolicies.stream().skip(offset).limit(limit).collect(Collectors.toList());
		List<PolicyDto> policyDtos = createPolicyDtos(policies, expand);
		PaginatedPolicyResponseDto paginatedPolicyResponse = createPaginatedPolicyResponseDto(limit, offset, expand, allPolicies.size(), policyDtos);
		return ResponseEntity.ok(paginatedPolicyResponse);
	}

	/**
	 * Returns the policy for the given policy id.
	 * <br><br>
	 * The query parameter {@code expand } allows clients to provide a so-called <a href="https://www.microservice-api-patterns.org/patterns/quality/dataTransferParsimony/WishList">Wish List</a>.
	 * By default, getPolicy() returns the following response:
	 * <pre>
	 *   <code>
	 * GET http://localhost:8090/policies/h3riovf4xq/
	 *
	 * {
	 *   "_expandable" : [ "customer" ],
	 *   "policyId" : "h3riovf4xq",
	 *   "customer" : "rgpp0wkpec",
	 *   "creationDate" : "2018-06-19T12:45:46.743+0000",
	 *   ...
	 * }
	 *   </code>
	 * </pre>
	 *
	 * The response includes only the customer's id. The {@code _expandable } section indicates that the {@code customer } resource can be expanded:
	 *
	 * <pre>
	 *   <code>
	 * GET http://localhost:8090/policies/h3riovf4xq/?expand=customer
	 *
	 * {
	 *   "_expandable" : [ "customer" ],
	 *   "policyId" : "h3riovf4xq",
	 *   "customer" : {
	 *     "customerId" : "rgpp0wkpec",
	 *     "customerProfile" : {
	 *       "firstname" : "Max",
	 *       "lastname" : "Mustermann",
	 *       ...
	 *     },
	 *     ...
	 *   },
	 *   "creationDate" : "2018-06-19T12:45:46.743+0000",
	 *   ...
	 * }
	 *   </code>
	 * </pre>
	 *
	 * @see <a href=
	 *      "https://www.microservice-api-patterns.org/patterns/quality/dataTransferParsimony/WishList">https://www.microservice-api-patterns.org/patterns/quality/dataTransferParsimony/WishList</a>
	 */
	@Operation(summary = "Get a single policy.")
	@GetMapping(value = "/{policyId}")
	public ResponseEntity<PolicyDto> getPolicy(
			@Parameter(description = "the policy's unique id", required = true) @PathVariable PolicyId policyId,
			@Parameter(description = "a comma-separated list of the fields that should be expanded in the response", required = false) @RequestParam(value = "expand", required = false, defaultValue = "") String expand) {
		logger.debug("Fetching policy with id '{}'", policyId.getId());
		Optional<PolicyAggregateRoot> optPolicy = policyRepository.findById(policyId);
		if(!optPolicy.isPresent()) {
			final String errorMessage = "Failed to find a policy with id '{}'";
			logger.warn(errorMessage, policyId.getId());
			throw new PolicyNotFoundException(errorMessage);
		}

		PolicyAggregateRoot policy = optPolicy.get();
		PolicyDto response = createPolicyDtos(Arrays.asList(policy), expand).get(0);
		return ResponseEntity.ok(response);
	}
}


import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.web.servlet.error.AbstractErrorController;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * This class implements a custom error controller that returns an <a href=
 * "https://www.microservice-api-patterns.org/patterns/quality/qualityManagementAndGovernance/ErrorReport">Error
 * Report</a>.
 */
@Controller
public class ErrorController extends AbstractErrorController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	public ErrorController(ErrorAttributes errorAttributes) {
		super(errorAttributes);
	}

	@RequestMapping(value = "/error", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Map<String, Object> handleError(HttpServletRequest request) {
		Map<String, Object> errorAttributes = super.getErrorAttributes(request, ErrorAttributeOptions.of(ErrorAttributeOptions.Include.STACK_TRACE));

		Object path = errorAttributes.get("path");
		Object status = errorAttributes.get("status");
		Object error = errorAttributes.get("error");
		Object message = errorAttributes.get("message");

		logger.warn("An error occurred while accessing {}: {} {}, {}", path, status, error, message);

		return errorAttributes;
	}
}

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import com.lakesidemutual.policymanagement.domain.customer.CustomerId;
import com.lakesidemutual.policymanagement.domain.insurancequoterequest.CustomerDecisionEvent;
import com.lakesidemutual.policymanagement.domain.insurancequoterequest.CustomerInfoEntity;
import com.lakesidemutual.policymanagement.domain.insurancequoterequest.InsuranceQuoteExpiredEvent;
import com.lakesidemutual.policymanagement.domain.insurancequoterequest.InsuranceQuoteRequestAggregateRoot;
import com.lakesidemutual.policymanagement.domain.insurancequoterequest.PolicyCreatedEvent;
import com.lakesidemutual.policymanagement.domain.insurancequoterequest.RequestStatus;
import com.lakesidemutual.policymanagement.domain.policy.InsuringAgreementEntity;
import com.lakesidemutual.policymanagement.domain.policy.MoneyAmount;
import com.lakesidemutual.policymanagement.domain.policy.PolicyAggregateRoot;
import com.lakesidemutual.policymanagement.domain.policy.PolicyId;
import com.lakesidemutual.policymanagement.domain.policy.PolicyPeriod;
import com.lakesidemutual.policymanagement.domain.policy.PolicyType;
import com.lakesidemutual.policymanagement.domain.policy.UpdatePolicyEvent;
import com.lakesidemutual.policymanagement.infrastructure.CustomerCoreRemoteProxy;
import com.lakesidemutual.policymanagement.infrastructure.CustomerSelfServiceMessageProducer;
import com.lakesidemutual.policymanagement.infrastructure.InsuranceQuoteRequestRepository;
import com.lakesidemutual.policymanagement.infrastructure.PolicyRepository;
import com.lakesidemutual.policymanagement.infrastructure.RiskManagementMessageProducer;
import com.lakesidemutual.policymanagement.interfaces.dtos.customer.CustomerDto;
import com.lakesidemutual.policymanagement.interfaces.dtos.policy.PolicyDto;

/**
 * CustomerDecisionMessageConsumer is a Spring component that consumes CustomerDecisionEvents
 * as they arrive through the ActiveMQ message queue. It processes these events by updating the
 * corresponding insurance quote requests.
 * */
@Component
public class CustomerDecisionMessageConsumer {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private InsuranceQuoteRequestRepository insuranceQuoteRequestRepository;

	@Autowired
	private PolicyRepository policyRepository;

	@Autowired
	private CustomerSelfServiceMessageProducer customerSelfServiceMessageProducer;

	@Autowired
	private RiskManagementMessageProducer riskManagementMessageProducer;

	@Autowired
	private CustomerCoreRemoteProxy customerCoreRemoteProxy;

	@JmsListener(destination = "${customerDecisionEvent.queueName}")
	public void receiveCustomerDecision(final Message<CustomerDecisionEvent> message) {
		logger.debug("A new CustomerDecisionEvent has been received.");
		final CustomerDecisionEvent customerDecisionEvent = message.getPayload();
		final Long id = customerDecisionEvent.getInsuranceQuoteRequestId();
		final Optional<InsuranceQuoteRequestAggregateRoot> insuranceQuoteRequestOpt = insuranceQuoteRequestRepository.findById(id);

		if(!insuranceQuoteRequestOpt.isPresent()) {
			logger.error("Unable to process a customer decision event with an invalid insurance quote request id.");
			return;
		}

		final InsuranceQuoteRequestAggregateRoot insuranceQuoteRequest = insuranceQuoteRequestOpt.get();
		final Date decisionDate = customerDecisionEvent.getDate();

		if(customerDecisionEvent.isQuoteAccepted()) {
			if(insuranceQuoteRequest.getStatus().equals(RequestStatus.QUOTE_EXPIRED) || insuranceQuoteRequest.hasQuoteExpired(decisionDate)) {
				/*
				 * If the quote has been accepted after it has already expired, we mark the quote as accepted
				 * and expired and send a InsuranceQuoteExpiredEvent back to the Customer Self-Service backend.
				 * */
				Date expirationDate;
				if(insuranceQuoteRequest.getStatus().equals(RequestStatus.QUOTE_EXPIRED)) {
					expirationDate = insuranceQuoteRequest.popStatus().getDate();
				} else {
					expirationDate = decisionDate;
				}

				insuranceQuoteRequest.acceptQuote(decisionDate);
				insuranceQuoteRequest.markQuoteAsExpired(expirationDate);
				InsuranceQuoteExpiredEvent event = new InsuranceQuoteExpiredEvent(expirationDate, insuranceQuoteRequest.getId());
				customerSelfServiceMessageProducer.sendInsuranceQuoteExpiredEvent(event);
			} else {
				logger.info("The insurance quote for request {} has been accepted", insuranceQuoteRequest.getId());
				insuranceQuoteRequest.acceptQuote(decisionDate);
				PolicyAggregateRoot policy = createPolicyForInsuranceQuoteRequest(insuranceQuoteRequest);
				String policyId = policy.getId().getId();
				policyRepository.save(policy);
				Date policyCreationDate = new Date();
				insuranceQuoteRequest.finalizeQuote(policyId, policyCreationDate);

				PolicyCreatedEvent policyCreatedEvent = new PolicyCreatedEvent(policyCreationDate, insuranceQuoteRequest.getId(), policyId);
				customerSelfServiceMessageProducer.sendPolicyCreatedEvent(policyCreatedEvent);

				CustomerInfoEntity customerInfo = insuranceQuoteRequest.getCustomerInfo();
				List<CustomerDto> customers = customerCoreRemoteProxy.getCustomersById(customerInfo.getCustomerId());
				if(!customers.isEmpty()) {
					CustomerDto customer = customers.get(0);
					final PolicyDto policyDto = PolicyDto.fromDomainObject(policy);
					final UpdatePolicyEvent event = new UpdatePolicyEvent("<customer-self-service-backend>", decisionDate, customer, policyDto);
					riskManagementMessageProducer.emitEvent(event);
				}
			}
		} else {
			/*
			 * If a quote has been rejected by the customer after it has already expired,
			 * we discard the QUOTE_EXPIRED status in favor of the QUOTE_REJECTED status.
			 * */
			if(insuranceQuoteRequest.getStatus().equals(RequestStatus.QUOTE_EXPIRED)) {
				insuranceQuoteRequest.popStatus();
			}

			logger.info("The insurance quote for request {} has been rejected", insuranceQuoteRequest.getId());
			insuranceQuoteRequest.rejectQuote(decisionDate);
		}

		insuranceQuoteRequestRepository.save(insuranceQuoteRequest);
	}

	private PolicyAggregateRoot createPolicyForInsuranceQuoteRequest(InsuranceQuoteRequestAggregateRoot insuranceQuoteRequest) {
		PolicyId policyId = PolicyId.random();
		CustomerId customerId = insuranceQuoteRequest.getCustomerInfo().getCustomerId();

		Date startDate = insuranceQuoteRequest.getInsuranceOptions().getStartDate();
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(startDate);
		calendar.add(Calendar.YEAR, 1);
		Date endDate = calendar.getTime();
		PolicyPeriod policyPeriod = new PolicyPeriod(startDate, endDate);

		PolicyType policyType = new PolicyType(insuranceQuoteRequest.getInsuranceOptions().getInsuranceType().getName());
		MoneyAmount deductible = insuranceQuoteRequest.getInsuranceOptions().getDeductible();
		MoneyAmount insurancePremium = insuranceQuoteRequest.getInsuranceQuote().getInsurancePremium();
		MoneyAmount policyLimit = insuranceQuoteRequest.getInsuranceQuote().getPolicyLimit();
		InsuringAgreementEntity insuringAgreement = new InsuringAgreementEntity(Collections.emptyList());
		return new PolicyAggregateRoot(policyId, customerId, new Date(), policyPeriod, policyType, deductible, policyLimit, insurancePremium, insuringAgreement);
	}
}


import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lakesidemutual.policymanagement.domain.insurancequoterequest.InsuranceQuoteEntity;
import com.lakesidemutual.policymanagement.domain.insurancequoterequest.InsuranceQuoteRequestAggregateRoot;
import com.lakesidemutual.policymanagement.domain.insurancequoterequest.InsuranceQuoteResponseEvent;
import com.lakesidemutual.policymanagement.domain.insurancequoterequest.RequestStatus;
import com.lakesidemutual.policymanagement.domain.policy.MoneyAmount;
import com.lakesidemutual.policymanagement.infrastructure.CustomerSelfServiceMessageProducer;
import com.lakesidemutual.policymanagement.infrastructure.InsuranceQuoteRequestRepository;
import com.lakesidemutual.policymanagement.interfaces.dtos.insurancequoterequest.InsuranceQuoteRequestDto;
import com.lakesidemutual.policymanagement.interfaces.dtos.insurancequoterequest.InsuranceQuoteRequestNotFoundException;
import com.lakesidemutual.policymanagement.interfaces.dtos.insurancequoterequest.InsuranceQuoteResponseDto;
import com.lakesidemutual.policymanagement.interfaces.dtos.policy.MoneyAmountDto;

/**
 * This REST controller gives clients access to the insurance quote requests. It is an example of the
 * <i>Information Holder Resource</i> pattern. This particular one is a special type of information holder called <i>Operational Data Holder</i>.
 *
 * @see <a href="https://www.microservice-api-patterns.org/patterns/responsibility/informationHolderEndpointTypes/OperationalDataHolder">Operational Data Holder</a>
 * 
 * As it supports responding to requests, you can also view it as a Processing Resource:
 * 
 *  * @see <a href="https://www.microservice-api-patterns.org/patterns/responsibility/endpointRoles/ProcessingResource">Processing Resource</a>
 *  
 *Matching RDD role stereotypes are <i>Coordinator</i> and <i>Information Holder</i>:
 *
 *  * @see <a href="http://www.wirfs-brock.com/PDFs/A_Brief-Tour-of-RDD.pdf">A Brief Tour of RDD</a>
 */
@RestController
@RequestMapping("/insurance-quote-requests")
public class InsuranceQuoteRequestProcessingResource {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private InsuranceQuoteRequestRepository insuranceQuoteRequestRepository;

	@Autowired
	private CustomerSelfServiceMessageProducer customerSelfServiceMessageProducer;

	@Operation(summary = "Get all Insurance Quote Requests.")
	@GetMapping
	public ResponseEntity<List<InsuranceQuoteRequestDto>> getInsuranceQuoteRequests() {
		logger.debug("Fetching all Insurance Quote Requests");
		List<InsuranceQuoteRequestAggregateRoot> quoteRequests = insuranceQuoteRequestRepository.findAllByOrderByDateDesc();
		List<InsuranceQuoteRequestDto> quoteRequestDtos = quoteRequests.stream().map(InsuranceQuoteRequestDto::fromDomainObject).collect(Collectors.toList());
		return ResponseEntity.ok(quoteRequestDtos);
	}

	@Operation(summary = "Get a specific Insurance Quote Request.")
	@GetMapping(value = "/{id}") /* MAP: Retrieval Operation */
	public ResponseEntity<InsuranceQuoteRequestDto> getInsuranceQuoteRequest(@Parameter(description = "the insurance quote request's unique id", required = true) @PathVariable Long id) {
		logger.debug("Fetching Insurance Quote Request with id '{}'", id);
		Optional<InsuranceQuoteRequestAggregateRoot> optInsuranceQuoteRequest = insuranceQuoteRequestRepository.findById(id);
		if (!optInsuranceQuoteRequest.isPresent()) {
			final String errorMessage = "Failed to find an Insurance Quote Request with id '{}'";
			logger.warn(errorMessage, id);
			throw new InsuranceQuoteRequestNotFoundException(errorMessage);
		}

		InsuranceQuoteRequestAggregateRoot insuranceQuoteRequest = optInsuranceQuoteRequest.get();
		InsuranceQuoteRequestDto insuranceQuoteRequestDto = InsuranceQuoteRequestDto.fromDomainObject(insuranceQuoteRequest);
		return ResponseEntity.ok(insuranceQuoteRequestDto);
	}

	@Operation(summary = "Updates the status of an existing Insurance Quote Request")
	@PatchMapping(value = "/{id}") /* MAP: State Transition Operation */
	public ResponseEntity<InsuranceQuoteRequestDto> respondToInsuranceQuoteRequest(
			@Parameter(description = "the insurance quote request's unique id", required = true) @PathVariable Long id,
			@Parameter(description = "the response that contains a new insurance quote if the request has been accepted", required = true)
			@Valid @RequestBody InsuranceQuoteResponseDto insuranceQuoteResponseDto) {

		logger.debug("Responding to Insurance Quote Request with id '{}'", id);

		Optional<InsuranceQuoteRequestAggregateRoot> optInsuranceQuoteRequest = insuranceQuoteRequestRepository.findById(id);
		if (!optInsuranceQuoteRequest.isPresent()) {
			final String errorMessage = "Failed to respond to Insurance Quote Request, because there is no Insurance Quote Request with id '{}'";
			logger.warn(errorMessage, id);
			throw new InsuranceQuoteRequestNotFoundException(errorMessage);
		}

		final Date date = new Date();
		final InsuranceQuoteRequestAggregateRoot insuranceQuoteRequest = optInsuranceQuoteRequest.get();
		if(insuranceQuoteResponseDto.getStatus().equals(RequestStatus.QUOTE_RECEIVED.toString())) {
			logger.info("Insurance Quote Request with id '{}' has been accepted", id);

			Date expirationDate = insuranceQuoteResponseDto.getExpirationDate();
			MoneyAmountDto insurancePremiumDto = insuranceQuoteResponseDto.getInsurancePremium();
			MoneyAmountDto policyLimitDto = insuranceQuoteResponseDto.getPolicyLimit();
			MoneyAmount insurancePremium = insurancePremiumDto.toDomainObject();
			MoneyAmount policyLimit = policyLimitDto.toDomainObject();
			InsuranceQuoteEntity insuranceQuote = new InsuranceQuoteEntity(expirationDate, insurancePremium, policyLimit);
			insuranceQuoteRequest.acceptRequest(insuranceQuote, date);
			final InsuranceQuoteResponseEvent insuranceQuoteResponseEvent = new InsuranceQuoteResponseEvent(date, insuranceQuoteRequest.getId(), true, expirationDate, insurancePremiumDto, policyLimitDto);
			customerSelfServiceMessageProducer.sendInsuranceQuoteResponseEvent(insuranceQuoteResponseEvent);
		} else if(insuranceQuoteResponseDto.getStatus().equals(RequestStatus.REQUEST_REJECTED.toString())) {
			logger.info("Insurance Quote Request with id '{}' has been rejected", id);

			insuranceQuoteRequest.rejectRequest(date);
			final InsuranceQuoteResponseEvent insuranceQuoteResponseEvent = new InsuranceQuoteResponseEvent(date, insuranceQuoteRequest.getId(), false, null, null, null);
			customerSelfServiceMessageProducer.sendInsuranceQuoteResponseEvent(insuranceQuoteResponseEvent);
		}
		insuranceQuoteRequestRepository.save(insuranceQuoteRequest);

		InsuranceQuoteRequestDto insuranceQuoteRequestDto = InsuranceQuoteRequestDto.fromDomainObject(insuranceQuoteRequest);
		return ResponseEntity.ok(insuranceQuoteRequestDto);
	}
}

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.List;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lakesidemutual.policymanagement.domain.customer.CustomerId;
import com.lakesidemutual.policymanagement.domain.policy.PolicyAggregateRoot;
import com.lakesidemutual.policymanagement.infrastructure.CustomerCoreRemoteProxy;
import com.lakesidemutual.policymanagement.infrastructure.PolicyRepository;
import com.lakesidemutual.policymanagement.interfaces.dtos.customer.CustomerDto;
import com.lakesidemutual.policymanagement.interfaces.dtos.customer.CustomerIdDto;
import com.lakesidemutual.policymanagement.interfaces.dtos.customer.CustomerNotFoundException;
import com.lakesidemutual.policymanagement.interfaces.dtos.customer.PaginatedCustomerResponseDto;
import com.lakesidemutual.policymanagement.interfaces.dtos.policy.PolicyDto;

/**
 * This REST controller gives clients access to the customer data. It is an example of the
 * <i>Information Holder Resource</i> pattern. This particular one is a special type of information holder called <i>Master Data Holder</i>.
 *
 * @see <a href="https://www.microservice-api-patterns.org/patterns/responsibility/endpointRoles/InformationHolderResource">Information Holder Resource</a>
 * @see <a href="https://www.microservice-api-patterns.org/patterns/responsibility/informationHolderEndpointTypes/MasterDataHolder">Master Data Holder</a>
 */
@RestController
@RequestMapping("/customers")
public class CustomerInformationHolder {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private PolicyRepository policyRepository;

	@Autowired
	private CustomerCoreRemoteProxy customerCoreRemoteProxy;

	@Operation(summary = "Get all customers.")
	@GetMapping
	public ResponseEntity<PaginatedCustomerResponseDto> getCustomers(
			@Parameter(description = "search terms to filter the customers by name", required = false) @RequestParam(value = "filter", required = false, defaultValue = "") String filter,
			@Parameter(description = "the maximum number of customers per page", required = false) @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit,
			@Parameter(description = "the offset of the page's first customer", required = false) @RequestParam(value = "offset", required = false, defaultValue = "0") Integer offset) {
		logger.debug("Fetching a page of customers (offset={},limit={},filter='{}')", offset, limit, filter);
		PaginatedCustomerResponseDto paginatedResponseIn = customerCoreRemoteProxy.getCustomers(filter, limit, offset);
		PaginatedCustomerResponseDto paginatedResponseOut = createPaginatedCustomerResponseDto(
				paginatedResponseIn.getFilter(),
				paginatedResponseIn.getLimit(),
				paginatedResponseIn.getOffset(),
				paginatedResponseIn.getSize(),
				paginatedResponseIn.getCustomers());
		return ResponseEntity.ok(paginatedResponseOut);
	}

	private PaginatedCustomerResponseDto createPaginatedCustomerResponseDto(String filter, Integer limit, Integer offset, int size, List<CustomerDto> customerDtos) {
		customerDtos.forEach(this::addCustomerLinks);
		PaginatedCustomerResponseDto paginatedCustomerResponseDto = new PaginatedCustomerResponseDto(filter, limit, offset, size, customerDtos);
		paginatedCustomerResponseDto.add(linkTo(methodOn(CustomerInformationHolder.class).getCustomers(filter, limit, offset)).withSelfRel());

		if (offset > 0) {
			paginatedCustomerResponseDto.add(linkTo(
					methodOn(CustomerInformationHolder.class).getCustomers(filter, limit, Math.max(0, offset - limit)))
					.withRel("prev"));
		}

		if (offset < size - limit) {
			paginatedCustomerResponseDto.add(linkTo(methodOn(CustomerInformationHolder.class).getCustomers(filter, limit, offset + limit))
					.withRel("next"));
		}

		return paginatedCustomerResponseDto;
	}

	/**
	 * The CustomerDto could contain a nested list containing the customer's policies. However, many clients may not be
	 * interested in the policies when they access the customer resource. To avoid sending large messages containing lots
	 * of data that is not or seldom needed we instead add a link to a separate endpoint which returns the customer's policies.
	 * This is an example of the <i>Linked Information Holder</i> pattern.
	 *
	 * @see <a href="https://www.microservice-api-patterns.org/patterns/quality/referenceManagement/LinkedInformationHolder">Linked Information Holder</a>
	 */
	private void addCustomerLinks(CustomerDto customerDto) {
		CustomerIdDto customerId = new CustomerIdDto(customerDto.getCustomerId());
		Link selfLink = linkTo(methodOn(CustomerInformationHolder.class).getCustomer(customerId)).withSelfRel();
		Link policiesLink = linkTo(methodOn(CustomerInformationHolder.class).getPolicies(customerId, "")).withRel("policies");
		customerDto.add(selfLink);
		customerDto.add(policiesLink);
	}

	/**
	 * Returns the customer with the given customer id. Example Usage:
	 *
	 * <pre>
	 * <code>
	 * GET http://localhost:8090/customers/rgpp0wkpec
	 *
	 * {
	 *   "customerId" : "rgpp0wkpec",
	 *   "firstname" : "Max",
	 *   "lastname" : "Mustermann",
	 *   "birthday" : "1989-12-31T23:00:00.000+0000",
	 *   "streetAddress" : "Oberseestrasse 10",
	 *   "postalCode" : "8640",
	 *   "city" : "Rapperswil",
	 *   "email" : "admin@example.com",
	 *   "phoneNumber" : "055 222 4111",
	 *   "moveHistory" : [ ]
	 * }
	 * </code>
	 * </pre>
	 * If the given customer id is not valid, an error response with HTTP Status Code 404 is returned. The response body contains additional
	 * information about the error in JSON form. This is an example of the <a href="https://www.microservice-api-patterns.org/patterns/quality/qualityManagementAndGovernance/ErrorReport">Error Report</a>
	 * pattern:
	 * <pre>
	 * <code>
	 * GET http://localhost:8090/customers/123
	 *
	 * {
	 *   "timestamp" : "2018-09-18T08:28:44.644+0000",
	 *   "status" : 404,
	 *   "error" : "Not Found",
	 *   "message" : "Failed to find a customer with id '123'.",
	 *   "path" : "/customers/123"
	 * }
	 * </code>
	 * </pre>
	 *
	 * @see <a href="https://www.microservice-api-patterns.org/patterns/quality/qualityManagementAndGovernance/ErrorReport">https://www.microservice-api-patterns.org/patterns/quality/qualityManagementAndGovernance/ErrorReport</a>
	 */
	@Operation(summary = "Get customer with a given customer id.")
	@GetMapping(value = "/{customerIdDto}")
	public ResponseEntity<CustomerDto> getCustomer(
			@Parameter(description = "the customer's unique id", required = true) @PathVariable CustomerIdDto customerIdDto) {
		CustomerId customerId = new CustomerId(customerIdDto.getId());
		logger.debug("Fetching a customer with id '{}'", customerId.getId());
		CustomerDto customer = customerCoreRemoteProxy.getCustomer(customerId);
		if(customer == null) {
			final String errorMessage = "Failed to find a customer with id '{}'";
			logger.warn(errorMessage, customerId.getId());
			throw new CustomerNotFoundException(errorMessage);
		}

		addCustomerLinks(customer);
		return ResponseEntity.ok(customer);
	}

	@Operation(summary = "Get a customer's policies.")
	@GetMapping(value = "/{customerIdDto}/policies")
	public ResponseEntity<List<PolicyDto>> getPolicies(
			@Parameter(description = "the customer's unique id", required = true) @PathVariable CustomerIdDto customerIdDto,
			@Parameter(description = "a comma-separated list of the fields that should be expanded in the response", required = false) @RequestParam(value = "expand", required = false, defaultValue = "") String expand) {
		CustomerId customerId = new CustomerId(customerIdDto.getId());
		logger.debug("Fetching policies for customer with id '{}' (fields='{}')", customerId.getId(), expand);
		List<PolicyAggregateRoot> policies = policyRepository.findAllByCustomerIdOrderByCreationDateDesc(customerId);
		List<PolicyDto> policyDtos = policies.stream().map(p -> createPolicyDto(p, expand)).collect(Collectors.toList());
		return ResponseEntity.ok(policyDtos);
	}

	private PolicyDto createPolicyDto(PolicyAggregateRoot policy, String expand) {
		PolicyDto policyDto = PolicyDto.fromDomainObject(policy);
		if(expand.equals("customer")) {
			CustomerDto customer = customerCoreRemoteProxy.getCustomer(policy.getCustomerId());
			policyDto.setCustomer(customer);
		}

		Link selfLink = linkTo(methodOn(PolicyInformationHolder.class).getPolicy(policy.getId(), expand)).withSelfRel();
		policyDto.add(selfLink);
		return policyDto;
	}
}



import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import com.lakesidemutual.policymanagement.domain.insurancequoterequest.CustomerInfoEntity;
import com.lakesidemutual.policymanagement.domain.insurancequoterequest.InsuranceOptionsEntity;
import com.lakesidemutual.policymanagement.domain.insurancequoterequest.InsuranceQuoteRequestAggregateRoot;
import com.lakesidemutual.policymanagement.domain.insurancequoterequest.InsuranceQuoteRequestEvent;
import com.lakesidemutual.policymanagement.domain.insurancequoterequest.RequestStatus;
import com.lakesidemutual.policymanagement.infrastructure.InsuranceQuoteRequestRepository;
import com.lakesidemutual.policymanagement.interfaces.dtos.insurancequoterequest.InsuranceQuoteRequestDto;
import com.lakesidemutual.policymanagement.interfaces.dtos.insurancequoterequest.RequestStatusChangeDto;

/**
 * InsuranceQuoteRequestMessageConsumer is a Spring component that consumes InsuranceQuoteRequestEvents
 * as they arrive through the ActiveMQ message queue. It processes these events by creating corresponding
 * InsuranceQuoteRequestAggregateRoot instances.
 * */
@Component
public class InsuranceQuoteRequestMessageConsumer {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private InsuranceQuoteRequestRepository insuranceQuoteRequestRepository;

	@JmsListener(destination = "${insuranceQuoteRequestEvent.queueName}")
	public void receiveInsuranceQuoteRequest(final Message<InsuranceQuoteRequestEvent> message) {
		logger.info("A new InsuranceQuoteRequestEvent has been received.");

		InsuranceQuoteRequestEvent insuranceQuoteRequestEvent = message.getPayload();
		InsuranceQuoteRequestDto insuranceQuoteRequestDto = insuranceQuoteRequestEvent.getInsuranceQuoteRequestDto();
		Long id = insuranceQuoteRequestDto.getId();
		Date date = insuranceQuoteRequestDto.getDate();
		List<RequestStatusChangeDto> statusHistory = insuranceQuoteRequestDto.getStatusHistory();
		RequestStatus status = RequestStatus.valueOf(statusHistory.get(statusHistory.size()-1).getStatus());

		CustomerInfoEntity customerInfo = insuranceQuoteRequestDto.getCustomerInfo().toDomainObject();
		InsuranceOptionsEntity insuranceOptions = insuranceQuoteRequestDto.getInsuranceOptions().toDomainObject();

		InsuranceQuoteRequestAggregateRoot insuranceQuoteAggregateRoot = new InsuranceQuoteRequestAggregateRoot(id, date, status, customerInfo, insuranceOptions, null, null);
		insuranceQuoteRequestRepository.save(insuranceQuoteAggregateRoot);
	}
}


import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Date;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lakesidemutual.policymanagement.interfaces.dtos.risk.RiskFactorRequestDto;
import com.lakesidemutual.policymanagement.interfaces.dtos.risk.RiskFactorResponseDto;

/**
 * This REST controller allows clients to compute the risk factor for a given customer. It is an application of
 * the <a href="https://www.microservice-api-patterns.org/patterns/responsibility/operationResponsibilities/ComputationFunction">Computation Function</a> pattern.
 * A Computation Service performs a function f that only depends on its input parameters and does not alter the state of the server.
 *
 * @see <a href=
 *      "https://www.microservice-api-patterns.org/patterns/responsibility/operationResponsibilities/ComputationFunction">https://www.microservice-api-patterns.org/patterns/responsibility/operationResponsibilities/ComputationFunction</a>
 */
@RestController
@RequestMapping("/riskfactor")
public class RiskComputationService {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Operation(summary = "Computes the risk factor for a given customer.")
	@PostMapping(value = "/compute")
	public ResponseEntity<RiskFactorResponseDto> computeRiskFactor(
			@Parameter(description = "the request containing relevant customer attributes (e.g., postal code, birthday)", required = true)
			@Valid
			@RequestBody
			RiskFactorRequestDto riskFactorRequest) {
		int age = getAge(riskFactorRequest.getBirthday());
		String postalCode = riskFactorRequest.getPostalCode();
		logger.debug("Computing risk factor (age={}, postal-code={})", age, postalCode);
		int riskFactor = computeRiskFactor(age, postalCode);
		return ResponseEntity.ok(new RiskFactorResponseDto(riskFactor));
	}

	private int getAge(Date birthday) {
		LocalDate birthdayLocalDate = birthday.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		LocalDate now = LocalDate.now();
		return Period.between(birthdayLocalDate, now).getYears();
	}

	private int computeRiskFactor(int age, String postalCode) {
		int ageGroupRiskFactor = computeAgeGroupRiskFactor(age);
		int localityRiskFactor = computeLocalityRiskFactor(postalCode);
		return (ageGroupRiskFactor + localityRiskFactor) / 2;
	}

	private int computeAgeGroupRiskFactor(int age) {
		if(age > 90) {
			return 100;
		} else if(age > 70) {
			return 90;
		} else if(age > 60) {
			return 70;
		} else if(age > 50) {
			return 60;
		} else if(age > 40) {
			return 50;
		} else if(age > 25) {
			return 20;
		} else {
			return 40;
		}
	}

	private int computeLocalityRiskFactor(String postalCodeStr) {
		try {
			int postalCode = Integer.parseInt(postalCodeStr);
			if((postalCode >= 8000 && postalCode < 9000) || (postalCode >= 1000 && postalCode < 2000)) {
				return 80;
			} else if(postalCode >= 5000 && postalCode < 6000) {
				return 10;
			} else {
				return 30;
			}
		} catch(NumberFormatException e) {
			return 0;
		}
	}
}

import java.io.IOException;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

@Component
public class HeaderRequestInterceptor implements ClientHttpRequestInterceptor {
	@Value("${apikey.header}")
	private String apiKeyHeader;

	@Value("${apikey.value}")
	private String apiKeyValue;

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
			throws IOException {
		final HttpHeaders httpHeaders = request.getHeaders();
		httpHeaders.set(apiKeyHeader, "Bearer " + apiKeyValue);
		httpHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
		return execution.execute(request, body);
	}
}

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;

/**
 * Provides a RestTemplate that can be injected into other components. The
 * RestTemplate uses the configured API Key when making a request.
 */
@Configuration
@Profile("default")
public class DefaultAuthenticatedRestTemplateClient {
	@Autowired
	private HeaderRequestInterceptor headerRequestInterceptor;

	@Bean
	public RestTemplate restTemplate() {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setInterceptors(Arrays.asList(headerRequestInterceptor));
		return restTemplate;
	}
}

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The SwaggerConfiguration class configures the HTTP resource API documentation
 * that is generated by Springfox.
 */
@Configuration
public class SwaggerConfiguration {

	@Bean
	public OpenAPI customerSelfServiceApi() {
		return new OpenAPI()
				.info(new Info().title("PolicyManagement API")
						.description("This API allows LM staff to manage the policies of their customers.")
						.version("v1.0.0")
						.license(new License().name("Apache 2.0")));
	}
}


import jakarta.servlet.Filter;
import jakarta.servlet.Servlet;
import org.apache.catalina.servlets.DefaultServlet;
import org.h2.server.web.JakartaWebServlet;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

/**
 * The WebConfiguration class is used to customize the default Spring MVC configuration.
 */
@Configuration
public class WebConfiguration {
    /**
     * This web servlet makes the web console of the H2 database engine available at the "/console" endpoint.
     */
    @Bean
    public ServletRegistrationBean<JakartaWebServlet> h2servletRegistration() {
        ServletRegistrationBean<JakartaWebServlet> registrationBean = new ServletRegistrationBean<>(new JakartaWebServlet());
        registrationBean.addUrlMappings("/console/*");
        return registrationBean;
    }

    /**
     * This is a filter that generates an ETag value based on the content of the response. This ETag is compared to the If-None-Match header
     * of the request. If these headers are equal, the response content is not sent, but rather a 304 "Not Modified" status instead.
     */
    @Bean
    public Filter shallowETagHeaderFilter() {
        return new ShallowEtagHeaderFilter();
    }
}

import com.lakesidemutual.policymanagement.domain.insurancequoterequest.CustomerDecisionEvent;
import com.lakesidemutual.policymanagement.domain.insurancequoterequest.InsuranceQuoteRequestEvent;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerPlugin;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.security.SimpleAuthenticationPlugin;
import org.apache.activemq.usage.SystemUsage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

import java.util.HashMap;
import java.util.Map;

/**
 * The MessagingConfiguration class configures the ActiveMQ message broker. This broker is used
 * to send events to the Risk Management Server when a policy changes.
 */
@Configuration
public class MessagingConfiguration {
    @Value("${policymanagement.stompBrokerBindAddress}")
    private String stompBrokerBindAddress;

    @Value("${policymanagement.tcpBrokerBindAddress}")
    private String tcpBrokerBindAddress;

    @Value("${spring.activemq.user}")
    private String username;

    @Value("${spring.activemq.password}")
    private String password;

    @Bean
    public BrokerService broker() throws Exception {
        final BrokerService broker = new BrokerService();
        broker.addConnector(stompBrokerBindAddress);
        broker.addConnector(tcpBrokerBindAddress);
        broker.setPersistent(true);
        broker.setUseJmx(true);

        // Set store limit
        SystemUsage systemUsage = broker.getSystemUsage();
        systemUsage.getStoreUsage().setLimit(100 * 1024 * 1024); // Set store limit to 100 MB

        final Map<String, String> userPasswords = new HashMap<>();
        userPasswords.put(username, password);
        SimpleAuthenticationPlugin authenticationPlugin = new SimpleAuthenticationPlugin();
        authenticationPlugin.setUserPasswords(userPasswords);
        broker.setPlugins(new BrokerPlugin[]{authenticationPlugin});
        return broker;
    }

    /**
     * The @Bean attribute turns the returned object (in this case a MessageConverter) into a Spring bean.
     * A bean is an object that is instantiated, assembled, and otherwise managed by a Spring container.
     * Other classes can use dependency injection (e.g., using the @Autowired annotation) to obtain a reference
     * to a specific bean.
     *
     * @see <a href=
     * "https://docs.spring.io/spring-javaconfig/docs/1.0.0.M4/reference/html/ch02s02.html">https://docs.spring.io/spring-javaconfig/docs/1.0.0.M4/reference/html/ch02s02.html</a>
     */
    @Bean
    public MessageConverter jacksonJmsMessageConverter() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");

        final Map<String, Class<?>> typeIdMappings = new HashMap<>();
        typeIdMappings.put("com.lakesidemutual.customerselfservice.domain.insurancequoterequest.InsuranceQuoteRequestEvent", InsuranceQuoteRequestEvent.class);
        typeIdMappings.put("com.lakesidemutual.customerselfservice.domain.insurancequoterequest.CustomerDecisionEvent", CustomerDecisionEvent.class);
        converter.setTypeIdMappings(typeIdMappings);
        return converter;
    }

    @Bean
    public ActiveMQConnectionFactory connectionFactory() {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory();
        connectionFactory.setTrustAllPackages(true);
        connectionFactory.setUserName(username);
        connectionFactory.setPassword(password);
        return connectionFactory;
    }

    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory() {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory());
        factory.setConcurrency("1-1");
        factory.setMessageConverter(jacksonJmsMessageConverter());
        return factory;
    }

    @Bean
    public JmsTemplate jmsTemplate() {
        JmsTemplate template = new JmsTemplate();
        template.setMessageConverter(jacksonJmsMessageConverter());
        template.setConnectionFactory(connectionFactory());
        return template;
    }
}

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * The WebSecurityConfiguration class configures the security policies used for the exposed HTTP resource API.
 * In this case, the API is accessible without authentication.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfiguration {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)
                        .cacheControl(HeadersConfigurer.CacheControlConfig::disable))
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((request, response, exception) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"))
                        .accessDeniedHandler((request, response, exception) -> response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden"))
                )
                .sessionManagement(sessionManagement -> sessionManagement
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        return http.build();
    }

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("authorization", "content-type", "x-auth-token"));
        configuration.setExposedHeaders(Arrays.asList("x-auth-token"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}


import java.io.IOException;
import java.util.Random;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * This RequestTracingFilter class generates a new random ID for each request and stores
 * it in the MDC (Mapped Diagnostic Context). The ID will then appear in each log entry
 * that is logged as a result of the corresponding request. This can help correlate different
 * log entries during debugging.
 * */
@Component
@Order(1)
public class RequestTracingFilter implements Filter {
	private final static String REQUEST_ID_KEY = "requestId";
	private final Random rand = new Random();

	private String createRequestId() {
		return Integer.toString(rand.nextInt(9999));
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		MDC.put(REQUEST_ID_KEY, createRequestId());
		chain.doFilter(request, response);
	}
}


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * You might be wondering why this Exception class is in the dtos package. Exceptions of this type
 * are thrown whenever the client tries to create a policy for an unknown customer.
 * Spring will then convert this exception into an HTTP 424 response.
 * */
@ResponseStatus(code = HttpStatus.FAILED_DEPENDENCY)
public class UnknownCustomerException extends RuntimeException {
	private static final long serialVersionUID = -7187275179643436311L;

	public UnknownCustomerException(String errorMessage) {
		super(errorMessage);
	}
}

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * You might be wondering why this Exception class is in the dtos package. Exceptions of this type
 * are thrown whenever the Policy Management backend can't connect to the Customer Core. Spring will then
 * convert this exception into an HTTP 502 response.
 * */
@ResponseStatus(code = HttpStatus.BAD_GATEWAY)
public class CustomerCoreNotAvailableException extends RuntimeException {
	private static final long serialVersionUID = 2146599135907479601L;

	public CustomerCoreNotAvailableException(String errorMessage) {
		super(errorMessage);
	}
}

import java.util.List;

import org.springframework.hateoas.RepresentationModel;

/**
 * The PaginatedPolicyResponseDto holds a collection of PolicyDto
 * with additional metadata parameters such as the limit, offset and size that
 * are used in the <a href=
 * "https://www.microservice-api-patterns.org/patterns/structure/compositeRepresentations/Pagination">Pagination</a>
 * pattern, specifically the <em>Offset-Based</em> Pagination variant.
 */
public class PaginatedPolicyResponseDto extends RepresentationModel {
	private final int limit;
	private final int offset;
	private final int size;
	private final List<PolicyDto> policies;

	public PaginatedPolicyResponseDto(int limit, int offset, int size, List<PolicyDto> policies) {
		this.limit = limit;
		this.offset = offset;
		this.size = size;
		this.policies = policies;
	}

	public List<PolicyDto> getPolicies() {
		return policies;
	}

	public int getLimit() {
		return limit;
	}

	public int getOffset() {
		return offset;
	}

	public int getSize() {
		return size;
	}
}

import java.util.Date;

import org.springframework.hateoas.RepresentationModel;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lakesidemutual.policymanagement.domain.policy.PolicyAggregateRoot;

/**
 * The PolicyDto class is a data transfer object (DTO) that represents a single insurance policy.
 * It inherits from the ResourceSupport class which allows us to create a REST representation (e.g., JSON, XML)
 * that follows the HATEOAS principle. For example, links can be added to the representation (e.g., self, address.change)
 * which means that future actions the client may take can be discovered from the resource representation.
 *
 * @see <a href="https://docs.spring.io/spring-hateoas/docs/current/reference/html/">Spring HATEOAS - Reference Documentation</a>
 */
public class PolicyDto extends RepresentationModel {
	private String policyId;
	private Object customer;
	private Date creationDate;
	private PolicyPeriodDto policyPeriod;
	private String policyType;
	private MoneyAmountDto deductible;
	private MoneyAmountDto policyLimit;
	private MoneyAmountDto insurancePremium;
	private InsuringAgreementDto insuringAgreement;
	@JsonProperty("_expandable")
	private String[] expandable;

	public PolicyDto() {}

	public PolicyDto(
			String policyId,
			Object customer,
			Date creationDate,
			PolicyPeriodDto policyPeriod,
			String policyType,
			MoneyAmountDto deductible,
			MoneyAmountDto policyLimit,
			MoneyAmountDto insurancePremium,
			InsuringAgreementDto insuringAgreement) {
		this.policyId = policyId;
		this.customer = customer;
		this.creationDate = creationDate;
		this.policyPeriod = policyPeriod;
		this.policyType = policyType;
		this.deductible = deductible;
		this.policyLimit = policyLimit;
		this.insurancePremium = insurancePremium;
		this.insuringAgreement = insuringAgreement;
		this.expandable = new String[]{"customer"};
	}

	public static PolicyDto fromDomainObject(PolicyAggregateRoot policy) {
		return new PolicyDto(
				policy.getId().getId(),
				policy.getCustomerId().getId(),
				policy.getCreationDate(),
				PolicyPeriodDto.fromDomainObject(policy.getPolicyPeriod()),
				policy.getPolicyType().getName(),
				MoneyAmountDto.fromDomainObject(policy.getDeductible()),
				MoneyAmountDto.fromDomainObject(policy.getPolicyLimit()),
				MoneyAmountDto.fromDomainObject(policy.getInsurancePremium()),
				InsuringAgreementDto.fromDomainObject(policy.getInsuringAgreement())
				);
	}

	public Object getCustomer() {
		return customer;
	}

	public void setCustomer(Object customer) {
		this.customer = customer;
	}

	public String getPolicyId() {
		return policyId;
	}

	public void setPolicyId(String policyId) {
		this.policyId = policyId;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public PolicyPeriodDto getPolicyPeriod() {
		return policyPeriod;
	}

	public void setPolicyPeriod(PolicyPeriodDto policyPeriod) {
		this.policyPeriod = policyPeriod;
	}

	public String getPolicyType() {
		return policyType;
	}

	public void setPolicyType(String policyType) {
		this.policyType = policyType;
	}

	public MoneyAmountDto getDeductible() {
		return deductible;
	}

	public void setDeductible(MoneyAmountDto deductible) {
		this.deductible = deductible;
	}

	public MoneyAmountDto getPolicyLimit() {
		return policyLimit;
	}

	public void setPolicyLimit(MoneyAmountDto policyLimit) {
		this.policyLimit = policyLimit;
	}

	public MoneyAmountDto getInsurancePremium() {
		return insurancePremium;
	}

	public void setInsurancePremium(MoneyAmountDto insurancePremium) {
		this.insurancePremium = insurancePremium;
	}

	public InsuringAgreementDto getInsuringAgreement() {
		return insuringAgreement;
	}

	public void setInsuringAgreement(InsuringAgreementDto insuringAgreement) {
		this.insuringAgreement = insuringAgreement;
	}

	public String[] getExpandable() {
		return expandable;
	}

	public void setExpandable(String[] expandable) {
		this.expandable = expandable;
	}
}


import java.math.BigDecimal;
import java.util.Currency;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.lakesidemutual.policymanagement.domain.policy.MoneyAmount;

/**
 * MoneyAmountDto is a data transfer object (DTO) that represents an amount of money in a specific currency.
 */
public class MoneyAmountDto {
	@NotNull
	@DecimalMax(value = "1000000000000", inclusive = false)
	@DecimalMin("0")
	private BigDecimal amount;

	@NotEmpty
	private String currency;

	public MoneyAmountDto() {
	}

	public MoneyAmountDto(BigDecimal amount, String currency) {
		this.amount = amount;
		this.currency = currency;
	}

	public static MoneyAmountDto fromDomainObject(MoneyAmount moneyAmount) {
		return new MoneyAmountDto(moneyAmount.getAmount(), moneyAmount.getCurrency().toString());
	}

	public MoneyAmount toDomainObject() {
		return new MoneyAmount(amount, Currency.getInstance(currency));
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public String getCurrency() {
		return currency;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}
}


import java.util.Date;

import jakarta.validation.constraints.NotNull;

import com.lakesidemutual.policymanagement.domain.policy.PolicyPeriod;

/**
 * PolicyPeriodDto is a data transfer object (DTO) that represents the period during which a policy is valid.
 * */
public class PolicyPeriodDto {
	@NotNull
	private Date startDate;

	@NotNull
	private Date endDate;

	public PolicyPeriodDto() {
	}

	public PolicyPeriodDto(Date startDate, Date endDate) {
		this.startDate = startDate;
		this.endDate = endDate;
	}

	public static PolicyPeriodDto fromDomainObject(PolicyPeriod policyPeriod) {
		return new PolicyPeriodDto(policyPeriod.getStartDate(), policyPeriod.getEndDate());
	}

	public PolicyPeriod toDomainObject() {
		return new PolicyPeriod(startDate, endDate);
	}

	public Date getStartDate() {
		return startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}
}


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * CreatePolicyRequestDto is a data transfer object (DTO) that is sent to the Policy Management backend when a
 * Lakeside Mutual employee creates a new policy for a customer.
 */
public class CreatePolicyRequestDto {
	@Valid
	private String customerId;

	@Valid
	private PolicyPeriodDto policyPeriod;

	@Valid
	@NotNull
	private String policyType;

	@Valid
	private MoneyAmountDto deductible;

	@Valid
	private MoneyAmountDto policyLimit;

	@Valid
	private MoneyAmountDto insurancePremium;

	@Valid
	private InsuringAgreementDto insuringAgreement;

	public CreatePolicyRequestDto() {}

	public CreatePolicyRequestDto(
			String customerId,
			PolicyPeriodDto policyPeriod,
			String policyType,
			MoneyAmountDto deductible,
			MoneyAmountDto policyLimit,
			MoneyAmountDto insurancePremium,
			InsuringAgreementDto insuringAgreement) {
		this.customerId = customerId;
		this.policyPeriod = policyPeriod;
		this.policyType = policyType;
		this.deductible = deductible;
		this.policyLimit = policyLimit;
		this.insurancePremium = insurancePremium;
		this.insuringAgreement = insuringAgreement;
	}

	public String getCustomerId() {
		return customerId;
	}

	public PolicyPeriodDto getPolicyPeriod() {
		return policyPeriod;
	}

	public String getPolicyType() {
		return policyType;
	}

	public MoneyAmountDto getDeductible() {
		return deductible;
	}

	public MoneyAmountDto getPolicyLimit() {
		return policyLimit;
	}

	public MoneyAmountDto getInsurancePremium() {
		return insurancePremium;
	}

	public InsuringAgreementDto getInsuringAgreement() {
		return insuringAgreement;
	}
}


import java.util.List;
import java.util.stream.Collectors;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.lakesidemutual.policymanagement.domain.policy.InsuringAgreementEntity;
import com.lakesidemutual.policymanagement.domain.policy.InsuringAgreementItem;

/**
 * InsuringAgreementDto is a data transfer object (DTO) that represents the
 * insuring agreement between a customer and Lakeside Mutual.
 */
public class InsuringAgreementDto {
	@Valid
	@NotNull
	private final List<InsuringAgreementItemDto> agreementItems;

	public InsuringAgreementDto() {
		this.agreementItems = null;
	}

	public InsuringAgreementDto(List<InsuringAgreementItemDto> agreementItems) {
		this.agreementItems = agreementItems;
	}

	public static InsuringAgreementDto fromDomainObject(InsuringAgreementEntity insuringAgreement) {
		List<InsuringAgreementItemDto> insuringAgreementItemDtos = insuringAgreement.getAgreementItems().stream()
				.map(InsuringAgreementItemDto::fromDomainObject)
				.collect(Collectors.toList());
		return new InsuringAgreementDto(insuringAgreementItemDtos);
	}

	public InsuringAgreementEntity toDomainObject() {
		List<InsuringAgreementItem> insuringAgreementItems = getAgreementItems().stream()
				.map(InsuringAgreementItemDto::toDomainObject)
				.collect(Collectors.toList());
		return new InsuringAgreementEntity(insuringAgreementItems);
	}

	public List<InsuringAgreementItemDto> getAgreementItems() {
		return agreementItems;
	}
}


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import com.lakesidemutual.policymanagement.domain.policy.InsuringAgreementItem;

/**
 * InsuringAgreementItemDto is a data transfer object (DTO) that is used to represent a single item in an insuring agreement.
 */
public class InsuringAgreementItemDto {
	@Valid
	@NotEmpty
	private final String title;

	@Valid
	@NotEmpty
	private final String description;

	public InsuringAgreementItemDto() {
		this.title = null;
		this.description = null;
	}

	private InsuringAgreementItemDto(String title, String description) {
		this.title = title;
		this.description = description;
	}

	public static InsuringAgreementItemDto fromDomainObject(InsuringAgreementItem item) {
		return new InsuringAgreementItemDto(item.getTitle(), item.getDescription());
	}

	public InsuringAgreementItem toDomainObject() {
		return new InsuringAgreementItem(title, description);
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}
}


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * You might be wondering why this Exception class is in the dtos package. Exceptions of this type
 * are thrown whenever the client sends a request with an invalid policy id. Spring will then
 * convert this exception into an HTTP 404 response.
 * */
@ResponseStatus(code = HttpStatus.NOT_FOUND)
public class PolicyNotFoundException extends RuntimeException {
	private static final long serialVersionUID = -328902266721157272L;

	public PolicyNotFoundException(String errorMessage) {
		super(errorMessage);
	}
}


import java.util.Date;

/**
 * RiskFactorRequestDto is a data transfer object (DTO) that is sent to the Policy Management backend
 * when a client wants to compute the risk factor of a specific customer.
 * */
public class RiskFactorRequestDto {
	private Date birthday;
	private String postalCode;

	public RiskFactorRequestDto() {}

	public RiskFactorRequestDto(Date birthday, String postalCode) {
		this.setBirthday(birthday);
		this.setPostalCode(postalCode);
	}

	public Date getBirthday() {
		return birthday;
	}

	public void setBirthday(Date birthday) {
		this.birthday = birthday;
	}

	public String getPostalCode() {
		return postalCode;
	}

	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}
}


/**
 * RiskFactorResponseDto is a data transfer object (DTO) that contains the risk factor
 * that was computed for a specific customer.
 * */
public class RiskFactorResponseDto {
	private int riskFactor;

	public RiskFactorResponseDto(int riskFactor) {
		this.riskFactor = riskFactor;
	}

	public int getRiskFactor() {
		return riskFactor;
	}
}


import java.util.Date;

import com.lakesidemutual.policymanagement.domain.insurancequoterequest.InsuranceOptionsEntity;
import com.lakesidemutual.policymanagement.domain.insurancequoterequest.InsuranceType;
import com.lakesidemutual.policymanagement.interfaces.dtos.policy.MoneyAmountDto;

/**
 * InsuranceOptionsDto is a data transfer object (DTO) that contains the insurance options
 * (e.g., start date, insurance type, etc.) that a customer selected for an Insurance Quote Request.
 */
public class InsuranceOptionsDto {
	private Date startDate;
	private String insuranceType;
	private MoneyAmountDto deductible;

	public InsuranceOptionsDto() {
	}

	private InsuranceOptionsDto(Date startDate, String insuranceType, MoneyAmountDto deductible) {
		this.startDate = startDate;
		this.insuranceType = insuranceType;
		this.deductible = deductible;
	}

	public static InsuranceOptionsDto fromDomainObject(InsuranceOptionsEntity insuranceOptions) {
		Date startDate = insuranceOptions.getStartDate();
		InsuranceType insuranceType = insuranceOptions.getInsuranceType();
		String insuranceTypeDto = insuranceType.getName();
		MoneyAmountDto deductibleDto = MoneyAmountDto.fromDomainObject(insuranceOptions.getDeductible());
		return new InsuranceOptionsDto(startDate, insuranceTypeDto, deductibleDto);
	}

	public InsuranceOptionsEntity toDomainObject() {
		return new InsuranceOptionsEntity(startDate, new InsuranceType(insuranceType), deductible.toDomainObject());
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public String getInsuranceType() {
		return insuranceType;
	}

	public void setInsuranceType(String insuranceType) {
		this.insuranceType = insuranceType;
	}

	public MoneyAmountDto getDeductible() {
		return deductible;
	}

	public void setDeductible(MoneyAmountDto deductible) {
		this.deductible = deductible;
	}
}


import java.util.Date;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import com.lakesidemutual.policymanagement.interfaces.dtos.policy.MoneyAmountDto;

/**
 * InsuranceQuoteResponseDto is a data transfer object (DTO) that contains Lakeside Mutual's
 * response to a specific insurance quote request.
 * */
public class InsuranceQuoteResponseDto {
	@Valid
	@NotNull
	private String status;

	@Valid
	@Future
	private Date expirationDate;

	@Valid
	private MoneyAmountDto insurancePremium;

	@Valid
	private MoneyAmountDto policyLimit;

	public InsuranceQuoteResponseDto() {
	}

	public InsuranceQuoteResponseDto(String status, Date expirationDate, MoneyAmountDto insurancePremium, MoneyAmountDto policyLimit) {
		this.status = status;
		this.expirationDate = expirationDate;
		this.insurancePremium = insurancePremium;
		this.policyLimit = policyLimit;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Date getExpirationDate() {
		return expirationDate;
	}

	public void setExpirationDate(Date expirationDate) {
		this.expirationDate = expirationDate;
	}

	public MoneyAmountDto getInsurancePremium() {
		return insurancePremium;
	}

	public void setInsurancePremium(MoneyAmountDto insurancePremium) {
		this.insurancePremium = insurancePremium;
	}

	public MoneyAmountDto getPolicyLimit() {
		return policyLimit;
	}

	public void setPolicyLimit(MoneyAmountDto policyLimit) {
		this.policyLimit = policyLimit;
	}
}

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.lakesidemutual.policymanagement.domain.insurancequoterequest.InsuranceQuoteRequestAggregateRoot;

/**
 * InsuranceQuoteRequestDto is a data transfer object (DTO) that represents a request
 * by a customer for a new insurance quote.
 */
public class InsuranceQuoteRequestDto {
	private Long id;

	@Valid
	private Date date;

	@Valid
	private List<RequestStatusChangeDto> statusHistory;

	@Valid
	@NotNull
	private CustomerInfoDto customerInfo;

	@Valid
	@NotNull
	private InsuranceOptionsDto insuranceOptions;

	@Valid
	private InsuranceQuoteDto insuranceQuote;

	private String policyId;

	public InsuranceQuoteRequestDto() {
	}

	public InsuranceQuoteRequestDto(Long id, Date date, List<RequestStatusChangeDto> statusHistory, CustomerInfoDto customerInfo, InsuranceOptionsDto insuranceOptions, InsuranceQuoteDto insuranceQuote, String policyId) {
		this.id = id;
		this.date = date;
		this.statusHistory = statusHistory;
		this.customerInfo = customerInfo;
		this.insuranceOptions = insuranceOptions;
		this.insuranceQuote = insuranceQuote;
		this.policyId = policyId;
	}

	public static InsuranceQuoteRequestDto fromDomainObject(InsuranceQuoteRequestAggregateRoot insuranceQuoteRequest) {
		Long id = insuranceQuoteRequest.getId();
		Date date = insuranceQuoteRequest.getDate();
		List<RequestStatusChangeDto> statusHistory = insuranceQuoteRequest.getStatusHistory().stream()
				.map(RequestStatusChangeDto::fromDomainObject)
				.collect(Collectors.toList());
		CustomerInfoDto customerInfoDto = CustomerInfoDto.fromDomainObject(insuranceQuoteRequest.getCustomerInfo());
		InsuranceOptionsDto insuranceOptionsDto = InsuranceOptionsDto.fromDomainObject(insuranceQuoteRequest.getInsuranceOptions());
		InsuranceQuoteDto insuranceQuoteDto = insuranceQuoteRequest.getInsuranceQuote() != null ? InsuranceQuoteDto.fromDomainObject(insuranceQuoteRequest.getInsuranceQuote()) : null;
		String policyId = insuranceQuoteRequest.getPolicyId();
		return new InsuranceQuoteRequestDto(id, date, statusHistory, customerInfoDto, insuranceOptionsDto, insuranceQuoteDto, policyId);
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public List<RequestStatusChangeDto> getStatusHistory() {
		return statusHistory;
	}

	public void setStatusHistory(List<RequestStatusChangeDto> statusHistory) {
		this.statusHistory = statusHistory;
	}

	public CustomerInfoDto getCustomerInfo() {
		return customerInfo;
	}

	public void setCustomerInfo(CustomerInfoDto customerInfo) {
		this.customerInfo = customerInfo;
	}

	public InsuranceOptionsDto getInsuranceOptions() {
		return insuranceOptions;
	}

	public void setInsuranceOptions(InsuranceOptionsDto insuranceOptions) {
		this.insuranceOptions = insuranceOptions;
	}

	public InsuranceQuoteDto getInsuranceQuote() {
		return insuranceQuote;
	}

	public void setInsuranceQuote(InsuranceQuoteDto insuranceQuote) {
		this.insuranceQuote = insuranceQuote;
	}

	public String getPolicyId() {
		return policyId;
	}

	public void setPolicyId(String policyId) {
		this.policyId = policyId;
	}
}

import java.util.Date;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import com.lakesidemutual.policymanagement.domain.insurancequoterequest.RequestStatusChange;

/**
 * RequestStatusChangeDto is a data transfer object (DTO) that represents a status change of an insurance quote request.
 */
public class RequestStatusChangeDto {
	@Valid
	private Date date;

	@NotEmpty
	private String status;

	public RequestStatusChangeDto() {
	}

	public RequestStatusChangeDto(Date date, String status) {
		this.date = date;
		this.status = status;
	}

	public static RequestStatusChangeDto fromDomainObject(RequestStatusChange requestStatusChange) {
		return new RequestStatusChangeDto(requestStatusChange.getDate(), requestStatusChange.getStatus().name());
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}

import com.lakesidemutual.policymanagement.domain.customer.CustomerId;
import com.lakesidemutual.policymanagement.domain.insurancequoterequest.CustomerInfoEntity;
import com.lakesidemutual.policymanagement.interfaces.dtos.customer.AddressDto;

/**
 * CustomerInfoDto is a data transfer object (DTO) that represents the
 * customer infos that are part of an Insurance Quote Request.
 * */
public class CustomerInfoDto {
	private String customerId;
	private String firstname;
	private String lastname;
	private AddressDto contactAddress;
	private AddressDto billingAddress;

	public CustomerInfoDto() {
	}

	private CustomerInfoDto(String customerId, String firstname, String lastname, AddressDto contactAddress, AddressDto billingAddress) {
		this.customerId = customerId;
		this.firstname = firstname;
		this.lastname = lastname;
		this.contactAddress = contactAddress;
		this.billingAddress = billingAddress;
	}

	public static CustomerInfoDto fromDomainObject(CustomerInfoEntity customerInfo) {
		String customerId = customerInfo.getCustomerId().getId();
		String firstname = customerInfo.getFirstname();
		String lastname = customerInfo.getLastname();
		AddressDto contactAddressDto = AddressDto.fromDomainObject(customerInfo.getContactAddress());
		AddressDto billingAddressDto = AddressDto.fromDomainObject(customerInfo.getBillingAddress());
		return new CustomerInfoDto(customerId, firstname, lastname, contactAddressDto, billingAddressDto);
	}

	public CustomerInfoEntity toDomainObject() {
		return new CustomerInfoEntity(new CustomerId(customerId), firstname, lastname, contactAddress.toDomainObject(), billingAddress.toDomainObject());
	}

	public String getCustomerId() {
		return customerId;
	}

	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}

	public String getFirstname() {
		return firstname;
	}

	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

	public String getLastname() {
		return lastname;
	}

	public void setLastname(String lastname) {
		this.lastname = lastname;
	}

	public AddressDto getContactAddress() {
		return contactAddress;
	}

	public void setContactAddress(AddressDto contactAddress) {
		this.contactAddress = contactAddress;
	}

	public AddressDto getBillingAddress() {
		return billingAddress;
	}

	public void setBillingAddress(AddressDto billingAddress) {
		this.billingAddress = billingAddress;
	}
}

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * You might be wondering why this Exception class is in the dtos package. Exceptions of this type
 * are thrown whenever the client tries to fetch an insurance quote request that doesn't exist. Spring will then
 * convert this exception into an HTTP 404 response.
 * */
@ResponseStatus(code = HttpStatus.NOT_FOUND)
public class InsuranceQuoteRequestNotFoundException extends RuntimeException {
	private static final long serialVersionUID = 532390503974496190L;

	public InsuranceQuoteRequestNotFoundException(String errorMessage) {
		super(errorMessage);
	}
}


import java.util.Date;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.lakesidemutual.policymanagement.domain.insurancequoterequest.InsuranceQuoteEntity;
import com.lakesidemutual.policymanagement.interfaces.dtos.policy.MoneyAmountDto;

/**
 * InsuranceQuoteDto is a data transfer object (DTO) that represents an Insurance Quote
 * which has been submitted as a response to a specific Insurance Quote Request.
 */
public class InsuranceQuoteDto {
	@Valid
	@NotNull
	private Date expirationDate;

	@Valid
	@NotNull
	private MoneyAmountDto insurancePremium;

	@Valid
	@NotNull
	private MoneyAmountDto policyLimit;

	public InsuranceQuoteDto() {
	}

	private InsuranceQuoteDto(Date expirationDate, MoneyAmountDto insurancePremium, MoneyAmountDto policyLimit) {
		this.expirationDate = expirationDate;
		this.insurancePremium = insurancePremium;
		this.policyLimit = policyLimit;
	}

	public static InsuranceQuoteDto fromDomainObject(InsuranceQuoteEntity insuranceQuote) {
		Date expirationDate = insuranceQuote.getExpirationDate();
		MoneyAmountDto insurancePremiumDto = MoneyAmountDto.fromDomainObject(insuranceQuote.getInsurancePremium());
		MoneyAmountDto policyLimitDto = MoneyAmountDto.fromDomainObject(insuranceQuote.getPolicyLimit());
		return new InsuranceQuoteDto(expirationDate, insurancePremiumDto, policyLimitDto);
	}

	public Date getExpirationDate() {
		return expirationDate;
	}

	public void setExpirationDate(Date expirationDate) {
		this.expirationDate = expirationDate;
	}

	public MoneyAmountDto getInsurancePremium() {
		return insurancePremium;
	}

	public void setInsurancePremium(MoneyAmountDto insurancePremium) {
		this.insurancePremium = insurancePremium;
	}

	public MoneyAmountDto getPolicyLimit() {
		return policyLimit;
	}

	public void setPolicyLimit(MoneyAmountDto policyLimit) {
		this.policyLimit = policyLimit;
	}
}

import org.springframework.hateoas.RepresentationModel;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * The CustomerDto class is a data transfer object (DTO) that represents a single customer.
 * It inherits from the ResourceSupport class which allows us to create a REST representation (e.g., JSON, XML)
 * that follows the HATEOAS principle. For example, links can be added to the representation (e.g., self, address.change)
 * which means that future actions the client may take can be discovered from the resource representation.
 *
 * The CustomerDto is part of the API contract (see the getCustomer() method in @CustomerInformationHolder). In the Swagger API docs
 * (http://localhost:8090/v2/api-docs) generated by SpringFox the is represented by the following JSON Schema. JSON Schema (https://json-schema.org/)
 * is an IETF draft to define the schema of JSON documents:
 *
 * <pre>
 * <code>
 * {
 *  "title":"CustomerDto",
 *  "type":"object",
 *  "properties":{
 *     // Note that this _links section of the JSON Schema appears here, because CustomerDto
 *     // inherits from ResourceSupport which can add HATEOAS links to the JSON representation
 *     // of the resource.
 *     "_links":{
 *        "type":"array",
 *        "xml":{
 *           "name":"link",
 *           "attribute":false,
 *           "wrapped":false
 *        },
 *        "items":{
 *           "$ref":"#/definitions/Link"
 *        }
 *     },
 *     "birthday":{
 *        "type":"string",
 *        "format":"date-time"
 *     },
 *     "city":{
 *        "type":"string"
 *     },
 *     "customerId":{
 *        "type":"string"
 *     },
 *     "email":{
 *        "type":"string"
 *     },
 *     "firstname":{
 *        "type":"string"
 *     },
 *     "lastname":{
 *        "type":"string"
 *     },
 *     "moveHistory":{
 *        "type":"array",
 *        "items":{
 *           "$ref":"#/definitions/AddressDto"
 *        }
 *     },
 *     "phoneNumber":{
 *        "type":"string"
 *     },
 *     "postalCode":{
 *        "type":"string"
 *     },
 *     "streetAddress":{
 *        "type":"string"
 *     }
 *  }
 * }
 * </code>
 * </pre>
 *
 * @see <a href="https://docs.spring.io/spring-hateoas/docs/current/reference/html/">Spring HATEOAS - Reference Documentation</a>
 */
public class CustomerDto extends RepresentationModel {
	private String customerId;
	@JsonUnwrapped
	private CustomerProfileDto customerProfile;

	public CustomerDto() {
	}

	public CustomerDto(String customerId, CustomerProfileDto customerProfile) {
		this.customerId = customerId;
		this.customerProfile = customerProfile;
	}

	public String getCustomerId() {
		return customerId;
	}

	public CustomerProfileDto getCustomerProfile() {
		return this.customerProfile;
	}

	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}

	public void setCustomerProfile(CustomerProfileDto customerProfile) {
		this.customerProfile = customerProfile;
	}
}


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * You might be wondering why this Exception class is in the dtos package. Exceptions of this type
 * are thrown whenever the client sends a request with an invalid customer id. Spring will then
 * convert this exception into an HTTP 404 response.
 * */
@ResponseStatus(code = HttpStatus.NOT_FOUND)
public class CustomerNotFoundException extends RuntimeException {
	private static final long serialVersionUID = -3416956458510420831L;

	public CustomerNotFoundException(String errorMessage) {
		super(errorMessage);
	}
}


import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * CustomerProfileDto is a data transfer object (DTO) that represents the personal data (customer profile) of a customer.
 */
public class CustomerProfileDto {
	private String firstname;
	private String lastname;
	private Date birthday;
	@JsonUnwrapped
	private AddressDto currentAddress;
	private String email;
	private String phoneNumber;
	private List<AddressDto> moveHistory;

	public CustomerProfileDto() {
	}

	public CustomerProfileDto(String firstname, String lastname, Date birthday, AddressDto currentAddress, String email, String phoneNumber, List<AddressDto> moveHistory) {
		this.firstname = firstname;
		this.lastname = lastname;
		this.birthday = birthday;
		this.currentAddress = currentAddress;
		this.email = email;
		this.phoneNumber = phoneNumber;
		this.moveHistory = moveHistory;
	}

	public String getFirstname() {
		return firstname;
	}

	public String getLastname() {
		return lastname;
	}

	public Date getBirthday() {
		return birthday;
	}

	public AddressDto getCurrentAddress() {
		return currentAddress;
	}

	public String getEmail() {
		return email;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public List<AddressDto> getMoveHistory() {
		return moveHistory;
	}

	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

	public void setLastname(String lastname) {
		this.lastname = lastname;
	}

	public void setBirthday(Date birthday) {
		this.birthday = birthday;
	}

	public void setCurrentAddress(AddressDto currentAddress) {
		this.currentAddress = currentAddress;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public void setMoveHistory(List<AddressDto> moveHistory) {
		this.moveHistory = moveHistory;
	}
}


import java.util.List;

import org.springframework.hateoas.RepresentationModel;

/**
 * The CustomersDto class is a data transfer object (DTO) that contains a list of customers.
 * It inherits from the ResourceSupport class which allows us to create a REST representation (e.g., JSON, XML)
 * that follows the HATEOAS principle. For example, links can be added to the representation (e.g., self, next, prev)
 * which means that future actions the client may take can be discovered from the resource representation.
 *
 * @see <a href="https://docs.spring.io/spring-hateoas/docs/current/reference/html/">Spring HATEOAS - Reference Documentation</a>
 */
public class CustomersDto extends RepresentationModel {
	private List<CustomerDto> customers;

	public CustomersDto() {}

	public CustomersDto(List<CustomerDto> customers) {
		this.customers = customers;
	}

	public List<CustomerDto> getCustomers() {
		return customers;
	}

	public void setCustomers(List<CustomerDto> customers) {
		this.customers = customers;
	}
}

/**
 * CustomerIdDto is a data transfer object (DTO) that represents the unique ID of a customer.
 * */
public class CustomerIdDto {
	private String id;

	public CustomerIdDto() {
	}

	/**
	 * This constructor is needed by ControllerLinkBuilder, see the following
	 * spring-hateoas issue for details:
	 * https://github.com/spring-projects/spring-hateoas/issues/352
	 */
	public CustomerIdDto(String id) {
		this.id = id;
	}

	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return id;
	}
}

import java.util.List;

import org.springframework.hateoas.RepresentationModel;

/**
 * The PaginatedCustomerResponseDto holds a collection of CustomerDto
 * with additional metadata parameters such as the limit, offset and size that
 * are used in the <a href=
 * "https://www.microservice-api-patterns.org/patterns/structure/compositeRepresentations/Pagination">Pagination</a>
 * pattern, specifically the <em>Offset-Based</em> Pagination variant.
 */
public class PaginatedCustomerResponseDto extends RepresentationModel {
	private String filter;
	private int limit;
	private int offset;
	private int size;
	private List<CustomerDto> customers;

	public PaginatedCustomerResponseDto() {}

	public PaginatedCustomerResponseDto(String filter, int limit, int offset, int size, List<CustomerDto> customers) {
		this.filter = filter;
		this.limit = limit;
		this.offset = offset;
		this.size = size;
		this.customers = customers;
	}

	public List<CustomerDto> getCustomers() {
		return customers;
	}

	public void setCustomers(List<CustomerDto> customers) {
		this.customers = customers;
	}

	public String getFilter() {
		return filter;
	}

	public void setFilter(String filter) {
		this.filter = filter;
	}

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	public int getOffset() {
		return offset;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}
}

import com.lakesidemutual.policymanagement.domain.insurancequoterequest.Address;

/**
 * AddressDto is a data transfer object (DTO) that represents the postal address of a customer.
 * */
public class AddressDto {
	private String streetAddress;
	private String postalCode;
	private String city;

	public AddressDto() {
	}

	private AddressDto(String streetAddress, String postalCode, String city) {
		this.streetAddress = streetAddress;
		this.postalCode = postalCode;
		this.city = city;
	}

	public static AddressDto fromDomainObject(Address address) {
		return new AddressDto(address.getStreetAddress(), address.getPostalCode(), address.getCity());
	}

	public Address toDomainObject() {
		return new Address(streetAddress, postalCode, city);
	}

	public String getStreetAddress() {
		return streetAddress;
	}

	public String getPostalCode() {
		return postalCode;
	}

	public String getCity() {
		return city;
	}

	public void setStreetAddress(String streetAddress) {
		this.streetAddress = streetAddress;
	}

	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}

	public void setCity(String city) {
		this.city = city;
	}
}


import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.lakesidemutual.policymanagement.interfaces.dtos.customer.CustomerIdDto;

/**
 * This converter class allows us to use CustomerIdDto as the type of
 * a @PathVariable parameter in a Spring @RestController class.
 */
@Component
public class StringToCustomerIdDtoConverter implements Converter<String, CustomerIdDto> {
	@Override
	public CustomerIdDto convert(String source) {
		CustomerIdDto customerId = new CustomerIdDto();
		customerId.setId(source);
		return customerId;
	}
}



import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.lakesidemutual.policymanagement.domain.policy.PolicyId;

/**
 * This converter class allows us to use PolicyId as the type of
 * a @PathVariable parameter in a Spring @RestController class.
 */
@Component
public class StringToPolicyIdConverter implements Converter<String, PolicyId> {
	@Override
	public PolicyId convert(String source) {
		return new PolicyId(source);
	}
}


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * InsuringAgreementEntity is an entity which represents the insuring agreement between a
 * customer and Lakeside Mutual. Each InsuringAgreementEntity belongs to a PolicyAggregateRoot.
 */
@Entity
@Table(name = "insuringagreements")
public class InsuringAgreementEntity implements org.microserviceapipatterns.domaindrivendesign.Entity {
	@GeneratedValue
	@Id
	private Long id;

	@OneToMany(cascade = CascadeType.ALL)
	private final List<InsuringAgreementItem> agreementItems;

	public InsuringAgreementEntity() {
		this.agreementItems = null;
	}

	public InsuringAgreementEntity(List<InsuringAgreementItem> agreementItems) {
		this.agreementItems = agreementItems;
	}

	public Long getId() {
		return id;
	}

	public List<InsuringAgreementItem> getAgreementItems() {
		return agreementItems;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}

		InsuringAgreementEntity other = (InsuringAgreementEntity) obj;
		ArrayList<InsuringAgreementItem> lhs = new ArrayList<>(agreementItems);
		ArrayList<InsuringAgreementItem> rhs = new ArrayList<>(other.agreementItems);
		return lhs.equals(rhs);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(new ArrayList<>(agreementItems));
	}
}


import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Embeddable;

import org.apache.commons.lang3.RandomStringUtils;

import org.microserviceapipatterns.domaindrivendesign.EntityIdentifier;
import org.microserviceapipatterns.domaindrivendesign.ValueObject;

/**
 * A PolicyId is a value object that is used to represent the unique id of a policy.
 */
@Embeddable
public class PolicyId implements Serializable, ValueObject, EntityIdentifier<String> {
	private static final long serialVersionUID = 1L;

	private String id;

	public PolicyId() {
		this.setId(null);
	}

	/**
	 * This constructor is needed by ControllerLinkBuilder, see the following
	 * spring-hateoas issue for details:
	 * https://github.com/spring-projects/spring-hateoas/issues/352
	 */
	public PolicyId(String id) {
		this.setId(id);
	}

	public static PolicyId random() {
		return new PolicyId(RandomStringUtils.randomAlphanumeric(10).toLowerCase());
	}

	@Override
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return getId().toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj == null) {
			return false;
		}

		if (getClass() != obj.getClass()) {
			return false;
		}

		PolicyId other = (PolicyId) obj;
		return Objects.equals(getId(), other.getId());
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(getId());
	}
}


import java.util.Date;

import org.microserviceapipatterns.domaindrivendesign.DomainEvent;

import com.lakesidemutual.policymanagement.interfaces.dtos.customer.CustomerDto;
import com.lakesidemutual.policymanagement.interfaces.dtos.policy.PolicyDto;

/**
 * UpdatePolicyEvent is a domain event that is sent to the Risk Management Server
 * every time a new policy is created or an existing policy is updated.
 * */
public class UpdatePolicyEvent implements DomainEvent {
	private String kind;
	private String originator;
	private Date date;
	private CustomerDto customer;
	private PolicyDto policy;

	public UpdatePolicyEvent() {
	}

	public UpdatePolicyEvent(String originator, Date date, CustomerDto customer, PolicyDto policy) {
		this.kind = "UpdatePolicyEvent";
		this.originator = originator;
		this.date = date;
		this.customer = customer;
		this.policy = policy;
	}

	public String getKind() {
		return kind;
	}

	public void setKind(String kind) {
		this.kind = kind;
	}

	public String getOriginator() {
		return originator;
	}

	public void setOriginator(String originator) {
		this.originator = originator;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public CustomerDto getCustomer() {
		return customer;
	}

	public void setCustomer(CustomerDto customer) {
		this.customer = customer;
	}

	public PolicyDto getPolicy() {
		return policy;
	}

	public void setPolicy(PolicyDto policy) {
		this.policy = policy;
	}
}


import java.util.Date;

import org.microserviceapipatterns.domaindrivendesign.DomainEvent;

/**
 * DeletePolicyEvent is a domain event that is sent to the Risk Management Server
 * every time a policy is deleted.
 * */
public class DeletePolicyEvent implements DomainEvent {
	private String kind;
	private String originator;
	private Date date;
	private String policyId;

	public DeletePolicyEvent() {
	}

	public DeletePolicyEvent(String originator, Date date, String policyId) {
		this.kind = "DeletePolicyEvent";
		this.originator = originator;
		this.date = date;
		this.policyId = policyId;
	}

	public String getKind() {
		return kind;
	}

	public void setKind(String kind) {
		this.kind = kind;
	}

	public String getOriginator() {
		return originator;
	}

	public void setOriginator(String originator) {
		this.originator = originator;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getPolicyId() {
		return policyId;
	}

	public void setPolicyId(String policyId) {
		this.policyId = policyId;
	}
}


import java.util.Objects;

import org.microserviceapipatterns.domaindrivendesign.ValueObject;

/**
 * A PolicyType is a value object that is used to represent the type of a policy (e.g., health insurance, life insurance, etc).
 */
public class PolicyType implements ValueObject {
	private String name;

	public PolicyType() {
		this.name = "";
	}

	public PolicyType(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}

		PolicyType other = (PolicyType) obj;
		return Objects.equals(name, other.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}
}


import java.util.Date;
import java.util.Objects;

import jakarta.persistence.Embeddable;

import org.microserviceapipatterns.domaindrivendesign.ValueObject;

/**
 * A PolicyPeriod is a value object that is used to represent the period during which a policy is valid.
 */
@Embeddable
public class PolicyPeriod implements ValueObject {
	private Date startDate;
	private Date endDate;

	public PolicyPeriod() {}

	public PolicyPeriod(Date startDate, Date endDate) {
		Objects.requireNonNull(startDate);
		Objects.requireNonNull(endDate);
		this.startDate = startDate;
		this.endDate = endDate;
	}

	public Date getStartDate() {
		return startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}

		PolicyPeriod other = (PolicyPeriod) obj;
		if(startDate.getTime() != other.startDate.getTime()) {
			return false;
		}

		if(endDate.getTime() != other.endDate.getTime()) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return Objects.hash(startDate, endDate);
	}
}


import java.util.Date;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.microserviceapipatterns.domaindrivendesign.RootEntity;
import com.lakesidemutual.policymanagement.domain.customer.CustomerId;

import io.github.adr.embedded.MADR;

/**
 * PolicyAggregateRoot is the root entity of the Policy aggregate. Note that there is
 * no class for the Policy aggregate, so the package can be seen as aggregate.
 */
@MADR(
		value = 2,
		title = "Separation between domain data model and infrastructure data model",
		contextAndProblem = "JPA / Spring Data annotations usually belong into a separate data model in the infrastructure layer",
		alternatives = {
				"Keep the JPA / Spring Data annotations in the domain data model",
				"Implement a separate data model with JPA / Spring Data annotations in the infrastructure layer",
		},
		chosenAlternative = "Keep the JPA / Spring Data annotations in the domain data model",
		justification = "The relatively small size of this application does not warrant the additional complexity (yet)."
		)
@Entity
@Table(name = "insurancepolicies")
public class PolicyAggregateRoot implements RootEntity {
	@EmbeddedId
	private PolicyId id;

	@Embedded
	@AttributeOverrides({
		@AttributeOverride(name="id", column=@Column(name="customerId"))})
	private CustomerId customerId;

	/**
	 * When retrieving these entities, we want to be able to sort them by their creation date:
	 *
	 * policyRepository.findAll(Sort.by(Sort.Direction.DESC, PolicyAggregateRoot.FIELD_CREATION_DATE));
	 *
	 * Using a constant that contains the field name, we can don't have to use string literals to reference the field name. This also
	 * improves the maintainability of the code, if the name ever changes, we can just update the constant as well.
	 */
	public final static String FIELD_CREATION_DATE = "creationDate";

	private Date creationDate;

	@Embedded
	private PolicyPeriod policyPeriod;

	@Embedded
	private PolicyType policyType;

	/**
	 * These @AttributeOverrides attributes break the information hiding principle somewhat. However, we decided to keep them for now,
	 * because they are the easiest way to embed multiple MoneyAmount value objects into the PolicyAggregateRoot.
	 * */
	@Embedded
	@AttributeOverrides({
		@AttributeOverride(name="amount", column=@Column(name="deductibleAmount")),
		@AttributeOverride(name="currency", column=@Column(name="deductibleCurrency"))})
	private MoneyAmount deductible;

	@Embedded
	@AttributeOverrides({
		@AttributeOverride(name="amount", column=@Column(name="limitAmount")),
		@AttributeOverride(name="currency", column=@Column(name="limitCurrency"))})
	private MoneyAmount policyLimit;

	@Embedded
	@AttributeOverrides({
		@AttributeOverride(name="amount", column=@Column(name="premiumAmount")),
		@AttributeOverride(name="currency", column=@Column(name="premiumCurrency"))})
	private MoneyAmount insurancePremium;

	@OneToOne(cascade = CascadeType.ALL)
	private InsuringAgreementEntity insuringAgreement;

	public PolicyAggregateRoot() {}

	public PolicyAggregateRoot(
			PolicyId id,
			CustomerId customerId,
			Date creationDate,
			PolicyPeriod policyPeriod,
			PolicyType policyType,
			MoneyAmount deductible,
			MoneyAmount policyLimit,
			MoneyAmount insurancePremium,
			InsuringAgreementEntity insuringAgreement) {
		this.id = id;
		this.customerId = customerId;
		this.creationDate = creationDate;
		this.policyPeriod = policyPeriod;
		this.policyType = policyType;
		this.deductible = deductible;
		this.policyLimit = policyLimit;
		this.insurancePremium = insurancePremium;
		this.insuringAgreement = insuringAgreement;
	}

	public PolicyId getId() {
		return id;
	}

	public CustomerId getCustomerId() {
		return customerId;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public PolicyPeriod getPolicyPeriod() {
		return policyPeriod;
	}

	public void setPolicyPeriod(PolicyPeriod policyPeriod) {
		this.policyPeriod = policyPeriod;
	}

	public PolicyType getPolicyType() {
		return policyType;
	}

	public void setPolicyType(PolicyType policyType) {
		this.policyType = policyType;
	}

	public MoneyAmount getDeductible() {
		return deductible;
	}

	public void setDeductible(MoneyAmount deductible) {
		this.deductible = deductible;
	}

	public MoneyAmount getPolicyLimit() {
		return policyLimit;
	}

	public void setPolicyLimit(MoneyAmount policyLimit) {
		this.policyLimit = policyLimit;
	}

	public MoneyAmount getInsurancePremium() {
		return insurancePremium;
	}

	public void setInsurancePremium(MoneyAmount insurancePremium) {
		this.insurancePremium = insurancePremium;
	}

	public InsuringAgreementEntity getInsuringAgreement() {
		return insuringAgreement;
	}

	public void setInsuringAgreement(InsuringAgreementEntity insuringAgreement) {
		this.insuringAgreement = insuringAgreement;
	}
}


import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

import jakarta.persistence.Embeddable;

import org.microserviceapipatterns.domaindrivendesign.ValueObject;

/**
 * An instance of MoneyAmount is a value object that represents an amount of money in a specific currency.
 * For example, this is used to represent the insurance premium of a policy.
 */
@Embeddable
public class MoneyAmount implements ValueObject {
	private BigDecimal amount;
	private Currency currency;

	public MoneyAmount() {}

	public MoneyAmount(BigDecimal amount, Currency currency) {
		Objects.requireNonNull(amount);
		Objects.requireNonNull(currency);
		this.amount = amount;
		this.currency = currency;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public Currency getCurrency() {
		return currency;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}

		MoneyAmount other = (MoneyAmount) obj;
		if(amount.compareTo(other.amount) != 0) {
			return false;
		}

		if(!currency.equals(other.currency)) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return Objects.hash(amount, currency);
	}
}


import java.util.Objects;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.microserviceapipatterns.domaindrivendesign.ValueObject;

/**
 * An InsuranceAgreementItem is a value object that is used to represent a single item in an insuring agreement.
 * */
@Entity
@Table(name = "insuranceagreementitems")
public class InsuringAgreementItem implements ValueObject {

	@GeneratedValue
	@Id
	private Long id;

	private final String title;

	private final String description;

	public InsuringAgreementItem() {
		this.title = null;
		this.description = null;
	}

	public InsuringAgreementItem(String title, String description) {
		this.title = title;
		this.description = description;
	}

	public Long getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}

		InsuringAgreementItem other = (InsuringAgreementItem) obj;
		return Objects.equals(title, other.title) && Objects.equals(description, other.description);
	}

	@Override
	public int hashCode() {
		return Objects.hash(title, description);
	}
}


import java.util.Date;

import org.microserviceapipatterns.domaindrivendesign.DomainEvent;

/**
 * InsuranceQuoteExpiredEvent is a domain event class that is used to notify the Customer Self-Service Backend
 * when the Insurance Quote for a specific Insurance Quote Request has expired.
 * */
public class InsuranceQuoteExpiredEvent implements DomainEvent {
	private Date date;
	private Long insuranceQuoteRequestId;

	public InsuranceQuoteExpiredEvent() {
	}

	public InsuranceQuoteExpiredEvent(Date date, Long insuranceQuoteRequestId) {
		this.date = date;
		this.insuranceQuoteRequestId = insuranceQuoteRequestId;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public Long getInsuranceQuoteRequestId() {
		return insuranceQuoteRequestId;
	}

	public void setInsuranceQuoteRequestId(Long insuranceQuoteRequestId) {
		this.insuranceQuoteRequestId = insuranceQuoteRequestId;
	}
}

import java.util.Date;

import org.microserviceapipatterns.domaindrivendesign.DomainEvent;

/**
 * CustomerDecisionEvent is a domain event class that is used to notify the Policy Management Backend
 * about a decision by a customer to accept or reject a specific Insurance Quote.
 * */
public class CustomerDecisionEvent implements DomainEvent {
	private Date date;
	private Long insuranceQuoteRequestId;
	private boolean quoteAccepted;

	public CustomerDecisionEvent() {
	}

	public CustomerDecisionEvent(Date date, Long insuranceQuoteRequestId, boolean quoteAccepted) {
		this.date = date;
		this.insuranceQuoteRequestId = insuranceQuoteRequestId;
		this.quoteAccepted = quoteAccepted;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public Long getInsuranceQuoteRequestId() {
		return insuranceQuoteRequestId;
	}

	public void setInsuranceQuoteRequestId(Long insuranceQuoteRequestId) {
		this.insuranceQuoteRequestId = insuranceQuoteRequestId;
	}

	public boolean isQuoteAccepted() {
		return quoteAccepted;
	}

	public void setQuoteAccepted(boolean quoteAccepted) {
		this.quoteAccepted = quoteAccepted;
	}
}


import java.util.Date;
import java.util.Objects;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.microserviceapipatterns.domaindrivendesign.ValueObject;

/**
 * An instance of RequestStatusChange is a value object that represents a status change
 * of an insurance quote request. It contains the date of the status change as well as the new status.
 */
@Entity
@Table(name = "requeststatuschanges")
@Embeddable
public class RequestStatusChange implements ValueObject {
	@GeneratedValue
	@Id
	private Long id;

	private Date date;

	@Enumerated(EnumType.STRING)
	private RequestStatus status;

	public RequestStatusChange() {}

	public RequestStatusChange(Date date, RequestStatus status) {
		Objects.requireNonNull(date);
		Objects.requireNonNull(status);
		this.date = date;
		this.status = status;
	}

	public Date getDate() {
		return date;
	}

	public RequestStatus getStatus() {
		return status;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}

		RequestStatusChange other = (RequestStatusChange) obj;
		return Objects.equals(date, other.date) && Objects.equals(status, other.status);
	}

	@Override
	public int hashCode() {
		return Objects.hash(date, status);
	}
}

import java.util.Date;

import org.microserviceapipatterns.domaindrivendesign.DomainEvent;

import com.lakesidemutual.policymanagement.interfaces.dtos.policy.MoneyAmountDto;

/**
 * InsuranceQuoteResponseEvent is a domain event class that is used to notify the Customer Self-Service Backend
 * when Lakeside Mutual has submitted a response for a specific Insurance Quote Request.
 * */
public class InsuranceQuoteResponseEvent implements DomainEvent {
	private Date date;
	private Long insuranceQuoteRequestId;
	private boolean requestAccepted;
	private Date expirationDate;
	private MoneyAmountDto insurancePremium;
	private MoneyAmountDto policyLimit;

	public InsuranceQuoteResponseEvent() {
	}

	public InsuranceQuoteResponseEvent(Date date, Long insuranceQuoteRequestId, boolean requestAccepted, Date expirationDate, MoneyAmountDto insurancePremium, MoneyAmountDto policyLimit) {
		this.date = date;
		this.insuranceQuoteRequestId = insuranceQuoteRequestId;
		this.requestAccepted = requestAccepted;
		this.expirationDate = expirationDate;
		this.insurancePremium = insurancePremium;
		this.policyLimit = policyLimit;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public Long getInsuranceQuoteRequestId() {
		return insuranceQuoteRequestId;
	}

	public void setInsuranceQuoteRequestId(Long insuranceQuoteRequestId) {
		this.insuranceQuoteRequestId = insuranceQuoteRequestId;
	}

	public boolean isRequestAccepted() {
		return requestAccepted;
	}

	public void setRequestAccepted(boolean requestAccepted) {
		this.requestAccepted = requestAccepted;
	}

	public Date getExpirationDate() {
		return expirationDate;
	}

	public void setExpirationDate(Date expirationDate) {
		this.expirationDate = expirationDate;
	}

	public MoneyAmountDto getInsurancePremium() {
		return insurancePremium;
	}

	public void setInsurancePremium(MoneyAmountDto insurancePremium) {
		this.insurancePremium = insurancePremium;
	}

	public MoneyAmountDto getPolicyLimit() {
		return policyLimit;
	}

	public void setPolicyLimit(MoneyAmountDto policyLimit) {
		this.policyLimit = policyLimit;
	}
}

import java.util.Date;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.lakesidemutual.policymanagement.domain.policy.MoneyAmount;

/**
 * InsuranceOptionsEntity is an entity that contains the insurance options (e.g., start date, insurance type, etc.)
 * that a customer selected for an Insurance Quote Request.
 */
@Entity
@Table(name = "insuranceoptions")
public class InsuranceOptionsEntity implements org.microserviceapipatterns.domaindrivendesign.Entity {
	@GeneratedValue
	@Id
	private Long id;

	private Date startDate;

	@Embedded
	private InsuranceType insuranceType;

	@Embedded
	@AttributeOverrides({
		@AttributeOverride(name="amount", column=@Column(name="deductibleAmount")),
		@AttributeOverride(name="currency", column=@Column(name="deductibleCurrency"))})
	private MoneyAmount deductible;

	public InsuranceOptionsEntity() {
	}

	public InsuranceOptionsEntity(Date startDate, InsuranceType insuranceType, MoneyAmount deductible) {
		this.startDate = startDate;
		this.insuranceType = insuranceType;
		this.deductible = deductible;
	}

	public Date getStartDate() {
		return startDate;
	}

	public InsuranceType getInsuranceType() {
		return insuranceType;
	}

	public MoneyAmount getDeductible() {
		return deductible;
	}
}


import java.util.Date;

import org.microserviceapipatterns.domaindrivendesign.DomainEvent;

/**
 * PolicyCreatedEvent is a domain event class that is used to notify the Customer Self-Service Backend
 * when a new Policy has been created after an Insurance Quote has been accepted.
 * */
public class PolicyCreatedEvent implements DomainEvent {
	private Date date;
	private Long insuranceQuoteRequestId;
	private String policyId;

	public PolicyCreatedEvent() {
	}

	public PolicyCreatedEvent(Date date, Long insuranceQuoteRequestId, String policyId) {
		this.date = date;
		this.insuranceQuoteRequestId = insuranceQuoteRequestId;
		this.policyId = policyId;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public Long getInsuranceQuoteRequestId() {
		return insuranceQuoteRequestId;
	}

	public void setInsuranceQuoteRequestId(Long insuranceQuoteRequestId) {
		this.insuranceQuoteRequestId = insuranceQuoteRequestId;
	}

	public String getPolicyId() {
		return policyId;
	}

	public void setPolicyId(String policyId) {
		this.policyId = policyId;
	}
}

import java.util.Date;

import org.microserviceapipatterns.domaindrivendesign.DomainEvent;
import com.lakesidemutual.policymanagement.interfaces.dtos.insurancequoterequest.InsuranceQuoteRequestDto;

/**
 * InsuranceQuoteRequestEvent is a domain event class that is used to notify the Policy Management Backend
 * when a new Insurance Quote Request has been submitted by a customer.
 * */
public class InsuranceQuoteRequestEvent implements DomainEvent {
	private Date date;
	private InsuranceQuoteRequestDto insuranceQuoteRequestDto;

	public InsuranceQuoteRequestEvent() {
	}

	public InsuranceQuoteRequestEvent(Date date, InsuranceQuoteRequestDto insuranceQuoteRequestDto) {
		this.date = date;
		this.insuranceQuoteRequestDto = insuranceQuoteRequestDto;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public InsuranceQuoteRequestDto getInsuranceQuoteRequestDto() {
		return insuranceQuoteRequestDto;
	}

	public void setInsuranceQuoteRequestDto(InsuranceQuoteRequestDto insuranceQuoteRequestDto) {
		this.insuranceQuoteRequestDto = insuranceQuoteRequestDto;
	}
}


import java.util.Date;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.lakesidemutual.policymanagement.domain.policy.MoneyAmount;

/**
 * InsuranceQuoteEntity is an entity that represents an Insurance Quote
 * which has been submitted as a response to a specific Insurance Quote Request.
 */
@Entity
@Table(name = "insurancequotes")
public class InsuranceQuoteEntity implements org.microserviceapipatterns.domaindrivendesign.Entity {
	@GeneratedValue
	@Id
	private Long id;

	private Date expirationDate;

	@Embedded
	@AttributeOverrides({
		@AttributeOverride(name="amount", column=@Column(name="insurancePremiumAmount")),
		@AttributeOverride(name="currency", column=@Column(name="insurancePremiumCurrency"))})
	private MoneyAmount insurancePremium;

	@Embedded
	@AttributeOverrides({
		@AttributeOverride(name="amount", column=@Column(name="policyLimitAmount")),
		@AttributeOverride(name="currency", column=@Column(name="policyLimitCurrency"))})
	private MoneyAmount policyLimit;

	public InsuranceQuoteEntity() {
	}

	public InsuranceQuoteEntity(Date expirationDate, MoneyAmount insurancePremium, MoneyAmount policyLimit) {
		this.expirationDate = expirationDate;
		this.insurancePremium = insurancePremium;
		this.policyLimit = policyLimit;
	}

	public Date getExpirationDate() {
		return expirationDate;
	}

	public MoneyAmount getInsurancePremium() {
		return insurancePremium;
	}

	public MoneyAmount getPolicyLimit() {
		return policyLimit;
	}
}


import java.util.Objects;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.microserviceapipatterns.domaindrivendesign.ValueObject;

/**
 * An Address is a value object that is used to represent the postal address of a customer.
 *
 * You might be wondering why the Address class implements the ValueObject interface even though it has a JPA @Entity annotation.
 * This discrepancy exists for technical reasons. JPA requires Address to be declared as an entity, because it is part of a one-to-many
 * relationship. However, in the DDD sense, Address behaves like a value object.
 *
 * You also might be wondering why this code is the same as that in the Customer Core. This does violate DRY but for
 * good reasons: strategic DD suggests that model boundaries decouple (sub-)systems so that they can be deployed and
 * evolved independently.
 * */
@Entity
@Table(name = "addresses")
public class Address implements ValueObject {
	@GeneratedValue
	@Id
	private Long id;

	private final String streetAddress;

	private final String postalCode;

	private final String city;

	public Address() {
		this.streetAddress = null;
		this.postalCode = null;
		this.city = null;
	}

	public Address(String streetAddress, String postalCode, String city) {
		this.streetAddress = streetAddress;
		this.postalCode = postalCode;
		this.city = city;
	}

	public String getStreetAddress() {
		return streetAddress;
	}

	public String getPostalCode() {
		return postalCode;
	}

	public String getCity() {
		return city;
	}

	@Override
	public String toString() {
		return String.format("%s, %s %ss", streetAddress, postalCode, city);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}

		Address other = (Address) obj;
		return Objects.equals(streetAddress, other.streetAddress) &&
				Objects.equals(postalCode, other.postalCode) &&
				Objects.equals(city, other.city);
	}

	@Override
	public int hashCode() {
		return Objects.hash(streetAddress, postalCode, city);
	}
}

import org.microserviceapipatterns.domaindrivendesign.ValueObject;

/**
 * A RequestStatus is a value object that is used to represent
 * the current status of an insurance quote request.
 *
 * The following diagram shows the possible state transitions:
 *
 * <pre>
 *
 *                                               │
 *                                               ▼
 *                                        ┌────────────┐
 *                                        │  REQUEST_  │
 *                                        │ SUBMITTED  │
 *                                        └────────────┘
 *                                               │
 *                             ┌─────────────────┴────────────────┐
 *                             │                                  │
 *                             ▼                                  ▼
 *                      ┌────────────┐                     ╔════════════╗
 *                      │   QUOTE_   │                     ║  REQUEST_  ║
 *          ┌───────────│  RECEIVED  │─────────────┐       ║  REJECTED  ║
 *          │           └────────────┘             │       ╚════════════╝
 *          │                  │                   │
 *          │                  │                   │
 *          ▼                  ▼                   ▼
 *   ╔════════════╗     ┌────────────┐      ╔════════════╗
 *   ║   QUOTE_   ║     │   QUOTE_   │      ║   QUOTE_   ║
 *   ║  REJECTED  ║     │  ACCEPTED  │─────▶║  EXPIRED   ║
 *   ╚════════════╝     └────────────┘      ╚════════════╝
 *                             │
 *                             │
 *                             ▼
 *                      ╔════════════╗
 *                      ║  POLICY_   ║
 *                      ║  CREATED   ║
 *                      ╚════════════╝
 *
 * </pre>
 */
public enum RequestStatus implements ValueObject {
	/** The customer has submitted a request. No answer has been received yet. */
	REQUEST_SUBMITTED,

	/** Lakeside Mutual has rejected the request. No quote has been made. */
	REQUEST_REJECTED,

	/** Lakeside Mutual has accepted the request and made a corresponding quote. */
	QUOTE_RECEIVED,

	/** The customer has accepted Lakeside Mutual's quote. */
	QUOTE_ACCEPTED,

	/** The customer has rejected Lakeside Mutual's quote. */
	QUOTE_REJECTED,

	/** The quote has expired and is no longer valid. */
	QUOTE_EXPIRED,

	/** A new insurance policy has been created. */
	POLICY_CREATED;

	public boolean canTransitionTo(RequestStatus newStatus) {
		switch(this) {
		case REQUEST_SUBMITTED:
			return newStatus == REQUEST_REJECTED || newStatus == QUOTE_RECEIVED;
		case QUOTE_RECEIVED:
			return newStatus == QUOTE_ACCEPTED || newStatus == QUOTE_REJECTED || newStatus == QUOTE_EXPIRED;
		case QUOTE_ACCEPTED:
			return newStatus == POLICY_CREATED || newStatus == QUOTE_EXPIRED;
		case REQUEST_REJECTED:
		case POLICY_CREATED:
		case QUOTE_REJECTED:
		case QUOTE_EXPIRED:
			return false;
		default:
			return false;
		}
	}
}

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.microserviceapipatterns.domaindrivendesign.RootEntity;

/**
 * InsuranceQuoteRequestAggregateRoot is the root entity of the Insurance Quote Request aggregate. Note that there is
 * no class for the Insurance Quote Request aggregate, so the package can be seen as aggregate.
 */
@Entity
@Table(name = "insurancequoterequests")
public class InsuranceQuoteRequestAggregateRoot implements RootEntity {
	@Id
	private Long id;

	private Date date;

	@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	private List<RequestStatusChange> statusHistory;

	@OneToOne(cascade = CascadeType.ALL)
	private CustomerInfoEntity customerInfo;

	@OneToOne(cascade = CascadeType.ALL)
	private InsuranceOptionsEntity insuranceOptions;

	@OneToOne(cascade = CascadeType.ALL)
	private InsuranceQuoteEntity insuranceQuote;

	private String policyId;

	public InsuranceQuoteRequestAggregateRoot() {}

	public InsuranceQuoteRequestAggregateRoot(Long id, Date date, RequestStatus initialStatus, CustomerInfoEntity customerInfo, InsuranceOptionsEntity insuranceOptions, InsuranceQuoteEntity insuranceQuote, String policyId) {
		this.id = id;
		this.date = date;
		List<RequestStatusChange> statusHistory = new ArrayList<>();
		statusHistory.add(new RequestStatusChange(date, initialStatus));
		this.statusHistory = statusHistory;
		this.customerInfo = customerInfo;
		this.insuranceOptions = insuranceOptions;
		this.insuranceQuote = insuranceQuote;
		this.policyId = policyId;
	}

	public Long getId() {
		return id;
	}

	public Date getDate() {
		return date;
	}

	public RequestStatus getStatus() {
		return statusHistory.get(statusHistory.size()-1).getStatus();
	}

	public List<RequestStatusChange> getStatusHistory() {
		return statusHistory;
	}

	private void changeStatusTo(RequestStatus newStatus, Date date) {
		if (!getStatus().canTransitionTo(newStatus)) {
			throw new RuntimeException(String.format("Cannot change insurance quote request status from %s to %s", getStatus(), newStatus));
		}
		statusHistory.add(new RequestStatusChange(date, newStatus));
	}

	public RequestStatusChange popStatus() {
		if(statusHistory.isEmpty()) {
			return null;
		}
		return statusHistory.remove(statusHistory.size()-1);
	}

	public void acceptRequest(InsuranceQuoteEntity insuranceQuote, Date date) {
		this.insuranceQuote = insuranceQuote;
		changeStatusTo(RequestStatus.QUOTE_RECEIVED, date);
	}

	public void rejectRequest(Date date) {
		changeStatusTo(RequestStatus.REQUEST_REJECTED, date);
	}

	public void acceptQuote(Date date) {
		changeStatusTo(RequestStatus.QUOTE_ACCEPTED, date);
	}

	public void rejectQuote(Date date) {
		changeStatusTo(RequestStatus.QUOTE_REJECTED, date);
	}

	/**
	 * Checks the quote expiration date and changes the request status to QUOTE_EXPIRED if necessary.
	 * Returns true if the request status has been changed.
	 * */
	public boolean checkQuoteExpirationDate(Date date) {
		if(getStatus().canTransitionTo(RequestStatus.QUOTE_EXPIRED) && hasQuoteExpired(date)) {
			markQuoteAsExpired(date);
			return true;
		}
		return false;
	}

	public boolean hasQuoteExpired(Date date) {
		return insuranceQuote != null && insuranceQuote.getExpirationDate().before(date);
	}

	public void markQuoteAsExpired(Date date) {
		changeStatusTo(RequestStatus.QUOTE_EXPIRED, date);
	}

	public void finalizeQuote(String policyId, Date date) {
		this.policyId = policyId;
		changeStatusTo(RequestStatus.POLICY_CREATED, date);
	}

	public CustomerInfoEntity getCustomerInfo() {
		return customerInfo;
	}

	public InsuranceOptionsEntity getInsuranceOptions() {
		return insuranceOptions;
	}

	public InsuranceQuoteEntity getInsuranceQuote() {
		return insuranceQuote;
	}

	public String getPolicyId() {
		return policyId;
	}
}

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import com.lakesidemutual.policymanagement.domain.customer.CustomerId;

/**
 * CustomerInfoEntity is an entity that is part of an InsuranceQuoteRequestAggregateRoot
 * and contains infos about the initiator of the request.
 */
@Entity
@Table(name = "customerinfos")
public class CustomerInfoEntity implements org.microserviceapipatterns.domaindrivendesign.Entity {
	@GeneratedValue
	@Id
	private Long id;

	@Embedded
	@AttributeOverrides({
		@AttributeOverride(name="id", column=@Column(name="customerId"))})
	private final CustomerId customerId;

	private final String firstname;

	private final String lastname;

	@OneToOne(cascade = CascadeType.ALL)
	private final Address contactAddress;

	@OneToOne(cascade = CascadeType.ALL)
	private final Address billingAddress;

	public CustomerInfoEntity() {
		this.customerId = null;
		this.firstname = null;
		this.lastname = null;
		this.contactAddress = null;
		this.billingAddress = null;
	}

	public CustomerInfoEntity(CustomerId customerId, String firstname, String lastname, Address contactAddress, Address billingAddress) {
		this.customerId = customerId;
		this.firstname = firstname;
		this.lastname = lastname;
		this.contactAddress = contactAddress;
		this.billingAddress = billingAddress;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public CustomerId getCustomerId() {
		return customerId;
	}

	public String getFirstname() {
		return firstname;
	}

	public String getLastname() {
		return lastname;
	}

	public Address getContactAddress() {
		return contactAddress;
	}

	public Address getBillingAddress() {
		return billingAddress;
	}
}



import java.util.Objects;

import org.microserviceapipatterns.domaindrivendesign.ValueObject;

/**
 * An instance of InsuranceType is a value object that is used to represent the type of insurance (e.g., health insurance, life insurance, etc).
 */
public class InsuranceType implements ValueObject {
	private String name;

	public InsuranceType() {
		this.name = "";
	}

	public InsuranceType(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}

		InsuranceType other = (InsuranceType) obj;
		return Objects.equals(name, other.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}
}

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Embeddable;

import org.microserviceapipatterns.domaindrivendesign.EntityIdentifier;
import org.microserviceapipatterns.domaindrivendesign.ValueObject;

import io.github.adr.embedded.MADR;

@MADR(
		value = 2,
		title = "Separation between domain data model and infrastructure data model",
		contextAndProblem = "JPA / Spring Data annotations usually belong into a separate data model in the infrastructure layer",
		alternatives = {
				"Keep the JPA / Spring Data annotations in the domain data model",
				"Implement a separate data model with JPA / Spring Data annotations in the infrastructure layer",
		},
		chosenAlternative = "Keep the JPA / Spring Data annotations in the domain data model",
		justification = "The relatively small size of this application does not warrant the additional complexity (yet)."
		)
/**
 * A CustomerId is a value object that is used to represent the unique id of a customer.
 */
@Embeddable
public class CustomerId implements Serializable, ValueObject, EntityIdentifier<String> {
	private static final long serialVersionUID = 1L;

	private String id;

	public CustomerId() {
		this.setId(null);
	}

	/**
	 * This constructor is needed by ControllerLinkBuilder, see the following
	 * spring-hateoas issue for details:
	 * https://github.com/spring-projects/spring-hateoas/issues/352
	 */
	public CustomerId(String id) {
		this.setId(id);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj == null) {
			return false;
		}

		if (getClass() != obj.getClass()) {
			return false;
		}

		CustomerId other = (CustomerId) obj;
		return Objects.equals(getId(), other.getId());
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(getId());
	}

	@Override
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return getId();
	}
}

