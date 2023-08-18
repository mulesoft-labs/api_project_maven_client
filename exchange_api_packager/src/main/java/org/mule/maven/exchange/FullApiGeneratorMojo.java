/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.maven.exchange;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.mule.maven.exchange.utils.ApiProjectConstants.*;
import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

@Mojo(name = "generate-full-api", defaultPhase = LifecyclePhase.GENERATE_SOURCES, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
@Execute(goal = "generate-full-api")
public class FullApiGeneratorMojo extends AbstractMojo {


    @Component
    private MavenProject project;

    @Component
    private MavenSession mavenSession;

    @Component
    private BuildPluginManager pluginManager;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        //Download transitive dependencies

        executeMojo(
                plugin(
                        groupId("org.apache.maven.plugins"),
                        artifactId("maven-dependency-plugin"),
                        version("2.0")
                ),
                goal("copy-dependencies"),
                configuration(
                        element(name("outputDirectory"), "${project.build.directory}/" + MAVEN_SKIP_REST_CONNECT),
                        element(name("useRepositoryLayout"), "true")
                ),
                executionEnvironment(
                        project,
                        mavenSession,
                        pluginManager
                )
        );

        final File buildDirectory = new File(project.getBuild().getDirectory());
        final File fullApiDirectory = getFatApiDirectory(buildDirectory);
        final File sourceDirectory = new File(project.getBuild().getSourceDirectory());
        final String targetRootPath = fullApiDirectory.getPath() + File.separator + EXCHANGE_MODULES;
        final List<Dependency> projectDependencies = getDependenciesWithPotencialBadFormedPath();

        try {
            unzipDependenciesAndCopyTo(new File(buildDirectory, MAVEN_SKIP_REST_CONNECT), new File(fullApiDirectory, EXCHANGE_MODULES), targetRootPath, projectDependencies);
            FileUtils.copyDirectory(sourceDirectory, fullApiDirectory, new ApiSourceFileFilter(sourceDirectory, buildDirectory), true);
        } catch (IOException e) {
            throw new MojoExecutionException("Exception while trying to copy sources for `exchange-generate-full-api`", e);
        }

    }

    private void unzipDependenciesAndCopyTo(File sourceFile, File targetFile, String targetRootPath, List<Dependency> projectDependencies) throws MojoExecutionException {
        try {

            if (sourceFile.isDirectory()) {
                File[] listFiles = sourceFile.listFiles();
                if (listFiles != null) {
                    for (File childFile : listFiles) {
                        unzipDependenciesAndCopyTo(childFile, new File(targetFile, childFile.getName()), targetRootPath, projectDependencies);
                    }
                }
            } else if (sourceFile.isFile() && sourceFile.getName().endsWith(".zip")) {

                Optional<String> fixedPath = getFixedPath(projectDependencies, targetFile, targetRootPath);
                File targetDirectory = targetFile.getParentFile();

               if(fixedPath.isPresent()){
                   targetDirectory = new File(fixedPath.get());
               }

                targetDirectory.mkdirs();
                byte[] buffer = new byte[1024];
                try (ZipInputStream zis = new ZipInputStream(new FileInputStream(sourceFile))) {

                    ZipEntry zipEntry = zis.getNextEntry();
                    while (zipEntry != null) {
                        File newFile = new File(targetDirectory, zipEntry.getName());
                        if(zipEntry.isDirectory()){
                            newFile.mkdir();
                        }else{
                            try (FileOutputStream fos = new FileOutputStream(newFile)) {
                                int len;
                                while ((len = zis.read(buffer)) > 0) {
                                    fos.write(buffer, 0, len);
                                }
                            }
                        }
                        zipEntry = zis.getNextEntry();
                    }
                    zis.closeEntry();
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to unzip " + sourceFile.getAbsolutePath(), e);
        }
    }

    private Optional<String> getFixedPath(List<Dependency> projectDependencies, File targetFile, String targetRootPath){
        for(Dependency p : projectDependencies) {
            String groupIdPath = StringUtils.replace(p.getGroupId(), ".", File.separator);
            String partGavPath = File.separator  + p.getArtifactId() + File.separator + p.getVersion();
            String gavPath = groupIdPath + partGavPath;

            if(StringUtils.endsWith(targetFile.getParentFile().getPath(), gavPath)){
                return Optional.of(targetRootPath + File.separator + p.getGroupId() + partGavPath);
            }
        }
        return Optional.empty();

    }

    private List<Dependency> getDependenciesWithPotencialBadFormedPath(){
        Stream<Dependency> collect = project.getDependencies().stream().filter(d -> StringUtils.containsAny(((Dependency) d).getGroupId(), "."));
        List<Dependency> depWithDotsInGroupId = collect.collect(Collectors.toList());
        return depWithDotsInGroupId;
    }
}
