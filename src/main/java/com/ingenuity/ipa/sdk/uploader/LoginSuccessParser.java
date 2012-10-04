package com.ingenuity.ipa.sdk.uploader;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.html.HtmlMapper;
import org.apache.tika.parser.html.HtmlParser;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.xpath.AttributeMatcher;
import org.apache.tika.sax.xpath.MatchingContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.InputStream;

public class LoginSuccessParser {

    boolean success;

    public void parse(InputStream htmlResponseStream) throws Exception {
        Metadata metadata = new Metadata();
        ParseContext context = new ParseContext();
        context.set(HtmlMapper.class, AllTagMapper.class.newInstance());

        ContentHandler handler = new MatchingContentHandler(new BodyContentHandler(), new AttributeMatcher()) {
            @Override
            public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
                if ("genericSuccess".equals(attributes.getValue("class"))) {
                    success = true;
                }
            }
        };

        Parser parser = new HtmlParser();

        parser.parse(htmlResponseStream, handler, metadata, context);
    }

    public boolean isSuccess() {
        return success;
    }
}
