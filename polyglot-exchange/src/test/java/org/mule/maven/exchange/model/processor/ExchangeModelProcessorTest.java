/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.maven.exchange.model.processor;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelProcessor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mule.maven.exchange.ExchangeModelProcessor;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;

import static org.junit.Assert.assertFalse;

@RunWith(Parameterized.class)
public class ExchangeModelProcessorTest {

    public static final String EXCHANGE_JSON = "exchange.json";
    public static final String POM_XML = "pom.xml";
    final File testCase;

    public ExchangeModelProcessorTest(String name, File testCase) {
        this.testCase = testCase;
    }

    @Test
    public void generateModelCorrectly() throws IOException {
        try {
            if (!testCase.getName().startsWith("groupId")) {
                System.setProperty("groupId", "org.mule.test");
            }
            final ExchangeModelProcessor exchangeModelProcessor = new ExchangeModelProcessor();
            final HashMap<String, Object> options = new HashMap<>();
            final File exchangeFile = getExchangeFile(testCase);
            final FileModelSource fileModelSource = new FileModelSource(exchangeModelProcessor.locatePom(testCase));
            options.put(ModelProcessor.SOURCE, fileModelSource);
            final FileInputStream inputStream = new FileInputStream(exchangeFile);
            final Model read = exchangeModelProcessor.read(inputStream, options);
            final String result = exchangeModelProcessor.toXmlString(read);
            final List<String> xmlLines = Files.readAllLines(getPomFile(testCase).toPath(), Charset.forName("UTF-8"));
            final String content = xmlLines.stream().reduce((l, r) -> l + "\n" + r).orElse("");
            final Diff myDiff = DiffBuilder.compare(Input.fromString(result)).withTest(Input.fromString(content))
                    .checkForSimilar()
                    .ignoreWhitespace()
                    .build();

            if (myDiff.hasDifferences()) {
                System.out.println(result);
            }

            assertFalse("XML similar " + myDiff.toString(), myDiff.hasDifferences());
        } finally {
            System.clearProperty("groupId");
        }
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() throws IOException {
        final ClassLoader classLoader = ExchangeModelProcessorTest.class.getClassLoader();
        final String path = ExchangeModelProcessorTest.class.getName().replace('.', File.separatorChar) + ".class";
        final Enumeration<URL> resources = classLoader.getResources(path);
        final List<Object[]> args = new ArrayList<>();
        while (resources.hasMoreElements()) {
            final URL url = resources.nextElement();
            final File parentFile = new File(url.getFile()).getParentFile();
            if (parentFile.exists()) {
                final File[] files = parentFile.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isDirectory() && isTestDirectory(file)) {
                            final File workingDir = new File(FileUtils.getTempDirectory(), file.getName());
                            FileUtils.copyDirectory(file, workingDir);
                            args.add(new Object[]{workingDir.getName(), workingDir});
                        }
                    }
                }
            }
        }
        return args;
    }

    private static boolean isTestDirectory(File file) {
        return getExchangeFile(file).exists() && getPomFile(file).exists();
    }

    private static File getPomFile(File file) {
        return new File(file, POM_XML);
    }

    private static File getExchangeFile(File file) {
        return new File(file, EXCHANGE_JSON);
    }
}
