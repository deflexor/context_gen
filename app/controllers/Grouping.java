package controllers;

import com.cbroker.stemutils.KeywordGrouper;
import com.cbroker.stemutils.data.KeywordGroup;
import java.util.List;
import java.util.Set;
import play.data.validation.Required;
import play.mvc.*;


public class Grouping extends Controller {

    public static void index(@Required Set<String> keywords) {
        if (validation.hasErrors()) {
            String errorStr = "";
            for (play.data.validation.Error error : validation.errors()) {
                if(!errorStr.isEmpty()) errorStr += ", ";
                errorStr += error.getKey() + ": " + error.message();
            }
            renderJSON("{ _error: \"" + errorStr + "\" }");
        }
        else {
            try {
                List<KeywordGroup> groups = KeywordGrouper.groupKeywords(keywords);
                renderJSON(groups);
            } catch (Exception ex) {
                ex.printStackTrace();
                renderJSON("{ _error: \"" + ex.toString() + "\" }");
            }
        }
    }

}