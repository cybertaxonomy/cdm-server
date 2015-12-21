<%@page import="org.codehaus.jackson.node.ArrayNode"
%><%@ page contentType="application/json;charset=UTF-8" language="java"
%><%@page import="eu.etaxonomy.cdm.server.Bootloader"
%><%@page import="eu.etaxonomy.cdm.server.instance.CdmInstance"
%><%@page import="java.util.Set"
%><%@page import="java.net.URL"
%><%@page import="org.codehaus.jackson.map.ObjectMapper"
%><%@page import="org.codehaus.jackson.JsonNode"
%><%@page import="org.codehaus.jackson.node.ObjectNode"
%><%@page import="eu.etaxonomy.cdm.server.instance.Configuration"
%><%//////////////////////////////////////////////////////////////////////////////////
//
// The BootloaderService service exposes the Bootloader.getConfigAndStatus()
// property as webservice. Before beeing serialized to JSON the ConfigAndStatus
// properties will be extended by the basePath of the cdm-remote instances.
// For security the password field will be hidden!
//
//////////////////////////////////////////////////////////////////////////////////

  ObjectMapper jsonMapper = new ObjectMapper();

  response.setHeader("Content-Type", "application/json;charset=UTF-8");

  // the servelt context must use the class loader of the Bootloader class otherwise
  // getting the status will not work in multihreading environments !!!
  Bootloader bootloader = Bootloader.getBootloader();
  
  ObjectNode infoNode = jsonMapper.createObjectNode();
  infoNode.put("cdmlibServicesVersion", bootloader.getCdmlibServicesVersion());
  infoNode.put("cdmlibServicesLastModified", bootloader.getCdmlibServicesLastModified());

  out.append(infoNode.toString());
  %>