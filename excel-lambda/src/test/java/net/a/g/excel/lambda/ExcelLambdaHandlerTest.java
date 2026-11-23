package net.a.g.excel.lambda;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.a.g.excel.engine.ExcelEngineImpl;
import net.a.g.excel.model.ExcelRequest;
import net.a.g.excel.model.ExcelResource;
import net.a.g.excel.model.ExcelResult;
import net.a.g.excel.param.ExcelParameterImpl;
import net.a.g.excel.repository.ExcelRepositoryImpl;

/** Plain unit test (no Lambda runtime): the handler used to always throw a NullPointerException. */
public class ExcelLambdaHandlerTest {

	private ExcelLambdaHandler handler;

	@BeforeEach
	public void setup() throws Exception {
		ExcelRepositoryImpl repo = new ExcelRepositoryImpl();
		ExcelResource r = new ExcelResource();
		r.setName("KYCAPI");
		r.setFile("KYCAPI.xlsx");
		r.setDoc(Files.readAllBytes(Path.of("../sample/KYCAPI.xlsx")));
		repo.add(r);

		ExcelEngineImpl engine = new ExcelEngineImpl();
		engine.setRepository(repo);
		engine.setParameter(new ExcelParameterImpl());

		handler = new ExcelLambdaHandler();
		handler.engine = engine;
	}

	@Test
	public void computes() {
		ExcelRequest req = new ExcelRequest("abc");
		req.setResource("KYCAPI");
		req.setSheet("ComputeKYC");
		req.setOutputs(List.of("SCORE"));
		req.setInputs(Map.of("PEP", "true", "COUNTRY", "CY", "AMOUNT", "1000000"));

		ExcelResult res = handler.handleRequest(req, null);

		assertNull(res.getError());
		assertEquals("abc", res.getUuid());
		assertEquals(125.0, res.getCells().iterator().next().getValue());
	}

	@Test
	public void unknownResourceIsReportedNotThrown() {
		ExcelRequest req = new ExcelRequest("xyz");
		req.setResource("NOPE");
		req.setOutputs(List.of("A!A1"));

		ExcelResult res = handler.handleRequest(req, null);

		assertEquals("404", res.getError().getCode());
		assertEquals("xyz", res.getUuid());
	}
}
