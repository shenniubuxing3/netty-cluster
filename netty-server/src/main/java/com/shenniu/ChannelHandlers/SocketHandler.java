package com.shenniu.ChannelHandlers;

import com.shenniu.NettyServerApplication;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import nettyutils.extend.DataExtend;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Created by Administrator on 2019/6/27.
 */
public class SocketHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
        String content = String.format("服务器SocketHandler收到客户端[%s]消息：%s",
                channelHandlerContext.channel().remoteAddress(), DataExtend.getContentByBb((ByteBuf) o));
        System.out.println(content);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        //加入全局channel
        NettyServerApplication.channelGroup.add(ctx.channel());
        ctx.channel().writeAndFlush(DataExtend.getBbByString(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + " =》 连接到服务器 -- " + ctx.channel().localAddress()));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        NettyServerApplication.channelGroup.remove(ctx.channel());
    }
}
