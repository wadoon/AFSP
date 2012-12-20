package edu.kit.tm.afsp.g1;

import java.io.File;

import edu.kit.tm.afsp.g1.ui.MainFrame;

public class FileAddThread extends Thread {

	private File[] files;
	private LocalFileList list;
	private MainFrame frame;
	
	public FileAddThread(File[] files, LocalFileList l,MainFrame f)
	{
		this.frame = f;
		this.files = files;
		list = l;
	}
	
	public void run()
	{
		for(File f : files)
		{
			list.addFile(new FileData(f),new FileViewUpdate(frame));
		}
		
	}
}
