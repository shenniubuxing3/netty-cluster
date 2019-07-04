package com.shenniu.ChannelHandlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import nettyutils.extend.DataExtend;

/**
 * Created by Administrator on 2019/6/27.
 */
public class SocketHandler extends SimpleChannelInboundHandler {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
        String content = "客户端SocketHandler收到消息：" + DataExtend.getContentByBb((ByteBuf) o);
        System.out.println(content);
        channelHandlerContext.writeAndFlush(DataExtend.getBbByString(content));
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
}
