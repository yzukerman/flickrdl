/**
 * 
 */
package com.enavigo.flickrdl;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Scanner;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.REST;
import com.flickr4java.flickr.RequestContext;
import com.flickr4java.flickr.auth.Auth;
import com.flickr4java.flickr.auth.AuthInterface;
import com.flickr4java.flickr.auth.Permission;
import com.flickr4java.flickr.collections.Collection;
import com.flickr4java.flickr.photosets.Photoset;
import com.flickr4java.flickr.photosets.PhotosetsInterface;
import com.flickr4java.flickr.test.*;
import com.flickr4java.flickr.util.AuthStore;
import com.flickr4java.flickr.util.FileAuthStore;
import com.github.scribejava.core.model.OAuth1RequestToken;
import com.github.scribejava.core.model.OAuth1Token;

/**
 * @author yzukerma
 *
 */
public class BackupClient {

	private final String nsid;

    private final Flickr flickr;

    private AuthStore authStore;
	
	/**
	 * @param args
	 * @throws FlickrException 
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		 String apiKey = "d5cb7c4bcaceed5a2c69ea55bcf5a597";
		 String sharedSecret = "6d6edccc4b38776a";
		 String nsid2 = "https://farm4.staticflickr.com/3572/buddyicons/48947735@N00.jpg?1240579772#48947735@N00";
		 BackupClient bc = new BackupClient(apiKey, "48947735@N00", sharedSecret, new File("C:\\Development\\authstore\\"));
		 bc.doBackup();
	}
	
	public BackupClient(String apiKey, String nsid, String sharedSecret, File authsDir) throws FlickrException {
        flickr = new Flickr(apiKey, sharedSecret, new REST());
        this.nsid = nsid;

        if (authsDir != null) {
            this.authStore = new FileAuthStore(authsDir);
        }
	}
	
	public void doBackup() throws Exception
	{
		RequestContext rc = RequestContext.getRequestContext();

        if (this.authStore != null) {
            Auth auth = this.authStore.retrieve(this.nsid);
            if (auth == null) {
                authorize();
            } else {
                rc.setAuth(auth);
            }
        }
        
        
        PhotosetsInterface pi = flickr.getPhotosetsInterface();
        Iterator<Photoset> sets = pi.getList(this.nsid).getPhotosets().iterator();
        while (sets.hasNext()) {
            Photoset set = (Photoset) sets.next();
            System.out.println(set.getTitle());
        }
	}
	
	private void authorize() throws IOException, FlickrException {
        AuthInterface authInterface = flickr.getAuthInterface();
        OAuth1RequestToken requestToken = authInterface.getRequestToken();

        String url = authInterface.getAuthorizationUrl(requestToken, Permission.READ);
        System.out.println("Follow this URL to authorise yourself on Flickr");
        System.out.println(url);
        System.out.println("Paste in the token it gives you:");
        System.out.print(">>");

        Scanner s = new Scanner(System.in);
        String tokenKey = s.nextLine();
        s.close();

        OAuth1Token accessToken = authInterface.getAccessToken(requestToken, tokenKey);

        Auth auth = authInterface.checkToken(accessToken);
        RequestContext.getRequestContext().setAuth(auth);
        this.authStore.store(auth);
        System.out.println("Thanks.  You probably will not have to do this every time.  Now starting backup.");
	}

}
