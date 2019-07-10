package zkutils;

import lombok.Data;

import java.io.Serializable;

@Data
public class ZkRqDto implements Serializable {
    private String zkString;
    private String zkRootNode;
    private String ip;
    private int port;

    public ZkRqDto(String zkString, String zkRootNode, String ip, int port) {
        this.zkString = zkString;
        this.zkRootNode = zkRootNode;
        this.ip = ip;
        this.port = port;
    }

    public ZkRqDto() {
    }
}
