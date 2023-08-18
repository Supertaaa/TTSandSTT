package com.vega.service.api.googleUtils;

import java.sql.*;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.vega.service.api.object.City;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class db{
	
	private String DB_URL = "jdbc:mysql://192.168.158.208/alofun";
	
	private String USER = "root";
	private  String PASS = "vega@123";
	private Connection conn;
	
	
	public Boolean insertServey(String msisdn, int isEnjoy, int enjoyLevel) throws ClassNotFoundException, SQLException {
		
		Class.forName("com.mysql.jdbc.Driver");
		conn = DriverManager.getConnection(DB_URL, USER, PASS);
		String sqlInsert = "INSERT INTO servey (msisdn,isEnjoy, enjoyLevel) VALUES('" + msisdn + "'," + Integer.toString(isEnjoy) + "," +  Integer.toString(enjoyLevel) + ")";
		System.out.println(sqlInsert);
		Statement stmt = conn.createStatement();
		
		int rowEffect = stmt.executeUpdate(sqlInsert);
		
		if(rowEffect > 0) {
			return true;
		}

		return false;
	}
	
	public List<String> getLicencePlateByProvince(String province) throws SQLException, Exception{
		Class.forName("com.mysql.jdbc.Driver");
		conn = DriverManager.getConnection(DB_URL, USER, PASS);
		String sqlQuery = "SELECT * FROM `province_infor` WHERE `province` = '" + removeAccent(province.toLowerCase()) + "'";
		System.out.println(sqlQuery);
		City city = new City();
		
		List<String> result =  new ArrayList<>();
		
		result.add(province);		
		Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sqlQuery);
        
        while (rs.next()) {

        	result.add(rs.getString("licence_plate"));
        	
        	
        }
        conn.close();
		 
		return result;
	}
	
	public String getProvinceByLicencePlate(String licence_plate) throws SQLException, Exception {
		
		Class.forName("com.mysql.jdbc.Driver");
		conn = DriverManager.getConnection(DB_URL, USER, PASS);
		String sqlQuery = "SELECT * FROM `province_infor` WHERE `licence_plate` = '" + licence_plate + "'";
		
		Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sqlQuery);
        
        if (rs.next()) {
        	return rs.getString("real_province_name");
        }
        
        return "";
		
		
	}
	
	
	public City getCityByName(String cityName) throws SQLException, Exception{
		Class.forName("com.mysql.jdbc.Driver");
		conn = DriverManager.getConnection(DB_URL, USER, PASS);
		String sqlQuery = "SELECT * FROM `city` WHERE `name` = '" + removeAccent(cityName.toLowerCase()) + "'";
		//System.out.println(sqlQuery);
		City city = new City();
		
		
		Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sqlQuery);
        
        while (rs.next()) {
//        	System.out.println(rs.getInt("code"));
//        	System.out.println(rs.getString("name"));
//        	System.out.println(rs.getString("lat"));
//        	System.out.println(rs.getString("lon"));
        	
        	city.setCode(rs.getInt("code"));
        	city.setName(rs.getString("actual_name"));
        	city.setLat(rs.getString("lat"));
        	city.setLon(rs.getString("lon"));
        	
        }
        conn.close();
		 
		return city;
	}
	
	
	public String getMusicPathByName(String musicName) throws SQLException, Exception{
		Class.forName("com.mysql.jdbc.Driver");
		conn = DriverManager.getConnection(DB_URL, USER, PASS);
		String sqlQuery = "SELECT * FROM `music_by_genre` WHERE `music_genre` = '" + removeAccent(musicName.toLowerCase()) + "'";
		//String sqlQuery = "SELECT * FROM `music_by_genre` WHERE `music_genre` = 'âu mỹ'";
		System.out.println(sqlQuery);
		String result = "";
		Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sqlQuery);
        
        //System.out.println(rs.next());
        
        
        while (rs.next()) {
//        	System.out.println(rs.getInt("code"));
//        	System.out.println(rs.getString("name"));
//        	System.out.println(rs.getString("lat"));
//        	System.out.println(rs.getString("lon"));
        	System.out.println(result);
        	result = rs.getString("path");
        	System.out.println(result);
        	
        }
        conn.close();
		 
		return result;
	}
	
	
	public List<String> getActionService(String question) throws ClassNotFoundException, SQLException {
		Class.forName("com.mysql.jdbc.Driver");
		conn = DriverManager.getConnection(DB_URL, USER, PASS);
		
		//String sqlQuery = "SELECT action FROM ngu AS a WHERE a.priority = (SELECT MAX( priority ) FROM ( SELECT * FROM ngu WHERE LOCATE( question_key,'" + question + "' ) > 0 AND LOCATE( question_case, '" + question + "' ) > 0 ) AS b ) AND LOCATE( question_key,'" + question + "' ) > 0 AND LOCATE( question_case, '" + question + "' ) > 0;" ;
	
		String sqlQuery = "SELECT action FROM question_dictionary AS a WHERE a.priority = (SELECT MAX( priority ) FROM ( SELECT * FROM question_dictionary WHERE LOCATE( question_key,'" + removeAccent(question.toLowerCase()) + "' ) > 0 AND LOCATE( question_case, '" + removeAccent(question.toLowerCase()) + "' ) > 0 ) AS b ) AND LOCATE( question_key,'" + removeAccent(question.toLowerCase()) + "' ) > 0 AND LOCATE( question_case, '" + removeAccent(question.toLowerCase()) + "' ) > 0;" ;
		System.out.println(sqlQuery);
		List<String> result = new ArrayList<>();
		Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sqlQuery);
        //System.out.println(rs.next());
        
        while (rs.next()) {

        	System.out.println(result);
        	result.add(rs.getString("action"));
        	System.out.println(result);
        }
        conn.close();
        
	
		return result;
		
		
	}
	
	
	public City getProvince(String question) throws ClassNotFoundException, SQLException {
		Class.forName("com.mysql.jdbc.Driver");
		conn = DriverManager.getConnection(DB_URL, USER, PASS);
		
		City city = new City();
	
		String sqlQuery = "SELECT * FROM province_infor WHERE LOCATE( province,'" + removeAccent(question.toLowerCase()) + "' ) > 0;" ;
		System.out.println(sqlQuery);
		List<String> result = new ArrayList<>();
		Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sqlQuery);
        //System.out.println(rs.next());
        if(rs.next()) {
        	city.setCode(rs.getInt("id"));
        	city.setName(rs.getString("real_province_name"));
        	city.setLat(rs.getString("lat"));
        	city.setLon(rs.getString("lon"));
        }
        
        conn.close();
		 
		return city;
		
		
	}
	
	
	public static String removeAccent(String s) { 
    	String temp = Normalizer.normalize(s, Normalizer.Form.NFD); Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+"); temp = pattern.matcher(temp).replaceAll(""); 
    	return temp.replaceAll("đ", "d"); 
    }
	
	public static int getIndex(String[] part, String subname){
	    for (int i = 0; i < part.length; i++){
	        if(part[i].equals(subname)){
	            return i;
	        }
	    }
	    return -1;
	}
	
	
	
	
}