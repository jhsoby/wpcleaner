/*
 *  WPCleaner: A tool to help on Wikipedia maintenance tasks.
 *  Copyright (C) 2014  Nicolas Vervelle
 *
 *  See README.txt file for licensing information.
 */


package org.wikipediacleaner.gui.swing.component;

import java.awt.Component;
import java.awt.event.ActionListener;
import java.beans.EventHandler;
import java.util.HashMap;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.JTextComponent;

import org.wikipediacleaner.api.constants.EnumWikipedia;
import org.wikipediacleaner.gui.swing.Controller;
import org.wikipediacleaner.gui.swing.basic.Utilities;
import org.wikipediacleaner.images.EnumImageSize;


/**
 * Cell renderer and editor for a go to button.
 */
public abstract class GoToRenderer extends AbstractCellEditor implements
    TableCellRenderer, TableCellEditor {

  /** Serialization */
  private static final long serialVersionUID = -7425915853836707658L;

  /** Maps of all the buttons. */
  private HashMap<Object, JButton> buttons;

  /** Text pane where the text is. */
  protected final JTextComponent textPane;

  /** Wiki */
  private final EnumWikipedia wiki;

  /**
   * @param textPane Text pane where the text is.
   * @param wiki Wiki.
   */
  public GoToRenderer(JTextComponent textPane, EnumWikipedia wiki) {
    buttons = new HashMap<Object, JButton>();
    this.textPane = textPane;
    this.wiki = wiki;
  }

  /**
   * @return Value in the editor.
   * @see javax.swing.CellEditor#getCellEditorValue()
   */
  @Override
  public Object getCellEditorValue() {
    return null;
  }

  /**
   * @param table Table.
   * @param value Value in the cell.
   * @param isSelected True if cell is selected.
   * @param row Row number.
   * @param column Column number.
   * @return Component to edit the cell.
   * @see javax.swing.table.TableCellEditor#getTableCellEditorComponent(javax.swing.JTable, java.lang.Object, boolean, int, int)
   */
  @Override
  public Component getTableCellEditorComponent(
      JTable table, Object value,
      boolean isSelected, int row, int column) {
    return getButton(value);
  }

  /**
   * @param table Table.
   * @param value Value in the cell.
   * @param isSelected True if cell is selected.
   * @param hasFocus True if cell has focus.
   * @param row Row number.
   * @param column Column number.
   * @return Component to render the cell.
   * @see javax.swing.table.TableCellRenderer#getTableCellRendererComponent(javax.swing.JTable, java.lang.Object, boolean, boolean, int, int)
   */
  @Override
  public Component getTableCellRendererComponent(
      JTable table, Object value,
      boolean isSelected, boolean hasFocus, int row, int column) {
    return getButton(value);
  }

  /**
   * Creates the button for the detection if it doesn't already exist.
   * 
   * @param value Detection.
   * @return Button for the detection.
   */
  private JButton getButton(Object value) {
    if (buttons.containsKey(value)) {
      return buttons.get(value);
    }
    String actionCommand = constructActionCommand(value);
    if (actionCommand == null) {
      return null;
    }
    JButton button = new JButton(Utilities.getImageIcon(
        (textPane != null) ? "gnome-edit-find.png" : "gnome-system-run.png",
        EnumImageSize.SMALL));
    button.setBorderPainted(false);
    button.setContentAreaFilled(false);
    button.setActionCommand(actionCommand);
    button.setEnabled(true);
    button.addActionListener(EventHandler.create(
        ActionListener.class, this, "goTo", "actionCommand"));
    buttons.put(value, button);
    return button;
  }

  /**
   * Construct the action command.
   * 
   * @param value Object.
   * @return Action command, either begin;end or page name.
   */
  protected abstract String constructActionCommand(Object value);

  /**
   * @param location Location to go.
   */
  public void goTo(String location) {
    if (textPane != null) {
      try {
        String[] locations = location.split(";");
        if (locations.length > 0) {
          int startValue = Integer.valueOf(locations[0]);
          if ((startValue >= 0) && (startValue < textPane.getText().length())) {
            textPane.setCaretPosition(startValue);
            textPane.moveCaretPosition(startValue);
            if (locations.length > 1) {
              int endValue = Integer.valueOf(locations[1]);
              if ((endValue >= 0) && (endValue <= textPane.getText().length())) {
                textPane.moveCaretPosition(endValue);
              }
            }
            textPane.requestFocusInWindow();
          }
        }
      } catch (NumberFormatException e) {
        //
      }
    } else {
      Controller.runFullAnalysis(location, null, wiki);
    }
  }
}
