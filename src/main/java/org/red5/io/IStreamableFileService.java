package org.red5.io;

import java.io.File;
import java.io.IOException;

/**
 * Provides access to files that can be streamed. 
 */
public interface IStreamableFileService {

	/**
	 * Sets the prefix. 
	 */
	public void setPrefix(String prefix);
	
	/**
     * Getter for prefix. Prefix is used in filename composition to fetch real file name. 
     */
    public String getPrefix();

    /**
     * Sets the file extensions serviced. If there are more than one, they are separated
     * by commas. 
     */
    public void setExtension(String extension);
    
	/**
     * Getter for extension of file 
     */
    public String getExtension();

    /**
     * Prepair given string to conform filename requirements, for example, add
     * extension to the end if missing. 
     */
    public String prepareFilename(String name);

    /**
     * Check whether file can be used by file service, that is, it does exist and have valid extension
     */
    public boolean canHandle(File file);

    /**
     * Return streamable file reference. For FLV files returned streamable file already has
     * generated metadata injected. 
     */
    public IStreamableFile getStreamableFile(File file) throws IOException;

}
