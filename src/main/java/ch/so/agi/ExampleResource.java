package ch.so.agi;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.jboss.logging.Logger;

import io.agroal.api.AgroalDataSource;

@Path("/hello")
public class ExampleResource {
    private static final Logger LOGGER = Logger.getLogger(ExampleResource.class);

    @Inject
    AgroalDataSource defaultDataSource;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        
        LOGGER.info("fubar");
        
        
        try (Connection con = defaultDataSource.getConnection(); 
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT t_id, nummer, egris_egrid FROM live.dm01vch24lv95dliegenschaften_grundstueck LIMIT 10")) {
     
            while (rs.next()) {
                String t_id = rs.getString(1);
//                String s = rs.getString("b");
//                float f = rs.getFloat("c");
                
                LOGGER.info(t_id);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    
        return "hello";
    }
}