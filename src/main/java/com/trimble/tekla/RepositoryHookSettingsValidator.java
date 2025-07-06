package com.trimble.tekla;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.atlassian.bitbucket.scope.Scope;
import com.atlassian.bitbucket.setting.Settings;
import com.atlassian.bitbucket.setting.SettingsValidationErrors;
import com.atlassian.bitbucket.setting.SettingsValidator;
import com.atlassian.bitbucket.i18n.I18nService;

/**
 * Class for validating hook configuration form
 */
public class RepositoryHookSettingsValidator implements SettingsValidator {

    private static final Pattern URL_VALIDATION_PATTERN = Pattern.compile("^https?://[^\\s/$.?#].[^\\s]*$", Pattern.CASE_INSENSITIVE);
    private static final String BRANCH_TEST_STRING = "refs/heads/master";

    private final I18nService i18n;

    /**
     * Class constructor
     *
     * @param i18n - {@link I18nService} injected via component-import in atlassian-plugin.xml
     */
    @Inject
    public RepositoryHookSettingsValidator(final I18nService i18n) {
        this.i18n = i18n;
    }

    @Override
    public void validate(Settings stngs, SettingsValidationErrors sve, Scope scope) {
        try {
            validateConnectionTab(stngs, sve);
            validaterepositoryTriggersTab(stngs, sve);
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
    
    /**
     * Validates form fields in connections tab
     *
     * @param settings - to be validated.
     * @param errors - callback for reporting validation errors.
     */
    private void validateConnectionTab(final Settings settings, final SettingsValidationErrors errors) {
        
        final Boolean isDebugOn = settings.getBoolean(Field.DEBUG);
                
        final String bitbucketUrl = settings.getString(Field.BITBUCKET_URL, StringUtils.EMPTY);
        if (!URL_VALIDATION_PATTERN.matcher(bitbucketUrl).matches()) {
            errors.addFieldError(Field.BITBUCKET_URL, this.i18n.getText("error.invalid.url", "Invalid url"));
        }

        final String urlTeamcity = settings.getString(Field.TEAMCITY_URL, StringUtils.EMPTY);
        if (!URL_VALIDATION_PATTERN.matcher(urlTeamcity).matches()) {
            errors.addFieldError(Field.TEAMCITY_URL, this.i18n.getText("error.invalid.url", "Invalid url"));
        }

        final String teamCityUserName = settings.getString(Field.TEAMCITY_USERNAME, StringUtils.EMPTY);
        if (StringUtils.EMPTY.equals(teamCityUserName)) {
            errors.addFieldError(Field.TEAMCITY_USERNAME, this.i18n.getText("error.required.field", "field required"));
        }

        final String teamCityPassword = settings.getString(Field.TEAMCITY_PASSWORD, StringUtils.EMPTY);
        if (StringUtils.EMPTY.equals(teamCityPassword)) {
            errors.addFieldError(Field.TEAMCITY_PASSWORD, this.i18n.getText("error.required.field", "field required"));
        }

        if (!Constant.TEAMCITY_PASSWORD_SAVED_VALUE.equals(teamCityPassword)) {
            errors.addFieldError(Field.TEAMCITY_PASSWORD,
                this.i18n.getText("error.require.validation", this.i18n.getText("connetion.test.button", "test")));
        }
    }

    /**
     * Validates form data in repository triggers tab
     *
     * @param settings - to be validated.
     * @param errors - callback for reporting validation errors.
     * @throws IOException if JSON parsing error occurs
     */
    private void validaterepositoryTriggersTab(final Settings settings, final SettingsValidationErrors errors) throws IOException {
        final String repositoryTriggersJson = settings.getString(Field.REPOSITORY_TRIGGERS_JSON, StringUtils.EMPTY);
        if (StringUtils.isBlank(repositoryTriggersJson)) {
            return;
        }
        // Native parsing: expects a flat JSON object of the form {"key":{...}, ...}
        String json = repositoryTriggersJson.trim();
        if (json.startsWith("{") && json.endsWith("}")) {
            json = json.substring(1, json.length() - 1).trim();
        }
        if (json.isEmpty()) return;
        String[] entries = json.split("(?<=\\}),");
        for (String entry : entries) {
            entry = entry.trim();
            if (entry.endsWith(",")) entry = entry.substring(0, entry.length() - 1);
            int colonIdx = entry.indexOf(":");
            if (colonIdx == -1) continue;
            String key = entry.substring(0, colonIdx).replaceAll("[\"{} ]", "");
            String value = entry.substring(colonIdx + 1).trim();
            // Now value is a JSON object for Trigger, e.g. {"target":"...","regex":"..."}
            String target = null;
            String regex = null;
            // Parse target
            int targetIdx = value.indexOf("\"target\"");
            if (targetIdx != -1) {
                int colon = value.indexOf(':', targetIdx);
                int comma = value.indexOf(',', colon);
                if (comma == -1) comma = value.indexOf('}', colon);
                if (colon != -1 && comma != -1) {
                    target = value.substring(colon + 1, comma).replaceAll("[\"{} ]", "");
                }
            }
            // Parse regex
            int regexIdx = value.indexOf("\"regex\"");
            if (regexIdx != -1) {
                int colon = value.indexOf(':', regexIdx);
                int comma = value.indexOf(',', colon);
                if (comma == -1) comma = value.indexOf('}', colon);
                if (colon != -1 && comma != -1) {
                    regex = value.substring(colon + 1, comma).replaceAll("[\"{} ]", "");
                }
            }
            if (StringUtils.isBlank(target)) {
                errors.addFieldError(key, this.i18n.getText("error.string.empty", "empty"));
            } else if (StringUtils.containsWhitespace(target)) {
                errors.addFieldError(key, this.i18n.getText("error.string.contains.whitespace", "contains whitespace"));
            }
            if (regex != null) {
                try {
                    final Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
                    final Matcher matcher = pattern.matcher(BRANCH_TEST_STRING);
                    if (matcher.groupCount() != 1) {
                        errors.addFieldError(key, this.i18n.getText("error.regex.needs.capturing", "capturing"));
                    }
                } catch (final PatternSyntaxException e) {
                    errors.addFieldError(key, e.getLocalizedMessage());
                }
            }
        }
    }
}
