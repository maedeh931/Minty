package com.minty.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.minty.app.databinding.ItemChallengeBinding;

import java.util.List;

/**
 * Adapter for collaborative study challenges.
 */
public class StudyChallengeAdapter extends RecyclerView.Adapter<StudyChallengeAdapter.ChallengeViewHolder> {

    public interface OnChallengeActionListener {
        void onAccept(StudyChallenge challenge);
        void onDecline(StudyChallenge challenge);
    }

    private List<StudyChallenge> challenges;
    private final String currentUid;
    private final OnChallengeActionListener listener;

    public StudyChallengeAdapter(List<StudyChallenge> challenges, String currentUid, OnChallengeActionListener listener) {
        this.challenges = challenges;
        this.currentUid = currentUid;
        this.listener = listener;
    }

    public void setChallenges(List<StudyChallenge> challenges) {
        this.challenges = challenges;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChallengeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemChallengeBinding binding = ItemChallengeBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ChallengeViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ChallengeViewHolder holder, int position) {
        StudyChallenge challenge = challenges.get(position);

        holder.binding.tvChallengeTitle.setText(challenge.getTitle());

        long daysLeft = Math.max(0, (challenge.getEndDate() - System.currentTimeMillis()) / 86400000L);
        holder.binding.tvChallengeMeta.setText("Target: " + challenge.getTargetDisplay() + " • Ends in " + daysLeft + " days");

        String status = challenge.getStatus();
        if (StudyChallenge.STATUS_ACTIVE.equals(status)) {
            holder.binding.tvChallengeStatusBadge.setText("Active 🟢");
            holder.binding.layoutChallengeActions.setVisibility(View.GONE);
        } else if (StudyChallenge.STATUS_COMPLETED.equals(status)) {
            holder.binding.tvChallengeStatusBadge.setText("Completed 🏆");
            holder.binding.layoutChallengeActions.setVisibility(View.GONE);
        } else if (StudyChallenge.STATUS_PENDING.equals(status)) {
            holder.binding.tvChallengeStatusBadge.setText("Pending ⏳");
            boolean isInvited = currentUid != null && currentUid.equals(challenge.getInvitedUid());
            holder.binding.layoutChallengeActions.setVisibility(isInvited ? View.VISIBLE : View.GONE);
        } else {
            holder.binding.tvChallengeStatusBadge.setText(status);
            holder.binding.layoutChallengeActions.setVisibility(View.GONE);
        }

        // Dual progress calculation
        int target = Math.max(1, challenge.getTarget());
        int myProg = challenge.getMyProgress(currentUid);
        int buddyProg = challenge.getBuddyProgress(currentUid);

        int myPercent = Math.min(100, (int) Math.round((myProg / (double) target) * 100));
        int buddyPercent = Math.min(100, (int) Math.round((buddyProg / (double) target) * 100));

        boolean isCreator = currentUid != null && currentUid.equals(challenge.getCreatorUid());
        String myName = "You";
        String buddyName = isCreator ? challenge.getInvitedName() : challenge.getCreatorName();

        holder.binding.tvMyNameProgress.setText(myName + ":");
        holder.binding.tvMyStatsProgress.setText(myProg + " / " + challenge.getTargetDisplay() + " (" + myPercent + "%)");
        holder.binding.pbMyProgress.setProgress(myPercent);

        holder.binding.tvBuddyNameProgress.setText(buddyName + ":");
        holder.binding.tvBuddyStatsProgress.setText(buddyProg + " / " + challenge.getTargetDisplay() + " (" + buddyPercent + "%)");
        holder.binding.pbBuddyProgress.setProgress(buddyPercent);

        holder.binding.btnAcceptChallenge.setOnClickListener(v -> {
            if (listener != null) listener.onAccept(challenge);
        });

        holder.binding.btnDeclineChallenge.setOnClickListener(v -> {
            if (listener != null) listener.onDecline(challenge);
        });
    }

    @Override
    public int getItemCount() {
        return challenges != null ? challenges.size() : 0;
    }

    public static class ChallengeViewHolder extends RecyclerView.ViewHolder {
        final ItemChallengeBinding binding;

        public ChallengeViewHolder(@NonNull ItemChallengeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
