package org.mule.maven.exchange;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.maven.model.*;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelProcessor;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.building.ModelSource2;
import org.apache.maven.model.io.ModelParseException;
import org.apache.maven.model.io.ModelReader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.mule.maven.exchange.model.ExchangeDependency;
import org.mule.maven.exchange.model.ExchangeModel;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;

@Component(role = ModelProcessor.class)
public class ExchangeModelProcessor implements ModelProcessor {

    private static final String JSON_EXT = ".json";

    public static final String PACKAGER_VERSION = "1.0-SNAPSHOT";

    private ObjectMapper objectMapper = new ObjectMapper();

    @Requirement
    private ModelReader modelReader;

    public Model read(File file, Map<String, ?> map) throws IOException, ModelParseException {
        return read(new FileInputStream(file), map);
    }


    public Model read(InputStream inputStream, Map<String, ?> map) throws IOException, ModelParseException {
        return read(new InputStreamReader(inputStream, StandardCharsets.UTF_8), map);
    }

    public Model read(Reader reader, Map<String, ?> options) throws IOException, ModelParseException {

        Object source = (options != null) ? options.get(SOURCE) : null;
        if (source instanceof ModelSource2 && ((ModelSource2) source).getLocation().endsWith(JSON_EXT)) {
            final ExchangeModel model = objectMapper.readValue(reader, ExchangeModel.class);
            final Model mavenModel = toMavenModel(model);
            if (Boolean.getBoolean("exchange.maven.debug")) {
                System.out.println("Maven Model \n" + toXmlString(mavenModel));
            }
            return mavenModel;
        } else {
            //It's a normal maven project with a pom.xml file
            //It's a normal maven project with a pom.xml file
            return modelReader.read(reader, options);
        }
    }

    public String toXmlString(Model mavenModel) throws IOException {
        StringWriter stringWriter = new StringWriter();
        new MavenXpp3Writer().write(stringWriter, mavenModel);
        return stringWriter.toString();
    }

    private Model toMavenModel(ExchangeModel model) {
        final Model result = new Model();
        result.setModelVersion("4.0.0");
        result.setArtifactId(model.getAssetId());
        result.setGroupId(model.getGroupId());
        result.setName(model.getName());
        result.setVersion(model.getVersion());
        result.setRepositories(singletonList(createExchangeRepository()));
        final List<Dependency> dependencies = model.getDependencies().stream().map(this::toMavenDependency).collect(Collectors.toList());
        result.setDependencies(dependencies);
        final Build build = new Build();
        build.setDirectory("${project.basedir}/.exchange_modules_tmp/target");
        build.setSourceDirectory("${project.basedir}");
        build.addPlugin(createPackagerPlugin(model.getClassifier()));
        result.setBuild(build);
        return result;
    }

    private Plugin createPackagerPlugin(String classifier) {
        Plugin result = new Plugin();
        result.setGroupId("org.mule.maven.exchange");
        result.setArtifactId("exchange_api_packager");
        result.setVersion(PACKAGER_VERSION);
        final Xpp3Dom configuration = new Xpp3Dom("configuration");
        final Xpp3Dom classifierNode = new Xpp3Dom("classifier");
        classifierNode.setValue(classifier);
        configuration.addChild(classifierNode);
        result.setConfiguration(configuration);
        PluginExecution pluginExecution = new PluginExecution();
        pluginExecution.setPhase("package");
        pluginExecution.addGoal("package");
        result.addExecution(pluginExecution);
        return result;
    }

//
//    <build>
//        <plugins>
//            <plugin>
//                <groupId>com.salesforce.turtles</groupId>
//                <artifactId>turtles-maven-plugin</artifactId>
//                <version>0.1.1-PACKAGER_VERSION</version>
//                <configuration>
//                    <mainClass>com.force.weave.WeaveRunner</mainClass>
//                    <turtlesVersion>0.1.1-PACKAGER_VERSION</turtlesVersion>
//                    <disableLimits>true</disableLimits>
//                </configuration>
//
//                <executions>
//                    <execution>
//                        <goals>
//                            <goal>invoke</goal>
//                        </goals>
//                    </execution>
//                </executions>
//            </plugin>
//        </plugins>
//    </build>

    private Dependency toMavenDependency(ExchangeDependency dep) {
        Dependency result = new Dependency();
        result.setArtifactId(dep.getAssetId());
        result.setGroupId(dep.getGroupId());
        result.setVersion(dep.getVersion());
        result.setClassifier("raml-fragment");
        result.setType("zip");
        return result;
    }

    // <repository>
//     <id>anypoint-exchange-v2</id>
//     <name>Anypoint Exchange</name>
//     <url>https://maven.anypoint.mulesoft.com/api/v2/maven</url>
//    <layout>default</layout>
// </repository>
    private Repository createExchangeRepository() {
        Repository repository = new Repository();
        repository.setId("anypoint-exchange-v2");
        repository.setName("Anypoint Exchange");
        repository.setUrl("https://maven.anypoint.mulesoft.com/api/v2/maven");
        repository.setLayout("default");
        return repository;
    }

    public File locatePom(File projectDirectory) {
        return new File(projectDirectory, "exchange.json");
    }
}
