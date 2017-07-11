package com.izettle.wrench.core;

import android.net.Uri;

public class WrenchProviderContract {
    public static final String WRENCH_AUTHORITY = BuildConfig.WRENCH_AUTHORITY;

    private static final Uri boltUri = Uri.parse("content://" + WRENCH_AUTHORITY + "/currentConfiguration");
    private static final Uri nutUri = Uri.parse("content://" + WRENCH_AUTHORITY + "/predefinedConfigurationValue");

    public static Uri boltUri(long id) {
        return Uri.withAppendedPath(boltUri, String.valueOf(id));
    }

    public static Uri boltUri(String key) {
        return Uri.withAppendedPath(boltUri, Uri.encode(key));
    }

    public static Uri boltUri() {
        return boltUri;
    }

    public static Uri nutUri() {
        return nutUri;
    }
}