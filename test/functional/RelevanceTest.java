package functional;

import com.google.common.collect.ImmutableMap;
import org.junit.*;
import play.test.*;
import play.mvc.Http.*;
//import models.*;

public class RelevanceTest extends FunctionalTest {

    // curl -d "url=http://sample.com&keygroups=sample&keygroups=free"  http://localhost:9000/relevance.json
    
    private final static String TEST_URI = "http://www.sample.com";

    @Test
    public void testRelevanceWorks() {
        Response response = POST("/relevance,json", ImmutableMap.of("url", TEST_URI, "keygroups", "sample"));
        assertIsOk(response);
        assertContentType("application/json", response);
        assertCharset("utf-8", response);
    }

}