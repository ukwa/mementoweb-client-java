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


public class TimeBundle {

	private String mUrl;
	private String mRel;
	
	public TimeBundle(Link link) {
		mUrl = link.getUrl();
		mRel = link.getRel();
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
	
	@Override
	public String toString() {
		return "TimeBundle: url=[" + mUrl + "] rel=[" + mRel + "]";
	}
}
