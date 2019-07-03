package nettyutils.client;

import com.alibaba.fastjson.JSON;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.logging.log4j.util.Strings;
import org.springframework.util.CollectionUtils;
import zkutils.PollRqDto;
import zkutils.PollServerDto;
import zkutils.ZkHelper;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Created by Administrator on 2019/6/27.
 */
public class BootstrapHelper {
    private String ip;
    private int port;
    private Consumer<ChannelPipeline> consumer;

    private Consumer<ChannelFuture> channelFutureConsumer;
    private int workGroup;

    private String zkString;
    private String zkRootNode;

    /**
     * 手动绑定ip
     *
     * @param ip
     * @param port
     * @param consumer
     */
    public BootstrapHelper(String ip, int port, Consumer<ChannelPipeline> consumer) throws Exception {
        this(ip, port, consumer, "", "");
    }

    /**
     * 自动发现ip
     *
     * @param zkString
     * @param consumer
     */
    public BootstrapHelper(String zkString, Consumer<ChannelPipeline> consumer) throws Exception {
        this(zkString, "", consumer);
    }

    public BootstrapHelper(String zkString, String zkRootNode, Consumer<ChannelPipeline> consumer) throws Exception {
        this(zkString, zkRootNode, consumer, 99, b -> {
        });
    }

    public BootstrapHelper(String ip, int port, Consumer<ChannelPipeline> consumer, String zkString, String zkRootNode) throws Exception {
        this(ip, port, consumer, 99, b -> {
        }, zkString, zkRootNode);
    }

    public BootstrapHelper(String zkString, String zkRootNode, Consumer<ChannelPipeline> consumer,
                           int workGroup, Consumer<ChannelFuture> channelFutureConsumer) throws Exception {
        this("", 0, consumer, workGroup, channelFutureConsumer, zkString, zkRootNode);
    }

    public BootstrapHelper(String ip, int port, Consumer<ChannelPipeline> consumer,
                           int workGroup, Consumer<ChannelFuture> channelFutureConsumer,
                           String zkString, String zkRootNode) throws Exception {
        this.ip = ip;
        this.port = port;
        this.consumer = consumer;
        this.workGroup = workGroup;
        this.channelFutureConsumer = channelFutureConsumer;
        this.zkString = zkString;
        this.zkRootNode = zkRootNode;

        if (Strings.isEmpty(this.zkRootNode)) {
            this.zkRootNode = "/netty_server";
        }

        if (Strings.isNotEmpty(this.zkString)) {
            Optional<PollRqDto> optional = this.selectServer(3, 10, TimeUnit.SECONDS);
            if (!optional.isPresent()) {
                throw new Exception("server is not exist");
            }
            PollServerDto serverDto = JSON.parseObject(optional.get().getData().toString(), PollServerDto.class);
            if (serverDto == null) {
                throw new Exception("server info is error");
            }
            this.ip = serverDto.getIp();
            this.port = serverDto.getPort();
        }
    }

    /**
     * netty启动
     */
    public void run() {
        EventLoopGroup workGroup = new NioEventLoopGroup(this.workGroup);
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(workGroup).
                    channel(NioServerSocketChannel.class).
                    handler(new ChannelSocketHelper(this.consumer)).
                    option(ChannelOption.SO_BACKLOG, 1024).
                    option(ChannelOption.SO_KEEPALIVE, true);
            ChannelFuture channelFuture = bootstrap.connect("192.168.181.19", this.port).sync();

            //order consumer
            if (Objects.nonNull(this.channelFutureConsumer)) {
                this.channelFutureConsumer.accept(channelFuture);
            }
            channelFuture.channel().closeFuture().sync();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            workGroup.shutdownGracefully();
            System.out.println(String.format("client connect %s:%s is end", this.ip, this.port));
        }
    }

    /**
     * 发现zk服务上的最少统计数的server信息
     */
    Optional<PollRqDto> discoverServer() {
        if (Strings.isEmpty(this.zkString) || Strings.isEmpty(this.zkRootNode)) {
            return Optional.empty();
        }
        try {
            ZkHelper zkHelper = new ZkHelper(this.zkString, b -> {
                System.out.println("zk status is " + b.getState());
            });
            Map<String, PollRqDto> servers = zkHelper.getChildData(this.zkRootNode, PollRqDto.class, null);
            if (CollectionUtils.isEmpty(servers)) {
                return Optional.empty();
            }
            return servers.values().stream().min(Comparator.comparing(PollRqDto::getPoolNum));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    /**
     * 发现server信息策略
     *
     * @param loop     重试次数
     * @param time     间隔时间
     * @param timeUnit 间隔时间单位
     * @return
     */
    Optional<PollRqDto> selectServer(int loop, long time, TimeUnit timeUnit) {
        Optional<PollRqDto> opt = Optional.empty();
        for (int i = 0; i < loop; i++) {
            opt = this.discoverServer();
            if (opt.isPresent()) {
                break;
            }
            try {
                timeUnit.sleep(time);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return opt;
    }
}
