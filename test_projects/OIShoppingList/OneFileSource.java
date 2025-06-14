
import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

public class AnimatableTextView extends TextView {
    public AnimatableTextView(Context context) {
        super(context);
    }

    public AnimatableTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AnimatableTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.view.WearableListView;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import org.openintents.shopping.R;
import org.openintents.shopping.library.provider.ShoppingContract;

import java.util.ArrayList;
import java.util.List;

public class ShoppingActivity extends Activity implements ServiceConnection, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, ResultCallback<DataItemBuffer> {

    public static final String EXTRA_LIST_ID = "EXTRA_LIST_ID";
    private static final String TAG = "ShoppintListActivity";
    private TextView mTextView;
    private ShoppingWearableListenerService mService;
    private com.google.android.gms.common.api.GoogleApiClient mGoogleApiClient;
    private ShoppingDataItemAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shopping);
        final WearableListView listView = (WearableListView) findViewById(R.id.shopping_list);
        adapter = new ShoppingDataItemAdapter(this);
        listView.setAdapter(adapter);

        listView.setClickListener(new WearableListView.ClickListener() {
            @Override
            public void onClick(WearableListView.ViewHolder viewHolder) {
                long itemId = viewHolder.getItemId();
                Log.d(TAG, "id: " + viewHolder.getItemId());

                int position = viewHolder.getPosition();
                DataItem item = adapter.getItem(position);
                Log.d(TAG, "id: " + DataMapItem.fromDataItem(item).getDataMap().getString(ShoppingContract.ContainsFull.ITEM_NAME));
                Log.d(TAG, "url:" + item.getUri().toString());
                toggleShoppingItem(position, itemId);
            }

            @Override
            public void onTopEmptyRegionClick() {

            }
        });

        registerLocalNewDataReceiver();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
    }

    private void toggleShoppingItem(int position, long itemId) {
        PutDataRequest request;
        //Wearable.DataApi.putDataItem(mGoogleApiClient, request);

        adapter.remove(position);
    }

    private void registerLocalNewDataReceiver() {
        IntentFilter intentFilter = new IntentFilter("new_data");
        LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                byte[] items = intent.getByteArrayExtra("data");
            }
        }, intentFilter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mGoogleApiClient.isConnected()){
            loadItems();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mService = (ShoppingWearableListenerService) service;
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mService = null;
    }

    @Override
    public void onConnected(Bundle bundle) {
        loadItems();
    }

    private void loadItems() {
        Wearable.DataApi.getDataItems(mGoogleApiClient).setResultCallback(this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onResult(DataItemBuffer dataItemBuffer) {
        List<DataItem> items = new ArrayList<DataItem>();
        String listPrefix = "/" + getListId() + "/";

        for (int i=0; i< dataItemBuffer.getCount(); i++){
            DataItem item = dataItemBuffer.get(i);
            if (item.getUri().getPath().startsWith(listPrefix)){
                DataMap mapItem = DataMapItem.fromDataItem(item).getDataMap();
                String name = mapItem.getString(ShoppingContract.ContainsFull.ITEM_NAME);
                if (!TextUtils.isEmpty(name)) {
                    items.add(item);
                }
            }
        }

        adapter.setItems(items);
    }

    private String getListId() {
        return getIntent().getStringExtra(EXTRA_LIST_ID);
    }
}


import android.app.Activity;
import android.os.Bundle;

public class LaunchActivity extends Activity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ShoppingWearableListenerService.buildShoppingNotification(this, "1");
        finish();
    }
}


import android.content.Context;
import android.support.wearable.view.WearableListView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.openintents.shopping.R;

public class ShoppingItemView extends FrameLayout implements WearableListView.OnCenterProximityListener {


    private final TextView title;
    private final TextView tags;
    private final float mDefaultTextSize;
    private final float mSelectedTextSize;

    public ShoppingItemView(Context context, float defaultTextSize, float selectedTextSize) {
        super(context);
        View.inflate(context, R.layout.item_shopping, this);
        title = (TextView) findViewById(R.id.title);
        tags = (TextView) findViewById(R.id.tags);
        mDefaultTextSize = defaultTextSize;
        mSelectedTextSize = selectedTextSize;
    }

    @Override
    public void onCenterPosition(boolean b) {
        title.animate().scaleX(1f).scaleY(1f).alpha(1);
        tags.animate().scaleX(1f).scaleY(1f).alpha(1);
    }

    @Override
    public void onNonCenterPosition(boolean b) {
        title.animate().scaleX(0.8f).scaleY(0.8f).alpha(0.6f);
        tags.animate().scaleX(0.8f).scaleY(0.8f).alpha(0.6f);
    }
}


import android.content.Context;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;

import org.openintents.shopping.R;
import org.openintents.shopping.library.provider.ShoppingContract;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class ShoppingDataItemAdapter extends WearableListView.Adapter {

    private static final String EMPTY_STRING = "";
    private static final String TAG = "ShoppingDataItemAdapter";
    private List<DataItem> mItems = new ArrayList<DataItem>();
    private Context mContext;

    public ShoppingDataItemAdapter(Context context){
        mContext = context;
    }

    @Override
    public WearableListView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int position) {
        return new WearableListView.ViewHolder(new ShoppingItemView(mContext, 14, 20));
    }

    @Override
    public void onBindViewHolder(WearableListView.ViewHolder viewHolder, int position) {
        String name = DataMapItem.fromDataItem(mItems.get(position)).getDataMap().getString(ShoppingContract.ContainsFull.ITEM_NAME);
        String quantity =DataMapItem.fromDataItem(mItems.get(position)).getDataMap().getString(ShoppingContract.ContainsFull.QUANTITY);
        String units = DataMapItem.fromDataItem(mItems.get(position)).getDataMap().getString(ShoppingContract.ContainsFull.ITEM_UNITS);
        String status = DataMapItem.fromDataItem(mItems.get(position)).getDataMap().getString(ShoppingContract.ContainsFull.STATUS);
        String titleDisplay = getTitle(name, quantity, units);
        ((TextView)viewHolder.itemView.findViewById(R.id.title)).setText(titleDisplay);

        String tags = DataMapItem.fromDataItem(mItems.get(position)).getDataMap().getString(ShoppingContract.ContainsFull.ITEM_TAGS);
        if (tags == null){
            tags = EMPTY_STRING;
        }
        ((TextView)viewHolder.itemView.findViewById(R.id.tags)).setText(tags);
    }

    private String getTitle(String name, String quantity, String units) {
        String titleDisplay;
        if (quantity == null){
            titleDisplay = name;
        } else {
            if (units == null){
                titleDisplay = quantity + " " + name;
            } else {
                titleDisplay = quantity + units + " " + name;
            }
        }
        return titleDisplay;
    }

    public void setItems(DataItemBuffer items) {
        mItems.clear();
        for (int i= 0; i < items.getCount();i++){
            Log.d(TAG, "received: " + items.get(i).getUri() );

            DataMap contentValues = DataMapItem.fromDataItem(items.get(i)).getDataMap();
            Log.d(TAG, "content:" + contentValues.getString(ShoppingContract.ContainsFull.ITEM_NAME));
            this.mItems.add(items.get(i));
        }
        notifyDataSetChanged();
    }

    public void setItems(Collection<DataItem> items) {
        mItems.clear();
        mItems.addAll(items);
        notifyDataSetChanged();
    }

    public DataItem getItem(int position){
        if (mItems != null){
            return mItems.get(position);
        } else {
            return null;
        }
    }

    @Override
    public int getItemCount() {
        if (mItems == null){
            return 0;
        } else {
            return mItems.size();
        }
    }

    public void remove(int position) {
        mItems.remove(position);
        notifyDataSetChanged();
    }
}


import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import org.openintents.shopping.R;

public class ShoppingWearableListenerService extends WearableListenerService implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    private GoogleApiClient mGoogleApiClient;

    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onPeerConnected(Node peer) {
        super.onPeerConnected(peer);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        FreezableUtils.freezeIterable(dataEvents);
        for (DataEvent dataEvent : dataEvents) {
            if (dataEvent.getType() == DataEvent.TYPE_CHANGED) {
                DataMap dataMap = DataMapItem.fromDataItem(dataEvent.getDataItem()).getDataMap();

            }
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath() != null){
            String listId = new String(messageEvent.getData());
            buildShoppingNotification(this, listId);
        }
    }

    public static void buildShoppingNotification(Context context, String listId) {
        Intent shoppingActivityIntent = createShoppingActivityIntent(context, listId);
        PendingIntent intent = PendingIntent.getActivity(context, 0, shoppingActivityIntent, 0);
        Notification notification = new NotificationCompat.Builder(context)
                .setContentText("Start Shopping")
                .setContentTitle("Items are synchronized")
                .setSmallIcon(R.drawable.ic_launcher_shoppinglist)
                .setContentIntent(intent)
                .build();
        NotificationManagerCompat.from(context).notify(1, notification);
    }

    private static Intent createShoppingActivityIntent(Context context, String listId) {
        Intent i = new Intent(context, ShoppingActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.putExtra(ShoppingActivity.EXTRA_LIST_ID, listId);
        return i;
    }

    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
}


import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

public interface SyncSupport {
    boolean isAvailable();
    void pushListItem(long listId, Cursor cursor);
    void updateListItem(long listId, Uri itemUri, ContentValues values);
    void pushList(Cursor cursor);

    boolean isSyncEnabled();
    void setSyncEnabled(boolean enableSync);
}

/* 
 * Copyright (C) 2007-2011 OpenIntents.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import android.text.TextUtils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

public class PriceConverter {

    public static NumberFormat mPriceFormatter = DecimalFormat
            .getNumberInstance(Locale.ENGLISH);

    private static boolean initialized;

    public static void init() {
        PriceConverter.mPriceFormatter.setMaximumFractionDigits(2);
        PriceConverter.mPriceFormatter.setMinimumFractionDigits(2);
        initialized = true;
    }

    public static Long getCentPriceFromString(String price) {
        if (!initialized) {
            init();
        }
        Long priceLong;
        if (TextUtils.isEmpty(price)) {
            priceLong = 0L;
        } else {
            try {
                priceLong = Math
                        .round(100 * PriceConverter.mPriceFormatter
                                .parse(price).doubleValue());
            } catch (ParseException e) {
                priceLong = null;
            }
        }
        return priceLong;
    }

    public static String getStringFromCentPrice(long pricecent) {
        if (!initialized) {
            init();
        }
        String price = mPriceFormatter.format(pricecent * 0.01d);
        if (pricecent == 0) {
            // Empty field for easier editing
            // (Otherwise "0.00" has to be deleted manually first)
            price = "";
        }
        return price;
    }
}

/* 
 * Copyright (C) 2007-2011 OpenIntents.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;

import org.openintents.shopping.library.provider.ShoppingContract;
import org.openintents.shopping.library.provider.ShoppingContract.*;

public class ShoppingUtils {
    /**
     * TAG for logging.
     */
    private static final String TAG = "ShoppingUtils";
    private static final boolean debug = false;

    /**
     * Obtain item id by name.
     *
     * @param context
     * @param name
     * @return Item ID or -1 if item does not exist.
     */
    public static long getItemId(Context context, String name) {
        long id = -1;

        Cursor existingItems = context.getContentResolver().query(
                Items.CONTENT_URI, new String[]{Items._ID},
                "upper(name) = upper(?)", new String[]{name}, null);
        if (existingItems.getCount() > 0) {
            existingItems.moveToFirst();
            id = existingItems.getLong(0);
        }

        existingItems.close();
        return id;
    }

    public static long getItemIdForList(Context context, String name, String list_id) {
        long id = -1;

        Cursor existingItems = context.getContentResolver().query(
                ContainsFull.CONTENT_URI, new String[]{Contains.ITEM_ID},
                "list_id = ? and upper(items.name) = upper(?)", new String[]{list_id, name}, null);
        if (existingItems.getCount() > 0) {
            existingItems.moveToFirst();
            id = existingItems.getLong(0);
        }
        existingItems.close();
        return id;
    }

    public static String getItemName(Context context, long itemId) {
        String name = "";
        Cursor existingItems = context.getContentResolver().query(
                ShoppingContract.Items.CONTENT_URI,
                new String[]{ShoppingContract.Items.NAME}, "_id = ?",
                new String[]{String.valueOf(itemId)}, null);
        if (existingItems.getCount() > 0) {
            existingItems.moveToFirst();
            name = existingItems.getString(0);
        }
        existingItems.close();
        return name;
    }

    /**
     * Gets or creates a new item and returns its id. If the item exists
     * already, the existing id is returned. Otherwise a new item is created.
     *
     * @param name    New name of the item.
     * @param price
     * @param barcode
     * @return id of the new or existing item.
     */
    public static long updateOrCreateItem(Context context, String name,
                                          String tags, String price, String barcode, String list_id) {
        long id;

        if (list_id == null) {
            id = getItemId(context, name);
        } else {
            id = getItemIdForList(context, name, list_id);
        }

        if (id >= 0) {
            // Update existing item
            // (pass 'null' for name: Existing item: no need to change name.)
            ContentValues values = getContentValues(name, tags, price, barcode);
            try {
                Uri uri = Uri.withAppendedPath(
                        ShoppingContract.Items.CONTENT_URI, String.valueOf(id));
                context.getContentResolver().update(uri, values, null, null);
                if (debug) {
                    Log.d(TAG, "updated item: " + uri);
                }
            } catch (Exception e) {
                Log.e(TAG, "Update item failed", e);
            }
        }

        if (id == -1) {
            // Add new item to list.
            ContentValues values = getContentValues(name, tags, price, barcode);
            try {
                Uri uri = context.getContentResolver().insert(
                        ShoppingContract.Items.CONTENT_URI, values);
                if (debug) {
                    Log.d(TAG, "Insert new item: " + uri);
                }
                id = Long.parseLong(uri.getPathSegments().get(1));
            } catch (Exception e) {
                Log.e(TAG, "Insert item failed", e);
                // return -1
            }
        }
        return id;

    }

    private static ContentValues getContentValues(String name, String tags,
                                                  String price, String barcode) {
        ContentValues values = new ContentValues(4);
        if (name != null) {
            values.put(ShoppingContract.Items.NAME, name);
        }
        if (tags != null) {
            values.put(ShoppingContract.Items.TAGS, tags);
        }
        if (price != null) {
            Long priceLong = PriceConverter.getCentPriceFromString(price);
            values.put(ShoppingContract.Items.PRICE, priceLong);
        }
        if (barcode != null) {
            values.put(ShoppingContract.Items.BARCODE, barcode);
        }
        return values;
    }

    /**
     * Gets or creates a new item and returns its id. If the item exists
     * already, the existing id is returned. Otherwise a new item is created.
     *
     * @param name New name of the item.
     * @return id of the new or existing item.
     */
    public static long getItem(Context context, String name, String tags,
                               String price, String units, String note, Boolean duplicate,
                               Boolean update) {
        long id = -1;

        if (!duplicate) {
            if (id == -1) {
                id = getItemId(context, name);
            }

            if (id != -1 && !update) {
                return id;
            }
        }

        return getItem(context, id, name, tags, price, units, note);
    }

    /**
     * Gets or creates a new item and returns its id. If the item exists
     * already, the existing id is returned. Otherwise a new item is created.
     *
     * @param id   id of the item to update, or -1 to create a new item.
     * @param name New name of the item.
     * @return id of the new or existing item.
     */
    public static long getItem(Context context, long id, String name,
                               String tags, String price, String units, String note) {

        // now we are either updating or adding.
        // either way we need some content values.
        // Add item to list:
        ContentValues values = new ContentValues(1);
        if (id == -1) {
            values.put(Items.NAME, name);
        }

        values.put(Items.TAGS, tags);
        if (!TextUtils.isEmpty(note)) {
            values.put(Items.NOTE, note);
        }
        if (price != null) {
            values.put(Items.PRICE, price);
        }
        if (!TextUtils.isEmpty(units)) {
            // in the items table we store the string directly,
            // but we register the units in the units table for use in
            // completion.
            long unit_id = getUnits(context, units);
            values.put(Items.UNITS, units);
        }

        try {
            if (id == -1) {
                Uri uri = context.getContentResolver().insert(
                        Items.CONTENT_URI, values);
                if (debug) {
                    Log.d(TAG, "Insert new item: " + uri);
                }
                id = Long.parseLong(uri.getPathSegments().get(1));
            } else {
                context.getContentResolver().update(
                        Uri.withAppendedPath(Items.CONTENT_URI,
                                String.valueOf(id)), values, null, null
                );
            }
        } catch (Exception e) {
            Log.e(TAG, "Insert item failed", e);
            // return -1
        }

        return id;
    }

    public static long getUnits(Context context, String units) {
        long id = -1;
        Cursor existingUnits = context.getContentResolver().query(
                Units.CONTENT_URI, new String[]{Units._ID},
                "upper(name) = upper(?)", new String[]{units}, null);
        if (existingUnits.getCount() > 0) {
            existingUnits.moveToFirst();
            id = existingUnits.getLong(0);
            existingUnits.close();

        } else {
            existingUnits.close();
            // Add item to list:
            ContentValues values = new ContentValues(1);
            values.put(Units.NAME, units);
            try {
                Uri uri = context.getContentResolver().insert(
                        Units.CONTENT_URI, values);
                if (debug) {
                    Log.d(TAG, "Insert new units: " + uri);
                }
                id = Long.parseLong(uri.getPathSegments().get(1));
            } catch (Exception e) {
                Log.e(TAG, "Insert units failed", e);
                // return -1
            }
        }
        return id;
    }

    /**
     * Gets or creates a new shopping list and returns its id. If the list
     * exists already, the existing id is returned. Otherwise a new list is
     * created.
     *
     * @param context
     * @param name    New name of the list.
     * @return id of the new or existing list.
     */
    public static long getList(Context context, final String name) {
        long id = -1;
        Cursor existingItems = context.getContentResolver().query(
                Lists.CONTENT_URI, new String[]{Items._ID},
                "upper(name) = upper(?)", new String[]{name}, null);
        if (existingItems.getCount() > 0) {
            existingItems.moveToFirst();
            id = existingItems.getLong(0);
            existingItems.close();
        } else {
            // Add list to list:
            ContentValues values = new ContentValues(1);
            values.put(Lists.NAME, name);
            try {
                Uri uri = context.getContentResolver().insert(
                        Lists.CONTENT_URI, values);
                if (debug) {
                    Log.d(TAG, "Insert new list: " + uri);
                }
                id = Long.parseLong(uri.getPathSegments().get(1));
            } catch (Exception e) {
                Log.e(TAG, "insert list failed", e);
                return -1;
            }
        }
        return id;
    }

    /**
     * Gets or creates a new store and returns its id. If the store exists
     * already, the existing id is returned. Otherwise a new store is created.
     *
     * @param context
     * @param name    New name of the list.
     * @return id of the new or existing list.
     */
    public static long getStore(Context context, final String name,
                                final long listId) {
        long id = -1;
        Cursor existingItems = context.getContentResolver().query(
                Stores.CONTENT_URI, new String[]{Stores._ID},
                "upper(name) = upper(?) AND list_id = ?",
                new String[]{name, String.valueOf(listId)}, null);
        if (existingItems.getCount() > 0) {
            existingItems.moveToFirst();
            id = existingItems.getLong(0);
            existingItems.close();
        } else {
            // Add list to list:
            ContentValues values = new ContentValues(1);
            values.put(Stores.NAME, name);
            values.put(Stores.LIST_ID, listId);
            try {
                Uri uri = context.getContentResolver().insert(
                        Stores.CONTENT_URI, values);
                if (debug) {
                    Log.d(TAG, "Insert new store: " + uri);
                }
                id = Long.parseLong(uri.getPathSegments().get(1));
            } catch (Exception e) {
                Log.e(TAG, "insert store failed", e);
                return -1;
            }
        }
        return id;
    }

    /**
     * Adds a new item to a specific list and returns its id. If the item exists
     * already, the existing id is returned.
     *
     * @param itemId       The id of the new item.
     * @param listId       The id of the shopping list the item is added.
     * @param status       The status of the new item
     * @param priority     The priority of the new item
     * @param quantity     The quantity of the new item
     * @param togglestatus If true, then status is toggled between WANT_TO_BUY and BOUGHT
     * @return id of the "contains" table entry, or -1 if insert failed.
     */
    public static long addItemToList(Context context, final long itemId,
                                     final long listId, final long status, String priority,
                                     String quantity, final boolean togglestatus,
                                     final boolean known_new, final boolean resetQuantity) {
        long id = -1;
        Cursor existingItems = null;

        if (!known_new) {
            existingItems = context.getContentResolver()
                    .query(Contains.CONTENT_URI,
                            new String[]{Contains._ID, Contains.STATUS},
                            "list_id = ? AND item_id = ?",
                            new String[]{String.valueOf(listId),
                                    String.valueOf(itemId)}, null
                    );
        }
        if (existingItems != null && existingItems.getCount() > 0) {
            existingItems.moveToFirst();
            id = existingItems.getLong(0);
            long oldstatus = existingItems.getLong(1);
            existingItems.close();

            long newstatus = Status.WANT_TO_BUY;
            // Toggle status:
            if (oldstatus == Status.WANT_TO_BUY) {
                newstatus = Status.BOUGHT;
            }

            // set status to want_to_buy:
            ContentValues values = new ContentValues(3);
            if (togglestatus) {
                values.put(Contains.STATUS, newstatus);
            } else {
                values.put(Contains.STATUS, status);
            }
            if (quantity != null) {
                // Only change quantity if an explicit value has been passed.
                // (see issue 286)
                values.put(ShoppingContract.Contains.QUANTITY, quantity);
            } else {
                if (resetQuantity) {
                    values.put(ShoppingContract.Contains.QUANTITY, "");
                }
            }
            if (priority != null) {
                values.put(ShoppingContract.Contains.PRIORITY, priority);
            }

            Uri uri = Uri.withAppendedPath(Contains.CONTENT_URI,
                    String.valueOf(id));
            try {
                context.getContentResolver().update(uri, values, null, null);
                if (debug) {
                    Log.d(TAG, "updated item: " + uri);
                }
            } catch (Exception e) {
                try {
                    // Maybe old version of OI Shopping List is installed:
                    values.remove(Contains.PRIORITY);
                    context.getContentResolver()
                            .update(uri, values, null, null);
                    if (debug) {
                        Log.d(TAG, "updated item: " + uri);
                    }
                } catch (Exception e2) {
                    Log.e(TAG, "insert into table 'contains' failed", e2);
                    id = -1;
                }
            }

        } else {
            if (existingItems != null) {
                existingItems.close();
            }
            // Add item to list:
            ContentValues values = new ContentValues(2);
            values.put(Contains.ITEM_ID, itemId);
            values.put(Contains.LIST_ID, listId);
            if (togglestatus) {
                values.put(Contains.STATUS, Status.WANT_TO_BUY);
            } else {
                values.put(Contains.STATUS, status);
            }
            if (quantity != null) {
                values.put(Contains.QUANTITY, quantity);
            }
            if (priority != null) {
                values.put(Contains.PRIORITY, priority);
            }

            try {
                Uri uri = context.getContentResolver().insert(
                        Contains.CONTENT_URI, values);
                if (debug) {
                    Log.d(TAG, "Insert new entry in 'contains': " + uri);
                }
                id = Long.parseLong(uri.getPathSegments().get(1));
            } catch (Exception e) {
                try {
                    // Maybe old version of OI Shopping List is installed:
                    values.remove(Contains.PRIORITY);
                    Uri uri = context.getContentResolver().insert(
                            Contains.CONTENT_URI, values);
                    if (debug) {
                        Log.d(TAG, "Insert new entry in 'contains': " + uri);
                    }
                    id = Long.parseLong(uri.getPathSegments().get(1));
                } catch (Exception e2) {
                    Log.e(TAG, "insert into table 'contains' failed", e2);
                    id = -1;
                }
            }
        }
        return id;
    }

    /**
     * Adds an item to a specific store and returns its id. If the item exists
     * already, the existing id is returned.
     *
     * @param itemId     The id of the new item.
     * @param storeId    The id of the shopping list the item is added.
     * @param stocksItem The type of the new item
     * @return id of the "contains" table entry, or -1 if insert failed.
     */
    public static long addItemToStore(Context context, final long itemId,
                                      final long storeId, final boolean stocksItem, final String aisle,
                                      final String price, boolean known_new) {
        long id = -1;
        Cursor existingItems = null;

        if (!known_new) {
            existingItems = context.getContentResolver()
                    .query(ItemStores.CONTENT_URI,
                            new String[]{ItemStores._ID},
                            "store_id = ? AND item_id = ?",
                            new String[]{String.valueOf(storeId),
                                    String.valueOf(itemId)}, null
                    );
        }
        if (existingItems != null && existingItems.getCount() > 0) {
            existingItems.moveToFirst();
            id = existingItems.getLong(0);
            existingItems.close();

            // update aisle and price:
            ContentValues values = new ContentValues(3);
            if (!TextUtils.isEmpty(price))
                values.put(ItemStores.PRICE, price);
            if (!TextUtils.isEmpty(aisle))
                values.put(ItemStores.AISLE, aisle);
            values.put(ItemStores.STOCKS_ITEM, stocksItem);
            try {
                Uri uri = Uri.withAppendedPath(ItemStores.CONTENT_URI,
                        String.valueOf(id));
                context.getContentResolver().update(uri, values, null, null);
                if (debug) {
                    Log.d(TAG, "updated itemstore: " + uri);
                }
            } catch (Exception e) {
                Log.e(TAG, "Update itemstore failed", e);
            }

        } else {
            if (existingItems != null) {
                existingItems.close();
            }
            // Add item to list:
            ContentValues values = new ContentValues(5);
            values.put(ItemStores.ITEM_ID, itemId);
            values.put(ItemStores.STORE_ID, storeId);
            values.put(ItemStores.PRICE, price);
            values.put(ItemStores.AISLE, aisle);
            values.put(ItemStores.STOCKS_ITEM, stocksItem);
            try {
                Uri uri = context.getContentResolver().insert(
                        ItemStores.CONTENT_URI, values);
                if (debug) {
                    Log.d(TAG, "Insert new entry in 'itemstores': " + uri);
                }
                id = Long.parseLong(uri.getPathSegments().get(1));
            } catch (Exception e) {
                Log.e(TAG, "insert into table 'itemstores' failed", e);
                id = -1;
            }
        }
        return id;
    }

    /**
     * Adds an item to a specific store and returns its id. If the item exists
     * already, the existing id is returned.
     *
     * @param itemId   The id of the new item.
     * @param storeId  The id of the store to which the item is added.
     * @param aisle    The aisle in which the item can be found at this store.
     *                 Can be null.
     * @param price    The price of the item at this store.
     *                 Can be null.
     * @param known_new true if the caller knows the item is not yet in the table for this store.
     * @return id of the "contains" table entry, or -1 if insert failed.
     */
    public static long addItemToStore(Context context, final long itemId,
                                      final long storeId, final String aisle, final String price, boolean known_new) {
        return addItemToStore(context, itemId, storeId, true, aisle, price, known_new);
    }

    /**
     * Returns the id of the default shopping list. Currently this is always 1.
     *
     * @return The id of the default shopping list.
     */
    public static long getDefaultList(Context context) {
        long id = 1;
        try {
            Cursor c = context.getContentResolver().query(
                    ActiveList.CONTENT_URI, ActiveList.PROJECTION, null, null,
                    null);
            if (c.getCount() > 0) {
                c.moveToFirst();
                id = c.getLong(0);
                c.close();
            }
        } catch (IllegalArgumentException e) {
            // The URI has not been defined.
            // The URI requires OI Shopping List 1.3.0 or higher.
            // Most probably we want to access OI Shopping List 1.2.6 or
            // earlier.
            Log.e(TAG, "ActiveList URI not supported", e);
        }
        return id;
    }

    public static Uri getListForItem(Context context, String itemId) {
        Cursor cursor = context.getContentResolver().query(
                Contains.CONTENT_URI, new String[]{Contains.LIST_ID},
                Contains.ITEM_ID + " = ?", new String[]{itemId},
                Contains.DEFAULT_SORT_ORDER);
        if (cursor != null) {
            Uri uri;
            if (cursor.moveToFirst()) {

                uri = Uri.withAppendedPath(ShoppingContract.Lists.CONTENT_URI,
                        cursor.getString(0));

            } else {
                uri = null;
            }
            cursor.close();
            return uri;
        } else {
            return null;
        }
    }

    public static void addTagToItem(Context context, long itemId, String newTag) {
        String allTags = "";
        Cursor existingTags = context.getContentResolver().query(
                Items.CONTENT_URI, new String[]{Items.TAGS}, "_id = ?",
                new String[]{String.valueOf(itemId)}, null);
        if (existingTags.getCount() > 0) {
            existingTags.moveToFirst();
            allTags = existingTags.getString(0);
            existingTags.close();
        }

        if (!TextUtils.isEmpty(allTags)) {
            if (allTags.equals(newTag))
                return;
            if (allTags.startsWith(newTag + ","))
                return;
            if (allTags.contains(", " + newTag))
                return;
            allTags = allTags + ", " + newTag;
        } else {
            allTags = newTag;
        }

        ContentValues values = new ContentValues(1);
        values.put(Items.TAGS, allTags);

        context.getContentResolver()
                .update(Uri.withAppendedPath(Items.CONTENT_URI,
                        String.valueOf(itemId)), values, null, null);
    }

    /**
     * Cleanly deletes an item from a list (from the Contains table), but the
     * item itself remains. Afterwards, either the item should be moved to
     * another list, or the item should be deleted. Deletion includes itemstores
     * and contains.
     *
     * @param context
     * @param itemId
     * @return 1 if the item got deleted, 0 otherwise.
     */
    public static int deleteItemFromList(Context context, String itemId,
                                         String listId) {
        // First delete all itemstores for item
        List<String> itemStoreIds = getItemStoreIdsForList(context, itemId,
                listId);
        for (String itemStoreId : itemStoreIds) {
            context.getContentResolver().delete(ItemStores.CONTENT_URI,
                    "itemstores._id = " + itemStoreId, null);
        }

        // Delete item from currentList by deleting contains row
        return context.getContentResolver().delete(
                Contains.CONTENT_URI, "item_id = ? and list_id = ?",
                new String[]{itemId, listId});
    }

    /**
     * Cleanly deletes an item from a particular list if it does not exist on
     * any other list. Deletion includes itemstores and the item itself.
     *
     * @param context
     * @param itemId
     * @return 1 if the item got deleted, 0 otherwise.
     */
    public static int deleteItem(Context context, String itemId, String listId) {
        deleteItemFromList(context, itemId, listId);

        int itemsDeleted = 0;
        if (!isItemContainedInOtherExistingList(context, itemId)) {
            // Delete the item itself if it is not contained in an existing list
            // anymore
            itemsDeleted = context.getContentResolver().delete(
                    Items.CONTENT_URI, "_id = ?", new String[]{itemId});
        }

        return itemsDeleted;
    }

    /**
     * Returns true if the item is contained in an existing list. Extra care is
     * taken because old contains could be left over from lists that do not
     * exist anymore.
     *
     * @param context
     * @param itemId
     * @return
     */
    private static boolean isItemContainedInOtherExistingList(Context context,
                                                              String itemId) {
        Cursor c = context.getContentResolver().query(Contains.CONTENT_URI,
                new String[]{Contains.LIST_ID}, Contains.ITEM_ID + " = ?",
                new String[]{itemId}, null);
        if (c != null) {
            while (c.moveToNext()) {
                // Item is contained in some list...
                String listId = c.getString(0);
                Cursor c2 = context.getContentResolver().query(
                        Lists.CONTENT_URI, new String[]{Lists._ID},
                        Lists._ID + " = ?", new String[]{listId}, null);
                if (c2 != null) {
                    if (c2.moveToNext()) {
                        // ... and that list exists
                        c2.close();
                        c.close();
                        return true;
                    }
                    c2.close();
                }

            }
            c.close();
        }
        return false;
    }

    /**
     * Cleanly deletes a store. Deletion includes itemstores and the store
     * itself.
     *
     * @param context
     * @param storeId
     * @return 1 if the store got deleted, 0 otherwise.
     */
    public static int deleteStore(Context context, String storeId) {
        // First delete all items for store
        context.getContentResolver().delete(ItemStores.CONTENT_URI,
                "store_id = " + storeId, null);

        // Then delete currently selected store
        return context.getContentResolver().delete(
                Stores.CONTENT_URI, "_id = " + storeId, null);
    }

    /**
     * Cleanly deletes a list. Deletion includes stores, itemstores, items, and
     * the list itself.
     *
     * @param context
     * @param listId
     * @return 1 if the list got deleted, 0 otherwise.
     */
    public static int deleteList(Context context, String listId) {
        // Delete all items
        List<String> itemIds = getItemIdsForList(context, listId);
        for (String itemId : itemIds) {
            deleteItem(context, itemId, listId);
        }

        // Delete all stores
        List<String> storeIds = getStoreIdsForList(context, listId);
        for (String storeId : storeIds) {
            deleteStore(context, storeId);
        }

        // Then delete currently selected list
        return context.getContentResolver().delete(
                Lists.CONTENT_URI, "_id = " + listId, null);
    }

    private static List<String> getItemStoreIdsForList(Context context,
                                                       String itemId, String listId) {
        // Get a cursor for all stores
        Cursor c = context.getContentResolver().query(
                ItemStores.CONTENT_URI.buildUpon().appendPath("item")
                        .appendPath(itemId).appendPath(listId).build(),
                new String[]{"itemstores._id"}, null, null, null
        );
        return getStringListAndCloseCursor(c, 0);
    }

    private static List<String> getItemIdsForList(Context context, String listId) {
        Cursor c = context.getContentResolver().query(Contains.CONTENT_URI,
                new String[]{Contains.ITEM_ID}, Contains.LIST_ID + " = ?",
                new String[]{listId}, null);
        return getStringListAndCloseCursor(c, 0);
    }

    private static List<String> getStoreIdsForList(Context context,
                                                   String listId) {
        Cursor c = context.getContentResolver().query(Stores.CONTENT_URI,
                new String[]{Stores._ID}, Stores.LIST_ID + " = ?",
                new String[]{listId}, null);
        return getStringListAndCloseCursor(c, 0);
    }

    private static List<String> getStringListAndCloseCursor(Cursor c, int index) {
        List<String> items = new LinkedList<String>();
        if (c != null) {
            while (c.moveToNext()) {
                String item = c.getString(index);
                items.add(item);
            }
            c.close();
        }
        return items;
    }

    private static String getListFilterStoreId(Context context, Uri list_uri) {
        String store_id = null;
        Cursor c = context.getContentResolver().query(list_uri,
                new String[]{Lists.STORE_FILTER}, null, null, null);
        if (c.getCount() > 0) {
            c.moveToFirst();
            store_id = c.getString(0);
            c.deactivate();
            c.close();
        }
        return store_id;
    }

    public static String getListFilterStoreName(Context context, Uri list_uri) {
        String filter = null;
        String store_id = getListFilterStoreId(context, list_uri);

        if (store_id != null && store_id.length() > 0) {
            Cursor c = context.getContentResolver().query(Stores.CONTENT_URI,
                    new String[]{Stores.NAME}, "_id = ?", new String[]{store_id}, null);
            if (c != null) {
                if (c.getCount() > 0) {
                    c.moveToFirst();
                    filter = c.getString(0);
                }
                c.deactivate();
                c.close();
            }
        }

        return filter;
    }

    public static String getListTagsFilter(Context context, Uri list_uri) {
        String filter = null;
        Cursor c = context.getContentResolver().query(list_uri,
                new String[]{Lists.TAGS_FILTER}, null, null, null);
        if (c.getCount() > 0) {
            c.moveToFirst();
            filter = c.getString(0);
            c.deactivate();
            c.close();

            if (filter != null && filter.length() == 0) {
                filter = null;
            }
        }
        return filter;
    }

    public static void addDefaultsToAddedItem(Context context, long list_id, long item_id) {
        Uri list_uri = Uri.withAppendedPath(ShoppingContract.Lists.CONTENT_URI,
                Long.toString(list_id));
        String tagsFilter = getListTagsFilter(context, list_uri);
        String storeId = getListFilterStoreId(context, list_uri);
        boolean hasTagsFilter = !TextUtils.isEmpty(tagsFilter);
        boolean hasStoreIdFilter = !TextUtils.isEmpty(storeId);

        if (hasStoreIdFilter) {
            addItemToStore(context, item_id, Long.parseLong(storeId), true, null, null, false);
        }

        if (hasTagsFilter) {
            addTagToItem(context, item_id, tagsFilter);
        }
    }

    public static String getListSortOrder(Context context, long list_id) {
        String sort = null;
        Uri list_uri = Uri.withAppendedPath(ShoppingContract.Lists.CONTENT_URI,
                Long.toString(list_id));
        Cursor c = context.getContentResolver().query(list_uri,
                new String[]{Lists.ITEMS_SORT}, null, null, null);
        if (c.getCount() > 0) {
            c.moveToFirst();
            sort = c.getString(0);
            c.deactivate();
            c.close();

            if (sort != null && sort.length() == 0) {
                sort = null;
            }
        }
        return sort;
    }

}

/* 
 * Copyright (C) 2007-2011 OpenIntents.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Definition for content provider related to shopping.
 */
public abstract class ShoppingContract {

    /**
     * TAG for logging.
     */
    private static final String TAG = "Shopping";
    public static final String ITEM_TYPE = "vnd.android.cursor.item/vnd.openintents.shopping.item";
    public static final String QUERY_ITEMS_WITH_STATE = "itemsWithState";
    public static final String AUTHORITY = "org.openintents.shopping";

    /**
     * Items that can be put into shopping lists.
     */
    public static final class Items implements BaseColumns {
        /**
         * The content:// style URL for this table.
         */
        public static final Uri CONTENT_URI = Uri
                .parse("content://org.openintents.shopping/items");

        /**
         * The default sort order for this table.
         */
        public static final String DEFAULT_SORT_ORDER = "modified ASC";

        /**
         * The name of the item.
         * <p/>
         * Type: TEXT
         * </P>
         */
        public static final String NAME = "name";

        /**
         * An image of the item (uri).
         * <p/>
         * Type: TEXT
         * </P>
         */
        public static final String IMAGE = "image";

        /**
         * A price for the item (in cent)
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String PRICE = "price";

        /**
         * Units for the item
         * <p/>
         * Type: VARCHAR
         * </P>
         */
        public static final String UNITS = "units";

        /**
         * Tags for the item
         * <p/>
         * Type: VARCHAR
         * </P>
         */
        public static final String TAGS = "tags";

        /**
         * A barcode (EAN or QR)
         * <p/>
         * Type: VARCHAR
         * </P>
         */
        public static final String BARCODE = "barcode";

        /**
         * a location where to find it, as geo:lat,long uri
         * <p/>
         * Type: VARCHAR
         * </P>
         */
        public static final String LOCATION = "location";

        /**
         * text of a note about the item
         * <p/>
         * Type: VARCHAR
         * </P>
         */
        public static final String NOTE = "note";

        /**
         * The timestamp for when the item was created.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String CREATED_DATE = "created";

        /**
         * The timestamp for when the item was last modified.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String MODIFIED_DATE = "modified";

        /**
         * The timestamp for when the item was last accessed.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String ACCESSED_DATE = "accessed";

        /**
         * The timestamp for when the item is due.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String DUE_DATE = "due";

        /**
         * Generic projection map.
         */
        public static final String[] PROJECTION = {_ID, NAME, IMAGE, PRICE,
                CREATED_DATE, MODIFIED_DATE, ACCESSED_DATE, UNITS};

        public static final String[] PROJECTION_TO_COPY = {
                NAME, IMAGE, PRICE, UNITS, TAGS, BARCODE, LOCATION, NOTE
        };

        /**
         * Offset in PROJECTION array.
         */
        public static final int PROJECTION_ID = 0;
        public static final int PROJECTION_NAME = 1;
        public static final int PROJECTION_IMAGE = 2;
        public static final int PROJECTION_PRICE = 3;
        public static final int PROJECTION_CREATED_DATE = 4;
        public static final int PROJECTION_MODIFIED_DATE = 5;
        public static final int PROJECTION_ACCESSED_DATE = 6;
        public static final int PROJECTION_UNITS = 7;
    }

    /**
     * Shopping lists that can contain items.
     */
    public static final class Lists implements BaseColumns {
        /**
         * The content:// style URL for this table.
         */
        public static final Uri CONTENT_URI = Uri
                .parse("content://org.openintents.shopping/lists");

        /**
         * The default sort order for this table.
         */
        public static final String DEFAULT_SORT_ORDER
                // = "modified DESC";
                = "modified ASC";

        /**
         * The name of the list.
         * <p/>
         * Type: TEXT
         * </P>
         */
        public static final String NAME = "name";

        /**
         * An image of the list (uri).
         * <p/>
         * Type: TEXT
         * </P>
         */
        public static final String IMAGE = "image";

        /**
         * The timestamp for when the item was created.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String CREATED_DATE = "created";

        /**
         * The timestamp for when the item was last modified.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String MODIFIED_DATE = "modified";

        /**
         * The timestamp for when the item was last accessed.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String ACCESSED_DATE = "accessed";

        /**
         * The name of the shared shopping list that should be worldwide unique.
         * <p/>
         * It is formed of the current user's email address and a unique suffix.
         * <p/>
         * <p/>
         * Type: TEXT
         * </P>
         * Available since release 0.1.6.
         */
        public static final String SHARE_NAME = "share_name";

        /**
         * The comma separated list of contacts with whom this list is shared.
         * <p/>
         * <p/>
         * Type: TEXT
         * </P>
         * Available since release 0.1.6.
         */
        public static final String SHARE_CONTACTS = "share_contacts";

        /**
         * Name of background image.
         * <p/>
         * <p/>
         * Type: TEXT
         * </P>
         * Available since release 0.1.6.
         */
        public static final String SKIN_BACKGROUND = "skin_background";

        /**
         * Name of font in list.
         * <p/>
         * <p/>
         * Type: TEXT
         * </P>
         * Available since release 0.1.6.
         */
        public static final String SKIN_FONT = "skin_font";

        /**
         * Color of text in list.
         * <p/>
         * <p/>
         * Type: INTEGER (long)
         * </P>
         * Available since release 0.1.6.
         */
        public static final String SKIN_COLOR = "skin_color";

        /**
         * Color of strikethrough text in list.
         * <p/>
         * <p/>
         * Type: INTEGER (long)
         * </P>
         * Available since release 0.1.6.
         */
        public static final String SKIN_COLOR_STRIKETHROUGH = "skin_color_strikethrough";

        /**
         * ID of store to filter in list, -1 to show all stores.
         * <p/>
         * <p/>
         * Type: INTEGER (long)
         * </P>
         * Available since release 1.6.
         */
        public static final String STORE_FILTER = "store_filter";

        /**
         * Tag text to filter in list.
         * <p/>
         * <p/>
         * Type: TEXT
         * </P>
         * Available since release 1.6.
         */
        public static final String TAGS_FILTER = "tags_filter";

        public static final String[] SORT_ORDERS = new String[]{
                "UPPER(" + NAME + ") ASC",
                "UPPER(" + NAME + ") DESC",
                CREATED_DATE + " DESC",
                CREATED_DATE + " ASC"
        };

        /**
         * ID of sort order to use for this list, null to follow prefs.
         * <p/>
         * <p/>
         * Type: INTEGER (long)
         * </P>
         * Available since release 2.0.
         */
        public static final String ITEMS_SORT = "items_sort";

    }

    /**
     * Information which list contains which items/lists/(recipes)
     */
    public static final class Contains implements BaseColumns {
        /**
         * The content:// style URL for this table.
         */
        public static final Uri CONTENT_URI = Uri
                .parse("content://org.openintents.shopping/contains");

        /**
         * The default sort order for this table.
         */
        public static final String DEFAULT_SORT_ORDER = "modified DESC";

        /**
         * The id of the item.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String ITEM_ID = "item_id";

        /**
         * The id of the list that contains item_id.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String LIST_ID = "list_id";

        /**
         * Quantity specifier.
         * <p/>
         * Type: TEXT
         * </P>
         */
        public static final String QUANTITY = "quantity";

        /**
         * Priority specifier.
         * <p/>
         * Type: INTEGER (long) 1-5
         * </P>
         */
        public static final String PRIORITY = "priority";

        /**
         * Status: WANT_TO_BUY or BOUGHT.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String STATUS = "status";

        /**
         * The timestamp for when the item was created.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String CREATED_DATE = "created";

        /**
         * The timestamp for when the item was last modified.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String MODIFIED_DATE = "modified";

        /**
         * The timestamp for when the item was last accessed.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String ACCESSED_DATE = "accessed";

        /**
         * Name of person who inserted the item.
         * <p/>
         * <p/>
         * Type: TEXT
         * </P>
         * Available since release 0.1.6.
         */
        public static final String SHARE_CREATED_BY = "share_created_by";

        /**
         * Name of person who changed status of the item, for example mark it as
         * bought.
         * <p/>
         * <p/>
         * Type: TEXT
         * </P>
         * Available since release 0.1.6.
         */
        public static final String SHARE_MODIFIED_BY = "share_modified_by";

        /**
         * sort key with in the list
         * <p/>
         * Type: INTEGER
         * </P>
         */
        public static final String SORT_KEY = "sort_key";
        /**
         * Support sort orders. The "sort order" in the preferences is an index
         * into this array.
         */
        public static final String[] SORT_ORDERS = {
                // unchecked first, alphabetical
                "contains.status ASC, items.name COLLATE NOCASE ASC",

                "items.name COLLATE NOCASE ASC",

                "contains.modified DESC",

                "contains.modified ASC",

                // sort by tags, but put empty tags last.
                "(items.tags IS NULL or items.tags = '') ASC, items.tags COLLATE NOCASE ASC, items.name COLLATE NOCASE ASC",

                "items.price DESC, items.name COLLATE NOCASE ASC",

                // unchecked first, tags alphabetical, but put empty tags last.
                "contains.status ASC, (items.tags IS NULL or items.tags = '') ASC, items.tags COLLATE NOCASE ASC, items.name COLLATE NOCASE ASC",

                // unchecked first, priority, alphabetical
                "contains.status ASC, contains.priority ASC, items.name COLLATE NOCASE ASC",

                // unchecked first, priority, tags alphabetical, but put empty
                // tags last.
                "contains.status ASC, contains.priority ASC, (items.tags IS NULL or items.tags = '') ASC, items.tags COLLATE NOCASE ASC, items.name COLLATE NOCASE ASC",

                // priority, tags alphabetical, but put empty tags last.
                "contains.priority ASC, (items.tags IS NULL or items.tags = '') ASC, items.tags COLLATE NOCASE ASC, items.name COLLATE NOCASE ASC",
        };

        /**
         * For each of the above sort orders, does it depend on status?
         */
        public static final boolean[] StatusAffectsSortOrder = {
                true, false, false, false, false, false, true, true, true, false
        };

        public static final String[] PROJECTION_TO_COPY = {
                LIST_ID, QUANTITY, PRIORITY, STATUS
        };
    }

    /**
     * Combined table of contents, items, and lists.
     */
    public static final class ContainsFull implements BaseColumns {

        /**
         * The content:// style URL for this table.
         */
        public static final Uri CONTENT_URI = Uri
                .parse("content://org.openintents.shopping/containsfull");

        /**
         * The default sort order for this table.
         */
        public static final String DEFAULT_SORT_ORDER
                // = "contains.modified DESC";
                = "contains.modified ASC";

        // Elements from Contains

        /**
         * The id of the item.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String ITEM_ID = "item_id";

        /**
         * The id of the list that contains item_id.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String LIST_ID = "list_id";

        /**
         * Quantity specifier.
         * <p/>
         * Type: TEXT
         * </P>
         */
        public static final String QUANTITY = "quantity";

        /**
         * Priority specifier.
         * <p/>
         * Type: INTEGER (long) 1-5
         * </P>
         */
        public static final String PRIORITY = "priority";

        /**
         * Status: WANT_TO_BUY or BOUGHT.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String STATUS = "status";

        /**
         * The timestamp for when the item was created.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String CREATED_DATE = "created";

        /**
         * The timestamp for when the item was last modified.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String MODIFIED_DATE = "modified";

        /**
         * The timestamp for when the item was last accessed.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String ACCESSED_DATE = "accessed";

        /**
         * Name of person who inserted the item.
         * <p/>
         * <p/>
         * Type: TEXT
         * </P>
         * Available since release 0.1.6.
         */
        public static final String SHARE_CREATED_BY = "share_created_by";

        /**
         * Name of person who crossed out the item.
         * <p/>
         * <p/>
         * Type: TEXT
         * </P>
         * Available since release 0.1.6.
         */
        public static final String SHARE_MODIFIED_BY = "share_modified_by";

        // Elements from Items

        /**
         * The name of the item.
         * <p/>
         * Type: TEXT
         * </P>
         */
        public static final String ITEM_NAME = "item_name";

        /**
         * An image of the item (uri).
         * <p/>
         * Type: TEXT
         * </P>
         */
        public static final String ITEM_IMAGE = "item_image";

        /**
         * A price of the item (in cent).
         * <p/>
         * Type: INTEGER
         * </P>
         */
        public static final String ITEM_PRICE = "item_price";

        /**
         * Units of the item.
         * <p/>
         * Type: VARCHAR
         * </P>
         */
        public static final String ITEM_UNITS = "item_units";

        /**
         * tags of the item.
         * <p/>
         * Type: VARCHAR
         * </P>
         */
        public static final String ITEM_TAGS = "item_tags";

        // Elements from Lists

        /**
         * The name of the list.
         * <p/>
         * Type: TEXT
         * </P>
         */
        public static final String LIST_NAME = "list_name";

        /**
         * An image of the list (uri).
         * <p/>
         * Type: TEXT
         * </P>
         */
        public static final String LIST_IMAGE = "list_image";

        /**
         * A barcode (EAN or QR)
         * <p/>
         * Type: VARCHAR
         * </P>
         */
        public static final String BARCODE = "barcode";

        /**
         * a location where to find it, as geo:lat,long uri
         * <p/>
         * Type: VARCHAR
         * </P>
         */
        public static final String LOCATION = "location";

        /**
         * The timestamp for when the item is due.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String DUE_DATE = "due";

        /**
         * Whether the item has a note.
         * <p/>
         * Type: INTEGER
         * </P>
         */
        public static final String ITEM_HAS_NOTE = "item_has_note";
    }

    /**
     * Status of "contains" element.
     */
    public static final class Status {

        /**
         * Want to buy this item.
         */
        public static final long WANT_TO_BUY = 1;

        /**
         * Have bought this item.
         */
        public static final long BOUGHT = 2;

        /**
         * Have removed it from the list. Won't be deleted, in oder to keep
         * reference for later suggestions.
         */
        public static final long REMOVED_FROM_LIST = 3;

        /**
         * Checks whether a status is a valid possibility.
         *
         * @param s status to be checked.
         * @return true if status is a valid possibility.
         */
        public static boolean isValid(final long s) {
            return s == WANT_TO_BUY || s == BOUGHT || s == REMOVED_FROM_LIST;
        }
    }

    /**
     * Stores which might be able to sell items.
     */
    public static final class Stores implements BaseColumns {
        /**
         * The content:// style URL for this table.
         */
        public static final Uri CONTENT_URI = Uri
                .parse("content://org.openintents.shopping/stores");

        /**
         * The default sort order for this table.
         */
        public static final String DEFAULT_SORT_ORDER = "name ASC";

        /**
         * The name of the item.
         * <p/>
         * Type: TEXT
         * </P>
         */
        public static final String NAME = "name";

        /**
         * The id of the list associated with this store.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String LIST_ID = "list_id";

        /**
         * The timestamp for when the store was created.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String CREATED_DATE = "created";

        /**
         * The timestamp for when the store was last modified.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String MODIFIED_DATE = "modified";

        public static final Uri QUERY_BY_LIST_URI = Uri
                .parse("content://org.openintents.shopping/liststores");

    }

    /**
     * Items that can be put into shopping lists.
     */
    public static final class ItemStores implements BaseColumns {
        /**
         * The content:// style URL for this table.
         */
        public static final Uri CONTENT_URI = Uri
                .parse("content://org.openintents.shopping/itemstores");

        /**
         * The default sort order for this table.
         */
        public static final String DEFAULT_SORT_ORDER = "item_id ASC";

        /**
         * The timestamp for when the itemstore record was created.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String CREATED_DATE = "created";

        /**
         * The timestamp for when the itemstore record was last modified.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String MODIFIED_DATE = "modified";

        /**
         * The id of the item.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String ITEM_ID = "item_id";

        /**
         * The id of one store that contains item.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String STORE_ID = "store_id";

        /**
         * The aisle which contains item item_id at store store_id.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String AISLE = "aisle";

        /**
         * The price of item item_id at store store_id.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String PRICE = "price";

        /**
         * Whether we expect to find item item_id at store store_id.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String STOCKS_ITEM = "stocks_item";
    }

    /**
     * Completion table for the Units field of Items.
     */
    public static final class Units implements BaseColumns {
        /**
         * The content:// style URL for this table.
         */
        public static final Uri CONTENT_URI = Uri
                .parse("content://org.openintents.shopping/units");

        /**
         * The default sort order for this table.
         */
        public static final String DEFAULT_SORT_ORDER = "name ASC";

        /**
         * The name of the units.
         * <p/>
         * Type: TEXT
         * </P>
         */
        public static final String NAME = "name";

        /**
         * The name of the units when quantity == 1, if different from
         * general/plural unit name.
         * <p/>
         * Type: TEXT
         * </P>
         */
        public static final String SINGULAR = "singular";

        /**
         * The timestamp for when the unit was created.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String CREATED_DATE = "created";

        /**
         * The timestamp for when the unit was last modified.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String MODIFIED_DATE = "modified";
    }

    public static final class Notes implements BaseColumns {

        // unlike other tables, this one does not correspond
        // to its own sql table... it just defines a projection of the items
        // table.

        // This class cannot be instantiated
        private Notes() {
        }

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri
                .parse("content://org.openintents.shopping/notes");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of notes.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.openintents.notepad.note";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single
         * note.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.openintents.notepad.note";

        /**
         * The title of the note
         * <p/>
         * Type: TEXT
         * </P>
         */
        public static final String TITLE = "title";

        /**
         * The note itself
         * <p/>
         * Type: TEXT
         * </P>
         */
        public static final String NOTE = "note";

        /**
         * The timestamp for when the note was created
         * <p/>
         * Type: INTEGER (long from System.curentTimeMillis())
         * </P>
         */
        public static final String CREATED_DATE = "created";

        /**
         * The timestamp for when the note was last modified
         * <p/>
         * Type: INTEGER (long from System.curentTimeMillis())
         * </P>
         */
        public static final String MODIFIED_DATE = "modified";

        /**
         * Tags associated with a note. Multiple tags are separated by commas.
         * <p/>
         * Type: TEXT
         * </P>
         *
         * @since 1.1.0
         */
        public static final String TAGS = "tags";

        /**
         * Whether the note is encrypted. 0 = not encrypted. 1 = encrypted.
         * <p/>
         * Type: INTEGER
         * </P>
         *
         * @since 1.1.0
         */
        public static final String ENCRYPTED = "encrypted";

        /**
         * A theme URI.
         * <p/>
         * Type: TEXT
         * </P>
         *
         * @since 1.1.0
         */
        public static final String THEME = "theme";
    }

    /**
     * Virtual table containing the id of the active list.
     */
    public static final class ActiveList implements BaseColumns {
        /**
         * The content:// style URL for this table.
         */
        public static final Uri CONTENT_URI = Uri
                .parse("content://org.openintents.shopping/lists/active");

        /**
         * Generic projection map.
         */
        public static final String[] PROJECTION = {_ID};
    }

    /**
     * Virtual table containing subtotals of items by status and priority.
     */
    public static final class Subtotals {
        /**
         * The content:// style URL for this table.
         */
        public static final Uri CONTENT_URI = Uri
                .parse("content://org.openintents.shopping/subtotals");

        /**
         * Priority
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String PRIORITY = "priority";

        /**
         * Status
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String STATUS = "status";

        /**
         * Number of items subtotaled in this cell.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String COUNT = "count";

        /**
         * Subtotal.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String SUBTOTAL = "subtotal";

        /**
         * Generic projection map.
         */
        public static final String[] PROJECTION = {PRIORITY, STATUS, COUNT,
                SUBTOTAL};
        // index values for use with cursors using the default projection
        public static final int PRIORITY_INDEX = 0;
        public static final int STATUS_INDEX = 1;
        public static final int COUNT_INDEX = 2;
        public static final int SUBTOTAL_INDEX = 3;
    }
}

/*
 * Copyright (C) 2007-2008 OpenIntents.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;

/**
 * Provides OpenIntents action and category specifiers.
 * <p/>
 * These specifiers extend the standard Android specifiers.
 */
public abstract class OpenIntents {
    // !! CAREFUL !!
    // If you change any of the string definitions, you have
    // to change it in all Manifests that use them as well!

    // -----------------------------------------------
    // Tags
    // -----------------------------------------------
    /**
     * Identifier for tag action.
     */
    public static final String TAG_ACTION = "org.openintents.action.TAG";

    // -----------------------------------------------
    // Shopping
    // -----------------------------------------------
    /**
     * Change share settings for an item.
     * <p/>
     * Currently implemented for shopping list.
     */
    public static final String SET_SHARE_SETTINGS_ACTION = "org.openintents.action.SET_SHARE_SETTINGS";

    /**
     * Change theme settings or appearance for an item.
     * <p/>
     * Currently implemented for shopping list.
     */
    public static final String SET_THEME_SETTINGS_ACTION = "org.openintents.action.SET_THEME_SETTINGS";

    /**
     * Broadcasts updated information about an item or a list.
     * <p/>
     * If the list does not exist on one of the recipients, it is created. If
     * the item does not exist, it is created. This action is intended to be
     * received through GTalk or XMPP.
     */
    public static final String SHARE_UPDATE_ACTION = "org.openintents.action.SHARE_UPDATE";

    /**
     * Inserts an item into a shared shopping list.
     * <p/>
     * This action is intended to be received through GTalk or XMPP.
     */
    public static final String SHARE_INSERT_ACTION = "org.openintents.action.SHARE_INSERT";

    /**
     * Notifies a list that the content changed.
     */
    public static final String REFRESH_ACTION = "org.openintents.action.REFRESH";

    /**
     * Adds a location alert to a specific item.
     * <p/>
     * Currently implemented for shopping list.
     */
    public static final String ADD_LOCATION_ALERT_ACTION = "org.openintents.action.ADD_LOCATION_ALERT";

    public static final String LOCATION_ALERT_DISPATCH = "org.openintents.action.LOCATION_ALERT_DISPATCH";
    public static final String DATE_TIME_ALERT_DISPATCH = "org.openintents.action.DATE_TIME_ALERT_DISPATCH";
    public static final String SERVICE_MANAGER = "org.openintents.action.SERVICE_MANAGER";

    // -----------------------------------------------
    // Categories
    // -----------------------------------------------
    /**
     * Main category specifier.
     * <p/>
     * Applications placed into this category in the AndroidManifest.xml file
     * are displayed in the main view of OpenIntents.
     */
    public static final String MAIN_CATEGORY = "org.openintents.category.MAIN";

    /**
     * Settings category specifier.
     * <p/>
     * Applications placed into this category in the AndroidManifest.xml file
     * are displayed in the settings tab of OpenIntents.
     */
    public static final String SETTINGS_CATEGORY = "org.openintents.category.SETTINGS";

    /**
     * identifier for adding generic alerts action.
     *
     * @deprecated will be removed by 0.2.1 latest
     */
    public static final String ADD_GENERIC_ALERT = "org.openintents.action.ADD_GENERIC_ALERT";
    /**
     * identifier for adding generic alerts action.
     *
     * @deprecated will be removed by 0.2.1 latest
     */
    public static final String EDIT_GENERIC_ALERT = "org.openintents.action.EDIT_GENERIC_ALERT";
    public static final String PREFERENCES_INIT_DEFAULT_VALUES = "InitView";
    public static final String PREFERENCES_DONT_SHOW_INIT_DEFAULT_VALUES = "dontShowInitDefaultValues";

    /**
     * shows an English message if open intents is not installed, finishes the
     * activity after user clicked "ok".
     *
     * @param activity
     */
    public static void requiresOpenIntents(final Activity activity) {
        try {
            activity.getPackageManager().getPackageInfo("org.openintents", 0);
        } catch (NameNotFoundException e) {
            new AlertDialog.Builder(activity)
                    .setTitle("Warning")
                    .setMessage(
                            "Requires OpenIntents! Please install the open intents application from www.openintents.org first.")
                    .setPositiveButton("ok", new OnClickListener() {

                        public void onClick(DialogInterface arg0, int arg1) {
                            activity.finish();
                        }

                    }).show();

        }
    }

    /**
     * calls the InitDefaultValues activity (unless unchecked).
     */
    public static void suggestInitDefaultValues(final Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(
                PREFERENCES_INIT_DEFAULT_VALUES, 0);
        boolean b = prefs.getBoolean(PREFERENCES_DONT_SHOW_INIT_DEFAULT_VALUES,
                false);
        if (b == false) {
            // User does not want to see intro screen again.
            Intent intent = new Intent();
            intent.setClassName("org.openintents",
                    "org.openintents.main.InitView");
            activity.startActivity(intent);

        }

    }
}

/*
 * Copyright (C) 2008 OpenIntents.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import android.os.Bundle;

import org.openintents.shopping.R;

public class PreferenceActivity extends android.preference.PreferenceActivity {
    public static final String PREFS_SHOPPINGLIST_ENCODING = "shopping_encoding";
    public static final String PREFS_SHOPPINGLIST_USE_CUSTOM_ENCODING = "shoppinglist_use_custom_encoding";
    public static final String PREFS_SHOPPINGLIST_FILENAME = "shoppinglist_filename";
    public static final String PREFS_SHOPPINGLIST_FORMAT = "shoppinglist_format";
    public static final String PREFS_ASK_IF_FILE_EXISTS = "ask_if_file_exists";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences_convertcsv);
    }
}

/*
 * Copyright (C) 2008 OpenIntents.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import org.openintents.convertcsv.common.ConvertCsvBaseActivity;
import org.openintents.convertcsv.opencsv.CSVWriter;
import org.openintents.shopping.R;
import org.openintents.shopping.library.provider.ShoppingContract;
import org.openintents.shopping.library.provider.ShoppingContract.ContainsFull;
import org.openintents.shopping.library.provider.ShoppingContract.Lists;
import org.openintents.shopping.library.provider.ShoppingContract.Status;

import java.io.IOException;
import java.io.Writer;

public class ExportCsv {

    public static final String[] PROJECTION_LISTS = new String[]{Lists._ID,
            Lists.NAME, Lists.IMAGE, Lists.SHARE_NAME, Lists.SHARE_CONTACTS,
            Lists.SKIN_BACKGROUND};

    public static final String[] PROJECTION_CONTAINS_FULL = new String[]{
            ContainsFull._ID, ContainsFull.ITEM_NAME, ContainsFull.ITEM_IMAGE,
            ContainsFull.STATUS, ContainsFull.ITEM_ID, ContainsFull.LIST_ID,
            ContainsFull.ITEM_TAGS,
            ContainsFull.SHARE_CREATED_BY, ContainsFull.SHARE_MODIFIED_BY};
    public static final String[] PROJECTION_CONTAINS_FULL_HANDY_SHOPPER = new String[]{
            ContainsFull._ID, ContainsFull.ITEM_NAME, ContainsFull.ITEM_IMAGE,
            ContainsFull.QUANTITY, ContainsFull.PRIORITY,
            ContainsFull.STATUS, ContainsFull.ITEM_ID, ContainsFull.LIST_ID,
            ContainsFull.ITEM_TAGS,
            ContainsFull.ITEM_PRICE, ContainsFull.ITEM_UNITS,
            ContainsFull.SHARE_CREATED_BY, ContainsFull.SHARE_MODIFIED_BY};
    Context mContext;
    String handyShopperColumns = "Need,Priority,Description,CustomText,Quantity,Units,Price,Aisle,Date,Category,Stores,PerStoreInfo,EntryOrder,Coupon,Tax,Tax2,AutoDelete,Private,Note,Alarm,AlarmMidi,Icon,AutoOrder";

    public ExportCsv(Context context) {
        mContext = context;
    }

    /**
     * @param dos
     * @throws IOException
     */
    public void exportCsv(Writer writer) throws IOException {

        CSVWriter csvwriter = new CSVWriter(writer);

        csvwriter.write(mContext.getString(R.string.header_subject));
        csvwriter.write(mContext.getString(R.string.header_percent_complete));
        csvwriter.write(mContext.getString(R.string.header_categories));
        csvwriter.write(mContext.getString(R.string.header_tags));
        csvwriter.writeNewline();

        Cursor c = mContext.getContentResolver().query(
                Lists.CONTENT_URI, PROJECTION_LISTS, null,
                null, Lists.DEFAULT_SORT_ORDER);

        if (c != null) {

            while (c.moveToNext()) {

                String listname = c.getString(c
                        .getColumnIndexOrThrow(Lists.NAME));
                long id = c
                        .getLong(c.getColumnIndexOrThrow(Lists._ID));

                // Log.i(ConvertCsvActivity.TAG, "List: " + listname);

                Cursor ci = mContext.getContentResolver().query(
                        ContainsFull.CONTENT_URI,
                        PROJECTION_CONTAINS_FULL,
                        ContainsFull.LIST_ID + " = ?",
                        new String[]{"" + id},
                        ContainsFull.DEFAULT_SORT_ORDER);

                if (ci != null) {
                    int itemcount = ci.getCount();
                    ConvertCsvBaseActivity.dispatchSetMaxProgress(itemcount);
                    int progress = 0;

                    while (ci.moveToNext()) {
                        ConvertCsvBaseActivity
                                .dispatchConversionProgress(progress++);
                        String itemname = ci
                                .getString(ci
                                        .getColumnIndexOrThrow(ContainsFull.ITEM_NAME));
                        int status = ci
                                .getInt(ci
                                        .getColumnIndexOrThrow(ContainsFull.STATUS));
                        int percentage = (status == Status.BOUGHT) ? 1
                                : 0;
                        String tags = ci
                                .getString(ci
                                        .getColumnIndexOrThrow(ContainsFull.ITEM_TAGS));
                        csvwriter.write(itemname);
                        csvwriter.write(percentage);
                        csvwriter.write(listname);
                        csvwriter.write(tags);
                        csvwriter.writeNewline();
                    }
                }
            }
        }

        csvwriter.close();
    }

    /**
     * @param dos
     * @throws IOException
     */
    public void exportHandyShopperCsv(Writer writer, long listId) throws IOException {

        CSVWriter csvwriter = new CSVWriter(writer);
        csvwriter.setLineEnd("\r\n");
        csvwriter.setQuoteCharacter(CSVWriter.NO_QUOTE_CHARACTER);

        csvwriter.write(handyShopperColumns);
        csvwriter.writeNewline();

        csvwriter.setQuoteCharacter(CSVWriter.DEFAULT_QUOTE_CHARACTER);


        Cursor ci = mContext.getContentResolver().query(
                ContainsFull.CONTENT_URI,
                PROJECTION_CONTAINS_FULL_HANDY_SHOPPER,
                ContainsFull.LIST_ID + " = ?",
                new String[]{"" + listId},
                ContainsFull.DEFAULT_SORT_ORDER);

        if (ci != null) {
            int itemcount = ci.getCount();
            ConvertCsvBaseActivity.dispatchSetMaxProgress(itemcount);
            int progress = 0;

            while (ci.moveToNext()) {
                ConvertCsvBaseActivity
                        .dispatchConversionProgress(progress++);
                String itemname = ci.getString(ci.getColumnIndexOrThrow(ContainsFull.ITEM_NAME));
                int status = ci.getInt(ci.getColumnIndexOrThrow(ContainsFull.STATUS));
                String tags = ci.getString(ci.getColumnIndexOrThrow(ContainsFull.ITEM_TAGS));
                String priority = ci.getString(ci.getColumnIndex(ContainsFull.PRIORITY));
                String quantity = ci.getString(ci.getColumnIndex(ContainsFull.QUANTITY));
                long price = ci.getLong(ci.getColumnIndex(ContainsFull.ITEM_PRICE));
                String pricestring = "";
                if (price != 0) {
                    pricestring += (double) price / 100.d;
                }
                String unit = ci.getString(ci.getColumnIndex(ContainsFull.ITEM_UNITS));
                long itemId = ci.getInt(ci.getColumnIndex(ContainsFull.ITEM_ID));

                String statusText = getHandyShopperStatusText(status);

                // Split off first tag.
                if (tags == null) {
                    tags = "";
                }
                int t = tags.indexOf(",");
                String firstTag = "";
                String otherTags = "";
                if (t >= 0) {
                    firstTag = tags.substring(0, t); // -> Category
                    otherTags = tags.substring(t + 1); // -> CustomText
                } else {
                    firstTag = tags; // -> Category
                    otherTags = ""; // -> CustomText
                }

                // Retrieve note:
                String note = getHandyShopperNote(itemId);
                if (note != null) {
                    // Replace LF by CR+LF
                    note = note.replace("\n", "\r\n");
                }

                String stores = getHandyShopperStores(itemId);
                String perStoreInfo = getHandyShopperPerStoreInfo(itemId);


                csvwriter.writeValue(statusText); // 0 Need
                csvwriter.writeValue(priority); // 1 Priority
                csvwriter.writeValue(itemname); // 2 Description
                csvwriter.writeValue(otherTags); // 3 CustomText
                csvwriter.writeValue(quantity); // 4 Quantity
                csvwriter.writeValue(unit); // 5 Units
                csvwriter.writeValue(pricestring); // 6 Price
                csvwriter.writeValue(""); // 7 Aisle
                csvwriter.writeValue(""); // 8 Date
                csvwriter.writeValue(firstTag); // 9 Category
                csvwriter.writeValue(stores); // 10 Stores
                csvwriter.writeValue(perStoreInfo); // 11 PerStoreInfo
                csvwriter.writeValue(""); // 12 EntryOrder
                csvwriter.writeValue(""); // 13 Coupon
                csvwriter.writeValue(""); // 14 Tax
                csvwriter.writeValue(""); // 15 Tax2
                csvwriter.writeValue(""); // 16 AutoDelete
                csvwriter.writeValue(""); // 17 Private
                csvwriter.write(note); // 18 Note (use quotes)
                csvwriter.writeValue(""); // 19 Alarm
                csvwriter.writeValue("0"); // 20 AlarmMidi
                csvwriter.writeValue("0"); // 21 Icon
                csvwriter.writeValue(""); // 22 AutoOrder

                csvwriter.writeNewline();
            }
            ci.close();
        }

        csvwriter.close();
    }

    String getHandyShopperStatusText(int status) {
        String statusText = "";
        if (status == Status.WANT_TO_BUY) {
            statusText = "x";
        } else if (status == Status.REMOVED_FROM_LIST) {
            statusText = "have";
        } else if (status == Status.BOUGHT) {
            statusText = "";
        }
        return statusText;
    }

    private String getHandyShopperNote(long itemId) {
        Uri uri = ContentUris.withAppendedId(ShoppingContract.Items.CONTENT_URI, itemId);

        String note = "";
        Cursor c1 = mContext.getContentResolver().query(uri,
                new String[]{ShoppingContract.Items.NOTE}, null, null, null);
        if (c1 != null) {
            if (c1.moveToFirst()) {
                note = c1.getString(0);
            }
            c1.close();
        }
        return note;
    }

    private String getHandyShopperStores(long itemId) {
        String stores = "";

        Cursor c1 = mContext.getContentResolver().query(
                ShoppingContract.ItemStores.CONTENT_URI,
                new String[]{ShoppingContract.ItemStores.ITEM_ID,
                        ShoppingContract.ItemStores.STORE_ID},
                ShoppingContract.ItemStores.ITEM_ID + " = ?",
                new String[]{"" + itemId}, null);
        if (c1 != null) {
            while (c1.moveToNext()) {
                long storeId = c1.getLong(c1.getColumnIndexOrThrow(ShoppingContract.ItemStores.STORE_ID));
                Uri uri2 = ContentUris.withAppendedId(ShoppingContract.Stores.CONTENT_URI, storeId);
                Cursor c2 = mContext.getContentResolver().query(uri2,
                        new String[]{ShoppingContract.Stores.NAME}, null, null, null);
                if (c2 != null) {
                    if (c2.moveToFirst()) {
                        String storeName = c2.getString(c2.getColumnIndexOrThrow(ShoppingContract.Stores.NAME));
                        if (stores.equals("")) {
                            stores = storeName;
                        } else {
                            stores += ";" + storeName;
                        }
                    }
                    c2.close();
                }
            }
            c1.close();
        }
        return stores;
    }

    // Deal with per-store aisles and prices from column 11.
    // example value for column 11:    Big Y=/0.50;BJ's=11/0.42
    private String getHandyShopperPerStoreInfo(long itemId) {
        String perStoreInfo = "";

        Cursor c1 = mContext.getContentResolver().query(
                ShoppingContract.ItemStores.CONTENT_URI,
                new String[]{ShoppingContract.ItemStores.ITEM_ID,
                        ShoppingContract.ItemStores.STORE_ID,
                        ShoppingContract.ItemStores.AISLE,
                        ShoppingContract.ItemStores.PRICE},
                ShoppingContract.ItemStores.ITEM_ID + " = ?",
                new String[]{"" + itemId}, null);
        if (c1 != null) {
            while (c1.moveToNext()) {
                long storeId = c1.getLong(c1.getColumnIndexOrThrow(ShoppingContract.ItemStores.STORE_ID));
                String aisle = c1.getString(c1.getColumnIndexOrThrow(ShoppingContract.ItemStores.AISLE));
                long price = c1.getLong(c1.getColumnIndexOrThrow(ShoppingContract.ItemStores.PRICE));
                String pricestring = "" + (double) price / 100.d;

                Uri uri2 = ContentUris.withAppendedId(ShoppingContract.Stores.CONTENT_URI, storeId);
                Cursor c2 = mContext.getContentResolver().query(uri2,
                        new String[]{ShoppingContract.Stores.NAME}, null, null, null);

                if (c2 != null) {
                    if (c2.moveToFirst()) {
                        String storeName = c2.getString(c2.getColumnIndexOrThrow(ShoppingContract.Stores.NAME));

                        if (price != 0) {
                            String info = storeName + "=" + aisle + "/" + pricestring;

                            if (perStoreInfo.equals("")) {
                                perStoreInfo = info;
                            } else {
                                perStoreInfo += ";" + info;
                            }
                        }
                    }
                    c2.close();
                }
            }
            c1.close();
        }
        return perStoreInfo;
    }

}

/*
 * Copyright (C) 2008 OpenIntents.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Xml.Encoding;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;

import org.openintents.convertcsv.PreferenceActivity;
import org.openintents.convertcsv.common.ConvertCsvBaseActivity;
import org.openintents.convertcsv.common.WrongFormatException;
import org.openintents.shopping.R;
import org.openintents.shopping.library.provider.ShoppingContract;
import org.openintents.shopping.library.util.ShoppingUtils;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

public class ConvertCsvActivity extends ConvertCsvBaseActivity {

    public static final String TAG = "ConvertCsvActivity";

    final String HANDYSHOPPER_FORMAT = "handyshopper";

    public void setPreferencesUsed() {
        PREFERENCE_FILENAME = PreferenceActivity.PREFS_SHOPPINGLIST_FILENAME;
        DEFAULT_FILENAME = getString(R.string.default_shoppinglist_filename);
        PREFERENCE_FORMAT = PreferenceActivity.PREFS_SHOPPINGLIST_FORMAT;
        DEFAULT_FORMAT = "outlook tasks";
        PREFERENCE_ENCODING = PreferenceActivity.PREFS_SHOPPINGLIST_ENCODING;
        PREFERENCE_USE_CUSTOM_ENCODING = PreferenceActivity.PREFS_SHOPPINGLIST_USE_CUSTOM_ENCODING;
        RES_STRING_FILEMANAGER_TITLE = R.string.filemanager_title_shoppinglist;
        RES_ARRAY_CSV_FILE_FORMAT = R.array.shoppinglist_format;
        RES_ARRAY_CSV_FILE_FORMAT_VALUE = R.array.shoppinglist_format_value;
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (mConvertInfo != null) {
            mConvertInfo.setText(R.string.convert_all_shoppinglists);
        }

        if (mSpinner != null) {
            mSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                    updateInfo();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parentView) {
                    updateInfo();
                }
            });
        }
    }

    public void updateInfo() {
        if (mConvertInfo == null) return;

        String format = getFormat();
        if (DEFAULT_FORMAT.equals(format)) {
            mConvertInfo.setText(R.string.convert_all_shoppinglists);
        } else if (HANDYSHOPPER_FORMAT.equals(format)) {
            long listId = getCurrentListId();
            String listname = getListName(listId);
            if (listname != null) {
                String text = getString(R.string.convert_list, listname);
                mConvertInfo.setText(text);
            }

        }
    }

    /**
     * @param reader
     * @throws IOException
     */
    @Override
    public void doImport(Reader reader) throws IOException,
            WrongFormatException {
        ImportCsv ic = new ImportCsv(this, getValidatedImportPolicy());
        String format = getFormat();
        if (DEFAULT_FORMAT.equals(format)) {
            ic.importCsv(reader);
        } else if (HANDYSHOPPER_FORMAT.equals(format)) {
            SharedPreferences pm = PreferenceManager.getDefaultSharedPreferences(this);
            Boolean importStores = pm.getBoolean("shoppinglist_import_stores", true);
            long listId = getCurrentListId();
            ic.importHandyShopperCsv(reader, listId, importStores);
        }
    }

    @Override
    public void onImportFinished() {
        Intent i = new Intent(Intent.ACTION_VIEW);
        Uri uri = ShoppingContract.Lists.CONTENT_URI.buildUpon().appendPath(String.valueOf(getCurrentListId())).build();
        i.setData(uri);
        startActivity(i);
    }

    @Override
    protected Encoding getDefaultEncoding() {
        long id = mSpinner.getSelectedItemId();
        if (0 == id) {
            return Encoding.ISO_8859_1; // Default encoding for "MS Outlook Tasks".
        } else if (1 == id) {
            return Encoding.UTF_8; // Default encoding for "HandyShopper".
        } else {
            return super.getDefaultEncoding();
        }
    }

    /**
     * @param writer
     * @throws IOException
     */
    @Override
    public void doExport(Writer writer) throws IOException {
        ExportCsv ec = new ExportCsv(this);
        String format = getFormat();
        if (DEFAULT_FORMAT.equals(format)) {
            ec.exportCsv(writer);
        } else if (HANDYSHOPPER_FORMAT.equals(format)) {
            long listId = getCurrentListId();
            ec.exportHandyShopperCsv(writer, listId);
			/*runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(ConvertCsvActivity.this, R.string.error_not_yet_implemented,
							Toast.LENGTH_LONG).show();
				}
			});*/
        }
    }

    /**
     * @return The string that identifies the import policy for this importer.
     * null if this derived activity does not support import policies.
     */
    public String getImportPolicyPrefString() {
        return "shoppinglist_import_policy";
    }

    /**
     * @return The default import policy
     */
    public String getDefaultImportPolicy() {
        return "" + IMPORT_POLICY_KEEP;
    }

    public long getCurrentListId() {
        long listId = -1;

        // Try the URI with which Convert CSV has been called:
        Uri uri = getIntent().getData();
        Cursor c = getContentResolver().query(uri,
                new String[]{ShoppingContract.Lists._ID}, null, null, null);
        if (c != null) {
            if (c.moveToFirst()) {
                listId = c.getLong(0);
            }
            c.close();
        }

        // Use default list if URI is not valid.
        if (listId < 0) {
            listId = ShoppingUtils.getDefaultList(this);
        }
        return listId;
    }

    public String getListName(long listId) {
        String listname = null;
        Uri uri = getIntent().getData();
        Cursor c = getContentResolver().query(uri
                , new String[]{ShoppingContract.Lists.NAME}, null, null, null);
        if (c != null) {
            if (c.moveToFirst()) {
                listname = c.getString(0);
            }
            c.close();
        }
        return listname;
    }
}
/*
 * Copyright (C) 2008 OpenIntents.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import android.content.Context;
import android.text.TextUtils;

import org.openintents.convertcsv.common.ConvertCsvBaseActivity;
import org.openintents.convertcsv.common.WrongFormatException;
import org.openintents.convertcsv.opencsv.CSVReader;
import org.openintents.shopping.R;
import org.openintents.shopping.library.provider.ShoppingContract.Status;
import org.openintents.shopping.library.util.ShoppingUtils;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;

public class ImportCsv {

    Context mContext;
    Boolean mDuplicate = true;
    Boolean mUpdate = false;

    public ImportCsv(Context context, int importPolicy) {
        mContext = context;
        switch (importPolicy) {
            case ConvertCsvBaseActivity.IMPORT_POLICY_KEEP:
                mDuplicate = false;
                mUpdate = false;
                break;
            case ConvertCsvBaseActivity.IMPORT_POLICY_RESTORE:
                // not implemented, treat as overwrite for now.
            case ConvertCsvBaseActivity.IMPORT_POLICY_OVERWRITE:
                mDuplicate = false;
                mUpdate = true;
                break;
            case ConvertCsvBaseActivity.IMPORT_POLICY_DUPLICATE:
                mDuplicate = true;
                mUpdate = false;
                break;
        }
    }

    /**
     * @param dis
     * @throws IOException
     */
    public void importCsv(Reader reader) throws IOException,
            WrongFormatException {
        CSVReader csvreader = new CSVReader(reader);
        String[] nextLine;
        while ((nextLine = csvreader.readNext()) != null) {
            if (nextLine.length != 4) {
                throw new WrongFormatException();
            }
            // nextLine[] is an array of values from the line
            String statusstring = nextLine[1];
            if (statusstring.equals(mContext.getString(R.string.header_percent_complete))) {
                // First line is just subject, so let us skip it
                continue;
            }
            String itemname = nextLine[0];
            long status;
            try {
                status = Long.parseLong(statusstring);
            } catch (NumberFormatException e) {
                status = 0;
            }
            String listname = nextLine[2];
            String tags = nextLine[3];

            // Add item to list
            long listId = ShoppingUtils.getList(mContext, listname);
            long itemId = ShoppingUtils.getItem(mContext, itemname, tags, null,
                    null, null, mDuplicate, mUpdate);

            if (status == 1) {
                status = Status.BOUGHT;
            } else if (status == 0) {
                status = Status.WANT_TO_BUY;
            } else {
                status = Status.REMOVED_FROM_LIST;
            }


            ShoppingUtils.addItemToList(mContext, itemId, listId, status, null, null,
                    false, mDuplicate, false);
        }

    }

    private String convert_hs_price(String hs_price) {
        String price = hs_price;
        try {
            Double fprice = Double.parseDouble(price);
            fprice *= 100;
            price = ((Long) Math.round(fprice)).toString();
        } catch (NumberFormatException nfe) {
        }
        return price;
    }

    public void importHandyShopperCsv(Reader reader, long listId, Boolean importStores) throws IOException, WrongFormatException {
        CSVReader csvreader = new CSVReader(reader);
        String[] nextLine;
        HashMap<String, Long> seen_stores = new HashMap<String, Long>();
        HashMap<String, Long> item_stores = new HashMap<String, Long>();

        while ((nextLine = csvreader.readNext()) != null) {
            if (nextLine.length != 23) {
                throw new WrongFormatException();
            }
            // nextLine[] is an array of values from the line
            String statusstring = nextLine[0];
            if (statusstring.equals(mContext.getString(R.string.header_need))) {
                // First line is just subject, so let us skip it
                continue;
            }

            long status;
            if ("x".equalsIgnoreCase(statusstring)) {
                status = Status.WANT_TO_BUY;
            } else if ("".equalsIgnoreCase(statusstring)) {
                status = Status.BOUGHT;
            } else if ("have".equalsIgnoreCase(statusstring)) {
                status = Status.REMOVED_FROM_LIST;
            } else {
                status = Status.REMOVED_FROM_LIST;
            }

            String itemname = nextLine[2]; // Description
            String tags = nextLine[9]; // Category
            String price = nextLine[6]; // Price
            String note = nextLine[18]; // Note
            String units = nextLine[5];

            if (nextLine[3].length() > 0) {
                if (tags.length() == 0) {
                    tags = nextLine[3];
                } else {
                    tags += "," + nextLine[3];
                }
            }

            String quantity = nextLine[4]; // Quantity
            String priority = nextLine[1]; // Priority

            if (price.length() > 0) {
                price = convert_hs_price(price);
            }

            // Add item to list
            //long listId = ShoppingUtils.getDefaultList(mContext);
            long itemId = ShoppingUtils.getItem(mContext, itemname, tags, price, units, note,
                    mDuplicate, mUpdate);
            ShoppingUtils.addItemToList(mContext, itemId, listId, status, priority, quantity,
                    false, mDuplicate, false);

            // Two columns contain per-store information. Column 10 lists
            // all stores which carry this item, delimited by semicolons. Column 11
            // lists aisles and prices for some subset of those stores.
            //
            // To save time, we first deal with the prices in column 11, then from
            // Column 10 we add only the ones not already added from Column 11.

            String[] stores;
            item_stores.clear();

            // example value for column 11:    Big Y=/0.50;BJ's=11/0.42
            if (nextLine[11].length() > 0 && importStores) {
                stores = nextLine[11].split(";");

                for (int i_store = 0; i_store < stores.length; i_store++) {
                    String[] key_vals = stores[i_store].split("=");
                    String store_name = key_vals[0];
                    String[] aisle_price = key_vals[1].split("/");
                    if (aisle_price.length == 0)
                        continue;
                    String aisle = aisle_price[0];
                    String store_price = "";
                    if (aisle_price.length > 1) {
                        store_price = convert_hs_price(aisle_price[1]);
                    }

                    Long storeId = seen_stores.get(store_name);
                    if (storeId == null) {
                        storeId = ShoppingUtils.getStore(mContext, store_name, listId);
                        seen_stores.put(store_name, storeId);
                    }
                    item_stores.put(store_name, storeId);
                    long item_store = ShoppingUtils.addItemToStore(mContext, itemId, storeId, aisle, store_price, mDuplicate);
                }
            }

            if (nextLine[10].length() > 0) {
                stores = nextLine[10].split(";");
                for (int i_store = 0; i_store < stores.length; i_store++) {
                    if (importStores) {    // real store import
                        Long storeId = item_stores.get(stores[i_store]);
                        if (storeId != null)
                            // existence of item at store handled in price handling, no need to add it again.
                            continue;
                        storeId = seen_stores.get(stores[i_store]);
                        if (storeId == null) {
                            storeId = ShoppingUtils.getStore(mContext, stores[i_store], listId);
                            seen_stores.put(stores[i_store], storeId);
                        }
                        item_stores.put(stores[i_store], storeId); // not strictly required, but...
                        long item_store = ShoppingUtils.addItemToStore(mContext, itemId, storeId, "", "", mDuplicate);
                    } else if (!TextUtils.isEmpty(stores[i_store])) {
                        // store names added as tags.
                        ShoppingUtils.addTagToItem(mContext, itemId, stores[i_store]);
                    }
                }
            }
        }
    }
}

/*
 * Copyright (C) 2008 OpenIntents.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/**
 * Copyright 2005 Bytecode Pty Ltd.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * The project is available at http://sourceforge.net/projects/opencsv/
 * <p>
 * Modifications:
 * - Peli: Dec 12, 2008: Remove "this.".
 */

/**
 * The project is available at http://sourceforge.net/projects/opencsv/
 */

/**
 * Modifications: 
 *   - Peli: Dec 12, 2008: Remove "this.".
 */

import org.openintents.convertcsv.common.ConvertCsvBaseActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * A very simple CSV reader released under a commercial-friendly license.
 *
 * @author Glen Smith
 *
 */
public class CSVReader {

    /** The default separator to use if none is supplied to the constructor. */
    public static final char DEFAULT_SEPARATOR = ',';
    /**
     * The default quote character to use if none is supplied to the
     * constructor.
     */
    public static final char DEFAULT_QUOTE_CHARACTER = '"';
    /**
     * The default line to start reading.
     */
    public static final int DEFAULT_SKIP_LINES = 0;
    private BufferedReader br;
    private boolean hasNext = true;
    private char separator;
    private char quotechar;
    private int skipLines;
    private int readSoFar;
    private boolean linesSkiped;

    /**
     * Constructs CSVReader using a comma for the separator.
     *
     * @param reader
     *            the reader to an underlying CSV source.
     */
    public CSVReader(Reader reader) {
        this(reader, DEFAULT_SEPARATOR);
    }

    /**
     * Constructs CSVReader with supplied separator.
     *
     * @param reader
     *            the reader to an underlying CSV source.
     * @param separator
     *            the delimiter to use for separating entries.
     */
    public CSVReader(Reader reader, char separator) {
        this(reader, separator, DEFAULT_QUOTE_CHARACTER);
    }


    /**
     * Constructs CSVReader with supplied separator and quote char.
     *
     * @param reader
     *            the reader to an underlying CSV source.
     * @param separator
     *            the delimiter to use for separating entries
     * @param quotechar
     *            the character to use for quoted elements
     */
    public CSVReader(Reader reader, char separator, char quotechar) {
        this(reader, separator, quotechar, DEFAULT_SKIP_LINES);
    }

    /**
     * Constructs CSVReader with supplied separator and quote char.
     *
     * @param reader
     *            the reader to an underlying CSV source.
     * @param separator
     *            the delimiter to use for separating entries
     * @param quotechar
     *            the character to use for quoted elements
     * @param line
     *            the line number to skip for start reading 
     */
    public CSVReader(Reader reader, char separator, char quotechar, int line) {
        this.br = new BufferedReader(reader);
        this.separator = separator;
        this.quotechar = quotechar;
        this.skipLines = line;
        this.readSoFar = 0;
    }

    /**
     * Reads the entire file into a List with each element being a String[] of
     * tokens.
     *
     * @return a List of String[], with each String[] representing a line of the
     *         file.
     *
     * @throws IOException
     *             if bad things happen during the read
     */
    public List readAll() throws IOException {

        List allElements = new ArrayList();
        while (hasNext) {
            String[] nextLineAsTokens = readNext();
            if (nextLineAsTokens != null)
                allElements.add(nextLineAsTokens);
        }
        return allElements;

    }

    /**
     * Reads the next line from the buffer and converts to a string array.
     *
     * @return a string array with each comma-separated element as a separate
     *         entry.
     *
     * @throws IOException
     *             if bad things happen during the read
     */
    public String[] readNext() throws IOException {

        String nextLine = getNextLine();
        return hasNext ? parseLine(nextLine) : null;
    }

    /**
     * Reads the next line from the file.
     *
     * @return the next line from the file without trailing newline
     * @throws IOException
     *             if bad things happen during the read
     */
    private String getNextLine() throws IOException {
        if (!linesSkiped) {
            for (int i = 0; i < skipLines; i++) {
                String skippedLine = br.readLine();

                if (skippedLine != null) {
                    readSoFar += skippedLine.length() + 1;    // This is an approximation, CR/LF might count as 2
                    ConvertCsvBaseActivity.dispatchConversionProgress(readSoFar);
                }
            }
            linesSkiped = true;
        }
        String nextLine = br.readLine();
        if (nextLine == null) {
            hasNext = false;
        } else {
            readSoFar += nextLine.length() + 1;    // This is an approximation, CR/LF might count as 2
            ConvertCsvBaseActivity.dispatchConversionProgress(readSoFar);
        }
        return hasNext ? nextLine : null;
    }

    /**
     * Parses an incoming String and returns an array of elements.
     *
     * @param nextLine
     *            the string to parse
     * @return the comma-tokenized list of elements, or null if nextLine is null
     * @throws IOException if bad things happen during the read
     */
    private String[] parseLine(String nextLine) throws IOException {

        if (nextLine == null) {
            return null;
        }

        List tokensOnThisLine = new ArrayList();
        StringBuffer sb = new StringBuffer();
        boolean inQuotes = false;
        do {
            if (inQuotes) {
                // continuing a quoted section, reappend newline
                sb.append("\n");
                nextLine = getNextLine();
                if (nextLine == null)
                    break;
            }
            for (int i = 0; i < nextLine.length(); i++) {

                char c = nextLine.charAt(i);
                if (c == quotechar) {
                    // this gets complex... the quote may end a quoted block, or escape another quote.
                    // do a 1-char lookahead:
                    if (inQuotes  // we are in quotes, therefore there can be escaped quotes in here.
                            && nextLine.length() > (i + 1)  // there is indeed another character to check.
                            && nextLine.charAt(i + 1) == quotechar) { // ..and that char. is a quote also.
                        // we have two quote chars in a row == one quote char, so consume them both and
                        // put one on the token. we do *not* exit the quoted text.
                        sb.append(nextLine.charAt(i + 1));
                        i++;
                    } else {
                        inQuotes = !inQuotes;
                        // the tricky case of an embedded quote in the middle: a,bc"d"ef,g
                        if (i > 2 //not on the begining of the line
                                && nextLine.charAt(i - 1) != separator //not at the begining of an escape sequence
                                && nextLine.length() > (i + 1) &&
                                nextLine.charAt(i + 1) != separator //not at the	end of an escape sequence
                        ) {
                            sb.append(c);
                        }
                    }
                } else if (c == separator && !inQuotes) {
                    tokensOnThisLine.add(sb.toString());
                    sb = new StringBuffer(); // start work on next token
                } else {
                    sb.append(c);
                }
            }
        } while (inQuotes);
        tokensOnThisLine.add(sb.toString());
        return (String[]) tokensOnThisLine.toArray(new String[0]);

    }

    /**
     * Closes the underlying reader.
     *
     * @throws IOException if the close fails
     */
    public void close() throws IOException {
        br.close();
    }

}

/*
 * Copyright (C) 2008 OpenIntents.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/**
 * Copyright 2005 Bytecode Pty Ltd.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * The project is available at http://sourceforge.net/projects/opencsv/
 * <p>
 * Modifications:
 * - Peli: Dec 2, 2008: Add possibility to write mixed output through new functions
 * write() and writeNewline().
 * - Peli: Dec 3, 2008: Add writeValue() function.
 * - Peli: Dec 12, 2008: Add setLineEnd().
 */

/**
 * The project is available at http://sourceforge.net/projects/opencsv/
 */

/**
 * Modifications: 
 *   - Peli: Dec 2, 2008: Add possibility to write mixed output through new functions
 *     write() and writeNewline().
 *   - Peli: Dec 3, 2008: Add writeValue() function.
 *   - Peli: Dec 12, 2008: Add setLineEnd().
 */

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;

/**
 * A very simple CSV writer released under a commercial-friendly license.
 *
 * @author Glen Smith
 *
 */
public class CSVWriter {

    /** The character used for escaping quotes. */
    public static final char DEFAULT_ESCAPE_CHARACTER = '"';
    /** The default separator to use if none is supplied to the constructor. */
    public static final char DEFAULT_SEPARATOR = ',';
    /**
     * The default quote character to use if none is supplied to the
     * constructor.
     */
    public static final char DEFAULT_QUOTE_CHARACTER = '"';
    /** The quote constant to use when you wish to suppress all quoting. */
    public static final char NO_QUOTE_CHARACTER = '\u0000';
    /** The escape constant to use when you wish to suppress all escaping. */
    public static final char NO_ESCAPE_CHARACTER = '\u0000';
    /** Default line terminator uses platform encoding. */
    public static final String DEFAULT_LINE_END = "\n";
    private static final SimpleDateFormat
            TIMESTAMP_FORMATTER =
            new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
    private static final SimpleDateFormat
            DATE_FORMATTER =
            new SimpleDateFormat("dd-MMM-yyyy");
    private Writer rawWriter;
    private PrintWriter pw;
    private char separator;
    private char quotechar;
    private char escapechar;
    private String lineEnd;
    private int currentColumn;
    private StringBuffer stringBuffer;

    /**
     * Constructs CSVWriter using a comma for the separator.
     *
     * @param writer
     *            the writer to an underlying CSV source.
     */
    public CSVWriter(Writer writer) {
        this(writer, DEFAULT_SEPARATOR);
    }

    /**
     * Constructs CSVWriter with supplied separator.
     *
     * @param writer
     *            the writer to an underlying CSV source.
     * @param separator
     *            the delimiter to use for separating entries.
     */
    public CSVWriter(Writer writer, char separator) {
        this(writer, separator, DEFAULT_QUOTE_CHARACTER);
    }

    /**
     * Constructs CSVWriter with supplied separator and quote char.
     *
     * @param writer
     *            the writer to an underlying CSV source.
     * @param separator
     *            the delimiter to use for separating entries
     * @param quotechar
     *            the character to use for quoted elements
     */
    public CSVWriter(Writer writer, char separator, char quotechar) {
        this(writer, separator, quotechar, DEFAULT_ESCAPE_CHARACTER);
    }

    /**
     * Constructs CSVWriter with supplied separator and quote char.
     *
     * @param writer
     *            the writer to an underlying CSV source.
     * @param separator
     *            the delimiter to use for separating entries
     * @param quotechar
     *            the character to use for quoted elements
     * @param escapechar
     *            the character to use for escaping quotechars or escapechars
     */
    public CSVWriter(Writer writer, char separator, char quotechar, char escapechar) {
        this(writer, separator, quotechar, escapechar, DEFAULT_LINE_END);
    }


    /**
     * Constructs CSVWriter with supplied separator and quote char.
     *
     * @param writer
     *            the writer to an underlying CSV source.
     * @param separator
     *            the delimiter to use for separating entries
     * @param quotechar
     *            the character to use for quoted elements
     * @param lineEnd
     * 			  the line feed terminator to use
     */
    public CSVWriter(Writer writer, char separator, char quotechar, String lineEnd) {
        this(writer, separator, quotechar, DEFAULT_ESCAPE_CHARACTER, lineEnd);
    }


    /**
     * Constructs CSVWriter with supplied separator, quote char, escape char and line ending.
     *
     * @param writer
     *            the writer to an underlying CSV source.
     * @param separator
     *            the delimiter to use for separating entries
     * @param quotechar
     *            the character to use for quoted elements
     * @param escapechar
     *            the character to use for escaping quotechars or escapechars
     * @param lineEnd
     * 			  the line feed terminator to use
     */
    public CSVWriter(Writer writer, char separator, char quotechar, char escapechar, String lineEnd) {
        this.rawWriter = writer;
        this.pw = new PrintWriter(writer);
        this.separator = separator;
        this.quotechar = quotechar;
        this.escapechar = escapechar;
        this.lineEnd = lineEnd;

        this.currentColumn = 0;
        this.stringBuffer = new StringBuffer();
    }

    private static String getColumnValue(ResultSet rs, int colType, int colIndex)
            throws SQLException, IOException {

        String value = "";

        switch (colType) {
            case Types.BIT:
                Object bit = rs.getObject(colIndex);
                if (bit != null) {
                    value = String.valueOf(bit);
                }
                break;
            case Types.BOOLEAN:
                boolean b = rs.getBoolean(colIndex);
                if (!rs.wasNull()) {
                    value = Boolean.valueOf(b).toString();
                }
                break;
            case Types.CLOB:
                Clob c = rs.getClob(colIndex);
                if (c != null) {
                    value = read(c);
                }
                break;
            case Types.BIGINT:
            case Types.DECIMAL:
            case Types.DOUBLE:
            case Types.FLOAT:
            case Types.REAL:
            case Types.NUMERIC:
                BigDecimal bd = rs.getBigDecimal(colIndex);
                if (bd != null) {
                    value = "" + bd.doubleValue();
                }
                break;
            case Types.INTEGER:
            case Types.TINYINT:
            case Types.SMALLINT:
                int intValue = rs.getInt(colIndex);
                if (!rs.wasNull()) {
                    value = "" + intValue;
                }
                break;
            case Types.JAVA_OBJECT:
                Object obj = rs.getObject(colIndex);
                if (obj != null) {
                    value = String.valueOf(obj);
                }
                break;
            case Types.DATE:
                java.sql.Date date = rs.getDate(colIndex);
                if (date != null) {
                    value = DATE_FORMATTER.format(date);
                    ;
                }
                break;
            case Types.TIME:
                Time t = rs.getTime(colIndex);
                if (t != null) {
                    value = t.toString();
                }
                break;
            case Types.TIMESTAMP:
                Timestamp tstamp = rs.getTimestamp(colIndex);
                if (tstamp != null) {
                    value = TIMESTAMP_FORMATTER.format(tstamp);
                }
                break;
            case Types.LONGVARCHAR:
            case Types.VARCHAR:
            case Types.CHAR:
                value = rs.getString(colIndex);
                break;
            default:
                value = "";
        }


        if (value == null) {
            value = "";
        }

        return value;

    }

    private static String read(Clob c) throws SQLException, IOException {
        StringBuffer sb = new StringBuffer((int) c.length());
        Reader r = c.getCharacterStream();
        char[] cbuf = new char[2048];
        int n = 0;
        while ((n = r.read(cbuf, 0, cbuf.length)) != -1) {
            if (n > 0) {
                sb.append(cbuf, 0, n);
            }
        }
        return sb.toString();
    }

    public void setLineEnd(String lineEnd) {
        this.lineEnd = lineEnd;
    }

    public void setQuoteCharacter(char quotecharacter) {
        this.quotechar = quotecharacter;
    }

    /**
     * Writes the entire list to a CSV file. The list is assumed to be a
     * String[]
     *
     * @param allLines
     *            a List of String[], with each String[] representing a line of
     *            the file.
     */
    public void writeAll(List allLines) {

        for (Iterator iter = allLines.iterator(); iter.hasNext(); ) {
            String[] nextLine = (String[]) iter.next();
            writeNext(nextLine);
        }

    }

    protected void writeColumnNames(ResultSetMetaData metadata)
            throws SQLException {

        int columnCount = metadata.getColumnCount();

        String[] nextLine = new String[columnCount];
        for (int i = 0; i < columnCount; i++) {
            nextLine[i] = metadata.getColumnName(i + 1);
        }
        writeNext(nextLine);
    }

    /**
     * Writes the entire ResultSet to a CSV file.
     *
     * The caller is responsible for closing the ResultSet.
     *
     * @param rs the recordset to write
     * @param includeColumnNames true if you want column names in the output, false otherwise
     *
     */
    public void writeAll(ResultSet rs, boolean includeColumnNames) throws SQLException, IOException {

        ResultSetMetaData metadata = rs.getMetaData();


        if (includeColumnNames) {
            writeColumnNames(metadata);
        }

        int columnCount = metadata.getColumnCount();

        while (rs.next()) {
            String[] nextLine = new String[columnCount];

            for (int i = 0; i < columnCount; i++) {
                nextLine[i] = getColumnValue(rs, metadata.getColumnType(i + 1), i + 1);
            }

            writeNext(nextLine);
        }
    }

    /**
     * Writes the next line to the file.
     *
     * @param nextLine
     *            a string array with each comma-separated element as a separate
     *            entry.
     */
    public void writeNext(String[] nextLine) {

        if (nextLine == null)
            return;

        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < nextLine.length; i++) {

            if (i != 0) {
                sb.append(separator);
            }

            String nextElement = nextLine[i];
            if (nextElement == null)
                continue;
            if (quotechar != NO_QUOTE_CHARACTER)
                sb.append(quotechar);
            for (int j = 0; j < nextElement.length(); j++) {
                char nextChar = nextElement.charAt(j);
                if (escapechar != NO_ESCAPE_CHARACTER && nextChar == quotechar) {
                    sb.append(escapechar).append(nextChar);
                } else if (escapechar != NO_ESCAPE_CHARACTER && nextChar == escapechar) {
                    sb.append(escapechar).append(nextChar);
                } else {
                    sb.append(nextChar);
                }
            }
            if (quotechar != NO_QUOTE_CHARACTER)
                sb.append(quotechar);
        }

        sb.append(lineEnd);
        pw.write(sb.toString());

    }

    /**
     * Write a single item.
     * A complete line has to be finished by calling writeNewline().
     *
     * @param string
     * @param useQuotes
     */
    public void write(String string, boolean useQuotes) {
        if (currentColumn > 0) {
            stringBuffer.append(separator);
        }
        currentColumn++;

        if (string == null)
            return;

        boolean usingQuotes = useQuotes;
        if ((quotechar != NO_QUOTE_CHARACTER && string.contains("" + quotechar))
                || string.contains("" + separator)
                || (escapechar != NO_ESCAPE_CHARACTER && string.contains("" + escapechar))
                || string.contains("" + lineEnd)) {
            // Have to use quotes
            usingQuotes = true;
        }

        if ((quotechar != NO_QUOTE_CHARACTER) && usingQuotes)
            stringBuffer.append(quotechar);
        for (int j = 0; j < string.length(); j++) {
            char nextChar = string.charAt(j);
            if (escapechar != NO_ESCAPE_CHARACTER && nextChar == quotechar) {
                stringBuffer.append(escapechar).append(nextChar);
            } else if (escapechar != NO_ESCAPE_CHARACTER && nextChar == escapechar) {
                stringBuffer.append(escapechar).append(nextChar);
            } else {
                stringBuffer.append(nextChar);
            }
        }
        if ((quotechar != NO_QUOTE_CHARACTER) && usingQuotes)
            stringBuffer.append(quotechar);
    }

    /**
     * Write a string. Quote chars are used.
     * @param string
     */
    public void write(String string) {
        write(string, true);
    }

    /**
     * Write a value. Quote chars are only used if necessary.
     *
     * @param string
     */
    public void writeValue(String string) {
        write(string, false);
    }

    /**
     * Write an integer value. Quote chars are only used if necessary.
     *
     * @param i
     */
    public void write(int i) {
        write(String.valueOf(i), false);
    }

    /**
     * End a line of items that have been added through write().
     */
    public void writeNewline() {
        stringBuffer.append(lineEnd);
        pw.write(stringBuffer.toString());
        stringBuffer.delete(0, stringBuffer.length());
        currentColumn = 0;
    }


    /**
     * Flush underlying stream to writer.
     *
     * @throws IOException if bad things happen
     */
    public void flush() throws IOException {

        pw.flush();

    }

    /**
     * Close the underlying stream writer flushing any buffered content.
     *
     * @throws IOException if bad things happen
     *
     */
    public void close() throws IOException {
        pw.flush();
        pw.close();
        rawWriter.close();
    }

}

/*
 * Copyright (C) 2008 OpenIntents.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.provider.OpenableColumns;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml.Encoding;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import org.openintents.convertcsv.PreferenceActivity;
import org.openintents.distribution.DownloadOIAppDialog;
import org.openintents.shopping.R;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

public class ConvertCsvBaseActivity extends AppCompatActivity {

    static final public int IMPORT_POLICY_DUPLICATE = 0;
    static final public int IMPORT_POLICY_KEEP = 1;
    static final public int IMPORT_POLICY_OVERWRITE = 2;
    static final public int IMPORT_POLICY_RESTORE = 3;
    static final public int IMPORT_POLICY_MAX = IMPORT_POLICY_RESTORE;
    static final public int MESSAGE_SET_PROGRESS = 1;    // Progress changed, arg1 = new status
    static final public int MESSAGE_SUCCESS = 2;        // Operation finished.
    static final public int MESSAGE_ERROR = 3;            // An error occured, arg1 = string ID of error
    static final public int MESSAGE_SET_MAX_PROGRESS = 4;    // Set maximum progress int, arg1 = new max value
    protected static final int MENU_SETTINGS = Menu.FIRST + 1;
    protected static final int MENU_CANCEL = Menu.FIRST + 2;
    protected static final int MENU_DISTRIBUTION_START = Menu.FIRST + 100; // MUST BE LAST
    protected static final int DIALOG_ID_WARN_OVERWRITE = 1;
    protected static final int DIALOG_ID_NO_FILE_MANAGER_AVAILABLE = 2;
    protected static final int DIALOG_ID_WARN_RESTORE_POLICY = 3;
    protected static final int DIALOG_ID_PERMISSIONS = 4;
    protected static final int DIALOG_DISTRIBUTION_START = 100; // MUST BE LAST
    protected static final int REQUEST_CODE_PICK_FILE = 1;
    private final static String TAG = "ConvertCsvBaseActivity";
    // This is the activity's message handler that the worker thread can use to communicate
    // with the main thread. This may be null if the activity is paused and could change, so
    // it needs to be read and verified before every use.
    static protected Handler smCurrentHandler;
    // True if we have an active worker thread.
    static boolean smHasWorkerThread;
    // Max value for the progress bar.
    static int smProgressMax;
    protected TextView mFilePathView;
    protected TextView mFileNameView;
    protected TextView mConvertInfo;
    protected Spinner mSpinner;
    protected String PREFERENCE_FILENAME;
    protected String DEFAULT_FILENAME;
    protected String PREFERENCE_FORMAT;
    protected String DEFAULT_FORMAT = null;
    protected String PREFERENCE_ENCODING;
    protected String PREFERENCE_USE_CUSTOM_ENCODING;
    protected int RES_STRING_FILEMANAGER_TITLE = 0;
    protected int RES_STRING_FILEMANAGER_BUTTON_TEXT = 0;
    protected int RES_ARRAY_CSV_FILE_FORMAT = 0;
    protected int RES_ARRAY_CSV_FILE_FORMAT_VALUE = 0;
    String[] mFormatValues;
    // Message handler that receives status messages from the
    // CSV import/export thread.
    Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_SET_PROGRESS:
                    ConvertCsvBaseActivity.this.setConversionProgress(msg.arg1);
                    break;

                case MESSAGE_SET_MAX_PROGRESS:
                    ConvertCsvBaseActivity.this.setMaxProgress(msg.arg1);
                    break;


                case MESSAGE_SUCCESS:
                    ConvertCsvBaseActivity.this.displayMessage(msg.arg1, true);
                    break;

                case MESSAGE_ERROR:
                    ConvertCsvBaseActivity.this.displayMessage(msg.arg1, false);
                    break;
            }
        }
    };

    private Spinner mSpinnerEncoding;

    private CheckBox mCustomEncoding;

    private OnCheckedChangeListener mCustomEncodingListener = new OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            mSpinnerEncoding.setEnabled(isChecked);
        }
    };

    private static int findString(String[] array, String string) {
        int length = array.length;
        for (int i = 0; i < length; i++) {
            if (string.equals(array[i])) {
                return i;
            }
        }
        return -1;
    }

    static public void dispatchSuccess(int successMsg) {
        dispatchMessage(MESSAGE_SUCCESS, successMsg);
    }

    static public void dispatchError(int errorMsg) {
        dispatchMessage(MESSAGE_ERROR, errorMsg);
    }

    static public void dispatchConversionProgress(int newProgress) {
        dispatchMessage(MESSAGE_SET_PROGRESS, newProgress);
    }

    static public void dispatchSetMaxProgress(int maxProgress) {
        dispatchMessage(MESSAGE_SET_MAX_PROGRESS, maxProgress);
    }

    static void dispatchMessage(int what, int argument) {
        // Cache the handler since the other thread could modify it at any time.
        Handler handler = smCurrentHandler;

        if (handler != null) {
            Message msg = Message.obtain(handler, what, argument, 0);
            handler.sendMessage(msg);
        }
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Always create the main layout first, since we need to populate the
        // variables with all the views.
        switchToMainLayout();
        if (smHasWorkerThread) {
            switchToConvertLayout();
        }
    }


    private void switchToMainLayout() {
        setContentView(R.layout.convert);

        DEFAULT_FILENAME = getString(R.string.default_filename);

        setPreferencesUsed();

        mFilePathView = findViewById(R.id.file_path);
        mFileNameView = findViewById(R.id.file_name);

        SharedPreferences pm = PreferenceManager.getDefaultSharedPreferences(this);
        String filepath = pm.getString(PREFERENCE_FILENAME, "");

        if (TextUtils.isEmpty(filepath)) {
            setFileUriUnknown();
        } else {
            setFileUri(Uri.parse(filepath));
        }


        ImageButton buttonFileManager = findViewById(R.id.new_document);
        buttonFileManager.setOnClickListener(arg0 -> openFileManagerForNewDocument());

        buttonFileManager = findViewById(R.id.open_document);
        buttonFileManager.setOnClickListener(arg0 -> openFileManagerForChoosingDocument());

        mConvertInfo = findViewById(R.id.convert_info);

        Button buttonImport = findViewById(R.id.file_import);
        buttonImport.setOnClickListener(arg0 -> startImport());

        Button buttonExport = findViewById(R.id.file_export);
        buttonExport.setOnClickListener(arg0 -> startExport());

        mSpinner = (Spinner) findViewById(R.id.spinner1);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, RES_ARRAY_CSV_FILE_FORMAT, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(adapter);

        mFormatValues = getResources().getStringArray(RES_ARRAY_CSV_FILE_FORMAT_VALUE);

        setSpinner(pm.getString(PREFERENCE_FORMAT, DEFAULT_FORMAT));

        // set encoding spinner
        mSpinnerEncoding = (Spinner) findViewById(R.id.spinner_encoding);
        EncodingAdapter adapterEncoding = new EncodingAdapter(this, android.R.layout.simple_spinner_item);
        adapterEncoding.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerEncoding.setAdapter(adapterEncoding);

        String encodingString = getDefaultEncoding().name();
        try {
            encodingString = pm.getString(PREFERENCE_ENCODING, encodingString);
        } catch (ClassCastException ignored) {
        }

        Encoding encoding;
        try {
            encoding = Encoding.valueOf(encodingString);
        } catch (IllegalArgumentException e) {
            encoding = Encoding.UTF_8;
        }
        int encodingPosition = adapterEncoding.getPosition(encoding);
        if (encodingPosition != Spinner.INVALID_POSITION) {
            mSpinnerEncoding.setSelection(encodingPosition);
        }

        // set encoding checkbox
        mCustomEncoding = (CheckBox) findViewById(R.id.custom_encoding);
        mCustomEncoding.setOnCheckedChangeListener(mCustomEncodingListener);
        mCustomEncoding.setChecked(pm.getBoolean(PREFERENCE_USE_CUSTOM_ENCODING, false));

        Intent intent = getIntent();
        String type = intent.getType();
        if (type != null && type.equals("text/csv")) {
            // Someone wants to import a CSV document through the file manager.
            // Set the path accordingly:
            Uri path = getIntent().getData();
            if (path != null) {
                setFileUri(path);
            } else {
                setFileUriUnknown();
            }
        }
    }

    private void switchToConvertLayout() {
        setContentView(R.layout.convertprogress);
        ((ProgressBar) findViewById(R.id.Progress)).setMax(smProgressMax);
        smCurrentHandler = mHandler;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // The worker thread is on its own now.
        smCurrentHandler = null;
    }

    public void setSpinner(String value) {
        // get the ID:
        int id = findString(mFormatValues, value);

        if (id != -1) {
            mSpinner.setSelection(id);
        }
    }

    public void setPreferencesUsed() {

    }

    /**
     * Display the current import policy.
     */
    public void displayImportPolicy() {
        int importPolicy = getValidatedImportPolicy();

        String[] policyStrings = getResources().getStringArray(R.array.import_policy_detail);

        TextView policyView = findViewById(R.id.import_policy_detail);

        if (policyView != null) {
            policyView.setText(policyStrings[importPolicy]);
        }
    }

    public int getValidatedImportPolicy() {
        String prefKey = getImportPolicyPrefString();

        if (prefKey == null) {
            // This activity does not support import policies.
            return IMPORT_POLICY_DUPLICATE;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        String importPolicy = prefs.getString(prefKey, getDefaultImportPolicy());

        try {
            int policy = Integer.parseInt(importPolicy);

            if (policy < 0 || policy > IMPORT_POLICY_MAX) {
                return 0;
            }

            return policy;
        } catch (NumberFormatException e) {
            // Invalid prefs.
            return 0;
        }
    }

    /**
     * @return The string that identifies the import policy for this importer.
     * null if this derived activity does not support import policies.
     */
    public String getImportPolicyPrefString() {
        return null;
    }

    /**
     * @return The default import policy
     */
    public String getDefaultImportPolicy() {
        return "" + IMPORT_POLICY_DUPLICATE;
    }

    public void startImport() {
        int importPolicy = getValidatedImportPolicy();

        if (importPolicy == IMPORT_POLICY_RESTORE) {
            showDialog(DIALOG_ID_WARN_RESTORE_POLICY);
        } else {
            startImportPostCheck();
        }
    }

    public void startImportPostCheck() {
        // First delete old lists
        //getContentResolver().delete(Shopping.Contains.CONTENT_URI, null, null);
        //getContentResolver().delete(Shopping.Items.CONTENT_URI, null, null);
        //getContentResolver().delete(Shopping.Lists.CONTENT_URI, null, null);


        String fileName = getFilenameAndSavePreferences();

        Log.i(TAG, "Importing..." + fileName);

        final Uri file = Uri.parse(fileName);


        // If this is the RESTORE policy, make sure we let the user know
        // what kind of trouble he's getting himself into.
        switchToConvertLayout();
        smHasWorkerThread = true;
        supportInvalidateOptionsMenu();

        new Thread() {
            public void run() {
                try {

                    InputStream inputStream = getContentResolver().openInputStream(file);

                    Reader reader;

                    Encoding enc = getCurrentEncoding();
                    if (enc == null) {
                        reader = new InputStreamReader(inputStream);
                    } else {
                        reader = new InputStreamReader(inputStream, enc.name());
                    }

                    int size = getDocumentSize(file);
                    if (size > 0) {
                        smProgressMax = size;
                        ((ProgressBar) findViewById(R.id.Progress)).setMax(smProgressMax);
                    }

                    doImport(reader);

                    reader.close();
                    dispatchSuccess(R.string.import_finished);
                    onImportFinished();

                } catch (FileNotFoundException e) {
                    dispatchError(R.string.error_file_not_found);
                    Log.i(TAG, "File not found", e);
                } catch (IOException e) {
                    dispatchError(R.string.error_reading_file);
                    Log.i(TAG, "IO exception", e);
                } catch (WrongFormatException e) {
                    dispatchError(R.string.wrong_csv_format);
                    Log.i(TAG, "array index out of bounds", e);
                }

                smHasWorkerThread = false;
                supportInvalidateOptionsMenu();
            }
        }.start();

    }

    public int getDocumentSize(Uri uri) {
        Cursor cursor = getContentResolver()
                .query(uri, null, null, null, null, null);

        int size = -1;
        try {
            if (cursor != null && cursor.moveToFirst()) {
                String displayName = cursor.getString(
                        cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                Log.i(TAG, "Display Name: " + displayName);

                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (!cursor.isNull(sizeIndex)) {
                    size = cursor.getInt(sizeIndex);
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return size;
    }

    public String getDocumentName(Uri uri) {
        Cursor cursor = getContentResolver()
                .query(uri, null, null, null, null, null);

        String displayName = uri.getLastPathSegment();
        try {
            if (cursor != null && cursor.moveToFirst()) {
                displayName = cursor.getString(
                        cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return displayName;
    }

    protected Encoding getCurrentEncoding() {
        if (mCustomEncoding.isChecked()) {
            return (Encoding) mSpinnerEncoding.getSelectedItem();
        } else {
            return getDefaultEncoding();
        }
    }

    protected Encoding getDefaultEncoding() {
        return Encoding.UTF_8;
    }

    void displayMessage(int message, boolean success) {
        // Just make a toast instead?
        //Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        //finish();

        new AlertDialog.Builder(this)
                .setIcon((success) ? android.R.drawable.ic_dialog_info : android.R.drawable.ic_dialog_alert)
                .setMessage(message)
                .setPositiveButton(R.string.dialog_ok, (dialog, which) -> finish())
                .show();
    }

    void setConversionProgress(int newProgress) {
        ((ProgressBar) findViewById(R.id.Progress)).setProgress(newProgress);
    }

    void setMaxProgress(int maxProgress) {
        ((ProgressBar) findViewById(R.id.Progress)).setMax(maxProgress);
    }

    /**
     * @param reader
     * @throws IOException
     */
    public void doImport(Reader reader) throws IOException,
            WrongFormatException {

    }

    public void onImportFinished() {

    }

    public void startExport() {
        Log.i(TAG, "Exporting...");
        doExport();
    }

    public void doExport() {
        String fileName = getFilenameAndSavePreferences();
        final Uri file = Uri.parse(fileName);

        switchToConvertLayout();
        smHasWorkerThread = true;

        new Thread() {
            public void run() {
                try {
                    Writer writer;
                    Encoding enc = getCurrentEncoding();
                    ParcelFileDescriptor pfd = getContentResolver().
                            openFileDescriptor(file, "w");
                    if (enc == null) {
                        writer = new OutputStreamWriter(new FileOutputStream(pfd.getFileDescriptor()));
                    } else {
                        writer = new OutputStreamWriter(new FileOutputStream(pfd.getFileDescriptor()), enc.name());
                    }


                    doExport(writer);

                    writer.close();
                    pfd.close();
                    dispatchSuccess(R.string.export_finished);

                } catch (IOException e) {
                    dispatchError(R.string.error_writing_file);
                    Log.i(TAG, "IO exception", e);
                }

                smHasWorkerThread = false;
            }
        }.start();
    }

    /**
     * @param writer
     * @throws IOException
     */
    public void doExport(Writer writer) throws IOException {

    }

    /**
     * @return
     */
    public String getFilenameAndSavePreferences() {

        String fileName = mFilePathView.getText().toString();

        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(this);
        Editor editor = prefs.edit();
        editor.putString(PREFERENCE_FILENAME, fileName);
        editor.putString(PREFERENCE_FORMAT, getFormat());
        if (mCustomEncoding.isChecked()) {
            editor.putString(PREFERENCE_ENCODING, ((Encoding) mSpinnerEncoding.getSelectedItem()).name());
        }
        editor.putBoolean(PREFERENCE_USE_CUSTOM_ENCODING, mCustomEncoding.isChecked());
        editor.apply();

        return fileName;
    }

    public String getFormat() {
        int id = mSpinner.getSelectedItemPosition();
        if (id != Spinner.INVALID_POSITION) {
            return mFormatValues[id];
        }
        return DEFAULT_FORMAT;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        // Let's not let the user mess around while we're busy.
        if (!smHasWorkerThread) {
            menu.add(0, MENU_SETTINGS, 0, R.string.menu_settings).setShortcut(
                    '1', 's').setIcon(android.R.drawable.ic_menu_preferences);
        } else {
            menu.add(0, MENU_CANCEL, 0, R.string.menu_cancel).setShortcut(
                    '1', 'c').setIcon(android.R.drawable.ic_menu_close_clear_cancel);
        }

        return true;
    }

    @Override
    public void onResume() {
        super.onResume();

        displayImportPolicy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_SETTINGS:
                Intent intent = new Intent(this, PreferenceActivity.class);
                startActivity(intent);
                break;
            case MENU_CANCEL:
                smHasWorkerThread = false;
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected Dialog onCreateDialog(int id) {

        switch (id) {
            case DIALOG_ID_WARN_OVERWRITE:
                LayoutInflater inflater = LayoutInflater.from(this);
                View view = inflater.inflate(R.layout.file_exists, null);
                final CheckBox cb = (CheckBox) view
                        .findViewById(R.id.dont_ask_again);
                return new AlertDialog.Builder(this).setView(view).setPositiveButton(
                        android.R.string.yes, new OnClickListener() {

                            public void onClick(DialogInterface dialog, int which) {

                                saveBooleanPreference(PreferenceActivity.PREFS_ASK_IF_FILE_EXISTS, !cb.isChecked());
                                finish();

                            }


                        }).setNegativeButton(android.R.string.no, new OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        // Cancel should not do anything.

                        //saveBooleanPreference(PreferenceActivity.PREFS_ASK_IF_FILE_EXISTS, !cb.isChecked());
                        //finish();
                    }

                }).create();

            case DIALOG_ID_WARN_RESTORE_POLICY:
                return new AlertDialog.Builder(this)
                        .setTitle(R.string.warn_restore_policy_title)
                        .setMessage(R.string.warn_restore_policy)
                        .setPositiveButton(android.R.string.yes, new OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                startImportPostCheck();
                            }
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .create();

            case DIALOG_ID_NO_FILE_MANAGER_AVAILABLE:
                return new DownloadOIAppDialog(this,
                        DownloadOIAppDialog.OI_FILEMANAGER);
            case DIALOG_ID_PERMISSIONS:
                return new AlertDialog.Builder(this)
                        .setTitle(R.string.warn_install_order_title)
                        .setMessage(R.string.warn_install_order)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            dialog.dismiss();
                        })
                        .create();
        }
        return super.onCreateDialog(id);
    }


    /**
     * @param preference
     * @param value
     */
    private void saveBooleanPreference(String preference, boolean value) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(this);
        Editor editor = prefs.edit();
        editor.putBoolean(preference, value);
        editor.apply();
        doExport();
    }

    private void openFileManagerForNewDocument() {

        String fileName = mFileNameView.getText().toString();
        String filePath = mFilePathView.getText().toString();
        if (TextUtils.isEmpty(filePath)) {
            fileName = DEFAULT_FILENAME;
        }
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/*");
        intent.putExtra(Intent.EXTRA_TITLE, fileName);

        try {
            startActivityForResult(intent, REQUEST_CODE_PICK_FILE);
        } catch (ActivityNotFoundException e) {
            showDialog(DIALOG_ID_NO_FILE_MANAGER_AVAILABLE);
        }
    }

    private void openFileManagerForChoosingDocument() {

        String fileName = mFilePathView.getText().toString();

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        intent.addCategory(Intent.CATEGORY_OPENABLE);

        intent.setType("text/*");
        intent.putExtra(Intent.EXTRA_TITLE, fileName);

        try {
            startActivityForResult(intent, REQUEST_CODE_PICK_FILE);
        } catch (ActivityNotFoundException e) {
            showDialog(DIALOG_ID_NO_FILE_MANAGER_AVAILABLE);
        }
    }

    /**
     * Prepends the system's SD card path to the file name.
     *
     * @param filename
     * @return
     */
    protected String getSdCardFilename(String filename) {
        String sdpath = android.os.Environment
                .getExternalStorageDirectory().getAbsolutePath();
        String path;
        if (sdpath.substring(sdpath.length() - 1, sdpath.length()).equals("/")) {
            path = sdpath + filename;
        } else {
            path = sdpath + "/" + filename;
        }
        return path;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG, "onActivityResult");

        switch (requestCode) {
            case REQUEST_CODE_PICK_FILE:
                if (resultCode == RESULT_OK && data != null) {
                    Uri documentUri = data.getData();
                    if (documentUri != null) {
                        setFileUri(documentUri);
                    } else {
                        setFileUriUnknown();
                    }

                }
                break;
        }
    }

    private void setFileUriUnknown() {
        mFileNameView.setText(getString(R.string.unknown_document));
        mFilePathView.setText("");
    }

    private void setFileUri(Uri documentUri) {
        mFileNameView.setText(getDocumentName(documentUri));
        mFilePathView.setText(documentUri.toString());
    }
}


import android.content.Context;
import android.util.Xml.Encoding;
import android.widget.ArrayAdapter;

public class EncodingAdapter extends ArrayAdapter<Encoding> {

    public EncodingAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId, Encoding.values());
    }
}


public class WrongFormatException extends Exception {

}


//Version Nov 21, 2008

/**
 * Provides OpenIntents actions, extras, and categories used by providers.
 * <p>
 * These specifiers extend the standard Android specifiers.
 * </p>
 */
public final class ProviderIntents {

    /**
     * Broadcast Action: Sent after a new entry has been inserted.
     * <p/>
     * <p>
     * Constant Value: "org.openintents.action.INSERTED"
     * </p>
     */
    public static final String ACTION_INSERTED = "org.openintents.action.INSERTED";

    /**
     * Broadcast Action: Sent after an entry has been modified.
     * <p/>
     * <p>
     * Constant Value: "org.openintents.action.MODIFIED"
     * </p>
     */
    public static final String ACTION_MODIFIED = "org.openintents.action.MODIFIED";

    /**
     * Broadcast Action: Sent after an entry has been deleted.
     * <p/>
     * <p>
     * Constant Value: "org.openintents.action.DELETED"
     * </p>
     */
    public static final String ACTION_DELETED = "org.openintents.action.DELETED";

    /**
     * Added by the ACTION_DELETED broadcast if it contains a where clause.
     * <p/>
     * <p>
     * The extra contains a long[] which contains the row IDs of all rows
     * affected by the where clause. It contains NULL if all rows specified by
     * the URI are affected.
     * </p>
     * <p/>
     * <p>
     * Constant Value: "org.openintents.extra.AFFECTED_ROWS"
     * </p>
     */
    public static final String EXTRA_AFFECTED_ROWS = "org.openintents.extra.AFFECTED_ROWS";
}


/**
 * Intents for automation.
 *
 * @author Peli
 * @version 1.0.0
 */
public class AutomationIntents {

    /**
     * Activity Action: This activity is called to create or edit automation
     * settings.
     * <p/>
     * There can be several activities in an apk package that implement this
     * intent.
     * <p/>
     * <p>
     * Constant Value: "org.openintents.action.EDIT_AUTOMATION_SETTINGS"
     * </p>
     */
    public static final String ACTION_EDIT_AUTOMATION = "org.openintents.action.EDIT_AUTOMATION";

    /**
     * Broadcast Action: This broadcast is sent to the same package in order to
     * activate an automation.
     * <p/>
     * There can only be one broadcast receiver per package that implements this
     * intent. Any differentiation should be done through intent extras.
     * <p/>
     * <p>
     * Constant Value: "org.openintents.action.EDIT_AUTOMATION_SETTINGS"
     * </p>
     */
    public static final String ACTION_RUN_AUTOMATION = "org.openintents.action.RUN_AUTOMATION";

    /**
     * String extra containing a human readable description of the action to be
     * performed.
     * <p/>
     * <p>
     * Constant Value: "org.openintents.extra.DESCRIPTION"
     * </p>
     */
    public static final String EXTRA_DESCRIPTION = "org.openintents.extra.DESCRIPTION";

}


//Version Nov 21, 2008

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.text.TextUtils;

public class ProviderUtils {

    /**
     * Returns the row IDs of all affected rows.
     *
     * @param db
     * @param table
     * @param whereClause
     * @param whereArgs
     * @return
     */
    public static long[] getAffectedRows(SQLiteDatabase db, String table,
                                         String whereClause, String[] whereArgs) {
        if (TextUtils.isEmpty(whereClause)) {
            return null;
        }

        Cursor c = db.query(table, new String[]{BaseColumns._ID},
                whereClause, whereArgs, null, null, null);
        long[] affectedRows = null;
        if (c != null) {
            affectedRows = new long[c.getCount()];
            for (int i = 0; c.moveToNext(); i++) {
                affectedRows[i] = c.getLong(0);
            }
        }
        c.close();
        return affectedRows;
    }

}

/*
 * Copyright (C) 2008 OpenIntents.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


// Version Dec 9, 2008


/**
 * Provides OpenIntents actions, extras, and categories used by providers.
 * <p>These specifiers extend the standard Android specifiers.</p>
 */
public final class FileManagerIntents {

    /**
     * Activity Action: Pick a file through the file manager, or let user
     * specify a custom file name.
     * Data is the current file name or file name suggestion.
     * Returns a new file name as file URI in data.
     *
     * <p>Constant Value: "org.openintents.action.PICK_FILE"</p>
     */
    public static final String ACTION_PICK_FILE = "org.openintents.action.PICK_FILE";

    /**
     * Activity Action: Pick a directory through the file manager, or let user
     * specify a custom file name.
     * Data is the current directory name or directory name suggestion.
     * Returns a new directory name as file URI in data.
     *
     * <p>Constant Value: "org.openintents.action.PICK_DIRECTORY"</p>
     */
    public static final String ACTION_PICK_DIRECTORY = "org.openintents.action.PICK_DIRECTORY";

    /**
     * The title to display.
     *
     * <p>This is shown in the title bar of the file manager.</p>
     *
     * <p>Constant Value: "org.openintents.extra.TITLE"</p>
     */
    public static final String EXTRA_TITLE = "org.openintents.extra.TITLE";

    /**
     * The text on the button to display.
     *
     * <p>Depending on the use, it makes sense to set this to "Open" or "Save".</p>
     *
     * <p>Constant Value: "org.openintents.extra.BUTTON_TEXT"</p>
     */
    public static final String EXTRA_BUTTON_TEXT = "org.openintents.extra.BUTTON_TEXT";

}


/**
 * @author Peli
 * @version 1.2.4 (May 2010)
 */
public class ShoppingListIntents {

    /**
     * String extra containing the action to be performed.
     * <p/>
     * <p>
     * Constant Value: "org.openintents.extra.ACTION"
     * </p>
     */
    public static final String EXTRA_ACTION = "org.openintents.extra.ACTION";

    /**
     * String extra containing the data on which to perform the action.
     * <p/>
     * <p>
     * Constant Value: "org.openintents.extra.DATA"
     * </p>
     */
    public static final String EXTRA_DATA = "org.openintents.extra.DATA";

    /**
     * Task to be used in EXTRA_ACTION.
     * <p/>
     * <p>
     * Constant Value: "org.openintents.shopping.task.clean_up_list"
     * </p>
     */
    public static final String TASK_CLEAN_UP_LIST = "org.openintents.shopping.task.clean_up_list";

    /**
     * Inserts shopping list items from a string array in intent extras.
     * <p/>
     * <p>
     * Constant Value: "org.openintents.type/string.arraylist.shopping"
     * </p>
     */
    public static final String TYPE_STRING_ARRAYLIST_SHOPPING = "org.openintents.type/string.arraylist.shopping";

    /**
     * Inserts shopping list items from a string array in intent extras.
     * <p/>
     * <p>
     * Constant Value: "org.openintents.extra.STRING_ARRAYLIST_SHOPPING"
     * </p>
     */
    public static final String EXTRA_STRING_ARRAYLIST_SHOPPING = "org.openintents.extra.STRING_ARRAYLIST_SHOPPING";

    /**
     * Intent extra for list of quantities corresponding to shopping list items
     * in STRING_ARRAYLIST_SHOPPING.
     * <p/>
     * <p>
     * Constant Value: "org.openintents.extra.STRING_ARRAYLIST_QUANTITY"
     * </p>
     */
    public static final String EXTRA_STRING_ARRAYLIST_QUANTITY = "org.openintents.extra.STRING_ARRAYLIST_QUANTITY";

    /**
     * Intent extra for list of prices corresponding to shopping list items in
     * STRING_ARRAYLIST_SHOPPING.
     * <p/>
     * <p>
     * Constant Value: "org.openintents.extra.STRING_ARRAYLIST_PRICE"
     * </p>
     */
    public static final String EXTRA_STRING_ARRAYLIST_PRICE = "org.openintents.extra.STRING_ARRAYLIST_PRICE";

    /**
     * Intent extra for list of barcodes corresponding to shopping list items in
     * STRING_ARRAYLIST_SHOPPING.
     * <p/>
     * <p>
     * Constant Value: "org.openintents.extra.STRING_ARRAYLIST_BARCODE"
     * </p>
     */
    public static final String EXTRA_STRING_ARRAYLIST_BARCODE = "org.openintents.extra.STRING_ARRAYLIST_BARCODE";

}


/**
 * @author Ben
 * @version 1.0.0
 */
public class GeneralIntents {

    /**
     * Inserts a items from intent extras into any list.
     */
    public static final String ACTION_INSERT_FROM_EXTRAS = "org.openintents.action.INSERT_FROM_EXTRAS";

}


/**
 * The main activity prior to version 1.4 was ".ShoppingActivity". Home screens
 * may still contain a direct link to the old activity, therefore this class
 * must never be renamed or moved.
 * <p/>
 * This class is derived from .ui.ShoppingActivity which contains the actual
 * implementation.
 * <p/>
 * This solution is used instead of using an activity-alias in the Manifest,
 * because the activity-alias does not respect the
 * android:windowSoftInputMode="stateHidden|adjustResize" setting.
 */
public class ShoppingActivity extends
        org.openintents.shopping.ui.ShoppingActivity {

    /**
     * For the implementation, see .ui.ShoppingActivity.
     */
}


public class LogConstants {
    public final static String TAG = "Shopping";
    public final static boolean debug = false;
}


import android.app.Application;

public class ShoppingApplication extends Application {
    public OptionalDependencies dependencies() {
        return new OptionalDependencies();
    }
}


import android.app.Activity;
import android.content.Context;

import org.openintents.shopping.sync.NoSyncSupport;
import org.openintents.shopping.ui.ToggleBoughtInputMethod;
import org.openintents.shopping.ui.widget.ShoppingItemsView;

/**
 * This is the default implementation for all product flavors for any implementation
 * <p>
 * If the signature is changed make sure that the corresponding implementations are changed
 * because Android Studio does only show usage for the current build flavor.
 */
public class BaseOptionalDependencies {

    public void onResumeShoppingActivity(Activity context) {
        // do nothing;
    }

    public ToggleBoughtInputMethod getToggleBoughtInputMethod(Context context, ShoppingItemsView itemsView) {
        return null;
    }

    public SyncSupport getSyncSupport(final Context context) {
        return new NoSyncSupport();
    }
}


import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.FileBackupHelper;
import android.app.backup.SharedPreferencesBackupHelper;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import org.openintents.shopping.provider.ShoppingDatabase;

import java.io.IOException;

public class ShoppingBackupAgent extends BackupAgentHelper {
    private static final String TAG = "ShoppingBackupAgent";
    private static final boolean debug = false || LogConstants.debug;

    // The name of the SharedPreferences file
    private static final String PREFS = "org.openintents.shopping_preferences";

    // A key to uniquely identify the set of backup data
    private static final String PREFS_BACKUP_KEY = "prefs";

    private static final String DB_BACKUP_KEY = "db";

    // Allocate a helper and add it to the backup agent
    public void onCreate() {
        if (debug) {
            Log.v(TAG, "onCreate");
        }
        SharedPreferencesBackupHelper prefsHelper = new SharedPreferencesBackupHelper(
                this.getApplicationContext(), PREFS);
        addHelper(PREFS_BACKUP_KEY, prefsHelper);

        FileBackupHelper helper = new FileBackupHelper(this, "../databases/"
                + ShoppingDatabase.DATABASE_NAME);
        addHelper(DB_BACKUP_KEY, helper);
    }

    @Override
    public void onRestore(BackupDataInput data, int appVersionCode,
                          ParcelFileDescriptor newState) throws IOException {
        if (debug) {
            Log.v(TAG, "onRestore");
        }
        super.onRestore(data, appVersionCode, newState);
    }

    @Override
    public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
                         ParcelFileDescriptor newState) throws IOException {
        if (debug) {
            Log.v(TAG, "onBackup");
        }
        super.onBackup(oldState, data, newState);
    }
}


import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import org.openintents.shopping.SyncSupport;

public class NoSyncSupport implements SyncSupport {
    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public void pushListItem(long listId, Cursor cursor) {

    }

    @Override
    public void pushList(Cursor cursor) {

    }

    @Override
    public boolean isSyncEnabled() {
        return false;
    }

    @Override
    public void setSyncEnabled(boolean enableSync) {

    }

    @Override
    public void updateListItem(long listId, Uri itemUri, ContentValues values) {

    }

}


public class ThemeShoppingList {

    // For Shopping list theme
    // (move to separate class eventually)

    public static final String SHOPPING_LIST_THEME = "org.openintents.shoppinglist";

    public static final String background = "background";
    public static final String backgroundPadding = "backgroundPadding";
    public static final String backgroundPaddingLeft = "backgroundPaddingLeft";
    public static final String backgroundPaddingTop = "backgroundPaddingTop";
    public static final String backgroundPaddingRight = "backgroundPaddingRight";
    public static final String backgroundPaddingBottom = "backgroundPaddingBottom";
    public static final String divider = "divider";
    public static final String shopping_divider = "shopping_divider";
    public static final String textTypeface = "textTypeface";
    public static final String textUpperCaseFont = "textUpperCaseFont";
    public static final String textSizeTiny = "textSizeTiny";
    public static final String textSizeSmall = "textSizeSmall";
    public static final String textSizeMedium = "textSizeMedium";
    public static final String textSizeLarge = "textSizeLarge";
    public static final String textColor = "textColor";
    public static final String textColorPrice = "textColorPrice";
    public static final String textColorChecked = "textColorChecked";
    public static final String textColorPriority = "textColorPriority";
    public static final String textStrikethroughChecked = "textStrikethroughChecked";
    public static final String textSuffixUnchecked = "textSuffixUnchecked";
    public static final String textSuffixChecked = "textSuffixChecked";
    public static final String showCheckBox = "showCheckBox";

    public static final String lineMode = "lineMode";
    public static final String lineColor = "lineColor";
}


import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Helper functions for retrieving remote themes, that are themes in external
 * packages.
 *
 * @author Peli
 */
public class ThemeUtils {
    public static final String METADATA_THEMES = "org.openintents.themes";
    public static final String ELEM_THEMES = "themes";

    // For XML:
    public static final String ELEM_ATTRIBUTESET = "attributeset";
    public static final String ELEM_THEME = "theme";
    public static final String ATTR_NAME = "name";
    public static final String ATTR_TITLE = "title";
    public static final String ATTR_STYLE = "style";
    private static final String TAG = "ThemeUtils";
    public static String SCHEMA = "http://schemas.openintents.org/android/themes";

    public static int[] getAttributeIds(Context context, String[] attrNames,
                                        String packageName) {
        int len = attrNames.length;
        Resources res = context.getResources();

        int[] attrIds = new int[len];
        for (int i = 0; i < len; i++) {
            attrIds[i] = res.getIdentifier(attrNames[i], "attr", packageName);
        }
        return attrIds;
    }

    /**
     * Return list of all applications that contain the theme meta-tag.
     *
     * @param pm
     * @param firstPackage : package name of package that should be moved to front.
     * @return
     */
    private static List<ApplicationInfo> getThemePackages(PackageManager pm,
                                                          String firstPackage) {
        List<ApplicationInfo> appinfolist = new LinkedList<>();

        try {
            List<ApplicationInfo> allapps = pm
                    .getInstalledApplications(PackageManager.GET_META_DATA);
            for (ApplicationInfo ai : allapps) {
                if (ai.metaData != null) {
                    if (ai.metaData.containsKey(METADATA_THEMES)) {
                        if (ai.packageName.equals(firstPackage)) {
                            // Add this package at the beginning of the list
                            appinfolist.add(0, ai);
                        } else {
                            appinfolist.add(ai);
                        }
                    }
                }
            }
            return appinfolist;
        } catch (Exception e) {
            // getInstalledApplications can throw android.os.TransactionTooLargeException (data > 1mb)
        }

        return appinfolist;
    }

    private static void addThemeInfos(PackageManager pm, String attributeset,
                                      ApplicationInfo appinfo, List<ThemeInfo> themeinfolist) {
        XmlResourceParser xml = appinfo.loadXmlMetaData(pm, METADATA_THEMES);

        boolean useThisAttributeSet = false;

        try {
            int tagType = xml.next();
            while (XmlPullParser.END_DOCUMENT != tagType) {

                if (XmlPullParser.START_TAG == tagType) {

                    AttributeSet attr = Xml.asAttributeSet(xml);

                    if (xml.getName().equals(ELEM_THEMES)) {

                    } else if (xml.getName().equals(ELEM_ATTRIBUTESET)) {
                        String name = attr.getAttributeValue(SCHEMA, ATTR_NAME);
                        useThisAttributeSet = name.equals(attributeset);
                    } else if (xml.getName().equals(ELEM_THEME)) {
                        if (useThisAttributeSet) {
                            ThemeInfo ti = new ThemeInfo();

                            ti.packageName = appinfo.packageName;
                            int titleResId = attr.getAttributeResourceValue(
                                    SCHEMA, ATTR_TITLE, 0);
                            int styleResId = attr.getAttributeResourceValue(
                                    SCHEMA, ATTR_STYLE, 0);

                            try {
                                Resources res = pm
                                        .getResourcesForApplication(appinfo);
                                ti.title = res.getString(titleResId);
                                ti.styleName = res.getResourceName(styleResId);
                            } catch (NameNotFoundException e) {
                                ti.title = "";
                            }
                            themeinfolist.add(ti);
                        }
                    }
                } else if (XmlPullParser.END_TAG == tagType) {

                }

                tagType = xml.next();
            }

        } catch (XmlPullParserException ex) {
            Log.e(TAG, String.format(
                    "XML parse exception when parsing metadata for '%s': %s",
                    appinfo.packageName, ex.getMessage()));
        } catch (IOException ex) {
            Log.e(TAG, String.format(
                    "I/O exception when parsing metadata for '%s': %s",
                    appinfo.packageName, ex.getMessage()));
        }

        xml.close();
    }

    /**
     * Create a list of all possible themes installed on the device for a
     * specific attributeset.
     *
     * @param context
     * @param attributeset
     * @return
     */
    public static List<ThemeInfo> getThemeInfos(Context context,
                                                String attributeset) {
        PackageManager pm = context.getPackageManager();
        String thisPackageName = context.getPackageName();

        List<ApplicationInfo> appinfolist = getThemePackages(pm,
                thisPackageName);
        List<ThemeInfo> themeinfolist = new LinkedList<>();

        for (ApplicationInfo ai : appinfolist) {
            addThemeInfos(pm, attributeset, ai, themeinfolist);
        }

        return themeinfolist;
    }

    public static String getPackageNameFromStyle(String style) {
        int pos = style.indexOf(':');
        if (pos >= 0) {
            return style.substring(0, pos);
        } else {
            return null;
        }
    }

    public static class ThemeInfo {
        public String packageName;
        public String title;
        public String styleName;
    }
}

/*
 * Copyright (C) 2010 OpenIntents.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import android.content.Context;
import android.content.res.TypedArray;

/**
 * There were trouble retrieving several attributes at once in
 * obtainStyledAttributes - that's why this class tries to retrieve one
 * attribute at a time.
 *
 * @author Peli
 */
public class ThemeAttributes {
    private Context mContext;
    private String mPackageName;
    private int mThemeId;

    public ThemeAttributes(Context context, String packageName, int themeId) {
        mContext = context;
        mPackageName = packageName;
        mThemeId = themeId;
    }

    public boolean getBoolean(String attrName, boolean defaultValue) {
        int[] attr = ThemeUtils.getAttributeIds(mContext,
                new String[]{attrName}, mPackageName);
        TypedArray a = mContext.obtainStyledAttributes(mThemeId, attr);
        boolean b = a.getBoolean(0, defaultValue);
        a.recycle();
        return b;
    }

    public int getColor(String attrName, int defaultValue) {
        int[] attr = ThemeUtils.getAttributeIds(mContext,
                new String[]{attrName}, mPackageName);
        TypedArray a = mContext.obtainStyledAttributes(mThemeId, attr);
        int c = a.getColor(0, defaultValue);
        a.recycle();
        return c;
    }

    public int getDimensionPixelOffset(String attrName, int defaultValue) {
        int[] attr = ThemeUtils.getAttributeIds(mContext,
                new String[]{attrName}, mPackageName);
        TypedArray a = mContext.obtainStyledAttributes(mThemeId, attr);
        int i = a.getDimensionPixelOffset(0, defaultValue);
        a.recycle();
        return i;
    }

    public int getInteger(String attrName, int defaultValue) {
        int[] attr = ThemeUtils.getAttributeIds(mContext,
                new String[]{attrName}, mPackageName);
        TypedArray a = mContext.obtainStyledAttributes(mThemeId, attr);
        int i = a.getInteger(0, defaultValue);
        a.recycle();
        return i;
    }

    public int getResourceId(String attrName, int defaultValue) {
        int[] attr = ThemeUtils.getAttributeIds(mContext,
                new String[]{attrName}, mPackageName);
        TypedArray a = mContext.obtainStyledAttributes(mThemeId, attr);
        int i = a.getResourceId(0, defaultValue);
        a.recycle();
        return i;
    }

    public String getString(String attrName) {
        int[] attr = ThemeUtils.getAttributeIds(mContext,
                new String[]{attrName}, mPackageName);
        TypedArray a = mContext.obtainStyledAttributes(mThemeId, attr);
        String s = a.getString(0);
        a.recycle();
        return s;
    }

}

/*
 * Copyright (C) 2007-2011 OpenIntents.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.MatrixCursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import org.openintents.intents.ProviderIntents;
import org.openintents.intents.ProviderUtils;
import org.openintents.shopping.LogConstants;
import org.openintents.shopping.R;
import org.openintents.shopping.library.provider.ShoppingContract;
import org.openintents.shopping.library.provider.ShoppingContract.Contains;
import org.openintents.shopping.library.provider.ShoppingContract.ContainsFull;
import org.openintents.shopping.library.provider.ShoppingContract.ItemStores;
import org.openintents.shopping.library.provider.ShoppingContract.Items;
import org.openintents.shopping.library.provider.ShoppingContract.Lists;
import org.openintents.shopping.library.provider.ShoppingContract.Status;
import org.openintents.shopping.library.provider.ShoppingContract.Stores;
import org.openintents.shopping.library.provider.ShoppingContract.Units;
import org.openintents.shopping.ui.PreferenceActivity;
import org.openintents.shopping.ui.ShoppingActivity;
import org.openintents.shopping.ui.widget.ShoppingItemsView;

import java.util.HashMap;

/**
 * Provides access to a database of shopping items and shopping lists.
 * <p/>
 * ShoppingProvider maintains the following tables: * items: items you want to
 * buy * lists: shopping lists ("My shopping list", "Bob's shopping list") *
 * contains: which item/list/(recipe) is contained in which shopping list. *
 * stores: * itemstores: (which store carries which item)
 */
public class ShoppingProvider extends ContentProvider {

    protected static final String TAG = "ShoppingProvider";
    private static final boolean debug = false || LogConstants.debug;
    // Basic tables
    private static final int ITEMS = 1;
    private static final int ITEM_ID = 2;
    private static final int LISTS = 3;
    private static final int LIST_ID = 4;
    private static final int CONTAINS = 5;
    private static final int CONTAINS_ID = 6;
    private static final int STORES = 7;
    private static final int STORES_ID = 8;
    private static final int STORES_LISTID = 9;
    private static final int ITEMSTORES = 10;
    private static final int ITEMSTORES_ID = 11;
    private static final int NOTES = 12;
    private static final int NOTE_ID = 13;
    private static final int UNITS = 14;
    private static final int UNITS_ID = 15;
    private static final int PREFS = 16;
    private static final int ITEMSTORES_ITEMID = 17;
    private static final int SUBTOTALS = 18;
    private static final int SUBTOTALS_LISTID = 19;
    private static final int CONTAINS_FULL_LISTID = 20;
    // Derived tables
    private static final int CONTAINS_FULL = 101; // combined with items and
    // lists
    private static final int CONTAINS_FULL_ID = 102;
    private static final int ACTIVELIST = 103;
    // duplicate specified contains record and its item, return ids
    private static final int CONTAINS_COPYOFID = 104;
    private static final int TAGS_LISTID = 105;
    private static final UriMatcher URL_MATCHER;
    private static HashMap<String, String> ITEMS_PROJECTION_MAP;
    private static HashMap<String, String> LISTS_PROJECTION_MAP;
    private static HashMap<String, String> CONTAINS_PROJECTION_MAP;
    private static HashMap<String, String> CONTAINS_FULL_PROJECTION_MAP;
    private static HashMap<String, String> CONTAINS_FULL_CHEAPEST_PROJECTION_MAP;
    private static HashMap<String, String> CONTAINS_FULL_STORE_PROJECTION_MAP;
    private static HashMap<String, String> STORES_PROJECTION_MAP;
    private static HashMap<String, String> ITEMSTORES_PROJECTION_MAP;
    private static HashMap<String, String> NOTES_PROJECTION_MAP;
    private static HashMap<String, String> UNITS_PROJECTION_MAP;
    private static HashMap<String, String> SUBTOTALS_PROJECTION_MAP;

    static {
        URL_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        URL_MATCHER.addURI("org.openintents.shopping", "items", ITEMS);
        URL_MATCHER.addURI("org.openintents.shopping", "items/#", ITEM_ID);
        URL_MATCHER.addURI("org.openintents.shopping", "lists", LISTS);
        URL_MATCHER.addURI("org.openintents.shopping", "lists/active",
                ACTIVELIST);
        URL_MATCHER.addURI("org.openintents.shopping", "lists/#", LIST_ID);
        URL_MATCHER.addURI("org.openintents.shopping", "contains", CONTAINS);
        URL_MATCHER.addURI("org.openintents.shopping", "contains/#",
                CONTAINS_ID);
        URL_MATCHER.addURI("org.openintents.shopping", "contains/copyof/#",
                CONTAINS_COPYOFID);
        URL_MATCHER.addURI("org.openintents.shopping", "containsfull",
                CONTAINS_FULL);
        URL_MATCHER.addURI("org.openintents.shopping", "containsfull/#",
                CONTAINS_FULL_ID);
        URL_MATCHER.addURI("org.openintents.shopping", "containsfull/list/#",
                CONTAINS_FULL_LISTID);
        URL_MATCHER.addURI("org.openintents.shopping", "stores", STORES);
        URL_MATCHER.addURI("org.openintents.shopping", "stores/#", STORES_ID);
        URL_MATCHER
                .addURI("org.openintents.shopping", "itemstores", ITEMSTORES);
        URL_MATCHER.addURI("org.openintents.shopping", "itemstores/#",
                ITEMSTORES_ID);
        URL_MATCHER.addURI("org.openintents.shopping", "itemstores/item/#/#",
                ITEMSTORES_ITEMID);
        URL_MATCHER.addURI("org.openintents.shopping", "liststores/#",
                STORES_LISTID);
        URL_MATCHER.addURI("org.openintents.shopping", "listtags/#",
                TAGS_LISTID);
        URL_MATCHER.addURI("org.openintents.shopping", "notes", NOTES);
        URL_MATCHER.addURI("org.openintents.shopping", "notes/#", NOTE_ID);
        URL_MATCHER.addURI("org.openintents.shopping", "units", UNITS);
        URL_MATCHER.addURI("org.openintents.shopping", "units/#", UNITS_ID);

        URL_MATCHER.addURI("org.openintents.shopping", "prefs", PREFS);
        // subtotals for the specified list id, or active list if not specified
        URL_MATCHER.addURI("org.openintents.shopping", "subtotals/#",
                SUBTOTALS_LISTID);
        URL_MATCHER.addURI("org.openintents.shopping", "subtotals", SUBTOTALS);

        ITEMS_PROJECTION_MAP = new HashMap<String, String>();
        ITEMS_PROJECTION_MAP.put(Items._ID, "items._id");
        ITEMS_PROJECTION_MAP.put(Items.NAME, "items.name");
        ITEMS_PROJECTION_MAP.put(Items.IMAGE, "items.image");
        ITEMS_PROJECTION_MAP.put(Items.PRICE, "items.price");
        ITEMS_PROJECTION_MAP.put(Items.UNITS, "items.units");
        ITEMS_PROJECTION_MAP.put(Items.TAGS, "items.tags");
        ITEMS_PROJECTION_MAP.put(Items.BARCODE, "items.barcode");
        ITEMS_PROJECTION_MAP.put(Items.LOCATION, "items.location");
        ITEMS_PROJECTION_MAP.put(Items.DUE_DATE, "items.due");
        ITEMS_PROJECTION_MAP.put(Items.CREATED_DATE, "items.created");
        ITEMS_PROJECTION_MAP.put(Items.MODIFIED_DATE, "items.modified");
        ITEMS_PROJECTION_MAP.put(Items.ACCESSED_DATE, "items.accessed");

        LISTS_PROJECTION_MAP = new HashMap<String, String>();
        LISTS_PROJECTION_MAP.put(Lists._ID, "lists._id");
        LISTS_PROJECTION_MAP.put(Lists.NAME, "lists.name");
        LISTS_PROJECTION_MAP.put(Lists.IMAGE, "lists.image");
        LISTS_PROJECTION_MAP.put(Lists.CREATED_DATE, "lists.created");
        LISTS_PROJECTION_MAP.put(Lists.MODIFIED_DATE, "lists.modified");
        LISTS_PROJECTION_MAP.put(Lists.ACCESSED_DATE, "lists.accessed");
        LISTS_PROJECTION_MAP.put(Lists.SHARE_NAME, "lists.share_name");
        LISTS_PROJECTION_MAP.put(Lists.SHARE_CONTACTS, "lists.share_contacts");
        LISTS_PROJECTION_MAP
                .put(Lists.SKIN_BACKGROUND, "lists.skin_background");
        LISTS_PROJECTION_MAP.put(Lists.SKIN_FONT, "lists.skin_font");
        LISTS_PROJECTION_MAP.put(Lists.SKIN_COLOR, "lists.skin_color");
        LISTS_PROJECTION_MAP.put(Lists.SKIN_COLOR_STRIKETHROUGH,
                "lists.skin_color_strikethrough");
        LISTS_PROJECTION_MAP.put(Lists.ITEMS_SORT, "lists.items_sort");

        CONTAINS_PROJECTION_MAP = new HashMap<String, String>();
        CONTAINS_PROJECTION_MAP.put(Contains._ID, "contains._id");
        CONTAINS_PROJECTION_MAP.put(Contains.ITEM_ID, "contains.item_id");
        CONTAINS_PROJECTION_MAP.put(Contains.LIST_ID, "contains.list_id");
        CONTAINS_PROJECTION_MAP.put(Contains.QUANTITY, "contains.quantity");
        CONTAINS_PROJECTION_MAP.put(Contains.PRIORITY, "contains.priority");

        CONTAINS_PROJECTION_MAP.put(Contains.STATUS, "contains.status");
        CONTAINS_PROJECTION_MAP.put(Contains.CREATED_DATE, "contains.created");
        CONTAINS_PROJECTION_MAP
                .put(Contains.MODIFIED_DATE, "contains.modified");
        CONTAINS_PROJECTION_MAP
                .put(Contains.ACCESSED_DATE, "contains.accessed");
        CONTAINS_PROJECTION_MAP.put(Contains.SHARE_CREATED_BY,
                "contains.share_created_by");
        CONTAINS_PROJECTION_MAP.put(Contains.SHARE_MODIFIED_BY,
                "contains.share_modified_by");

        CONTAINS_FULL_PROJECTION_MAP = new HashMap<String, String>();
        CONTAINS_FULL_PROJECTION_MAP.put(ContainsFull._ID,
                "contains._id as _id");
        CONTAINS_FULL_PROJECTION_MAP.put(ContainsFull.ITEM_ID,
                "contains.item_id");
        CONTAINS_FULL_PROJECTION_MAP.put(ContainsFull.LIST_ID,
                "contains.list_id");
        CONTAINS_FULL_PROJECTION_MAP.put(ContainsFull.QUANTITY,
                "contains.quantity as quantity");
        CONTAINS_FULL_PROJECTION_MAP.put(ContainsFull.PRIORITY,
                "contains.priority as priority");
        CONTAINS_FULL_PROJECTION_MAP
                .put(ContainsFull.STATUS, "contains.status");
        CONTAINS_FULL_PROJECTION_MAP.put(ContainsFull.CREATED_DATE,
                "contains.created");
        CONTAINS_FULL_PROJECTION_MAP.put(ContainsFull.MODIFIED_DATE,
                "contains.modified");
        CONTAINS_FULL_PROJECTION_MAP.put(ContainsFull.ACCESSED_DATE,
                "contains.accessed");
        CONTAINS_FULL_PROJECTION_MAP.put(ContainsFull.SHARE_CREATED_BY,
                "contains.share_created_by");
        CONTAINS_FULL_PROJECTION_MAP.put(ContainsFull.SHARE_MODIFIED_BY,
                "contains.share_modified_by");
        CONTAINS_FULL_PROJECTION_MAP.put(ContainsFull.ITEM_NAME,
                "items.name as item_name");
        CONTAINS_FULL_PROJECTION_MAP.put(ContainsFull.ITEM_IMAGE,
                "items.image as item_image");
        CONTAINS_FULL_PROJECTION_MAP.put(ContainsFull.ITEM_PRICE,
                "items.price as item_price");
        CONTAINS_FULL_PROJECTION_MAP.put(ContainsFull.ITEM_UNITS,
                "items.units as item_units");
        CONTAINS_FULL_PROJECTION_MAP.put(ContainsFull.ITEM_TAGS,
                "items.tags as item_tags");
        CONTAINS_FULL_PROJECTION_MAP.put(ContainsFull.LIST_NAME,
                "lists.name as list_name");
        CONTAINS_FULL_PROJECTION_MAP.put(ContainsFull.LIST_IMAGE,
                "lists.image as list_image");
        CONTAINS_FULL_PROJECTION_MAP.put(ContainsFull.ITEM_HAS_NOTE,
                "items.note is not NULL and items.note <> '' as item_has_note");

        CONTAINS_FULL_CHEAPEST_PROJECTION_MAP = new HashMap<String, String>(
                CONTAINS_FULL_PROJECTION_MAP);
        CONTAINS_FULL_CHEAPEST_PROJECTION_MAP.put(ContainsFull.ITEM_PRICE,
                "min(itemstores.price) as item_price");

        CONTAINS_FULL_STORE_PROJECTION_MAP = new HashMap<String, String>(
                CONTAINS_FULL_PROJECTION_MAP);
        CONTAINS_FULL_STORE_PROJECTION_MAP.put(ContainsFull.ITEM_PRICE,
                "itemstores.price as item_price");

        UNITS_PROJECTION_MAP = new HashMap<String, String>();
        UNITS_PROJECTION_MAP.put(Units._ID, "units._id");
        UNITS_PROJECTION_MAP.put(Units.CREATED_DATE, "units.created");
        UNITS_PROJECTION_MAP.put(Units.MODIFIED_DATE, "units.modified");
        UNITS_PROJECTION_MAP.put(Units.NAME, "units.name");
        UNITS_PROJECTION_MAP.put(Units.SINGULAR, "units.singular");

        STORES_PROJECTION_MAP = new HashMap<String, String>();
        STORES_PROJECTION_MAP.put(Stores._ID, "stores._id");
        STORES_PROJECTION_MAP.put(Stores.CREATED_DATE, "stores.created");
        STORES_PROJECTION_MAP.put(Stores.MODIFIED_DATE, "stores.modified");
        STORES_PROJECTION_MAP.put(Stores.NAME, "stores.name");
        STORES_PROJECTION_MAP.put(Stores.LIST_ID, "stores.list_id");

        ITEMSTORES_PROJECTION_MAP = new HashMap<String, String>();
        ITEMSTORES_PROJECTION_MAP.put(ItemStores._ID, "itemstores._id");
        ITEMSTORES_PROJECTION_MAP.put(ItemStores.CREATED_DATE,
                "itemstores.created");
        ITEMSTORES_PROJECTION_MAP.put(ItemStores.MODIFIED_DATE,
                "itemstores.modified");
        ITEMSTORES_PROJECTION_MAP.put(ItemStores.ITEM_ID, "itemstores.item_id");
        ITEMSTORES_PROJECTION_MAP.put(ItemStores.STORE_ID,
                "itemstores.store_id");
        ITEMSTORES_PROJECTION_MAP.put(Stores.NAME, "stores.name");
        ITEMSTORES_PROJECTION_MAP.put(ItemStores.AISLE, "itemstores.aisle");
        ITEMSTORES_PROJECTION_MAP.put(ItemStores.PRICE, "itemstores.price");
        ITEMSTORES_PROJECTION_MAP.put(ItemStores.STOCKS_ITEM,
                "itemstores.stocks_item");

        NOTES_PROJECTION_MAP = new HashMap<String, String>();
        NOTES_PROJECTION_MAP.put(ShoppingContract.Notes._ID, "items._id");
        NOTES_PROJECTION_MAP.put(ShoppingContract.Notes.NOTE, "items.note");
        NOTES_PROJECTION_MAP.put(ShoppingContract.Notes.TITLE, "null as title");
        NOTES_PROJECTION_MAP.put(ShoppingContract.Notes.TAGS, "null as tags");
        NOTES_PROJECTION_MAP.put(ShoppingContract.Notes.ENCRYPTED,
                "null as encrypted");
        NOTES_PROJECTION_MAP.put(ShoppingContract.Notes.THEME, "null as theme");

        SUBTOTALS_PROJECTION_MAP = new HashMap<String, String>();
        SUBTOTALS_PROJECTION_MAP.put(ShoppingContract.Subtotals.COUNT,
                "count() as count");
        SUBTOTALS_PROJECTION_MAP.put(ShoppingContract.Subtotals.PRIORITY,
                "priority");
        SUBTOTALS_PROJECTION_MAP.put(ShoppingContract.Subtotals.SUBTOTAL,
                "sum(qty_price) as subtotal");
        SUBTOTALS_PROJECTION_MAP.put(ShoppingContract.Subtotals.STATUS,
                "status");
    }

    private ShoppingDatabase mOpenHelper;

    public static String escapeSQLChars(String trapped) {
        /*
        In order for this method to work properly, the query using the result must use '`' as its
        Escape character.
        */
        return trapped.replaceAll("'", "''").replaceAll("`", "``").replaceAll("%", "`%").replaceAll("_", "`_");
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new ShoppingDatabase(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri url, String[] projection, String selection,
                        String[] selectionArgs, String sort) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        long list_id = -1;

        if (debug) {
            Log.d(TAG, "Query for URL: " + url);
        }

        String defaultOrderBy = null;
        String groupBy = null;

        switch (URL_MATCHER.match(url)) {

            case ITEMS:
                qb.setTables("items");
                qb.setProjectionMap(ITEMS_PROJECTION_MAP);
                defaultOrderBy = Items.DEFAULT_SORT_ORDER;
                break;

            case ITEM_ID:
                qb.setTables("items");
                qb.appendWhere("_id=" + url.getPathSegments().get(1));
                break;

            case LISTS:
                qb.setTables("lists");
                qb.setProjectionMap(LISTS_PROJECTION_MAP);
                defaultOrderBy = Lists.DEFAULT_SORT_ORDER;
                break;

            case LIST_ID:
                qb.setTables("lists");
                qb.appendWhere("_id=" + url.getPathSegments().get(1));
                break;

            case CONTAINS:
                qb.setTables("contains");
                qb.setProjectionMap(CONTAINS_PROJECTION_MAP);
                defaultOrderBy = Contains.DEFAULT_SORT_ORDER;
                break;

            case CONTAINS_ID:
                qb.setTables("contains");
                qb.appendWhere("_id=" + url.getPathSegments().get(1));
                break;

            case CONTAINS_FULL:

                boolean inSearchMode = appIsInSearchMode();

                // all callers pass list id as selection_args[0]. perhaps not so
                // nice to depend on that, but... need to choose the projection map
                // based on the list's store filter.
                if (!inSearchMode
                        && PreferenceActivity.getUsingFiltersFromPrefs(getContext())
                        && listUsesStoreFilter(selectionArgs[0])) {
                    // actually there are two ways we could do the query when
                    // filtering by stores. perhaps
                    // we should offer both. for now choose the first one...
                    if (true) {
                        // show only items which have corresponding records in
                        // itemstores
                        qb.setTables("contains, items, lists, itemstores");
                        qb.setProjectionMap(CONTAINS_FULL_STORE_PROJECTION_MAP);
                        qb.appendWhere("contains.item_id = items._id AND "
                                + "contains.list_id = lists._id AND "
                                + "items._id = itemstores.item_id AND "
                                + "lists.store_filter = itemstores.store_id");
                    } else {
                        // this query is not quite right, but the idea is
                        // show all items, but only show prices from the selected
                        // store.
                        qb.setTables("contains, items, lists left outer join itemstores on (items._id = itemstores.item_id)");
                        qb.setProjectionMap(CONTAINS_FULL_STORE_PROJECTION_MAP);
                        qb.appendWhere("contains.item_id = items._id AND "
                                + "contains.list_id = lists._id AND "
                                + "items._id = itemstores.item_id AND "
                                + "lists.store_filter = itemstores.store_id");
                    }

                } else if (PreferenceActivity
                        .getUsingPerStorePricesFromPrefs(getContext())) {
                    qb.setTables("contains, items, lists left outer join itemstores on (items._id = itemstores.item_id)");
                    qb.setProjectionMap(CONTAINS_FULL_CHEAPEST_PROJECTION_MAP);
                    qb.appendWhere("contains.item_id = items._id AND "
                            + "contains.list_id = lists._id");
                    groupBy = "items._id";

                } else {
                    qb.setTables("contains, items, lists");
                    qb.setProjectionMap(CONTAINS_FULL_PROJECTION_MAP);
                    qb.appendWhere("contains.item_id = items._id AND "
                            + "contains.list_id = lists._id");

                }
                defaultOrderBy = ContainsFull.DEFAULT_SORT_ORDER;
                String tagFilter = getListTagsFilter(selectionArgs[0]);
                if (!inSearchMode && !TextUtils.isEmpty(tagFilter)) {
                    qb.appendWhere(" AND items.tags like '%" + escapeSQLChars(tagFilter) + "%' ESCAPE '`'");
                }
                break;

            case CONTAINS_FULL_ID:
                qb.setTables("contains, items, lists");
                qb.appendWhere("_id=" + url.getPathSegments().get(1));
                qb.appendWhere("contains.item_id = items._id AND "
                        + "contains.list_id = lists._id");
                break;

            case CONTAINS_FULL_LISTID:
                list_id = Long.parseLong(url.getPathSegments().get(2));
                qb.setTables("contains, items, lists");
                qb.appendWhere("contains.item_id = items._id AND " +
                        "contains.list_id = lists._id AND " +
                        "lists._id = " + list_id);
                break;

            case STORES:
                qb.setTables("stores");
                qb.setProjectionMap(STORES_PROJECTION_MAP);
                break;

            case STORES_ID:
                qb.setTables("stores");
                qb.appendWhere("_id=" + url.getPathSegments().get(1));
                break;

            case STORES_LISTID:
                qb.setTables("stores");
                qb.setProjectionMap(STORES_PROJECTION_MAP);
                qb.appendWhere("list_id=" + url.getPathSegments().get(1));
                break;

            case TAGS_LISTID:
                // this is for querying tags regardless of filters.
                // might want to restrict the projection map.
                qb.setTables("contains, items, lists");
                qb.setProjectionMap(CONTAINS_FULL_PROJECTION_MAP);
                qb.appendWhere("contains.item_id = items._id AND "
                        + "contains.list_id = lists._id AND " + "contains.list_id="
                        + url.getPathSegments().get(1));
                groupBy = "items.tags";
                break;

            case ITEMSTORES:
                qb.setTables("itemstores, items, stores");
                qb.setProjectionMap(ITEMSTORES_PROJECTION_MAP);
                qb.appendWhere("itemstores.item_id = items._id AND itemstores.store_id = stores._id");
                break;

            case ITEMSTORES_ID:
                qb.setTables("itemstores, items, stores");
                qb.appendWhere("_id=" + url.getPathSegments().get(1));
                qb.appendWhere("itemstores.item_id = items._id AND itemstores.store_id = stores._id");
                break;

            case ITEMSTORES_ITEMID:
                // path segment 1 is "item", path segment 2 is item id, path segment
                // 3 is list id.
                qb.setTables("stores left outer join itemstores on (stores._id = itemstores.store_id and "
                        + "itemstores.item_id = "
                        + url.getPathSegments().get(2)
                        + ")");
                qb.appendWhere("stores.list_id = " + url.getPathSegments().get(3));
                break;

            case NOTES:
                qb.setTables("items");
                qb.setProjectionMap(NOTES_PROJECTION_MAP);
                break;

            case NOTE_ID:
                qb.setTables("items");
                qb.setProjectionMap(NOTES_PROJECTION_MAP);
                qb.appendWhere("_id=" + url.getPathSegments().get(1));
                break;

            case UNITS:
                qb.setTables("units");
                qb.setProjectionMap(UNITS_PROJECTION_MAP);
                break;

            case UNITS_ID:
                qb.setTables("units");
                qb.setProjectionMap(UNITS_PROJECTION_MAP);
                qb.appendWhere("_id=" + url.getPathSegments().get(1));
                break;

            case ACTIVELIST:
                MatrixCursor m = new MatrixCursor(projection);
                // assumes only one projection will ever be used,
                // asking only for the id of the active list.
                SharedPreferences sp = getContext().getSharedPreferences(
                        "org.openintents.shopping_preferences",
                        Context.MODE_PRIVATE);
                list_id = sp.getInt("lastused", 1);
                m.addRow(new Object[]{Long.toString(list_id)});
                return m;
            case PREFS:
                m = new MatrixCursor(projection);
                // assumes only one projection will ever be used,
                // asking only for the id of the active list.
                String sortOrder = PreferenceActivity.getSortOrderFromPrefs(
                        getContext(), ShoppingItemsView.MODE_IN_SHOP);
                m.addRow(new Object[]{sortOrder});
                return m;

            case SUBTOTALS_LISTID:
                list_id = Long.parseLong(url.getPathSegments().get(1));
                // FALLTHROUGH
            case SUBTOTALS:
                if (list_id == -1) {
                    // this gets the wrong answer if user has switched lists in this
                    // session.
                    sp = getContext().getSharedPreferences(
                            "org.openintents.shopping_preferences",
                            Context.MODE_PRIVATE);
                    list_id = sp.getInt("lastused", 1);
                }
                qb.setProjectionMap(SUBTOTALS_PROJECTION_MAP);
                groupBy = "priority, status";
                if (PreferenceActivity
                        .getUsingPerStorePricesFromPrefs(getContext())) {
                    // status added to "group by" to cover the case where there are
                    // no store prices
                    // for any checked items. still need to count them separately so
                    // Clean List
                    // can be ungreyed.
                    qb.setTables("(SELECT (min(itemstores.price) * case when ((contains.quantity is null) or (length(contains.quantity) = 0)) then 1 else contains.quantity end) as qty_price, "
                            + "contains.status as status, contains.priority as priority FROM contains, items left outer join itemstores on (items._id = itemstores.item_id) "
                            + "WHERE (contains.item_id = items._id AND contains.list_id = "
                            + list_id
                            + " ) AND contains.status != 3 GROUP BY itemstores.item_id, status) ");

                } else {
                    qb.setTables("(SELECT (items.price * case when ((contains.quantity is null) or (length(contains.quantity) = 0)) then 1 else contains.quantity end) as qty_price, "
                            + "contains.status as status, contains.priority as priority FROM contains, items "
                            + "WHERE (contains.item_id = items._id AND contains.list_id = "
                            + list_id + " ) AND contains.status != 3) ");

                }
                break;

            case CONTAINS_COPYOFID:
                long oldContainsId = Long.parseLong(url.getPathSegments().get(2));
                return copyItemAndContains(projection, oldContainsId);

            default:
                throw new IllegalArgumentException("Unknown URL " + url);
        }

        // If no sort order is specified use the default

        String orderBy;
        if (TextUtils.isEmpty(sort)) {
            orderBy = defaultOrderBy;
        } else {
            orderBy = sort;
        }

        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        if (debug) {
            String qs = qb.buildQuery(projection, selection, null, groupBy,
                    null, orderBy, null);
            Log.d(TAG, "Query : " + qs);
        }

        Cursor c = qb.query(db, projection, selection, selectionArgs, groupBy,
                null, orderBy);
        c.setNotificationUri(getContext().getContentResolver(), url);
        return c;
    }

    private boolean listUsesStoreFilter(String listId) {
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables("lists");
        qb.appendWhere("_id=" + listId);
        Cursor c = qb.query(db, new String[]{Lists.STORE_FILTER}, null,
                null, null, null, null);
        if (c.getCount() != 1) {
            return false;
        }

        c.moveToFirst();
        long storeId = c.getLong(0);
        c.deactivate();
        c.close();

        return (storeId != -1);
    }

    private String getListTagsFilter(String listId) {
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables("lists");
        qb.appendWhere("_id=" + listId);
        Cursor c = qb.query(db, new String[]{Lists.TAGS_FILTER}, null, null,
                null, null, null);
        if (c.getCount() != 1) {
            return null;
        }

        c.moveToFirst();
        String tag = c.getString(0);
        c.deactivate();
        c.close();

        return (tag);
    }

    private boolean appIsInSearchMode() {
        SharedPreferences sp = getContext().getSharedPreferences(
                "org.openintents.shopping_preferences",
                Context.MODE_PRIVATE);
        return sp.getBoolean("_searching", false);
    }

    // caller wants us to copy the item and the contains record.
    // only supported projection is item_id, contains_id of the copy.
    private Cursor copyItemAndContains(String[] projection, long oldContainsId) {
        long oldItemId, containsCopyId, itemCopyId;
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        // find the item id from the contains record
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables("contains");
        qb.appendWhere("_id=" + oldContainsId);
        Cursor c = qb.query(db, new String[]{Contains.ITEM_ID}, null, null,
                null, null, null);
        if (c.getCount() != 1) {
            return null;
        }

        c.moveToFirst();
        oldItemId = c.getLong(0);
        c.deactivate();
        c.close();

        // read the item
        qb = new SQLiteQueryBuilder();
        qb.setTables("items");
        qb.appendWhere("_id=" + oldItemId);
        c = qb.query(db, Items.PROJECTION_TO_COPY, null, null, null, null, null);
        if (c.getCount() != 1) {
            return null;
        }
        c.moveToFirst();
        ContentValues itemValues = new ContentValues();
        DatabaseUtils.cursorRowToContentValues(c, itemValues);
        c.deactivate();
        c.close();

        // read the contains record
        qb = new SQLiteQueryBuilder();
        qb.setTables("contains");
        qb.appendWhere("_id=" + oldContainsId);
        c = qb.query(db, Contains.PROJECTION_TO_COPY, null, null, null, null,
                null);
        if (c.getCount() != 1) {
            return null;
        }
        c.moveToFirst();
        ContentValues containsValues = new ContentValues();
        DatabaseUtils.cursorRowToContentValues(c, containsValues);
        c.deactivate();
        c.close();

        // insert the item copy
        validateItemValues(itemValues);
        itemCopyId = db.insert("items", "items", itemValues);

        // insert the contains record copy
        containsValues.put(Contains.ITEM_ID, itemCopyId);
        validateContainsValues(containsValues);
        containsCopyId = db.insert("contains", "contains", containsValues);

        // not sure, should we also copy ItemStores records?

        MatrixCursor m = new MatrixCursor(projection);
        m.addRow(new Object[]{Long.toString(itemCopyId),
                Long.toString(containsCopyId)});
        return m;
    }

    @Override
    public Uri insert(Uri url, ContentValues initialValues) {
        ContentValues values;
        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }

        // insert is supported for items or lists
        switch (URL_MATCHER.match(url)) {
            case ITEMS:
            case NOTES:
                return insertItem(url, values);

            case LISTS:
                return insertList(url, values);

            case CONTAINS:
                return insertContains(url, values);

            case CONTAINS_FULL:
                throw new IllegalArgumentException("Insert not supported for "
                        + url + ", use CONTAINS instead of CONTAINS_FULL.");

            case STORES:
                return insertStore(url, values);

            case ITEMSTORES:
                return insertItemStore(url, values);

            case UNITS:
                return insertUnits(url, values);

            default:
                throw new IllegalArgumentException("Unknown URL " + url);
        }
    }

    private Uri insertItem(Uri url, ContentValues values) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long rowID;

        validateItemValues(values);

        // TODO: Here we should check, whether item exists already.
        // (see TagsProvider)
        // insert the item.
        rowID = db.insert("items", "items", values);
        if (rowID > 0) {
            Uri uri = ContentUris.withAppendedId(Items.CONTENT_URI, rowID);
            getContext().getContentResolver().notifyChange(uri, null);

            Intent intent = new Intent(ProviderIntents.ACTION_INSERTED);
            intent.setData(uri);
            getContext().sendBroadcast(intent);

            return uri;
        }

        // If everything works, we should not reach the following line:
        throw new SQLException("Failed to insert row into " + url);
    }

    private void validateItemValues(ContentValues values) {
        Long now = Long.valueOf(System.currentTimeMillis());
        // Make sure that the fields are all set
        if (!values.containsKey(Items.NAME)) {
            Resources r = getContext().getResources();
            values.put(Items.NAME, r.getString(R.string.new_item));
        }

        if (!values.containsKey(Items.IMAGE)) {
            values.put(Items.IMAGE, "");
        }

        if (!values.containsKey(Items.CREATED_DATE)) {
            values.put(Items.CREATED_DATE, now);
        }

        if (!values.containsKey(Items.MODIFIED_DATE)) {
            values.put(Items.MODIFIED_DATE, now);
        }

        if (!values.containsKey(Items.ACCESSED_DATE)) {
            values.put(Items.ACCESSED_DATE, now);
        }
    }

    private Uri insertList(Uri url, ContentValues values) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long rowID;

        Long now = Long.valueOf(System.currentTimeMillis());
        Resources r = Resources.getSystem();

        // Make sure that the fields are all set
        if (!values.containsKey(Lists.NAME)) {
            values.put(Lists.NAME, r.getString(R.string.new_list));
        }

        if (!values.containsKey(Lists.IMAGE)) {
            values.put(Lists.IMAGE, "");
        }

        if (!values.containsKey(Lists.CREATED_DATE)) {
            values.put(Lists.CREATED_DATE, now);
        }

        if (!values.containsKey(Lists.MODIFIED_DATE)) {
            values.put(Lists.MODIFIED_DATE, now);
        }

        if (!values.containsKey(Lists.ACCESSED_DATE)) {
            values.put(Lists.ACCESSED_DATE, now);
        }

        if (!values.containsKey(Lists.SHARE_CONTACTS)) {
            values.put(Lists.SHARE_CONTACTS, "");
        }

        if (!values.containsKey(Lists.SKIN_BACKGROUND)) {
            values.put(Lists.SKIN_BACKGROUND, "");
        }

        if (!values.containsKey(Lists.SKIN_FONT)) {
            values.put(Lists.SKIN_FONT, "");
        }

        if (!values.containsKey(Lists.SKIN_COLOR)) {
            values.put(Lists.SKIN_COLOR, 0);
        }

        if (!values.containsKey(Lists.SKIN_COLOR_STRIKETHROUGH)) {
            values.put(Lists.SKIN_COLOR_STRIKETHROUGH, 0xFF006600);
        }

        // TODO: Here we should check, whether item exists already.
        // (see TagsProvider)

        // insert the tag.
        rowID = db.insert("lists", "lists", values);
        if (rowID > 0) {
            Uri uri = ContentUris.withAppendedId(Lists.CONTENT_URI, rowID);
            getContext().getContentResolver().notifyChange(uri, null);

            Intent intent = new Intent(ProviderIntents.ACTION_INSERTED);
            intent.setData(uri);
            getContext().sendBroadcast(intent);

            return uri;
        }

        // If everything works, we should not reach the following line:
        throw new SQLException("Failed to insert row into " + url);

    }

    private Uri insertContains(Uri url, ContentValues values) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        // Make sure that the fields are all set
        if (!(values.containsKey(Contains.ITEM_ID) && values
                .containsKey(Contains.LIST_ID))) {
            // At least these values should exist.
            throw new SQLException("Failed to insert row into " + url
                    + ": ITEM_ID and LIST_ID must be given.");
        }

        // TODO: Check here that ITEM_ID and LIST_ID
        // actually exist in the tables.
        if (!values.containsKey(Contains.STATUS)) {
            values.put(Contains.STATUS, Status.WANT_TO_BUY);
        } else {
            // Check here that STATUS is valid.
            long s = values.getAsInteger(Contains.STATUS);
            if (!Status.isValid(s)) {
                throw new SQLException("Failed to insert row into " + url
                        + ": Status " + s + " is not valid.");
            }
        }

        validateContainsValues(values);

        // TODO: Here we should check, whether item exists already.
        // (see TagsProvider)

        // insert the item.
        long rowId = db.insert("contains", "contains", values);
        if (rowId > 0) {
            Uri uri = ContentUris.withAppendedId(Contains.CONTENT_URI, rowId);
            getContext().getContentResolver().notifyChange(uri, null);

            Intent intent = new Intent(ProviderIntents.ACTION_INSERTED);
            intent.setData(uri);
            getContext().sendBroadcast(intent);

            return uri;
        }

        // If everything works, we should not reach the following line:
        throw new SQLException("Failed to insert row into " + url);
    }

    private void validateContainsValues(ContentValues values) {
        Long now = Long.valueOf(System.currentTimeMillis());

        if (!values.containsKey(Contains.CREATED_DATE)) {
            values.put(Contains.CREATED_DATE, now);
        }
        if (!values.containsKey(Contains.MODIFIED_DATE)) {
            values.put(Contains.MODIFIED_DATE, now);
        }

        if (!values.containsKey(Contains.ACCESSED_DATE)) {
            values.put(Contains.ACCESSED_DATE, now);
        }

        if (!values.containsKey(Contains.SHARE_CREATED_BY)) {
            values.put(Contains.SHARE_CREATED_BY, "");
        }

        if (!values.containsKey(Contains.SHARE_MODIFIED_BY)) {
            values.put(Contains.SHARE_MODIFIED_BY, "");
        }

    }

    private Uri insertStore(Uri url, ContentValues values) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long rowID;

        Long now = Long.valueOf(System.currentTimeMillis());

        // Make sure that the fields are all set
        if (!values.containsKey(Stores.NAME)) {
            throw new SQLException("Failed to insert row into " + url
                    + ": Store NAME must be given.");
        }

        if (!values.containsKey(Stores.CREATED_DATE)) {
            values.put(Stores.CREATED_DATE, now);
        }

        if (!values.containsKey(Stores.MODIFIED_DATE)) {
            values.put(Stores.MODIFIED_DATE, now);
        }

        // TODO: Here we should check, whether item exists already.
        // (see TagsProvider)

        // insert the tag.
        rowID = db.insert("stores", "stores", values);
        if (rowID > 0) {
            Uri uri = ContentUris.withAppendedId(Stores.CONTENT_URI, rowID);
            getContext().getContentResolver().notifyChange(uri, null);

            Intent intent = new Intent(ProviderIntents.ACTION_INSERTED);
            intent.setData(uri);
            getContext().sendBroadcast(intent);

            return uri;
        }

        // If everything works, we should not reach the following line:
        throw new SQLException("Failed to insert row into " + url);

    }

    private Uri insertItemStore(Uri url, ContentValues values) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        Long now = Long.valueOf(System.currentTimeMillis());

        // Make sure that the fields are all set
        if (!(values.containsKey(ItemStores.ITEM_ID) && values
                .containsKey(ItemStores.STORE_ID))) {
            // At least these values should exist.
            throw new SQLException("Failed to insert row into " + url
                    + ": ITEM_ID and STORE_ID must be given.");
        }

        // TODO: Check here that ITEM_ID and STORE_ID
        // actually exist in the tables.

        if (!values.containsKey(ItemStores.PRICE)) {
            values.put(ItemStores.PRICE, -1);
        }
        if (!values.containsKey(ItemStores.AISLE)) {
            values.putNull(ItemStores.AISLE);
        }

        if (!values.containsKey(ItemStores.CREATED_DATE)) {
            values.put(ItemStores.CREATED_DATE, now);
        }

        if (!values.containsKey(ItemStores.MODIFIED_DATE)) {
            values.put(ItemStores.MODIFIED_DATE, now);
        }

        // TODO: Here we should check, whether item exists already.
        // (see TagsProvider)

        // insert the item.
        long rowId = db.insert("itemstores", "itemstores", values);
        if (rowId > 0) {
            Uri uri = ContentUris.withAppendedId(ItemStores.CONTENT_URI, rowId);
            getContext().getContentResolver().notifyChange(uri, null);

            Intent intent = new Intent(ProviderIntents.ACTION_INSERTED);
            intent.setData(uri);
            getContext().sendBroadcast(intent);

            return uri;
        }

        // If everything works, we should not reach the following line:
        throw new SQLException("Failed to insert row into " + url);
    }

    private Uri insertUnits(Uri url, ContentValues values) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long rowID;

        Long now = Long.valueOf(System.currentTimeMillis());

        // Make sure that the fields are all set
        if (!values.containsKey(Units.NAME)) {
            throw new SQLException("Failed to insert row into " + url
                    + ": Units NAME must be given.");
        }

        if (!values.containsKey(Units.CREATED_DATE)) {
            values.put(Units.CREATED_DATE, now);
        }

        if (!values.containsKey(Stores.MODIFIED_DATE)) {
            values.put(Units.MODIFIED_DATE, now);
        }

        // TODO: Here we should check, whether item exists already.
        // (see TagsProvider)

        // insert the units.
        rowID = db.insert("units", "units", values);
        if (rowID > 0) {
            Uri uri = ContentUris.withAppendedId(Units.CONTENT_URI, rowID);
            getContext().getContentResolver().notifyChange(uri, null);

            Intent intent = new Intent(ProviderIntents.ACTION_INSERTED);
            intent.setData(uri);
            getContext().sendBroadcast(intent);

            return uri;
        }

        // If everything works, we should not reach the following line:
        throw new SQLException("Failed to insert row into " + url);

    }

    @Override
    public int delete(Uri url, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        long[] affectedRows = null;
        // long rowId;
        switch (URL_MATCHER.match(url)) {
            case ITEMS:
                affectedRows = ProviderUtils.getAffectedRows(db, "items", where,
                        whereArgs);
                count = db.delete("items", where, whereArgs);
                break;

            case ITEM_ID:
                String segment = url.getPathSegments().get(1); // contains rowId
                // rowId = Long.parseLong(segment);
                String whereString;
                if (!TextUtils.isEmpty(where)) {
                    whereString = " AND (" + where + ')';
                } else {
                    whereString = "";
                }

                affectedRows = ProviderUtils.getAffectedRows(db, "items", "_id="
                        + segment + whereString, whereArgs);
                count = db.delete("items", "_id=" + segment + whereString,
                        whereArgs);
                break;

            case LISTS:
                affectedRows = ProviderUtils.getAffectedRows(db, "lists", where,
                        whereArgs);
                count = db.delete("lists", where, whereArgs);
                break;

            case LIST_ID:
                segment = url.getPathSegments().get(1); // contains rowId
                // rowId = Long.parseLong(segment);
                if (!TextUtils.isEmpty(where)) {
                    whereString = " AND (" + where + ')';
                } else {
                    whereString = "";
                }

                affectedRows = ProviderUtils.getAffectedRows(db, "lists", "_id="
                        + segment + whereString, whereArgs);
                count = db.delete("lists", "_id=" + segment + whereString,
                        whereArgs);
                break;

            case CONTAINS:
                affectedRows = ProviderUtils.getAffectedRows(db, "contains", where,
                        whereArgs);
                count = db.delete("contains", where, whereArgs);
                break;

            case CONTAINS_ID:
                segment = url.getPathSegments().get(1); // contains rowId
                // rowId = Long.parseLong(segment);
                if (!TextUtils.isEmpty(where)) {
                    whereString = " AND (" + where + ')';
                } else {
                    whereString = "";
                }

                affectedRows = ProviderUtils.getAffectedRows(db, "contains", "_id="
                        + segment + whereString, whereArgs);
                count = db.delete("contains", "_id=" + segment + whereString,
                        whereArgs);
                break;

            case NOTE_ID:
                // don't delete the row, just the note.
                ContentValues values = new ContentValues();
                values.putNull("note");
                count = update(url, values, null, null);
                break;

            case STORES:
                affectedRows = ProviderUtils.getAffectedRows(db, "stores", where,
                        whereArgs);
                count = db.delete("stores", where, whereArgs);
                break;

            case ITEMSTORES:
                affectedRows = ProviderUtils.getAffectedRows(db, "itemstores",
                        where, whereArgs);
                count = db.delete("itemstores", where, whereArgs);
                break;

            case ITEMSTORES_ID:
                segment = url.getPathSegments().get(1); // contains rowId
                // rowId = Long.parseLong(segment);
                if (!TextUtils.isEmpty(where)) {
                    whereString = " AND (" + where + ')';
                } else {
                    whereString = "";
                }

                affectedRows = ProviderUtils.getAffectedRows(db, "itemstores",
                        "_id=" + segment + whereString, whereArgs);
                count = db.delete("itemstores", "_id=" + segment + whereString,
                        whereArgs);
                break;

            default:
                throw new IllegalArgumentException("Unknown URL " + url);
        }

        getContext().getContentResolver().notifyChange(url, null);

        Intent intent = new Intent(ProviderIntents.ACTION_DELETED);
        intent.setData(url);
        intent.putExtra(ProviderIntents.EXTRA_AFFECTED_ROWS, affectedRows);
        getContext().sendBroadcast(intent);

        return count;
    }

    @Override
    public int update(Uri url, ContentValues values, String where,
                      String[] whereArgs) {
        if (debug) {
            Log.d(TAG, "update called for: " + url);
        }
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        Uri secondUri = null;

        // long rowId;
        switch (URL_MATCHER.match(url)) {
            case ITEMS:
            case NOTES:
                count = db.update("items", values, where, whereArgs);
                break;

            case NOTE_ID:
                // drop some OI Notepad fields on the floor.
                values.remove("title");
                values.remove("encrypted");
                values.remove("theme");
                values.remove("nothing_To_see_here");
                // fall through...
            case ITEM_ID:
                String segment = url.getPathSegments().get(1); // contains rowId
                // rowId = Long.parseLong(segment);
                String whereString;
                if (!TextUtils.isEmpty(where)) {
                    whereString = " AND (" + where + ')';
                } else {
                    whereString = "";
                }

                count = db.update("items", values, "_id=" + segment + whereString,
                        whereArgs);
                secondUri = ShoppingContract.Items.CONTENT_URI;
                break;

            case LISTS:
                count = db.update("lists", values, where, whereArgs);
                break;

            case LIST_ID:
                segment = url.getPathSegments().get(1); // contains rowId
                // rowId = Long.parseLong(segment);
                if (!TextUtils.isEmpty(where)) {
                    whereString = " AND (" + where + ')';
                } else {
                    whereString = "";
                }

                count = db.update("lists", values, "_id=" + segment + whereString,
                        whereArgs);
                break;

            case CONTAINS:
                count = db.update("contains", values, where, whereArgs);
                break;

            case CONTAINS_ID:
                segment = url.getPathSegments().get(1); // contains rowId
                // rowId = Long.parseLong(segment);
                if (!TextUtils.isEmpty(where)) {
                    whereString = " AND (" + where + ')';
                } else {
                    whereString = "";
                }

                count = db.update("contains", values, "_id=" + segment
                        + whereString, whereArgs);
                break;

            case STORES:
                count = db.update("stores", values, where, whereArgs);
                break;

            case STORES_ID:
                segment = url.getPathSegments().get(1); // contains rowId
                // rowId = Long.parseLong(segment);
                if (!TextUtils.isEmpty(where)) {
                    whereString = " AND (" + where + ')';
                } else {
                    whereString = "";
                }
                count = db.update("stores", values, "_id=" + segment + whereString,
                        whereArgs);
                break;

            case ITEMSTORES:
                count = db.update("itemstores", values, where, whereArgs);
                break;

            case ITEMSTORES_ID:
                segment = url.getPathSegments().get(1); // contains rowId
                // rowId = Long.parseLong(segment);
                if (!TextUtils.isEmpty(where)) {
                    whereString = " AND (" + where + ')';
                } else {
                    whereString = "";
                }
                count = db.update("itemstores", values, "_id=" + segment
                        + whereString, whereArgs);
                break;

            case UNITS:
                count = db.update("units", values, where, whereArgs);
                break;

            case UNITS_ID:
                segment = url.getPathSegments().get(1); // contains rowId
                // rowId = Long.parseLong(segment);
                if (!TextUtils.isEmpty(where)) {
                    whereString = " AND (" + where + ')';
                } else {
                    whereString = "";
                }
                count = db.update("units", values, "_id=" + segment + whereString,
                        whereArgs);
                break;

            default:
                Log.e(TAG, "Update received unknown URL: " + url);
                throw new IllegalArgumentException("Unknown URL " + url);
        }

        getContext().getContentResolver().notifyChange(url, null);
        if (secondUri != null) {
            getContext().getContentResolver().notifyChange(secondUri, null);
        }

        Intent intent = new Intent(ProviderIntents.ACTION_MODIFIED);
        intent.setData(url);
        getContext().sendBroadcast(intent);

        return count;
    }

    @Override
    public String getType(Uri url) {
        switch (URL_MATCHER.match(url)) {
            case ITEMS:
                return "vnd.android.cursor.dir/vnd.openintents.shopping.item";

            case ITEM_ID:
                return ShoppingContract.ITEM_TYPE;

            case LISTS:
                return "vnd.android.cursor.dir/vnd.openintents.shopping.list";

            case LIST_ID:
                return "vnd.android.cursor.item/vnd.openintents.shopping.list";

            case CONTAINS:
                return "vnd.android.cursor.dir/vnd.openintents.shopping.contains";

            case CONTAINS_ID:
                return "vnd.android.cursor.item/vnd.openintents.shopping.contains";

            case CONTAINS_FULL:
                return "vnd.android.cursor.dir/vnd.openintents.shopping.containsfull";

            case CONTAINS_FULL_ID:
                return "vnd.android.cursor.item/vnd.openintents.shopping.containsfull";

            case CONTAINS_FULL_LISTID:
                return "vnd.android.cursor.dir/vnd.openintents.shopping.containsfull";

            case STORES:
                return "vnd.android.cursor.dir/vnd.openintents.shopping.stores";

            case STORES_ID:
            case STORES_LISTID:
                return "vnd.android.cursor.item/vnd.openintents.shopping.stores";

            case NOTES:
                return ShoppingContract.Notes.CONTENT_TYPE;
            case NOTE_ID:
                return ShoppingContract.Notes.CONTENT_ITEM_TYPE;

            case ITEMSTORES:
                return "vnd.android.cursor.dir/vnd.openintents.shopping.itemstores";
            case ITEMSTORES_ID:
                return "vnd.android.cursor.item/vnd.openintents.shopping.itemstores";
            case ITEMSTORES_ITEMID:
                return "vnd.android.cursor.dir/vnd.openintents.shopping.itemstores";

            case UNITS:
                return "vnd.android.cursor.dir/vnd.openintents.shopping.units";
            case UNITS_ID:
                return "vnd.android.cursor.item/vnd.openintents.shopping.units";

            case ACTIVELIST:
                // not sure this is quite right
                return "vnd.android.cursor.item/vnd.openintents.shopping.list";

            default:
                throw new IllegalArgumentException("Unknown URL " + url);
        }
    }
}

/*
 * Copyright (C) 2007-2011 OpenIntents.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import android.content.ContentValues;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.openintents.shopping.library.provider.ShoppingContract.Contains;
import org.openintents.shopping.library.provider.ShoppingContract.ItemStores;
import org.openintents.shopping.library.provider.ShoppingContract.Items;
import org.openintents.shopping.library.provider.ShoppingContract.Lists;

public class ShoppingDatabase extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "shopping.db";
    /**
     * Version of database.
     * <p/>
     * The various versions were introduced in the following releases:
     * <p/>
     * 1: Release 0.1.1 2: Release 0.1.6 3: Release 1.0.4-beta 4: Release
     * 1.0.4-beta 5: Release 1.2.7-beta 6: Release 1.2.7-beta 7: Release
     * 1.2.7-beta 8: Release 1.2.7-beta 9: Release 1.3.0 10: Release 1.3.1-beta
     * 11: Release 1.4.0-beta
     */
    private static final int DATABASE_VERSION = 13;

    ShoppingDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Creates tables "items", "lists", and "contains".
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE items (" + "_id INTEGER PRIMARY KEY," // V1
                + "name VARCHAR," // V1
                + "image VARCHAR," // V1
                + "price INTEGER," // V3
                + "units VARCHAR," // V8
                + "tags VARCHAR," // V3
                + "barcode VARCHAR," // V4
                + "location VARCHAR," // V4
                + "note VARCHAR," // V7
                + "due INTEGER," // V4
                + "created INTEGER," // V1
                + "modified INTEGER," // V1
                + "accessed INTEGER" // V1
                + ");");
        db.execSQL("CREATE TABLE lists (" + "_id INTEGER PRIMARY KEY," // V1
                + "name VARCHAR," // V1
                + "image VARCHAR," // V1
                + "created INTEGER," // V1
                + "modified INTEGER," // V1
                + "accessed INTEGER," // V1
                + "share_name VARCHAR," // V2
                + "share_contacts VARCHAR," // V2
                + "skin_background VARCHAR," // V2
                + "skin_font VARCHAR," // V2
                + "skin_color INTEGER," // V2
                + "skin_color_strikethrough INTEGER," // V2
                + "store_filter INTEGER DEFAULT -1," // V12
                + "tags_filter VARCHAR," // V12
                + "items_sort INTEGER" // V13
                + ");");
        db.execSQL("CREATE TABLE contains (" + "_id INTEGER PRIMARY KEY," // V1
                + "item_id INTEGER," // V1
                + "list_id INTEGER," // V1
                + "quantity VARCHAR," // V1
                + "status INTEGER," // V1
                + "created INTEGER," // V1
                + "modified INTEGER," // V1
                + "accessed INTEGER," // V1
                + "share_created_by VARCHAR," // V2
                + "share_modified_by VARCHAR," // V2
                + "sort_key INTEGER," // V3
                + "priority INTEGER" // V6
                + ");");
        db.execSQL("CREATE TABLE stores (" + "_id INTEGER PRIMARY KEY," // V5
                + "name VARCHAR, " // V5
                + "list_id INTEGER," // V5
                + "created INTEGER," // V5
                + "modified INTEGER" // V5
                + ");");
        db.execSQL("CREATE TABLE itemstores(" + "_id INTEGER PRIMARY KEY," // V5
                + "item_id INTEGER," // V5
                + "store_id INTEGER," // V5
                + "stocks_item INTEGER DEFAULT 1," // V10
                + "aisle INTEGER," // V5
                + "price INTEGER," // V5
                + "created INTEGER," // V5
                + "modified INTEGER" // V5
                + ");");
        db.execSQL("CREATE TABLE units (" + "_id INTEGER PRIMARY KEY," // V8
                + "name VARCHAR, " // V8
                + "singular VARCHAR, " // V8
                + "created INTEGER," // V8
                + "modified INTEGER" // V8
                + ");");
        db.execSQL("CREATE INDEX itemstores_item_id on itemstores "
                + " ( item_id asc, price asc );"); // V11
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        Log.w(ShoppingProvider.TAG, "Upgrading database from version "
                + oldVersion + " to " + newVersion + "");
        if (newVersion > oldVersion) {
            // Upgrade
            switch (oldVersion) {
                case 2:
                    // Upgrade from version 2
                    // It seems SQLite3 only allows to add one column at a time,
                    // so we need three SQL statements:
                    try {
                        db.execSQL("ALTER TABLE items ADD COLUMN " + Items.PRICE
                                + " INTEGER;");
                        db.execSQL("ALTER TABLE items ADD COLUMN " + Items.TAGS
                                + " VARCHAR;");
                        db.execSQL("ALTER TABLE contains ADD COLUMN "
                                + Contains.SORT_KEY + " INTEGER;");
                    } catch (SQLException e) {
                        Log.e(ShoppingProvider.TAG, "Error executing SQL: ", e);
                        // If the error is "duplicate column name" then
                        // everything is fine,
                        // as this happens after upgrading 2->3, then
                        // downgrading 3->2,
                        // and then upgrading again 2->3.
                    }
                    // NO break; - fall through for further upgrades.
                case 3:
                    try {
                        db.execSQL("ALTER TABLE items ADD COLUMN " + Items.BARCODE
                                + " VARCHAR;");
                        db.execSQL("ALTER TABLE items ADD COLUMN " + Items.LOCATION
                                + " VARCHAR;");
                        db.execSQL("ALTER TABLE items ADD COLUMN " + Items.DUE_DATE
                                + " INTEGER;");
                    } catch (SQLException e) {
                        Log.e(ShoppingProvider.TAG, "Error executing SQL: ", e);
                        // If the error is "duplicate column name" then
                        // everything is fine,
                        // as this happens after upgrading 2->3, then
                        // downgrading 3->2,
                        // and then upgrading again 2->3.
                    }
                    // NO break; - fall through for further upgrades.
                case 4:
                    try {
                        db.execSQL("CREATE TABLE stores ("
                                + "_id INTEGER PRIMARY KEY," // V5
                                + "name VARCHAR, " // V5
                                + "list_id INTEGER," // V5
                                + "created INTEGER," // V5
                                + "modified INTEGER" // V5
                                + ");");
                        db.execSQL("CREATE TABLE itemstores("
                                + "_id INTEGER PRIMARY KEY," // V5
                                + "item_id INTEGER," // V5
                                + "store_id INTEGER," // V5
                                + "aisle INTEGER," // V5
                                + "price INTEGER," // V5
                                + "created INTEGER," // V5
                                + "modified INTEGER" // V5
                                + ");");
                    } catch (SQLException e) {
                        Log.e(ShoppingProvider.TAG, "Error executing SQL: ", e);
                    }
                    // NO break;
                case 5:
                    try {
                        db.execSQL("ALTER TABLE contains ADD COLUMN "
                                + Contains.PRIORITY + " INTEGER;");
                    } catch (SQLException e) {
                        Log.e(ShoppingProvider.TAG, "Error executing SQL: ", e);
                    }
                    // NO break;
                case 6:
                    try {
                        db.execSQL("ALTER TABLE items ADD COLUMN " + Items.NOTE
                                + " VARCHAR;");
                    } catch (SQLException e) {
                        Log.e(ShoppingProvider.TAG, "Error executing SQL: ", e);
                    }
                    // NO break;
                case 7:
                    try {
                        db.execSQL("ALTER TABLE items ADD COLUMN " + Items.UNITS
                                + " VARCHAR;");
                        db.execSQL("CREATE TABLE units ("
                                + "_id INTEGER PRIMARY KEY," // V8
                                + "name VARCHAR, " // V8
                                + "singular VARCHAR, " // V8
                                + "created INTEGER," // V8
                                + "modified INTEGER" // V8
                                + ");");

                    } catch (SQLException e) {
                        Log.e(ShoppingProvider.TAG, "Error executing SQL: ", e);
                    }
                    // NO break;
                case 8:
                    try {
                        // There is no simple command in sqlite to change the type
                        // of a field.
                        // -> copy the whole table to change type of aisle
                        // from INTEGER to VARCHAR.
                        // (see http://www.sqlite.org/faq.html#q11 )
                        // ("BEGIN TRANSACTION;" and "COMMIT;" are not valid
                        // because we are already within a transaction.)
                        // db.execSQL("CREATE TEMPORARY TABLE itemstores_backup("
                        // + "_id INTEGER PRIMARY KEY," // V5
                        // + "item_id INTEGER," // V5
                        // + "store_id INTEGER," // V5
                        // + "aisle INTEGER," // V5:INTEGER, (V9:VARCHAR)
                        // + "price INTEGER," // V5
                        // + "created INTEGER," // V5
                        // + "modified INTEGER" // V5
                        // + ");");
                        // db.execSQL("INSERT INTO itemstores_backup SELECT "
                        // + "_id,item_id,store_id,aisle,price,created,modified"
                        // + " FROM itemstores;");
                        // db.execSQL("DROP TABLE itemstores;");
                        // db.execSQL("CREATE TABLE itemstores("
                        // + "_id INTEGER PRIMARY KEY," // V5
                        // + "item_id INTEGER," // V5
                        // + "store_id INTEGER," // V5
                        // + "aisle VARCHAR," // (V5:INTEGER), V9
                        // + "price INTEGER," // V5
                        // + "created INTEGER," // V5
                        // + "modified INTEGER" // V5
                        // + ");");
                        // db.execSQL("INSERT INTO itemstores SELECT "
                        // + "_id,item_id,store_id,aisle,price,created,modified"
                        // + " FROM itemstores_backup;");
                        // db.execSQL("DROP TABLE itemstores_backup;");

                        // Replace "-1" values by "".
                        ContentValues values = new ContentValues();
                        values.put(ItemStores.AISLE, "");
                        db.update("itemstores", values, "aisle = '-1'", null);
                    } catch (SQLException e) {
                        Log.e(ShoppingProvider.TAG, "Error executing SQL: ", e);
                    }
                    // NO break;
                case 9:
                    try {
                        db.execSQL("ALTER TABLE itemstores ADD COLUMN "
                                + ItemStores.STOCKS_ITEM + " INTEGER DEFAULT 1;");

                    } catch (SQLException e) {
                        Log.e(ShoppingProvider.TAG, "Error executing SQL: ", e);
                    }
                    // NO break;
                case 10:
                    db.execSQL("CREATE INDEX IF NOT EXISTS "
                            + " itemstores_item_id on itemstores "
                            + " ( item_id asc, price asc );"); // V11
                    // NO break;
                case 11:
                    try {
                        db.execSQL("ALTER TABLE lists ADD COLUMN "
                                + Lists.STORE_FILTER + " INTEGER DEFAULT -1;");
                        db.execSQL("ALTER TABLE lists ADD COLUMN "
                                + Lists.TAGS_FILTER + " VARCHAR;");
                    } catch (SQLException e) {
                        Log.e(ShoppingProvider.TAG, "Error executing SQL: ", e);
                    }
                    // NO break;
                case 12:
                    try {
                        db.execSQL("ALTER TABLE lists ADD COLUMN "
                                + Lists.ITEMS_SORT + " INTEGER;");
                    } catch (SQLException e) {
                        Log.e(ShoppingProvider.TAG, "Error executing SQL: ", e);
                    }
                    // NO break;

                    // NO break UNTIL HERE!
                    break;
                default:
                    Log.w(ShoppingProvider.TAG, "Unknown version " + oldVersion
                            + ". Creating new database.");
                    db.execSQL("DROP TABLE IF EXISTS items");
                    db.execSQL("DROP TABLE IF EXISTS lists");
                    db.execSQL("DROP TABLE IF EXISTS contains");
                    db.execSQL("DROP TABLE IF EXISTS stores");
                    db.execSQL("DROP TABLE IF EXISTS itemstores");
                    db.execSQL("DROP TABLE IF EXISTS units");
                    onCreate(db);
            }
        } else { // newVersion <= oldVersion
            // Downgrade
            Log.w(ShoppingProvider.TAG,
                    "Don't know how to downgrade. Will not touch database and hope they are compatible.");
            // Do nothing.
        }

    }
}
/*
 * Copyright (C) 2007-2010 OpenIntents.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import android.app.AlertDialog;
import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.support.v4.view.ActionProvider;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.WrapperListAdapter;

import org.openintents.OpenIntents;
import org.openintents.convertcsv.shoppinglist.ConvertCsvActivity;
import org.openintents.distribution.DistributionLibraryFragmentActivity;
import org.openintents.distribution.DownloadOIAppDialog;
import org.openintents.intents.GeneralIntents;
import org.openintents.intents.ShoppingListIntents;
import org.openintents.provider.Alert;
import org.openintents.shopping.LogConstants;
import org.openintents.shopping.R;
import org.openintents.shopping.ShoppingApplication;
import org.openintents.shopping.library.provider.ShoppingContract;
import org.openintents.shopping.library.provider.ShoppingContract.Contains;
import org.openintents.shopping.library.provider.ShoppingContract.ContainsFull;
import org.openintents.shopping.library.provider.ShoppingContract.Items;
import org.openintents.shopping.library.provider.ShoppingContract.Lists;
import org.openintents.shopping.library.provider.ShoppingContract.Status;
import org.openintents.shopping.library.provider.ShoppingContract.Stores;
import org.openintents.shopping.library.util.PriceConverter;
import org.openintents.shopping.library.util.ShoppingUtils;
import org.openintents.shopping.ui.dialog.DialogActionListener;
import org.openintents.shopping.ui.dialog.EditItemDialog;
import org.openintents.shopping.ui.dialog.EditItemDialog.FieldType;
import org.openintents.shopping.ui.dialog.EditItemDialog.OnItemChangedListener;
import org.openintents.shopping.ui.dialog.NewListDialog;
import org.openintents.shopping.ui.dialog.RenameListDialog;
import org.openintents.shopping.ui.dialog.ThemeDialog;
import org.openintents.shopping.ui.dialog.ThemeDialog.ThemeDialogListener;
import org.openintents.shopping.ui.widget.QuickSelectMenu;
import org.openintents.shopping.ui.widget.ShoppingItemsView;
import org.openintents.shopping.ui.widget.ShoppingItemsView.ActionBarListener;
import org.openintents.shopping.ui.widget.ShoppingItemsView.DragListener;
import org.openintents.shopping.ui.widget.ShoppingItemsView.DropListener;
import org.openintents.shopping.ui.widget.ShoppingItemsView.OnCustomClickListener;
import org.openintents.shopping.widgets.CheckItemsWidget;
import org.openintents.util.MenuIntentOptionsWithIcons;
import org.openintents.util.ShakeSensorListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import static org.openintents.shopping.ui.widget.ShoppingItemsView.MODE_IN_SHOP;

/**
 * Displays a shopping list.
 */
public class ShoppingActivity extends DistributionLibraryFragmentActivity
        implements ThemeDialogListener, OnCustomClickListener,
        ActionBarListener, OnItemChangedListener,
        UndoListener, ShoppingItemsView.OnModeChangeListener { // implements
    // AdapterView.OnItemClickListener
    // {

    public static final int DIALOG_GET_FROM_MARKET = 7;
    public static final int LOADER_TOTALS = 0;
    public static final int LOADER_ITEMS = 1;

    public static final String[] PROJECTION_ITEMS = new String[]{
            ContainsFull._ID, ContainsFull.ITEM_NAME, ContainsFull.ITEM_IMAGE,
            ContainsFull.ITEM_TAGS, ContainsFull.ITEM_PRICE,
            ContainsFull.QUANTITY, ContainsFull.STATUS, ContainsFull.ITEM_ID,
            ContainsFull.SHARE_CREATED_BY, ContainsFull.SHARE_MODIFIED_BY,
            ContainsFull.PRIORITY, ContainsFull.ITEM_HAS_NOTE,
            ContainsFull.ITEM_UNITS};
    public static final int mStringItemsITEMNAME = 1;
    public static final int mStringItemsITEMTAGS = 3;
    public static final int mStringItemsITEMPRICE = 4;
    public static final int mStringItemsQUANTITY = 5;
    public static final int mStringItemsSTATUS = 6;
    public static final int mStringItemsITEMID = 7;
    public static final int mStringItemsPRIORITY = 10;
    public static final int mStringItemsITEMHASNOTE = 11;
    public static final int mStringItemsITEMUNITS = 12;
    /**
     * TAG for logging.
     */
    private static final String TAG = "ShoppingActivity";
    private static final boolean debug = LogConstants.debug;
    private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_MAX_OFF_PATH = 250;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;
    private static final int MENU_NEW_LIST = Menu.FIRST;
    private static final int MENU_CLEAN_UP_LIST = Menu.FIRST + 1;
    private static final int MENU_DELETE_LIST = Menu.FIRST + 2;
    private static final int MENU_SHARE = Menu.FIRST + 3;
    private static final int MENU_THEME = Menu.FIRST + 4;
    private static final int MENU_ADD_LOCATION_ALERT = Menu.FIRST + 5;
    private static final int MENU_RENAME_LIST = Menu.FIRST + 6;
    private static final int MENU_MARK_ITEM = Menu.FIRST + 7;
    private static final int MENU_EDIT_ITEM = Menu.FIRST + 8; // includes rename
    private static final int MENU_DELETE_ITEM = Menu.FIRST + 9;
    private static final int MENU_INSERT_FROM_EXTRAS = Menu.FIRST + 10;
    private static final int MENU_COPY_ITEM = Menu.FIRST + 11;
    private static final int MENU_SORT_LIST = Menu.FIRST + 12;
    private static final int MENU_SEARCH_ADD = Menu.FIRST + 13;
    // TODO: obsolete pick items button, now in drawer
    private static final int MENU_PICK_ITEMS = Menu.FIRST + 14;
    // TODO: Implement "select list" action
    // that can be called by other programs.
    // private static final int MENU_SELECT_LIST = Menu.FIRST + 15; // select a
    // shopping list
    private static final int MENU_PREFERENCES = Menu.FIRST + 17;
    private static final int MENU_SEND = Menu.FIRST + 18;
    private static final int MENU_REMOVE_ITEM_FROM_LIST = Menu.FIRST + 19;
    private static final int MENU_MOVE_ITEM = Menu.FIRST + 20;
    private static final int MENU_MARK_ALL_ITEMS = Menu.FIRST + 21;
    private static final int MENU_ITEM_STORES = Menu.FIRST + 22;
    private static final int MENU_UNMARK_ALL_ITEMS = Menu.FIRST + 23;
    private static final int MENU_SYNC_WEAR = Menu.FIRST + 25;
    private static final int MENU_CONVERT_CSV = Menu.FIRST + 26;
    private static final int MENU_DISTRIBUTION_START = Menu.FIRST + 100; // MUST BE LAST
    private static final int DIALOG_ABOUT = 1;
    // private static final int DIALOG_TEXT_ENTRY = 2;
    private static final int DIALOG_NEW_LIST = 2;
    private static final int DIALOG_RENAME_LIST = 3;
    private static final int DIALOG_EDIT_ITEM = 4;
    private static final int DIALOG_DELETE_ITEM = 5;
    private static final int DIALOG_THEME = 6;
    private static final int DIALOG_DISTRIBUTION_START = 100; // MUST BE LAST
    private static final int REQUEST_CODE_CATEGORY_ALTERNATIVE = 1;
    private static final int REQUEST_PICK_LIST = 2;
    /**
     * The main activity.
     * <p/>
     * Displays the shopping list that was used last time.
     */
    private static final int STATE_MAIN = 0;
    /**
     * VIEW action on a item/list URI.
     */
    private static final int STATE_VIEW_LIST = 1;
    /**
     * PICK action on an dir/item URI.
     */
    private static final int STATE_PICK_ITEM = 2;
    /**
     * GET_CONTENT action on an item/item URI.
     */
    private static final int STATE_GET_CONTENT_ITEM = 3;
    /**
     * Definition of the requestCode for the subactivity.
     */
    static final private int SUBACTIVITY_LIST_SHARE_SETTINGS = 0;
    /**
     * Definition for message handler:
     */
    static final private int MESSAGE_UPDATE_CURSORS = 1;
    private static final String[] mStringListFilter = new String[]{Lists._ID,
            Lists.NAME, Lists.IMAGE, Lists.SHARE_NAME, Lists.SHARE_CONTACTS,
            Lists.SKIN_BACKGROUND};
    private static final int mStringListFilterID = 0;
    private static final int mStringListFilterNAME = 1;
    private static final int mStringListFilterIMAGE = 2;
    private static final int mStringListFilterSHARENAME = 3;
    private static final int mStringListFilterSHARECONTACTS = 4;
    private static final int mStringListFilterSKINBACKGROUND = 5;
    private static final int mStringItemsCONTAINSID = 0;
    private static final int mStringItemsITEMIMAGE = 2;
    private static final int mStringItemsSHARECREATEDBY = 8;
    private static final int mStringItemsSHAREMODIFIEDBY = 9;
    // private static final String BUNDLE_TEXT_ENTRY_MENU = "text entry menu";
    // private static final String BUNDLE_CURSOR_ITEMS_POSITION =
    // "cursor items position";
    private static final String BUNDLE_ITEM_URI = "item uri";
    private static final String BUNDLE_RELATION_URI = "relation_uri";
    private static final String BUNDLE_MODE = "mode";

    // private Cursor mCursorItems;
    private static final String BUNDLE_MODE_BEFORE_SEARCH = "mode_before_search";
    // TODO: Set up state information for onFreeze(), ...
    // State data to be stored when freezing:
    private final String ORIGINAL_ITEM = "original item";
    private ItemsFromExtras mItemsFromExtras = new ItemsFromExtras();
    private ToggleBoughtInputMethod toggleBoughtInputMethod;
    /**
     * Current state
     */
    private int mState;
    /*
     * Value of PreferenceActivity.updateCount last time we called fillItems().
     */
    private int lastAppliedPrefChange = -1;
    private boolean mEditingFilter;
    /**
     * URI of current list
     */
    private Uri mListUri;
    /**
     * URI of selected item
     */
    private Uri mItemUri;
    /**
     * URI of current list and item
     */
    private Uri mListItemUri;
    /**
     * Update interval for automatic requires.
     * <p/>
     * (Workaround since ContentObserver does not work.)
     */
    private int mUpdateInterval;
    private boolean mUpdating;
    /**
     * Private members connected to list of shopping lists
     */
    // Temp - making it generic for tablet compatibility
    private AdapterView mShoppingListsView;
    private Cursor mCursorShoppingLists;
    private ShoppingItemsView mItemsView;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerListsView;
    private ActionBarDrawerToggle mDrawerToggle;
    private CharSequence mTitle, mDrawerTitle, mSubTitle;
    private LinearLayout.LayoutParams mLayoutParamsItems;
    private int mAllowedListHeight; // Height for the list allowed in this view.
    private AutoCompleteTextView mEditText;
    private Button mButton;
    private View mAddPanel;
    private Button mStoresFilterButton;
    private Button mTagsFilterButton;
    private Button mShoppingListsFilterButton;
    private String mSortOrder;

    private ListSortActionProvider mListSortActionProvider;

    // Skins --------------------------

    /**
     * Remember position for screen orientation change.
     */
    // int mEditItemPosition = -1;

    // public int mPriceVisibility;
    // private int mTagsVisibility;
    private SensorManager mSensorManager;
    private SensorEventListener mMySensorListener = new ShakeSensorListener() {

        @Override
        public void onShake() {
            // Provide some visual feedback.
            Animation shake = AnimationUtils.loadAnimation(
                    ShoppingActivity.this, R.anim.shake);
            findViewById(R.id.background).startAnimation(shake);

            cleanupList();
        }

    };

    /**
     * isActive is true only after onResume() and before onPause().
     */
    private boolean mIsActive;

    /**
     * Whether to use the sensor for shake.
     */
    private boolean mUseSensor;
    private Uri mRelationUri;
    private int mMoveItemPosition;

    private EditItemDialog.FieldType mEditItemFocusField = EditItemDialog.FieldType.ITEMNAME;
    private GestureDetector mGestureDetector;
    private View.OnTouchListener mGestureListener;
    private int mDeleteItemPosition;
    // Handle the process of automatically updating enabled sensors:
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MESSAGE_UPDATE_CURSORS) {
                mCursorShoppingLists.requery();

                if (mUpdating) {
                    sendMessageDelayed(obtainMessage(MESSAGE_UPDATE_CURSORS),
                            mUpdateInterval);
                }

            }
        }
    };

    /**
     * Set theme for all lists.
     *
     * @param context
     * @param theme
     */
    public static void setThemeForAll(Context context, String theme) {
        ContentValues values = new ContentValues();
        values.put(Lists.SKIN_BACKGROUND, theme);
        context.getContentResolver().update(Lists.CONTENT_URI, values, null,
                null);
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        if (debug) {
            Log.d(TAG, "Shopping list onCreate()");
        }

        mSortOrder = PreferenceActivity.getShoppingListSortOrderFromPrefs(this);

        mDistribution.setFirst(MENU_DISTRIBUTION_START,
                DIALOG_DISTRIBUTION_START);

        // Check whether EULA has been accepted
        // or information about new version can be presented.
        if (false && mDistribution.showEulaOrNewVersion()) {
            return;
        }

        if (LayoutChoiceActivity.show(this)) {
            finish();
        }
        setContentView(R.layout.activity_shopping);

        // mEditItemPosition = -1;

        // Automatic requeries (once a second)
        mUpdateInterval = 2000;
        mUpdating = false;

        // General Uris:
        mListUri = ShoppingContract.Lists.CONTENT_URI;
        mItemUri = ShoppingContract.Items.CONTENT_URI;
        mListItemUri = ShoppingContract.Items.CONTENT_URI;

        int defaultShoppingList = getLastUsedListFromPrefs();

        // Handle the calling intent
        final Intent intent = getIntent();
        final String type = intent.resolveType(this);
        final String action = intent.getAction();

        if (action == null) {
            // Main action
            mState = STATE_MAIN;
            mListUri = buildDefaultShoppingListUri(defaultShoppingList);
            intent.setData(mListUri);

        } else if (Intent.ACTION_MAIN.equals(action)) {
            // Main action
            mState = STATE_MAIN;
            mListUri = buildDefaultShoppingListUri(defaultShoppingList);
            intent.setData(mListUri);

        } else if (Intent.ACTION_VIEW.equals(action)) {
            mState = STATE_VIEW_LIST;
            mListUri = setListUriFromIntent(intent.getData(), type);

        } else if (Intent.ACTION_INSERT.equals(action)) {
            mState = STATE_VIEW_LIST;
            mListUri = setListUriFromIntent(intent.getData(), type);

        } else if (Intent.ACTION_PICK.equals(action)) {
            mState = STATE_PICK_ITEM;
            mListUri = buildDefaultShoppingListUri(defaultShoppingList);

        } else if (Intent.ACTION_GET_CONTENT.equals(action)) {
            mState = STATE_GET_CONTENT_ITEM;
            mListUri = buildDefaultShoppingListUri(defaultShoppingList);

        } else if (GeneralIntents.ACTION_INSERT_FROM_EXTRAS.equals(action)) {
            if (ShoppingListIntents.TYPE_STRING_ARRAYLIST_SHOPPING.equals(type)) {
                /*
                 * Need to insert new items from a string array in the intent
                 * extras Use main action but add an item to the options menu
                 * for adding extra items
                 */
                mItemsFromExtras.getShoppingExtras(intent);
                mState = STATE_MAIN;
                mListUri = buildDefaultShoppingListUri(defaultShoppingList);
                intent.setData(mListUri);
            } else if (intent.getDataString().startsWith(
                    ShoppingContract.Lists.CONTENT_URI.toString())) {
                // Somewhat quick fix to pass data from ShoppingListsActivity to
                // this activity.

                // We received a valid shopping list URI:
                mListUri = intent.getData();

                mItemsFromExtras.getShoppingExtras(intent);
                mState = STATE_MAIN;
                intent.setData(mListUri);
            }
        } else {
            // Unknown action.
            Log.e(TAG, "Shopping: Unknown action, exiting");
            Toast.makeText(this, "Don't know how to handle " + action, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // hook up all buttons, lists, edit text:
        createView();

        // populate the lists
        fillListFilter();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        // Get last part of URI:
        int selectList;
        try {
            selectList = Integer.parseInt(mListUri.getLastPathSegment());
        } catch (NumberFormatException e) {
            selectList = defaultShoppingList;
        }

        // select the default shopping list at the beginning:
        setSelectedListId(selectList);

        if (icicle != null) {
            String prevText = icicle.getString(ORIGINAL_ITEM);
            if (prevText != null) {
                mEditText.setTextKeepState(prevText);
            }
            // mTextEntryMenu = icicle.getInt(BUNDLE_TEXT_ENTRY_MENU);
            // mEditItemPosition = icicle.getInt(BUNDLE_CURSOR_ITEMS_POSITION);
            mItemUri = Uri.parse(icicle.getString(BUNDLE_ITEM_URI));
            List<String> pathSegs = mItemUri.getPathSegments();
            int num = pathSegs.size();
            mListItemUri = Uri
                    .withAppendedPath(mListUri, pathSegs.get(num - 1));
            if (icicle.containsKey(BUNDLE_RELATION_URI)) {
                mRelationUri = Uri.parse(icicle.getString(BUNDLE_RELATION_URI));
            }
            mItemsView.setModes(icicle.getInt(BUNDLE_MODE), icicle.getInt(BUNDLE_MODE_BEFORE_SEARCH));
        }

        // set focus to the edit line:
        mEditText.requestFocus();

        // TODO remove initFromPreferences from onCreate
        // we need it in resume to update after settings have changed
        initFromPreferences();
        // now update title and fill all items
        onModeChanged();

        mItemsView.setActionBarListener(this);
        mItemsView.setUndoListener(this);

        toggleBoughtInputMethod = ((ShoppingApplication) getApplication()).dependencies().getToggleBoughtInputMethod(this, mItemsView);
    }

    private Uri buildDefaultShoppingListUri(final int defaultShoppingList) {
        return Uri.withAppendedPath(Lists.CONTENT_URI,
                String.valueOf(defaultShoppingList));
    }

    private Uri setListUriFromIntent(Uri data, String type) {
        if (ShoppingContract.ITEM_TYPE.equals(type)) {
            return ShoppingUtils.getListForItem(this, data
                    .getLastPathSegment());
        } else return data;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (mDrawerToggle != null) {
            // Sync the toggle state after onRestoreInstanceState has occurred.
            mDrawerToggle.syncState();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mDrawerToggle != null) {
            mDrawerToggle.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        updateWidgets();
    }

    private void updateWidgets() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        appWidgetManager.getAppWidgetIds(new ComponentName(this
                .getPackageName(), CheckItemsWidget.class.getName()));
        List<AppWidgetProviderInfo> b = appWidgetManager.getInstalledProviders();
        for (AppWidgetProviderInfo i : b) {
            if (i.provider.getPackageName().equals(this.getPackageName())) {
                int[] a = appWidgetManager.getAppWidgetIds(i.provider);
                new CheckItemsWidget().onUpdate(this, appWidgetManager, a);
            }
        }
    }

    // used at startup, otherwise use getSelectedListId
    private int getLastUsedListFromPrefs() {

        SharedPreferences sp = getSharedPreferences(
                "org.openintents.shopping_preferences", MODE_PRIVATE);

        return sp.getInt(PreferenceActivity.PREFS_LASTUSED, 1);
    }

    private void initFromPreferences() {

        SharedPreferences sp = getSharedPreferences(
                "org.openintents.shopping_preferences", MODE_PRIVATE);

        if (mItemsView != null) {
            // UGLY WORKAROUND:
            // On screen orientation changes, fillItems() is called twice.
            // That is why we have to set the list position twice.
            // Nov14: Seems to not be required anymore, so change to 1 to
            // avoid unwanted scrolling when marking items.
            mItemsView.mUpdateLastListPosition = 1;

            mItemsView.mLastListPosition = sp.getInt(
                    PreferenceActivity.PREFS_LASTLIST_POSITION, 0);
            mItemsView.mLastListTop = sp.getInt(
                    PreferenceActivity.PREFS_LASTLIST_TOP, 0);

            if (debug) {
                Log.d(TAG, "Load list position: pos: "
                        + mItemsView.mLastListPosition + ", top: "
                        + mItemsView.mLastListTop);
            }

            // selected list must be set after changing ordering
            String sortOrder = PreferenceActivity
                    .getShoppingListSortOrderFromPrefs(this);
            if (!mSortOrder.equals(sortOrder)) {
                mSortOrder = sortOrder;
                fillListFilter();
                setSelectedListId(getLastUsedListFromPrefs());
            } else if (getSelectedListId() == -1) {
                setSelectedListId(getLastUsedListFromPrefs());
            }
        }

        if (sp.getBoolean(PreferenceActivity.PREFS_SCREENLOCK,
                PreferenceActivity.PREFS_SCREENLOCK_DEFAULT)) {
            getWindow()
                    .addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        if (mItemsView != null) {
            if (sp.getBoolean(PreferenceActivity.PREFS_SHOW_PRICE,
                    PreferenceActivity.PREFS_SHOW_PRICE_DEFAULT)) {
                mItemsView.mPriceVisibility = View.VISIBLE;
            } else {
                mItemsView.mPriceVisibility = View.GONE;
            }

            if (sp.getBoolean(PreferenceActivity.PREFS_SHOW_TAGS,
                    PreferenceActivity.PREFS_SHOW_TAGS_DEFAULT)) {
                mItemsView.mTagsVisibility = View.VISIBLE;
            } else {
                mItemsView.mTagsVisibility = View.GONE;
            }
            if (sp.getBoolean(PreferenceActivity.PREFS_SHOW_QUANTITY,
                    PreferenceActivity.PREFS_SHOW_QUANTITY_DEFAULT)) {
                mItemsView.mQuantityVisibility = View.VISIBLE;
            } else {
                mItemsView.mQuantityVisibility = View.GONE;
            }
            if (sp.getBoolean(PreferenceActivity.PREFS_SHOW_UNITS,
                    PreferenceActivity.PREFS_SHOW_UNITS_DEFAULT)) {
                mItemsView.mUnitsVisibility = View.VISIBLE;
            } else {
                mItemsView.mUnitsVisibility = View.GONE;
            }
            if (sp.getBoolean(PreferenceActivity.PREFS_SHOW_PRIORITY,
                    PreferenceActivity.PREFS_SHOW_PRIORITY_DEFAULT)) {
                mItemsView.mPriorityVisibility = View.VISIBLE;
            } else {
                mItemsView.mPriorityVisibility = View.GONE;
            }
        }

        mUseSensor = sp.getBoolean(PreferenceActivity.PREFS_SHAKE,
                PreferenceActivity.PREFS_SHAKE_DEFAULT);

        boolean nowEditingFilter = sp.getBoolean(
                PreferenceActivity.PREFS_USE_FILTERS,
                PreferenceActivity.PREFS_USE_FILTERS_DEFAULT);
        if (mStoresFilterButton != null && mEditingFilter != nowEditingFilter) {
            updateFilterWidgets();
            fillItems(false);
        }

        if (PreferenceActivity.getCompletionSettingChanged(this)) {
            fillAutoCompleteTextViewAdapter(mEditText);
        }
    }

    private void registerSensor() {
        if (!mUseSensor) {
            // Don't use sensors
            return;
        }

        if (mItemsView.inShopMode()) {
            registerAcceleratorSensor();
        }

    }

    private void registerAcceleratorSensor() {
        if (mSensorManager == null) {
            mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        }
        mSensorManager.registerListener(mMySensorListener,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_UI);
    }

    private void unregisterSensor() {
        if (mSensorManager != null) {
            mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            mSensorManager.unregisterListener(mMySensorListener);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        ((ShoppingApplication) getApplication()).dependencies().onResumeShoppingActivity(this);

        // Reload preferences, in case something changed
        initFromPreferences();

        mIsActive = true;

        this.setRequestedOrientation(PreferenceActivity
                .getOrientationFromPrefs(this));

        if (getSelectedListId() != -1) {
            setListTheme(loadListTheme());
            applyListTheme();
            updateTitle();
        }
        mItemsView.onResume();

        mEditText
                .setKeyListener(PreferenceActivity
                        .getCapitalizationKeyListenerFromPrefs(getApplicationContext()));

        if (!mUpdating) {
            mUpdating = true;
            // mHandler.sendMessageDelayed(mHandler.obtainMessage(
            // MESSAGE_UPDATE_CURSORS), mUpdateInterval);
        }


        if (mItemsFromExtras.hasItems()) {
            mItemsFromExtras.insertInto(this, mItemsView);
        }

        // Items received through intents are added in
        // fillItems().

        registerSensor();

        if (debug) {
            Log.i(TAG, "Shopping list onResume() finished");
        }
    }

    private void updateTitle() {
        // Modify our overall title depending on the mode we are running in.
        // In most cases, "title" is the name of the current list, and subtitle
        // depends on the mode -- shopping vs pick items, for example.

        mTitle = getCurrentListName();
        mSubTitle = getText(R.string.app_name);

        if (mState == STATE_MAIN || mState == STATE_VIEW_LIST) {
            if (PreferenceActivity
                    .getPickItemsInListFromPrefs(getApplicationContext())) {
                // 2 different modes
                if (mItemsView.inShopMode()) {
                    mSubTitle = getString(R.string.menu_start_shopping);
                    registerSensor();
                } else {
                    mSubTitle = getString(R.string.menu_pick_items);
                    unregisterSensor();
                }
            }
        } else if ((mState == STATE_PICK_ITEM)
                || (mState == STATE_GET_CONTENT_ITEM)) {
            mSubTitle = (getText(R.string.pick_item));
            setTitleColor(0xFFAAAAFF);
        }

        getSupportActionBar().setTitle(mTitle);
        getSupportActionBar().setSubtitle(mSubTitle);

        // also update the button label
        updateButton();
    }

    private void updateButton() {
        if (mItemsView.inAddItemsMode()) {
            String newItem = mEditText.getText().toString();
            if (TextUtils.isEmpty(newItem)) {
                // If in "add items" mode and the text field is empty,
                // set the button text to "Shopping"
                mButton.setText(R.string.menu_start_shopping);
            } else {
                mButton.setText(R.string.add);
            }
        } else {
            mButton.setText(R.string.add);
        }
    }

    private void saveActiveList(boolean savePos) {
        SharedPreferences sp = getSharedPreferences(
                "org.openintents.shopping_preferences", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        // need to do something about the fact that the spinner is driving this
        // even though it isn't always used. also the long vs int is fishy.
        // but for now, just don't overwrite previous setting with -1.
        int list_id = new Long(getSelectedListId()).intValue();
        if (list_id != -1) {
            editor.putInt(PreferenceActivity.PREFS_LASTUSED, list_id);
        }
        // Save position and pixel position of first visible item
        // of current shopping list
        int listposition = mItemsView.getFirstVisiblePosition();
        if (savePos) {
            View v = mItemsView.getChildAt(0);
            int listtop = (v == null) ? 0 : v.getTop();
            if (debug) {
                Log.d(TAG, "Save list position: pos: " + listposition + ", top: "
                        + listtop);
            }

            editor.putInt(PreferenceActivity.PREFS_LASTLIST_POSITION, listposition);
            editor.putInt(PreferenceActivity.PREFS_LASTLIST_TOP, listtop);
        }
        editor.commit();
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onPause()
     */
    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        if (debug) {
            Log.i(TAG, "Shopping list onPause()");
        }
        if (debug) {
            Log.i(TAG, "Spinner: onPause: " + mIsActive);
        }
        mIsActive = false;
        if (debug) {
            Log.i(TAG, "Spinner: onPause: " + mIsActive);
        }

        unregisterSensor();

        saveActiveList(true);
        // TODO ???
        /*
         * // Unregister refresh intent receiver
         * unregisterReceiver(mIntentReceiver);
         */

        mItemsView.onPause();
    }

    public void onDestroy() {
        super.onDestroy();
        if (toggleBoughtInputMethod != null) {
            toggleBoughtInputMethod.release();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (debug) {
            Log.i(TAG, "Shopping list onSaveInstanceState()");
        }

        // Save original text from edit box
        String s = mEditText.getText().toString();
        outState.putString(ORIGINAL_ITEM, s);

        outState.putString(BUNDLE_ITEM_URI, mItemUri.toString());
        if (mRelationUri != null) {
            outState.putString(BUNDLE_RELATION_URI, mRelationUri.toString());
        }
        // When app is stopped, it will be thrown out of search mode; thus if we are in search mode,
        // we need to ensure the app resumes in the original mode it was in before entering search.
        final int saveMode = mItemsView.getInSearch() ? mItemsView.mModeBeforeSearch : mItemsView.getMode();
        outState.putInt(BUNDLE_MODE, saveMode);
        outState.putInt(BUNDLE_MODE_BEFORE_SEARCH, saveMode);
        mUpdating = false;

        // after items have been added through an "insert from extras" the
        // action name should be different to avoid duplicate inserts e.g. on
        // rotation.
        if (mItemsFromExtras.hasBeenInserted()
                && GeneralIntents.ACTION_INSERT_FROM_EXTRAS.equals(getIntent()
                .getAction())) {
            setIntent(getIntent().setAction(Intent.ACTION_VIEW));
        }
    }

    /**
     * Hook up buttons, lists, and edittext with functionality.
     */
    private void createView() {

        // Temp-create either Spinner or List based upon the Display
        createList();

        mAddPanel = findViewById(R.id.add_panel);
        mEditText = (AutoCompleteTextView) findViewById(R.id.autocomplete_add_item);

        fillAutoCompleteTextViewAdapter(mEditText);
        mEditText.setThreshold(1);
        mEditText.setOnKeyListener(new OnKeyListener() {

            private int mLastKeyAction = KeyEvent.ACTION_UP;

            public boolean onKey(View v, int keyCode, KeyEvent key) {
                // Shortcut: Instead of pressing the button,
                // one can also press the "Enter" key.
                if (debug) {
                    Log.i(TAG, "Key action: " + key.getAction());
                }
                if (debug) {
                    Log.i(TAG, "Key code: " + keyCode);
                }
                if (keyCode == KeyEvent.KEYCODE_ENTER) {

                    if (mEditText.isPopupShowing()) {
                        mEditText.performCompletion();
                    }

                    // long key press might cause call of duplicate onKey events
                    // with ACTION_DOWN
                    // this would result in inserting an item and showing the
                    // pick list

                    if (key.getAction() == KeyEvent.ACTION_DOWN
                            && mLastKeyAction == KeyEvent.ACTION_UP) {
                        insertNewItem();
                    }

                    mLastKeyAction = key.getAction();
                    return true;
                }
                return false;
            }
        });
        mEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {
                if (mItemsView.inAddItemsMode()) {
                    // small optimization: Only care about updating
                    // the button label on each key pressed if we
                    // are in "add items" mode.
                    updateButton();
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
            }

        });

        mButton = (Button) findViewById(R.id.button_add_item);
        mButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                insertNewItem();
            }
        });
        mButton.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                if (PreferenceActivity
                        .getAddForBarcode(getApplicationContext()) == false) {
                    if (debug) {
                        Log.v(TAG,
                                "barcode scanner on add button long click disabled");
                    }
                    return false;
                }

                Intent intent = new Intent();
                intent.setData(mListUri);
                intent.setClassName("org.openintents.barcodescanner",
                        "org.openintents.barcodescanner.BarcodeScanner");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
                try {
                    startActivityForResult(intent,
                            REQUEST_CODE_CATEGORY_ALTERNATIVE);
                } catch (ActivityNotFoundException e) {
                    if (debug) {
                        Log.v(TAG, "barcode scanner not found");
                    }
                    showDialog(DIALOG_GET_FROM_MARKET);
                    return false;
                }

                // Instead of calling the class of barcode
                // scanner directly, a more generic approach would
                // be to use a general activity picker.
                //
                // TODO: Implement onActivityResult.
                // Problem: User has to pick activity every time.
                // Choice should be storeable in Stettings.
                // Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                // intent.setData(mListUri);
                // intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
                //
                // Intent pickIntent = new Intent(Intent.ACTION_PICK_ACTIVITY);
                // pickIntent.putExtra(Intent.EXTRA_INTENT, intent);
                // pickIntent.putExtra(Intent.EXTRA_TITLE,
                // getText(R.string.title_select_item_from));
                // try {
                // startActivityForResult(pickIntent,
                // REQUEST_CODE_CATEGORY_ALTERNATIVE);
                // } catch (ActivityNotFoundException e) {
                // Log.v(TAG, "barcode scanner not found");
                // return false;
                // }
                return true;
            }
        });

        mLayoutParamsItems = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        mItemsView = (ShoppingItemsView) findViewById(R.id.list_items);
        mItemsView.setThemedBackground(findViewById(R.id.background));
        mItemsView.setCustomClickListener(this);
        mItemsView.setOnModeChangeListener(this);
        mItemsView.initTotals();

        mItemsView.setItemsCanFocus(true);
        mItemsView.setDragListener(new DragListener() {

            @Override
            public void drag(int from, int to) {
                if (debug) {
                    Log.v("DRAG", "" + from + "/" + to);
                }

            }
        });
        mItemsView.setDropListener(new DropListener() {

            @Override
            public void drop(int from, int to) {
                if (debug) {
                    Log.v("DRAG", "" + from + "/" + to);
                }

            }
        });

        mItemsView.setOnItemClickListener(new OnItemClickListener() {

            public void onItemClick(AdapterView parent, View v, int pos, long id) {
                Cursor c = (Cursor) parent.getItemAtPosition(pos);
                onCustomClick(c, pos, EditItemDialog.FieldType.ITEMNAME, v);
                // DO NOT CLOSE THIS CURSOR - IT IS A MANAGED ONE.
                // ---- c.close();
            }

        });

        mItemsView
                .setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {

                    public void onCreateContextMenu(ContextMenu contextmenu,
                                                    View view, ContextMenuInfo info) {
                        contextmenu.add(0, MENU_EDIT_ITEM, 0,
                                R.string.menu_edit_item).setShortcut('1', 'e');
                        contextmenu.add(0, MENU_MARK_ITEM, 0,
                                R.string.menu_mark_item).setShortcut('2', 'm');
                        contextmenu.add(0, MENU_ITEM_STORES, 0,
                                R.string.menu_item_stores)
                                .setShortcut('3', 's');
                        contextmenu.add(0, MENU_REMOVE_ITEM_FROM_LIST, 0,
                                R.string.menu_remove_item)
                                .setShortcut('4', 'r');
                        contextmenu.add(0, MENU_COPY_ITEM, 0,
                                R.string.menu_copy_item).setShortcut('5', 'c');
                        contextmenu.add(0, MENU_DELETE_ITEM, 0,
                                R.string.menu_delete_item)
                                .setShortcut('6', 'd');
                        contextmenu.add(0, MENU_MOVE_ITEM, 0,
                                R.string.menu_move_item).setShortcut('7', 'l');
                    }

                });
    }

    private void createList() {

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        mTitle = mDrawerTitle = getTitle();
        if (mDrawerLayout != null) {
            mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {

                /**
                 * Called when a drawer has settled in a completely closed state.
                 */
                public void onDrawerClosed(View view) {
                    getSupportActionBar().setTitle(mTitle);
                    getSupportActionBar().setSubtitle(mSubTitle);
                    invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                }

                /**
                 * Called when a drawer has settled in a completely open state.
                 */
                public void onDrawerOpened(View drawerView) {
                    getSupportActionBar().setTitle(mDrawerTitle);
                    getSupportActionBar().setSubtitle(null);
                    invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                }
            };

            // Set the drawer toggle as the DrawerListener
            mDrawerLayout.setDrawerListener(mDrawerToggle);
        }

        mDrawerListsView = (ListView) findViewById(R.id.left_drawer);


        mShoppingListsView = (Spinner) findViewById(R.id.spinner_listfilter);
        mShoppingListsView
                .setOnItemSelectedListener(new OnItemSelectedListener() {
                    public void onItemSelected(AdapterView parent, View v,
                                               int position, long id) {
                        if (debug) {
                            Log.d(TAG, "Spinner: onItemSelected");
                        }

                        // Update list cursor:
                        getSelectedListId();

                        // Set the theme based on the selected list:
                        setListTheme(loadListTheme());

                        // If it's the same list we had before, requery only
                        // if a preference has changed since then.
                        fillItems(id == mItemsView.getListId());

                        updateTitle();

                        // Apply the theme after the list has been filled:
                        applyListTheme();
                    }

                    public void onNothingSelected(AdapterView arg0) {
                        if (debug) {
                            Log.d(TAG, "Spinner: onNothingSelected: "
                                    + mIsActive);
                        }
                        if (mIsActive) {
                            fillItems(false);
                        }
                    }
                });

        mShoppingListsFilterButton = (Button) findViewById(R.id.listfilter);
        if (mShoppingListsFilterButton != null) {
            mShoppingListsFilterButton.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    showListFilter(v);
                }
            });
        }


        mStoresFilterButton = (Button) findViewById(R.id.storefilter);
        mStoresFilterButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                showStoresFilter(v);
            }
        });
        mTagsFilterButton = (Button) findViewById(R.id.tagfilter);
        mTagsFilterButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                showTagsFilter(v);
            }
        });
    }

    protected void showListFilter(final View v) {
        QuickSelectMenu popup = new QuickSelectMenu(this, v);
        int i_list;

        Menu menu = popup.getMenu();
        if (menu == null) {
            return;
        }

        // get the list of lists
        mCursorShoppingLists.requery();
        int count = mCursorShoppingLists.getCount();
        mCursorShoppingLists.moveToFirst();
        for (i_list = 0; i_list < count; i_list++) {
            String name = mCursorShoppingLists.getString(mStringListFilterNAME);
            menu.add(0, i_list, Menu.NONE, name);
            mCursorShoppingLists.moveToNext();
        }

        popup.setOnItemSelectedListener(new QuickSelectMenu.OnItemSelectedListener() {
            public void onItemSelected(CharSequence name, int pos) {
                setSelectedListPos(pos);
            }
        });

        popup.show();
    }

    protected void showStoresFilter(final View v) {
        QuickSelectMenu popup = new QuickSelectMenu(this, v);

        Menu menu = popup.getMenu();
        if (menu == null) {
            return;
        }
        Cursor c = getContentResolver()
                .query(Stores.QUERY_BY_LIST_URI.buildUpon()
                                .appendPath(this.mListUri.getLastPathSegment()).build(),
                        new String[]{Stores._ID, Stores.NAME}, null, null,
                        "stores.name COLLATE NOCASE ASC"
                );
        int i_store, count = c.getCount();
        if (count == 0) {
            Toast.makeText(this, R.string.no_stores_available,
                    Toast.LENGTH_SHORT).show();
        }

        // prepend the "no filter" option
        menu.add(0, -1, Menu.NONE, R.string.unfiltered);

        // get the list of stores
        c.moveToFirst();
        for (i_store = 0; i_store < count; i_store++) {
            long id = c.getLong(0);
            String name = c.getString(1);
            menu.add(0, (int) id, Menu.NONE, name);
            c.moveToNext();
        }
        c.deactivate();
        c.close();

        popup.setOnItemSelectedListener(new QuickSelectMenu.OnItemSelectedListener() {
            public void onItemSelected(CharSequence name, int id) {
                // set the selected store filter
                // update the filter summary? not until filter region collapsed.
                ContentValues values = new ContentValues();
                values.put(Lists.STORE_FILTER, (long) id);
                getContentResolver().update(mListUri, values, null, null);
                if (id == -1) {
                    ((Button) v).setText(R.string.stores);
                } else {
                    ((Button) v).setText(name);
                }
                fillItems(false);
            }
        });

        popup.show();
    }

    protected void showTagsFilter(final View v) {
        QuickSelectMenu popup = new QuickSelectMenu(this, v);

        Menu menu = popup.getMenu();
        if (menu == null) {
            return;
        }
        String[] tags = getTaglist(mListUri.getLastPathSegment());
        int i_tag, count = tags.length;

        if (count == 0) {
            Toast.makeText(this, R.string.no_tags_available, Toast.LENGTH_SHORT)
                    .show();
        }

        // prepend the "no filter" option
        menu.add(0, -1, Menu.NONE, R.string.unfiltered);

        for (i_tag = 0; i_tag < count; i_tag++) {
            menu.add(tags[i_tag]);
        }

        popup.setOnItemSelectedListener(new QuickSelectMenu.OnItemSelectedListener() {
            public void onItemSelected(CharSequence name, int id) {
                // set the selected tags filter
                ContentValues values = new ContentValues();
                values.put(Lists.TAGS_FILTER, id == -1 ? "" : (String) name);
                getContentResolver().update(mListUri, values, null, null);
                if (id == -1) {
                    ((Button) v).setText(R.string.tags);
                } else {
                    ((Button) v).setText(name);
                }
                fillItems(false);
            }
        });

        popup.show();
    }

    protected void updateFilterWidgets() {
        mEditingFilter = PreferenceActivity.getUsingFiltersFromPrefs(this);

        mStoresFilterButton.setVisibility(mEditingFilter ? View.VISIBLE : View.GONE);
        mTagsFilterButton.setVisibility(mEditingFilter ? View.VISIBLE : View.GONE);

        if (mShoppingListsFilterButton != null) {
            mShoppingListsFilterButton
                    .setVisibility(View.GONE);
        }
        // spinner goes the opposite way
        if (mShoppingListsView != null) {
            mShoppingListsView.setVisibility(View.GONE);
        }

        if (mEditingFilter) {
            String storeName = ShoppingUtils.getListFilterStoreName(this,
                    mListUri);
            if (storeName != null) {
                mStoresFilterButton.setText(storeName);
            } else {
                mStoresFilterButton.setText(R.string.stores);
            }
            String tagFilter = ShoppingUtils.getListTagsFilter(this, mListUri);
            if (tagFilter != null) {
                mTagsFilterButton.setText(tagFilter);
            } else {
                mTagsFilterButton.setText(R.string.tags);
            }
        }
    }

    public void onCustomClick(Cursor c, int pos,
                              EditItemDialog.FieldType field, View clicked_view) {
        if (mState == STATE_PICK_ITEM) {
            pickItem(c);
        } else {
            if (mItemsView.mShowCheckBox) {
                boolean handled = false;
                // In default theme, there is an extra check box,
                // so clicking on anywhere else means to edit the
                // item.

                if (field == EditItemDialog.FieldType.PRICE
                        && PreferenceActivity
                        .getUsingPerStorePricesFromPrefs(this))
                // should really be a per-list preference
                {
                    editItemStores(pos);
                    handled = true;
                }

                if ((field == EditItemDialog.FieldType.PRIORITY || field == EditItemDialog.FieldType.QUANTITY)
                        && PreferenceActivity.getQuickEditModeFromPrefs(this)) {
                    handled = QuickEditFieldPopupMenu(c, pos, field,
                            clicked_view);
                }

                if (!handled) {
                    editItem(pos, field);
                }
            } else {
                // For themes without a checkbox, clicking anywhere means
                // to toggle the item.
                mItemsView.toggleItemBought(pos);
            }
        }
    }

    // Menu

    private boolean QuickEditFieldPopupMenu(final Cursor c, final int pos,
                                            final FieldType field, View v) {
        QuickSelectMenu popup = new QuickSelectMenu(this, v);

        Menu menu = popup.getMenu();
        if (menu == null) {
            return false;
        }
        menu.add("1");
        menu.add("2");
        menu.add("3");
        menu.add("4");
        menu.add("5");
        if (field == FieldType.QUANTITY) {
            menu.add(R.string.otherqty);
        }
        if (field == FieldType.PRIORITY) {
            menu.add(R.string.otherpri);
        }

        popup.setOnItemSelectedListener(new QuickSelectMenu.OnItemSelectedListener() {
            public void onItemSelected(CharSequence name, int id) {
                // TODO: use a flavor of menu.add which takes id,
                // then identifying the selection becomes easier here.

                if (name.length() > 1) {
                    // Other ... use edit dialog
                    editItem(pos, field);
                } else {
                    long number = name.charAt(0) - '0';
                    ContentValues values = new ContentValues();
                    switch (field) {
                        case PRIORITY:
                            values.put(Contains.PRIORITY, number);
                            break;
                        case QUANTITY:
                            values.put(Contains.QUANTITY, number);
                            break;
                        default:
                            break;
                    }
                    mItemsView.mCursorItems.moveToPosition(pos);
                    String containsId = mItemsView.mCursorItems
                            .getString(mStringItemsCONTAINSID);
                    Uri uri = Uri.withAppendedPath(
                            ShoppingContract.Contains.CONTENT_URI, containsId);
                    getApplicationContext().getContentResolver().update(uri,
                            values, null, null);
                    onItemChanged(); // probably overkill
                    mItemsView.updateTotal();
                }
            }
        });

        popup.show();
        return true;
    }

    /**
     * Inserts new item from edit box into currently selected shopping list.
     */
    private void insertNewItem() {
        String newItem = mEditText.getText().toString().trim();

        // Only add if there is something to add:
        if (newItem.length() > 0) {
            long listId = getSelectedListId();
            if (listId < 0) {
                // No valid list - probably view is not active
                // and no item is selected.
                return;
            }
            mItemsView.insertNewItem(this, newItem, null, null, null, null);
            mEditText.setText("");
            fillAutoCompleteTextViewAdapter(mEditText);
        } else {
            // Open list to select item from
            pickItems();
        }
    }

    /**
     * Picks an item and returns to calling activity.
     */
    private void pickItem(Cursor c) {
        long itemId = c.getLong(mStringItemsITEMID);
        Uri url = ContentUris.withAppendedId(
                ShoppingContract.Items.CONTENT_URI, itemId);

        Intent intent = new Intent();
        intent.setData(url);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        /*
         * int MENU_ACTION_WITH_TEXT=0;
         *
         * //Temp- for backward compatibility with OS 3 features
         *
         * if(!usingListSpinner()){ try{ //setting the value equivalent to
         * desired expression
         * //MenuItem.SHOW_AS_ACTION_IF_ROOM|MenuItem.SHOW_AS_ACTION_WITH_TEXT
         * java.lang.reflect.Field
         * field=MenuItem.class.getDeclaredField("SHOW_AS_ACTION_IF_ROOM");
         * MENU_ACTION_WITH_TEXT=field.getInt(MenuItem.class);
         * field=MenuItem.class.getDeclaredField("SHOW_AS_ACTION_WITH_TEXT");
         * MENU_ACTION_WITH_TEXT|=field.getInt(MenuItem.class); }catch(Exception
         * e){ //reset value irrespective of cause MENU_ACTION_WITH_TEXT=0; }
         *
         * }
         */

        // Add menu option for auto adding items from string array in intent
        // extra if they exist
        if (mItemsFromExtras.hasItems()) {
            menu.add(0, MENU_INSERT_FROM_EXTRAS, 0, R.string.menu_auto_add)
                    .setIcon(android.R.drawable.ic_menu_upload);
        }

        MenuItem item;

        View searchView = mItemsView.getSearchView();
        if (searchView != null) {
            item = menu.add(0, MENU_SEARCH_ADD, 0, R.string.menu_search_add);
            MenuItemCompat.setActionView(item, searchView);
            MenuItemCompat.setShowAsAction(item, MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        mAddPanel.setVisibility(searchView == null ? View.VISIBLE : View.GONE);

        item = menu.add(0, MENU_SORT_LIST, 0, R.string.menu_sort_list)
                .setIcon(android.R.drawable.ic_menu_sort_alphabetically);
        MenuItemCompat.setShowAsAction(item, MenuItem.SHOW_AS_ACTION_IF_ROOM
                | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        if (mListSortActionProvider == null) {
            mListSortActionProvider = new ListSortActionProvider(this);
        }
        MenuItemCompat.setActionProvider(item, mListSortActionProvider);

        // Standard menu

        // tentatively moved "new list" to drawer
        //item = menu.add(0, MENU_NEW_LIST, 0, R.string.new_list)
        //		.setIcon(R.drawable.ic_menu_add_list).setShortcut('0', 'n');
        // MenuCompat.setShowAsAction(item, MenuItem.SHOW_AS_ACTION_IF_ROOM);

        item = menu.add(0, MENU_CLEAN_UP_LIST, 0, R.string.clean_up_list)
                .setIcon(R.drawable.ic_menu_cleanup).setShortcut('1', 'c');
        MenuItemCompat.setShowAsAction(item, MenuItem.SHOW_AS_ACTION_IF_ROOM
                | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        menu.add(0, MENU_PICK_ITEMS, 0, R.string.menu_pick_items)
                .setIcon(android.R.drawable.ic_menu_add).setShortcut('2', 'p').
                // tentatively replaced by buttons in drawer.
                        setVisible(false);

        /*
         * menu.add(0, MENU_SHARE, 0, R.string.share)
         * .setIcon(R.drawable.contact_share001a) .setShortcut('4', 's');
         */

        menu.add(0, MENU_THEME, 0, R.string.theme)
                .setIcon(android.R.drawable.ic_menu_manage)
                .setShortcut('3', 't');

        menu.add(0, MENU_PREFERENCES, 0, R.string.preferences)
                .setIcon(android.R.drawable.ic_menu_preferences)
                .setShortcut('4', 'p');

        menu.add(0, MENU_RENAME_LIST, 0, R.string.rename_list)
                .setIcon(android.R.drawable.ic_menu_edit).setShortcut('5', 'r');

        menu.add(0, MENU_DELETE_LIST, 0, R.string.delete_list).setIcon(
                android.R.drawable.ic_menu_delete);

        menu.add(0, MENU_SEND, 0, R.string.send)
                .setIcon(android.R.drawable.ic_menu_send).setShortcut('7', 's');

        if (addLocationAlertPossible()) {
            menu.add(0, MENU_ADD_LOCATION_ALERT, 0, R.string.shopping_add_alert)
                    .setIcon(android.R.drawable.ic_menu_mylocation)
                    .setShortcut('8', 'l');
        }

        menu.add(0, MENU_MARK_ALL_ITEMS, 0, R.string.mark_all_items)
                .setIcon(android.R.drawable.ic_menu_agenda)
                .setShortcut('9', 'm');

        menu.add(0, MENU_UNMARK_ALL_ITEMS, 0, R.string.unmark_all_items);

        menu.add(0, MENU_SYNC_WEAR, 0, R.string.sync_wear);

        menu.add(0, MENU_CONVERT_CSV, 0, R.string.convert_csv);

        // Add distribution menu items last.
        mDistribution.onCreateOptionsMenu(menu);

        // NOTE:
        // Dynamically added menu items are included in onPrepareOptionsMenu()
        // instead of here!
        // (Explanation see there.)

        return true;
    }

    /**
     * Check whether an application exists that handles the pick activity.
     *
     * @return
     */
    private boolean addLocationAlertPossible() {

        // Test whether intent exists for picking a location:
        PackageManager pm = getPackageManager();
        Intent intent = new Intent(Intent.ACTION_PICK, Uri.parse("geo:"));
        List<ResolveInfo> resolve_pick_location = pm.queryIntentActivities(
                intent, PackageManager.MATCH_DEFAULT_ONLY);
        /*
         * for (int i = 0; i < resolve_pick_location.size(); i++) { Log.d(TAG,
         * "Activity name: " + resolve_pick_location.get(i).activityInfo.name);
         * }
         */

        // Check whether adding alerts is possible.
        intent = new Intent(Intent.ACTION_VIEW, Alert.Generic.CONTENT_URI);
        List<ResolveInfo> resolve_view_alerts = pm.queryIntentActivities(
                intent, PackageManager.MATCH_DEFAULT_ONLY);

        boolean pick_location_possible = (resolve_pick_location.size() > 0);
        boolean view_alerts_possible = (resolve_view_alerts.size() > 0);
        if (debug) {
            Log.d(TAG, "Pick location possible: " + pick_location_possible);
        }
        if (debug) {
            Log.d(TAG, "View alerts possible: " + view_alerts_possible);
        }
        return pick_location_possible && view_alerts_possible;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        boolean drawerOpen = mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mDrawerListsView);
        boolean holoSearch = PreferenceActivity.getUsingHoloSearchFromPrefs(this);

        // Add menu option for auto adding items from string array in intent
        // extra if they exist
        if (mItemsFromExtras.hasBeenInserted()) {
            menu.removeItem(MENU_INSERT_FROM_EXTRAS);
        }

        // Selected list:
        long listId = getSelectedListId();

        // set menu title for change mode
        MenuItem menuItem = menu.findItem(MENU_PICK_ITEMS);

        if (mItemsView.inAddItemsMode()) {
            menuItem.setTitle(R.string.menu_start_shopping);
            menuItem.setIcon(android.R.drawable.ic_menu_myplaces);
        } else {
            menu.findItem(MENU_PICK_ITEMS).setTitle(R.string.menu_pick_items);
            menuItem.setIcon(android.R.drawable.ic_menu_add);
        }

        menuItem = menu.findItem(MENU_SEARCH_ADD);
        if (menuItem != null) {
            menuItem.setVisible(holoSearch && !drawerOpen);
            if (!holoSearch) {
                mAddPanel.setVisibility(View.VISIBLE);
            }

            View searchView = menuItem.getActionView();
            int searchImgId = getResources().getIdentifier("android:id/search_button", null, null);
            View imageView = searchView.findViewById(searchImgId);
            if (imageView instanceof ImageView) {
                ((ImageView) imageView).setImageResource(android.R.drawable.ic_menu_add);
            }

        }

        menuItem = menu.findItem(MENU_SYNC_WEAR);
        if (menuItem != null) {
            menuItem.setVisible(mItemsView.isWearSupportAvailable());
        }

        menu.findItem(MENU_MARK_ALL_ITEMS).setVisible(mItemsView.mNumUnchecked > 0);
        menu.findItem(MENU_UNMARK_ALL_ITEMS).setVisible(mItemsView.mNumChecked > 0);

        menu.findItem(MENU_CLEAN_UP_LIST).setEnabled(
                mItemsView.mNumChecked > 0).setVisible(!drawerOpen);


        // Delete list is possible, if we have more than one list:
        // AND
        // the current list is not the default list (listId == 0) - issue #105
        // TODO: Later, the default list should be user-selectable,
        // and not deletable.

        // TODO ???
        /*
         * menu.setItemShown(MENU_DELETE_LIST, mCursorListFilter.count() > 1 &&
         * listId != 1); // 1 is hardcoded number of default first list.
         */

        // The following code is put from onCreateOptionsMenu to
        // onPrepareOptionsMenu,
        // because the URI of the shopping list can change if the user switches
        // to another list.
        // Generate any additional actions that can be performed on the
        // overall list. This allows other applications to extend
        // our menu with their own actions.
        Intent intent = new Intent(null, getIntent().getData());
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        // menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
        // new ComponentName(this, NoteEditor.class), null, intent, 0, null);

        // Workaround to add icons:
        MenuIntentOptionsWithIcons menu2 = new MenuIntentOptionsWithIcons(this,
                menu);
        menu2.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this,
                        org.openintents.shopping.ShoppingActivity.class), null,
                intent, 0, null
        );

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (debug) {
            Log.d(TAG, "onOptionsItemSelected getItemId: " + item.getItemId());
        }
        Intent intent;
        switch (item.getItemId()) {
            case MENU_NEW_LIST:
                showDialog(DIALOG_NEW_LIST);
                return true;

            case MENU_CLEAN_UP_LIST:
                cleanupList();
                return true;

            case MENU_RENAME_LIST:
                showDialog(DIALOG_RENAME_LIST);
                return true;

            case MENU_DELETE_LIST:
                deleteListConfirm();
                return true;

            case MENU_PICK_ITEMS:
                pickItems();
                return true;

            case MENU_SHARE:
                setShareSettings();
                return true;

            case MENU_THEME:
                setThemeSettings();
                return true;

            case MENU_ADD_LOCATION_ALERT:
                addLocationAlert();
                return true;

            case MENU_PREFERENCES:
                intent = new Intent(this, PreferenceActivity.class);
                startActivity(intent);
                return true;
            case MENU_SEND:
                sendList();
                return true;
            case MENU_INSERT_FROM_EXTRAS:
                mItemsFromExtras.insertInto(this, mItemsView);
                return true;
            case MENU_MARK_ALL_ITEMS:
                mItemsView.toggleAllItems(true);
                return true;
            case MENU_UNMARK_ALL_ITEMS:
                mItemsView.toggleAllItems(false);
                return true;
            case MENU_SYNC_WEAR:
                mItemsView.pushItemsToWear();
                return true;
            case MENU_CONVERT_CSV:
                startActivity(new Intent(this, ConvertCsvActivity.class).setData(mListUri));
                return true;
            default:
                break;
        }
        if (debug) {
            Log.d(TAG, "Start intent group id : " + item.getGroupId());
        }
        if (Menu.CATEGORY_ALTERNATIVE == item.getGroupId()) {
            // Start alternative cateogory intents with option to return a
            // result.
            if (debug) {
                Log.d(TAG, "Start alternative intent for : "
                        + item.getIntent().getDataString());
            }
            startActivityForResult(item.getIntent(),
                    REQUEST_CODE_CATEGORY_ALTERNATIVE);
            return true;
        }
        if (mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);

    }

    // /////////////////////////////////////////////////////
    //
    // Menu functions
    //

    /**
     *
     */
    private void pickItems() {
        if (PreferenceActivity
                .getPickItemsInListFromPrefs(getApplicationContext())) {
            if (mItemsView.inShopMode()) {
                mItemsView.setAddItemsMode();
            } else {
                mItemsView.setInShopMode();
            }
            onModeChanged();
        } else {
            if (!mItemsView.inShopMode()) {
                mItemsView.setInShopMode();
                onModeChanged();
            }
            pickItemsUsingDialog();
        }
    }

    private void pickItemsUsingDialog() {
        Intent intent;
        intent = new Intent(this, PickItemsActivity.class);
        intent.setData(mListUri);
        startActivity(intent);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        super.onContextItemSelected(item);
        AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item
                .getMenuInfo();
        switch (item.getItemId()) {
            case MENU_MARK_ITEM:
                markItem(menuInfo.position);
                break;
            case MENU_EDIT_ITEM:
                editItem(menuInfo.position, EditItemDialog.FieldType.ITEMNAME);
                break;
            case MENU_REMOVE_ITEM_FROM_LIST:
                removeItemFromList(menuInfo.position);
                break;
            case MENU_DELETE_ITEM:
                deleteItemDialog(menuInfo.position);
                break;
            case MENU_MOVE_ITEM:
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_PICK);
                intent.setData(ShoppingContract.Lists.CONTENT_URI);
                startActivityForResult(intent, REQUEST_PICK_LIST);
                mMoveItemPosition = menuInfo.position;
                break;
            case MENU_COPY_ITEM:
                copyItem(menuInfo.position);
                break;
            case MENU_ITEM_STORES:
                editItemStores(menuInfo.position);
                break;
            default:
                break;
        }

        return true;
    }

    /**
     * Creates a new list from dialog.
     *
     * @return true if new list was created. False if new list was not created,
     * because user has not given any name.
     */
    private boolean createNewList(String name) {

        if (name.equals("")) {
            // User has not provided any name
            Toast.makeText(this, getString(R.string.please_enter_name),
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        String previousTheme = loadListTheme();

        int newId = (int) ShoppingUtils.getList(this, name);
        fillListFilter();

        setSelectedListId(newId);

        // Now set the theme based on the selected list:
        saveListTheme(previousTheme);
        setListTheme(previousTheme);
        applyListTheme();

        return true;
    }

    private void setListTheme(String theme) {
        mItemsView.setListTheme(theme);
    }

    private void applyListTheme() {
        mItemsView.applyListTheme();

        // In Holo themes, apply the theme text color also to the
        // input box and the button, because the background is
        // semi-transparent.
        mEditText.setTextColor(mItemsView.mTextColor);
        if (mStoresFilterButton != null) {
            mStoresFilterButton.setTextColor(mItemsView.mTextColor);
        }
        if (mTagsFilterButton != null) {
            mTagsFilterButton.setTextColor(mItemsView.mTextColor);
        }
        if (mShoppingListsFilterButton != null) {
            mShoppingListsFilterButton.setTextColor(mItemsView.mTextColor);
        }
        if (mShoppingListsView instanceof Spinner) {
            View view = mShoppingListsView.getChildAt(0);
            setSpinnerTextColorInHoloTheme(view);
        }
    }

    /**
     * For holo themes with transparent widgets, set font color of the spinner
     * using theme color.
     *
     * @param view
     */
    private void setSpinnerTextColorInHoloTheme(View view) {
        if (view instanceof TextView) {
            ((TextView) view).setTextColor(mItemsView.mTextColor);
        }
    }

    // TODO: Convert into proper dialog that remains across screen orientation
    // changes.

    /**
     * Rename list from dialog.
     *
     * @return true if new list was renamed. False if new list was not renamed,
     * because user has not given any name.
     */
    private boolean renameList(String newName) {

        if (newName.equals("")) {
            // User has not provided any name
            Toast.makeText(this, getString(R.string.please_enter_name),
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        // Rename currently selected list:
        long listId = getSelectedListId();

        ContentValues values = new ContentValues();
        values.put(Lists.NAME, "" + newName);
        getContentResolver().update(mListUri, values, null, null
        );

        //
        mCursorShoppingLists.requery();
        setSelectedListId((int) listId);
        updateTitle();
        return true;
    }

    private void sendList() {
        if (mItemsView.getAdapter() instanceof CursorAdapter) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mItemsView.getAdapter().getCount(); i++) {
                Cursor item = (Cursor) mItemsView.getAdapter().getItem(i);
                if (item.getLong(mStringItemsSTATUS) == ShoppingContract.Status.BOUGHT) {
                    sb.append("[X] ");
                } else {
                    sb.append("[ ] ");
                }
                String quantity = item.getString(mStringItemsQUANTITY);
                long pricecent = item.getLong(mStringItemsITEMPRICE);
                String price = PriceConverter.getStringFromCentPrice(pricecent);
                String tags = item.getString(mStringItemsITEMTAGS);
                if (!TextUtils.isEmpty(quantity)) {
                    sb.append(quantity);
                    sb.append(" ");
                }
                String units = item.getString(mStringItemsITEMUNITS);
                if (!TextUtils.isEmpty(units)) {
                    sb.append(units);
                    sb.append(" ");
                }
                sb.append(item.getString(mStringItemsITEMNAME));
                // Put additional info (price, tags) in brackets
                boolean p = !TextUtils.isEmpty(price);
                boolean t = !TextUtils.isEmpty(tags);
                if (p || t) {
                    sb.append(" (");
                    if (p) {
                        sb.append(price);
                    }
                    if (p && t) {
                        sb.append(", ");
                    }
                    if (t) {
                        sb.append(tags);
                    }
                    sb.append(")");
                }
                sb.append("\n");
            }

            Intent i = new Intent();
            i.setAction(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_SUBJECT, getCurrentListName());
            i.putExtra(Intent.EXTRA_TEXT, sb.toString());

            try {
                startActivity(Intent.createChooser(i, getString(R.string.send)));
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, R.string.email_not_available,
                        Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Email client not installed");
            }
        } else {
            Toast.makeText(this, R.string.empty_list_not_sent,
                    Toast.LENGTH_SHORT).show();
        }

    }

    /**
     * Clean up the currently visible shopping list by removing items from list
     * that are marked BOUGHT.
     */
    private void cleanupList() {
        // Remove all items from current list
        // which have STATUS = Status.BOUGHT

        if (!mItemsView.cleanupList()) {
            // Show toast
            Toast.makeText(this, R.string.no_items_marked, Toast.LENGTH_SHORT)
                    .show();
        }
    }

    /**
     * Confirm 'delete list' command by AlertDialog.
     */
    private void deleteListConfirm() {
        new AlertDialog.Builder(this)
                // .setIcon(R.drawable.alert_dialog_icon)
                .setTitle(R.string.confirm_delete_list)
                .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                // click Ok
                                deleteList();
                            }
                        }
                )
                .setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                // click Cancel
                            }
                        }
                )
                // .create()
                .show();
    }

    /**
     * Deletes currently selected shopping list.
     */
    private void deleteList() {
        String listId = mCursorShoppingLists.getString(0);
        ShoppingUtils.deleteList(this, listId);

        // Update view
        fillListFilter();

        getSelectedListId();

        // Set the theme based on the selected list:
        setListTheme(loadListTheme());

        fillItems(false);
        applyListTheme();
        updateTitle();
    }

    /**
     * Mark item
     */
    void markItem(int position) {
        mItemsView.toggleItemBought(position);
    }

    /**
     * Edit item
     *
     * @param field
     */
    void editItem(long itemId, long containsId, EditItemDialog.FieldType field) {
        mItemUri = Uri.withAppendedPath(ShoppingContract.Items.CONTENT_URI, Long.toString(itemId));
        mListItemUri = Uri.withAppendedPath(mListUri, Long.toString(itemId));
        mRelationUri = Uri.withAppendedPath(
                ShoppingContract.Contains.CONTENT_URI, Long.toString(containsId));
        mEditItemFocusField = field;

        showDialog(DIALOG_EDIT_ITEM);
    }

    /**
     * Edit item
     *
     * @param field
     */
    void editItem(int position, EditItemDialog.FieldType field) {
        if (debug) {
            Log.d(TAG, "EditItems: Position: " + position);
        }
        mItemsView.mCursorItems.moveToPosition(position);
        // mEditItemPosition = position;

        long itemId = mItemsView.mCursorItems.getLong(mStringItemsITEMID);
        long containsId = mItemsView.mCursorItems
                .getLong(mStringItemsCONTAINSID);

        editItem(itemId, containsId, field);
    }

    void editItemStores(int position) {
        if (debug) {
            Log.d(TAG, "EditItemStores: Position: " + position);
        }

        mItemsView.mCursorItems.moveToPosition(position);
        // mEditItemPosition = position;
        long itemId = mItemsView.mCursorItems.getLong(mStringItemsITEMID);

        Intent intent;
        intent = new Intent(this, ItemStoresActivity.class);
        intent.setData(mListUri.buildUpon().appendPath(String.valueOf(itemId))
                .build());
        startActivity(intent);
    }

    /**
     * delete item
     */
    void deleteItemDialog(int position) {
        if (debug) {
            Log.d(TAG, "EditItems: Position: " + position);
        }
        mItemsView.mCursorItems.moveToPosition(position);
        mDeleteItemPosition = position;

        showDialog(DIALOG_DELETE_ITEM);
    }

    /**
     * delete item
     */
    void deleteItem(int position) {
        Cursor c = mItemsView.mCursorItems;
        c.moveToPosition(position);

        String listId = mListUri.getLastPathSegment();
        String itemId = c.getString(mStringItemsITEMID);
        ShoppingUtils.deleteItem(this, itemId, listId);

        // c.requery();
        mItemsView.requery();
        fillAutoCompleteTextViewAdapter(mEditText);
    }

    /**
     * move item
     */
    void moveItem(int position, int targetListId) {
        Cursor c = mItemsView.mCursorItems;
        mItemsView.mCursorItems.requery();
        c.moveToPosition(position);

        long listId = getSelectedListId();
        if (false && listId < 0) {
            // No valid list - probably view is not active
            // and no item is selected.
            return;
        }

        // Attach item to new list, preserving all other fields
        String containsId = c.getString(mStringItemsCONTAINSID);
        ContentValues cv = new ContentValues(1);
        cv.put(Contains.LIST_ID, targetListId);
        getContentResolver().update(
                Uri.withAppendedPath(Contains.CONTENT_URI, containsId), cv,
                null, null);

        mItemsView.requery();
    }

    /**
     * copy item
     */
    void copyItem(int position) {
        Cursor c = mItemsView.mCursorItems;
        mItemsView.mCursorItems.requery();
        c.moveToPosition(position);
        String containsId = c.getString(mStringItemsCONTAINSID);
        Long newContainsId;
        Long newItemId;

        c = getContentResolver().query(
                Uri.withAppendedPath(
                        Uri.withAppendedPath(Contains.CONTENT_URI, "copyof"),
                        containsId), new String[]{"item_id", "contains_id"},
                null, null, null
        );

        if (c.getCount() != 1) {
            return;
        }

        c.moveToFirst();
        newItemId = c.getLong(0);
        newContainsId = c.getLong(1);
        c.deactivate();
        c.close();

        editItem(newItemId, newContainsId, FieldType.ITEMNAME);

        // mItemsView.requery();
    }

    /**
     * removeItemFromList
     */
    void removeItemFromList(int position) {
        Cursor c = mItemsView.mCursorItems;
        c.moveToPosition(position);
        // Remember old values before delete (for share below)
        String itemName = c.getString(mStringItemsITEMNAME);
        long oldstatus = c.getLong(mStringItemsSTATUS);

        // Delete item by changing its state
        ContentValues values = new ContentValues();
        values.put(Contains.STATUS, Status.REMOVED_FROM_LIST);
        if (PreferenceActivity.getResetQuantity(getApplicationContext())) {
            values.put(Contains.QUANTITY, "");
        }
        getContentResolver().update(Contains.CONTENT_URI, values, "_id = ?",
                new String[]{c.getString(mStringItemsCONTAINSID)});

        // c.requery();

        mItemsView.requery();

        // If we share items, mark item on other lists:
        // TODO ???
        /*
         * String recipients =
         * mCursorListFilter.getString(mStringListFilterSHARECONTACTS); if (!
         * recipients.equals("")) { String shareName =
         * mCursorListFilter.getString(mStringListFilterSHARENAME); long
         * newstatus = Shopping.Status.BOUGHT;
         *
         * Log.i(TAG, "Update shared item. " + " recipients: " + recipients +
         * ", shareName: " + shareName + ", status: " + newstatus);
         * mGTalkSender.sendItemUpdate(recipients, shareName, itemName,
         * itemName, oldstatus, newstatus); }
         */
    }

    /**
     * Calls the share settings with the currently selected list.
     */
    void setShareSettings() {
        // Obtain URI of current list

        // Call share settings as subactivity
        Intent intent = new Intent(OpenIntents.SET_SHARE_SETTINGS_ACTION,
                mListUri);
        startActivityForResult(intent, SUBACTIVITY_LIST_SHARE_SETTINGS);

    }

    void setThemeSettings() {
        showDialog(DIALOG_THEME);
    }

    @Override
    public String onLoadTheme() {
        return loadListTheme();
    }

    @Override
    public void onSaveTheme(String theme) {
        saveListTheme(theme);
    }

    @Override
    public void onSetTheme(String theme) {
        setListTheme(theme);
        applyListTheme();
    }

    @Override
    public void onSetThemeForAll(String theme) {
        setThemeForAll(this, theme);
    }

    /**
     * Loads the theme settings for the currently selected theme.
     * <p/>
     * Up to version 1.2.1, only one of 3 hardcoded themes are available. These
     * are stored in 'skin_background' as '1', '2', or '3'.
     * <p/>
     * Starting in 1.2.2, also themes of other packages are allowed.
     *
     * @return
     */
    public String loadListTheme() {
        /*
         * long listId = getSelectedListId(); if (listId < 0) { // No valid list
         * - probably view is not active // and no item is selected. return 1;
         * // return default theme }
         */

        // Return default theme if something unexpected happens:
        if (mCursorShoppingLists == null) {
            return "1";
        }
        if (mCursorShoppingLists.getPosition() < 0) {
            return "1";
        }

        // mCursorListFilter has been set to correct position
        // by calling getSelectedListId(),
        // so we can read out further elements:
        return mCursorShoppingLists
                .getString(mStringListFilterSKINBACKGROUND);
    }

    public void saveListTheme(String theme) {
        long listId = getSelectedListId();
        if (listId < 0) {
            // No valid list - probably view is not active
            // and no item is selected.
            return; // return default theme
        }

        ContentValues values = new ContentValues();
        values.put(Lists.SKIN_BACKGROUND, theme);
        getContentResolver().update(
                Uri.withAppendedPath(Lists.CONTENT_URI,
                        mCursorShoppingLists.getString(0)), values, null, null
        );

        mCursorShoppingLists.requery();
    }

    /**
     * Calls a dialog for setting the locations alert.
     */
    void addLocationAlert() {

        // Call dialog as activity
        Intent intent = new Intent(OpenIntents.ADD_LOCATION_ALERT_ACTION,
                mListUri);
        // startSubActivity(intent, SUBACTIVITY_ADD_LOCATION_ALERT);
        startActivity(intent);
    }

    @Override
    protected Dialog onCreateDialog(int id) {

        switch (id) {

            case DIALOG_NEW_LIST:
                return new NewListDialog(this, new DialogActionListener() {

                    public void onAction(String name) {
                        createNewList(name);
                    }
                });

            case DIALOG_RENAME_LIST:
                return new RenameListDialog(this, getCurrentListName(),
                        new DialogActionListener() {

                            public void onAction(String name) {
                                renameList(name);
                            }
                        }
                );

            case DIALOG_EDIT_ITEM:
                return new EditItemDialog(this, mItemUri, mRelationUri,
                        mListItemUri);

            case DIALOG_DELETE_ITEM:
                return new AlertDialog.Builder(this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle(R.string.menu_delete_item)
                        .setMessage(R.string.delete_item_confirm)
                        .setPositiveButton(R.string.delete,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int whichButton) {
                                        deleteItem(mDeleteItemPosition);
                                    }
                                }
                        )
                        .setNegativeButton(android.R.string.cancel,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int whichButton) {
                                        // Don't do anything
                                    }
                                }
                        ).create();

            case DIALOG_THEME:
                return new ThemeDialog(this, this);

            case DIALOG_GET_FROM_MARKET:
                return new DownloadOIAppDialog(this,
                        DownloadOIAppDialog.OI_BARCODESCANNER);
            default:
                break;
        }
        return super.onCreateDialog(id);

    }

    // /////////////////////////////////////////////////////
    //
    // Helper functions
    //

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        super.onPrepareDialog(id, dialog);

        switch (id) {

            case DIALOG_RENAME_LIST:
                ((RenameListDialog) dialog).setName(getCurrentListName());
                break;

            case DIALOG_EDIT_ITEM:
                EditItemDialog d = (EditItemDialog) dialog;
                d.setItemUri(mItemUri, mListItemUri);
                d.setRelationUri(mRelationUri);
                d.setFocusField(mEditItemFocusField);

                String[] taglist = getTaglist();
                d.setTagList(taglist);

                d.setOnItemChangedListener(this);
                break;

            case DIALOG_THEME:
                ((ThemeDialog) dialog).prepareDialog();
                break;
            case DIALOG_GET_FROM_MARKET:
                DownloadOIAppDialog.onPrepareDialog(this, dialog);
                break;
            case DIALOG_DELETE_ITEM:
                if (mItemsView != null && dialog instanceof AlertDialog) {
                    ListAdapter adapter = mItemsView.getAdapter();
                    if (adapter != null && adapter instanceof CursorAdapter) {
                        Cursor c = (Cursor) adapter.getItem(mDeleteItemPosition);
                        if (c != null) {
                            String itemName = c.getString(mStringItemsITEMNAME);
                            if (itemName != null) {
                                ((AlertDialog) dialog).setMessage(
                                        getResources().getString(R.string.delete_item_confirm, itemName));
                            }
                        }
                    }
                }
                break;
            default:
                break;
        }
    }

    /**
     * Returns the ID of the selected shopping list.
     * <p/>
     * As a side effect, the item URI is updated. Returns -1 if nothing is
     * selected.
     *
     * @return ID of selected shopping list.
     */
    long getSelectedListId() {
        int pos = mShoppingListsView.getSelectedItemPosition();
        if (pos < 0) {
            // nothing selected - probably view is out of focus:
            // Do nothing.
            return -1;
        }

        // Obtain Id of currently selected shopping list:
        mCursorShoppingLists.moveToPosition(pos);

        long listId = mCursorShoppingLists.getLong(mStringListFilterID);

        mListUri = Uri.withAppendedPath(ShoppingContract.Lists.CONTENT_URI,
                Long.toString(listId));

        getIntent().setData(mListUri);

        return listId;
    }

    /**
     * sets the selected list to a specific list Id
     */
    void setSelectedListId(int id) {
        // Is there a nicer way to accomplish the following?
        // (we look through all elements to look for the
        // one entry that has the same ID as returned by
        // getDefaultList()).
        //
        // unfortunately, a SQL query won't work, as it would
        // return 1 row, but I still would not know which
        // row in the mCursorListFilter corresponds to that id.
        //
        // one could use: findViewById() but how will this
        // translate to the position in the list?
        mCursorShoppingLists.moveToPosition(-1);
        while (mCursorShoppingLists.moveToNext()) {
            int posId = mCursorShoppingLists.getInt(mStringListFilterID);
            if (posId == id) {
                int row = mCursorShoppingLists.getPosition();

                // if we found the Id, then select this in
                // the Spinner:
                setSelectedListPos(row);
                break;
            }
        }
    }

    private void setSelectedListPos(int pos) {
        mShoppingListsView.setTag(pos);

        mShoppingListsView.setSelection(pos);

        long id = getSelectedListId();

        // Set the theme based on the selected list:
        setListTheme(loadListTheme());

        if (id != mItemsView.getListId()) {
            fillItems(false);
        }
        applyListTheme();
        updateTitle();
        if (mShoppingListsView instanceof ListView) {
            ((ListView) mShoppingListsView).setItemChecked(pos, true);
        }
    }

    /**
     *
     */
    private void fillListFilter() {
        // Get a cursor with all lists

        mCursorShoppingLists = getContentResolver().query(Lists.CONTENT_URI,
                mStringListFilter, null, null, mSortOrder);
        startManagingCursor(mCursorShoppingLists);

        if (mCursorShoppingLists == null) {
            Log.e(TAG, "missing shopping provider");
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    R.layout.list_item_shopping_list,
                    new String[]{getString(R.string.no_shopping_provider)});
            setSpinnerAndDrawerListAdapter(adapter);

            return;
        }

        if (mCursorShoppingLists.getCount() < 1) {
            // We have to create default shopping list:
            long listId = ShoppingUtils.getList(this,
                    getText(R.string.my_shopping_list).toString());

            // Check if insertion really worked. Otherwise
            // we may end up in infinite recursion.
            if (listId < 0) {
                // for some reason insertion did not work.
                return;
            }

            // The insertion should have worked, so let us call ourselves
            // to try filling the list again:
            fillListFilter();
            return;
        }

        class mListContentObserver extends ContentObserver {

            public mListContentObserver(Handler handler) {
                super(handler);
                if (debug) {
                    Log.i(TAG, "mListContentObserver: Constructor");
                }
            }

            /*
             * (non-Javadoc)
             *
             * @see android.database.ContentObserver#deliverSelfNotifications()
             */
            @Override
            public boolean deliverSelfNotifications() {
                // TODO Auto-generated method stub
                if (debug) {
                    Log.i(TAG, "mListContentObserver: deliverSelfNotifications");
                }
                return super.deliverSelfNotifications();
            }

            /*
             * (non-Javadoc)
             *
             * @see android.database.ContentObserver#onChange(boolean)
             */
            @Override
            public void onChange(boolean arg0) {
                // TODO Auto-generated method stub
                if (debug) {
                    Log.i(TAG, "mListContentObserver: onChange");
                }

                mCursorShoppingLists.requery();

                super.onChange(arg0);
            }

        }

        mListContentObserver observer = new mListContentObserver(new Handler());
        mCursorShoppingLists.registerContentObserver(observer);

        // Register a ContentObserver, so that a new list can be
        // automatically detected.
        // mCursor

        /*
         * ArrayList<String> list = new ArrayList<String>(); // TODO Create
         * summary of all lists // list.add(ALL); while
         * (mCursorListFilter.next()) {
         * list.add(mCursorListFilter.getString(mStringListFilterNAME)); }
         * ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
         * android.R.layout.simple_spinner_item, list);
         * adapter.setDropDownViewResource(
         * android.R.layout.simple_spinner_dropdown_item);
         * mSpinnerListFilter.setAdapter(adapter);
         */

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
                // Use a template that displays a text view
                R.layout.list_item_shopping_list,
                // Give the cursor to the list adapter
                mCursorShoppingLists, new String[]{Lists.NAME},
                new int[]{R.id.text1});
        setSpinnerAndDrawerListAdapter(adapter);

    }

    public void onModeChanged() {

        if (debug) {
            Log.d(TAG, "onModeChanged()");
        }
        fillItems(false);

        invalidateOptionsMenu();

        updateTitle();
    }

    @Override
    public void updateActionBar() {
        invalidateOptionsMenu();
    }

    private String getCurrentListName() {
        long listId = getSelectedListId();

        // calling getSelectedListId also updates mCursorShoppingLists:
        if (listId >= 0) {
            return mCursorShoppingLists.getString(mStringListFilterNAME);
        } else {
            return "";
        }
    }

    private void fillItems(boolean onlyIfPrefsChanged) {
        if (debug) {
            Log.d(TAG, "fillItems()");
        }

        long listId = getSelectedListId();
        if (listId < 0) {
            // No valid list - probably view is not active
            // and no item is selected.
            Log.d(TAG, "fillItems: listId not availalbe");
            return;
        }

        // Insert any pending items received either through intents
        // or in onActivityResult:
        if (mItemsFromExtras.hasItems()) {
            mItemsFromExtras.insertInto(this, mItemsView);
        }

        updateFilterWidgets();

        if (onlyIfPrefsChanged
                && (lastAppliedPrefChange == PreferenceActivity.updateCount)) {
            return;
        }

        if (debug) {
            Log.d(TAG, "fillItems() for list " + listId);
        }
        lastAppliedPrefChange = PreferenceActivity.updateCount;
        mItemsView.fillItems(this, listId);

        // Also refresh AutoCompleteTextView:
        fillAutoCompleteTextViewAdapter(mEditText);
    }

    /**
     * Fill input field (AutoCompleteTextView) with suggestions: Unique item
     * names from all lists are collected. The adapter is filled in the
     * background.
     */
    public void fillAutoCompleteTextViewAdapter(final AutoCompleteTextView textView) {
        boolean limit_selections = PreferenceActivity.getCompleteFromCurrentListOnlyFromPrefs(this);
        String listId = null;
        if (limit_selections) {
            listId = mListUri.getLastPathSegment();
        }

        // TODO: Optimize: This routine is called too often.
        if (debug) {
            Log.d(TAG, "fill AutoCompleteTextViewAdapter");
        }

        new AsyncTask<String, Integer, ArrayAdapter<String>>() {

            private ArrayAdapter<String> adapter;

            @Override
            protected ArrayAdapter<String> doInBackground(String... params) {
                return fillAutoCompleteAdapter(params[0]);
            }

            @Override
            protected void onPostExecute(ArrayAdapter<String> adapter) {
                if (textView != null) {
                    // use result from doInBackground()
                    textView.setAdapter(adapter);
                }
            }

            private ArrayAdapter<String> fillAutoCompleteAdapter(String listId) {
                // Create list of item names
                Uri uri;
                String retCol;
                if (listId == null) {
                    uri = Items.CONTENT_URI;
                    retCol = Items.NAME;
                } else {
                    uri = Uri.parse("content://org.openintents.shopping/containsfull/list").buildUpon().
                            appendPath(listId).build();
                    retCol = "items.name";
                }
                List<String> autocompleteItems = new LinkedList<>();
                Cursor c = getContentResolver().query(uri, new String[]{retCol}, null, null, retCol + " asc");
                if (c != null) {
                    String lastitem = "";
                    while (c.moveToNext()) {
                        String newitem = c.getString(0);
                        // Only add items if they have not been added previously
                        // (list is sorted)
                        if (!newitem.equals(lastitem)) {
                            autocompleteItems.add(newitem);
                            lastitem = newitem;
                        }
                    }
                    c.close();
                }
                return new ArrayAdapter<>(ShoppingActivity.this,
                        android.R.layout.simple_dropdown_item_1line,
                        autocompleteItems);
            }

        }.execute(listId);
    }

    /**
     * Create list of tags.
     * <p/>
     * Tags for notes can be comma-separated. Here we create a list of the
     * unique tags.
     *
     * @return
     */
    String[] getTaglist() {
        return getTaglist(null);
    }

    /**
     * Create list of tags.
     * <p/>
     * Tags for notes can be comma-separated. Here we create a list of the
     * unique tags.
     *
     * @param listId
     * @return
     */
    String[] getTaglist(String listId) {
        Cursor c;

        if (listId == null) {
            c = getContentResolver().query(ShoppingContract.Items.CONTENT_URI,
                    new String[]{ShoppingContract.Items.TAGS}, null, null,
                    ShoppingContract.Items.DEFAULT_SORT_ORDER);
        } else {
            Uri uri = Uri.parse("content://org.openintents.shopping/listtags")
                    .buildUpon().appendPath(listId).build();
            c = getContentResolver().query(uri,
                    new String[]{ShoppingContract.ContainsFull.ITEM_TAGS},
                    null, null, null);
        }

        // Create a set of all tags (every tag should only appear once).
        HashSet<String> tagset = new HashSet<>();
        c.moveToPosition(-1);
        while (c.moveToNext()) {
            String tags = c.getString(0);
            if (tags != null) {
                // Split several tags in a line, separated by comma
                String[] smalltaglist = tags.split(",");
                for (String tag : smalltaglist) {
                    if (!tag.equals("")) {
                        tagset.add(tag.trim());
                    }
                }
            }
        }
        c.close();

        // Sort the list
        // 1. Convert HashSet to String list.
        ArrayList<String> list = new ArrayList<>();
        list.addAll(tagset);
        // 2. Sort the String list
        Collections.sort(list);
        // 3. Convert it to String array
        return list.toArray(new String[list.size()]);
    }

    /**
     * Tests whether the current list is shared via GTalk. (not local sharing!)
     *
     * @return true if SHARE_CONTACTS contains the '@' character.
     */
    boolean isCurrentListShared() {
        long listId = getSelectedListId();
        if (listId < 0) {
            // No valid list - probably view is not active
            // and no item is selected.
            return false;
        }

        // mCursorListFilter has been set to correct position
        // by calling getSelectedListId(),
        // so we can read out further elements:
        // String shareName =
        // mCursorListFilter.getString(mStringListFilterSHARENAME);
        String recipients = mCursorShoppingLists
                .getString(mStringListFilterSHARECONTACTS);

        // If recipients contains the '@' symbol, it is shared.
        return recipients.contains("@");
    }

    /**
     * This method is called when the sending activity has finished, with the
     * result it supplied.
     *
     * @param requestCode The original request code as given to startActivity().
     * @param resultCode  From sending activity as per setResult().
     * @param data        From sending activity as per setResult().
     * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (debug) {
            Log.i(TAG, "ShoppingView: onActivityResult. ");
        }

        if (requestCode == SUBACTIVITY_LIST_SHARE_SETTINGS) {
            if (debug) {
                Log.i(TAG, "SUBACTIVITY_LIST_SHARE_SETTINGS");
            }

            if (resultCode == RESULT_OK) {
                // Broadcast the intent

                Uri uri = data.getData();

                if (!mListUri.equals(uri)) {
                    Log.e(TAG, "Unexpected uri returned: Should be " + mListUri
                            + " but was " + uri);
                    return;
                }

                // TODO ???
                Bundle extras = data.getExtras();

                String sharename = extras
                        .getString(ShoppingContract.Lists.SHARE_NAME);
                String contacts = extras
                        .getString(ShoppingContract.Lists.SHARE_CONTACTS);

                if (debug) {
                    Log.i(TAG, "Received bundle: sharename: " + sharename
                            + ", contacts: " + contacts);
                }
            }

        } else if (REQUEST_CODE_CATEGORY_ALTERNATIVE == requestCode) {
            if (debug) {
                Log.d(TAG, "result received");
            }
            if (RESULT_OK == resultCode) {
                if (debug) {
                    Log.d(TAG, "result OK");
                }

                if (data.getExtras() != null) {
                    if (debug) {
                        Log.d(TAG, "extras received");
                    }
                    mItemsFromExtras.getShoppingExtras(data);
                }
            }
        } else if (REQUEST_PICK_LIST == requestCode) {
            if (debug) {
                Log.d(TAG, "result received");
            }

            if (RESULT_OK == resultCode) {
                int position = mMoveItemPosition;
                if (mMoveItemPosition >= 0) {
                    moveItem(position, Integer.parseInt(data.getData()
                            .getLastPathSegment()));
                }
            }

            mMoveItemPosition = -1;
        }
    }

    public void changeList(int value) {

        int pos = mShoppingListsView.getSelectedItemPosition();
        int newPos;

        if (pos < 0) {
            // nothing selected - probably view is out of focus:
            // Do nothing.
            newPos = -1;
        } else if (pos == 0) {
            newPos = mShoppingListsView.getCount() - 1;
        } else if (pos == mShoppingListsView.getCount()) {
            newPos = 0;
        } else {
            newPos = pos + value;
        }
        setSelectedListPos(newPos);
    }

    private void closeDrawer() {
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(mDrawerListsView);
        }
    }

    /**
     * With the requirement of OS3, making an intermediary decision depending
     * upon the widget
     *
     * @param adapter
     */
    private void setSpinnerAndDrawerListAdapter(ListAdapter adapter) {
        DrawerListAdapter adapterWrapper = (new DrawerListAdapter(this, adapter));
        mDrawerListsView.setAdapter(adapterWrapper);
        mDrawerListsView.setOnItemClickListener(adapterWrapper);
        mShoppingListsView.setAdapter(adapter);
    }

    @Override
    public void onItemChanged() {
        mItemsView.mCursorItems.requery();
        fillAutoCompleteTextViewAdapter(mEditText);
    }

    @Override
    public void onUndoAvailable(SnackbarUndoOperation undoOp) {
        Snackbar snackbar = Snackbar.make(mItemsView, undoOp.getDescription(this),
                Snackbar.LENGTH_LONG);
        snackbar.setAction(R.string.undo, undoOp);
        snackbar.show();
    }

    private class DrawerListAdapter implements WrapperListAdapter, OnItemClickListener {

        private ListAdapter mAdapter;
        private int mNumAboveList = 3;
        private int mNumBelowList = 1;
        private int mViewTypeNum;
        private LayoutInflater mInflater;

        public DrawerListAdapter(Context context, ListAdapter adapter) {
            mAdapter = adapter;
            mViewTypeNum = mAdapter.getViewTypeCount();
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            int list_pos, post_pos;
            int list_count = mAdapter.getCount();
            if (position < mNumAboveList) {
                return (position != 2); // not the "Lists" header
            } else if ((list_pos = position - mNumAboveList) < list_count) {
                // actual list entries can be selected
            } else {
                post_pos = list_pos - list_count;
                // New List button can be selected
            }
            return true;
        }

        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return mAdapter.getCount() + mNumAboveList + mNumBelowList;
        }

        @Override
        public Object getItem(int position) {
            int list_pos, post_pos;
            int list_count = mAdapter.getCount();
            if (position < mNumAboveList) {
            } else if ((list_pos = position - mNumAboveList) < list_count) {
                return mAdapter.getItem(list_pos);
            } else {
                post_pos = list_pos - list_count;
            }
            return null;
        }

        @Override
        public long getItemId(int position) {
            int list_pos, post_pos;
            int list_count = mAdapter.getCount();
            if (position < mNumAboveList) {

            } else if ((list_pos = position - mNumAboveList) < list_count) {
                return mAdapter.getItemId(list_pos);
            } else {
                post_pos = list_pos - list_count;
            }
            return -1;
        }

        @Override
        public int getItemViewType(int position) {
            int list_pos, post_pos;
            int list_count = mAdapter.getCount();
            if (position < mNumAboveList) {
            } else if ((list_pos = position - mNumAboveList) < list_count) {
                return mAdapter.getItemViewType(list_pos);
            }
            return IGNORE_ITEM_VIEW_TYPE;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            int list_pos;
            int list_count = mAdapter.getCount();
            View v = null;
            View v2;

            if (position < mNumAboveList) {
                switch (position) {
                    case 1:
                        v = mInflater.inflate(R.layout.drawer_item_radio, parent, false);
                        v2 = v.findViewById(R.id.text1);
                        ((TextView) v2).setText(R.string.menu_pick_items);
                        v2 = v.findViewById(R.id.mode_radio_button);
                        v2.setSelected(mItemsView.inAddItemsMode());
                        break;
                    case 0:
                        v = mInflater.inflate(R.layout.drawer_item_radio, parent, false);
                        v2 = v.findViewById(R.id.text1);
                        ((TextView) v2).setText(R.string.menu_start_shopping);
                        v2 = v.findViewById(R.id.mode_radio_button);
                        v2.setSelected(mItemsView.inShopMode());
                        break;
                    case 2:
                        v = mInflater.inflate(R.layout.drawer_item_header, parent, false);
                        ((TextView) v).setText(R.string.list); // fix me
                        break;
                    default:
                        break;

                }
            } else if ((list_pos = position - mNumAboveList) < list_count) {
                int curListPos = mShoppingListsView.getSelectedItemPosition();
                if (list_pos == curListPos) {
                    mDrawerListsView.setItemChecked(position, true);
                }
                v = mAdapter.getView(list_pos, convertView, parent);
            } else {
                v = mInflater.inflate(R.layout.drawer_item_radio, parent, false);
                v2 = v.findViewById(R.id.text1);
                ((TextView) v2).setText(R.string.new_list);
                v2 = v.findViewById(R.id.mode_radio_button);
                ((ImageView) v2).setImageResource(R.drawable.ic_menu_add_list);
            }
            return v;
        }

        @Override
        public int getViewTypeCount() {
            return mViewTypeNum;
        }

        @Override
        public boolean hasStableIds() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public void registerDataSetObserver(DataSetObserver observer) {
            mAdapter.registerDataSetObserver(observer);
        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {
            mAdapter.unregisterDataSetObserver(observer);
        }

        @Override
        public ListAdapter getWrappedAdapter() {
            return mAdapter;
        }

        public void onItemClick(AdapterView parent, View v,
                                int position, long id) {
            if (debug) {
                Log.d(TAG, "DrawerListAdapter: onItemClick");
            }

            int list_pos;
            int list_count = mAdapter.getCount();
            if (position < mNumAboveList) {
                // Pick Items or Shopping selected
                closeDrawer();
                if (position == 1) mItemsView.setAddItemsMode();
                else mItemsView.setInShopMode();
                mDrawerListsView.setItemChecked(position, true);
                mDrawerListsView.setItemChecked(1 - position, false);
                mDrawerListsView.invalidateViews();
                onModeChanged();
            } else if ((list_pos = position - mNumAboveList) < list_count) {
                // Update list cursor:
                mShoppingListsView.setSelection(list_pos);
                getSelectedListId();

                // Set the theme based on the selected list:
                setListTheme(loadListTheme());

                // If it's the same list we had before, requery only
                // if a preference has changed since then.
                fillItems(id == mItemsView.getListId());

                // Apply the theme after the list has been filled:
                applyListTheme();

                updateTitle();

                mDrawerListsView.setItemChecked(position, true);
                closeDrawer();
            } else {
                closeDrawer();
                showDialog(DIALOG_NEW_LIST);
            }
        }
    }

    private class ListSortActionProvider extends ActionProvider implements OnMenuItemClickListener {

        private Context mContext;
        private String[] mSortLabels;
        private String[] mSortVals;
        private Integer[] mSortValInts;

        public ListSortActionProvider(Context context) {
            super(context);
            mContext = context;
            mSortLabels = mContext.getResources().getStringArray(R.array.preference_sortorder_entries);
            mSortVals = mContext.getResources().getStringArray(R.array.preference_sortorder_entryvalues);
            mSortValInts = new Integer[mSortVals.length];
            for (int i = 0; i < mSortVals.length; i++)
                mSortValInts[i] = Integer.parseInt(mSortVals[i]);
        }

        @Override
        public View onCreateActionView() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean hasSubMenu() {
            return true;
        }

        public void onPrepareSubMenu(SubMenu subMenu) {
            int i;
            saveActiveList(false);
            subMenu.clear();
            int curSort = PreferenceActivity.getSortOrderIndexFromPrefs(mContext, MODE_IN_SHOP);
            for (i = 0; i < mSortLabels.length; i++) {
                subMenu.add(1, i, i, mSortLabels[i]).setOnMenuItemClickListener(this)
                        .setChecked(mSortValInts[i] == curSort);
            }
            subMenu.setGroupCheckable(1, true, true);
        }

        public boolean overridesItemVisibility() {
            return true;
        }

        public boolean isVisible() {
            boolean vis = true;
            if (!mItemsView.inShopMode()) {
                vis = false;
            } else if (!PreferenceActivity.getUsingPerListSortFromPrefs(mContext)) {
                vis = false;
            }
            if (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mDrawerListsView)) {
                vis = false;
            }
            return vis;
        }

        public boolean onMenuItemClick(MenuItem item) {
            int sortOrderNum = Integer.parseInt(mSortVals[item.getItemId()]);
            // String sortOrder = Contains.SORT_ORDERS[sortOrderNum];
            ContentValues values = new ContentValues();
            values.put(Lists.ITEMS_SORT, sortOrderNum);
            getContentResolver().update(mListUri, values, null, null);
            fillItems(false);
            return true;
        }
    }
}

/*
 * Copyright (C) 2007-2008 OpenIntents.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.openintents.provider.Alert;
import org.openintents.provider.Location.Locations;
import org.openintents.provider.Tag;
import org.openintents.shopping.R;

/**
 * Allows to edit the share settings for a shopping list.
 */
public class AddLocationAlertActivity extends Activity implements
        OnClickListener {
    /**
     * TAG for logging.
     */
    private static final String TAG = "AddLocationAlert";

    private static final int REQUEST_PICK_LOC = 1;

    private TextView mAlertAdded;
    private TextView mTags;
    private TextView mLocation;

    private Uri mShoppingListUri;

    private Tag mTag;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mTag = new Tag(this);

        setContentView(R.layout.activity_add_location_alert);

        // Get the uri of the list
        mShoppingListUri = getIntent().getData();

        // Set up click handlers for the text field and button
        mAlertAdded = (TextView) this.findViewById(R.id.alert_added_text);
        mTags = (TextView) this.findViewById(R.id.tags);
        mLocation = (TextView) this.findViewById(R.id.location);

        Button picklocation = (Button) this.findViewById(R.id.picklocation);
        picklocation.setOnClickListener(this);

        /*
         * Button addlocationalert = (Button)
         * this.findViewById(R.id.addlocationalert);
         * addlocationalert.setOnClickListener(this);
         */

        Button viewalerts = (Button) this.findViewById(R.id.viewalerts);
        viewalerts.setOnClickListener(this);

    }

    public void onClick(final View v) {
        switch (v.getId()) {
            case R.id.picklocation:
                pickLocation();
                break;
            // case R.id.addlocationalert:
            // addLocationAlert();
            // break;
            case R.id.viewalerts:
                viewAlerts();
                break;
            default:
                // Don't know what to do - do nothing.
                Log.e(TAG, "AddLocationAlertActivity: Unexpedted view id clicked.");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();

        // TODO Here we should store temporary information

    }

    public void pickLocation() {
        // Call the pick location activity
        Intent intent;

        intent = new Intent(Intent.ACTION_PICK, Locations.CONTENT_URI);
        try {
            startActivityForResult(intent, REQUEST_PICK_LOC);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.locations_not_installed,
                    Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Locations not installed", e);
        }
    }

    public void addLocationAlert() {
        // Add location into database
        addAlert(mLocation.getText().toString(), null, Intent.ACTION_VIEW,
                null, mShoppingListUri.toString());
    }

    public void viewAlerts() {
        // View list of alerts

        Intent intent = new Intent(Intent.ACTION_VIEW,
                Alert.Generic.CONTENT_URI);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.alerts_not_installed,
                    Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Alerts not installed", e);
        }
    }

    // / TODO: Simply copied this routine from LocationsView.java.
    // This should be a convenience function in the alerts provider!
    private void addAlert(String locationUri, String data, String actionName,
                          String type, String uri) {

        ContentValues values = new ContentValues();
        values.put(Alert.Location.ACTIVE, Boolean.TRUE);
        values.put(Alert.Location.ACTIVATE_ON_BOOT, Boolean.TRUE);
        values.put(Alert.Location.DISTANCE, 100L);
        values.put(Alert.Location.POSITION, locationUri);
        values.put(Alert.Location.INTENT, actionName);
        values.put(Alert.Location.INTENT_URI, uri);
        // TODO convert type to uri (?) or add INTENT_MIME_TYPE column
        // getContentResolver().insert(Alert.Location.CONTENT_URI, values);
        // using alert.insert will register alerts automatically.
        Alert.insert(Alert.Location.CONTENT_URI, values);
        int textId;
        if (uri != null) {
            textId = R.string.alert_added;

            mAlertAdded.setText(getString(R.string.location_alert_added));
        } else {
            textId = R.string.alert_not_added;

            mAlertAdded.setText(getString(R.string.alert_not_added));
        }
        Toast.makeText(this, textId, Toast.LENGTH_SHORT).show();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {

                case REQUEST_PICK_LOC:
                    /*
                     * mLocation.setText(bundle.getString(Locations.EXTRA_GEO));
                     * mTags.setText(mTag.findTags(data, ", "));
                     */

                    String geo = data.getStringExtra(Locations.EXTRA_GEO);
                    mLocation.setText(geo);
                    mTags.setText(mTag.findTags(data.getDataString(), ", "));
                    addLocationAlert();
                    break;
                default:
                    break;
            }
        }
    }

}


import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import org.openintents.shopping.R;
import org.openintents.shopping.ui.widget.ShoppingItemsView;

public class PickItemsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_pick_items);

        final ShoppingItemsView listItems = (ShoppingItemsView) findViewById(R.id.list_items);
        listItems.setPickItemsDlgMode();

        String listId = getIntent().getData().getLastPathSegment();
        listItems.fillItems(this, Long.parseLong(listId));
        // mListItems.setListTheme(ShoppingListView.MARK_CHECKBOX);
        listItems.setListTheme("1");
        // mListItems.setOnItemClickListener(new OnItemClickListener() {

        //
        // public void onItemClick(AdapterView parent, View v, int pos, long id)
        // {
        // mListItems.toggleItemRemovedFromList(pos);
        // v.invalidate();
        // }

        //
        // });

    }

    public void onButton1Click(View view) {
        finish();
    }
}


import android.app.Activity;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.openintents.shopping.R;
import org.openintents.shopping.library.provider.ShoppingContract;
import org.openintents.shopping.ui.widget.ShoppingItemsView;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class ShoppingTotalsHandler implements LoaderManager.LoaderCallbacks<Cursor> {
    private final static String TAG = "ShoppingTotalsHandler";
    private final static boolean debug = false;

    private Activity mActivity;
    private ShoppingItemsView mItemsView;
    private CursorLoader mCursorLoader;

    private long mListId;

    private TextView mTotalTextView;
    private TextView mPriTotalTextView;
    private TextView mTotalCheckedTextView;
    private TextView mCountTextView;

    private NumberFormat mPriceFormatter = DecimalFormat.getNumberInstance(Locale.ENGLISH);

    public ShoppingTotalsHandler(ShoppingItemsView view) {
        mItemsView = view;
        mActivity = (Activity) view.getContext();

        mTotalCheckedTextView = (TextView) mActivity.findViewById(R.id.total_1);
        mTotalTextView = (TextView) mActivity.findViewById(R.id.total_2);
        mPriTotalTextView = (TextView) mActivity.findViewById(R.id.total_3);
        mCountTextView = (TextView) mActivity.findViewById(R.id.count);

        mPriceFormatter.setMaximumFractionDigits(2);
        mPriceFormatter.setMinimumFractionDigits(2);
    }

    public void update(LoaderManager manager, long listId) {
        if (mCursorLoader == null) {
            mListId = listId;
            mCursorLoader = (CursorLoader) manager.initLoader(ShoppingActivity.LOADER_TOTALS, null, this);
        } else {
            if (mListId != listId) {
                mListId = listId;
                mCursorLoader.setUri(ShoppingContract.Subtotals.CONTENT_URI.buildUpon().appendPath(Long.toString(mListId)).build());
            }
            manager.restartLoader(ShoppingActivity.LOADER_TOTALS, null, this);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader loader = new CursorLoader(mActivity);
        loader.setProjection(ShoppingContract.Subtotals.PROJECTION);
        loader.setUri(ShoppingContract.Subtotals.CONTENT_URI.buildUpon().appendPath(Long.toString(mListId)).build());
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor total_cursor) {
        long total = 0;
        long totalchecked = 0;
        long priority_total = 0;
        int priority_threshold = PreferenceActivity.getSubtotalByPriorityThreshold(mActivity);
        boolean prioIncludesChecked =
                PreferenceActivity.prioritySubtotalIncludesChecked(mActivity);
        int numChecked = 0, numUnchecked = 0;

        total_cursor.moveToPosition(-1);
        while (total_cursor.moveToNext()) {
            long item_status = total_cursor.getLong(ShoppingContract.Subtotals.STATUS_INDEX);
            boolean isChecked = (item_status == ShoppingContract.Status.BOUGHT);

            if (item_status == ShoppingContract.Status.REMOVED_FROM_LIST) {
                continue;
            }

            long price = total_cursor.getLong(ShoppingContract.Subtotals.SUBTOTAL_INDEX);
            total += price;

            if (isChecked) {
                totalchecked += price;
                numChecked += total_cursor.getLong(ShoppingContract.Subtotals.COUNT_INDEX);
            } else if (item_status == ShoppingContract.Status.WANT_TO_BUY) {
                numUnchecked += total_cursor.getLong(ShoppingContract.Subtotals.COUNT_INDEX);
            }

            if (priority_threshold != 0 && (prioIncludesChecked || !isChecked)) {
                String priority_str = total_cursor.getString(ShoppingContract.Subtotals.PRIORITY_INDEX);
                if (priority_str != null) {
                    int priority = 0;
                    try {
                        priority = Integer.parseInt(priority_str);
                    } catch (NumberFormatException e) {
                        // pretend it's a 0 then...
                    }
                    if (priority != 0 && priority <= priority_threshold) {
                        priority_total += price;
                    }
                }
            }
        }

        if (debug) {
            Log.d(TAG, "Total: " + total + ", Checked: " + totalchecked + "(#" + numChecked + ")");
        }
        mItemsView.updateNumChecked(numChecked, numUnchecked);

        if (mTotalTextView == null || mTotalCheckedTextView == null) {
            // Most probably in "Add item" mode where no total is displayed
            return;
        }

        if (mItemsView.mPriceVisibility != View.VISIBLE) {
            // If price is not displayed, do not display total
            mTotalTextView.setVisibility(View.GONE);
            mPriTotalTextView.setVisibility(View.GONE);
            mTotalCheckedTextView.setVisibility(View.GONE);
            return;
        }


        mTotalTextView.setTextColor(mItemsView.mTextColorPrice);
        mPriTotalTextView.setTextColor(mItemsView.mTextColorPrice);
        mTotalCheckedTextView.setTextColor(mItemsView.mTextColorPrice);
        mCountTextView.setTextColor(mItemsView.mTextColorPrice);

        if (total != 0) {
            String s = mPriceFormatter.format(total * 0.01d);
            s = mActivity.getString(R.string.total, s);
            mTotalTextView.setText(s);
            mTotalTextView.setVisibility(View.VISIBLE);
        } else {
            mTotalTextView.setVisibility(View.GONE);
        }

        if (priority_total != 0) {
            final int[] captions = {0, R.string.priority1_total, R.string.priority2_total,
                    R.string.priority3_total, R.string.priority4_total};
            String s = mPriceFormatter.format(priority_total * 0.01d);
            s = mActivity.getString(captions[priority_threshold], s);
            mPriTotalTextView.setText(s);
            mPriTotalTextView.setVisibility(View.VISIBLE);
        } else {
            mPriTotalTextView.setVisibility(View.GONE);
        }

        if (totalchecked != 0) {
            String s = mPriceFormatter.format(totalchecked * 0.01d);
            s = mActivity.getString(R.string.total_checked, s);
            mTotalCheckedTextView.setText(s);
            mTotalCheckedTextView.setVisibility(View.VISIBLE);
            mCountTextView.setVisibility(View.VISIBLE);
        } else {
            mTotalCheckedTextView.setVisibility(View.GONE);
            mCountTextView.setVisibility(View.GONE);
        }
        mCountTextView.setText("#" + numChecked);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}



import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import org.openintents.intents.GeneralIntents;
import org.openintents.intents.ShoppingListIntents;
import org.openintents.shopping.R;
import org.openintents.shopping.library.provider.ShoppingContract;
import org.openintents.shopping.library.provider.ShoppingContract.Lists;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

/**
 * Activity to show list of shopping lists Used for INSERT_FROM_EXTRAS
 */
public class ShoppingListsActivity extends ListActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Cursor cursor = managedQuery(ShoppingContract.Lists.CONTENT_URI,
                new String[]{Lists._ID, Lists.NAME}, null, null,
                PreferenceActivity.getShoppingListSortOrderFromPrefs(this));
        setListAdapter(new SimpleCursorAdapter(this,
                android.R.layout.simple_list_item_1, cursor,
                new String[]{Lists.NAME}, new int[]{android.R.id.text1}));

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        if (action.equals(Intent.ACTION_CREATE_SHORTCUT)) {
            setTitle(R.string.pick_list_for_shortcut);
        }
        if (action.equals(GeneralIntents.ACTION_INSERT_FROM_EXTRAS)) {
            setTitle(R.string.pick_list_to_insert_items);
        }
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (Intent.ACTION_SEND.equals(action) && "text/plain".equals(type) && sharedText != null) {
            setTitle(R.string.pick_list_to_insert_items);
            // from now on handle this as an ACTION_INSERT_FROM_EXTRAS
            // for each line in the shared text, an item will be added
            intent.setAction(GeneralIntents.ACTION_INSERT_FROM_EXTRAS);
            intent.setType(ShoppingListIntents.TYPE_STRING_ARRAYLIST_SHOPPING);
            ArrayList<String> data = readSharedText(intent, sharedText);
            intent.putStringArrayListExtra("org.openintents.extra.STRING_ARRAYLIST_SHOPPING", data);
        }

    }

    private ArrayList<String> readSharedText(Intent intent, String sharedText) {
        ArrayList<String> data = new ArrayList<String>();
        BufferedReader reader = new BufferedReader(new StringReader(sharedText));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                data.add(line);
            }

            reader.close();
        } catch (IOException e) {
        }
        return data;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        String action = getIntent().getAction();

        // if (getCallingActivity() != null) {
        if (Intent.ACTION_PICK.equals(action)) {
            Intent data = new Intent();
            data.setData(Uri.withAppendedPath(Lists.CONTENT_URI,
                    String.valueOf(id)));
            setResult(RESULT_OK, data);
            finish();
        } else if (Intent.ACTION_CREATE_SHORTCUT.equals(action)) {
            Intent data = new Intent(Intent.ACTION_VIEW);
            Uri uri = Uri.withAppendedPath(Lists.CONTENT_URI,
                    String.valueOf(id));
            data.setData(uri);

            String title = getTitle(uri);

            Intent shortcut = new Intent(Intent.ACTION_CREATE_SHORTCUT);
            shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, title);
            shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, data);
            Intent.ShortcutIconResource sir = Intent.ShortcutIconResource
                    .fromContext(this, R.drawable.ic_launcher_shoppinglist);
            shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, sir);

            setResult(RESULT_OK, shortcut);
            finish();
        } else if (GeneralIntents.ACTION_INSERT_FROM_EXTRAS.equals(action)) {
            // Forward the intent to the shopping activity
            Intent intent = new Intent(getIntent());

            // Add the selected list
            intent.setClass(this,
                    org.openintents.shopping.ShoppingActivity.class);
            Uri uri = Uri.withAppendedPath(Lists.CONTENT_URI,
                    String.valueOf(id));
            intent.setData(uri);

            // After the user had a chance to look at the list, return to the
            // calling activity.
            intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);

            startActivity(intent);

            finish();
        }
        // }
    }

    private String getTitle(Uri uri) {
        Cursor c = getContentResolver().query(uri,
                new String[]{ShoppingContract.Lists.NAME}, null, null, null);
        if (c != null && c.moveToFirst()) {
            return c.getString(0);
        }
        if (c != null) {
            c.close();
        }

        // If there was a problem retrieving the list title
        // simply use the application name
        return getString(R.string.app_name);
    }
}


import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.view.View;

import org.openintents.shopping.R;
import org.openintents.shopping.library.provider.ShoppingContract;
import org.openintents.shopping.library.provider.ShoppingContract.Contains;
import org.openintents.shopping.ui.widget.ShoppingItemsView;

import java.util.ArrayList;

public class SnackbarUndoMultipleItemStatusOperation extends SnackbarUndoOperation {

    /**
     *
     */
    public static final int UNMARK_ALL = 0;
    public static final int MARK_ALL = 1;
    public static final int CLEAN_LIST = 2;
    private final ShoppingItemsView mShoppingItemsView;
    private long[] old_status = {ShoppingContract.Status.BOUGHT,
            ShoppingContract.Status.WANT_TO_BUY, ShoppingContract.Status.BOUGHT};
    private int[] resIds = {R.plurals.undoable_unmark_all, R.plurals.undoable_mark_all,
            R.plurals.undoable_clean_list};
    private Context mContext;
    private ArrayList<String> mItemList;

    public SnackbarUndoMultipleItemStatusOperation(ShoppingItemsView shoppingItemsView, Context context,
                                                   int type, long listId, boolean batch) {
        super(1, type, batch);
        mShoppingItemsView = shoppingItemsView;
        mContext = context;

        // remember all contains ids for listId where status = old_status
        String selection = "list_id = ? AND " + ShoppingContract.Contains.STATUS
                + " == " + old_status[mType];
        Cursor c = context.getContentResolver().query(
                Contains.CONTENT_URI, new String[]{Contains._ID},
                selection, new String[]{String.valueOf(listId)}, null);
        int numItems = c.getCount();
        mItemList = new ArrayList<String>();
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            mItemList.add(c.getString(0));
        }
    }

    @Override
    public String getDescription(Context context) {
        int count = mItemList.size();
        return String.format(context.getResources().getQuantityString(resIds[mType], count), count);
    }

    @Override
    public String getSingularDescription(Context context) {
        return getDescription(context);
    }

    @Override
    public void onClick(View view) {
        // here is where we get to try batch provider operation
        ArrayList<ContentProviderOperation> ops =
                new ArrayList<ContentProviderOperation>();
        for (int i = 0; i < mItemList.size(); i++) {
            String containsId = mItemList.get(i);
            Uri uri = Uri.withAppendedPath(Contains.CONTENT_URI, containsId);
            ops.add(ContentProviderOperation.newUpdate(uri).
                    withValue(ShoppingContract.Contains.STATUS, old_status[mType]).build());
        }
        try {
            mContext.getContentResolver().applyBatch(ShoppingContract.AUTHORITY, ops);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {

        }
        mShoppingItemsView.requery();
        mShoppingItemsView.invalidate();
    }
}

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioGroup;

import org.openintents.shopping.R;

public class LayoutChoiceActivity extends AppCompatActivity implements RadioGroup.OnCheckedChangeListener {

    public static boolean show(Activity context) {
        if (PreferenceActivity.getShowLayoutChoice(context) && PreferenceActivity.getUsingHoloSearchFromPrefs(context)) {
            context.startActivity(new Intent(context, LayoutChoiceActivity.class));
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_layout_choice);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final RadioGroup radioGroup = (RadioGroup) findViewById(R.id.layout_choice);

        if (PreferenceActivity.getUsingHoloSearchFromPrefs(this)) {
            radioGroup.check(R.id.layout_choice_actionbar);
        } else {
            radioGroup.check(R.id.layout_choice_bottom);
        }
        radioGroup.setOnCheckedChangeListener(this);

        findViewById(R.id.image_actionbar).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                radioGroup.check(R.id.layout_choice_actionbar);
            }
        });
        findViewById(R.id.image_bottom).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                radioGroup.check(R.id.layout_choice_bottom);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
        PreferenceActivity.setUsingHoloSearch(this, checkedId == R.id.layout_choice_actionbar);
        PreferenceActivity.setShowLayoutChoice(this, false);
        startActivity(new Intent(this, org.openintents.shopping.ShoppingActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        finish();
    }
}



public interface ToggleBoughtInputMethod {
    public void release();
}


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.Toast;

import org.openintents.shopping.R;
import org.openintents.shopping.library.provider.ShoppingContract.Stores;
import org.openintents.shopping.library.util.ShoppingUtils;
import org.openintents.shopping.ui.dialog.DialogActionListener;
import org.openintents.shopping.ui.dialog.RenameListDialog;
import org.openintents.shopping.ui.widget.StoreListView;

import java.util.List;

/**
 * UI for showing and editing stores for a specific item
 *
 * @author OpenIntents
 */
public class ItemStoresActivity extends Activity {

    public static final int MENU_RENAME_STORE = Menu.FIRST;
    public static final int MENU_DELETE_STORE = Menu.FIRST + 1;
    private static final int DIALOG_NEW_STORE = 1;
    private static final int DIALOG_RENAME_STORE = 2;
    private long mListId;
    private long mItemId;
    private StoreListView mItemStores;

    private int mSelectedStorePosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_itemstores);

        mItemStores = (StoreListView) findViewById(R.id.list_stores);

        mItemStores
                .setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {

                    public void onCreateContextMenu(ContextMenu contextmenu,
                                                    View view, ContextMenuInfo info) {
                        contextmenu.add(0, MENU_RENAME_STORE, 0,
                                R.string.menu_rename_store).setShortcut('1',
                                'r');
                        contextmenu.add(0, MENU_DELETE_STORE, 0,
                                R.string.menu_delete_store).setShortcut('2',
                                'd');
                    }

                });

        String listId;
        String itemId;

        List<String> pathSegs = getIntent().getData().getPathSegments();
        int num = pathSegs.size();
        listId = pathSegs.get(num - 2);
        itemId = pathSegs.get(num - 1);

        mListId = Long.parseLong(listId);
        mItemId = Long.parseLong(itemId);

        mItemStores.fillItems(this, Long.parseLong(listId),
                Long.parseLong(itemId));

        String itemname = ShoppingUtils.getItemName(this,
                Long.parseLong(itemId));
        setTitle(itemname + " @ ...");

        Button b = (Button) findViewById(R.id.button_ok);
        b.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mItemStores.applyUpdate();
                finish();
            }
        });
        b = (Button) findViewById(R.id.button_cancel);
        b.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mItemStores.undoChanges();
                finish();
            }
        });
        b = (Button) findViewById(R.id.button_add_store);
        b.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                showDialog(DIALOG_NEW_STORE);
            }
        });

    }

    @Override
    protected Dialog onCreateDialog(int id) {

        switch (id) {

            case DIALOG_NEW_STORE:
                return new NewStoreDialog(this, new DialogActionListener() {

                    public void onAction(String name) {
                        createStore(name);
                    }

                });

            case DIALOG_RENAME_STORE:
                return new NewStoreDialog(this, getSelectedStoreName(),
                        new DialogActionListener() {

                            public void onAction(String name) {
                                renameStore(name);
                            }
                        }
                );
            default:
                break;
        }
        return super.onCreateDialog(id);
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        super.onPrepareDialog(id, dialog);

        switch (id) {
            case DIALOG_NEW_STORE:
                ((NewStoreDialog) dialog).setName("");
                break;

            case DIALOG_RENAME_STORE:
                ((NewStoreDialog) dialog).setName(getSelectedStoreName());
                break;
            default:
                break;
        }
    }

    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item
                .getMenuInfo();

        mSelectedStorePosition = menuInfo.position;

        switch (item.getItemId()) {
            case MENU_RENAME_STORE:
                showDialog(DIALOG_RENAME_STORE);
                break;

            case MENU_DELETE_STORE:
                deleteStoreConfirm();
                break;
            default:
                break;
        }

        return true;
    }

    private String getSelectedStoreName() {
        return mItemStores.getStoreName(mSelectedStorePosition);
    }

    private void createStore(String name) {
        if (TextUtils.isEmpty(name)) {
            // User has not provided any name
            Toast.makeText(this, getString(R.string.please_enter_name),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        ShoppingUtils.getStore(getApplicationContext(), name, mListId);
        mItemStores.requery();
    }

    private void renameStore(String newName) {

        if (TextUtils.isEmpty(newName)) {
            // User has not provided any name
            Toast.makeText(this, getString(R.string.please_enter_name),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String storeId = mItemStores.getStoreId(mSelectedStorePosition);
        ContentValues values = new ContentValues();
        values.put(Stores.NAME, newName);
        getContentResolver().update(
                Uri.withAppendedPath(Stores.CONTENT_URI, storeId), values,
                null, null);

        mItemStores.requery();
    }

    /**
     * Confirm 'delete list' command by AlertDialog.
     */
    private void deleteStoreConfirm() {
        new AlertDialog.Builder(this)
                // .setIcon(R.drawable.alert_dialog_icon)
                .setTitle(R.string.confirm_delete_store)
                .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                // click Ok
                                deleteStore();
                            }
                        }
                )
                .setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                // click Cancel
                            }
                        }
                )
                // .create()
                .show();
    }

    // TODO: Convert into proper dialog that remains across screen orientation
    // changes.

    /**
     * Deletes currently selected store.
     */
    private void deleteStore() {
        String storeId = mItemStores.getStoreId(mSelectedStorePosition);
        ShoppingUtils.deleteStore(this, storeId);

        mItemStores.requery();
    }

    public class NewStoreDialog extends RenameListDialog {

        public NewStoreDialog(Context context) {
            super(context);

            setTitle(R.string.ask_new_store);
            mEditText.setHint("");
        }

        public NewStoreDialog(Context context, DialogActionListener listener) {
            super(context);

            setTitle(R.string.ask_new_store);
            mEditText.setHint("");
            setDialogActionListener(listener);
        }

        public NewStoreDialog(Context context, String name,
                              DialogActionListener listener) {
            super(context);

            setTitle(R.string.ask_new_store);
            mEditText.setHint("");
            setName(name);
            setDialogActionListener(listener);
        }
    }

}

/*******************************************************************************
 *      borrowed from AOSP UnifiedEmail app
 *
 *      Copyright (C) 2011 Google Inc.
 *      Licensed to The Android Open Source Project.
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 *******************************************************************************/

import android.content.Context;
import android.view.View;

/**
 * A simple holder class that stores the information to undo the application of a folder.
 */
public class SnackbarUndoOperation implements View.OnClickListener {
    public static final int UNDO = 0;
    public static final int ERROR = 1;
    protected final int mCount;
    protected final boolean mBatch;
    protected final int mType;

    /**
     * Create a SnackbarUndoOperation
     *
     * @param count Number of conversations this action would be applied to.
     * @param type  type of action
     * @param batch whether it is a batch operation
     */
    public SnackbarUndoOperation(int count, int type, boolean batch) {
        mCount = count;
        mBatch = batch;
        mType = type;
    }

    public int getType() {
        return mType;
    }

    public boolean isBatchUndo() {
        return mBatch;
    }

    /**
     * Get a string description of the operation that will be performed
     * when the user taps the undo bar.
     */
    public String getDescription(Context context) {
        final int resId = -1;

        return (resId == -1) ? "" :
                String.format(context.getResources().getQuantityString(resId, mCount), mCount);
    }

    public String getSingularDescription(Context context) {
        final int resId = -1;
        return (resId == -1) ? "" : context.getString(resId);
    }

    @Override
    public void onClick(View view) {

    }
}


import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.view.View;

import org.openintents.shopping.R;
import org.openintents.shopping.library.provider.ShoppingContract;
import org.openintents.shopping.ui.widget.ShoppingItemsView;

public class SnackbarUndoSingleItemStatusOperation extends SnackbarUndoOperation {

    /**
     *
     */
    private final ShoppingItemsView mShoppingItemsView;
    private Context mContext;
    private long mOldStatus;
    private long mNewStatus;
    private String mItemName;
    private String mContainsId;

    public SnackbarUndoSingleItemStatusOperation(ShoppingItemsView shoppingItemsView, Context context,
                                                 String containsId, String name,
                                                 long old_status, long new_status,
                                                 int type, boolean batch) {
        super(1, type, batch);
        mShoppingItemsView = shoppingItemsView;
        mContext = context;
        mContainsId = containsId;
        mItemName = name;
        mOldStatus = old_status;
        mNewStatus = new_status;
    }

    @Override
    public String getDescription(Context context) {
        return getSingularDescription(context);
    }

    @Override
    public String getSingularDescription(Context context) {
        int resId;

        if (mShoppingItemsView.inAddItemsMode()) {
            if (mNewStatus == ShoppingContract.Status.WANT_TO_BUY) {
                resId = R.string.undoable_added_item;
            } else {
                resId = R.string.undoable_removed_item;
            }
        } else {
            if (mNewStatus == ShoppingContract.Status.WANT_TO_BUY) {
                resId = R.string.undoable_unmarked_item;
            } else {
                resId = R.string.undoable_marked_item;
            }
        }
        return String.format(context.getResources().getString(resId), mItemName);
    }

    @Override
    public void onClick(View view) {
        ContentValues values = new ContentValues();
        values.put(ShoppingContract.Contains.STATUS, mOldStatus);
        mContext.getContentResolver().update(
                Uri.withAppendedPath(ShoppingContract.Contains.CONTENT_URI,
                        mContainsId), values, null, null
        );
        mShoppingItemsView.requery();
        mShoppingItemsView.invalidate();
    }
}

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.InputType;
import android.text.method.KeyListener;
import android.text.method.TextKeyListener;
import android.widget.Toast;

import org.openintents.shopping.R;
import org.openintents.shopping.library.provider.ShoppingContract.Contains;
import org.openintents.shopping.library.provider.ShoppingContract.Lists;
import org.openintents.shopping.library.util.ShoppingUtils;
import org.openintents.shopping.ui.widget.ShoppingItemsView;
import org.openintents.util.BackupManagerWrapper;
import org.openintents.util.IntentUtils;

public class PreferenceActivity extends android.preference.PreferenceActivity
        implements OnSharedPreferenceChangeListener {
    public static final String PREFS_SAMESORTFORPICK = "samesortforpick";
    public static final boolean PREFS_SAMESORTFORPICK_DEFAULT = false;
    public static final String PREFS_SORTORDER = "sortorder";
    public static final String PREFS_PICKITEMS_SORTORDER = "sortorderForPickItems";
    public static final String PREFS_SORTORDER_DEFAULT = "3";
    public static final String PREFS_PICKITEMS_SORTORDER_DEFAULT = "1";
    public static final String PREFS_SORTORDER_SHOPPINGLISTS = "sortorderForShoppingLists";
    public static final String PREFS_SORTORDER_SHOPPINGLISTS_DEFAULT = "0";
    public static final String PREFS_FONTSIZE = "fontsize";
    public static final String PREFS_FONTSIZE_DEFAULT = "2";
    public static final String PREFS_ORIENTATION = "orientation";
    public static final String PREFS_ORIENTATION_DEFAULT = "-1";
    @Deprecated
    public static final String PREFS_LOADLASTUSED = "loadlastused";
    @Deprecated
    public static final boolean PREFS_LOADLASTUSED_DEFAULT = true;
    public static final String PREFS_LASTUSED = "lastused";
    public static final String PREFS_LASTLIST_POSITION = "lastlist_position";
    public static final String PREFS_LASTLIST_TOP = "lastlist_top";
    public static final String PREFS_HIDECHECKED = "hidechecked";
    public static final boolean PREFS_HIDECHECKED_DEFAULT = false;
    public static final String PREFS_FASTSCROLL = "fastscroll";
    public static final boolean PREFS_FASTSCROLL_DEFAULT = false;
    public static final String PREFS_CAPITALIZATION = "capitalization";
    public static final String PREFS_SHOW_PRICE = "showprice";
    public static final boolean PREFS_SHOW_PRICE_DEFAULT = true;
    public static final String PREFS_PERSTOREPRICES = "perstoreprices";
    public static final boolean PREFS_PERSTOREPRICES_DEFAULT = false;
    public static final String PREFS_SHOW_TAGS = "showtags";
    public static final boolean PREFS_SHOW_TAGS_DEFAULT = true;
    public static final String PREFS_SHOW_QUANTITY = "showquantity";
    public static final boolean PREFS_SHOW_QUANTITY_DEFAULT = true;
    public static final String PREFS_SHOW_UNITS = "showunits";
    public static final boolean PREFS_SHOW_UNITS_DEFAULT = true;
    public static final String PREFS_SHOW_PRIORITY = "showpriority";
    public static final boolean PREFS_SHOW_PRIORITY_DEFAULT = true;
    public static final String PREFS_SCREENLOCK = "screenlock";
    public static final boolean PREFS_SCREENLOCK_DEFAULT = false;
    public static final String PREFS_RESETQUANTITY = "resetquantity";
    public static final boolean PREFS_RESETQUANTITY_DEFAULT = false;
    public static final String PREFS_ADDFORBARCODE = "addforbarcode";
    public static final boolean PREFS_ADDFORBARCODE_DEFAULT = false;
    public static final String PREFS_SHAKE = "shake";
    public static final boolean PREFS_SHAKE_DEFAULT = false;
    public static final String PREFS_MARKET_EXTENSIONS = "preference_market_extensions";
    public static final String PREFS_MARKET_THEMES = "preference_market_themes";
    public static final String PREFS_THEME_SET_FOR_ALL = "theme_set_for_all";
    public static final String PREFS_SCREEN_ADDONS = "preference_screen_addons";
    public static final String PREFS_PRIOSUBTOTAL = "priority_subtotal_threshold";
    public static final String PREFS_PRIOSUBTOTAL_DEFAULT = "0";
    public static final String PREFS_PRIOSUBINCLCHECKED = "priosubtotal_includes_checked";
    public static final boolean PREFS_PRIOSUBINCLCHECKED_DEFAULT = true;
    public static final String PREFS_PICKITEMSINLIST = "pickitemsinlist";
    public static final boolean PREFS_PICKITEMSINLIST_DEFAULT = false;
    public static final String PREFS_QUICKEDITMODE = "quickedit";
    public static final boolean PREFS_QUICKEDITMODE_DEFAULT = false;
    public static final String PREFS_USE_FILTERS = "use_filters";
    public static final boolean PREFS_USE_FILTERS_DEFAULT = false;
    public static final String PREFS_CURRENT_LIST_COMPLETE = "autocomplete_only_this_list";
    public static final boolean PREFS_CURRENT_LIST_COMPLETE_DEFAULT = false;
    public static final String PREFS_SORT_PER_LIST = "perListSort";
    public static final boolean PREFS_SORT_PER_LIST_DEFAULT = false;
    public static final String PREFS_HOLO_SEARCH = "holosearch";
    public static final boolean PREFS_HOLO_SEARCH_DEFAULT = true;
    public static final String PREFS_SHOW_LAYOUT_CHOICE = "show_layout_choice";
    public static final String PREFS_RESET_ALL_SETTINGS = "reset_all_settings";
    public static final int PREFS_CAPITALIZATION_DEFAULT = 1;
    public static final String EXTRA_SHOW_GET_ADD_ONS = "show_get_add_ons";
    private static final String TAG = "PreferenceActivity";
    private static final TextKeyListener.Capitalize[] smCapitalizationSettings = {
            TextKeyListener.Capitalize.NONE,
            TextKeyListener.Capitalize.SENTENCES,
            TextKeyListener.Capitalize.WORDS};
    private static final int[] smCapitalizationInputTypes = {
            InputType.TYPE_CLASS_TEXT,
            InputType.TYPE_TEXT_FLAG_CAP_SENTENCES,
            InputType.TYPE_TEXT_FLAG_CAP_WORDS};
    public static int updateCount;
    private static boolean mBackupManagerAvailable;
    private static boolean mFilterCompletionChanged;

    static {
        try {
            BackupManagerWrapper.checkAvailable();
            mBackupManagerAvailable = true;
        } catch (Throwable e) {
            mBackupManagerAvailable = false;
        }
    }

    private ListPreference mPrioSubtotal;
    private CheckBoxPreference mIncludesChecked;
    private ListPreference mPickItemsSort;

    public static int getFontSizeFromPrefs(Context context) {
        return Integer.parseInt(PreferenceManager
                .getDefaultSharedPreferences(context).getString(PREFS_FONTSIZE,
                        PREFS_FONTSIZE_DEFAULT));
    }

    public static int getOrientationFromPrefs(Context context) {
        return Integer.parseInt(PreferenceManager
                .getDefaultSharedPreferences(context).getString(
                        PREFS_ORIENTATION, PREFS_ORIENTATION_DEFAULT));
    }

    public static boolean getCompleteFromCurrentListOnlyFromPrefs(Context context) {
        return PreferenceManager
                .getDefaultSharedPreferences(context).getBoolean(PREFS_CURRENT_LIST_COMPLETE,
                        PREFS_CURRENT_LIST_COMPLETE_DEFAULT);
    }

    public static boolean getCompletionSettingChanged(Context context) {
        return mFilterCompletionChanged;
    }

    public static boolean getUsingPerStorePricesFromPrefs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PREFS_PERSTOREPRICES, PREFS_PERSTOREPRICES_DEFAULT);
    }

    public static boolean getQuickEditModeFromPrefs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PREFS_QUICKEDITMODE, PREFS_QUICKEDITMODE_DEFAULT);
    }

    public static boolean getUsingFiltersFromPrefs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PREFS_USE_FILTERS, PREFS_USE_FILTERS_DEFAULT);
    }

    public static boolean getUsingHoloSearchFromPrefs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PREFS_HOLO_SEARCH, PREFS_HOLO_SEARCH_DEFAULT);
    }

    public static boolean getPickItemsInListFromPrefs(Context context) {
        // boolean using = PreferenceManager.getDefaultSharedPreferences(context)
        //		.getBoolean(PREFS_PICKITEMSINLIST,
        //				PREFS_PICKITEMSINLIST_DEFAULT);
        // return using;
        return true;
    }

    public static boolean getUsingPerListSortFromPrefs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PREFS_SORT_PER_LIST,
                        PREFS_SORT_PER_LIST_DEFAULT);
    }

    /**
     * Returns the sort order for the notes list based on the user preferences.
     * Performs error-checking.
     *
     * @param context The context to grab the preferences from.
     */
    static public int getSortOrderIndexFromPrefs(Context context, int mode, long listId) {
        int sortOrder = 0;

        if (mode != ShoppingItemsView.MODE_IN_SHOP) {
            boolean followShopping = PreferenceManager
                    .getDefaultSharedPreferences(context).getBoolean(
                            PREFS_SAMESORTFORPICK,
                            PREFS_SAMESORTFORPICK_DEFAULT);
            if (followShopping) {
                mode = ShoppingItemsView.MODE_IN_SHOP;
            }
        }

        if (mode != ShoppingItemsView.MODE_IN_SHOP) {
            // use the pick-items-specific value, if there is one
            try {
                sortOrder = Integer.parseInt(PreferenceManager
                        .getDefaultSharedPreferences(context).getString(
                                PREFS_PICKITEMS_SORTORDER,
                                PREFS_PICKITEMS_SORTORDER_DEFAULT));
            } catch (NumberFormatException e) {
                // Guess somebody messed with the preferences and put a string
                // into
                // this field. We'll follow shopping mode then.
                mode = ShoppingItemsView.MODE_IN_SHOP;
            }
        }

        if (mode == ShoppingItemsView.MODE_IN_SHOP) {

            boolean set = false;
            if (PreferenceActivity.getUsingPerListSortFromPrefs(context)) {
                String sortOrderStr = ShoppingUtils.getListSortOrder(context,
                        listId);
                if (sortOrderStr != null) {
                    try {
                        sortOrder = Integer.parseInt(sortOrderStr);
                        set = true;
                    } catch (NumberFormatException e) {
                        // Guess somebody messed with the preferences and put a string
                        // into
                        // this field. We'll use the default value then.
                    }
                }
            }

            if (set == false) {
                try {
                    sortOrder = Integer.parseInt(PreferenceManager
                            .getDefaultSharedPreferences(context).getString(
                                    PREFS_SORTORDER, PREFS_SORTORDER_DEFAULT));
                } catch (NumberFormatException e) {
                    // Guess somebody messed with the preferences and put a string
                    // into
                    // this field. We'll use the default value then.
                }
            }
        }

        if (sortOrder >= 0 && sortOrder < Contains.SORT_ORDERS.length) {
            return sortOrder;
        }

        // Value out of range - somebody messed with the preferences.
        return 0;
    }

    static public int getSortOrderIndexFromPrefs(Context context, int mode) {
        long listId = ShoppingUtils.getDefaultList(context);
        return getSortOrderIndexFromPrefs(context, mode, listId);
    }

    static public String getSortOrderFromPrefs(Context context, int mode) {
        int index = getSortOrderIndexFromPrefs(context, mode);
        return Contains.SORT_ORDERS[index];
    }

    static public String getSortOrderFromPrefs(Context context, int mode, long listId) {
        int index = getSortOrderIndexFromPrefs(context, mode, listId);
        return Contains.SORT_ORDERS[index];
    }

    static public boolean prefsStatusAffectsSort(Context context, int mode) {
        int index = getSortOrderIndexFromPrefs(context, mode);
        boolean affects = Contains.StatusAffectsSortOrder[index];
        if (mode == ShoppingItemsView.MODE_IN_SHOP && !affects) {
            // in shopping mode we should also invalidate display when
            // marking items if we are hiding checked items.
            affects = getHideCheckedItemsFromPrefs(context);
        }
        return affects;
    }

    public static String getShoppingListSortOrderFromPrefs(Context context) {
        int index = Integer.parseInt(PreferenceManager
                .getDefaultSharedPreferences(context).getString(
                        PREFS_SORTORDER_SHOPPINGLISTS,
                        PREFS_SORTORDER_SHOPPINGLISTS_DEFAULT));
        if (index >= 0 && index < Lists.SORT_ORDERS.length) {
            return Lists.SORT_ORDERS[index];
        }

        return Lists.DEFAULT_SORT_ORDER;
    }

    public static boolean getHideCheckedItemsFromPrefs(Context context) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        return prefs.getBoolean(PREFS_HIDECHECKED, PREFS_HIDECHECKED_DEFAULT);
    }

    public static boolean getFastScrollEnabledFromPrefs(Context context) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        return prefs.getBoolean(PREFS_FASTSCROLL, PREFS_FASTSCROLL_DEFAULT);
    }

    private static int getSubtotalByPriorityThreshold(SharedPreferences prefs) {
        String pref = prefs.getString(PREFS_PRIOSUBTOTAL,
                PREFS_PRIOSUBTOTAL_DEFAULT);
        int threshold = 0;
        try {
            threshold = Integer.parseInt(pref);
        } catch (NumberFormatException e) {
            // Guess somebody messed with the preferences and put a string into
            // this
            // field. We'll use the default value then.
        }
        return threshold;
    }

    public static int getSubtotalByPriorityThreshold(Context context) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        return getSubtotalByPriorityThreshold(prefs);
    }

    public static boolean prioritySubtotalIncludesChecked(Context context) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        return prefs.getBoolean(PREFS_PRIOSUBINCLCHECKED,
                PREFS_PRIOSUBINCLCHECKED_DEFAULT);
    }

    /**
     * Returns a KeyListener for edit texts that will match the capitalization
     * preferences of the user.
     *
     * @ param context The context to grab the preferences from.
     */
    static public KeyListener getCapitalizationKeyListenerFromPrefs(
            Context context) {
        int capitalization = PREFS_CAPITALIZATION_DEFAULT;
        try {
            capitalization = Integer.parseInt(PreferenceManager
                    .getDefaultSharedPreferences(context).getString(
                            PREFS_CAPITALIZATION,
                            Integer.toString(PREFS_CAPITALIZATION_DEFAULT)));
        } catch (NumberFormatException e) {
            // Guess somebody messed with the preferences and put a string
            // into this
            // field. We'll use the default value then.
        }

        if (capitalization < 0
                || capitalization > smCapitalizationSettings.length) {
            // Value out of range - somebody messed with the preferences.
            capitalization = PREFS_CAPITALIZATION_DEFAULT;
        }

        return new TextKeyListener(smCapitalizationSettings[capitalization],
                true);
    }

    /**
     * Returns InputType for the search bar based on the capitalization
     * preferences of the user.
     *
     * @ param context The context to grab the preferences from.
     */
    static public int getSearchInputTypeFromPrefs(
            Context context) {
        int capitalization = PREFS_CAPITALIZATION_DEFAULT;
        try {
            capitalization = Integer.parseInt(PreferenceManager
                    .getDefaultSharedPreferences(context).getString(
                            PREFS_CAPITALIZATION,
                            Integer.toString(PREFS_CAPITALIZATION_DEFAULT)));
        } catch (NumberFormatException e) {
            // Guess somebody messed with the preferences and put a string
            // into this
            // field. We'll use the default value then.
        }

        if (capitalization < 0
                || capitalization > smCapitalizationSettings.length) {
            // Value out of range - somebody messed with the preferences.
            capitalization = PREFS_CAPITALIZATION_DEFAULT;
        }

        return smCapitalizationInputTypes[capitalization];
    }

    public static boolean getThemeSetForAll(Context context) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        return prefs.getBoolean(PREFS_THEME_SET_FOR_ALL, false);
    }

    public static boolean getResetQuantity(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PREFS_RESETQUANTITY, PREFS_RESETQUANTITY_DEFAULT);
    }

    public static boolean getAddForBarcode(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PREFS_ADDFORBARCODE, PREFS_ADDFORBARCODE_DEFAULT);
    }

    public static void setThemeSetForAll(Context context, boolean setForAll) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        Editor ed = prefs.edit();
        ed.putBoolean(PREFS_THEME_SET_FOR_ALL, setForAll);
        ed.commit();
    }

    public static void setUsingHoloSearch(Context context, boolean useHoloSearch) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        Editor ed = prefs.edit();
        ed.putBoolean(PREFS_HOLO_SEARCH, useHoloSearch);
        ed.apply();
    }

    public static boolean getShowLayoutChoice(Context context) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        return prefs.getBoolean(PREFS_SHOW_LAYOUT_CHOICE, true);
    }

    public static void setShowLayoutChoice(Context context, boolean showLayoutChoice) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);

        prefs.edit()
                .putBoolean(PREFS_SHOW_LAYOUT_CHOICE, showLayoutChoice)
                .apply();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        // Set enabled state of Market preference
        PreferenceScreen sp = (PreferenceScreen) findPreference(PREFS_MARKET_EXTENSIONS);
        sp.setEnabled(isMarketAvailable());
        sp = (PreferenceScreen) findPreference(PREFS_MARKET_THEMES);
        sp.setEnabled(isMarketAvailable());

        mPrioSubtotal = (ListPreference) findPreference(PREFS_PRIOSUBTOTAL);
        mPickItemsSort = (ListPreference) findPreference(PREFS_PICKITEMS_SORTORDER);

        mIncludesChecked = (CheckBoxPreference) findPreference(PREFS_PRIOSUBINCLCHECKED);

        Preference layoutChoicePreference = findPreference("layout_choice");
        layoutChoicePreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(PreferenceActivity.this, LayoutChoiceActivity.class));
                return true;
            }
        });
        SharedPreferences shared = getPreferenceScreen().getSharedPreferences();
        updatePrioSubtotalSummary(shared);
        updatePickItemsSortPref(shared);
        resetAllSettings(shared);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (getIntent() != null && getIntent().hasExtra(EXTRA_SHOW_GET_ADD_ONS)) {
            // Open License section directly:
            PreferenceScreen licensePrefScreen = (PreferenceScreen) getPreferenceScreen()
                    .findPreference(PREFS_SCREEN_ADDONS);
            setPreferenceScreen(licensePrefScreen);
        }
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
        mFilterCompletionChanged = false;
    }

    @Override
    protected void onPause() {
        if (mBackupManagerAvailable) {
            new BackupManagerWrapper(this).dataChanged();
        }
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        updateCount++;
        if (key.equals(PREFS_PRIOSUBTOTAL)) {
            updatePrioSubtotalSummary(prefs);
        }
        if (key.equals(PREFS_SAMESORTFORPICK)) {
            updatePickItemsSortPref(prefs);
        }
        if (key.equals(PREFS_CURRENT_LIST_COMPLETE)) {
            mFilterCompletionChanged = true;
        }
    }

    private void resetAllSettings(final SharedPreferences prefs) {
        Preference resetAllSettings = findPreference(PREFS_RESET_ALL_SETTINGS);
        resetAllSettings
                .setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        AlertDialog alert = new AlertDialog.Builder(
                                PreferenceActivity.this).create();
                        alert.setTitle(R.string.preference_reset_all_settings);
                        alert.setMessage(getString(R.string.preference_reset_all_settings_alert));
                        alert.setButton(getString(android.R.string.yes),
                                new OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                        SharedPreferences.Editor editor = prefs
                                                .edit();
                                        // Main
                                        editor.putString(PREFS_FONTSIZE,
                                                PREFS_FONTSIZE_DEFAULT);
                                        editor.putString(PREFS_SORTORDER,
                                                PREFS_SORTORDER_DEFAULT);
                                        // Main advanced
                                        editor.putString(
                                                PREFS_CAPITALIZATION,
                                                String.valueOf(PREFS_CAPITALIZATION_DEFAULT));
                                        editor.putString(PREFS_ORIENTATION,
                                                PREFS_ORIENTATION_DEFAULT);
                                        editor.putBoolean(PREFS_HIDECHECKED,
                                                PREFS_HIDECHECKED_DEFAULT);
                                        editor.putBoolean(PREFS_FASTSCROLL,
                                                PREFS_FASTSCROLL_DEFAULT);
                                        editor.putBoolean(PREFS_SHAKE,
                                                PREFS_SHAKE_DEFAULT);
                                        editor.putBoolean(PREFS_PERSTOREPRICES,
                                                PREFS_PERSTOREPRICES_DEFAULT);
                                        editor.putBoolean(PREFS_ADDFORBARCODE,
                                                PREFS_ADDFORBARCODE_DEFAULT);
                                        editor.putBoolean(PREFS_SCREENLOCK,
                                                PREFS_SCREENLOCK_DEFAULT);
                                        editor.putBoolean(PREFS_QUICKEDITMODE,
                                                PREFS_QUICKEDITMODE_DEFAULT);
                                        editor.putBoolean(PREFS_USE_FILTERS,
                                                PREFS_USE_FILTERS_DEFAULT);
                                        editor.putBoolean(PREFS_RESETQUANTITY,
                                                PREFS_RESETQUANTITY_DEFAULT);
                                        editor.putBoolean(PREFS_HOLO_SEARCH,
                                                PREFS_HOLO_SEARCH_DEFAULT);
                                        // Appearance
                                        editor.putBoolean(PREFS_SHOW_PRICE,
                                                PREFS_SHOW_PRICE_DEFAULT);
                                        editor.putBoolean(PREFS_SHOW_TAGS,
                                                PREFS_SHOW_TAGS_DEFAULT);
                                        editor.putBoolean(PREFS_SHOW_UNITS,
                                                PREFS_SHOW_UNITS_DEFAULT);
                                        editor.putBoolean(PREFS_SHOW_QUANTITY,
                                                PREFS_SHOW_QUANTITY_DEFAULT);
                                        editor.putBoolean(PREFS_SHOW_PRIORITY,
                                                PREFS_SHOW_PRIORITY_DEFAULT);
                                        // Pick items
                                        editor.putBoolean(
                                                PREFS_SAMESORTFORPICK,
                                                PREFS_SAMESORTFORPICK_DEFAULT);
                                        editor.putString(
                                                PREFS_PICKITEMS_SORTORDER,
                                                PREFS_PICKITEMS_SORTORDER_DEFAULT);
                                        editor.putBoolean(
                                                PREFS_PICKITEMSINLIST,
                                                PREFS_PICKITEMSINLIST_DEFAULT);
                                        editor.putString(
                                                PREFS_SORTORDER_SHOPPINGLISTS,
                                                PREFS_SORTORDER_SHOPPINGLISTS_DEFAULT);
                                        // Subtotal
                                        editor.putString(PREFS_PRIOSUBTOTAL,
                                                PREFS_PRIOSUBTOTAL_DEFAULT);
                                        editor.putBoolean(
                                                PREFS_PRIOSUBINCLCHECKED,
                                                PREFS_PRIOSUBINCLCHECKED_DEFAULT);

                                        editor.putBoolean(PREFS_CURRENT_LIST_COMPLETE, PREFS_CURRENT_LIST_COMPLETE_DEFAULT);
                                        editor.putBoolean(PREFS_SORT_PER_LIST, PREFS_SORT_PER_LIST_DEFAULT);

                                        editor.commit();

                                        Toast.makeText(
                                                PreferenceActivity.this,
                                                R.string.preference_reset_all_settings_done,
                                                Toast.LENGTH_LONG).show();
                                        finish();
                                    }
                                }
                        );
                        alert.setButton2(getString(android.R.string.cancel),
                                new OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                        dialog.dismiss();
                                    }
                                }
                        );
                        alert.show();
                        return false;
                    }
                });
    }

    private void updatePrioSubtotalSummary(SharedPreferences prefs) {
        int threshold = getSubtotalByPriorityThreshold(prefs);
        CharSequence[] labels = mPrioSubtotal.getEntries();
        mPrioSubtotal.setSummary(labels[threshold]);
        mIncludesChecked.setEnabled(threshold != 0);
    }

    private void updatePickItemsSortPref(SharedPreferences prefs) {
        boolean sameSort = prefs.getBoolean(PREFS_SAMESORTFORPICK,
                PREFS_SAMESORTFORPICK_DEFAULT);
        mPickItemsSort.setEnabled(!sameSort);
        // maybe we should set the label to say the active sort order.
        // but not tonight.
        // CharSequence labels[] = mPickItemsSort.getEntries();
    }

    /**
     * Check whether Market is available.
     *
     * @return true if Market is available
     */
    private boolean isMarketAvailable() {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri
                .parse(getString(R.string.preference_market_extensions_link)));
        return IntentUtils.isIntentAvailable(this, i);
    }
}


import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import org.openintents.intents.ShoppingListIntents;
import org.openintents.shopping.LogConstants;
import org.openintents.shopping.R;
import org.openintents.shopping.library.provider.ShoppingContract;
import org.openintents.shopping.ui.widget.ShoppingItemsView;

import java.util.List;

public class ItemsFromExtras {
    private static final boolean debug = LogConstants.debug;
    private static final String TAG = "ItemsFromExtras";
    /**
     * The items to add to the shopping list.
     * <p/>
     * Received as a string array list in the intent extras.
     */
    private List<String> mExtraItems;
    /**
     * The quantities for items to add to the shopping list.
     * <p/>
     * Received as a string array list in the intent extras.
     */
    private List<String> mExtraQuantities;
    /**
     * The prices for items to add to the shopping list.
     * <p/>
     * Received as a string array list in the intent extras.
     */
    private List<String> mExtraPrices;
    /**
     * The barcodes for items to add to the shopping list.
     * <p/>
     * Received as a string array list in the intent extras.
     */
    private List<String> mExtraBarcodes;
    /**
     * The list URI received together with intent extras.
     */
    private Uri mExtraListUri;

    /**
     * Inserts new item from string array received in intent extras.
     */
    public void insertInto(ShoppingActivity activity, ShoppingItemsView itemsView) {
        if (mExtraItems != null) {
            // Make sure we are in the correct list:
            if (mExtraListUri != null) {
                long listId = Long
                        .parseLong(mExtraListUri.getLastPathSegment());
                if (debug) {
                    Log.d(TAG, "insert items into list " + listId);
                }
                if (listId != activity.getSelectedListId()) {
                    if (debug) {
                        Log.d(TAG, "set new list: " + listId);
                    }
                    activity.setSelectedListId((int) listId);
                }
                itemsView.fillItems(activity, listId);
            }

            int max = mExtraItems.size();
            int maxQuantity = (mExtraQuantities != null) ? mExtraQuantities
                    .size() : -1;
            int maxPrice = (mExtraPrices != null) ? mExtraPrices.size() : -1;
            int maxBarcode = (mExtraBarcodes != null) ? mExtraBarcodes.size()
                    : -1;
            for (int i = 0; i < max; i++) {
                String item = mExtraItems.get(i);
                String quantity = (i < maxQuantity) ? mExtraQuantities.get(i)
                        : null;
                String price = (i < maxPrice) ? mExtraPrices.get(i) : null;
                String barcode = (i < maxBarcode) ? mExtraBarcodes.get(i)
                        : null;
                if (debug) {
                    Log.d(TAG, "Add item: " + item + ", quantity: " + quantity
                            + ", price: " + price + ", barcode: " + barcode);
                }
                itemsView.insertNewItem(activity, item, quantity, null, price,
                        barcode);
            }
            // delete the string array list of extra items so it can't be
            // inserted twice
            mExtraItems = null;
            mExtraQuantities = null;
            mExtraPrices = null;
            mExtraBarcodes = null;
            mExtraListUri = null;
        } else {
            Toast.makeText(activity, R.string.no_items_available,
                    Toast.LENGTH_SHORT).show();
        }
    }

    void getShoppingExtras(final Intent intent) {
        mExtraItems = intent.getExtras().getStringArrayList(
                ShoppingListIntents.EXTRA_STRING_ARRAYLIST_SHOPPING);
        mExtraQuantities = intent.getExtras().getStringArrayList(
                ShoppingListIntents.EXTRA_STRING_ARRAYLIST_QUANTITY);
        mExtraPrices = intent.getExtras().getStringArrayList(
                ShoppingListIntents.EXTRA_STRING_ARRAYLIST_PRICE);
        mExtraBarcodes = intent.getExtras().getStringArrayList(
                ShoppingListIntents.EXTRA_STRING_ARRAYLIST_BARCODE);

        mExtraListUri = null;
        if ((intent.getDataString() != null)
                && (intent.getDataString()
                .startsWith(ShoppingContract.Lists.CONTENT_URI
                        .toString()))) {
            // We received a valid shopping list URI.

            // Set current list to received list:
            mExtraListUri = intent.getData();
            if (debug) {
                Log.d(TAG, "Received extras for " + mExtraListUri.toString());
            }
        }
    }

    public boolean hasBeenInserted() {
        return mExtraItems == null;
    }

    public boolean hasItems() {
        return mExtraItems != null;
    }
}
/*
 * Copyright (C) 2012 Google Inc.
 * Licensed to The Android Open Source Project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/**
 * Classes that can undo an operation should implement this interface.
 */
public interface UndoListener {
    public void onUndoAvailable(SnackbarUndoOperation undoOp);
}


import android.app.Activity;
import android.app.Dialog;
import android.app.LoaderManager;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.SearchView;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.ViewBinder;
import android.widget.TextView;

import org.openintents.distribution.DownloadAppDialog;
import org.openintents.shopping.R;
import org.openintents.shopping.ShoppingApplication;
import org.openintents.shopping.SyncSupport;
import org.openintents.shopping.library.provider.ShoppingContract;
import org.openintents.shopping.library.provider.ShoppingContract.Contains;
import org.openintents.shopping.library.provider.ShoppingContract.ContainsFull;
import org.openintents.shopping.library.provider.ShoppingContract.Status;
import org.openintents.shopping.library.util.ShoppingUtils;
import org.openintents.shopping.provider.ShoppingProvider;
import org.openintents.shopping.theme.ThemeAttributes;
import org.openintents.shopping.theme.ThemeShoppingList;
import org.openintents.shopping.theme.ThemeUtils;
import org.openintents.shopping.ui.PreferenceActivity;
import org.openintents.shopping.ui.ShoppingActivity;
import org.openintents.shopping.ui.ShoppingTotalsHandler;
import org.openintents.shopping.ui.SnackbarUndoMultipleItemStatusOperation;
import org.openintents.shopping.ui.SnackbarUndoSingleItemStatusOperation;
import org.openintents.shopping.ui.UndoListener;
import org.openintents.shopping.ui.dialog.EditItemDialog;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * View to show a shopping list with its items
 */
public class ShoppingItemsView extends ListView implements LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * mode: separate dialog to add items from existing list
     */
    private static final int MODE_PICK_ITEMS_DLG = 3;
    /**
     * mode: add items from existing list
     */
    private static final int MODE_ADD_ITEMS = 2;
    /**
     * mode: I am in the shop
     */
    public static final int MODE_IN_SHOP = 1;

    private final static String TAG = "ShoppingListView";
    private final static boolean debug = false;
    public int mPriceVisibility;
    public int mTagsVisibility;
    public int mQuantityVisibility;
    public int mUnitsVisibility;
    public int mPriorityVisibility;
    public String mTextTypeface;
    public float mTextSize;
    public boolean mTextUpperCaseFont;
    public int mTextColor;
    public int mTextColorPrice;
    public int mTextColorChecked;
    public int mTextColorPriority;
    public boolean mShowCheckBox;
    public boolean mShowStrikethrough;
    public String mTextSuffixUnchecked;
    public String mTextSuffixChecked;
    public int mBackgroundPadding;
    public int mUpdateLastListPosition;
    public int mLastListPosition;
    public int mLastListTop;
    public long mNumChecked;
    public long mNumUnchecked;
    private int mMode = MODE_IN_SHOP;
    public int mModeBeforeSearch;
    public Cursor mCursorItems;
    private Typeface mCurrentTypeface;
    private ThemeAttributes mThemeAttributes;
    private PackageManager mPackageManager;
    private String mPackageName;
    private NumberFormat mPriceFormatter = DecimalFormat
            .getNumberInstance(Locale.ENGLISH);
    private String mFilter;
    private boolean mInSearch;
    private Activity mCursorActivity;

    private View mThemedBackground;
    private long mListId;

    private long mFocusItemId = -1;

    private Drawable mDefaultDivider;

    private int mDragPos; // which item is being dragged
    private int mFirstDragPos; // where was the dragged item originally
    private int mDragPoint; // at what offset inside the item did the user grab
    // it
    private int mCoordOffset; // the difference between screen coordinates and
    // coordinates in this view

    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mWindowParams;
    private Rect mTempRect = new Rect();

    // dragging elements
    private Bitmap mDragBitmap;
    private ImageView mDragView;
    private int mHeight;
    private int mUpperBound;
    private int mLowerBound;
    private int mTouchSlop;
    private int mItemHeightHalf;
    private int mItemHeightNormal;
    private int mItemHeightExpanded;

    private DragListener mDragListener;
    private DropListener mDropListener;

    private ActionBarListener mActionBarListener;
    private UndoListener mUndoListener;
    private Snackbar mSnackbar;
    private SyncSupport mSyncSupport;
    private ShoppingTotalsHandler mTotalsHandler;
    private SearchView mSearchView;
    private OnCustomClickListener mListener;
    private OnModeChangeListener mModeChangeListener;
    private boolean mDragAndDropEnabled;

    public ShoppingItemsView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public ShoppingItemsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ShoppingItemsView(Context context) {
        super(context);
        init();
    }

    public View getSearchView() {
        Context context = getContext();
        if (PreferenceActivity.getUsingHoloSearchFromPrefs(context)) {
            mSearchView = new SearchView(mCursorActivity);
            mSearchView.setSubmitButtonEnabled(true);
            mSearchView.setInputType(PreferenceActivity.getSearchInputTypeFromPrefs(context));
            mSearchView.setOnQueryTextListener(new SearchQueryListener());
            mSearchView.setOnCloseListener(new SearchDismissedListener());
            mSearchView.setImeOptions(EditorInfo.IME_ACTION_UNSPECIFIED);
        }
        return mSearchView;
    }

    private void disposeItemsCursor() {
        if (mCursorActivity != null) {
            mCursorActivity.stopManagingCursor(mCursorItems);
            mCursorActivity = null;
        }
        mCursorItems.deactivate();
        if (!mCursorItems.isClosed()) {
            mCursorItems.close();
        }
        mCursorItems = null;
    }

    private void init() {
        mItemHeightNormal = 45;
        mItemHeightHalf = mItemHeightNormal / 2;
        mItemHeightExpanded = 90;

        // Remember standard divider
        mDefaultDivider = getDivider();
        mSyncSupport = ((ShoppingApplication) getContext().getApplicationContext()).dependencies().getSyncSupport(getContext());
    }

    public void initTotals() {
        // Can't be called during init because that happens while
        // still inflating the parent activity, so findViewById
        // doesn't work yet.
        mTotalsHandler = new ShoppingTotalsHandler(this);
    }

    public void setActionBarListener(ActionBarListener listener) {
        mActionBarListener = listener;
    }

    public void setUndoListener(UndoListener listener) {
        mUndoListener = listener;
    }

    public void onResume() {
        setFastScrollEnabled(PreferenceActivity.getFastScrollEnabledFromPrefs(getContext()));
    }

    public void onPause() {

    }

    public long getListId() {
        return mListId;
    }

    public boolean getInSearch() {
        return mInSearch;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader loader = new CursorLoader(mCursorActivity);
        createItemsCursor(mListId, loader);
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor items) {

        // Get a cursor for all items that are contained
        // in currently selected shopping list.
        mCursorItems = items;

        // Activate the following for a striped list.
        // setupListStripes(mListItems, this);

        if (mCursorItems == null) {
            Log.e(TAG, "missing shopping provider");
            setAdapter(new ArrayAdapter<>(this.getContext(),
                    android.R.layout.simple_list_item_1,
                    new String[]{"no shopping provider"}));
            return;
        }

        int layout_row = R.layout.list_item_shopping_item;
        int size = PreferenceActivity.getFontSizeFromPrefs(getContext());
        if (size < 3) {
            layout_row = R.layout.list_item_shopping_item_small;
        }

        Context context = getContext();

        // If background is light, we apply the light holo theme to widgets.

        // determine color from text color:
        int gray = (Color.red(mTextColor) + Color.green(mTextColor) + Color.blue(mTextColor));
        if (gray < 3 * 128) {
            // dark text color <-> light background color => use light holo theme.
            context = new ContextThemeWrapper(context, android.R.style.Theme_Holo_Light);
        }

        mSimpleCursorAdapter adapter = (mSimpleCursorAdapter) getAdapter();

        if (adapter != null) {
            adapter.swapCursor(mCursorItems);
        } else {
            adapter = new mSimpleCursorAdapter(
                    context,
                    // Use a template that displays a text view
                    layout_row,
                    // Give the cursor to the list adapter
                    mCursorItems,
                    // Map the IMAGE and NAME to...
                    new String[]{ContainsFull.ITEM_NAME, /*
                                                         * ContainsFull.ITEM_IMAGE
														 * ,
														 */
                            ContainsFull.ITEM_TAGS, ContainsFull.ITEM_PRICE,
                            ContainsFull.QUANTITY, ContainsFull.PRIORITY,
                            ContainsFull.ITEM_UNITS
                    },
                    // the view defined in the XML template
                    new int[]{R.id.name, /* R.id.image_URI, */R.id.tags,
                            R.id.price, R.id.quantity, R.id.priority, R.id.units}
            );
            setAdapter(adapter);
        }

        if (mFocusItemId != -1) {
            // Set the item that we have just selected:
            // Get position of ID:
            mCursorItems.moveToPosition(-1);
            while (mCursorItems.moveToNext()) {
                if (mCursorItems.getLong(ShoppingActivity.mStringItemsITEMID) == mFocusItemId) {
                    int pos = mCursorItems.getPosition();
                    // scroll item near top, but not all the way to top, to provide context.
                    pos = Math.max(pos - 3, 0);
                    postDelayedSetSelection(pos);
                    break;
                }
            }
            if (!mInSearch) {
                mFocusItemId = -1;
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCursorItems = null;
    }

    /**
     * @param activity Activity to manage the cursor.
     * @param listId
     * @return
     */
    public void fillItems(Activity activity, long listId) {

        mCursorItems = null;
        mCursorActivity = activity;

        mListId = listId;

        setSearchModePref();
        activity.getLoaderManager().restartLoader(ShoppingActivity.LOADER_ITEMS, null, this);

        updateTotal();

    }

    private void setSearchModePref() {
        // this is not a real user preference, just used to communicate
        // current app state to the content provider.
        SharedPreferences sp = mCursorActivity.getSharedPreferences(
                "org.openintents.shopping_preferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean("_searching", mInSearch);
        editor.apply();
    }

    private Cursor createItemsCursor(long listId, CursorLoader loader) {
        String sortOrder = PreferenceActivity.getSortOrderFromPrefs(this
                .getContext(), mMode, listId);
        boolean hideBought = PreferenceActivity
                .getHideCheckedItemsFromPrefs(this.getContext());
        String selection;
        String[] selection_args = new String[]{String.valueOf(listId)};
        if (mFilter != null) {
            selection = "list_id = ? AND " + ContainsFull.ITEM_NAME +
                    " like '%" + ShoppingProvider.escapeSQLChars(mFilter) + "%' ESCAPE '`'";
        } else if (inShopMode()) {
            if (hideBought) {
                selection = "list_id = ? AND " + Contains.STATUS
                        + " == " + Status.WANT_TO_BUY;
            } else {
                selection = "list_id = ? AND " + Contains.STATUS
                        + " <> " + Status.REMOVED_FROM_LIST;
            }
        } else {
            selection = "list_id = ? ";
        }

        if (loader != null) {
            loader.setUri(ContainsFull.CONTENT_URI);
            loader.setProjection(ShoppingActivity.PROJECTION_ITEMS);
            loader.setSelection(selection);
            loader.setSelectionArgs(selection_args);
            loader.setSortOrder(sortOrder);
            return null;
        }

        return getContext().getContentResolver().query(
                ContainsFull.CONTENT_URI, ShoppingActivity.PROJECTION_ITEMS,
                selection, selection_args, sortOrder);
    }

    /**
     * Set theme according to Id.
     *
     * @param themeName
     */
    public void setListTheme(String themeName) {
        int size = PreferenceActivity.getFontSizeFromPrefs(getContext());

        // backward compatibility:
        if (themeName == null) {
            setLocalStyle(R.style.Theme_ShoppingList, size);
        } else if (themeName.equals("1")) {
            setLocalStyle(R.style.Theme_ShoppingList, size);
        } else if (themeName.equals("2")) {
            setLocalStyle(R.style.Theme_ShoppingList_Classic, size);
        } else if (themeName.equals("3")) {
            setLocalStyle(R.style.Theme_ShoppingList_Android, size);
        } else {
            // New styles:
            boolean themeFound = setRemoteStyle(themeName, size);

            if (!themeFound) {
                // Some error occured, let's use default style:
                setLocalStyle(R.style.Theme_ShoppingList, size);
            }
        }

        invalidate();
        if (mCursorItems != null) {
            requery();
        }
    }

    private void setLocalStyle(int styleResId, int size) {
        String styleName = getResources().getResourceName(styleResId);

        boolean themefound = setRemoteStyle(styleName, size);

        if (!themefound) {
            // Actually this should never happen.
            Log.e(TAG, "Local theme not found: " + styleName);
        }
    }

    private boolean setRemoteStyle(String styleName, int size) {
        if (TextUtils.isEmpty(styleName)) {
            if (debug) {
                Log.e(TAG, "Empty style name: " + styleName);
            }
            return false;
        }

        mPackageManager = getContext().getPackageManager();

        mPackageName = ThemeUtils.getPackageNameFromStyle(styleName);

        if (mPackageName == null) {
            Log.e(TAG, "Invalid style name: " + styleName);
            return false;
        }

        Context c = null;
        try {
            c = getContext().createPackageContext(mPackageName, 0);
        } catch (NameNotFoundException e) {
            Log.e(TAG, "Package for style not found: " + mPackageName + ", "
                    + styleName);
            return false;
        }

        Resources res = c.getResources();

        int themeid = res.getIdentifier(styleName, null, null);

        if (themeid == 0) {
            Log.e(TAG, "Theme name not found: " + styleName);
            return false;
        }

        try {
            mThemeAttributes = new ThemeAttributes(c, mPackageName, themeid);

            mTextTypeface = mThemeAttributes.getString(ThemeShoppingList.textTypeface);
            mCurrentTypeface = createTypeface(mTextTypeface);

            mTextUpperCaseFont = mThemeAttributes.getBoolean(
                    ThemeShoppingList.textUpperCaseFont, false);

            mTextColor = mThemeAttributes.getColor(ThemeShoppingList.textColor,
                    android.R.color.white);

            mTextColorPrice = mThemeAttributes.getColor(ThemeShoppingList.textColorPrice,
                    android.R.color.white);

            // Use color of price if color of priority has not been defined
            mTextColorPriority = mThemeAttributes.getColor(ThemeShoppingList.textColorPriority,
                    mTextColorPrice);

            if (size == 0) {
                mTextSize = getTextSizeTiny(mThemeAttributes);
            } else if (size == 1) {
                mTextSize = getTextSizeSmall(mThemeAttributes);
            } else if (size == 2) {
                mTextSize = getTextSizeMedium(mThemeAttributes);
            } else {
                mTextSize = getTextSizeLarge(mThemeAttributes);
            }
            if (debug) {
                Log.d(TAG, "textSize: " + mTextSize);
            }

            mTextColorChecked = mThemeAttributes.getColor(ThemeShoppingList.textColorChecked,
                    android.R.color.white);
            mShowCheckBox = mThemeAttributes.getBoolean(ThemeShoppingList.showCheckBox, true);
            mShowStrikethrough = mThemeAttributes.getBoolean(
                    ThemeShoppingList.textStrikethroughChecked, false);
            mTextSuffixUnchecked = mThemeAttributes
                    .getString(ThemeShoppingList.textSuffixUnchecked);
            mTextSuffixChecked = mThemeAttributes
                    .getString(ThemeShoppingList.textSuffixChecked);

            // field was named divider, until a conflict with the appcompat library
            // forced us to rename it. To continue to support old themes, check for
            // shopping_divider first, but if it's not found, check for divider also.
            int divider = mThemeAttributes.getInteger(ThemeShoppingList.shopping_divider, 0);
            if (divider == 0) {
                divider = mThemeAttributes.getInteger(ThemeShoppingList.divider, 0);
            }

            Drawable div;
            if (divider > 0) {
                div = getResources().getDrawable(divider);
            } else if (divider < 0) {
                div = null;
            } else {
                div = mDefaultDivider;
            }

            setDivider(div);

            return true;

        } catch (UnsupportedOperationException e) {
            // This exception is thrown e.g. if one attempts
            // to read an integer attribute as dimension.
            Log.e(TAG, "UnsupportedOperationException", e);
            return false;
        } catch (NumberFormatException e) {
            // This exception is thrown e.g. if one attempts
            // to read a string as integer.
            Log.e(TAG, "NumberFormatException", e);
            return false;
        }
    }

    private Typeface createTypeface(String typeface) {
        Typeface newTypeface = null;
        try {
            // Look for special cases:
            if ("monospace".equals(typeface)) {
                newTypeface = Typeface.create(Typeface.MONOSPACE,
                        Typeface.NORMAL);
            } else if ("sans".equals(typeface)) {
                newTypeface = Typeface.create(Typeface.SANS_SERIF,
                        Typeface.NORMAL);
            } else if ("serif".equals(typeface)) {
                newTypeface = Typeface.create(Typeface.SERIF,
                        Typeface.NORMAL);
            } else if (!TextUtils.isEmpty(typeface)) {

                try {
                    if (debug) {
                        Log.d(TAG, "Reading typeface: package: " + mPackageName
                                + ", typeface: " + typeface);
                    }
                    Resources remoteRes = mPackageManager
                            .getResourcesForApplication(mPackageName);
                    newTypeface = Typeface.createFromAsset(remoteRes
                            .getAssets(), typeface);
                    if (debug) {
                        Log.d(TAG, "Result: " + newTypeface);
                    }
                } catch (NameNotFoundException e) {
                    Log.e(TAG, "Package not found for Typeface", e);
                }
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "type face can't be made " + typeface);
        }
        return newTypeface;
    }

    /**
     * Must be called after setListTheme();
     */
    public void applyListTheme() {

        if (mThemedBackground != null) {
            mBackgroundPadding = mThemeAttributes.getDimensionPixelOffset(
                    ThemeShoppingList.backgroundPadding, -1);
            int backgroundPaddingLeft = mThemeAttributes.getDimensionPixelOffset(
                    ThemeShoppingList.backgroundPaddingLeft,
                    mBackgroundPadding);
            int backgroundPaddingTop = mThemeAttributes.getDimensionPixelOffset(
                    ThemeShoppingList.backgroundPaddingTop,
                    mBackgroundPadding);
            int backgroundPaddingRight = mThemeAttributes.getDimensionPixelOffset(
                    ThemeShoppingList.backgroundPaddingRight,
                    mBackgroundPadding);
            int backgroundPaddingBottom = mThemeAttributes.getDimensionPixelOffset(
                    ThemeShoppingList.backgroundPaddingBottom,
                    mBackgroundPadding);
            try {
                Resources remoteRes = mPackageManager
                        .getResourcesForApplication(mPackageName);
                int resid = mThemeAttributes.getResourceId(ThemeShoppingList.background,
                        0);
                if (resid != 0) {
                    Drawable d = remoteRes.getDrawable(resid);
                    mThemedBackground.setBackgroundDrawable(d);
                } else {
                    // remove background
                    mThemedBackground.setBackgroundResource(0);
                }
            } catch (NameNotFoundException e) {
                Log.e(TAG, "Package not found for Theme background.", e);
            } catch (Resources.NotFoundException e) {
                Log.e(TAG, "Resource not found for Theme background.", e);
            }

            // Apply padding
            if (mBackgroundPadding >= 0 || backgroundPaddingLeft >= 0
                    || backgroundPaddingTop >= 0
                    || backgroundPaddingRight >= 0
                    || backgroundPaddingBottom >= 0) {
                mThemedBackground.setPadding(backgroundPaddingLeft,
                        backgroundPaddingTop, backgroundPaddingRight,
                        backgroundPaddingBottom);
            } else {
                // 9-patches do the padding automatically
                // todo clear padding
            }
        }

    }

    private float getTextSizeTiny(ThemeAttributes ta) {
        float size = ta.getDimensionPixelOffset(ThemeShoppingList.textSizeTiny,
                -1);
        if (size == -1) {
            // Try to obtain from small:
            size = (12f / 18f) * getTextSizeSmall(ta);
        }
        return size;
    }

    private float getTextSizeSmall(ThemeAttributes ta) {
        float size = ta.getDimensionPixelOffset(
                ThemeShoppingList.textSizeSmall, -1);
        if (size == -1) {
            // Try to obtain from small:
            size = (18f / 23f) * getTextSizeMedium(ta);
        }
        return size;
    }

    private float getTextSizeMedium(ThemeAttributes ta) {
        final float scale = getResources().getDisplayMetrics().scaledDensity;
        return ta.getDimensionPixelOffset(
                ThemeShoppingList.textSizeMedium, (int) (23 * scale + 0.5f));
    }

    private float getTextSizeLarge(ThemeAttributes ta) {
        float size = ta.getDimensionPixelOffset(
                ThemeShoppingList.textSizeLarge, -1);
        if (size == -1) {
            // Try to obtain from small:
            size = (28f / 23f) * getTextSizeMedium(ta);
        }
        return size;
    }

    public void setThemedBackground(View background) {
        mThemedBackground = background;

    }

    /**
     * set the status of all items according to the parameter
     *
     * @param on if true all want_to_buy items are set to bought, if false all bought items are set to want_to_buy
     */
    public void toggleAllItems(boolean on) {
        int op_type = on ? SnackbarUndoMultipleItemStatusOperation.MARK_ALL : SnackbarUndoMultipleItemStatusOperation.UNMARK_ALL;
        SnackbarUndoMultipleItemStatusOperation op = null;

        if (mUndoListener != null) {
            op = new SnackbarUndoMultipleItemStatusOperation(this, mCursorActivity,
                    op_type, mListId, false);
        }

        for (int i = 0; i < mCursorItems.getCount(); i++) {
            mCursorItems.moveToPosition(i);

            long oldstatus = mCursorItems
                    .getLong(ShoppingActivity.mStringItemsSTATUS);

            // Toggle status ON:
            // bought -> bought
            // want_to_buy -> bought
            // removed_from_list -> removed_from_list

            // Toggle status OFF:
            // bought -> want_to_buy
            // want_to_buy -> want_to_buy
            // removed_from_list -> removed_from_list

            long newstatus;
            boolean doUpdate;
            if (on) {
                newstatus = ShoppingContract.Status.BOUGHT;
                doUpdate = (oldstatus == ShoppingContract.Status.WANT_TO_BUY);
            } else {
                newstatus = ShoppingContract.Status.WANT_TO_BUY;
                doUpdate = (oldstatus == ShoppingContract.Status.BOUGHT);
            }

            if (doUpdate) {
                final ContentValues values = new ContentValues();
                values.put(ShoppingContract.Contains.STATUS, newstatus);
                if (debug) {
                    Log.d(TAG, "update row " + mCursorItems.getString(0) + ", newstatus "
                            + newstatus);
                }
                final Uri itemUri = Uri.withAppendedPath(Contains.CONTENT_URI,
                        mCursorItems.getString(0));
                getContext().getContentResolver().update(
                        itemUri, values, null, null
                );
                pushUpdatedItemToWear(values, itemUri);

            }
        }

        requery();

        invalidate();
        if (mUndoListener != null) {
            mUndoListener.onUndoAvailable(op);
        }
    }

    public void toggleItemBought(int position) {
        boolean shouldFocusItem = false;

        if (mCursorItems.getCount() <= position) {
            Log.e(TAG, "toggle inexistent item. Probably clicked too quickly?");
            return;
        }

        mCursorItems.moveToPosition(position);

        long oldstatus = mCursorItems
                .getLong(ShoppingActivity.mStringItemsSTATUS);

        // Toggle status depending on mode:
        long newstatus = ShoppingContract.Status.WANT_TO_BUY;

        if (inShopMode()) {
            if (oldstatus == ShoppingContract.Status.WANT_TO_BUY) {
                newstatus = ShoppingContract.Status.BOUGHT;
            } // else old was BOUGHT, new should be WANT_TO_BUY, which is the default.
        } else { // MODE_ADD_ITEMS or MODE_PICK_ITEMS_DLG
            // when we are in integrated add items mode, all three states
            // might be displayed, but the user can only create two of them.
            // want_to_buy-> removed_from_list
            // bought -> want_to_buy
            // removed_from_list -> want_to_buy
            if (oldstatus == ShoppingContract.Status.WANT_TO_BUY) {
                newstatus = ShoppingContract.Status.REMOVED_FROM_LIST;
                shouldFocusItem = mInSearch && mFilter != null && mFilter.length() > 0;
            } else { // old is REMOVE_FROM_LIST or BOUGHT, new is WANT_TO_BUY, which is the default.
                if (mInSearch) {
                    shouldFocusItem = true;
                }
            }
        }

        String contains_id = mCursorItems.getString(0);
        final ContentValues values = new ContentValues();
        values.put(ShoppingContract.Contains.STATUS, newstatus);
        if (debug) {
            Log.d(TAG, "update row " + mCursorItems.getString(0) + ", newstatus "
                    + newstatus);
        }

        if (mInSearch && newstatus == ShoppingContract.Status.WANT_TO_BUY) {
            long item_id = mCursorItems.getLong(ShoppingActivity.mStringItemsITEMID);
            ShoppingUtils.addDefaultsToAddedItem(getContext(), mListId, item_id);
        }

        if (shouldFocusItem) {
            mFocusItemId = mCursorItems.getLong(ShoppingActivity.mStringItemsITEMID);
        }

        final Uri itemUri = Uri.withAppendedPath(Contains.CONTENT_URI, contains_id);
        getContext().getContentResolver().update(
                itemUri, values, null, null
        );

        pushUpdatedItemToWear(values, itemUri);

        boolean affectsSort = PreferenceActivity.prefsStatusAffectsSort(getContext(), mMode);
        boolean hidesItem = true /* TODO */;
        if (mUndoListener != null && (affectsSort || hidesItem)) {
            String item_name = mCursorItems.getString(ShoppingActivity.mStringItemsITEMNAME);
            SnackbarUndoSingleItemStatusOperation op = new SnackbarUndoSingleItemStatusOperation(this, getContext(),
                    contains_id, item_name, oldstatus, newstatus, 0, false);
            mUndoListener.onUndoAvailable(op);
        }

        requery();

        if (affectsSort) {
            invalidate();
        }
    }

    public boolean cleanupList() {

        boolean nothingdeleted;

        SnackbarUndoMultipleItemStatusOperation op = null;
        if (mUndoListener != null) {
            op = new SnackbarUndoMultipleItemStatusOperation(this, mCursorActivity,
                    SnackbarUndoMultipleItemStatusOperation.CLEAN_LIST, mListId, false);
        }

        // by changing state
        ContentValues values = new ContentValues();
        values.put(Contains.STATUS, Status.REMOVED_FROM_LIST);
        if (PreferenceActivity.getResetQuantity(getContext())) {
            values.put(Contains.QUANTITY, "");
        }
        nothingdeleted = getContext().getContentResolver().update(
                Contains.CONTENT_URI,
                values,
                ShoppingContract.Contains.LIST_ID + " = " + mListId + " AND "
                        + ShoppingContract.Contains.STATUS + " = "
                        + ShoppingContract.Status.BOUGHT, null
        ) == 0;

        requery();

        if (mUndoListener != null) {
            mUndoListener.onUndoAvailable(op);
        }

        return !nothingdeleted;

    }

    /**
     * @param activity Activity to manage new Cursor.
     * @param newItem
     * @param quantity
     * @param price
     * @param barcode
     */
    public void insertNewItem(Activity activity, String newItem,
                              String quantity, String priority, String price, String barcode) {

        String list_id = null;
        if (PreferenceActivity.getCompleteFromCurrentListOnlyFromPrefs(getContext())) {
            list_id = String.valueOf(mListId);
        }

        newItem = newItem.trim();

        long itemId = ShoppingUtils.updateOrCreateItem(getContext(), newItem,
                null, price, barcode, list_id);

        if (debug) {
            Log.i(TAG, "Insert new item. " + " itemId = " + itemId + ", listId = "
                    + mListId);
        }
        boolean resetQuantity = PreferenceActivity.getResetQuantity(getContext());
        ShoppingUtils.addItemToList(getContext(), itemId, mListId, Status.WANT_TO_BUY,
                priority, quantity, false, false, resetQuantity);
        ShoppingUtils.addDefaultsToAddedItem(getContext(), mListId, itemId);
        mFocusItemId = itemId;
        fillItems(activity, mListId);
    }

    public boolean isWearSupportAvailable() {
        return mSyncSupport != null && mSyncSupport.isAvailable();
    }

    public void pushItemsToWear() {
        if (mSyncSupport.isAvailable()) {
            new Thread() {
                @Override
                public void run() {
                    Cursor cursor = createItemsCursor(mListId, null);
                    Log.d(TAG, "pushing " + cursor.getCount() + " items");
                    cursor.moveToFirst();
                    while (cursor.moveToNext()) {
                        mSyncSupport.pushListItem(mListId, cursor);
                    }
                }
            }.start();
        }
    }

    private void pushUpdatedItemToWear(final ContentValues values, final Uri itemUri) {
        if (mSyncSupport.isAvailable() && mSyncSupport.isSyncEnabled())
            new Thread() {
                @Override
                public void run() {
                    mSyncSupport.updateListItem(mListId, itemUri, values);
                }
            }.start();
    }

    /**
     * Post setSelection delayed, because onItemSelected() may be called more
     * than once, leading to fillItems() being called more than once as well.
     * Posting delayed ensures that items added through intents that return
     * results (like a barcode scanner) are put into visible position.
     *
     * @param pos
     */
    void postDelayedSetSelection(final int pos) {
        // set immediately
        setSelection(pos);

        // if for any reason this does not work, a delayed version
        // will succeed:
        postDelayed(new Runnable() {

            @Override
            public void run() {
                setSelection(pos);
            }

        }, 1000);
    }

    public void requery() {
        if (debug) {
            Log.d(TAG, "requery()");
        }

        // Test for null pointer exception (issue 313)
        if (mCursorItems != null) {
            mCursorItems.requery();
            updateTotal();

            if (mUpdateLastListPosition > 0) {
                if (debug) {
                    Log.d(TAG, "Restore list position: pos: " + mLastListPosition
                            + ", top: " + mLastListTop + ", tries: " + mUpdateLastListPosition);
                }
                setSelectionFromTop(mLastListPosition, mLastListTop);
                mUpdateLastListPosition--;
            }
        }
    }

    /**
     * Update the text fields for "Total:" and "Checked:" with corresponding
     * price information.
     */
    public void updateTotal() {
        if (debug) {
            Log.d(TAG, "updateTotal()");
        }
        mTotalsHandler.update(mCursorActivity.getLoaderManager(), mListId);
    }

    public void updateNumChecked(long numChecked, long numUnchecked) {

        mNumChecked = numChecked;
        mNumUnchecked = numUnchecked;

        // Update ActionBar in ShoppingActivity
        // for the "Clean up list" command
        if (mActionBarListener != null && !mInSearch) {
            mActionBarListener.updateActionBar();
        }
    }

    private long getQuantityPrice(Cursor cursor) {
        long price = cursor.getLong(ShoppingActivity.mStringItemsITEMPRICE);
        if (price != 0) {
            String quantityString = cursor
                    .getString(ShoppingActivity.mStringItemsQUANTITY);
            if (!TextUtils.isEmpty(quantityString)) {
                try {
                    double quantity = Double.parseDouble(quantityString);
                    price = (long) (price * quantity);
                } catch (NumberFormatException e) {
                    // do nothing
                }
            }
        }
        return price;
    }

    public void setCustomClickListener(OnCustomClickListener listener) {
        mListener = listener;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

        if (mDragAndDropEnabled) {
            if (mDragListener != null || mDropListener != null) {
                switch (ev.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        int x = (int) ev.getX();
                        int y = (int) ev.getY();
                        int itemnum = pointToPosition(x, y);
                        if (itemnum == AdapterView.INVALID_POSITION) {
                            break;
                        }
                        ViewGroup item = (ViewGroup) getChildAt(itemnum
                                - getFirstVisiblePosition());
                        mDragPoint = y - item.getTop();
                        mCoordOffset = ((int) ev.getRawY()) - y;
                        item.setDrawingCacheEnabled(true);
                        Bitmap bitmap = Bitmap.createBitmap(item.getDrawingCache());
                        startDragging(bitmap, y);
                        mDragPos = itemnum;
                        mFirstDragPos = mDragPos;
                        mHeight = getHeight();
                        int touchSlop = mTouchSlop;
                        mUpperBound = Math.min(y - touchSlop, mHeight / 3);
                        mLowerBound = Math.max(y + touchSlop, mHeight * 2 / 3);
                        return false;
                    default:
                        break;
                }
            }
        }
        return super.onInterceptTouchEvent(ev);
    }

    private int myPointToPosition(int x, int y) {
        if (y < 0) {
            int pos = myPointToPosition(x, y + mItemHeightNormal);
            if (pos > 0) {
                return pos - 1;
            }
        }
        Rect frame = mTempRect;
        final int count = getChildCount();
        for (int i = count - 1; i >= 0; i--) {
            final View child = getChildAt(i);
            child.getHitRect(frame);
            if (frame.contains(x, y)) {
                return getFirstVisiblePosition() + i;
            }
        }
        return INVALID_POSITION;
    }

    private int getItemForPosition(int y) {
        int adjustedy = y - mDragPoint - mItemHeightHalf;
        int pos = myPointToPosition(0, adjustedy);
        if (pos >= 0) {
            if (pos <= mFirstDragPos) {
                pos += 1;
            }
        } else if (adjustedy < 0) {
            pos = 0;
        }
        return pos;
    }

    private void adjustScrollBounds(int y) {
        if (y >= mHeight / 3) {
            mUpperBound = mHeight / 3;
        }
        if (y <= mHeight * 2 / 3) {
            mLowerBound = mHeight * 2 / 3;
        }
    }

    private void unExpandViews(boolean deletion) {
        for (int i = 0; ; i++) {
            View v = getChildAt(i);
            if (v == null) {
                if (deletion) {
                    int position = getFirstVisiblePosition();
                    int y = getChildAt(0).getTop();
                    setAdapter(getAdapter());
                    setSelectionFromTop(position, y);
                }
                layoutChildren();
                v = getChildAt(i);
                if (v == null) {
                    break;
                }
            }
            ViewGroup.LayoutParams params = v.getLayoutParams();
            params.height = mItemHeightNormal;
            v.setLayoutParams(params);
            v.setVisibility(View.VISIBLE);
        }
    }

    private void doExpansion() {
        int childnum = mDragPos - getFirstVisiblePosition();
        if (mDragPos > mFirstDragPos) {
            childnum++;
        }

        View first = getChildAt(mFirstDragPos - getFirstVisiblePosition());

        for (int i = 0; ; i++) {
            View vv = getChildAt(i);
            if (vv == null) {
                break;
            }
            int height = mItemHeightNormal;
            int visibility = View.VISIBLE;
            if (vv.equals(first)) {
                if (mDragPos == mFirstDragPos) {
                    visibility = View.INVISIBLE;
                } else {
                    height = 1;
                }
            } else if (i == childnum) {
                if (mDragPos < getCount() - 1) {
                    height = mItemHeightExpanded;
                }
            }
            ViewGroup.LayoutParams params = vv.getLayoutParams();
            params.height = height;
            vv.setLayoutParams(params);
            vv.setVisibility(visibility);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {

        if ((mDragListener != null || mDropListener != null)
                && mDragView != null) {
            int action = ev.getAction();
            switch (action) {
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    Rect r = mTempRect;
                    mDragView.getDrawingRect(r);
                    stopDragging();
                    if (mDropListener != null && mDragPos >= 0
                            && mDragPos < getCount()) {
                        mDropListener.drop(mFirstDragPos, mDragPos);
                    }
                    unExpandViews(false);
                    break;
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    int x = (int) ev.getX();
                    int y = (int) ev.getY();
                    dragView(x, y);
                    int itemnum = getItemForPosition(y);
                    if (itemnum >= 0) {
                        if (action == MotionEvent.ACTION_DOWN
                                || itemnum != mDragPos) {
                            if (mDragListener != null) {
                                mDragListener.drag(mDragPos, itemnum);
                            }
                            mDragPos = itemnum;
                            doExpansion();
                        }
                        int speed = 0;
                        adjustScrollBounds(y);
                        if (y > mLowerBound) {
                            // scroll the list up a bit
                            speed = y > (mHeight + mLowerBound) / 2 ? 16 : 4;
                        } else if (y < mUpperBound) {
                            // scroll the list down a bit
                            speed = y < mUpperBound / 2 ? -16 : -4;
                        }
                        if (speed != 0) {
                            int ref = pointToPosition(0, mHeight / 2);
                            if (ref == AdapterView.INVALID_POSITION) {
                                // we hit a divider or an invisible view, check
                                // somewhere else
                                ref = pointToPosition(0, mHeight / 2
                                        + getDividerHeight() + 64);
                            }
                            View v = getChildAt(ref - getFirstVisiblePosition());
                            if (v != null) {
                                int pos = v.getTop();
                                setSelectionFromTop(ref, pos - speed);
                            }
                        }
                    }
                    break;
                default:
                    break;
            }
            return true;
        }
        return super.onTouchEvent(ev);
    }

    private void startDragging(Bitmap bm, int y) {
        stopDragging();

        mWindowParams = new WindowManager.LayoutParams();
        mWindowParams.gravity = Gravity.TOP;
        mWindowParams.x = 0;
        mWindowParams.y = y - mDragPoint + mCoordOffset;

        mWindowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        mWindowParams.format = PixelFormat.TRANSLUCENT;
        mWindowParams.windowAnimations = 0;

        Context context = getContext();
        ImageView v = new ImageView(context);
        int backGroundColor = context.getResources()
                .getColor(R.color.darkgreen);
        v.setBackgroundColor(backGroundColor);
        v.setImageBitmap(bm);
        mDragBitmap = bm;

        mWindowManager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        mWindowManager.addView(v, mWindowParams);
        mDragView = v;
    }

    private void dragView(int x, int y) {
        mWindowParams.y = y - mDragPoint + mCoordOffset;
        mWindowManager.updateViewLayout(mDragView, mWindowParams);
    }

    private void stopDragging() {
        if (mDragView != null) {
            WindowManager wm = (WindowManager) getContext().getSystemService(
                    Context.WINDOW_SERVICE);
            wm.removeView(mDragView);
            mDragView.setImageDrawable(null);
            mDragView = null;
        }
        if (mDragBitmap != null) {
            mDragBitmap.recycle();
            mDragBitmap = null;
        }
    }

    public void setDragListener(DragListener l) {
        mDragListener = l;
    }

    public void setDropListener(DropListener l) {
        mDropListener = l;
    }

    public void setOnModeChangeListener(OnModeChangeListener listener) {
        mModeChangeListener = listener;
    }

    public void setModes(int mode, int modeBeforeSearch) {
        mMode = mode;
        mModeBeforeSearch = modeBeforeSearch;
    }

    public void setPickItemsDlgMode() {
        mMode = MODE_PICK_ITEMS_DLG;
    }

    public void setAddItemsMode() {
        mMode = MODE_ADD_ITEMS;
    }

    public void setInShopMode() {
        mMode = MODE_IN_SHOP;
    }

    public int getMode() {
        return mMode;
    }

    public interface OnCustomClickListener {
        void onCustomClick(Cursor c, int pos, EditItemDialog.FieldType field, View v);
    }

    public interface DragListener {
        void drag(int from, int to);
    }

    public interface DropListener {
        void drop(int from, int to);
    }

    public interface RemoveListener {
        void remove(int which);
    }

    public interface ActionBarListener {
        void updateActionBar();
    }

    public interface OnModeChangeListener {
        void onModeChanged();
    }

    /**
     * Extend the SimpleCursorAdapter to strike through items. if STATUS ==
     * Shopping.Status.BOUGHT
     */
    public class mSimpleCursorAdapter extends SimpleCursorAdapter implements
            ViewBinder {

        /**
         * Constructor simply calls super class.
         *
         * @param context Context.
         * @param layout  Layout.
         * @param c       Cursor.
         * @param from    Projection from.
         * @param to      Projection to.
         */
        mSimpleCursorAdapter(final Context context, final int layout,
                             final Cursor c, final String[] from, final int[] to) {
            super(context, layout, c, from, to);
            super.setViewBinder(this);

            mPriceFormatter.setMaximumFractionDigits(2);
            mPriceFormatter.setMinimumFractionDigits(2);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = super.newView(context, cursor, parent);
            mItemRowState rowState = new mItemRowState(view); // sets view tags
            return view;
        }

        /**
         * Additionally to the standard bindView, we also check for STATUS, and
         * strike the item through if BOUGHT.
         */
        @Override
        public void bindView(final View view, final Context context,
                             final Cursor cursor) {
            super.bindView(view, context, cursor);

            long status = cursor.getLong(ShoppingActivity.mStringItemsSTATUS);
            mItemRowState state = (mItemRowState) view.getTag();
            state.mCursorPos = cursor.getPosition();
            state.mCursor = cursor;


            // set style for name view and friends
            TextView[] styled_as_name = {state.mNameView, state.mUnitsView, state.mQuantityView};
            int i;
            for (i = 0; i < styled_as_name.length; i++) {
                TextView t = styled_as_name[i];

                // Set font
                if (mCurrentTypeface != null) {
                    t.setTypeface(mCurrentTypeface);
                }

                // Set size
                t.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTextSize);

                // Check for upper case:
                if (mTextUpperCaseFont) {
                    // Only upper case should be displayed
                    CharSequence cs = t.getText();
                    t.setText(cs.toString().toUpperCase());
                }

                t.setTextColor(mTextColor);

                if (status == ShoppingContract.Status.BOUGHT) {
                    t.setTextColor(mTextColorChecked);

                    if (mShowStrikethrough) {
                        // We have bought the item,
                        // so we strike it through:

                        // First convert text to 'spannable'
                        t.setText(t.getText(), TextView.BufferType.SPANNABLE);
                        Spannable str = (Spannable) t.getText();

                        // Strikethrough
                        str.setSpan(new StrikethroughSpan(), 0, str.length(),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                        // apply color
                        // TODO: How to get color from resource?
                        // Drawable colorStrikethrough = context
                        // .getResources().getDrawable(R.drawable.strikethrough);
                        // str.setSpan(new ForegroundColorSpan(0xFF006600), 0,
                        // str.setSpan(new ForegroundColorSpan
                        // (getResources().getColor(R.color.darkgreen)), 0,
                        // str.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        // color: 0x33336600
                    }

                    if (i == 0 && mTextSuffixChecked != null) {
                        // very simple
                        t.append(mTextSuffixChecked);
                    }

                } else {
                    // item not bought:
                    if (i == 0 && mTextSuffixUnchecked != null) {
                        t.append(mTextSuffixUnchecked);
                    }
                }
            }

            // we have a check box now.. more visual and gets the point across

            if (debug) {
                Log.i(TAG, "bindview: pos = " + cursor.getPosition());
            }

            // set style for check box

            if (mShowCheckBox) {
                state.mCheckView.setVisibility(CheckBox.VISIBLE);
                state.mCheckView.setChecked(status == ShoppingContract.Status.BOUGHT);
            } else {
                state.mCheckView.setVisibility(CheckBox.GONE);
            }

            if (inShopMode()) {
                state.mNoCheckView.setVisibility(ImageView.GONE);
            } else {  // mMode == ShoppingActivity.MODE_ADD_ITEMS
                if (status == ShoppingContract.Status.REMOVED_FROM_LIST) {
                    state.mNoCheckView.setVisibility(ImageView.VISIBLE);
                    if (mShowCheckBox) {
                        // replace check box
                        state.mCheckView.setVisibility(CheckBox.INVISIBLE);
                    }
                } else {
                    state.mNoCheckView.setVisibility(ImageView.INVISIBLE);
                }
            }
        }

        private void hideTextView(TextView view) {
            view.setVisibility(View.GONE);
            view.setText("");
        }

        public boolean setViewValue(View view, Cursor cursor, int i) {
            int id = view.getId();
            long price = 0;
            boolean hasPrice = false;
            String tags = null;
            String priceString = null;
            boolean hasTags = false;
            mItemRowState state = (mItemRowState) view.getTag();
            if (mPriceVisibility == View.VISIBLE) {
                price = getQuantityPrice(cursor);
                hasPrice = (price != 0);
            }
            if (mTagsVisibility == View.VISIBLE) {
                tags = cursor.getString(ShoppingActivity.mStringItemsITEMTAGS);
                hasTags = !TextUtils.isEmpty(tags);
            }

            if (id == R.id.name) {
                boolean hasNote = cursor
                        .getInt(ShoppingActivity.mStringItemsITEMHASNOTE) != 0;
                String name = cursor
                        .getString(ShoppingActivity.mStringItemsITEMNAME);
                TextView tv = (TextView) view;
                SpannedStringBuilder name_etc = new SpannedStringBuilder();
                name_etc.appendSpannedString(new ClickableItemSpan(), name);
                if (name.equalsIgnoreCase(mFilter)) {
                    name_etc.setSpan(new StyleSpan(Typeface.BOLD_ITALIC), 0, name_etc.length(), 0);
                }
                if (hasNote) {
                    Drawable d = getResources().getDrawable(R.drawable.ic_launcher_notepad_small);
                    float ratio = d.getIntrinsicWidth() / d.getIntrinsicHeight();
                    d.setBounds(0, 0, (int) (ratio * mTextSize), (int) mTextSize);
                    ImageSpan noteimgspan = new ImageSpan(d, ImageSpan.ALIGN_BASELINE);
                    name_etc.appendSpannedString(noteimgspan, new ClickableNoteSpan(), "\u00A0");
                }

                if (hasPrice) {
                    // set price text while setting name, so that correct size is known below
                    priceString = mPriceFormatter.format(price * 0.01d);
                    state.mPriceView.setText(priceString);
                }
                if (hasPrice && !hasTags) {
                    TextPaint paint = state.mPriceView.getPaint();
                    Rect bounds = new Rect();
                    ColorDrawable price_overlay = new ColorDrawable();
                    price_overlay.setAlpha(0);
                    paint.getTextBounds(priceString, 0, priceString.length(), bounds);
                    price_overlay.setBounds(0, 0, bounds.width(), bounds.height());
                    ImageSpan priceimgspan = new ImageSpan(price_overlay, ImageSpan.ALIGN_BASELINE);
                    name_etc.appendSpannedString(priceimgspan, " ");
                }
                tv.setText(name_etc);
                tv.setMovementMethod(LinkMovementMethod.getInstance());
                return true;
            } else if (id == R.id.price) {
                TextView tv = (TextView) view;
                if (hasPrice) {
                    tv.setVisibility(View.VISIBLE);
                    tv.setTextColor(mTextColorPrice);
                } else {
                    hideTextView(tv);
                }
                return true;
            } else if (id == R.id.tags) {

                TextView tv = (TextView) view;
                if (hasTags) {
                    tv.setVisibility(View.VISIBLE);
                    tv.setTextColor(mTextColorPrice);
                    tv.setText(tags);
                    if (hasPrice) {
                        // don't overlap the price
                        RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) tv.getLayoutParams();
                        rlp.addRule(RelativeLayout.LEFT_OF, R.id.price);
                    }
                } else {
                    hideTextView(tv);
                }
                return true;
            } else if (id == R.id.quantity) {
                String quantity = cursor.getString(ShoppingActivity.mStringItemsQUANTITY);
                TextView tv = (TextView) view;
                if (mQuantityVisibility == View.VISIBLE &&
                        !TextUtils.isEmpty(quantity)) {
                    tv.setVisibility(View.VISIBLE);
                    // tv.setTextColor(mPriceTextColor);
                    tv.setText(quantity + " ");
                } else {
                    hideTextView(tv);
                }
                return true;
            } else if (id == R.id.units) {
                String units = cursor.getString(ShoppingActivity.mStringItemsITEMUNITS);
                String quantity = cursor.getString(ShoppingActivity.mStringItemsQUANTITY);
                TextView tv = (TextView) view;
                // looks more natural if you only show units when showing qty.
                if (mUnitsVisibility == View.VISIBLE &&
                        mQuantityVisibility == View.VISIBLE &&
                        !TextUtils.isEmpty(units) && !TextUtils.isEmpty(quantity)) {
                    tv.setVisibility(View.VISIBLE);
                    // tv.setTextColor(mPriceTextColor);
                    tv.setText(units + " ");
                } else {
                    hideTextView(tv);
                }
                return true;
            } else if (id == R.id.priority) {
                String priority = cursor.getString(ShoppingActivity.mStringItemsPRIORITY);
                TextView tv = (TextView) view;
                if (mPriorityVisibility == View.VISIBLE &&
                        !TextUtils.isEmpty(priority)) {
                    tv.setVisibility(View.VISIBLE);
                    tv.setTextColor(mTextColorPriority);
                    tv.setText("-" + priority + "- ");
                } else {
                    hideTextView(tv);
                }
                return true;
            } else {
                return false;
            }
        }

        @Override
        public void setViewBinder(ViewBinder viewBinder) {
            throw new RuntimeException("this adapter implements setViewValue");
        }

        private class mItemRowState {
            public View mParentView;
            public TextView mNameView;
            public TextView mQuantityView;
            public TextView mUnitsView;
            public TextView mPriceView;
            public TextView mPriorityView;
            public TextView mTagsView;
            public CheckBox mCheckView;
            public ImageView mNoCheckView;

            public Cursor mCursor;
            public int mCursorPos;

            public mItemRowState(View view) {
                // This class is here to initialize state information related
                // to a single reusable item row, to reduce the amount of
                // setup that needs to be done each time the row is reused.
                //
                // Callbacks can be bound up-front here if they depend on cursor position.

                mParentView = view;
                mNameView = (TextView) view.findViewById(R.id.name);
                mPriceView = (TextView) view.findViewById(R.id.price);
                mTagsView = (TextView) view.findViewById(R.id.tags);
                mQuantityView = (TextView) view.findViewById(R.id.quantity);
                mUnitsView = (TextView) view.findViewById(R.id.units);
                mPriorityView = (TextView) view.findViewById(R.id.priority);
                mCheckView = (CheckBox) view.findViewById(R.id.check);
                mNoCheckView = (ImageView) view.findViewById(R.id.nocheck);

                mParentView.setTag(this);
                mNameView.setTag(this);
                mPriceView.setTag(this);
                mTagsView.setTag(this);
                mQuantityView.setTag(this);
                mUnitsView.setTag(this);
                mPriorityView.setTag(this);
                mCheckView.setTag(this);
                mNoCheckView.setTag(this);

                mQuantityView.setOnClickListener(new mItemClickListener("Quantity Click ",
                        EditItemDialog.FieldType.QUANTITY));
                mPriceView.setOnClickListener(new mItemClickListener("Click on price: ",
                        EditItemDialog.FieldType.PRICE));
                mUnitsView.setOnClickListener(new mItemClickListener("Click on units: ",
                        EditItemDialog.FieldType.UNITS));
                mPriorityView.setOnClickListener(new mItemClickListener("Click on priority: ",
                        EditItemDialog.FieldType.PRIORITY));
                mTagsView.setOnClickListener(new mItemClickListener("Click on tags: ",
                        EditItemDialog.FieldType.TAGS));

                mCheckView.setOnClickListener(new mItemToggleListener("Click: "));
                // also check around check box
                RelativeLayout l = (RelativeLayout) view.findViewById(R.id.check_surround);
                l.setTag(this);
                l.setOnClickListener(new mItemToggleListener("Click around: "));

                // Check for clicks on and around item text
                RelativeLayout r = (RelativeLayout) view.findViewById(R.id.description);
                r.setTag(this);
                r.setOnClickListener(new mItemClickListener("Click on description: ",
                        EditItemDialog.FieldType.ITEMNAME));

                mPriceView.setVisibility(mPriceVisibility);
                mTagsView.setVisibility(mTagsVisibility);
                mQuantityView.setVisibility(mQuantityVisibility);
                mUnitsView.setVisibility(mUnitsVisibility);
                mPriorityView.setVisibility(mPriorityVisibility);

            }

            private class mItemClickListener implements OnClickListener {
                private String mLogMessage;
                private EditItemDialog.FieldType mFieldType;

                public mItemClickListener(String logMessage, EditItemDialog.FieldType fieldType) {
                    mLogMessage = logMessage;
                    mFieldType = fieldType;
                }

                public void onClick(View v) {
                    if (debug) {
                        Log.d(TAG, mLogMessage);
                    }
                    if (mListener != null) {
                        mItemRowState state = (mItemRowState) v.getTag();
                        mListener.onCustomClick(state.mCursor, state.mCursorPos,
                                mFieldType, v);
                    }
                }
            }

            private class mItemToggleListener implements OnClickListener {
                private String mLogMessage;

                public mItemToggleListener(String logMessage) {
                    mLogMessage = logMessage;
                }

                public void onClick(View v) {
                    if (debug) {
                        Log.d(TAG, mLogMessage);
                    }
                    mItemRowState state = (mItemRowState) v.getTag();
                    toggleItemBought(state.mCursorPos);
                }
            }
        }

        private class ClickableNoteSpan extends ClickableSpan {
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                mItemRowState state = (mItemRowState) view.getTag();
                int cursorpos = state.mCursorPos;
                if (debug) {
                    Log.d(TAG, "Click on has_note: " + cursorpos);
                }
                mCursorItems.moveToPosition(cursorpos);
                long note_id = mCursorItems.getLong(ShoppingActivity.mStringItemsITEMID);
                Uri uri = ContentUris.withAppendedId(ShoppingContract.Notes.CONTENT_URI, note_id);
                i.setData(uri);
                Context context = getContext();
                try {
                    context.startActivity(i);
                } catch (ActivityNotFoundException e) {
                    // we could add a simple edit note dialog, but for now...
                    Dialog g = new DownloadAppDialog(context,
                            R.string.notepad_not_available,
                            R.string.notepad,
                            R.string.notepad_package,
                            R.string.notepad_website);
                    g.show();
                }
            }
        }

        private class ClickableItemSpan extends ClickableSpan {
            public void onClick(View view) {
                if (debug) {
                    Log.d(TAG, "Click on description: ");
                }
                if (mListener != null) {
                    mItemRowState state = (mItemRowState) view.getTag();
                    int cursorpos = state.mCursorPos;
                    mListener.onCustomClick(mCursorItems, cursorpos,
                            EditItemDialog.FieldType.ITEMNAME, view);
                }

            }

            public void updateDrawState(TextPaint ds) {
                // Override the parent's method to avoid having the text
                // in this span look like a link.
            }
        }

        private class SpannedStringBuilder extends SpannableStringBuilder {
            public SpannedStringBuilder appendSpannedString(Object o, CharSequence text) {
                int spanStart = length();
                super.append(text);
                setSpan(o, spanStart, spanStart + text.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                return this;
            }

            public SpannedStringBuilder appendSpannedString(Object o, Object p, CharSequence text) {
                int spanStart = length();
                super.append(text);
                setSpan(o, spanStart, spanStart + text.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                setSpan(p, spanStart, spanStart + text.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                return this;
            }
        }

    }

    public boolean inShopMode() {
        return mMode == MODE_IN_SHOP;
    }

    public boolean inAddItemsMode() {
        return mMode == MODE_ADD_ITEMS;
    }

    public boolean inPickItemsDialogMode() {
        return mMode == MODE_PICK_ITEMS_DLG;
    }

    private class SearchQueryListener implements SearchView.OnQueryTextListener {
        public boolean onQueryTextChange(String query) {
            boolean isIconified = mSearchView.isIconified();
            String prevFilter = mFilter;

            if (isIconified) {
                // Something tries to restore the query text after the drawer is dismissed, but
                // it doesn't re-expand the search view. Force the query string empty when it is
                // not shown, and switch back to non-search mode.
                if (query != null && query.length() > 0) {
                    mSearchView.setQuery("", false);
                }
                query = null;
                if (mInSearch) {
                    mMode = mModeBeforeSearch;
                    mInSearch = false;
                }
            }

            if (mInSearch == false && !isIconified) {
                mInSearch = true;
                mModeBeforeSearch = mMode;
                mMode = MODE_ADD_ITEMS;
            }

            if (query == null || query.length() == 0) {
                mFilter = null;
            } else {
                mFilter = query;
            }

            if ((prevFilter == null && mFilter == null) ||
                    (prevFilter != null && prevFilter.equals(mFilter))) {
                return true;
            }

            fillItems(mCursorActivity, mListId);

            return true;
        }

        public boolean onQueryTextSubmit(String query) {
            if (query.length() > 0) {
                insertNewItem(mCursorActivity, query, null, null, null, null);
                mSearchView.setQuery("", false);
                fillItems(mCursorActivity, mListId);
            }
            return true;
        }
    }

    private class SearchDismissedListener implements SearchView.OnCloseListener {
        public boolean onClose() {
            if (mInSearch) {
                mMode = mModeBeforeSearch;
                if (mModeChangeListener != null) {
                    mModeChangeListener.onModeChanged();
                }
            }
            mInSearch = false;
            mFilter = null;
            fillItems(mCursorActivity, mListId);
            // invalidate();
            return false;
        }
    }

}


import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

/* This class exposes a subset of PopupMenu functionality, and chooses whether
 * to use the platform PopupMenu (on Honeycomb or above) or a backported version.
 */
public class QuickSelectMenu {

    private android.support.v7.widget.PopupMenu mImplPlatform;

    private OnItemSelectedListener mItemSelectedListener;

    public QuickSelectMenu(Context context, View anchor) {
        mImplPlatform = new android.support.v7.widget.PopupMenu(context, anchor);
        mImplPlatform
                .setOnMenuItemClickListener(new android.support.v7.widget.PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        return onMenuItemClickImpl(item);
                    }
                });
    }

    // not sure if we want to expose this or just an add() method.
    public Menu getMenu() {
        return mImplPlatform.getMenu();
    }

    public void setOnItemSelectedListener(OnItemSelectedListener listener) {
        mItemSelectedListener = listener;
    }

    public void show() {
        mImplPlatform.show();
    }

    public boolean onMenuItemClickImpl(MenuItem item) {
        CharSequence name = item.getTitle();
        int id = item.getItemId();
        this.mItemSelectedListener.onItemSelected(name, id);
        return true;
    }

    // popup.setOnMenuItemClickListener(new
    // android.widget.PopupMenu.OnMenuItemClickListener() {

    /**
     * Interface responsible for receiving menu item click events if the items
     * themselves do not have individual item click listeners.
     */
    public interface OnItemSelectedListener {
        /**
         * This method will be invoked when an item is selected.
         *
         * @param item {@link CharSequence} that was selected
         * @param id
         */
        public void onItemSelected(CharSequence item, int id);
    }
}


import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.ViewBinder;
import android.widget.TextView;

import org.openintents.shopping.R;
import org.openintents.shopping.library.provider.ShoppingContract;
import org.openintents.shopping.library.provider.ShoppingContract.ItemStores;
import org.openintents.shopping.library.provider.ShoppingContract.Stores;
import org.openintents.shopping.library.util.PriceConverter;
import org.openintents.shopping.library.util.ShoppingUtils;
import org.openintents.shopping.ui.PreferenceActivity;

/**
 * View to show a list of stores for a specific item
 */
public class StoreListView extends ListView {
    private final static String TAG = "StoreListView";
    private final static boolean debug = false;
    private final static int cursorColumnID = 0;
    private final static int cursorColumnNAME = 1;
    private final static int cursorColumnSTOCKS_ITEM = 2;
    private final static int cursorColumnPRICE = 3;
    private final static int cursorColumnAISLE = 4;
    private final static int cursorColumnSTORE_ID = 5;
    private final String[] mStringItems = new String[]{
            "itemstores." + ItemStores._ID, Stores.NAME,
            ItemStores.STOCKS_ITEM, ItemStores.PRICE, ItemStores.AISLE,
            "stores._id as store_id"};
    public int mPriceVisibility;
    public String mTextTypeface;
    public float mTextSize;
    public boolean mTextUpperCaseFont;
    public int mTextColor;
    public int mTextColorPrice;
    public int mTextColorChecked;
    public boolean mShowCheckBox;
    public boolean mInTextInput;
    public boolean mBinding;
    private Typeface mCurrentTypeface;
    private boolean mTextChanged;
    private Cursor mCursorItemstores;
    private long mItemId;
    private long mListId;
    private ContentValues[] mBackup;
    private boolean mDirty;

    private EditText m_lastView;
    private int m_lastCol;
    private ContentObserver mContentObserver = new ContentObserver(new Handler()) {

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            mDirty = true;
            if (mCursorItemstores != null && !mInTextInput) {
                try {
                    requery();
                } catch (IllegalStateException e) {
                    Log.e(TAG, "IllegalStateException ", e);
                    // Somehow the logic is not completely right yet...
                    mCursorItemstores = null;
                }
            }

        }

    };

    public StoreListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public StoreListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public StoreListView(Context context) {
        super(context);
        init();
    }

    public void applyUpdate() {
        if (m_lastView == null) {
            return;
        }
        String val = m_lastView.getText().toString();
        if (m_lastCol == cursorColumnPRICE) {
            val = Long.toString(PriceConverter.getCentPriceFromString(val));
        }
        Integer row = (Integer) m_lastView.getTag();
        if (row != null) {
            if (debug) {
                Log.d(TAG, "Text changed to " + val + " @ pos " + row
                        + ", col " + m_lastCol);
            }
            maybeUpdate(row, m_lastCol, val);
        }
        m_lastView = null;
    }

    private void init() {

    }

    public void onResume() {

        // Content observer registered at fillItems()
        // registerContentObserver();
    }

    public void onPause() {
        unregisterContentObserver();
    }

    private void backupValues() {
        int nRows = mCursorItemstores.getCount();
        if (mBackup != null) {
            return;
        }
        mBackup = new ContentValues[nRows];
        int i = 0;
        while (mCursorItemstores.moveToNext()) {
            mBackup[i] = new ContentValues();
            DatabaseUtils.cursorRowToContentValues(mCursorItemstores,
                    mBackup[i++]);
        }
    }

    public void undoChanges() {
        for (int i = 0; i < mBackup.length; i++) {
            ContentValues cv = mBackup[i];
            String storeId = cv.getAsString("store_id");

            if (cv.getAsString("_id") == null) {
                // dummy record. delete any itemstores for this item id and
                // store id.
                // (may have been created during editing)
                ContentResolver cr = getContext().getContentResolver();
                Cursor existingItems = cr.query(ItemStores.CONTENT_URI,
                        new String[]{ItemStores._ID},
                        "store_id = ? AND item_id = ?", new String[]{storeId,
                                String.valueOf(mItemId)}, null
                );
                if (existingItems.getCount() > 0) {
                    existingItems.moveToFirst();
                    long id = existingItems.getLong(cursorColumnID);
                    cr.delete(
                            ItemStores.CONTENT_URI.buildUpon()
                                    .appendPath(String.valueOf(id)).build(),
                            null, null
                    );
                    existingItems.close();
                }
            } else {
                // real record, restore its values.
                // long itemstore_id = cv.getAsLong("_id");
                boolean has_item = cv.getAsBoolean("stocks_item");
                String price = cv.getAsString("price");
                String aisle = cv.getAsString("aisle");
                ShoppingUtils.addItemToStore(getContext(), mItemId,
                        Long.parseLong(storeId), has_item, aisle, price, false);
            }
        }
    }

    /**
     * @param activity Activity to manage the cursor.
     * @param listId
     * @return
     */
    public Cursor fillItems(Activity activity, long listId, long itemId) {

        mListId = listId;
        mItemId = itemId;
        String sortOrder = "stores.name";

        if (mCursorItemstores != null && !mCursorItemstores.isClosed()) {
            mCursorItemstores.close();
        }

        // Get a cursor for all stores
        mCursorItemstores = getContext().getContentResolver().query(
                ItemStores.CONTENT_URI.buildUpon().appendPath("item")
                        .appendPath(String.valueOf(mItemId))
                        .appendPath(String.valueOf(mListId)).build(),
                mStringItems, null, null, sortOrder
        );
        activity.startManagingCursor(mCursorItemstores);

        registerContentObserver();

        if (mCursorItemstores == null) {
            Log.e(TAG, "missing shopping provider");
            setAdapter(new ArrayAdapter<String>(this.getContext(),
                    android.R.layout.simple_list_item_1,
                    new String[]{"no shopping provider"}));
            return mCursorItemstores;
        }

        backupValues();

        int layout_row = R.layout.list_item_store;
        mPriceVisibility = PreferenceActivity
                .getUsingPerStorePricesFromPrefs(getContext()) ? View.VISIBLE
                : View.INVISIBLE;

        mSimpleCursorAdapter adapter = new mSimpleCursorAdapter(
                this.getContext(),
                // Use a template that displays a text view
                layout_row,
                // Give the cursor to the list adapter
                mCursorItemstores,
                // Map the IMAGE and NAME to...
                new String[]{Stores.NAME, ItemStores.PRICE, ItemStores.AISLE},
                // the view defined in the XML template
                new int[]{R.id.name, R.id.price, R.id.aisle});
        setAdapter(adapter);

        return mCursorItemstores;
    }

    /**
     *
     */
    private void registerContentObserver() {
        getContext().getContentResolver()
                .registerContentObserver(
                        ShoppingContract.ItemStores.CONTENT_URI, true,
                        mContentObserver);
    }

    private void unregisterContentObserver() {
        getContext().getContentResolver().unregisterContentObserver(
                mContentObserver);
    }

    public void toggleItemstore(int position) {
        if (mCursorItemstores.getCount() <= position) {
            Log.e(TAG, "toggle inexistent item. Probably clicked too quickly?");
            return;
        }

        mCursorItemstores.moveToPosition(position);

        long oldstatus = 0;

        // should first check if the itemstore record exists...
        String itemstore_id;

        if (mCursorItemstores.isNull(0)) {
            long storeId = mCursorItemstores.getLong(cursorColumnSTORE_ID);
            long isid = ShoppingUtils.addItemToStore(getContext(), mItemId,
                    storeId, "", "", false);
            itemstore_id = Long.toString(isid);
        } else {
            itemstore_id = mCursorItemstores.getString(cursorColumnID);
            oldstatus = mCursorItemstores.getLong(cursorColumnSTOCKS_ITEM);
        }

        // Toggle status:
        long newstatus = 1 - oldstatus;

        ContentValues values = new ContentValues();
        values.put(ItemStores.STOCKS_ITEM, newstatus);
        if (debug) {
            Log.d(TAG, "update row " + itemstore_id + ", newstatus "
                    + newstatus);
        }
        getContext().getContentResolver().update(
                Uri.withAppendedPath(ShoppingContract.ItemStores.CONTENT_URI,
                        itemstore_id), values, null, null
        );

        requery();
        invalidate();
    }

    public void maybeUpdate(int position, int column, String new_val) {
        if (mCursorItemstores.getCount() <= position) {
            Log.e(TAG, "edit nonexistent item.");
            return;
        }

        mCursorItemstores.moveToPosition(position);
        String old_val = mCursorItemstores.getString(column);
        if (new_val.equals(old_val)) {
            return;
        }

        if (mCursorItemstores.isNull(0)) {
            long storeId = mCursorItemstores.getLong(cursorColumnSTORE_ID);
            String aisle = "";
            String price = "";

            if (column == 3) {
                price = new_val;
            }
            if (column == 4) {
                aisle = new_val;
            }
            ShoppingUtils.addItemToStore(getContext(), mItemId, storeId, aisle,
                    price, false);

            /*
             * At the corresponding points in the item view, we would requery
             * and invalidate. However that is mainly because the editing
             * happens in widgets outside the list view itself, where here it
             * happens in EditTexts directly in the list. So we probably don't
             * need to invalidate() here. Do we really need to requery()?
             * Probably somewhere, perhaps not here.
             */
            // requery();
            // invalidate();
            // need to do those somewhere else.
            mDirty = true;
            return;
        }

        String itemstore_id = mCursorItemstores.getString(cursorColumnID);
        Uri uri = Uri.withAppendedPath(ItemStores.CONTENT_URI, itemstore_id);
        ContentValues cv = new ContentValues();
        cv.put(mStringItems[column], new_val);
        getContext().getContentResolver().update(uri, cv, null, null);

        // see comment above
        // requery();
        // invalidate();
        mDirty = true;

    }

    public void requery() {
        if (debug) {
            Log.d(TAG, "requery()");
        }
        mCursorItemstores.requery();
        mDirty = false;
    }

    public String getStoreName(int cursorPosition) {
        String name = "";
        Cursor c = mCursorItemstores;
        if (c != null) {
            if (c.moveToPosition(cursorPosition)) {
                name = c.getString(cursorColumnNAME);
            }
        }
        return name;
    }

    public String getStoreId(int cursorPosition) {
        String id = null;
        Cursor c = mCursorItemstores;
        if (c != null) {
            if (c.moveToPosition(cursorPosition)) {
                id = c.getString(cursorColumnSTORE_ID);
            }
        }
        return id;
    }

    /**
     * Extend the SimpleCursorAdapter to handle updates to the data
     */
    public class mSimpleCursorAdapter extends SimpleCursorAdapter implements
            ViewBinder {

        /**
         * Constructor simply calls super class.
         *
         * @param context Context.
         * @param layout  Layout.
         * @param c       Cursor.
         * @param from    Projection from.
         * @param to      Projection to.
         */
        mSimpleCursorAdapter(final Context context, final int layout,
                             final Cursor c, final String[] from, final int[] to) {
            super(context, layout, c, from, to);
            super.setViewBinder(this);

        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = super.newView(context, cursor, parent);

            EditText v;
            v = (EditText) view.findViewById(R.id.price);
            v.addTextChangedListener(new EditTextWatcher(v, cursorColumnPRICE));
            v.setVisibility(mPriceVisibility);

            v = (EditText) view.findViewById(R.id.aisle);
            v.addTextChangedListener(new EditTextWatcher(v, cursorColumnAISLE));
            v.setVisibility(mPriceVisibility);

            return view;
        }

        /**
         * Additionally to the standard bindView, we also check for STATUS, and
         * strike the item through if BOUGHT.
         */
        @Override
        public void bindView(final View view, final Context context,
                             final Cursor cursor) {

            // set tags to null during binding, to help avoid extra db updates
            // while binding
            EditText v;
            v = (EditText) view.findViewById(R.id.price);
            v.setTag(null);
            v = (EditText) view.findViewById(R.id.aisle);
            v.setTag(null);

            mBinding = true;
            super.bindView(view, context, cursor);
            mBinding = false;

            boolean status = cursor.getInt(cursorColumnSTOCKS_ITEM) != 0;
            final int cursorpos = cursor.getPosition();

            CheckBox c = (CheckBox) view.findViewById(R.id.check);

            if (debug) {
                Log.i(TAG, "bindview: pos = " + cursor.getPosition());
            }

            // set style for check box
            c.setTag(cursor.getPosition());

            c.setVisibility(CheckBox.VISIBLE);
            c.setChecked(status);

            // The parent view knows how to deal with clicks.
            // We just pass the click through.
            // c.setClickable(false);

            c.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    if (debug) {
                        Log.d(TAG, "Click: ");
                    }
                    toggleItemstore(cursorpos);
                }

            });

            TextView t;

            t = (TextView) view.findViewById(R.id.name);
            t.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {

                public void onCreateContextMenu(ContextMenu contextmenu,
                                                View view, ContextMenuInfo info) {
                    // Context menus are created in the main activity
                    // ItemStoresActivity
                }

            });

            v = (EditText) view.findViewById(R.id.price);
            v.setTag(cursor.getPosition());
            v = (EditText) view.findViewById(R.id.aisle);
            v.setTag(cursor.getPosition());

        }

        public boolean setViewValue(View view, Cursor cursor, int i) {
            int id = view.getId();
            if (id == R.id.price) {
                long price = cursor.getLong(cursorColumnPRICE);
                if (price != 0) {
                    String text = PriceConverter.getStringFromCentPrice(price);
                    ((TextView) view).setText(text);
                    return true;
                }
            }
            // let SimpleCursorAdapter handle the binding.
            return false;
        }

        @Override
        public void setViewBinder(ViewBinder viewBinder) {
            throw new RuntimeException("this adapter implements setViewValue");
        }

        private class EditTextWatcher implements TextWatcher,
                OnFocusChangeListener {

            private int mCol;
            private EditText mView;

            public EditTextWatcher(EditText v, int col) {
                if (debug) {
                    Log.d(TAG, "New EditTextWatcher for " + v.toString()
                            + " col " + col);
                }
                mView = v;
                mCol = col;
            }

            @Override
            public void afterTextChanged(Editable s) {

                if (mBinding) {
                    return; // for update purposes, doesn't count as change
                }

                if (mView != m_lastView) {
                    mView.setOnFocusChangeListener(this);
                    // applyUpdate();
                }

                m_lastView = mView;
                m_lastCol = mCol;

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {

            }

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (v == m_lastView && hasFocus == false) {
                    mInTextInput = true;
                    applyUpdate();
                    mInTextInput = false;
                }
            }

        }

    }
}


import android.content.Context;

import org.openintents.shopping.R;

public class NewListDialog extends RenameListDialog {

    public NewListDialog(Context context) {
        super(context);

        setTitle(R.string.ask_new_list);
    }

    public NewListDialog(Context context, DialogActionListener listener) {
        super(context);

        setTitle(R.string.ask_new_list);
        setDialogActionListener(listener);
    }

}


public interface DialogActionListener {

    public void onAction(String name);
}


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.text.method.KeyListener;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import org.openintents.shopping.R;
import org.openintents.shopping.ui.PreferenceActivity;

public class RenameListDialog extends AlertDialog implements OnClickListener {

    protected EditText mEditText;
    private Context mContext;
    private DialogActionListener mDialogActionListener;

    public RenameListDialog(Context context) {
        super(context);
        mContext = context;
        init();
    }

    public RenameListDialog(Context context, String name,
                            DialogActionListener listener) {
        super(context);
        mContext = context;
        init();
        setName(name);
        setDialogActionListener(listener);
    }

    /**
     * @param context
     */
    private void init() {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        final View view = inflater.inflate(R.layout.dialog_rename_list, null);
        setView(view);

        mEditText = (EditText) view.findViewById(R.id.edittext);

        KeyListener kl = PreferenceActivity
                .getCapitalizationKeyListenerFromPrefs(mContext);
        mEditText.setKeyListener(kl);

        setIcon(android.R.drawable.ic_menu_edit);
        setTitle(R.string.ask_rename_list);

        setButton(mContext.getText(R.string.ok), this);
        setButton2(mContext.getText(R.string.cancel), this);
    }

    public void setName(String name) {
        mEditText.setText(name);

        // To move cursor position to the end of list's name
        mEditText.setSelection(name.length());
    }

    public void setDialogActionListener(DialogActionListener listener) {
        mDialogActionListener = listener;
    }

    public void onClick(DialogInterface dialog, int which) {
        if (which == BUTTON1) {
            pressOk();
        }

    }

    public void pressOk() {
        String name = mEditText.getText().toString();
        mDialogActionListener.onAction(name);
    }
}


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.KeyListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.ImageButton;
import android.widget.MultiAutoCompleteTextView;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorAdapter.CursorToStringConverter;
import android.widget.TextView;

import org.openintents.distribution.DownloadAppDialog;
import org.openintents.shopping.R;
import org.openintents.shopping.library.provider.ShoppingContract;
import org.openintents.shopping.library.provider.ShoppingContract.Contains;
import org.openintents.shopping.library.provider.ShoppingContract.Items;
import org.openintents.shopping.library.provider.ShoppingContract.Units;
import org.openintents.shopping.library.util.PriceConverter;
import org.openintents.shopping.ui.ItemStoresActivity;
import org.openintents.shopping.ui.PreferenceActivity;

public class EditItemDialog extends AlertDialog implements OnClickListener {

    private final String[] mProjection = {ShoppingContract.Items.NAME,
            ShoppingContract.Items.TAGS, ShoppingContract.Items.PRICE,
            ShoppingContract.Items.NOTE, ShoppingContract.Items._ID,
            ShoppingContract.Items.UNITS};
    private final String[] mRelationProjection = {
            ShoppingContract.Contains.QUANTITY,
            ShoppingContract.Contains.PRIORITY};
    private Context mContext;
    private Uri mItemUri;
    private Uri mListItemUri;
    private long mItemId;
    private String mNoteText;
    private EditText mEditText;
    private MultiAutoCompleteTextView mTags;
    private EditText mPrice;
    private Button mPriceStore;
    private EditText mQuantity;
    private EditText mPriority;
    private AutoCompleteTextView mUnits;
    private TextView mPriceLabel;
    private ImageButton mNote;
    private String[] mTagList;
    private OnItemChangedListener mOnItemChangedListener;
    private SimpleCursorAdapter mUnitsAdapter;
    private TextWatcher mTextWatcher = new TextWatcher() {

        @Override
        public void afterTextChanged(Editable arg0) {
            updateQuantityPrice();
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                                      int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before,
                                  int count) {
        }

    };
    private Uri mRelationUri;

    public EditItemDialog(final Context context, final Uri itemUri,
                          final Uri relationUri, final Uri listItemUri) {
        super(context);
        mContext = context;

        LayoutInflater inflater = LayoutInflater.from(context);
        final View view = inflater.inflate(R.layout.dialog_edit_item, null);
        setView(view);

        mEditText = (EditText) view.findViewById(R.id.edittext);
        mTags = (MultiAutoCompleteTextView) view.findViewById(R.id.edittags);
        mPrice = (EditText) view.findViewById(R.id.editprice);
        mQuantity = (EditText) view.findViewById(R.id.editquantity);
        mPriority = (EditText) view.findViewById(R.id.editpriority);
        mUnits = (AutoCompleteTextView) view.findViewById(R.id.editunits);

        mUnitsAdapter = new SimpleCursorAdapter(mContext,
                android.R.layout.simple_dropdown_item_1line, null,
                // Map the units name...
                new String[]{Units.NAME},
                // to the view defined in the XML template
                new int[]{android.R.id.text1});
        mUnitsAdapter.setCursorToStringConverter(new CursorToStringConverter() {
            public String convertToString(android.database.Cursor cursor) {
                return cursor.getString(1);
            }
        });
        mUnitsAdapter.setFilterQueryProvider(new FilterQueryProvider() {
            public Cursor runQuery(CharSequence constraint) {
                // Search for units whose names begin with the specified
                // letters.
                String query = null;
                String[] args = null;

                if (constraint != null) {
                    // query = "units." + Units.NAME + " like '?%' ";
                    // args = new String[] {(constraint != null ?
                    // constraint.toString() : null)} ;
                    // http://code.google.com/p/android/issues/detail?id=3153
                    //
                    // workaround:
                    query = "units." + Units.NAME + " like '"
                            + constraint.toString() + "%' ";
                }

                return mContext.getContentResolver().query(
                        Units.CONTENT_URI,
                        new String[]{Units._ID, Units.NAME}, query, args,
                        Units.NAME);
            }
        });
        mUnits.setAdapter(mUnitsAdapter);
        mUnits.setThreshold(0);

        mPriceStore = (Button) view.findViewById(R.id.pricestore);

        mPriceStore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, ItemStoresActivity.class);
                intent.setData(mListItemUri);
                context.startActivity(intent);
            }
        });

        mNote = (ImageButton) view.findViewById(R.id.note);
        mNote.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Uri uri = ContentUris.withAppendedId(
                        ShoppingContract.Notes.CONTENT_URI, mItemId);

                if (mNoteText == null) {
                    // Maybe an earlier edit activity added it? If so,
                    // we should not replace with empty string below.
                    Cursor c = mContext.getContentResolver().query(mItemUri,
                            new String[]{ShoppingContract.Items.NOTE}, null,
                            null, null);
                    if (c != null) {
                        if (c.moveToFirst()) {
                            mNoteText = c.getString(0);
                        }
                        c.close();
                    }
                }

                if (mNoteText == null) {
                    // can't edit a null note, put an empty one instead.
                    ContentValues values = new ContentValues();
                    values.put("note", "");
                    mContext.getContentResolver().update(mItemUri, values,
                            null, null);
                    mContext.getContentResolver().notifyChange(mItemUri, null);
                }

                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(uri);
                try {
                    mContext.startActivity(i);
                } catch (ActivityNotFoundException e) {
                    Dialog g = new DownloadAppDialog(mContext,
                            R.string.notepad_not_available, R.string.notepad,
                            R.string.notepad_package, R.string.notepad_website);
                    g.show();
                }
            }

        });

        mPriceLabel = (TextView) view.findViewById(R.id.labeleditprice);

        final KeyListener kl = PreferenceActivity
                .getCapitalizationKeyListenerFromPrefs(context);
        mEditText.setKeyListener(kl);
        mTags.setKeyListener(kl);

        mTags.setImeOptions(EditorInfo.IME_ACTION_DONE);
        mTags.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
        mTags.setThreshold(0);
        mTags.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                toggleTaglistPopup();
            }

        });

        // setIcon(android.R.drawable.ic_menu_edit);
        setTitle(R.string.ask_edit_item);

        setItemUri(itemUri, listItemUri);
        setRelationUri(relationUri);

        setButton(context.getText(R.string.ok), this);
        setButton2(context.getText(R.string.cancel), this);

        /*
         * setButton(R.string.ok, new DialogInterface.OnClickListener() { public
         * void onClick(DialogInterface dialog, int whichButton) {
         *
         * dialog.dismiss(); doTextEntryDialogAction(mTextEntryMenu, (Dialog)
         * dialog);
         *
         * } }).setNegativeButton(R.string.cancel, new
         * DialogInterface.OnClickListener() { public void
         * onClick(DialogInterface dialog, int whichButton) {
         *
         * dialog.cancel(); } }).create();
         */

        mQuantity.addTextChangedListener(mTextWatcher);
        mPrice.addTextChangedListener(mTextWatcher);
    }

    public void setTagList(String[] taglist) {
        mTagList = taglist;

        if (taglist != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(mContext,
                    android.R.layout.simple_dropdown_item_1line, mTagList);
            mTags.setAdapter(adapter);
        }
    }

    /**
     * Set cursor to be requeried if item is changed.
     *
     * @param c
     */
    public void setOnItemChangedListener(OnItemChangedListener listener) {
        mOnItemChangedListener = listener;
    }

    private void toggleTaglistPopup() {
        if (mTags.isPopupShowing()) {
            mTags.dismissDropDown();
        } else {
            mTags.showDropDown();
        }
    }

    void updateQuantityPrice() {
        try {
            double price = Double.parseDouble(mPrice.getText().toString());
            String quantityString = mQuantity.getText().toString();
            if (!TextUtils.isEmpty(quantityString)) {
                double quantity = Double.parseDouble(quantityString);
                price = quantity * price;
                String s = PriceConverter.mPriceFormatter.format(price);
                mPriceLabel
                        .setText(mContext.getText(R.string.price) + ": " + s);
                return;
            }
        } catch (NumberFormatException e) {
            // do nothing
        }

        // Otherwise show default label:
        mPriceLabel.setText(mContext.getText(R.string.price));
    }

    public void setItemUri(Uri itemUri, Uri listItemUri) {
        mItemUri = itemUri;
        mListItemUri = listItemUri;

        Cursor c = mContext.getContentResolver().query(mItemUri, mProjection,
                null, null, null);
        if (c != null && c.moveToFirst()) {
            String text = c.getString(0);
            String tags = c.getString(1);
            long pricecent = c.getLong(2);
            String price = PriceConverter.getStringFromCentPrice(pricecent);
            mNoteText = c.getString(3);
            mItemId = c.getLong(4);
            String units = c.getString(5);

            mEditText.setText(text);
            mTags.setText(tags);
            mPrice.setText(price);

            if (units == null) {
                units = "";
            }
            mUnits.setText(units);

            boolean trackPerStorePrices = PreferenceActivity
                    .getUsingPerStorePricesFromPrefs(mContext);

            if (!trackPerStorePrices) {
                mPrice.setVisibility(View.VISIBLE);
                mPriceStore.setVisibility(View.GONE);
            } else {
                mPrice.setVisibility(View.GONE);
                mPriceStore.setVisibility(View.VISIBLE);
            }
        }
        c.close();
    }

    public void setRelationUri(Uri relationUri) {
        mRelationUri = relationUri;
        Cursor c = mContext.getContentResolver().query(mRelationUri,
                mRelationProjection, null, null, null);
        if (c != null && c.moveToFirst()) {
            String quantity = c.getString(0);
            mQuantity.setText(quantity);
            String priority = c.getString(1);
            mPriority.setText(priority);
        }
        c.close();
    }

    public void onClick(DialogInterface dialog, int which) {
        if (which == BUTTON1) {
            editItem();
        }

    }

    void editItem() {
        String text = mEditText.getText().toString();
        String tags = mTags.getText().toString();
        String price = mPrice.getText().toString();
        String quantity = mQuantity.getText().toString();
        String priority = mPriority.getText().toString();
        String units = mUnits.getText().toString();

        Long priceLong = PriceConverter.getCentPriceFromString(price);

        text = text.trim();

        // Remove trailing ","
        tags = tags.trim();
        if (tags.endsWith(",")) {
            tags = tags.substring(0, tags.length() - 1);
        }
        tags = tags.trim();

        ContentValues values = new ContentValues();
        values.put(Items.NAME, text);
        values.put(Items.TAGS, tags);
        if (price != null) {
            values.put(Items.PRICE, priceLong);
        }
        if (units != null) {
            values.put(Items.UNITS, units);
        }
        mContext.getContentResolver().update(mItemUri, values, null, null);
        mContext.getContentResolver().notifyChange(mItemUri, null);

        values.clear();
        values.put(Contains.QUANTITY, quantity);
        values.put(Contains.PRIORITY, priority);

        mContext.getContentResolver().update(mRelationUri, values, null, null);
        mContext.getContentResolver().notifyChange(mRelationUri, null);

        if (mOnItemChangedListener != null) {
            mOnItemChangedListener.onItemChanged();
        }
    }

    private void focus_field(EditText e, Boolean selectAll) {
        InputMethodManager imm = (InputMethodManager) mContext
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (selectAll) {
            e.selectAll();
        }
        if (e.requestFocus())
        // this part doesn't seem to work:
        {
            imm.showSoftInput(e, 0);
        }
        imm.toggleSoftInputFromWindow(e.getWindowToken(), 0, 0);
    }

    public void setFocusField(FieldType focusField) {

        switch (focusField) {
            // hack, need to share some values with ShoppingActivity.
            case QUANTITY:
                focus_field(mQuantity, true);
                break;
            case PRIORITY:
                focus_field(mPriority, true);
                break;
            case PRICE:
                focus_field(mPrice, true);
                break;
            case UNITS:
                focus_field(mUnits, true);
                break;
            case TAGS:
                focus_field(mTags, false);
                break;
            case ITEMNAME:
                focus_field(mEditText, false);
                break;
            default:
                break;

        }
    }

    public enum FieldType {
        ITEMNAME, QUANTITY, PRICE, PRIORITY, UNITS, TAGS
    }

    public interface OnItemChangedListener {
        public void onItemChanged();
    }

}

/*
 * Copyright (C) 2007-2010 OpenIntents.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;

import org.openintents.shopping.LogConstants;
import org.openintents.shopping.R;
import org.openintents.shopping.theme.ThemeShoppingList;
import org.openintents.shopping.theme.ThemeUtils;
import org.openintents.shopping.theme.ThemeUtils.ThemeInfo;
import org.openintents.shopping.ui.PreferenceActivity;

import java.util.List;

public class ThemeDialog extends AlertDialog implements OnClickListener,
        OnCancelListener, OnItemClickListener {
    private static final String TAG = "ThemeDialog";
    private static final boolean debug = false || LogConstants.debug;

    private static final String BUNDLE_THEME = "theme";

    private Context mContext;
    private ThemeDialogListener mListener;
    private ListView mListView;
    private CheckBox mCheckBox;
    private List<ThemeInfo> mListInfo;

    public ThemeDialog(Context context) {
        super(context);
        mContext = context;
        init();
    }

    public ThemeDialog(Context context, ThemeDialogListener listener) {
        super(context);
        mContext = context;
        mListener = listener;
        init();
    }

    private void init() {
        setInverseBackgroundForced(true);

        LayoutInflater inflate = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view;

        view = inflate.inflate(R.layout.dialog_theme_settings, null);

        setView(view);

        mListView = (ListView) view.findViewById(R.id.list1);
        mListView.setCacheColorHint(0);
        mListView.setItemsCanFocus(false);
        mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        Button b = new Button(mContext);
        b.setText(R.string.get_more_themes);
        b.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent i = new Intent(mContext, PreferenceActivity.class);
                i.putExtra(PreferenceActivity.EXTRA_SHOW_GET_ADD_ONS, true);
                mContext.startActivity(i);

                pressCancel();
                dismiss();
            }
        });

        LinearLayout ll = new LinearLayout(mContext);
        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);
        ll.setPadding(20, 10, 20, 10);
        ll.addView(b, lp);
        ll.setGravity(Gravity.CENTER);
        mListView.addFooterView(ll);

        mCheckBox = (CheckBox) view.findViewById(R.id.check1);

        setTitle(R.string.theme_pick);

        setButton(Dialog.BUTTON_POSITIVE, mContext.getText(R.string.ok), this);
        setButton(Dialog.BUTTON_NEGATIVE, mContext.getText(R.string.cancel),
                this);
        setOnCancelListener(this);

        prepareDialog();
    }

    private void fillThemes() {
        mListInfo = ThemeUtils.getThemeInfos(mContext,
                ThemeShoppingList.SHOPPING_LIST_THEME);

        String[] s = new String[mListInfo.size()];
        int i = 0;
        for (ThemeInfo ti : mListInfo) {
            s[i] = ti.title;
            i++;
        }

        mListView.setAdapter(new ArrayAdapter<>(mContext,
                android.R.layout.simple_list_item_single_choice, s));

        mListView.setOnItemClickListener(this);
    }

    public void prepareDialog() {
        fillThemes();
        updateList();
        mCheckBox.setChecked(PreferenceActivity.getThemeSetForAll(mContext));
    }

    /**
     * Set selection to currently used theme.
     */
    private void updateList() {
        String theme = mListener.onLoadTheme();

        // Check special cases for backward compatibility:
        if ("1".equals(theme)) {
            theme = mContext.getResources().getResourceName(
                    R.style.Theme_ShoppingList);
        } else if ("2".equals(theme)) {
            theme = mContext.getResources().getResourceName(
                    R.style.Theme_ShoppingList_Classic);
        } else if ("3".equals(theme)) {
            theme = mContext.getResources().getResourceName(
                    R.style.Theme_ShoppingList_Android);
        }

        // Reset selection in case the current theme is not
        // in this list (for example got uninstalled).
        mListView.setItemChecked(-1, false);
        mListView.setSelection(0);

        int pos = 0;
        for (ThemeInfo ti : mListInfo) {
            if (ti.styleName.equals(theme)) {
                mListView.setItemChecked(pos, true);

                // Move list to show the selected item:
                mListView.setSelection(pos);
                break;
            }
            pos++;
        }
    }

    @Override
    public Bundle onSaveInstanceState() {
        if (debug) {
            Log.d(TAG, "onSaveInstanceState");
        }

        Bundle b = super.onSaveInstanceState();
        String theme = getSelectedTheme();
        b.putString(BUNDLE_THEME, theme);
        return b;
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (debug) {
            Log.d(TAG, "onRestore");
        }

        String theme = getSelectedTheme();

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(BUNDLE_THEME)) {
                theme = savedInstanceState.getString(BUNDLE_THEME);

                if (debug) {
                    Log.d(TAG, "onRestore theme " + theme);
                }
            }
        }

        mListener.onSetTheme(theme);
    }

    public void onClick(DialogInterface dialog, int which) {
        if (which == BUTTON_POSITIVE) {
            pressOk();
        } else if (which == BUTTON_NEGATIVE) {
            pressCancel();
        }

    }

    @Override
    public void onCancel(DialogInterface arg0) {
        pressCancel();
    }

    public void pressOk() {

        /* User clicked Yes so do some stuff */
        String theme = getSelectedTheme();
        mListener.onSaveTheme(theme);
        mListener.onSetTheme(theme);

        boolean setForAllThemes = mCheckBox.isChecked();
        PreferenceActivity.setThemeSetForAll(mContext, setForAllThemes);
        if (setForAllThemes) {
            mListener.onSetThemeForAll(theme);
        }
    }

    private String getSelectedTheme() {
        int pos = mListView.getCheckedItemPosition();

        if (pos != ListView.INVALID_POSITION) {
            ThemeInfo ti = mListInfo.get(pos);
            return ti.styleName;
        } else {
            return null;
        }
    }

    public void pressCancel() {
        /* User clicked No so do some stuff */
        String theme = mListener.onLoadTheme();
        mListener.onSetTheme(theme);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {
        String theme = getSelectedTheme();

        if (theme != null) {
            mListener.onSetTheme(theme);
        }
    }

    public interface ThemeDialogListener {
        void onSetTheme(String theme);

        void onSetThemeForAll(String theme);

        String onLoadTheme();

        void onSaveTheme(String theme);
    }
}


import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.widget.RemoteViews;

import org.openintents.shopping.R;
import org.openintents.shopping.ShoppingActivity;
import org.openintents.shopping.library.provider.ShoppingContract;
import org.openintents.shopping.library.provider.ShoppingContract.ContainsFull;
import org.openintents.shopping.ui.PreferenceActivity;
import org.openintents.shopping.ui.widget.ShoppingItemsView;

import java.util.List;

public class CheckItemsWidget extends AppWidgetProvider {
    private final static int LIMIT_ITEMS = 5;
    private final static String PREFS = "check_items_widget";
    private final static String ACTION_CHECK = "ActionCheck";
    private final static String ACTION_NEXT_PAGE = "ActionNextPage";
    private final static String ACTION_PREV_PAGE = "ActionPrevPage";

    public static Cursor fillItems(Context context, long listId) {
        String sortOrder = PreferenceActivity.getSortOrderFromPrefs(context,
                ShoppingItemsView.MODE_IN_SHOP);
        String selection = "list_id = ? AND "
                + ShoppingContract.Contains.STATUS + " == "
                + ShoppingContract.Status.WANT_TO_BUY;

        return context.getContentResolver().query(
                ContainsFull.CONTENT_URI, ShoppingActivity.PROJECTION_ITEMS,
                selection, new String[]{String.valueOf(listId)}, sortOrder);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        Bundle extras = intent.getExtras();
        if (extras != null) {
            int widgetId = extras.getInt("widgetId",
                    AppWidgetManager.INVALID_APPWIDGET_ID);

            Integer id = Integer.valueOf(0);
            Integer page = Integer.valueOf(0);
            if (intent.getAction().equals(ACTION_CHECK)) {
                id = extras.getInt("id", 0);
            } else if (intent.getAction().equals(ACTION_NEXT_PAGE)) {
                page = 1;
            } else if (intent.getAction().equals(ACTION_PREV_PAGE)) {
                page = -1;
            }

            SharedPreferences sharedPreferences = context.getSharedPreferences(
                    PREFS, 0);

            if (page != 0) {
                int pagePreference = sharedPreferences.getInt(
                        widgetId + "Page", 0);

                if (page == -1 && pagePreference != 0) {
                    pagePreference--;
                } else if (page == 1) {
                    pagePreference++;
                }

                SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences
                        .edit();
                sharedPreferencesEditor.putInt(widgetId + "Page",
                        pagePreference);
                sharedPreferencesEditor.commit();
            }

            if (id != 0) {
                ContentValues values = new ContentValues();
                values.put(ShoppingContract.Contains.STATUS,
                        ShoppingContract.Status.BOUGHT);
                context.getContentResolver().update(
                        Uri.withAppendedPath(
                                ShoppingContract.Contains.CONTENT_URI,
                                String.valueOf(id)), values, null, null
                );
            }
        }
        updateWidgets(context);
    }

    private void updateWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager
                .getInstance(context);
        int[] a = appWidgetManager.getAppWidgetIds(new ComponentName(context
                .getPackageName(), CheckItemsWidget.class.getName()));
        List<AppWidgetProviderInfo> b = appWidgetManager
                .getInstalledProviders();
        for (AppWidgetProviderInfo i : b) {
            if (i.provider.getPackageName().equals(context.getPackageName())) {
                a = appWidgetManager.getAppWidgetIds(i.provider);
                new CheckItemsWidget().onUpdate(context, appWidgetManager, a);
            }
        }
    }

    @Override
    public void onUpdate(final Context context,
                         AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(
                PREFS, 0);

        for (int widgetId : appWidgetIds) {
            long listId = sharedPreferences.getLong(String.valueOf(widgetId),
                    -1);
            int page = sharedPreferences.getInt(widgetId + "Page", 0);

            if (listId != -1) {
                RemoteViews updateView = buildUpdate(context, listId, widgetId,
                        page);
                appWidgetManager.updateAppWidget(widgetId, updateView);
            }
        }
    }

    public RemoteViews buildUpdate(Context context, long listId, int widgetId,
                                   int page) {
        RemoteViews views = new RemoteViews(context.getPackageName(),
                R.layout.widget_check_items);

        Cursor cursor = fillItems(context, listId);

        // Clean all text views
        // Need for correct update
        for (int i = 1; i <= LIMIT_ITEMS; i++) {
            int viewId = context.getResources().getIdentifier("item_" + i,
                    "id", context.getPackageName());
            views.setTextViewText(viewId, "");
        }

        views.setTextViewText(R.id.item_1,
                context.getString(R.string.widget_no_items, page + 1));

        if (cursor.getCount() > 0) {
            int i = 1;

            cursor.moveToPosition(page * LIMIT_ITEMS - 1);

            while (cursor.moveToNext()) {
                if (i > LIMIT_ITEMS) {
                    break;
                }

                int viewId = context.getResources().getIdentifier("item_" + i,
                        "id", context.getPackageName());
                views.setTextViewText(viewId, cursor.getString(cursor
                        .getColumnIndex(ContainsFull.ITEM_NAME)));

                Intent intentCheckService = new Intent(context,
                        CheckItemsWidget.class);
                intentCheckService.putExtra("widgetId", widgetId);
                intentCheckService.putExtra("id",
                        Integer.valueOf(cursor.getString(0)));
                intentCheckService.setAction(ACTION_CHECK);

                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        context, Integer.valueOf(cursor.getString(0)),
                        intentCheckService, PendingIntent.FLAG_ONE_SHOT);
                views.setOnClickPendingIntent(viewId, pendingIntent);

                i++;
            }
            /*
             * Icon
             */
            Intent intentGoToApp = new Intent(context, ShoppingActivity.class);
            intentGoToApp.setAction(Intent.ACTION_VIEW);
            intentGoToApp.setData(Uri.withAppendedPath(
                    ShoppingContract.Lists.CONTENT_URI, Long.toString(listId)));
            PendingIntent pendingIntentGoToApp = PendingIntent.getActivity(
                    context, 0, intentGoToApp,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            /*
             * List title
             */
            String title = getTitle(
                    context,
                    Uri.withAppendedPath(ShoppingContract.Lists.CONTENT_URI,
                            Long.toString(listId))
            );
            views.setTextViewText(R.id.list_name, title);
            views.setOnClickPendingIntent(R.id.list_name, pendingIntentGoToApp);
            views.setOnClickPendingIntent(R.id.button_go_to_app,
                    pendingIntentGoToApp);

            /*
             * Preference button
             */
            Intent intentPreferences = new Intent(context,
                    CheckItemsWidgetConfig.class);
            intentPreferences.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    widgetId);
            intentPreferences.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
            PendingIntent pendingIntentPreferences = PendingIntent.getActivity(
                    context, 0, intentPreferences,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.button_go_to_preferences,
                    pendingIntentPreferences);

            /*
             * Prev page
             */
            Intent intentPrevPage = new Intent(context, CheckItemsWidget.class);
            intentPrevPage.setAction(ACTION_PREV_PAGE);
            intentPrevPage.putExtra("widgetId", widgetId);
            PendingIntent pendingIntentPrevPage = PendingIntent.getBroadcast(
                    context, widgetId, intentPrevPage,
                    PendingIntent.FLAG_ONE_SHOT);
            views.setOnClickPendingIntent(R.id.button_prev,
                    pendingIntentPrevPage);

            /*
             * Next page
             */
            Intent intentNextPage = new Intent(context, CheckItemsWidget.class);
            intentNextPage.setAction(ACTION_NEXT_PAGE);
            intentNextPage.putExtra("widgetId", widgetId);
            PendingIntent pendingIntentNextPage = PendingIntent.getBroadcast(
                    context, widgetId, intentNextPage,
                    PendingIntent.FLAG_ONE_SHOT);
            views.setOnClickPendingIntent(R.id.button_next,
                    pendingIntentNextPage);
        }

        if (cursor != null) {
            cursor.deactivate();
            cursor.close();
        }

        return views;
    }

    /*
     * Get from ShoppingListsActivity class
     */
    private String getTitle(Context context, Uri uri) {
        Cursor c = context.getContentResolver().query(uri,
                new String[]{ShoppingContract.Lists.NAME}, null, null, null);
        if (c != null && c.moveToFirst()) {
            String title = c.getString(0);
            c.deactivate();
            c.close();
            return title;
        }
        if (c != null) {
            c.deactivate();
            c.close();
        }

        // If there was a problem retrieving the note title
        // simply use the application name
        return context.getString(R.string.app_name);
    }

}

import android.app.ListActivity;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import org.openintents.shopping.R;
import org.openintents.shopping.library.provider.ShoppingContract;
import org.openintents.shopping.library.provider.ShoppingContract.Lists;
import org.openintents.shopping.ui.PreferenceActivity;

import java.util.List;

public class CheckItemsWidgetConfig extends ListActivity {
    private final static String PREFS = "check_items_widget";
    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setResult(RESULT_CANCELED);

        Cursor cursor = managedQuery(ShoppingContract.Lists.CONTENT_URI,
                new String[]{Lists._ID, Lists.NAME}, null, null,
                PreferenceActivity.getShoppingListSortOrderFromPrefs(this));
        setListAdapter(new SimpleCursorAdapter(this,
                android.R.layout.simple_list_item_1, cursor,
                new String[]{Lists.NAME}, new int[]{android.R.id.text1}));

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }

        setTitle(R.string.widget_choose_a_list);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS, 0);
        SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences
                .edit();
        sharedPreferencesEditor.putLong(String.valueOf(mAppWidgetId), id);
        sharedPreferencesEditor.commit();

        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_OK, resultValue);

        updateWidgets();

        finish();
    }

    private void updateWidgets() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int[] a = appWidgetManager.getAppWidgetIds(new ComponentName(
                getPackageName(), CheckItemsWidget.class.getName()));
        List<AppWidgetProviderInfo> b = appWidgetManager
                .getInstalledProviders();
        for (AppWidgetProviderInfo i : b) {
            if (i.provider.getPackageName().equals(getPackageName())) {
                a = appWidgetManager.getAppWidgetIds(i.provider);
                new CheckItemsWidget().onUpdate(this, appWidgetManager, a);
            }
        }
    }

}

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import org.openintents.intents.AutomationIntents;
import org.openintents.intents.ShoppingListIntents;
import org.openintents.shopping.LogConstants;
import org.openintents.shopping.R;
import org.openintents.shopping.library.provider.ShoppingContract;

public class EditAutomationActivity extends Activity {

    private static final String TAG = LogConstants.TAG;
    private static final boolean debug = false || LogConstants.debug;

    private static final int REQUEST_CODE_PICK_LIST = 1;

    private static final String BUNDLE_ACTION = "action";
    private static final String BUNDLE_LIST_URI = "list";

    private TextView mTextCommand;
    // TextView mTextSelectAction;
    // TextView mTextSelectCountdown;
    private Spinner mSpinnerAction;
    private Button mButtonOk;
    private Button mButtonCountdown;

    private String mDescriptionAction;
    private String mDescriptionShoppingList;

    private Uri mUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_edit_automation);

        mUri = null;
        mDescriptionShoppingList = "?";

        mSpinnerAction = (Spinner) findViewById(R.id.spinner_action);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.automation_actions,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerAction.setAdapter(adapter);

        mSpinnerAction
                .setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(AdapterView<?> parent,
                                               View view, int position, long id) {
                        mDescriptionAction = getResources().getStringArray(
                                R.array.automation_actions)[position];

                        updateTextViews();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }

                });

        mButtonCountdown = (Button) findViewById(R.id.button_list);
        mButtonCountdown.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                pickList();
            }

        });

        mButtonOk = (Button) findViewById(R.id.button_ok);
        mButtonOk.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                doOk();
            }

        });

        mButtonOk.setEnabled(false);

        Button b = (Button) findViewById(R.id.button_cancel);
        b.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                doCancel();
            }

        });

        mTextCommand = (TextView) findViewById(R.id.command);
        // mTextSelectAction = (TextView) findViewById(R.id.select_action);
        // mTextSelectCountdown = (TextView)
        // findViewById(R.id.select_countdown);

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(BUNDLE_ACTION)) {
                int i = savedInstanceState.getInt(BUNDLE_ACTION);
                mSpinnerAction.setSelection(i);
            }
            if (savedInstanceState.containsKey(BUNDLE_LIST_URI)) {
                mUri = Uri.parse(savedInstanceState.getString(BUNDLE_LIST_URI));
                setListNameFromUri();
            }
        } else {
            final Intent intent = getIntent();

            if (intent != null) {
                String action = intent
                        .getStringExtra(ShoppingListIntents.EXTRA_ACTION);

                if (ShoppingListIntents.TASK_CLEAN_UP_LIST.equals(action)) {
                    mSpinnerAction.setSelection(0);
                } else {
                    // set default
                    mSpinnerAction.setSelection(0);
                }

                // Get list:

                final String dataString = intent
                        .getStringExtra(ShoppingListIntents.EXTRA_DATA);
                if (dataString != null) {
                    mUri = Uri.parse(dataString);
                }
                setListNameFromUri();
            }
        }

        updateTextViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (debug) {
            Log.i(TAG, "onPause");
        }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(BUNDLE_ACTION, mSpinnerAction.getSelectedItemPosition());
        if (mUri != null) {
            outState.putString(BUNDLE_LIST_URI, mUri.toString());
        }
    }

    void pickList() {
        Intent i = new Intent(Intent.ACTION_PICK);
        i.setData(ShoppingContract.Lists.CONTENT_URI);

        startActivityForResult(i, REQUEST_CODE_PICK_LIST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == REQUEST_CODE_PICK_LIST && resultCode == RESULT_OK) {
            mUri = intent.getData();

            setListNameFromUri();
        }

        updateTextViews();
    }

    private void setListNameFromUri() {
        mDescriptionShoppingList = "";

        if (mUri != null) {
            mButtonOk.setEnabled(true);

            // Get name of list from content provider
            Cursor c = getContentResolver().query(
                    mUri,
                    new String[]{ShoppingContract.Lists._ID,
                            ShoppingContract.Lists.NAME}, null, null, null
            );

            if (c != null && c.moveToFirst()) {
                mDescriptionShoppingList = c.getString(1);
            }

            c.close();
        }

        if (TextUtils.isEmpty(mDescriptionShoppingList)) {
            mDescriptionShoppingList = getString(android.R.string.untitled);
        }
    }

    void doOk() {
        updateResult();
        finish();
    }

    void doCancel() {
        setResult(RESULT_CANCELED);
        finish();
    }

    void updateResult() {
        Intent intent = new Intent();

        long id = mSpinnerAction.getSelectedItemId();
        if (id == 0) {
            intent.putExtra(ShoppingListIntents.EXTRA_ACTION,
                    ShoppingListIntents.TASK_CLEAN_UP_LIST);
        }
        intent.putExtra(ShoppingListIntents.EXTRA_DATA, mUri.toString());

        String description = mDescriptionAction + ": "
                + mDescriptionShoppingList;
        intent.putExtra(AutomationIntents.EXTRA_DESCRIPTION, description);

        if (debug) {
            Log.i(TAG, "Created intent (URI)   : " + intent.toURI());
        }
        if (debug) {
            Log.i(TAG, "Created intent (String): " + intent.toString());
        }

        setResult(RESULT_OK, intent);
    }

    void updateTextViews() {
        mTextCommand.setText(mDescriptionAction + ": "
                + mDescriptionShoppingList);
        // mTextSelectAction.setText(getString(R.string.select_action,
        // mDescriptionAction));
        // mTextSelectCountdown.setText(getString(R.string.select_countdown,
        // mDescriptionCountdown));
        // mTextSelectAction.setText(getString(R.string.select_action, ""));
        // mTextSelectCountdown.setText(getString(R.string.select_countdown,
        // ""));
        mButtonCountdown.setText(mDescriptionShoppingList);
    }
}


import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

import org.openintents.shopping.library.provider.ShoppingContract;
import org.openintents.shopping.library.provider.ShoppingContract.Contains;
import org.openintents.shopping.library.provider.ShoppingContract.Status;

public class AutomationActions {

    public static void cleanUpList(Context context, Uri uri) {

        if (uri != null) {
            long id = Integer.parseInt(uri.getLastPathSegment());

            // by changing state
            ContentValues values = new ContentValues();
            values.put(Contains.STATUS, Status.REMOVED_FROM_LIST);
            context.getContentResolver().update(
                    Contains.CONTENT_URI,
                    values,
                    ShoppingContract.Contains.LIST_ID + " = " + id + " AND "
                            + ShoppingContract.Contains.STATUS + " = "
                            + ShoppingContract.Status.BOUGHT, null
            );
        }
    }

}


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import org.openintents.intents.ShoppingListIntents;
import org.openintents.shopping.LogConstants;

public class AutomationReceiver extends BroadcastReceiver {

    private final static String TAG = "AutomationReceiver";
    private final static boolean debug = false || LogConstants.debug;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (debug) {
            Log.i(TAG, "Receive intent: " + intent.toString());
        }

        final String action = intent
                .getStringExtra(ShoppingListIntents.EXTRA_ACTION);
        final String dataString = intent
                .getStringExtra(ShoppingListIntents.EXTRA_DATA);
        Uri data = null;
        if (dataString != null) {
            data = Uri.parse(dataString);
        }
        if (debug) {
            Log.i(TAG, "action: " + action + ", data: " + dataString);
        }

        if (ShoppingListIntents.TASK_CLEAN_UP_LIST.equals(action)) {
            // Clean up list.
            if (data != null) {
                if (debug) {
                    Log.i(TAG, "Clean up list " + data);
                }
                AutomationActions.cleanUpList(context, data);
            }
        }

    }

}

/*
 * Copyright (C) 2007-2008 OpenIntents.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * This code is based on Android's API demos.
 */


import android.content.ContentResolver;
import android.content.Context;

import org.openintents.shopping.library.provider.ShoppingContract;
import org.openintents.shopping.library.provider.ShoppingContract.ContainsFull;

/**
 * Handles receiving information about changes in shared shopping lists.
 */
public class GTalkReceiver /* extends IntentReceiver */ {
    /**
     * Tag for log.
     */
    private static final String TAG = "GTalkReceiver";

    /**
     * Array of items for editing. This defines the projection for the table
     * Lists.
     */
    private static final String[] mProjectionLists = new String[]{
            ShoppingContract.Lists._ID, ShoppingContract.Lists.NAME,
            ShoppingContract.Lists.SHARE_NAME,
            ShoppingContract.Lists.SHARE_CONTACTS};

    /**
     * Index of ID in the Projection for Lists
     */
    private static final int mProjectionListsID = 0;
    private static final int mProjectionListsNAME = 1;
    private static final int mProjectionListsSHARENAME = 2;
    private static final int mProjectionListsSHARECONTACTS = 3;

    /**
     * Array of items for editing. This defines the projection for the table
     * ContainsFull.
     */
    private static final String[] mProjectionContainsFull = new String[]{
            ContainsFull._ID, ContainsFull.ITEM_NAME, ContainsFull.ITEM_IMAGE,
            ContainsFull.STATUS, ContainsFull.ITEM_ID,
            ContainsFull.SHARE_CREATED_BY, ContainsFull.SHARE_MODIFIED_BY};
    private static final int mProjectionContainsFullCONTAINSID = 0;
    private static final int mProjectionContainsFullITEMNAME = 1;
    private static final int mProjectionContainsFullITEMIMAGE = 2;
    private static final int mProjectionContainsFullSTATUS = 3;
    private static final int mProjectionContainsFullITEMID = 4;
    private static final int mProjectionContainsFullSHARECREATEDBY = 5;
    private static final int mProjectionContainsFullSHAREMODIFIEDBY = 6;

    private Context mContext;
    private ContentResolver mContentResolver;
    /*
     * public void onReceiveIntent(Context context, Intent intent) { mContext =
     * context; mContentResolver = mContext.getContentResolver(); String action
     * = intent.getAction(); Uri data = intent.getData(); Bundle bundle =
     * intent.getExtras();
     *
     * if (data == null) { // Here we have to work around an GTalk issue in
     * Android m5-rc14/15: // GTalk does not send data, so we send them in the
     * bundle: if (bundle != null) { data =
     * Uri.parse(bundle.getString(GTalkSender.DATA)); } else { Log.i(TAG,
     * "IntentReceiver: Received neither data nor bundle."); return; } }
     *
     * Log.i(TAG, "Received intent " + action + ", data " + data.toString());
     *
     * if (data.equals(Shopping.Lists.CONTENT_URI)) { if
     * (action.equals(OpenIntents.SHARE_UPDATE_ACTION)) { // Update on a
     * shopping list updateList(bundle); return; } } else if
     * (data.equals(Shopping.Items.CONTENT_URI)) { if
     * (action.equals(OpenIntents.SHARE_INSERT_ACTION)) { // Insert a new item
     * insertItem(bundle); return; } else if
     * (action.equals(OpenIntents.SHARE_UPDATE_ACTION)) { // Update an item
     * updateItem(bundle); return; } }
     *
     * }
     */
    /**
     * Updates shared list information or creates a new list.
     *
     * If the shared list does not exist yet, a new list is created.
     *
     * @param bundle
     */
    /*
     * void updateList(Bundle bundle) { // Update information about list: if
     * (bundle != null) { String shareListName =
     * bundle.getString(Shopping.Lists.SHARE_NAME); String shareContacts =
     * bundle.getString(Shopping.Lists.SHARE_CONTACTS);
     *
     * if (shareListName == null) { Log.e(TAG,
     * "Bundle received is incomplete: shareListName is null."); return; } if
     * (shareContacts == null) { Log.e(TAG,
     * "Bundle received is incomplete: shareContacts is null."); return; }
     *
     * // Get unique list identifier // Get a cursor for all items that are
     * contained // in currently selected shopping list. Cursor c =
     * mContentResolver.query( Shopping.Lists.CONTENT_URI, mProjectionLists,
     * Shopping.Lists.SHARE_NAME + " = '" + shareListName + "'", null,
     * Shopping.Lists.DEFAULT_SORT_ORDER);
     *
     * if (c == null || c.count() < 1) { // List does not exist yet: // Create
     * it first. // Add item to list: ContentValues values = new
     * ContentValues(2);
     *
     * // Let us use the share name as default name
     * values.put(Shopping.Lists.NAME, shareListName);
     * values.put(Shopping.Lists.SHARE_NAME, shareListName);
     *
     * // The sender is responsible that the contacts list // we receive // (1)
     * contains the sender's email, // (2) does not contain our email.
     * values.put(Shopping.Lists.SHARE_CONTACTS, shareContacts);
     *
     * try { Uri uri = mContentResolver.insert(Lists.CONTENT_URI, values);
     * Log.i(TAG, "Insert new list: " + uri);
     *
     * Toast.makeText(mContext, "Received new shopping list: " + shareListName,
     * Toast.LENGTH_LONG).show(); } catch (Exception e) { Log.i(TAG,
     * "insert list failed", e); } } else { // List exists, let us just update
     * email contacts c.first(); c.updateString(mProjectionListsSHARECONTACTS,
     * shareContacts);
     *
     * c.commitUpdates();
     *
     * c.requery(); }
     *
     * // Finally send notification that data changed:
     * mContext.broadcastIntent(new Intent(OpenIntents.REFRESH_ACTION));
     *
     * } else { Log.e(TAG, "Bundle received is null"); } }
     */

    /**
     * Inserts an item into a list.
     *
     * @param bundle
     */
    /*
     * void insertItem(Bundle bundle) { // Update information about list: if
     * (bundle != null) { String shareListName =
     * bundle.getString(Shopping.Lists.SHARE_NAME); String itemName =
     * bundle.getString(Shopping.Items.NAME);
     *
     * if (shareListName == null) { Log.e(TAG,
     * "Bundle received is incomplete: shareListName is null."); return; } if
     * (itemName == null) { Log.e(TAG,
     * "Bundle received is incomplete: shareContacts is null."); return; }
     *
     * // Get unique list identifier // Get a cursor for all items that are
     * contained // in currently selected shopping list. Cursor c =
     * mContentResolver.query( Shopping.Lists.CONTENT_URI, mProjectionLists,
     * Shopping.Lists.SHARE_NAME + " = '" + shareListName + "'", null,
     * Shopping.Lists.DEFAULT_SORT_ORDER);
     *
     * if (c == null || c.count() < 1) { // List does not exist:
     *
     * Log.i(TAG, "insertItem: Received item for list that does not exist");
     *
     * // TODO: Ask user what to do about the item: // Either demand new list +
     * synchronization, or drop item // OR: Automatically create the list with
     * minimum entries } else { // List exists, let us insert item c.first();
     * long listId = c.getLong(mProjectionListsID); long itemId =
     * Shopping.getItem(itemName);
     *
     * Shopping.addItemToList(itemId, listId); }
     *
     *
     * // Finally send notification that data changed:
     * mContext.broadcastIntent(new Intent(OpenIntents.REFRESH_ACTION));
     *
     * } else { Log.e(TAG, "Bundle received is null"); } }
     */

    /**
     * Updates information about an item in a list.
     *
     * If the item does not exist yet, it is created. Update could include to
     * strike an item through.
     *
     * @param bundle
     */
    /*
     * void updateItem(Bundle bundle) { // Update information about list: if
     * (bundle != null) { String shareListName =
     * bundle.getString(Shopping.Lists.SHARE_NAME); String itemNameOld =
     * bundle.getString(Shopping.Items.NAME + GTalkSender.OLD); String itemName
     * = bundle.getString(Shopping.Items.NAME); // TODO: In m5, Android only
     * supports Strings in bundles for GTalk String itemStatusOld =
     * bundle.getString(Shopping.Contains.STATUS + GTalkSender.OLD); String
     * itemStatus = bundle.getString(Shopping.Contains.STATUS); String
     * itemSender = bundle.getString(GTalkSender.SENDER);
     *
     * if (shareListName == null) { Log.e(TAG,
     * "Bundle received is incomplete: shareListName is null."); return; } if
     * (itemName == null) { Log.e(TAG,
     * "Bundle received is incomplete: shareContacts is null."); return; } if
     * (itemNameOld == null || itemStatusOld == null || itemStatus == null ||
     * itemSender == null) { Log.e(TAG,
     * "Bundle received is incomplete: at least one item missing."); return; }
     *
     * // Get a cursor for shared list. Cursor c = mContentResolver.query(
     * Shopping.Lists.CONTENT_URI, mProjectionLists, Shopping.Lists.SHARE_NAME +
     * " = '" + shareListName + "'", null, Shopping.Lists.DEFAULT_SORT_ORDER);
     *
     * if (c == null || c.count() < 1) { // List does not exist:
     *
     * Log.i(TAG, "insertItem: Received item for list that does not exist");
     *
     * // TODO: Ask user what to do about the item: // Either demand new list +
     * synchronization, or drop item // OR: Automatically create the list with
     * minimum entries } else { // List exists, let us update item c.first();
     * long listId = c.getLong(mProjectionListsID);
     *
     * Log.i(TAG, "Query: item name = " + itemNameOld + ", status = " +
     * itemStatusOld);
     *
     * // Now look for item with old name and old status: Cursor citem =
     * mContentResolver.query( Shopping.ContainsFull.CONTENT_URI,
     * mProjectionContainsFull, Shopping.ContainsFull.LIST_ID + " = '" + listId
     * + "'" + " AND " + Shopping.ContainsFull.ITEM_NAME + " = '" + itemNameOld
     * + "'" + " AND " + Shopping.ContainsFull.STATUS + " = '" + itemStatusOld +
     * "'", null, null);
     *
     * if (citem == null || citem.count() < 1) { // Item with same name and same
     * status does not exist. // Let us see if there exists one with same name
     * at least. // The assumption is that probably "status" is out of sync, //
     * and we want to avoid having duplicates.
     *
     * // (Later, there should really be a hidden Item ID that is // unique so
     * that these issues of duplicates can not arise. Log.i(TAG,
     * "Re-Query: item name = " + itemNameOld);
     *
     * citem = mContentResolver.query( Shopping.ContainsFull.CONTENT_URI,
     * mProjectionContainsFull, Shopping.ContainsFull.LIST_ID + " = '" + listId
     * + "'" + " AND " + Shopping.ContainsFull.ITEM_NAME + " = '" + itemNameOld
     * + "'", null, null); }
     *
     * if (citem == null || citem.count() < 1) { // Item does not exist - we
     * will insert (create) it:
     *
     * Log.i(TAG,
     * "insertItem: Received update for item that does not exist - inserting item"
     * );
     *
     * long itemId = Shopping.getItem(itemName);
     *
     * // Add item to list: ContentValues values = new ContentValues(2);
     * values.put(Contains.ITEM_ID, itemId); values.put(Contains.LIST_ID,
     * listId); values.put(Contains.STATUS, Long.parseLong(itemStatus));
     * values.put(Contains.SHARE_MODIFIED_BY, itemSender); try { Uri uri =
     * mContentResolver.insert(Contains.CONTENT_URI, values); Log.i(TAG,
     * "Insert new entry in 'contains': " + uri); // return
     * Long.parseLong(uri.getPathSegments().get(1)); } catch (Exception e) {
     * Log.i(TAG, "insert into table 'contains' failed", e); // return -1; } }
     * else { // Item exists, let us update citem.first();
     *
     * long itemId = citem.getLong(mProjectionContainsFullITEMID); long
     * containsId = citem.getLong(mProjectionContainsFullCONTAINSID);
     *
     * Uri itemUri = Uri.withAppendedPath(Shopping.Items.CONTENT_URI, "" +
     * itemId); ContentValues values = new ContentValues(1);
     * values.put(Shopping.Items.NAME, itemName);
     * mContentResolver.update(itemUri, values, null, null);
     *
     * Uri containsUri = Uri.withAppendedPath(Shopping.Contains.CONTENT_URI, ""
     * + containsId); values = new ContentValues(2);
     * values.put(Shopping.Contains.STATUS, Long.parseLong(itemStatus));
     * values.put(Shopping.Contains.SHARE_MODIFIED_BY, itemSender);
     * mContentResolver.update(containsUri, values, null, null);
     *
     * } }
     *
     * // Finally send notification that data changed:
     * mContext.broadcastIntent(new Intent(OpenIntents.REFRESH_ACTION));
     *
     * } else { Log.e(TAG, "Bundle received is null"); } }
     */

}

/*
 * Copyright (C) 2007-2008 OpenIntents.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.openintents.shopping.R;
import org.openintents.shopping.library.provider.ShoppingContract;
import org.openintents.shopping.library.provider.ShoppingContract.Lists;

/**
 * Allows to edit the share settings for a shopping list.
 */
public class ListShareSettingsActivity extends Activity {
    /**
     * TAG for logging.
     */
    private static final String TAG = "ListShareSettings";

    /**
     * Array of items we need to edit. This defines the projection for the table
     * Lists.
     */
    private static final String[] mProjectionLists = new String[]{
            ShoppingContract.Lists._ID, ShoppingContract.Lists.NAME,
            ShoppingContract.Lists.SHARE_NAME,
            ShoppingContract.Lists.SHARE_CONTACTS};

    /**
     * Index of ID in the Projection for Lists
     */
    private static final int mProjectionListsID = 0;
    private static final int mProjectionListsNAME = 1;
    private static final int mProjectionListsSHARENAME = 2;
    private static final int mProjectionListsSHARECONTACTS = 3;

    /**
     * Cursor for access to the list.
     */
    private Cursor mCursor;

    /**
     * The EditText containing the unique shared list name.
     */
    private EditText mShareName;

    /**
     * The EditText containing the contacts.
     */
    private EditText mContacts;

    private Uri mUri;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.activity_list_share_settings);

        // Get the uri of the list
        mUri = getIntent().getData();

        // Get a cursor to access the note
        mCursor = managedQuery(mUri, mProjectionLists, null, null, null);

        // Set up click handlers for the text field and button
        mContacts = (EditText) this.findViewById(R.id.contacts);
        // mText.setOnClickListener(this);
        mShareName = (EditText) this.findViewById(R.id.share_name);

        // Button b = (Button) findViewById(R.id.ok);
        // b.setOnClickListener(this);

        Button bOk = (Button) this.findViewById(R.id.ok);
        bOk.setOnClickListener(new OnClickListener() {
            public void onClick(final View v) {
                pressOK();
            }
        });

        Button bCancel = (Button) this.findViewById(R.id.cancel);
        bCancel.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        // Initialize the text with the title column from the cursor
        if (mCursor != null) {
            mCursor.moveToFirst();
            String sn = mCursor.getString(mProjectionListsSHARENAME);
            mShareName.setText(sn);
            String contacts = mCursor.getString(mProjectionListsSHARECONTACTS);
            mContacts.setText(contacts);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // TODO Here we should store temporary information

    }

    void pressOK() {
        String sharename = mShareName.getText().toString();
        String contacts = mContacts.getText().toString();

        if (!contacts.equals("") && sharename.equals("")) {
            mShareName.requestFocus();
            Toast.makeText(this, getString(R.string.please_enter_description),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Write the text back into the cursor
        if (mCursor != null) {
            ContentValues values = new ContentValues();
            values.put(Lists.SHARE_NAME, sharename);
            values.put(Lists.SHARE_CONTACTS, contacts);
            getContentResolver().update(mUri, values, "_id = ?",
                    new String[]{mCursor.getString(0)});

        }

        // Broadcast the information to peers:
        // Should be done in the calling activity.
        Bundle bundle = new Bundle();
        bundle.putString(ShoppingContract.Lists.SHARE_NAME, sharename);
        bundle.putString(ShoppingContract.Lists.SHARE_CONTACTS, contacts);

        /*
         * setResult(RESULT_OK, mUri.toString(), bundle);
         */
        // TODO ??? OK???
        setResult(RESULT_OK);

        // Log.i(TAG, "call finish()");
        finish();
        // Log.i(TAG, "called finish()");

        // setResult(RESULT_OK, mUri.toString());
        Log.i(TAG, "Sending bundle: sharename: " + sharename + ", contacts: "
                + contacts);

        // Log.i(TAG, "Return RESULT_OK");

    }

    /*
     * public void onClick(View v) { // When the user clicks, just finish this
     * activity. // onPause will be called, and we save our data there.
     * finish(); }
     */
}

/*
 * Copyright (C) 2007-2008 OpenIntents.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * This code is based on Android's API demos.
 */


import android.content.Context;
import android.text.TextUtils;
import android.widget.Toast;

/**
 * Handles sending out information about changes in shared shopping lists.
 */
public class GTalkSender {
    /**
     * Suffix for bundle items to mark them old.
     * <p/>
     * For the update version, both, the old and the new value are sent. Old
     * values are marked with this suffix.
     */
    public static final String OLD = "_old";
    /**
     * Bundle marker for sender.
     */
    public static final String SENDER = "sender";

    // ??? IGTalkSession mGTalkSession = null;
    /**
     * Bundle marker for data (content URI).
     * <p/>
     * This is only necessary for the Anroid m5 issue that data is not sent
     * along with a GTalk message.
     */
    public static final String DATA = "data";
    private static final String TAG = "GTalkSender";
    private Context mContext;
    private boolean mBound;

    /**
     * Constructs a new sender GTalk. You have to manually bind before using
     * GTalk.
     *
     * @param mContext
     */
    public GTalkSender(Context context) {
        mContext = context;
        mBound = false;

        // bindGTalkService();
    }

    /**
     * Bind to GTalk service.
     */
    /*
     * public void bindGTalkService() { if (!mBound) { Intent intent = new
     * Intent(); intent.setComponent(
     * com.google.android.gtalkservice.GTalkServiceConstants
     * .GTALK_SERVICE_COMPONENT); mContext.bindService(intent, mConnection, 0);
     * mBound = true; } else { // already bound - do nothing. } }
     */

    /*
     * public void unbindGTalkService() { if (mBound) {
     * mContext.unbindService(mConnection); mBound = false; } else { // have not
     * been bound - do nothing. } }
     */
    /*
     * ??? private ServiceConnection mConnection = new ServiceConnection() {
     * public void onServiceConnected(ComponentName className, IBinder service)
     * { // This is called when the connection with the GTalkService has been //
     * established, giving us the service object we can use to // interact with
     * the service. We are communicating with our // service through an IDL
     * interface, so get a client-side // representation of that from the raw
     * service object. IGTalkService GTalkService =
     * IGTalkService.Stub.asInterface(service);
     *
     * try { mGTalkSession = GTalkService.getDefaultSession();
     *
     * if (mGTalkSession == null) { // this should not happen.
     * //showMessage(mContext.getText(R.string.gtalk_session_not_found));
     * showMessage(mContext.getText(R.string.gtalk_not_connected)); return; } }
     * catch (DeadObjectException ex) { Log.e(TAG, "caught " + ex);
     * showMessage(mContext.getText(R.string.gtalk_found_stale_service)); } }
     *
     * public void onServiceDisconnected(ComponentName className) { // This is
     * called when the connection with the service has been // unexpectedly
     * disconnected -- that is, its process crashed. mGTalkSession = null; } };
     */
    private boolean isValidUsername(String username) {
        if (TextUtils.isEmpty(username)) {
            return false;
        }

        return username.indexOf('@') != -1;

    }

    private void showMessage(CharSequence msg) {
        Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
    }

    // ////////////////////////////////////////////////////
    // Shopping related methods follow

    /**
     * Sends updated list and email information to all recipients.
     *
     * "local/[id]" refers to a local shopping list.
     */
    /*
     * public void sendList (String recipients, String shareListName) {
     * Log.i(TAG, "sendList(" + recipients + ", " + shareListName + ")");
     *
     * // First take out white spaces String r = recipients.replace(" ", ""); //
     * Then convert to list String[] recipientList = r.split(",");
     *
     * int max = recipientList.length; if (max == 1 &&
     * recipientList[0].equals("")) { // this is an empty list - nothing to
     * send: return; } for (int i = 0; i < max; i++) { String recipient =
     * recipientList[i];
     *
     * // Let us construct the recipient list without // the current recipient
     * StringBuilder modifiedRecipientList = new StringBuilder(); for (int j=0;
     * j < max; j++) { if (j != i) { // Note, we start with ',' but this will be
     * // prepended by the sending user data modifiedRecipientList.append(",");
     * modifiedRecipientList.append(recipientList[j]); } }
     *
     * if (recipient.startsWith("local/")) { Log.i(TAG, "local recipient: " +
     * recipient); // Recipient is local address
     *
     * // Prepend the modified recipient list by information // about the sender
     * modifiedRecipientList.insert(0, "local/" + shareListName);
     *
     * // If we have a local list, we map the shareListName to a // new name so
     * that we can synchronize two different local // lists. String
     * newShareListName = recipient.substring("local/".length());
     *
     * Intent intent = new Intent(OpenIntents.SHARE_UPDATE_ACTION,
     * Shopping.Lists.CONTENT_URI); intent.putExtra(Shopping.Lists.SHARE_NAME,
     * newShareListName); intent.putExtra(Shopping.Lists.SHARE_CONTACTS,
     * modifiedRecipientList.toString());
     *
     * mContext.broadcastIntent(intent);
     *
     * } else { Log.i(TAG, "remote recipient: " + recipient);
     *
     * // Recipient is remote address if (mGTalkSession != null) { try {
     *
     *
     *
     *
     * // Prepend the modified recipient list by information // about the sender
     * modifiedRecipientList.insert(0, mGTalkSession.getJid()); // or
     * getUsername() ?
     *
     * //Intent intent = new Intent(OpenIntents.SHARE_UPDATE_ACTION, //
     * Shopping.Lists.CONTENT_URI);
     *
     * // workaround for Anroid m5 issue: send content URI in bundle. Intent
     * intent = new Intent(OpenIntents.SHARE_UPDATE_ACTION);
     * intent.putExtra(DATA, Shopping.Lists.CONTENT_URI.toString());
     *
     * intent.putExtra(Shopping.Lists.SHARE_NAME, shareListName);
     * intent.putExtra(Shopping.Lists.SHARE_CONTACTS,
     * modifiedRecipientList.toString());
     *
     * mGTalkSession.sendDataMessage(recipient, intent); } catch
     * (DeadObjectException ex) { Log.e(TAG, "caught " + ex);
     * showMessage(mContext.getText(R.string.gtalk_found_stale_service));
     * mGTalkSession = null; bindGTalkService(); } } else {
     * //showMessage(mContext.getText(R.string.gtalk_service_not_connected));
     * showMessage(mContext.getText(R.string.gtalk_not_connected)); return; }
     *
     * } }
     *
     * }
     */
    /**
     * Sends information about a new item to all recipients.
     */
    /*
     * public void sendItem(String recipients, String shareListName, String
     * itemName) { Log.i(TAG, "sendItem(" + recipients + ", " + shareListName +
     * ", " + itemName + ")");
     *
     * // First take out white spaces String r = recipients.replace(" ", ""); //
     * Then convert to list String[] recipientList = r.split(",");
     *
     * int max = recipientList.length; if (max == 1 &&
     * recipientList[0].equals("")) { // this is an empty list - nothing to
     * send: return; } for (int i = 0; i < max; i++) { String recipient =
     * recipientList[i];
     *
     * if (recipient.startsWith("local/")) { Log.i(TAG, "local recipient: " +
     * recipient); // Recipient is local address
     *
     * // If we have a local list, we map the shareListName to a // new name so
     * that we can synchronize two different local // lists. String
     * newShareListName = recipient.substring("local/".length());
     *
     * //Intent intent = new Intent(OpenIntents.SHARE_INSERT_ACTION, //
     * Shopping.Items.CONTENT_URI);
     *
     * // workaround for Anroid m5 issue: send content URI in bundle. Intent
     * intent = new Intent(OpenIntents.SHARE_INSERT_ACTION);
     * intent.putExtra(DATA, Shopping.Items.CONTENT_URI.toString());
     * intent.putExtra(Shopping.Lists.SHARE_NAME, newShareListName);
     * intent.putExtra(Shopping.Items.NAME, itemName);
     *
     * mContext.broadcastIntent(intent);
     *
     * } else { Log.i(TAG, "remote recipient: " + recipient);
     *
     * // Recipient is remote address if (mGTalkSession != null) { try {
     * //Intent intent = new Intent(OpenIntents.SHARE_INSERT_ACTION, //
     * Shopping.Items.CONTENT_URI);
     *
     * // workaround for Anroid m5 issue: send content URI in bundle. Intent
     * intent = new Intent(OpenIntents.SHARE_INSERT_ACTION);
     * intent.putExtra(DATA, Shopping.Items.CONTENT_URI.toString());
     *
     * intent.putExtra(Shopping.Lists.SHARE_NAME, shareListName);
     * intent.putExtra(Shopping.Items.NAME, itemName);
     *
     * mGTalkSession.sendDataMessage(recipient, intent); } catch
     * (DeadObjectException ex) { Log.e(TAG, "caught " + ex);
     * showMessage(mContext.getText(R.string.gtalk_found_stale_service));
     * mGTalkSession = null; bindGTalkService(); } } else {
     * showMessage(mContext.getText(R.string.gtalk_service_not_connected));
     * return; }
     *
     * } }
     *
     *
     * }
     */
    /**
     * Sends information about a new item to all recipients.
     */
    /*
     * public void sendItemUpdate(String recipients, String shareListName,
     * String itemNameOld, String itemName, Long itemStatusOld, Long itemStatus)
     * { Log.i(TAG, "sendItemUpdate(" + recipients + ", " + shareListName + ", "
     * + itemNameOld + ", " + itemName + ", " + itemStatusOld + ", " +
     * itemStatus + ")");
     *
     * String itemSender = "";
     *
     *
     *
     * // First take out white spaces String r = recipients.replace(" ", ""); //
     * Then convert to list String[] recipientList = r.split(",");
     *
     * int max = recipientList.length; if (max == 1 &&
     * recipientList[0].equals("")) { // this is an empty list - nothing to
     * send: return; } for (int i = 0; i < max; i++) { String recipient =
     * recipientList[i];
     *
     * if (recipient.startsWith("local/")) { Log.i(TAG, "local recipient: " +
     * recipient); // Recipient is local address
     *
     * // If we have a local list, we map the shareListName to a // new name so
     * that we can synchronize two different local // lists. String
     * newShareListName = recipient.substring("local/".length());
     *
     * // The item sender's name will be just the unique list id: itemSender =
     * shareListName;
     *
     * Intent intent = new Intent(OpenIntents.SHARE_UPDATE_ACTION,
     * Shopping.Items.CONTENT_URI); intent.putExtra(Shopping.Lists.SHARE_NAME,
     * newShareListName); intent.putExtra(Shopping.Items.NAME + OLD,
     * itemNameOld); intent.putExtra(Shopping.Items.NAME, itemName); // TODO: In
     * m5, Android only supports Strings in bundles for GTalk
     * intent.putExtra(Shopping.Contains.STATUS + OLD, "" + itemStatusOld);
     * intent.putExtra(Shopping.Contains.STATUS, "" + itemStatus);
     * intent.putExtra(GTalkSender.SENDER, itemSender);
     *
     * mContext.broadcastIntent(intent);
     *
     * } else { Log.i(TAG, "remote recipient: " + recipient);
     *
     * // Recipient is remote address if (mGTalkSession != null) { try {
     * itemSender = mGTalkSession.getUsername();
     *
     * //Intent intent = new Intent(OpenIntents.SHARE_UPDATE_ACTION, //
     * Shopping.Items.CONTENT_URI);
     *
     * // workaround for Anroid m5 issue: send content URI in bundle. Intent
     * intent = new Intent(OpenIntents.SHARE_UPDATE_ACTION);
     * intent.putExtra(DATA, Shopping.Items.CONTENT_URI.toString());
     *
     * intent.putExtra(Shopping.Lists.SHARE_NAME, shareListName);
     * intent.putExtra(Shopping.Items.NAME + OLD, itemNameOld);
     * intent.putExtra(Shopping.Items.NAME, itemName); // TODO: In m5, Android
     * only supports Strings in bundles for GTalk
     * intent.putExtra(Shopping.Contains.STATUS + OLD, "" + itemStatusOld);
     * intent.putExtra(Shopping.Contains.STATUS, "" + itemStatus);
     * intent.putExtra(GTalkSender.SENDER, itemSender);
     *
     * mGTalkSession.sendDataMessage(recipient, intent); } catch
     * (DeadObjectException ex) { Log.e(TAG, "caught " + ex);
     * showMessage(mContext.getText(R.string.gtalk_found_stale_service));
     * mGTalkSession = null; bindGTalkService(); } } else { //
     * showMessage(mContext.getText(R.string.gtalk_service_not_connected));
     * showMessage(mContext.getText(R.string.gtalk_not_connected)); return; }
     *
     * } }
     *
     *
     * }
     */

}


import android.app.Dialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

//public class DistributionLibraryActivity extends Activity {//Temp - FragmentActivity for 3.x compatibility
public class DistributionLibraryFragmentActivity extends AppCompatActivity {

    private static final int MENU_DISTRIBUTION_START = Menu.FIRST;

    private static final int DIALOG_DISTRIBUTION_START = 1;

    protected DistributionLibrary mDistribution;

    /**
     * Called when the activity is first created.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDistribution = new DistributionLibrary(this, MENU_DISTRIBUTION_START,
                DIALOG_DISTRIBUTION_START);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        mDistribution.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDistribution.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        return mDistribution.onCreateDialog(id);
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        super.onPrepareDialog(id, dialog);
        mDistribution.onPrepareDialog(id, dialog);
    }
}


import android.app.backup.BackupManager;
import android.content.Context;

public class BackupManagerWrapper {
    /* class initialization fails when this throws an exception */
    static {
        try {
            Class.forName("android.app.backup.BackupManager");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private BackupManager mInstance;

    public BackupManagerWrapper(Context ctx) {
        mInstance = new BackupManager(ctx);

    }

    /* calling here forces class initialization */
    public static void checkAvailable() {
    }

    public void dataChanged() {
        mInstance.dataChanged();
    }
}


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.view.Menu;
import android.view.MenuItem;

import java.util.List;

/**
 * Adds intent options with icons.
 * <p/>
 * This code is retrieved from this message:
 * http://groups.google.com/group/android
 * -developers/browse_frm/thread/3fed25cdda765b02
 */
public class MenuIntentOptionsWithIcons {

    private Context mContext;
    private Menu mMenu;

    public MenuIntentOptionsWithIcons(Context context, Menu menu) {
        mContext = context;
        mMenu = menu;
    }

    public int addIntentOptions(int group, int id, int categoryOrder,
                                ComponentName caller, Intent[] specifics, Intent intent, int flags,
                                MenuItem[] outSpecificItems) {
        PackageManager pm = mContext.getPackageManager();
        final List<ResolveInfo> lri = pm.queryIntentActivityOptions(caller,
                specifics, intent, 0);
        final int N = lri != null ? lri.size() : 0;
        if ((flags & Menu.FLAG_APPEND_TO_GROUP) == 0) {
            mMenu.removeGroup(group);
        }
        for (int i = 0; i < N; i++) {
            final ResolveInfo ri = lri.get(i);
            Intent rintent = new Intent(ri.specificIndex < 0 ? intent
                    : specifics[ri.specificIndex]);
            rintent.setComponent(new ComponentName(
                    ri.activityInfo.applicationInfo.packageName,
                    ri.activityInfo.name));
            final MenuItem item = mMenu
                    .add(group, id, categoryOrder, ri.loadLabel(pm))
                    .setIcon(ri.loadIcon(pm)).setIntent(rintent);
            if (outSpecificItems != null && ri.specificIndex >= 0) {
                outSpecificItems[ri.specificIndex] = item;
            }
        }
        return N;
    }
}


import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public abstract class ShakeSensorListener implements SensorEventListener {

    private double mTotalForcePrev; // stores the previous total force value

    @Override
    public void onAccuracyChanged(final Sensor sensor, final int accuracy) {
        //ignore
    }

    @Override
    public void onSensorChanged(final SensorEvent event) {
        int sensor = event.sensor.getType();
        final float[] values = event.values;
        if (sensor == Sensor.TYPE_ACCELEROMETER) {
            double forceThreshHold = 1.5f;

            double totalForce = 0.0f;
            totalForce += Math.pow(values[0]
                    / SensorManager.GRAVITY_EARTH, 2.0);
            totalForce += Math.pow(values[1]
                    / SensorManager.GRAVITY_EARTH, 2.0);
            totalForce += Math.pow(values[2]
                    / SensorManager.GRAVITY_EARTH, 2.0);
            totalForce = Math.sqrt(totalForce);

            if ((totalForce < forceThreshHold)
                    && (mTotalForcePrev > forceThreshHold)) {
                onShake();
            }

            mTotalForcePrev = totalForce;
        }
    }

    public abstract void onShake();

}

/*
 * Copyright (C) 2008 OpenIntents.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

/**
 * Definition for content provider related to hardware. Stores hardware
 * abstraction and hardware simulator related data.
 */
public abstract class Hardware {

    public static final String[] mProjectionPreferencesFilter = new String[]{
            Preferences._ID, Preferences.NAME, Preferences.VALUE};
    public static final int mProjectionPreferencesID = 0;

    // //////////////////////////////////////////////////////
    // Convenience functions:
    public static final int mProjectionPreferencesNAME = 1;
    public static final int mProjectionPreferencesVALUE = 2;
    // Some default preference values
    public static final String IPADDRESS = "IP address";
    public static final String SOCKET = "Socket";
    public static final String DEFAULT_SOCKET = "8010";
    /**
     * TAG for logging.
     */
    private static final String TAG = "Hardware";
    /**
     * The content resolver has to be set before accessing any of these
     * functions.
     */
    public static ContentResolver mContentResolver;

    /**
     * Obtains the 'value' for preferenceID, or returns "" if not existent.
     *
     * @param name The name of the preference.
     * @return The value for preference 'name'.
     */
    public static String getPreference(final String name) {
        String s = "";
        try {
            Log.i(TAG, "getPreference()");
            Cursor c = mContentResolver.query(Preferences.CONTENT_URI,
                    mProjectionPreferencesFilter, Preferences.NAME + "= '"
                            + name + "'", null, Preferences.DEFAULT_SORT_ORDER
            );
            if (c.getCount() >= 1) {
                c.moveToFirst();
                return c.getString(mProjectionPreferencesVALUE);
            } else if (c.getCount() == 0) {
                // This value does not exist yet!
            } else {
                Log.e(TAG, "table 'preferences' corrupt. Multiple NAME!");
            }
            c.close();
        } catch (Exception e) {
            Log.e(TAG, "insert into table 'contains' failed", e);
            s = "Preferences table corrupt!";
        }
        return s;
    }

    /**
     * Updates the 'value' for the preferenceID.
     *
     * @param name  The name of the preference.
     * @param value The value to set.
     */
    public static void setPreference(final String name, final String value) {
        /*
         * // This value does not exist yet. Let's insert it: ContentValues
         * values2 = new ContentValues(2); values2.put(Preferences.NAME, name);
         * values2.put(Preferences.VALUE, value);
         * mContentResolver.insert(Preferences.CONTENT_URI, values2);
         */

        Log.i(TAG, "setPreference");
        try {
            Log.i(TAG, "get Cursor.");
            if (mContentResolver == null) {
                Log.i(TAG, "Panic!.");
            }
            Cursor c = mContentResolver.query(Preferences.CONTENT_URI,
                    mProjectionPreferencesFilter, Preferences.NAME + "= '"
                            + name + "'", null, Preferences.DEFAULT_SORT_ORDER
            );
            Log.i(TAG, "got Cursor.");
            // Log.i(TAG, "Cursor: " + c.toString());

            if (c == null) {
                Log.e(TAG, "missing hardware provider");
                return;
            }

            if (c.getCount() < 1) {
                Log.i(TAG, "Insert");

                // This value does not exist yet. Let's insert it:
                ContentValues values = new ContentValues(2);
                values.put(Preferences.NAME, name);
                values.put(Preferences.VALUE, value);
                mContentResolver.insert(Preferences.CONTENT_URI, values);
            } else if (c.getCount() >= 1) {
                Log.i(TAG, "Update");

                // This is the key, so we can update it:
                c.moveToFirst();
                String id = c.getString(mProjectionPreferencesID);
                ContentValues cv = new ContentValues();
                cv.put(Preferences.VALUE, value);
                mContentResolver.update(
                        Uri.withAppendedPath(Preferences.CONTENT_URI, id), cv,
                        null, null);

                // c.requery();
                c.getString(mProjectionPreferencesVALUE);
            } else {
                Log.e(TAG, "table 'preferences' corrupt. Multiple NAME!");
            }
            c.close();

        } catch (Exception e) {
            Log.i(TAG, "setPreference() failed", e);

        }
    }

    /**
     * Hardware preferences. Simple table to store name-value pairs.
     */
    public static final class Preferences implements BaseColumns {
        /**
         * The content:// style URL for this table.
         */
        public static final Uri CONTENT_URI = Uri
                .parse("content://org.openintents.hardware/preferences");

        /**
         * The default sort order for this table.
         */
        public static final String DEFAULT_SORT_ORDER = "_id ASC";

        /**
         * The name of the item.
         * <p/>
         * Type: TEXT
         * </P>
         */
        public static final String NAME = "name";

        /**
         * An image of the item (uri).
         * <p/>
         * Type: TEXT
         * </P>
         */
        public static final String VALUE = "value";

    }
}


/*
 * Copyright (C) 2007-2008 OpenIntents.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.AlarmManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.util.Log;

/**
 * Provider for the Alert Framework, Make sure you call init(Context c) before
 * using any of the convenience functions. Location.Position is an Uri of the
 * format geo:long,lat example: geo:3.1472,567890 Location.Distance is distance
 * in meters as long.
 *
 * @author Ronan 'Zero' Schwarz
 */
public class Alert {

    public static final String _TAG = "Alert";

    public static final String TYPE_LOCATION = "location";
    public static final String TYPE_SENSOR = "sensor";
    public static final String TYPE_GENERIC = "generic";
    public static final String TYPE_COMBINED = "combined";
    public static final String TYPE_DATE_TIME = "datetime";

    public static final String NATURE_USER = "user";
    public static final String NATURE_SYSTEM = "system";
    // ugly hack to make the mock provider work,
    // see [..]
    public static final long LOCATION_EXPIRES = 1000000;
    public static final String EXTRA_URI = "URI";
    private static final UriMatcher URL_MATCHER;
    private static final int ALERT_GENERIC = 100;
    private static final int ALERT_GENERIC_ID = 101;
    private static final int ALERT_LOCATION = 102;
    private static final int ALERT_LOCATION_ID = 103;
    private static final int ALERT_COMBINED = 104;
    private static final int ALERT_COMBINED_ID = 105;
    private static final int ALERT_SENSOR = 106;
    private static final int ALERT_SENSOR_ID = 106;
    private static final int ALERT_DATE_TIME = 107;
    private static final int ALERT_DATE_TIME_ID = 108;
    public static ContentResolver mContentResolver;
    protected static LocationManager locationManager;

    protected static AlarmManager alarmManager;

    protected static Context context;

    static {

        URL_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

        URL_MATCHER.addURI("org.openintents.alert", "generic/", ALERT_GENERIC);
        URL_MATCHER.addURI("org.openintents.alert", "generic/#",
                ALERT_GENERIC_ID);
        URL_MATCHER.addURI("org.openintents.alert", "location", ALERT_LOCATION);
        URL_MATCHER.addURI("org.openintents.alert", "location/#",
                ALERT_LOCATION_ID);
        URL_MATCHER.addURI("org.openintents.alert", "combined", ALERT_COMBINED);
        URL_MATCHER.addURI("org.openintents.alert", "combined/#",
                ALERT_COMBINED_ID);
        URL_MATCHER
                .addURI("org.openintents.alert", "datetime", ALERT_DATE_TIME);
        URL_MATCHER.addURI("org.openintents.alert", "datetime/#",
                ALERT_DATE_TIME_ID);
        URL_MATCHER.addURI("org.openintents.alert", "", 6000);
        URL_MATCHER.addURI("org.openintents.alert", "/", 6001);
    }

    public static void registerManagedService(String serviceClassName,
                                              long timeIntervall, boolean useWhileRoaming) {
        long minTime;
        ContentValues cv;

        Cursor c = mContentResolver.query(ManagedService.CONTENT_URI,
                ManagedService.PROJECTION, ManagedService.SERVICE_CLASS
                        + " like '" + serviceClassName + "'", null, null
        );

        if (c != null && c.getCount() > 0) {// update
            c.moveToFirst();
            String id = c
                    .getString(c.getColumnIndexOrThrow(ManagedService._ID));
            ContentValues values = new ContentValues();
            values.put(ManagedService.TIME_INTERVALL,
                    Long.toString(timeIntervall));
            values.put(ManagedService.DO_ROAMING,
                    Boolean.toString(useWhileRoaming));
            mContentResolver.update(
                    Uri.withAppendedPath(ManagedService.CONTENT_URI, id),
                    values, null, null);
        } else {
            // insert
            cv = new ContentValues();
            cv.put(ManagedService.SERVICE_CLASS, serviceClassName);
            cv.put(ManagedService.TIME_INTERVALL, timeIntervall);
            cv.put(ManagedService.DO_ROAMING, useWhileRoaming);
            insert(ManagedService.CONTENT_URI, cv);

        }
        if (c != null) {
            c.close();
        }
        // get all entry && compute new minimum time intervall
        // TODO: make this in sql.
        c = mContentResolver.query(ManagedService.CONTENT_URI,
                ManagedService.PROJECTION, null, null, null);

        c.moveToFirst();
        minTime = c.getLong(c.getColumnIndex(ManagedService.TIME_INTERVALL));
        while (!c.isAfterLast()) {
            long l = c.getLong(c.getColumnIndex(ManagedService.TIME_INTERVALL));

            if (l < minTime) {
                minTime = l;
            }

            c.moveToNext();
        }
        c.close();

        c = mContentResolver.query(DateTime.CONTENT_URI, DateTime.PROJECTION,
                DateTime.INTENT + " like '"
                        + org.openintents.OpenIntents.SERVICE_MANAGER + "'",
                null, null
        );
        String now = "time:epoch," + System.currentTimeMillis();
        cv = new ContentValues();
        cv.put(DateTime.TIME, now);
        cv.put(DateTime.REOCCURENCE, minTime);
        cv.put(DateTime.INTENT, org.openintents.OpenIntents.SERVICE_MANAGER);
        cv.put(DateTime.NATURE, Alert.NATURE_SYSTEM);
        cv.put(DateTime.ACTIVATE_ON_BOOT, true);
        cv.put(DateTime.ACTIVE, true);
        cv.put(DateTime.TYPE, Alert.TYPE_DATE_TIME);
        if (c != null && c.getCount() > 0) {

            update(DateTime.CONTENT_URI, cv, DateTime.INTENT + " like '"
                    + org.openintents.OpenIntents.SERVICE_MANAGER + "'", null);

            // TODO new SDK cancle PendingIntent
            // alarmManager.cancel(new
            // Intent().setAction(org.openintents.OpenIntents.SERVICE_MANAGER));

            registerDateTimeAlert(cv);
            // registerDateTimeAlert
        } else {

            insert(DateTime.CONTENT_URI, cv);

        }
        if (c != null) {
            c.close();
        }
        Log.d(_TAG, "registerManagedService: finished");

    }

    public static void unregisterManagedService(String serviceClassName) {
        ContentValues cv = new ContentValues();
        long minTime;

        delete(ManagedService.CONTENT_URI, ManagedService.SERVICE_CLASS
                + " like '" + serviceClassName + "'", null);

        // get all entry && compute new minimum time intervall
        // TODO: make this in sql.
        Cursor c = mContentResolver.query(ManagedService.CONTENT_URI,
                ManagedService.PROJECTION, null, null, null);

        c.moveToFirst();
        minTime = c.getLong(c.getColumnIndex(ManagedService.TIME_INTERVALL));
        while (!c.isAfterLast()) {
            long l = c.getLong(c.getColumnIndex(ManagedService.TIME_INTERVALL));

            if (l < minTime) {
                minTime = l;
            }

            c.moveToNext();
        }
        c.close();

        c = mContentResolver.query(DateTime.CONTENT_URI, DateTime.PROJECTION,
                DateTime.INTENT + " like '"
                        + org.openintents.OpenIntents.SERVICE_MANAGER + "'",
                null, null
        );
        String now = "time:epoch," + System.currentTimeMillis();
        cv.put(DateTime.TIME, now);
        cv.put(DateTime.REOCCURENCE, minTime);
        cv.put(DateTime.INTENT, org.openintents.OpenIntents.SERVICE_MANAGER);
        cv.put(DateTime.NATURE, Alert.NATURE_SYSTEM);
        cv.put(DateTime.ACTIVATE_ON_BOOT, true);
        cv.put(DateTime.ACTIVE, true);
        cv.put(DateTime.TYPE, Alert.TYPE_DATE_TIME);
        if (c != null && c.getCount() > 0) {

            update(DateTime.CONTENT_URI, cv, DateTime.INTENT + " like '"
                    + org.openintents.OpenIntents.SERVICE_MANAGER + "'", null);

            // TODO new SDK cancle PendingIntent
            // alarmManager.cancel(new
            // Intent().setAction(org.openintents.OpenIntents.SERVICE_MANAGER));
            registerDateTimeAlert(cv);
            // registerDateTimeAlert
        }

    }

    /**
     * @param uri the content uri to insert to
     * @param cv  the ContentValues that will be inserted to
     */
    public static Uri insert(Uri uri, ContentValues cv) {
        Uri res;
        int type = URL_MATCHER.match(uri);

        res = mContentResolver.insert(uri, cv);
        Log.d(_TAG, " insert, result>>" + res + "<<");
        if (res != null) {// register alert
            Log.d(_TAG, "uri>>" + uri + "<< matched>>" + type + "<<");
            switch (type) {
                case ALERT_LOCATION:
                    registerLocationAlert(cv);
                    break;
                case ALERT_DATE_TIME:
                    registerDateTimeAlert(cv);
                    break;
                default:
                    break;
            }

        }
        return res;
    }

    /**
     * @param uri           the content uri to delete
     * @param selection     the selection to check against
     * @param selectionArgs the arguments applied to selection string (optional)
     * @return number of deleted rows
     */
    public static int delete(Uri uri, String selection, String[] selectionArgs) {

        return mContentResolver.delete(uri, selection, selectionArgs);
    }

    /**
     * @param uri           the content uri to update
     * @param cv            the ContentValues that will be update in selected rows.
     * @param selection     the selection to check against
     * @param selectionArgs the arguments applied to selection string (optional)
     * @return number of updated rows
     */
    public static int update(Uri uri, ContentValues values, String selection,
                             String[] selectionArgs) {
        return mContentResolver.update(uri, values, selection, selectionArgs);
    }

    public static void registerLocationAlert(ContentValues cv) {
        Uri gUri = null;
        String distStr = "";

        String geo = "";
        String[] loc = null;

        try {
            gUri = Uri.parse(cv.getAsString(Location.POSITION));
            // do this for easier debugging.
            distStr = cv.getAsString(Location.DISTANCE);
            // float dist=cv.getAsFloat(Location.DISTANCE);
            geo = gUri.getSchemeSpecificPart();
            loc = geo.split(",");

            // TODO: find out how to handle this now
            /*
             * PendingIntent i= new PendingIntent();
             * //i.setClassName("org.openintents.alert"
             * ,"LocationAlertDispatcher");
             * i.setAction("org.openintents.action.LOCATION_ALERT_DISPATCH");
             * //i.setData(gUri); i.putExtra(Location.POSITION,
             * cv.getAsString(Location.POSITION));
             *
             * locationManager.addProximityAlert( latitude, longitude, dist,
             * LOCATION_EXPIRES, i );
             * Log.d(_TAG,"Registerd alert geo:"+geo+" dist:"+dist);
             * Log.d(_TAG,"Registered alert intent:" + i);
             */
        } catch (ArrayIndexOutOfBoundsException aioe) {
            Log.e(_TAG, "Error parsing geo uri. not in format geo:lat,long");
        } catch (NumberFormatException nfe) {
            Log.e(_TAG,
                    "Error parsing longitude/latitude. Not A Number (NAN)\n uri>>"
                            + gUri + "<< \n dist>>" + distStr + "<<"
            );
        } catch (NullPointerException npe) {
            Log.e(_TAG, "Nullpointer occured. did you call init(context) ?");
            npe.printStackTrace();
        }

        // registerReceiver(org.openintents.alert.LocationAlertDispatcher,);
    }

    public static void init(Context c) {
        context = c;
        locationManager = (LocationManager) context
                .getSystemService(Context.LOCATION_SERVICE);
        mContentResolver = context.getContentResolver();
        alarmManager = (AlarmManager) context
                .getSystemService(Context.ALARM_SERVICE);
    }

    public static void registerDateTimeAlert(ContentValues cv) {

        String myDate = cv.getAsString(DateTime.TIME);
        String[] s = myDate.split(",");
        Log.d(_TAG, "registerDateTimeAlert: s[0]>>" + s[0] + "<< s[1]>>+"
                + s[1] + "<<");
        long time = 0;
        long myReoccurence = cv.getAsLong(DateTime.REOCCURENCE);
        Intent i = new Intent();
        Bundle b = new Bundle();
        b.putString(DateTime.TIME, myDate);
        b.putLong(DateTime.REOCCURENCE, myReoccurence);
        i.setAction(org.openintents.OpenIntents.DATE_TIME_ALERT_DISPATCH);
        try {
            time = Long.parseLong(s[1]);
        } catch (NumberFormatException nfe) {
            Log.e(_TAG,
                    "registerDateTimeAlert: Date/Time couldn't be parsed, check time format of >"
                            + myDate + "<"
            );
            return;
        }

        if (myReoccurence == 0) {
            // TODO new SDK cancle PendingIntent
            // alarmManager.set(AlarmManager.RTC,time,i);
            Log.d(_TAG, "registerDateTimeAlert: registerd single @>>" + time
                    + "<<");

        } else {
            // TODO new SDK cancle PendingIntent
            // alarmManager.setRepeating(AlarmManager.RTC,time,myReoccurence,i);
            Log.d(_TAG, "registerDateTimeAlert: registerd reoccuirng @>>"
                    + time + "<< intervall>>" + myReoccurence + "<<");

        }
    }

    public static void unregisterDateTimeAlert(ContentValues cv) {
        /*
         * String myDate=cv.getAsString(DateTime.TIME); String
         * s[]=myDate.split(",");
         * Log.d(_TAG,"registerDateTimeAlert: s[0]>>"+s[0]
         * +"<< s[1]>>+"+s[1]+"<<"); long time=0; long
         * myReoccurence=cv.getAsLong(DateTime.REOCCURENCE);
         *
         * Cursor c=mContentResolver.query( DateTime.CONTENT_URI,
         * DateTime.PROJECTION_MAP, DateTime.TIME+" like '"+myDate+"'", null
         * null );
         *
         * if (c==null||c.count()==0) {//alert has been deleted
         *
         * }else if (c!=null&&c.count()==1) {//exactly out alert alarmManager. }
         * //TODO: check if there are now other alerts at this time.
         */

        // atm it would oly be possible to delete all dateTimeDispatch alerts
        // and register the need a new. so we just leave them be, means some
        // emtpy lookups, but hey! :/
    }

    public static final class Generic implements BaseColumns {

        public static final Uri CONTENT_URI = Uri
                .parse("content://org.openintents.alert/generic");

        public static final String CONDITION1 = "condition1";

        public static final String CONDITION2 = "condition2";

        public static final String TYPE = "alert_type";

        public static final String RULE = "rule";

        public static final String NATURE = "nature";

        public static final String ACTIVE = "active";

        public static final String ACTIVATE_ON_BOOT = "activate_on_boot";

        public static final String INTENT = "intent";

        public static final String INTENT_CATEGORY = "intent_category";

        public static final String INTENT_URI = "intent_uri";

        public static final String INTENT_MIME_TYPE = "intent_mime_type";

        public static final String DEFAULT_SORT_ORDER = "";

        public static final String[] PROJECTION = {_ID, _COUNT, CONDITION1,
                CONDITION2, TYPE, RULE, NATURE, ACTIVE, ACTIVATE_ON_BOOT,
                INTENT, INTENT_CATEGORY, INTENT_URI, INTENT_MIME_TYPE};

    }

    /**
     * location based alerts. you must at least specify a position
     */
    public static final class Location implements BaseColumns {

        public static final Uri CONTENT_URI = Uri
                .parse("content://org.openintents.alert/location");

        /**
         * Location.Position is an Uri of the format geo:long,lat example:
         * geo:3.1472,567890
         */
        public static final String POSITION = Generic.CONDITION1;

        /**
         * Location.Distance is distance in meters as long.
         */
        public static final String DISTANCE = Generic.CONDITION2;

        /**
         * Type must always be of Alert.TYPE_LOCATION any other values will
         * result in your alert not being processed.
         */
        public static final String TYPE = Generic.TYPE;

        public static final String RULE = Generic.RULE;

        public static final String NATURE = Generic.NATURE;

        public static final String ACTIVE = Generic.ACTIVE;

        public static final String ACTIVATE_ON_BOOT = Generic.ACTIVATE_ON_BOOT;

        public static final String INTENT = Generic.INTENT;

        public static final String INTENT_CATEGORY = Generic.INTENT_CATEGORY;

        public static final String INTENT_URI = Generic.INTENT_URI;

        public static final String INTENT_MIME_TYPE = Generic.INTENT_MIME_TYPE;

        public static final String DEFAULT_SORT_ORDER = "";

        public static final String[] PROJECTION = {_ID, _COUNT, POSITION,
                DISTANCE, TYPE, RULE, NATURE, ACTIVE, ACTIVATE_ON_BOOT, INTENT,
                INTENT_CATEGORY, INTENT_URI, INTENT_MIME_TYPE};

    }

    public static final class DateTime implements BaseColumns {

        public static final Uri CONTENT_URI = Uri
                .parse("content://org.openintents.alert/datetime");

        /**
         * The point in time for the alarm, in format time:epoch1234456 number
         * is time in millisecond sice 1970, like you get from
         * System.getCurrentMillis
         */
        public static final String TIME = Generic.CONDITION1;

        /**
         * the alert reocurs every n milliseconds, or not at all if set to 0.
         * reouccreny should be at least 1 minute.
         */
        public static final String REOCCURENCE = Generic.CONDITION2;

        public static final String TYPE = Generic.TYPE;

        public static final String RULE = Generic.RULE;

        public static final String NATURE = Generic.NATURE;

        public static final String ACTIVE = Generic.ACTIVE;

        public static final String ACTIVATE_ON_BOOT = Generic.ACTIVATE_ON_BOOT;

        public static final String INTENT = Generic.INTENT;

        public static final String INTENT_CATEGORY = Generic.INTENT_CATEGORY;

        public static final String INTENT_URI = Generic.INTENT_URI;

        public static final String INTENT_MIME_TYPE = Generic.INTENT_MIME_TYPE;

        public static final String DEFAULT_SORT_ORDER = "";

        public static final String[] PROJECTION = {_ID, _COUNT, TIME,
                REOCCURENCE, TYPE, RULE, NATURE, ACTIVE, ACTIVATE_ON_BOOT,
                INTENT, INTENT_CATEGORY, INTENT_URI, INTENT_MIME_TYPE};

    }

    public static final class ManagedService implements BaseColumns {

        public static final Uri CONTENT_URI = Uri
                .parse("content://org.openintents.alert/managedservice");
        public static final String SERVICE_CLASS = "service_class";

        public static final String TIME_INTERVALL = "time_intervall";

        public static final String DO_ROAMING = "do_roaming";

        public static final String LAST_TIME = "last_time";

        public static final String[] PROJECTION = {_ID, _COUNT, SERVICE_CLASS,
                TIME_INTERVALL, DO_ROAMING, LAST_TIME};
    }

}/* eoc */
/*
 * Copyright (C) 2007-2008 OpenIntents.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Point;
import android.net.Uri;
import android.net.Uri.Builder;
import android.provider.BaseColumns;

/**
 * Definition for content provider related to location.
 */
public class Location {

    private ContentResolver mResolver;

    public Location(ContentResolver resolver) {
        mResolver = resolver;
    }

    public Uri addLocation(android.location.Location location) {
        ContentValues values = new ContentValues(2);
        values.put(Locations.LATITUDE, location.getLatitude());
        values.put(Locations.LONGITUDE, location.getLongitude());
        return mResolver.insert(Locations.CONTENT_URI, values);
    }

    public int deleteLocation(long id) {

        return mResolver.delete(
                ContentUris.withAppendedId(Locations.CONTENT_URI, id), null,
                null);

    }

    public Point getPoint(long id) {
        Point p = null;
        Cursor cursor = mResolver.query(
                ContentUris.withAppendedId(Locations.CONTENT_URI, id),
                new String[]{Locations._ID, Locations.LATITUDE,
                        Locations.LONGITUDE}, null, null,
                Locations.DEFAULT_SORT_ORDER
        );
        if (cursor.moveToNext()) {
            int lat = Double.valueOf(cursor.getDouble(1) * 1E6).intValue();
            int lon = Double.valueOf(cursor.getDouble(2) * 1E6).intValue();
            p = new Point(lat, lon);
        } else {
        }
        cursor.close();
        return p;
    }

    /**
     * @param locationId
     * @return
     * @deprecated !! Warning !! Cursor has to be closed by caller. Alternative
     * API desired.
     */
    public Cursor queryExtras(long locationId) {
        Builder uri = Locations.CONTENT_URI.buildUpon()
                .appendPath(String.valueOf(locationId))
                .appendPath(Extras.URI_PATH_EXTRAS);
        return mResolver.query(uri.build(), new String[]{Extras._ID,
                Extras.KEY, Extras.VALUE}, null, null, Extras.KEY + ","
                + Extras.VALUE);
    }

    public void updateExtras(long locationId, long extrasId,
                             ContentValues values) {

    }

    public int deleteExtra(long extraId) {
        return mResolver.delete(
                ContentUris.withAppendedId(Extras.CONTENT_URI, extraId), null,
                null);

    }

    public Uri addExtra(long locationId) {
        Builder uri = Locations.CONTENT_URI.buildUpon()
                .appendPath(String.valueOf(locationId))
                .appendPath(Extras.URI_PATH_EXTRAS);
        return mResolver.insert(uri.build(), null);

    }

    public static final class Locations implements BaseColumns {
        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri
                .parse("content://org.openintents.locations/locations");

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = "modified DESC";

        /**
         * The latitude of the location
         * <p/>
         * Type: TEXT
         * </P>
         */
        public static final String LATITUDE = "latitude";

        /**
         * The longitude of the location
         * <p/>
         * Type: TEXT
         * </P>
         */
        public static final String LONGITUDE = "longitude";

        /**
         * The timestamp for when the note was created
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String CREATED_DATE = "created";

        /**
         * The timestamp for when the note was last modified
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String MODIFIED_DATE = "modified";

        /**
         * bundle/extra key for pick action, containing location uri with scheme
         * geo:
         */
        public static final String EXTRA_GEO = "geo";
    }

    public static final class Extras implements BaseColumns {

        public static final String LOCATION_ID = "locationId";
        public static final String KEY = "key";
        public static final String VALUE = "value";

        public static final String URI_PATH_EXTRAS = "extras";
        public static final Uri CONTENT_URI = Uri
                .parse("content://org.openintents.locations/extras");

    }
}

/*
 * Copyright (C) 2007-2008 OpenIntents.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

/**
 * Definition for content provider related to tag.
 */
public class Tag {

    private static final String TAG = "Tag.java";
    private static final String DELETE_URI = "tag.content_id = (select content2._id FROM content content2 WHERE content2.uri = ?)";
    private static final String DELETE_TAG_URI = "tag.tag_id = (select content1._id FROM content content1 WHERE content1.uri = ?) "
            + "AND tag.content_id = (select content2._id FROM content content2 WHERE content2.uri = ?)";
    private Context mContext;

    public Tag(Context context) {
        mContext = context;
    }

    public void removeTag(String tag, String uri) {

        mContext.getContentResolver().delete(Tags.CONTENT_URI,
                Tag.DELETE_TAG_URI, new String[]{tag, uri});

    }

    public void removeAllTags(String uri) {

        mContext.getContentResolver().delete(Tags.CONTENT_URI, Tag.DELETE_URI,
                new String[]{uri});

    }

    public void insertTag(String tag, String content) {
        ContentValues values = new ContentValues(2);
        values.put(Tags.URI_1, tag);
        values.put(Tags.URI_2, content);

        try {
            mContext.getContentResolver().insert(Tags.CONTENT_URI, values);
        } catch (Exception e) {
            Log.i(TAG, "insert failed", e);
        }
    }

    public void insertUniqueTag(String tag, String content) {
        ContentValues values = new ContentValues(2);
        values.put(Tags.URI_1, tag);
        values.put(Tags.URI_2, content);

        try {
            Uri uri = Tags.CONTENT_URI.buildUpon()
                    .appendQueryParameter(Tags.QUERY_UNIQUE_TAG, "true")
                    .build();
            mContext.getContentResolver().insert(uri, values);
        } catch (Exception e) {
            Log.i(TAG, "insert failed", e);
        }
    }

    /**
     * cursor over contentUriStrings is returned where the content is tagged
     * with the given tag.
     *
     * @param tag
     * @param contentUri
     * @return
     * @deprecated !! WARNING !! Cursor has to be closed by caller. Alternative
     * API desired.
     */
    public Cursor findTaggedContent(String tag, String contentUri) {
        return mContext.getContentResolver().query(Tags.CONTENT_URI,
                new String[]{Tags._ID, Tags.URI_2},
                "content1.uri like ? and content2.uri like ?",
                new String[]{tag, contentUri + "%"}, "content2.uri");
    }

    /**
     * cursor over tags with all tags for the given content is returned.
     *
     * @param tag
     * @param contentUri
     * @return
     * @deprecated !! WARNING !! Cursor has to be closed by caller. Alternative
     * API desired.
     */
    public Cursor findTags(String contentUri) {
        return mContext.getContentResolver().query(Tags.CONTENT_URI,
                new String[]{Tags._ID, Tags.URI_1}, "content2.uri = ?",
                new String[]{contentUri}, "content1.uri");
    }

    public String findTags(String uri, String separator) {
        Cursor tags = findTags(uri);
        StringBuffer sb = new StringBuffer();
        int colIndex = tags.getColumnIndex(Tags.URI_1);
        while (tags.moveToNext()) {
            sb.append(tags.getString(colIndex));
            sb.append(separator);
        }
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - separator.length());
        }
        tags.close();
        return sb.toString();
    }

    /**
     * cursor over tags with all tags for the given content is returned.
     *
     * @param contentUriPrefix
     * @return
     * @deprecated !! WARNING !! Cursor has to be closed by caller. Alternative
     * API desired.
     */
    public Cursor findTagsForContentType(String contentUriPrefix) {
        Uri uri = Contents.CONTENT_URI.buildUpon()
                .appendQueryParameter(Tags.DISTINCT, "true").build();
        return mContext
                .getContentResolver()
                .query(uri,
                        new String[]{Contents._ID, Contents.URI},
                        "exists(select * from content content2, tag tag where content2.uri like ? and content2._id = tag.content_id and content._id = tag.tag_id)",
                        new String[]{contentUriPrefix + "%"}, "content.uri");
    }

    /**
     * Get a cursor with all tags
     *
     * @return
     * @deprecated !! WARNING !! Cursor has to be closed by caller. Alternative
     * API desired.
     */
    public Cursor findAllTags() {
        return mContext.getContentResolver().query(Contents.CONTENT_URI,
                new String[]{Contents._ID, Contents.URI, Contents.TYPE},
                "type like 'TAG%'", null, Contents.DEFAULT_SORT_ORDER);
    }

    /**
     * Get a cursor with all used tags, i.e. at least one content has been
     * tagged with this tag.
     *
     * @return
     * @deprecated !! WARNING !! Cursor has to be closed by caller. Alternative
     * API desired.
     */
    public Cursor findAllUsedTags() {
        return mContext
                .getContentResolver()
                .query(Contents.CONTENT_URI,
                        new String[]{Contents._ID, Contents.URI,
                                Contents.TYPE},
                        "type like 'TAG%' and (select count(*) from tag where tag.tag_id = content._id) > 0",
                        null, Contents.DEFAULT_SORT_ORDER
                );
    }

    /**
     * start add tag activity. Only useful, if tag or uri is null. Consider
     * using insertTag if you want to add the tag without user interaction.
     *
     * @param tag
     * @param uri
     */
    public void startAddTagActivity(String tag, String uri) {
        Intent intent = new Intent(org.openintents.OpenIntents.TAG_ACTION,
                Tags.CONTENT_URI).putExtra(Tags.QUERY_TAG, tag).putExtra(
                Tags.QUERY_URI, uri);
        mContext.startActivity(intent);

    }

    public static final class Tags implements BaseColumns {
        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri
                .parse("content://org.openintents.tags/tags");

        /**
         * The default sort order for this table.
         */
        public static final String DEFAULT_SORT_ORDER = "modified DESC";

        /**
         * The id of the tag.
         * <p/>
         * Type: STRING
         * </P>
         */
        public static final String TAG_ID = "tag_id";

        /**
         * The id of the content.
         * <p/>
         * Type: STRING
         * </P>
         */
        public static final String CONTENT_ID = "content_id";

        /**
         * The timestamp for when the tag was created.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String CREATED_DATE = "created";

        /**
         * The timestamp for when the tag was last modified.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String MODIFIED_DATE = "modified";

        /**
         * The timestamp for when the tag was last modified.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String ACCESS_DATE = "accessed";

        /**
         * First URI of the relationship (usually the tag).
         */
        public static final String URI_1 = "uri_1";

        /**
         * Second URI of the relationship (usually the content).
         */
        public static final String URI_2 = "uri_2";

        /**
         * The Uri to be tagged that the query is about.
         */
        public static final String QUERY_URI = "uri";

        /**
         * The tag that the query is about.
         */
        public static final String QUERY_TAG = "tag";

        public static final String DISTINCT = "distinct";
        public static final String QUERY_UNIQUE_TAG = "unique";
    }

    public static final class Contents implements BaseColumns {
        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri
                .parse("content://org.openintents.tags/contents");

        /**
         * The default sort order for this table.
         */
        public static final String DEFAULT_SORT_ORDER = "type DESC, uri";

        /**
         * The uri of the content, or the tag text if the uri starts with "TAG".
         * This can be tested in SQL using "WHERE type like 'TAG%'".
         * <p/>
         * Type: TEXT
         * </P>
         */
        public static final String URI = "uri";

        /**
         * The type of the content, e.g TAG null means CONTENT.
         * <p/>
         * Type: TEXT
         * </P>
         */
        public static final String TYPE = "type";

        /**
         * The timestamp for when the note was created.
         * <p/>
         * Type: INTEGER (long)
         * </P>
         */
        public static final String CREATED_DATE = "created";

        public static final String QUERY_BY_TYPE = "byType";

    }

}


import android.net.Uri;

public abstract class Intents {
    public static final Uri CONTENT_URI = Uri.parse("openintents://intents");
    public static final String EXTRA_TYPE = "type";
    public static final String EXTRA_ACTION = "action";
    public static final String EXTRA_URI = "uri";

    /**
     * boolean extra flag indicating whether action list should include all
     * android actions.
     */
    public static final String EXTRA_ANDROID_ACTIONS = "androidActions";
    /**
     * string extra containing comma separated list of actions that should be
     * included in action list.
     */
    public static final String EXTRA_ACTION_LIST = "actionList";

    public static final String TYPE_PREFIX_DIR = "vnd.android.cursor.dir";
    public static final String TYPE_PREFIX_ITEM = "vnd.android.cursor.item";

}

