package dev.memento;

/*
 * #%L
 * MementoWeb Java Client Stubs
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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class MementoClientTest {

	private MementoClient mc;

	@Before
	public void setUp() throws Exception {
		mc = new MementoClient();
		mc.setTimegateUri("http://www.webarchive.org.uk/wayback/archive/");
	}

	@Test
	public void test() {
		System.out.println("Testing via "+mc.getTimegateUri());
		mc.setTargetURI("http://www.bl.uk");
		MementoList ms = mc.getMementos();
		assertNull("Got error message: " + mc.getErrorMessage(),
				mc.getErrorMessage());
		printMementosSummary(ms);

		// TODO This has problems at the moment, due to aggr config presumably:
		ms = mc.getMementos("http://www.webarchive.org.uk");
	}

	private void printMementosSummary(MementoList ms) {
		System.out.println("Got " + ms.size() + " mementos.");
		assertTrue("No mementos found!", ms.size() > 0);
	}

}
