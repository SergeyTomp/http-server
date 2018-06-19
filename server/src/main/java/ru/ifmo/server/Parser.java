package ru.ifmo.server;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;

public interface Parser {

    ServerConfig parse() throws ReflectiveOperationException, IOException, XMLStreamException;
}
