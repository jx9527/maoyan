package org.red5;


import org.red5.demo.PushRTMP;
import org.red5.server.util.RootPathUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.web.servlet.ServletComponentScan; 

@ServletComponentScan
@SpringBootApplication 
@EnableAutoConfiguration(exclude = {JmxAutoConfiguration.class})
public class SpringBootRed5Application {

    public static void main(String[] args) throws Exception {
    	//设置root目录
    	RootPathUtil.iniRoot();
    	//启动服务器
        SpringApplication.run(SpringBootRed5Application.class, args);
        //视频推流
        PushRTMP.run();
    } 
}
