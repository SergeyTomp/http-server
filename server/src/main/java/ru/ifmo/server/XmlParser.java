package ru.ifmo.server;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class XmlParser extends AbstractParser {

    public XmlParser(File file) {
        super(file);
    }

    public XmlParser(InputStream in) {
        super(in);
    }

    @Override
    public ServerConfig parse() throws ReflectiveOperationException, IOException, XMLStreamException {

        XMLInputFactory factory = XMLInputFactory.newFactory();
        XMLStreamReader reader = factory.createXMLStreamReader(in);

        StringBuilder value = new StringBuilder();
        String url = null;

        ServerConfig config = new ServerConfig();

        while (reader.hasNext()) {
            reader.next();

            if (reader.getEventType() == XMLStreamReader.CHARACTERS) {
                value.append(reader.getText());

            } else if (reader.getEventType() == XMLStreamConstants.START_ELEMENT) {
                String qName = reader.getName().getLocalPart();

                if ("handler".equals(qName)) {
                    for (int i = 0; i < reader.getAttributeCount(); i++) {
                        String attrName = reader.getAttributeName(i).getLocalPart();

                        if ("url".equals(attrName)) {
                            url = reader.getAttributeValue(i);
                            break;
                        }
                    }
                }

            } else if (reader.getEventType() == XMLStreamConstants.END_ELEMENT) {

                String qName = reader.getName().getLocalPart();

                String val = value.toString().trim();

                if ("handler".equals(qName)) {
                    Handler handler = (Handler) Class.forName(val).newInstance();
                    config.addHandler(url, handler);

                } else if ("scanclass".equals(qName)) {
                    config.addClass(Class.forName(val));
                } else if ("handlers".equals(qName) || "scanclasses".equals(qName)) {
                    continue;

                } else {
                    reflectiveSetParam(config, qName, val);
                }

                value.setLength(0);
            }
        }

        return config;
    }

}
