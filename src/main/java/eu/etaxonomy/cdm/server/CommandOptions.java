/**
 * Copyright (C) 2009 EDIT
 * European Distributed Institute of Taxonomy
 * http://www.e-taxonomy.eu
 *
 * The contents of this file are subject to the Mozilla Public License Version 1.1
 * See LICENSE.TXT at the top of this package for the full license terms.
 */

package eu.etaxonomy.cdm.server;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

public class CommandOptions{

    private static Options options = null;

    public static final Option HELP = new Option( "help", "print this message" );
    public static final Option JMX = new Option( "jmx", "Start the server with the Jetty MBeans in JMX Management mode. \n" +
            "For testing you can use the following jvm options:\n" +
            "   -Dcom.sun.management.jmxremote.ssl=false\n" +
            "   -Dcom.sun.management.jmxremote.authenticate=false\n" +
            "   -Dcom.sun.management.jmxremote.port=9999" );
    public static final Option WIN32SERVICE= new Option("win32service", "ONLY USED INTERNALLY - prepare for running as win32 service, the server will not be started automatically!");

    @SuppressWarnings("static-access")
    public static final Option WEBAPP = OptionBuilder
            .withArgName("file")
            .hasArg()
            .withDescription( "Defines the webapplication to run from, this either can be a compressed war or extracted file.\n" +
                    "Defaults to the cdm-remote-webapp.war which is found in cdm-server/traget\n" +
                    "If this option is used extracting the war from the cdmserver jar file is omitted.\n \n" +
                    "DEVELOPMENT MODE:\n" +
                    "  If the specified path points to a directory the cdm-server will run in development mode.\n" +
                    "  In development mode the default webapplication containing the cdm-server management web \n" +
                    "  interface will be loaded from the source folder cdm-server/src/main/webapp. In normal \n" +
                    "  mode the default-webapp.war file will be used insead.\n" +
                    "  Using the following paths developers can run the cdm-webapp instances completely\n" +
                    "  from the target folder or from source (examples are for the eclipse ide):\n" +
                    "   - run from maven target: '{cdmlib-project-root}/cdm-webapp/target/cdmserver'\n " +
                    "   - run from source: '{cdmlib-project-root}/cdm-webapp/src/main/webapp'\n" +
                    "     When running from source you must also set the webapp-classpath option: \n" +
                    "     -webappClasspath=${project_classpath:cdm-webapp} " )
            .create("webapp");

  @SuppressWarnings("static-access")
    public static final Option WEBAPP_CLASSPATH = OptionBuilder
            .withArgName("classpath")
            .hasArg()
            .withDescription("Sets the classpath for the cdm-webapp instance when running from source code,\n" +
                    "e.g: ${project_classpath:cdm-webapp}\n" +
                    "See option -webapp")
            .create("webappClasspath");

    @SuppressWarnings("static-access")
    public static final Option HTTP_PORT = OptionBuilder
            .withArgName("httpPortNumber")
            .hasArg()
            .withDescription( "set the http listening port. Default is 8080")
            .create("httpPort") ;

    @SuppressWarnings("static-access")
    public static final Option LOG_DIR = OptionBuilder
            .withArgName("file")
            .hasArg()
            .withDescription( "Alternative location to write logs to. " +
                    "By default the cdm server will use ${user.home}/.cdmLibrary/logs/" )
            .create("logdir");

    @SuppressWarnings("static-access")
    public static final Option DATASOURCES_FILE = OptionBuilder
        .withArgName("datasourcesfile")
        .hasArg()
        .withDescription( "use the specified datasources file. Default is {user.home}/.cdmLibrary/datasources.xml")
        .create("datasources");

    @SuppressWarnings("static-access")
    public static final Option CONTEXT_PATH_PREFIX = OptionBuilder
            .withArgName("url path element")
            .hasArg()
            .withDescription(
                    "The url path element to use as prefix for all cdm-server instances.\n" +
                    "Per default the instances are running at the server root.")
            .create("contextPathPrefix") ;

    public static final Option FORCE_SCHEMA_UPDATE = new Option( "forceSchemaUpdate",
            "USE THIS OPTION WITH CARE!"
            + " Outdated cdm database schema versions will"
            + " be forcably updated to the version of the cdm-server. This option is only intended"
            + " to be used for development purposes.");


    public static Options getOptions(){
        if(options == null){
            options = new Options();
            options.addOption(HELP);
            options.addOption(WEBAPP);
            options.addOption(WEBAPP_CLASSPATH);
            options.addOption(HTTP_PORT);
            options.addOption(LOG_DIR);
            options.addOption(DATASOURCES_FILE);
            options.addOption(JMX);
            options.addOption(WIN32SERVICE);
            options.addOption(CONTEXT_PATH_PREFIX);
            options.addOption(FORCE_SCHEMA_UPDATE);
        }
        return options;
    }


}
