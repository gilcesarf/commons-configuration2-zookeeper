package io.github.gilcesarf.commons.configuration2.zookeeper.json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class ZookeeperConfigurationJsonUtil {
    /**
     * Cannot be instantiated
     */
    private ZookeeperConfigurationJsonUtil() {
    }

    public static String toJsonString(Object obj) throws JsonGenerationException, JsonMappingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, false);
        mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, false);
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
        mapper.writeValue(baos, obj);
        return new String(baos.toByteArray());
    }

    public static <T> T fromJsonString(String json, Class<T> c)
            throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        T obj = (T) mapper.readValue(json, c);
        return obj;
    }

    public static ObjectNode fromPojoToObjectNode(Object pojo)
            throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode obj = (ObjectNode) mapper.valueToTree(pojo);
        return obj;
    }

    public static <T> T fromObjectNodeToPojo(ObjectNode node, Class<T> c)
            throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        T obj = (T) mapper.treeToValue(node, c);
        return obj;
    }
}
