package dev.memento;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class MementoClientTest {

	private MementoClient mc;

	@Before
	public void setUp() throws Exception {
		mc = new MementoClient();
	}

	@Test
	public void test() {
		mc.setTargetURI("http://portico.bl.uk");
		MementoList ms = mc.getMementos();
		assertNull("Got error message: " + mc.getErrorMessage(),
				mc.getErrorMessage());
		printMementosSummary(ms);

		ms = mc.getMementos("http://www.jam.org.uk/");
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
