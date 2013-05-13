// $Id$
/**
* Copyright (C) 2009 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.server.instance;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


/**
 * @author a.kohlbecker
 * @date 30.03.2010
 *
 */
public class DataSourcePropertyParser {

    public static final Logger logger = Logger.getLogger(DataSourcePropertyParser.class);

    public static List<Configuration> parseDataSourceConfigs(File datasourcesFile){

        logger.info("loading bean definition file: " + datasourcesFile.getAbsolutePath());
        List<Configuration> configList = new ArrayList<Configuration>();
        Set<String> idSet = new HashSet<String>();
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(datasourcesFile);
            NodeList beanNodes  = doc.getElementsByTagName("bean");
            for(int i=0; i < beanNodes.getLength(); i++){
                Configuration conf = new Configuration();
                Node beanNode = beanNodes.item(i);
                // ATTRIBUTE_DATASOURCE_NAME
                NamedNodeMap namedNodeMap = beanNode.getAttributes();
                String beanId = namedNodeMap.getNamedItem("id").getNodeValue();


                conf.setInstanceName(beanId);
                // ATTRIBUTE_DATASOURCE_DRIVERCLASS
                String driverClass = getXMLNodeProperty(beanNode, "driverClass");

                if(driverClass == null || driverClass.isEmpty()){
                    // not a data source bean
                    continue;
                }

                conf.setDriverClass(driverClass);
                conf.setUsername(getXMLNodeProperty(beanNode, "username"));
                if(conf.getUsername() == null){
                    conf.setUsername(getXMLNodeProperty(beanNode, "user"));
                }
                conf.setPassword(getXMLNodeProperty(beanNode, "password"));

                conf.setDataSourceUrl(getXMLNodeProperty(beanNode, "url"));
                if(conf.getDataSourceUrl() == null){
                    conf.setDataSourceUrl(getXMLNodeProperty(beanNode, "jdbcUrl"));
                }

                if(idSet.add(conf.getInstanceName())) {
                    logger.debug("adding instanceName '"+ conf.getInstanceName() + "'");
                    configList.add(conf);
                } else {
                    logger.error("instance with name '"+ conf.getInstanceName() + "' alreaddy exists");
                }

            }

        } catch (SAXException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return configList;

    }

    private static String getXMLNodeProperty(Node beanNode, String name){
        NodeList children = beanNode.getChildNodes();
        for(int i=0; i < children.getLength(); i++){
            Node p = children.item(i);
            if(p.getNodeName().equals("property")
                    && p.getAttributes().getNamedItem("name").getNodeValue().equals(name)){
                return p.getAttributes().getNamedItem("value").getNodeValue();
            }
        }
        return null;
    }



}
