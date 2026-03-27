package com.kaeru.shutthebox.ui.home;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.kaeru.shutthebox.R;
import com.kaeru.shutthebox.databinding.FragmentHomeBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class HomeFragment extends Fragment implements View.OnTouchListener {

    private FragmentHomeBinding binding;
    private MediaPlayer mediaPlayerRoll, mediaPlayerClick, mediaPlayerRestart, mediaPlayerAxe, mediaPlayerWin, mediaPlayerLose;
    private int zar1, zar2, zarbakiyesi, skor;
    private List<Integer> kalansayilar;
    private List<ImageView> sayiImageViews;

    private static final int[] diceImages = {
            R.drawable.zar1, R.drawable.zar2, R.drawable.zar3,
            R.drawable.zar4, R.drawable.zar5, R.drawable.zar6
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeUI();
        skor = 0;
        updateScoreDisplay();
        zarbakiyesi = 0;
    }

    private void initializeUI() {
        setupMediaPlayers();

        kalansayilar = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        sayiImageViews = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            int resId = getResources().getIdentifier("sayi" + (i + 1), "id", requireContext().getPackageName());
            ImageView imageView = binding.getRoot().findViewById(resId);
            if (imageView != null) {
                imageView.setTag(i + 1);
                imageView.setOnTouchListener(this);
                sayiImageViews.add(imageView);
            }
        }

        binding.bilgi.setVisibility(View.GONE);
        binding.restart.setVisibility(View.GONE);
        binding.restart.setOnClickListener(v -> restartGame());
        binding.zarat.setOnClickListener(v -> rollDice());
        binding.winner.setVisibility(View.GONE);
        binding.loser.setVisibility(View.GONE);
        binding.kaybetmenedeni.setVisibility(View.INVISIBLE);
    }

    private void setupMediaPlayers() {
        mediaPlayerRoll = MediaPlayer.create(requireContext(), R.raw.rolling_dice);
        mediaPlayerClick = MediaPlayer.create(requireContext(), R.raw.click);
        mediaPlayerRestart = MediaPlayer.create(requireContext(), R.raw.restart);
        mediaPlayerAxe = MediaPlayer.create(requireContext(), R.raw.axe);
        mediaPlayerLose = MediaPlayer.create(requireContext(), R.raw.losesound);
        mediaPlayerWin = MediaPlayer.create(requireContext(), R.raw.winsound);
    }

    private void rollDice() {
        Random random = new Random();
        zar1 = random.nextInt(6) + 1;
        zar2 = random.nextInt(6) + 1;
        zarbakiyesi = zar1 + zar2;

        binding.kalanzarbakiyesi.setVisibility(View.VISIBLE);
        binding.kalanzarbakiyesi.setText(getString(R.string.dice_balance_format, zarbakiyesi));
        binding.bilgi.setVisibility(View.VISIBLE);
        binding.imageViewZar1.setImageResource(diceImages[zar1 - 1]);
        binding.imageViewZar2.setImageResource(diceImages[zar2 - 1]);

        binding.zarat.setVisibility(View.INVISIBLE);

        if (mediaPlayerRoll != null) {
            mediaPlayerRoll.seekTo(0);
            mediaPlayerRoll.start();
        }

        if (!kontrol(kalansayilar, zarbakiyesi)) {
            endGame(false);
        }
    }

    private boolean kontrol(List<Integer> sayilar, int zarToplami) {
        if (zarToplami == 0) {
            return true;
        }
        return isPossible(sayilar, zarToplami, 0);
    }

    private boolean isPossible(List<Integer> sayilar, int target, int startIndex) {
        if (target == 0) {
            return true;
        }

        for (int i = startIndex; i < sayilar.size(); i++) {
            if (sayilar.get(i) <= target) {
                int number = sayilar.get(i);
                sayilar.remove(i);
                boolean result = isPossible(sayilar, target - number, i);
                sayilar.add(i, number);

                if (result) {
                    return true;
                }
            }
        }
        return false;
    }

    private void endGame(boolean isWin) {
        binding.restart.setVisibility(View.VISIBLE);
        binding.kalanzarbakiyesi.setVisibility(View.INVISIBLE);
        binding.bilgi.setVisibility(View.GONE);
        binding.zarat.setVisibility(View.GONE);

        if (isWin) {
            if (mediaPlayerWin != null) {
                mediaPlayerWin.seekTo(0);
                mediaPlayerWin.start();
            }
            Glide.with(this).asGif().load(R.drawable.win2).into(binding.gifImageView);
            binding.winner.setVisibility(View.VISIBLE);
        } else {
            if (mediaPlayerLose != null) {
                mediaPlayerLose.seekTo(0);
                mediaPlayerLose.start();
            }
            binding.loser.setVisibility(View.VISIBLE);
            binding.kaybetmenedeni.setVisibility(View.VISIBLE);
        }

        saveGameScore();
    }

    private void restartGame() {
        if (mediaPlayerRestart != null) {
            mediaPlayerRestart.seekTo(0);
            mediaPlayerRestart.start();
        }
        binding.restart.setVisibility(View.GONE);
        binding.winner.setVisibility(View.GONE);
        binding.loser.setVisibility(View.GONE);
        binding.kaybetmenedeni.setVisibility(View.INVISIBLE);
        binding.zarat.setVisibility(View.VISIBLE);
        binding.gifImageView.setImageDrawable(null);

        kalansayilar = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        for (ImageView imageView : sayiImageViews) {
            imageView.setVisibility(View.VISIBLE);
        }
        skor = 0;
        updateScoreDisplay();
        zarbakiyesi = 0;
        binding.kalanzarbakiyesi.setText(R.string.please_roll_dice);
    }

    private void updateScoreDisplay() {
        int kalansayilartoplami = 0;
        for (int num : kalansayilar) {
            kalansayilartoplami += num;
        }
        skor = (int) (100 - (kalansayilartoplami * 100.0 / 55.0));
        binding.skorugoster.setText(getString(R.string.score_format, skor));
    }

    private void saveGameScore() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            String userId = auth.getCurrentUser().getUid();
            db.collection("users")
                    .document(userId)
                    .collection("scores")
                    .add(new Score(skor))
                    .addOnSuccessListener(doc -> showToast(getString(R.string.score_saved)))
                    .addOnFailureListener(e -> showToast(getString(R.string.score_save_failed, e.getMessage())));
        } else {
            showToast(getString(R.string.user_not_logged_in));
        }
    }

    private void showToast(String message) {
        if (isAdded()) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            int tappedNumber = (int) v.getTag();
            if (kalansayilar.contains(tappedNumber) && zarbakiyesi >= tappedNumber) {
                if (mediaPlayerAxe != null) {
                    mediaPlayerAxe.seekTo(0);
                    mediaPlayerAxe.start();
                }
                kalansayilar.remove(Integer.valueOf(tappedNumber));
                zarbakiyesi -= tappedNumber;

                if (!kontrol(kalansayilar, zarbakiyesi)) {
                    endGame(false);
                } else if (kalansayilar.isEmpty()) {
                    endGame(true);
                }

                v.setVisibility(View.GONE);
                if (zarbakiyesi == 0) {
                    binding.kalanzarbakiyesi.setText(R.string.please_roll_dice);
                    binding.zarat.setVisibility(View.VISIBLE);
                } else {
                    binding.kalanzarbakiyesi.setText(getString(R.string.remaining_dice_balance_format, zarbakiyesi));
                }
                updateScoreDisplay();
            }
        }
        return true;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        releaseMediaPlayers();
        binding = null;
    }

    private void releaseMediaPlayers() {
        if (mediaPlayerRoll != null) { mediaPlayerRoll.release(); mediaPlayerRoll = null; }
        if (mediaPlayerClick != null) { mediaPlayerClick.release(); mediaPlayerClick = null; }
        if (mediaPlayerRestart != null) { mediaPlayerRestart.release(); mediaPlayerRestart = null; }
        if (mediaPlayerAxe != null) { mediaPlayerAxe.release(); mediaPlayerAxe = null; }
        if (mediaPlayerWin != null) { mediaPlayerWin.release(); mediaPlayerWin = null; }
        if (mediaPlayerLose != null) { mediaPlayerLose.release(); mediaPlayerLose = null; }
    }

    public static class Score {
        private int score;
        public Score() {} // Required for Firestore
        public Score(int score) { this.score = score; }
        public int getScore() { return score; }
        public void setScore(int score) { this.score = score; }
    }
}
