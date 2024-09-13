package com.newtifry.pro3.urlimageviewhelper;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageView;

import org.apache.http.NameValuePair;

import com.newtifry.pro3.CommonUtilities;
import com.newtifry.pro3.Preferences;
import com.newtifry.pro3.database.NewtifryMessage2;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;

@SuppressWarnings("deprecation")
public final class UrlImageViewHelper {
    static Resources mResources;
    static DisplayMetrics mMetrics;

    private static boolean mUseBitmapScaling = false;
    
    public static final int CACHE_DURATION_INFINITE = Integer.MAX_VALUE;
    public static final int CACHE_DURATION_ONE_DAY = 1000 * 60 * 60 * 24;
    public static final int CACHE_DURATION_TWO_DAYS = CACHE_DURATION_ONE_DAY * 2;
    public static final int CACHE_DURATION_THREE_DAYS = CACHE_DURATION_ONE_DAY * 3;
    public static final int CACHE_DURATION_FOUR_DAYS = CACHE_DURATION_ONE_DAY * 4;
    public static final int CACHE_DURATION_FIVE_DAYS = CACHE_DURATION_ONE_DAY * 5;
    public static final int CACHE_DURATION_SIX_DAYS = CACHE_DURATION_ONE_DAY * 6;
    public static final int CACHE_DURATION_ONE_WEEK = CACHE_DURATION_ONE_DAY * 7;
    private static int timeout = 5000;

    static void clog(String format, Object... args) {
        String log;
        if (args.length == 0)
            log = format;
        else
            log = String.format(format, args);
        if (Constants.LOG_ENABLED)
            Log.i(Constants.LOGTAG, log);
    }

    public static int copyStream(final InputStream input, final OutputStream output) throws IOException {
        final byte[] stuff = new byte[8192];
        int read;
        int total = 0;
        while ((read = input.read(stuff)) != -1)
        {
            output.write(stuff, 0, read);
            total += read;
        }
        return total;
    }

    private static void prepareResources(final Context context) {
        if (mMetrics != null) {
            return;
        }
        mMetrics = new DisplayMetrics();
        //final Activity act = (Activity)context;
        //act.getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
        ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE))
        .getDefaultDisplay().getMetrics(mMetrics);
        final AssetManager mgr = context.getAssets();
        mResources = new Resources(mgr, mMetrics, context.getResources().getConfiguration());
    }

    /**
     * Bitmap scaling will use smart/sane values to limit the maximum
     * dimension of the bitmap during decode. This will prevent any dimension of the
     * bitmap from being larger than the dimensions of the device itself.
     * Doing this will conserve memory.
     * @param useBitmapScaling Toggle for smart resizing.
     */
    public static void setUseBitmapScaling(boolean useBitmapScaling) {
        mUseBitmapScaling = useBitmapScaling;
    }
    /**
     * Bitmap scaling will use smart/sane values to limit the maximum
     * dimension of the bitmap during decode. This will prevent any dimension of the
     * bitmap from being larger than the dimensions of the device itself.
     * Doing this will conserve memory.
     */
    public static boolean getUseBitmapScaling() {
        return mUseBitmapScaling;
    }

	private static Bitmap loadBitmapFromStream(final Context context, final String url, final String filename, final int targetWidth, final int targetHeight) {
        prepareResources(context);

        InputStream stream = null;
        clog("Decoding: " + url + " " + filename);
        try {
            BitmapFactory.Options o = null;
            if (mUseBitmapScaling) {
                o = new BitmapFactory.Options();
                o.inJustDecodeBounds = true;
                stream = new BufferedInputStream(new FileInputStream(filename), 8192);
                BitmapFactory.decodeStream(stream, null, o);
                stream.close();
                int scale = 0;
                while ((o.outWidth >> scale) > targetWidth || (o.outHeight >> scale) > targetHeight) {
                    scale++;
                }
                o = new Options();
                o.inSampleSize = 1 << scale;
            }
            stream = new BufferedInputStream(new FileInputStream(filename), 8192);
            return BitmapFactory.decodeStream(stream, null, o);
        } catch (final IOException e) {
            return null;
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    Log.w(Constants.LOGTAG, "Failed to close FileInputStream", e);
                }
            }
        }
    }



	public static void setHttpTimeout(int timeout) {
    	UrlImageViewHelper.timeout = timeout;
    }

	private static UrlImageViewCallback defaultCallback = new UrlImageViewCallback() {
	    @Override
	    public void onLoaded(Context context, 
	    						ImageView imageView,
	    						Bitmap loadedBitmap, 
	    						long messageId,  
	    						int imageId, 
	    						String filename, 
	    						boolean loadedFromCache) {
			NewtifryMessage2 message = NewtifryMessage2.get(context, messageId);
			if (message == null) {
				return;
			}
	    	if (loadedBitmap == null) {
	    		if (CommonUtilities.okToDownloadData(context) == false) {
		    		message.setImageLoadingStatus(imageId, NewtifryMessage2.IMAGE_NOT_LOADED);
	    		} else {
		    		message.setImageLoadingStatus(imageId, NewtifryMessage2.IMAGE_LOADING_ERROR);
	    		}
	        } else {
	        	message.setImageLoadingStatus(imageId, NewtifryMessage2.IMAGE_LOADED);
	        }
    		message.save(context);
	    }

		@Override
		public boolean isEnable(int position) {
			return false;
		}
	};
	
    public static void loadUrl(final Context context, 
								final long messageId, 
								final int imageId, 
    							final String url, 
								final UrlImageViewCallback callback,
    							final boolean cache) {
    	if (callback == null) {
    		// use default callback
            setUrlDrawable(context, null, url, messageId, imageId,defaultCallback, cache);
    	} else {
    		setUrlDrawable(context, null, url, messageId, imageId,callback, cache);
    	}
    }

    public static void cleanupCache(final Context context) {
    	cleanup(context, 0);
    	if (mDeadCache != null) {
    		mDeadCache.evictAll();
    	}
    }

    public static int getCacheCount(final Context context) {
    	final String[] files = context.getFilesDir().list();
        if (files == null) {
            return 0;
        }
        return files.length;
    }
    
    /**
     * Clear out cached images.
     * @param context
     * @param age The max age of a file. Files older than this age
     *              will be removed.
     */
    public static void cleanup(final Context context, long age) {
        try {
            // purge any *.urlimage files over age
            final String[] files = context.getFilesDir().list();
            if (files == null) {
                return;
            }
            for (final String file : files) {
                if (!file.endsWith(".urlimage")) {
                    continue;
                }

                final File f = new File(context.getFilesDir().getAbsolutePath() + '/' + file);
                if (age == 0 || System.currentTimeMillis() > f.lastModified() + age) {
                    f.delete(); 
                }
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Clear out all cached images 
     * @param context
     */
    public static void cleanup(final Context context) {
    	cleanup(context, 0);
    }
    
    
    public static Bitmap getCachedBitmap(String url) {
        if (url == null)
            return null;
        Bitmap ret = null;
        if (mDeadCache != null)
            ret = mDeadCache.get(url);
        if (ret != null)
            return ret;
        if (mLiveCache != null) {
            Drawable drawable = mLiveCache.get(url);
            if (drawable instanceof ZombieDrawable)
                return ((ZombieDrawable)drawable).getBitmap();
        }
        return null;
    }


    
    private static boolean isNullOrEmpty(final CharSequence s) {
        return (s == null || s.equals("") || s.equals("null") || s.equals("NULL"));
    }

    private static boolean checkCacheDuration(File file, long cacheDurationMs) {
        return cacheDurationMs == CACHE_DURATION_INFINITE || System.currentTimeMillis() < file.lastModified() + cacheDurationMs;
    }

    private static String getFileStreamPath(Context context, String url) {
    	String filename = url.hashCode() + ".urlimage";
    	return context.getFileStreamPath(filename).getAbsolutePath();
    }
    
    public static void setUrlDrawable(final Context context, 
    									final ImageView imageView, 
    									final String url, 
    									final long messageId,
    									final int imageId, 
    									final UrlImageViewCallback callback, 
    									final boolean cache) {
    	long cacheDurationMs = Preferences.getCacheBitmapDurationInMs(context);
//        assert (Looper.getMainLooper().getThread() == Thread.currentThread()) : "setUrlDrawable and loadUrlDrawable should only be called from the main thread.";
        cleanup(context, cacheDurationMs);
        // disassociate this ImageView from any pending downloads
        if (isNullOrEmpty(url)) {
            if (imageView != null) {
                mPendingViews.remove(imageView);
            }
            return;
        }

        final int tw;
        final int th;
        if (mMetrics == null)
            prepareResources(context);
        tw = mMetrics.widthPixels;
        th = mMetrics.heightPixels;

        
        
        final String filename = getFileStreamPath(context, url);
//        final String filename = context.getFileStreamPath(getFilenameForUrl(url)).getAbsolutePath();
        final File file = new File(filename);

        // check the dead and live cache to see if we can find this url's bitmap
        if (mDeadCache == null) {
            mDeadCache = new LruBitmapCache(getHeapSize(context) / 8);
        }
        Drawable drawable = null;
        Bitmap bitmap = mDeadCache.remove(url);
        if (bitmap != null) {
            clog("zombie load: " + url);
        } else {
            drawable = mLiveCache.get(url);
        }

        // if something was found, verify it was fresh.
        if (drawable != null || bitmap != null) {
            clog("Cache hit on: " + url);
            // if the file age is older than the cache duration, force a refresh.
            // note that the file must exist, otherwise it is using a default.
            // not checking for file existence would do a network call on every
            // 404 or failed load.
            if (file.exists() && !checkCacheDuration(file, cacheDurationMs)) {
                clog("Cache hit, but file is stale. Forcing reload: " + url);
                if (drawable != null && drawable instanceof ZombieDrawable)
                    ((ZombieDrawable)drawable).headshot();
                drawable = null;
                bitmap = null;
            }
            else {
                clog("Using cached: " + url);
            }
        }

        // if the bitmap is fresh, set the imageview
        if (drawable != null || bitmap != null) {
            if (imageView != null) {
                mPendingViews.remove(imageView);
                if (drawable instanceof ZombieDrawable)
                    drawable = ((ZombieDrawable)drawable).clone(mResources);
                else if (bitmap != null)
                    drawable = new ZombieDrawable(url, mResources, bitmap);

                imageView.setImageDrawable(drawable);
            }
            // invoke any bitmap callbacks
            if (callback != null && callback.isEnable(imageId)) {
                // when invoking the callback from cache, check to see if this was
                // a drawable that was successfully loaded from the filesystem or url.
                // this will be indicated by it being a ZombieDrawable (ie, something we are managing).
                // The default drawables will be BitmapDrawables (or whatever else the user passed in).
                if (bitmap == null && drawable instanceof ZombieDrawable)
                    bitmap = ((ZombieDrawable)drawable).getBitmap();
                callback.onLoaded(context, imageView, bitmap, messageId, imageId, url, true);
            }
            return;
        }

        if (CommonUtilities.okToDownloadData(context) == false) {
            callback.onLoaded(context, imageView, null, messageId, imageId, url, true);
        	return; // no image in cache and no wifi
        }
        // oh noes, at this point we definitely do not have the file available in memory
        // let's prepare for an asynchronous load of the image.

        // null it while it is downloading
        // since listviews reuse their views, we need to
        // take note of which url this view is waiting for.
        // This may change rapidly as the list scrolls or is filtered, etc.
        clog("Waiting for " + url + " " + imageView);
        if (imageView != null) {
            mPendingViews.put(imageView, url);
        }

        final ArrayList<ImageView> currentDownload = mPendingDownloads.get(url);
        if (currentDownload != null && currentDownload.size() != 0) {
            // Also, multiple vies may be waiting for this url.
            // So, let's maintain a list of these views.
            // When the url is downloaded, it sets the imagedrawable for
            // every view in the list. It needs to also validate that
            // the imageview is still waiting for this url.
            if (imageView != null) {
                currentDownload.add(imageView);
            }
            return;
        }

        final ArrayList<ImageView> downloads = new ArrayList<ImageView>();
        if (imageView != null) {
            downloads.add(imageView);
        }
        mPendingDownloads.put(url, downloads);

        final int targetWidth = tw <= 0 ? Integer.MAX_VALUE : tw;
        final int targetHeight = th <= 0 ? Integer.MAX_VALUE : th;
        final Loader loader = new Loader() {
            @Override
            public void onDownloadComplete(UrlDownloader downloader, InputStream in, String existingFilename) {
                try {
                    assert (in == null || existingFilename == null);
                    if (in == null && existingFilename == null)
                        return;
                    String targetFilename = filename;
                    if (in != null) {
                        in = new BufferedInputStream(in, 8192);
                        OutputStream fout = new BufferedOutputStream(new FileOutputStream(filename), 8192);
                        copyStream(in, fout);
                        fout.close();
                        // TODO : save a thumnail of this bitmap in a public dir 
                        // to use by the smartwatch plugin
                    }
                    else {
                        targetFilename = existingFilename;
                    }
                    result = loadBitmapFromStream(context, url, targetFilename, targetWidth, targetHeight);
                }
                catch (final Exception ex) {
                    // always delete busted files when we throw.
                    new File(filename).delete();
                    if (Constants.LOG_ENABLED)
                        Log.e(Constants.LOGTAG, "Error loading " + url, ex);
                }
                finally {
                    // if we're not supposed to cache this thing, delete the temp file.
                    if ((downloader != null && !downloader.allowCache()))
                        new File(filename).delete();
                }
            }
        };

        final Runnable completion = new Runnable() {
            @Override
            public void run() {
//                assert (Looper.myLooper().equals(Looper.getMainLooper()));
                Bitmap bitmap = loader.result;
                Drawable usableResult = null;
                if (bitmap != null) {
                    usableResult = new ZombieDrawable(url, mResources, bitmap);
                }
                if (usableResult == null) {
                    clog("No usable result, defaulting " + url);
                    usableResult = null;
                    mLiveCache.put(url, usableResult);
                }
                mPendingDownloads.remove(url);
//                mLiveCache.put(url, usableResult);
                if (callback != null && callback.isEnable(imageId) && imageView == null)
                    callback.onLoaded(context, null, loader.result, messageId, imageId, url, false);
                int waitingCount = 0;
                for (final ImageView iv: downloads) {
                    // validate the url it is waiting for
                    final String pendingUrl = mPendingViews.get(iv);
                    if (!url.equals(pendingUrl)) {
                        clog("Ignoring out of date request to update view for " + url + " " + pendingUrl + " " + iv);
                        continue;
                    }
                    waitingCount++;
                    mPendingViews.remove(iv);
                    if (usableResult != null) {
//                        System.out.println(String.format("imageView: %dx%d, %dx%d", imageView.getMeasuredWidth(), imageView.getMeasuredHeight(), imageView.getWidth(), imageView.getHeight()));
                        iv.setImageDrawable(usableResult);
//                        System.out.println(String.format("imageView: %dx%d, %dx%d", imageView.getMeasuredWidth(), imageView.getMeasuredHeight(), imageView.getWidth(), imageView.getHeight()));
                        // onLoaded is called with the loader's result (not what is actually used). null indicates failure.
                    }
                    if (callback != null && callback.isEnable(imageId) && iv == imageView)
                        callback.onLoaded(context, iv, loader.result, messageId, imageId, url, false);
                }
                clog("Populated: " + waitingCount);
            }
        };


        if (file.exists()) {
            try {
                if (checkCacheDuration(file, cacheDurationMs)) {
                    clog("File Cache hit on: " + url + ". " + (System.currentTimeMillis() - file.lastModified()) + "ms old.");

                    final AsyncTask<Void, Void, Void> fileloader = new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(final Void... params) {
                            loader.onDownloadComplete(null, null, filename);
                            return null;
                        }
                        @Override
                        protected void onPostExecute(final Void result) {
                            completion.run();
                        }
                    };
                    executeTask(fileloader);
                    return;
                }
                else {
                    clog("File cache has expired. Refreshing.");
                }
            }
            catch (final Exception ex) {
            }
        }
        
        for (UrlDownloader downloader: mDownloaders) {
            if (downloader.canDownloadUrl(url)) {
                downloader.download(context, url, filename, loader, completion, timeout);
                return;
            }
        }
    }
   
   
    private static abstract class Loader implements UrlDownloader.UrlDownloaderCallback {
        Bitmap result;
    }
    
    private static HttpUrlDownloader mHttpDownloader = new HttpUrlDownloader();
    private static FileUrlDownloader mFileDownloader = new FileUrlDownloader();
    private static ArrayList<UrlDownloader> mDownloaders = new ArrayList<UrlDownloader>();
    public static ArrayList<UrlDownloader> getDownloaders() {
        return mDownloaders;
    }
    
    static {
        mDownloaders.add(mHttpDownloader);
        mDownloaders.add(mFileDownloader);
    }
    
    public static interface RequestPropertiesCallback {
        public ArrayList<NameValuePair> getHeadersForRequest(Context context, String url);
    }

    private static RequestPropertiesCallback mRequestPropertiesCallback;

    public static RequestPropertiesCallback getRequestPropertiesCallback() {
        return mRequestPropertiesCallback;
    }

    public static void setRequestPropertiesCallback(final RequestPropertiesCallback callback) {
        mRequestPropertiesCallback = callback;
    }

    private static DrawableCache mLiveCache = DrawableCache.getInstance();
    private static LruBitmapCache mDeadCache;
    private static HashSet<Bitmap> mAllCache = new HashSet<Bitmap>();

    private static int getHeapSize(final Context context) {
        return ((ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass() * 1024 * 1024;
    }

    /***
     * Remove a url from the cache
     * @param url
     * @return The bitmap removed, if any.
     */
    public static Bitmap remove(Context context, String url) {
    	final String filename = getFileStreamPath(context, url);
        new File(filename).delete();
        
        Drawable drawable = mLiveCache.remove(url);
        if (drawable instanceof ZombieDrawable) {
            ZombieDrawable zombie = (ZombieDrawable)drawable;
            Bitmap ret = zombie.getBitmap();
            zombie.headshot();
            return ret;
        }
        
        return null;
    }
    
    /***
     * ZombieDrawable refcounts Bitmaps by hooking the finalizer.
     *
     */
    private static class ZombieDrawable extends BitmapDrawable {
        private static class Brains {
            int mRefCounter;
            boolean mHeadshot;
        }
        public ZombieDrawable(final String url, Resources resources, final Bitmap bitmap) {
            this(url, resources, bitmap, new Brains());
        }

        Brains mBrains;
        private ZombieDrawable(final String url, Resources resources, final Bitmap bitmap, Brains brains) {
            super(resources, bitmap);
            mUrl = url;
            mBrains = brains;

            mAllCache.add(bitmap);
            mDeadCache.remove(url);
            mLiveCache.put(url, this);
            
            mBrains.mRefCounter++;
        }
        
        public ZombieDrawable clone(Resources resources) {
            return new ZombieDrawable(mUrl, resources, getBitmap(), mBrains);
        }

        String mUrl;

        @Override
        protected void finalize() throws Throwable {
            super.finalize();

            mBrains.mRefCounter--;
            if (mBrains.mRefCounter == 0) {
                if (!mBrains.mHeadshot)
                    mDeadCache.put(mUrl, getBitmap());
                mAllCache.remove(getBitmap());
                mLiveCache.remove(mUrl);
                clog("Zombie GC event " + mUrl);
            }
        }

        // kill this zombie, forever.
        public void headshot() {
            clog("BOOM! Headshot: " + mUrl);
            mBrains.mHeadshot = true;
            mLiveCache.remove(mUrl);
            mAllCache.remove(getBitmap());
        }
    }

    static void executeTask(final AsyncTask<Void, Void, Void> task) {
        if (Build.VERSION.SDK_INT < Constants.HONEYCOMB) {
            task.execute();
        } else {
            executeTaskHoneycomb(task);
        }
    }

    @TargetApi(Constants.HONEYCOMB)
    private static void executeTaskHoneycomb(final AsyncTask<Void, Void, Void> task) {
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static  int getPendingDownloads() {
        return mPendingDownloads.size();
    }

    private static Hashtable<ImageView, String> mPendingViews = new Hashtable<ImageView, String>();
    private static Hashtable<String, ArrayList<ImageView>> mPendingDownloads = new Hashtable<String, ArrayList<ImageView>>();
}




