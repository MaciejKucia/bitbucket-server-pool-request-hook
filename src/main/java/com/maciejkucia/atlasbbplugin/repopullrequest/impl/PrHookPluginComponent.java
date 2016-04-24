package com.maciejkucia.atlasbbplugin.repopullrequest.impl;

import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.ApplicationProperties;
import javax.inject.Inject;
import javax.inject.Named;

@ExportAsService ({PrHookPluginComponent.class})
@Named ("PrHookPluginComponent")
class PrHookPluginComponent
{
    @ComponentImport
    private final ApplicationProperties applicationProperties;

    @Inject
    public PrHookPluginComponent(final ApplicationProperties applicationProperties)
    {
        this.applicationProperties = applicationProperties;
    }

    public String getName()
    {
        if(null != applicationProperties)
        {
            return "myComponent:" + applicationProperties.getDisplayName();
        }
        
        return "myComponent";
    }
}