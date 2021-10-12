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

import net.a.g.excel.util.ExcelConfiguration;

@ExtendWith(WeldJunit5Extension.class)
public class EngineTest {

	@WeldSetup
	public WeldInitiator weld = WeldInitiator.from(ExcelEngine.class, ExcelConfiguration.class)
			.activate(RequestScoped.class, SessionScoped.class).build();;

	@Inject
	ExcelEngine engine;

	@BeforeEach
	public void setup() {
		assertNotNull(engine);
		InputStream inputStream = EngineTest.class.getResourceAsStream("/KYC.xlsx");
		assertTrue(engine.addFile("KYC", inputStream));

	}

	@AfterEach
	public void close() {
		engine.clearAll();
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
		List<String> actual = new ArrayList(engine.listOfFile());

		assertThat(actual, hasSize(1));
		List<String> expect = Arrays.asList("KYC");

		assertThat(actual, is(expect));
	}

	@Test
	public void testSheetKYC() {

		List<String> actual = engine.listOfSheet("KYC");
		assertThat(actual, hasSize(3));

		List<String> expect = Arrays.asList("ComputeKYC", "COUNTRY", "AMOUNT");
		assertThat(actual, is(expect));
	}
	
	
	@Test
	public void testComputeKYCValues() {
		
		List<String> actual = new ArrayList(engine.cellFormular("KYC", "ComputeKYC").values());
		assertThat(actual, hasSize(4));

		List<String> expect = Arrays.asList("VLOOKUP(B3,COUNTRY!A1:B5,2,FALSE)", "VLOOKUP(B4,AMOUNT!A1:B5,2,TRUE)", "SUM(C2:C4)", "IF(B2,50,0)");
		assertThat(actual, is(expect));
	}

	
	@Test
	public void testComputeKYCKeys() {
		
		List<String> actual = new ArrayList(engine.cellFormular("KYC", "ComputeKYC").keySet());
		assertThat(actual, hasSize(4));

		List<String> expect = Arrays.asList("C3", "C4", "C6", "C2");
		assertThat(actual, is(expect));
	}
	
}