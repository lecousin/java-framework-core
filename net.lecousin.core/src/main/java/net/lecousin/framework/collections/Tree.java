package net.lecousin.framework.collections;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.lecousin.framework.util.ObjectUtil;

/**
 * Tree structure where each node contains an element of type T, and sub-nodes as a Tree.
 * @param <T> type of elements.
 */
public class Tree<T> {

	/** Tree node.
	 * @param <T> type of element
	 */
	public static class Node<T> {
		private T element;
		private Tree<T> subNodes = new Tree<>();
		
		public T getElement() { return element; }
		
		public Tree<T> getSubNodes() { return subNodes; }
	}
	
	private ArrayList<Node<T>> nodes = new ArrayList<>();
	
	/** Append a new node with the given element. */
	public Node<T> add(T element) {
		Node<T> node = new Node<>();
		node.element = element;
		nodes.add(node);
		return node;
	}
	
	/** Remove the first occurrence of the given element.
	 * @return true if an element has been removed
	 */
	public boolean remove(T element) {
		for (Iterator<Node<T>> it = nodes.iterator(); it.hasNext(); )
			if (ObjectUtil.equalsOrNull(it.next().element, element)) {
				it.remove();
				return true;
			}
		return false;
	}
	
	/** Remove the first occurrence of the given instance.
	 * @return true if an element has been removed
	 */
	public boolean removeInstance(T element) {
		for (Iterator<Node<T>> it = nodes.iterator(); it.hasNext(); )
			if (it.next().element == element) {
				it.remove();
				return true;
			}
		return false;
	}
	
	/** Get the node containing the given element. */
	public Node<T> get(T element) {
		for (Node<T> node : nodes)
			if (node.element.equals(element))
				return node;
		return null;
	}
	
	/** Get the node at the given index. */
	public Node<T> get(int index) {
		return nodes.get(index);
	}
	
	public List<Node<T>> getNodes() {
		return new ArrayList<>(nodes);
	}
	
	/** Return a list of elements contained in the nodes. */
	public List<T> getElements() {
		ArrayList<T> list = new ArrayList<>(nodes.size());
		for (Node<T> node : nodes)
			list.add(node.element);
		return list;
	}
	
	/** Return the number of elements. */
	public int size() { return nodes.size(); }
	
}
