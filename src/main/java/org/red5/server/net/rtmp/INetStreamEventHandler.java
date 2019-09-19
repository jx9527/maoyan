package org.red5.server.net.rtmp;

import org.red5.server.net.rtmp.event.Notify;

public interface INetStreamEventHandler {
	void onStreamEvent(Notify notify);
}
