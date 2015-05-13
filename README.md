# zenshiner
This project's objective is to contain tools that could stress test any HttpServer with ConcurrentHttpRequests in a non blocking* way and responses are captured and also test corner cases for a http server like response/ request streaming.

Why I wrote this tool ?
This project's objective is to contain tools that could stress test any HttpServer with ConcurrentHttpRequests in a non blocking way and responses are captured as they come back to understand characteristics of a Http Server.

The HttP server application that I was working on was seeing lots of symptoms of I/O errors which looked like after-effect symptoms of timeouts on request response interruptions on the threads on which the request response cycle was carried on. This was specifically happening  for target backends which were exhibiting certain slow latencies from the target backend routed by the HttpServer .
1. Concurrent Http Request: To simulate this, I had a bunch of patterns of concurrent requests simulated through the tool's command line. So, I added different options for patterns of requests:
2. number of concurrent requests targeted towards a URI
batches of number of concurrent requests which can shoot concurrent requests with timed waits between the batches.
concurrent requests testing over a fixed duration of time
3. Slow Input/Output streaming: Next, I extended the tool to support input and output request/response streaming which simulates slowness based on command line parameters. 
4. Command line: All of these options are configurable with command like parameters.

Caution: This tool is yet to be non-blocking in some ways. The threading model is as below:
1. Each request is created and called in a separate thread pool.
2. The request calls are created as a future and called by exectutor pool. 
3. The threads in the pool as and when they become available make the http calls through ApacheHttpClient library as a syncronous call.
Since the threads making the calls to the http server are blocking until response comes back with a timeout based on the Apache Http client library, a slow HttpServer can make tool less concurrent.
