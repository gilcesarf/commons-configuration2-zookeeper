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
    private static final String connectString = "localhost:2181/commons-config";
    private static final RetryPolicy retryPolicy = new BoundedExponentialBackoffRetry(100, 3000, 10);
    private static final String rootPath = "/config";

    public static void main(String[] args) throws Exception {
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
            String data = new String(curator.getData().forPath(path));
            LOG.info("Path: {} Data: {}", path, data);
            List<String> children = curator.getChildren().forPath(path);
            for (String child : children) {
                dumpNode(path + "/" + child);
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
