/**
 * 
 */
package uk.bl.wa.memento.client;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;

/**
 * @author Andrew Jackson <Andrew.Jackson@bl.uk>
 *
 */
public class MementosAggregator {
	
	private PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();

	private CloseableHttpClient httpClient;

	private String[] timeMapPrefixes = {
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

	public MementosAggregator() {
		// Increase max total connection to 200
		cm.setMaxTotal(200);
		// Increase default max connection per route to 20
		cm.setDefaultMaxPerRoute(20);

		//
		int timeoutSeconds = 30;
		RequestConfig requestConfig = RequestConfig.custom()
				.setConnectTimeout(timeoutSeconds * 1000)
				.setSocketTimeout(timeoutSeconds * 1000).build();
		httpClient = HttpClients.custom()
				.setDefaultRequestConfig(requestConfig)
				.setConnectionManager(cm).build();

	}

	/**
	 * 
	 * @param url
	 * @throws InterruptedException
	 */
	public void lookup(String url) throws InterruptedException {
		long start = System.currentTimeMillis();

		// Spawn a thread for each timeMap endpoint:
		// create a thread for each URI
		GetThread[] threads = new GetThread[timeMapPrefixes.length];
		for (int i = 0; i < threads.length; i++) {
			HttpGet httpget = new HttpGet(timeMapPrefixes[i] + url);
			threads[i] = new GetThread(httpClient, httpget);
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

		System.out.println("Overall, took " + (end - start));


	}

	static class GetThread extends Thread {

		private final CloseableHttpClient httpClient;
		private final HttpContext context;
		private final HttpGet httpget;

		public GetThread(CloseableHttpClient httpClient, HttpGet httpget) {
			this.httpClient = httpClient;
			this.context = HttpClientContext.create();
			this.httpget = httpget;
		}

		@Override
		public void run() {
			long start = System.currentTimeMillis();
			try {
				CloseableHttpResponse response = httpClient.execute(httpget,
						context);
				try {
					System.out.println("GOT " + response.getStatusLine()
							+ " from " + httpget.getURI());
					HttpEntity entity = response.getEntity();
					byte[] headBytes = new byte[100];
					entity.getContent().read(headBytes);
					System.out.println("GOT " + new String(headBytes) + "\n\n");
				} finally {
					response.close();
				}
			} catch (ClientProtocolException ex) {
				// Handle protocol errors
				System.err.println("ERROR " + ex + " for " + httpget.getURI());
			} catch (IOException ex) {
				// Handle I/O errors
				System.err.println("ERROR " + ex + " for " + httpget.getURI());
			}
			long end = System.currentTimeMillis();
			System.out.println(httpget.getURI() + " took " + (end - start));
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		MementosAggregator me = new MementosAggregator();
		try {
			me.lookup("http://www.google.co.uk");
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
