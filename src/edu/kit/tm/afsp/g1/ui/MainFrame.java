package edu.kit.tm.afsp.g1.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.flexdock.docking.DockingManager;
import org.flexdock.view.View;
import org.flexdock.view.Viewport;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.JXTaskPane;
import org.jdesktop.swingx.JXTaskPaneContainer;
import org.jdesktop.swingx.painter.RectanglePainter;
import edu.kit.tm.afsp.g1.AFSPHost;
import edu.kit.tm.afsp.g1.AFSPHostListener;

public class MainFrame extends JFrame implements AFSPHostListener {
	private static final long serialVersionUID = 2025237748962798075L;

	private RemoveFileAction actRemoveFile = new RemoveFileAction();
	private AddFileAction actAddFile = new AddFileAction();

	private WAction actUDPSignIn = new UDPSignInAction();
	private WAction actUDPSignOut = new UDPSignOutAction();
	private WAction actUDPHeartbeat = new UDPHeartbeatAction();

	public JButton btnRemoveFile = new JButton(actRemoveFile);
	public JButton btnAddFile = new JButton(actAddFile);
	public JTable tblLocalFiles;
	private JTable tblRemoteFiles;
	private AFSPHost afspHost;

	private Logger logger = LogManager.getLogger("afsp-ui");
	private RemoteFileListTableModel remoteDataModel;
	private LocalFileListTableModel localDataModel;

	private WAction actTCPRequestFileList = new TCPRequestFileListAction();
	private WAction actTCPSendFileList = new TCPSendFileList();
	private WAction actTCPDownloadFile = new TCPDownloadFile();

	private File downloadDirectory = FileUtils.getUserDirectory();

	private String pseudonym = "default";

	public MainFrame(AFSPHost host) {
		afspHost = host;
		afspHost.addListener(this);
		setTitle("AFSP -- Gruppe1");
		logger.debug("construct frame");
		initUI();
	}

	private void initUI() {
		setLayout(new BorderLayout());
		add(createTitlePane(), BorderLayout.NORTH);
		add(createContentPane());
		setSize(1000, 500);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
	}

	private Component createContentPane() {
		JPanel p = new JPanel(new BorderLayout(0, 0));
		p.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		Viewport viewport = new Viewport();
		p.add(viewport, BorderLayout.CENTER);

		View hostView = createLocalFileView();
		View logView = createLogView();
		View localFilesView = createNetworkView();
		View actionView = createTaskbar();

		DockingManager.setFloatingEnabled(true);
		DockingManager.setAutoPersist(true);

		viewport.dock(hostView);
		hostView.dock(logView, DockingManager.SOUTH_REGION, .7f);
		hostView.dock(localFilesView, DockingManager.EAST_REGION, .5f);
		hostView.dock(actionView, DockingManager.WEST_REGION, .5f);

		return p;
	}

	private View createLogView() {
		View view = new View("logger", "Log", "Logger");

		view.setIcon(new ImageIcon(getClass().getResource(
				"assets/application_xp_terminal.png")));
		view.getContentPane().setLayout(new BorderLayout());

		JPanel p = new JPanel(new BorderLayout());
		TableAppender ta = new TableAppender();
		ta.setPreferredSize(new Dimension(1000, 300));
		p.add(ta);
		view.add(p);
		return view;
	}

	private Component createTitlePane() {
		JXPanel pane = new JXPanel(new BorderLayout(10, 10));
		pane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		final RectanglePainter p = new RectanglePainter();
		p.setAntialiasing(true);
		p.setFillPaint(new GradientPaint(new Point2D.Double(0, 0), Color.WHITE,
				new Point2D.Double(300, 50), new Color(200, 200, 255), false));
		pane.setBackgroundPainter(p);

		Box b = new Box(BoxLayout.X_AXIS);
		JLabel lblHead = new JLabel("AFSP");
		lblHead.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 30));
		b.add(lblHead);
		pane.add(b);
		return pane;
	}

	private View createTaskbar() {
		View view = new View("taskbar", "Task");
		view.getContentPane().setLayout(new BorderLayout());
		JXTaskPaneContainer container = new JXTaskPaneContainer();

		JXTaskPane test = new JXTaskPane("test");
		test.add(new CloseAction());
		test.add(new SelectDownloadFolder());
		test.add(new RenameUser());
		container.add(test);

		JXTaskPane localFileTasks = new JXTaskPane("Local Files");
		localFileTasks.add(actAddFile);
		localFileTasks.add(actRemoveFile);
		container.add(localFileTasks);

		JXTaskPane taskUDPPane = new JXTaskPane("UDP");
		taskUDPPane.add(actUDPSignIn);
		taskUDPPane.add(actUDPSignOut);
		taskUDPPane.add(actUDPHeartbeat);
		container.add(taskUDPPane);

		JXTaskPane taskTCPPane = new JXTaskPane("TCP");
		taskTCPPane.add(actTCPRequestFileList);
		taskTCPPane.add(actTCPSendFileList);
		taskTCPPane.add(actTCPDownloadFile);
		container.add(taskTCPPane);

		view.add(container);
		return view;
	}

	private View createLocalFileView() {
		View view = new View("localfiles", "Local Files");
		view.getContentPane().setLayout(new BorderLayout());
		JPanel p = new JPanel(new BorderLayout());

		tblLocalFiles = new JTable();
		localDataModel = new LocalFileListTableModel(afspHost);
		tblLocalFiles.setModel(localDataModel);

		tblLocalFiles
				.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);

		tblLocalFiles.setColumnSelectionAllowed(false);
		tblLocalFiles.setCellSelectionEnabled(false);
		tblLocalFiles.setShowVerticalLines(false);
		tblLocalFiles.setRowSelectionAllowed(true);

		tcaLocalTable = new TableColumnAdjuster(tblLocalFiles);
		tcaLocalTable.adjustColumns();

		Box buttons = new Box(BoxLayout.X_AXIS);
		buttons.add(Box.createGlue());
		buttons.add(btnRemoveFile);
		buttons.add(btnAddFile);

		p.add(new JScrollPane(tblLocalFiles));
		p.add(buttons, BorderLayout.SOUTH);

		view.add(p);
		return view;
	}

	TableColumnAdjuster tcaRemoteTable, tcaLocalTable;

	private View createNetworkView() {
		View view = new View("network", "Network");
		view.getContentPane().setLayout(new BorderLayout());
		JPanel p = new JPanel(new BorderLayout());

		tblRemoteFiles = new JTable();
		remoteDataModel = new RemoteFileListTableModel(afspHost);
		tblRemoteFiles.setModel(remoteDataModel);

		tblRemoteFiles.setColumnSelectionAllowed(false);
		tblRemoteFiles.setCellSelectionEnabled(false);
		tblRemoteFiles.setShowVerticalLines(false);
		tblRemoteFiles.setRowSelectionAllowed(true);

		remoteDataModel.update();

		tcaRemoteTable = new TableColumnAdjuster(tblRemoteFiles);
		tcaRemoteTable.adjustColumns();

		Box buttons = new Box(BoxLayout.X_AXIS);
		buttons.add(Box.createGlue());
		buttons.add(new JButton(actTCPDownloadFile));
		buttons.add(new JButton(actTCPSendFileList));
		buttons.add(new JButton(actTCPRequestFileList));
		p.add(new JScrollPane(tblRemoteFiles));
		p.add(buttons, BorderLayout.SOUTH);

		view.add(p);
		return view;
	}

	class UDPSignInAction extends WAction {
		private static final long serialVersionUID = -9022451576733958350L;

		public UDPSignInAction() {
			setText("SignIn");
			setAsset("connect");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			logger.debug("send signin, requested by user");
			try {
				afspHost.signin();
			} catch (IOException e1) {
				logger.error(e1);
			}
		}
	}

	class UDPSignOutAction extends WAction {
		private static final long serialVersionUID = 848598212549264585L;

		public UDPSignOutAction() {
			setText("SignOut");
			setAsset("disconnect");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			logger.debug("send signout, requested by user");
			try {
				afspHost.signout();
			} catch (IOException e1) {
				logger.error(e1);
			}
		}
	}

	class UDPHeartbeatAction extends WAction {
		private static final long serialVersionUID = 3328506465379579293L;

		public UDPHeartbeatAction() {
			setText("Heartbeat");
			setAsset("heart");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			logger.debug("send heartbeat, requested by user");
			try {
				afspHost.heartbeat();
			} catch (IOException e1) {
				logger.error(e1);
			}
		}
	}

	class AddFileAction extends WAction {
		private static final long serialVersionUID = 1045963004149071917L;
		JFileChooser jfc = new JFileChooser(FileUtils.getUserDirectory());

		public AddFileAction() {
			setName("Add File");
			setAsset("add");
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
				tcaLocalTable.adjustColumns();
				invalidate();
				repaint();

			}
		}
	}

	class RemoveFileAction extends WAction {
		private static final long serialVersionUID = 1L;

		public RemoveFileAction() {
			setName("Del File");
			setAsset("delete");
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
			tcaLocalTable.adjustColumns();
			invalidate();
			repaint();
		}
	}

	class TCPRequestFileListAction extends WAction {

		private static final long serialVersionUID = 8131996377989194539L;

		public TCPRequestFileListAction() {
			setName("Req. File List");
			setAsset("folder_explore");
		}

		@Override
		public void actionPerformed(ActionEvent e) {

			TableEntry te = remoteDataModel.getEntry(tblRemoteFiles
					.getSelectedRow());
			logger.debug("request filelist from: " + te);

			try {
				afspHost.exchange_filelist_req(te.getForeignHost());
			} catch (IOException e1) {
				logger.error(e1);
			}
		}
	}

	class TCPSendFileList extends WAction {

		private static final long serialVersionUID = -8243357669935903813L;

		public TCPSendFileList() {
			setText("Send FileList");
			setAsset("email_go");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			logger.debug("file xchg by user");
			int i = tblRemoteFiles.getSelectedRow();
			TableEntry te = remoteDataModel.getEntry(i);
			logger.debug("table entry: " + te);

			try {
				afspHost.exchange_filelist(te.getForeignHost());
			} catch (IOException e1) {
				logger.error(e1);
			}

		}
	}

	class TCPDownloadFile extends WAction {

		private static final long serialVersionUID = 6508260632607701044L;

		public TCPDownloadFile() {
			setText("Download");
			setAsset("control_fastforward_blue");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			logger.debug("download request by user");

			int i = tblRemoteFiles.getSelectedRow();
			TableEntry te = remoteDataModel.getEntry(i);
			logger.debug("table entry: " + te);

			File target = new File(downloadDirectory, te.getFilename());
			logger.info("download target is " + target.getAbsolutePath());
			try {
				afspHost.download(te.getForeignHost(), te.getDigest(), target);
			} catch (IOException e1) {
				logger.error(e1);
			}

		}
	}

	class CloseAction extends WAction {
		private static final long serialVersionUID = 1L;

		public CloseAction() {
			setText("Exit");
			setAsset("bomb");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				afspHost.signout();
			} catch (IOException e1) {
				logger.error(e1);
			}
			afspHost.quit();
			System.exit(0);
		}
	}

	class SelectDownloadFolder extends WAction {
		private static final long serialVersionUID = -6638753153441018855L;

		private JFileChooser jfc = new JFileChooser(downloadDirectory);

		public SelectDownloadFolder() {
			setText("Select DownloadFolder");
			setAsset("basket");

		}

		@Override
		public void actionPerformed(ActionEvent e) {
			int c = jfc.showOpenDialog(MainFrame.this);
			if (c == JFileChooser.APPROVE_OPTION) {
				downloadDirectory = jfc.getSelectedFile().getParentFile();
				logger.info("changed download folder to: "
						+ downloadDirectory.getAbsolutePath());
			}
		}
	}

	class RenameUser extends WAction {

		private static final long serialVersionUID = 1L;
		private String name = pseudonym;
		private boolean error = false;

		public RenameUser() {
			setText("Rename User");
			setAsset("user");

		}

		@Override
		public void actionPerformed(ActionEvent e) {
			do {
				if (error)
					name = JOptionPane
							.showInputDialog("Fehlerhafte Eingabe!\nGewünschtes Pseudonym angeben");
				else
					name = JOptionPane
							.showInputDialog("Gewünschtes Pseudonym angeben");
				error = true;
			} while (name.equals(""));
			if (name != null) {
				pseudonym = name;
				try {
					afspHost.signout();
					afspHost.setPseudonym(pseudonym);
					afspHost.signin();
				} catch (IOException e1) {
					logger.error(e1);
				}
			}
		}
	}

	@Override
	public void onPeerUpdate() {
		remoteDataModel.fireTableDataChanged();
		remoteDataModel.update();
		remoteDataModel.fireTableDataChanged();
		tcaRemoteTable.adjustColumns();
	}

}
