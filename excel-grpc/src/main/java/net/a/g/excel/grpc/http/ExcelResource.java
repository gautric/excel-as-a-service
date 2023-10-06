package net.a.g.excel.grpc.http;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.protobuf.Empty;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Multi;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import net.a.g.excel.grpc.Excel;
import net.a.g.excel.grpc.ExcelCell;
import net.a.g.excel.grpc.ExcelRequest;

@Path("/grpc")
public class ExcelResource {

	@GrpcClient("excel")
	Excel client;

	@GET
	@Path("")
	@Produces(MediaType.TEXT_PLAIN)
	public Multi<net.a.g.excel.grpc.ExcelResource> listOfResource() {
		return client.listOfResource(Empty.newBuilder().build());
	}

	@GET
	@Path("{resource}")
	@Produces(MediaType.TEXT_PLAIN)
	public Multi<net.a.g.excel.grpc.ExcelSheet> listOfSheet(@PathParam("resource") String resource) {
		return client.listOfSheet(ExcelRequest.newBuilder().setResource(resource).build());
	}

	@GET
	@Path("{resource}/{sheet}")
	@Produces(MediaType.TEXT_PLAIN)
	public Multi<net.a.g.excel.grpc.ExcelCell> listOfCell(@PathParam("resource") String resource,
			@PathParam("sheet") String sheet) {
		return (Multi<ExcelCell>) client
				.listOfCell(ExcelRequest.newBuilder().setResource(resource).setSheet(sheet).build());
	}

	@GET
	@Path("{resource}/{sheet}/{cells}")
	@Produces(MediaType.TEXT_PLAIN)
	public Multi<net.a.g.excel.grpc.ExcelCell> cellsQuery(@PathParam("resource") String resource,
			@PathParam("sheet") String sheet, @PathParam("cells") String cells, @Context UriInfo uriInfo) {

		Log.debug(uriInfo.getRequestUri().toString());

		Map<String, List<String>> param = uriInfo.getQueryParameters();

		Map<String, String> query = param.entrySet().stream()
				.collect(Collectors.toMap(e -> renameFunction(sheet).apply(e.getKey()), kv -> kv.getValue().get(0)));

		ExcelRequest request = ExcelRequest.newBuilder().setResource(resource).setSheet(sheet).putAllInputs(query)
				.addAllOutputs(Arrays.asList(cells.split(",")).stream().map(c -> this.renameFunction(sheet).apply(c))
						.collect(Collectors.toList()))
				.build();

		Multi<net.a.g.excel.grpc.ExcelCell> ret = client.computeCell(request);

		return ret;
	}

	private Function<String, String> renameFunction(String sheet) {

		return cn -> cn.contains("!") ? cn : sheet + "!" + cn;
	}

}
