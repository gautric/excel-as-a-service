package net.a.g.excel.engine;

import static java.util.stream.Collectors.toList;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.a.g.excel.model.ExcelCell;
import net.a.g.excel.model.ExcelResource;
import net.a.g.excel.model.ExcelSheet;
import net.a.g.excel.util.ExcelConfiguration;

@ApplicationScoped
@Named
public class ExcelEngine {

	public static final Logger LOG = LoggerFactory.getLogger(ExcelEngine.class);

	private static final Predicate<Cell> FORMULA_PREDICATE = cell -> CellType.FORMULA == cell.getCellType();

	@Inject
	ExcelConfiguration conf;

	private final Function<Cell, ExcelCell> rawMapping = cell -> celltoExcelCell(cell);

	private Map<String, ExcelResource> listOfResources = new HashMap<String, ExcelResource>();

	public boolean addResource(ExcelResource resource) {

		assert (resource != null);

		if (extractedWorkbook(resource.getName(), resource.getDoc()) == null) {
			LOG.error("Workbook {} is not readable", resource.getName());
			return false;
		}
		listOfResources.put(resource.getName(), resource);

		LOG.info("Add {} OK", resource);

		return true;
	}

	public ExcelResource getResource(String resource) {
		return listOfResources.get(resource);
	}

	public void clearAllResource() {
		listOfResources.clear();
	}

	public int countListOfResource() {
		return listOfResources.keySet().size();
	}

	public Collection<ExcelResource> lisfOfResource() {
		return listOfResources.values();
	}

	public boolean isResourceExists(String name) {
		return listOfResources.containsKey(name);
	}

	private Workbook retrieveWorkbook(String name) {
		Workbook workbook = null;
		if (listOfResources.containsKey(name)) {
			byte[] byteArray = listOfResources.get(name).getDoc();
			workbook = convertByteToWorkbook(byteArray);
		}
		return workbook;
	}

	private Workbook convertByteToWorkbook(byte[] byteArray) {
		try {
			return WorkbookFactory.create(new ByteArrayInputStream(byteArray));
		} catch (EncryptedDocumentException ex) {
			LOG.error("Workbook  is not a XSSF file", ex);
		} catch (IOException ex) {
			LOG.error("Workbook  is not a XSSF file", ex);
		}
		return null;
	}

	private Workbook extractedWorkbook(String name, byte[] byteArray) {
		Workbook workbook = null;

		try {
			workbook = WorkbookFactory.create(new ByteArrayInputStream(byteArray));
			return workbook;
		} catch (Exception ex) {
			LOG.error("Workbook '" + name + "' is not a XSSF file", ex);
		}

		return workbook;
	}

	private FormulaEvaluator formula(Workbook wb) {
		return wb.getCreationHelper().createFormulaEvaluator();
	}

	public List<ExcelSheet> listOfSheet(String name) {
		List<ExcelSheet> list = null;

		if (listOfResources.containsKey(name)) {

			list = StreamSupport.stream(retrieveWorkbook(name).spliterator(), false).map(Sheet::getSheetName)
					.map(ExcelSheet::new).collect(toList());

		}
		return list;
	}

	public ExcelSheet getSheet(String resource, String sheet) {
		return (isSheetExists(resource, sheet)) ? new ExcelSheet(sheet) : null;
	}

	public boolean isSheetExists(String resource, String sheetName) {
		Workbook workbook = retrieveWorkbook(resource);
		Sheet sheet = workbook.getSheet(sheetName);
		return sheet != null;
	}

	public Map<String, ExcelCell> mapOfFormularCell(String excelName, String sheetName) {
		return mapOfCell(excelName, sheetName, FORMULA_PREDICATE);
	}

	public Map<String, ExcelCell> mapOfCell(String resource, String sheet, Predicate<Cell> predicate) {
		Workbook workbook = retrieveWorkbook(resource);

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
		Workbook workbook = retrieveWorkbook(excelName);

		return streamOfCell(workbook, sheetName, predicate).map(this::celltoExcelCell).collect(Collectors.toList());
	}

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

		Function<String, String> renameFunction = cn -> cn.contains("!") ? cn : sheet + "!" + cn;

		return cellCalculation(resource, output.stream().map(renameFunction).collect(toList()), input.entrySet()
				.stream().collect(Collectors.toMap(e -> renameFunction.apply(e.getKey()), Map.Entry::getValue)),
				global);
	}

	public List<ExcelCell> cellCalculation(String resource, List<String> outputs) {
		return cellCalculation(resource, outputs, Map.<String, String>of(), false);

	}

	public List<ExcelCell> cellCalculation(String resource, List<String> outputs, boolean global) {
		return cellCalculation(resource, outputs, Map.<String, String>of(), global);
	}

	public List<ExcelCell> cellCalculation(String resource, List<String> outputs, Map<String, String> inputs) {
		return cellCalculation(resource, outputs, inputs, false);
	}

	public List<ExcelCell> cellCalculation(String resource, List<String> outputs, Map<String, String> inputs,
			boolean global) {

		Workbook workbook = retrieveWorkbook(resource);
		FormulaEvaluator exec = formula(workbook);

		if (global && inputs.size() > 0) {
			LOG.debug("mode global aka cross-ref enable");

			Map<String, FormulaEvaluator> workbooks = listOfResources.values().stream().collect(Collectors.toMap(
					ExcelResource::getFile,
					r -> (resource.compareTo(r.getName()) == 0) ? exec : formula(convertByteToWorkbook(r.getDoc()))));
			exec.setupReferencedWorkbooks(workbooks);
		}

		Function<Cell, ExcelCell> execFunction = null;

		inputs.entrySet().stream()
				.map(kv -> Map.entry(retrieveCellByAdress(new CellReference(kv.getKey()), workbook), kv.getValue()))
				.forEach(kv -> updateCell(kv.getKey(), kv.getValue()));

		if (inputs.size() > 0) {
			execFunction = cell -> computeCell(cell, exec);
		} else {
			execFunction = rawMapping;
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
		return cellCalculation(resource.get(), outputs.get(), inputs.get(), global);
	}

	private Cell retrieveCellByAdress(CellReference cr, Workbook workbook) {
		Cell ret = null;
		Sheet sheet = workbook.getSheet(cr.getSheetName());
		Row row = sheet.getRow(cr.getRow());
		if (row != null) {
			ret = row.getCell(cr.getCol(), MissingCellPolicy.CREATE_NULL_AS_BLANK);
		}
		return ret;
	}

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
					SimpleDateFormat sdf = new SimpleDateFormat(conf.getFormatDate());
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
					SimpleDateFormat sdf = new SimpleDateFormat(conf.getFormatDate());
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

	private Stream<Cell> streamCell(Workbook workbook, String sheetName) {
		return streamCell(workbook, s -> s.getSheetName().compareTo(sheetName) == 0);
	}

	private Stream<Cell> streamCell(Workbook workbook, Predicate<? super Sheet> predicate) {
		return StreamSupport.stream(workbook.spliterator(), false).filter(predicate).flatMap(sheet -> StreamSupport
				.stream(sheet.spliterator(), false).flatMap(r -> StreamSupport.stream(r.spliterator(), false)));
	}

	private String getCellComment(Cell cell) {

		String ret = null;
		if (cell.getCellComment() != null) {
			ret = cell.getCellComment().getString().toString().replaceAll("\\n", "");
		}
		return ret;
	}

	private ExcelCell computeCell(Cell cell, FormulaEvaluator exec) {
		return celltoExcelCell(cell, c -> exec.evaluateInCell(c));
	}

	private ExcelCell celltoExcelCell(Cell cell) {
		return celltoExcelCell(cell, Function.identity());
	}

	private ExcelCell celltoExcelCell(Cell cell, Function<Cell, Cell> valueFunction) {
		ExcelCell ret = new ExcelCell();

		ret.setAddress(cell.getSheet().getSheetName() + "!" + cell.getAddress().formatAsString());
		ret.setValue(getRawCell(valueFunction.apply(cell)));
		ret.setType(getCellType(cell));
		ret.setMetadata(getCellComment(cell));
		return ret;
	}

}
