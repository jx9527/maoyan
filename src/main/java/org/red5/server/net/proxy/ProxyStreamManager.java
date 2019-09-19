package org.red5.server.net.proxy;

import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

import org.red5.server.api.scheduling.IScheduledJob;
import org.red5.server.api.scheduling.ISchedulingService;
import org.red5.server.scheduling.QuartzSchedulingService;


/**
 * 代理流管理
 * 
 * @author pengliren
 * 
 */
public class ProxyStreamManager {

	protected String instanceId;

	private ConcurrentHashMap<String, IProxyStream> streams;
	private String currentName;
	private  IProxyStream currentStream;
	
	private static final class SingletonHolder {

		private static final ProxyStreamManager INSTANCE = new ProxyStreamManager();
	}

	public static ProxyStreamManager getInstance() {

		return SingletonHolder.INSTANCE;
	}
	
	private ProxyStreamManager() {
		
		streams = new ConcurrentHashMap<String, IProxyStream>();
		
		QuartzSchedulingService.getInstance().addScheduledJob(1000, new IScheduledJob() {
			@Override
					public void execute(ISchedulingService service) throws CloneNotSupportedException {
						Enumeration<String> enums = streams.keys();
						while (enums.hasMoreElements()) {
							currentName = enums.nextElement();
							currentStream = streams.get(currentName);
							if (currentStream.isClosed()) {
								streams.remove(currentName);
							}
						}
						enums = null;
					}
		});
	}
	
	public void register(String name,IProxyStream stream){
		streams.put(name, stream);
	}
	public IProxyStream unregister(String name){
		return streams.remove(name);
	}
    public boolean exists(String name){
    	return streams.containsKey(name);
    }
}
