package twetailer.console.golf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

import twetailer.dto.Entity;
import twetailer.dto.Location;

/**
 * Wrapper of the Location class to place a customisable name in place of {postalCode; countryCode}
 *
 * @author Dom Derrien
 */
public class City implements Comparable<City> {
    private Long key;
    private String postalCode;
    private String countryCode;
    private static String defaultName;
    private String name;

    private static final String EMPTY = "";
    private Pattern separator = Pattern.compile("\\s+");

    private City() {}

    public City(Long key, String postalCode, String countryCode, String name) {
        setKey(key);
        setPostalCode(postalCode);
        setCountryCode(countryCode);
        setName(name);
    }

    private static final String DEFAULT_NAME = "<?>";

    public String toString() { return getName(); }

    public Long getKey() { return key; }
    public String getPostalCode() { return postalCode; }
    public String getCountryCode() { return countryCode; }
    public static String getDefaultName() {
        return defaultName == null ? DEFAULT_NAME : defaultName;
    }
    public String getName() {
        if (name == null) {
            if (getPostalCode() == null || getCountryCode() == null) {
                return getDefaultName();
            }
            return getPostalCode() + " " + getCountryCode();
        }
        return name;
    }

    public void setKey(Long key) { this.key = key; }
    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode == null ? null : separator.matcher(postalCode).replaceAll(EMPTY);
    }
    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode == null ? null : separator.matcher(countryCode).replaceAll(EMPTY);
    }
    public static void setDefaultName(String defaultName) { City.defaultName = defaultName; }
    public void setName(String name) { this.name = name; }

    /**
     * Helper used to sort a Collection by a call to its sort() method
     *
     * @param other Other instance to compare to
     * @return -1 if the other city has a name prior to the current one in the alphabetical order
     *          0 if the city names are equal
     *          1 otherwise
     */
    public int compareTo(City other) {
        return name.compareTo(((City) other).name);
    }

    // Used by the List<City>.indexOf() method
    @Override
    public boolean equals(Object other) {
        if (this == other) { return true; }
        if (!(other instanceof City)) { return false; }
        City otherCity = (City) other;
        if (key != null && key != 0L && otherCity.key != null && otherCity.key != 0L) {
            return key.equals(otherCity.key);
        }
        return false;
        // if (!postalCode.equals(otherCity.postalCode)) { return false; }
        // return countryCode.equals(otherCity.countryCode);
    }

    /**
     * To update the city list with new locations
     *
     * @param locations Information about Locations just received from the back-end
     */
    public static void consolidate(JSONArray locations) {

        // The City object can be reused if the corresponding Location is already registered
        City newCity = new City();

        // Process all locations
        int locationIdx = locations.length();
        while (0 < locationIdx) {
            locationIdx --;

            // Get a location and update the City object with its key
            JSONObject location = locations.optJSONObject(locationIdx);
            newCity.setKey(location.optLong(Entity.KEY));

            // Check if the corresponding city is already in the list
            Integer existingCityIdx = lookupCityIndex(newCity);

            // Add the new city to the list
            if (existingCityIdx == null) {
                // Update the city object because the location was not found
                newCity.setPostalCode(location.optString(Location.POSTAL_CODE));
                newCity.setCountryCode(location.optString(Location.COUNTRY_CODE));

                // Add the new city to the local stores
                keepCity(newCity, false);

                // Prepare a new object for the next location to process
                if (locationIdx != 0) {
                    newCity = new City();
                }
            }

            // Update the existing city if needed
            if (existingCityIdx != null) {
                City existingCity = cities.get(existingCityIdx);
                if (existingCity.getPostalCode() == null || existingCity.getCountryCode() == null) {
                    // Update the city object because the location was not found
                    existingCity.setPostalCode(location.optString(Location.POSTAL_CODE));
                    existingCity.setCountryCode(location.optString(Location.COUNTRY_CODE));
                }
            }
        }
    }

    /**
     * Helper adding the given city into the list of tracked ones
     *
     * @param city City information to keep an eye on
     * @param prepend if <code>true</code> the city if put at the first place, otherwise it's just appended.
     * @return position of the just added city
     */
    public static int keepCity(City city, boolean prepend) {
        if (prepend) {
            lookupIndexes.clear(); // Reset the cache because the indexes are all changed
            lookupIndexes.put(city.getKey(), 0);
            cities.add(0, city);
            return 0;
        }
        int position = cities.size(); // Before insertion because it's zero-based
        lookupIndexes.put(city.getKey(), position);
        cities.add(city);
        return position;
    }

    /**
     * Try to the index of a customised city that match the given one
     *
     * @param proposal Incomplete city description, with probably no name
     * @return The index of a corresponding city or <code>null</code>
     *
     * @see City#lookupCity(City)
     */
    public static Integer lookupCityIndex(City proposal) {
        Integer selectedIdx = lookupIndexes.get(proposal.getKey());
        if (selectedIdx == null) {
            selectedIdx = cities.indexOf(proposal);
            if (selectedIdx.intValue() != -1) {
                lookupIndexes.put(proposal.getKey(), selectedIdx);
            }
            else {
                selectedIdx = null;
            }
        }
        return selectedIdx;
    }

    /**
     * Try to match the given city among the ones customised for the user
     *
     * @param proposal Incomplete city description, with probably no name
     * @return The given city or a corresponding one possibly customised
     *
     * @see City#lookupCityIndex(City)
     */
    public static City lookupCity(City proposal) {
        Integer proposalIdx = lookupCityIndex(proposal);
        if (proposalIdx == null) {
            proposalIdx = keepCity(proposal, false);
        }
        return cities.get(proposalIdx);
    }

    private static Map<Long, Integer> lookupIndexes = new HashMap<Long, Integer>();
    private static List<City> cities = new ArrayList<City>();
    static {
        /* Temporary hack 1 -- start
         * ========================= */
        /** /
        cities.add(new City(387001L, "H3H1A2", "CA", "Montréal"));
        cities.add(new City(388001L, "J4J3K7", "CA", "Longueuil"));
        cities.add(new City(389001L, "J8E1T1", "CA", "Mont Tremblant"));
        cities.add(new City(390001L, "J6J1Z6", "CA", "Châteauguay"));
        cities.add(new City(391001L, "J4B8L1", "CA", "Boucherville"));
        cities.add(new City(387002L, "H7M5C8", "CA", "Laval"));
        /**/
        cities.add(new City(954L, "H3H1A2", "CA", "Montréal"));
        cities.add(new City(955L, "J4J3K7", "CA", "Longueuil"));
        cities.add(new City(956L, "J8E1T1", "CA", "Mont Tremblant"));
        cities.add(new City(957L, "J6J1Z6", "CA", "Châteauguay"));
        cities.add(new City(958L, "J4B8L1", "CA", "Boucherville"));
        cities.add(new City(959L, "H7M5C8", "CA", "Laval"));
        /**/
        /* =======================
         * Temporary hack 1 -- end */

        Collections.sort(cities);
    }

    /**
     * Accessor on the list of registered cities.
     * Be carefull to not manipulate it directly!
     *
     * @return List of registered cities
     */
    public static List<City> getRegisteredCities() {
        return cities;
    }
}

