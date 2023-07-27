/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */

package org.mule.maven.exchange.model;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * The Root Schema
 * <p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "main",
        "name",
        "classifier",
        "tags",
        "groupId",
        "backwardsCompatible",
        "assetId",
        "version",
        "apiVersion",
        "dependencies",
        "metadata"
})
public class ExchangeModel {

    /**
     * The Main Schema
     * <p>
     * <p>
     * (Required)
     */
    @JsonProperty("main")
    private String main = "";
    /**
     * The Name Schema
     * <p>
     * <p>
     * (Required)
     */
    @JsonProperty("name")
    private String name = "";
    /**
     * The Classifier Schema
     * <p>
     * <p>
     * (Required)
     */
    @JsonProperty("classifier")
    private String classifier = "";
    /**
     * The Tags Schema
     * <p>
     * <p>
     * (Required)
     */
    @JsonProperty("tags")
    private List<Object> tags = null;
    /**
     * The Groupid Schema
     * <p>
     * <p>
     * (Required)
     */
    @JsonProperty("groupId")
    private String groupId = "";

    /**
     * The Assetid Schema
     * <p>
     * <p>
     * (Required)
     */
    @JsonProperty("assetId")
    private String assetId = "";
    /**
     * The Version Schema
     * <p>
     * <p>
     * (Required)
     */
    @JsonProperty("version")
    private String version = "";
    /**
     * The Apiversion Schema
     * <p>
     * <p>
     * (Required)
     */
    @JsonProperty("apiVersion")
    private String apiVersion = "";
    /**
     * The Dependencies Schema
     * <p>
     * <p>
     * (Required)
     */
    @JsonProperty("dependencies")
    private List<ExchangeDependency> dependencies = new ArrayList<>();
    /**
     * The ExchangeMetadata Schema
     * <p>
     * <p>
     * (Required)
     */
    @JsonProperty("metadata")
    private ExchangeMetadata metadata;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * The Main Schema
     * <p>
     * <p>
     * (Required)
     */
    @JsonProperty("main")
    public String getMain() {
        return main;
    }

    /**
     * The Main Schema
     * <p>
     * <p>
     * (Required)
     */
    @JsonProperty("main")
    public void setMain(String main) {
        this.main = main;
    }

    public ExchangeModel withMain(String main) {
        this.main = main;
        return this;
    }

    /**
     * The Name Schema
     * <p>
     * <p>
     * (Required)
     */
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    /**
     * The Name Schema
     * <p>
     * <p>
     * (Required)
     */
    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    public ExchangeModel withName(String name) {
        this.name = name;
        return this;
    }

    /**
     * The Classifier Schema
     * <p>
     * <p>
     * (Required)
     */
    @JsonProperty("classifier")
    public String getClassifier() {
        return classifier;
    }

    /**
     * The Classifier Schema
     * <p>
     * <p>
     * (Required)
     */
    @JsonProperty("classifier")
    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    public ExchangeModel withClassifier(String classifier) {
        this.classifier = classifier;
        return this;
    }

    /**
     * The Tags Schema
     * <p>
     * <p>
     * (Required)
     */
    @JsonProperty("tags")
    public List<Object> getTags() {
        return tags;
    }

    /**
     * The Tags Schema
     * <p>
     * <p>
     * (Required)
     */
    @JsonProperty("tags")
    public void setTags(List<Object> tags) {
        this.tags = tags;
    }

    public ExchangeModel withTags(List<Object> tags) {
        this.tags = tags;
        return this;
    }

    /**
     * The Groupid Schema
     * <p>
     * <p>
     * (Required)
     */
    @JsonProperty("groupId")
    public String getGroupId() {
        return groupId;
    }

    /**
     * The Groupid Schema
     * <p>
     * <p>
     * (Required)
     */
    @JsonProperty("groupId")
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public ExchangeModel withGroupId(String groupId) {
        this.groupId = groupId;
        return this;
    }


    /**
     * The Assetid Schema
     * <p>
     * <p>
     * (Required)
     */
    @JsonProperty("assetId")
    public String getAssetId() {
        return assetId;
    }

    /**
     * The Assetid Schema
     * <p>
     * <p>
     * (Required)
     */
    @JsonProperty("assetId")
    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public ExchangeModel withAssetId(String assetId) {
        this.assetId = assetId;
        return this;
    }

    /**
     * The Version Schema
     * <p>
     * <p>
     * (Required)
     */
    @JsonProperty("version")
    public String getVersion() {
        return version;
    }

    /**
     * The Version Schema
     * <p>
     * <p>
     * (Required)
     */
    @JsonProperty("version")
    public void setVersion(String version) {
        this.version = version;
    }

    public ExchangeModel withVersion(String version) {
        this.version = version;
        return this;
    }

    /**
     * The Apiversion Schema
     * <p>
     * <p>
     * (Required)
     */
    @JsonProperty("apiVersion")
    public String getApiVersion() {
        return apiVersion;
    }

    /**
     * The Apiversion Schema
     * <p>
     * <p>
     * (Required)
     */
    @JsonProperty("apiVersion")
    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public ExchangeModel withApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
        return this;
    }

    /**
     * The Dependencies Schema
     * <p>
     * <p>
     * (Required)
     */
    @JsonProperty("dependencies")
    public List<ExchangeDependency> getDependencies() {
        return dependencies;
    }

    /**
     * The Dependencies Schema
     * <p>
     * <p>
     * (Required)
     */
    @JsonProperty("dependencies")
    public void setDependencies(List<ExchangeDependency> dependencies) {
        this.dependencies = dependencies;
    }

    public ExchangeModel withDependencies(List<ExchangeDependency> dependencies) {
        this.dependencies = dependencies;
        return this;
    }

    /**
     * The ExchangeMetadata Schema
     * <p>
     * <p>
     * (Required)
     */
    @JsonProperty("metadata")
    public ExchangeMetadata getMetadata() {
        return metadata;
    }

    /**
     * The ExchangeMetadata Schema
     * <p>
     * <p>
     * (Required)
     */
    @JsonProperty("metadata")
    public void setMetadata(ExchangeMetadata metadata) {
        this.metadata = metadata;
    }

    public ExchangeModel withMetadata(ExchangeMetadata metadata) {
        this.metadata = metadata;
        return this;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public ExchangeModel withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExchangeModel that = (ExchangeModel) o;
        return Objects.equals(main, that.main) &&
                Objects.equals(name, that.name) &&
                Objects.equals(classifier, that.classifier) &&
                Objects.equals(tags, that.tags) &&
                Objects.equals(groupId, that.groupId) &&
                Objects.equals(assetId, that.assetId) &&
                Objects.equals(version, that.version) &&
                Objects.equals(apiVersion, that.apiVersion) &&
                Objects.equals(dependencies, that.dependencies) &&
                Objects.equals(metadata, that.metadata) &&
                Objects.equals(additionalProperties, that.additionalProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(main, name, classifier, tags, groupId, assetId, version, apiVersion, dependencies, metadata, additionalProperties);
    }

    @Override
    public String toString() {
        return "ExchangeModel{" +
                "main='" + main + '\'' +
                ", name='" + name + '\'' +
                ", classifier='" + classifier + '\'' +
                ", tags=" + tags +
                ", groupId='" + groupId + '\'' +
                ", assetId='" + assetId + '\'' +
                ", version='" + version + '\'' +
                ", apiVersion='" + apiVersion + '\'' +
                ", dependencies=" + dependencies +
                ", metadata=" + metadata +
                ", additionalProperties=" + additionalProperties +
                '}';
    }
}
