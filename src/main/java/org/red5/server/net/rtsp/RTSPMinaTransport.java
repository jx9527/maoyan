package org.red5.server.net.rtsp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.buffer.SimpleBufferAllocator;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.filter.executor.OrderedThreadPoolExecutor;
import org.apache.mina.transport.socket.SocketAcceptor;
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.apache.mina.transport.socket.nio.NioDatagramAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.red5.server.ExtConfiguration;
import org.red5.server.net.rtp.RTPMinaIoHandler;
import org.red5.server.net.rtp.rtcp.RTCPMinaIoHandler;
import org.red5.server.util.CustomizableThreadFactory;

/**
 * RTSP Mina Transport
 * @author pengliren
 *
 */
public class RTSPMinaTransport {

	private static final Logger log = LoggerFactory.getLogger(RTSPMinaTransport.class);
	
	private SocketAcceptor acceptor;
	private IoHandler ioHandler;
	protected boolean useHeapBuffers = true;
	
	public final static NioDatagramAcceptor RTP_VIDEO_ACCEPTOR = new NioDatagramAcceptor();
	public final static NioDatagramAcceptor RTP_AUDIO_ACCEPTOR = new NioDatagramAcceptor();
	public final static NioDatagramAcceptor RTCP_VIDEO_ACCEPTOR = new NioDatagramAcceptor();
	public final static NioDatagramAcceptor RTCP_AUDIO_ACCEPTOR = new NioDatagramAcceptor();
	
	public RTSPMinaTransport() throws IOException {
							
		ExecutorService executor = Executors.newFixedThreadPool(4, new CustomizableThreadFactory("UdpWorkerExecutor-"));
		RTP_VIDEO_ACCEPTOR.setHandler(new RTPMinaIoHandler());
		RTP_VIDEO_ACCEPTOR.getFilterChain().addLast("threadPool", new ExecutorFilter(executor)); 
		
		RTP_AUDIO_ACCEPTOR.setHandler(new RTPMinaIoHandler());
		RTP_AUDIO_ACCEPTOR.getFilterChain().addLast("threadPool", new ExecutorFilter(executor)); 
				
		RTCP_VIDEO_ACCEPTOR.setHandler(new RTCPMinaIoHandler());
		RTCP_VIDEO_ACCEPTOR.getFilterChain().addLast("threadPool", new ExecutorFilter(executor)); 
				
		RTCP_AUDIO_ACCEPTOR.setHandler(new RTCPMinaIoHandler());
		RTCP_AUDIO_ACCEPTOR.getFilterChain().addLast("threadPool", new ExecutorFilter(executor)); 
	}
	
	public void start() throws IOException {
		log.info("RTSP Mina Transport starting...");
		if (useHeapBuffers) {
			// dont pool for heap buffers
			IoBuffer.setAllocator(new SimpleBufferAllocator());
		}
		
		acceptor = new NioSocketAcceptor(ExtConfiguration.RTSP_IO_THREADS);	
		ioHandler = new RTSPMinaIoHandler();
		acceptor.setHandler(ioHandler);
		acceptor.setBacklog(ExtConfiguration.RTSP_MAX_BACKLOG);
		
		SocketSessionConfig sessionConf = acceptor.getSessionConfig();
		//reuse the addresses
		sessionConf.setReuseAddress(true);
		sessionConf.setTcpNoDelay(ExtConfiguration.RTSP_TCP_NODELAY);
		sessionConf.setReceiveBufferSize(ExtConfiguration.RTSP_RECEIVE_BUFFER_SIZE);
		sessionConf.setMaxReadBufferSize(ExtConfiguration.RTSP_RECEIVE_BUFFER_SIZE);
		sessionConf.setSendBufferSize(ExtConfiguration.RTSP_SEND_BUFFER_SIZE);
		//set reuse address on the socket acceptor as well
		acceptor.setReuseAddress(true);		
		OrderedThreadPoolExecutor executor = new OrderedThreadPoolExecutor(ExtConfiguration.RTSP_WORKER_THREADS);
		executor.setThreadFactory(new CustomizableThreadFactory("RtspWorkerExecutor-"));
		acceptor.getFilterChain().addLast("threadPool", new ExecutorFilter(executor));
		acceptor.bind(new InetSocketAddress(ExtConfiguration.RTSP_HOST, ExtConfiguration.RTSP_PORT));
		log.info("RTSP Socket Acceptor bound to :"+ExtConfiguration.RTSP_PORT);
		
		RTP_VIDEO_ACCEPTOR.bind(new InetSocketAddress(0));
		log.info("RTP VIDEO Socket Acceptor bound to :"+RTP_VIDEO_ACCEPTOR.getLocalAddress().getPort());
		RTP_AUDIO_ACCEPTOR.bind(new InetSocketAddress(0));		
		log.info("RTP AUDIO Socket Acceptor bound to :"+RTP_AUDIO_ACCEPTOR.getLocalAddress().getPort());
		RTCP_VIDEO_ACCEPTOR.bind(new InetSocketAddress(0));
		log.info("RTCP VIDEO Socket Acceptor bound to :"+RTCP_VIDEO_ACCEPTOR.getLocalAddress().getPort());
		RTCP_AUDIO_ACCEPTOR.bind(new InetSocketAddress(0));
		log.info("RTCP AUDIO Socket Acceptor bound to :"+RTCP_AUDIO_ACCEPTOR.getLocalAddress().getPort());
	}
	
	public void stop() {
		acceptor.unbind();
		log.info("RTSP Mina Transport stopped");
	}

	public void setIoHandler(IoHandler ioHandler) {
		this.ioHandler = ioHandler;
	}
}
