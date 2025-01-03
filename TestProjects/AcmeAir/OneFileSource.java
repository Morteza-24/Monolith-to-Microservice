/*******************************************************************************
* Copyright (c) 2013 IBM Corp.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*******************************************************************************/


public interface AcmeAirConstants {

	
}


public interface AirportCodeMapping{
	
	
	public String getAirportCode();
	
	public void setAirportCode(String airportCode);
	
	public String getAirportName();
	
	public void setAirportName(String airportName);

}

import java.util.Date;

public interface Booking {

	public String getBookingId();
		
	public String getFlightId();
	
	public String getCustomerId();
	
	public Date getDateOfBooking();
	
}

public interface Customer {

	public enum MemberShipStatus { NONE, SILVER, GOLD, PLATINUM, EXEC_PLATINUM, GRAPHITE };
	public enum PhoneType { UNKNOWN, HOME, BUSINESS, MOBILE };
	
	
	public String getCustomerId();
	
	public String getUsername();
	
	public void setUsername(String username);
	
	public String getPassword();
	
	public void setPassword(String password);
	
	public MemberShipStatus getStatus();
	
	public void setStatus(MemberShipStatus status);
	
	public int getTotal_miles();
	
	public int getMiles_ytd();
	
	public String getPhoneNumber();

	public void setPhoneNumber(String phoneNumber);

	public PhoneType getPhoneNumberType();

	public void setPhoneNumberType(PhoneType phoneNumberType);

	public CustomerAddress getAddress();

	public void setAddress(CustomerAddress address);

}

public interface CustomerAddress {

	
	public String getStreetAddress1();
	public void setStreetAddress1(String streetAddress1);
	public String getStreetAddress2();
	public void setStreetAddress2(String streetAddress2);
	public String getCity();
	public void setCity(String city);
	public String getStateProvince();
	public void setStateProvince(String stateProvince);
	public String getCountry();
	public void setCountry(String country);
	public String getPostalCode();
	public void setPostalCode(String postalCode);
		
}

public interface CustomerSession {
	
	public String getId();

	public String getCustomerid();

	public Date getLastAccessedTime();
	
	public Date getTimeoutTime();

}

import java.math.BigDecimal;
import java.util.Date;


public interface Flight{
	
	String getFlightId();
	
	void setFlightId(String id);
	
	String getFlightSegmentId();
	
	FlightSegment getFlightSegment();

	void setFlightSegment(FlightSegment flightSegment);
		
	Date getScheduledDepartureTime();

	Date getScheduledArrivalTime();

	BigDecimal getFirstClassBaseCost();

	BigDecimal getEconomyClassBaseCost();

	int getNumFirstClassSeats();

	int getNumEconomyClassSeats();

	String getAirplaneTypeId();
}

public interface FlightSegment {
	
	public String getFlightName();

	public String getOriginPort();

	public String getDestPort();

	public int getMiles();
		
}

import com.acmeair.entities.Customer;
import com.acmeair.entities.CustomerAddress;
import com.acmeair.entities.Customer.PhoneType;
import com.acmeair.service.CustomerService;
import com.acmeair.service.ServiceLocator;


public class CustomerLoader {

	private CustomerService customerService = ServiceLocator.instance().getService(CustomerService.class);

	
	public void loadCustomers(long numCustomers) {
		CustomerAddress address = customerService.createAddress("123 Main St.", null, "Anytown", "NC", "USA", "27617");
		for (long ii = 0; ii < numCustomers; ii++) {
			customerService.createCustomer("uid"+ii+"@email.com", "password", Customer.MemberShipStatus.GOLD, 1000000, 1000, "919-123-4567", PhoneType.BUSINESS, address);
		}
	}

}

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.*;
import java.math.*;

import com.acmeair.entities.AirportCodeMapping;
import com.acmeair.service.FlightService;
import com.acmeair.service.ServiceLocator;




public class FlightLoader {
	
	private static final int MAX_FLIGHTS_PER_SEGMENT = 30;
	

	private FlightService flightService = ServiceLocator.instance().getService(FlightService.class);

	public void loadFlights() throws Exception {
		InputStream csvInputStream = FlightLoader.class.getResourceAsStream("/mileage.csv");
		
		LineNumberReader lnr = new LineNumberReader(new InputStreamReader(csvInputStream));
		String line1 = lnr.readLine();
		StringTokenizer st = new StringTokenizer(line1, ",");
		ArrayList<AirportCodeMapping> airports = new ArrayList<AirportCodeMapping>();
		
		// read the first line which are airport names
		while (st.hasMoreTokens()) {
			AirportCodeMapping acm = flightService.createAirportCodeMapping(null, st.nextToken());
		//	acm.setAirportName(st.nextToken());
			airports.add(acm);
		}
		// read the second line which contains matching airport codes for the first line
		String line2 = lnr.readLine();
		st = new StringTokenizer(line2, ",");
		int ii = 0;
		while (st.hasMoreTokens()) {
			String airportCode = st.nextToken();
			airports.get(ii).setAirportCode(airportCode);
			ii++;
		}
		// read the other lines which are of format:
		// airport name, aiport code, distance from this airport to whatever airport is in the column from lines one and two
		String line;
		int flightNumber = 0;
		while (true) {
			line = lnr.readLine();
			if (line == null || line.trim().equals("")) {
				break;
			}
			st = new StringTokenizer(line, ",");
			String airportName = st.nextToken();
			String airportCode = st.nextToken();
			if (!alreadyInCollection(airportCode, airports)) {
				AirportCodeMapping acm = flightService.createAirportCodeMapping(airportCode, airportName);
				airports.add(acm);
			}
			int indexIntoTopLine = 0;
			while (st.hasMoreTokens()) {
				String milesString = st.nextToken();
				if (milesString.equals("NA")) {
					indexIntoTopLine++;
					continue;
				}
				int miles = Integer.parseInt(milesString);
				String toAirport = airports.get(indexIntoTopLine).getAirportCode();
				String flightId = "AA" + flightNumber;			
				flightService.storeFlightSegment(flightId, airportCode, toAirport, miles);
				Date now = new Date();
				for (int daysFromNow = 0; daysFromNow < MAX_FLIGHTS_PER_SEGMENT; daysFromNow++) {
					Calendar c = Calendar.getInstance();
					c.setTime(now);
					c.set(Calendar.HOUR_OF_DAY, 0);
				    c.set(Calendar.MINUTE, 0);
				    c.set(Calendar.SECOND, 0);
				    c.set(Calendar.MILLISECOND, 0);
					c.add(Calendar.DATE, daysFromNow);
					Date departureTime = c.getTime();
					Date arrivalTime = getArrivalTime(departureTime, miles);
					flightService.createNewFlight(flightId, departureTime, arrivalTime, new BigDecimal(500), new BigDecimal(200), 10, 200, "B747");
					
				}
				flightNumber++;
				indexIntoTopLine++;
			}
		}
		
		for (int jj = 0; jj < airports.size(); jj++) {
			flightService.storeAirportMapping(airports.get(jj));
		}
		lnr.close();
	}
	
	private static Date getArrivalTime(Date departureTime, int mileage) {
		double averageSpeed = 600.0; // 600 miles/hours
		double hours = (double) mileage / averageSpeed; // miles / miles/hour = hours
		double partsOfHour = hours % 1.0;
		int minutes = (int)(60.0 * partsOfHour);
		Calendar c = Calendar.getInstance();
		c.setTime(departureTime);
		c.add(Calendar.HOUR, (int)hours);
		c.add(Calendar.MINUTE, minutes);
		return c.getTime();
	}
	
	static private boolean alreadyInCollection(String airportCode, ArrayList<AirportCodeMapping> airports) {
		for (int ii = 0; ii < airports.size(); ii++) {
			if (airports.get(ii).getAirportCode().equals(airportCode)) {
				return true;
			}
		}
		return false;
	}
}

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

public class Loader {
	public static String REPOSITORY_LOOKUP_KEY = "com.acmeair.repository.type";

	private static Logger logger = Logger.getLogger(Loader.class.getName());

	public String queryLoader() {			
		String message = System.getProperty("loader.numCustomers");
		if (message == null){
			logger.info("The system property 'loader.numCustomers' has not been set yet. Looking up the default properties.");
			lookupDefaults();
			message = System.getProperty("loader.numCustomers");
		}				
		return message;	
	}
	
	public String loadDB(long numCustomers) {		
		String message = "";
		if(numCustomers == -1)
			message = execute();
		else {
			System.setProperty("loader.numCustomers", Long.toString(numCustomers));
			message = execute(numCustomers);
		}
		return message;	
	}
	
	
	
	public static void main(String args[]) throws Exception {
		Loader loader = new Loader();
		loader.execute();
	}
	
	
	private String execute() {
		String numCustomers = System.getProperty("loader.numCustomers");
		if (numCustomers == null){
			logger.info("The system property 'loader.numCustomers' has not been set yet. Looking up the default properties.");
			lookupDefaults();
			numCustomers = System.getProperty("loader.numCustomers");
		}
		return execute(Long.parseLong(numCustomers));
	}
	
	
	private String execute(long numCustomers) {
		FlightLoader flightLoader = new FlightLoader();
		CustomerLoader customerLoader = new CustomerLoader();

    	double length = 0;
		try {
			long start = System.currentTimeMillis();
			logger.info("Start loading flights");
			flightLoader.loadFlights();
			logger.info("Start loading " +  numCustomers + " customers");
			customerLoader.loadCustomers(numCustomers);
			long stop = System.currentTimeMillis();
			logger.info("Finished loading in " + (stop - start)/1000.0 + " seconds");
			length = (stop - start)/1000.0;
		}
		catch (Exception e) {
			e.printStackTrace();
		}		
		return "Loaded flights and "  +  numCustomers + " customers in " + length + " seconds";
	}
	
	
	private void lookupDefaults (){
		Properties props = getProperties();
		
        String numCustomers = props.getProperty("loader.numCustomers","100");
    	System.setProperty("loader.numCustomers", numCustomers);		
	}
	
	
	private Properties getProperties(){
        /*
         * Get Properties from loader.properties file. 
         * If the file does not exist, use default values
         */
		Properties props = new Properties();
		String propFileName = "/loader.properties";
		try{			
			InputStream propFileStream = Loader.class.getResourceAsStream(propFileName);
			props.load(propFileStream);
		//	props.load(new FileInputStream(propFileName));
		}catch(FileNotFoundException e){
			logger.info("Property file " + propFileName + " not found.");
		}catch(IOException e){
			logger.info("IOException - Property file " + propFileName + " not found.");
		}
    	return props;
	}
	/*
	private void execute(String args[]) {
		ApplicationContext ctx = null;
         //
         // Get Properties from loader.properties file. 
         // If the file does not exist, use default values
         //
		Properties props = new Properties();
		String propFileName = "/loader.properties";
		try{			
			InputStream propFileStream = Loader.class.getResourceAsStream(propFileName);
			props.load(propFileStream);
		//	props.load(new FileInputStream(propFileName));
		}catch(FileNotFoundException e){
			logger.info("Property file " + propFileName + " not found.");
		}catch(IOException e){
			logger.info("IOException - Property file " + propFileName + " not found.");
		}
		
        String numCustomers = props.getProperty("loader.numCustomers","100");
    	System.setProperty("loader.numCustomers", numCustomers);

		String type = null;
		String lookup = REPOSITORY_LOOKUP_KEY.replace('.', '/');
		javax.naming.Context context = null;
		javax.naming.Context envContext;
		try {
			context = new javax.naming.InitialContext();
			envContext = (javax.naming.Context) context.lookup("java:comp/env");
			if (envContext != null)
				type = (String) envContext.lookup(lookup);
		} catch (NamingException e) {
			// e.printStackTrace();
		}
		
		if (type != null) {
			logger.info("Found repository in web.xml:" + type);
		}
		else if (context != null) {
			try {
				type = (String) context.lookup(lookup);
				if (type != null)
					logger.info("Found repository in server.xml:" + type);
			} catch (NamingException e) {
				// e.printStackTrace();
			}
		}

		if (type == null) {
			type = System.getProperty(REPOSITORY_LOOKUP_KEY);
			if (type != null)
				logger.info("Found repository in jvm property:" + type);
			else {
				type = System.getenv(REPOSITORY_LOOKUP_KEY);
				if (type != null)
					logger.info("Found repository in environment property:" + type);
			}
		}

		if (type ==null) // Default to wxsdirect
		{
			type = "wxsdirect";
			logger.info("Using default repository :" + type);
		}
		if (type.equals("wxsdirect"))
			ctx = new AnnotationConfigApplicationContext(WXSDirectAppConfig.class);
		else if (type.equals("mongodirect"))
			ctx = new AnnotationConfigApplicationContext(MongoDirectAppConfig.class);
		else
		{
			logger.info("Did not find a matching config. Using default repository wxsdirect instead");
			ctx = new AnnotationConfigApplicationContext(WXSDirectAppConfig.class);
		}
		
		FlightLoader flightLoader = ctx.getBean(FlightLoader.class);
		CustomerLoader customerLoader = ctx.getBean(CustomerLoader.class);
		
		try {
			long start = System.currentTimeMillis();
			logger.info("Start loading flights");
			flightLoader.loadFlights();
			logger.info("Start loading " +  numCustomers + " customers");
			customerLoader.loadCustomers(Long.parseLong(numCustomers));
			long stop = System.currentTimeMillis();
			logger.info("Finished loading in " + (stop - start)/1000.0 + " seconds");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	*/
}

import java.util.List;

import com.acmeair.entities.Booking;


public interface BookingService {

	//String bookFlight(String customerId, FlightPK flightId);
//	String bookFlight(String customerId, String flightId);
	
	String bookFlight(String customerId, String flightSegmentId, String FlightId);
	
	Booking getBooking(String user, String id);

	List<Booking> getBookingsByUser(String user);
	
	void cancelBooking(String user, String id);
	
	Long count();
}

import java.util.Calendar;
import java.util.Date;

import javax.inject.Inject;

import com.acmeair.entities.Customer;
import com.acmeair.entities.CustomerAddress;
import com.acmeair.entities.Customer.MemberShipStatus;
import com.acmeair.entities.Customer.PhoneType;
import com.acmeair.entities.CustomerSession;

public abstract class CustomerService {
	protected static final int DAYS_TO_ALLOW_SESSION = 1;
	
	@Inject
	protected KeyGenerator keyGenerator;
	
	public abstract Customer createCustomer(
			String username, String password, MemberShipStatus status, int total_miles,
			int miles_ytd, String phoneNumber, PhoneType phoneNumberType, CustomerAddress address);
	
	public abstract CustomerAddress createAddress (String streetAddress1, String streetAddress2,
			String city, String stateProvince, String country, String postalCode);
	
	public abstract Customer updateCustomer(Customer customer);
		
	
	protected abstract Customer getCustomer(String username);
	
	public Customer getCustomerByUsername(String username) {
		Customer c = getCustomer(username);
		if (c != null) {
			c.setPassword(null);
		}
		return c;
	}
	
	public boolean validateCustomer(String username, String password) {
		boolean validatedCustomer = false;
		Customer customerToValidate = getCustomer(username);
		if (customerToValidate != null) {
			validatedCustomer = password.equals(customerToValidate.getPassword());
		}
		return validatedCustomer;
	}
	
	public Customer getCustomerByUsernameAndPassword(String username,
			String password) {
		Customer c = getCustomer(username);
		if (!c.getPassword().equals(password)) {
			return null;
		}
		// Should we also set the password to null?
		return c;
	}
		
	public CustomerSession validateSession(String sessionid) {
		CustomerSession cSession = getSession(sessionid);
		if (cSession == null) {
			return null;
		}

		Date now = new Date();

		if (cSession.getTimeoutTime().before(now)) {
			removeSession(cSession);
			return null;
		}
		return cSession;		
	}
	
	protected abstract CustomerSession getSession(String sessionid);
	
	protected abstract void removeSession(CustomerSession session);
	
	public CustomerSession createSession(String customerId) {
		String sessionId = keyGenerator.generate().toString();
		Date now = new Date();
		Calendar c = Calendar.getInstance();
		c.setTime(now);
		c.add(Calendar.DAY_OF_YEAR, DAYS_TO_ALLOW_SESSION);
		Date expiration = c.getTime();
		
		return createSession(sessionId, customerId, now, expiration);
	}
	
	protected abstract CustomerSession createSession(String sessionId, String customerId, Date creation, Date expiration);

	public abstract void invalidateSession(String sessionid);
	
	public abstract Long count();
	
	public abstract Long countSessions();
	
}

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.inject.Qualifier;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;


@Qualifier @Retention(RUNTIME) @Target({TYPE, METHOD, FIELD, PARAMETER})
public @interface DataService {
	String name() default "none";
	String description() default "none";

}

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.acmeair.entities.Flight;
import com.acmeair.entities.FlightSegment;
import com.acmeair.entities.AirportCodeMapping;

public abstract class FlightService {
	protected Logger logger =  Logger.getLogger(FlightService.class.getName());
	
	//TODO:need to find a way to invalidate these maps
	protected static ConcurrentHashMap<String, FlightSegment> originAndDestPortToSegmentCache = new ConcurrentHashMap<String,FlightSegment>();
	protected static ConcurrentHashMap<String, List<Flight>> flightSegmentAndDataToFlightCache = new ConcurrentHashMap<String,List<Flight>>();
	protected static ConcurrentHashMap<String, Flight> flightPKtoFlightCache = new ConcurrentHashMap<String, Flight>();
	
	

	public Flight getFlightByFlightId(String flightId, String flightSegment) {
		try {
			Flight flight = flightPKtoFlightCache.get(flightId);
			if (flight == null) {				
				flight = getFlight(flightId, flightSegment);
				if (flightId != null && flight != null) {
					flightPKtoFlightCache.putIfAbsent(flightId, flight);
				}
			}
			return flight;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	
	protected abstract Flight getFlight(String flightId, String flightSegment);
	
	public List<Flight> getFlightByAirportsAndDepartureDate(String fromAirport,	String toAirport, Date deptDate) {
		if(logger.isLoggable(Level.FINE))
			logger.fine("Search for flights from "+ fromAirport + " to " + toAirport + " on " + deptDate.toString());

		String originPortAndDestPortQueryString= fromAirport+toAirport;
		FlightSegment segment = originAndDestPortToSegmentCache.get(originPortAndDestPortQueryString);

		if (segment == null) {
			segment = getFlightSegment(fromAirport, toAirport);
			originAndDestPortToSegmentCache.putIfAbsent(originPortAndDestPortQueryString, segment);
		}		
		// cache flights that not available (checks against sentinel value above indirectly)
		if (segment.getFlightName() == null) {
			return new ArrayList<Flight>(); 
		}

		String segId = segment.getFlightName();
		String flightSegmentIdAndScheduledDepartureTimeQueryString = segId + deptDate.toString();
		List<Flight> flights = flightSegmentAndDataToFlightCache.get(flightSegmentIdAndScheduledDepartureTimeQueryString);

		if (flights == null) {				
			flights = getFlightBySegment(segment, deptDate);
			flightSegmentAndDataToFlightCache.putIfAbsent(flightSegmentIdAndScheduledDepartureTimeQueryString, flights);
		}
		if(logger.isLoggable(Level.FINEST))
			logger.finest("Returning "+ flights);
		return flights;

	}

	// NOTE:  This is not cached
	public List<Flight> getFlightByAirports(String fromAirport, String toAirport) {
			FlightSegment segment = getFlightSegment(fromAirport, toAirport);
			if (segment == null) {
				return new ArrayList<Flight>(); 
			}	
			return getFlightBySegment(segment, null);
	}
	
	protected abstract FlightSegment getFlightSegment(String fromAirport, String toAirport);
	
	protected abstract List<Flight> getFlightBySegment(FlightSegment segment, Date deptDate);  
			
	public abstract void storeAirportMapping(AirportCodeMapping mapping);

	public abstract AirportCodeMapping createAirportCodeMapping(String airportCode, String airportName);
	
	public abstract Flight createNewFlight(String flightSegmentId,
			Date scheduledDepartureTime, Date scheduledArrivalTime,
			BigDecimal firstClassBaseCost, BigDecimal economyClassBaseCost,
			int numFirstClassSeats, int numEconomyClassSeats,
			String airplaneTypeId);

	public abstract void storeFlightSegment(FlightSegment flightSeg);
	
	public abstract void storeFlightSegment(String flightName, String origPort, String destPort, int miles);
	
	public abstract Long countFlightSegments();
	
	public abstract Long countFlights();
	
	public abstract Long countAirports();
	
}

public class KeyGenerator {
	
	public Object generate() {
		return java.util.UUID.randomUUID().toString();
	}
}

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.PostConstruct;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import java.util.logging.Logger;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;


public class ServiceLocator {

	public static String REPOSITORY_LOOKUP_KEY = "com.acmeair.repository.type";
	private static String serviceType;
	private static Logger logger = Logger.getLogger(ServiceLocator.class.getName());

	private static AtomicReference<ServiceLocator> singletonServiceLocator = new AtomicReference<ServiceLocator>();

	@Inject
	BeanManager beanManager;
	
	public static ServiceLocator instance() {
		if (singletonServiceLocator.get() == null) {
			synchronized (singletonServiceLocator) {
				if (singletonServiceLocator.get() == null) {
					singletonServiceLocator.set(new ServiceLocator());
				}
			}
		}
		return singletonServiceLocator.get();
	}
	
	@PostConstruct
	private void initialization()  {		
		if(beanManager == null){
			logger.info("Attempting to look up BeanManager through JNDI at java:comp/BeanManager");
			try {
				beanManager = (BeanManager) new InitialContext().lookup("java:comp/BeanManager");
			} catch (NamingException e) {
				logger.severe("BeanManager not found at java:comp/BeanManager");
			}
		}
		
		if(beanManager == null){
			logger.info("Attempting to look up BeanManager through JNDI at java:comp/env/BeanManager");
			try {
				beanManager = (BeanManager) new InitialContext().lookup("java:comp/env/BeanManager");
			} catch (NamingException e) {
				logger.severe("BeanManager not found at java:comp/env/BeanManager ");
			}
		}
	}
	
	public static void updateService(String serviceName){
		logger.info("Service Locator updating service to : " + serviceName);
		serviceType = serviceName;
	}

	private ServiceLocator() {
		String type = null;
		String lookup = REPOSITORY_LOOKUP_KEY.replace('.', '/');
		javax.naming.Context context = null;
		javax.naming.Context envContext = null;
		try {
			context = new javax.naming.InitialContext();
			envContext = (javax.naming.Context) context.lookup("java:comp/env");
			if (envContext != null)
				type = (String) envContext.lookup(lookup);
		} catch (NamingException e) {
			// e.printStackTrace();
		}
		
		if (type != null) {
			logger.info("Found repository in web.xml:" + type);
		}
		else if (context != null) {
			try {
				type = (String) context.lookup(lookup);
				if (type != null)
					logger.info("Found repository in server.xml:" + type);
			} catch (NamingException e) {
				// e.printStackTrace();
			}
		}

		if (type == null) {
			type = System.getProperty(REPOSITORY_LOOKUP_KEY);
			if (type != null)
				logger.info("Found repository in jvm property:" + type);
			else {
				type = System.getenv(REPOSITORY_LOOKUP_KEY);
				if (type != null)
					logger.info("Found repository in environment property:" + type);
			}
		}

		if(beanManager == null) {
			logger.info("Attempting to look up BeanManager through JNDI at java:comp/BeanManager");
			try {
				beanManager = (BeanManager) new InitialContext().lookup("java:comp/BeanManager");
			} catch (NamingException e) {
				logger.severe("BeanManager not found at java:comp/BeanManager");
			}
		}	
		
		if(beanManager == null){
			logger.info("Attempting to look up BeanManager through JNDI at java:comp/env/BeanManager");
			try {
				beanManager = (BeanManager) new InitialContext().lookup("java:comp/env/BeanManager");
			} catch (NamingException e) {
				logger.severe("BeanManager not found at java:comp/env/BeanManager ");
			}
		}
		
		if (type==null)
		{
			String vcapJSONString = System.getenv("VCAP_SERVICES");
			if (vcapJSONString != null) {
				logger.info("Reading VCAP_SERVICES");
				Object jsonObject = JSONValue.parse(vcapJSONString);
				logger.fine("jsonObject = " + ((JSONObject)jsonObject).toJSONString());
				JSONObject json = (JSONObject)jsonObject;
				String key;
				for (Object k: json.keySet())
				{
					key = (String ) k;
					if (key.startsWith("ElasticCaching")||key.startsWith("DataCache"))
					{
						logger.info("VCAP_SERVICES existed with service:"+key);
						type ="wxs";
						break;
					}
					if (key.startsWith("mongo"))
					{
						logger.info("VCAP_SERVICES existed with service:"+key);
						type ="morphia";
						break;
					}
					if (key.startsWith("redis"))
					{
						logger.info("VCAP_SERVICES existed with service:"+key);
						type ="redis";
						break;
					}
					if (key.startsWith("mysql")|| key.startsWith("cleardb"))
					{
						logger.info("VCAP_SERVICES existed with service:"+key);
						type ="mysql";
						break;
					}
					if (key.startsWith("postgresql"))
					{
						logger.info("VCAP_SERVICES existed with service:"+key);
						type ="postgresql";
						break;
					}
					if (key.startsWith("db2"))
					{
						logger.info("VCAP_SERVICES existed with service:"+key);
						type ="db2";
						break;
					}
				}
			}
		}
				
		serviceType = type;
		logger.info("ServiceType is now : " + serviceType);
		if (type ==null) {
			logger.warning("Can not determine type. Use default service implementation.");			
		}
	}

	@SuppressWarnings("unchecked")
	public <T> T getService (Class<T> clazz) {		
		logger.fine("Looking up service:  "+clazz.getName() + " with service type: " + serviceType);
		if(beanManager == null) {
			logger.severe("BeanManager is null!!!");
		}		
    	Set<Bean<?>> beans = beanManager.getBeans(clazz,new AnnotationLiteral<Any>() {
			private static final long serialVersionUID = 1L;});
    	for (Bean<?> bean : beans) {
    		logger.fine(" Bean = "+bean.getBeanClass().getName());
    		for (Annotation qualifer: bean.getQualifiers()) {
    			if(null == serviceType) {
    				logger.warning("Service type is not set, searching for the default implementation.");
    				if(Default.class.getName().equalsIgnoreCase(qualifer.annotationType().getName())){
    					CreationalContext<?> ctx = beanManager.createCreationalContext(bean);
    					return  (T) beanManager.getReference(bean, clazz, ctx);
    				}
    			} else {    				   
    				if(DataService.class.getName().equalsIgnoreCase(qualifer.annotationType().getName())){
    					DataService service = (DataService) qualifer;
    					logger.fine("   name="+service.name()+" description="+service.description());
    					if(serviceType.equalsIgnoreCase(service.name())) {
    						CreationalContext<?> ctx = beanManager.createCreationalContext(bean);
    						return  (T) beanManager.getReference(bean, clazz, ctx);

    					}
    				}
    			}
    		}
    	}
    	logger.warning("No Service of type: " + serviceType + " found for "+clazz.getName()+" ");
    	return null;
	}
	
	/**
	 * Retrieves the services that are available for use with the description for each service. 
	 * The Services are determined by looking up all of the implementations of the 
	 * Customer Service interface that are using the  DataService qualifier annotation. 
	 * The DataService annotation contains the service name and description information. 
	 * @return Map containing a list of services available and a description of each one.
	 */
	public Map<String,String> getServices (){
		TreeMap<String,String> services = new TreeMap<String,String>();
		logger.fine("Getting CustomerService Impls");
    	Set<Bean<?>> beans = beanManager.getBeans(CustomerService.class,new AnnotationLiteral<Any>() {
			private static final long serialVersionUID = 1L;});
    	for (Bean<?> bean : beans) {    		
    		for (Annotation qualifer: bean.getQualifiers()){
    			if(DataService.class.getName().equalsIgnoreCase(qualifer.annotationType().getName())){
    				DataService service = (DataService) qualifer;
    				logger.fine("   name="+service.name()+" description="+service.description());
    				services.put(service.name(), service.description());
    			}
    		}
    	}    	
    	return services;
	}
	
	/**
	 * The type of service implementation that the application is 
	 * currently configured to use.  
	 * 
	 * @return The type of service in use, or "default" if no service has been set. 
	 */
	public String getServiceType (){
		if(serviceType == null){
			return "default";
		}
		return serviceType;
	}
}

public interface TransactionService {
	
	void prepareForTransaction() throws Exception;

}

import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import com.acmeair.service.BookingService;
import com.acmeair.service.CustomerService;
import com.acmeair.service.FlightService;
import com.acmeair.service.ServiceLocator;


@Path("/config")
public class AcmeAirConfiguration {
    
	@Inject
	BeanManager beanManager;
	Logger logger = Logger.getLogger(AcmeAirConfiguration.class.getName());

	private BookingService bs = ServiceLocator.instance().getService(BookingService.class);
	private CustomerService customerService = ServiceLocator.instance().getService(CustomerService.class);
	private FlightService flightService = ServiceLocator.instance().getService(FlightService.class);

	
    public AcmeAirConfiguration() {
        super();
    }

	@PostConstruct
	private void initialization()  {		
		if(beanManager == null){
			logger.info("Attempting to look up BeanManager through JNDI at java:comp/BeanManager");
			try {
				beanManager = (BeanManager) new InitialContext().lookup("java:comp/BeanManager");
			} catch (NamingException e) {
				logger.severe("BeanManager not found at java:comp/BeanManager");
			}
		}
		
		if(beanManager == null){
			logger.info("Attempting to look up BeanManager through JNDI at java:comp/env/BeanManager");
			try {
				beanManager = (BeanManager) new InitialContext().lookup("java:comp/env/BeanManager");
			} catch (NamingException e) {
				logger.severe("BeanManager not found at java:comp/env/BeanManager ");
			}
		}
	}
    
    
	@GET
	@Path("/dataServices")
	@Produces("application/json")
	public ArrayList<ServiceData> getDataServiceInfo() {
		try {	
			ArrayList<ServiceData> list = new ArrayList<ServiceData>();
			Map<String, String> services =  ServiceLocator.instance().getServices();
			logger.fine("Get data service configuration info");
			for (Map.Entry<String, String> entry : services.entrySet()){
				ServiceData data = new ServiceData();
				data.name = entry.getKey();
				data.description = entry.getValue();
				list.add(data);
			}
			
			return list;
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	
	@GET
	@Path("/activeDataService")
	@Produces("application/json")
	public Response getActiveDataServiceInfo() {
		try {		
			logger.fine("Get active Data Service info");
			return  Response.ok(ServiceLocator.instance().getServiceType()).build();
		}
		catch (Exception e) {
			e.printStackTrace();
			return Response.ok("Unknown").build();
		}
	}
	
	@GET
	@Path("/runtime")
	@Produces("application/json")
	public ArrayList<ServiceData> getRuntimeInfo() {
		try {
			logger.fine("Getting Runtime info");
			ArrayList<ServiceData> list = new ArrayList<ServiceData>();
			ServiceData data = new ServiceData();
			data.name = "Runtime";
			data.description = "Java";			
			list.add(data);
			
			data = new ServiceData();
			data.name = "Version";
			data.description = System.getProperty("java.version");			
			list.add(data);
			
			data = new ServiceData();
			data.name = "Vendor";
			data.description = System.getProperty("java.vendor");			
			list.add(data);
			
			return list;
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	
	class ServiceData {
		public String name = "";
		public String description = "";
	}
	
	@GET
	@Path("/countBookings")
	@Produces("application/json")
	public Response countBookings() {
		try {
			Long count = bs.count();			
			return Response.ok(count).build();
		}
		catch (Exception e) {
			e.printStackTrace();
			return Response.ok(-1).build();
		}
	}
	
	@GET
	@Path("/countCustomers")
	@Produces("application/json")
	public Response countCustomer() {
		try {
			Long customerCount = customerService.count();
			
			return Response.ok(customerCount).build();
		}
		catch (Exception e) {
			e.printStackTrace();
			return Response.ok(-1).build();
		}
	}
	
	
	@GET
	@Path("/countSessions")
	@Produces("application/json")
	public Response countCustomerSessions() {
		try {
			Long customerCount = customerService.countSessions();
			
			return Response.ok(customerCount).build();
		}
		catch (Exception e) {
			e.printStackTrace();
			return Response.ok(-1).build();
		}
	}
	
	
	@GET
	@Path("/countFlights")
	@Produces("application/json")
	public Response countFlights() {
		try {
			Long count = flightService.countFlights();			
			return Response.ok(count).build();
		}
		catch (Exception e) {
			e.printStackTrace();
			return Response.ok(-1).build();
		}
	}
	
	@GET
	@Path("/countFlightSegments")
	@Produces("application/json")
	public Response countFlightSegments() {
		try {
			Long count = flightService.countFlightSegments();			
			return Response.ok(count).build();
		}
		catch (Exception e) {
			e.printStackTrace();
			return Response.ok(-1).build();
		}
	}
	
	@GET
	@Path("/countAirports")
	@Produces("application/json")
	public Response countAirports() {
		try {			
			Long count = flightService.countAirports();	
			return Response.ok(count).build();
		}
		catch (Exception e) {
			e.printStackTrace();
			return Response.ok(-1).build();
		}
	}
	
}

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import com.acmeair.loader.Loader;


@Path("/loader")
public class LoaderREST {

//	private static Logger logger = Logger.getLogger(LoaderREST.class.getName());
	
	@Inject
	private Loader loader;	
	
	@GET
	@Path("/query")
	@Produces("text/plain")
	public Response queryLoader() {			
		String response = loader.queryLoader();
		return Response.ok(response).build();	
	}
	
	
	@GET
	@Path("/load")
	@Produces("text/plain")
	public Response loadDB(@DefaultValue("-1") @QueryParam("numCustomers") long numCustomers) {	
		String response = loader.loadDB(numCustomers);
		return Response.ok(response).build();	
	}
}

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.acmeair.entities.CustomerAddress;



@XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
@XmlRootElement(name="CustomerAddress")
public class AddressInfo implements Serializable{
	
	private static final long serialVersionUID = 1L;
	private String streetAddress1;
	private String streetAddress2;
	private String city;
	private String stateProvince;
	private String country;
	private String postalCode;

	public AddressInfo() {
	}
	
	public AddressInfo(String streetAddress1, String streetAddress2,
			String city, String stateProvince, String country, String postalCode) {
		super();
		this.streetAddress1 = streetAddress1;
		this.streetAddress2 = streetAddress2;
		this.city = city;
		this.stateProvince = stateProvince;
		this.country = country;
		this.postalCode = postalCode;
	}
	
	public AddressInfo(CustomerAddress address) {
		super();
		this.streetAddress1 = address.getStreetAddress1();
		this.streetAddress2 = address.getStreetAddress2();
		this.city = address.getCity();
		this.stateProvince = address.getStateProvince();
		this.country = address.getCountry();
		this.postalCode = address.getPostalCode();
	}
	
	public String getStreetAddress1() {
		return streetAddress1;
	}
	public void setStreetAddress1(String streetAddress1) {
		this.streetAddress1 = streetAddress1;
	}
	public String getStreetAddress2() {
		return streetAddress2;
	}
	public void setStreetAddress2(String streetAddress2) {
		this.streetAddress2 = streetAddress2;
	}
	public String getCity() {
		return city;
	}
	public void setCity(String city) {
		this.city = city;
	}
	public String getStateProvince() {
		return stateProvince;
	}
	public void setStateProvince(String stateProvince) {
		this.stateProvince = stateProvince;
	}
	public String getCountry() {
		return country;
	}
	public void setCountry(String country) {
		this.country = country;
	}
	public String getPostalCode() {
		return postalCode;
	}
	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}
	@Override
	public String toString() {
		return "CustomerAddress [streetAddress1=" + streetAddress1
				+ ", streetAddress2=" + streetAddress2 + ", city=" + city
				+ ", stateProvince=" + stateProvince + ", country=" + country
				+ ", postalCode=" + postalCode + "]";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AddressInfo other = (AddressInfo) obj;
		if (city == null) {
			if (other.city != null)
				return false;
		} else if (!city.equals(other.city))
			return false;
		if (country == null) {
			if (other.country != null)
				return false;
		} else if (!country.equals(other.country))
			return false;
		if (postalCode == null) {
			if (other.postalCode != null)
				return false;
		} else if (!postalCode.equals(other.postalCode))
			return false;
		if (stateProvince == null) {
			if (other.stateProvince != null)
				return false;
		} else if (!stateProvince.equals(other.stateProvince))
			return false;
		if (streetAddress1 == null) {
			if (other.streetAddress1 != null)
				return false;
		} else if (!streetAddress1.equals(other.streetAddress1))
			return false;
		if (streetAddress2 == null) {
			if (other.streetAddress2 != null)
				return false;
		} else if (!streetAddress2.equals(other.streetAddress2))
			return false;
		return true;
	}
	
	
}

import java.util.Date;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.acmeair.entities.Booking;

@XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
@XmlRootElement(name="Booking")
public class BookingInfo {

	@XmlElement(name="bookingId")
	private String bookingId;	
	
	@XmlElement(name="flightId")
	private String flightId;
	
	@XmlElement(name="customerId")
	private String customerId;
	
	@XmlElement(name="dateOfBooking")
	private Date dateOfBooking;
	
	@XmlElement(name="pkey")
	private BookingPKInfo pkey;
	
	public BookingInfo() {
		
	}

	public BookingInfo(Booking booking){
		this.bookingId = booking.getBookingId();
		this.flightId = booking.getFlightId();
		this.customerId = booking.getCustomerId();
		this.dateOfBooking = booking.getDateOfBooking();
		this.pkey = new BookingPKInfo(this.customerId, this.bookingId);
	}
	
	
	public String getBookingId() {
		return bookingId;
	}
	public void setBookingId(String bookingId) {
		this.bookingId = bookingId;
	}
	public String getFlightId() {
		return flightId;
	}
	public void setFlightId(String flightId) {
		this.flightId = flightId;
	}
	public String getCustomerId() {
		return customerId;
	}
	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}
	public Date getDateOfBooking() {
		return dateOfBooking;
	}
	public void setDateOfBooking(Date dateOfBooking) {
		this.dateOfBooking = dateOfBooking;
	}	
	public BookingPKInfo getPkey(){
		return pkey;
	}
	
}

import javax.xml.bind.annotation.XmlElement;



public class BookingPKInfo {

	@XmlElement(name="id")
	private String id;
	
	@XmlElement(name="customerId")
	private String customerId;
	
	public BookingPKInfo() {
		
	}


	public BookingPKInfo(String customerId,String id) {
		
		this.id = id;
		this.customerId = customerId;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getCustomerId() {
		return customerId;
	}

	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}
}

public class BookingReceiptInfo {
	
	private String departBookingId;
	private String returnBookingId;
	private boolean oneWay;
	
	public BookingReceiptInfo(String departBookingId, String returnBookingId, boolean oneWay) {
		this.departBookingId = departBookingId;
		this.returnBookingId = returnBookingId;
		this.oneWay = oneWay;
	}
	
	public BookingReceiptInfo() {
	}
	
	public String getDepartBookingId() {
		return departBookingId;
	}
	public void setDepartBookingId(String departBookingId) {
		this.departBookingId = departBookingId;
	}
	public String getReturnBookingId() {
		return returnBookingId;
	}
	public void setReturnBookingId(String returnBookingId) {
		this.returnBookingId = returnBookingId;
	}
	public boolean isOneWay() {
		return oneWay;
	}
	public void setOneWay(boolean oneWay) {
		this.oneWay = oneWay;
	}
	
	@Override
	public String toString() {
		return "BookingInfo [departBookingId=" + departBookingId
				+ ", returnBookingId=" + returnBookingId + ", oneWay=" + oneWay
				+ "]";
	}
}

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.acmeair.entities.Customer;


@XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
@XmlRootElement(name="Customer")
public class CustomerInfo implements Serializable{

	
	private static final long serialVersionUID = 1L;
	@XmlElement(name="_id")
	private String _id;
	
	@XmlElement(name="password")
	private String password;
	
	@XmlElement(name="status")
	private String status;
	
	@XmlElement(name="total_miles")
	private int total_miles;
	
	@XmlElement(name="miles_ytd")
	private int miles_ytd;

	@XmlElement(name="address")
	private AddressInfo address;
	
	@XmlElement(name="phoneNumber")
	private String phoneNumber;
	
	@XmlElement(name="phoneNumberType")
	private String phoneNumberType;
	
	public CustomerInfo() {
	}
	
	public CustomerInfo(String username, String password, String status, int total_miles, int miles_ytd, AddressInfo address, String phoneNumber, String phoneNumberType) {
		this._id = username;
		this.password = password;
		this.status = status;
		this.total_miles = total_miles;
		this.miles_ytd = miles_ytd;
		this.address = address;
		this.phoneNumber = phoneNumber;
		this.phoneNumberType = phoneNumberType;
	}
	
	public CustomerInfo(Customer c) {
		this._id = c.getUsername();
		this.password = c.getPassword();
		this.status = c.getStatus().toString();
		this.total_miles = c.getTotal_miles();
		this.miles_ytd = c.getMiles_ytd();
		this.address = new AddressInfo(c.getAddress());
		this.phoneNumber = c.getPhoneNumber();
		this.phoneNumberType = c.getPhoneNumberType().toString();
	}

	public String getUsername() {
		return _id;
	}
	
	public void setUsername(String username) {
		this._id = username;
	}
	
	public String getPassword() {
		return password;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	public String getStatus() {
		return status;
	}
	
	public void setStatus(String status) {
		this.status = status;
	}
	
	public int getTotal_miles() {
		return total_miles;
	}
	
	public void setTotal_miles(int total_miles) {
		this.total_miles = total_miles;
	}
	
	public int getMiles_ytd() {
		return miles_ytd;
	}
	
	public void setMiles_ytd(int miles_ytd) {
		this.miles_ytd = miles_ytd;
	}
	
	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public String getPhoneNumberType() {
		return phoneNumberType;
	}

	public void setPhoneNumberType(String phoneNumberType) {
		this.phoneNumberType = phoneNumberType;
	}

	public AddressInfo getAddress() {
		return address;
	}

	public void setAddress(AddressInfo address) {
		this.address = address;
	}

	@Override
	public String toString() {
		return "Customer [id=" + _id + ", password=" + password + ", status="
				+ status + ", total_miles=" + total_miles + ", miles_ytd="
				+ miles_ytd + ", address=" + address + ", phoneNumber="
				+ phoneNumber + ", phoneNumberType=" + phoneNumberType + "]";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CustomerInfo other = (CustomerInfo) obj;
		if (address == null) {
			if (other.address != null)
				return false;
		} else if (!address.equals(other.address))
			return false;
		if (_id == null) {
			if (other._id != null)
				return false;
		} else if (!_id.equals(other._id))
			return false;
		if (miles_ytd != other.miles_ytd)
			return false;
		if (password == null) {
			if (other.password != null)
				return false;
		} else if (!password.equals(other.password))
			return false;
		if (phoneNumber == null) {
			if (other.phoneNumber != null)
				return false;
		} else if (!phoneNumber.equals(other.phoneNumber))
			return false;
		if (phoneNumberType != other.phoneNumberType)
			return false;
		if (status != other.status)
			return false;
		if (total_miles != other.total_miles)
			return false;
		return true;
	}

}

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.acmeair.entities.Customer;


@XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
@XmlRootElement(name="Customer")
public class CustomerInfo implements Serializable{

	
	private static final long serialVersionUID = 1L;
	@XmlElement(name="_id")
	private String _id;
	
	@XmlElement(name="password")
	private String password;
	
	@XmlElement(name="status")
	private String status;
	
	@XmlElement(name="total_miles")
	private int total_miles;
	
	@XmlElement(name="miles_ytd")
	private int miles_ytd;

	@XmlElement(name="address")
	private AddressInfo address;
	
	@XmlElement(name="phoneNumber")
	private String phoneNumber;
	
	@XmlElement(name="phoneNumberType")
	private String phoneNumberType;
	
	public CustomerInfo() {
	}
	
	public CustomerInfo(String username, String password, String status, int total_miles, int miles_ytd, AddressInfo address, String phoneNumber, String phoneNumberType) {
		this._id = username;
		this.password = password;
		this.status = status;
		this.total_miles = total_miles;
		this.miles_ytd = miles_ytd;
		this.address = address;
		this.phoneNumber = phoneNumber;
		this.phoneNumberType = phoneNumberType;
	}
	
	public CustomerInfo(Customer c) {
		this._id = c.getUsername();
		this.password = c.getPassword();
		this.status = c.getStatus().toString();
		this.total_miles = c.getTotal_miles();
		this.miles_ytd = c.getMiles_ytd();
		this.address = new AddressInfo(c.getAddress());
		this.phoneNumber = c.getPhoneNumber();
		this.phoneNumberType = c.getPhoneNumberType().toString();
	}

	public String getUsername() {
		return _id;
	}
	
	public void setUsername(String username) {
		this._id = username;
	}
	
	public String getPassword() {
		return password;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	public String getStatus() {
		return status;
	}
	
	public void setStatus(String status) {
		this.status = status;
	}
	
	public int getTotal_miles() {
		return total_miles;
	}
	
	public void setTotal_miles(int total_miles) {
		this.total_miles = total_miles;
	}
	
	public int getMiles_ytd() {
		return miles_ytd;
	}
	
	public void setMiles_ytd(int miles_ytd) {
		this.miles_ytd = miles_ytd;
	}
	
	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public String getPhoneNumberType() {
		return phoneNumberType;
	}

	public void setPhoneNumberType(String phoneNumberType) {
		this.phoneNumberType = phoneNumberType;
	}

	public AddressInfo getAddress() {
		return address;
	}

	public void setAddress(AddressInfo address) {
		this.address = address;
	}

	@Override
	public String toString() {
		return "Customer [id=" + _id + ", password=" + password + ", status="
				+ status + ", total_miles=" + total_miles + ", miles_ytd="
				+ miles_ytd + ", address=" + address + ", phoneNumber="
				+ phoneNumber + ", phoneNumberType=" + phoneNumberType + "]";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CustomerInfo other = (CustomerInfo) obj;
		if (address == null) {
			if (other.address != null)
				return false;
		} else if (!address.equals(other.address))
			return false;
		if (_id == null) {
			if (other._id != null)
				return false;
		} else if (!_id.equals(other._id))
			return false;
		if (miles_ytd != other.miles_ytd)
			return false;
		if (password == null) {
			if (other.password != null)
				return false;
		} else if (!password.equals(other.password))
			return false;
		if (phoneNumber == null) {
			if (other.phoneNumber != null)
				return false;
		} else if (!phoneNumber.equals(other.phoneNumber))
			return false;
		if (phoneNumberType != other.phoneNumberType)
			return false;
		if (status != other.status)
			return false;
		if (total_miles != other.total_miles)
			return false;
		return true;
	}

}

import java.math.BigDecimal;
import java.util.Date;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.acmeair.entities.Flight;

@XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
@XmlRootElement(name="Flight")
public class FlightInfo {
	
	@XmlElement(name="_id")
	private String _id;	
	private String flightSegmentId;		
	private Date scheduledDepartureTime;
	private Date scheduledArrivalTime;
	private BigDecimal firstClassBaseCost;
	private BigDecimal economyClassBaseCost;
	private int numFirstClassSeats;
	private int numEconomyClassSeats;
	private String airplaneTypeId;
	private FlightSegmentInfo flightSegment;
	
	@XmlElement(name="pkey")
	private FlightPKInfo pkey;
	
	public FlightInfo(){
		
	}
	
	public FlightInfo(Flight flight){
		this._id = flight.getFlightId();
		this.flightSegmentId = flight.getFlightSegmentId();
		this.scheduledDepartureTime = flight.getScheduledDepartureTime();
		this.scheduledArrivalTime = flight.getScheduledArrivalTime();
		this.firstClassBaseCost = flight.getFirstClassBaseCost();
		this.economyClassBaseCost = flight.getEconomyClassBaseCost();
		this.numFirstClassSeats = flight.getNumFirstClassSeats();
		this.numEconomyClassSeats = flight.getNumEconomyClassSeats();
		this.airplaneTypeId = flight.getAirplaneTypeId();
		if(flight.getFlightSegment() != null){
			this.flightSegment = new FlightSegmentInfo(flight.getFlightSegment());
		} else {
			this.flightSegment = null;
		}
		this.pkey = new FlightPKInfo(this.flightSegmentId, this._id);
	}
	
	public String get_id() {
		return _id;
	}
	public void set_id(String _id) {
		this._id = _id;
	}
	public String getFlightSegmentId() {
		return flightSegmentId;
	}
	public void setFlightSegmentId(String flightSegmentId) {
		this.flightSegmentId = flightSegmentId;
	}
	public Date getScheduledDepartureTime() {
		return scheduledDepartureTime;
	}
	public void setScheduledDepartureTime(Date scheduledDepartureTime) {
		this.scheduledDepartureTime = scheduledDepartureTime;
	}
	public Date getScheduledArrivalTime() {
		return scheduledArrivalTime;
	}
	public void setScheduledArrivalTime(Date scheduledArrivalTime) {
		this.scheduledArrivalTime = scheduledArrivalTime;
	}
	public BigDecimal getFirstClassBaseCost() {
		return firstClassBaseCost;
	}
	public void setFirstClassBaseCost(BigDecimal firstClassBaseCost) {
		this.firstClassBaseCost = firstClassBaseCost;
	}
	public BigDecimal getEconomyClassBaseCost() {
		return economyClassBaseCost;
	}
	public void setEconomyClassBaseCost(BigDecimal economyClassBaseCost) {
		this.economyClassBaseCost = economyClassBaseCost;
	}
	public int getNumFirstClassSeats() {
		return numFirstClassSeats;
	}
	public void setNumFirstClassSeats(int numFirstClassSeats) {
		this.numFirstClassSeats = numFirstClassSeats;
	}
	public int getNumEconomyClassSeats() {
		return numEconomyClassSeats;
	}
	public void setNumEconomyClassSeats(int numEconomyClassSeats) {
		this.numEconomyClassSeats = numEconomyClassSeats;
	}
	public String getAirplaneTypeId() {
		return airplaneTypeId;
	}
	public void setAirplaneTypeId(String airplaneTypeId) {
		this.airplaneTypeId = airplaneTypeId;
	}
	public FlightSegmentInfo getFlightSegment() {
		return flightSegment;
	}
	public void setFlightSegment(FlightSegmentInfo flightSegment) {
		this.flightSegment = flightSegment;
	}
	public FlightPKInfo getPkey(){
		return pkey;
	}
}

public class FlightPKInfo {

	private String id;
	private String flightSegmentId;
	
	FlightPKInfo(){}
	FlightPKInfo(String flightSegmentId,String id){
		this.id = id;
		this.flightSegmentId = flightSegmentId;
	}
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getFlightSegmentId() {
		return flightSegmentId;
	}
	public void setFlightSegmentId(String flightSegmentId) {
		this.flightSegmentId = flightSegmentId;
	}
}

import com.acmeair.entities.FlightSegment;
public class FlightSegmentInfo {

	private String _id;
	private String originPort;
	private String destPort;
	private int miles;
	
	public FlightSegmentInfo() {
		
	}
	public FlightSegmentInfo(FlightSegment flightSegment) {
		this._id = flightSegment.getFlightName();
		this.originPort = flightSegment.getOriginPort();
		this.destPort = flightSegment.getDestPort();
		this.miles = flightSegment.getMiles();
	}
	
	public String get_id() {
		return _id;
	}
	public void set_id(String _id) {
		this._id = _id;
	}
	public String getOriginPort() {
		return originPort;
	}
	public void setOriginPort(String originPort) {
		this.originPort = originPort;
	}
	public String getDestPort() {
		return destPort;
	}
	public void setDestPort(String destPort) {
		this.destPort = destPort;
	}
	public int getMiles() {
		return miles;
	}
	public void setMiles(int miles) {
		this.miles = miles;
	}	
}

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * TripFlightOptions is the main return type when searching for flights.
 * 
 * The object will return as many tripLeg's worth of Flight options as requested.  So if the user
 * requests a one way flight they will get a List that has only one TripLegInfo and it will have
 * a list of flights that are options for that flight.  If a user selects round trip, they will
 * have a List of two TripLegInfo objects.  If a user does a multi-leg flight then the list will
 * be whatever size they requested.  For now, only supporting one way and return flights so the
 * list should always be of size one or two.
 * 
 * 
 * @author aspyker
 *
 */
@XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
@XmlRootElement
public class TripFlightOptions {
	private int tripLegs;
	
	private List<TripLegInfo> tripFlights;

	public int getTripLegs() {
		return tripLegs;
	}

	public void setTripLegs(int tripLegs) {
		this.tripLegs = tripLegs;
	}

	public List<TripLegInfo> getTripFlights() {
		return tripFlights;
	}

	public void setTripFlights(List<TripLegInfo> tripFlights) {
		this.tripFlights = tripFlights;
	}
}

import java.util.ArrayList;
import java.util.List;

import com.acmeair.entities.Flight;

/**
 * The TripLegInfo object contains a list of flights that satisfy the query request for any one
 * leg of a trip.  Also, it supports paging so a query can't return too many requests.
 * @author aspyker
 *
 */
public class TripLegInfo {
	public static int DEFAULT_PAGE_SIZE = 10;
	
	private boolean hasMoreOptions;
	
	private int numPages;
	private int pageSize;
	private int currentPage;
	
	private List<FlightInfo> flightsOptions;

	public boolean isHasMoreOptions() {
		return hasMoreOptions;
	}

	public void setHasMoreOptions(boolean hasMoreOptions) {
		this.hasMoreOptions = hasMoreOptions;
	}

	public int getNumPages() {
		return numPages;
	}

	public void setNumPages(int numPages) {
		this.numPages = numPages;
	}

	public int getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	public int getCurrentPage() {
		return currentPage;
	}

	public void setCurrentPage(int currentPage) {
		this.currentPage = currentPage;
	}

	public List<FlightInfo> getFlightsOptions() {
		return flightsOptions;
	}

	public void setFlightsOptions(List<Flight> flightsOptions) {
		List<FlightInfo> flightInfoOptions = new ArrayList<FlightInfo>();
		for(Flight info : flightsOptions){
			flightInfoOptions.add(new FlightInfo(info));
		}
		this.flightsOptions = flightInfoOptions;
	}
	

}

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;
import javax.ws.rs.ApplicationPath;

@ApplicationPath("/rest/api")
public class AcmeAirApp extends Application {
    public Set<Class<?>> getClasses() {
        return new HashSet<Class<?>>(Arrays.asList(BookingsREST.class, CustomerREST.class, FlightsREST.class, LoginREST.class));
    }
}

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;
import javax.ws.rs.ApplicationPath;

import com.acmeair.config.AcmeAirConfiguration;
import com.acmeair.config.LoaderREST;

@ApplicationPath("/rest/info")
public class AppConfig extends Application {
    public Set<Class<?>> getClasses() {
        return new HashSet<Class<?>>(Arrays.asList(LoaderREST.class, AcmeAirConfiguration.class));
    }
}

import java.util.*;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;

import com.acmeair.entities.Booking;
import com.acmeair.service.BookingService;
import com.acmeair.service.ServiceLocator;
import com.acmeair.web.dto.BookingInfo;
import com.acmeair.web.dto.BookingReceiptInfo;

@Path("/bookings")
public class BookingsREST {
	
	private BookingService bs = ServiceLocator.instance().getService(BookingService.class);
	
	@POST
	@Consumes({"application/x-www-form-urlencoded"})
	@Path("/bookflights")
	@Produces("application/json")
	public /*BookingInfo*/ Response bookFlights(
			@FormParam("userid") String userid,
			@FormParam("toFlightId") String toFlightId,
			@FormParam("toFlightSegId") String toFlightSegId,
			@FormParam("retFlightId") String retFlightId,
			@FormParam("retFlightSegId") String retFlightSegId,
			@FormParam("oneWayFlight") boolean oneWay) {
		try {
			String bookingIdTo = bs.bookFlight(userid, toFlightSegId, toFlightId);
			String bookingIdReturn = null;
			if (!oneWay) {
				bookingIdReturn = bs.bookFlight(userid, retFlightSegId, retFlightId);
			}
			// YL. BookingInfo will only contains the booking generated keys as customer info is always available from the session
			BookingReceiptInfo bi;
			if (!oneWay)
				bi = new BookingReceiptInfo(bookingIdTo, bookingIdReturn, oneWay);
			else
				bi = new BookingReceiptInfo(bookingIdTo, null, oneWay);
			
			return Response.ok(bi).build();
		}
		catch (Exception e) {
			e.printStackTrace();
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	@GET
	@Path("/bybookingnumber/{userid}/{number}")
	@Produces("application/json")
	public BookingInfo getBookingByNumber(
			@PathParam("number") String number,
			@PathParam("userid") String userid) {
		try {
			Booking b = bs.getBooking(userid, number);
			BookingInfo bi = null;
			if(b != null){
				bi = new BookingInfo(b);
			}
			return bi;
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	@GET
	@Path("/byuser/{user}")
	@Produces("application/json")
	public List<BookingInfo> getBookingsByUser(@PathParam("user") String user) {
		try {
			List<Booking> list =  bs.getBookingsByUser(user);
			List<BookingInfo> newList = new ArrayList<BookingInfo>();
			for(Booking b : list){
				newList.add(new BookingInfo(b));
			}
			return newList;
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	@POST
	@Consumes({"application/x-www-form-urlencoded"})
	@Path("/cancelbooking")
	@Produces("application/json")
	public Response cancelBookingsByNumber(
			@FormParam("number") String number,
			@FormParam("userid") String userid) {
		try {
			bs.cancelBooking(userid, number);
			return Response.ok("booking " + number + " deleted.").build();
					
		}
		catch (Exception e) {
			e.printStackTrace();
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	

}

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import com.acmeair.entities.Customer;
import com.acmeair.entities.CustomerAddress;
import com.acmeair.service.*;
import com.acmeair.web.dto.*;

import javax.ws.rs.core.Context;

@Path("/customer")
public class CustomerREST {
	
	private CustomerService customerService = ServiceLocator.instance().getService(CustomerService.class);
	
	@Context 
	private HttpServletRequest request;


	private boolean validate(String customerid)	{
		String loginUser = (String) request.getAttribute(RESTCookieSessionFilter.LOGIN_USER);
		return customerid.equals(loginUser);
	}
	@GET
	@Path("/byid/{custid}")
	@Produces("application/json")
	public Response getCustomer(@CookieParam("sessionid") String sessionid, @PathParam("custid") String customerid) {
		try {
			// make sure the user isn't trying to update a customer other than the one currently logged in
			if (!validate(customerid)) {
				return Response.status(Response.Status.FORBIDDEN).build();
				
			}
			Customer customer = customerService.getCustomerByUsername(customerid);	
			CustomerInfo customerDTO = new CustomerInfo(customer);			
			return Response.ok(customerDTO).build();
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@POST
	@Path("/byid/{custid}")
	@Produces("application/json")
	public /* Customer */ Response putCustomer(@CookieParam("sessionid") String sessionid, CustomerInfo customer) {
		if (!validate(customer.getUsername())) {
			return Response.status(Response.Status.FORBIDDEN).build();
		}
		
		Customer customerFromDB = customerService.getCustomerByUsernameAndPassword(customer.getUsername(), customer.getPassword());
		if (customerFromDB == null) {
			// either the customer doesn't exist or the password is wrong
			return Response.status(Response.Status.FORBIDDEN).build();
		}
		
		CustomerAddress addressFromDB = customerFromDB.getAddress();
		addressFromDB.setStreetAddress1(customer.getAddress().getStreetAddress1());
		if (customer.getAddress().getStreetAddress2() != null) {
			addressFromDB.setStreetAddress2(customer.getAddress().getStreetAddress2());
		}
		addressFromDB.setCity(customer.getAddress().getCity());
		addressFromDB.setStateProvince(customer.getAddress().getStateProvince());
		addressFromDB.setCountry(customer.getAddress().getCountry());
		addressFromDB.setPostalCode(customer.getAddress().getPostalCode());
		
		customerFromDB.setPhoneNumber(customer.getPhoneNumber());
		customerFromDB.setPhoneNumberType(Customer.PhoneType.valueOf(customer.getPhoneNumberType()));
		
		customerService.updateCustomer(customerFromDB);
		customerFromDB.setPassword(null);
		
		return Response.ok(customerFromDB).build();
	}
	

	
}

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import com.acmeair.entities.Flight;
import com.acmeair.service.FlightService;
import com.acmeair.service.ServiceLocator;
import com.acmeair.web.dto.TripFlightOptions;
import com.acmeair.web.dto.TripLegInfo;

@Path("/flights")
public class FlightsREST {
	
	private FlightService flightService = ServiceLocator.instance().getService(FlightService.class);
	
	// TODO:  Consider a pure GET implementation of this service, but maybe not much value due to infrequent similar searches
	@POST
	@Path("/queryflights")
	@Consumes({"application/x-www-form-urlencoded"})
	@Produces("application/json")
	public TripFlightOptions getTripFlights(
			@FormParam("fromAirport") String fromAirport,
			@FormParam("toAirport") String toAirport,
			@FormParam("fromDate") Date fromDate,
			@FormParam("returnDate") Date returnDate,
			@FormParam("oneWay") boolean oneWay
			) {
		TripFlightOptions options = new TripFlightOptions();
		ArrayList<TripLegInfo> legs = new ArrayList<TripLegInfo>();
		
		TripLegInfo toInfo = new TripLegInfo();
		List<Flight> toFlights = flightService.getFlightByAirportsAndDepartureDate(fromAirport, toAirport, fromDate);
		toInfo.setFlightsOptions(toFlights);
		legs.add(toInfo);
		toInfo.setCurrentPage(0);
		toInfo.setHasMoreOptions(false);
		toInfo.setNumPages(1);
		toInfo.setPageSize(TripLegInfo.DEFAULT_PAGE_SIZE);
		
		if (!oneWay) {
			TripLegInfo retInfo = new TripLegInfo();
			List<Flight> retFlights = flightService.getFlightByAirportsAndDepartureDate(toAirport, fromAirport, returnDate);
			retInfo.setFlightsOptions(retFlights);
			legs.add(retInfo);
			retInfo.setCurrentPage(0);
			retInfo.setHasMoreOptions(false);
			retInfo.setNumPages(1);
			retInfo.setPageSize(TripLegInfo.DEFAULT_PAGE_SIZE);
			options.setTripLegs(2);
		}
		else {
			options.setTripLegs(1);
		}
		
		options.setTripFlights(legs);
		
		return options;
	}
	
	
	@POST
	@Path("/browseflights")
	@Consumes({"application/x-www-form-urlencoded"})
	@Produces("application/json")
	public TripFlightOptions browseFlights(
			@FormParam("fromAirport") String fromAirport,
			@FormParam("toAirport") String toAirport,
			@FormParam("oneWay") boolean oneWay
			) {
		TripFlightOptions options = new TripFlightOptions();
		ArrayList<TripLegInfo> legs = new ArrayList<TripLegInfo>();
		
		TripLegInfo toInfo = new TripLegInfo();
		List<Flight> toFlights = flightService.getFlightByAirports(fromAirport, toAirport);
		toInfo.setFlightsOptions(toFlights);
		legs.add(toInfo);
		toInfo.setCurrentPage(0);
		toInfo.setHasMoreOptions(false);
		toInfo.setNumPages(1);
		toInfo.setPageSize(TripLegInfo.DEFAULT_PAGE_SIZE);
		
		if (!oneWay) {
			TripLegInfo retInfo = new TripLegInfo();
			List<Flight> retFlights = flightService.getFlightByAirports(toAirport, fromAirport);
			retInfo.setFlightsOptions(retFlights);
			legs.add(retInfo);
			retInfo.setCurrentPage(0);
			retInfo.setHasMoreOptions(false);
			retInfo.setNumPages(1);
			retInfo.setPageSize(TripLegInfo.DEFAULT_PAGE_SIZE);
			options.setTripLegs(2);
		}
		else {
			options.setTripLegs(1);
		}
		
		options.setTripFlights(legs);
		
		return options;
	}	

}

import javax.ws.rs.*;
import javax.ws.rs.core.*;

import com.acmeair.entities.CustomerSession;
import com.acmeair.service.*;


@Path("/login")
public class LoginREST {
	
	public static String SESSIONID_COOKIE_NAME = "sessionid";
	
	private CustomerService customerService = ServiceLocator.instance().getService(CustomerService.class);
	
	
	@POST
	@Consumes({"application/x-www-form-urlencoded"})
	@Produces("text/plain")
	public Response login(@FormParam("login") String login, @FormParam("password") String password) {
		try {
			boolean validCustomer = customerService.validateCustomer(login, password);
			
			if (!validCustomer) {
				return Response.status(Response.Status.FORBIDDEN).build();
			}
			
			CustomerSession session = customerService.createSession(login);
			// TODO:  Need to fix the security issues here - they are pretty gross likely
			NewCookie sessCookie = new NewCookie(SESSIONID_COOKIE_NAME, session.getId());
			// TODO: The mobile client app requires JSON in the response. 
			// To support the mobile client app, choose one of the following designs:
			// - Change this method to return JSON, and change the web app javascript to handle a JSON response.
			//   example:  return Response.ok("{\"status\":\"logged-in\"}").cookie(sessCookie).build();
			// - Or create another method which is identical to this one, except returns JSON response.
			//   Have the web app use the original method, and the mobile client app use the new one.
			return Response.ok("logged in").cookie(sessCookie).build();
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	@GET
	@Path("/logout")
	@Produces("text/plain")
	public Response logout(@QueryParam("login") String login, @CookieParam("sessionid") String sessionid) {
		try {
			customerService.invalidateSession(sessionid);
			// The following call will trigger query against all partitions, disable for now
//			customerService.invalidateAllUserSessions(login);
			
			// TODO:  Want to do this with setMaxAge to zero, but to do that I need to have the same path/domain as cookie
			// created in login.  Unfortunately, until we have a elastic ip and domain name its hard to do that for "localhost".
			// doing this will set the cookie to the empty string, but the browser will still send the cookie to future requests
			// and the server will need to detect the value is invalid vs actually forcing the browser to time out the cookie and
			// not send it to begin with
			NewCookie sessCookie = new NewCookie(SESSIONID_COOKIE_NAME, "");
			return Response.ok("logged out").cookie(sessCookie).build();
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}

import java.io.IOException;

import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.acmeair.entities.CustomerSession;
import com.acmeair.service.CustomerService;
import com.acmeair.service.ServiceLocator;
import com.acmeair.service.TransactionService;

public class RESTCookieSessionFilter implements Filter {
	
	static final String LOGIN_USER = "acmeair.login_user";
	private static final String LOGIN_PATH = "/rest/api/login";
	private static final String LOGOUT_PATH = "/rest/api/login/logout";
	private static final String LOADDB_PATH = "/rest/api/loaddb";
	
	private CustomerService customerService = ServiceLocator.instance().getService(CustomerService.class);
	private TransactionService transactionService = ServiceLocator.instance().getService(TransactionService.class);; 

	@Inject
	BeanManager beanManager;
	
	@Override
	public void destroy() {
	}

	
	@Override
	public void doFilter(ServletRequest req, ServletResponse resp,	FilterChain chain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest)req;
		HttpServletResponse response = (HttpServletResponse)resp;
		
		String path = request.getContextPath() + request.getServletPath() + request.getPathInfo();
		// The following code is to ensure that OG is always set on the thread	
		try{			
			if (transactionService!=null)
				transactionService.prepareForTransaction();
		}catch( Exception e)
		{
			e.printStackTrace();
		}
	
		
		if (path.endsWith(LOGIN_PATH) || path.endsWith(LOGOUT_PATH) || path.endsWith(LOADDB_PATH)) {
			// if logging in, logging out, or loading the database, let the request flow
			chain.doFilter(req, resp);
			return;
		}
		
		Cookie cookies[] = request.getCookies();
		Cookie sessionCookie = null;
		if (cookies != null) {
			for (Cookie c : cookies) {
				if (c.getName().equals(LoginREST.SESSIONID_COOKIE_NAME)) {
					sessionCookie = c;
				}
				if (sessionCookie!=null)
					break; 
			}
			String sessionId = "";
			if (sessionCookie!=null) // We need both cookie to work
				sessionId= sessionCookie.getValue().trim();
			// did this check as the logout currently sets the cookie value to "" instead of aging it out
			// see comment in LogingREST.java
			if (sessionId.equals("")) {
				response.sendError(HttpServletResponse.SC_FORBIDDEN);
				return;
			}
			// Need the URLDecoder so that I can get @ not %40
			CustomerSession cs = customerService.validateSession(sessionId);
			if (cs != null) {
				request.setAttribute(LOGIN_USER, cs.getCustomerid());
				chain.doFilter(req, resp);
				return;
			}
			else {
				response.sendError(HttpServletResponse.SC_FORBIDDEN);
				return;
			}
		}
		
		// if we got here, we didn't detect the session cookie, so we need to return 404
		response.sendError(HttpServletResponse.SC_FORBIDDEN);
	}

	@Override
	public void init(FilterConfig config) throws ServletException {
	}
}

import java.io.File;
import java.io.IOException;

import java.io.BufferedReader;

import java.io.FileReader;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;

import com.acmeair.reporter.util.Messages;


public class JmeterJTLParser {
    
	private String jmeterJTLFileName = "AcmeAir[1-9].jtl";
	
    private String regEx =
        "<httpSample\\s*" + 
          "t=\"([^\"]*)\"\\s*"  +  
          "lt=\"([^\"]*)\"\\s*" +  
          "ts=\"([^\"]*)\"\\s*" +  
          "s=\"([^\"]*)\"\\s*"  +  
          "lb=\"([^\"]*)\"\\s*" +  
          "rc=\"([^\"]*)\"\\s*" +  
          "rm=\"([^\"]*)\"\\s*" +  
          "tn=\"([^\"]*)\"\\s*" +  
          "dt=\"([^\"]*)\"\\s*" +  
          "by=\"([^\"]*)\"\\s*" + 
          "FLIGHTTOCOUNT=\"([^\"]*)\"\\s*" +
          "FLIGHTRETCOUNT=\"([^\"]*)\"\\s*"+
          "ONEWAY\\s*=\"([^\"]*)\"\\s*";
    // NOTE: The regular expression depends on user.properties in jmeter having the sample_variables property added.
    //       sample_variables=FLIGHTTOCOUNT,FLIGHTRETCOUNT,ONEWAY
    

    private int GROUP_T  = 1;
    private int GROUP_TS = 3;
    private int GROUP_S  = 4;
    private int GROUP_LB = 5;
    private int GROUP_RC = 6;
    private int GROUP_TN = 8;
    private int GROUP_FLIGHTTOCOUNT = 11;
    private int GROUP_FLIGHTRETCOUNT = 12;
    private int GROUP_ONEWAY = 13;
        
    
    private  JtlTotals totalAll;
    private Map<String, JtlTotals> totalUrlMap;

    public JmeterJTLParser() {
    	totalAll = new JtlTotals();
    	totalUrlMap = new HashMap<String, JtlTotals>(); 
    	
       	String jtlRegularExpression = Messages.getString("parsers.JmeterJTLParser.jtlRegularExpression");
    	if (jtlRegularExpression != null){
    		System.out.println("set regex string to be '" + jtlRegularExpression+ "'");
    		regEx = jtlRegularExpression;
    	}
    	
      	String matcherGroup = Messages.getString("parsers.JmeterJTLParser.regexGroups.t");
    	if (matcherGroup != null){
    		GROUP_T = new Integer(matcherGroup).intValue();
    	}
    	
      	matcherGroup = Messages.getString("parsers.JmeterJTLParser.regexGroups.ts");
    	if (matcherGroup != null){
    		GROUP_TS = new Integer(matcherGroup).intValue();
    	}
    	
      	matcherGroup = Messages.getString("parsers.JmeterJTLParser.regexGroups.s");
    	if (matcherGroup != null){
    		GROUP_S = new Integer(matcherGroup).intValue();
    	}   
    	
      	matcherGroup = Messages.getString("parsers.JmeterJTLParser.regexGroups.lb");
    	if (matcherGroup != null){
    		GROUP_LB = new Integer(matcherGroup).intValue();
    	}    	
    	
      	matcherGroup = Messages.getString("parsers.JmeterJTLParser.regexGroups.rc");
    	if (matcherGroup != null){
    		GROUP_RC = new Integer(matcherGroup).intValue();
    	}
    	
      	matcherGroup = Messages.getString("parsers.JmeterJTLParser.regexGroups.tn");
    	if (matcherGroup != null){
    		GROUP_TN = new Integer(matcherGroup).intValue();
    	}    
    	
      	matcherGroup = Messages.getString("parsers.JmeterJTLParser.regexGroups.FLIGHTTOCOUNT");
    	if (matcherGroup != null){
    		GROUP_FLIGHTTOCOUNT = new Integer(matcherGroup).intValue();
    	}
    	
     	matcherGroup = Messages.getString("parsers.JmeterJTLParser.regexGroups.FLIGHTRETCOUNT");
    	if (matcherGroup != null){
    		GROUP_FLIGHTRETCOUNT = new Integer(matcherGroup).intValue();
    	}    
    	
     	matcherGroup = Messages.getString("parsers.JmeterJTLParser.regexGroups.ONEWAY");
    	if (matcherGroup != null){
    		GROUP_ONEWAY = new Integer(matcherGroup).intValue();
    	}   
    	
      	String responseTimeStepping = Messages.getString("parsers.JmeterJTLParser.responseTimeStepping");
    	if (responseTimeStepping != null){
    		JtlTotals.setResponseTimeStepping(new Integer(responseTimeStepping).intValue());
    	}    	
    }

    
    public void setLogFileName (String logFileName) {
    	this.jmeterJTLFileName = logFileName;
    }
    
    
    public void processResultsDirectory(String dirName) {
    	File root = new File(dirName);
    	try {
    		Collection<File> files = FileUtils.listFiles(root,
    				new RegexFileFilter(jmeterJTLFileName),
    				DirectoryFileFilter.DIRECTORY);

    		for (Iterator<File> iterator = files.iterator(); iterator.hasNext();) {
    			File file = (File) iterator.next();
    			parse(file);
    		}
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    }

    
    public void parse(File jmeterJTLfile) throws IOException {
    	if(totalAll == null){
    		totalAll = new JtlTotals();
    		totalUrlMap = new HashMap<String, JtlTotals>(); 
    	}
    	totalAll.incrementFiles();
        Pattern pattern = Pattern.compile(regEx);
        HashMap <String, Integer> threadCounter = new HashMap<String, Integer>();
        
        BufferedReader reader = new BufferedReader(new FileReader(jmeterJTLfile));
        try {
            String line = reader.readLine();
            while(line != null) {
            	
                Matcher matcher = pattern.matcher(line);
                if(matcher.find()) {
                    add(matcher, totalAll);
                    
                    String url = matcher.group(GROUP_LB);
                    JtlTotals urlTotals = totalUrlMap.get(url);
                    if(urlTotals == null) {
                        urlTotals = new JtlTotals();                        
                        totalUrlMap.put(url, urlTotals);
                    }
                    add(matcher, urlTotals);
                    String threadName = matcher.group(GROUP_TN);
                    Integer threadCnt = threadCounter.get(threadName);
                    if(threadCnt == null) {
                    	threadCnt = new Integer(1);
                    }else{
                    	threadCnt = Integer.valueOf(threadCnt.intValue()+1);
                    }
                    threadCounter.put(threadName, threadCnt);
                }                
                line = reader.readLine();
            }
            
        } finally {
        	reader.close();
        }
        totalAll.setThreadMap(threadCounter);
        if(totalAll.getCount() == 0) {
            System.out.println("JmeterJTLParser - No results found!");
            return;
        }
    } 
    
    public JtlTotals getResults() {
    	return totalAll;
    }

    public Map<String, JtlTotals> getResultsByUrl() {
    	return totalUrlMap;
    }
    
    private void add(Matcher matcher, JtlTotals total) {
        
        long timestamp = Long.parseLong(matcher.group(GROUP_TS));
        total.addTimestamp(timestamp);
        
        int time = Integer.parseInt(matcher.group(GROUP_T));
        total.addTime(time);
                
        String rc = matcher.group(GROUP_RC);
        total.addReturnCode(rc);
              
        if(!matcher.group(GROUP_S).equalsIgnoreCase("true")) {
        	total.incrementFailures();
        }

        String strFlightCount = matcher.group(GROUP_FLIGHTTOCOUNT);
        if (strFlightCount != null && !strFlightCount.isEmpty()){  
        	int count = Integer.parseInt(strFlightCount);
        	total.addToFlight(count);        	
        }        

        strFlightCount = matcher.group(GROUP_FLIGHTRETCOUNT);
        if (strFlightCount != null && !strFlightCount.isEmpty()){
        	total.addFlightRetCount(Integer.parseInt(strFlightCount));
        }
        
        String oneWay = matcher.group(GROUP_ONEWAY);
        if (oneWay != null && oneWay.equalsIgnoreCase("true")){        	
        	total.incrementOneWayCount();
        }        
    } 
} 

import java.io.File;

import com.acmeair.reporter.parser.IndividualChartResults;
import com.acmeair.reporter.parser.ResultParser;
import com.acmeair.reporter.parser.ResultParserHelper;

public class JmeterSummariserParser extends ResultParser {

	private static boolean SKIP_JMETER_DROPOUTS = false; 
	static {
		SKIP_JMETER_DROPOUTS = System.getProperty("SKIP_JMETER_DROPOUTS") != null;
	}
	
	private String jmeterFileName = "AcmeAir[1-9].log";	
	private String testDate = "";
	
	@Override
	protected void processFile(File file) {
		IndividualChartResults result= getData(file.getPath());		
		super.processData(ResultParserHelper.scaleDown(result.getInputList(),8),false);
		IndividualChartResults individualResults = new IndividualChartResults();
		if(result.getTitle() != null){
			individualResults.setTitle(result.getTitle());
		} else {
			individualResults.setTitle(file.getName());
		}
		individualResults.setInputList(ResultParserHelper.scaleDown(result.getInputList(),6));
		individualResults.setTimeList(ResultParserHelper.scaleDown(result.getTimeList(),3));
		super.getMultipleChartResults().getResults().add(individualResults);
	}

	@Override
	public String getFileName() {
		return jmeterFileName;
	}

	@Override
	public void setFileName(String fileName) {
		jmeterFileName = fileName;
	}

	public String getTestDate(){
		return testDate;
	}

	@Override
	protected void processLine(IndividualChartResults results, String strLine) {		
		if (strLine.indexOf("summary +") > 0) {			
			String[] tokens = strLine.split(" ");
			results.getTimeList().add(tokens[1].trim());
			testDate = tokens[0].trim();		
			int endposition = strLine.indexOf("/s");
			int startposition = strLine.indexOf("=");
			String thoughputS = strLine.substring(startposition + 1, endposition).trim();
			Double throughput = Double.parseDouble(thoughputS);
			if (throughput == 0.0 && SKIP_JMETER_DROPOUTS) {
				return;
			}
			results.getInputList().add(throughput);
		} else if (strLine.indexOf("Name:") > 0) {
			int startIndex = strLine.indexOf(" Name:")+7;
			int endIndex = strLine.indexOf(" ", startIndex);
			String name = strLine.substring(startIndex, endIndex);
			results.setTitle(name);
		}
	}
}

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

public class JtlTotals {
    private static final String DECIMAL_PATTERN = "#,##0.0##";
    private static final double MILLIS_PER_SECOND = 1000.0;
    private static int millisPerBucket = 500;
    private int files = 0;
    private int request_count = 0;
    private int time_sum = 0;
    private int time_max = 0; 
    private int time_min = Integer.MAX_VALUE; 

    private int failures = 0;
    private long timestamp_start = Long.MAX_VALUE; 
    private long timestamp_end = 0;  
    private Map<String, Integer> rcMap = new HashMap<String, Integer>(); // key rc, value count
    private Map<Integer, Integer> millisMap = new TreeMap<Integer, Integer>(); // key bucket Integer, value count
    private Map <String, Integer> threadMap = new HashMap<String,Integer>(); 
    private ArrayList<Integer> timeList = new ArrayList<Integer>();
    private long flight_to_sum = 0;
    private long flight_to_count = 0;
	private int flight_to_empty_count = 0;    
    private long flight_ret_count = 0;
    private long one_way_count = 0;
    

    
    public JtlTotals() {
    }
    
    
    public void add(JtlTotals totals){
      rcMap.putAll(totals.getReturnCodeCounts());
      millisMap.putAll(totals.getMillisMap());
      threadMap.putAll(totals.getThreadMap());
      one_way_count += totals.getOneWayCount();
      flight_ret_count += totals.getFlightRetCount();
      flight_to_empty_count += totals.getEmptyToFlightCount();
      flight_to_sum += totals.getFlightToSum();
      flight_to_count += totals.getFlightToCount();
      failures += totals.getFailures();
      request_count += totals.getCount();  
    }
    
    public long getFlightToCount() {
		return flight_to_count;
	}
    
    public void addTime(int time){
    	request_count++;
    	time_sum+=time;
        time_max = Math.max(time_max, time);
        time_min = Math.min(time_min, time);
        timeList.add(time);
        Integer bucket = new Integer(time / millisPerBucket);
        Integer count = millisMap.get(bucket);
        if(count == null) {
            count = new Integer(0);
        }
        millisMap.put(bucket, new Integer(count.intValue() + 1));
    }
    
    public Map<Integer, Integer> getMillisMap() {
		return millisMap;
	}


	public void addReturnCode(String rc){
        Integer rc_count = rcMap.get(rc);
        if(rc_count == null) {
            rc_count = new Integer(0);
        }
        rcMap.put(rc, new Integer(rc_count.intValue() + 1));    
    }
    
    public void setThreadMap(Map<String,Integer> threadMap){
    	this.threadMap = threadMap;
    }
    
    public void addTimestamp(long timestamp){
    	timestamp_end = Math.max(timestamp_end, timestamp);
        timestamp_start = Math.min(timestamp_start, timestamp);
    }
    
    public void incrementFailures(){
    	failures++;
    }
    
    public void addToFlight(int count){
    	this.flight_to_count++;
    	this.flight_to_sum += count;
    	if(count == 0)
    		this.flight_to_empty_count++;	
    }
      
    public void addFlightRetCount(int count){
    	this.flight_ret_count += count;
    }
    
    public void incrementOneWayCount(){
    	one_way_count++;
    }
    
    public void incrementFiles(){
    	files++;
    }
    
    public int getFilesCount(){
    	return files;
    }
    
    public int getCount(){
    	return request_count;
    }
    
    public Map<String,Integer> getThreadMap(){
    	return this.threadMap;
    }
    
    public int getAverageResponseTime(){
    	//in case .jtl file doesn't exist, request_count could be 0
    	//adding this condition to avoid "divide by zero" runtime exception
    	if (request_count==0) {
    		return time_sum;
    	}
    	return  (time_sum/request_count);
    }

    public int getMaxResponseTime(){
    	return time_max;
    }

    public int getMinResponseTime(){
    	return time_min;
    }
    public int getFailures(){
    	return failures;
    }
    public int get90thPrecentile(){
    	if(timeList.isEmpty()){
    		return  Integer.MAX_VALUE; 
    	}
    	int target = (int)Math.round(timeList.size() * .90 );
    	Collections.sort(timeList); 
    	if(target == timeList.size()){target--;}    	
    	return timeList.get(target);
    }    

    public Map<String, Integer> getReturnCodeCounts(){
    	return rcMap;
    }

    public long getElapsedTimeInSeconds(){
        double secondsElaspsed = (timestamp_end - timestamp_start) / MILLIS_PER_SECOND;
        return Math.round(secondsElaspsed);
    }
    
    public long getRequestsPerSecond (){      
        return  Math.round(request_count / getElapsedTimeInSeconds());
    }
    
    public long getFlightToSum(){
    	return flight_to_sum;
    }

    public long getEmptyToFlightCount(){
    	return flight_to_empty_count;
    }    

    public float getAverageToFlights(){
    	return (float)flight_to_sum/flight_to_count;
    }
    
    public long getFlightRetCount(){
    	return flight_ret_count;
    }
    
    public long getOneWayCount(){
    	return one_way_count;
    }
    
    public static void setResponseTimeStepping(int milliseconds){
    	millisPerBucket = milliseconds;
    }
    
    public static int getResponseTimeStepping(){
    	return millisPerBucket;
    }
    
    public String cntByTimeString() {
        DecimalFormat df = new DecimalFormat(DECIMAL_PATTERN);
        List<String> millisStr = new LinkedList<String>();
        
        Iterator <Entry<Integer,Integer>>iter = millisMap.entrySet().iterator();
        while(iter.hasNext()) {
            Entry<Integer,Integer> millisEntry = iter.next();
            Integer bucket = (Integer)millisEntry.getKey();
            Integer bucketCount = (Integer)millisEntry.getValue();
            
            int minMillis = bucket.intValue() * millisPerBucket;
            int maxMillis = (bucket.intValue() + 1) * millisPerBucket;
            
            millisStr.add(
              df.format(minMillis/MILLIS_PER_SECOND)+" s "+
              "- "+
              df.format(maxMillis/MILLIS_PER_SECOND)+" s "+
              "= " + bucketCount);
        }
        return millisStr.toString();
    }
    
    public HashMap<String, Integer> cntByTime() {
        DecimalFormat df = new DecimalFormat(DECIMAL_PATTERN);     
        LinkedHashMap<String, Integer> millisStr = new LinkedHashMap<String, Integer>(); 
        Iterator <Entry<Integer,Integer>>iter = millisMap.entrySet().iterator();
        while(iter.hasNext()) {
            Entry<Integer,Integer> millisEntry = iter.next();
            Integer bucket = (Integer)millisEntry.getKey();
            Integer bucketCount = (Integer)millisEntry.getValue();
            
            int minMillis = bucket.intValue() * millisPerBucket;
            int maxMillis = (bucket.intValue() + 1) * millisPerBucket;
            
            millisStr.put(
              df.format(minMillis/MILLIS_PER_SECOND)+" s "+
              "- "+
              df.format(maxMillis/MILLIS_PER_SECOND)+" s "
              , bucketCount);
        }
        return millisStr;
    }
}

import java.io.File;

import com.acmeair.reporter.parser.IndividualChartResults;
import com.acmeair.reporter.parser.ResultParser;
import com.acmeair.reporter.parser.ResultParserHelper;


public class NmonParser extends ResultParser{

	private String nmonFileName = "output.nmon";
	
	public NmonParser(){
		super.setMultipleYAxisLabel("usr%+sys%"); //default label
	}
	
	@Override
	protected void processFile(File file) {
		IndividualChartResults result= getData(file.getPath());
		super.processData(ResultParserHelper.scaleDown(result.getInputList(),8),false);
		IndividualChartResults individualResults = new IndividualChartResults();

		individualResults.setTitle(result.getTitle());
		individualResults.setInputList(ResultParserHelper.scaleDown(result.getInputList(),6));
		individualResults.setTimeList(ResultParserHelper.scaleDown(result.getTimeList(),3));
		super.getMultipleChartResults().getResults().add(individualResults);
	}

	
	@Override
	public String getFileName() {
		return nmonFileName;
	}
	
	@Override
	public void setFileName(String fileName) {
		nmonFileName = fileName;
	}
	
	@Override
	protected void processLine(IndividualChartResults results, String strLine) {
		if(strLine.startsWith("AAA,host,")){
			String[] tokens = strLine.split(",");
			 results.setTitle(tokens[2].trim());			
		}
		
		if (strLine.indexOf("ZZZZ") >=0){
			String[] tokens = strLine.split(",");
			 results.getTimeList().add(tokens[2].trim());
		}
		
		if (strLine.indexOf("CPU_ALL") >=0 && strLine.indexOf("CPU Total")<0) {
			String[] tokens = strLine.split(",");
			String user = tokens[2].trim();
			String sys = tokens[3].trim();
			Double userDouble = Double.parseDouble(user);
			Double sysDouble = Double.parseDouble(sys);
			 results.getInputList().add(userDouble+sysDouble);		
		}
	}
}

import java.util.ArrayList;

public class IndividualChartResults {
	private ArrayList<Double> inputList = new ArrayList<Double>();
	private String title;
	private ArrayList<String> timeList = new ArrayList<String>();
	private int files = 0;
	
	public void setTitle(String title) {
		this.title = title;
	}
	public ArrayList<Double> getInputList() {
		return inputList;
	}
	public void setInputList(ArrayList<Double> inputList) {
		this.inputList = inputList;
	}
	public ArrayList<String> getTimeList() {
		return timeList;
	}
	public void setTimeList(ArrayList<String> timeList) {
		this.timeList = timeList;
	}
	
	public String getTitle() {
		return title;
	}
	
    public void incrementFiles(){
    	files++;
    }
    
    public int getFilesCount(){
    	return files;
    }
	
}

import java.util.ArrayList;

public class MultipleChartResults {
	
	private String multipleChartTitle;
	private String multipleChartYAxisLabel;
	private ArrayList<IndividualChartResults> results = new  ArrayList<IndividualChartResults> ();
	private ArrayList<String> charStrings= new ArrayList<String>();
	
	public String getMultipleChartTitle() {
		return multipleChartTitle;
	}
	public void setMultipleChartTitle(String multipleChartTitle) {
		this.multipleChartTitle = multipleChartTitle;
	}
	public String getMultipleChartYAxisLabel() {
		return multipleChartYAxisLabel;
	}
	public void setMultipleChartYAxisLabel(String multipleChartYAxisLabel) {
		this.multipleChartYAxisLabel = multipleChartYAxisLabel;
	}

	public ArrayList<IndividualChartResults> getResults() {
		return results;
	}
	public void setResults(ArrayList<IndividualChartResults> results) {
		this.results = results;
	}
		
	public ArrayList<String> getCharStrings() {
		return charStrings;
	}
	public void setCharStrings(ArrayList<String> charStrings) {
		this.charStrings = charStrings;
	}
}

import java.util.ArrayList;

public class OverallResults {
	private ArrayList<Double> allInputList = new ArrayList<Double>();
	private ArrayList<String> allTimeList = new ArrayList<String>();
	private double scale_max;
	private double overallScale_max;

	public ArrayList<Double> getAllInputList() {
		return allInputList;
	}

	public void setAllInputList(ArrayList<Double> allInputList) {
		this.allInputList = new ArrayList<Double> (allInputList);
	}

	public ArrayList<String> getAllTimeList() {
		return allTimeList;
	}

	public void setAllTimeList(ArrayList<String> allTimeList) {
		this.allTimeList = new ArrayList<String> (allTimeList);
	}

	public double getOverallScale_max() {
		return overallScale_max;
	}

	public void setOverallScale_max(double overallScale_max) {
		this.overallScale_max = overallScale_max;
	}

	public double getScale_max() {
		return scale_max;
	}

	public void setScale_max(double scale_max) {
		this.scale_max = scale_max;
	}
}

import static com.googlecode.charts4j.Color.ALICEBLUE;
import static com.googlecode.charts4j.Color.BLACK;
import static com.googlecode.charts4j.Color.LAVENDER;
import static com.googlecode.charts4j.Color.WHITE;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;

import com.acmeair.reporter.parser.component.NmonParser;
import com.googlecode.charts4j.AxisLabels;
import com.googlecode.charts4j.AxisLabelsFactory;
import com.googlecode.charts4j.AxisStyle;
import com.googlecode.charts4j.AxisTextAlignment;
import com.googlecode.charts4j.Color;
import com.googlecode.charts4j.Data;
import com.googlecode.charts4j.DataEncoding;
import com.googlecode.charts4j.Fills;
import com.googlecode.charts4j.GCharts;
import com.googlecode.charts4j.Line;
import com.googlecode.charts4j.LineChart;
import com.googlecode.charts4j.LineStyle;
import com.googlecode.charts4j.LinearGradientFill;
import com.googlecode.charts4j.Plots;
import com.googlecode.charts4j.Shape;

public abstract class ResultParser {

	protected MultipleChartResults multipleChartResults = new MultipleChartResults();
	protected OverallResults overallResults = new OverallResults();
	
	public MultipleChartResults getMultipleChartResults() {
		return multipleChartResults;
	}

	protected void addUp(ArrayList<Double> list) {
		//if empty, don't need to add up
		if (overallResults.getAllInputList().isEmpty()) {
			overallResults.setAllInputList(list);
			return;
		}
		int size = overallResults.getAllInputList().size();
		if (size > list.size()) {
			size = list.size();
		}
		for (int i = 0; i < size; i++) {
			overallResults.getAllInputList().set(i, overallResults.getAllInputList().get(i) + list.get(i));
		}

	}

	public void processDirectory(String dirName) {
		File root = new File(dirName);
		try {
			Collection<File> files = FileUtils.listFiles(root,
					new RegexFileFilter(getFileName()),
					DirectoryFileFilter.DIRECTORY);

			for (Iterator<File> iterator = files.iterator(); iterator.hasNext();) {
				File file = (File) iterator.next();
				processFile(file);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String generateChartStrings(String titileLabel, String ylabel,
			String xlable, double[] inputs, ArrayList<String> timeList, boolean addToList) {
		if (inputs == null || inputs.length == 0)
			return null;
		Line line1 = Plots.newLine(Data.newData(inputs),
				Color.newColor("CA3D05"), "");
		line1.setLineStyle(LineStyle.newLineStyle(2, 1, 0));
		line1.addShapeMarkers(Shape.DIAMOND, Color.newColor("CA3D05"), 6);		
		LineChart chart = GCharts.newLineChart(line1);
		// Defining axis info and styles
		chart.addYAxisLabels(AxisLabelsFactory.newNumericRangeAxisLabels(0,
				overallResults.getScale_max() / 0.9));		
		if (timeList != null && timeList.size() > 0) {
			chart.addXAxisLabels(AxisLabelsFactory.newAxisLabels(timeList));
		}

		String url = generateDefaultChartSettings(titileLabel, ylabel, xlable,
				chart, addToList);		
		return url;
	}

	public String generateDefaultChartSettings(String titileLabel,
			String ylabel, String xlable, LineChart chart, boolean addToList) {
		AxisStyle axisStyle = AxisStyle.newAxisStyle(BLACK, 13,
				AxisTextAlignment.CENTER);
		AxisLabels yAxisLabel = AxisLabelsFactory.newAxisLabels(ylabel, 50.0);
		yAxisLabel.setAxisStyle(axisStyle);
		AxisLabels time = AxisLabelsFactory.newAxisLabels(xlable, 50.0);
		time.setAxisStyle(axisStyle);

		chart.addYAxisLabels(yAxisLabel);

		chart.addXAxisLabels(time);

		chart.setDataEncoding(DataEncoding.SIMPLE);

		chart.setSize(1000, 300);

		chart.setTitle(titileLabel, BLACK, 16);
		chart.setGrid(100, 10, 3, 2);
		chart.setBackgroundFill(Fills.newSolidFill(ALICEBLUE));
		LinearGradientFill fill = Fills.newLinearGradientFill(0, LAVENDER, 100);
		fill.addColorAndOffset(WHITE, 0);
		chart.setAreaFill(fill);
		String url = chart.toURLString();
		if(addToList) {
			getCharStrings().add(url);
		}
		return url;
	}

	public String generateMultipleLinesCharString(String titileLabel,
			String ylabel, String xlabel, List<IndividualChartResults> list) {

		if (list ==null || list.size()==0) {
			return null;
		}
		Line[] lines = new Line[list.size()];
		for (int i = 0; i < list.size(); i++) {
			double[] multiLineData = processMultiLineData(list.get(i).getInputList());
			if (multiLineData!=null) {
				lines[i] = Plots.newLine(Data.newData(multiLineData), ResultParserHelper.getColor(i), list.get(i).getTitle());
				lines[i].setLineStyle(LineStyle.newLineStyle(2, 1, 0));		
			} else {
				System.out.println("found jmeter log file that doesn't have data:\" " + list.get(i).getTitle() +"\" skipping!");
				return null;
			}
		}

		LineChart chart = GCharts.newLineChart(lines);
		chart.addYAxisLabels(AxisLabelsFactory.newNumericRangeAxisLabels(0,
				overallResults.getOverallScale_max() / 0.9));
		
		chart.addXAxisLabels(AxisLabelsFactory.newAxisLabels(list.get(0)
				.getTimeList()));
		// Defining axis info and styles
		String url = generateDefaultChartSettings(titileLabel, ylabel, xlabel,
				chart, true);		
		return url;
	}

	public ArrayList<Double> getAllInputList() {
		return overallResults.getAllInputList();
	}
	public ArrayList<String> getAllTimeList() {
		return overallResults.getAllTimeList();
	}
	public ArrayList<String> getCharStrings() {
		return getMultipleChartResults().getCharStrings();
	}

	protected <E> IndividualChartResults getData(String fileName) {
		IndividualChartResults results = new IndividualChartResults();
		try {
			FileInputStream fstream = new FileInputStream(fileName);
			// Get the object of DataInputStream
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;

			while ((strLine = br.readLine()) != null) {
				processLine(results, strLine);
			}
			in.close();
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
		}		

		addUp(results.getInputList());
		overallResults.setAllTimeList(results.getTimeList());
		return results;
	}

	public abstract String getFileName();
	
	public abstract void setFileName(String fileName);

	public ArrayList<IndividualChartResults> getResults() {
		return getMultipleChartResults().getResults();
	}


	public double[] processData(ArrayList<Double> inputList, boolean isTotalThroughput) {
		if (inputList != null && inputList.size() > 0) {
			if (this instanceof NmonParser) {
					overallResults.setScale_max(90.0);
			} else {
				overallResults.setScale_max(Collections.max(inputList));
			}
			if (overallResults.getOverallScale_max() < overallResults.getScale_max() && !isTotalThroughput) {
				overallResults.setOverallScale_max( overallResults.getScale_max());
			}			
			double scale_factor = 90 / overallResults.getScale_max();
			return ResultParserHelper.scaleInputsData(inputList, scale_factor);
		}
		return null;
	}

	protected abstract void processFile(File file);

	protected abstract void processLine(IndividualChartResults result, String strLine);

	public double[] processMultiLineData(ArrayList<Double> inputList) {
		if (inputList != null && inputList.size() > 0) {			
			double scale_factor = 90 / overallResults.getOverallScale_max();
			return ResultParserHelper.scaleInputsData(inputList, scale_factor);
		}
		return null;
	}

	public String getMultipleChartTitle() {		
		return multipleChartResults.getMultipleChartTitle();
	}

	public void setMultipleYAxisLabel(String label){
		multipleChartResults.setMultipleChartYAxisLabel(label);
	}
	
	public void setMultipleChartTitle(String label){
		multipleChartResults.setMultipleChartTitle(label);
	}
	public String getMultipleYAxisLabel() {
		return multipleChartResults.getMultipleChartYAxisLabel();
	}
	
}

import java.util.ArrayList;

import com.googlecode.charts4j.Color;

public class ResultParserHelper {

	public static Color getColor(int i) {
		Color[] colors = { Color.RED, Color.BLACK, Color.BLUE, Color.YELLOW,
				Color.GREEN, Color.ORANGE, Color.PINK, Color.SILVER,
				Color.GOLD, Color.WHITE, Color.BROWN, Color.CYAN,Color.GRAY,Color.HONEYDEW,Color.IVORY };
		return colors[i % 15];
	}

	public static <E> ArrayList<E> scaleDown(ArrayList<E> testList, int scaleDownFactor) {
		
		if (testList==null) {
			return null;
		}
		if (testList.size() <= 7)
			return testList;
		if (scaleDownFactor > 10 || scaleDownFactor < 0) {
			throw new RuntimeException(
					"currently only support factor from 0-10");
		}
		int listLastItemIndex = testList.size() - 1;
		int a = (int) java.lang.Math.pow(2, scaleDownFactor);
		if (a > listLastItemIndex) {
			return testList;
		}
		ArrayList<E> newList = new ArrayList<E>();
		newList.add(testList.get(0));
	
		if (scaleDownFactor == 0) {
			newList.add(testList.get(listLastItemIndex));
	
		} else {
	
			for (int m = 1; m <= a; m++) {
				newList.add(testList.get(listLastItemIndex * m / a));
			}
		}
		return newList;
	}

	public static double[] scaleInputsData(ArrayList<Double> inputList,
			double scale_factor) {
		double[] inputs = new double[inputList.size()];
		for (int i = 0; i <= inputList.size() - 1; i++) {
			inputs[i] = inputList.get(i) * scale_factor;
		}
		return inputs;
	}
}

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.apache.velocity.tools.generic.ComparisonDateTool;
import org.apache.velocity.tools.generic.MathTool;
import org.apache.velocity.tools.generic.NumberTool;

import com.acmeair.reporter.util.Messages;
import com.acmeair.reporter.util.StatResult;
import com.acmeair.reporter.parser.IndividualChartResults;
import com.acmeair.reporter.parser.ResultParser;
import com.acmeair.reporter.parser.ResultParserHelper;
import com.acmeair.reporter.parser.component.JmeterJTLParser;
import com.acmeair.reporter.parser.component.JmeterSummariserParser;
import com.acmeair.reporter.parser.component.JtlTotals;
import com.acmeair.reporter.parser.component.NmonParser;

//import freemarker.cache.ClassTemplateLoader;
//import freemarker.template.Configuration;
//import freemarker.template.Template;

public class ReportGenerator {
	private static final int max_lines = 15;
			 
	private static final String RESULTS_FILE = Messages.getString("ReportGenerator.RESULT_FILE_NAME");    	
	private static String searchingLocation = Messages.getString("inputDirectory"); 
	private static String jmeterFileName = Messages.getString("ReportGenerator.DEFAULT_JMETER_FILENAME"); 
	private static String nmonFileName = Messages.getString("ReportGenerator.DEFAULT_NMON_FILE_NAME"); 
	
	private static final String BOOK_FLIGHT = "BookFlight";
	private static final String CANCEL_BOOKING = "Cancel Booking";
	private static final String LOGIN = "Login";
	private static final String LOGOUT = "logout";	
	private static final String LIST_BOOKINGS = "List Bookings";
	private static final String QUERY_FLIGHT = "QueryFlight";
	private static final String UPDATE_CUSTOMER = "Update Customer";
	private static final String VIEW_PROFILE = "View Profile Information";
	
	
	private LinkedHashMap<String,ArrayList<String>> charMap = new LinkedHashMap<String,ArrayList<String>>();
	
	public static void main(String[] args) {
		if (args.length == 1) {
			searchingLocation = (args[0]);
		}
		if (!new File(searchingLocation).isDirectory()) {
			System.out.println("\"" + searchingLocation + "\" is not a valid directory");
			return;
		}
		System.out.println("Parsing acme air test results in the location \"" + searchingLocation + "\""); 
		
		ReportGenerator generator = new ReportGenerator();
		long start, stop;
		start = System.currentTimeMillis();
		generator.process();
		stop = System.currentTimeMillis();
		System.out.println("Results generated in " + (stop - start)/1000.0 + " seconds");
	}

	public void process() {
		long start, stop;
		String overallChartTitle = Messages.getString("ReportGenerator.THROUGHPUT_TOTAL_LABEL"); 
		String throughputChartTitle = Messages.getString("ReportGenerator.THROUGHPUT_TITLE"); 
		String yAxisLabel = Messages.getString("ReportGenerator.THROUGHPUT_YAXIS_LABEL");
		Map<String, Object> input = new HashMap<String, Object>();	
		start = System.currentTimeMillis();
		JmeterSummariserParser jmeterParser = new JmeterSummariserParser();
		jmeterParser.setFileName(jmeterFileName);
		jmeterParser.setMultipleChartTitle(throughputChartTitle);
		jmeterParser.setMultipleYAxisLabel(yAxisLabel);
		jmeterParser.processDirectory(searchingLocation);
		//always call it before call generating multiple chart string		
		String url = jmeterParser.generateChartStrings(overallChartTitle, yAxisLabel,
				"", jmeterParser.processData(jmeterParser.getAllInputList(), true),
				ResultParserHelper.scaleDown(jmeterParser.getAllTimeList(), 3), false);
		ArrayList<String> list = new ArrayList<String>();		
		list.add(url);
		charMap.put(overallChartTitle, list);
		generateMulitpleLinesChart(jmeterParser);
		
		charMap.put(throughputChartTitle, jmeterParser.getCharStrings());
		
		StatResult jmeterStats = StatResult.getStatistics(jmeterParser.getAllInputList());
    	input.put("jmeterStats", jmeterStats);
    	if(!jmeterParser.getAllTimeList().isEmpty()){
    		input.put("testStart", jmeterParser.getTestDate() + " " + jmeterParser.getAllTimeList().get(0));
    		input.put("testEnd", jmeterParser.getTestDate() + " " + jmeterParser.getAllTimeList().get(jmeterParser.getAllTimeList().size()-1));
    	}
		
    	input.put("charUrlMap", charMap);
		
		stop = System.currentTimeMillis();
		System.out.println("Parsed jmeter in " + (stop - start)/1000.0 + " seconds");
		
		start = System.currentTimeMillis();
		JmeterJTLParser jtlParser = new JmeterJTLParser();
		jtlParser.processResultsDirectory(searchingLocation);
		
    	input.put("totals", jtlParser.getResults());
    	String urls[] = {BOOK_FLIGHT,CANCEL_BOOKING,LOGIN,LOGOUT,LIST_BOOKINGS,QUERY_FLIGHT,UPDATE_CUSTOMER,VIEW_PROFILE,"Authorization"};

    	input.put("totalUrlMap" ,reorderTestcases(jtlParser.getResultsByUrl(), urls));	      
    	input.put("queryTotals", getTotals(QUERY_FLIGHT, jtlParser.getResultsByUrl()));
    	input.put("bookingTotals", getTotals(BOOK_FLIGHT, jtlParser.getResultsByUrl()));
    	input.put("loginTotals", getTotals(LOGIN, jtlParser.getResultsByUrl()));

		stop = System.currentTimeMillis();
		System.out.println("Parsed jmeter jtl files in " + (stop - start)/1000.0 + " seconds");



    	List<Object> nmonParsers = Messages.getConfiguration().getList("parsers.nmonParser.directory");
    	if (nmonParsers != null){
        	LinkedHashMap<String,StatResult> cpuList = new LinkedHashMap<String,StatResult>();
    		start = System.currentTimeMillis();
    		for(int i = 0;i < nmonParsers.size(); i++) {
    			
    			String enabled = Messages.getString("parsers.nmonParser("+i+")[@enabled]");			
    			if (enabled == null ||  !enabled.equalsIgnoreCase("false")) {

    				String directory = Messages.getString("parsers.nmonParser("+i+").directory");    				
    				String chartTitle = Messages.getString("parsers.nmonParser("+i+").chartTitle");
    				String label = Messages.getString("parsers.nmonParser("+i+").label");
    				String fileName = Messages.getString("parsers.nmonParser("+i+").fileName");    				
    				String relativePath = Messages.getString("parsers.nmonParser("+i+").directory[@relative]");
    				
    				if (relativePath == null ||  !relativePath.equalsIgnoreCase("false")) {
    					directory = searchingLocation +"/" + directory;
    				} 
    				if (fileName == null){
    					fileName = nmonFileName;
    				}

    				NmonParser nmon = parseNmonDirectory(directory, fileName, chartTitle);
    				cpuList  = addCpuStats(nmon, label, cpuList);
    			}
    		}
 
    		input.put("cpuList", cpuList);

    		stop = System.currentTimeMillis();
    		System.out.println("Parsed nmon files in " + (stop - start)/1000.0 + " seconds");
       	}				
		
		
		if (charMap.size() > 0) {
			start = System.currentTimeMillis();
			generateHtmlfile(input);
			stop = System.currentTimeMillis();
			System.out.println("Generated html file in " + (stop - start)/1000.0 + " seconds");
			System.out.println("Done, charts were saved to \""
							+ searchingLocation + System.getProperty("file.separator") + RESULTS_FILE + "\""); 
		} else {
			System.out.println("Failed, cannot find valid \"" 
							+ jmeterFileName + "\" or \"" + nmonFileName + "\" files in location " + searchingLocation); 
		}
	}
	
	private void generateMulitpleLinesChart(ResultParser parser) {
		if (parser.getResults().size()<=max_lines){
			parser.generateMultipleLinesCharString(parser.getMultipleChartTitle(),
				parser.getMultipleYAxisLabel(), "", parser.getResults());
		}else {
			System.out.println("More than "+max_lines+" throughput files found, will break them to "+max_lines+" each");
			ArrayList<IndividualChartResults> results= parser.getResults();
			int size = results.size();
			for (int i=0;i<size;i=i+max_lines){
				int endLocation = i+max_lines;
				if (endLocation >size) {
					endLocation=size;
				}
				parser.generateMultipleLinesCharString(parser.getMultipleChartTitle(),
						parser.getMultipleYAxisLabel(), "", results.subList(i,endLocation)); 
			}
		}
	}
	    

    
    private ArrayList<Double> getCombinedResultsList (NmonParser parser){
		Iterator<IndividualChartResults> itr = parser.getMultipleChartResults().getResults().iterator();
        ArrayList<Double> resultList = new ArrayList<Double>();
		while(itr.hasNext()){
			//trim trailing idle times from each of the individual results,
			//then combine the results together to get the final tallies. 				
			ArrayList<Double>  curList = itr.next().getInputList();
			
			for(int j = curList.size() - 1; j >= 0; j--){
				  if (curList.get(j).doubleValue() < 1){
					  curList.remove(j);
				  }
			}
			resultList.addAll(curList);
		}
		return resultList;
    }
   /* 
    private void generateHtmlfile(Map<String, Object> input) {	   
	    try{
	    	Configuration cfg = new Configuration();
	    	ClassTemplateLoader ctl = new ClassTemplateLoader(getClass(), "/templates");
	    	cfg.setTemplateLoader(ctl);	    	
	    	Template template = cfg.getTemplate("acmeair-report.ftl");
	    	
	    	Writer file = new FileWriter(new File(searchingLocation
					+ System.getProperty("file.separator") + RESULTS_FILE));
	    	template.process(input, file);
	    	file.flush();
	    	file.close();
	      
	    }catch(Exception e){
	    	e.printStackTrace();
	    }
    }
    
    */
    
    
    private void generateHtmlfile(Map<String, Object> input) {	   
	    try{
	    	VelocityEngine ve = new VelocityEngine();
	    	ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
	    	ve.setProperty("classpath.resource.loader.class",ClasspathResourceLoader.class.getName());
	    	ve.init();
	    	Template template = ve.getTemplate("templates/acmeair-report.vtl");
	    	VelocityContext context = new VelocityContext();
	    	 	    
	    	 
	    	for(Map.Entry<String, Object> entry: input.entrySet()){
	    		context.put(entry.getKey(), entry.getValue());
	    	}
	    	context.put("math", new MathTool());
	    	context.put("number", new NumberTool());
	    	context.put("date", new ComparisonDateTool());
	    	
	    	Writer file = new FileWriter(new File(searchingLocation
					+ System.getProperty("file.separator") + RESULTS_FILE));	    
	    	template.merge( context, file );
	    	file.flush();
	    	file.close();
	      
	    }catch(Exception e){
	    	e.printStackTrace();
	    }
    }

    private LinkedHashMap<String,StatResult> addCpuStats(NmonParser parser, String label, LinkedHashMap<String,StatResult> toAdd){
    	if (parser != null) {				
    		StatResult cpuStats = StatResult.getStatistics(getCombinedResultsList(parser));
    		cpuStats.setNumberOfResults(parser.getMultipleChartResults().getResults().size());
    		toAdd.put(label, cpuStats);			
    	}else {
    		System.out.println("no "+label+" cpu data found");
    	}
    	return toAdd;
    }
    
    
    /**
     * Re-orders a given map to using an array of Strings. 
     * Any remaining items in the map that was passed in will be appended to the end of
     * the map to be returned. 
     * @param totalUrlMap the map to be re-ordered. 
     * @param urls An array of Strings with the desired order for the map keys.
     * @return     A LinkedHashMap with the keys in the order requested. 
     * @see LinkedHashMap
     */
    private Map<String,JtlTotals> reorderTestcases(Map<String,JtlTotals> totalUrlMap, String urls[]){
    	LinkedHashMap<String,JtlTotals> newMap = new LinkedHashMap<String,JtlTotals>();
    	
    	Iterator<String> keys;
		for(int i=0; i< urls.length;i++){
			keys  = totalUrlMap.keySet().iterator();
			while (keys.hasNext()) {
				String key = keys.next();		        	      
	        	if(key.toLowerCase().contains(urls[i].toLowerCase())){
	        		newMap.put(key, totalUrlMap.get(key));	        		
	        	}			        	
	        }
		}
		//loop 2nd time to get the remaining items
		keys  = totalUrlMap.keySet().iterator();
		while (keys.hasNext()) {
	        String key = keys.next();
	        boolean found = false;		        
	        for(int i=0; i< urls.length;i++){
	        	if(key.toLowerCase().contains(urls[i].toLowerCase())){
	        		found = true;	
	        	}
	        }
	        if(!found){
	        	newMap.put(key, totalUrlMap.get(key));	        	
        	}
		}		
    	return newMap;
    }
  
    /**
     * Searches the map for the given jmeter testcase url key. 
     * The passed in string is expected to contain all or part of the desired key. 
     * for example "QueryFlight"  could match both "Mobile QueryFlight" and "Desktop QueryFlight" or just "QueryFlight".
     * If multiple results are found, their totals are added togehter in the JtlTotals Object returned. 
     * 
     * @param url         String, jMeter Testcase URL string to search for. 
     * @param totalUrlMap Map containing Strings and JtlTotals results. 
     * @return   JtlTotals object. 
     * @see JtlTotals
     */
    private JtlTotals getTotals(String url, Map<String,JtlTotals> totalUrlMap){
    	JtlTotals urlTotals = null;
    	Iterator<String> keys  = totalUrlMap.keySet().iterator();

    	while (keys.hasNext()) {
    		String key = keys.next();
    		if(key.toLowerCase().contains(url.toLowerCase())){

    			if(urlTotals == null){
    				urlTotals = totalUrlMap.get(key);
    			}else {
    				urlTotals.add(totalUrlMap.get(key));
    			}
    		}
    	}
    	return urlTotals;
    }

	
	/**
	 * Sets up a new NmonParser Object for parsing a given directory.
	 * @param directory   directory to search for nmon files.
	 * @param chartTitle  Name of the title for the chart to be generated. 
	 * @return            NmonParser object
	 */
	private NmonParser parseNmonDirectory (String directory, String fileName, String chartTitle ){	
		if (!new File(directory).isDirectory()) {
			return null;
		}		
		NmonParser parser = new NmonParser();
		parser.setFileName(fileName);
		parser.setMultipleChartTitle(chartTitle);
		parser.processDirectory(directory);
		generateMulitpleLinesChart(parser);
		charMap.put(chartTitle, parser.getCharStrings());
		return parser;
	}
}

import java.io.Serializable;

import org.mongodb.morphia.annotations.Entity;

import com.acmeair.entities.AirportCodeMapping;

@Entity(value="airportCodeMapping")
public class AirportCodeMappingImpl implements AirportCodeMapping, Serializable{
	
	private static final long serialVersionUID = 1L;

	private String _id;
	private String airportName;
	
	public AirportCodeMappingImpl() {
	}
	
	public AirportCodeMappingImpl(String airportCode, String airportName) {
		this._id = airportCode;
		this.airportName = airportName;
	}
	
	public String getAirportCode() {
		return _id;
	}
	
	public void setAirportCode(String airportCode) {
		this._id = airportCode;
	}
	
	public String getAirportName() {
		return airportName;
	}
	
	public void setAirportName(String airportName) {
		this.airportName = airportName;
	}

}

import java.io.Serializable;
import java.util.Date;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import com.acmeair.entities.Booking;
import com.acmeair.entities.Customer;
import com.acmeair.entities.Flight;

@Entity(value="booking")
public class BookingImpl implements Booking, Serializable{
	
	private static final long serialVersionUID = 1L;
	
	@Id
	private String _id;	
	private String flightId;
	private String customerId;
	private Date dateOfBooking;
		
	public BookingImpl() {
	}
	
	public BookingImpl(String bookingId, Date dateOfFlight, String customerId, String flightId) {
		this._id = bookingId;		
		this.flightId = flightId;
		this.customerId = customerId;
		this.dateOfBooking = dateOfFlight;
	}
	
	public BookingImpl(String bookingId, Date dateOfFlight, Customer customer, Flight flight) {		
		this._id = bookingId;
		this.flightId = flight.getFlightId();
		this.dateOfBooking = dateOfFlight;
		this.customerId = customer.getCustomerId();		
	}
	
	
	public String getBookingId() {
		return _id;
	}
	
	public void setBookingId(String bookingId) {
		this._id = bookingId;
	}

	public String getFlightId() {
		return flightId;
	}

	public void setFlightId(String flightId) {
		this.flightId = flightId;
	}

	

	public Date getDateOfBooking() {
		return dateOfBooking;
	}
	
	public void setDateOfBooking(Date dateOfBooking) {
		this.dateOfBooking = dateOfBooking;
	}

	public String getCustomerId() {
		return customerId;
	}
	
	public void setCustomer(String customerId) {
		this.customerId = customerId;
	}



	@Override
	public String toString() {
		return "Booking [key=" + _id + ", flightId=" + flightId
				+ ", dateOfBooking=" + dateOfBooking + ", customerId=" + customerId + "]";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BookingImpl other = (BookingImpl) obj;
		if (customerId == null) {
			if (other.customerId != null)
				return false;
		} else if (!customerId.equals(other.customerId))
			return false;
		if (dateOfBooking == null) {
			if (other.dateOfBooking != null)
				return false;
		} else if (!dateOfBooking.equals(other.dateOfBooking))
			return false;
		if (flightId == null) {
			if (other.flightId != null)
				return false;
		} else if (!flightId.equals(other.flightId))
			return false;		
		return true;
	}

}

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.acmeair.entities.CustomerAddress;

@XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
@XmlRootElement
public class CustomerAddressImpl implements CustomerAddress, Serializable{
	
	private static final long serialVersionUID = 1L;
	private String streetAddress1;
	private String streetAddress2;
	private String city;
	private String stateProvince;
	private String country;
	private String postalCode;

	public CustomerAddressImpl() {
	}
	
	public CustomerAddressImpl(String streetAddress1, String streetAddress2,
			String city, String stateProvince, String country, String postalCode) {
		super();
		this.streetAddress1 = streetAddress1;
		this.streetAddress2 = streetAddress2;
		this.city = city;
		this.stateProvince = stateProvince;
		this.country = country;
		this.postalCode = postalCode;
	}
	
	public String getStreetAddress1() {
		return streetAddress1;
	}
	public void setStreetAddress1(String streetAddress1) {
		this.streetAddress1 = streetAddress1;
	}
	public String getStreetAddress2() {
		return streetAddress2;
	}
	public void setStreetAddress2(String streetAddress2) {
		this.streetAddress2 = streetAddress2;
	}
	public String getCity() {
		return city;
	}
	public void setCity(String city) {
		this.city = city;
	}
	public String getStateProvince() {
		return stateProvince;
	}
	public void setStateProvince(String stateProvince) {
		this.stateProvince = stateProvince;
	}
	public String getCountry() {
		return country;
	}
	public void setCountry(String country) {
		this.country = country;
	}
	public String getPostalCode() {
		return postalCode;
	}
	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}
	@Override
	public String toString() {
		return "CustomerAddress [streetAddress1=" + streetAddress1
				+ ", streetAddress2=" + streetAddress2 + ", city=" + city
				+ ", stateProvince=" + stateProvince + ", country=" + country
				+ ", postalCode=" + postalCode + "]";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CustomerAddressImpl other = (CustomerAddressImpl) obj;
		if (city == null) {
			if (other.city != null)
				return false;
		} else if (!city.equals(other.city))
			return false;
		if (country == null) {
			if (other.country != null)
				return false;
		} else if (!country.equals(other.country))
			return false;
		if (postalCode == null) {
			if (other.postalCode != null)
				return false;
		} else if (!postalCode.equals(other.postalCode))
			return false;
		if (stateProvince == null) {
			if (other.stateProvince != null)
				return false;
		} else if (!stateProvince.equals(other.stateProvince))
			return false;
		if (streetAddress1 == null) {
			if (other.streetAddress1 != null)
				return false;
		} else if (!streetAddress1.equals(other.streetAddress1))
			return false;
		if (streetAddress2 == null) {
			if (other.streetAddress2 != null)
				return false;
		} else if (!streetAddress2.equals(other.streetAddress2))
			return false;
		return true;
	}
	
	
}

import java.io.Serializable;

import org.mongodb.morphia.annotations.Entity;

import com.acmeair.entities.Customer;
import com.acmeair.entities.CustomerAddress;

@Entity(value="customer")
public class CustomerImpl implements Customer, Serializable{

	private static final long serialVersionUID = 1L;

	private String _id;
	private String password;
	private MemberShipStatus status;
	private int total_miles;
	private int miles_ytd;

	private CustomerAddress address;
	private String phoneNumber;
	private PhoneType phoneNumberType;
	
	public CustomerImpl() {
	}
	
	public CustomerImpl(String username, String password, MemberShipStatus status, int total_miles, int miles_ytd, CustomerAddress address, String phoneNumber, PhoneType phoneNumberType) {
		this._id = username;
		this.password = password;
		this.status = status;
		this.total_miles = total_miles;
		this.miles_ytd = miles_ytd;
		this.address = address;
		this.phoneNumber = phoneNumber;
		this.phoneNumberType = phoneNumberType;
	}

	public String getCustomerId(){
		return _id;
	}
	
	public String getUsername() {
		return _id;
	}
	
	public void setUsername(String username) {
		this._id = username;
	}
	
	public String getPassword() {
		return password;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	public MemberShipStatus getStatus() {
		return status;
	}
	
	public void setStatus(MemberShipStatus status) {
		this.status = status;
	}
	
	public int getTotal_miles() {
		return total_miles;
	}
	
	public void setTotal_miles(int total_miles) {
		this.total_miles = total_miles;
	}
	
	public int getMiles_ytd() {
		return miles_ytd;
	}
	
	public void setMiles_ytd(int miles_ytd) {
		this.miles_ytd = miles_ytd;
	}
	
	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public PhoneType getPhoneNumberType() {
		return phoneNumberType;
	}

	public void setPhoneNumberType(PhoneType phoneNumberType) {
		this.phoneNumberType = phoneNumberType;
	}

	public CustomerAddress getAddress() {
		return address;
	}

	public void setAddress(CustomerAddress address) {
		this.address = address;
	}

	@Override
	public String toString() {
		return "Customer [id=" + _id + ", password=" + password + ", status="
				+ status + ", total_miles=" + total_miles + ", miles_ytd="
				+ miles_ytd + ", address=" + address + ", phoneNumber="
				+ phoneNumber + ", phoneNumberType=" + phoneNumberType + "]";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CustomerImpl other = (CustomerImpl) obj;
		if (address == null) {
			if (other.address != null)
				return false;
		} else if (!address.equals(other.address))
			return false;
		if (_id == null) {
			if (other._id != null)
				return false;
		} else if (!_id.equals(other._id))
			return false;
		if (miles_ytd != other.miles_ytd)
			return false;
		if (password == null) {
			if (other.password != null)
				return false;
		} else if (!password.equals(other.password))
			return false;
		if (phoneNumber == null) {
			if (other.phoneNumber != null)
				return false;
		} else if (!phoneNumber.equals(other.phoneNumber))
			return false;
		if (phoneNumberType != other.phoneNumberType)
			return false;
		if (status != other.status)
			return false;
		if (total_miles != other.total_miles)
			return false;
		return true;
	}

}

import java.io.Serializable;
import java.util.Date;

import org.mongodb.morphia.annotations.Entity;

import com.acmeair.entities.CustomerSession;

@Entity(value="customerSession")
public class CustomerSessionImpl implements CustomerSession, Serializable {


	private static final long serialVersionUID = 1L;

	private String _id;
	private String customerid;
	private Date lastAccessedTime;
	private Date timeoutTime;
	
	public CustomerSessionImpl() {
	}

	public CustomerSessionImpl(String id, String customerid, Date lastAccessedTime,	Date timeoutTime) {
		this._id= id;
		this.customerid = customerid;
		this.lastAccessedTime = lastAccessedTime;
		this.timeoutTime = timeoutTime;
	}
	

	public String getId() {
		return _id;
	}

	public void setId(String id) {
		this._id = id;
	}

	public String getCustomerid() {
		return customerid;
	}

	public void setCustomerid(String customerid) {
		this.customerid = customerid;
	}

	public Date getLastAccessedTime() {
		return lastAccessedTime;
	}

	public void setLastAccessedTime(Date lastAccessedTime) {
		this.lastAccessedTime = lastAccessedTime;
	}

	public Date getTimeoutTime() {
		return timeoutTime;
	}

	public void setTimeoutTime(Date timeoutTime) {
		this.timeoutTime = timeoutTime;
	}

	@Override
	public String toString() {
		return "CustomerSession [id=" + _id + ", customerid=" + customerid
				+ ", lastAccessedTime=" + lastAccessedTime + ", timeoutTime="
				+ timeoutTime + "]";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CustomerSessionImpl other = (CustomerSessionImpl) obj;
		if (customerid == null) {
			if (other.customerid != null)
				return false;
		} else if (!customerid.equals(other.customerid))
			return false;
		if (_id == null) {
			if (other._id != null)
				return false;
		} else if (!_id.equals(other._id))
			return false;
		if (lastAccessedTime == null) {
			if (other.lastAccessedTime != null)
				return false;
		} else if (!lastAccessedTime.equals(other.lastAccessedTime))
			return false;
		if (timeoutTime == null) {
			if (other.timeoutTime != null)
				return false;
		} else if (!timeoutTime.equals(other.timeoutTime))
			return false;
		return true;
	}


	
}

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import com.acmeair.entities.Flight;
import com.acmeair.entities.FlightSegment;

@Entity(value="flight")
public class FlightImpl implements Flight, Serializable{

	private static final long serialVersionUID = 1L;
	
	@Id
	private String _id;
	
	private String flightSegmentId;
		
	private Date scheduledDepartureTime;
	private Date scheduledArrivalTime;
	private BigDecimal firstClassBaseCost;
	private BigDecimal economyClassBaseCost;
	private int numFirstClassSeats;
	private int numEconomyClassSeats;
	private String airplaneTypeId;
	
	private FlightSegment flightSegment;
	
	public FlightImpl() {
	}
	
	public FlightImpl(String id, String flightSegmentId,
			Date scheduledDepartureTime, Date scheduledArrivalTime,
			BigDecimal firstClassBaseCost, BigDecimal economyClassBaseCost,
			int numFirstClassSeats, int numEconomyClassSeats,
			String airplaneTypeId) {
		this._id = id;
		this.flightSegmentId = flightSegmentId;
		this.scheduledDepartureTime = scheduledDepartureTime;
		this.scheduledArrivalTime = scheduledArrivalTime;
		this.firstClassBaseCost = firstClassBaseCost;
		this.economyClassBaseCost = economyClassBaseCost;
		this.numFirstClassSeats = numFirstClassSeats;
		this.numEconomyClassSeats = numEconomyClassSeats;
		this.airplaneTypeId = airplaneTypeId;
	}

	public String getFlightId(){
		return _id;
	}
	
	public void setFlightId(String id){
		this._id = id;
	}
	
	public String getFlightSegmentId()
	{
		return flightSegmentId;
	}
	
	public void setFlightSegmentId(String segmentId){
		this.flightSegmentId = segmentId;
	}
	
	public Date getScheduledDepartureTime() {
		return scheduledDepartureTime;
	}


	public void setScheduledDepartureTime(Date scheduledDepartureTime) {
		this.scheduledDepartureTime = scheduledDepartureTime;
	}


	public Date getScheduledArrivalTime() {
		return scheduledArrivalTime;
	}


	public void setScheduledArrivalTime(Date scheduledArrivalTime) {
		this.scheduledArrivalTime = scheduledArrivalTime;
	}


	public BigDecimal getFirstClassBaseCost() {
		return firstClassBaseCost;
	}


	public void setFirstClassBaseCost(BigDecimal firstClassBaseCost) {
		this.firstClassBaseCost = firstClassBaseCost;
	}


	public BigDecimal getEconomyClassBaseCost() {
		return economyClassBaseCost;
	}


	public void setEconomyClassBaseCost(BigDecimal economyClassBaseCost) {
		this.economyClassBaseCost = economyClassBaseCost;
	}


	public int getNumFirstClassSeats() {
		return numFirstClassSeats;
	}


	public void setNumFirstClassSeats(int numFirstClassSeats) {
		this.numFirstClassSeats = numFirstClassSeats;
	}


	public int getNumEconomyClassSeats() {
		return numEconomyClassSeats;
	}


	public void setNumEconomyClassSeats(int numEconomyClassSeats) {
		this.numEconomyClassSeats = numEconomyClassSeats;
	}


	public String getAirplaneTypeId() {
		return airplaneTypeId;
	}


	public void setAirplaneTypeId(String airplaneTypeId) {
		this.airplaneTypeId = airplaneTypeId;
	}


	public FlightSegment getFlightSegment() {
		return flightSegment;
	}

	public void setFlightSegment(FlightSegment flightSegment) {
		this.flightSegment = flightSegment;
	}

	@Override
	public String toString() {
		return "Flight key="+_id
				+ ", scheduledDepartureTime=" + scheduledDepartureTime
				+ ", scheduledArrivalTime=" + scheduledArrivalTime
				+ ", firstClassBaseCost=" + firstClassBaseCost
				+ ", economyClassBaseCost=" + economyClassBaseCost
				+ ", numFirstClassSeats=" + numFirstClassSeats
				+ ", numEconomyClassSeats=" + numEconomyClassSeats
				+ ", airplaneTypeId=" + airplaneTypeId + "]";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FlightImpl other = (FlightImpl) obj;
		if (airplaneTypeId == null) {
			if (other.airplaneTypeId != null)
				return false;
		} else if (!airplaneTypeId.equals(other.airplaneTypeId))
			return false;
		if (economyClassBaseCost == null) {
			if (other.economyClassBaseCost != null)
				return false;
		} else if (!economyClassBaseCost.equals(other.economyClassBaseCost))
			return false;
		if (firstClassBaseCost == null) {
			if (other.firstClassBaseCost != null)
				return false;
		} else if (!firstClassBaseCost.equals(other.firstClassBaseCost))
			return false;
		if (flightSegment == null) {
			if (other.flightSegment != null)
				return false;
		} else if (!flightSegment.equals(other.flightSegment))
			return false;
		if (_id == null) {
			if (other._id != null)
				return false;
		} else if (!_id.equals(other._id))
			return false;
		if (numEconomyClassSeats != other.numEconomyClassSeats)
			return false;
		if (numFirstClassSeats != other.numFirstClassSeats)
			return false;
		if (scheduledArrivalTime == null) {
			if (other.scheduledArrivalTime != null)
				return false;
		} else if (!scheduledArrivalTime.equals(other.scheduledArrivalTime))
			return false;
		if (scheduledDepartureTime == null) {
			if (other.scheduledDepartureTime != null)
				return false;
		} else if (!scheduledDepartureTime.equals(other.scheduledDepartureTime))
			return false;
		return true;
	}
	
	
}

import java.io.Serializable;

import org.mongodb.morphia.annotations.Entity;

import com.acmeair.entities.FlightSegment;

@Entity(value="flightSegment")
public class FlightSegmentImpl implements FlightSegment, Serializable{

	private static final long serialVersionUID = 1L;

	private String _id;
	private String originPort;
	private String destPort;
	private int miles;

	public FlightSegmentImpl() {
	}
	
	public FlightSegmentImpl(String flightName, String origPort, String destPort, int miles) {
		this._id = flightName;
		this.originPort = origPort;
		this.destPort = destPort;
		this.miles = miles;
	}
	
	public String getFlightName() {
		return _id;
	}

	public void setFlightName(String flightName) {
		this._id = flightName;
	}

	public String getOriginPort() {
		return originPort;
	}

	public void setOriginPort(String originPort) {
		this.originPort = originPort;
	}

	public String getDestPort() {
		return destPort;
	}

	public void setDestPort(String destPort) {
		this.destPort = destPort;
	}

	public int getMiles() {
		return miles;
	}

	public void setMiles(int miles) {
		this.miles = miles;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("FlightSegment ").append(_id).append(" originating from:\"").append(originPort).append("\" arriving at:\"").append(destPort).append("\"");
		return sb.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FlightSegmentImpl other = (FlightSegmentImpl) obj;
		if (destPort == null) {
			if (other.destPort != null)
				return false;
		} else if (!destPort.equals(other.destPort))
			return false;
		if (_id == null) {
			if (other._id != null)
				return false;
		} else if (!_id.equals(other._id))
			return false;
		if (miles != other.miles)
			return false;
		if (originPort == null) {
			if (other.originPort != null)
				return false;
		} else if (!originPort.equals(other.originPort))
			return false;
		return true;
	}
	
	
}

import java.io.IOException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.acmeair.morphia.BigDecimalConverter;
import com.acmeair.morphia.MorphiaConstants;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;

public class MongoConnectionManager implements MorphiaConstants{

	private static AtomicReference<MongoConnectionManager> connectionManager = new AtomicReference<MongoConnectionManager>();
	
	private final static Logger logger = Logger.getLogger(MongoConnectionManager.class.getName());
	
	@Resource(name = JNDI_NAME)
	protected DB db;
	private static Datastore datastore;
	
	public static MongoConnectionManager getConnectionManager() {
		if (connectionManager.get() == null) {
			synchronized (connectionManager) {
				if (connectionManager.get() == null) {
					connectionManager.set(new MongoConnectionManager());
				}
			}
		}
		return connectionManager.get();
	}
	
	
	private MongoConnectionManager (){

		Morphia morphia = new Morphia();
		// Set default client options, and then check if there is a properties file.
		boolean fsync = false;
		int w = 0;
		int connectionsPerHost = 5;
		int threadsAllowedToBlockForConnectionMultiplier = 10;
		int connectTimeout= 0;
		int socketTimeout= 0;
		boolean socketKeepAlive = true;
		int maxWaitTime = 2000;


		Properties prop = new Properties();
		URL mongoPropertyFile = MongoConnectionManager.class.getResource("/com/acmeair/morphia/services/util/mongo.properties");
		if(mongoPropertyFile != null){
			try {
				logger.info("Reading mongo.properties file");
				prop.load(mongoPropertyFile.openStream());
				fsync = new Boolean(prop.getProperty("mongo.fsync"));
				w = new Integer(prop.getProperty("mongo.w"));
				connectionsPerHost = new Integer(prop.getProperty("mongo.connectionsPerHost"));
				threadsAllowedToBlockForConnectionMultiplier = new Integer(prop.getProperty("mongo.threadsAllowedToBlockForConnectionMultiplier"));
				connectTimeout= new Integer(prop.getProperty("mongo.connectTimeout"));
				socketTimeout= new Integer(prop.getProperty("mongo.socketTimeout"));
				socketKeepAlive = new Boolean(prop.getProperty("mongo.socketKeepAlive"));
				maxWaitTime =new Integer(prop.getProperty("mongo.maxWaitTime"));
			}catch (IOException ioe){
				logger.severe("Exception when trying to read from the mongo.properties file" + ioe.getMessage());
			}
		}
		
		// Set the client options
		MongoClientOptions.Builder builder = new MongoClientOptions.Builder()
			.writeConcern(new WriteConcern(w, 0, fsync))
			.connectionsPerHost(connectionsPerHost)
			.connectTimeout(connectTimeout)
			.socketTimeout(socketTimeout)
			.socketKeepAlive(socketKeepAlive)
			.maxWaitTime(maxWaitTime)
			.threadsAllowedToBlockForConnectionMultiplier(threadsAllowedToBlockForConnectionMultiplier);

				
		try {
			//Check if VCAP_SERVICES exist, and if it does, look up the url from the credentials.
			String vcapJSONString = System.getenv("VCAP_SERVICES");
			if (vcapJSONString != null) {
				logger.info("Reading VCAP_SERVICES");
				Object jsonObject = JSONValue.parse(vcapJSONString);
				JSONObject vcapServices = (JSONObject)jsonObject;
				JSONArray mongoServiceArray =null;					
				for (Object key : vcapServices.keySet()){
					if (key.toString().startsWith("mongo")){
						mongoServiceArray = (JSONArray) vcapServices.get(key);
						break;
					}
				}
				
				if (mongoServiceArray == null) {
					logger.severe("VCAP_SERVICES existed, but a mongo service was not definied.");
				} else {					
					JSONObject mongoService = (JSONObject)mongoServiceArray.get(0); 
					JSONObject credentials = (JSONObject)mongoService.get("credentials");
					String url = (String) credentials.get("url");
					logger.fine("service url = " + url);				
					MongoClientURI mongoURI = new MongoClientURI(url, builder);
					MongoClient mongo = new MongoClient(mongoURI);

					morphia.getMapper().getConverters().addConverter(new BigDecimalConverter());
					datastore = morphia.createDatastore( mongo ,mongoURI.getDatabase());
				}	

			} else {
				//VCAP_SERVICES don't exist, so use the DB resource  
				logger.fine("No VCAP_SERVICES found");
				if(db == null){
					try {
						logger.warning("Resource Injection failed. Attempting to look up " + JNDI_NAME + " via JNDI.");
						db = (DB) new InitialContext().lookup(JNDI_NAME);
					} catch (NamingException e) {
						logger.severe("Caught NamingException : " + e.getMessage() );
					}	        
				}

				if(db == null){
					String host; 
					String port;
					String database;
					logger.info("Creating the MongoDB Client connection. Looking up host and port information " );
					try {	        	
						host = (String) new InitialContext().lookup("java:comp/env/" + HOSTNAME);
						port = (String) new InitialContext().lookup("java:comp/env/" + PORT);
						database = (String) new InitialContext().lookup("java:comp/env/" + DATABASE);
						ServerAddress server = new ServerAddress(host, Integer.parseInt(port));
						MongoClient mongo = new MongoClient(server);
						db = mongo.getDB(database);
					} catch (NamingException e) {
						logger.severe("Caught NamingException : " + e.getMessage() );			
					} catch (Exception e) {
						logger.severe("Caught Exception : " + e.getMessage() );
					}
				}

				if(db == null){
					logger.severe("Unable to retreive reference to database, please check the server logs.");
				} else {
					
					morphia.getMapper().getConverters().addConverter(new BigDecimalConverter());
					datastore = morphia.createDatastore(new MongoClient(db.getMongo().getConnectPoint(),builder.build()), db.getName());
				}
			}
		} catch (UnknownHostException e) {
			logger.severe("Caught Exception : " + e.getMessage() );				
		}			

		logger.info("created mongo datastore with options:"+datastore.getMongo().getMongoClientOptions());
	}
	
	public DB getDB(){
		return db;
	}
	
	public Datastore getDatastore(){
		return datastore;
	}
	
	@SuppressWarnings("deprecation")
	public String getDriverVersion(){
		return datastore.getMongo().getVersion();
	}
	
	public String getMongoVersion(){
		return datastore.getDB().command("buildInfo").getString("version");
	}
}

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.mongodb.morphia.Datastore;

import com.acmeair.entities.Booking;
import com.acmeair.entities.Customer;
import com.acmeair.entities.Flight;
import com.acmeair.morphia.MorphiaConstants;
import com.acmeair.morphia.entities.BookingImpl;
import com.acmeair.morphia.services.util.MongoConnectionManager;
import com.acmeair.service.BookingService;
import com.acmeair.service.CustomerService;
import com.acmeair.service.DataService;
import com.acmeair.service.FlightService;
import com.acmeair.service.KeyGenerator;
import com.acmeair.service.ServiceLocator;

import org.mongodb.morphia.query.Query;



@DataService(name=MorphiaConstants.KEY,description=MorphiaConstants.KEY_DESCRIPTION)
public class BookingServiceImpl implements BookingService, MorphiaConstants {

	//private final static Logger logger = Logger.getLogger(BookingService.class.getName()); 

		
	Datastore datastore;
	
	@Inject 
	KeyGenerator keyGenerator;
	
	private FlightService flightService = ServiceLocator.instance().getService(FlightService.class);
	private CustomerService customerService = ServiceLocator.instance().getService(CustomerService.class);


	@PostConstruct
	public void initialization() {	
		datastore = MongoConnectionManager.getConnectionManager().getDatastore();	
	}	
	
	
	
	public String bookFlight(String customerId, String flightId) {
		try{
			Flight f = flightService.getFlightByFlightId(flightId, null);
			Customer c = customerService.getCustomerByUsername(customerId);
			
			Booking newBooking = new BookingImpl(keyGenerator.generate().toString(), new Date(), c, f);

			datastore.save(newBooking);
			return newBooking.getBookingId();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String bookFlight(String customerId, String flightSegmentId, String flightId) {
		return bookFlight(customerId, flightId);	
	}
	
	@Override
	public Booking getBooking(String user, String bookingId) {
		try{
			Query<BookingImpl> q = datastore.find(BookingImpl.class).field("_id").equal(bookingId);
			Booking booking = q.get();
			
			return booking;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<Booking> getBookingsByUser(String user) {
		try{
			Query<BookingImpl> q = datastore.find(BookingImpl.class).disableValidation().field("customerId").equal(user);
			List<BookingImpl> bookingImpls = q.asList();
			List<Booking> bookings = new ArrayList<Booking>();
			for(Booking b: bookingImpls){
				bookings.add(b);
			}
			return bookings;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void cancelBooking(String user, String bookingId) {
		try{
			datastore.delete(BookingImpl.class, bookingId);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	
	@Override
	public Long count() {
		return datastore.find(BookingImpl.class).countAll();
	}	
}

import java.util.Date;

import javax.annotation.PostConstruct;

import com.acmeair.entities.Customer;
import com.acmeair.entities.Customer.MemberShipStatus;
import com.acmeair.entities.Customer.PhoneType;
import com.acmeair.entities.CustomerAddress;
import com.acmeair.entities.CustomerSession;
import com.acmeair.morphia.entities.CustomerAddressImpl;
import com.acmeair.morphia.entities.CustomerSessionImpl;
import com.acmeair.morphia.MorphiaConstants;
import com.acmeair.morphia.entities.CustomerImpl;
import com.acmeair.morphia.services.util.MongoConnectionManager;
import com.acmeair.service.DataService;
import com.acmeair.service.CustomerService;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;



@DataService(name=MorphiaConstants.KEY,description=MorphiaConstants.KEY_DESCRIPTION)
public class CustomerServiceImpl extends CustomerService implements MorphiaConstants {	
		
//	private final static Logger logger = Logger.getLogger(CustomerService.class.getName()); 
	
	protected Datastore datastore;
		
	
	@PostConstruct
	public void initialization() {	
		datastore = MongoConnectionManager.getConnectionManager().getDatastore();
	}
	
	@Override
	public Long count() {
		return datastore.find(CustomerImpl.class).countAll();
	}
	
	@Override
	public Long countSessions() {
		return datastore.find(CustomerSessionImpl.class).countAll();
	}
	
	@Override
	public Customer createCustomer(String username, String password,
			MemberShipStatus status, int total_miles, int miles_ytd,
			String phoneNumber, PhoneType phoneNumberType,
			CustomerAddress address) {
	
		Customer customer = new CustomerImpl(username, password, status, total_miles, miles_ytd, address, phoneNumber, phoneNumberType);
		datastore.save(customer);
		return customer;
	}
	
	@Override 
	public CustomerAddress createAddress (String streetAddress1, String streetAddress2,
			String city, String stateProvince, String country, String postalCode){
		CustomerAddress address = new CustomerAddressImpl(streetAddress1, streetAddress2,
				 city, stateProvince,  country,  postalCode);
		return address;
	}

	@Override
	public Customer updateCustomer(Customer customer) {
		datastore.save(customer);
		return customer;
	}

	@Override
	protected Customer getCustomer(String username) {
		Query<CustomerImpl> q = datastore.find(CustomerImpl.class).field("_id").equal(username);
		Customer customer = q.get();					
		return customer;
	}
	
	@Override
	public Customer getCustomerByUsername(String username) {
		Query<CustomerImpl> q = datastore.find(CustomerImpl.class).field("_id").equal(username);
		Customer customer = q.get();
		if (customer != null) {
			customer.setPassword(null);
		}			
		return customer;
	}
	
	@Override
	protected CustomerSession getSession(String sessionid){
		Query<CustomerSessionImpl> q = datastore.find(CustomerSessionImpl.class).field("_id").equal(sessionid);		
		return q.get();
	}
	
	@Override
	protected void removeSession(CustomerSession session){		
		datastore.delete(session);	
	}
	
	@Override
	protected  CustomerSession createSession(String sessionId, String customerId, Date creation, Date expiration) {
		CustomerSession cSession = new CustomerSessionImpl(sessionId, customerId, creation, expiration);
		datastore.save(cSession);
		return cSession;
	}

	@Override
	public void invalidateSession(String sessionid) {		
		Query<CustomerSessionImpl> q = datastore.find(CustomerSessionImpl.class).field("_id").equal(sessionid);
		datastore.delete(q);
	}

}

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.acmeair.entities.AirportCodeMapping;
import com.acmeair.entities.Flight;
import com.acmeair.entities.FlightSegment;
import com.acmeair.morphia.MorphiaConstants;
import com.acmeair.morphia.entities.AirportCodeMappingImpl;
import com.acmeair.morphia.entities.FlightImpl;
import com.acmeair.morphia.entities.FlightSegmentImpl;
import com.acmeair.morphia.services.util.MongoConnectionManager;
import com.acmeair.service.DataService;
import com.acmeair.service.FlightService;
import com.acmeair.service.KeyGenerator;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;

@DataService(name=MorphiaConstants.KEY,description=MorphiaConstants.KEY_DESCRIPTION)
public class FlightServiceImpl extends FlightService implements  MorphiaConstants {

	//private final static Logger logger = Logger.getLogger(FlightService.class.getName()); 
		
	Datastore datastore;
	
	@Inject
	KeyGenerator keyGenerator;
	

	
	@PostConstruct
	public void initialization() {	
		datastore = MongoConnectionManager.getConnectionManager().getDatastore();
	}
	
	
	@Override
	public Long countFlights() {
		return datastore.find(FlightImpl.class).countAll();
	}
	
	@Override
	public Long countFlightSegments() {
		return datastore.find(FlightSegmentImpl.class).countAll();
	}
	
	@Override
	public Long countAirports() {
		return datastore.find(AirportCodeMappingImpl.class).countAll();
	}
	
	/*
	@Override
	public Flight getFlightByFlightId(String flightId, String flightSegmentId) {
		try {
			Flight flight = flightPKtoFlightCache.get(flightId);
			if (flight == null) {
				Query<FlightImpl> q = datastore.find(FlightImpl.class).field("_id").equal(flightId);
				flight = q.get();
				if (flightId != null && flight != null) {
					flightPKtoFlightCache.putIfAbsent(flight, flight);
				}
			}
			return flight;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	*/
	
	protected Flight getFlight(String flightId, String segmentId) {
		Query<FlightImpl> q = datastore.find(FlightImpl.class).field("_id").equal(flightId);
		return q.get();
	}

	@Override
	protected  FlightSegment getFlightSegment(String fromAirport, String toAirport){
		Query<FlightSegmentImpl> q = datastore.find(FlightSegmentImpl.class).field("originPort").equal(fromAirport).field("destPort").equal(toAirport);
		FlightSegment segment = q.get();
		if (segment == null) {
			segment = new FlightSegmentImpl(); // put a sentinel value of a non-populated flightsegment 
		}
		return segment;
	}
	
	@Override
	protected  List<Flight> getFlightBySegment(FlightSegment segment, Date deptDate){
		Query<FlightImpl> q2;
		if(deptDate != null) {
			q2 = datastore.find(FlightImpl.class).disableValidation().field("flightSegmentId").equal(segment.getFlightName()).field("scheduledDepartureTime").equal(deptDate);
		} else {
			q2 = datastore.find(FlightImpl.class).disableValidation().field("flightSegmentId").equal(segment.getFlightName());
		}
		List<FlightImpl> flightImpls = q2.asList();
		List<Flight> flights;
		if (flightImpls != null) {
			flights =  new ArrayList<Flight>(); 
			for (Flight flight : flightImpls) {
				flight.setFlightSegment(segment);
				flights.add(flight);
			}
		}
		else {
			flights = new ArrayList<Flight>(); // put an empty list into the cache in the cache in the case where no matching flights
		}
		return flights;
	}
	

	@Override
	public void storeAirportMapping(AirportCodeMapping mapping) {
		try{
			datastore.save(mapping);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override 
	public AirportCodeMapping createAirportCodeMapping(String airportCode, String airportName){
		AirportCodeMapping acm = new AirportCodeMappingImpl(airportCode, airportName);
		return acm;
	}
	
	@Override
	public Flight createNewFlight(String flightSegmentId,
			Date scheduledDepartureTime, Date scheduledArrivalTime,
			BigDecimal firstClassBaseCost, BigDecimal economyClassBaseCost,
			int numFirstClassSeats, int numEconomyClassSeats,
			String airplaneTypeId) {
		String id = keyGenerator.generate().toString();
		Flight flight = new FlightImpl(id, flightSegmentId,
			scheduledDepartureTime, scheduledArrivalTime,
			firstClassBaseCost, economyClassBaseCost,
			numFirstClassSeats, numEconomyClassSeats,
			airplaneTypeId);
		try{
			datastore.save(flight);
			return flight;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void storeFlightSegment(FlightSegment flightSeg) {
		try{
			datastore.save(flightSeg);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override 
	public void storeFlightSegment(String flightName, String origPort, String destPort, int miles) {
		FlightSegment flightSeg = new FlightSegmentImpl(flightName, origPort, destPort, miles);
		storeFlightSegment(flightSeg);
	}
}

import java.math.BigDecimal;

import org.mongodb.morphia.converters.SimpleValueConverter;
import org.mongodb.morphia.converters.TypeConverter;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.MappingException;

public class BigDecimalConverter extends TypeConverter implements SimpleValueConverter{

    public BigDecimalConverter() {
        super(BigDecimal.class);
    }

    @Override
    public Object encode(Object value, MappedField optionalExtraInfo) {
        return value.toString();
    }

    @Override
    public Object decode(Class targetClass, Object fromDBObject, MappedField optionalExtraInfo) throws MappingException {
        if (fromDBObject == null) return null;
        return new BigDecimal(fromDBObject.toString());
    }
}

import java.math.BigInteger;

import org.mongodb.morphia.converters.SimpleValueConverter;
import org.mongodb.morphia.converters.TypeConverter;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.MappingException;

public class BigIntegerConverter extends TypeConverter implements SimpleValueConverter{

    public BigIntegerConverter() {
        super(BigInteger.class);
    }

    @Override
    public Object encode(Object value, MappedField optionalExtraInfo) {
        return value.toString();
    }

    @Override
    public Object decode(Class targetClass, Object fromDBObject, MappedField optionalExtraInfo) throws MappingException {
        if (fromDBObject == null) return null;

        return new BigInteger(fromDBObject.toString());
    }
}

import java.util.Properties;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.acmeair.entities.Booking;
import com.acmeair.entities.Flight;
import com.acmeair.entities.FlightSegment;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.WriteConcern;

public class DatastoreFactory {

	private static String mongourl = null;
	
	static {
		String vcapJSONString = System.getenv("VCAP_SERVICES");
		if (vcapJSONString != null) {
			System.out.println("Reading VCAP_SERVICES");
			Object jsonObject = JSONValue.parse(vcapJSONString);
			JSONObject json = (JSONObject)jsonObject;
			System.out.println("jsonObject = " + json.toJSONString());
			for (Object key: json.keySet())
			{
				if (((String)key).contains("mongo"))
				{
					System.out.println("Found mongo service:" +key);
					JSONArray mongoServiceArray = (JSONArray)json.get(key);
					JSONObject mongoService = (JSONObject) mongoServiceArray.get(0);
					JSONObject credentials = (JSONObject)mongoService.get("credentials");
					mongourl = (String)credentials.get("url");
					if (mongourl==null)
						mongourl= (String)credentials.get("uri");
					System.out.println("service url = " + mongourl);
					break;
				}
			}
		}


	}

	public static Datastore getDatastore(Datastore ds)
	{
		Datastore result =ds;
		
		if (mongourl!=null)
		{
			try{
				Properties prop = new Properties();
				prop.load(DatastoreFactory.class.getResource("/acmeair-mongo.properties").openStream());
				boolean fsync = new Boolean(prop.getProperty("mongo.fsync"));
				int w = new Integer(prop.getProperty("mongo.w"));
				int connectionsPerHost = new Integer(prop.getProperty("mongo.connectionsPerHost"));
				int threadsAllowedToBlockForConnectionMultiplier = new Integer(prop.getProperty("mongo.threadsAllowedToBlockForConnectionMultiplier"));
				
				// To match the local options
				MongoClientOptions.Builder builder = new MongoClientOptions.Builder()
					.writeConcern(new WriteConcern(w, 0, fsync))
					.connectionsPerHost(connectionsPerHost)
					.threadsAllowedToBlockForConnectionMultiplier(threadsAllowedToBlockForConnectionMultiplier);
			
				MongoClientURI mongoURI = new MongoClientURI(mongourl, builder);
				MongoClient mongo = new MongoClient(mongoURI);
				Morphia morphia = new Morphia();				
				result = morphia.createDatastore( mongo ,mongoURI.getDatabase());
				System.out.println("create mongo datastore with options:"+result.getMongo().getMongoClientOptions());
			}catch (Exception e)
			{
				e.printStackTrace();
			}
		}
    	// The converter is added for handing JDK 7 issue
	//	result.getMapper().getConverters().addConverter(new BigDecimalConverter());		
    //	result.getMapper().getConverters().addConverter(new BigIntegerConverter());
    	
		// Enable index
		result.ensureIndex(Booking.class, "pkey.customerId");
		result.ensureIndex(Flight.class, "pkey.flightSegmentId,scheduledDepartureTime");
		result.ensureIndex(FlightSegment.class, "originPort,destPort");

    	return result;
	}
}

import com.acmeair.AcmeAirConstants;

public interface MorphiaConstants extends AcmeAirConstants {

	public static final String JNDI_NAME = "mongo/acmeairMongodb";
	public static final String KEY = "morphia";
	public static final String KEY_DESCRIPTION = "mongoDB with morphia implementation";
	
	public static final String HOSTNAME = "mongohostname";
	public static final String PORT = "mongoport";
	public static final String DATABASE = "mongodatabase";
	
	
}

public interface BookingPK {

	public String getId();
	public String getCustomerId();
}

public interface FlightPK {

}

import java.io.Serializable;

import com.acmeair.entities.AirportCodeMapping;

public class AirportCodeMappingImpl implements AirportCodeMapping, Serializable{
	
	private static final long serialVersionUID = 1L;

	private String _id;
	private String airportName;
	
	public AirportCodeMappingImpl() {
	}
	
	public AirportCodeMappingImpl(String airportCode, String airportName) {
		this._id = airportCode;
		this.airportName = airportName;
	}
	
	public String getAirportCode() {
		return _id;
	}
	
	public void setAirportCode(String airportCode) {
		this._id = airportCode;
	}
	
	public String getAirportName() {
		return airportName;
	}
	
	public void setAirportName(String airportName) {
		this.airportName = airportName;
	}

}

import java.io.Serializable;
import java.util.*;

import com.acmeair.entities.Booking;
import com.acmeair.entities.Customer;
import com.acmeair.entities.Flight;


public class BookingImpl implements Booking, Serializable{
	
	private static final long serialVersionUID = 1L;

	private BookingPKImpl pkey;
	private FlightPKImpl flightKey;
	private Date dateOfBooking;
	private Customer customer;
	private Flight flight;
	
	public BookingImpl() {
	}
	
	public BookingImpl(String id, Date dateOfFlight, Customer customer, Flight flight) {
		this(id, dateOfFlight, customer, (FlightImpl)flight);
	}
	
	public BookingImpl(String id, Date dateOfFlight, Customer customer, FlightImpl flight) {
		this.pkey = new BookingPKImpl(customer.getUsername(),id);
		
		this.flightKey = flight.getPkey();
		this.dateOfBooking = dateOfFlight;
		this.customer = customer;
		this.flight = flight;
	}
	
	public BookingPKImpl getPkey() {
		return pkey;
	}

	// adding the method for index calculation
	public String getCustomerId() {
		return pkey.getCustomerId();
	}
	
	public void setPkey(BookingPKImpl pkey) {
		this.pkey = pkey;
	}

	public FlightPKImpl getFlightKey() {
		return flightKey;
	}

	public void setFlightKey(FlightPKImpl flightKey) {
		this.flightKey = flightKey;
	}

	
	public void setFlight(Flight flight) {
		this.flight = flight;
	}

	public Date getDateOfBooking() {
		return dateOfBooking;
	}
	
	public void setDateOfBooking(Date dateOfBooking) {
		this.dateOfBooking = dateOfBooking;
	}

	public Customer getCustomer() {
		return customer;
	}
	
	public void setCustomer(Customer customer) {
		this.customer = customer;
	}

	public Flight getFlight() {
		return flight;
	}


	@Override
	public String toString() {
		return "Booking [key=" + pkey + ", flightKey=" + flightKey
				+ ", dateOfBooking=" + dateOfBooking + ", customer=" + customer
				+ ", flight=" + flight + "]";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BookingImpl other = (BookingImpl) obj;
		if (customer == null) {
			if (other.customer != null)
				return false;
		} else if (!customer.equals(other.customer))
			return false;
		if (dateOfBooking == null) {
			if (other.dateOfBooking != null)
				return false;
		} else if (!dateOfBooking.equals(other.dateOfBooking))
			return false;
		if (flight == null) {
			if (other.flight != null)
				return false;
		} else if (!flight.equals(other.flight))
			return false;
		if (flightKey == null) {
			if (other.flightKey != null)
				return false;
		} else if (!flightKey.equals(other.flightKey))
			return false;
		if (pkey == null) {
			if (other.pkey != null)
				return false;
		} else if (!pkey.equals(other.pkey))
			return false;
		return true;
	}

	@Override
	public String getBookingId() {
		return pkey.getId();
	}

	@Override
	public String getFlightId() {
		return flight.getFlightId();		
	}

}

import java.io.Serializable;

import com.acmeair.entities.BookingPK;

import com.ibm.websphere.objectgrid.plugins.PartitionableKey;

public class BookingPKImpl implements BookingPK, Serializable, PartitionableKey {
	
	private static final long serialVersionUID = 1L;
	private String id;
	private String customerId;
	
	public BookingPKImpl() {
		super();
	}

	public BookingPKImpl(String customerId,String id) {
		super();
		this.id = id;
		this.customerId = customerId;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getCustomerId() {
		return customerId;
	}

	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}

	@Override
	public Object ibmGetPartition() {
		return this.customerId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((customerId == null) ? 0 : customerId.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BookingPKImpl other = (BookingPKImpl) obj;
		if (customerId == null) {
			if (other.customerId != null)
				return false;
		} else if (!customerId.equals(other.customerId))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "BookingPK [customerId=" + customerId + ",id=" + id + "]";
	}

	
}

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.acmeair.entities.CustomerAddress;

@XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
@XmlRootElement
public class CustomerAddressImpl implements CustomerAddress, Serializable{
	
	private static final long serialVersionUID = 1L;
	private String streetAddress1;
	private String streetAddress2;
	private String city;
	private String stateProvince;
	private String country;
	private String postalCode;

	public CustomerAddressImpl() {
	}
	
	public CustomerAddressImpl(String streetAddress1, String streetAddress2,
			String city, String stateProvince, String country, String postalCode) {
		super();
		this.streetAddress1 = streetAddress1;
		this.streetAddress2 = streetAddress2;
		this.city = city;
		this.stateProvince = stateProvince;
		this.country = country;
		this.postalCode = postalCode;
	}
	
	public String getStreetAddress1() {
		return streetAddress1;
	}
	public void setStreetAddress1(String streetAddress1) {
		this.streetAddress1 = streetAddress1;
	}
	public String getStreetAddress2() {
		return streetAddress2;
	}
	public void setStreetAddress2(String streetAddress2) {
		this.streetAddress2 = streetAddress2;
	}
	public String getCity() {
		return city;
	}
	public void setCity(String city) {
		this.city = city;
	}
	public String getStateProvince() {
		return stateProvince;
	}
	public void setStateProvince(String stateProvince) {
		this.stateProvince = stateProvince;
	}
	public String getCountry() {
		return country;
	}
	public void setCountry(String country) {
		this.country = country;
	}
	public String getPostalCode() {
		return postalCode;
	}
	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}
	@Override
	public String toString() {
		return "CustomerAddress [streetAddress1=" + streetAddress1
				+ ", streetAddress2=" + streetAddress2 + ", city=" + city
				+ ", stateProvince=" + stateProvince + ", country=" + country
				+ ", postalCode=" + postalCode + "]";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CustomerAddressImpl other = (CustomerAddressImpl) obj;
		if (city == null) {
			if (other.city != null)
				return false;
		} else if (!city.equals(other.city))
			return false;
		if (country == null) {
			if (other.country != null)
				return false;
		} else if (!country.equals(other.country))
			return false;
		if (postalCode == null) {
			if (other.postalCode != null)
				return false;
		} else if (!postalCode.equals(other.postalCode))
			return false;
		if (stateProvince == null) {
			if (other.stateProvince != null)
				return false;
		} else if (!stateProvince.equals(other.stateProvince))
			return false;
		if (streetAddress1 == null) {
			if (other.streetAddress1 != null)
				return false;
		} else if (!streetAddress1.equals(other.streetAddress1))
			return false;
		if (streetAddress2 == null) {
			if (other.streetAddress2 != null)
				return false;
		} else if (!streetAddress2.equals(other.streetAddress2))
			return false;
		return true;
	}
	
	
}

import java.io.Serializable;

import com.acmeair.entities.Customer;
import com.acmeair.entities.CustomerAddress;


public class CustomerImpl implements Customer, Serializable{
	
	private static final long serialVersionUID = 1L;

	private String _id;
	private String password;
	private MemberShipStatus status;
	private int total_miles;
	private int miles_ytd;

	private CustomerAddress address;
	private String phoneNumber;
	private PhoneType phoneNumberType;
	
	public CustomerImpl() {
	}
	
	public CustomerImpl(String username, String password, MemberShipStatus status, int total_miles, int miles_ytd, CustomerAddress address, String phoneNumber, PhoneType phoneNumberType) {
		this._id = username;
		this.password = password;
		this.status = status;
		this.total_miles = total_miles;
		this.miles_ytd = miles_ytd;
		this.address = address;
		this.phoneNumber = phoneNumber;
		this.phoneNumberType = phoneNumberType;
	}

	@Override
	public String getCustomerId() {
		return _id;
	}
	
	public String getUsername() {
		return _id;
	}
	
	public void setUsername(String username) {
		this._id = username;
	}
	
	public String getPassword() {
		return password;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	public MemberShipStatus getStatus() {
		return status;
	}
	
	public void setStatus(MemberShipStatus status) {
		this.status = status;
	}
	
	public int getTotal_miles() {
		return total_miles;
	}
	
	public void setTotal_miles(int total_miles) {
		this.total_miles = total_miles;
	}
	
	public int getMiles_ytd() {
		return miles_ytd;
	}
	
	public void setMiles_ytd(int miles_ytd) {
		this.miles_ytd = miles_ytd;
	}
	
	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public PhoneType getPhoneNumberType() {
		return phoneNumberType;
	}

	public void setPhoneNumberType(PhoneType phoneNumberType) {
		this.phoneNumberType = phoneNumberType;
	}

	public CustomerAddress getAddress() {
		return address;
	}

	public void setAddress(CustomerAddress address) {
		this.address = address;
	}

	@Override
	public String toString() {
		return "Customer [id=" + _id + ", password=" + password + ", status="
				+ status + ", total_miles=" + total_miles + ", miles_ytd="
				+ miles_ytd + ", address=" + address + ", phoneNumber="
				+ phoneNumber + ", phoneNumberType=" + phoneNumberType + "]";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CustomerImpl other = (CustomerImpl) obj;
		if (address == null) {
			if (other.address != null)
				return false;
		} else if (!address.equals(other.address))
			return false;
		if (_id == null) {
			if (other._id != null)
				return false;
		} else if (!_id.equals(other._id))
			return false;
		if (miles_ytd != other.miles_ytd)
			return false;
		if (password == null) {
			if (other.password != null)
				return false;
		} else if (!password.equals(other.password))
			return false;
		if (phoneNumber == null) {
			if (other.phoneNumber != null)
				return false;
		} else if (!phoneNumber.equals(other.phoneNumber))
			return false;
		if (phoneNumberType != other.phoneNumberType)
			return false;
		if (status != other.status)
			return false;
		if (total_miles != other.total_miles)
			return false;
		return true;
	}

}

import java.io.Serializable;
import java.util.Date;

import com.acmeair.entities.CustomerSession;

public class CustomerSessionImpl implements CustomerSession, Serializable {


	private static final long serialVersionUID = 1L;

	private String _id;
	private String customerid;
	private Date lastAccessedTime;
	private Date timeoutTime;
	
	public CustomerSessionImpl() {
	}

	public CustomerSessionImpl(String id, String customerid, Date lastAccessedTime,	Date timeoutTime) {
		this._id= id;
		this.customerid = customerid;
		this.lastAccessedTime = lastAccessedTime;
		this.timeoutTime = timeoutTime;
	}
	

	public String getId() {
		return _id;
	}

	public void setId(String id) {
		this._id = id;
	}

	public String getCustomerid() {
		return customerid;
	}

	public void setCustomerid(String customerid) {
		this.customerid = customerid;
	}

	public Date getLastAccessedTime() {
		return lastAccessedTime;
	}

	public void setLastAccessedTime(Date lastAccessedTime) {
		this.lastAccessedTime = lastAccessedTime;
	}

	public Date getTimeoutTime() {
		return timeoutTime;
	}

	public void setTimeoutTime(Date timeoutTime) {
		this.timeoutTime = timeoutTime;
	}

	@Override
	public String toString() {
		return "CustomerSession [id=" + _id + ", customerid=" + customerid
				+ ", lastAccessedTime=" + lastAccessedTime + ", timeoutTime="
				+ timeoutTime + "]";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CustomerSessionImpl other = (CustomerSessionImpl) obj;
		if (customerid == null) {
			if (other.customerid != null)
				return false;
		} else if (!customerid.equals(other.customerid))
			return false;
		if (_id == null) {
			if (other._id != null)
				return false;
		} else if (!_id.equals(other._id))
			return false;
		if (lastAccessedTime == null) {
			if (other.lastAccessedTime != null)
				return false;
		} else if (!lastAccessedTime.equals(other.lastAccessedTime))
			return false;
		if (timeoutTime == null) {
			if (other.timeoutTime != null)
				return false;
		} else if (!timeoutTime.equals(other.timeoutTime))
			return false;
		return true;
	}


	
}

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import com.acmeair.entities.Flight;
import com.acmeair.entities.FlightSegment;


public class FlightImpl implements Flight, Serializable{

	private static final long serialVersionUID = 1L;
				
	private FlightPKImpl pkey;
	private Date scheduledDepartureTime;
	private Date scheduledArrivalTime;
	private BigDecimal firstClassBaseCost;
	private BigDecimal economyClassBaseCost;
	private int numFirstClassSeats;
	private int numEconomyClassSeats;
	private String airplaneTypeId;
	
	private FlightSegment flightSegment;
	
	public FlightImpl() {
	}
	
	public FlightImpl(String id, String flightSegmentId,
			Date scheduledDepartureTime, Date scheduledArrivalTime,
			BigDecimal firstClassBaseCost, BigDecimal economyClassBaseCost,
			int numFirstClassSeats, int numEconomyClassSeats,
			String airplaneTypeId) {
		this.pkey = new FlightPKImpl(flightSegmentId,id);
		
		this.scheduledDepartureTime = scheduledDepartureTime;
		this.scheduledArrivalTime = scheduledArrivalTime;
		this.firstClassBaseCost = firstClassBaseCost;
		this.economyClassBaseCost = economyClassBaseCost;
		this.numFirstClassSeats = numFirstClassSeats;
		this.numEconomyClassSeats = numEconomyClassSeats;
		this.airplaneTypeId = airplaneTypeId;
	}

	public FlightPKImpl getPkey() {
		return pkey;
	}

	public void setPkey(FlightPKImpl pkey) {
		this.pkey = pkey;		
	}
	

	@Override
	public String getFlightId() {
		return pkey.getId();
	}

	@Override
	public void setFlightId(String id) {
		pkey.setId(id);		
	}

	
	// The method is needed for index calculation
	public String getFlightSegmentId()
	{
		return pkey.getFlightSegmentId();
	}
	
	public Date getScheduledDepartureTime() {
		return scheduledDepartureTime;
	}


	public void setScheduledDepartureTime(Date scheduledDepartureTime) {
		this.scheduledDepartureTime = scheduledDepartureTime;
	}


	public Date getScheduledArrivalTime() {
		return scheduledArrivalTime;
	}


	public void setScheduledArrivalTime(Date scheduledArrivalTime) {
		this.scheduledArrivalTime = scheduledArrivalTime;
	}


	public BigDecimal getFirstClassBaseCost() {
		return firstClassBaseCost;
	}


	public void setFirstClassBaseCost(BigDecimal firstClassBaseCost) {
		this.firstClassBaseCost = firstClassBaseCost;
	}


	public BigDecimal getEconomyClassBaseCost() {
		return economyClassBaseCost;
	}


	public void setEconomyClassBaseCost(BigDecimal economyClassBaseCost) {
		this.economyClassBaseCost = economyClassBaseCost;
	}


	public int getNumFirstClassSeats() {
		return numFirstClassSeats;
	}


	public void setNumFirstClassSeats(int numFirstClassSeats) {
		this.numFirstClassSeats = numFirstClassSeats;
	}


	public int getNumEconomyClassSeats() {
		return numEconomyClassSeats;
	}


	public void setNumEconomyClassSeats(int numEconomyClassSeats) {
		this.numEconomyClassSeats = numEconomyClassSeats;
	}


	public String getAirplaneTypeId() {
		return airplaneTypeId;
	}


	public void setAirplaneTypeId(String airplaneTypeId) {
		this.airplaneTypeId = airplaneTypeId;
	}


	public FlightSegment getFlightSegment() {
		return flightSegment;
	}

	public void setFlightSegment(FlightSegment flightSegment) {
		this.flightSegment = flightSegment;
	}

	@Override
	public String toString() {
		return "Flight key="+pkey
				+ ", scheduledDepartureTime=" + scheduledDepartureTime
				+ ", scheduledArrivalTime=" + scheduledArrivalTime
				+ ", firstClassBaseCost=" + firstClassBaseCost
				+ ", economyClassBaseCost=" + economyClassBaseCost
				+ ", numFirstClassSeats=" + numFirstClassSeats
				+ ", numEconomyClassSeats=" + numEconomyClassSeats
				+ ", airplaneTypeId=" + airplaneTypeId + "]";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FlightImpl other = (FlightImpl) obj;
		if (airplaneTypeId == null) {
			if (other.airplaneTypeId != null)
				return false;
		} else if (!airplaneTypeId.equals(other.airplaneTypeId))
			return false;
		if (economyClassBaseCost == null) {
			if (other.economyClassBaseCost != null)
				return false;
		} else if (!economyClassBaseCost.equals(other.economyClassBaseCost))
			return false;
		if (firstClassBaseCost == null) {
			if (other.firstClassBaseCost != null)
				return false;
		} else if (!firstClassBaseCost.equals(other.firstClassBaseCost))
			return false;
		if (flightSegment == null) {
			if (other.flightSegment != null)
				return false;
		} else if (!flightSegment.equals(other.flightSegment))
			return false;
		if (pkey == null) {
			if (other.pkey != null)
				return false;
		} else if (!pkey.equals(other.pkey))
			return false;
		if (numEconomyClassSeats != other.numEconomyClassSeats)
			return false;
		if (numFirstClassSeats != other.numFirstClassSeats)
			return false;
		if (scheduledArrivalTime == null) {
			if (other.scheduledArrivalTime != null)
				return false;
		} else if (!scheduledArrivalTime.equals(other.scheduledArrivalTime))
			return false;
		if (scheduledDepartureTime == null) {
			if (other.scheduledDepartureTime != null)
				return false;
		} else if (!scheduledDepartureTime.equals(other.scheduledDepartureTime))
			return false;
		return true;
	}


	/*
	public void setFlightSegmentId(String segmentId) {
		pkey.setFlightSegmentId(segmentId);
	}
	*/
	
}

import java.io.Serializable;
import com.acmeair.entities.FlightPK;
import com.ibm.websphere.objectgrid.plugins.PartitionableKey;


public class FlightPKImpl implements FlightPK, Serializable, PartitionableKey {
	
	private static final long serialVersionUID = 1L;
	
	private String id;
	private String flightSegmentId;
	
	public FlightPKImpl() {
		super();
	}

	public FlightPKImpl(String flightSegmentId,String id) {
		super();
		this.id = id;
		this.flightSegmentId = flightSegmentId;
	}
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getFlightSegmentId() {
		return flightSegmentId;
	}
	public void setFlightSegmentId(String flightSegmentId) {
		this.flightSegmentId = flightSegmentId;
	}
	
	@Override
	public Object ibmGetPartition() {
		return this.flightSegmentId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((flightSegmentId == null) ? 0 : flightSegmentId.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FlightPKImpl other = (FlightPKImpl) obj;
		if (flightSegmentId == null) {
			if (other.flightSegmentId != null)
				return false;
		} else if (!flightSegmentId.equals(other.flightSegmentId))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "FlightPK [flightSegmentId=" + flightSegmentId +",id=" + id+ "]";
	}
	
}

import java.io.Serializable;

import com.acmeair.entities.FlightSegment;

public class FlightSegmentImpl implements FlightSegment, Serializable{

	private static final long serialVersionUID = 1L;

	private String _id;
	private String originPort;
	private String destPort;
	private int miles;

	public FlightSegmentImpl() {
	}
	
	public FlightSegmentImpl(String flightName, String origPort, String destPort, int miles) {
		this._id = flightName;
		this.originPort = origPort;
		this.destPort = destPort;
		this.miles = miles;
	}
	
	public String getFlightName() {
		return _id;
	}

	public void setFlightName(String flightName) {
		this._id = flightName;
	}

	public String getOriginPort() {
		return originPort;
	}

	public void setOriginPort(String originPort) {
		this.originPort = originPort;
	}

	public String getDestPort() {
		return destPort;
	}

	public void setDestPort(String destPort) {
		this.destPort = destPort;
	}

	public int getMiles() {
		return miles;
	}

	public void setMiles(int miles) {
		this.miles = miles;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("FlightSegment ").append(_id).append(" originating from:\"").append(originPort).append("\" arriving at:\"").append(destPort).append("\"");
		return sb.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FlightSegmentImpl other = (FlightSegmentImpl) obj;
		if (destPort == null) {
			if (other.destPort != null)
				return false;
		} else if (!destPort.equals(other.destPort))
			return false;
		if (_id == null) {
			if (other._id != null)
				return false;
		} else if (!_id.equals(other._id))
			return false;
		if (miles != other.miles)
			return false;
		if (originPort == null) {
			if (other.originPort != null)
				return false;
		} else if (!originPort.equals(other.originPort))
			return false;
		return true;
	}
	
	
}

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.acmeair.entities.Booking;
import com.acmeair.entities.Customer;
import com.acmeair.entities.Flight;
import com.acmeair.service.BookingService;
import com.acmeair.service.CustomerService;
import com.acmeair.service.DataService;
import com.acmeair.service.FlightService;
import com.acmeair.service.KeyGenerator;
import com.acmeair.service.ServiceLocator;
import com.acmeair.wxs.WXSConstants;
import com.acmeair.wxs.entities.BookingImpl;
import com.acmeair.wxs.entities.BookingPKImpl;
import com.acmeair.wxs.entities.FlightPKImpl;
import com.acmeair.wxs.utils.WXSSessionManager;
import com.ibm.websphere.objectgrid.ObjectGrid;
import com.ibm.websphere.objectgrid.ObjectGridException;
import com.ibm.websphere.objectgrid.ObjectMap;
import com.ibm.websphere.objectgrid.Session;
import com.ibm.websphere.objectgrid.UndefinedMapException;
import com.ibm.websphere.objectgrid.plugins.TransactionCallbackException;
import com.ibm.websphere.objectgrid.plugins.index.MapIndex;


@DataService(name=WXSConstants.KEY,description=WXSConstants.KEY_DESCRIPTION)
public class BookingServiceImpl implements BookingService, WXSConstants  {
	
	private final static Logger logger = Logger.getLogger(BookingService.class.getName()); 
	
	private static String BOOKING_MAP_NAME="Booking";
	private static String BASE_BOOKING_MAP_NAME="Booking";

	private ObjectGrid og;
	
	@Inject
	private KeyGenerator keyGenerator;
	
	private FlightService flightService = ServiceLocator.instance().getService(FlightService.class);
	private CustomerService customerService = ServiceLocator.instance().getService(CustomerService.class);
	
	
	@PostConstruct
	private void initialization()  {
		try {
			og = WXSSessionManager.getSessionManager().getObjectGrid();
			BOOKING_MAP_NAME = BASE_BOOKING_MAP_NAME + WXSSessionManager.getSessionManager().getMapSuffix();
		} catch (ObjectGridException e) {
			logger.severe("Unable to retreive the ObjectGrid reference " + e.getMessage());
		}
	}
	
		
	public BookingPKImpl bookFlight(String customerId, FlightPKImpl flightId) {
		try{
			// We still delegate to the flight and customer service for the map access than getting the map instance directly
			Flight f = flightService.getFlightByFlightId(flightId.getId(), flightId.getFlightSegmentId());
			Customer c = customerService.getCustomerByUsername(customerId);
			
			BookingImpl newBooking = new BookingImpl(keyGenerator.generate().toString(), new Date(), c, f);
			BookingPKImpl key = newBooking.getPkey();
			
			//Session session = sessionManager.getObjectGridSession();
			Session session = og.getSession();
			ObjectMap bookingMap = session.getMap(BOOKING_MAP_NAME);
			@SuppressWarnings("unchecked")
			HashSet<Booking> bookingsByUser = (HashSet<Booking>)bookingMap.get(customerId);
			if (bookingsByUser == null) {
				bookingsByUser = new HashSet<Booking>();
			}
			if (bookingsByUser.contains(newBooking)) {
				throw new Exception("trying to book a duplicate booking");
			}
			bookingsByUser.add(newBooking);
			bookingMap.upsert(customerId, bookingsByUser);
			return key;
		}catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public String bookFlight(String customerId, String flightSegmentId, String id) {
		if(logger.isLoggable(Level.FINER))
			logger.finer("WXS booking service,  bookFlight with customerId = '"+ customerId+"', flightSegmentId = '"+ flightSegmentId + "',  and id = '" + id + "'");
		return bookFlight(customerId, new FlightPKImpl(flightSegmentId, id)).getId();
	}
	
	@Override
	public Booking getBooking(String user, String id) {
		
		try{
			//Session session = sessionManager.getObjectGridSession();
			Session session = og.getSession();
			ObjectMap bookingMap = session.getMap(BOOKING_MAP_NAME);
			
//			return (Booking)bookingMap.get(new BookingPK(user, id));
			@SuppressWarnings("unchecked")
			HashSet<BookingImpl> bookingsByUser = (HashSet<BookingImpl>)bookingMap.get(user);
			if (bookingsByUser == null) {
				return null;
			}
			for (BookingImpl b : bookingsByUser) {
				if (b.getPkey().getId().equals(id)) {
					return b;
				}
			}
			return null;

		}catch (Exception e)
		{
			throw new RuntimeException(e);
		}
			
	}

	@Override
	public void cancelBooking(String user, String id) {
		try{
			Session session = og.getSession();
			//Session session = sessionManager.getObjectGridSession();
			ObjectMap bookingMap = session.getMap(BOOKING_MAP_NAME);
			@SuppressWarnings("unchecked")
			HashSet<BookingImpl> bookingsByUser = (HashSet<BookingImpl>)bookingMap.get(user);
			if (bookingsByUser == null) {
				return;
			}
			boolean found = false;
			HashSet<Booking> newBookings = new HashSet<Booking>();
			for (BookingImpl b : bookingsByUser) {
				if (b.getPkey().getId().equals(id)) {
					found = true;
				}
				else {
					newBookings.add(b);
				}
			}
			
			if (found) {
				bookingMap.upsert(user, newBookings);
			}
		}catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}		
	
	@Override
	public List<Booking> getBookingsByUser(String user) {
		try{
			Session session = og.getSession();
			//Session session = sessionManager.getObjectGridSession();
	
			boolean startedTran = false;
			if (!session.isTransactionActive()) {
				startedTran = true;
				session.begin();
			}
			
			ObjectMap bookingMap = session.getMap(BOOKING_MAP_NAME);
			@SuppressWarnings("unchecked")
			HashSet<Booking> bookingsByUser = (HashSet<Booking>)bookingMap.get(user);
			if (bookingsByUser == null) {
				bookingsByUser = new HashSet<Booking>();
			}
			
			ArrayList<Booking> bookingsList = new ArrayList<Booking>();
			for (Booking b : bookingsByUser) {
				bookingsList.add(b);
			}
		
			if (startedTran)
				session.commit();
			
			return bookingsList;
		}catch (Exception e)
		{
			throw new RuntimeException(e);
		}
		
	}
	
	@Override
	public Long count () {
		try {
			Session session = og.getSession();
			ObjectMap objectMap = session.getMap(BOOKING_MAP_NAME);			
			MapIndex mapIndex = (MapIndex)objectMap.getIndex("com.ibm.ws.objectgrid.builtin.map.KeyIndex");			
			Iterator<?> keyIterator = mapIndex.findAll();
			Long result = 0L;
			while(keyIterator.hasNext()) {
				keyIterator.next(); 
				result++;
			}
			/*
			int partitions = og.getMap(BOOKING_MAP_NAME).getPartitionManager().getNumOfPartitions();
			Long result = 0L;
			ObjectQuery query = og.getSession().createObjectQuery("SELECT COUNT ( o ) FROM " + BOOKING_MAP_NAME + " o ");
			for(int i = 0; i<partitions;i++){
				query.setPartition(i);
				result += (Long) query.getSingleResult();
			}
			*/			
			return result;
		} catch (UndefinedMapException e) {
			e.printStackTrace();
		} catch (TransactionCallbackException e) {
			e.printStackTrace();
		} catch (ObjectGridException e) {
			e.printStackTrace();
		}	
		return -1L;
	}
}

import java.util.Date;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Default;
import javax.inject.Inject;

import com.acmeair.entities.Customer;
import com.acmeair.entities.Customer.MemberShipStatus;
import com.acmeair.entities.Customer.PhoneType;
import com.acmeair.entities.CustomerAddress;
import com.acmeair.entities.CustomerSession;
import com.acmeair.service.BookingService;
import com.acmeair.service.CustomerService;
import com.acmeair.service.DataService;
import com.acmeair.service.KeyGenerator;
import com.acmeair.wxs.WXSConstants;
import com.acmeair.wxs.entities.CustomerAddressImpl;
import com.acmeair.wxs.entities.CustomerImpl;
import com.acmeair.wxs.entities.CustomerSessionImpl;
import com.acmeair.wxs.utils.WXSSessionManager;
import com.ibm.websphere.objectgrid.ObjectGrid;
import com.ibm.websphere.objectgrid.ObjectGridException;
import com.ibm.websphere.objectgrid.ObjectMap;
import com.ibm.websphere.objectgrid.Session;
import com.ibm.websphere.objectgrid.UndefinedMapException;
import com.ibm.websphere.objectgrid.plugins.TransactionCallbackException;
import com.ibm.websphere.objectgrid.plugins.index.MapIndex;
import com.ibm.websphere.objectgrid.plugins.index.MapIndexPlugin;


@Default
@DataService(name=WXSConstants.KEY,description=WXSConstants.KEY_DESCRIPTION)
public class CustomerServiceImpl extends CustomerService implements WXSConstants{
	
	private static String BASE_CUSTOMER_MAP_NAME="Customer";
	private static String BASE_CUSTOMER_SESSION_MAP_NAME="CustomerSession";
	private static String CUSTOMER_MAP_NAME="Customer";
	private static String CUSTOMER_SESSION_MAP_NAME="CustomerSession";
	
		
	private final static Logger logger = Logger.getLogger(BookingService.class.getName()); 

	private ObjectGrid og;
	
	@Inject
	KeyGenerator keyGenerator;

	
	@PostConstruct
	private void initialization()  {
		try {
			og = WXSSessionManager.getSessionManager().getObjectGrid();
			CUSTOMER_MAP_NAME = BASE_CUSTOMER_MAP_NAME + WXSSessionManager.getSessionManager().getMapSuffix();
			CUSTOMER_SESSION_MAP_NAME = BASE_CUSTOMER_SESSION_MAP_NAME + WXSSessionManager.getSessionManager().getMapSuffix();
		} catch (ObjectGridException e) {
			logger.severe("Unable to retreive the ObjectGrid reference " + e.getMessage());
		}
	}
	
	@Override
	public Long count () {
		try {
			Session session = og.getSession();
			ObjectMap objectMap = session.getMap(CUSTOMER_MAP_NAME);			
			MapIndex mapIndex = (MapIndex)objectMap.getIndex(MapIndexPlugin.SYSTEM_KEY_INDEX_NAME);			
			Iterator<?> keyIterator = mapIndex.findAll();
			Long result = 0L;
			while(keyIterator.hasNext()) {
				keyIterator.next(); 
				result++;
			}
			/*
			int partitions = og.getMap(CUSTOMER_MAP_NAME).getPartitionManager().getNumOfPartitions();			
			ObjectQuery query = og.getSession().createObjectQuery("SELECT COUNT ( o ) FROM " + CUSTOMER_MAP_NAME + " o ");
			for(int i = 0; i<partitions;i++){
				query.setPartition(i);
				result += (Long) query.getSingleResult();
			}
			*/			
			return result;
		} catch (UndefinedMapException e) {
			e.printStackTrace();
		} catch (TransactionCallbackException e) {
			e.printStackTrace();
		} catch (ObjectGridException e) {
			e.printStackTrace();
		}		
		return -1L;
	}
	
	@Override
	public Long countSessions () {
		try {
			Session session = og.getSession();
			ObjectMap objectMap = session.getMap(CUSTOMER_SESSION_MAP_NAME);			
			MapIndex mapIndex = (MapIndex)objectMap.getIndex(MapIndexPlugin.SYSTEM_KEY_INDEX_NAME);			
			Iterator<?> keyIterator = mapIndex.findAll();
			Long result = 0L;
			while(keyIterator.hasNext()) {
				keyIterator.next(); 
				result++;
			}
			/*
			int partitions = og.getMap(CUSTOMER_SESSION_MAP_NAME).getPartitionManager().getNumOfPartitions();
			Long result = 0L;
			ObjectQuery query = og.getSession().createObjectQuery("SELECT COUNT ( o ) FROM " + CUSTOMER_SESSION_MAP_NAME + " o ");
			for(int i = 0; i<partitions;i++){
				query.setPartition(i);
				result += (Long) query.getSingleResult();
			}
			*/			
			return result;
		} catch (UndefinedMapException e) {
			e.printStackTrace();
		} catch (TransactionCallbackException e) {
			e.printStackTrace();
		} catch (ObjectGridException e) {
			e.printStackTrace();
		}	
		return -1L;
	}
	
	@Override
	public Customer createCustomer(String username, String password,
			MemberShipStatus status, int total_miles, int miles_ytd,
			String phoneNumber, PhoneType phoneNumberType,
			CustomerAddress address) {
		try{
			Customer customer = new CustomerImpl(username, password, status, total_miles, miles_ytd, address, phoneNumber, phoneNumberType);
			// Session session = sessionManager.getObjectGridSession();
			Session session = og.getSession();
			ObjectMap customerMap = session.getMap(CUSTOMER_MAP_NAME);
			customerMap.insert(customer.getUsername(), customer);
			return customer;
		}catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override 
	public CustomerAddress createAddress (String streetAddress1, String streetAddress2,
			String city, String stateProvince, String country, String postalCode){
		CustomerAddress address = new CustomerAddressImpl(streetAddress1, streetAddress2,
				 city, stateProvince,  country,  postalCode);
		return address;
	}
	
	@Override
	public Customer updateCustomer(Customer customer) {
		try{
			//Session session = sessionManager.getObjectGridSession();
			Session session = og.getSession();
			ObjectMap customerMap = session.getMap(CUSTOMER_MAP_NAME);
			customerMap.update(customer.getUsername(), customer);
			return customer;
		}catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected Customer getCustomer(String username) {
		try{
			//Session session = sessionManager.getObjectGridSession();
			Session session = og.getSession();
			ObjectMap customerMap = session.getMap(CUSTOMER_MAP_NAME);
			
			Customer c = (Customer) customerMap.get(username);
			return c;
		}catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	

	@Override
	protected CustomerSession getSession(String sessionid){
		try {
			Session session = og.getSession();
			ObjectMap customerSessionMap = session.getMap(CUSTOMER_SESSION_MAP_NAME);

			return (CustomerSession)customerSessionMap.get(sessionid);
		}catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	protected void removeSession(CustomerSession session){
		try {
			Session ogSession = og.getSession();
			ObjectMap customerSessionMap = ogSession.getMap(CUSTOMER_SESSION_MAP_NAME);

			customerSessionMap.remove(session.getId());
		}catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	protected CustomerSession createSession(String sessionId, String customerId, Date creation, Date expiration) {
		try{
			CustomerSession cSession = new CustomerSessionImpl(sessionId, customerId, creation, expiration);
			// Session session = sessionManager.getObjectGridSession();
			Session session = og.getSession();
			ObjectMap customerSessionMap = session.getMap(CUSTOMER_SESSION_MAP_NAME);
			customerSessionMap.insert(cSession.getId(), cSession);
			return cSession;
		}catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void invalidateSession(String sessionid) {
		try{
			//Session session = sessionManager.getObjectGridSession();
			Session session = og.getSession();
			ObjectMap customerSessionMap = session.getMap(CUSTOMER_SESSION_MAP_NAME);
			customerSessionMap.remove(sessionid);
		}catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.acmeair.entities.AirportCodeMapping;
import com.acmeair.entities.Flight;
import com.acmeair.entities.FlightSegment;
import com.acmeair.service.BookingService;
import com.acmeair.service.DataService;
import com.acmeair.service.FlightService;
import com.acmeair.service.KeyGenerator;
import com.acmeair.wxs.WXSConstants;
import com.acmeair.wxs.entities.AirportCodeMappingImpl;
import com.acmeair.wxs.entities.FlightImpl;
import com.acmeair.wxs.entities.FlightSegmentImpl;
import com.acmeair.wxs.utils.WXSSessionManager;
import com.ibm.websphere.objectgrid.ObjectGrid;
import com.ibm.websphere.objectgrid.ObjectGridException;
import com.ibm.websphere.objectgrid.ObjectMap;
import com.ibm.websphere.objectgrid.Session;
import com.ibm.websphere.objectgrid.UndefinedMapException;
import com.ibm.websphere.objectgrid.plugins.TransactionCallbackException;
import com.ibm.websphere.objectgrid.plugins.index.MapIndex;
import com.ibm.websphere.objectgrid.plugins.index.MapIndexPlugin;

@DataService(name=WXSConstants.KEY,description=WXSConstants.KEY_DESCRIPTION)
public class FlightServiceImpl extends FlightService implements  WXSConstants {

	private static String FLIGHT_MAP_NAME="Flight";
	private static String FLIGHT_SEGMENT_MAP_NAME="FlightSegment";
	private static String AIRPORT_CODE_MAPPING_MAP_NAME="AirportCodeMapping";
	
	private static String BASE_FLIGHT_MAP_NAME="Flight";
	private static String BASE_FLIGHT_SEGMENT_MAP_NAME="FlightSegment";
	private static String BASE_AIRPORT_CODE_MAPPING_MAP_NAME="AirportCodeMapping";
	
	private final static Logger logger = Logger.getLogger(BookingService.class.getName()); 
	
	private ObjectGrid og;
	
	@Inject
	KeyGenerator keyGenerator;
	
	
	@PostConstruct
	private void initialization()  {	
		try {
			og = WXSSessionManager.getSessionManager().getObjectGrid();
			FLIGHT_MAP_NAME = BASE_FLIGHT_MAP_NAME + WXSSessionManager.getSessionManager().getMapSuffix();
			FLIGHT_SEGMENT_MAP_NAME = BASE_FLIGHT_SEGMENT_MAP_NAME + WXSSessionManager.getSessionManager().getMapSuffix();
			AIRPORT_CODE_MAPPING_MAP_NAME = BASE_AIRPORT_CODE_MAPPING_MAP_NAME + WXSSessionManager.getSessionManager().getMapSuffix();
		} catch (ObjectGridException e) {
			logger.severe("Unable to retreive the ObjectGrid reference " + e.getMessage());
		}
	}
	
	@Override
	public Long countFlights() {
		try {
			Session session = og.getSession();
			ObjectMap objectMap = session.getMap(FLIGHT_MAP_NAME);			
			MapIndex mapIndex = (MapIndex)objectMap.getIndex(MapIndexPlugin.SYSTEM_KEY_INDEX_NAME);			
			Iterator<?> keyIterator = mapIndex.findAll();
			Long result = 0L;
			while(keyIterator.hasNext()) {
				keyIterator.next(); 
				result++;
			}
			/*
			int partitions = og.getMap(FLIGHT_MAP_NAME).getPartitionManager().getNumOfPartitions();
			Long result = 0L;
			ObjectQuery query = og.getSession().createObjectQuery("SELECT COUNT ( o ) FROM " + FLIGHT_MAP_NAME + " o ");
			for(int i = 0; i<partitions;i++){
				query.setPartition(i);
				result += (Long) query.getSingleResult();
			}
			*/
			return result;
		} catch (UndefinedMapException e) {
			e.printStackTrace();
		} catch (TransactionCallbackException e) {
			e.printStackTrace();
		} catch (ObjectGridException e) {
			e.printStackTrace();
		}
		return -1L;
	}
	
	@Override
	public Long countAirports() {
		try {
			Session session = og.getSession();
			ObjectMap objectMap = session.getMap(AIRPORT_CODE_MAPPING_MAP_NAME);			
			MapIndex mapIndex = (MapIndex)objectMap.getIndex(MapIndexPlugin.SYSTEM_KEY_INDEX_NAME);			
			Iterator<?> keyIterator = mapIndex.findAll();
			Long result = 0L;
			while(keyIterator.hasNext()) {
				keyIterator.next(); 
				result++;
			}
			return result;
		} catch (UndefinedMapException e) {
			e.printStackTrace();
		} catch (TransactionCallbackException e) {
			e.printStackTrace();
		} catch (ObjectGridException e) {
			e.printStackTrace();
		}
		return -1L;
	}
	
	@Override
	public Long countFlightSegments() {
		try {
			Session session = og.getSession();
			ObjectMap objectMap = session.getMap(FLIGHT_SEGMENT_MAP_NAME);			
			MapIndex mapIndex = (MapIndex)objectMap.getIndex(MapIndexPlugin.SYSTEM_KEY_INDEX_NAME);			
			Iterator<?> keyIterator = mapIndex.findAll();
			Long result = 0L;
			while(keyIterator.hasNext()) {
				keyIterator.next(); 
				result++;
			}
			/*
			int partitions = og.getMap(FLIGHT_SEGMENT_MAP_NAME).getPartitionManager().getNumOfPartitions();
			Long result = 0L;
			ObjectQuery query = og.getSession().createObjectQuery("SELECT COUNT ( o ) FROM " + FLIGHT_SEGMENT_MAP_NAME + " o ");
			for(int i = 0; i<partitions;i++){
				query.setPartition(i);
				result += (Long) query.getSingleResult();
			}
			*/			
			return result;

		} catch (UndefinedMapException e) {
			e.printStackTrace();
		} catch (TransactionCallbackException e) {
			e.printStackTrace();
		} catch (ObjectGridException e) {
			e.printStackTrace();
		}
		return -1L;
	}
	
	/*
	public Flight getFlightByFlightKey(FlightPK key) {
		try {
			Flight flight;
			flight = flightPKtoFlightCache.get(key);
			if (flight == null) {
				//Session session = sessionManager.getObjectGridSession();
				Session session = og.getSession();
				ObjectMap flightMap = session.getMap(FLIGHT_MAP_NAME);
				@SuppressWarnings("unchecked")
				HashSet<Flight> flightsBySegment = (HashSet<Flight>)flightMap.get(key.getFlightSegmentId());
				for (Flight f : flightsBySegment) {
					if (f.getPkey().getId().equals(key.getId())) {
						flightPKtoFlightCache.putIfAbsent(key, f);
						flight = f;
						break;
					}
				}
			}
			return flight;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	*/
	@Override
	protected Flight getFlight(String flightId, String flightSegmentId) {
		try {
			if(logger.isLoggable(Level.FINER))
				logger.finer("in WXS getFlight.  search for flightId = '" + flightId + "' and flightSegmentId = '"+flightSegmentId+"'");
			//Session session = sessionManager.getObjectGridSession();
			Session session = og.getSession();
			ObjectMap flightMap = session.getMap(FLIGHT_MAP_NAME);
			@SuppressWarnings("unchecked")
			HashSet<FlightImpl> flightsBySegment = (HashSet<FlightImpl>)flightMap.get(flightSegmentId);
			for (FlightImpl flight : flightsBySegment) {
				if (flight.getFlightId().equals(flightId)) {
					return flight;
				}
			}
			logger.warning("No matching flights found for flightId =" + flightId + " and flightSegment " + flightSegmentId);
			return null;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected  FlightSegment getFlightSegment(String fromAirport, String toAirport) {
		try {
		Session session = null;
//		boolean startedTran = false;
		//session = sessionManager.getObjectGridSession();
		session = og.getSession();
		FlightSegment segment = null;
/*		if (!session.isTransactionActive()) {
			startedTran = true;
			session.begin();
		}
		*/
		ObjectMap flightSegmentMap = session.getMap(FLIGHT_SEGMENT_MAP_NAME);
		@SuppressWarnings("unchecked")
		HashSet<FlightSegment> segmentsByOrigPort = (HashSet<FlightSegment>)flightSegmentMap.get(fromAirport);
		if (segmentsByOrigPort!=null) {
			for (FlightSegment fs : segmentsByOrigPort) {
				if (fs.getDestPort().equals(toAirport)) {
					segment = fs;
					return segment;
				}
			}
		}
		if (segment == null) {
			segment = new FlightSegmentImpl(); // put a sentinel value of a non-populated flightsegment
		}
//		if (startedTran)
//			session.commit();
		
		return segment;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	protected  List<Flight> getFlightBySegment(FlightSegment segment, Date deptDate){
		try {
		List<Flight> flights = new ArrayList<Flight>();
		Session session = null;
		boolean startedTran = false;
		if (session == null) {
			//session = sessionManager.getObjectGridSession();
			session = og.getSession();
			if (!session.isTransactionActive()) {
				startedTran = true;
				session.begin();
			}
		}				
		
		ObjectMap flightMap = session.getMap(FLIGHT_MAP_NAME);
		@SuppressWarnings("unchecked")
		HashSet<Flight> flightsBySegment = (HashSet<Flight>)flightMap.get(segment.getFlightName());
		if(deptDate != null){
			for (Flight f : flightsBySegment) {
				if (areDatesSameWithNoTime(f.getScheduledDepartureTime(), deptDate)) {
					f.setFlightSegment(segment);
					flights.add(f);
				}
			}
		} else {
			for (Flight f : flightsBySegment) {
				f.setFlightSegment(segment);
				flights.add(f);
			}
		}
		if (startedTran)
			session.commit();
		
		return flights;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	
	private static boolean areDatesSameWithNoTime(Date d1, Date d2) {
		return getDateWithNoTime(d1).equals(getDateWithNoTime(d2));
	}
	
	private static Date getDateWithNoTime(Date date) {
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		return c.getTime();
	}
	
	
	@Override
	public void storeAirportMapping(AirportCodeMapping mapping) {
		try{
			//Session session = sessionManager.getObjectGridSession();
			Session session = og.getSession();
			ObjectMap airportCodeMappingMap = session.getMap(AIRPORT_CODE_MAPPING_MAP_NAME);
			airportCodeMappingMap.upsert(mapping.getAirportCode(), mapping);
		}catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override 
	public AirportCodeMapping createAirportCodeMapping(String airportCode, String airportName){
		AirportCodeMapping acm = new AirportCodeMappingImpl(airportCode, airportName);
		return acm;
	}
	
	@Override
	public Flight createNewFlight(String flightSegmentId,
			Date scheduledDepartureTime, Date scheduledArrivalTime,
			BigDecimal firstClassBaseCost, BigDecimal economyClassBaseCost,
			int numFirstClassSeats, int numEconomyClassSeats,
			String airplaneTypeId) {
		try{
			String id = keyGenerator.generate().toString();
			Flight flight = new FlightImpl(id, flightSegmentId,
				scheduledDepartureTime, scheduledArrivalTime,
				firstClassBaseCost, economyClassBaseCost,
				numFirstClassSeats, numEconomyClassSeats,
				airplaneTypeId);
			//Session session = sessionManager.getObjectGridSession();
			Session session = og.getSession();
			ObjectMap flightMap = session.getMap(FLIGHT_MAP_NAME);
			//flightMap.insert(flight.getPkey(), flight);
			//return flight;
			@SuppressWarnings("unchecked")
			HashSet<Flight> flightsBySegment = (HashSet<Flight>)flightMap.get(flightSegmentId);
			if (flightsBySegment == null) {
				flightsBySegment = new HashSet<Flight>();
			}
			if (!flightsBySegment.contains(flight)) {
				flightsBySegment.add(flight);
				flightMap.upsert(flightSegmentId, flightsBySegment);
			}
			return flight;
		}catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public void storeFlightSegment(FlightSegment flightSeg) {
		try {
			//Session session = sessionManager.getObjectGridSession();
			Session session = og.getSession();
			ObjectMap flightSegmentMap = session.getMap(FLIGHT_SEGMENT_MAP_NAME);
			// TODO: Consider moving this to a ArrayList - List ??
			@SuppressWarnings("unchecked")
			HashSet<FlightSegment> segmentsByOrigPort = (HashSet<FlightSegment>)flightSegmentMap.get(flightSeg.getOriginPort());
			if (segmentsByOrigPort == null) {
				segmentsByOrigPort = new HashSet<FlightSegment>();
			}
			if (!segmentsByOrigPort.contains(flightSeg)) {
				segmentsByOrigPort.add(flightSeg);
				flightSegmentMap.upsert(flightSeg.getOriginPort(), segmentsByOrigPort);
			}
		
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override 
	public void storeFlightSegment(String flightName, String origPort, String destPort, int miles) {
		FlightSegment flightSeg = new FlightSegmentImpl(flightName, origPort, destPort, miles);
		storeFlightSegment(flightSeg);
	}



}

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import com.ibm.websphere.objectgrid.ObjectGridException;
import com.ibm.websphere.objectgrid.ObjectMap;
import com.ibm.websphere.objectgrid.Session;
import com.ibm.websphere.objectgrid.datagrid.MapGridAgent;
import com.ibm.websphere.objectgrid.plugins.io.dataobject.SerializedKey;

public class MapPutAllAgent implements MapGridAgent  {
	
	private static final long serialVersionUID = 1L;

	private static final Logger logger = Logger.getLogger(MapPutAllAgent.class.getName());
	
	private HashMap<Object, HashMap<Object,Object>>objectsToSave = null  ;

	public HashMap<Object, HashMap<Object,Object>> getObjectsToSave() {
		return objectsToSave;
	}

	public void setObjectsToSave(HashMap<Object,HashMap<Object,Object>> objectsToSave) {
		this.objectsToSave = objectsToSave;
	}

	//@Override
	public Object process(Session arg0, ObjectMap arg1, Object arg2) {
		// The key is the partition key, can be either the PK or when partition field is defined the partition field value
		try{
			Object key;
			// I need to find the real key as the hashmap is using the real key...
     		if( arg2 instanceof SerializedKey )
    		     key = ((SerializedKey)arg2).getObject();
    		else 
    		     key = arg2;     		
			
			HashMap<Object, Object> objectsForThePartition =  this.objectsToSave.get(key);
			
			if (objectsForThePartition==null)
				logger.info("ERROR!!! Can not get the objects for partiton key:"+arg2);
			else
			{
				Entry<Object, Object> entry;
				Object value;
				for (Iterator<Map.Entry<Object, Object>> itr = objectsForThePartition.entrySet().iterator(); itr.hasNext();)
				{
					entry = itr.next();
					key = entry.getKey();
					value = entry.getValue();
					
					logger.finer("Save using agent:"+key+",value:"+value);
					arg1.upsert(key, value);
				}
			}
		}catch (ObjectGridException e)
		{
			logger.info("Getting exception:"+e);
		}
		return arg2;	
	}

	//@Override
	public Map<Object, Object> processAllEntries(Session arg0, ObjectMap arg1) {
		return null; 
	}

}

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.acmeair.service.DataService;
import com.acmeair.service.TransactionService;
import com.acmeair.wxs.WXSConstants;
import com.ibm.websphere.objectgrid.BackingMap;
import com.ibm.websphere.objectgrid.ClientClusterContext;
import com.ibm.websphere.objectgrid.ObjectGrid;
import com.ibm.websphere.objectgrid.ObjectGridException;
import com.ibm.websphere.objectgrid.ObjectGridManager;
import com.ibm.websphere.objectgrid.ObjectGridManagerFactory;
import com.ibm.websphere.objectgrid.ObjectGridRuntimeException;
import com.ibm.websphere.objectgrid.Session;
import com.ibm.websphere.objectgrid.config.BackingMapConfiguration;
import com.ibm.websphere.objectgrid.config.ObjectGridConfigFactory;
import com.ibm.websphere.objectgrid.config.ObjectGridConfiguration;
import com.ibm.websphere.objectgrid.config.Plugin;
import com.ibm.websphere.objectgrid.config.PluginType;
import com.ibm.websphere.objectgrid.security.config.ClientSecurityConfiguration;
import com.ibm.websphere.objectgrid.security.config.ClientSecurityConfigurationFactory;
import com.ibm.websphere.objectgrid.security.plugins.CredentialGenerator;
import com.ibm.websphere.objectgrid.security.plugins.builtins.UserPasswordCredentialGenerator;
import com.ibm.websphere.objectgrid.spring.SpringLocalTxManager;

@DataService(name=WXSConstants.KEY,description=WXSConstants.KEY_DESCRIPTION)
public class WXSSessionManager implements TransactionService, WXSConstants{
	
		private static final String GRID_CONNECT_LOOKUP_KEY = "com.acmeair.service.wxs.gridConnect";
		private static final String GRID_NAME_LOOKUP_KEY = "com.acmeair.service.wxs.gridName";
		private static final String GRID_DISABLE_NEAR_CACHE_NAME_LOOKUP_KEY = "com.acmeair.service.wxs.disableNearCacheName";
		private static final String GRID_PARTITION_FIELD_NAME_LOOKUP_KEY = "com.acmeair.service.wxs.partitionFieldName";
		private static final Logger logger = Logger.getLogger(WXSSessionManager.class.getName());
		private static final String SPLIT_COMMA = "\\s*,\\s*";
		private static final String SPLIT_COLON = "\\s*:\\s*";		
	
		private String gridConnectString;
		private String gridUsername = null;
		private String gridPassword = null;
		private String gridName = "Grid";
		private boolean integrateWithWASTransactions = false;
		private String disableNearCacheNameString;
		private String[] disableNearCacheNames = null;
		private String partitionFieldNameString;
		private HashMap<String, String> partitionFieldNames = null; // For now to make it simple to only support one partition field
		private SpringLocalTxManager txManager;
        private String mapSuffix = "";
		private AtomicReference<ObjectGrid> sharedGrid = new AtomicReference<ObjectGrid>();
		private static AtomicReference<WXSSessionManager> connectionManager = new AtomicReference<WXSSessionManager>();
		
		
		public static WXSSessionManager getSessionManager() {
			if (connectionManager.get() == null) {
				synchronized (connectionManager) {
					if (connectionManager.get() == null) {
						connectionManager.set(new WXSSessionManager());
					}
				}
			}
			return connectionManager.get();
		}	
		
		
		private WXSSessionManager(){
			ObjectGrid og = null;
			
			try {
				InitialContext ic = new InitialContext();			
				og = (ObjectGrid) ic.lookup(JNDI_NAME);
				
			} catch (NamingException e) {
				logger.warning("Unable to look up the ObjectGrid reference " + e.getMessage());
			}
			if(og != null) {
				sharedGrid.set(og);
			} else {				
				initialization();				
			}
			
		}
		
		
		private void initialization()  {		
			
			
			String vcapJSONString = System.getenv("VCAP_SERVICES");
			if (vcapJSONString != null) {
				logger.info("Reading VCAP_SERVICES");
				Object jsonObject = JSONValue.parse(vcapJSONString);
				logger.info("jsonObject = " + ((JSONObject)jsonObject).toJSONString());
				JSONObject json = (JSONObject)jsonObject;
				String key;
				for (Object k: json.keySet())
				{
					key = (String ) k;
					if (key.startsWith("ElasticCaching")||key.startsWith("DataCache"))
					{
						JSONArray elasticCachingServiceArray = (JSONArray)json.get(key);
						JSONObject elasticCachingService = (JSONObject)elasticCachingServiceArray.get(0); 
						JSONObject credentials = (JSONObject)elasticCachingService.get("credentials");
						String username = (String)credentials.get("username");
						setGridUsername(username);
						String password = (String)credentials.get("password");
						setGridPassword(password);
						String gridName = (String)credentials.get("gridName");
						String catalogEndPoint = (String)credentials.get("catalogEndPoint");
						logger.info("username = " + username + "; password = " + password + "; gridName =  " + gridName + "; catalogEndpoint = " + catalogEndPoint);
						setGridConnectString(catalogEndPoint);
						setGridName(gridName);
						break;
					}
				}
				setMapSuffix(".NONE.O");
			} else {
				logger.info("Creating the WXS Client connection. Looking up host and port information" );
				gridName = lookup(GRID_NAME_LOOKUP_KEY);
				if(gridName == null){
					gridName = "AcmeGrid";
				}

				gridConnectString = lookup(GRID_CONNECT_LOOKUP_KEY);
				if(gridConnectString == null){							
					gridConnectString = "127.0.0.1:2809";
					logger.info("Using default grid connection setting of " + gridConnectString);
				}

				setDisableNearCacheNameString(lookup(GRID_DISABLE_NEAR_CACHE_NAME_LOOKUP_KEY));
				setPartitionFieldNameString(lookup(GRID_PARTITION_FIELD_NAME_LOOKUP_KEY));

			}
			
			
			if(getDisableNearCacheNameString() == null){
				setDisableNearCacheNameString("Flight,FlightSegment,AirportCodeMapping,CustomerSession,Booking,Customer");
				logger.info("Using default disableNearCacheNameString value of " + disableNearCacheNameString);
			}
			
			if(getPartitionFieldNameString() == null){
				setPartitionFieldNameString("Flight:pk.flightSegmentId,FlightSegment:originPort,Booking:pk.customerId");
				logger.info("Using default partitionFieldNameString value of " + partitionFieldNameString);
			}
			
			if (!integrateWithWASTransactions && txManager!=null) // Using Spring TX if WAS TX is not enabled
			{
				logger.info("Session will be created from SpringLocalTxManager w/ tx support.");
			}else
			{
				txManager=null;
				logger.info("Session will be created from ObjectGrid directly w/o tx support.");
			}
			
			
			try {
				prepareForTransaction();
			} catch (ObjectGridException e) {
				e.printStackTrace();
			} 
		}	
		
		private String lookup (String key){
			String value = null;
			String lookup = key.replace('.', '/');
			javax.naming.Context context = null;
			javax.naming.Context envContext = null;
			try {
				context = new javax.naming.InitialContext();
				envContext = (javax.naming.Context) context.lookup("java:comp/env");
				if (envContext != null)
					value = (String) envContext.lookup(lookup);
			} catch (NamingException e) {  }
			
			if (value != null) {
				logger.info("JNDI Found " + lookup + " : " + value);
			}
			else if (context != null) {
				try {
					value = (String) context.lookup(lookup);
					if (value != null)
						logger.info("JNDI Found " +lookup + " : " + value);
				} catch (NamingException e) {	}
			}

			if (value == null) {
				value = System.getProperty(key);
				if (value != null)
					logger.info("Found " + key + " in jvm property : " + value);
				else {
					value = System.getenv(key);
					if (value != null)
						logger.info("Found "+key+" in environment property : " + value);
				}
			}
			return value;
		}
		
	    /**
	     * Connect to a remote ObjectGrid
	     * @param cep the catalog server end points in the form: <host>:<port>
	     * @param gridName the name of the ObjectGrid to connect to that is managed by the Catalog Service
	     * @return a client ObjectGrid connection.
	     */
		private ObjectGrid connectClient(String cep, String gridName, boolean integrateWithWASTransactions,String[] disableNearCacheNames) {
			try {
				ObjectGrid gridToReturn = sharedGrid.get();
				if (gridToReturn == null) {
					synchronized(sharedGrid) {
						if (sharedGrid.get() == null) {
							ObjectGridManager ogm = ObjectGridManagerFactory.getObjectGridManager();
							ObjectGridConfiguration ogConfig = ObjectGridConfigFactory.createObjectGridConfiguration(gridName);
							if (integrateWithWASTransactions) // Using WAS Transactions as Highest Priority
							{

								Plugin trans = ObjectGridConfigFactory.createPlugin(PluginType.TRANSACTION_CALLBACK,
										"com.ibm.websphere.objectgrid.plugins.builtins.WebSphereTransactionCallback");
								ogConfig.addPlugin(trans);
							}
							if (disableNearCacheNames!=null) {
								String mapNames[] = disableNearCacheNames;
								for (String mName : mapNames) {									
									BackingMapConfiguration bmc = ObjectGridConfigFactory.createBackingMapConfiguration(mName);
									bmc.setNearCacheEnabled(false);
									ogConfig.addBackingMapConfiguration(bmc);
								}
							}					
													
							ClientClusterContext ccc = null;
							if (gridUsername != null) {
								ClientSecurityConfiguration clientSC = ClientSecurityConfigurationFactory.getClientSecurityConfiguration();
								clientSC.setSecurityEnabled(true);
								CredentialGenerator credGen = new UserPasswordCredentialGenerator(gridUsername, gridPassword);
								clientSC.setCredentialGenerator(credGen);
								ccc = ogm.connect(cep, clientSC, null);
							}
							else {
								ccc = ogm.connect(cep, null, null);
							}

							ObjectGrid grid = ObjectGridManagerFactory.getObjectGridManager().getObjectGrid(ccc, gridName, ogConfig);
							sharedGrid.compareAndSet(null, grid);
							gridToReturn = grid;
							logger.info("Create instance of Grid: " + gridToReturn);
						}else{
							gridToReturn = sharedGrid.get(); 
						}
					}
				}
				return gridToReturn;
			} catch (Exception e) {
				throw new ObjectGridRuntimeException("Unable to connect to catalog server at endpoints:" + cep,	e);
			}
		}
		public String getMapSuffix(){
			return mapSuffix;
		}
		
		public void setMapSuffix(String suffix){
			this.mapSuffix = suffix;
		}
		
		public String getGridConnectString() {
			return gridConnectString;
		}
		public void setGridConnectString(String gridConnectString) {
			this.gridConnectString = gridConnectString;
		}
		public String getGridName() {
			return gridName;
		}
		public void setGridName(String gridName) {
			this.gridName = gridName;
		}
		public String getGridUsername() {
			return gridUsername;
		}

		public void setGridUsername(String gridUsername) {
			this.gridUsername = gridUsername;
		}

		public String getGridPassword() {
			return gridPassword;
		}

		public void setGridPassword(String gridPassword) {
			this.gridPassword = gridPassword;
		}

		public boolean isIntegrateWithWASTransactions() {
			return integrateWithWASTransactions;
		}
		public void setIntegrateWithWASTransactions(boolean integrateWithWASTransactions) {
			this.integrateWithWASTransactions = integrateWithWASTransactions;
		}

		public String getDisableNearCacheNameString() {
			return disableNearCacheNameString;
		}
		public void setDisableNearCacheNameString(String disableNearCacheNameString) {
			this.disableNearCacheNameString = disableNearCacheNameString;
			if (disableNearCacheNameString ==null || disableNearCacheNameString.length()==0)
				disableNearCacheNames =null;
			else
				disableNearCacheNames = disableNearCacheNameString.split(SPLIT_COMMA);
		}
		
		public String getPartitionFieldNameString() {
			return partitionFieldNameString;
		}
		public void setPartitionFieldNameString(String partitionFieldNameString) {
			this.partitionFieldNameString = partitionFieldNameString;
			// In the form of <MapName>:<PartitionFieldName>,<MapName>:<PartitionFieldName>
			if (partitionFieldNameString ==null || partitionFieldNameString.length()==0)
				partitionFieldNames =null;
			else
			{
				String[] maps = partitionFieldNameString.split(SPLIT_COMMA);
				partitionFieldNames = new HashMap<String, String>();
				String[] mapDef;
				for (int i=0; i<maps.length; i++)
				{
					mapDef = maps[i].split(SPLIT_COLON);
					partitionFieldNames.put(mapDef[0], mapDef[1]);
				}
			}
			
		}
		public String getPartitionFieldName(String mapName) {
			if (partitionFieldNames == null)
				return null;
			return partitionFieldNames.get(mapName);
		}
		
		public SpringLocalTxManager getTxManager() {
			return txManager;
		}
		
		public void setTxManager(SpringLocalTxManager txManager) {
			logger.finer("txManager:"+txManager);
			this.txManager = txManager;
		}
		
		
		// This method needs to be called by the client from its thread before triggering a service with @Transactional annotation
		public void prepareForTransaction() throws ObjectGridException
		{
			ObjectGrid grid = this.getObjectGrid();
			if (txManager!=null)
				txManager.setObjectGridForThread(grid);
		}
		
		// Helper function
		public ObjectGrid getObjectGrid() throws ObjectGridException {
			ObjectGrid grid = connectClient(this.gridConnectString, this.gridName, this.integrateWithWASTransactions, this.disableNearCacheNames);
			return grid;
		}

		public Session getObjectGridSession() throws ObjectGridException {
			Session result;
			ObjectGrid grid = getObjectGrid();
			if (txManager!=null)
				result= txManager.getSession();
			else
				result = grid.getSession();
			
//			this.log.debug("Got session:"+ result);
			return result;
		}
		
		public BackingMap getBackingMap(String mapName)throws ObjectGridException
		{
			return this.getObjectGrid().getMap(mapName);			
		}
		
		
}

public interface WXSConstants {

	public static final String JNDI_NAME = "wxs/acmeair";
	public static final String KEY = "wxs";
	public static final String KEY_DESCRIPTION = "eXtreme Scale";
	
}

