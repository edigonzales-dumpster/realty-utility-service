package ch.so.agi;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.inject.Inject;
//import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.ByteOrderValues;
import org.locationtech.jts.io.WKBWriter;

import io.agroal.api.AgroalDataSource;

@Path("/")
public class MainController {
    private static final Logger LOGGER = Logger.getLogger(MainController.class);

    private static final String TABLE_DM01VCH24LV95DLIEGENSCHAFTEN_LSNACHFUEHRUNG = "dm01vch24lv95dliegenschaften_lsnachfuehrung";
    private static final String TABLE_DM01VCH24LV95DLIEGENSCHAFTEN_GRUNDSTUECK = "dm01vch24lv95dliegenschaften_grundstueck";
    private static final String TABLE_DM01VCH24LV95DLIEGENSCHAFTEN_LIEGENSCHAFT = "dm01vch24lv95dliegenschaften_liegenschaft";
    private static final String TABLE_DM01VCH24LV95DLIEGENSCHAFTEN_SELBSTRECHT = "dm01vch24lv95dliegenschaften_selbstrecht";
    private static final String TABLE_DM01VCH24LV95DLIEGENSCHAFTEN_BERGWERK = "dm01vch24lv95dliegenschaften_bergwerk";
    private static final String TABLE_DM01VCH24LV95DLIEGENSCHAFTEN_PROJGRUNDSTUECK = "dm01vch24lv95dliegenschaften_projgrundstueck";
    private static final String TABLE_DM01VCH24LV95DLIEGENSCHAFTEN_PROJLIEGENSCHAFT = "dm01vch24lv95dliegenschaften_projliegenschaft";
    private static final String TABLE_DM01VCH24LV95DLIEGENSCHAFTEN_PROJSELBSTRECHT = "dm01vch24lv95dliegenschaften_projselbstrecht";
    private static final String TABLE_DM01VCH24LV95DLIEGENSCHAFTEN_PROJBERGWERK = "dm01vch24lv95dliegenschaften_projbergwerk";

    @ConfigProperty(name = "app.dbschema") 
    private String dbschema;

    @Inject
    AgroalDataSource defaultDataSource;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/getrealty")    
    public Response hello(@QueryParam("XY") String xy, @QueryParam("GNSS") String gnss) {
        if(xy==null && gnss==null) {
            throw new IllegalArgumentException("parameter XY or GNSS required");
        } else if(xy!=null && gnss!=null) {
            throw new IllegalArgumentException("only one of parameters XY or GNSS is allowed");
        }

        Coordinate coord = null;
        int srid = 2056;
        double scale = 1000.0;
        if(xy!=null) {
            coord = parseCoord(xy);
        } else {
            coord = parseCoord(gnss);
            srid = 4326;
            scale = 100000.0;
        }

        WKBWriter geomEncoder = new WKBWriter(2, ByteOrderValues.BIG_ENDIAN, true);
        PrecisionModel precisionModel = new PrecisionModel(scale);
        GeometryFactory geomFact = new GeometryFactory(precisionModel, srid);
        byte geom[] = geomEncoder.write(geomFact.createPoint(coord));
        String wkt = geomFact.createPoint(coord).toText(); 
                
        String sql = "WITH realty AS\n" + 
                "(\n" + 
                "    SELECT grundstueck.t_id, grundstueck.nummer, grundstueck.nbident, grundstueck.egris_egrid, grundstueck.art, nf.gueltigkeit, TO_CHAR(nf.gueltigereintrag, 'yyyy-mm-dd') AS gueltigereintrag, geom.geometrie\n" + 
                "    FROM "+dbschema+"."+TABLE_DM01VCH24LV95DLIEGENSCHAFTEN_GRUNDSTUECK+" AS grundstueck\n" + 
                "    LEFT JOIN \n" + 
                "    (\n" + 
                "        SELECT liegenschaft_von AS von, geometrie FROM "+dbschema+"."+TABLE_DM01VCH24LV95DLIEGENSCHAFTEN_LIEGENSCHAFT+" \n" + 
                "        UNION ALL \n" + 
                "        SELECT selbstrecht_von AS von, geometrie FROM "+dbschema+"."+TABLE_DM01VCH24LV95DLIEGENSCHAFTEN_SELBSTRECHT+"\n" + 
                "        UNION ALL \n" + 
                "        SELECT bergwerk_von AS von, geometrie FROM "+dbschema+"."+TABLE_DM01VCH24LV95DLIEGENSCHAFTEN_BERGWERK+" \n" + 
                "    ) AS geom ON grundstueck.t_id = geom.von\n" + 
                "    LEFT JOIN "+dbschema+"."+TABLE_DM01VCH24LV95DLIEGENSCHAFTEN_LSNACHFUEHRUNG+" AS nf" + 
                "    ON grundstueck.entstehung = nf.t_id WHERE ST_DWithin(ST_Transform(ST_PointFromText('"+wkt+"', "+srid+"),2056),geom.geometrie,1.0)\n" +                 
                "    UNION ALL\n" + 
                "    SELECT grundstueck.t_id, grundstueck.nummer, grundstueck.nbident, grundstueck.egris_egrid, grundstueck.art, nf.gueltigkeit, TO_CHAR(nf.gueltigereintrag, 'yyyy-mm-dd') AS gueltigereintrag, geom.geometrie\n" + 
                "    FROM "+dbschema+"."+TABLE_DM01VCH24LV95DLIEGENSCHAFTEN_PROJGRUNDSTUECK+" AS grundstueck\n" + 
                "    LEFT JOIN \n" + 
                "    (\n" + 
                "        SELECT projliegenschaft_von AS von, geometrie FROM "+dbschema+"."+TABLE_DM01VCH24LV95DLIEGENSCHAFTEN_PROJLIEGENSCHAFT+" \n" + 
                "        UNION ALL \n" + 
                "        SELECT projselbstrecht_von AS von, geometrie FROM "+dbschema+"."+TABLE_DM01VCH24LV95DLIEGENSCHAFTEN_PROJSELBSTRECHT+"\n" + 
                "        UNION ALL \n" + 
                "        SELECT projbergwerk_von AS von, geometrie FROM "+dbschema+"."+TABLE_DM01VCH24LV95DLIEGENSCHAFTEN_PROJBERGWERK+" \n" + 
                "    ) AS geom ON grundstueck.t_id = geom.von\n" + 
                "    LEFT JOIN "+dbschema+"."+TABLE_DM01VCH24LV95DLIEGENSCHAFTEN_LSNACHFUEHRUNG+" AS nf\n" + 
                "    ON grundstueck.entstehung = nf.t_id WHERE ST_DWithin(ST_Transform(ST_PointFromText('"+wkt+"', "+srid+"),2056),geom.geometrie,1.0)\n" + 
                ")\n" + 
                "SELECT json_build_object(\n" + 
                "    'type', 'FeatureCollection',\n" + 
                "    'crs',  json_build_object(\n" + 
                "        'type',      'name', \n" + 
                "        'properties', json_build_object(\n" + 
                "            'name', 'EPSG:"+srid+"'  \n" + 
                "        )\n" + 
                "    ), \n" + 
                "    'features', json_agg(\n" + 
                "        json_build_object(\n" + 
                "            'type', 'Feature',\n" + 
                "            'id', t_id, \n" + 
                "            'geometry',   ST_AsGeoJSON(geometrie)::json,\n" + 
                "            'properties', json_build_object(\n" + 
                "                'egrid', egris_egrid,\n" + 
                "                'number', nummer,\n" + 
                "                'identDN', nbident,\n" + 
                "                'type', art,\n" + 
                "                'validityType', gueltigkeit,\n" + 
                "                'stateOf', gueltigereintrag\n" + 
                "            )\n" + 
                "        )\n" + 
                "    )\n" + 
                ")\n" + 
                "FROM realty;";
        
        LOGGER.debug(sql);
        
        String result = null;
        try (Connection con = defaultDataSource.getConnection(); 
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result = rs.getString(1);
                LOGGER.debug(result);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    
        if (result == null) {
            return Response.noContent().build();
        }
        return Response.ok(result).build();
    }
    
    private Coordinate parseCoord(String xy) {
        int sepPos = xy.indexOf(',');
        double x = Double.parseDouble(xy.substring(0, sepPos));
        double y = Double.parseDouble(xy.substring(sepPos+1));
        Coordinate coord = new Coordinate(x,y);
        return coord;
    }    
}