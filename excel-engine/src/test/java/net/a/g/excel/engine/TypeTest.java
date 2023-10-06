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
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import net.a.g.excel.load.ExcelLoader;
import net.a.g.excel.model.ExcelCell;
import net.a.g.excel.util.ExcelConfiguration;

@ExtendWith(WeldJunit5Extension.class)
@org.junit.jupiter.api.Disabled
public class TypeTest {

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
		assertTrue(
				loader.injectResource("Type", "Type.xlsx", FileUtils.openInputStream(new File("../sample/Type.xlsx"))));

		assertEquals(1, engine.countListOfResource());

	}

	@AfterEach
	public void close() {
		engine.clearAllResource();
		assertEquals(0, engine.countListOfResource());
	}

	@ParameterizedTest
	@MethodSource("testCellwithParamValue")
	void testCellwithParam(String input, Object value, String type) {

		List<ExcelCell> map = engine.cellCalculation("Type", List.of("Sheet1!" + input));
		assertThat(map, hasSize(1));

		assertThat(map.get(0).getValue(), is(value));
		assertThat(map.get(0).getType(), is(type));

	}

	// @formatter:off
	private static Stream<Arguments> testCellwithParamValue() {
		return Stream.of(
				Arguments.of("A1", 0.0, "NUMERIC"), 
				Arguments.of("A2", 1.0, "NUMERIC"),
				Arguments.of("A3", "A", "STRING"), 
				Arguments.of("A4", "B", "STRING"),
				Arguments.of("A5", "2021-01-01", "DATE"), 
				Arguments.of("A6", 123.0, "NUMERIC"),
				Arguments.of("A7", "Hello ", "STRING"), 
				Arguments.of("A8", "A1:A2", "FORMULA"),
				Arguments.of("A9", "A1+A2+41", "FORMULA")

		);
	}
	// @formatter:on
	@ParameterizedTest
	@MethodSource("testCellwithParamValueCall")
	void testCellwithParamCall(String input, Object value, String type) {

		List<ExcelCell> map = engine.cellCalculation("Type", List.of("Sheet1!" + input), Map.of("Sheet1!B1", "LBLLB"));
		assertThat(map, hasSize(1));

		assertThat(map.get(0).getValue(), is(value));
		assertThat(map.get(0).getType(), is(type));

	}

	// @formatter:off
	private static Stream<Arguments> testCellwithParamValueCall() {
		return Stream.of(
				Arguments.of("A1", 0.0, "NUMERIC"), 
				Arguments.of("A2", 1.0, "NUMERIC"),
				Arguments.of("A3", "A", "STRING"), 
				Arguments.of("A4", "B", "STRING"),
				Arguments.of("A5", "2021-01-01", "DATE"), 
				Arguments.of("A6", 123.0, "NUMERIC"),
				Arguments.of("A7", "Hello ", "STRING"), 
				Arguments.of("A8", "#ERROR", "ERROR"),
				Arguments.of("A9", 42.0, "NUMERIC")

		);
	}
	// @formatter:on
}