package net.a.g.excel.engine;

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
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;
import jakarta.enterprise.inject.spi.CDI;
import net.a.g.excel.load.ExcelLoaderImpl;
import net.a.g.excel.model.ExcelCell;
import net.a.g.excel.repository.ExcelRepository;
import net.a.g.excel.repository.ExcelRepositoryImpl;

@DisplayName("Test with parameterized test")
public class TypeTestCore extends ExcelUnitTest {

	@BeforeEach
	public void setup() throws MalformedURLException, IOException {
		super.setup();
		assertNotNull(engine);

		assertTrue(
				loader.injectResource("Type", "Type.xlsx", FileUtils.openInputStream(new File("../sample/Type.xlsx"))));
		assertEquals(1, repo.count());
	}

	@DisplayName("Should pass a non-null message to our test method")
	@ParameterizedTest(name = "{index} => message=''{0}''")
	@ValueSource(strings = { "Hello", "World" })
	void shouldPassNonNullMessageAsMethodParameter(String message) {
		assertNotNull(message);
	}

	@DisplayName("Test Calculation with Param")
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
	@DisplayName("Test Calculation with Param Call")
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