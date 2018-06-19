package ru.ifmo.server;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/** Universal config file parser */

public class ConfigLoader {

    public ServerConfig load(File file) {
        assert file != null;

        try {
            return getParser(file).parse();
        } catch (ReflectiveOperationException | XMLStreamException | IOException e) {
            throw new ServerException("Unable to parse config files: " + file.getAbsolutePath(), e);
        }
    }

    public ServerConfig load() {
        Parser parser = getParser();

        try {
            return parser == null ? null : parser.parse();
        } catch (ReflectiveOperationException | XMLStreamException | IOException e) {
            throw new ServerException("Unable to parse config files: ", e);
        }
    }

    public Parser getParser(File file) {
        if (file.getName().endsWith(".properties"))
            return new PropertiesParser(file);

        if (file.getName().endsWith(".xml"))
            return new XmlParser(file);

        throw new ServerException("Unsupported file format");
    }

    public Parser getParser() {
        InputStream in = getClass().getClassLoader().getResourceAsStream("web-server-config.properties");

        if (in != null)
            return new PropertiesParser(in);

        in = getClass().getClassLoader().getResourceAsStream("web-server-config.xml");

        if (in != null)
            return new XmlParser(in);

        return null;
    }

}
