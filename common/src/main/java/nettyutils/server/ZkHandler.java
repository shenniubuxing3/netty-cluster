package nettyutils.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.util.Strings;
import org.apache.zookeeper.KeeperException;
import zkutils.PollRqDto;
import zkutils.ZkHelper;
import zkutils.ZkRqDto;

import java.io.IOException;

/**
 * Created by Administrator on 2019/7/9.
 */
public class ZkHandler extends ChannelInboundHandlerAdapter {
    private ZkRqDto zkRqDto;

    public ZkHandler(ZkRqDto zkRqDto) throws InterruptedException, IOException, KeeperException {
        this.zkRqDto = zkRqDto;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        System.out.println(ctx.channel().remoteAddress() + "连接成功，记录数+1");
        this.poolNumIncr(1);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        System.out.println(ctx.channel().remoteAddress() + "断开连接，记录数-1");
        this.poolNumIncr(-1);
    }

    /**
     * zk load banlance num set + -
     */
    synchronized void poolNumIncr(int num) throws IOException {
        if (this.zkRqDto == null || Strings.isEmpty(this.zkRqDto.getZkString()) || Strings.isEmpty(this.zkRqDto.getZkRootNode())) {
            return;
        }
        ZkHelper zkHelper = null;
        try {
            zkHelper = new ZkHelper(this.zkRqDto.getZkString(), b -> {
                System.out.println("zk status is " + b.getState());
            });

            StringBuilder node = new StringBuilder().append(this.zkRqDto.getIp()).append(":").append(this.zkRqDto.getPort());
            String path = this.zkRqDto.getZkRootNode() + "/" + node;
            PollRqDto pollRqDto = zkHelper.getPoll(path, null, PollRqDto.class);
            if (pollRqDto == null || pollRqDto.getPoolNum() <= 0 && num <= -1) {
                return;
            }
            pollRqDto.setPoolNum(pollRqDto.getPoolNum() + num);
            zkHelper.setPollChild(path, pollRqDto);
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (zkHelper != null) {
                zkHelper.close();
            }
        }
    }
}
