/**
 * 
 */
package com.ceperman.pcsecrets.test;

import org.json.JSONException;
import org.json.JSONObject;

import junit.framework.TestCase;

/**
 * Test JSON string escape behaviour
 * 
 * @author Chris Wood
 */
public class JSONTest extends TestCase {

  /**
   * @throws JSONException
   */
  public void testEscape() throws JSONException {
    String jsonString = null;
    JSONObject jsonThing = new JSONObject();
    jsonThing.put("a", "string with characters like [ ] \" , : \\ won't mess things up?");
    jsonThing.put("b", "Embedded cc: \n");
    jsonThing.put("c", "Embedded cc: [{}");
    jsonThing.put("d", "Embedded cc: ]\"'");
    jsonString = jsonThing.toString();
    System.out.println(jsonString);
    jsonThing = new JSONObject(jsonString);
    System.out.println("a - " + jsonThing.getString("a"));
    System.out.println("b - " + jsonThing.getString("b"));
    System.out.println("c - " + jsonThing.getString("c"));
    System.out.println("d - " + jsonThing.getString("d"));
  }
  
}
