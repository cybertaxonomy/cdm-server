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

import static eu.etaxonomy.cdm.server.CommandOptions.DATASOURCES_FILE;
import static eu.etaxonomy.cdm.server.CommandOptions.HELP;
import static eu.etaxonomy.cdm.server.CommandOptions.HTTP_PORT;
import static eu.etaxonomy.cdm.server.CommandOptions.JMX;
import static eu.etaxonomy.cdm.server.CommandOptions.WEBAPP;
import static eu.etaxonomy.cdm.server.CommandOptions.WIN32SERVICE;
import static eu.etaxonomy.cdm.server.CommandOptions.LOG_DIR;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ContextHandler.Context;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.component.LifeCycle.Listener;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.webapp.WebAppClassLoader;
import org.eclipse.jetty.webapp.WebAppContext;

import eu.etaxonomy.cdm.server.CdmInstanceProperties.Status;
import eu.etaxonomy.cdm.server.win32service.Win32Service;


/**
 * A bootstrap class for starting Jetty Runner using an embedded war.
 *
 * Recommended start options for the java virtual machine:
 * <pre>
 * -Xmx1024M
 *
 * -XX:PermSize=128m
 * -XX:MaxPermSize=192m
 *
 * -XX:+UseConcMarkSweepGC
 * -XX:+CMSClassUnloadingEnabled
 * -XX:+CMSPermGenSweepingEnabled
 * </pre>
 *
 * @version $Revision$
 */
public final class Bootloader {
    /**
     *
     */
    private static final String VERSION_PROPERTIES_FILE = "version.properties";

    //private static final String DEFAULT_WARFILE = "target/";



    /**
     * @author a.kohlbecker
     * @date 03.02.2011
     *
     */
    private class WebAppContextListener implements Listener {

        WebAppContext cdmWebappContext;
        /**
         * @param cdmWebappContext
         */
        public WebAppContextListener(WebAppContext cdmWebappContext) {
            this.cdmWebappContext = cdmWebappContext;
        }

        @Override
        public void lifeCycleStopping(LifeCycle event) {
            logger.info("lifeCycleStopping");
        }

        @Override
        public void lifeCycleStopped(LifeCycle event) {
            logger.info("lifeCycleStopped");

        }

        @Override
        public void lifeCycleStarting(LifeCycle event) {
            logger.info("lifeCycleStarting");
        }

        @SuppressWarnings("unchecked")
        @Override
        public void lifeCycleStarted(LifeCycle event) {
            logger.info("lifeCycleStarted");

            List<String> messages = getServletContextAttribute(cdmWebappContext, ATTRIBUTE_ERROR_MESSAGES, List.class);
            String dataSourceName = getServletContextAttribute(cdmWebappContext, ATTRIBUTE_DATASOURCE_NAME, String.class);

            if(messages != null && dataSourceName != null){
                CdmInstanceProperties configAndStatus = findConfigAndStatusFor(dataSourceName);
                configAndStatus.getProblems().addAll(messages);
                configAndStatus.setStatus(Status.error);
                try {
                    logger.warn("Stopping context '" + dataSourceName + "' due to errors reported in ServletContext");
                    cdmWebappContext.stop();
                } catch (Exception e) {
                    logger.error(e);
                }
            }
        }


        @Override
        public void lifeCycleFailure(LifeCycle event, Throwable cause) {
            logger.error("lifeCycleFailure");
        }
    }

    private static final Logger logger = Logger.getLogger(Bootloader.class);

    private static final String DATASOURCE_BEANDEF_FILE = "datasources.xml";
    private static final String REALM_PROPERTIES_FILE = "cdm-server-realm.properties";

    private static final String USERHOME_CDM_LIBRARY_PATH = System.getProperty("user.home")+File.separator+".cdmLibrary"+File.separator;
    private static final String TMP_PATH = USERHOME_CDM_LIBRARY_PATH + "server" + File.separator;
    private static final String LOG_PATH = USERHOME_CDM_LIBRARY_PATH + "log" + File.separator;

    private static final String APPLICATION_NAME = "CDM Server";
    private static final String WAR_POSTFIX = ".war";

    private static final String CDMLIB_REMOTE_WEBAPP = "cdmlib-remote-webapp";
    private static final String CDMLIB_REMOTE_WEBAPP_VERSION = "cdmlib-remote-webapp.version";

    private static final String DEFAULT_WEBAPP_WAR_NAME = "default-webapp";
    private static final File DEFAULT_WEBAPP_TEMP_FOLDER = new File(TMP_PATH + DEFAULT_WEBAPP_WAR_NAME);
    private static final File CDM_WEBAPP_TEMP_FOLDER = new File(TMP_PATH + CDMLIB_REMOTE_WEBAPP);

    private static final String ATTRIBUTE_JDBC_JNDI_NAME = "cdm.jdbcJndiName";
    private static final String ATTRIBUTE_DATASOURCE_NAME = "cdm.datasource";
    private static final String ATTRIBUTE_CDM_LOGFILE = "cdm.logfile";
    /**
     * same as in eu.etaxonomy.cdm.remote.config.DataSourceConfigurer
     */
    private static final String ATTRIBUTE_ERROR_MESSAGES = "cdm.errorMessages";


    // memory requirements
    private static final long MB = 1024 * 1024;
    private static final long PERM_GEN_SPACE_PER_INSTANCE = 64 * MB;
    private static final long HEAP_PER_INSTANCE = 150 * MB;

    private static final int KB = 1024;


    private Set<CdmInstanceProperties> configAndStatusSet = null;

    public Set<CdmInstanceProperties> getConfigAndStatus() {
        return configAndStatusSet;
    }

    private File cdmRemoteWebAppFile = null;
    private File defaultWebAppFile = null;

    private String logPath = null;

    private Server server = null;
    private ContextHandlerCollection contexts = new ContextHandlerCollection();

    private CommandLine cmdLine;

    /* thread save singleton implementation */

    private static Bootloader instance = new Bootloader();

    private Bootloader() {}

    /**
     * @return the thread save singleton instance of the Bootloader
     */
    public synchronized static Bootloader getBootloader(){
        return instance;
    }

    /* end of singleton implementation */

    private Set<CdmInstanceProperties> loadDataSources(){
        if(configAndStatusSet == null){
            File datasourcesFile = new File(USERHOME_CDM_LIBRARY_PATH, DATASOURCE_BEANDEF_FILE);
            configAndStatusSet = DataSourcePropertyParser.parseDataSourceConfigs(datasourcesFile);
            logger.info("cdm server instance names loaded: "+ configAndStatusSet.toString());
        }
        return configAndStatusSet;
    }

    public int writeStreamTo(final InputStream input, final OutputStream output, int bufferSize) throws IOException {
        int available = Math.min(input.available(), 256 * KB);
        byte[] buffer = new byte[Math.max(bufferSize, available)];
        int answer = 0;
        int count = input.read(buffer);
        while (count >= 0) {
            output.write(buffer, 0, count);
            answer += count;
            count = input.read(buffer);
        }
        return answer;
    }

    private boolean bindJndiDataSource(CdmInstanceProperties conf) {
        try {
            Class<DataSource> dsCass = (Class<DataSource>) Thread.currentThread().getContextClassLoader().loadClass("com.mchange.v2.c3p0.ComboPooledDataSource");
            DataSource datasource = dsCass.newInstance();
            dsCass.getMethod("setDriverClass", new Class[] {String.class}).invoke(datasource, new Object[] {conf.getDriverClass()});
            dsCass.getMethod("setJdbcUrl", new Class[] {String.class}).invoke(datasource, new Object[] {conf.getUrl()});
            dsCass.getMethod("setUser", new Class[] {String.class}).invoke(datasource, new Object[] {conf.getUsername()});
            dsCass.getMethod("setPassword", new Class[] {String.class}).invoke(datasource, new Object[] {conf.getPassword()});

            Connection connection = null;
            String sqlerror = null;
            try {
                connection = datasource.getConnection();
                connection.close();
            } catch (SQLException e) {
                sqlerror = e.getMessage() + "["+ e.getSQLState() + "]";
                conf.getProblems().add(sqlerror);
                if(connection !=  null){
                    try {connection.close();} catch (SQLException e1) { /* IGNORE */ }
                }
                logger.error(conf.toString() + " has problem : "+ sqlerror );
            }

            if(!conf.hasProblems()){
                logger.info("binding jndi datasource at " + conf.getJdbcJndiName() + " with "+conf.getUsername() +"@"+ conf.getUrl());
                org.eclipse.jetty.plus.jndi.Resource jdbcResource = new org.eclipse.jetty.plus.jndi.Resource(conf.getJdbcJndiName(), datasource);
                return true;
            }

        } catch (IllegalArgumentException e) {
            logger.error(e);
            e.printStackTrace();
        } catch (SecurityException e) {
            logger.error(e);
        } catch (ClassNotFoundException e) {
            logger.error(e);
        } catch (InstantiationException e) {
            logger.error(e);
        } catch (IllegalAccessException e) {
            logger.error(e);
        } catch (InvocationTargetException e) {
            logger.error(e);
        } catch (NoSuchMethodException e) {
            logger.error(e);
        } catch (NamingException e) {
            logger.error(e);
        }
        return false;
    }

    public void parseCommandOptions(String[] args) throws ParseException {
        CommandLineParser parser = new GnuParser();
        cmdLine = parser.parse( CommandOptions.getOptions(), args );

         // print the help message
         if(cmdLine.hasOption(HELP.getOpt())){
             HelpFormatter formatter = new HelpFormatter();
             formatter.setWidth(200);
             formatter.printHelp( "java .. ", CommandOptions.getOptions() );
             System.exit(0);
         }
    }


    private File extractWar(String warName) throws IOException, FileNotFoundException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String warFileName = warName + WAR_POSTFIX;

        // 1. find in classpath
        URL resource = classLoader.getResource(warFileName);
        if (resource == null) {
            logger.error("Could not find the " + warFileName + " on classpath!");

            File pomxml = new File("pom.xml");
            if(pomxml.exists()){
                // 2. try finding in target folder of maven project
                File warFile = new File("target" + File.separator + warFileName);
                logger.debug("looging for war file at " + warFile.getAbsolutePath());
                if (warFile.canRead()) {
                    resource = warFile.toURI().toURL();
                } else {
                    logger.error("Also could not find the " + warFileName + " in maven project, try excuting 'mvn install'");
                }
            }
        }

        if (resource == null) {
            // no way finding the war file :-(
            System.exit(1);
        }


        File warFile = new File(TMP_PATH, warName + "-" + WAR_POSTFIX);
        logger.info("Extracting " + warFileName + " to " + warFile + " ...");

        writeStreamTo(resource.openStream(), new FileOutputStream(warFile), 8 * KB);

        logger.info("Extracted " + warFileName);
        return warFile;
    }


    /**
     * MAIN METHOD
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        Bootloader bootloader = Bootloader.getBootloader();

        bootloader.parseCommandOptions(args);

        bootloader.startServer();
    }



    public void startServer() throws IOException,
            FileNotFoundException, Exception, InterruptedException {


        if(cmdLine.hasOption(LOG_DIR.getOpt())){
            logPath = cmdLine.getOptionValue(LOG_DIR.getOpt());
        } else {
            logPath = LOG_PATH;
        }


        //assure LOG_PATH exists
        File logPathFile = new File(logPath);
        if(!logPathFile.exists()){
            FileUtils.forceMkdir(new File(logPath));
        }

        //append logger
        configureFileLogger();

        logger.info("Starting "+APPLICATION_NAME);
        logger.info("Using  " + System.getProperty("user.home") + " as home directory. Can be specified by -Duser.home=<FOLDER>");

        //assure TMP_PATH exists and clean it up
        File tempDir = new File(TMP_PATH);
        if(!tempDir.exists() && !tempDir.mkdirs()){
            logger.error("Error creating temporary directory for webapplications " + tempDir.getAbsolutePath());
            System.exit(-1);
        } else {
            if(FileUtils.deleteQuietly(tempDir)){
                tempDir.mkdirs();
                logger.info("Old webapplications successfully cleared");
            }
        }
        tempDir = null;


         // WARFILE
         if(cmdLine.hasOption(WEBAPP.getOpt())){
             cdmRemoteWebAppFile = new File(cmdLine.getOptionValue(WEBAPP.getOpt()));
             if(cdmRemoteWebAppFile.isDirectory()){
                 logger.info("using user defined web application folder: " + cdmRemoteWebAppFile.getAbsolutePath());
             } else {
                 logger.info("using user defined warfile: " + cdmRemoteWebAppFile.getAbsolutePath());
             }
             if(isRunningFromCdmRemoteWebAppSource()){
                 //TODO check if all local paths are valid !!!!
                defaultWebAppFile = new File("./src/main/webapp");

             } else {
                defaultWebAppFile = extractWar(DEFAULT_WEBAPP_WAR_NAME);
             }
         } else {
             // read version number
             String version = readCdmRemoteVersion();

             cdmRemoteWebAppFile = extractWar(CDMLIB_REMOTE_WEBAPP + "-" + version);
             defaultWebAppFile = extractWar(DEFAULT_WEBAPP_WAR_NAME);
         }

         // HTTP Port
         int httpPort = 8080;
         if(cmdLine.hasOption(HTTP_PORT.getOpt())){
             try {
                httpPort = Integer.parseInt(cmdLine.getOptionValue(HTTP_PORT.getOpt()));
                logger.info(HTTP_PORT.getOpt()+" set to "+cmdLine.getOptionValue(HTTP_PORT.getOpt()));
            } catch (NumberFormatException e) {
                logger.error("Supplied portnumber is not an integer");
                System.exit(-1);
            }
         }

         if(cmdLine.hasOption(DATASOURCES_FILE.getOpt())){
             logger.error(DATASOURCES_FILE.getOpt() + " NOT JET IMPLEMENTED!!!");
         }

        loadDataSources();

        verifyMemoryRequirements();


        server = new Server(httpPort);

        // JMX support
        if(cmdLine.hasOption(JMX.getOpt())){
            logger.info("adding JMX support ...");
            MBeanContainer mBeanContainer = new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
            server.getContainer().addEventListener(mBeanContainer);
            mBeanContainer.addBean(Log.getLog());
            mBeanContainer.start();
        }

        if(cmdLine.hasOption(WIN32SERVICE.getOpt())){
            Win32Service win32Service = new Win32Service();
            win32Service.setServer(server);
            server.setStopAtShutdown(true);
            server.addBean(win32Service);
        }

        // add servelet contexts


        //
        // 1. default context
        //
        logger.info("preparing default WebAppContext");
        WebAppContext defaultWebappContext = new WebAppContext();

        setWebApp(defaultWebappContext, defaultWebAppFile);
        defaultWebappContext.setContextPath("/");
        defaultWebappContext.setTempDirectory(DEFAULT_WEBAPP_TEMP_FOLDER);

        // configure security context
        // see for reference * http://docs.codehaus.org/display/JETTY/Realms
        //                   * http://wiki.eclipse.org/Jetty/Starting/Porting_to_Jetty_7
        HashLoginService loginService = new HashLoginService();
        loginService.setConfig(USERHOME_CDM_LIBRARY_PATH + REALM_PROPERTIES_FILE);
        defaultWebappContext.getSecurityHandler().setLoginService(loginService);

        // Important:
        // the defaultWebappContext MUST USE the super classloader
        // otherwise the status page (index.jsp) might not work
        defaultWebappContext.setClassLoader(this.getClass().getClassLoader());
        contexts.addHandler(defaultWebappContext);

        //
        // 2. cdm server contexts
        //
        server.addLifeCycleListener(new LifeCycle.Listener(){

            @Override
            public void lifeCycleFailure(LifeCycle event, Throwable cause) {
                logger.error("Jetty LifeCycleFailure", cause);
            }

            @Override
            public void lifeCycleStarted(LifeCycle event) {
                logger.info("cdmserver has started, now adding CDM server contexts");
                try {
                    addCdmServerContexts(true);
                } catch (IOException e1) {
                    logger.error(e1);
                }
            }

            @Override
            public void lifeCycleStarting(LifeCycle event) {
            }

            @Override
            public void lifeCycleStopped(LifeCycle event) {
            }

            @Override
            public void lifeCycleStopping(LifeCycle event) {
            }

            });


        logger.info("setting contexts ...");
        server.setHandler(contexts);
        logger.info("starting jetty ...");
//        try {

            server.start();

//        } catch(org.springframework.beans.BeansException e){
//        	Throwable rootCause = null;
//        	while(e.getCause() != null){
//        		rootCause = e.getCause();
//        	}
//        	if(rootCause != null && rootCause.getClass().getSimpleName().equals("InvalidCdmVersionException")){
//
//        		logger.error("rootCause ----------->" + rootCause.getMessage());
////        		for(CdmInstanceProperties props : configAndStatus){
////        			if(props.getDataSourceName())
////        		}
//        	}
//        }

        if(cmdLine.hasOption(WIN32SERVICE.getOpt())){
            logger.info("jetty has started as win32 service");
        } else {
            server.join();
            logger.info(APPLICATION_NAME+" stopped.");
            System.exit(0);
        }
    }

    public String readCdmRemoteVersion() throws IOException {
        InputStream versionInStream = Bootloader.class.getClassLoader().getResourceAsStream(VERSION_PROPERTIES_FILE);
         Properties versionProperties = new Properties();
         versionProperties.load(versionInStream);
         String version = versionProperties.getProperty(CDMLIB_REMOTE_WEBAPP_VERSION);
        return version;
    }

    /**
     *
     */
    private void verifyMemoryRequirements() {

        verifyMemoryRequirement("PermGenSpace", PERM_GEN_SPACE_PER_INSTANCE, JvmManager.getPermGenSpaceUsage().getMax());
        verifyMemoryRequirement("HeapSpace", HEAP_PER_INSTANCE, JvmManager.getHeapMemoryUsage().getMax());

    }

    private void verifyMemoryRequirement(String memoryName, long requiredSpacePerIntance, long availableSpace) {


        long requiredSpace = configAndStatusSet.size() * requiredSpacePerIntance;

        if(requiredSpace > availableSpace){

            String message = memoryName + " ("
                + (availableSpace / MB)
                + "MB) insufficient for "
                + configAndStatusSet.size()
                + " instances. Increase " + memoryName + " to "
                + (requiredSpace / MB)
                + "MB";
                ;
            logger.error(message + " => disabling some instances!!!");

            // disabling some instances
            int i=0;
            for(CdmInstanceProperties instanceProps : configAndStatusSet){
                i++;
                if(i * requiredSpacePerIntance > availableSpace){
                    instanceProps.setStatus(Status.disabled);
                    instanceProps.getProblems().add("Disbled due to: " + message);
                }
            }
        }
    }

    /**
     * Configures and adds a {@link RollingFileAppender} to the root logger
     *
     * The log files of the cdm-remote instances are configured by the
     * {@link eu.etaxonomy.cdm.remote.config.LoggingConfigurer}
     */
    private void configureFileLogger() {

        PatternLayout layout = new PatternLayout("%d %p [%c] - %m%n");
        try {
            String logFile = logPath + File.separator + "cdmserver.log";
            RollingFileAppender appender = new RollingFileAppender(layout, logFile);
            appender.setMaxBackupIndex(3);
            appender.setMaxFileSize("2MB");
            Logger.getRootLogger().addAppender(appender);
            logger.info("logging to :" + logFile);
        } catch (IOException e) {
            logger.error("Creating RollingFileAppender failed:", e);
        }
    }

    private void addCdmServerContexts(boolean austostart) throws IOException {

        for(CdmInstanceProperties conf : configAndStatusSet){

            if(!conf.isEnabled()){
                logger.info(conf.getDataSourceName() + " is disabled due to JVM memory limitations => skipping");
                continue;
            }
            conf.setStatus(CdmInstanceProperties.Status.initializing);
            logger.info("preparing WebAppContext for '"+ conf.getDataSourceName() + "'");
            WebAppContext cdmWebappContext = new WebAppContext();

            cdmWebappContext.setContextPath("/"+conf.getDataSourceName());
            cdmWebappContext.setTempDirectory(CDM_WEBAPP_TEMP_FOLDER);

            if(!bindJndiDataSource(conf)){
                // a problem with the datasource occurred skip this webapp
                cdmWebappContext = null;
                logger.error("a problem with the datasource occurred -> skipping /" + conf.getDataSourceName());
                conf.setStatus(CdmInstanceProperties.Status.error);
                continue;
            }

            cdmWebappContext.setAttribute(ATTRIBUTE_DATASOURCE_NAME, conf.getDataSourceName());
            cdmWebappContext.setAttribute(ATTRIBUTE_JDBC_JNDI_NAME, conf.getJdbcJndiName());
            setWebApp(cdmWebappContext, cdmRemoteWebAppFile);

            cdmWebappContext.setAttribute(ATTRIBUTE_CDM_LOGFILE,
                    logPath + File.separator + "cdm-"
                            + conf.getDataSourceName() + ".log");

            if(cdmRemoteWebAppFile.isDirectory() && isRunningFromCdmRemoteWebAppSource()){

                /*
                 * when running the webapp from {projectpath} src/main/webapp we
                 * must assure that each web application is using it's own
                 * classloader thus we tell the WebAppClassLoader where the
                 * dependencies of the webapplication can be found. Otherwise
                 * the system classloader would load these resources.
                 */
                logger.info("Running webapp from source folder, thus adding java.class.path to WebAppClassLoader");

                WebAppClassLoader classLoader = new WebAppClassLoader(cdmWebappContext);

                String classPath = System.getProperty("java.class.path");
                classLoader.addClassPath(classPath);
                cdmWebappContext.setClassLoader(classLoader);
            }

            cdmWebappContext.addLifeCycleListener(new WebAppContextListener(cdmWebappContext));
            contexts.addHandler(cdmWebappContext);

            if(austostart){
                try {
                    conf.setStatus(CdmInstanceProperties.Status.starting);
                    cdmWebappContext.start();
                    if(!conf.getStatus().equals(Status.error)){
                        conf.setStatus(CdmInstanceProperties.Status.started);
                    }
                } catch (Exception e) {
                    logger.error("Could not start " + cdmWebappContext.getContextPath());
                    conf.setStatus(CdmInstanceProperties.Status.error);
                }
            }

        }
    }

    /**
     * @param context
     * @param webApplicationResource
     */
    private void setWebApp(WebAppContext context, File webApplicationResource) {
        if(webApplicationResource.isDirectory()){
            context.setResourceBase(webApplicationResource.getAbsolutePath());
            logger.debug("setting directory " + webApplicationResource.getAbsolutePath() + " as webapplication");
        } else {
            context.setWar(webApplicationResource.getAbsolutePath());
            logger.debug("setting war file " + webApplicationResource.getAbsolutePath() + " as webapplication");
        }
    }

    /**
     * @return
     */
    private boolean isRunningFromCdmRemoteWebAppSource() {
        String webappPathNormalized = cdmRemoteWebAppFile.getAbsolutePath().replace('\\', '/');
        return webappPathNormalized.endsWith("src/main/webapp") || webappPathNormalized.endsWith("cdmlib-remote-webapp/target/cdmserver");
    }

    /**
     * @param dataSourceName
     * @return
     */
    private CdmInstanceProperties findConfigAndStatusFor(String dataSourceName){
        for(CdmInstanceProperties props : configAndStatusSet){
            if(props.getDataSourceName().equals(dataSourceName)){
                return props;
            }
        }
        return null;
    }

    /**
     * @param <T>
     * @param webAppContext
     * @param attributeName
     * @param type
     * @return
     */
    @SuppressWarnings("unchecked")
    private <T> T getServletContextAttribute(WebAppContext webAppContext, String attributeName, Class<T> type) {

        Context servletContext = webAppContext.getServletContext();
        Object value = servletContext.getAttribute(attributeName);
        if( value != null && type.isAssignableFrom(value.getClass())){

        }
        return (T) value;
    }

    public Server getServer() {
        return server;
    }
}
