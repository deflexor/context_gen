
import java.util.Set;

import com.cbroker.stemutils.KeywordFinder;
import com.cbroker.stemutils.data.KeywordStats;
import com.cbroker.utils.UserAgentHelper;
import org.jsoup.nodes.Document;
import org.junit.*;
import play.test.*;
//import models.*;

public class KeywordFinderTest extends UnitTest {

    private final static String TEST_URI = "http://www.reg.ru";
    private final static String TEST_URI1 = "http://www.reg.ru/whois/index";
    private final static String TEST_URI2 = "http://www.reg.ru/newdomain/suggest-variants";
    

    @Test
    public void findKeywordsTest1() {
        KeywordStats kwk = null;
        
        try {
            Document doc = UserAgentHelper.fetchURIasDocument(TEST_URI1);
            KeywordFinder finder = new KeywordFinder(doc);
            kwk = finder.findKeywords();
            //System.err.println(kwk.getWordsTotal() + "\n\n");
        } catch (Exception ex) {
            System.err.println("!!!! GOT ERROR !!!!");
            ex.printStackTrace();
        }
//        Set<String> keySet = kwk.getStatsMap(2).keySet();
//        System.err.println("\n\n\n");
//        for (String key : keySet) {
//            if( key.contains("hois") ) { 
//                System.err.println(""+ key + " " + kwk.getStatsMap(2).get(key).score);
//            }
//        }
        
        assertEquals(0.8, kwk.getStatsMap(2).get("whois информация").getScore(), 0.0001);
        assertEquals(7.6, kwk.getStatsMap(2).get("whois домен").getScore(), 0.0001);
        assertEquals(6.6, kwk.getStatsMap(2).get("сервис whois").getScore(), 0.0001);
        assertEquals(536, kwk.getWordsTotal());
    }

    
    @Test
    public void findKeywordsTest2() {
        KeywordStats kwk = null;
        try {
            Document doc = UserAgentHelper.fetchURIasDocument(TEST_URI2);
            KeywordFinder finder = new KeywordFinder(doc);
            kwk = finder.findKeywords();
            //System.err.println(kwk.getWordsTotal() + "\n\n");
        } catch (Exception ex) {
            System.err.println("!!!! GOT ERROR !!!!");
            ex.printStackTrace();
        }
        //assertEquals(1.2, kwk.getStatsMap(2).get("зарегистрировать домен").score, 0.0001);
        //assertEquals(1.8, kwk.getStatsMap(2).get("свободный проверка").score, 0.0001);
        assertEquals(427, kwk.getWordsTotal());
    }

}
