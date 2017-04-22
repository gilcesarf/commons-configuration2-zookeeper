package io.github.gilcesarf.commons.configuration2.zookeeper;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.configuration2.BaseHierarchicalConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.Initializable;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.sync.LockMode;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.curator.RetryPolicy;
import org.apache.curator.RetrySleeper;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.BoundedExponentialBackoffRetry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooKeeper.States;
import org.apache.zookeeper.data.Stat;

import io.github.gilcesarf.commons.configuration2.zookeeper.json.ZookeeperConfigurationJsonUtil;
import io.github.gilcesarf.commons.configuration2.zookeeper.json.ZookeeperConfigurationNode;
import io.github.gilcesarf.commons.configuration2.zookeeper.json.ZookeeperConfigurationNodeAttributes;

public class ZookeeperConfiguration extends BaseHierarchicalConfiguration implements Initializable, Closeable {
    private static final Logger LOG = LogManager.getLogger();
    private static final String ZK_SEQ_NAME_ATTR = "multinode.sequence.name";
    static final Pattern ZK_SEQ_NAME_PATTERN = Pattern.compile("(.*)(\\d{10})$");

    private String connectString = null;
    private RetryPolicy retryPolicy = null;
    private String rootPath = null;
    private CuratorFramework curator = null;
    private Map<String, String> multinodeMap = null;
    private boolean needSaving = false;
    private boolean needCleanup = false;

    public ZookeeperConfiguration() {
        super();
    }

    public String getConnectString() {
        return connectString;
    }

    public void setConnectString(String connectString) {
        this.connectString = connectString;
    }

    public RetryPolicy getRetryPolicy() {
        return retryPolicy;
    }

    public void setRetryPolicy(RetryPolicy retryPolicy) {
        this.retryPolicy = retryPolicy;
    }

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public Map<String, String> getMultinodeMap() {
        return multinodeMap;
    }

    public void setMultinodeMap(Map<String, String> multinodeMap) {
        this.multinodeMap = multinodeMap;
    }

    public CuratorFramework getCuratorFramework() {
        return this.curator;
    }

    public void initialize() {
        if (curator == null) {
            curator = CuratorFrameworkFactory.newClient(this.getConnectString(), this.getRetryPolicy());
            curator.start();
        }
    }

    protected final void connect() throws Exception {
        if (this.retryPolicy == null) {
            this.retryPolicy = new BoundedExponentialBackoffRetry(500, 5000, 5);
        }
        RetrySleeper sleeper = new RetrySleeper() {
            @Override
            public void sleepFor(long time, TimeUnit unit) throws InterruptedException {
                long value = unit.toMillis(time);
                Thread.sleep(value);
            }
        };
        long now = System.currentTimeMillis();
        long elapsed = 0L;
        int counter = 0;
        while (curator.getZookeeperClient().getZooKeeper().getState() != States.CONNECTED
               && this.retryPolicy.allowRetry(counter++, elapsed, sleeper)) {
            long aux = System.currentTimeMillis();
            elapsed = aux - now;
            now = aux;
        }
        if (curator.getZookeeperClient().getZooKeeper().getState() != States.CONNECTED) {
            close();
            String message = "Connection timed out";
            throw new ConfigurationException(message);
        }
        readConfiguration();
    }

    private void readConfiguration() throws Exception {
        ImmutableNode.Builder rootBuilder = new ImmutableNode.Builder();
        MutableObject<List<String>> rootValue = new MutableObject<List<String>>();
        Map<String, String> attributes = constructHierarchy(this.getRootPath(), rootBuilder, rootValue, 0);
        ImmutableNode top = rootBuilder.name(rootPath).value(rootValue.getValue()).addAttributes(attributes).create();
        getSubConfigurationParentModel().mergeRoot(top, rootPath, null, null, this);
    }

    public void close() {
        if (curator != null) {
            curator.close();
            curator = null;
            // TODO: should discard model???
        }
    }

    @Override
    public void copy(Configuration c) {
        throw new UnsupportedOperationException("copy not supported");
    }

    private String extractName(String fullName) {
        int lastIndex = fullName.lastIndexOf('/');
        if (lastIndex == -1) {
            return fullName;
        } else {
            if (fullName.endsWith("/")) {
                return extractName(fullName.substring(0, lastIndex));
            } else {
                return fullName.substring(lastIndex + 1);
            }
        }
    }

    private String extractParent(String fullName) {
        int lastIndex = fullName.lastIndexOf('/');
        if (lastIndex == -1) {
            return "";
        } else {
            if (fullName.endsWith("/")) {
                return extractName(fullName.substring(0, lastIndex));
            } else {
                return fullName.substring(0, lastIndex);
            }
        }
    }

    private Map<String, String> constructHierarchy(String path, ImmutableNode.Builder node,
            MutableObject<List<String>> refValue, int level) throws Exception {
        Map<String, String> attributes = new HashMap<String, String>();
        boolean multinode = false;
        Stat rootStat = curator.checkExists().forPath(path);
        String nodeName = extractName(path);
        Matcher matcher = ZK_SEQ_NAME_PATTERN.matcher(path);
        String zkSeqName = null;
        if (matcher.matches()) {
            multinode = isMultinode(matcher.group(1));
            if (multinode) {
                zkSeqName = nodeName;
                nodeName = extractName(matcher.group(1));
            }
        }
        node.name(nodeName);
        if (rootStat != null) {
            ZookeeperConfigurationNode zkNode = null;
            byte[] data = curator.getData().forPath(path);
            if (data != null) {
                String dataStr = new String(data);
                if (dataStr != null && !"".equals(dataStr)) {
                    try {
                        zkNode = ZookeeperConfigurationJsonUtil.fromJsonString(dataStr,
                                ZookeeperConfigurationNode.class);
                        refValue.setValue(zkNode.getValues());
                        if (zkNode.getNodeAttributes() != null && zkNode.getNodeAttributes().getAttributes() != null
                            && !zkNode.getNodeAttributes().getAttributes().isEmpty()) {
                            attributes.putAll(zkNode.getNodeAttributes().getAttributes());
                        }
                        if (multinode) {
                            attributes.put(ZK_SEQ_NAME_ATTR, zkSeqName);
                        }
                    } catch (IOException e) {
                        // log and ignore
                        LOG.warn("Cannot deserialize data for node {}.", path);
                        LOG.catching(e);
                    }
                }
            }
            List<String> children = curator.getChildren().forPath(path);
            for (String child : children) {
                ImmutableNode.Builder childNode = new ImmutableNode.Builder();
                childNode.name(child);
                MutableObject<List<String>> refChildValue = new MutableObject<List<String>>();
                childNode.addAttributes(constructHierarchy(path + "/" + child, childNode, refChildValue, level + 1))
                        .value(refChildValue.getValue());
                node.addChild(childNode.create());
            }
        }
        return attributes;
    }

    private boolean isMultinode(String path) {
        String multinodeKey = path.replaceFirst(rootPath, "");
        if ("".equals(multinodeKey)) {
            multinodeKey = "/";
        }
        return this.multinodeMap != null && this.multinodeMap.containsKey(multinodeKey);
    }

    public void copyConfigurationFrom(BaseHierarchicalConfiguration config) {
        config.lock(LockMode.READ);
        ImmutableNode root = config.getNodeModel().getInMemoryRepresentation();
        ImmutableNode copy = copyImmutableNodeHierarchy(root);
        getSubConfigurationParentModel().replaceRoot(copy, this);
        this.needSaving = true;
        this.needCleanup = true;
        config.unlock(LockMode.READ);
    }

    private ImmutableNode copyImmutableNodeHierarchy(ImmutableNode source) {
        ImmutableNode.Builder builder = new ImmutableNode.Builder();
        builder.name(source.getNodeName());
        builder.value(source.getValue());
        builder.addAttributes(source.getAttributes());
        for (ImmutableNode child : source.getChildren()) {
            builder.addChild(copyImmutableNodeHierarchy(child));
        }
        return builder.create();
    }

    public boolean persist() {
        boolean result = false;
        lock(LockMode.WRITE);
        ImmutableNode root = getSubConfigurationParentModel().getRootNode();
        if (needCleanup) {
            cleanupZookeeperNodes(root, this.getRootPath() + "/" + root.getNodeName());
            this.needCleanup = false;
        }
        result = persistNode(root, this.getRootPath() + "/" + root.getNodeName());
        this.needSaving = false;
        unlock(LockMode.WRITE);
        return result;
    }

    private void cleanupZookeeperNodes(ImmutableNode node, String path) {
        try {
            curator.delete().deletingChildrenIfNeeded().forPath(path);
        } catch (Exception e) {
            LOG.error("Failure while recursively deleting path {}", path);
            LOG.catching(e);
        }
    }

    private boolean persistNode(ImmutableNode node, String path) {
        boolean result = false;
        boolean multinode = isMultinode(path);

        String nodeName = node.getNodeName();

        ZookeeperConfigurationNode zkNode = new ZookeeperConfigurationNode();
        List<String> value = extractValue(node);
        zkNode.setValues(value);

        Map<String, String> attr = extractAttributes(node);
        if (!attr.isEmpty()) {
            ZookeeperConfigurationNodeAttributes zkNodeAttr = new ZookeeperConfigurationNodeAttributes();
            zkNodeAttr.setAttributes(attr);
            zkNode.setAttributes(zkNodeAttr);
        }

        String data = null;
        try {
            if (multinode) {
                String zkNodeSeqName = null;
                ZookeeperConfigurationNodeAttributes zkNodeAttributes = zkNode.getNodeAttributes();
                if (zkNodeAttributes != null) {
                    zkNodeSeqName = zkNodeAttributes.removeAttribute(ZK_SEQ_NAME_ATTR);
                }
                data = ZookeeperConfigurationJsonUtil.toJsonString(zkNode);
                if (zkNodeSeqName == null || "".equals(zkNodeSeqName)) {
                    // first time this node has been saved
                    zkNodeSeqName = extractName(curator.create()
                            .creatingParentsIfNeeded()
                            .withMode(CreateMode.PERSISTENT_SEQUENTIAL)
                            .forPath(path, data.getBytes()));
                    result = true;
                } else {
                    // this node has already been saved before
                    curator.setData().forPath(path + zkNodeSeqName, data.getBytes());
                    result = true;
                }
                for (ImmutableNode child : node.getChildren()) {
                    result = result && persistNode(child,
                            extractParent(path) + "/" + zkNodeSeqName + "/" + child.getNodeName());
                }
            } else {
                data = ZookeeperConfigurationJsonUtil.toJsonString(zkNode);
                Stat existResult = curator.checkExists().forPath(path);
                if (existResult == null) {
                    String createResult = curator.create()
                            .creatingParentsIfNeeded()
                            .withMode(CreateMode.PERSISTENT)
                            .forPath(path, data.getBytes());
                    result = true;
                } else {
                    curator.setData().forPath(path, data.getBytes());
                    result = true;
                }

                for (ImmutableNode child : node.getChildren()) {
                    result = result && persistNode(child, path + "/" + child.getNodeName());
                }
            }
        } catch (IOException e) {
            LOG.warn("Cannot serialize data for path {}. Node and all children will be ignored.", path);
            LOG.catching(e);
        } catch (Exception e) {
            LOG.error("Failure while persisting data for path {}. Node and all children will be ignored.", path);
            LOG.catching(e);
        }
        return result;
    }

    private List<String> extractValue(ImmutableNode node) {
        Object nodeValue = node.getValue();
        List<String> value = null;
        if (nodeValue != null) {
            value = new ArrayList<String>();
            if (nodeValue instanceof String) {
                value.add((String) nodeValue);
            } else if (nodeValue instanceof List<?>) {
                for (Object elem : (List<?>) nodeValue) {
                    if (elem instanceof String) {
                        value.add((String) elem);
                    } else {
                        value.add(elem.toString());
                    }
                }
            } else {
                value.add(nodeValue.toString());
            }
        }
        return value;
    }

    private Map<String, String> extractAttributes(ImmutableNode node) {
        Map<String, String> attr = new HashMap<String, String>();
        Map<String, Object> nodeAttrs = node.getAttributes();
        for (Entry<String, Object> nodeAttr : nodeAttrs.entrySet()) {
            if (nodeAttr.getValue() instanceof String) {
                attr.put(nodeAttr.getKey(), (String) nodeAttr.getValue());
            } else {
                attr.put(nodeAttr.getKey(), nodeAttr.getValue().toString());
            }
        }
        return attr;
    }

}
