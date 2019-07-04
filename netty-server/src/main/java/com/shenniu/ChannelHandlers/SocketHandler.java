package com.shenniu.ChannelHandlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import nettyutils.extend.DataExtend;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Created by Administrator on 2019/6/27.
 */
public class SocketHandler extends SimpleChannelInboundHandler {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
        String content = String.format("服务器SocketHandler收到客户端[%s]消息：%s",
                channelHandlerContext.channel().remoteAddress(),
                DataExtend.getContentByBb((ByteBuf) o));
        System.out.println(content);
        // channelHandlerContext.writeAndFlush(Unpooled.copiedBuffer(content, CharsetUtil.UTF_8));
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        cause.printStackTrace();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.channel().writeAndFlush(
                DataExtend.getBbByString("[服务器信息]：时间 - " +
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
    }
}
