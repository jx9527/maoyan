package org.red5.conf;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.red5.cache.impl.NoCacheImpl;
import org.red5.demo.Application1;
import org.red5.io.CachingFileKeyFrameMetaCache;
import org.red5.io.flv.impl.FLV;
import org.red5.io.flv.impl.FLVReader;
import org.red5.io.mp3.impl.MP3Reader;
import org.red5.server.ClientRegistry;
import org.red5.server.Context;
import org.red5.server.CoreHandler;
import org.red5.server.MappingStrategy;
import org.red5.server.Server;
import org.red5.server.api.scope.IScopeSecurityHandler;
import org.red5.server.api.service.IStreamableFileService;
import org.red5.server.net.http.HTTPApplicationAdapter;
import org.red5.server.net.http.HTTPMinaIoHandler;
import org.red5.server.net.http.HTTPMinaTransport;
import org.red5.server.net.remoting.RemotingClient;
import org.red5.server.net.remoting.codec.RemotingCodecFactory;
import org.red5.server.net.rtmp.RTMPConnManager;
import org.red5.server.net.rtmp.RTMPHandler;
import org.red5.server.net.rtmp.RTMPMinaConnection;
import org.red5.server.net.rtmp.RTMPMinaIoHandler;
import org.red5.server.net.rtmp.RTMPMinaTransport;
import org.red5.server.net.rtmp.codec.RTMPMinaProtocolDecoder;
import org.red5.server.net.rtmp.codec.RTMPMinaProtocolEncoder;
import org.red5.server.net.rtmp.status.StatusObjectService;
import org.red5.server.net.rtmpt.RTMPTConnection;
import org.red5.server.net.rtmpt.RTMPTHandler;
import org.red5.server.net.rtmpt.RTMPTServlet;
import org.red5.server.net.rtmpt.codec.RTMPTCodecFactory;
import org.red5.server.scheduling.JDKSchedulingService;
import org.red5.server.scope.GlobalScope;
import org.red5.server.scope.ScopeResolver;
import org.red5.server.scope.ScopeSecurityHandler;
import org.red5.server.scope.WebScope;
import org.red5.server.service.ContextServiceResolver;
import org.red5.server.service.HandlerServiceResolver;
import org.red5.server.service.IServiceResolver;
import org.red5.server.service.ScopeServiceResolver;
import org.red5.server.service.ServiceInvoker;
import org.red5.server.service.flv.impl.FLVService;
import org.red5.server.service.m4a.impl.M4AService;
import org.red5.server.service.mp3.impl.MP3Service;
import org.red5.server.service.mp4.impl.MP4Service;
import org.red5.server.so.SharedObjectService;
import org.red5.server.stream.ClientBroadcastStream;
import org.red5.server.stream.ConsumerService;
import org.red5.server.stream.PlaylistSubscriberStream;
import org.red5.server.stream.ProviderService;
import org.red5.server.stream.RtmpSampleAccess;
import org.red5.server.stream.SingleItemSubscriberStream;
import org.red5.server.stream.StreamService;
import org.red5.server.stream.StreamableFileFactory;
import org.red5.server.stream.consumer.FileConsumer;
import org.red5.server.stream.timeshift.RecordableBroadcastStream;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.MethodInvokingFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration 
public class Red5Core {  
	
	@Value("${rtmp.scheduler.pool_size}")
	int poolSize;
	
	@Value("${rtmp.executor.core_pool_size}")
	int corePoolSize;
	@Value("${rtmp.executor.max_pool_size}")
	int maxPoolSize;
	@Value("${rtmp.executor.queue_capacity}")
	int queueCapacity;
	
	@Value("${rtmp.deadlockguard.sheduler.pool_size}")
	int deadPoolSize;
	
	@Value("${rtmpt.encoder_base_tolerance}")
	long baseTolerance;
	@Value("${rtmpt.encoder_drop_live_future}")
	boolean dropLiveFuture;
	
	
	//-------------------------------------------default-----------------------------
	@Order(1)
	@Bean(name="global.clientRegistry")
	@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public ClientRegistry clientRegistry(){
        return new ClientRegistry();
    }
	@Order(2)
	@Bean(name="global.serviceInvoker")
	@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public ServiceInvoker serviceInvoker(){
		ServiceInvoker inv = new ServiceInvoker();
		Set<IServiceResolver> serviceResolvers = new HashSet<IServiceResolver>();
		serviceResolvers.add(new ScopeServiceResolver());
		serviceResolvers.add(new HandlerServiceResolver());
		serviceResolvers.add(new ContextServiceResolver());
		inv.setServiceResolvers(serviceResolvers);
        return inv;
    } 
	@Order(3)
	@Bean(name="global.mappingStrategy")
	@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public MappingStrategy mappingStrategy(){ 
        return new MappingStrategy();
    }
	@Order(4)
	@Bean(name="global.context",autowire=Autowire.BY_TYPE)
	@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public Context context(){ 
        return new Context();
    }
	@Order(5)
	@Bean(name="global.handler",autowire=Autowire.BY_TYPE)
	@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public CoreHandler coreHandler(){ 
        return new CoreHandler();
    }
	@Order(6)
	@Bean(name="global.scope",initMethod="register")
	@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public GlobalScope globalScope() throws Exception{
		GlobalScope gsope = new GlobalScope(); 
		gsope.setServer(server());
		gsope.setContext(context());
		gsope.setHandler(coreHandler());
		gsope.setPersistenceClass("org.red5.server.persistence.FilePersistence");
		Set<IScopeSecurityHandler> securityHandlers = new HashSet<>();
		ScopeSecurityHandler handler = new ScopeSecurityHandler();
		handler.setConnectionAllowed(false);
		securityHandlers.add(handler);
		gsope.setSecurityHandlers(securityHandlers); 
		return gsope;
    }
	@Order(7)
	@Bean(name="red5.scopeResolver")
	@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public ScopeResolver scopeResolver() throws Exception{
		ScopeResolver resolver = new ScopeResolver();
		resolver.setGlobalScope(globalScope());
        return resolver;
    }
	
	
		
	//---------------------------------------common----------------------
	@Order(8)
	@Bean(name="red5.server")
	@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public Server server(){ 
        return Server.getInstance();
    }
	@Order(9)
	@Bean(name="statusObjectService",autowire= Autowire.BY_TYPE) 
	@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public StatusObjectService statusObjectService(){
        return new StatusObjectService();
    }
	
	@Order(10)
	@Bean(name="minaEncoder") 
	@Scope("prototype")
    public RTMPMinaProtocolEncoder minaEncoder(){
        return new RTMPMinaProtocolEncoder();
    }
	@Order(11)
	@Bean(name="minaDecoder") 
    public RTMPMinaProtocolDecoder minaDecoder(){
        return new RTMPMinaProtocolDecoder();
    }
	@Order(12)
	@Bean(name="rtmptCodecFactory",initMethod = "init",autowire=Autowire.BY_TYPE) 
	@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public RTMPTCodecFactory rtmptCodecFactory(){
		RTMPTCodecFactory factory = new RTMPTCodecFactory(); 
		factory.setBaseTolerance(baseTolerance);
		factory.setDropLiveFuture(dropLiveFuture); 
		return factory;
    }
	@Order(13)
	@Bean(name="remotingCodecFactory",initMethod = "init",autowire= Autowire.BY_TYPE) 
    public RemotingCodecFactory remotingCodecFactory(){
		RemotingCodecFactory factory = new RemotingCodecFactory();  
		return factory;
    }
	@Order(14)
	@Bean(name="streamableFileFactory") 
    public StreamableFileFactory streamableFileFactory(){
		StreamableFileFactory factory = new StreamableFileFactory(); 
		Set<IStreamableFileService> services = new HashSet<>();
		 
		FLVService  flv = new FLVService();
		flv.setGenerateMetadata(true);
		services.add(flv);
		services.add(new MP3Service());
		services.add(new MP4Service());
		services.add(new M4AService()); 
		factory.setServices(services);
		return factory;
    }
	
	@Value("${so.max.events.per.update}")
	int maxUpdate;
	@Order(15)
	@Bean(name="sharedObjectService") 
    public SharedObjectService sharedObjectService(){
		SharedObjectService factory = new SharedObjectService(); 
		//factory.setMaximumEventsPerUpdate(maxUpdate);
		factory.setPersistenceClassName("org.red5.server.persistence.FilePersistence");
		factory.setScheduler(poolScheduler());
		return factory;
    } 
	@Value("${so.scheduler.pool_size}")
	int soPoolSize;
	@Order(16)
    public ThreadPoolTaskScheduler poolScheduler(){
    	ThreadPoolTaskScheduler factory = new ThreadPoolTaskScheduler(); 
    	factory.setPoolSize(soPoolSize);
    	factory.setWaitForTasksToCompleteOnShutdown(false);
    	factory.setDaemon(true);
    	factory.setThreadNamePrefix("SharedObjectScheduler-");
		return factory;
    }
	@Order(17)
    @Bean(name="streamService") 
    public StreamService streamService(){ 
		return new StreamService();
    }
	@Order(18)
    @Bean(name="providerService") 
    public ProviderService providerService(){ 
		return new ProviderService();
    }
	@Order(19)
    @Bean(name="consumerService") 
    public ConsumerService consumerService(){ 
		return new ConsumerService();
    }
	@Order(20)
    @Bean(name="schedulingService") 
    public JDKSchedulingService schedulingService(){ 
		return new JDKSchedulingService();
    }
	@Order(21)
    @Bean(name="remotingClient") 
    public RemotingClient remotingClient(){ 
    	RemotingClient client = new RemotingClient();
    	client.setPoolSize(2);
		return client;
    }
	@Order(22)
    @Bean(name="object.cache") 
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public NoCacheImpl objectCache(){ 
		return NoCacheImpl.getInstance();
    }
    
    @Value("${keyframe.cache.entry.max}")
    int entryMax;
    @Order(23)
    @Bean(name="keyframe.cache") 
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public CachingFileKeyFrameMetaCache keyframeCache(){ 
    	CachingFileKeyFrameMetaCache cache = new CachingFileKeyFrameMetaCache();
    	cache.setMaxCacheEntry(entryMax);
		return cache;
    }
    @Order(24)
    @Bean(name="flv.impl") 
    public FLV flvImpl(){ 
    	FLV cache = new FLV();
    	cache.setCache(objectCache());
		return cache;
    }
    @Order(25)
    @Bean(name="flvreader.impl") 
    public FLVReader flvreaderImpl(){ 
    	FLVReader cache = new FLVReader();
    	cache.setKeyFrameCache(keyframeCache());
		return cache;
    }
    @Order(26)
    @Bean(name="mp3reader.impl") 
    public MP3Reader mp3readerImpl(){ 
    	MP3Reader cache = new MP3Reader();
    	cache.setFrameCache(keyframeCache());
		return cache;
    }
    @Order(27)
    @Bean(name="invokingOne")
    public MethodInvokingFactoryBean invoking1(){ 
    	MethodInvokingFactoryBean cache = new MethodInvokingFactoryBean();
    	cache.setStaticMethod("org.red5.io.flv.impl.FLVReader.setBufferType");
		cache.setArguments("auto");
    	return cache;
    }
    @Order(28)
    @Bean(name="invokingTwo") 
    public MethodInvokingFactoryBean invoking2(){ 
    	MethodInvokingFactoryBean cache = new MethodInvokingFactoryBean();
    	cache.setStaticMethod("org.red5.io.flv.impl.FLVReader.setBufferSize");
		cache.setArguments("4096");
    	return cache;
    }
    @Order(29)
    @Bean(name="streamExecutor") 
    public ScheduledThreadPoolExecutor streamExecutor(){ 
    	ScheduledThreadPoolExecutor cache = new ScheduledThreadPoolExecutor(16);
    	cache.setMaximumPoolSize(64);
		return cache;
    }
    
    @Value("${subscriberstream.buffer.check.interval}")
    int interval;
    @Value("${subscriberstream.underrun.trigger}")
    int trigger;
    
    @Order(30)
    @Bean(name="playlistSubscriberStream")
    @Lazy
    @Scope("prototype")
    public PlaylistSubscriberStream playlistSubscriberStream(){ 
    	PlaylistSubscriberStream cache = new PlaylistSubscriberStream();
    	cache.setBufferCheckInterval(interval);
    	cache.setUnderrunTrigger(trigger); 
		return cache;
    }
    @Order(31)
    @Bean(name="clientBroadcastStream")
    @Lazy
    @Scope("prototype")
    public ClientBroadcastStream clientBroadcastStream(){ 
    	ClientBroadcastStream cache = new ClientBroadcastStream(); 
		return cache;
    } 
	
	//-----------------------------------------------------------core------------------
    @Order(32)
	@Bean(name="rtmpScheduler")
	@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public ThreadPoolTaskScheduler trmpScheduler(){
		ThreadPoolTaskScheduler poolSched = new ThreadPoolTaskScheduler();
		 
		poolSched.setPoolSize(poolSize);
		poolSched.setDaemon(true);
		poolSched.setWaitForTasksToCompleteOnShutdown(true);
		poolSched.setThreadNamePrefix("RTMPConnectionScheduler-");
		return poolSched;
    }
    @Order(33)
	@Bean(name="messageExecutor")
	@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public ThreadPoolTaskExecutor messageExecutor(){
		ThreadPoolTaskExecutor poolSched = new ThreadPoolTaskExecutor();
		 
		poolSched.setCorePoolSize(corePoolSize);
		poolSched.setMaxPoolSize(maxPoolSize);
		poolSched.setQueueCapacity(queueCapacity);
		poolSched.setDaemon(false);
		poolSched.setWaitForTasksToCompleteOnShutdown(true);
		poolSched.setThreadNamePrefix("RTMPConnectionExecutor-");
		return poolSched;
    }
    @Order(34)
	@Bean(name="deadlockGuardScheduler") 
	@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public ThreadPoolTaskScheduler deadlockGuardScheduler(){
		ThreadPoolTaskScheduler poolSched = new ThreadPoolTaskScheduler();
		 
		poolSched.setPoolSize(deadPoolSize);
		poolSched.setDaemon(false);
		poolSched.setWaitForTasksToCompleteOnShutdown(true);
		poolSched.setThreadNamePrefix("DeadlockGuardScheduler-");
		return poolSched;
    }
    @Order(35)
	@Bean(name="rtmpConnManager") 
	@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public RTMPConnManager rtmpConnManager(){ 
		return new RTMPConnManager();
    }
    @Order(36)
	@Bean(name="rtmpHandler") 
	@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public RTMPHandler rtmpHandler(){
		RTMPHandler poolSched = new RTMPHandler(); 
		poolSched.setServer(server()); 
		poolSched.setStatusObjectService(statusObjectService());
		poolSched.setUnvalidatedConnectionAllowed(true);
		return poolSched;
    }
    @Order(37)
	@Bean(name="rtmpMinaIoHandler") 
	@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public RTMPMinaIoHandler rtmpMinaIoHandler(){
		RTMPMinaIoHandler poolSched = new RTMPMinaIoHandler(); 
		poolSched.setHandler(rtmpHandler()); 
		return poolSched;
    }
	
	@Value("${rtmp.host}")
	String host;
	@Value("${rtmp.port}")
	String port;
	@Value("${rtmp.io_threads}")
	int ioThreads;
	@Value("${rtmp.send_buffer_size}")
	int bufferSize;
	@Value("${rtmp.receive_buffer_size}")
	int receiveBufferSize;
	@Value("${rtmp.traffic_class}")
	int trafficClass;
	@Value("${rtmp.backlog}")
	int backlog;
	@Value("${rtmp.tcp_nodelay}")
	boolean tcpNoDelay;
	@Value("${rtmp.tcp_keepalive}")
	boolean keepAlive;
	@Value("${rtmp.thoughput_calc_interval}")
	int thoughputCalcInterval;
	@Value("${rtmp.default_acceptor}")
	boolean enableDefaultAcceptor;
	@Value("${rtmp.initial_pool_size}")
	int initialPoolSize;
	@Value("${rtmp.max_pool_size}")
	int rmaxPoolSize;
	@Value("${rtmp.max_processor_pool_size}")
	int maxProcessorPoolSize;
	@Value("${rtmp.executor_keepalive_time}")
	int executorKeepAliveTime;
	@Value("${jmx.mina.poll.interval}")
	int minaPollInterval; 
	@Value("${jmx.mina.monitor.enable}")
	boolean enableMinaMonitor;
	@Value("${mina.logfilter.enable}")
	boolean enableMinaLogFilter;
	@Order(38)
	@Bean(name="rtmpTransport",initMethod = "start", destroyMethod = "stop") 
    public RTMPMinaTransport rtmpTransport(){
		RTMPMinaTransport poolSched = new RTMPMinaTransport(); 
		poolSched.setIoHandler(rtmpMinaIoHandler());
		List<String> addresses = new ArrayList<String>();
		addresses.add(host+":"+port);
		poolSched.setAddresses(addresses);
		poolSched.setIoThreads(ioThreads);
		poolSched.setSendBufferSize(bufferSize);
		poolSched.setReceiveBufferSize(receiveBufferSize);
		poolSched.setTrafficClass(trafficClass);
		poolSched.setBacklog(backlog);
		poolSched.setTcpNoDelay(tcpNoDelay);
		poolSched.setKeepAlive(keepAlive);
		poolSched.setThoughputCalcInterval(thoughputCalcInterval);
		poolSched.setEnableDefaultAcceptor(enableDefaultAcceptor);
		poolSched.setInitialPoolSize(initialPoolSize);
		poolSched.setMaxPoolSize(maxPoolSize);
		poolSched.setMaxProcessorPoolSize(maxProcessorPoolSize);
		poolSched.setExecutorKeepAliveTime(executorKeepAliveTime);
		poolSched.setMinaPollInterval(minaPollInterval);
		poolSched.setEnableMinaMonitor(enableMinaMonitor);
		poolSched.setEnableMinaLogFilter(enableMinaLogFilter);
		return poolSched;
    }
	
	@Value("${http.host}")
	String httpHost;
	@Value("${http.io_threads}")
	int httpIoThrads;
	@Value("${http.max_backlog}")
	int httpMaxBacklog;
	@Value("${http.tcp_nodelay}")
	boolean httpTcpNoDelay;
	@Value("${http.receive_buffer_size}")
	int httpReceiveBufferSize;
	@Value("${http.send_buffer_size}")
	int httpSendBufferSize;
	@Value("${http.worker_threads}")
	int httpWorkerThreads;
	@Value("${http.port}")
	private int httpPort;
	@Bean(name="hTTPMinaTransport",initMethod = "start", destroyMethod = "stop") 
    public HTTPMinaTransport hTTPMinaTransport(){
		HTTPMinaTransport poolSched = new HTTPMinaTransport(); 
		poolSched.setIoHandler(hTTPMinaIoHandler());
		poolSched.setHttpHost(httpHost);
		poolSched.setHttpPort(httpPort);
		poolSched.setHttpIoThrads(httpIoThrads);
		poolSched.setHttpMaxBacklog(httpMaxBacklog);
		poolSched.setHttpReceiveBufferSize(httpReceiveBufferSize);
		poolSched.setHttpSendBufferSize(httpSendBufferSize);
		poolSched.setHttpTcpNoDelay(httpTcpNoDelay);
		poolSched.setHttpWorkerThreads(httpWorkerThreads);
		return poolSched;
    }
	
	@Bean(name="hTTPMinaIoHandler") 
	@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public HTTPMinaIoHandler hTTPMinaIoHandler(){
		HTTPMinaIoHandler poolSched = new HTTPMinaIoHandler();  
		return poolSched;
    }
	@Value("${rtmp.ping_interval}")
	int pingInterval;
	@Value("${rtmp.max_inactivity}")
	int maxInactivity;
	@Value("${rtmp.max_handshake_time}")
	int maxHandshakeTimeout;
	@Value("${rtmp.default_server_bandwidth}")
	int defaultServerBandwidth;
	@Value("${rtmp.default_client_bandwidth}")
	int defaultClientBandwidth;
	@Value("${rtmp.client_bandwidth_limit_type}")
	int limitType;
	@Value("${rtmp.bandwidth_detection}")
	boolean bandwidthDetection;
	@Value("${rtmp.max_handling_time}")
	int maxHandlingTimeout;
	@Value("${rtmp.executor.queue_size_to_drop_audio_packets}")
	int executorQueueSizeToDropAudioPackets;
	@Value("${rtmp.channel.initial.capacity}")
	int channelsInitalCapacity;
	@Value("${rtmp.channel.concurrency.level}")
	int channelsConcurrencyLevel;
	@Value("${rtmp.stream.initial.capacity}")
	int streamsInitalCapacity;
	@Value("${rtmp.stream.concurrency.level}")
	int streamsConcurrencyLevel;
	@Value("${rtmp.pending.calls.initial.capacity}")
	int pendingCallsInitalCapacity;
	@Value("${rtmp.pending.calls.concurrency.level}")
	int pendingCallsConcurrencyLevel;
	@Value("${rtmp.reserved.streams.initial.capacity}")
	int reservedStreamsInitalCapacity;
	@Value("${rtmp.reserved.streams.concurrency.level}")
	int reservedStreamsConcurrencyLevel;
	@Order(39)
	@Bean(name="rtmpMinaConnection") 
	@Scope("prototype")
    public RTMPMinaConnection rtmpMinaConnection(){
		RTMPMinaConnection poolSched = new RTMPMinaConnection(); 
		poolSched.setScheduler(trmpScheduler());
		poolSched.setExecutor(messageExecutor());
		poolSched.setDeadlockGuardScheduler(deadlockGuardScheduler());
		poolSched.setPingInterval(pingInterval);
		poolSched.setMaxInactivity(maxInactivity);
		poolSched.setMaxHandshakeTimeout(maxHandshakeTimeout);
		poolSched.setDefaultServerBandwidth(defaultServerBandwidth);
		poolSched.setDefaultClientBandwidth(defaultClientBandwidth);
		poolSched.setLimitType(limitType);
		poolSched.setBandwidthDetection(bandwidthDetection);
		poolSched.setMaxHandlingTimeout(maxHandlingTimeout);
		poolSched.setExecutorQueueSizeToDropAudioPackets(executorQueueSizeToDropAudioPackets);
		poolSched.setChannelsInitalCapacity(channelsInitalCapacity);
		poolSched.setChannelsConcurrencyLevel(channelsConcurrencyLevel);
		poolSched.setStreamsInitalCapacity(streamsInitalCapacity);
		poolSched.setStreamsConcurrencyLevel(streamsConcurrencyLevel);
		poolSched.setPendingCallsInitalCapacity(pendingCallsInitalCapacity);
		poolSched.setPendingCallsConcurrencyLevel(pendingCallsConcurrencyLevel);
		poolSched.setReservedStreamsInitalCapacity(reservedStreamsInitalCapacity);
		poolSched.setReservedStreamsConcurrencyLevel(reservedStreamsConcurrencyLevel);
		return poolSched;
    }
	@Order(40)
	@Bean(name="rtmptHandler",autowire=Autowire.BY_TYPE)
	@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public RTMPTHandler rtmptHandler(){
		RTMPTHandler poolSched = new RTMPTHandler(); 
		poolSched.setCodecFactory(rtmptCodecFactory());
		return poolSched;
    }
	
	@Value("${rtmpt.target_reponse_size}")
	int targetResponseSize;
	@Order(41)
	@Bean(name="rtmptServlet") 
    public RTMPTServlet rtmptServlet(){
		RTMPTServlet poolSched = new RTMPTServlet(); 
		poolSched.setManager(rtmpConnManager());
		poolSched.setHandler(rtmptHandler());
		poolSched.setTargetResponseSize(targetResponseSize);
		return poolSched;
    }
	
	@Value("${rtmpt.ping_interval}")
	int pingIntervalt;
	@Value("${rtmpt.max_inactivity}")
	int maxInactivityt;
	@Value("${rtmpt.max_handshake_time}")
	int maxHandshakeTimeoutt;
	 
	@Value("${rtmpt.max_in_msg_process}")
	int maxInMessagesPerProcess;
	@Value("${rtmpt.max_queue_offer_time}")
	int maxQueueOfferTime;
	@Value("${rtmpt.max_queue_offer_attempts}")
	int maxQueueOfferAttempts; 
	@Order(42)
	@Bean(name="rtmptConnection") 
	@Scope("prototype")
    public RTMPTConnection rtmptConnection(){
		RTMPTConnection poolSched = new RTMPTConnection(); 
		poolSched.setScheduler(trmpScheduler());
		poolSched.setExecutor(messageExecutor());
		poolSched.setDeadlockGuardScheduler(deadlockGuardScheduler());
		poolSched.setPingInterval(pingIntervalt);
		poolSched.setMaxInactivity(maxInactivityt);
		poolSched.setMaxHandshakeTimeout(maxHandshakeTimeoutt);
		poolSched.setMaxInMessagesPerProcess(maxInMessagesPerProcess);
		poolSched.setMaxQueueOfferTime(maxQueueOfferTime);
		poolSched.setMaxQueueOfferAttempts(maxQueueOfferAttempts);
		poolSched.setMaxHandlingTimeout(maxHandlingTimeout);
		poolSched.setExecutorQueueSizeToDropAudioPackets(executorQueueSizeToDropAudioPackets);
		poolSched.setChannelsInitalCapacity(channelsInitalCapacity);
		poolSched.setChannelsConcurrencyLevel(channelsConcurrencyLevel);
		poolSched.setStreamsInitalCapacity(streamsInitalCapacity);
		poolSched.setStreamsConcurrencyLevel(streamsConcurrencyLevel);
		poolSched.setPendingCallsInitalCapacity(pendingCallsInitalCapacity);
		poolSched.setPendingCallsConcurrencyLevel(pendingCallsConcurrencyLevel);
		poolSched.setReservedStreamsInitalCapacity(reservedStreamsInitalCapacity);
		poolSched.setReservedStreamsConcurrencyLevel(reservedStreamsConcurrencyLevel);
		return poolSched;
    }
	//---------------------------------------新增-------------------------
	 
	@Bean(name="rtmpSampleAccess") 
	@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public RtmpSampleAccess rtmpSampleAccess() throws Exception{
		RtmpSampleAccess context = new RtmpSampleAccess(); 
		context.setAudioAllowed(true);
		context.setVideoAllowed(true);
        return context;
    }
	
	@Bean(name="broadcastStream") 
	@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public RecordableBroadcastStream recordableBroadcastStream() throws Exception{
		RecordableBroadcastStream context = new RecordableBroadcastStream();
		context.setCanRecord(false);
		context.setStorePath("e:/temp");
		context.setDurationPerFile(60);
        return context;
    }
	@Bean(name="singleStream") 
	@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public SingleItemSubscriberStream singleItemSubscriberStream() throws Exception{
		SingleItemSubscriberStream context = new SingleItemSubscriberStream(); 
        return context;
    }	
	@Bean(name="fileConsumer") 
	@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public FileConsumer fileConsumer(){
		FileConsumer context = new FileConsumer(); 
        return context;
    }	
	
	//----------------------------------------demo-------------------------
	@Order(43)
	@Bean(name="web.context.oflaDemo") 
	@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public Context contextOflaDemo() throws Exception{
		Context context = new Context();
		context.setScopeResolver(scopeResolver());
		context.setClientRegistry(clientRegistry());
		context.setServiceInvoker(serviceInvoker());
		context.setMappingStrategy(mappingStrategy());
        return context;
    }
	
	@Order(44)
	@Bean(name="web.handler.oflaDemo") 
	@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public Application1 handlerOflaDemo() throws Exception{
		Application1 context = new Application1(); 
        return context;
    }
	 
	@Bean(name="httpApplicationAdapter") 
	@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public HTTPApplicationAdapter httpApplicationAdapter(){
		HTTPApplicationAdapter context = new HTTPApplicationAdapter(); 
        return context;
    }
	
	
	@Order(45)
	@Bean(name="web.scope.oflaDemo") 
    public WebScope scopeOflaDemo() throws Exception{
		WebScope context = new WebScope(); 
		context.setServer(server());
		context.setParent(globalScope()); 
		context.setContext(contextOflaDemo());
		context.setHandler(handlerOflaDemo());
		context.setHttpApplicationAdapter(httpApplicationAdapter());
		context.setContextPath("/oflaDemo");
		context.setVirtualHosts("*, localhost, localhost:5080, 127.0.0.1:5080");
        return context;
    }
	
	
	
}
 