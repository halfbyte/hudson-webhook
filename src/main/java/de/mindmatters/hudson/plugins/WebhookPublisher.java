package de.mindmatters.hudson.plugins;
import hudson.Extension;
import hudson.Functions;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Mailer;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * @author jankrutisch
 */

public class WebhookPublisher extends Notifier {
    private static final List<String> VALUES_REPLACED_WITH_NULL = Arrays.asList("", "(Default)",
            "(System Default)");
    private static final Logger LOGGER = Logger.getLogger(WebhookPublisher.class.getName());

    private String hookurl;
    private String method;
    private String data;

    @DataBoundConstructor
    public WebhookPublisher(String hookurl, String method, String data) {
        LOGGER.log(Level.INFO, "databound stuff:" + hookurl);
        this.hookurl = cleanToString(hookurl);
        this.method = cleanToString(method);
        this.data = cleanToString(data);
    }
    private static String cleanToString(String string) {
        return VALUES_REPLACED_WITH_NULL.contains(string) ? null : string;
    }

    private static Boolean cleanToBoolean(String string) {
        Boolean result = null;
        if ("true".equals(string) || "Yes".equals(string)) {
                result = Boolean.TRUE;
        } else if ("false".equals(string) || "No".equals(string)) {
                result = Boolean.FALSE;
        }
        return result;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
        if (shouldPublish(build)) {
            try {
                LOGGER.log(Level.INFO, "Should've Publishd");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Unable to send tweet.", e);
            }
        }
        return true;
    }

    /**
     * Determine if this build represents a failure or recovery. A build failure
     * includes both failed and unstable builds. A recovery is defined as a
     * successful build that follows a build that was not successful. Always
     * returns false for aborted builds.
     *
     * @param build the Build object
     * @return true if this build represents a recovery or failure
     */
    protected boolean isFailureOrRecovery(AbstractBuild<?, ?> build) {
        if (build.getResult() == Result.FAILURE || build.getResult() == Result.UNSTABLE) {
            return true;
        } else if (build.getResult() == Result.SUCCESS) {
                AbstractBuild<?, ?> previousBuild = build.getPreviousBuild();
            if (previousBuild != null && previousBuild.getResult() != Result.SUCCESS) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
    /**
     * Determine if this build results should be tweeted. Uses the local
     * settings if they are provided, otherwise the global settings.
     *
     * @param build the Build object
     * @return true if we should tweet this build result
     */
    protected boolean shouldPublish(AbstractBuild<?, ?> build) {
        return isFailureOrRecovery(build);
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        private static final Logger LOGGER = Logger.getLogger(DescriptorImpl.class.getName());
        public String hookurl;
        public String hudsonUrl;
        public String data;
        public String method;

        public DescriptorImpl() {
            super(WebhookPublisher.class);
            load();
            LOGGER.log(Level.INFO, "after load:" + this.hookurl);
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // set the booleans to false as defaults
            req.bindParameters(this, "webhook.");
            LOGGER.log(Level.INFO, "before save:" + this.hookurl);
            hudsonUrl = Mailer.descriptor().getUrl();


            save();
            
            return super.configure(req, formData);
        }
        @Override
        public String getDisplayName() {
            return "Webhook Publisher";
        }
        public String getHookurl() {
            return hookurl;
        }
        public String getMethod() {
            return method;
        }
        public String getUrl() {
            return hudsonUrl;
        }
        public String getData() {
            return data;
        }
        @SuppressWarnings("unchecked")
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }
        @Override
        public Publisher newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            if (hudsonUrl == null) {
                // if Hudson URL is not configured yet, infer some default
                hudsonUrl = Functions.inferHudsonURL(req);
                save();
            }
            return super.newInstance(req, formData);
        }
    }
}