package io.github.gilcesarf.commons.configuration2.zookeeper;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeSelectionModel;

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

    static class MultinodeData {
        private String path = null;
        private String name = null;
        private Boolean multinode = false;

        @Override
        public String toString() {
            return (path != null) ? path.toString() : super.toString();
        }

        public MultinodeData() {
        }

        public MultinodeData(String path, Boolean multinode) {
            setPath(path);
            setMultinode(multinode);
        }

        protected String getName() {
            return this.name;
        }

        protected String getPath() {
            return path;
        }

        protected void setPath(String path) {
            this.path = path;
            this.name = ZookeeperConfiguration.extractName(path);
        }

        protected Boolean isMultinode() {
            return multinode;
        }

        protected void setMultinode(Boolean multinode) {
            this.multinode = multinode;
        }

    }

    private static final String PATH_SEPARATOR = "/";
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
    private DefaultMutableTreeNode multinodeRootNode;

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
        return (rootPath == null) ? "" : rootPath;
    }

    public void setRootPath(String rootPath) {
        if (rootPath != null && !rootPath.startsWith(PATH_SEPARATOR)) {
            throw new IllegalArgumentException("root path must start with '/' character");
        }
        this.rootPath = rootPath;
    }

    public Map<String, String> getMultinodeMap() {
        return multinodeMap;
    }

    public void setMultinodeMap(Map<String, String> multinodeMap) {
        this.multinodeMap = multinodeMap;
        this.multinodeRootNode = buildTreeNodeFromMap(this.multinodeMap);
    }

    public DefaultMutableTreeNode getRootTreeNode() {
        return this.multinodeRootNode;
    }

    public static DefaultMutableTreeNode buildTreeNodeFromMap(Map<String, String> multinodeMap) {
        DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode();
        for (Entry<String, String> entry : multinodeMap.entrySet()) {
            String key = entry.getKey();
            DefaultMutableTreeNode insertionPoint = createOrLocateTreeNode(createTreePath(key), 0, treeNode);
            ((MultinodeData) insertionPoint.getUserObject()).setMultinode(true);
        }
        return treeNode;
    }

    private static String[] createTreePath(String key) {
        StringTokenizer tokenizer = new StringTokenizer(key, PATH_SEPARATOR);
        String[] treePath = new String[tokenizer.countTokens()];
        for (int i = 0; i < treePath.length; i++) {
            treePath[i] = tokenizer.nextToken();
        }
        return treePath;
    }

    private static DefaultMutableTreeNode createOrLocateTreeNode(String[] treePath, int currentPos,
            DefaultMutableTreeNode treeNode) {
        DefaultMutableTreeNode ret = null;
        MultinodeData nodeData = (MultinodeData) treeNode.getUserObject();
        if (nodeData == null) {
            nodeData = createNewNodeData(treePath, currentPos);
            treeNode.setUserObject(nodeData);
        }
        if (treePath.length == currentPos) {
            ret = treeNode;
        } else {
            // if (treePath[currentPos].equals(nodeData.getName())) {
            Enumeration<?> childEnum = treeNode.children();
            DefaultMutableTreeNode selectedChild = null;
            while (childEnum.hasMoreElements()) {
                DefaultMutableTreeNode currentChild = (DefaultMutableTreeNode) childEnum.nextElement();
                MultinodeData childNodeData = (MultinodeData) currentChild.getUserObject();
                if (childNodeData != null && treePath[currentPos].equals(childNodeData.getName())) {
                    selectedChild = currentChild;
                    break;
                }
            }
            if (selectedChild == null) {
                // No child fits next path level. Need to create a new one to continue.
                // no need to create a MultinodeData here. It will be created into the recursive call
                selectedChild = new DefaultMutableTreeNode();
                treeNode.add(selectedChild);
            }
            // use selectedChild to keep searching recursively.
            ret = createOrLocateTreeNode(treePath, currentPos + 1, selectedChild);
            // } else {
            // System.out.println("Oopss.");
            // }
        }
        return ret;
    }

    private static DefaultMutableTreeNode locateTreeNode(String[] treePath, int currentPos,
            DefaultMutableTreeNode treeNode, boolean exactMatch) {
        DefaultMutableTreeNode ret = null;
        MultinodeData nodeData = (MultinodeData) treeNode.getUserObject();
        if (nodeData == null) {
            return null;
        }
        if (treePath.length == currentPos) {
            ret = treeNode;
        } else {
            Enumeration<?> childEnum = treeNode.children();
            DefaultMutableTreeNode selectedChild = null;
            while (childEnum.hasMoreElements()) {
                DefaultMutableTreeNode currentChild = (DefaultMutableTreeNode) childEnum.nextElement();
                MultinodeData childNodeData = (MultinodeData) currentChild.getUserObject();
                if (childNodeData != null) {
                    if (treePath[currentPos].equals(childNodeData.getName())) {
                        selectedChild = currentChild;
                        break;
                    } else if ((!exactMatch) && childNodeData.isMultinode()
                               && treePath[currentPos].startsWith(childNodeData.getName())
                               && ZK_SEQ_NAME_PATTERN.matcher(treePath[currentPos]).matches()) {
                        selectedChild = currentChild;
                        break;
                    }
                }
            }
            if (selectedChild == null) {
                ret = null;
            } else {
                // use selectedChild to keep searching recursively.
                ret = locateTreeNode(treePath, currentPos + 1, selectedChild, exactMatch);
            }
        }
        return ret;
    }

    private static MultinodeData createNewNodeData(String[] treePath, int currentPos) {
        MultinodeData nodeData;
        nodeData = new MultinodeData();
        StringBuffer buf = new StringBuffer();
        int i = 0;
        do {
            buf.append(PATH_SEPARATOR);
            if (i < currentPos) {
                buf.append(treePath[i]);
            }
        } while (++i < currentPos);
        nodeData.setPath(buf.toString());
        return nodeData;
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

    private static final String extractName(String fullName) {
        int lastIndex = fullName.lastIndexOf('/');
        if (lastIndex == -1) {
            return fullName;
        } else {
            if (PATH_SEPARATOR.equals(fullName)) {
                return "";
            } else if (fullName.endsWith(PATH_SEPARATOR)) {
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
            if (fullName.endsWith(PATH_SEPARATOR)) {
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
            multinode = isMultinode(matcher.group(1), false);
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
                childNode
                        .addAttributes(
                                constructHierarchy(path + PATH_SEPARATOR + child, childNode, refChildValue, level + 1))
                        .value(refChildValue.getValue());
                node.addChild(childNode.create());
            }
        }
        return attributes;
    }

    public boolean isMultinode(String path, boolean exactMatch) {
        if (this.multinodeMap == null || this.multinodeRootNode == null) {
            return false;
        }
        String multinodeKey = path.replaceFirst(rootPath, "");
        if ("".equals(multinodeKey)) {
            multinodeKey = PATH_SEPARATOR;
        }
        DefaultMutableTreeNode node = locateTreeNode(createTreePath(multinodeKey), 0, multinodeRootNode, exactMatch);
        return node != null && ((MultinodeData) node.getUserObject()).isMultinode();
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
            cleanupZookeeperNodes(root, this.getRootPath() + PATH_SEPARATOR + root.getNodeName());
            this.needCleanup = false;
        }
        result = persistNode(root, this.getRootPath() + PATH_SEPARATOR + root.getNodeName());
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
        boolean multinode = isMultinode(path, false);

        String nodeName = node.getNodeName();

        ZookeeperConfigurationNode zkNode = buildZookeeperConfigurationNode(node);

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
                            extractParent(path) + PATH_SEPARATOR + zkNodeSeqName + PATH_SEPARATOR
                                                          + child.getNodeName());
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
                    result = result && persistNode(child, path + PATH_SEPARATOR + child.getNodeName());
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

    private static ZookeeperConfigurationNode buildZookeeperConfigurationNode(ImmutableNode node) {
        ZookeeperConfigurationNode zkNode = new ZookeeperConfigurationNode();
        List<String> value = extractValue(node);
        zkNode.setValues(value);

        Map<String, String> attr = extractAttributes(node);
        if (!attr.isEmpty()) {
            ZookeeperConfigurationNodeAttributes zkNodeAttr = new ZookeeperConfigurationNodeAttributes();
            zkNodeAttr.setAttributes(attr);
            zkNode.setAttributes(zkNodeAttr);
        }
        return zkNode;
    }

    private static List<String> extractValue(ImmutableNode node) {
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

    private static Map<String, String> extractAttributes(ImmutableNode node) {
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

    public void createAndShowConfigurationDataGUI() {
        // Create and set up the window.
        JFrame frame = new JFrame("Configuration Tree Data");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Add content to the window.
        ImmutableNode root = getSubConfigurationParentModel().getRootNode();
        DefaultMutableTreeNode rootNode = buildDefaultMutableTreeNode(root);
        frame.add(new TreePanel(rootNode));

        // Display the window.
        frame.pack();
        frame.setVisible(true);
    }

    private DefaultMutableTreeNode buildDefaultMutableTreeNode(ImmutableNode root) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(root);
        for (ImmutableNode child : root.getChildren()) {
            node.add(buildDefaultMutableTreeNode(child));
        }
        return node;
    }

    public void createAndShowMultinodeGUI() {
        // Create and set up the window.
        JFrame frame = new JFrame("Multinode Tree Data");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Add content to the window.
        frame.add(new TreePanel(this.multinodeRootNode));

        // Display the window.
        frame.pack();
        frame.setVisible(true);
    }

    @SuppressWarnings("serial")
    static class MultinodeDataTreeRenderer extends DefaultTreeCellRenderer {

        public MultinodeDataTreeRenderer() {
        }

        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            Object nodeInfo = ((DefaultMutableTreeNode) value).getUserObject();
            if (nodeInfo instanceof MultinodeData) {
                setText(((ZookeeperConfiguration.MultinodeData) nodeInfo).getName());
            } else if (nodeInfo instanceof ImmutableNode) {
                setText(((ImmutableNode) nodeInfo).getNodeName());
            } else {
                setText(nodeInfo.toString());
            }
            return this;
        }

        public String getNodeText(Object nodeInfo) {
            if (nodeInfo instanceof MultinodeData) {
                return ((MultinodeData) nodeInfo).isMultinode().toString();
            } else if (nodeInfo instanceof ImmutableNode) {
                ZookeeperConfigurationNode zkNode =
                        ZookeeperConfiguration.buildZookeeperConfigurationNode((ImmutableNode) nodeInfo);
                try {
                    return ZookeeperConfigurationJsonUtil.toJsonString(zkNode);
                } catch (IOException e) {
                    return "Error while converting zkNode:\n" + e.getMessage();
                }
            } else if (nodeInfo != null) {
                return nodeInfo.toString();
            } else {
                return "null";
            }
        }
    }

    @SuppressWarnings("serial")
    static class TreePanel extends JPanel implements TreeSelectionListener {
        private String lineStyle = null;
        private MultinodeDataTreeRenderer renderer = null;

        private JTree tree;
        private JEditorPane htmlPane;

        public TreePanel(DefaultMutableTreeNode top) {
            this(top, "Horizontal", new MultinodeDataTreeRenderer());
        }

        public TreePanel(DefaultMutableTreeNode top, MultinodeDataTreeRenderer renderer) {
            this(top, "Horizontal", renderer);
        }

        public TreePanel(DefaultMutableTreeNode top, String lineStyle) {
            this(top, lineStyle, new MultinodeDataTreeRenderer());
        }

        public TreePanel(DefaultMutableTreeNode top, String style, MultinodeDataTreeRenderer r) {
            super(new GridLayout(1, 0));
            this.lineStyle = style;
            this.renderer = r;

            // Create a tree that allows one selection at a time.
            tree = new JTree(top);
            tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

            // Listen for when the selection changes.
            tree.addTreeSelectionListener(this);

            tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
            tree.setCellRenderer(renderer);
            tree.setRootVisible(false);
            tree.putClientProperty("JTree.lineStyle", lineStyle);

            // Create the scroll pane and add the tree to it.
            JScrollPane treeView = new JScrollPane(tree);

            htmlPane = new JEditorPane();
            htmlPane.setEditable(false);

            JScrollPane htmlView = new JScrollPane(htmlPane);

            // Add the scroll panes to a split pane.
            JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
            splitPane.setTopComponent(treeView);
            splitPane.setBottomComponent(htmlView);

            Dimension minimumSize = new Dimension(100, 50);
            treeView.setMinimumSize(minimumSize);
            htmlView.setMinimumSize(minimumSize);
            splitPane.setDividerLocation(100);
            splitPane.setPreferredSize(new Dimension(500, 300));

            // Add the split pane to this panel.
            add(splitPane);
        }

        public void valueChanged(TreeSelectionEvent e) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();

            if (node == null)
                return;

            Object nodeInfo = node.getUserObject();
            htmlPane.setText(this.renderer.getNodeText(nodeInfo));
        }

    }

}
