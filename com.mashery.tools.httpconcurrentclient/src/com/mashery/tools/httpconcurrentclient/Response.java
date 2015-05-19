package com.mashery.tools.httpconcurrentclient;

import java.io.InputStream;

/**
 * Created with IntelliJ IDEA.
 * User: mramakr1
 * Date: 10/9/13
 * Time: 5:31 PM
 * To change this template use File | Settings | File Templates.
 */
public class Response {
    private InputStream body;

    public Response(InputStream body) {
        this.body = body;
    }

    public InputStream getBody() {
        return body;
    }
}
