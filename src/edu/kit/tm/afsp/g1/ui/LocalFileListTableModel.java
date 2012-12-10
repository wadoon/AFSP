package edu.kit.tm.afsp.g1.ui;

import java.io.File;

import javax.swing.table.AbstractTableModel;

import edu.kit.tm.afsp.g1.AFSPHost;
import edu.kit.tm.afsp.g1.LocalFileList;

public class LocalFileListTableModel extends AbstractTableModel {
    private static final long serialVersionUID = -3001909179793776769L;

    private String[] COLUMNS = { "Filename", "Digest" };

    private LocalFileList localFileList;

    public LocalFileListTableModel(AFSPHost afspHost) {
	this.localFileList = afspHost.getLocalFiles();
    }

    @Override
    public int getRowCount() {
	return localFileList.size();
    }

    @Override
    public int getColumnCount() {
	return COLUMNS.length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
	File f = localFileList.get(rowIndex);
	switch (columnIndex) {
	case 0:
	    return f.getName();
	case 1:
	    return localFileList.getDigest(f);
	}
	return "<n/a>";
    }

    @Override
    public String getColumnName(int column) {
	return COLUMNS[column];
    }
}