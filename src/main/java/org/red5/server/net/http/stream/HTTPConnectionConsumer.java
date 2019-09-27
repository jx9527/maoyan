package org.red5.server.net.http.stream;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.utils.IOUtils;
import org.red5.server.api.stream.IStreamPacket;
import org.red5.server.messaging.IMessage;
import org.red5.server.messaging.IMessageComponent;
import org.red5.server.messaging.IPipe;
import org.red5.server.messaging.OOBControlMessage;
import org.red5.server.net.http.HTTPMinaConnection;
import org.red5.server.net.rtmp.status.StatusCodes;
import org.red5.server.stream.message.RTMPMessage;
import org.red5.server.stream.message.StatusMessage;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HTTPConnectionConsumer implements ICustomPushableConsumer {
	
	private HTTPMinaConnection conn;
	
	private boolean closed = false;
	
	private boolean inited = false;
	
	private static IoBuffer header = IoBuffer.allocate(13);
	
	static {
		// write flv header
    	header.put("FLV".getBytes());
    	header.put(new byte[] { 0x01, 0x05 });
    	header.putInt(0x09);
    	header.putInt(0x00);
    	header.flip();
	}
    
    public HTTPConnectionConsumer(HTTPMinaConnection conn) {
		this.conn = conn;    		
	}
    
	@Override
	public void onOOBControlMessage(IMessageComponent source, IPipe pipe,
			OOBControlMessage oobCtrlMsg) {
		if ("ConnectionConsumer".equals(oobCtrlMsg.getTarget())) {
			if ("pendingVideoCount".equals(oobCtrlMsg.getServiceName())) {
				long pendings = conn.getPendingMessages();
				if(pendings > 500){
					log.info("http pending packet:{}", pendings);
					oobCtrlMsg.setResult(pendings);
				} else if(pendings > 1000) {
					log.info("http pending packet > 1000, network is bad");
					closed = true;
				}			
			}
		}
	}

	/**
	 * 最终发送消息的方法
	 */
	@Override
	public void pushMessage(IPipe pipe, IMessage message) {
		
		if(!inited) {
			//new byte{}{0x46, 0x4c, 0x56, 0x01, 0x05, 0x00, 0x00, 0x00, 0x09}
			conn.write(header.asReadOnlyBuffer());
			inited = true;
		}
		
		if (message instanceof RTMPMessage) {
			if (((RTMPMessage) message).getBody() instanceof IStreamPacket) {
				IStreamPacket packet = (IStreamPacket) (((RTMPMessage) message).getBody());
				if (packet.getData() != null) {
					int bodySize = packet.getData().limit();
					IoBuffer data = IoBuffer.allocate(bodySize+16);
					data.put(packet.getDataType());
					IOUtils.writeMediumInt(data, bodySize);
					IOUtils.writeExtendedMediumInt(data,packet.getTimestamp());
					IOUtils.writeMediumInt(data, 0);
					data.put(packet.getData().duplicate());
					data.putInt(bodySize + 11);
					data.flip(); 
					conn.write(data);
				}
			}
		} else if(message instanceof StatusMessage) {
			if(((StatusMessage) message).getBody().getCode().equals(StatusCodes.NS_PLAY_UNPUBLISHNOTIFY)) {
				closed = true;
				conn.close();
			}
		}
	}
	
	@Override
	public HTTPMinaConnection getConnection() {
		return conn;
	}

	public boolean isClosed() {
		return closed;
	}
	
	public void setClose(boolean value) {
		this.closed = value;
	}
}
