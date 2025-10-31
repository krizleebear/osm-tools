package net.leberfinger.osm.nominatim;

/**
 * https://wiki.openstreetmap.org/wiki/Tag:boundary%3Dadministrative#10_admin_level_values_for_specific_countries
 */
public enum AdminLevel {
	UNKNOWN("addr:unknown", 0),
    COUNTRY("addr:country", 2),
    ADMINLVL_3("addr:adminlvl_3", 3),
    STATE("addr:state", 4),
    STATE_DISTRICT("addr:state_district", 5),
    COUNTY("addr:county", 6),
    AMT("addr:amt", 7),
    CITY("addr:city", 8),
    CITY_DISTRICT("addr:city_district", 9),
    CITY_DISTRICT_10("addr:city_district", 10),
    CITY_DISTRICT_11("addr:city_district_11", 11),
    CITY_DISTRICT_12("addr:city_district_12", 12),
    CITY_DISTRICT_13("addr:city_district_13", 13),
    CITY_DISTRICT_14("addr:city_district_14", 14),
    CITY_DISTRICT_15("addr:city_district_15", 15),
    CITY_DISTRICT_16("addr:city_district_16", 16),
    DEPENDENCY("addr:dependency", -1); // Special case for dependencies
	
    
    private final String addressElement;
    private final int osmAdminLevel;
    
    AdminLevel(String addressElement, int osmAdminLevel) {
        this.addressElement = addressElement;
        this.osmAdminLevel = osmAdminLevel;
    }
    
    public String getAddressElement() {
        return addressElement;
    }
    
    public int getOsmAdminLevel() {
        return osmAdminLevel;
    }
    
    /**
     * Get AdminLevel from OSM integer admin level
     */
    public static AdminLevel fromOsmAdminLevel(int adminLevel) {
        for (AdminLevel level : values()) {
            if (level.osmAdminLevel == adminLevel) {
                return level;
            }
        }
        throw new RuntimeException("Unknown admin level " + adminLevel);
    }
    
    /**
     * Get AdminLevel from Overture Maps subtype
     * https://docs.overturemaps.org/schema/reference/divisions/division_area/
     */
    public static AdminLevel fromOvertureSubtype(String subtype) {
        switch (subtype) {
        case "country":
            return COUNTRY;
        case "dependency":
            return DEPENDENCY;
        case "region":
            return STATE;
        case "county":
            return COUNTY;
        case "localadmin":
            return CITY;
        case "locality":
            return CITY;
        case "macrohood":
            return CITY_DISTRICT;
        case "neighborhood":
            return CITY_DISTRICT;
        default:
            throw new RuntimeException("Unknown Overture subtype " + subtype);
        }
    }
    
    /**
     * Get address element string from OSM admin level
     */
    public static String getAddressElementForOsmAdminLevel(int adminLevel) {
        return fromOsmAdminLevel(adminLevel).getAddressElement();
    }
    
    /**
     * Get address element string from Overture subtype
     */
    public static String getAddressElementForOvertureSubtype(String subtype) {
        return fromOvertureSubtype(subtype).getAddressElement();
    }
}