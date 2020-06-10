/**
* Copyright (C) 2020 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.server.logging;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.slf4j.MDC;

/**
 * @author a.kohlbecker
 * @since Jun 9, 2020
 */
public class InstanceLogWrapper extends HandlerWrapper {


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
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
        // Collect Info for NDC/MDC
        MDC.put(CDM_INSTANCE, instanceName);
        try
        {
            super.handle(target, baseRequest, request, response);
        }
        finally
        {
            // Pop info out / clear the NDC/MDC
            MDC.clear();
        }
    }

}
