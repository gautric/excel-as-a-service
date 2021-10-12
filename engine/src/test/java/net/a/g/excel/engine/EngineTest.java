package net.a.g.excel.engine;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.inject.Inject;

import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import net.a.g.excel.util.ExcelConfiguration;
import net.a.g.excel.util.ExcelUtils;

@ExtendWith(WeldJunit5Extension.class)
public class EngineTest {
	
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.of(ExcelEngine.class, ExcelConfiguration.class);
    
	@Inject
	ExcelEngine engine;
	
	
	@BeforeEach
	public void setup() {
		
	}
	
	@Test
	public void testEngine() {
		assertNotNull(engine);
	}
	
}