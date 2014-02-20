/**
 * Copyright 2013 Chris Wood
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ceperman.pcsecrets;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * Handle export operations.
 * 
 * @author Chris Wood
 */
public class FileExporter {

   private static Logger logger = Logger.getLogger(FileExporter.class.getName());
   
   private static final String CSVEXT = "csv";
   private static final String JSONEXT = "json";
   private static final String XMLEXT = "xml";
   
   private final String[] column_names = { Constants.COL_DESCRIPTION, Constants.COL_USERNAME, Constants.COL_PASSWORD,
                                                               Constants.COL_EMAIL, Constants.COL_NOTES, Constants.COL_TIMESTAMP };
   
   /**
    * Export secrets.
    * Here handle selecting the output file name and location, and the export
    * format.
    * 
    * @param mainWindow
    */
   public void export(MainWindow mainWindow) {
      File file = null;
      final JFileChooser fc = new JFileChooser();
      fc.setDialogTitle(Messages.getString("DataHandler.exportchoosertitle"));
      FileNameExtensionFilter filter = new FileNameExtensionFilter(Messages.getString("DataHandler.csvfile"), CSVEXT);
      fc.addChoosableFileFilter(filter);
      fc.setFileFilter(filter); // set selected filter
      fc.addChoosableFileFilter(new FileNameExtensionFilter(Messages.getString("DataHandler.jsonfile"), JSONEXT));
      fc.addChoosableFileFilter(new FileNameExtensionFilter(Messages.getString("DataHandler.xmlfile"), XMLEXT));
      // the following gets ignored by the L&F, but maybe some day it won't
      fc.setApproveButtonText(Messages.getString("DataHandler.exportconfirmbutton"));
      int rc = fc.showSaveDialog(mainWindow);
      while (rc == JFileChooser.APPROVE_OPTION) {
         file = fc.getSelectedFile();
         filter = (FileNameExtensionFilter)fc.getFileFilter();
         String ext = (filter.getExtensions())[0];
         if(!file.getAbsolutePath().endsWith(ext)){
             file = new File(fc.getSelectedFile() + "." + ext);
         }
         boolean overwrite = false;
         if (file.exists()) {
            int choice = JOptionPane.showConfirmDialog(
                  mainWindow,
                  Messages.getString("DataHandler.exportoverwritequestion"),
                  Messages.getString("DataHandler.exportchoosertitle"),
                   JOptionPane.YES_NO_OPTION);
            if (choice == 0) {
               overwrite = true;
            }
         }
         if (!(file.exists()) || overwrite == true) {
            logger.log(Level.INFO, "export: File name is " + file.getName());
            if (ext.equals(CSVEXT)) {
               exportCSV(mainWindow,file);
            } else if (ext.equals(JSONEXT)) {
               exportJSON(mainWindow,file);
            } else if (ext.equals(XMLEXT)) {
               exportXML(mainWindow,file);
            }
            return;
         }
         rc = fc.showOpenDialog(mainWindow);
      }
   }

   /*
    * Export CSV data
    * @param mainWindow
    * @param file
    */
   private void exportCSV(MainWindow mainWindow, File file) {
      FileWriter fw = null;
      SecretsCollection listModel = mainWindow.getListModel();
      
      try {
         fw = new FileWriter(file);
         CSVWriter csvw = new CSVWriter(fw);
         /* write CSV header */
         csvw.writeNext(column_names);
         /* write all the secrets */
         for (int i = 0; i < listModel.getSize(); i++) {
            HostSecret hs = (HostSecret) listModel.getElementAt(i);
            csvw.writeNext(new String[] { hs.getDescription(), hs.getUsername(), hs.getPassword(), hs.getEmail(), hs.getNote(), hs.getFormattedTimestamp() });
         }  
         mainWindow.getAreaMsg().setText(MessageFormat.format(Messages.getString("DataHandler.exported"), listModel.getSize()));
      } catch (IOException e) {
         String errorMsg = MessageFormat.format(Messages.getString("DataHandler.exporterrortext"), file.getName(), e.getClass().getName());
         JOptionPane.showMessageDialog(mainWindow, errorMsg, Messages.getString("DataHandler.exporterrordialogtitle"), JOptionPane.ERROR_MESSAGE);
      } finally {
         try {
            fw.close();
         } catch (IOException e) {} /* ignore any problem here */
      }
   }
   
   /*
    * Export JSON data
    * @param mainWindow
    * @param file
    */
   private void exportJSON(MainWindow mainWindow, File file) {
      FileWriter fw = null;
      SecretsCollection listModel = mainWindow.getListModel();
      try {
         fw = new FileWriter(file);
         JSONObject jsonSecrets = getJSONData(listModel);
         fw.write(jsonSecrets.toString());
         mainWindow.getAreaMsg().setText(MessageFormat.format(Messages.getString("DataHandler.exported"), listModel.getSize()));
      } catch (Exception e) {
         String errorMsg = MessageFormat.format(Messages.getString("DataHandler.exporterrortext"), file.getName(), e.getClass().getName());
         JOptionPane.showMessageDialog(mainWindow, errorMsg, Messages.getString("DataHandler.exporterrordialogtitle"), JOptionPane.ERROR_MESSAGE);
      } finally {
         try {
            fw.close();
         } catch (IOException e) {} /* ignore any problem here */
      }  
   }
   
   /*
    * Export XML data
    * @param mainWindow
    * @param file
    */
   private void exportXML(MainWindow mainWindow, File file) {
      FileWriter fw = null;
      SecretsCollection listModel = mainWindow.getListModel();
      try {
         fw = new FileWriter(file);
         JSONObject jsonSecrets = getJSONData(listModel);
         fw.write(toXMLString(jsonSecrets));
         mainWindow.getAreaMsg().setText(MessageFormat.format(Messages.getString("DataHandler.exported"), listModel.getSize()));
      } catch (Exception e) {
         String errorMsg = MessageFormat.format(Messages.getString("DataHandler.exporterrortext"), file.getName(), e.getClass().getName());
         JOptionPane.showMessageDialog(mainWindow, errorMsg, Messages.getString("DataHandler.exporterrordialogtitle"), JOptionPane.ERROR_MESSAGE);
      } finally {
         try {
            fw.close();
         } catch (IOException e) {} /* ignore any problem here */
      }  
   }
   
   private JSONObject getJSONData(SecretsCollection listModel) throws JSONException {
      final String SECRET_FIELD_NAME = "secret";
      final String SECRETS_FIELD_NAME = "secrets";
      JSONObject jsonSecrets = new JSONObject();
      JSONArray jsonArray = new JSONArray();
      for (int i = 0; i < listModel.getSize(); i++) {
         HostSecret hs = (HostSecret) listModel.getElementAt(i);
         JSONObject jo = new JSONObject();
         jo.put(SECRET_FIELD_NAME, hs.toJSON());
         jsonArray.put(jo);
      }
      jsonSecrets.put(SECRETS_FIELD_NAME, jsonArray);
      return jsonSecrets;
   }
   
   /* Following two methods derived from JSON.org XML class methods.
    * JSON.org copyright notice is included
    */
   
   /*
   Copyright (c) 2002 JSON.org

   Permission is hereby granted, free of charge, to any person obtaining a copy
   of this software and associated documentation files (the "Software"), to deal
   in the Software without restriction, including without limitation the rights
   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
   copies of the Software, and to permit persons to whom the Software is
   furnished to do so, subject to the following conditions:

   The above copyright notice and this permission notice shall be included in all
   copies or substantial portions of the Software. */
   
   /**
    * Convert a JSONObject into a well-formed, element-normal XML string.
    * 
    * @param object A JSONObject.
    * @return  A string.
    * @throws  JSONException
    */
   public String toXMLString(Object object) throws JSONException {
       return toXMLString(object, "", null);
   }

   /**
    * Convert a JSONObject into a well-formed, element-normal XML string.
    * 
    * The modifications are:
    * - assumes arrays are homogeneous and elements can be emitted together
    * - uses new line and indentation formatting 
    *   
    * @param object A JSONObject.
    * @param tagName The optional name of the enclosing tag.
    * @return A string.
    * @throws JSONException
    */
   @SuppressWarnings("rawtypes")
   private String toXMLString(Object object, String indent, String tagName)
           throws JSONException {
       StringBuffer sb = new StringBuffer();
       int          i;
       JSONArray    ja;
       JSONObject   jo;
       String       key;
       Iterator     keys;
       int          length;
       String       string;
       Object       value;
       
       if (object instanceof JSONObject) {

//Emit <tagName>

           if (tagName != null) {
               sb.append(indent);
               sb.append('<');
               sb.append(tagName);
               sb.append(">\n");
           }

//Loop thru the keys.

           jo = (JSONObject)object;
           keys = jo.keys();
           while (keys.hasNext()) {
               key = keys.next().toString();
               value = jo.opt(key);
               if (value == null) {
                   value = "";
               }
               if (value instanceof String) {
                   string = (String)value;
               } else {
                   string = null;
               }

//Emit content in body

               if ("content".equals(key)) {
                   if (value instanceof JSONArray) {
                       ja = (JSONArray)value;
                       length = ja.length();
                       for (i = 0; i < length; i += 1) {
                           if (i > 0) {
                               sb.append('\n');
                           }
                           sb.append(XML.escape(ja.get(i).toString()));
                       }
                   } else {
                       sb.append(XML.escape(value.toString()));
                   }

//Emit an array of similar keys

               } else if (value instanceof JSONArray) {
                  ja = (JSONArray)value;
                  length = ja.length();
                  sb.append(indent);
                  sb.append('<');
                  sb.append(key);
                  sb.append(">\n");
                  for (i = 0; i < length; i += 1) {
                      value = ja.get(i);
                      sb.append(toXMLString(value, indent + "  ", null));
                  }
                  sb.append(indent);
                  sb.append("</");
                  sb.append(key);
                  sb.append(">\n");
               } else if ("".equals(value)) {
                   sb.append(indent + "  ");
                   sb.append('<');
                   sb.append(key);
                   sb.append("/>\n");

//Emit a new tag <k>

               } else {
                   sb.append(toXMLString(value, indent + "  ", key));
               }
           }
           if (tagName != null) {

//Emit the </tagname> close tag

               sb.append(indent);
               sb.append("</");
               sb.append(tagName);
               sb.append(">\n");
           }
           return sb.toString();

//XML does not have good support for arrays. If an array appears in a place
//where XML is lacking, synthesize an <array> element.

       } else {
           if (object.getClass().isArray()) {
               object = new JSONArray(object);
           }
           if (object instanceof JSONArray) {
               ja = (JSONArray)object;
               length = ja.length();
               for (i = 0; i < length; i += 1) {
                   sb.append(toXMLString(ja.opt(i), indent, tagName == null ? "array" : tagName));
               }
               return sb.toString();
           } else {
               string = (object == null) ? "null" : XML.escape(object.toString());
               return (tagName == null) ? "\"" + string + "\"" :
                   (string.length() == 0) ? indent + "<" + tagName + "/>\n" :
                      indent + "<" + tagName + ">" + string + "</" + tagName + ">\n";
           }
       }
   }
}
