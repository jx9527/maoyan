package org.red5.io;

import java.io.File;
import java.util.Set;

import org.red5.server.api.scope.IScopeService;

/**
 * Scope service extension that provides method to get streamable file services set
 */
public interface IStreamableFileFactory extends IScopeService {

	public static String BEAN_NAME = "streamableFileFactory";

	public abstract IStreamableFileService getService(File fp);

	/**
     * Getter for services
     *
     * @return  Set of streamable file services
     */
    public abstract Set<IStreamableFileService> getServices();

}