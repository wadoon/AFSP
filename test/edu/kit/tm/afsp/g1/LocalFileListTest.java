package edu.kit.tm.afsp.g1;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

public class LocalFileListTest {

    LocalFileList lfl;

    // 772c61607a58f654f1ef88e8d3c03c5c *build.xml
    private static final byte[] DIGEST_BUILD_XML = convert("772c61607a58f654f1ef88e8d3c03c5c");
    // 7f97854dc04c119d461fed14f5d8bb96 *commons-io-2.4.jar
    private static final byte[] DIGEST_COMMONS_JAR = convert("7f97854dc04c119d461fed14f5d8bb96");

    @Before
    public void setUp() throws Exception {
	lfl = new LocalFileList();
	lfl.add(new File("commons-io-2.4.jar"));
	lfl.add(new File("build.xml"));
    }

    private static byte[] convert(String string) {
	byte b[] = new byte[string.length() / 2];
	for (int i = 0; i < string.length(); i += 2) {
	    b[i / 2] = ((byte) (int) (Integer.valueOf(
		    string.substring(i, i + 2), 16)));
	}
	return b;
    }

    @Test
    public void testAddFile() {
	// nothing todo
    }

    @Test
    public void testGet() {

    }

    @Test
    public void getDigest() {
	assertTrue(Arrays.equals(DIGEST_BUILD_XML,
		lfl.getDigest(new File("build.xml"))));
	assertTrue(Arrays.equals(DIGEST_COMMONS_JAR,
		lfl.getDigest(new File("commons-io-2.4.jar"))));
    }

    @Test
    public void testSize() {
	assertEquals(2, lfl.size());
    }

    @Test
    public void testToByteArray() {
	System.out.println(Arrays.toString(lfl.toByteArray()));

    }

    @Test
    public void testReadByteArray() throws IOException {
	byte[] input = lfl.toByteArray();
	DataInputStream dis = new DataInputStream(new ByteArrayInputStream(
		input));

	ForeignHost fh = ForeignHost.create(null, "dummy");
	AFSPHost.parseFileList(fh, dis);

	assertTrue(Arrays.equals(DIGEST_BUILD_XML, fh.getHash("build.xml")));
	assertTrue(Arrays.equals(DIGEST_COMMONS_JAR,
		fh.getHash("commons-io-2.4.jar")));

	assertEquals(2320, fh.getFileSize("build.xml"));
    }
}
