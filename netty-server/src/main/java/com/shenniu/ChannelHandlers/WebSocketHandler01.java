package com.shenniu.ChannelHandlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

/**
 * Created by Administrator on 2019/6/27.
 */
public class WebSocketHandler01 extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    public WebSocketHandler01(){
//        super(false);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, TextWebSocketFrame textWebSocketFrame) throws Exception {
        String content = "服务器WebSocketHandler01收到消息：" + textWebSocketFrame.text();
        System.out.println(content);
        channelHandlerContext.writeAndFlush(new TextWebSocketFrame(content));
//        channelHandlerContext.fireChannelRead(new TextWebSocketFrame(content));
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
