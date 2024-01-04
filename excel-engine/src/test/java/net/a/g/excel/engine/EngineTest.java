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

import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import net.a.g.excel.load.ExcelLoader;
import net.a.g.excel.model.ExcelCell;
import net.a.g.excel.model.ExcelResource;
import net.a.g.excel.model.ExcelSheet;
import net.a.g.excel.repository.ExcelRepository;
import net.a.g.excel.repository.ExcelRepositoryImpl;
import net.a.g.excel.util.ExcelConfiguration;

@ExtendWith(WeldJunit5Extension.class)
public class EngineTest {

	@WeldSetup
	public WeldInitiator weld = WeldInitiator
			.from(ExcelEngineImpl.class, ExcelConfiguration.class, ExcelLoader.class, ExcelRepositoryImpl.class)
			.activate(RequestScoped.class, SessionScoped.class).build();;

	@Inject
	ExcelEngine engine;

	@Inject
	ExcelLoader loader;
	
	@Inject
	ExcelRepository repo;

	@BeforeEach
	public void setup() {
		assertNotNull(engine);
		InputStream inputStream = EngineTest.class.getResourceAsStream("/KYC.xlsx");
		assertTrue(loader.injectResource("KYC", null, inputStream));
		assertEquals(1, repo.count());

	}

	@AfterEach
	public void close() {
		repo.purge();
		assertEquals(0, repo.count());
	}

	@Test
	public void testEngine() {
		assertNotNull(engine);
	}

	@Test
	public void testTest() {
		assertEquals(1, repo.count());
	}

	@Test
	public void testResourceKYC() {
		List<String> actual = new ArrayList(
				repo.listOfResources().stream().map(ExcelResource::getName).collect(Collectors.toList()));

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
				false, false);
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
				false, false);
		assertThat(map, hasSize(1));

		assertThat(map.get(0).getValue(), is("2021-02-12"));
	}

	@Test
	public void testComputeKYCC10Compute43() {

		List<ExcelCell> map = engine.cellCalculation("KYC", List.of("ComputeKYC!C10"), Map.of("ComputeKYC!C9", "43"),
				false, false);
		assertThat(map, hasSize(1));

		assertThat(map.get(0).getValue(), is("2021-02-13"));
	}

	@Test
	public void testLoading() {
		InputStream inputStream = EngineTest.class.getResourceAsStream("/KYC.xlsx");
		assertTrue(loader.injectResource("newtest", null, inputStream));
		assertEquals(2, repo.count());
	}

	@Test
	public void testComputeKYCC6Compute_EmptyInput() {

		Map<String, String> input = Map.of();

		List<ExcelCell> list = engine.cellCalculation("KYC", Arrays.asList("ComputeKYC!C6"), input, false, false);
		assertThat(list, hasSize(1));

		assertThat(list.get(0).getValue(), is("SUM(C2:C4)"));

	}

	@Test
	public void testComputeKYCC6Compute_EmptyInput_force() {

		Map<String, String> input = Map.of();

		List<ExcelCell> list = engine.cellCalculation("KYC", Arrays.asList("ComputeKYC!C6"), input, false, true);
		assertThat(list, hasSize(1));

		assertThat(list.get(0).getValue(), is(0.0));

	}

	@Test
	public void testComputeKYCC6ComputeB2() {

		Map<String, String> input = Map.of("ComputeKYC!B2", "TRUE");

		List<ExcelCell> list = engine.cellCalculation("KYC", Arrays.asList("ComputeKYC!C6"), input, false, false);
		assertThat(list, hasSize(1));

		assertThat(list.get(0).getValue(), is(50.0));
	}

	@Test
	public void testComputeKYCC6ComputeB2B3() {

		Map<String, String> input = Map.of("ComputeKYC!B2", "TRUE", "ComputeKYC!B3", "CY");

		List<ExcelCell> list = engine.cellCalculation("KYC", Arrays.asList("ComputeKYC!C6"), input, false, false);
		assertThat(list, hasSize(1));

		assertThat(list.get(0).getValue(), is(75.0));
	}

	@Test
	public void testComputeKYCC6Compute_B2_B3_B4() {

		Map<String, String> input = Map.of("ComputeKYC!B2", "TRUE", "ComputeKYC!B3", "CY", "ComputeKYC!B4", "1000000");

		List<ExcelCell> list = engine.cellCalculation("KYC", Arrays.asList("ComputeKYC!C6"), input, false, false);
		assertThat(list, hasSize(1));

		assertThat(list.get(0).getValue(), is(125.0));
	}

}