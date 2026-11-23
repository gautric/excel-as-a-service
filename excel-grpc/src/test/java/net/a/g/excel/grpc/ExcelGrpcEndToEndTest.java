package net.a.g.excel.grpc;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.google.protobuf.DoubleValue;
import com.google.protobuf.Empty;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

/** Real gRPC server and client over the wire (no Docker needed). */
@QuarkusTest
@TestProfile(ExcelGrpcEndToEndTest.Profile.class)
class ExcelGrpcEndToEndTest {

	public static class Profile implements QuarkusTestProfile {
		@Override
		public Map<String, String> getConfigOverrides() {
			// shortcut: fixed gRPC port so the client can find the server; random HTTP port
			return Map.of("excel.static.resouces.uri", "../sample", //
					"quarkus.http.test-port", "0", //
					"quarkus.grpc.server.test-port", "19317", //
					"quarkus.grpc.clients.excel.port", "19317", //
					"quarkus.grpc.clients.excel.test-port", "19317");
		}
	}

	private static final Duration TIMEOUT = Duration.ofSeconds(30);

	@GrpcClient("excel")
	Excel client;

	@Test
	void listOfResourceOverTheWire() {
		Set<String> names = client.listOfResource(Empty.getDefaultInstance()).collect().asList().await()
				.atMost(TIMEOUT).stream().map(ExcelResource::getName).collect(Collectors.toSet());
		assertTrue(names.containsAll(Set.of(Fixtures.ALL)), names::toString);
	}

	@Test
	void listOfSheetOverTheWire() {
		List<String> sheets = client.listOfSheet(ExcelRequest.newBuilder().setResource("Test1").build()).collect()
				.asList().await().atMost(TIMEOUT).stream().map(ExcelSheet::getName).toList();
		assertEquals(List.of("Sheet1", "Sheet2", "Sheet3"), sheets);
	}

	@Test
	void computeCellOverTheWireUnpacksAny() throws Exception {
		List<ExcelCell> cells = client.computeCell(Fixtures.kycapi("true", "CY", "1000000").build()).collect().asList()
				.await().atMost(TIMEOUT);
		assertEquals(1, cells.size());
		assertEquals(125.0, cells.get(0).getValue().unpack(DoubleValue.class).getValue());
	}

	@Test
	void globalComputeOverTheWire() throws Exception {
		ExcelRequest r = Fixtures.req("Primary", "Feuil1").addOutputs("B1").putInputs("C1", "2").setGlobal(true).build();
		ExcelCell c = client.computeCell(r).collect().asList().await().atMost(TIMEOUT).get(0);
		assertEquals(46.0, c.getValue().unpack(DoubleValue.class).getValue());
	}

	@Test
	void statusCodesSurviveTheTransport() {
		StatusRuntimeException notFound = assertThrows(StatusRuntimeException.class,
				() -> client.listOfCell(Fixtures.req("KYCAPI", "Nope").build()).collect().asList().await()
						.atMost(TIMEOUT));
		assertEquals(Status.Code.NOT_FOUND, notFound.getStatus().getCode());
		assertEquals("Sheet 'Nope' not found in resource 'KYCAPI'", notFound.getStatus().getDescription());

		StatusRuntimeException invalid = assertThrows(StatusRuntimeException.class,
				() -> client.computeCell(Fixtures.kycapi("true", "CY", "abc").build()).collect().asList().await()
						.atMost(TIMEOUT));
		assertEquals(Status.Code.INVALID_ARGUMENT, invalid.getStatus().getCode());
	}

	@Test
	void httpFacadeGoesThroughTheGrpcClient() {
		given().when().get("/grpc/Test1").then().statusCode(200).body(containsString("Sheet2"));
	}
}
