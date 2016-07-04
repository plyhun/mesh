package com.gentics.mesh.core.field.list;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.list.impl.NumberGraphFieldListImpl;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.list.impl.DateFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.HtmlFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NumberFieldListImpl;

public class GraphListFieldNumberVerticleTest extends AbstractGraphListFieldVerticleTest {

	@Override
	String getListFieldType() {
		return "number";
	}

	@Test
	public void testNumberList() throws IOException {
		NumberFieldListImpl listField = new NumberFieldListImpl();
		listField.add(0.1);
		listField.add(1337);
		listField.add(42);

		NodeResponse response = createNode(FIELD_NAME, listField);
		NumberFieldListImpl listFromResponse = response.getFields().getNumberFieldList(FIELD_NAME);
		assertEquals(3, listFromResponse.getItems().size());
	}

	@Test
	public void testUpdateNodeWithNumberField() throws IOException {
		Node node = folder("2015");

		List<List<Number>> valueCombinations = Arrays.asList(Arrays.asList(1.1, 2, 3), Arrays.asList(3, 2, 1.1), Collections.emptyList(),
				Arrays.asList(47.11, 8.15), Arrays.asList(3));

		for (int i = 0; i < 20; i++) {
			NodeGraphFieldContainer container = node.getGraphFieldContainer("en");
			List<Number> oldValue = getListValues(container, NumberGraphFieldListImpl.class, FIELD_NAME);
			List<Number> newValue = valueCombinations.get(i % valueCombinations.size());

			NumberFieldListImpl list = new NumberFieldListImpl();
			for (Number value : newValue) {
				list.add(value);
			}
			NodeResponse response = updateNode(FIELD_NAME, list);
			NumberFieldListImpl field = response.getFields().getNumberFieldList(FIELD_NAME);
			assertThat(field.getItems()).as("Updated field").containsExactlyElementsOf(list.getItems());
			node.reload();
			container.reload();

			assertEquals("Check version number", container.getVersion().nextDraft().toString(), response.getVersion().getNumber());
			assertEquals("Check old value", oldValue, getListValues(container, NumberGraphFieldListImpl.class, FIELD_NAME));
		}
	}

	@Test
	@Override
	public void testUpdateSetNull() {
		NumberFieldListImpl list = new NumberFieldListImpl();
		list.add(42);
		list.add(41.1f);
		updateNode(FIELD_NAME, list);

		NodeResponse secondResponse = updateNode(FIELD_NAME, null);
		assertThat(secondResponse.getFields().getNumberFieldList(FIELD_NAME)).as("Updated Field").isNull();
	}

	@Test
	public void testUpdateSetEmpty() {
		NumberFieldListImpl list = new NumberFieldListImpl();
		list.add(42);
		list.add(41.1f);
		updateNode(FIELD_NAME, list);

		NodeResponse secondResponse = updateNode(FIELD_NAME, new NumberFieldListImpl());
		assertThat(secondResponse.getFields().getNumberFieldList(FIELD_NAME)).as("Updated field list").isNotNull();
		assertThat(secondResponse.getFields().getNumberFieldList(FIELD_NAME).getItems()).as("Field value should be truncated").isEmpty();
	}

}
