package controllers;

import com.cbroker.stemutils.RelevantPageFinder;
import com.cbroker.utils.MiniInfo;
import play.mvc.*;

import java.util.*;
import play.data.validation.Required;
import play.data.validation.URL;

public class Relevance extends Controller {

    public static void index(@Required @URL String url, @Required List<String> keygroups) {
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
                HashMap<String, String> groupsPages = RelevantPageFinder.findPagesForGroups(url, keygroups);
                renderJSON(groupsPages);
            } catch (Exception ex) {
                ex.printStackTrace();
                renderJSON("{ _error: \"" + ex.toString() + "\" }");
            }
        }
    }
}