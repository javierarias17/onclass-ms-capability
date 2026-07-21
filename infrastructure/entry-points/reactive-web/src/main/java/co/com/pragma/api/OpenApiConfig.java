package co.com.pragma.api;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Onclass Capability API",
        version = "1.0.0",
        description = "API for Onclass Capability Microservice",
        contact = @Contact(
            name = "Pragma Team",
            email = "support@pragma.co",
            url = "https://pragma.co"
        ),
        license = @License(
            name = "Apache 2.0",
            url = "https://www.apache.org/licenses/LICENSE-2.0"
        )
    )
)
public class OpenApiConfig {
    // Configuration is handled via annotations
}
