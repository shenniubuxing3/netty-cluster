package nettyutils.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Created by Administrator on 2019/6/29.
 */
public class ChannelSocketHelper extends ChannelInitializer<SocketChannel> {
    private Consumer<ChannelPipeline> consumer;

    public ChannelSocketHelper(Consumer<ChannelPipeline> consumer) {
        this.consumer = consumer;
    }

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        ChannelPipeline channelPipeline = socketChannel.pipeline()
                .addLast("idleStateHandler", new IdleStateHandler(0, 2, 0, TimeUnit.MINUTES));
        consumer.accept(channelPipeline);
    }
}
