package com.zype.android.ui.Gallery;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.zype.android.Auth.AuthHelper;
import com.zype.android.Billing.SubscriptionsHelper;
import com.zype.android.Db.Entity.Playlist;
import com.zype.android.Db.Entity.PlaylistItem;
import com.zype.android.Db.Entity.Video;
import com.zype.android.R;
import com.zype.android.ZypeConfiguration;
import com.zype.android.core.settings.SettingsProvider;
import com.zype.android.ui.NavigationHelper;
import com.zype.android.ui.main.fragments.playlist.PlaylistActivity;
import com.zype.android.ui.main.fragments.videos.VideosActivity;
import com.zype.android.ui.main.fragments.videos.VideosCursorAdapter;
import com.zype.android.utils.BundleConstants;
import com.zype.android.utils.Logger;
import com.zype.android.utils.UiUtils;
import com.zype.android.webapi.model.video.Thumbnail;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static android.view.View.GONE;

/**
 * Created by Evgeny Cherkasov on 14.06.2018
 */
public class GalleryRowItemsAdapter extends RecyclerView.Adapter<GalleryRowItemsAdapter.ViewHolder> {
    private List<? extends PlaylistItem> items;
    private String playlistId;

    public GalleryRowItemsAdapter() {
        items = new ArrayList<>();
    }

    public void setData(List<? extends PlaylistItem> items, String playlistId) {
        this.items = items;
        this.playlistId = playlistId;
        notifyDataSetChanged();
    }

    @Override
    public GalleryRowItemsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.gallery_row_list_item, parent, false);
        return new GalleryRowItemsAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final GalleryRowItemsAdapter.ViewHolder holder, int position) {
        holder.item = items.get(position);

        updateTitle(holder);
        loadThumbnail(holder);
        updateLockIcon(holder);

        holder.view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavigationHelper navigationHelper = NavigationHelper.getInstance(holder.view.getContext());
                if (holder.item instanceof Video) {
                    Video video = (Video) holder.item;
                    if (AuthHelper.isVideoAuthorized(holder.view.getContext(), video.id)) {
                        navigationHelper.switchToVideoDetailsScreen((Activity) holder.view.getContext(), video.id,
                                        playlistId, true);
                    }
                    else {
                        navigationHelper.handleNotAuthorizedVideo((Activity) holder.view.getContext(), video.id);
                    }
//                    if (ZypeConfiguration.isUniversalTVODEnabled(holder.view.getContext())
//                            && Boolean.valueOf(video.purchaseRequired)) {
//                        if (Boolean.valueOf(video.isEntitled.toString())) {
//                            NavigationHelper.getInstance(holder.view.getContext())
//                                    .switchToVideoDetailsScreen((Activity) holder.view.getContext(), video.id,
//                                            playlistId, true);
//                        }
//                        else {
//                            if (SettingsProvider.getInstance().isLoggedIn()) {
//                                // TODO: Need to request video entitlement
////                                requestEntitled(video.id);
//                            }
//                            else {
//                                NavigationHelper.getInstance(holder.view.getContext())
//                                        .switchToLoginScreen((Activity) holder.view.getContext());
//                            }
//                        }
//                        return;
//                    }
//                    if (Integer.valueOf(video.subscriptionRequired) == 1) {
//                        NavigationHelper.getInstance(holder.view.getContext())
//                                .checkSubscription((Activity) holder.view.getContext(), video.id,
//                                        playlistId, Boolean.valueOf(video.onAir.toString()));
//                    }
//                    else {
//                        NavigationHelper.getInstance(holder.view.getContext())
//                                .switchToVideoDetailsScreen((Activity) holder.view.getContext(), video.id,
//                                        playlistId, true);
//                    }
                }
                else if (holder.item instanceof Playlist) {
                    Playlist playlist = (Playlist) holder.item;
                    if (playlist.playlistItemCount > 0) {
                        navigationHelper.switchToPlaylistVideosScreen((Activity) holder.view.getContext(), playlist.id);                    }
                    else {
                        navigationHelper.switchToGalleryScreen((Activity) holder.view.getContext(), playlist.id);
                    }
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        if (items == null) {
            return 0;
        }
        else {
            return items.size();
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        public final View view;
        public PlaylistItem item;
        public FrameLayout layoutTitle;
        public TextView textTitle;
        public ImageView imageThumbnail;
        public ImageView imageLocked;

        public ViewHolder(View view) {
            super(view);
            this.view = view;
            layoutTitle = view.findViewById(R.id.layoutTitle);
            textTitle = view.findViewById(R.id.textTitle);
            imageThumbnail = view.findViewById(R.id.imageThumbnail);
            imageLocked = view.findViewById(R.id.imageLocked);
        }
    }

    private void loadThumbnail(ViewHolder holder) {
        if (holder.item instanceof Video) {
            Video video = (Video) holder.item;
            if (video.thumbnails != null) {
                Type thumbnailType = new TypeToken<List<Thumbnail>>(){}.getType();
                List<Thumbnail> thumbnails = new Gson().fromJson(video.thumbnails, thumbnailType);
                if (thumbnails.size() > 0) {
                    UiUtils.loadImage(holder.view.getContext(), thumbnails.get(1).getUrl(),
                            R.drawable.outline_play_circle_filled_white_white_48, holder.imageThumbnail, null);
                }
                else {
                    holder.imageThumbnail.setImageDrawable(ContextCompat.getDrawable(holder.view.getContext(),
                            R.drawable.outline_play_circle_filled_white_white_48));
                }
            }
        }
        else if (holder.item instanceof Playlist) {
            Playlist playlist = (Playlist) holder.item;
            if (playlist.thumbnails != null) {
                Type thumbnailType = new TypeToken<List<Thumbnail>>(){}.getType();
                List<Thumbnail> thumbnails = new Gson().fromJson(playlist.thumbnails, thumbnailType);
                if (thumbnails.size() > 0) {
                    UiUtils.loadImage(holder.view.getContext(), thumbnails.get(1).getUrl(),
                            R.drawable.outline_video_library_white_48, holder.imageThumbnail, null);
                }
                else {
                    holder.imageThumbnail.setImageDrawable(ContextCompat.getDrawable(holder.view.getContext(),
                            R.drawable.outline_video_library_white_48));
                }
            }
        }
    }

    private void updateTitle(ViewHolder holder) {
        if (ZypeConfiguration.playlistGalleryItemTitles(holder.view.getContext())) {
            holder.layoutTitle.setVisibility(View.VISIBLE);
            holder.textTitle.setText(holder.item.getTitle());
        }
        else {
            holder.layoutTitle.setVisibility(GONE);
        }
    }

    private void updateLockIcon(ViewHolder holder) {
        if (holder.item instanceof Video) {
            Video video = (Video) holder.item;
            if (AuthHelper.isVideoAuthorized(holder.view.getContext(), video.id)) {
                holder.imageLocked.setVisibility(GONE);
            }
            else {
                holder.imageLocked.setVisibility(View.VISIBLE);
            }
        }
        else {
            holder.imageLocked.setVisibility(GONE);
        }
    }

}

