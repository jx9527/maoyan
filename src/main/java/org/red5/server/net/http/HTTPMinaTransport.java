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
import org.red5.conf.ExtConfiguration;
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
	
	public void start() throws IOException {
		log.info("HTTP Mina Transport starting...");
		if (useHeapBuffers) {
			// dont pool for heap buffers
			IoBuffer.setAllocator(new SimpleBufferAllocator());
		}
		
		acceptor = new NioSocketAcceptor(ExtConfiguration.HTTP_IO_THREADS);	
		ioHandler = new HTTPMinaIoHandler();
		acceptor.setHandler(ioHandler);
		acceptor.setBacklog(ExtConfiguration.HTTP_MAX_BACKLOG);
		
		SocketSessionConfig sessionConf = acceptor.getSessionConfig();
		//reuse the addresses
		sessionConf.setReuseAddress(true);
		sessionConf.setTcpNoDelay(ExtConfiguration.HTTP_TCP_NODELAY);
		sessionConf.setReceiveBufferSize(ExtConfiguration.HTTP_RECEIVE_BUFFER_SIZE);
		sessionConf.setMaxReadBufferSize(ExtConfiguration.HTTP_RECEIVE_BUFFER_SIZE);
		sessionConf.setSendBufferSize(ExtConfiguration.HTTP_SEND_BUFFER_SIZE);		
		//set reuse address on the socket acceptor as well
		acceptor.setReuseAddress(true);		
		OrderedThreadPoolExecutor executor = new OrderedThreadPoolExecutor(ExtConfiguration.HTTP_WORKER_THREADS);
		executor.setThreadFactory(new CustomizableThreadFactory("HttpWorkerExecutor-"));
		acceptor.getFilterChain().addLast("threadPool", new ExecutorFilter(executor));
		acceptor.bind(new InetSocketAddress(ExtConfiguration.HTTP_HOST,ExtConfiguration.HTTP_PORT));
		log.info("HTTP Socket Acceptor bound to :"+ExtConfiguration.HTTP_PORT);
	}
	
	public void stop() {
		acceptor.unbind();
		log.info("HTTP Mina Transport stopped");
	}

	public void setIoHandler(IoHandler ioHandler) {
		this.ioHandler = ioHandler;
	} 
	
}
