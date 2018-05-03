import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.CountDownLatch;

/**
 * Created by HP on 2017/6/23.
 */

public class Inter {
	
	  一 网络类
	  
	  
          1 Tcp 的三次握手和四次挥手，以及等待两个msl周期

               三次握手： 标志位：SYN，ACK, FIN,URG; 发出序列号sn; 应答序列号sn;

              客户端发送SYN（进入SYN_SENT状态） ,

              服务端应答ACK（进入SYN_REVD状态）,分配资源，同时向客户端发送SYN

              客户端收到服务端SYN,回答ACK， 连接建立（进入ESTABLISHED状态）

              四次挥手： 客户端发送FIN, 表示数据发送完成，进入FINISH_WAIT1状态

               服务端收到FIN,由于可能还有数据要发送，不能立刻结束，所以发送ACK报文
               进入（CLOSE_WAIT状态）

               此时客户端收到ACK以后，进入FINISH_WAIT2的状态

               等到服务端发送完成，发送FIN报文，进入LAST_ACK状态

                客户端收到FIN以后，发送ACK,进入TIME_WAIT状态，等待2msl以后关闭

                服务端收到ACK关闭连接

               为什么三次握手：
			   
               为超时而考虑：
               假如只要两次握手的话，客户端只要发送请求，服务端收到连接，
               连接就会建立，正常情况下是ＯＫ的，但是以下情况会导致建立无用连接

　　　　　　　　　假如客户端发送了一个请求超时了，很久没到达服务端，
            　　　　　　　　　此时客户端重新发送了一个请求，正常得到响应，连接建立
　　　　　　　　 　过了一会儿，那个超时的请求到达了服务端。
            　　　　　　　　　假如，如果是两次握手的话，
            　　　　　　　　　此时又会建立一个连接（这是一个无用的连接）
            　　　　　　　　　假若是三次握手的话，当超时的请求来到服务端，服务端响应，
            　　　　　　　　　此时客户端处理，发现他是失效的响应，根本不会发送ＡＣＫ，
            　　　　　　　　　连接就不会建立

            为什么四次挥手：
			
               由于连接是双工的，两次握手只能结束一端的连接，所以要四次
　　　　　　　　 　如果和三次挥手的过程对比的话，可以发现建立连接的时候，服务端响应ＳＹＮ和ＡＣＫ是同时发送的，而挥手的时候，一次只能
　　　　　　　　　 发送一个指令，所以多了一次。
            　　　　　　　　　
            等待两个周期：
               为超时而考虑
　　　　　　　　　服务端进入ＬＡＳＴ＿ＡＣＫ以后，客户端收到请求，发送ＡＣＫ以后，假如客户端立即关闭连接，但是他发送的ＡＣＫ丢失了，服务端没有收到
　　　　　　　　　，这样服务端就无法关闭连接。假如等待两个ｍｓｌ周期，服务端发现没有响应，就会重新发送ＦＩＮ指令，再次进入ＬＡＳＴ＿ＡＣＫ状态，
            　　此时客户端可以响应，后台手段响应正常关闭

             1） 滑动窗口控制 怎么验证数据顺序是否在正确：
			 
			     参考地址：https://coolshell.cn/articles/11564.html
				 
				           https://www.zhihu.com/question/32255109
						   
				 1)TCP滑动窗口分为接受窗口，发送窗口

				   滑动窗口协议是传输层进行流控的一种措施，接收方通过通告发送方自己的窗口大小，从而控制发送方的发送速度，
				 
				   从而达到防止发送方发送速度过快而导致自己被淹没的目的。
				   
				       一 是期望接收到的下一字节的序号n，该n代表接收方已经接收到了前n-1字节数据，此时如果接收方收到第n+1
				   
				   字节数据而不是第n字节数据，接收方是不会发送序号为n+2的ACK的。举个例子，假如接收端收到1-1024字节，它会发送一个确认号为1025的ACK,
				   
				   但是接下来收到的是2049-3072，它是不会发送确认号为3072的ACK,而依旧发送1025的ACK。
				   
				      二  是当前的窗口大小m，如此发送方在接收到ACK包含的这两个数据后就可以计算出还可以发送多少字节的数据给对方，
					 
				  假定当前发送方已发送到第x字节，则可以发送的字节数就是y=m-(x-n).这就是滑动窗口控制流量的基本原理
				  
				  TCP的Window是一个16bit位字段，它代表的是窗口的字节容量，也就是TCP的标准窗口最大为2^16-1=65535个字节。
				  
				  另外在TCP的选项字段中还包含了一个TCP窗口扩大因子，option-kind为3，option-length为3个字节，option-data取值范围0-14。
				  
				  窗口扩大因子用来扩大TCP窗口，可把原来16bit的窗口，扩大为31bit。


				  
				  
				     三  TCP的滑动窗口主要有两个作用，一是提供TCP的可靠性，二是提供TCP的流控特性。
				   
				  同时滑动窗口机制还体现了TCP面向字节流的设计思路。TCP 段中窗口的相关字段。
					
					TCP是双工的协议，会话的双方都可以同时接收、发送数据。TCP会话的双方都各自维护一个“发送窗口”和一个“接收窗口”。
					
					其中各自的“接收窗口”大小取决于应用、系统、硬件的限制（TCP传输速率不能大于应用的数据处理速率）。
					
					各自的“发送窗口”则要求取决于对端通告的“接收窗口”，要求相同。
					
					四  滑动窗口实现面向流的可靠性
					
					    1）最基本的传输可靠性来源于“确认重传”机制。
					
					    2）TCP的滑动窗口的可靠性也是建立在“确认重传”基础上的。
						
						3）发送窗口只有收到对端对于本段发送窗口内字节的ACK确认，才会移动发送窗口的左边界。
						
						4）接收窗口只有在前面所有的段都确认的情况下才会移动左边界。当在前面还有字节未接收但收到后面字节的情况下，
						
						   窗口不会移动，并不对后续字节确认。以此确保对端会对这些数据重传。






			 
			 
         2  https 握手过程
	  
             1) 客户端发送请求 ，服务端产生公钥和私钥，将私钥发送给客户端
             2) 客户端随机产生秘钥，用公钥加密 产生的秘钥，发送给服务端
             3) 服务端用私钥解密，得出秘钥，用得出的秘钥进行加密信息，进行传输
			 
			
			 
		 3  开源框架介绍Retrofit2 介绍
			  一： Retrofit 组成部分，使用先定义一个接口
			  1) 注解部分 包含请求方法如POST,GET,查询参数 QUERY  请求头HEADERS（注解是RunTime类型）
			  2) 网络请求部分  使用okhttp
			  3) 请求处理部分，加入converter，支持gson,xml,protobuffer
			  4) RequestBuilder 类,用于构建请求
			  5) Response 接收返回
			  6) Retrofit 初始化请求的各类参数，使用生成器模式 builder 模式
			  调用过程  通过动态代理生成对象newProxyInstance , 调用某一个方法时
			  调用invoke 的方法，传入Method 参数，通过method 的各类注解获得请求的各个参数
			  并生成一个叫ServiceMethod的对象，并缓存，ServiceMethod 中有一个toRequest 方法
			  生成RequestBuilder 对象，并把请求加入okhttp的请求队列

              二: Okhttp的介绍
             1)复用连接池  OkHttpClient 保存着5个Socket 长连接,每个长连接存活时间5分钟，
                         每个连接都维护者一个引用计数，当该连接被使用时，计数加1，当连接释放时
                         引用计数减一，每次添加新连接时，会执行cleanup操作
						 
						 我们可以总结，okhttp使用了类似于引用计数法与标记擦除法的混合使用，当连接空闲或者释放时，
						 StreamAllocation的数量会渐渐变成0，
						 从而被线程池监测到并回收，这样就可以保持多个健康的keep-alive连接，Okhttp的黑科技就是这样实现的。
						 
             2)缓存      带有本地的响应缓存，用LruDiskCache 实现把url做md5处理以后用作key,每次请求
                         会先获取本地响应缓存，以后再根据请求的请求头进行处理，是决定使用该缓存还是
                          是直接重新请求云端；// 缓存清除功能，每次set/get的调用
						  HTTP缓存基础知识
						  
						 1.  Expires
						 
						     表示到期时间，一般用在response报文中，当超过此事件后响应将被认为是无效的而需要网络连接，反之而是直接使用缓存
                             Expires: Thu, 12 Jan 2017 11:01:33 GMT
							 
					     2.  Cache-Control

                             相对值，单位是秒，指定某个文件被续多少秒的时间，从而避免额外的网络请求。比expired更好的选择，
							 它不用要求服务器与客户端的时间同步，也不用服务器时刻同步修改配置Expired中的绝对时间，而且它的优先级比Expires更高。
							 比如简书静态资源有如下的header，表示可以续31536000秒，也就是一年。
							 
						 3.  修订文件名(Reving Filenames)

                             如果我们通过设置header保证了客户端可以缓存的，而此时远程服务器更新了文件如何解决呢？我们这时可以通过
							 修改url中的文件名版本后缀进行缓存，比如下文是又拍云的公共CDN就提供了多个版本的JQuery

                             upcdn.b0.upaiyun.com/libs/jquery/jquery-2.0.3.min.js
							 
						 4. 条件GET请求(Conditional GET Requests)与304

                            如缓存果过期或者强制放弃缓存，在此情况下，缓存策略全部交给服务器判断，客户端只用发送条件get请求即可，
							如果缓存是有效的，则返回304 Not Modifiled，否则直接返回body。
							
							4.1. Last-Modified-Date:

                            客户端第一次网络请求时，服务器返回了

                            Last-Modified: Tue, 12 Jan 2016 09:31:27 GMT
                            客户端再次请求时，通过发送

                            If-Modified-Since: Tue, 12 Jan 2016 09:31:27 GMT
                            交给服务器进行判断，如果仍然可以缓存使用，服务器就返回304

                           4.2. ETag

                            ETag是对资源文件的一种摘要，客户端并不需要了解实现细节。当客户端第一请求时，服务器返回了

                            ETag: "5694c7ef-24dc"
                            客户端再次请求时，通过发送

                            If-None-Match:"5694c7ef-24dc"
                            交给服务器进行判断，如果仍然可以缓存使用，服务器就返回304
							
						 5. 其它标签

                            no-cache/no-store: 不使用缓存，no-cache指令的目的是防止从缓存中返回过期的资源。客户端发送的请求中如果包含no-cache指令的话，
							表示客户端将不会接受缓存过的相应，于是缓存服务器必须把客户端请求转发给源服务器。服务器端返回的相应中包含no-cache
							指令的话那么缓存服务器不能对资源进行缓存。
                            only-if-cached: 只使用缓存
                            Date: The date and time that the message was sent
                            Age: The Age response-header field conveys the sender's estimate of the amount of time since the response (or its revalidation) 
							was generated at the origin server. 说人话就是CDN反代服务器到原始服务器获取数据延时的缓存时间

                         DiskCache内在实现 
						
						    在学习之前，我们要了解一下LinkedHashMap。LinkedHashMap继承于HashMap。
							
							在get元素时，如果设置accessOrder为true时，通过调用如下回调移动元素到链尾，这里特别强调移动，如果这个元素本身已经在链表中，那它将只会移动，而不是新建

                            // move node to last
                            void afterNodeAccess(Node<K,V> e)
							
                            综上，当你反复对元素进行get/put操作时，经常使用的元素会被移动到tail中，而长期不用的元素会被移动到head
							
							                获取	 查找	添加/删除	空间	
                             ArrayList	    O(1)	 O(1)	  O(N)	   O(N)
                            LinkedList      O(N)	 O(N)	  O(1)	   O(N)
                            HashMap	O(N/Bucket_size)	O(N/Bucket_size)	O(N/Bucket_size)	O(N)
							
 
                        总结：
					     1  OkHttp通过对文件进行了多次封装，实现了非常简单的I/O操作
                         2  OkHttp通过对请求url进行md5实现了与文件的映射，实现写入，删除等操作
                         3  OkHttp内部维护着清理线程池，实现对缓存文件的自动清理


							 
              3)任务调度  维护一个核心数为0，无最大线程数量的线程池，当工作完成后，线程池会在60s后相继关闭所有线程
                          默认最多64个请求，内部会用维护两个双端队列，一个是正在执行的队列，一个是正在等待的队列，
                          一个请求进来以后，若请求数已经达到最大，则将请求放入等待队列，反之就立即执行，
                          并放入正在执行的对列，当一个请求执行完成以后，会根据当前情况，把正在等待的请求拿出来执行
						 
						 通过上述的分析，我们知道了：

                         1 OkHttp采用Dispatcher技术，类似于Nginx，与线程池配合实现了高并发，低阻塞的运行
                         2 Okhttp采用Deque作为缓存，按照入队的顺序先进先出
                         3 OkHttp最出彩的地方就是在try/finally中调用了finished函数，可以主动控制等待队列的移动，
						   而不是采用锁或者wait/notify，极大减少了编码复杂性


						 
				  1）	SSL：（Secure Socket Layer，安全套接字层），位于可靠的面向连接的网络层协议和应用层协议之间的一种协议层。SSL通过互相认证、
					    使用数字签名确保完整性、使用加密确保私密性，以实现客户端和服务器之间的安全通讯。该协议由两层组成：SSL记录协议和ＳＳＬ握手协议。

                  2）  TLS：(Transport Layer Security，传输层安全协议)，
				       用于两个应用程序之间提供保密性和数据完整性。该协议由两层组成：TLS记录协议和TLS握手协议。
					
				  3）  Inteceptor 拦截器很强大，它可以用来监控log，修改请求，修改结果，甚至是对用户透明的GZIP压缩。
				  
					  dns 寻址算法，HTTP代理的本质是改Header信息，当你访问HTTP/HTTPS服务时，本质是明文向跳板发送如下raw，远程服务器帮你完成dns与请求操作，
					  比如HTTPS请求源码就详细的解释了发送的内容是非加密的，下面是我实际抓包的内容
					  Socket 超时时间因素：
					  在现代浏览器中，一般同时开启6～8个keepalive connections的socket连接，并保持一定的链路生命，当不需要时再关闭；而在服务器中，
					  一般是由软件根据负载情况(比如FD最大值、Socket内存、超时时间、栈内存、栈数量等)决定是否主动关闭。
				  
				  //keepAlive 缺点 
				  
				  当然keepalive也有缺点，在提高了单个客户端性能的同时，复用却阻碍了其他客户端的链路速度，具体来说如下

                  1 根据TCP的拥塞机制，当总水管大小固定时，如果存在大量空闲的keepalive connections（我们可以称作僵尸连接或者泄漏连接），
				    其它客户端们的正常连接速度也会受到影响，这也是运营商为何限制P2P连接数的道理
                  2 服务器/防火墙上有并发限制，比如apache服务器对每个请求都开线程，导致只支持150个并发连接（数据来源于nginx官网），
				    不过这个瓶颈随着高并发server软硬件的发展（golang/分布式/IO多路复用）将会越来越少
                  3 大量的DDOS产生的僵尸连接可能被用于恶意攻击服务器，耗尽资源
				  
				  //下文假设服务器是经过专业的运维配置好的，它默认开启了keep-alive，并不主动关闭连接
				  
				  
			     4） DNS查询算法：
				    
					 DNs共有的种查询方法，分别是递归查询和迭代查询。
					 
					 1 递归查询即由该计算机指定的DNS服务器代表客户端向其他DNs服务器进行查询，
					   以便完全解析该域名，并将结果返回至客户端。
					   
					 2 选代查询即由该计算指定的DNs服务器向客户端返问一个可以解析该域名的其他DNs服务器地址，
					   客户端再继续向其他DNs服务器进行查询。下面分别对这两种解析方法进行详细介绍。
					   
				 5)  最常用的是域名地址、IP地址和MAC地址，分别对应应用层、网络层、物理层。
				 
				 6)  通过ip 地址获得 mac 地址
				 
				     如果主机A和主机B是一个局域网的话：主机Aip数据包的包头部分的目的地址就是主机B 的mac地址
                     
                      ① 主机A发出ARP请求，请求帧中的数据部分包括发送者MAC地址00-0C-04-18-19-aa、发送者IP地址172.16.20.20和目标MAC地址，这里全部填充0，
					     因为它未知（这正是ARP要询问的），目标IP地址是172.16.20.5。
                      ② 在请求帧的帧头部分，目的MAC地址是广播地址，因此所有收到的站点（其中就包括主机B）都打开这个帧查看其数据部分的内容。
                      ③ 只有符合目标IP地址172.16.20.5的主机（主机B）回答这个ARP请求，其他站点则忽略这个请求。
                      ④ 主机B把自己的MAC地址写入“目标地址”字段中，送给主机A。
                         主机A通过ARP的操作得到了主机B的MAC地址，可以继续完成它的封装过程，从而最终执行了Ping的操作。
                          ARP请求者收到应答后，会在自己的缓存中构建一个ARP表，将得到的地址信息缓存起来，以备将来和同一目
						  的节点通信时直接在ARP表中查找，避免了多次的广播请求。	
 
                     如果主机A和主机B不在一个局域网的话： 主机Aip数据包的包头部分的目的地址就不是主机B 的mac地址，而是路由器的mac地址
					 
					    由于主机B位于路由器的另外一侧，因此主机B要想收到主机A发出的以太网帧必须通过路由器转发，那么路由器是否会转发呢？
						答案是否定的。路由器在收到某个以太网帧后首先检查其目的MAC，而这里假设A发出的帧中的目的MAC是B的网卡地址，
						路由器从Ethernet 0接口收到该帧后，查看目的MAC地址，发现它不是自己的MAC地址，从而将其丢弃掉。由此看来，
						位于不同子网的主机之间在通信时，目的MAC地址不能是目标主机的物理地址。
						
						实际上，不同子网之间的主机通信要经过路由过程，这里就是需要路由器A进行转发。因此，主机A发现目标主机与自己不在同
						一个子网中时就要借助于路由器。它需要把数据帧送到路由器上，然后路由器会继续转发至目标节点。在该例中，主机A发现
						主机B位于不同子网时，它必须将数据帧送到路由器上，这就需要在帧头的“目的地址”字段上写入路由器接口Ethernet 0
						的MAC地址。因此，主机A需要通过ARP询问路由器Ethernet 0 接口的MAC地址。
						
                        这里仍然是两个操作过程，一个是ARP请求；另一个是ARP应答。不过在ARP的请求帧中，目标IP地址将是路由器Ethernet
						0接口的IP地址，这个地址实际上就是子网172.16.10.0/24中主机的默认网关。路由器收到ARP请求后回答自己Ethernet 0接口
						的MAC地址，这样主机A就获得了其默认网关的MAC地址。主机A构建完整的数据帧并将其发送给到路由器。路由器收到主机A的数据后，
						根据路由表的指示将从另一接口Ethernet 1把数据发送给主机B。同样，在发送前路由器也要封装2层帧头，也需要知道主机B的MAC地址，
						路由器也是通过ARP协议来获得B的MAC地址的。
						
						
						综合以上两种情况，主机A的完整操作过程如下：
                        主机A首先比较目的IP地址与自己的IP地址是否在同一子网中，如果在同一子网，则向本网发送ARP广播，
						获得目标IP所对应的MAC地址；如果不在同一子网，就通过ARP询问默认网关对应的MAC地址。
				
				4    Http1.0、Spdy和Http2.0的对比 
				
				  1） Http 1.1 和Http1.0 区别：

                    1、缓存处理，在HTTP1.0中主要使用header里的If-Modified-Since,Expires来做为缓存判断的标准，HTTP1.1则引入了更多的缓存控制策略例如Entity tag，
					
				  	If-Unmodified-Since, If-Match, If-None-Match等更多可供选择的缓存头来控制缓存策略。

                    2、带宽优化及网络连接的使用，HTTP1.0中，存在一些浪费带宽的现象，例如客户端只是需要某个对象的一部分，而服务器却将整个对象送过来了，并且不支持断点续传功能，
				      HTTP1.1则在请求头引入了range头域，它允许只请求资源的某个部分，即返回码是206（Partial Content），这样就方便了开发者自由的选择以便于充分利用带宽和连接。

                    3、错误通知的管理，在HTTP1.1中新增了24个错误状态响应码，如409（Conflict）表示请求的资源与资源的当前状态发生冲突；410（Gone）表示服务器上的某个资源被永久性的删除。

                    4、Host头处理，在HTTP1.0中认为每台服务器都绑定一个唯一的IP地址，因此，请求消息中的URL并没有传递主机名（hostname）。但随着虚拟主机技术的发展，在一台物理服务
				      器上可以存在多个虚拟主机（Multi-homed Web Servers），并且它们共享一个IP地址。HTTP1.1的请求消息和响应消息都应支持Host头域，
					  且请求消息中如果没有Host头域会报告一个错误（400 Bad Request）。

                    5、长连接，HTTP 1.1支持长连接（PersistentConnection）和请求的流水线（Pipelining）处理，在一个TCP连接上可以传送多个HTTP请求和响应，
				     减少了建立和关闭连接的消耗和延迟，在HTTP1.1中默认开启Connection： keep-alive，一定程度上弥补了HTTP1.0每次请求都要创建连接的缺点。
					 
			      2） HTTP1.0和1.1现存的一些问题
				  
				     1、上面提到过的，HTTP1.x在传输数据时，每次都需要重新建立连接，无疑增加了大量的延迟时间，特别是在移动端更为突出。

                     2、HTTP1.x在传输数据时，所有传输的内容都是明文，客户端和服务器端都无法验证对方的身份，这在一定程度上无法保证数据的安全性。

                     3、HTTP1.x在使用时，header里携带的内容过大，在一定程度上增加了传输的成本，并且每次请求header基本不怎么变化，尤其在移动端增加用户流量。

                     4、虽然HTTP1.x支持了keep-alive，来弥补多次创建连接产生的延迟，但是keep-alive使用多了同样会给服务端带来大量的性能压力，并且对于单个文件被不
					 
					    断请求的服务(例如图片存放网站)，keep-alive可能会极大的影响性能，因为它在文件被请求之后还保持了不必要的连接很长时间。
				  
				  3） HTTPS与HTTP的一些区别
					
					 1、HTTPS协议需要到CA申请证书，一般免费证书很少，需要交费。

                     2、HTTP协议运行在TCP之上，所有传输的内容都是明文，HTTPS运行在SSL/TLS之上，SSL/TLS运行在TCP之上，所有传输的内容都经过加密的。

                     3、HTTP和HTTPS使用的是完全不同的连接方式，用的端口也不一样，前者是80，后者是443。

                     4、HTTPS可以有效的防止运营商劫持，解决了防劫持的一个大问题。
					 
				  5）SPDY 相对于 HTTPS和HTTP两者优点于一体的传输协议，主要解决：

                     1、降低延迟，针对HTTP高延迟的问题，SPDY优雅的采取了多路复用（multiplexing）。多路复用通过多个请求stream共享一个tcp连接的方式，解决了HOL blocking的问题，降低了延迟同时提高了带宽的利用率。

                     2、请求优先级（request prioritization）。多路复用带来一个新的问题是，在连接共享的基础之上有可能会导致关键请求被阻塞。SPDY允许给每个request设置优先级，这样重要的请求就会优先得到响应。
					     比如浏览器加载首页，首页的html内容应该优先展示，之后才是各种静态资源文件，脚本文件等加载，这样可以保证用户能第一时间看到网页内容。

                     3、header压缩。前面提到HTTP1.x的header很多时候都是重复多余的。选择合适的压缩算法可以减小包的大小和数量。

                     4、基于HTTPS的加密协议传输，大大提高了传输数据的可靠性。

                     5、服务端推送（server push），采用了SPDY的网页，例如我的网页有一个sytle.css的请求，在客户端收到sytle.css数据的同时，服务端会将sytle.js的文件推送给客户端，
					 
					    当客户端再次尝试获取sytle.js时就可以直接从缓存中获取到，不用再发请求了。
				  
				  6）SPDY和Http2.0 大体相同 主要以下区别
				  
				     1）新的二进制格式（Binary Format）

                        HTTP1.x的解析是基于文本。基于文本协议的格式解析存在天然缺陷，文本的表现形式有多样性，要做到健壮性考虑的场景必然很多，二进制则不同，
						只认0和1的组合。基于这种考虑HTTP2.0的协议解析决定采用二进制格式，实现方便且健壮。
						
				     2） 允许明文传输
					 
					 
				   7）Http SPDY Https TSL TCP 运行的层数
				   
				      Http  
					  SPDY
					  SSL/TSL
					  TCP
					 
				
			 
		 二 自定义view 类
		 
		  1  自定义控件大小的测量
             /view 本身,
             如何处理控件的WRAP_CONTENT,MATCH_PARENT
             //view group
             如何自定义一个控件
		   
		     AttachInfo 记录view的状态以及和window的绑定关系
		   
          2 Scroller的原理以及ViewPager的实现
			
			    // Scoller是一个滚动工具的包装类：
				// 以下是使用方法：
				
				第一步：初始化实例：
				
			      mScroller = new Scroller(context);
                  ViewConfiguration configuration = ViewConfiguration.get(context);
                  // 获取TouchSlop值，表示最小的移动像素
                  mTouchSlop = ViewConfigurationCompat.getScaledPagingTouchSlop(configuration);
				
				第二步：处理点击事件：
				
				  // 1) 先判断父控件是否该拦截该事件
				  
                  public boolean onInterceptTouchEvent(MotionEvent ev) {
                      switch (ev.getAction()) {
                          case MotionEvent.ACTION_DOWN:
                               mXDown = ev.getRawX();
                               mXLastMove = mXDown;
                               break;
                          case MotionEvent.ACTION_MOVE:
                               mXMove = ev.getRawX();
                               float diff = Math.abs(mXMove - mXDown);
                               mXLastMove = mXMove;
                             // 当手指拖动值大于TouchSlop值时，认为应该进行滚动，拦截子控件的事件
                               if (diff > mTouchSlop) {
                                   return true;
                                }
                               break;
                    }
                     return super.onInterceptTouchEvent(ev);
                 }
				  
				  // 2) 在onTouchEvent 执行真正的操作，记住处理ACTION_CANCEL 处理方式一般与ACTION_UP相同
				  
				  public boolean onTouchEvent(MotionEvent event) {
                      switch (event.getAction()) {
                          case MotionEvent.ACTION_MOVE:
                               mXMove = event.getRawX();
                               int scrolledX = (int) (mXLastMove - mXMove);
							   //控制左边界
                               if (getScrollX() + scrolledX < leftBorder) {
                                   scrollTo(leftBorder, 0);
                                   return true;
							   //控制右边界
                               } else if (getScrollX() + getWidth() + scrolledX > rightBorder) {
                                    scrollTo(rightBorder - getWidth(), 0);
                                    return true;
                               }
                               scrollBy(scrolledX, 0);
                               mXLastMove = mXMove;
                               break;
						  case MotionEvent.ACTION_CANCEL:
                          case MotionEvent.ACTION_UP:
                               // 当手指抬起时，根据当前的滚动值来判定应该滚动到哪个子控件的界面
                               int targetIndex = (getScrollX() + getWidth() / 2) / getWidth();
                               int dx = targetIndex * getWidth() - getScrollX();
                               // 第二步，调用startScroll()方法来初始化滚动数据并刷新界面
                               mScroller.startScroll(getScrollX(), 0, dx, 0);
                               invalidate();
                               break;
                     }
                      return super.onTouchEvent(event);
                }
				  
				  
				 第三步：在view的computeScroll方法里重写
				 
				    @Override
                  public void computeScroll() {
                     // 第三步，重写computeScroll()方法，并在其内部完成平滑滚动的逻辑
                      if (mScroller.computeScrollOffset()) {
                          scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
						  //一定要调用，否则容易出问题
                          invalidate();
                       }
                  }
				 
				 
				 //整个View树的绘图流程是在ViewRootImpl类的performTraversals()方法
				 
				 //里面可以看到performMeasure performLayout 和 performDraw的方法
				 
				 
			3   View的绘制

               //处理事件用GestureDetector

               //修改视图大小 可以在onSizeChanged 处理

               // 处理滑动事件用Scroller类 记住要调用computeOffsetScroll;

               // onMeasured 的处理

                先用 MeasureSpec.getMode（）获得模式  分为三种 EXACTLY,AT_MOST,UNSPECIFIED;

                MeasureSpec.getSize（） 获得建议的宽高

                // 然后根据以上三种模式

                假如是EXACTLY， 则具体高度用刚刚获取的赋值

                假如是其他两种 则要自己手动计算

                最后调用 setMeasuredDimension(width, height);   方法
				
				
				int desiredWidth = 100;
                int desiredHeight = 100;

                int widthMode = MeasureSpec.getMode(widthMeasureSpec);
                int widthSize = MeasureSpec.getSize(widthMeasureSpec);
                int heightMode = MeasureSpec.getMode(heightMeasureSpec);
                int heightSize = MeasureSpec.getSize(heightMeasureSpec);

                int width;
                int height;

                //Measure Width
                if (widthMode == MeasureSpec.EXACTLY) {
                     //Must be this size
                     width = widthSize;
                } else if (widthMode == MeasureSpec.AT_MOST) {
                    //Can't be bigger than...
                    width = Math.min(desiredWidth, widthSize);
                } else {
                   //Be whatever you want
                   width = desiredWidth;
                }

               //Measure Height
               if (heightMode == MeasureSpec.EXACTLY) {
                  //Must be this size
                  height = heightSize;
                } else if (heightMode == MeasureSpec.AT_MOST) {
                  //Can't be bigger than...
                  height = Math.min(desiredHeight, heightSize);
                } else {
                 //Be whatever you want
                 height = desiredHeight;
                }

               //MUST CALL THIS 必须调用
              setMeasuredDimension(width, height);

           // view group

              1  必须要重载onLayout() ,再在onLayout方法里 调用layout的方法确定孩子的位置

                 layout的方法 不能重载

              2  view 绘制的步骤 measure ---> layout ----> draw


           4  SurfaceView 和 TextureView; 如何保证线程安全

              1 TextureView 可以当做一般的view 使用，可以移动，翻转，只能在硬件加速时使用

              2 SurfaceView 窗口后面在创建一个surface

              3 实现SurfaceHolder.CallBack, 在onSurfaceCreated 方法里去开启一个线程去绘制，绘制前需要绘制lockCanvas,

              绘制完后要调用unlockAndPostCanvas;


           5  双缓冲

              当使用双缓冲时，首先在内存缓冲区里完成所有绘制操作，而 不是在屏幕上直接进行绘图。

              当所有绘制操作完成后，把内存缓冲区完成的图像直接复制 到屏幕。

               因为在屏幕上只执行一个图形操作，所以消除了由复杂绘制操作造成的图像闪烁 问题。

              1)  创建Bitmap 缓冲区   cacheBitmap

              2)  创建画new 一个canvas 对象

              3)  调用canvas.setBitmap 的方法，并调用canvas.drawbitmap 的方法

                      mBitQQ = ((BitmapDrawable) getResources().getDrawable(R.drawable.qq)).getBitmap();  
          
						/* 创建屏幕大小的缓冲区 */  
					  mSCBitmap=Bitmap.createBitmap(320, 480, Config.ARGB_8888);    
          
						/* 创建Canvas */  
                      mCanvas = new Canvas();    
          
                       /* 设置将内容绘制在mSCBitmap上 */  
					  mCanvas.setBitmap(mSCBitmap);   
          
                      mPaint = new Paint();  
          
                      /* 将mBitQQ绘制到mSCBitmap上 */  
					  mCanvas.drawBitmap(mBitQQ, 0, 0, mPaint);

           6    为什么可以在子线程中更新UI

           7  (4)  getMeasuredWidth()与getWidth() 区别；

                  getMeasuredWidth()与getWidth()的区别。他们的值大部分时间都是相同的，但意义确是根本不一样的，我们就来简单分析一下。

                  区别主要体现在下面几点：

                 -  首先getMeasureWidth()方法在measure()过程结束后就可以获取到了，而getWidth()方法要在layout()过程结束后才能获取到。

                 -  getMeasureWidth()方法中的值是通过setMeasuredDimension()方法来进行设置的，

                    而getWidth()方法中的值则是通过layout(left,top,right,bottom)方法设置的。


            8   onMeasure 在遍历各个孩子的时候，需要调用measureChild 来获得孩子的宽度和高度。
			
			    onLayout 的方法里需要遍历各个孩子并调用view.layout()的方法来确定各个孩子的位置
			
			9   有意思的控件
                  TagLayout 几点误区

                   1）确定每行里面最高的子布局，用动态规划的思想，保存每行最高的子视图，把每一行最高的叠加起来
                      作为视图的高度
                   2）确定布局的宽度，由于每行的宽度不一定，那布局的宽度该为哪一个呢，同上 用动态规划的思想，保存
                      一个最宽的一行，作为视图
                   3）onMeasure 里 如何确定该换行，也就是当布局超过哪一个宽度该换行，早期是想通过屏幕的宽度，但这是错误的，因为
                      父布局的宽度未必是屏幕宽度啊，正确的做法是 先执行父类的onMeasure 的方法，然后用getMeasuredWidth
                      的方法取得父控件建议的宽度，然后在本类的onMeasure方法里 测量子布局 然后根据子布局的大小，排列和getMeasuredWidth
                      排列做对比，然后确定换行；
                   4）当遍历到最后一个子试图，需要特殊处理//增加高度，或者变化高度

                1  转盘  详细描述

            10  各种touch Event 事件详解
			  
		       ViewGroup 包含onInterceptEvent , 每次view触摸后，就会执行onInterceptEvent, 假如返回true的话，说明父视图自己会响应
			   然后再在onTouchEvent 进行处理事件，并向子视图发送Action_CANCEL 事件。假如返回false,则会把事件传给子视图
		 
	    	11  How the Activity handles touch:
		    
				• Activity.dispatchTouchEvent()
			   
					 • Always first to be called
					 
					 • Sends event to root view attached to Window
					 
					 • onTouchEvent()
					 
						• Called if no views consume the event
						
						• Always last to be called
						
			 // How the View handles touch:
			 
				• View.dispatchTouchEvent()

					 • Sends event to listener first, if exists

						 • View.OnTouchListener.onTouch()
						 
					 • If not consumed, processes the touch itself

						 • View.onTouchEvent()
						 
			 // How a ViewGroup handles touch:
			 
				 • ViewGroup.dispatchTouchEvent()
				 
					 • onInterceptTouchEvent()
					
						 • Check if it should supersede children
						 
						 • Passes  ACTION_CANCEL to active child
						 
						 • Return true once, consumes all subsequent events
						 
					 • For each child view, in reverse order they were added
					 
						 • If touch is relevant (inside view),  child.dispatchTouchEvent()    //2017-08-18 复习时 忘了这一步
						 
						 • If not handled by previous, dispatch to next view    //2017-08-18 复习时 忘了这一步
						 
					 • If no children handle event, listener gets a chance

						  • OnTouchListener.onTouch()   //2017-08-18 复习时 忘了这一步
						  
					 • If no listener, or not handled
					 
						 • onTouchEvent()
						 
				 • Intercepted events jump over child step
				 
				 @Override
				 public boolean onTouchEvent(MotionEvent event) {
					return super.onTouchEvent(event);
				 }
				 
				 //以上super.onTouchEvent 表示调用父类的方法并非父控件的方法

            12   GLSurfaceView;

                 具有生命周期GLSufaceView    glSurfaceView.bringToFront()， glSurfaceView.onPause();

                 Renderer 真正渲染线程， 有三个函数 ，onDrawFrame， onSurfaceChanged ，onSurfaceCreated

                 YUV转RGB 颜色空间 明度亮度饱和度， 与RGB 之间有固定转换公式

                 YCbCr 4:2:0   亮度和色度    1.5byte        Y0 U0 Y1 Y2 U2 Y3 Y5 V5 Y6 Y7 V7 Y8（8像素为例）

                 requestRender 方法刷新页面

                 渲染包括vertex shader 和 fragment shader
				 
				 
			13   视频流的处理  我的店铺  详细流程介绍

                 AudioTrack 播放音频，采集是pcm_16bit,原始的视频流是yuv420 的数据 要先转为rgb565; 软解通过cpu;

                 1）获取视频流

                 2) 如果不支持opengles 2.0 使用cpu解码 调用本地方法把yuv的数据转为rgb 然后再转成bitmap 显示在图片上

                 如果支持gpu解码 openglES GlSurfaceView 渲染 通过shader language 把yuv 转为rgb ，然后进行渲染

                 每一帧调用requestRender  刷新界面

                  相关链接 http://blog.csdn.net/ueryueryuery/article/details/17608185

                 3） 然后展示

                 4）p2p 的原理 有一台服务器s 记录着所有工作的摄像头 ， 当一台电脑想看某一台摄像头时，发个请求给s,
                    s把某一台摄像机的ip地址和端口号发给该电脑，然后监控
					
		    14  universal Image loader

                  缓存策略：
                       基本过程 文件从云端下到本地，再从本地读取缓存到内存中
					  
                 1） 硬盘缓存策略
			   
                     LruDiskCache
                     LimitedAgeDiscCache（设定文件存活的最长时间，当超过这个值，就删除该文件）
                     UnlimitedDiscCache（这个缓存类没有任何的限制）
				   
                 2）内存缓存策略
			   
			         综述: 总的接口是MemoryCache,
					 LruMemoryCache 直接继承MemoryCache 使用的是强引用
					 其余memoryCache 继承BaseMemoryCache, 弱引用和强引用结合，BaseMemoryCache 中有一个hardMap 用于保存强引用
                     类再自行保存另外的缓存softMap,当图片的缓存的大小在最大限制以内 就保存在hardMap,否则保存在softMap里面		
					 
                    1. 只使用的是强引用缓存
                     LruMemoryCache（这个类就是这个开源框架默认的内存缓存类，缓存的是bitmap的强引用，下面我会从源码上面分析这个类）
					 
                    2. 使用强引用和弱引用相结合的缓存有
                    UsingFreqLimitedMemoryCache（如果缓存的图片总量超过限定值，先删除使用频率最小的bitmap）
                    LRULimitedMemoryCache（这个也是使用的lru算法，和LruMemoryCache不同的是，他缓存的是bitmap的弱引用）
					     实现容器是LinkedHashMap;
                    FIFOLimitedMemoryCache（先进先出的缓存策略，当超过设定值，先删除最先加入缓存的bitmap）
                    LargestLimitedMemoryCache(当超过缓存限定值，先删除最大的bitmap对象)
                    LimitedAgeMemoryCache（当 bitmap加入缓存中的时间超过我们设定的值，将其删除）
					
                    3. 只使用弱引用缓存
                    WeakMemoryCache（这个类缓存bitmap的总大小没有限制，唯一不足的地方就是不稳定，缓存的图片容易被回收掉）
            
			      3） 核心类ImageLoaderTask, 加载图片 ImageDecodingInfo 存储图片解码信息,ImageConfiguration：各种参数设置;
  
             15   View Stub 用法
			 
				   占位标签，通常与android:layout 属性一起使用，制定一个布局，默认不展示。当有需要时调用
				   inflate 展示调用代码如下
				   ViewStub noDataViewStub = (ViewStub) view.findViewById(R.id.no_data_viewstub);
				   noDataView = noDataViewStub.inflate();
				   
			 16   自定义控件中，onIntercpetTouchEvent 只能接受action_down 事件的问题
			       
				   需要子控件有一个孩子onTouchEvent 返回true才可以，正常拦截
				   
				   详情请看 https://stackoverflow.com/questions/13283827/onintercepttouchevent-only-gets-action-down
				   
		 	 17   ViewStub/Merge/Include

                  1)  <include />标签能够重用布局文件
				  
				  2)  merge 可以减少视图层级
				  
				      <merge/>标签在UI的结构优化中起着非常重要的作用，它可以删减多余的层级，优化UI。<merge/>
					  多用于替换FrameLayout或者当一个布局包含另一个时，<merge/>标签消除视图层次结构中多余的视图组。
					  例如你的主布局文件是垂直布局，引入了一个垂直布局的include，这是如果include布局使用的LinearLayout就没意义了，
					  使用的话反而减慢你的UI表现。使用如下：
					  
					  <merge xmlns:android="http://schemas.android.com/apk/res/android">  
  
							<Button  
								android:layout_width="fill_parent"   
								android:layout_height="wrap_content"  
								android:text="@string/add"/>  
						  
							<Button  
								android:layout_width="fill_parent"   
								android:layout_height="wrap_content"  
								android:text="@string/delete"/>  
						  
					   </merge> 
					   
				  3)  <ViewStub /> 
				  
				       <ViewStub />标签最大的优点是当你需要时才会加载，使用他并不会影响UI初始化时的性能。
					   各种不常用的布局想进度条、显示错误消息等可以使用<ViewStub />标签，以减少内存使用量，
					   加快渲染速度。<ViewStub />是一个不可见的，大小为0的View。<ViewStub />标签使用如下：
					   
					   <ViewStub  
							android:id="@+id/stub_import"  
							android:inflatedId="@+id/panel_import"  
							android:layout="@layout/progress_overlay"  
							android:layout_width="fill_parent"  
							android:layout_height="wrap_content"  
							android:layout_gravity="bottom" />  
							
					  <ViewStub android:id="@+id/stub"
							android:inflatedId="@+id/subTree"
							android:layout="@layout/mySubTree"
						    android:layout_width="120dip"
							android:layout_height="40dip"/>  
							
					   ((ViewStub) findViewById(R.id.stub_import)).setVisibility(View.VISIBLE);  
						// or  
						View importPanel = ((ViewStub) findViewById(R.id.stub_import)).inflate();  
						
			 18   SurfaceView 所用的锁是 ReentrantLock	
			 
			 19   SurfaceView android TextureView 差别
			    
				  1）TextureView 只有在硬件加速下才能使用，SurfaceView 则没要求
				  
				  2）SurfaceView 是在Window 的下方挖一个洞，并不是一个真正的View,不支持view的缩放和移动，
				   
				     TextureView 则支持
					 
			 20   卡顿优化 //systracce
			  
			      在developer option 打开GPU Render Profile 通过adb shell dumpsys gfxinfo <package-name> 命令获得某一个app的绘制情况
				  
				   Draw    Prepare Process Execute
					0.91    0.10    1.91    1.50
					0.88    0.10    1.76    1.19
					0.92    0.10    2.00    1.26
					0.99    0.10    1.71    1.30
					1.22    0.16    3.07    2.83
					
				   得到以上数据，四个过程加起来是一帧的绘制过程，如果超过16毫秒，则分析原因
				   
				   Execute 是指将 View 的一帧数据给到 compositor 的时间
				   
				   Process 是 Android 2D Renderer 处理 Display List 的时间，假如子 View 很多，层级很深，
				   这部分时间就会花费较多
				   
				   Draw 是指花费在构造 Display List 上的时间，可以理解为 View.onDraw(Canvas) 所消耗的时间。
				   
			 21   如何旋转字体
			 
			      1） 先确定字体的坐标和度数 如下
				  
				      canvas.save();
					  canvas.rotate(90f, 50, 50);
					  canvas.drawText("Text",50, 50, paint);
					  canvas.restore();

			 
			 22   严格模式
			 
			     ThreadPolicy线程策略检测

				线程策略检测的内容有
				自定义的耗时调用 使用detectCustomSlowCalls()开启
				磁盘读取操作 使用detectDiskReads()开启
				磁盘写入操作 使用detectDiskWrites()开启
				网络操作 使用detectNetwork()开启
				VmPolicy虚拟机策略检测

				Activity泄露 使用detectActivityLeaks()开启
				未关闭的Closable对象泄露 使用detectLeakedClosableObjects()开启
				泄露的Sqlite对象 使用detectLeakedSqlLiteObjects()开启
				检测实例数量 使用setClassInstanceLimit()开启
				
			 23  view 的绘制方式
			     
				 ViewRootImpl.doTraversal(ViewRootImpl.java:1528)
			     ViewRootImpl.performTraversals(ViewRootImpl.java:1841)
				 
		     24  Viewpager的坑，当viewpager作为adapter的一部分的时候，viewpager外面一定要包一层外层控件如,
			     Linearlayout 和 RelativeLayout
				 
		     25  处理OnTouchEvent事件 要注意处理ACTION_CANCEL 否则容易有意想不到的情况
			 
			 
			 
			 26  硬解：就是调用GPU的专门模块编码来解，减少CPU运算，对CPU等硬件要求也相对低点。软解需要CPU运算，
			  
			     变相加大CPU负担耗电增加很多:
				 
				 软解码：即通过软件让CPU来对视频进行解码处理，就是通过CPU来运行视频编解码代码，我们最最常见的视频软解码开源看就是FFmpeg: 
				 
				 硬件码优势：更加省电，适合长时间的移动端视频播放器和直播，手机电池有限的情况下，使用硬件解码会更加好。减少CPU的占用，可以把CUP让给别的线程使用，有利于手机的流畅度。
			 
			 27  YUV 格式详解：
			 
			      YUV分为三个分量，“Y”表示明亮度（Luminance或Luma），也就是灰度值；而“U”和“V” 表示的则是色度（Chrominance或Chroma），作用是描述影像色彩及饱和度，用于指定像素的颜色。 

				  与我们熟知的RGB类似，YUV也是一种颜色编码方法，主要用于电视系统以及模拟视频领域，它将亮度信息（Y）与色彩信息（UV）分离，没有UV信息一样可以显示完整的图像，
				  
				  只不过是黑白的，这样的设计很好地解决了彩色电视机与黑白电视的兼容问题。并且，YUV不像RGB那样要求三个独立的视频信号同时传输，所以用YUV方式传送占用极少的频宽。
				  
				  YUV格式有两大类：Plane和Packed。

				  Plane：先连续存储所有像素点的Y，紧接着存储所有像素点的U，随后是所有像素点的V。
				  
                  Packed：每个像素点的Y,U,V是连续交错存储的
				  
				  先记住下面这段话，以后提取每个像素的YUV分量会用到。

                  YUV 4:4:4采样，每一个Y对应一组UV分量。

				  YUV 4:2:2采样，每两个Y共用一组UV分量。 

				  YUV 4:2:0采样，每四个Y共用一组UV分量。 
				  
			// 20   NestedScrolling 机制
			  
				 
		     
			 三 多线程类
			 
			 
             1  多线程

                1）锁的种类：乐观锁和悲观锁

                 Atomic 类核心思想，compareAndSet;

                 i++,和++i 多线程有问题*/

                  public final int incrementAndGet() {
                      for (; ; ) {
                          int current = get();
                          int next = current + 1;
                          if (compareAndSet(current, next))
                           return next;
                      }
                  }

               current 值，next 值， 在compareAndSet 将current 值与 内存中

               当前值对比，如果是相等的，才update 成next 的值

               如果不相等的话  重新循环；


              2  Lock

                   1）Lock用法

                   Lock lock  = new ReentrantLock();
                      lock.lock();
                      try{
                         //可能会出现线程安全的操作
                         }finally{
                             //一定在finally中释放锁
                        //也不能把获取锁在try中进行，因为有可能在获取锁的时候抛出异常
                      lock.ublock();
                     }

                   2）ReadWriteLock 读写锁

                     在读的时候允许其他线程访问，写线程访问的时候，所有的其他读和写都不能访问

                   3）ReentrantLock;

                     重进入锁

                     公平锁 FairLock：有队列在等待，先到先得

                     非公平锁NonFairLock, 立马获得锁。

                      公平锁比较慢  

               3   线程池

					 假设一个服务器完成一项任务所需时间为：T1 创建线程时间，T2 在线程中执行任务的时间，
					 T3 销毁线程时间。如果：T1 + T3 远大于 T2，则可以采用线程池，以提高服务器性能。

					 1）newFixedThreadPool 核心线程数，最大线程数，执行LinkedBlockingQueue 策略
					    先进先出
						
					 2）newSingleThreadPool 核心线程数和最大核心数都只有一个，执行LinkedBlockingQueue;

					 3）newCachedThreadPool 核心线程数0，最大线程数没有限制，任务执行完成后，60秒后会自动退出
						执行线程SynchronousQueue,这个策略比较特殊他并没有把任务加入队列中，而是来一个创建一个

					 4）scheduledThreadExecutor 执行定时任务 ，采用DelayWorkerQueue 策略；

					 5）shutdown 不再接受新任务，要等待所有任务执行完才关闭
						 shutdownnow 不再接受新任务，并且试图停止正在执行的任务
						 
					 6）拒绝策略：
					 
						 ThreadPoolExecutor.AbortPolicy:         
						 丢弃任务并抛出RejectedExecutionException异常。
						 ThreadPoolExecutor.DiscardPolicy：       也是丢弃任务，但是不抛出异常。
						 ThreadPoolExecutor.DiscardOldestPolicy： 丢弃队列最前面的任务，然后重新尝试执行任务（重复此过程）
						 ThreadPoolExecutor.CallerRunsPolicy：    由调用线程处理该任务					  
			    
				
              4  生产者，消费者模式
			
					  * class Producer implements Runnable {
					  *   private final BlockingQueue queue;
					  *   Producer(BlockingQueue q) { queue = q; }
					  *   public void run() {
					  *     try {
					  *       while (true) { queue.put(produce()); }
					  *     } catch (InterruptedException ex) { ... handle ...}
					  *   }
					  *   Object produce() { ... }
					  * }
					  *
					  * class Consumer implements Runnable {
					  *   private final BlockingQueue queue;
					  *   Consumer(BlockingQueue q) { queue = q; }
					  *   public void run() {
					  *     try {
					  *       while (true) { consume(queue.take()); }
					  *     } catch (InterruptedException ex) { ... handle ...}
					  *   }
					  *   void consume(Object x) { ... }
					  * }
					  *
					  * class Setup {
					  *   void main() {
					  *     BlockingQueue q = new SomeQueueImplementation();
					  *     Producer p = new Producer(q);
					  *     Consumer c1 = new Consumer(q);
					  *     Consumer c2 = new Consumer(q);
					  *     new Thread(p).start();
					  *     new Thread(c1).start();
					  *     new Thread(c2).start();
					  *   }
					  * }

             5  CountDownLatch,  CycleBarrier;

                 CountDownLatch 示例代码*/

			 	public class Sample {
					*
					 * 计数器，用来控制线程
					 * 传入参数2，表示计数器计数为2
					
					private final CountDownLatch mCountDownLatch = new CountDownLatch(2);

					*
					 * 示例工作线程类
					
					private class WorkingThread extends Thread {
						private final String mThreadName;
						private final int mSleepTime;

						public WorkingThread(String name, int sleepTime) {
							mThreadName = name;
							mSleepTime = sleepTime;
						}

						@Override
						public void run() {
							System.out.println("[" + mThreadName + "] started!");
							try {
								Thread.sleep(mSleepTime);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							mCountDownLatch.countDown();
							System.out.println("[" + mThreadName + "] end!");
						}
					}

					/**
					 * 示例线程类
					 */
					private class SampleThread extends Thread {

						@Override
						public void run() {
							System.out.println("[SampleThread] started!");
							try {
								// 会阻塞在这里等待 mCountDownLatch 里的count变为0；
								// 也就是等待另外的WorkingThread调用countDown()
								mCountDownLatch.await();
							} catch (InterruptedException e) {

							}
							System.out.println("[SampleThread] end!");
						}
					}

					public void main(String[] args) throws Exception {
						// 最先run SampleThread
						new SampleThread().start();
						// 运行两个工作线程
						// 工作线程1运行5秒
						new WorkingThread("WorkingThread1", 5000).start();
						// 工作线程2运行2秒
						new WorkingThread("WorkingThread2", 2000).start();
					}
				}
				
				CyclicBarrier 示例代码 示例代码

				http://www.java-redefined.com/p/cyclicbarrier.html

			6  CountDownLatch 和 CyclicBarrier 区别
			
					 1  CyclicBarrier rest以后可以复用
					 2  When using a CyclicBarrier, the assumption is that you specify the number of waiting
					threads that trigger the barrier. If you specify 5,
					you must have at least 5 threads to call await().

					When using a CountDownLatch, you specify the number of calls to countDown()
					that will result in all waiting threads being released. This means that you
					can use a CountDownLatch with only a single thread

			7   Condition  signal and await ; 用于条件判断,或者唤醒其他线程
				 和  Object#wait() wait}, {@link Object#notify notify}, and {@link
				   Object#notifyAll notifyAll} 方法的功能类似
				   
				   
		       
			8   synchronized和volatile理解
				      volatile 内存语义：
					     如果一个变量被volatile关键字修饰时，说明对这个变量的写时将本地内存拷贝刷新到共享内存中；
						 对这个变量的读会有一些不同，读的时候无视他的本地内存的拷贝，直接去共享变量中读取数据
						
				       synchronized 的内存语义
					      
						  如果一个变量被synchronized 关键字修饰，那么对这个变量的写时将本地内存中的拷贝刷新到共享内存中
						  ；对这个变量的读就是讲共享内存中的值刷新到本地内存，再从本地内存中读取数据。因为全过程是变量是加锁的
						  ，其他线程无法对这个变量进行读写操作，所以可以理解成对这个变量的任何操作都是原子性的，即使线程安全的。
						  
						volatile 的非原子性
						   遇到count++ 这种情况时，无法保证线程安全，因为count++ 是非原子性的。可以分解为
						   int tem=count; tem=tem+1; count=tmp;
						   
						附 i++ 和 ++ i的区别
						 
						  1） 前者先赋值再自增，后者先自增，再赋值
						  2） ++i和i++ 都是分两步完成的。因为++i后面一步才赋值，所有它能够当做一个变量进行级联赋值，
						       ++i=a=b;++i是一个左值（可被寻址的值）；i++的后面一步是自增，不是左值；
							   
					    volatile 作用线程可见性和防止指令重排序
						
						 什么是指令重排序？有两个层面：
                          在虚拟机层面，为了尽可能减少内存操作速度远慢于CPU运行速度所带来的CPU空置的影响，虚拟机会按照自己的一些规则(这规则后面再叙述)将程序编写顺序打乱——
					      即写在后面的代码在时间顺序上可能会先执行，而写在前面的代码会后执行——以尽可能充分地利用CPU。拿上面的例子来说：
					      假如不是a=1的操作，而是a=new byte[1024*1024](分配1M空间)，那么它会运行地很慢，此时CPU是等待其执行结束呢，还是先执行下面那句flag= true呢？
					      显然，先执行flag=true可以提前使用CPU，加快整体效率，当然这样的前提是不会产生错误(什么样的错误后面再说) 。虽然这里有两种情况：
					      后面的代码先于前面的代码开始执行；前面的代码先开始执行，但当效率较慢的时候，后面的代码开始执行并先于前面的代码执 行结束。不管谁先开始，
					      总之后面的代码在一些情况下存在先结束的可能。
                          在硬件层面，CPU会将接收到的一批指令按照其规则重排序，同样是基于CPU速度比缓存速度快的原因，和上一点的目的类似，
						  只是硬件处理的话，每次只能在接收到的有限指令范围内重排序，而虚拟机可以在更大层面、更多指令范围内重排序。
                          硬件的重排序机制参见《从JVM并发看CPU内存指令重排序(Memory Reordering)》  
						
						 比较：

                         ①  volatile轻量级，只能修饰变量。synchronized重量级，还可修饰方法

                         ②  volatile只能保证数据的可见性，不能用来同步，因为多个线程并发访问volatile修饰的变量不会阻塞。
                            synchronized不仅保证可见性，而且还保证原子性，因为，只有获得了锁的线程才能进入临界区，
						     从而保证临界区中的所有语句都全部执行。多个线程争抢synchronized锁对象时，会出现阻塞。
						   
                 9 Unsafe类的原理，使用它来实现CAS。因此诞生了AtomicInteger系列等
				 
				      
                 10 CAS可能产生的ABA问题的解决，如加入修改次数、版本号
				 
				      CAS虽然很高效的解决原子操作，但是CAS仍然存在三大问题。ABA问题，循环时间长开销大和只能保证一个
					  共享变量的原子操作
 
                      1.ABA问题。因为CAS需要在操作值的时候检查下值有没有发生变化，如果没有发生变化则更新，
					     但是如果一个值原来是A，变成了B，又变成了A，那么使用CAS进行检查时会发现它的值没有发生变化，
					     但是实际上却变化了。ABA问题的解决思路就是使用版本号。在变量前面追加上版本号，每次变量更新的时候
                         把版本号加一，那么A－B－A 就会变成1A-2B－3A。
                         从Java1.5开始JDK的atomic包里提供了一个类AtomicStampedReference来解决ABA问题。这个类的compareAndSet
					     方法作用是首先检查当前引用是否等于预期引用，并且当前标志是否等于预期标志，如果全部相等，
					     则以原子方式将该引用和该标志的值设置为给定的更新值。
                         关于ABA问题参考文档: http://blog.hesey.NET/2011/09/resolve-aba-by-atomicstampedreference.html 

                       2. 循环时间长开销大。自旋CAS如果长时间不成功，会给CPU带来非常大的执行开销。如果JVM能支持
                          处理器提供的pause指令那么效率会有一定的提升，pause指令有两个作用，第一它可以延迟流水线执行指令（de-pipeline）,
						  使CPU不会消耗过多的执行资源，延迟的时间取决于具体实现的版本，在一些处理器上延迟时间是零。
                          第二它可以避免在退出循环的时候因内存顺序冲突（memory order violation）
                          而引起CPU流水线被清空（CPU pipeline flush），从而提高CPU的执行效率。
 
                        3. 只能保证一个共享变量的原子操作。当对一个共享变量执行操作时，我们可以使用
	                       循环CAS的方式来保证原子操作，但是对多个共享变量操作时，循环CAS就无法保证操作的原子性，
						   这个时候就可以用锁，或者有一个取巧的办法，就是把多个共享变量合并成一个共享变量来操作。
						   比如有两个共享变量i＝2,j=a，合并一下ij=2a，然后用CAS来操作ij。从Java1.5开始JDK提供了
						   AtomicReference类来保证引用对象之间的原子性，你可以把多个变量放在一个对象里来进行CAS操作。
				     
                 11 同步器AQS的实现原理
				 
				     1） 一个抽象类，内部是一个先进先出的队列（CLH队列，自旋锁的队列，有点空间复杂度低），用双向链表实现，
					     子类必须重写tryRelease和 tryAcquire 的方法来自定义锁的操作，这个类有三个比较重要的变量head 表示
						 当前持有锁的线程。tail 队尾等待的线程，state 表示锁状态（其实不同的状态下有不同的语义），如ReentrantLock
						 state等于0表示目前锁可用，state=1 表示该锁被占用了一次，2 表示两次，以此类推，在AQS内部基于CAS对其进行更新。
						 
                 12  独占锁、共享锁；可重入的独占锁ReentrantLock、共享锁 实现原理
				 
				     独占锁：只有一个线程能执行，如ReentrantLock ; 重写tryAcuire 尝试获取资源，成功返回true 失败返回fasle
					         tryRelease 尝试释放资源，成功返回true,失败返回fasle;
							 isHeldExclusiveLy：该线程是否独占资源。只有用到Condition才需要去实现它
							 
							 以ReentrantLock 为例，state初始化为0表示未锁定。当lock时，会调用tryAcquire 的方法并独占该所并将
							 state+1. 此后其他线程再tryAcquire时就会失败，直到调用unlock,state=0 时，其他线程才有机会机会。
							 当然，释放锁之前，A线程是可以重复获取此锁的，state会累加，这就是可重入的概念。但是获取多少次，就要释放
							 多少次，这样才能保障state 回到0的状态
							 
					 共享锁：过个线程可以同时执行，如Semaphore、CountDownLatch)。
					 
					         tryAcquireShared 尝试获取资源。负数表示失败，0表示成功但没有可用资源；正式表示成功，有剩余资源
							 tryReleasedShared 尝试释放资源，成功返回true，失败返回false;
							 
							 以countdownlatch为例，任务分为N个子线程去执行，state也初始化为N,（注意N要与线程个数保持一致）。这些子线程
						     是并行执行的，每个子线程执行完后countDown一次，state会用CAS减一，等待所有的线程执行完后（state=0）,会unpark
							 主调用线程，然后主调用线程会从await()函数返回，继续后续动作；await 会将该函数加入等待队列的头部，countDown为0
							 时，取出来继续执行；
							 
				      一般来说，要么是共享模式，要么是独占模式。但是AQS 也支持独占和共享模式的，比如ReenrantReadWriteLock。
					    
                 13 公平锁和非公平锁
				 
				     公平锁： 先进先出 等待队列策略
					 非公平锁：一进入立马尝试获得锁，如失败
				 
				     ReentrantLock 支持公平锁和非公平锁；// 位移,CLH队列锁算法
				 
				      
                 14 读写锁 ReentrantReadWriteLock的实现原理
				      //多读书
                 15 LockSupport工具
				     类似于wait 和 notify的功能，jdk1.5 提供了LockSupport.park() 和 LockSupport.unpark() 的本地方法实现，实现线程的阻塞和唤醒
					 
                 16 Condition接口及其实现原理
				     Condition 是个接口类，主要用于线程的等待和执行操作，包含wait cancel signal 注意不包含（lock 和 unlock 方法，那是属于lock的接口）的方法，
					 主要实现原理是内部维护了一个等待线程的队列，当调用lock 方法时，
					 会在内部的等待队列新增一个节点，当调用signal 的方法时，将这个节点转移到同步的队列的队尾
					 
                 17 HashMap、HashSet、ArrayList、LinkedList、HashTable、ConcurrentHashMap、TreeMap的实现原理
				      ConcurrentHashMap 分段锁。分成很多个Segment 每一个segment 都是一个hashtable
					  Treemap  红黑树
                 18 HashMap的并发问题
				      多线程put 导致get死循环// 用hashtable 或者concurrenthashmap/ 或者用集合类Synchronized 一下
				      
                 19 ConcurrentLinkedQueue的实现原理
				      
					  队列有两种阻塞队列BlockingQueue和非阻塞队列ConcurrentLinkedqueue：
					  ConcurrentLinkedQueue 使用CAS来保持元素的一致性，来实现并发
					  BlockingQueue 是基于ReentrantLock 实现的
				 
                 20 Fork/Join框架
				      Fork/Join 是java7 提供了的一个用于并行执行任务的框架，是把一个大任务分割成若干个小任务，
					  最终汇总每个小任务后得到大任务结果的框架
					  
					  工作窃取算法：
					   wokr-stealing 算法是指某个线程从其他队列里窃取任务来执行。原因是，有些任务执行的快，如果执行完不能干等着
					   所以会从其他的工作队列从获取。维护的数据结构是一个双端队列，正常读取从队列头部获取任务，窃取是从队尾获取；
					   这样设计是为了减少线程的竞争
					   
					   提供两个子类：
					     
						 RecursiveAction: 用于没有返回结果的任务。
						 RecursiveTask:   用于有返回结果的任务
						 
					     需要重载compute 的方法当一个任务的长度小于一定的阈值则直接进行计算
						 如果大于，就继续进行拆分，直到小于阈值为止；
						 
						 拆分时调用fork方法，作用是把他放入到一个ForkAndJoinTask 的数组队列里面，然后再调用ForkJoinPool里面的signalWork()
						 方法唤醒或创建一个工作线程
						 
						 join用于合并执行结果，主要作用是阻塞当前线程获取结果
						 
						 public class Calculator extends RecursiveTask<Integer> {

                         private static final int THRESHOLD = 100;
                         private int start;
                         private int end;

                         public Calculator(int start, int end) {
                            this.start = start;
                            this.end = end;
                         }

                         @Override
                         protected Integer compute() {
                            int sum = 0;
                            if((start - end) < THRESHOLD){
                                for(int i = start; i< end;i++){
                                    sum += i;
                                }
                           }else{
                               int middle = (start + end) /2;
                               Calculator left = new Calculator(start, middle);
                               Calculator right = new Calculator(middle + 1, end);
                               left.fork();
                               right.fork();

                               sum = left.join() + right.join();
                           }
                           return sum;
                         }

                       }
						 
                21  CountDownLatch和CyclicBarrier，Semaphore  //尾递归，死锁
				 
				      CountDownLatch： 一个任务要在其他任务都执行完以后，才能执行，
					  用法：在等待的线程里调用await(), 然后 再在其他执行的线程里调用countdown
					        当countdown执行到0时，等待的线程会继续执行
							
					  CyclicBarrier: 当所有任务到达一个节点以后再继续往下执行，通过在线程里面调用await来等待
					  
					  实现原理和CountDownLatch 有所不同，CountDownLatch 是通过继承aqs 来自定义同步器的，但是CyclicBarrier
					  则是通过ReentrantLock 和 condition类来实现
					  
					  * class Solver {
                      *   final int N;
                      *   final float[][] data;
                      *   final CyclicBarrier barrier;
                      *
                      *   class Worker implements Runnable {
                      *     int myRow;
                      *     Worker(int row) { myRow = row; }
                      *     public void run() {
                      *       while (!done()) {
                      *         processRow(myRow);
                      *
                      *         try {
                      *           barrier.await();
                      *         } catch (InterruptedException ex) {
                      *           return;
                      *         } catch (BrokenBarrierException ex) {
                      *           return;
                      *         }
                      *       }
                      *     }
                      *   }
                      *
                      *   public Solver(float[][] matrix) {
                      *     data = matrix;
                      *     N = matrix.length;
                      *     barrier = new CyclicBarrier(N,
                      *                                 new Runnable() {
                      *                                   public void run() {
                      *                                     mergeRows(...);
                      *                                   }
                      *                                 });
                      *     for (int i = 0; i < N; ++i)
                      *       new Thread(new Worker(i)).start();
                      *
                      *     waitUntilDone();
                      *   }
                      * }}
				 
				      Semaphore:  信号量是用来对于某一共享资源所能访问的最大个数进行限制，比如说20个人上厕所，但是只有5个坑，所以有15个人需要等待，等别人用
					              完了才能用。另外其他人的等待是可以随机获得优先机会，也是可以按照先来后台的顺序获得机会，这取决于所用的是公平模式还是非公平
								  模式。
								  通过acquire方法获得许可，release 释放许可
								  
					   public class TestSemaphore {

                            public static void main(String[] args) {

                           // 线程池

                           ExecutorService exec = Executors.newCachedThreadPool();

                          // 只能5个线程同时访问

                          final Semaphore semp = new Semaphore(5);
 
                          // 模拟20个客户端访问
 
                          for (int index = 0; index < 20; index++) {

                                  final int NO = index;
 
                                  Runnable run = new Runnable() {

                                                 public void run() {

                                                            try {

                                                                    // 获取许可

                                                                    semp.acquire();

                                                                    System.out.println("Accessing: " + NO);

                                                                    Thread.sleep((long) (Math.random() * 10000));

                                                                    // 访问完后，释放

                                                                    semp.release();

                                                                    System.out.println("-----------------"+semp.availablePermits());

                                                            } catch (InterruptedException e) {

                                                                    e.printStackTrace();

                                                            }

                                                  }

                                       };

                                    exec.execute(run);

                                }

                                  // 退出线程池

                                exec.shutdown();

                         }
 
                   } 

                   执行结果如下：

                  Accessing: 0

                  Accessing: 1

                  Accessing: 3

                  Accessing: 4

                  Accessing: 2

                  -----------------0

                  Accessing: 6

                  -----------------1

                  Accessing: 7

                  -----------------1

                  Accessing: 8

                  -----------------1

                  Accessing: 10

                  -----------------1

                  Accessing: 9

                  -----------------1

                  Accessing: 5

                  -----------------1

                  Accessing: 12

                  -----------------1

                  Accessing: 11

                  -----------------1

                  Accessing: 13

                   -----------------1

                   Accessing: 14

                   -----------------1

                   Accessing: 15

                   -----------------1

                   Accessing: 16

                   -----------------1

                   Accessing: 17

                   -----------------1

                   Accessing: 18

                   -----------------1

                   Accessing: 19'

								   
								  
				       
				 22  synchronized 和 lock
				      
					  1、 ReentrantLock 拥有Synchronized相同的并发性和内存语义，此外还多了 锁投票，定时锁等候和中断锁等候
                         线程A和B都要获取对象O的锁定，假设A获取了对象O锁，B将等待A释放对O的锁定，
                         如果使用 synchronized ，如果A不释放，B将一直等下去，不能被中断
                         如果 使用ReentrantLock，如果A不释放，可以使B在等待了足够长的时间以后，中断等待，而干别的事情

                         ReentrantLock获取锁定与三种方式：
                         a) lock(), 如果获取了锁立即返回，如果别的线程持有锁，当前线程则一直处于休眠状态，直到获取锁
                         b) tryLock(), 如果获取了锁立即返回true，如果别的线程正持有锁，立即返回false；
                         c)tryLock(long timeout,TimeUnit unit)， 如果获取了锁定立即返回true，如果别的线程正持有锁，会等待参数给定的时间，在等待的过程中，如果获取了锁定，
						 就返回true，如果等待超时，返回false；
                         d) lockInterruptibly:如果获取了锁定立即返回，如果没有获取锁定，当前线程处于休眠状态，直到或者锁定，或者当前线程被别的线程中断

                     2、 synchronized是在JVM层面上实现的，不但可以通过一些监控工具监控synchronized的锁定，而且在代码执行时出现异常，
					       JVM会自动释放锁定， 但是使用Lock则不行，lock是通过代码实现的，要保证锁定一定会被释放，就必须将unLock()放到finally{}中
 
                     3、 在资源竞争不是很激烈的情况下，Synchronized的性能要优于ReetrantLock，但是在资源竞争很激烈的情况下，Synchronized的性能会下降几十倍，
						   但是ReetrantLock的性能能维持常态；5.0 的多线程任务包对于同步的性能方面有了很大的改进，在原有synchronized关键字的基础上，
						   又增加了ReentrantLock，以及各种Atomic类。了解其性能的优劣程度，有助与我们在特定的情形下做出正确的选择。
					  
				 23 ThreadLocal 设计
				      每个线程保存本地对象。
					  并不是用来解决共享对象的多线程访问问题的，一般情况下，通过threadLocal.set()到线程中的对象是该线程使用自己的对象，其他线程不需要访问也访问
					  不到的。各个线程中访问得到的是不同的对象。另外。说ThreadLocal使得各个线程保持各自独立的一个对象，是通过每个线程创建一个对象来实现的，
					  并不是一个副本。
					  
					  内部实现：会维护一个Map,key是threadLocal 本身，set是keyMap 保存， 一个threadlocal 可以有多个map. 之前版本用ThreadLocalMap 实现 Entry自定义为软引用
					  当前版本用values 类来实现，内部维护了一个数组，index是 threadlocal 的hash 值，和values 类的mask 通过与取得，mask值是数组长度减一
					  
					  常用场景： 数据库连接管理，session 管理
					   join 用法： 内部基于wait 实现
					   
					   
				 24 AsyncTask 源码分析 //双端队列Dequeue
		  
				    核心线程数：根据CPU核心数，至少两个，最多四个，最好可以比cpu核心数少1，以免占满cpu
				    最大线程数 核心数的2倍加一，存活时间30秒；
				    排队策略：LInkedBlockQUeue,最多128个等待线程
				    线程池当前版本串行，串行--》并发---》串行
				    默认线程池 SERIAL_EXECUTOR 一个进程只有一个
				    Callable 接口返回泛型，mWOkerThread 就是继承Callable;
				    FutureTask 真正的工作类，用于包装Callable, 实现Runnable,和Future类，Future 类定义了多个函数
				    一般用于线程是否执行完，cancel,get 执行结果
					
					SERIAL_EXECUTOR  用于排队；队列 入队offer  出队poll;
					THREAD_POOL_EXECUTOR 用于执行任务  
					
					各个版本的区别：
					
					 对于AsyncTask的在不同版本之间的差异不得不提一下。在Android1.6，AsyncTask采用的是串行执行任务，
					 在Android1.6的时候采用线程池处理并行任务，而在3.0以后才通过SerialExecutor线程池串行处理任务。
					 在Android4.1之前AsyncTask类必须在主线程中，但是在之后的版本中就被系统自动完成。
					 而在Android5.0的版本中会在ActivityThread的main方法中执行AsyncTask的init方法，而在Android6.0中又将init方法删除。
					
				 25  AsynckTask 源码自己的理解：
				 
				     核心类：WorkerRunnable ;工作线程 实现Callable 接口主要任务在这边的call方法处理
					         
							 FutureTask :实现Future，和RunnableFutrue,实现runnable 的run方法，在这个run方法里执行Callable 的call 方法；
							 
							 SERIAL_EXECUTOR: 用于任务的排队，当执行excute 方法时，将任务塞进队列 用于排队的类是ArrayDeque;
							 
							 THREAD_POOL_EXECUTOR： 用于任务的执行 ，核心线程数：根据CPU核心数，至少两个，最多四个，最好可以比cpu核心数少1，以免占满cpu
				                                           最大线程数 核心数的2倍加一，存活时间30秒；
				                                     排队策略：LInkedBlockQUeue,最多128个等待线程
							ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                                                           
														   CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,sPoolWorkQueue, sThreadFactory);
													 
						     当new AsyncTask 的时候 WorkRunnable 会实例化  ， FutureTask 会实例化mFutureTask，并把workrunnable 作为实例传进来
							 
							 当执行excute()的方法的时候 会调用executeOnExcutor的方法,此时调用OnPreExecute 其实执行的是 SerialExecutor 的execute的方法
							 
							 以下是该类的实现  表明了整个流程：
							 
							 private static class SerialExecutor implements Executor {
                                 final ArrayDeque<Runnable> mTasks = new ArrayDeque<Runnable>();
                                    Runnable mActive;

                               public synchronized void execute(final Runnable r) {
                                      mTasks.offer(new Runnable() {
                                     public void run() {
                                     try {
                                           r.run();
                                      } finally {
                                            scheduleNext();
                                         }
                                }
                               });
                               if (mActive == null) {
                                    scheduleNext();
                                }
                              }

                             protected synchronized void scheduleNext() {
                                if ((mActive = mTasks.poll()) != null) {
                                   THREAD_POOL_EXECUTOR.execute(mActive);
                              }
                            }
    
						    
							1  先排队 排队的时候new一个线程 然后执行runnable（这个run方法其实是FutureTask的run方法） 的run方法 --》如果没有任务在执行，
							
							   从队列中取出到THREAD_POOL_EXECUTOR执行，每条线程执行完以后 会finally 一下scheduleNext 执行下一条任务， 执行顺序是FutureTask.run  --> 
							
							   WorkRunnable.call -->  执行doInbackground方法 此时内部自己实现 publishProgress方法
						      
							    --》执行完成后，通过handler 发送出来---》onPostExecute 方法；
							
							  
							 
							 
							 
							
					
		 四 操作系统类
		 
		    
		    1  进程：

			进程间的通信
			通常的做法是，发送方将准备好的数据存放在缓存区中，
			调用API通过系统调用进入内核中。内核服务程序在内核空间分配内存，
			将数据从发送方缓存区复制到内核缓存区中。接收方读数据时也要提供一
			块缓存区，内核将数据从内核缓存区拷贝到接收方提供的缓存区中并唤醒接收线程，
			完成一次数据发送。
			发送方缓冲区-----》内核缓冲区-----》接收方缓冲区
			由Binder驱动负责管理数据接收缓存。我们注意到Binder驱动实现了mmap()系统调用，
			这对字符设备是比较特殊的，因为mmap()通常用在有物理存储介质的文件系统上，
			而象Binder这样没有物理介质，纯粹用来通信的字符设备没必要支持mmap()。
			Binder驱动当然不是为了在物理介质和用户空间做映射，
			而是用来创建数据接收的缓存空间。
			发送方缓冲区 -----》接收方缓冲区 节省了一个环节
			通过上面介绍可以看到，驱动为接收方分担了最为繁琐的任务：分配/释放大小不等，难以预测的有效负荷缓存区，
			而接收方只需要提供缓存来存放大小固定，最大空间可以预测的消息头即可。在效率上，由于mmap()
			分配的内存是映射在接收方用户空间里的，所有总体效果就相当于对有效负荷数据做了一次从发送方用户
			空间到接收方用户空间的直接数据拷贝，省去了内核中暂存这个步骤，提升了一倍的性能。
			Client、Server和Service Manager运行在用户空间，Binder驱动程序运行内核空间


			用户空间 和 内核空间 进程上下文，，transact 发送数据 onTransact 接收数据

			2  用户空间和内存空间

			内核空间中存放的是内核代码和数据，而进程的用户空间中存放的是用户程序的代码和数据。
			不管是内核空间还是用户空间，它们都处于虚拟空间中。

			Linux 操作系统和驱动程序运行在内核空间，应用程序运行在用户空间，
			两者不能简单地使用指针传递数据，因为Linux使用的虚拟内存机制，用户空间的数据可能被换出，
			当内核空间使用用户空间指针时，对应的数据可能不在内存中。

			针对linux操作系统而言，将最高的1G字节（从虚拟地址0xC0000000到0xFFFFFFFF），
			供内核使用，称为内核空间，而将较低的3G字节（从虚拟地址0x00000000到0xBFFFFFFF），
			供各个进程使用，称为用户空间。每个进程可以通过系统调用进入内核，因此，
			Linux内核由系统内的所有进程共享。于是，从具体进程的角度来看，
			每个进程可以拥有4G字节的虚拟空间。

			3 进程上下文

			（1）内核态，运行于进程上下文，内核代表进程运行于内核空间。
			（2）内核态，运行于中断上下文，内核代表硬件运行于内核空间。
			（3）用户态，运行于用户空间。

			上下文context： 上下文简单说来就是一个环境。
			用户空间的应用程序，通过系统调用，进入内核空间。这个时候用户空间的进程要传递 很多变量、参数的值给内核，
			内核态运行的时候也要保存用户进程的一些寄存 器值、变量等。所谓的“进程上下文”，可以看作是用户进程传递给
			内核的这些参数以及内核要保存的那一整套的变量和寄存器值和当时的环境等。
			相对于进程而言，就是进程执行时的环境。具体来说就是各个变量和数据，包括
			所有的寄存器变量、进程打开的文件、内存信息等。一个进程的上下文可以分为三个部分:
			用户级上下文、寄存器上下文以及系统级上下文。

			 （1）用户级上下文: 正文、数据、用户堆栈以及共享存储区；
			 （2）寄存器上下文: 通用寄存器、程序寄存器(IP)、处理器状态寄存器(EFLAGS)、栈指针(ESP)；
			 （3）系统级上下文: 进程控制块task_struct、内存管理信息(mm_struct、vm_area_struct、pgd、pte)、内核栈。

			  当发生进程调度时，进行进程切换就是上下文切换(context switch).操作系统必须对上面提到的全部信息进行切换，
			  新调度的进程才能运行。而系统调用进行的模式切换(mode switch)。模式切换与进程切换比较起来，容易很多，
			  而且节省时间，因为模式切换最主要的任务只是切换进程寄存器上下文的切换。

		   4  父进程/子进程
		   
			   特点  1）调用一次返回两次  2）写时复制
			         1) 调用一次，返回两次，原因，父进程创建子进程以后，pc寄存器的指令会定位到同一个地方
			            两个进程会同时执行同一段代码，所以会返回两次。父进程返回子进程id, 子进程，如果创建
			            成功返回0，创建不成功返回负数
			         2) 写时复制
			             在fork之后exec之前两个进程用的是相同的物理空间（内存区），子进程的代码段、数据段、
			             堆栈都是指向父进程的物理空间，也就是说，两者的虚拟空间不同，但其对应的物理空间是同一个。
			             当父子进程中有更改相应段的行为发生时，再为子进程相应的段分配物理空间

			   44.6 进程的组成
					  jvm 方法区，堆，栈，本地方法区，pc register 用于存储指令
					  在Linux系统中进程由以下三部分组成：①进程控制块PCB；②数据段；③正文段。

		  5  Binder 机制介绍

           1) 为什么不用传统的进程通信方式,
		   
					IPC              内存拷贝次数
				   共享内存                 0
				   Binder                   1
				  Socket/管道/消息队列      2
				  其中使用共享内存，有线程安全问题，使用Socket/管道/消息队列 需要复制两次内存，性能消耗较大
				 一般进程间的通信，
				 通常的做法是，发送方将准备好的数据存放在缓存区中，
				 调用API通过系统调用进入内核中。内核服务程序在内核空间分配内存，
				 将数据从发送方缓存区复制到内核缓存区中。接收方读数据时也要提供一
				  块缓存区，内核将数据从内核缓存区拷贝到接收方提供的缓存区中并唤醒接收线程，
				  完成一次数据发送。 而由Binder驱动负责管理数据接收缓存。我们注意到Binder驱动实现了mmap()系统调用，
				  这对字符设备是比较特殊的，因为mmap()通常用在有物理存储介质的文件系统上，
				  而象Binder这样没有物理介质，纯粹用来通信的字符设备没必要支持mmap()。
				  Binder驱动当然不是为了在物理介质和用户空间做映射，
				  而是用来创建数据接收的缓存空间。 通过上面介绍可以看到，驱动为接收方分担了最为繁琐的任务：分配/释放大小不等，难以预测的有效负荷缓存区，
				  而接收方只需要提供缓存来存放大小固定，最大空间可以预测的消息头即可。在效率上，由于mmap()
				  分配的内存是映射在接收方用户空间里的，所有总体效果就相当于对有效负荷数据做了一次从发送方用户
				  空间到接收方用户空间的直接数据拷贝，省去了内核中暂存这个步骤，提升了一倍的性能。
				  
            2) Binder 调用的方式；
                 binder 调用的组成client,server,ServiceManager 用来注册和搜索服务，以及Binder 驱动
                 client ---->SM查找service，并返回该service的一个代理proxy --->
                 Binder 驱动（与binder驱动进行通信）--->server收到请求并执行，【回复（）比如ActivityManagerService
                 会通过ProcessRecord 里面的IApppllicationThread 来通信】
				 
		     3) Binder 架构中   Client: BpBinder      Server: BnBinder
			 
			 
			 4) Aidl介绍：
			 
			     1）实现Stub类
				
				 2) 在OnServiceConnection 返回Aidl的Binder
				
				    private ServiceConnection serviceConnection = newServiceConnection() {

                           //@Override

                        public void onServiceConnected(ComponentName name, IBinder service) {

                             if(ITestProxy== null)

                              ITestProxy = ITest.Stub.asInterface(service);//这样你就得到binder了

                      }
			    3) 传递复杂的数据结构
				
				 .   它必须实现implements Parcelable接口。

                 . 内部必须有一个静态的CREATOR类。
				 
			    
				 
	    6  进程隔离的实现
		
				根源：如果直接使用物理内存，进程A会直接访问进程B的内存，导致奔溃
				
				解决:
				
				为了解决上述问题，人们想到了一种变通的方法，就是增加一个中间层，利用一种间接的地址访问方法访问物理内存。
				按照这种方法，程序中访问的内存地址不再是实际的物理内存地址，而是一个虚拟地址，然后由操作系统将这
				个虚拟地址映射到适当的物理内存地址上。这样，只要操作系统处理好虚拟地址到物理内存地址的映 射，
				就可以保证不同的程序最终访问的内存地址位于不同的区域，彼此没有重叠，就可以达到内存地址空间隔离的效果。
				 当创建一个进程时，操作系统会为该进程分配一个 4GB 大小的虚拟进程地址空间
				 
		      // bitmap display task 展示图片
         7  进程回收顺序
		 
			   1) foreground activity 如果该进程中含有foreground activity或者是Service 被设置成Foreground,正在与用户进行交互，只有在该进程使用的内存
					比剩下的内存多，才会被回收
			   2) visible activity  如果该activity 不是foreground activity，比如说在一个dialog后面，这个activity也非常重要
					只有在内存非常不足，为了保证其他foreground activity 运行，进程才会被回收
			   3) background activity  就是该activity 不可见， 当为了保证 foreground activity 和 visible activity,运行时
					background activity 才会被回收， 当这个activity 被销毁后，但是又回到前台时 会重新创建activity
				  并将之前在 onSaveInstanceState 保存的bundle 返回，执行onRestoreInstance 方法
			   4) empty process 空进程，不含有activity , 或者service, 或者broadcast receiver , 当内存不足时，很容易被回收
			   
			   
			   	
          8   内存泄露的几种方式
		  
				1)监听器没有及时关闭
				2)非静态内部类持有外部类的引用  比如asyncTask 写一个静态的内部类
				3) 未正确使用context 导致Actvity 无法被回收，
				4) Handler 内存泄露
				5) 资源对象未关闭
					 资源对象比如Cursor、File等，往往都用了缓冲，不使用的时候应该关闭它们。
					 把他们的引用置为null，而不关闭它们，往往会造成内存泄漏。因此，在资源对象不使用时，
					 一定要确保它已经关闭，通常在finally语句中关闭，防止出现异常时，资源未被释放的问题。
				6） webview 内存泄露，将他单独放在一个进程里
				7)  2.8 集合中对象没清理
					   通常把一些对象的引用加入到了集合中，当不需要该对象时，如果没有把它的引用从集合中清理掉，
					   这样这个集合就会越来越大。如果这个集合是static的话，那情况就会更加严重。

				8) 2.9 Bitmap对象
					  临时创建的某个相对比较大的bitmap对象，在经过变换得到新的bitmap对象之后，应该尽快回收原始的bitmap，这样能够更快释放原始bitmap所占用的空间。
					 避免静态变量持有比较大的bitmap对象或者其他大的数据对象，如果已经持有，要尽快置空该静态变量。
					 
		        Binder 传输数据大小 最多 800kb
				
                在onTransact 返回处理结果 通过 IApplicationThread 的一个实现类 实际上是ApplicationThreadProxy；
				
	       9    zygote Android通过zygote生成其他程序和进程
		      
		         为什么用zygote
			    
				1) Linux 下进程的产生 是通过fork函数产生的，是c++函数，应用程序使用java写的，应用直接调肯定不方便，
  				所以包装了一个zygote 给大家使用
				2)zygote启动以后 会向系统注册Socket ， 监听进来的socket 请求用户创建进程
				3)当zygote被杀死后，其他app都会死亡，写时复制引起的（猜想）;
				
		   10   Handler 的介绍和解析
		        
				重点：MessageQueue  存储方式单向链表    Looper : 可以是主的looper （Handler） 也可以是非主的looper(HandlerThread);
				
				      
		   11   ListView和RecyclerView的区别缓存机制:
		   
		       1) mRecyclerView 可以单独改变某个item的布局 ，各个ItemView 加Flag 来区分,ListView 一锅端
			   
			   2）ListView 缓存view   recycleView 缓存ViewHolder;
			   
			   3) RecyclerView onLayout()为重点，分为三步：

                  dispathLayoutStep1()： 记录RecyclerView刷新前列表项ItemView的各种信息，如Top,Left,Bottom,Right，用于动画的相关计算；

                  dispathLayoutStep2()： 真正测量布局大小，位置，核心函数为layoutChildren()；

                  dispathLayoutStep3()： 计算布局前后各个ItemView的状态，如Remove，Add，Move，Update等，如有必要执行相应的动画
				  
			
				  
			   
		   12  ThreadLocal 解析
		   
		        1）每个线程中都创建了一个副本，那么每个线程可以访问自己内部的副本变量。
				
				    
				2） 重要： Thread类有一个thread_local 的变量 
				
				     public T get() {
                          Thread t = Thread.currentThread();
                          ThreadLocalMap map = getMap(t);
                          if (map != null) {
                              ThreadLocalMap.Entry e = map.getEntry(this);
                           if (e != null) {
                               @SuppressWarnings("unchecked")
                              T result = (T)e.value;
                            return result;
                         }
                       }
                      return setInitialValue();
                     }
					 
					 
					  private T setInitialValue() {
                         T value = initialValue();
                         Thread t = Thread.currentThread();
                         ThreadLocalMap map = getMap(t);
                         if (map != null)
                            map.set(this, value);
                          else
                            createMap(t, value);
                         return value;
                     }
					 
					 
					 void createMap(Thread t, T firstValue) {
                        t.threadLocals = new ThreadLocalMap(this, firstValue);
                     }
					 
					 
					 protected T initialValue() {
                       return null;
                     }

				3）使用场景 和例子
				
				    最常见的ThreadLocal使用场景为 用来解决 数据库连接、Session管理等。
					
					Connection:
					
					private static ThreadLocal<Connection> connectionHolder= new ThreadLocal<Connection>() {
                       public Connection initialValue() {
                         return DriverManager.getConnection(DB_URL);
                       }
                    };
 
                        public static Connection getConnection() {
                         return connectionHolder.get();
                    }
				  
				   
				    Session:
					
					private static final ThreadLocal threadSession = new ThreadLocal();
 
                    public static Session getSession() throws InfrastructureException {
                         Session s = (Session) threadSession.get();
                        try {
                           if (s == null) {
                               s = getSessionFactory().openSession();
                           threadSession.set(s);
                        }
                        } catch (HibernateException ex) {
                         throw new InfrastructureException(ex);
                       }
                       return s;
                    }
					 
					 
				
				    
		   13   列表的优化方案:
		   
		         1、ViewHolder优化

                    使用ViewHolder的原因是findViewById方法耗时较大，如果控件个数过多，会严重影响性能，而使用ViewHolder主要是为了可以省去这个时间。通过setTag，getTag直接获取View
					
				 2、图片加载优化

                    如果ListView需要加载显示网络图片，我们尽量不要在ListView滑动的时候加载图片，那样会使ListView变得卡顿，所以我们需要在监听器里面监听ListView的状态，
					
					如果ListView滑动（SCROLL_STATE_TOUCH_SCROLL）或者被猛滑（SCROLL_STATE_FLING）的时候，停止加载图片，如果没有滑动（SCROLL_STATE_IDLE），则开始加载图片。
		         
			     3、onClickListener处理

				   当ListView的item中有比如button这些子view时，需要对其设置onclickListener，通常的写法是在getView方法中一个个设置，比如

                   holder.img.setonClickListener(new onClickListenr)...  
				   
                   但是这种写法每次调用getView时都设置了一个新的onClick事件，效率很低。高效的写法可以直接在ViewHolder中设置一个position，
				  
				   然后viewHolder implements OnClickListenr：
				   
			    4, 减少Item View的布局层级
				   
                   这是所有layout都必须遵循的，布局层级过深会直接导致View的测量与绘制浪费大量的时间
				   
				5、 adapter中的getView方法尽量少做耗时操作

				6、 adapter中的getView方法避免创建大量对象

				7、 将ListView的scrollingCache和animateCache设置为false   
				
				8,  其它

                    1、 利用好 View Type，例如你的 ListView 中有几个类型的 Item，需要给每个类型创建不同的 View，这样有利于 ListView 的回收，当然类型不能太多

					2、 善用自定义 View，自定义 View 可以有效的减小 Layout 的层级，而且对绘制过程可以很好的控制；

					3、 尽量能保证 Adapter 的 hasStableIds() 返回 true，这样在 notifyDataSetChanged() 的时候，如果 id 不变，ListView 将不会重新绘制这个 View，达到优化的目的；

					4、 每个Item 不能太高，特别是不要超过屏幕的高度，可以参考 Facebook 的优化方法，把特别复杂的 Item 分解成若干小的 Item

					5、 ListView 中元素避免半透明

					6、 尽量开启硬件加速

					7、 使用 RecycleView 代替。 ListView 每次更新数据都要 notifyDataSetChanged()，有些太暴力了。RecycleView 在性能和可定制性上都有很大的改善，推荐使用。
					
				   
				 
	   五 Framework 类
	   
	        1、应用程序窗口 (Application Window): 包括所有应用程序自己创建的窗口，以及在应用起来之前系统负责显示的窗口。

            2、子窗口(Sub Window)：比如应用自定义的对话框，或者输入法窗口，子窗口必须依附于某个应用窗口（设置相同的token)。

            3、系统窗口(System Window):

            4、dialog 的创建过程

                Dialog的Window的创建过程和Activity类似，有以下几个步骤：
                创建Window。同样是通过PolicyManager的makeNewWindow方法来完成的。
                初始化DecorView并将Dialog的视图添加到DecorView中。
                将DecorView添加到Window中并显示。在Dialog的show方法中，会通过WindowManager将DecorView添加到Window中。
                  普通的Dialog有一个特殊之处，那就是必须采用Activity的Context，如果采用Application的Context，就会报错。

            5、activity 创建过程

               在Activity的attach方法里，系统会创建Activity所属的Window对象并为其设置回调接口，
               Window对象的创建是通过PolicyManager的makeNewWindow方法实现的，
               对于Activity的setContentView的实现可以看出，Activity将具体实现交给了Window处理，
               而Window的具体实现是PhoneWindow，所以只需要看PhoneWindow相关逻辑即可，
               DecorView已经被创建初始化完毕，Activity的布局文件已经成功添加到了DecorView的mContentParent中，
               但是这个时候DecorView还没有被WindowManager正式添加到Window中，
               真正被视图调用是在Activity的onResume方法，接着会调用Activity的makeVisible()，
               正是在makeVisible方法中，DecorView真正地完成了添加和显示这两个过程。

            6、service 创建过程

            7、broadcastreceiver 创建过程

            8、contentProvider 创建过程

            9、window的创建过程

              window是android中的窗口，表示顶级窗口的意思，也就是主窗口，
              它有两个实现类，PhoneWindow和MidWindow,
              每个主窗口中都有一个View，称之为DecorView，
              是主窗口中的顶级view（实际上就是ViewGroup）

              Window 类 一个接口 包含一些回调

              WindowManager类主要用来管理窗口的一些状态、属性、view增加、删除、更新、窗口顺序、消息收集和处理等。

              ViewRoot是View和WindowManager之间的桥梁，真正把View传递给WindowManager的是通过ViewRoot的setView()方法，

              ViewRoot实现了View和WindowManager之间的消息传递,WindowManager 继承ViewManager 用于管理View,有以下方法。
			  
			  public void addView(View view, ViewGroup.LayoutParams params);
			  public void updateViewLayout(View view, ViewGroup.LayoutParams params);
			  public void removeView(View view);
			   
			   
			 
            10、Toast的Window创建过程

                Toast和Dialog不同，它的工作过程稍微复杂。首先Toast也是基于Window来实现的，

                但是由于Toast具有定时取消这一功能，所以系统采用了Handler。在Toast的内部有两类的IPC过程，

                第一类是Toast访问NotificationManagerService，第二类是NotificationManagerService回调Toast的TN接口。

                Toast属于系统Window，它内部的视图有两种方式指定，一种是系统默认的样式，

                另一种是通过setView方法来指定一个自定义View，不管如何，

                它们都对应Toast的一个View类型的内部成员mNextView。Toast提供了show和cancel分别用于显示和隐藏Toast，

                 它们的内部是一个IPC过程。


            11  ActvityManagerService

                ActivityManagerServices，简称AMS，服务端对象，负责系统中所有Activity的生命周期
                ActivityThread，App的真正入口。当开启App之后，会调用main()开始运行，开启消息循环队列，这就是传说中的
                UI线程或者叫主线程。与ActivityManagerServices配合，一起完成Activity的管理工作
				
				AMS在跨进程通信过程中，既可以当客户端，也可以端服务端：
				
			    1) App和AMS通信的时候：
				
				   App 是客户端  -----》代理对象ActivityManagerProxy  --->Binder 驱动 -----》AMS 服务端
				   
				   客户端请求的时候会调用ActivityManagerProxy实现的IActivityManager的接口 来发送请求
				   
				2）AMS 通知APP 事件的时候

                   AMS是客户端  -----》 代理对象ApplicationThreadProxy ---->Binder 驱动 -----》ActivityThread 服务端			
				
                   AMS 通知客户端的时候ApplicationThreadProxy实现的IApplicationThread 来schedule 方法来操作，然后在
				   
				   ActivityThread 里的Handler 来发送消息 来让Instrument 做dirty work;

                Instrumentation，每一个应用程序只有一个Instrumentation对象，每个Activity内都有一个对该对象的引用。
                Instrumentation可以理解为应用进程的管家，ActivityThread要创建或暂停某个Activity时，都需要通过Instrumentation来进行具体的操作。

                 ActivityStack，Activity在AMS的栈管理，用来记录已经启动的Activity的先后关系，状态信息等。
                 通过ActivityStack决定是否需要启动新的进程。

                 ActivityRecord，ActivityStack的管理对象，每个Activity在AMS对应一个ActivityRecord，
                 来记录Activity的状态以及其他的管理信息。其实就是服务器端的Activity对象的映像。

                 TaskRecord，AMS抽象出来的一个“任务”的概念，是记录ActivityRecord的栈，
                 一个“Task”包含若干个ActivityRecord。AMS用TaskRecord确保Activity启动和退出的顺序。如果你清楚Activity的4种launchMode，
                 那么对这个概念应该不陌生。

                 这里的ActivityManagerNative.getDefault返回的就是ActivityManagerService的远程接口，即ActivityManagerProxy。都实现了
                 IActivityManager 接口
				 
				  
			 12  Activity 4 种启动模式

                 1  singleTop  如果顶部有该实例，则不实例化新的activity, 调用onNewIntent, 如果没有新实例，就创建新的;

                 2  singleTask 表示一个栈种只能有一个，如果一个栈中，有一个实例，若重新开启，则会把该实例上的所有Acitivity都destroy 掉  并调用onNewIntent;

                 3  standard 每次startActivity 就创建一个新实例

                 4  singleInstance 一个Activity 单独一个栈
				 
				 
			 13  Activity 启动流程


				   Activity.startActivity ---> Instrumentation.exeStartActivity --->
				   ActivityManagerNative.getDefault().startActivity(）；// getDefault返回的其实是
				   ActivityManagerProxy对象，调用binder.transact方法，将信息传给binder 驱动
				   
				   protected IActivityManager create() {
				   // 去servie manager 查询service,返回ServiceManager的Binder
				   IBinder b = ServiceManager.getService("activity");
					   if (false) {
						   Log.v("ActivityManager", "default service binder = " + b);
					   }
					   IActivityManager am = asInterface(b);
					   if (false) {
						   Log.v("ActivityManager", "default service = " + am);
					   }
				   return am;
			       }
			
				   public IActivityManager asInterface(IBinder obj) {
					   if (obj == null) {
						   return null;
					   }
					   // 从servicemanager 查找代理
					   IActivityManager in =
							   (IActivityManager)obj.queryLocalInterface(descriptor);
					   if (in != null) {
						   return in;
					   }
				
					   return new ActivityManagerProxy(obj);
				   }
				   
				   		
             14  Android 沙盒机制
		 
				  Android provides layer of protection in that it doesn’t give one app access to the resource of another app. 
				  This is known as the ‘sandbox’ where every app gets to play in its own sandbox and can’t use another app’s toys! 
				  Android does this by giving each app a unique user id (a UID) and by running that 
				  app as a separate process with that UID. Only processes with the same UIDs can share resources which, 
				  as each ID is uniquely assigned, means that no other apps have permission.
				  This means that if an app tries to do something it shouldn’t, like read the data from another app, 
				  or dial the phone (which is a separate application) 
				  then Android protects against this because the app doesn’t have the right privileges.
				  
				  
			  15  ArrayMap SparseArray 优缺点：
			  
				  1） 省内存，避免装箱操作，HashMap 的Entry 的Key 和 value 并不支持基本数据类型，
					  需要装箱操作
					  
                  2） 缺点查找速度慢，当只有几百个items 时候 和传统的hashmap 查找的差别不大于50%
					  当数据量变大的时候，差别就变得显著了。 设计思想是空间换时间，节省内存，
					  但是查找速度和传统的haspmap 相比变慢
					  
			 16  fragment 创建过程
			 
					大体生命周期：onAttach --> onCreate --> onCreateView --> onViewCreated --> onActivityCreated
							   ---->onResume-->onStop-->onDestroyView -->  onDestroy --onDetach;

					 1 ）FragmentController 包含的实现类和FragmentHostCallBack, Callback 包含 FragmentManager   会在onCreate
					调用attachHost container fragment 由于我们是最顶层，故传入null;
					
					 2）我们一般会在Activity 的onCreate 方法里调用
					添加fragment 的代码，先getFragmentManager,然后在beginTransaction, beginTraction是开启一个事务，这个事务是
					BackStackRecord, 实现了runnable的接口,继承了FragmentTransaction，
					
					 3）然后调用replace 的方法， 这个时候会生成一个操作节点，叫Op，op里面有一个cmd
				    字段表示操作节点,再把op插入到list里，
					
					 4）然后commit, commit 会执行BackStackRecord 的run方法， run方法里面，调用
				     moveToState，该方法内会根据操作的类型，以及Fragment 当前的状态执行相应生命周期的方法，当state 是Initializing 的话
					 就会执行fragment 的onAttach 和 onCreate 的方法
					 
		    17   Js 与 Android交互 方式有两种
 
                   1) 通过js 代码的方式

                   2) 通过自定义协议，Android在webview 过程中拦截url;

                   方式 1）双方可以互调 web可以调Android代码，android可以通过webview.loadUrl 方式调用web方法

                   方式 2）只能web调用android
				   
			 18   Activity的启动模式：
			 
			        standard：一个Task中可以有多个相同类型的Activity。注意，此处是相同类型的Activity，而不是同一个Activity对象。例如在Task中有A、B、C、D4个Activity，如果再启动A类Activity，

            				  Task就会变成A、B、C、D、A。最后一个A和第一个A是同一类型，却并非同一对象。另外，多个Task中也可以有同类型的Activity。

·                  singleTop：当某Task中有A、B、C、D4个Activity时，如果D想再启动一个D类型的Activity，那么Task将是什么样子呢？在singleTop模式下，Task中仍然是A、B、C、D，

                              只不过D的onNewIntent函数将被调用以处理这个新Intent，而在standard模式下，则Task将变成A、B、C、D、D，最后的D为新创建的D类型Activity对象。在singleTop这种模式下，
							 
							  只有目标Acitivity当前正好在栈顶时才有效，例如只有处于栈顶的D启动时才有用，如果D启动不处于栈顶的A、B、C等，则无效。

·                 singleTask：在这种启动模式下，该Activity只存在一个实例，并且将和一个Task绑定。当需要此Activity时，系统会以onNewIntent方式启动它，而不会新建Task和Activity。

                              注意，该Activity虽只有一个实例，但是在Task中除了它之外，还可以有其他的Activity。

·             singleInstance：它是singleTask的加强版，即一个Task只能有这么一个设置了singleInstance的Activity，不能再有别的Activity。

                               而在singleTask模式中，Task还可以有其他的Activity。
							   
				 其他FLAG:
				 
				   除了启动模式外，Android还有其他一些标志用于控制Activity及Task之间的关系。这里只列举一二，详细信息请参阅SDK文档中Intent的相关说明。

                   ·  FLAG_ACTIVITY_NEW_TASK：将目标Activity放到一个新的Task中。

                   ·  FLAG_ACTIVITY_CLEAR_TASK：当启动一个Activity时，先把和目标Activity有关联的Task“干掉“，然后启动一个新的Task，并把目标Activity放到新的Task中。
				      该标志必须和FLAG_ACTIVITY_NEW_TASK标志一起使用。

                   ·  FLAG_ACTIVITY_CLEAR_TOP：当启动一个不处于栈顶的Activity时候，先把排在它前面的Activity“干掉”。例如Task有A、B、C、D4个Activity，要要启动B，
				   
				      直接把C、D“干掉”，而不是新建一个B。
					  
			 19   WindowManagerService解析
			 
			 
			 20   Activity 显示过程：
			 
			      1） Activity 显示过程
			       
				      一般来说，应用程序的外表是通过Activity来展示的。那么，Activity是如何完成界面绘制工作的呢？根据前面所讲的知识，应用程序的显示和Surface有关，那么具体到Activity上，它和Surface又是什么关系呢？

                      本节就来讨论这些问题。首先从Activity的创建说起。 
					  
					  ActivityThread类中有一个handleLaunchActivity函数，它就是创建Activity的地方。一起来看这个函数，代码如下所示：
					  
					  private final void handleLaunchActivity(ActivityRecord r, Intent customIntent) {

                            //①performLaunchActivity返回一个Activity

                           Activity a = performLaunchActivity(r, customIntent);

 

                          if(a != null) {

                               r.createdConfig = new Configuration(mConfiguration);

                               Bundle oldState = r.state;

                               //②调用handleResumeActivity

                              handleResumeActivity(r.token, false, r.isForward);

                            }

                     }
					 
					 
					 handleLaunchActivity函数中列出了两个关键点，下面对其分别介绍。

                     1. 创建Activity

					     第一个关键函数performLaunchActivity返回一个Activity，这个Activity就是App中的那个Activity（仅考虑App中只有一个Activity的情况），它是怎么创建的呢？其代码如下所示：
						 
						 [-->ActivityThread.java]

                         private final Activity performLaunchActivity(ActivityRecord r,Intent customIntent) {

        

                              ActivityInfo aInfo = r.activityInfo;
  
                             //完成一些准备工作

                             //Activity定义在Activity.java中

                             Activity activity = null;

                             try {

                                 java.lang.ClassLoader cl = r.packageInfo.getClassLoader();

                                 /*

                                   mInstrumentation为Instrumentation类型，源文件为Instrumentation.java。

                                   它在newActivity函数中根据Activity的类名通过Java反射机制来创建对应的Activity，

                                   这个函数比较复杂，待会我们再分析它。

                                    */

                               activity = mInstrumentation.newActivity(

                               cl,component.getClassName(), r.intent);

                               r.intent.setExtrasClassLoader(cl);

                               if (r.state != null) {

                                r.state.setClassLoader(cl);

                                }

                               }catch (Exception e) {

                                   ......

                                }

 

                            try {

                              Application app = r.packageInfo.makeApplication(false,mInstrumentation);

 

                          if (activity != null) {

                                //在Activity中getContext函数返回的就是这个ContextImpl类型的对象

                                ContextImpl appContext = new ContextImpl();

                                  ......

                               //下面这个函数会调用Activity的onCreate函数

                                mInstrumentation.callActivityOnCreate(activity, r.state);

                               ......

                            return activity;

                        }
						
						好了，performLaunchActivity函数的作用明白了吧？

                        ·  根据类名以Java反射的方法创建一个Activity。

                        ·  调用Activity的onCreate函数，开始SDK中大书特书Activity的生命周期。

                           那么，在onCreate函数中，我们一般会做什么呢？在这个函数中，和UI相关的重要工作就是调用setContentView来设置UI的外观。接下去，
						   
						   需要看handleLaunchActivity中第二个关键函数handleResumeActivity。
						   
						   
						 2. 分析handleResumeActivity  
						 
                           上面已创建好了一个Activity，再来看handleResumeActivity。它的代码如下所示：

                           [-->ActivityThread.java]  
						   
						   final void handleResumeActivity(IBinder token,boolean clearHide,boolean isForward) {

                                 boolean willBeVisible = !a.mStartedActivity;

          

                              if (r.window == null && !a.mFinished&& willBeVisible) {

                                   r.window= r.activity.getWindow();

                                  //①获得一个View对象

                                  Viewdecor = r.window.getDecorView();

                                 decor.setVisibility(View.INVISIBLE);

                                //②获得ViewManager对象

                                 ViewManager wm = a.getWindowManager();

                                  ......

                               //③把刚才的decor对象加入到ViewManager中

                                 wm.addView(decor,l);

                                }

                                 ......//其他处理

                             }
							 
							上面有三个关键点。这些关键点似乎已经和UI部分（如View、Window）有联系了。那么这些联系是在什么时候建立的呢？在分析上面代码中的三个关键点之前，请大家想想在前面的过程中，哪些地方会和UI挂上钩呢？

                            ·  答案就在onCreate函数中，Activity一般都在这个函数中通过setContentView设置UI界面。

                               看来，必须先分析setContentView，才能继续后面的征程。
							   
							   
						   3. 分析setContentView

						      setContentView有好几个同名函数，现在只看其中的一个就可以了。代码如下所示：

                              [-->Activity.java]
							  
							  public void setContentView(View view) {

                                 //getWindow返回的是什么呢？一起来看看。

                                 getWindow().setContentView(view);

                              }
							  
							  
							  public Window getWindow() {

                               return mWindow; //返回一个类型为Window的mWindow，它是什么？

                              }
							  
							  
							  根据上面的介绍，大家可能会产生两个疑问：

                              ·  Window是一个抽象类，它实际的对象到底是什么类型？

                              ·  Window Manager究竟是什么？

                                 如果能有这样的疑问，就说明我们非常细心了。下面试来解决这两个问题。

                                （1）Activity的Window

								     据上文讲解可知，Window是一个抽象类。它实际的对象到底属于什么类型？先回到Activity创建的地方去看看。下面正是创建Activity时的代码，可当时没有深入地分析。
									 
									 activity = mInstrumentation.newActivity(

                                     cl,component.getClassName(), r.intent);
									 
									 代码中调用了Instrumentation的newActivity，再去那里看看。
									 
									 public Activity newActivity(Class<?>clazz, Context context,

                                            IBinder token, Application application, Intent intent,

                                             ActivityInfo info, CharSequencetitle, Activity parent,

                                             String id,Object lastNonConfigurationInstance) throws InstantiationException, IllegalAccessException{

       

                                              Activity activity = (Activity)clazz.newInstance();
 
                                              ActivityThread aThread = null;

                                             //关键函数attach!!

                                           activity.attach(context, aThread, this, token, application, intent,

                                          info, title,parent, id, lastNonConfigurationInstance,

                                        new Configuration());

                                        return activity;

                                    }
									
									看到关键函数attach了吧？Window的真相马上就要揭晓了，让我们用咆哮体②来表达内心的激动之情吧！！！！
									
									[-->Activity.java]
									
									final void attach(Context context,ActivityThread aThread,Instrumentation instr, IBinder token, int ident,

                                                        Application application, Intent intent, ActivityInfo info, CharSequence title, Activity parent, String id,

                                                        Object lastNonConfigurationInstance,HashMap<String,Object> lastNonConfigurationChildInstances,

                                                           Configuration config) {

                                                   ......

                                                  //利用PolicyManager来创建Window对象

                                                mWindow = PolicyManager.makeNewWindow(this);

                                                mWindow.setCallback(this);

                                                  ......

                                             //创建WindowManager对象

                                            mWindow.setWindowManager(null, mToken, mComponent.flattenToString());

                                          if(mParent != null) {

                                          mWindow.setContainer(mParent.getWindow());

                                         }
 
                                       //保存这个WindowManager对象

                                        mWindowManager = mWindow.getWindowManager();

                                        mCurrentConfig = config;

                                     }
							  
					            （2）水面下的冰山——PolicyManager
								 
								      PolicyManager定义于PolicyManager.java文件，该文件在一个非常独立的目录下，现将其单独列出来：

                                      下面来看这个PolicyManager，它比较简单。

                                      [-->PolicyManager.java]
									  
									   public final class PolicyManager {

                                        private static final String POLICY_IMPL_CLASS_NAME =

                                                  "com.android.internal.policy.impl.Policy";

 

                                        private static final IPolicy sPolicy;

 

                                       static{

                                        //

                                      try {

                                       Class policyClass = Class.forName(POLICY_IMPL_CLASS_NAME);

                                        //创建Policy对象

                                       sPolicy = (IPolicy)policyClass.newInstance();

                                       }catch (ClassNotFoundException ex) {

                                        ......

                                       }

 

                                     private PolicyManager() {}

 

                                     //通过Policy对象的makeNewWindow创建一个Window

                                     publicstatic Window makeNewWindow(Context context) {

                                        return sPolicy.makeNewWindow(context);

                                     }

                                      ......
 
                                     }
									 
							  （3） 真正的Window
                                   
								      Policy类型的定义代码如下所示：

                                      [-->Policy.java]

                                      public class Policy implements IPolicy {

                                           private static final String TAG = "PhonePolicy";

 

                                           private static final String[] preload_classes = {

                                              "com.android.internal.policy.impl.PhoneLayoutInflater",

                                              "com.android.internal.policy.impl.PhoneWindow",

                                              "com.android.internal.policy.impl.PhoneWindow$1",

                                              "com.android.internal.policy.impl.PhoneWindow$ContextMenuCallback",

                                              "com.android.internal.policy.impl.PhoneWindow$DecorView",

                                              "com.android.internal.policy.impl.PhoneWindow$PanelFeatureState",

                                              "com.android.internal.policy.impl.PhoneWindow$PanelFeatureState$SavedState",

                                            };

 

                                        static{

                                       //加载所有的类

                                      for (String s : preload_classes) {

                                       try {

                                         Class.forName(s);

                                         } catch (ClassNotFoundException ex) {

                                         ......

                                        }

                                      }

                                    }

 

                                     public PhoneWindow makeNewWindow(Contextcontext) {

                                          //makeNewWindow返回的是PhoneWindow对象

                                     return new PhoneWindow(context);

                                       }

 

   

                                      }

                                      至此，终于知道了代码：
 
                                      mWindow = PolicyManager.makeNewWindow(this);

                                      返回的Window，原来是一个PhoneWindow对象。它的定义在PhoneWindow.java中。

                                      mWindow的真实身份搞清楚了，还剩下个WindowManager。现在就来揭示其真面目。
									  
							 （4）真正的WindowManager
							 
                                      先看WindowManager创建的代码，如下所示：

                                      [-->Activity.java]

                                      ......//创建mWindow对象

                                     //调用mWindow的setWindowManager函数

                                     mWindow.setWindowManager(null, mToken,mComponent.flattenToString());

                                     .....

                                    上面的函数设置了PhoneWindow的WindowManager，不过第一个参数是null，这是什么意思？在回答此问题之前，先来看PhoneWindow的定义，它是从Window类派生。

                                     [-->PhoneWindow.java::PhoneWindow定义]

                                      public class PhoneWindow extends Windowimplements MenuBuilder.Callback

                                         前面调用的setWindowManager函数，其实是由PhoneWindow的父类Window类来实现的，来看其代码，如下所示：

                                         [-->Window.java]
										 
										  public void setWindowManager(WindowManagerwm,IBinder appToken, String appName) {     //注意，传入的wm值为null

                                                  mAppToken = appToken;

                                                 mAppName = appName;

                                         if(wm == null) {

                                         //如果wm为空的话，则创建WindowManagerImpl对象

                                          wm = WindowManagerImpl.getDefault();

                                        }

                                        //mWindowManager是一个LocalWindowManager

                                         mWindowManager = new LocalWindowManager(wm);

                                       }

                                       LocalWindowManager是在Window中定义的内部类，请看它的构造函数，其定义如下所示：

                                      [-->Window.java::LocalWindowManager定义]

                                      private class LocalWindowManager implements WindowManager {

                                             LocalWindowManager(WindowManager wm) {

                                             mWindowManager = wm;//还好，只是简单地保存了传入的wm参数

                                             mDefaultDisplay = mContext.getResources().getDefaultDisplay(

                                                         mWindowManager.getDefaultDisplay());

                                        }

                                             ......

                                       如上面代码所示，LocalWindowManager将保存一个WindowManager类型的对象，这个对象的实际类型是WindowManagerImpl。而WindowManagerImpl又是什么呢？来看它的代码，如下所示：

                                      [-->WindowManagerImpl.java]

                                     public class WindowManagerImpl implements WindowManager {

                                            ......

 

                                       public static WindowManagerImpl getDefault()

                                                  {

                                        return mWindowManager; //返回的就是WindowManagerImpl对象

                                      }

                                      private static WindowManagerImpl mWindowManager= new WindowManagerImpl();

                                      }
							4. 重回handleResumeActivity

							   看完setContentView的分析后，不知大家是否还记得这样一个问题：为什么要分析这个setContentView函数？在继续前行之前，先来回顾一下被setContentView打断的流程。

                               当时，我们正在分析handleResumeActivity，代码如下所示：

                               [-->ActivityThread.java]		

                                final void handleResumeActivity(IBinder token,boolean clearHide,boolean isForward) {
									

                                 booleanwillBeVisible = !a.mStartedActivity;

                                    ......

                                if (r.window == null && !a.mFinished&& willBeVisible) {

                                    r.window= r.activity.getWindow();

                                    //①获得一个View对象。现在知道这个view就是DecorView

                                    Viewdecor = r.window.getDecorView();

                                     decor.setVisibility(View.INVISIBLE);

                                    //②获得ViewManager对象,这个wm就是LocalWindowManager

                                    ViewManagerwm = a.getWindowManager();

                                    WindowManager.LayoutParamsl = r.window.getAttributes();

                                    a.mDecor= decor;

                                   l.type =WindowManager.LayoutParams.TYPE_BASE_APPLICATION;

                                  if(a.mVisibleFromClient) {

                                    a.mWindowAdded= true;

                                  //③把刚才的decor对象加入到ViewManager中

                                    wm.addView(decor,l);  //非常关键的代码

                                  }

                                  ......//其他处理

                                 }
								 

                                 在上面的代码中，由于出现了多个之前不熟悉的东西，如View、ViewManager等，而这些东西的来源又和setContentView有关，所以我们才转而去分析setContentView了。想起来了吧？

                                 由于代码比较长，跳转关系也很多，在分析代码时，请读者把握流程，在大脑中建立一个代码分析的堆栈。

                                 下面就从addView的分析开始。如前面所介绍的，它的调用方法是：

                                 wm.addView(decor, l);//wm类型实际是LocalWindowManager

                                 来看这个addView函数，它的代码如下所示：

                                 [-->Window.javaLocalWindowManager]

                                   public final void addView(View view,ViewGroup.LayoutParams params) {

  

                                     WindowManager.LayoutParams wp =(WindowManager.LayoutParams)params;

                                     CharSequence curTitle = wp.getTitle();

                                     ...... //做一些操作，可以不管它

                                      //还记得前面提到过的Proxy模式吗？mWindowManager对象实际上是WindowManagerImpl类型

                                      mWindowManager.addView(view, params);

                                    }

                                  看来，要搞清楚这个addView函数还是比较麻烦的，因为现在必须到WindowManagerImpl中去看看。它的代码如下所示：
								  

                                 [-->WindowManagerImpl.java]
								 

                                 private void addView(View view,ViewGroup.LayoutParams params, boolean nest){

                                         ViewRootroot; //ViewRoot，幕后的主角终于登场了！

                                             synchronized(this) {

                                               //①创建ViewRoot

                                              root =new ViewRoot(view.getContext());

                                              root.mAddNesting = 1;

                                              view.setLayoutParams(wparams);

           

                                            if(mViews == null) {

                                               index = 1;

                                               mViews = new View[1];

                                               mRoots= new ViewRoot[1];

                                               mParams = new WindowManager.LayoutParams[1];

                                            } else{

                                                ......
        
                                            }

                                             index--;

                                             mViews[index]= view;

                                             mRoots[index]= root;//保存这个root

                                             mParams[index]= wparams;

 

                                             //②setView,其中view是刚才我们介绍的DecorView

                                             root.setView(view,wparams, panelParentView);//

                                 }

                                   “ViewRoot，ViewRoot ....”，主角终于出场了！即使没介绍它的真实身份，不禁也想欢呼几声。可为避免高兴得过早，还是应该先冷静地分析一下它。这里，列出了ViewRoot的两个重要关键点。

                            （1）ViewRoot是什么？
  
                                    ViewRoot是什么？看起来好像和View有些许关系，至少名字非常像。事实上，它的确和View有关系，因为它实现了ViewParent接口。SDK的文档中有关于ViewParent的介绍。
									
									但它和Android基本绘图单元中的View却不太一样，比如：ViewParent不处理绘画，因为它没有onDraw函数。

                                    如上所述，ViewParent和绘画没有关系，那么，它的作用是什么？先来看它的代码，如下所示：

                                   [-->ViewRoot.java::ViewRoot定义]

                                   public final class ViewRoot extends Handlerimplements ViewParent,

                                              View.AttachInfo.Callbacks //从Handler类派生

                                            {

                                            private final Surface mSurface = new Surface();//这里创建了一个Surface对象

                                           final W mWindow; //这个是什么？

                                            View mView;

                                        } 
                            
							（2）神笔马良乎？
                                
								这里冒出来一个Surface类。它是什么？在回答此问题之前，先来考虑这样一个问题：

                                ·  前文介绍的View、DecorView等都是UI单元，这些UI单元的绘画工作都在onDraw函数中完成。如果把onDraw想象成画图过程，那么画布是什么？

                                   Android肯定不是“马良”，它也没有那支可以在任何物体上作画的“神笔”，所以我们需要一块实实在在的画布，这块画布就是Surface。SDK文档对Surface类的说明是：
								   
								   Handle on to a raw buffer thatis being managed by the screen compositor。这句话的意思是：

                                ·  有一块Raw buffer，至于是内存还是显存，不必管它。

                                ·  Surface操作这块Raw buffer。

                                ·  Screen compositor（其实就是SurfaceFlinger）管理这块Raw buffer。  										

								结合之前所讲的知识，图8-5清晰地传达了如下几条信息：

                                 ·  ViewRoot有一个成员变量mSurface，它是Surface类型，它和一块Raw Buffer有关联。

                                 ·  ViewRoot是一个ViewParent，它的子View的绘画操作，是在画布Surface上展开的。

                                 ·  Surface和SurfaceFlinger有交互，这非常类似AudioTrack和AudioFlinger之间的交互。
							   
						 （3）  ViewRoot的创建和对setView的分析
							
							
                               来分析ViewRoot的构造。关于它所包含内容，代码如下所示：
 
                               [-->ViewRoot.java]

                               public ViewRoot(Context context) {

                                      super();

                                    ....
 
                                    // getWindowSession？我们进去看看

                                  getWindowSession(context.getMainLooper());

                                  ......//ViewRoot的mWindow是一个W类型，注意它不是Window类型，而是IWindow类型

                                  mWindow= new W(this, context);

                                }

                               getWindowsession函数，将建立Activity的ViewRoot和WindowManagerService的关系。代码如下所示：

                                [-->ViewRoot.java]

                               public static IWindowSessiongetWindowSession(Looper mainLooper) {

                                      synchronized (mStaticInit) {

                                        if(!mInitialized) {

                                             try {

                                        InputMethodManagerimm =

                                                InputMethodManager.getInstance(mainLooper);

                                     //下面这个函数先得到WindowManagerService的Binder代理，然后调用它的openSession

                                     sWindowSession = IWindowManager.Stub.asInterface(

                                              ServiceManager.getService("window"))

                                             .openSession(imm.getClient(), imm.getInputContext());

                                             mInitialized = true;

                                         } catch (RemoteException e) {

                                              }

                                            }

                                            return sWindowSession;

                                      }

                                  }
							

                             WindowSession？WindowManagerService？第一次看到这些东西时，我快疯了。复杂，太复杂，无比复杂！要攻克这些难题，应先来回顾一下与Zygote相关的知识：

                           ·  WindowManagerService（以后简称WMS）由System_Server进程启动，SurfaceFlinger服务也在这个进程中。

                              看来，Activity的显示还不单纯是它自己的事，还需要和WMS建立联系才行。继续看。先看setView的处理。这个函数很复杂，注意其中关键的几句。

                              openSession的操作是一个使用Binder通信的跨进程调用，暂且记住这个函数，在精简流程之后再来分析。

                              代码如下所示：

                              [-->ViewRoot.java]

                               public void setView(View view, WindowManager.LayoutParamsattrs,
 
                                              View panelParentView){//第一个参数view是DecorView

                                         ......

                                    mView= view;//保存这个view

                                  synchronized (this) {

                                         requestLayout(); //待会先看看这个。

                                 try {

                                      //调用IWindowSession的add函数，第一个参数是mWindow

                                       res =sWindowSession.add(mWindow, mWindowAttributes,

                                           getHostVisibility(), mAttachInfo.mContentInsets);
 
                                }

                                 ......

                              }

                             ViewRoot的setView函数做了三件事：

                            ·  保存传入的view参数为mView，这个mView指向PhoneWindow的DecorView。

                            ·  调用requestLayout。

                            ·  调用IWindowSession的add函数，这是一个跨进程的Binder通信，第一个参数是mWindow，它是W类型，从IWindow.stub派生。

                                先来看这个requestLayout函数，它非常简单，就是往handler中发送了一个消息。注意，ViewRoot是从Handler派生的，所以这个消息最后会由ViewRoot自己处理，代码如下所示：

                             [-->ViewRoot.java]

                              public void requestLayout() {

                                  checkThread();

                                  mLayoutRequested = true;

                                  scheduleTraversals();

                              }

                             public void scheduleTraversals() {

                                 if(!mTraversalScheduled) {

                                mTraversalScheduled = true;

                                   sendEmptyMessage(DO_TRAVERSAL); //发送DO_TRAVERSAL消息

                               }

                            }

                           好，requestLayout分析完毕。

                           从上面的代码中可发现，ViewRoot和远端进程SystemServer的WMS有交互，先来总结一下它和WMS的交互流程：

                            ·  ViewRoot调用openSession，得到一个IWindowSession对象。

                            ·  调用WindowSession对象的add函数，把一个W类型的mWindow对象做为参数传入。     

                          
                        (4)  Activity的绘制：

                              ViewRoot的setView函数中，会有一个requestLayout。根据前面的分析可知，它会向ViewRoot发送一个DO_TRAVERSAL消息，来看它的handleMessage函数，代码如下所示：

                              [-->ViewRoot.java]

                              public void handleMessage(Message msg) {

                                  switch (msg.what) {

                                           ......

                                  case DO_TRAVERSAL:

                                           ......

                                       performTraversals();//调用performTraversals函数

                                   ......

                                 break;

                                 ......

                                }

                              }

                            再去看performTraversals函数，这个函数比较复杂，先只看它的关键部分，代码如下所示：

                            [-->ViewRoot.java]

                            private void performTraversals() {

                                   finalView host = mView;//还记得这mView吗？它就是DecorView喔
                                  
								  boolean initialized = false;

                                  boolean contentInsetsChanged = false;

                                  boolean visibleInsetsChanged;

                                 try {

                                   relayoutResult= //①关键函数relayoutWindow

                                   relayoutWindow(params, viewVisibility,insetsPending);

                                  }

                                 ......

                                draw(fullRedrawNeeded);// ②开始绘制

                                ......

                             }

                              1. relayoutWindow的分析

						       performTraversals函数比较复杂，暂时只关注其中的两个函数relayoutWindow和draw即可。先看第一个relayoutWindow，代码如下所示：

                                 [-->ViewRoot.java]

                                 private intrelayoutWindow(WindowManager.LayoutParams params,

                                    int viewVisibility, boolean insetsPending)throws RemoteException {

      

                                     //原来是调用IWindowSession的relayOut，暂且记住这个调用

                                    int relayoutResult = sWindowSession.relayout(

                                         mWindow, params,

                                       (int) (mView.mMeasuredWidth * appScale + 0.5f),

                                        (int) (mView.mMeasuredHeight * appScale + 0.5f),

                                       viewVisibility, insetsPending, mWinFrame,

                                       mPendingContentInsets, mPendingVisibleInsets,

                                      mPendingConfiguration, mSurface); mSurface做为参数传进去了。
									  
						 	  

                                  }

                                  ......

                                }

                                relayoutWindow中会调用IWindowSession的relayout函数，暂且记住这个调用，在精简流程后再进行分析。

                             2. draw的分析

							    再来看draw函数。这个函数非常重要，它可是Acitivity漂亮脸蛋的塑造大师啊，代码如下所示：

                               [-->ViewRoot.java]

                                private void draw(boolean fullRedrawNeeded) {

                                     Surface surface = mSurface;//mSurface是ViewRoot的成员变量

                                      ......

                                    Canvas canvas;

                                  try {

                                   int left = dirty.left;
 
                                   int top = dirty.top;

                                   int right = dirty.right;

                                   int bottom = dirty.bottom;

                                   //从mSurface中lock一块Canvas

                                   canvas = surface.lockCanvas(dirty);

                                   ......

                                    mView.draw(canvas);//调用DecorView的draw函数，canvas就是画布的意思啦！

                                   ......

                                    //unlock画布，屏幕上马上就会见到漂亮宝贝的长相了。

                                   surface.unlockCanvasAndPost(canvas);

                                }

                                   ......

                                  }

                                UI的显示好像很简单嘛！真的是这样的吗？在揭露这个“惊天秘密”之前我们先总结一下Activity的显示流程。						 
							        
				        （5） Activity显示自己的总结：
						
						       ActivityThread  执行performLaunchActivity  在此方法里创建新的activity  用的是 Instrumentation的new Activity
							                                        
																	  |
							                                          |
					                                                  V
							  在 Instrumentation newActivity里会调用attach 方法 在此方法里创建Window 调用的是PolicyManager.makeNewWindow 返回的是PhoneWindow对象，并创建decorView
							                                          |
							                                          |
					                                                  V
							  执行performLaunchActivity，做了一些初始化工作以后，调用Activity onCreate 方法，的setContentView 方法 将contentView set到decorView里
							  
							                                          |
							                                          |
					                                                  V
							  onResume:  handleOnResumeActivity 里 调用WindowManager.addView 方法将 decorView 压入窗口，addView 的方法里会调用到ViewRootImp这个类 在这个类里进行view 的 measure layout draw 
							  
							  操作，然后将view显示出来
							  
							  
			  21  Surface类 ；

                     在Android中，Surface系统工作时，会由SurfaceFlinger对这些按照Z轴排好序的显示层进行图像混合，混合后的图像就是在屏幕上看到的美妙画面了。这种按Z轴排序的方式符合我们在日常生活中的体验，例如前面的物体会遮挡住后面的物体。

                     注意，Surface系统中定义了一个名为Layer类型的类，为了区分广义概念上的Layer和代码中的Layer，这里称广义层的Layer为显示层，以免混淆。

                     Surface系统提供了三种属性，一共四种不同的显示层。简单介绍一下：

                     ·  第一种属性是eFXSurfaceNormal属性，大多数的UI界面使用的就是这种属性。它有两种模式：

                       1）Normal模式，这种模式的数据，是通过前面的mView.draw(canvas)画上去的。这也是绝大多数UI所采用的方式。

                       2）PushBuffer模式，这种模式对应于视频播放、摄像机摄录/预览等应用场景。以摄像机为例，当摄像机运行时，来自Camera的预览数据直接push到Buffer中，无须应用层自己再去draw了。

                    ·  第二种属性是eFXSurfaceBlur属性，这种属性的UI有点朦胧美，看起来很像隔着一层毛玻璃。

                    ·  第三种属性是eFXSurfaceDim属性，这种属性的UI看起来有点暗，好像隔了一层深色玻璃。从视觉上讲，虽然它的UI看起来有点暗，但并不模糊。而eFXSurfaceBlur不仅暗，还有些模糊。	

                    （1）FrameBuffer的介绍

					    FrameBuffer的中文名叫帧缓冲，它实际上包括两个不同的方面：

                       ·  Frame：帧，就是指一幅图像。在屏幕上看到的那幅图像就是一帧。

                       ·  Buffer：缓冲，就是一段存储区域，可这个区域存储的是帧。

                          FrameBuffer的概念很清晰，它就是一个存储图形/图像帧数据的缓冲。这个缓冲来自哪里？理解这个问题，需要简单介绍一下Linux平台的虚拟显示设备FrameBuffer Device（简称FBD）。FBD是Linux系统中的一个虚拟设备，
						  
						  设备文件对应为/dev/fb%d（比如/dev/fb0）。这个虚拟设备将不同硬件厂商实现的真实设备统一在一个框架下，这样应用层就可以通过标准的接口进行图形/图像的输入和输出了。	

                    （2）PageFlipping

					    图形/图像数据和音频数据不太一样，我们一般把音频数据叫音频流，它是没有边界的, 而图形/图像数据是一帧一帧的，是有边界的。这一点非常类似UDP和TCP之间的区别。所以在图形/图像数据的生产/消费过程中，人们使用了一种叫PageFlipping的技术。

                        PageFlipping的中文名叫画面交换，其操作过程如下所示：

                       ·  分配一个能容纳两帧数据的缓冲，前面一个缓冲叫FrontBuffer，后面一个缓冲叫BackBuffer。

                       ·  消费者使用FrontBuffer中的旧数据，而生产者用新数据填充BackBuffer，二者互不干扰。

                       ·  当需要更新显示时，BackBuffer变成FrontBuffer，FrontBuffer变成BackBuffer。如此循环，这样就总能显示最新的内容了。这个过程很像我们平常的翻书动作，所以它被形象地称为PageFlipping。

                         说白了，PageFlipping其实就是使用了一个只有两个成员的帧缓冲队列，以后在分析数据传输的时候还会见到诸如dequeue和queue的操作。

				  
				  22 关于WindowManagerService
				  
				     1) 总结在客户端创建一个窗口的步骤：

                       ·  获取IWindowSession和WMS实例。客户端可以通过IWindowSession向WMS发送请求。

                       ·  创建并初始化WindowManager.LayoutParams。注意这里是WindowManager下的LayoutParams，它继承自ViewGroup.LayoutParams类，并扩展了一些窗口相关的属性。其中最重要的是type属性。
					  
					      这个属性描述了窗口的类型，而窗口类型正是WMS对多个窗口进行ZOrder排序的依据。

                       ·  向WMS添加一个窗口令牌（WindowToken）。本章后续将分析窗口令牌的概念，目前读者只要知道，窗口令牌描述了一个显示行为，并且WMS要求每一个窗口必须隶属于某一个显示令牌。

                      ·  向WMS添加一个窗口。必须在LayoutParams中指明此窗口所隶属于的窗口令牌，否则在某些情况下添加操作会失败。在SampleWindow中，不设置令牌也可成功完成添加操作，
					     因为窗口的类型被设为TYPE_SYSTEM_ALERT，它是系统窗口的一种。而对于系统窗口，WMS会自动为其创建显示令牌，故无需客户端操心。此话题将会在后文进行更具体的讨论。

                      ·  向WMS申请对窗口进行重新布局（relayout）。所谓的重新布局，就是根据窗口新的属性去调整其Surface相关的属性，或者重新创建一个Surface（例如窗口尺寸变化导致之前的Surface不满足要求）。
					  
					    向WMS添加一个窗口之后，其仅仅是将它在WMS中进行了注册而已。只有经过重新布局之后，窗口才拥有WMS为其分配的画布。有了画布，窗口之后就可以随时进行绘制工作了。

                    2) 而窗口的绘制过程如下：

                        ·  通过Surface.lock()函数获取可以在其上作画的Canvas实例。

                        ·  使用Canvas实例进行作画。

                        ·  通过Surface.unlockCanvasAndPost()函数提交绘制结果。
							  
							  
		  六： JAVA 语言类
		   
		      1   apt 动态编译
	   
	             APT(Annotation processing tool) 是一种处理注释的工具,它对源代码文件进行检测找出其中的Annotation，使用Annotation进行额外的处理。
                 Annotation处理器在处理Annotation时可以根据源文件中的Annotation生成额外的源文件和其它的文件(文件具体内容由Annotation处理器的编写者决定),
				 APT还会编译生成的源文件和原来的源文件，将它们一起生成class文件.使用APT主要的目的是简化开发者的工作量。
                 因为APT可以编译程序源代码的同时，生成一些附属文件(比如源文件类文件程序发布描述文件等)，这些附属文件的内容也都是
				 与源代码相关的，换句话说，使用APT可以代替传统的对代码信息和附属文件的维护工作。
				 
			  
              2  注解Retention的种类：
			  
				 Source 只保留在源码中 编译会被忽视 比如我们通常用的deprecated 还有 surpressing warning
				 CLAss, 只保存在字节码文件中，不会在VM中运行，比如butternife 中的 注解
				 runtime,保存到运行时，比如retrofit 的POST,GET

			  3  类的加载机制
		 
			      1、Bootstrap Loader（启动类加载器）：加载System.getProperty("sun.boot.class.path")所指定的路径或jar。
                  2、Extended Loader（标准扩展类加载器ExtClassLoader）：加载System.getProperty("java.ext.dirs")所指定的路径或jar。
				     在使用Java运行程序时，也可以指定其搜索路径，例如：java -Djava.ext.dirs=d:\projects\testproj\classes HelloWorld
				
				  1、命令行启动应用时候由JVM初始化加载
				  1、命令行启动应用时候由JVM初始化加载
                  2、通过Class.forName()方法动态加载
                  3、通过ClassLoader.loadClass()方法动态加载
                  4、同一个ClassLoader加载的类文件，只有一个Class实例。但是，如果同一个类文件被不同的ClassLoader载入，则会有两份不同的
				     ClassLoader实例（前提是着两个类加载器不能用相同的父类加载器）
				  5 自定义加载器要重写findclass
 
                  3、AppClass Loader（系统类加载器AppClassLoader）：加载System.getProperty("java.class.path")所指定的路径或jar。
				  在使用Java运行程序时，也可以加上-cp来覆盖原有的Classpath设置，例如： java -cp ./lavasoft/classes HelloWorld
		     
			  4   双亲委托机制以及原因
			  
				    双亲委托模型的工作过程是：如果一个类加载器收到了类加载的请求，它首先不会自己去尝试加载这个类，而是把这个请求委托给
					父类加载器去完成，每一个层次的类加载器都是如此，因此所有的加载请求最终都应该传送到顶层的启动类加载器中，
					只有当父类加载器反馈自己无法完成这个加载请求（它的搜索范围中没有找到所需要加载的类）时，子加载器才会尝试自己去加载
					使用双亲委托机制的好处是：能够有效确保一个类的全局唯一性，当程序中出现多个限定名相同的类时，类加载器在执行加载时，
					始终只会加载其中的某一个类。
					
					使用双亲委托模型来组织类加载器之间的关系，有一个显而易见的好处就是Java类随着它的类加载器一起具备了一种带有优先级的层次关系。
					例如类java.lang.Object，它存放在rt.jar之中，无论哪一个类加载器要加载这个类，最终都是委托给处于模型最顶端的启动类加载器进行加载，
					因此Object类在程序的各种加载器环境中都是同一个类。相反，如果没有使用双亲委托模型，由各个类加载器自行去加载的话，
					如果用户自己编写了一个称为java.lang.Object的类，并放在程序的ClassPath中，那系统中将会出现多个不同的Object类，
					Java类型体系中最基础的行为也就无法保证，应用程序也将会变得一片混乱。
					双亲机制是为了保证java核心库的类型安全，不会出现用户自己能定义java.lang.Object类的情况。
					
				    在JVM中表示两个class对象是否为同一个类对象存在两个必要条件

                    类的完整类名必须一致，包括包名，加载这个类的ClassLoader(指ClassLoader实例对象)必须相同。
					
			  4   实现自己的类加载方法，主要要注意以下几个重要方法：
			  
			       1) loadClass(): 
			  
			       该方法加载指定名称（包括包名）的二进制类型，该方法在JDK1.2之后不再建议用户重写但用户可以直接调用该方法，loadClass()方法是ClassLoader类自己实现的，
				   
				   该方法中的逻辑就是双亲委派模式的实现，其源码如下，loadClass(String name, boolean resolve)是一个重载方法，
				   
				   resolve参数代表是否生成class对象的同时进行解析相关操作。
				   
				   protected Class<?> loadClass(String name, boolean resolve)throws ClassNotFoundException {
					   
                               synchronized (getClassLoadingLock(name)) {
                            // 先从缓存查找该class对象，找到就不用重新加载
                          Class<?> c = findLoadedClass(name);
                          if (c == null) {
                               long t0 = System.nanoTime();
                              try {
                                 if (parent != null) {
                                    //如果找不到，则委托给父类加载器去加载
                                      c = parent.loadClass(name, false);
                                } else {
                                    //如果没有父类，则委托给启动加载器去加载
                                    c = findBootstrapClassOrNull(name);
                            }
                           } catch (ClassNotFoundException e) {
                              // ClassNotFoundException thrown if class not found
                              // from the non-null parent class loader
                          }

                       if (c == null) {
                                 // If still not found, then invoke findClass in order
                               // 如果都没有找到，则通过自定义实现的findClass去查找并加载
                              c = findClass(name);

                              // this is the defining class loader; record the stats
                               sun.misc.PerfCounter.getParentDelegationTime().addTime(t1 - t0);
                               sun.misc.PerfCounter.getFindClassTime().addElapsedTimeFrom(t1);
                               sun.misc.PerfCounter.getFindClasses().increment();
                        }
                      }
                        if (resolve) {//是否需要在加载时进行解析
                              resolveClass(c);
                            }
                            return c;
                         }
                       }
				   
				   2) findClass(): 
				   
				       在JDK1.2之前，在自定义类加载时，总会去继承ClassLoader类并重写loadClass方法，从而实现自定义的类加载类，
					   
					   但是在JDK1.2之后已不再建议用户去覆盖loadClass()方法，而是建议把自定义的类加载逻辑写在findClass()方法中，
					   
					   从前面的分析可知，findClass()方法是在loadClass()方法中被调用的，当loadClass()方法中父加载器加载失败后，
					   
					   则会调用自己的findClass()方法来完成类加载，这样就可以保证自定义的类加载器也符合双亲委托模式。
					   
					   需要注意的是ClassLoader类中并没有实现findClass()方法的具体代码逻辑，取而代之的是抛出ClassNotFoundException异常，
					   
					   同时应该知道的是findClass方法通常是和defineClass方法一起使用的(稍后会分析)
					    
						//直接抛出异常
                        protected Class<?> findClass(String name) throws ClassNotFoundException {
							
                           throw new ClassNotFoundException(name);
						   
                        }
					   
				   
				   3) defineClass()
				         
						 方法是用来将byte字节流解析成JVM能够识别的Class对象(ClassLoader中已实现该方法逻辑)，通过这个方法不仅能够通过class文件实例化class对象，
					  
					     也可以通过其他方式实例化class对象，如通过网络接收一个类的字节码，然后转换为byte字节流创建对应的Class对象，
					  
					     defineClass()方法通常与findClass()方法一起使用，一般情况下，在自定义类加载器时，
					   
					     会直接覆盖ClassLoader的findClass()方法并编写加载规则，取得要加载类的字节码后转换成流，
					   
					     然后调用defineClass()方法生成类的Class对象，简单例子如下
						 
						 protected Class<?> findClass(String name) throws ClassNotFoundException {
                                // 获取类的字节数组
                           byte[] classData = getClassData(name);  
                          if (classData == null) {
                              throw new ClassNotFoundException();
                          } else {
                             //使用defineClass生成class对象
                           return defineClass(name, classData, 0, classData.length);
                         }
						 
						 
				    4)  resolveClass(Class≺?≻ c) 
					
					     使用该方法可以使用类的Class对象创建完成也同时被解析。前面我们说链接阶段主要是对字节码进行验证，
						 
						 为类变量分配内存并设置初始值同时将字节码文件中的符号引用转换为直接引用。
						 
						 
  }
					
					
			   5  类的初始化顺序
				  
				    属性、方法、构造方法和自由块都是类中的成员，在创建类的对象时，类中各成员的执行顺序：
                      1. 父类静态成员和静态初始化快，按在代码中出现的顺序依次执行。 // 2018-7-18复习出现概念性错误，以为成员和块不是同时实例化
                      2. 子类静态成员和静态初始化块，按在代码中出现的顺序依次执行。
                      3. 父类的实例成员和实例初始化块，按在代码中出现的顺序依次执行。
                      4. 执行父类的构造方法。                                       // 成员变量会和构造函数一块执行
                      5. 子类实例成员和实例初始化块，按在代码中出现的顺序依次执行。
                      6. 执行子类的构造方法。
					   
	                   class SingleTon {
	                         
							 private static SingleTon singleTon = new SingleTon();
	                         public static int count1;
	                         public static int count2 = 0;
       
	                         private SingleTon() {
	                             	count1++;
	                             	count2++;
	                         }

	                         public static SingleTon getInstance() {
		                            return singleTon;
	                         }

                             public static void main(String[] args) {
		                           SingleTon singleTon = SingleTon.getInstance();
	                                System.out.println("count1=" + singleTon.count1);
		                           System.out.println("count2=" + singleTon.count2);
	                          }
                         }
                   
				         输出结果：
						 
					      count1=1
                          count2=0
						  
						  		   
			   6  JVM
			   
			       JVM 的组成由类加载器把字节码文件加进虚拟机
				   虚拟机包括，方法区，java 栈， 本地方法栈，堆，和指令计数器
				   
				   其中 java 栈，指令计数器和本地方法栈是线程私有的
				   
				   堆区和方法区是线程间共享
				   
				   1)程序计数器(Program Counter Register)

　　                   程序计数器是用于存储每个线程下一步将执行的JVM指令，如该方法为native的，则程序计数器中不存储任何信息

                    2)JVM栈(JVM Stack)
                       JVM栈是线程私有的，每个线程创建的同时都会创建JVM栈，JVM栈中存放的为当前线程中局部基本类型的变量（java
					   中定义的八种基本类型：boolean、char、byte、short、int、long、float、double）、部分的返回结果以及Stack Frame，
					   非基本类型的对象在JVM栈上仅存放一个指向堆上的地址

                    3)堆(heap)
 
　                   　它是JVM用来存储对象实例以及数组值的区域，可以认为Java中所有通过new创建的对象的内存都在此分配，Heap中的对象的内存需要等待GC进行回收。

　　                   1）堆是JVM中所有线程共享的，因此在其上进行对象内存的分配均需要进行加锁，这也导致了new对象的开销是比较大的

　　                    2）Sun Hotspot JVM为了提升对象内存分配的效率，对于所创建的线程都会分配一块独立的空间TLAB
                           （Thread Local Allocatio Buffer），其大小由JVM根据运行的情况计算而得，在TLAB上分配对象时不需要加锁，
						   因此JVM在给线程的对象分配内存时会尽量的在TLAB上分配，在这种情况下JVM中分配对象内存的性能和C基本是
						   一样高效的，但如果对象过大的话则仍然是直接使用堆空间分配

　　                   3）TLAB仅作用于新生代的Eden Space，因此在编写Java程序时，通常多个小的对象比大的对象分配起来更加高效。

                       4)方法区（Method Area）

　　                      （1）在Sun JDK中这块区域对应的为PermanetGeneration，又称为持久代。

                          　2）方法区域存放了所加载的类的信息（名称、修饰符等）、类中的静态变量、类中定义为final类型的常量、
						    类中的Field信息、类中的方法信息，当开发人员在程序中通过Class对象中的getName、isInterface等方法来获取信息时，
							这些数据都来源于方法区域，同时方法区域也是全局共享的，在一定的条件下它也会被GC，当方法区域需要使用的内存
							超过其允许的大小时，会抛出OutOfMemory的错误信息。

                        5)本地方法栈（Native Method Stacks）

　                      　JVM采用本地方法栈来支持native方法的执行，此区域用于存储每个native方法调用的状态。

                        6)运行时常量池（Runtime ConstantPool）
						   存放的为类中的固定的常量信息、方法和Field的引用信息等，其空间从方法区域中分配。JVM在加载类时会为
						   每个class分配一个独立的常量池，但是运行时常量池中的字符串常量池是全局共享的。
				 
				 7  NIO优点和原理
				 
				     在Java 1.4 之前的I/O系统中，提供的都是面向流的I/O系统，系统一次一个字节地处理数据，一个输入流产生一个字节的数据，
					 一个输出流消费一个字节的数据，面向流的I/O速度非常慢，而在Java 1.4 中推出了NIO，这是一个面向块的I/O系统，
					 系统以块的方式处理处理，每一个操作在一步中产生或者消费一个数据块，按块处理要比按字节处理数据快的多。
					 
					 在NIO中有几个核心对象需要掌握：缓冲区（Buffer）、通道（Channel）、选择器（Selector）。
					 
					 1）缓冲区Buffer
					 
                     缓冲区实际上是一个容器对象，更直接的说，其实就是一个数组，在NIO库中，所有数据都是用缓冲区处理的。
					 在读取数据时，它是直接读到缓冲区中的； 在写入数据时，它也是写入到缓冲区中的；任何时候访问 NIO 中的数据，
					 都是将它放到缓冲区中。而在面向流I/O系统中，所有数据都是直接写入或者直接将数据读取到Stream对象中。
					 
					 2）通道Channel
					 
                     通道是一个对象，通过它可以读取和写入数据，当然了所有数据都通过Buffer对象来处理。
					 我们永远不会将字节直接写入通道中，相反是将数据写入包含一个或者多个字节的缓冲区。
					 同样不会直接从通道中读取字节，而是将数据从通道读入缓冲区，再从缓冲区获取这个字节。
                     在NIO中，提供了多种通道对象，而所有的通道对象都实现了Channel接口。
					 
					 3）使用NIO读取数据
					 
                     在前面我们说过，任何时候读取数据，都不是直接从通道读取，而是从通道读取到缓冲区。所以使用NIO读取数据可以分为下面三个步骤： 
                     1. 从FileInputStream获取Channel 
                     2. 创建Buffer 
                     3. 将数据从Channel读取到Buffer中
                     下面是一个简单的使用NIO从文件中读取数据的例子：
					 
                     public class Program {  
                          static public void main( String args[] ) throws Exception {  
                              FileInputStream fin = new FileInputStream("c:\\test.txt");  
          
                             // 获取通道  
                             FileChannel fc = fin.getChannel();  
          
                             // 创建缓冲区  
                             ByteBuffer buffer = ByteBuffer.allocate(1024);  
          
                             // 读取数据到缓冲区  
                             fc.read(buffer);  
          
                             buffer.flip();  
          
                             while (buffer.remaining()>0) {  
                                 byte b = buffer.get();  
                                 System.out.print(((char)b));  
                                }  
          
                               fin.close();  
                             }  
                     }  
					 
				  
				 8  java 内部类
				   
				      1） 使用匿名内部类的好处
					     
						  1 内部类方法可以访问该类定义所在作用域的数据，包括私有数据
						  2 内部类对同一个包的其他类隐藏起来
						  3 使用匿名内部类定义回调函数节省代码
						  
						  
				      2） 静态内部类和非静态内部类的区别
					  
					      非静态内部类持有外部类的一个饮用，而静态的没有
						  
					  
					  3） 匿名内部类的变量为什么要是final 的
					   
					        本人比较如同的解释：
                            “这是一个编译器设计的问题，如果你了解java的编译原理的话很容易理解。  
                            首先，内部类被编译的时候会生成一个单独的内部类的.class文件，这个文件并不与外部类在同一class文件中。  
                            当外部类传的参数被内部类调用时，从java程序的角度来看是直接的调用例如：  
                             public void dosome(final String a,final int b){  
                                 class Dosome{public void dosome(){System.out.println(a+b)}};  
                                                Dosome some=new Dosome();  
                                                some.dosome();  
                                           }  
                             从代码来看好像是那个内部类直接调用的a参数和b参数，但是实际上不是，在java编译器编译以后实际的操作代码是  
                              class Outer$Dosome{  
                                   public Dosome(final String a,final int b){  
                                       this.Dosome$a=a;  
                                       this.Dosome$b=b;  
                                      }  
                                   public void dosome(){  
                                        System.out.println(this.Dosome$a+this.Dosome$b);  
                                      }  
                                   }
							  }  
                             从以上代码看来，内部类并不是直接调用方法传进来的参数，而是内部类将传进来的参数通过自己的构造器备份到了自己的内部，
                             自己内部的方法调用的实际是自己的属性而不是外部类方法的参数。这样理解就很容易得出为什么要用final了，
                             因为两者从外表看起来是同一个东西，实际上却不是这样，如果内部类改掉了这些参数的值也不可能影响到原参数，
                             然而这样却失去了参数的一致性，因为从编程人员的角度来看他们是同一个东西，如果编程人员
                             在程序设计的时候在内部类中改掉参数的值，但是外部调用的时候又发现值其实没有被改掉，
                             这就让人非常的难以理解和接受，为了避免这种尴尬的问题存在，所以编译器设计人员把内
                             部类能够使用的参数设定为必须是final来规避这种莫名其妙错误的存在。”
							 
						4）   For循环实现原理
							
							其实内部是Iterator 实现的，编译成字节码的时候， for 循环和 Iterator 字节码是一样的
							
				  9   Java 内存模型
				  
				       Java内存模型即Java Memory Model，简称JMM。JMM定义了Java 虚拟机(JVM)在计算机内存(RAM)中的工作方式。
					   
					   Java内存模型定义了多线程之间共享变量的可见性以及如何在需要的时候对共享变量进行同步。
					   
					   关于并发编程:
                       
					   在并发编程领域，有两个关键问题：线程之间的通信和同步。
					   
					   线程之间的通信:
					   
                       线程的通信是指线程之间以何种机制来交换信息。在命令式编程中，线程之间的通信机制有两种共享内存和消息传递。

                       在共享内存的并发模型里，线程之间共享程序的公共状态，线程之间通过写-读内存中的公共状态来隐式进行通信，典型的共享内存通信方式就是通过共享对象进行通信。

                       在消息传递的并发模型里，线程之间没有公共状态，线程之间必须通过明确的发送消息来显式进行通信，在java中典型的消息传递方式就是wait()和notify()。
					   
					   线程之间的同步:
                       
					   同步是指程序用于控制不同线程之间操作发生相对顺序的机制。

                       在共享内存并发模型里，同步是显式进行的。程序员必须显式指定某个方法或某段代码需要在线程之间互斥执行。

                       在消息传递的并发模型里，由于消息的发送必须在消息的接收之前，因此同步是隐式进行的。
					   
					   Java内存模型:
					   
					   JMM决定一个线程对共享变量的写入何时对另一个线程可见。从抽象的角度来看，JMM定义了线程和主内存之间的抽象关系：
					   
					   线程之间的共享变量存储在主内存（main memory）中，每个线程都有一个私有的本地内存（local memory），
					   
					   本地内存中存储了该线程以读/写共享变量的副本。
					   
				  
				  
				  10  大端和小端
				  
				      大端——高尾端， 小端——低尾端
					  
					  以11 22 33 44 为例：
					  
					  高尾端存储顺序：
					  
					        11 22 33 44    // 地址递增顺序  低----》高
						
                      低尾端存储顺序：

                            44 33 22 11	   // 地址递增顺序  高《----低		
							
				  11  Java GC 原理
				  
				      在主流商用语言(如Java、C#)的主流实现中, 都是通过可达性分析算法来判定对象是否存活的: 
					  
					  通过一系列的称为 GC Roots 的对象作为起点, 然后向下搜索; 搜索所走过的路径称为引用链/Reference Chain,

					  当一个对象到 GC Roots 没有任何引用链相连时, 即该对象不可达, 也就说明此对象是不可用的, 如下图: Object5、6、7 虽然互有关联,

					  但它们到GC Roots是不可达的, 因此也会被判定为可回收的对象:
					  
					  
					  在Java, 可作为GC Roots的对象包括:
					  
                      方法区: 类静态属性引用的对象;
                      
					  方法区: 常量引用的对象;

					  虚拟机栈(本地变量表)中引用的对象.
					  
                      本地方法栈JNI(Native方法)中引用的对象。
					  
                     注: 即使在可达性分析算法中不可达的对象, VM也并不是马上对其回收, 因为要真正宣告一个对象死亡, 至少要经历两次标记过程: 
					 
					 第一次是在可达性分析后发现没有与GC Roots相连接的引用链, 
					 
					 第二次是GC对在F-Queue执行队列中的对象进行的小规模标记(对象需要覆盖finalize()方法且没被调用过).
					 
				 12  Java GC 算法
				 
				     分代收集算法 VS 分区收集算法
					 
					 1）分代收集法：
					 
                      当前主流VM垃圾收集都采用”分代收集”(Generational Collection)算法, 这种算法会根据对象存活周期的不同将内存划分为几块, 
					  
					  如JVM中的 新生代、老年代、永久代. 这样就可以根据各年代特点分别采用最适当的GC算法:
                      
					  在新生代: 每次垃圾收集都能发现大批对象已死, 只有少量存活. 因此选用复制算法, 只需要付出少量存活对象的复制成本就可以完成收集.

					  在老年代: 因为对象存活率高、没有额外空间对它进行分配担保, 就必须采用“标记—清理”或“标记—整理”算法来进行回收, 
					            不必进行内存复制, 且直接腾出空闲内存.
								
					  注：标记—清理算法会有以下两个问题:
                      
					  I. 效率问题: 标记和清除过程的效率都不高;
					  
                      II. 空间问题: 标记清除后会产生大量不连续的内存碎片, 空间碎片太多可能会导致
					  
					      在运行过程中需要分配较大对象时无法找到足够的连续内存而不得不提前触发另一次垃圾收集.
						  
						  因此采用标记-整理：
						  
						  标记清除算法会产生内存碎片问题, 而复制算法需要有额外的内存担保空间, 于是针对老年代的特点, 又有了标记整理算法. 
						  
						  标记整理算法的标记过程与标记清除算法相同, 但后续步骤不再对可回收对象直接清理, 而是让所有存活的对象都向一端移动,然后清理掉端边界以外的内存.
								
					 2）分区收集法：
					 
					   上面介绍的分代收集算法是将对象的生命周期按长短划分为两个部分, 而分区算法则将整个堆空间划分为连续的不同小区间, 
					   
					   每个小区间独立使用, 独立回收. 这样做的好处是可以控制一次回收多少个小区间.
                       
					   在相同条件下, 堆空间越大, 一次GC耗时就越长, 从而产生的停顿也越长. 为了更好地控制GC产生的停顿时间, 将一块大的内存区域分割为多个小块, 
					   
					   根据目标停顿时间, 每次合理地回收若干个小区间(而不是整个堆), 从而减少一次GC所产生的停顿.
					 
					    

                  							
					  
                      
                      					  
			七, 设计模式
			
			     1 动态代理
					jdk动态代理的应用前提，必须是目标类基于统一的接口。如果没有上述前提，jdk动态代理不能应用。
					内部基于反射实现  示例代码：

					public interface Subject {
						public void rent();

						public void hello(String str);
					}

					public class RealSubject implements Subject {
						@Override
						public void rent() {
							System.out.println("I want to rent my house");
						}

						@Override
						public void hello(String str) {
							System.out.println("hello: " + str);
						}
					}
					public class DynamicProxy implements InvocationHandler
					{
						//　这个就是我们要代理的真实对象
						private Object subject;

						//    构造方法，给我们要代理的真实对象赋初值
						public DynamicProxy(Object subject)
						{
							this.subject = subject;
						}

						@Override
						public Object invoke(Object object, Method method, Object[] args)
								throws Throwable
						{
							//　　在代理真实对象前我们可以添加一些自己的操作
							System.out.println("before rent house");

							System.out.println("Method:" + method);

							//    当代理对象调用真实对象的方法时，其会自动的跳转到代理对象关联的handler对象的invoke方法来进行调用
							method.invoke(subject, args);

							//　　在代理真实对象后我们也可以添加一些自己的操作
							System.out.println("after rent house");

							return null;
						}

					}

					public  void main(String[] args)
					{
						//    我们要代理的真实对象
						Subject realSubject = new RealSubject();

						//    我们要代理哪个真实对象，就将该对象传进去，最后是通过该真实对象来调用其方法的
						InvocationHandler handler = new DynamicProxy(realSubject);

						/*
						 * 通过Proxy的newProxyInstance方法来创建我们的代理对象，我们来看看其三个参数
						 * 第一个参数 handler.getClass().getClassLoader() ，我们这里使用handler这个类的ClassLoader对象来加载我们的代理对象
						 * 第二个参数realSubject.getClass().getInterfaces()，我们这里为代理对象提供的接口是真实对象所实行的接口，表示我要代理的是该真实对象，这样我就能调用这组接口中的方法了
						 * 第三个参数handler， 我们这里将这个代理对象关联到了上方的 InvocationHandler 这个对象上
						 */
						Subject subject = (Subject) Proxy.newProxyInstance(handler.getClass().getClassLoader(), realSubject
								.getClass().getInterfaces(), handler);

						System.out.println(subject.getClass().getName());
						subject.rent();
						subject.hello("world");
					}
					
					
              2  MVP的精髓就是 多出一个presenter 类，表示用户执行的操作， 在Activity或fragment进行交互时，不直接操作他们当中的视图

                  而是抽象出一个接口，直接与抽象接口进行交互，不用去管具体的实现，activity 或者fragment 需要去实现这个抽象类。
				  
	     八： NDK
		 
		      1 NDK的调试总结
		 
		      1） 一般我们会拿到如下的报错日志：
			  
			   I/DEBUG   (   31): *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** ***

               I/DEBUG   (   31): Build fingerprint: 'Meizu/meizu_PRO7H_CN/PRO7H:7.0/NRD90M/1513563377:user/release-keys'
 
               I/DEBUG   (   31): Revision: '0' ABI: 'arm' pid: 29870, tid: 29967, name: Thread-31  >>> com.yeahka.mach.android.openpos <<<

               I/DEBUG   (   31): signal 11 (SIGSEGV), code 1 (SEGV_MAPERR), fault addr 0x1400008   

               I/DEBUG   (   31): r0 0eefe56a  r1 de085ad0  r2 00000005  r3 000020fd
 
               I/DEBUG   (   31): r4 fffffff8  r5 c74f9607  r6 01400004  r7 c74f9748    
 
               I/DEBUG   (   31): r8 c74f9870  r9 c7b86700  sl 00000000  fp c74f97fc   

               I/DEBUG   (   31): ip c44738d0  sp c74f95c0  lr ea8d873a  pc e78101a0  cpsr 600b0030

               I/DEBUG   (   31): backtrace:

               I/DEBUG   (   31): #00 pc 0000b1a0  /data/app/com.yeahka.mach.android.openpos-1/lib/arm/liblepos.so (Java_com_yeahka_android_lepos_Device_nativeFunction68+51)

               I/DEBUG   (   31): #01 pc 00e9b783  /data/app/com.yeahka.mach.android.openpos-1/oat/arm/base.odex (offset 0xdfa000)
			   
			 
			 2） 调用如下命令 可以找到具体报错代码的行数：
			 
			     方法一：
			 
			     addr2line -f -e  D:\TestProjects\ShuabaoV2.3.0(013)_JniTest\Shuabao\build\intermediates\ndkBuild\shuabao\release\obj\local\armeabi\liblepos.so 0000b1a0
				 
				 一个个解释上面的含义：
				 
				 addr2line: 是ndk toolchain 底下的一个工具：一般路径是在toolchains\arm-linux-androideabi-4.9\prebuilt\windows-x86_64\bin 下面 作用是把指令地址转为代码中的某一行
				 
				 -f:  表示的是文件
				 
				 -e:  未知
				 
				 D:\TestProjects\ShuabaoV2.3.0(013)_JniTest\Shuabao\build\intermediates\ndkBuild\shuabao\release\obj\local\armeabi\liblepos.so： 代表的是你编译的.so 库所在的路径
				 
				 0000b1a0：表示的是指令的 地址  可以从以上的报错日志 backtrace: 以后的信息获得， pc 开头的是指令的地址
				 
				 执行以上命令以后  会得到如下的结果：
				 
				 Java_com_yeahka_android_lepos_Device_nativeFunction68
                 
				 D:/TestProjects/ShuabaoV2.3.0(013)_JniTest/Shuabao/src/main/jni/lepos.c:1000
				 
				 
				 Java_com_yeahka_android_lepos_Device_nativeFunction68： 代表的是 某个具体方法
				 
				 D:/TestProjects/ShuabaoV2.3.0(013)_JniTest/Shuabao/src/main/jni/lepos.c:1000： 1000 是源文件报错的行数
				 
				 
				 方法二：
				 
				 ndk-stack -sym  D:\TestProjects\ShuabaoV2.3.0(013)_JniTest\Shuabao\build\intermediates\ndkBuild\shuabao\release\obj\local\armeabi -dump D:\TestProjects\ShuabaoV2.3.0(013)_JniTest\Shuabao\logcat.txt
				 
				 ndk-stack: 找出方法调用栈的工具 一般在ndk 的目录下 和ndk-build 同级  
				 
				 -sym: 表示符号  找出编译以后 符号表内所对应的符号
				 
				 D:\TestProjects\ShuabaoV2.3.0(013)_JniTest\Shuabao\build\intermediates\ndkBuild\shuabao\release\obj\local\armeabi： 符号表所对应的目录 和 cpu 架构有关：
				 
				 -dump: 理解成导出代码快照  snapshot
				 
				 D:\TestProjects\ShuabaoV2.3.0(013)_JniTest\Shuabao\logcat.txt:  报错日志的目录， 把上面的日志考到一个文本文件里，目录传进来，此处有一点需要注意，报错日志要包含
				 
				  *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** ***， 不然系统可能识别不了
				 
				 
				 执行以上方法后  会得到如下结果：
				 
				 ********** Crash dump: **********
				 
                 Build fingerprint: 'Meizu/meizu_PRO7H_CN/PRO7H:7.0/NRD90M/1513563377:user/release-keys'
				 
                 pid: 29870, tid: 29967, name: Thread-31  >>> com.yeahka.mach.android.openpos <<<
				 
                 signal 11 (SIGSEGV), code 1 (SEGV_MAPERR), fault addr 0x1400008
				 
                 Stack frame  I/DEBUG   (   31): #00 pc 0000b1a0  /data/app/com.yeahka.mach.android.openpos-1/lib/arm/liblepos.so (Java_com_yeahka_android_lepos_Device_nativeFunction68+51): 
				 
				 Routine Java_com_yeahka_android_lepos_Device_nativeFunction68 at D:/TestProjects/ShuabaoV2.3.0(013)_JniTest/Shuabao/src/main/jni/lepos.c:1000

				 Stack frame  I/DEBUG   (   31): #01 pc 00e9b783  /data/app/com.yeahka.mach.android.openpos-1/oat/arm/base.odex (offset 0xdfa000)；
				 
				 
				 从以上可以看到调用栈帧， 信息和方法一差不多

				 
				参考： https://stackoverflow.com/questions/17840521/android-fatal-signal-11-sigsegv-at-0x636f7d89-code-1-how-can-it-be-tracked
				
				       https://mp.weixin.qq.com/s?__biz=MjM5MjAwODM4MA==&mid=203534700&idx=1&sn=21389e203c374b13b29d63bc9b69622c&scene=4
					   
			   2. JNI技术介绍
			   
			      1） JNI函数注册的方法。
				  
				      （1）静态方法  
					  
					        如何使用这种方法完成JNI函数的注册，这种方法就是根据函数名来找对应的JNI函数。这种方法需要Java的工具程序javah参与，整体流程如下：
							
							·  先编写Java代码，然后编译生成.class文件
							
							·  使用Java的工具程序javah，如javah–o output packagename.classname ，这样它会生成一个叫output.h的JNI层头文件。其中packagename.classname
							
							    是Java代码编译后的class文件，而在生成的output.h文件里，声明了对应的JNI层函数，只要实现里面的函数即可,以下是android_media_MediaScanner.h::样例文件：
								
								/* DO NOT EDIT THIS FILE - it is machinegenerated */

                                #include <jni.h>  //必须包含这个头文件，否则编译通不过

                                /* Header for class android_media_MediaScanner*/

 

                                #ifndef _Included_android_media_MediaScanner

                                #define _Included_android_media_MediaScanner

                                #ifdef __cplusplus

                                extern "C" {

                                #endif

                                ...... 略去一部分注释内容

                               //processFile的JNI函数

                              JNIEXPORT void JNICALLJava_android_media_MediaScanner_processFile

                                      (JNIEnv *, jobject, jstring,jstring, jobject);

 

                                ......//略去一部分注释内容

                               //native_init对应的JNI函数

                              JNIEXPORT void JNICALLJava_android_media_MediaScanner_native_1init

                                (JNIEnv*, jclass);

 

                            #ifdef __cplusplus

}

                            #endif

                            #endif
							
							·  当Java层调用native_init函数时，它会从对应的JNI库Java_android_media_MediaScanner_native_linit，如果没有，就会报错。如果找到，则会为这个native_init和Java_android_media_MediaScanner_native_linit建立一个关联关系，其实就是保存JNI层函数的函数指针。以后再调用native_init函数时，直接使用这个函数指针就可以了，当然这项工作是由虚拟机完成的。

                                从这里可以看出，静态方法就是根据函数名来建立Java函数和JNI函数之间的关联关系的，它要求JNI层函数的名字必须遵循特定的格式。这种方法也有几个弊端，它们是：

                              ·  需要编译所有声明了native函数的Java类，每个生成的class文件都得用javah生成一个头文件。

                              ·  javah生成的JNI层函数名特别长，书写起来很不方便。

                              ·  初次调用native函数时要根据函数名字搜索对应的JNI层函数来建立关联关系，这样会影响运行效率。
							  
					  （2）	 动态注册：
					  
					         既然Java native函数数和JNI函数是一一对应的，那么是不是会有一个结构来保存这种关联关系呢？答案是肯定的。在JNI技术中，用来记录这种一一对应关系的，是一个叫JNINativeMethod的结构，其定义如下：
							 
							 typedef struct {

                              //Java中native函数的名字，不用携带包的路径。例如“native_init“。

                                constchar* name;    

                             //Java函数的签名信息，用字符串表示，是参数类型和返回值类型的组合。

                               const char* signature;

                               void*       fnPtr;  //JNI层对应函数的函数指针，注意它是void*类型。

                            } JNINativeMethod;
							
							 应该如何使用这个结构体呢？来看MediaScanner JNI层是如何做的，[-->android_media_MediaScanner.cpp] 代码如下所示：
							 
							 //定义一个JNINativeMethod数组，其成员就是MS中所有native函数的一一对应关系。

                             static JNINativeMethod gMethods[] = {

                                          ......

                                   {

                                    "processFile" //Java中native函数的函数名。

                                   //processFile的签名信息，签名信息的知识，后面再做介绍。

                                    "(Ljava/lang/String;Ljava/lang/String;Landroid/media/MediaScannerClient;)V",   

                                   (void*)android_media_MediaScanner_processFile //JNI层对应函数指针。

                                   },


                                   {

                                   "native_init",       

                                    "()V",                     

                                     (void *)android_media_MediaScanner_native_init

                                    }
									
								};

                             //注册JNINativeMethod数组

                            int register_android_media_MediaScanner(JNIEnv*env)

                             {

                              //调用AndroidRuntime的registerNativeMethods函数，第二个参数表明是Java中的哪个类

                               returnAndroidRuntime::registerNativeMethods(env,

                                 "android/media/MediaScanner", gMethods, NELEM(gMethods));

                             }
							 
							 AndroidRunTime类提供了一个registerNativeMethods函数来完成注册工作，下面看registerNativeMethods的实现，代码如下：

                             [-->AndroidRunTime.cpp]
							 
							 int AndroidRuntime::registerNativeMethods(JNIEnv*env,

                                constchar* className, const JNINativeMethod* gMethods, int numMethods)

                                     {

                                     //调用jniRegisterNativeMethods函数完成注册

                                     returnjniRegisterNativeMethods(env, className, gMethods, numMethods);

                                     }
									 
							 其中jniRegisterNativeMethods是Android平台中，为了方便JNI使用而提供的一个帮助函数，其代码如下所示：

                                 [-->JNIHelp.c]
								 
								 
								 int jniRegisterNativeMethods(JNIEnv* env, constchar* className,

                                  constJNINativeMethod* gMethods, int numMethods)

{

                                  jclassclazz;

                                  clazz= (*env)->FindClass(env, className);


                                 //实际上是调用JNIEnv的RegisterNatives函数完成注册的

                               if((*env)->RegisterNatives(env, clazz, gMethods, numMethods) < 0) {

                                  return -1;

                                 }

                               return0;

                              }
							  
							  
							  wow，好像很麻烦啊！其实动态注册的工作，只用两个函数就能完成。总结如下：
							  
							  /*

                                env指向一个JNIEnv结构体，它非常重要，后面会讨论它。classname为对应的Java类名，由于

                                JNINativeMethod中使用的函数名并非全路径名，所以要指明是哪个类。

                              */

                              jclass clazz =  (*env)->FindClass(env, className);

                              //调用JNIEnv的RegisterNatives函数，注册关联关系。

                             (*env)->RegisterNatives(env, clazz, gMethods,numMethods);
							 
							  所以，在自己的JNI层代码中使用这种方法，就可以完成动态注册了。这里还有一个很棘手的问题：这些动态注册的函数在什么时候、什么地方被谁调用呢？：

                              ·  当Java层通过System.loadLibrary加载完JNI动态库后，紧接着会查找该库中一个叫JNI_OnLoad的函数，如果有，就调用它，而动态注册的工作就是在这里完成的。

                                 所以，如果想使用动态注册方法，就必须要实现JNI_OnLoad函数，只有在这个函数中，才有机会完成动态注册的工作。静态注册则没有这个要求，可我建议读者也实现这个
								 
								 JNI_OnLoad函数，因为有一些初始化工作是可以在这里做的。
								 
							     那么，libmedia_jni.so的JNI_OnLoad函数是在哪里实现的呢？由于多媒体系统很多地方都使用了JNI，所以码农把它放到android_media_MediaPlayer.cpp中了，代码如下所示：

                                 [-->android_media_MediaPlayer.cpp]：
								 
								     jint JNI_OnLoad(JavaVM* vm, void* reserved)

                                     {

                                      //该函数的第一个参数类型为JavaVM,这可是虚拟机在JNI层的代表喔，每个Java进程只有一个

                                      //这样的JavaVM

                                       JNIEnv* env = NULL;

                                       jintresult = -1;

 

                                     if(vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {

                                      gotobail;

                                     }

                                     ...... //动态注册MediaScanner的JNI函数。

                                      if(register_android_media_MediaScanner(env) < 0) {

                                         goto bail;

                                        }

                                       returnJNI_VERSION_1_4;//必须返回这个值，否则会报错。

                                      }
									  
									
							  

                  2）  Java和JNI层数据类型的转换。
				  
				       JNIEnv是一个和线程相关的，代表JNI环境的结构体
					   
					   JNIEnv实际上就是提供了一些JNI系统函数。通过这些函数可以做到：
					   
					    ·  调用Java的函数。

                        ·  操作jobject对象等很多事情。
						
						   通过JNIEnv操作jobject：
						   
						   从另外一个角度来解释这个问题。一个Java对象是由什么组成的？当然是它的成员变量和成员函数了。那么，操作jobject的本质就应当是操作这些对象的成员变量和成员函数。
						   
						   所以应先来看与成员变量及成员函数有关的内容。
						   
						    I: jfieldID 和jmethodID的介绍

							  我们知道，成员变量和成员函数是由类定义的，它是类的属性，所以在JNI规则中，用jfieldID 和jmethodID 来表示Java类的成员变量和成员函数，它们通过JNIEnv的下面两个函数可以得到：

                              jfieldID GetFieldID(jclass clazz,const char*name, const char *sig);

                              jmethodID GetMethodID(jclass clazz, const char*name,const char *sig);
							  
							  其中，jclass代表Java类，name表示成员函数或成员变量的名字，sig为这个函数和变量的签名信息。如前所示，成员函数和成员变量都是类的信息，这两个函数的第一个参数都是jclass。

                              MS中是怎么使用它们的呢？来看代码，如下所示：
							  
							      [-->android_media_MediaScanner.cpp::MyMediaScannerClient构造函数]
								  
								   MyMediaScannerClient(JNIEnv *env, jobjectclient)......

                                {

                                    //先找到android.media.MediaScannerClient类在JNI层中对应的jclass实例。

                                     jclass mediaScannerClientInterface =

                                     env->FindClass("android/media/MediaScannerClient");

                                    //取出MediaScannerClient类中函数scanFile的jMethodID。

                                    mScanFileMethodID = env->GetMethodID(

                                    mediaScannerClientInterface, "scanFile",

                                     "(Ljava/lang/String;JJ)V");

                                  //取出MediaScannerClient类中函数handleStringTag的jMethodID。

                                   mHandleStringTagMethodID = env->GetMethodID(

                                   mediaScannerClientInterface,"handleStringTag",

                                  "(Ljava/lang/String;Ljava/lang/String;)V");

  

                                }
								
								·  如果每次操作jobject前都去查询jmethoID或jfieldID的话将会影响程序运行的效率。所以我们在初始化的时候，就可以取出这些ID并保存起来以供后续使用。

                                   取出jmethodID后，又该怎么用它呢
								   
						   II:  使用jfieldID和jmethodID
						   
                                 下面再看一个例子，其代码如下所示：

                                  [-->android_media_MediaScanner.cpp::MyMediaScannerClient的scanFile]
								  
								  
								  virtualbool scanFile(const char* path, long long lastModified,

                                            long long fileSize)

                                            {

                                             jstring pathStr;

                                          if((pathStr = mEnv->NewStringUTF(path)) == NULL) return false;

       

                                           /*

                                          调用JNIEnv的CallVoidMethod函数，注意CallVoidMethod的参数：

                                          第一个是代表MediaScannerClient的jobject对象，

                                          第二个参数是函数scanFile的jmethodID，后面是Java中scanFile的参数。

                                           */

                                    mEnv->CallVoidMethod(mClient, mScanFileMethodID, pathStr,

                                   lastModified, fileSize);

 

                                     mEnv->DeleteLocalRef(pathStr);

                                  return (!mEnv->ExceptionCheck());

                                 }
								 
								 
								 明白了，通过JNIEnv输出的CallVoidMethod，再把jobject、jMethodID和对应参数传进去，JNI层就能够调用Java对象的函数了！

                                 实际上JNIEnv输出了一系列类似CallVoidMethod的函数，形式如下：

                                 NativeType Call<type>Method(JNIEnv *env,jobject obj,jmethodID methodID, ...)。

                                 其中type是对应Java函数的返回值类型，例如CallIntMethod、CallVoidMethod等。

                                 上面是针对非static函数的，如果想调用Java中的static函数，则用JNIEnv输出的CallStatic<Type>Method系列函数。

                                现在，我们已了解了如何通过JNIEnv操作jobject的成员函数，那么怎么通过jfieldID操作jobject的成员变量呢？这里，直接给出整体解决方案，如下所示：

                               //获得fieldID后，可调用Get<type>Field系列函数获取jobject对应成员变量的值。

                                NativeType Get<type>Field(JNIEnv *env,jobject obj,jfieldID fieldID)

                               //或者调用Set<type>Field系列函数来设置jobject对应成员变量的值。

                               void Set<type>Field(JNIEnv *env,jobject obj,jfieldID fieldID,NativeType value)

                               //下面我们列出一些参加的Get/Set函数。

                                GetObjectField()         SetObjectField()

                                GetBooleanField()         SetBooleanField()

                                GetByteField()           SetByteField()

                                GetCharField()           SetCharField()

                                GetShortField()          SetShortField()

                                GetIntField()            SetIntField()

                                GetLongField()           SetLongField()

                                GetFloatField()          SetFloatField()

                                GetDoubleField()         SetDoubleField()
								
								

                  3） JNIEnv和jstring的使用方法，以及JNI中的类型签名。

                  4）  最后介绍了垃圾回收在JNI层中的使用，以及异常处理方面的知识。
				  
				           Java中创建的对象最后是由垃圾回收器来回收和释放内存的，可它对JNI有什么影响呢？下面看一个例子：

                             [-->垃圾回收例子]:
							 
							 static jobject save_thiz = NULL; //定义一个全局的jobject

                            static void

                             android_media_MediaScanner_processFile(JNIEnv*env, jobject thiz, jstring path,

                                             jstringmimeType, jobject client)

                           {

                             //保存Java层传入的jobject对象，代表MediaScanner对象

                              save_thiz = thiz;


                            return;

                        }

                          //假设在某个时间，有地方调用callMediaScanner函数

                        void callMediaScanner()

                       {

                          //在这个函数中操作save_thiz，会有问题吗？

                       }
					   
					   
					   上面的做法肯定会有问题，因为和save_thiz对应的Java层中的MediaScanner很有可能已经被垃圾回收了，也就是说，save_thiz保存的这个jobject可能是一个野指针，如使用它，后果会很严重。

                        可能有人要问，将一个引用类型进行赋值操作，它的引用计数不会增加吗？而垃圾回收机制只会保证那些没有被引用的对象才会被清理。问得对，但如果在JNI层使用下面这样的语句，是不会增加引用计数的。

                       save_thiz = thiz; //这种赋值不会增加jobject的引用计数。

                       那该怎么办？不必担心，JNI规范已很好地解决了这一问题，JNI技术一共提供了三种类型的引用，它们分别是：

                         ·  Local Reference：本地引用。在JNI层函数中使用的非全局引用对象都是Local Reference。它包括函数调用时传入的jobject、在JNI层函数中创建的jobject。LocalReference最大的特点就是，
						 
						    一旦JNI层函数返回，这些jobject就可能被垃圾回收。

                         ·  Global Reference：全局引用，这种对象如不主动释放，就永远不会被垃圾回收。

                         ·  Weak Global Reference：弱全局引用，一种特殊的GlobalReference，在运行过程中可能会被垃圾回收。所以在程序中使用它之前，需要调用JNIEnv的IsSameObject判断它是不是被回收了。
						 
					 JNI中也有异常，不过它和C++、Java的异常不太一样。当调用JNIEnv的某些函数出错后，会产生一个异常，但这个异常不会中断本地函数的执行，直到从JNI层返回到Java层后，
					 
					 虚拟机才会抛出这个异常。虽然在JNI层中产生的异常不会中断本地函数的运行，但一旦产生异常后，就只能做一些资源清理工作了（例如释放全局引用，或者ReleaseStringChars）。
					 
					 如果这时调用除上面所说函数之外的其他JNIEnv函数，则会导致程序死掉。

                     来看一个和异常处理有关的例子，代码如下所示：
					 
					    [-->android_media_MediaScanner.cpp::MyMediaScannerClient的scanFile函数]
						
						
						  JNI层函数可以在代码中截获和修改这些异常，JNIEnv提供了三个函数进行帮助：

                          ·  ExceptionOccured函数，用来判断是否发生异常。

                          ·  ExceptionClear函数，用来清理当前JNI层中发生的异常。

                          ·  ThrowNew函数，用来向Java层抛出异常。

                          异常处理是JNI层代码必须关注的事情，读者在编写代码时务小心对待。
						  
						  
					  
					    

				 
	    九 Java中的容器：

             1 HashMap和Hashtable的区别

               1） 两者最主要的区别在于Hashtable是线程安全，而HashMap则非线程安全。Hashtable的实现方法里面都添加了synchronized关键字来确保线程同步，
			       因此相对而言HashMap性能会高一些，我们平时使用时若无特殊需求建议使用HashMap	

               2） HashMap可以使用null作为key，不过建议还是尽量避免这样使用。HashMap以null作为key时，总是存储在table数组的第一个节点上。而Hashtable则不允许null作为key。
 
            
               3） HashMap和Hashtable的底层实现都是数组+链表结构实现。
                   
			   4） 两者计算hash的方法不同：Hashtable计算hash是直接使用key的hashcode对table数组的长度直接进行取模：
                   HashMap计算hash对key的hashcode进行了二次hash，以获得更好的散列值，然后对table数组长度取摸：

               
             2  ArrayList和LinkedList的实现原理

               1、ArrayList和LinkedList可想从名字分析，它们一个是Array(动态数组)的数据结构，一个是Link(链表)的数据结构，此外，它们两个都是对List接口的实现。

                  前者是数组队列，相当于动态数组；后者为双向链表结构，也可当作堆栈、队列、双端队列

               2、当随机访问List时（get和set操作），ArrayList比LinkedList的效率更高，因为LinkedList是线性的数据存储方式，所以需要移动指针从前往后依次查找。

               3、当对数据进行增加和删除的操作时(add和remove操作)，LinkedList比ArrayList的效率更高，因为ArrayList是数组，
			      所以在其中进行增删操作时，会对操作点之后所有数据的下标索引造成影响，需要进行数据的移动。

               4、从利用效率来看，ArrayList自由性较低，因为它需要手动的设置固定大小的容量，但是它的使用比较方便，只需要创建，然后添加数据，通过调用下标进行使用；
			      而LinkedList自由性较高，能够动态的随数据量的变化而变化，但是它不便于使用。

               5、ArrayList主要控件开销在于需要在lList列表预留一定空间；而LinkList主要控件开销在于需要存储结点信息以及结点指针信息。

               6，LinkedList 还实现了队列和栈的实现方法			

             3  LruCache 介绍

               LruCache是个泛型类，主要算法原理是把最近使用的对象用强引用（即我们平常使用的对象引用方式）存储在 LinkedHashMap 中。当缓存满时，把最近最少使用的对象从内存中移除，并提供了get和put方法来完成缓存的获取和添加操作。

               注意点：LinkedHashMap 是放到队尾			   
			
		      
			
			     

		遗留问题
		
		      1  MVVM简介与运用:
                 
				 1） MVC框架：
				  
				     M-Model : 业务逻辑和实体模型(biz/bean)

				     V-View : 布局文件(XML)
  
				     C-Controller : 控制器(Activity)
				  
				     相信大家都熟悉这个框架，这个也是初学者最常用的框架，该框架虽然也是把代码逻辑和UI层分离，但是View层能做的事情还是很少的，很多对于页面的呈现还是交由C实现，
				  
				     这样会导致项目中C的代码臃肿，如果项目小，代码臃肿点还是能接受的，但是随着项目的不断迭代，代码量的增加，你就会没办法忍受该框架开发的项目，这时MVP框架就应运而生。
					 
				 2） MVP框架：
				 
				     M-Model : 业务逻辑和实体模型(biz/bean)

					 V-View : 布局文件(XML)和Activity

					 P-Presenter : 完成View和Model的交互
					 
					 MVP框架相对于MVC框架做了较大的改变，将Activity当做View使用，代替MVC框架中的C的是P，对比MVC和MVP的模型图可以发现变化最大的是View层和Model层不在直接通信，
					 
					 所有交互的工作都交由Presenter层来解决。既然两者都通过Presenter来通信，为了复用和可拓展性，MVP框架基于接口设计的理念大家自然就可以理解其用意。

                     但MVP框架也有不足之处:

                     1.接口过多，一定程度影响了编码效率。

                     2.业务逻辑抽象到Presenter中，较为复杂的界面Activity代码量依然会很多。

                     3.导致Presenter的代码量过大。
					 
				 3） MVVM框架：

                      M-Model : 实体模型(biz/bean)

					  V-View : 布局文件(XML)

					  VM-ViewModel : DataBinding所在之处，对外暴露出公共属性，View和Model的绑定器		

					  1.    可重用性。你可以把一些视图逻辑放在一个ViewModel里面，让很多View重用这段视图逻辑。 在Android中，布局里可以进行一个视图逻辑，并且Model发生变化，View也随着发生变化。

                      2.    低耦合。以前Activity、Fragment中需要把数据填充到View，还要进行一些视图逻辑。现在这些都可在布局中完成（具体代码请看后面） 甚至都不需要再Activity、Fragment去findViewById（）。
					  
					        这时候Activity、Fragment只需要做好的逻辑处理就可以了。
					  
					       下面来比较一下布局与之前大家常用的格式的区别：

                          <?xml version="1.0" encoding="utf-8"?>
                          <layout xmlns:android="http://schemas.android.com/apk/res/android">
                            <data>
                              <variable name="user" type="com.example.User"/>
                           </data>
                          <LinearLayout
                           android:orientation="vertical"
                           android:layout_width="match_parent"
                           android:layout_height="match_parent">
                          <TextView android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="@{user.firstName}"/>
                          <TextView android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="@{user.lastName}"/>
                          </LinearLayout>
                         </layout>
						
					 3  缺点： 1 使用复杂 2 所处理的逻辑问题比较有限  实际情况经常有视图的变化
					 
				  2  热补丁Tinker 实现原理
				   
				     tinker将old.apk和new.apk做了diff，拿到patch.dex，然后将patch.dex与本机中apk的classes.dex做了合并，生成新的classes.dex，运行时通过反射将合并后的dex文件放置在加载的dexElements数组的前面。

                     运行时替代的原理，其实和Qzone的方案差不多，都是去反射修改dexElements。

                     两者的差异是：Qzone是直接将patch.dex插到数组的前面；而tinker是将patch.dex与app中的classes.dex合并后的全量dex插在数组的前面。

                     tinker这么做的目的还是因为Qzone方案中提到的CLASS_ISPREVERIFIED的解决方案存在问题；而tinker相当于换个思路解决了该问题。

                     接下来我们就从代码中去验证该原理。
					 
					 
					 通用原理：
					 
					  重点需要知道的就是，Android的ClassLoader体系，android中加载类一般使用的是PathClassLoader和DexClassLoader，首先看下这两个类的区别：
					  
					  PathClassLoader：
					  
					    对于PathClassLoader，从文档上的注释来看：可以看出，Android是使用这个类作为其系统类和应用类的加载器。并且对于这个类呢，只能去加载已经安装到Android系统中的apk文件。
						
					  DexClassLoader:
					  
					    对于DexClassLoader，依然看下注释：可以看出，该类呢，可以用来从.jar和.apk类型的文件内部加载classes.dex文件。可以用来执行非安装的程序代码。
						
					Android使用PathClassLoader作为其类加载器，DexClassLoader可以从.jar和.apk类型的文件内部加载classes.dex文件就好了。

                    上面我们已经说了，Android使用PathClassLoader作为其类加载器，那么热修复的原理具体是？
					
					   ok，对于加载类，无非是给个classname，然后去findClass，我们看下源码就明白了。 

					   PathClassLoader和DexClassLoader都继承自BaseDexClassLoader。在BaseDexClassLoader中有如下源码：
					   
					   
					   #BaseDexClassLoader
                               @Override
                        protected Class<?> findClass(String name) throws ClassNotFoundException {
                                 Class clazz = pathList.findClass(name);

                              if (clazz == null) {
                               throw new ClassNotFoundException(name);
                              }

                            return clazz;
                             }
							 

                        #DexPathList
					    public Class findClass(String name) {
                          for (Element element : dexElements) {
                            DexFile dex = element.dexFile;

                           if (dex != null) {
                            Class clazz = dex.loadClassBinaryName(name, definingContext);
                           if (clazz != null) {
                              return clazz;
                            }
                            }
                          }

                         return null;
                       }

                      #DexFile
                       public Class loadClassBinaryName(String name, ClassLoader loader) {
                         return defineClass(name, loader, mCookie);
                        }
					   
                       private native static Class defineClass(String name, ClassLoader loader, int cookie);
					   
					   
					   可以看出呢，BaseDexClassLoader中有个pathList对象，pathList中包含一个DexFile的集合dexElements，而对于类加载呢，就是遍历这个集合，通过DexFile去寻找。

                          ok，通俗点说：

                       一个ClassLoader可以包含多个dex文件，每个dex文件是一个Element，多个dex文件排列成一个有序的数组dexElements，当找类的时候，会按顺序遍历dex文件，
					   
					   然后从当前遍历的dex文件中找类，如果找类则返回，如果找不到从下一个dex文件继续查找。(来自：安卓App热补丁动态修复技术介绍)
					
					   那么这样的话，我们可以在这个dexElements中去做一些事情，比如，在这个数组的第一个元素放置我们的patch.jar，里面包含修复过的类，这样的话，
					   
					   当遍历findClass的时候，我们修复的类就会被查找到，从而替代有bug的类。
					   
					   
					   不过，还存在一个CLASS_ISPREVERIFIED的问题，对于这个问题呢，详见：安卓App热补丁动态修复技术介绍该文有图文详解。

                       ok，对于CLASS_ISPREVERIFIED，还是带大家理一下：

                       根据上面的文章，在虚拟机启动的时候，当verify选项被打开的时候，如果static方法、private方法、构造函数等，其中的直接引用（第一层关系）
					   
					   到的类都在同一个dex文件中（是因为在c++代码中会校验 引用者和被引用者是否来自同一个dex,否的话 会抛出异常），那么该类就会被打上CLASS_ISPREVERIFIED标志
					   
					   那么，我们要做的就是，阻止该类打上CLASS_ISPREVERIFIED的标志。

                       注意下，是阻止引用者的类，也就是说，假设你的app里面有个类叫做LoadBugClass，再其内部引用了BugClass。发布过程中发现BugClass有编写错误，那么想要发布一个新的BugClass类，
					   
					   那么你就要阻止LoadBugClass这个类打上CLASS_ISPREVERIFIED的标志。也就是说，你在生成apk之前，就需要阻止相关类打上CLASS_ISPREVERIFIED的标志了。对于如何阻止，
					   
					   上面的文章说的很清楚，让LoadBugClass在构造方法中，去引用别的dex文件，比如：hack.dex中的某个类即可。
					   
					   ok，总结下：

                       其实就是两件事：1、动态改变BaseDexClassLoader对象间接引用的dexElements；2、在app打包的时候，阻止相关类去打上CLASS_ISPREVERIFIED标志。
					  
					   三、阻止相关类打上CLASS_ISPREVERIFIED标志

					   ok，接下来的代码基本上会通过https://github.com/dodola/HotFix所提供的代码来讲解。


					   那么，这里拿具体的类来说：


					   大致的流程是：在dx工具执行之前，将LoadBugClass.class文件呢，进行修改，再其构造中添加System.out.println(dodola.hackdex.AntilazyLoad.class)，然后继续打包的流程。注意：AntilazyLoad.class这个类是独立在hack.dex中。

                        ok，这里大家可能会有2个疑问：

                         1 如何去修改一个类的class文件
                        
					 	 2 如何在dx之前去进行疑问1的操作
						 
						  一.  修改class文件：
						       
							   package dodola.hackdex;
                             
							   public class AntilazyLoad{

                               }

                              package dodola.hotfix;
                              public class BugClass {
                                 public String bug(){
                                  return "bug class";
                              }
                              }

                            package dodola.hotfix;
                           
						     public class LoadBugClass
                            {
                              public String getBugString(){
                              BugClass bugClass = new BugClass();
                             return bugClass.bug();
                             }
                             }
							 
						   注意下，这里的package，我们要做的是，上述类正常编译以后产生class文件。比如：LoadBugClass.class，我们在LoadBugClass.class的构造中去添加一行：

                             System.out.println(dodola.hackdex.AntilazyLoad.class)
							 
							 
							 package test;

                            import javassist.ClassPool;
                            import javassist.CtClass;
                            import javassist.CtConstructor;

                            public class InjectHack
                              {
                             public static void main(String[] args)
                            {
                           try
                              {
                                String path = "/Users/zhy/develop_work/eclipse_android/imooc/JavassistTest/";
                                ClassPool classes = ClassPool.getDefault();
                                classes.appendClassPath(path + "bin");//项目的bin目录即可
                                CtClass c = classes.get("dodola.hotfix.LoadBugClass");
                                CtConstructor ctConstructor = c.getConstructors()[0];
                               ctConstructor
                                  .insertAfter("System.out.println(dodola.hackdex.AntilazyLoad.class);");
                               c.writeFile(path + "/output");
                             } catch (Exception e)
                              {
                                e.printStackTrace();
                              }

                               }
                             
							}
							
						  之所以选择构造函数是因为他不增加方法数，一个类即使没有显式的构造函数，也会有一个隐式的默认构造函数。
						 
						   
					 
				 3 进程的回收


                   Foreground process、Visible process、Service process、Background process、Empty process。				 
						
			// 1   心跳机制的了解

			// 2   长连接保活

			// 3   内存优化 dumb mat

			// 4   H264

				// 1) GPU软解码    2） CPU硬解码   3) android 用 mediaCodec;

				// 4）一路解码和多路解码   5）h264 压缩   6）rtmp

			// 28   rtsp

			// 37   java 的容器46

				// 二分查找的代码

			// 47 java容器的学习

			 // 遗留问题  deque 以及线程池 SynchronousQueue 实现；

			// 52  项目介绍

			// 66 热修复技术Tinker Andfix Robust // Instant run 原理

			// 67 大端 小端

			// 68 udp 校验

			// 68 tcp校验
					 
			// 70 restful
			 
			 // 内存泄露的几种方式

			// 二  开发遇到的难点回顾

			// 1    超长图的处理

			// 写了一个自定义控件 继承ImageView;

			// 1）先用option,获得图片原始的大小，Bitmap region decoder;
			// 2) 初始化一个Rect 的对象 来保存解码区域和绘制区域，用于bitmap region decoder 解码， 默认展示图片的的中心区域
			// 3) 重写onTouchEvent,根据移动距离修改绘制区域，调用invalidate;// 缩放用matrix 这个类

			// 2  推送保活，提高存活率

			// 1) 方案一： onDestroy 时唤起自己

			// 2）方案二： 写一个广播每隔一段时间唤起自己

			// 3）方案三： 观察别人存活久的app 是怎么做的，发现存活久的app怎么做的

			   // 反编译微信 看其实现，发现和以上实现相同

			   // 继续查找原因，关注其公众号，看他的文章，发现和厂商合作紧密，

			   // 得出结论可能是被加了白名单
			   
			// 4）替代方案：

			  // 用友盟推送，友盟被阿里收购，其实就是用的淘宝的推送，通过旗下大量日活高的app，
			  
			  // 相互唤起，有种推送联盟的感觉。
			  
			  // 3   监听卸载 监听自己的app 是否被卸载

			 // linux 移动系统

     五：算法类：
	 
	     1  二叉树的概念：
		 
		     结点的度：结点拥有的子树的数目

             叶子结点：度为0的结点

             分支结点：度不为0的结点

             树的度：树中结点的最大的度

             层次：根结点的层次为1，其余结点的层次等于该结点的双亲结点的层次加1

             树的高度：树中结点的最大层次

             森林：0个或多个不相交的树组成。对森林加上一个根，森林即成为树；删去根，树即成为森林。

 
		 
		  1）定义：
		  
		  二叉树是每个结点最多有两个子树的树结构。它有五种基本形态：二叉树可以是空集；根可以有空的左子树或右子树；
		  或者左、右子树皆为空。
		  
		  2）性质：
		  
		  性质1：二叉树第i层上的结点数目最多为2i-1(i>=1)

          性质2：深度为k的二叉树至多有2k-1个结点（k>=1）
		  
		  性质3：包含n个结点的二叉树的高度至少为(log2n)+1

          性质4：在任意一棵二叉树中，若终端结点的个数为n0，度为2的结点数为n2，则n0=n2+1
		  
		 3）种类
		 
		    一 满二叉树
			
			   定义：高度为h，并且由2h-1个结点组成的二叉树，称为满二叉树

            
		    二 完全二叉树
			
			   定义：一棵二叉树中，只有最下面两层结点的度可以小于2，并且最下层的叶结点集中在靠左的若干位置上，
			   
			   这样的二叉树称为完全二叉树。
			   
			   
			   面试题：如果一个完全二叉树的结点总数为768个，求叶子结点的个数。
			   

               由二叉树的性质知：n0=n2+1，将之带入768=n0+n1+n2中得：768=n1+2n2+1，因为完全二叉树度为1的结点个数要么为0，
			   
			   要么为1，那么就把n1=0或者1都代入公式中，很容易发现n1=1才符合条件。所以算出来n2=383，所以叶子结点个数n0=n2+1=384。

               总结规律：如果一棵完全二叉树的结点总数为n，那么叶子结点等于n/2（当n为偶数时）或者(n+1)/2（当n为奇数时）
			   
			3、二叉查找树

               定义：二叉查找树又被称为二叉搜索树。设x为二叉查找树中的一个结点，x结点包含关键字key，
			   
			   结点x的key值计为key[x]。如果y是x的左子树中的一个结点，则key[y]<=key[x]；如果y是x的右子树的一个结点，
			   
			   则key[y]>=key[x]
			   
			   
			4  平衡二叉树
			
			    平衡二叉搜索树（Self-balancing binary search tree）又被称为AVL树（有别于AVL算法），且具有以下性质：
				
				它是一 棵空树或它的左右两个子树的高度差的绝对值不超过1，并且左右两个子树都是一棵平衡二叉树。
			   
            5  二叉树的遍历：
			
			   二叉树的遍历分为以下三种：

               先序遍历：遍历顺序规则为【根左右】

               中序遍历：遍历顺序规则为【左根右】

               后序遍历：遍历顺序规则为【左右根】
			   
	 六  视频编解码/相机：
	 
	      1 yuv是什么：
		   
		     YUV，分为三个分量，“Y”表示明亮度（Luminance或Luma），也就是灰度值；
			 
			 而“U”和“V” 表示的则是色度（Chrominance或Chroma），作用是描述影像色彩及饱和度，用于指定像素的颜色。
			 
		  2 yuv 的种类：
		  
		     YUV格式有两大类：planar和packed。
  
             对于planar的YUV格式，先连续存储所有像素点的Y，紧接着存储所有像素点的U，随后是所有像素点的V。

			 对于packed的YUV格式，每个像素点的Y,U,V是连续交*存储的。
			 
			 与我们熟知的RGB类似，YUV也是一种颜色编码方法，主要用于电视系统以及模拟视频领域，
			 
			 它将亮度信息（Y）与色彩信息（UV）分离，没有UV信息一样可以显示完整的图像，只不过是黑白的，
			 
			 这样的设计很好地解决了彩色电视机与黑白电视的兼容问题。并且，YUV不像RGB那样要求三个独立的视频信号
			 
			 同时传输，所以用YUV方式传送占用极少的频宽。
			 
	      3  常用的格式：
		  
		      YUV420 planar数据， 以720×488大小图象YUV420 planar为例，

              所以YUV420 数据在内存中的长度是 width * hight * 3 / 2，
			  
			  一般来说，直接采集到的视频数据是RGB24的格式，RGB24一帧的大小size＝width×heigth×3 Bit，
			  
			  RGB32的size＝width×heigth×4，如果是I420（即YUV标准格式4：2：0）的数据量是 size＝width×heigth×1.5 Bit。

			  在采集到RGB24数据后，需要对这个格式的数据进行第一次压缩。即将图像的颜色空间由RGB2YUV。
			  
			  因为，X264在进行编码的时候需要标准的YUV（4：2：0）。
			  
			  
		  4  H264
		  
		     H264是什么？H264是一种视频流的标准，与MP4、FLV等的区别在于，MP4里面可以包含H264信息和
			 
			 音频信息和其他的视频的属性信息，所以无论H264放在MP4文件里还是放在FLV里面，H264就是H264，
			 
			 是视频的一种格式，就像MP3格式的音频，无论放在MP4文件中还是放在FLV文件中，它都是MP3。
			 
			 
			 预测编码：
			   
			      视频描述的是连续的图像的集合。经过大量的统计表明，前后两幅图像中有大量的数据是一样的，
			 
			 也就是存在着冗余数据，那么使用当前图像对前一张图像做“减法”，获得两个图像的“差值”。那么只需要“差值”
			 
			 即可从前一幅图像中获得当前的图像信息。这个差值我们称之为残差。并且这个差值可以看做是二维数组即看做
			 
			 是一个二维矩阵。使用一些数学上的方法对矩阵进行变换达到一定的压缩目的
			 
			 
			 运动估计：
			 
			      由于活动图像临近帧中存在一定的相关性，因此将图像分成若干个宏块，并搜索出各个宏块在临近图像中
				  
			 的位置。并且得到宏块的相对偏移量。得到的相对偏移量称为运动矢量。得到运动矢量的过程称为运动估计。
			 
			 在帧间预测编码中，由于活动图像临近帧中景物存在一定的相关性，因此，可以将活动图像分成若干块或宏块，
			 
			 并设法搜索出每个块或宏块在临近帧图像中的位置，并得出亮着之间的控件位置相对偏移量，
			 
			 得到的相对偏移量就是通常所指的运动矢量，得到运动矢量的过程被称为运动估计。
			 
			 运动矢量和经过匹配后得到的预测误差共同发送到解码端，在解码端按照运动矢量指明的位置，从
			 
			 已经解码的临近参考帧图像中找到相应的块或宏块，和预测误差相加后就得到了块或宏块在当前帧中的位置。
			 
			 通过运动估计可以去除帧间的冗余度，使得视频传输的比特数大为减少，因此运动估计是视频压缩处理的一个重要组成部分。

          5  AwesomePlayer 播放原理

               1） AwesomeEvent这个是同步相应的事件而做的一个类,跟framework层的looper和handler作用相似,
			   
			   player有一些异步操作比如解析文件,这些操作比较耗时,做异步操作然后做回调会有更好的用户体验
			   
			   structAwesomeEvent:publicTimedEventQueue::Event 

              
               2） DataSourceInput   --->  Extractor (VideoSource) ---> Decoder (Video Sourece)	

                   ---> VideoRender（AwesomeRenderer）  ---> Surface (ISurface)

				
               3)  播放流程详细版

                   3.1 设置数据源URI

                        status_t AwesomePlayer::setDataSource_l(  
        
                    	const char *uri, const KeyedVector<String8, String8> *headers) {  
    
	                      /// 这里只是把URL保存起来而已, 真正的工作在Prepare之后进行  
                                      mUri = uri;  
                                  return OK;  
                        } 				   
		  
		           3.2   开启定时器队列,并且 Post一个AsyncPrepareEvent 事件
				   
				         status_t AwesomePlayer::prepareAsync_l() {  
 
                            /// 开启定时器队列  
                            mQueue.start();  
 
                            /// Post AsyncPrepare 事件  
                            mAsyncPrepareEvent = new AwesomeEvent(  
           
                    		this, &AwesomePlayer::onPrepareAsyncEvent);  
 
                            mQueue.postEvent(mAsyncPrepareEvent);  
                         
						     return OK;  
                         }  
			

                   3.3   AsyncPrepare 事件被触发
				   
				           当这个事件被触发时, AwesomePlayer 开始创建 VideoTrack和AudioTrack , 然后创建

						   VideoDecoder和AudioDecoder

                              void AwesomePlayer::onPrepareAsyncEvent() {  
    
	                           /// a. 创建视频源和音频源  
                              finishSetDataSource_l();  
 
                              /// b. 创建视频解码器  
                              initVideoDecoder();  
 
                               /// c. 创建音频解码器  
                              initAudioDecoder();  
                            } 
							
							至此,播放器准备工作完成, 可以开始播放了

                   3.4   Post 第一个VideoEvent
				   
				         AwesomePlayer::play() 调用 -> AwesomePlayer::play_l() 调用 -> AwesomePlayer::postVideoEvent_l
						 
						 (int64_t delayUs)  
						 
						 
						 void AwesomePlayer::postVideoEvent_l(int64_t delayUs) {  
    
	                         mQueue.postEventWithDelay(mVideoEvent, delayUs < 0 ? 10000 : delayUs); 

                 		}  

   				   3.5	VideoEvent 被触发

                         void AwesomePlayer::onVideoEvent() {  
 
                             /// 从视频解码器中读出视频图像  
                             mVideoSource->read(&mVideoBuffer, &options);  
 
                             /// 创建AwesomeRenderer (如果没有的话)  
                             if (mVideoRendererIsPreview || mVideoRenderer == NULL) {  
                                initRenderer_l();  
                             }  
 
                             /// 渲染视频图像  
                             mVideoRenderer->render(mVideoBuffer);  
 
                             /// 再次发送一个VideoEvent, 这样播放器就不停的播放了  
                             postVideoEvent_l();  
                            
							}  		

                          总结: SetDataSource -> Prepare -> Play -> postVieoEvent -> OnVideoEvent 
				  
				                -> postVideoEvent-> .... onVideoEvent-> postStreamDoneEvent -> 播放结束							



          6   滤镜的实现：
		  
		      两种实现：
			  
			  1） gpuimage 的实现：
			  
			       将相机回传的yuv420 由natvie library 转成 rgb, 再交给GlSurfaceView 来渲染纹理
			  
			       GPUImageNativeLibrary.YUVtoRBGA(data, previewSize.width, previewSize.height,
                            mGLRgbBuffer.array());
                   mGLTextureId = OpenGlUtils.loadTexture(mGLRgbBuffer, previewSize, mGLTextureId);
                   camera.addCallbackBuffer(data);
				   
				   缺点：yuv 转 rgb 由cpu实现计算量太大，反映不够快
      
	          2） CameraFilter 实现
              
			       由opengl es 创建一个textureId 然后赋值给SurfaceTexture, 然后再将这个surfaceTexture 传给camera展示
				   
				    // Create texture for camera preview
                   cameraTextureId = MyGLUtils.genTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
                   cameraSurfaceTexture = new SurfaceTexture(cameraTextureId);

                   // Start camera preview
                   try {
                     camera.setPreviewTexture(cameraSurfaceTexture);
                     camera.startPreview();
                      } catch (IOException ioe) {
                    // Something bad happened
                   }
				   
				   然后再在onDrawFrame 里 调用surfaceTexture.updateTexImage 来刷新界面
				   
				   优点：Gpu实现 快速高效
                  
          7    可变管线和固定管线的区别
		  
		       参考：https://www.zhihu.com/question/42190368?sort=created
		  
		       Shader可以说是现代OpenGL的灵魂。这牵涉到一个历史遗留问题：
			   
			   GL 1.x中只有固定管线，渲染效果是固定的，而在GL 2.0中增加了可编程管线，想要什么渲染效果都可以自己往加。
			   
			   GL 3.0+就废弃了固定管线，也就是说不写Shader是不推荐的。不写Shader的固定管线时代早就在十多年前过去了。
			   
			   回到GLES中，GLES 1.0是固定管线（为了某些原因），不需要也不能写Shader；GLES 2.0+是可编程管线，必须写Shader。
			   
			   建议题主少走点弯路，直接上可编程管线。虽然比固定管线稍微要多做点工作，但长远来看是好的，能做出更炫的效果并充分利用GPU。
			   
			   退固定管线保平安。





