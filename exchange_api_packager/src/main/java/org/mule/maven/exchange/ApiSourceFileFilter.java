/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.maven.exchange;

import java.io.File;
import java.io.FileFilter;

import static org.mule.maven.exchange.utils.ApiProjectConstants.*;

public class ApiSourceFileFilter implements FileFilter {

    private final File sourceDirectory;
    private final File buildDirectory;

    public ApiSourceFileFilter(File sourceDirectory, File buildDirectory) {
        this.sourceDirectory = sourceDirectory;
        this.buildDirectory = buildDirectory;
    }

    @Override
    public boolean accept(File path) {
        if (path.equals(buildDirectory)) {
            return false;
        } else if (path.getParentFile().equals(sourceDirectory)) {
            boolean ignore = path.getName().equals(EXCHANGE_MODULES) || path.getName().equals(EXCHANGE_MODULES_TMP) || path.getName().equals(".mvn") || path.getName().equals(".apivcs") || path.isHidden();
            return !ignore;
        } else {
            return true;
        }
    }
}
