/**
* Copyright (C) 2012 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.server;

/**
 * @author a.kohlbecker
 * @date May 10, 2013
 *
 */
public class AssumedMemoryRequirements {

    // memory requirements
	public static final int KB = 1024;
	public static final long MB = 1024 * KB;
    public static final long PERM_GEN_SPACE_PER_INSTANCE = 55 * MB;
    public static final long HEAP_PER_INSTANCE = 130 * MB;
    public static final long PERM_GEN_SPACE_CDMSERVER = 19 * MB;
    public static final long HEAP_CDMSERVER = 15 * MB;

}
