package zkutils;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by Administrator on 2019/6/27.
 */
@Data
public class PollRqDto implements Serializable {
    private Long poolNum;
    private Object data;
    private String nodePath;
}

