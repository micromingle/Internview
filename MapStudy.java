/**
 * Created by HP on 2017/7/3.
 */

public class MapStudy {

   /* 1 HashMap 和hashStable 差别
       1）  hashmap 不支持同步， hasttable支持同步
       2）  hashtable 不允许key 和value 为null ， hashtable 允许
     2 rehash
       当数组扩容时需要扩容，扩容以后需要重新hash

     3 最早插入的Entry 在链表的最末端

     4 HashSet 的内部是HashMap 实现的

     5 Map 的Size 和 capacity 区别
       1）size 当前key-value 的个数
       2）capacity  map 的容量

     5 LinkedHashMap
       1）如果是accessOrder, 迭代的顺序是 least recently used  --> most recently used
          也就是最新取的放在队尾
       2)大部分情况下LinkedhashMap 效率比较低,但是迭代的性能比较高，因为LinkedHashMap 的性能不受 capacity 的影响
         而是受size 的影响
       3) LinkedMap 是如何保证迭代顺序的
          // 网上暂时没有答案

╔══════════════╦═════════════════════╦═══════════════════╦═════════════════════╗
║   Property   ║       HashMap       ║      TreeMap      ║     LinkedHashMap   ║
╠══════════════╬═════════════════════╬═══════════════════╬═════════════════════╣
║              ║  no guarantee order ║ sorted according  ║                     ║
║   Order      ║ will remain constant║ to the natural    ║    insertion-order  ║
║              ║      over time      ║    ordering       ║                     ║
╠══════════════╬═════════════════════╬═══════════════════╬═════════════════════╣
║  Get/put     ║                     ║                   ║                     ║
║   remove     ║         O(1)        ║      O(log(n))    ║         O(1)        ║
║ containsKey  ║                     ║                   ║                     ║
╠══════════════╬═════════════════════╬═══════════════════╬═════════════════════╣
║              ║                     ║   NavigableMap    ║                     ║
║  Interfaces  ║         Map         ║       Map         ║         Map         ║
║              ║                     ║    SortedMap      ║                     ║
╠══════════════╬═════════════════════╬═══════════════════╬═════════════════════╣
║              ║                     ║                   ║                     ║
║     Null     ║       allowed       ║    only values    ║       allowed       ║
║ values/keys  ║                     ║                   ║                     ║
╠══════════════╬═════════════════════╩═══════════════════╩═════════════════════╣
║              ║   Fail-fast behavior of an iterator cannot be guaranteed      ║
║   Fail-fast  ║ impossible to make any hard guarantees in the presence of     ║
║   behavior   ║           unsynchronized concurrent modification              ║
╠══════════════╬═════════════════════╦═══════════════════╦═════════════════════╣
║              ║                     ║                   ║                     ║
║Implementation║      buckets        ║   Red-Black Tree  ║    double-linked    ║
║              ║                     ║                   ║       buckets       ║
╠══════════════╬═════════════════════╩═══════════════════╩═════════════════════╣
║      Is      ║                                                               ║
║ synchronized ║              implementation is not synchronized               ║
╚══════════════╩═══════════════════════════════════════════════════════════════╝
       6  ConcurrentHashmap
          1)原理一个Hashmap 默认分成16个桶、Segment，每个Segment 就是一个hashtable,锁的时候锁其中的一个segment 锁的粒度比较细
           1) 写的时候需要锁,但只锁一部分，
           2) 读的时候不需要锁
           3）size 和 containsValue 的时候需要全部锁
           4） 迭代的时候，即使元素的值被改变不会抛出异常，而是会取到最新值原因是因为Entry 定义的值
           1. static final class HashEntry<K,V> {
                         final K key;
                        final int hash;
                        volatile V value;
                        final HashEntry<K,V> next;
                      }
            如上由于value 值是volatile 的所以 即使值被改变就可以拿到最新的，
            其他值是final 的，决定了，Entry 的删除和添加不能在中间或者结尾，只能在头部
            对于put操作，可以一律添加到Hash链的头部。但是对于remove操作，可能需要从中间删除一个节点，
            这就需要将要删除节点的前面所有节点整个复制一遍，最后一个节点指向要删除结点的下一个结点。
            这在讲解删除操作时还会详述。为了确保读操作能够看到最新的值，
            将value设置成volatile，这避免了加锁。
            5）对于由于初始化是2的n次方 所以哈希冲突很严重，所以要再hash

            6） volatile
               volatile关键字就是提示VM：对于这个成员变量不能保存它的私有拷贝，而应直接与共享成员变量交互。

            7)Java的serialization提供了一种持久化对象实例的机制。当持久化对象时，可能有一个特殊的对象数据成员，我们不想
               用serialization机制来保存它。为了在一个特定对象的一个域上关闭serialization，可以在这个域前加上关键字transient。
               transient是Java语言的关键字，用来表示一个域不是该对象串行化的一部分。当一个对象被串行化的时候，
               transient型变量的值不包括在串行化的表示中，然而非transient型的变量是被包括进去的。
         7  SparseArray 和 ArrayMap
                SparseArray
              1)SparseArray key 是int数组维护，value 是object 数组维护
              2）设计意图：优点：1)比HashMap 省空间，HashMap 不支持基础类型，需要自动Box,带来性能消耗
                            而且一个Entry 所占的内存比较大
                              2) 当value 被删除时，并不直接去除节点缩小数组，认识标志为DELETED 留待重用，或者等待gc时
                              再回收；
                         缺点：Hashmap 的查找时间是o(1) 而SpareArray 用的是二分法，查找是O(lgn),查询速度比较慢
                         空间换时间
              3）遗留问题二分法的排序是在哪里排序的？

                ArrayMap
               1)优点和SparseArray 大体相近，另外可以避免rehash 带来的开销
			   
			   
         8  扩展// Universal Image Loader 中的
		    //  


       */


}
