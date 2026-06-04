package com.afet.koordinasyon.client.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AfadEventDto {

    // AFAD farklı sürümlerde farklı casing kullanabilir
    @JsonProperty("eventID")
    @JsonAlias({"eventid", "EventID", "event_id", "eventId", "EVENTID"})
    private String eventId;

    @JsonProperty("location")
    @JsonAlias({"Location", "LOCATION", "yer", "Yer", "YER", "eventLocation", "EventLocation", "description", "Description"})
    private String location;

    @JsonProperty("latitude")
    @JsonAlias({"Latitude", "lat", "Lat"})
    private Double latitude;

    @JsonProperty("longitude")
    @JsonAlias({"Longitude", "lon", "Lon", "lng", "Lng"})
    private Double longitude;

    @JsonProperty("depth")
    @JsonAlias({"Depth", "DEPTH"})
    private Double depth;

    @JsonProperty("type")
    @JsonAlias({"Type", "TYPE"})
    private String type;

    @JsonProperty("magnitude")
    @JsonAlias({"Magnitude", "mag", "Mag", "MAGNITUDE"})
    private Double magnitude;

    @JsonProperty("country")
    @JsonAlias({"Country", "COUNTRY"})
    private String country;

    @JsonProperty("province")
    @JsonAlias({"Province", "PROVINCE", "il", "Il", "IL", "city", "City", "CITY"})
    private String province;

    @JsonProperty("district")
    @JsonAlias({"District", "DISTRICT", "ilce", "Ilce", "ILCE", "town", "Town", "TOWN"})
    private String district;

    @JsonProperty("neighbourhood")
    @JsonAlias({"neighborhood", "Neighbourhood", "NEIGHBOURHOOD"})
    private String neighbourhood;

    // AFAD farklı sürümlerde "date", "Date", "datetime", "time" kullanabilir
    @JsonProperty("date")
    @JsonAlias({"Date", "DATE", "datetime", "Datetime", "time", "Time", "eventDate"})
    private String date;
}
