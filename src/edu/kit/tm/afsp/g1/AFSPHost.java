package edu.kit.tm.afsp.g1;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
	private Timer timer;

	private static Logger logger = LogManager.getLogger("afsp");

	private ExecutorService exeService = Executors.newCachedThreadPool();
	private LinkedList<AFSPHostListener> listeners = new LinkedList<>();

	private HeartBeatTimer heartBeatTimer = new HeartBeatTimer();

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
	}

	public void download(ForeignHost fh, byte[] digest, File target)
			throws IOException {
		logger.trace("AFSP.download " + fh + "," + target);

		// TODO start in a worker thread
		Header header = Header.create(MessageType.DOWNLOAD_REQ, pseudonym,
				DIGEST_LENGTH_BYTE);
		Socket client = openClientSocket(fh, header);
		OutputStream os = client.getOutputStream();

		os.write(digest);
		os.flush();

		InputStream is = client.getInputStream();
		Header answer = new Header(is);

		logger.info("got answer " + answer + " for " + header);

		switch (answer.getMessageType()) {
		case DOWNLOAD_ACK:
			// go ahead
			break;
		case DOWNLOAD_ERR:
			logger.error("DOWNLOAD_ERR received!");
			IOUtils.closeQuietly(client);
			return;
		default:
			logger.fatal("client " + fh
					+ " have sent wrong messagetype of dwnld_rq " + header);
			IOUtils.closeQuietly(client);
			return;
		}

		Header ack;
		try {
			FileOutputStream fos = new FileOutputStream(target);
			BufferedOutputStream bos = new BufferedOutputStream(fos);

			logger.info("reading stream, length should be: "
					+ answer.getLength());
			long count = copyLarge(is, bos, answer.getLength());

			IOUtils.closeQuietly(bos);

			logger.info("stream read, file wrote");

			if (count != answer.getLength()) {
				logger.fatal("length mismatch of file  and answer.length");
			}
			logger.debug("compare of hashsum");

			byte[] hash = DigestUtils.md5(new FileInputStream(target));

			logger.debug("got hashsum: " + LocalFileList.md52str(hash)
					+ " should " + LocalFileList.md52str(digest));
			ack = Header.create(
					Arrays.equals(hash, digest) ? MessageType.DOWNLOAD_ACK
							: MessageType.DOWNLOAD_ERR, pseudonym, 0);
		} catch (Exception e) {
			ack = Header.create(MessageType.DOWNLOAD_ERR, pseudonym, 0);
		}

		logger.debug("send state of download: " + ack);
		os.write(ack.toByteArray());
		os.flush();
		IOUtils.closeQuietly(client);
	}

	public void sendEmptyUDP(MessageType mt) throws IOException {
		Header h = new Header();
		h.setPseudonym(pseudonym);
		h.setLength(0);
		h.setMessageType(mt);

		logger.info("info send empty udp-datagram with: " + h + " to: "
				+ broadcastAddress + ":" + port);

		byte buf[] = h.toByteArray();
		DatagramPacket outgoing = new DatagramPacket(buf, buf.length);
		outgoing.setPort(port);
		outgoing.setAddress(broadcastAddress);
		udpSocket.send(outgoing);
		logger.info("sent");
	}

	public void signin() throws IOException {
		logger.info("send signin");
		sendEmptyUDP(MessageType.SIGNIN);

		logger.info("start HeartBeatTimer");

		if (timer != null) // cancel all heartbeat timers
			timer.cancel();
		timer = new Timer("HEARTBEAT", true);
		timer.schedule(heartBeatTimer, 10 * 1000, 10 * 1000);
	}

	public void signout() throws IOException {
		logger.info("send signout");
		sendEmptyUDP(MessageType.SIGNOUT);

		// cancel heartbeat
		if (timer != null)
			timer.cancel();
	}

	public Socket openClientSocket(ForeignHost fh, Header header)
			throws IOException {
		logger.info("open tcp socket to: " + fh.getAddr() + ":" + port);
		logger.info("send header: " + header);

		Socket s = new Socket(fh.getAddr(), port);
		OutputStream os = s.getOutputStream();
		os.write(header.toByteArray());
		return s;
	}

	public void signin_ack(ForeignHost fh) throws IOException {
		logger.debug("signin_ack to: " + fh);
		Header h = Header.create(MessageType.SIGNIN_ACK, pseudonym, 0);
		Socket s = openClientSocket(fh, h);
		IOUtils.closeQuietly(s);
	}

	public void exchange_filelist(ForeignHost fh) throws IOException {
		logger.info("open tcp socket to: " + fh.getAddr() + ":" + port);
		Socket s = new Socket(fh.getAddr(), port);
		try {
			sendFilelist(s);
		} catch (IOException e) {
			logger.error("error at sending filelist", e);
		}
		IOUtils.closeQuietly(s);
	}

	public void exchange_filelist_req(ForeignHost fh) throws IOException {
		logger.info("exchange filelist request to: " + fh);
		Header h = Header.create(MessageType.EXCHANGE_FILELIST_REQ, pseudonym,
				0);
		Socket client = openClientSocket(fh, h);
		readFileList(fh, client);
		firePeerUpdate();
	}

	public static void readFileList(ForeignHost fh, Socket client)
			throws IOException {
		logger.info("reading file list");
		InputStream is = client.getInputStream();
		Header inHeader = new Header(is);

		byte[] content = new byte[(int) inHeader.getLength()];
		IOUtils.readFully(is, content);

		logger.info(content.length + " bytes received");

		ByteArrayInputStream bais = new ByteArrayInputStream(content);
		DataInputStream inputStream = new DataInputStream(bais);
		parseFileList(fh, inputStream);
		IOUtils.closeQuietly(inputStream);
	}

	public static void parseFileList(ForeignHost fh, DataInputStream inputStream)
			throws IOException {
		logger.debug("parse file list");
		while (inputStream.available() >= 1) {
			byte[] digest = new byte[DIGEST_LENGTH_BYTE];// TODO MD5

			long length = Header.read6ByteInt(inputStream);
			short fileNameLength = inputStream.readShort();
			IOUtils.readFully(inputStream, digest);
			String filename = Header.readUTF(inputStream, fileNameLength);

			logger.debug("parsed " + length + "," + fileNameLength + ","
					+ filename + "," + LocalFileList.md52str(digest));
			fh.addFile(filename, digest, length);
		}
	}

	public void heartbeat() throws IOException {
		logger.info("send heartbeat");
		sendEmptyUDP(MessageType.HEARTBEAT);
	}

	@SuppressWarnings("incomplete-switch")
	private void handleTCPRequest(Socket client) throws IOException {
		logger.info("handleTCPRequest of " + client.getRemoteSocketAddress());

		InputStream is = client.getInputStream();
		Header h = new Header(is);
		ForeignHost fh = getKnownHost(client.getInetAddress(), h);

		logger.info("Header: " + h);
		logger.info("ForeignHost Info: " + fh);

		switch (h.getMessageType()) {
		case EXCHANGE_FILELIST_REQ: // request for filelist
			sendFilelist(client);
			break;
		case EXCHANGE_FILELIST: // remote want to send filelist
			// e.g. after signin
			parseFileList(fh, new DataInputStream(is));
			firePeerUpdate();
			break;
		case DOWNLOAD_REQ:
			download_rpl(h, client);
			break;
		case SIGNIN_ACK:
			// TODO
			break;
		}
		logger.info("end request handling, closing socket");
		client.close();
	}

	private void download_rpl(Header h, Socket client) throws IOException {
		InputStream is = client.getInputStream();
		OutputStream os = client.getOutputStream();

		logger.debug("handle download for " + client.getRemoteSocketAddress());

		if (h.getLength() != DIGEST_LENGTH_BYTE) {
			logger.fatal("length mismatch, md5 is 16 byte, got: "
					+ h.getLength());
		}

		byte[] digest = new byte[(int) h.getLength()];
		IOUtils.readFully(is, digest);
		File fil = localFileList.get(digest);

		if (fil == null) {
			Header resp = Header.create(MessageType.DOWNLOAD_ERR, pseudonym, 0);
			os.write(resp.toByteArray());
			os.flush();
			IOUtils.closeQuietly(client);
		} else {
			BufferedInputStream bis = new BufferedInputStream(
					new FileInputStream(fil));
			Header resp = Header.create(MessageType.DOWNLOAD_ACK, pseudonym,
					fil.length());

			os.write(resp.toByteArray());
			IOUtils.copy(bis, os);
			IOUtils.closeQuietly(bis);
			// IOUtils.closeQuietly(os);

			Header response = new Header(is);
			switch (response.getMessageType()) {
			case DOWNLOAD_ACK:
				logger.info("Download successful");
				break;
			case DOWNLOAD_ERR:
				logger.info("Download unsuccessful");
				break;
			default:
				logger.fatal("no valid download response");
				break;
			}
		}
	}

	public void setPseudonym(String p)
	{
		pseudonym = p;
	}
	
	private void sendFilelist(Socket client) throws IOException {
		logger.debug("send filelist to client");

		byte[] b = localFileList.toByteArray();
		Header h = Header.create(MessageType.EXCHANGE_FILELIST, pseudonym,
				b.length);

		logger.debug("send header: " + h);
		logger.debug("send DATA: " + Arrays.toString(b));

		OutputStream os = client.getOutputStream();
		os.write(h.toByteArray());
		os.write(b);
		os.flush();
	}

	@SuppressWarnings("incomplete-switch")
	private void handleUDPRequest(DatagramPacket packet) throws IOException {
		logger.debug("handle udp request");

		Header h = new Header(packet);

		logger.info("got header: " + h);

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
		ForeignHost fh = getKnownHost(packet.getAddress(), h);
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
				heartbeat();
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
					logger.error("tcp-thread", e);
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
					logger.error("Error in udp-thread");
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
				logger.error("Error in udp-thread: " + p.getAddress(), e);
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
				logger.info("started clientThread for "
						+ client.getRemoteSocketAddress());

				handleTCPRequest(client);
			} catch (IOException e) {
				logger.error(
						"Error in client thread: "
								+ client.getRemoteSocketAddress(), e);
			}
		}
	}

	public void serve() {
		try {
			logger.debug("waiting for tcpServerThread to close");
			tcpServerThread.join();
		} catch (InterruptedException e) {
			logger.error(e);
		}
	}

	@SuppressWarnings("deprecation")
	public void quit() {
		logger.debug("quit requested by method call");

		logger.debug("tear down udpSocket");
		udpSocket.close();

		try {
			logger.debug("tear down tcpServerSocket");
			tcpServerSocket.close();
		} catch (IOException e) {
			logger.error("Error on quitting", e);
			tcpServerThread.stop();
		}
	}

	public LocalFileList getLocalFiles() {
		return localFileList;
	}

	public void onFilesListUpdated() {
		logger.debug("filelist change -> send info to peers");
		// TODO SEND EXCHG FILELIST TO ALL PEERS
		for (ForeignHost fh : knownHosts) {
			try {
				exchange_filelist(fh);
			} catch (IOException e) {
				logger.error(e);
			}
		}
	}

	public void addListener(AFSPHostListener lis) {
		listeners.add(lis);
	}

	public void firePeerUpdate() {
		logger.debug("firePeerUpdate");
		for (AFSPHostListener lis : listeners) {
			lis.onPeerUpdate();
		}
	}

	public List<ForeignHost> getForeignHosts() {
		return knownHosts;
	}

	private static final int EOF = -1;
	private static final int BUFFER_SIZE = 5 * 1024 * 1024;

	public static long copyLarge(InputStream input, OutputStream output,
			long max) throws IOException {
		byte buffer[] = new byte[BUFFER_SIZE];
		long count = 0;
		int n = 0;
		while (EOF != (n = input.read(buffer))) {
			output.write(buffer, 0, n);
			count += n;

			if (count >= max) {
				break;
			}
		}
		return count;
	}

}
