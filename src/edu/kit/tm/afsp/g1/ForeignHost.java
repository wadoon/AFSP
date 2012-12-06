package edu.kit.tm.afsp.g1;

import java.net.InetAddress;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class ForeignHost {
	private InetAddress addr;
	private String pseudonym;
	private Date lastSeen;

	private Collection<String> publicFiles = new LinkedList<>();
	private Map<String, byte[]> publicFilesHashes = new HashMap<>();
	private Map<String, Long> publicFilesLengths = new HashMap<>();

	public ForeignHost() {
		lastSeen = new Date();
	}

	public ForeignHost(InetAddress addr, String pseudonym) {
		this();
		this.addr = addr;
		this.pseudonym = pseudonym;
	}

	public void addFile(String name, byte[] digest, long length) {
		publicFiles.add(name);
		publicFilesHashes.put(name, digest);
		publicFilesLengths.put(name, length);
	}

	public byte[] getHash(String file) {
		return publicFilesHashes.get(file);
	}

	public Collection<String> getPublicFiles() {
		return publicFiles;
	}

	public void setPublicFiles(Collection<String> publicFiles) {
		this.publicFiles = publicFiles;
	}

	public InetAddress getAddr() {
		return addr;
	}

	public void setAddr(InetAddress addr) {
		this.addr = addr;
	}

	public String getPseudonym() {
		return pseudonym;
	}

	public void setPseudonym(String pseudonym) {
		this.pseudonym = pseudonym;
	}

	public Date getLastSeen() {
		return lastSeen;
	}

	public void setLastSeen(Date lastSeen) {
		this.lastSeen = lastSeen;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((addr == null) ? 0 : addr.hashCode());
		result = prime * result
				+ ((pseudonym == null) ? 0 : pseudonym.hashCode());
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
		ForeignHost other = (ForeignHost) obj;
		if (addr == null) {
			if (other.addr != null)
				return false;
		} else if (!addr.equals(other.addr))
			return false;
		if (pseudonym == null) {
			if (other.pseudonym != null)
				return false;
		} else if (!pseudonym.equals(other.pseudonym))
			return false;
		return true;
	}

	public static ForeignHost create(InetAddress address, String pseudonym2) {
		ForeignHost fh = new ForeignHost();
		fh.setAddr(address);
		fh.setPseudonym(pseudonym2);
		return fh;
	}
}