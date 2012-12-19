package edu.kit.tm.afsp.g1;

import edu.kit.tm.afsp.g1.ui.MainFrame;

public class FileViewUpdate implements Runnable{

	private MainFrame frame;
	
	public FileViewUpdate(MainFrame f)
	{
		frame = f;	
	}
	@Override
	public void run() {
		// TODO Auto-generated method stub
		frame.UpdateView();
	}
	
	

}
