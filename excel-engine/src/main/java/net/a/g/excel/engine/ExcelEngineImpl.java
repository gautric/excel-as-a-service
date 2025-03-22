package net.a.g.excel.engine;

import static java.util.stream.Collectors.toList;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.a.g.excel.model.ExcelCell;
import net.a.g.excel.model.ExcelResource;
import net.a.g.excel.model.ExcelSheet;
import net.a.g.excel.param.ExcelParameter;
import net.a.g.excel.repository.ExcelRepository;
import net.a.g.excel.util.POITools;

/**
 * Implementation of the ExcelEngine interface that provides Excel workbook operations
 * using Apache POI. This class handles Excel file operations, cell manipulations,
 * formula evaluations, and cell value calculations.
 * 
 * @see ExcelEngine
 * @see org.apache.poi.ss.usermodel.Workbook
 */
public class ExcelEngineImpl implements ExcelEngine {

	private static final Logger LOG = LoggerFactory.getLogger(ExcelEngineImpl.class);

	/** Parameter configuration for Excel operations */
	private ExcelParameter param;

	/** Repository for storing and retrieving Excel resources */
	private ExcelRepository repo;

	/**
	 * Sets the parameter configuration for Excel operations.
	 * 
	 * @param param the Excel parameter configuration
	 */
	public void setParameter(ExcelParameter param) {
		this.param = param;
	}

	/**
	 * Sets the repository for Excel resources.
	 * 
	 * @param repo the Excel repository implementation
	 */
	public void setRepository(ExcelRepository repo) {
		this.repo = repo;
	}

	/**
	 * Creates a formula evaluator for the given workbook.
	 * 
	 * @param wb the workbook to create an evaluator for
	 * @return a formula evaluator for the workbook
	 */
	private FormulaEvaluator formula(Workbook wb) {
		return wb.getCreationHelper().createFormulaEvaluator();
	}

	public List<ExcelSheet> listOfSheet(String name) {
		List<ExcelSheet> list = null;

		if (repo.contains(name)) {

			list = StreamSupport.stream(repo.retrieveWorkbook(name).spliterator(), false).map(Sheet::getSheetName)
					.map(ExcelSheet::new).collect(toList());

		}
		return list;
	}

	public ExcelSheet getSheet(String resource, String sheet) {
		return (isSheetExists(resource, sheet)) ? new ExcelSheet(sheet) : null;
	}

	public boolean isSheetExists(String resource, String sheetName) {
		Workbook workbook = repo.retrieveWorkbook(resource);
		Sheet sheet = workbook.getSheet(sheetName);
		return sheet != null;
	}

	public Map<String, ExcelCell> mapOfFormularCell(String excelName, String sheetName) {
		return mapOfCell(excelName, sheetName, cell -> CellType.FORMULA == cell.getCellType());
	}

	public Map<String, ExcelCell> mapOfCell(String resource, String sheet, Predicate<Cell> predicate) {
		Workbook workbook = repo.retrieveWorkbook(resource);

		return streamOfCell(workbook, sheet, predicate)
				.collect(Collectors.toMap(cell -> cell.getAddress().formatAsString(), this::celltoExcelCell));
	}

	public List<ExcelCell> listOfInput(String resource, String sheet) {
		return listOfCell(resource, sheet, cell -> {
			String content = getCellComment(cell);
			return content != null && content.contains("@input");
		});
	}

	public List<ExcelCell> listOfOutput(String resource, String sheet) {
		return listOfCell(resource, sheet, cell -> {
			String content = getCellComment(cell);
			return content != null && content.contains("@output");
		});
	}

	public List<ExcelCell> listOfAPI(String resource, String sheet) {
		return listOfCell(resource, sheet, cell -> {
			String content = getCellComment(cell);
			return content != null && (content.contains("@output") || content.contains("@input"));
		});
	}

	public Map<String, List<ExcelCell>> mapOfAPI(String resource, String sheet) {
		return listOfAPI(resource, sheet).stream()
				.collect(Collectors.groupingBy(v -> (v.getMetadata().contains("@input")) ? "IN" : "OUT"));
	}

	public List<ExcelCell> listOfCell(String excelName, String sheetName, Predicate<Cell> predicate) {
		Workbook workbook = repo.retrieveWorkbook(excelName);

		return streamOfCell(workbook, sheetName, predicate).map(this::celltoExcelCell).collect(Collectors.toList());
	}

	/**
	 * Creates a stream of cells from a workbook sheet that match the given predicate.
	 * 
	 * @param workbook the workbook to stream cells from
	 * @param sheetName the name of the sheet to process
	 * @param predicate the condition cells must satisfy
	 * @return stream of matching cells
	 */
	private Stream<Cell> streamOfCell(Workbook workbook, String sheetName, Predicate<Cell> predicate) {
		return streamCell(workbook, sheetName).filter(predicate);
	}

	public Map<String, ExcelCell> mapOfCellCalculated(String resource, String sheet, String[] outputs,
			Map<String, List<String>> inputs, boolean global) {

		// Compute All cellNames
		return cellCalculationOld(resource, sheet, Arrays.asList(outputs), inputs, global).stream()
				.collect(Collectors.toMap(ExcelCell::getAddress, Function.identity()));

	}

	public List<ExcelCell> cellCalculationOld(String resource, String sheet, List<String> outputs,
			Map<String, List<String>> inputs, boolean global) {

		inputs = (inputs == null) ? Map.of() : inputs;

		return cellCalculation(resource, sheet, outputs,
				inputs.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0))),
				global);

	}

	public List<ExcelCell> cellCalculation(String resource, String sheet, List<String> output,
			Map<String, String> input, boolean global) {

		return cellCalculation(resource, sheet, output, input, global, false);
	}

	public List<ExcelCell> cellCalculation(String resource, String sheet, List<String> output,
			Map<String, String> input, boolean global, boolean force) {

		Function<String, String> renameFunction = cn -> cn.contains("!") ? cn : sheet + "!" + cn;

		return cellCalculation(resource, output.stream().map(renameFunction).collect(toList()), input.entrySet()
				.stream().collect(Collectors.toMap(e -> renameFunction.apply(e.getKey()), Map.Entry::getValue)), global,
				force);
	}

	public List<ExcelCell> cellCalculation(String resource, List<String> outputs) {
		return cellCalculation(resource, outputs, Map.<String, String>of(), false, false);

	}

	public List<ExcelCell> cellCalculation(String resource, List<String> outputs, boolean global) {
		return cellCalculation(resource, outputs, Map.<String, String>of(), global, false);
	}

	public List<ExcelCell> cellCalculation(String resource, List<String> outputs, Map<String, String> inputs) {
		return cellCalculation(resource, outputs, inputs, false, false);
	}

	public List<ExcelCell> cellCalculation(String resource, List<String> outputs, Map<String, String> inputs,
			boolean global, boolean force) {

		Workbook workbook = repo.retrieveWorkbook(resource);
		FormulaEvaluator exec = formula(workbook);

		if (global && inputs.size() > 0) {
			LOG.debug("mode global aka cross-ref enable");

			Map<String, FormulaEvaluator> workbooks = repo.listOfResource().stream()
					.collect(Collectors.toMap(ExcelResource::getFile, r -> (resource.compareTo(r.getName()) == 0) ? exec
							: formula(POITools.convertByteToWorkbook(r.getDoc()))));
			exec.setupReferencedWorkbooks(workbooks);
		}

		Function<Cell, ExcelCell> execFunction = null;

		inputs.entrySet().stream()
				.map(kv -> Map.entry(retrieveCellByAdress(new CellReference(kv.getKey()), workbook), kv.getValue()))
				.forEach(kv -> updateCell(kv.getKey(), kv.getValue()));

		if (inputs.size() > 0 || force) {
			LOG.debug("some input value || force -> go for cell evaluation");
			execFunction = cell -> computeCell(cell, exec);
		} else {
			LOG.debug("No input value -> no cell evaluation");
			execFunction = cell -> celltoExcelCell(cell);
		}

		LOG.debug("Resource {} ", resource);
		LOG.debug("Inputs: {}", inputs);
		LOG.debug("Outputs: {}", outputs);

		// Compute All cellNames
		List<ExcelCell> ret = outputs.stream().map(CellReference::new).map(cr -> retrieveCellByAdress(cr, workbook))
				.flatMap(Stream::ofNullable).map(execFunction).collect(toList());

		LOG.debug("Computed: {}", ret);

		return ret;
	}

	public List<ExcelCell> cellCalculation(Supplier<String> resource, Supplier<List<String>> outputs,
			Supplier<Map<String, String>> inputs, boolean global) {
		return cellCalculation(resource.get(), outputs.get(), inputs.get(), global, false);
	}

	/**
	 * Retrieves a cell from a workbook using a cell reference.
	 * Creates a blank cell if the specified cell doesn't exist.
	 * 
	 * @param cr the cell reference
	 * @param workbook the workbook to retrieve the cell from
	 * @return the cell at the specified reference, or a new blank cell
	 */
	private Cell retrieveCellByAdress(CellReference cr, Workbook workbook) {
		Cell ret = null;
		Sheet sheet = workbook.getSheet(cr.getSheetName());
		Row row = sheet.getRow(cr.getRow());
		if (row != null) {
			ret = row.getCell(cr.getCol(), MissingCellPolicy.CREATE_NULL_AS_BLANK);
		}
		return ret;
	}

	/**
	 * Gets the raw value from a cell based on its type.
	 * Handles different cell types including:
	 * - Boolean values
	 * - Numeric values (including dates)
	 * - String values
	 * - Blank cells
	 * - Error cells
	 * - Formula cells
	 * 
	 * @param cell the cell to get the value from
	 * @return the cell's raw value as an Object
	 */
	private Object getRawCell(Cell cell) {

		Object ret = "";
		if (cell != null) {
			LOG.debug("Read {}!{} ({}) ", cell.getSheet().getSheetName(), cell.getAddress(), cell.getCellType());

			switch (cell.getCellType()) {
			case BOOLEAN:
				ret = Boolean.toString(cell.getBooleanCellValue());
				break;
			case NUMERIC:

				if (DateUtil.isCellDateFormatted(cell)) {
					SimpleDateFormat sdf = new SimpleDateFormat(param.getFormatDate());
					ret = sdf.format(cell.getDateCellValue());
				} else {
					ret = cell.getNumericCellValue();
				}

				break;
			case STRING:
				ret = "" + cell.getStringCellValue();
				break;
			case BLANK:
				ret = "" + cell.getStringCellValue();
				break;
			case ERROR:
				ret = "#ERROR";
				break;
			case FORMULA:
				ret = cell.getCellFormula();
				break;
			default:
				break;
			}
		}
		return ret;
	}

	/**
	 * Gets the type of a cell as a string.
	 * For numeric cells, distinguishes between regular numbers and dates.
	 * 
	 * @param cell the cell to get the type from
	 * @return string representation of the cell type
	 */
	private String getCellType(Cell cell) {

		String ret = "";
		if (cell != null) {
			LOG.debug("Read Type {}!{} ({}) ", cell.getSheet().getSheetName(), cell.getAddress(), cell.getCellType());

			CellType type = cell.getCellType();
			switch (cell.getCellType()) {

			case NUMERIC:

				if (DateUtil.isCellDateFormatted(cell)) {
					return "DATE";
				} else {
					return type.name();
				}

			case BOOLEAN:
			case STRING:
			case BLANK:
			case ERROR:
			case FORMULA:
				return type.name();
			default:
				break;
			}
		}
		return ret;
	}

	/**
	 * Updates a cell's value based on its type.
	 * Handles type conversion for:
	 * - Boolean values
	 * - Numeric values (including dates)
	 * - String values
	 * - Blank cells
	 * Special handling for formula cells (warns but doesn't modify).
	 * 
	 * @param cell the cell to update
	 * @param value the new value as a string
	 */
	private void updateCell(Cell cell, String value) {

		if (cell != null) {
			LOG.debug("Write {}!{} ({}) = {} ", cell.getSheet().getSheetName(), cell.getAddress(), cell.getCellType(),
					value);

			switch (cell.getCellType()) {
			case BOOLEAN:
				cell.setCellValue(Boolean.parseBoolean(value));
				break;
			case NUMERIC:

				if (DateUtil.isCellDateFormatted(cell)) {
					SimpleDateFormat sdf = new SimpleDateFormat(param.getFormatDate());
					try {
						cell.setCellValue(sdf.parse(value));
					} catch (ParseException ex) {
						LOG.error("", ex);
					}
				} else {
					cell.setCellValue(Double.parseDouble(value));
				}

				break;
			case STRING:
				cell.setCellValue(value);
				break;
			case BLANK:
				cell.setCellValue(value);
				break;
			case ERROR:
				break;
			case FORMULA:
				LOG.warn("Cell at '{}' is a '{}'", cell.getAddress(), CellType.FORMULA);
				break;
			default:
				break;
			}
		}
	}

	/**
	 * Creates a stream of all cells in a specific sheet.
	 * 
	 * @param workbook the workbook containing the sheet
	 * @param sheetName the name of the sheet to stream
	 * @return stream of all cells in the sheet
	 */
	private Stream<Cell> streamCell(Workbook workbook, String sheetName) {
		return streamCell(workbook, s -> s.getSheetName().compareTo(sheetName) == 0);
	}

	/**
	 * Creates a stream of cells from sheets that match the given predicate.
	 * 
	 * @param workbook the workbook to stream cells from
	 * @param predicate the condition sheets must satisfy
	 * @return stream of cells from matching sheets
	 */
	private Stream<Cell> streamCell(Workbook workbook, Predicate<? super Sheet> predicate) {
		return StreamSupport.stream(workbook.spliterator(), false).filter(predicate).flatMap(sheet -> StreamSupport
				.stream(sheet.spliterator(), false).flatMap(r -> StreamSupport.stream(r.spliterator(), false)));
	}

	/**
	 * Gets the comment text from a cell, removing newlines.
	 * 
	 * @param cell the cell to get the comment from
	 * @return the cell's comment text, or null if no comment exists
	 */
	private String getCellComment(Cell cell) {

		String ret = null;
		if (cell.getCellComment() != null) {
			ret = cell.getCellComment().getString().toString().replaceAll("\\n", "");
		}
		return ret;
	}

	/**
	 * Computes a cell's value using a formula evaluator.
	 * 
	 * @param cell the cell to compute
	 * @param exec the formula evaluator to use
	 * @return an ExcelCell containing the computed value
	 */
	private ExcelCell computeCell(Cell cell, FormulaEvaluator exec) {
		return celltoExcelCell(cell, c -> exec.evaluateInCell(c));
	}

	/**
	 * Converts a POI Cell to an ExcelCell without evaluation.
	 * 
	 * @param cell the POI Cell to convert
	 * @return the converted ExcelCell
	 */
	private ExcelCell celltoExcelCell(Cell cell) {
		return celltoExcelCell(cell, Function.identity());
	}

	/**
	 * Converts a POI Cell to an ExcelCell with custom value transformation.
	 * 
	 * @param cell the POI Cell to convert
	 * @param valueFunction function to transform the cell before conversion
	 * @return the converted ExcelCell
	 */
	private ExcelCell celltoExcelCell(Cell cell, Function<Cell, Cell> valueFunction) {
		ExcelCell ret = new ExcelCell();

		ret.setAddress(cell.getSheet().getSheetName() + "!" + cell.getAddress().formatAsString());
		ret.setValue(getRawCell(valueFunction.apply(cell)));
		ret.setType(getCellType(cell));
		ret.setMetadata(getCellComment(cell));
		return ret;
	}

}
