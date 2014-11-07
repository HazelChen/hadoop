package org.apache.hadoop.hdfs.ec;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.hadoop.conf.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

public class ECSchemaLoader {
	public static final Log LOG = LogFactory
			.getLog(ECSchemaLoader.class.getName()); 
	
	private static final String CODER_CONF_PREFIX = "hadoop.hdfs.ec.erasurecodec.codec.";

	/**
	 * Path to XML file containing allocations. If the path is relative, it is
	 * searched for in the classpath, but loaded like a regular File.
	 */
	public File getConfigurationFile(Configuration conf) {
		String allocFilePath = conf.get(ECConfiguration.CONFIGURATION_FILE,
		        ECConfiguration.DEFAULT_CONFIGURATION_FILE);
		File allocFile = new File(allocFilePath);
		if (!allocFile.isAbsolute()) {
			URL url = Thread.currentThread().getContextClassLoader()
					.getResource(allocFilePath);
			if (url == null) {
				LOG.warn(allocFilePath + " not found on the classpath.");
				allocFile = null;
			} else if (!url.getProtocol().equalsIgnoreCase("file")) {
				throw new RuntimeException(
						"EC configuration file "
								+ url
								+ " found on the classpath is not on the local filesystem.");
			} else {
				allocFile = new File(url.getPath());
			}
		}
		return allocFile;
	}

	/**
	 * Updates the erasure codec list from the config file. This file is
	 * expected to be in the XML format specified in the design doc.
	 * @return codecs form configure file
	 * @throws IOException
	 *             if the config file cannot be read.
	 * @throws ECConfigurationException
	 *             if allocations are invalid.
	 * @throws ParserConfigurationException
	 *             if XML parser is misconfigured.
	 * @throws SAXException
	 *             if config file is malformed.
	 */
	public synchronized List<ErasureCodec> loadSchema(Configuration conf) throws IOException,
			ParserConfigurationException, SAXException,
			ECConfigurationException {
		File confFile = getConfigurationFile(conf);
		if (confFile == null) {
			return new ArrayList<ErasureCodec>();
		}
		LOG.info("Loading ec configuration file " + confFile);

		// Read and parse the allocations file.
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		docBuilderFactory.setIgnoringComments(true);
		DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
		Document doc = builder.parse(confFile);
		Element root = doc.getDocumentElement();
		if (!"ecschemas".equals(root.getTagName()))
			throw new ECConfigurationException(
					"Bad fair scheduler config "
							+ "file: top-level element not <allocations>");
		NodeList elements = root.getChildNodes();
		List<ErasureCodec> codecs = new ArrayList<ErasureCodec>();
		for (int i = 0; i < elements.getLength(); i++) {
			Node node = elements.item(i);
			if (node instanceof Element) {
				Element element = (Element) node;
				if ("ecschema".equals(element.getTagName())) {
					ErasureCodec codec = loadCodec(element);
					if (conf.get(CODER_CONF_PREFIX + codec.getErasureCoder()) != null) {
						codecs.add(codec);
					}
				} else {
					LOG.warn("Bad element in EC configuration file: " + element.getTagName());
				}
			}
		}
		return codecs;
	}
	
	/**
	   * Loads a erasure codec from a codec element in the configuration file
	   */
	  private ErasureCodec loadCodec(Element element) {
	    String codecName = element.getAttribute("name");
	    Map<String, String> ecProperties = new HashMap<String, String>();
	    String erasureCoder = null;
	    NodeList fields = element.getChildNodes();
//	    boolean isLeaf = true;

	    for (int j = 0; j < fields.getLength(); j++) {
	      Node fieldNode = fields.item(j);
	      if (!(fieldNode instanceof Element))
	        continue;
	      Element field = (Element) fieldNode;
	      String tagName = field.getTagName();
	      if ("erasurecodec".equals(tagName)) {
	    	  erasureCoder = ((Text)field.getFirstChild()).getData().trim();
	    	  ecProperties.put(tagName, erasureCoder);
	      }/* else if ("codec".endsWith(field.getTagName())) {
	    	  loadCodec(field);
	    	  isLeaf = false;
	      }*/ else {
	    	  String value = ((Text)field.getFirstChild()).getData().trim();
	    	  ecProperties.put(tagName, value);
	      }
	    }
	    
	    ErasureCodec codec = new ErasureCodec(codecName, ecProperties, erasureCoder);
	    return codec;
	  }
}
