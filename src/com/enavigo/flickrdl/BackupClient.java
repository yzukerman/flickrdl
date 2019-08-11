/**
 * 
 */
package com.enavigo.flickrdl;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.REST;
import com.flickr4java.flickr.RequestContext;
import com.flickr4java.flickr.auth.Auth;
import com.flickr4java.flickr.auth.AuthInterface;
import com.flickr4java.flickr.auth.Permission;
import com.flickr4java.flickr.collections.Collection;
import com.flickr4java.flickr.photos.Photo;
import com.flickr4java.flickr.photos.PhotoList;
import com.flickr4java.flickr.photos.PhotosInterface;
import com.flickr4java.flickr.photos.Size;
import com.flickr4java.flickr.photosets.Photoset;
import com.flickr4java.flickr.photosets.PhotosetsInterface;
import com.flickr4java.flickr.util.AuthStore;
import com.flickr4java.flickr.util.FileAuthStore;
import com.github.scribejava.core.model.OAuth1RequestToken;
import com.github.scribejava.core.model.OAuth1Token;
import java.awt.Desktop;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author yzukerma
 *
 */
public class BackupClient {

	private final String nsid;

    private final Flickr flickr;

    private AuthStore authStore;
    
    private final Logger logger = LoggerFactory.getLogger(BackupClient.class);
    
    private String downloadDirectory = "Flickrdl";
	
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
                logger.info("Creating Authstore at " + authsDir);
                this.authStore = new FileAuthStore(authsDir);
            }
	}
	
	private void doBackup() throws Exception
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
        
        // Get photo sets 
        PhotosetsInterface pi = flickr.getPhotosetsInterface();
        Iterator<Photoset> sets = pi.getList(this.nsid).getPhotosets().iterator();
        Map<String, Collection> allPhotos = new HashMap<String, Collection>();
        int photosInSet = 0;
        int photoSetPage = 1;
        int setCount = 0;
        PhotoList<Photo> photos = null;
        Photoset set = null;
        File downloadDirectory = new File(System.getProperty("user.home") + File.separatorChar + this.downloadDirectory);
     // file download thread pool
        ExecutorService pool = Executors.newFixedThreadPool(5);
        
        logger.info("Target download directory is: " + downloadDirectory.getName());
        
        if(!downloadDirectory.exists())
        {
        	logger.info("Creating directory...");
        	downloadDirectory.mkdir();
        }
        
        // create folder for each photo set, then download images into that folder
        while (sets.hasNext()) {
            set = (Photoset) sets.next();
            logger.info(set.getTitle());
            photoSetPage = 1;
            
            for(photosInSet = set.getPhotoCount(); photosInSet > 0; photosInSet -= 500)
            {
            	photos = pi.getPhotos(set.getId(), 500, photoSetPage);
            	photoSetPage++;
            }
            logger.info("Set #" + setCount + " Size: " + photos.size());
            if (setCount > 10 && setCount < 12)
            {
            	logger.info("Downloading!");
            	//downloadSet(set.getTitle(), photos, downloadDirectory, pool);            	
            }
            setCount++;
        }
        
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
	}
	
	private void authorize() throws IOException, 
                                        FlickrException, 
                                        URISyntaxException 
        {
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

	
	/**
	 * Downloads a set of images
	 * @param setName The name of the set on Flickr
	 * @param photos The photos contained within the set
	 * @param downloadDirectory The directory in which all photos will be stored
	 * @throws Exception
	 */
	private void downloadSet(String setName, 
							 PhotoList<Photo> photos, 
							 File downloadDirectory,
							 ExecutorService pool) throws Exception
	{
		logger.info("Downloading set '" + setName + "'");
		// create the directory for the set's download
		String setDirectoryName = makeSafeFilename(setName);
        File setDirectory = new File(downloadDirectory, setDirectoryName);
        setDirectory.mkdir();
        PhotosInterface photoInt = flickr.getPhotosInterface();
        
        // iterate and download the set's images
        Iterator<Photo> setIterator = photos.iterator();
        Photo p = null;
        
        
        
        while (setIterator.hasNext()) {

            p = (Photo) setIterator.next();
            pool.submit(new DownloadTask(p, setDirectory, photoInt));
            
        }
        
        
    
	}
	
	/**
	 * Cleans up the file name from offensive characters
	 * @param input The unsanitized file name requested
	 * @return the sanitized file name
	 */
	private String makeSafeFilename(String input) {
        byte[] fname = input.getBytes();
        byte[] bad = new byte[] { '\\', '/', '"' };
        byte replace = '_';
        for (int i = 0; i < fname.length; i++) {
            for (byte element : bad) {
                if (fname[i] == element) {
                    fname[i] = replace;
                }
            }
        }
        return new String(fname);
    }
	
	private static class DownloadTask implements Runnable {
		
		private Photo p;
		private File setDirectory;
		private PhotosInterface photoInt;
		private String url;
		private URL u;
		private String filename;
		private final Logger logger = LoggerFactory.getLogger(DownloadTask.class);
		private FileOutputStream fos;
		private BufferedInputStream inStream;
		
		public DownloadTask(Photo photo, File setDir, PhotosInterface pi)
		{
			this.p = photo;
			this.setDirectory = setDir;
			this.photoInt = pi;
			fos = null;
			inStream = null;
		}
		
				
		@Override 
		public void run()
		{
	        url = p.getLargeUrl();
	        try
	        {
	        // GeoData g = p.getGeoData();
		        u = new URL(url);
		        filename = u.getFile();
		        filename = filename.substring(filename.lastIndexOf("/") + 1, filename.length());
	       
		        System.out.println("Now writing " + filename + " to " + setDirectory.getCanonicalPath());
		        inStream = new BufferedInputStream(photoInt.getImageAsStream(p, Size.ORIGINAL));
		        File newFile = new File(setDirectory, filename);
		
		        fos = new FileOutputStream(newFile);
		
		        int read = 0;
		
		        while ((read = inStream.read()) != -1) {
		            fos.write(read);
		        }
	        }
	        catch (IOException ioe)
	        {
	        	logger.warn("Download failed!");
	        	logger.warn(ioe.getLocalizedMessage());
	        }
	        catch (FlickrException fe)
	        {
	        	logger.warn("Flickr Issue; Download failed.");
	        	logger.warn(fe.getLocalizedMessage());
	        }
	        finally
	        {
		        disposeResources();
	        }
		}
		
		protected void disposeResources()
		{
			try
			{
				if(fos != null)
				{
					fos.flush();
			        fos.close();
				}
				
				if (inStream != null)
				{
					inStream.close();
				}
			}
			catch (IOException ioe)
			{
				logger.warn("Exception disposing of file resources");
				logger.warn(ioe.getLocalizedMessage());
			}
	        
		}
	}
}
