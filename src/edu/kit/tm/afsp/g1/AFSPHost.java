package edu.kit.tm.afsp.g1;

import java.awt.BorderLayout;
import java.awt.Component;
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
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.kit.tm.afsp.g1.ui.LocalFileListTableModel;
import edu.kit.tm.afsp.g1.ui.MainFrame;

public class AFSPHost {
    private static final int DIGEST_LENGTH_BYTE = 16;
    private ServerSocket tcpServerSocket;
    private DatagramSocket udpSocket;
    private String pseudonym;
    private InetAddress broadcastAddress;
    private int port;
    private List<ForeignHost> knownHosts = new LinkedList<>();

    private LocalFileList localFileList = new LocalFileList();
    private Thread tcpServerThread;
    private Thread udpServerThread;
    private Timer timer = new Timer("HEARTBEAT", true);

    private static Logger LOGGER = LogManager.getLogger("afsp");

    private ExecutorService exeService = Executors.newCachedThreadPool();
    private LinkedList<AFSPHostListener> listeners = new LinkedList<>();

    public AFSPHost(String pseudonym, SocketAddress serverAddress, int port,
	    SocketAddress udpAddress, InetAddress broadcastAddr)
	    throws IOException {

	this.pseudonym = pseudonym;
	this.port = port;
	this.broadcastAddress = broadcastAddr;

	tcpServerSocket = new ServerSocket(port);
	udpSocket = new DatagramSocket(port, InetAddress.getByName("0.0.0.0"));

	// tcpServerSocket.bind(serverAddress);
	udpSocket.setBroadcast(true);

	tcpServerThread = new TCPServerThread();
	udpServerThread = new UDPServerThread();
	udpServerThread.start();
	tcpServerThread.start();

	// every 10 seconds
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

	LOGGER.info("info send empty udp-datagram with: " + h + " to: "
		+ broadcastAddress + ":" + port);

	byte buf[] = h.toByteArray();
	DatagramPacket outgoing = new DatagramPacket(buf, buf.length);
	outgoing.setPort(port);
	outgoing.setAddress(broadcastAddress);
	udpSocket.send(outgoing);
	LOGGER.info("sent");
    }

    public void signin() throws IOException {
	LOGGER.info("send signin");
	sendEmptyUDP(MessageType.SIGNIN);
    }

    public void signout() throws IOException {
	LOGGER.info("send signout");
	sendEmptyUDP(MessageType.SIGNOUT);
    }

    public Socket openClientSocket(ForeignHost fh, Header header)
	    throws IOException {
	LOGGER.info("open tcp socket to: " + fh.getAddr() + ":" + port);
	LOGGER.info("send header: " + header);

	Socket s = new Socket(fh.getAddr(), port);
	OutputStream os = s.getOutputStream();
	os.write(header.toByteArray());
	return s;
    }

    public void signin_ack(ForeignHost fh) throws IOException {
	LOGGER.debug("signin_ack to: " + fh);
	Header h = Header.create(MessageType.SIGNIN_ACK, pseudonym, 0);
	Socket s = openClientSocket(fh, h);
	IOUtils.closeQuietly(s);
    }

    public void exchange_filelist_req(ForeignHost fh) throws IOException {
	LOGGER.info("exchange filelist request to: " + fh);
	Header h = Header.create(MessageType.EXCHANGE_FILELIST_REQ, pseudonym,
		0);
	Socket s = openClientSocket(fh, h);

	InputStream is = s.getInputStream();
	Header inHeader = new Header(is);

	byte[] content = new byte[(int) inHeader.getLength()];
	IOUtils.readFully(is, content);

	ByteArrayInputStream bais = new ByteArrayInputStream(content);
	DataInputStream inputStream = new DataInputStream(bais);
	parseFileList(fh, inputStream);
	IOUtils.closeQuietly(inputStream);
    }

    public static void parseFileList(ForeignHost fh, DataInputStream inputStream)
	    throws IOException {
	while (inputStream.available() >= 1) {
	    byte[] digest = new byte[DIGEST_LENGTH_BYTE];// TODO SHA512

	    long length = Header.read6ByteInt(inputStream);
	    short fileNameLength = inputStream.readShort();
	    IOUtils.readFully(inputStream, digest);
	    String filename = Header.readUTF(inputStream, fileNameLength);

	    LOGGER.debug("parsed " + length + "," + fileNameLength + ","
		    + filename + "," + LocalFileList.md52str(digest));

	    fh.addFile(filename, digest, length);
	}
    }

    public void sendHeartbeat() throws IOException {
	LOGGER.info("send heartbeat");
	sendEmptyUDP(MessageType.HEARTBEAT);
    }

    @SuppressWarnings("incomplete-switch")
    private void handleTCPRequest(Socket client) throws IOException {
	LOGGER.info("handleTCPRequest of " + client.getRemoteSocketAddress());

	InputStream is = client.getInputStream();
	Header h = new Header(is);
	ForeignHost fh = getKnownHost(client.getInetAddress(), h);

	LOGGER.info("Header: " + h);
	LOGGER.info("ForeignHost Info: " + fh);

	switch (h.getMessageType()) {
	case EXCHANGE_FILELIST:
	    exchange_filelist(client);
	    break;
	case DOWNLOAD_REQ:
	    download_rpl(client);
	    break;
	}
	LOGGER.info("end request handling, closing socket");
	client.close();
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
	LOGGER.debug("handle EXCHANGE_FILELIST");

	byte[] b = localFileList.toByteArray();
	Header h = Header.create(MessageType.EXCHANGE_FILELIST, pseudonym,
		b.length);

	LOGGER.debug("send header: " + h);
	LOGGER.debug("send DATA: " + Arrays.toString(b));

	OutputStream os = client.getOutputStream();
	os.write(h.toByteArray());
	os.write(b);
	os.flush();
	IOUtils.closeQuietly(os);
    }

    @SuppressWarnings("incomplete-switch")
    private void handleUDPRequest(DatagramPacket packet) throws IOException {
	LOGGER.debug("handle udp request");

	Header h = new Header(packet);

	LOGGER.info("got header: " + h);

	switch (h.getMessageType()) {
	case SIGNIN:
	    ForeignHost fh = getKnownHost(packet.getAddress(), h);
	    signin_ack(fh);
	    exchange_filelist_req(fh);
	    break;
	case SIGNOUT:
	    removeFromKnownHost(packet, h);
	    break;
	case HEARTBEAT:
	    ForeignHost host = getKnownHost(packet.getAddress(), h);
	    host.setLastSeen(new Date());
	    break;
	}

	firePeerUpdate();
    }

    private void removeFromKnownHost(DatagramPacket packet, Header h) {
	ForeignHost fh = new ForeignHost(packet.getAddress(), h.getPseudonym());
	knownHosts.remove(fh);
    }

    private ForeignHost getKnownHost(InetAddress addr, Header h) {
	ForeignHost fh = ForeignHost.create(addr, h.getPseudonym());

	if (knownHosts.contains(fh))
	    return knownHosts.get(knownHosts.indexOf(fh));

	knownHosts.add(fh);
	return fh;
    }

    class HeartBeatTimer extends TimerTask {
	@Override
	public void run() {
	    try {
		sendHeartbeat();
	    } catch (IOException e) {
		e.printStackTrace();
	    }
	}
    }

    class TCPServerThread extends Thread {
	public TCPServerThread() {
	    setName("TCP");
	}

	@Override
	public void run() {
	    while (true) {
		try {
		    Socket clientSocket = tcpServerSocket.accept();
		    Runnable clientThread = new TCPClientThread(clientSocket);
		    exeService.execute(clientThread);
		} catch (IOException e) {
		    LOGGER.error("tcp-thread", e);
		}
	    }
	}
    }

    class UDPServerThread extends Thread {

	public UDPServerThread() {
	    setName("UDP");
	}

	@Override
	public void run() {
	    while (true) {
		try {
		    byte[] buf = new byte[64 * 1024];
		    DatagramPacket p = new DatagramPacket(buf, buf.length);
		    udpSocket.receive(p);

		    UDPClientThread clientThread = new UDPClientThread(p);
		    exeService.execute(clientThread);
		} catch (IOException e) {
		    LOGGER.error("Error in udp-thread");
		}
	    }

	}
    }

    class UDPClientThread implements Runnable {
	private DatagramPacket p;

	public UDPClientThread(DatagramPacket p) {
	    this.p = p;
	}

	@Override
	public void run() {
	    try {
		handleUDPRequest(p);
	    } catch (IOException e) {
		LOGGER.error("Error in udp-thread: " + p.getAddress(), e);
	    }
	}
    }

    class TCPClientThread implements Runnable {
	private Socket client;

	public TCPClientThread(Socket clientSocket) {
	    client = clientSocket;
	}

	@Override
	public void run() {
	    try {
		LOGGER.info("started clientThread for "
			+ client.getRemoteSocketAddress());

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
	    LOGGER.debug("wainting for tcpServerThread to close");
	    tcpServerThread.join();
	} catch (InterruptedException e) {
	    LOGGER.error(e);
	}
    }

    @SuppressWarnings("deprecation")
    public void quit() {
	LOGGER.debug("quit requested by method call");

	LOGGER.debug("tear down udpSocket");
	udpSocket.close();

	try {
	    LOGGER.debug("tear down tcpServerSocket");
	    tcpServerSocket.close();
	} catch (IOException e) {
	    LOGGER.error("Error on quitting", e);
	    tcpServerThread.stop();
	}
    }

    public LocalFileList getLocalFiles() {
	return localFileList;
    }

    public void onFilesListUpdated() {
	LOGGER.debug("filelist change -> send info to peers");
	// TODO SEND EXCHG FILELIST TO ALL PEERS
    }

    public void addListener(AFSPHostListener lis) {
	listeners.add(lis);
    }

    public void firePeerUpdate() {
	LOGGER.debug("firePeerUpdate");
	for (AFSPHostListener lis : listeners) {
	    lis.onPeerUpdate();
	}
    }

    public List<ForeignHost> getForeignHosts() {
	return knownHosts;
    }
}
