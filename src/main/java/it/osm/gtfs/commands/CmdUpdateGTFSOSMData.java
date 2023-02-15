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
package it.osm.gtfs.commands;

import it.osm.gtfs.input.GTFSParser;
import it.osm.gtfs.input.OSMParser;
import it.osm.gtfs.models.BoundingBox;
import it.osm.gtfs.models.GTFSStop;
import it.osm.gtfs.models.OSMStop;
import it.osm.gtfs.models.Relation;
import it.osm.gtfs.utils.DownloadUtils;
import it.osm.gtfs.utils.GTFSImportSettings;
import it.osm.gtfs.utils.OsmosisUtils;
import it.osm.gtfs.utils.StopsUtils;
import org.apache.commons.httpclient.util.URIUtil;
import org.fusesource.jansi.Ansi;
import org.openstreetmap.osmosis.core.pipeline.common.Pipeline;
import org.xml.sax.SAXException;
import picocli.CommandLine;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;

import static org.fusesource.jansi.Ansi.ansi;

@CommandLine.Command(name = "update", mixinStandardHelpOptions = true, description = "Generate/update data from OpenStreetMap")
public class CmdUpdateGTFSOSMData implements Callable<Void> {

    @CommandLine.Option(names = {"-r", "--relation"}, description = "Optional relation ID to generate/update single relation from api server")
    String relation;

    @Override
    public Void call() throws IOException, InterruptedException, ParserConfigurationException, SAXException {

        if (relation == null || relation.isBlank()) {

            File cachedirectory = new File(GTFSImportSettings.getInstance().getCachePath());
            File osmdatadirectory = new File(GTFSImportSettings.getInstance().getOsmDataPath());

            cachedirectory.mkdirs();
            osmdatadirectory.mkdirs();

            updateGTFSData();
            updateBusStops();
            updateBaseRels();
            updateFullRels();

            System.out.println(ansi().fg(Ansi.Color.GREEN).a("GTFS and OSM data update completed.").reset());

        } else {
            StringTokenizer st = new StringTokenizer(relation, " ,\n\t");

            List<String> relationsIDsToUpdate = new ArrayList<>();

            while (st.hasMoreTokens()){
                relationsIDsToUpdate.add(st.nextToken());
            }

            updateFullRels(relationsIDsToUpdate);
        }
        return null;
    }

    private static void updateGTFSData() {
        System.out.println(ansi().fg(Ansi.Color.YELLOW).a("Downloading and extracting GTFS data from " + GTFSImportSettings.getInstance().getGTFSZipUrl() + " ...").reset());
        DownloadUtils.downloadZip(GTFSImportSettings.getInstance().getGTFSZipUrl(), GTFSImportSettings.getInstance().getGTFSDataPath());
    }

    private static void updateBusStops() throws IOException, InterruptedException {

        List<GTFSStop> gtfsStops = GTFSParser.readStops(GTFSImportSettings.getInstance().getGTFSDataPath() + GTFSImportSettings.GTFS_STOP_FILE_NAME);
        BoundingBox bb = new BoundingBox(gtfsStops);

        String queryBusStopsUrl = "data=[bbox];node[highway=bus_stop];out meta;&bbox=" + bb.getAPIQuery();
        File busFileTemp = new File(GTFSImportSettings.getInstance().getCachePath() + "tmp_busstops.osm");
        URIUtil.encodeQuery(queryBusStopsUrl);

        String busStopsUrl = GTFSImportSettings.OSM_OVERPASS_API_SERVER + URIUtil.encodeQuery(queryBusStopsUrl);;

        DownloadUtils.download(busStopsUrl, busFileTemp, false);

        Thread.sleep(1000L);

        String queryStopPositionsUrl = "data=[bbox];node[public_transport=stop_position];out meta;&bbox=" + bb.getAPIQuery();
        File stopPositionsFileTemp = new File(GTFSImportSettings.getInstance().getCachePath() + "tmp_stoppositions.osm");
        String stopPositionsUrl = GTFSImportSettings.OSM_OVERPASS_API_SERVER + URIUtil.encodeQuery(queryStopPositionsUrl);;
        DownloadUtils.download(stopPositionsUrl, stopPositionsFileTemp, false);

        Thread.sleep(1000L);

        String queryTramStopsUrl = "data=[bbox];node[railway=tram_stop];out meta;&bbox=" + bb.getAPIQuery();
        File tramFileTemp = new File(GTFSImportSettings.getInstance().getCachePath() + "tmp_tramstops.osm");
        String tramStopsUrl = GTFSImportSettings.OSM_OVERPASS_API_SERVER + URIUtil.encodeQuery(queryTramStopsUrl);;
        DownloadUtils.download(tramStopsUrl, tramFileTemp, false);

        Thread.sleep(2000L);

        String queryMetroTrainStationsUrl = "data=[bbox];node[railway=station];out meta;&bbox=" + bb.getAPIQuery();
        File metroFileTemp = new File(GTFSImportSettings.getInstance().getCachePath() + "tmp_metrostops.osm");
        String metroTrainStationsUrl = GTFSImportSettings.OSM_OVERPASS_API_SERVER + URIUtil.encodeQuery(queryMetroTrainStationsUrl);;
        DownloadUtils.download(metroTrainStationsUrl, metroFileTemp, false);

        String queryUrlstat = "data=[bbox];node[public_transport=station];out meta;&bbox=" + bb.getAPIQuery();
        File stationsFileTemp = new File(GTFSImportSettings.getInstance().getCachePath() + "tmp_stationstops.osm");
        String urlstat = GTFSImportSettings.OSM_OVERPASS_API_SERVER + URIUtil.encodeQuery(queryUrlstat);;
        DownloadUtils.download(urlstat, stationsFileTemp, false);

        List<File> tempFileList = new ArrayList<>();
        tempFileList.add(busFileTemp);
        tempFileList.add(stopPositionsFileTemp);
        tempFileList.add(tramFileTemp);
        tempFileList.add(metroFileTemp);
        tempFileList.add(stationsFileTemp);

        File finalMergedFileOut = new File(GTFSImportSettings.getInstance().getOsmStopsFilePath());

        OsmosisUtils.checkProcessOutput(OsmosisUtils.runOsmosisMerge(tempFileList, finalMergedFileOut));
    }

    private static void updateBaseRels() throws IOException {
        String queryRel = "data=(relation[network=" + GTFSImportSettings.getInstance().getNetwork() +  "];>;);out%20meta;";
        String urlrel = GTFSImportSettings.OSM_OVERPASS_API_SERVER + URIUtil.encodeQuery(queryRel);;

        File filerel = new File(GTFSImportSettings.getInstance().getCachePath() + "tmp_rels.osm");
        DownloadUtils.download(urlrel, filerel, false);
    }

    private static void updateFullRels() throws ParserConfigurationException, SAXException, IOException {

        updateFullRels(null);
    }

    //todo: we should cleanup the cache relations files before every update i think
    private static void updateFullRels(List<String> osmRelationsIDsToUpdate) throws ParserConfigurationException, SAXException, IOException {

        //TODO: should we really read stops of ANY operator?
        List<OSMStop> osmStops = OSMParser.readOSMStops(GTFSImportSettings.getInstance().getOsmStopsFilePath(), true);
        Map<String, OSMStop> osmIdOSMStopMap = StopsUtils.getOSMIdOSMStopMap(osmStops);

        List<File> sortedfiles = new ArrayList<>();

        // Default to all available rel, then override forced updates
        List<Relation> osmRels = OSMParser.readOSMRelations(new File(GTFSImportSettings.getInstance().getCachePath() +  "tmp_rels.osm"), osmIdOSMStopMap);


        List<Relation> relationsToUpdate = new ArrayList<>();


        for (Relation relation : osmRels){
            File filesorted = new File(GTFSImportSettings.getInstance().getCachePath() + "tmp_s" + relation.getId() + ".osm");


            //if no relations ids are specified, add every relation to the relations to update. otherwise, if the current-loop relation ID is present in the relations to be updated, add it
            if(osmRelationsIDsToUpdate == null || osmRelationsIDsToUpdate.contains(relation.getId())) {

                relationsToUpdate.add(relation);
            }


            if (filesorted.exists()) {
                sortedfiles.add(filesorted);
            }
        }

        Pipeline previousTask = null;

        for (Relation currRelationToUpdate : relationsToUpdate) {
            System.out.println("Processing relation " + currRelationToUpdate.getId());

            File filesorted = new File(GTFSImportSettings.getInstance().getCachePath() + "tmp_s" + currRelationToUpdate.getId() + ".osm");
            sortedfiles.add(filesorted);

            boolean uptodate = false;
            try {
                if (filesorted.exists()){
                    List<Relation> relationInFile = OSMParser.readOSMRelations(filesorted, osmIdOSMStopMap);

                    if (relationInFile.size() > 0 && relationInFile.get(0).getVersion().equals(currRelationToUpdate.getVersion())) //todo: equals will always return false here because they are always different instances, try to find a way to compare the relations in a better way
                        uptodate = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (!uptodate) {
                File filerelation = new File(GTFSImportSettings.getInstance().getCachePath() + "tmp_r" + currRelationToUpdate.getId() + ".osm");
                String url = GTFSImportSettings.OSM_API_SERVER + "relation/" + currRelationToUpdate.getId() + "/full";
                DownloadUtils.download(url, filerelation, false);

                Pipeline current = OsmosisUtils.runOsmosisSort(filerelation, filesorted);
                OsmosisUtils.checkProcessOutput(previousTask);
                previousTask = current;
            }
        }

        OsmosisUtils.checkProcessOutput(previousTask);

        File osmStopsFile = new File(GTFSImportSettings.getInstance().getOsmStopsFilePath());
        File osmRelationsFileOut = new File(GTFSImportSettings.getInstance().getOsmRelationsFilePath());

        File tonyout = new File(GTFSImportSettings.getInstance().getOsmDataPath() + "tony.osm");
        OsmosisUtils.checkProcessOutput(OsmosisUtils.runOsmosisMerge(sortedfiles, tonyout));

        sortedfiles.add(osmStopsFile);

        OsmosisUtils.checkProcessOutput(OsmosisUtils.runOsmosisMerge(sortedfiles, osmRelationsFileOut));

    }
}
