package org.red5.server.net.http;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.WriteFuture;
import org.red5.server.api.Red5;
import org.red5.server.net.http.codec.QueryStringDecoder;
import org.red5.server.net.http.file.HTTPStaticFileService;
import org.red5.server.net.http.message.HTTPChunk;
import org.red5.server.net.http.message.HTTPRequest;
import org.red5.server.net.http.message.HTTPResponse;
import org.red5.server.net.http.message.HTTPResponseStatus;
import org.red5.server.net.http.stream.CustomSingleItemSubStream;
import org.red5.server.net.http.stream.HTTPAESKeyService;
import org.red5.server.net.http.stream.HTTPConnectionConsumer;
import org.red5.server.net.http.stream.HTTPFlvPlayerService;
import org.red5.server.net.http.stream.HTTPLiveFlvPublisherService;
import org.red5.server.net.http.stream.HTTPM3U8Service;
import org.red5.server.net.http.stream.HTTPShutDownService;
import org.red5.server.net.http.stream.HTTPTSService;
import org.red5.server.net.proxy.HTTPPushProxyStream;
import org.red5.server.scope.Scope;
import org.red5.server.util.MatcherUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP Application Adapter
 * @author pengliren
 *
 */
public class HTTPApplicationAdapter implements IHTTPApplicationAdapter {

	private static Logger log = LoggerFactory.getLogger(HTTPApplicationAdapter.class);
	
	private Scope scope;
	
	private Map<String, IHTTPService> serviceMap = new LinkedHashMap<String, IHTTPService>();
	
	public HTTPApplicationAdapter() {
		init();
	}
	
	private void init() {
		
		addHttpService("/*", new HTTPStaticFileService());		
		addHttpService("/flv/*", new HTTPFlvPlayerService());
		addHttpService("*/aes", new HTTPAESKeyService());
		addHttpService("/liveflv/*", new HTTPLiveFlvPublisherService());
		addHttpService("*.m3u8", new HTTPM3U8Service());
		addHttpService("*.ts", new HTTPTSService());
		addHttpService("*/shutdown", new HTTPShutDownService());
		log.info("init http application adater");
	}
	
	@Override
	public void onHTTPRequest(HTTPRequest req, HTTPResponse resp) throws Exception {
	
		String path = req.getPath();
		QueryStringDecoder decoder = new QueryStringDecoder(path);
		path = decoder.getPath();
		boolean find = false;
		// handle add http service
		String[] keys = serviceMap.keySet().toArray(new String[0]);
		for(int i = keys.length - 1; i >= 0 ; i--) {
			if(MatcherUtil.match(keys[i], path)) {
				serviceMap.get(keys[i]).handleRequest(req, resp, scope);
				find = true;
				break;
			}
		}
				
		if(!find) {
			HTTPMinaConnection conn = (HTTPMinaConnection)Red5.getConnectionLocal();
			resp.setStatus(HTTPResponseStatus.NOT_FOUND);
			WriteFuture future = conn.write(resp);
			future.addListener(new IoFutureListener<IoFuture>() {
				@Override
				public void operationComplete(IoFuture future) {
					future.getSession().closeNow();
				}
			});
		}
		System.out.println(resp.toString());
	}
	
	@Override
	public void onHTTPChunk(HTTPChunk chunk) throws Exception {
		
		HTTPMinaConnection conn = (HTTPMinaConnection)Red5.getConnectionLocal();
		if(conn.getAttribute("pushStream") != null) {
			HTTPPushProxyStream pushStream = (HTTPPushProxyStream)conn.getAttribute("pushStream");
			if(pushStream != null) pushStream.handleMessage(chunk.getContent());
		}
	}
	
	@Override
	public void onConnectionStart(HTTPMinaConnection conn) {
		
	}

	@Override
	public void onConnectionClose(HTTPMinaConnection conn) {
		
		if(conn.getAttribute("consumer") != null) {
			HTTPConnectionConsumer consumer = (HTTPConnectionConsumer)conn.getAttribute("consumer"); 
			consumer.setClose(true);
		}
		
		if(conn.getAttribute("stream") != null) {
			CustomSingleItemSubStream stream = (CustomSingleItemSubStream)conn.getAttribute("stream"); 
			stream.close();
		}
		
		if(conn.getAttribute("pushStream") != null) {
			HTTPPushProxyStream pushStream = (HTTPPushProxyStream)conn.getAttribute("pushStream");
			pushStream.stop();
		}
	}

	@Override
	public void setScope(Scope scope) {

		this.scope = scope;
	}

	@Override
	public Scope getScope() {

		return this.scope;
	}

	@Override
	public void addHttpService(String name, IHTTPService httpService) {
		serviceMap.put(name, httpService);
		httpService.start();
	}
	
	@Override
	public IHTTPService getHttpService(String name) {

		IHTTPService service = null;
		for (String key : serviceMap.keySet()) {
			if (key.equals(name)) {
				service = serviceMap.get(key);
				break;
			}
		}

		return service;
	}

	@Override
	public void removeHttpService(String name) {
		IHTTPService service = serviceMap.remove(name);
		service.stop();
	}
}
