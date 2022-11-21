package edu.uwm.cs351.util;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Consumer;

import junit.framework.TestCase;

/*
 * Andrew Le
 * Homework 10, CS 351
 */

public class TreeMap<K,V>  extends AbstractMap<K,V> {

	// Here is the data structure to use.
	
	private static class Node<K,V> extends DefaultEntry<K,V> {
		Node<K,V> left, right;
		Node<K,V> parent;
		Node(K k, V v) {
			super(k,v);
			parent = left = right = null;
		}
	}
	
	private Comparator<K> comparator;
	private Node<K,V> dummy;
	private int numItems = 0;
	private int version = 0;
	
	
	/// Invariant checks:
	
	private static Consumer<String> reporter = (s) -> { System.err.println("Invariant error: " + s); };
	
	private boolean report(String error) {
		reporter.accept(error);
		return false;
	}
	
	/**
	 * Return whether nodes in the subtree rooted at the given node have correct parent
	 * and have keys that are never null and are correctly sorted and are all in the range 
	 * between the lower and upper (both exclusive).
	 * If either bound is null, then that means that there is no limit at this side.
	 * The first problem is found will be reported.
	 * @param node root of subtree to examine
	 * @param p parent of subtree to examine
	 * @param lower value that all nodes must be greater than.  If null, then
	 * there is no lower bound.
	 * @param upper value that all nodes must be less than. If null,
	 * then there is no upper bound.
	 * @return whether the subtree is fine.  If false is 
	 * returned, there is a problem, which has already been reported.
	 */
	
	/*
	 * Have correct parents
	 * Keys that are never null
	 * Between the lower and upper bounds (exclusive)
	 */
	private boolean checkInRange(Node<K,V> node, Node<K, V> p, K lower, K upper) {
		if (node.parent != p) {
			return false;
		}
		if (node.key == null) {
			return false;
		}
		if (lower != null && comparator.compare(lower, node.key) >= 0) {
			return false;
		}
		if (upper != null && comparator.compare(upper, node.key) <= 0) {
			return false;
		}
			
		
		if (node.right != null) {
			return checkInRange(node.right, node, node.key, upper);
		}
		if (node.left != null) {
			return checkInRange(node.left, node, lower, node.key);
		}

		
		return true; // TODO
	}
	
	/**
	 * Return the number of nodes in a binary tree.
	 * @param r binary (search) tree, may be null but must not have cycles
	 * @return number of nodes in this tree
	 */
	private int countNodes(Node<K,V> r) {
		if (r == null) return 0;
		return 1 + countNodes(r.left) + countNodes(r.right);
	}
	
	
	/**
	 * Check the invariant, printing a message if not satisfied.
	 * @return whether invariant is correct
	 */
	private boolean wellFormed() {
		// TODO:
		// 1. check that comparator is not null
		// 2. check that dummy is not null
		// 3. check that dummy's key, right subtree and parent are null
		// 4. check that all (non-dummy) nodes are in range
		// 5. check that all nodes have correct parents
		// 6. check that number of items matches number of (non-dummy) nodes
		// "checkInRange" will help with 4,5
		
		//Invariant 1
		if (comparator == null) return report("comparator is null");
			
		//Invariant 2
		if (dummy == null) return report("dummy is null");
		
		//Invariant 3
		if (dummy.key != null || dummy.right != null || dummy.parent != null) return report("dummy is not correct");
		
		//Invariant 4 + 5
		if (dummy.left != null && !checkInRange(dummy.left, dummy, null, null)) return report("checkInRange not good");
		
		//Invariant 6
		if (countNodes(dummy.left) != numItems) return report("countNodes does not equal numItems");
		
		return true;
	}
	
	
	/// constructors
	
	private TreeMap(boolean ignored) { } // do not change this.
	
	public TreeMap() {
		this(null);
		assert wellFormed() : "invariant broken after constructor()";
	}
	
	@SuppressWarnings("unchecked") // for comparator creation
	public TreeMap(Comparator<K> c) {
		// TODO
		// Update the parameter comparator if necessary
		// Create the dummy node.
		if (c == null) {
			comparator = (a, b) -> ((Comparable<K>) a).compareTo(b);
		}
		else {
			comparator = c;
		}
		dummy = new Node<K, V>(null, null);
	
		assert wellFormed() : "invariant broken after constructor(Comparator)";
	}

	@SuppressWarnings("unchecked")
	private K asKey(Object x) {
		if (dummy.left == null || x == null) return null;
		try {
			comparator.compare(dummy.left.key,(K)x);
			comparator.compare((K)x,dummy.left.key);
			return (K)x;
		} catch (ClassCastException ex) {
			return null;
		}
	}

	/**
	 * Find the node for a given key.  Return null if the key isn't present
	 * in the tree.  This helper method assumes that the tree is well formed,
	 * but doesn't check that.
	 * @param o object treated as a key.
	 * @return node whose data is equal to o, 
	 * or null if no nodes in the tree have this property.
	 */
	private Node<K, V> findKey(Object o){
		K key = asKey(o);
		if (key == null) return null;
		
		Node<K, V> i = dummy.left;
		while (i != null) {
			int c = comparator.compare(key, asKey(i.key));
			if (c == 0) return i;
			if (c > 0) i = i.right;
			else {
				i = i.left;
			}
		}
		
		return i; // TODO (non-recursive is fine)
	}

	// TODO: many methods to override here:
	// size, containsKey(Object), get(Object), clear(), put(K, V), remove(Object)
	// make sure to use @Override and assert wellformedness
	// plus any private helper methods.
	// Our solution has getNode(Object)
	// Increase version if data structure if modified.
	
	@Override // implementation
	public int size() {
		return numItems;
	}
	
	@Override // implementation
	public boolean containsKey(Object o) {
		assert wellFormed() : "wellFormed failed at the start of containsKey";
		if (findKey(o) == null) return false;
		return true;
	}
	
	@Override // efficiency
	public V get(Object o){
		V i = null;
		if (findKey(o) != null) {
			i = findKey(o).getValue();
		}
		return i;
	}
	
	@Override // efficiency
	public void clear() {
		dummy.left = null;
		numItems = 0;
		version++;
	}
	
	private Node<K, V> getNode(Object o){
		return findKey(o);
	}
	
	private Node<K, V> firstInTree(Node<K, V> r){
		while (r.left != null) {
			r = r.left;
		}
		return r;
	}
	
	private Node<K, V> doPut(Node<K, V> r, Node<K, V> p, K k, V v) {
		if (r == null) { 
			r = new Node<K, V>(k, v);
			r.parent = p;
			return r;
		}
		
		if (comparator.compare(r.key, k) < 0) {
			r.right = doPut(r.right, r, k, v);
		}
		else {
			r.left = doPut(r.left, r, k, v);
		}
		
		return r;
	}
	
	
	@Override // implementation
	public V put(K k, V v) {
		assert wellFormed() : "wellFormed failed at start of put";
		if (k == null) throw new NullPointerException();
		V val = null;
		
		if (containsKey(k)) {
			val = findKey(k).getValue();
			findKey(k).setValue(v);
		}
		else {
			dummy.left = doPut(dummy.left, dummy, k, v);
			numItems++;
		}
		
		version++;
		
		assert wellFormed() : "wellFormed failed at end of put";
		return val;
	}
	
	private Node<K, V> doRemove(Node<K, V> r, Node<K, V> p, Node<K, V> target){
		if (r == target) {
			if (r.left == null) {
				if (r.right != null) {
					r.right.parent = p;
				}
				return r.right;
			}
			if (r.right == null) {
				if (r.left != null) {
					r.left.parent = p;
				}
				return r.left;
			}
			
			Node<K, V> t = firstInTree(r.right);
			t.right = doRemove(r.right, r, t);
			t.left = r.left;
			t.left.parent = t;
			t.parent = p;
			r = t;
		}
		else if (r != null){
			if (comparator.compare(target.key, r.key) < 0) {
				r.left = doRemove(r.left, r, target);
			}
			else {
				r.right = doRemove(r.right, r, target);
			}
		}

		return r;
	}
	
	@Override // implementation
	public V remove(Object o) {
		assert wellFormed() : "wellFormed failed in remove(main) start";
		if (findKey(o) == null) return null;
		
		Node<K, V> i = getNode(o);
		dummy.left = doRemove(dummy.left, dummy, i);
		numItems--;
		version++;
		
		assert wellFormed() : "wellFormed failed in remove(main) end";
		return i.getValue();
	}

	
	private volatile Set<Entry<K,V>> entrySet;
	
	@Override
	public Set<Entry<K, V>> entrySet() {
		assert wellFormed() : "invariant broken at beginning of entrySet";
		if (entrySet == null) {
			entrySet = new EntrySet();
		}
		return entrySet;
	}

	/**
	 * The set for this map, backed by the map.
	 * By "backed: we mean that this set doesn't have its own data structure:
	 * it uses the data structure of the map.
	 */
	private class EntrySet extends AbstractSet<Entry<K,V>> {
		// Do NOT add any fields! 
		
		@Override
		public int size() {
			// TODO: Easy: delegate to TreeMap.size()
			return TreeMap.this.size();
		}

		@Override
		public Iterator<Entry<K, V>> iterator() {
			return new MyIterator();
		}
		
		
		@Override
		public boolean contains(Object o) {
			assert wellFormed() : "Invariant broken at start of EntrySet.contains";
			// TODO if o is not an entry (instanceof Entry<?,?>), return false
			// Otherwise, check the entry for this entry's key.
			// If there is no such entry return false;
			// Otherwise return whether the entries match (use the equals method of the Node class). 
			// N.B. You can't check whether the key is of the correct type
			// because K is a generic type parameter.  So you must handle any
			// Object similarly to how "get" does.
			if (!(o instanceof Entry<?, ?>)) return false;
			
			Entry<?, ?> temp = (Entry<?, ?>) o;
			
			if (!TreeMap.this.containsKey(temp.getKey())) return false;
			
			if (!TreeMap.this.getNode(temp.getKey()).equals(o)) return false;
			
			return true;
		}

		@Override
		public boolean remove(Object x) {
			// TODO: if the tree doesn't contain x, return false
			// otherwise do a TreeMap remove.
			// make sure that the invariant is true before returning.
			assert wellFormed() : "wellFormed failed at the beginning of remove(EntrySet)";
			
			Entry<?, ?> temp = (Entry<?, ?>) x;
			if (!TreeMap.this.containsKey(temp.getKey())) return false;
			if (TreeMap.this.getNode(temp.getKey()).getValue() != temp.getValue()) return false;
			TreeMap.this.remove(temp.getKey());
			
			assert wellFormed() : "wellFormed failed at the beginning of remove(EntrySet)";
			return true;
		}
		
		@Override
		public void clear() {
			// TODO: Easy: delegate to the TreeMap.clear()
			TreeMap.this.clear();
		}
	}

	
	/**
	 * Iterator over the map.
	 * We use parent pointers.
	 * current points to node (if any) that can be removed.
	 * next points to dummy indicating no more next.
	 */
	private class MyIterator implements Iterator<Entry<K,V>> {
		
		Node<K, V> current, next;
		int colVersion = version;
		
		
		boolean wellFormed() {
			// TODO: See Homework description for more details.  Here's a summary:
			// (1) check the outer wellFormed()
			// (2) If version matches, do the remaining checks:
			//     (a) current should either be null or a non-dummy node in the tree
			//     (b) next should never be null and should be in the tree (maybe dummy).
			//     (c) if current is not null, make sure it is the last node before where next is.
			
			//Invariant 1
			if (!TreeMap.this.wellFormed()) return false;
			
			if (colVersion == version) {
				//Invariant 2
				if (current != null && TreeMap.this.findKey(current.key) == null) return report("current not in TreeMap");
				
				//Invariant 3
				if (next == null) {
					return report("next is null");
				}
				else {
					if (next != dummy && TreeMap.this.findKey(next.key) == null) {
						return report("next is not equal to dummy and not in TreeMap");
					}
				}
				//Invariant 4
				if (current != null) {
					Node<K, V> i = next;
					if (i.left != null) {
						i = i.left;
						while (i.right != null) {
							i = i.right;
						}
					}
					else {
						while (comparator.compare(i.parent.key, next.key) > 0){
							i = i.parent;
						}
						i = i.parent;
					}
					if (current != i) return report("current is not before next");
				}
				
			}
			
			return true;
		}
		
		MyIterator(boolean ignored) {} // do not change this
		
		MyIterator() {
			// TODO: initialize next to the leftmost node
			current = null;
			next = TreeMap.this.firstInTree(dummy);
			assert wellFormed() : "invariant broken after iterator constructor";
		}
		
		public void checkVersion() {
			if (version != colVersion) throw new ConcurrentModificationException("stale iterator");
		}
		
		public boolean hasNext() {
			assert wellFormed() : "invariant broken before hasNext()";
			// TODO: easy!
			return next != dummy;
		}

		public Entry<K, V> next() {
			assert wellFormed() : "invariant broken at start of next()";
			// TODO
			// We don't use (non-existent)nextInTree: 
			// but rather parent pointers in the second case.
			if (!hasNext()) throw new NoSuchElementException("no more");
			checkVersion();
			
			current = next;
			if (current.right != null) {
				next = firstInTree(current.right);
			}
			else {
				if (next.right == null) {
					Node<K, V> i = next;
					while (i.parent != null) {
						i = i.parent;
						if (i != dummy && comparator.compare(next.key, i.key) < 0) {
							break;
						}
					}
					next = i;
				}
				else {
					next = firstInTree(next.right);
				}
			}
			
			
			assert wellFormed() : "invariant broken at end of next()";
			return current;
		}

		public void remove() {
			assert wellFormed() : "invariant broken at start of iterator.remove()";
			// TODO: check that there is something to remove.
			// Use the remove method from TreeMap to remove it.
			// (See handout for details.)
			// After removal, record that there is nothing to remove any more.
			// Handle versions.
			if (current == null) throw new IllegalStateException();
			
			TreeMap.this.remove(current.key);
			
			current = next;
			colVersion++;
			
			assert wellFormed() : "invariant broken at end of iterator.remove()";
		}
		
	}
	
	
	/// Junit test case of private internal structure.
	// Do not change this nested class.
	
	public static class TestSuite extends TestCase {
		
		protected Consumer<String> getReporter() {
			return reporter;
		}
		
		protected void setReporter(Consumer<String> c) {
			reporter = c;
		}

		protected static class Node<K,V> extends TreeMap.Node<K, V> {
			public Node(K k, V v) {
				super(k,v);
			}
			
			public void setLeft(Node<K,V> n) {
				this.left = n;
			}
			
			public void setRight(Node<K,V> n) {
				this.right = n;
			}
			
			public void setParent(Node<K,V> n) {
				this.parent = n;
			}
		}
		
		protected class MyIterator extends TreeMap<Integer,String>.MyIterator {
			public MyIterator() {
				tree.super(false);
			}
			
			public void setCurrent(Node<Integer,String> c) {
				this.current = c;
			}
			public void setNext(Node<Integer,String> nc) {
				this.next = nc;
			}
			public void setColVersion(int cv) {
				this.colVersion = cv;
			}
			
			@Override // make visible
			public boolean wellFormed() {
				return super.wellFormed();
			}
		}
		
		protected TreeMap<Integer,String> tree;
		
		@Override // implementation
		protected void setUp() {
			tree = new TreeMap<>(false);
		}

		protected boolean wellFormed() {
			return tree.wellFormed();
		}
		
		protected void setDummy(Node<Integer,String> d) {
			tree.dummy = d;
		}
		
		protected void setNumItems(int ni) {
			tree.numItems = ni;
		}
		
		protected void setComparator(Comparator<Integer> c) {
			tree.comparator = c;
		}
		
		protected void setVersion(int v) {
			tree.version = v;
		}

		protected Node<Integer,String> findKey(Object key) {
			return (Node<Integer,String>)tree.findKey(key);
		}
	}
}
