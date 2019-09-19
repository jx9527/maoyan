package org.red5.server.stream.timeshift;

import java.io.File;
import java.io.IOException;

import org.red5.io.flv.impl.FLVReader;
import org.red5.server.util.FileUtil;
import org.red5.server.util.FileUtilS;

/**
 * FLV Reader From Disk
 * @author pengliren
 *
 */
public class FLVDiskReader extends FLVReader implements IRecordFLVReader {

	private RecordFLVIndexReader flvIndexReader;
	
	private String flvFilePath;
	
	public FLVDiskReader(File f) throws IOException {
			
		super(f);
		flvFilePath = f.getAbsolutePath();
	}
	
	//do not cache the keyframes
	@Override
	protected void postInitialize() {
		
		if (getRemainingBytes() >= 9) {
			decodeHeader();
		}
	}
	
	@Override
	public void seekByTs(long ts) {
		
		if(flvIndexReader == null){
			String flvIndexPath = FileUtilS.getFileName(flvFilePath) + ".idx"; 
			if(!(new File(flvIndexPath)).exists()){
				RecordFLVWriter.generateFlvIndexFile(flvFilePath);
			}
			flvIndexReader = new RecordFLVIndexReader(flvIndexPath);
			
		}
		setCurrentPosition(flvIndexReader.getPosition(ts).getPosition()-4);
		flvIndexReader.close();
		flvIndexReader = null;
	}
	
	@Override
	public void close() {
	
		super.close();
		
		if(flvIndexReader != null) {
			flvIndexReader.close();
		}
	}
}
