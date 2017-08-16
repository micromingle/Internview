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

             //滑动窗口控制 怎么验证数据顺序是否在正确；？？
			 
			 
         2  https 握手过程
	  
             1) 客户端发送请求 ，服务端产生公钥和私钥，将私钥发送给客户端
             2) 客户端随机产生秘钥，用公钥加密 产生的秘钥，发送给服务端
             3) 服务端用私钥解密，得出秘钥，用得出的秘钥进行加密信息，进行传输
			 
		 3  开源框架介绍Retrofit2 介绍
			  一： Retrofit 组成部分，使用先定义一个接口
			  1) 注解部分 包含请求方法如POST,GET,查询参数 QUERY  请求头HEADERS
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
                     
                      ① 主机A发出ARP请求，请求帧中的数据部分包括发送者MAC地址00-0C-04-18-19-aa、发送者IP地址172.16.20.20和目标MAC地址，这里全部填充0，因为它未知（这正是ARP要询问的），目标IP地址是172.16.20.5。
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
				  
				  // 2) 在onTouchEvent 执行真正的操作
				  
				  public boolean onTouchEvent(MotionEvent event) {
                      switch (event.getAction()) {
                          case MotionEvent.ACTION_MOVE:
                               mXMove = event.getRawX();
                               int scrolledX = (int) (mXLastMove - mXMove);
                               if (getScrollX() + scrolledX < leftBorder) {
                                   scrollTo(leftBorder, 0);
                                   return true;
                               } else if (getScrollX() + getWidth() + scrolledX > rightBorder) {
                                    scrollTo(rightBorder - getWidth(), 0);
                                    return true;
                               }
                               scrollBy(scrolledX, 0);
                               mXLastMove = mXMove;
                               break;
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
                          invalidate();
                       }
                  }
				 
				 
				 //整个View树的绘图流程是在ViewRootImpl类的performTraversals()方法
				 
				 
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

               //MUST CALL THIS
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



           6    为什么可以在子线程中更新UI

           7  (4)  getMeasuredWidth()与getWidth() 区别；

                  getMeasuredWidth()与getWidth()的区别。他们的值大部分时间都是相同的，但意义确是根本不一样的，我们就来简单分析一下。

                  区别主要体现在下面几点：

                 -  首先getMeasureWidth()方法在measure()过程结束后就可以获取到了，而getWidth()方法要在layout()过程结束后才能获取到。

                 -  getMeasureWidth()方法中的值是通过setMeasuredDimension()方法来进行设置的，

                    而getWidth()方法中的值则是通过layout(left,top,right,bottom)方法设置的。


            8   onMeasure 在遍历各个孩子的时候，需要调用measureChild 来获得孩子的宽度和高度。
			
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
					 
						 • If touch is relevant (inside view),  child.dispatchTouchEvent()
						 
						 • If not handled by previous, dispatch to next view
						 
					 • If no children handle event, listener gets a chance

						  • OnTouchListener.onTouch()
						  
					 • If no listener, or not handled
					 
						 • onTouchEvent()
						 
				 • Intercepted events jump over child step
				 

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

					 3) newCachedThreadPool 核心线程数0，最大线程数没有限制，任务执行完成后，60秒后会自动退出
						执行线程SynchronousQueue,这个策略比较特殊他并没有把任务加入队列中，而是来一个创建一个

					 4）scheduledThreadExecutor 执行定时任务 ，采用DelayWorkerQueue 策略；

					 5）shutdown 不再接受新任务，要等待所有任务执行完才关闭
						 shutdownnow 不再接受新任务，并且试图停止正在执行的任务
					 6）拒绝策略：
						 ThreadPoolExecutor.AbortPolicy:丢弃任务并抛出RejectedExecutionException异常。
						 ThreadPoolExecutor.DiscardPolicy：也是丢弃任务，但是不抛出异常。
						 ThreadPoolExecutor.DiscardOldestPolicy：丢弃队列最前面的任务，然后重新尝试执行任务（重复此过程）
						 ThreadPoolExecutor.CallerRunsPolicy：由调用线程处理该任务					  
			    
				
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
                       在虚拟机层面，为了尽可能减少内存操作速度远慢于CPU运行速度所带来的CPU空置的影响，虚拟机会按照自己的一些规则(这规则后面再叙述)将程序编写顺序打乱——即写在后面的代码在时间顺序上可能会先执行，而写在前面的代码会后执行——以尽可能充分地利用CPU。拿上面的例子来说：假如不是a=1的操作，而是a=new byte[1024*1024](分配1M空间)，那么它会运行地很慢，此时CPU是等待其执行结束呢，还是先执行下面那句flag= true呢？显然，先执行flag=true可以提前使用CPU，加快整体效率，当然这样的前提是不会产生错误(什么样的错误后面再说) 。虽然这里有两种情况：后面的代码先于前面的代码开始执行；前面的代码先开始执行，但当效率较慢的时候，后面的代码开始执行并先于前面的代码执 行结束。不管谁先开始，总之后面的代码在一些情况下存在先结束的可能。
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
                          处理器提供的pause指令那么效率会有一定的提升，pause指令有两个作用，第一它可以延迟流水线执行指令（de-pipeline）,使CPU不会消耗过多的执行资源，延迟的时间取决于具体实现的版本，在一些处理器上延迟时间是零。
                          第二它可以避免在退出循环的时候因内存顺序冲突（memory order violation）
                          而引起CPU流水线被清空（CPU pipeline flush），从而提高CPU的执行效率。
 
                        3. 只能保证一个共享变量的原子操作。当对一个共享变量执行操作时，我们可以使用
	                       循环CAS的方式来保证原子操作，但是对多个共享变量操作时，循环CAS就无法保证操作的原子性，
						   这个时候就可以用锁，或者有一个取巧的办法，就是把多个共享变量合并成一个共享变量来操作。
						   比如有两个共享变量i＝2,j=a，合并一下ij=2a，然后用CAS来操作ij。从Java1.5开始JDK提供了
						   AtomicReference类来保证引用对象之间的原子性，你可以把多个变量放在一个对象里来进行CAS操作。
				     
                 11 同步器AQS的实现原理
				 
				     1） 一个抽象类，内部是一个先进先出的队列（CLH队列，自旋锁的队列，有点空间复杂度低），用双向链表实现，子类必须重写tryRelease和 tryAcquire    
					     的方法来自定义锁的操作，这个类有三个比较重要的变量head 表示当前持有锁的线程。tail 队尾等待的线程，state 表示锁状态（其实不同的状态下有不同的语义），state等于0表示目前锁可用，state=1 表示该锁被占用了一次，2 表示两次，以此类推，在AQS内部基于CAS对其进行更新。
						 
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
				     Condition 是个接口类，主要用于线程的等待和执行操作，包含wait cancel signal 注意不包含（lock 和 unlock 方法，那是属于lock的接口）的方法，主要实现原理是内部维护了一个等待线程的队列，当调用lock 方法时，
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
                         c)tryLock(long timeout,TimeUnit unit)， 如果获取了锁定立即返回true，如果别的线程正持有锁，会等待参数给定的时间，在等待的过程中，如果获取了锁定，就返回true，如果等待超时，返回false；
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
				    默认线程池 SERIAL_EXECUTOR一个进程只有一个
				    Callable 接口返回泛型，mWOkerThread 就是继承Callable;
				    FutureTask 真正的工作类，用于包装Callable, 实现Runnable,和Future类，Future 类定义了多个函数
				    一般用于线程是否执行完，cancel,get 执行结果
					
				   
				   
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
			用户空间的应用程序，通过系统调用，进入内核空间。这个时候用户空间的进程要传递 很多变量、参数的值给内核，内核态运行的时候也要保存用户进程的一些寄存 器值、变量等。所谓的“进程上下文”，可以看作是用户进程传递给内核的这些参数以及内核要保存的那一整套的变量和寄存器值和当时的环境等。
			相对于进程而言，就是进程执行时的环境。具体来说就是各个变量和数据，包括所有的寄存器变量、进程打开的文件、内存信息等。一个进程的上下文可以分为三个部分:用户级上下文、寄存器上下文以及系统级上下文。

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

               ViewRoot实现了View和WindowManager之间的消息传递。
			   
			   
			 
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
                ApplicationThread，用来实现ActivityManagerService与ActivityThread之间的交互。在ActivityManagerService
                需要管理相关Application中的Activity的生命周期时，通过ApplicationThread的代理对象与ActivityThread通讯。
               ApplicationThreadProxy，是ApplicationThread在服务器端的代理，负责和客户端的ApplicationThread通讯。
                AMS就是通过该代理与ActivityThread进行通信的。

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
                  客户端：ApplicationThread <=====Binder驱动<===== ApplicationThreadProxy：服务器，都实现了IApplicationThread方法。
				  
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
		 
				  Android provides layer of protection in that it doesn’t give one app access to the resource of another app. This is known as the ‘sandbox’ where every 
				  app gets to play in its own sandbox and can’t use another app’s toys! Android does this by giving each app a unique user id (a UID) and by running that 
				  app as a separate process with that UID. Only processes with the same UIDs can share resources which, as each ID is uniquely assigned, 
				  means that no other apps have permission.
				  This means that if an app tries to do something it shouldn’t, like read the data from another app, or dial the phone (which is a separate application) 
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
					BackStackRecord, 实现了runnable的接口，
					
					 3）然后调用replace 的方法， 这个时候会生成一个操作节点，叫Op，op里面有一个cmd
				    字段表示操作节点,再把op插入到一个双向链表里，
					
					 4）然后commit, commit 会执行BackStackRecord 的run方法， run方法里面，调用
				     moveToState，该方法内会根据操作的类型，以及Fragment 当前的状态执行相应生命周期的方法，当state 是Initializing 的是偶
					 就会执行fragment 的onAttach 和 onCreate 的方法
					 
		    17   Js 与 Android交互 方式有两种
 
                   1) 通过js 代码的方式

                   2) 通过自定义协议，Android在webview 过程中拦截url;

                   方式 1）双方可以互调 web可以调Android代码，android可以通过webview.loadUrl 方式调用web方法

                   方式 2）只能web调用android
				   
					  
		  六： JAVA 语言类
		   
		      1   apt 动态编译
	   
	             APT(Annotation processing tool) 是一种处理注释的工具,它对源代码文件进行检测找出其中的Annotation，使用Annotation进行额外的处理。
                 Annotation处理器在处理Annotation时可以根据源文件中的Annotation生成额外的源文件和其它的文件(文件具体内容由Annotation处理器的编写者决定),
				 APT还会编译生成的源文件和原来的源文件，将它们一起生成class文件.使用APT主要的目的是简化开发者的工作量。
                 因为APT可以编译程序源代码的同时，生成一些附属文件(比如源文件类文件程序发布描述文件等)，这些附属文件的内容也都是
				 与源代码相关的，换句话说，使用APT可以代替传统的对代码信息和附属文件的维护工作。
				 
			  
              2  注解Retention的种类：
			  
				 Source 只保留在源码中 编译会被忽视 比如我们通常用的deprecated 还有 surpressing warning
				 CLAss, 只保存在字节码文件中，不会在VM中运营，比如butternife 中的 注解
				 runtime,保存到运行时，比如retrofit 的POST,GET

			  3  类的加载机制
		 
			      1、Bootstrap Loader（启动类加载器）：加载System.getProperty("sun.boot.class.path")所指定的路径或jar。
                  2、Extended Loader（标准扩展类加载器ExtClassLoader）：加载System.getProperty("java.ext.dirs")所指定的路径或jar。在使用Java运行程序时，也可以指定其搜索路径，例如：java -Djava.ext.dirs=d:\projects\testproj\classes HelloWorld
				
				  1、命令行启动应用时候由JVM初始化加载
                  2、通过Class.forName()方法动态加载
                  3、通过ClassLoader.loadClass()方法动态加载
                  4、同一个ClassLoader加载的类文件，只有一个Class实例。但是，如果同一个类文件被不同的ClassLoader载入，则会有两份不同的ClassLoader实例（前提是着       两个类加载器不能用相同的父类加载器）
				  5 自定义加载器要重写findclass
 
                  3、AppClass Loader（系统类加载器AppClassLoader）：加载System.getProperty("java.class.path")所指定的路径或jar。在使用Java运行程序时，也可以加上-cp来覆盖原有的Classpath设置，例如： java -cp ./lavasoft/classes HelloWorld
		     
			  4   双亲委托机制以及原因
				    双亲委托模型的工作过程是：如果一个类加载器收到了类加载的请求，它首先不会自己去尝试加载这个类，而是把这个请求委托给父类加载器去完成，每一个层次的类加载器都是如此，因此所有的加载请求最终都应该传送到顶层的启动类加载器中，只有当父类加载器反馈自己无法完成这个加载请求（它的搜索范围中没有找到所需要加载的类）时，子加载器才会尝试自己去加载
					
					使用双亲委托机制的好处是：能够有效确保一个类的全局唯一性，当程序中出现多个限定名相同的类时，类加载器在执行加载时，始终只会加载其中的某一个类。

                     使用双亲委托模型来组织类加载器之间的关系，有一个显而易见的好处就是Java类随着它的类加载器一起具备了一种带有优先级的层次关系。例如类java.lang.Object，它存放在rt.jar之中，无论哪一个类加载器要加载这个类，最终都是委托给处于模型最顶端的启动类加载器进行加载，因此Object类在程序的各种加载器环境中都是同一个类。相反，如果没有使用双亲委托模型，由各个类加载器自行去加载的话，如果用户自己编写了一个称为java.lang.Object的类，并放在程序的ClassPath中，那系统中将会出现多个不同的Object类，Java类型体系中最基础的行为也就无法保证，应用程序也将会变得一片混乱。
					 双亲机制是为了保证java核心库的类型安全，不会出现用户自己能定义java.lang.Object类的情况。
					
			   5  类的初始化顺序
				  
				    属性、方法、构造方法和自由块都是类中的成员，在创建类的对象时，类中各成员的执行顺序：
                      1. 父类静态成员和静态初始化快，按在代码中出现的顺序依次执行。
                      2. 子类静态成员和静态初始化块，按在代码中出现的顺序依次执行。
                      3. 父类的实例成员和实例初始化块，按在代码中出现的顺序依次执行。
                      4. 执行父类的构造方法。
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
					   中定义的八种基本类型：boolean、char、byte、short、int、long、float、double）、部分的返回结果以及Stack Frame，非基本类型的对象在JVM栈上仅存放一个指向堆上的地址

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
							 
						9   For循环实现原理
							 
							 
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
			
			     

				 
        遗留问题

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




