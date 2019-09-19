package org.red5.io.m4a;

import org.red5.io.IStreamableFileService;
import org.red5.io.object.Deserializer;
import org.red5.io.object.Serializer;

/**
 * A M4AService sets up the service and hands out M4A objects to 
 * its callers
 */
public interface IM4AService extends IStreamableFileService {

	/**
	 * Sets the serializer
	 * 
	 * @param serializer        Serializer object
	 */
	public void setSerializer(Serializer serializer);

	/**
	 * Sets the deserializer
	 * 
	 * @param deserializer      Deserializer object
	 */
	public void setDeserializer(Deserializer deserializer);

}
