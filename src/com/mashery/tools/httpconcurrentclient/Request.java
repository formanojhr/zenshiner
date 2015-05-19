package com.mashery.tools.httpconcurrentclient;



import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.impl.Log4JLogger;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import com.mashery.tools.common.StatsCalculator;
import com.mashery.tools.httpconcurrentclient.ConcurrentHttpRequestExecutor.methodType;


public class Request implements Callable<Response> {
	private final String url;
	private final Map<String, String> headerMap;
	private HttpResponse response;
	private static int count=0;
	private static Log4JLogger logger = new Log4JLogger(ConcurrentHttpRequestExecutor.class.getName());
	private final int batchNum;
	private final int requestNum;
	private final  StatsCalculator calculator;
	private final ConcurrentHttpRequestExecutor.methodType method;
	private final String body;
	private CountDownLatch streamingLatch;
	private final CountDownLatch mainLatch;
	private volatile ScheduledExecutorService exec;
	private volatile boolean destroyed;
	private final ScheduledExecutorService scheduledExec;
	private final int streamingDelay=0;


	private long responseStreamingDelay=0;

	public Request(String url, Log4JLogger logger, int batchNum, int requestNum, StatsCalculator calculator, methodType method, String body, CountDownLatch mainLatch, long streamingDelay2, long responseStreamingDelay) {
		this.url = url;
		this.headerMap=new HashMap<String, String>();
		this.batchNum=batchNum;
		this.requestNum=requestNum;
		this.calculator=calculator;
		this.method=method;
		this.body=body;
		exec=Executors.newScheduledThreadPool(1);
		this.scheduledExec = Executors
				.newScheduledThreadPool(10);
		this.streamingLatch=new CountDownLatch((int) streamingDelay2 );
		this.mainLatch=mainLatch;
		this.responseStreamingDelay = responseStreamingDelay;
	}

	@Override
	public Response call() throws Exception {
		DefaultHttpClient client = new DefaultHttpClient();
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		HttpClient httpClient = new DefaultHttpClient();
		Long startRequestTime = System.currentTimeMillis();
		try {
			HttpRequestBase request = null;
			if (method.equals(methodType.GET)) {
				request = new HttpGet(url);
				addHeadersToGetRequest(request);
				// if there response streaming delay is given then start the get request with a streaming response handler.
				if (this.responseStreamingDelay > 0) {
					this.streamingLatch = new CountDownLatch(1);
					StreamingResponseHandler<String> responseHandler = new StreamingResponseHandler<String>(
							streamingLatch, logger, (int) this.responseStreamingDelay);
					// Input streaming of the http response
					String responseBody = client.execute(request,
							responseHandler);
					streamingLatch.await();
				} else {
					response = client.execute(request);//Call for the get request
				}
			} else if ((method.equals(methodType.POST))) {
				request = new HttpPost(url);
				HttpPost httppost = new HttpPost(url);
				httppost.setEntity(new StreamingRequestEntity(this.streamingDelay,
						streamingLatch, logger));
				addHeadersToPostRequest(httppost);
				httppost.addHeader("Content-Type", "text/plain");
				// if (this.responseStreamingDelay > 0) {
				// this.streamingLatch = new CountDownLatch(1);
				// StreamingResponseHandler<String> responseHandler = new StreamingResponseHandler<String>(
				// streamingLatch, logger, (int) this.responseStreamingDelay);
				// // Input streaming of the http response
				// String response = client.execute(request,
				// responseHandler);
				// logger.info("Total artificial streaming slowness in reading response created:"
				// + responseHandler.getStreamintSlownessTime()
				// + "  (ms)");
				// streamingLatch.await();
				// }
				// else{
				response = client.execute(httppost);
				logger.info("Waiting on streaming latch...");
				streamingLatch.await();
				logger.info("Released streaming latch...");
			}
			// }


			HttpEntity resEntityGet = response.getEntity();
			count++;
			long latency = System.currentTimeMillis() - startRequestTime;
			calculator.addValue(latency);
			System.out.println("Latency:"+ latency);
			if (resEntityGet != null) {
				// do something with the response
				logger.info(dateFormat.format(date) + "  "
						+ "Got response: Batch no.:  " + this.batchNum
						+ " Request no.:" + this.requestNum + " "
						+ EntityUtils.toString(resEntityGet) + "in:" + latency
						+ "(ms)");
				logger.info("Status code:    "+ response.getStatusLine());
			}
		} catch (Exception e) {
			logger.debug(e.getCause());
			logger.debug(e.toString());
			mainLatch.countDown();

		}
		mainLatch.countDown();// count down for the request
		return new Response(response.getEntity().getContent());
	}


	private void addHeadersToGetRequest(HttpRequestBase request) {
		// add all possible headers
		Iterator it = headerMap.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, String> headerEntry = (Map.Entry) it.next();
			request.addHeader(headerEntry.getKey(), headerEntry.getValue());
		}

	}

	private void addHeadersToPostRequest(HttpPost httppost) {
		// add all possible headers
		Iterator it = headerMap.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, String> headerEntry = (Map.Entry) it.next();
			httppost.addHeader(headerEntry.getKey(), headerEntry.getValue());
		}
		httppost.addHeader("Content-Type", "text/plain");
		httppost.addHeader("X-Buffer-Size", String.valueOf(1024));
	}

	/**
	 * Helper to schedule the streaming bytes to the http server request for
	 * configured number of seconds
	 * 
	 * @param out
	 */
	private void writeRequesBody(final ByteArrayOutputStream out) {
		int chunkSize = 1024;
		final byte[] buf = new byte[chunkSize];
		Arrays.fill(buf, (byte) ('.' & 0xff));
		logger.info(".");
		// produce request in another thread pool
		exec.execute(new Runnable() {
			@Override
			public void run() {
				try {
					long count = Request.this.streamingLatch.getCount();
					if (count-- > 0) {
						// non-final chunk
						out.write(buf);
						out.flush();
						streamingLatch.countDown();// decrement count down latch
						try {
							// reschedule this task for another run later in 1
							// second
							logger.info("Scheduling a streaming for the next ");
							Request.this.scheduledExec.schedule(this, 1000L,
									TimeUnit.MILLISECONDS);// Submit the
							// streaming task
							// with
						} catch (RejectedExecutionException e) {
						}
						return;
					}
					// final chunk
					if (count >= 0) {
						out.write(buf);
						out.flush();
					}
				} catch (IOException e) {
					logger.error(
							"I/O exception occured while streming the request body.",
							e);
				}
			}
		});
	}


	public void addHeader(String key, String value){
		headerMap.put(key, value);
	}
}