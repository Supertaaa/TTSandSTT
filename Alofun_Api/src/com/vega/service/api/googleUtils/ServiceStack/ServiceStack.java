package com.vega.service.api.googleUtils.ServiceStack;

import java.util.HashMap;
import java.util.List;

import com.vega.service.api.googleUtils.TTSUtil;
import com.vega.service.api.response.Result;


import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;



import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.model.*;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;

public class ServiceStack{
	
	private static String apiKey = "AIzaSyC4ypmGHSf85EetJDbf38mbeou4lSAm8bY";
	
	public static Result getVoice(HashMap<String, String> params) {
    	
    	Result result = new Result();
    	String text = params.get("text");
    	System.out.println(text);
    
    	try {
    		Boolean resultTTS = TTSUtil.getTTS(text);
    		
    		if (resultTTS) {
    			result.setErrorCode("0");
    			result.setName("TTS");
    		}
    		else {
    			result.setErrorCode("-1");
    		}
    		return result;
    	} catch (Exception e) {
    		// TODO Auto-generated catch block
    		e.printStackTrace();
    	}

    	return new Result();
    }
	
	
	public static List<String> getRoute(String ori, String des) throws IOException, URISyntaxException {
        // Thay thế YOUR_API_KEY bằng khóa API của bạn
		
		System.out.println(ori);
        System.out.println(des);
        List<String> steps = new ArrayList<>();
        String apiKey = "AIzaSyC4ypmGHSf85EetJDbf38mbeou4lSAm8bY";
        // Khởi tạo đối tượng GeoApiContext với khóa API của bạn
        GeoApiContext context = new GeoApiContext.Builder()
                .apiKey(apiKey)
                .build();
        // Điểm xuất phát và điểm đến (ví dụ: San Francisco và Los Angeles)
        List<String> originCoor = getCoordinates(apiKey, ori);
        List<String> destinationCoor = getCoordinates(apiKey, des);
        
        System.out.println(originCoor);
        System.out.println(destinationCoor);


        LatLng origin = new LatLng(Double.parseDouble(originCoor.get(0)), Double.parseDouble(originCoor.get(1)));
        LatLng destination = new LatLng(Double.parseDouble(destinationCoor.get(0)), Double.parseDouble(destinationCoor.get(1)));
        try {
            // Gửi yêu cầu đến Directions API
            DirectionsResult result = DirectionsApi.newRequest(context)
                    .mode(TravelMode.DRIVING) // Chọn phương tiện di chuyển (DRIVING, WALKING, BICYCLING, TRANSIT)
                    .origin(origin)
                    .destination(destination)
                    .language("vi")
                    .await();
            // Lấy thông tin chỉ đường từ kết quả
            DirectionsLeg routeText = result.routes[0].legs[0];
            for (int i = 0; i < routeText.steps.length; i++) {
                String htmlString = routeText.steps[i].htmlInstructions;
                // Create a custom Safelist to preserve specific tags and attributes
                Safelist safelist = Safelist.relaxed().addTags("b", "i", "a", "div").preserveRelativeLinks(true);
                // Parse the input HTML string into a Jsoup document
                Document document = Jsoup.parse(htmlString);
                // Clean HTML using the custom Safelist
                Cleaner cleaner = new Cleaner(safelist);
                Document cleanDocument = cleaner.clean(document);
                // Get the cleaned text without HTML tags
                String cleanText = cleanDocument.body().text()
                        .replace("Ng.", "ngõ") // Strip HTML tags
                        .replace("P.", "phường")
                        .replace("Ngh.", "ngách")// Add newline after periods
                        .replace("Đ.", "đường");
                steps.add(cleanText);
                //System.out.println("Cleaned Text: " + cleanText);
                //System.out.println(Jsoup.clean(routeText.steps[i].htmlInstructions, new Safelist().removeTags()));
            }
            return steps;
            //System.out.println("Chỉ đường: " + routeText);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<String> getCoordinates(String apiKey, String address) throws IOException, URISyntaxException {
        String baseUri = "https://maps.googleapis.com/maps/api/geocode/json";
        URI uri = new URIBuilder(baseUri)
                .addParameter("address", address)
                .addParameter("key", apiKey)
                .build();

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(uri);
            String responseBody = EntityUtils.toString(httpClient.execute(httpGet).getEntity());
            String[] loation = responseBody.substring(responseBody.indexOf("location") + 14, responseBody.indexOf("location") + 84).replaceAll("\\s+","").split(",");
            List<String> coordinates = new ArrayList<>();
            for(String s : loation){
                coordinates.add(s.split(":")[1]);
            }
            return coordinates;
        }
    }
    
    
    
    
	
	
	
	
	
	
}