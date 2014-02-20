/**
 * 
 */
package com.ceperman.pcsecrets;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

/**
 *
 * @author Chris Wood
 */
public class JSONToXMLTest {

   /**
    * @param args
    * @throws JSONException 
    */
   public static void main(String[] args) throws JSONException {
      String jsonString = "{ \"group\" : [ { \"a\" : 1 }, { \"a\" : 2 } ] }";
//      String jsonString = "{ \"group\" : [ { \"a\" : [{ \"b\" : 1 }, { \"b\" : 2 }] }, { \"a\" : [{ \"b\" : 3 }, { \"b\" : 4 }] } ] }";
      JSONObject jo = new JSONObject(jsonString);
      System.out.println(jo.toString());
      String xmlString = XML.toString(jo);
      System.out.println(xmlString);
      jo = XML.toJSONObject(xmlString);
      System.out.println(jo.toString());
      jo = XML.toJSONObject("<group><a>1</a><a>2</a></group>");
      System.out.println(jo.toString());
      xmlString = XML.toString(jo);
      System.out.println(xmlString);
   }

}
