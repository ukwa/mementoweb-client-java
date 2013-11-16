package dev.memento.test;

/*
 * #%L
 * MementoWeb Java Client Stubs
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


import dev.memento.SimpleDateTime;
import junit.framework.TestCase;

public class SimpleDateTimeTest extends TestCase {

	protected static void setUpBeforeClass() throws Exception {
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	public void testCompareTo() {
		SimpleDateTime date1 = new SimpleDateTime();
    	SimpleDateTime date2 = new SimpleDateTime(date1);
    	
    	int actual = date1.compareTo(date2);
    	int expected = 0;
		assertEquals(expected, actual);
		
		// Make the two dates off by an hour but the same day
		date1 = new SimpleDateTime("Sat, 22 Dec 2007 09:05:17 GMT");
		date2 = new SimpleDateTime("Sat, 22 Dec 2007 10:05:17 GMT");
		
		actual = date1.compareTo(date2);
		expected = -1;
		assertEquals(expected, actual);
		
		date1 = new SimpleDateTime("Sat, 22 Dec 2007 09:05:17 GMT");
		date2 = new SimpleDateTime("Sun, 23 Dec 2007 10:05:17 GMT");
		actual = date1.compareTo(date2);
		assertTrue(actual < 0);
		
		date1 = new SimpleDateTime("Sat, 22 Dec 2007 09:05:17 GMT");
		date2 = new SimpleDateTime("Fri, 21 Dec 2007 10:05:17 GMT");
		actual = date1.compareTo(date2);
		assertTrue(actual > 0);
	}
}
