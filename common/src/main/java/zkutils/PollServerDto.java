package zkutils;

import lombok.Data;

import java.io.Serializable;

@Data
public class PollServerDto implements Serializable {
    private String ip;
    private int port;
}
