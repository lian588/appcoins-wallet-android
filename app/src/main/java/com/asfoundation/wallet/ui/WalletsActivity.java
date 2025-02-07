package com.asfoundation.wallet.ui;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.asf.wallet.R;
import com.asfoundation.wallet.entity.ErrorEnvelope;
import com.asfoundation.wallet.entity.Wallet;
import com.asfoundation.wallet.ui.widget.adapter.WalletsAdapter;
import com.asfoundation.wallet.util.KeyboardUtils;
import com.asfoundation.wallet.viewmodel.WalletsViewModel;
import com.asfoundation.wallet.viewmodel.WalletsViewModelFactory;
import com.asfoundation.wallet.widget.AddWalletView;
import com.asfoundation.wallet.widget.BackupView;
import com.asfoundation.wallet.widget.BackupWarningView;
import com.asfoundation.wallet.widget.SystemView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import dagger.android.AndroidInjection;
import javax.inject.Inject;

import static com.asfoundation.wallet.C.IMPORT_REQUEST_CODE;
import static com.asfoundation.wallet.C.SHARE_REQUEST_CODE;

public class WalletsActivity extends BaseActivity
    implements View.OnClickListener, AddWalletView.OnNewWalletClickListener,
    AddWalletView.OnImportWalletClickListener {

  private final Handler handler = new Handler();
  @Inject WalletsViewModelFactory walletsViewModelFactory;
  WalletsViewModel viewModel;
  private WalletsAdapter adapter;
  private SystemView systemView;
  private BackupWarningView backupWarning;
  private Dialog dialog;
  private boolean isSetDefault;

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    AndroidInjection.inject(this);
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_wallets);
    // Init toolbar
    toolbar();

    adapter =
        new WalletsAdapter(this::onSetWalletDefault, this::onDeleteWallet, this::onExportWallet);
    SwipeRefreshLayout refreshLayout = findViewById(R.id.refresh_layout);
    systemView = findViewById(R.id.system_view);
    backupWarning = findViewById(R.id.backup_warning);

    RecyclerView list = findViewById(R.id.list);

    list.setLayoutManager(new LinearLayoutManager(this));
    list.setAdapter(adapter);

    systemView.attachRecyclerView(list);
    systemView.attachSwipeRefreshLayout(refreshLayout);
    backupWarning.setOnPositiveClickListener((__, wallet) -> onNowBackup(wallet));
    backupWarning.setOnSkipClickListener(v -> {
      hideDialog();
      showToolbar();
      if (adapter.getItemCount() <= 1) {
        onBackPressed();
      } else {
        backupWarning.hide();
      }
    });

    viewModel = ViewModelProviders.of(this, walletsViewModelFactory)
        .get(WalletsViewModel.class);

    viewModel.error()
        .observe(this, this::onError);
    viewModel.progress()
        .observe(this, systemView::showProgress);
    viewModel.wallets()
        .observe(this, this::onFetchWallet);
    viewModel.defaultWallet()
        .observe(this, this::onChangeDefaultWallet);
    viewModel.createdWallet()
        .observe(this, this::onCreatedWallet);
    viewModel.createWalletError()
        .observe(this, this::onCreateWalletError);
    viewModel.exportedStore()
        .observe(this, this::openShareDialog);
    viewModel.exportWalletError()
        .observe(this, this::onExportWalletError);
    viewModel.deleteWalletError()
        .observe(this, this::onDeleteWalletError);

    refreshLayout.setOnRefreshListener(viewModel::fetchWallets);
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.action_add: {
        onAddWallet();
      }
      break;
      case android.R.id.home: {
        onBackPressed();
        return true;
      }
    }
    return super.onOptionsItemSelected(item);
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (requestCode == IMPORT_REQUEST_CODE) {
      showToolbar();
      if (resultCode == RESULT_OK) {
        viewModel.fetchWallets();
        Snackbar.make(systemView, getString(R.string.toast_message_wallet_imported),
            Snackbar.LENGTH_SHORT)
            .show();
        if (adapter.getItemCount() <= 1) {
          viewModel.showTransactions(this);
        }
      }
    } else if (requestCode == SHARE_REQUEST_CODE) {
      if (resultCode == RESULT_OK) {
        Snackbar.make(systemView, getString(R.string.toast_message_wallet_exported),
            Snackbar.LENGTH_SHORT)
            .show();
        backupWarning.hide();
        showToolbar();
        hideDialog();
        if (adapter.getItemCount() <= 1) {
          onBackPressed();
        }
      } else {
        dialog = buildDialog().setMessage(R.string.do_manage_make_backup)
            .setPositiveButton(R.string.yes_continue, (dialog, which) -> {
              hideDialog();
              backupWarning.hide();
              showToolbar();
              if (adapter.getItemCount() <= 1) {
                onBackPressed();
              }
            })
            .setNegativeButton(R.string.no_repeat, (dialog, which) -> {
              openShareDialog(viewModel.exportedStore()
                  .getValue());
              hideDialog();
            })
            .create();
        dialog.show();
      }
    }
  }

  private void onCreateWalletError(ErrorEnvelope errorEnvelope) {
    dialog = buildDialog().setTitle(R.string.title_dialog_error)
        .setMessage(
            TextUtils.isEmpty(errorEnvelope.message) ? getString(R.string.error_create_wallet)
                : errorEnvelope.message)
        .setPositiveButton(R.string.ok, (dialog, which) -> {
        })
        .create();
    dialog.show();
  }

  private void onExportWallet(Wallet wallet) {
    showBackupDialog(wallet, false);
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    if (adapter.getItemCount() > 0) {
      getMenuInflater().inflate(R.menu.menu_add, menu);
    }
    return super.onCreateOptionsMenu(menu);
  }

  @Override public void onBackPressed() {
    if (backupWarning.isShown() && adapter.getItemCount() > 1) {
      hideDialog();
      showToolbar();
      backupWarning.hide();
    } else {
      // User can't start work without wallet.
      if (adapter.getItemCount() > 0) {
        super.onBackPressed();
      } else {
        finish();
        System.exit(0);
      }
    }
  }

  @Override protected void onPause() {
    super.onPause();

    hideDialog();
  }

  @Override public void onClick(View view) {
    if (view.getId() == R.id.try_again) {
      viewModel.fetchWallets();
    }
  }

  @Override public void onNewWallet(View view) {
    hideDialog();
    viewModel.newWallet();
  }

  @Override public void onImportWallet(View view) {
    hideDialog();
    viewModel.importWallet(this);
  }

  private void onAddWallet() {
    AddWalletView addWalletView = new AddWalletView(this);
    addWalletView.setOnNewWalletClickListener(this);
    addWalletView.setOnImportWalletClickListener(this);
    dialog = new BottomSheetDialog(this);
    dialog.setContentView(addWalletView);
    dialog.setCancelable(true);
    dialog.setCanceledOnTouchOutside(true);
    BottomSheetBehavior behavior = BottomSheetBehavior.from((View) addWalletView.getParent());
    dialog.setOnShowListener(dialog -> behavior.setPeekHeight(addWalletView.getHeight()));
    dialog.show();
  }

  private void onChangeDefaultWallet(Wallet wallet) {
    if (isSetDefault) {
      viewModel.showTransactions(this);
    } else {
      adapter.setDefaultWallet(wallet);
    }
  }

  private void onFetchWallet(Wallet[] wallets) {
    enableDisplayHomeAsUp();
    adapter.setWallets(wallets);
    systemView.setVisibility(View.GONE);
    invalidateOptionsMenu();
  }

  private void onCreatedWallet(Wallet wallet) {
    hideToolbar();
    backupWarning.show(wallet);
  }

  private void onNowBackup(Wallet wallet) {
    showBackupDialog(wallet, true);
  }

  private void showBackupDialog(Wallet wallet, boolean isNew) {
    BackupView view = new BackupView(this);
    dialog = buildDialog().setView(view)
        .setPositiveButton(R.string.ok, (dialogInterface, i) -> {
          viewModel.exportWallet(wallet, view.getPassword());
          KeyboardUtils.hideKeyboard(view.findViewById(R.id.password));
        })
        .setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
          if (isNew) {
            onCreatedWallet(wallet);
          }
          KeyboardUtils.hideKeyboard(view.findViewById(R.id.password));
        })
        .setOnDismissListener(
            dialog -> KeyboardUtils.hideKeyboard(view.findViewById(R.id.password)))
        .create();
    dialog.show();
    handler.postDelayed(() -> KeyboardUtils.showKeyboard(view.findViewById(R.id.password)), 500);
  }

  private void openShareDialog(String jsonData) {
    if (jsonData != null) {
      viewModel.clearExportedStore();
      Intent sharingIntent = new Intent(Intent.ACTION_SEND);
      sharingIntent.setType("text/plain");
      sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "Keystore");
      sharingIntent.putExtra(Intent.EXTRA_TEXT, jsonData);
      startActivityForResult(Intent.createChooser(sharingIntent, "Share via"), SHARE_REQUEST_CODE);
    }
  }

  private void onExportWalletError(ErrorEnvelope errorEnvelope) {
    dialog = buildDialog().setTitle(R.string.title_dialog_error)
        .setMessage(TextUtils.isEmpty(errorEnvelope.message) ? getString(R.string.error_export)
            : errorEnvelope.message)
        .setPositiveButton(R.string.ok, (dialogInterface, with) -> {
        })
        .create();
    dialog.show();
  }

  private void onDeleteWalletError(ErrorEnvelope errorEnvelope) {
    dialog = buildDialog().setTitle(R.string.title_dialog_error)
        .setMessage(
            TextUtils.isEmpty(errorEnvelope.message) ? getString(R.string.error_deleting_account)
                : errorEnvelope.message)
        .setPositiveButton(R.string.ok, (dialogInterface, with) -> {
        })
        .create();
    dialog.show();
  }

  private void onError(ErrorEnvelope errorEnvelope) {
    systemView.showError(errorEnvelope.message, this);
  }

  private void onSetWalletDefault(Wallet wallet) {
    viewModel.setDefaultWallet(wallet);
    isSetDefault = true;
  }

  private void onDeleteWallet(Wallet wallet) {
    dialog = buildDialog().setTitle(getString(R.string.title_delete_account))
        .setMessage(getString(R.string.confirm_delete_account))
        .setIcon(R.drawable.ic_warning_black_24dp)
        .setPositiveButton(android.R.string.yes, (dialog, btn) -> viewModel.deleteWallet(wallet))
        .setNegativeButton(android.R.string.no, null)
        .create();
    dialog.show();
  }

  private AlertDialog.Builder buildDialog() {
    hideDialog();
    return new AlertDialog.Builder(this);
  }

  private void hideDialog() {
    if (dialog != null && dialog.isShowing()) {
      dialog.dismiss();
      dialog = null;
    }
  }
}
