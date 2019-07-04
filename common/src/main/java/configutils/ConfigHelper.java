package configutils;

import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by Administrator on 2019/7/4.
 */
public class ConfigHelper {

    private String fileName;

    public ConfigHelper(String fileName) {
        this.fileName = StringUtils.isEmpty(fileName) ? "default.properties" : fileName;
    }

    public static ConfigHelper getHelper() {
        return getHelper("");
    }

    public static ConfigHelper getHelper(String fileName) {
        return new ConfigHelper(fileName);
    }

    public InputStream inputStream() {
        InputStream stream = null;
        try {
            stream = this.getClass().getClassLoader().getResourceAsStream(this.fileName);
        } catch (Exception ex) {
            stream = ClassLoader.getSystemResourceAsStream(fileName);
        }
        return stream;
    }

    public Properties load() {
        Properties properties = new Properties();
        try {
            InputStream stream = this.inputStream();
            properties.load(stream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return properties;
    }
}
