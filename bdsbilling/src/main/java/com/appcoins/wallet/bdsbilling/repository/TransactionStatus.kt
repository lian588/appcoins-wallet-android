package com.appcoins.wallet.bdsbilling.repository

enum class TransactionStatus {
  PENDING_SERVICE_AUTHORIZATION,
  PENDING_USER_PAYMENT,
  PROCESSING,
  SETTLED,
  COMPLETED,
  CANCELED,
  FAILED
}