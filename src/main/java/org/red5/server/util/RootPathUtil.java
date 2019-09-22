package org.red5.server.util;

import java.io.File;

public class RootPathUtil {
	
	public static void iniRoot(){
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
