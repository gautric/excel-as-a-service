package net.a.g.excel.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Getters, setters, constructors, toString and JSON mapping of every model class.
 * None of the model classes override equals/hashCode, so identity semantics are asserted.
 */
public class ExcelModelsTest {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Nested
	class ExcelCellTest {

		@Test
		void defaultConstructorLeavesEverythingNull() {
			ExcelCell c = new ExcelCell();
			assertNull(c.getAddress());
			assertNull(c.getValue());
			assertNull(c.getMetadata());
			assertNull(c.getType());
			assertNotNull(c.getLinks());
			assertTrue(c.getLinks().isEmpty());
		}

		@Test
		void settersAndGettersRoundTrip() {
			ExcelCell c = new ExcelCell();
			c.setAddress("S!A1");
			c.setValue(42.0);
			c.setMetadata("@output(X)");
			c.setType("NUMERIC");
			assertEquals("S!A1", c.getAddress());
			assertEquals(42.0, c.getValue());
			assertEquals("@output(X)", c.getMetadata());
			assertEquals("NUMERIC", c.getType());
		}

		@Test
		void fiveArgConstructorSetsAddressAndValue() {
			ExcelCell c = new ExcelCell("S!B2", "SUM(A1)", 3.0, "ref", "meta");
			assertEquals("S!B2", c.getAddress());
			assertEquals(3.0, c.getValue());
		}

		@Test
		void fiveArgConstructorKeepsMetadata() {
			ExcelCell c = new ExcelCell("S!B2", "SUM(A1)", 3.0, "ref", "meta");
			assertEquals("meta", c.getMetadata());
		}

		@Test
		void toStringListsAllFields() {
			ExcelCell c = new ExcelCell();
			c.setAddress("S!A1");
			c.setValue(1.5);
			c.setMetadata("m");
			c.setType("NUMERIC");
			assertEquals("ExcelCell [address=S!A1, value=1.5, metadata=m, type=NUMERIC]", c.toString());
		}

		@Test
		void equalsIsIdentityBased() {
			ExcelCell a = new ExcelCell();
			ExcelCell b = new ExcelCell();
			a.setAddress("X!A1");
			b.setAddress("X!A1");
			assertEquals(a, a);
			assertNotEquals(a, b);
		}

		@Test
		void jsonOmitsNullFieldsAndUsesPropertyNames() throws Exception {
			ExcelCell c = new ExcelCell();
			c.setAddress("S!A1");
			c.setValue(2.0);
			JsonNode node = MAPPER.valueToTree(c);
			assertEquals("S!A1", node.get("address").asText());
			assertEquals(2.0, node.get("value").asDouble());
			assertFalse(node.has("metadata"));
			assertFalse(node.has("type"));
			assertTrue(node.has("_links"));
		}

		@Test
		void jsonRoundTrip() throws Exception {
			String json = "{\"address\":\"S!C6\",\"value\":125.0,\"metadata\":\"@output(SCORE)\",\"type\":\"NUMERIC\"}";
			ExcelCell c = MAPPER.readValue(json, ExcelCell.class);
			assertEquals("S!C6", c.getAddress());
			assertEquals(125.0, c.getValue());
			assertEquals("@output(SCORE)", c.getMetadata());
			assertEquals("NUMERIC", c.getType());
		}
	}

	@Nested
	class ExcelErrorTest {

		@Test
		void settersAndGetters() {
			ExcelError e = new ExcelError();
			e.setCode("404");
			e.setError("not found");
			assertEquals("404", e.getCode());
			assertEquals("not found", e.getError());
		}

		@Test
		void jsonMapsErrorToMessageProperty() {
			ExcelError e = new ExcelError();
			e.setCode("400");
			e.setError("bad");
			JsonNode node = MAPPER.valueToTree(e);
			assertEquals("400", node.get("code").asText());
			assertEquals("bad", node.get("message").asText());
			assertFalse(node.has("error"));
		}
	}

	@Nested
	class ExcelLinkTest {

		@Test
		void settersAndGetters() {
			ExcelLink l = new ExcelLink();
			l.setRel("self");
			l.setHref("/api/excel/KYC");
			l.setType("application/json");
			l.setMethod("GET");
			assertEquals("self", l.getRel());
			assertEquals("/api/excel/KYC", l.getHref());
			assertEquals("application/json", l.getType());
			assertEquals("GET", l.getMethod());
		}

		@Test
		void jsonContainsOnlySetFields() {
			ExcelLink l = new ExcelLink();
			l.setRel("self");
			l.setMethod("POST");
			JsonNode node = MAPPER.valueToTree(l);
			assertEquals(2, node.size());
			assertEquals("self", node.get("rel").asText());
			assertEquals("POST", node.get("method").asText());
		}
	}

	@Nested
	class ExcelModelLinksTest {

		@Test
		void setLinksReplacesList() {
			ExcelSheet s = new ExcelSheet("A");
			List<ExcelLink> links = new ArrayList<>();
			s.setLinks(links);
			assertSame(links, s.getLinks());
		}

		@Test
		void linksSerialisedUnderUnderscoreLinks() {
			ExcelSheet s = new ExcelSheet("A");
			ExcelLink l = new ExcelLink();
			l.setRel("self");
			s.getLinks().add(l);
			JsonNode node = MAPPER.valueToTree(s);
			assertEquals("self", node.get("_links").get(0).get("rel").asText());
		}
	}

	@Nested
	class ExcelRequestTest {

		@Test
		void defaultConstructorGeneratesUuid() {
			ExcelRequest r = new ExcelRequest();
			assertNotNull(r.getUuid());
			assertDoesNotThrow(() -> UUID.fromString(r.getUuid()));
		}

		@Test
		void uuidConstructorKeepsGivenValue() {
			assertEquals("abc", new ExcelRequest("abc").getUuid());
		}

		@Test
		void defaultCollectionsAreEmptyAndMutable() {
			ExcelRequest r = new ExcelRequest();
			assertTrue(r.getOutputs().isEmpty());
			assertTrue(r.getInputs().isEmpty());
			r.getOutputs().add("A1");
			r.getInputs().put("B1", "x");
			assertEquals(List.of("A1"), r.getOutputs());
			assertEquals(Map.of("B1", "x"), r.getInputs());
		}

		@Test
		void defaultFlags() {
			ExcelRequest r = new ExcelRequest();
			assertFalse(r.isForce());
			assertNull(r.getGlobal());
			assertNull(r.getResource());
			assertNull(r.getSheet());
		}

		@Test
		void settersAndGetters() {
			ExcelRequest r = new ExcelRequest();
			r.setUuid("u");
			r.setResource("KYC");
			r.setSheet("ComputeKYC");
			r.setForce(true);
			r.setGlobal(Boolean.TRUE);
			r.setOutputs(List.of("C6"));
			Map<String, String> in = new HashMap<>();
			in.put("B2", "true");
			r.setInputs(in);
			assertEquals("u", r.getUuid());
			assertEquals("KYC", r.getResource());
			assertEquals("ComputeKYC", r.getSheet());
			assertTrue(r.isForce());
			assertEquals(Boolean.TRUE, r.getGlobal());
			assertEquals(List.of("C6"), r.getOutputs());
			assertSame(in, r.getInputs());
		}

		@Test
		void toStringContainsEveryField() {
			ExcelRequest r = new ExcelRequest("id");
			r.setResource("R");
			r.setSheet("S");
			r.setForce(true);
			r.setGlobal(false);
			r.setOutputs(List.of("A1"));
			r.setInputs(Map.of("B1", "v"));
			assertEquals("ExcelRequest [uuid=id, resource=R, sheet=S, force=true, global=false, outputs=[A1], inputs={B1=v}]",
					r.toString());
		}

		@Test
		void jsonDeserialisation() throws Exception {
			String json = "{\"uuid\":\"x\",\"resource\":\"KYCAPI\",\"sheet\":\"ComputeKYC\",\"global\":true,"
					+ "\"outputs\":[\"SCORE\"],\"inputs\":{\"PEP\":\"true\"}}";
			ExcelRequest r = MAPPER.readValue(json, ExcelRequest.class);
			assertEquals("x", r.getUuid());
			assertEquals("KYCAPI", r.getResource());
			assertEquals("ComputeKYC", r.getSheet());
			assertEquals(Boolean.TRUE, r.getGlobal());
			assertEquals(List.of("SCORE"), r.getOutputs());
			assertEquals("true", r.getInputs().get("PEP"));
		}
	}

	@Nested
	class ExcelResourceTest {

		@Test
		void constructors() {
			assertNull(new ExcelResource().getName());
			assertEquals("KYC", new ExcelResource("KYC").getName());
		}

		@Test
		void settersAndGetters() {
			ExcelResource r = new ExcelResource();
			byte[] doc = { 1, 2, 3 };
			r.setName("n");
			r.setFile("n.xlsx");
			r.setDoc(doc);
			assertEquals("n", r.getName());
			assertEquals("n.xlsx", r.getFile());
			assertArrayEquals(new byte[] { 1, 2, 3 }, r.getDoc());
		}

		@Test
		void toStringTruncatesDoc() {
			ExcelResource r = new ExcelResource("n");
			r.setFile("n.xlsx");
			r.setDoc(new byte[] { 10, 20, 30, 40, 50 });
			assertEquals("ExcelResource [name=n, file=n.xlsx, doc=[10, 20, 3.... ]", r.toString());
		}

		@Test
		void toStringWithNullDocDoesNotThrow() {
			assertDoesNotThrow(() -> new ExcelResource("n").toString());
		}


		@Test
		void docIsNotSerialisedToJson() {
			ExcelResource r = new ExcelResource("n");
			r.setFile("n.xlsx");
			r.setDoc(new byte[] { 1 });
			JsonNode node = MAPPER.valueToTree(r);
			assertFalse(node.has("doc"));
			assertEquals("n", node.get("name").asText());
			assertEquals("n.xlsx", node.get("file").asText());
		}
	}

	@Nested
	class ExcelSheetTest {

		@Test
		void constructors() {
			assertNull(new ExcelSheet().getName());
			assertEquals("S", new ExcelSheet("S").getName());
		}
	}

	@Nested
	class ExcelResultTest {

		@Test
		void defaultConstructor() {
			ExcelResult r = new ExcelResult();
			assertEquals(0, r.getCount());
			assertNull(r.getUuid());
			assertNull(r.getCells());
			assertNull(r.getSheets());
			assertNull(r.getResources());
			assertNull(r.getError());
		}

		@Test
		void collectionOfSheetsFillsSheets() {
			ExcelResult r = new ExcelResult(List.of(new ExcelSheet("A"), new ExcelSheet("B")));
			assertEquals(2, r.getCount());
			assertEquals(2, r.getSheets().size());
			assertNull(r.getCells());
			assertNull(r.getResources());
		}

		@Test
		void collectionOfCellsFillsCells() {
			ExcelCell c = new ExcelCell();
			ExcelResult r = new ExcelResult(List.of(c));
			assertEquals(1, r.getCount());
			assertSame(c, r.getCells().iterator().next());
			assertNull(r.getSheets());
			assertNull(r.getResources());
		}

		@Test
		void collectionOfResourcesFillsResources() {
			ExcelResult r = new ExcelResult(List.of(new ExcelResource("a"), new ExcelResource("b"), new ExcelResource("c")));
			assertEquals(3, r.getCount());
			assertEquals(3, r.getResources().size());
			assertNull(r.getCells());
		}

		@Test
		void emptyCollectionGivesEmptyResources() {
			ExcelResult r = new ExcelResult(List.of());
			assertEquals(0, r.getCount());
			assertNotNull(r.getResources());
			assertTrue(r.getResources().isEmpty());
		}

		@Test
		void settersAndGetters() {
			ExcelResult r = new ExcelResult();
			ExcelError e = new ExcelError();
			r.setUuid("u");
			r.setCount(7);
			r.setError(e);
			r.setCells(List.of(new ExcelCell()));
			r.setSheets(List.of(new ExcelSheet("s")));
			r.setResources(List.of(new ExcelResource("r")));
			assertEquals("u", r.getUuid());
			assertEquals(7, r.getCount());
			assertSame(e, r.getError());
			assertEquals(1, r.getCells().size());
			assertEquals("s", r.getSheets().iterator().next().getName());
			assertEquals("r", r.getResources().iterator().next().getName());
		}

		@Test
		void jsonUsesUnderscoreCountAndOmitsNulls() {
			ExcelResult r = new ExcelResult(List.of(new ExcelSheet("A")));
			r.setUuid("u");
			JsonNode node = MAPPER.valueToTree(r);
			assertEquals(1, node.get("_count").asInt());
			assertEquals("u", node.get("uuid").asText());
			assertEquals("A", node.get("sheets").get(0).get("name").asText());
			assertFalse(node.has("cells"));
			assertFalse(node.has("error"));
		}
	}
}
