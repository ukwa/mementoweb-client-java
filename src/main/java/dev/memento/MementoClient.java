/**
 * MementoBrowser.java
 * 
 * Copyright 2010 Frank McCown
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  
 *  This is the Memento Browser activity which houses a customized web browser for
 *  performing http queries using Memento.
 *  
 *  Learn more about Memento:
 *  http://mementoweb.org/
 */

package dev.memento;

/*
 * #%L
 * mementoweb-java-client
 * %%
 * Copyright (C) 2012 - 2013 The British Library
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

import java.net.URISyntaxException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;


public class MementoClient {
	static Logger log = Logger.getLogger(MementoClient.class.getCanonicalName());
	
	static final int DIALOG_DATE = 0;
    static final int DIALOG_ERROR = 1;
    static final int DIALOG_MEMENTO_DATES = 2;
    static final int DIALOG_MEMENTO_YEARS = 3;
    static final int DIALOG_HELP = 4;
    
	private String[] mTimegateUris = { "http://timetravel.mementoweb.org/timegate/" };
	
	private HttpClient httpClient;

	// Let the TimeGate URI default to LANL Aggregator:
	private String mDefaultTimegateUri = mTimegateUris[0];
	
    private SimpleDateTime mDateChosen = new SimpleDateTime();
        
    private TimeBundle mTimeBundle;
    private HashSet<TimeMap> mTimeMaps;
    private Memento mFirstMemento;
    private Memento mLastMemento;
    private MementoList mMementos;
    
    private final int MAX_NUM_MEMENTOS_IN_LIST = 20;
    
    private CharSequence mErrorMessage;

    // Used when selecting a memento
    int mSelectedYear = 0;

    // Used in http requests
    public String mUserAgent;

	private String mDefaultErrorMessage = "Sorry, but there was an unexpected error that will "
			+ "prevent the Memento from being displayed. Try again in 5 minutes.";

    /**
     * 
     */
    public MementoClient() {
        // Set the date and time format
        SimpleDateTime.mDateFormat = DateFormat.getDateInstance(DateFormat.DEFAULT, Locale.US);
        SimpleDateTime.mTimeFormat = DateFormat.getTimeInstance(DateFormat.DEFAULT, Locale.US);        
        
        // Holds all the timemaps for the web page being viewed
        mTimeMaps = new HashSet<TimeMap>();    	
        mMementos = new MementoList();       
        //
        setupHttpClient();
    }

    /**
     * 
     * @param timegate
     */
    public MementoClient(String timegate) {
    	this();
    	this.setTimegateUri(timegate);
    }
    
    public MementoClient(String timegate, HttpClient httpClient) {
    	this.httpClient = httpClient;
    	this.setTimegateUri(timegate);
    }
    
    /**
     *  Helper to create a web-proxy-aware HttpClient:
     * @return
     */
    private void setupHttpClient() {
    	if( httpClient != null) {
    		log.debug("Using existing httpClient...");
    		return;
    	}
    	
    	HttpHost proxy = null;
    	if( System.getProperty("http.proxyHost") != null ) {
    		proxy = new HttpHost( System.getProperty("http.proxyHost"), 
    				Integer.parseInt(System.getProperty("http.proxyPort")), "http");
    		log.debug("Proxying via "+proxy);
    	} else {
    		log.debug("No web proxy.");
    	}
    	// Disable automatic redirect handling so we can process the 302 ourself 
		httpClient = HttpClientBuilder.create()
			    .disableRedirectHandling()
			    .setProxy(proxy)
			    .build();
    }

		
	/**
     * Make http requests to the Timegate at the proxy server to obtain a Memento 
     * and its TimeMap.  This is done in a background thread so the UI is not locked up.
     * If an error occurs, mErrorMessage is set to an error message which is shown
     * to the user.
     * @param initUrl The URL whose Memento is to be discovered
     */
    private void makeHttpRequests(String initUrl) {
    	
    	// Contact Memento proxy with chosen Accept-Datetime:
    	// http://mementoproxy.lanl.gov/aggr/timegate/http://example.com/
    	// Accept-Datetime: Tue, 24 Jul 2001 15:45:04 GMT    	   	
 
    	String url = mDefaultTimegateUri + initUrl;
        HttpGet httpget = new HttpGet(url);
        
        // Change the request date to 23:00:00 if this is the first memento.
        // Otherwise we'll be out of range.
        
        String acceptDatetime;
        
        if (mFirstMemento != null && mFirstMemento.getDateTime().equals(mDateChosen)) {
        	log.debug("Changing chosen time to 23:59 since datetime matches first Memento.");
        	SimpleDateTime dt = new SimpleDateTime(mDateChosen);
        	dt.setToLastHour();
        	acceptDatetime = dt.longDateFormatted();
        }
        else {
        	acceptDatetime = mDateChosen.longDateFormatted(); 
        }
        
        httpget.setHeader("Accept-Datetime", acceptDatetime);
        httpget.setHeader("User-Agent", mUserAgent);
                
        log.debug("Accessing: " + httpget.getURI());
        log.debug("Accept-Datetime: " + acceptDatetime);

        log.debug("HC mHR Requesting...");
        HttpResponse response = null;
		try {			
			response = httpClient.execute(httpget);
			
			log.debug("Response code = " + response.getStatusLine());
			
		} catch (Exception e) {
			mErrorMessage = "Sorry, we are having problems contacting the server. Please " +
					"try again later.";
			log.error("Exception when performing query to "+this.getTimegateUri(), e);
			return;		
		}
        log.debug("HC mHR Responded.");
        
        // Get back:
		// 300 (TCN: list with multiple Mementos to choose from)
		// or 302 (TCN: choice) 
		// or 404 (no Mementos for this URL)
    	// or 406 (TCN: list with only first and last Mementos)
		
		int statusCode = response.getStatusLine().getStatusCode(); 
		if (statusCode == 300) {
			// TODO: Implement.  Right now the lanl proxy doesn't appear to be returning this
			// code, so let's just ignore it for now.
			//FIXME log.debug("Pick a URL from list - NOT IMPLEMENTED");			
		} else if (statusCode == 301) {
			mErrorMessage = mDefaultErrorMessage;
			log.info("Got 301 pointing to: "
					+ response.getHeaders("Location")[0]);
			log.error("Status code 301 not supported!");
		} else if (statusCode == 302) {
			// Send browser to Location header URL
			// Note that the date/time of this memento is not given in the Location but can
			// be found when parsing the Link header.
			
			Header[] headers = response.getHeaders("Location");
			if (headers.length == 0) {
				mErrorMessage = mDefaultErrorMessage;
				log.error("Error: Location header not found in response headers.");
			}
			else {					
				final String redirectUrl = headers[0].getValue();
				
				// We can't update the view directly since we're running
				// in a thread, so use mUpdateResults to show a toast message
				// if accessing a different date than what was requested.
				
				//mHandler.post(mUpdateResults);
				
				// Parse various Links
				headers = response.getHeaders("Link");
				if (headers.length == 0) {
					log.error("Error: Link header not found in response headers.");
					mErrorMessage = "Sorry, but the Memento could not be accessed. Try again in 5 minutes.";
				}
				else {
					String linkValue = headers[0].getValue();
											
					mTimeMaps.clear();
			    	mTimeBundle = null;
			    	mMementos.clear();
			    	
			    	// Get the datetime of this mememnto which should be supplied in the
			    	// Link: headers
			    	// Do not add the mementos to the global list of mementos because
			    	// the global list will be created when we process the timemap later.
			    	Memento memento = parseCsvLinks(linkValue, false);
			    	
					if (mTimeMaps.size() > 0)
						if (!accessTimeMap() && mErrorMessage == null)
							mErrorMessage = "There were problems accessing the Memento's TimeMap. " +
									"Please try again later.";
				}
			}
		}		
		else if (statusCode == 404) {
			//FIXME log.debug("Received 404 from proxy so no mementos for " + initUrl);
			mErrorMessage = "Sorry, there are no Mementos for this web page.";
		}
		else if (statusCode == 406) {
														
			// Parse various Links
			Header[] headers = response.getHeaders("Link");
			
			if (headers.length == 0) {
				log.debug("Error: Link header not found in 406 response headers.");
				//mErrorMessage = "Sorry, but there was an error in retreiving this Memento.";
				
				// The lanl proxy has it wrong.  It should return 404 when the URL is not
				// present, so we'll just pretend this is a 404.
				mErrorMessage = "Sorry, but there are no Mementos for this URL.";						
			}
			else {
				String linkValue = headers[0].getValue();
				
				mTimeMaps.clear();
		    	mTimeBundle = null;
		    	mMementos.clear();
		    	
				parseCsvLinks(linkValue, false);		
		    	
				if (mTimeMaps.size() > 0)
					accessTimeMap();
				
				if (mFirstMemento == null || mLastMemento == null) {
					log.error("Could not find first or last Memento in 406 response for " + url);
					mErrorMessage = "Sorry, but there was an error in retreiving this Memento.";
				}
				else {			
					log.debug("Not available in this date range (" + mFirstMemento.getDateTimeSimple() +
							" to " + mLastMemento.getDateTimeSimple() + ")");
					
					// According to Rob Sanderson (LANL), we will only get 406 when the date is too
					// early, so redirect to first Memento
					
					// FIXME ?
										
				}
			}
		}
		else {
			mErrorMessage = "Sorry, but there was an unexpected error that will " +
				"prevent the Memento from being displayed. Try again in 5 minutes.";
			log.error("Unexpected response code in makeHttpRequests = " + statusCode);
		}               
    }  
     
    /**
     * Makes sure that this link contains a timemap that has not already been seen.
     * @param link
     * @return true if the timemap's URL already exists in the list of timemaps, false otherwise.
     */
    private boolean timeMapAlreadyExists(Link link) {
    	for (TimeMap tm : mTimeMaps) {
			if (tm.getUrl().equals(link.getUrl())) {
				log.debug("Link contains a duplicate timemap URL that is being " +
						"ignored: " + link.toString());
				return true;
			}
    	}
    	
    	return false;
    }
    
    /**
     * Parse the links in CSV format and return the date of the last item with rel="memento" since
     * this information is needed when getting a 302 and needing to find the resource's datetime.
     * 
     * Example data:
     * 	 <http://mementoproxy.lanl.gov/aggr/timebundle/http://www.harding.edu/fmccown/>;rel="timebundle",
     * 	 <http://www.harding.edu/fmccown/>;rel="original",
     * 	 <http://web.archive.org/web/20010724154504/www.harding.edu/fmccown/>;rel="first memento";datetime="Tue, 24 Jul 2001 15:45:04 GMT",
     * 	 <http://web.archive.org/web/20010910203350/www.harding.edu/fmccown/>;rel="memento";datetime="Mon, 10 Sep 2001 20:33:50 GMT",
     * 
     * Another example:
     *   <http://mementoproxy.lanl.gov/google/timebundle/http://www.digitalpreservation.gov/>;rel="timebundle",
     *   <http://www.digitalpreservation.gov/>;rel="original",
     *   <http://mementoproxy.lanl.gov/google/timemap/link/http://www.digitalpreservation.gov/>;rel="timemap";type="application/link-format",
     *   <http://webcache.googleusercontent.com/search?q=cache:http://www.digitalpreservation.gov/>;rel="first last memento";datetime="Tue, 07 Sep 2010 11:54:29 GMT"
     *   
     * @param links
     */
    public Memento parseCsvLinks(String links, boolean addToMementoList) {
    	    	
    	mFirstMemento = null;
    	mLastMemento = null;
    	
    	Memento returnMemento = null;
    	    	
    	// Dump to file for debugging
    	//dumpToFile(links);
    	    	
		String[] linkStrings = links.split("\"\\s*,");				
		log.debug("Start parsing " + linkStrings.length + " links");
		
		int mementoLinks = 0;
		
    	// Place all Links into the array and then sort it based on date
    	for (String linkStr : linkStrings) {
			    		
			// Add back "
			if (!linkStr.endsWith("\""))
				linkStr += "\"";
						
			linkStr = linkStr.trim();
			
			Link link = new Link(linkStr);
			
			String rel = link.getRel();
			if (rel.contains("memento")) {
				mementoLinks++;
				Memento m = new Memento(link);
				
				// There may be just one memento in the links, so it should be returned
				if (returnMemento == null)
					returnMemento = m;
				
				if (addToMementoList)
					mMementos.add(m);				
				
				// Peel out all values in rel which are separated by white space
				String[] items = link.getRelArray();
				for (String r : items) {						
					r = r.toLowerCase();
										
					// First and last should be reported in 302 response
					if (r.contains("first")) {
						mFirstMemento = m;
					}
					if (r.contains("last")) {
						mLastMemento = m;
					}
				}		
			}
			else if (rel.equals("timemap")) {
				// See if this is really a new timemap (server could be mistaken, and
				// we don't want to be caught in an infinite loop
				TimeMap tm = new TimeMap(link);
				if( "application/link-format".equalsIgnoreCase(tm.getType()) ) {
					if (!timeMapAlreadyExists(link)) {
						log.debug("Adding new timemap " + link.toString());
						mTimeMaps.add(tm);
					}
				} else {
					log.debug("Skipping timemap in unsupported format "+tm.getType());
				}
			}
			else if (rel.equals("timebundle")) {
				mTimeBundle = new TimeBundle(link);
			}
		}    	
    	    	
    	// Sorting can take a long time.  If there are just a few (like from a TimeGate), 
    	// go ahead and sort since they are not usually listed in order.  But a large 
    	// listing from a TimeMap is already sorted by the LANL proxy.
    	if (addToMementoList && mMementos.size() < 5) {
    		log.debug("Sorting short Memento list...");
    		Collections.sort(mMementos);
    	}
    	
    	log.debug("Finished parsing, found " + mementoLinks + " Memento links");		
    	log.debug("Total mementos: " + mMementos.size());
				
		// If these aren't set then this is likely a timemap 
		if (mFirstMemento == null)
			mFirstMemento = mMementos.getFirst();
		if (mLastMemento == null)
			mLastMemento = mMementos.getLast();    
		
		return returnMemento;
    }  
 
    /**
     * Return a timemap that has not been downloaded yet.
     * 
     * @return
     */
    private TimeMap getTimemapToDownload() {
//    	if (Log.LOG) {
//    		Log.d(LOG_TAG, "All " + mTimeMaps.size() + " timemaps:");
//    		for (TimeMap tm : mTimeMaps) {
//        		Log.d(LOG_TAG, tm.toString());
//        	}
//    	}    	
    	
    	for (TimeMap tm : mTimeMaps) {
    		if (!tm.isDownloaded()) 
    			return tm;
    	}
		return null;    	
    }
    
    /**
     * Retrieve the TimeMap from the Web and parse out the Mementos.
     * Currently this only recognizes TimeMaps using CSV formats. 
     * Other formats to be implemented: RDF/XML, N3, and HTML.
     * Supports paging timemaps where a timemap includes references
     * to other timemaps.
     * 
     * @return true if TimeMap was successfully retreived, false otherwise.
     */
    private boolean accessTimeMap() {    	   	

    	TimeMap tm = getTimemapToDownload();
    	
    	// Access every timemap that has been discovered
    	while (tm != null) {
    		    	
    		tm.setDownloaded(true);
	    	String url = tm.getUrl();
	        HttpGet httpget = new HttpGet(url);
	        httpget.setHeader("User-Agent", mUserAgent);
	                
	        log.debug("Accessing TimeMap: " + httpget.getURI());
	
	        log.debug("HC TM Requesting...");
	        HttpResponse response = null;
			try {			
				response = httpClient.execute(httpget);				
				log.debug("Response code = " + response.getStatusLine());
			} catch (Exception e) {
				log.error(Utilities.getExceptionStackTraceAsString(e));
				return false;                
			}
	        log.debug("HC TM Responded.");
			
	        // Should get back 200 unless something is really wrong			
			int statusCode = response.getStatusLine().getStatusCode(); 
			if (statusCode == 200) {
				
				// See if MIME type is the same as Type		
				Header type = response.getFirstHeader("Content-Type");
				if (type == null) {
					log.warn("Could not find the Content-Type for " + url);
				}
				else if (!type.getValue().contains(tm.getType())) {
					log.warn("Content-Type is [" + type.getValue() + "] but TimeMap type is [" +
							tm.getType() + "] for " + url);
				}
				
				// Timemap MUST be "application/link-format", but leave csv for
				// backwards-compatibility with earlier Memento implementations
				if (tm.getType().equals("text/csv") ||
						tm.getType().equals("application/link-format") ||
						tm.getType().equals("application/link-format")) {
					try {
						String responseBody = EntityUtils.toString(response.getEntity());
						parseCsvLinks(responseBody, true); 
					} catch (Exception ex) {
						//log.error(Utilities.getExceptionStackTraceAsString(ex));
						ex.printStackTrace();
						return false;
					} 
				}
				else {
					log.error("Unable to handle TimeMap type " + tm.getType());
					return false;
				}
			}		
			else if (statusCode == 404) {
				log.debug("404 response means no mementos");
				mErrorMessage = "Sorry, there are no Mementos for this web page.";
				return false;
			}
			else {
				log.debug("Unexpected response code in accessTimeMap = " + statusCode);
				return false;
			}        
			
			tm = getTimemapToDownload();
    	}
    	
        return true;
    }
    
    //@Deprecated
    public void setTargetURI( String target ) {
    	log.debug("Looking for "+target);
		// Just in case an archive URL was being viewed
    	target = Utilities.getUrlFromArchiveUrl(target);
    	// Start the requests...
    	this.mErrorMessage = null;
    	this.makeHttpRequests( target );
    }

    //@Deprecated
    public MementoList getMementos() {
    	return this.mMementos;
    }

    /**
     * 
     * @param uri
     * @return
     */
    public MementoList getMementos(String uri) {
    	this.setTargetURI(uri);
    	return this.getMementos();
    }
    
    /**
     * @return null if all is well.
     */
    public String getErrorMessage() {
    	if( this.mErrorMessage == null ) return null;
    	return this.mErrorMessage.toString();
    }

	/**
	 * @return the mTimegateUri
	 */
	public String getTimegateUri() {
		return mDefaultTimegateUri;
	}

	/**
	 * @param mTimegateUri the mTimegateUri to set
	 */
	public void setTimegateUri(String mTimegateUri) {
		this.mDefaultTimegateUri = mTimegateUri;
	}
	
	public void finalise() {
		// Deallocate all system resources
        httpClient.getConnectionManager().shutdown();
	}
	
    /**
     * Command-line utility to take a URL and look up who holds archived copies (Mementos)
     * @param args
     * @throws URISyntaxException 
     */
    public static void main( String[] args ) throws URISyntaxException {
    	String query = "http://www.bl.uk";
    	if( args.length > 0 ) {
    		query = args[0];
    	}
    	System.out.println("Looking for: "+query);
    	// Query:
    	MementoClient mc = new MementoClient();
    	long start = System.currentTimeMillis();
        log.debug("Launch: "+Calendar.getInstance());
    	//mc.setTimegateUri("http://www.webarchive.org.uk/wayback/memento/timegate/");
    	mc.setTargetURI(query);
        log.debug("Qdone: "+Calendar.getInstance());
        long end = System.currentTimeMillis();
    	// Get results:
    	//mc.getMementos().displayAll();
    	log.debug("Duration: "+(end-start)/1000.0);
    }
    
}
