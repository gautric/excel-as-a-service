package net.a.g.excel.param;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import net.a.g.excel.param.ExcelParameter.EXCELRETURN;

public class ExcelParameterImplTest {

	@Test
	void defaults() {
		ExcelParameterImpl p = new ExcelParameterImpl();
		assertFalse(p.isReadOnly());
		assertEquals(EXCELRETURN.MAP, p.getRetourFormat());
		assertEquals("yyyy-MM-dd", p.getFormatDate());
		assertFalse(p.isHideFormular());
		assertTrue(p.returnMap());
		assertFalse(p.returnList());
	}

	@Test
	void deprecatedGetterMatchesRetourFormat() {
		ExcelParameterImpl p = new ExcelParameterImpl();
		p.setRetourFormat(EXCELRETURN.LIST);
		assertEquals(p.getRetourFormat(), p.getFormatRetour());
	}

	@ParameterizedTest
	@CsvSource({ "LIST,true,false", "MAP,false,true" })
	void returnFlagsFollowFormat(EXCELRETURN format, boolean list, boolean map) {
		ExcelParameterImpl p = new ExcelParameterImpl();
		p.setRetourFormat(format);
		assertEquals(list, p.returnList());
		assertEquals(map, p.returnMap());
	}

	@Test
	void nullFormatIsNeitherListNorMap() {
		ExcelParameterImpl p = new ExcelParameterImpl();
		p.setRetourFormat(null);
		assertNull(p.getRetourFormat());
		assertFalse(p.returnList());
		assertFalse(p.returnMap());
	}

	@Test
	void setters() {
		ExcelParameterImpl p = new ExcelParameterImpl();
		p.setReadOnly(true);
		p.setFormatDate("dd/MM/yyyy");
		p.setHideFormular(true);
		assertTrue(p.isReadOnly());
		assertEquals("dd/MM/yyyy", p.getFormatDate());
		assertTrue(p.isHideFormular());
	}

	@Test
	void toStringReflectsState() {
		ExcelParameterImpl p = new ExcelParameterImpl();
		assertEquals("ExcelParameterImpl [readOnly=false, retourFormat=MAP, formatDate=yyyy-MM-dd, hideFormular=false]",
				p.toString());
		p.setReadOnly(true);
		p.setRetourFormat(EXCELRETURN.LIST);
		p.setHideFormular(true);
		assertEquals("ExcelParameterImpl [readOnly=true, retourFormat=LIST, formatDate=yyyy-MM-dd, hideFormular=true]",
				p.toString());
	}

	@Test
	void enumValues() {
		assertEquals(EXCELRETURN.LIST, EXCELRETURN.valueOf("LIST"));
		assertEquals(EXCELRETURN.MAP, EXCELRETURN.valueOf("MAP"));
	}
}
