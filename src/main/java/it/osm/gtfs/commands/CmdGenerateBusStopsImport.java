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

package it.osm.gtfs.commands;

import it.osm.gtfs.commands.gui.GTFSStopsReviewGui;
import it.osm.gtfs.enums.OSMStopType;
import it.osm.gtfs.input.GTFSParser;
import it.osm.gtfs.input.OSMParser;
import it.osm.gtfs.models.BoundingBox;
import it.osm.gtfs.models.GTFSStop;
import it.osm.gtfs.models.OSMStop;
import it.osm.gtfs.output.OSMBusImportGenerator;
import it.osm.gtfs.utils.*;
import org.fusesource.jansi.Ansi;
import org.jxmapviewer.viewer.GeoPosition;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import picocli.CommandLine;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static org.fusesource.jansi.Ansi.ansi;


@CommandLine.Command(name = "stops", mixinStandardHelpOptions = true, description = "Generate files to import bus stops into osm merging with existing stops")
public class CmdGenerateBusStopsImport implements Callable<Void> {

    final ArrayList<OSMStop> osmStopsToReview = new ArrayList<>(); //TODO: maybe find a better place for these two variables?
    final Map<OSMStop, GeoPosition> finalReviewedGeopositions = new HashMap<>();
    @CommandLine.Option(names = {"-s", "--skipupdate"}, description = "Skip OSM data update (not recommended)")
    Boolean noUpdate = false;
    @CommandLine.Option(names = {"-n", "--noreview"}, description = "Disable GUI review. For every node that is too distant from the GTFS coords a new stop will be generated.")
    Boolean noGuiReview = false;
    @CommandLine.Mixin
    private SharedCliOptions sharedCliOptions;

    @Override
    public Void call() throws IOException, ParserConfigurationException, SAXException, TransformerException, InterruptedException {

        //update/download OSM data
        if (!noUpdate) {
            new CmdUpdateGTFSOSMData().call();
        }


        List<GTFSStop> gtfsStopsList = GTFSParser.readStops(GTFSImportSettings.getInstance().getGTFSDataPath() + GTFSImportSettings.GTFS_STOP_FILE_NAME);
        BoundingBox bb = new BoundingBox(gtfsStopsList);

        List<OSMStop> osmStopsList = OSMParser.readOSMStops(GTFSImportSettings.getInstance().getOsmStopsFilePath(), SharedCliOptions.checkStopsOfAnyOperatorTagValue);

        //TODO: TO REMOVE - THIS IS ONLY FOR A QUICK DEBUG!!!!
        // osmStopsList = osmStopsList.subList(0, 500);


        //first matching phase between GTFS and OSM stops - check the StopUtils match() function to understand the criteria used to consider whether the GTFS and OSM stops are the same or not
        for (GTFSStop gtfsStop : gtfsStopsList) {

            for (OSMStop osmStop : osmStopsList) {
                if (StopsUtils.match(gtfsStop, osmStop)) {
                    if (osmStop.getStopType().equals(OSMStopType.TRAM_STOP_POSITION)) { //todo: maybe add also a check for OSMStopType.PHYSICAL_TRAM_STOP ?

                        //we check for multiple matches for tram stops && bus stops, and we handle them based on how distant the current loop stop and the already matched stop are
                        if (gtfsStop.railwayStopMatchedWith != null) {
                            System.out.println(ansi().render("@|red Multiple match found between current GTFS stop and two other OSM stops: |@"));
                            System.out.println(ansi().render("@|red Current GTFS stop: |@" + gtfsStop));
                            System.out.println(ansi().render("@|red Current-matching OSM stop: |@" + osmStop));
                            System.out.println(ansi().render("@|red Already-matched OSM stop: |@" + gtfsStop.osmStopMatchedWith));

                            double distanceBetweenCurrentStop = OSMDistanceUtils.distVincenty(gtfsStop.getGeoPosition(), osmStop.getGeoPosition());
                            double distanceBetweenAlreadyMatchedStop = OSMDistanceUtils.distVincenty(gtfsStop.getGeoPosition(), gtfsStop.railwayStopMatchedWith.getGeoPosition());

                            if (distanceBetweenCurrentStop > distanceBetweenAlreadyMatchedStop) {

                                System.out.println(ansi().render("@|cyan Keeping the closest one, which is " + gtfsStop.osmStopMatchedWith + "|@"));

                                continue;
                            }

                            System.out.println(ansi().render("@|cyan Keeping the closest one, which is " + osmStop + "|@"));

                            gtfsStop.railwayStopMatchedWith.gtfsStopMatchedWith = null;

                        }

                        gtfsStop.railwayStopMatchedWith = osmStop;

                    } else {
                        if (osmStop.gtfsStopMatchedWith != null || gtfsStop.osmStopMatchedWith != null) {
                            System.out.println(ansi().render("@|red Multiple match found between current GTFS stop and two other OSM stops: |@"));
                            System.out.println(ansi().render("@|red Current GTFS stop: |@" + gtfsStop));
                            System.out.println(ansi().render("@|red Current-matching OSM stop: |@" + osmStop));
                            System.out.println(ansi().render("@|red Already-matched OSM stop: |@" + gtfsStop.osmStopMatchedWith));

                            double distanceBetweenCurrentStop = OSMDistanceUtils.distVincenty(gtfsStop.getGeoPosition(), osmStop.getGeoPosition());
                            double distanceBetweenAlreadyMatchedStop = OSMDistanceUtils.distVincenty(gtfsStop.getGeoPosition(), gtfsStop.osmStopMatchedWith.getGeoPosition());

                            //in case of multiple matching we check what stop is the closest one to the gtfs coordinates between the current loop stop and the already-matched stop
                            if (distanceBetweenCurrentStop > distanceBetweenAlreadyMatchedStop) {

                                System.out.println(ansi().render("@|cyan Keeping the closest one, which is " + gtfsStop.osmStopMatchedWith + "|@"));
                                //in case the already-matched stop is the closest one to the gtfs coordinates then we skip setting the stop match variables, and we go ahead with the loop
                                continue;
                            }

                            System.out.println(ansi().render("@|cyan Keeping the closest one, which is " + osmStop + "|@"));

                            //in case the current loop stop is the closest one to the gtfs coordinates, we remove the matched gtfs stop from the  already matched osm stop
                            gtfsStop.osmStopMatchedWith.gtfsStopMatchedWith = null;
                            //gtfsStop.osmStopMatchedWith = null; nope because we just replace 5 lines later
                        }

                        gtfsStop.stopsMatchedWith.add(osmStop);
                        osmStop.stopsMatchedWith.add(gtfsStop);

                        gtfsStop.osmStopMatchedWith = osmStop;

                    }

                    osmStop.gtfsStopMatchedWith = gtfsStop;

                }
            }
        }

        //second matching phase by checking all osm stops again (also checking stops that didn't get matched && those that we don't consider matched)
        {

            int matched_stops = 0;
            int not_matched_osm_stops = 0;
            int stopsToReview = 0;

            OSMBusImportGenerator bufferNotMatchedStops = new OSMBusImportGenerator(bb);
            OSMBusImportGenerator bufferMatchedStops = new OSMBusImportGenerator(bb);

            for (OSMStop osmStop : osmStopsList) {
                Element originalNode = (Element) osmStop.originalXMLNode;

                //we check if the osm stop got matched with a gtfs stop AND only IF the osm stop needs the position review but the user doesn't want to review the stops then we consider the stop as not matched and we handle it in the else case
                if (osmStop.gtfsStopMatchedWith != null && !(osmStop.needsPositionReview() && noGuiReview)) {


                    //if the stop was marked as disused but now was matched again, we remove the disused marking
                    if(osmStop.isDisused()) {
                        String stringOutput = "OSM Stop node id " + osmStop.getOSMId() + " (ref=" + osmStop.getCode() + ", gtfs_id=" + osmStop.getGtfsId() + ")" + " was marked as disused but now it has been matched.";

                        System.out.println(stringOutput);
                        OSMXMLUtils.unmarkDisused(originalNode);
                    }

                    //we update the XML data according to the relative GTFS stop data
                    StopsUtils.updateOSMNodeMetadata(osmStop);

                    //for stops that need a position review
                    if (osmStop.needsPositionReview()) {

                        osmStopsToReview.add(osmStop);
                        stopsToReview++;
                    } else { //we will add the stops that need review to the matched file after the review later

                        //add the node to the buffer of matched stops
                        bufferMatchedStops.appendNode(originalNode);
                    }

                    matched_stops++;

                } else {
                    String notMatchedStringOutput = "OSM Stop node id " + osmStop.getOSMId() + " (ref=" + osmStop.getCode() + ", gtfs_id=" + osmStop.getGtfsId() + ")" + " didn't get matched to a GTFS stop as either they are too distant or the ref code is no more available in gtfs.";


                    if (osmStop.getOperator() != null) {//if there is a stop that has no operator, and it has NOT been matched, then it couuld be managed by another bus company/operator. so we don't consider it anymore
                        System.out.println(notMatchedStringOutput);

                        //for the not matched stops we add the action=delete keyvalue so that JOSM knows that these stops need to be deleted on upload
                        //OSMXMLUtils.addOSMDeleteActionAttribute(originalNode);

                        //instead of adding a JOSM "delete" action, we mark this stop as disused, so that it is preserved in OSM in case this is a false positive
                        OSMXMLUtils.markDisused(originalNode); //to stops that are already marked as disused we re-mark them as disused in case some tags are not marked as disused

                        bufferNotMatchedStops.appendNode(originalNode);

                        not_matched_osm_stops++;
                    }
                }
            }


            //stops position review with GUI
            //only execute if the noreview flag is set to false
            if (!noGuiReview && !osmStopsToReview.isEmpty()) {
                System.out.println(ansi().render("@|cyan Starting stop positions review...|@"));

                final Object lockObject = new Object();

                new GTFSStopsReviewGui(osmStopsToReview, finalReviewedGeopositions, lockObject);

                synchronized (lockObject) { //i don't really know if this lock-sync thing is really needed to make the tool stay up when the gui is started
                    try {
                        lockObject.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                if (osmStopsToReview.size() == finalReviewedGeopositions.size()) {
                    System.out.println(ansi().render("@|cyan Stop review done |@"));
                    System.out.println(ansi().render("@|yellow Saving accepted coordinates... |@"));

                    for (OSMStop reviewedOsmStop : osmStopsToReview) {
                        Element originalNode = (Element) reviewedOsmStop.originalXMLNode;

                        GeoPosition chosenGeoPosition = finalReviewedGeopositions.get(reviewedOsmStop);

                        //we set the new chosen coordinates to the node
                        originalNode.setAttribute("lat", String.valueOf(chosenGeoPosition.getLatitude()));
                        originalNode.setAttribute("lon", String.valueOf(chosenGeoPosition.getLongitude()));

                        //we add the node with new coords to the matched stops buffer
                        bufferMatchedStops.appendNode(originalNode);
                    }
                } else {
                    System.out.println(ansi().render("@|red Stop locations review not completed. \n If you don't want to review the stops manually you can use the --noreview command option. |@"));
                    System.out.println(ansi().render("@|red No stop data will be saved. You may run the tool again to restart the review. |@"));

                    return null; //we end this command
                }
            }


            if (matched_stops > 0) {
                bufferMatchedStops.end();
                bufferMatchedStops.saveTo(new FileOutputStream(GTFSImportSettings.getInstance().getOutputPath() + GTFSImportSettings.OUTPUT_MATCHED_WITH_UPDATED_METADATA));


                System.out.println(ansi().fg(Ansi.Color.GREEN).a("Total Matched OSM stops with GTFS data: ").reset().a(matched_stops).fg(Ansi.Color.YELLOW).a(" (created osm change file to review: " + GTFSImportSettings.OUTPUT_MATCHED_WITH_UPDATED_METADATA + ")").reset());

                if (noGuiReview) {
                    System.out.println(ansi().fg(Ansi.Color.CYAN).a("You chose to NOT review the stops that need manual position review. Therefore these stops have been considered to be removed and a new stop node has been created for each of those removed stops with the updated coordinates.").reset());
                } else {
                    System.out.println("(" + ansi().fg(Ansi.Color.CYAN).a("Matched stops that need manual position review: ").reset().a(stopsToReview) + ")");
                }
            } else {
                System.out.println(ansi().fg(Ansi.Color.YELLOW).a("No OSM stop got matched with GTFS data!").reset());
            }

            if (not_matched_osm_stops > 0) {
                bufferNotMatchedStops.end();
                bufferNotMatchedStops.saveTo(new FileOutputStream(GTFSImportSettings.getInstance().getOutputPath() + GTFSImportSettings.OUTPUT_NOT_MATCHED_STOPS));
                System.out.println(ansi().fg(Ansi.Color.GREEN).a("NOT MATCHED OSM stops that should be *removed* from OSM: ").reset().a(not_matched_osm_stops).fg(Ansi.Color.YELLOW).a(" (created osm change file to review: " + GTFSImportSettings.OUTPUT_NOT_MATCHED_STOPS + ")").reset());
            }

        }


        //new stops from gtfs data
        {
            int new_stops_from_gtfs = 0;
            OSMBusImportGenerator buffer = new OSMBusImportGenerator(bb);

            for (GTFSStop gtfsStop : gtfsStopsList) {
                if (gtfsStop.osmStopMatchedWith == null && gtfsStop.stopsMatchedWith.size() == 0 || (gtfsStop.osmStopMatchedWith != null && gtfsStop.osmStopMatchedWith.needsPositionReview() && noGuiReview)) {
                    new_stops_from_gtfs++;

                    //we create the new node with new tags here
                    buffer.appendNode(gtfsStop.getNewXMLNode(buffer));
                }
            }

            buffer.end();

            if (new_stops_from_gtfs > 0) {
                buffer.saveTo(new FileOutputStream(GTFSImportSettings.getInstance().getOutputPath() + GTFSImportSettings.OUTPUT_NEW_STOPS_FROM_GTFS));
                System.out.println(ansi().fg(Ansi.Color.GREEN).a("New stops from GTFS (unmatched stops from GTFS): ").reset().a(new_stops_from_gtfs).fg(Ansi.Color.YELLOW).a(" (created osm change file to import data: " + GTFSImportSettings.OUTPUT_NEW_STOPS_FROM_GTFS + ")").reset());
            } else {
                System.out.println(ansi().fg(Ansi.Color.GREEN).a("New stops from GTFS (unmatched stops from GTFS): ").reset().a(new_stops_from_gtfs));
            }
        }


        return null;
    }
}
