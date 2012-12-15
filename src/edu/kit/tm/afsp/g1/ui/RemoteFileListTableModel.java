package edu.kit.tm.afsp.g1.ui;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import edu.kit.tm.afsp.g1.AFSPHost;
import edu.kit.tm.afsp.g1.ForeignHost;
import edu.kit.tm.afsp.g1.LocalFileList;

public class RemoteFileListTableModel extends AbstractTableModel {
    private static final long serialVersionUID = 9028959364184361725L;

    private final String[] COLUMNS = { "Host", "Filename", "Size", "LastUp",
	    "Digest" };

    private AFSPHost afspHost;
    private List<TableEntry> entries = new ArrayList<>();

    public RemoteFileListTableModel(AFSPHost afspHost) {
	this.afspHost = afspHost;
    }

    @Override
    public int getRowCount() {
	return entries.size();
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
	TableEntry te = entries.get(rowIndex);
	ForeignHost fh = te.fh;

	switch (columnIndex) {
	case 0:
	    return fh.getPseudonym() + "(" + fh.getAddr() + ")";
	case 1:
	    return te.filename;
	case 2:
	    return te.length;
	case 3:
	    return DateFormat.getTimeInstance(DateFormat.SHORT).format(
		    fh.getLastSeen());
	case 4:
	    return LocalFileList.md52str(te.digest);
	}
	return "<n/a>";
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
	return false;
    }

    public void update() {
	entries.clear();

	List<ForeignHost> list = afspHost.getForeignHosts();

	for (ForeignHost foreignHost : list) {
	    Collection<String> files = foreignHost.getPublicFiles();

	    if (files.size() == 0) {
		entries.add(TableEntry.create(foreignHost, "dummy entry", 0L,
			new byte[] { 0 }));
	    }

	    for (String file : files) {
		TableEntry te = TableEntry.create(foreignHost, file,
			foreignHost.getFileSize(file),
			foreignHost.getHash(file));
		entries.add(te);
	    }
	}

	fireTableDataChanged();
    }

    public TableEntry getEntry(int i) {
	return entries.get(i);
    }
}

class TableEntry {
    ForeignHost fh;
    String filename;
    byte[] digest;
    long length;

    public TableEntry(ForeignHost fh, String filename, byte[] digest,
	    long length) {
	this.fh = fh;
	this.filename = filename;
	this.digest = digest;
	this.length = length;
    }

    public static TableEntry create(ForeignHost foreignHost, String filename,
	    long length, byte[] digest) {
	return new TableEntry(foreignHost, filename, digest, length);
    }

    public ForeignHost getForeignHost() {
	return fh;
    }

    public void setFh(ForeignHost fh) {
	this.fh = fh;
    }

    public String getFilename() {
	return filename;
    }

    public void setFilename(String filename) {
	this.filename = filename;
    }

    public byte[] getDigest() {
	return digest;
    }

    public void setDigest(byte[] digest) {
	this.digest = digest;
    }

    public long getLength() {
	return length;
    }

    public void setLength(long length) {
	this.length = length;
    }

    @Override
    public String toString() {
	return "TableEntry [fh=" + fh + ", filename=" + filename + ", digest="
		+ Arrays.toString(digest) + ", length=" + length + "]";
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + Arrays.hashCode(digest);
	result = prime * result + ((fh == null) ? 0 : fh.hashCode());
	result = prime * result
		+ ((filename == null) ? 0 : filename.hashCode());
	result = prime * result + (int) (length ^ (length >>> 32));
	return result;
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj)
	    return true;
	if (obj == null)
	    return false;
	if (getClass() != obj.getClass())
	    return false;
	TableEntry other = (TableEntry) obj;
	if (!Arrays.equals(digest, other.digest))
	    return false;
	if (fh == null) {
	    if (other.fh != null)
		return false;
	} else if (!fh.equals(other.fh))
	    return false;
	if (filename == null) {
	    if (other.filename != null)
		return false;
	} else if (!filename.equals(other.filename))
	    return false;
	if (length != other.length)
	    return false;
	return true;
    }

}
