package org.red5.server.net.rtp.packetizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.red5.io.utils.BufferUtil;
import org.red5.io.utils.BufferUtils;
import org.red5.server.util.NtpTimeStampUtil;
import org.red5.server.util.SystemTimer;

/**
 * RTP Packetizer RTCP Sender
 * @author pengliren
 *
 */
public class RTPPacketizerRTCPSender extends RTPPacketizerRTCPSenderBase {

	private static Logger log = LoggerFactory.getLogger(RTPPacketizerRTCPSender.class);
	
	public static final int RTCP_FREQUENCY = 5000;
	public static final int RTCP_FREQUENCY_FREQ = 1000;
	public static final byte[] SR_PACKET = { (byte) 0x80, (byte) 0xC8, 0x00,
			0x06, 0x00, 0x00, 0x60, 0x4E, (byte) 0xCD, 0x64, 0x1D, (byte) 0xEE,
			0x60, 0x00, 0x02, 0x22, 0x00, 0x00, 0x08, (byte) 0x9A, 0x00, 0x00,
			0x00, 0x01, 0x00, 0x00, 0x02, (byte) 0xF8 };
	protected long lastSentRTCP = -1L;
	protected long sendCount = 0L;
	private long createTime = System.currentTimeMillis();
	private long ntpRTOffset = -1L;

	@Override
	public void sendRTCP(IRTPPacketizer packetizer, long ts) {

		try {
			long cts = SystemTimer.currentTimeMillis();
			int frequency = this.sendCount < 3L ? RTCP_FREQUENCY_FREQ : RTCP_FREQUENCY;
			if (lastSentRTCP == -1L || cts - lastSentRTCP > frequency) {
				lastSentRTCP = cts;
				sendCount += 1L;
				byte[] data = new byte[SR_PACKET.length];
				System.arraycopy(SR_PACKET, 0, data, 0, data.length);
				long ssrc = packetizer.getSsrc();
				int timeScale = packetizer.getTimescale();
				long timecode = Math.round(ts * timeScale / 1000.0D);
				long ntp = getNormalizedNTPTimecode(ts);
				long pcount = packetizer.getPacketCount();
				long ocount = packetizer.getRTCPOctetCount();
				int n = 4;
				BufferUtil.longToByteArray(ssrc, data, n, 4);
				n += 4;
				BufferUtil.longToByteArray(ntp, data, n, 8);
				n += 8;
				BufferUtil.longToByteArray(timecode, data, n, 4);
				n += 4;
				BufferUtil.longToByteArray(pcount, data, n, 4);
				n += 4;
				BufferUtil.longToByteArray(ocount, data, n, 4);
				sendRCTPMessage(data, 0, data.length);
			}
		} catch (Exception e) {
			log.error("send rtcp sr exception : ", e.getMessage());
		}
	}

	private long getNormalizedNTPTimecode(long ts) {

		long tempTs = ts;
		if (ntpRTOffset == -1L) {
			ntpRTOffset = ts;
		}
		tempTs = createTime + (ts - ntpRTOffset);
		return NtpTimeStampUtil.toNTPTime(tempTs);
	}
}
