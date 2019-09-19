package org.red5.server.net.http;

import org.red5.server.scope.Scope;
import org.red5.server.net.http.message.HTTPChunk;
import org.red5.server.net.http.message.HTTPRequest;
import org.red5.server.net.http.message.HTTPResponse;

/**
 * HTTP Application Adapter Inteface
 * @author pengliren
 *
 */
public interface IHTTPApplicationAdapter {

	public void onHTTPRequest(HTTPRequest req, HTTPResponse resp) throws Exception;
	
	public void onHTTPChunk(HTTPChunk chunk) throws Exception;
	
	public void onConnectionStart(HTTPMinaConnection conn);
	
	public void onConnectionClose(HTTPMinaConnection conn);
	
	public void setScope(Scope scope);
	
	public Scope getScope();
	
	public void addHttpService(String name, IHTTPService httpService);
	
	public IHTTPService getHttpService(String name);
	
	public void removeHttpService(String name);
}
