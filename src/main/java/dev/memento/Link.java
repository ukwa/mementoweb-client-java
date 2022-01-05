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


import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class Link {
	Logger log = LogManager.getLogger(Link.class.getCanonicalName());
	
	private String mUrl;
	private String mRel;
	private String mType;
	private SimpleDateTime mDatetime;
	
		
	/*
	 *  Parse the text provided which comes from the http response
	 *  Examples:
	 *  
	 *  <http://mementoproxy.lanl.gov/aggr/timebundle/http://www.harding.edu/fmccown/>;rel="timebundle",
	 *  <http://www.harding.edu/fmccown/>;rel="original",<http://mementoproxy.lanl.gov/aggr/timemap/link/http://www.harding.edu/fmccown/>;rel="timemap";type="text/csv",
	 *  <http://web.archive.org/web/20010724154504/www.harding.edu/fmccown/>;rel="first-memento prev-memento";datetime="Tue, 24 Jul 2001 15:45:04 GMT",
	 *  <http://web.archive.org/web/20010910203350/www.harding.edu/fmccown/>;rel="memento";datetime="Mon, 10 Sep 2001 20:33:50 GMT",
	 *  <http://webcache.googleusercontent.com/search?q=cache:http://www.digitalpreservation.gov/>;rel="first-memento last-memento memento";datetime="Tue, 07 Sep 2010 11:54:29 GMT"
	 *  
	 */	
	public Link(String link) {
				
		// Grab URL
		int index = link.indexOf(">;");
		if (index == -1) {
			log.error("Unable to find >; in [" + link + "]");
			return;
		}
		else
			mUrl = link.substring(1, index);
					
		// Remove URL part so we can grab the remaining parts
		link = link.substring(index + 2);
		
		String[] parts = link.split("\\s*;\\s*");
		if (parts.length == 0) {
			log.error("Unexpected format: [" + link + "]");
			return;
		}
				
		for (String part : parts) {
			part = part.trim();
			
			// Get rid of potential spaces before and after equal sign
			part = part.replaceFirst("^(\\w+)\\s*=\\s*", "$1=");
			
			if (part.startsWith("rel="))
				parseRel(part);
			else if (part.startsWith("datetime="))
				parseDatetime(part);
			else if (part.startsWith("type="))
				parseType(part);
			else if (part.startsWith("from="))
				parseFromDate(part);
			else if (part.startsWith("until="))
				parseUntilDate(part);
			else {
				log.error("Unexpected value: [" + part + "] when looking for rel, datetime, or type");
				return;
			}
		}
		
		// Make sure all parts were present
		
		if (mRel == null) {
			log.error("Missing rel for memento in: [" + link + "]");		
		}
		else if (mRel.contains("memento")) {									
			if (mDatetime == null)
				log.error("Missing datetime for memento in: [" + link + "]");		
		}
		else if (mRel.equals("timemap")) {
			if (mType == null)
				log.error("Missing type for timemap in: [" + link + "]");		
		}
	}
	
	private void parseFromDate(String date) {
		// TODO: Parse "from" date for TimeMap
	}
	
	private void parseUntilDate(String date) {
		// TODO: Parse "until" date for TimeMap
	}
	
	private void parseRel(String rel) {
				
		// Get rid of rel= at the beginning and quotes
		rel = rel.replace("rel=\"", "");
		rel = rel.replaceAll("\",?$", "");
		
		if (rel.contains("timebundle")) 
			mRel = "timebundle";
		else if (rel.contains("timemap")) 
			mRel = "timemap";
		else if (rel.contains("timegate")) 
			mRel = "timegate";
		else if (rel.contains("original"))
			mRel = "original";
		else if (rel.contains("self"))   // Used only on timemaps 
			mRel = "self";     
		else if (rel.contains("memento")) {		
			// Could contain "first memento", "last memento", "prev memento", "next memento", 
			// or "memento" or any combination of these like "first last memento"
			mRel = rel;
		}
		else {
			log.error("Undefined rel: [" + rel + "]");
			return;
		}    
	}
	
	private void parseDatetime(String datetime) {
		
		// Remove initial part and quotes
		datetime = datetime.replaceAll("datetime=\"", "");
		datetime = datetime.replaceAll("\",?$", "");
		
		mDatetime = new SimpleDateTime(datetime);		
	}
	
	private void parseType(String type) {
		
		// Remove initial part and quotes (and possible comma at end)
		type = type.replaceAll("type=\"", "");
		type = type.replaceAll("\",?$", "");
		
		mType = type;
	}

	public String getUrl() {
		return mUrl;
	}

	public void setUrl(String url) {
		this.mUrl = url;
	}

	public String getRel() {
		return mRel;
	}
	
	public String[] getRelArray() {
		if (mRel == null)
			return null;
		else
			return mRel.split("\\s+");
	}

	public void setRel(String rel) {
		this.mRel = rel;
	}

	public String getType() {
		return mType;
	}

	public void setType(String type) {
		this.mType = type;
	}

	public SimpleDateTime getDatetime() {
		return mDatetime;
	}

	public void setDatetime(SimpleDateTime datetime) {
		this.mDatetime = datetime;
	}
	
	@Override
	public String toString() {
		return "Link [mUrl=" + mUrl + ", mRel=" + mRel + ", mType=" + mType
				+ ", mDatetime=" + mDatetime + "]";
	}
}
