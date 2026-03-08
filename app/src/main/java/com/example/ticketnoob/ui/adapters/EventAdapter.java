package com.example.ticketnoob.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ticketnoob.R;
import com.example.ticketnoob.model.Event;

import java.util.List;
import java.util.Locale;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    public interface OnEventClickListener {
        void onEventClick(Event event);
    }

    public interface OnEditClickListener {
        void onEditClick(Event event);
    }

    private List<Event> events;
    private final OnEventClickListener listener;
    private OnEditClickListener editListener;
    private boolean isAdmin = false;

    public EventAdapter(List<Event> events, OnEventClickListener listener) {
        this.events = events;
        this.listener = listener;
    }

    public void setAdminMode(boolean isAdmin, OnEditClickListener editListener) {
        this.isAdmin = isAdmin;
        this.editListener = editListener;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = events.get(position);

        holder.tvTitle.setText(event.getTitle());
        holder.tvDate.setText(event.getDate());
        holder.tvLocation.setText(event.getLocation());
        holder.tvCategory.setText(event.getCategory());
        holder.tvPrice.setText(String.format(Locale.US, "$%.2f", event.getPrice()));
        holder.tvSeats.setText(event.getAvailableSeats() + " seats left");

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onEventClick(event);
        });

        // Show/hide edit button based on admin status
        if (isAdmin && holder.btnEdit != null) {
            holder.btnEdit.setVisibility(View.VISIBLE);
            holder.btnEdit.setOnClickListener(v -> {
                if (editListener != null) editListener.onEditClick(event);
            });
        } else if (holder.btnEdit != null) {
            holder.btnEdit.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return events != null ? events.size() : 0;
    }

    public void updateEvents(List<Event> newEvents) {
        this.events = newEvents;
        notifyDataSetChanged();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDate, tvLocation, tvCategory, tvPrice, tvSeats;
        Button btnEdit;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvEventTitle);
            tvDate = itemView.findViewById(R.id.tvEventDate);
            tvLocation = itemView.findViewById(R.id.tvEventLocation);
            tvCategory = itemView.findViewById(R.id.tvEventCategory);
            tvPrice = itemView.findViewById(R.id.tvEventPrice);
            tvSeats = itemView.findViewById(R.id.tvEventSeats);
            btnEdit = itemView.findViewById(R.id.btnEditEvent);
        }
    }
}