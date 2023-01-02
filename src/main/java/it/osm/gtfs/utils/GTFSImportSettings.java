/**
 Licensed under the GNU General Public License version 3
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.gnu.org/licenses/gpl-3.0.html

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 **/
package it.osm.gtfs.utils;

import it.osm.gtfs.plugins.DefaultPlugin;
import it.osm.gtfs.plugins.GTFSPlugin;

import java.io.File;
import java.util.Properties;

public class GTFSImportSettings {
    public static final String GTFS_STOP_FILE_NAME = "stops.txt";
    public static final String GTFS_STOP_TIME_FILE_NAME = "stop_times.txt";
    public static final String GTFS_ROUTES_FILE_NAME = "routes.txt";
    public static final String GTFS_SHAPES_FILE_NAME = "shapes.txt";
    public static final String GTFS_TRIPS_FILE_NAME = "trips.txt";

    /* instanze disponibili di Overpass: https://wiki.openstreetmap.org/wiki/Overpass_API
    meglio usare l'instanza di Overpass russa perchè non ha forme di rate limiting */
    public static final String OSM_OVERPASS_API_SERVER = "https://maps.mail.ru/osm/tools/overpass/api/interpreter?";
    //public static final String OSM_OVERPASS_XAPI_SERVER = "http://overpass.osm.rambler.ru/cgi/xapi?"; //vecchia xapi

    public static final String OSM_API_SERVER =  "https://www.openstreetmap.org/api/0.6/";
    public static final String OSM_RELATIONS_FILE_NAME = "relations.osm";
    public static final String OSM_RELATIONS_FILE_PATH = getInstance().getOsmDataPath() + OSM_RELATIONS_FILE_NAME;
    public static final String OSM_STOP_FILE_NAME = "stops.osm";
    public static final String OSM_STOP_FILE_PATH = getInstance().getOsmDataPath() + OSM_STOP_FILE_NAME;
    public static final String OUTPUT_MATCHED_WITH_UPDATED_METADATA = "gtfs_import_matched_with_updated_metadata.osm";
    public static final String OUTPUT_NOT_MATCHED_STOPS = "gtfs_import_not_matched_stops.osm";
    public static final String OUTPUT_NEW_STOPS_FROM_GTFS = "gtfs_import_new_stops_from_gtfs";

    private final Properties properties;

    private GTFSImportSettings() {
        properties = new Properties();
        try {
            properties.load(getClass().getClassLoader().getResourceAsStream("gtfs-import.properties"));
        } catch (Exception e) {
            throw new IllegalArgumentException("An error occurred while reading setting: " + e.getMessage());
        }
    }

    private static class SettingsHolder {
        private final static GTFSImportSettings INSTANCE = new GTFSImportSettings();
    }

    public static GTFSImportSettings getInstance() {
        return SettingsHolder.INSTANCE;
    }

    private String gtfsPath = null;
    public String getGTFSPath() {
        if (gtfsPath == null){
            synchronized (this) {
                gtfsPath = properties.getProperty("gtfs_path");
                if (gtfsPath == null)
                    throw new IllegalArgumentException("Please set a valid gtfs-path.");
                if (!gtfsPath.endsWith(File.separator))
                    gtfsPath = gtfsPath + File.separator;
                if (!new File(gtfsPath).isDirectory())
                    throw new IllegalArgumentException("Please set a valid gtfs-path.");
            }
        }
        return gtfsPath;
    }

    private String outputPath = null;
    public String getOutputPath() {
        if (outputPath == null){
            synchronized (this) {
                outputPath = properties.getProperty("output_path");
                if (outputPath == null)
                    throw new IllegalArgumentException("Please set a valid output-path.");
                if (!outputPath.endsWith(File.separator))
                    outputPath = outputPath + File.separator;
                if (!new File(outputPath).isDirectory())
                    throw new IllegalArgumentException("Please set a valid output-path.");
            }
        }
        return outputPath;
    }

    public String getCachePath() {
        return getOutputPath() + File.separator + "cache" + File.separator;
    }

    public String getOsmDataPath() {
        return getOutputPath() + File.separator + "osmdata" + File.separator;
    }

    private GTFSPlugin plugin = null;
    public GTFSPlugin getPlugin(){
        if (plugin == null){
            synchronized (this) {
                String pluginName = properties.getProperty("plugin");
                if (pluginName == null){
                    plugin = new DefaultPlugin();
                }else{
                    try{
                        Class<?> pluginClass  = Class.forName(pluginName);
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
                            throw new IllegalArgumentException("The specified plugin is not found or not valid");
                    }catch (Exception e) {
                        throw new IllegalArgumentException("The specified plugin is not found or not valid");
                    }
                }
            }
        }
        return plugin;
    }

    private String operator = null;
    public String getOperator() {
        if (operator == null){
            synchronized (this) {
                operator = properties.getProperty("operator");
                if (operator == null)
                    throw new IllegalArgumentException("Please set a valid operator.");
            }
        }
        return operator;
    }

    private String network = null;
    public String getNetwork() {
        if (network == null){
            synchronized (this) {
                network = properties.getProperty("network");
                if (network == null)
                    throw new IllegalArgumentException("Please set a valid network.");
            }
        }
        return network;
    }

    private String revisedKey = null;
    public String getRevisedKey() {
        if (revisedKey == null){
            synchronized (this) {
                revisedKey = properties.getProperty("revised_key");
                if (revisedKey == null)
                    throw new IllegalArgumentException("Please set a valid operator.");
            }
        }
        return revisedKey;
    }


}
