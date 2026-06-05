package net.sourceforge.plantuml.mcp;

import java.util.List;

import net.sourceforge.plantuml.command.Explanation;
import net.sourceforge.plantuml.utils.LineLocation;

/**
 * JSON-friendly view of a single explanation, returned by the
 * <code>diagram_explain</code> MCP tool.
 *
 * @param input      the source line(s) that produced this explanation, never
 *                   {@code null} (possibly empty)
 * @param explain    the explanation text
 * @param lineNumber the 1-based line of the explained source, or {@code null}
 *                   when no location applies
 */
public record DiagramExplainRecord(List<String> input, String explain, Integer lineNumber) {

	/**
	 * Builds the JSON view from a core {@link Explanation}.
	 *
	 * @param explanation the core explanation (never {@code null})
	 * @return the JSON-friendly view
	 */
	public static DiagramExplainRecord from(final Explanation explanation) {
		final LineLocation location = explanation.getLocation();
		// getPosition() is 0-based; the public contract exposes a 1-based line.
		final Integer lineNumber = location == null ? null : location.getPosition() + 1;
		return new DiagramExplainRecord(explanation.getInput(), explanation.getExplain(), lineNumber);
	}

}
