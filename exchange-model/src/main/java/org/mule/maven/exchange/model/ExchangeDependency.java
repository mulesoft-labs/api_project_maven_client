/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */

package org.mule.maven.exchange.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * The Items Schema
 * <p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "groupId",
        "assetId",
        "version"
})
public class ExchangeDependency {

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
     * The Scope of the dependency
     * <p>
     * <p>
     * (not Required, only descriptorVersion: 1.0.0)
     */
    @JsonProperty("scope")
    private String scope = "";
    /**
     * The Classifier of the dependency
     * <p>
     * <p>
     * (not Required, only descriptorVersion: 1.0.0)
     */
    @JsonProperty("classifier")
    private String classifier = "";
    /**
     * The Packaging of the dependency
     * <p>
     * <p>
     * (not Required, only descriptorVersion: 1.0.0)
     */
    @JsonProperty("packaging")
    private String packaging = "";
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

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

    public ExchangeDependency withGroupId(String groupId) {
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

    public ExchangeDependency withAssetId(String assetId) {
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

    public ExchangeDependency withVersion(String version) {
        this.version = version;
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

    public ExchangeDependency withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExchangeDependency that = (ExchangeDependency) o;
        return Objects.equals(groupId, that.groupId) &&
                Objects.equals(assetId, that.assetId) &&
                Objects.equals(version, that.version) &&
                Objects.equals(additionalProperties, that.additionalProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, assetId, version, additionalProperties);
    }

    @Override
    public String toString() {
        return "ExchangeDependency{" +
                "groupId='" + groupId + '\'' +
                ", assetId='" + assetId + '\'' +
                ", version='" + version + '\'' +
                ", scope='" + scope + '\'' +
                ", classifier='" + classifier + '\'' +
                ", packaging='" + packaging + '\'' +
                ", additionalProperties=" + additionalProperties +
                '}';
    }

    @JsonProperty("classifier")
    public String getClassifier() {
        return classifier;
    }

    @JsonProperty("classifier")
    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    @JsonProperty("scope")
    public String getScope() {
        return scope;
    }

    @JsonProperty("scope")
    public void setScope(String scope) {
        this.scope = scope;
    }

    @JsonProperty("packaging")
    public String getPackaging() {
        return packaging;
    }

    @JsonProperty("packaging")
    public void setPackaging(String packaging) {
        this.packaging = packaging;
    }
}
