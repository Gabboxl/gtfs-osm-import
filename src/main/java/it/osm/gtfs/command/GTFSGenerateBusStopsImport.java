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
package it.osm.gtfs.command;

import it.osm.gtfs.input.GTFSParser;
import it.osm.gtfs.input.OSMParser;
import it.osm.gtfs.model.BoundingBox;
import it.osm.gtfs.model.Stop;
import it.osm.gtfs.model.Stop.GTFSStop;
import it.osm.gtfs.output.OSMBusImportGenerator;
import it.osm.gtfs.utils.GTFSImportSettings;
import it.osm.gtfs.utils.OSMDistanceUtils;
import it.osm.gtfs.utils.OSMXMLUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;

import org.fusesource.jansi.Ansi;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import picocli.CommandLine;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import static org.fusesource.jansi.Ansi.ansi;


@CommandLine.Command(name = "stops", description = "Generate files to import bus stops into osm merging with existing stops")
public class GTFSGenerateBusStopsImport implements Callable<Void> {

    @CommandLine.Option(names = {"-s", "--small"}, description = "Export to small file")
    boolean smallFileExport;

    @Override
    public Void call() throws IOException, ParserConfigurationException, SAXException, TransformerException {
        List<GTFSStop> gtfsStops = GTFSParser.readBusStop(GTFSImportSettings.getInstance().getGTFSPath() + GTFSImportSettings.GTFS_STOP_FILE_NAME);
        BoundingBox bb = new BoundingBox(gtfsStops);

        List<Stop> osmStops = OSMParser.readOSMStops(GTFSImportSettings.getInstance().getOSMPath() + GTFSImportSettings.OSM_STOP_FILE_NAME);

        for (GTFSStop gtfsStop : gtfsStops){
            for (Stop osmStop : osmStops){
                if (gtfsStop.matches(osmStop)){
                    if (osmStop.isStopPosition()){
                        if (osmStop.stopMatchedWith != null){
                            System.err.println("Mupliple match found.");
                            System.err.println(" OSM: " + osmStop);
                            System.err.println("GTFS: " + gtfsStop);
                            System.err.println(" OSM: " + gtfsStop.railwayMatchedWith);
                            System.err.println("GTFS: " + osmStop.stopMatchedWith);
                            throw new IllegalArgumentException("Multiple match found, this is currently unsupported.");
                        }

                        gtfsStop.stopPositionsMatchedWith.add(osmStop);
                        osmStop.stopMatchedWith = gtfsStop;
                    }else if (osmStop.isRailway()){
                        if (gtfsStop.railwayMatchedWith != null || osmStop.stopMatchedWith != null){
                            System.err.println("Mupliple match found.");
                            System.err.println(" OSM: " + osmStop);
                            System.err.println("GTFS: " + gtfsStop);
                            System.err.println(" OSM: " + gtfsStop.railwayMatchedWith);
                            System.err.println("GTFS: " + osmStop.stopMatchedWith);
                            throw new IllegalArgumentException("Multiple match found, this is currently unsupported.");
                        }

                        gtfsStop.railwayMatchedWith = osmStop;
                        osmStop.stopMatchedWith = gtfsStop;
                    }else{
                        if (gtfsStop.stopMatchedWith != null || osmStop.stopMatchedWith != null){
                            System.err.println("Mupliple match found.");
                            System.err.println(" OSM: " + osmStop);
                            System.err.println("GTFS: " + gtfsStop);
                            System.err.println(" OSM: " + gtfsStop.stopMatchedWith);
                            System.err.println("GTFS: " + osmStop.stopMatchedWith);
                            throw new IllegalArgumentException("Multiple match found, this is currently unsupported.");
                        }

                        gtfsStop.stopMatchedWith = osmStop;
                        osmStop.stopMatchedWith = gtfsStop;
                    }

                }
            }
        }

        //Paired with gtfs_id (also checking stops no longer in GTFS)
        {
            //FIXME: check all tag present
            int paired_with_gtfs_id = 0;
            int osm_with_gtfs_id_not_in_gtfs = 0;
            int osm_with_different_gtfs_id = 0;
            OSMBusImportGenerator bufferNotInGTFS = new OSMBusImportGenerator(bb);
            OSMBusImportGenerator bufferDifferentGTFS = new OSMBusImportGenerator(bb);
            Map<Double, String> messages = new TreeMap<Double, String>();
            for (Stop osmStop:osmStops){
                if (osmStop.stopMatchedWith != null && osmStop.getGtfsId() != null){
                    paired_with_gtfs_id++;
                    double dist = OSMDistanceUtils.distVincenty(osmStop.getLat(), osmStop.getLon(), osmStop.stopMatchedWith.getLat(), osmStop.stopMatchedWith.getLon());
                    if (dist > 5){
                        messages.put(dist, "Stop ref " + osmStop.getCode() +
                                " distance GTFS-OSM: " + OSMDistanceUtils.distVincenty(osmStop.getLat(), osmStop.getLon(), osmStop.stopMatchedWith.getLat(), osmStop.stopMatchedWith.getLon()) + " m");
                    }
                    if (!osmStop.stopMatchedWith.getGtfsId().equals(osmStop.getGtfsId())){
                        osm_with_different_gtfs_id++;
                        Element n = (Element) osmStop.originalXMLNode;
                        OSMXMLUtils.addTagOrReplace(n, "gtfs_id", osmStop.stopMatchedWith.getGtfsId());
                        bufferDifferentGTFS.appendNode(n);
                        System.out.println("OSM Stop node id " + osmStop.getOSMId() + " (ref " + osmStop.getCode() + ")" + " has gtfs_id: " + osmStop.getGtfsId() + " but in GTFS has gtfs_id: " + osmStop.stopMatchedWith.getGtfsId());
                    }
                }else if (osmStop.getGtfsId() != null){
                    osm_with_gtfs_id_not_in_gtfs++;
                    System.out.println("OSM Stop node id " + osmStop.getOSMId() + " (ref " + osmStop.getCode() + ")" + " has gtfs_id: " + osmStop.getGtfsId() + " but the stop ref is no longer present in GTFS (probably the stop got removed/is no longer in function).");
                    Element n = (Element) osmStop.originalXMLNode;
                    bufferNotInGTFS.appendNode(n);
                }
            }
            for(String msg:messages.values())
                System.out.println(msg);

            if (osm_with_different_gtfs_id > 0){
                bufferDifferentGTFS.end();
                bufferDifferentGTFS.saveTo(new FileOutputStream(GTFSImportSettings.getInstance().getOutputPath() + GTFSImportSettings.OUTPUT_PAIRED_WITH_DIFFERENT_GTFS));
                System.out.println(ansi().fg(Ansi.Color.GREEN).a("OSM stops with different gtfs_id (stops with new gtfs_id from GTFS): ").reset().a(osm_with_different_gtfs_id).fg(Ansi.Color.YELLOW).a(" (created josm osm change file to review data: " + GTFSImportSettings.OUTPUT_PAIRED_WITH_DIFFERENT_GTFS + ")"));
            }
            if (osm_with_gtfs_id_not_in_gtfs > 0){
                bufferNotInGTFS.end();
                bufferNotInGTFS.saveTo(new FileOutputStream(GTFSImportSettings.getInstance().getOutputPath() + GTFSImportSettings.OUTPUT_OSM_WITH_GTFSID_NOT_IN_GTFS));
                System.out.println(ansi().fg(Ansi.Color.GREEN).a("OSM stops with gtfs_id not found in GTFS (OLD RIPPED STOPS): ").reset().a(osm_with_gtfs_id_not_in_gtfs).fg(Ansi.Color.YELLOW).a(" (created josm osm change file to review data: " + GTFSImportSettings.OUTPUT_OSM_WITH_GTFSID_NOT_IN_GTFS + ")"));
            }
            System.out.println(ansi().fg(Ansi.Color.GREEN).a("Paired stops with gtfs_id (stops that are already OK): ").reset().a(paired_with_gtfs_id));
        }

        //Paired without gtfs_id
        {
            //FIXME: check all tag present
            int paired_without_gtfs_id = 0;
            OSMBusImportGenerator buffer = new OSMBusImportGenerator(bb);
            for (Stop osmStop:osmStops){
                if (osmStop.stopMatchedWith != null && osmStop.getGtfsId() == null){
                    Element n = (Element) osmStop.originalXMLNode;
                    OSMXMLUtils.addTag(n, "gtfs_id", osmStop.stopMatchedWith.getGtfsId());
                    OSMXMLUtils.addTagIfNotExisting(n, "operator", GTFSImportSettings.getInstance().getOperator());
                    OSMXMLUtils.addTagIfNotExisting(n, GTFSImportSettings.getInstance().getRevisedKey(), "no");
                    OSMXMLUtils.addTagIfNotExisting(n, "name", GTFSImportSettings.getInstance().getPlugin().fixBusStopName(osmStop.stopMatchedWith.getName()));

                    buffer.appendNode(n);

                    paired_without_gtfs_id++;
                }
            }
            if (paired_without_gtfs_id > 0){
                buffer.end();
                buffer.saveTo(new FileOutputStream(GTFSImportSettings.getInstance().getOutputPath() + GTFSImportSettings.OUTPUT_PAIRED_WITHOUT_GTFS));
                System.out.println(ansi().fg(Ansi.Color.GREEN).a("Paired stops without gtfs_id: ").reset().a(paired_without_gtfs_id).fg(Ansi.Color.YELLOW).a(" (created josm osm change file to import data: " + GTFSImportSettings.OUTPUT_PAIRED_WITHOUT_GTFS + ")"));
            }else{
                System.out.println(ansi().fg(Ansi.Color.GREEN).a("Paired stops without gtfs_id: ").reset().a(paired_without_gtfs_id));
            }
        }

        //new stops from gtfs data
        {
            int unpaired_from_gtfs = 0;
            int current_part = 0;
            OSMBusImportGenerator buffer = new OSMBusImportGenerator(bb);

            for (GTFSStop gtfsStop:gtfsStops){
                if (gtfsStop.stopMatchedWith == null && gtfsStop.railwayMatchedWith == null && gtfsStop.stopPositionsMatchedWith.size() == 0){
                    unpaired_from_gtfs++;
                    buffer.appendNode(gtfsStop.getNewXMLNode(buffer));
                    if (smallFileExport && unpaired_from_gtfs % 10 == 0){
                        buffer.end();
                        buffer.saveTo(new FileOutputStream(GTFSImportSettings.getInstance().getOutputPath() + GTFSImportSettings.OUTPUT_UNPAIRED_FROM_GTFS + "."+ (current_part++) + ".osm"));
                        buffer = new OSMBusImportGenerator(bb);
                    }
                }
            }
            buffer.end();
            if (unpaired_from_gtfs > 0){
                buffer.saveTo(new FileOutputStream(GTFSImportSettings.getInstance().getOutputPath() + GTFSImportSettings.OUTPUT_UNPAIRED_FROM_GTFS + "."+ (current_part++) + ".osm"));
                System.out.println(ansi().fg(Ansi.Color.GREEN).a("Unpaired stops from GTFS (new stops from GTFS): ").reset().a(unpaired_from_gtfs).fg(Ansi.Color.YELLOW).a(" (created josm osm change file to import data: " + GTFSImportSettings.OUTPUT_UNPAIRED_FROM_GTFS + "[.part].osm)").reset());
            }else{
                System.out.println(ansi().fg(Ansi.Color.GREEN).a("Unpaired stops from GTFS (new stops from GTFS): ").reset().a(unpaired_from_gtfs));
            }
        }
        return null;
    }
}
