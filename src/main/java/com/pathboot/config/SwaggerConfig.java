package com.pathboot.config;

import com.pathboot.util.PathBootConstants;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Springdoc OpenAPI (Swagger) configuration.
 *
 * <p>Swagger UI is available at {@code /swagger-ui.html} after startup.</p>
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI pathBootOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title(PathBootConstants.SWAGGER_API_TITLE)
                        .description(PathBootConstants.SWAGGER_API_DESCRIPTION)
                        .version(PathBootConstants.SWAGGER_API_VERSION)
                        .contact(new Contact()
                                .name("PathBoot PI Team")
                                .email("support@pathboot.local"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}

