package org.red5.server.net.rtsp;

import java.net.InetSocketAddress;
import java.util.Map;

import org.apache.mina.core.session.IoSession;

import org.red5.server.BaseConnection;
import org.red5.server.api.stream.IClientBroadcastStream;
import org.red5.server.api.stream.IClientStream;
import org.red5.server.api.stream.IPlaylistSubscriberStream;
import org.red5.server.api.stream.ISingleItemSubscriberStream;
import org.red5.server.api.stream.IStreamCapableConnection;
import org.red5.server.net.http.stream.CustomSingleItemSubStream;
import org.red5.server.net.rtp.RTPPlayer;
import org.red5.server.net.udp.UDPPortManager;
import org.red5.server.net.proxy.RTSPPushProxyStream;

/**
 * RTSP Connection
 * @author pengliren
 *
 */
public class RTSPMinaConnection extends BaseConnection implements IStreamCapableConnection {

	public static final String RTSP_CONNECTION_KEY = "rtsp.conn";
	
	public final static int PLAY_TYPE_UDP = 0;
	
	public final static int PLAY_TYPE_TCP = 1;
	
	private int playType;
	
	private boolean isClosed = false;
	
	private RTPPlayer rtpConnector;
	
	private int[] videoPairPort;
	
	private int[] audioPairPort;

	private IoSession rtspSession;
	
	private boolean isMpegts = false;
	
	private boolean isLive = false;
	
	public RTSPMinaConnection(IoSession session) {

		this.rtspSession = session;
		this.sessionId = String.valueOf(session.getId());
	}

	public void close() {
		
		if(isClosed == false) {
			isClosed = true;
		} else {
			return;
		}
		
		RTSPConnectionConsumer rtspConsumer = (RTSPConnectionConsumer)getAttribute("rtspConsumer");
		if (rtspConsumer != null) {
			if (rtpConnector != null) {
				rtspConsumer.removeStreamListener(rtpConnector);
				rtpConnector.close();
			}
		}

		UDPPortManager udpPortMgr = UDPPortManager.getInstance();
		if (videoPairPort != null && videoPairPort.length == 2) {
			RTSPMinaTransport.RTP_VIDEO_ACCEPTOR.unbind(new InetSocketAddress("127.0.0.1", videoPairPort[0]));
			udpPortMgr.releaseUDPPortPair(videoPairPort[0]);
			RTSPMinaTransport.RTCP_VIDEO_ACCEPTOR.unbind(new InetSocketAddress("127.0.0.1", videoPairPort[1]));
			udpPortMgr.releaseUDPPortPair(videoPairPort[1]);
		}

		if (audioPairPort != null && audioPairPort.length == 2) {
			RTSPMinaTransport.RTP_AUDIO_ACCEPTOR.unbind(new InetSocketAddress("127.0.0.1", audioPairPort[0]));
			udpPortMgr.releaseUDPPortPair(audioPairPort[0]);
			RTSPMinaTransport.RTCP_AUDIO_ACCEPTOR.unbind(new InetSocketAddress("127.0.0.1", audioPairPort[1]));
			udpPortMgr.releaseUDPPortPair(audioPairPort[1]);
		}
		
		if(rtpConnector != null) {
			if(rtpConnector.getVideoRtpPacketizer() != null) {
				InetSocketAddress videoAddress = rtpConnector.getVideoRtpPacketizer().getRtcpAddress();
				if (videoAddress != null) {
					RTSPCore.rtpSocketMaps.remove(String.format("%s:%d", videoAddress.getAddress().getHostAddress(), videoAddress.getPort()));
				}
			}

			if(rtpConnector.getAudioRtpPacketizer() != null) {
				InetSocketAddress audioAddress = rtpConnector.getAudioRtpPacketizer().getRtcpAddress();
				if (audioAddress != null) {
					RTSPCore.rtpSocketMaps.remove(String.format("%s:%d", audioAddress.getAddress().getHostAddress(), audioAddress.getPort()));
				}
			}
		}
		
		// check play stram is null
		CustomSingleItemSubStream pullStream = (CustomSingleItemSubStream)getAttribute("rtspStream");
		
		// check publish stream is null
		RTSPPushProxyStream pushStream = (RTSPPushProxyStream)getAttribute("pushStream");
		
		// we must colse play stream
		if(pullStream != null) {
			pullStream.close();
		}
		
		// we must stop publish stream
		if(pushStream != null) {
			pushStream.stop();
		}			
		
		// clear rtsp tunnel
		if(getAttribute("sessioncookie") != null) RTSPTunnel.RTSP_TUNNEL_CONNS.remove(getAttribute("sessioncookie"));
		
		if(rtspSession != null) rtspSession.closeNow();
	}
	
	public int getPlayType() {
		return playType;
	}

	public void setPlayType(int playType) {
		this.playType = playType;
	}
	
	public RTPPlayer getRtpConnector() {
		return rtpConnector;
	}

	public void setRtpConnector(RTPPlayer rtpConnector) {
		this.rtpConnector = rtpConnector;
	}

	public IoSession getRtspSession() {
		return rtspSession;
	}

	public boolean isMpegts() {
		return isMpegts;
	}

	public void setMpegts(boolean isMpegts) {
		this.isMpegts = isMpegts;
	}

	public void setVideoPairPort(int[] videoPairPort) {
		this.videoPairPort = videoPairPort;
	}

	public void setAudioPairPort(int[] audioPairPort) {
		this.audioPairPort = audioPairPort;
	}
	
	public void write(Object out) {
		
		rtspSession.write(out);
	}

	public boolean isLive() {
		return isLive;
	}

	public void setLive(boolean isLive) {
		this.isLive = isLive;
	}

	@Override
	public Encoding getEncoding() {
		return null;
	}

	@Override
	public void ping() {
		
	}

	@Override
	public int getLastPingTime() {

		return 0;
	}

	@Override
	public void setBandwidth(int mbits) {
		
	}

	@Override
	public long getReadBytes() {
		return 0;
	}

	@Override
	public long getWrittenBytes() {
		return 0;
	} 

	@Override
	public String getProtocol() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Number reserveStreamId() throws IndexOutOfBoundsException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Number reserveStreamId(Number streamId) throws IndexOutOfBoundsException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void unreserveStreamId(Number streamId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deleteStreamById(Number streamId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public IClientStream getStreamById(Number streamId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ISingleItemSubscriberStream newSingleItemSubscriberStream(Number streamId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IPlaylistSubscriberStream newPlaylistSubscriberStream(Number streamId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IClientBroadcastStream newBroadcastStream(Number streamId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Number, IClientStream> getStreamsMap() {
		// TODO Auto-generated method stub
		return null;
	}	
}
