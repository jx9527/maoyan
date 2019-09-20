package org.red5;


import java.io.File;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;


@ServletComponentScan
@Controller
@SpringBootApplication 
@EnableAutoConfiguration(exclude = {JmxAutoConfiguration.class})
public class SpringBootRed5Application {

    public static void main(String[] args) throws Exception {
    	SpringApplication springApplication = new SpringApplication(SpringBootRed5Application.class);
        springApplication.run(args); 
        setRoot();
    }

    private static void setRoot(){
    	String root = System.getProperty("red5.root");
 		// if root is null check environmental
 		if (root == null) {
 			//check for env variable
 			root = System.getenv("RED5_HOME");
 		}
 		// if root is null find out current directory and use it as root
 		if (root == null || ".".equals(root)) {
 			root = System.getProperty("user.dir");
 		}
 		//if were on a windows based os flip the slashes
 		if (File.separatorChar != '/') {
 			root = root.replaceAll("\\\\", "/");
 		}
 		//drop last slash if exists
 		if (root.charAt(root.length() - 1) == '/') {
 			root = root.substring(0, root.length() - 1);
 		}
 		//set/reset property
 		System.setProperty("red5.root", root);
    }
    
    @RequestMapping(value = "/")
    public String index() {
        return "/index.html";
    }

}
