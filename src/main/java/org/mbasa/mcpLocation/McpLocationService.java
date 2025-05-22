package org.mbasa.mcpLocation;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@Service
public class McpLocationService {

    @Value("${ai.model.base.url:http://mb.georepublic.info}")
    private String GC_BASE_URL;
    private RestClient gcRestClient;

    public McpLocationService() {
    }

    private RestClient getRestClient() {
        if (this.gcRestClient == null) {
            this.gcRestClient = RestClient.builder()
                    .baseUrl(GC_BASE_URL)
                    .defaultHeader("Accept", "application/json;charset=UTF-8")
                    .build();
        }
        return this.gcRestClient;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DriveTimeParams(
            double source_lat, double source_lng, double target_lat, double target_lng) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DriveRoute(@JsonProperty("properties") Props properties) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Props(@JsonProperty("feat_length") double feat_length, @JsonProperty("fid") int fid) {
        }
    }

    @Tool(description = "Geocode input Japanese address")
    public String geocodeAddress(@ToolParam(description = "Japanese address to be geocoded") String address) {

        String retVal = getRestClient().get().uri("/geocoderService/service/geocode/json/{address}", address).retrieve()
                .body(String.class);

        return retVal;
    }

    @Tool(description = "Reverse Geocode Latitude, Longitude coordinates")
    public String reverseGeocode(
            @ToolParam(description = "reverse geocde the latitude/longitude points") double latitude,
            double longitude) {

        String retVal = getRestClient().get()
                .uri("/geocoderService/service/reversegeocode/json/{lon},{lat}",
                        longitude, latitude)
                .retrieve()
                .body(String.class);

        return retVal;
    }

    @Tool(description = "Driving Distance in meters between two Latitude, Longitude coordinates")
    public String drivingDistance(DriveTimeParams drp) {

        DriveRoute dr = getRestClient().get().uri(
                "/pgrServer/api/latlng/dijkstra?source_x={source_x}8&source_y={source_y}&target_x={target_x}&target_y={target_y}",
                drp.source_lng, drp.source_lat, drp.target_lng, drp.target_lat)
                .retrieve().body(DriveRoute.class);

        return "{\"distance_in_meters\" : " + dr.properties().feat_length() + "}";
    }

    @Tool(description = "Shortest Path between two Latitude, Longitude coordinates")
    public String shortestPath(DriveTimeParams drp) {

        String retval = getRestClient().get().uri(
                "/pgrServer/api/latlng/dijkstra?source_x={source_x}8&source_y={source_y}&target_x={target_x}&target_y={target_y}",
                drp.source_lng, drp.source_lat, drp.target_lng, drp.target_lat)
                .retrieve().body(String.class);

        return retval;
    }

    public static void main(String[] args) {
        McpLocationService client = new McpLocationService();
        System.out.println(client.geocodeAddress("杉並区清水１−３−１４"));
        System.out.println(client.reverseGeocode(35.710788822, 139.620139631));
        System.out.println(client.drivingDistance(new DriveTimeParams(35.689627, 139.691778,
                35.608323, 140.105996)));
    }

}
