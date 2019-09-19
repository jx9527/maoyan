package org.red5.server.net.http.stream;

import java.util.Set;

import org.red5.server.api.Red5;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IClientStream;
import org.red5.server.api.stream.IStreamPublishSecurity;
import org.red5.server.api.stream.IStreamSecurityService;
import org.red5.server.net.http.BaseHTTPService;
import org.red5.server.net.http.HTTPMinaConnection;
import org.red5.server.net.http.IHTTPService;
import org.red5.server.net.http.message.HTTPRequest;
import org.red5.server.net.http.message.HTTPResponse;
import org.red5.server.net.http.message.HTTPResponseStatus;
import org.red5.server.net.proxy.HTTPPushProxyStream;
import org.red5.server.util.ScopeUtils;

public class HTTPLiveFlvPublisherService extends BaseHTTPService implements IHTTPService {

	@Override
	public void setHeader(HTTPResponse resp) {

		resp.addHeader("Pragma", "no-cache"); 
		resp.setHeader("Cache-Control", "no-cache");
	}

	@Override
	public void handleRequest(HTTPRequest req, HTTPResponse resp, IScope scope) throws Exception {

		HTTPMinaConnection conn = (HTTPMinaConnection)Red5.getConnectionLocal();
		String path = req.getPath().substring(1);
		String[] segments = path.split("/");
		if(!REQUEST_POST_METHOD.equalsIgnoreCase(req.getMethod().toString()) || segments.length < 2) {
			sendError(req, resp, HTTPResponseStatus.BAD_REQUEST);
			return;
		}
		String streamName = segments[1];
		
		// publish security
		IStreamSecurityService security = (IStreamSecurityService) ScopeUtils.getScopeService(scope, IStreamSecurityService.class);
		if (security != null) {
			Set<IStreamPublishSecurity> handlers = security.getStreamPublishSecurity();
			for (IStreamPublishSecurity handler : handlers) {
				if (!handler.isPublishAllowed(scope, streamName, IClientStream.MODE_LIVE)) {
					sendError(req, resp, HTTPResponseStatus.BAD_REQUEST);
					return;
				}
			}
		}
		
		HTTPPushProxyStream pushStream = new HTTPPushProxyStream(streamName);
		pushStream.setScope(scope);
		pushStream.start();
		conn.setAttribute("pushStream", pushStream);
		conn.write(resp);
	}
}
