package net.a.g.excel.engine;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.a.g.excel.model.ExcelCell;
import net.a.g.excel.model.ExcelResource;
import net.a.g.excel.model.ExcelSheet;

/**
 * Core test suite for Excel engine functionality using KYC (Know Your Customer) test cases.
 * Tests basic Excel operations including:
 * - Resource management
 * - Sheet operations
 * - Cell calculations
 * - Formula evaluations
 * - Value lookups
 * 
 * Uses a KYC.xlsx test file that contains:
 * - ComputeKYC sheet: Main calculation sheet
 * - COUNTRY sheet: Lookup table for country data
 * - AMOUNT sheet: Lookup table for amount thresholds
 */
@DisplayName("Test Excel KYC")
public class EngineTestCore extends ExcelUnitTest {

    /**
     * Sets up the test environment.
     * Loads the KYC.xlsx test file into the repository.
     *
     * @throws MalformedURLException if resource URLs are invalid
     * @throws IOException if there are issues reading the test file
     */
    @BeforeEach
    public void setup() throws MalformedURLException, IOException {
		super.setup();
		assertNotNull(engine);

		InputStream inputStream = EngineTestCore.class.getResourceAsStream("/KYC.xlsx");
		assertTrue(loader.injectResource("KYC", null, inputStream));
		assertEquals(1, repo.count());
	}

	@AfterEach
	public void close() {
		repo.purge();
		assertEquals(0, repo.count());
	}

    /**
     * Verifies that the Excel engine is properly initialized.
     */
    @Test
    public void testEngine() {
		assertNotNull(engine);
	}

	@Test
	public void testTest() {
		assertEquals(1, repo.count());
	}

    /**
     * Tests that the KYC resource is properly loaded in the repository.
     * Verifies the resource name matches expectations.
     */
    @Test
    public void testResourceKYC() {
		List<String> actual = new ArrayList(
				repo.listOfResources().stream().map(ExcelResource::getName).collect(Collectors.toList()));

		assertThat(actual, hasSize(1));
		List<String> expect = Arrays.asList("KYC");

		assertThat(actual, is(expect));
	}

    /**
     * Tests that all expected sheets are present in the KYC workbook.
     * Verifies the presence and names of ComputeKYC, COUNTRY, and AMOUNT sheets.
     */
    @Test
    public void testSheetKYC() {

		List<String> actual = engine.listOfSheet("KYC").stream().map(ExcelSheet::getName).collect(Collectors.toList());
		assertThat(actual, hasSize(3));

		List<String> expect = Arrays.asList("ComputeKYC", "COUNTRY", "AMOUNT");
		assertThat(actual, is(expect));
	}

    /**
     * Tests formula cell values in the ComputeKYC sheet.
     * Verifies VLOOKUP formulas, SUM functions, and conditional logic.
     */
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
		InputStream inputStream = EngineTestCore.class.getResourceAsStream("/KYC.xlsx");
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

    /**
     * Tests cell calculation with PEP (Politically Exposed Person) flag set to TRUE.
     * Verifies the score calculation when only the PEP input is provided.
     */
    @Test
    public void testComputeKYCC6ComputeB2() {

		Map<String, String> input = Map.of("ComputeKYC!B2", "TRUE");

		List<ExcelCell> list = engine.cellCalculation("KYC", Arrays.asList("ComputeKYC!C6"), input, false, false);
		assertThat(list, hasSize(1));

		assertThat(list.get(0).getValue(), is(50.0));
	}

    /**
     * Tests cell calculation with both PEP flag and country code.
     * Verifies the score calculation when both PEP and country inputs are provided.
     */
    @Test
    public void testComputeKYCC6ComputeB2B3() {

		Map<String, String> input = Map.of("ComputeKYC!B2", "TRUE", "ComputeKYC!B3", "CY");

		List<ExcelCell> list = engine.cellCalculation("KYC", Arrays.asList("ComputeKYC!C6"), input, false, false);
		assertThat(list, hasSize(1));

		assertThat(list.get(0).getValue(), is(75.0));
	}

    /**
     * Tests complete KYC calculation with all inputs:
     * - PEP status
     * - Country code
     * - Transaction amount
     * Verifies the final risk score calculation.
     */
    @Test
    public void testComputeKYCC6Compute_B2_B3_B4() {

		Map<String, String> input = Map.of("ComputeKYC!B2", "TRUE", "ComputeKYC!B3", "CY", "ComputeKYC!B4", "1000000");

		List<ExcelCell> list = engine.cellCalculation("KYC", Arrays.asList("ComputeKYC!C6"), input, false, false);
		assertThat(list, hasSize(1));

		assertThat(list.get(0).getValue(), is(125.0));
	}

}
