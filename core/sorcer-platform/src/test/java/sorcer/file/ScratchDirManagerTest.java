package sorcer.file;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;

/**
 * @author Rafał Krupiński
 */
public class ScratchDirManagerTest {

    @Test
    public void testGetNewScratchDir() throws Exception {
        long start = System.currentTimeMillis();
        File root = new File(FileUtils.getTempDirectory(), "scratch");
        ScratchDirManager manager = new ScratchDirManager(root, 0);
        File testDir = manager.getNewScratchDir("a");

        FileUtils.forceMkdir(testDir);
        Thread.sleep(2000);
        manager.cleanup1(100);

        System.out.println(testDir.list());
        System.out.println(Arrays.toString(testDir.list()));
        Assert.assertArrayEquals(new String[0],testDir.list());
    }
}