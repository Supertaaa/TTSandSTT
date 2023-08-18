package com.vega.service.api.googleUtils;


import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1.*;
import com.google.cloud.speech.v1.RecognitionConfig.AudioEncoding;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import com.google.protobuf.ByteString;
import java.io.FileInputStream;

public class ARSUtils{
	

    static String jsonCredentialPath = "/opt/tuananh/Resources/gg_json.json";
    
    //private static String jsonCredentialPath = "C:/Users/Admin PC/Downloads/API_1.8_alofun/gg_json.json";
    
    private static String recordFilePath = "/alofun/promt/recordedFile.wav";
    
    
    //private static String recordFilePath = "C:/Users/Admin PC/Downloads/bbb.wav";

    public static String getAsr() throws IOException {

        FileInputStream serviceAccountStream = new FileInputStream(jsonCredentialPath);
        GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccountStream);
        SpeechSettings setting = SpeechSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                .build();
        SpeechClient speechClient = SpeechClient.create(setting);
        RecognitionConfig config =
                RecognitionConfig.newBuilder()
                        .setEncoding(AudioEncoding.LINEAR16)
                        .setSampleRateHertz(8000)
                        .setLanguageCode("vi-VN")
                        .build();
        byte[] audioData = Files.readAllBytes(Paths.get(recordFilePath));
        RecognitionAudio audio = RecognitionAudio.newBuilder().setContent(ByteString.copyFrom(audioData)).build();
        RecognizeResponse response = speechClient.recognize(config, audio);
        List<SpeechRecognitionResult> results = response.getResultsList();
        for (SpeechRecognitionResult result : results) {
            SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
            System.out.printf("Transcription: %s%n", alternative.getTranscript());
            System.out.println(result);
            return alternative.getTranscript();
        }
        return null;
    }
    
    
}