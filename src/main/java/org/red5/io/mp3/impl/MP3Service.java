
package org.red5.io.mp3.impl;

import java.io.File;
import java.io.IOException;

import org.red5.io.BaseStreamableFileService;
import org.red5.io.IStreamableFile;
import org.red5.io.mp3.IMP3Service;

/**
 * Streamable file service extension for MP3
 */
public class MP3Service extends BaseStreamableFileService implements
		IMP3Service {

	/** {@inheritDoc} */
    @Override
	public String getPrefix() {
		return "mp3";
	}

	/** {@inheritDoc} */
    @Override
	public String getExtension() {
		return ".mp3";
	}

	/** {@inheritDoc} */
    @Override
	public IStreamableFile getStreamableFile(File file) throws IOException {
		return new MP3(file);
	}

}
