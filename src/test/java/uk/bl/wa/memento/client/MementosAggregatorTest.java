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

import static org.junit.Assert.*;

import org.junit.Test;

import dev.memento.MementoList;

public class MementosAggregatorTest {

	@Test
	public void testSimpleInvocation() throws InterruptedException {
		MementosAggregator me = new MementosAggregator();
		MementoList ms = me.lookup("http://www.bl.uk");
		System.out.println("Got "+ms.size()+" mementos.");
	}

}
