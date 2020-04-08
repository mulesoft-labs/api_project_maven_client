package org.mule.maven.exchange;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.execution.MavenSession;
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
import java.nio.file.Files;
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
        try {
            unzipDependenciesAndCopyTo(new File(buildDirectory, MAVEN_SKIP_REST_CONNECT), new File(fullApiDirectory, EXCHANGE_MODULES), targetRootPath);
            FileUtils.copyDirectory(sourceDirectory, fullApiDirectory, new ApiSourceFileFilter(sourceDirectory, buildDirectory), true);
        } catch (IOException e) {
            throw new MojoExecutionException("Exception while trying to copy sources for `exchange-generate-full-api`", e);
        }

    }

    private void unzipDependenciesAndCopyTo(File sourceFile, File targetFile, String targetRootPath) throws MojoExecutionException {
        try {
            if (sourceFile.isDirectory()) {
                File[] listFiles = sourceFile.listFiles();
                if (listFiles != null) {
                    for (File childFile : listFiles) {
                        unzipDependenciesAndCopyTo(childFile, new File(targetFile, childFile.getName()), targetRootPath);
                    }
                }
            } else if (sourceFile.isFile() && sourceFile.getName().endsWith(".zip")) {
                // there are cases that the groupId is generated folder levels, we need to reduce the amount of folders
                // to one and build the groupId with dot instead of slash

                // assuming that the path is built with groupId/artifactId/version/ramlFileName.zip
                int validFolders = 4;
                File finalTargetFile = targetFile;
                File currentDirectory = targetFile;
                boolean wellBuilded = false;
                while(validFolders > 0){
                    if(currentDirectory.getParentFile().getPath().equals(targetRootPath)){
                        // the path is well built as we found the groupId represented with one folder level
                        wellBuilded = true;
                    } else if(validFolders > 1){
                        currentDirectory = currentDirectory.getParentFile();
                    }

                    validFolders--;
                }
                if(!wellBuilded){
                    String endPath = StringUtils.remove(targetFile.getPath(), currentDirectory.getPath());
                    String groupId = StringUtils.remove(currentDirectory.getPath(), targetRootPath + File.separator);
                    String newGroupId = StringUtils.replace(groupId, File.separator, ".");
                    String finalPath = targetRootPath + File.separator + newGroupId + endPath;

                    finalTargetFile = new File(finalPath);
                }


                File targetDirectory = finalTargetFile.getParentFile();
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
}
