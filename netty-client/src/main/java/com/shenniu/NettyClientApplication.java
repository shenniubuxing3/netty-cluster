package com.shenniu;

import com.shenniu.ChannelHandlers.SocketHandler;
import nettyutils.client.BootstrapHelper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

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
        BootstrapHelper helper = new BootstrapHelper("192.168.181.19:2181", b -> {
            b.addLast(new SocketHandler());
        });
        helper.run();
    }
}
