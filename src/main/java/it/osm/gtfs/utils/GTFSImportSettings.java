/**
 * Licensed under the GNU General Public License version 3
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.gnu.org/licenses/gpl-3.0.html
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package it.osm.gtfs.utils;

import it.osm.gtfs.plugins.DefaultPlugin;
import it.osm.gtfs.plugins.GTFSPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Properties;

import static org.fusesource.jansi.Ansi.ansi;

public class GTFSImportSettings {
    public static final String GTFS_STOP_FILE_NAME = "stops.txt";
    public static final String GTFS_STOP_TIMES_FILE_NAME = "stop_times.txt";
    public static final String GTFS_ROUTES_FILE_NAME = "routes.txt";
    public static final String GTFS_SHAPES_FILE_NAME = "shapes.txt";
    public static final String GTFS_TRIPS_FILE_NAME = "trips.txt";
    public static final String GTFS_FEED_INFO_FILE_NAME = "feed_info.txt";

    //public static final String OSM_OVERPASS_XAPI_SERVER = "http://overpass.osm.rambler.ru/cgi/xapi?"; //vecchia xapi

    public static final String OSM_API_SERVER = "https://www.openstreetmap.org/api/0.6/";
    public static final String OSM_RELATIONS_FILE_NAME = "relations.osm";
    public static final String OSM_STOPS_FILE_NAME = "stops.osm";
    public static final String OUTPUT_MATCHED_WITH_UPDATED_METADATA = "gtfs_import_matched_with_updated_metadata.osm";
    public static final String OUTPUT_NOT_MATCHED_STOPS = "gtfs_import_not_matched_stops.osm";
    public static final String OUTPUT_NEW_STOPS_FROM_GTFS = "gtfs_import_new_stops_from_gtfs.osm";

    public static final String OSM_OVERPASS_WAYS_FILE_NAME = "overpassways.osm";
    public static final String PROPERTIES_FILE_NAME = "gtfs-import.properties";
    public static final String DUMMY_PROPERTIES_FILE_NAME = PROPERTIES_FILE_NAME + ".dummy";

    public static final String REVISED_KEY = "import:revised";

    //properties file data
    private final Properties properties;

    private String overpassApiServer = null;
    private String gtfsZipUrl = null;
    private String outputPath = null;
    private GTFSPlugin plugin = null;
    private String operator = null;
    private String network = null;
    private boolean useRevisedKey = true;


    private GTFSImportSettings() {

        System.out.println("\nLoading config properties...");

        properties = new Properties();

        try {

            if (Objects.requireNonNull(getClass().getResource("")).toString().startsWith("jar:")) {

                File propfile = new File(PROPERTIES_FILE_NAME);

                if (!propfile.exists()) {
                    System.out.println(ansi().render("\n @|yellow No properties file found, looks like this is a fresh start! \n Creating new gtfs-import.properties file into current directory...|@"));

                    InputStream dummyprops = getClass().getClassLoader().getResourceAsStream(DUMMY_PROPERTIES_FILE_NAME);

                    assert dummyprops != null;
                    Files.copy(dummyprops, propfile.toPath());

                    System.out.println(ansi().render("\n @|yellow A new properties file has been copied to the current directory! \n Check it before restarting the tool && before making any operation!|@"));

                    System.exit(0);
                }

                properties.load(new FileInputStream(propfile));

            } else {
                properties.load(getClass().getClassLoader().getResourceAsStream(PROPERTIES_FILE_NAME));
            }


            readProperties();


            System.out.println(ansi().render("@|green Config properties loaded successfully.|@"));

        } catch (Exception e) {
            throw new IllegalArgumentException("An error occurred while loading config properties: " + e.getMessage());
        }
    }

    public static void init() {
        SettingsHolder.INSTANCE = new GTFSImportSettings();
    }

    public String getOverpassApiServer() {
        return overpassApiServer;
    }

    private static class SettingsHolder {
        private static GTFSImportSettings INSTANCE;
    }

    public static GTFSImportSettings getInstance() {
        return SettingsHolder.INSTANCE;
    }

    private void readProperties() {

        //gtfs_zip_url value
        synchronized (this) {
            gtfsZipUrl = properties.getProperty("gtfs_zip_url");
            if (gtfsZipUrl == null)
                throw new IllegalArgumentException("Please set a valid gtfs_zip_url value.");
        }

        //output_path value
        synchronized (this) {
            outputPath = properties.getProperty("output_path");
            if (outputPath == null)
                throw new IllegalArgumentException("Please set a valid output_path value.");
            if (!outputPath.endsWith(File.separator))
                outputPath = outputPath + File.separator;
            if (!new File(outputPath).isDirectory())
                throw new IllegalArgumentException("Please set a valid output_path value.");
        }

        //plugin class value
        synchronized (this) {
            String pluginName = properties.getProperty("plugin");
            if (pluginName == null) {
                plugin = new DefaultPlugin();
            } else {
                try {
                    Class<?> pluginClass = Class.forName(pluginName);
                    boolean validPlugin = false;
                    for (Class<?> c : pluginClass.getInterfaces()) {
                        if (c.equals(GTFSPlugin.class)) {
                            validPlugin = true;
                            break; //termino il for loop perchè il plugin è stato trovato e non ha senso continuare con il cliclo anche se corto
                        }
                    }
                    if (validPlugin)
                        plugin = (GTFSPlugin) pluginClass.getDeclaredConstructor().newInstance();
                    else
                        throw new IllegalArgumentException("The specified plugin was not found or is not valid");
                } catch (Exception e) {
                    throw new IllegalArgumentException("The specified plugin was not found or is not valid");
                }
            }
        }


        //operator value
        synchronized (this) {
            operator = properties.getProperty("operator");
            if (operator == null)
                throw new IllegalArgumentException("Please set a valid operator value.");
        }


        //network value
        synchronized (this) {
            network = properties.getProperty("network");
            if (network == null)
                throw new IllegalArgumentException("Please set a valid network value.");
        }

        //revised_key value
        String tempUseRevisedKey = properties.getProperty("revised_key");
        if (tempUseRevisedKey == null) {
            throw new IllegalArgumentException("Please set a revised_key value.");
        } else useRevisedKey = !tempUseRevisedKey.equals("false");

        //overpass_api_server value
        synchronized (this) {
            overpassApiServer = properties.getProperty("overpass_api_server");
            if (overpassApiServer == null)
                throw new IllegalArgumentException("Please set a valid overpass_api_server value.");
        }
    }

    public String getCachePath() {
        return getOutputPath() + File.separator + "cache" + File.separator;
    }

    public String getOsmDataPath() {
        return getCachePath() + "osmdata" + File.separator;
    }

    public String getGTFSDataPath() {
        return getCachePath() + "gtfsdata" + File.separator;
    }

    public String getOsmRelationsFilePath() {
        return getOsmDataPath() + OSM_RELATIONS_FILE_NAME;
    }

    public String getOsmStopsFilePath() {
        return getOsmDataPath() + OSM_STOPS_FILE_NAME;
    }

    public String getOsmOverpassWaysFilePath() {
        return getOsmDataPath() + OSM_OVERPASS_WAYS_FILE_NAME;
    }

    public String getFullRelsOutputPath() {
        return getOutputPath() + "fullrelations";
    }

    public String getGTFSZipUrl() {
        return gtfsZipUrl;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public GTFSPlugin getPlugin() {
        return plugin;
    }

    public String getOperator() {
        return operator;
    }

    public String getNetwork() {
        return network;
    }

    public boolean useRevisedKey() {
        return useRevisedKey;
    }

}
