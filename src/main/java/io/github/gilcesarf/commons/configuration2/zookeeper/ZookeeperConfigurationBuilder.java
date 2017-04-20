package io.github.gilcesarf.commons.configuration2.zookeeper;

import java.util.Map;

import org.apache.commons.configuration2.builder.BasicConfigurationBuilder;
import org.apache.commons.configuration2.builder.BuilderParameters;
import org.apache.commons.configuration2.ex.ConfigurationException;

public class ZookeeperConfigurationBuilder<T extends ZookeeperConfiguration> extends BasicConfigurationBuilder<T> {

    public ZookeeperConfigurationBuilder(Class<? extends T> resCls, Map<String, Object> params,
            boolean allowFailOnInit) {
        super(resCls, params, allowFailOnInit);
    }

    public ZookeeperConfigurationBuilder(Class<? extends T> resCls, Map<String, Object> params) {
        this(resCls, params, false);
    }

    public ZookeeperConfigurationBuilder(Class<? extends T> resCls) {
        this(resCls, null);
    }

    /**
     * {@inheritDoc} This method is overridden here to change the result type.
     */
    @Override
    public ZookeeperConfigurationBuilder<T> configure(BuilderParameters... params) {
        super.configure(params);
        return this;
    }

    @Override
    public T getConfiguration() throws ConfigurationException {
        T result = super.getConfiguration();
        if (result instanceof ZookeeperConfiguration) {
            try {
                // block until connected
                ZookeeperConfiguration zkConfig = ((ZookeeperConfiguration) result);
                zkConfig.connect();
            } catch (Exception e) {
                throw new ConfigurationException(e);
            }
        }
        return result;
    }

}
