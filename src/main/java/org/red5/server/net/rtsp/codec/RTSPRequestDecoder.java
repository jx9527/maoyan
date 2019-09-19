package org.red5.server.net.rtsp.codec;

import org.red5.server.net.http.message.DefaultHttpRequest;
import org.red5.server.net.http.message.HTTPMessage;
import org.red5.server.net.rtsp.message.RTSPMethods;
import org.red5.server.net.rtsp.message.RTSPVersions;

/**
 * RTSP Request Decoder
 * @author pengliren
 *
 */
public class RTSPRequestDecoder extends RTSPMessageDecoder {
	
	@Override
	protected HTTPMessage createMessage(String[] initialLine) throws Exception {

		return new DefaultHttpRequest(RTSPVersions.valueOf(initialLine[2]),
                RTSPMethods.valueOf(initialLine[0]), initialLine[1]);
	}
}
