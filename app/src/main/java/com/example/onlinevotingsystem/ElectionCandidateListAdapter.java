package com.example.onlinevotingsystem;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ElectionCandidateListAdapter extends RecyclerView.Adapter<ElectionCandidateListAdapter.ElectionCandidateListViewHolder> {
    private final List<String> candidateNames;
    private final OnCandidateClickListener listener;
    private int selectedPosition = -1;

    public ElectionCandidateListAdapter(List<String> candidateNames, OnCandidateClickListener listener) {
        this.candidateNames = candidateNames;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ElectionCandidateListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_candidate_layout, parent, false);
        return new ElectionCandidateListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ElectionCandidateListViewHolder holder, int position) {
        String candidateName = candidateNames.get(position);
        holder.candidateName.setText(candidateName);
        holder.radioButton.setChecked(position == selectedPosition);

        holder.radioButton.setOnClickListener(v -> {
            selectedPosition = holder.getAdapterPosition();
            listener.onCandidateSelected(candidateName);
            notifyDataSetChanged();
        });
    }

    @Override
    public int getItemCount() {
        return candidateNames.size();
    }

    public interface OnCandidateClickListener {
        void onCandidateSelected(String candidate);
    }

    public static class ElectionCandidateListViewHolder extends RecyclerView.ViewHolder {
        TextView candidateName;
        RadioButton radioButton;

        public ElectionCandidateListViewHolder(@NonNull View itemView) {
            super(itemView);
            candidateName = itemView.findViewById(R.id.candidateName);
            radioButton = itemView.findViewById(R.id.rdCandidateValue);
        }
    }
}
