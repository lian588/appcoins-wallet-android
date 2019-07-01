package com.asfoundation.wallet.repository;

public interface PreferenceRepositoryType {
  boolean hasCompletedOnboarding();

  void setOnboardingComplete();

  boolean hasClickedSkipOnboarding();

  void setOnboardingSkipClicked();

  boolean isCreatingWallet();

  void setCreatingWallet(boolean isCreating);

  String getCurrentWalletAddress();

  void setCurrentWalletAddress(String address);
}
