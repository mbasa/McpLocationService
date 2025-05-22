# MCP Location Services for LLM

This Model Context Protocol (MCP) Server allows a LLM to call [pgGeocoder](http://github.com/mbasa/pgGeocoder) Japanese Geocoder to `geocode` addresses as well as `reverse geocode` coordinates. It also allows the LLM to call [pgrServer](https://github.com/mbasa/pgrServer)(a fast Routing service) to return the `driving distance` and the `path` between two coordinates after performing a Dijkstra shortest path search.   

### Installation

* Download Claude AI Desktop

* Download the latest ***mcpLocation.jar***

* Ensure that JDK 17 or newer is installed

* Integrate the MCP Service into Claude Desktop by going to Settings -> Developer -> Edit Config and open `claude_desktop_config.json` file in a text editor

* Copy the text below and paste into the json file

```json
{
    "mcpServers": {
      "LocationServices": {
        "command": "<FULL PATH>/java",
        "args": [
          "-Dspring.ai.mcp.server.stdio=true",
          "-Dspring.main.web-application-type=none",
          "-Dlogging.pattern.console=",
          "-Dai.model.base.url=http://mb.georepublic.info",
          "-jar",
          "<FULL PATH>/mcpLocation.jar"
        ]
      }
    }
}
```

* Replace `<FULL PATH>` with the correct full paths of both the java application and the downloaded mcpLocation.jar file 

* Save then restart Claude Desktop

* After restaring the Claude Desktop, the tools button should display the available LocationServices tools.

![image](pics/tools.png)

* From here, it is now possible to request the Claude AI to use the registered tools with prompts such as these: 

```text
get the Japanese addresses of Nihonbashi Takashimaya and Shinjuku Takashimaya then geocode the addresses. Display the full geocoded information and get the driving distance between the two coordinates.
```

and 

```text
reverse geocode the Lat/Lng Coordinate 35.68125852, 139.773173143 and display the returned information
```

### Building the application

To build the jar file from source, issue the maven command:

```
mvn clean install
```