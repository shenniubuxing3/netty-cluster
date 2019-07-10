package zkutils;

import com.alibaba.fastjson.JSON;
import org.apache.logging.log4j.util.Strings;
import org.apache.zookeeper.*;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

/**
 * Created by Administrator on 2019/6/27.
 */
public class ZkHelper implements Closeable {

    private ZooKeeper zooKeeper;
    private String connectString;
    private int timeOut;
    private Watcher watcher;

    public ZkHelper(String connectString, Watcher watcher) throws IOException {
        this(connectString, 1000 * 60, watcher);
    }

    public ZkHelper(String connectString, int timeOut, Watcher watcher) throws IOException {
        this.connectString = connectString;
        this.timeOut = timeOut;
        this.watcher = watcher;

        init();
    }

    public void init() throws IOException {
        this.zooKeeper = new ZooKeeper(this.connectString, this.timeOut, this.watcher);
    }

    public String create(String path, byte[] bb, CreateMode createMode) throws KeeperException, InterruptedException {
        if (!createMode.isSequential() && this.exists(path)) {
            return Strings.EMPTY;
        }
        return this.zooKeeper.create(path, bb, ZooDefs.Ids.OPEN_ACL_UNSAFE, createMode);
    }

    public String create(String path, String content, CreateMode createMode) throws KeeperException, InterruptedException {
        return this.create(path, content.getBytes(StandardCharsets.UTF_8), createMode);
    }

    /**
     * 创建负载节点 文本格式方便查看
     *
     * @param path
     * @param t
     * @param <T>
     * @return
     * @throws KeeperException
     * @throws InterruptedException
     */
    public <T> String createPoll(String path, T t) throws KeeperException, InterruptedException {
        PollRqDto pollRqDto = new PollRqDto();
        pollRqDto.setPoolNum(0L);
        pollRqDto.setData(t);
        pollRqDto.setNodePath(path);
        return this.create(path,
                JSON.toJSONString(pollRqDto),
                CreateMode.EPHEMERAL);
    }

    public String createPersistentFolder(String path, boolean isSeq) throws KeeperException, InterruptedException {
        return this.createPersistent(path, "", isSeq);
    }

    /**
     * 创建零时节点
     *
     * @param path
     * @param content
     * @param isSeq   true：有序 false：无序
     * @return
     * @throws KeeperException
     * @throws InterruptedException
     */
    public String createPersistent(String path, String content, boolean isSeq) throws KeeperException, InterruptedException {
        if (!isSeq && this.exists(path)) {
            return path;
        }
        return this.zooKeeper.create(
                path, content.getBytes(),
                ZooDefs.Ids.OPEN_ACL_UNSAFE,
                isSeq ? CreateMode.PERSISTENT_SEQUENTIAL : CreateMode.PERSISTENT);
    }

    /**
     * 创建零时节点
     *
     * @param path
     * @param content
     * @param isSeq   true：有序 false：无序
     * @return
     * @throws KeeperException
     * @throws InterruptedException
     */
    public String createEphemeral(String path, String content, boolean isSeq) throws KeeperException, InterruptedException {
        if (!isSeq && this.exists(path)) {
            return Strings.EMPTY;
        }
        return this.zooKeeper.create(
                path, content.getBytes(StandardCharsets.UTF_8),
                ZooDefs.Ids.OPEN_ACL_UNSAFE,
                isSeq ? CreateMode.EPHEMERAL_SEQUENTIAL : CreateMode.EPHEMERAL);
    }

    public boolean exists(String path) throws KeeperException, InterruptedException {
        return this.zooKeeper.exists(path, false) != null;
    }

    private <T> T getChild(String rootPath, Function<List<String>, T> function) throws KeeperException, InterruptedException {
        Assert.notEmpty(Arrays.asList(rootPath), "rootPath不为空");
        List<String> list = this.zooKeeper.getChildren(rootPath, true);
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        return function.apply(list);
    }

    /**
     * 获取有序节点最小节点
     *
     * @param rootPath 根目录
     * @param nodeName 有序节点名称前缀
     * @return
     * @throws KeeperException
     * @throws InterruptedException
     */
    public String getMinChild(String rootPath, String nodeName) throws KeeperException, InterruptedException {
        Assert.notEmpty(Arrays.asList(rootPath, nodeName), "rootPath或nodeName不为空");
        int prexIndex = nodeName.length();
        return this.getChild(rootPath, b -> {
            Optional<String> optional = b.stream().min(Comparator.comparing(a -> Long.valueOf(a.substring(prexIndex))));
            return optional.isPresent() ? optional.get() : Strings.EMPTY;
        });
    }

    /**
     * 获取有序节点最大节点
     *
     * @param rootPath 根目录
     * @param nodeName 有序节点名称前缀
     * @return
     * @throws KeeperException
     * @throws InterruptedException
     */
    public String getMaxChild(String rootPath, String nodeName) throws KeeperException, InterruptedException {
        Assert.notEmpty(Arrays.asList(rootPath, nodeName), "rootPath或nodeName不为空");
        int prexIndex = nodeName.length();
        return this.getChild(rootPath, b -> {
            return b.stream().
                    max(Comparator.comparing(a -> Long.valueOf(a.substring(prexIndex)))).
                    orElse(Strings.EMPTY);
        });
    }

    /**
     * 获取目录下子项内容 文本内容
     *
     * @param rootPath
     * @return
     * @throws KeeperException
     * @throws InterruptedException
     */
    public Map<String, String> getChildData(String rootPath, Watcher watcher) throws KeeperException, InterruptedException {
        return this.getChild(rootPath, nodes -> {
            Map<String, String> nodeDatas = new HashMap<>();
            nodes.parallelStream().forEach(node -> {
                byte[] bb = this.getChildData(rootPath, node, watcher);
                nodeDatas.put(node, new String(bb, StandardCharsets.UTF_8));
            });
            return nodeDatas;
        });
    }

    /**
     * 获取目录下子项内容 对象内容
     *
     * @param rootPath
     * @param tClass
     * @param <T>
     * @return
     * @throws KeeperException
     * @throws InterruptedException
     */
    public <T> Map<String, T> getChildData(String rootPath, Class<T> tClass, Watcher watcher) throws KeeperException, InterruptedException {
        return this.getChild(rootPath, nodes -> {
            Map<String, T> nodeDatas = new HashMap<>();
            nodes.parallelStream().forEach(node -> {
                byte[] bb = this.getChildData(rootPath, node, watcher);
                nodeDatas.put(node, JSON.parseObject(new String(bb, StandardCharsets.UTF_8), tClass));
            });
            return nodeDatas;
        });
    }

    /**
     * 获取节点内容
     *
     * @param rootPath
     * @param node
     * @return
     */
    public byte[] getChildData(String rootPath, String node, Watcher watcher) {
        return this.getData(rootPath + "/" + node, watcher);
    }

    public byte[] getData(String nodePath, Watcher watcher) {
        byte[] bb = new byte[0];
        try {
            bb = this.zooKeeper.getData(nodePath, watcher, null);
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return bb;
    }

    /**
     * 获取选举的节点-方式：最少记录数=轮询
     *
     * @param rootPath
     * @return
     * @throws KeeperException
     * @throws InterruptedException
     */
    public PollRqDto getPollChild(String rootPath, Watcher watcher) throws KeeperException, InterruptedException {
        return this.getChildData(rootPath, watcher).values().
                parallelStream().
                filter(val -> Strings.isNotEmpty(val)).
                map(val -> JSON.parseObject(val, PollRqDto.class)).
                min(Comparator.comparing(PollRqDto::getPoolNum)).orElse(new PollRqDto());
    }

    public <T> T getPoll(String path, Watcher watcher, Class<T> tClass) {
        byte[] bb = this.getData(path, watcher);
        String val = new String(bb, StandardCharsets.UTF_8);
        return JSON.parseObject(val, tClass);
    }

    public boolean setPollChild(String path, String content) throws KeeperException, InterruptedException {
        return this.zooKeeper.setData(path, content.getBytes(StandardCharsets.UTF_8), -1) != null;
    }

    public <T> boolean setPollChild(String path, T t) throws KeeperException, InterruptedException {
        return this.setPollChild(path, JSON.toJSONString(t));
    }

    @Override
    public void close() throws IOException {
        try {
            this.zooKeeper.close();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}

