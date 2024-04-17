package common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.text.ParseException;

import org.junit.jupiter.api.Test;

public class StringTreeTest {
	@Test
	public void merge() throws ParseException {
		StringTree tree = new StringTree("fruit");
		StringTree child = tree.addChild("citrus", "orange");
		assertEquals("orange", child.getName());
		assertEquals("citrus", child.getParent().getName());		
		tree.addChild("citrus", "mandarine");
		tree.addChild("apple");
		tree.addChild("peach");
		assertEquals("(fruit:(apple:),(citrus:(mandarine:),(orange:)),(peach:))", tree.dump());
		
		StringTree other = new StringTree("fruit");
		tree.addChild("tropical", "banana");
		tree.addChild("tropical", "pineapple");
		
		tree.merge(other);
		assertEquals("(fruit:(apple:),(citrus:(mandarine:),(orange:)),(peach:),(tropical:(banana:),(pineapple:)))", tree.dump());
	}

	@Test
	public void testGetRoot() {
		assertEquals("(d:)", new StringTree("a").addChild("b", "c", "d").dump());
		assertEquals("(a:(b:(c:(d:))))", new StringTree("a").addChild("b", "c", "d").getRoot().dump());
	}
}
