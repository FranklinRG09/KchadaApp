package com.kchada.KchadaApp.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class FirebaseConfig {

    @Value("${kchada.firebase.project-id}")
    private String projectId;

    @Value("${kchada.firebase.credentials-path}")
    private String credentialsPath;

    @Bean(destroyMethod = "delete")
    public FirebaseApp firebaseApp() throws IOException {

        try (InputStream stream = Files.newInputStream(
                Path.of(credentialsPath))) {

            GoogleCredentials credentials =
                    GoogleCredentials.fromStream(stream);

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .setProjectId(projectId)
                    .build();

            return FirebaseApp.initializeApp(options);
        }
    }

    @Bean(destroyMethod = "close")
    public Firestore firestore(FirebaseApp firebaseApp) {

        return FirestoreClient.getFirestore(firebaseApp);
    }
}