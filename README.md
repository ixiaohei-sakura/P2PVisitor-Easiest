# P2PVisitor-Easiest
 \最简单的P2P联机工具/

## 声明

感谢 [fatedier](https://github.com/fatedier/) 提供的开源项目 [FRP](https://github.com/fatedier/frp), 我的项目只是在他的基础上包了个壳。

这个项目中使用的 frp 可执行文件不会是最新版。如果想使用最新版，请自行编译。

## 使用

在任何系统中使用，你只需要双击编译好的 `jar` 文件。程序将自动在 jar 文件的同级目录下生成 start.bat / start.sh (对于 Windows 系统 和 UNIX 系统)，然后启动 start.bat / start.sh 脚本文件。其他事宜请联系你的管理者。

## 对于管理者

打开 [MDS](https://github.com/ixiaohei-sakura/P2PVisitor-Easiest/tree/master/MetaDataServer) 的目录，在目录里编辑文件 `metaData.json`。这个文件的内容大概是这个样子

```json
{
  "example": {
    "sa": "x.x.x.x", 对应下面配置文件的 common 中的 server_addr
    "sp": "7000",  common 中的 server_port
    "sup": "7001", common 中的 server_udp_port
    "token": "123456", common 中的 token
    "name": "p2p_ssh_visitor", 对应下面第二个配置组的名称, 下面的例子是 p2p_ssh
    "type": "xtcp", 连接类型 自行了解，不多解释
    "role": "visitor", 略
    "sn": "p2p_ssh", 对应下面的 server_name
    "sk": "abcdefg" 无变化, sk
   }
}
```

```ini
# frpc.ini
[common]
server_addr = x.x.x.x
server_port = 7000
server_udp_port = 7001
token = 123456

[p2p_ssh_visitor]
type = xtcp
role = visitor
server_name = p2p_ssh
sk = abcdefg
bind_addr = 127.0.0.1
bind_port = 6000
```

客户端的请求过程是

C -> START

C -> S { status: x=1 }

S -> C { status: x+1 data: 上面的数据}

然后，客户端会以这个数据为配置文件，当返回的元数据中p2p服务端大于数量 >1 的时候, 客户端会让用户选择服务器。可以连接多个服务器，具体自行翻看源代码。
配置好后，启动 `metaDataServer.py` , 等待客户端连接。MDS端口配置在 `metaDataServer.py` 中。

注: 资源文件夹中的 `resources.zip` 在编译前需要被解压，我压缩它是因为里面有重复的二进制段。 

## P2P服务端

P2P服务端的配置自行查看  [FRP](https://github.com/fatedier/frp)。上面的配置要与 P2P 服务端的配置对应。

