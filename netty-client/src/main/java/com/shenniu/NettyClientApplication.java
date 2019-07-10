package com.shenniu;

import com.shenniu.ChannelHandlers.SocketHandler;
import configutils.ConfigHelper;
import nettyutils.client.BootstrapHelper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Properties;

@SpringBootApplication
public class NettyClientApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(NettyClientApplication.class, args);
    }

    @Override
    public void run(String... strings) throws Exception {
        test01();
    }

    void test01() throws Exception {
        Properties conf = ConfigHelper.getHelper().load();
        BootstrapHelper helper = new BootstrapHelper(
                conf.getProperty("shenniu003.zk.link"),
                pipeline -> {
                    pipeline.addLast(new SocketHandler());
                },
                channelFuture -> {

                });
        helper.run();
    }
}
