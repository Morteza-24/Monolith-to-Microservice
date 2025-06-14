
import android.content.Context;
import android.content.SharedPreferences;

import com.marwaeltayeb.souq.model.LoginApiResponse;
import com.marwaeltayeb.souq.model.User;

public class LoginUtils {

    private static final String SHARED_PREF_NAME = "shared_preference";
    private static final String ID = "id";
    private static final String NAME = "name";
    private static final String EMAIL = "email";
    private static final String PASSWORD = "password";
    private static final String TOKEN = "token";
    private static final String IS_ADMIN = "isAdmin";

    private static LoginUtils mInstance;
    private final Context mCtx;

    private LoginUtils(Context mCtx) {
        this.mCtx = mCtx;
    }

    public static synchronized LoginUtils getInstance(Context mCtx) {
        if (mInstance == null) {
            mInstance = new LoginUtils(mCtx);
        }
        return mInstance;
    }

    public void saveUserInfo(LoginApiResponse response) {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putInt(ID, response.getId());
        editor.putString(NAME, response.getName());
        editor.putString(EMAIL, response.getEmail());
        editor.putString(PASSWORD, response.getPassword());
        editor.putString(TOKEN, response.getToken());
        editor.putBoolean(IS_ADMIN, response.isAdmin());
        editor.apply();
    }

    public boolean isLoggedIn() {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getInt("id", -1) != -1;
    }

    public void saveUserInfo(User user) {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putInt(ID, user.getId());
        editor.putString(NAME, user.getName());
        editor.putString(EMAIL, user.getEmail());
        editor.putString(PASSWORD, user.getPassword());
        editor.putBoolean(IS_ADMIN, user.isAdmin());
        editor.apply();
    }

    public User getUserInfo() {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        return new User(
                sharedPreferences.getInt(ID, -1),
                sharedPreferences.getString(NAME, null),
                sharedPreferences.getString(EMAIL, null),
                sharedPreferences.getString(PASSWORD, null),
                sharedPreferences.getBoolean(IS_ADMIN, false)
        );
    }


    public String getUserToken() {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(TOKEN, "");
    }

    public void clearAll() {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear().apply();
        editor.apply();
    }

}


import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import java.util.Locale;

public class LanguageUtils {

    private LanguageUtils(){}

    public static void setEnglishState(Context context, boolean isEnglishEnabled){
        SharedPreferences sharedpreferences = context.getSharedPreferences("language_data", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putBoolean("language", isEnglishEnabled);
        editor.apply();
    }

    public static boolean getEnglishState(Context context){
        SharedPreferences sharedpreferences = context.getSharedPreferences("language_data", Context.MODE_PRIVATE);
        return sharedpreferences.getBoolean("language", true);
    }

    public static void setLocale(Context context,String language){
        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
        // Save data to shared preferences
        SharedPreferences.Editor editor = context.getSharedPreferences("Settings", MODE_PRIVATE).edit();
        editor.putString("My_lang",language);
        editor.apply();
    }

    // Load language saved in shared preferences
    public static void loadLocale(Context context){
        SharedPreferences prefs = context.getSharedPreferences("Settings", MODE_PRIVATE);
        String language = prefs.getString("My_lang","");
        setLocale(context,language);
    }
}


import static com.marwaeltayeb.souq.utils.InternetUtils.isNetworkConnected;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.marwaeltayeb.souq.utils.OnNetworkListener;

public class NetworkChangeReceiver extends BroadcastReceiver {

    OnNetworkListener onNetworkListener;

    public void setOnNetworkListener(OnNetworkListener onNetworkListener) {
        this.onNetworkListener = onNetworkListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!isNetworkConnected(context)) {
            onNetworkListener.onNetworkDisconnected();
        } else {
            onNetworkListener.onNetworkConnected();
        }
    }
}


import static com.marwaeltayeb.souq.utils.Constant.LOCALHOST;
import static com.marwaeltayeb.souq.utils.Utils.shareProduct;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProviders;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import com.marwaeltayeb.souq.R;
import com.marwaeltayeb.souq.databinding.ProductListItemBinding;
import com.marwaeltayeb.souq.model.Cart;
import com.marwaeltayeb.souq.model.Favorite;
import com.marwaeltayeb.souq.model.History;
import com.marwaeltayeb.souq.model.Product;
import com.marwaeltayeb.souq.storage.LoginUtils;
import com.marwaeltayeb.souq.utils.RequestCallback;
import com.marwaeltayeb.souq.viewmodel.AddFavoriteViewModel;
import com.marwaeltayeb.souq.viewmodel.FromCartViewModel;
import com.marwaeltayeb.souq.viewmodel.RemoveFavoriteViewModel;
import com.marwaeltayeb.souq.viewmodel.ToCartViewModel;
import com.marwaeltayeb.souq.viewmodel.ToHistoryViewModel;

import java.text.DecimalFormat;

public class ProductAdapter extends PagedListAdapter<Product, ProductAdapter.ProductViewHolder> {

    private final Context mContext;
    private Product product;
    private final AddFavoriteViewModel addFavoriteViewModel;
    private final RemoveFavoriteViewModel removeFavoriteViewModel;
    private final ToCartViewModel toCartViewModel;
    private final FromCartViewModel fromCartViewModel;
    private final ToHistoryViewModel toHistoryViewModel;

    private final ProductAdapterOnClickHandler clickHandler;

    /**
     * The interface that receives onClick messages.
     */
    public interface ProductAdapterOnClickHandler {
        void onClick(Product product);
    }

    public ProductAdapter(Context mContext, ProductAdapterOnClickHandler clickHandler) {
        super(DIFF_CALLBACK);
        this.mContext = mContext;
        this.clickHandler = clickHandler;
        addFavoriteViewModel = ViewModelProviders.of((FragmentActivity) mContext).get(AddFavoriteViewModel.class);
        removeFavoriteViewModel = ViewModelProviders.of((FragmentActivity) mContext).get(RemoveFavoriteViewModel.class);
        toCartViewModel = ViewModelProviders.of((FragmentActivity) mContext).get(ToCartViewModel.class);
        fromCartViewModel = ViewModelProviders.of((FragmentActivity) mContext).get(FromCartViewModel.class);
        toHistoryViewModel = ViewModelProviders.of((FragmentActivity) mContext).get(ToHistoryViewModel.class);
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
        ProductListItemBinding productListItemBinding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.product_list_item, parent, false);
        return new ProductViewHolder(productListItemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        holder.setIsRecyclable(false);
        product = getItem(position);

        if (product != null) {
            String productName = product.getProductName();
            holder.binding.txtProductName.setText(productName);

            DecimalFormat formatter = new DecimalFormat("#,###,###");
            String formattedPrice = formatter.format(product.getProductPrice());
            holder.binding.txtProductPrice.setText(formattedPrice + " EGP");

            // Load the Product image into ImageView
            String imageUrl = LOCALHOST + product.getProductImage().replaceAll("\\\\", "/");
            Glide.with(mContext)
                    .load(imageUrl)
                    .into(holder.binding.imgProductImage);

            Log.d("imageUrl", imageUrl);

            holder.binding.imgShare.setOnClickListener(v -> shareProduct(mContext, productName, imageUrl));

            // If product is inserted
            if (product.isFavourite() == 1) {
                holder.binding.imgFavourite.setImageResource(R.drawable.ic_favorite_pink);
            }

            // If product is added to cart
            if (product.isInCart() == 1) {
                holder.binding.imgCart.setImageResource(R.drawable.ic_shopping_cart_green);
            }

        } else {
            Toast.makeText(mContext, "Product is null", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    public void notifyOnInsertedItem(int position) {
        notifyItemInserted(position);
        notifyItemRangeInserted(position, getCurrentList().size()-1);
        notifyDataSetChanged();
    }

    // It determine if two list objects are the same or not
    private static final DiffUtil.ItemCallback<Product> DIFF_CALLBACK = new DiffUtil.ItemCallback<Product>() {
        @Override
        public boolean areItemsTheSame(@NonNull Product oldProduct, @NonNull Product newProduct) {
            return oldProduct.getProductName().equals(newProduct.getProductName());
        }

        @SuppressLint("DiffUtilEquals")
        @Override
        public boolean areContentsTheSame(@NonNull Product oldProduct, @NonNull Product newProduct) {
            return oldProduct.equals(newProduct);
        }
    };

    class ProductViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        // Create view instances
        private final ProductListItemBinding binding;

        private ProductViewHolder(ProductListItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            // Register a callback to be invoked when this view is clicked.
            itemView.setOnClickListener(this);
            binding.imgFavourite.setOnClickListener(this);
            binding.imgCart.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int position = getBindingAdapterPosition();
            // Get position of the product
            product = getItem(position);

            switch (v.getId()) {
                case R.id.card_view:
                    // Send product through click
                    clickHandler.onClick(product);
                    insertProductToHistory();
                    break;
                case R.id.imgFavourite:
                    toggleFavourite();
                    break;
                case R.id.imgCart:
                    toggleProductsInCart();
                    break;
                default: // Should not get here
            }
        }

        private void toggleFavourite() {
            // If favorite is not bookmarked
            if (product.isFavourite() != 1) {
                binding.imgFavourite.setImageResource(R.drawable.ic_favorite_pink);
                insertFavoriteProduct(() -> {
                    product.setIsFavourite(true);
                    notifyDataSetChanged();
                });
                showSnackBar("Bookmark Added");
            } else {
                binding.imgFavourite.setImageResource(R.drawable.ic_favorite_border);
                deleteFavoriteProduct(() -> {
                    product.setIsFavourite(false);
                    notifyDataSetChanged();
                });
                showSnackBar("Bookmark Removed");
            }
        }

        private void toggleProductsInCart() {
            // If Product is not added to cart
            if (product.isInCart() != 1) {
                binding.imgCart.setImageResource(R.drawable.ic_shopping_cart_green);
                insertToCart(() -> {
                    product.setIsInCart(true);
                    notifyDataSetChanged();
                });
                showSnackBar("Added To Cart");
            } else {
                binding.imgCart.setImageResource(R.drawable.ic_add_shopping_cart);
                deleteFromCart(() -> {
                    product.setIsInCart(false);
                    notifyDataSetChanged();
                });
                showSnackBar("Removed From Cart");
            }
        }

        private void showSnackBar(String text) {
            Snackbar.make(itemView, text, Snackbar.LENGTH_SHORT).show();
        }

        private void insertFavoriteProduct(RequestCallback callback) {
            Favorite favorite = new Favorite(LoginUtils.getInstance(mContext).getUserInfo().getId(), product.getProductId());
            addFavoriteViewModel.addFavorite(favorite,callback);
        }

        private void deleteFavoriteProduct(RequestCallback callback) {
            removeFavoriteViewModel.removeFavorite(LoginUtils.getInstance(mContext).getUserInfo().getId(), product.getProductId(),callback);
        }

        private void insertToCart(RequestCallback callback) {
            Cart cart = new Cart(LoginUtils.getInstance(mContext).getUserInfo().getId(), product.getProductId());
            toCartViewModel.addToCart(cart, callback);
        }

        private void deleteFromCart(RequestCallback callback) {
            fromCartViewModel.removeFromCart(LoginUtils.getInstance(mContext).getUserInfo().getId(), product.getProductId(),callback);
        }

        private void insertProductToHistory() {
            History history = new History(LoginUtils.getInstance(mContext).getUserInfo().getId(), product.getProductId());
            toHistoryViewModel.addToHistory(history);
        }
    }


}

import static com.marwaeltayeb.souq.utils.Constant.LOCALHOST;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.marwaeltayeb.souq.R;
import com.marwaeltayeb.souq.databinding.NewsfeedListItemBinding;
import com.marwaeltayeb.souq.model.NewsFeed;

import java.util.List;

public class NewsFeedAdapter extends RecyclerView.Adapter<NewsFeedAdapter.NewsFeedViewHolder>{

    private final Context mContext;
    private final List<NewsFeed> newsFeedList;

    public NewsFeedAdapter(Context mContext, List<NewsFeed> newsFeedList) {
        this.mContext = mContext;
        this.newsFeedList = newsFeedList;
    }

    @NonNull
    @Override
    public NewsFeedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
        NewsfeedListItemBinding newsfeedListItemBinding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.newsfeed_list_item,parent,false);
        return new NewsFeedViewHolder(newsfeedListItemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull NewsFeedViewHolder holder, int position) {
        NewsFeed currentNewsFeed = newsFeedList.get(position);

        // Load poster into ImageView
        String posterUrl = LOCALHOST + currentNewsFeed.getImage().replaceAll("\\\\", "/");
        Glide.with(mContext)
                .load(posterUrl)
                .into(holder.binding.poster);
    }

    @Override
    public int getItemCount() {
        if (newsFeedList == null) {
            return 0;
        }
        return newsFeedList.size();
    }

    static class NewsFeedViewHolder extends RecyclerView.ViewHolder {

        private final NewsfeedListItemBinding binding;

        private NewsFeedViewHolder(NewsfeedListItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

}


import static com.marwaeltayeb.souq.utils.Constant.LOCALHOST;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import com.marwaeltayeb.souq.R;
import com.marwaeltayeb.souq.databinding.WishlistItemBinding;
import com.marwaeltayeb.souq.model.Cart;
import com.marwaeltayeb.souq.model.Product;
import com.marwaeltayeb.souq.storage.LoginUtils;
import com.marwaeltayeb.souq.utils.RequestCallback;
import com.marwaeltayeb.souq.viewmodel.FromCartViewModel;
import com.marwaeltayeb.souq.viewmodel.RemoveFavoriteViewModel;
import com.marwaeltayeb.souq.viewmodel.ToCartViewModel;

import java.text.DecimalFormat;
import java.util.List;

public class WishListAdapter extends RecyclerView.Adapter<WishListAdapter.WishListViewHolder> {

    private final Context mContext;
    private final List<Product> favoriteList;

    private Product currentProduct;

    private final RemoveFavoriteViewModel removeFavoriteViewModel;
    private final ToCartViewModel toCartViewModel;
    private final FromCartViewModel fromCartViewModel;

    private WishListAdapter.WishListAdapterOnClickHandler clickHandler;

    /**
     * The interface that receives onClick messages.
     */
    public interface WishListAdapterOnClickHandler {
        void onClick(Product product);
    }

    public WishListAdapter(Context mContext, List<Product> favoriteList, WishListAdapter.WishListAdapterOnClickHandler clickHandler, FragmentActivity activity) {
        this.mContext = mContext;
        this.favoriteList = favoriteList;
        this.clickHandler = clickHandler;
        removeFavoriteViewModel = ViewModelProviders.of(activity).get(RemoveFavoriteViewModel.class);
        toCartViewModel = ViewModelProviders.of(activity).get(ToCartViewModel.class);
        fromCartViewModel = ViewModelProviders.of(activity).get(FromCartViewModel.class);
    }

    @NonNull
    @Override
    public WishListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
        WishlistItemBinding wishlistItemBinding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.wishlist_item, parent, false);
        return new WishListViewHolder(wishlistItemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull WishListViewHolder holder, int position) {
        currentProduct = favoriteList.get(position);
        holder.binding.txtProductName.setText(currentProduct.getProductName());

        DecimalFormat formatter = new DecimalFormat("#,###,###");
        String formattedPrice = formatter.format(currentProduct.getProductPrice());
        holder.binding.txtProductPrice.setText(formattedPrice + " EGP");

        // Load the Product image into ImageView
        String imageUrl = LOCALHOST + currentProduct.getProductImage().replaceAll("\\\\", "/");
        Glide.with(mContext)
                .load(imageUrl)
                .into(holder.binding.imgProductImage);

        // If product is added to cart
        if (currentProduct.isInCart()==1) {
            holder.binding.imgCart.setImageResource(R.drawable.ic_shopping_cart_green);
        }

    }

    @Override
    public int getItemCount() {
        if (favoriteList == null) {
            return 0;
        }
        return favoriteList.size();
    }

    class WishListViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        // Create view instances
        private final WishlistItemBinding binding;

        private WishListViewHolder(WishlistItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            // Register a callback to be invoked when this view is clicked.
            itemView.setOnClickListener(this);
            binding.imgFavourite.setOnClickListener(this);
            binding.imgCart.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int position = getBindingAdapterPosition();
            // Get position of the product
            currentProduct = favoriteList.get(position);

            switch (v.getId()) {
                case R.id.card_view:
                    // Send product through click
                    clickHandler.onClick(currentProduct);
                    break;
                case R.id.imgFavourite:
                    deleteFavorite();
                    break;
                case R.id.imgCart:
                    toggleProductsInCart();
                    break;
                default: // should not get here
            }
        }

        private void deleteFavorite() {
            deleteFavoriteProduct(() -> {
                currentProduct.setIsFavourite(false);
                notifyDataSetChanged();
            });
            favoriteList.remove(getBindingAdapterPosition());
            notifyItemRemoved(getBindingAdapterPosition());
            notifyItemRangeChanged(getBindingAdapterPosition(), favoriteList.size());
            showSnackBar("Bookmark Removed");
        }

        private void toggleProductsInCart() {
            // If Product is not added to cart
            if (currentProduct.isInCart()!=1) {
                binding.imgCart.setImageResource(R.drawable.ic_shopping_cart_green);
                insertToCart(() -> {
                    currentProduct.setIsInCart(true);
                    notifyDataSetChanged();
                });
                showSnackBar("Added To Cart");
            } else {
                binding.imgCart.setImageResource(R.drawable.ic_add_shopping_cart);
                deleteFromCart(() -> {
                    currentProduct.setIsInCart(false);
                    notifyDataSetChanged();
                });
                showSnackBar("Removed From Cart");
            }
        }

        private void showSnackBar(String text) {
            Snackbar.make(itemView, text, Snackbar.LENGTH_SHORT).show();
        }

        private void deleteFavoriteProduct(RequestCallback callback) {
            removeFavoriteViewModel.removeFavorite(LoginUtils.getInstance(mContext).getUserInfo().getId(), currentProduct.getProductId(), callback);
        }

        private void insertToCart(RequestCallback callback) {
            Cart cart = new Cart(LoginUtils.getInstance(mContext).getUserInfo().getId(), currentProduct.getProductId());
            toCartViewModel.addToCart(cart, callback);
        }

        private void deleteFromCart(RequestCallback callback) {
            fromCartViewModel.removeFromCart(LoginUtils.getInstance(mContext).getUserInfo().getId(), currentProduct.getProductId(),callback);
        }
    }

}


import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.marwaeltayeb.souq.R;
import com.marwaeltayeb.souq.databinding.HelpListItemBinding;
import com.marwaeltayeb.souq.model.Help;

import java.util.List;

public class HelpAdapter extends RecyclerView.Adapter<HelpAdapter.HelpCenterHolder>{

    private final List<Help> helpList;

    public HelpAdapter(List<Help> helpList) {
        this.helpList = helpList;
    }

    @NonNull
    @Override
    public HelpCenterHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
        HelpListItemBinding helpListItemBinding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.help_list_item,parent,false);
        return new HelpCenterHolder(helpListItemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull HelpCenterHolder holder, int position) {
        Help currentHelp = helpList.get(position);
        holder.binding.txtQuestion.setText(currentHelp.getQuestion());
        holder.binding.txtAnswer.setText(String.valueOf(currentHelp.getAnswer()));

    }

    @Override
    public int getItemCount() {
        if (helpList == null) {
            return 0;
        }
        return helpList.size();
    }

    static class HelpCenterHolder extends RecyclerView.ViewHolder{

        private final HelpListItemBinding binding;

        private HelpCenterHolder(HelpListItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.marwaeltayeb.souq.R;

import java.util.List;

public class WordAdapter extends ArrayAdapter<String> {

    public WordAdapter(Context context, List<String> words) {
        super(context, 0, words);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View listItemView = convertView;

        if (listItemView == null) {
            listItemView = LayoutInflater.from(getContext()).inflate(R.layout.word_list_item, parent, false);
        }

        final String currentWord = getItem(position);

        TextView name = listItemView.findViewById(R.id.txtWord);
        name.setText(currentWord);

        return listItemView;
    }
}


import static com.marwaeltayeb.souq.utils.Constant.LOCALHOST;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import com.marwaeltayeb.souq.R;
import com.marwaeltayeb.souq.databinding.SearchListItemBinding;
import com.marwaeltayeb.souq.model.Cart;
import com.marwaeltayeb.souq.model.Favorite;
import com.marwaeltayeb.souq.model.History;
import com.marwaeltayeb.souq.model.Product;
import com.marwaeltayeb.souq.storage.LoginUtils;
import com.marwaeltayeb.souq.utils.RequestCallback;
import com.marwaeltayeb.souq.viewmodel.AddFavoriteViewModel;
import com.marwaeltayeb.souq.viewmodel.FromCartViewModel;
import com.marwaeltayeb.souq.viewmodel.RemoveFavoriteViewModel;
import com.marwaeltayeb.souq.viewmodel.ToCartViewModel;
import com.marwaeltayeb.souq.viewmodel.ToHistoryViewModel;

import java.text.DecimalFormat;
import java.util.List;

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.SearchViewHolder>{

    private final Context mContext;
    private final List<Product> productList;

    private Product currentProduct;
    private final AddFavoriteViewModel addFavoriteViewModel;
    private final RemoveFavoriteViewModel removeFavoriteViewModel;
    private final ToCartViewModel toCartViewModel;
    private final FromCartViewModel fromCartViewModel;
    private final ToHistoryViewModel toHistoryViewModel;

    // Create a final private SearchAdapterOnClickHandler called mClickHandler
    private final SearchAdapterOnClickHandler clickHandler;

    /**
     * The interface that receives onClick messages.
     */
    public interface SearchAdapterOnClickHandler {
        void onClick(Product product);
    }

    public SearchAdapter(Context mContext,List<Product> productList,SearchAdapterOnClickHandler clickHandler, FragmentActivity activity) {
        this.mContext = mContext;
        this.productList = productList;
        this.clickHandler = clickHandler;
        addFavoriteViewModel = ViewModelProviders.of(activity).get(AddFavoriteViewModel.class);
        removeFavoriteViewModel = ViewModelProviders.of(activity).get(RemoveFavoriteViewModel.class);
        toCartViewModel = ViewModelProviders.of(activity).get(ToCartViewModel.class);
        fromCartViewModel = ViewModelProviders.of(activity).get(FromCartViewModel.class);
        toHistoryViewModel = ViewModelProviders.of(activity).get(ToHistoryViewModel.class);
    }

    @NonNull
    @Override
    public SearchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
        SearchListItemBinding searchListItemBinding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()),R.layout.search_list_item,parent,false);
        return new SearchViewHolder(searchListItemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchViewHolder holder, int position) {
        currentProduct = productList.get(position);
        holder.binding.txtProductName.setText(currentProduct.getProductName());

        DecimalFormat formatter = new DecimalFormat("#,###,###");
        String formattedPrice = formatter.format(currentProduct.getProductPrice());
        holder.binding.txtProductPrice.setText(formattedPrice + " EGP");

        // Load the Product image into ImageView
        String imageUrl = LOCALHOST + currentProduct.getProductImage().replaceAll("\\\\", "/");
        Glide.with(mContext)
                .load(imageUrl)
                .into(holder.binding.imgProductImage);

        // If product is inserted
        if (currentProduct.isFavourite()==1){
            holder.binding.imgFavourite.setImageResource(R.drawable.ic_favorite_pink);
        }

        // If product is added to cart
        if (currentProduct.isInCart()==1) {
            holder.binding.imgCart.setImageResource(R.drawable.ic_shopping_cart_green);
        }
    }

    @Override
    public int getItemCount() {
        if (productList == null) {
            return 0;
        }
        return productList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    class SearchViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        // Create view instances
        private final SearchListItemBinding binding;

        private SearchViewHolder(SearchListItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            // Register a callback to be invoked when this view is clicked.
            itemView.setOnClickListener(this);
            binding.imgFavourite.setOnClickListener(this);
            binding.imgCart.setOnClickListener(this);
            binding.addToCart.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int position = getBindingAdapterPosition();
            // Get position of the product
            currentProduct = productList.get(position);

            switch (v.getId()) {
                case R.id.card_view:
                    // Send product through click
                    clickHandler.onClick(currentProduct);
                    insertProductToHistory();
                    break;
                case R.id.imgFavourite:
                    toggleFavourite();
                    break;
                case R.id.imgCart:
                    toggleProductsInCart();
                    break;
                case R.id.addToCart:
                    addToCart();
                    break;
                default: // Should not get here
            }
        }


        private void toggleFavourite() {
            // If favorite is not bookmarked
            if (currentProduct.isFavourite() != 1) {
                binding.imgFavourite.setImageResource(R.drawable.ic_favorite_pink);
                insertFavoriteProduct(() -> {
                    currentProduct.setIsFavourite(true);
                    notifyDataSetChanged();
                });
                showSnackBar("Bookmark Added");
            } else {
                binding.imgFavourite.setImageResource(R.drawable.ic_favorite_border);
                deleteFavoriteProduct(() -> {
                    currentProduct.setIsFavourite(false);
                    notifyDataSetChanged();
                });
                showSnackBar("Bookmark Removed");
            }
        }

        private void toggleProductsInCart() {
            // If Product is not added to cart
            if (currentProduct.isInCart() != 1) {
                binding.imgCart.setImageResource(R.drawable.ic_shopping_cart_green);
                insertToCart(() -> {
                    currentProduct.setIsInCart(true);
                    notifyDataSetChanged();
                });
                showSnackBar("Added To Cart");
            } else {
                binding.imgCart.setImageResource(R.drawable.ic_add_shopping_cart);
                deleteFromCart(() -> {
                    currentProduct.setIsInCart(false);
                    notifyDataSetChanged();
                });
                showSnackBar("Removed From Cart");
            }
        }

        private void addToCart() {
            // If Product is not added to cart
            if (currentProduct.isInCart() != 1) {
                binding.imgCart.setImageResource(R.drawable.ic_shopping_cart_green);
                insertToCart(() -> {
                    currentProduct.setIsInCart(true);
                    notifyDataSetChanged();
                });
                showSnackBar("Added To Cart");
            }
        }

        private void showSnackBar(String text) {
            Snackbar.make(itemView, text, Snackbar.LENGTH_SHORT).show();
        }

        private void insertFavoriteProduct(RequestCallback callback) {
            Favorite favorite = new Favorite(LoginUtils.getInstance(mContext).getUserInfo().getId(), currentProduct.getProductId());
            addFavoriteViewModel.addFavorite(favorite,callback);
        }

        private void deleteFavoriteProduct(RequestCallback callback) {
            removeFavoriteViewModel.removeFavorite(LoginUtils.getInstance(mContext).getUserInfo().getId(), currentProduct.getProductId(),callback);
        }

        private void insertToCart(RequestCallback callback) {
            Cart cart = new Cart(LoginUtils.getInstance(mContext).getUserInfo().getId(), currentProduct.getProductId());
            toCartViewModel.addToCart(cart, callback);
        }

        private void deleteFromCart(RequestCallback callback) {
            fromCartViewModel.removeFromCart(LoginUtils.getInstance(mContext).getUserInfo().getId(), currentProduct.getProductId(),callback);
        }

        private void insertProductToHistory() {
            History history = new History(LoginUtils.getInstance(mContext).getUserInfo().getId(), currentProduct.getProductId());
            toHistoryViewModel.addToHistory(history);
        }
    }
}


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.marwaeltayeb.souq.R;
import com.marwaeltayeb.souq.databinding.OrderListItemBinding;
import com.marwaeltayeb.souq.model.Order;

import java.text.DecimalFormat;
import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder>{

    private final List<Order> orderList;
    private Order currentOrder;

    private final OrderAdapter.OrderAdapterOnClickHandler clickHandler;

    /**
     * The interface that receives onClick messages.
     */
    public interface OrderAdapterOnClickHandler {
        void onClick(Order order);
    }

    public OrderAdapter(List<Order> orderList, OrderAdapter.OrderAdapterOnClickHandler clickHandler) {
        this.orderList = orderList;
        this.clickHandler = clickHandler;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
        OrderListItemBinding orderListItemBinding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.order_list_item,parent,false);
        return new OrderViewHolder(orderListItemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        currentOrder = orderList.get(position);

        DecimalFormat formatter = new DecimalFormat("#,###,###");
        String formattedPrice = formatter.format(currentOrder.getProductPrice());
        holder.binding.productPrice.setText(formattedPrice + " EGP");

        holder.binding.orderNumber.setText(currentOrder.getOrderNumber());
        holder.binding.orderDate.setText(currentOrder.getOrderDate());
    }

    @Override
    public int getItemCount() {
        if (orderList == null) {
            return 0;
        }
        return orderList.size();
    }

    class OrderViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        // Create view instances
        private final OrderListItemBinding binding;

        private OrderViewHolder(OrderListItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            // Register a callback to be invoked when this view is clicked.
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int position = getBindingAdapterPosition();
            // Get position of the product
            currentOrder = orderList.get(position);
            // Send product through click
            clickHandler.onClick(currentOrder);
        }
    }
}


import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.marwaeltayeb.souq.R;
import com.marwaeltayeb.souq.databinding.ReviewListItemBinding;
import com.marwaeltayeb.souq.model.Review;

import java.util.List;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    private final List<Review> reviewList;

    public ReviewAdapter(List<Review> reviewList) {
        this.reviewList = reviewList;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
        ReviewListItemBinding reviewListItemBinding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.review_list_item,parent,false);
        return new ReviewViewHolder(reviewListItemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review currentReview = reviewList.get(position);
        holder.binding.userName.setText(currentReview.getUserName());
        holder.binding.dateOfReview.setText(currentReview.getReviewDate());
        holder.binding.rateProduct.setRating(currentReview.getReviewRate());
        holder.binding.userFeedback.setText(currentReview.getFeedback());
    }

    @Override
    public int getItemCount() {
        if (reviewList == null) {
            return 0;
        }
        return reviewList.size();
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        // Create view instances
        private final ReviewListItemBinding binding;

        private ReviewViewHolder(ReviewListItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}


import static com.marwaeltayeb.souq.utils.Constant.LOCALHOST;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import com.marwaeltayeb.souq.R;
import com.marwaeltayeb.souq.databinding.CartListItemBinding;
import com.marwaeltayeb.souq.model.Favorite;
import com.marwaeltayeb.souq.model.Product;
import com.marwaeltayeb.souq.storage.LoginUtils;
import com.marwaeltayeb.souq.utils.RequestCallback;
import com.marwaeltayeb.souq.viewmodel.AddFavoriteViewModel;
import com.marwaeltayeb.souq.viewmodel.FromCartViewModel;
import com.marwaeltayeb.souq.viewmodel.RemoveFavoriteViewModel;

import java.text.DecimalFormat;
import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private final Context mContext;

    private final List<Product> productsInCart;

    private Product currentProduct;

    private final AddFavoriteViewModel addFavoriteViewModel;
    private final RemoveFavoriteViewModel removeFavoriteViewModel;
    private final FromCartViewModel fromCartViewModel;

    private final CartAdapter.CartAdapterOnClickHandler clickHandler;

    /**
     * The interface that receives onClick messages.
     */
    public interface CartAdapterOnClickHandler {
        void onClick(Product product);
    }

    public CartAdapter(Context mContext, List<Product> productInCart, CartAdapter.CartAdapterOnClickHandler clickHandler, FragmentActivity activity) {
        this.mContext = mContext;
        this.productsInCart = productInCart;
        this.clickHandler = clickHandler;
        addFavoriteViewModel = ViewModelProviders.of(activity).get(AddFavoriteViewModel.class);
        removeFavoriteViewModel = ViewModelProviders.of(activity).get(RemoveFavoriteViewModel.class);
        fromCartViewModel = ViewModelProviders.of(activity).get(FromCartViewModel.class);
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
        CartListItemBinding cartListItemBinding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.cart_list_item, parent, false);
        return new CartViewHolder(cartListItemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        currentProduct = productsInCart.get(position);
        holder.binding.txtProductName.setText(currentProduct.getProductName());

        DecimalFormat formatter = new DecimalFormat("#,###,###");
        String formattedPrice = formatter.format(currentProduct.getProductPrice());
        holder.binding.txtProductPrice.setText(formattedPrice + " EGP");

        // Load the Product image into ImageView
        String imageUrl = LOCALHOST + currentProduct.getProductImage().replaceAll("\\\\", "/");
        Glide.with(mContext)
                .load(imageUrl)
                .into(holder.binding.imgProductImage);

        // If product is inserted
        if (currentProduct.isFavourite() == 1) {
            holder.binding.imgFavourite.setImageResource(R.drawable.ic_favorite_pink);
        }
    }

    @Override
    public int getItemCount() {
        if (productsInCart == null) {
            return 0;
        }
        return productsInCart.size();
    }

    class CartViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        // Create view instances
        private final CartListItemBinding binding;

        private CartViewHolder(CartListItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            // Register a callback to be invoked when this view is clicked.
            itemView.setOnClickListener(this);
            binding.imgFavourite.setOnClickListener(this);
            binding.imgCart.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int position = getBindingAdapterPosition();
            // Get position of the product
            currentProduct = productsInCart.get(position);

            switch (v.getId()) {
                case R.id.card_view:
                    // Send product through click
                    clickHandler.onClick(currentProduct);
                    break;
                case R.id.imgFavourite:
                    toggleFavourite();
                    break;
                case R.id.imgCart:
                    deleteProductsInCart();
                    break;
                default: // Should not get here
            }
        }

        private void toggleFavourite() {
            // If favorite is not bookmarked
            if (currentProduct.isFavourite() != 1) {
                binding.imgFavourite.setImageResource(R.drawable.ic_favorite_pink);
                insertFavoriteProduct(() -> {
                    currentProduct.setIsFavourite(true);
                    notifyDataSetChanged();
                });
                showSnackBar("Bookmark Added");
            } else {
                binding.imgFavourite.setImageResource(R.drawable.ic_favorite_border);
                deleteFavoriteProduct(() -> {
                    currentProduct.setIsFavourite(false);
                    notifyDataSetChanged();
                });
                showSnackBar("Bookmark Removed");
            }
        }

        private void deleteProductsInCart() {
            deleteFromCart(() -> {
                currentProduct.setIsInCart(false);
                notifyDataSetChanged();
            });
            productsInCart.remove(getBindingAdapterPosition());
            notifyItemRemoved(getBindingAdapterPosition());
            notifyItemRangeChanged(getBindingAdapterPosition(), productsInCart.size());
            showSnackBar("Removed From Cart");
        }

        private void showSnackBar(String text) {
            Snackbar.make(itemView, text, Snackbar.LENGTH_SHORT).show();
        }

        private void insertFavoriteProduct(RequestCallback callback) {
            Favorite favorite = new Favorite(LoginUtils.getInstance(mContext).getUserInfo().getId(), currentProduct.getProductId());
            addFavoriteViewModel.addFavorite(favorite, callback);
        }

        private void deleteFavoriteProduct(RequestCallback callback) {
            removeFavoriteViewModel.removeFavorite(LoginUtils.getInstance(mContext).getUserInfo().getId(), currentProduct.getProductId(), callback);
        }

        private void deleteFromCart(RequestCallback callback) {
            fromCartViewModel.removeFromCart(LoginUtils.getInstance(mContext).getUserInfo().getId(), currentProduct.getProductId(), callback);
        }
    }
}


import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class CommunicateUtils {

    private CommunicateUtils(){}

    public static void shareApp(Context context) {
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, "https://play.google.com/store/apps/details?id=com.android.chrome&fbclid=IwAR2OYOl_XZ9k7AP68xhNjEZnL1OXWiiWucNT1QsPTthr-IHr-5G1_0AH1AA");
        context.startActivity(Intent.createChooser(sharingIntent, "Share using"));
    }


    public static void rateAppOnGooglePlay(Context context) {
        Uri uri = Uri.parse("market://details?id=" + "com.android.chrome");
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        // To count with Play market backstack, After pressing back button,
        // to taken back to our application, we need to add following flags to intent.
        goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        try {
            context.startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            context.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=" + "com.android.chrome")));
        }
    }
}


import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.annotation.RequiresApi;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.OutputStream;

public class ImageUtils {

    private ImageUtils(){}

    // And to convert the image URI to the direct file system path of the image file
    public static String getRealPathFromURI(Context context, Uri contentUri) {
        // can post image
        String [] projection ={MediaStore.Images.Media.DATA};
        Cursor cursor = context.getContentResolver().query( contentUri,
                projection , // Which columns to return
                null,       // WHERE clause; which rows to return (all rows)
                null,       // WHERE clause selection arguments (none)
                null); // Order-by clause (ascending by name)
        int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();

        String result = cursor.getString(columnIndex);
        cursor.close();

        return result;
    }


    @RequiresApi(api = Build.VERSION_CODES.Q)
    public static Uri getImageUriQ(Context context, Bitmap bitmap) {

        String filename = "IMG_$"+ System.currentTimeMillis()+".jpg";

        OutputStream fos = null;
        Uri imageUri;

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg");
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
        contentValues.put(MediaStore.Video.Media.IS_PENDING, 1);

        ContentResolver contentResolver = context.getContentResolver();

        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        try {
            fos = contentResolver.openOutputStream(imageUri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, fos);
        contentValues.clear();
        contentValues.put(MediaStore.Video.Media.IS_PENDING, 0);

        contentResolver.update(imageUri, contentValues, null, null);

        return imageUri;
    }

    public static Uri getImageUriBeforeQ(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    public static Uri getImageUri(Context context, Bitmap photo){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return getImageUriQ(context, photo);
        } else {
            return getImageUriBeforeQ(context, photo);
        }
    }




}


public interface OnNetworkListener {
    void onNetworkConnected();
    void onNetworkDisconnected();
}


import android.util.Patterns;

public class Validation {

    private static final int PASSWORD_MIN_LENGTH = 8;
    private static final int NAME_MIN_LENGTH = 3;

    private Validation(){ }

    public static boolean isValidEmail(String email){
        return !Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public static boolean isValidPassword(String password){
        return password.length() >= PASSWORD_MIN_LENGTH;
    }

    public static boolean isValidName(String name){
        return name.length() >= NAME_MIN_LENGTH;
    }
}


public class FlagsManager {

    private static FlagsManager instance = null;

    private boolean isHistoryDeleted = false;
    private boolean isActivityRunning = false;

    public static FlagsManager getInstance() {
        if (instance == null)
            instance = new FlagsManager();

        return instance;
    }

    public boolean isHistoryDeleted() {
        return isHistoryDeleted;
    }

    public void setHistoryDeleted(boolean historyDeleted) {
        isHistoryDeleted = historyDeleted;
    }

    public boolean isActivityRunning() {
        return isActivityRunning;
    }

    public void setActivityRunning(boolean activityRunning) {
        isActivityRunning = activityRunning;
    }
}





import android.app.Activity;
import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;

import com.marwaeltayeb.souq.R;

public class ProgressDialog {

    private ProgressDialog(){}

    public static AlertDialog createAlertDialog(Activity activity){
        LayoutInflater inflater = activity.getLayoutInflater();
        View dialogLayout = inflater.inflate(R.layout.custom_progress_dialog, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setView(dialogLayout);
        AlertDialog alert = builder.create();
        alert.show();
        alert.getWindow().setLayout(600, 300);
        return alert;
    }
}


public interface RequestCallback {
    void onCallBack();
}


public class Constant {

    private Constant(){}

    public static final String LOCALHOST = "http://192.168.1.6:3000/";

    // Constant Variables
    public static final String PRODUCT = "product";
    public  static final String ORDER = "order";

    public static final int READ_EXTERNAL_STORAGE_CODE = 200;

    public static final int CAMERA_PERMISSION_CODE = 400;


    public static final String PRODUCT_ID = "ProductId";

    public static final String CATEGORY = "Category";


    public static final String EMAIL = "email";
    public static final String OTP = "otp";

    public static final String PRODUCTID = "Product_id";

    public static final String KEYWORD = "keyword";
}

import android.content.Context;
import android.content.Intent;

public class Utils {

    private Utils(){}

    public static void shareProduct(Context context,String productName ,String url) {
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, "Hey, Check out this amazing item '" + productName +"' with its photo at "+ url);
        context.startActivity(Intent.createChooser(sharingIntent, "Share using"));
    }
}


import com.marwaeltayeb.souq.R;

import java.util.ArrayList;
import java.util.List;

public class Slide {

    private Slide(){}

    private static final List<Integer> slides = new ArrayList<>();

    static {
        slides.add(R.drawable.slide1);
        slides.add(R.drawable.slide2);
        slides.add(R.drawable.slide3);
        slides.add(R.drawable.slide4);
        slides.add(R.drawable.slide5);
        slides.add(R.drawable.slide6);
        slides.add(R.drawable.slide7);
    }

    public static List<Integer> getSlides() {
        return slides;
    }
}


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;

public class InternetUtils {

    private InternetUtils(){}

    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) return false;
            NetworkCapabilities actNw = connectivityManager.getNetworkCapabilities(network);
            return actNw != null && (actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH));
        } else {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        }
    }
}


import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.marwaeltayeb.souq.model.Cart;
import com.marwaeltayeb.souq.net.RetrofitClient;
import com.marwaeltayeb.souq.utils.RequestCallback;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ToCartRepository {

    private static final String TAG = ToCartRepository.class.getSimpleName();

    public LiveData<ResponseBody> addToCart(Cart cart, RequestCallback callback) {
        final MutableLiveData<ResponseBody> mutableLiveData = new MutableLiveData<>();
        RetrofitClient.getInstance().getApi().addToCart(cart).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Log.d(TAG , "onResponse" + response.code());

                if(response.code() == 200){
                    callback.onCallBack();
                }

                ResponseBody responseBody = response.body();

                if (response.body() != null) {
                    mutableLiveData.setValue(responseBody);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.d(TAG,"onFailure"  + t.getMessage());
            }
        });
        return mutableLiveData;
    }
}


import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.marwaeltayeb.souq.model.FavoriteApiResponse;
import com.marwaeltayeb.souq.net.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FavoriteRepository {

    private static final String TAG = FavoriteRepository.class.getSimpleName();

    public LiveData<FavoriteApiResponse> getFavorites(int userId) {
        final MutableLiveData<FavoriteApiResponse> mutableLiveData = new MutableLiveData<>();

        RetrofitClient.getInstance().getApi().getFavorites(userId).enqueue(new Callback<FavoriteApiResponse>() {
            @Override
            public void onResponse(Call<FavoriteApiResponse> call, Response<FavoriteApiResponse> response) {
                Log.d(TAG, "onResponse: Succeeded");

                FavoriteApiResponse favoriteApiResponse = response.body();

                if (response.body() != null) {
                    mutableLiveData.setValue(favoriteApiResponse);
                    Log.d(TAG, String.valueOf(response.body().getFavorites()));
                }
            }

            @Override
            public void onFailure(Call<FavoriteApiResponse> call, Throwable t) {
                Log.d(TAG, "onFailure: " + t.getMessage());
            }
        });

        return mutableLiveData;
    }
}


import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.marwaeltayeb.souq.model.RegisterApiResponse;
import com.marwaeltayeb.souq.model.User;
import com.marwaeltayeb.souq.net.RetrofitClient;

import retrofit2.Callback;
import retrofit2.Response;

public class RegisterRepository {

    private static final String TAG = RegisterRepository.class.getSimpleName();

    public LiveData<RegisterApiResponse> getRegisterResponseData(User user) {
        final MutableLiveData<RegisterApiResponse> mutableLiveData = new MutableLiveData<>();

        RetrofitClient.getInstance().getApi().createUser(user).enqueue(new Callback<RegisterApiResponse>() {
            @Override
            public void onResponse(retrofit2.Call<RegisterApiResponse> call, Response<RegisterApiResponse> response) {
                Log.d(TAG, "onResponse: Succeeded");

                RegisterApiResponse registerApiResponse = response.body();

                if (response.body() != null) {
                    mutableLiveData.setValue(registerApiResponse);
                    Log.d(TAG, response.body().getMessage());
                }
            }

            @Override
            public void onFailure(retrofit2.Call<RegisterApiResponse> call, Throwable t) {
                Log.d(TAG, t.getMessage());
            }
        });
        return mutableLiveData;
    }
}


import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.marwaeltayeb.souq.model.History;
import com.marwaeltayeb.souq.net.RetrofitClient;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ToHistoryRepository {

    private static final String TAG = ToHistoryRepository.class.getSimpleName();

    public LiveData<ResponseBody> addToHistory(History history) {
        final MutableLiveData<ResponseBody> mutableLiveData = new MutableLiveData<>();
        RetrofitClient.getInstance().getApi().addToHistory(history).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Log.d(TAG, "onResponse" + response.code());

                ResponseBody responseBody = response.body();

                if (response.body() != null) {
                    mutableLiveData.setValue(responseBody);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.d(TAG, "onFailure" + t.getMessage());
            }
        });
        return mutableLiveData;
    }
}


import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.marwaeltayeb.souq.model.Shipping;
import com.marwaeltayeb.souq.net.RetrofitClient;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ShippingRepository {

    private static final String TAG = ShippingRepository.class.getSimpleName();

    public LiveData<ResponseBody> addShippingAddress(Shipping shipping) {
        final MutableLiveData<ResponseBody> mutableLiveData = new MutableLiveData<>();

        RetrofitClient.getInstance().getApi().addShippingAddress(shipping).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Log.d(TAG, "onResponse: " + response.body());

                ResponseBody responseBody = response.body();

                if (response.body() != null) {
                    mutableLiveData.setValue(responseBody);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.d(TAG, "onFailure: " + t.getMessage());
            }
        });

        return mutableLiveData;
    }




}


import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.marwaeltayeb.souq.model.Ordering;
import com.marwaeltayeb.souq.net.RetrofitClient;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderingRepository {

    private static final String TAG = OrderingRepository.class.getSimpleName();

    public LiveData<ResponseBody> orderProduct(Ordering ordering) {
        final MutableLiveData<ResponseBody> mutableLiveData = new MutableLiveData<>();

        RetrofitClient.getInstance().getApi().orderProduct(ordering).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Log.d(TAG, "onResponse: " + response.body());

                ResponseBody responseBody = response.body();

                if (response.body() != null) {
                    mutableLiveData.setValue(responseBody);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.d(TAG, "onFailure: " + t.getMessage());
            }
        });

        return mutableLiveData;
    }


}


import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.marwaeltayeb.souq.model.ReviewApiResponse;
import com.marwaeltayeb.souq.net.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReviewRepository {

    private static final String TAG = ReviewRepository.class.getSimpleName();
    
    public LiveData<ReviewApiResponse> getReviews(int productId) {
        final MutableLiveData<ReviewApiResponse> mutableLiveData = new MutableLiveData<>();
        RetrofitClient.getInstance().getApi().getAllReviews(productId).enqueue(new Callback<ReviewApiResponse>() {
            @Override
            public void onResponse(Call<ReviewApiResponse> call, Response<ReviewApiResponse> response) {
                Log.d("onResponse",response.code() + "");

                ReviewApiResponse responseBody = response.body();

                if (response.body() != null) {
                    mutableLiveData.setValue(responseBody);
                }
            }

            @Override
            public void onFailure(Call<ReviewApiResponse> call, Throwable t) {
                Log.d(TAG, "onFailure: " + t.getMessage());
            }
        });
        return mutableLiveData;
    }
}



import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.marwaeltayeb.souq.net.RetrofitClient;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DeleteUserRepository{

    private static final String TAG = DeleteUserRepository.class.getSimpleName();

    public LiveData<ResponseBody> deleteUser(String token, int userId) {
        final MutableLiveData<ResponseBody> mutableLiveData = new MutableLiveData<>();
        RetrofitClient.getInstance().getApi().deleteAccount(token, userId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                Log.d(TAG, "onResponse: Succeeded");
                ResponseBody responseBody = response.body();
                if (response.body() != null) {
                    mutableLiveData.setValue(responseBody);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Log.d(TAG, "onFailure: " + t.getMessage());
            }
        });

        return mutableLiveData;
    }
}


import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.marwaeltayeb.souq.net.RetrofitClient;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FromHistoryRepository {

    private static final String TAG = FromHistoryRepository.class.getSimpleName();

    public LiveData<ResponseBody> removeAllFromHistory() {
        final MutableLiveData<ResponseBody> mutableLiveData = new MutableLiveData<>();
        RetrofitClient.getInstance().getApi().removeAllFromHistory().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Log.d(TAG, "onResponse" + response.code());

                ResponseBody responseBody = response.body();

                if (response.body() != null) {
                    mutableLiveData.setValue(responseBody);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.d(TAG, "onFailure" + t.getMessage());
            }
        });
        return mutableLiveData;

    }
}


import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.marwaeltayeb.souq.model.Otp;
import com.marwaeltayeb.souq.net.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OtpRepository {

    private static final String TAG = OtpRepository.class.getSimpleName();

    public LiveData<Otp> getOtpCode(String token , String email) {
        final MutableLiveData<Otp> mutableLiveData = new MutableLiveData<>();

        RetrofitClient.getInstance().getApi().getOtp(token,email).enqueue(new Callback<Otp>() {
            @Override
            public void onResponse(@NonNull Call<Otp> call, @NonNull Response<Otp> response) {

                Log.d(TAG, "onResponse: Succeeded");

                Otp otp;
                if (response.code() == 200) {
                    otp = response.body();
                } else {
                    otp = new Otp("Incorrect Email");
                }
                mutableLiveData.setValue(otp);

            }

            @Override
            public void onFailure(@NonNull Call<Otp> call, @NonNull Throwable t) {
                Log.d(TAG, "onFailure: " + t.getMessage());
            }
        });

        return mutableLiveData;
    }
}


import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.marwaeltayeb.souq.model.CartApiResponse;
import com.marwaeltayeb.souq.net.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CartRepository {

    private static final String TAG = CartRepository.class.getSimpleName();

    public LiveData<CartApiResponse> getProductsInCart(int userId) {
        final MutableLiveData<CartApiResponse> mutableLiveData = new MutableLiveData<>();

        RetrofitClient.getInstance().getApi().getProductsInCart(userId).enqueue(new Callback<CartApiResponse>() {
            @Override
            public void onResponse(Call<CartApiResponse> call, Response<CartApiResponse> response) {
                Log.d(TAG, "onResponse: Succeeded");

                CartApiResponse cartApiResponse = response.body();

                if (response.body() != null) {
                    mutableLiveData.setValue(cartApiResponse);
                    Log.d(TAG, String.valueOf(response.body().getProductsInCart()));
                }
            }

            @Override
            public void onFailure(Call<CartApiResponse> call, Throwable t) {
                Log.d(TAG, "onFailure: " + t.getMessage());
            }
        });
        return mutableLiveData;
    }
}


import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.marwaeltayeb.souq.model.Favorite;
import com.marwaeltayeb.souq.net.RetrofitClient;
import com.marwaeltayeb.souq.utils.RequestCallback;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddFavoriteRepository {

    private static final String TAG = AddFavoriteRepository.class.getSimpleName();

    public LiveData<ResponseBody> addFavorite(Favorite favorite, RequestCallback callback) {
        final MutableLiveData<ResponseBody> mutableLiveData = new MutableLiveData<>();
        RetrofitClient.getInstance().getApi().addFavorite(favorite).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Log.d(TAG, "onResponse" + response.code());

                ResponseBody responseBody = response.body();

                if(response.code() == 200){
                    callback.onCallBack();
                }

                if (response.body() != null) {
                    mutableLiveData.setValue(responseBody);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.d(TAG, "onFailure" + t.getMessage());
            }
        });
        return mutableLiveData;
    }

}


import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.marwaeltayeb.souq.model.LoginApiResponse;
import com.marwaeltayeb.souq.net.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginRepository {

    private static final String TAG = LoginRepository.class.getSimpleName();

    public LiveData<LoginApiResponse> getLoginResponseData(String email, String password) {
        final MutableLiveData<LoginApiResponse> mutableLiveData = new MutableLiveData<>();
        RetrofitClient.getInstance().getApi().logInUser(email, password).enqueue(new Callback<LoginApiResponse>() {
            @Override
            public void onResponse(Call<LoginApiResponse> call, Response<LoginApiResponse> response) {
                Log.d(TAG, "onResponse: Succeeded");

                LoginApiResponse loginResponse;
                if(response.code() == 200){
                    loginResponse = response.body();
                }else if (response.code() == 214){
                    // Add Custom message
                    loginResponse = new LoginApiResponse("Account does not exist");
                }else {
                    loginResponse = new LoginApiResponse("Incorrect Password");
                }
                mutableLiveData.setValue(loginResponse);
            }

            @Override
            public void onFailure(Call<LoginApiResponse> call, Throwable t) {
                Log.d(TAG, t.getMessage());
            }
        });

        return mutableLiveData;
    }
}


import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.marwaeltayeb.souq.net.RetrofitClient;
import com.marwaeltayeb.souq.utils.RequestCallback;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RemoveFavoriteRepository {

    private static final String TAG = RemoveFavoriteRepository.class.getSimpleName();

    public LiveData<ResponseBody> removeFavorite(int userId, int productId, RequestCallback callback) {
        final MutableLiveData<ResponseBody> mutableLiveData = new MutableLiveData<>();
        RetrofitClient.getInstance().getApi().removeFavorite(userId,productId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Log.d(TAG, "onResponse" + response.code());

                if(response.code() == 200){
                    callback.onCallBack();
                }

                ResponseBody responseBody = response.body();

                if (response.body() != null) {
                    mutableLiveData.setValue(responseBody);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.d(TAG, "onFailure"+ t.getMessage());
            }
        });
        return mutableLiveData;
    }
}


import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.marwaeltayeb.souq.model.Review;
import com.marwaeltayeb.souq.net.RetrofitClient;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WriteReviewRepository {

    private static final String TAG = WriteReviewRepository.class.getSimpleName();

    public LiveData<ResponseBody> writeReview(Review review) {
        final MutableLiveData<ResponseBody> mutableLiveData = new MutableLiveData<>();
        RetrofitClient.getInstance().getApi().addReview(review).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Log.d(TAG, "onResponse: " + response.body());

                ResponseBody responseBody = response.body();

                if (response.body() != null) {
                    mutableLiveData.setValue(responseBody);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.d(TAG, "onFailure: " + t.getMessage());
            }
        });

        return mutableLiveData;
    }
}


import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.marwaeltayeb.souq.model.OrderApiResponse;
import com.marwaeltayeb.souq.net.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderRepository {

    private static final String TAG = OrderRepository.class.getSimpleName();

    public LiveData<OrderApiResponse> getOrders(int userId) {
        final MutableLiveData<OrderApiResponse> mutableLiveData = new MutableLiveData<>();
        RetrofitClient.getInstance().getApi().getOrders(userId).enqueue(new Callback<OrderApiResponse>() {
            @Override
            public void onResponse(Call<OrderApiResponse> call, Response<OrderApiResponse> response) {
                Log.d("onResponse", "" + response.code());

                OrderApiResponse responseBody = response.body();

                if (response.body() != null) {
                    mutableLiveData.setValue(responseBody);
                }
            }

            @Override
            public void onFailure(Call<OrderApiResponse> call, Throwable t) {
                Log.d(TAG, "onFailure: " + t.getMessage());
            }
        });


        return mutableLiveData;
    }


}

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.marwaeltayeb.souq.model.Image;
import com.marwaeltayeb.souq.net.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserImageRepository {

    private static final String TAG = UserImageRepository.class.getSimpleName();

    public LiveData<Image> getUserImage(int userId) {
        final MutableLiveData<Image> mutableLiveData = new MutableLiveData<>();
        RetrofitClient.getInstance().getApi().getUserImage(userId).enqueue(new Callback<Image>() {
            @Override
            public void onResponse(Call<Image> call, Response<Image> response) {
                Log.d("onResponse", "" + response.code());

                Image responseBody = response.body();

                if (response.body() != null) {
                    mutableLiveData.setValue(responseBody);
                }
            }

            @Override
            public void onFailure(Call<Image> call, Throwable t) {
                Log.d(TAG, "onFailure: "+ t.getMessage());
            }
        });
        return mutableLiveData;
    }


}


import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.marwaeltayeb.souq.net.RetrofitClient;

import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddProductRepository {

    private static final String TAG = AddProductRepository.class.getSimpleName();

    public LiveData<ResponseBody> addProduct(String token, Map<String, RequestBody> productInfo, MultipartBody.Part image) {
        final MutableLiveData<ResponseBody> mutableLiveData = new MutableLiveData<>();

        RetrofitClient.getInstance().getApi().insertProduct(token,productInfo,image).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Log.d(TAG, "onResponse: " + "Product Inserted");

                ResponseBody responseBody = response.body();

                if (response.body() != null) {
                    mutableLiveData.setValue(responseBody);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.d(TAG, "onFailure: " + t.getMessage());
            }
        });
        return mutableLiveData;
    }
}


import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.marwaeltayeb.souq.net.RetrofitClient;
import com.marwaeltayeb.souq.utils.RequestCallback;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FromCartRepository {

    private static final String TAG = FromCartRepository.class.getSimpleName();

    public LiveData<ResponseBody> removeFromCart(int userId, int productId, RequestCallback callback) {
        final MutableLiveData<ResponseBody> mutableLiveData = new MutableLiveData<>();
        RetrofitClient.getInstance().getApi().removeFromCart(userId, productId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Log.d(TAG,"onResponse" + response.code());

                if(response.code() == 200){
                    callback.onCallBack();
                }

                ResponseBody responseBody = response.body();

                if (response.body() != null) {
                    mutableLiveData.setValue(responseBody);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.d(TAG,"onFailure" + t.getMessage());
            }
        });

        return mutableLiveData;
    }
}


import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.marwaeltayeb.souq.model.NewsFeedResponse;
import com.marwaeltayeb.souq.net.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NewsFeedRepository {

    private static final String TAG = NewsFeedRepository.class.getSimpleName();

    public LiveData<NewsFeedResponse> getPosters() {
        final MutableLiveData<NewsFeedResponse> mutableLiveData = new MutableLiveData<>();

        RetrofitClient.getInstance().getApi().getPosters().enqueue(new Callback<NewsFeedResponse>() {
            @Override
            public void onResponse(Call<NewsFeedResponse> call, Response<NewsFeedResponse> response) {

                Log.d("onResponse", "" + response.code());

                NewsFeedResponse responseBody = response.body();

                if (response.body() != null) {
                    mutableLiveData.setValue(responseBody);
                }
            }

            @Override
            public void onFailure(Call<NewsFeedResponse> call, Throwable t) {
                Log.d(TAG, "onFailure: " + t.getMessage());
            }
        });

        return mutableLiveData;
    }
}


import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.marwaeltayeb.souq.net.RetrofitClient;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PasswordRepository {

    private static final String TAG = PasswordRepository.class.getSimpleName();

    public LiveData<ResponseBody> updatePassword(String token, String newPassword, int userId) {
        final MutableLiveData<ResponseBody> mutableLiveData = new MutableLiveData<>();
        RetrofitClient.getInstance().getApi().updatePassword(token, newPassword, userId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Log.d(TAG,"onResponse"+ response.code());

                ResponseBody responseBody = response.body();

                if (response.body() != null) {
                    mutableLiveData.setValue(responseBody);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.d(TAG, "onFailure" + t.getMessage());
            }
        });

        return mutableLiveData;
    }


}


import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.marwaeltayeb.souq.model.ProductApiResponse;
import com.marwaeltayeb.souq.net.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchRepository {

    private static final String TAG = SearchRepository.class.getSimpleName();

    public LiveData<ProductApiResponse> getResponseDataBySearch(String keyword, int userId) {
        final MutableLiveData<ProductApiResponse> mutableLiveData = new MutableLiveData<>();

        RetrofitClient.getInstance()
                .getApi().searchForProduct(keyword, userId)
                .enqueue(new Callback<ProductApiResponse>() {
                    @Override
                    public void onResponse(Call<ProductApiResponse> call, Response<ProductApiResponse> response) {
                        Log.d(TAG, "onResponse: Succeeded");

                        ProductApiResponse productApiResponse = response.body();

                        if (response.body() != null) {
                            mutableLiveData.setValue(productApiResponse);
                            Log.d(TAG, String.valueOf(response.body().getProducts()));
                        }
                    }

                    @Override
                    public void onFailure(Call<ProductApiResponse> call, Throwable t) {
                        Log.v("onFailure", " Failed to get products");
                    }
                });
        return mutableLiveData;
    }
}


import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.marwaeltayeb.souq.net.RetrofitClient;
import com.marwaeltayeb.souq.storage.LoginUtils;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UploadPhotoRepository {

    private static final String TAG = UploadPhotoRepository.class.getSimpleName();
    private final Application application;

    public UploadPhotoRepository(Application application) {
        this.application = application;
    }

    public LiveData<ResponseBody> uploadPhoto(String pathname) {
        final MutableLiveData<ResponseBody> mutableLiveData = new MutableLiveData<>();

        File file = new File(pathname);

        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);

        MultipartBody.Part photo = MultipartBody.Part.createFormData("image", file.getName(), requestFile);

        RequestBody id = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(LoginUtils.getInstance(application).getUserInfo().getId()));

        String token = String.valueOf(LoginUtils.getInstance(application).getUserToken());
        Log.d(TAG, "token: " + token);

        RetrofitClient.getInstance().getApi().uploadPhoto(token,photo, id).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Log.d(TAG, "onResponse: " + "Image Updated");

                ResponseBody responseBody = response.body();

                if (response.body() != null) {
                    mutableLiveData.setValue(responseBody);
                }

                if (response.code()!= 200) {
                    //
                }

            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.d(TAG, "onFailure: " + t.getMessage());
            }
        });

        return mutableLiveData;
    }
}


public class User {

    private int id;
    private String name;
    private String email;
    private String password;
    private boolean isAdmin;

    public User(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }

    public User(int id,String name, String email, String password, boolean isAdmin) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.isAdmin = isAdmin;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

}


public class LoginApiResponse {

    private int id;
    private String name;
    private String email;
    private boolean error;
    private String message;
    private String password;
    private String token;
    private boolean isAdmin;

    public LoginApiResponse(String message) {
        this.message = message;
        this.error = true;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public boolean isError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public String getPassword() {
        return password;
    }

    public String getToken() {
        return token;
    }

    public boolean isAdmin() {
        return isAdmin;
    }
}


import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ReviewApiResponse {

    @SerializedName("avrg_review")
    private float averageReview;

    @SerializedName("review")
    private List<Review> reviewList;

    public float getAverageReview() {
        return averageReview;
    }

    public List<Review> getReviewList() {
        return reviewList;
    }

}


import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Order implements Serializable {

    @SerializedName("id")
    private int productId;
    @SerializedName("product_name")
    private String productName;
    @SerializedName("order_number")
    private String orderNumber;
    @SerializedName("order_date")
    private String orderDate;
    @SerializedName("price")
    private double productPrice;
    @SerializedName("status")
    private String orderDateStatus;
    @SerializedName("name")
    private String userName;
    @SerializedName("address")
    private String shippingAddress;
    @SerializedName("phone")
    private String shippingPhone;

    public int getProductId() {
        return productId;
    }

    public double getProductPrice() {
        return productPrice;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public String getOrderDate() {
        return orderDate;
    }

    public String getProductName() {
        return productName;
    }

    public String getOrderDateStatus() {
        return orderDateStatus;
    }

    public String getUserName() {
        return userName;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public String getShippingPhone() {
        return shippingPhone;
    }
}


import com.google.gson.annotations.SerializedName;

import java.util.List;

public class FavoriteApiResponse {

    @SerializedName("favorites")
    private List<Product> favorites;

    public List<Product> getFavorites() {
        return favorites;
    }
}


import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ProductApiResponse {

    @SerializedName("products")
    private List<Product> products;

    public List<Product> getProducts() {
        return products;
    }
}


import com.google.gson.annotations.SerializedName;

import java.util.List;

public class NewsFeedResponse {

    @SerializedName("posters")
    private List<NewsFeed> posters;

    public List<NewsFeed> getPosters() {
        return posters;
    }
}


import com.google.gson.annotations.SerializedName;

import java.util.List;

public class CartApiResponse {

    @SerializedName("carts")
    private List<Product> carts;

    public List<Product> getProductsInCart() {
        return carts;
    }
}


import com.google.gson.annotations.SerializedName;

public class History {

    @SerializedName("userId")
    private int userId;
    @SerializedName("productId")
    private int productId;

    public History(int userId, int productId) {
        this.userId = userId;
        this.productId = productId;
    }
}


import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

public class Product implements Parcelable {

    @SerializedName("id")
    private int productId;
    @SerializedName("product_name")
    private String productName;
    @SerializedName("price")
    private double productPrice;
    @SerializedName("quantity")
    private int productQuantity;
    @SerializedName("supplier")
    private String productSupplier;
    @SerializedName("category")
    private String productCategory;
    @SerializedName("image")
    private String productImage;
    @SerializedName("isFavourite")
    private int isFavourite;
    @SerializedName("isInCart")
    private int isInCart;
    // Include child Parcelable objects
    private Product mInfo;


    public Product(String productName, double productPrice, int productQuantity, String productSupplier, String productCategory) {
        this.productName = productName;
        this.productPrice = productPrice;
        this.productQuantity = productQuantity;
        this.productSupplier = productSupplier;
        this.productCategory = productCategory;
    }

    public Product() { }

    public int getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public double getProductPrice() {
        return productPrice;
    }

    public String getProductImage() {
        return productImage;
    }

    public int isFavourite() {
        return isFavourite;
    }

    public int isInCart() {
        return isInCart;
    }

    public void setIsFavourite(boolean isFavourite) {
        this.isFavourite = isFavourite ? 1 : 0;
    }

    public void setIsInCart(boolean isInCart) {
        this.isInCart = isInCart ? 1 : 0;
    }

    // Write the values to be saved to the `Parcel`.
    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(productId);
        out.writeString(productName);
        out.writeDouble(productPrice);
        out.writeInt(productQuantity);
        out.writeString(productSupplier);
        out.writeString(productCategory);
        out.writeString(productImage);
        out.writeInt(isFavourite);
        out.writeInt(isInCart);
        out.writeParcelable(mInfo, flags);
    }

    // Retrieve the values written into the `Parcel`.
    private Product(Parcel in) {
        productId = in.readInt();
        productName = in.readString();
        productPrice = in.readDouble();
        productQuantity = in.readInt();
        productSupplier = in.readString();
        productCategory = in.readString();
        productImage = in.readString();
        isFavourite = in.readInt();
        isInCart = in.readInt();
        mInfo = in.readParcelable(Product.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<Product> CREATOR
            = new Parcelable.Creator<Product>() {

        // This simply calls our new constructor and
        // passes along `Parcel`, and then returns the new object!
        @Override
        public Product createFromParcel(Parcel in) {
            return new Product(in);
        }

        @Override
        public Product[] newArray(int size) {
            return new Product[size];
        }
    };
}


import com.google.gson.annotations.SerializedName;

import java.util.List;

public class HistoryApiResponse {

    @SerializedName("history")
    private List<Product> historyList;

    public List<Product> getHistoryList() {
        return historyList;
    }
}


import com.google.gson.annotations.SerializedName;

public class NewsFeed {

    @SerializedName("poster_id")
    private int posterId;

    @SerializedName("image")
    private String image;

    public int getPosterId() {
        return posterId;
    }

    public void setPosterId(int posterId) {
        this.posterId = posterId;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}


public class Help {

    private String question;
    private String answer;

    public Help(String question, String answer) {
        this.question = question;
        this.answer = answer;
    }

    public String getQuestion() {
        return question;
    }

    public String getAnswer() {
        return answer;
    }
}


import com.google.gson.annotations.SerializedName;

public class Review {

    @SerializedName("userId")
    private int userId;
    @SerializedName("productId")
    private int productId;
    @SerializedName("name")
    private String userName;
    @SerializedName("date")
    private String reviewDate;
    @SerializedName("rate")
    private float reviewRate;
    @SerializedName("feedback")
    private String feedback;

    public Review(int userId, int productId, float reviewRate, String feedback) {
        this.userId = userId;
        this.productId = productId;
        this.reviewRate = reviewRate;
        this.feedback = feedback;
    }

    public String getUserName() {
        return userName;
    }

    public String getReviewDate() {
        return reviewDate;
    }

    public float getReviewRate() {
        return reviewRate;
    }

    public String getFeedback() {
        return feedback;
    }

}


import com.google.gson.annotations.SerializedName;

public class Shipping {

    @SerializedName("address")
    private String address;
    @SerializedName("city")
    private String city;
    @SerializedName("country")
    private String country;
    @SerializedName("zip")
    private String zip;
    @SerializedName("phone")
    private String phone;
    @SerializedName("userId")
    private int userId;
    @SerializedName("productId")
    private int productId;

    public Shipping(String address, String city, String country, String zip, String phone, int userId, int productId) {
        this.address = address;
        this.city = city;
        this.country = country;
        this.zip = zip;
        this.phone = phone;
        this.userId = userId;
        this.productId = productId;
    }
}


import com.google.gson.annotations.SerializedName;

public class Ordering {

    @SerializedName("name_on_card")
    private String nameOnCard;
    @SerializedName("card_number")
    private String cardNumber;
    @SerializedName("expiration_date")
    private String fullDate;
    @SerializedName("userId")
    private int userId;
    @SerializedName("productId")
    private int productId;

    public Ordering(String nameOnCard, String cardNumber, String fullDate, int userId, int productId) {
        this.nameOnCard = nameOnCard;
        this.cardNumber = cardNumber;
        this.fullDate = fullDate;
        this.userId = userId;
        this.productId = productId;
    }
}





import com.google.gson.annotations.SerializedName;

public class Otp {

    @SerializedName("otp")
    private String optCode;
    private String email;
    private boolean error;
    private String message;

    public Otp(String message) {
        this.message = message;
        this.error = true;
    }

    public String getOptCode() {
        return optCode;
    }

    public String getEmail() {
        return email;
    }

    public String getMessage() {
        return message;
    }

    public boolean isError() {
        return error;
    }
}


import com.google.gson.annotations.SerializedName;

import java.util.List;

public class OrderApiResponse {

    @SerializedName("orders")
    private List<Order> orderList;

    public List<Order> getOrderList() {
        return orderList;
    }
}


import com.google.gson.annotations.SerializedName;

public class Favorite {

    @SerializedName("userId")
    private int userId;
    @SerializedName("productId")
    private int productId;

    public Favorite(int userId, int productId) {
        this.userId = userId;
        this.productId = productId;
    }
}


public class RegisterApiResponse {

    private int id;
    private boolean error;
    private String message;
    private User user;

    public RegisterApiResponse(boolean error, String message) {
        this.error = error;
        this.message = message;
    }

    public int getId() {
        return id;
    }

    public boolean isError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public User getUser() {
        return user;
    }
}


import com.google.gson.annotations.SerializedName;

public class Cart {

    @SerializedName("userId")
    private int userId;
    @SerializedName("productId")
    private int productId;

    public Cart(int userId, int productId) {
        this.userId = userId;
        this.productId = productId;
    }
}


import com.google.gson.annotations.SerializedName;

public class Image {

    private boolean error;
    private String message;
    @SerializedName("image")
    private String imagePath;

    public String getMessage() {
        return message;
    }

    public String getImagePath() {
        return imagePath;
    }
}


import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.marwaeltayeb.souq.model.Otp;
import com.marwaeltayeb.souq.repository.OtpRepository;

public class OtpViewModel extends ViewModel {

    private final OtpRepository otpRepository;

    public OtpViewModel() {
        otpRepository = new OtpRepository();
    }

    public LiveData<Otp> getOtpCode(String token,String email) {
        return otpRepository.getOtpCode(token,email);
    }
}


import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.marwaeltayeb.souq.model.History;
import com.marwaeltayeb.souq.repository.ToHistoryRepository;

import okhttp3.ResponseBody;

public class ToHistoryViewModel extends ViewModel {

    private final ToHistoryRepository toHistoryRepository;

    public ToHistoryViewModel() {
        toHistoryRepository = new ToHistoryRepository();
    }

    public LiveData<ResponseBody> addToHistory(History history) {
        return toHistoryRepository.addToHistory(history);
    }
}


import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.marwaeltayeb.souq.repository.FromHistoryRepository;

import okhttp3.ResponseBody;

public class FromHistoryViewModel extends ViewModel {

    private final FromHistoryRepository fromHistoryRepository;

    public FromHistoryViewModel() {
        fromHistoryRepository = new FromHistoryRepository();
    }

    public LiveData<ResponseBody> removeAllFromHistory() {
        return fromHistoryRepository.removeAllFromHistory();
    }
}


import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.marwaeltayeb.souq.model.ReviewApiResponse;
import com.marwaeltayeb.souq.repository.ReviewRepository;

public class ReviewViewModel extends ViewModel {

    private final ReviewRepository reviewRepository;

    public ReviewViewModel(  ) {
        reviewRepository = new ReviewRepository();
    }

    public LiveData<ReviewApiResponse> getReviews(int productId) {
        return reviewRepository.getReviews(productId);
    }
}


import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.marwaeltayeb.souq.model.Shipping;
import com.marwaeltayeb.souq.repository.ShippingRepository;

import okhttp3.ResponseBody;

public class ShippingViewModel  extends ViewModel {

    private final ShippingRepository shippingRepository;

    public ShippingViewModel() {
        shippingRepository = new ShippingRepository();
    }

    public LiveData<ResponseBody> addShippingAddress(Shipping shipping) {
        return shippingRepository.addShippingAddress(shipping);
    }
}


import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.marwaeltayeb.souq.model.RegisterApiResponse;
import com.marwaeltayeb.souq.model.User;
import com.marwaeltayeb.souq.repository.RegisterRepository;

public class RegisterViewModel extends ViewModel {

    private final RegisterRepository registerRepository;

    public RegisterViewModel() {
        registerRepository = new RegisterRepository();
    }

    public LiveData<RegisterApiResponse> getRegisterResponseLiveData(User user) {
        return registerRepository.getRegisterResponseData(user);
    }
}


import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;

import com.marwaeltayeb.souq.model.Product;
import com.marwaeltayeb.souq.net.ProductDataSource;
import com.marwaeltayeb.souq.net.ProductDataSourceFactory;

public class CategoryViewModel extends ViewModel {

    // Create liveData for PagedList and PagedKeyedDataSource
    public LiveData<PagedList<Product>> categoryPagedList;

    public void loadProductsByCategory(String category, int userId) {
        // Get our database source factory
        ProductDataSourceFactory productDataSourceFactory = new ProductDataSourceFactory(category,userId);

        // Get PagedList configuration
        PagedList.Config pagedListConfig =
                (new PagedList.Config.Builder())
                        .setEnablePlaceholders(false)
                        .setPageSize(ProductDataSource.PAGE_SIZE)
                        .build();

        // Build the paged list
        categoryPagedList = (new LivePagedListBuilder<>(productDataSourceFactory, pagedListConfig)).build();
    }
}


import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.marwaeltayeb.souq.model.ProductApiResponse;
import com.marwaeltayeb.souq.repository.SearchRepository;

public class SearchViewModel  extends ViewModel {

    private final SearchRepository searchRepository;

    public SearchViewModel(  ) {
        searchRepository = new SearchRepository();
    }


    public LiveData<ProductApiResponse> getProductsBySearch(String keyword, int userId) {
        return searchRepository.getResponseDataBySearch(keyword, userId);
    }
}


import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.marwaeltayeb.souq.model.LoginApiResponse;
import com.marwaeltayeb.souq.repository.LoginRepository;

public class LoginViewModel extends ViewModel {

    private final LoginRepository loginRepository;

    public LoginViewModel() {
        loginRepository = new LoginRepository();
    }

    public LiveData<LoginApiResponse> getLoginResponseLiveData(String email, String password) {
        return loginRepository.getLoginResponseData(email, password);
    }
}


import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.marwaeltayeb.souq.model.Cart;
import com.marwaeltayeb.souq.repository.ToCartRepository;
import com.marwaeltayeb.souq.utils.RequestCallback;

import okhttp3.ResponseBody;

public class ToCartViewModel extends ViewModel {

    private final ToCartRepository toCartRepository;

    public ToCartViewModel() {
        toCartRepository = new ToCartRepository();
    }

    public LiveData<ResponseBody> addToCart(Cart cart, RequestCallback callback) {
        return toCartRepository.addToCart(cart, callback);
    }
}


import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.marwaeltayeb.souq.repository.DeleteUserRepository;

import okhttp3.ResponseBody;

public class DeleteUserViewModel extends ViewModel {

    private final DeleteUserRepository deleteUserRepository;

    public DeleteUserViewModel() {
        deleteUserRepository = new DeleteUserRepository();
    }

    public LiveData<ResponseBody> deleteUser(String token, int userId) {
        return deleteUserRepository.deleteUser(token, userId);
    }
}



import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.marwaeltayeb.souq.model.Image;
import com.marwaeltayeb.souq.repository.UserImageRepository;

public class UserImageViewModel extends ViewModel {

    private final UserImageRepository userImageRepository;

    public UserImageViewModel() {
        userImageRepository = new UserImageRepository();
    }

    public LiveData<Image> getUserImage(int userId) {
        return userImageRepository.getUserImage(userId);
    }
}


import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.marwaeltayeb.souq.model.NewsFeedResponse;
import com.marwaeltayeb.souq.repository.NewsFeedRepository;

public class NewsFeedViewModel extends ViewModel {

    private final NewsFeedRepository newsFeedRepository;

    public NewsFeedViewModel() {
        newsFeedRepository = new NewsFeedRepository();
    }

    public LiveData<NewsFeedResponse> getPosters() {
        return newsFeedRepository.getPosters();
    }
}


import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.marwaeltayeb.souq.model.FavoriteApiResponse;
import com.marwaeltayeb.souq.repository.FavoriteRepository;

public class FavoriteViewModel extends ViewModel {

    private final FavoriteRepository favoriteRepository;

    public FavoriteViewModel() {
        favoriteRepository = new FavoriteRepository();
    }

    public LiveData<FavoriteApiResponse> getFavorites(int userId) {
        return favoriteRepository.getFavorites(userId);
    }
}


import static com.marwaeltayeb.souq.net.LaptopDataSourceFactory.laptopDataSource;
import static com.marwaeltayeb.souq.net.ProductDataSourceFactory.productDataSource;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;

import com.marwaeltayeb.souq.model.Product;
import com.marwaeltayeb.souq.net.LaptopDataSourceFactory;
import com.marwaeltayeb.souq.net.ProductDataSource;
import com.marwaeltayeb.souq.net.ProductDataSourceFactory;

public class ProductViewModel extends ViewModel {

    // Create liveData for PagedList and PagedKeyedDataSource
    public LiveData<PagedList<Product>> productPagedList;

    public LiveData<PagedList<Product>> laptopPagedList;

    // Get PagedList configuration
    private static final PagedList.Config  pagedListConfig =
            (new PagedList.Config.Builder())
                    .setEnablePlaceholders(false)
                    .setPageSize(ProductDataSource.PAGE_SIZE)
                    .build();

    public void loadMobiles(String category, int userId){
        // Get our database source factory
        ProductDataSourceFactory productDataSourceFactory = new ProductDataSourceFactory(category,userId);

        // Build the paged list
        productPagedList = (new LivePagedListBuilder<>(productDataSourceFactory, pagedListConfig)).build();
    }

    public void loadLaptops(String category, int userId){
        // Get our database source factory
        LaptopDataSourceFactory laptopDataSourceFactory = new LaptopDataSourceFactory(category,userId);

        // Build the paged list
        laptopPagedList = (new LivePagedListBuilder<>(laptopDataSourceFactory, pagedListConfig)).build();
    }

    public void invalidate(){
        if(productDataSource != null) productDataSource.invalidate();
        if(laptopDataSource!= null) laptopDataSource.invalidate();
    }
}


import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.marwaeltayeb.souq.model.Ordering;
import com.marwaeltayeb.souq.repository.OrderingRepository;

import okhttp3.ResponseBody;

public class OrderingViewModel extends ViewModel {

    private final OrderingRepository orderingRepository;

    public OrderingViewModel(  ) {
        orderingRepository = new OrderingRepository();
    }

    public LiveData<ResponseBody> orderProduct(Ordering ordering) {
        return orderingRepository.orderProduct(ordering);
    }
}


import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.marwaeltayeb.souq.repository.RemoveFavoriteRepository;
import com.marwaeltayeb.souq.utils.RequestCallback;

import okhttp3.ResponseBody;

public class RemoveFavoriteViewModel extends ViewModel {

    private final RemoveFavoriteRepository removeFavoriteRepository;

    public RemoveFavoriteViewModel() {
        removeFavoriteRepository = new RemoveFavoriteRepository();
    }

    public LiveData<ResponseBody> removeFavorite(int userId, int productId, RequestCallback callback) {
        return removeFavoriteRepository.removeFavorite(userId, productId, callback);
    }
}


import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.marwaeltayeb.souq.repository.PasswordRepository;

import okhttp3.ResponseBody;

public class PasswordViewModel extends ViewModel {

    private final PasswordRepository passwordRepository;

    public PasswordViewModel() {
        passwordRepository = new PasswordRepository();
    }

    public LiveData<ResponseBody> updatePassword(String token, String newPassword, int userId) {
        return passwordRepository.updatePassword(token, newPassword, userId);
    }
}


import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.marwaeltayeb.souq.model.OrderApiResponse;
import com.marwaeltayeb.souq.repository.OrderRepository;

public class OrderViewModel extends ViewModel {

    private final OrderRepository orderRepository;

    public OrderViewModel() {
        orderRepository = new OrderRepository();
    }

    public LiveData<OrderApiResponse> getOrders(int userId) {
        return orderRepository.getOrders(userId);
    }
}



import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.marwaeltayeb.souq.repository.AddProductRepository;

import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

public class AddProductViewModel extends ViewModel {

    private final AddProductRepository addProductRepository;

    public AddProductViewModel() {
        addProductRepository = new AddProductRepository();
    }

    public LiveData<ResponseBody> addProduct(String token, Map<String, RequestBody> productInfo, MultipartBody.Part image) {
        return addProductRepository.addProduct(token,productInfo,image);
    }
}


import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.marwaeltayeb.souq.repository.FromCartRepository;
import com.marwaeltayeb.souq.utils.RequestCallback;

import okhttp3.ResponseBody;

public class FromCartViewModel extends ViewModel {

    private final FromCartRepository fromCartRepository;

    public FromCartViewModel(  ) {
        fromCartRepository = new FromCartRepository();
    }

    public LiveData<ResponseBody> removeFromCart(int userId, int productId, RequestCallback callback) {
        return fromCartRepository.removeFromCart(userId, productId, callback);
    }
}


import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.marwaeltayeb.souq.model.Review;
import com.marwaeltayeb.souq.repository.WriteReviewRepository;

import okhttp3.ResponseBody;

public class WriteReviewViewModel extends ViewModel {

    private final WriteReviewRepository writeReviewRepository;

    public WriteReviewViewModel() {
        writeReviewRepository = new WriteReviewRepository();
    }

    public LiveData<ResponseBody> writeReview(Review review) {
        return writeReviewRepository.writeReview(review);
    }
}


import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.marwaeltayeb.souq.model.Favorite;
import com.marwaeltayeb.souq.repository.AddFavoriteRepository;
import com.marwaeltayeb.souq.utils.RequestCallback;

import okhttp3.ResponseBody;

public class AddFavoriteViewModel extends ViewModel {

    private final AddFavoriteRepository addFavoriteRepository;

    public AddFavoriteViewModel() {
        addFavoriteRepository = new AddFavoriteRepository();
    }

    public LiveData<ResponseBody> addFavorite(Favorite favorite, RequestCallback callback) {
        return addFavoriteRepository.addFavorite(favorite,callback);
    }
}


import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.marwaeltayeb.souq.model.CartApiResponse;
import com.marwaeltayeb.souq.repository.CartRepository;

public class CartViewModel extends ViewModel {

    private final CartRepository cartRepository;

    public CartViewModel() {
        cartRepository = new CartRepository();
    }

    public LiveData<CartApiResponse> getProductsInCart(int userId) {
        return cartRepository.getProductsInCart(userId);
    }
}


import static com.marwaeltayeb.souq.net.HistoryDataSourceFactory.historyDataSource;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;

import com.marwaeltayeb.souq.model.Product;
import com.marwaeltayeb.souq.net.HistoryDataSource;
import com.marwaeltayeb.souq.net.HistoryDataSourceFactory;

public class HistoryViewModel extends ViewModel {

    // Create liveData for PagedList and PagedKeyedDataSource
    public LiveData<PagedList<Product>> historyPagedList;

    public void loadHistory(int userId) {
        HistoryDataSourceFactory historyDataSourceFactory = new HistoryDataSourceFactory(userId);

        // Get PagedList configuration
        PagedList.Config pagedListConfig =
                (new PagedList.Config.Builder())
                        .setEnablePlaceholders(false)
                        .setPageSize(HistoryDataSource.PAGE_SIZE).build();

        // Build the paged list
        historyPagedList = (new LivePagedListBuilder<>(historyDataSourceFactory, pagedListConfig)).build();
    }

    public void invalidate(){
        if(historyDataSource != null) historyDataSource.invalidate();
    }


}


import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.annotation.NonNull;

import com.marwaeltayeb.souq.repository.UploadPhotoRepository;

import okhttp3.ResponseBody;

public class UploadPhotoViewModel extends AndroidViewModel {

    private final UploadPhotoRepository uploadPhotoRepository;

    public UploadPhotoViewModel(@NonNull Application application) {
        super(application);
        uploadPhotoRepository = new UploadPhotoRepository(application);
    }

    public LiveData<ResponseBody> uploadPhoto(String pathname) {
        return uploadPhotoRepository.uploadPhoto(pathname);
    }
}


import android.util.Log;

import androidx.annotation.NonNull;
import androidx.paging.PageKeyedDataSource;

import com.marwaeltayeb.souq.model.Product;
import com.marwaeltayeb.souq.model.ProductApiResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductDataSource extends PageKeyedDataSource<Integer, Product> {

    private static final String TAG = "ProductDataSource";
    private static final int FIRST_PAGE = 1;
    public static final int PAGE_SIZE = 20;
    private final String category;
    private final int userId;

    ProductDataSource(String category, int userId) {
        this.category = category;
        this.userId = userId;
    }

    @Override
    public void loadInitial(@NonNull LoadInitialParams<Integer> params, @NonNull final LoadInitialCallback<Integer, Product> callback) {
        RetrofitClient.getInstance()
                .getApi().getProductsByCategory(category, userId,FIRST_PAGE)
                .enqueue(new Callback<ProductApiResponse>() {
                    @Override
                    public void onResponse(Call<ProductApiResponse> call, Response<ProductApiResponse> response) {
                        Log.v(TAG, "Succeeded " + response.body().getProducts().size());

                        if (response.body().getProducts() == null) {
                            return;
                        }

                        if (response.body() != null) {
                            callback.onResult(response.body().getProducts(), null, FIRST_PAGE + 1);
                        }
                    }

                    @Override
                    public void onFailure(Call<ProductApiResponse> call, Throwable t) {
                        Log.v(TAG, "Failed to get Products");
                    }
                });
    }

    @Override
    public void loadBefore(@NonNull final LoadParams<Integer> params, @NonNull final LoadCallback<Integer, Product> callback) {
        RetrofitClient.getInstance()
                .getApi().getProductsByCategory(category,userId,params.key)
                .enqueue(new Callback<ProductApiResponse>() {
                    @Override
                    public void onResponse(Call<ProductApiResponse> call, Response<ProductApiResponse> response) {
                        Integer adjacentKey = (params.key > 1) ? params.key - 1 : null;
                        if (response.body() != null) {
                            // Passing the loaded database and the previous page key
                            callback.onResult(response.body().getProducts(), adjacentKey);
                        }
                    }

                    @Override
                    public void onFailure(Call<ProductApiResponse> call, Throwable t) {
                        Log.v(TAG, "Failed to previous Products");
                    }
                });
    }

    @Override
    public void loadAfter(@NonNull final LoadParams<Integer> params, @NonNull final LoadCallback<Integer, Product> callback) {
        RetrofitClient.getInstance()
                .getApi().getProductsByCategory(category,userId,params.key)
                .enqueue(new Callback<ProductApiResponse>() {
                    @Override
                    public void onResponse(Call<ProductApiResponse> call, Response<ProductApiResponse> response) {
                        if (response.body() != null) {
                            // If the response has next page, increment the next page number
                            Integer key = response.body().getProducts().size() == PAGE_SIZE ? params.key + 1 : null;

                            // Passing the loaded database and next page value
                            callback.onResult(response.body().getProducts(), key);
                        }
                    }

                    @Override
                    public void onFailure(Call<ProductApiResponse> call, Throwable t) {
                        Log.v(TAG, "Failed to get next Products");
                    }
                });
    }
}


import static com.marwaeltayeb.souq.utils.Constant.LOCALHOST;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static final String BASE_URL = LOCALHOST;
    private static RetrofitClient mInstance;
    private final Retrofit retrofit;


    private RetrofitClient() {
        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    public static synchronized RetrofitClient getInstance() {
        if (mInstance == null) {
            mInstance = new RetrofitClient();
        }
        return mInstance;
    }

    public Api getApi() {
        return retrofit.create(Api.class);
    }

}


import com.marwaeltayeb.souq.model.Cart;
import com.marwaeltayeb.souq.model.CartApiResponse;
import com.marwaeltayeb.souq.model.Favorite;
import com.marwaeltayeb.souq.model.FavoriteApiResponse;
import com.marwaeltayeb.souq.model.History;
import com.marwaeltayeb.souq.model.HistoryApiResponse;
import com.marwaeltayeb.souq.model.Image;
import com.marwaeltayeb.souq.model.LoginApiResponse;
import com.marwaeltayeb.souq.model.NewsFeedResponse;
import com.marwaeltayeb.souq.model.OrderApiResponse;
import com.marwaeltayeb.souq.model.Ordering;
import com.marwaeltayeb.souq.model.Otp;
import com.marwaeltayeb.souq.model.ProductApiResponse;
import com.marwaeltayeb.souq.model.RegisterApiResponse;
import com.marwaeltayeb.souq.model.Review;
import com.marwaeltayeb.souq.model.ReviewApiResponse;
import com.marwaeltayeb.souq.model.Shipping;
import com.marwaeltayeb.souq.model.User;

import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.PartMap;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface Api {

    @POST("users/register")
    Call<RegisterApiResponse> createUser(@Body User user);

    @GET("users/login")
    Call<LoginApiResponse> logInUser(@Query("email") String email, @Query("password") String password);

    @DELETE("users/{userId}")
    Call<ResponseBody> deleteAccount(@Header("authorization") String token , @Path("userId") int userId);

    @Multipart
    @PUT("users/upload")
    Call<ResponseBody> uploadPhoto(@Header("authorization") String token , @Part MultipartBody.Part userPhoto, @Part("id") RequestBody userId);

    @PUT("users/info")
    Call<ResponseBody> updatePassword(@Header("authorization") String token, @Query("password") String password, @Query("id") int userId);

    @Multipart
    @POST("products/insert")
    Call<ResponseBody> insertProduct(@Header("authorization") String token, @PartMap Map<String, RequestBody> productInfo, @Part MultipartBody.Part image);

    @GET("users/getImage")
    Call<Image> getUserImage(@Query("id") int userId);

    @GET("users/otp")
    Call<Otp> getOtp(@Header("authorization") String token, @Query("email") String email);

    @GET("products")
    Call<ProductApiResponse> getProductsByCategory(@Query("category") String category, @Query("userId") int userId,@Query("page") int page);

    @GET("products/search")
    Call<ProductApiResponse> searchForProduct(@Query("q") String keyword, @Query("userId") int userId);

    @POST("favorites/add")
    Call<ResponseBody> addFavorite(@Body Favorite favorite);

    @DELETE("favorites/remove")
    Call<ResponseBody> removeFavorite(@Query("userId") int userId, @Query("productId") int productId);

    @GET("favorites")
    Call<FavoriteApiResponse> getFavorites(@Query("userId") int userId);

    @POST("carts/add")
    Call<ResponseBody> addToCart(@Body Cart cart);

    @DELETE("carts/remove")
    Call<ResponseBody> removeFromCart(@Query("userId") int userId, @Query("productId") int productId);

    @GET("carts")
    Call<CartApiResponse> getProductsInCart(@Query("userId") int userId);

    @POST("history/add")
    Call<ResponseBody> addToHistory(@Body History history);

    @DELETE("history/remove")
    Call<ResponseBody> removeAllFromHistory();

    @GET("history")
    Call<HistoryApiResponse> getProductsInHistory(@Query("userId") int userId, @Query("page") int page);

    @POST("review/add")
    Call<ResponseBody> addReview(@Body Review review);

    @GET("review")
    Call<ReviewApiResponse> getAllReviews(@Query("productId") int productId);

    @GET("posters")
    Call<NewsFeedResponse> getPosters();

    @GET("orders/get")
    Call<OrderApiResponse> getOrders(@Query("userId") int userId);

    @POST("address/add")
    Call<ResponseBody> addShippingAddress(@Body Shipping shipping);

    @POST("orders/add")
    Call<ResponseBody> orderProduct(@Body Ordering ordering);
}


import androidx.lifecycle.MutableLiveData;
import androidx.paging.DataSource;
import androidx.paging.PageKeyedDataSource;

import com.marwaeltayeb.souq.model.Product;

public class HistoryDataSourceFactory extends DataSource.Factory{

    private final int userId;

    public HistoryDataSourceFactory(int userId) {
        this.userId = userId;
    }

    // Creating the mutable live database
    private final MutableLiveData<PageKeyedDataSource<Integer, Product>> historyLiveDataSource = new MutableLiveData<>();

    public static HistoryDataSource historyDataSource;

    @Override
    public DataSource<Integer, Product> create() {
        // Getting our Data source object
        historyDataSource = new HistoryDataSource(userId);

        // Posting the Data source to get the values
        historyLiveDataSource.postValue(historyDataSource);

        // Returning the Data source
        return historyDataSource;
    }
}


import androidx.lifecycle.MutableLiveData;
import androidx.paging.DataSource;
import androidx.paging.PageKeyedDataSource;

import com.marwaeltayeb.souq.model.Product;

public class LaptopDataSourceFactory extends DataSource.Factory{

    // Creating the mutable live database
    private final MutableLiveData<PageKeyedDataSource<Integer, Product>> laptopLiveDataSource = new MutableLiveData<>();

    public static ProductDataSource laptopDataSource;

    private final String category;
    private final int userId;

    public LaptopDataSourceFactory(String category, int userId){
        this.category = category;
        this.userId = userId;
    }

    @Override
    public DataSource<Integer, Product> create() {
        // Getting our Data source object
        laptopDataSource = new ProductDataSource(category,userId);

        // Posting the Data source to get the values
        laptopLiveDataSource.postValue(laptopDataSource);

        // Returning the Data source
        return laptopDataSource;
    }
}

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.paging.PageKeyedDataSource;

import com.marwaeltayeb.souq.model.HistoryApiResponse;
import com.marwaeltayeb.souq.model.Product;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HistoryDataSource extends PageKeyedDataSource<Integer, Product> {

    private static final String TAG = "HistoryDataSource";
    private static final int FIRST_PAGE = 1;
    public static final int PAGE_SIZE = 20;
    private final int userId;

    public HistoryDataSource(int userId) {
        this.userId = userId;
    }

    @Override
    public void loadInitial(@NonNull LoadInitialParams<Integer> params, @NonNull LoadInitialCallback<Integer, Product> callback) {
        RetrofitClient.getInstance()
                .getApi().getProductsInHistory(userId,FIRST_PAGE)
                .enqueue(new Callback<HistoryApiResponse>() {
                    @Override
                    public void onResponse(Call<HistoryApiResponse> call, Response<HistoryApiResponse> response) {
                        Log.v(TAG, "Succeeded " + response.body().getHistoryList().size());

                        if (response.body().getHistoryList()== null) {
                            return;
                        }

                        if (response.body() != null) {
                            callback.onResult(response.body().getHistoryList(), null, FIRST_PAGE + 1);
                        }
                    }

                    @Override
                    public void onFailure(Call<HistoryApiResponse> call, Throwable t) {
                        Log.v(TAG, "Failed to get Products");
                    }
                });
    }

    @Override
    public void loadBefore(@NonNull LoadParams<Integer> params, @NonNull LoadCallback<Integer, Product> callback) {
        RetrofitClient.getInstance()
                .getApi().getProductsInHistory(userId,params.key)
                .enqueue(new Callback<HistoryApiResponse>() {
                    @Override
                    public void onResponse(Call<HistoryApiResponse> call, Response<HistoryApiResponse> response) {
                        Integer adjacentKey = (params.key > 1) ? params.key - 1 : null;
                        if (response.body() != null) {
                            // Passing the loaded database and the previous page key
                            callback.onResult(response.body().getHistoryList(), adjacentKey);
                        }
                    }

                    @Override
                    public void onFailure(Call<HistoryApiResponse> call, Throwable t) {
                        Log.v(TAG, "Failed to previous Products");
                    }
                });
    }

    @Override
    public void loadAfter(@NonNull LoadParams<Integer> params, @NonNull LoadCallback<Integer, Product> callback) {
        RetrofitClient.getInstance()
                .getApi().getProductsInHistory(userId,params.key)
                .enqueue(new Callback<HistoryApiResponse>() {
                    @Override
                    public void onResponse(Call<HistoryApiResponse> call, Response<HistoryApiResponse> response) {
                        if (response.body() != null) {
                            // If the response has next page, increment the next page number
                            Integer key = response.body().getHistoryList().size() == PAGE_SIZE ? params.key + 1 : null;

                            // Passing the loaded database and next page value
                            callback.onResult(response.body().getHistoryList(), key);
                        }
                    }

                    @Override
                    public void onFailure(Call<HistoryApiResponse> call, Throwable t) {
                        Log.v(TAG, "Failed to get next Products");
                    }
                });
    }
}


import androidx.lifecycle.MutableLiveData;
import androidx.paging.DataSource;
import androidx.paging.PageKeyedDataSource;

import com.marwaeltayeb.souq.model.Product;

public class ProductDataSourceFactory extends DataSource.Factory{

    // Creating the mutable live database
    private final MutableLiveData<PageKeyedDataSource<Integer, Product>> productLiveDataSource = new MutableLiveData<>();

    public static ProductDataSource productDataSource;

    private final String category;
    private final int userId;

    public ProductDataSourceFactory(String category, int userId){
        this.category = category;
        this.userId = userId;
    }

    @Override
    public DataSource<Integer, Product> create() {
        // Getting our Data source object
        productDataSource = new ProductDataSource(category, userId);

        // Posting the Data source to get the values
        productLiveDataSource.postValue(productDataSource);

        // Returning the Data source
        return productDataSource;
    }
}

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.marwaeltayeb.souq.R;
import com.marwaeltayeb.souq.adapter.NewsFeedAdapter;
import com.marwaeltayeb.souq.databinding.ActivityNewsFeedBinding;
import com.marwaeltayeb.souq.viewmodel.NewsFeedViewModel;

public class NewsFeedActivity extends AppCompatActivity {

    private ActivityNewsFeedBinding binding;
    private NewsFeedViewModel newsFeedViewModel;
    private NewsFeedAdapter newsFeedAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_news_feed);

        newsFeedViewModel = ViewModelProviders.of(this).get(NewsFeedViewModel.class);

        setUpRecyclerView();

        getPosters();
    }

    private void setUpRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        binding.newsFeedList.setLayoutManager(layoutManager);
        binding.newsFeedList.setHasFixedSize(true);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this, layoutManager.getOrientation());
        binding.newsFeedList.addItemDecoration(dividerItemDecoration);
    }

    private void getPosters() {
        newsFeedViewModel.getPosters().observe(this, newsFeedResponse -> {
            newsFeedAdapter = new NewsFeedAdapter(getApplicationContext(), newsFeedResponse.getPosters());
            binding.newsFeedList.setAdapter(newsFeedAdapter);
        });
    }
}


import static com.marwaeltayeb.souq.utils.Constant.KEYWORD;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.preference.PreferenceManager;

import com.marwaeltayeb.souq.R;
import com.marwaeltayeb.souq.adapter.WordAdapter;
import com.marwaeltayeb.souq.databinding.ActivitySearchBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SearchActivity extends AppCompatActivity implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

    private static final String HISTORY_DATA = "history_data";
    private static final int DRAWABLE_LEFT = 0;
    private static final int DRAWABLE_RIGHT = 2;

    private ActivitySearchBinding binding;
    private String word;
    private WordAdapter adapter;
    private List<String> list;
    private SharedPreferences sharedpreferences;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        getSupportActionBar().setElevation(0);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_search);

        binding.wordList.setOnItemClickListener(this);
        binding.wordList.setOnItemLongClickListener(this);

        // Return a collection view of the values contained in this map
        list = new ArrayList<>(getWords(this).keySet());

        adapter = new WordAdapter(this,  list);
        binding.wordList.setDivider(null);
        binding.wordList.setAdapter(adapter);
        adapter.notifyDataSetChanged();

        binding.editQuery.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Do nothing
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Do nothing
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (binding.editQuery.getText().toString().trim().length() == 1) {

                    binding.editQuery.clearFocus();
                    binding.editQuery.requestFocus();
                    binding.editQuery.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_arrow_back, 0, R.drawable.ic_close, 0);
                }
            }
        });

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
        binding.editQuery.requestFocus();

        binding.editQuery.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                // Your piece of code on keyboard search click
                Intent searchIntent = new Intent(SearchActivity.this, ResultActivity.class);
                word = binding.editQuery.getText().toString().trim();
                // Set Key with its specific key
                setWord(getApplicationContext(), word, word);
                searchIntent.putExtra(KEYWORD, word);
                startActivity(searchIntent);
                return true;
            }
            return false;
        });

        binding.editQuery.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {

                if (event.getRawX() >= (binding.editQuery.getRight() - binding.editQuery.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                    // your action here
                    binding.editQuery.getText().clear();
                    return true;
                }else if ((event.getRawX() + binding.editQuery.getPaddingLeft()) <= (binding.editQuery.getCompoundDrawables()[DRAWABLE_LEFT].getBounds().width() + binding.editQuery.getLeft())) {
                    Intent mainIntent = new Intent(SearchActivity.this, ProductActivity.class);
                    startActivity(mainIntent);
                    return true;
                }
            }
            return false;
        });

        sharedpreferences.registerOnSharedPreferenceChangeListener(prefChangeListener);
    }

    public void setWord(Context context , String key , String word){
        sharedpreferences = context.getSharedPreferences(HISTORY_DATA, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putString(String.valueOf(key), word);
        editor.apply();
    }

    public Map<String, ?> getWords(Context context){
        sharedpreferences = context.getSharedPreferences(HISTORY_DATA, Context.MODE_PRIVATE);
        // Returns a map containing a list of pairs key/value representing the preferences.
        return sharedpreferences.getAll();
    }

    public static void clearSharedPreferences(Context context){
        context.getSharedPreferences(HISTORY_DATA, Context.MODE_PRIVATE).edit().clear().apply();
    }

    public static void clearOneItemInSharedPreferences(String key, Context context){
        context.getSharedPreferences(HISTORY_DATA, Context.MODE_PRIVATE).edit().remove(key).apply();
    }

    @Override
    protected void onDestroy() {
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(prefChangeListener);
        super.onDestroy();
    }

    public void clearAll(View view) {
        clearSharedPreferences(getApplicationContext());
        adapter.clear();
        Toast.makeText(SearchActivity.this, "Cleared", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = new Intent(getApplicationContext(), ResultActivity.class);
        // Send KEYWORD to ResultActivity
        intent.putExtra(KEYWORD, list.get(position));
        startActivity(intent);
    }


    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        Log.v("long clicked","position: " + position);
        // Get value of a specific position
        word = list.get(position);
        // Set word as a key
        clearOneItemInSharedPreferences(word, getApplicationContext());
        // Remove element from adapter
        adapter.remove(word);
        Toast.makeText(this, "Removed", Toast.LENGTH_SHORT).show();
        return true;
    }

    private final SharedPreferences.OnSharedPreferenceChangeListener prefChangeListener = (sharedPreferences, key) -> {
        if (key.equals(word)) {
            // Clear the adapter, then add list
            adapter.clear();
            list = new ArrayList<>(getWords(getApplicationContext()).keySet());
            adapter.addAll(list);
            binding.wordList.setAdapter(adapter);
        }
    };
}


import static com.marwaeltayeb.souq.storage.LanguageUtils.loadLocale;
import static com.marwaeltayeb.souq.utils.Constant.PRODUCT_ID;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProviders;

import com.marwaeltayeb.souq.R;
import com.marwaeltayeb.souq.databinding.ActivityWriteReviewBinding;
import com.marwaeltayeb.souq.model.Review;
import com.marwaeltayeb.souq.storage.LoginUtils;
import com.marwaeltayeb.souq.viewmodel.WriteReviewViewModel;

import java.io.IOException;

public class WriteReviewActivity extends AppCompatActivity implements View.OnClickListener {

    private ActivityWriteReviewBinding binding;
    private int productId;

    private WriteReviewViewModel writeReviewViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadLocale(this);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_write_review);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getResources().getString(R.string.write_review));

        writeReviewViewModel = ViewModelProviders.of(this).get(WriteReviewViewModel.class);

        binding.btnSubmit.setOnClickListener(this);
        binding.txtName.setText(LoginUtils.getInstance(this).getUserInfo().getName());

        getCurrentTextLength();

        Intent intent = getIntent();
        productId = intent.getIntExtra(PRODUCT_ID, 0);

    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btnSubmit) {
            writeReview();
        }
    }

    private void writeReview() {
        int userId = LoginUtils.getInstance(this).getUserInfo().getId();
        String feedback = binding.editFeedback.getText().toString().trim();
        float rate = binding.rateProduct.getRating();

        // Check if there are no empty values
        if (TextUtils.isEmpty(feedback) || rate == 0.0f) {
            Toast.makeText(this, getString(R.string.required_data), Toast.LENGTH_SHORT).show();
            return;
        }

        Review review = new Review(userId, productId, rate, feedback);
        writeReviewViewModel.writeReview(review).observe(this, responseBody -> {
            if ((responseBody != null)) {
                try {
                    Toast.makeText(this, responseBody.string(), Toast.LENGTH_SHORT).show();
                    finish();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void getCurrentTextLength(){
        binding.editFeedback.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Do nothing
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Do nothing
            }

            @Override
            public void afterTextChanged(Editable s) {
                int textLength = 150;
                int writtenTextLength = s.toString().length();
                binding.textLength.setText(String.valueOf(textLength - writtenTextLength));
            }
        });
    }
}


import static com.marwaeltayeb.souq.storage.LanguageUtils.loadLocale;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.marwaeltayeb.souq.R;
import com.marwaeltayeb.souq.storage.LoginUtils;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_TIME_OUT = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadLocale(this);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent i = new Intent(SplashActivity.this, ProductActivity.class);
            startActivity(i);

            // Close this activity
            finish();
            // If user does not log in before, go to LoginActivity
            if(!LoginUtils.getInstance(SplashActivity.this).isLoggedIn()) {
                Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                startActivity(intent);
            }

        }, SPLASH_TIME_OUT);
    }
}


import static com.marwaeltayeb.souq.storage.LanguageUtils.loadLocale;
import static com.marwaeltayeb.souq.utils.Constant.PRODUCT;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;

import com.marwaeltayeb.souq.R;
import com.marwaeltayeb.souq.adapter.ProductAdapter;
import com.marwaeltayeb.souq.databinding.ActivityAllLaptopsBinding;
import com.marwaeltayeb.souq.model.Product;
import com.marwaeltayeb.souq.storage.LoginUtils;
import com.marwaeltayeb.souq.viewmodel.ProductViewModel;

public class AllLaptopsActivity extends AppCompatActivity implements ProductAdapter.ProductAdapterOnClickHandler {

    private ActivityAllLaptopsBinding binding;
    private ProductAdapter laptopAdapter;
    private ProductViewModel productViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadLocale(this);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_all_laptops);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getResources().getString(R.string.all_laptops));

        int userID = LoginUtils.getInstance(this).getUserInfo().getId();

        productViewModel = ViewModelProviders.of(this).get(ProductViewModel.class);
        productViewModel.loadLaptops("laptop",userID);

        setupRecyclerViews();

        getAllLaptops();
    }

    private void setupRecyclerViews() {
        // Laptops
        binding.allLaptopsRecyclerView.setHasFixedSize(true);
        binding.allLaptopsRecyclerView.setLayoutManager(new GridLayoutManager(this, (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) ? 2 : 4));
        laptopAdapter = new ProductAdapter(this, this);
    }

    public void getAllLaptops() {
        productViewModel.laptopPagedList.observe(this, products -> laptopAdapter.submitList(products));
        binding.allLaptopsRecyclerView.setAdapter(laptopAdapter);
    }

    @Override
    public void onClick(Product product) {
        Intent intent = new Intent(AllLaptopsActivity.this, DetailsActivity.class);
        // Pass an object of product class
        intent.putExtra(PRODUCT, (product));
        startActivity(intent);
    }
}


import static com.marwaeltayeb.souq.utils.Constant.ORDER;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.marwaeltayeb.souq.R;
import com.marwaeltayeb.souq.adapter.OrderAdapter;
import com.marwaeltayeb.souq.databinding.ActivityOrdersBinding;
import com.marwaeltayeb.souq.model.Order;
import com.marwaeltayeb.souq.storage.LoginUtils;
import com.marwaeltayeb.souq.viewmodel.OrderViewModel;

public class OrdersActivity extends AppCompatActivity implements OrderAdapter.OrderAdapterOnClickHandler {

    private ActivityOrdersBinding binding;
    private OrderViewModel orderViewModel;
    private OrderAdapter orderAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orders);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_orders);

        orderViewModel = ViewModelProviders.of(this).get(OrderViewModel.class);

        setUpRecycleView();

        getOrders();
    }

    private void setUpRecycleView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        binding.orderList.setLayoutManager(layoutManager);
        binding.orderList.setHasFixedSize(true);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this, layoutManager.getOrientation());
        binding.orderList.addItemDecoration(dividerItemDecoration);
    }

    private void getOrders() {
        orderViewModel.getOrders(LoginUtils.getInstance(this).getUserInfo().getId()).observe(this, orderApiResponse -> {
            orderAdapter = new OrderAdapter( orderApiResponse.getOrderList(),this);
            binding.orderList.setAdapter(orderAdapter);
        });
    }

    @Override
    public void onClick(Order order) {
        Intent intent = new Intent(OrdersActivity.this, StatusActivity.class);
        // Pass an object of order class
        intent.putExtra(ORDER, (order));
        startActivity(intent);
    }
}


import static com.marwaeltayeb.souq.utils.Constant.PRODUCTID;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProviders;

import com.marwaeltayeb.souq.R;
import com.marwaeltayeb.souq.databinding.ActivityShippingAddressBinding;
import com.marwaeltayeb.souq.model.Shipping;
import com.marwaeltayeb.souq.storage.LoginUtils;
import com.marwaeltayeb.souq.viewmodel.ShippingViewModel;

import java.io.IOException;

public class ShippingAddressActivity extends AppCompatActivity implements View.OnClickListener{

    private ActivityShippingAddressBinding binding;

    private ShippingViewModel shippingViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_shipping_address);

        shippingViewModel = ViewModelProviders.of(this).get(ShippingViewModel.class);

        binding.proceed.setOnClickListener(this);

        binding.txtName.setText(LoginUtils.getInstance(this).getUserInfo().getName());
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.proceed) {
            addShippingAddress();
        }
    }

    private void addShippingAddress() {
        String address = binding.address.getText().toString().trim();
        String city = binding.city.getText().toString().trim();
        String country = binding.country.getText().toString().trim();
        String zip = binding.zip.getText().toString().trim();
        String phone = binding.phone.getText().toString().trim();
        int userId = LoginUtils.getInstance(this).getUserInfo().getId();
        Intent intent = getIntent();
        int productId = intent.getIntExtra(PRODUCTID, 0);

        Shipping shipping = new Shipping(address, city, country, zip, phone,userId, productId);

        shippingViewModel.addShippingAddress(shipping).observe(this, responseBody -> {
            try {
                Toast.makeText(ShippingAddressActivity.this, responseBody.string()+"", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Intent orderProductIntent = new Intent(ShippingAddressActivity.this, OrderProductActivity.class);
            orderProductIntent.putExtra(PRODUCTID,productId);
            startActivity(orderProductIntent);
        });
    }
}


import static com.marwaeltayeb.souq.storage.LanguageUtils.loadLocale;
import static com.marwaeltayeb.souq.utils.Constant.PRODUCT;
import static com.marwaeltayeb.souq.utils.InternetUtils.isNetworkConnected;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.marwaeltayeb.souq.R;
import com.marwaeltayeb.souq.adapter.ProductAdapter;
import com.marwaeltayeb.souq.databinding.ActivityCategoryBinding;
import com.marwaeltayeb.souq.model.Product;
import com.marwaeltayeb.souq.receiver.NetworkChangeReceiver;
import com.marwaeltayeb.souq.storage.LoginUtils;
import com.marwaeltayeb.souq.utils.Constant;
import com.marwaeltayeb.souq.utils.OnNetworkListener;
import com.marwaeltayeb.souq.viewmodel.CategoryViewModel;

public class CategoryActivity extends AppCompatActivity implements ProductAdapter.ProductAdapterOnClickHandler, OnNetworkListener {

    private ActivityCategoryBinding binding;
    private ProductAdapter productAdapter;
    private CategoryViewModel categoryViewModel;
    private Snackbar snack;
    private NetworkChangeReceiver mNetworkReceiver;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadLocale(this);
        binding= DataBindingUtil.setContentView(this, R.layout.activity_category);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getResources().getString(R.string.app_name));

        // This line shows Up button
        actionBar.setDisplayHomeAsUpEnabled(true);

        snack = Snackbar.make(findViewById(android.R.id.content), getResources().getString(R.string.no_internet_connection), Snackbar.LENGTH_INDEFINITE);

        // Get Category from ProductActivity Intent
        Intent intent = getIntent();
        String category = intent.getStringExtra(Constant.CATEGORY);

        // Update Toolbar
        getSupportActionBar().setTitle(category);

        int userID = LoginUtils.getInstance(this).getUserInfo().getId();
        categoryViewModel = ViewModelProviders.of(this).get(CategoryViewModel.class);
        categoryViewModel.loadProductsByCategory(category.toLowerCase(), userID);

        setupRecyclerViews();

        getProductsByCategory();

        mNetworkReceiver = new NetworkChangeReceiver();
        mNetworkReceiver.setOnNetworkListener(this);
    }

    private void setupRecyclerViews() {
        binding.categoryList.setLayoutManager(new GridLayoutManager(this, (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) ? 2 : 4));
        binding.categoryList.setHasFixedSize(true);
        productAdapter = new ProductAdapter(this,this);
    }


    public void getProductsByCategory() {
        if (isNetworkConnected(this)) {
            categoryViewModel.categoryPagedList.observe(this, products -> {
                productAdapter.submitList(products);
            });

            binding.categoryList.setAdapter(productAdapter);
        }
    }

    @Override
    public void onClick(Product product) {
        Intent intent = new Intent(CategoryActivity.this, DetailsActivity.class);
        // Pass an object of product class
        intent.putExtra(PRODUCT, (product));
        startActivity(intent);
    }

    @Override
    public void onNetworkConnected() {
        hideSnackBar();
        getProductsByCategory();
    }

    @Override
    public void onNetworkDisconnected() {
        showSnackBar();
    }

    public void showSnackBar() {
        snack.getView().setBackgroundColor(ContextCompat.getColor(this, R.color.red));
        snack.show();
    }

    public void hideSnackBar() {
        snack.dismiss();
    }

    private void registerNetworkBroadcastForNougat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            registerReceiver(mNetworkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            registerReceiver(mNetworkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerNetworkBroadcastForNougat();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mNetworkReceiver);
    }
}


import static com.marwaeltayeb.souq.storage.LanguageUtils.loadLocale;
import static com.marwaeltayeb.souq.utils.Constant.PRODUCT_ID;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.marwaeltayeb.souq.R;
import com.marwaeltayeb.souq.adapter.ReviewAdapter;
import com.marwaeltayeb.souq.databinding.ActivityAllReviewsBinding;
import com.marwaeltayeb.souq.model.Review;
import com.marwaeltayeb.souq.viewmodel.ReviewViewModel;

import java.util.List;

public class AllReviewsActivity extends AppCompatActivity {

    private ActivityAllReviewsBinding binding;
    private ReviewViewModel reviewViewModel;
    private ReviewAdapter reviewAdapter;
    private List<Review> reviewList;
    private int productId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadLocale(this);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_all_reviews);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getResources().getString(R.string.reviews));

        reviewViewModel = ViewModelProviders.of(this).get(ReviewViewModel.class);

        Intent intent = getIntent();
        productId = intent.getIntExtra(PRODUCT_ID, 0);

        setUpRecycleView();

        getReviewsOfProduct();
    }

    private void setUpRecycleView() {
        binding.allReviewsList.setHasFixedSize(true);
        binding.allReviewsList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
    }

    private void getReviewsOfProduct() {
        reviewViewModel.getReviews(productId).observe(this, reviewApiResponse -> {
            if (reviewApiResponse != null) {
                binding.rateProduct.setRating(reviewApiResponse.getAverageReview());
                binding.rateNumber.setText(reviewApiResponse.getAverageReview() + getString(R.string.highestNumber));
                reviewList = reviewApiResponse.getReviewList();
                reviewAdapter = new ReviewAdapter(reviewList);
                binding.allReviewsList.setAdapter(reviewAdapter);
            }
        });
    }
}


import static com.marwaeltayeb.souq.utils.Constant.EMAIL;
import static com.marwaeltayeb.souq.utils.Constant.OTP;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProviders;

import com.marwaeltayeb.souq.R;
import com.marwaeltayeb.souq.databinding.ActivityAuthenticationBinding;
import com.marwaeltayeb.souq.storage.LoginUtils;
import com.marwaeltayeb.souq.utils.FlagsManager;
import com.marwaeltayeb.souq.viewmodel.OtpViewModel;

public class AuthenticationActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "AuthenticationActivity";
    private ActivityAuthenticationBinding binding;
    private OtpViewModel otpViewModel;
    private String email;
    private String correctOtpCode;
    private int clickCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_authentication);

        otpViewModel = ViewModelProviders.of(this).get(OtpViewModel.class);

        binding.proceed.setOnClickListener(this);
        binding.reSend.setOnClickListener(this);

        Intent intent = getIntent();
        email = intent.getStringExtra(EMAIL);
        correctOtpCode = intent.getStringExtra(OTP);
        String formatted = getString(R.string.description2, email);

        TextView authentication = findViewById(R.id.authentication);
        authentication.setText(formatted);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.proceed) {
            checkOtpCode();
        } else if (view.getId() == R.id.reSend) {
            clickCount = clickCount + 1;
            getAnotherOtpCode();
            if (clickCount >= 3) {
                binding.reSend.setClickable(false);
                binding.numberOfClicks.setVisibility(View.VISIBLE);
            }
        }
    }

    private void getAnotherOtpCode() {
        otpViewModel.getOtpCode(LoginUtils.getInstance(this).getUserToken(), email).observe(this, responseBody -> {
            if (!responseBody.isError()) {
                correctOtpCode = responseBody.getOptCode();
                binding.reSend.setEnabled(false);
                binding.countDownTimer.setVisibility(View.VISIBLE);
                countDownTimer(binding.countDownTimer);
            }
        });
    }

    private void checkOtpCode() {
        String otpEntered = binding.otpCode.getText().toString();

        if (!otpEntered.equals(correctOtpCode)) {
            binding.otpCode.setError(getString(R.string.incorrect_code));
            binding.otpCode.requestFocus();
        } else {
            Intent passwordIntent = new Intent(this, PasswordActivity.class);
            startActivity(passwordIntent);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        FlagsManager.getInstance().setActivityRunning(true);
    }

    @Override
    public void onStop() {
        super.onStop();
        FlagsManager.getInstance().setActivityRunning(false);
    }

    private void countDownTimer(TextView textView) {
        new CountDownTimer(60000, 1000) {

            public void onTick(long millisUntilFinished) {
                textView.setText(String.valueOf(millisUntilFinished / 1000));
            }

            public void onFinish() {
                Log.d(TAG, "onFinish: " + "done!");
                binding.reSend.setEnabled(true);
                binding.countDownTimer.setVisibility(View.INVISIBLE);
            }

        }.start();
    }
}


import static com.marwaeltayeb.souq.storage.LanguageUtils.loadLocale;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProviders;

import com.marwaeltayeb.souq.R;
import com.marwaeltayeb.souq.databinding.ActivityPasswordBinding;
import com.marwaeltayeb.souq.storage.LoginUtils;
import com.marwaeltayeb.souq.utils.FlagsManager;
import com.marwaeltayeb.souq.utils.Validation;
import com.marwaeltayeb.souq.viewmodel.PasswordViewModel;

import java.io.IOException;

public class PasswordActivity extends AppCompatActivity implements View.OnClickListener{


    private ActivityPasswordBinding binding;
    private PasswordViewModel passwordViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadLocale(this);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_password);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getResources().getString(R.string.change_password));

        passwordViewModel = ViewModelProviders.of(this).get(PasswordViewModel.class);

        binding.saveChanges.setOnClickListener(this);
        binding.cancel.setOnClickListener(this);

        if(FlagsManager.getInstance().isActivityRunning()){
            binding.currentPassword.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.saveChanges:
                updatePassword();
                break;
            case R.id.cancel:
                finish();
                break;
            default: // Should not get here
        }
    }

    private void updatePassword() {
        int userId = LoginUtils.getInstance(this).getUserInfo().getId();
        String token = LoginUtils.getInstance(this).getUserToken();
        String oldPassword = LoginUtils.getInstance(this).getUserInfo().getPassword();
        String currentPassword = binding.currentPassword.getText().toString();
        String newPassword = binding.newPassword.getText().toString();
        String retypePassword =binding.retypePassword.getText().toString();

        if(!currentPassword.equals(oldPassword)){
            binding.currentPassword.setError(getString(R.string.enter_current_password));
            binding.currentPassword.requestFocus();
            return;
        }

        if (!Validation.isValidPassword(newPassword)) {
            binding.newPassword.setError(getString(R.string.password__at_least_8_characters));
            binding.newPassword.requestFocus();
            return;
        }

        if (!(retypePassword.equals(newPassword))) {
            binding.retypePassword.setError(getString(R.string.password_not_match));
            binding.retypePassword.requestFocus();
            return;
        }

        passwordViewModel.updatePassword(token,newPassword, userId).observe(this, responseBody -> {
            try {
                Toast.makeText(PasswordActivity.this, responseBody.string(), Toast.LENGTH_SHORT).show();
                finish();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}


import static com.marwaeltayeb.souq.utils.Constant.EMAIL;
import static com.marwaeltayeb.souq.utils.Constant.OTP;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProviders;

import com.marwaeltayeb.souq.R;
import com.marwaeltayeb.souq.databinding.ActivityPasswordAssistantBinding;
import com.marwaeltayeb.souq.storage.LoginUtils;
import com.marwaeltayeb.souq.viewmodel.OtpViewModel;

public class PasswordAssistantActivity extends AppCompatActivity implements View.OnClickListener {

    private ActivityPasswordAssistantBinding binding;
    private OtpViewModel otpViewModel;
    private String userEmail;
    private String otpCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_password_assistant);

        otpViewModel = ViewModelProviders.of(this).get(OtpViewModel.class);

        binding.proceed.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.proceed) {
            checkUserEmail();
        }
    }

    private void checkUserEmail() {
        String emailEntered = binding.emailAddress.getText().toString();
        String token = LoginUtils.getInstance(this).getUserToken();

        otpViewModel.getOtpCode(token,emailEntered).observe(this, responseBody -> {
            if (!responseBody.isError()) {
                userEmail = responseBody.getEmail();
                otpCode = responseBody.getOptCode();
                goToAuthenticationActivity();
            } else {
                binding.emailAddress.setError(responseBody.getMessage());
            }
        });
    }

    private void goToAuthenticationActivity() {
        Intent intent = new Intent(this, AuthenticationActivity.class);
        intent.putExtra(EMAIL, userEmail);
        intent.putExtra(OTP, otpCode);
        startActivity(intent);
    }
}


import static com.marwaeltayeb.souq.storage.LanguageUtils.loadLocale;
import static com.marwaeltayeb.souq.utils.ProgressDialog.createAlertDialog;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProviders;

import com.marwaeltayeb.souq.R;
import com.marwaeltayeb.souq.databinding.ActivityLoginBinding;
import com.marwaeltayeb.souq.storage.LoginUtils;
import com.marwaeltayeb.souq.utils.Validation;
import com.marwaeltayeb.souq.viewmodel.LoginViewModel;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    private ActivityLoginBinding binding;
    private LoginViewModel loginViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadLocale(this);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getResources().getString(R.string.app_name));

        binding.buttonLogin.setOnClickListener(this);
        binding.textViewSignUp.setOnClickListener(this);
        binding.forgetPassword.setOnClickListener(this);

        loginViewModel = ViewModelProviders.of(this).get(LoginViewModel.class);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // If user logged in, go directly to ProductActivity
        if (LoginUtils.getInstance(this).isLoggedIn()) {
            goToProductActivity();
        }
    }

    private void logInUser() {
        String email = binding.inputEmail.getText().toString();
        String password = binding.inputPassword.getText().toString();

        if (email.isEmpty()) {
            binding.inputEmail.setError(getString(R.string.email_required));
            binding.inputEmail.requestFocus();
        }

        if (Validation.isValidEmail(email)) {
            binding.inputEmail.setError(getString(R.string.enter_a_valid_email_address));
            binding.inputEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            binding.inputPassword.setError(getString(R.string.password_required));
            binding.inputPassword.requestFocus();
            return;
        }

        if (!Validation.isValidPassword(password)) {
            binding.inputPassword.setError(getString(R.string.password__at_least_8_characters));
            binding.inputPassword.requestFocus();
            return;
        }

        AlertDialog alert = createAlertDialog(this);

        loginViewModel.getLoginResponseLiveData(email,password).observe(this, loginApiResponse -> {
            if (!loginApiResponse.isError()) {
                LoginUtils.getInstance(this).saveUserInfo(loginApiResponse);
                Toast.makeText(this, loginApiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                alert.dismiss();
                goToProductActivity();
            }else {
                alert.dismiss();
                Toast.makeText(this, loginApiResponse.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.buttonLogin:
                logInUser();
                break;
            case R.id.textViewSignUp:
                goToSignUpActivity();
                break;
            case R.id.forgetPassword:
                goToPasswordAssistantActivity();
                break;
            default: // Should not get here
        }
    }

    private void goToSignUpActivity() {
        Intent intent = new Intent(this, SignUpActivity.class);
        startActivity(intent);
    }

    private void goToProductActivity() {
        Intent intent = new Intent(this, ProductActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void goToPasswordAssistantActivity() {
        Intent intent = new Intent(this, PasswordAssistantActivity.class);
        startActivity(intent);
    }
}


import static com.marwaeltayeb.souq.utils.Constant.PRODUCTID;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProviders;

import com.marwaeltayeb.souq.R;
import com.marwaeltayeb.souq.databinding.ActivityOrderProductBinding;
import com.marwaeltayeb.souq.model.Ordering;
import com.marwaeltayeb.souq.storage.LoginUtils;
import com.marwaeltayeb.souq.viewmodel.OrderingViewModel;

import java.io.IOException;

public class OrderProductActivity extends AppCompatActivity implements View.OnClickListener{

    private ActivityOrderProductBinding binding;
    private OrderingViewModel orderingViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_order_product);

        orderingViewModel = ViewModelProviders.of(this).get(OrderingViewModel.class);

        binding.addCard.setOnClickListener(this);

        populateSpinner();
    }

    private void orderProduct() {
        String nameOnCard = binding.nameOnCard.getText().toString().trim();
        String cardNumber = binding.cardNumber.getText().toString().trim();

        String year = binding.spinnerYearMenu.getEditableText().toString().toLowerCase();
        String month = binding.spinnerMonthMenu.getEditableText().toString().toLowerCase();
        String fullDate = year + "-" + month + "-00";

        int userId = LoginUtils.getInstance(this).getUserInfo().getId();
        Intent intent = getIntent();
        int productId = intent.getIntExtra(PRODUCTID, 0);

        Ordering ordering = new Ordering(nameOnCard,cardNumber,fullDate,userId,productId);

        orderingViewModel.orderProduct(ordering).observe(this, responseBody -> {
            try {
                Toast.makeText(OrderProductActivity.this, responseBody.string() + "", Toast.LENGTH_SHORT).show();
                finish();
                Intent homeIntent = new Intent(OrderProductActivity.this, ProductActivity.class);
                startActivity(homeIntent);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.addCard) {
            orderProduct();
        }
    }

    private void populateSpinner() {
        String[] years = {"2020","2021","2022","2023","2024","2025","2026","2027","2028","2029","2030"};
        ArrayAdapter<CharSequence> yearsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, years );
        binding.spinnerYearMenu.setAdapter(yearsAdapter);

        String[] months = {"01","02","03","04","05","06","07","08","09","10","11","12"};
        ArrayAdapter<CharSequence> monthsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, months );
        binding.spinnerMonthMenu.setAdapter(monthsAdapter);
    }
}


import static com.marwaeltayeb.souq.storage.LanguageUtils.loadLocale;
import static com.marwaeltayeb.souq.utils.Constant.LOCALHOST;
import static com.marwaeltayeb.souq.utils.Constant.PRODUCT;
import static com.marwaeltayeb.souq.utils.Constant.PRODUCTID;
import static com.marwaeltayeb.souq.utils.Constant.PRODUCT_ID;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.marwaeltayeb.souq.R;
import com.marwaeltayeb.souq.adapter.ReviewAdapter;
import com.marwaeltayeb.souq.databinding.ActivityDetailsBinding;
import com.marwaeltayeb.souq.model.Cart;
import com.marwaeltayeb.souq.model.Product;
import com.marwaeltayeb.souq.model.Review;
import com.marwaeltayeb.souq.storage.LoginUtils;
import com.marwaeltayeb.souq.utils.RequestCallback;
import com.marwaeltayeb.souq.viewmodel.ReviewViewModel;
import com.marwaeltayeb.souq.viewmodel.ToCartViewModel;

import java.util.List;

public class DetailsActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "DetailsActivity";

    private ActivityDetailsBinding binding;
    private ReviewViewModel reviewViewModel;
    private ToCartViewModel toCartViewModel;
    private ReviewAdapter reviewAdapter;
    private List<Review> reviewList;
    private Product product;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadLocale(this);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_details);

        reviewViewModel = ViewModelProviders.of(this).get(ReviewViewModel.class);
        toCartViewModel = ViewModelProviders.of(this).get(ToCartViewModel.class);

        binding.txtSeeAllReviews.setOnClickListener(this);
        binding.writeReview.setOnClickListener(this);
        binding.addToCart.setOnClickListener(this);
        binding.buy.setOnClickListener(this);

        getProductDetails();

        setUpRecycleView();

        getReviewsOfProduct();
    }

    private void setUpRecycleView() {
        binding.listOfReviews.setHasFixedSize(true);
        binding.listOfReviews.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.listOfReviews.setItemAnimator(null);
    }

    private void getProductDetails() {
        // Receive the product object
        product = getIntent().getParcelableExtra(PRODUCT);

        Log.d(TAG,"isFavourite " + product.isFavourite() + " isInCart " + product.isInCart());

        // Set Custom ActionBar Layout
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setCustomView(R.layout.action_bar_title_layout);
        ((TextView) findViewById(R.id.action_bar_title)).setText(product.getProductName());

        binding.nameOfProduct.setText(product.getProductName());
        binding.priceOfProduct.setText(String.valueOf(product.getProductPrice()));

        String imageUrl = LOCALHOST + product.getProductImage().replaceAll("\\\\", "/");
        Glide.with(this)
                .load(imageUrl)
                .into(binding.imageOfProduct);
    }

    private void getReviewsOfProduct() {
        reviewViewModel.getReviews(product.getProductId()).observe(this, reviewApiResponse -> {
            if (reviewApiResponse != null) {
                reviewList = reviewApiResponse.getReviewList();
                reviewAdapter = new ReviewAdapter(reviewList);
                binding.listOfReviews.setAdapter(reviewAdapter);
            }

            if(reviewList.size() == 0){
                binding.listOfReviews.setVisibility(View.GONE);
                binding.txtFirst.setVisibility(View.VISIBLE);
            }else {
                binding.listOfReviews.setVisibility(View.VISIBLE);
                binding.txtFirst.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.txtSeeAllReviews) {
            Intent allReviewIntent = new Intent(DetailsActivity.this, AllReviewsActivity.class);
            allReviewIntent.putExtra(PRODUCT_ID,product.getProductId());
            startActivity(allReviewIntent);
        } else if (view.getId() == R.id.writeReview) {
            Intent allReviewIntent = new Intent(DetailsActivity.this, WriteReviewActivity.class);
            allReviewIntent.putExtra(PRODUCT_ID,product.getProductId());
            startActivity(allReviewIntent);
        }else if(view.getId() == R.id.addToCart){
            insertToCart(() -> product.setIsInCart(true));
            Intent cartIntent = new Intent(DetailsActivity.this, CartActivity.class);
            startActivity(cartIntent);
        }else if(view.getId() == R.id.buy){
            Intent shippingIntent = new Intent(DetailsActivity.this, ShippingAddressActivity.class);
            shippingIntent.putExtra(PRODUCTID, product.getProductId());
            startActivity(shippingIntent);
        }
    }

    private void insertToCart(RequestCallback callback) {
        Cart cart = new Cart(LoginUtils.getInstance(this).getUserInfo().getId(), product.getProductId());
        toCartViewModel.addToCart(cart, callback);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getReviewsOfProduct();
    }
}


import static com.marwaeltayeb.souq.utils.Constant.KEYWORD;
import static com.marwaeltayeb.souq.utils.Constant.PRODUCT;
import static com.marwaeltayeb.souq.utils.InternetUtils.isNetworkConnected;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;

import com.marwaeltayeb.souq.R;
import com.marwaeltayeb.souq.adapter.SearchAdapter;
import com.marwaeltayeb.souq.databinding.ActivityResultBinding;
import com.marwaeltayeb.souq.model.Product;
import com.marwaeltayeb.souq.storage.LoginUtils;
import com.marwaeltayeb.souq.viewmodel.SearchViewModel;

import java.util.List;

public class ResultActivity extends AppCompatActivity {

    private ActivityResultBinding binding;
    private SearchAdapter searchAdapter;
    private List<Product> searchedList;
    private SearchViewModel searchViewModel;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_result);

        searchViewModel = ViewModelProviders.of(this).get(SearchViewModel.class);

        Intent intent = getIntent();
        String keyword = intent.getStringExtra(KEYWORD);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(keyword);

        userId = LoginUtils.getInstance(this).getUserInfo().getId();

        if (isNetworkConnected(getApplicationContext())) {
            search(keyword);
        }
    }

    private void search(String query) {

        binding.listOfSearchedList.setHasFixedSize(true);
        binding.listOfSearchedList.setLayoutManager(new GridLayoutManager(this, (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) ? 2 : 4));

        searchViewModel.getProductsBySearch(query, userId).observe(this, productApiResponse -> {
            if (productApiResponse != null) {
                searchedList = productApiResponse.getProducts();
                if (searchedList.isEmpty()) {
                    Toast.makeText(ResultActivity.this, "No Result", Toast.LENGTH_SHORT).show();
                }

                searchAdapter = new SearchAdapter(getApplicationContext(), searchedList, product -> {
                    Intent intent = new Intent(ResultActivity.this, DetailsActivity.class);
                    // Pass an object of product class
                    intent.putExtra(PRODUCT, product);
                    startActivity(intent);
                },this);
            }
            binding.listOfSearchedList.setAdapter(searchAdapter);
        });
    }
}


import static com.marwaeltayeb.souq.storage.LanguageUtils.loadLocale;

import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.marwaeltayeb.souq.R;
import com.marwaeltayeb.souq.adapter.HelpAdapter;
import com.marwaeltayeb.souq.databinding.ActivityHelpBinding;
import com.marwaeltayeb.souq.model.Help;

import java.util.ArrayList;

public class HelpActivity extends AppCompatActivity {

    private ActivityHelpBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadLocale(this);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_help);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getResources().getString(R.string.help_center));

        setUpRecyclerView();
    }

    private void setUpRecyclerView(){
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        binding.helpList.setLayoutManager(layoutManager);
        binding.helpList.setHasFixedSize(true);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this, layoutManager.getOrientation());
        binding.helpList.addItemDecoration(dividerItemDecoration);

        HelpAdapter helpAdapter = new HelpAdapter(getHelpList());
        binding.helpList.setAdapter(helpAdapter);
    }

    private ArrayList<Help> getHelpList() {
        final ArrayList<Help> helpList = new ArrayList<>();
        helpList.add(new Help(getString(R.string.inquiryOne), getString(R.string.answerOne)));
        helpList.add(new Help(getString(R.string.inquiryTwo), getString(R.string.answerTwo)));
        helpList.add(new Help(getString(R.string.inquiryThree), getString(R.string.answerThree)));
        helpList.add(new Help(getString(R.string.inquiryFour), getString(R.string.answerFour)));
        helpList.add(new Help(getString(R.string.inquiryFive), getString(R.string.answerFive)));
        return helpList;
    }
}


import static android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;
import static com.marwaeltayeb.souq.storage.LanguageUtils.loadLocale;
import static com.marwaeltayeb.souq.utils.ProgressDialog.createAlertDialog;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProviders;

import com.marwaeltayeb.souq.R;
import com.marwaeltayeb.souq.databinding.ActivitySignupBinding;
import com.marwaeltayeb.souq.model.User;
import com.marwaeltayeb.souq.storage.LoginUtils;
import com.marwaeltayeb.souq.utils.Validation;
import com.marwaeltayeb.souq.viewmodel.RegisterViewModel;

public class SignUpActivity extends AppCompatActivity implements View.OnClickListener {

    private ActivitySignupBinding binding;
    private RegisterViewModel registerViewModel;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadLocale(this);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_signup);

        binding.buttonSignUp.setOnClickListener(this);
        binding.textViewLogin.setOnClickListener(this);

        registerViewModel = ViewModelProviders.of(this).get(RegisterViewModel.class);

        setBoldStyle();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (LoginUtils.getInstance(this).isLoggedIn()) {
            goToProductActivity();
        }
    }

    private void signUpUser() {
        String name = binding.userName.getText().toString();
        String email = binding.userEmail.getText().toString();
        String password = binding.userPassword.getText().toString();

        if (name.isEmpty()) {
            binding.userName.setError(getString(R.string.name_required));
            binding.userName.requestFocus();
            return;
        }

        if (!Validation.isValidName(name)) {
            binding.userName.setError(getString(R.string.enter_at_least_3_characters));
            binding.userName.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            binding.userEmail.setError(getString(R.string.email_required));
            binding.userEmail.requestFocus();
        }

        if (Validation.isValidEmail(email)) {
            binding.userEmail.setError(getString(R.string.enter_a_valid_email_address));
            binding.userEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            binding.userPassword.setError(getString(R.string.password_required));
            binding.userPassword.requestFocus();
            return;
        }

        if (!Validation.isValidPassword(password)) {
            binding.userPassword.setError(getString(R.string.password__at_least_8_characters));
            binding.userPassword.requestFocus();
            return;
        }

        AlertDialog alert = createAlertDialog(this);

        registerViewModel.getRegisterResponseLiveData(new User(name, email, password)).observe(this, registerApiResponse -> {
            if (!registerApiResponse.isError()) {
                Toast.makeText(this, registerApiResponse.getMessage(), Toast.LENGTH_LONG).show();
                LoginUtils.getInstance(this).saveUserInfo(registerApiResponse.getUser());
                alert.dismiss();
                goToProductActivity();
            }else {
                alert.dismiss();
                Toast.makeText(this, registerApiResponse.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.buttonSignUp:
                signUpUser();
                break;
            case R.id.textViewLogin:
                goToLoginActivity();
                break;
            default: // Should not get here
        }
    }

    private void goToLoginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    private void goToProductActivity() {
        Intent intent = new Intent(this, ProductActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setBoldStyle() {
        String boldText = getString(R.string.boldText);
        String normalText = getString(R.string.normalText);
        SpannableString str = new SpannableString(boldText + normalText);
        str.setSpan(new StyleSpan(Typeface.BOLD), 0, boldText.length(), SPAN_EXCLUSIVE_EXCLUSIVE);
        binding.textViewLogin.setText(str);
    }
}


import static com.marwaeltayeb.souq.storage.LanguageUtils.loadLocale;
import static com.marwaeltayeb.souq.utils.Constant.PRODUCT;
import static com.marwaeltayeb.souq.utils.InternetUtils.isNetworkConnected;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.marwaeltayeb.souq.R;
import com.marwaeltayeb.souq.adapter.WishListAdapter;
import com.marwaeltayeb.souq.databinding.ActivityWishlistBinding;
import com.marwaeltayeb.souq.model.Product;
import com.marwaeltayeb.souq.storage.LoginUtils;
import com.marwaeltayeb.souq.viewmodel.FavoriteViewModel;

import java.util.List;

public class WishListActivity extends AppCompatActivity {

    private ActivityWishlistBinding binding;
    private WishListAdapter wishListAdapter;
    private List<Product> favoriteList;
    private FavoriteViewModel favoriteViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadLocale(this);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_wishlist);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getResources().getString(R.string.my_wishList));

        setUpRecyclerView();

        getFavorites();
    }

    private void setUpRecyclerView() {
        binding.favoriteList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        binding.favoriteList.setHasFixedSize(true);
        favoriteViewModel = ViewModelProviders.of(this).get(FavoriteViewModel.class);
    }

    private void getFavorites() {
        if (isNetworkConnected(this)) {
            favoriteViewModel.getFavorites(LoginUtils.getInstance(this).getUserInfo().getId()).observe(this, favoriteApiResponse -> {
                if (favoriteApiResponse != null) {
                    favoriteList = favoriteApiResponse.getFavorites();
                    if (favoriteList.size() == 0) {
                        binding.noBookmarks.setVisibility(View.VISIBLE);
                        binding.emptyWishlist.setVisibility(View.VISIBLE);
                    }else {
                        binding.favoriteList.setVisibility(View.VISIBLE);
                    }
                    wishListAdapter = new WishListAdapter(getApplicationContext(), favoriteList, product -> {
                        Intent intent = new Intent(WishListActivity.this, DetailsActivity.class);
                        // Pass an object of product class
                        intent.putExtra(PRODUCT, (product));
                        startActivity(intent);
                    },this);
                }
                binding.loadingIndicator.setVisibility(View.GONE);
                binding.favoriteList.setAdapter(wishListAdapter);
            });
        }else {
            binding.emptyWishlist.setVisibility(View.VISIBLE);
            binding.loadingIndicator.setVisibility(View.GONE);
            binding.emptyWishlist.setText(getString(R.string.no_internet_connection));
        }
    }

}


import static com.marwaeltayeb.souq.storage.LanguageUtils.loadLocale;
import static com.marwaeltayeb.souq.utils.Constant.PRODUCT;
import static com.marwaeltayeb.souq.utils.InternetUtils.isNetworkConnected;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.marwaeltayeb.souq.R;
import com.marwaeltayeb.souq.adapter.CartAdapter;
import com.marwaeltayeb.souq.databinding.ActivityCartBinding;
import com.marwaeltayeb.souq.model.Product;
import com.marwaeltayeb.souq.storage.LoginUtils;
import com.marwaeltayeb.souq.viewmodel.CartViewModel;

import java.util.List;

public class CartActivity extends AppCompatActivity {

    private ActivityCartBinding binding;
    private CartAdapter cartAdapter;
    private List<Product> favoriteList;
    private CartViewModel cartViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadLocale(this);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_cart);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getResources().getString(R.string.cart));

        setUpRecyclerView();

        getProductsInCart();
    }

    private void setUpRecyclerView() {
        binding.productsInCart.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        binding.productsInCart.setHasFixedSize(true);
        cartViewModel = ViewModelProviders.of(this).get(CartViewModel.class);
    }

    private void getProductsInCart() {
        if (isNetworkConnected(this)) {
            cartViewModel.getProductsInCart(LoginUtils.getInstance(this).getUserInfo().getId()).observe(this, cartApiResponse -> {
                if (cartApiResponse != null) {
                    favoriteList = cartApiResponse.getProductsInCart();
                    if (favoriteList.size() == 0) {
                        binding.noBookmarks.setVisibility(View.VISIBLE);
                        binding.emptyCart.setVisibility(View.VISIBLE);
                    } else {
                        binding.productsInCart.setVisibility(View.VISIBLE);
                    }
                    cartAdapter = new CartAdapter(getApplicationContext(), favoriteList, product -> {
                        Intent intent = new Intent(CartActivity.this, DetailsActivity.class);
                        // Pass an object of product class
                        intent.putExtra(PRODUCT, (product));
                        startActivity(intent);
                    }, this);
                }

                binding.loadingIndicator.setVisibility(View.GONE);
                binding.productsInCart.setAdapter(cartAdapter);
            });
        } else {
            binding.emptyCart.setVisibility(View.VISIBLE);
            binding.loadingIndicator.setVisibility(View.GONE);
            binding.emptyCart.setText(getString(R.string.no_internet_connection));
        }
    }
}


import static com.marwaeltayeb.souq.storage.LanguageUtils.loadLocale;
import static com.marwaeltayeb.souq.utils.Constant.CAMERA_PERMISSION_CODE;
import static com.marwaeltayeb.souq.utils.Constant.CATEGORY;
import static com.marwaeltayeb.souq.utils.Constant.LOCALHOST;
import static com.marwaeltayeb.souq.utils.Constant.PRODUCT;
import static com.marwaeltayeb.souq.utils.Constant.READ_EXTERNAL_STORAGE_CODE;
import static com.marwaeltayeb.souq.utils.ImageUtils.getImageUri;
import static com.marwaeltayeb.souq.utils.ImageUtils.getRealPathFromURI;
import static com.marwaeltayeb.souq.utils.InternetUtils.isNetworkConnected;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProviders;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.marwaeltayeb.souq.R;
import com.marwaeltayeb.souq.adapter.ProductAdapter;
import com.marwaeltayeb.souq.databinding.ActivityProductBinding;
import com.marwaeltayeb.souq.model.Product;
import com.marwaeltayeb.souq.receiver.NetworkChangeReceiver;
import com.marwaeltayeb.souq.storage.LoginUtils;
import com.marwaeltayeb.souq.utils.FlagsManager;
import com.marwaeltayeb.souq.utils.OnNetworkListener;
import com.marwaeltayeb.souq.utils.Slide;
import com.marwaeltayeb.souq.viewmodel.HistoryViewModel;
import com.marwaeltayeb.souq.viewmodel.ProductViewModel;
import com.marwaeltayeb.souq.viewmodel.UploadPhotoViewModel;
import com.marwaeltayeb.souq.viewmodel.UserImageViewModel;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProductActivity extends AppCompatActivity implements View.OnClickListener, OnNetworkListener, ProductAdapter.ProductAdapterOnClickHandler,
        NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "ProductActivity";
    private ActivityProductBinding binding;

    private ProductAdapter mobileAdapter;
    private ProductAdapter laptopAdapter;
    private ProductAdapter historyAdapter;

    private ProductViewModel productViewModel;
    private HistoryViewModel historyViewModel;
    private UploadPhotoViewModel uploadPhotoViewModel;
    private UserImageViewModel userImageViewModel;

    private Snackbar snack;

    private CircleImageView circleImageView;

    private NetworkChangeReceiver mNetworkReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadLocale(this);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_product);

        int userID = LoginUtils.getInstance(this).getUserInfo().getId();

        productViewModel = ViewModelProviders.of(this).get(ProductViewModel.class);
        productViewModel.loadMobiles("mobile", userID);
        productViewModel.loadLaptops("laptop",userID);
        historyViewModel = ViewModelProviders.of(this).get(HistoryViewModel.class);
        historyViewModel.loadHistory(userID);
        uploadPhotoViewModel = ViewModelProviders.of(this).get(UploadPhotoViewModel.class);
        userImageViewModel = ViewModelProviders.of(this).get(UserImageViewModel.class);

        snack = Snackbar.make(findViewById(android.R.id.content), getResources().getString(R.string.no_internet_connection), Snackbar.LENGTH_INDEFINITE);

        binding.included.content.txtSeeAllMobiles.setOnClickListener(this);
        binding.included.content.txtSeeAllLaptops.setOnClickListener(this);
        binding.included.content.txtCash.setOnClickListener(this);
        binding.included.content.txtReturn.setOnClickListener(this);
        binding.included.txtSearch.setOnClickListener(this);

        setUpViews();

        getMobiles();
        getLaptops();
        getHistory();
        getUserImage();

        flipImages(Slide.getSlides());

        mNetworkReceiver = new NetworkChangeReceiver();
        mNetworkReceiver.setOnNetworkListener(this);
    }

    private void setUpViews() {
        Toolbar toolbar = binding.included.toolbar;
        setSupportActionBar(toolbar);

        DrawerLayout drawer = binding.drawerLayout;

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        binding.navView.setNavigationItemSelectedListener(this);

        View headerContainer = binding.navView.getHeaderView(0);
        circleImageView = headerContainer.findViewById(R.id.profile_image);
        circleImageView.setOnClickListener(this);
        TextView userName = headerContainer.findViewById(R.id.nameOfUser);
        userName.setText(LoginUtils.getInstance(this).getUserInfo().getName());
        TextView userEmail = headerContainer.findViewById(R.id.emailOfUser);
        userEmail.setText(LoginUtils.getInstance(this).getUserInfo().getEmail());

        binding.included.content.listOfMobiles.setHasFixedSize(true);
        binding.included.content.listOfMobiles.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.included.content.listOfMobiles.setItemAnimator(null);

        binding.included.content.listOfLaptops.setHasFixedSize(true);
        binding.included.content.listOfLaptops.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.included.content.listOfLaptops.setItemAnimator(null);

        binding.included.content.historyList.setHasFixedSize(true);
        binding.included.content.historyList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.included.content.historyList.setItemAnimator(null);

        mobileAdapter = new ProductAdapter(this, this);
        laptopAdapter = new ProductAdapter(this, this);
        historyAdapter = new ProductAdapter(this, this);

        if (FlagsManager.getInstance().isHistoryDeleted()) {
            binding.included.content.textViewHistory.setVisibility(View.GONE);
        }
    }

    private void getMobiles() {
        if (isNetworkConnected(this)) {
            productViewModel.productPagedList.observe(this, products -> mobileAdapter.submitList(products));

            binding.included.content.listOfMobiles.setAdapter(mobileAdapter);
        } else {
            showOrHideViews(View.INVISIBLE);
            showSnackBar();
        }
    }

    private void getLaptops() {
        if (isNetworkConnected(this)) {
            productViewModel.laptopPagedList.observe(this, products -> laptopAdapter.submitList(products));

            binding.included.content.listOfLaptops.setAdapter(laptopAdapter);
        } else {
            showOrHideViews(View.INVISIBLE);
            showSnackBar();
        }
    }

    private void getHistory() {
        if (isNetworkConnected(this)) {
            historyViewModel.historyPagedList.observe(this, products -> {
                binding.included.content.historyList.setAdapter(historyAdapter);
                historyAdapter.submitList(products);
                historyAdapter.notifyDataSetChanged();
                
                products.addWeakCallback(null, productCallback);
            });
        } else {
            showOrHideViews(View.INVISIBLE);
            binding.included.content.textViewHistory.setVisibility(View.GONE);
            showSnackBar();
        }

    }

    private void flipImages(List<Integer> images) {
        for (int image : images) {
            ImageView imageView = new ImageView(this);
            imageView.setBackgroundResource(image);
            binding.included.content.imageSlider.addView(imageView);
        }

        binding.included.content.imageSlider.setFlipInterval(2000);
        binding.included.content.imageSlider.setAutoStart(true);

        // Set the animation for the view that enters the screen
        binding.included.content.imageSlider.setInAnimation(this, R.anim.slide_in_right);
        // Set the animation for the view leaving th screen
        binding.included.content.imageSlider.setOutAnimation(this, R.anim.slide_out_left);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.txtSeeAllMobiles:
                Intent mobileIntent = new Intent(this, AllMobilesActivity.class);
                startActivity(mobileIntent);
                break;
            case R.id.txtSeeAllLaptops:
                Intent laptopIntent = new Intent(this, AllLaptopsActivity.class);
                startActivity(laptopIntent);
                break;
            case R.id.profile_image:
                showCustomAlertDialog();
                break;
            case R.id.txtCash:
                showNormalAlertDialog(getString(R.string.cash));
                break;
            case R.id.txtReturn:
                showNormalAlertDialog(getString(R.string.returnProduct));
                break;
            case R.id.txtSearch:
                Intent searchIntent = new Intent(ProductActivity.this, SearchActivity.class);
                startActivity(searchIntent);
                break;
            default: // Should not get here
        }
    }

    private void showNormalAlertDialog(String message) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton(R.string.ok, null).show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, R.color.darkGreen));
    }

    private void showCustomAlertDialog() {
        final Dialog dialog = new Dialog(ProductActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.custom_image_dialog);

        Button takePicture = dialog.findViewById(R.id.takePicture);
        Button useGallery = dialog.findViewById(R.id.useGallery);

        takePicture.setEnabled(true);
        useGallery.setEnabled(true);

        takePicture.setOnClickListener(v -> {
            launchCamera();
            dialog.cancel();
        });

        useGallery.setOnClickListener(v -> {
            getImageFromGallery();
            dialog.cancel();
        });

        dialog.show();
    }

    private void getImageFromGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE_CODE);
            } else {
                try {
                    Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    getIntent.setType("image/*");

                    Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                    Intent chooserIntent = Intent.createChooser(getIntent, "Select Image");
                    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{pickIntent});
                    getImageFromGallery.launch(chooserIntent);
                } catch (Exception exp) {
                    Log.i("Error", exp.toString());
                }
            }
        }
    }

    private void launchCamera() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, CAMERA_PERMISSION_CODE);
            } else {
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                getImageFromCamera.launch(cameraIntent);
            }
        }
    }

    ActivityResultLauncher<Intent> getImageFromGallery = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    Uri imageUri = data.getData();
                    circleImageView.setImageURI(imageUri);

                    String filePath = getRealPathFromURI(this, imageUri);
                    Log.d(TAG, "getImageFromGallery: " + filePath);

                    uploadPhoto(filePath);
                }
            });

    ActivityResultLauncher<Intent> getImageFromCamera = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    Bitmap photo = (Bitmap) data.getExtras().get("data");
                    circleImageView.setImageBitmap(photo);

                    Uri imageUri = getImageUri(this, photo);
                    String filePath = getRealPathFromURI(this, imageUri);
                    Log.d(TAG, "getImageFromCamera: " + filePath);

                    uploadPhoto(filePath);
                }
            });


    private void uploadPhoto(String pathname) {
        uploadPhotoViewModel.uploadPhoto(pathname).observe(this, responseBody -> {
            Log.d(TAG, "Image Uploaded");
            getUserImage();
        });
    }

    private void getUserImage() {
        userImageViewModel.getUserImage(LoginUtils.getInstance(this).getUserInfo().getId()).observe(this, response -> {
            Log.d(TAG,  "getUserImage");

            if (response != null) {
                String imageUrl = LOCALHOST + response.getImagePath().replaceAll("\\\\", "/");

                RequestOptions options = new RequestOptions()
                        .centerCrop()
                        .placeholder(R.drawable.profile_picture)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .priority(Priority.HIGH)
                        .dontAnimate()
                        .dontTransform();

                Glide.with(getApplicationContext())
                        .load(imageUrl)
                        .apply(options)
                        .into(circleImageView);
            }
        });
    }

    public void showSnackBar() {
        snack.getView().setBackgroundColor(ContextCompat.getColor(this, R.color.red));
        snack.show();
    }

    public void hideSnackBar() {
        snack.dismiss();
    }

    private void registerNetworkBroadcastForNougat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            registerReceiver(mNetworkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerNetworkBroadcastForNougat();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mNetworkReceiver);
    }

    @Override
    public void onNetworkConnected() {
        hideSnackBar();
        showOrHideViews(View.VISIBLE);
        getMobiles();
        getLaptops();
        getHistory();
        Log.d(TAG, "onNetworkConnected");
        getUserImage();
    }

    @Override
    public void onNetworkDisconnected() {
        showSnackBar();
    }

    @Override
    public void onClick(Product product) {
        Intent intent = new Intent(ProductActivity.this, DetailsActivity.class);
        // Pass an object of product class
        intent.putExtra(PRODUCT, (product));
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search, menu);

        MenuItem addMenu = menu.findItem(R.id.action_addProduct);
        boolean isAdmin = LoginUtils.getInstance(this).getUserInfo().isAdmin();
        addMenu.setVisible(isAdmin);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_cart:
                Intent cartIntent = new Intent(this, CartActivity.class);
                startActivity(cartIntent);
                return true;
            case R.id.action_addProduct:
                Intent addProductIntent = new Intent(this, AddProductActivity.class);
                startActivity(addProductIntent);
                return true;
            default: return super.onOptionsItemSelected(item);
        }
    }

    private void showOrHideViews(int view) {
        binding.included.content.textViewMobiles.setVisibility(view);
        binding.included.content.txtSeeAllMobiles.setVisibility(view);
        binding.included.content.textViewLaptops.setVisibility(view);
        binding.included.content.txtSeeAllLaptops.setVisibility(view);
        binding.included.content.txtCash.setVisibility(view);
        binding.included.content.txtReturn.setVisibility(view);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        int id = menuItem.getItemId();

        if (id == R.id.nav_mobiles) {
            goToCategoryActivity("Mobile");
        } else if (id == R.id.nav_laptops) {
            goToCategoryActivity("Laptop");
        } else if (id == R.id.nav_babies) {
            goToCategoryActivity("Baby");
        } else if (id == R.id.nav_toys) {
            goToCategoryActivity("Toy");
        } else if (id == R.id.nav_trackOrder) {
            Intent orderIntent = new Intent(this, OrdersActivity.class);
            startActivity(orderIntent);
        } else if (id == R.id.nav_myAccount) {
            Intent accountIntent = new Intent(this, AccountActivity.class);
            startActivity(accountIntent);
        } else if (id == R.id.nav_newsFeed) {
            Intent newsFeedIntent = new Intent(this, NewsFeedActivity.class);
            startActivity(newsFeedIntent);
        } else if (id == R.id.nav_wishList) {
            Intent wishListIntent = new Intent(this, WishListActivity.class);
            startActivity(wishListIntent);
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void goToCategoryActivity(String category) {
        Intent categoryIntent = new Intent(this, CategoryActivity.class);
        categoryIntent.putExtra(CATEGORY, category);
        startActivity(categoryIntent);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            closeApplication();
        }
    }

    private void closeApplication() {
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage(R.string.want_to_exit)
                .setPositiveButton(R.string.ok, (dialog, which) -> finish())
                .setNegativeButton(R.string.cancel, null)
                .show();

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this,R.color.darkGreen));
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, R.color.darkGreen));
    }


    @Override
    protected void onResume() {
        super.onResume();
        productViewModel.invalidate();
        getMobiles();
        getLaptops();
        historyViewModel.invalidate();
        getHistory();
    }

    private final PagedList.Callback productCallback = new PagedList.Callback() {
        @Override
        public void onChanged(int position, int count) {
            Log.d(TAG, "onChanged: "+ count);
        }

        @Override
        public void onInserted(int position, int count) {
            Log.d(TAG, "onInserted: "+ count);
            if (count != 0) {
                binding.included.content.textViewHistory.setVisibility(View.VISIBLE);
                historyAdapter.notifyOnInsertedItem(position);
                getHistory();
            }
        }

        @Override
        public void onRemoved(int position, int count) {
            Log.d(TAG, "onRemoved: "+ count);
        }
    };

}


import static com.marwaeltayeb.souq.storage.LanguageUtils.getEnglishState;
import static com.marwaeltayeb.souq.storage.LanguageUtils.loadLocale;
import static com.marwaeltayeb.souq.storage.LanguageUtils.setEnglishState;
import static com.marwaeltayeb.souq.storage.LanguageUtils.setLocale;
import static com.marwaeltayeb.souq.utils.CommunicateUtils.rateAppOnGooglePlay;
import static com.marwaeltayeb.souq.utils.CommunicateUtils.shareApp;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProviders;

import com.marwaeltayeb.souq.R;
import com.marwaeltayeb.souq.databinding.ActivityAccountBinding;
import com.marwaeltayeb.souq.storage.LoginUtils;
import com.marwaeltayeb.souq.utils.FlagsManager;
import com.marwaeltayeb.souq.viewmodel.DeleteUserViewModel;
import com.marwaeltayeb.souq.viewmodel.FromHistoryViewModel;

import java.io.IOException;

public class AccountActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "AccountActivity";
    private DeleteUserViewModel deleteUserViewModel;
    private FromHistoryViewModel fromHistoryViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadLocale(this);
        ActivityAccountBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_account);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getResources().getString(R.string.my_account));

        deleteUserViewModel = ViewModelProviders.of(this).get(DeleteUserViewModel.class);
        fromHistoryViewModel = ViewModelProviders.of(this).get(FromHistoryViewModel.class);

        binding.nameOfUser.setText(LoginUtils.getInstance(this).getUserInfo().getName());
        binding.emailOfUser.setText(LoginUtils.getInstance(this).getUserInfo().getEmail());

        binding.myOrders.setOnClickListener(this);
        binding.myWishList.setOnClickListener(this);
        binding.languages.setOnClickListener(this);
        binding.helpCenter.setOnClickListener(this);
        binding.shareWithFriends.setOnClickListener(this);
        binding.rateUs.setOnClickListener(this);
        binding.changePassword.setOnClickListener(this);
        binding.deleteAccount.setOnClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.account, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_signOut) {
            signOut();
            deleteAllProductsInHistory();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void signOut() {
        LoginUtils.getInstance(this).clearAll();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void deleteAllProductsInHistory() {
       fromHistoryViewModel.removeAllFromHistory().observe(this, responseBody -> Log.d(TAG,getString(R.string.all_removed)));
       FlagsManager.getInstance().setHistoryDeleted(true);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.myOrders:
                Intent ordersIntent = new Intent(this, OrdersActivity.class);
                startActivity(ordersIntent);
                break;
            case R.id.myWishList:
                Intent wishListIntent = new Intent(this, WishListActivity.class);
                startActivity(wishListIntent);
                break;
            case R.id.languages:
                showCustomAlertDialog();
                break;
            case R.id.helpCenter:
                Intent helpCenterIntent = new Intent(this, HelpActivity.class);
                startActivity(helpCenterIntent);
                break;
            case R.id.shareWithFriends:
                shareApp(this);
                break;
            case R.id.rateUs:
                rateAppOnGooglePlay(this);
                break;
            case R.id.changePassword:
                Intent passwordIntent = new Intent(this, PasswordActivity.class);
                startActivity(passwordIntent);
                break;
            case R.id.deleteAccount:
                deleteAccount();
                break;
            default: // Should not get here
        }
    }

    private void deleteAccount() {
        deleteUserViewModel.deleteUser(LoginUtils.getInstance(this).getUserToken(), LoginUtils.getInstance(this).getUserInfo().getId()).observe(this, responseBody -> {
            if(responseBody!= null){
                LoginUtils.getInstance(getApplicationContext()).clearAll();
                try {
                    Toast.makeText(AccountActivity.this, responseBody.string() + "", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "onResponse: delete account" + responseBody.string());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                goToLoginActivity();
            }
        });
    }

    private void goToLoginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void showCustomAlertDialog() {
        final Dialog dialog = new Dialog(AccountActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.custom_language_dialog);

        Button english = dialog.findViewById(R.id.txtEnglish);
        Button arabic = dialog.findViewById(R.id.txtArabic);

        if(getEnglishState(this)){
            english.setEnabled(false);
            english.setAlpha(.5f);
            arabic.setEnabled(true);
        }else {
            arabic.setEnabled(false);
            arabic.setAlpha(.5f);
            english.setEnabled(true);
        }

        english.setOnClickListener(v -> {
            english.setEnabled(true);
            chooseEnglish();
            dialog.cancel();
        });

        arabic.setOnClickListener(v -> {
            arabic.setEnabled(true);
            chooseArabic();
            dialog.cancel();
        });

        dialog.show();
    }

    private void chooseEnglish() {
        setLocale(this,"en");
        recreate();
        Toast.makeText(this, "English", Toast.LENGTH_SHORT).show();
        setEnglishState(this, true);
    }

    private void chooseArabic() {
        setLocale(this,"ar");
        recreate();
        Toast.makeText(this, "Arabic", Toast.LENGTH_SHORT).show();
        setEnglishState(this, false);
    }
}

import static com.marwaeltayeb.souq.utils.Constant.ORDER;
import static com.marwaeltayeb.souq.utils.Constant.PRODUCTID;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.marwaeltayeb.souq.R;
import com.marwaeltayeb.souq.databinding.ActivityStatusBinding;
import com.marwaeltayeb.souq.model.Order;

public class StatusActivity extends AppCompatActivity implements View.OnClickListener {

    private int productId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityStatusBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_status);

        // Receive Order object
        Intent intent = getIntent();
        Order order = (Order) intent.getSerializableExtra(ORDER);

        productId = order.getProductId();
        binding.orderDate.setText(order.getOrderDate());
        binding.orderNumber.setText(order.getOrderNumber());
        binding.userName.setText(order.getUserName());
        binding.userAddress.setText(order.getShippingAddress());
        binding.userPhone.setText(order.getShippingPhone());
        binding.txtProductName.setText(order.getProductName());
        binding.txtProductPrice.setText(String.valueOf(order.getProductPrice()));
        String status = getString(R.string.item, order.getOrderDateStatus());
        binding.orderStatus.setText(status);

        binding.reOrder.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.reOrder) {
            Intent reOrderIntent = new Intent(this, OrderProductActivity.class);
            reOrderIntent.putExtra(PRODUCTID, productId);
            startActivity(reOrderIntent);
        }
    }
}


import static com.marwaeltayeb.souq.storage.LanguageUtils.loadLocale;
import static com.marwaeltayeb.souq.utils.Constant.PRODUCT;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;

import com.marwaeltayeb.souq.R;
import com.marwaeltayeb.souq.adapter.ProductAdapter;
import com.marwaeltayeb.souq.databinding.ActivityAllMobilesBinding;
import com.marwaeltayeb.souq.model.Product;
import com.marwaeltayeb.souq.storage.LoginUtils;
import com.marwaeltayeb.souq.viewmodel.ProductViewModel;

public class AllMobilesActivity extends AppCompatActivity implements ProductAdapter.ProductAdapterOnClickHandler{

    private ActivityAllMobilesBinding binding;
    private ProductAdapter productAdapter;
    private ProductViewModel productViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadLocale(this);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_all_mobiles);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getResources().getString(R.string.all_mobiles));

        int userID = LoginUtils.getInstance(this).getUserInfo().getId();

        productViewModel = ViewModelProviders.of(this).get(ProductViewModel.class);
        productViewModel.loadMobiles("mobile", userID);

        setupRecyclerViews();

        getAllMobiles();
    }

    private void setupRecyclerViews() {
        // Mobiles
        binding.allMobilesRecyclerView.setLayoutManager(new GridLayoutManager(this, (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) ? 2 : 4));
        binding.allMobilesRecyclerView.setHasFixedSize(true);
        productAdapter = new ProductAdapter(this,this);
    }

    public void getAllMobiles() {
        // Observe the productPagedList from ViewModel
        productViewModel.productPagedList.observe(this, products -> {
            productAdapter.submitList(products);
        });

        binding.allMobilesRecyclerView.setAdapter(productAdapter);
    }

    @Override
    public void onClick(Product product) {
        Intent intent = new Intent(AllMobilesActivity.this, DetailsActivity.class);
        // Pass an object of product class
        intent.putExtra(PRODUCT, (product));
        startActivity(intent);
    }
}


import static com.marwaeltayeb.souq.storage.LanguageUtils.loadLocale;
import static com.marwaeltayeb.souq.utils.Constant.READ_EXTERNAL_STORAGE_CODE;
import static com.marwaeltayeb.souq.utils.ImageUtils.getRealPathFromURI;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProviders;

import com.marwaeltayeb.souq.R;
import com.marwaeltayeb.souq.databinding.ActivityAddProductBinding;
import com.marwaeltayeb.souq.storage.LoginUtils;
import com.marwaeltayeb.souq.viewmodel.AddProductViewModel;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class AddProductActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "AddProductActivity";
    private static final String IMAGE = "image/*";
    private ActivityAddProductBinding binding;
    private AddProductViewModel addProductViewModel;
    private String filePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadLocale(this);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_add_product);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getResources().getString(R.string.add_product));

        addProductViewModel = ViewModelProviders.of(this).get(AddProductViewModel.class);

        binding.btnSelectImage.setOnClickListener(this);

        populateSpinner();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_save) {
            addProduct(filePath);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void addProduct(String pathname) {
        String nameString = binding.txtName.getText().toString().trim();
        String priceString = binding.txtPrice.getText().toString().trim();
        String quantityString = binding.txtQuantity.getText().toString().trim();
        String supplierString = binding.txtSupplier.getText().toString().trim();
        String categoryString = binding.categorySpinner.getSelectedItem().toString().toLowerCase();

        // Check if there are no empty values
        if (TextUtils.isEmpty(nameString) || TextUtils.isEmpty(priceString) ||
                TextUtils.isEmpty(quantityString) || TextUtils.isEmpty(supplierString)
                || TextUtils.isEmpty(categoryString)) {
            Toast.makeText(this, getString(R.string.required_data), Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, RequestBody> map = new HashMap<>();
        map.put("name", toRequestBody(nameString));
        map.put("price", toRequestBody(priceString));
        map.put("quantity", toRequestBody(quantityString));
        map.put("supplier", toRequestBody(supplierString));
        map.put("category", toRequestBody(categoryString));

        if (TextUtils.isEmpty(pathname)) {
            Toast.makeText(this, "No picture is chosen", Toast.LENGTH_SHORT).show();
            return;
        }

        // Pathname
        File file = new File(pathname);
        RequestBody requestFile = RequestBody.create(MediaType.parse(IMAGE), file);
        MultipartBody.Part photo = MultipartBody.Part.createFormData("image", file.getName(), requestFile);

        String token = LoginUtils.getInstance(this).getUserToken();

        addProductViewModel.addProduct(token,map, photo).observe(this, responseBody -> {
            try {
                if (responseBody != null) {
                    Toast.makeText(AddProductActivity.this, responseBody.string() + "", Toast.LENGTH_SHORT).show();
                    finish();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public static RequestBody toRequestBody(String value) {
        return RequestBody.create(MediaType.parse("text/plain"), value);
    }

    private void populateSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.categories_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.categorySpinner.setAdapter(adapter);
    }

    private void getImageFromGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (AddProductActivity.this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE_CODE);
            } else {

                try {
                    Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    getIntent.setType(IMAGE);

                    Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    pickIntent.setType(IMAGE);

                    Intent chooserIntent = Intent.createChooser(getIntent, "Select Image");
                    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{pickIntent});

                    addProductActivityResultLauncher.launch(chooserIntent);
                } catch (Exception exp) {
                    Log.i("Error", exp.toString());
                }
            }
        }
    }

    ActivityResultLauncher<Intent> addProductActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    Uri selectedImage = data.getData();
                    binding.imageOfProduct.setImageURI(selectedImage);

                    filePath = getRealPathFromURI(getApplicationContext(), selectedImage);
                    Log.d(TAG, "onActivityResult: " + filePath);
                }
            });

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btnSelectImage) {
            getImageFromGallery();
        }
    }
}

