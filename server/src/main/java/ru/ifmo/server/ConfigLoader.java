package ru.ifmo.server;

import javax.xml.stream.XMLStreamException;
import java.io.*;

public class ConfigLoader {

    public ServerConfig load(File file) {
        assert file != null : "ConfigFile does not exist!";
        try {
            return getParser(file).parse();
        } catch (ReflectiveOperationException | XMLStreamException | IOException e) {
            throw new ServerException("Unable to parse configFile from file: " + file.getAbsolutePath(), e);
        }
    }

    public ServerConfig load() {
        Parser parser = getParser();
        try {
            return parser == null ? null : parser.parse();
        } catch (ReflectiveOperationException | XMLStreamException | IOException e) {
            throw new ServerException("Unable to parse configFile from filestream: ", e);
        }
    }

    public Parser getParser(File file) {
        InputStream in = null;
        try {
            in = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            System.out.println("ConfigFile does not exist!");
            e.printStackTrace();
        }
        if (file.getName().endsWith(".properties")){
            return new PropertiesParser(in);
        }
        if (file.getName().endsWith(".xml")){
            return new XmlParser(in);
        }
        throw new ServerException("Unsupported file type");
    }

    public Parser getParser() {

        InputStream in = getClass().getClassLoader().getResourceAsStream("web-server-xml.xml");
        if (in != null){
            return new XmlParser(in);
        }
         in = getClass().getClassLoader().getResourceAsStream("web-server-properties.properties");
        if (in != null){
            return new PropertiesParser(in);
        }
        throw new ServerException("No configuration file found");
    }
}
