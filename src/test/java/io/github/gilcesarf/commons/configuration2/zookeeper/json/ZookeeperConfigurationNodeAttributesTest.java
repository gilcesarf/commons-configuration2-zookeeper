package io.github.gilcesarf.commons.configuration2.zookeeper.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class ZookeeperConfigurationNodeAttributesTest {
    @Test
    public void test() {
        Map<String, String> attributes = new HashMap<String, String>();
        ZookeeperConfigurationNodeAttributes nodeAttr = new ZookeeperConfigurationNodeAttributes(attributes);
        assertEquals(nodeAttr.getAttributes(), attributes);
        String key = "k1";
        assertNull(nodeAttr.getAttribute(key));
        assertNull(nodeAttr.removeAttribute(key));
        String value = "v1";
        nodeAttr.setAdditionalAttribute(key, value);
        assertEquals(nodeAttr.getAttribute(key), value);
        String ret = nodeAttr.removeAttribute(key);
        assertNull(nodeAttr.getAttribute(key));
        assertEquals(ret, value);
        HashMap<String, String> attributes2 = new HashMap<String, String>();
        nodeAttr.setAttributes(attributes2);
        assertTrue(nodeAttr.getAttributes() != attributes);
        assertEquals(nodeAttr.getAttributes(), attributes2);
        assertNull(nodeAttr.getAttribute(null));
        assertNull(nodeAttr.removeAttribute(null));
    }

}
