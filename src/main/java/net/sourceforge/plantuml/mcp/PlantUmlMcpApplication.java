package net.sourceforge.plantuml.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class PlantUmlMcpApplication {

	public static void main(final String[] args) {
		SpringApplication.run(PlantUmlMcpApplication.class, args);
	}

	@Bean
	public ToolCallbackProvider plantUmlToolCallbackProvider(final PlantUmlTools tools) {
		return MethodToolCallbackProvider.builder().toolObjects(tools).build();
	}

}
