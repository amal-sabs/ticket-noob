package com.example.ticketnoob.util;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ticketnoob.R;
import com.example.ticketnoob.model.Event;
import com.example.ticketnoob.model.Reservation;

import java.util.List;
import java.util.Locale;

public class ReservationsAdapter extends RecyclerView.Adapter<ReservationsAdapter.ReservationViewHolder> {

    public interface OnCancelClickListener {
        void onCancel(Reservation reservation);
    }

    private final List<Reservation> reservations;
    private final List<Event> events;
    private final OnCancelClickListener cancelListener;

    public ReservationsAdapter(List<Reservation> reservations, List<Event> events, OnCancelClickListener cancelListener) {
        this.reservations = reservations;
        this.events = events;
        this.cancelListener = cancelListener;
    }

    public void updateData(List<Reservation> newReservations, List<Event> newEvents) {
        reservations.clear();
        reservations.addAll(newReservations);
        events.clear();
        events.addAll(newEvents);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReservationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reservation, parent, false);
        return new ReservationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReservationViewHolder holder, int position) {
        Reservation reservation = reservations.get(position);
        Event event = events.get(position);
        holder.bind(event, reservation);
    }

    @Override
    public int getItemCount() {
        return reservations.size();
    }

    class ReservationViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle, tvDate, tvLocation, tvPrice;
        private final Button btnCancel;

        ReservationViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvReservationTitle);
            tvDate = itemView.findViewById(R.id.tvReservationDate);
            tvLocation = itemView.findViewById(R.id.tvReservationLocation);
            tvPrice = itemView.findViewById(R.id.tvReservationPrice);
            btnCancel = itemView.findViewById(R.id.btnCancelReservation);
        }

        void bind(Event event, Reservation reservation) {
            tvTitle.setText(event.getTitle());
            tvDate.setText(event.getDate());
            tvLocation.setText(event.getLocation());
            tvPrice.setText(String.format(Locale.US, "$%.2f", event.getPrice()));
            btnCancel.setOnClickListener(v -> cancelListener.onCancel(reservation));
        }
    }
}