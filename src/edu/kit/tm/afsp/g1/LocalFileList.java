package edu.kit.tm.afsp.g1;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class LocalFileList {
	private Collection<File> files = new LinkedList<>();
	private Map<File, byte[]> hashes = new HashMap<>();
	public static final String HASH_ALGORITHM = "SHA-512";
	public MessageDigest md;

	public LocalFileList() {
	}

	public void add(File f) throws NoSuchAlgorithmException, IOException {
		files.add(f);
		hashes.put(f, calculateHash(f));
	}

	public byte[] toByteArray() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		try {
			for (File f : files) {
				Header.write6ByteInt(dos, f.length());
				dos.writeShort(f.getName().length());
				dos.write(hashes.get(f));
				dos.writeUTF(f.getName());
			}
			dos.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return baos.toByteArray();
	}

	private byte[] calculateHash(File f) throws NoSuchAlgorithmException,
			IOException {
		md = MessageDigest.getInstance(HASH_ALGORITHM);
		FileInputStream fis = new FileInputStream(f);
		byte[] buffer = new byte[64 * 1024];
		int sz;
		while ((sz = fis.read(buffer)) > 0) {
			md.update(buffer, 0, sz);
		}
		fis.close();
		return md.digest();
	}
}
