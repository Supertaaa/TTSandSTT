package com.vega.service.api.googleUtils;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.texttospeech.v1.*;
import com.google.protobuf.ByteString;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class TTSUtil{
	
	
    private static String voicePath = "/alofun/promt/";
    
    
    //private static String voicePath = "C:/Users/Admin PC/Downloads/";

    private static String jsonCredentialPath = "/opt/tuananh/Resources/gg_json.json";
    
    //private static String jsonCredentialPath = "C:/Users/Admin PC/Downloads/API_1.8_alofun/gg_json.json";
    
  
    
    public static Boolean getTTS(String text)throws Exception{

        FileInputStream serviceAccountStream = new FileInputStream(jsonCredentialPath);
        GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccountStream);
        TextToSpeechSettings setting = TextToSpeechSettings.newBuilder().setCredentialsProvider(FixedCredentialsProvider.create(credentials)).build();
        try (TextToSpeechClient textToSpeechClient = TextToSpeechClient.create(setting)) {
            SynthesisInput input = SynthesisInput.newBuilder().setText(text).build();
            VoiceSelectionParams voice =
                    VoiceSelectionParams.newBuilder()
                            .setLanguageCode("vi-VN")
                            .setSsmlGender(SsmlVoiceGender.FEMALE)
                            .build();
            AudioConfig audioConfig =
                    AudioConfig.newBuilder().setAudioEncoding(AudioEncoding.LINEAR16).build();
            SynthesizeSpeechResponse response =
                    textToSpeechClient.synthesizeSpeech(input, voice, audioConfig);
            ByteString audioContents = response.getAudioContent();
            String filePath = voicePath + "TTS.wav";
            try (OutputStream out = new FileOutputStream(filePath)) {
                out.write(audioContents.toByteArray());
                
                System.out.println(filePath);
                return true;
            }
        }
        catch (IOException e){
            return false;
        }
    }
	
	
	
	
	
	
	
}



