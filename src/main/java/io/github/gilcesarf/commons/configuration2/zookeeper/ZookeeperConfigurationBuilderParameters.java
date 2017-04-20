package io.github.gilcesarf.commons.configuration2.zookeeper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.configuration2.builder.BuilderParameters;
import org.apache.curator.RetryPolicy;

public class ZookeeperConfigurationBuilderParameters implements BuilderParameters {
    static final String CONNECT_STRING = "connectString";
    static final String RETRY_POLICY = "retryPolicy";
    static final String ROOT_PATH = "rootPath";
    static final String MULTINODE_MAP = "multinodeMap";

    private HashMap<String, Object> parameters = new HashMap<String, Object>();

    @Override
    public final Map<String, Object> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    public final ZookeeperConfigurationBuilderParameters setConnectString(String connectString) {
        this.parameters.put(CONNECT_STRING, connectString);
        return this;
    }

    public final ZookeeperConfigurationBuilderParameters setRetryPolicy(RetryPolicy retryPolicy) {
        this.parameters.put(RETRY_POLICY, retryPolicy);
        return this;
    }

    public final ZookeeperConfigurationBuilderParameters setRootPath(String path) {
        this.parameters.put(ROOT_PATH, path);
        return this;
    }

    public void setMultinodeMap(Map<String, String> multinodeMap) {
        this.parameters.put(MULTINODE_MAP, multinodeMap);
    }

}
