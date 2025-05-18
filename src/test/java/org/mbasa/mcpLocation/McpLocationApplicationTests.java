package org.mbasa.mcpLocation;

import java.util.Map;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;

class McpLocationApplicationTests {

	public static void main(String[] args) {

		var stdioParams = ServerParameters.builder("java")
				.args("-jar", "./target/mcpLocation.jar")
				.build();

		var transport = new StdioClientTransport(stdioParams);
		var client = McpClient.sync(transport).build();

		client.initialize();

		// List and demonstrate tools
		ListToolsResult toolsList = client.listTools();
		System.out.println("Available Tools = " + toolsList);

		CallToolResult geocoderResult = client
				.callTool(new CallToolRequest("geocodeAddress", Map.of("address",
						"杉並区清水一丁目−三−十四")));
		System.out.println("Geocoder Result: " + geocoderResult.toString());

		CallToolResult revGeocoderResult = client
				.callTool(new CallToolRequest("reverseGeocode", Map.of("latitude",
						35.710788822, "longitude", 139.620139631)));
		System.out.println("Reverse Geocoder Result: " + revGeocoderResult.toString());

		client.closeGracefully();
	}

}
