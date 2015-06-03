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
%><%

    //the servelt context must use the class loader of the Bootloader class otherwise
    //getting the status will not work in mulithreading environments !!!
    Bootloader bootloader = Bootloader.getBootloader();
    long recommendedMinimumHeap = bootloader.getInstanceManager().recommendedMinimumSpace(AssumedMemoryRequirements.HEAP_CDMSERVER, AssumedMemoryRequirements.HEAP_PER_INSTANCE, null);
    long recommendedMinimumPermGenSpace = bootloader.getInstanceManager().recommendedMinimumSpace(AssumedMemoryRequirements.PERM_GEN_SPACE_CDMSERVER, AssumedMemoryRequirements.PERM_GEN_SPACE_PER_INSTANCE, null);

    ObjectMapper jsonMapper = new ObjectMapper();

    response.setHeader("Content-Type", "application/json;charset=UTF-8");

    MemoryUsage  heapMemoryUsage = JvmManager.getHeapMemoryUsage();
    MemoryUsage  permGenSpaceUsage = JvmManager.getPermGenSpaceUsage();

    ObjectNode node = jsonMapper.createObjectNode();
    node.put("recommendedMinimumHeap", recommendedMinimumHeap);
    node.put("recommendedMinimumPermGenSpace", recommendedMinimumPermGenSpace);
    node.put("availableProcessors", JvmManager.availableProcessors());
    node.putPOJO("heapMemoryUsage", heapMemoryUsage);
    node.putPOJO("permGenSpaceUsage", permGenSpaceUsage);

    JsonFactory jsonFactory = new JsonFactory();
    JsonGenerator jg = jsonFactory.createJsonGenerator(out);
    jg.setCodec(jsonMapper);
    node.serialize(jg, new StdSerializerProvider());
    out.append("}"); //TODO why do we have to add the closing bracket ecplicitely ???? ==> it seems as if the serialization stops at some point du to an exception
%>