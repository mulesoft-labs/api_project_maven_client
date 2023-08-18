/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.maven.exchange;


import amf.apicontract.client.platform.AMFBaseUnitClient;
import amf.apicontract.client.platform.AMFConfiguration;
import amf.apicontract.client.platform.WebAPIConfiguration;
import amf.core.client.platform.AMFParseResult;
import amf.core.client.platform.model.document.BaseUnit;
import amf.core.client.platform.validation.AMFValidationReport;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;
import org.mule.maven.exchange.utils.ApiProjectConstants;
import org.mule.maven.exchange.utils.ExchangeModulesResourceLoader;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.concurrent.ExecutionException;

@Mojo(name = "validate-api", defaultPhase = LifecyclePhase.COMPILE, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
@Execute(goal = "validate-api")
public class ValidateApiMojo extends AbstractMojo {

    @Component
    private MavenProject project;

    @Parameter(defaultValue = "raml")
    private String classifier;

    /**
     * must point to the main file of the spec (same value of the "main" attribute of the exchange.json file)
     */
    @Parameter()
    private String mainFile;

    /**
     * reference to the directory that's self contained, if not provided it will be guessed based on {@link ApiProjectConstants#getFatApiDirectory(java.io.File)}
     */
    @Parameter
    private String fatApiDirectory;

    /**
     * property to skip the complete connector generation
     */
    @Parameter(property = ApiProjectConstants.MAVEN_SKIP_VALIDATE_API, defaultValue = "false")
    private boolean skipValidateApi;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if(skipValidateApi){
            getLog().info("Ignoring validation..");
            return;
        } else {
            getLog().info(String.format("To disable api validation parameterize '-D%s=true'", ApiProjectConstants.MAVEN_SKIP_VALIDATE_API));
        }
        final File buildDirectory = new File(project.getBuild().getDirectory());
        if (classifier.equals("raml") || classifier.equals("raml-fragment") || classifier.equals("oas")) {
            try {
                /* Parsing Raml 10 with specified file returning future. */
                BaseUnit result;
                File parent = calculateFatDirectory(buildDirectory);
                final File ramlFile = new File(parent, this.mainFile);
                if (!ramlFile.exists()) {
                    throw new MojoFailureException("The specified 'main' property '" + this.mainFile + "' can not be found. Please review your exchange.json");
                }
                final String mainFileURL = URLDecoder.decode(ramlFile.toURI().toString(), "UTF-8");
                final AMFBaseUnitClient client;
                final AMFParseResult parseResult;
                final AMFConfiguration amfConfiguration = WebAPIConfiguration.WebAPI().withResourceLoader(new ExchangeModulesResourceLoader(parent.getAbsolutePath().replace(File.separator, "/")));

                client = amfConfiguration.baseUnitClient();
                parseResult = client.parse(mainFileURL).get();

                if (!parseResult.conforms()) {
                    getLog().error(parseResult.toString());
                    throw new MojoFailureException("Build Fail");
                }

                result = parseResult.baseUnit();

                /* Run RAML default validations on parsed unit (expects no errors). */
                final AMFBaseUnitClient validatorClient = WebAPIConfiguration.fromSpec(result.sourceSpec().get()).baseUnitClient();
                final AMFValidationReport validationReport = validatorClient.validate(result).get();

                if (!validationReport.conforms()) {
                    getLog().error(validationReport.toString());
                    throw new MojoFailureException("Build Fail");
                }

            } catch (InterruptedException | ExecutionException | IOException e) {
                throw new MojoExecutionException("Internal error while validating.", e);
            }
        }
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
}
