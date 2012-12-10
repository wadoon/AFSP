package edu.kit.tm.afsp.g1.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.kit.tm.afsp.g1.AFSPHost;

public class MainFrame extends JFrame {
    private static final long serialVersionUID = 2025237748962798075L;

    private RemoveFileAction actRemoveFile = new RemoveFileAction();
    private AddFileAction actAddFile = new AddFileAction();
    private JButton btnRemoveFile = new JButton(actRemoveFile);
    private JButton btnAddFile = new JButton(actAddFile);
    private JTable tblLocalFiles;
    private JTable tblRemoteFiles;
    private AFSPHost afspHost;

    private Logger logger = LogManager.getLogger("afsp-ui");

    public MainFrame(AFSPHost host) {
	afspHost = host;
	setTitle("AFSP -- Gruppe1");
	logger.debug("construct frame");
	initUI();
    }

    private void initUI() {
	setLayout(new BorderLayout());
	JSplitPane jsp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

	jsp.setLeftComponent(initLeftComponent());
	jsp.setRightComponent(initRightComponent());

	add(jsp);
	setSize(1000, 500);
    }

    private Component initRightComponent() {
	JPanel p = new JPanel(new BorderLayout());

	tblLocalFiles = new JTable();
	tblLocalFiles.setModel(new LocalFileListTableModel(afspHost));

	Box buttons = new Box(BoxLayout.X_AXIS);
	buttons.add(Box.createGlue());
	buttons.add(btnRemoveFile);
	buttons.add(btnAddFile);

	p.add(new JScrollPane(tblLocalFiles));
	p.add(buttons, BorderLayout.SOUTH);

	return p;
    }

    private Component initLeftComponent() {
	JPanel p = new JPanel(new BorderLayout());

	tblRemoteFiles = new JTable();
	tblRemoteFiles.setModel(new RemoteFileListTableModel(afspHost));

	Box buttons = new Box(BoxLayout.X_AXIS);
	buttons.add(Box.createGlue());
	// buttons.add(btnRemoveFile);
	// buttons.add(btnAddFile);

	p.add(new JScrollPane(tblRemoteFiles));
	p.add(buttons, BorderLayout.SOUTH);

	return p;
    }

    class AddFileAction extends WAction {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1045963004149071917L;

	public AddFileAction() {
	    setName("Add File");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
	    System.out.println("MainFrame.AddFileAction.actionPerformed()");
	}

    }

    class RemoveFileAction extends WAction {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public RemoveFileAction() {
	    setName("Del File");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
	    System.out.println("MainFrame.RemoveFileAction.actionPerformed()");
	}
    }
}
