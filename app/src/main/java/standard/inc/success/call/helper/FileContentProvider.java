package standard.inc.success.call.helper;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Objects;

public class FileContentProvider extends ContentProvider {
  private static final String TAG = "FileContentProvider";
  // Define authority and URI matcher
  private static final String AUTHORITY = "standard.inc.success.call.helper.FileContentProvider";
  private static final int FILE_CODE = 1;
  private static final UriMatcher uriMatcher;

  static {
    uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    uriMatcher.addURI(AUTHORITY, "cte", FILE_CODE);
    uriMatcher.addURI(AUTHORITY, "cte/*", FILE_CODE);
  }

  @Override
  public boolean onCreate() {
    return true;
  }

  @Override
  public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException {
//    Log.d(TAG, "openFile, uri: " + uri);
    // Match the incoming URI to determine if it's a valid request
    if (uriMatcher.match(uri) != FILE_CODE) {
      throw new FileNotFoundException("Invalid URI");
    }

    // Get the file path from the URI
    File file = new File(
      Objects.requireNonNull(getContext()).getFilesDir(),
      Objects.requireNonNull(uri.getLastPathSegment())
    );
    if (!file.exists()) {
      throw new FileNotFoundException("File not found: " + file.getAbsolutePath());
    }

    // Return a ParcelFileDescriptor to allow other apps to read the file
    return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
  }

  // Unused methods for this example, but required by ContentProvider
  @Override
  public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
    return null;
  }

  @Override
  public String getType(@NonNull Uri uri) {
    return null;
  }

  @Override
  public Uri insert(@NonNull Uri uri, ContentValues values) {
    return null;
  }

  @Override
  public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
    return 0;
  }

  @Override
  public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
    return 0;
  }

  public static String getUriForFile(String filename) {
    return "content://" + AUTHORITY + "/cte/" + filename;
  }
}

