package com.aliyun.www.cos;
import static com.aliyun.www.cos.utils.CredentialUtils.lookupSystemCredentials;
import static org.apache.commons.lang.StringUtils.isNotBlank;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

import javax.annotation.CheckForNull;
import javax.servlet.ServletException;

import com.aliyun.www.cos.projects.Project;
import com.aliyun.www.cos.projects.ReturnMsg;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.docker.commons.credentials.DockerServerCredentials;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.aliyun.www.cos.utils.CredentialsListBoxModel;
import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.ItemGroup;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

/**
 * Sample {@link Builder}.
 *
 * <p>
 * When the user configures the project and enables this builder,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked
 * and a new {@link DeployBuilder} is created. The created
 * instance is persisted to the project configuration XML by using
 * XStream, so this allows you to use instance fields (like {@link #masterurl})
 * to remember the configuration.
 *
 * <p>
 * When a build is performed, the {@link #perform} method will be invoked. 
 *
 * @author Kohsuke Kawaguchi
 */
public class DeployBuilder extends Builder implements SimpleBuildStep {

    @CheckForNull
    private String masterurl;

    @CheckForNull
    private String credentialsId;
    private String appName;
    private String composeTemplate;
    private String compose;
    public Project project;
    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public DeployBuilder(String masterurl) {setMasterurl(masterurl);}

    /**
     * We'll use this from the <tt>config.jelly</tt>.
     */
    public String getMasterurl() {return masterurl;}

    public void setMasterurl(String masterurl){this.masterurl = masterurl; }

    public String getAppName(){ return appName; }

    @DataBoundSetter
    public void setAppName(String appName) {this.appName = appName; }

    public String getComposeTemplate(){ return composeTemplate; }

    @DataBoundSetter
    public void setComposeTemplate(String composeTemplate) {
        this.composeTemplate = composeTemplate;
    }

    public String getCredentialsId() {return credentialsId; }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    @Override
    public void perform(Run<?,?> build, FilePath workspace, Launcher launcher, TaskListener listener) {
        // This is where you 'build' the project.
        // Since this is a dummy, we just say 'hello world' and call that a build.

        // This also shows how you can consult the global configuration of the builder
//        if (getDescriptor().getUseFrench())
//            listener.getLogger().println("Bonjour, "+name+"!");
//        else
//            listener.getLogger().println("Hello, "+name+"!");
        ReturnMsg returnMsg = null;
        listener.getLogger().println("master-url is : " + masterurl);
        listener.getLogger().println("credential is : " + credentialsId);
        listener.getLogger().println("appName is : " + appName);
        if (isNotBlank(credentialsId)) {
            Credentials credentials = lookupSystemCredentials(credentialsId);
            if (credentials instanceof DockerServerCredentials) {
                final DockerServerCredentials dockerCreds = (DockerServerCredentials) credentials;
                project = new Project(masterurl,
                        dockerCreds.getServerCaCertificate(),
                        dockerCreds.getClientCertificate(),
                        dockerCreds.getClientKey()
                        );
                FilePath composePath = workspace.child(composeTemplate);
                try {
                    compose = composePath.readToString();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                int exist = project.IsProjectExist(appName);
                if (exist == 0) {
                    String time="";
                    SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
                    time = df.format(new Date());
                    returnMsg = project.RefreshProject(appName, compose, time);
                }
                else {
                    returnMsg = project.AddProject(appName, compose);
                }

                if(returnMsg.getIsSuccess()) {
                    listener.getLogger().println("returnCode is : " + returnMsg.getReturnCode());
                    listener.getLogger().println("Send deploy request successful!");
                }
                else {
                    listener.getLogger().println("returnCode is : " + returnMsg.getReturnCode());
                    listener.getLogger().println("detail message is : " + returnMsg.getDetailMsg());
                    listener.getLogger().println("Send deploy request fail!");

                }

                try {
                    waitForProject(60, listener);
                } catch (Exception e) {
                    listener.getLogger().println("error happen when wait for deploy project, " + e.getMessage());
                    return;
                }


                project.destroy();
            }
        }
    }


    private void waitForProject(int timeoutMin, TaskListener listener) throws Exception {
        int oneMinMils = 1000 * 30;
        String status;
        int i = timeoutMin;
        for (; i > 0; i--) {
            try {
                 status = project.QueryProjectStatus(appName);
            } catch (Exception e) {
                listener.getLogger().println("deploy is not finished, will retry after one minute");
                try {
                    Thread.sleep(oneMinMils);
                } catch (InterruptedException e1) {
                }
                continue;
            }
            if (!"running".equals(status)) {
                if("failed".equals(status)){
                    listener.getLogger().println(
                            "deploy failed! " + Objects.toString(status));
                            throw new RuntimeException("deploy failed!");
                }
                try {
                    listener.getLogger().println(
                            "deployment is not finished!, will retry after one minute, status: " + Objects.toString(status));
                    Thread.sleep(oneMinMils);
                    continue;
                } catch (InterruptedException e) {
                }
            } else {
                listener.getLogger().println(
                        "deploy successfully!, will go into next phase, status: " + Objects.toString(status));
                try {
                    Thread.sleep(oneMinMils * 1 / 2);
                } catch (InterruptedException e) {
                }
                break;
            }
        }

        if (i == 0) {
            listener.getLogger().println("deploy failed, timeout");
            throw new RuntimeException("deploy failed, timeout");
        }
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) Jenkins.getActiveInstance().getDescriptor(DeployBuilder.class);
    }

    /**
     * Descriptor for {@link DeployBuilder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     *
     * <p>
     * See <tt>src/main/resources/hudson/plugins/hello_world/DeployBuilder/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        /**
         * To persist global configuration information,
         * simply store it in a field and call save().
         *
         * <p>
         * If you don't want fields to be persisted, use <tt>transient</tt>.
         */
//        private boolean useFrench;

        /**
         * In order to load the persisted global configuration, you have to 
         * call load() in the constructor.
         */
        public DescriptorImpl() {
            load();
        }

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath ItemGroup context) {
            List<StandardCredentials> credentials =
                    CredentialsProvider.lookupCredentials(StandardCredentials.class, context, ACL.SYSTEM,
                            Collections.<DomainRequirement>emptyList());
            return new CredentialsListBoxModel()
                    .withEmptySelection()
                    .withMatching(CredentialsMatchers.always(), credentials);
        }
        
        /**
         * Performs on-the-fly validation of the form field 'name'.
         *
         * @param value
         *      This parameter receives the value that the user has typed.
         * @return
         *      Indicates the outcome of the validation. This is sent to the browser.
         *      <p>
         *      Note that returning {@link FormValidation#error(String)} does not
         *      prevent the form from being saved. It just means that a message
         *      will be displayed to the user. 
         */
        public FormValidation doCheckMasterurl(@QueryParameter String value)
                throws IOException, ServletException {
//            if (value.length() == 0)
//                return FormValidation.error("Please set a name by robin3");
//            if (value.length() < 4)
//                return FormValidation.warning("Isn't the name too short?");
            return FormValidation.ok();
        }


        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Aliyun Container Service Deploy";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
//            useFrench = formData.getBoolean("useFrench");
            // ^Can also use req.bindJSON(this, formData);
            //  (easier when there are many fields; need set* methods for this, like setUseFrench)
            save();
            return super.configure(req,formData);
        }

        /**
         * This method returns true if the global configuration says we should speak French.
         *
         * The method name is bit awkward because global.jelly calls this method to determine
         * the initial state of the checkbox by the naming convention.
         */
//        public boolean getUseFrench() {
//            return useFrench;
//        }
    }
}

