package org.red5.server.net.http.stream;

import static org.red5.server.net.http.message.HTTPHeaders.Names.CONTENT_TYPE;
import static org.red5.server.net.http.message.HTTPHeaders.Names.HOST;

import org.red5.server.api.scope.IScope;
import org.red5.server.net.http.BaseHTTPService;
import org.red5.server.net.http.IHTTPService;
import org.red5.server.net.http.message.HTTPRequest;
import org.red5.server.net.http.message.HTTPResponse;

public class HTTPShutDownService extends BaseHTTPService implements IHTTPService {

	@Override
	public void setHeader(HTTPResponse resp) {
		
		resp.addHeader("Accept-Ranges", "bytes");
		resp.addHeader(CONTENT_TYPE, "text/html");
		resp.addHeader("Pragma", "no-cache"); 
		resp.setHeader("Cache-Control", "no-cache");
	}

	@Override
	public void handleRequest(HTTPRequest req, HTTPResponse resp, IScope scope)
			throws Exception {
		
		setHeader(resp);
		commitResponse(req, resp, null);
		String host = req.getHeader(HOST);
		if(host.startsWith("127.0.0.1") || host.startsWith("localhost")) {
			System.exit(-1);
			log.info("stream server shutdown!");
		} else {
			log.info("stream server dont shutdown, host = {}", host);
		}		
	}
}
