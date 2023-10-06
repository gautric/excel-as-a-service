package net.a.g.excel.rest;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import net.a.g.excel.model.ExcelError;
import net.a.g.excel.model.ExcelLink;
import net.a.g.excel.model.ExcelResult;

public class ExcelRestTool {

	public static Response returnKO(Status status, String message) {
		ExcelResult ret = new ExcelResult();
		ExcelError err = new ExcelError();

		err.setCode("" + status.getStatusCode());
		err.setError(message);
		ret.setError(err);

		return Response.status(status).entity(ret).build();

	}

	public static Response returnOK(ExcelResult ret, Link link) {

		ExcelLink el = new ExcelLink();
		el.setHref(URLDecoder.decode(link.getUri().toString(), StandardCharsets.UTF_8));
		el.setRel("self");
		el.setType(MediaType.APPLICATION_JSON);

		ret.getLinks().add(el);

		return Response.status(Response.Status.OK).entity(ret).links(link).build();
	}

}
