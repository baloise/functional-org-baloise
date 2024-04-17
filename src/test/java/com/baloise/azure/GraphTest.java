package com.baloise.azure;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.TreeSet;

import org.junit.jupiter.api.Test;

class GraphTest {

	@Test
	public void testExpandRoles() {
		Graph g = new Graph();
		assertEquals(new TreeSet<String>(asList("Member","ProductOwner", "ScrumMaster")), g.expandRoles(null));
		assertEquals(new TreeSet<String>(asList("Member","ProductOwner", "ScrumMaster")), g.expandRoles());
		assertEquals(new TreeSet<String>(asList("Member","ProductOwner", "ScrumMaster")), g.expandRoles("~SCRUM"));
		assertEquals(new TreeSet<String>(asList("Member","ProductOwner", "ScrumMaster", "test")), g.expandRoles("~SCRUM", "test"));
		assertEquals(new TreeSet<String>(asList("a", "test")), g.expandRoles("a", "test"));
	}

}
