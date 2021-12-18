/*
 * ========================================================================
 * Based on Jetty 6 code from Mort Bay Consulting Pty. Ltd.
 * Modified for CDM Server by Andreas Kohlbecker. 2010-10-24
 * ========================================================================
 * Copyright 2004-2008 Mort Bay Consulting Pty. Ltd.
 * ------------------------------------------------------------------------
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ========================================================================
 */
package eu.etaxonomy.cdm.server.win32service;


import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.tanukisoftware.wrapper.WrapperListener;
import org.tanukisoftware.wrapper.WrapperManager;
/**
 * @author a.kohlbecker
 * @date 26.10.2010
 *
 */
public class CDMServerWrapperListener implements WrapperListener {

    private static final Logger logger = Logger.getLogger(CDMServerWrapperListener.class);

    private static Server __server = null;

    public CDMServerWrapperListener(){
    }

    @Override
    public void controlEvent(int event) {
        if (!WrapperManager.isControlledByNativeWrapper()) {
            if ((event == WrapperManager.WRAPPER_CTRL_C_EVENT)
                    || (event == WrapperManager.WRAPPER_CTRL_CLOSE_EVENT)
                    || (event == WrapperManager.WRAPPER_CTRL_SHUTDOWN_EVENT)){
                WrapperManager.stop(0);
            }
        }
    }

    @Override
    public Integer start(String[] args) {
        for(int i=0; i<args.length; i++) {
        	logger.info("ARG[" + i + "] = " + args[i]);
        }
        try {
			eu.etaxonomy.cdm.server.Bootloader.main(args);
			logger.info("The CDM Server Bootloader has started.");
		} catch (Exception e) {
			logger.error(e);
		}
        return null;
    }

    @Override
    public int stop(int code) {
        try {
        	logger.info("Stopping CDM Server!!!");
            __server.stop();
            logger.info("CDM Server Stopped!!!");
            return code;
        }
        catch (Exception e) {
        	logger.error("Stop Server Error", e);
            return -1;
        }
    }

    public static void setServer(Server server) {
        __server = server;
    }

    public static Server getServer() {
        return __server;
    }

    public static void main(String[] args) {
        String newStrArgs[] = new String[args.length + 1];
        newStrArgs[0] = System.getProperty("jetty.home") + "etc/jetty-win32-service.xml";
        for(int i=0; i<args.length; i++) {
            newStrArgs[i+1] = args[i];
        }
        WrapperManager.start(new CDMServerWrapperListener(), newStrArgs);
    }
}