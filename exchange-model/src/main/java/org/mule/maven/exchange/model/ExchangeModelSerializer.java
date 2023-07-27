/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.maven.exchange.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.*;

public class ExchangeModelSerializer {

    private ObjectMapper objectMapper = new ObjectMapper();

    public ExchangeModelSerializer() {
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public ExchangeModel read(File exchangeFile) throws IOException {
        return read(new FileInputStream(exchangeFile));
    }

    public ExchangeModel read(InputStream inputStream) throws IOException {
        try {
            return read(new InputStreamReader(inputStream, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public ExchangeModel read(Reader reader) throws IOException {
        return objectMapper.readValue(reader, ExchangeModel.class);
    }


    public void write(ExchangeModel model, File output) throws IOException {
        objectMapper.writeValue(output, model);
    }


    public void write(ExchangeModel model, Writer output) throws IOException {
        objectMapper.writeValue(output, model);
    }

    public void write(ExchangeModel model, OutputStream output) throws IOException {
        objectMapper.writeValue(output, model);
    }

    /**
     * Hack due to apikit's code to read the main file from the exchange.json file.
     * Until it's fixed, or just for backwards compatibility, leave it here.
     * See APIKIT-1956
     *
     * @param shouldIndent boolean to enable/disable the feature on the mapper side.
     */
    public void indent(boolean shouldIndent){
        if (shouldIndent) {
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        } else {
            objectMapper.disable(SerializationFeature.INDENT_OUTPUT);
        }
    }

}
