
package com.mashery.tools.httpconcurrentclient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kohsuke.args4j.CmdLineParser;

import com.mashery.tools.common.MyOptions;

/**
 * Created with IntelliJ IDEA.
 * User: mramakr1
 * Date: 10/14/13
 * Time: 2:32 PM
 * THe main test class for starting the ConcurrentLoadTest.
 */
public class ProxyConcurrentHttpLoadTester {

	public static void main(String[] args) throws IOException {
		MyOptions optionsBean = new MyOptions();
		CmdLineParser parser = new CmdLineParser(optionsBean);
		try {
			// parse the command line arguments
			parser.parseArgument(args);
			Map<String, String> headerMap = parseHeaderString(optionsBean
					.getHeaders());
			if (optionsBean.getBody() != null) {
				Map<String, String> bodyMap = parseBodyString(optionsBean
						.getBody());
			}
			ConcurrentHttpRequestExecutor concurrentHttpRequestExecutor = new ConcurrentHttpRequestExecutor(
					optionsBean.getUri(), optionsBean.getNumOfRequests(),
					optionsBean.getWaitTime(), headerMap,
					optionsBean.getWaitBatch(), optionsBean.getNumBatch(),
					optionsBean.getTestDuration(),
					optionsBean.getRequestMethod(), optionsBean.getBody(), optionsBean.getRequestStreamingTime(), optionsBean.getResponseStreamingTime());
			concurrentHttpRequestExecutor.testConcurrentHttpRequests();
		} catch (Exception e) {
			System.err.println(e.getMessage());
			System.err
			.println("java -jar myprogram.jar [options...] arguments...");
			parser.printUsage(System.err);
			return;
		}
	}

	/**
	 * Split a string like below into relevant map of strings.
	 * E.g: parameter=value&also=another,"Content-Type": "application/x-zip""
	 * @param bodyString
	 * @return
	 */
	public static Map<String, String> parseBodyString(String body){
		List<String> bodyList = new ArrayList<String>(Arrays.asList(body.split("&")));
		Map<String,String> headerMap= new HashMap<String, String>();
		for (String header:bodyList) {
			String[] headerPair=header.split("=");
			headerMap.put(headerPair[0],headerPair[1]);
		}
		return headerMap;
	}


	/**
	 * Split a string like below into relevant map of strings.
	 * E.g: "Host": "lowcalapi.localproxy.example.com","Content-Type": "application/x-zip""
	 * @param headerString
	 * @return
	 */
	public static Map<String, String>  parseHeaderString(String headerString){
		List<String> headerList = new ArrayList<String>(Arrays.asList(headerString.split(",")));
		Map<String,String> headerMap= new HashMap<String, String>();
		for (String header:headerList) {
			String[] headerPair=header.split(":");
			headerMap.put(headerPair[0],headerPair[1]);
		}
		return headerMap;

	}


}
