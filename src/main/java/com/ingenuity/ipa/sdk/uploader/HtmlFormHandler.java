package com.ingenuity.ipa.sdk.uploader;

import static org.apache.tika.sax.XHTMLContentHandler.XHTML;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;

public class HtmlFormHandler extends DefaultHandler {

    private String action;
    private final List<Input> inputs = new ArrayList<Input>();

    public String getAction() {
        return action;
    }

    public List<Input> getInputs() {
        return inputs;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (XHTML.equals(uri)) {
            if ("form".equals(localName)) {
                action = attributes.getValue("action");
            }
            if ("input".equals(localName)) {
                inputs.add(new Input(attributes.getValue("type"), attributes.getValue("name"), attributes.getValue("value")));
            }
        }
    }
}
