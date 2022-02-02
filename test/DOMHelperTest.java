
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;

import play.test.UnitTest;

import com.cbroker.utils.DOMHelper;

public class DOMHelperTest extends UnitTest {

    private final static String TEST_URI = "http://www.reg.ru";
    
    @Test
    public void findLinksinDocumentTest() {
        
        Set<URI> links = null;
        try {
            File testFile = new File("./test/data/regru.html");
            Document doc = Jsoup.parse(testFile, "CP1251", TEST_URI);
            links = DOMHelper.findLinksInDocument(doc, TEST_URI);
            
        } catch (Exception ex) {
        }
        for (URI url : links) {
            System.err.println(url);
        }
        assertEquals(72, links.size());
    }

    @Test
    public void findTitleinDocumentTest() {
        String title = null;
        try {
            File testFile = new File("./test/data/regru.html");
            Document doc = Jsoup.parse(testFile, "CP1251", TEST_URI);
            title = DOMHelper.findTitleInDocument(doc);
            
        } catch (Exception ex) {
        }
        assertEquals("регистрация доменов", title.substring(9,28));
    }

}
