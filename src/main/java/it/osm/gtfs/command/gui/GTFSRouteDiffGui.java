package it.osm.gtfs.command.gui;

import it.osm.gtfs.input.GTFSParser;
import it.osm.gtfs.input.OSMParser;
import it.osm.gtfs.model.Relation;
import it.osm.gtfs.model.Route;
import it.osm.gtfs.model.Stop;
import it.osm.gtfs.model.StopsList;
import it.osm.gtfs.model.Trip;
import it.osm.gtfs.utils.GTFSImportSetting;

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

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.ELProperty;
import org.jdesktop.beansbinding.util.logging.Logger;
import org.jdesktop.swingbinding.JListBinding;
import org.jdesktop.swingbinding.SwingBindings;
import org.xml.sax.SAXException;

public class GTFSRouteDiffGui extends JFrame implements ListSelectionListener, KeyListener {
	private static final long serialVersionUID = 1L;

	private JList gtfsList;
	private JList gtfsStopList;
	private JList osmStopList;
	private JList osmList;

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
			gtfsList = createEmptyJList();
			gtfsList.setCellRenderer(new GTFSListCellRendered());
			gtfsList.addListSelectionListener(this);
			gtfsList.addKeyListener(this);
			updateGTFSBind();
			add(new JScrollPane(gtfsList));
		}
		{
			gtfsStopList = createEmptyJList();
			gtfsStopList.setCellRenderer(new GTFSStopsCellRendered());
			updateGTFSStopBind();
			add(new JScrollPane(gtfsStopList));
		}
		{
			osmStopList = createEmptyJList();
			osmStopList.setCellRenderer(new OSMStopsCellRendered());
			updateOSMStopBind();
			add(new JScrollPane(osmStopList));
		}
		{
			osmList = createEmptyJList();
			osmList.addListSelectionListener(this);
			updateOSMBind();
			add(new JScrollPane(osmList));
		}

	}


	private void readData() throws ParserConfigurationException, SAXException, IOException{
		List<Stop> osmStops;
		Map<String, Stop> osmstopsGTFSId;
		Map<String, Stop> osmstopsOsmID;
		Map<String, Route> routes;
		Map<String, StopsList> stopTimes;
		List<Trip> trips;

		osmStops = OSMParser.readOSMStops(GTFSImportSetting.getInstance().getOSMPath() +  GTFSImportSetting.OSM_STOP_FILE_NAME);
		osmstopsGTFSId = OSMParser.applyGTFSIndex(osmStops);
		osmstopsOsmID = OSMParser.applyOSMIndex(osmStops);
		osmRels = convertoToWigthed(OSMParser.readOSMRelations(new File(GTFSImportSetting.getInstance().getOSMPath() +  GTFSImportSetting.OSM_RELATIONS_FILE_NAME), osmstopsOsmID));

		routes = GTFSParser.readRoutes(GTFSImportSetting.getInstance().getGTFSPath() +  GTFSImportSetting.GTFS_ROUTES_FILE_NAME);
		stopTimes = GTFSParser.readStopTimes(GTFSImportSetting.getInstance().getGTFSPath() +  GTFSImportSetting.GTFS_STOP_TIME_FILE_NAME, osmstopsGTFSId);
		trips = GTFSParser.readTrips(GTFSImportSetting.getInstance().getGTFSPath() +  GTFSImportSetting.GTFS_TRIPS_FILE_NAME,
				routes, stopTimes);
		Set<Trip> uniqueTripSet = new TreeSet<Trip>(trips);
		uniqueTrips = new ArrayList<Trip>();
		for (Trip trip:uniqueTripSet){
			if (GTFSImportSetting.getInstance().getPlugin().isValidRoute(routes.get(trip.getRoute().getId())) &&
					GTFSImportSetting.getInstance().getPlugin().isValidTrip(trips, uniqueTripSet, trip, stopTimes.get(trip.getTripID()))){
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
		}catch (Exception e) {}
		
		for (Trip t:uniqueTrips){
			if (tripIdMarkers.contains(t.getTripID())){
				uniqueTripsMarkerIgnore.add(t);
			}
			for (WeightedRelation r:osmRels){
				if(r.getStopsAffinity(t.getStopTime()) == Integer.MAX_VALUE){
					uniqueTripsMarkerOk.add(t);
					break;
				}
			}
		}
	}

	private JList createEmptyJList(){
		JList j = new JList();
		j.setLayoutOrientation(JList.VERTICAL);
		j.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		j.setSize(200, 500);
		return j;
	}

	@Override
	public void valueChanged(ListSelectionEvent event) {
		if (event.getSource().equals(gtfsList)){
			Trip selectedTrip = uniqueTrips.get(gtfsList.getSelectedIndex());
			currentGTFSStops.clear();
			currentGTFSStops.addAll(selectedTrip.getStopTime().getStops().values());
			updateGTFSStopBind();
			updateAffinity(selectedTrip);
		}else if (event.getSource().equals(osmList)){
			currentOSMStops.clear();
			if (osmList.getSelectedIndex() >= 0){
				WeightedRelation selectedRel = osmRels.get(osmList.getSelectedIndex());
				currentOSMStops.addAll(selectedRel.getStops().values());
			}
			updateOSMStopBind();
		}
	}

	@Override
	public void keyPressed(KeyEvent event) {
		if (event.getSource().equals(gtfsList)){
			if (event.getKeyCode() == KeyEvent.VK_V){
				uniqueTripsMarkerIgnore.add(uniqueTrips.get(gtfsList.getSelectedIndex()));
				gtfsList.repaint();
			}else if (event.getKeyCode() == KeyEvent.VK_X){
				uniqueTripsMarkerIgnore.remove(uniqueTrips.get(gtfsList.getSelectedIndex()));
				gtfsList.repaint();
			}else if (event.getKeyCode() == KeyEvent.VK_S){
				BufferedWriter bw;
				try {
					bw = new BufferedWriter(new FileWriter("tripmarker.txt"));
					for (Trip t:uniqueTripsMarkerIgnore){
						bw.write(t.getTripID()+"\n");
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
			r.setWeight(r.getStopsAffinity(selectedTrip.getStopTime()));
		}
		Collections.sort(osmRels);
		updateOSMBind();
		osmList.setSelectedIndex(0);
	}

	private void updateGTFSBind() {
		JListBinding<Trip, List<Trip>, JList> gtfsListBind = SwingBindings.createJListBinding(UpdateStrategy.READ, uniqueTrips, gtfsList);
		ELProperty<Trip, String> fullNameP = ELProperty.create("${route.shortName} ${name}");
		gtfsListBind.setDetailBinding(fullNameP);
		gtfsListBind.bind();
	}

	private void updateGTFSStopBind(){
		updateStopMarkers();
		JListBinding<Stop, List<Stop>, JList> gtfsStopListBind = SwingBindings.createJListBinding(UpdateStrategy.READ, currentGTFSStops, gtfsStopList);
		ELProperty<Stop, String> fullNameP = ELProperty.create("${code} ${name}");
		gtfsStopListBind.setDetailBinding(fullNameP);
		gtfsStopListBind.bind();
	}

	private void updateOSMStopBind() {
		updateStopMarkers();
		JListBinding<Stop, List<Stop>, JList> osmStopListBind = SwingBindings.createJListBinding(UpdateStrategy.READ, currentOSMStops, osmStopList);
		ELProperty<Stop, String> fullNameP = ELProperty.create("${code} ${name}");
		osmStopListBind.setDetailBinding(fullNameP);
		osmStopListBind.bind();
	}

	private void updateStopMarkers() {
		currentGTFSStopsMarker = new HashSet<Stop>();
		currentOSMStopsMarker = new HashSet<Stop>();
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
		osmStopList.repaint();
		gtfsStopList.repaint();
	}


	private void updateOSMBind() {
		JListBinding<WeightedRelation, List<WeightedRelation>, JList> osmListBind = SwingBindings.createJListBinding(UpdateStrategy.READ, osmRels, osmList);
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
			setStopsTime(r.getStopsTime());
		}

		@Override
		public int compareTo(WeightedRelation other) {
			if (other.weight == weight){
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
	
	public class GTFSListCellRendered extends DefaultListCellRenderer{
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
	public class GTFSStopsCellRendered extends DefaultListCellRenderer{
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
	public class OSMStopsCellRendered extends DefaultListCellRenderer{
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