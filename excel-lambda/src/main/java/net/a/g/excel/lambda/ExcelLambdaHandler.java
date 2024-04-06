package net.a.g.excel.lambda;

import java.util.List;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import net.a.g.excel.engine.ExcelEngine;
import net.a.g.excel.model.ExcelCell;
import net.a.g.excel.model.ExcelRequest;
import net.a.g.excel.model.ExcelResult;
import net.a.g.excel.param.ExcelParameter;

@Named("lambda")
public class ExcelLambdaHandler implements RequestHandler<ExcelRequest, ExcelResult>  {

	@Inject
	ExcelParameter conf;

	@Inject
	ExcelEngine engine;
	
	@Override
	public ExcelResult handleRequest(ExcelRequest input, Context context) {
		ExcelResult ret = null;
		List<ExcelCell> list = null;
		
		list =  engine.cellCalculation(input.getResource(), input.getOutputs(), input.getInputs() , false, false);
		
		ret.setCells(list);
		ret.setUuid(input.getUuid())
		;		
		return ret;
	}

}