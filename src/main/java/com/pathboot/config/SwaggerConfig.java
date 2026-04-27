package com.pathboot.config;

import com.pathboot.util.PathBootConstants;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Springdoc OpenAPI (Swagger) configuration.
 *
 * <p>Swagger UI is available at {@code /swagger-ui.html} after startup.</p>
 * <p>Click the <b>Authorize</b> button and enter your API key to test secured endpoints.</p>
 */
@Configuration
public class SwaggerConfig {

    /** Must match the name used in {@link SecurityConfig}. */
    private static final String SECURITY_SCHEME_NAME = "ApiKeyAuth";

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
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                // ── Register the API Key security scheme ──────────────────────
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name(PathBootConstants.API_KEY_HEADER)
                                .description("Provide your API key in the **"
                                             + PathBootConstants.API_KEY_HEADER
                                             + "** header. Example: `pathboot-dev-key-2026`")))
                // ── Apply globally so every endpoint shows the padlock icon ───
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }
}

