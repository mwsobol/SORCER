package sorcer.scratch;

import org.junit.Assert;
import sorcer.data.DataService;

import java.io.File;

public class ScratchManagerSupportTest {

    //@Test
    public void testGetScratchDir() throws Exception {
        ScratchManager scratchManager = new ScratchManagerSupport();
        File dataDir = new File(DataService.getDataDir());
        if(!dataDir.exists())
            dataDir.mkdirs();
        for(File f : dataDir.listFiles()) {
            if(f.getName().startsWith(ScratchManagerSupport.DEFAULT_SCRATCH_DIR))  {
                if(deleteDirectory(f))
                    System.out.println("Removed "+f.getName());
            }
        }
        for(int i=0; i<10; i++)
            scratchManager.getScratchDir();
        int count = 0;
        for(File f : dataDir.listFiles()) {
            if(f.getName().startsWith(ScratchManagerSupport.DEFAULT_SCRATCH_DIR))  {
                count++;
            }
        }
        Assert.assertTrue("Expected 10, got "+count, count==10);
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
        return( path.delete() );
    }
}