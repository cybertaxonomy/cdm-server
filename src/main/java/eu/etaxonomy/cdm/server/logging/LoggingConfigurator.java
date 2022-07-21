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
 * in http://www.eclipse.org/jetty/documentation/jetty-9/index.html#example-logging-logback-centralized
 * In our situation of an embedded jetty which manages the cdm-webapp instance directly the
 * configuration need to be a bit different.
 *
 * The config files of the official example can found on github:
 * https://github.com/jetty-project/jetty-webapp-logging/
 *
 * @author a.kohlbecker
 * @since Jun 10, 2020
 */
public class LoggingConfigurator {

    public void configureServer() {
        // Configure logging (1)

        // v20190813   > https://github.com/jetty-project/jetty-webapp-logging/blob/jetty-webapp-logging-9.4.20.v20190813/jetty-webapp-logging/src/main/config/etc/jetty-jul-to-slf4j.xml
        // Apr 2, 2020 > https://github.com/jetty-project/jetty-webapp-logging/blob/master/jetty-webapp-logging/src/main/config/etc/jetty-jul-to-slf4j.xml
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        // >
        // jetty-webapp-logging-9.4.20.v20190813-config/resources/jetty-logging.properties
        System.setProperty("org.eclipse.jetty.util.log.class", org.eclipse.jetty.util.log.Slf4jLog.class.getName());

    }

    public Handler configureWebApp(WebAppContext cdmWebappContext, CdmInstance instance) {

        // v20190813   > https://github.com/jetty-project/jetty-webapp-logging/blob/jetty-webapp-logging-9.4.20.v20190813/jetty-webapp-logging/src/main/config/etc/jetty-webapp-logging.xml
        // Apr 2, 2020 > https://github.com/jetty-project/jetty-webapp-logging/blob/master/jetty-webapp-logging/src/main/java/org/eclipse/jetty/webapp/logging/CentralizedWebAppLoggingBinding.java
        // ---> adds the org.eclipse.jetty.webapp.logging.CentralizedWebAppLoggingBinding
        // (from jetty-webapp-logging-9.4.20.v20190813.jar) to the DeploymentManager,
        // in the  cdm-server we are not using the DeploymentManager so
        // this needs to be done per web app explicitly:
        cdmWebappContext.getSystemClasspathPattern().add("org.apache.log4j.");  //log4j12  probably not needed anymore
        cdmWebappContext.getSystemClasspathPattern().add("org.apache.logging.log4j."); //log4j2
        cdmWebappContext.getSystemClasspathPattern().add("org.slf4j.");
        cdmWebappContext.getSystemClasspathPattern().add("org.apache.commons.logging.");

        // UPDATE:
        // in the latest version of the jetty-webapp-logging (Apr 2, 2020) the classnames are also removed from the ServerClasspathPatterns:
        cdmWebappContext.getServerClasspathPattern().add("-org.apache.log4j.");
        cdmWebappContext.getServerClasspathPattern().add("-org.apache.logging.log4j.");
        cdmWebappContext.getServerClasspathPattern().add("-org.slf4j.");
        cdmWebappContext.getServerClasspathPattern().add("-org.apache.commons.logging.");


        // v20190813  > https://github.com/jetty-project/jetty-webapp-logging/blob/jetty-webapp-logging-9.4.20.v20190813/jetty-webapp-logging/src/etc/jetty-mdc-handler.xml
        // Apr 2, 2020 > https://github.com/jetty-project/jetty-webapp-logging/blob/master/jetty-webapp-logging/src/main/config/etc/jetty-mdc-handler.xml
        InstanceLogWrapper mdcHandler = new InstanceLogWrapper(instance.getName());
        mdcHandler.setHandler(cdmWebappContext); // wrap context handler
        return mdcHandler;
    }
}
