package com.ingenuity.ipa.sdk.uploader;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.html.HtmlMapper;
import org.apache.tika.parser.html.HtmlParser;

import java.io.InputStream;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: danny
 * Date: 10/3/12
 * Time: 1:21 PM
 * To change this template use File | Settings | File Templates.
 */
public class HtmlFormParser {

    HtmlFormHandler htmlFormHandler;

    public HtmlFormParser() {

    }

    public void parse(InputStream htmlFormContent) throws Exception {
        Metadata metadata = new Metadata();
        ParseContext context = new ParseContext();
        context.set(HtmlMapper.class, AllTagMapper.class.newInstance());

        htmlFormHandler = new HtmlFormHandler();

        Parser parser = new HtmlParser();

        parser.parse(htmlFormContent, htmlFormHandler, metadata, context);
    }

    public String getAction() {
        return htmlFormHandler.getAction();
    }

    public List<Input> getInputs() {
        return htmlFormHandler.getInputs();
    }

    public Input getInput(String name) {
        for (Input input : htmlFormHandler.getInputs()) {
            if (name.equals(input.getName())) {
                return input;
            }
        }
        return null;
    }
}
