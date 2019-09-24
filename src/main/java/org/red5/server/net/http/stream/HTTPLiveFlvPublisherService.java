package org.red5.server.net.http.stream;

import java.io.IOException;
import java.util.Set;

import org.apache.mina.core.session.IdleStatus;
import org.red5.conf.ExtConfiguration;
import org.red5.server.api.Red5;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.service.IStreamSecurityService;
import org.red5.server.api.stream.IClientStream;
import org.red5.server.api.stream.IStreamPublishSecurity;
import org.red5.server.api.stream.support.SimplePlayItem;
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
		String method = req.getMethod().toString();
		//不知道为什么必须post,我又给加了一个get
		if((!REQUEST_GET_METHOD.equalsIgnoreCase(method) && !REQUEST_POST_METHOD.equalsIgnoreCase(method)) || segments.length < 2) {
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
		
		//不知道要干嘛用
		HTTPPushProxyStream pushStream = new HTTPPushProxyStream(scope,streamName); 
		pushStream.start();
		conn.setAttribute("pushStream", pushStream);
		conn.write(resp);
		
		/*HTTPConnectionConsumer consumer = new HTTPConnectionConsumer(conn);		
		
		conn.getHttpSession().getConfig().setReaderIdleTime(0);
		conn.getHttpSession().getConfig().setWriterIdleTime(0);
		conn.getHttpSession().getConfig().setIdleTime(IdleStatus.WRITER_IDLE, ExtConfiguration.HTTP_IDLE);
		
		consumer.getConnection().connect(scope);
		CustomSingleItemSubStream stream = new CustomSingleItemSubStream(scope, consumer);
		SimplePlayItem playItem = SimplePlayItem.build(streamName, -2000, -1);
		stream.setPlayItem(playItem);
		stream.start();
		
		conn.setAttribute("consumer", consumer);
		conn.setAttribute("stream", stream);
		
		setHeader(resp);
		conn.write(resp);
		
		try {
			stream.play();
		} catch (IOException e) {
			log.info("http play faile {}", e.getMessage());
			sendError(req, resp, HTTPResponseStatus.BAD_REQUEST);
			stream.stop();
			return;
		}
		
		if (stream.isFailure()) {
			log.info("stream {} http play faile", streamName);
			sendError(req, resp, HTTPResponseStatus.BAD_REQUEST);
			stream.stop();
			return;
		}*/
	}
}
