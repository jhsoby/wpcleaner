/*
 *  WikipediaCleaner: A tool to help on Wikipedia maintenance tasks.
 *  Copyright (C) 2008  Nicolas Vervelle
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.wikipediacleaner.api.check;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.NameValuePair;
import org.wikipediacleaner.api.base.APIException;
import org.wikipediacleaner.api.base.APIFactory;
import org.wikipediacleaner.api.check.algorithm.CheckErrorAlgorithm;
import org.wikipediacleaner.api.constants.EnumWikipedia;
import org.wikipediacleaner.api.data.DataManager;
import org.wikipediacleaner.api.data.Page;
import org.wikipediacleaner.i18n.GT;


/**
 * An abstract class for managing errors defind in the check wikipedia project.
 */
public class CheckError {

  public final static int PRIORITY_UNKOWN = -1;
  public final static int PRIORITY_DEACTIVATED = 0;
  public final static int PRIORITY_TOP = 1;
  public final static int PRIORITY_MIDDLE = 2;
  public final static int PRIORITY_LOWEST = 3;
  public final static int PRIORITY_BOT_ONLY = 4;

  /**
   * Analyze a page to find error types.
   * 
   * @param errors Possible error types.
   * @param page Page to be analyzed.
   * @param contents Page contents (may be different from page.getContents()).
   * @return Errors found in the page.
   */
  public static ArrayList<CheckErrorPage> analyzeErrors(
      ArrayList<CheckError> errors, Page page, String contents) {
    ArrayList<CheckErrorPage> errorsFound = new ArrayList<CheckErrorPage>();
    if ((errors != null) && (page != null)) {
      if (contents == null) {
        contents = page.getContents();
      }
      for (CheckError error : errors) {
        if ((error.algorithm != null) && (error.algorithm.isAvailable())) {
          ArrayList<CheckErrorResult> results = new ArrayList<CheckErrorResult>();
          //System.out.println("Checking error n°" + error.getErrorNumber() + " for " + page.getTitle());
          if (error.algorithm.analyze(page, contents, results)) {
            CheckErrorPage errorPage = new CheckErrorPage(page, error.algorithm);
            errorPage.setResults(true, results);
            errorsFound.add(errorPage);
          }
        }
      }
    }
    return errorsFound;
  }

  /**
   * Analyze a page to find errors of a given type.
   * 
   * @param errorPage Error page.
   * @param contents Page contents (may be different from page.getContents()).
   */
  public static void analyzeError(
      CheckErrorPage errorPage, String contents) {
    if (errorPage != null) {
      ArrayList<CheckErrorResult> errorsFound = new ArrayList<CheckErrorResult>();
      boolean errorFound = false;
      if ((errorPage.getAlgorithm() != null) &&
          (errorPage.getAlgorithm().isAvailable()) &&
          (errorPage.getPage() != null)) {
        errorFound = errorPage.getAlgorithm().analyze(
            errorPage.getPage(), contents, errorsFound);
      }
      errorPage.setResults(errorFound, errorsFound);
    }
  }

  /**
   * Retrieve error priority from configuration.
   * 
   * @param errorNumber Error number.
   * @return Priority.
   */
  public static int getErrorPriority(
      EnumWikipedia wikipedia, int errorNumber) {
    int errorPriority = PRIORITY_UNKOWN;
    String prioWiki = wikipedia.getCheckWikiProperty("prio", errorNumber, true, false, false);
    if (prioWiki != null) {
      try {
        errorPriority = Integer.parseInt(prioWiki);
      } catch (NumberFormatException e) {
        //
      }
    }
    if (errorPriority == PRIORITY_UNKOWN) {
      String prioScript = wikipedia.getCheckWikiProperty("prio", errorNumber, false, true, false);
      if (prioScript != null) {
        try {
          errorPriority = Integer.parseInt(prioScript);
        } catch (NumberFormatException e) {
          //
        }
      }
    }
    if (errorPriority == PRIORITY_DEACTIVATED) {
      String botOnly = wikipedia.getCheckWikiProperty("bot", errorNumber, true, true, false);
      if ((botOnly != null) && Boolean.valueOf(botOnly.trim())) {
        errorPriority = PRIORITY_BOT_ONLY;
      }
    }
    return errorPriority;
  }

  /**
   * @param priority Priority.
   * @return Flag indicating if the priority is active.
   */
  public static boolean isPriorityActive(int priority) {
    if ((priority == PRIORITY_TOP) ||
        (priority == PRIORITY_MIDDLE) ||
        (priority == PRIORITY_LOWEST) ||
        (priority == PRIORITY_BOT_ONLY)) {
      return true;
    }
    return false;
  }

  /**
   * Compare 2 priorities.
   * 
   * @param p1 Priority 1.
   * @param p2 Priority 2.
   * @return 0 if priorities are equal, -1 if p1 < p2, 1 if p1 > p2.
   */
  public static int comparePriority(int p1, int p2) {
    if (p1 == p2) {
      return 0;
    }
    if (p1 == PRIORITY_UNKOWN) {
      return -1;
    }
    if (p2 == PRIORITY_UNKOWN) {
      return 1;
    }
    if (p1 == PRIORITY_DEACTIVATED) {
      return -1;
    }
    if (p2 == PRIORITY_DEACTIVATED) {
      return 1;
    }
    return (p1 < p2) ? -1 : 1;
  }

  /**
   * @param priority Priority.
   * @return Textual description of the priority.
   */
  public static String getPriority(int priority) {
    switch (priority) {
    case PRIORITY_DEACTIVATED:
      return GT._("Deactivated");
    case PRIORITY_LOWEST:
      return GT._("Low priority");
    case PRIORITY_MIDDLE:
      return GT._("Middle priority");
    case PRIORITY_TOP:
      return GT._("Top priority");
    case PRIORITY_BOT_ONLY:
      return GT._("For Bot");
    default:
      return GT._("Priority unknown");
    }
  }

  /**
   * Retrieve error short description from configuration.
   * 
   * @param wikipedia Wikipedia.
   * @param errorNumber Error number.
   * @return Short description.
   */
  public static String getErrorShortDescription(
      EnumWikipedia wikipedia, int errorNumber) {
    return wikipedia.getCheckWikiProperty("head", errorNumber, true, true, false);
  }

  /**
   * Retrieve error long description from configuration.
   * 
   * @param errorNumber Error number.
   * @return Long description.
   */
  public static String getErrorLongDescription(
      EnumWikipedia wikipedia, int errorNumber) {
    return wikipedia.getCheckWikiProperty("desc", errorNumber, true, true, false);
  }

  /**
   * Retrieve link to error description from configuration.
   * 
   * @param wikipedia Wikipedia.
   * @param errorNumber Error number.
   * @return Link to error description.
   */
  public static String getErrorLink(
      EnumWikipedia wikipedia, int errorNumber) {
    return wikipedia.getCheckWikiProperty("link", errorNumber, true, true, false);
  }

  /**
   * Retrieve white list from configuration.
   * 
   * @param wikipedia Wikipedia.
   * @param errorNumber Error number.
   * @return White list
   */
  public static String[] getErrorWhiteList(
      EnumWikipedia wikipedia, int errorNumber) {
    String whiteListString = wikipedia.getCheckWikiProperty(
        "whitelist", errorNumber, true, false, false);
    if (whiteListString == null) {
      return null;
    }
    return wikipedia.convertPropertyToStringArray(whiteListString);
  }

  /**
   * @param errors Errors list.
   * @param wikipedia Wikipedia.
   * @param errorNumber Error number.
   * @param stream Stream containing list of pages for the error number.
   */
  public static void addCheckError(
      ArrayList<CheckError> errors,
      EnumWikipedia wikipedia, int errorNumber, InputStream stream) {

    // Analyze properties to find infos about error number
    int priority = getErrorPriority(wikipedia, errorNumber);
    if (!isPriorityActive(priority)) {
      return;
    }
    String shortDescription = getErrorShortDescription(wikipedia, errorNumber);
    String longDescription = getErrorLongDescription(wikipedia, errorNumber);
    String link = getErrorLink(wikipedia, errorNumber);
    String[] whiteList = getErrorWhiteList(wikipedia, errorNumber);

    // Create error
    CheckError error = new CheckError(
        wikipedia, errorNumber, priority,
        shortDescription, longDescription,
        link, whiteList);
    if (stream != null) {
      try {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        String line = null;
        // TODO: Correctly parse HTML ?
        while (((line = reader.readLine()) != null) && !line.endsWith("<pre>")) {
          // Waiting for <pre>
        }
        while (((line = reader.readLine()) != null) && !line.startsWith("</pre>")) {
          // TODO: Use something like Apache Commons Lang StringEscapeUtils ?
          line = line.replaceAll(Pattern.quote("&#039;"), "'");
          line = line.replaceAll(Pattern.quote("&quot;"), "\"");
          error.addPage(line);
        }
      } catch (UnsupportedEncodingException e) {
        //
      } catch (IOException e) {
        //
      }
    }

    // Add / Replace error
    for (int i = errors.size(); i > 0; i--) {
      if (errors.get(i - 1).getErrorNumber() == errorNumber) {
        errors.remove(i - 1);
      }
    }
    errors.add(error);
  }

  private final EnumWikipedia wikipedia;
  private final int errorNumber;
  private final CheckErrorAlgorithm algorithm;
  private final ArrayList<Page> errors;

  /**
   * Constructor
   * 
   * @param wikipedia Wikipedia.
   * @param errorNumber Error number as defined in the check wikipedia project.
   * @param priority Error priority.
   * @param shortDescription Short description.
   * @param longDescription Long description.
   * @param link Link to error description.
   * @param whiteList White list.
   */
  private CheckError(
      EnumWikipedia wikipedia, int errorNumber, int priority,
      String shortDescription, String longDescription,
      String link, String[] whiteList) {
    this.wikipedia = wikipedia;
    DecimalFormat errorNumberFormat = new DecimalFormat("000");
    String className = CheckErrorAlgorithm.class.getName() + errorNumberFormat.format(errorNumber);
    CheckErrorAlgorithm tmpAlgorithm = null;
    try {
      Class algorithmClass = Class.forName(className);
      tmpAlgorithm = (CheckErrorAlgorithm) algorithmClass.newInstance();
      tmpAlgorithm.setPriority(priority);
      tmpAlgorithm.setShortDescription(shortDescription);
      tmpAlgorithm.setLongDescription(longDescription);
      tmpAlgorithm.setLink(link);
      tmpAlgorithm.setWhiteList(whiteList);
    } catch (ClassNotFoundException e) {
      // Not found: error not yet available in WikiCleaner.
    } catch (InstantiationException e) {
      System.err.println("InstantiationException for " + className);
    } catch (IllegalAccessException e) {
      System.err.println("IllegalAccessException for " + className);
    } catch (ClassCastException e) {
      System.err.println(
          "Class " + className +
          " doesn't implement " + CheckErrorAlgorithm.class.getName());
    }
    this.algorithm = tmpAlgorithm;
    this.errorNumber = errorNumber;
    this.errors = new ArrayList<Page>();
  }
  
  /**
   * @return Error number as defined in the check wikipedia project.
   */
  public int getErrorNumber() {
    return errorNumber;
  }

  /**
   * @return Algorithm.
   */
  public CheckErrorAlgorithm getAlgorithm() {
    return algorithm;
  }

  /**
   * @return Number of error pages.
   */
  public int getPageCount() {
    return errors.size();
  }

  /**
   * @param index Page index.
   * @return Error page.
   */
  public Page getPage(int index) {
    if ((index < 0) || (index >= errors.size())) {
      return null;
    }
    return errors.get(index);
  }

  /**
   * Add a page to the list of errors.
   * 
   * @param page Page.
   */
  private void addPage(String page) {
    Page tmpPage = DataManager.getPage(wikipedia, page, null, null);
    if (!errors.contains(tmpPage)) {
      errors.add(tmpPage);
    }
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    String count = Integer.toString(errors.size());
    return GT._("Error n°{0} ({1} - {2}) - {3}", new Object[] {
        Integer.valueOf(errorNumber),
        count,
        CheckError.getPriority((algorithm != null) ?
            algorithm.getPriority() : CheckError.PRIORITY_UNKOWN),
        (algorithm != null) ?
            algorithm.getShortDescriptionReplaced() :
            GT._("Error unkown from WikiCleaner") });
  }

  /**
   * Remove a page from the list of errors.
   * 
   * @param page Page.
   */
  public void remove(Page page) {
    if (page == null) {
      return;
    }
    for (int i = errors.size(); i > 0; i--) {
      if (Page.areSameTitle(page.getTitle(), errors.get(i - 1).getTitle())) {
        errors.remove(i - 1);
      }
    }
  }

  /**
   * Fix an error for the page.
   * 
   * @param page Page.
   * @param errorNumber Error number.
   * @return Flag indicating if fix was done.
   */
  public static boolean fix(Page page, String errorNumber) {
    try {
      NameValuePair[] parameters = new NameValuePair[] {
          new NameValuePair("id", Integer.toString(Integer.parseInt(errorNumber))),
          new NameValuePair("pageid", Integer.toString(page.getPageId())),
          new NameValuePair("project", page.getWikipedia().getCode() + "wiki"),
          new NameValuePair("view", "only")
      };
      APIFactory.getAPI().askToolServerPost(
          "~sk/cgi-bin/checkwiki/checkwiki.cgi", parameters, false);
    } catch (NumberFormatException e) {
      return false;
    } catch (APIException e) {
      return false;
    }
    return true;
  }

  /**
   * Fix an error for the page.
   * 
   * @param page Page.
   * @return Flag indicating if fix was done.
   */
  public boolean fix(Page page) {
    errors.remove(page);
    return fix(page, Integer.toString(errorNumber));
  }
}
