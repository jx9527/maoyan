package org.red5.server.net.http;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.buffer.SimpleBufferAllocator;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.filter.executor.OrderedThreadPoolExecutor;
import org.apache.mina.transport.socket.SocketAcceptor;
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.red5.server.util.CustomizableThreadFactory;

/**
 * HTTP Server Transport
 * @author pengliren
 *
 */
public class HTTPMinaTransport {

	private static final Logger log = LoggerFactory.getLogger(HTTPMinaTransport.class);
	
	private SocketAcceptor acceptor;
	private IoHandler ioHandler;
	protected boolean useHeapBuffers = true;
	
	private int httpIoThrads;
	private int httpMaxBacklog;
	private boolean httpTcpNoDelay;
	private int httpReceiveBufferSize;
	private int httpSendBufferSize;
	private int httpWorkerThreads;
	private String httpHost;
	private int httpPort;
	
	public void start() throws IOException {
		log.info("HTTP Mina Transport starting...");
		if (useHeapBuffers) {
			// dont pool for heap buffers
			IoBuffer.setAllocator(new SimpleBufferAllocator());
		}
		
		acceptor = new NioSocketAcceptor(httpIoThrads);	
		ioHandler = new HTTPMinaIoHandler();
		acceptor.setHandler(ioHandler);
		acceptor.setBacklog(httpMaxBacklog);
		
		SocketSessionConfig sessionConf = acceptor.getSessionConfig();
		//reuse the addresses
		sessionConf.setReuseAddress(true);
		sessionConf.setTcpNoDelay(httpTcpNoDelay);
		sessionConf.setReceiveBufferSize(httpReceiveBufferSize);
		sessionConf.setMaxReadBufferSize(httpReceiveBufferSize);
		sessionConf.setSendBufferSize(httpSendBufferSize);		
		//set reuse address on the socket acceptor as well
		acceptor.setReuseAddress(true);		
		OrderedThreadPoolExecutor executor = new OrderedThreadPoolExecutor(httpWorkerThreads);
		executor.setThreadFactory(new CustomizableThreadFactory("HttpWorkerExecutor-"));
		acceptor.getFilterChain().addLast("threadPool", new ExecutorFilter(executor));
		acceptor.bind(new InetSocketAddress(httpHost,httpPort));
		log.info("HTTP Socket Acceptor bound to :"+httpPort);
	}
	
	public void stop() {
		acceptor.unbind();
		log.info("HTTP Mina Transport stopped");
	}

	public int getHttpIoThrads() {
		return httpIoThrads;
	}

	public void setHttpIoThrads(int httpIoThrads) {
		this.httpIoThrads = httpIoThrads;
	}

	public int getHttpMaxBacklog() {
		return httpMaxBacklog;
	}

	public void setHttpMaxBacklog(int httpMaxBacklog) {
		this.httpMaxBacklog = httpMaxBacklog;
	}

	public boolean isHttpTcpNoDelay() {
		return httpTcpNoDelay;
	}

	public void setHttpTcpNoDelay(boolean httpTcpNoDelay) {
		this.httpTcpNoDelay = httpTcpNoDelay;
	}

	public int getHttpReceiveBufferSize() {
		return httpReceiveBufferSize;
	}

	public void setHttpReceiveBufferSize(int httpReceiveBufferSize) {
		this.httpReceiveBufferSize = httpReceiveBufferSize;
	}

	public int getHttpSendBufferSize() {
		return httpSendBufferSize;
	}

	public void setHttpSendBufferSize(int httpSendBufferSize) {
		this.httpSendBufferSize = httpSendBufferSize;
	}

	public int getHttpWorkerThreads() {
		return httpWorkerThreads;
	}

	public void setHttpWorkerThreads(int httpWorkerThreads) {
		this.httpWorkerThreads = httpWorkerThreads;
	}

	public String getHttpHost() {
		return httpHost;
	}

	public void setHttpHost(String httpHost) {
		this.httpHost = httpHost;
	}

	public int getHttpPort() {
		return httpPort;
	}

	public void setHttpPort(int httpPort) {
		this.httpPort = httpPort;
	}

	public void setIoHandler(IoHandler ioHandler) {
		this.ioHandler = ioHandler;
	}
	
}
