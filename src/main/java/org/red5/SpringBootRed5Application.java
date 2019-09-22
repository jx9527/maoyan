package org.red5;


import java.io.File;

import org.red5.demo.PushRTMP;
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
    	setRoot();
    	//启动服务器
    	SpringApplication springApplication = new SpringApplication(SpringBootRed5Application.class);
        springApplication.run(args); 
        //视频推流
        PushRTMP.run();
    }

    private static void setRoot(){
    	String root = System.getProperty("red5.root");
    	root = root == null ? System.getenv("RED5_HOME") : root; 
 		if (root == null || ".".equals(root)) {
 			root = System.getProperty("user.dir");
 		}
 		if (File.separatorChar != '/') {
 			root = root.replaceAll("\\\\", "/");
 		}
 		if (root.charAt(root.length() - 1) == '/') {
 			root = root.substring(0, root.length() - 1);
 		}
 		System.setProperty("red5.root", root);
    } 
}
