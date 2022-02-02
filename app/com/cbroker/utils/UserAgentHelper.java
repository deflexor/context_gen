/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cbroker.utils;

import java.io.IOException;

import org.jsoup.Connection.Response;
import static org.jsoup.Connection.Method.GET;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 *
 * @author dfr
 */
public class UserAgentHelper {

    public static Document fetchURIasDocument(String uri) throws IOException  {
        Response response = Jsoup.connect(uri).timeout(2000).method(GET).execute();
        if(response.statusCode() != 200) {
            throw new IOException("response status not ok: " + response.statusMessage());
        }
        return response.parse();
    }
    
    /*
     * 
    public static Document fetchURIasDOM(String uri) throws SAXException, IOException {
    // fetch document
    HttpResponse resp = WS.url(uri).timeout("5s").followRedirects(true).get();
    
    // detect charset
    String ctype = resp.getHeader("Content-Type");
    if(!ctype.startsWith("text/html")) {
    return null;
    }
    String charset = ctype.substring( ctype.indexOf(CHARSET_HEADER_PART) + CHARSET_HEADER_PART.length());
    try { 
    Charset.isSupported(charset);
    }
    catch (Exception e) {
    charset = "UTF-8";
    }
    
    // parse html document into DOM tree
    SimpleUserAgentContext context = new SimpleUserAgentContext();
    context.setScriptingEnabled(false);
    context.setExternalCSSEnabled(false);
    DocumentBuilderImpl dbi = new DocumentBuilderImpl(context);
    
    Document doc = dbi.parse(new InputSourceImpl(resp.getStream(), uri, charset));
    return doc;
    }
     * 
     */
}
