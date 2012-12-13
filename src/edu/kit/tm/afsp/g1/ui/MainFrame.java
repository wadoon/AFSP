package edu.kit.tm.afsp.g1.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.kit.tm.afsp.g1.AFSPHost;
import edu.kit.tm.afsp.g1.AFSPHostListener;

public class MainFrame extends JFrame implements AFSPHostListener {
    private static final long serialVersionUID = 2025237748962798075L;

    private RemoveFileAction actRemoveFile = new RemoveFileAction();
    private AddFileAction actAddFile = new AddFileAction();
    public JButton btnRemoveFile = new JButton(actRemoveFile);
    public JButton btnAddFile = new JButton(actAddFile);
    public JTable tblLocalFiles;
    private JTable tblRemoteFiles;
    private AFSPHost afspHost;

    private Logger logger = LogManager.getLogger("afsp-ui");
    private RemoteFileListTableModel remoteDataModel;
    private LocalFileListTableModel localDataModel;

    public MainFrame(AFSPHost host) {
	afspHost = host;
	afspHost.addListener(this);

	setTitle("AFSP -- Gruppe1");
	logger.debug("construct frame");
	initUI();
    }

    private void initUI() {
	setLayout(new BorderLayout());
	JSplitPane jsp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

	jsp.setLeftComponent(initLeftComponent());
	jsp.setRightComponent(initRightComponent(this));

	add(jsp);
	setSize(1000, 500);
    }

    /**
     * @param mainFrame
     * @return
     */
    private Component initRightComponent(MainFrame mainFrame) {
	JPanel p = new JPanel(new BorderLayout());

	mainFrame.tblLocalFiles = new JTable();
	localDataModel = new LocalFileListTableModel(afspHost);
	tblLocalFiles.setModel(localDataModel);

	tblLocalFiles
		.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);

	tblLocalFiles.setColumnSelectionAllowed(false);
	tblLocalFiles.setCellSelectionEnabled(false);
	tblLocalFiles.setShowVerticalLines(false);
	tblLocalFiles.setRowSelectionAllowed(true);

	Box buttons = new Box(BoxLayout.X_AXIS);
	buttons.add(Box.createGlue());
	buttons.add(mainFrame.btnRemoveFile);
	buttons.add(mainFrame.btnAddFile);

	p.add(new JScrollPane(mainFrame.tblLocalFiles));
	p.add(buttons, BorderLayout.SOUTH);

	return p;
    }

    private Component initLeftComponent() {
	JPanel p = new JPanel(new BorderLayout());

	tblRemoteFiles = new JTable();
	remoteDataModel = new RemoteFileListTableModel(afspHost);
	tblRemoteFiles.setModel(remoteDataModel);

	tblRemoteFiles.setRowSelectionAllowed(true);
	tblRemoteFiles.setColumnSelectionAllowed(false);
	tblRemoteFiles.setCellSelectionEnabled(false);

	remoteDataModel.update();

	Box buttons = new Box(BoxLayout.X_AXIS);
	buttons.add(Box.createGlue());
	// buttons.add(btnRemoveFile);
	// buttons.add(btnAddFile);

	buttons.add(new JButton("TEST"));
	p.add(new JScrollPane(tblRemoteFiles));
	p.add(buttons, BorderLayout.SOUTH);

	return p;
    }

    class AddFileAction extends WAction {
	private static final long serialVersionUID = 1045963004149071917L;
	JFileChooser jfc = new JFileChooser(".");

	public AddFileAction() {
	    setName("Add File");
	    jfc.setMultiSelectionEnabled(true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
	    logger.debug("MainFrame.AddFileAction.actionPerformed()");

	    int c = jfc.showOpenDialog(MainFrame.this);
	    if (c == JFileChooser.APPROVE_OPTION) {
		File[] files = jfc.getSelectedFiles();

		for (File file : files) {
		    logger.debug("add file " + file);
		    afspHost.getLocalFiles().add(file);
		}
		afspHost.onFilesListUpdated();
		localDataModel.fireTableDataChanged();
		invalidate();
		repaint();
	    }
	}
    }

    class RemoveFileAction extends WAction {
	private static final long serialVersionUID = 1L;

	public RemoveFileAction() {
	    setName("Del File");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
	    int[] idx = tblLocalFiles.getSelectedRows();
	    File[] files = new File[idx.length];

	    for (int i = 0; i < idx.length; i++) {
		files[i] = afspHost.getLocalFiles().get(i);
	    }

	    for (File file : files) {
		afspHost.getLocalFiles().remove(file);
	    }
	    afspHost.onFilesListUpdated();

	    localDataModel.fireTableRowsDeleted(idx[0], idx[idx.length - 1]);
	    invalidate();
	    repaint();
	}
    }

    @Override
    public void onPeerUpdate() {
	remoteDataModel.fireTableDataChanged();
	remoteDataModel.update();
	remoteDataModel.fireTableDataChanged();
    }
}
