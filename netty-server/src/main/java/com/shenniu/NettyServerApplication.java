package com.shenniu;

import com.shenniu.ChannelHandlers.SocketHandler;
import com.shenniu.ChannelHandlers.WebSocketHandler;
import com.shenniu.ChannelHandlers.WebSocketHandler01;
import configutils.ConfigHelper;
import nettyutils.server.ServerBootstrapHelper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootApplication
public class NettyServerApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(NettyServerApplication.class, args);
    }

    @Override
    public void run(String... strings) throws Exception {
//        test0();
//        test1();
        test2();
    }

    void test2() {
        Properties conf = ConfigHelper.getHelper().load();
        for (int i = 0; i <= 5; i++) {
            int finalI = i;
            new Thread(() -> {
                ServerBootstrapHelper helper = new ServerBootstrapHelper(
                        1980 + finalI,
                        pip -> pip.addLast(new SocketHandler()),
                        conf.getProperty("shenniu003.zk.link"));
                helper.run();
            }).start();
        }
    }

    void test0() {
        ExecutorService executorService = Executors.newFixedThreadPool(3);

        executorService.execute(() -> {
            ServerBootstrapHelper helper = new ServerBootstrapHelper(
                    1990, "shenniu_ws",
                    pip -> pip.addLast(new WebSocketHandler()),
                    "192.168.181.19:2181", "/netty_server_websocket");
            helper.run();
        });

        executorService.execute(() -> {
            ServerBootstrapHelper helper1 = new ServerBootstrapHelper(
                    1991, "shenniu_ws",
                    pip -> pip.addLast(new WebSocketHandler01()),
                    "192.168.181.19:2181", "/netty_server_websocket");
            helper1.run();
        });
    }

    void test1() {

//        System.out.println(NettyServerApplication.class + " is running");

//        ServerBootstrapHelper helper = new ServerBootstrapHelper(
//                1991, "shenniu_ws",
//                pip -> {
//                    pip.addLast(new WebSocketHandler())
//                            .addLast(new WebSocketHandler01())
//                            .addLast(new WebSocketHandler02());
//                },
//                channelFuture -> {
//                    channelFuture.addListener(future -> {
//
//                    });
//                });


//        ServerBootstrapHelper helper4 = new ServerBootstrapHelper(
//                1994, "shenniu_ws",
//                pip -> {
//                    pip.addLast(new WebSocketHandler());
//                    pip.addLast(new WebSocketHandler01());
//                    pip.addLast(new WebSocketHandler02());
//                },
//                "192.168.181.19:2181");
//        helper4.run();


//        ZkHelper zkHandler = new ZkHelper("192.168.181.19:2181", b -> {
//            System.out.println(b.getPath() + "---" + b.getState());
//        });
//        String rootPath = "/netty_server";
//        System.out.println(zkHandler.createPersistentFolder(rootPath, false));
//
//        System.out.println(zkHandler.createPoll(rootPath + "/192.101.10.1", "阿里"));
//        System.out.println(zkHandler.createPoll(rootPath + "/192.101.10.2", "腾讯"));
//        System.out.println(zkHandler.createPoll(rootPath + "/192.101.10.3", "百度"));

    }
}
