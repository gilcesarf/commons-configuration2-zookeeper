package io.github.gilcesarf.commons.configuration2.zookeeper;

import static org.junit.Assert.*;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.curator.RetryPolicy;
import org.apache.curator.RetrySleeper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ZookeeperConfigurationBuilderParametersTest {
    private ZookeeperConfigurationBuilderParameters param = null;

    @Test
    public void testConnectString() {
        assertTrue(param.getParameters().size() == 0);
        String connStr = "foo";
        param.setConnectString(connStr);
        Map<String, Object> map = param.getParameters();
        assertTrue(map.size() == 1);
        for (Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            assertTrue(value instanceof String);
            assertEquals(connStr, value);
            assertEquals(key, ZookeeperConfigurationBuilderParameters.CONNECT_STRING);
        }
    }

    @Test
    public void testRetryPolicy() {
        assertTrue(param.getParameters().size() == 0);
        RetryPolicy policy = new RetryPolicy() {
            @Override
            public boolean allowRetry(int retryCount, long elapsedTimeMs, RetrySleeper sleeper) {
                return false;
            }
        };
        param.setRetryPolicy(policy);
        Map<String, Object> map = param.getParameters();
        assertTrue(map.size() == 1);
        for (Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            assertTrue(value instanceof RetryPolicy);
            assertEquals(policy, value);
            assertEquals(key, ZookeeperConfigurationBuilderParameters.RETRY_POLICY);
        }
    }

    @Test
    public void testRootPath() {
        assertTrue(param.getParameters().size() == 0);
        String rootPath = "foo";
        param.setRootPath(rootPath);
        Map<String, Object> map = param.getParameters();
        assertTrue(map.size() == 1);
        for (Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            assertTrue(value instanceof String);
            assertEquals(rootPath, value);
            assertEquals(key, ZookeeperConfigurationBuilderParameters.ROOT_PATH);
        }
    }

    @Before
    public void setUp() throws Exception {
        param = new ZookeeperConfigurationBuilderParameters();
    }

    @After
    public void tearDown() throws Exception {
        param = null;
    }

}
