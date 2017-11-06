# alloc
mpush allocator demo

### 服务用途

> * alloc 是针对client提供的一个轻量级的负载均衡服务
> * 每次客户端在链接MPUSH server之前都要调用下该服务
> * 以获取可用的MPUSH server列表,然后按顺序去尝试建立TCP链接,直到链接建立成功

### 对外提供的接口定义

> 接口类型     ：HTTP
>
> Method       : GET
>
> 参数         ：无
>
> 返回值格式   : ip:port,ip:port
>
> content-type : text/plain;charset=utf-8 

### 打包部署
alloc打包方法
进入master目录 运行命令：mvn install（或者将mpush-client-0.8.0.jar手动放入alloc的jar文件夹下）
进入alloc目录，运行命令
mvn clean package -Pzip,pub


