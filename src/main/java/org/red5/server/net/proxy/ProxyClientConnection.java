package org.red5.server.net.proxy;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.server.api.IConnection;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmp.message.Packet;

public class ProxyClientConnection extends RTMPConnection {

	public ProxyClientConnection() {
		super(IConnection.Type.PERSISTENT.toString());
		this.state = new RTMP(RTMP.MODE_CLIENT);
	} 

	@Override
	public void write(Packet out) {

	}

	@Override
	protected void onInactive() {
		this.close();
	}

	@Override
	public void writeRaw(IoBuffer out) {
		// TODO Auto-generated method stub
		
	}

}
