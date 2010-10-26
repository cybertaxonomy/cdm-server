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

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.component.AbstractLifeCycle;


/**
 * @author a.kohlbecker
 * @date 26.10.2010
 *
 */
public class Win32Service extends AbstractLifeCycle implements Runnable
{
    private Server server;
    public void doStart()
    {
        
        
        CDMServerWrapperListener.setServer(server);
         
    }
    
    public void doStop()
    {
        System.out.println("Listener is stopping CDM Server Instance!!!");
        
    }
    
    public void run()
    {
        doStop();
        
    }

    public void stopServer()
    {
        try
        {
            System.out.println("Thread Test Stopper!!!");
            server.stop();
            //WrapperManager.stop(0);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    
    
    public Server getServer()
    {
        return server;
    }

    public void setServer(Server server)
    {
        this.server = server;
    }
    
   

   
    
    
    
}