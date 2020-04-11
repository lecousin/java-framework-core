package net.lecousin.framework.collections.sort;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

import net.lecousin.framework.util.ObjectUtil;

/**
 * Sorted list, where each element is associated with a long value used to compare elements.
 * To sort the elements, a <a href=https://en.wikipedia.org/wiki/Red%E2%80%93black_tree">red-black tree</a> is used.
 * @param <T> type of elements
 */
public class RedBlackTreeLong<T> implements Sorted.AssociatedWithLong<T> {

	/** Node in the RedBlackTree.
	 * @param <T> type of element
	 */
	public static class Node<T> {
		private long value;
		private T element;
		private Node<T> left;
		private Node<T> right;
		private boolean red;
		private int n = 1;
		
		private Node(long value, T element, boolean red) {
			this.value = value;
			this.element = element;
			this.red = red;
		}
		
		/** Return the value hold by this node. */
		public long getValue() { return value; }
		
		/** Return the element hold by this node. */
		public T getElement() { return element; }
		
		/** Replace the element hold by this node. */
		public void setElement(T element) { this.element = element; }
	}
	
	private Node<T> root = null;
	private Node<T> first = null;
	private Node<T> last = null;
	
    @Override
	public int size() { return root == null ? 0 : root.n; }

    // is this symbol table empty?
    @Override
	public boolean isEmpty() {
        return root == null;
    }
    
    @Override
    public void clear() {
    	root = first = last = null;
    }
    
	// ----------------
	// ---- search ----
	// ----------------

	/**
     * Returns the node associated to the given key, or null if no such key exists.
     */
	public Node<T> get(long value) {
    	if (root == null) return null;
    	if (value == first.value) return first;
    	if (value == last.value) return last;
    	if (value < first.value) return null;
    	if (value > last.value) return null;
    	return get(root, value);
    }

    // value associated with the given key in subtree rooted at x; null if no such key
    private Node<T> get(Node<T> x, long key) {
        while (x != null) {
        	if (x.value == key) return x;
        	if (key < x.value) x = x.left;
        	else x = x.right;
        }
        return null;
    }
    
	/**
     * Returns the node containing the key just before the given key.
     */
	public Node<T> getPrevious(long value) {
    	if (value == first.value) return null;
    	if (value == root.value) {
    		if (root.left == null) return null;
    		Node<T> n = root.left;
    		while (n.right != null) n = n.right;
    		return n;
    	}
    	if (value < root.value) {
    		if (root.left == null) return null;
    		return getPrevious(root.left, value);
    	}
    	if (root.right == null) return null;
    	return getPrevious(root, root.right, value);
    }
	
    /**
     * Returns the node containing the key just before the key of the given node.
     */
    public Node<T> getPrevious(Node<T> node) {
    	if (node.left != null) {
    		node = node.left;
    		while (node.right != null) node = node.right;
    		return node;
    	}
    	// nothing at left, it may be the parent node if the node is at the right
    	if (node == first) return null;
    	return getPrevious(root, node.value);
    }
    
    private Node<T> getPrevious(Node<T> node, long value) {
    	if (value == node.value) {
    		if (node.left == null) return null;
    		node = node.left;
    		while (node.right != null) node = node.right;
    		return node;
    	}
    	if (value < node.value) {
    		if (node.left == null) return null;
    		return getPrevious(node.left, value);
    	}
    	if (node.right == null) return null;
    	return getPrevious(node, node.right, value);
    }
    
    private Node<T> getPrevious(Node<T> parentPrevious, Node<T> node, long value) {
    	if (node.value == value) {
    		if (node.left == null) return parentPrevious;
    		node = node.left;
    		while (node.right != null) node = node.right;
    		return node;
    	}
    	if (value < node.value) {
    		if (node.left == null) return null;
    		return getPrevious(parentPrevious, node.left, value);
    	}
    	if (node.right == null) return null;
    	return getPrevious(node, node.right, value);
    }

    /**
     * Returns the node containing the key just before the given key.
     */
    public Node<T> getNext(long value) {
    	if (value == last.value) return null;
    	if (value == root.value) {
    		if (root.right == null) return null;
    		Node<T> n = root.right;
    		while (n.left != null) n = n.left;
    		return n;
    	}
    	if (value > root.value) {
    		if (root.right == null) return null;
    		return getNext(root.right, value);
    	}
    	if (root.left == null) return null;
    	return getNext(root, root.left, value);
    }
    
    /**
     * Returns the node containing the key just before the key of the given node.
     */
    public Node<T> getNext(Node<T> node) {
    	if (node.right != null) {
    		node = node.right;
    		while (node.left != null) node = node.left;
    		return node;
    	}
    	// nothing at right, it may be the parent node if the node is at the left
    	if (node == last) return null;
    	return getNext(root, node.value);
    }
    
    private Node<T> getNext(Node<T> node, long value) {
    	if (value == node.value) {
    		if (node.right == null) return null;
    		node = node.right;
    		while (node.left != null) node = node.left;
    		return node;
    	}
    	if (value > node.value) {
    		if (node.right == null) return null;
    		return getNext(node.right, value);
    	}
    	if (node.left == null) return null;
    	return getNext(node, node.left, value);
    }
    
    private Node<T> getNext(Node<T> parentNext, Node<T> node, long value) {
    	if (node.value == value) {
    		if (node.right == null) return parentNext;
    		node = node.right;
    		while (node.left != null) node = node.left;
    		return node;
    	}
    	if (value > node.value) {
    		if (node.right == null) return null;
    		return getNext(parentNext, node.right, value);
    	}
    	if (node.left == null) return null;
    	return getNext(node, node.left, value);
    }
    
    
    /**
     * Returns true if the given key exists in the tree.
     */
    public boolean containsKey(long value) {
        return get(value) != null;
    }
    
    /**
     * Returns true if the given key exists in the tree and its associated value is the given element.
     * Comparison of the element is using the equals method.
     */
    @Override
    public boolean contains(long value, T element) {
    	if (root == null) return false;
    	if (value < first.value) return false;
    	if (value > last.value) return false;
    	if (value == first.value && ObjectUtil.equalsOrNull(element, first.element)) return true;
    	if (value == last.value && ObjectUtil.equalsOrNull(element, last.element)) return true;
    	return contains(root, value, element);
    }
    
    private boolean contains(Node<T> x, long key, T element) {
        while (x != null) {
        	if (key < x.value) x = x.left;
        	else {
        		if (x.value == key && ObjectUtil.equalsOrNull(x.element, element)) return true;
        		x = x.right;
        	}
        }
        return false;
    }
    
    /**
     * Returns true if the given key exists in the tree and its associated value is the given element.
     * Comparison of the element is using the == operator.
     */
    @Override
    public boolean containsInstance(long value, T instance) {
    	if (root == null) return false;
    	if (value < first.value) return false;
    	if (value > last.value) return false;
    	if (value == first.value && instance == first.element) return true;
    	if (value == last.value && instance == last.element) return true;
    	return containsInstance(root, value, instance);
    }
    
    private boolean containsInstance(Node<T> x, long key, T instance) {
        while (x != null) {
        	if (key < x.value) x = x.left;
        	else {
        		if (x.value == key && x.element == instance) return true;
        		x = x.right;
        	}
        }
        return false;
    }
    
    public Node<T> getMin() { return first; }
    
    public Node<T> getMax() { return last; }
    
    /**
     * Returns the node containing the highest value strictly lower than the given one.
     */
    public Node<T> searchNearestLower(long value, boolean acceptEquals) {
    	if (root == null) return null;
    	return searchNearestLower(root, value, acceptEquals);
    }
    
    private Node<T> searchNearestLower(Node<T> node, long value, boolean acceptEquals) {
    	if (value < node.value) {
    		if (node.left == null) return null;
    		return searchNearestLower(node.left, value, acceptEquals);
    	}
    	if (value > node.value) {
    		if (node.right == null) return node;
    		if (value > node.right.value) return searchNearestLower(node.right, value, acceptEquals);
    		if (value == node.right.value) {
    			if (acceptEquals) return node.right;
    			Node<T> n = searchNearestLower(node.right, value, false);
    			if (n == null) return node;
    			return n;
    		}
    		Node<T> n = searchNearestLower(node.right, value, acceptEquals);
    		if (n == null) return node;
    		return n;
    	}
    	if (acceptEquals) return node;
    	if (node.left == null) return null;
    	Node<T> n = node.left;
    	while (n.right != null) n = n.right;
    	return n;
    }
    
    /**
     * Returns the node containing the lowest value strictly higher than the given one.
     */
    public Node<T> searchNearestHigher(long value, boolean acceptEquals) {
    	if (root == null) return null;
    	return searchNearestHigher(root, value, acceptEquals);
    }
    
    private Node<T> searchNearestHigher(Node<T> node, long value, boolean acceptEquals) {
    	if (value > node.value) {
    		if (node.right == null) return null;
    		return searchNearestHigher(node.right, value, acceptEquals);
    	}
    	if (value < node.value) {
    		if (node.left == null) return node;
    		if (value < node.left.value) return searchNearestHigher(node.left, value, acceptEquals);
    		if (value == node.left.value) {
    			if (acceptEquals) return node.left;
    			Node<T> n = searchNearestHigher(node.left, value, acceptEquals);
    			if (n == null) return node;
    			return n;
    		}
    		Node<T> n = searchNearestHigher(node.left, value, acceptEquals);
    		if (n == null) return node;
    		return n;
    	}
    	if (acceptEquals) return node;
    	if (node.right == null) return null;
    	Node<T> n = node.right;
    	while (n.left != null) n = n.left;
    	return n;
    }
    
	// -----------------
	// --- insertion ---
	// -----------------
	
	@Override
	public void add(long value, T element) {
		if (root == null) {
			root = new Node<>(value, element, false);
			first = last = root;
			return;
		}
		root = add(value, element, root);
		root.red = false;
	}
	
	private Node<T> add(long value, T element, Node<T> h) {
		if (value < h.value) {
			if (h.left == null) {
				h.left = new Node<>(value, element, true);
				if (value < first.value) first = h.left;
			} else {
				h.left = add(value, element, h.left);
			}
		} else if (value >= h.value) {
			if (h.right == null) {
				h.right = new Node<>(value, element, true);
				if (value > last.value || last == h) last = h.right;
			} else {
				h.right = add(value, element, h.right);
			}
		}

		if (h.right != null && h.right.red && (h.left == null || !h.left.red))
			h = rotateLeft(h);
		if (h.left != null && h.left.red && h.left.left != null && h.left.left.red)
			h = rotateRight(h);
        if (h.left != null && h.left.red && h.right != null && h.right.red)
        	flipColors(h);
        h.n = (h.left == null ? 0 : h.left.n) + (h.right == null ? 0 : h.right.n) + 1;
        return h;
	}
	
	// ----------------
	// --- deletion ---
	// ----------------
	
	/** Remove the first element. */
	public void removeMin() {
    	if (root == null)
    		return;

        // if both children of root are black, set root to red
        if ((root.left == null || !root.left.red) && (root.right == null || !root.right.red))
            root.red = true;

        root = removeMin(root, true);
        if (root != null) root.red = false;
        else first = last = null;
    }

    // delete the key-value pair with the minimum key rooted at h
    private Node<T> removeMin(Node<T> h, boolean updateFirst) { 
        if (h.left == null)
            return null;

        if (!h.left.red && (h.left.left == null || !h.left.left.red))
            h = moveRedLeft(h);

        h.left = removeMin(h.left, updateFirst);
        if (h.left == null && updateFirst)
        	first = h;
        return balance(h);
    }

    /** Remove the last element. */
    public void removeMax() {
    	if (root == null)
    		return;

        // if both children of root are black, set root to red
        if ((root.left == null || !root.left.red) && (root.right == null || !root.right.red))
            root.red = true;

        root = removeMax(root, true);
        if (root != null) root.red = false;
        else first = last = null;
    }

    // delete the key-value pair with the maximum key rooted at h
    private Node<T> removeMax(Node<T> h, boolean updateLast) { 
        if (h.left != null && h.left.red)
            h = rotateRight(h);

        if (h.right == null)
            return null;

        if (!h.right.red && (h.right.left == null || !h.right.left.red))
            h = moveRedRight(h);

        h.right = removeMax(h.right, updateLast);
        if (h.right == null && updateLast) last = h;
        return balance(h);
    }
    
    /** Remove the given node. */
    public void remove(Node<T> node) {
    	if (first == node) {
    		removeMin();
    		return;
    	}
    	if (last == node) {
    		removeMax();
    		return;
    	}
    	// if both children of root are black, set root to red
    	if ((root.left == null || !root.left.red) && (root.right == null || !root.right.red))
            root.red = true;

        root = remove(root, node);
        if (root != null) root.red = false;
        else first = last = null;
    }

    private Node<T> remove(Node<T> h, Node<T> node) { 
        if (node.value < h.value)  {
        	if (h.left == null || (!h.left.red && (h.left.left == null || !h.left.left.red)))
                h = moveRedLeft(h);
            h.left = remove(h.left, node);
        } else {
            if (h.left != null && h.left.red)
                h = rotateRight(h);
            if (node.value == h.value && (h.right == null))
                return null;
            if (h.right == null || (!h.right.red && (h.right.left == null || !h.right.left.red)))
                h = moveRedRight(h);
            if (node.value == h.value) {
                Node<T> x = min(h.right);
                h.value = x.value;
                h.element = x.element;
                h.right = removeMin(h.right, false);
            } else {
            	h.right = remove(h.right, node);
            }
        }
        return balance(h);
    }
    
    /**
     * Remove the node containing the given key and element. The node MUST exists, else the tree won't be valid anymore.
     */
    @Override
    public void remove(long key, T element) {
    	if (first.value == key && ObjectUtil.equalsOrNull(element, first.element)) {
    		removeMin();
    		return;
    	}
    	if (last.value == key && ObjectUtil.equalsOrNull(element, last.element)) {
    		removeMax();
    		return;
    	}
    	
    	// if both children of root are black, set root to red
    	if ((root.left == null || !root.left.red) && (root.right == null || !root.right.red))
            root.red = true;

        root = remove(root, key, element);
        if (root != null) root.red = false;
    }
    
    // delete the key-value pair with the given key rooted at h
    private Node<T> remove(Node<T> h, long key, T element) { 
        if (key < h.value)  {
        	if (h.left == null || (!h.left.red && (h.left.left == null || !h.left.left.red)))
                h = moveRedLeft(h);
            h.left = remove(h.left, key, element);
        } else {
            if (h.left != null && h.left.red)
                h = rotateRight(h);
            if (key == h.value && ObjectUtil.equalsOrNull(element, h.element) && (h.right == null))
                return null;
            if (h.right == null || (!h.right.red && (h.right.left == null || !h.right.left.red)))
                h = moveRedRight(h);
            if (key == h.value && ObjectUtil.equalsOrNull(element, h.element)) {
                Node<T> x = min(h.right);
                h.value = x.value;
                h.element = x.element;
                h.right = removeMin(h.right, false);
            } else {
            	h.right = remove(h.right, key, element);
            }
        }
        return balance(h);
    }

    /**
     * Remove the given key. The key MUST exists, else the tree won't be valid anymore.
     */
    public void removeKey(long key) { 
    	if (key == first.value) {
    		removeMin();
    		return;
    	}
    	if (key == last.value) {
    		removeMax();
    		return;
    	}
    	// if both children of root are black, set root to red
    	if ((root.left == null || !root.left.red) && (root.right == null || !root.right.red))
            root.red = true;

        root = removeKey(root, key);
        if (root != null) root.red = false;
    }
    
    // delete the key-value pair with the given key rooted at h
    private Node<T> removeKey(Node<T> h, long key) { 
        if (key < h.value)  {
        	if (h.left == null || (!h.left.red && (h.left.left == null || !h.left.left.red)))
                h = moveRedLeft(h);
            h.left = removeKey(h.left, key);
        } else {
            if (h.left != null && h.left.red)
                h = rotateRight(h);
            if (key == h.value && (h.right == null))
                return null;
            if (h.right == null || (!h.right.red && (h.right.left == null || !h.right.left.red)))
                h = moveRedRight(h);
            if (key == h.value) {
                Node<T> x = min(h.right);
                h.value = x.value;
                h.element = x.element;
                h.right = removeMin(h.right, false);
            } else {
            	h.right = removeKey(h.right, key);
            }
        }
        return balance(h);
    }

    /**
     * Remove the node containing the given key and element. The node MUST exists, else the tree won't be valid anymore.
     */
    @Override
    public void removeInstance(long key, T instance) {
    	if (first.value == key && instance == first.element) {
    		removeMin();
    		return;
    	}
    	if (last.value == key && instance == last.element) {
    		removeMax();
    		return;
    	}

    	// if both children of root are black, set root to red
    	if ((root.left == null || !root.left.red) && (root.right == null || !root.right.red))
            root.red = true;

        root = removeInstance(root, key, instance);
        if (root != null) root.red = false;
    }
    
    private Node<T> removeInstance(Node<T> h, long key, T instance) { 
        if (key < h.value)  {
        	if (h.left == null || (!h.left.red && (h.left.left == null || !h.left.left.red)))
                h = moveRedLeft(h);
            h.left = removeInstance(h.left, key, instance);
        } else {
            if (h.left != null && h.left.red)
                h = rotateRight(h);
            if (key == h.value && instance == h.element && (h.right == null))
                return null;
            if (h.right == null || (!h.right.red && (h.right.left == null || !h.right.left.red)))
                h = moveRedRight(h);
            if (key == h.value && instance == h.element) {
                Node<T> x = min(h.right);
                h.value = x.value;
                h.element = x.element;
                h.right = removeMin(h.right, false);
            } else {
            	h.right = removeInstance(h.right, key, instance);
            }
        }
        return balance(h);
    }
	
	// ------------------------
	// --- helper functions ---
	// ------------------------

	
	// make a left-leaning link lean to the right
	private Node<T> rotateRight(Node<T> h) {
		// assert (h != null) && isRed(h.left);
		Node<T> x = h.left;
		h.left = x.right;
		x.right = h;
		x.red = x.right.red;
		x.right.red = true;
		x.n = h.n;
		h.n = (h.left == null ? 0 : h.left.n) + (h.right == null ? 0 : h.right.n) + 1;
		return x;
    }

    // make a right-leaning link lean to the left
    private Node<T> rotateLeft(Node<T> h) {
        // assert (h != null) && isRed(h.right);
        Node<T> x = h.right;
        h.right = x.left;
        x.left = h;
        x.red = x.left.red;
        x.left.red = true;
        x.n = h.n;
        h.n = (h.left == null ? 0 : h.left.n) + (h.right == null ? 0 : h.right.n) + 1;
        return x;
    }

    // flip the colors of a node and its two children
    private void flipColors(Node<T> h) {
        // h must have opposite color of its two children
        // assert (h != null) && (h.left != null) && (h.right != null);
        // assert (!isRed(h) &&  isRed(h.left) &&  isRed(h.right))
        //    || (isRed(h)  && !isRed(h.left) && !isRed(h.right));
        h.red = !h.red;
        h.left.red = !h.left.red;
        h.right.red = !h.right.red;
    }
	
    // Assuming that h is red and both h.left and h.left.left
    // are black, make h.left or one of its children red.
    private Node<T> moveRedLeft(Node<T> h) {
        // assert (h != null);
        // assert isRed(h) && !isRed(h.left) && !isRed(h.left.left);

        flipColors(h);
        if (h.right.left != null && h.right.left.red) { 
            h.right = rotateRight(h.right);
            h = rotateLeft(h);
            flipColors(h);
        }
        return h;
    }

    // Assuming that h is red and both h.right and h.right.left
    // are black, make h.right or one of its children red.
    private Node<T> moveRedRight(Node<T> h) {
        // assert (h != null);
        // assert isRed(h) && !isRed(h.right) && !isRed(h.right.left);
        flipColors(h);
        if (h.left.left != null && h.left.left.red) { 
            h = rotateRight(h);
            flipColors(h);
        }
        return h;
    }

    // restore red-black tree invariant
    private Node<T> balance(Node<T> h) {
        // assert (h != null);

        if (h.right != null && h.right.red) h = rotateLeft(h);
        if (h.left != null && h.left.red && h.left.left != null && h.left.left.red) h = rotateRight(h);
        if (h.left != null && h.left.red && h.right != null && h.right.red) flipColors(h);

        h.n = (h.left == null ? 0 : h.left.n) + (h.right == null ? 0 : h.right.n) + 1;
        return h;
    }
    
    // the smallest key in subtree rooted at x; null if no such key
    private Node<T> min(Node<T> x) {
    	do {
    		if (x.left == null) return x;
    		x = x.left;
    	} while (true);
    }
    
    @Override
    public Iterator<T> iterator() { return new RBTreeIterator(root); }
    
    public Iterator<Node<T>> nodeIterator() { return new RBTreeNodeIterator(root); }
    
    public Iterator<Node<T>> nodeIteratorOrdered() { return new NodeIteratorOrdered<>(root); }

    public Iterator<Node<T>> nodeIteratorReverse() { return new NodeIteratorReverseOrder<>(root); }
    
    @Override
    public Iterator<T> orderedIterator() { return new IteratorOrdered<>(root); }
    
    @Override
    public Iterator<T> reverseOrderIterator() { return new IteratorReverseOrder<>(root); }
    
    private class RBTreeIterator implements Iterator<T> {
    	private RBTreeIterator(Node<T> node) {
    		this.node = node;
    	}
    	
    	private Node<T> node;
    	private RBTreeIterator leftIterator = null;
    	private RBTreeIterator rightIterator = null;
    	
    	@Override
    	public boolean hasNext() {
    		return (node != null || leftIterator != null || rightIterator != null);
    	}
    	
    	@Override
    	public T next() {
    		if (node != null) {
    			if (node.left != null) leftIterator = new RBTreeIterator(node.left);
    			if (node.right != null) rightIterator = new RBTreeIterator(node.right);
    			T e = node.element;
    			node = null;
    			return e;
    		}
    		if (leftIterator != null) {
    			T e = leftIterator.next();
    			if (!leftIterator.hasNext()) leftIterator = null;
    			return e;
    		}
    		T e = rightIterator.next();
    		if (!rightIterator.hasNext()) rightIterator = null;
    		return e;
    	}
    }
    
    private class RBTreeNodeIterator implements Iterator<Node<T>> {
    	private RBTreeNodeIterator(Node<T> node) {
    		this.node = node;
    	}
    	
    	private Node<T> node;
    	private RBTreeNodeIterator leftIterator = null;
    	private RBTreeNodeIterator rightIterator = null;
    	
    	@Override
    	public boolean hasNext() {
    		return (node != null || leftIterator != null || rightIterator != null);
    	}
    	
    	@Override
    	public Node<T> next() {
    		if (node != null) {
    			if (node.left != null) leftIterator = new RBTreeNodeIterator(node.left);
    			if (node.right != null) rightIterator = new RBTreeNodeIterator(node.right);
    			Node<T> n = node;
    			node = null;
    			return n;
    		}
    		if (leftIterator != null) {
    			Node<T> n = leftIterator.next();
    			if (!leftIterator.hasNext()) leftIterator = null;
    			return n;
    		}
    		Node<T> n = rightIterator.next();
    		if (!rightIterator.hasNext()) rightIterator = null;
    		return n;
    	}
    }

    private static class NodeIteratorOrdered<T> implements Iterator<Node<T>> {
    	private NodeIteratorOrdered(Node<T> root) {
    		next = root;
    		if (root == null) return;
    		parents = new ArrayList<>(root.n / 2);
    		// go to the left
    		while (next.left != null) {
    			parents.add(next);
    			next = next.left;
    		}
    	}
    	
    	private ArrayList<Node<T>> parents;
    	private Node<T> next;
    	
    	@Override
    	public boolean hasNext() { return next != null; }
    	
    	@Override
    	public Node<T> next() {
    		if (next == null) throw new NoSuchElementException();
    		Node<T> res = next;
    		if (next.right != null) {
    			next = next.right;
    			while (next.left != null) {
    				parents.add(next);
    				next = next.left;
    			}
    			return res;
    		}
    		if (parents.isEmpty()) {
    			next = null;
    			return res;
    		}
    		next = parents.remove(parents.size() - 1);
    		return res;
    	}
    }

    private static class IteratorOrdered<T> implements Iterator<T> {
    	private IteratorOrdered(Node<T> root) {
    		it = new NodeIteratorOrdered<>(root);
    	}
    	
    	private NodeIteratorOrdered<T> it;
    	
    	@Override
    	public boolean hasNext() { return it.hasNext(); }
    	
    	@Override
    	public T next() { return it.next().getElement(); }
    }
    
    private static class NodeIteratorReverseOrder<T> implements Iterator<Node<T>> {
    	private NodeIteratorReverseOrder(Node<T> root) {
    		next = root;
    		if (root == null) return;
    		parents = new ArrayList<>(root.n / 2);
    		// go to the right
    		while (next.right != null) {
    			parents.add(next);
    			next = next.right;
    		}
    	}
    	
    	private ArrayList<Node<T>> parents;
    	private Node<T> next;
    	
    	@Override
    	public boolean hasNext() { return next != null; }
    	
    	@Override
    	public Node<T> next() {
    		if (next == null) throw new NoSuchElementException();
    		Node<T> res = next;
    		if (next.left != null) {
    			next = next.left;
    			while (next.right != null) {
    				parents.add(next);
    				next = next.right;
    			}
    			return res;
    		}
    		if (parents.isEmpty()) {
    			next = null;
    			return res;
    		}
    		next = parents.remove(parents.size() - 1);
    		return res;
    	}
    }

    private static class IteratorReverseOrder<T> implements Iterator<T> {
    	private IteratorReverseOrder(Node<T> root) {
    		it = new NodeIteratorReverseOrder<>(root);
    	}
    	
    	private NodeIteratorReverseOrder<T> it;
    	
    	@Override
    	public boolean hasNext() { return it.hasNext(); }
    	
    	@Override
    	public T next() { return it.next().getElement(); }
    }

}
