<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "DTD/xhtml1-strict.dtd">
<%@page import="eu.etaxonomy.cdm.server.Bootloader"%>
<%@page import="java.util.Set" %>
<%@page import="java.net.URL" %>
<%@page import="eu.etaxonomy.cdm.server.CdmInstanceProperties"%>
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">

<head>
  <title>CDM Server</title>
  <link type="text/css" rel="stylesheet" media="all" href="css/style.css" />
  <link type="text/css" rel="stylesheet" media="all" href="css/server.css" />
  <script type="text/javascript" src="js/jquery.js"></script>
  <script type="text/javascript" src="js/oai-pmh-status.js"></script>
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
                  <div class="block" id="status">
                    Status: <div class="status_value">RUNNING</div>
                  </div>
                </div>
                <div class="block-wrapper">
                Actions:
                  <ul>
                      <li><a href="manage/index.jsp">EDIT CDM-Server management (login required!)</a></li>
                  </ul>
                </div>
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
