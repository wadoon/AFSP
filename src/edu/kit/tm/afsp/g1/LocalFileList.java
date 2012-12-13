package edu.kit.tm.afsp.g1;

import java.awt.List;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LocalFileList extends LinkedList<File> {
    /**
     * 
     */
    private static final long serialVersionUID = 3832934914708172286L;

    private Map<byte[], File> files = new HashMap<>();
    private Map<File, byte[]> hashes = new HashMap<>();
    public static final String HASH_ALGORITHM = "SHA-512";
    private static Logger logger = LogManager.getLogger("afsp");

    public LocalFileList() {
    }

    public boolean add(File f) {
	byte[] hash;
	try {
	    hash = calculateHash(f);
	    hashes.put(f, hash);
	    files.put(hash, f);
	    super.add(f);

	    logger.debug("file added to local list " + f + " :: "
		    + Arrays.toString(hash));

	    return true;
	} catch (NoSuchAlgorithmException e) {
	    e.printStackTrace();
	} catch (IOException e) {
	    e.printStackTrace();
	}
	return false;
    }

    public byte[] toByteArray() {
	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	DataOutputStream dos = new DataOutputStream(baos);
	try {
	    for (File f : hashes.keySet()) {
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

    private static byte[] calculateHash(File f)
	    throws NoSuchAlgorithmException, IOException {
	MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
	FileInputStream fis = new FileInputStream(f);
	byte[] buffer = new byte[64 * 1024];
	int sz;
	while ((sz = fis.read(buffer)) > 0) {
	    md.update(buffer, 0, sz);
	}
	fis.close();
	return md.digest();
    }

    public File get(byte[] digest) {
	return files.get(digest);
    }

    public int size() {
	return files.size();
    }

    public byte[] getDigest(File f) {
	return hashes.get(f);
    }
}
