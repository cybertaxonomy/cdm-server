package eu.etaxonomy.cdm.server.jsvc;

import eu.etaxonomy.cdm.server.Bootloader;

public class ServiceWrapper {


    /**
     * Method required by jsvc. jsvc is being used by the linux start up script
     * @param args
     * @throws Exception
     */
    public void init(String[] args) throws Exception {
        Bootloader.getBootloader().parseCommandOptions(args);
    }

   /**
    * Method required by jsvc. jsvc is being used by the linux start up script
    * @param args
    * @throws Exception
    */
   public void start() throws Exception {
       Bootloader.getBootloader().startServer();
    }

   /**
    * Method required by jsvc. jsvc is being used by the linux start up script
    * @param args
    * @throws Exception
    */
   public void stop() throws Exception {
       Bootloader.getBootloader().getServer().stop();
    }

   /**
    * Method required by jsvc. jsvc is being used by the linux start up script
    * @param args
    * @throws Exception
    */
   public void destroy() throws Exception {
       Bootloader.getBootloader().getServer().destroy();
    }


}
