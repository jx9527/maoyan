package org.red5.server.net.http.message;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.mina.core.buffer.IoBuffer;

/**
 * HTTP Message Interface
 * @author pengliren
 *
 */
public interface HTTPMessage {
 
    String getHeader(String name);
 
    List<String> getHeaders(String name);
 
    List<Map.Entry<String, String>> getHeaders();
 
    boolean containsHeader(String name);
 
    Set<String> getHeaderNames();
 
    HTTPVersion getProtocolVersion();
 
    void setProtocolVersion(HTTPVersion version);
 
    IoBuffer getContent();
 
    void setContent(IoBuffer content);
 
    void addHeader(String name, Object value);
 
    void setHeader(String name, Object value);
 
    void setHeader(String name, Iterable<?> values);
 
    void removeHeader(String name);
 
    void clearHeaders();
 
    boolean isChunked();
 
    void setChunked(boolean chunked);
}
