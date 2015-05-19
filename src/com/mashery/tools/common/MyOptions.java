package com.mashery.tools.common;
import org.kohsuke.args4j.Option;

/**
 * @author mramakr1
 * Date: 10/14/13
 * Time: 2:34 PM
 */
public class MyOptions {

	@Option(name="-n",usage="The number of concurrent http clients to be run")
	private int numOfRequests;      // The number of concurrent requests needed

	@Option(name="-u",usage="server URI the tool client should hit")
	private String uri;   // Client URI to hit from the tool

	@Option(name="-w",usage="Wait time between the requests in millisecs")
	private long waitTime;  // Wait time between the concurrent requests

	@Option(name="-h",usage="Comma separated header values. E.g: \"Host\": \"localapi.localproxy.example.com\",\"Content-Type\": \"application/x-zip\"" )
	private String headers;  // Wait time between the concurrent requests

	@Option(name="-t",usage="Wait time between batch requests in millisec E.g: 500 secs" )
	private long waitBatch;  // Wait time between the concurrent requests

	@Option(name="-b",usage="Number of batch requests i.e. Number of time the requested number of requests are run in a batch." )
	private int numBatch;  // Wait time between the concurrent requests

	@Option(name="-D",usage="Time in seconds for which the test will be run.")
	private int testDuration;  // Wait time between the concurrent requests

	@Option(name="-R",usage="Request type could be GET or POST" )
	private String requestMethod;  // Method type

	@Option(name="-B",usage="Body of the POST request in the form parameter=value&also=another" )
	private String body;  // Body of the request

	@Option(name="-S", usage="Streaming time for body for POST in secs")
	private long requestStreamingTime;

	@Option(name="-T", usage="Streaming time response for GET request")
	private long responseStreamingTime;

	public long getResponseStreamingTime() {
		return responseStreamingTime;
	}

	public void setResponseStreamingTime(long responseStreamingTime) {
		this.responseStreamingTime = responseStreamingTime;
	}

	public long getRequestStreamingTime() {
		return requestStreamingTime;
	}

	public void setRequestStreamingTime(long requestStreamingTime) {
		this.requestStreamingTime = requestStreamingTime;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getRequestMethod() {
		return requestMethod;
	}

	public void setRequestMethod(String requestMethod) {
		this.requestMethod = requestMethod;
	}

	public int getNumOfRequests() {
		return numOfRequests;
	}

	public void setNumOfRequests(int numOfRequests) {
		this.numOfRequests = numOfRequests;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public long getWaitTime() {
		return waitTime;
	}

	public void setWaitTime(long waitTime) {
		this.waitTime = waitTime;
	}

	public String getHeaders() {
		return headers;
	}

	public void setHeaders(String headers) {
		this.headers = headers;
	}

	public long getWaitBatch() {
		return waitBatch;
	}

	public void setWaitBatch(long waitBatch) {
		this.waitBatch = waitBatch;
	}

	public int getNumBatch() {
		return numBatch;
	}

	public void setNumBatch(int numBatch) {
		this.numBatch = numBatch;
	}

	public int getTestDuration() {
		return testDuration;
	}

	public void setTestDuration(int testDuration) {
		this.testDuration = testDuration;
	}
}
