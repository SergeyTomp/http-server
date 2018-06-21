package ru.ifmo.server;

import ru.ifmo.server.util.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;

public class PropertiesParser extends AbstractParser {

//    public PropertiesParser(File file) {
//        super(file);
//    }

    public PropertiesParser(InputStream in) {
        super(in);
    }

    @Override
    public ServerConfig parse() throws ReflectiveOperationException, IOException {

        Properties prop = new Properties();
        try {
            prop.load(in);
        }
        finally {
            Utils.closeQuiet(in);
        }

        Enumeration<String> keys = (Enumeration<String>) prop.propertyNames();
        ServerConfig config = new ServerConfig();

        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            String val = (String) prop.get(key);
            if ("handlers".equals(key)) {
                String[] mapping = val.split(",");
                for (String route: mapping) {
                    String[] split = route.split("=");
                    Handler handler = (Handler) Class.forName(split[1]).getConstructor().newInstance();
                    config.addHandler(split[0], handler);
                }
            } else if ("handlersclass".equals(key)) {
                String[] mapping = val.split(",");
                for (String route : mapping) {
                    config.addClass(Class.forName(route));
                }
            }
            else {
                reflectiveSetParam(config, key, val);
            }
        }
        return config;
    }
}
