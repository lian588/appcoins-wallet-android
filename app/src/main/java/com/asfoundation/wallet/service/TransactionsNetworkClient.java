package com.asfoundation.wallet.service;

import com.asfoundation.wallet.entity.NetworkInfo;
import com.asfoundation.wallet.entity.RawTransaction;
import com.asfoundation.wallet.entity.Wallet;
import com.google.gson.Gson;
import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import io.reactivex.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.List;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class TransactionsNetworkClient implements TransactionsNetworkClientType {

  private static final int PAGE_LIMIT = 20;

  private final OkHttpClient httpClient;
  private final Gson gson;

  private ApiClient apiClient;

  public TransactionsNetworkClient(OkHttpClient httpClient, Gson gson, NetworkInfo networkInfo) {
    this.httpClient = httpClient;
    this.gson = gson;

    onNetworkChanged(networkInfo);
  }

  private void buildApiClient(String baseUrl) {
    apiClient = new Retrofit.Builder().baseUrl(baseUrl)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .build()
        .create(ApiClient.class);
  }

  @Override public Observable<RawTransaction[]> fetchLastTransactions(Wallet wallet,
      RawTransaction lastTransaction, NetworkInfo networkInfo) {
    return Observable.fromCallable(() -> {
      if (networkInfo.isMainNetwork) {
        @NonNull String lastTransactionHash = lastTransaction == null ? "" : lastTransaction.hash;
        List<RawTransaction> result = new ArrayList<>();
        int pages = 0;
        int page = 0;
        boolean hasMore = true;
        do {
          page++;
          Call<ApiClientResponse> call =
              apiClient.fetchTransactions(PAGE_LIMIT, page, wallet.address);
          Response<ApiClientResponse> response = call.execute();
          if (response.isSuccessful()) {
            ApiClientResponse body = response.body();
            if (body != null) {
              pages = body.pages;
              for (RawTransaction transaction : body.docs) {
                if (lastTransactionHash.equals(transaction.hash)) {
                  hasMore = false;
                  break;
                }
                result.add(transaction);
              }
            }
          }
        } while (page < pages && hasMore);
        return result.toArray(new RawTransaction[result.size()]);
      } else {
        return new RawTransaction[0];
      }
    })
        .subscribeOn(Schedulers.io());
  }

  private void onNetworkChanged(NetworkInfo networkInfo) {
    buildApiClient(networkInfo.backendUrl);
  }

  private interface ApiClient {
    @GET("/transactions") Call<ApiClientResponse> fetchTransactions(@Query("limit") int pageLimit,
        @Query("page") int page, @Query("address") String address);
  }

  private final static class ApiClientResponse {
    RawTransaction[] docs;
    int pages;
  }

}
