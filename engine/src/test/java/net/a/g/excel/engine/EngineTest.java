package net.a.g.excel.engine;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;

import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import net.a.g.excel.load.ExcelLoader;
import net.a.g.excel.model.ExcelCell;
import net.a.g.excel.model.ExcelResource;
import net.a.g.excel.model.ExcelSheet;
import net.a.g.excel.util.ExcelConfiguration;

@ExtendWith(WeldJunit5Extension.class)
public class EngineTest {

	@WeldSetup
	public WeldInitiator weld = WeldInitiator.from(ExcelEngine.class, ExcelConfiguration.class, ExcelLoader.class)
			.activate(RequestScoped.class, SessionScoped.class).build();;

	@Inject
	ExcelEngine engine;

	@Inject
	ExcelLoader loader;

	@BeforeEach
	public void setup() {
		assertNotNull(engine);
		InputStream inputStream = EngineTest.class.getResourceAsStream("/KYC.xlsx");
		assertTrue(loader.injectResource("KYC", null, inputStream));
		assertEquals(1, engine.countListOfResource());

	}

	@AfterEach
	public void close() {
		engine.clearAllResource();
		assertEquals(0, engine.countListOfResource());
	}

	@Test
	public void testEngine() {
		assertNotNull(engine);
	}

	@Test
	public void testTest() {
		assertEquals(1, engine.countListOfResource());
	}

	@Test
	public void testResourceKYC() {
		List<String> actual = new ArrayList(
				engine.lisfOfResource().stream().map(ExcelResource::getName).collect(Collectors.toList()));

		assertThat(actual, hasSize(1));
		List<String> expect = Arrays.asList("KYC");

		assertThat(actual, is(expect));
	}

	@Test
	public void testSheetKYC() {

		List<String> actual = engine.listOfSheet("KYC").stream().map(ExcelSheet::getName).collect(Collectors.toList());
		assertThat(actual, hasSize(3));

		List<String> expect = Arrays.asList("ComputeKYC", "COUNTRY", "AMOUNT");
		assertThat(actual, is(expect));
	}

	@Test
	public void testComputeKYCValues() {

		List<String> actual = new ArrayList(engine.mapOfFormularCell("KYC", "ComputeKYC").values().stream()
				.map(cell -> cell.getValue()).collect(Collectors.toList()));
		assertThat(actual, hasSize(5));

		List<String> expect = Arrays.asList("VLOOKUP(B3,COUNTRY!A1:B5,2,FALSE)", "VLOOKUP(B4,AMOUNT!A1:B5,2,TRUE)",
				"SUM(C2:C4)", "B9+C9", "IF(B2,50,0)");
		assertThat(actual, is(expect));
	}

	@Test
	public void testComputeKYCKeys() {

		List<String> actual = new ArrayList(engine.mapOfFormularCell("KYC", "ComputeKYC").keySet());
		assertThat(actual, hasSize(5));

		List<String> expect = Arrays.asList("C3", "C4", "C6", "C10", "C2");
		assertThat(actual, is(expect));
	}

	@Test
	public void testComputeKYCC6() {

		Map<String, ExcelCell> map = engine.mapOfCellCalculated("KYC", "ComputeKYC", new String[] { "C6" }, null,
				false);
		List<String> actual = new ArrayList(map.values());
		assertThat(actual, hasSize(1));

		assertThat(map.get("ComputeKYC!C6").getValue(), is("SUM(C2:C4)"));
	}

	@Test
	public void testComputeKYCC6Compute() {

		List<ExcelCell> map = engine.cellCalculation("KYC", List.of("ComputeKYC!C6"), Map.of("ComputeKYC!C9", "43"),
				false);
		assertThat(map, hasSize(1));

		assertThat(map.get(0).getValue(), is(0.0));
	}

	@Test
	public void testComputeKYCC10() {

		Map<String, ExcelCell> map = engine.mapOfCellCalculated("KYC", "ComputeKYC", new String[] { "C10" }, null,
				false);
		List<String> actual = new ArrayList(map.values());
		assertThat(actual, hasSize(1));

		assertThat(map.get("ComputeKYC!C10").getValue(), is("B9+C9"));
	}

	@Test
	public void testComputeKYCC10Compute() {

		List<ExcelCell> map = engine.cellCalculation("KYC", List.of("ComputeKYC!C10"), Map.of("ComputeKYC!C9", "42"),
				false);
		assertThat(map, hasSize(1));

		assertThat(map.get(0).getValue(), is("2021-02-12"));
	}

	@Test
	public void testComputeKYCC10Compute43() {

		List<ExcelCell> map = engine.cellCalculation("KYC", List.of("ComputeKYC!C10"), Map.of("ComputeKYC!C9", "43"),
				false);
		assertThat(map, hasSize(1));

		assertThat(map.get(0).getValue(), is("2021-02-13"));
	}

	@Test
	public void testLoading() {
		InputStream inputStream = EngineTest.class.getResourceAsStream("/KYC.xlsx");
		assertTrue(loader.injectResource("newtest", null, inputStream));
		assertEquals(2, engine.countListOfResource());
	}

	@Test
	public void testComputeKYCC6ComputeB2() {

		Map<String, String> input = Map.of("ComputeKYC!B2", "TRUE");

		List<ExcelCell> list = engine.cellCalculation("KYC", Arrays.asList("ComputeKYC!C6"), input, false);
		assertThat(list, hasSize(1));

		assertThat(list.get(0).getValue(), is(50.0));
	}

	@Test
	public void testComputeKYCC6ComputeB2B3() {

		Map<String, String> input = Map.of("ComputeKYC!B2", "TRUE", "ComputeKYC!B3", "CY");

		List<ExcelCell> list = engine.cellCalculation("KYC", Arrays.asList("ComputeKYC!C6"), input, false);
		assertThat(list, hasSize(1));

		assertThat(list.get(0).getValue(), is(75.0));
	}

}