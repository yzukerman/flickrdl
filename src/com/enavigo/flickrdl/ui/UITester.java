/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.enavigo.flickrdl.ui;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.REST;
import com.flickr4java.flickr.RequestContext;
import com.flickr4java.flickr.auth.Auth;
import com.flickr4java.flickr.auth.AuthInterface;
import com.flickr4java.flickr.auth.Permission;
import com.github.scribejava.core.model.OAuth1RequestToken;
import com.github.scribejava.core.model.OAuth1Token;
import java.io.IOException;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.concurrent.Worker.State;

/**
 *
 * @author yzukerma
 */
public class UITester extends Application {
    
    private Flickr flickr;
    private Stage mainWindow;
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
       
	String nsid2 = "https://farm4.staticflickr.com/3572/buddyicons/48947735@N00.jpg?1240579772#48947735@N00";

    }

    
     @Override
    public void start(Stage primaryStage) {
        
        this.mainWindow = primaryStage;
        
        try
        {
            authorize();
        }
        catch (Exception e)
        {
            System.out.println(e.getLocalizedMessage());
        }
    }
    
    /**
     * Open a popup window to have the user authenticate and authorize the app
     * to access Flickr's API
     * @param ai The interface for authentication to use
     * @param token The token generated
     * @param url The authorization URL
     * @return The token returned by Flickr
     */
    private String getTokenKey(AuthInterface ai, 
            OAuth1RequestToken token, 
            String url)
    {
        String tokenKey = null;
        // Pop browser window at URL
        StackPane stackPane = new StackPane();
        Scene scene = new Scene(stackPane, 800, 600);
        
        // Web view for authorization on Flickr
        WebView browser = new WebView();
        WebEngine webEngine = browser.getEngine();
        //webEngine.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.77 Safari/537.36");
        webEngine.load(url);
        stackPane.getChildren().add(browser);
        
        webEngine.getLoadWorker().stateProperty().addListener
        (
              new ChangeListener<State>() 
              {
                    
              @Override 
              public void changed(ObservableValue ov, State oldState, State newState) {

                  if (newState == Worker.State.SUCCEEDED) {
                      System.out.println(webEngine.getLocation().toString());
                  }
              }
              });
        
        
        mainWindow.setScene(scene);
        
        mainWindow.show();        
        
        
        
        // read token key from screen
        return tokenKey;
    }
    
    private void authorize() throws IOException, FlickrException 
    {
        
        String apiKey = "d5cb7c4bcaceed5a2c69ea55bcf5a597";
        String sharedSecret = "6d6edccc4b38776a";
        
        flickr = new Flickr(apiKey, sharedSecret, new REST());
        AuthInterface authInterface = flickr.getAuthInterface();
        OAuth1RequestToken requestToken = authInterface.getRequestToken();

        String url = authInterface.getAuthorizationUrl(requestToken, Permission.READ);
//        System.out.println("Follow this URL to authorise yourself on Flickr");
//        System.out.println(url);
//        System.out.println("Paste in the token it gives you:");
//        System.out.print(">>");

//        Scanner s = new Scanner(System.in);
//        String tokenKey = s.nextLine();
//        s.close();
        String tokenKey = getTokenKey(authInterface, requestToken, url);

//        OAuth1Token accessToken = authInterface.getAccessToken(requestToken, tokenKey);
//
//        Auth auth = authInterface.checkToken(accessToken);
//        RequestContext.getRequestContext().setAuth(auth);
//        //this.authStore.store(auth);
//        System.out.println("Thanks.  You probably will not have to do this every time.  Now starting backup.");
        
    }
}
