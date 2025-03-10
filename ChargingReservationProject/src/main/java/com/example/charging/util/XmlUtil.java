package com.example.charging.util;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.*;
import org.xml.sax.InputSource;

public class XmlUtil {

    /**
     * 将 Map 转换为 XML 字符串（简单实现）
     */
    public static String mapToXml(Map<String, String> data) throws Exception {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        Document document = documentBuilder.newDocument();
        Element root = document.createElement("xml");
        document.appendChild(root);
        for (Map.Entry<String, String> entry : data.entrySet()) {
            Element field = document.createElement(entry.getKey());
            field.appendChild(document.createTextNode(entry.getValue()));
            root.appendChild(field);
        }
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        return writer.getBuffer().toString();
    }

    /**
     * 将 XML 字符串转换为 Map
     */
    public static Map<String, String> xmlToMap(String xmlStr) throws Exception {
        Map<String, String> map = new HashMap<>();
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        Document doc = documentBuilder.parse(new InputSource(new StringReader(xmlStr)));
        NodeList nodeList = doc.getDocumentElement().getChildNodes();
        for (int idx = 0; idx < nodeList.getLength(); idx++) {
            Node node = nodeList.item(idx);
            if (node instanceof Element) {
                map.put(node.getNodeName(), node.getTextContent());
            }
        }
        return map;
    }
}
