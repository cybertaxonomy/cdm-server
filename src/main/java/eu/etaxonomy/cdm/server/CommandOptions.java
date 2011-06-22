// $Id$
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
                    "If this option is used extraction of the war from the cdmserver jar file is omitted.\n \n" +
                    "Using the following paths developers can run the cdmlib-remote-webapp instaces completely from the target folder or from source:\n" +
                    " - '{cdmlib-project-root}/cdmlib-remote-webapp/target/cdmserver'\n " +
                    " - '{cdmlib-project-root}/cdmlib-remote-webapp/src/main/webapp'\n" +
                    "This will also affect the cdm-server project, if any of both paths is set cdm-server will be run using the cdm-server/src/main/webapp folder!" )
            .create("webapp");

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




    public static Options getOptions(){
        if(options == null){
            options = new Options();
            options.addOption(HELP);
            options.addOption(WEBAPP);
            options.addOption(HTTP_PORT);
            options.addOption(LOG_DIR);
            options.addOption(DATASOURCES_FILE);
            options.addOption(JMX);
            options.addOption(WIN32SERVICE);
        }
        return options;
    }


}
