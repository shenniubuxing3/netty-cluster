package nettyutils.client;

import com.alibaba.fastjson.JSON;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.apache.logging.log4j.util.Strings;
import org.springframework.util.CollectionUtils;
import zkutils.PollRqDto;
import zkutils.ZkHelper;
import zkutils.ZkRqDto;

import java.io.IOException;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
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
    public BootstrapHelper(String zkString, Consumer<ChannelPipeline> consumer, Consumer<ChannelFuture> channelFutureConsumer) throws Exception {
        this(zkString, "", consumer, 99, channelFutureConsumer);
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

        discoverIp(3, 10);
    }

    /**
     * netty启动
     */
    public void run() throws Exception {
        EventLoopGroup workGroup = new NioEventLoopGroup(this.workGroup);
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(workGroup).
                    channel(NioSocketChannel.class).
                    option(ChannelOption.SO_BACKLOG, 1024).
                    option(ChannelOption.SO_KEEPALIVE, true).
                    handler(new ChannelSocketHelper(this.consumer));
            ChannelFuture channelFuture = bootstrap.connect(this.ip, this.port).sync();
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
            //retry
            registerRetry();
        }
    }

    /**
     * client discover server Ip
     * @param loop
     * @param time
     * @throws Exception
     */
    void discoverIp(int loop, long time) throws Exception {
        if (Strings.isEmpty(this.zkRootNode)) {
            this.zkRootNode = "/netty_server";
        }
        if (Strings.isNotEmpty(this.zkString)) {
            Optional<PollRqDto> optional = this.selectServer(loop, time, TimeUnit.SECONDS);
            if (!optional.isPresent()) {
                throw new Exception("server is not exist");
            }
            ZkRqDto zkRqDto = JSON.parseObject(optional.get().getData().toString(), ZkRqDto.class);
            if (zkRqDto == null) {
                throw new Exception("client version is lower");
            }
            this.ip = zkRqDto.getIp();
            this.port = zkRqDto.getPort();
        }
    }

    /**
     * registerRetry 60 price
     *
     * @throws Exception
     */
    void registerRetry() throws Exception {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                System.out.println("init retry...");
                //discoverIp
                discoverIp(60, 30);
                //restart
                run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * discover zk server less load banlance info
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
     * discover server policy
     *
     * @param loop     retry num
     * @param time     time
     * @param timeUnit unit
     * @return
     */
    Optional<PollRqDto> selectServer(int loop, long time, TimeUnit timeUnit) throws IOException {
        Optional<PollRqDto> opt = Optional.empty();
        for (int i = 0; i < loop; i++) {
            opt = this.discoverServer();
            if (opt.isPresent()) {
                break;
            }
            try {
                System.out.println("discover Server ip ...");
                timeUnit.sleep(time);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return opt;
    }
}
