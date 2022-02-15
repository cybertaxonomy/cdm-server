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
 */
public class AssumedMemoryRequirements {

    // these values are best obtaines by using the following command sequence:
    // export JPID=$(jps | grep Jetty | awk '{ print $1}'); jcmd $JPID GC.run; sleep 1;  jstat -gc $JPID | sed -e 's/ \+/\t/g'

    /*
     * From Java 8 on PermGen is replaced by the Metaspace
     *
     * Metaspace capacity:
     *
     * By default class metadata allocation is
     * limited by the amount of available native memory (capacity will
     * of course depend if you use a 32-bit JVM vs. 64-bit along with OS
     * virtual memory availability). A new flag is available
     * (MaxMetaspaceSize), allowing you to limit the amount of native
     * memory used for class metadata. If you don’t specify this flag,
     * the Metaspace will dynamically re-size depending of the
     * application demand at runtime.
     */

    public static final int KB = 1024;
    public static final long MB = 1024 * KB;

    /**
     * 25% on to on the idle footprint
     */
    private static final double FREE_HEAP = 1.25;

    /*
     * based on the HU value of the  jstat -gc output
     */
    public static final long HEAP_PER_INSTANCE = (long)(153 * MB * FREE_HEAP);
    public static final long HEAP_CDMSERVER = (long) (15 * MB * FREE_HEAP);

}
