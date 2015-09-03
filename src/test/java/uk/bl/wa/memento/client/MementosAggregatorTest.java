package uk.bl.wa.memento.client;

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
