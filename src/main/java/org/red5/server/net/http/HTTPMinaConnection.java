package org.red5.server.net.http;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;

import org.red5.server.BaseConnection;
import org.red5.server.api.stream.IClientBroadcastStream;
import org.red5.server.api.stream.IClientStream;
import org.red5.server.api.stream.IPlaylistSubscriberStream;
import org.red5.server.api.stream.ISingleItemSubscriberStream;
import org.red5.server.api.stream.IStreamCapableConnection;

/**
 * HTTP Mina Connection
 * @author pengliren
 *
 */
public class HTTPMinaConnection extends BaseConnection implements IStreamCapableConnection {

	public static final String HTTP_CONNECTION_KEY = "http.conn";
	
	private boolean isClosed = false;
	
	private IoSession httpSession;
	
	private AtomicInteger pendings = new AtomicInteger();
	
	private IHTTPApplicationAdapter applicationAdapter;
	
	public HTTPMinaConnection(IoSession session) {

		httpSession = session;		
	}
	
	public void close() {

		if (isClosed == false) {
			isClosed = true;
		} else {
			return;
		}
		
		if(applicationAdapter != null) applicationAdapter.onConnectionClose(this);
	}
	
	public WriteFuture write(Object out) {
		
		pendings.incrementAndGet();
		return httpSession.write(out);		
	}
	
	public void messageSent(Object message) {

		if (message instanceof IoBuffer) {
			pendings.decrementAndGet();
		}
	}
	
	@Override
	public Encoding getEncoding() {
		return null;
	}

	@Override
	public void ping() {
		
	}

	@Override
	public int getLastPingTime() {
		return 0;
	}

	@Override
	public void setBandwidth(int mbits) {
		
	}  
 
	@Override
	public long getReadBytes() {
		return httpSession.getReadBytes();
	}

	@Override
	public long getWrittenBytes() {
		return httpSession.getWrittenBytes();
	}

	@Override
	public long getPendingMessages() {
		
		return pendings.longValue();
	}
	
	public boolean isClosing() {

		return httpSession.isClosing();
	}
	
	public IoSession getHttpSession() {
		
		return httpSession;
	}

	public IHTTPApplicationAdapter getApplicationAdapter() {
		return applicationAdapter;
	}

	public void setApplicationAdapter(IHTTPApplicationAdapter applicationAdapter) {
		this.applicationAdapter = applicationAdapter;
	}

	@Override
	public String getProtocol() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Number reserveStreamId() throws IndexOutOfBoundsException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Number reserveStreamId(Number streamId) throws IndexOutOfBoundsException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void unreserveStreamId(Number streamId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deleteStreamById(Number streamId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public IClientStream getStreamById(Number streamId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ISingleItemSubscriberStream newSingleItemSubscriberStream(Number streamId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IPlaylistSubscriberStream newPlaylistSubscriberStream(Number streamId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IClientBroadcastStream newBroadcastStream(Number streamId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Number, IClientStream> getStreamsMap() {
		// TODO Auto-generated method stub
		return null;
	}
}
