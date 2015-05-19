package com.mashery.tools.httpconcurrentclient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.logging.impl.Log4JLogger;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.Priority;

import com.mashery.tools.common.StatsCalculator;

/**
 * Created with IntelliJ IDEA.
 * @author mramakr1
 * Date: 10/9/13
 * Time: 5:32 PM
 * Objective: Stress test any HttpServer with Concurrent Http client connections.
 * This tool would attempt to create non blocking concurrent http requests to any http server URI.
 * This uses Future callable semantic to simulate non blocking concurrent http client connections. The test method spawns as many future task
 * of the Request (callable) and submits them to a Thread Pool Executor.
 */

public class ConcurrentHttpRequestExecutor{
	static int batchCount=0;
	private final static String URL_STRING = "http://localhost:8080/wwo.php?delay=25&api_key=testdevcustomerkey";
	private static List<Future<Response>> httpRequestFutureList;
	private final String proxyURI;
	private final int numberOfConcurrentRequests;
	private final long waitBetweenRequests;
	private final Map<String, String> headerMap;
	private final long waitBetweenBatchRequest;
	private int numBatch;
	private static Log4JLogger logger = new Log4JLogger(ConcurrentHttpRequestExecutor.class.getName());
	private volatile  StatsCalculator calculator;
	private final ScheduledExecutorService timeoutExecutor = Executors.newSingleThreadScheduledExecutor();
	private static final TimeUnit DEFAULT_TIMEOUT_UNIT = TimeUnit.SECONDS;
	private static final long DEFAULT_TIMEOUT_DELAY = 6000L;       // Defaulted to 1 min
	private volatile AtomicBoolean timeout;
	private volatile long testDuration;
	private String bodyString;
	public static enum methodType{GET,POST};
	private methodType method;
	private final long requestStreamingDelay;
	private final CountDownLatch requestFinishedLatch;
	private final long responseStreamingDelay;

	public ConcurrentHttpRequestExecutor(String URL,
			int numberOfConcurrentRequests, long waitBetweenRequests,
			Map<String, String> headerMap, long waitBetweenRequest,
			int numBatch, long testDuration, String method, String bodyString,
			long requestStreamingTime, long responseStreamingDelay) throws IllegalArgumentException {
		if (numBatch == 0 || numberOfConcurrentRequests == 0) {
			throw new IllegalArgumentException(
					"The value for number of batch value or number of request value is zero. The minimun value for them is 1..");
		}
		// Validate the request HTTP method type
		if (method.contentEquals("GET"))
			this.method = methodType.GET;
		else if (method.contentEquals("POST"))
			this.method = methodType.POST;
		else
			throw new UnknownError("Unknown method type: " + method);

		this.bodyString=bodyString;
		this.waitBetweenBatchRequest = waitBetweenRequest;

		if (URL != null) {
			this.proxyURI = URL;
		}
		else
			throw new IllegalArgumentException("URI parameter is a NULL");

		this.numberOfConcurrentRequests=numberOfConcurrentRequests;
		this.waitBetweenRequests=waitBetweenRequests;
		this.headerMap=headerMap;
		if(numBatch > 0) {
			this.numBatch=numBatch;
		}
		else {
			this.numBatch=1;
		}
		calculator = new StatsCalculator();

		this.testDuration= testDuration  ;

		//        else{
		//            this.testDuration=DEFAULT_TIMEOUT_DELAY;
		//        }
		if (this.testDuration > 0) {
			TimeoutImpl timeoutImpl = new TimeoutImpl(this.testDuration);
		}
		timeout= new AtomicBoolean(false);
		this.requestStreamingDelay=requestStreamingTime;
		this.requestFinishedLatch=new CountDownLatch(this.numBatch * this.numberOfConcurrentRequests);
		this.responseStreamingDelay=responseStreamingDelay;
	}

	public void testConcurrentHttpRequests() {
		// Have one (or more) threads ready to do the async tasks. Do this during startup of your app.
		ExecutorService executor = Executors.newFixedThreadPool(numberOfConcurrentRequests);
		Future<Response> response;
		httpRequestFutureList = Collections.synchronizedList(new ArrayList<Future<Response>>());

		//Thread
		try {
			ConsoleAppender console = new ConsoleAppender(); //create appender
			//configure the appender
			String PATTERN = "%d [%p|%c|%C{1}] %m%n";
			console.setLayout(new PatternLayout(PATTERN));
			console.setThreshold(Priority.INFO);
			console.activateOptions();
			//add appender to any Logger (here is root)
			logger.getLogger().addAppender(console);
			// PropertyConfigurator.configure("log4j.properties") ;
			//PropertyConfigurator.configure(getClass().getResource("log4j.properties"));
			FileAppender fh;
			fh = new FileAppender();
			fh.setName("ConcurrentHttpRequestExecutor");
			fh.setFile("/etc/log/ConcurrentHttpRequestExecutor.log");
			fh.setLayout(new PatternLayout("%d %-5p [%c{1}] %m%n"));
			fh.setThreshold(Priority.DEBUG);
			fh.setAppend(true);
			fh.activateOptions();

			logger.getLogger().addAppender(fh);
			logger.info("Starting concurrent requests to proxy with Config URI:" + this.proxyURI);

			while ((timeout.get() != true)) {
				for (int j = 0; j < this.numBatch; j++) {
					logger.debug("Batch started No:" + batchCount++);
					for (int i = 0; i < this.numberOfConcurrentRequests; i++) {
						if (timeout.get() != true) {//create a future callable http request here
							Request httpRequest = new Request(this.proxyURI,
									logger, j + 1, i + 1, calculator, method,
									bodyString, this.requestFinishedLatch, this.requestStreamingDelay, this.responseStreamingDelay);
							// add all possible headers
							Iterator it = headerMap.entrySet().iterator();
							while (it.hasNext()) {
								Map.Entry<String, String> headerEntry = (Map.Entry) it
										.next();
								httpRequest.addHeader(headerEntry.getKey(),
										headerEntry.getValue());
							}
							Thread.sleep(waitBetweenRequests);
							// Fire a request.
							response = executor.submit(httpRequest);

							httpRequestFutureList.add(response);
							logger.debug("Submitted Request: " + i);

							logger.debug("Batch ended No:" + batchCount);
							Thread.sleep(waitBetweenBatchRequest);
						} else {
							break; // timeout break loop
						}

					}
				}
				if (this.testDuration == 0) {//If the test duration is 0, quit restarting concurrent requests for the test duration and wait for requests to complete.
					break;
				}
			}
			if (this.testDuration == 0) {//If test duration is zero wait on countdown latch to be released
				this.requestFinishedLatch.await();
			}
			printStatsAndEndTest();
			executor.shutdownNow();
		} catch (Exception e) {
			logger.debug("Exception Occurred:" + e.getCause());
			executor.shutdownNow();
		}

		logger.info("****************All TESTS COMPLETE**************");
		// Shutdown the threads during shutdown of your app.
		System.exit(0);
	}


	public void setBodyString(String bodyString) {
		this.bodyString = bodyString;
	}



	private void printStatsAndEndTest() {
		logger.info("Tests...Printing Stats...");
		logger.info("URI.."+ this.proxyURI);
		logger.info("Request HTTP Method Type: "+ this.method);
		logger.info("Request Streaming latecy if given: "+ this.requestStreamingDelay  + "(sec)");
		logger.info("Concurrency: " + numberOfConcurrentRequests);
		logger.info("Batches: " + numBatch);
		logger.info("TestDuration: " + testDuration + " (" + DEFAULT_TIMEOUT_UNIT + ")");
		logger.info("***************Printing Statistics for Test************************");
		logger.info("Median:  " + calculator.getMedian() + "(" + TimeUnit.MILLISECONDS + ")");
		logger.info("Mean:  " + calculator.getMean() + "(" + TimeUnit.MILLISECONDS + ")");
		logger.info("WorstCase:  " + calculator.getMax() + "(" + TimeUnit.MILLISECONDS + ")");
		logger.info("BestCase:  " + calculator.getMin() + "(" + TimeUnit.MILLISECONDS + ")");
		logger.info("Total Requests:  " + calculator.getTotal());
		logger.info( "Request Rate: " + (calculator.getTotal()/ testDuration));
	}

	/**
	 * This could be a possible way of asynchronously poll for the future responses. Not used for now.
	 *  NOTE: Not used currrently.
	 */
	private class HttpResponsePoller implements Runnable {

		@Override
		public void run() {
			//To change body of implemented methods use File | Settings | File Templates.

			int i=1;
			for(Future<Response> responseFuture:httpRequestFutureList){
				if(responseFuture.isDone()){
					try {
						Response response=responseFuture.get();
						httpRequestFutureList.remove(responseFuture);
						System.out.println("Got response from request No:" +i );
						i++;
					} catch (InterruptedException e) {
						e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
					} catch (ExecutionException e) {
						e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
					}
				}
			}
		}
	}


	/**
	 * This class to timeout when time is runout
	 */
	private class TimeoutImpl implements Runnable {
		private final Thread runningThread = Thread.currentThread();
		private volatile ScheduledFuture<?> future;

		public TimeoutImpl(long timeoutDelay) {
			future = timeoutExecutor.schedule(this, timeoutDelay, DEFAULT_TIMEOUT_UNIT);

		}

		@Override
		public void run() {
			timeout.getAndSet(true);
		}


		public boolean cancel() {
			return future.cancel(false);
		}

	}

}
