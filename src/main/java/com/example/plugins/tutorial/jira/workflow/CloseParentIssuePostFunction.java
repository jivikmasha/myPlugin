package com.example.plugins.tutorial.jira.workflow;

import java.util.*;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.config.SubTaskManager;
import com.atlassian.jira.issue.*;
import com.atlassian.jira.issue.link.IssueLink;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.link.LinkCollection;
import com.atlassian.jira.issue.resolution.Resolution;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.workflow.JiraWorkflow;
import com.atlassian.jira.workflow.WorkflowManager;
import com.atlassian.jira.workflow.function.issue.AbstractJiraFunctionProvider;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.WorkflowException;

import com.opensymphony.workflow.loader.ActionDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the post-function class that gets executed at the end of the transition.
 * Any parameters that were saved in your factory class will be available in the transientVars Map.
 */
@Scanned
public class CloseParentIssuePostFunction extends AbstractJiraFunctionProvider
{
    private static final Logger log = LoggerFactory.getLogger(CloseParentIssuePostFunction.class);
    public static final String FIELD_MESSAGE = "messageField";
    private final WorkflowManager workflowManager;
    private final SubTaskManager subTaskManager;
    private final IssueManager issueManager;
    private final JiraAuthenticationContext authenticationContext;
    private final Status closedStatus;
    private Resolution resolution;

    public CloseParentIssuePostFunction(@ComponentImport ConstantsManager constantsManager, @ComponentImport WorkflowManager workflowManager,
                                        @ComponentImport SubTaskManager subTaskManager, @ComponentImport JiraAuthenticationContext authenticationContext, @ComponentImport IssueManager issueManager) {
        this.workflowManager = workflowManager;
        this.subTaskManager = subTaskManager;
        this.authenticationContext = authenticationContext;
        this.issueManager = issueManager;
        System.out.println("constructor");
        closedStatus = constantsManager.getStatus(Integer.toString(IssueFieldConstants.CLOSED_STATUS_ID));
        System.out.println("notice me");
        System.out.println(closedStatus);
    }

    public void execute(Map transientVars, Map args, PropertySet ps)throws WorkflowException{
        System.out.println("exec");
        // Retrieve the sub-task
        MutableIssue subTask=getIssue(transientVars);


        ///////
        List<Issue> linkedIssuesToSub = new ArrayList<Issue>();
        IssueLinkManager issueLinkManager = ComponentAccessor.getIssueLinkManager();
        for(IssueLink issueLink: issueLinkManager.getInwardLinks(subTask.getId())){
            System.out.println(issueLink.getDestinationObject().getKey());
            System.out.println();
            linkedIssuesToSub.add(issueManager.getIssueObject(issueLink.getSourceObject().getKey()));
            System.out.println("");

        }
        /////


        // Retrieve the parent issue
//        MutableIssue parentIssue = ComponentAccessor.getIssueManager().getIssueObject(subTask.getParentId());

        //Le Costil 2 - пока что будем доставать будто одна родителтская задача боже хочу спать
        Issue parentIssue = null;
        try {
            parentIssue = linkedIssuesToSub.get(0);
        }
        catch (Exception e) {
            return;
        }

        if (parentIssue == null) {
            return;
        }

        // Ensure that the parent issue is not already closed
        if (IssueFieldConstants.CLOSED_STATUS_ID == Integer.parseInt(parentIssue.getStatusId()))
        {
            return;
        }


        //////////////////////////
        // Get all linked tasks :3
        Collection<Issue> linkedIssues = new ArrayList<Issue>();


//        IssueLinkManager issueLinkManager = ComponentAccessor.getIssueLinkManager();
        for(IssueLink issueLink: issueLinkManager.getOutwardLinks(parentIssue.getId())){
            System.out.println(issueLink.getDestinationObject().getKey());
            System.out.println();
            linkedIssues.add(issueManager.getIssueObject(issueLink.getDestinationObject().getKey()));
            System.out.println("");

        }


        // Check that ALL OTHER sub-tasks are closed
//        Collection<Issue> subTasks = subTaskManager.getSubTaskObjects(parentIssue);

        for (Iterator<Issue> iterator = linkedIssues.iterator(); iterator.hasNext();)
        {
            Issue associatedSubTask =  iterator.next();
            if (!subTask.getKey().equals(associatedSubTask.getKey()))
            {
                // If other associated sub-task is still open - do not continue
                if (IssueFieldConstants.CLOSED_STATUS_ID !=
                        Integer.parseInt(associatedSubTask.getStatusId()))
                {
                    return;
                }
            }
        }

        // All sub-tasks are now closed - close the parent issue
        try
        {
            System.out.println("TRY");
            resolution = subTask.getResolution();
            closeIssue(parentIssue);
            System.out.println("TRY DONE");
        }
        catch (WorkflowException e)
        {
            System.out.println("error");
            log.error("Error occurred while closing the issue: " + parentIssue.getString("key") + ": " + e, e);
            e.printStackTrace();
        }
    }

    private void closeIssue(Issue issue) throws WorkflowException
    {
        Status currentStatus = issue.getStatus();
        JiraWorkflow workflow = workflowManager.getWorkflow(issue);
        List<ActionDescriptor> actions = workflow.getLinkedStep(currentStatus).getActions();
        System.out.println(actions);
        // look for the closed transition
        ActionDescriptor closeAction = null;
        System.out.println(closedStatus.getName());
        System.out.println(closedStatus);
        for (ActionDescriptor descriptor : actions)
        {
            System.out.println("descriptor");
            System.out.println(descriptor.getUnconditionalResult().getStatus());
            if (descriptor.getUnconditionalResult().getStatus().equals(closedStatus.getName()))
            {
                closeAction = descriptor;
                System.out.println("found");
                break;
            }
        }
        System.out.println(closeAction);

        if (closeAction != null)
        {
            System.out.println("doing");
            ApplicationUser currentUser =  authenticationContext.getLoggedInUser();
            IssueService issueService = ComponentAccessor.getIssueService();
            IssueInputParameters parameters = issueService.newIssueInputParameters();
            parameters.setRetainExistingValuesWhenParameterNotProvided(true);
            IssueService.TransitionValidationResult validationResult =
                    issueService.validateTransition(currentUser, issue.getId(),
                            closeAction.getId(), parameters);
            IssueService.IssueResult result = issueService.transition(currentUser, validationResult);
            System.out.println("done");
        }

//        if (closeAction != null)
//        {
//            System.out.println("doing");
//            ApplicationUser currentUser =  authenticationContext.getLoggedInUser();
//            IssueService issueService = ComponentAccessor.getIssueService();
//            IssueInputParameters parameters = issueService.newIssueInputParameters();
//            parameters.setRetainExistingValuesWhenParameterNotProvided(true);
//
////            parameters.setResolutionId(resolution.getId());
//            IssueService.TransitionValidationResult validationResult =
//                    issueService.validateTransition(currentUser, issue.getId(), closeAction.getId(), parameters);
//            IssueService.IssueResult result = issueService.transition(currentUser, validationResult);
//            System.out.println("done");
//        }
    }
}