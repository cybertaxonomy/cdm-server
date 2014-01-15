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
  java.util.List<CdmInstance> instances = bootloader.getCdmInstances();
  if(instances != null){
   int i = 0;

   ArrayNode arrayNode = jsonMapper.createArrayNode();

   for(CdmInstance instance: instances){
    i++;
    ObjectNode instanceNode = jsonMapper.createObjectNode();
    instanceNode.put("status", instance.getStatus().name());
    instanceNode.put("messages", jsonMapper.valueToTree(instance.getProblems()));


    Configuration conf = instance.getConfiguration();
    String basePath = request.getContextPath() + "/" + conf.getInstanceName();
      URL fullURL = new URL(request.getScheme(),
                  request.getServerName(),
                  request.getServerPort(),
                  basePath);

      JsonNode configNode = jsonMapper.valueToTree(conf);
      if(configNode instanceof ObjectNode){
           ((ObjectNode)configNode).put("basePath", basePath);
           ((ObjectNode)configNode).put("fullUrl", fullURL.toString());
           ((ObjectNode)configNode).remove("password");
      }

      ((ObjectNode)instanceNode).put("configuration", configNode);
      arrayNode.add(instanceNode);
   }
   out.append(arrayNode.toString());
  }%>