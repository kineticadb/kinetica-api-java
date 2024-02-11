/*
 *  This file was autogenerated by the Kinetica schema processor.
 *
 *  DO NOT EDIT DIRECTLY.
 */
package com.gpudb.protocol;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.IndexedRecord;

public class VisualizeImageClassbreakRequest implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("VisualizeImageClassbreakRequest")
            .namespace("com.gpudb")
            .fields()
                .name("tableNames").type().array().items().stringType().noDefault()
                .name("worldTableNames").type().array().items().stringType().noDefault()
                .name("xColumnName").type().stringType().noDefault()
                .name("yColumnName").type().stringType().noDefault()
                .name("symbolColumnName").type().stringType().noDefault()
                .name("geometryColumnName").type().stringType().noDefault()
                .name("trackIds").type().array().items().array().items().stringType().noDefault()
                .name("cbAttr").type().stringType().noDefault()
                .name("cbVals").type().array().items().stringType().noDefault()
                .name("cbPointcolorAttr").type().stringType().noDefault()
                .name("cbPointcolorVals").type().array().items().stringType().noDefault()
                .name("cbPointalphaAttr").type().stringType().noDefault()
                .name("cbPointalphaVals").type().array().items().stringType().noDefault()
                .name("cbPointsizeAttr").type().stringType().noDefault()
                .name("cbPointsizeVals").type().array().items().stringType().noDefault()
                .name("cbPointshapeAttr").type().stringType().noDefault()
                .name("cbPointshapeVals").type().array().items().stringType().noDefault()
                .name("minX").type().doubleType().noDefault()
                .name("maxX").type().doubleType().noDefault()
                .name("minY").type().doubleType().noDefault()
                .name("maxY").type().doubleType().noDefault()
                .name("width").type().intType().noDefault()
                .name("height").type().intType().noDefault()
                .name("projection").type().stringType().noDefault()
                .name("bgColor").type().longType().noDefault()
                .name("styleOptions").type().map().values().array().items().stringType().noDefault()
                .name("options").type().map().values().stringType().noDefault()
                .name("cbTransparencyVec").type().array().items().intType().noDefault()
            .endRecord();

    public static Schema getClassSchema() {
        return schema$;
    }

    public static final class Projection {
        public static final String EPSG_4326 = "EPSG:4326";
        public static final String PLATE_CARREE = "PLATE_CARREE";
        public static final String _900913 = "900913";
        public static final String EPSG_900913 = "EPSG:900913";
        public static final String _102100 = "102100";
        public static final String EPSG_102100 = "EPSG:102100";
        public static final String _3857 = "3857";
        public static final String EPSG_3857 = "EPSG:3857";
        public static final String WEB_MERCATOR = "WEB_MERCATOR";

        private Projection() {  }
    }

    public static final class StyleOptions {
        public static final String DO_POINTS = "do_points";
        public static final String TRUE = "true";
        public static final String FALSE = "false";
        public static final String DO_SHAPES = "do_shapes";
        public static final String DO_TRACKS = "do_tracks";
        public static final String DO_SYMBOLOGY = "do_symbology";
        public static final String POINTCOLORS = "pointcolors";
        public static final String CB_POINTALPHAS = "cb_pointalphas";
        public static final String POINTSIZES = "pointsizes";
        public static final String POINTOFFSET_X = "pointoffset_x";
        public static final String POINTOFFSET_Y = "pointoffset_y";
        public static final String POINTSHAPES = "pointshapes";
        public static final String NONE = "none";
        public static final String CIRCLE = "circle";
        public static final String SQUARE = "square";
        public static final String DIAMOND = "diamond";
        public static final String HOLLOWCIRCLE = "hollowcircle";
        public static final String HOLLOWSQUARE = "hollowsquare";
        public static final String HOLLOWDIAMOND = "hollowdiamond";
        public static final String SYMBOLCODE = "symbolcode";
        public static final String DASH = "dash";
        public static final String PIPE = "pipe";
        public static final String PLUS = "plus";
        public static final String HOLLOWSQUAREWITHPLUS = "hollowsquarewithplus";
        public static final String DOT = "dot";
        public static final String SYMBOLROTATIONS = "symbolrotations";
        public static final String SHAPELINEWIDTHS = "shapelinewidths";
        public static final String SHAPELINECOLORS = "shapelinecolors";
        public static final String SHAPELINEPATTERNS = "shapelinepatterns";
        public static final String SHAPELINEPATTERNLEN = "shapelinepatternlen";
        public static final String SHAPEFILLCOLORS = "shapefillcolors";
        public static final String HASHLINEINTERVALS = "hashlineintervals";
        public static final String HASHLINECOLORS = "hashlinecolors";
        public static final String HASHLINEANGLES = "hashlineangles";
        public static final String HASHLINELENS = "hashlinelens";
        public static final String HASHLINEWIDTHS = "hashlinewidths";
        public static final String TRACKLINEWIDTHS = "tracklinewidths";
        public static final String TRACKLINECOLORS = "tracklinecolors";
        public static final String TRACKMARKERSIZES = "trackmarkersizes";
        public static final String TRACKMARKERCOLORS = "trackmarkercolors";
        public static final String TRACKMARKERSHAPES = "trackmarkershapes";
        public static final String ORIENTED_ARROW = "oriented_arrow";
        public static final String ORIENTED_TRIANGLE = "oriented_triangle";
        public static final String TRACKHEADCOLORS = "trackheadcolors";
        public static final String TRACKHEADSIZES = "trackheadsizes";
        public static final String TRACKHEADSHAPES = "trackheadshapes";

        private StyleOptions() {  }
    }

    public static final class Options {
        public static final String TRACK_ID_COLUMN_NAME = "track_id_column_name";
        public static final String TRACK_ORDER_COLUMN_NAME = "track_order_column_name";

        private Options() {  }
    }

    private List<String> tableNames;
    private List<String> worldTableNames;
    private String xColumnName;
    private String yColumnName;
    private String symbolColumnName;
    private String geometryColumnName;
    private List<List<String>> trackIds;
    private String cbAttr;
    private List<String> cbVals;
    private String cbPointcolorAttr;
    private List<String> cbPointcolorVals;
    private String cbPointalphaAttr;
    private List<String> cbPointalphaVals;
    private String cbPointsizeAttr;
    private List<String> cbPointsizeVals;
    private String cbPointshapeAttr;
    private List<String> cbPointshapeVals;
    private double minX;
    private double maxX;
    private double minY;
    private double maxY;
    private int width;
    private int height;
    private String projection;
    private long bgColor;
    private Map<String, List<String>> styleOptions;
    private Map<String, String> options;
    private List<Integer> cbTransparencyVec;

    public VisualizeImageClassbreakRequest() {
        tableNames = new ArrayList<>();
        worldTableNames = new ArrayList<>();
        xColumnName = "";
        yColumnName = "";
        symbolColumnName = "";
        geometryColumnName = "";
        trackIds = new ArrayList<>();
        cbAttr = "";
        cbVals = new ArrayList<>();
        cbPointcolorAttr = "";
        cbPointcolorVals = new ArrayList<>();
        cbPointalphaAttr = "";
        cbPointalphaVals = new ArrayList<>();
        cbPointsizeAttr = "";
        cbPointsizeVals = new ArrayList<>();
        cbPointshapeAttr = "";
        cbPointshapeVals = new ArrayList<>();
        projection = "";
        styleOptions = new LinkedHashMap<>();
        options = new LinkedHashMap<>();
        cbTransparencyVec = new ArrayList<>();
    }

    public VisualizeImageClassbreakRequest(List<String> tableNames, List<String> worldTableNames, String xColumnName, String yColumnName, String symbolColumnName, String geometryColumnName, List<List<String>> trackIds, String cbAttr, List<String> cbVals, String cbPointcolorAttr, List<String> cbPointcolorVals, String cbPointalphaAttr, List<String> cbPointalphaVals, String cbPointsizeAttr, List<String> cbPointsizeVals, String cbPointshapeAttr, List<String> cbPointshapeVals, double minX, double maxX, double minY, double maxY, int width, int height, String projection, long bgColor, Map<String, List<String>> styleOptions, Map<String, String> options, List<Integer> cbTransparencyVec) {
        this.tableNames = (tableNames == null) ? new ArrayList<String>() : tableNames;
        this.worldTableNames = (worldTableNames == null) ? new ArrayList<String>() : worldTableNames;
        this.xColumnName = (xColumnName == null) ? "" : xColumnName;
        this.yColumnName = (yColumnName == null) ? "" : yColumnName;
        this.symbolColumnName = (symbolColumnName == null) ? "" : symbolColumnName;
        this.geometryColumnName = (geometryColumnName == null) ? "" : geometryColumnName;
        this.trackIds = (trackIds == null) ? new ArrayList<List<String>>() : trackIds;
        this.cbAttr = (cbAttr == null) ? "" : cbAttr;
        this.cbVals = (cbVals == null) ? new ArrayList<String>() : cbVals;
        this.cbPointcolorAttr = (cbPointcolorAttr == null) ? "" : cbPointcolorAttr;
        this.cbPointcolorVals = (cbPointcolorVals == null) ? new ArrayList<String>() : cbPointcolorVals;
        this.cbPointalphaAttr = (cbPointalphaAttr == null) ? "" : cbPointalphaAttr;
        this.cbPointalphaVals = (cbPointalphaVals == null) ? new ArrayList<String>() : cbPointalphaVals;
        this.cbPointsizeAttr = (cbPointsizeAttr == null) ? "" : cbPointsizeAttr;
        this.cbPointsizeVals = (cbPointsizeVals == null) ? new ArrayList<String>() : cbPointsizeVals;
        this.cbPointshapeAttr = (cbPointshapeAttr == null) ? "" : cbPointshapeAttr;
        this.cbPointshapeVals = (cbPointshapeVals == null) ? new ArrayList<String>() : cbPointshapeVals;
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
        this.width = width;
        this.height = height;
        this.projection = (projection == null) ? "" : projection;
        this.bgColor = bgColor;
        this.styleOptions = (styleOptions == null) ? new LinkedHashMap<String, List<String>>() : styleOptions;
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
        this.cbTransparencyVec = (cbTransparencyVec == null) ? new ArrayList<Integer>() : cbTransparencyVec;
    }

    public List<String> getTableNames() {
        return tableNames;
    }

    public VisualizeImageClassbreakRequest setTableNames(List<String> tableNames) {
        this.tableNames = (tableNames == null) ? new ArrayList<String>() : tableNames;
        return this;
    }

    public List<String> getWorldTableNames() {
        return worldTableNames;
    }

    public VisualizeImageClassbreakRequest setWorldTableNames(List<String> worldTableNames) {
        this.worldTableNames = (worldTableNames == null) ? new ArrayList<String>() : worldTableNames;
        return this;
    }

    public String getXColumnName() {
        return xColumnName;
    }

    public VisualizeImageClassbreakRequest setXColumnName(String xColumnName) {
        this.xColumnName = (xColumnName == null) ? "" : xColumnName;
        return this;
    }

    public String getYColumnName() {
        return yColumnName;
    }

    public VisualizeImageClassbreakRequest setYColumnName(String yColumnName) {
        this.yColumnName = (yColumnName == null) ? "" : yColumnName;
        return this;
    }

    public String getSymbolColumnName() {
        return symbolColumnName;
    }

    public VisualizeImageClassbreakRequest setSymbolColumnName(String symbolColumnName) {
        this.symbolColumnName = (symbolColumnName == null) ? "" : symbolColumnName;
        return this;
    }

    public String getGeometryColumnName() {
        return geometryColumnName;
    }

    public VisualizeImageClassbreakRequest setGeometryColumnName(String geometryColumnName) {
        this.geometryColumnName = (geometryColumnName == null) ? "" : geometryColumnName;
        return this;
    }

    public List<List<String>> getTrackIds() {
        return trackIds;
    }

    public VisualizeImageClassbreakRequest setTrackIds(List<List<String>> trackIds) {
        this.trackIds = (trackIds == null) ? new ArrayList<List<String>>() : trackIds;
        return this;
    }

    public String getCbAttr() {
        return cbAttr;
    }

    public VisualizeImageClassbreakRequest setCbAttr(String cbAttr) {
        this.cbAttr = (cbAttr == null) ? "" : cbAttr;
        return this;
    }

    public List<String> getCbVals() {
        return cbVals;
    }

    public VisualizeImageClassbreakRequest setCbVals(List<String> cbVals) {
        this.cbVals = (cbVals == null) ? new ArrayList<String>() : cbVals;
        return this;
    }

    public String getCbPointcolorAttr() {
        return cbPointcolorAttr;
    }

    public VisualizeImageClassbreakRequest setCbPointcolorAttr(String cbPointcolorAttr) {
        this.cbPointcolorAttr = (cbPointcolorAttr == null) ? "" : cbPointcolorAttr;
        return this;
    }

    public List<String> getCbPointcolorVals() {
        return cbPointcolorVals;
    }

    public VisualizeImageClassbreakRequest setCbPointcolorVals(List<String> cbPointcolorVals) {
        this.cbPointcolorVals = (cbPointcolorVals == null) ? new ArrayList<String>() : cbPointcolorVals;
        return this;
    }

    public String getCbPointalphaAttr() {
        return cbPointalphaAttr;
    }

    public VisualizeImageClassbreakRequest setCbPointalphaAttr(String cbPointalphaAttr) {
        this.cbPointalphaAttr = (cbPointalphaAttr == null) ? "" : cbPointalphaAttr;
        return this;
    }

    public List<String> getCbPointalphaVals() {
        return cbPointalphaVals;
    }

    public VisualizeImageClassbreakRequest setCbPointalphaVals(List<String> cbPointalphaVals) {
        this.cbPointalphaVals = (cbPointalphaVals == null) ? new ArrayList<String>() : cbPointalphaVals;
        return this;
    }

    public String getCbPointsizeAttr() {
        return cbPointsizeAttr;
    }

    public VisualizeImageClassbreakRequest setCbPointsizeAttr(String cbPointsizeAttr) {
        this.cbPointsizeAttr = (cbPointsizeAttr == null) ? "" : cbPointsizeAttr;
        return this;
    }

    public List<String> getCbPointsizeVals() {
        return cbPointsizeVals;
    }

    public VisualizeImageClassbreakRequest setCbPointsizeVals(List<String> cbPointsizeVals) {
        this.cbPointsizeVals = (cbPointsizeVals == null) ? new ArrayList<String>() : cbPointsizeVals;
        return this;
    }

    public String getCbPointshapeAttr() {
        return cbPointshapeAttr;
    }

    public VisualizeImageClassbreakRequest setCbPointshapeAttr(String cbPointshapeAttr) {
        this.cbPointshapeAttr = (cbPointshapeAttr == null) ? "" : cbPointshapeAttr;
        return this;
    }

    public List<String> getCbPointshapeVals() {
        return cbPointshapeVals;
    }

    public VisualizeImageClassbreakRequest setCbPointshapeVals(List<String> cbPointshapeVals) {
        this.cbPointshapeVals = (cbPointshapeVals == null) ? new ArrayList<String>() : cbPointshapeVals;
        return this;
    }

    public double getMinX() {
        return minX;
    }

    public VisualizeImageClassbreakRequest setMinX(double minX) {
        this.minX = minX;
        return this;
    }

    public double getMaxX() {
        return maxX;
    }

    public VisualizeImageClassbreakRequest setMaxX(double maxX) {
        this.maxX = maxX;
        return this;
    }

    public double getMinY() {
        return minY;
    }

    public VisualizeImageClassbreakRequest setMinY(double minY) {
        this.minY = minY;
        return this;
    }

    public double getMaxY() {
        return maxY;
    }

    public VisualizeImageClassbreakRequest setMaxY(double maxY) {
        this.maxY = maxY;
        return this;
    }

    public int getWidth() {
        return width;
    }

    public VisualizeImageClassbreakRequest setWidth(int width) {
        this.width = width;
        return this;
    }

    public int getHeight() {
        return height;
    }

    public VisualizeImageClassbreakRequest setHeight(int height) {
        this.height = height;
        return this;
    }

    public String getProjection() {
        return projection;
    }

    public VisualizeImageClassbreakRequest setProjection(String projection) {
        this.projection = (projection == null) ? "" : projection;
        return this;
    }

    public long getBgColor() {
        return bgColor;
    }

    public VisualizeImageClassbreakRequest setBgColor(long bgColor) {
        this.bgColor = bgColor;
        return this;
    }

    public Map<String, List<String>> getStyleOptions() {
        return styleOptions;
    }

    public VisualizeImageClassbreakRequest setStyleOptions(Map<String, List<String>> styleOptions) {
        this.styleOptions = (styleOptions == null) ? new LinkedHashMap<String, List<String>>() : styleOptions;
        return this;
    }

    public Map<String, String> getOptions() {
        return options;
    }

    public VisualizeImageClassbreakRequest setOptions(Map<String, String> options) {
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
        return this;
    }

    public List<Integer> getCbTransparencyVec() {
        return cbTransparencyVec;
    }

    public VisualizeImageClassbreakRequest setCbTransparencyVec(List<Integer> cbTransparencyVec) {
        this.cbTransparencyVec = (cbTransparencyVec == null) ? new ArrayList<Integer>() : cbTransparencyVec;
        return this;
    }

    @Override
    public Schema getSchema() {
        return schema$;
    }

    @Override
    public Object get(int index) {
        switch (index) {
            case 0:
                return this.tableNames;

            case 1:
                return this.worldTableNames;

            case 2:
                return this.xColumnName;

            case 3:
                return this.yColumnName;

            case 4:
                return this.symbolColumnName;

            case 5:
                return this.geometryColumnName;

            case 6:
                return this.trackIds;

            case 7:
                return this.cbAttr;

            case 8:
                return this.cbVals;

            case 9:
                return this.cbPointcolorAttr;

            case 10:
                return this.cbPointcolorVals;

            case 11:
                return this.cbPointalphaAttr;

            case 12:
                return this.cbPointalphaVals;

            case 13:
                return this.cbPointsizeAttr;

            case 14:
                return this.cbPointsizeVals;

            case 15:
                return this.cbPointshapeAttr;

            case 16:
                return this.cbPointshapeVals;

            case 17:
                return this.minX;

            case 18:
                return this.maxX;

            case 19:
                return this.minY;

            case 20:
                return this.maxY;

            case 21:
                return this.width;

            case 22:
                return this.height;

            case 23:
                return this.projection;

            case 24:
                return this.bgColor;

            case 25:
                return this.styleOptions;

            case 26:
                return this.options;

            case 27:
                return this.cbTransparencyVec;

            default:
                throw new IndexOutOfBoundsException("Invalid index specified.");
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void put(int index, Object value) {
        switch (index) {
            case 0:
                this.tableNames = (List<String>)value;
                break;

            case 1:
                this.worldTableNames = (List<String>)value;
                break;

            case 2:
                this.xColumnName = (String)value;
                break;

            case 3:
                this.yColumnName = (String)value;
                break;

            case 4:
                this.symbolColumnName = (String)value;
                break;

            case 5:
                this.geometryColumnName = (String)value;
                break;

            case 6:
                this.trackIds = (List<List<String>>)value;
                break;

            case 7:
                this.cbAttr = (String)value;
                break;

            case 8:
                this.cbVals = (List<String>)value;
                break;

            case 9:
                this.cbPointcolorAttr = (String)value;
                break;

            case 10:
                this.cbPointcolorVals = (List<String>)value;
                break;

            case 11:
                this.cbPointalphaAttr = (String)value;
                break;

            case 12:
                this.cbPointalphaVals = (List<String>)value;
                break;

            case 13:
                this.cbPointsizeAttr = (String)value;
                break;

            case 14:
                this.cbPointsizeVals = (List<String>)value;
                break;

            case 15:
                this.cbPointshapeAttr = (String)value;
                break;

            case 16:
                this.cbPointshapeVals = (List<String>)value;
                break;

            case 17:
                this.minX = (Double)value;
                break;

            case 18:
                this.maxX = (Double)value;
                break;

            case 19:
                this.minY = (Double)value;
                break;

            case 20:
                this.maxY = (Double)value;
                break;

            case 21:
                this.width = (Integer)value;
                break;

            case 22:
                this.height = (Integer)value;
                break;

            case 23:
                this.projection = (String)value;
                break;

            case 24:
                this.bgColor = (Long)value;
                break;

            case 25:
                this.styleOptions = (Map<String, List<String>>)value;
                break;

            case 26:
                this.options = (Map<String, String>)value;
                break;

            case 27:
                this.cbTransparencyVec = (List<Integer>)value;
                break;

            default:
                throw new IndexOutOfBoundsException("Invalid index specified.");
        }
    }

    @Override
    public boolean equals(Object obj) {
        if( obj == this ) {
            return true;
        }

        if( (obj == null) || (obj.getClass() != this.getClass()) ) {
            return false;
        }

        VisualizeImageClassbreakRequest that = (VisualizeImageClassbreakRequest)obj;

        return ( this.tableNames.equals( that.tableNames )
                 && this.worldTableNames.equals( that.worldTableNames )
                 && this.xColumnName.equals( that.xColumnName )
                 && this.yColumnName.equals( that.yColumnName )
                 && this.symbolColumnName.equals( that.symbolColumnName )
                 && this.geometryColumnName.equals( that.geometryColumnName )
                 && this.trackIds.equals( that.trackIds )
                 && this.cbAttr.equals( that.cbAttr )
                 && this.cbVals.equals( that.cbVals )
                 && this.cbPointcolorAttr.equals( that.cbPointcolorAttr )
                 && this.cbPointcolorVals.equals( that.cbPointcolorVals )
                 && this.cbPointalphaAttr.equals( that.cbPointalphaAttr )
                 && this.cbPointalphaVals.equals( that.cbPointalphaVals )
                 && this.cbPointsizeAttr.equals( that.cbPointsizeAttr )
                 && this.cbPointsizeVals.equals( that.cbPointsizeVals )
                 && this.cbPointshapeAttr.equals( that.cbPointshapeAttr )
                 && this.cbPointshapeVals.equals( that.cbPointshapeVals )
                 && ( (Double)this.minX ).equals( (Double)that.minX )
                 && ( (Double)this.maxX ).equals( (Double)that.maxX )
                 && ( (Double)this.minY ).equals( (Double)that.minY )
                 && ( (Double)this.maxY ).equals( (Double)that.maxY )
                 && ( this.width == that.width )
                 && ( this.height == that.height )
                 && this.projection.equals( that.projection )
                 && ( this.bgColor == that.bgColor )
                 && this.styleOptions.equals( that.styleOptions )
                 && this.options.equals( that.options )
                 && this.cbTransparencyVec.equals( that.cbTransparencyVec ) );
    }

    @Override
    public String toString() {
        GenericData gd = GenericData.get();
        StringBuilder builder = new StringBuilder();
        builder.append( "{" );
        builder.append( gd.toString( "tableNames" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.tableNames ) );
        builder.append( ", " );
        builder.append( gd.toString( "worldTableNames" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.worldTableNames ) );
        builder.append( ", " );
        builder.append( gd.toString( "xColumnName" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.xColumnName ) );
        builder.append( ", " );
        builder.append( gd.toString( "yColumnName" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.yColumnName ) );
        builder.append( ", " );
        builder.append( gd.toString( "symbolColumnName" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.symbolColumnName ) );
        builder.append( ", " );
        builder.append( gd.toString( "geometryColumnName" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.geometryColumnName ) );
        builder.append( ", " );
        builder.append( gd.toString( "trackIds" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.trackIds ) );
        builder.append( ", " );
        builder.append( gd.toString( "cbAttr" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.cbAttr ) );
        builder.append( ", " );
        builder.append( gd.toString( "cbVals" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.cbVals ) );
        builder.append( ", " );
        builder.append( gd.toString( "cbPointcolorAttr" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.cbPointcolorAttr ) );
        builder.append( ", " );
        builder.append( gd.toString( "cbPointcolorVals" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.cbPointcolorVals ) );
        builder.append( ", " );
        builder.append( gd.toString( "cbPointalphaAttr" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.cbPointalphaAttr ) );
        builder.append( ", " );
        builder.append( gd.toString( "cbPointalphaVals" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.cbPointalphaVals ) );
        builder.append( ", " );
        builder.append( gd.toString( "cbPointsizeAttr" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.cbPointsizeAttr ) );
        builder.append( ", " );
        builder.append( gd.toString( "cbPointsizeVals" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.cbPointsizeVals ) );
        builder.append( ", " );
        builder.append( gd.toString( "cbPointshapeAttr" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.cbPointshapeAttr ) );
        builder.append( ", " );
        builder.append( gd.toString( "cbPointshapeVals" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.cbPointshapeVals ) );
        builder.append( ", " );
        builder.append( gd.toString( "minX" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.minX ) );
        builder.append( ", " );
        builder.append( gd.toString( "maxX" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.maxX ) );
        builder.append( ", " );
        builder.append( gd.toString( "minY" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.minY ) );
        builder.append( ", " );
        builder.append( gd.toString( "maxY" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.maxY ) );
        builder.append( ", " );
        builder.append( gd.toString( "width" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.width ) );
        builder.append( ", " );
        builder.append( gd.toString( "height" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.height ) );
        builder.append( ", " );
        builder.append( gd.toString( "projection" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.projection ) );
        builder.append( ", " );
        builder.append( gd.toString( "bgColor" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.bgColor ) );
        builder.append( ", " );
        builder.append( gd.toString( "styleOptions" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.styleOptions ) );
        builder.append( ", " );
        builder.append( gd.toString( "options" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.options ) );
        builder.append( ", " );
        builder.append( gd.toString( "cbTransparencyVec" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.cbTransparencyVec ) );
        builder.append( "}" );

        return builder.toString();
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        hashCode = (31 * hashCode) + this.tableNames.hashCode();
        hashCode = (31 * hashCode) + this.worldTableNames.hashCode();
        hashCode = (31 * hashCode) + this.xColumnName.hashCode();
        hashCode = (31 * hashCode) + this.yColumnName.hashCode();
        hashCode = (31 * hashCode) + this.symbolColumnName.hashCode();
        hashCode = (31 * hashCode) + this.geometryColumnName.hashCode();
        hashCode = (31 * hashCode) + this.trackIds.hashCode();
        hashCode = (31 * hashCode) + this.cbAttr.hashCode();
        hashCode = (31 * hashCode) + this.cbVals.hashCode();
        hashCode = (31 * hashCode) + this.cbPointcolorAttr.hashCode();
        hashCode = (31 * hashCode) + this.cbPointcolorVals.hashCode();
        hashCode = (31 * hashCode) + this.cbPointalphaAttr.hashCode();
        hashCode = (31 * hashCode) + this.cbPointalphaVals.hashCode();
        hashCode = (31 * hashCode) + this.cbPointsizeAttr.hashCode();
        hashCode = (31 * hashCode) + this.cbPointsizeVals.hashCode();
        hashCode = (31 * hashCode) + this.cbPointshapeAttr.hashCode();
        hashCode = (31 * hashCode) + this.cbPointshapeVals.hashCode();
        hashCode = (31 * hashCode) + ((Double)this.minX).hashCode();
        hashCode = (31 * hashCode) + ((Double)this.maxX).hashCode();
        hashCode = (31 * hashCode) + ((Double)this.minY).hashCode();
        hashCode = (31 * hashCode) + ((Double)this.maxY).hashCode();
        hashCode = (31 * hashCode) + this.width;
        hashCode = (31 * hashCode) + this.height;
        hashCode = (31 * hashCode) + this.projection.hashCode();
        hashCode = (31 * hashCode) + ((Long)this.bgColor).hashCode();
        hashCode = (31 * hashCode) + this.styleOptions.hashCode();
        hashCode = (31 * hashCode) + this.options.hashCode();
        hashCode = (31 * hashCode) + this.cbTransparencyVec.hashCode();
        return hashCode;
    }
}
