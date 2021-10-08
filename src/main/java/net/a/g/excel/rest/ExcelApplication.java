package net.a.g.excel.rest;

import javax.ws.rs.ApplicationPath;

import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Info;

@OpenAPIDefinition(info = @Info(title = "Excel REST API", version = "1.0"))
@ApplicationPath("api")
public class ExcelApplication extends javax.ws.rs.core.Application {

}