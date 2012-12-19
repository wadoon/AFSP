package edu.kit.tm.afsp.g1;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.nio.charset.Charset;
import java.util.Arrays;
import org.apache.commons.io.IOUtils;

/**
 * Standard-Header
 * 
 * @author weigla
 * 
 * 
 *         +--------+----------+----------+---------+---------+ | Offset | Byte
 *         1 | 2 | 3 | 4 | +--------+----------+----------+---------+---------+
 *         | 0 | Opcode | Pseudonym |
 *         +--------+----------+------------------------------+ | 4+ | Pseudonym
 *         | +--------+-----------------------------------------+ | 32 | Länge
 *         Nutzdaten | +--------+-----------------------------------------+ |
 *         36+ | Nutzdaten |
 *         +--------+-----------------------------------------+
 */
class Header {
	/**
	 * in byte TYPE(1) + PSEUDONYM(29) + LENGTH(6)
	 */
	private static final int PSEUDONYM_LENGTH = 25;
	public static final int HEADER_LENGTH = 1 + PSEUDONYM_LENGTH + 6;
	static final String CHARSET = "utf8";
	private static final int LENGTH_LENGTH = 6;
	private long length;
	private String pseudonym;
	private MessageType messageType;

	public Header() {
	}

	public Header(DatagramPacket packet) {
		byte[] data = Arrays.copyOf(packet.getData(), HEADER_LENGTH);
		setHeaderData(data);
	}

	public Header(InputStream is) throws IOException {
		byte[] head = new byte[HEADER_LENGTH];
		is.read(head);
		setHeaderData(head);
	}

	private void setHeaderData(byte[] data) {
		byte bT[] = slice(data, 0, 1);
		byte bP[] = slice(data, 1, PSEUDONYM_LENGTH);
		byte bL[] = slice(data, data.length - LENGTH_LENGTH, LENGTH_LENGTH);

		setMessageType(byOpcode(bT[0]));
		try {
			setPseudonym(new String(bP, CHARSET));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		setLength(toInt(bL));
	}

	private byte[] slice(byte[] b, int start, int length) {
		byte n[] = new byte[length];
		System.arraycopy(b, start, n, 0, length);
		return n;
	}

	private MessageType byOpcode(byte b) {
		for (MessageType mt : MessageType.values())
			if (mt.opcode == b)
				return mt;
		return MessageType.UNKNOWN;
	}

	private long toInt(byte[] b) {
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(b));
		try {
			return read6ByteInt(dis);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
	}

	public long getLength() {
		return length;
	}

	static final long MASK = 0x0000_FFFF_FFFF_FFFFL;

	public void setLength(long length) {
		this.length = length & MASK;
	}

	public String getPseudonym() {
		return pseudonym;
	}

	public void setPseudonym(String pseudonym) {
		if (pseudonym != null)
			this.pseudonym = pseudonym.trim();
		else
			pseudonym = "";
	}

	public MessageType getMessageType() {
		return messageType;
	}

	public void setMessageType(MessageType messageType) {
		this.messageType = messageType;
	}

	public static void write6ByteInt(OutputStream dos, long l)
			throws IOException {

		// short upper = (short) (l >> 32);
		// int lower = (int) l & 0xFFFFFFFF;
		// dos.writeShort(upper);
		// dos.writeInt(lower);

		byte[] buf = new byte[6];

		for (int i = buf.length - 1; i >= 0; i--) {
			buf[i] = (byte) (l & 0xFF);
			l >>= 8;
		}
		dos.write(buf);
	}

	public static void write2ByteInt(OutputStream dos, int num)
			throws IOException {
		byte[] buf = new byte[2];

		for (int i = buf.length - 1; i >= 0; i--) {
			buf[i] = (byte) (num & 0xFF);
			num >>= 8;
		}
		dos.write(buf);
	}

	public static long read6ByteInt(InputStream dos) throws IOException {
		// Does not work!
		// short u = dos.readShort();
		// long upper = getUnsignedShort(u);
		//
		// upper <<= 32;
		//
		// int l = dos.readInt();
		// long lower = getUnsignedInt(l);
		//
		// long len = (upper | lower) & MASK;
		//
		// return len;

		long len = 0;
		byte[] buf = new byte[6];
		dos.read(buf);
		for (byte b : buf) {
			int l = b & 0xFF;

			len <<= 8;
			len = (len | l);
		}
		return len;
	}

	public static long getUnsignedInt(int x) {
		long l = x;
		if (x < 0) {
			l = -x;
		}
		return l & 0x00000000ffffffffL;
	}

	public static long getUnsignedShort(short x) {
		return x & 0x000000000000ffffL;
	}

	/**
	 * generate header in byte format
	 * 
	 * @return
	 */
	public byte[] toByteArray() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(HEADER_LENGTH);
		DataOutputStream dos = new DataOutputStream(baos);
		try {
			dos.writeByte(messageType.opcode);
			writeUTF(dos, padding(pseudonym, PSEUDONYM_LENGTH));
			write6ByteInt(dos, length);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return baos.toByteArray();
	}

	private String padding(String string, int i) {
		StringBuilder sb = new StringBuilder(i);
		sb.append(string);
		for (int j = string.length(); j < i; j++) {
			sb.append(' ');
		}
		return sb.toString();
	}

	private void writeUTF(DataOutputStream dos, String string)
			throws IOException {
		Charset cs = Charset.forName(CHARSET);
		byte[] b = string.getBytes(cs);
		dos.write(b);
	}

	public static Header create(MessageType type, String pseudonym, long l) {
		Header h = new Header();
		h.setMessageType(type);
		h.setPseudonym(pseudonym);
		h.setLength(l);
		return h;
	}

	public static String readUTF(DataInputStream inputStream, short length)
			throws IOException {
		byte buf[] = new byte[length];
		IOUtils.readFully(inputStream, buf);
		return new String(buf, CHARSET);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (length ^ (length >>> 32));
		result = prime * result
				+ ((messageType == null) ? 0 : messageType.hashCode());
		result = prime * result
				+ ((pseudonym == null) ? 0 : pseudonym.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Header other = (Header) obj;
		if (length != other.length)
			return false;
		if (messageType != other.messageType)
			return false;
		if (pseudonym == null) {
			if (other.pseudonym != null)
				return false;
		} else if (!pseudonym.equals(other.pseudonym))
			return false;
		return true;
	}

	public static void writeUTF(OutputStream os, String name)
			throws IOException {
		try {
			byte b[] = name.getBytes(CHARSET);
			os.write(b);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String toString() {
		return "Header [length=" + length + ", pseudonym=" + pseudonym
				+ ", messageType=" + messageType + "]";
	}

}