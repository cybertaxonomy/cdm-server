/**
* Copyright (C) 2020 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.server.logging;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.bridge.SLF4JBridgeHandler;

import eu.etaxonomy.cdm.server.instance.CdmInstance;

/**
 * The technique used in this class is based on the example
 * for a jetty server which uses a deployment manager, explained
 * in https://www.eclipse.org/jetty/documentation/9.4.29.v20200521/example-logging-logback-centralized.html.
 * In our situation of an embedded jetty which manages the cdm-webapp instance directly the
 * configuration need to be a bit different.
 *
 * The config files of the official example can be downloaded from
 * https://repo1.maven.org/maven2/org/eclipse/jetty/jetty-webapp-logging/9.4.20.v20190813/jetty-webapp-logging-9.4.20.v20190813-config.jar
 * extract the jar to examine the contained config files.
 *
 * @author a.kohlbecker
 * @since Jun 10, 2020
 */
public class LoggingConfigurator {

    public void configureServer() {
        // Configure logging (1)


        // > jetty-webapp-logging-9.4.20.v20190813-config/etc/jetty-jul-to-slf4j.xml
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        // >
        // jetty-webapp-logging-9.4.20.v20190813-config/resources/jetty-logging.properties
        System.setProperty("org.eclipse.jetty.util.log.class", org.eclipse.jetty.util.log.Slf4jLog.class.getName());

    }

    public Handler configureWebApp(WebAppContext cdmWebappContext, CdmInstance instance) {

        // > jetty-webapp-logging-9.4.20.v20190813-config/etc/jetty-webapp-logging.xml
        // ---> adds the org.eclipse.jetty.webapp.logging.CentralizedWebAppLoggingBinding
        // (from jetty-webapp-logging-9.4.20.v20190813.jar) to the DeploymentManager,
        // in the  cdm-server we are not using the DeploymentManager so
        // this needs to be done per web app explicitely:
        cdmWebappContext.addSystemClass("org.apache.log4j.");
        cdmWebappContext.addSystemClass("org.slf4j.");
        cdmWebappContext.addSystemClass("org.apache.commons.logging.");

        // > jetty-webapp-logging-9.4.20.v20190813-config/etc/jetty-mdc-handler.xml
        InstanceLogWrapper mdcHandler = new InstanceLogWrapper(instance.getName());
        mdcHandler.setHandler(cdmWebappContext); // wrap context handler
        return mdcHandler;
    }
}
