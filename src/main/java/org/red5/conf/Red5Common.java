/*package org.red5.conf;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.red5.cache.impl.NoCacheImpl;
import org.red5.io.CachingFileKeyFrameMetaCache;
import org.red5.io.flv.impl.FLV;
import org.red5.io.flv.impl.FLVReader;
import org.red5.io.mp3.impl.MP3Reader;
import org.red5.server.Server;
import org.red5.server.api.service.IStreamableFileService;
import org.red5.server.net.remoting.RemotingClient;
import org.red5.server.net.remoting.codec.RemotingCodecFactory;
import org.red5.server.net.rtmp.codec.RTMPMinaProtocolDecoder;
import org.red5.server.net.rtmp.codec.RTMPMinaProtocolEncoder;
import org.red5.server.net.rtmp.status.StatusObjectService;
import org.red5.server.net.rtmpt.codec.RTMPTCodecFactory;
import org.red5.server.scheduling.JDKSchedulingService;
import org.red5.server.service.flv.impl.FLVService;
import org.red5.server.service.m4a.impl.M4AService;
import org.red5.server.service.mp3.impl.MP3Service;
import org.red5.server.service.mp4.impl.MP4Service;
import org.red5.server.so.SharedObjectService;
import org.red5.server.stream.ClientBroadcastStream;
import org.red5.server.stream.ConsumerService;
import org.red5.server.stream.PlaylistSubscriberStream;
import org.red5.server.stream.ProviderService;
import org.red5.server.stream.StreamService;
import org.red5.server.stream.StreamableFileFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.MethodInvokingFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class Red5Common{
	
	@Value("${rtmpt.encoder_base_tolerance}")
	long baseTolerance;
	@Value("${rtmpt.encoder_drop_live_future}")
	boolean dropLiveFuture;
	
	@Bean(name="red5.server") 
    public Server server(){
        return new Server();
    }
	
	@Bean(name="statusObjectService") 
    public StatusObjectService statusObjectService(){
        return new StatusObjectService();
    }
	
	
	@Bean(name="minaEncoder") 
	@Scope("prototype")
    public RTMPMinaProtocolEncoder minaEncoder(){
        return new RTMPMinaProtocolEncoder();
    }
	@Bean(name="minaDecoder") 
    public RTMPMinaProtocolDecoder minaDecoder(){
        return new RTMPMinaProtocolDecoder();
    }
	
	@Bean(name="rtmptCodecFactory") 
    public RTMPTCodecFactory rtmptCodecFactory(){
		RTMPTCodecFactory factory = new RTMPTCodecFactory();
		<property name="baseTolerance" value="${rtmpt.encoder_base_tolerance}" />
		<property name="dropLiveFuture" value="${rtmpt.encoder_drop_live_future}" />
		factory.setBaseTolerance(baseTolerance);
		factory.setDropLiveFuture(dropLiveFuture);
		factory.init();
		return factory;
    }
	
	@Bean(name="remotingCodecFactory") 
    public RemotingCodecFactory remotingCodecFactory(){
		RemotingCodecFactory factory = new RemotingCodecFactory(); 
		factory.init();
		return factory;
    }
	
	@Bean(name="streamableFileFactory") 
    public StreamableFileFactory streamableFileFactory(){
		StreamableFileFactory factory = new StreamableFileFactory(); 
		Set<IStreamableFileService> services = new HashSet<>();
		<list>
		<bean id="flvFileService" class="org.red5.server.service.flv.impl.FLVService">
			<property name="generateMetadata" value="true" />
			</bean>
			<bean id="mp3FileService" class="org.red5.server.service.mp3.impl.MP3Service" />
			<bean id="mp4FileService" class="org.red5.server.service.mp4.impl.MP4Service" />
			<bean id="m4aFileService" class="org.red5.server.service.m4a.impl.M4AService" />
		</list>
		FLVService  flv = new FLVService();
		flv.setGenerateMetadata(true);
		services.add(new MP3Service());
		services.add(new MP4Service());
		services.add(new M4AService()); 
		factory.setServices(services);
		return factory;
    }
	
	@Value("${so.max.events.per.update}")
	int maxUpdate;
	
	@Bean(name="sharedObjectService") 
    public SharedObjectService sharedObjectService(){
		SharedObjectService factory = new SharedObjectService(); 
		factory.setMaximumEventsPerUpdate(maxUpdate);
		factory.setPersistenceClassName("org.red5.server.persistence.FilePersistence");
		factory.setScheduler(poolScheduler());
		return factory;
    } 
	@Value("${so.scheduler.pool_size}")
	int poolSize;
    public ThreadPoolTaskScheduler poolScheduler(){
    	ThreadPoolTaskScheduler factory = new ThreadPoolTaskScheduler(); 
    	factory.setPoolSize(poolSize);
    	factory.setWaitForTasksToCompleteOnShutdown(false);
    	factory.setDaemon(true);
    	factory.setThreadNamePrefix("SharedObjectScheduler-");
		return factory;
    }
    
    @Bean(name="streamService") 
    public StreamService streamService(){ 
		return new StreamService();
    } 
    @Bean(name="providerService") 
    public ProviderService providerService(){ 
		return new ProviderService();
    }
    
    @Bean(name="consumerService") 
    public ConsumerService consumerService(){ 
		return new ConsumerService();
    }
    
    @Bean(name="schedulingService") 
    public JDKSchedulingService schedulingService(){ 
		return new JDKSchedulingService();
    }
    
    @Bean(name="remotingClient") 
    public RemotingClient remotingClient(){ 
    	RemotingClient client = new RemotingClient();
    	client.setPoolSize(2);
		return client;
    }
    
    @Bean(name="object.cache") 
    public NoCacheImpl objectCache(){ 
		return NoCacheImpl.getInstance();
    }
    
    @Value("${keyframe.cache.entry.max}")
    int entryMax;
    
    @Bean(name="keyframe.cache") 
    public CachingFileKeyFrameMetaCache keyframeCache(){ 
    	CachingFileKeyFrameMetaCache cache = new CachingFileKeyFrameMetaCache();
    	cache.setMaxCacheEntry(entryMax);
		return cache;
    }
    
    @Bean(name="flv.impl") 
    public FLV flvImpl(){ 
    	FLV cache = new FLV();
    	cache.setCache(objectCache());
		return cache;
    }
    
    @Bean(name="flvreader.impl") 
    public FLVReader flvreaderImpl(){ 
    	FLVReader cache = new FLVReader();
    	cache.setKeyFrameCache(keyframeCache());
		return cache;
    }
    
    @Bean(name="mp3reader.impl") 
    public MP3Reader mp3readerImpl(){ 
    	MP3Reader cache = new MP3Reader();
    	cache.setFrameCache(keyframeCache());
		return cache;
    }
     
    public MethodInvokingFactoryBean invoking1(){ 
    	MethodInvokingFactoryBean cache = new MethodInvokingFactoryBean();
    	cache.setStaticMethod("org.red5.io.flv.impl.FLVReader.setBufferType");
		cache.setArguments("auto");
    	return cache;
    }
    
    public MethodInvokingFactoryBean invoking2(){ 
    	MethodInvokingFactoryBean cache = new MethodInvokingFactoryBean();
    	cache.setStaticMethod("org.red5.io.flv.impl.FLVReader.setBufferSize");
		cache.setArguments("4096");
    	return cache;
    }
    
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
    @Bean(name="playlistSubscriberStream")
    @Lazy
    @Scope("prototype")
    public PlaylistSubscriberStream playlistSubscriberStream(){ 
    	PlaylistSubscriberStream cache = new PlaylistSubscriberStream();
    	cache.setBufferCheckInterval(interval);
    	cache.setUnderrunTrigger(trigger); 
		return cache;
    }
    
    @Bean(name="clientBroadcastStream")
    @Lazy
    @Scope("prototype")
    public ClientBroadcastStream clientBroadcastStream(){ 
    	ClientBroadcastStream cache = new ClientBroadcastStream(); 
		return cache;
    }
}
*/