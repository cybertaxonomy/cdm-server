<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "DTD/xhtml1-strict.dtd">
<%@page import="eu.etaxonomy.cdm.server.Bootloader"%>
<%@page import="java.util.Set" %>
<%@page import="java.net.URL" %>
<%@page import="eu.etaxonomy.cdm.server.CdmInstanceProperties"%>
<%@page import="eu.etaxonomy.cdm.server.JvmManager" %>
<%@page import="java.io.IOException"%>
<%!

// the servelt context must use the class loader of the Bootloader class otherwise
// getting the status will not work in mulithreading environments !!!
Bootloader bootloader = Bootloader.getBootloader();

%>

<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">

<head>
  <title>CDM Server</title>
  <link type="text/css" rel="stylesheet" media="all" href="../css/style.css" />
  <link type="text/css" rel="stylesheet" media="all" href="../css/server.css" />
  <script type="text/javascript" src="../js/jquery.js"></script>
  <script type="text/javascript" src="../js/oai-pmh-status.js"></script>
  <script type="text/javascript" src="../js/memory-status.js"></script>
  <script type="text/javascript" src="../js/instance-status.js"></script>
</head>
<body class="layout-main">
    <div id="page" class="clearfix">
      <div id="header-wrapper">
        <div id="header" class="clearfix">
          <div id="header-first">
            <div id="logo">
              </div>
        <h1>CDM Server</h1>
          </div><!-- /header-first -->
        </div><!-- /header -->
      </div><!-- /header-wrapper -->

      <div id="primary-menu-wrapper" class="clearfix">
        <div id="primary-menu">
            <div id="version"><%= bootloader.readCdmRemoteVersion() %></div>
        </div><!-- /primary_menu -->
      </div><!-- /primary-menu-wrapper -->

      <div id="main-wrapper">
        <div id="main" class="clearfix">

          <div id="sidebar-first">
          </div><!-- /sidebar-first -->

          <div id="content-wrapper">
            <div id="content">
                <!-- ============================= -->
                <div class="block-wrapper">
                  <h2 class="title block-title pngfix">Server Status</h2>
                  <div class="block" id="status"><!-- The memory status will be rendered by memory-status.js --></div>
                </div>


                <div class="block-wrapper">
                  <div class="block" id="datasources">
                    <h2 class="title block-title pngfix">CDM Server Instances</h2>
                    <div class="container">
                    <table>
                      <tr><th>Path</th><th> </th><th>Database Url</th><th>Status</th><th>OAI-PMH Provider</th></tr>
                                            <%
                                           java.util.List<CdmInstanceProperties> configAndStatus = bootloader.getConfigAndStatus();
                                           if(configAndStatus != null){
                                             int i = 0;
                                             for(CdmInstanceProperties props : configAndStatus){
                                               i++;

                                               String basePath = props.getDataSourceName();
                                                  /*  URL fullURL = new URL(request.getScheme(),
                                                           request.getServerName(),
                                                           request.getServerPort(),
                                                           basePath); */

                                                   String fullURL = "../" + basePath;

                                                   String oddOrEven = i % 2 == 0 ? "odd" : "even";
                                                   String noBottomBorder = props.getStatus().equals(CdmInstanceProperties.Status.error) ? " style=\"border-bottom:none;\"" : "";

                                               out.append("<tr id=\""+basePath+"\" class=\"entry " + oddOrEven + "\" " +noBottomBorder+ ">");
                                               out.append("<td class=\"base-url\"><a href=\"" + fullURL + "\">" + basePath + "</a></td>");
                                               out.append("<td class=\"test-url\"><a href=\"" + fullURL + "/portal/classification\">Test</a></td>");
                                                   out.append("<td class=\"db-url\">" + props.getUrl() + "</td>");
                                                   out.append("<td class=\"status " + props.getStatus() + "\">" + props.getStatus() + "</td>");

                                                   // OAI-PMH Status will be requested using javascript
                                                   out.append("<td class=\"oai-pmh\">requesting status ...</td>");
                                                   out.append("</tr>");
                                                   if(props.getStatus().equals(CdmInstanceProperties.Status.error) || !props.isEnabled()){
                                                     out.append("<tr id=\""+basePath+"-error-log\" class=\"error-log " + oddOrEven + "\">");
                                                     out.append("<td></td><td  class=\"error\" colspan=\"4\">");
                                                         for( String problem : props.getProblems()){
                                                           out.append("<div>" + problem + "</div>");
                                                         }
                                                     out.append("</td>");
                                                     out.append("</tr>");
                                                   }
                                             }
                                           }
                                           %>
                    </table>
                    </div>
                  </div>
                </div>
<%/*
                <div class="block-wrapper">
                  <div class="block" id="test">
                    <h2 class="title block-title pngfix">Test your CDM Server (using the default data base)</h2>
                    <form name="input" action="/default/portal/taxon/find" method="get">
                    <input type="text" name="query"></br>
                    <input type="submit" value="submit">
                    </form>
                  </div>
                </div><!-- test -->
*/
%>
                <!-- ============================= -->
            </div><!-- /content -->
          </div><!-- /content-wrapper -->

          <div id="footer" class="clearfix">
          The CDM Server is a component of the <a href="http://wp5.e-taxonomy.eu/">EDIT Platform for Cybertaxonomy</a>.
          </div><!-- /footer -->

        </div><!-- /main -->
      </div><!-- /main-wrapper -->
    </div><!-- /page -->
</body>
</html>
