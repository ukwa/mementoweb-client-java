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


import java.io.Serializable;

public class Memento implements Comparable<Memento>, Serializable{
	
	private static final long serialVersionUID = 1L;
	
	private String mUrl;
	private String mRel;
	private SimpleDateTime mDatetime;
	
	
	public Memento(Link link) {
		mUrl = link.getUrl();
		mRel = link.getRel();
		mDatetime = link.getDatetime();	
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
	
	public void setRel(String rel) {
		this.mRel = rel;
	}
	
	public SimpleDateTime getDateTime() {
		return mDatetime;
	}
	
	public String getDateTimeString() {
		return mDatetime.longDateFormatted();
	}
	
	public void setDateTime(String datetime) {
		this.mDatetime = new SimpleDateTime(datetime);
	}
	
	public String getDateTimeSimple() {
		return mDatetime.dateFormatted().toString();
	}
	
	@Override
	public String toString() {
		return "Memento: url=[" + mUrl + "] rel=[" + mRel + "]" + 
			" datetime=[" + mDatetime + "]";
	}

	@Override
	public int compareTo(Memento memento) {			
		return mDatetime.compareTo(memento.mDatetime);
	}    	
}
