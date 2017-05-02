package io.github.gilcesarf.commons.configuration2.zookeeper;

import static org.junit.Assert.*;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.io.FileHandler;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.BoundedExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ZookeeperConfigurationTest extends AbstractZookeeperTest {

    private static final String ROOT_PATH = "/config";

    private static final String BASE_PATH = "/commons-config";

    // Test Data
    private String[] tests = {
            "/config/atalla-monitor/rules/rule0000000001", "rule0000000001", "ABC123"
    };

    private String[][] groups = {
            {
                    "/config/atalla-monitor/rules/rule", "0000000001"
            }, {
                    "rule", "0000000001"
            }, {}
    };

    private boolean[] results = {
            true, true, false
    };

    private int[] count = {
            2, 2, 0
    };

    String[][] pathsAndData = {
            {
                    "/config", ""
            }, {
                    "/config/atalla-monitor", "{\"nodeAttributes\":{\"id\":\"1\"}}"
            }, {
                    "/config/atalla-monitor/endpoints", "{}"
            }, {
                    "/config/atalla-monitor/endpoints/endpoint0000000000",
                    "{\"nodeAttributes\":{\"address\":\"190.146.154.194\",\"management_port\":\"7005\",\"commandList\":\"98,99\",\"interval\":\"60000\",\"id\":\"1\",\"ascii_port\":\"7000\"}}"
            }, {
                    "/config/atalla-monitor/rules", "{}"
            }, {
                    "/config/atalla-monitor/rules/rule0000000002",
                    "{\"nodeAttributes\":{\"endpointId\":\"1\",\"id\":\"3\",\"type\":\"BATTERY_LIFE\"}}"
            }, {
                    "/config/atalla-monitor/rules/rule0000000002/config", "{}"
            }, {
                    "/config/atalla-monitor/rules/rule0000000002/config/threshold.medium", "{\"values\":[\"60\"]}"
            }, {
                    "/config/atalla-monitor/rules/rule0000000002/config/threshold.high", "{\"values\":[\"30\"]}"
            }, {
                    "/config/atalla-monitor/rules/rule0000000002/config/threshold.low", "{\"values\":[\"90\"]}"
            }, {
                    "/config/atalla-monitor/rules/rule0000000001",
                    "{\"nodeAttributes\":{\"endpointId\":\"1\",\"id\":\"2\",\"type\":\"SOCKET_INFO\"}}"
            }, {
                    "/config/atalla-monitor/rules/rule0000000001/config", "{}"
            }, {
                    "/config/atalla-monitor/rules/rule0000000001/config/threshold.medium", "{\"values\":[\"2\"]}"
            }, {
                    "/config/atalla-monitor/rules/rule0000000001/config/threshold.high", "{\"values\":[\"0\"]}"
            }, {
                    "/config/atalla-monitor/rules/rule0000000001/config/threshold.low", "{\"values\":[\"4\"]}"
            }, {
                    "/config/atalla-monitor/rules/rule0000000001/config/port", "{\"values\":[\"7000\"]}"
            }, {
                    "/config/atalla-monitor/rules/rule0000000000",
                    "{\"nodeAttributes\":{\"endpointId\":\"1\",\"id\":\"1\",\"type\":\"CPU\"}}"
            }, {
                    "/config/atalla-monitor/rules/rule0000000000/config", "{}"
            }, {
                    "/config/atalla-monitor/rules/rule0000000000/config/threshold.medium", "{\"values\":[\"80\"]}"
            }, {
                    "/config/atalla-monitor/rules/rule0000000000/config/threshold.high", "{\"values\":[\"90\"]}"
            }, {
                    "/config/atalla-monitor/rules/rule0000000000/config/threshold.low", "{\"values\":[\"70\"]}"
            }, {
                    "/config/atalla-monitor/rules/rule0000000000/config/windows.size.ms", "{\"values\":[\"300000\"]}"
            }, {
                    "/config/atalla-monitor/broker-properties", "{}"
            }, {
                    "/config/atalla-monitor/broker-properties/retries", "{\"values\":[\"10\"]}"
            }, {
                    "/config/atalla-monitor/broker-properties/zookeeper.servers", "{\"values\":[\"localhost:2181\"]}"
            }, {
                    "/config/atalla-monitor/broker-properties/acks", "{\"values\":[\"all\"]}"
            }, {
                    "/config/atalla-monitor/broker-properties/batch.size", "{\"values\":[\"16384\"]}"
            }, {
                    "/config/atalla-monitor/broker-properties/reconnect.backoff.ms", "{\"values\":[\"30000\"]}"
            }, {
                    "/config/atalla-monitor/broker-properties/bootstrap.servers", "{\"values\":[\"localhost:9099\"]}"
            }, {
                    "/config/atalla-monitor/broker-properties/buffer.memory", "{\"values\":[\"33554432\"]}"
            }, {
                    "/config/atalla-monitor/broker-properties/linger.ms", "{\"values\":[\"1\"]}"
            }
    };

    private ZookeeperConfiguration.MultinodeData[] multinodeTestData = {
            new ZookeeperConfiguration.MultinodeData("/config/testconfig", false),
            new ZookeeperConfiguration.MultinodeData("/config/testconfig/element", false),
            new ZookeeperConfiguration.MultinodeData("/config/testconfig/element2", false),
            new ZookeeperConfiguration.MultinodeData("/config/testconfig/element2/subelement", false),
            new ZookeeperConfiguration.MultinodeData("/config/testconfig/element2/subelement/subsubelement", false),
            new ZookeeperConfiguration.MultinodeData("/config/testconfig/element3", false),
            new ZookeeperConfiguration.MultinodeData("/config/testconfig/test0000000003", true),
            new ZookeeperConfiguration.MultinodeData("/config/testconfig/test0000000003/comment", false),
            new ZookeeperConfiguration.MultinodeData("/config/testconfig/test0000000003/entity", false),
            new ZookeeperConfiguration.MultinodeData("/config/testconfig/test0000000003/cdata", false),
            new ZookeeperConfiguration.MultinodeData("/config/testconfig/mean", false),
            new ZookeeperConfiguration.MultinodeData("/config/testconfig/mean/submean", false),
            new ZookeeperConfiguration.MultinodeData("/config/testconfig/test0000000005", true),
            new ZookeeperConfiguration.MultinodeData("/config/testconfig/test0000000005/short", false),
            new ZookeeperConfiguration.MultinodeData("/config/testconfig/list0000000006", true),
            new ZookeeperConfiguration.MultinodeData("/config/testconfig/list0000000006/item", true),
            new ZookeeperConfiguration.MultinodeData("/config/testconfig/clear", false),
            new ZookeeperConfiguration.MultinodeData("/config/testconfig/clear/comment", false),
            new ZookeeperConfiguration.MultinodeData("/config/testconfig/clear/list0000000004", true),
            new ZookeeperConfiguration.MultinodeData("/config/testconfig/clear/list0000000004/item0000000000", true),
            new ZookeeperConfiguration.MultinodeData("/config/testconfig/clear/list0000000005", true),
            new ZookeeperConfiguration.MultinodeData("/config/testconfig/clear/list0000000005/item0000000000", true),
            new ZookeeperConfiguration.MultinodeData("/config/testconfig/clear/element2", false),
            new ZookeeperConfiguration.MultinodeData("/config/testconfig/clear/element", false),
            new ZookeeperConfiguration.MultinodeData("/config/testconfig/clear/cdata", false),
            new ZookeeperConfiguration.MultinodeData("/config/testconfig/expressions", false),
            new ZookeeperConfiguration.MultinodeData("/config/testconfig/attrList", false),
            new ZookeeperConfiguration.MultinodeData("/config/testconfig/attrList/a", false),
            new ZookeeperConfiguration.MultinodeData("/config/testconfig/space", false),
            new ZookeeperConfiguration.MultinodeData("/config/testconfig/space/description", false),
            new ZookeeperConfiguration.MultinodeData("/config/testconfig/space/blanc", false),
            new ZookeeperConfiguration.MultinodeData("/config/testconfig/space/stars", false),
            new ZookeeperConfiguration.MultinodeData("/config/testconfig/space/testInvalid", false),
            new ZookeeperConfiguration.MultinodeData("/config/testconfig/spaceElement", false),
            new ZookeeperConfiguration.MultinodeData("/config/testconfig/empty", false),
            new ZookeeperConfiguration.MultinodeData("/config/testconfig/split", false),
            new ZookeeperConfiguration.MultinodeData("/config/testconfig/split/list1", false),
            new ZookeeperConfiguration.MultinodeData("/config/testconfig/split/list4", false),
            new ZookeeperConfiguration.MultinodeData("/config/testconfig/split/list3", false),
            new ZookeeperConfiguration.MultinodeData("/config/testconfig/split/list2", false),
            new ZookeeperConfiguration.MultinodeData("/config/testconfig/list0000000007", true),
            new ZookeeperConfiguration.MultinodeData("/config/testconfig/list0000000007/item", true),
            new ZookeeperConfiguration.MultinodeData("/config/testconfig/list0000000007/sublist", false),
            new ZookeeperConfiguration.MultinodeData("/config/testconfig/list0000000007/sublist/item", true),
            new ZookeeperConfiguration.MultinodeData("/config/testconfig/complexNames", false),
            new ZookeeperConfiguration.MultinodeData("/config/testconfig/complexNames/my.elem", false),
            new ZookeeperConfiguration.MultinodeData("/config/testconfig/complexNames/my.elem/sub.elem", false)
    };

    private ZookeeperConfiguration.MultinodeData[] multinodeTestData2 = {
            new ZookeeperConfiguration.MultinodeData("/config/atalla-monitor", false),
            new ZookeeperConfiguration.MultinodeData("/config/atalla-monitor/broker-properties", false),
            new ZookeeperConfiguration.MultinodeData("/config/atalla-monitor/broker-properties/bootstrap.servers",
                    false),
            new ZookeeperConfiguration.MultinodeData("/config/atalla-monitor/broker-properties/zookeeper.servers",
                    false),
            new ZookeeperConfiguration.MultinodeData("/config/atalla-monitor/broker-properties/acks", false),
            new ZookeeperConfiguration.MultinodeData("/config/atalla-monitor/broker-properties/retries", false),
            new ZookeeperConfiguration.MultinodeData("/config/atalla-monitor/broker-properties/batch.size", false),
            new ZookeeperConfiguration.MultinodeData("/config/atalla-monitor/broker-properties/linger.ms", false),
            new ZookeeperConfiguration.MultinodeData("/config/atalla-monitor/broker-properties/reconnect.backoff.ms",
                    false),
            new ZookeeperConfiguration.MultinodeData("/config/atalla-monitor/broker-properties/buffer.memory", false),
            new ZookeeperConfiguration.MultinodeData("/config/atalla-monitor/endpoints", false),
            new ZookeeperConfiguration.MultinodeData("/config/atalla-monitor/endpoints/endpoint0000000000", true),
            new ZookeeperConfiguration.MultinodeData("/config/atalla-monitor/rules", false),
            new ZookeeperConfiguration.MultinodeData("/config/atalla-monitor/rules/rule0000000000", true),
            new ZookeeperConfiguration.MultinodeData("/config/atalla-monitor/rules/rule0000000000/config", false),
            new ZookeeperConfiguration.MultinodeData(
                    "/config/atalla-monitor/rules/rule0000000000/config/windows.size.ms", false),
            new ZookeeperConfiguration.MultinodeData(
                    "/config/atalla-monitor/rules/rule0000000000/config/threshold.high", false),
            new ZookeeperConfiguration.MultinodeData(
                    "/config/atalla-monitor/rules/rule0000000000/config/threshold.medium", false),
            new ZookeeperConfiguration.MultinodeData("/config/atalla-monitor/rules/rule0000000000/config/threshold.low",
                    false),
            new ZookeeperConfiguration.MultinodeData("/config/atalla-monitor/rules/rule0000000001", true),
            new ZookeeperConfiguration.MultinodeData("/config/atalla-monitor/rules/rule0000000001/config", false),
            new ZookeeperConfiguration.MultinodeData("/config/atalla-monitor/rules/rule0000000001/config/port", false),
            new ZookeeperConfiguration.MultinodeData(
                    "/config/atalla-monitor/rules/rule0000000001/config/threshold.high", false),
            new ZookeeperConfiguration.MultinodeData(
                    "/config/atalla-monitor/rules/rule0000000001/config/threshold.medium", false),
            new ZookeeperConfiguration.MultinodeData("/config/atalla-monitor/rules/rule0000000001/config/threshold.low",
                    false),
            new ZookeeperConfiguration.MultinodeData("/config/atalla-monitor/rules/rule0000000002", true),
            new ZookeeperConfiguration.MultinodeData("/config/atalla-monitor/rules/rule0000000002/config", false),
            new ZookeeperConfiguration.MultinodeData(
                    "/config/atalla-monitor/rules/rule0000000002/config/threshold.high", false),
            new ZookeeperConfiguration.MultinodeData(
                    "/config/atalla-monitor/rules/rule0000000002/config/threshold.medium", false),
            new ZookeeperConfiguration.MultinodeData("/config/atalla-monitor/rules/rule0000000002/config/threshold.low",
                    false)
    };

    // auxiliary variables
    private int currentPortIndex = 0;

    private boolean initialized = false;

    private final String testProperties = getTestFile("test.xml").getAbsolutePath();

    /** The directory with the test files. */
    public static final String TEST_DIR_NAME = "target/test-classes";
    public static final File TEST_DIR = new File(TEST_DIR_NAME);

    public static File getTestFile(String name) {
        return new File(TEST_DIR, name);
    }

    // test attributes

    private ZookeeperConfiguration conf = null;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        this.initialized = false;
        XMLConfiguration xmlConfig = createFromFile(testProperties);
        conf = createZookeeperConfiguration(null);
        conf.connect();
        conf.copyConfigurationFrom(xmlConfig);
    }

    @After
    public void tearDown() throws Exception {
        if (conf != null) {
            conf.close();
        }
        super.tearDown();
    }

    private ZookeeperConfiguration createZookeeperConfiguration(Map<String, String> multinodeMap)
            throws ConfigurationException {
        if (!initialized) {
            try (CuratorFramework curator = CuratorFrameworkFactory.newClient("localhost:" + port[currentPortIndex],
                    new BoundedExponentialBackoffRetry(100, 3000, 10))) {
                curator.start();
                String result = curator.create().withMode(CreateMode.PERSISTENT).forPath(BASE_PATH, null);
                assertEquals(BASE_PATH, result);
                result = curator.create().withMode(CreateMode.PERSISTENT).forPath(BASE_PATH + ROOT_PATH, null);
                assertEquals(BASE_PATH + ROOT_PATH, result);
            } catch (Exception e) {
                fail(e.getMessage());
            }
            this.initialized = true;
        }
        ZookeeperConfigurationBuilderParameters params = new ZookeeperConfigurationBuilderParameters();
        params.setConnectString("localhost:" + port[currentPortIndex++] + BASE_PATH);
        if (currentPortIndex >= port.length) {
            currentPortIndex = 0;
        }
        params.setRetryPolicy(new BoundedExponentialBackoffRetry(100, 2000, 12));
        params.setRootPath(ROOT_PATH);
        if (multinodeMap == null) {
            multinodeMap = new HashMap<String, String>();
            multinodeMap.put("/testconfig/test", "");
            multinodeMap.put("/testconfig/list", "");
            multinodeMap.put("/testconfig/list/item", "");
            multinodeMap.put("/testconfig/list/sublist/item", "");
            multinodeMap.put("/testconfig/clear/list", "");
            multinodeMap.put("/testconfig/clear/list/item", "");
        }
        params.setMultinodeMap(multinodeMap);
        ZookeeperConfigurationBuilder<ZookeeperConfiguration> builder =
                new ZookeeperConfigurationBuilder<ZookeeperConfiguration>(ZookeeperConfiguration.class);
        builder.configure(params);
        ZookeeperConfiguration config = null;
        config = builder.getConfiguration();
        return config;
    }

    /**
     * Creates a new XMLConfiguration and loads the specified file.
     *
     * @param fileName
     *            the name of the file to be loaded
     * @return the newly created configuration instance
     * @throws ConfigurationException
     *             if an error occurs
     */
    private static XMLConfiguration createFromFile(String fileName) throws ConfigurationException {
        XMLConfiguration config = new XMLConfiguration();
        config.setListDelimiterHandler(new DefaultListDelimiterHandler(','));
        load(config, fileName);
        return config;
    }

    /**
     * Helper method for loading the specified configuration file.
     *
     * @param config
     *            the configuration
     * @param fileName
     *            the name of the file to be loaded
     * @throws ConfigurationException
     *             if an error occurs
     */
    private static void load(XMLConfiguration config, String fileName) throws ConfigurationException {
        FileHandler handler = new FileHandler(config);
        handler.setFileName(fileName);
        handler.load();
    }

    @Test
    public void testIsMultinode() throws Exception {
        for (ZookeeperConfiguration.MultinodeData testData : multinodeTestData) {
            String msg = "Path: " + testData.getPath() + "\tExpected: " + testData.isMultinode() + "\tGot: "
                         + conf.isMultinode(testData.getPath(), false);
            assertTrue(msg, testData.isMultinode() == conf.isMultinode(testData.getPath(), false));
        }
        conf.close();
        conf = null;

        String testProperties = getTestFile("config-atalla-monitor.xml").getAbsolutePath();
        XMLConfiguration xmlConfig = createFromFile(testProperties);

        Map<String, String> multinodeMap = new HashMap<String, String>();
        multinodeMap.put("/atalla-monitor/rules/rule", "");
        multinodeMap.put("/atalla-monitor/endpoints/endpoint", "");
        conf = createZookeeperConfiguration(multinodeMap);
        conf.connect();
        conf.copyConfigurationFrom(xmlConfig);
        for (ZookeeperConfiguration.MultinodeData testData : multinodeTestData2) {
            String msg = "Path: " + testData.getPath() + "\tExpected: " + testData.isMultinode() + "\tGot: "
                         + conf.isMultinode(testData.getPath(), false);
            assertTrue(msg, testData.isMultinode() == conf.isMultinode(testData.getPath(), false));
        }
    }

    @Test
    public void testPersist() throws Exception {
        conf.close();
        conf = null;

        String testProperties = getTestFile("config-atalla-monitor.xml").getAbsolutePath();
        XMLConfiguration xmlConfig = createFromFile(testProperties);
        Map<String, String> multinodeMap = new HashMap<String, String>();
        multinodeMap.put("/atalla-monitor/rules/rule", "");
        multinodeMap.put("/atalla-monitor/endpoints/endpoint", "");
        conf = createZookeeperConfiguration(multinodeMap);
        conf.connect();
        CuratorFramework curator = conf.getCuratorFramework();
        conf.copyConfigurationFrom(xmlConfig);
        assertTrue(conf.persist());
        for (int i = 0; i < pathsAndData.length; i++) {
            String path = pathsAndData[i][0];
            String testData = pathsAndData[i][1];
            try {
                byte[] dataAsByteArray = curator.getData().forPath(path);
                if (dataAsByteArray != null) {
                    String data = new String(dataAsByteArray);
                    assertEquals(data, testData);
                } else {
                    assertTrue(testData == null || "".equals(testData));
                }
            } catch (Exception e) {
                fail(e.getMessage());
            }
        }
    }

    @Test
    public void testPatternMatch() {
        for (int i = 0; i < tests.length; i++) {
            Matcher matcher = ZookeeperConfiguration.ZK_SEQ_NAME_PATTERN.matcher(tests[i]);
            assertTrue(matcher.matches() == results[i]);
            if (matcher.matches()) {
                assertTrue(matcher.groupCount() == count[i]);
                for (int j = 0; j < count[i]; j++) {
                    assertEquals(groups[i][j], matcher.group(j + 1));
                }
            }
        }
    }

    @Test
    public void testGetProperty() {
        assertEquals("value", conf.getProperty("element"));
    }

    @Test
    public void testGetCommentedProperty() {
        assertEquals("", conf.getProperty("test.comment"));
    }

    @Test
    public void testGetPropertyWithXMLEntity() {
        assertEquals("1<2", conf.getProperty("test.entity"));
    }

    @Test
    public void testClearPropertyNotExisting() {
        String key = "clearly";
        conf.clearProperty(key);
        assertNull(key, conf.getProperty(key));
        assertNull(key, conf.getProperty(key));
    }

    @Test
    public void testClearPropertySingleElement() {
        String key = "clear.element";
        conf.clearProperty(key);
        assertNull(key, conf.getProperty(key));
        assertNull(key, conf.getProperty(key));
    }

    @Test
    public void testClearPropertySingleElementWithAttribute() {
        String key = "clear.element2";
        conf.clearProperty(key);
        assertNull(key, conf.getProperty(key));
        assertNull(key, conf.getProperty(key));
        key = "clear.element2[@id]";
        assertNotNull(key, conf.getProperty(key));
        assertNotNull(key, conf.getProperty(key));
    }

    @Test
    public void testClearPropertyNonText() {
        String key = "clear.comment";
        conf.clearProperty(key);
        assertNull(key, conf.getProperty(key));
        assertNull(key, conf.getProperty(key));
    }

    @Test
    public void testClearPropertyCData() {
        String key = "clear.cdata";
        conf.clearProperty(key);
        assertNull(key, conf.getProperty(key));
        assertNull(key, conf.getProperty(key));
    }

    @Test
    public void testClearPropertyMultipleSiblings() {
        String key = "clear.list.item";
        conf.clearProperty(key);
        assertNull(key, conf.getProperty(key));
        assertNull(key, conf.getProperty(key));
        key = "clear.list.item[@id]";
        assertNotNull(key, conf.getProperty(key));
        assertNotNull(key, conf.getProperty(key));
    }

    @Test
    public void testClearPropertyMultipleDisjoined() throws Exception {
        String key = "list.item";
        conf.clearProperty(key);
        assertNull(key, conf.getProperty(key));
        assertNull(key, conf.getProperty(key));
    }

    @Test
    public void testgetProperty() {
        // test non-leaf element
        Object property = conf.getProperty("clear");
        assertNull(property);

        // test non-existent element
        property = conf.getProperty("e");
        assertNull(property);

        // test non-existent element
        property = conf.getProperty("element3[@n]");
        assertNull(property);

        // test single element
        property = conf.getProperty("element");
        assertNotNull(property);
        assertTrue(property instanceof String);
        assertEquals("value", property);

        // test single attribute
        property = conf.getProperty("element3[@name]");
        assertNotNull(property);
        assertTrue(property instanceof String);
        assertEquals("foo", property);

        // test non-text/cdata element
        property = conf.getProperty("test.comment");
        assertEquals("", property);

        // test cdata element
        property = conf.getProperty("test.cdata");
        assertNotNull(property);
        assertTrue(property instanceof String);
        assertEquals("<cdata value>", property);

        // test multiple sibling elements
        property = conf.getProperty("list.sublist.item");
        assertNotNull(property);
        assertTrue(property instanceof List);
        List<?> list = (List<?>) property;
        assertEquals(2, list.size());
        assertEquals("five", list.get(0));
        assertEquals("six", list.get(1));

        // test multiple, disjoined elements
        property = conf.getProperty("list.item");
        assertNotNull(property);
        assertTrue(property instanceof List);
        list = (List<?>) property;
        assertEquals(4, list.size());
        assertEquals("one", list.get(0));
        assertEquals("two", list.get(1));
        assertEquals("three", list.get(2));
        assertEquals("four", list.get(3));

        // test multiple, disjoined attributes
        property = conf.getProperty("list.item[@name]");
        assertNotNull(property);
        assertTrue(property instanceof List);
        list = (List<?>) property;
        assertEquals(2, list.size());
        assertEquals("one", list.get(0));
        assertEquals("three", list.get(1));
    }

    @Test
    public void testGetAttribute() {
        assertEquals("element3[@name]", "foo", conf.getProperty("element3[@name]"));
    }

    @Test
    public void testClearAttributeNonExisting() {
        String key = "clear[@id]";
        conf.clearProperty(key);
        assertNull(key, conf.getProperty(key));
        assertNull(key, conf.getProperty(key));
    }

    @Test
    public void testClearAttributeSingle() {
        String key = "clear.element2[@id]";
        conf.clearProperty(key);
        assertNull(key, conf.getProperty(key));
        assertNull(key, conf.getProperty(key));
        key = "clear.element2";
        assertNotNull(key, conf.getProperty(key));
        assertNotNull(key, conf.getProperty(key));
    }

    @Test
    public void testClearAttributeMultipleDisjoined() throws Exception {
        String key = "clear.list.item[@id]";
        conf.clearProperty(key);
        assertNull(key, conf.getProperty(key));
        assertNull(key, conf.getProperty(key));
        key = "clear.list.item";
        assertNotNull(key, conf.getProperty(key));
        assertNotNull(key, conf.getProperty(key));
    }

    @Test
    public void testSetAttribute() {
        // replace an existing attribute
        conf.setProperty("element3[@name]", "bar");
        assertEquals("element3[@name]", "bar", conf.getProperty("element3[@name]"));

        // set a new attribute
        conf.setProperty("foo[@bar]", "value");
        assertEquals("foo[@bar]", "value", conf.getProperty("foo[@bar]"));

        conf.setProperty("name1", "value1");
        assertEquals("value1", conf.getProperty("name1"));
    }

    /**
     * Tests whether an attribute value can be overridden.
     */
    @Test
    public void testOverrideAttribute() {
        conf.addProperty("element3[@name]", "bar");

        List<Object> list = conf.getList("element3[@name]");
        assertNotNull("null list", list);
        assertTrue("'bar' element missing", list.contains("bar"));
        assertEquals("list size", 1, list.size());
    }

    @Test
    public void testAddObjectAttribute() {
        conf.addProperty("test.boolean[@value]", Boolean.TRUE);
        assertTrue("test.boolean[@value]", conf.getBoolean("test.boolean[@value]"));
    }

    /**
     * Tests setting an attribute on the root element.
     */
    @Test
    public void testSetRootAttribute() throws ConfigurationException {
        // conf.setProperty("[@test]", "true");
        // assertEquals("Root attribute not set", "true", conf.getString("[@test]"));
        // saveTestConfig();
        // XMLConfiguration checkConf = checkSavedConfig();
        // assertTrue("Attribute not found after save", checkConf.containsKey("[@test]"));
        // checkConf.setProperty("[@test]", "newValue");
        // conf = checkConf;
        // saveTestConfig();
        // checkConf = checkSavedConfig();
        // assertEquals("Attribute not modified after save", "newValue", checkConf.getString("[@test]"));
    }

    @Test
    public void testAddList() {
        conf.addProperty("test.array", "value1");
        conf.addProperty("test.array", "value2");

        List<Object> list = conf.getList("test.array");
        assertNotNull("null list", list);
        assertTrue("'value1' element missing", list.contains("value1"));
        assertTrue("'value2' element missing", list.contains("value2"));
        assertEquals("list size", 2, list.size());
    }

    @Test
    public void testGetComplexProperty() {
        assertEquals("I'm complex!", conf.getProperty("element2.subelement.subsubelement"));
    }

}
