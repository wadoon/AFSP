package edu.kit.tm.afsp.g1;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AFSPHost {
    private ServerSocket tcpServerSocket;
    private DatagramSocket udpSocket;
    private String pseudonym;
    private InetAddress broadcastAddress;
    private int port;
    private Map<ForeignHost, ForeignHost> knownHosts = new HashMap<>();

    private LocalFileList localFileList = new LocalFileList();
    private Thread tcpServerThread;
    private Thread udpServerThread;

    private static Logger LOGGER = LogManager.getLogger("afsp");

    public AFSPHost(String pseudonym, SocketAddress serverAddress, int port,
	    SocketAddress udpAddress, InetAddress broadcastAddr)
	    throws IOException {

	this.pseudonym = pseudonym;

	this.port = port;
	this.broadcastAddress = broadcastAddr;

	tcpServerSocket = new ServerSocket(port);
	udpSocket = new DatagramSocket(port);

	// tcpServerSocket.bind(serverAddress);
	udpSocket.connect(udpAddress);

	tcpServerThread = new Thread(new TCPServerThread());
	udpServerThread = new Thread(new UDPServerThread());

	tcpServerThread.setName("AFSP-TCP");
	udpServerThread.setName("AFSP-UDP");

	tcpServerThread.setDaemon(true);
	udpServerThread.setDaemon(true);

	udpServerThread.start();
	tcpServerThread.start();

	// every 10 seconds
	Timer timer = new Timer();
	timer.schedule(new HeartBeatTimer(), 10 * 1000, 10 * 1000);
    }

    public void download(ForeignHost fh, byte[] digest) {
	// TODO
    }

    public void sendEmptyUDP(MessageType mt) throws IOException {
	Header h = new Header();
	h.setPseudonym(pseudonym);
	h.setLength(0);
	h.setMessageType(mt);

	byte buf[] = h.toByteArray();
	DatagramPacket outgoing = new DatagramPacket(buf, buf.length);
	outgoing.setPort(port);
	outgoing.setAddress(broadcastAddress);
	udpSocket.send(outgoing);
    }

    public void signin() throws IOException {
	sendEmptyUDP(MessageType.SIGNIN);
    }

    public void signout() throws IOException {
	sendEmptyUDP(MessageType.SIGNOUT);
    }

    public Socket openClientSocket(ForeignHost fh, Header header)
	    throws IOException {
	Socket s = new Socket(fh.getAddr(), port);
	OutputStream os = s.getOutputStream();
	os.write(header.toByteArray());
	return s;
    }

    public void signin_ack(ForeignHost fh) throws IOException {
	Header h = Header.create(MessageType.SIGNIN_ACK, pseudonym, 0);
	Socket s = openClientSocket(fh, h);
	s.close();
    }

    public void exchange_filelist_req(ForeignHost fh) throws IOException {
	Header h = Header.create(MessageType.EXCHANGE_FILELIST_REQ, pseudonym,
		0);
	Socket s = openClientSocket(fh, h);

	InputStream is = s.getInputStream();
	Header inHeader = new Header(is);

	byte[] content = new byte[(int) inHeader.getLength()];
	fill(is, content);

	ByteArrayInputStream bais = new ByteArrayInputStream(content);
	DataInputStream inputStream = new DataInputStream(bais);

	while (inputStream.available() >= 1) {
	    long length = Header.read6ByteInt(inputStream);
	    short fileNameLength = inputStream.readShort();
	    byte[] digest = new byte[512 / 8];// TODO SHA512
	    inputStream.read(digest);
	    String filename = Header.readUTF(inputStream, fileNameLength);

	    fh.addFile(filename, digest, length);
	}
    }

    private static void fill(InputStream is, byte[] content) throws IOException {
	for (int readed = 0; readed < content.length;) {
	    int length = is.read(content, readed, content.length - readed);
	    readed += length;
	}
    }

    public void sendHEARTBEAT() throws IOException {
	sendEmptyUDP(MessageType.HEARTBEAT);
    }

    @SuppressWarnings("incomplete-switch")
    private void handleTCPRequest(Socket client) throws IOException {
	InputStream is = client.getInputStream();
	Header h = new Header(is);
	ForeignHost fh = addToKnownHost(client.getInetAddress(), h);

	switch (h.getMessageType()) {
	case EXCHANGE_FILELIST:
	    exchange_filelist(client);
	    break;
	case DOWNLOAD_REQ:
	    download_rpl(client);
	    break;
	}
    }

    private void download_rpl(Socket client) throws IOException {
	InputStream is = client.getInputStream();
	OutputStream os = client.getOutputStream();

	byte[] digest = null;// TODO read out hash value
	File fil = localFileList.get(digest);

	if (fil == null) {
	    Header h = Header.create(MessageType.DOWNLOAD_ERR, pseudonym, 0);
	    os.write(h.toByteArray());
	    os.close();
	    client.close();
	} else {
	    BufferedInputStream bis = new BufferedInputStream(
		    new FileInputStream(fil));
	    Header h = Header.create(MessageType.DOWNLOAD_RPL, pseudonym,
		    fil.length());

	    os.write(h.toByteArray());
	    IOUtils.copy(bis, os);

	    Header response = new Header(is);
	    switch (response.getMessageType()) {
	    case DOWNLOAD_ACK:
		LOGGER.info("Download successful");
		break;
	    case DOWNLOAD_ERR:
		LOGGER.info("Download unsuccessful");
		break;
	    default:
		LOGGER.info("no valid download response");
		break;
	    }
	}
    }

    private void exchange_filelist(Socket client) throws IOException {
	byte[] b = localFileList.toByteArray();
	Header h = Header.create(MessageType.EXCHANGE_FILELIST, pseudonym,
		b.length);
	OutputStream os = client.getOutputStream();
	os.write(h.toByteArray());
	os.write(b);
	os.close();
	client.close();
    }

    @SuppressWarnings("incomplete-switch")
    private void handleUDPRequest(DatagramPacket packet) throws IOException {
	Header h = new Header(packet);

	switch (h.getMessageType()) {
	case SIGNIN:
	    ForeignHost fh = addToKnownHost(packet.getAddress(), h);
	    signin_ack(fh);
	    exchange_filelist_req(fh);
	    break;
	case SIGNOUT:
	    removeFromKnownHost(packet, h);
	    break;
	case HEARTBEAT:
	    ForeignHost host = addToKnownHost(packet.getAddress(), h);
	    host.setLastSeen(new Date());
	    break;
	}
    }

    private void removeFromKnownHost(DatagramPacket packet, Header h) {
	ForeignHost fh = new ForeignHost(packet.getAddress(), h.getPseudonym());
	knownHosts.remove(fh);
    }

    private ForeignHost addToKnownHost(InetAddress addr, Header h) {
	ForeignHost fh = ForeignHost.create(addr, h.getPseudonym());

	if (knownHosts.containsKey(fh))
	    return knownHosts.get(fh);

	knownHosts.put(fh, fh);
	return fh;
    }

    class HeartBeatTimer extends TimerTask {
	@Override
	public void run() {
	    try {
		sendHEARTBEAT();
	    } catch (IOException e) {
		e.printStackTrace();
	    }
	}
    }

    class TCPServerThread implements Runnable {
	@Override
	public void run() {
	    while (true) {
		try {
		    Socket clientSocket = tcpServerSocket.accept();
		    Thread t = new Thread(new ClientThread(clientSocket));
		    t.setName("client thread - "
			    + clientSocket.getRemoteSocketAddress());
		    t.start();
		} catch (IOException e) {
		    LOGGER.error("tcp-thread", e);
		}
	    }
	}
    }

    class UDPServerThread implements Runnable {
	@Override
	public void run() {

	    while (true) {
		while (true) {
		    byte[] buf = new byte[64 * 1024];
		    final DatagramPacket p = new DatagramPacket(buf, buf.length);
		    try {
			udpSocket.receive(p);
			new Thread(new Runnable() {
			    @Override
			    public void run() {
				try {
				    handleUDPRequest(p);
				} catch (IOException e) {
				    LOGGER.error(
					    "Error in udp-thread: "
						    + p.getAddress(), e);
				}
			    }
			}).start();
		    } catch (IOException e) {
			LOGGER.error("Error in udp-thread: " + p.getAddress(),
				e);
		    }
		}
	    }

	}
    }

    class ClientThread implements Runnable {
	private Socket client;

	public ClientThread(Socket clientSocket) {
	    client = clientSocket;
	}

	@Override
	public void run() {
	    try {
		handleTCPRequest(client);
	    } catch (IOException e) {
		LOGGER.error(
			"Error in client thread: "
				+ client.getRemoteSocketAddress(), e);
	    }
	}
    }

    public void serve() {
	try {
	    tcpServerThread.join();
	} catch (InterruptedException e) {
	    LOGGER.error(e);
	}
    }

    public void quit() {
	udpSocket.close();
	try {
	    tcpServerSocket.close();
	} catch (IOException e) {
	    LOGGER.error("Error on quitting", e);
	}
    }

    public LocalFileList getLocalFiles() {
	return localFileList;
    }

    public void onFilesListUpdated() {
	// TODO SEND EXCHG FILELIST TO ALL PEERS
    }
}
