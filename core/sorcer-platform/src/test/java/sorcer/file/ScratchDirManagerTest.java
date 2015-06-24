package sorcer.file;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Rafał Krupiński
 */
public class ScratchDirManagerTest {

    @Test
    public void testGetNewScratchDir() throws Exception {
        Path root = Paths.get(FileUtils.getTempDirectoryPath(), "scratch");
        ScratchDirManager manager = new ScratchDirManager(root, 0);
        File testDir = manager.getNewScratchDir("a");

        FileUtils.forceMkdir(testDir);
        Thread.sleep(2000);
        manager.cleanup1(100);

        Assert.assertNull(testDir.list());
    }
}