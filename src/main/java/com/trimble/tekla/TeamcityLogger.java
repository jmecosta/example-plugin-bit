/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.trimble.tekla;

import com.atlassian.bitbucket.setting.Settings;

import org.slf4j.LoggerFactory;

/**
 *
 * @author jocs
 */
public class TeamcityLogger {
  private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(TeamcityLogger.class);
 
  // Use this method for logging with Settings
  public static void logMessage(Settings settings, String repoName, String message) {
    if (settings == null) {
      LOG.error("[TeamcityTriggerHook][INFO][" + repoName + "] : Context is null, cant get debug flag");
      return;
    }

    if(isDebugEnabled(settings, repoName)) {
      LOG.info("[TeamcityTriggerHook][INFO][" + repoName + "] : " + message);
    } else {
      LOG.info("[TeamcityTriggerHook][INFO][" + repoName + "] : Logging Disable");
    }
  }

  // Use this method for error logging with Settings
  public static void logError(Settings settings, String repoName, String message, Throwable ex) {
    if (settings == null) {
      LOG.error("[TeamcityTriggerHook][ERROR][" + repoName + "] : Context is null, cant get debug flag");
      return;
    }
    LOG.error("[TeamcityTriggerHook][ERROR][" + repoName + "] : " + message, ex);
  }

  private static boolean isDebugEnabled(Settings settings, String repoName) {
    try {
      return settings.getBoolean(Field.DEBUG);  
    } catch (Exception e) {      
      LOG.info("[TeamcityTriggerHook][INFO][" + repoName + "] : Logging is not setup, will be disabled");
      return false;
    }    
  }

  // If you need repository info, inject or pass Repository directly (see Bitbucket 9.x REST v2 docs)
}
