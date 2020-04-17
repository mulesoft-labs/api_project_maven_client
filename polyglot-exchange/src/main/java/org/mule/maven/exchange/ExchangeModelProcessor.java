package org.mule.maven.exchange;

import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelProcessor;
import org.apache.maven.model.building.ModelSource2;
import org.apache.maven.model.io.ModelParseException;
import org.apache.maven.model.io.ModelReader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.model.locator.ModelLocator;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.mule.maven.exchange.model.ExchangeDependency;
import org.mule.maven.exchange.model.ExchangeModel;
import org.mule.maven.exchange.model.ExchangeModelSerializer;
import org.mule.maven.exchange.utils.ApiProjectConstants;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;

@Component(role = ModelProcessor.class)
public class ExchangeModelProcessor implements ModelProcessor {

    public static final String ORG_ID_KEY = "orgId";
    public static final String RAML_FRAGMENT = "raml-fragment";

    private static Logger LOGGER = Logger.getLogger(ExchangeModelProcessor.class.getName());

    private static final String EXCHANGE_JSON = "exchange.json";
    private static final String TEMPORAL_EXCHANGE_XML = ".exchange.xml";

    public static final String PACKAGER_VERSION = "1.0.2";

    public static final String MAVEN_FACADE_SYSTEM_PROPERTY = "-Dexchange.maven.repository.url";

    private ExchangeModelSerializer objectMapper = new ExchangeModelSerializer();

    @Requirement
    private ModelReader modelReader;

    @Requirement
    private ModelLocator modelLocator;

    public ExchangeModelProcessor() {
    }

    public void setModelReader(ModelReader modelReader) {
        this.modelReader = modelReader;
    }

    public void setModelLocator(ModelLocator modelLocator) {
        this.modelLocator = modelLocator;
    }

    @Override
    public File locatePom(File projectDirectory) {
        File pomFile = new File(projectDirectory, EXCHANGE_JSON);
        if (pomFile.exists()) {
            pomFile = new File(pomFile.getParentFile(), TEMPORAL_EXCHANGE_XML);
            try {
                pomFile.createNewFile();
                pomFile.deleteOnExit();
            } catch (IOException e) {
                throw new RuntimeException(String.format("error creating temporal `%s` empty file", TEMPORAL_EXCHANGE_XML), e);
            }
        } else {
            // behave like proper maven in case there is no pom from manager
            pomFile = modelLocator.locatePom(projectDirectory);
        }
        return pomFile;
    }

    @Override
    public Model read(File file, Map<String, ?> map) throws IOException, ModelParseException {
        return read(new FileInputStream(file), map);
    }

    @Override
    public Model read(InputStream inputStream, Map<String, ?> map) throws IOException, ModelParseException {
        return read(new InputStreamReader(inputStream, StandardCharsets.UTF_8), map);
    }

    @Override
    public Model read(Reader reader, Map<String, ?> options) throws IOException, ModelParseException {
        Object source = (options != null) ? options.get(SOURCE) : null;
        if (source instanceof ModelSource2 && ((ModelSource2) source).getLocation().endsWith(TEMPORAL_EXCHANGE_XML)) {
            // lookup the temporal file ".exchange.xml"
            final String temporalExchangeXml = ((ModelSource2) source).getLocation();
            final File temporaryExchangeXml = new File(temporalExchangeXml);
            final File exchangeJson = new File(temporaryExchangeXml.getParent(), EXCHANGE_JSON);

            // retrieve the original "exchange.json" file and obtain the Maven model
            final String exchangeJsonLocation = exchangeJson.getAbsolutePath();
            final FileInputStream exchangeJsonInputStream = new FileInputStream(exchangeJson);
            final Model mavenModel = getModel(exchangeJsonLocation, exchangeJsonInputStream);

            // store the reference from the original source of truth, the "exchange.json" file
            final FileModelSource temporalSourceXml = new FileModelSource(exchangeJson);
            ((Map) options).put(ModelProcessor.SOURCE, temporalSourceXml);

            // serialize the Maven model as XML in the temporal ".exchange.xml" file for proper installation of the .pom
            final String data = toXmlString(mavenModel);
            FileUtils.fileWrite(temporaryExchangeXml, data);
            mavenModel.setPomFile(temporaryExchangeXml);

            // done =]
            return mavenModel;
        } else {
            //It's a normal maven project with a pom.xml file
            //It's a normal maven project with a pom.xml file
            return modelReader.read(reader, options);
        }
    }

    /**
     * Helper method used by studio by reflection do not change the signature.
     *
     * @param exchangeJson
     * @return
     * @throws IOException
     */
    public static String toPomXml(File exchangeJson) throws IOException {
        final ExchangeModelProcessor exchangeModelProcessor = new ExchangeModelProcessor();
        final FileInputStream exchangeJsonInputStream = new FileInputStream(exchangeJson);
        final Model mavenModel = exchangeModelProcessor.getModel(exchangeJson.getAbsolutePath(), exchangeJsonInputStream);
        return exchangeModelProcessor.toXmlString(mavenModel);
    }

    private Model getModel(String location, InputStream inputStream) throws IOException {
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

    private Repository createExchangeRepository() {
        String url = System.getProperty(MAVEN_FACADE_SYSTEM_PROPERTY, "https://maven.anypoint.mulesoft.com/api/v2/maven");
        Repository repository = new Repository();
        repository.setId("anypoint-exchange-v2");
        repository.setName("Anypoint Exchange");
        repository.setUrl(url);
        repository.setLayout("default");
        return repository;
    }

}
