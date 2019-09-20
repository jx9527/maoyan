package org.red5.demo;

import java.util.Map;

import org.red5.server.adapter.ApplicationAdapter;
import org.red5.server.api.IConnection;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IServerStream;
import org.red5.server.net.http.stream.MpegtsSegmenterService;
import org.red5.server.net.udp.MulticastOutgoingService;
import org.red5.server.net.udp.UDPDatagramConfig;
import org.red5.server.stream.ClientBroadcastStream;


public class Application1 extends ApplicationAdapter {

	private IScope appScope;

	private IServerStream serverStream;

	/** {@inheritDoc} */
	@Override
	public boolean appStart(IScope app) {
		super.appStart(app);
		log.info("oflaDemo appStart");
		System.out.println("oflaDemo appStart");
		appScope = app;
		//registerStreamPlaybackSecurity(new NamePlaybackSecurity());
		//registerStreamPublishSecurity(new NamePublishSecurity());
		return true;
	}

	/** {@inheritDoc} */
	@Override
	public boolean appConnect(IConnection conn, Object[] params) {

		System.out.println(conn);

		log.info("oflaDemo appConnect");
		IScope appScope = conn.getScope();
		log.debug("App connect called for scope: {}", appScope.getName());
		// getting client parameters
		Map<String, Object> properties = conn.getConnectParams();
		//if (log.isDebugEnabled()) {
			for (Map.Entry<String, Object> e : properties.entrySet()) {
				log.debug("Connection property: {} = {}", e.getKey(), e.getValue());
				System.out.println( e.getKey()+" "+e.getValue());
			}
		//}

		// Trigger calling of "onBWDone", required for some FLV players
		// commenting out the bandwidth code as it is replaced by the mina
		// filters
		// measureBandwidth(conn);
		// if (conn instanceof IStreamCapableConnection) {
		// IStreamCapableConnection streamConn = (IStreamCapableConnection)
		// conn;
		// SimpleConnectionBWConfig bwConfig = new SimpleConnectionBWConfig();
		// bwConfig.getChannelBandwidth()[IBandwidthConfigure.OVERALL_CHANNEL] =
		// 1024 * 1024;
		// bwConfig.getChannelInitialBurst()[IBandwidthConfigure.OVERALL_CHANNEL]
		// =
		// 128 * 1024;
		// streamConn.setBandwidthConfigure(bwConfig);
		// }

		// if (appScope == conn.getScope()) {
		// serverStream = StreamUtils.createServerStream(appScope, "live0");
		// SimplePlayItem item = new SimplePlayItem();
		// item.setStart(0);
		// item.setLength(10000);
		// item.setName("on2_flash8_w_audio");
		// serverStream.addItem(item);
		// item = new SimplePlayItem();
		// item.setStart(20000);
		// item.setLength(10000);
		// item.setName("on2_flash8_w_audio");
		// serverStream.addItem(item);
		// serverStream.start();
		// try {
		// serverStream.saveAs("aaa", false);
		// serverStream.saveAs("bbb", false);
		// } catch (Exception e) {}
		// }

		return super.appConnect(conn, params);
	}

	/** {@inheritDoc} */
	@Override
	public void appDisconnect(IConnection conn) {
		log.info("oflaDemo appDisconnect");
		if (appScope == conn.getScope() && serverStream != null) {
			serverStream.close();
		}
		super.appDisconnect(conn);
	}
 
	public IConnection iconn;
	public IScope iscope;
	public ClientBroadcastStream istream;// 用来接受flash上传的stream的类

	@Override
	public void streamPublishStart(IBroadcastStream stream) {
		//添加hls监听
		MpegtsSegmenterService ss = MpegtsSegmenterService.getInstance();
		stream.addStreamListener(ss);
		
//		MulticastOutgoingService ms = MulticastOutgoingService.getInstance();
//		UDPDatagramConfig config = new UDPDatagramConfig();
//		config.setReceiveBufferSize(8192);
//		config.setSendBufferSize(8192);
//		ms.register(stream, config, "0.0.0.0", 1234);
//		stream.addStreamListener(ms);
		super.streamPublishStart(stream);
		
		
		/*String streamName = stream.getPublishedName();
		 
		iconn = Red5.getConnectionLocal();// 得到当前的连接
		iscope = iconn.getScope();// 一组连入服务器的客户
		this.connect(iconn, iscope, null);
		System.out.println("connection Established!");
		// 注意stream name，在flash端也需要匹配
		istream = (ClientBroadcastStream) this.getBroadcastStream(iscope, streamName);
		System.out.println(
				"The publisher's name is: " + stream.getPublishedName() + ", created at: " + stream.getCreationTime());
		System.out.println("the stream Name is: " + streamName);
		try { 
			 
			//istream.saveAs(streamName,false);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/

		super.streamPublishStart(stream);
	}
	@Override
	public void streamBroadcastClose(IBroadcastStream stream) {
		
		MpegtsSegmenterService ss = MpegtsSegmenterService.getInstance();
		stream.removeStreamListener(ss);
		ss.removeSegment(stream.getScope().getName(), stream.getPublishedName());
//		
//		MulticastOutgoingService ms = MulticastOutgoingService.getInstance();
//		stream.removeStreamListener(ms);
//		ms.unregister(stream.getPublishedName());
 		
		
		super.streamBroadcastClose(stream);
		
	}
}
