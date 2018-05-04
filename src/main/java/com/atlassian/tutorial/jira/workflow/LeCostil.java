package com.atlassian.tutorial.jira.workflow;

import com.atlassian.jira.plugin.workflow.AbstractWorkflowPluginFactory;
import com.atlassian.jira.plugin.workflow.WorkflowPluginFactory;
import com.opensymphony.workflow.loader.AbstractDescriptor;

import java.util.Collection;
import java.util.Map;

/**
 * Created by Administrator on 05.05.2018.
 */
public class LeCostil extends AbstractWorkflowPluginFactory implements WorkflowPluginFactory {
    @Override
    public Map<String, Object> getVelocityParams(String resourceName, AbstractDescriptor descriptor) {
        return super.getVelocityParams(resourceName, descriptor);
    }

    @Override
    protected Map<String, ?> extractMultipleParams(Map<String, Object> params, Collection<String> paramNames) {
        return super.extractMultipleParams(params, paramNames);
    }

    @Override
    protected String extractSingleParam(Map<String, Object> conditionParams, String paramName) {
        return super.extractSingleParam(conditionParams, paramName);
    }

    @Override
    protected Map<String, String> createMap(Map<String, String> extractedParams) {
        return super.createMap(extractedParams);
    }

    @Override
    protected void getVelocityParamsForInput(Map<String, Object> map) {

    }

    @Override
    protected void getVelocityParamsForEdit(Map<String, Object> map, AbstractDescriptor abstractDescriptor) {

    }

    @Override
    protected void getVelocityParamsForView(Map<String, Object> map, AbstractDescriptor abstractDescriptor) {

    }

    @Override
    public Map<String, ?> getDescriptorParams(Map<String, Object> map) {
        return null;
    }

    public LeCostil() {
        super();
    }
}
