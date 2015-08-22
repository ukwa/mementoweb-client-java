/**
 * 
 */
package uk.bl.wa.memento.client;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import dev.memento.Memento;
import dev.memento.MementoClient;
import dev.memento.MementoList;

/**
 * @author Andrew Jackson <Andrew.Jackson@bl.uk>
 *
 */
public class MementosAggregator {
	static Logger log = Logger.getLogger(MementosAggregator.class.getCanonicalName());
	
	private PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();

	private CloseableHttpClient httpClient;

	private String[] defaultTimeMapPrefixes = {
			"http://www.webarchive.org.uk/wayback/archive/timemap/link/",
			"http://archive.today/timemap/",
			"http://wayback.archive-it.org/all/timemap/link/",
			"http://wayback.vefsafn.is/wayback/timemap/link/",
			"http://web.archive.org/web/timemap/link/",
			"http://webarchive.proni.gov.uk/timemap/",
			"https://swap.stanford.edu/timemap/link/",
			"http://webarchive.nationalarchives.gov.uk/timemap/",
			"http://webarchive.parliament.uk/timemap/"
	};
	
	private List<String> timeMapPrefixes;
	private List<String> timeGates;

	public MementosAggregator() {
		// Try to get latest list:
		try {
			getTimeMapPrefixes();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			this.timeMapPrefixes = new ArrayList<String>();
			for( String item : defaultTimeMapPrefixes) {
				this.timeMapPrefixes.add(item);
			}
		}
		// Output
		for( String tg : this.timeGates ) {
			log.error("TG: "+tg);
		}
		
		// Increase max total connection to 200
		cm.setMaxTotal(200);
		// Increase default max connection per route to 20
		cm.setDefaultMaxPerRoute(20);

		//
		int timeoutSeconds = 15;
		RequestConfig requestConfig = RequestConfig.custom()
				.setConnectTimeout(timeoutSeconds * 1000)
				.setSocketTimeout(timeoutSeconds * 1000).build();
		httpClient = HttpClients.custom()
				.setDefaultRequestConfig(requestConfig)
				.disableRedirectHandling()
				.setConnectionManager(cm).build();

	}

	/**
	 * 
	 * @param url
	 * @return 
	 * @throws InterruptedException
	 */
	public MementoList lookup(String url) throws InterruptedException {
		long start = System.currentTimeMillis();
		
		// Create a threadsafe holder for results:
		ConcurrentHashMap<String,Memento> ms = new ConcurrentHashMap<String,Memento>();

		// Spawn a thread for each timeMap endpoint:
		// create a thread for each URI
		GetThread[] threads = new GetThread[timeMapPrefixes.size()];
		for (int i = 0; i < threads.length; i++) {
			log.error("Defining "+i+" "+timeGates.get(i));
			MementoClient httpget = new MementoClient(timeGates.get(i), httpClient);
			threads[i] = new GetThread(ms, httpget, url);
		}

		// start the threads
		for (int j = 0; j < threads.length; j++) {
			log.warn("Starting..."+j);
			threads[j].start();
		}

		// join the threads
		for (int j = 0; j < threads.length; j++) {
			log.warn("JOINING..."+j);
			threads[j].join();
		}
		long end = System.currentTimeMillis();

		log.error("Overall, took " + (end - start) + " got "+ms.values().size()+ " mementos.");
		
		MementoList ml = new MementoList();
		for( Memento m : ms.values()) {
			ml.add(m);
		}
		
		return ml;
	}

	static class GetThread extends Thread {

		private final MementoClient mc;
		private final HttpContext context;
		private final String url;
		private ConcurrentHashMap<String, Memento> ms;

		public GetThread(ConcurrentHashMap<String, Memento> ms, MementoClient mc, String url) {
			this.ms = ms;
			this.mc = mc;
			this.context = HttpClientContext.create();
			this.url = url;
		}

		@Override
		public void run() {
			long start = System.currentTimeMillis();
			try {
				mc.setTargetURI(url);
				for( Memento item: mc.getMementos()) {
					log.info("TG+"+mc.getTimegateUri()+":\n\t"+item.getUrl());
					ms.put(item.getDateTimeString(), item);
				}
			} catch (RuntimeException ex) {
				// Handle errors
				log.error("ERROR-- " + ex + " for " + url, ex);
			}
			long end = System.currentTimeMillis();
			log.info(url + " took " + (end - start));
		}
	}

	private  void getTimeMapPrefixes() throws Exception {
		//
		this.timeMapPrefixes = new ArrayList<String>();
		this.timeGates = new ArrayList<String>();
		//
		DocumentBuilderFactory domFactory = DocumentBuilderFactory
				.newInstance();
		domFactory.setNamespaceAware(true);
		DocumentBuilder builder = domFactory.newDocumentBuilder();
		Document doc = builder
				.parse(new URL(
				"http://labs.mementoweb.org/aggregator_config/archivelist.xml")
				.openStream());
		XPath xpath = XPathFactory.newInstance().newXPath();
		// XPath Query for showing all nodes value
		XPathExpression expr = xpath.compile("//links/link/timemap");

		Object result = expr.evaluate(doc, XPathConstants.NODESET);
		NodeList nodes = (NodeList) result;
		for (int i = 0; i < nodes.getLength(); i++) {
			timeMapPrefixes.add(nodes.item(i).getAttributes().getNamedItem("uri")
					.getNodeValue());
		}

		// XPath Query for showing all nodes value
		expr = xpath.compile("//links/link/timegate");

		result = expr.evaluate(doc, XPathConstants.NODESET);
		nodes = (NodeList) result;
		for (int i = 0; i < nodes.getLength(); i++) {
			timeGates.add(nodes.item(i).getAttributes().getNamedItem("uri")
					.getNodeValue());
		}

	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		MementosAggregator me = new MementosAggregator();
		try {
			me.lookup("http://www.bl.uk");
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
