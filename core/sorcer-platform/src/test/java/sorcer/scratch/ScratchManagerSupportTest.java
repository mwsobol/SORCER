package sorcer.scratch;

import org.junit.Test;
import sorcer.core.context.ServiceContext;
import sorcer.data.DataService;

import java.io.File;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sorcer.core.SorcerConstants.SCRATCH_DIR;

public class ScratchManagerSupportTest {

    @Test
    public void testGetScratchDir() {
        ScratchManagerSupport scratchManager = new ScratchManagerSupport();
        File dataDir = new File(DataService.getDataDir());
        if(!dataDir.exists())
            dataDir.mkdirs();
        for(File f : dataDir.listFiles()) {
            if(f.getName().startsWith(scratchManager.getDefaultScratchPrefix()))  {
                if(deleteDirectory(f))
                    System.out.println("Removed "+f.getName());
            }
        }
        for(int i=0; i<10; i++)
            scratchManager.getScratchDir();
        int count = 0;
        for(File f : dataDir.listFiles()) {
            if(f.getName().startsWith(scratchManager.getDefaultScratchPrefix()))  {
                count++;
            }
        }
        System.out.println(scratchManager.getScratchGbFree());
        scratchManager.clean();
        assertEquals("Expected 10, got " + count, 10, count);
    }

    @Test
    public void testGetScratchDirFromProperties() {
        Properties p = new Properties();
        p.setProperty(SCRATCH_DIR, "bumble-bee");
        ScratchManagerSupport scratchManager = new ScratchManagerSupport();
        scratchManager.setProperties(p);
        File f = scratchManager.getScratchDir();
        assertTrue(f.getName().contains("bumble-bee"));
        scratchManager.clean();
    }

    @Test
    public void testGetScratchDirFromContext() {
        ServiceContext context = new ServiceContext();
        ScratchManagerSupport scratchManager = new ScratchManagerSupport();
        File f = scratchManager.getScratchDir(context, "french-fry");
        assertTrue(f.getName().contains("french-fry"));
        scratchManager.clean();
    }

    @Test
    public void testRoot() {
        File root = new File(DataService.getDataDir(), "green-eyed/potatoes");
        if(root.exists())
            deleteDirectory(root);
        ScratchManagerSupport scratchManagerSupport = new ScratchManagerSupport(root);
        scratchManagerSupport.getScratchDir();
        assertTrue(root.exists());
        assertEquals("potatoes", root.getName());
        scratchManagerSupport.clean();
    }

     boolean deleteDirectory(File path) {
        if( path.exists() ) {
            File[] files = path.listFiles();
            for(int i=0; i<files.length; i++) {
                if(files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                }
                else {
                    files[i].delete();
                }
            }
        }
        return(path.delete());
    }
}