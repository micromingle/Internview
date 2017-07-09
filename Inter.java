import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.CountDownLatch;

/**
 * Created by HP on 2017/6/23.
 */

public class Inter {

  /*   1  自定义控件大小的测量
           /view 本身,
           如何处理控件的WRAP_CONTENT,MATCH_PARENT
          //view group
           如何自定义一个控件

       2  WindowManagerService

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


            10、视频的处理

            11、超长图的处理

            12、Toast的Window创建过程

                Toast和Dialog不同，它的工作过程稍微复杂。首先Toast也是基于Window来实现的，

                但是由于Toast具有定时取消这一功能，所以系统采用了Handler。在Toast的内部有两类的IPC过程，

                第一类是Toast访问NotificationManagerService，第二类是NotificationManagerService回调Toast的TN接口。

                Toast属于系统Window，它内部的视图有两种方式指定，一种是系统默认的样式，

                另一种是通过setView方法来指定一个自定义View，不管如何，

                它们都对应Toast的一个View类型的内部成员mNextView。Toast提供了show和cancel分别用于显示和隐藏Toast，

                 它们的内部是一个IPC过程。


            13  ActvityManagerService

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




            14  Ndk //调用方式

            15  WindowManagerService的了解

            16  View的绘制

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

           // view group

              1  必须要重载onLayout() ,再在onLayout方法里 调用layout的方法确定孩子的位置

                 layout的方法 不能重载

              2  view 绘制的步骤 measure ---> layout ----> draw


           17 SurfaceView 和 TextureView; 如何保证线程安全

              1 TextureView 可以当做一般的view 使用，可以移动，翻转，只能在硬件加速时使用

              2 SurfaceView 窗口后面在创建一个surface

              3 实现SurfaceHolder.CallBack, 在onSurfaceCreated 方法里去开启一个线程去绘制，绘制前需要绘制lockCanvas,

              绘制完后要调用unlockAndPostCanvas;

          18 Tcp 的三次握手和四次挥手，以及等待两个msl周期

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

             滑动窗口控制 怎么验证数据顺序是否在正确；

           19 双缓冲

              当使用双缓冲时，首先在内存缓冲区里完成所有绘制操作，而 不是在屏幕上直接进行绘图。

              当所有绘制操作完成后，把内存缓冲区完成的图像直接复制 到屏幕。

               因为在屏幕上只执行一个图形操作，所以消除了由复杂绘制操作造成的图像闪烁 问题。

              1)  创建Bitmap 缓冲区   cacheBitmap

              2)  创建画new 一个canvas 对象

              3)  调用canvas.setBitmap 的方法，并调用canvas.drawbitmap 的方法



           20    为什么可以在子线程中更新UI

          21  (4)  getMeasuredWidth()与getWidth() 区别；

                  getMeasuredWidth()与getWidth()的区别。他们的值大部分时间都是相同的，但意义确是根本不一样的，我们就来简单分析一下。

                  区别主要体现在下面几点：

                 -  首先getMeasureWidth()方法在measure()过程结束后就可以获取到了，而getWidth()方法要在layout()过程结束后才能获取到。

                 -  getMeasureWidth()方法中的值是通过setMeasuredDimension()方法来进行设置的，

                    而getWidth()方法中的值则是通过layout(left,top,right,bottom)方法设置的。


            22   onMeasure 在遍历各个孩子的时候，需要调用measureChild 来获得孩子的宽度和高度。

            23   ndk 优化，so文件适配问题

                1   armeabi设备只兼容armeabi；
                    armeabi-v7a设备兼容armeabi-v7a、armeabi；
                    arm64-v8a设备兼容arm64-v8a、armeabi-v7a、armeabi；
                    X86设备兼容X86、armeabi；
                    X86_64设备兼容X86_64、X86、armeabi；
                    mips64设备兼容mips64、mips；
                    mips只兼容mips；

               2   目前主流的Android设备肯定是armeabi-v7a架构的
                   针对以上情况，我们可以应用的设备分布和市场情况再进行取舍斟酌，
                   如果你的应用仍有不少armeabi类型的设备，可以考虑只保留armeabi
                   目录下的SO文件（万金油特性）。但是，尽管armeabi可以兼容多种平台，
                   仍有些运算在armeabi-v7a、arm64-v8a去使用armeabi的SO文件时，
                   性能会非常差强人意，所以还是应该用其对应平台架构的SO文件进行运算。
                   注意，这里并不是要带多一整套SO文件到不同的目录下，而是将性能差异
                   比较明显的某个armeabi-v7a、arm64-v8a平台下的SO文件放到armeabi目录，
                   然后通过代码判断设备的CPU类型，再加载其对应架构的SO文件，
                   很多大厂的应用便是这么做的。如微信的lib下虽然只有armeabi一个目录，
                   但目录内的文件仍放着v5、v7a架构的SO文件，用于处理兼容带来的某些
                   性能运算问题。

            24   心跳机制的了解

                 长连接保活

            25   内存优化 dumb mat

            26   H264

                1) GPU软解码    2） CPU硬解码   3) android 用 mediaCodec;

                 4）一路解码和多路解码   5）h264 压缩   6）rtmp


            28   rtsp

            29   视频流的处理  我的店铺  详细流程介绍

                 AudioTrack 播放音频，采集是pcm_16bit,原始的视频流是yuv420 的数据 要先转为rgb565; 软解通过cpu;

                 1）获取视频流

                 2) 如果不支持opengles 2.0 使用cpu解码 调用本地方法把yuv的数据转为rgb 然后再转成bitmap 显示在图片上

                 如果支持gpu解码 openglES GlSurfaceView 渲染 通过shader language 把yuv 转为rgb ，然后进行渲染

                 每一帧调用requestRender  刷新界面

                  相关链接 http://blog.csdn.net/ueryueryuery/article/details/17608185

                 3） 然后展示

                 4）p2p 的原理 有一台服务器s 记录着所有工作的摄像头 ， 当一台电脑想看某一台摄像头时，发个请求给s,
                    s把某一台摄像机的ip地址和端口号发给该电脑，然后监控

           30   有意思的控件
               0   TagLayout 几点误区

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



            31   GLSurfaceView;

                 具有生命周期GLSufaceView    glSurfaceView.bringToFront()， glSurfaceView.onPause();

                 Renderer 真正渲染线程， 有三个函数 ，onDrawFrame， onSurfaceChanged ，onSurfaceCreated

                 YUV转RGB 颜色空间 明度亮度饱和度， 与RGB 之间有固定转换公式

                 YCbCr 4:2:0   亮度和色度    1.5byte        Y0 U0 Y1 Y2 U2 Y3 Y5 V5 Y6 Y7 V7 Y8（8像素为例）

                 requestRender 方法刷新页面

                 渲染包括vertex shader 和 fragment shader

              32  MVP的精髓就是 多出一个presenter 类，表示用户执行的操作， 在Activity或fragment进行交互时，不直接操作他们当中的视图

                  而是抽象出一个接口，直接与抽象接口进行交互，不用去管具体的实现，activity 或者fragment 需要去实现这个抽象类。

              33   Js 与 Android交互 方式有两种

                  1) 通过js 代码的方式

                  2) 通过自定义协议，Android在webview 过程中拦截url;

                 方式 1）双方可以互调 web可以调Android代码，android可以通过webview.loadUrl 方式调用web方法

                  方式 2）只能web调用android


            34    Activity 4 种启动模式

                 1  singleTop  如果顶部有该实例，则不实例化新的activity, 调用onNewIntent, 如果没有新实例，就创建新的;

                 2  singleTask 表示一个栈种只能有一个，如果一个栈中，有一个实例，若重新开启，则会把该实例上的所有Acitivity都destroy 掉  并调用onNewIntent;

                 3  standard 每次startActivity 就创建一个新实例

                 4  singleInstance 一个Activity 单独一个栈


            35   多线程

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

            /* current 值，next 值， 在compareAndSet 将current 值与 内存中

              当前值对比，如果是相等的，才update 成next 的值

               如果不相等的话  重新循环；*/


            /*  2）Lock

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

                      公平锁比较慢   */

       /*   36   apt 动态编译

            37   java 的容器

            38   线程池

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

            39   生产者，消费者模式

            40   CountDownLatch,  CycleBarrier;

               CountDownLatch 示例代码*/

    public class Sample {
        /**
         * 计数器，用来控制线程
         * 传入参数2，表示计数器计数为2
         */
        private final CountDownLatch mCountDownLatch = new CountDownLatch(2);

        /**
         * 示例工作线程类
         */
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
    //CyclicBarrier 示例代码 示例代码

    // http://www.java-redefined.com/p/cyclicbarrier.html

    // CountDownLatch 和 CyclicBarrier 区别
    //  1  CyclicBarrier rest以后可以复用
    //  2  When using a CyclicBarrier, the assumption is that you specify the number of waiting
    // threads that trigger the barrier. If you specify 5,
    // you must have at least 5 threads to call await().

    // When using a CountDownLatch, you specify the number of calls to countDown()
    // that will result in all waiting threads being released. This means that you
    // can use a CountDownLatch with only a single thread

    // 41   Condition  signal and await ; 用于条件判断,或者唤醒其他线程
    //  和  Object#wait() wait}, {@link Object#notify notify}, and {@link
    //    Object#notifyAll notifyAll} 方法的功能类似

    //  用法

    //42  进程：

    // 进程间的通信
    // 通常的做法是，发送方将准备好的数据存放在缓存区中，
    // 调用API通过系统调用进入内核中。内核服务程序在内核空间分配内存，
    // 将数据从发送方缓存区复制到内核缓存区中。接收方读数据时也要提供一
    // 块缓存区，内核将数据从内核缓存区拷贝到接收方提供的缓存区中并唤醒接收线程，
    // 完成一次数据发送。
    // 发送方缓冲区-----》内核缓冲区-----》接收方缓冲区
    // 由Binder驱动负责管理数据接收缓存。我们注意到Binder驱动实现了mmap()系统调用，
    // 这对字符设备是比较特殊的，因为mmap()通常用在有物理存储介质的文件系统上，
    // 而象Binder这样没有物理介质，纯粹用来通信的字符设备没必要支持mmap()。
    // Binder驱动当然不是为了在物理介质和用户空间做映射，
    // 而是用来创建数据接收的缓存空间。
    // 发送方缓冲区 -----》接收方缓冲区 节省了一个环节
    // 通过上面介绍可以看到，驱动为接收方分担了最为繁琐的任务：分配/释放大小不等，难以预测的有效负荷缓存区，
    // 而接收方只需要提供缓存来存放大小固定，最大空间可以预测的消息头即可。在效率上，由于mmap()
    // 分配的内存是映射在接收方用户空间里的，所有总体效果就相当于对有效负荷数据做了一次从发送方用户
    // 空间到接收方用户空间的直接数据拷贝，省去了内核中暂存这个步骤，提升了一倍的性能。
    // Client、Server和Service Manager运行在用户空间，Binder驱动程序运行内核空间


    // 用户空间 和 内核空间 进程上下文，，transact 发送数据 onTransact 接收数据

    //43  用户空间和内存空间

    // 内核空间中存放的是内核代码和数据，而进程的用户空间中存放的是用户程序的代码和数据。
    // 不管是内核空间还是用户空间，它们都处于虚拟空间中。

    // Linux 操作系统和驱动程序运行在内核空间，应用程序运行在用户空间，
    // 两者不能简单地使用指针传递数据，因为Linux使用的虚拟内存机制，用户空间的数据可能被换出，
    // 当内核空间使用用户空间指针时，对应的数据可能不在内存中。

    // 针对linux操作系统而言，将最高的1G字节（从虚拟地址0xC0000000到0xFFFFFFFF），
    // 供内核使用，称为内核空间，而将较低的3G字节（从虚拟地址0x00000000到0xBFFFFFFF），
    // 供各个进程使用，称为用户空间。每个进程可以通过系统调用进入内核，因此，
    // Linux内核由系统内的所有进程共享。于是，从具体进程的角度来看，
    // 每个进程可以拥有4G字节的虚拟空间。

    // 44 进程上下文

    // （1）内核态，运行于进程上下文，内核代表进程运行于内核空间。
    // （2）内核态，运行于中断上下文，内核代表硬件运行于内核空间。
    // （3）用户态，运行于用户空间。

    // 上下文context： 上下文简单说来就是一个环境。
    // 用户空间的应用程序，通过系统调用，进入内核空间。这个时候用户空间的进程要传递 很多变量、参数的值给内核，内核态运行的时候也要保存用户进程的一些寄存 器值、变量等。所谓的“进程上下文”，可以看作是用户进程传递给内核的这些参数以及内核要保存的那一整套的变量和寄存器值和当时的环境等。
    // 相对于进程而言，就是进程执行时的环境。具体来说就是各个变量和数据，包括所有的寄存器变量、进程打开的文件、内存信息等。一个进程的上下文可以分为三个部分:用户级上下文、寄存器上下文以及系统级上下文。

    //  （1）用户级上下文: 正文、数据、用户堆栈以及共享存储区；
    //  （2）寄存器上下文: 通用寄存器、程序寄存器(IP)、处理器状态寄存器(EFLAGS)、栈指针(ESP)；
    //  （3）系统级上下文: 进程控制块task_struct、内存管理信息(mm_struct、vm_area_struct、pgd、pte)、内核栈。

    //   当发生进程调度时，进行进程切换就是上下文切换(context switch).操作系统必须对上面提到的全部信息进行切换，
    //   新调度的进程才能运行。而系统调用进行的模式切换(mode switch)。模式切换与进程切换比较起来，容易很多，
    //   而且节省时间，因为模式切换最主要的任务只是切换进程寄存器上下文的切换。

    // 44.5 父进程/子进程
       // 特点 1）调用一次返回两次  2）写时复制
       //       1) 调用一次，返回两次，原因，父进程创建子进程以后，pc寄存器的指令会定位到同一个地方
       //          两个进程会同时执行同一段代码，所以会返回两次。父进程返回子进程id, 子进程，如果创建
       //          成功返回0，创建不成功返回负数
       //       2) 写时复制
       //           在fork之后exec之前两个进程用的是相同的物理空间（内存区），子进程的代码段、数据段、
       //           堆栈都是指向父进程的物理空间，也就是说，两者的虚拟空间不同，但其对应的物理空间是同一个。
       //           当父子进程中有更改相应段的行为发生时，再为子进程相应的段分配物理空间

    // 44.6 进程的组成
              // jvm 方法区，堆，栈，本地方法区，pc register 用于存储指令
              // 在Linux系统中进程由以下三部分组成：①c；②数据段；③正文段。

    //  45  Binder 机制介绍

           // 1) 为什么不用传统的进程通信方式,
           //      IPC              内存拷贝次数
           //     共享内存                 0
           //     Binder                  1
           //    Socket/管道/消息队列       2
           //   其中使用共享内存，有线程安全问题，使用Socket/管道/消息队列 需要复制两次内存，性能消耗较大
           //   一般进程间的通信，
           //   通常的做法是，发送方将准备好的数据存放在缓存区中，
           //   调用API通过系统调用进入内核中。内核服务程序在内核空间分配内存，
           //   将数据从发送方缓存区复制到内核缓存区中。接收方读数据时也要提供一
           //    块缓存区，内核将数据从内核缓存区拷贝到接收方提供的缓存区中并唤醒接收线程，
           //    完成一次数据发送。 而由Binder驱动负责管理数据接收缓存。我们注意到Binder驱动实现了mmap()系统调用，
           //    这对字符设备是比较特殊的，因为mmap()通常用在有物理存储介质的文件系统上，
           //    而象Binder这样没有物理介质，纯粹用来通信的字符设备没必要支持mmap()。
           //    Binder驱动当然不是为了在物理介质和用户空间做映射，
           //    而是用来创建数据接收的缓存空间。 通过上面介绍可以看到，驱动为接收方分担了最为繁琐的任务：分配/释放大小不等，难以预测的有效负荷缓存区，
           //    而接收方只需要提供缓存来存放大小固定，最大空间可以预测的消息头即可。在效率上，由于mmap()
           //    分配的内存是映射在接收方用户空间里的，所有总体效果就相当于对有效负荷数据做了一次从发送方用户
           //    空间到接收方用户空间的直接数据拷贝，省去了内核中暂存这个步骤，提升了一倍的性能。
           // 2) Binder 调用的方式；
                 //binder 调用的组成client,server,ServiceManager 用来注册和搜索服务，以及Binder 驱动
                 // client ---->SM查找service，并返回该service的一个代理proxy --->
                 // Binder 驱动（与binder驱动进行通信）--->server收到请求并执行，【回复（）比如ActivityManagerService
                 // 会通过ProcessRecord 里面的IApppllicationThread 来通信】

    //  46  二分查找的代码

    //  47 java容器的学习

    //  48  进程隔离的实现
            // 根源：如果直接使用物理内存，进程A会直接访问进程B的内存，导致奔溃
            // 解决:
            // 为了解决上述问题，人们想到了一种变通的方法，就是增加一个中间层，利用一种间接的地址访问方法访问物理内存。
            // 按照这种方法，程序中访问的内存地址不再是实际的物理内存地址，而是一个虚拟地址，然后由操作系统将这
            // 个虚拟地址映射到适当的物理内存地址上。这样，只要操作系统处理好虚拟地址到物理内存地址的映 射，
            // 就可以保证不同的程序最终访问的内存地址位于不同的区域，彼此没有重叠，就可以达到内存地址空间隔离的效果。
             //当创建一个进程时，操作系统会为该进程分配一个 4GB 大小的虚拟进程地址空间

    // ArrayMap SparseArray 优缺点：
    // 1） 省内存，避免装箱操作，HashMap 的Entry 的Key 和 value 并不支持基本数据类型，
    // 需要装箱操作
    // 2）缺点查找速度慢，当只有几百个items 时候 和传统的hashmap 查找的差别不大于50%
    //    当数据量变大的时候，差别就变得显著了。 设计思想是空间换时间，节省内存，
    //    但是查找速度和传统的haspmap 相比变慢

    // 49 动态代理
    // jdk动态代理的应用前提，必须是目标类基于统一的接口。如果没有上述前提，jdk动态代理不能应用。
    // 内部基于反射实现  示例代码：

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

    // 50 开源框架介绍Retrofit2 介绍和 butterknife 介绍
        // 一： Retrofit 组成部分，使用先定义一个接口
          // 1) 注解部分 包含请求方法如POST,GET,查询参数 QUERY  请求头HEADERS
          // 2) 网络请求部分  使用okhttp
          // 3) 请求处理部分，加入converter，支持gson,xml,protobuffer
          // 4) RequestBuilder 类,用于构建请求
          // 5) Response 接收返回
          // 6) Retrofit 初始化请求的各类参数，使用生成器模式 builder 模式
          // 调用过程  通过动态代理生成对象newProxyInstance , 调用某一个方法时
          // 调用invoke 的方法，传入Method 参数，通过method 的各类注解获得请求的各个参数
          // 并生成一个叫ServiceMethod的对象，并缓存，ServiceMethod 中有一个toRequest 方法
          // 生成RequestBuilder 对象，并把请求加入okhttp的请求队列

       // 二: Okhttp的介绍
             // 1)连接池  OkHttpClient 保存着5个Socket 长连接,每个长连接存活时间5分钟，
                         //每个连接都维护者一个引用计数，当该连接被使用时，计数加1，当连接释放时
                         // 引用计数减一，每次添加新连接时，会执行cleanup操作
             // 2)缓存    //带有本地的响应缓存，用LruDiskCache 实现把url做md5处理以后用作key,每次请求
                         //会先获取本地响应缓存，以后再根据请求的请求头进行处理，是决定使用该缓存还是
                         // 还是直接重新请求云端；// 缓存清除功能，每次set/get的调用
             // 3)任务调度 // 维护一个核心数为0，无最大线程数量的线程池，当工作完成后，线程池会在60s后相继关闭所有线程
                         // 默认最多64个请求，内部会用维护两个双端队列，一个是正在执行的队列，一个是正在等待的队列，
                         // 一个请求进来以后，若请求数已经达到最大，则将请求放入等待队列，反之就立即执行，
                         // 并放入正在执行的对列，当一个请求执行完成以后，会根据当前情况，把正在等待的请求拿出来执行
             // 遗留问题  deque 以及线程池 Synchronous Queue 实现；

        // 三 universal Image loader

              // 缓存策略：
                // 基本过程 文件从云端下到本地，再从本地读取缓存到内存中
               // 1）硬盘缓存策略
                    // LruDiskCache
                   // LimitedAgeDiscCache（设定文件存活的最长时间，当超过这个值，就删除该文件）
                   // UnlimitedDiscCache（这个缓存类没有任何的限制）
               // 2）内存缓存策略
			        //综述: 总的接口是MemoryCache,
					//    LruMemoryCache 直接继承MemoryCache 使用的是强引用
					//    其余memoryCache 继承BaseMemoryCache, 弱引用和强引用结合，BaseMemoryCache 中有一个hardMap 用于保存强引用
                    // 		子类再自行保存另外的缓存softMap,当图片的缓存的大小在最大限制以内 就保存在hardMap,否则保存在softMap里面			
                    // 1. 只使用的是强引用缓存
                    //  LruMemoryCache（这个类就是这个开源框架默认的内存缓存类，缓存的是bitmap的强引用，下面我会从源码上面分析这个类）
                    // 2.使用强引用和弱引用相结合的缓存有
                    // UsingFreqLimitedMemoryCache（如果缓存的图片总量超过限定值，先删除使用频率最小的bitmap）
                    // LRULimitedMemoryCache（这个也是使用的lru算法，和LruMemoryCache不同的是，他缓存的是bitmap的弱引用）
					//      实现容器是LinkedHashMap;
                    // FIFOLimitedMemoryCache（先进先出的缓存策略，当超过设定值，先删除最先加入缓存的bitmap）
                    // LargestLimitedMemoryCache(当超过缓存限定值，先删除最大的bitmap对象)
                    // LimitedAgeMemoryCache（当 bitmap加入缓存中的时间超过我们设定的值，将其删除）
                   // 3.只使用弱引用缓存
                    // WeakMemoryCache（这个类缓存bitmap的总大小没有限制，唯一不足的地方就是不稳定，缓存的图片容易被回收掉）
               // 3）核心类ImageLoaderTask, 加载图片 ImageDecodingInfo 存储图片解码信息,ImageConfiguration：各种参数设置;
                     // bitmap display task 展示图片
    // 51 进程回收顺序
           //1) foreground activity 如果该进程中含有foreground activity或者是Service 被设置成Foreground,正在与用户进行交互，只有在该进程使用的内存
                //比剩下的内存多，才会被回收
           //2) visible activity  如果该activity 不是foreground activity，比如说在一个dialog后面，这个activity也非常重要
                //只有在内存非常不足，为了保证其他foreground activity 运行，进程才会被回收
           //3) background activity  就是该activity 不可见， 当为了保证 foreground activity 和 visible activity,运行时
                // background activity 才会被回收， 当这个activity 被销毁后，但是又回到前台时 会重新创建activity
              // 并将之前在 onSaveInstanceState 保存的bundle 返回，执行onRestoreInstance 方法
           // 4) empty process 空进程，不含有activity , 或者service, 或者broadcast receiver , 当内存不足时，很容易被回收
    // 52  项目介绍

    // 53  Activity 启动流程

//
//        Activity.startActivity ---> Instrumentation.exeStartActivity --->
//        ActivityManagerNative.getDefault().startActivity(）；// getDefault返回的其实是
//        ActivityManagerProxy对象，调用binder.transact方法，将信息传给binder 驱动
//        protected IActivityManager create() {
//        // 去servie manager 查询service,返回ServiceManager的Binder
//        IBinder b = ServiceManager.getService("activity");
//        if (false) {
//            Log.v("ActivityManager", "default service binder = " + b);
//        }
//        IActivityManager am = asInterface(b);
//        if (false) {
//            Log.v("ActivityManager", "default service = " + am);
//        }
//        return am;
//    }
//
//    public IActivityManager asInterface(IBinder obj) {
//        if (obj == null) {
//            return null;
//        }
//        // 从servicemanager 查找代理
//        IActivityManager in =
//                (IActivityManager)obj.queryLocalInterface(descriptor);
//        if (in != null) {
//            return in;
//        }
//
//        return new ActivityManagerProxy(obj);
//    }
//
//    Binder 传输数据大小 最多 800kb
//     在onTransact 返回处理结果 通过 IApplicationThread 的一个实现类 实际上是ApplicationThreadProxy；

      // 54 注解Retention的种类：
            // Source 只保留在源码中 编译会被忽视 比如我们通常用的deprecated 还有 surpressing warning
            // CLAss, 只保存在字节码文件中，不会在VM中运营，比如butternife 中的 注解
            // runtime,保存到运行时，比如retrofit 的POST,GET

      // 55 https 握手过程
             // 1) 客户端发送请求 ，服务端产生公钥和私钥，将私钥发送给客户端
             // 2) 客户端随机产生秘钥，用公钥加密 产生的秘钥，发送给服务端
             // 3) 服务端用私钥解密，得出秘钥，用得出的秘钥进行加密信息，进行传输
			 
			 
      // 56  view 的支持滑动ScrollView 的实现 ，onInterceptTouchEvent

      // 57 onInterceptTouchEvent  只有ViewGroup 的onInterceptTouchEvent 返回false,
        // 子view的onTouchEvent 才能收到事件
		
		
		
      // 58 内存泄露的几种方式
            // 1)监听器没有及时关闭
            // 2)非静态内部类持有外部类的引用  比如asyncTask 写一个静态的内部类
            // 3) 未正确使用context 导致Actvity 无法被回收，
            // 4) Handler 内存泄露
            // 5) 资源对象未关闭
                 // 资源对象比如Cursor、File等，往往都用了缓冲，不使用的时候应该关闭它们。
                 // 把他们的引用置为null，而不关闭它们，往往会造成内存泄漏。因此，在资源对象不使用时，
                 // 一定要确保它已经关闭，通常在finally语句中关闭，防止出现异常时，资源未被释放的问题。
            // 6） webview 内存泄露，将他单独放在一个进程里
            // 7)  2.8 集合中对象没清理
                   // 通常把一些对象的引用加入到了集合中，当不需要该对象时，如果没有把它的引用从集合中清理掉，
                   // 这样这个集合就会越来越大。如果这个集合是static的话，那情况就会更加严重。

            //8) 2.9 Bitmap对象
                  //临时创建的某个相对比较大的bitmap对象，在经过变换得到新的bitmap对象之后，应该尽快回收原始的bitmap，这样能够更快释放原始bitmap所占用的空间。
                 // 避免静态变量持有比较大的bitmap对象或者其他大的数据对象，如果已经持有，要尽快置空该静态变量。

           // 59 fragment 创建过程
                // 大体生命周期：onAttach --> onCreate --> onCreateView --> onViewCreated --> onActivityCreated
                //            ---->onResume-->onStop-->onDestroyView -->  onDestroy --onDetach;

                 // 1 ）FragmentController 包含的实现类和FragmentHostCallBack, Callback 包含 FragmentManager   会在onCreate
                // 调用attachHost container fragment 由于我们是最顶层，故传入null;
                 // 2）我们一般会在Activity 的onCreate 方法里调用
                //     添加fragment 的代码，先getFragmentManager,然后在beginTransaction, beginTraction是开启一个事务，这个事务是
                //      BackStackRecord, 实现了runnable的接口，
                 // 3）然后调用replace 的方法， 这个时候会生成一个操作节点，叫Op，op里面有一个cmd
                      // 字段表示操作节点,再把op插入到一个双向链表里，
                 // 4）然后commit, commit 会执行BackStackRecord 的run方法， run方法里面，调用
                    // moveToState，该方法内会根据操作的类型，以及Fragment 当前的状态执行相应生命周期的方法，当state 是Initializing 的是偶
                   // 就会执行fragment 的onAttach 和 onCreate 的方法
         // 60 上次功能的实现，, system trace，viewStub;

          // View Stub 用法
           // 占位标签，通常与android:layout 属性一起使用，制定一个布局，默认不展示。当有需要时调用
           // inflate 展示调用代码如下
           //   ViewStub noDataViewStub = (ViewStub) view.findViewById(R.id.no_data_viewstub);
           //    noDataView = noDataViewStub.inflate();

         // 61 SurfaceView 黑屏事件

          // 62 view 滑动冲突的解决 / 滚动冲突的解决

         // 63  各种touch Event 事件详解
		       ViewGroup 包含onInterceptEvent , 每次view触摸后，就会执行onInterceptEvent, 假如返回true的话，说明父视图自己会响应
			   然后再在onTouchEvent 进行处理事件，并向子视图发送Action_CANCEL 事件。假如返回false,则会把事件传给子视图
		 
		 // How the Activity handles touch:
		    
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

         // 64  操作系统

         // 65  类的加载机制
		 
		     // 加载器的种类：
			    1、Bootstrap Loader（启动类加载器）：加载System.getProperty("sun.boot.class.path")所指定的路径或jar。
                2、Extended Loader（标准扩展类加载器ExtClassLoader）：加载System.getProperty("java.ext.dirs")所指定的路径或jar。在使用Java运行程序时，也可以指定其搜索路径，例如：java -Djava.ext.dirs=d:\projects\testproj\classes HelloWorld
 
                3、AppClass Loader（系统类加载器AppClassLoader）：加载System.getProperty("java.class.path")所指定的路径或jar。在使用Java运行程序时，也可以加上-cp来覆盖原有的Classpath设置，例如： java -cp ./lavasoft/classes HelloWorld
		    // 特点  
			    三、双亲委托机制以及原因
				    双亲委托模型的工作过程是：如果一个类加载器收到了类加载的请求，它首先不会自己去尝试加载这个类，而是把这个请求委托给父类加载器去完成，每一个层次的类加载器都是如此，因此所有的加载请求最终都应该传送到顶层的启动类加载器中，只有当父类加载器反馈自己无法完成这个加载请求（它的搜索范围中没有找到所需要加载的类）时，子加载器才会尝试自己去加载
					
					使用双亲委托机制的好处是：能够有效确保一个类的全局唯一性，当程序中出现多个限定名相同的类时，类加载器在执行加载时，始终只会加载其中的某一个类。

                     使用双亲委托模型来组织类加载器之间的关系，有一个显而易见的好处就是Java类随着它的类加载器一起具备了一种带有优先级的层次关系。例如类java.lang.Object，它存放在rt.jar之中，无论哪一个类加载器要加载这个类，最终都是委托给处于模型最顶端的启动类加载器进行加载，因此Object类在程序的各种加载器环境中都是同一个类。相反，如果没有使用双亲委托模型，由各个类加载器自行去加载的话，如果用户自己编写了一个称为java.lang.Object的类，并放在程序的ClassPath中，那系统中将会出现多个不同的Object类，Java类型体系中最基础的行为也就无法保证，应用程序也将会变得一片混乱。
					 双亲机制是为了保证java核心库的类型安全，不会出现用户自己能定义java.lang.Object类的情况。
					
					
					
				   
				 
				 五、类的加载
 
                 类加载有三种方式：
                  1、命令行启动应用时候由JVM初始化加载
                  2、通过Class.forName()方法动态加载
                  3、通过ClassLoader.loadClass()方法动态加载
                  4、同一个ClassLoader加载的类文件，只有一个Class实例。但是，如果同一个类文件被不同的ClassLoader载入，则会有两份不同的ClassLoader实例（前提是着   两个类加载器不能用相同的父类加载器）
				  
				   5 自定义加载器要重写findclass
				  
				  六， 类的初始化顺序
				  
				    属性、方法、构造方法和自由块都是类中的成员，在创建类的对象时，类中各成员的执行顺序：
                      1.父类静态成员和静态初始化快，按在代码中出现的顺序依次执行。
                      2.子类静态成员和静态初始化块，按在代码中出现的顺序依次执行。
                      3. 父类的实例成员和实例初始化块，按在代码中出现的顺序依次执行。
                      4.执行父类的构造方法。
                      5.子类实例成员和实例初始化块，按在代码中出现的顺序依次执行。
                      6.执行子类的构造方法。
					   
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

         // 66 热修复技术Tinker Andfix Robust // Instant run 原理

         // 67 大端 小端

         // 68 udp 校验
		 
		 // 70 restful
		 // 69 zygote Android通过zygote生成其他程序和进程
		 
		    //  为什么用zygote
			    
				// 1) Linux 下进程的产生 是通过fork函数产生的，是c++函数，应用程序使用java写的，应用直接调肯定不方便，
  				// 所以包装了一个zygote 给大家使用
				// 2)zygote启动以后 会向系统注册Socket ， 监听进来的socket 请求用户创建进程
				// 3)当zygote被杀死后，其他app都会死亡，写时复制引起的（猜想）;
				
         //70 Android 沙盒机制
		 
		  Android provides layer of protection in that it doesn’t give one app access to the resource of another app. This is known as the ‘sandbox’ where every 
		  app gets to play in its own sandbox and can’t use another app’s toys! Android does this by giving each app a unique user id (a UID) and by running that 
		  app as a separate process with that UID. Only processes with the same UIDs can share resources which, as each ID is uniquely assigned, 
		  means that no other apps have permission.
          This means that if an app tries to do something it shouldn’t, like read the data from another app, or dial the phone (which is a separate application) 
		  then Android protects against this because the app doesn’t have the right privileges.
		 
		  71 AsyncTask 源码分析 //双端队列Dequeue
		  
		    // 核心线程数：根据CPU核心数，至少两个，最多四个，最好可以比cpu核心数少1，以免占满cpu
			// 最大线程数 核心数的2倍加一，存活时间30秒；
			// 排队策略：LInkedBlockQUeue,最多128个等待线程
			 // 线程池当前版本串行，串行--》并发---》串行
			 // 默认线程池 SERIAL_EXECUTOR一个进程只有一个
			 // Callable 接口返回泛型，mWOkerThread 就是继承Callable;
			  // FutureTask 真正的工作类，用于包装Callable, 实现Runnable,和Future类，Future 类定义了多个函数
			  // 一般用于线程是否执行完，cancel,get 执行结果
			  
			  72 AbstractQUeuedSnchronizer 内部是链表，现先进先出的队列
			  
		    73  Semaphores
			
			 74  视图的缩放和拖动
			 
			 75  多线程部分
			 
			     6.1 synchronized和volatile理解
                 6.2 Unsafe类的原理，使用它来实现CAS。因此诞生了AtomicInteger系列等
                 6.3 CAS可能产生的ABA问题的解决，如加入修改次数、版本号
                 6.4 同步器AQS的实现原理
                 6.5 独占锁、共享锁；可重入的独占锁ReentrantLock、共享锁 实现原理
                 6.6 公平锁和非公平锁
                 6.7 读写锁 ReentrantReadWriteLock的实现原理
                 6.8 LockSupport工具
                 6.9 Condition接口及其实现原理
                 6.10 HashMap、HashSet、ArrayList、LinkedList、HashTable、ConcurrentHashMap、TreeMap的实现原理
                 6.11 HashMap的并发问题
                 6.12 ConcurrentLinkedQueue的实现原理
                 6.13 Fork/Join框架
                 6.14 CountDownLatch和CyclicBarrier
				 
			   76  JVM
			 
    //    二  开发遇到的难点回顾
    //
    //        1    超长图的处理
    //
    //    写了一个自定义控件 继承ImageView;
//
//            1）先用option,获得图片原始的大小，Bitmap region decoder;
//
//            2) 初始化一个Rect 的对象 来保存解码区域和绘制区域，用于bitmap region decoder 解码， 默认展示图片的的中心区域
//
//            3) 重写onTouchEvent,根据移动距离修改绘制区域，调用invalidate;// 缩放用matrix 这个类
//
//
//           2   推送保活，提高存活率
//
//
//           1) 方案一： onDestroy 时唤起自己
//
//           2）方案二： 写一个广播每隔一段时间唤起自己
//
//           3）方案三： 观察别人存活久的app 是怎么做的，发现存活久的app怎么做的
//
//                   反编译微信 看其实现，发现和以上实现相同
//
//                    继续查找原因，关注其公众号，看他的文章，发现和厂商合作紧密，
//
//                    得出结论可能是被加了白名单
//
//           4）替代方案：
//
//              用友盟推送，友盟被阿里收购，其实就是用的淘宝的推送，通过旗下大量日活高的app，
//
//               相互唤起，有种推送联盟的感觉。
//
//
//            3   监听卸载 监听自己的app 是否被卸载
//
//                linux 移动系统

//

}
