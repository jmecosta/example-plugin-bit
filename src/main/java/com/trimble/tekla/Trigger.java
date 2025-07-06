package com.trimble.tekla;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.*;
import java.util.regex.*;

/**
 *
 */
public class Trigger {
  private String branchConfig;
  private String regex;
  private String type;
  private String target;
  private boolean triggerOnPullRequest;
  private String downStreamTriggerTarget;
  private String downStreamTriggerType;
  private String downStreamTriggerDescription;
  private String triggerInclusion;
  private String triggerExclusion;
  private boolean cancelRunningBuilds;
  private boolean cancelDependencies;
  private boolean triggerOnEmptyBranches;
  private boolean hideOnPullRequest;
  private boolean triggerWhenNoReviewers;

  public String gettriggerInclusion() {
    return this.triggerInclusion;
  }

  public void settriggerInclusion(final String triggerInclusion) {
    this.triggerInclusion = triggerInclusion;
  }
  
   public String gettriggerExclusion() {
    return this.triggerExclusion;
  }

  public void setExcludeRule(final String triggerExclusion) {
    this.triggerExclusion = triggerExclusion;
  } 
  
  public String getBranchConfig() {
    return this.branchConfig;
  }

  public void setBranchConfig(final String branchConfig) {
    this.branchConfig = branchConfig;
  }

  public boolean isCancelRunningBuilds() {
    return this.cancelRunningBuilds;
  }

  public void setCancelRunningBuilds(final boolean cancelRunningBuilds) {
    this.cancelRunningBuilds = cancelRunningBuilds;
  }

  public boolean isCancelDependencies() {
    return this.cancelDependencies;
  }

  public void setCancelDependencies(final boolean cancelDependencies) {
    this.cancelDependencies = cancelDependencies;
  }

  public boolean isTriggerOnPullRequest() {
    return this.triggerOnPullRequest;
  }

  public void setTriggerOnPullRequest(final boolean triggerOnPullRequest) {
    this.triggerOnPullRequest = triggerOnPullRequest;
  }
  
  public boolean isHideOnPullRequest() {
      return this.hideOnPullRequest;
  }

  public void setHideOnPullRequest(final boolean hideOnPullRequest) {
    this.hideOnPullRequest = hideOnPullRequest;
  }  
  
  public boolean isTriggerWhenNoReviewers() {
      return this.triggerWhenNoReviewers;
  }

  public void setTriggerWhenNoReviewers(final boolean triggerWhenNoReviewers) {
    this.triggerWhenNoReviewers = triggerWhenNoReviewers;
  }   
  
  public String getDownStreamTriggerType() {
    return this.downStreamTriggerType;
  }

  public void setDownStreamTriggerType(final String downStreamTriggerType) {
    this.downStreamTriggerType = downStreamTriggerType;
  }
  
  public String getDownStreamTriggerDescription() {
    return this.downStreamTriggerDescription;
  }

  public void setDownStreamTriggerDescription(final String downStreamTriggerDescription) {
    this.downStreamTriggerDescription = downStreamTriggerDescription;
  }

  public String getDownStreamTriggerTarget() {
    return this.downStreamTriggerTarget;
  }

  public void setDownStreamTriggerTarget(final String downStreamTriggerTarget) {
    this.downStreamTriggerTarget = downStreamTriggerTarget;
  }

  public String getRegex() {
    return this.regex;
  }

  public void setRegex(final String regex) {
    this.regex = regex;
  }

  public String getType() {
    return this.type;
  }

  public void setType(final String type) {
    this.type = type;
  }

  public String getTarget() {
    return this.target;
  }

  public void setTarget(final String target) {
    this.target = target;
  }

  public boolean isTriggerOnEmptyBranches() {
    return triggerOnEmptyBranches;
  }

  public void setTriggerOnEmptyBranches(boolean triggerOnEmptyBranches) {
    this.triggerOnEmptyBranches = triggerOnEmptyBranches;
  }

public static Trigger[] GetBuildConfigurationsFromBranch(final String jsonConfiguration, final String branch) {
    final Map<String, Trigger> triggerMap = new HashMap<>();
    final List<Trigger> configs = new ArrayList<>();

    // Simple JSON parsing using standard Java libraries
    String[] entries = jsonConfiguration.replace("{", "").replace("}", "").split(",");
    for (String entry : entries) {
        String[] keyValue = entry.split(":");
        String key = keyValue[0].trim().replace("\"", "");
        String regex = keyValue[1].trim().replace("\"", "");

        Trigger trigger = new Trigger();
        trigger.setRegex(regex);
        triggerMap.put(key, trigger);
    }

    for (final Map.Entry<String, Trigger> triggerEntry : triggerMap.entrySet()) {
        final Pattern pattern = Pattern.compile(triggerEntry.getValue().getRegex(), Pattern.CASE_INSENSITIVE);
        final Matcher matcher = pattern.matcher(branch);
        if (matcher.find()) {
            triggerEntry.getValue().setBranchConfig(matcher.group(matcher.groupCount()));
            configs.add(triggerEntry.getValue());
        }
    }

    return configs.toArray(new Trigger[configs.size()]);
}
}
