package edu.kit.tm.afsp.g1;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import javax.swing.SwingUtilities;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.kit.tm.afsp.g1.ui.MainFrame;

public class Main {
    private static final byte _255 = (byte) 0xff;
    private static final int PORT = 8888;

    public static void main(String[] args) throws IOException {

	Logger logger = LogManager.getLogger("afsp");
	InetAddress broadcast = InetAddress.getByAddress(new byte[] { _255,
		_255, _255, _255 });

	logger.debug("Starting...");
	logger.debug("PORT: " + PORT);
	logger.debug("BROADCAST: " + broadcast);

	SocketAddress udpaddr = new InetSocketAddress(broadcast, PORT);
	SocketAddress tcpaddr = new InetSocketAddress(PORT);

	logger.debug("starting client");
	AFSPHost host = new AFSPHost("weigla", tcpaddr, PORT, udpaddr,
		broadcast);
	
	final MainFrame mf = new MainFrame(host);
	SwingUtilities.invokeLater(new Runnable() {
	    @Override
	    public void run() {
		mf.setVisible(true);
	    }
	});

	logger.debug("init signin");
	host.signin();
	logger.debug("waiting for serving");
	host.serve();

	logger.debug("push out signout");
	host.signout();

	host.quit();
    }

}
