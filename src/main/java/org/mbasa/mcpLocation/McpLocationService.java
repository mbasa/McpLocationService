package org.mbasa.mcpLocation;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashSet;
import java.util.Set;

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
        public record DrivePolyParams(
                        double latitude, double longitude, double radius) {
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record DriveDistParams(
                        double source_lat, double source_lng, double target_lat, double target_lng) {
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record DriveRoute(@JsonProperty("properties") Props properties) {

                @JsonIgnoreProperties(ignoreUnknown = true)
                public record Props(@JsonProperty("feat_length") double feat_length, @JsonProperty("fid") int fid) {
                }
        }

        @Tool(description = "Japanese 500m Mesh Census data which will be searched from a Latitude, Longitude coordinate parameter with a Radius in meters. Returns CSV with a header row; the mesh polygon geometry is encoded as WKT in the WKT column. Returns the message \"No results found for the given search area.\" instead of CSV if no mesh matches the search area.")
        public String meshCensusData(
                        @ToolParam(description = "Latitude of the center point") double latitude,
                        @ToolParam(description = "Longitude of the center point") double longitude,
                        @ToolParam(description = "Search radius in meters") double radius) {

                String geoJson = this.driveTimePolygon(latitude, longitude, radius);

                String featureCollection = getRestClient().get()
                                .uri("/CensusService/service/census/mesh4?geoJson={geoJson}", geoJson)
                                .retrieve()
                                .body(String.class);

                return geoJsonFeatureCollectionToCsv(featureCollection);
        }

        private String geoJsonFeatureCollectionToCsv(String featureCollectionJson) {
                JsonNode root = new ObjectMapper().readTree(featureCollectionJson);
                JsonNode features = root.get("features");
                if (features.isEmpty()) {
                        return "No results found for the given search area.";
                }

                Set<String> propertyKeys = new LinkedHashSet<>();
                for (JsonNode feature : features) {
                        propertyKeys.addAll(feature.get("properties").propertyNames());
                }

                StringBuilder csv = new StringBuilder("WKT");
                for (String key : propertyKeys) {
                        csv.append(",").append(csvEscape(key));
                }
                csv.append("\n");

                for (JsonNode feature : features) {
                        csv.append(csvEscape(geometryToWkt(feature.get("geometry"))));
                        JsonNode properties = feature.get("properties");
                        for (String key : propertyKeys) {
                                JsonNode value = properties.get(key);
                                csv.append(",").append(value == null || value.isNull() ? "" : csvEscape(value.asText()));
                        }
                        csv.append("\n");
                }

                return csv.toString();
        }

        private String geometryToWkt(JsonNode geometry) {
                String type = geometry.get("type").asText();
                JsonNode coordinates = geometry.get("coordinates");

                switch (type) {
                        case "Point":
                                return "POINT (" + coordinates.get(0).asText() + " " + coordinates.get(1).asText() + ")";
                        case "MultiPoint":
                                return "MULTIPOINT " + ringToWkt(coordinates);
                        case "LineString":
                                return "LINESTRING " + ringToWkt(coordinates);
                        case "MultiLineString":
                                StringBuilder mls = new StringBuilder("MULTILINESTRING (");
                                for (int i = 0; i < coordinates.size(); i++) {
                                        if (i > 0)
                                                mls.append(", ");
                                        mls.append(ringToWkt(coordinates.get(i)));
                                }
                                return mls.append(")").toString();
                        case "Polygon":
                                return "POLYGON " + polygonRingsToWkt(coordinates);
                        case "MultiPolygon":
                                StringBuilder sb = new StringBuilder("MULTIPOLYGON (");
                                for (int i = 0; i < coordinates.size(); i++) {
                                        if (i > 0)
                                                sb.append(", ");
                                        sb.append(polygonRingsToWkt(coordinates.get(i)));
                                }
                                return sb.append(")").toString();
                        default:
                                throw new IllegalArgumentException("Unsupported geometry type: " + type);
                }
        }

        private String polygonRingsToWkt(JsonNode rings) {
                StringBuilder sb = new StringBuilder("(");
                for (int i = 0; i < rings.size(); i++) {
                        if (i > 0)
                                sb.append(", ");
                        sb.append(ringToWkt(rings.get(i)));
                }
                return sb.append(")").toString();
        }

        private String ringToWkt(JsonNode ring) {
                StringBuilder sb = new StringBuilder("(");
                for (int i = 0; i < ring.size(); i++) {
                        if (i > 0)
                                sb.append(", ");
                        JsonNode point = ring.get(i);
                        sb.append(point.get(0).asText()).append(" ").append(point.get(1).asText());
                }
                return sb.append(")").toString();
        }

        private String csvEscape(String value) {
                if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
                        return "\"" + value.replace("\"", "\"\"") + "\"";
                }
                return value;
        }

        @Tool(description = "POI data which will be searched from a Latitude, Longitude coordinate parameter with a Radius in meters. Returns CSV with a header row; the geometry is encoded as WKT in the WKT column. Returns the message \"No results found for the given search area.\" instead of CSV if no POIs are found in the search area.")
        public String poiData(
                        @ToolParam(description = "Latitude of the center point") double latitude,
                        @ToolParam(description = "Longitude of the center point") double longitude,
                        @ToolParam(description = "Search radius in meters") double radius) {

                String geoJson = this.driveTimePolygon(latitude, longitude, radius);

                String featureCollection = getRestClient().get()
                                .uri("/CensusService/service/poi?geoJson={geoJson}", geoJson)
                                .retrieve()
                                .body(String.class);

                return geoJsonFeatureCollectionToCsv(featureCollection);
        }

        @Tool(description = "People Flow (人流) data which will be searched from a Latitude, Longitude coordinate parameter with a Radius in meters. Returns CSV with a header row; the geometry is encoded as WKT in the WKT column. Returns the message \"No results found for the given search area.\" instead of CSV if no data is found in the search area.")
        public String peopleFlowData(
                        @ToolParam(description = "Latitude of the center point") double latitude,
                        @ToolParam(description = "Longitude of the center point") double longitude,
                        @ToolParam(description = "Search radius in meters") double radius) {

                String geoJson = this.driveTimePolygon(latitude, longitude, radius);

                String featureCollection = getRestClient().get()
                                .uri("/CensusService/service/people_flow?geoJson={geoJson}", geoJson)
                                .retrieve()
                                .body(String.class);

                return geoJsonFeatureCollectionToCsv(featureCollection);
        }

        @Tool(description = "Geocode input Japanese address")
        public String geocodeAddress(
                        @ToolParam(description = "Japanese address to geocode") String address) {

                return getRestClient().get()
                                .uri("/geocoderService/service/geocode/json/{address}", address)
                                .retrieve()
                                .body(String.class);
        }

        @Tool(description = "Reverse Geocode Latitude, Longitude coordinates")
        public String reverseGeocode(
                        @ToolParam(description = "Latitude coordinate to reverse geocode") double latitude,
                        @ToolParam(description = "Longitude coordinate to reverse geocode") double longitude) {

                return getRestClient().get()
                                .uri("/geocoderService/service/reversegeocode/json/{lon},{lat}",
                                                longitude, latitude)
                                .retrieve()
                                .body(String.class);
        }

        @Tool(description = "Driving distance in meters between two Latitude, Longitude coordinates")
        public String drivingDistance(
                        @ToolParam(description = "Latitude of the source/starting point") double source_lat,
                        @ToolParam(description = "Longitude of the source/starting point") double source_lng,
                        @ToolParam(description = "Latitude of the target/destination point") double target_lat,
                        @ToolParam(description = "Longitude of the target/destination point") double target_lng) {

                DriveDistParams drp = new DriveDistParams(source_lat, source_lng, target_lat, target_lng);

                DriveRoute dr = getRestClient().get().uri(
                                "/pgrServer/api/latlng/dijkstra?source_x={source_x}&source_y={source_y}&target_x={target_x}&target_y={target_y}",
                                drp.source_lng(), drp.source_lat(), drp.target_lng(), drp.target_lat())
                                .retrieve().body(DriveRoute.class);

                return "{\"distance_in_meters\" : " + dr.properties().feat_length() + "}";
        }

        @Tool(description = "Creates a Drive Time Polygon with a Latitude, Longitude coordinate parameter with a Radius in meters")
        public String driveTimePolygon(
                        @ToolParam(description = "Latitude of the center point") double latitude,
                        @ToolParam(description = "Longitude of the center point") double longitude,
                        @ToolParam(description = "Search radius in meters") double radius) {

                DrivePolyParams drpp = new DrivePolyParams(latitude, longitude, radius);

                return getRestClient().get().uri(
                                "/pgrServer/api/latlng/drivingDistance?source_x={source_x}&source_y={source_y}&radius={radius}",
                                drpp.longitude(), drpp.latitude(), drpp.radius())
                                .retrieve().body(String.class);
        }

        @Tool(description = "Returns k alternative driving paths between two Latitude, Longitude coordinates as GeoJSON. Use this tool only when multiple paths are explicitly requested (i.e. more than one path).")
        public String kShortestPath(
                        @ToolParam(description = "Number of paths to return. Default is 3, maximum is 10.") int k,
                        @ToolParam(description = "Latitude of the source/starting point") double source_lat,
                        @ToolParam(description = "Longitude of the source/starting point") double source_lng,
                        @ToolParam(description = "Latitude of the target/destination point") double target_lat,
                        @ToolParam(description = "Longitude of the target/destination point") double target_lng) {

                int paths = Math.min(Math.max(k < 1 ? 3 : k, 1), 10);
                DriveDistParams drp = new DriveDistParams(source_lat, source_lng, target_lat, target_lng);

                return getRestClient().get().uri(
                                "/pgrServer/api/latlng/kShortestPath?source_x={source_x}&source_y={source_y}&target_x={target_x}&target_y={target_y}&k={k}",
                                drp.source_lng(), drp.source_lat(), drp.target_lng(), drp.target_lat(), paths)
                                .retrieve().body(String.class);
        }

        @Tool(description = "Shortest path between two Latitude, Longitude coordinates")
        public String shortestPath(
                        @ToolParam(description = "Latitude of the source/starting point") double source_lat,
                        @ToolParam(description = "Longitude of the source/starting point") double source_lng,
                        @ToolParam(description = "Latitude of the target/destination point") double target_lat,
                        @ToolParam(description = "Longitude of the target/destination point") double target_lng) {

                DriveDistParams drp = new DriveDistParams(source_lat, source_lng, target_lat, target_lng);

                return getRestClient().get().uri(
                                "/pgrServer/api/latlng/dijkstra?source_x={source_x}&source_y={source_y}&target_x={target_x}&target_y={target_y}",
                                drp.source_lng(), drp.source_lat(), drp.target_lng(), drp.target_lat())
                                .retrieve().body(String.class);
        }

        public static void main(String[] args) {
                McpLocationService client = new McpLocationService();
                System.out.println(client.geocodeAddress("杉並区清水１−３−１４"));
                System.out.println(client.reverseGeocode(35.710788822, 139.620139631));
                System.out.println(client.drivingDistance(35.689627, 139.691778, 35.608323, 140.105996));
        }

}
