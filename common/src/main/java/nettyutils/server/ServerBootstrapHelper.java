package nettyutils.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.logging.log4j.util.Strings;
import org.apache.zookeeper.Watcher;
import zkutils.PollServerDto;
import zkutils.ZkHelper;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Created by shenniu003 on 2019/6/27.
 */
public class ServerBootstrapHelper {
    private String ip;
    private int port;
    private String webSocketPath;
    private Consumer<ChannelPipeline> consumer;

    private Consumer<ChannelFuture> channelFutureConsumer;
    private int bossGroup;
    private int workGroup;

    private String zkString;
    private String zkRootNode;

    /**
     * socket启动zk注册功能
     *
     * @param port
     * @param consumer
     * @param zkString
     */
    public ServerBootstrapHelper(int port, Consumer<ChannelPipeline> consumer, String zkString) {
        this(port, consumer, 1, 99, b -> {
        }, zkString, "");
    }

    public ServerBootstrapHelper(int port, Consumer<ChannelPipeline> consumer, int bossGroup, int workGroup, Consumer<ChannelFuture> channelFutureConsumer, String zkString, String zkRootNode) {
        this("", port, "", consumer, bossGroup, workGroup, channelFutureConsumer, zkString, zkRootNode);
    }

    /**
     * webSocket启动zk注册功能
     *
     * @param port
     * @param webSocketPath
     * @param consumer
     * @param zkString
     */
    public ServerBootstrapHelper(int port, String webSocketPath, Consumer<ChannelPipeline> consumer, String zkString) {
        this(port, webSocketPath, consumer, zkString, "");
    }

    public ServerBootstrapHelper(int port, String webSocketPath, Consumer<ChannelPipeline> consumer, String zkString, String zkRootNode) {
        this("", port, webSocketPath, consumer, 1, 99, channelFuture -> {
        }, zkString, zkRootNode);
    }

    public ServerBootstrapHelper(String ip, int port, String webSocketPath, Consumer<ChannelPipeline> consumer,
                                 int bossGroup, int workGroup, Consumer<ChannelFuture> channelFutureConsumer,
                                 String zkString, String zkRootNode) {
        this.ip = ip;
        this.port = port;
        this.webSocketPath = webSocketPath;
        this.consumer = consumer;
        this.bossGroup = bossGroup;
        this.workGroup = workGroup;
        this.channelFutureConsumer = channelFutureConsumer;
        this.zkString = zkString;
        this.zkRootNode = zkRootNode;

        if (Strings.isEmpty(this.ip)) {
            this.ip = "127.0.0.1";
        }
        if (Strings.isEmpty(this.zkRootNode)) {
            this.zkRootNode = "/netty_server";
        }
    }

    /**
     * netty启动
     */
    public void run() {
        EventLoopGroup bossGroup = new NioEventLoopGroup(this.bossGroup);
        EventLoopGroup workGroup = new NioEventLoopGroup(this.workGroup);
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workGroup).channel(NioServerSocketChannel.class);
            if (Strings.isNotEmpty(this.webSocketPath)) {
                //webSocket
                bootstrap.childHandler(new ChannelWebSocketHelper(this.consumer, this.webSocketPath));
            } else {
                //socket
                bootstrap.childHandler(new ChannelSocketHelper(this.consumer));
            }
            bootstrap.option(ChannelOption.SO_BACKLOG, 1024).childOption(ChannelOption.SO_KEEPALIVE, true);
            ChannelFuture channelFuture = bootstrap.bind(this.port).sync();

            //order consumer
            if (Objects.nonNull(this.channelFutureConsumer)) {
                this.channelFutureConsumer.accept(channelFuture);
            }
            //add linstenr
            this.addListener(channelFuture);

            channelFuture.channel().closeFuture().sync();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            workGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
            System.out.println(String.format("server %s:%s is end", this.ip, this.port));
        }
    }

    /**
     * 添加事件监听
     *
     * @param channelFuture
     */
    void addListener(ChannelFuture channelFuture) {
        channelFuture.addListener(future -> {
            //register zk
            this.registerZk(channelFuture);
        });
    }

    /**
     * 启动zk注册功能
     */
    void registerZk(ChannelFuture channelFuture) {
        if (Strings.isEmpty(this.zkString) || Strings.isEmpty(this.zkRootNode)) {
            return;
        }
        boolean isClose = false;
        try {
            ZkHelper zkHelper = new ZkHelper(this.zkString, b -> {
                System.out.println("zk status is " + b.getState());
            });
            //create rootnode
            String rootPath = zkHelper.createPersistentFolder(this.zkRootNode, false);

            //create node = register ip+port
            StringBuilder node = new StringBuilder().append(this.ip).append(":").append(this.port);
            PollServerDto serverDto = new PollServerDto();
            serverDto.setIp(this.ip);
            serverDto.setPort(this.port);
            String nodePath = zkHelper.createPoll(rootPath + "/" + node, serverDto);
            if (Strings.isEmpty(nodePath)) {
                isClose = true;
                return;
            }
            System.out.println(String.format("server %s:%s is start", this.ip, this.port));
            System.out.println("server is register zk nodePath：" + nodePath);
            //listener event
            zkHelper.getData(nodePath.toString(), b -> {
                if (b.getType() == Watcher.Event.EventType.NodeDeleted) {
                    System.out.println("zk nodePath：" + nodePath + " is " + b.getType());
                    channelFuture.channel().close();
                }
            });
        } catch (Exception ex) {
            isClose = true;
            ex.printStackTrace();
        } finally {
            if (isClose) {
                channelFuture.channel().close();
            }
        }
    }
}
