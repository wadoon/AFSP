package edu.kit.tm.afsp.g1;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.codec.digest.DigestUtils;

public class FileData {

	public byte[] hash;
	public File file;
	
	public FileData(File file)
	{
		this.file = file;
		try {
			hash = DigestUtils.md5(new FileInputStream(file));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
