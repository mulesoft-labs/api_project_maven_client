package org.mule.maven.exchange.polyglot;

import org.codehaus.plexus.component.annotations.Component;
import org.sonatype.maven.polyglot.mapping.Mapping;
import org.sonatype.maven.polyglot.mapping.MappingSupport;

@Component(role = Mapping.class, hint = "exchange")
public class ExchangeMapping extends MappingSupport {
    public ExchangeMapping() {
        super("exchange");
        setPomNames("exchange.json");
        setAcceptLocationExtensions(".json");
        setAcceptOptionKeys("exchange:4.0.0");
        setPriority(1);
    }
}