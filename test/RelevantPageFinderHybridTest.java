import static com.cbroker.utils.Constants.FORMAT2;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.junit.Test;

import play.test.UnitTest;

import com.cbroker.stemutils.RelevantPageFinderHybrid;
import com.cbroker.stemutils.data.SiteSection;
import com.cbroker.utils.MiniInfo;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import com.thoughtworks.xstream.persistence.FileStreamStrategy;
import com.thoughtworks.xstream.persistence.XmlArrayList;

public class RelevantPageFinderHybridTest extends UnitTest {

    List<String>         groups    = Arrays.asList("Whois домен",
                                           "регистрация доменов",
                                           "регистрация доменных имен",
                                           "домен", "домены com", "домены ru",
                                           "домены рф",
                                           "зарегистрировать домен",
                                           "куплю домен", "продам домен");

    @Test
    public void findPagesForGroupsTest1() throws Exception {
        long start = System.currentTimeMillis();
        System.err.println();
        HashMap<String, String> groupsPages = new HashMap<String, String>();
        XStream x = new XStream(new StaxDriver());
        FileStreamStrategy strategy = new FileStreamStrategy(new File("test/data/xstream"), x);
        List persisted = new XmlArrayList(strategy);

        List<SiteSection> sections = new ArrayList<SiteSection>();
        for (Object s : persisted) {
            sections.add((SiteSection)s);
        }
        System.err.println("done restoration in " + FORMAT2.format((System.currentTimeMillis()-start)/1000));
        start = System.currentTimeMillis();
        for (String group : groups) {
            System.err.print("group: " + group);
            String relevantSection = RelevantPageFinderHybrid
                    .findRelevantSectionForGroup(group, sections);
            if (relevantSection != null) {
                groupsPages.put(group, relevantSection);
            }
            System.err.println(" done in " + FORMAT2.format((System.currentTimeMillis()-start)/1000));
            start = System.currentTimeMillis();
        }

        assertEquals(9, groupsPages.size());
        assertEquals("http://reg.ru/reseller/index", groupsPages.get("домен"));
        assertEquals("http://reg.ru/reseller/index", groupsPages.get("регистрация доменов"));
        assertEquals("http://reg.ru/kb/zonepedia?dzone=com", groupsPages.get("домены com"));
        assertEquals("http://reg.ru/newdomain/choose", groupsPages.get("регистрация доменных имен"));
        assertEquals("http://rf.reg.ru/", groupsPages.get("домены рф"));
        assertEquals("http://stat.reg.ru/", groupsPages.get("домены ru"));
        assertEquals("http://reg.ru/whois/index", groupsPages.get("Whois домен"));
        //assertEquals("http://reg.ru/for_professionals", groupsPages.get("продам домен"));
        assertEquals("http://rf.reg.ru/", groupsPages.get("зарегистрировать домен"));
        assertEquals("http://reg.ru/sslcertificate/", groupsPages.get("куплю домен"));
    }

}
