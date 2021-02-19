import socket
import json
import threading
import Logger
import queue


class ClientTask(object):
    def __init__(self, clientSocket, clientAddr):
        self.clientAddr = clientAddr
        self.clientSocket = clientSocket


class ClientThread(threading.Thread):
    def __init__(self, count, parentLogger):
        super(ClientThread, self).__init__()
        self.logger = parentLogger
        self.name = "ClientThread Thread #" + str(count)
        self.daemon = True
        self.tasks = queue.Queue(10)
        self.interrupt = False

    def addTask(self, task):
        self.tasks.put(task)

    def stop(self):
        self.interrupt = True

    def handler(self, client):
        clientAddr = client.clientAddr
        clientSocket = client.clientSocket
        self.logger.info("客户端已连接，地址: " + clientAddr.__str__())
        self.logger.info("等待数据发送")
        buff = clientSocket.recv(256)
        rd = json.loads(buff)
        self.logger.info("客户端请求数据:\n" + json.dumps(rd, indent=3))
        self.logger.info("收到客户端数据，状态值: " + str(rd['status']))
        if int(rd['status']) == 1:
            rd['data'] = json.loads(open("./metaData.json", 'r').read())
            rd['status'] = int(rd['status']) + 1
            self.logger.info("发送数据:\n" + json.dumps(rd, indent=3))
            clientSocket.send(json.dumps(rd).encode("utf-8"))
            self.logger.info("数据已发送")
            clientSocket.close()
        else:
            self.logger.info("状态值错误, 断开连接")
            clientSocket.shutdown(2)
            clientSocket.close()

    def run(self):
        while not self.interrupt:
            while len(self.tasks.queue):
                self.handler(self.tasks.get())


class Main:
    def __init__(self, addr, port, parentLogger, mt=5):
        self.logger = parentLogger
        self.maximaThreads = mt
        self.socket = socket.socket()
        self.socket.bind((addr, port))
        self.socket.listen(5)
        self.logger.info("监听地址 {}:{}".format(addr, port))
        self.count = 0
        self.threadPool = []

    def creatThreads(self):
        for i in range(self.maximaThreads+1):
            thread = ClientThread(self.count, self.logger)
            thread.start()
            self.threadPool.append(thread)
            self.count += 1
        self.count = 0

    def waitForConnection(self):
        while True:
            for thread in self.threadPool:
                try:
                    c, a = self.socket.accept()
                    thread.addTask(ClientTask(c, a))
                    self.logger.info("获取到一个新连接, 客户端处理任务已添加")
                    self.logger.info("处理的连接数: " + str(self.count))
                    self.count += 1
                except KeyboardInterrupt:
                    self.socket.close()
                    for t in self.threadPool:
                        t.stop()
                        t.join()
                    exit(0)

    def main(self):
        self.creatThreads()
        self.waitForConnection()


if __name__ == '__main__':
    logger = Logger.Logger(name="MDS", sub_name="PyV")
    logger.info("初始化中")
    m = Main("0.0.0.0", 9000, logger)
    logger.info("服务器已启动，等待客户端连接")
    m.main()
