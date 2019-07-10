package nettyutils.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.apache.logging.log4j.util.Strings;
import org.apache.zookeeper.Watcher;
import zkutils.ZkHelper;
import zkutils.ZkRqDto;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Created by Administrator on 2019/6/29.
 */
public class ChannelSocketHelper extends ChannelInitializer<SocketChannel> {
    private Consumer<ChannelPipeline> consumer;
    private ZkRqDto zkRqDto;

    public ChannelSocketHelper(Consumer<ChannelPipeline> consumer, ZkRqDto zkRqDto) throws Exception {
        this.consumer = consumer;
        this.zkRqDto = zkRqDto;

        if (this.zkRqDto == null || Strings.isEmpty(this.zkRqDto.getZkString()) || Strings.isEmpty(this.zkRqDto.getZkRootNode())) {
            return;
        }
        //start register zk
        registerZk();
    }

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        ChannelPipeline channelPipeline = socketChannel.pipeline()
                .addLast("idleStateHandler", new IdleStateHandler(0, 2, 0, TimeUnit.MINUTES))
                .addLast("zkHandler", new ZkHandler(this.zkRqDto));
        consumer.accept(channelPipeline);
    }

    /**
     * start register zk
     */
    void registerZk() throws Exception {
        ZkHelper zkHelper = new ZkHelper(this.zkRqDto.getZkString(), b -> {
        });
        //create rootnode
        String rootPath = zkHelper.createPersistentFolder(this.zkRqDto.getZkRootNode(), false);
        //create node = register ip+port
        StringBuilder node = new StringBuilder().append(this.zkRqDto.getIp()).append(":").append(this.zkRqDto.getPort());
        String nodePath = zkHelper.createPoll(rootPath + "/" + node, this.zkRqDto);
        if (Strings.isEmpty(nodePath)) {
            throw new Exception("server is register zk nodePath：" + nodePath + " fail");
        }
        System.out.println(String.format("server %s is start", node));
        System.out.println("server is register zk nodePath：" + nodePath);
        //listener event
        zkHelper.getData(nodePath, b -> {
            if (b.getType() == Watcher.Event.EventType.NodeDeleted) {
                System.out.println("zk nodePath：" + nodePath + " is " + b.getType());
            }
        });
    }
}
