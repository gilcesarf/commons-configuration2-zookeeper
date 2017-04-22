package io.github.gilcesarf.commons.configuration2.zookeeper.json;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class ZookeeperConfigurationJsonUtilTest {

    private String node1 =
            "{\"nodeAttributes\":{\"address\":\"2\",\"management_port\":\"4\",\"interval\":\"5\",\"commandList\":\"6\",\"id\":\"1\",\"ascii_port\":\"3\"},\"values\":[\"val1\",\"val2\"]}";
    private String node2 =
            "{\"nodeAttributes\":{\"address\":\"12\",\"management_port\":\"14\",\"interval\":\"15\",\"commandList\":\"16\",\"id\":\"11\",\"ascii_port\":\"13\"},\"values\":[\"val4\",\"val5\"]}";

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testSerialize() throws IOException {
        ZookeeperConfigurationNode node = createNode1();
        String json = ZookeeperConfigurationJsonUtil.toJsonString(node);
        assertEquals(json, node1);
        node = createNode2();
        json = ZookeeperConfigurationJsonUtil.toJsonString(node);
        assertEquals(json, node2);
    }

    private ZookeeperConfigurationNode createNode2() {
        ZookeeperConfigurationNode node;
        ZookeeperConfigurationNodeAttributes attr;
        Map<String, String> map;
        List<String> values;
        node = new ZookeeperConfigurationNode();
        attr = new ZookeeperConfigurationNodeAttributes();
        map = attr.getAttributes();
        map.put("id", "11");
        map.put("address", "12");
        map.put("ascii_port", "13");
        map.put("management_port", "14");
        map.put("interval", "15");
        map.put("commandList", "16");
        node.setAttributes(attr);
        values = new ArrayList<String>();
        values.add("val4");
        values.add("val5");
        node.setValues(values);
        return node;
    }

    private ZookeeperConfigurationNode createNode1() {
        ZookeeperConfigurationNode node = new ZookeeperConfigurationNode();
        ZookeeperConfigurationNodeAttributes attr = new ZookeeperConfigurationNodeAttributes();
        Map<String, String> map = attr.getAttributes();
        map.put("id", "1");
        map.put("address", "2");
        map.put("ascii_port", "3");
        map.put("management_port", "4");
        map.put("interval", "5");
        map.put("commandList", "6");
        node.setAttributes(attr);
        List<String> values = new ArrayList<String>();
        values.add("val1");
        values.add("val2");
        node.setValues(values);
        return node;
    }

    @Test
    public void testDeserialize() throws IOException {
        String json = node1;
        ZookeeperConfigurationNode n1 =
                ZookeeperConfigurationJsonUtil.fromJsonString(json, ZookeeperConfigurationNode.class);

        assertTrue(ZookeeperConfigurationJsonUtil.fromPojoToObjectNode(createNode1())
                .equals(ZookeeperConfigurationJsonUtil.fromPojoToObjectNode(n1)));
        json = node2;

        ZookeeperConfigurationNode n2 =
                ZookeeperConfigurationJsonUtil.fromJsonString(json, ZookeeperConfigurationNode.class);

        assertTrue(ZookeeperConfigurationJsonUtil.fromPojoToObjectNode(createNode2())
                .equals(ZookeeperConfigurationJsonUtil.fromPojoToObjectNode(n2)));
    }

    @Test
    public void testFromObjectNodeToPojo() throws IOException {
        String json = node1;
        ZookeeperConfigurationNode n1 =
                ZookeeperConfigurationJsonUtil.fromJsonString(json, ZookeeperConfigurationNode.class);

        ObjectNode objNode1 = ZookeeperConfigurationJsonUtil.fromPojoToObjectNode(createNode1());
        ObjectNode objNode2 = ZookeeperConfigurationJsonUtil.fromPojoToObjectNode(n1);

        assertTrue(objNode1.equals(objNode2));
        ZookeeperConfigurationNode r1 =
                ZookeeperConfigurationJsonUtil.fromObjectNodeToPojo(objNode1, ZookeeperConfigurationNode.class);
        ZookeeperConfigurationNode r2 =
                ZookeeperConfigurationJsonUtil.fromObjectNodeToPojo(objNode2, ZookeeperConfigurationNode.class);
        assertEquals(ZookeeperConfigurationJsonUtil.toJsonString(r1), ZookeeperConfigurationJsonUtil.toJsonString(r2));

    }

    @Test
    public void testNonDefaultConstructor() {
        List<String> values = new ArrayList<String>();
        ZookeeperConfigurationNodeAttributes nodeAttr = new ZookeeperConfigurationNodeAttributes();
        ZookeeperConfigurationNode node = new ZookeeperConfigurationNode(nodeAttr, values);
        assertEquals(node.getNodeAttributes(), nodeAttr);
        assertEquals(node.getValues(), values);
    }
}
