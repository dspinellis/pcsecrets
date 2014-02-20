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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.UIDefaults;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import au.com.bytecode.opencsv.CSVReader;

import com.jayway.jsonpath.JsonPath;

/**
 * Import secrets from external data.
 * 
 * @author Chris Wood
 */
public class FileImporter {
   private static Logger logger = Logger.getLogger(FileImporter.class.getName());
   
   private static final String SECRETS_FIELD_NAME = "secrets";
   private static final String CSVEXT = "csv";
   private static final String JSONEXT = "json";
   private static final String XMLEXT = "xml";
   
   private String[] columnNames;
   private JSONObject data = null;

   private File importFile;
   
   private int fileType = TYPE_UNKNOWN;
   private int dataType = TYPE_UNKNOWN;
   
   private static int TYPE_UNKNOWN = 0;
   private static int TYPE_CSV = 1;
   private static int TYPE_JSON = 2;
   private static int TYPE_XML = 2;
   
   private boolean hasDataHeader = false; // assume no data header
   private boolean useDataHeader = true; // use data header if present
   
   private MainWindow mainWindow;
   
   private final String[] secretFields = { Constants.COL_DESCRIPTION, Constants.COL_USERNAME, Constants.COL_PASSWORD,
               Constants.COL_EMAIL, Constants.COL_NOTES, Constants.COL_TIMESTAMP };

   private String[] mappingColumnNames = { Messages.getString("FileImporter.secretfield"), Messages.getString("FileImporter.mappedto")};

   private Object[][] mappingTableData;
   
   /* Mapping dialog fields */
   private JDialog dialog;
   private JLabel columnInfoText = new JLabel("", JLabel.LEFT);
   private JTable columnTable;
   private JLabel hdrInfoText = new JLabel("", JLabel.LEFT);
   private JTable mappingTable;
   private String[] comboFields;
   private JRadioButton replaceDupButton;
   private JRadioButton ignoreDupButton;
   
   /* Subset dialog fields */
   private JButton continuebtn;
   
   private List<String> candidatePaths;

   /**
    * Handle the import
    * 
    * @param mainWindow
    */
   public void handleImport(MainWindow mainWindow) {
      this.mainWindow = mainWindow;
      final JFileChooser fc = new JFileChooser();
      fc.setDialogTitle(Messages.getString("DataHandler.importchoosertitle"));
      FileNameExtensionFilter filter = new FileNameExtensionFilter(Messages.getString("DataHandler.csvfile"), CSVEXT);
      fc.addChoosableFileFilter(filter);
      fc.setFileFilter(filter);
      fc.addChoosableFileFilter(new FileNameExtensionFilter(Messages.getString("DataHandler.jsonfile"), JSONEXT));
      fc.addChoosableFileFilter(new FileNameExtensionFilter(Messages.getString("DataHandler.xmlfile"), XMLEXT));

      int rc = fc.showOpenDialog(mainWindow);
      if (rc != JFileChooser.APPROVE_OPTION) {
         return;
      }
      importFile = fc.getSelectedFile();
      String fileName = importFile.getName();
      logger.log(Level.FINE, "FileImporter: Selected file name is " + fileName);
      int index = fileName.lastIndexOf(".");
      if (index > 0 && index < fileName.length()-1) {
         String ext = fileName.substring(index+1);
         if (ext.equals(CSVEXT)) {
            fileType = TYPE_CSV;
         } else if (ext.equals(JSONEXT)) {
            fileType = TYPE_JSON;
         } else if (ext.equals(XMLEXT)) {
            fileType = TYPE_XML;
         }
      }
      
      // determine the data format - don't rely on the file extension, which
      // could be missing or wrong
      try {
         String[] record1 = getFirstRecord(importFile);
         if (record1 == null) {
            JOptionPane.showMessageDialog(mainWindow, Messages.getString("FileImporter.emptyfile"),
                        Messages.getString("FileImporter.title"), JOptionPane.INFORMATION_MESSAGE);
            return;
         }
         if (record1[0].startsWith("[") || record1[0].startsWith("{") || record1[0].startsWith("<")) {
            // this is "structured" data
            if (record1[0].startsWith("[") || record1[0].startsWith("{")) {
               dataType = TYPE_JSON;
               hasDataHeader = true; // JSON always has field names
               data = getJSONData(importFile);
            } else {
               dataType = TYPE_XML;
               hasDataHeader = true; // XML always has field names
               data = getXMLData(importFile);
            }
         } else {
            // treat as CSV
            if (fileType == TYPE_JSON) {
               JOptionPane.showMessageDialog(mainWindow, Messages.getString("FileImporter.notjsonfile"),
                           Messages.getString("FileImporter.errordialogtitle"), JOptionPane.ERROR_MESSAGE);
               return;
            } else if (fileType == TYPE_XML) {
               JOptionPane.showMessageDialog(mainWindow, Messages.getString("FileImporter.notxmlfile"),
                           Messages.getString("FileImporter.errordialogtitle"), JOptionPane.ERROR_MESSAGE);
               return;
            }
            dataType = TYPE_CSV;
            hasDataHeader = guessHeader(record1);
            showCSVHeaderDialog(mainWindow, record1);
            data = getCSVData(importFile);
         }
         
         if (data == null || data.length() == 0) {
            JOptionPane.showMessageDialog(mainWindow, Messages.getString("FileImporter.nodata"),
                        Messages.getString("FileImporter.title"), JOptionPane.INFORMATION_MESSAGE);
            return;
         }
         
         if (dataType == TYPE_JSON || dataType == TYPE_XML) {
            // analyse for a suitable subset
            candidatePaths = new ArrayList<String>();
            findPrimitiveObjects(data, "$", false);
            logger.log(Level.FINE, Arrays.toString(candidatePaths.toArray()));
            if (candidatePaths.size() == 0) {
               JOptionPane.showMessageDialog(mainWindow, Messages.getString("FileImporter.nousefuldata"),
                           Messages.getString("FileImporter.title"), JOptionPane.INFORMATION_MESSAGE);
               return;
            }
            showSubsetSelectionDialog(mainWindow);
            if (data == null) {
               return;
            }
         }
         
         initMappingValues();
         showMappingDialog(mainWindow);
         
      } catch (Throwable e) {
         String errorMsg = MessageFormat.format(Messages.getString("FileImporter.openerrortext"), importFile.getName(), e);
         logger.log(Level.WARNING, errorMsg);
         JOptionPane.showMessageDialog(mainWindow, errorMsg, Messages.getString("FileImporter.errordialogtitle"), JOptionPane.ERROR_MESSAGE);
      }
   }
   
   /*
    * Ask the user about handling the CSV header, if any
    * 
    * @param parent
    * @param record1 
    */
   private void showCSVHeaderDialog(JFrame parent, String[] record1) {
      /* create columns pane and add to content pane */
      JPanel columnsPane = UIUtils.createTitledBorderedPanel(Messages.getString("FileImporter.csvhdrboxtitle"));
      columnsPane.setLayout(new BoxLayout(columnsPane, BoxLayout.PAGE_AXIS));
      columnInfoText.setAlignmentX(Component.LEFT_ALIGNMENT);
      columnInfoText.setText(MessageFormat.format(Messages.getString("FileImporter.columnInfoText"), record1.length));
      columnsPane.add(columnInfoText);
      JPanel columnTablePane = new JPanel();
      columnTablePane.setBorder(BorderFactory.createCompoundBorder(
                  BorderFactory.createEmptyBorder(5,2,5,2), BorderFactory.createLineBorder(Color.gray)));
      columnTablePane.setAlignmentX(Component.LEFT_ALIGNMENT);
      columnsPane.add(columnTablePane);
      columnTablePane.setLayout(new BorderLayout());
      String[][] tableData = new String[1][];
      tableData[0] = record1;
      columnTable = new JTable(tableData, record1);
      columnTablePane.add(columnTable, BorderLayout.CENTER);
      columnInfoText.setFont(columnTable.getFont());
      
      hdrInfoText.setAlignmentX(Component.LEFT_ALIGNMENT);
      hdrInfoText.setFont(columnTable.getFont());
      columnsPane.add(hdrInfoText);
      
      JLabel noHdrInfoText = new JLabel(Messages.getString("FileImporter.noHdrInfoText"), JLabel.LEFT);
      noHdrInfoText.setBorder(BorderFactory.createEmptyBorder(10,0,0,0));
      noHdrInfoText.setFont(columnTable.getFont());
      columnsPane.add(noHdrInfoText);
      
      JPanel columnsPaneCheckboxArea = new JPanel(new FlowLayout(FlowLayout.LEFT));
      columnsPaneCheckboxArea.setAlignmentX(Component.LEFT_ALIGNMENT);
      JCheckBox hdrCheckbox = new JCheckBox();
      JLabel hdrCheckboxText = new JLabel(Messages.getString("FileImporter.usehdrcheckboxtext"), JLabel.LEFT);
      hdrCheckboxText.setFont(columnTable.getFont());
      columnsPane.add(columnsPaneCheckboxArea, BorderLayout.CENTER);
      columnsPaneCheckboxArea.add(hdrCheckbox);
      columnsPaneCheckboxArea.add(hdrCheckboxText);
      hdrCheckbox.addItemListener(new ItemListener() {
         public void itemStateChanged(ItemEvent event) {
            useDataHeader = ((JCheckBox)event.getSource()).isSelected();
         }
      });
      if (hasDataHeader) {
         hdrInfoText.setText(Messages.getString("FileImporter.okHdrInfoText"));
         hdrCheckbox.setSelected(true);
      } else {
         hdrInfoText.setText(Messages.getString("FileImporter.neverHdrInfoText"));
         hdrCheckbox.setEnabled(false);
         hdrCheckboxText.setEnabled(false);
      }
      
      String[] options = { Messages.getString("FileImporter.btncontinue") };
      JOptionPane optionPane = new JOptionPane(columnsPane,
                  JOptionPane.QUESTION_MESSAGE,
                  JOptionPane.OK_OPTION,
                  null,     //do not use a custom Icon
                  options,  //the titles of buttons
                  options[0]); //default button
      JDialog dialog = optionPane.createDialog(parent, Messages.getString("FileImporter.title"));
      dialog.pack();
      dialog.setModalityType(ModalityType.APPLICATION_MODAL);
      dialog.setVisible(true);
   }
   
   /*
    * Show data subset selection dialog
    */
   @SuppressWarnings("serial")
   private void showSubsetSelectionDialog(JFrame parent) {
      dialog = new JDialog();
      dialog.setTitle(Messages.getString("FileImporter.subsetdialogtitle"));
      dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
      Container cp = dialog.getContentPane();
      cp.setLayout(new BorderLayout());
      
      JPanel mainPane = new JPanel(new BorderLayout());
      mainPane.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
      cp.add(mainPane, BorderLayout.CENTER);
      
      /* dialog will have a main centre pane and a lower button pane */
      
      /* create top pane and add to top of main pane */
      JPanel topPane = new JPanel(new BorderLayout());
      mainPane.add(topPane, BorderLayout.NORTH);
      
      /* subsetting info text */
      JTextArea subsettingInfo = new JTextArea(Messages.getString("FileImporter.subsetinfo"));
      subsettingInfo.setEditable(false);
      subsettingInfo.setLineWrap(true);
      subsettingInfo.setWrapStyleWord(true);
      subsettingInfo.setBackground(mainPane.getBackground());
      
      /* Hack for Nimbus LAF to force correct background colour
       * Only works for Java 1.6.10+ when the Nimbus LAF was moved to the javax package - before it was
       * located in com.sun package.
       * We have to dynamically load the painter class otherwise NoClassDefFound error on earlier Java
       */
      UIDefaults defaults = new UIDefaults();
      try {
         NimbusBGPainter myBgPainter = (NimbusBGPainter)Class.forName("com.ceperman.pcsecrets.NimbusBGPainter").newInstance();
         defaults.put("TextArea[Enabled+NotInScrollPane].backgroundPainter", myBgPainter);
         subsettingInfo.putClientProperty("Nimbus.Overrides", defaults);
         subsettingInfo.putClientProperty("Nimbus.Overrides.InheritDefaults", true);
      } catch (Throwable e) {
         // can't load the class so forget it
         logger.log(Level.FINE, "Cannot load com.ceperman.pcsecrets.NimbusBGPainter");
      }
      
      subsettingInfo.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
      topPane.add(subsettingInfo, BorderLayout.CENTER);
      
      /* selection pane */
      JPanel selectionPane = new JPanel(new BorderLayout());
      selectionPane.setBorder(BorderFactory.createLineBorder(Color.gray));
      mainPane.add(selectionPane, BorderLayout.CENTER);
      
      String[] elements = new String[candidatePaths.size()];
      String[] fields = new String[candidatePaths.size()];
      for (int i = 0; i < elements.length; i++) {
         String path = candidatePaths.get(i);
         elements[i] = getElementNameFromPath(path);
         fields[i] = Arrays.toString(getObjectFields(path));
      }
      
      Action showDetails = new AbstractAction() {
         @Override
         public void actionPerformed(ActionEvent event) {
            int row = Integer.parseInt(event.getActionCommand());
            String path = candidatePaths.get(row);
            String fields = Arrays.toString(getObjectFields(path));
            int elementCount = 0;
            net.minidev.json.JSONArray jsonSArray = JsonPath.read(data.toString(), candidatePaths.get(row));
            elementCount = jsonSArray.size();
            
            JPanel msgPane = new JPanel();
            msgPane.setLayout(new BoxLayout(msgPane, BoxLayout.PAGE_AXIS));
            msgPane.add(new JLabel(Messages.getString("FileImporter.subsetmoreinfopath")));
            msgPane.add(new JLabel(" " + path));
            msgPane.add(new JLabel(" "));
            msgPane.add(new JLabel(Messages.getString("FileImporter.subsetmoreinfofields")));
            msgPane.add(new JLabel(" " + fields));
            msgPane.add(new JLabel(" "));
            msgPane.add(new JLabel(Messages.getString("FileImporter.subsetmoreinfoelementcount") + " " + elementCount));
            JOptionPane.showMessageDialog(mainWindow, msgPane, Messages.getString("FileImporter.subsetmoreinfotitle"),
                        JOptionPane.PLAIN_MESSAGE);
         }
      };
      
      final SubsetTableModel tableModel = new SubsetTableModel(elements, fields);
      JTable selectionTable = new JTable(tableModel);
      tableModel.setColumnWidths(selectionTable.getColumnModel());
      new ButtonColumn(selectionTable, showDetails , 3);
      selectionPane.add(selectionTable, BorderLayout.SOUTH);
      /* for table not in a JScrollPane the header must be explicitly shown */
      selectionPane.add(selectionTable.getTableHeader(), BorderLayout.CENTER);
      
      /* create button pane and add to bottom of main pane */
      JPanel buttonPane = new JPanel();
      mainPane.add(buttonPane, BorderLayout.SOUTH);
      buttonPane.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0)); // create top padding
      /* create contents of button pane */
      continuebtn = new JButton(Messages.getString("FileImporter.btncontinue"));
      continuebtn.setEnabled(false);
      continuebtn.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent event) {
            try {
               int index = tableModel.getSelectedIndex();
               net.minidev.json.JSONArray jsonSArray = JsonPath.read(data.toString(), candidatePaths.get(index));
               JSONArray jsonArray = new JSONArray(jsonSArray.toJSONString());
               logger.log(Level.FINE, jsonArray.toString());
               // extract "column" names from first object
               JSONObject jsonObject = jsonArray.getJSONObject(0);
               columnNames = JSONObject.getNames(jsonObject);
               data = new JSONObject();
               data.put(SECRETS_FIELD_NAME, jsonArray);
            } catch (JSONException e) {
               logger.log(Level.SEVERE, "showSubsetSelectionDialog: exception " + e);
            }
            dialog.dispose();
         }
      });
      buttonPane.add(continuebtn);
      JButton cancelButton = new JButton(Messages.getString("PCSecrets.cancel"));
      cancelButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent event) {
            data = null; // abandon import
            mainWindow.getAreaMsg().setText(Messages.getString("FileImporter.importcancelled"));
            dialog.dispose();
         }});
      buttonPane.add(cancelButton);
      
      dialog.addWindowListener(new WindowAdapter() {
         public void windowClosing(WindowEvent e) {
            data = null; // abandon import
            mainWindow.getAreaMsg().setText(Messages.getString("FileImporter.importcancelled"));
            dialog.dispose();
         }
      });
      dialog.pack();
      dialog.setSize(dialog.getPreferredSize()); // JTextArea bug - does not return correct preferredSize when linewrap used
      dialog.setResizable(false);
//      dialog.setMinimumSize(dialog.getSize());
      dialog.setLocationRelativeTo(parent);
      dialog.setModalityType(ModalityType.APPLICATION_MODAL);
      dialog.setVisible(true);
   }
   
   private String getElementNameFromPath(String path) {
      int dotIndex = path.lastIndexOf(".");
      if (dotIndex < 0) {
         return path;
      } else {
         String lastField = path.substring(dotIndex + 1);
         int bracketIndex = lastField.lastIndexOf("[");
         if (bracketIndex < 0) {
            bracketIndex = lastField.length();
         }
         return lastField.substring(0, bracketIndex);
      }
   }
   
   /*
    * Model for the subsetting table
    */
   @SuppressWarnings("serial")
   private class SubsetTableModel extends AbstractTableModel {
      private String[] colNames = { Messages.getString("FileImporter.tbltxtselect"),
            Messages.getString("FileImporter.tbltxtelement"), Messages.getString("FileImporter.tbltxtfields"), "" };
      Object[][] tableData = null;
      
      public SubsetTableModel(String[] elements, String[] fields) {
         tableData = new Object[elements.length][4];
         for (int i = 0; i < elements.length; i++) {
            tableData[i][0] = new Boolean(false);
            tableData[i][1] = elements[i];
            tableData[i][2] = fields[i];
            tableData[i][3] = Messages.getString("FileImporter.tbltxtdetails");
         }
      }
      
      public void setColumnWidths(TableColumnModel columnModel) {
         /* set column widths */
         int[] columnWeights = new int[]{ 10, 20, 55, 15 };
         for (int i = 0; i < columnModel.getColumnCount(); i++) {
            TableColumn column = columnModel.getColumn(i);
            column.setPreferredWidth(columnWeights[i] * 6);
         }
      }
      
      @Override
      public int getColumnCount() {
         return colNames.length;
     }

      @Override
      public int getRowCount() {
         return tableData.length;
      }

      @Override
      public Object getValueAt(int row, int col) {
         return tableData[row][col];
      }
      
      @Override
      public String getColumnName(int col) {
         return colNames[col];
      }

      @Override
      public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
         if (columnIndex == 0) {
            for (int i = 0; i < tableData.length; i++) {
               if (i == rowIndex) {
                  boolean newState = !((Boolean)tableData[i][0]).booleanValue();
                  tableData[i][0] = new Boolean(newState);
                  continuebtn.setEnabled(newState);
               } else {
                  tableData[i][0] = new Boolean(false);
               }
            }
            fireTableDataChanged();
         }
      }

      /*
       * JTable uses this method to determine the default renderer/
       * editor for each cell.  If we didn't implement this method,
       * then the last column would contain text ("true"/"false"),
       * rather than a check box.
       */
      public Class<?> getColumnClass(int c) {
         return getValueAt(0, c).getClass();
      }

      @Override
      public boolean isCellEditable(int rowIndex, int columnIndex) {
         if (columnIndex == 0 || columnIndex == 3) return true;
         return true;
      }
      
      public int getSelectedIndex() {
         int retval = 0;
         for (int i = 0; i < tableData.length; i++) {
            if (((Boolean)tableData[i][0]).booleanValue()) {
               retval = i;
            }
         }
         return retval;
      }
   }   
   
   /*
    * Show mapping dialog
    * 
    * @param parent
    */
   private void showMappingDialog(JFrame parent) {
      dialog = new JDialog();
      dialog.setTitle(Messages.getString("FileImporter.title"));
      dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
      Container cp = dialog.getContentPane();
      cp.setLayout(new BorderLayout());
      
      /* create center pane and add to content pane */
      JPanel centerPane = new JPanel();
      centerPane.setLayout(new BorderLayout());
      cp.add(centerPane, BorderLayout.CENTER);

      /* create mapping pane and add to center pane */
      JPanel mappingPane = UIUtils.createTitledBorderedPanel(Messages.getString("FileImporter.mappingsectiontitle"));
      centerPane.add(mappingPane, BorderLayout.NORTH);
      mappingPane.setLayout(new BorderLayout());
      /* mapping info text */
      JTextArea mappingInfo = new JTextArea(Messages.getString("FileImporter.mappinginfo"));
      mappingInfo.setEditable(false);
      mappingInfo.setLineWrap(true);
      mappingInfo.setWrapStyleWord(true);
      mappingInfo.setBackground(centerPane.getBackground());
      
      /* Hack for Nimbus LAF to force correct background colour
       * Only works for Java 1.6.10+ when the Nimbus LAF was moved to the javax package - before it was
       * located in com.sun package.
       * We have to dynamically load the painter class otherwise NoClassDefFound error on earlier Java
       */
      UIDefaults defaults = new UIDefaults();
      try {
         NimbusBGPainter myBgPainter = (NimbusBGPainter)Class.forName("com.ceperman.pcsecrets.NimbusBGPainter").newInstance();
         defaults.put("TextArea[Enabled+NotInScrollPane].backgroundPainter", myBgPainter);
         mappingInfo.putClientProperty("Nimbus.Overrides", defaults);
         mappingInfo.putClientProperty("Nimbus.Overrides.InheritDefaults", true);
      } catch (Throwable e) {
         // can't load the class so forget it
         logger.log(Level.FINE, "Cannot load com.ceperman.pcsecrets.NimbusBGPainter");
      }
      
      mappingInfo.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
      mappingPane.add(mappingInfo, BorderLayout.NORTH);

      /* create a panel so the table can have a border */
      JPanel tablePane = new JPanel(new BorderLayout());
      tablePane.setBorder(BorderFactory.createLineBorder(Color.gray));
      mappingPane.add(tablePane, BorderLayout.CENTER);

      /* add table parts to the pane */
      tablePane.add(mappingTable, BorderLayout.CENTER);
      /* for table not in a JScrollPane the header must be explicitly shown */
      tablePane.add(mappingTable.getTableHeader(), BorderLayout.NORTH);
      
      JTextArea mappingInfo2 = new JTextArea(Messages.getString("FileImporter.mappinginfo2"));
      mappingInfo2.setEditable(false);
      mappingInfo2.setLineWrap(true);
      mappingInfo2.setWrapStyleWord(true);
      mappingInfo2.setBackground(centerPane.getBackground());
      
      // hack for Nimbus LAF
      mappingInfo2.putClientProperty("Nimbus.Overrides", defaults);
      mappingInfo2.putClientProperty("Nimbus.Overrides.InheritDefaults", true);
      
      mappingInfo2.setBorder(BorderFactory.createEmptyBorder(10, 0, 5, 0));
      mappingPane.add(mappingInfo2, BorderLayout.SOUTH);

      /* create duplicates pane and add to center pane */
      JPanel duplicatesPane = UIUtils.createTitledBorderedPanel(Messages.getString("FileImporter.dupstitle"));
      centerPane.add(duplicatesPane, BorderLayout.SOUTH);
      duplicatesPane.setLayout(new BorderLayout());

      /* create the dup radio buttons */
      replaceDupButton = new JRadioButton(Messages.getString("FileImporter.replacebtn"));
      ignoreDupButton = new JRadioButton(Messages.getString("FileImporter.ignorebtn"));
      ignoreDupButton.setSelected(true);

      ButtonGroup radioButtons = new ButtonGroup();
      radioButtons.add(replaceDupButton);
      radioButtons.add(ignoreDupButton);

      duplicatesPane.add(replaceDupButton, BorderLayout.NORTH);
      duplicatesPane.add(ignoreDupButton, BorderLayout.SOUTH);

      /* create import button and add to content pane */
      JPanel buttonPane = new JPanel();
      cp.add(buttonPane, BorderLayout.SOUTH);
      JButton importButton = new JButton(Messages.getString("FileImporter.import"));
      importButton.setActionCommand(Constants.IMPORT);
      importButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            // check that description is mapped
            if (((String)mappingTableData[0][1]).equals(Messages.getString("FileImporter.notmapped"))) {
               JOptionPane.showMessageDialog(dialog, Messages.getString("FileImporter.importerrornodescriptiontext"),
                           Messages.getString("FileImporter.importerrordialogtitle"), JOptionPane.ERROR_MESSAGE);
               return;
            }
            doImport(data);
            dialog.dispose();
         }
      });
      buttonPane.add(importButton);
      JButton cancelButton = new JButton(Messages.getString("PCSecrets.cancel"));
      cancelButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent event) {
            mainWindow.getAreaMsg().setText(Messages.getString("FileImporter.importcancelled"));
            dialog.dispose();
         }});
      buttonPane.add(cancelButton);
      
      dialog.addWindowListener(new WindowAdapter() {
         public void windowClosing(WindowEvent e) {
            mainWindow.getAreaMsg().setText(Messages.getString("FileImporter.importcancelled"));
            dialog.dispose();
         }
      });

      dialog.setMinimumSize(new Dimension(500, 350));
      dialog.pack();
      dialog.setSize(dialog.getPreferredSize()); // JTextArea bug - does not return correct preferredSize when linewrap used
      dialog.setLocationRelativeTo(parent);
      dialog.setModalityType(ModalityType.APPLICATION_MODAL);
      dialog.setVisible(true);
   }

   /*
    * Initialise mapping table dialog values from the data:
    * - the first column values are set to the secrets field names
    * - the second column is set to same as the secrets field if an input field
    *   of that name exists. This creates an assumed mapping for the field. If
    *   one doesn't exist, the value is set to "-- none -" (not mapped).
    *   
    * The cell editor for the second column is set to a combo box containing
    * the input field names.
    */
   private void initMappingValues() {
      /* initialise the mapping table */
      mappingTableData = new Object[secretFields.length][2];
      mappingTable = new JTable(mappingTableData, mappingColumnNames);
      for (int i = 0; i < secretFields.length; i++) {
         mappingTableData[i][0] = secretFields[i];
         mappingTableData[i][1] = Messages.getString("FileImporter.notmapped");
      }

      /* initialise combo box fields */
      comboFields = new String[columnNames.length + 1];
      comboFields[0] = Messages.getString("FileImporter.notmapped");
      for (int i = 0; i < columnNames.length; i++) {
         comboFields[i + 1] = columnNames[i];
      }
      
      /* init the combo defaults */
      for (int i = 0; i < secretFields.length; i++) {
         if (columnNames.length == secretFields.length) {
            // if the number of columns match then assume a 1:1 relationship
            mappingTableData[i][1] = comboFields[i + 1];
         } else {
            // try to match names
            boolean mapped = false;
            for (int j = 0; j < columnNames.length; j++) {
               if (((String)mappingTableData[i][0]).equalsIgnoreCase(comboFields[j + 1])) {
                  mappingTableData[i][1] = comboFields[j + 1];
                  mapped = true;
               }
            }
            if (!mapped) {
               mappingTableData[i][1] = Messages.getString("FileImporter.notmapped");
            }
         }
      }
      
      /* create combo box for the mapping table column editor */
      JComboBox<String> comboBox = new JComboBox<String>(comboFields);
      TableColumn comboColumn = mappingTable.getColumnModel().getColumn(1);
      comboBox.setFont(mappingTable.getFont());
      comboColumn.setCellEditor(new DefaultCellEditor(comboBox));
   }

   /*
    * Get the first file record as a string array using the CSV reader
    */
   private String[] getFirstRecord(File file) throws IOException {
      String[] fieldValues = null;
      CSVReader csvr = null;
      try {
         if (file.length() == 0) {
            return null;
         }
         csvr = new CSVReader(new FileReader(file));
         fieldValues = csvr.readNext();
         if (fieldValues == null) {
            return null;
         }
      } catch (FileNotFoundException e) {
         throw new EOFException("File not found");
      } finally {
         if (csvr != null)
            try {
               csvr.close();
            } catch (IOException e) {} // ignore
      }

      return fieldValues;
   }

   /*
    * Guess if first record is a header. Each field must contain only alphanumberic
    * chars plus hyphen and underscore to be a valid header.
    */
   private boolean guessHeader(String[] firstRecord) {
      for (String string : firstRecord) {
         if (!string.matches("^[a-zA-Z0-9-_]+$")) return false;
      }
      return true;
   }

   /*
    * Get JSON data. The input file is not empty, but the json array might be.
    * Return JSONObject, or null if any error occurs
    */
   private JSONObject getJSONData(File file) {
      JSONObject retval = null;
      BufferedReader br = null;
      try {
         String stringData = null;
         {
            br = new BufferedReader(new FileReader(file));
            StringBuilder sb = new StringBuilder();
            // read the file into a single string
            // don't use readLine() in order to preserve line breaks
            int c = br.read();
            while (c != -1) {
               sb.append((char)c);
               c = br.read();
            }
            stringData = sb.toString();
         }
         if (stringData.startsWith("[")) {
            JSONArray jsonArray = new JSONArray(stringData);
            if (jsonArray.length() == 0) {
               // the json array is empty
               String errorTxt = Messages.getString("FileImporter.jsonemptyarray");
               logger.log(Level.WARNING, errorTxt);
               JOptionPane.showMessageDialog(mainWindow, errorTxt,
                           Messages.getString("FileImporter.jsonimporterrordialogtitle"), JOptionPane.ERROR_MESSAGE);
               return null;
            }
            // extract "column" names from first object
            JSONObject jsonObject = jsonArray.getJSONObject(0);
            columnNames = JSONObject.getNames(jsonObject);
            
            retval = new JSONObject();
            retval.put(SECRETS_FIELD_NAME, jsonArray);
         } else if (stringData.startsWith("{")) {
            retval = new JSONObject(stringData);
         }
      } catch (IOException e) {
         // use errorTxt if set, otherwise construct general text here
         String errorTxt = MessageFormat.format(Messages.getString("FileImporter.ioerrortext"), file.getName(), e.getClass()
                        .getName());
         JOptionPane.showMessageDialog(mainWindow, errorTxt,
                     Messages.getString("FileImporter.jsonimporterrordialogtitle"), JOptionPane.ERROR_MESSAGE);
         logger.log(Level.WARNING, "getJSONData; " + errorTxt);
      } catch (JSONException e) {
         // use errorTxt if set, otherwise construct general text here

         String errorTxt = MessageFormat.format(Messages.getString("FileImporter.jsonopenerrortext"), file.getName(), e.getClass()
                        .getName());
         JOptionPane.showMessageDialog(mainWindow, errorTxt,
                     Messages.getString("FileImporter.jsonimporterrordialogtitle"), JOptionPane.ERROR_MESSAGE);
         logger.log(Level.WARNING, "getJSONData; " + errorTxt);
      } finally {
         try {
            br.close();
         } catch (IOException e) {
            // ignore
         }
      }
      return retval;
   }
   
   /*
    * Get XML data. The input file is not empty, but the xml might be.
    * Convert to JSON object.
    * Return JSONObject, or null if any error occurs
    */
   private JSONObject getXMLData(File file) {
      BufferedReader br = null;
      String errorTxt = null;
      try {
         String stringData = null;
         {
            br = new BufferedReader(new FileReader(file));
            StringBuilder sb = new StringBuilder();
            // read the file into a single string
            // don't use readLine() in order to preserve line breaks
            int c = br.read();
            while (c != -1) {
               sb.append((char)c);
               c = br.read();
            }
            stringData = sb.toString();
         }
         
         /* XML.toJSONObject converts an empty element (e.g. <username/>) to an empty JSONObject,
          * which will in turn get converted to a string value of "{}" when we want "" (an empty
          * string). This is unwanted behaviour that we have to fix here.
          */
         JSONObject jo = XML.toJSONObject(stringData);
         fixupEmptyElements(jo);
         return jo;
      } catch (Exception e) {
      // use errorTxt if set, otherwise construct general text here
         if (errorTxt == null) {
            errorTxt = MessageFormat.format(Messages.getString("FileImporter.ioerrortext"), file.getName(), e.getClass()
                        .getName());
         }
         JOptionPane.showMessageDialog(mainWindow, errorTxt,
                     Messages.getString("FileImporter.jsonimporterrordialogtitle"), JOptionPane.ERROR_MESSAGE);
      } finally {
         try {
            br.close();
         } catch (IOException e) {
            // ignore
         }
      }
      return null;
   }
   
   /*
    * Get CSV data from the non-empty file and convert to a JSON array.
    * Return JSONObject or null if any error occurs
    */
   private JSONObject getCSVData(File file) {
      JSONObject retval = null;
      FileReader r = null;
      String errorTxt = null;
      try {
         JSONArray jsonArray = new JSONArray();
         String[] fieldValues = null;
         int fieldCount = 0;
         r = new FileReader(file);
         int recCount = 0;
         CSVReader csvr = new CSVReader(r);
         fieldValues = csvr.readNext();
         fieldCount = fieldValues.length;
         
         // set up field names
         if (hasDataHeader & useDataHeader) {
            columnNames = fieldValues;
            fieldValues = csvr.readNext();
            recCount++;
         } else {
            // create artificial field names
            columnNames = new String[fieldCount];
            for (int i = 0; i < fieldCount; i++) {
               columnNames[i] = "#" + (i + 1);
            }
         }
         
         // process all records
         while (!(fieldValues == null)) {
            if (fieldValues.length != fieldCount) {
               // problem if all records not same length
               errorTxt = MessageFormat.format(Messages.getString("FileImporter.csvfieldcounterror"), recCount+1);
               logger.log(Level.WARNING, errorTxt);
               throw new IOException();
            }
            recCount++;
            
            // create JSON object from record values
            JSONObject jsonObject = new JSONObject();
            for (int i = 0; i < fieldValues.length; i++) {
               jsonObject.put(columnNames[i], fieldValues[i]);
            }
            jsonArray.put(jsonObject);
            fieldValues = csvr.readNext();
         }
         retval = new JSONObject();
         retval.put(SECRETS_FIELD_NAME, jsonArray);
      } catch (Exception e) {
         // use errorTxt if set, otherwise construct general text here
         if (errorTxt == null) {
            errorTxt = MessageFormat.format(Messages.getString("FileImporter.ioerrortext"), file.getName(), e.getClass()
                        .getName());
         }
         JOptionPane.showMessageDialog(mainWindow, errorTxt,
                     Messages.getString("FileImporter.csvimporterrordialogtitle"), JOptionPane.ERROR_MESSAGE);
      } finally {
         try {
            r.close();
         } catch (IOException e) {
            // ignore
         }
      }
      return retval;
   }

   /*
    * Merge the imported JSON data into the secrets collection
    */
   private void doImport(JSONObject dataObject) {
      // create field correspondence map from user input
      Map<String, String> fieldMap = new HashMap<String, String>();
      for (int i = 0; i < secretFields.length; i++) {
         fieldMap.put(secretFields[i], (String)mappingTableData[i][1]);
      }
      int recCount = 0;
      int addedCount = 0;
      int dupCount = 0;
      try {
         JSONArray data = dataObject.getJSONArray(SECRETS_FIELD_NAME);
         for (int i = 0; i < data.length(); i++) {
            JSONObject jsonObject = data.getJSONObject(i);
            String[] values = new String[secretFields.length]; 
            for (int j = 0; j < values.length; j++) {
               // get the secrets field from the mapped field of the imported object
               values[j] = jsonObject.optString(fieldMap.get(secretFields[j]));
               if (j == 0) { // description
                  // ensure that key (description) has no newline chars
                  values[j] = values[j].split("\n")[0];
               }
            }
            logger.log(Level.FINE, "Values are '" + Arrays.toString(values));
            boolean toBeAdded = true;
            if (mainWindow.getListModel().contains(values[0])) {
               dupCount++;
               if (ignoreDupButton.isSelected()) {
                  toBeAdded = false;
                  logger.log(Level.FINE, "dup - ignore");
               } else {
                  logger.log(Level.FINE, "dup - replace");
               }
            }
            if (toBeAdded) {
               mainWindow.getListModel().addOrUpdate(new HostSecret(values));
               addedCount++;
               logger.log(Level.FINE, "added");
            }
         }

         mainWindow.getAreaMsg().setText(MessageFormat.format(Messages.getString("FileImporter.imported"), 
                     recCount, addedCount, dupCount, (ignoreDupButton.isSelected() ? Messages.getString("FileImporter.ignored"):
                        Messages.getString("FileImporter.replaced"))));

      } catch (Exception e) {
         String errorMsg = MessageFormat.format(Messages.getString("FileImporter.importgeneralerror"), importFile.getName(), e.getClass().getName());
         JOptionPane.showMessageDialog(dialog, errorMsg, Messages.getString("FileImporter.importerrordialogtitle"), JOptionPane.ERROR_MESSAGE);
         logger.log(Level.SEVERE, "Exception during import of file " + importFile.getName() + ": " + e.getMessage());
      } finally {
         dialog.dispose();
      }
   }
   
   /* Discover JSON objects in an array (somewhere) that contain only simple values
    * 
    * IMPORTANT: JSONObject.getNames(Object) and JSONObject.getNames(JSONObject) give
    * different results, the javadoc is unclear. For an empty JSONObject, the first
    * returns a String array with a single value 'NULL' (probably the JSON NULL object),
    * the second returns null (as documented).
    * 
    * @param rootObject
    * @param path JSON path to this element
    * @param isCollection path is part of a collection
    */
   private void findPrimitiveObjects(Object rootObject, String path, boolean isCollection) throws JSONException {
      if (rootObject instanceof JSONObject) {
         boolean primitive = true;
         JSONObject jo = (JSONObject)rootObject;
         String[] keys = JSONObject.getNames(jo);
         for (int i = 0; i < keys.length; i++) {
            Object value = jo.get(keys[i]);
            if (value instanceof JSONObject && !((JSONObject.getNames((JSONObject)value) == null)) ||
                        value instanceof JSONArray) {
               primitive = false;
               findPrimitiveObjects(value, path + "." + keys[i], value instanceof JSONArray ? true : isCollection);
            }
         }
         // if all keys simple, add as candidate
         if (primitive & isCollection) {
            candidatePaths.add(path);
            logger.log(Level.FINE, "findPrimitiveObjects: candidate added: " + path);
         }
      } else if (rootObject instanceof JSONArray) {
         JSONArray ja = (JSONArray)rootObject;
         // TODO JSON arrays do not have to be homogeneous so we should really check
         // that it is.
         // For now assume it is so just examine first object
         Object object = ja.get(0);
         findPrimitiveObjects(object, path + "[*]", true);
      }
   }
   
   /* Replace empty JSON objects with an empty string value.
    * 
    * See comment re. JSONObject.getNames()
    * 
    * @param rootObject
    */
   private void fixupEmptyElements(Object rootObject) throws JSONException {
      if (rootObject instanceof JSONObject) {
         JSONObject jo = (JSONObject)rootObject;
         String[] keys = JSONObject.getNames(jo);
         for (int i = 0; i < keys.length; i++) {
            System.out.println("key: " + keys[i] + " has a value of type " + jo.get(keys[i]).getClass().getName());
            Object value = jo.get(keys[i]);
            if (value instanceof JSONObject || value instanceof JSONArray) {
               if (value instanceof JSONObject && JSONObject.getNames((JSONObject)value) == null) {
                  jo.put(keys[i], ""); // replace the empty JSONObject with the empty string
                  logger.log(Level.FINE, "Empty JSONObject for " + keys[i] + " has been replaced with an empty string");
               } else {
                  fixupEmptyElements(value);
               }
            }
         }
      } else if (rootObject instanceof JSONArray) {
         JSONArray ja = (JSONArray)rootObject;
         for (int i = 0; i < ja.length(); i++) {
            fixupEmptyElements(ja.get(i));
         }
      }
   }
   
   /*
    * Retrieve fields from jsonpath objects.
    * 
    * @param jsonPath
    * @return array of field names
    */
   private String[] getObjectFields(String jsonPath) {
      String[] fields = { "" };
      net.minidev.json.JSONArray jsonSArray = JsonPath.read(data.toString(), jsonPath);
      try {
         JSONArray jsonArray = new JSONArray(jsonSArray.toJSONString());
         logger.log(Level.FINE, "getObjectFields: number of elements in JSONArray: " + jsonArray.length());
         JSONObject jsonObject = jsonArray.getJSONObject(0);
         fields = new String[jsonObject.length()];
         Iterator<?> keys = jsonObject.keys();
         for (int i = 0; keys.hasNext(); i++) {
            fields[i] = (String) keys.next();
         }
      } catch (JSONException e) {
         // should not occur!
         logger.log(Level.FINE, "getObjectFields: " + jsonPath + e);
      }
      return fields;
   }
}