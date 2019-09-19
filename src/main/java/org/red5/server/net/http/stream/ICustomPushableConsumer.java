package org.red5.server.net.http.stream;

import org.red5.server.api.IConnection;
import org.red5.server.messaging.IPushableConsumer;

public interface ICustomPushableConsumer extends IPushableConsumer {

	IConnection getConnection();
}
