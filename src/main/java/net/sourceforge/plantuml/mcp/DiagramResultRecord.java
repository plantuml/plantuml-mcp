package net.sourceforge.plantuml.mcp;

import java.util.List;

public record DiagramResultRecord(boolean valid, String diagramType, int lineCount, Integer errorLineNumber,
		String errorLine, String errorMessage, String errorContext, List<String> warnings, String svg) {

	public static DiagramResultRecord from(final McpResult result) {
		if (result.isOk())
			return new DiagramResultRecord(true, result.getDiagramType(), result.getLineCount(), null, null, null, null,
					result.getWarnings(), result.getSvg());

		final Integer lineNumber = result.getErrorLineNumber() > 0 ? result.getErrorLineNumber() : null;
		return new DiagramResultRecord(false, null, result.getLineCount(), lineNumber, result.getErrorLine(),
				result.getErrorMessage(), result.getErrorContext(), result.getWarnings(), null);
	}

}
