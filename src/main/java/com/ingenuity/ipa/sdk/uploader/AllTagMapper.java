package com.ingenuity.ipa.sdk.uploader;

import org.apache.tika.parser.html.HtmlMapper;

public class AllTagMapper implements HtmlMapper {

    @Override
    public String mapSafeElement(String name) {
        return name.toLowerCase();
    }

    @Override
    public boolean isDiscardElement(String name) {
        return false;
    }

    @Override
    public String mapSafeAttribute(String elementName, String attributeName) {
        return attributeName.toLowerCase();
    }
}
