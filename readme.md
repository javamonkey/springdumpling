1:协作服务
=====
```java
/**
 * 类似于Spring的@Service所有标记@CooperationService的类都将检查类方法里是否有如下annotation 
 */
@CooperationService
/**
 * path    : 一个逻辑路径,必须要
 * ruleExp : 根据输入值,输出值的表达式判断是否需要Publish,否则,总是通知其他机器.如规则rule="return Value==true",默认是发送
 * argExp  : 一个参数表达式列表,如果没有,则按照输入参数和输出参数作为参数列表,传递个sub.参数格式如argExp="args[0].orderId,args[1],returnValue;"
 */
@Publish
/**
 * path      : 一个逻辑路径,必须要
 * runPolicy : 运行时机,有俩个值,一个是sameTransacion,表示于publish方法在同一事物里,另外一个是afterCommint,这是默认值,
 *             表示当事物成功提交后异步执行,如果Publish没有在事物上下文里,则@publish方法执行完毕后立刻执行
 */
@Subscribe
/**
 * path : 一个逻辑路径,一旦机器(JVM)获取锁,将永久占有锁,机器宕机或者失去连接,将导致其他机器中的某一个占用
 *        allowAcessAsFistTime: false或者true,默认false.如果为true,则允许第一次调用忽略锁
 */
@ClusterSync
/**
 * 同Publish,但是发布到远程
 * path   : 一个路径
 * ruleEx : 根据输入值,输出值判断是否需要Notify,默认是returnValue!=null ,否则,总是通知其他机器.如规则rule="returnValue==true"
 * argExp : 一个参数表达式列表,如果没有,则按照输入参数和输出参数作为参数列表,传递个sub.
 *          参数格式如：argExp="arg0=input[0].orderId;arg1=input[0].cash;arg2=returnValue;"
 */
@RemotePublish
/**
 * path:一个路径.
 */
@RemoteSubscriber
@RemoteSynronized
/**
 * 远程只能有一个被执行
 * path   : 一个路径
 * rule   : 根据输入值,输出值的表达式判断是否需要Notify,否则,总是通知其他机器. 如规则rule="returnValue==true",默认是发送
 * argExp : 一个参数表达式列表,如果没有,则按照输入参数和输出参数作为参数列表,传递个sub.
 *          参数格式如：argExp="arg0=input[0].orderId;arg1=input[0].cash;arg2=returnValue;"
 */
@RemoteNotify
/** path:一个路径 */
@RemoteWait
@Process / @Task // 待定
```

2:协作服务提供者
=====
> 每组annotation可以有自己的协作服务提供者,或者共用一种协作服务提供者(如果服务提供者都支持).
> 协作服务提供时机应该是系统启动成功后(也包括系统各个组件初始化成功后)
> Remote的协作服务实现可以通过JMS,ZK,甚至数据库表共享来实现.推荐使用ZK,但ZK对RemotePublish支持并不好.不适合线上业务,只适合一些数据同步和管理功能.
> @Publish,@Process是基于Local的,则不需一个第三方协作服务提供者.

3:协作服务发现
=====
> 通过Spring的机制PostProcessor,首先找到注解为@CooperationService的类,然后依次遍历方法,可以发现这些需要服务的方法和类
> 也能适合别的框架,如没有spring的情况

4:协作者
=====
分为发起方和接收方,通过申明annotation,可以实现协作.也可以直接在代码里调用协作服务API以满足发起方的灵活性要求.如一个方法体里需要俩次Publish到不同地方
```java
PublicService service = context.get("CooperationService-Pub");
service.send(path1, arglist1);
service.send(path2, arglist2);
```

5:应用场景说明
=====
> 1.多台机器上只有一台能执行某个job,则使用@RemoteSynronized 
> 2.主业务调用后会调用一些次要业务,不希望次要业务影响主业务的性能和牺牲可维护性 主业务使用@Publish,多个次业务使用@Subscribe.
> 3.数据需要同步到多台机器上,使用@RemotePublish和 @RemoteSubscriber标签
> 4.数据需要交给远程的任一台机器处理,使用@RemoteNotify然后结合@RemoteWait 标签一起用
> 5.主业务和次要业务处理后,还要求交给远端一个机器处理 可以在使用@Publish,@Subscrbie后,可以结合@RemoteNotify,@RemoteWait 来处理