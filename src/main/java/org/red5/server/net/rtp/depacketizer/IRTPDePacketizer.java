package org.red5.server.net.rtp.depacketizer;

import org.apache.mina.core.buffer.IoBuffer;

import org.red5.server.net.rtp.RTPPacket;


/**
 * RTP DePacketizer Interface
 * @author pengliren
 *
 */
public interface IRTPDePacketizer {

	public IoBuffer handleRTPPacket(RTPPacket packet);
	
	public void handleRTCPPacket();
}
