package org.red5.demo;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
@Controller
public class ShowApp {
	@RequestMapping(value = "/")
    public String index() {
        return "/index.html";
    } 
}
