import java.util.*;
import java.util.concurrent.atomic.*;

// Coarse Set is a collection of unique elements
// maintained as a linked list. The list of nodes
// are arranged in ascending order by their key,
// which is obtained using `hashCode()`. This
// facilitates the search of a item within the
// list. When the list is empty, it contains two
// sentinel nodes `head` and `tail` with minimum
// and maximum key values respectively. These
// sentinel nodes are not part of the set.
// 
// It uses a common, coarse-grained lock, for all
// method calls. This set performs well only when
// contention is low. If however, contention is
// high, despite the performance of lock, all
// methods calls will be essential sequential. The
// main advantage of this algorithms is that its
// obviously correct.

class CoarseSet<T> extends AbstractSet<T> {
  final AtomicInteger size;
  final Node<T> head;
  // lock: common (coarse) lock for set
  // size: number of items in set
  // head: points to begin of nodes in set

  public CoarseSet() {
    size = new AtomicInteger(0);
    head = new Node<>(null, Integer.MIN_VALUE);
    head.next = new Node<>(null, Integer.MAX_VALUE);
  }

  // 1. Create new node beforehand.
  // 2. Acquire lock before any action.
  // 3. Find node after which to insert.
  // 4. Add node, only if key is unique.
  // 5. Increment size if node was added.
  // 6. Release the lock.
  @Override
  public boolean add(T v) {
    Node<T> x = new Node<>(v);    // 1
    Node<T> p = findNode(x.key);  // 2
    boolean done = addNode(p, x); // 3
    if (done) size.incrementAndGet(); // 4
    unlockNode(p); // 5
    return done;
  }

  // 1. Acquire lock before any action.
  // 2. Find node after which to remove.
  // 3. Remove node, only if key matches.
  // 4. Decrement size if node was removed.
  // 5. Release the lock.
  @Override
  public boolean remove(Object v) {
    int k = v.hashCode();
    Node<T> p = findNode(k);          // 1
    boolean done = removeNode(p, k);  // 2
    if (done) size.decrementAndGet(); // 3
    unlockNode(p); // 4
    return done;
  }

  // 1. Acquire lock before any action.
  // 2. Find node previous to search key.
  // 3. Check if next node matches search key.
  // 4. Release the lock.
  @Override
  public boolean contains(Object v) {
    int k = v.hashCode();
    Node<T> p = findNode(k);       // 1
    boolean has = p.next.key == k; // 2
    unlockNode(p); // 3
    return has;
  }

  private boolean addNode(Node<T> p, Node<T> x) {
    Node<T> q = p.next;
    if (q.key == x.key) return false;
    x.next = q;
    p.next = x;
    return true;
  }

  private boolean removeNode(Node<T> p, int k) {
    Node<T> q = p.next;
    if (q.key != k) return false;
    p.next = q.next;
    return true;
  }

  private Node<T> findNode(int k) {
    Node<T> p = head;
    lockNode(p);
    while (p.next.key < k)
      p = nextNode(p);
    return p;
  }
  
  private void lockNode(Node<T> p) {
    p.lock();
    p.next.lock();
  }

  private void unlockNode(Node<T> p) {
    p.next.unlock();
    p.unlock();
  }

  private Node<T> nextNode(Node<T> p) {
    p.unlock();
    p = p.next;
    p.next.lock();
    return p;
  }

  @Override
  public Iterator<T> iterator() {
    Collection<T> a = new ArrayList<>();
    Node<T> p = head;
    lockNode(p);
    while (p.next.next != null) {
      a.add(p.next.value);
      p = nextNode(p);
    }
    unlockNode(p);
    return a.iterator();
  }

  @Override
  public int size() {
    return size.get();
  }
}
