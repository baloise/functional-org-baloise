package common;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public class StringTree implements Comparable<StringTree>{
	private final String name;
	private StringTree parent;
	private final TreeSet<StringTree> children = new TreeSet<>();
	private Map<String, String> lazyProperties;
	
	
	private Map<String, String> getProperties() {
		if(lazyProperties == null) lazyProperties = new HashMap<>();
		return lazyProperties;
	}

	public StringTree(String[] names) {
		this(names[0]);
		for (int i = 1; i < names.length; i++) {
			addChild(names[i]);			
		}
	}
	
	public StringTree(String name) {
		if(name == null) throw new IllegalArgumentException("name must not be null");
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public StringTree getRoot() {
		return parent == null? this : parent.getRoot();
	}
	
	public StringTree getParent() {
		return parent;
	}
	
	public boolean isLeaf() {
		return getChildren().isEmpty();
	}

	public Set<StringTree> getChildren() {
		return children;
	}
	
	public StringTree addChild(String ... names) {
		StringTree parent = this;
		for (String name : names) {
			parent = parent.addChild(new StringTree(name));
		}
		return parent;
	}

	public StringTree addChild(StringTree tree) {
		if(children.add(tree)) {
			tree.parent = this;
			return tree;
		} else {			
			return getChild(tree);
		}
	}
	
	public StringTree getChild(String ... names) {
		StringTree tree = this;
		StringTree child;
		for (String name : names) {
			child = tree.getChild(name);
			if(child == null) throw new IllegalArgumentException(format("%s has no child named %s", tree.getName(), name));
			tree = child;
		}
		return tree;
	}

	public StringTree getChild(String name) {
		return getChild(new StringTree(name));
	}
	
	public StringTree getChild(StringTree tree) {
		if(tree == null) throw new IllegalArgumentException("tree must not be null");
		StringTree floor = children.floor(tree);
		return (floor != null && tree.compareTo(floor) == 0) ? floor : null; 
	}
	
	public StringTree merge(StringTree other) {
		if(!equals(other)) throw new IllegalArgumentException(format("Can not merge %s into %s - names musts be equal", other, this));
		for (StringTree otherChild : other.children) {
			StringTree myChild = getChild(other);
			if(myChild == null)
				addChild(otherChild);
			else
				myChild.merge(otherChild);
		}
		return this;
	}
	
	@Override
	public String toString() {
		return getName();
	}
	
	public String dump() {
		return format("(%s:%s)", name, children.stream().map(StringTree::dump).collect(joining(",")));
	}	

	@Override
	public int compareTo(StringTree o) {
		return getName().compareTo(o.getName());
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StringTree other = (StringTree) obj;
		return compareTo(other) == 0;
	}

	public StringTree withProperty(String key, String value) {
		getProperties().put(key, value);
		return this;
	}
	
	public String getProperty(String key) {
		return getProperties().get(key);
	}
	
}
