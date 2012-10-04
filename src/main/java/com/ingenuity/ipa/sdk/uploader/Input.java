package com.ingenuity.ipa.sdk.uploader;

import lombok.Data;

@Data
public class Input {

    private final String type;
    private final String name;
    private final String value;

    public Input(String type, String name, String value) {
        this.type = type;
        this.name = name;
        this.value = value;
    }
}
