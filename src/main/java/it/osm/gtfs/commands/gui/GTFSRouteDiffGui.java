package it.osm.gtfs.commands.gui;

import it.osm.gtfs.input.GTFSParser;
import it.osm.gtfs.input.OSMParser;
import it.osm.gtfs.models.*;
import it.osm.gtfs.utils.GTFSImportSettings;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.xml.parsers.ParserConfigurationException;

import it.osm.gtfs.utils.StopsUtils;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.ELProperty;
import org.jdesktop.beansbinding.util.logging.Logger;
import org.jdesktop.swingbinding.JListBinding;
import org.jdesktop.swingbinding.SwingBindings;
import org.xml.sax.SAXException;

public class GTFSRouteDiffGui extends JFrame implements ListSelectionListener, KeyListener {
    private static final long serialVersionUID = 1L;

    private final JList<String> gtfsTripsList;
    private final JList<String> gtfsStopsList;
    private final JList<String> osmStopsList;
    private final JList<String> osmTripsList;

    List<Trip> uniqueTrips;
    Set<Trip> uniqueTripsMarkerOk = new HashSet<Trip>();
    Set<Trip> uniqueTripsMarkerIgnore = new HashSet<Trip>();
    List<WeightedRelation> osmRels;
    List<Stop> currentGTFSStops = new ArrayList<Stop>();
    Set<Stop> currentGTFSStopsMarker = new HashSet<Stop>();
    List<Stop> currentOSMStops = new ArrayList<Stop>();
    Set<Stop> currentOSMStopsMarker = new HashSet<Stop>();

    public GTFSRouteDiffGui() throws ParserConfigurationException, SAXException, IOException{
        super();
        setLayout(new GridLayout(1, 2));
        setSize(800, 500);
        Logger.getLogger("org.jdesktop.beansbinding.ELProperty").setLevel(Level.SEVERE);
        readData();
        populateMatchedGTFSTrips();
        {
            gtfsTripsList = createEmptyJList();
            gtfsTripsList.setCellRenderer(new GTFSListCellRenderer());
            gtfsTripsList.addListSelectionListener(this);
            gtfsTripsList.addKeyListener(this);
            updateGTFSBind();
            add(new JScrollPane(gtfsTripsList));
        }
        {
            gtfsStopsList = createEmptyJList();
            gtfsStopsList.setCellRenderer(new GTFSStopsCellRenderer());
            updateStopBinding(currentGTFSStops, gtfsStopsList);
            add(new JScrollPane(gtfsStopsList));
        }
        {
            osmStopsList = createEmptyJList();
            osmStopsList.setCellRenderer(new OSMStopsCellRenderer());
            updateStopBinding(currentOSMStops, osmStopsList);
            add(new JScrollPane(osmStopsList));
        }
        {
            osmTripsList = createEmptyJList();
            osmTripsList.addListSelectionListener(this);
            updateOSMBind();
            add(new JScrollPane(osmTripsList));
        }

    }


    private void readData() throws ParserConfigurationException, SAXException, IOException{
        List<OSMStop> osmStops;
        Map<String, OSMStop> osmstopsGTFSId; //change weird map variable name
        Map<String, OSMStop> osmstopsOsmID; //change weird map variable name
        Map<String, Route> routes;
        Map<String, TripStopsList> stopTimes;
        List<Trip> trips;

        osmStops = OSMParser.readOSMStops(GTFSImportSettings.OSM_STOPS_FILE_PATH, true);
        osmstopsGTFSId = StopsUtils.getGTFSIdOSMStopMap(osmStops);
        osmstopsOsmID = StopsUtils.getOSMIdOSMStopMap(osmStops);
        osmRels = convertoToWigthed(OSMParser.readOSMRelations(new File(GTFSImportSettings.OSM_RELATIONS_FILE_PATH), osmstopsOsmID));

        routes = GTFSParser.readRoutes(GTFSImportSettings.getInstance().getGTFSDataPath() +  GTFSImportSettings.GTFS_ROUTES_FILE_NAME);
        stopTimes = GTFSParser.readStopTimes(GTFSImportSettings.getInstance().getGTFSDataPath() +  GTFSImportSettings.GTFS_STOP_TIMES_FILE_NAME, osmstopsGTFSId);
        trips = GTFSParser.readTrips(GTFSImportSettings.getInstance().getGTFSDataPath() +  GTFSImportSettings.GTFS_TRIPS_FILE_NAME,
                routes, stopTimes);
        Set<Trip> uniqueTripSet = new TreeSet<Trip>(trips);
        uniqueTrips = new ArrayList<Trip>();
        for (Trip trip:uniqueTripSet){
            if (GTFSImportSettings.getInstance().getPlugin().isValidRoute(routes.get(trip.getRoute().getId())) &&
                    GTFSImportSettings.getInstance().getPlugin().isValidTrip(trips, uniqueTripSet, trip, stopTimes.get(trip.getTripId()))){
                uniqueTrips.add(trip);
            }
        }
    }

    private void populateMatchedGTFSTrips(){
        Set<String> tripIdMarkers = new HashSet<String>();
        try{
            BufferedReader br = new BufferedReader(new FileReader("tripmarker.txt"));
            String line = br.readLine();
            while(line != null){
                tripIdMarkers.add(line);
                line = br.readLine();
            }
            br.close();
        }catch (Exception e) {
            e.printStackTrace();
        }

        for (Trip t:uniqueTrips){
            if (tripIdMarkers.contains(t.getTripId())){
                uniqueTripsMarkerIgnore.add(t);
            }
            for (WeightedRelation r:osmRels){
                if(r.getStopsAffinity(t.getStopsList()) == Integer.MAX_VALUE){
                    uniqueTripsMarkerOk.add(t);
                    break;
                }
            }
        }
    }

    private JList<String> createEmptyJList(){
        JList<String> j = new JList<String>();
        j.setLayoutOrientation(JList.VERTICAL);
        j.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        j.setSize(200, 500);
        return j;
    }

    @Override
    public void valueChanged(ListSelectionEvent event) {
        if (event.getSource().equals(gtfsTripsList)){
            Trip selectedTrip = uniqueTrips.get(gtfsTripsList.getSelectedIndex());
            currentGTFSStops.clear();
            currentGTFSStops.addAll(selectedTrip.getStopsList().getStopSequenceOSMStopMap().values());
            updateStopBinding(currentGTFSStops, gtfsStopsList);
            updateAffinity(selectedTrip);
        }else if (event.getSource().equals(osmTripsList)){
            currentOSMStops.clear();
            if (osmTripsList.getSelectedIndex() >= 0){
                WeightedRelation selectedRel = osmRels.get(osmTripsList.getSelectedIndex());
                currentOSMStops.addAll(selectedRel.getStops().values());
            }
            updateStopBinding(currentOSMStops, osmStopsList);
        }
    }

    @Override
    public void keyPressed(KeyEvent event) {
        if (event.getSource().equals(gtfsTripsList)){
            if (event.getKeyCode() == KeyEvent.VK_V){
                uniqueTripsMarkerIgnore.add(uniqueTrips.get(gtfsTripsList.getSelectedIndex()));
                gtfsTripsList.repaint();
            }else if (event.getKeyCode() == KeyEvent.VK_X){
                uniqueTripsMarkerIgnore.remove(uniqueTrips.get(gtfsTripsList.getSelectedIndex()));
                gtfsTripsList.repaint();
            }else if (event.getKeyCode() == KeyEvent.VK_S){
                BufferedWriter bw;
                try {
                    bw = new BufferedWriter(new FileWriter("tripmarker.txt"));
                    for (Trip t:uniqueTripsMarkerIgnore){
                        bw.write(t.getTripId()+"\n");
                    }
                    bw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    @Override
    public void keyReleased(KeyEvent arg0) {
    }


    @Override
    public void keyTyped(KeyEvent arg0) {
    }

    private void updateAffinity(Trip selectedTrip) {
        for (WeightedRelation r:osmRels){
            r.setWeight(r.getStopsAffinity(selectedTrip.getStopsList()));
        }
        Collections.sort(osmRels);
        updateOSMBind();
        osmTripsList.setSelectedIndex(0);
    }

    private void updateGTFSBind() {
        JListBinding<Trip, List<Trip>, JList> gtfsListBind = SwingBindings.createJListBinding(UpdateStrategy.READ, uniqueTrips, gtfsTripsList);
        ELProperty<Trip, String> fullNameP = ELProperty.create("${route.shortName} ${name}");
        gtfsListBind.setDetailBinding(fullNameP);
        gtfsListBind.bind();
    }


    private void updateStopBinding(List<Stop> currentStops, JList stopList) {
        updateStopMarkers();
        JListBinding<Stop, List<Stop>, JList> osmStopListBind = SwingBindings.createJListBinding(UpdateStrategy.READ, currentStops, stopList);
        ELProperty<Stop, String> fullNameP = ELProperty.create("${code} ${name}");
        osmStopListBind.setDetailBinding(fullNameP);
        osmStopListBind.bind();
    }

    private void updateStopMarkers() {
        currentGTFSStopsMarker = new HashSet<>();
        currentOSMStopsMarker = new HashSet<>();
        if (currentGTFSStops.size() == 0 || currentOSMStops.size() == 0){
            return;
        }else{
            for (Stop s:currentGTFSStops)
                if (currentOSMStops.contains(s) && currentGTFSStops.indexOf(s) >= currentOSMStops.indexOf(s)){
                    //equals
                }else{
                    currentGTFSStopsMarker.add(s);
                }
            for (Stop s:currentOSMStops)
                if (currentGTFSStops.contains(s) && currentGTFSStops.indexOf(s) <= currentOSMStops.indexOf(s)){
                    //equals
                }else{
                    currentOSMStopsMarker.add(s);
                }
        }
        osmStopsList.repaint();
        gtfsStopsList.repaint();
    }


    private void updateOSMBind() {
        JListBinding<WeightedRelation, List<WeightedRelation>, JList> osmListBind = SwingBindings.createJListBinding(UpdateStrategy.READ, osmRels, osmTripsList);
        ELProperty<WeightedRelation, String> fullNameP = ELProperty.create("${weightstr} ${ref} ${id} ${from} ${to}");
        osmListBind.setDetailBinding(fullNameP);
        osmListBind.bind();
    }

    private static List<WeightedRelation> convertoToWigthed(List<Relation> rels){
        List<WeightedRelation> out = new ArrayList<GTFSRouteDiffGui.WeightedRelation>();
        for (Relation r:rels){
            out.add(new WeightedRelation(r));
        }
        return out;
    }

    public static class WeightedRelation extends Relation implements Comparable<WeightedRelation>{
        private Integer weight = 0;
        private String weightstr = "0";

        public WeightedRelation(Relation r) {
            super(r.getId());
            setFrom(r.getFrom());
            setName(r.getName());
            setRef(r.getRef());
            setTo(r.getTo());
            setType(r.getType());
            setVersion(r.getVersion());
            setWayMembers(r.getWayMembers());
            setStops(r.getStops());
        }

        @Override
        public int compareTo(WeightedRelation other) {
            if (other.weight.equals(weight)){
                return getId().compareTo(other.getId());
            }else if(other.weight > weight){
                return 1;
            }else{
                return -1;
            }
        }

        public Integer getWeight() {
            return weight;
        }
        public String getWeightstr() {
            return weightstr;
        }
        public void setWeightstr(String weightstr) {
            this.weightstr = weightstr;
        }

        public void setWeight(Integer weight) {
            this.weight = weight;
            this.setWeightstr(weight.toString());
        }

        public String toString(){
            return getWeightstr() + getWeight();
        }
    }

    public class GTFSListCellRenderer extends DefaultListCellRenderer{
        private static final long serialVersionUID = 1L;

        @Override
        public Component getListCellRendererComponent(JList list, Object arg1,
                                                      int index, boolean arg3, boolean arg4) {
            Component c = super.getListCellRendererComponent(list, arg1, index, arg3, arg4);
            if (uniqueTripsMarkerOk.contains(uniqueTrips.get(index)))
                c.setBackground(new Color(120, 255, 120));
            else if (uniqueTripsMarkerIgnore.contains(uniqueTrips.get(index)))
                c.setBackground(new Color(255, 255, 120));
            return c;
        }
    }
    public class GTFSStopsCellRenderer extends DefaultListCellRenderer{
        private static final long serialVersionUID = 1L;

        @Override
        public Component getListCellRendererComponent(JList list, Object arg1,
                                                      int index, boolean arg3, boolean arg4) {
            Component c = super.getListCellRendererComponent(list, arg1, index, arg3, arg4);
            if (currentGTFSStopsMarker.contains(currentGTFSStops.get(index)))
                c.setBackground(new Color(255, 120, 120));
            return c;
        }
    }
    public class OSMStopsCellRenderer extends DefaultListCellRenderer{
        private static final long serialVersionUID = 1L;

        @Override
        public Component getListCellRendererComponent(JList list, Object arg1,
                                                      int index, boolean arg3, boolean arg4) {
            Component c = super.getListCellRendererComponent(list, arg1, index, arg3, arg4);
            if (currentOSMStopsMarker.contains(currentOSMStops.get(index)))
                c.setBackground(new Color(255, 120, 120));
            return c;
        }
    }

}