/**
 * Copyright (C) 2009 EDIT
 * European Distributed Institute of Taxonomy
 * http://www.e-taxonomy.eu
 *
 * The contents of this file are subject to the Mozilla Public License Version 1.1
 * See LICENSE.TXT at the top of this package for the full license terms.
 */
package eu.etaxonomy.cdm.server;

/**
 * @author andreas
 * @date Jul 17, 2012
 *
 */
public class OsChecker {

    public boolean isMac() {
        try {
            Class.forName("com.apple.eawt.Application");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // can be defeated by creating a cmd.exe in PATH
    public boolean isWin() {
        try {
            Runtime.getRuntime().exec(new String[] { "cmd.exe", "/C", "dir" }).waitFor();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isLinux() {
        if (isMac())
            return false;
        try {
            Runtime.getRuntime().exec(new String[] { "sh", "-c", "ls" }).waitFor();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
