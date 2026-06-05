#!/usr/bin/env python3
"""
Small test client for the plantuml-mcp `diagram_explain` tool over the
Streamable HTTP endpoint.

It performs the full MCP handshake and then calls `diagram_explain` on a sample
diagram, printing the JSON result returned by the server.

The sample diagram mirrors the Java unit test for DiagramExplainer: a sequence
diagram with an actor and a single message, which is expected to yield two
explanations (one for the participant creation, one for the message).

Usage:
    # Start the server in HTTP mode first:
    #   java -jar build/libs/plantuml-mcp-1.0.0.jar --spring.profiles.active=http
    python test_diagram_explain.py
    python test_diagram_explain.py --url http://localhost:8080/mcp

Only the standard library is used; no external dependencies.
"""

import argparse
import json
import sys
import urllib.error
import urllib.request

# MCP protocol version implemented by the server (see its initialize response).
PROTOCOL_VERSION = "2025-03-26"

# Both content types must be accepted: the server may answer with a plain JSON
# body or with a Server-Sent Events stream depending on the message.
ACCEPT = "application/json, text/event-stream"

# The diagram whose parsing we want explained. Mirrors the Java test2() source.
SAMPLE_DIAGRAM = "\n".join([
	"@startuml",
	"actor Alice",
	"Alice -> Bob",
	"@enduml",
])


def parse_body(raw: bytes, content_type: str) -> dict:
	"""Parse a response body that is either raw JSON or an SSE stream.

	For SSE, the JSON payload sits on one or more `data:` lines; we collect
	them and parse the concatenated result.
	"""
	text = raw.decode("utf-8").strip()
	if "text/event-stream" in content_type:
		data_lines = []
		for line in text.splitlines():
			if line.startswith("data:"):
				data_lines.append(line[len("data:"):].strip())
		text = "".join(data_lines)
	if text == "":
		return {}
	return json.loads(text)


def post(url: str, payload: dict, session_id: str | None) -> tuple[dict, str | None]:
	"""Send one JSON-RPC message and return (parsed_body, session_id)."""
	data = json.dumps(payload).encode("utf-8")
	headers = {
		"Content-Type": "application/json",
		"Accept": ACCEPT,
	}
	if session_id is not None:
		headers["Mcp-Session-Id"] = session_id

	request = urllib.request.Request(url, data=data, headers=headers, method="POST")
	try:
		with urllib.request.urlopen(request) as response:
			returned_session = response.headers.get("Mcp-Session-Id", session_id)
			content_type = response.headers.get("Content-Type", "")
			body = parse_body(response.read(), content_type)
			return body, returned_session
	except urllib.error.HTTPError as error:
		detail = error.read().decode("utf-8", errors="replace")
		raise SystemExit(f"HTTP {error.code} on {url}: {detail}") from error
	except urllib.error.URLError as error:
		raise SystemExit(
			f"Cannot reach {url}: {error.reason}. "
			"Is the server running with --spring.profiles.active=http ?"
		) from error


def notify(url: str, payload: dict, session_id: str | None) -> None:
	"""Send a JSON-RPC notification (no response expected)."""
	data = json.dumps(payload).encode("utf-8")
	headers = {
		"Content-Type": "application/json",
		"Accept": ACCEPT,
	}
	if session_id is not None:
		headers["Mcp-Session-Id"] = session_id
	request = urllib.request.Request(url, data=data, headers=headers, method="POST")
	try:
		with urllib.request.urlopen(request) as response:
			response.read()
	except urllib.error.HTTPError as error:
		# Some servers answer 202 Accepted with an empty body for notifications.
		if error.code not in (200, 202):
			detail = error.read().decode("utf-8", errors="replace")
			raise SystemExit(f"HTTP {error.code} on notify: {detail}") from error


def extract_text(tool_result: dict) -> str:
	"""Pull the text payload out of a tools/call result."""
	result = tool_result.get("result", {})
	content = result.get("content", [])
	texts = [item.get("text", "") for item in content if item.get("type") == "text"]
	return "\n".join(texts).strip() or json.dumps(result)


def main() -> int:
	parser = argparse.ArgumentParser(description="Test the plantuml-mcp diagram_explain tool.")
	parser.add_argument(
		"--url",
		default="http://localhost:8080/mcp",
		help="MCP endpoint URL (default: http://localhost:8080/mcp)",
	)
	args = parser.parse_args()

	# 1. initialize: open the session and negotiate the protocol version.
	print(f"-> initialize  ({args.url})")
	init_payload = {
		"jsonrpc": "2.0",
		"id": 1,
		"method": "initialize",
		"params": {
			"protocolVersion": PROTOCOL_VERSION,
			"capabilities": {},
			"clientInfo": {"name": "plantuml-mcp-tester", "version": "1.0"},
		},
	}
	init_result, session_id = post(args.url, init_payload, None)
	server_info = init_result.get("result", {}).get("serverInfo", {})
	print(f"   connected to {server_info.get('name', '?')} {server_info.get('version', '')}")
	print(f"   session id: {session_id}")

	# 2. notifications/initialized: required by the protocol after initialize.
	notify(
		args.url,
		{"jsonrpc": "2.0", "method": "notifications/initialized"},
		session_id,
	)

	# 3. tools/call: invoke diagram_explain with the sample diagram.
	print("-> tools/call  diagram_explain")
	print("   source:")
	for line in SAMPLE_DIAGRAM.splitlines():
		print(f"     | {line}")
	call_payload = {
		"jsonrpc": "2.0",
		"id": 2,
		"method": "tools/call",
		"params": {
			"name": "diagram_explain",
			"arguments": {"source": SAMPLE_DIAGRAM},
		},
	}
	call_result, session_id = post(args.url, call_payload, session_id)

	raw = extract_text(call_result)
	print("\nResult returned by the server:")
	# The tool returns a list of DiagramExplainRecord serialized as JSON, which
	# Spring AI wraps as a text content item. Try to pretty-print it as JSON;
	# fall back to the raw text if the shape differs.
	try:
		parsed = json.loads(raw)
		print(json.dumps(parsed, indent=2))
		if isinstance(parsed, list):
			print(f"\n=> {len(parsed)} explanation(s)")
			for item in parsed:
				if isinstance(item, dict):
					line = item.get("line")
					input_lines = item.get("input", [])
					explain = item.get("explain", "")
					print(f"   line {line}: {input_lines} ==> {explain}")
	except json.JSONDecodeError:
		print(raw)
	return 0


if __name__ == "__main__":
	sys.exit(main())
