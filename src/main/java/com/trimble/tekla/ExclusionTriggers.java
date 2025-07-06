/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.trimble.tekla;

/**
 *
 * @author jocs
 */
public class ExclusionTriggers {

  public static Boolean ShouldTriggerOnListOfFiles(String triggerInclusion, String triggerExclusion, Iterable<String> changedfilespath) {
    if ("".equals(triggerInclusion) && "".equals(triggerExclusion)) {
      return true;
    }

    if ("".equals(triggerInclusion)) {
      triggerInclusion = "?";
    }

    if ("".equals(triggerExclusion)) {
      triggerExclusion = "?";
    }

    Boolean isIncluded = false;
    Boolean isExcluded = false;
    Boolean isNotIncludedOrExcluded = false;
    for (String changeFile : changedfilespath) {
      if (ContainsChange(changeFile, triggerInclusion)) {
        isIncluded = true;
      } else {
        if (ContainsChange(changeFile, triggerExclusion)) {
          isExcluded = true;
        } else {
          isNotIncludedOrExcluded = true;
        }
      }
    }

    if (isIncluded) {
      return true;
    }

    if (isExcluded && isNotIncludedOrExcluded) {
      return true;
    }

    if (!isIncluded && !isExcluded) {
      if (!"?".equals(triggerInclusion)) {
        return false;
      }

      return true;
    }

    return false;
  }

    private static boolean ContainsChange(String changeFile, String data) {
        String [] elements = data.split(",");
        
        for(String elem : elements) {
            if (changeFile.contains(elem)) {
                return true;
            }
        }
        
        return false;
    }
}
