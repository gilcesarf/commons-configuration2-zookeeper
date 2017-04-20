package io.github.gilcesarf.commons.configuration2.zookeeper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.io.FileHandler;
import org.apache.curator.retry.BoundedExponentialBackoffRetry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ZookeeperConfigurationTest {
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

    private final String testProperties = getTestFile("test.xml").getAbsolutePath();

    public static final String TEST_DIR_NAME = "target/test-classes";

    /** The directory with the test files. */
    public static final File TEST_DIR = new File(TEST_DIR_NAME);

    public static File getTestFile(String name) {
        return new File(TEST_DIR, name);
    }

    // test attributes

    private ZookeeperConfiguration conf = null;

    @Before
    public void setUp() throws Exception {
        XMLConfiguration xmlConfig = createFromFile(testProperties);
        conf = createZookeeperConfiguration();
        conf.copyConfigurationFrom(xmlConfig);
    }

    @After
    public void tearDown() throws Exception {
        if (conf != null) {
            conf.close();
        }
    }

    private static ZookeeperConfiguration createZookeeperConfiguration() throws ConfigurationException {
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
