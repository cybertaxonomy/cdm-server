<%@page import="eu.etaxonomy.cdm.server.AssumedMemoryRequirements"%>
<%@page import="org.codehaus.jackson.node.JsonNodeFactory"
%><%@page import="org.codehaus.jackson.node.ArrayNode"
%><%@ page contentType="application/json;charset=UTF-8" language="java"
%><%@page import="java.util.Set"
%><%@page import="org.codehaus.jackson.map.ObjectMapper"
%><%@page import="org.codehaus.jackson.JsonNode"
%><%@page import="org.codehaus.jackson.node.ObjectNode"
%><%@page import="eu.etaxonomy.cdm.server.JvmManager"
%><%@page import="java.lang.management.MemoryUsage"
%><%@page import="org.codehaus.jackson.JsonFactory"
%><%@page import="org.codehaus.jackson.JsonGenerator"
%><%@page import="org.codehaus.jackson.map.ser.StdSerializerProvider"
%><%@page import="eu.etaxonomy.cdm.server.Bootloader"
%><%@page import="eu.etaxonomy.cdm.server.instance.InstanceManager"
%><%@page import="eu.etaxonomy.cdm.server.instance.CdmInstance"
%><%


    Bootloader bootloader = Bootloader.getBootloader();
    InstanceManager instanceManager = bootloader.getInstanceManager();

    String action = request.getParameter("action");
    String redirectTo = request.getParameter("redirectTo");

    if(action != null){

        if(action.equals("reloadConfig")){
            instanceManager.reLoadInstanceConfigurations();
        } else {
            String instanceName = request.getParameter("instanceName");
            if(instanceName == null) {
                response.sendError(400, "parameter 'instanceName' is missing");
            }
            CdmInstance instance =  instanceManager.getInstance(instanceName);
            if(instance == null){
                response.sendError(400, "parameter 'instanceName' refers non non existing instance: " + instanceName);
            }

            action = action.toLowerCase();
            if(action.equals("stop")){
                instanceManager.stop(instance);
            }
            if(action.equals("start")){
                bootloader.addCdmInstanceContext(instance);
                instanceManager.start(instance);
            }
            /* if(action.equals("update")){
                bootloader.addCdmInstanceContext(instance);
                instanceManager.updateToCurrentVersion(instance);
            } */
        }
        if(redirectTo != null){
          response.sendRedirect(redirectTo);
        }
    }
%>