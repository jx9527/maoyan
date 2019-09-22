package org.red5.conf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 
 * @ClassName: Configuration
 * @Description: 读取配置信息
 * @author pengliren
 * 
 */
@Configuration
public class ExtConfiguration{
 
	public static String HTTP_HOST = "0.0.0.0";
	public static int HTTP_PORT = 5080;
	public static int HTTP_IO_THREADS = 2; 
	public static int HTTP_WORKER_THREADS = 10;
	public static int HTTP_SEND_BUFFER_SIZE = 65536;
	public static int HTTP_RECEIVE_BUFFER_SIZE = 65536;
	public static boolean HTTP_TCP_NODELAY = true;
	public static int HTTP_MAX_BACKLOG = 5000;
	public static int HTTP_IDLE = 30;
	
	public static int HLS_SEGMENT_MAX = 3;
	public static int HLS_SEGMENT_TIME = 10;
	public static boolean HLS_ENCRYPT = false;

	public static String RTMP_HOST = "0.0.0.0";
	public static int RTMP_PORT = 1935;
	public static int RTMP_IO_THREADS = 2;
	public static int RTMP_WORKER_THREADS = 10;
	public static int RTMP_SEND_BUFFER_SIZE = 271360;
	public static int RTMP_RECEIVE_BUFFER_SIZE = 65536;
	public static int RTMP_PING_INTERVAL = 1000;
	public static int RTMP_MAX_INACTIVITY = 60000;
	public static int RTMP_MAX_HANDSHAKE_TIMEOUT = 5000;
	public static boolean RTMP_TCP_NODELAY = true;
	public static int RTMP_MAX_BACKLOG = 5000;
	public static int RTMP_DEFAULT_SERVER_BANDWIDTH = 10000000;
	public static int RTMP_DEFAULT_CLIENT_BANDWIDTH = 10000000;
	public static int RTMP_CLIENT_BANDWIDTH_LIMIT_TYPE = 2;
	public static boolean RTMP_BANDWIDTH_DETECTION = true;
	
	public static String RTSP_HOST = "0.0.0.0";
	public static int RTSP_PORT = 554;
	public static int RTSP_IO_THREADS = 2;
	public static int RTSP_WORKER_THREADS = 10;
	public static int RTSP_SEND_BUFFER_SIZE = 65536;
	public static int RTSP_RECEIVE_BUFFER_SIZE = 65536;
	public static boolean RTSP_TCP_NODELAY = true;
	public static int RTSP_MAX_BACKLOG = 8000;
	public static int UDP_PORT_START = 6970;
	
	public static String JMX_RMI_HOST = "0.0.0.0";
	public static String JMX_RMI_PORT_REMOTEOBJECTS = "";
	public static int JMX_RMI_PORT_REGISTRY = 9999;
	public static boolean JMX_RMI_ENABLE = false;
	
	public static long NOTIFY_SYSTIMER_TICK = 20;
	
	public static int FILECACHE_MAXSIZE = 500;
	public static int FILECACHE_PURGE = 10;
	public static int CACHE_INTERVAL = 10;
	
	
	public static int MULTICAST_EXECUTOR_THREADS = 4;
	public static int UNICAST_EXECUTOR_THREADS = 4;
	 
	
	
	public static int POOL_SIZE = 8; 
	public static int CORE_POOL_SIZE = 4; 
	public static int MAX_POOL_SIZE = 12; 
	public static int QUEUE_CAPACITY = 64; 
	public static int DEAD_POOL_SIZE = 8; 
	public static long BASE_TO_LERANCE = 5000; 
	public static boolean DROP_LIVE_FUTURE = false;
	
	
	
	@Value("${rtmpt.encoder_drop_live_future}")
	public  void setDropLiveFuture(boolean dropLiveFuture) {
		DROP_LIVE_FUTURE = dropLiveFuture;
	}
	
	@Value("${rtmpt.encoder_base_tolerance}")
	public  void setBaseTolerance(int baseTolerance) {
		BASE_TO_LERANCE = baseTolerance;
	}
	
	@Value("${rtmp.deadlockguard.sheduler.pool_size}")
	public  void setDeadPoolSize(int deadPoolSize) {
		DEAD_POOL_SIZE = deadPoolSize;
	}
	
	@Value("${rtmp.executor.queue_capacity}")
	public  void setQueueCapacity(int queueCapacity) {
		QUEUE_CAPACITY = queueCapacity;
	}
	
	@Value("${rtmp.executor.max_pool_size}")
	public  void setMaxPoolSize(int maxPoolSize) {
		MAX_POOL_SIZE = maxPoolSize;
	}
	
	@Value("${rtmp.scheduler.pool_size}")
	public  void setPoolSize(int poolSize) {
		POOL_SIZE = poolSize;
	}
	@Value("${rtmp.executor.core_pool_size}")
	public  void setCorePoolSize(int corePoolSize) {
		CORE_POOL_SIZE = corePoolSize;
	} 
	
	@Value("${http.host}")
	public  void setHTTP_HOST(String hTTP_HOST) {
		HTTP_HOST = hTTP_HOST;
	}
	@Value("${http.port}")
	public  void setHTTP_PORT(int hTTP_PORT) {
		HTTP_PORT = hTTP_PORT;
	}
	@Value("${http.io_threads}")
	public  void setHTTP_IO_THREADS(int hTTP_IO_THREADS) {
		HTTP_IO_THREADS = hTTP_IO_THREADS;
	}
	@Value("${http.worker_threads}")
	public  void setHTTP_WORKER_THREADS(int hTTP_WORKER_THREADS) {
		HTTP_WORKER_THREADS = hTTP_WORKER_THREADS;
	}
	@Value("${http.send_buffer_size}")
	public  void setHTTP_SEND_BUFFER_SIZE(int hTTP_SEND_BUFFER_SIZE) {
		HTTP_SEND_BUFFER_SIZE = hTTP_SEND_BUFFER_SIZE;
	}
	@Value("${http.receive_buffer_size}")
	public  void setHTTP_RECEIVE_BUFFER_SIZE(int hTTP_RECEIVE_BUFFER_SIZE) {
		HTTP_RECEIVE_BUFFER_SIZE = hTTP_RECEIVE_BUFFER_SIZE;
	}
	@Value("${http.tcp_nodelay}")
	public  void setHTTP_TCP_NODELAY(boolean hTTP_TCP_NODELAY) {
		HTTP_TCP_NODELAY = hTTP_TCP_NODELAY;
	}
	@Value("${http.max_backlog}")
	public  void setHTTP_MAX_BACKLOG(int hTTP_MAX_BACKLOG) {
		HTTP_MAX_BACKLOG = hTTP_MAX_BACKLOG;
	}
	@Value("${http.idle}")
	public  void setHTTP_IDLE(int hTTP_IDLE) {
		HTTP_IDLE = hTTP_IDLE;
	}
	@Value("${hls.segment_max}")
	public  void setHLS_SEGMENT_MAX(int hLS_SEGMENT_MAX) {
		HLS_SEGMENT_MAX = hLS_SEGMENT_MAX;
	}
	@Value("${hls.segment_time}")
	public  void setHLS_SEGMENT_TIME(int hLS_SEGMENT_TIME) {
		HLS_SEGMENT_TIME = hLS_SEGMENT_TIME;
	}
	@Value("${hls.encrypt}")
	public  void setHLS_ENCRYPT(boolean hLS_ENCRYPT) {
		HLS_ENCRYPT = hLS_ENCRYPT;
	} 
	 
	@Value("${rtsp.host}")
	public  void setRTSP_HOST(String rTSP_HOST) {
		RTSP_HOST = rTSP_HOST;
	}
	@Value("${rtsp.port}")
	public  void setRTSP_PORT(int rTSP_PORT) {
		RTSP_PORT = rTSP_PORT;
	}
	@Value("${rtsp.io_threads}")
	public  void setRTSP_IO_THREADS(int rTSP_IO_THREADS) {
		RTSP_IO_THREADS = rTSP_IO_THREADS;
	}
	@Value("${rtsp.worker_threads}")
	public  void setRTSP_WORKER_THREADS(int rTSP_WORKER_THREADS) {
		RTSP_WORKER_THREADS = rTSP_WORKER_THREADS;
	}
	@Value("${rtsp.send_buffer_size}")
	public  void setRTSP_SEND_BUFFER_SIZE(int rTSP_SEND_BUFFER_SIZE) {
		RTSP_SEND_BUFFER_SIZE = rTSP_SEND_BUFFER_SIZE;
	}
	@Value("${rtsp.receive_buffer_size}")
	public  void setRTSP_RECEIVE_BUFFER_SIZE(int rTSP_RECEIVE_BUFFER_SIZE) {
		RTSP_RECEIVE_BUFFER_SIZE = rTSP_RECEIVE_BUFFER_SIZE;
	}
	@Value("${rtsp.tcp_nodelay}")
	public  void setRTSP_TCP_NODELAY(boolean rTSP_TCP_NODELAY) {
		RTSP_TCP_NODELAY = rTSP_TCP_NODELAY;
	}
	@Value("${rtsp.max_backlog}")
	public  void setRTSP_MAX_BACKLOG(int rTSP_MAX_BACKLOG) {
		RTSP_MAX_BACKLOG = rTSP_MAX_BACKLOG;
	}
	@Value("${udp.port_start}")
	public  void setUDP_PORT_START(int uDP_PORT_START) {
		UDP_PORT_START = uDP_PORT_START;
	}
	@Value("${jmx.rmi.host}")
	public  void setJMX_RMI_HOST(String jMX_RMI_HOST) {
		JMX_RMI_HOST = jMX_RMI_HOST;
	}
	@Value("${jmx.rmi.remoteobjects}")
	public  void setJMX_RMI_PORT_REMOTEOBJECTS(String jMX_RMI_PORT_REMOTEOBJECTS) {
		JMX_RMI_PORT_REMOTEOBJECTS = jMX_RMI_PORT_REMOTEOBJECTS;
	}
	@Value("${jmx.rmi.registryes}")
	public  void setJMX_RMI_PORT_REGISTRY(int jMX_RMI_PORT_REGISTRY) {
		JMX_RMI_PORT_REGISTRY = jMX_RMI_PORT_REGISTRY;
	}
	@Value("${jmx.rmi.enable}")
	public  void setJMX_RMI_ENABLE(boolean jMX_RMI_ENABLE) {
		JMX_RMI_ENABLE = jMX_RMI_ENABLE;
	}
	@Value("${notify.systimer.tick}")
	public  void setNOTIFY_SYSTIMER_TICK(long nOTIFY_SYSTIMER_TICK) {
		NOTIFY_SYSTIMER_TICK = nOTIFY_SYSTIMER_TICK;
	}
	@Value("${filecache_maxsize}")
	public  void setFILECACHE_MAXSIZE(int fILECACHE_MAXSIZE) {
		FILECACHE_MAXSIZE = fILECACHE_MAXSIZE;
	}
	@Value("${filecache_purge}")
	public  void setFILECACHE_PURGE(int fILECACHE_PURGE) {
		FILECACHE_PURGE = fILECACHE_PURGE;
	}
	@Value("${cache_interval}")
	public  void setCACHE_INTERVAL(int cACHE_INTERVAL) {
		CACHE_INTERVAL = cACHE_INTERVAL;
	}
	@Value("${multicast.executor_threads}")
	public  void setMULTICAST_EXECUTOR_THREADS(int mULTICAST_EXECUTOR_THREADS) {
		MULTICAST_EXECUTOR_THREADS = mULTICAST_EXECUTOR_THREADS;
	}
	@Value("${unicast.executor_threads}")
	public  void setUNICAST_EXECUTOR_THREADS(int uNICAST_EXECUTOR_THREADS) {
		UNICAST_EXECUTOR_THREADS = uNICAST_EXECUTOR_THREADS;
	} 
	 
}
