package com.danspot.dsprsrvservice.Recycler;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.danspot.dsprsrvservice.Entity.ReservationInfo;
import com.danspot.dsprsrvservice.Service.GoogleCalendarApi;
import com.danspot.dsprsrvservice.MainActivity;
import com.danspot.dsprsrvservice.R;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import java.util.List;

public class DspRecyclerAdapter extends RecyclerView.Adapter<DspRecyclerAdapter.DspViewHolder> {
    Context mContext;
    List<ReservationInfo> mData;
    GoogleAccountCredential mCredential;
    ProgressDialog mProgress;
    MainActivity mainActivity;

    public DspRecyclerAdapter(Context context, List<ReservationInfo> data, GoogleAccountCredential credential, ProgressDialog progress, MainActivity activity){
        this.mContext = context;
        this.mData = data;
        this.mCredential = credential;
        this.mProgress = progress;
        this.mainActivity = activity;
        notifyDataSetChanged();
    }

    public void setData(List<ReservationInfo> data){
        this.mData = data;
        notifyDataSetChanged();;
    }

    @NonNull
    @Override
    public DspViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflate = LayoutInflater.from(mContext);
        View view = inflate.inflate(R.layout.list_item, parent, false);
        DspViewHolder vh = new DspViewHolder(view);
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull DspViewHolder holder, int position) {
        ReservationInfo resv = mData.get(position);
        holder.textView.setText("- 이름:"+resv.getName()+"\n"+resv.getHall()+"\n- 예약시간:"+resv.getFmtdDate()+" "+resv.getFromTime()+"~"+resv.getToTime());
        holder.textView.setTextColor(Color.BLACK);
        holder.textView.setTextSize(20);
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public class DspViewHolder extends RecyclerView.ViewHolder{
        public TextView textView;

        public DspViewHolder(@NonNull View itemView) {
            super(itemView);
            this.textView = itemView.findViewById(R.id.textView);

            itemView.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    int pos = getAdapterPosition();
                    if(pos != RecyclerView.NO_POSITION){
                        ReservationInfo res = mData.get(pos);
                        GoogleCalendarApi calendarApi = new GoogleCalendarApi(mainActivity, mContext, mCredential, res, 2, mProgress);
                        calendarApi.getResultsFromApi();
                    }
                }
            });

        }


    }
}
