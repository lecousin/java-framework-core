package net.lecousin.framework.xml.dom;

import java.util.List;

import org.w3c.dom.NodeList;

/** DOM NodeList. */
public class XMLNodeList implements NodeList {

	/** Constructor. */
	public XMLNodeList(List<XMLNode> list) {
		this.list = list;
	}
	
	protected List<XMLNode> list;

	@Override
	public XMLNode item(int index) {
		if (list == null) return null;
		if (index < 0) return null;
		if (index >= list.size()) return null;
		return list.get(index);
	}

	@Override
	public int getLength() {
		if (list == null) return 0;
		return list.size();
	}
	
}
