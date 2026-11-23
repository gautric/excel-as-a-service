package net.a.g.excel.grpc;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Any;
import com.google.protobuf.BoolValue;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;

import io.grpc.Status;
import io.quarkus.grpc.GrpcService;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.a.g.excel.engine.ExcelEngine;
import net.a.g.excel.exception.ExcelInvalidInputException;
import net.a.g.excel.exception.ExcelResourceNotFoundException;
import net.a.g.excel.repository.ExcelRepository;
import net.a.g.excel.validation.InputValidator;

/**
 * gRPC facade. {@code computeCell} uses the same engine entry point as REST,
 * Kafka and Lambda; engine errors become gRPC status codes
 * (NOT_FOUND, INVALID_ARGUMENT, INTERNAL).
 */
@GrpcService
@Singleton
@Blocking // POI evaluation is CPU-bound: run on worker threads, not the event loop
public class ExcelService implements Excel {

	public final static Logger LOG = LoggerFactory.getLogger(ExcelService.class);

	static Function<net.a.g.excel.model.ExcelCell, net.a.g.excel.grpc.ExcelCell> functorMap = cell -> {
		ExcelCell.Builder build = ExcelCell.newBuilder();
		if (cell.getAddress() != null) {
			build.setAddress(cell.getAddress());
		}
		if (cell.getType() != null) {
			build.setType(cell.getType());
		}
		if (cell.getMetadata() != null) {
			build.setMetadata(cell.getMetadata());
		}
		Message message = toMessage(cell);
		if (message != null) {
			build.setValue(Any.pack(message));
		}
		return build.build();
	};

	private static Message toMessage(net.a.g.excel.model.ExcelCell cell) {
		Object v = cell.getValue();
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return DoubleValue.of(n.doubleValue());
		}
		if ("BOOLEAN".equals(cell.getType())) {
			return BoolValue.of(Boolean.parseBoolean(v.toString()));
		}
		// STRING, FORMULA, DATE, BLANK, ERROR
		return StringValue.of(v.toString());
	}

	@Inject
	ExcelEngine engine;

	@Inject
	ExcelRepository repo;

	@Override
	public Multi<net.a.g.excel.grpc.ExcelResource> listOfResource(Empty request) {
		return guarded(() -> repo.listOfResources().stream().map(net.a.g.excel.model.ExcelResource::getName)
				.map(name -> ExcelResource.newBuilder().setName(name).build()));
	}

	@Override
	public Multi<net.a.g.excel.grpc.ExcelSheet> listOfSheet(net.a.g.excel.grpc.ExcelRequest request) {
		return guarded(() -> engine.listOfSheet(request.getResource()).stream()
				.map(net.a.g.excel.model.ExcelSheet::getName).map(name -> ExcelSheet.newBuilder().setName(name).build()));
	}

	@Override
	public Multi<net.a.g.excel.grpc.ExcelCell> computeCell(net.a.g.excel.grpc.ExcelRequest request) {
		return guarded(() -> {
			net.a.g.excel.model.ExcelRequest req = new net.a.g.excel.model.ExcelRequest();
			req.setResource(request.getResource());
			req.setSheet(request.getSheet());
			req.setOutputs(request.getOutputsList());
			req.setInputs(request.getInputsMap());
			req.setGlobal(request.getGlobal());
			return engine.compute(req).getCells().stream().map(functorMap);
		});
	}

	@Override
	public Multi<net.a.g.excel.grpc.ExcelCell> listOfCell(ExcelRequest request) {
		return guarded(() -> {
			InputValidator.validateResourceName(request.getResource());
			if (!engine.isSheetExists(request.getResource(), request.getSheet())) {
				throw new ExcelResourceNotFoundException(request.getResource(),
						"Sheet '" + request.getSheet() + "' not found in resource '" + request.getResource() + "'");
			}
			return engine.listOfCell(request.getResource(), request.getSheet(), cell -> true).stream()
					.map(functorMap);
		});
	}

	private static <T> Multi<T> guarded(Supplier<Stream<T>> body) {
		try {
			return Multi.createFrom().items(body.get());
		} catch (ExcelResourceNotFoundException e) {
			return Multi.createFrom().failure(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
		} catch (ExcelInvalidInputException | IllegalArgumentException e) {
			return Multi.createFrom()
					.failure(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
		} catch (Exception e) {
			LOG.error("gRPC unexpected error", e);
			return Multi.createFrom().failure(Status.INTERNAL.withDescription("Internal error").asRuntimeException());
		}
	}

}
