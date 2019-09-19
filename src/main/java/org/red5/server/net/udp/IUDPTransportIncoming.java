package org.red5.server.net.udp;

/**
 * UDP TransportIncoming
 * @author pengliren
 *
 */
public interface IUDPTransportIncoming {

	public IUDPTransportIncomingConnection bind(UDPDatagramConfig config, IUDPMessageHandler messageHandler, String ipAddress, int port);
	
	public void unbind(IUDPTransportIncomingConnection connection);
	
	public long getConnectionCount();
}
