package edu.kit.tm.afsp.g1.ui;

import javax.swing.table.AbstractTableModel;

import edu.kit.tm.afsp.g1.AFSPHost;

public class RemoteFileListTableModel extends AbstractTableModel {
    private static final long serialVersionUID = 9028959364184361725L;

    private final String[] COLUMNS = { "Host", "Filename", "Size", "Digest" };

    public RemoteFileListTableModel(AFSPHost afspHost) {
	// TODO Auto-generated constructor stub
    }

    @Override
    public int getRowCount() {
	// TODO Auto-generated method stub
	return 0;
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
    public Object getValueAt(int rowIndex, int columnIndex) {
	// TODO Auto-generated method stub
	return null;
    }

}
