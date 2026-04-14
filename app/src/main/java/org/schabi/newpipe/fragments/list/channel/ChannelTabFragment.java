package org.schabi.newpipe.fragments.list.channel;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.schabi.newpipe.R;
import org.schabi.newpipe.error.UserAction;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.channel.ChannelTabInfo;
import org.schabi.newpipe.extractor.linkhandler.ChannelTabs;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.fragments.list.BaseListInfoFragment;
import org.schabi.newpipe.player.playqueue.PlayQueue;
import org.schabi.newpipe.player.playqueue.SinglePlayQueue;
import org.schabi.newpipe.util.Constants;
import org.schabi.newpipe.util.ExtractorHelper;
import org.schabi.newpipe.util.NavigationHelper;
import org.schabi.newpipe.util.OnClickGesture;

import icepick.State;
import io.reactivex.rxjava3.core.Single;

import java.util.ArrayList;
import java.util.List;

public class ChannelTabFragment extends BaseListInfoFragment<InfoItem, ChannelTabInfo> {

    @State
    protected int serviceId = Constants.NO_SERVICE_ID;

    @State
    protected ListLinkHandler tabHandler;

    @State
    protected String channelName;

    public static ChannelTabFragment getInstance(final int serviceId,
                                                 final ListLinkHandler tabHandler,
                                                 final String channelName) {
        final ChannelTabFragment instance = new ChannelTabFragment();
        instance.serviceId = serviceId;
        instance.tabHandler = tabHandler;
        instance.channelName = channelName;
        return instance;
    }

    public ChannelTabFragment() {
        super(UserAction.REQUESTED_CHANNEL);
    }

    /*//////////////////////////////////////////////////////////////////////////
    // LifeCycle
    //////////////////////////////////////////////////////////////////////////*/

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_channel_tab, container, false);
    }

    @Override
    protected Single<ChannelTabInfo> loadResult(final boolean forceLoad) {
        return ExtractorHelper.getChannelTab(serviceId, tabHandler, forceLoad);
    }

    @Override
    protected Single<ListExtractor.InfoItemsPage<InfoItem>> loadMoreItemsLogic() {
        return ExtractorHelper.getMoreChannelTabItems(serviceId, tabHandler, currentNextPage);
    }

    @Override
    protected void initListeners() {
        super.initListeners();

        if (!isPodcastsTab()) {
            return;
        }

        infoListAdapter.setOnStreamSelectedListener(new OnClickGesture<>() {
            @Override
            public void selected(final StreamInfoItem selectedItem) {
                onItemSelected(selectedItem);
                final PlayQueue playQueue = getPodcastsStreamPlayQueueStartingAt(selectedItem);
                NavigationHelper.playOnBackgroundPlayer(requireContext(), playQueue, true);
                NavigationHelper.openVideoDetailFragment(requireContext(), getFM(),
                        selectedItem.getServiceId(), selectedItem.getUrl(), selectedItem.getName(),
                        playQueue, false, false, true);
            }

            @Override
            public void held(final StreamInfoItem selectedItem) {
                showInfoItemDialog(selectedItem);
            }
        });

        infoListAdapter.setOnPlaylistSelectedListener(new OnClickGesture<>() {
            @Override
            public void selected(final PlaylistInfoItem selectedItem) {
                onItemSelected(selectedItem);
                NavigationHelper.openPlaylistFragment(getFM(),
                        selectedItem.getServiceId(),
                        selectedItem.getUrl(),
                        selectedItem.getName(),
                        true);
            }

            @Override
            public void held(final PlaylistInfoItem selectedItem) {
                NavigationHelper.openPlaylistFragment(getFM(),
                        selectedItem.getServiceId(),
                        selectedItem.getUrl(),
                        selectedItem.getName(),
                        true);
            }
        });
    }

    @Override
    public void setTitle(final String title) {
        super.setTitle(channelName);
    }

    private boolean isPodcastsTab() {
        if (tabHandler == null || tabHandler.getContentFilters().isEmpty()) {
            return false;
        }
        return ChannelTabs.PODCASTS.equals(tabHandler.getContentFilters().get(0).getName());
    }

    private PlayQueue getPodcastsStreamPlayQueueStartingAt(
            @NonNull final StreamInfoItem selectedItem) {
        final List<StreamInfoItem> streamItems = new ArrayList<>();
        for (final InfoItem item : infoListAdapter.getItemsList()) {
            if (item instanceof StreamInfoItem) {
                streamItems.add((StreamInfoItem) item);
            }
        }

        if (streamItems.isEmpty()) {
            return new SinglePlayQueue(selectedItem);
        }
        return new SinglePlayQueue(streamItems, Math.max(streamItems.indexOf(selectedItem), 0));
    }
}
