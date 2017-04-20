package io.github.gilcesarf.commons.configuration2.zookeeper.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ZookeeperConfiguratinJsonUtilTest {

    private String node1 =
            "{\"nodeAttributes\":{\"address\":\"2\",\"management_port\":\"4\",\"interval\":\"5\",\"commandList\":\"6\",\"id\":\"1\",\"ascii_port\":\"3\"},\"values\":[\"val1\",\"val2\"]}";
    private String node2 =
            "{\"nodeAttributes\":{\"address\":\"12\",\"management_port\":\"14\",\"interval\":\"15\",\"commandList\":\"16\",\"id\":\"11\",\"ascii_port\":\"13\"},\"values\":[\"val4\",\"val5\"]}";

    @Before
    public void setUp() throws Exception {
        System.out.println("Node 1 Length: " + node1.getBytes().length);
        System.out.println("Node 2 Length: " + node2.getBytes().length);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testSerialize() throws IOException {
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
        String json = ZookeeperConfiguratinJsonUtil.toJsonString(node);
        System.out.println(json);

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

        json = ZookeeperConfiguratinJsonUtil.toJsonString(node);
        System.out.println(json);
    }

    @Test
    public void testDeserialize() throws IOException {
        String json = node1;
        ZookeeperConfigurationNode n1 =
                ZookeeperConfiguratinJsonUtil.fromJsonString(json, ZookeeperConfigurationNode.class);
        System.out.println(ZookeeperConfiguratinJsonUtil.toJsonString(n1));
        json = node2;
        ZookeeperConfigurationNode n2 =
                ZookeeperConfiguratinJsonUtil.fromJsonString(json, ZookeeperConfigurationNode.class);
        System.out.println(ZookeeperConfiguratinJsonUtil.toJsonString(n2));
    }

}
