package net.a.g.excel.engine.cdi;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import net.a.g.excel.exception.ExcelInvalidInputException;
import net.a.g.excel.model.ExcelCell;
import net.a.g.excel.param.ExcelParameter;
import net.a.g.excel.param.ExcelParameter.EXCELRETURN;
import net.a.g.excel.util.ExcelConstants;

@DisplayName("ExcelParameter configuration through CDI")
public class ParameterConfigTest extends CdiTestSupport {

	@BeforeEach
	void loadWorkbooks() {
		load("Type", "Type.xlsx");
		// test copy of KYC: B9 is a date (2021-01-01), C9 a number, C10 = B9+C9 date-formatted
		load("KYCD", Path.of("src", "test", "resources", "KYC.xlsx"));
	}

	@Test
	void defaultsWhenNothingConfigured() {
		assertEquals(EXCELRETURN.MAP, param.getRetourFormat());
		assertTrue(param.returnMap());
		assertFalse(param.returnList());
		assertEquals(ExcelConstants.FORMAT_DATE_ISO, param.getFormatDate());
		assertFalse(param.isHideFormular());
	}

	@Test
	void configuredDefaultReadOnlyIsApplied() {
		assertTrue(param.isReadOnly());
	}

	@ParameterizedTest
	@EnumSource(EXCELRETURN.class)
	void returnFormatDrivesListAndMapFlags(EXCELRETURN format) {
		param.setRetourFormat(format);
		assertEquals(format, param.getRetourFormat());
		assertEquals(format, param.getFormatRetour());
		assertEquals(format == EXCELRETURN.LIST, param.returnList());
		assertEquals(format == EXCELRETURN.MAP, param.returnMap());
	}

	@Test
	void toStringReflectsEverySetter() {
		param.setReadOnly(true);
		param.setRetourFormat(EXCELRETURN.LIST);
		param.setFormatDate("dd/MM/yyyy");
		param.setHideFormular(true);
		String s = param.toString();
		assertThat(s, containsString("readOnly=true"));
		assertThat(s, containsString("retourFormat=LIST"));
		assertThat(s, containsString("formatDate=dd/MM/yyyy"));
		assertThat(s, containsString("hideFormular=true"));
	}

	@Test
	void parameterIsOneApplicationScopedInstance() {
		param.setFormatDate("yyyyMMdd");
		assertEquals("yyyyMMdd", weld.select(ExcelParameter.class).get().getFormatDate());
	}

	@ParameterizedTest(name = "format ''{0}'' renders Type!A5 as ''{1}''")
	@CsvSource({ "dd/MM/yyyy,01/01/2021", "yyyyMMdd,20210101",
			"yyyy-MM-dd'T'HH:mm,2021-01-01T00:00" })
	void engineRendersDatesWithConfiguredFormat(String format, String expected) {
		param.setFormatDate(format);
		List<ExcelCell> cells = engine.cellCalculation("Type", List.of("Sheet1!A5"));
		assertThat(cells.get(0).getValue(), is(expected));
		assertThat(cells.get(0).getType(), is("DATE"));
	}

	@Test
	void dateInputIsParsedWithConfiguredFormat() {
		param.setFormatDate("dd/MM/yyyy");
		List<ExcelCell> cells = engine.cellCalculation("KYCD", List.of("ComputeKYC!C10"),
				Map.of("ComputeKYC!B9", "15/03/2021", "ComputeKYC!C9", "1"));
		assertThat(cells.get(0).getValue(), is("16/03/2021"));
	}

	@Test
	void dateInputNotMatchingFormatIsATypedError() {
		param.setFormatDate("dd/MM/yyyy");
		ExcelInvalidInputException ex = assertThrows(ExcelInvalidInputException.class,
				() -> engine.cellCalculation("KYCD", List.of("ComputeKYC!C10"), Map.of("ComputeKYC!B9", "2021-03-15x")));
		assertEquals("B9", ex.getInputName());
		assertThat(ex.getMessage(), containsString("dd/MM/yyyy"));
	}
}
