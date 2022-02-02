
import com.cbroker.stemutils.KeywordGrouper;
import com.cbroker.stemutils.data.KeywordGroup;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.List;
import org.apache.log4j.Logger;
import org.junit.*;
import play.test.*;

public class KeywordGrouperTest extends UnitTest {
    
    private final String TEST_URI = "http://www.reggi.ru/blog/rss/";

    @Test
    public void groupKeywordsTest() {
        File testFile = new File("./test/data/_shablons1.txt");
        try {
            List<String> keywords = Files.readLines(testFile, Charset.forName("CP1251"));
            List<KeywordGroup> groups = KeywordGrouper.groupKeywords(new HashSet<String>(keywords));
            assertEquals(400, groups.size());
            assertEquals(7, groups.get(0).getKeyPhrases().size());
            assertEquals(3, groups.get(1).getKeyPhrases().size());
        } catch (IOException ex) {
            //ex.printStackTrace();
            Logger.getLogger(BasicTest.class.getName()).error(ex.getMessage());
        }
    }
    
}
