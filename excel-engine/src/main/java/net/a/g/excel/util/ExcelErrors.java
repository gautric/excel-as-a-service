package net.a.g.excel.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.a.g.excel.exception.ExcelInvalidInputException;
import net.a.g.excel.exception.ExcelResourceNotFoundException;
import net.a.g.excel.model.ExcelError;
import net.a.g.excel.model.ExcelResult;

/**
 * Turns an exception into an {@link ExcelResult} carrying an {@link ExcelError},
 * with HTTP-like codes shared by every protocol: "404" not found, "400" invalid
 * request, "500" anything else. Internal details (stack traces) are only logged.
 */
public final class ExcelErrors {

	private static final Logger LOG = LoggerFactory.getLogger(ExcelErrors.class);

	private ExcelErrors() {
	}

	public static String code(Throwable e) {
		if (e instanceof ExcelResourceNotFoundException) {
			return "404";
		}
		if (e instanceof ExcelInvalidInputException || e instanceof IllegalArgumentException) {
			return "400";
		}
		return "500";
	}

	public static ExcelResult toResult(Throwable e, String uuid) {
		String code = code(e);
		ExcelError err = new ExcelError();
		err.setCode(code);
		if ("500".equals(code)) {
			LOG.error("Unexpected error for request {}", uuid, e);
			err.setError("Internal error");
		} else {
			LOG.debug("Rejected request {}: {}", uuid, e.getMessage());
			err.setError(e.getMessage());
		}
		ExcelResult ret = new ExcelResult();
		ret.setUuid(uuid);
		ret.setError(err);
		return ret;
	}
}
