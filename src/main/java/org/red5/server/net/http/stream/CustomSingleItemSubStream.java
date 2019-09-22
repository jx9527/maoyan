package org.red5.server.net.http.stream;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.codec.AudioCodec;
import org.red5.codec.IAudioStreamCodec;
import org.red5.codec.IStreamCodecInfo;
import org.red5.codec.IVideoStreamCodec;
import org.red5.codec.VideoCodec;
import org.red5.io.IStreamableFile;
import org.red5.io.IStreamableFileFactory;
import org.red5.io.IStreamableFileService;
import org.red5.io.ITag;
import org.red5.io.ITagReader;
import org.red5.io.StreamableFileFactory;
import org.red5.io.flv.FLVUtils;
import org.red5.server.ScopeContextBean;
import org.red5.server.api.Red5;
import org.red5.server.api.scheduling.ISchedulingService;
import org.red5.server.api.scope.IBasicScope;
import org.red5.server.api.scope.IBroadcastScope;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.scope.ScopeType;
import org.red5.server.api.stream.IClientBroadcastStream;
import org.red5.server.api.stream.IClientStream;
import org.red5.server.api.stream.IPlayItem;
import org.red5.server.api.stream.IStreamCapableConnection;
import org.red5.server.api.stream.StreamState;
import org.red5.server.messaging.IMessageOutput;
import org.red5.server.messaging.IPipe;
import org.red5.server.messaging.InMemoryPushPushPipe;
import org.red5.server.scheduling.QuartzSchedulingService;
import org.red5.server.stream.IConsumerService;
import org.red5.server.stream.IProviderService;
import org.red5.server.stream.IProviderService.INPUT_TYPE;
import org.red5.server.stream.PlayEngine;
import org.red5.server.stream.SingleItemSubscriberStream;
import org.red5.server.stream.StreamNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomSingleItemSubStream extends SingleItemSubscriberStream {

	private static final Logger log = LoggerFactory.getLogger(CustomSingleItemSubStream.class);
	
	private ICustomPushableConsumer consumer;
	private IPlayItem item;
	private boolean isFailure;
	private boolean isLive;
	
	public CustomSingleItemSubStream(IScope scope, final ICustomPushableConsumer consumer) {
		
		this.setScope(scope);
		this.consumer = consumer;
		this.setClientBufferDuration(2000);
		this.setConnection((IStreamCapableConnection)consumer.getConnection());
		Red5.setConnectionLocal(consumer.getConnection());
	}
	
	@Override
	public IStreamCapableConnection getConnection() {
		return  (IStreamCapableConnection)consumer.getConnection();
	}
	
	@Override
	public void start() {
		
		// ensure the play engine exists
		if (engine == null) {
			IScope scope = getScope();
			if (scope != null) {
				ISchedulingService schedulingService = QuartzSchedulingService.getInstance();
				IConsumerService consumerService = new IConsumerService() {
					@Override
					public IMessageOutput getConsumerOutput(IClientStream stream) {
						IPipe pipe = new InMemoryPushPushPipe();
						pipe.subscribe(consumer, null);
						return pipe;
					}

				};
				IProviderService providerService = (IProviderService) scope.getContext().getBean(ScopeContextBean.PROVIDERSERVICE_BEAN);
				engine = new PlayEngine.Builder(this, schedulingService, consumerService, providerService).build();
			} 
		}
		// set buffer check interval
		engine.setBufferCheckInterval(1000);
		// set underrun trigger
		engine.setUnderrunTrigger(5000);
		engine.setMaxPendingVideoFrames(2000);
		// Start playback engine
		engine.start();
		isFailure = false;
	}
	
	@Override
	public void play() throws IOException {
		try {
			engine.play(item);
		} catch (IllegalStateException e) {
			log.info(e.getMessage());
			isFailure = true;
		} catch (StreamNotFoundException e) {
			log.info(e.getMessage());
			isFailure = true;
		}
	}
	
	@Override
	public void close() {
		
		if(state.get() != StreamState.CLOSED) {
			super.close();
		}
	}
	
	@Override
	public void onChange(StreamState state, Object... changed) {
		
		super.onChange(state, changed);		
		if(state == StreamState.STOPPED) {
			consumer.getConnection().close();					
		} else if(state == StreamState.PLAYING) {
			isLive = (Boolean) changed[1];
		}
	}

	@Override
	public void setPlayItem(IPlayItem item) {
		this.item = item;
		super.setPlayItem(item);
	}

	public boolean isFailure() {
		return isFailure;
	}

	public ICustomPushableConsumer getConsumer() {
		return consumer;
	}
	
	public boolean isLive() {
		
		return isLive;
	}
	
	public int getLastPlayTs() {
		
		int ts = engine.getLastMessageTimestamp();
		return ts;
	}
	
	public INPUT_TYPE lookupStreamInput() {
		IScope scope = getScope();
		IProviderService providerService = (IProviderService) scope.getContext().getBean(ScopeContextBean.PROVIDERSERVICE_BEAN);
		return providerService.lookupProviderInput(scope, item.getName(), 0);
	}	
	
	/**
	 * 
	 * @param videoConfig
	 * @param audioConfig
	 * @return
	 * @throws IOException 
	 */
	public void getConfig(IoBuffer videoConfig, IoBuffer audioConfig, AtomicLong duration) throws IOException {
		
		
		IScope scope = getScope();
		IProviderService providerService = (IProviderService) scope.getContext().getBean(ScopeContextBean.PROVIDERSERVICE_BEAN);
		INPUT_TYPE result = lookupStreamInput();
		
		if(result == INPUT_TYPE.VOD) { // reader file get video and audio config
			File file = providerService.getVODProviderFile(scope, item.getName());
			if(file != null && file.exists()) {
				IStreamableFileFactory factory = StreamableFileFactory.getInstance();
				IStreamableFileService service = factory.getService(file);
				boolean audioChecked = false;
				boolean videoChecked = false;
				IStreamableFile streamFile = service.getStreamableFile(file);		
				ITagReader reader = streamFile.getReader();
				duration.set(reader.getDuration());
				ITag tag;
				int codec;
				for (int i = 0; i < 10; i++) {
					if (audioChecked && videoChecked) break;
					tag = reader.readTag();
					if (tag == null) return;
					if (ITag.TYPE_VIDEO == tag.getDataType()) {
						codec = FLVUtils.getVideoCodec(tag.getBody().get(0));
						if (codec == VideoCodec.AVC.getId() && tag.getBody().get(1) == 0x00) {
							videoChecked = true;
							videoConfig.put(tag.getBody());
							videoConfig.flip();
						}
					} else if (ITag.TYPE_AUDIO == tag.getDataType()) {
						codec = FLVUtils.getAudioCodec(tag.getBody().get(0));
						if ((codec == AudioCodec.AAC.getId() && tag.getBody().get(1) == 0x00) || codec == AudioCodec.MP3.getId()) {
							audioChecked = true;
							audioConfig.put(tag.getBody());
							audioConfig.flip();
						}
					}
				}
				reader.close();
			}
		} else if(result == INPUT_TYPE.LIVE) { // get live video and audio config
			//IBasicScope basicScope = scope.getBasicScope(ScopeType.APPLICATION, item.getName());
			//IBasicScope basicScope1 = scope.getBasicScope(ScopeType.GLOBAL, item.getName());
			IBasicScope basicScope = scope.getBasicScope(ScopeType.BROADCAST, item.getName()); 
			
			
			IClientBroadcastStream bs = null;
			if(basicScope != null) {
				bs = (IClientBroadcastStream)basicScope.getAttribute(IBroadcastScope.STREAM_ATTRIBUTE);
			}
			
			if(bs != null) {
				IStreamCodecInfo codecInfo =  bs.getCodecInfo();
				IVideoStreamCodec videoCodecInfo = null;
				IAudioStreamCodec audioCodecInfo = null;
				if(codecInfo != null) {
					videoCodecInfo = codecInfo.getVideoCodec();
					audioCodecInfo = codecInfo.getAudioCodec();
				}
				
				if (videoCodecInfo != null && videoCodecInfo.getDecoderConfiguration() != null) {
					videoConfig.put(videoCodecInfo.getDecoderConfiguration());
					videoConfig.flip();
				}

				if (audioCodecInfo != null && audioCodecInfo.getDecoderConfiguration() != null) {
					audioConfig.put(audioCodecInfo.getDecoderConfiguration());
					audioConfig.flip();
				}
			}
		}
	}
	
	public IPlayItem getPlayItem() {
		return item;
	}
}
