package org.red5;


import org.java_websocket.client.WebSocketClient;
import org.red5.demo.PushRTMP;
import org.red5.server.util.RootPathUtil;
import org.red5.websocket.MyWebSocketClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.Bean;

import java.net.URI;
import java.net.URISyntaxException;

@ServletComponentScan
@SpringBootApplication 
@EnableAutoConfiguration(exclude = {JmxAutoConfiguration.class})
public class SpringBootRed5Application {

    public static void main(String[] args) throws Exception {
    	//设置root目录
    	RootPathUtil.iniRoot();
    	//启动服务器
        SpringApplication.run(SpringBootRed5Application.class, args);
//        视频推流
//                //PushRTMP.run();
    }
    //初始化websocket客户端
    @Bean
    public WebSocketClient webSocketClient(){
        try{
            System.out.println("执行了webClient初始化");
            MyWebSocketClient myWebSocketClient = new MyWebSocketClient(new URI("ws://127.0.0.1:8001/websocket/12/owner"));
            myWebSocketClient.connect();
            return myWebSocketClient;
        }catch (URISyntaxException e){
            e.printStackTrace();
        }
        return null;
    }
}
