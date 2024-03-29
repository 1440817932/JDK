/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package java.util;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Hash table based implementation of the <tt>Map</tt> interface.  This
 * implementation provides all of the optional map operations, and permits
 * <tt>null</tt> values and the <tt>null</tt> key.  (The <tt>HashMap</tt>
 * class is roughly equivalent to <tt>Hashtable</tt>, except that it is
 * unsynchronized and permits nulls.)  This class makes no guarantees as to
 * the order of the map; in particular, it does not guarantee that the order
 * will remain constant over time.
 *
 * <p>This implementation provides constant-time performance for the basic
 * operations (<tt>get</tt> and <tt>put</tt>), assuming the hash function
 * disperses the elements properly among the buckets.  Iteration over
 * collection views requires time proportional to the "capacity" of the
 * <tt>HashMap</tt> instance (the number of buckets) plus its size (the number
 * of key-value mappings).  Thus, it's very important not to set the initial
 * capacity too high (or the load factor too low) if iteration performance is
 * important.
 *
 * <p>An instance of <tt>HashMap</tt> has two parameters that affect its
 * performance: <i>initial capacity</i> and <i>load factor</i>.  The
 * <i>capacity</i> is the number of buckets in the hash table, and the initial
 * capacity is simply the capacity at the time the hash table is created.  The
 * <i>load factor</i> is a measure of how full the hash table is allowed to
 * get before its capacity is automatically increased.  When the number of
 * entries in the hash table exceeds the product of the load factor and the
 * current capacity, the hash table is <i>rehashed</i> (that is, internal data
 * structures are rebuilt) so that the hash table has approximately twice the
 * number of buckets.
 *
 * <p>As a general rule, the default load factor (.75) offers a good
 * tradeoff between time and space costs.  Higher values decrease the
 * space overhead but increase the lookup cost (reflected in most of
 * the operations of the <tt>HashMap</tt> class, including
 * <tt>get</tt> and <tt>put</tt>).  The expected number of entries in
 * the map and its load factor should be taken into account when
 * setting its initial capacity, so as to minimize the number of
 * rehash operations.  If the initial capacity is greater than the
 * maximum number of entries divided by the load factor, no rehash
 * operations will ever occur.
 *
 * <p>If many mappings are to be stored in a <tt>HashMap</tt>
 * instance, creating it with a sufficiently large capacity will allow
 * the mappings to be stored more efficiently than letting it perform
 * automatic rehashing as needed to grow the table.  Note that using
 * many keys with the same {@code hashCode()} is a sure way to slow
 * down performance of any hash table. To ameliorate impact, when keys
 * are {@link Comparable}, this class may use comparison order among
 * keys to help break ties.
 *
 * <p><strong>Note that this implementation is not synchronized.</strong>
 * If multiple threads access a hash map concurrently, and at least one of
 * the threads modifies the map structurally, it <i>must</i> be
 * synchronized externally.  (A structural modification is any operation
 * that adds or deletes one or more mappings; merely changing the value
 * associated with a key that an instance already contains is not a
 * structural modification.)  This is typically accomplished by
 * synchronizing on some object that naturally encapsulates the map.
 *
 * If no such object exists, the map should be "wrapped" using the
 * {@link Collections#synchronizedMap Collections.synchronizedMap}
 * method.  This is best done at creation time, to prevent accidental
 * unsynchronized access to the map:<pre>
 *   Map m = Collections.synchronizedMap(new HashMap(...));</pre>
 *
 * <p>The iterators returned by all of this class's "collection view methods"
 * are <i>fail-fast</i>: if the map is structurally modified at any time after
 * the iterator is created, in any way except through the iterator's own
 * <tt>remove</tt> method, the iterator will throw a
 * {@link ConcurrentModificationException}.  Thus, in the face of concurrent
 * modification, the iterator fails quickly and cleanly, rather than risking
 * arbitrary, non-deterministic behavior at an undetermined time in the
 * future.
 *
 * <p>Note that the fail-fast behavior of an iterator cannot be guaranteed
 * as it is, generally speaking, impossible to make any hard guarantees in the
 * presence of unsynchronized concurrent modification.  Fail-fast iterators
 * throw <tt>ConcurrentModificationException</tt> on a best-effort basis.
 * Therefore, it would be wrong to write a program that depended on this
 * exception for its correctness: <i>the fail-fast behavior of iterators
 * should be used only to detect bugs.</i>
 *
 * <p>This class is a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 *
 * @author  Doug Lea
 * @author  Josh Bloch
 * @author  Arthur van Hoff
 * @author  Neal Gafter
 * @see     Object#hashCode()
 * @see     Collection
 * @see     Map
 * @see     TreeMap
 * @see     Hashtable
 * @since   1.2
 */


/**
 * 为什么使用数组链表：
 * 数组：查找容易，通过index 快速定位；插入和删除困难，需要移动插入和删除位置之后的节点；
 * 链表：查找困难，需要从头节点或尾节点开始遍历，知道找到目标节点；插入和删除容易，只需要修改目标节点前后的prev和next属性即可。
 *
 *     首先通过index快速定位索引位置，利用了数组的优点；然后遍历链表找到节点，进行节点的新增/修改/删除操作，利用了链表的优点。
 *
 * 1.8改成数组链表红黑树：
 * 问题：定位索引位置后，需要先先遍历链表找到节点，如果链表很长的话（hash冲突严重的时候），会有查找性能问题。
 * 使用链表查找性能 O(n),使用红黑树是 O(log n)
 * @param <K>
 * @param <V>
 */
public class HashMap<K,V> extends AbstractMap<K,V>
    implements Map<K,V>, Cloneable, Serializable {

    private static final long serialVersionUID = 362498820763181265L;

    /*
     * Implementation notes.
     *
     * This map usually acts as a binned (bucketed) hash table, but
     * when bins get too large, they are transformed into bins of
     * TreeNodes, each structured similarly to those in
     * java.util.TreeMap. Most methods try to use normal bins, but
     * relay to TreeNode methods when applicable (simply by checking
     * instanceof a node).  Bins of TreeNodes may be traversed and
     * used like any others, but additionally support faster lookup
     * when overpopulated. However, since the vast majority of bins in
     * normal use are not overpopulated, checking for existence of
     * tree bins may be delayed in the course of table methods.
     *
     * Tree bins (i.e., bins whose elements are all TreeNodes) are
     * ordered primarily by hashCode, but in the case of ties, if two
     * elements are of the same "class C implements Comparable<C>",
     * type then their compareTo method is used for ordering. (We
     * conservatively check generic types via reflection to validate
     * this -- see method comparableClassFor).  The added complexity
     * of tree bins is worthwhile in providing worst-case O(log n)
     * operations when keys either have distinct hashes or are
     * orderable, Thus, performance degrades gracefully under
     * accidental or malicious usages in which hashCode() methods
     * return values that are poorly distributed, as well as those in
     * which many keys share a hashCode, so long as they are also
     * Comparable. (If neither of these apply, we may waste about a
     * factor of two in time and space compared to taking no
     * precautions. But the only known cases stem from poor user
     * programming practices that are already so slow that this makes
     * little difference.)
     *
     * Because TreeNodes are about twice the size of regular nodes, we
     * use them only when bins contain enough nodes to warrant use
     * (see TREEIFY_THRESHOLD). And when they become too small (due to
     * removal or resizing) they are converted back to plain bins.  In
     * usages with well-distributed user hashCodes, tree bins are
     * rarely used.  Ideally, under random hashCodes, the frequency of
     * nodes in bins follows a Poisson distribution
     * (http://en.wikipedia.org/wiki/Poisson_distribution) with a
     * parameter of about 0.5 on average for the default resizing
     * threshold of 0.75, although with a large variance because of
     * resizing granularity. Ignoring variance, the expected
     * occurrences of list size k are (exp(-0.5) * pow(0.5, k) /
     * factorial(k)). The first values are:
     *
     * 0:    0.60653066
     * 1:    0.30326533
     * 2:    0.07581633
     * 3:    0.01263606
     * 4:    0.00157952
     * 5:    0.00015795
     * 6:    0.00001316
     * 7:    0.00000094
     * 8:    0.00000006
     * more: less than 1 in ten million
     *
     * The root of a tree bin is normally its first node.  However,
     * sometimes (currently only upon Iterator.remove), the root might
     * be elsewhere, but can be recovered following parent links
     * (method TreeNode.root()).
     *
     * All applicable internal methods accept a hash code as an
     * argument (as normally supplied from a public method), allowing
     * them to call each other without recomputing user hashCodes.
     * Most internal methods also accept a "tab" argument, that is
     * normally the current table, but may be a new or old one when
     * resizing or converting.
     *
     * When bin lists are treeified, split, or untreeified, we keep
     * them in the same relative access/traversal order (i.e., field
     * Node.next) to better preserve locality, and to slightly
     * simplify handling of splits and traversals that invoke
     * iterator.remove. When using comparators on insertion, to keep a
     * total ordering (or as close as is required here) across
     * rebalancings, we compare classes and identityHashCodes as
     * tie-breakers.
     *
     * The use and transitions among plain vs tree modes is
     * complicated by the existence of subclass LinkedHashMap. See
     * below for hook methods defined to be invoked upon insertion,
     * removal and access that allow LinkedHashMap internals to
     * otherwise remain independent of these mechanics. (This also
     * requires that a map instance be passed to some utility methods
     * that may create new nodes.)
     *
     * The concurrent-programming-like SSA-based coding style helps
     * avoid aliasing errors amid all of the twisty pointer operations.
     */

    /**
     * The default initial capacity - MUST be a power of two.
     */
    // 默认初始容量
    // 容量必须是2的N次方，HashMap会根据我们传入的容量计算一个“大于等于该容量的2的N次方”
    // 例如传16，容量为16；传17容量为32
    // 为什么容量必须是2的N次方
    // 核心目的：实现节点均匀分布，减少hash冲突

    static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; // aka 16

    /**
     * The maximum capacity, used if a higher value is implicitly specified
     * by either of the constructors with arguments.
     * MUST be a power of two <= 1<<30.
     */
    // 最大容量
    static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * The load factor used when none specified in constructor.
     */
    // 加载因子
    static final float DEFAULT_LOAD_FACTOR = 0.75f;

    /**
     * The bin count threshold for using a tree rather than list for a
     * bin.  Bins are converted to trees when adding an element to a
     * bin with at least this many nodes. The value must be greater
     * than 2 and should be at least 8 to mesh with assumptions in
     * tree removal about conversion back to plain bins upon
     * shrinkage.
     */
    // 数组转树阈值
    //为什么是8？ 红黑树节点大小约为链表节点的2倍，在节点太少时，红黑树的查找性能优势并不明显，付出的代价不值得
    static final int TREEIFY_THRESHOLD = 8;

    /**
     * The bin count threshold for untreeifying a (split) bin during a
     * resize operation. Should be less than TREEIFY_THRESHOLD, and at
     * most 6 to mesh with shrinkage detection under removal.
     */
    // 树转数组阈值
    static final int UNTREEIFY_THRESHOLD = 6;

    /**
     * The smallest table capacity for which bins may be treeified.
     * (Otherwise the table is resized if too many nodes in a bin.)
     * Should be at least 4 * TREEIFY_THRESHOLD to avoid conflicts
     * between resizing and treeification thresholds.
     */
    // 数组转树需要数组长度大于等于64，则会触发链表节点转红黑树节点
    static final int MIN_TREEIFY_CAPACITY = 64;

    /**
     * Basic hash bin node, used for most entries.  (See below for
     * TreeNode subclass, and in LinkedHashMap for its Entry subclass.)
     */
    static class Node<K,V> implements Map.Entry<K,V> {
        final int hash;
        final K key;
        V value;
        Node<K,V> next;

        Node(int hash, K key, V value, Node<K,V> next) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
        }

        public final K getKey()        { return key; }
        public final V getValue()      { return value; }
        public final String toString() { return key + "=" + value; }

        public final int hashCode() {
            return Objects.hashCode(key) ^ Objects.hashCode(value);
        }

        public final V setValue(V newValue) {
            V oldValue = value;
            value = newValue;
            return oldValue;
        }

        public final boolean equals(Object o) {
            if (o == this)
                return true;
            if (o instanceof Map.Entry) {
                Map.Entry<?,?> e = (Map.Entry<?,?>)o;
                if (Objects.equals(key, e.getKey()) &&
                    Objects.equals(value, e.getValue()))
                    return true;
            }
            return false;
        }
    }

    /* ---------------- Static utilities -------------- */

    /**
     * Computes key.hashCode() and spreads (XORs) higher bits of hash
     * to lower.  Because the table uses power-of-two masking, sets of
     * hashes that vary only in bits above the current mask will
     * always collide. (Among known examples are sets of Float keys
     * holding consecutive whole numbers in small tables.)  So we
     * apply a transform that spreads the impact of higher bits
     * downward. There is a tradeoff between speed, utility, and
     * quality of bit-spreading. Because many common sets of hashes
     * are already reasonably distributed (so don't benefit from
     * spreading), and because we use trees to handle large sets of
     * collisions in bins, we just XOR some shifted bits in the
     * cheapest possible way to reduce systematic lossage, as well as
     * to incorporate impact of the highest bits that would otherwise
     * never be used in index calculations because of table bounds.
     */
    static final int hash(Object key) {
        int h;
        //  主要目的就是为了让hash值更加分散
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }

    /**
     * Returns x's Class if it is of the form "class C implements
     * Comparable<C>", else null.
     */
    //如果类C实现了Comparable ,即返回对象x的类C的Class。
    static Class<?> comparableClassFor(Object x) {
        if (x instanceof Comparable) {
            // 类实现Comparable 接口
            Class<?> c;
            Type[] ts, as;
            Type t;
            // 参数化类型,是Type的子接口
            ParameterizedType p;
            if ((c = x.getClass()) == String.class) { // bypass checks
                // 如果x是字符串对象，则返回x.getclass，因为String已经实现了Comparable<String>
                return c;
            }

            //如果存在实现的接口，将接口的数组传给ts（包括参数化类型）
            //getGenericInterfaces  getInterfaces的区别：一个返回Type[],一个返回Class[](已经被类型擦除了，so)
            // getGenericInterfaces（）：返回表示直接由该对象表示的类或接口实现的接口的类型。
            if ((ts = c.getGenericInterfaces()) != null) {
                for (int i = 0; i < ts.length; ++i) {
                    //我们要判定的是，x是否实现了Comparable<C>,所以，父接口若为Comparable<C>,则
                    //它应该是一个参数化类型，
                    //声明此类的接口为Comparable
                    //实际类型参数的数组不为空
                    if (((t = ts[i]) instanceof ParameterizedType) && //它应该是一个参数化类型
                            // getRawType():表示声明此类型的类或接口
                            ((p = (ParameterizedType) t).getRawType() == //声明此类的接口为Comparable
                                    Comparable.class) &&
                            //getActualTypeArguments()就是获取泛型参数的类型
                            (as = p.getActualTypeArguments()) != null && //实际类型参数的数组不为空
                            as.length == 1 && as[0] == c) // type arg is c  //并且长度为1,且为C
                        return c;
                }
            }
        }
        return null;
    }

    /**
     * Returns k.compareTo(x) if x matches kc (k's screened comparable
     * class), else 0.
     */
    @SuppressWarnings({"rawtypes","unchecked"}) // for cast to Comparable
    static int compareComparables(Class<?> kc, Object k, Object x) {
        return (x == null || x.getClass() != kc ? 0 :
                ((Comparable)k).compareTo(x));
    }

    /**
     * Returns a power of two size for the given target capacity.
     */
    // 大于等于该容量的最小的2的N次方计算
    // 这段代码的目的是把n的二进制数中的0置为1
    /*
    假设传进来一个数的二进制是100001001；
		n |= n >>> 1;
		在这里处理之后是110001101，这一步可以这样理解，
		如果你是1，那么把你的下一位变成1，那么是不是有1的地方会存在两个1，
		这也就解释了为什么下一行要无符号右移2，依此类推1，2，4，8，16，下下次直接右移4.
        n |= n >>> 2;
        在这里处理之后是111101111
        n |= n >>> 4;
        在这里处理之后是111111111
        n |= n >>> 8;
        n |= n >>> 16;
     */
    static final int tableSizeFor(int cap) {
        // 处理本身cap是2的N次方情况
        int n = cap - 1;
        // “ | ” 或等于  如：a| = b -> a = a | b
        // " >>> " 无符号右移 右移后左边空出的位用 0 补充， 移出右边的位被丢弃
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        // 5个公式通过最高位的1，拿到1个1、4个1、8个1、16个1、32个1 。然后返回的时候加1.会得到一个比n大的2的N次方。
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }

    /* ---------------- Fields -------------- */

    /**
     * The table, initialized on first use, and resized as
     * necessary. When allocated, length is always a power of two.
     * (We also tolerate length zero in some operations to allow
     * bootstrapping mechanics that are currently not needed.)
     */
    transient Node<K,V>[] table;

    /**
     * Holds cached entrySet(). Note that AbstractMap fields are used
     * for keySet() and values().
     */
    transient Set<Map.Entry<K,V>> entrySet;

    /**
     * The number of key-value mappings contained in this map.
     */
    transient int size;

    /**
     * The number of times this HashMap has been structurally modified
     * Structural modifications are those that change the number of mappings in
     * the HashMap or otherwise modify its internal structure (e.g.,
     * rehash).  This field is used to make iterators on Collection-views of
     * the HashMap fail-fast.  (See ConcurrentModificationException).
     */
    transient int modCount;

    /**
     * The next size value at which to resize (capacity * load factor).
     *
     * @serial
     */
    // (The javadoc description is true upon serialization.
    // Additionally, if the table array has not been allocated, this
    // field holds the initial array capacity, or zero signifying
    // DEFAULT_INITIAL_CAPACITY.)
    //
    // 扩容阈值：
    // 1）当HashMap的个数达到g该值，触发扩容
    // 2）初始化时的容量，在我们新建HashMap对象时，threshold 还会被用来存初始化时的容量。
    // 3）HashMap 直到我们第一次插入节点时，才会对table进行初始化，避免不必要的空间浪费。

    int threshold;

    /**
     * The load factor for the hash table.
     *
     * @serial
     */
    // 负载因子： 扩容阈值 = 容量 * 负载因子
    final float loadFactor;

    /* ---------------- Public operations -------------- */

    /**
     * Constructs an empty <tt>HashMap</tt> with the specified initial
     * capacity and load factor.
     *
     * @param  initialCapacity the initial capacity
     * @param  loadFactor      the load factor
     * @throws IllegalArgumentException if the initial capacity is negative
     *         or the load factor is nonpositive
     */
    public HashMap(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Illegal initial capacity: " +
                                               initialCapacity);
        // 初始化容量比最大容量大，使用默认的最大容量
        if (initialCapacity > MAXIMUM_CAPACITY)
            initialCapacity = MAXIMUM_CAPACITY;
        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new IllegalArgumentException("Illegal load factor: " +
                                               loadFactor);
        this.loadFactor = loadFactor;
        this.threshold = tableSizeFor(initialCapacity);
    }

    /**
     * Constructs an empty <tt>HashMap</tt> with the specified initial
     * capacity and the default load factor (0.75).
     *
     * @param  initialCapacity the initial capacity.
     * @throws IllegalArgumentException if the initial capacity is negative.
     */
    public HashMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Constructs an empty <tt>HashMap</tt> with the default initial capacity
     * (16) and the default load factor (0.75).
     */
    public HashMap() {
        this.loadFactor = DEFAULT_LOAD_FACTOR; // all other fields defaulted
    }

    /**
     * Constructs a new <tt>HashMap</tt> with the same mappings as the
     * specified <tt>Map</tt>.  The <tt>HashMap</tt> is created with
     * default load factor (0.75) and an initial capacity sufficient to
     * hold the mappings in the specified <tt>Map</tt>.
     *
     * @param   m the map whose mappings are to be placed in this map
     * @throws  NullPointerException if the specified map is null
     */
    public HashMap(Map<? extends K, ? extends V> m) {
        this.loadFactor = DEFAULT_LOAD_FACTOR;
        putMapEntries(m, false);
    }

    /**
     * Implements Map.putAll and Map constructor
     *
     * @param m the map
     * @param evict false when initially constructing this map, else
     * true (relayed to method afterNodeInsertion).
     */
    final void putMapEntries(Map<? extends K, ? extends V> m, boolean evict) {
        int s = m.size();
        if (s > 0) {
            if (table == null) { // pre-size
                // 如果未初始化，则计算HashMap的最小需要的容量（即容量刚好不大于扩容阈值）。这里Map的大小s就被当作HashMap的扩容阈值，然后用传入Map的大小除以负载因子就能得到对应的HashMap的容量大小（当前m的大小 / 负载因子 = HashMap容量）
                // 先不考虑容量必须为2的幂，那么下面括号里会算出来一个容量，使得size刚好不大于阈值。但这样会算出小数来，但作为容量就必须向上取整，所以这里要加1。此时ft可以临时看作HashMap容量大小
                float ft = ((float)s / loadFactor) + 1.0F;
                /*
                如果我们设置的默认值是7，经过Jdk处理之后，会被设置成8，但是，这个HashMap在元素个数达到 8*0.75 = 6的时候就会进行一次扩容，这明显是我们不希望见到的。
                如果我们通过expectedSize / 0.75F + 1.0F计算，7/0.75 + 1 = 10 ,10经过Jdk处理之后，会被设置成16，这就大大的减少了扩容的几率。
                 */
                int t = ((ft < (float)MAXIMUM_CAPACITY) ?
                         (int)ft : MAXIMUM_CAPACITY);
                if (t > threshold)
                    threshold = tableSizeFor(t);
            }
            else if (s > threshold)
                resize();
            for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
                K key = e.getKey();
                V value = e.getValue();
                putVal(hash(key), key, value, false, evict);
            }
        }
    }

    /**
     * Returns the number of key-value mappings in this map.
     *
     * @return the number of key-value mappings in this map
     */
    public int size() {
        return size;
    }

    /**
     * Returns <tt>true</tt> if this map contains no key-value mappings.
     *
     * @return <tt>true</tt> if this map contains no key-value mappings
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Returns the value to which the specified key is mapped,
     * or {@code null} if this map contains no mapping for the key.
     *
     * <p>More formally, if this map contains a mapping from a key
     * {@code k} to a value {@code v} such that {@code (key==null ? k==null :
     * key.equals(k))}, then this method returns {@code v}; otherwise
     * it returns {@code null}.  (There can be at most one such mapping.)
     *
     * <p>A return value of {@code null} does not <i>necessarily</i>
     * indicate that the map contains no mapping for the key; it's also
     * possible that the map explicitly maps the key to {@code null}.
     * The {@link #containsKey containsKey} operation may be used to
     * distinguish these two cases.
     *
     * @see #put(Object, Object)
     */
    public V get(Object key) {
        Node<K,V> e;
        return (e = getNode(hash(key), key)) == null ? null : e.value;
    }

    /**
     * Implements Map.get and related methods
     *
     * @param hash hash for key
     * @param key the key
     * @return the node, or null if none
     */
    final Node<K,V> getNode(int hash, Object key) {
        //tab：引用当前hashMap的散列表
        //first：桶位中的头元素
        //e：临时node元素
        //n：table数组长度
        Node<K,V>[] tab; Node<K,V> first, e; int n; K k;
        if ((tab = table) != null && (n = tab.length) > 0 &&
            (first = tab[(n - 1) & hash]) != null) {
            //第一种情况：定位出来的桶位元素 即为咱们要get的数据
            if (first.hash == hash && // always check first node
                ((k = first.key) == key || (key != null && key.equals(k))))
                return first;
            //说明当前桶位不止一个元素，可能 是链表 也可能是 红黑树
            if ((e = first.next) != null) {
                if (first instanceof TreeNode)
                    return ((TreeNode<K,V>)first).getTreeNode(hash, key);
                do {
                    if (e.hash == hash &&
                        ((k = e.key) == key || (key != null && key.equals(k))))
                        return e;
                } while ((e = e.next) != null);
            }
        }
        return null;
    }

    /**
     * Returns <tt>true</tt> if this map contains a mapping for the
     * specified key.
     *
     * @param   key   The key whose presence in this map is to be tested
     * @return <tt>true</tt> if this map contains a mapping for the specified
     * key.
     */
    public boolean containsKey(Object key) {
        return getNode(hash(key), key) != null;
    }

    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for the key, the old
     * value is replaced.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with <tt>key</tt>, or
     *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
     *         (A <tt>null</tt> return can also indicate that the map
     *         previously associated <tt>null</tt> with <tt>key</tt>.)
     */
    public V put(K key, V value) {
        return putVal(hash(key), key, value, false, true);
    }

    /**
     * Implements Map.put and related methods
     *
     * @param hash hash for key
     * @param key the key
     * @param value the value to put
     * @param onlyIfAbsent if true, don't change existing value
     * @param evict if false, the table is in creation mode.
     * @return previous value, or null if none
     */
    final V putVal(int hash, K key, V value, boolean onlyIfAbsent,
                   boolean evict) {
        //tab：引用当前hashMap的散列表
        // p: 存放新key的值的Node 节点（可能为空，可能有值）
        // n：表示散列表数组的长度
        // i：表示路由寻址 结果 key的hash值（键值）
        Node<K,V>[] tab; Node<K,V> p; int n, i;
        // 先判断数组是否为空
        // 如果为空初始化数组
        //延迟初始化逻辑，第一次调用putVal时会初始化hashMap对象中的最耗费内存的散列表
        if ((tab = table) == null || (n = tab.length) == 0)
            n = (tab = resize()).length;
        // 不为空计算索引位置
        // 如果索引位置是否有节点

        //
        //　　运算符 % ：模运算，
        //
        //　　　　　　（1）、当运算符左边小于右边，结果就等于左边；
        //
        //　　　　　　（2）、当运算符左边大于右边，就和算术中的取余是一样的效果。

        // hash % n == hash & (n-1)
        //解释：
        //n表示哈希桶的长度，就是hashmap这个实例的容量，刚才说了会是2^n，不设置默认是16，
        // 那为什么要减1，我们知道16的二进制是10000，减1后就变成1111，这样我们有了一个二进制全部为1的数后就可以和hash值进行&运算。
        //**&：按位与，都为1才是1**

        //// 假设我的哈希桶的长度是16
        //// n - 1 即为 15
        //// 二进制表示为 1111
        //// 假设 hash值 是 111101001001101010101010111011
        //// 即
        //111101001001101010101010111011 // hash
        //000000000000000000000000001111 // n-1
        //000000000000000000000000001011 // 结果为 1011 即 11

        // 一个数模上一个偶数（n），结果肯定是小于这个偶数的值，即在[0, n)范围内；数组下标从零开始，妙啊。
        // 采用二进制位操作 &，相对于%能够提高运算效率
        if ((p = tab[i = (n - 1) & hash]) == null)
            // 索引位置没有节点，直接新建节点放到索引位置
            //最简单的一种情况：寻址找到的桶位 刚好是 null，这个时候，直接将当前k-v=>node 扔进去就可以了
            tab[i] = newNode(hash, key, value, null);
            // 索引位置有节点
        else {
            // e：不为null的话，找到了一个与当前要插入的key-value一致的key的元素
            // k：表示临时的一个key
            Node<K,V> e; K k;
            // 判断键的hash值是否跟节点相等，先等在比较键值，都相等则替换旧值，返回旧值
            if (p.hash == hash &&
                ((k = p.key) == key || (key != null && key.equals(k))))
                e = p;
            // 插头节点和插入的key不相等，且头节点为红黑树 进入putTreeVal方法
            else if (p instanceof TreeNode)
                e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value);
            else {
                // 判断键的hash值是否跟节点不相等节点不是红黑树
                for (int binCount = 0; ; ++binCount) {
                    if ((e = p.next) == null) {
                        p.next = newNode(hash, key, value, null);
                        // binCount从零开始，所以 减一
                        // 链表长度等于8转树
                        if (binCount >= TREEIFY_THRESHOLD - 1) // -1 for 1st
                            // 链表转红黑树 数组大小大于64转树，否则扩容
                            treeifyBin(tab, hash);
                        break;
                    }
                    //条件成立的话，说明找到了相同key的node元素，中断循环，需要进行替换操作
                    if (e.hash == hash &&
                        ((k = e.key) == key || (key != null && key.equals(k))))
                        break;
                    p = e;
                }
            }
            if (e != null) { // existing mapping for key
                V oldValue = e.value;
                // onlyIfAbsent ：用来判断没有就插入
                if (!onlyIfAbsent || oldValue == null)
                    e.value = value;
                afterNodeAccess(e);
                return oldValue;
            }
        }
        //modCount：表示散列表结构被修改的次数，替换Node元素的value不计数
        ++modCount;
        //插入新元素，size自增，如果自增后的值大于扩容阈值，则触发扩容。
        if (++size > threshold)
            resize();
        afterNodeInsertion(evict);
        return null;
    }

    /**
     * Initializes or doubles table size.  If null, allocates in
     * accord with initial capacity target held in field threshold.
     * Otherwise, because we are using power-of-two expansion, the
     * elements from each bin must either stay at same index, or move
     * with a power of two offset in the new table.
     *
     * @return the table
     */

    // TODO: 2022/11/11 HashMap扩容时的resize()方法中(e.hash & oldCap) == 0算法推导

    /**
     * HashMap在扩容时，需要先创建一个新数组,然后再将旧数组中的数据转移到新数组上来
     * 此时，旧数组上的数据就会根据(e.hash & oldCap) 是否等于0这个算法,被很巧妙地分为2类:
     * ① 等于0时，则将该头节点放到新数组时的索引位置等于其在旧数组时的索引位置,记为低位区链表lo开头-low;
     * ② 不等于0时,则将该头节点放到新数组时的索引位置等于其在旧数组时的索引位置再加上旧数组长度，记为高位区链表hi开头-high.
     * 具体,详见下述的算法推导解析:
     */
    /**
     * 算法：
     * (e.hash & oldCap) = 0
     * 前提：
     *  e.hash代表的是旧数组中节点或元素或数据e的hash值，该hash值是根据key确定过的：(key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16) ；
     *  oldCap为旧数组的数组长度，是2的n次幂的整数。即 e.hash & 2^n=0
     *  如： e.hash   1 0 1 0 1
     *      2 ^ n   1 0 0 0 0 0
     -------------------------------
     e.hash & 2^n   0 0 0 0 0 0  = 0
     */

     /** 推导过程1(e.hash & oldCap)=0：
     *
     * 1) 因为oldCap是2的n次幂的整数,其二进制表达为1个1后面跟n个0：1000…0，
     * 若想要e.hash & oldCap的结果为0，则e.hash的二进制形式中与对应oldCap的二进制的1的位置一定为0，其他位置的可以随意，这样即可保证结果为0；
     * 2) 假设：
     * oldCap= 2 ^ 3 =8 = 1000
     * 则e.hash可以是     0101
     * ------------------------------
     * e.hash & oldCap   0000=0
     *
     * 3) (2oldCap -1)=2 ^ 4-1=01111，其二进制位数比oldCap多一位，但多的这一位是0,其余都是1(其低三位肯定也是1);
      * (oldCap-1)=2 ^ 3-1=0111，
     * 其二进制位数与oldCap相同,且其低3位的值都是1。故(2oldCap-1)和(oldCap -1)两者与只有4位且首位为0的e.hash=0101计算时，
     * 其实只有低3位真正能影响计算结果，而两者的低3位相同，都是111
     *
     * 4) 故在前提条件下，(2oldCap-1)和(oldCap -1)两者与e.hash进行&运算之后的结果一样：
     * (2oldCap -1)=2 ^ 4-1= 01111        |       (oldCap-1)=2 ^ 3-1= 0111
     *                e.hash  0101        |                    e.hash 0101
     *   -------------------------------------------------------------------
     *(2oldCap -1) & e.hash  00101 = 5  |   (oldCap -1) & e.hash    0101 = 5
     *
     * 5)  而(oldCap -1) &e.hash恰巧代表的就是e元素在旧数组中的索引位置；
     * 而(2oldCap -1) &e.hash则代表的就是e元素在旧数组长度扩容2倍后的新数组里的索引位置
     *
     *  综上，可得出满足e.hash&oldCap=0的元素，其在新旧数组中的索引位置不变；
     */

     /** 推导过程2  (e.hash & oldCap)不等于0：
     *
     * 1) 因为oldCap是2的n次幂的整数,其二进制表达为1个1后面跟n个0：1000…0，若想要e.hash&oldCap的结果不为0，
     * 则e.hash的二进制形式中与对应oldCap的二进制的1的位置一定不为0，其他位置的可以随意，这样即可保证结果不为0；
     * 2) 假设：
     * oldCap= 2 ^ 3 =8 = 1000
      *    则e.hash可以是 1101
      *     ----------------------
      *    e.hash&oldCap 1000=13
      *
      * 3)  (2oldCap -1)=2 ^ 4-1=01111,其二进制位数比oldCap多一位,但多的这一位是0,
      * 其余都是1(其低三位肯定也是1,其从左到右数的第4位为1);
      * (oldCap-1)=2 ^ 3-1=0111,其二进制位数与oldCap相同,且其低3位的值都是1,
      * 其从左到右数的第4位为0,。故(2oldCap-1)和(oldCap -1)两者与只有4位且首位为1的e.hash=1101计算时,
      * 其实也只有从左到右数的第4位(0)真正能影响计算结果,因为低3位完全一样都是1;
      *
      * 4) 故在前提条件下，(2oldCap-1)和(oldCap -1)两者与e.hash进行&运算后结果相差了oldCap：
      *    (2oldCap -1)=2^4-1= 01111    ( oldCap - 1 ) =2 ^ 3-1= 0111
      *                  e.hash 1101                      e.hash 1101
      * ---------------------------------------------------------------------------------------
      *(2oldCap -1) & e.hash   01101=8+5    (oldCap -1) & e.hash 0101=5
      *
      * 5)而(oldCap -1) &e.hash恰巧代表的就是e元素在旧数组中的索引位置；
      *   而(2oldCap -1) &e.hash则代表的就是e元素在旧数组长度扩容2倍后的新数组里的索引位置
      *
      * 6) 综上，可得出满足e.hash & oldCap不等于0的元素，其在新数组中的索引位置是其在旧数组中索引位置的基础上再加上旧数组长度个偏移量。
      *
      *
      */
     /*
      *为什么需要扩容？为了解决哈希冲突导致的链化影响查询效率的问题，扩容会缓解该问题。
      */
    final Node<K,V>[] resize() {// 调整
        //oldTab：引用扩容前的哈希表
        Node<K,V>[] oldTab = table;
        // 原本数组大小
        int oldCap = (oldTab == null) ? 0 : oldTab.length;
        // 表示扩容之前的扩容阈值，触发本次扩容的阈值
        int oldThr = threshold;
        /**
         * 先算出新的 扩容阈值 和 容量
         */
        int newCap, newThr = 0;
        // 条件如果成立说明 hashMap 中的散列表已经初始化过了，这是一次正常扩容
        if (oldCap > 0) {
            //扩容之前的table数组大小已经达到 最大阈值后，则不扩容，且设置扩容条件为 int 最大值
            if (oldCap >= MAXIMUM_CAPACITY) {
                threshold = Integer.MAX_VALUE;
                return oldTab;
            }
            // 注意： 新的容量 = 旧容量 * 2  即扩容大小为原来的两倍
            // 如果还是小于阈值最大值且旧的容量大于默认阈值。新的扩容阈值为旧的2倍。（newCap 小于数组最大值限制 且 扩容之前的阈值 >= 16）
            else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY &&
                     oldCap >= DEFAULT_INITIAL_CAPACITY)
                newThr = oldThr << 1; // double threshold
        }
        // oldCap = 0 ,容量变为旧的阈值
        else if (oldThr > 0) // initial capacity was placed in threshold
            newCap = oldThr;
        // oldCap == 0，oldThr == 0
        else {               // zero initial threshold signifies using defaults 零初始阈值表示使用默认值
            newCap = DEFAULT_INITIAL_CAPACITY; //16
            newThr = (int)(DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY); //12
        }
        // 如果新的阈值为零，即老的容量乘2大于int最大值或旧的容量还没到默认的容量大小，需要重新计算新的扩容阈值
        //newThr为零时，通过newCap和loadFactor计算出一个newThr
        if (newThr == 0) {
            float ft = (float)newCap * loadFactor;
            newThr = (newCap < MAXIMUM_CAPACITY && ft < (float)MAXIMUM_CAPACITY ?
                      (int)ft : Integer.MAX_VALUE);
        }
        threshold = newThr;
        @SuppressWarnings({"rawtypes","unchecked"}) //屏蔽一些无关紧要的警告
            Node<K,V>[] newTab = (Node<K,V>[])new Node[newCap];
        table = newTab;
        // 扩容前table 不为空
        if (oldTab != null) {
            // 遍历旧数组
            for (int j = 0; j < oldCap; ++j) {
                Node<K,V> e;
                // 获取数组的Node节点
                //说明当前桶位中有数据，但是数据具体是 单个数据，还是链表 还是 红黑树 并不知道
                if ((e = oldTab[j]) != null) {
                    // 2022/10/31 旧值置为空 为什么？ 重新计算在数组位置 -------->方便JVM GC时回收内存
                    oldTab[j] = null;
                    if (e.next == null)
                        // 第一种情况：如果数组只有一个节点，直接计算新数组的插入位置
                        newTab[e.hash & (newCap - 1)] = e;
                    else if (e instanceof TreeNode)
                        // 如果是树结构
                    /**
                     * 1）在新数组重新计算hash找到节点在新数组位置
                     * 2） 在新的数组位置计算是否需要转树
                     *  2.1）不需要：直接插入链表
                     *  2.2）需要直接
                     *      2.2.1）遍历当前点的插入（当前节点是已经计算好在新数组位置）
                     *          2.3.1）获取插入位置
                     *          2.3.2) 遍历根节点
                     *              1）找到当前节点插入的合适位置
                     *              2）平衡调整
                     */

                    // 第二种情况：是树结构

                        ((TreeNode<K,V>)e).split(this, newTab, j, oldCap);
                    else { // preserve order
                        //第三种情况：桶位已经形成链表
                        Node<K,V> loHead = null, loTail = null;
                        Node<K,V> hiHead = null, hiTail = null;
                        Node<K,V> next;
                        do {
                            // 循环条件
                            next = e.next;
                            // 尾插法
                            // e在新旧数组中的索引位置不变
                            if ((e.hash & oldCap) == 0) {
                                // 连成链
                                if (loTail == null)
                                    loHead = e;
                                else
                                    loTail.next = e;
                                // 赋值尾
                                loTail = e;
                            }
                            else {
                                if (hiTail == null)
                                    hiHead = e;
                                else
                                    hiTail.next = e;
                                hiTail = e;
                            }
                        } while ((e = next) != null);
                        if (loTail != null) {
                            loTail.next = null;
                            newTab[j] = loHead;
                        }
                        if (hiTail != null) {
                            hiTail.next = null;
                            newTab[j + oldCap] = hiHead;
                        }
                    }
                }
            }
        }
        return newTab;
    }

    /**
     * Replaces all linked nodes in bin at index for given hash unless
     * table is too small, in which case resizes instead.
     */
    final void treeifyBin(Node<K,V>[] tab, int hash) {
        int n, index; Node<K,V> e;
        // 数组转树需要数组长度大于等于64，则会触发链表节点转红黑树节点否则继续增加链表节点
        if (tab == null || (n = tab.length) < MIN_TREEIFY_CAPACITY)
            resize();
        else if ((e = tab[index = (n - 1) & hash]) != null) {
            TreeNode<K,V> hd = null, tl = null;
            do {
                TreeNode<K,V> p = replacementTreeNode(e, null);
                if (tl == null)
                    hd = p;
                else {
                    p.prev = tl;
                    tl.next = p;
                }
                tl = p;
            } while ((e = e.next) != null);
            if ((tab[index] = hd) != null)
                hd.treeify(tab);
        }
    }

    /**
     * Copies all of the mappings from the specified map to this map.
     * These mappings will replace any mappings that this map had for
     * any of the keys currently in the specified map.
     *
     * @param m mappings to be stored in this map
     * @throws NullPointerException if the specified map is null
     */
    public void putAll(Map<? extends K, ? extends V> m) {
        putMapEntries(m, true);
    }

    /**
     * Removes the mapping for the specified key from this map if present.
     *
     * @param  key key whose mapping is to be removed from the map
     * @return the previous value associated with <tt>key</tt>, or
     *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
     *         (A <tt>null</tt> return can also indicate that the map
     *         previously associated <tt>null</tt> with <tt>key</tt>.)
     */
    public V remove(Object key) {
        Node<K,V> e;
        return (e = removeNode(hash(key), key, null, false, true)) == null ?
            null : e.value;
    }

    /**
     * Implements Map.remove and related methods
     *
     * @param hash hash for key
     * @param key the key
     * @param value the value to match if matchValue, else ignored
     * @param matchValue if true only remove if value is equal
     * @param movable if false do not move other nodes while removing
     * @return the node, or null if none
     */
    final Node<K,V> removeNode(int hash, Object key, Object value,
                               boolean matchValue, boolean movable) {
        //tab：引用当前hashMap中的散列表
        //p：当前node元素
        //n：表示散列表数组长度
        //index：表示寻址结果
        Node<K,V>[] tab; Node<K,V> p; int n, index;
        //p:要删除节点坐在数组位置，先找到删除节点，然后赋值给node
        if ((tab = table) != null && (n = tab.length) > 0 &&
            (p = tab[index = (n - 1) & hash]) != null) {
            // node要删除的节点
            //e：当前Node的下一个元素
            Node<K,V> node = null, e; K k; V v;
            // 找到hash和key的hash相等的key
            if (p.hash == hash &&
                ((k = p.key) == key || (key != null && key.equals(k))))
                // 第一个就是删除元素
                node = p;
            else if ((e = p.next) != null) {//删除节点不是数组第一个 可能是链表 或 红黑色
                if (p instanceof TreeNode)
                    // 获取树类型的节点
                    node = ((TreeNode<K,V>)p).getTreeNode(hash, key);
                else {
                    // 不是树节点，则遍历链表获取节点
                    do {
                        if (e.hash == hash &&
                            ((k = e.key) == key ||
                             (key != null && key.equals(k)))) {
                            node = e;
                            break;
                        }
                        // 将节点的下一个赋值给p
                        p = e;
                    } while ((e = e.next) != null);
                }
            }
            //判断node不为空的话，说明按照key查找到需要删除的数据了
            if (node != null && (!matchValue || (v = node.value) == value ||
                                 (value != null && value.equals(v)))) {
                if (node instanceof TreeNode)
                    //第一种情况：node是树节点，说明需要进行树节点移除操作

                    ((TreeNode<K,V>)node).removeTreeNode(this, tab, movable);
                else if (node == p)
                    //第二种情况：桶位元素即为查找结果，则将该元素的下一个元素放至桶位中

                    tab[index] = node.next;
                else
                    //第三种情况：将当前node元素的下一个元素 设置成 要删除元素的 下一个元素。
                    // 因为node是删除节点，p是删除节点的上一个，所以删除node，需要将删除节点的下一个跟p的next相连
                    p.next = node.next;
                ++modCount;
                --size;
                afterNodeRemoval(node);
                return node;
            }
        }
        return null;
    }

    /**
     * Removes all of the mappings from this map.
     * The map will be empty after this call returns.
     */
    public void clear() {
        Node<K,V>[] tab;
        modCount++;
        if ((tab = table) != null && size > 0) {
            size = 0;
            for (int i = 0; i < tab.length; ++i)
                tab[i] = null;
        }
    }

    /**
     * Returns <tt>true</tt> if this map maps one or more keys to the
     * specified value.
     *
     * @param value value whose presence in this map is to be tested
     * @return <tt>true</tt> if this map maps one or more keys to the
     *         specified value
     */
    public boolean containsValue(Object value) {
        Node<K,V>[] tab; V v;
        if ((tab = table) != null && size > 0) {
            for (int i = 0; i < tab.length; ++i) {
                for (Node<K,V> e = tab[i]; e != null; e = e.next) {
                    if ((v = e.value) == value ||
                        (value != null && value.equals(v)))
                        return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns a {@link Set} view of the keys contained in this map.
     * The set is backed by the map, so changes to the map are
     * reflected in the set, and vice-versa.  If the map is modified
     * while an iteration over the set is in progress (except through
     * the iterator's own <tt>remove</tt> operation), the results of
     * the iteration are undefined.  The set supports element removal,
     * which removes the corresponding mapping from the map, via the
     * <tt>Iterator.remove</tt>, <tt>Set.remove</tt>,
     * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt>
     * operations.  It does not support the <tt>add</tt> or <tt>addAll</tt>
     * operations.
     *
     * @return a set view of the keys contained in this map
     */
    public Set<K> keySet() {
        Set<K> ks;
        return (ks = keySet) == null ? (keySet = new KeySet()) : ks;
    }

    final class KeySet extends AbstractSet<K> {
        public final int size()                 { return size; }
        public final void clear()               { HashMap.this.clear(); }
        public final Iterator<K> iterator()     { return new KeyIterator(); }
        public final boolean contains(Object o) { return containsKey(o); }
        public final boolean remove(Object key) {
            return removeNode(hash(key), key, null, false, true) != null;
        }
        public final Spliterator<K> spliterator() {
            return new KeySpliterator<>(HashMap.this, 0, -1, 0, 0);
        }
        public final void forEach(Consumer<? super K> action) {
            Node<K,V>[] tab;
            if (action == null)
                throw new NullPointerException();
            if (size > 0 && (tab = table) != null) {
                int mc = modCount;
                for (int i = 0; i < tab.length; ++i) {
                    for (Node<K,V> e = tab[i]; e != null; e = e.next)
                        action.accept(e.key);
                }
                if (modCount != mc)
                    throw new ConcurrentModificationException();
            }
        }
    }

    /**
     * Returns a {@link Collection} view of the values contained in this map.
     * The collection is backed by the map, so changes to the map are
     * reflected in the collection, and vice-versa.  If the map is
     * modified while an iteration over the collection is in progress
     * (except through the iterator's own <tt>remove</tt> operation),
     * the results of the iteration are undefined.  The collection
     * supports element removal, which removes the corresponding
     * mapping from the map, via the <tt>Iterator.remove</tt>,
     * <tt>Collection.remove</tt>, <tt>removeAll</tt>,
     * <tt>retainAll</tt> and <tt>clear</tt> operations.  It does not
     * support the <tt>add</tt> or <tt>addAll</tt> operations.
     *
     * @return a view of the values contained in this map
     */
    public Collection<V> values() {
        Collection<V> vs;
        return (vs = values) == null ? (values = new Values()) : vs;
    }

    final class Values extends AbstractCollection<V> {
        public final int size()                 { return size; }
        public final void clear()               { HashMap.this.clear(); }
        public final Iterator<V> iterator()     { return new ValueIterator(); }
        public final boolean contains(Object o) { return containsValue(o); }
        public final Spliterator<V> spliterator() {
            return new ValueSpliterator<>(HashMap.this, 0, -1, 0, 0);
        }
        public final void forEach(Consumer<? super V> action) {
            Node<K,V>[] tab;
            if (action == null)
                throw new NullPointerException();
            if (size > 0 && (tab = table) != null) {
                int mc = modCount;
                for (int i = 0; i < tab.length; ++i) {
                    for (Node<K,V> e = tab[i]; e != null; e = e.next)
                        action.accept(e.value);
                }
                if (modCount != mc)
                    throw new ConcurrentModificationException();
            }
        }
    }

    /**
     * Returns a {@link Set} view of the mappings contained in this map.
     * The set is backed by the map, so changes to the map are
     * reflected in the set, and vice-versa.  If the map is modified
     * while an iteration over the set is in progress (except through
     * the iterator's own <tt>remove</tt> operation, or through the
     * <tt>setValue</tt> operation on a map entry returned by the
     * iterator) the results of the iteration are undefined.  The set
     * supports element removal, which removes the corresponding
     * mapping from the map, via the <tt>Iterator.remove</tt>,
     * <tt>Set.remove</tt>, <tt>removeAll</tt>, <tt>retainAll</tt> and
     * <tt>clear</tt> operations.  It does not support the
     * <tt>add</tt> or <tt>addAll</tt> operations.
     *
     * @return a set view of the mappings contained in this map
     */
    public Set<Map.Entry<K,V>> entrySet() {
        Set<Map.Entry<K,V>> es;
        return (es = entrySet) == null ? (entrySet = new EntrySet()) : es;
    }

    final class EntrySet extends AbstractSet<Map.Entry<K,V>> {
        public final int size()                 { return size; }
        public final void clear()               { HashMap.this.clear(); }
        public final Iterator<Map.Entry<K,V>> iterator() {
            return new EntryIterator();
        }
        public final boolean contains(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<?,?> e = (Map.Entry<?,?>) o;
            Object key = e.getKey();
            Node<K,V> candidate = getNode(hash(key), key);
            return candidate != null && candidate.equals(e);
        }
        public final boolean remove(Object o) {
            if (o instanceof Map.Entry) {
                Map.Entry<?,?> e = (Map.Entry<?,?>) o;
                Object key = e.getKey();
                Object value = e.getValue();
                return removeNode(hash(key), key, value, true, true) != null;
            }
            return false;
        }
        public final Spliterator<Map.Entry<K,V>> spliterator() {
            return new EntrySpliterator<>(HashMap.this, 0, -1, 0, 0);
        }
        public final void forEach(Consumer<? super Map.Entry<K,V>> action) {
            Node<K,V>[] tab;
            if (action == null)
                throw new NullPointerException();
            if (size > 0 && (tab = table) != null) {
                int mc = modCount;
                for (int i = 0; i < tab.length; ++i) {
                    for (Node<K,V> e = tab[i]; e != null; e = e.next)
                        action.accept(e);
                }
                if (modCount != mc)
                    throw new ConcurrentModificationException();
            }
        }
    }

    // Overrides of JDK8 Map extension methods

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        Node<K,V> e;
        return (e = getNode(hash(key), key)) == null ? defaultValue : e.value;
    }

    @Override
    public V putIfAbsent(K key, V value) {
        return putVal(hash(key), key, value, true, true);
    }

    @Override
    public boolean remove(Object key, Object value) {
        return removeNode(hash(key), key, value, true, true) != null;
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        Node<K,V> e; V v;
        if ((e = getNode(hash(key), key)) != null &&
            ((v = e.value) == oldValue || (v != null && v.equals(oldValue)))) {
            e.value = newValue;
            afterNodeAccess(e);
            return true;
        }
        return false;
    }

    @Override
    public V replace(K key, V value) {
        Node<K,V> e;
        if ((e = getNode(hash(key), key)) != null) {
            V oldValue = e.value;
            e.value = value;
            afterNodeAccess(e);
            return oldValue;
        }
        return null;
    }

    @Override
    public V computeIfAbsent(K key,
                             Function<? super K, ? extends V> mappingFunction) {
        if (mappingFunction == null)
            throw new NullPointerException();
        int hash = hash(key);
        Node<K,V>[] tab; Node<K,V> first; int n, i;
        int binCount = 0;
        TreeNode<K,V> t = null;
        Node<K,V> old = null;
        if (size > threshold || (tab = table) == null ||
            (n = tab.length) == 0)
            n = (tab = resize()).length;
        if ((first = tab[i = (n - 1) & hash]) != null) {
            if (first instanceof TreeNode)
                old = (t = (TreeNode<K,V>)first).getTreeNode(hash, key);
            else {
                Node<K,V> e = first; K k;
                do {
                    if (e.hash == hash &&
                        ((k = e.key) == key || (key != null && key.equals(k)))) {
                        old = e;
                        break;
                    }
                    ++binCount;
                } while ((e = e.next) != null);
            }
            V oldValue;
            if (old != null && (oldValue = old.value) != null) {
                afterNodeAccess(old);
                return oldValue;
            }
        }
        V v = mappingFunction.apply(key);
        if (v == null) {
            return null;
        } else if (old != null) {
            old.value = v;
            afterNodeAccess(old);
            return v;
        }
        else if (t != null)
            t.putTreeVal(this, tab, hash, key, v);
        else {
            tab[i] = newNode(hash, key, v, first);
            if (binCount >= TREEIFY_THRESHOLD - 1)
                treeifyBin(tab, hash);
        }
        ++modCount;
        ++size;
        afterNodeInsertion(true);
        return v;
    }

    public V computeIfPresent(K key,
                              BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (remappingFunction == null)
            throw new NullPointerException();
        Node<K,V> e; V oldValue;
        int hash = hash(key);
        if ((e = getNode(hash, key)) != null &&
            (oldValue = e.value) != null) {
            V v = remappingFunction.apply(key, oldValue);
            if (v != null) {
                e.value = v;
                afterNodeAccess(e);
                return v;
            }
            else
                removeNode(hash, key, null, false, true);
        }
        return null;
    }

    @Override
    public V compute(K key,
                     BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (remappingFunction == null)
            throw new NullPointerException();
        int hash = hash(key);
        Node<K,V>[] tab; Node<K,V> first; int n, i;
        int binCount = 0;
        TreeNode<K,V> t = null;
        Node<K,V> old = null;
        if (size > threshold || (tab = table) == null ||
            (n = tab.length) == 0)
            n = (tab = resize()).length;
        if ((first = tab[i = (n - 1) & hash]) != null) {
            if (first instanceof TreeNode)
                old = (t = (TreeNode<K,V>)first).getTreeNode(hash, key);
            else {
                Node<K,V> e = first; K k;
                do {
                    if (e.hash == hash &&
                        ((k = e.key) == key || (key != null && key.equals(k)))) {
                        old = e;
                        break;
                    }
                    ++binCount;
                } while ((e = e.next) != null);
            }
        }
        V oldValue = (old == null) ? null : old.value;
        V v = remappingFunction.apply(key, oldValue);
        if (old != null) {
            if (v != null) {
                old.value = v;
                afterNodeAccess(old);
            }
            else
                removeNode(hash, key, null, false, true);
        }
        else if (v != null) {
            if (t != null)
                t.putTreeVal(this, tab, hash, key, v);
            else {
                tab[i] = newNode(hash, key, v, first);
                if (binCount >= TREEIFY_THRESHOLD - 1)
                    treeifyBin(tab, hash);
            }
            ++modCount;
            ++size;
            afterNodeInsertion(true);
        }
        return v;
    }

    @Override
    public V merge(K key, V value,
                   BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        if (value == null)
            throw new NullPointerException();
        if (remappingFunction == null)
            throw new NullPointerException();
        int hash = hash(key);
        Node<K,V>[] tab; Node<K,V> first; int n, i;
        int binCount = 0;
        TreeNode<K,V> t = null;
        Node<K,V> old = null;
        if (size > threshold || (tab = table) == null ||
            (n = tab.length) == 0)
            n = (tab = resize()).length;
        if ((first = tab[i = (n - 1) & hash]) != null) {
            if (first instanceof TreeNode)
                old = (t = (TreeNode<K,V>)first).getTreeNode(hash, key);
            else {
                Node<K,V> e = first; K k;
                do {
                    if (e.hash == hash &&
                        ((k = e.key) == key || (key != null && key.equals(k)))) {
                        old = e;
                        break;
                    }
                    ++binCount;
                } while ((e = e.next) != null);
            }
        }
        if (old != null) {
            V v;
            if (old.value != null)
                v = remappingFunction.apply(old.value, value);
            else
                v = value;
            if (v != null) {
                old.value = v;
                afterNodeAccess(old);
            }
            else
                removeNode(hash, key, null, false, true);
            return v;
        }
        if (value != null) {
            if (t != null)
                t.putTreeVal(this, tab, hash, key, value);
            else {
                tab[i] = newNode(hash, key, value, first);
                if (binCount >= TREEIFY_THRESHOLD - 1)
                    treeifyBin(tab, hash);
            }
            ++modCount;
            ++size;
            afterNodeInsertion(true);
        }
        return value;
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        Node<K,V>[] tab;
        if (action == null)
            throw new NullPointerException();
        if (size > 0 && (tab = table) != null) {
            int mc = modCount;
            for (int i = 0; i < tab.length; ++i) {
                for (Node<K,V> e = tab[i]; e != null; e = e.next)
                    action.accept(e.key, e.value);
            }
            if (modCount != mc)
                throw new ConcurrentModificationException();
        }
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        Node<K,V>[] tab;
        if (function == null)
            throw new NullPointerException();
        if (size > 0 && (tab = table) != null) {
            int mc = modCount;
            for (int i = 0; i < tab.length; ++i) {
                for (Node<K,V> e = tab[i]; e != null; e = e.next) {
                    e.value = function.apply(e.key, e.value);
                }
            }
            if (modCount != mc)
                throw new ConcurrentModificationException();
        }
    }

    /* ------------------------------------------------------------ */
    // Cloning and serialization

    /**
     * Returns a shallow copy of this <tt>HashMap</tt> instance: the keys and
     * values themselves are not cloned.
     *
     * @return a shallow copy of this map
     */
    @SuppressWarnings("unchecked")
    @Override
    public Object clone() {
        HashMap<K,V> result;
        try {
            result = (HashMap<K,V>)super.clone();
        } catch (CloneNotSupportedException e) {
            // this shouldn't happen, since we are Cloneable
            throw new InternalError(e);
        }
        result.reinitialize();
        result.putMapEntries(this, false);
        return result;
    }

    // These methods are also used when serializing HashSets
    final float loadFactor() { return loadFactor; }
    final int capacity() {
        return (table != null) ? table.length :
            (threshold > 0) ? threshold :
            DEFAULT_INITIAL_CAPACITY;
    }

    /**
     * Save the state of the <tt>HashMap</tt> instance to a stream (i.e.,
     * serialize it).
     *
     * @serialData The <i>capacity</i> of the HashMap (the length of the
     *             bucket array) is emitted (int), followed by the
     *             <i>size</i> (an int, the number of key-value
     *             mappings), followed by the key (Object) and value (Object)
     *             for each key-value mapping.  The key-value mappings are
     *             emitted in no particular order.
     */
    private void writeObject(java.io.ObjectOutputStream s)
        throws IOException {
        int buckets = capacity();
        // Write out the threshold, loadfactor, and any hidden stuff
        s.defaultWriteObject();
        s.writeInt(buckets);
        s.writeInt(size);
        internalWriteEntries(s);
    }

    /**
     * Reconstitute the {@code HashMap} instance from a stream (i.e.,
     * deserialize it).
     */
    private void readObject(java.io.ObjectInputStream s)
        throws IOException, ClassNotFoundException {
        // Read in the threshold (ignored), loadfactor, and any hidden stuff
        s.defaultReadObject();
        reinitialize();
        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new InvalidObjectException("Illegal load factor: " +
                                             loadFactor);
        s.readInt();                // Read and ignore number of buckets
        int mappings = s.readInt(); // Read number of mappings (size)
        if (mappings < 0)
            throw new InvalidObjectException("Illegal mappings count: " +
                                             mappings);
        else if (mappings > 0) { // (if zero, use defaults)
            // Size the table using given load factor only if within
            // range of 0.25...4.0
            float lf = Math.min(Math.max(0.25f, loadFactor), 4.0f);
            float fc = (float)mappings / lf + 1.0f;
            int cap = ((fc < DEFAULT_INITIAL_CAPACITY) ?
                       DEFAULT_INITIAL_CAPACITY :
                       (fc >= MAXIMUM_CAPACITY) ?
                       MAXIMUM_CAPACITY :
                       tableSizeFor((int)fc));
            float ft = (float)cap * lf;
            threshold = ((cap < MAXIMUM_CAPACITY && ft < MAXIMUM_CAPACITY) ?
                         (int)ft : Integer.MAX_VALUE);
            @SuppressWarnings({"rawtypes","unchecked"})
                Node<K,V>[] tab = (Node<K,V>[])new Node[cap];
            table = tab;

            // Read the keys and values, and put the mappings in the HashMap
            for (int i = 0; i < mappings; i++) {
                @SuppressWarnings("unchecked")
                    K key = (K) s.readObject();
                @SuppressWarnings("unchecked")
                    V value = (V) s.readObject();
                putVal(hash(key), key, value, false, false);
            }
        }
    }

    /* ------------------------------------------------------------ */
    // iterators

    abstract class HashIterator {
        Node<K,V> next;        // next entry to return
        Node<K,V> current;     // current entry
        int expectedModCount;  // for fast-fail
        int index;             // current slot

        HashIterator() {
            expectedModCount = modCount;
            Node<K,V>[] t = table;
            current = next = null;
            index = 0;
            if (t != null && size > 0) { // advance to first entry
                // 为了在数组中找到下一个，并赋值next
                do {} while (index < t.length && (next = t[index++]) == null);
            }
        }

        public final boolean hasNext() {
            return next != null;
        }

        final Node<K,V> nextNode() {
            Node<K,V>[] t;
            Node<K,V> e = next;
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            if (e == null)
                throw new NoSuchElementException();
            if ((next = (current = e).next) == null && (t = table) != null) {
                do {} while (index < t.length && (next = t[index++]) == null);
            }
            return e;
        }

        public final void remove() {
            Node<K,V> p = current;
            if (p == null)
                throw new IllegalStateException();
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            current = null;
            K key = p.key;
            removeNode(hash(key), key, null, false, false);
            expectedModCount = modCount;
        }
    }

    final class KeyIterator extends HashIterator
        implements Iterator<K> {
        public final K next() { return nextNode().key; }
    }

    final class ValueIterator extends HashIterator
        implements Iterator<V> {
        public final V next() { return nextNode().value; }
    }

    final class EntryIterator extends HashIterator
        implements Iterator<Map.Entry<K,V>> {
        public final Map.Entry<K,V> next() { return nextNode(); }
    }

    /* ------------------------------------------------------------ */
    // spliterators

    static class HashMapSpliterator<K,V> {
        final HashMap<K,V> map;
        Node<K,V> current;          // current node
        int index;                  // current index, modified on advance/split
        int fence;                  // one past last index
        int est;                    // size estimate
        int expectedModCount;       // for comodification checks

        HashMapSpliterator(HashMap<K,V> m, int origin,
                           int fence, int est,
                           int expectedModCount) {
            this.map = m;
            this.index = origin;
            this.fence = fence;
            this.est = est;
            this.expectedModCount = expectedModCount;
        }

        final int getFence() { // initialize fence and size on first use
            int hi;
            if ((hi = fence) < 0) {
                HashMap<K,V> m = map;
                est = m.size;
                expectedModCount = m.modCount;
                Node<K,V>[] tab = m.table;
                hi = fence = (tab == null) ? 0 : tab.length;
            }
            return hi;
        }

        public final long estimateSize() {
            getFence(); // force init
            return (long) est;
        }
    }

    static final class KeySpliterator<K,V>
        extends HashMapSpliterator<K,V>
        implements Spliterator<K> {
        KeySpliterator(HashMap<K,V> m, int origin, int fence, int est,
                       int expectedModCount) {
            super(m, origin, fence, est, expectedModCount);
        }

        public KeySpliterator<K,V> trySplit() {
            int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
            return (lo >= mid || current != null) ? null :
                new KeySpliterator<>(map, lo, index = mid, est >>>= 1,
                                        expectedModCount);
        }

        public void forEachRemaining(Consumer<? super K> action) {
            int i, hi, mc;
            if (action == null)
                throw new NullPointerException();
            HashMap<K,V> m = map;
            Node<K,V>[] tab = m.table;
            if ((hi = fence) < 0) {
                mc = expectedModCount = m.modCount;
                hi = fence = (tab == null) ? 0 : tab.length;
            }
            else
                mc = expectedModCount;
            if (tab != null && tab.length >= hi &&
                (i = index) >= 0 && (i < (index = hi) || current != null)) {
                Node<K,V> p = current;
                current = null;
                do {
                    if (p == null)
                        p = tab[i++];
                    else {
                        action.accept(p.key);
                        p = p.next;
                    }
                } while (p != null || i < hi);
                if (m.modCount != mc)
                    throw new ConcurrentModificationException();
            }
        }

        public boolean tryAdvance(Consumer<? super K> action) {
            int hi;
            if (action == null)
                throw new NullPointerException();
            Node<K,V>[] tab = map.table;
            if (tab != null && tab.length >= (hi = getFence()) && index >= 0) {
                while (current != null || index < hi) {
                    if (current == null)
                        current = tab[index++];
                    else {
                        K k = current.key;
                        current = current.next;
                        action.accept(k);
                        if (map.modCount != expectedModCount)
                            throw new ConcurrentModificationException();
                        return true;
                    }
                }
            }
            return false;
        }

        public int characteristics() {
            return (fence < 0 || est == map.size ? Spliterator.SIZED : 0) |
                Spliterator.DISTINCT;
        }
    }

    static final class ValueSpliterator<K,V>
        extends HashMapSpliterator<K,V>
        implements Spliterator<V> {
        ValueSpliterator(HashMap<K,V> m, int origin, int fence, int est,
                         int expectedModCount) {
            super(m, origin, fence, est, expectedModCount);
        }

        public ValueSpliterator<K,V> trySplit() {
            int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
            return (lo >= mid || current != null) ? null :
                new ValueSpliterator<>(map, lo, index = mid, est >>>= 1,
                                          expectedModCount);
        }

        public void forEachRemaining(Consumer<? super V> action) {
            int i, hi, mc;
            if (action == null)
                throw new NullPointerException();
            HashMap<K,V> m = map;
            Node<K,V>[] tab = m.table;
            if ((hi = fence) < 0) {
                mc = expectedModCount = m.modCount;
                hi = fence = (tab == null) ? 0 : tab.length;
            }
            else
                mc = expectedModCount;
            if (tab != null && tab.length >= hi &&
                (i = index) >= 0 && (i < (index = hi) || current != null)) {
                Node<K,V> p = current;
                current = null;
                do {
                    if (p == null)
                        p = tab[i++];
                    else {
                        action.accept(p.value);
                        p = p.next;
                    }
                } while (p != null || i < hi);
                if (m.modCount != mc)
                    throw new ConcurrentModificationException();
            }
        }

        public boolean tryAdvance(Consumer<? super V> action) {
            int hi;
            if (action == null)
                throw new NullPointerException();
            Node<K,V>[] tab = map.table;
            if (tab != null && tab.length >= (hi = getFence()) && index >= 0) {
                while (current != null || index < hi) {
                    if (current == null)
                        current = tab[index++];
                    else {
                        V v = current.value;
                        current = current.next;
                        action.accept(v);
                        if (map.modCount != expectedModCount)
                            throw new ConcurrentModificationException();
                        return true;
                    }
                }
            }
            return false;
        }

        public int characteristics() {
            return (fence < 0 || est == map.size ? Spliterator.SIZED : 0);
        }
    }

    static final class EntrySpliterator<K,V>
        extends HashMapSpliterator<K,V>
        implements Spliterator<Map.Entry<K,V>> {
        EntrySpliterator(HashMap<K,V> m, int origin, int fence, int est,
                         int expectedModCount) {
            super(m, origin, fence, est, expectedModCount);
        }

        public EntrySpliterator<K,V> trySplit() {
            int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
            return (lo >= mid || current != null) ? null :
                new EntrySpliterator<>(map, lo, index = mid, est >>>= 1,
                                          expectedModCount);
        }

        public void forEachRemaining(Consumer<? super Map.Entry<K,V>> action) {
            int i, hi, mc;
            if (action == null)
                throw new NullPointerException();
            HashMap<K,V> m = map;
            Node<K,V>[] tab = m.table;
            if ((hi = fence) < 0) {
                mc = expectedModCount = m.modCount;
                hi = fence = (tab == null) ? 0 : tab.length;
            }
            else
                mc = expectedModCount;
            if (tab != null && tab.length >= hi &&
                (i = index) >= 0 && (i < (index = hi) || current != null)) {
                Node<K,V> p = current;
                current = null;
                do {
                    if (p == null)
                        p = tab[i++];
                    else {
                        action.accept(p);
                        p = p.next;
                    }
                } while (p != null || i < hi);
                if (m.modCount != mc)
                    throw new ConcurrentModificationException();
            }
        }

        public boolean tryAdvance(Consumer<? super Map.Entry<K,V>> action) {
            int hi;
            if (action == null)
                throw new NullPointerException();
            Node<K,V>[] tab = map.table;
            if (tab != null && tab.length >= (hi = getFence()) && index >= 0) {
                while (current != null || index < hi) {
                    if (current == null)
                        current = tab[index++];
                    else {
                        Node<K,V> e = current;
                        current = current.next;
                        action.accept(e);
                        if (map.modCount != expectedModCount)
                            throw new ConcurrentModificationException();
                        return true;
                    }
                }
            }
            return false;
        }

        public int characteristics() {
            return (fence < 0 || est == map.size ? Spliterator.SIZED : 0) |
                Spliterator.DISTINCT;
        }
    }

    /* ------------------------------------------------------------ */
    // LinkedHashMap support


    /*
     * The following package-protected methods are designed to be
     * overridden by LinkedHashMap, but not by any other subclass.
     * Nearly all other internal methods are also package-protected
     * but are declared final, so can be used by LinkedHashMap, view
     * classes, and HashSet.
     */

    // Create a regular (non-tree) node
    Node<K,V> newNode(int hash, K key, V value, Node<K,V> next) {
        return new Node<>(hash, key, value, next);
    }

    // For conversion from TreeNodes to plain nodes
    Node<K,V> replacementNode(Node<K,V> p, Node<K,V> next) {
        return new Node<>(p.hash, p.key, p.value, next);
    }

    // Create a tree bin node
    TreeNode<K,V> newTreeNode(int hash, K key, V value, Node<K,V> next) {
        return new TreeNode<>(hash, key, value, next);
    }

    // For treeifyBin
    TreeNode<K,V> replacementTreeNode(Node<K,V> p, Node<K,V> next) {
        return new TreeNode<>(p.hash, p.key, p.value, next);
    }

    /**
     * Reset to initial default state.  Called by clone and readObject.
     */
    void reinitialize() {
        table = null;
        entrySet = null;
        keySet = null;
        values = null;
        modCount = 0;
        threshold = 0;
        size = 0;
    }

    // Callbacks to allow LinkedHashMap post-actions
    void afterNodeAccess(Node<K,V> p) { }
    void afterNodeInsertion(boolean evict) { }
    void afterNodeRemoval(Node<K,V> p) { }

    // Called only from writeObject, to ensure compatible ordering.
    void internalWriteEntries(java.io.ObjectOutputStream s) throws IOException {
        Node<K,V>[] tab;
        if (size > 0 && (tab = table) != null) {
            for (int i = 0; i < tab.length; ++i) {
                for (Node<K,V> e = tab[i]; e != null; e = e.next) {
                    s.writeObject(e.key);
                    s.writeObject(e.value);
                }
            }
        }
    }

    /* ------------------------------------------------------------ */
    // Tree bins

    /**
     * Entry for Tree bins. Extends LinkedHashMap.Entry (which in turn
     * extends Node) so can be used as extension of either regular or
     * linked node.
     */
    static final class TreeNode<K,V> extends LinkedHashMap.Entry<K,V> {
        TreeNode<K,V> parent;  // red-black tree links
        TreeNode<K,V> left;
        TreeNode<K,V> right;
        TreeNode<K,V> prev;    // needed to unlink next upon deletion
        boolean red;
        TreeNode(int hash, K key, V val, Node<K,V> next) {
            super(hash, key, val, next);
        }

        /**
         * Returns root of tree containing this node.
         */
        final TreeNode<K,V> root() {
            for (TreeNode<K,V> r = this, p;;) {
                if ((p = r.parent) == null)
                    return r;
                r = p;
            }
        }

        /**
         * Ensures that the given root is the first node of its bin.
         */
        //确保给定根是的第一个节点
        static <K,V> void moveRootToFront(Node<K,V>[] tab, TreeNode<K,V> root) {
            int n;
            // 树的根节点不为空，数组不为空
            if (root != null && tab != null && (n = tab.length) > 0) {
                // 树在数组中的下标
                int index = (n - 1) & root.hash;
                // 数组位置的第一个节点
                TreeNode<K,V> first = (TreeNode<K,V>)tab[index];
                // 根节点不是数组位置的第一个
                if (root != first) {
                    Node<K,V> rn;
                    // 移动根节点到数组到数组节点
                    tab[index] = root;
                    TreeNode<K,V> rp = root.prev;

                    // 链表处理

                    if ((rn = root.next) != null)
                        // 根节点第一个节点的next节点的上一个指向根节点的上一个
                        ((TreeNode<K,V>)rn).prev = rp;//相当于把root从链表中摘除
                    if (rp != null)
                        // 根节点第一个节点的next节点的上一个指向根节点的上一个
                        rp.next = rn;
                    if (first != null)
                        first.prev = root;
                    root.next = first;
                    root.prev = null;
                }
                // 移动之后会确认下当前数据结构是否符合红黑树性质
                /*
                 * 这一步是防御性的编程
                 * 校验TreeNode对象是否满足红黑树和双链表的特性
                 * 如果这个方法校验不通过：可能是因为用户编程失误，破坏了结构（例如：并发场景下）；也可能是TreeNode的实现有问题（这个是理论上的以防万一）；
                 */
                assert checkInvariants(root);
            }
        }

        /**
         * Finds the node starting at root p with the given hash and key.
         * The kc argument caches comparableClassFor(key) upon first use
         * comparing keys.
         */
        // 使用给定的哈希和键查找从根p开始的节点。kc参数在首次使用比较键时缓存compatileClassFor（key）。
        final TreeNode<K,V> find(int h, Object k, Class<?> kc) {
            TreeNode<K,V> p = this;
            do {
                int ph, dir; K pk;
                // 获取当前节点的左孩子、右孩子。定义一个对象q用来存储并返回找到的对象
                TreeNode<K,V> pl = p.left, pr = p.right, q;
                // 从根节点开始找，根节点hash大于要查找的值说明需要查找的在左边。
                if ((ph = p.hash) > h)
                    p = pl;
                else if (ph < h)
                    p = pr;
                else if ((pk = p.key) == k || (k != null && k.equals(pk)))
                    return p;
                    // 执行到这里说明 hash比对相同，但是pk和k不相等（ k.equals(pk) == false）
                else if (pl == null)
                    p = pr;
                else if (pr == null)
                    p = pl;
                // 这一轮的对比主要是想通过comparable方法来比较pk和k的大小
                else if ((kc != null ||
                        // kc 是可比较类
                          (kc = comparableClassFor(k)) != null) &&
                         (dir = compareComparables(kc, k, pk)) != 0)
                    p = (dir < 0) ? pl : pr;
                // 执行到这里说明两key值比较之后还是相等
                    // 从右孩子节点递归循环查找，如果找到了匹配的则返回
                else if ((q = pr.find(h, k, kc)) != null)
                    return q;
                else
                    p = pl;
            } while (p != null);
            return null;
        }

        /**
         * Calls find for root node.
         */
        final TreeNode<K,V> getTreeNode(int h, Object k) {
            return ((parent != null) ? root() : this).find(h, k, null);
        }

        /**
         * Tie-breaking utility for ordering insertions when equal
         * hashCodes and non-comparable. We don't require a total
         * order, just a consistent insertion rule to maintain
         * equivalence across rebalancings. Tie-breaking further than
         * necessary simplifies testing a bit.
         */
        static int tieBreakOrder(Object a, Object b) {
            int d;
            if (a == null || b == null ||
                (d = a.getClass().getName().
                 compareTo(b.getClass().getName())) == 0)
                //System.identityHashCode：用于返回给定对象的哈希码–通过使用此方法，哈希码的值将与使用hashCode()方法的哈希码的值相同。
                d = (System.identityHashCode(a) <= System.identityHashCode(b) ?
                     -1 : 1);
            return d;
        }

        /**
         * Forms tree of the nodes linked from this node.
         * @return root of tree
         */
        // 根据链表生成树，其实就是遍历链表，一个一个红黑树插入。
        // 插入后调用balanceInsertion()，插入后的平衡
        final void treeify(Node<K,V>[] tab) {
            TreeNode<K,V> root = null;
            // x 链表中的数据
            for (TreeNode<K,V> x = this, next; x != null; x = next) {
                next = (TreeNode<K,V>)x.next;
                // 将左右节点重置为空
                x.left = x.right = null;
                if (root == null) {
                    // 第一次循环进来
                    // 设置根节点且为黑色
                    x.parent = null;
                    x.red = false;
                    //
                    root = x;
                }
                else {
                    K k = x.key;
                    int h = x.hash;
                    Class<?> kc = null;
                    // 从根节点便利，找到插入位置
                    for (TreeNode<K,V> p = root;;) {
                        int dir, ph;
                        K pk = p.key;
                        if ((ph = p.hash) > h)
                            // 当前节点hash小于根节点的hash
                            dir = -1;
                        else if (ph < h)
                            // 当前节点hash大于根节点hash
                            dir = 1;
                        else if ((kc == null &&
                                  (kc = comparableClassFor(k)) == null) ||
                               // 找到新key是否实现comparable接口，如果两个key是相同的类，然后在比较两个对象的
                                 (dir = compareComparables(kc, k, pk)) == 0)
                            // 如果kc 为空 或 两个不是相同的类
                            // 再通过对象hash比较
                            // 调用System.identityHashCode(Object o) 比较对象hash码
                            // 返回给定对象的哈希码，该代码与默认的方法 hashCode() 返回的代码一样，无论给定对象的类是否重写 hashCode()。null 引用的哈希码为 0。
                            dir = tieBreakOrder(k, pk);

                        // dir 小于零指向左子树 dir 大于零指向右子树
                        // 如果为null 直接插入 如果不为null 则继续比较
                        TreeNode<K,V> xp = p;
                        if ((p = (dir <= 0) ? p.left : p.right) == null) {
                            // 连接当前节点
                            x.parent = xp;
                            if (dir <= 0)
                                xp.left = x;
                            else
                                xp.right = x;
                            root = balanceInsertion(root, x);
                            break;
                        }
                    }
                }
            }
            moveRootToFront(tab, root);
        }

        /**
         * Returns a list of non-TreeNodes replacing those linked from
         * this node.
         */
        final Node<K,V> untreeify(HashMap<K,V> map) {
            Node<K,V> hd = null, tl = null;
            // q:当前节点，替换到map 的数组上
            // 一个个插进去，可能会有树的转换
            for (Node<K,V> q = this; q != null; q = q.next) {
                Node<K,V> p = map.replacementNode(q, null);
                if (tl == null)
                    hd = p;
                else
                    tl.next = p;
                tl = p;
            }
            //返回头节点
            return hd;
        }

        /**
         * Tree version of putVal.
         */
        // h为新key 的hash值
        final TreeNode<K,V> putTreeVal(HashMap<K,V> map, Node<K,V>[] tab,
                                       int h, K k, V v) {
            Class<?> kc = null;
            boolean searched = false;
            // this： 为TreeNode
            TreeNode<K,V> root = (parent != null) ? root() : this;
            // 找到红黑树的根节点，从根节点开始遍历
            for (TreeNode<K,V> p = root;;) {
                // dir：
                int dir, ph; K pk;
                if ((ph = p.hash) > h)
                    dir = -1;
                else if (ph < h)
                    dir = 1;
                else if ((pk = p.key) == k || (k != null && k.equals(pk)))
                    // 找到树节点的k值相等直接返回
                    return p;
                //到这里说明key的hash值相等，但是值不相等
                else if ((kc == null &&
                          (kc = comparableClassFor(k)) == null) ||
                        // 找到新key是否实现comparable接口，如果两个key是相同的对象，然后在比较两个对象的
                         (dir = compareComparables(kc, k, pk)) == 0) {
                    if (!searched) {
                        TreeNode<K,V> q, ch;
                        searched = true;
                        if (((ch = p.left) != null &&
                             (q = ch.find(h, k, kc)) != null) ||
                            ((ch = p.right) != null &&
                             (q = ch.find(h, k, kc)) != null))
                            return q;
                    }
                    dir = tieBreakOrder(k, pk);
                }

                // 新建节点放入红黑树相应位置
                TreeNode<K,V> xp = p;
                if ((p = (dir <= 0) ? p.left : p.right) == null) {
                    // xpn：当前节点的下一个
                    Node<K,V> xpn = xp.next;
                    TreeNode<K,V> x = map.newTreeNode(h, k, v, xpn);
                    if (dir <= 0)
                        xp.left = x;
                    else
                        xp.right = x;
                    // 新节点赋值给当前的next节点
                    xp.next = x;
                    // 新节点的上一个和父节点都是 当前的节点
                    x.parent = x.prev = xp;
                    if (xpn != null)
                        ((TreeNode<K,V>)xpn).prev = x;
                    // 红黑树平衡调整；移动根节点到数组节点
                    moveRootToFront(tab, balanceInsertion(root, x));
                    return null;
                }
            }
        }

        /**
         * Removes the given node, that must be present before this call.
         * This is messier than typical red-black deletion code because we
         * cannot swap the contents of an interior node with a leaf
         * successor that is pinned by "next" pointers that are accessible
         * independently during traversal. So instead we swap the tree
         * linkages. If the current tree appears to have too few nodes,
         * the bin is converted back to a plain bin. (The test triggers
         * somewhere between 2 and 6 nodes, depending on tree structure).
         */
        final void removeTreeNode(HashMap<K,V> map, Node<K,V>[] tab,
                                  boolean movable) {
            int n;
            if (tab == null || (n = tab.length) == 0)
                return;
            int index = (n - 1) & hash;
            // 通过hash值定位数组位置
            TreeNode<K,V> first = (TreeNode<K,V>)tab[index], root = first, rl;
            TreeNode<K,V> succ = (TreeNode<K,V>)next, pred = prev;
            // 前一个节点为空， 删除节点的next作为数组第一个节点
            if (pred == null)
                tab[index] = first = succ;
            // 前一个节点不为空，则前一个节点的后一个节点指向后一个（其实就是把当前删了）
            // 删除节点上一个与删除节点下一个相连（链表）
            else
                pred.next = succ;
            // 如果后一个节点不为空，将后一个节点的前节点指向当前节点的前一个
            if (succ != null)
                // 上边删除节点的上一个指向了删除节点的下一个（此时下一个指向上一个， 首尾相连）
                succ.prev = pred;
            // 如果第一个节点为空直接返回（此时改数组位置没有节点）
            if (first == null)
                return;
            // 根节点存在父节点，说明不是根节点，调用root（）获取，确保root是根节点
            // 树的情况？
            if (root.parent != null)
                root = root.root();
            // 根据节点及其左右子树，来判断此时红黑树节点的数量，进而转为链表
            // 通过root节点来判断此红黑树是否太小, 如果是则调用untreeify方法转为链表节点并返回
            if (root == null || root.right == null ||
                (rl = root.left) == null || rl.left == null) {
                tab[index] = first.untreeify(map);  // too small
                return;
            }
            // p要删除的节点，replacement删除后代替他的节点
            // 删除了一个中间的节点，但是他还有子节点，肯定是要连接到树上的，此时需要一个节点但来顶替他的位置
            // 找到哪个作为代替节点？
            TreeNode<K,V> p = this, pl = left, pr = right, replacement;
            // 删除节点，左右孩纸都不为空时
            if (pl != null && pr != null) {
                // s： 当删除节点的右节点的最左节点（此节点是删除节点的替换节点）
                TreeNode<K,V> s = pr, sl;
                while ((sl = s.left) != null) // find successor
                    s = sl;
                // 颜色交换（当前节点和最小节点）
                boolean c = s.red; s.red = p.red; p.red = c; // swap colors
                TreeNode<K,V> sr = s.right;   // sr ： 替换节点的右子树(因为最小左子树可能有右儿子)
                TreeNode<K,V> pp = p.parent;  // pp： 删除节点的父节点
                // 位置交换
                // 删除节点右子树没有左子树（s本来是右节点最小左节点）
                if (s == pr) { // p was s's direct parent
                    // 删除节点跟右子节点交换位置
                    p.parent = s;
                    s.right = p;
                }
                // 删除节点右节点有左节点
                else {
                    // sp：删除节点右节点的左节点的父节点
                    TreeNode<K,V> sp = s.parent;
                    //当前节点的指向，替换节点的父节点
                    // 交换位置
                        if ((p.parent = sp) != null) {
                        if (s == sp.left)
                            sp.left = p;
                        else
                            sp.right = p;
                    }
                        // 替换上来的节点连接右节点
                    // 原父节点，原左节点还没链接好呢？？？？？？（在下面啦）
                    if ((s.right = pr) != null)
                        pr.parent = s;
                }
                // 替换了肯定没有左节点
                p.left = null;
                // 将替换节点的右节点的最小左左节点的右节点的父节点 与 删除节点相连
                if ((p.right = sr) != null)
                    sr.parent = p;
                // 删除的左节点连 与 交换后的新节点 相连
                if ((s.left = pl) != null)
                    pl.parent = s;
                // 父节点 与 交换后的新节点 相连
                if ((s.parent = pp) == null)
                    // 如果源节点父节点为空，则删除节点为根节点
                    root = s;
                else if (p == pp.left)// 删除节点是父节点的左节点 连接上新节点
                    // 如果源节点父节点不为空且左子树不为空
                    // 将新节点作为根节点的左子树
                    pp.left = s;
                else // 删除节点是父节点的右节点 连接上新节点
                    // 否则将新节点作为根节点的右子树
                    pp.right = s;
                if (sr != null) // sr：为跟删除节点交换位置的节点 的 右节点
                    // 将替换节点的右节点的最小左左节点不为空
                    // 赋值给替换节点
                    replacement = sr;
                else
                    //否则将原节点作为替换节点
                    replacement = p;
            }
            // 删除节点，左右孩纸都不为空不成立
            else if (pl != null)
                // 原节点的左节点做为替换节点
                replacement = pl;
            else if (pr != null)
                // 原节点的右节点做为替换节点
                replacement = pr;
            else
                //都没有满足原节点作为替换节点
                replacement = p;
            // 如果替换节点不为原节点
            if (replacement != p) {
                /**
                 *  到这里已经是将删除的节点和代替它的节点交换完成
                 */
                // 把原节点的父节点赋值给新节点的父节点
                TreeNode<K,V> pp = replacement.parent = p.parent;
                if (pp == null)
                    root = replacement;
                else if (p == pp.left)
                    pp.left = replacement;
                else
                    pp.right = replacement;
                // 把删除节点删除掉
                p.left = p.right = p.parent = null;
            }
            // 删除节点为红色 ？ 根 ：树平衡调整
            TreeNode<K,V> r = p.red ? root : balanceDeletion(root, replacement);

            // 没有左右子树，替换替换节点为当前节点
            // 需要将删除节点的父节点 断开连接
            // 如果右左右子树存在，在上面已经删除掉啦
            if (replacement == p) {  // detach
                TreeNode<K,V> pp = p.parent;
                p.parent = null;
                if (pp != null) {
                    if (p == pp.left)
                        pp.left = null;
                    else if (p == pp.right)
                        pp.right = null;
                }
            }
            // 将根节点移到数据第一个
            if (movable)
                moveRootToFront(tab, r);
        }

        /**
         * Splits nodes in a tree bin into lower and upper tree bins,
         * or untreeifies if now too small. Called only from resize;
         * see above discussion about split bits and indices.
         *
         * @param map the map
         * @param tab the table for recording bin heads
         * @param index the index of the table being split
         * @param bit the bit of hash to split on
         */
        /**
         * 当hasmmap 的node类型为TreeNode，它的next属性是怎么相连的？
         *  把父节点的next 赋值到新节点的next；父节点的next指向新节点
         * @param map
         * @param tab
         * @param index 当前节点在旧的数组中的位置
         * @param bit 旧数组容量 （通过当前节点的hash值 和 bit 做 & 得到 0 和 1 确定节点在新数组中的位置）
         */
        final void split(HashMap<K,V> map, Node<K,V>[] tab, int index, int bit) {
            TreeNode<K,V> b = this;// 当前节点
            // Relink into lo and hi lists, preserving order
            TreeNode<K,V> loHead = null, loTail = null;
            TreeNode<K,V> hiHead = null, hiTail = null;
            // lc进来节点相连node个数
            int lc = 0, hc = 0;
            for (TreeNode<K,V> e = b, next; e != null; e = next) {
                next = (TreeNode<K,V>)e.next;
                e.next = null;
                if ((e.hash & bit) == 0) {
                    if ((e.prev = loTail) == null)
                        loHead = e;//先放头
                    else
                        loTail.next = e;
                    loTail = e; // 再连接尾
                    ++lc;
                }
                else {
                    if ((e.prev = hiTail) == null)
                        hiHead = e;
                    else
                        hiTail.next = e;
                    hiTail = e;
                    ++hc;
                }
            }

            if (loHead != null) {
                if (lc <= UNTREEIFY_THRESHOLD)// 链表连接node
                    tab[index] = loHead.untreeify(map);
                else {
                    tab[index] = loHead;
                    if (hiHead != null) // (else is already treeified)//如果hiHead为空则已经转成树了
                        //转树
                        loHead.treeify(tab);
                }
            }
            if (hiHead != null) {
                if (hc <= UNTREEIFY_THRESHOLD)
                    tab[index + bit] = hiHead.untreeify(map);
                else {
                    tab[index + bit] = hiHead;
                    if (loHead != null)
                        hiHead.treeify(tab);
                }
            }
        }

        /* ------------------------------------------------------------ */
        // Red-black tree methods, all adapted from CLR

        /**
         * 节点左旋
         * @param root 根节点
         * @param p 要左旋的节点
         */
        static <K,V> TreeNode<K,V> rotateLeft(TreeNode<K,V> root,
                                              TreeNode<K,V> p) {
            TreeNode<K,V> r, pp, rl;
            // r ： 左旋节点的右孩子
            // pp: 当前节点的父节点
            // rl: 左旋节点右孩子的左节点

            if (p != null && (r = p.right) != null) {// 要左旋的节点和要左旋的节点的右孩子不为空
                // 要左旋的节点的右孩子的左节点 赋给 要左旋的节点的右孩子 节点为：rl
                if ((rl = p.right = r.left) != null)
                    // 注意：两个节点相连，需要将A节点的左或右节点指向B，B的父节点也需要指向A
                    rl.parent = p;// 设置rl和要左旋的节点的父子关系

                // 将要左旋的节点的右孩子的父节点  指向 要左旋的节点的父节点（相当于右孩子提升了一层），
                if ((pp = r.parent = p.parent) == null)
                    // 此时如果父节点为空， 说明r 已经是顶层节点了，应该作为root 并且标为黑色
                    (root = r).red = false;
                    // 如果父节点不为空 并且 要左旋的节点是个左孩子
                else if (pp.left == p)
                    // 设置r和父节点的父子关系【之前只是孩子认了爹，爹还没有答应，这一步爹也认了孩子】
                    pp.left = r;
                else // 要左旋的节点是个右孩子
                    pp.right = r; // 爹认孩子
                r.left = p; // 要左旋的节点  作为 他的右孩子的左节点
                p.parent = r;// 要左旋的节点的右孩子  作为  他的父节点
            }
            // 返回根节点
            return root;
        }

        /**
         * 节点右旋
         * @param root 根节点
         * @param p 要右旋的节点
         * @return 返回根节点
         */
        static <K,V> TreeNode<K,V> rotateRight(TreeNode<K,V> root,
                                               TreeNode<K,V> p) {
            TreeNode<K,V> l, pp, lr;
            // l: 需要旋转节点的左节点
            // pp： 原爷爷的父节点节点
            // lr：需要旋转节点的左节点的右子树
            if (p != null && (l = p.left) != null) {// 要右旋的节点不为空以及要右旋的节点的左孩子不为空
                // 要右旋的节点的左孩子的右节点 赋给 要右旋节点的左孩子 节点为：lr
                if ((lr = p.left = l.right) != null)
                    // 设置lr和要右旋的节点的父子关系【之前只是爹认了孩子，孩子还没有答应，这一步孩子也认了爹】
                    lr.parent = p;
                // 将要右旋的节点的左孩子的父节点  指向 要右旋的节点的父节点（相当于左孩子提升了一层）
                // 此时如果父节点为空， 说明l 已经是顶层节点了，应该作为root 并且标为黑色
                if ((pp = l.parent = p.parent) == null)
                    (root = l).red = false;
                else if (pp.right == p)// 如果父节点不为空 并且 要右旋的节点是个右孩子
                    pp.right = l;// 设置l和父节点的父子关系【之前只是孩子认了爹，爹还没有答应，这一步爹也认了孩子】
                else// 要右旋的节点是个左孩子
                    pp.left = l; //爹也认了孩子
                // 要右旋的节点 作为 他左孩子的右节点
                l.right = p;
                // 要右旋的节点的父节点 指向 他的左孩子
                p.parent = l;
            }
            return root;
        }

        // TODO: 2022/11/11 变色，左旋，右旋

        /**
         * 插入平衡调整
         * 变色情况及规则：
         *      情况：当前节点的父节点是红色，叔叔节点也是红色。
         *          变换：
         *              1）父节点，叔叔节点变为黑色
         *              2） 爷爷节点，变为红色
         *              3） 把指针定义到爷爷节点，分析爷爷节点的变换规则（因为爷爷节点变为了红色，是否符合红黑书的规则未知）
         *
         * 左旋情况（父节点左旋）：当前父节点是红色，叔叔节点是黑色（或空），且当前节点是右子树。
         *
         * 右旋情况及规则（爷爷节点右旋）：
         *      当前节点是红色，叔叔节点是黑色（或空），且当前节点是左子树。
         *              1）父节点变为黑色
         *              2） 爷爷节点变为红色
         *              3）旋转
         *
         *
         *
         */

        /**
         * 红黑树的定义:
         *
         * 性质1. 节点是红色或黑色。
         *
         * 性质2. 根节点是黑色。
         *
         * 性质3.所有叶子都是黑色。（叶子是NUIL节点）
         *
         * 性质4. 每个红色节点的两个子节点都是黑色。（从每个叶子到根的所有路径上不能有两个连续的红色节点）
         *
         * 性质5. 从任一节点到其每个叶子的所有路径都包含相同数目的黑色节点。
         */

        /**
         * 红黑树插入节点后，需要重新平衡
         * @param root 当前根节点
         * @param x 新插入的节点
         * @return 返回重新平衡后的根节点
         */
        static <K,V> TreeNode<K,V> balanceInsertion(TreeNode<K,V> root,
                                                    TreeNode<K,V> x) {
            // 插入开始赋值为红节点
            x.red = true;
            /*
             * 这一步即定义了变量，又开起了循环，循环没有控制条件，只能从内部跳出
             * xp：当前节点的父节点、xpp：爷爷节点、xppl：左叔叔节点、xppr：右叔叔节点
             */
            for (TreeNode<K,V> xp, xpp, xppl, xppr;;) {
                // 如果父节点为空
                // 说明当前节点就是根节点，那么把当前节点标为黑色，返回当前节点
                if ((xp = x.parent) == null) { // L1
                    x.red = false;
                    return x;
                }
                // 父节点不为空 && 如果父节点为黑色 或者 【（父节点为红色 但是 爷爷节点为空）】
                else if (!xp.red || (xpp = xp.parent) == null) // L2
                    return root;
                // 判断父节点是左子树还是右子树
                if (xp == (xppl = xpp.left)) { // L3
                    // 进来说明是父是爷爷的左子树
                    // 如果右叔叔不为空 并且 为红色
                    if ((xppr = xpp.right) != null && xppr.red) { // L3_1
                        /**
                         * 变色情况
                         */
                        // 爷爷右子树是红色
                        // 父叔节点变黑
                        xppr.red = false;
                        xp.red = false;
                        // 爷爷变红
                        xpp.red = true;
                        // 运行到这里之后，就又会进行下一轮的循环了，将爷爷节点当做处理的起始节点
                        x = xpp;
                    }
                    else { // L3_2
                        // 进来说明是父是爷爷的左子树
                        // 如果右叔叔为空 或 黑色
                        if (x == xp.right) { // L3_2_1
                            // 当前节点是父节点的右子树
                            /**
                             * 父节点左旋
                             */
                            root = rotateLeft(root, x = xp);
                            xpp = (xp = x.parent) == null ? null : xp.parent; // 获取爷爷节点
                        }
                        if (xp != null) {// 如果父节点不为空 // L3_2_2
                            xp.red = false; // 父节点 置为黑色
                            if (xpp != null) {// 如果爷爷节点不为空
                                xpp.red = true; // 爷爷节点置为红色
                                root = rotateRight(root, xpp); //爷爷节点右旋
                            }
                        }
                    }
                }
                // 父节点是爷爷节点的右子树
                else { // L4
                    if (xppl != null && xppl.red) {// 如果左叔叔是红色 // L4_1
                        xppl.red = false; // 左叔叔置为 黑色
                        xp.red = false; // 父节点置为黑色
                        xpp.red = true; // 爷爷置为红色
                        x = xpp; // 运行到这里之后，就又会进行下一轮的循环了，将爷爷节点当做处理的起始节点
                    }
                    else { // 如果左叔叔为空或者是黑色 // L4_2
                        if (x == xp.left) { // 如果当前节点是个左孩子 // L4_2_1
                            root = rotateRight(root, x = xp); // 针对父节点做右旋
                            xpp = (xp = x.parent) == null ? null : xp.parent; // 获取爷爷节点
                        }
                        if (xp != null) {// 如果父节点不为空 // L4_2_4
                            xp.red = false;// 父节点置为黑色
                            if (xpp != null) {// 如果爷爷节点不为空
                                xpp.red = true; // 爷爷节点置为红色
                                root = rotateLeft(root, xpp); // 针对爷爷节点做左旋
                            }
                        }
                    }
                }
            }
        }

        /**
         * 删除要复杂些，不能从颜色的层面看所有情况，需要从删除节点的儿子子节点情况来看，同样分为三类
         * 有二个子节点
         * 有一个子节点
         * 没有子节点
         * @param root 根节点
         * @param x 替换节点
         */
        /*
            1） 先判断当前节点是空或根节点返回根节点
            2） 父节点为空 此节点为根节点，变黑返回
            3） 此节点是红色，变黑 返回根节点
            进入循环{
            4） 此节点为黑色，且为左孩子
                4.1） 兄弟节点为红色：兄变黑，父变红，父节点左旋
                4.2） 兄弟为空：接着循环（此时父节点作为当前节点）
                4.3） 兄弟为黑色（需要看兄弟孩子节点决定是否旋转）
                    4.3.1）兄弟儿子都没有红色节点（可以为空节点）：兄弟变红，父节点作为当前节点接着循环
                    4.3.2）兄弟儿子有红色节点（可以两个都红）：
                        4.3.2.1）与兄弟同边儿子节点（兄弟是右儿子，兄弟儿子也是右儿子）是黑色：转换为与兄同边，兄弟节点右旋
                        4.3.2.2）兄弟节点不等于空：父节点颜色赋值给兄弟节点，兄弟右孩子变黑
                        4.3.2.3）父节点不为空：父节点变黑，左旋
                        根节点作为当前节点，接着循环（说明调整结束）
                   }
         */
        static <K,V> TreeNode<K,V> balanceDeletion(TreeNode<K,V> root,
                                                   TreeNode<K,V> x) {
            /**
             * x.parent ：是替换了（交换后）删除节点位置的节点呀
             */
            for (TreeNode<K,V> xp, xpl, xpr;;)  {
                if (x == null || x == root)// L1
                    return root;
                else if ((xp = x.parent) == null) {// L2 当前数组位置只有删除节点本身
                    x.red = false;
                    return x;
                }
                else if (x.red) {// L3 替换节点为红色
                    x.red = false;
                    return root;
                }
                /**
                 * x 颜色是黑色
                 */
                else if ((xpl = xp.left) == x) {// L4  //替换节点为父节点的左孩子
                    if ((xpr = xp.right) != null && xpr.red) {// L4.1 // 兄弟节点为红色
                        // 兄弟变黑 父变红
                        xpr.red = false;
                        xp.red = true;
                        // 父节点左旋
                        root = rotateLeft(root, xp);
                        // 交换后 （对于当前位置来说）xpr 为兄弟节点（左旋变为了父节点的右孩子作为兄弟节点）
                        xpr = (xp = x.parent) == null ? null : xp.right;
                    }
                    /*
                    如果兄弟为红色 则 xpr 为兄弟左孩子（其跟替换节点成了兄弟）
                     */
                    if (xpr == null)// L4.2 兄弟节点为空
                        // 移动当前节点到xp
                        x = xp;
                    /**
                     *                       进入下一个循环
                     */
                    else {// L4.3 兄弟（黑色）不为空
                        /**
                         * sl：兄弟节点的左子树
                         * sr：兄弟节点的右子树
                         */
                        TreeNode<K,V> sl = xpr.left, sr = xpr.right;
                        if ((sr == null || !sr.red) &&
                            (sl == null || !sl.red)) { // L4.3.1 // 兄弟节点两个孩子节点都为黑 或 两个空 或 一个空另一个黑
                            // 没有红色的，兄弟节点变红色
                            xpr.red = true;
                            // 替换节点指向父节点
                            x = xp;
                            /**
                             *                       进入下一个循环
                             */
                        }
                        else {// L4.3.2 // 兄弟节点两个孩子节点有一个红或两个红
                            if (sr == null || !sr.red) {// L4.3.2.1 右子树不是红（左红右黑 左红右空）
                                /**
                                 * 与兄同边儿不为红，转换为与兄同边
                                 */
                                if (sl != null)
                                    // 左孩子变黑
                                    sl.red = false;
                                xpr.red = true; // 兄弟变红
                                root = rotateRight(root, xpr); // 兄弟右旋
                                xpr = (xp = x.parent) == null ?
                                    null : xp.right;
                            }
                            if (xpr != null) {// L4.3.2.2 兄弟不等于空
                                xpr.red = (xp == null) ? false : xp.red; // 将父节点颜色給兄弟
                                if ((sr = xpr.right) != null) // 兄弟右孩子不为空
                                    sr.red = false; // 兄弟右孩子变黑
                            }
                            if (xp != null) { // L4.3.2.3
                                xp.red = false; // 父变黑
                                root = rotateLeft(root, xp);
                            }
                            x = root;
                        }
                    }
                }
                else { // symmetric  // L5 // 替换节点为父节点的右孩子
                    if (xpl != null && xpl.red) { // L5.1 兄弟为红色
                        xpl.red = false;
                        xp.red = true;
                        root = rotateRight(root, xp);
                        xpl = (xp = x.parent) == null ? null : xp.left;
                    }
                    if (xpl == null) // L5.2 兄弟为空继续循环
                        x = xp;
                    else { // L5.3 兄弟不为空哦
                        TreeNode<K,V> sl = xpl.left, sr = xpl.right;
                        if ((sl == null || !sl.red) &&
                            (sr == null || !sr.red)) {// L5.3.1 兄弟孩子没有红色
                            // 兄弟变色，父当替换节点继续循环
                            xpl.red = true;
                            x = xp;
                        }
                        else {// L5.3.2 兄弟孩子有红色
                            if (sl == null || !sl.red) {// L5.3.2.1 兄弟左孩子不为红色
                                if (sr != null)
                                    sr.red = false;
                                xpl.red = true;
                                root = rotateLeft(root, xpl);
                                xpl = (xp = x.parent) == null ?
                                    null : xp.left;
                            }
                            if (xpl != null) {// L5.3.2.2 兄弟不为空
                                xpl.red = (xp == null) ? false : xp.red;
                                if ((sl = xpl.left) != null)
                                    sl.red = false;
                            }
                            if (xp != null) {// L5.3.2.3 父节点不为空，变黑右旋
                                xp.red = false;
                                root = rotateRight(root, xp);
                            }
                            x = root;
                        }
                    }
                }
            }
        }

        /**
         * Recursive invariant check
         */
        static <K,V> boolean checkInvariants(TreeNode<K,V> t) {
            TreeNode<K,V> tp = t.parent, tl = t.left, tr = t.right,
                tb = t.prev, tn = (TreeNode<K,V>)t.next;
            if (tb != null && tb.next != t)
                return false;
            if (tn != null && tn.prev != t)
                return false;
            if (tp != null && t != tp.left && t != tp.right)
                return false;
            if (tl != null && (tl.parent != t || tl.hash > t.hash))
                return false;
            if (tr != null && (tr.parent != t || tr.hash < t.hash))
                return false;
            if (t.red && tl != null && tl.red && tr != null && tr.red)
                return false;
            if (tl != null && !checkInvariants(tl))
                return false;
            if (tr != null && !checkInvariants(tr))
                return false;
            return true;
        }
    }

}
