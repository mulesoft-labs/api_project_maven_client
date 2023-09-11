/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.maven.exchange;

import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.mule.maven.exchange.model.ExchangeModel;
import org.mule.maven.exchange.model.ExchangeModelSerializer;
import org.mule.maven.exchange.utils.ApiProjectConstants;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.mule.maven.exchange.utils.ApiProjectConstants.getFatApiDirectory;

@Mojo(name = "package-api", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
@Execute(goal = "package-api")
public class ExchangeApiPackagerMojo extends AbstractMojo {


    private static final String EXCHANGE_JSON = "exchange.json";

    /**
     * Injected by Maven so that forked process can be
     * launched from the working directory of current maven project in a multi-module build.  Should not be user facing.
     */
    @Component
    private MavenProject project;

    @Component
    private MavenSession mavenSession;

    @Component
    private BuildPluginManager pluginManager;

    @Component
    protected MavenProjectHelper helper;

    @Parameter(defaultValue = "raml")
    private String classifier;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().debug(String.format("filter hidden files: %b", filterOn));
        final File sourceDirectory = new File(project.getBuild().getSourceDirectory());
        final File buildDirectory = new File(project.getBuild().getDirectory());
        final File apiZip = new File(buildDirectory, getFileName());
        createZip(sourceDirectory, new ApiSourceFileFilter(sourceDirectory, buildDirectory), apiZip);
        //create simple zip
        helper.attachArtifact(project, getType(), getClassifier(), apiZip);

        //create full zip
        final String fatFileName = getFatFileName(project, getClassifier(), getType());
        final File fatApiZip = new File(buildDirectory, fatFileName);
        createZip(getFatApiDirectory(buildDirectory), TrueFileFilter.INSTANCE, fatApiZip);
        helper.attachArtifact(project, getType(), createFatClassifier(getClassifier()), fatApiZip);
    }

    /**
     * Provides the name of the ZIP file that has the self contained API (with all of its dependencies)
     *
     * @param project    working project
     * @param classifier artifact's classifier
     * @param type       artifact's type
     * @return the name of the fat file for the ZIP file, such as "american-flights-api-1.0.3-raml-full.zip"
     */
    private String getFatFileName(MavenProject project, final String classifier, final String type) {
        return project.getBuild().getFinalName() + "-" + createFatClassifier(classifier) + "." + type;
    }

    private static String createFatClassifier(String classifier) {
        return "fat-" + classifier;
    }

    /**
     * property to avoid packaging hidden and zipped files
     */
    @Parameter(property = ApiProjectConstants.MAVEN_FILTER_HIDDEN, defaultValue = "false")
    private boolean filterOn;

    private void createZip(File sourceDir, FileFilter fileFilter, File zipFile) throws MojoExecutionException {
        try {
            try (FileOutputStream fileWriter = new FileOutputStream(zipFile);
                 ZipOutputStream zip = new ZipOutputStream(fileWriter)) {
                addZipEntries(sourceDir, fileFilter, zip, null, filterOn);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Exception while generating zip file", e);
        }
    }

    public String getType() {
        return "zip";
    }

    public String getClassifier() {
        return classifier;
    }

    private void addZipEntries(File sourceDir, FileFilter fileFilter, ZipOutputStream zip, String basePath, boolean filterOn) throws IOException {
        final File[] files = sourceDir.listFiles(fileFilter);
        if (files != null) {
            for (File file : files) {
                if(filterOn && filteredFiles(file.getName()))
                    continue; // avoid zipping hidden files or other zipped files
                final String name = basePath != null ? basePath + "/" + file.getName() : file.getName();
                if (file.isDirectory()) {
                    addZipEntries(file, fileFilter, zip, name, filterOn);
                } else {
                    // hack due to apikits issues while reading exchange.json file.
                    file = tamperFileIfExchangeJson(file);
                    try (FileInputStream in = new FileInputStream(file)) {
                        final byte[] buf = new byte[1024];
                        zip.putNextEntry(new ZipEntry(name));
                        int len;
                        while ((len = in.read(buf)) > 0) {
                            zip.write(buf, 0, len);
                        }
                        zip.closeEntry();
                    }
                }
            }
        }
    }

    private boolean filteredFiles(String entryName) {
        return entryName.toLowerCase().endsWith(".zip") || entryName.startsWith(".");
    }

    /**
     * Hack due to apikit's code to read the main file from the exchange.json file.
     * Until it's fixed, or just for backwards compatibility, leave it here.
     * See APIKIT-1956
     *
     * @param file file to check weather it's exchange.json or not.
     * @return the same input {@code file} if it wasn't the exchange.json file. Otherwise, it reads the full content and
     * removes all the spaces by using the serializer. In case anything fails, it falls back to the original file.
     */
    private File tamperFileIfExchangeJson(File file) {
        if (file.getName().equals(EXCHANGE_JSON)) {
            try {
                final ExchangeModelSerializer objectMapper = new ExchangeModelSerializer();
                objectMapper.indent(false);
                final ExchangeModel model = objectMapper.read(file);
                final File temporal_exchange = File.createTempFile("temporal_exchange", ".json");
                objectMapper.write(model, temporal_exchange);
                return temporal_exchange;
            } catch (IOException e) {
                //fail silently, returning the original file
                if (getLog().isDebugEnabled()){
                    getLog().debug(String.format("There has been an issue reading the [%s] file, message: [%s]. Full stack below.",
                            EXCHANGE_JSON,
                            e.getMessage()));
                    e.printStackTrace();
                }
            }
            return file;
        }
        return file;
    }

    protected String getFileName() {
        return project.getBuild().getFinalName() + "-" + getClassifier() + "." + getType();
    }

}
