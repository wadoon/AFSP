package edu.kit.tm.afsp.g1;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.Logger;

public class DownloadThread extends Thread{

	private AFSPHost host;
	private ForeignHost fHost;
	private byte[] digest;
	private File target;
	private Logger logger;
	
	public DownloadThread(AFSPHost h, ForeignHost fh, byte[] digest, File target, Logger l)
	{
		host = h;
		fHost = fh;
		this.digest = digest;
		this.target = target;
		logger = l;
	}
	
	public void run()
	{
		try {
			host.download(fHost, digest, target);
		} catch (IOException e1) {
			logger.error(e1);
		}
	}

}
