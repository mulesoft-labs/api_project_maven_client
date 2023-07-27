/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.maven.exchange;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.mule.connectivity.restconnect.api.ConnectorType;
import org.mule.connectivity.restconnect.api.Parser;
import org.mule.connectivity.restconnect.api.RestConnect;
import org.mule.connectivity.restconnect.api.SpecFormat;
import org.mule.maven.exchange.utils.ApiProjectConstants;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Mojo(name = "rest-connect", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
@Execute(goal = "rest-connect")
public class RestConnectPackageMojo extends AbstractMojo {

    private static final Map<String, String> EXTENSIONS_SPEC_FORMAT = Collections.unmodifiableMap(
            new HashMap<String, String>() {
                {
                    put("raml", "RAML");
                    put("yaml", "JsonOAS");
                    put("json", "YamlOAS");
                }
            });
    private static final List<String> SUPPORTED_TYPES = Arrays.asList("raml", "oas");


    /**
     * Injected by Maven so that forked process can be
     * launched from the working directory of current maven project in a multi-module build.  Should not be user facing.
     */
    @Component
    private MavenProject project;

    @Component
    protected MavenProjectHelper helper;

    @Parameter(defaultValue = "raml")
    private String classifier;

    /**
     * must point to the main file of the spec (same value of the "main" attribute of the exchange.json file)
     */
    @Parameter(required = true)
    private String mainFile;

    /**
     * reference to the directory that's self contained, if not provided it will be guessed based on {@link ApiProjectConstants#getFatApiDirectory(java.io.File)}
     */
    @Parameter
    private String fatApiDirectory;

    /**
     * property to skip the complete connector generation
     */
    @Parameter(property = ApiProjectConstants.MAVEN_SKIP_REST_CONNECT, defaultValue = "false")
    private boolean skipRestConnect;

    @Override
    public void execute() throws MojoExecutionException {
        if (skipRestConnect) {
            getLog().info("Disabling connector generation..");
            return;
        }else {
            getLog().info(String.format("To disable connector generation parameterize '-D%s=true'", ApiProjectConstants.MAVEN_SKIP_REST_CONNECT));
        }

        if (!SUPPORTED_TYPES.contains(classifier)) {
            // can't generate a connector for fragment
            getLog().info(String.format("Skipping connector generation, [%s] not supported (supported classifiers are [%s])", classifier, String.join(",", SUPPORTED_TYPES)));
            return;
        }

        //looks for zip fat API file
        final File buildDirectory = new File(project.getBuild().getDirectory());
        File fullApiDirectory = calculateFatDirectory(buildDirectory);
        if (!fullApiDirectory.exists()) {
            getLog().warn(String.format("Cant generate connector, full API directory [%s] doesn't exist", fullApiDirectory.getAbsolutePath()));
            return;
        }
        final File apiMainFile = new File(fullApiDirectory, mainFile);
        if (!apiMainFile.exists()) {
            getLog().warn(String.format("Cant generate connector, mainFile [%s] can't be found in the full API directory [%s]",
                    mainFile,
                    fullApiDirectory.getAbsolutePath()));
            return;
        }

        //execute rest-connect
        final File restConnectOutputDir = new File(buildDirectory, ApiProjectConstants.REST_CONNECT_OUTPUTDIR);
        String connectorArtifactId = "mule-plugin-" + project.getArtifactId();
        try {
            RestConnect.getInstance()
                    .createConnectorFromSpec(apiMainFile, SpecFormat.getFromString(calculateRestConnectFormat()), Parser.AMF, ConnectorType.SmartConnector)
                    .withApiName(project.getArtifactId())
                    .withGroupId(project.getGroupId())
                    .withArtifactId(connectorArtifactId)
                    .withVersion(project.getVersion())
                    .withPackageConnector(false)
                    .withprojectDescription(getDescription(project))
                    .withOutputDir(restConnectOutputDir.toPath())
                    .run();
        } catch (Exception e) {
            final String messageError = String.format("Cant generate connector for [%s], rest-connect failed with [%s]", mainFile, e.getMessage());
            getLog().error(messageError);
            throw new MojoExecutionException(messageError, e);
        }

        //verify if rest-connect did do something in the output directory, at least the pom.xml
        if (new File(restConnectOutputDir, "pom.xml").exists()) {
            getLog().info(String.format("Connector successfully generated, use this dependency for a Mule 4 Application:\n" +
                            "         <dependency>\n" +
                            "            <groupId>%s</groupId>\n" +
                            "            <artifactId>%s</artifactId>\n" +
                            "            <version>%s</version>\n" +
                            "            <classifier>mule-plugin</classifier>\n" +
                            "        </dependency>",
                    project.getGroupId(),
                    connectorArtifactId,
                    project.getVersion()));
        } else {
            throw new MojoExecutionException(String.format("Couldn't find any generated connector to attach under [%s]",
                    restConnectOutputDir.getAbsolutePath()));
        }
    }

    private String getDescription(MavenProject project) {
        return String.format("Generated with REST-Connect from version %s:%s:%s.", project.getGroupId(), project.getArtifactId(), project.getVersion());
    }

    /**
     * @return a file pointing to the directory of the fat API, either by taking it from the parameterized {@link #fatApiDirectory},
     * or by doing a guessing in the current build directory.
     */
    private File calculateFatDirectory(File buildDirectory) {
        File result;
        if (fatApiDirectory != null) {
            result = new File(fatApiDirectory);
        } else {
            result = ApiProjectConstants.getFatApiDirectory(buildDirectory);
            getLog().debug(String.format("Parameter 'fatApiDirectory' was null, guessing the fat API to [%s]", result.getAbsolutePath()));
        }
        return result;
    }

    /**
     * @return the supported spec on rest-connect side by the extension of the parameterized {@link #mainFile}
     * (see {@link #EXTENSIONS_SPEC_FORMAT})
     * @throws MojoExecutionException if the extension is not supported, or there's no extension at all
     */
    private String calculateRestConnectFormat() throws MojoExecutionException {
        final int offset = mainFile.lastIndexOf('.');
        final String extension = (offset > 0 ? mainFile.substring(offset + 1) : "DEFAULT_ERROR_VALUE").toLowerCase();
        if (!EXTENSIONS_SPEC_FORMAT.containsKey(extension)) {
            throw new MojoExecutionException(String.format("The 'mainFile' parameter needs to have a valid extension, but found [%s] (accepted extensions are: [%s])",
                    extension, String.join(",", EXTENSIONS_SPEC_FORMAT.keySet())));
        }
        return EXTENSIONS_SPEC_FORMAT.get(extension);
    }
}
