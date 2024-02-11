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

public class VisualizeImageContourRequest implements IndexedRecord {
    private static final Schema schema$ = SchemaBuilder
            .record("VisualizeImageContourRequest")
            .namespace("com.gpudb")
            .fields()
                .name("tableNames").type().array().items().stringType().noDefault()
                .name("xColumnName").type().stringType().noDefault()
                .name("yColumnName").type().stringType().noDefault()
                .name("valueColumnName").type().stringType().noDefault()
                .name("minX").type().doubleType().noDefault()
                .name("maxX").type().doubleType().noDefault()
                .name("minY").type().doubleType().noDefault()
                .name("maxY").type().doubleType().noDefault()
                .name("width").type().intType().noDefault()
                .name("height").type().intType().noDefault()
                .name("projection").type().stringType().noDefault()
                .name("styleOptions").type().map().values().stringType().noDefault()
                .name("options").type().map().values().stringType().noDefault()
            .endRecord();

    public static Schema getClassSchema() {
        return schema$;
    }

    public static final class Projection {
        public static final String _3857 = "3857";
        public static final String _102100 = "102100";
        public static final String _900913 = "900913";
        public static final String EPSG_4326 = "EPSG:4326";
        public static final String PLATE_CARREE = "PLATE_CARREE";
        public static final String EPSG_900913 = "EPSG:900913";
        public static final String EPSG_102100 = "EPSG:102100";
        public static final String EPSG_3857 = "EPSG:3857";
        public static final String WEB_MERCATOR = "WEB_MERCATOR";

        private Projection() {  }
    }

    public static final class StyleOptions {
        public static final String LINE_SIZE = "line_size";
        public static final String COLOR = "color";
        public static final String BG_COLOR = "bg_color";
        public static final String TEXT_COLOR = "text_color";
        public static final String COLORMAP = "colormap";
        public static final String JET = "jet";
        public static final String ACCENT = "accent";
        public static final String AFMHOT = "afmhot";
        public static final String AUTUMN = "autumn";
        public static final String BINARY = "binary";
        public static final String BLUES = "blues";
        public static final String BONE = "bone";
        public static final String BRBG = "brbg";
        public static final String BRG = "brg";
        public static final String BUGN = "bugn";
        public static final String BUPU = "bupu";
        public static final String BWR = "bwr";
        public static final String CMRMAP = "cmrmap";
        public static final String COOL = "cool";
        public static final String COOLWARM = "coolwarm";
        public static final String COPPER = "copper";
        public static final String CUBEHELIX = "cubehelix";
        public static final String DARK2 = "dark2";
        public static final String FLAG = "flag";
        public static final String GIST_EARTH = "gist_earth";
        public static final String GIST_GRAY = "gist_gray";
        public static final String GIST_HEAT = "gist_heat";
        public static final String GIST_NCAR = "gist_ncar";
        public static final String GIST_RAINBOW = "gist_rainbow";
        public static final String GIST_STERN = "gist_stern";
        public static final String GIST_YARG = "gist_yarg";
        public static final String GNBU = "gnbu";
        public static final String GNUPLOT2 = "gnuplot2";
        public static final String GNUPLOT = "gnuplot";
        public static final String GRAY = "gray";
        public static final String GREENS = "greens";
        public static final String GREYS = "greys";
        public static final String HOT = "hot";
        public static final String HSV = "hsv";
        public static final String INFERNO = "inferno";
        public static final String MAGMA = "magma";
        public static final String NIPY_SPECTRAL = "nipy_spectral";
        public static final String OCEAN = "ocean";
        public static final String ORANGES = "oranges";
        public static final String ORRD = "orrd";
        public static final String PAIRED = "paired";
        public static final String PASTEL1 = "pastel1";
        public static final String PASTEL2 = "pastel2";
        public static final String PINK = "pink";
        public static final String PIYG = "piyg";
        public static final String PLASMA = "plasma";
        public static final String PRGN = "prgn";
        public static final String PRISM = "prism";
        public static final String PUBU = "pubu";
        public static final String PUBUGN = "pubugn";
        public static final String PUOR = "puor";
        public static final String PURD = "purd";
        public static final String PURPLES = "purples";
        public static final String RAINBOW = "rainbow";
        public static final String RDBU = "rdbu";
        public static final String RDGY = "rdgy";
        public static final String RDPU = "rdpu";
        public static final String RDYLBU = "rdylbu";
        public static final String RDYLGN = "rdylgn";
        public static final String REDS = "reds";
        public static final String SEISMIC = "seismic";
        public static final String SET1 = "set1";
        public static final String SET2 = "set2";
        public static final String SET3 = "set3";
        public static final String SPECTRAL = "spectral";
        public static final String SPRING = "spring";
        public static final String SUMMER = "summer";
        public static final String TERRAIN = "terrain";
        public static final String VIRIDIS = "viridis";
        public static final String WINTER = "winter";
        public static final String WISTIA = "wistia";
        public static final String YLGN = "ylgn";
        public static final String YLGNBU = "ylgnbu";
        public static final String YLORBR = "ylorbr";
        public static final String YLORRD = "ylorrd";

        private StyleOptions() {  }
    }

    public static final class Options {
        public static final String MIN_LEVEL = "min_level";
        public static final String MAX_LEVEL = "max_level";
        public static final String NUM_LEVELS = "num_levels";
        public static final String ADJUST_LEVELS = "adjust_levels";
        public static final String SEARCH_RADIUS = "search_radius";
        public static final String MAX_SEARCH_CELLS = "max_search_cells";
        public static final String GRIDDING_METHOD = "gridding_method";
        public static final String INV_DST_POW = "INV_DST_POW";
        public static final String MIN_CURV = "MIN_CURV";
        public static final String KRIGING = "KRIGING";
        public static final String PASS_THROUGH = "PASS_THROUGH";
        public static final String FILL_RATIO = "FILL_RATIO";
        public static final String SMOOTHING_FACTOR = "smoothing_factor";
        public static final String GRID_SIZE = "grid_size";
        public static final String ADJUST_GRID = "adjust_grid";
        public static final String ADJUST_GRID_NEIGH = "adjust_grid_neigh";
        public static final String ADJUST_GRID_SIZE = "adjust_grid_size";
        public static final String MAX_GRID_SIZE = "max_grid_size";
        public static final String MIN_GRID_SIZE = "min_grid_size";
        public static final String RENDER_OUTPUT_GRID = "render_output_grid";
        public static final String COLOR_ISOLINES = "color_isolines";
        public static final String ADD_LABELS = "add_labels";
        public static final String LABELS_FONT_SIZE = "labels_font_size";
        public static final String LABELS_FONT_FAMILY = "labels_font_family";
        public static final String LABELS_SEARCH_WINDOW = "labels_search_window";
        public static final String LABELS_INTRALEVEL_SEPARATION = "labels_intralevel_separation";
        public static final String LABELS_INTERLEVEL_SEPARATION = "labels_interlevel_separation";
        public static final String LABELS_MAX_ANGLE = "labels_max_angle";
        public static final String ISOCHRONE_CONCAVITY = "isochrone_concavity";
        public static final String ISOCHRONE_OUTPUT_TABLE = "isochrone_output_table";
        public static final String ISOCHRONE_IMAGE = "isochrone_image";

        private Options() {  }
    }

    private List<String> tableNames;
    private String xColumnName;
    private String yColumnName;
    private String valueColumnName;
    private double minX;
    private double maxX;
    private double minY;
    private double maxY;
    private int width;
    private int height;
    private String projection;
    private Map<String, String> styleOptions;
    private Map<String, String> options;

    public VisualizeImageContourRequest() {
        tableNames = new ArrayList<>();
        xColumnName = "";
        yColumnName = "";
        valueColumnName = "";
        projection = "";
        styleOptions = new LinkedHashMap<>();
        options = new LinkedHashMap<>();
    }

    public VisualizeImageContourRequest(List<String> tableNames, String xColumnName, String yColumnName, String valueColumnName, double minX, double maxX, double minY, double maxY, int width, int height, String projection, Map<String, String> styleOptions, Map<String, String> options) {
        this.tableNames = (tableNames == null) ? new ArrayList<String>() : tableNames;
        this.xColumnName = (xColumnName == null) ? "" : xColumnName;
        this.yColumnName = (yColumnName == null) ? "" : yColumnName;
        this.valueColumnName = (valueColumnName == null) ? "" : valueColumnName;
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
        this.width = width;
        this.height = height;
        this.projection = (projection == null) ? "" : projection;
        this.styleOptions = (styleOptions == null) ? new LinkedHashMap<String, String>() : styleOptions;
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
    }

    public List<String> getTableNames() {
        return tableNames;
    }

    public VisualizeImageContourRequest setTableNames(List<String> tableNames) {
        this.tableNames = (tableNames == null) ? new ArrayList<String>() : tableNames;
        return this;
    }

    public String getXColumnName() {
        return xColumnName;
    }

    public VisualizeImageContourRequest setXColumnName(String xColumnName) {
        this.xColumnName = (xColumnName == null) ? "" : xColumnName;
        return this;
    }

    public String getYColumnName() {
        return yColumnName;
    }

    public VisualizeImageContourRequest setYColumnName(String yColumnName) {
        this.yColumnName = (yColumnName == null) ? "" : yColumnName;
        return this;
    }

    public String getValueColumnName() {
        return valueColumnName;
    }

    public VisualizeImageContourRequest setValueColumnName(String valueColumnName) {
        this.valueColumnName = (valueColumnName == null) ? "" : valueColumnName;
        return this;
    }

    public double getMinX() {
        return minX;
    }

    public VisualizeImageContourRequest setMinX(double minX) {
        this.minX = minX;
        return this;
    }

    public double getMaxX() {
        return maxX;
    }

    public VisualizeImageContourRequest setMaxX(double maxX) {
        this.maxX = maxX;
        return this;
    }

    public double getMinY() {
        return minY;
    }

    public VisualizeImageContourRequest setMinY(double minY) {
        this.minY = minY;
        return this;
    }

    public double getMaxY() {
        return maxY;
    }

    public VisualizeImageContourRequest setMaxY(double maxY) {
        this.maxY = maxY;
        return this;
    }

    public int getWidth() {
        return width;
    }

    public VisualizeImageContourRequest setWidth(int width) {
        this.width = width;
        return this;
    }

    public int getHeight() {
        return height;
    }

    public VisualizeImageContourRequest setHeight(int height) {
        this.height = height;
        return this;
    }

    public String getProjection() {
        return projection;
    }

    public VisualizeImageContourRequest setProjection(String projection) {
        this.projection = (projection == null) ? "" : projection;
        return this;
    }

    public Map<String, String> getStyleOptions() {
        return styleOptions;
    }

    public VisualizeImageContourRequest setStyleOptions(Map<String, String> styleOptions) {
        this.styleOptions = (styleOptions == null) ? new LinkedHashMap<String, String>() : styleOptions;
        return this;
    }

    public Map<String, String> getOptions() {
        return options;
    }

    public VisualizeImageContourRequest setOptions(Map<String, String> options) {
        this.options = (options == null) ? new LinkedHashMap<String, String>() : options;
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
                return this.xColumnName;

            case 2:
                return this.yColumnName;

            case 3:
                return this.valueColumnName;

            case 4:
                return this.minX;

            case 5:
                return this.maxX;

            case 6:
                return this.minY;

            case 7:
                return this.maxY;

            case 8:
                return this.width;

            case 9:
                return this.height;

            case 10:
                return this.projection;

            case 11:
                return this.styleOptions;

            case 12:
                return this.options;

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
                this.xColumnName = (String)value;
                break;

            case 2:
                this.yColumnName = (String)value;
                break;

            case 3:
                this.valueColumnName = (String)value;
                break;

            case 4:
                this.minX = (Double)value;
                break;

            case 5:
                this.maxX = (Double)value;
                break;

            case 6:
                this.minY = (Double)value;
                break;

            case 7:
                this.maxY = (Double)value;
                break;

            case 8:
                this.width = (Integer)value;
                break;

            case 9:
                this.height = (Integer)value;
                break;

            case 10:
                this.projection = (String)value;
                break;

            case 11:
                this.styleOptions = (Map<String, String>)value;
                break;

            case 12:
                this.options = (Map<String, String>)value;
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

        VisualizeImageContourRequest that = (VisualizeImageContourRequest)obj;

        return ( this.tableNames.equals( that.tableNames )
                 && this.xColumnName.equals( that.xColumnName )
                 && this.yColumnName.equals( that.yColumnName )
                 && this.valueColumnName.equals( that.valueColumnName )
                 && ( (Double)this.minX ).equals( (Double)that.minX )
                 && ( (Double)this.maxX ).equals( (Double)that.maxX )
                 && ( (Double)this.minY ).equals( (Double)that.minY )
                 && ( (Double)this.maxY ).equals( (Double)that.maxY )
                 && ( this.width == that.width )
                 && ( this.height == that.height )
                 && this.projection.equals( that.projection )
                 && this.styleOptions.equals( that.styleOptions )
                 && this.options.equals( that.options ) );
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
        builder.append( gd.toString( "xColumnName" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.xColumnName ) );
        builder.append( ", " );
        builder.append( gd.toString( "yColumnName" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.yColumnName ) );
        builder.append( ", " );
        builder.append( gd.toString( "valueColumnName" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.valueColumnName ) );
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
        builder.append( gd.toString( "styleOptions" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.styleOptions ) );
        builder.append( ", " );
        builder.append( gd.toString( "options" ) );
        builder.append( ": " );
        builder.append( gd.toString( this.options ) );
        builder.append( "}" );

        return builder.toString();
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        hashCode = (31 * hashCode) + this.tableNames.hashCode();
        hashCode = (31 * hashCode) + this.xColumnName.hashCode();
        hashCode = (31 * hashCode) + this.yColumnName.hashCode();
        hashCode = (31 * hashCode) + this.valueColumnName.hashCode();
        hashCode = (31 * hashCode) + ((Double)this.minX).hashCode();
        hashCode = (31 * hashCode) + ((Double)this.maxX).hashCode();
        hashCode = (31 * hashCode) + ((Double)this.minY).hashCode();
        hashCode = (31 * hashCode) + ((Double)this.maxY).hashCode();
        hashCode = (31 * hashCode) + this.width;
        hashCode = (31 * hashCode) + this.height;
        hashCode = (31 * hashCode) + this.projection.hashCode();
        hashCode = (31 * hashCode) + this.styleOptions.hashCode();
        hashCode = (31 * hashCode) + this.options.hashCode();
        return hashCode;
    }
}
