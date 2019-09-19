package org.red5.war;

import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//@Configuration // 用来定义 DispatcherServlet 应用上下文中的 bean、
public class WebConfig{
	//@Bean
	public ServletListenerRegistrationBean<WarLoaderServlet> serssionListenerBean(){
		ServletListenerRegistrationBean<WarLoaderServlet> 
		sessionListener = new ServletListenerRegistrationBean<WarLoaderServlet>(new WarLoaderServlet());
		return sessionListener;
	}
	 
}
