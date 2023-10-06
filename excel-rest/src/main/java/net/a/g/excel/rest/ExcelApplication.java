package net.a.g.excel.rest;

import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;

import jakarta.ws.rs.ApplicationPath;

@OpenAPIDefinition(info = @Info(title = "Excel as a Service", version = "1.0", description = "Engine to compute Excel Resource as a Service", contact = @Contact(email = "gautric@redhat.com", name = "Greg I/O", url = "https://github.com/gautric/excel-as-a-service")))
@ApplicationPath("eaas")
public class ExcelApplication extends jakarta.ws.rs.core.Application {

}