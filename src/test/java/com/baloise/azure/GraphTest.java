package com.baloise.azure;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.TreeSet;

import org.junit.jupiter.api.Test;

class GraphTest {

	Graph g = new Graph();
	@Test
	public void testExpandRoles() {
		assertEquals(new TreeSet<String>(asList("Member","ProductOwner", "ScrumMaster")), g.expandRoles(null));
		assertEquals(new TreeSet<String>(asList("Member","ProductOwner", "ScrumMaster")), g.expandRoles());
		assertEquals(new TreeSet<String>(asList("Member","ProductOwner", "ScrumMaster")), g.expandRoles("~SCRUM"));
		assertEquals(new TreeSet<String>(asList("Member","ProductOwner", "ScrumMaster", "test")), g.expandRoles("~SCRUM", "test"));
		assertEquals(new TreeSet<String>(asList("a", "test")), g.expandRoles("a", "test"));
	}

	@Test
	public void testGetRoleSchemes() {
		g.expandRoles("bla");
		final Graph expected = new Graph();
		assertNotEquals(expected.rolesSchemes, g.rolesSchemes);
		assertEquals(expected.getRoleSchemes(), g.getRoleSchemes());
	}

}
