package org.red5.server;

import java.util.HashMap;
import java.util.Map;

public class ScopeContextBean {

	public final static String RTMPAPPLICATIONADAPTER_BEAN = "rtmpApplicationAdapter";
	public final static String HTTPAPPLICATIONADAPTER_BEAN = "httpApplicationAdapter";
	public final static String BROADCASTSTREAM_BEAN = "broadcastStream";
	public final static String SUBSCRIBERSTREAM_BEAN = "subscriberStream";
	public final static String SINGLESTREAM_BEAN = "singleStream";
	public final static String PROVIDERSERVICE_BEAN = "providerService";
	public final static String CONSUMERSERVICE_BEAN = "consumerService";
	public final static String STREAMSERVICE_BEAN = "streamService";
	public final static String FILECONSUMER_BEAN = "fileConsumer";
	public final static String RTMPSAMPLEACCESS_BEAN = "rtmpSampleAccess";
	public final static String SECURITYFORBIDDEN_BEAN = "forbidden";

	private Map<String, ContextBean> clazzMap = new HashMap<String, ContextBean>();
	
	public void addClazz(String name, ContextBean bean) {
		
		clazzMap.put(name, bean);
	}
	
	public ContextBean getClazz(String name) {
		
		return clazzMap.get(name);
	}

	public Map<String, ContextBean> getClazzMap() {
		return clazzMap;
	}

	public void setClazzMap(Map<String, ContextBean> clazzMap) {
		this.clazzMap = clazzMap;
	}
}
