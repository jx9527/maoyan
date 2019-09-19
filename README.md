### red5_hls


1. red5_hls是对red5的1.1.1版本进行springboot整合，整合后直接使用springboot进行启动和管理。打jar包运行。目前通过实时推流测试。该项目主要是用来方便学习red5的源码。同时新增了http,rtp,rtsp,udp,hls等协议的支持。
2. 项目带有推送demo。
3. 在red5项目原有的基础上可以进行hls协议的离线和在线视频推流观看

### 启动


1. 首先运行SpringBootRed5Application启动red5服务器（Vm argments添加 -Dred5.root=路径）
2. 运行test下的GuangDongRTMP进行推流。
3. 使用VLC Mider player或其他软件播放 
4. 例如 rtmp://localhost:1935/oflaDemo/sssssss，http://127.0.0.1/oflaDemo/sssssss/playlist.m3u8，

### 感谢开源的道友

1. red5服务器源码[https://github.com/Red5](https://github.com/Red5)
2. 其他基于red5源码新增的协议 [https://github.com/pengliren/sms](https://github.com/pengliren/sms)
 

