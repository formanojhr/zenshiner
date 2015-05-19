package com.mashery.tools.httpconcurrentclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.impl.Log4JLogger;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;



public class StreamingResponseHandler<T> implements ResponseHandler<T> {
	protected final BufferFactory factory;
	private static long DEFAULT_READ_SLOWNESS=1; //(in ms)
	private long streamintSlownessTime;
	private static Log4JLogger logger;
	private volatile ScheduledExecutorService exec;
	private final ScheduledExecutorService scheduledExec;
	private final  CountDownLatch streamingLatch;
	private final AtomicInteger count;
	private final AtomicLong readTime;
	private final long responseStreamintSlowness;

	
	public long getStreamintSlownessTime() {
		return streamintSlownessTime;
	}

	public void setStreamintSlownessTime(long streamintSlownessTime) {
		this.streamintSlownessTime = streamintSlownessTime;
	}

	public StreamingResponseHandler(CountDownLatch latch, Log4JLogger logger2, int responseStreamingDelay) {
		this.factory = new BufferFactory.DefaultBufferFactory(4096) ;
		this.scheduledExec = Executors.newScheduledThreadPool((10));
		this.exec = Executors.newScheduledThreadPool(2);
		streamingLatch = latch;
		count = new AtomicInteger(responseStreamingDelay);
		readTime = new AtomicLong(0);
		this.logger = logger2;		
		this.responseStreamintSlowness = 10
				;
	}

	@Override
	public T handleResponse(HttpResponse response)
			throws ClientProtocolException, IOException {

		BufferedReader reader = new BufferedReader(new InputStreamReader(
				response.getEntity().getContent()));
		InputStream in = response.getEntity().getContent();
		byte[] buf = null;
		StringBuffer buffer = new StringBuffer();
		String line = null;
		this.count.set(0);
		long start=0;
		long sleepTime = 0;
		try {
			//
			buffer=respond(in);
			start = System.currentTimeMillis();
			streamingLatch.await();


		} catch (Exception e) {
			// TODO: handle exception
		}
		finally{
			in.close();
			//				(this.count.get() * this.DEFAULT_READ_SLOWNESS);
			setStreamintSlownessTime(System.currentTimeMillis() - start - this.readTime.get());
			logger.info("The total latency on the input response streaming read is:" + ((count.get() * responseStreamintSlowness) - readTime.get())/1000 +"s");
			
			this.exec.shutdownNow();
			this.scheduledExec.shutdownNow();
		}

		return (T) buffer.toString();
	}


	public StringBuffer respond(final InputStream in) throws IOException {
		logger.info("Streaming the response body now....");
		final StringBuffer buffer = new StringBuffer();

		logger.info("starting to stream response body for "
				+ this.count + " (sec)......");
		// produce request in another thread pool
		exec.execute(new Runnable() {
			@Override
			public void run() {
				try {
					byte[] buf = null;
					count.getAndIncrement();

					buf = factory.getBuffer();
					logger.info(".");
					long readStartTime = System.currentTimeMillis();
					if ((in.read(buf)) != -1) {
						readTime.getAndAdd(System.currentTimeMillis()- readStartTime);
						count.getAndAdd(1);
						StreamingResponseHandler.this.readTime.getAndAdd( System.currentTimeMillis() - readStartTime);
						try {
							// reschedule this task for another run later in 1
							// second
							logger.debug("Scheduling a streaming for the next ");
							StreamingResponseHandler.this.scheduledExec
							.schedule(
									this,
									responseStreamintSlowness,
									TimeUnit.MILLISECONDS);// Submit
							// the
							// streaming
							// task
							// with
						} catch (RejectedExecutionException e) {
						}
						return;
					} else {//After all reading from the stream is done, release the latch
						StreamingResponseHandler.this.streamingLatch
						.countDown();
						return;
					}
				} catch (IOException e) {
					logger.error(
							"I/O exception occured while streming the request body.",
							e);
				}
			}
		});
		return buffer;
	}




}
