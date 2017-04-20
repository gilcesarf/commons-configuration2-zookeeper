package io.github.gilcesarf.commons.configuration2.zookeeper;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

import org.apache.curator.test.InstanceSpec;
import org.apache.curator.test.TestingCluster;
import org.junit.After;
import org.junit.Before;

/**
 * Abstract class that starts a Zookeeper cluster to support testing.
 * 
 * It starts a 3-node cluster using temporary directories and cleanup the directories in tearDown.
 * 
 * @author gilcesarf
 *
 */
public abstract class AbstractZookeeperTest {
    protected TestingCluster zkCluster = null;
    protected InstanceSpec[] spec = null;
    protected File[] dataDirectory = null;
    protected int[] port = null;
    protected int[] electionPort = null;
    protected int[] quorumPort = null;

    @Before
    public void setUp() throws Exception {
        System.setProperty("zookeeper.jmx.log4j.disable", "true");
        dataDirectory = new File[3];
        port = new int[3];
        electionPort = new int[3];
        quorumPort = new int[3];
        int portBase = 3181;
        int electionPortBase = 3281;
        int quorumPortBase = 3381;
        spec = new InstanceSpec[3];
        for (int i = 0; i < spec.length; i++) {
            Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxr-x---");
            FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions.asFileAttribute(perms);
            dataDirectory[i] = Files.createTempDirectory("zkcluster", attr).toFile();
            // dataDirectory[i].deleteOnExit();
            port[i] = portBase + i;
            electionPort[i] = electionPortBase + i;
            quorumPort[i] = quorumPortBase + i;
            spec[i] = new InstanceSpec(dataDirectory[i], port[i], electionPort[i], quorumPort[i], true, i);
        }

        zkCluster = new TestingCluster(spec);
        zkCluster.start();
    }

    @After
    public void tearDown() throws Exception {
        zkCluster.stop();
        zkCluster = null;
        for (int i = 0; i < dataDirectory.length; i++) {
            recursiveDelete(dataDirectory[i]);
        }
    }

    private final void recursiveDelete(File file) {
        for (File child : file.listFiles()) {
            if (child.isDirectory()) {
                recursiveDelete(child);
            } else if (child.isFile()) {
                child.delete();
            }
        }
        file.delete();
    }

}
