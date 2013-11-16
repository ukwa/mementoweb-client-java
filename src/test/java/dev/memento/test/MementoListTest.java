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


import java.text.DateFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import dev.memento.Link;
import dev.memento.Memento;
import dev.memento.MementoList;
import dev.memento.SimpleDateTime;
import junit.framework.TestCase;

public class MementoListTest extends TestCase {

	private MementoList mList;
	private String[] mLinks = {
			"<http://api.wayback.archive.org/memento/20120114015201/http://www.cnn.com/>;rel=\"memento\"; datetime=\"Fri, 13 Jan 2012 18:52:01 UTC\"",
			"<http://api.wayback.archive.org/memento/20120118122633/http://cnn.com/>;rel=\"memento\"; datetime=\"Wed, 18 Jan 2012 05:26:33 UTC\"",
			"<http://api.wayback.archive.org/memento/20121124182832/http://www.cnn.com/>;rel=\"memento\"; datetime=\"Sat, 24 Nov 2012 11:28:32 UTC\"",
			"<http://api.wayback.archive.org/memento/20121231184654/http://www.cnn.com/>;rel=\"memento\"; datetime=\"Mon, 31 Dec 2012 11:46:54 UTC\"",
			"<http://wayback.archive-it.org/all/20130102192043/http://www.cnn.com/>;rel=\"memento\"; datetime=\"Wed, 02 Jan 2013 12:20:43 UTC\""
	};
	
	protected static void setUpBeforeClass() throws Exception {
	}

	protected void setUp() throws Exception {
		super.setUp();
		
		mList = new MementoList();
		for (String link : mLinks) {
			mList.add(new Memento(new Link(link)));
		}
		// Attempting to fix date formatting for tests to pass.
		SimpleDateTime.mDateFormat = DateFormat.getDateInstance(DateFormat.DEFAULT, Locale.US);
        SimpleDateTime.mTimeFormat = DateFormat.getTimeInstance(DateFormat.DEFAULT, Locale.US);
        
	}

	public void testGetClosestDate() {
		SimpleDateTime datetime = new SimpleDateTime("Fri, 13 Jan 2012 18:52:01 UTC");
		SimpleDateTime actual = mList.getClosestDate(new SimpleDateTime("Fri, 13 Jan 2012 00:00:00 UTC")).getDateTime();
		assertEquals(datetime, actual);
		/*
		actual = mList.getClosestDate(new SimpleDateTime("Fri, 13 Jan 2012 23:00:00 UTC"));
		assertEquals(datetime, actual);
		
		actual = mList.getClosestDate(new SimpleDateTime("Fri, 16 Jan 2012 00:00:00 UTC"));
		*/
	}

	public void testGetIndexSimpleDateTime() {
		int index = mList.getIndex(new SimpleDateTime("Fri, 13 Jan 2012 18:52:01 UTC"));
		assertEquals(0, index);
		
		index = mList.getIndex(new SimpleDateTime("Sat, 14 Jan 2012 18:52:01 UTC"));
		assertEquals(-1, index);		
	}

	public void testGetIndexByDate() {
		int index = mList.getIndexByDate(new SimpleDateTime("Fri, 13 Jan 2012 16:00:00 UTC"));
		assertEquals(0, index);
		
		index = mList.getIndexByDate(new SimpleDateTime("Sat, 14 Jan 2012 16:00:00 UTC"));
		assertEquals(-1, index);		
	}

	public void testGetFirst() {
		Memento actual = mList.getFirst();
		Memento expected = new Memento(new Link(mLinks[0]));
		assertEquals(expected, actual);
	}

	public void testGetLast() {
		Memento actual = mList.getLast();
		Memento expected = new Memento(new Link(mLinks[mLinks.length-1]));
		assertEquals(expected, actual);
	}

	public void testGetAllDates() {
		CharSequence[] list = mList.getAllDates();
		assertEquals(mList.size(), list.length);
	}

	public void testGetAllYears() {
		TreeMap<Integer, Integer> years = mList.getAllYears();
		
		// Should only be 2 different years
		assertEquals(2, years.size());
		
		assertEquals(Integer.valueOf(4), years.get(2012));
		assertEquals(Integer.valueOf(1), years.get(2013));
		assertEquals(null, years.get(2014));
	}

	public void testGetMonthsForYear() {
		LinkedHashMap<CharSequence, Integer> months = mList.getMonthsForYear(2012);
		assertEquals(3, months.size());
		
		// Make sure the months are in the proper order and with the right number of days
		String[] monthNames = {"January", "November", "December"};
		int[] size = {2, 1, 1};
		
		int i = 0;
		for(Map.Entry<CharSequence, Integer> entry : months.entrySet()) {
			CharSequence month = entry.getKey();
			int numDays = entry.getValue();
			assertTrue(monthNames[i].equals(month));
			assertEquals(size[i], numDays);
    		i++;
		}
		
		months = mList.getMonthsForYear(2013);
		assertEquals(1, months.size());
		assertEquals(Integer.valueOf(1), months.get("January"));		
	}
	
	public void testGetDatesForMonthAndYear() {
		CharSequence[] dates = mList.getDatesForMonthAndYear(1, 2012);
		assertEquals("Jan 13, 2012 6:52:01 PM", dates[0]);
		assertEquals("Jan 18, 2012 5:26:33 AM", dates[1]);
		
		dates = mList.getDatesForMonthAndYear(0, 2012);
		assertNull(dates);
		
		dates = mList.getDatesForMonthAndYear(2, 2012);
		assertEquals(0, dates.length);		
	}

	public void testGetDatesForYear() {
		CharSequence[] dates = mList.getDatesForYear(2012);
		assertEquals(4, dates.length);
		assertEquals("Jan 13, 2012 6:52:01 PM", dates[0]);
		assertEquals("Jan 18, 2012 5:26:33 AM", dates[1]);
		assertEquals("Dec 31, 2012 11:46:54 AM", dates[3]);
		
		dates = mList.getDatesForYear(2013);
		assertEquals(1, dates.length);
		assertEquals("Jan 2, 2013 12:20:43 PM", dates[0]);
	}

	public void testGetByMonthAndYear() {
		Memento[] list = mList.getByMonthAndYear(1, 2012);
		assertEquals(mList.get(0), list[0]);
		assertEquals(mList.get(1), list[1]);
		
		list = mList.getByMonthAndYear(1, 2013);
		assertEquals(mList.get(4), list[0]);
	}
	
	public void testGetByYear() {
		Memento[] list = mList.getByYear(2012);
		assertEquals(mList.get(0), list[0]);
		assertEquals(mList.get(1), list[1]);
		assertEquals(mList.get(2), list[2]);
		
		list = mList.getByMonthAndYear(1, 2013);
		assertEquals(mList.get(4), list[0]);
	}
}
