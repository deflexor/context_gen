
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jsoup.Jsoup;
import org.junit.*;
import org.jsoup.nodes.Document;
import play.test.*;

public class BasicTest extends UnitTest {

    private final String TEST_URI = "http://realty.ru";

    @Test
    public void UserAgentHelperTest() {
        try {
            Document doc = Jsoup.connect(TEST_URI).timeout(2000).get();
            String title = doc.title();
            System.err.println(""+ title);
        }
        catch (IOException ex) {
            Logger.getLogger(BasicTest.class.getName()).log(Level.SEVERE, "", ex);
        }
    }
//    
//    @Test
//    public void UserAgentHelperTest() {
//        try {
//            Document doc = UserAgentHelper.fetchURIasDOM(TEST_URI);
//        } catch (Exception ex) {
//            Logger.getLogger(BasicTest.class.getName()).error(ex.getCause());
//        }
//    }
}
