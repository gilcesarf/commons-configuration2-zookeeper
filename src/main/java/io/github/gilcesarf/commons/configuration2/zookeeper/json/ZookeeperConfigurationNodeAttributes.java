package io.github.gilcesarf.commons.configuration2.zookeeper.json;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ZookeeperConfigurationNodeAttributes {

    @JsonIgnore
    private Map<String, String> attributes = null;

    public ZookeeperConfigurationNodeAttributes() {
        this.attributes = new HashMap<String, String>();
    }

    public ZookeeperConfigurationNodeAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    @JsonIgnore
    public void setAttributes(Map<String, String> properties) {
        this.attributes = properties;
    }

    @JsonAnyGetter
    public Map<String, String> getAttributes() {
        return attributes;
    }

    @JsonAnySetter
    public void setAdditionalAttribute(String name, String value) {
        this.attributes.put(name, value);
    }

    @JsonIgnore
    public String getAttribute(String key) {
        if (attributes == null) {
            return null;
        }
        return attributes.get(key);
    }

    @JsonIgnore
    public String removeAttribute(String key) {
        if (attributes == null) {
            return null;
        }
        return attributes.remove(key);
    }

}
