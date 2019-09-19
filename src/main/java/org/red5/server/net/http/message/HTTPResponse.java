package org.red5.server.net.http.message;

/**
 * HTTP Response Interface
 * @author pengliren
 *
 */
public interface HTTPResponse extends HTTPMessage {

    /**
     * Returns the status of this response.
     */
    HTTPResponseStatus getStatus();

    /**
     * Sets the status of this response.
     */
    void setStatus(HTTPResponseStatus status);
}
