package org.red5.server.net.proxy;

public interface IProxyStream {

	public void register();
	public boolean isClosed();
	public void stop();
}
