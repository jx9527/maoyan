package org.red5.websocket;


import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.logging.Logger;

public class MyWebSocketClient extends WebSocketClient {

    private static final Logger LOGGER = (Logger) LoggerFactory.getLogger(MyWebSocketClient.class);

    public MyWebSocketClient(URI serverUri){
        super(serverUri);
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        LOGGER.info("----webSocketClient onOpen----");

    }

    @Override
    public void onClose(int i, String s, boolean b) {
        LOGGER.info("----webSocketClient onClose----");
    }

    @Override
    public void onError(Exception e) {
        LOGGER.info("----webSocketClient onError----");
    }

    @Override
    public void onMessage(String s) {
        LOGGER.info("----webSocketClient接收到服务端消息----");

    }
}
