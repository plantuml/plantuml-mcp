package net.sourceforge.plantuml.mcp;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import net.sourceforge.plantuml.command.Explanation;
import net.sourceforge.plantuml.version.Version;

/**
 * MCP tools exposing PlantUML capabilities to AI assistants.
 *
 * <p>
 * Four tools are provided so far: one that reports the embedded PlantUML
 * version (it validates the full chain: Gradle build, stdio transport, tool
 * discovery by the MCP client), one that checks the syntax of a single diagram
 * without rendering it, one that renders a single diagram to the requested
 * output format, and one that explains how a single diagram is parsed, line by
 * line.
 */
@Service
public class PlantUmlTools {

	@Tool(name = "plantuml_version", description = "Returns the version of the embedded PlantUML library")
	public String plantUmlVersion() {
		return Version.fullDescription();
	}

	@Tool(name = "check_syntax", description = "Checks the syntax of a single PlantUML diagram without rendering it. "
			+ "Returns a JSON object containing: 'valid' (boolean), 'diagramType' (if valid), 'lineCount', "
			+ "'warnings' (list of non-fatal warnings), and on failure 'errorLineNumber' (1-based), 'errorMessage', "
			+ "'errorLine' (the offending source line, when available), and 'errorContext'.")
	public DiagramResultRecord checkSyntax(
			@ToolParam(description = "The PlantUML source to check, including @start.../@end... (a single diagram)") final String source)
			throws IOException {
		final McpResult result = new SyntaxChecker().check(source);
		return DiagramResultRecord.from(result);
	}

	@Tool(name = "diagram_render", description = "Renders a single PlantUML diagram to the requested output format. "
			+ "Returns a JSON object containing: 'valid' (boolean), 'diagramType' (if valid), 'lineCount', "
			+ "'warnings' (list of non-fatal warnings), the rendered 'content' on success, and on failure, "
			+ "'errorLineNumber' (1-based), 'errorMessage', 'errorLine' (the offending source line, when available), "
			+ "and 'errorContext'.")
	public DiagramResultRecord diagramRender(
			@ToolParam(description = "The PlantUML source to render, including @start.../@end... (a single diagram)") final String source,
			@ToolParam(description = "Output format, only SVG supported right now") final String format)
			throws IOException {
		final McpResult result = new DiagramRenderer().render(source, format);
		return DiagramResultRecord.from(result);
	}

	@Tool(name = "diagram_explain", description = "Explains how a single PlantUML diagram is parsed, line by line. "
			+ "Returns a JSON array of objects, each containing: 'input' (the source line(s) that produced the explanation), "
			+ "'explain' (a human-readable explanation), and 'line' (1-based, or null when no location applies).")
	public List<DiagramExplainRecord> diagramExplain(
			@ToolParam(description = "The PlantUML source to explain, including @start.../@end... (a single diagram)") final String source)
			throws IOException {
		final List<Explanation> explanations = new DiagramExplainer().explain(source);
		return explanations.stream().map(DiagramExplainRecord::from).collect(Collectors.toList());
	}

}
