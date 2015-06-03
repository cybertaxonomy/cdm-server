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

import static eu.etaxonomy.cdm.server.AssumedMemoryRequirements.KB;
import static eu.etaxonomy.cdm.server.CommandOptions.CONTEXT_PATH_PREFIX;
import static eu.etaxonomy.cdm.server.CommandOptions.DATASOURCES_FILE;
import static eu.etaxonomy.cdm.server.CommandOptions.HELP;
import static eu.etaxonomy.cdm.server.CommandOptions.HTTP_PORT;
import static eu.etaxonomy.cdm.server.CommandOptions.JMX;
import static eu.etaxonomy.cdm.server.CommandOptions.LOG_DIR;
import static eu.etaxonomy.cdm.server.CommandOptions.WEBAPP;
import static eu.etaxonomy.cdm.server.CommandOptions.WEBAPP_CLASSPATH;
import static eu.etaxonomy.cdm.server.CommandOptions.WIN32SERVICE;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.eclipse.jetty.apache.jsp.JettyJasperInitializer;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.plus.annotation.ContainerInitializer;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.webapp.WebAppClassLoader;
import org.eclipse.jetty.webapp.WebAppContext;

import eu.etaxonomy.cdm.server.instance.CdmInstance;
import eu.etaxonomy.cdm.server.instance.Configuration;
import eu.etaxonomy.cdm.server.instance.InstanceManager;
import eu.etaxonomy.cdm.server.instance.SharedAttributes;
import eu.etaxonomy.cdm.server.instance.Status;
import eu.etaxonomy.cdm.server.win32service.Win32Service;


/**
 * A bootstrap class for starting Jetty Runner using an embedded war.
 *
 * @version $Revision$
 */
public final class Bootloader {
    /**
     *
     */
    private static final String VERSION_PROPERTIES_FILE = "version.properties";

    //private static final String DEFAULT_WARFILE = "target/";



    private static final Logger logger = Logger.getLogger(Bootloader.class);

    private static final String DATASOURCE_BEANDEF_FILE = "datasources.xml";
    private static final String REALM_PROPERTIES_FILE = "cdm-server-realm.properties";

    private static final String USERHOME_CDM_LIBRARY_PATH = System.getProperty("user.home")+File.separator+".cdmLibrary"+File.separator;
    private static final String TMP_PATH = USERHOME_CDM_LIBRARY_PATH + "server" + File.separator;
    private static final String LOG_PATH = USERHOME_CDM_LIBRARY_PATH + "log" + File.separator;

    private static final String APPLICATION_NAME = "CDM Server";
    private static final String WAR_POSTFIX = ".war";

    private static final String CDM_WEBAPP = "cdm-webapp";
    private static final String CDM_WEBAPP_VERSION = "cdm-webapp.version";

    private static final String DEFAULT_WEBAPP_WAR_NAME = "default-webapp";
    private static final File DEFAULT_WEBAPP_TEMP_FOLDER = new File(TMP_PATH + DEFAULT_WEBAPP_WAR_NAME);
    private static final File CDM_WEBAPP_TEMP_FOLDER = new File(TMP_PATH + CDM_WEBAPP);

    private final InstanceManager instanceManager = new InstanceManager(new File(USERHOME_CDM_LIBRARY_PATH, DATASOURCE_BEANDEF_FILE));

    public List<CdmInstance> getCdmInstances() {
        return instanceManager.getInstances();
    }

    public InstanceManager getInstanceManager(){
        return instanceManager;
    }

    private File cdmRemoteWebAppFile = null;
    private File defaultWebAppFile = null;

    private String logPath = null;

    private String webAppClassPath = null;

    /**
     * The contextPathPrefix is expected to be normalized:
     * it ends with a slash and starts not with a slash character
     */
    private String contextPathPrefix = "";

    private Server server = null;
    private final ContextHandlerCollection contexts = new ContextHandlerCollection();

    private CommandLine cmdLine;

    private boolean isRunningFromSource;

    private boolean isRunningfromTargetFolder;

    private boolean isRunningFromWarFile;


    /* thread save singleton implementation */

    private static Bootloader bootloader = new Bootloader();

    private Bootloader() {}

    /**
     * @return the thread save singleton instance of the Bootloader
     */
    public synchronized static Bootloader getBootloader(){
        return bootloader;
    }

    /* end of singleton implementation */


    /**
     * @param input
     * @param output
     * @param bufferSize
     * @return
     * @throws IOException
     */
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
                logger.debug("looking for war file at " + warFile.getAbsolutePath());
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


        // WEBAPP options
        //   prepare web application files to run either from war files (production mode)
        //   or from source (development mode)
        if(cmdLine.hasOption(WEBAPP.getOpt())){

            cdmRemoteWebAppFile = new File(cmdLine.getOptionValue(WEBAPP.getOpt()));
            if(cdmRemoteWebAppFile.isDirectory()){
                logger.info("using user defined web application folder: " + cdmRemoteWebAppFile.getAbsolutePath());
            } else {
                logger.info("using user defined warfile: " + cdmRemoteWebAppFile.getAbsolutePath());
            }

            updateServerRunMode();

            // load the default-web-application from source if running in development mode mode
            if(isRunningFromWarFile){
                defaultWebAppFile = extractWar(DEFAULT_WEBAPP_WAR_NAME);
            } else {
                defaultWebAppFile = new File("./src/main/webapp");
            }

            if(isRunningFromSource){
                if(cmdLine.hasOption(WEBAPP_CLASSPATH.getOpt())){
                    String classPathOption = cmdLine.getOptionValue(WEBAPP_CLASSPATH.getOpt());
                    normalizeClasspath(classPathOption);
                }
            }
        } else {
            // read version number
            String version = readCdmRemoteVersion();
            cdmRemoteWebAppFile = extractWar(CDM_WEBAPP + "-" + version);
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
             File datasourcesFile = new File(cmdLine.getOptionValue(DATASOURCES_FILE.getOpt()));
             if(datasourcesFile.canRead()) {
                instanceManager.setDatasourcesFile(datasourcesFile);
            } else {
                logger.error("File set as " + DATASOURCES_FILE.getOpt()
                        + " (" + cmdLine.getOptionValue(DATASOURCES_FILE.getOpt())
                        + ") is not readable.");
            }
         }

        if(cmdLine.hasOption(CONTEXT_PATH_PREFIX.getOpt())){

            String cppo  = cmdLine.getOptionValue(CONTEXT_PATH_PREFIX.getOpt());
            if(cppo.equals("/")) {
                this.contextPathPrefix = "";
            } else {
                Pattern pattern = Pattern.compile("^/*(.*?)/*$");
                String replacement = "$1";
                this.contextPathPrefix = pattern.matcher(cppo).replaceAll(replacement) + "/";
            }
        }

        verifySystemResources();

         // load the configured instances for the first time
        instanceManager.reLoadInstanceConfigurations();
        server = new Server(httpPort);
        server.addLifeCycleListener(instanceManager);

        org.eclipse.jetty.webapp.Configuration.ClassList classlist = org.eclipse.jetty.webapp.Configuration.ClassList.setServerDefault(server);
        classlist.addAfter("org.eclipse.jetty.webapp.FragmentConfiguration", "org.eclipse.jetty.plus.webapp.EnvConfiguration", "org.eclipse.jetty.plus.webapp.PlusConfiguration");
        classlist.addBefore("org.eclipse.jetty.webapp.JettyWebXmlConfiguration", "org.eclipse.jetty.annotations.AnnotationConfiguration");


        // JMX support
        if(cmdLine.hasOption(JMX.getOpt())){
            logger.info("adding JMX support ...");
            MBeanContainer mBeanContainer = new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
            server.addEventListener(mBeanContainer);
            server.addBean(Log.getLog());
        }

        if(cmdLine.hasOption(WIN32SERVICE.getOpt())){
            Win32Service win32Service = new Win32Service();
            win32Service.setServer(server);
            server.setStopAtShutdown(true);
            server.addBean(win32Service);
        }

        // add default servlet context
        logger.info("preparing default WebAppContext");
        WebAppContext defaultWebappContext = new WebAppContext();

        setWebApp(defaultWebappContext, defaultWebAppFile);

        // JSP
        //
        // configuring jsp according to http://eclipse.org/jetty/documentation/current/configuring-jsp.html
        // from example http://eclipse.org/jetty/documentation/current/embedded-examples.html#embedded-webapp-jsp
        // Set the ContainerIncludeJarPattern so that jetty examines these
        // container-path jars for tlds, web-fragments etc.
        // If you omit the jar that contains the jstl .tlds, the jsp engine will
        // scan for them instead.
        defaultWebappContext.setAttribute(
                "org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern",
                ".*/[^/]*servlet-api-[^/]*\\.jar$|.*/javax.servlet.jsp.jstl-.*\\.jar$|.*/[^/]*taglibs.*\\.jar$" );

        defaultWebappContext.setAttribute("org.eclipse.jetty.containerInitializers", jspInitializers());

        // Context path
        //
        defaultWebappContext.setContextPath("/" + (contextPathPrefix.isEmpty() ? "" : contextPathPrefix.substring(0, contextPathPrefix.length() - 1)));
        logger.info("defaultWebapp (manager) context path:" + defaultWebappContext.getContextPath());
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

    /**
     * @param classpath
     */
    private void normalizeClasspath(String classpath) {
        StringBuilder classPathBuilder = new StringBuilder((int) (classpath.length() * 1.2));
        String[] cpEntries = classpath.split("[\\:]");
        for(String cpEntry : cpEntries){
            classPathBuilder.append(',');
//            if(cpEntry.endsWith(".jar")){
//                classPathBuilder.append("jar:");
//            }
            classPathBuilder.append(cpEntry);
        }
        webAppClassPath = classPathBuilder.toString();
    }

    public String readCdmRemoteVersion() throws IOException {
        String version = "cdmlib version unreadable";
        InputStream versionInStream = Bootloader.class.getClassLoader().getResourceAsStream(VERSION_PROPERTIES_FILE);
        if (versionInStream != null){
            Properties versionProperties = new Properties();
            versionProperties.load(versionInStream);
            version = versionProperties.getProperty(CDM_WEBAPP_VERSION, version);
        }
        return version;
    }

    /**
    * Ensure the jsp engine is initialized correctly
    */
    private List<ContainerInitializer> jspInitializers()
    {
        JettyJasperInitializer sci = new JettyJasperInitializer();
        ContainerInitializer initializer = new ContainerInitializer(sci, null);
        List<ContainerInitializer> initializers = new ArrayList<ContainerInitializer>();
        initializers.add(initializer);
        return initializers;
    }



    private void verifySystemResources() {

        OsChecker osChecker = new OsChecker();
        if(osChecker.isLinux()){
            try {
                Process p = Runtime.getRuntime().exec(new String[] { "bash", "-c", "ulimit -n" });
                BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line;
                StringBuilder response = new StringBuilder();
                     while ((line = in.readLine()) != null) {
                         response.append(line);
                     }
               logger.info("OS Limit (Linux): maximum number of open files: " + response);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            logger.info("verifySystemResources only implemented for linux");
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


    /**
     * Adds a new WebAppContext to the contexts of the running jetty instance.
     * <ol>
     * <li>Initialize WebAppContext:
     * <ol>
     * <li>set context path</li>
     * <li>set tmp directory</li>
     * <li>bind JndiDataSource</li>
     * <li>set web app context attributes</li>
     * <li>create and setup individual classloader for the instance</li>
     * </ol>
     * </li>
     * <li>
     * finally add the new webappcontext to the contexts of the jetty instance</li>
     * </ol>
     *
     * @param instance
     * @return the instance given as parameter of null in case the instance has
     *         {@link Status.#disabled} or if it is already added.
     * @throws IOException
     */
    public WebAppContext addCdmInstanceContext(CdmInstance instance) throws IOException {

        Configuration conf = instance.getConfiguration();
        if(!instance.isEnabled()){
            logger.info(conf.getInstanceName() + " is disabled, possibly due to JVM memory limitations");
            return null;
        }
        if(getContextFor(conf) != null){
            logger.info(conf.getInstanceName() + " is alreaddy added to the contexts - skipping");
            return null;
        }

        instance.setStatus(Status.initializing);
        logger.info("preparing WebAppContext for '"+ conf.getInstanceName() + "'");
        WebAppContext cdmWebappContext = new WebAppContext();

        cdmWebappContext.setContextPath(constructContextPath(conf));
        logger.info("contextPath: " + cdmWebappContext.getContextPath());
        cdmWebappContext.setTempDirectory(CDM_WEBAPP_TEMP_FOLDER);

//        if(!instance.bindJndiDataSource()){
//            // a problem with the datasource occurred skip this webapp
//            cdmWebappContext = null;
//            logger.error("a problem with the datasource occurred -> skipping /" + conf.getInstanceName());
//            instance.setStatus(Status.error);
//            return cdmWebappContext;
//        }

        cdmWebappContext.setAttribute(SharedAttributes.ATTRIBUTE_DATASOURCE_NAME, conf.getInstanceName());
        cdmWebappContext.setAttribute(SharedAttributes.ATTRIBUTE_JDBC_JNDI_NAME, conf.getJdbcJndiName());
        setWebApp(cdmWebappContext, getCdmRemoteWebAppFile());

        cdmWebappContext.setAttribute(SharedAttributes.ATTRIBUTE_CDM_LOGFILE,
                logPath + File.separator + "cdm-"
                        + conf.getInstanceName() + ".log");

        if( isRunningFromSource ){

            /*
             * when running the webapp from {projectpath} src/main/webapp we
             * must assure that each web application is using it's own
             * classloader thus we tell the WebAppClassLoader where the
             * dependencies of the webapplication can be found. Otherwise
             * the system classloader would load these resources.
             */
            WebAppClassLoader classLoader = new WebAppClassLoader(cdmWebappContext);
            if(webAppClassPath != null){
                logger.info("Running cdm-webapp from source folder: Adding class path supplied by option '-" +  WEBAPP_CLASSPATH.getOpt() +" =" + webAppClassPath +"'  to WebAppClassLoader");
                classLoader.addClassPath(webAppClassPath);
            } else {
                throw new RuntimeException("Classpath cdm-webapp for missing while running cdm-webapp from source folder. Please supplied cdm-server option '-" +  WEBAPP_CLASSPATH.getOpt() +"");
            }
            cdmWebappContext.setClassLoader(classLoader);
        }

        contexts.addHandler(cdmWebappContext);
        instance.setWebAppContext(cdmWebappContext);
        cdmWebappContext.addLifeCycleListener(instance);
        instance.setStatus(Status.stopped);

        return cdmWebappContext;
    }

    /**
     * @param conf
     * @return
     */
    public String constructContextPath(Configuration conf) {

        return "/" + contextPathPrefix + conf.getInstanceName();
    }

    /**
     * Removes the given instance from the contexts. If the instance is running
     * at the time of calling this method it will be stopped first.
     * The JndiDataSource and the webapplicationContext will be released and removed.
     *
     * @param instance the instance to be removed
     *
     * @throws Exception in case stopping the instance fails
     */
    public void removeCdmInstanceContext(CdmInstance instance) throws Exception {

        if(instance.getWebAppContext() != null){
            if(instance.getWebAppContext().isRunning()){
                try {
                    instance.getWebAppContext().stop();
                } catch (Exception e) {
                    instance.getProblems().add("Error while stopping instance: " + e.getMessage());
                    throw e;
                }
            }
            contexts.removeHandler(instance.getWebAppContext());
            instance.releaseWebAppContext();
        } else  {
            // maybe something went wrong before, try to find the potentially lost
            // contexts directly in the server
            ContextHandler handler = getContextFor(instance.getConfiguration());
            if(handler != null){
                contexts.removeHandler(handler);
            }
        }
        instance.unbindJndiDataSource();
    }

    /**
     * Sets the webapp specified by the <code>webApplicationResource</code> to
     * the given <code>context</code>.
     *
     * @param context
     * @param webApplicationResource the resource can either be a directory containing
     * a Java web application or *.war file.
     *
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

    private void updateServerRunMode() {
        String webappPathNormalized = cdmRemoteWebAppFile.getAbsolutePath().replace('\\', '/');
        isRunningFromSource =  webappPathNormalized.endsWith("src/main/webapp");
        isRunningfromTargetFolder = webappPathNormalized.endsWith("cdm-webapp/target/cdmserver");
        isRunningFromWarFile = !(isRunningFromSource || isRunningfromTargetFolder);
    }


    public Server getServer() {
        return server;
    }

    public ContextHandler getContextFor(Configuration conf) {
        return getContextFor(constructContextPath(conf));
    }

    public ContextHandler getContextFor(String contextPath) {
        for( Handler handler : contexts.getHandlers()){
            if(handler instanceof ContextHandler){
                if(((ContextHandler)handler).getContextPath().equals(contextPath)){
                    return (ContextHandler)handler;
                }
            }
        }
        return null;
    }

    public ContextHandlerCollection getContexts() {
        return contexts;
    }

    /**
     * @return a File object pointing to the location of the cdm-webapp
     */
    public File getCdmRemoteWebAppFile(){
        if(cdmRemoteWebAppFile == null){
            throw new RuntimeException("Invalid order of action. Server not yet started. The startServer() method must be called first. ");
        }
        return cdmRemoteWebAppFile;
    }
}