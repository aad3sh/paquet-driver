package com.example.paquetdriver;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.mapbox.api.geocoding.v5.MapboxGeocoding;
import com.mapbox.api.geocoding.v5.models.GeocodingResponse;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
//import com.mapbox.geocoder.GeocoderCriteria;
//import com.mapbox.geocoder.MapboxGeocoder;
//import com.mapbox.geocoder.service.models.GeocoderFeature;
//import com.mapbox.geocoder.service.models.GeocoderResponse;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GeocoderAdapter extends BaseAdapter implements Filterable {

    private final Context context;

    private GeocoderFilter geocoderFilter;

    private List<CarmenFeature> features;

    public GeocoderAdapter(Context context) {
        this.context = context;
    }

    /*
     * Required by BaseAdapter
     */

    @Override
    public int getCount() {
        return features.size();
    }

    @Override
    public CarmenFeature getItem(int position) {
        return features.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    /*
     * Get a View that displays the data at the specified position in the data set.
     */

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get view
        View view;
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            view = inflater.inflate(android.R.layout.simple_dropdown_item_1line, parent, false);
        } else {
            view = convertView;
        }

        // It always is a textview
        TextView text = (TextView) view;
        text.setTextColor(context.getResources().getColor(R.color.colorPrimary));
        Typeface tf = Typeface.createFromAsset(context.getAssets(), "fonts/opensans_light.ttf");
        text.setTypeface(tf);

        // Set the place name
        CarmenFeature feature = getItem(position);
        text.setText(feature.placeName());

        return view;
    }

    /*
     * Required by Filterable
     */

    @Override
    public Filter getFilter() {
        if (geocoderFilter == null) {
            geocoderFilter = new GeocoderFilter();
        }

        return geocoderFilter;
    }

    private class GeocoderFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();

            // No constraint
            if (TextUtils.isEmpty(constraint)) {
                return results;
            }


            // The geocoder client
            MapboxGeocoding client = MapboxGeocoding.builder()
                    .accessToken("pk.eyJ1IjoicmVka2F5IiwiYSI6ImNqN2lpb2xjeTF0MTgzMm5wamY2NXJ2emcifQ.yHlWsKDNfEd-7vKTysjjqQ")
                    .query(constraint.toString())
                    .build();

            Response<GeocodingResponse> response;
            try {
                response = client.executeCall();
            } catch (IOException e) {
                e.printStackTrace();
                return results;
            }

            features = response.body().features();
            results.values = features;
            results.count = features.size();
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            if (results != null && results.count > 0) {
                features = (List<CarmenFeature>) results.values;
                notifyDataSetChanged();
            } else {
                notifyDataSetInvalidated();
            }
        }
    }
}