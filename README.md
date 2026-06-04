# plantuml-mcp

Model Context Protocol (MCP) server for [PlantUML](https://plantuml.com).

It exposes PlantUML capabilities as tools that AI assistants and LLM clients
(such as LM Studio or Claude) can call directly.

> **Status: MVP.** This is an early minimum viable product. For now it provides
> a single tool that returns the embedded PlantUML version. Its purpose is to
> validate the full chain — build, stdio transport, and tool discovery by the
> client — before richer tools such as diagram rendering and syntax validation
> are added.

## Requirements

- Java 21 or later (the build targets Java 21 by default).
- An MCP-capable client, for example LM Studio.

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

## Configuration in `mcp.json`

The server communicates over the **stdio** transport: the client launches it as
a subprocess and exchanges JSON-RPC messages over standard input/output. Add the
following entry to your client's `mcp.json`, adjusting the path to the JAR:

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

## Usage

Once the server is enabled, ask the model a question that triggers the tool. For
example:

> Which version of PlantUML is available?

The model calls the `plantuml_version` tool and returns the embedded PlantUML
version string.

## Notes

- In stdio mode, standard output is reserved for the MCP protocol. All logging
  is therefore redirected to a file (`plantuml-mcp.log`) so it does not corrupt
  the JSON-RPC stream. A harmless `Empty or null pattern` warning may appear at
  startup and can be ignored.
- When the client launches the server, the log file is created in the client's
  working directory, not necessarily next to the JAR.

## License

See [LICENSE](LICENSE).
