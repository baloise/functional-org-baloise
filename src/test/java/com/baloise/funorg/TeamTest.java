package com.baloise.funorg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TeamTest {

	@Test
	public void parseFAADTEAMBBEDevOpsSystemTeamINTERNAL() {
		Team team = Team.parse("F-AAD-TEAM-UNIT-DevOpsSystemTeam-INTERNAL");
		assertEquals("UNIT", team.unit());
		assertEquals("DevOpsSystemTeam", team.name());
		assertTrue(team.internal());
	}
	
	@Test
	public void parseFAADTEAMGITDevOpsSystemEXTERNAL() {
		Team team = Team.parse("F-AAD-TEAM-OTHER-ATEAM-EXTERNAL");
		assertEquals("OTHER", team.unit());
		assertEquals("ATEAM", team.name());
		assertFalse(team.internal());
	}
	
	@Test
	public void parseFAADTEAMBCHITDevOpsComplianceINTERNAL() {
		Team team = Team.parse("F-AAD-TEAM-A-UNIT-ATEAM-INTERNAL");
		assertEquals("A-UNIT", team.unit());
		assertEquals("ATEAM", team.name());
		assertFalse(team.internal());
	}

	
}
