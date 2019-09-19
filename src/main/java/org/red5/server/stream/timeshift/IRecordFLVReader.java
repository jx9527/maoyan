package org.red5.server.stream.timeshift;

import org.red5.io.ITagReader;

/**
 * FLV Record Reader
 * @author pengliren
 *
 */
public interface IRecordFLVReader extends ITagReader {

	public void seekByTs(long ts);
}
