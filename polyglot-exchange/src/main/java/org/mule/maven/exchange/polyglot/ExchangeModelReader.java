package org.mule.maven.exchange.polyglot;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.ModelParseException;
import org.apache.maven.model.io.ModelReader;
import org.codehaus.plexus.component.annotations.Component;
import org.mule.maven.exchange.ExchangeModelConverter;
import org.sonatype.maven.polyglot.PolyglotModelUtil;
import org.sonatype.maven.polyglot.io.ModelReaderSupport;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;

@Component(role = ModelReader.class, hint = "exchange")
public class ExchangeModelReader extends ModelReaderSupport {

    @Override
    public Model read(Reader input, Map<String, ?> options) throws IOException, ModelParseException {
        if (input == null) {
            throw new IllegalArgumentException("Exchange input reader is null.");
        }
        final String location = PolyglotModelUtil.getLocation(options);
        return new ExchangeModelConverter().getModel(location, input);
    }

}
