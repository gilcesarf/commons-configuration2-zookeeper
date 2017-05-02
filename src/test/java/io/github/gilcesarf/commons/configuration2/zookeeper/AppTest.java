package io.github.gilcesarf.commons.configuration2.zookeeper;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.curator.retry.BoundedExponentialBackoffRetry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AppTest {
    private static final Logger LOG = LogManager.getLogger();

    public static void main(String[] args) throws ConfigurationException {
        // XMLConfiguration xmlConfig = processConfigFilename("config-atalla-monitor.xml");
        XMLConfiguration xmlConfig = processConfigFilename("test.xml");
        ZookeeperConfiguration zkConfig = processZooKeeper();
        zkConfig.copyConfigurationFrom(xmlConfig);
        zkConfig.persist();
        zkConfig.createAndShowMultinodeGUI();
        zkConfig.createAndShowConfigurationDataGUI();
    }

    private static ZookeeperConfiguration processZooKeeper() throws ConfigurationException {
        ZookeeperConfigurationBuilderParameters params = new ZookeeperConfigurationBuilderParameters();
        params.setConnectString("localhost:2181/commons-config");
        params.setRetryPolicy(new BoundedExponentialBackoffRetry(100, 2000, 3));
        params.setRootPath("/config");
        Map<String, String> multinodeMap = new HashMap<String, String>();
        // multinodeMap.put("/atalla-monitor/rules/rule", "");
        // multinodeMap.put("/atalla-monitor/endpoints/endpoint", "");
        multinodeMap.put("/testconfig/test", "");
        multinodeMap.put("/testconfig/list", "");
        multinodeMap.put("/testconfig/list/item", "");
        multinodeMap.put("/testconfig/list/sublist/item", "");
        multinodeMap.put("/testconfig/clear/list", "");
        multinodeMap.put("/testconfig/clear/list/item", "");
        params.setMultinodeMap(multinodeMap);
        ZookeeperConfigurationBuilder<ZookeeperConfiguration> builder =
                new ZookeeperConfigurationBuilder<ZookeeperConfiguration>(ZookeeperConfiguration.class);
        builder.configure(params);
        ZookeeperConfiguration config = null;
        config = builder.getConfiguration();
        return config;
    }

    private static XMLConfiguration processConfigFilename(String configFilename) throws ConfigurationException {
        Configurations configs = new Configurations();
        XMLConfiguration config = null;
        try {
            config = configs.xml(configFilename);
            config.setThrowExceptionOnMissing(true);
        } catch (ConfigurationException cex) {
            LOG.info(cex.getMessage());
            LOG.catching(cex);
            throw cex;
        }
        return config;
    }

}
