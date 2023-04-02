# Nearby-Share📦

近场环境下共同群组安全通信移动应用

![20221008_2022软件课程设计_v2.3_output_11.png](https://s2.loli.net/2022/12/25/InsBmEPXbFY7KCg.png)

## 内容列表

- [功能](#功能)
- [安装](#安装)
- [使用说明](#使用说明)
- [存在的问题](#存在的问题)
- [如何贡献](#如何贡献)
- [使用许可](#使用许可)

## 功能

本安卓app实现了以下功能：

+ 文件传输：在近场环境下利用[WLAN 直连](https://www.wi-fi.org/discover-wi-fi/wi-fi-direct) (P2P) 技术进行设备间文件传输，传输速度快且无需连接外部网络，支持一对多传输。
+ 共同群组认证：识别周围环境中与您处在同一群组中的设备，不是同一群组的设备不进行文件传输。
+ 传输数据加密：使用[AES](https://www.nist.gov/publications/advanced-encryption-standard-aes)加密算法对传输文件进行加密。
+ 群组信息保护：使用[MD5](https://en.wikipedia.org/wiki/MD5)哈希算法对群组号信息进行加密，群组认证过程不泄露设备群组号。

## 安装

[下载链接](https://github.com/Robert-zwr/Nearby-Share/releases/download/v0.1.1/NearbyShare-v011.apk)

下载后在安卓设备上打开该apk文件即可进行安装（需要设备API级别14+）。

## 使用说明

本app使用步骤如下：

1. 点击”检查权限“按钮，检查设备是否支持WLAN直连以及应用所需权限是否获取到。
2. 在输入框内输入您的群组号，并点击”确定“按钮进行确认。
3. 文件发送方点击右上角菜单栏中的”创建群组“按钮，以群主身份进行群组创建。
4. 文件接收方点击右上角菜单栏中的”搜索附近设备“按钮，在设备列表中找到并选中群主设备后，点击”连接“按钮完成群组加入。
5. 若文件接收方不止一个，其他文件接收方重复步骤4，完成群组加入。
6. 文件发送方（群主）点击”选择文件“按钮，在弹出的文件选择界面选中要共享的文件。
7. 群组中与群主的群组号相同的所有设备即可接收到群主共享来的文件，群组号不同的设备则无法接收到文件。

## 存在的问题

在测试过程中发现该app还存在如下问题：

+ 运行一次只能传输一个文件，若想传输更多文件需要重新启动app。
+ 由于测试数量有限，部分图片/视频之外的文件类型可能仍存在找不到文件后缀名的情况。
+ 在使用安卓10系统的设备上运行本应用时，即使用户在运行时授予了位置权限，应用程序也不会运行，需要在”设置“中手动开启定位权限，[详见此处](https://stackoverflow.com/questions/65018782/wifip2pmanager-discoverpeers-fails-in-android-10-despite-of-using-runtime-perm) 。

## 如何贡献

本仓库为华中科技大学电子信息与通信学院20级软件课程设计项目，仓库暂时为私有状态，预计结课后转为公开状态。

非常欢迎你的加入！[提一个 Issue](https://github.com/Robert-zwr/Nearby-Share/issues/new/choose) 或者提交一个 Pull Request。


本项目遵循 [Contributor Covenant](http://contributor-covenant.org/version/1/3/0/) 行为规范。

## 使用许可

[MIT](LICENSE) © Robert-zwr
