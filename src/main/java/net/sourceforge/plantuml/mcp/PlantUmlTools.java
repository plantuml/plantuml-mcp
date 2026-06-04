package net.sourceforge.plantuml.mcp;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import net.sourceforge.plantuml.version.Version;

/**
 * MCP tools exposing PlantUML capabilities to AI assistants.
 *
 * <p>
 * For the MVP, a single tool is provided that reports the embedded PlantUML
 * version. This validates the full chain (Gradle build, stdio transport, tool
 * discovery by the MCP client) before richer tools such as rendering and
 * syntax validation are added.
 */
@Service
public class PlantUmlTools {

	@Tool(name = "plantuml_version", description = "Returns the version of the embedded PlantUML library")
	public String plantUmlVersion() {
		return Version.fullDescription();
	}

}
