/**
 * 
 */
package uk.bl.wa.memento.client;

/*
 * #%L
 * MementoWeb Java Client
 * %%
 * Copyright (C) 2012 - 2015 The British Library
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
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
	
	private Map<String,String> icons;

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
			log.info("TG: "+tg);
		}
		
		// Increase max total connection to 200
		cm.setMaxTotal(200);
		// Increase default max connection per route to 20
		cm.setDefaultMaxPerRoute(20);

		//
		int timeoutSeconds = 15;
		// Proxy?
		HttpHost proxy = null;
		if( System.getProperty("http.proxyHost") != null ) {
    		proxy = new HttpHost( System.getProperty("http.proxyHost"), 
    				Integer.parseInt(System.getProperty("http.proxyPort")), "http");
    		log.debug("Proxying via "+proxy);
    	} else {
    		log.debug("No web proxy.");
    	}
		// Set up request config:
		RequestConfig requestConfig = RequestConfig.custom()
				.setConnectTimeout(timeoutSeconds * 1000)
				.setSocketTimeout(timeoutSeconds * 1000).setProxy(proxy).build();
    	// Set up the client:
		httpClient = HttpClients.custom()
				.setDefaultRequestConfig(requestConfig)
				.disableRedirectHandling()
				.setConnectionManager(cm)
			    .build();
		
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
			MementoClient httpget = new MementoClient(timeGates.get(i), httpClient);
			threads[i] = new GetThread(ms, httpget, url);
		}

		// start the threads
		for (int j = 0; j < threads.length; j++) {
			threads[j].start();
		}

		// join the threads
		for (int j = 0; j < threads.length; j++) {
			threads[j].join();
		}
		long end = System.currentTimeMillis();

		log.info("Overall, took " + (end - start) + " got "+ms.values().size()+ " mementos.");
		
		MementoList ml = new MementoList();
		for( Memento m : ms.values()) {
			ml.add(m);
		}
		
		return ml;
	}

	static class GetThread extends Thread {

		private final MementoClient mc;
		private final String url;
		private ConcurrentHashMap<String, Memento> ms;

		public GetThread(ConcurrentHashMap<String, Memento> ms, MementoClient mc, String url) {
			this.ms = ms;
			this.mc = mc;
			this.url = url;
		}

		@Override
		public void run() {
			long start = System.currentTimeMillis();
			try {
				mc.setTargetURI(url);
				for( Memento item: mc.getMementos()) {
					log.debug("TG+"+mc.getTimegateUri()+":\n\t"+item.getUrl());
					ms.put(item.getDateTimeString(), item);
				}
			} catch (RuntimeException ex) {
				// Handle errors
				log.error("ERROR-- " + ex + " for " + url, ex);
			}
			long end = System.currentTimeMillis();
			log.debug("TG " + mc.getTimegateUri()+ " + " + url + " took " + (end - start));
		}
	}

	private void getTimeMapPrefixes() throws Exception {
		//
		this.timeMapPrefixes = new ArrayList<String>();
		this.timeGates = new ArrayList<String>();
		this.icons = new HashMap<String,String>();
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
		// XPath Query for iterating through link objects:
		//XPathExpression expr = xpath.compile("(//links/link/timegate|//links/link/timemap)");
		XPathExpression expr = xpath.compile("//links/link");

		// Go through the link entries:
		Object result = expr.evaluate(doc, XPathConstants.NODESET);
		NodeList nodes = (NodeList) result;
		for (int i = 0; i < nodes.getLength(); i++) {
			Node n = nodes.item(i);
			// Look for timegate and timemap:
			String timeMap = xpath.evaluate("./timemap[1]/@uri", n);
			if(timeMap != null && ! timeMap.trim().isEmpty() ) {
				timeMapPrefixes.add(timeMap);
				log.info("Got TM: "+timeMap);
			}
			String timeGate = xpath.evaluate("./timegate[1]/@uri", n);
			if(timeGate != null && ! timeGate.trim().isEmpty() ) {
				timeGates.add(timeGate);
				log.info("Got TG: "+timeGate);
			}
			// Icon
			String icon = xpath.evaluate("./icon/@uri", n);
			if( icon != null && ! icon.trim().isEmpty() ) {
				String uri = timeGate;
				if( uri.endsWith("/timegate/")) {
					uri = uri.substring(0, uri.length() - 9);
				}
				this.icons.put(uri,icon);
				log.info("Got icon: "+uri+" > "+icon);
			}
		}
	}
	
	public String getIconUriForMemento( Memento m ) {
		for( String prefix: this.icons.keySet()) {
			if( m.getUrl().startsWith(prefix)) return this.icons.get(prefix);
		}
		return null;
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		MementosAggregator me = new MementosAggregator();
		MementoList ms = me.lookup("http://www.bl.uk");
		System.out.println("Got "+ms.size()+" mementos.");
	}

}
