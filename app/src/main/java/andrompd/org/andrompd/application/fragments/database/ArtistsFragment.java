/*
 * Copyright (C) 2016  Hendrik Borghorst & Frederik Luetkes
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package andrompd.org.andrompd.application.fragments.database;


import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import java.util.List;

import andrompd.org.andrompd.R;
import andrompd.org.andrompd.application.adapters.ArtistsGridAdapter;
import andrompd.org.andrompd.application.loaders.ArtistsLoader;
import andrompd.org.andrompd.application.utils.ScrollSpeedListener;
import andrompd.org.andrompd.mpdservice.handlers.MPDConnectionStateChangeHandler;
import andrompd.org.andrompd.mpdservice.handlers.serverhandler.MPDQueryHandler;
import andrompd.org.andrompd.mpdservice.mpdprotocol.mpddatabase.MPDAlbum;
import andrompd.org.andrompd.mpdservice.mpdprotocol.mpddatabase.MPDArtist;

public class ArtistsFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<MPDArtist>>, AdapterView.OnItemClickListener {
    private static final String TAG = "ArtistFragment";
    /**
     * GridView adapter object used for this GridView
     */
    private ArtistsGridAdapter mArtistAdapter;

    /**
     * Save the root GridView for later usage.
     */
    private GridView mRootGrid;

    /**
     * Save the last position here. Gets reused when the user returns to this view after selecting sme
     * albums.
     */
    private int mLastPosition;

    private ArtistSelectedCallback mSelectedCallback;

    private ConnectionStateListener mConnectionStateListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_artists, container, false);

        // get gridview
        mRootGrid = (GridView) rootView.findViewById(R.id.artists_gridview);

        // add progressbar
        mRootGrid.setEmptyView(rootView.findViewById(R.id.artists_progressbar));

        mArtistAdapter = new ArtistsGridAdapter(getActivity(), mRootGrid);

        mRootGrid.setAdapter(mArtistAdapter);
        mRootGrid.setOnScrollListener(new ScrollSpeedListener(mArtistAdapter, mRootGrid));
        mRootGrid.setOnItemClickListener(this);

        // register for context menu
        registerForContextMenu(mRootGrid);

        mConnectionStateListener = new ConnectionStateListener(this);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Prepare loader ( start new one or reuse old )
        getLoaderManager().initLoader(0, getArguments(), this);
        MPDQueryHandler.registerConnectionStateListener(mConnectionStateListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.v(TAG,"onPause");
        getLoaderManager().destroyLoader(0);
        MPDQueryHandler.unregisterConnectionStateListener(mConnectionStateListener);
    }

    /**
     * Called when the fragment is first attached to its context.
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mSelectedCallback = (ArtistSelectedCallback) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnArtistSelectedListener");
        }
    }

    /**
     * This method creates a new loader for this fragment.
     *
     * @param id
     * @param args
     * @return
     */
    @Override
    public Loader<List<MPDArtist>> onCreateLoader(int id, Bundle args) {
        return new ArtistsLoader(getActivity());
    }

    /**
     * Called when the loader finished loading its data.
     *
     * @param loader The used loader itself
     * @param data   Data of the loader
     */
    @Override
    public void onLoadFinished(Loader<List<MPDArtist>> loader, List<MPDArtist> data) {
        // Set the actual data to the adapter.
        mArtistAdapter.swapModel(data);

        // Reset old scroll position
        if (mLastPosition >= 0) {
            mRootGrid.setSelection(mLastPosition);
            mLastPosition = -1;
        }
    }

    /**
     * If a loader is reset the model data should be cleared.
     *
     * @param loader Loader that was resetted.
     */
    @Override
    public void onLoaderReset(Loader<List<MPDArtist>> loader) {
        // Clear the model data of the adapter.
        mArtistAdapter.swapModel(null);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mLastPosition = position;

        MPDArtist artist = (MPDArtist) mArtistAdapter.getItem(position);

        mSelectedCallback.onArtistSelected(artist.getArtistName());
    }

    public interface ArtistSelectedCallback {
        void onArtistSelected(String artistname);
    }

    private class ConnectionStateListener extends MPDConnectionStateChangeHandler {
        private ArtistsFragment pFragment;

        public ConnectionStateListener(ArtistsFragment fragment) {
            pFragment = fragment;
        }

        @Override
        public void onConnected() {
            Log.v(TAG,"Reconnected to mpd server, refetch artist list");
            // Prepare loader ( start new one or reuse old )
            getLoaderManager().initLoader(0, getArguments(), pFragment);
        }

        @Override
        public void onDisconnected() {
            getLoaderManager().destroyLoader(0);
        }
    }
}