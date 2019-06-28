package org.mule.maven.exchange.polyglot;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.ModelWriter;
import org.codehaus.plexus.component.annotations.Component;
import org.sonatype.maven.polyglot.io.ModelWriterSupport;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

@Component(role = ModelWriter.class, hint = "exchange")
public class ExchangeModelWriter extends ModelWriterSupport {

    @Override
    public void write(Writer output, Map<String, Object> o, Model model) throws IOException {
        throw new UnsupportedOperationException(
                "Not implemented yet, this method should write in the content of an 'exchange.json' from a maven model into the output parameter."
        );
    }
}
