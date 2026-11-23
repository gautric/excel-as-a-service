package net.a.g.excel.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import net.a.g.excel.engine.ExcelEngine;
import net.a.g.excel.model.ExcelRequest;
import net.a.g.excel.model.ExcelResult;
import net.a.g.excel.util.ExcelErrors;

/**
 * AWS Lambda entry point. Same request/response contract as the REST
 * {@code POST /eaas/api/compute} and the Kafka consumer; errors come back in
 * {@code error.code} ("400", "404", "500") instead of failing the invocation.
 */
@Named("lambda")
public class ExcelLambdaHandler implements RequestHandler<ExcelRequest, ExcelResult> {

	@Inject
	ExcelEngine engine;

	@Override
	public ExcelResult handleRequest(ExcelRequest input, Context context) {
		try {
			return engine.compute(input);
		} catch (Exception e) {
			return ExcelErrors.toResult(e, input == null ? null : input.getUuid());
		}
	}

}
