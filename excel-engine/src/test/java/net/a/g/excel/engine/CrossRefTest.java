package net.a.g.excel.engine;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
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
import net.a.g.excel.model.ExcelSheet;
import net.a.g.excel.util.ExcelConfiguration;

@ExtendWith(WeldJunit5Extension.class)
public class CrossRefTest {

	@WeldSetup
	public WeldInitiator weld = WeldInitiator.from(ExcelEngine.class, ExcelConfiguration.class, ExcelLoader.class)
			.activate(RequestScoped.class, SessionScoped.class).build();;

	@Inject
	ExcelEngine engine;

	@Inject
	ExcelLoader loader;

	@BeforeEach
	public void setup() throws MalformedURLException, IOException {
		assertNotNull(engine);
		assertTrue(loader.injectResource("Primary", "Primary.xlsx",
				FileUtils.openInputStream(new File("../sample/Primary.xlsx"))));
		assertTrue(loader.injectResource("Secondary", "Secondary.xlsx",
				FileUtils.openInputStream(new File("../sample/Secondary.xlsx"))));

		assertEquals(2, engine.countListOfResource());

	}

	@AfterEach
	public void close() {
		engine.clearAllResource();
		assertEquals(0, engine.countListOfResource());
	}

	@Test
	public void testSheetPrimary() {

		List<String> actual = engine.listOfSheet("Primary").stream().map(ExcelSheet::getName)
				.collect(Collectors.toList());
		assertNotNull(actual);

		assertThat(actual, hasSize(1));

		List<String> expect = Arrays.asList("Feuil1");
		assertThat(actual, is(expect));
	}

	@Test
	public void testSheetSecondary() {

		List<String> actual = engine.listOfSheet("Secondary").stream().map(ExcelSheet::getName)
				.collect(Collectors.toList());
		assertNotNull(actual);

		assertThat(actual, hasSize(1));

		List<String> expect = Arrays.asList("Feuil1");
		assertThat(actual, is(expect));
	}

	@Test
	public void testSheetPrimaryV() {

		List<String> actual = new ArrayList(engine.mapOfFormularCell("Primary", "Feuil1").values().stream()
				.map(cell -> cell.getValue()).collect(Collectors.toList()));
		assertNotNull(actual);
		assertThat(actual, hasSize(2));

		List<String> expect = Arrays.asList("[1]Feuil1!$A$1+[1]Feuil1!$B$1+[1]Feuil1!$C$1", "A1+2+C1");
		assertThat(actual, is(expect));
	}

	@Test
	public void testSheetPrimaryK() {

		List<String> actual = new ArrayList(engine.mapOfFormularCell("Primary", "Feuil1").keySet());
		assertNotNull(actual);
		assertThat(actual, hasSize(2));

		List<String> expect = Arrays.asList("A1", "B1");
		assertThat(actual, is(expect));
	}

	@Test
	public void testSheetPrimaryCall() {

		List<String> actual = new ArrayList(engine.mapOfFormularCell("Primary", "Feuil1").keySet());
		assertNotNull(actual);
		assertThat(actual, hasSize(2));

		List<String> expect = Arrays.asList("A1", "B1");
		assertThat(actual, is(expect));

		Map<String, ExcelCell> toto = engine.mapOfCellCalculated("Primary", "Feuil1", new String[] { "B1" },
				Map.of("C1", List.of("10")), true);

		assertEquals(54.0, toto.get("Feuil1!B1").getValue());

	}

	@Test
	public void testSheetPrimaryCallList() {

		List<String> actual = new ArrayList(engine.mapOfFormularCell("Primary", "Feuil1").keySet());
		assertNotNull(actual);
		assertThat(actual, hasSize(2));

		List<String> expect = Arrays.asList("A1", "B1");
		assertThat(actual, is(expect));

		List<ExcelCell> result = engine.cellCalculation("Primary", List.of("Feuil1!B1"), Map.of("Feuil1!C1", "10"),
				true, false);

		assertEquals(54.0, result.get(0).getValue());

	}

}
