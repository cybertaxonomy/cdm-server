<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "DTD/xhtml1-strict.dtd">
<%@page import="eu.etaxonomy.cdm.server.Bootloader"%>
<%@page import="java.util.Set"%>
<%@page import="java.net.URL"%>
<%@page import="eu.etaxonomy.cdm.server.instance.Configuration"%>
<%@page import="eu.etaxonomy.cdm.server.instance.CdmInstance"%>
<%@page import="eu.etaxonomy.cdm.server.instance.Status"%>
<%@page import="eu.etaxonomy.cdm.server.JvmManager"%>
<%@page import="java.io.IOException"%>
<%!// the servelt context must use the class loader of the Bootloader class otherwise
    // getting the status will not work in mulithreading environments !!!
    Bootloader bootloader = Bootloader.getBootloader();%>

<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">

<head>
<title>CDM Server</title>
<link type="text/css" rel="stylesheet" media="all"  href="../css/style.css" />
<link type="text/css" rel="stylesheet" media="all"  href="../css/server.css" />
</head>
<body class="layout-main">
  <div id="page" class="clearfix">
    <div id="header-wrapper">
      <div id="header" class="clearfix">
        <div id="header-first">
          <div id="logo"></div>
          <h1>CDM Server</h1>
        </div>
        <!-- /header-first -->
      </div>
      <!-- /header -->
    </div>
    <!-- /header-wrapper -->

    <div id="primary-menu-wrapper" class="clearfix">
      <div id="primary-menu">
        <div id="version"><%=bootloader.readCdmRemoteVersion()%></div>
      </div>
      <!-- /primary_menu -->
    </div>
    <!-- /primary-menu-wrapper -->

    <div id="main-wrapper">
      <div id="main" class="clearfix">

        <div id="sidebar-first"></div>
        <!-- /sidebar-first -->

        <div id="content-wrapper">
          <div id="content">
            <!-- ============================= -->
            <div class="block-wrapper">
              <h2 class="title block-title pngfix">Server Status</h2>
              <div class="block" id="status">
                <!-- The memory status will be rendered by memory-status.js -->
              </div>
            </div>


            <div class="block-wrapper">
              <div class="block" id="instances">
                <h2 class="title block-title pngfix">CDM Server Instances</h2>
                <div><a class="redirect-rewrite" href="Action.jsp?&redirectTo=./&action=reloadConfig">Reload configuration</a></div>
                <div class="container">
                  <table>
                    <tr>
                      <th>Path</th>
                      <th></th>
                      <th>Database Url</th>
                      <th>Status</th>
                                            <th>Operation</th>
                      <th>OAI-PMH Provider</th>
                    </tr>
                    <%
                        java.util.List<CdmInstance> instances = bootloader.getCdmInstances();
                        if (instances != null) {
                            int i = 0;
                            for (CdmInstance instance : instances) {
                                i++;
                                Configuration props = instance.getConfiguration();

                                String basePath = props.getInstanceName();
                                /*  URL fullURL = new URL(request.getScheme(),
                                         request.getServerName(),
                                         request.getServerPort(),
                                         basePath); */

                                String fullURL = "../" + basePath;

                                // prepare actions parameters
                                String action = null;
                                String actionParams = null;
                                if(instance.getStatus().equals(Status.started) || instance.getStatus().equals(Status.starting)){
                                    action = "stop";
                                } else if(!instance.getStatus().equals(Status.removed) && !instance.getStatus().equals(Status.uninitialized)){
                                    action = "start";
                                }
                                if(action != null){
                                    actionParams = "instanceName=" + props.getInstanceName() + "&redirectTo=./&action=" + action;
                                }

                                // styling
                                String oddOrEven = i % 2 == 0 ? "odd" : "even";

                                // render a table row
                                out.append("<tr id=\"" + basePath + "\" class=\"entry " + oddOrEven + "\">");
                                out.append("<td class=\"base-url\"><a href=\"" + fullURL + "/\">" + basePath + "</a></td>");
                                out.append("<td class=\"test-url\"><a href=\"" + fullURL + "/portal/classification\">Test</a></td>");
                                out.append("<td class=\"db-url\">" + props.getDataSourceUrl() + "</td>");
                                out.append("<td class=\"status " + instance.getStatus() + "\">" + instance.getStatus() + "</td>");
                                out.append("<td class=\"operation\">" + (actionParams != null ?
                                        "<a class=\"redirect-rewrite\" href=\"Action.jsp?" + actionParams + "\">" + action + "</a>"
                                        : "&nbsp;")
                                        + "</td>");

                                // OAI-PMH Status will be requested using javascript
                                out.append("<td class=\"oai-pmh\">requesting status ...</td>");
                                out.append("</tr>");
                                out.append("<tr id=\"" + basePath + "-messages\" class=\"messages " + oddOrEven + "\">");
                                out.append("<td></td><td class=\"error messages\" colspan=\"5\">");
                                for (String problem : instance.getProblems()) {
                                    out.append("<div>" + problem + "</div>");
                                }
                                out.append("</td>");
                                out.append("</tr>");
                            }
                        }
                    %>
                  </table>
                </div>
              </div>
            </div>
            <%
                /*
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
          </div>
          <!-- /content -->
        </div>
        <!-- /content-wrapper -->

        <div id="footer" class="clearfix">
          The CDM Server is a component of the <a
            href="http://wp5.e-taxonomy.eu/">EDIT Platform for
            Cybertaxonomy</a>.
        </div>
        <!-- /footer -->

      </div>
      <!-- /main -->
    </div>
    <!-- /main-wrapper -->
  </div>
  <!-- /page -->
  <script type="text/javascript" src="../js/jquery.js"></script>
  <script type="text/javascript" src="../js/oai-pmh-status.js"></script>
  <script type="text/javascript" src="../js/memory-status.js"></script>
  <script type="text/javascript" src="../js/instance-status.js"></script>
  <script type="text/javascript" src="../js/redirect-to-rewriter.js"></script>
</body>
</html>
