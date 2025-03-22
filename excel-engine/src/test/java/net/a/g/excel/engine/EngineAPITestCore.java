package net.a.g.excel.engine;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.a.g.excel.model.ExcelCell;
import net.a.g.excel.model.ExcelResource;
import net.a.g.excel.model.ExcelSheet;

/**
 * Test suite for Excel engine API functionality using annotated KYC workbook.
 * Tests the handling of cells marked with @input and @output annotations.
 * These annotations are used to identify cells that serve as API endpoints
 * for external interaction with the Excel calculations.
 * 
 * Uses KYCAPI.xlsx test file which contains:
 * - Cells marked with @input for PEP status, country, and amount
 * - Cells marked with @output for risk score results
 * - Demonstrates API-driven Excel calculations
 */
@DisplayName("Test Excel KYC with API annotation")
public class EngineAPITestCore extends ExcelUnitTest {

    /**
     * Sets up the test environment.
     * Loads the KYCAPI.xlsx test file which contains API-annotated cells.
     *
     * @throws MalformedURLException if resource URLs are invalid
     * @throws IOException if there are issues reading the test file
     */
    @BeforeEach
    public void setup() throws MalformedURLException, IOException {
        super.setup();
        assertNotNull(engine);

        assertTrue(
                loader.injectResource("KYC", "KYC.xlsx", FileUtils.openInputStream(new File("../sample/KYCAPI.xlsx"))));

        assertEquals(1, repo.count());
    }

    /**
     * Verifies that the repository is properly initialized.
     */
    @Test
    public void testEngine() {
        assertNotNull(repo);
    }

    /**
     * Verifies the repository contains exactly one resource.
     */
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
        List<String> actual = repo.listOfResources().stream().map(ExcelResource::getName).collect(Collectors.toList());

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
     * Tests the retrieval and formula content of cell C6 in ComputeKYC sheet.
     */
    @Test
    public void testComputeKYCC6() {
        Map<String, ExcelCell> map = engine.mapOfCellCalculated("KYC", "ComputeKYC", new String[] { "C6" }, null,
                false);
        List<String> actual = new ArrayList(map.values());
        assertThat(actual, hasSize(1));

        assertThat(map.get("ComputeKYC!C6").getValue(), is("SUM(C2:C4)"));
    }

    /**
     * Tests the ability to load additional Excel resources into the repository.
     */
    @Test
    public void testLoading() {
        InputStream inputStream = EngineAPITestCore.class.getResourceAsStream("/KYC.xlsx");
        assertTrue(loader.injectResource("newtest", null, inputStream));
        assertEquals(2, repo.count());
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
     * Tests the identification of cells marked with @input annotation.
     * Verifies that all input cells are correctly identified:
     * - PEP status input
     * - Country code input
     * - Amount input
     */
    @Test
    public void testListOfInput() {
        List<ExcelCell> list = engine.listOfInput("KYC", "ComputeKYC");
        assertThat(list, hasSize(3));
        List<String> expect = Arrays.asList("@input(PEP)", "@input(COUNTRY)", "@input(AMOUNT)");
        assertThat(list.stream().map(ExcelCell::getMetadata).collect(toList()), is(expect));
    }

    /**
     * Tests the identification of cells marked with @output annotation.
     * Verifies that the risk score output cell is correctly identified.
     */
    @Test
    public void testListOfOutput() {
        List<ExcelCell> list = engine.listOfOutput("KYC", "ComputeKYC");
        assertThat(list, hasSize(1));
        List<String> expect = Arrays.asList("@output(SCORE)");
        assertThat(list.stream().map(ExcelCell::getMetadata).collect(toList()), is(expect));
    }

    /**
     * Tests the identification of all API-related cells.
     * Verifies that both input and output cells are correctly identified
     * and their annotations are properly parsed.
     */
    @Test
    public void testListOfAPI() {
        List<ExcelCell> list = engine.listOfAPI("KYC", "ComputeKYC");
        assertThat(list, hasSize(4));
        List<String> expect = Arrays.asList("@input(PEP)", "@input(COUNTRY)", "@input(AMOUNT)", "@output(SCORE)");
        assertThat(list.stream().map(ExcelCell::getMetadata).collect(toList()), is(expect));
    }
}
