package io.github.gilcesarf.commons.configuration2.zookeeper.json;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "nodeAttributes", "values"
})
public final class ZookeeperConfigurationNode {

    @JsonProperty("nodeAttributes")
    private ZookeeperConfigurationNodeAttributes nodeAttributes = null;

    @JsonProperty("values")
    private List<String> values = null;

    /**
     * No args constructor for use in serialization
     * 
     */
    public ZookeeperConfigurationNode() {
    }

    /**
     * 
     * @param values
     * @param nodeAttributes
     */
    public ZookeeperConfigurationNode(ZookeeperConfigurationNodeAttributes nodeAttributes, List<String> values) {
        super();
        this.nodeAttributes = nodeAttributes;
        this.values = values;
    }

    @JsonProperty("nodeAttributes")
    public ZookeeperConfigurationNodeAttributes getNodeAttributes() {
        return nodeAttributes;
    }

    @JsonProperty("nodeAttributes")
    public void setAttributes(ZookeeperConfigurationNodeAttributes nodeAttributes) {
        this.nodeAttributes = nodeAttributes;
    }

    @JsonProperty("values")
    public List<String> getValues() {
        return values;
    }

    @JsonProperty("values")
    public void setValues(List<String> values) {
        this.values = values;
    }

}