package org.red5;


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
    }

    @RequestMapping(value = "/")
    public String index() {
        return "/index.html";
    }

}
