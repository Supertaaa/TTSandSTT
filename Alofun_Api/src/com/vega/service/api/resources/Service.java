package com.vega.service.api.resources;

import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.vega.service.api.common.Decrypto;
import com.vega.service.api.googleUtils.*;
import com.vega.service.api.object.City;
import com.vega.service.api.response.Result;
import java.text.Normalizer;
import java.util.regex.Pattern;

import com.vega.service.api.RestfulStack;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;

import javax.ws.rs.core.UriInfo;
import org.apache.log4j.Logger;

@Path("/api")
public class Service {

    private static Logger logger = Logger.getLogger(Service.class);
    public static RestfulStack stack = new RestfulStack();

    public static RestfulStack getStack() {
        return stack;
    }

    public static void setStack(RestfulStack stack) {
        Service.stack = stack;
    }
    
    public int getIndex(String string, char subname){
	    for (int i = 0; i < string.length(); i++){
	        if(string.charAt(i) == subname){
	            return i;
	        }
	    }
	    return -1;
	}
    
    
    @POST
    @Path("/{param}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes("application/x-www-form-urlencoded")
    public Object post(@PathParam("param") String function, MultivaluedMap<String, String> formParams) {
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put("function_name", function);
        Set<String> key = formParams.keySet();
        Iterator<String> iterator = key.iterator();
        while (iterator.hasNext()) {
            String name = iterator.next();
            String value = formParams.getFirst(name);
            properties.put(name, value);
            logger.debug(name + " - " + value);
        }
        return stack.process(properties);
    }

    @GET
    @Path("/{param}")
    @Produces({ "application/json" })
    public Object get(@PathParam("param") final String function, @Context final UriInfo uriInfo, @Context final HttpServletRequest request) {
        final HashMap<String, String> properties = new HashMap<String, String>();
        String listParam = null;
        
        if (request.getQueryString() != null) {
        
	        try {
	        	
	            final String EncryptedQueryString = request.getQueryString().replace('*', '/');
	            listParam = Decrypto.decrypt(EncryptedQueryString, "123456789kjhgfds");
	            System.out.println(listParam);
	        }
	        catch (Exception e) {
	            e.printStackTrace();
	        }
	        
	        if(getIndex(listParam, '&') != -1) {
	        	final String[] listParams = listParam.split("&"); 
	        	
	        	for (int i = 0; i < listParams.length; i++) {
	 	           
	                final String[] element = listParams[i].split("=");
	                properties.put(element[0], element[1]);
	                System.out.println(element[0] + "  " + element[1]);
		            
		        }
	        }
	        else{
	        	final String[] element = listParam.split("=");
	        	properties.put(element[0], element[1]);
                System.out.println(element[0] + "  " + element[1]);
	        }
        }
        properties.put("function_name", function);
        return stack.process(properties);
    }
    
    
//    @GET
//    @Path("/testARS")
//    @Produces({ "application/json" })
//    public List<String> testARS() throws IOException, Exception, SQLException {
//    	System.out.println("NGU");
//    	com.vega.service.api.googleUtils.db db = new com.vega.service.api.googleUtils.db();
//    	return db.getActionService("tao muon xem thoi tiet ha noi ngay 10 thang 12");
//    }
//    
//    
    
    
    
//    @GET
//    @Path("/testDB")
//    @Produces(MediaType.APPLICATION_JSON)
//    public City getCityByName() throws SQLException, Exception{
//    	
//    	String DB_URL = "jdbc:mysql://192.168.158.208/alofun";
//    	String input = "Hồ Chí Minh";
//    	String USER = "root";
//    	String PASS = "vega@123";
//    	Connection conn; 
//    	
//		Class.forName("com.mysql.jdbc.Driver");
//		conn = DriverManager.getConnection(DB_URL, USER, PASS);
//		String sqlQuery = "SELECT * FROM `city` WHERE `name` = '" + removeAccent(input.toLowerCase()) + "'";
//		//String sqlQuery = "SELECT * FROM `city`";
//		System.out.println(sqlQuery);
//		City city = new City();
//		
//		
//		Statement stmt = conn.createStatement();
//        ResultSet rs = stmt.executeQuery(sqlQuery);
//        
////        System.out.println(rs.getInt(1));
////    	System.out.println(rs.getString(2));
////    	System.out.println(rs.getString(3));
////    	System.out.println(rs.getString(4));
//        
//        while (rs.next()) {
//        	System.out.println(rs.getInt(1));
//        	System.out.println(rs.getString(2));
//        	System.out.println(rs.getString(3));
//        	System.out.println(rs.getString(4));
//        	
//        	city.setCode(rs.getInt("code"));
//        	city.setName(rs.getString("name"));
//        	city.setLat(rs.getString("lat"));
//        	city.setLon(rs.getString("lon"));
//        	
//        }
//        conn.close();
//		 
//		return city;
//	}
//    
//    public static String removeAccent(String s) { 
//    	String temp = Normalizer.normalize(s, Normalizer.Form.NFD); Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+"); temp = pattern.matcher(temp).replaceAll(""); 
//    	return temp.replaceAll("đ", "d"); 
//    }
    
  
    
    
    
}
