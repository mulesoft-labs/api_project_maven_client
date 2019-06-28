package org.mule.maven.exchange;

import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.Repository;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.mule.maven.exchange.model.ExchangeDependency;
import org.mule.maven.exchange.model.ExchangeModel;
import org.mule.maven.exchange.model.ExchangeModelSerializer;
import org.mule.maven.exchange.utils.ApiProjectConstants;

import java.io.*;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;

 public class ExchangeModelConverter {

    private static Logger LOGGER = Logger.getLogger(ExchangeModelConverter.class.getName());

    public static final String ORG_ID_KEY = "orgId";
    public static final String RAML_FRAGMENT = "raml-fragment";
    public static final String PACKAGER_VERSION = "1.0-SNAPSHOT";

    private ExchangeModelSerializer objectMapper = new ExchangeModelSerializer();

    /**
     * Helper method used by studio by reflection do not change the signature.
     *
     * @param exchangeJson
     * @return
     * @throws IOException
     */
    public static String toPomXml(File exchangeJson) throws IOException {
        final ExchangeModelConverter exchangeModelProcessor = new ExchangeModelConverter();
        FileReader exchangeReader = new FileReader(exchangeJson);
        final Model mavenModel = exchangeModelProcessor.getModel(exchangeJson.getAbsolutePath(), exchangeReader);
        return exchangeModelProcessor.toXmlString(mavenModel);
    }

    public Model getModel(String location, Reader inputStream) throws IOException {
        final ExchangeModel model = objectMapper.read(inputStream);

        boolean modified = false;
        if (StringUtils.isBlank(model.getAssetId())) {
            model.setAssetId(dasherize(model.getName()));
            modified = true;
        }
        if (StringUtils.isBlank(model.getVersion())) {
            model.setVersion("1.0.0-SNAPSHOT");
            modified = true;
        }
        if (StringUtils.isBlank(model.getGroupId())) {
            final String orgId = guessOrgId(location);
            if (orgId != null) {
                model.setGroupId(orgId);
                modified = true;
            } else {
                throw new RuntimeException("No `groupId` on exchange json or System property `groupId` or being an apivcs project");
            }
        }

        if (modified) {
            LOGGER.log(Level.WARNING, "[WARNING] exchange.json was modified by the build.");
            objectMapper.write(model, new File(location));
        }

        final Model mavenModel = toMavenModel(model);
        if (Boolean.getBoolean("exchange.maven.debug")) {
            System.out.println("Maven Model \n" + toXmlString(mavenModel));
        }
        return mavenModel;
    }

    private String guessOrgId(String location) {
        String groupId = System.getProperty("groupId");
        if (groupId == null) {
            final File projectFolder = new File(location).getParentFile();
            final File apiVcsConfigFile = new File(new File(projectFolder, ".apivcs"), "config.properties");
            if (apiVcsConfigFile.exists()) {
                final Properties properties = new Properties();
                try (final FileInputStream fileInputStream = new FileInputStream(apiVcsConfigFile)) {
                    properties.load(fileInputStream);
                } catch (IOException e) {

                }
                groupId = properties.getProperty(ORG_ID_KEY);
            }
        }

        return groupId;
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
        build.setDirectory(String.format("${project.basedir}/%s/target", ApiProjectConstants.EXCHANGE_MODULES_TMP));
        build.setSourceDirectory("${project.basedir}");
        build.addPlugin(createPackagerPlugin(model));
        if (!model.getClassifier().equals(RAML_FRAGMENT)) {
            build.addPlugin(createConnectorInvokerPlugin("install"));
            build.addPlugin(createConnectorInvokerPlugin("deploy"));
        }
        result.setBuild(build);
        return result;
    }

    private Plugin createConnectorInvokerPlugin(String phase) {
        Plugin result = new Plugin();
        result.setGroupId("org.apache.maven.plugins");
        result.setArtifactId("maven-invoker-plugin");
        result.setVersion("3.2.0");
        final Xpp3Dom configuration = new Xpp3Dom("configuration");

        addSimpleNodeTo("goals", phase, configuration);
        addSimpleNodeTo("pom", String.format("${project.basedir}/%s/target/%s/pom.xml",
                ApiProjectConstants.EXCHANGE_MODULES_TMP,
                ApiProjectConstants.REST_CONNECT_OUTPUTDIR), configuration);
        boolean skipInvoker = Boolean.getBoolean(ApiProjectConstants.MAVEN_SKIP_REST_CONNECT);
        addSimpleNodeTo("skipInvocation", Boolean.toString(skipInvoker), configuration);

        // make the connector build a little bit faster by skipping docs and extension model generation
        final Xpp3Dom propertiesNode = new Xpp3Dom("properties");
        addSimpleNodeTo("skipDocumentation", "true", propertiesNode);
        addSimpleNodeTo("mule.maven.extension.model.disable", "true", propertiesNode);
        configuration.addChild(propertiesNode);

        result.setConfiguration(configuration);

        PluginExecution installConnector = new PluginExecution();
        installConnector.setId("rest-connect-" + phase);
        installConnector.setPhase(phase);
        installConnector.addGoal("run");
        result.addExecution(installConnector);

        return result;
    }

    private void addSimpleNodeTo(String nodeName, String valueNode, Xpp3Dom configuration) {
        final Xpp3Dom goalsNode = new Xpp3Dom(nodeName);
        goalsNode.setValue(valueNode);
        configuration.addChild(goalsNode);
    }

    private String dasherize(String name) {
        return name.toLowerCase().replaceAll(" ", "-");
    }

    private Plugin createPackagerPlugin(ExchangeModel model) {
        Plugin result = new Plugin();
        result.setGroupId("org.mule.maven.exchange");
        result.setArtifactId("exchange_api_packager");
        result.setVersion(PACKAGER_VERSION);
        final Xpp3Dom configuration = new Xpp3Dom("configuration");
        addSimpleNodeTo("classifier", model.getClassifier(), configuration);
        addSimpleNodeTo("mainFile", model.getMain(), configuration);
        result.setConfiguration(configuration);

        PluginExecution generateSources = new PluginExecution();
        generateSources.setId("generate-full-api");
        generateSources.setPhase("generate-sources");
        generateSources.addGoal("generate-full-api");
        result.addExecution(generateSources);

        PluginExecution compilePhase = new PluginExecution();
        compilePhase.setId("validate-api");
        compilePhase.setPhase("compile");
        compilePhase.addGoal("validate-api");
        result.addExecution(compilePhase);


        PluginExecution packagePhase = new PluginExecution();
        packagePhase.setId("generate-artifacts");
        packagePhase.setPhase("package");
        packagePhase.addGoal("package-api");
        packagePhase.addGoal("rest-connect");
        result.addExecution(packagePhase);
        return result;
    }

    private Dependency toMavenDependency(ExchangeDependency dep) {
        Dependency result = new Dependency();
        result.setArtifactId(dep.getAssetId());
        result.setGroupId(dep.getGroupId());
        result.setVersion(dep.getVersion());
        result.setClassifier(RAML_FRAGMENT);
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

}
