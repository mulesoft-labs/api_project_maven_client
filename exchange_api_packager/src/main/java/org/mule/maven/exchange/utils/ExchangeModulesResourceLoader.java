package org.mule.maven.exchange.utils;

import amf.client.remote.Content;
import amf.client.resource.ClientResourceLoader;
import amf.client.resource.ResourceNotFound;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExchangeModulesResourceLoader implements ClientResourceLoader {

    private static final String EXCHANGE_MODULES = "/exchange_modules/";

    private static final String EXCHANGE_MODULES_REGEX = ".*(\\/exchange_modules\\/.*)";

    private static final Pattern EXCHANGE_MODULES_PATTERN = Pattern.compile(EXCHANGE_MODULES_REGEX);


    private final String rootProjectPath;

    public ExchangeModulesResourceLoader(String rootProjectPath){
        this.rootProjectPath = rootProjectPath;
    }

    @Override
    public CompletableFuture<Content> fetch(String resource) {
        Matcher matcher = EXCHANGE_MODULES_PATTERN.matcher(resource);
        if (matcher.matches()){
            String group = matcher.group(matcher.groupCount());
            File file = new File(rootProjectPath, group);
            try {
                String stringContent = FileUtils.readFileToString(file);
                return CompletableFuture.completedFuture(new Content(stringContent,resource));
            } catch (IOException e) {
                e.fillInStackTrace();
            }
        }
        return failedFuture(resource);
    }

    private CompletableFuture<Content> failedFuture(String resource) {
        return CompletableFuture.supplyAsync(() -> {
            throw new RuntimeException(new ResourceNotFound(resource));
        });
    }

    @Override
    public boolean accepts(String resource) {
        return resource.contains(EXCHANGE_MODULES);
    }
}
