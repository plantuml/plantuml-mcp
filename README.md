# plantuml-mcp

Model Context Protocol (MCP) server for [PlantUML](https://plantuml.com).

It exposes PlantUML capabilities as tools that AI assistants and LLM clients
(such as LM Studio or Claude) can call directly.

> **Status: MVP.** This is an early minimum viable product. For now it provides
> a single tool that returns the embedded PlantUML version. Its purpose is to
> validate the full chain — build, transports, and tool discovery by the client
> — before richer tools such as diagram rendering and syntax validation are
> added.

## Requirements

- Java 21 or later (the build targets Java 21 by default).
- An MCP-capable client (for example LM Studio) for the stdio transport, or any
  Streamable HTTP MCP client for the HTTP transport.

## Available tools

| Tool               | Description                                          |
|--------------------|------------------------------------------------------|
| `plantuml_version` | Returns the version of the embedded PlantUML library |

## Build

The project uses Gradle (Kotlin DSL) with the bundled wrapper:

```bash
./gradlew bootJar
```

On Windows:

```powershell
.\gradlew bootJar
```

This produces the executable JAR at:

```
build/libs/plantuml-mcp-1.0.0.jar
```

The Java version defaults to 21 but can be overridden without editing the build
file:

```bash
./gradlew bootJar -PjavaVersion=17
```

## Transports

The server supports two MCP transports, selected at runtime via Spring profiles:

| Transport           | When to use                                              | Activation                          |
|---------------------|----------------------------------------------------------|-------------------------------------|
| **stdio** (default) | Local, single client launching the server as subprocess  | none (default profile)              |
| **Streamable HTTP** | Remote and/or multiple clients over the network          | `--spring.profiles.active=http`     |

The HTTP transport uses the modern **Streamable HTTP** protocol, not the
deprecated SSE transport.

## stdio transport (default)

In stdio mode, the client launches the server as a subprocess and exchanges
JSON-RPC messages over standard input/output. This is the simplest setup for a
local client such as LM Studio.

Add the following entry to your client's `mcp.json`, adjusting the path to the
JAR:

```json
{
  "mcpServers": {
    "plantuml": {
      "command": "java",
      "args": ["-jar", "C:\\github\\plantuml-mcp\\build\\libs\\plantuml-mcp-1.0.0.jar"]
    }
  }
}
```

On non-Windows systems, use the corresponding absolute path, for example
`/home/user/plantuml-mcp/build/libs/plantuml-mcp-1.0.0.jar`.

After saving, enable the `plantuml` server in your client and load a model that
supports tool calling.

You can also run the server manually to check it starts correctly:

```bash
java -jar build/libs/plantuml-mcp-1.0.0.jar
```

In stdio mode the process stays running and waits silently on standard input;
it does not return to the prompt. This is the expected behaviour. Stop it with
Ctrl+C.

## Streamable HTTP transport

In HTTP mode the server runs as a normal web application that can serve multiple
clients over the network. Start it with the `http` profile:

```bash
java -jar build/libs/plantuml-mcp-1.0.0.jar --spring.profiles.active=http
```

By default it listens on:

```
http://localhost:8080/mcp
```

The port can be changed in `src/main/resources/application-http.yml` (the
`server.port` property).

### Client configuration

For an MCP client that supports Streamable HTTP, point it at the endpoint URL:

```json
{
  "mcpServers": {
    "plantuml-http": {
      "url": "http://localhost:8080/mcp"
    }
  }
}
```

### Verifying the endpoint

The Streamable HTTP endpoint cannot be tested by opening the URL in a browser:
a browser does not send the required `Accept` header, and the server responds
with `Invalid Accept header. Expected TEXT_EVENT_STREAM`. This rejection means
the endpoint is alive; it is just refusing a request that does not speak MCP.

To send a valid initialization request (PowerShell):

```powershell
curl -Method POST http://localhost:8080/mcp `
  -Headers @{ "Content-Type" = "application/json"; "Accept" = "application/json, text/event-stream" } `
  -Body '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26","capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}}'
```

A `200` response containing a JSON-RPC result and an `Mcp-Session-Id` header
confirms the transport is working.

For interactive testing, the MCP Inspector handles the session mechanics for you
and lets you call tools by hand:

```bash
npx @modelcontextprotocol/inspector
```

Point it at `http://localhost:8080/mcp` using the Streamable HTTP transport.

### Client compatibility

Not all MCP clients support Streamable HTTP yet. A client that only offers a
`command`/`args` entry (rather than a `url` entry) speaks stdio only. For such
clients, either use the stdio transport directly, or put a bridge such as the
`mcp-remote` npm package in front of the HTTP server.

## Usage

Once the server is connected (over either transport), ask the model a question
that triggers the tool. For example:

> Which version of PlantUML is available?

The model calls the `plantuml_version` tool and returns the embedded PlantUML
version string.

## Notes

- In stdio mode, standard output is reserved for the MCP protocol. All logging
  is therefore redirected to a file (`plantuml-mcp.log`) so it does not corrupt
  the JSON-RPC stream. A harmless `Empty or null pattern` warning may appear at
  startup and can be ignored. In HTTP mode, standard output is not a transport
  channel, so normal console logging is restored.
- When a client launches the server in stdio mode, the log file is created in
  the client's working directory, not necessarily next to the JAR.

## License

See [LICENSE](LICENSE).
