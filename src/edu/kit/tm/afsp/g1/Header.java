package edu.kit.tm.afsp.g1;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.nio.charset.Charset;
import java.util.Arrays;

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
	public static final int HEADER_LENGTH = 1 + 29 + 6;
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
		setMessageType(byOpcode(data[0]));
		setPseudonym(toString());
		setLength(toInt(slice(data, 27, 4)));

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

	private int toInt(byte[] b) {
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(b));
		try {
			return dis.readInt();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
	}

	public long getLength() {
		return length;
	}

	public void setLength(long length) {
		this.length = length;
	}

	public String getPseudonym() {
		return pseudonym;
	}

	public void setPseudonym(String pseudonym) {
		if (pseudonym != null)
			this.pseudonym = pseudonym.trim();
	}

	public MessageType getMessageType() {
		return messageType;
	}

	public void setMessageType(MessageType messageType) {
		this.messageType = messageType;
	}

	public static void write6ByteInt(DataOutputStream dos, long l)
			throws IOException {
		short upper = (short) (l >> 32);
		int lower = (int) ((l << 32) >> 32);
		dos.writeShort(upper);
		dos.writeInt(lower);
	}

	public static long read6ByteInt(DataInputStream dos) throws IOException {
		long upper = dos.readShort();
		long lower = dos.readInt();
		long l = (upper << 32) | lower;
		return l;
	}

	/**
	 * generate header in byte format
	 * 
	 * @return
	 */
	public byte[] toByteArray() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(30);
		DataOutputStream dos = new DataOutputStream(baos);
		try {
			dos.writeByte(messageType.opcode);
			writeUTF(dos, padding(pseudonym, 29));
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
		Charset cs = Charset.forName("utf8");
		byte[] b = string.getBytes(cs);
		dos.write(b);
	}

	public static Header create(MessageType type, String pseudonym, int length) {
		Header h = new Header();
		h.setMessageType(type);
		h.setPseudonym(pseudonym);
		h.setLength(length);
		return h;
	}

	public static String readUTF(DataInputStream inputStream,
			short fileNameLength) throws IOException {
		byte buf[] = new byte[fileNameLength];
		inputStream.read(buf);
		return new String(buf);
	}
}