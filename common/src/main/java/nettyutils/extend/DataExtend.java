package nettyutils.extend;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;

/**
 * Created by Administrator on 2019/7/4.
 */
public class DataExtend {
    public static String getContentByBb(ByteBuf buf) {
        return buf.toString(CharsetUtil.UTF_8);
    }

    public static ByteBuf getBbByString(String content) {
        return Unpooled.copiedBuffer(content, CharsetUtil.UTF_8);
    }
}
