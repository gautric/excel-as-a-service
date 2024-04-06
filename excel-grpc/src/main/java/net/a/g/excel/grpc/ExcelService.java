package net.a.g.excel.grpc;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Any;
import com.google.protobuf.BoolValue;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.a.g.excel.engine.ExcelEngine;
import net.a.g.excel.repository.ExcelRepository;

@GrpcService
@Singleton
public class ExcelService implements Excel {

	public final static Logger LOG = LoggerFactory.getLogger(ExcelService.class);

	static Function<net.a.g.excel.model.ExcelCell, net.a.g.excel.grpc.ExcelCell> functorMap = cell -> {
		ExcelCell.Builder build = ExcelCell.newBuilder().setAddress(cell.getAddress()).setType(cell.getType());
		build = (cell.getMetadata() != null) ? build.setMetadata(cell.getMetadata()) : build;

		try {
			if (cell.getValue() != null) {

				Message message = null;

				switch (cell.getType()) {
				case "NUMERIC":
					message = DoubleValue.of((Double) cell.getValue());
					break;

				case "STRING":
					message = StringValue.of((String) cell.getValue());
					break;

				case "FORMULA":
					message = StringValue.of((String) cell.getValue());
					break;

				case "BOOLEAN":
					message = BoolValue.of(Boolean.parseBoolean(cell.getValue().toString()));
					break;
				default:
					break;
				}

				build.setValue(Any.pack(message));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return build.build();
	};

	@Inject
	ExcelEngine engine;

	@Inject
	ExcelRepository repo;

	@Override
	public Multi<net.a.g.excel.grpc.ExcelResource> listOfResource(Empty request) {
		LOG.error("listOfResource");
		return Multi.createFrom().items(repo.listOfResources().stream().map(net.a.g.excel.model.ExcelResource::getName)
				.map(name -> ExcelResource.newBuilder().setName(name).build()));
	}

	@Override
	public Multi<net.a.g.excel.grpc.ExcelSheet> listOfSheet(net.a.g.excel.grpc.ExcelRequest request) {
		return Multi.createFrom()
				.items(engine.listOfSheet(request.getResource()).stream().map(net.a.g.excel.model.ExcelSheet::getName)
						.map(name -> ExcelSheet.newBuilder().setName(name).build()));
	}

	@Override
	public Multi<net.a.g.excel.grpc.ExcelCell> computeCell(net.a.g.excel.grpc.ExcelRequest request) {
		return Multi.createFrom()
				.items(engine.cellCalculation(request.getResource(), request.getOutputsList(), request.getInputsMap())
						.stream().map(functorMap));
	}

	@Override
	public Multi<net.a.g.excel.grpc.ExcelCell> listOfCell(ExcelRequest request) {

		return Multi.createFrom().items(
				engine.listOfCell(request.getResource(), request.getSheet(), cell -> true).stream().map(functorMap));

	}

}