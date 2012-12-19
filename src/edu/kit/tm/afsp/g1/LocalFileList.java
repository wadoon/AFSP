package edu.kit.tm.afsp.g1;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LocalFileList extends LinkedList<File> {
	/**
     * 
     */
	private static final long serialVersionUID = 3832934914708172286L;

	private Map<String, File> files = new HashMap<>();
	private Map<File, byte[]> hashes = new HashMap<>();
	public static final String HASH_ALGORITHM = "SHA-512";
	private static Logger logger = LogManager.getLogger("afsp");

	public LocalFileList() {
	}

	public boolean add(File f) {
		byte[] hash;
		try {
			hash = DigestUtils.md5(new FileInputStream(f));
			hashes.put(f, hash);
			files.put(LocalFileList.md52str(hash), f);
			super.add(f);

			logger.debug("file added to local list " + f + " :: "
					+ md52str(hash));

			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	public static String md52str(byte[] hash) {
		StringBuilder sb = new StringBuilder(hash.length * 2);

		for (byte b : hash) {
			int i = ((int) b) & 0xff;
			sb.append(Integer.toString(i, 16));
		}

		return sb.toString();
	}

	public byte[] toByteArray() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		try {
			for (File f : hashes.keySet()) {
				Header.write6ByteInt(dos, f.length());
				Header.write2ByteInt(dos, f.getName().length());
				dos.write(hashes.get(f));
				Header.writeUTF(dos, f.getName());
			}
			dos.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return baos.toByteArray();
	}

	public File get(byte[] digest) {
		return files.get(LocalFileList.md52str(digest));
	}

	public int size() {
		return files.size();
	}

	public byte[] getDigest(File f) {
		return hashes.get(f);
	}

	public long length() {
		return toByteArray().length;// TODO optimize
	}
}
