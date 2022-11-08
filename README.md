### 介绍

本项目基于[推流端](https://github.com/pedroSG94/rtmp-rtsp-stream-client-java)抽取出来，去掉camera部分，推流数据为一组图片。

### 本项目使用到服务器
服务端：https://github.com/EasyDarwin/EasyDarwin

[直接下载服务端](https://github.com/buhuiming/RtspDemo/blob/main/EasyDarwin-windows-8.1.0-1901141151.zip)
，解压后打开EasyDarwin，ServiceInstall-EasyDarwin，命令窗显示服务器rtsp链接(如rtsp://10.82.144.10：554)，浏览器打开
http://10.82.144.10:10008 可以看到管理界面，注意要关闭电脑网络防火墙。

播放器(拉流)：VLC，[直接下载安装](https://github.com/buhuiming/RtspDemo/blob/main/VLCMediaPlayer3.0.17.4.rar)，点击左上角
媒体-打开网络串流-输入(如rtsp://10.82.144.70:554/test)

![image](https://user-images.githubusercontent.com/30099293/200507168-54573220-1def-472a-b475-e445a5e8a893.png)

![image](https://user-images.githubusercontent.com/30099293/200507252-e8693353-a11d-460c-b107-3d54e6bb3f98.png)

![image](https://user-images.githubusercontent.com/30099293/200507658-52f4eb6b-a972-4568-b90f-8e8ebd75dff4.png)
