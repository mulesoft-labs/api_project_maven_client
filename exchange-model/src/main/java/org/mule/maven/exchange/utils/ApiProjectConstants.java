/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.maven.exchange.utils;

import java.io.File;

public class ApiProjectConstants {

    public static final String EXCHANGE_MODULES = "exchange_modules";
    public static final String EXCHANGE_MODULES_TMP = ".exchange_modules_tmp";
    public static final String API_EXPANDED = "full_api";
    public static final String EXCHANGE_MODULES_REPOSITORY = "exchange_modules_repository";

    public static final String MAVEN_SKIP_REST_CONNECT = "exchange.maven.restConnect.skip";
    public static final String MAVEN_SKIP_VALIDATE_API = "exchange.maven.validateApi.skip";
    public static final String REST_CONNECT_OUTPUTDIR = "rest_connect_workdir";

    public static File getFatApiDirectory(File buildDirectory) {
        return new File(buildDirectory, API_EXPANDED);
    }
}
