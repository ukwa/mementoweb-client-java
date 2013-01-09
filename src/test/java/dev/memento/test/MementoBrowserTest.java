package dev.memento.test;

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


import static org.junit.Assert.*;

import org.junit.Test;

//import dev.memento.*;

public class MementoBrowserTest {

	String linkStr = "<http://mementoproxy.lanl.gov/google/timebundle/http://www.digitalpreservation.gov/>;rel=\"timebundle\"," +
     	"<http://www.digitalpreservation.gov/>;rel=\"original\"," +
     	"<http://mementoproxy.lanl.gov/google/timemap/link/http://www.digitalpreservation.gov/>;rel=\"timemap\";type=\"text/csv\"," +
     	"<http://webcache.googleusercontent.com/search?q=cache:http://www.digitalpreservation.gov/>;rel=\"first-memento last-memento memento\";datetime=\"Tue, 07 Sep 2010 11:54:29 GMT\"";
	
	@Test
	public void testFetchUrl() {
		fail("Not yet implemented");
	}

	@Test
	public void testMakeMementoRequests() {
		fail("Not yet implemented");
	}

	@Test
	public void testParseCsvLinks() {

		fail("Not yet implemented");
		
		/*
		MementoBrowser mb = new MementoBrowser();
		SimpleDateTime date = mb.parseCsvLinks(linkStr);
		SimpleDateTime expectedDatetime = new SimpleDateTime("Tue, 07 Sep 2010 11:54:29 GMT");
		assertEquals(expectedDatetime, date);
		*/
		
	}

}
