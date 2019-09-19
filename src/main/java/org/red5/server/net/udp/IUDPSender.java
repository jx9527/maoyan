package org.red5.server.net.udp;

/**
 * UDP Sender
 * @author pengliren
 *
 */
public interface IUDPSender {

	public void handleSendMessage(byte[] data, int pos, int len);
}
