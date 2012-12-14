package edu.kit.tm.afsp.g1.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.flexdock.docking.DockingManager;
import org.flexdock.view.View;
import org.flexdock.view.Viewport;
import org.jdesktop.swingx.JXCollapsiblePane;
import org.jdesktop.swingx.JXCollapsiblePane.Direction;
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
    }

    private Component createContentPane() {
	JPanel p = new JPanel(new BorderLayout(0, 0));
	p.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

	Viewport viewport = new Viewport();
	p.add(viewport, BorderLayout.CENTER);
	
	View hostView = initRightComponent();
	View logView = createLogView();
	View localFilesView = initLeftComponent();
	View actionView = createTaskbar();

	viewport.dock(hostView);
	hostView.dock(logView, DockingManager.SOUTH_REGION, .3f);
	hostView.dock(localFilesView, DockingManager.EAST_REGION, .3f);
	hostView.dock(actionView, DockingManager.WEST_REGION, .2f);

	//
	// hostView.dock(view2, SOUTH_REGION, .3f);
	// hostView.dock(view4, EAST_REGION, .3f);
	// view1.dock(view3, SOUTH_REGION, .3f);

	return p;
    }

    private View createLogView() {
	View view = new View("logger", "Log");
	TableAppender ta = new TableAppender();
	view.add(ta);
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
	View view = new View("taskbar");
	JXTaskPaneContainer container = new JXTaskPaneContainer();

	JXTaskPane taskPane = new JXTaskPane("UDP");
	taskPane.add(actUDPSignIn);
	taskPane.add(actUDPSignOut);
	taskPane.add(actUDPHeartbeat);

	container.add(taskPane);
	view.add(container);
	return view;
    }

    /**
     * @param mainFrame
     * @return
     */
    private View initRightComponent() {
	View view = new View("host.view", "Network");
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

	Box buttons = new Box(BoxLayout.X_AXIS);
	buttons.add(Box.createGlue());
	buttons.add(btnRemoveFile);
	buttons.add(btnAddFile);

	p.add(new JScrollPane(tblLocalFiles));
	p.add(buttons, BorderLayout.SOUTH);

	view.add(p);
	return view;
    }

    private View initLeftComponent() {
	View view = new View("localfiles", "Local Files");
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

	view.add(p);
	return view;
    }

    class UDPSignInAction extends WAction {
	public UDPSignInAction() {
	    setText("SignIn ->");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
	    logger.debug("send signin, requested by user");
	}
    }

    class UDPSignOutAction extends WAction {
	public UDPSignOutAction() {
	    setText("SignOut ->");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
	    logger.debug("send signout, requested by user");
	}
    }

    class UDPHeartbeatAction extends WAction {
	public UDPHeartbeatAction() {
	    setText("Heartbeat ->");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
	    logger.debug("send signout, requested by user");
	}
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
