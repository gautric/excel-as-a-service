package net.a.g.excel.grpc;

import static net.a.g.excel.grpc.Fixtures.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.google.protobuf.Empty;

import io.grpc.Status;
import net.a.g.excel.repository.ExcelRepositoryImpl;

class ListOfResourceTest {

	private static Set<String> names(ExcelService s) {
		return items(s.listOfResource(Empty.getDefaultInstance())).stream().map(ExcelResource::getName)
				.collect(Collectors.toSet());
	}

	@Test
	void listsEveryLoadedResourceByName() {
		assertEquals(Set.of(ALL), names(service(ALL)));
	}

	@Test
	void emptyRepositoryCompletesWithoutItems() {
		assertTrue(items(service(new ExcelRepositoryImpl()).listOfResource(Empty.getDefaultInstance())).isEmpty());
	}

	@Test
	void emitsExactlyOneItemPerResource() {
		List<ExcelResource> list = items(service(ALL).listOfResource(Empty.getDefaultInstance()));
		assertEquals(ALL.length, list.size());
	}

	@Test
	void reflectsResourcesAddedAfterServiceCreation() {
		ExcelRepositoryImpl repo = repo("KYC");
		ExcelService s = service(repo);
		assertEquals(Set.of("KYC"), names(s));
		repo.add(sample("Type"));
		assertEquals(Set.of("KYC", "Type"), names(s));
	}

	@Test
	void reflectsRemovedResources() {
		ExcelRepositoryImpl repo = repo("KYC", "Type");
		ExcelService s = service(repo);
		repo.remove("KYC");
		assertEquals(Set.of("Type"), names(s));
	}

	@Test
	void replacingAResourceKeepsASingleEntry() {
		ExcelRepositoryImpl repo = repo("KYC");
		repo.add(sample("KYC"));
		assertEquals(1, items(service(repo).listOfResource(Empty.getDefaultInstance())).size());
	}

	@Test
	void resourceNameIsTheLogicalNameNotTheFileName() {
		ExcelRepositoryImpl repo = new ExcelRepositoryImpl();
		repo.add(resource("Alias", "KYC.xlsx", sample("KYC").getDoc()));
		assertEquals(Set.of("Alias"), names(service(repo)));
	}

	@Test
	void unexpectedFailureIsInternalWithoutLeakingTheCause() {
		ExcelService s = service("KYC");
		s.repo = null; // any unexpected runtime failure (here an NPE) in the repository call
		var e = failure(s.listOfResource(Empty.getDefaultInstance()), Status.Code.INTERNAL);
		assertEquals("Internal error", e.getStatus().getDescription());
	}
}
