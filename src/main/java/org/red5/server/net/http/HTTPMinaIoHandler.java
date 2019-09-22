package org.red5.server.net.http;

import static org.red5.server.net.http.message.HTTPResponseStatus.OK;
import static org.red5.server.net.http.message.HTTPVersion.HTTP_1_1;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.red5.conf.ExtConfiguration;
import org.red5.server.api.Red5;
import org.red5.server.api.ScopeUtils;
import org.red5.server.net.http.codec.HTTPCodecFactory;
import org.red5.server.net.http.message.DefaultHttpResponse;
import org.red5.server.net.http.message.HTTPChunk;
import org.red5.server.net.http.message.HTTPRequest;
import org.red5.server.net.http.message.HTTPResponse;
import org.red5.server.scope.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP Mina IO Handler
 * @author pengliren
 *
 */
public class HTTPMinaIoHandler extends IoHandlerAdapter {
	
	protected static Logger log = LoggerFactory.getLogger(HTTPMinaIoHandler.class);	
	
	private HTTPConnManager httpConnMgr = HTTPConnManager.getInstance();
	
	@Override
	public void sessionCreated(IoSession session) throws Exception {
		
		log.debug("HTTP Session Created id = {}", session.getId());
		session.getConfig().setIdleTime(IdleStatus.READER_IDLE, ExtConfiguration.HTTP_IDLE);
		session.getFilterChain().addLast("protocolFilter", new ProtocolCodecFilter(new HTTPCodecFactory()));
		
		// create http connection
		HTTPMinaConnection conn = new HTTPMinaConnection(session);
		httpConnMgr.addConnection(conn, session.getId());
		// add to session
		session.setAttribute(HTTPMinaConnection.HTTP_CONNECTION_KEY, conn);
	}
	
	@Override
	public void messageReceived(IoSession session, Object message) throws Exception {
		
		HTTPMinaConnection conn = (HTTPMinaConnection)session.getAttribute(HTTPMinaConnection.HTTP_CONNECTION_KEY);
		Red5.setConnectionLocal(conn);
		if(message instanceof HTTPRequest) {	
			HTTPRequest req = (HTTPRequest)message;
			HTTPResponse resp = new DefaultHttpResponse(HTTP_1_1, OK);
			String path = req.getUri().substring(1);	
			String noAppPath = req.getUri();
			Scope scope = null;
			// get scope
			String[] segments = path.split("/");
			if(segments.length > 0) {
				scope = ScopeUtils.getScope(segments[0]);
				if(scope == null) { // root scope?
					scope = ScopeUtils.getScope("root");
				} else {
					noAppPath = req.getUri().replaceFirst(String.format("/%s", scope.getName()), "");
				}
			} else {// root scope
				scope = ScopeUtils.getScope("root");
			}
			req.setPath(noAppPath);
			IHTTPApplicationAdapter applicationAdapter = scope.getHttpApplicationAdapter();
			conn.setApplicationAdapter(applicationAdapter);
			applicationAdapter.onHTTPRequest(req, resp);
		} else if(message instanceof HTTPChunk) {
			
			HTTPChunk chunk = (HTTPChunk)message;
			IHTTPApplicationAdapter applicationAdapter = conn.getApplicationAdapter();
			if(applicationAdapter != null) {
				applicationAdapter.onHTTPChunk(chunk);
			} else {
				session.close(false);
			}
		} else {
			log.info("unkown http request : {}", message.toString());
			session.close(false);
		} 
		Red5.setConnectionLocal(null);
	}
	
	@Override
	public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
		cause.printStackTrace();
		log.error("Exception caught {}", cause.toString());
		session.close(false);
	}
	
	@Override
	public void sessionClosed(IoSession session) throws Exception {
		log.debug("HTTP Session Closed id = {}", session.getId());
		HTTPMinaConnection conn = (HTTPMinaConnection)session.getAttribute(HTTPMinaConnection.HTTP_CONNECTION_KEY);
		conn.close();
		httpConnMgr.removeConnection(session.getId());
	}
	
	@Override
	public void sessionIdle(IoSession session, IdleStatus status)
			throws Exception {
		log.debug("HTTP Session Idle id = {}", session.getId());
		session.close(false);
	}
	
	@Override
	public void messageSent(IoSession session, Object message) throws Exception {
		
		HTTPMinaConnection conn = (HTTPMinaConnection)session.getAttribute(HTTPMinaConnection.HTTP_CONNECTION_KEY);
		conn.messageSent(message);
	}
}
