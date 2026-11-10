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
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.slf4j.MDC;

/**
 * see also https://github.com/jetty-project/jetty-webapp-logging/blob/master/jetty-webapp-logging/src/main/java/org/eclipse/jetty/webapp/logging/ContextLogHandler.java
 *
 * @author a.kohlbecker
 * @since Jun 9, 2020
 */
public class InstanceLogWrapper extends Handler.Wrapper {

    /**
     * Key under which the instance name stored in the
     * Mapped Diagnostic Context (MDC)
     */
    public static final String CDM_INSTANCE = "cdmInstance";

    private String instanceName;

    public InstanceLogWrapper(String instanceName) {
        this.instanceName = instanceName;
    }

    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception {

        // Collect Info for NDC/MDC
        MDC.put(CDM_INSTANCE, instanceName);
        try {
            return super.handle(request, response, callback);
        }
        finally {
            // Pop info out / clear the NDC/MDC
            MDC.clear();
        }
    }
}