package org.red5.server.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic file utility containing useful file or directory
 * manipulation functions.
 */
public class FileUtilS {

	private static Logger log = LoggerFactory.getLogger(FileUtilS.class);

	public static void copyFile(File source, File dest) throws IOException {
		log.debug("Copy from {} to {}", source.getAbsoluteFile(), dest
				.getAbsoluteFile());
		FileInputStream fi = new FileInputStream(source);
		FileChannel fic = fi.getChannel();
		MappedByteBuffer mbuf = fic.map(FileChannel.MapMode.READ_ONLY, 0,
				source.length());
		fic.close();
		fi.close();
		fi = null;

		// ensure the destination directory exists
		if (!dest.exists()) {
			String destPath = dest.getPath();
			log.debug("Destination path: {}", destPath);
			String destDir = destPath.substring(0, destPath
					.lastIndexOf(File.separatorChar));
			log.debug("Destination dir: {}", destDir);
			File dir = new File(destDir);
			if (!dir.exists()) {
				if (dir.mkdirs()) {
					log.debug("Directory created");
				} else {
					log.warn("Directory not created");
				}
			}
			dir = null;
		}

		FileOutputStream fo = new FileOutputStream(dest);
		FileChannel foc = fo.getChannel();
		foc.write(mbuf);
		foc.close();
		fo.close();
		fo = null;

		mbuf.clear();
		mbuf = null;
	}

	public static void copyFile(String source, String dest) throws IOException {
		copyFile(new File(source), new File(dest));
	}

	public static void moveFile(String source, String dest) throws IOException {
		copyFile(source, dest);
		File src = new File(source);
		if (src.exists() && src.canRead()) {
			if (src.delete()) {
				log.debug("Source file was deleted");
			} else {
				log
						.debug("Source file was not deleted, the file will be deleted on exit");
				src.deleteOnExit();
			}
		} else {
			log.warn("Source file could not be accessed for removal");
		}
		src = null;
	}

	/**
	 * Deletes a directory and its contents. This will fail if there are any
	 * file locks or if the directory cannot be emptied.
	 * 
	 * @param directory directory to delete
	 * @throws IOException if directory cannot be deleted
	 * @return true if directory was successfully deleted; false if directory
	 *  did not exist
	 */
	public static boolean deleteDirectory(String directory) throws IOException {
		return deleteDirectory(directory, false);
	}

	/**
	 * Deletes a directory and its contents. This will fail if there are any
	 * file locks or if the directory cannot be emptied.
	 * 
	 * @param directory directory to delete
	 * @param useOSNativeDelete flag to signify use of operating system delete function
	 * @throws IOException if directory cannot be deleted
	 * @return true if directory was successfully deleted; false if directory
	 *  did not exist
	 */
	public static boolean deleteDirectory(String directory,
			boolean useOSNativeDelete) throws IOException {
		boolean result = false;
		if (!useOSNativeDelete) {
			File dir = new File(directory);
			// first all files have to be cleared out
			for (File file : dir.listFiles()) {
				if (file.delete()) {
					log.debug("{} was deleted", file.getName());
				} else {
					log.debug("{} was not deleted", file.getName());
					file.deleteOnExit();
				}
				file = null;
			}
			// not you may remove the dir
			if (dir.delete()) {
				log.debug("Directory was deleted");
				result = true;
			} else {
				log.debug("Directory was not deleted, it may be deleted on exit");
				dir.deleteOnExit();
			}
			dir = null;			
		} else {
			Process p = null;
			Thread std = null;
			try {
				Runtime runTime = Runtime.getRuntime();
				log.debug("Execute runtime");
				//determine file system type
				if (File.separatorChar == '\\') {
					//we are windows
					p = runTime.exec("CMD /D /C \"RMDIR /Q /S "
						+ directory.replace('/', '\\') + "\"");
				} else {
					//we are unix variant 
					p = runTime.exec("rm -rf " + directory.replace('\\', File.separatorChar));
				}
				// observe std out
				std = stdOut(p);
				// wait for the observer threads to finish
				while (std.isAlive()) {
					try {
						Thread.sleep(250);
					} catch (Exception e) {
					}
				}
				log.debug("Process threads wait exited");
				result = true;
			} catch (Exception e) {
				log.error("Error running delete script", e);
			} finally {
				if (null != p) {
					log.debug("Destroying process");
					p.destroy();
					p = null;
				}
				std = null;
			}
		}
		return result;
	}

	/**
	 * Rename a file natively; using REN on Windows and mv on *nix.
	 * 
	 * @param from old name
	 * @param to new name
	 */
	public static void rename(String from, String to) {
		Process p = null;
		Thread std = null;
		try {
			Runtime runTime = Runtime.getRuntime();
			log.debug("Execute runtime");
			//determine file system type
			if (File.separatorChar == '\\') {
				//we are windows
				p = runTime.exec("CMD /D /C \"REN " + from + ' ' + to + "\"");
			} else {
				//we are unix variant 
				p = runTime.exec("mv -f " + from + ' ' + to);
			}
			// observe std out
			std = stdOut(p);
			// wait for the observer threads to finish
			while (std.isAlive()) {
				try {
					Thread.sleep(250);
				} catch (Exception e) {
				}
			}
			log.debug("Process threads wait exited");
		} catch (Exception e) {
			log.error("Error running delete script", e);
		} finally {
			if (null != p) {
				log.debug("Destroying process");
				p.destroy();
				p = null;
				std = null;
			}
		}		
	}	
	
	/**
	 * Special method for capture of StdOut.
	 * 
	 * @return
	 */
	private final static Thread stdOut(final Process p) {
		final byte[] empty = new byte[128];
		for (int b = 0; b < empty.length; b++) {
			empty[b] = (byte) 0;
		}
		Thread std = new Thread() {
			public void run() {
				StringBuilder sb = new StringBuilder(1024);
				byte[] buf = new byte[128];
				BufferedInputStream bis = new BufferedInputStream(p
						.getInputStream());
				log.debug("Process output:");
				try {
					while (bis.read(buf) != -1) {
						sb.append(new String(buf).trim());
						// clear buffer
						System.arraycopy(empty, 0, buf, 0, buf.length);
					}
					log.debug(sb.toString());
					bis.close();
				} catch (Exception e) {
					log.error("{}", e);
				}
			}
		};
		std.setDaemon(true);
		std.start();
		return std;
	}

	/**
	 * Create a directory.
	 * 
	 * @param directory directory to make
	 * @return whether a new directory was made
	 * @throws IOException if directory does not already exist or cannot be made
	 */
	public static boolean makeDirectory(String directory) throws IOException {
		return makeDirectory(directory, false);
	}

	/**
	 * Create a directory. The parent directories will be created if
	 * <i>createParents</i> is passed as true.
	 * 
	 * @param directory directory
	 * @param createParents whether to create all parents
	 * @return true if directory was created; false if it already existed
	 * @throws IOException if we cannot create directory
	 * 
	 */
	public static boolean makeDirectory(String directory, boolean createParents)
			throws IOException {
		boolean created = false;
		File dir = new File(directory);
		if (createParents) {
			created = dir.mkdirs();
			if (created) {
				log.debug("Directory created: {}", dir.getAbsolutePath());
			} else {
				log.debug("Directory was not created: {}", dir
						.getAbsolutePath());
			}
		} else {
			created = dir.mkdir();
			if (created) {
				log.debug("Directory created: {}", dir.getAbsolutePath());
			} else {
				log.debug("Directory was not created: {}", dir
						.getAbsolutePath());
			}
		}
		dir = null;
		return created;
	}
	
	/**
	 * Unzips a war file to an application located under the webapps directory
	 * 
	 * @param compressedFileName The String name of the war file
	 * @param destinationDir The destination directory, ie: webapps
	 */
	public static void unzip(String compressedFileName, String destinationDir) {
		
		//strip everything except the applications name
		String dirName = null;
		
		// checks to see if there is a dash "-" in the filename of the war. 
		String applicationName = compressedFileName.substring(compressedFileName.lastIndexOf("/"));
		
		int dashIndex = applicationName.indexOf('-');
		if (dashIndex != -1) {
			//strip everything except the applications name
			dirName = compressedFileName.substring(0, dashIndex);
		} else {
			//grab every char up to the last '.'
			dirName = compressedFileName.substring(0, compressedFileName.lastIndexOf('.'));
		}			
		
		log.debug("Directory: {}", dirName);
		//String tmpDir = System.getProperty("java.io.tmpdir");
		File zipDir = new File(compressedFileName);
		File parent = zipDir.getParentFile();
		log.debug("Parent: {}", (parent != null ? parent.getName() : null));
		//File tmpDir = new File(System.getProperty("java.io.tmpdir"), dirName);
		File tmpDir = new File(destinationDir);

		// make the war directory
		log.debug("Making directory: {}", tmpDir.mkdirs());
		
		try {
			@SuppressWarnings("resource")
			ZipFile zf = new ZipFile(compressedFileName);
			Enumeration<?> e = zf.entries();
			while(e.hasMoreElements()) {
				ZipEntry ze = (ZipEntry) e.nextElement();
				log.debug("Unzipping {}", ze.getName());
				if(ze.isDirectory()) {
					log.debug("is a directory");
					File dir = new File(tmpDir + "/" + ze.getName());
					Boolean tmp = dir.mkdir();
					log.debug("{}", tmp);
					continue;
				}
				
				// checks to see if a zipEntry contains a path
				// i.e. ze.getName() == "META-INF/MANIFEST.MF"
				// if this case is true, then we create the path first
				if(ze.getName().lastIndexOf("/") != -1) {
					String zipName = ze.getName();
					String zipDirStructure = zipName.substring(0, zipName.lastIndexOf("/"));
					File completeDirectory = new File(tmpDir + "/" + zipDirStructure);
					if(!completeDirectory.exists()) {
						if(!completeDirectory.mkdirs()) {
							log.error("could not create complete directory structure");
						}
					}
				}
				
				// creates the file
				FileOutputStream fout = new FileOutputStream(tmpDir + "/" + ze.getName());
				InputStream in = zf.getInputStream(ze);
				copy(in, fout);
				in.close();
				fout.close();
			} 
			e = null;
		} catch (IOException e) {
			log.error("Errord unzipping", e);
			//log.warn("Exception {}", e);
		}
	}
	
	public static void copy(InputStream in, OutputStream out) throws IOException {
		synchronized(in) {
			synchronized(out) {
				byte[] buffer = new byte[256];
				while (true) {
					int bytesRead = in.read(buffer);
					if(bytesRead == -1) break;
					out.write(buffer, 0, bytesRead);
				}
			}
		}
	}

	/**
	 * Unzips a given archive to a specified destination directory.
	 * 
	 * @param compressedFileName
	 * @param destinationDir
	 */
//	public static void unzip(String compressedFileName, String destinationDir) {
//		log.debug("Unzip - file: {} destination: {}", compressedFileName, destinationDir);
//		try {
//			final int BUFFER = 2048;
//			BufferedOutputStream dest = null;
//			FileInputStream fis = new FileInputStream(compressedFileName);
//			CheckedInputStream checksum = new CheckedInputStream(fis,
//					new Adler32());
//			ZipInputStream zis = new ZipInputStream(new BufferedInputStream(
//					checksum));
//			ZipEntry entry;
//			while ((entry = zis.getNextEntry()) != null) {
//				log.debug("Extracting: {}", entry);
//				String name = entry.getName();
//				int count;
//				byte data[] = new byte[BUFFER];
//				// write the files to the disk
//				File destFile = new File(destinationDir, name);
//				log.debug("Absolute path: {}", destFile.getAbsolutePath());
//				//create dirs as needed, look for file extension to determine type
//				if (entry.isDirectory()) {
//					log.debug("Entry is detected as a directory");
//					if (destFile.mkdirs()) {
//						log.debug("Directory created: {}", destFile.getName());
//					} else {
//						log.warn("Directory was not created: {}", destFile.getName());
//					}
//					destFile = null;
//					continue;
//				}				
//
//				FileOutputStream fos = new FileOutputStream(destFile);
//				dest = new BufferedOutputStream(fos, BUFFER);
//				while ((count = zis.read(data, 0, BUFFER)) != -1) {
//					dest.write(data, 0, count);
//				}
//				dest.flush();
//				dest.close();
//				destFile = null;
//			}
//			zis.close();
//			log.debug("Checksum: {}", checksum.getChecksum().getValue());
//		} catch (Exception e) {
//			log.error("Error unzipping {}", compressedFileName, e);
//			log.warn("Exception {}", e);
//		}
//
//	}
	
    /**
     * Quick-n-dirty directory formatting to support launching in windows, specifically from ant.
     * @param absWebappsPath abs webapps path
     * @param contextDirName conext directory name
     * @return full path
     */
    public static String formatPath(String absWebappsPath, String contextDirName) {
        StringBuilder path = new StringBuilder(absWebappsPath.length() + contextDirName.length());
        path.append(absWebappsPath);
        if (log.isTraceEnabled()) {
        	log.trace("Path start: {}", path.toString());
        }
        int idx = -1;
        if (File.separatorChar != '/') {
            while ((idx = path.indexOf(File.separator)) != -1) {
                path.deleteCharAt(idx);
                path.insert(idx, '/');
            }
        }
        if (log.isTraceEnabled()) {
        	log.trace("Path step 1: {}", path.toString());
        }
        //remove any './'
        if ((idx = path.indexOf("./")) != -1) {
        	path.delete(idx, idx + 2);
        }        
        if (log.isTraceEnabled()) {
        	log.trace("Path step 2: {}", path.toString());
        }
        //add / to base path if one doesnt exist
        if (path.charAt(path.length() - 1) != '/') {
        	path.append('/');
        }
        if (log.isTraceEnabled()) {
        	log.trace("Path step 3: {}", path.toString());
        }        
        //remove the / from the beginning of the context dir
        if (contextDirName.charAt(0) == '/' && path.charAt(path.length() - 1) == '/') {
            path.append(contextDirName.substring(1));
        } else {
            path.append(contextDirName);
        }
        if (log.isTraceEnabled()) {
        	log.trace("Path step 4: {}", path.toString());
        }
        return path.toString();
    }	
	
	/**
	 * Generates a custom name containing numbers and an underscore ex. 282818_00023.
	 * The name contains current seconds and a random number component.
	 * 
	 * @return custom name
	 */
	public static String generateCustomName() {
		Random random = new Random();
    	StringBuilder sb = new StringBuilder();
    	sb.append(PropertyConverter.getCurrentTimeSeconds());
    	sb.append('_');
    	int i = random.nextInt(99999);
    	if (i < 10) {
    		sb.append("0000");
    	} else if (i < 100) {
       		sb.append("000");
    	} else if (i < 1000) {
       		sb.append("00");
    	} else if (i < 10000) {
       		sb.append('0');
    	}    	
    	sb.append(i);
    	return sb.toString();
    }	
	
	public static List<String> getDirectoryList(String path) {
		
		File fileDir = new File(path);
		List<String> dirList = new ArrayList<String>();
		if (fileDir.isDirectory()) {
			File[] fileList = fileDir.listFiles();
			for (int i = 0; i < fileList.length; i++) {
				if (fileList[i].isDirectory() && !fileList[i].getName().contains(".")) {
					dirList.add(fileList[i].getName());
				}
			}
		}
		
		return dirList;
	}
	
	public static List<File> getFileList(String path) {
		
		File fileDir = new File(path);
		List<File> retList = new ArrayList<File>();
		if (fileDir.isDirectory()) {
			File[] fileList = fileDir.listFiles();
			for (int i = 0; i < fileList.length; i++) {
				if (!fileList[i].isDirectory()) {
					retList.add(fileList[i]);
				}
			}
		} else if(fileDir.isFile()) {
			retList.add(new File(path));
		}
		
		return retList;
	}
	
	public static String getFileName(String file) {
		
		if(StringUtils.isEmpty(file)) return null;
		int pos = file.lastIndexOf(".");
		return file.substring(0, pos);
	}
	
	public static String getFileName(File file) {
		
		return getFileName(file.getAbsolutePath());
	}
}
