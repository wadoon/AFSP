package edu.kit.tm.afsp.g1;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

public class HeaderTest {

    private Header h;
    // truncate |
    static final long l1 = 0x0000_0000_0000_0000L;
    static final long l2 = 0x0110_0000_0000_0000L;
    static final long l3 = 0x1000_0000_1000_0001L;
    static final long l4 = 0x0000_FFFF_FFFF_FFFFL;
    static final long l5 = 0x0000_FF00_FF00_FF00L;

    static byte _FF = (byte) 0xff;

    byte[] exp = { 0, 0, 0, 0, 0, 0, // l1
	    0, 0, 0, 0, 0, 0, // l2
	    0, 0, 0x10, 0, 0, 1, // l3
	    _FF, _FF, _FF, _FF, _FF, _FF, // l4
	    _FF, 0, _FF, 0, _FF, 0 }; // l5

    @Before
    public void setUp() throws Exception {
	h = Header.create(MessageType.SIGNIN, "TEST", 0xFF00_FF00_FF00L);
    }

    @Test
    public void testHeader() {
	@SuppressWarnings("unused")
	Header h = new Header();
    }

    @Test
    public void testHeaderDatagramPacket() {

	byte[] buf = h.toByteArray();
	DatagramPacket dp = new DatagramPacket(buf, buf.length);
	Header g = new Header(dp);

	assertEquals(h.getPseudonym(), g.getPseudonym());
	assertEquals(h.getLength(), g.getLength());
	assertEquals(h.getMessageType(), g.getMessageType());
	assertEquals(h, g);
    }

    @Test
    public void testHeaderInputStream() throws IOException {
	ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(
		h.toByteArray());
	Header g = new Header(arrayInputStream);

	assertEquals(h.getPseudonym(), g.getPseudonym());
	assertEquals(h.getLength(), g.getLength());
	assertEquals(h.getMessageType(), g.getMessageType());
	assertEquals(h, g);
    }

    @Test
    public void testGetLength() {
	assertEquals(0xFF00_FF00_FF00L, h.getLength());
    }

    @Test
    public void testGetPseudonym() {
	assertEquals("TEST", h.getPseudonym());
    }

    @Test
    public void testGetMessageType() {
	assertEquals(MessageType.SIGNIN, h.getMessageType());
    }

    @Test
    public void testWrite6ByteInt() throws IOException {
	final ByteArrayOutputStream out = new ByteArrayOutputStream();
	DataOutputStream dos = new DataOutputStream(out);

	Header.write6ByteInt(dos, l1);
	Header.write6ByteInt(dos, l2);
	Header.write6ByteInt(dos, l3);
	Header.write6ByteInt(dos, l4);
	Header.write6ByteInt(dos, l5);

	byte[] ary = out.toByteArray();
	System.out.println(Arrays.toString(ary));
	System.out.println(Arrays.toString(exp));
	assertTrue(Arrays.equals(exp, ary));
    }

    @Test
    public void testRead6ByteInt() throws IOException {
	DataInputStream dos = new DataInputStream(new ByteArrayInputStream(exp));
	long k1 = Header.read6ByteInt(dos);
	long k2 = Header.read6ByteInt(dos);
	long k3 = Header.read6ByteInt(dos);
	long k4 = Header.read6ByteInt(dos);
	long k5 = Header.read6ByteInt(dos);

	assertEquals(l1 << 16 >> 16, k1);
	assertEquals(l2 << 16 >> 16, k2);
	assertEquals(l3 << 16 >> 16, k3);
	assertEquals(l5, k5);
	assertEquals(l4, k4);
    }

    @Test
    public void testToByteArray() {
	byte[] ary = h.toByteArray();
	byte[] exp = { 1, 84, 69, 83, 84, 32, 32, 32, 32, 32, 32, 32, 32, 32,
		32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, -1, 0, -1, 0,
		-1, 0 };

	System.out.println(Arrays.toString(ary));
	System.out.println(Arrays.toString(exp));

	assertEquals(Header.HEADER_LENGTH, ary.length);
	assertTrue(Arrays.equals(exp, ary));
    }

    @Test
    public void testReadUTF() {
	// fail("Not yet implemented");
    }

}
