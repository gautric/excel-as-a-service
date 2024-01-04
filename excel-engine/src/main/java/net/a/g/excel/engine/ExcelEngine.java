package net.a.g.excel.engine;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.apache.poi.ss.usermodel.Cell;

import net.a.g.excel.model.ExcelCell;
import net.a.g.excel.model.ExcelSheet;

public interface ExcelEngine {

	List<ExcelSheet> listOfSheet(String name);

	ExcelSheet getSheet(String resource, String sheet);

	boolean isSheetExists(String resource, String sheetName);

	Map<String, ExcelCell> mapOfFormularCell(String excelName, String sheetName);

	Map<String, ExcelCell> mapOfCell(String resource, String sheet, Predicate<Cell> predicate);

	List<ExcelCell> listOfInput(String resource, String sheet);

	List<ExcelCell> listOfOutput(String resource, String sheet);

	List<ExcelCell> listOfAPI(String resource, String sheet);

	Map<String, List<ExcelCell>> mapOfAPI(String resource, String sheet);

	List<ExcelCell> listOfCell(String excelName, String sheetName, Predicate<Cell> predicate);

	Map<String, ExcelCell> mapOfCellCalculated(String resource, String sheet, String[] outputs,
			Map<String, List<String>> inputs, boolean global);

	List<ExcelCell> cellCalculation(String resource, String sheet, List<String> output, Map<String, String> input,
			boolean global);

	List<ExcelCell> cellCalculation(String resource, String sheet, List<String> output, Map<String, String> input,
			boolean global, boolean force);

	List<ExcelCell> cellCalculation(String resource, List<String> outputs);

	List<ExcelCell> cellCalculation(String resource, List<String> outputs, boolean global);

	List<ExcelCell> cellCalculation(String resource, List<String> outputs, Map<String, String> inputs);

	List<ExcelCell> cellCalculation(String resource, List<String> outputs, Map<String, String> inputs, boolean global,
			boolean force);

	List<ExcelCell> cellCalculation(Supplier<String> resource, Supplier<List<String>> outputs,
			Supplier<Map<String, String>> inputs, boolean global);

}