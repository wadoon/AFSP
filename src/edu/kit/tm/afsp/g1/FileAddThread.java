package edu.kit.tm.afsp.g1;

import java.io.File;

import edu.kit.tm.afsp.g1.ui.MainFrame;

public class FileAddThread extends Thread {

	private File[] files;
	private FileData[] data;
	private LocalFileList list;
	private MainFrame f;
	
	public FileAddThread(File[] files, LocalFileList l,MainFrame f)
	{
		this.f = f;
		this.files = files;
		data = new FileData[files.length];
		list = l;
	}
	
	public void run()
	{
		int i = 0;
		for(File f : files)
		{
			data[i]= new FileData(f);
			i++;
		}
		list.addFiles(data,new FileViewUpdate(f));
	}
}
