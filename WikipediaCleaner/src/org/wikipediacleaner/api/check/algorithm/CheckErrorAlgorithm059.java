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

package org.wikipediacleaner.api.check.algorithm;

import java.util.Collection;

import org.wikipediacleaner.api.check.CheckErrorResult;
import org.wikipediacleaner.api.data.PageAnalysis;
import org.wikipediacleaner.api.data.PageContents;
import org.wikipediacleaner.api.data.PageElementTagFull;
import org.wikipediacleaner.api.data.PageElementTagData;
import org.wikipediacleaner.api.data.PageElementTemplate;
import org.wikipediacleaner.i18n.GT;


/**
 * Algorithm for analyzing error 59 of check wikipedia project.
 * Error 59: Template value end with break
 */
public class CheckErrorAlgorithm059 extends CheckErrorAlgorithmBase {

  public CheckErrorAlgorithm059() {
    super("Template value end with break");
  }

  /**
   * Analyze a page to check if errors are present.
   * 
   * @param pageAnalysis Page analysis.
   * @param errors Errors found in the page.
   * @return Flag indicating if the error was found.
   */
  public boolean analyze(
      PageAnalysis pageAnalysis,
      Collection<CheckErrorResult> errors) {
    if (pageAnalysis == null) {
      return false;
    }

    // Analyzing from the begining
    boolean errorFound = false;
    for (PageElementTemplate template : pageAnalysis.getTemplates()) {
      for (int i = 0; i < template.getParameterCount(); i++) {
        String parameterValue = template.getParameterValue(i);
        if (parameterValue != null) {

          // Find last <br> tag
          PageElementTagFull lastTag = null;
          int currentIndex = 0;
          while (currentIndex < parameterValue.length()) {
            PageElementTagFull tag = PageContents.findNextTag(
                pageAnalysis.getPage(), parameterValue, "br", currentIndex);
            if (tag != null) {
              currentIndex = tag.getEndTagEndIndex() - 1;
              lastTag = tag;
            } else {
              currentIndex = parameterValue.length();
            }
          }
          PageElementTagData lastTagData = null;
          currentIndex = 0;
          while (currentIndex < parameterValue.length()) {
            PageElementTagData tag = PageContents.findNextStartTag(
                pageAnalysis.getPage(), parameterValue, "br", currentIndex);
            if (tag != null) {
              currentIndex = tag.getEndIndex();
              lastTagData = tag;
            } else {
              currentIndex = parameterValue.length();
            }
          }
          currentIndex = 0;
          while (currentIndex < parameterValue.length()) {
            PageElementTagData tag = PageContents.findNextEndTag(
                pageAnalysis.getPage(), parameterValue, "br", currentIndex);
            if (tag != null) {
              currentIndex = tag.getEndIndex();
              if ((lastTagData == null) || (lastTagData.getEndIndex() < tag.getEndIndex())) {
                lastTagData = tag;
              }
            } else {
              currentIndex = parameterValue.length();
            }
          }

          if ((lastTag != null) && (lastTagData != null)) {
            if (lastTag.getEndTagEndIndex() < lastTagData.getEndIndex()) {
              lastTag = null;
            } else {
              lastTagData = null;
            }
          }
          if ((lastTag != null) || (lastTagData != null)) {
            int startTagIndex = 0;
            int endTagIndex = 0;
            if (lastTag != null) {
              startTagIndex = lastTag.getStartTagBeginIndex();
              endTagIndex = lastTag.getEndTagEndIndex();
            } else if (lastTagData != null) {
              startTagIndex = lastTagData.getBeginIndex();
              endTagIndex = lastTagData.getEndIndex();
            }
            currentIndex = endTagIndex;
            boolean ok = true;
            while (currentIndex < parameterValue.length()) {
              if (Character.isWhitespace(parameterValue.charAt(currentIndex))) {
                currentIndex++;
              } else if (parameterValue.startsWith("<!--", currentIndex)) {
                int endIndex = parameterValue.indexOf("-->", currentIndex + 4);
                if (endIndex < 0) {
                  currentIndex = parameterValue.length();
                  ok = false;
                } else {
                  currentIndex = endIndex + 3;
                }
              } else {
                currentIndex = parameterValue.length();
                ok = false;
              }
            }
            if (ok) {
              if (errors == null) {
                return true;
              }
              errorFound = true;
              CheckErrorResult errorResult = createCheckErrorResult(
                  pageAnalysis.getPage(),
                  template.getParameterValueOffset(i) + startTagIndex,
                  template.getParameterValueOffset(i) + endTagIndex);
              errorResult.addReplacement("", GT._("Delete"));
              errors.add(errorResult);
            }
          }
        }
      }
    }

    // Result
    return errorFound;
  }
}
