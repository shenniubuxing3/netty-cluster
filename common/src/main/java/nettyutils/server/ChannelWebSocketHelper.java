package nettyutils.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import org.springframework.util.Assert;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Created by Administrator on 2019/6/27.
 */
public class ChannelWebSocketHelper extends ChannelInitializer<SocketChannel> {
    private Consumer<ChannelPipeline> consumer;
    private String webSocketPath;

    public ChannelWebSocketHelper(Consumer<ChannelPipeline> consumer, String webSocketPath) {
        this.consumer = consumer;
        this.webSocketPath = webSocketPath;
    }

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        Assert.notNull(webSocketPath, "webSocket路径不能为空");
        this.webSocketPath = this.webSocketPath.startsWith("/") ? this.webSocketPath : "/" + this.webSocketPath;

        ChannelPipeline channelPipeline = socketChannel.pipeline()
                .addLast("httpServerCodec", new HttpServerCodec())
                .addLast("chunkedWriteHandler", new ChunkedWriteHandler())
                .addLast("httpObjectAggregator", new HttpObjectAggregator(8192))
                .addLast("idleStateHandler", new IdleStateHandler(3, 0, 0, TimeUnit.MINUTES))
                .addLast("webSocketServerProtocolHandler", new WebSocketServerProtocolHandler(this.webSocketPath));
        consumer.accept(channelPipeline);
    }
}