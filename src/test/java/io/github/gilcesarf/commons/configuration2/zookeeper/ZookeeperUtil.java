package io.github.gilcesarf.commons.configuration2.zookeeper;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.curator.RetryPolicy;
import org.apache.curator.RetrySleeper;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.BoundedExponentialBackoffRetry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.zookeeper.ZooKeeper.States;
import org.apache.zookeeper.data.Stat;

public class ZookeeperUtil {
    private static final Logger LOG = LogManager.getLogger();

    private static CuratorFramework curator = null;
    private static final String connectString = "127.0.0.1:3181";
    private static final RetryPolicy retryPolicy = new BoundedExponentialBackoffRetry(100, 3000, 10);
    private static final String rootPath = "/";

    public static void main(String[] args) throws Exception {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("zookeeper.jmx.log4j.disable", "true");
        test();
    }

    public static final void test() throws Exception {
        startCurator();
        dumpNode(rootPath);
        stopCurator();
    }

    private static void dumpNode(String path) throws Exception {
        Stat rootStat = curator.checkExists().forPath(path);
        if (rootStat != null) {
            String data = null;
            byte[] byteArray = curator.getData().forPath(path);
            if (byteArray != null) {
                data = new String(byteArray);
            }
            LOG.info("Path: {} Data: {}", path, data);
            List<String> children = curator.getChildren().forPath(path);
            for (String child : children) {
                if (path.trim().endsWith("/")) {
                    dumpNode(path.trim() + child);
                } else {
                    dumpNode(path.trim() + "/" + child);
                }
            }
        }
    }

    private static final void startCurator() throws Exception {
        curator = CuratorFrameworkFactory.newClient(connectString, retryPolicy);
        curator.start();
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
               && retryPolicy.allowRetry(counter++, elapsed, sleeper)) {
            long aux = System.currentTimeMillis();
            elapsed = aux - now;
            now = aux;
        }
        if (curator.getZookeeperClient().getZooKeeper().getState() != States.CONNECTED) {
            stopCurator();
            String message = "Connection timed out";
            throw new ConfigurationException(message);
        }
    }

    private static final void stopCurator() {
        if (curator != null) {
            curator.close();
            curator = null;
        }
    }
}
