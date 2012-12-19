package edu.kit.tm.afsp.g1.ui;

import java.awt.Color;
import java.awt.Component;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.ErrorHandler;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;

public class TableAppender extends JScrollPane {
	private static final long serialVersionUID = 1L;
	private LogTableModel model = new LogTableModel();
	JTable tbl = new JTable();
	private TableColumnAdjuster tableColumnAdjuster;

	public TableAppender() {
		tbl.setModel(model);
		setViewportView(tbl);

		tableColumnAdjuster = new TableColumnAdjuster(tbl);

		install((Logger) LogManager.getLogger("afsp"));
		install((Logger) LogManager.getLogger("afsp-ui"));

		tbl.setCellSelectionEnabled(false);
		tbl.setColumnSelectionAllowed(false);
		tbl.setRowSelectionAllowed(true);

		tbl.setDefaultRenderer(String.class, new DefaultLogTableRenderer());
		tbl.setDefaultRenderer(Long.class, new MilliSecondRender());
		tbl.setDefaultRenderer(Marker.class, new MarkerRender());
		tbl.setDefaultRenderer(Level.class, new LevelRender());
		tbl.setDefaultRenderer(Message.class, new MessageRender());
		tbl.setDefaultRenderer(SimpleMessage.class, new MessageRender());

		tbl.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

		tbl.getColumnModel().getColumn(0).setPreferredWidth(20);
		tbl.getColumnModel().getColumn(1).setMaxWidth(10);
		tbl.getColumnModel().getColumn(2).setMaxWidth(15);
		tbl.getColumnModel().getColumn(3).setMaxWidth(20);
		tbl.getColumnModel().getColumn(4).setMinWidth(800);
		tbl.getColumnModel().getColumn(4).setPreferredWidth(800);

		tbl.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

	}

	private void install(Logger logger) {
		logger.addAppender(model);
	}

	public void scrollDown() {
		int max = getVerticalScrollBar().getMaximum();
		getVerticalScrollBar().setValue(max);
		invalidate();
		repaint();
	}

	class LogTableModel extends AbstractTableModel implements
			Appender<Serializable> {
		private static final long serialVersionUID = 1846298727487403109L;
		private List<LogEvent> events = new ArrayList<>();
		private String[] COLUMNS = { "Time", "Name", "Level", "Thread",
				"Message" };

		@Override
		public int getRowCount() {
			return events.size();
		}

		@Override
		public int getColumnCount() {
			return COLUMNS.length;
		}

		@Override
		public String getColumnName(int column) {
			return COLUMNS[column];
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			switch (columnIndex) {
			case 0:
				return Long.class;
			case 2:
				return Level.class;
			case 4:
				return Message.class;
			default:
				return String.class;
			}
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			LogEvent evt = events.get(rowIndex);

			if (evt == null) {
				return "obj==null";
			}

			switch (columnIndex) {
			case 0:
				return evt.getMillis();
			case 1:
				return evt.getLoggerName();
			case 2:
				return evt.getLevel();
			case 3:
				return evt.getThreadName();
			case 4:
				return evt.getMessage();
			}
			return "<n/a>";
		}

		@Override
		public void start() {
		}

		@Override
		public void stop() {
		}

		@Override
		public boolean isStarted() {
			return true;
		}

		@Override
		public void append(LogEvent event) {
			events.add(event);
			fireTableRowsInserted(events.size() - 2, events.size() - 2);
			scrollDown();
			tableColumnAdjuster.adjustColumns();
		}

		@Override
		public String getName() {
			System.out.println("LogTableModel.getName()");
			return "test";
		}

		@Override
		public Layout<Serializable> getLayout() {
			return null;
		}

		@Override
		public boolean isExceptionSuppressed() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public ErrorHandler getHandler() {
			return null;
		}

		@Override
		public void setHandler(ErrorHandler handler) {

		}
	}

	class DefaultLogTableRenderer extends DefaultTableCellRenderer {

		private static final long serialVersionUID = 1L;

		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {
			JLabel lbl = (JLabel) super.getTableCellRendererComponent(table,
					value, isSelected, hasFocus, row, column);

			LogEvent le = model.events.get(row);

			setForeground(Color.BLACK);
			Color c;
			switch (le.getLevel()) {
			case FATAL:
				c = Color.BLACK;
				setForeground(Color.WHITE);
				break;
			case ERROR:
				c = new Color(250, 200, 200);
				break;
			case INFO:
				c = new Color(200, 220, 250);
				break;
			case DEBUG:
				c = new Color(220, 220, 220);
				break;
			default:
				c = Color.WHITE;
				break;
			}
			lbl.setOpaque(true);
			lbl.setBackground(c);

			return lbl;
		}
	}

	class MilliSecondRender extends DefaultLogTableRenderer {

		private static final long serialVersionUID = 1L;
		private DateFormat df = new SimpleDateFormat("h:m:s.S");

		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {

			long l = (Long) value;
			value = df.format(new Date(l));

			return super.getTableCellRendererComponent(table, value,
					isSelected, hasFocus, row, column);
		}
	}

	class MarkerRender extends DefaultLogTableRenderer {

		private static final long serialVersionUID = 1L;

		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {
			if (value != null) {
				Marker mark = (Marker) value;
				value = mark.getName();
			}
			return super.getTableCellRendererComponent(table, value,
					isSelected, hasFocus, row, column);
		}
	}

	class LevelRender extends DefaultLogTableRenderer {

		private static final long serialVersionUID = 1L;

		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {
			Level lvl = (Level) value;
			value = lvl.name();
			return super.getTableCellRendererComponent(table, value,
					isSelected, hasFocus, row, column);
		}
	}

	class MessageRender extends DefaultLogTableRenderer {

		private static final long serialVersionUID = 1L;

		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {
			Message msg = (Message) value;
			value = msg.getFormattedMessage();
			return super.getTableCellRendererComponent(table, value,
					isSelected, hasFocus, row, column);
		}
	}
}