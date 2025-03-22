package net.a.g.excel.engine;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.apache.poi.ss.usermodel.Cell;

import net.a.g.excel.model.ExcelCell;
import net.a.g.excel.model.ExcelSheet;

/**
 * Core interface for Excel operations and calculations.
 * This interface provides methods to interact with Excel workbooks, sheets, and cells,
 * including formula evaluation and cell calculations.
 */
public interface ExcelEngine {

    /**
     * Lists all sheets in the specified Excel resource.
     *
     * @param name the name of the Excel resource
     * @return list of Excel sheets
     */
    List<ExcelSheet> listOfSheet(String name);

    /**
     * Retrieves a specific sheet from an Excel resource.
     *
     * @param resource the name of the Excel resource
     * @param sheet the name of the sheet to retrieve
     * @return the requested Excel sheet, or null if not found
     */
    ExcelSheet getSheet(String resource, String sheet);

    /**
     * Checks if a sheet exists in the specified Excel resource.
     *
     * @param resource the name of the Excel resource
     * @param sheetName the name of the sheet to check
     * @return true if the sheet exists, false otherwise
     */
    boolean isSheetExists(String resource, String sheetName);

    /**
     * Returns a map of cells containing formulas in the specified sheet.
     *
     * @param excelName the name of the Excel resource
     * @param sheetName the name of the sheet
     * @return map of cell references to formula cells
     */
    Map<String, ExcelCell> mapOfFormularCell(String excelName, String sheetName);

    /**
     * Returns a map of cells that match the specified predicate.
     *
     * @param resource the name of the Excel resource
     * @param sheet the name of the sheet
     * @param predicate the condition cells must satisfy
     * @return map of cell references to matching cells
     */
    Map<String, ExcelCell> mapOfCell(String resource, String sheet, Predicate<Cell> predicate);

    /**
     * Lists all input cells in the specified sheet.
     * Input cells are typically cells that accept user input values.
     *
     * @param resource the name of the Excel resource
     * @param sheet the name of the sheet
     * @return list of input cells
     */
    List<ExcelCell> listOfInput(String resource, String sheet);

    /**
     * Lists all output cells in the specified sheet.
     * Output cells typically contain formulas or calculated values.
     *
     * @param resource the name of the Excel resource
     * @param sheet the name of the sheet
     * @return list of output cells
     */
    List<ExcelCell> listOfOutput(String resource, String sheet);

    /**
     * Lists all API-related cells in the specified sheet.
     *
     * @param resource the name of the Excel resource
     * @param sheet the name of the sheet
     * @return list of API cells
     */
    List<ExcelCell> listOfAPI(String resource, String sheet);

    /**
     * Returns a map of API-related cells grouped by category.
     *
     * @param resource the name of the Excel resource
     * @param sheet the name of the sheet
     * @return map of API categories to their respective cells
     */
    Map<String, List<ExcelCell>> mapOfAPI(String resource, String sheet);

    /**
     * Lists cells that match the specified predicate.
     *
     * @param excelName the name of the Excel resource
     * @param sheetName the name of the sheet
     * @param predicate the condition cells must satisfy
     * @return list of matching cells
     */
    List<ExcelCell> listOfCell(String excelName, String sheetName, Predicate<Cell> predicate);

    /**
     * Calculates and returns a map of cells based on specified outputs and inputs.
     *
     * @param resource the name of the Excel resource
     * @param sheet the name of the sheet
     * @param outputs array of output cell references
     * @param inputs map of input cell references to their values
     * @param global whether to use global scope for calculation
     * @return map of cell references to calculated cells
     */
    Map<String, ExcelCell> mapOfCellCalculated(String resource, String sheet, String[] outputs,
            Map<String, List<String>> inputs, boolean global);

    /**
     * Performs cell calculations based on specified outputs and inputs.
     *
     * @param resource the name of the Excel resource
     * @param sheet the name of the sheet
     * @param output list of output cell references
     * @param input map of input cell references to their values
     * @param global whether to use global scope for calculation
     * @return list of calculated cells
     */
    List<ExcelCell> cellCalculation(String resource, String sheet, List<String> output, Map<String, String> input,
            boolean global);

    /**
     * Performs cell calculations with force option.
     *
     * @param resource the name of the Excel resource
     * @param sheet the name of the sheet
     * @param output list of output cell references
     * @param input map of input cell references to their values
     * @param global whether to use global scope for calculation
     * @param force whether to force recalculation
     * @return list of calculated cells
     */
    List<ExcelCell> cellCalculation(String resource, String sheet, List<String> output, Map<String, String> input,
            boolean global, boolean force);

    /**
     * Performs cell calculations for specified outputs.
     *
     * @param resource the name of the Excel resource
     * @param outputs list of output cell references
     * @return list of calculated cells
     */
    List<ExcelCell> cellCalculation(String resource, List<String> outputs);

    /**
     * Performs cell calculations for specified outputs with global scope option.
     *
     * @param resource the name of the Excel resource
     * @param outputs list of output cell references
     * @param global whether to use global scope for calculation
     * @return list of calculated cells
     */
    List<ExcelCell> cellCalculation(String resource, List<String> outputs, boolean global);

    /**
     * Performs cell calculations with specified inputs.
     *
     * @param resource the name of the Excel resource
     * @param outputs list of output cell references
     * @param inputs map of input cell references to their values
     * @return list of calculated cells
     */
    List<ExcelCell> cellCalculation(String resource, List<String> outputs, Map<String, String> inputs);

    /**
     * Performs cell calculations with all options.
     *
     * @param resource the name of the Excel resource
     * @param outputs list of output cell references
     * @param inputs map of input cell references to their values
     * @param global whether to use global scope for calculation
     * @param force whether to force recalculation
     * @return list of calculated cells
     */
    List<ExcelCell> cellCalculation(String resource, List<String> outputs, Map<String, String> inputs, boolean global,
            boolean force);

    /**
     * Performs cell calculations using suppliers for dynamic resource, outputs, and inputs.
     *
     * @param resource supplier for the Excel resource name
     * @param outputs supplier for the list of output cell references
     * @param inputs supplier for the map of input values
     * @param global whether to use global scope for calculation
     * @return list of calculated cells
     */
    List<ExcelCell> cellCalculation(Supplier<String> resource, Supplier<List<String>> outputs,
            Supplier<Map<String, String>> inputs, boolean global);

}
