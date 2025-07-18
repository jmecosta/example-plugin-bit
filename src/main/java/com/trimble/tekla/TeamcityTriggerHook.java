package com.trimble.tekla;

import com.atlassian.bitbucket.commit.CommitService;
import com.atlassian.bitbucket.commit.NoSuchCommitException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.atlassian.bitbucket.hook.repository.PostRepositoryHook;
import com.atlassian.bitbucket.hook.repository.PostRepositoryHookContext;
import com.atlassian.bitbucket.hook.repository.RepositoryHookContext;
import com.atlassian.bitbucket.hook.repository.RepositoryHookRequest;
import com.atlassian.bitbucket.repository.RefChange;
import com.atlassian.bitbucket.repository.RefChangeType;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.setting.Settings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

/**
 * Note that hooks can implement RepositorySettingsValidator directly.
 */
public class TeamcityTriggerHook implements PostRepositoryHook<RepositoryHookRequest> {
  private final TeamcityConnector connector;
  private final CommitService scmService;
  private final TeamcityConnectionSettings connectionSettings;
  

  @Inject
  public TeamcityTriggerHook(
          final CommitService scmService,
          PluginSettingsFactory pluginSettingsFactory) {
    this.scmService = scmService;
    this.connectionSettings = new TeamcityConnectionSettings(pluginSettingsFactory);
    this.connector = new TeamcityConnector(new HttpConnector());
  }

  /**
   * Connects to a configured URL to notify of all changes.
   * @param context
   * @param hookRequest
   */
  @Override
  public void postUpdate(
          @Nonnull final PostRepositoryHookContext context,
          @Nonnull final RepositoryHookRequest hookRequest) {

    final String password = this.connectionSettings.getPassword(hookRequest.getRepository());

    if (password.isEmpty()) {
      TeamcityLogger.logMessage(
        context.getSettings(),
        hookRequest.getRepository().getName(),
        "postReceive: Teamcity secret password not set. Please set password so accounts dont get locked.");
      return;
    }

    final String repositoryTriggersJson = context.getSettings().getString(Field.REPOSITORY_TRIGGERS_JSON, StringUtils.EMPTY);
    if (repositoryTriggersJson.isEmpty()) {
      return;
    }


    final TeamcityConfiguration conf = new TeamcityConfiguration(
            context.getSettings().getString(Field.TEAMCITY_URL),
            context.getSettings().getString(Field.TEAMCITY_USERNAME),
            password);

    final Repository repository = hookRequest.getRepository();
    final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
    final Set<String> uniqueBranches = new LinkedHashSet<>();
    TeamcityLogger.logMessage(
      context.getSettings(),
      hookRequest.getRepository().getName(),
      "postReceive: " + uniqueBranches.size());

    for (final RefChange change : hookRequest.getRefChanges()) {
      final String referenceId = change.getRef().getId();
      if (uniqueBranches.contains(referenceId)) {
        continue;
      }

      Trigger[] configurations = GetConfigurations(context, repositoryTriggersJson, referenceId, hookRequest.getRepository().getName());
      if (configurations.length == 0) {
        continue;
      }

      if (change.getType().equals(RefChangeType.DELETE)) {
        TeamcityLogger.logMessage(
          context.getSettings(),
          hookRequest.getRepository().getName(),
          ">" + timeStamp + "< Skip trigger for delete operation in branch: " + referenceId);
        continue;
      }

      uniqueBranches.add(referenceId);

      TeamcityLogger.logMessage(context.getSettings(), hookRequest.getRepository().getName(), "Trigger From Ref: " + referenceId);
      try {
        final boolean isEmptyBranch = isEmptyBranch(context, timeStamp, repository, change);
        final Iterable<String> changedFiles = isEmptyBranch ? new ArrayList<>() : ChangesetService.GetChangedFiles(scmService, repository, change);
        for(Trigger configuration : configurations) {
          if (!ExclusionTriggers.ShouldTriggerOnListOfFiles(configuration.gettriggerInclusion(), configuration.gettriggerExclusion(), changedFiles)) {
            TeamcityLogger.logMessage(
              context.getSettings(),
              hookRequest.getRepository().getName(),
              "Trigger From Ref: " + referenceId + " Excluded: " +  configuration.getTarget());
            continue;
          }
          TeamcityLogger.logMessage(
            context.getSettings(),
            hookRequest.getRepository().getName(),
            "Trigger From Ref: " + referenceId + " Target: " + configuration.getTarget());
          TriggerBuild(configuration, context, referenceId, change.getToHash(), conf, timeStamp, hookRequest.getRepository().getName(), isEmptyBranch);
        }
      } catch (NoSuchCommitException ex) {
        TeamcityLogger.logError(          
          context.getSettings(),
          hookRequest.getRepository().getName(),
          "No commit Exception: " + ex.getCommitId() + " " + referenceId, ex);
      } catch (Exception ex) {
        TeamcityLogger.logError(
          context.getSettings(),
          hookRequest.getRepository().getName(),
          "Failed to trigger: " + ex.getMessage() + " " + referenceId, ex);
      }
    }
  }

  /**
   * @param context - {@link PostRepositoryHookContext}
   * @param timeStamp - {@link String} - timestamp for tracing logging to system out
   * @param repository - {@link Repository}
   * @param change - {@link RefChange}
   * @return true if specified {@link RefChange change} is a branch and last commit on it belongs to multiple branches
   */
  private boolean isEmptyBranch(final PostRepositoryHookContext context, final String timeStamp, final Repository repository, final RefChange change) {
    boolean isEmptyBranch = false;   
    return isEmptyBranch;
  }

  private void TriggerBuild(final Trigger buildConfig,
                            final RepositoryHookContext context,
                            final String refId,
                            final String revision,
                            final TeamcityConfiguration conf,
                            final String timestamp,
                            final String repoName,
                            final boolean isEmptyBranch) throws IOException {
    if (buildConfig.isTriggerOnPullRequest() || isEmptyBranch && !buildConfig.isTriggerOnEmptyBranches()) {
      TeamcityLogger.logMessage(context.getSettings(), repoName, "Skipped <Return>: " + buildConfig.getTarget() + " RefChange Type: " + refId);
      if(buildConfig.isTriggerOnPullRequest()) {
        TeamcityLogger.logMessage(context.getSettings(), repoName, "Skipped <buildConfig.isTriggerOnPullRequest() false>: " + buildConfig.getTarget() + " RefChange Type: " + refId);
      }
      if(isEmptyBranch) {
        TeamcityLogger.logMessage(context.getSettings(), repoName, "Skipped <isEmptyBranch true>: " + buildConfig.getTarget() + " RefChange Type: " + refId);
      }
      if(buildConfig.isTriggerOnEmptyBranches()) {
        TeamcityLogger.logMessage(context.getSettings(), repoName, "Skipped <buildConfig.isTriggerOnEmptyBranches() true>: " + buildConfig.getTarget() + " RefChange Type: " + refId);
      }
      return;
    }

    if (buildConfig.getType().equals("vcs")) {
      TriggerCheckForChanges(context, repoName, buildConfig.getTarget(), conf, context.getSettings());
    }

    if (buildConfig.getType().equals("build")) {
      TeamcityLogger.logMessage(context.getSettings(), repoName, "Will Try To Que: " + buildConfig.getTarget() + " RefChange Type: " + refId);
      QueueBuild(
              context,
              buildConfig.getTarget(),
              buildConfig.getBranchConfig(),
              revision,
              repoName,
              buildConfig.isCancelRunningBuilds(),
              buildConfig.isCancelDependencies(),
              conf,
              timestamp,
              false,
              context.getSettings());
    }
  }

  private void TriggerCheckForChanges(final RepositoryHookContext context,
                                      final String repoName,
                                      final String vcsRoot,
                                      final TeamcityConfiguration conf,
                                      final Settings settings) {
    try {
      TeamcityLogger.logMessage(context.getSettings(), repoName, "Trigger Check for Changes in: " + vcsRoot);
      this.connector.TriggerCheckForChanges(conf, vcsRoot, settings, repoName);
    } catch (final Exception e) {
      TeamcityLogger.logMessage(context.getSettings(), repoName, "Trigger Check for Changes in: " + vcsRoot + " Failed : " + e.getMessage());
    }
  }

  private void QueueBuild(
          final RepositoryHookContext context,
          final String buildIdIn,
          final String branch,
          final String revision,
          final String repoName,
          final Boolean cancelRunningBuilds,
          final Boolean cancelDependencies,
          final TeamcityConfiguration conf,
          final String timeStamp,
          final Boolean isDefault,
          final Settings settings) {

    final String baseUrl = context.getSettings().getString(Field.BITBUCKET_URL);
    String comment = "remote trigger from bitbucket server : server address not specified in Bitbucket";

    if (!baseUrl.isEmpty()) {
      comment = "remote trigger from bitbucket server : " + baseUrl + "/branches";
    }

    TeamcityLogger.logMessage(context.getSettings(), repoName, "" + timeStamp + " Trigger builds for branch: " + branch);
    try {
      TeamcityLogger.logMessage(context.getSettings(), repoName, "Trigger BuildId: " + buildIdIn + " " + branch);

      if (!this.connector.IsInQueue(conf, buildIdIn, branch, revision, settings, repoName)) {
        // check if build is running
        final String buildData = this.connector.GetBuildsForBranch(conf, branch, buildIdIn, settings, repoName, true);

        TeamcityLogger.logMessage(context.getSettings(), repoName, "Build Data Retrieved: " + buildData);
        try {
          // Native parsing for 'count'
          int count = 0;
          int countIdx = buildData.indexOf("\"count\"");
          if (countIdx != -1) {
            int colonIdx = buildData.indexOf(':', countIdx);
            int commaIdx = buildData.indexOf(',', colonIdx);
            if (commaIdx == -1) commaIdx = buildData.indexOf('}', colonIdx);
            String countStr = buildData.substring(colonIdx + 1, commaIdx).replaceAll("[^0-9]", "").trim();
            if (!countStr.isEmpty()) {
              count = Integer.parseInt(countStr);
            }
          }

          if (count == 0 || !cancelRunningBuilds) {
            this.connector.QueueBuild(conf, branch, buildIdIn, comment, isDefault, settings, repoName, null);
          } else {
            // Native parsing for 'build' array
            int buildArrIdx = buildData.indexOf("\"build\"");
            if (buildArrIdx != -1) {
              int arrStart = buildData.indexOf('[', buildArrIdx);
              int arrEnd = buildData.indexOf(']', arrStart);
              if (arrStart != -1 && arrEnd != -1) {
                String buildsArray = buildData.substring(arrStart + 1, arrEnd);
                String[] builds = buildsArray.split("\\},\\{");
                for (String buildEntry : builds) {
                  // Clean up braces
                  String entry = buildEntry.replace("{", "").replace("}", "");
                  String[] fields = entry.split(",");
                  String buildState = null;
                  String id = null;
                  for (String field : fields) {
                    String[] kv = field.split(":", 2);
                    if (kv.length == 2) {
                      String key = kv[0].replaceAll("[\" ]", "");
                      String value = kv[1].replaceAll("[\" ]", "");
                      if ("state".equals(key)) buildState = value;
                      if ("id".equals(key)) id = value;
                    }
                  }
                  if (buildState != null && id != null) {
                    if (buildState.equals("running")) {
                      this.connector.ReQueueBuild(conf, id, settings, false, repoName);
                    } else if (buildState.equals("queued")) {
                      if (cancelDependencies) {
                        this.connector.CancelDependenciesOfBuild(conf, id, settings, repoName);
                      }
                      this.connector.ReQueueBuild(conf, id, settings, false, repoName);
                    }
                  }
                }
              }
            }
            // at this point all builds were finished, so we need to trigger
            this.connector.QueueBuild(conf, branch, buildIdIn, comment, isDefault, settings, repoName, null);
            TeamcityLogger.logMessage(context.getSettings(), repoName, "Queued: " + buildIdIn + " " + branch);
          }
        } catch (final Exception e) {
          TeamcityLogger.logMessage(context.getSettings(), repoName, "Failed to Trigger will still try to trigger : Builddata : " + buildData + " Failed ");
          TeamcityLogger.logMessage(context.getSettings(), repoName, "Error: " + e.getMessage());
          TeamcityLogger.logMessage(context.getSettings(), repoName, "Will still try to trigger the build anyway just in case.");
          this.connector.QueueBuild(conf, branch, buildIdIn, comment, isDefault, settings, repoName, null);
        }
      } else {
        TeamcityLogger.logMessage(context.getSettings(), repoName, "Skip already in queue: " + buildIdIn + " " + branch);
      }
    } catch (final Exception e) {
      TeamcityLogger.logMessage(context.getSettings(), repoName, "BuildId: " + buildIdIn + " Failed " + branch);
      TeamcityLogger.logMessage(context.getSettings(), repoName, "Error: " + e.getMessage());
    }
  }

  private Trigger[] GetConfigurations(final RepositoryHookContext context, String repositoryTriggersJson, String refBranchId, String repoName) {
    return Trigger.GetBuildConfigurationsFromBranch(repositoryTriggersJson, refBranchId);
  }
}
