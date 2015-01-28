package io.github.hidroh.materialistic.data;

import android.content.AsyncQueryHandler;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.CursorWrapper;
import android.os.Parcel;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.DateUtils;

import io.github.hidroh.materialistic.HackerNewsClient;
import io.github.hidroh.materialistic.R;

public class FavoriteManager {

    public static final int LOADER = 0;
    public static final String ACTION_CLEAR = FavoriteManager.class.getName() + ".ACTION_CLEAR";

    public static void add(Context context, HackerNewsClient.Item story) {
        final ContentValues contentValues = new ContentValues();
        contentValues.put(MaterialisticProvider.FavoriteEntry.COLUMN_NAME_ITEM_ID, story.getId());
        contentValues.put(MaterialisticProvider.FavoriteEntry.COLUMN_NAME_URL, story.getUrl());
        contentValues.put(MaterialisticProvider.FavoriteEntry.COLUMN_NAME_TITLE, story.getTitle());
        contentValues.put(MaterialisticProvider.FavoriteEntry.COLUMN_NAME_TIME, String.valueOf(System.currentTimeMillis()));
        new AsyncQueryHandler(context.getContentResolver()) { }
                .startInsert(0, null, MaterialisticProvider.URI_FAVORITE, contentValues);
    }

    public static void clear(Context context) {
        final LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(context);
        new AsyncQueryHandler(context.getContentResolver()) {
            @Override
            protected void onDeleteComplete(int token, Object cookie, int result) {
                super.onDeleteComplete(token, cookie, result);
                broadcastManager.sendBroadcast(makeClearBroadcastIntent());
            }
        }.startDelete(0, null, MaterialisticProvider.URI_FAVORITE, null, null);
    }

    public static void check(Context context, final String itemId, final OperationCallbacks callbacks) {
        if (itemId == null) {
            return;
        }

        new AsyncQueryHandler(context.getContentResolver()) {
            @Override
            protected void onQueryComplete(int token, Object cookie, android.database.Cursor cursor) {
                super.onQueryComplete(token, cookie, cursor);
                if (cookie == null) {
                    return;
                }

                if (callbacks == null) {
                    return;
                }

                // TODO switch to local broadcast
                if (itemId.equals(cookie)) {
                    callbacks.onCheckComplete(cursor.getCount() > 0);
                }
            }
        }.startQuery(0, itemId, MaterialisticProvider.URI_FAVORITE, null,
                MaterialisticProvider.FavoriteEntry.COLUMN_NAME_ITEM_ID + " = ?",
                new String[]{itemId}, null);
    }

    public static void remove(Context context, final String itemId) {
        if (itemId == null) {
            return;
        }

        new AsyncQueryHandler(context.getContentResolver()) {
            @Override
            protected void onDeleteComplete(int token, Object cookie, int result) {
                super.onDeleteComplete(token, cookie, result);
                if (cookie == null) {
                    return;
                }

                // TODO local broadcast
            }
        }.startDelete(0, itemId, MaterialisticProvider.URI_FAVORITE,
                MaterialisticProvider.FavoriteEntry.COLUMN_NAME_ITEM_ID + " = ?",
                new String[]{itemId});
    }

    public static IntentFilter makeClearIntentFilter() {
        return new IntentFilter(ACTION_CLEAR);
    }

    private static Intent makeClearBroadcastIntent() {
        return new Intent(ACTION_CLEAR);
    }

    public static class Favorite implements HackerNewsClient.WebItem {
        private String itemId;
        private String url;
        private String title;
        private long time;

        public static final Creator<Favorite> CREATOR = new Creator<Favorite>() {
            @Override
            public Favorite createFromParcel(Parcel source) {
                return new Favorite(source);
            }

            @Override
            public Favorite[] newArray(int size) {
                return new Favorite[size];
            }
        };

        private Favorite(String itemId, String url, String title, long time) {
            this.itemId = itemId;
            this.url = url;
            this.title = title;
            this.time = time;
        }

        private Favorite(Parcel source) {
            itemId = source.readString();
            url = source.readString();
            title = source.readString();
        }

        @Override
        public String getUrl() {
            return url;
        }

        @Override
        public boolean isShareable() {
            return true;
        }

        @Override
        public String getId() {
            return itemId;
        }

        @Override
        public String getDisplayedTitle() {
            return title;
        }

        public String getCreated(Context context) {
            return context.getString(R.string.saved, DateUtils.getRelativeDateTimeString(context, time,
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.YEAR_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_MONTH));
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(itemId);
            dest.writeString(url);
            dest.writeString(title);
        }
    }

    public static class Cursor extends CursorWrapper {
        public Cursor(android.database.Cursor cursor) {
            super(cursor);
        }

        public Favorite getFavorite() {
            final String itemId = getString(getColumnIndexOrThrow(MaterialisticProvider.FavoriteEntry.COLUMN_NAME_ITEM_ID));
            final String url = getString(getColumnIndexOrThrow(MaterialisticProvider.FavoriteEntry.COLUMN_NAME_URL));
            final String title = getString(getColumnIndexOrThrow(MaterialisticProvider.FavoriteEntry.COLUMN_NAME_TITLE));
            final String time = getString(getColumnIndexOrThrow(MaterialisticProvider.FavoriteEntry.COLUMN_NAME_TIME));
            return new Favorite(itemId, url, title, Long.valueOf(time));
        }
    }

    public static class CursorLoader extends android.support.v4.content.CursorLoader {
        public CursorLoader(Context context) {
            super(context, MaterialisticProvider.URI_FAVORITE, null, null, null, null);
        }
    }

    public static abstract class OperationCallbacks {
        public void onCheckComplete(boolean isFavorite) { }
    }

}