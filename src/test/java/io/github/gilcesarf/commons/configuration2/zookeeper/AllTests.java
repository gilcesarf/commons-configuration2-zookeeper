package io.github.gilcesarf.commons.configuration2.zookeeper;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import io.github.gilcesarf.commons.configuration2.zookeeper.json.ZookeeperConfigurationJsonUtilTest;
import io.github.gilcesarf.commons.configuration2.zookeeper.json.ZookeeperConfigurationNodeAttributesTest;

@RunWith(Suite.class)
@SuiteClasses({
        ZookeeperConfigurationBuilderParametersTest.class, ZookeeperConfigurationTest.class,
        ZookeeperConfigurationNodeAttributesTest.class, ZookeeperConfigurationJsonUtilTest.class
})
public class AllTests {

}
