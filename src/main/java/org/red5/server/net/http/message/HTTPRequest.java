package org.red5.server.net.http.message;

/**
 * HTTP Request Interface
 * @author pengliren
 *
 */
public interface HTTPRequest extends HTTPMessage {
 
    HTTPMethod getMethod();
 
    void setMethod(HTTPMethod method);
 
    String getUri();
 
    void setUri(String uri);
     
    String getPath();
     
    void setPath(String path);
}
