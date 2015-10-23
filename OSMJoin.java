
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.TreeSet;
import java.util.Vector;

public class OSMJoin {

    private Vector<String> osmInFiles = new Vector<String>();
    private String osmOutFile = "";
    private boolean offset = true;
    private double disOffset = 0.001;

    private TreeSet<Long> allIDs = new TreeSet<Long>();

    private double minlat = Double.MAX_VALUE;
    private double maxlat = Double.MIN_VALUE;
    private double minlon = Double.MAX_VALUE;
    private double maxlon = Double.MIN_VALUE;

    public static void main(String[] args) {
        OSMJoin oj = new OSMJoin();
        oj.start(args);
    }

    public void start(String[] args) {
        checkArgs(args);

        readMinMaxBoundaries();

        readIDs();

        writeNewOsm();
    }

    /**
     * write new osm file: 1. write 4 nodes for two new ways in north and south
     * 2. write all nodes 3. write 2 way in north and south 4. write all ways
     */
    public void writeNewOsm() {

        long nodeId1 = 0;
        long nodeId2 = 0;
        long nodeId3 = 0;
        long nodeId4 = 0;

        try {
            BufferedWriter bWriter;
            bWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(osmOutFile)), "UTF-8"));

            bWriter.write("<?xml version='1.0' encoding='UTF-8'?>" + System.lineSeparator());
            bWriter.write("<osm version=\"0.6\" generator=\"osmconvert 0.8\" timestamp=\"2015-08-07T20:00:00Z\">" + System.lineSeparator());

            // write 4 nodes for two new ways in north and south
            if (offset) {
                bWriter.write("<bounds minlat=\"" + (minlat - (2 * disOffset)) + "\" minlon=\"" + (minlon - (2 * disOffset)) + "\" maxlat=\"" + (maxlat + (2 * disOffset)) + "\" maxlon=\"" + (maxlon + (2 * disOffset)) + "\"/>" + System.lineSeparator());

                nodeId1 = getNewID();
                bWriter.write("	<node id=\"" + nodeId1 + "\" lat=\"" + (minlat - disOffset) + "\" lon=\"" + (minlon - disOffset) + "\" />");
                bWriter.write(System.lineSeparator());

                nodeId2 = getNewID();
                bWriter.write("	<node id=\"" + nodeId2 + "\" lat=\"" + (minlat - disOffset) + "\" lon=\"" + (maxlon + disOffset) + "\" />");
                bWriter.write(System.lineSeparator());

                nodeId3 = getNewID();
                bWriter.write("	<node id=\"" + nodeId3 + "\" lat=\"" + (maxlat + disOffset) + "\" lon=\"" + (maxlon + disOffset) + "\" />");
                bWriter.write(System.lineSeparator());

                nodeId4 = getNewID();
                bWriter.write("	<node id=\"" + nodeId4 + "\" lat=\"" + (maxlat + disOffset) + "\" lon=\"" + (minlon - disOffset) + "\" />");
                bWriter.write(System.lineSeparator());
            } else {
                bWriter.write("<bounds minlat=\"" + minlat + "\" minlon=\"" + minlon + "\" maxlat=\"" + maxlat + "\" maxlon=\"" + maxlon + "\"/>" + System.lineSeparator());
            }

            TreeSet<Long> nodeIDs = new TreeSet<Long>();

            // write all nodes
            for (String path : osmInFiles) {
                BufferedReader bReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(path)), "UTF-8"));
                String line = bReader.readLine();
                while (line != null) {
                    String tline = line.trim();
                    if (tline.startsWith("<node id=\"")) {
                        String s = tline.replace("<node id=\"", "").split("\"")[0];
                        long id = Long.parseLong(s);

                        boolean write = nodeIDs.contains(id) == false;

                        if (tline.endsWith("/>")) {
                            if (write) {
                                bWriter.write(line + System.lineSeparator());
                            }
                        } else if (tline.endsWith(">")) {

                            if (write) {
                                bWriter.write(line + System.lineSeparator());
                            }

                            line = bReader.readLine();
                            tline = line.trim();

                            while (tline.equals("</node>") == false) {
                                if (write) {
                                    bWriter.write(line + System.lineSeparator());
                                }
                                line = bReader.readLine();
                                tline = line.trim();
                            }

                            if (write) {
                                bWriter.write(line + System.lineSeparator());
                            }

                            nodeIDs.add(id);

                        } else {
                            System.out.println("Error: End of node: " + id);
                        }
                    }
                    line = bReader.readLine();
                }
                bReader.close();
            }

            // write 2 way in north and south
            if (offset) {
                long idway = this.getNewID();
                bWriter.write("	<way id=\"" + idway + "\" >" + System.lineSeparator());
                bWriter.write("		<nd ref=\"" + nodeId1 + "\"/>" + System.lineSeparator());
                bWriter.write("		<nd ref=\"" + nodeId2 + "\"/>" + System.lineSeparator());
                bWriter.write("		<tag k=\"highway\" v=\"unclassified\"/>" + System.lineSeparator());
                bWriter.write("		<tag k=\"oneway\" v=\"yes\"/>" + System.lineSeparator());
                bWriter.write("	</way>" + System.lineSeparator());

                idway = this.getNewID();
                bWriter.write("	<way id=\"" + idway + "\" >" + System.lineSeparator());
                bWriter.write("		<nd ref=\"" + nodeId3 + "\"/>" + System.lineSeparator());
                bWriter.write("		<nd ref=\"" + nodeId4 + "\"/>" + System.lineSeparator());
                bWriter.write("		<tag k=\"highway\" v=\"unclassified\"/>" + System.lineSeparator());
                bWriter.write("		<tag k=\"oneway\" v=\"yes\"/>" + System.lineSeparator());
                bWriter.write("	</way>" + System.lineSeparator());
            }

            TreeSet<Long> wayIDs = new TreeSet<Long>();

            // write all ways
            for (String path : osmInFiles) {
                BufferedReader bReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(path)), "UTF-8"));
                String line = bReader.readLine();
                while (line != null) {
                    String tline = line.trim();
                    if (tline.startsWith("<way id=\"")) {
                        String s = tline.replace("<way id=\"", "").split("\"")[0];
                        long id = Long.parseLong(s);

                        boolean write = wayIDs.contains(id) == false;

                        if (write) {
                            bWriter.write(line + System.lineSeparator());
                        }

                        line = bReader.readLine();
                        tline = line.trim();

                        while (tline.startsWith("</way>") == false) {
                            if (write) {
                                bWriter.write(line + System.lineSeparator());
                            }

                            line = bReader.readLine();
                            tline = line.trim();
                        }

                        if (write) {
                            bWriter.write(line + System.lineSeparator());
                        }

                        wayIDs.add(id);
                    }
                    line = bReader.readLine();
                }
                bReader.close();
            }

            bWriter.write("</osm>" + System.lineSeparator());
            bWriter.close();

        } catch (Exception e) {
            System.out.print("Error: writeNewOsm: \n");
            e.printStackTrace();
            System.out.println(e.toString());
        }

    }

    /**
     *
     * @return create and return new unique id
     */
    public long getNewID() {

        for (long i = 1; i < 10000000; i++) {
            if (allIDs.contains(i) == false) {
                allIDs.add(i);
                return i;
            }
        }

        return -1;
    }

    /**
     * search after "used" id in all osm files and save them in allIDs
     */
    public void readIDs() {
        for (String path : osmInFiles) {
            try {
                BufferedReader bReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(path)), "UTF-8"));
                String line = bReader.readLine();
                while (line != null) {
                    if (line.contains(" id=\"")) {
                        String sa[] = line.split(" id=\"");
                        for (int i = 1; i < sa.length; i++) {
                            long l = Long.parseLong(sa[i].split("\"")[0]);
                            allIDs.add(l);
                        }
                    }
                    line = bReader.readLine();
                }
                bReader.close();
            } catch (Exception e) {
                System.out.print("Error: readIDs: \n");
                e.printStackTrace();
                System.out.println(e.toString());
            }
        }
    }

    /**
     * read the min and max GOS coordinates of all osm files
     */
    public void readMinMaxBoundaries() {
        for (String path : osmInFiles) {
            try {
                BufferedReader bReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(path)), "UTF-8"));
                String line = bReader.readLine();
                while (line != null) {
                    line = line.trim();
                    if (line.startsWith("<bounds")) {
                        String sa[] = line.split("minlat=\"");
                        sa = sa[1].split("\"");
                        double d = Double.parseDouble(sa[0]);
                        if (this.minlat > d) {
                            this.minlat = d;
                        }

                        sa = line.split("maxlat=\"");
                        sa = sa[1].split("\"");
                        d = Double.parseDouble(sa[0]);
                        if (this.maxlat < d) {
                            this.maxlat = d;
                        }

                        sa = line.split("minlon=\"");
                        sa = sa[1].split("\"");
                        d = Double.parseDouble(sa[0]);
                        if (this.minlon > d) {
                            this.minlon = d;
                        }

                        sa = line.split("maxlon=\"");
                        sa = sa[1].split("\"");
                        d = Double.parseDouble(sa[0]);
                        if (this.maxlon < d) {
                            this.maxlon = d;
                        }
                        break;
                    }
                    line = bReader.readLine();
                }
                bReader.close();
            } catch (Exception e) {
                System.out.print("Error: readMinMaxBoundaries: \n");
                e.printStackTrace();
                System.out.println(e.toString());
            }
        }
    }

    /**
     * check input args of user
     *
     * @param args
     */
    public void checkArgs(String[] args) {
        try {
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("-i")) {
                    i++;
                    osmInFiles.add(args[i]);
                } else if (args[i].equals("-o")) {
                    i++;
                    osmOutFile = args[i];
                } else if (args[i].equals("-ow")) {
                    i++;
                    offset = args[i].equals("true");
                }
            }
        } catch (Exception e) {
            printParameterInfo();
            System.exit(-1);
        }
        if (osmInFiles.size() == 0 || osmOutFile == "") {
            printParameterInfo();
            System.exit(-1);
        }
    }

    /**
     * print description of args for user
     */
    public void printParameterInfo() {
        System.out.println("");
        System.out.println("Parameter: ");
        System.out.println(" -i -> Path of Osm-Input-file (required & multi)");
        System.out.println(" -o -> Path of Osm-Output-file (required)");
        System.out.println(" -ow -> [true / false ] add a Offset Way with min/max  GPS coordinates (default: true)");
        System.out.println("");
    }
}
