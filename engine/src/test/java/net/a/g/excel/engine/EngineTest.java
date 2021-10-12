package net.a.g.excel.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;

import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import net.a.g.excel.util.ExcelConfiguration;

@ExtendWith(WeldJunit5Extension.class)
public class EngineTest {

	@WeldSetup
	public WeldInitiator weld = WeldInitiator.from(ExcelEngine.class, ExcelConfiguration.class)
			.activate(RequestScoped.class, SessionScoped.class).build();;

	@Inject
	ExcelEngine engine;

	@BeforeEach
	public void setup() {
		assertNotNull(engine);
		InputStream inputStream = EngineTest.class.getResourceAsStream("/KYC.xlsx");
		assertTrue(engine.addFile("KYC", inputStream));
	
	}
	
	@AfterEach
	public void close() {
		engine.clearAll();
		assertEquals(0,engine.countListOfResource());
	}

	@Test
	public void testEngine() {
		assertNotNull(engine);
	}

	@Test
	public void testTest() {
		assertNotNull(engine);
		assertEquals(1,engine.countListOfResource());
	}

}