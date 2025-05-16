package org.mbasa.mcpLocation;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class McpLocationApplication {

	public static void main(String[] args) {
		SpringApplication.run(McpLocationApplication.class, args);
	}

	@Bean
	public ToolCallbackProvider locationTools(McpLocationService locationService) {
		return MethodToolCallbackProvider.builder().toolObjects(locationService).build();
	}

}
