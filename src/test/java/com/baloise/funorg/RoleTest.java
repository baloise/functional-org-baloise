package com.baloise.funorg;


import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RoleTest {

	@Test
	public void testRole() {
		assertEquals("F-AAD-ROLE-Product-Owner", Role.PRODUCT_OWNER.entitlement);
	}

}
