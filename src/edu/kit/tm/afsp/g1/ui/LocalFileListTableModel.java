package edu.kit.tm.afsp.g1.ui;

import java.io.File;

import javax.swing.table.AbstractTableModel;

import edu.kit.tm.afsp.g1.AFSPHost;
import edu.kit.tm.afsp.g1.LocalFileList;

public class LocalFileListTableModel extends AbstractTableModel {
    private static final long serialVersionUID = -3001909179793776769L;

    private String[] COLUMNS = { "Filename", "Size", "Digest" };

    private AFSPHost afspHost;

    public LocalFileListTableModel(AFSPHost afspHost) {
	this.afspHost = afspHost;
    }

    @Override
    public int getRowCount() {
	return afspHost.getLocalFiles().size();
    }

    @Override
    public int getColumnCount() {
	return COLUMNS.length;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
	return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
	File f = afspHost.getLocalFiles().get(rowIndex);
	switch (columnIndex) {
	case 0:
	    return f.getName();
	case 1:
	    return f.length();
	case 2:
	    return LocalFileList.md52str(afspHost.getLocalFiles().getDigest(f));
	}
	return "<n/a>";
    }

    @Override
    public String getColumnName(int column) {
	return COLUMNS[column];
    }
}