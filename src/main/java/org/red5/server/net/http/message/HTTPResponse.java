package org.red5.server.net.http.message;

/**
 * HTTP Response Interface
 * @author pengliren
 *
 */
public interface HTTPResponse extends HTTPMessage {
 
    HTTPResponseStatus getStatus();
 
    void setStatus(HTTPResponseStatus status);
}
