package net.a.g.excel.engine;


import static java.util.stream.Collectors.*;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;

import org.apache.commons.io.FileUtils;
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
public class EngineAPITest {
	
	@WeldSetup
	public WeldInitiator weld = WeldInitiator.from(ExcelEngine.class, ExcelConfiguration.class, ExcelLoader.class)
			.activate(RequestScoped.class, SessionScoped.class).build();;

	@Inject
	ExcelEngine engine;

	@Inject
	ExcelLoader loader;

	@BeforeEach
	public void setup() throws Exception {
		assertNotNull(engine);

		assertTrue(
				loader.injectResource("KYC", "KYC.xlsx", FileUtils.openInputStream(new File("../sample/KYCAPI.xlsx"))));

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
		List<String> actual = engine.lisfOfResource().stream().map(ExcelResource::getName).collect(Collectors.toList());

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
	public void testComputeKYCC6() {

		Map<String, ExcelCell> map = engine.mapOfCellCalculated("KYC", "ComputeKYC", new String[] { "C6" }, null,
				false);
		List<String> actual = new ArrayList(map.values());
		assertThat(actual, hasSize(1));

		assertThat(map.get("ComputeKYC!C6").getValue(), is("SUM(C2:C4)"));
	}

	@Test
	public void testLoading() {
		InputStream inputStream = EngineAPITest.class.getResourceAsStream("/KYC.xlsx");
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
	
	@Test
	public void testListOfInput() {

		List<ExcelCell> list = engine.listOfInput("KYC", "ComputeKYC");
		assertThat(list, hasSize(3));
		List<String> expect = Arrays.asList("@input(PEP)", "@input(COUNTRY)","@input(AMOUNT)");
		assertThat(list.stream().map(ExcelCell::getMetadata).collect(toList()), is(expect));
	}

	@Test
	public void testListOfOutput() {

		List<ExcelCell> list = engine.listOfOutput("KYC", "ComputeKYC");
		assertThat(list, hasSize(1));
		List<String> expect = Arrays.asList("@output(SCORE)");
		assertThat(list.stream().map(ExcelCell::getMetadata).collect(toList()), is(expect));
	}
	
	@Test
	public void testListOfAPI() {

		List<ExcelCell> list = engine.listOfAPI("KYC", "ComputeKYC");
		assertThat(list, hasSize(4));
		List<String> expect = Arrays.asList("@input(PEP)", "@input(COUNTRY)","@input(AMOUNT)","@output(SCORE)");
		assertThat(list.stream().map(ExcelCell::getMetadata).collect(toList()), is(expect));
	}
	
}

