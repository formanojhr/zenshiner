package com.mashery.tools.httpconcurrentclient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.impl.Log4JLogger;
import org.apache.http.entity.AbstractHttpEntity;

/**
 * Streaming mock for the httpclient side load. Streams for number of seconds of
 * the size of the latch over an interval of 1 second
 * 
 * @author mramakr1
 * 
 */
public class StreamingRequestEntity extends AbstractHttpEntity {
	private final int count;
	private final CountDownLatch streamingLatch;
	private static Log4JLogger logger = new Log4JLogger(
			StreamingRequestEntity.class.getName());
	private volatile ScheduledExecutorService exec;
	private final ScheduledExecutorService scheduledExec;

	public StreamingRequestEntity(int count, CountDownLatch latch,
			Log4JLogger logger) {
		super();
		this.count = count;
		this.streamingLatch = latch;
		this.scheduledExec = Executors.newScheduledThreadPool((int) latch
				.getCount());
		this.exec = Executors.newScheduledThreadPool(1);
		this.logger = logger;
	}

	@Override
	public boolean isRepeatable() {
		return true;
	}

	@Override
	public long getContentLength() {
		// TODO Auto-generated method stub
		return this.streamingLatch.getCount();
	}

	@Override
	public InputStream getContent() throws IOException, IllegalStateException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public void consumeContent() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void writeTo(final OutputStream out) throws IOException {
		logger.info("Streaming the request body now....");
		int chunkSize = 16000;// intialiaze to 16 KB
		final byte[] buf = new byte[chunkSize];
		Arrays.fill(buf, (byte) ('A' & 0xff));
		logger.info("starting to stream request body for "
				+ this.streamingLatch.getCount() + " (sec)......" + "of chunk size(bytes): " + chunkSize);
		// produce request in another thread pool
		exec.execute(new Runnable() {
			@Override
			public void run() {
				try {
					streamingLatch.countDown();
					long count = StreamingRequestEntity.this.streamingLatch
							.getCount();
					if (count-- > 0) {
						logger.info(".");
						// non-final chunk
						out.write(buf);

						try {
							// reschedule this task for another run later in 1
							// second
							logger.debug("Scheduling a streaming for the next ");
							StreamingRequestEntity.this.scheduledExec.schedule(
									this, 1L, TimeUnit.MILLISECONDS);// Submit
																		// the
																		// streaming
																		// task
																		// with
						} catch (RejectedExecutionException e) {
						}
						return;
					}
					// final chunk
					if (count < 0) {
//						out.write(buf);
						logger.info("Flushing the bytes now.....");
						out.flush();
					}
				} catch (IOException e) {
					logger.error(
							"I/O exception occured while streming the request body.",
							e);
				}
			}
		});
		try {
			streamingLatch.await();
		} catch (InterruptedException e) {
			logger.info("Streaming Interrupted: returning....");
			return;
		} finally {
			try {
				out.close();
			} catch (IOException e) {
				logger.error("Error closing output stream", e);
			}
		}

	}

	@Override
	public boolean isStreaming() {
		// TODO Auto-generated method stub
		return true;
	}

}
