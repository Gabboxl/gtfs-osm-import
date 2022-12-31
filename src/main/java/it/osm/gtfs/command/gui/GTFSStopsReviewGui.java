package it.osm.gtfs.command.gui;

import it.osm.gtfs.model.OSMStop;
import it.osm.gtfs.utils.GTFSImportSettings;
import org.jxmapviewer.JXMapKit;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.TileFactoryInfo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Map;


public class GTFSStopsReviewGui
{
    public static void main(String[] args) {
        try {
            // we specify the default look and feel for the swing components
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            System.err.println("Error setting LookAndFeel: " + e.getLocalizedMessage());
       }

        //final GTFSStopsReviewGui app = new GTFSStopsReviewGui();
    }

    ListIterator<OSMStop> iteratorStopsToReview;

    final JXMapKit osmCoordsStopMap;

    final JXMapKit gtfsCoordsStopMap;

    final JLabel lab1;

    final JLabel lab2;

    final JLabel labOsmCoords;

    final JLabel labGtfsCoords;

    final JLabel labReviewCompleted;

    final JButton btChooseOSM;

    final JButton btChooseGTFS;

    final JButton btCloseWindow;

    String infoListReviewText = "<html><b>Stop review list:</b> (you can select any stop from this list to review it again)</html>";
    String infoStopText = "<html><b>Current stop ref code:</b> <i>%s</i> </html>";
    String textLabOsmCoords = "Current OSM stop coordinates on map (%f, %f):";
    String textLabGtfsCoords = "New GTFS stop coordinates on map (%f, %f):";

    final JLabel labInfoStop1;

    final JLabel labInfoReviewList;

    final JList<OSMStop> jListStops;

    final ArrayList<OSMStop> osmStopsToReview;

    final Map<OSMStop, GeoPosition> finalReviewedGeopositions;


    public GTFSStopsReviewGui(ArrayList<OSMStop> osmStopsToReview, Map<OSMStop, GeoPosition> finalReviewedGeopositions, Object lockObject) {
        this.osmStopsToReview = osmStopsToReview;
        this.finalReviewedGeopositions = finalReviewedGeopositions;


        osmCoordsStopMap = new JXMapKit();
        gtfsCoordsStopMap = new JXMapKit();

        TileFactoryInfo info = new OSMTileFactoryInfo();
        DefaultTileFactory tileFactory = new DefaultTileFactory(info);
        osmCoordsStopMap.setTileFactory(tileFactory);
        gtfsCoordsStopMap.setTileFactory(tileFactory);
        gtfsCoordsStopMap.setDataProviderCreditShown(true);


        iteratorStopsToReview = osmStopsToReview.listIterator();

        //set starting zoom level
        osmCoordsStopMap.setZoom(3);
        gtfsCoordsStopMap.setZoom(3);


        GridBagConstraints constraints = new GridBagConstraints();
        GridBagLayout layout = new GridBagLayout();

        final JFrame frame = new JFrame(); //we do not set the default title as we update it with a function later
        frame.setLayout(layout);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); //we handle the close action later
        frame.setMinimumSize(new Dimension(1200, 800));

        //frame close are you sure dialog
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (iteratorStopsToReview.hasNext()) {

                    int confirmed = JOptionPane.showConfirmDialog(null,
                            "Are you sure you want to exit the stop review? \n You will lose the review progress you made so far and no stop change files will be generated.", "Exit Stop Review Message Box",
                            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

                    if (confirmed == JOptionPane.YES_OPTION) {
                        frame.dispose(); //dispose al posto di System.exit() chiude soltanto questo jframe e non tutto il programma java, cosi' puo' continuare
                        //System.exit(0);
                    }
                } else {
                    frame.dispose();
                }
            }
        });

        //fix for minimum windows size for any platform
        frame.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent evt) {
                Dimension size = frame.getSize();
                Dimension min = frame.getMinimumSize();
                if (size.getWidth() < min.getWidth()) {
                    frame.setSize((int) min.getWidth(), (int) size.getHeight());
                }
                if (size.getHeight() < min.getHeight()) {
                    frame.setSize((int) size.getWidth(), (int) min.getHeight());
                }

                frame.setSize(size);
            }
        });

        frame.setSize(new Dimension(1200, 800));

        frame.addWindowListener(new WindowAdapter() { //when the window closes we notify the lockobject we defined earlier that has closed, so we can continue the execution of the tool
            @Override
            public void windowClosed(WindowEvent windowEvent) {
                synchronized (lockObject) {
                    lockObject.notify();
                }
            }
        });

        frame.setVisible(true);




        //we define the components of the frame here

        lab1 = new JLabel("GTFS-OSM stops location review");
        lab1.setFont(new Font(null, Font.BOLD, 20));
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 0;
        constraints.ipady = 0;
        constraints.weighty = 0;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(10,10,0,0);
        constraints.gridx = 0;
        constraints.gridwidth = 2;
        constraints.gridy = 0;
        frame.add(lab1, constraints);


        lab2 = new JLabel("Some OSM stops are too distant from the respective GTFS coordinates, so you need to choose what coordinates, between the OSM and the respective GTFS stop, are the correct ones.");
        lab2.setFont(new Font(null, Font.PLAIN, 13));
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 0;
        constraints.ipady = 0;
        constraints.weighty = 0;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(10,10,40,0);
        constraints.gridx = 0;
        constraints.gridwidth = 2;
        constraints.gridy = 1;
        frame.add(lab2, constraints);


        labInfoReviewList = new JLabel(infoListReviewText);
        labInfoReviewList.setFont(new Font(null, Font.PLAIN, 14));
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 0;
        constraints.anchor = GridBagConstraints.SOUTHWEST;
        constraints.insets = new Insets(0,10,5,0);
        constraints.ipady = 0;
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 2;
        constraints.gridheight = 1;
        constraints.weighty = 0;
        frame.add(labInfoReviewList, constraints);



        DefaultListModel<OSMStop> model = new DefaultListModel<>();
        for(OSMStop s:osmStopsToReview){
            model.addElement(s);
        }

        jListStops = new JList<>(){
            @Override
            public Dimension getPreferredScrollableViewportSize() {
                Dimension size = super.getPreferredScrollableViewportSize();
                size.width = 100; //we set a custom very small width of the jlist, because otherwise it happens this https://stackoverflow.com/questions/30563893/setting-the-width-of-a-jlist-in-a-jscrollpane-with-gridbaglayout
                return size;
            }
        };;
        jListStops.setModel(model);

        jListStops.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jListStops.setLayoutOrientation(JList.VERTICAL);
        jListStops.setCellRenderer(new CustomStopCellRenderer());
        jListStops.addListSelectionListener(listSelectionEvent -> {
            //JOptionPane.showMessageDialog(null, jListStops.getSelectedValue())
        });
        jListStops.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent mouseEvent) { //mouseReleased is better than mouseClicked because sometimes people click and drag over the list or maybe the mouse is not a good mouse and then the click&drag won't be considered a click
                    if (jListStops.getSelectedIndex() >= 0) {
                        //we set the iterator at the current list selection
                        iteratorStopsToReview = osmStopsToReview.listIterator(jListStops.getSelectedIndex() + 1); //the + 1 fixes the wrong starting point of  -1 for the iterator, i don't know why

                        updateCoords(osmStopsToReview.get(jListStops.getSelectedIndex()));

                        //in case a map is not set as visible it means we got to the end of the list and we set everything as not visible, so we set everything as visible again
                        if(!osmCoordsStopMap.isVisible()){
                            showEverythingStopRelated();
                        }

                        //JOptionPane.showMessageDialog(null, iteratorasd.nextIndex());
                    }
                }
        });

        JScrollPane scrollPaneStops = new JScrollPane(jListStops);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 0.5;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(0,15,5,0);
        constraints.ipady = 0;
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weighty = 0;
        frame.add(scrollPaneStops, constraints);

        // Add an empty label to the second column to take up the other half of the available space (thx chatgpt)
        labInfoStop1 = new JLabel(infoStopText);
        labInfoStop1.setFont(new Font(null, Font.PLAIN, 18));
        constraints.weightx = 0.5;
        constraints.gridx = 1;
        constraints.insets = new Insets(0,10,0,0);
        frame.add(labInfoStop1, constraints);


        labOsmCoords = new JLabel(textLabOsmCoords);
        labOsmCoords.setFont(new Font(null, Font.BOLD, 12));
        constraints.fill = GridBagConstraints.BOTH;
        constraints.ipady = 0;
        constraints.weighty = 0;
        constraints.anchor = GridBagConstraints.SOUTHWEST;
        constraints.insets = new Insets(10,20,0,0);
        constraints.gridx = 0;
        constraints.gridwidth = 2;
        constraints.gridy = 4;
        constraints.weightx = 0;
        frame.add(labOsmCoords, constraints);


        labGtfsCoords = new JLabel(textLabGtfsCoords);
        labGtfsCoords.setFont(new Font(null, Font.BOLD, 12));
        constraints.fill = GridBagConstraints.BOTH;
        constraints.ipady = 0;
        constraints.weighty = 0;
        constraints.anchor = GridBagConstraints.SOUTHWEST;
        constraints.insets = new Insets(10,10,0,0);
        constraints.gridx = 1;
        constraints.gridwidth = 2;
        constraints.gridy = 4;
        constraints.weightx = 0;
        frame.add(labGtfsCoords, constraints);


        labReviewCompleted = new JLabel("Stop positions review completed! You can now close this window!");
        labReviewCompleted.setFont(new Font(null, Font.BOLD, 24));
        labReviewCompleted.setForeground(Color.green.darker()); //we set the label text color to green but darker (lol i didn't know this function existed)
        labReviewCompleted.setVisible(false);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.ipady = 0;
        constraints.weighty = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(10,10,0,0);
        constraints.gridx = 0;
        constraints.gridwidth = 2;
        constraints.gridy = 4;
        constraints.weightx = 0;
        frame.add(labReviewCompleted, constraints);


        btCloseWindow = new JButton("Click here to close and save!");
        btCloseWindow.addActionListener(actionEvent -> {
            frame.dispose();
        });
        btCloseWindow.setVisible(false);
        constraints.fill = GridBagConstraints.NONE;
        constraints.ipady = 35;       //reset to default
        constraints.weighty = 0;   //request any extra vertical space
        constraints.anchor = GridBagConstraints.CENTER; //bottom of space
        constraints.insets = new Insets(20,0,0,0);  //top padding
        constraints.gridx = 0;       //aligned with button 2
        constraints.gridwidth = 2;   //2 columns wide
        constraints.gridy = 5;       //third row
        frame.add(btCloseWindow, constraints);


        constraints.fill = GridBagConstraints.BOTH;
        constraints.anchor = GridBagConstraints.SOUTHEAST;
        constraints.ipady = 10; //how much to add to the size of the component in y or x axis
        constraints.gridx = 0;
        constraints.gridy = 5;
        constraints.gridheight = 1;
        constraints.gridwidth = 1;
        constraints.insets.left = 20;
        constraints.insets.right = 10;
        constraints.weightx = 0;
        constraints.weighty = 1;
        frame.add(osmCoordsStopMap, constraints);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.ipady = 10;
        constraints.gridx = 1;
        constraints.gridy = 5;
        constraints.gridheight = 1;
        constraints.gridwidth = 1;
        constraints.insets.left = 10;
        constraints.insets.right = 20;
        constraints.weightx = 0;
        constraints.weighty = 1;
        frame.add(gtfsCoordsStopMap, constraints);


        btChooseOSM = new JButton("Accept current OSM coordinates");
        btChooseOSM.addActionListener(actionEvent -> {

            OSMStop currentStop = osmStopsToReview.get(iteratorStopsToReview.nextIndex() - 1);

            finalReviewedGeopositions.put(currentStop, currentStop.getGeoPosition()); //we put the OSM coords in the map


            nextStopToReview(); //TODO: to substitute with nextstoptoreview()
        });

        constraints.fill = GridBagConstraints.NONE;
        constraints.ipady = 10;       //reset to default
        constraints.weighty = 0;   //request any extra vertical space
        constraints.anchor = GridBagConstraints.CENTER; //bottom of space
        constraints.insets = new Insets(10,0,0,0);  //top padding
        constraints.gridx = 0;       //aligned with button 2
        constraints.gridwidth = 1;   //2 columns wide
        constraints.gridy = 6;       //row
        constraints.weightx = 0;
        frame.add(btChooseOSM, constraints);



        btChooseGTFS = new JButton("Accept new GTFS coordinates");
        btChooseGTFS.addActionListener(actionEvent -> {
            OSMStop currentStop = osmStopsToReview.get(iteratorStopsToReview.nextIndex() - 1);

            finalReviewedGeopositions.put(currentStop, currentStop.gtfsStopMatchedWith.getGeoPosition()); //we put the GTFS coords in the map

            nextStopToReview(); //TODO: to substitute with nextstoptoreview()
        });

        constraints.fill = GridBagConstraints.NONE;
        constraints.ipady = 10;       //reset to default
        constraints.weighty = 0;   //request any extra vertical space
        constraints.anchor = GridBagConstraints.CENTER; //bottom of space
        constraints.insets = new Insets(10,0,0,0);  //top padding
        constraints.gridx = 1;       //aligned with button 2
        constraints.gridwidth = 1;   //2 columns wide
        constraints.gridy = 6;       //row
        constraints.weightx = 0;
        frame.add(btChooseGTFS, constraints);



        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        constraints.weighty = 0.1; //set this to 1 when no other components use the 1 at weighty
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.gridx = 0;
        constraints.gridy = 7;
        constraints.gridwidth = 2;

        frame.add(new JLabel(" "), constraints);  // blank JLabel







        //things to update the window title
        osmCoordsStopMap.getMainMap().addPropertyChangeListener("zoom", event -> updateWindowTitle(frame));

        osmCoordsStopMap.getMainMap().addPropertyChangeListener("center", evt -> updateWindowTitle(frame));

        gtfsCoordsStopMap.getMainMap().addPropertyChangeListener("zoom", event -> updateWindowTitle(frame));

        gtfsCoordsStopMap.getMainMap().addPropertyChangeListener("center", evt -> updateWindowTitle(frame));

        updateWindowTitle(frame);


        //we start with the first stop of the list
        nextStop();

        //we ensure the window is the right size so that every element fits. we need this to avoid weird components sizes at frame startup
        frame.pack();
    }


    protected void updateWindowTitle(JFrame frame)
    {
        double latFirst = osmCoordsStopMap.getMainMap().getCenterPosition().getLatitude();
        double lonFirst = osmCoordsStopMap.getMainMap().getCenterPosition().getLongitude();
        int zoomFirst = osmCoordsStopMap.getMainMap().getZoom();

        double latSecond = gtfsCoordsStopMap.getMainMap().getCenterPosition().getLatitude();
        double lonSecond = gtfsCoordsStopMap.getMainMap().getCenterPosition().getLongitude();
        int zoomSecond = gtfsCoordsStopMap.getMainMap().getZoom();

        frame.setTitle(String.format("GTFSOSMImport stops changes review - origmap coords: (%.2f / %.2f) - Zoom: %d // updmap coords: (%.2f / %.2f) - Zoom: %d // %dx%d", latFirst, lonFirst, zoomFirst, latSecond, lonSecond, zoomSecond, frame.getSize().width, frame.getSize().height));

    }


    private void nextStop() {
        if (iteratorStopsToReview.hasNext()){
            var newindex = iteratorStopsToReview.nextIndex(); //this function does not go ahead through the list
            var newstop = iteratorStopsToReview.next(); //goes ahead

            jListStops.setSelectedIndex(newindex);

            updateCoords(newstop);

            //we ensure that the currently selected index is visible without manual scrolling
            jListStops.ensureIndexIsVisible(jListStops.getSelectedIndex());

        } else {
            hideEverythingStopRelated();

            //JOptionPane.showMessageDialog(null, "You reached the end of the stop list that needed a review!", "End reached!", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void nextStopToReview() {//TODO: maybe find a better implementation of this function, as currently it involves too much operations with arrays. probably by following the original approach that is commented below
        /*
        int c = 0;

        while (c < 2){

            if (iteratorStopsToReview.nextIndex() <= 1){
                c = 1; //we need to do only a full cycle and not two (continue the cycle till the end and then restart from the top) if we were at some point ahead in the list of the stop
            }
        }*/


        int splitindex = iteratorStopsToReview.nextIndex();

        ArrayList<OSMStop> reorderedArray = new ArrayList<>();

        // Copy the elements of the original array after the index to the beginning of the reordered array
        for (int i = 0; i < osmStopsToReview.size() - splitindex; i++) {
            reorderedArray.add(i, osmStopsToReview.get(splitindex + i));
        }

        // Copy the elements of the original array before the index to the end of the reordered array
        for (int i = 0; i < splitindex; i++) {
            reorderedArray.add(osmStopsToReview.size() - splitindex + i, osmStopsToReview.get(i));
        }

        ListIterator<OSMStop> reorderiterator = reorderedArray.listIterator();

        OSMStop newstop;

        while (reorderiterator.hasNext() && finalReviewedGeopositions.containsKey(reorderedArray.get(reorderiterator.nextIndex()))){
            newstop = reorderiterator.next();
        }

        if (!reorderiterator.hasNext()){
            hideEverythingStopRelated();
        } else {

            int newindex = reorderiterator.nextIndex();
            newstop = reorderiterator.next();

            int indextoselect =osmStopsToReview.indexOf(newstop);


            //we re set the original iterator accordingly
            iteratorStopsToReview = osmStopsToReview.listIterator(indextoselect + 1);

            jListStops.setSelectedIndex(indextoselect);

            updateCoords(newstop);

            //we ensure that the currently selected index is visible without manual scrolling
            jListStops.ensureIndexIsVisible(jListStops.getSelectedIndex());
        }

    }

    private void showEverythingStopRelated() {
        osmCoordsStopMap.setVisible(true);
        gtfsCoordsStopMap.setVisible(true);
        labOsmCoords.setVisible(true);
        labGtfsCoords.setVisible(true);
        btChooseOSM.setVisible(true);
        btChooseGTFS.setVisible(true);

        labReviewCompleted.setVisible(false);
        btCloseWindow.setVisible(false);
    }

    private void hideEverythingStopRelated() {
        osmCoordsStopMap.setVisible(false);
        gtfsCoordsStopMap.setVisible(false);
        labOsmCoords.setVisible(false);
        labGtfsCoords.setVisible(false);
        btChooseOSM.setVisible(false);
        btChooseGTFS.setVisible(false);

        labReviewCompleted.setVisible(true);
        btCloseWindow.setVisible(true);
    }

    private void updateCoords(OSMStop currentStop) {

        //osm and gtfs coordinates in form of GeoPosition
        GeoPosition osmStopCoords = currentStop.getGeoPosition();
        GeoPosition gtfsStopCoords = currentStop.gtfsStopMatchedWith.getGeoPosition();

        //we update the maps
        osmCoordsStopMap.setAddressLocation(osmStopCoords);
        gtfsCoordsStopMap.setAddressLocation(gtfsStopCoords);


        //we update the variout interface texts
        labInfoStop1.setText(String.format(infoStopText, currentStop.getCode()));
        labOsmCoords.setText(String.format(textLabOsmCoords, osmStopCoords.getLatitude(), osmStopCoords.getLongitude()));
        labGtfsCoords.setText(String.format(textLabGtfsCoords, gtfsStopCoords.getLatitude(), gtfsStopCoords.getLongitude()));

    }


    private class CustomStopCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component component = super.getListCellRendererComponent(list, value, index, isSelected, isSelected); //we put the isSelected value for the cellHasFocus parameter so that the border of the cell gets rendered when the cell is selected too and not only when focused by a mouse click

            OSMStop thisCellStop = (OSMStop) value;

            setText(index + ") " + GTFSImportSettings.getInstance().getPlugin().fixBusStopName(thisCellStop.gtfsStopMatchedWith.getName()) + " (ref: " + thisCellStop.getCode() + ")"); //make sure to use only name/data from the gtfs match as it could be more up to date than the osm one

            if (finalReviewedGeopositions.get(thisCellStop) != null && finalReviewedGeopositions.get(thisCellStop).equals(thisCellStop.getGeoPosition())){ //the user accepted the OSM coordinates
                setBackground(new Color(120, 255, 120));

                setText(getText() + " / OSM Accepted");
            } else if (finalReviewedGeopositions.get(thisCellStop) != null && finalReviewedGeopositions.get(thisCellStop).equals(thisCellStop.gtfsStopMatchedWith.getGeoPosition())){ //the user accepted the GTFS coordinates
                setBackground(new Color(138, 234, 255));

                setText(getText() + " / GTFS Accepted");
            } else {
                //nothing got accepted i think
            }

            /*
            if (cellHasFocus){
                setBackground(new Color(255, 120, 241));
            }else {
                setBackground(new Color(120, 255, 120));
            }

             */


            return component;
        }
    }
}
