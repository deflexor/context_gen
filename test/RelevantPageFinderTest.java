
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.junit.Test;

import play.test.UnitTest;

import com.cbroker.stemutils.RelevantPageFinder;
import com.cbroker.stemutils.RelevantPageFinderHybrid;

public class RelevantPageFinderTest extends UnitTest {
    
    private final String TEST_URI = "http://sample.com/";
    private final String TEST_URI1 = "http://reg.ru/";

    @Test
    public void findPagesForGroupsTest() {
        List<String> groups = Arrays.asList("free", "free sample");
        try {
            HashMap<String, String> relevantPages = RelevantPageFinder.findPagesForGroups(TEST_URI, groups);
            assertEquals(6, relevantPages.size());
            assertEquals("http://sample.com/free-coupons/", relevantPages.get("free"));
            assertEquals("http://sample.com/cosmetic-samples/", relevantPages.get("free sample"));
            
        } catch (Exception ex) {
        	ex.printStackTrace();
        }
    }
    
    //@Test
    public void findPagesForGroupsTest1() {
        List<String> groups = Arrays.asList("Whois домен" ,"регистрация доменов");
        try {
            HashMap<String, String> relevantPages = RelevantPageFinder.findPagesForGroups(TEST_URI1, groups);
            System.err.println(relevantPages.keySet() + "\n\n");
            assertEquals(6, relevantPages.size());
        } catch (Exception ex) {
        	ex.printStackTrace();
        }
    }

}
